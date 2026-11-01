```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Message#clone()}.
 *
 * The tests verify that:
 * <ul>
 *   <li>The cloned {@link Message} contains the same records and header values as the original.</li>
 *   <li>The internal lists (sections) are deep‑copied – modifications to the original do not affect the clone.</li>
 *   <li>The {@link Header} instance is cloned – changes to the original header do not propagate to the clone.</li>
 * </ul>
 */
class Message_clone_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
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
     * Verify that {@code clone()} creates a deep copy of the section lists.
     * Adding or removing records from the original after cloning must not affect the clone.
     */
    @Test
    void cloneCreatesDeepCopyOfSections() throws UnknownHostException {
        // Build a message with records in all four sections.
        Message original = new Message();
        Name name = Name.fromConstantString("example.com.");

        // QUESTION
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        original.addRecord(q, Section.QUESTION);

        // ANSWER
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        original.addRecord(a, Section.ANSWER);

        // AUTHORITY
        Record ns = Record.newRecord(name, Type.NS, DClass.IN);
        original.addRecord(ns, Section.AUTHORITY);

        // ADDITIONAL
        Record txt = new TXTRecord(name, DClass.IN, 3600, "additional");
        original.addRecord(txt, Section.ADDITIONAL);

        // Clone the message.
        Message clone = original.clone();

        // Verify that each section list is equal but not the same object.
        for (int sec = 0; sec < 4; sec++) {
            List<Record> origSec = original.getSection(sec);
            List<Record> cloneSec = clone.getSection(sec);
            assertEquals(origSec, cloneSec,
                    "Section " + sec + " should contain the same records after cloning");
            assertNotSame(origSec, cloneSec,
                    "Section list object should be a different instance after cloning");
        }

        // Mutate the original: add a new record to ANSWER and remove one from QUESTION.
        Record extraA = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("8.8.8.8"));
        original.addRecord(extraA, Section.ANSWER);
        original.removeRecord(q, Section.QUESTION);

        // The clone must stay unchanged.
        assertEquals(1, clone.getSection(Section.QUESTION).size(),
                "Clone's QUESTION section must remain unchanged");
        assertEquals(1, clone.getSection(Section.ANSWER).size(),
                "Clone's ANSWER section must remain unchanged");
        assertEquals(a, clone.getSection(Section.ANSWER).get(0),
                "Clone's ANSWER record must be the original one");
    }

    /**
     * Verify that the {@link Header} of a cloned message is a separate instance.
     * Changing a flag on the original header must not affect the cloned header.
     */
    @Test
    void cloneHeaderIsIndependent() {
        Message original = new Message(0x1234);
        // Set a flag on the original header.
        original.getHeader().setFlag(Flags.QR);

        // Clone the message.
        Message clone = original.clone();

        // The cloned header must have the same flag state initially.
        assertTrue(clone.getHeader().getFlag(Flags.QR));

        // Change the flag on the original header.
        original.getHeader().unsetFlag(Flags.QR);

        // The cloned header must retain the flag.
        assertTrue(clone.getHeader().getFlag(Flags.QR),
                "Cloned header should retain the flag state independent of the original");
        assertFalse(original.getHeader().getFlag(Flags.QR),
                "Original header flag should have been cleared");
    }

    /**
     * Verify that cloning a message that contains a TSIG (query TSIG) copies the TSIG record.
     * The cloned TSIG objects must not be the same instance as those in the original.
     */
    @Test
    void cloneCopiesTSIGRecords() throws IOException {
        // Prepare a simple query message.
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);

        // Create a TSIG key and associate it with the query.
        // The TSIG constructor used here matches the public API of dnsjava.
        TSIG tsigKey = new TSIG("testkey", "hmac-md5", "secret".getBytes());
        query.setTSIG(tsigKey, Rcode.NOERROR, null); // creates a query TSIG record

        // Ensure the query has a TSIG attached.
        assertNotNull(query.getTSIG(), "Original message should have a TSIG record");

        // Clone the query.
        Message clone = query.clone();

        // Both messages must have a TSIG, but they must be distinct objects.
        assertNotNull(clone.getTSIG(), "Cloned message should also have a TSIG record");
        assertNotSame(query.getTSIG(), clone.getTSIG(),
                "Cloned TSIG record must be a different instance from the original");
        // The contents (key name, MAC algorithm) should be equal.
        assertEquals(query.getTSIG().getName(), clone.getTSIG().getName(),
                "Cloned TSIG should have the same name");
        assertEquals(query.getTSIG().getAlgorithm(), clone.getTSIG().getAlgorithm(),
                "Cloned TSIG should have the same algorithm");
    }

    /**
     * Verify that a cloned response retains the same question and answer section records.
     */
    @Test
    void clonePreservesQuestionAndAnswer() throws UnknownHostException {
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message response = new Message();
        response.getHeader().setFlag(Flags.QR);
        response.addRecord(question, Section.QUESTION);
        Record answer = new ARecord(qname, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        response.addRecord(answer, Section.ANSWER);

        Message clone = response.clone();

        assertEquals(response.getQuestion(), clone.getQuestion(),
                "Cloned message must have the same question record");
        assertEquals(response.getSection(Section.ANSWER), clone.getSection(Section.ANSWER),
                "Cloned message must have the same answer section");
    }
}
```