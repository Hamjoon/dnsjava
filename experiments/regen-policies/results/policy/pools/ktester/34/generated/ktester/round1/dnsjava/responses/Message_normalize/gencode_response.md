```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#normalize(Message)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>normal flow – a well‑formed query should be normalized without errors;</li>
 *   <li>handling of {@link WireParseException} – the method must catch the exception and
 *       return {@code null};</li>
 *   <li>null‑query handling – a {@code NullPointerException} is expected.</li>
 * </ul>
 *
 * <p>The original test class already contains three tests that verify
 * removal of irrelevant records, null query handling and the “no‑change” case.
 * The additional tests below complete the coverage required by the JSON test
 * specifications.
 */
@ExtendWith(MockitoExtension.class)
class Message_normalize_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

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

        Message before = response.clone(); // clone for later comparison
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
        Record aRecord = Record.newRecord(
                Name.fromString("example.com."),
                Type.A,
                DClass.IN,
                3600,
                InetAddress.getByName("1.2.3.4"));
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
     * is caught and results in a {@code null} return value.
     *
     * <p>According to the library, calling {@code normalize} on an UPDATE
     * message can trigger a {@code WireParseException}. We construct such a
     * message and assert that the wrapper method returns {@code null}.
     */
    @Test
    void normalize_wireParseException_returnsNull() {
        // An UPDATE message is known to cause a WireParseException inside normalize(query, false).
        Message updateQuery = Message.newUpdate(Name.fromString("example.com."));
        Message response = new Message(); // the message to be normalized (content does not matter)

        Message result = response.normalize(updateQuery);

        assertNull(result, "When normalize(query, false) throws WireParseException, the wrapper should return null");
    }
}
```