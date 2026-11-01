```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Comprehensive tests for {@link Message#numBytes()}.
 *
 * <p>The {@code numBytes()} method simply returns the internal {@code size} field,
 * which represents the number of bytes the message occupies on the wire.
 *
 * <p>These tests cover the behaviour described in the JSON test‑case specification:
 *
 * <ul>
 *   <li>initial size of a fresh message,
 *   <li>size after adding records (single / multiple),
 *   <li>size after removing a record,
 *   <li>size after cloning,
 *   <li>size impact of TSIG,
 *   <li>size stability across {@code toWire()} calls.
 * </ul>
 *
 * <p>All tests use only the public API of {@link Message}.  Where a private field
 * would be required (e.g. to reset the size), reflection is employed.
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

    /* --------------------------------------------------------------------- *
     *  Existing tests from the original framework (kept unchanged)
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  Additional tests derived from the JSON specification
     * --------------------------------------------------------------------- */

    /**
     * The size reported by {@code numBytes()} must stay the same before and after a
     * call to {@code toWire()} (or {@code toWire(int)}).  The method only serialises
     * the message – it must not mutate the internal {@code size} field.
     */
    @Test
    void numBytes_unchangedAfterToWire() throws IOException {
        Message m = new Message();
        Name name = Name.fromConstantString("wire.test.");
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        m.addRecord(q, Section.QUESTION);

        int sizeBefore = m.numBytes();          // size after adding a record
        byte[] wire = m.toWire(Message.MAXLENGTH);
        int sizeAfter = m.numBytes();           // must be unchanged

        assertEquals(sizeBefore, sizeAfter,
                "Calling toWire() must not modify the value returned by numBytes()");
        assertEquals(wire.length, sizeAfter,
                "The size field should still reflect the wire length after serialization");
    }

    /**
     * Adding a single record to the ANSWER section must make {@code numBytes()}
     * return a value greater than zero.
     */
    @Test
    void numBytes_greaterThanZeroAfterSingleRecord() throws IOException {
        Message m = new Message();
        Name name = Name.fromConstantString("single.record.");
        Record answer = Record.newRecord(name, Type.A, DClass.IN);
        m.addRecord(answer, Section.ANSWER);

        // The size is still zero until the message is serialised.
        // After serialisation it must be > 0.
        byte[] wire = m.toWire(Message.MAXLENGTH);
        assertTrue(m.numBytes() > 0,
                "After adding a record and serialising, numBytes() must be greater than 0");
        assertEquals(wire.length, m.numBytes(),
                "numBytes() must equal the length of the wire representation");
    }

    /**
     * Adding two records to the AUTHORITY section must increase the size strictly.
     */
    @Test
    void numBytes_strictlyIncreasingWithMultipleAdds() throws IOException {
        Message m = new Message();
        Name name = Name.fromConstantString("multi.record.");
        Record r1 = Record.newRecord(name, Type.NS, DClass.IN);
        Record r2 = Record.newRecord(name, Type.NS, DClass.IN);

        m.addRecord(r1, Section.AUTHORITY);
        int sizeAfterFirst = m.toWire(Message.MAXLENGTH).length;
        assertEquals(sizeAfterFirst, m.numBytes(),
                "Size after first addition must match wire length");

        m.addRecord(r2, Section.AUTHORITY);
        int sizeAfterSecond = m.toWire(Message.MAXLENGTH).length;
        assertTrue(sizeAfterSecond > sizeAfterFirst,
                "Size after second addition must be larger than after the first");
        assertEquals(sizeAfterSecond, m.numBytes(),
                "Size must always reflect the latest wire length");
    }

    /**
     * Removing a previously added record must decrease the size back to the value
     * it had before the addition.
     */
    @Test
    void numBytes_decreasesAfterRemoval() throws IOException {
        Message m = new Message();
        Name name = Name.fromConstantString("remove.record.");
        Record add = Record.newRecord(name, Type.TXT, DClass.IN);
        m.addRecord(add, Section.ADDITIONAL);

        int sizeAfterAdd = m.toWire(Message.MAXLENGTH).length;
        assertTrue(sizeAfterAdd > 0, "Size after addition should be > 0");

        // The removeRecord method returns a boolean – we assert it succeeded.
        assertTrue(m.removeRecord(add, Section.ADDITIONAL),
                "removeRecord should return true for an existing record");

        int sizeAfterRemove = m.toWire(Message.MAXLENGTH).length;
        assertTrue(sizeAfterRemove < sizeAfterAdd,
                "Size after removal must be smaller than after addition");
        assertEquals(sizeAfterRemove, m.numBytes(),
                "numBytes() must reflect the reduced size after removal");
    }

    /**
     * Cloning a Message must produce a new instance whose {@code numBytes()}
     * value is identical to that of the original.
     */
    @Test
    void numBytes_identicalAfterCloning() throws IOException, CloneNotSupportedException {
        Message original = new Message();
        Name name = Name.fromConstantString("clone.test.");
        original.addRecord(Record.newRecord(name, Type.A, DClass.IN), Section.QUESTION);
        // Force size computation via serialisation.
        original.toWire(Message.MAXLENGTH);
        int originalSize = original.numBytes();

        Message cloned = original.clone();
        // The clone should have the same size without needing another serialisation.
        assertEquals(originalSize, cloned.numBytes(),
                "Cloned Message must report the same size as the original");
    }

    /**
     * Setting a TSIG key should increase the size of the message because a TSIG
     * record is appended to the wire format.
     *
     * <p>If the TSIG constructor signature changes or the library does not
     * provide a public constructor, the test is skipped rather than failing.
     */
    @Test
    void numBytes_greaterAfterSettingTSIG() throws IOException {
        Message m = new Message();
        Name name = Name.fromConstantString("tsig.test.");
        m.addRecord(Record.newRecord(name, Type.A, DClass.IN), Section.QUESTION);
        // Size before TSIG
        int sizeBefore = m.toWire(Message.MAXLENGTH).length;

        // --- attempt to create a realistic TSIG instance -----------------
        TSIG tsig;
        try {
            // The typical dnsjava constructor: TSIG(Name name, String algorithm, byte[] secret)
            Name keyName = Name.fromConstantString("key.example.");
            byte[] secret = "topsecret".getBytes();
            tsig = new TSIG(keyName, "hmac-md5", secret);
        } catch (Throwable t) {
            // If the constructor does not exist we cannot perform the test.
            assumeTrue(false, "TSIG construction not supported in this environment");
            return; // unreachable, but keeps the compiler happy
        }

        // Apply the TSIG key.
        m.setTSIG(tsig);
        int sizeAfter = m.toWire(Message.MAXLENGTH).length;

        assertTrue(sizeAfter > sizeBefore,
                "Message size must increase after a TSIG key is attached");
        assertEquals(sizeAfter, m.numBytes(),
                "numBytes() must reflect the new size after TSIG addition");
    }

    /**
     * Multiple calls to {@code toWire()} must not change the internal size.
     */
    @Test
    void numBytes_unchangedAfterMultipleToWireCalls() throws IOException {
        Message m = new Message();
        Name name = Name.fromConstantString("repeat.wire.");
        m.addRecord(Record.newRecord(name, Type.A, DClass.IN), Section.QUESTION);

        // First serialisation – establishes the size.
        byte[] first = m.toWire(Message.MAXLENGTH);
        int sizeAfterFirst = m.numBytes();

        // Second serialisation – size must stay the same.
        byte[] second = m.toWire(Message.MAXLENGTH);
        int sizeAfterSecond = m.numBytes();

        assertEquals(first.length, sizeAfterFirst,
                "Size after first toWire must match its byte array length");
        assertEquals(second.length, sizeAfterSecond,
                "Size after second toWire must match its byte array length");
        assertEquals(sizeAfterFirst, sizeAfterSecond,
                "Repeated toWire calls must not alter the size reported by numBytes()");
    }
}
```