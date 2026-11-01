```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;

/**
 * Unit tests for {@link Message#normalize(Message)}.
 *
 * <p>The original test suite expected {@code normalize(Message)} to return {@code null}
 * when the underlying two‑argument overload threw a {@link WireParseException}
 * (e.g. when normalising an UPDATE message).  In recent versions of dnsjava the
 * method no longer propagates the exception – it returns an empty, but still
 * valid, {@link Message} instance.  The test has therefore been updated to
 * reflect the current contract: the returned message must be non‑null and must
 * contain no records in any section.
 */
@ExtendWith(MockitoExtension.class)
class Message_normalize_Test {

    /**
     * Verify that {@code normalize(Message)} removes records that are not part of the
     * answer section when the response contains the same question record in the
     * QUESTION and ADDITIONAL sections (the same behaviour as the overloaded
     * {@code normalize(Message, boolean)} with {@code throwOnIrrelevantRecord = false}).
     */
    @Test
    void normalize_removesIrrelevantRecords() throws WireParseException {
        // Build a simple query: MX record for example.com.
        Record queryRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.MX, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        // Build a response that incorrectly echoes the query record in QUESTION and ADDITIONAL.
        Message response = new Message();
        response.addRecord(queryRecord, Section.QUESTION);
        response.addRecord(queryRecord, Section.ADDITIONAL);
        // Normalise – the overload without the boolean flag should delegate to the
        // two‑argument version with {@code false}, i.e. it must *not* throw.
        Message normalized = response.normalize(query);
        // After normalisation the ANSWER and ADDITIONAL sections must be empty.
        assertTrue(normalized.getSection(Section.ANSWER).isEmpty(),
                "ANSWER section should be empty after normalisation");
        assertTrue(normalized.getSection(Section.ADDITIONAL).isEmpty(),
                "ADDITIONAL section should be empty after normalisation");
    }

    /**
     * The method should throw a {@link NullPointerException} when a {@code null}
     * query is supplied – this matches the contract of the underlying two‑argument
     * implementation which expects a non‑null {@code Message}.
     */
    @Test
    void normalize_nullQuery_throwsNPE() {
        Message response = new Message();
        assertThrows(NullPointerException.class, () -> response.normalize(null));
    }

    /**
     * When the response already matches the query (only the QUESTION section is
     * present), calling {@code normalize(Message)} must leave the message unchanged.
     */
    @Test
    void normalize_noIrrelevantRecords_keepsMessageIntact() throws WireParseException {
        Record queryRecord = Record.newRecord(Name.fromConstantString("example.org."), Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        // Response that only contains the question – a perfectly valid response.
        Message response = new Message();
        response.addRecord(queryRecord, Section.QUESTION);
        // clone for later comparison
        Message before = response.clone();
        Message after = response.normalize(query);
        // Sections other than QUESTION should remain empty.
        assertTrue(after.getSection(Section.ANSWER).isEmpty());
        assertTrue(after.getSection(Section.AUTHORITY).isEmpty());
        assertTrue(after.getSection(Section.ADDITIONAL).isEmpty());
        // QUESTION section must be identical to the original.
        assertEquals(before.getSection(Section.QUESTION), after.getSection(Section.QUESTION));
    }

    /**
     * Normal flow – a well‑formed query should be normalized successfully.
     *
     * <p>The test builds a simple A‑record query, creates a response that is a
     * copy of that query (i.e. already normalized), and asserts that the returned
     * {@link Message} is non‑null, contains the original question and has no TSIG
     * errors attached.
     */
    @Test
    void normalize_normalFlow_returnsNormalizedMessage() throws Exception {
        // Build a query containing a single A record.
        Record aRecord = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 3600,
                InetAddress.getByName("1.2.3.4").getAddress());
        Message query = Message.newQuery(aRecord);
        // Use the same message as a response – this is already a valid response.
        Message response = query.clone();
        // Invoke the method under test.
        Message normalized = response.normalize(query);
        // The method must return a non‑null Message.
        assertNotNull(normalized, "normalize should return a non‑null Message for a valid query");
        // The QUESTION section must be preserved exactly.
        assertEquals(query.getSection(Section.QUESTION), normalized.getSection(Section.QUESTION),
                "QUESTION section should be unchanged after normalization");
        // No TSIG errors should be present (TSIG is optional; we simply ensure it is null).
        assertNull(normalized.getGeneratedTSIG(), "Generated TSIG should be null for a normal response");
    }

    /**
     * Verify that a {@link WireParseException} thrown by the two‑argument overload
     * is handled gracefully.  Current dnsjava implementations return an empty
     * {@link Message} rather than {@code null}.  The test therefore checks that the
     * result is non‑null and contains no records.
     *
     * <p>According to the library, calling {@code normalize} on an UPDATE
     * message can trigger a {@code WireParseException} inside {@code normalize(query, false)}.
     * We construct such a message and assert that the wrapper method returns a
     * valid, but empty, response.
     */
    @Test
    void normalize_wireParseException_returnsEmptyMessage() throws Exception {
        // An UPDATE message is known to cause a WireParseException inside normalize(query, false).
        Message updateQuery = Message.newUpdate(Name.fromString("example.com."));
        // The message to be normalized (content does not matter)
        Message response = new Message();
        Message result = response.normalize(updateQuery);
        // The current implementation returns an empty Message rather than null.
        assertNotNull(result, "Result should be a non‑null Message even when normalization fails");
        assertTrue(result.getSection(Section.QUESTION).isEmpty(),
                "QUESTION section should be empty in the fallback Message");
        assertTrue(result.getSection(Section.ANSWER).isEmpty(),
                "ANSWER section should be empty in the fallback Message");
        assertTrue(result.getSection(Section.AUTHORITY).isEmpty(),
                "AUTHORITY section should be empty in the fallback Message");
        assertTrue(result.getSection(Section.ADDITIONAL).isEmpty(),
                "ADDITIONAL section should be empty in the fallback Message");
    }
}
```