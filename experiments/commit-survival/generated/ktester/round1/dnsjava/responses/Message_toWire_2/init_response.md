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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#toWire()}.
 *
 * The tests create a {@link Message}, add records, convert it to a wire‑format
 * byte array with {@code toWire()}, then reconstruct a new {@link Message}
 * from that byte array.  The reconstructed message is compared with the
 * original to verify that the wire conversion is lossless.
 *
 * No private members of {@link Message} are accessed; the public API is used
 * exclusively.
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_2_Test {

    private static final Name QUESTION_NAME = Name.fromConstantString("www.example.");
    private static final Name ADDITIONAL_NAME = Name.fromConstantString("test.example.");

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise per‑test at the moment.
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
     * Verify that an empty message can be serialized and deserialized without
     * loss of information.
     */
    @Test
    void toWire_emptyMessage() throws IOException {
        Message original = new Message();               // default header, no sections
        byte[] wire = original.toWire();

        // The wire format must contain at least the 12‑byte DNS header.
        assertNotNull(wire);
        assertTrue(wire.length >= Message.MAXLENGTH - Message.MAXLENGTH + 12); // just >0

        Message parsed = new Message(wire);
        // Header IDs should be identical.
        assertEquals(original.getHeader().getID(), parsed.getHeader().getID());

        // All sections must be empty.
        for (int i = 0; i < 4; i++) {
            assertTrue(original.getSection(i).isEmpty(),
                    "original section " + i + " should be empty");
            assertTrue(parsed.getSection(i).isEmpty(),
                    "parsed section " + i + " should be empty");
        }
    }

    /**
     * Verify that a message containing a question and several additional
     * records round‑trips correctly through {@code toWire()}.
     */
    @Test
    void toWire_messageWithRecords() throws IOException {
        // Build a message with a question and a handful of additional records.
        Message original = new Message(0x1234);
        Record question = Record.newRecord(QUESTION_NAME, Type.A, DClass.IN);
        original.addRecord(question, Section.QUESTION);

        // Add a series of TXT records to the ADDITIONAL section.
        for (int i = 1; i <= 5; i++) {
            String txt = "sample-" + i;
            Record txtRec = new TXTRecord(ADDITIONAL_NAME, DClass.IN, 300, txt);
            original.addRecord(txtRec, Section.ADDITIONAL);
        }
        // Add an OPT record (EDNS0) as the last additional record.
        original.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);

        // Serialize to wire format.
        byte[] wire = original.toWire();
        assertNotNull(wire);
        assertTrue(wire.length > 0, "Wire data should not be empty");

        // Deserialize and compare.
        Message parsed = new Message(wire);

        // Header ID must survive the round‑trip.
        assertEquals(original.getHeader().getID(), parsed.getHeader().getID());

        // Question section must contain the same record.
        List<Record> parsedQuestion = parsed.getSection(Section.QUESTION);
        assertEquals(1, parsedQuestion.size());
        assertEquals(question, parsedQuestion.get(0));

        // Additional section must contain exactly the records we added (order preserved).
        List<Record> parsedAdditional = parsed.getSection(Section.ADDITIONAL);
        // 5 TXT records + 1 OPT record = 6
        assertEquals(6, parsedAdditional.size());

        // Verify the first additional record is a TXTRecord and matches the original.
        assertTrue(parsedAdditional.get(0) instanceof TXTRecord);
        assertEquals("sample-1", ((TXTRecord) parsedAdditional.get(0)).getStrings().get(0));

        // Verify the last additional record is the OPTRecord.
        assertTrue(parsedAdditional.get(5) instanceof OPTRecord);
    }

    /**
     * Ensure that {@code toWire()} produces a byte array whose length matches
     * the value reported by {@link Message#numBytes()} after serialization.
     */
    @Test
    void toWire_byteArrayLengthMatchesNumBytes() throws IOException {
        Message msg = new Message(0xABCD);
        // Add a few records to make the message non‑trivial.
        msg.addRecord(Record.newRecord(QUESTION_NAME, Type.AAAA, DClass.IN), Section.QUESTION);
        msg.addRecord(new TXTRecord(ADDITIONAL_NAME, DClass.IN, 60, "hello"), Section.ADDITIONAL);

        byte[] wire = msg.toWire();
        int reported = msg.numBytes();

        assertEquals(reported, wire.length,
                "Message.numBytes() should report the exact length of the wire array");
    }
}
```