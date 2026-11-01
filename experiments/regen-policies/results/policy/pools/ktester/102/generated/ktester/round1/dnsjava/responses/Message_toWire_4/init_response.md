```java
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

/**
 * Tests for {@link Message#toWire(int, boolean)}.
 *
 * <p>The tests cover three scenarios:
 *
 * <ul>
 *   <li>Successful wire conversion when the message fits into the supplied {@code maxLength}.
 *   <li>Exception is thrown when the message does not fit and {@code truncate} is {@code false}.
 *   <li>No exception is thrown when the message does not fit but {@code truncate} is {@code true};
 *       the returned byte array must be no larger than {@code maxLength}.
 * </ul>
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
        assertTrue(wire.length <= Message.MAXLENGTH,
                "Returned byte array must not exceed the supplied maxLength");
        // The Message API exposes the number of bytes that were actually written.
        assertEquals(wire.length, msg.numBytes(),
                "Message#numBytes() should report the same length as the wire array");
    }

    @Test
    void toWire_exceedsMaxLengthAndNoTruncate_throwsMessageSizeExceededException() throws IOException {
        // Arrange: create a message that is deliberately larger than a tiny maxLength.
        Message msg = buildMessageWithManyARecords(200); // enough to exceed 100 bytes easily

        int tinyMax = 100; // far smaller than the size of the message

        // Act & Assert
        MessageSizeExceededException ex = assertThrows(
                MessageSizeExceededException.class,
                () -> msg.toWire(tinyMax, false),
                "Expected MessageSizeExceededException when the message does not fit and truncate=false");
        assertEquals(tinyMax, ex.getMaxLength(),
                "Exception should carry the maxLength that caused the failure");
    }

    @Test
    void toWire_exceedsMaxLengthButTruncate_trueReturnsTruncatedArray() throws IOException {
        // Arrange: same oversized message as previous test.
        Message msg = buildMessageWithManyARecords(200);
        int tinyMax = 100; // force truncation

        // Act
        byte[] wire = msg.toWire(tinyMax, true);

        // Assert
        assertNotNull(wire, "Returned wire array must not be null even when truncating");
        assertTrue(wire.length <= tinyMax,
                "When truncate=true, the returned byte array must be no larger than maxLength");
        // The internal size field should reflect the number of bytes actually written.
        assertEquals(wire.length, msg.numBytes(),
                "Message#numBytes() should match the length of the truncated wire array");
    }
}
```