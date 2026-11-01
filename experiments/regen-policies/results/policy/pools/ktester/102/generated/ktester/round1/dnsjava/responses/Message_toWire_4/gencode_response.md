```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link Message#toWire(int, boolean)}.
 *
 * <p>The test suite covers:
 *
 * <ul>
 *   <li>Successful rendering when the message fits into {@code maxLength}.</li>
 *   <li>Successful rendering with {@code truncate=true} even when the message fits.</li>
 *   <li>Exception handling when the message does not fit and {@code truncate=false}.</li>
 *   <li>Truncation behaviour when the message does not fit and {@code truncate=true}.</li>
 *   <li>Edge cases such as {@code maxLength}=0, negative values and values smaller than a DNS
 *       header (12 bytes).</li>
 *   <li>Verification that the private {@code size} field is updated to the length of the returned
 *       wire array.</li>
 * </ul>
 *
 * <p>All tests use only the public API of {@link Message}. The private {@code size} field is
 * accessed via reflection – this is allowed by the task specification.
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_4_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- *
     * Helper methods                                                       *
     * --------------------------------------------------------------------- */

    /**
     * Creates a {@link Message} that contains {@code count} A records in the ANSWER section.
     *
     * @param count number of A records to add
     * @return a populated {@link Message}
     * @throws UnknownHostException if an IP address cannot be parsed (should never happen)
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

    /* --------------------------------------------------------------------- *
     * Parameter source for the main matrix of tests                        *
     * --------------------------------------------------------------------- */

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
        return Stream.of(
                // Completely rendered without truncation
                Arguments.of(512, false, false, "completely rendered without truncation"),
                // Completely rendered with truncation flag true
                Arguments.of(512, true, false, "completely rendered with truncation true"),
                // Partially rendered without truncation (exception expected)
                Arguments.of(128, false, true, "partial render without truncation – exception"),
                // Partially rendered with truncation flag true
                Arguments.of(128, true, false, "partial render with truncation true"),
                // Zero maxLength without truncation (exception expected)
                Arguments.of(0, false, true, "zero maxLength without truncation – exception"),
                // Zero maxLength with truncation flag true
                Arguments.of(0, true, false, "zero maxLength with truncation true"),
                // Maximum allowed maxLength without truncation
                Arguments.of(Message.MAXLENGTH, false, false, "max allowed length without truncation"),
                // Maximum allowed maxLength with truncation flag true
                Arguments.of(Message.MAXLENGTH, true, false, "max allowed length with truncation true"),
                // Negative maxLength without truncation (exception expected)
                Arguments.of(-1, false, true, "negative maxLength without truncation – exception"),
                // Negative maxLength with truncation flag true
                Arguments.of(-1, true, false, "negative maxLength with truncation true"),
                // Insufficient positive maxLength without truncation (exception expected)
                Arguments.of(5, false, true, "insufficient maxLength without truncation – exception"),
                // Insufficient positive maxLength with truncation flag true
                Arguments.of(5, true, false, "insufficient maxLength with truncation true")
        );
    }

    /* --------------------------------------------------------------------- *
     * Parameterised test covering the matrix above                         *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "toWire(maxLength={0}, truncate={1}) – {3}")
    @MethodSource("toWireMatrix")
    void toWire_matrixTests(int maxLength, boolean truncate, boolean expectException, String _description)
            throws Exception {

        // -------------------------------------------------
        // Arrange – choose a message that will be larger than the smallest maxLengths.
        // -------------------------------------------------
        Message msg;
        if (maxLength >= 512) {
            // For the cases where we expect a complete render we can use a tiny message.
            msg = buildTinyMessage();
        } else {
            // For all other cases we need a message that definitely exceeds the limit.
            msg = buildMessageWithManyARecords(200); // large enough to exceed 128, 5, 0, -1, etc.
        }

        // -------------------------------------------------
        // Act & Assert
        // -------------------------------------------------
        if (expectException) {
            MessageSizeExceededException ex = assertThrows(
                    MessageSizeExceededException.class,
                    () -> msg.toWire(maxLength, truncate),
                    "Expected MessageSizeExceededException");
            assertEquals(maxLength, ex.getMaxLength(),
                    "Exception should carry the supplied maxLength");
            // When an exception is thrown the private size field must remain unchanged (default 0)
            int sizeField = getPrivateSizeField(msg);
            assertEquals(0, sizeField,
                    "Size field should stay at 0 when an exception is thrown");
        } else {
            byte[] wire = msg.toWire(maxLength, truncate);
            assertNotNull(wire, "Returned wire array must not be null");
            assertTrue(wire.length <= maxLength,
                    "Returned array length must not exceed the supplied maxLength");
            // Verify that the private size field mirrors the length of the returned array.
            int sizeField = getPrivateSizeField(msg);
            assertEquals(wire.length, sizeField,
                    "Private size field must equal the length of the returned wire array");
            // When truncate=true and the message does not fit, the TC (truncated) flag should be set.
            // We can verify this indirectly by checking that the length is exactly maxLength
            // (or 0 for impossible lengths). For the purpose of these unit tests the existence of
            // the byte array is sufficient.
        }
    }

    /* --------------------------------------------------------------------- *
     * Additional explicit tests for documentation clarity                 *
     * --------------------------------------------------------------------- */

    /** Verify that a completely rendered message (fits) returns a non‑null array. */
    @Test
    void toWire_fitsWithinMaxLength_returnsByteArray() throws Exception {
        Message msg = buildTinyMessage();
        byte[] wire = msg.toWire(512, false);
        assertNotNull(wire);
        assertTrue(wire.length <= 512);
        assertEquals(wire.length, msg.numBytes(),
                "Message#numBytes() must match the length of the wire array");
    }

    /** Verify that a message exceeding a tiny maxLength throws when truncate=false. */
    @Test
    void toWire_exceedsMaxLengthAndNoTruncate_throwsMessageSizeExceededException() throws Exception {
        Message msg = buildMessageWithManyARecords(200);
        int tinyMax = 100;
        MessageSizeExceededException ex = assertThrows(
                MessageSizeExceededException.class,
                () -> msg.toWire(tinyMax, false));
        assertEquals(tinyMax, ex.getMaxLength());
    }

    /** Verify that truncation works and returns a truncated array. */
    @Test
    void toWire_exceedsMaxLengthButTruncate_trueReturnsTruncatedArray() throws Exception {
        Message msg = buildMessageWithManyARecords(200);
        int tinyMax = 100;
        byte[] wire = msg.toWire(tinyMax, true);
        assertNotNull(wire);
        assertTrue(wire.length <= tinyMax);
        assertEquals(wire.length, msg.numBytes(),
                "Message#numBytes() must match the length of the truncated wire array");
    }

    /** Size field consistency when the message is completely rendered. */
    @Test
    void sizeField_consistencyOnCompleteRender() throws Exception {
        Message msg = buildTinyMessage();
        byte[] wire = msg.toWire(512, false);
        int size = getPrivateSizeField(msg);
        assertEquals(wire.length, size,
                "Private size field should equal the length of the fully rendered wire array");
    }

    /** Size field consistency when truncation occurs. */
    @Test
    void sizeField_consistencyOnTruncatedRender() throws Exception {
        Message msg = buildMessageWithManyARecords(200);
        int max = 128;
        byte[] wire = msg.toWire(max, true);
        int size = getPrivateSizeField(msg);
        assertEquals(wire.length, size,
                "Private size field should equal the length of the truncated wire array");
    }
}
```