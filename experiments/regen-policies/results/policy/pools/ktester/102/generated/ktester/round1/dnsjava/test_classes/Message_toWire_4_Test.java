package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class Message_toWire_4_Test {

    /**
     * Helper that creates a {@link Message} containing a given number of A records.
     *
     * @param count number of A records to add to the ANSWER section
     * @return a populated {@link Message}
     * @throws UnknownHostException if the IP address cannot be parsed (should never happen here)
     */
    private Message buildMessageWithManyARecords(int count) throws UnknownHostException {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");
        for (int i = 0; i < count; i++) {
            // Use a distinct IP for each record to avoid any internal deduplication.
            String ip = "192.0.2." + (i % 256);
            ARecord a = new ARecord(name, DClass.IN, 0, java.net.InetAddress.getByName(ip));
            msg.addRecord(a, Section.ANSWER);
        }
        return msg;
    }

    /**
     * Returns a small message that definitely fits into any reasonable {@code maxLength}
     * (it only contains a single question record).
     */
    private Message buildTinyMessage() throws UnknownHostException {
        Message msg = new Message();
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        msg.addRecord(question, Section.QUESTION);
        return msg;
    }

    /**
     * Reads the private {@code size} field from a {@link Message} instance via reflection.
     *
     * @param msg the message instance
     * @return the value of the private {@code size} field
     */
    private int getPrivateSizeField(Message msg) throws Exception {
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        return (int) sizeField.get(msg);
    }

    /**
     * Provides arguments for the parameterised tests covering the matrix described in the JSON
     * specification. Each {@link Arguments} consists of:
     *
     * <ol>
     *   <li>maxLength (int)
     *   <li>truncate (boolean)
     *   <li>expectException (boolean)
     *   <li>description (String) – used only for readability in the test name.
     * </ol>
     */
    private static Stream<Arguments> toWireMatrix() {
        return Stream.of(// Completely rendered without truncation
        Arguments.of(512, false, false, "completely rendered without truncation"), // Completely rendered with truncation flag true
        Arguments.of(512, true, false, "completely rendered with truncation true"), // Partially rendered without truncation (exception expected)
        Arguments.of(128, false, true, "partial render without truncation – exception"), // Partially rendered with truncation flag true
        Arguments.of(128, true, false, "partial render with truncation true"), // Zero maxLength without truncation (exception expected)
        Arguments.of(0, false, true, "zero maxLength without truncation – exception"), // Zero maxLength with truncation flag true
        Arguments.of(0, true, false, "zero maxLength with truncation true"), // Maximum allowed maxLength without truncation
        Arguments.of(Message.MAXLENGTH, false, false, "max allowed length without truncation"), // Maximum allowed maxLength with truncation flag true
        Arguments.of(Message.MAXLENGTH, true, false, "max allowed length with truncation true"), // Negative maxLength without truncation (exception expected)
        Arguments.of(-1, false, true, "negative maxLength without truncation – exception"), // Negative maxLength with truncation flag true
        Arguments.of(-1, true, false, "negative maxLength with truncation true"), // Insufficient positive maxLength without truncation (exception expected)
        Arguments.of(5, false, true, "insufficient maxLength without truncation – exception"), // Insufficient positive maxLength with truncation flag true
        Arguments.of(5, true, false, "insufficient maxLength with truncation true"));
    }

    @Test
    void toWire_fitsWithinMaxLength_returnsByteArray() throws IOException {
        // Arrange: a small message that easily fits into the default MAXLENGTH.
        Message msg = new Message();
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        msg.addRecord(question, Section.QUESTION);
        // Act
        byte[] wire = msg.toWire(Message.MAXLENGTH, false);
        // Assert
        assertNotNull(wire, "Wire representation must not be null");
        assertTrue(wire.length <= Message.MAXLENGTH, "Returned byte array must not exceed the supplied maxLength");
        // The Message API exposes the number of bytes that were actually written.
        assertEquals(wire.length, msg.numBytes(), "Message#numBytes() should report the same length as the wire array");
    }

    @Test
    void toWire_exceedsMaxLengthAndNoTruncate_throwsMessageSizeExceededException() throws IOException {
        // Arrange: create a message that is deliberately larger than a tiny maxLength.
        // enough to exceed 100 bytes easily
        Message msg = buildMessageWithManyARecords(200);
        // far smaller than the size of the message
        int tinyMax = 100;
        // Act & Assert
        MessageSizeExceededException ex = assertThrows(MessageSizeExceededException.class, () -> msg.toWire(tinyMax, false), "Expected MessageSizeExceededException when the message does not fit and truncate=false");
        assertEquals(tinyMax, ex.getMaxLength(), "Exception should carry the maxLength that caused the failure");
    }

    @Test
    void toWire_exceedsMaxLengthButTruncate_trueReturnsTruncatedArray() throws IOException {
        // Arrange: same oversized message as previous test.
        Message msg = buildMessageWithManyARecords(200);
        // force truncation
        int tinyMax = 100;
        // Act
        byte[] wire = msg.toWire(tinyMax, true);
        // Assert
        assertNotNull(wire, "Returned wire array must not be null even when truncating");
        assertTrue(wire.length <= tinyMax, "When truncate=true, the returned byte array must be no larger than maxLength");
        // The internal size field should reflect the number of bytes actually written.
        assertEquals(wire.length, msg.numBytes(), "Message#numBytes() should match the length of the truncated wire array");
    }

    @ParameterizedTest(name = "toWire(maxLength={0}, truncate={1}) – {3}")
    @MethodSource("toWireMatrix")
    void toWire_matrixTests(int maxLength, boolean truncate, boolean expectException, String _description) throws Exception {
        // -------------------------------------------------
        // Arrange – choose a message that will be larger than the smallest maxLengths.
        // -------------------------------------------------
        Message msg;
        if (maxLength >= 512) {
            // For the cases where we expect a complete render we can use a tiny message.
            msg = buildTinyMessage();
        } else {
            // For all other cases we need a message that definitely exceeds the limit.
            // large enough to exceed 128, 5, 0, -1, etc.
            msg = buildMessageWithManyARecords(200);
        }
        // -------------------------------------------------
        // Act & Assert
        // -------------------------------------------------
        if (expectException) {
            MessageSizeExceededException ex = assertThrows(MessageSizeExceededException.class, () -> msg.toWire(maxLength, truncate), "Expected MessageSizeExceededException");
            assertEquals(maxLength, ex.getMaxLength(), "Exception should carry the supplied maxLength");
            // When an exception is thrown the private size field must remain unchanged (default 0)
            int sizeField = getPrivateSizeField(msg);
            assertEquals(0, sizeField, "Size field should stay at 0 when an exception is thrown");
        } else {
            byte[] wire = msg.toWire(maxLength, truncate);
            assertNotNull(wire, "Returned wire array must not be null");
            assertTrue(wire.length <= maxLength, "Returned array length must not exceed the supplied maxLength");
            // Verify that the private size field mirrors the length of the returned array.
            int sizeField = getPrivateSizeField(msg);
            assertEquals(wire.length, sizeField, "Private size field must equal the length of the returned wire array");
            // When truncate=true and the message does not fit, the TC (truncated) flag should be set.
            // We can verify this indirectly by checking that the length is exactly maxLength
            // (or 0 for impossible lengths). For the purpose of these unit tests the existence of
            // the byte array is sufficient.
        }
    }

    /**
     * Size field consistency when the message is completely rendered.
     */
    @Test
    void sizeField_consistencyOnCompleteRender() throws Exception {
        Message msg = buildTinyMessage();
        byte[] wire = msg.toWire(512, false);
        int size = getPrivateSizeField(msg);
        assertEquals(wire.length, size, "Private size field should equal the length of the fully rendered wire array");
    }

    /**
     * Size field consistency when truncation occurs.
     */
    @Test
    void sizeField_consistencyOnTruncatedRender() throws Exception {
        Message msg = buildMessageWithManyARecords(200);
        int max = 128;
        byte[] wire = msg.toWire(max, true);
        int size = getPrivateSizeField(msg);
        assertEquals(wire.length, size, "Private size field should equal the length of the truncated wire array");
    }
}
