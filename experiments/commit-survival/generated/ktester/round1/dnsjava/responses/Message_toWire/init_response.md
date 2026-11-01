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
 * Tests for {@link Message#toWire(DNSOutput)}.
 *
 * The tests create a {@link Message}, write it to a {@link DNSOutput} using the
 * package‑private {@code toWire} method and then read the resulting byte array
 * back into a new {@link Message}.  The round‑trip verification ensures that the
 * method correctly serialises the header and all populated sections.
 */
class Message_toWire_Test {

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
     * Verify that a message containing records in the QUESTION, ANSWER and
     * ADDITIONAL sections can be written with {@code toWire(DNSOutput)} and
     * read back unchanged.
     */
    @Test
    void testToWireRoundTrip_WithRecords() throws IOException {
        // Build a message with a few records.
        Message original = new Message(0x1234);
        Name qName = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qName, Type.A, DClass.IN);
        original.addRecord(question, Section.QUESTION);

        // ANSWER section – an A record.
        Record aRecord = new ARecord(qName, DClass.IN, 300,
                java.net.InetAddress.getByName("93.184.216.34"));
        original.addRecord(aRecord, Section.ANSWER);

        // ADDITIONAL section – a TXT record and an OPT record.
        Record txt = new TXTRecord(qName, DClass.IN, 300, "sample txt");
        original.addRecord(txt, Section.ADDITIONAL);
        original.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);

        // Write the message using the package‑private toWire(DNSOutput) method.
        DNSOutput out = new DNSOutput();
        original.toWire(out); // package‑private, but accessible from same package.

        // Obtain the raw wire data.
        byte[] wire = out.toByteArray();

        // Re‑parse the wire data.
        Message parsed = new Message(wire);

        // Header ID must survive the round‑trip.
        assertEquals(original.getHeader().getID(), parsed.getHeader().getID(),
                "Header ID should be preserved");

        // Verify that each section contains the same number of records.
        for (int sec = 0; sec < 4; sec++) {
            List<Record> origSec = original.getSection(sec);
            List<Record> parsedSec = parsed.getSection(sec);
            assertEquals(origSec.size(), parsedSec.size(),
                    "Section " + sec + " size mismatch");
            // Verify that the records are of the same concrete type and have equal
            // string representations (a simple proxy for equality without exposing
            // private fields).
            for (int i = 0; i < origSec.size(); i++) {
                Record o = origSec.get(i);
                Record p = parsedSec.get(i);
                assertEquals(o.getClass(), p.getClass(),
                        "Record class mismatch in section " + sec);
                assertEquals(o.toString(), p.toString(),
                        "Record content mismatch in section " + sec);
            }
        }
    }

    /**
     * Verify that an empty message (no records in any section) can be written
     * with {@code toWire(DNSOutput)} without throwing and that the resulting
     * message is still valid.
     */
    @Test
    void testToWireEmptyMessage() throws IOException {
        Message empty = new Message(); // default constructor – empty sections
        DNSOutput out = new DNSOutput();
        empty.toWire(out);
        byte[] wire = out.toByteArray();

        // Re‑parse – should not throw.
        Message parsed = new Message(wire);

        // All sections must be empty.
        for (int sec = 0; sec < 4; sec++) {
            assertTrue(parsed.getSection(sec).isEmpty(),
                    "Section " + sec + " should be empty after round‑trip");
        }

        // Header counts must be zero.
        Header h = parsed.getHeader();
        for (int i = 0; i < 4; i++) {
            assertEquals(0, h.getCount(i), "Header count for section " + i + " should be zero");
        }
    }
}
```