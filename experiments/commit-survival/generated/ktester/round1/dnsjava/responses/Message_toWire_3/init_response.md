```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Message#toWire(int)}.
 *
 * <p>The tests focus on the public contract of {@code toWire(int)}:
 *
 * <ul>
 *   <li>The returned byte array must not be larger than the supplied {@code maxLength}.
 *   <li>The method must produce the same wire format as {@link Message#toWire()} when the
 *       {@code maxLength} is {@link Message#MAXLENGTH}.
 *   <li>The internal size bookkeeping (exposed via {@link Message#numBytes()}) must match the
 *       length of the returned wire format.
 *   <li>A {@link WireParseException} is thrown when the message cannot be encoded within the
 *       supplied {@code maxLength}.
 * </ul>
 *
 * <p>Only public APIs are used – no reflection or access to private members.
 */
class Message_toWire_3_Test {

    /** Common objects used by several tests. */
    private Message message;
    private Name testName;
    private Name additionalName;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a fresh Message for each test.
        message = new Message(0x1234);
        testName = Name.fromConstantString("example.com.");
        additionalName = Name.fromConstantString("additional.example.com.");

        // Add a question record (required for a well‑formed DNS message).
        Record question = Record.newRecord(testName, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Message does not hold external resources.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verify that {@code toWire(Message.MAXLENGTH)} produces the same byte array as the
     * parameter‑less {@code toWire()} overload and that the reported size matches the
     * length of the wire format.
     */
    @Test
    void toWire_defaultMaxLength_matchesToWireWithoutArgument() throws IOException {
        // Add a few additional records to make the message non‑trivial.
        message.addRecord(new TXTRecord(testName, DClass.IN, 300, "first"), Section.ANSWER);
        message.addRecord(new TXTRecord(testName, DClass.IN, 300, "second"), Section.ANSWER);
        message.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);

        byte[] wireWithMax = message.toWire(Message.MAXLENGTH);
        byte[] wireWithoutMax = message.toWire(); // uses the default max length internally

        assertArrayEquals(wireWithoutMax, wireWithMax,
                "toWire(int) with MAXLENGTH should be identical to toWire()");
        assertEquals(wireWithMax.length, message.numBytes(),
                "Message.numBytes() must report the exact length of the wire format");
        assertTrue(wireWithMax.length <= Message.MAXLENGTH,
                "Returned wire format must not exceed Message.MAXLENGTH");
    }

    /**
     * Verify that a message that fits within a small {@code maxLength} is encoded correctly.
     */
    @Test
    void toWire_smallEnoughMaxLength_encodesSuccessfully() throws IOException {
        // The message contains only the question record added in setup.
        int smallMax = 512; // typical UDP size
        byte[] wire = message.toWire(smallMax);

        assertNotNull(wire, "Wire format must not be null");
        assertTrue(wire.length <= smallMax,
                "Encoded message must respect the supplied maxLength");
        assertEquals(wire.length, message.numBytes(),
                "Message.numBytes() must match the length of the encoded byte array");
    }

    /**
     * Verify that encoding a message that exceeds the supplied {@code maxLength}
     * results in a {@link WireParseException}.
     *
     * <p>The test builds a message with many TXT records so that the total size is
     * guaranteed to be larger than a deliberately tiny limit (e.g., 100 bytes).
     */
    @Test
    void toWire_exceedsMaxLength_throwsWireParseException() {
        // Populate the message with enough records to exceed 100 bytes.
        for (int i = 0; i < 20; i++) {
            message.addRecord(
                    new TXTRecord(additionalName, DClass.IN, 300, "payload-" + i),
                    Section.ADDITIONAL);
        }

        int tinyMax = 100; // far smaller than the expected size
        assertThrows(WireParseException.class,
                () -> message.toWire(tinyMax),
                "Encoding a message larger than maxLength should throw WireParseException");
    }

    /**
     * Verify that the wire format produced by {@code toWire(int)} can be parsed back
     * into an equivalent {@link Message} instance.
     */
    @Test
    void toWire_roundTrip_parsesBackToEquivalentMessage() throws IOException {
        // Add a mixture of records.
        message.addRecord(new TXTRecord(testName, DClass.IN, 300, "roundtrip"), Section.ANSWER);
        message.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        for (int i = 0; i < 5; i++) {
            message.addRecord(
                    new TXTRecord(additionalName, DClass.IN, 300, "extra-" + i),
                    Section.ADDITIONAL);
        }

        byte[] wire = message.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);

        // Header IDs must match.
        assertEquals(message.getHeader().getID(), parsed.getHeader().getID(),
                "Parsed message must preserve the header ID");

        // Sections must contain the same records (order is preserved for each section).
        for (int sec = 0; sec < 4; sec++) {
            List<Record> original = message.getSection(sec);
            List<Record> roundTrip = parsed.getSection(sec);
            assertEquals(original.size(), roundTrip.size(),
                    "Section " + sec + " size must be preserved after round‑trip");
            for (int i = 0; i < original.size(); i++) {
                assertEquals(original.get(i).toString(), roundTrip.get(i).toString(),
                        "Record at index " + i + " in section " + sec + " must be identical");
            }
        }
    }
}
```