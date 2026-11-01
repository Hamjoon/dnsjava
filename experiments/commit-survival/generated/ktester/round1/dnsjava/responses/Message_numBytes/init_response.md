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
 * Tests for {@link Message#numBytes()}.
 *
 * The {@code numBytes()} method simply returns the internal {@code size} field,
 * which represents the number of bytes the message occupies on the wire.
 *
 * The tests verify:
 * <ul>
 *   <li>A newly created empty {@code Message} reports a size of {@code 0}.</li>
 *   <li>When a message is serialized with {@code toWire()}, the size reported by a
 *   newly parsed message equals the length of the wire representation.</li>
 *   <li>Adding records changes the size accordingly (indirectly verified via
 *   {@code toWire()} length).</li>
 * </ul>
 */
class Message_numBytes_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
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
     * Verify that a freshly constructed empty Message reports zero bytes.
     */
    @Test
    void numBytes_emptyMessage_isZero() {
        Message m = new Message(); // uses default Header
        assertEquals(0, m.numBytes(),
                "A newly created Message should have size 0 before any data is added");
    }

    /**
     * Verify that after serialising a Message to wire format, a newly parsed Message
     * reports a size equal to the length of the wire array.
     */
    @Test
    void numBytes_afterWireRoundTrip_matchesWireLength() throws IOException {
        // Build a simple message with a few records.
        Message original = new Message(1234);
        Name name = Name.fromConstantString("example.com.");
        // Add a question record (required for a valid DNS message)
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        original.addRecord(question, Section.QUESTION);
        // Add some additional TXT records.
        original.addRecord(new TXTRecord(name, DClass.IN, 300, "first"), Section.ADDITIONAL);
        original.addRecord(new TXTRecord(name, DClass.IN, 300, "second"), Section.ADDITIONAL);
        // Serialize to wire format (use a sufficiently large max length)
        byte[] wire = original.toWire(Message.MAXLENGTH);
        // Parse the wire back into a Message.
        Message parsed = new Message(wire);
        // The parsed message's size should equal the wire length.
        assertEquals(wire.length, parsed.numBytes(),
                "Parsed Message size should match the length of the wire representation");
    }

    /**
     * Verify that adding records increases the reported size (indirectly via wire length).
     */
    @Test
    void numBytes_increasesWhenRecordsAdded() throws IOException {
        Message m = new Message();
        Name name = Name.fromConstantString("test.example.");
        // No records yet – size should be 0.
        assertEquals(0, m.numBytes());

        // Add a question record.
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        m.addRecord(q, Section.QUESTION);
        // Serialize and check size.
        byte[] wire1 = m.toWire(Message.MAXLENGTH);
        assertEquals(wire1.length, m.numBytes(),
                "Size after adding a question should match wire length");

        // Add an additional TXT record.
        m.addRecord(new TXTRecord(name, DClass.IN, 60, "payload"), Section.ADDITIONAL);
        byte[] wire2 = m.toWire(Message.MAXLENGTH);
        assertTrue(wire2.length > wire1.length,
                "Wire length (and thus size) should increase after adding another record");
        assertEquals(wire2.length, m.numBytes(),
                "Size after adding second record should match new wire length");
    }
}
```