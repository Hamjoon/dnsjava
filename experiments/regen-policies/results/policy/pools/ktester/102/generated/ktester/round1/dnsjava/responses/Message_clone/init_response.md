```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#clone()}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>The cloned {@link Message} is a distinct object.
 *   <li>The {@link Header} is deep‑copied.
 *   <li>The record sections are deep‑copied (modifying the original does not affect the clone).
 *   <li>TSIG related fields are cloned when present.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_clone_Test {

    /** Shared objects that are reused by several tests. */
    private static Name qname;
    private static Record question;
    private static Record answerRecord;
    private static TSIG tsigKey;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        qname = Name.fromConstantString("www.example.");
        question = Record.newRecord(qname, Type.A, DClass.IN);
        answerRecord = new ARecord(qname, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        // A simple TSIG key (algorithm HMAC-MD5, key "test")
        tsigKey = new TSIG("test.", "hmac-md5.", "test".getBytes());
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /** Verify that {@code clone()} returns a new Message instance. */
    @Test
    void cloneCreatesNewInstance() {
        Message original = new Message();
        Message copy = original.clone();
        assertNotSame(original, copy, "clone should return a different Message object");
    }

    /** Header must be deep‑copied; changes to the original header must not affect the clone. */
    @Test
    void cloneHeaderIsDeepCopy() {
        Message original = new Message(0x1234);
        Header originalHeader = original.getHeader();
        originalHeader.setFlag(Flags.QR); // set a flag on the original header

        Message copy = original.clone();
        Header copyHeader = copy.getHeader();

        // The IDs must be equal (they are copied)
        assertEquals(originalHeader.getID(), copyHeader.getID());

        // The flag we set on the original must NOT be present on the clone
        assertFalse(copyHeader.getFlag(Flags.QR), "Cloned header must not share flag state with original");
        // Conversely, the original still has the flag
        assertTrue(originalHeader.getFlag(Flags.QR));
    }

    /** Sections must be deep‑copied; modifications to the original after cloning must not affect the copy. */
    @Test
    void cloneSectionsAreDeepCopy() {
        Message original = new Message();
        // Populate QUESTION and ANSWER sections
        original.addRecord(question, Section.QUESTION);
        original.addRecord(answerRecord, Section.ANSWER);

        Message copy = original.clone();

        // Verify that the sections contain the same records initially
        assertEquals(original.getSection(Section.QUESTION), copy.getSection(Section.QUESTION));
        assertEquals(original.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));

        // Mutate original: add another record to ANSWER and remove the question
        Record extra = new ARecord(qname, DClass.IN, 0, InetAddress.getLoopbackAddress());
        original.addRecord(extra, Section.ANSWER);
        original.removeRecord(question, Section.QUESTION);

        // The clone must remain unchanged
        List<Record> copyAnswer = copy.getSection(Section.ANSWER);
        assertEquals(1, copyAnswer.size(), "Clone's ANSWER section size must stay unchanged");
        assertEquals(answerRecord, copyAnswer.get(0));

        List<Record> copyQuestion = copy.getSection(Section.QUESTION);
        assertEquals(1, copyQuestion.size(), "Clone's QUESTION section size must stay unchanged");
        assertEquals(question, copyQuestion.get(0));
    }

    /** When a TSIG key is set on the original message, the clone must contain a distinct TSIGRecord. */
    @Test
    void cloneCopiesTsigRecords() throws IOException {
        Message original = new Message();
        // Attach a TSIG to the message (this creates a generated TSIGRecord internally)
        original.setTSIG(tsigKey);
        // Also set a query‑time TSIGRecord via the overloaded method
        original.setTSIG(tsigKey, Rcode.NOERROR, null);

        // Ensure the original actually has TSIG records
        assertNotNull(original.getGeneratedTSIG(), "Original should have a generated TSIG record");
        assertNotNull(original.getTSIG(), "Original should have a query TSIG record");

        Message copy = original.clone();

        // The clone must have its own TSIG objects (different instances)
        TSIGRecord originalGen = original.getGeneratedTSIG();
        TSIGRecord copyGen = copy.getGeneratedTSIG();
        assertNotNull(copyGen);
        assertNotSame(originalGen, copyGen, "Generated TSIGRecord must be cloned, not shared");
        assertEquals(originalGen, copyGen, "Cloned generated TSIGRecord must be equal to original");

        TSIGRecord originalQuery = original.getTSIG();
        TSIGRecord copyQuery = copy.getTSIG();
        assertNotNull(copyQuery);
        assertNotSame(originalQuery, copyQuery, "Query TSIGRecord must be cloned, not shared");
        assertEquals(originalQuery, copyQuery, "Cloned query TSIGRecord must be equal to original");
    }

    /** Cloning a message that has no records or TSIG must still succeed and preserve empty sections. */
    @Test
    void cloneEmptyMessage() {
        Message original = new Message();
        Message copy = original.clone();

        // All sections should be empty but non‑null
        for (int i = 0; i < 4; i++) {
            assertTrue(copy.getSection(i).isEmpty(), "Section " + i + " should be empty after cloning");
        }

        // No TSIG records should be present
        assertNull(copy.getGeneratedTSIG());
        assertNull(copy.getTSIG());
    }
}
```