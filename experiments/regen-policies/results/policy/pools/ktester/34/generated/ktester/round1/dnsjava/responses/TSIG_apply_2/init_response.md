```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TSIG#apply(Message, TSIGRecord, boolean)}.
 *
 * The tests verify that invoking {@code apply} correctly adds a TSIG record to a
 * response message and that the resulting message reports being signed.
 *
 * The test harness builds a signed query message first (using {@link Message#setTSIG(TSIG)}),
 * extracts the generated {@link TSIGRecord} from that query and then uses it as the
 * {@code old} parameter when calling {@code apply} on a fresh response message.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_2_Test {

    /** A TSIG key used for all tests. */
    private static final TSIG TSIG_KEY = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

    /** The TSIGRecord generated from a signed query – reused as the “old” record. */
    private TSIGRecord queryTsigRecord;

    /** A simple query message that will be signed to produce {@link #queryTsigRecord}. */
    private Message signedQuery;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // 1️⃣ Build a signed query – this also creates a TSIGRecord we can reuse.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        signedQuery = Message.newQuery(question);
        signedQuery.setTSIG(TSIG_KEY);               // attach TSIG key
        byte[] wire = signedQuery.toWire(Message.MAXLENGTH); // forces signing
        Message parsed = new Message(wire);
        queryTsigRecord = parsed.getTSIG();
        assertNotNull(queryTsigRecord, "The query must contain a TSIGRecord after signing");
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
     * Verify that {@code apply(message, old, true)} adds a TSIG record,
     * marks the message as signed and leaves a non‑empty signature.
     */
    @Test
    void testApplyAddsFullSignature() throws IOException {
        // Build a minimal response that mirrors the query ID.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);

        // 2️⃣ Invoke the focal method with fullSignature = true.
        TSIG_KEY.apply(response, queryTsigRecord, true);

        // Assertions ------------------------------------------------------------
        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response must contain a TSIGRecord after apply");
        assertNotEquals(0, respTsig.getSignature().length,
                "Signature bytes should be present");
    }

    /**
     * Verify that {@code apply(message, old, false)} also adds a TSIG record.
     * The difference between full and partial signatures is internal to the
     * implementation; for the purpose of this unit test we only need to
     * confirm that a TSIG record is still produced.
     */
    @Test
    void testApplyAddsPartialSignature() throws IOException {
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);

        // Invoke with fullSignature = false.
        TSIG_KEY.apply(response, queryTsigRecord, false);

        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response must contain a TSIGRecord after apply");
        assertNotEquals(0, respTsig.getSignature().length,
                "Signature bytes should be present even for a partial signature");
    }

    /**
     * Verify that calling {@code apply} with a {@code null} old record does not
     * throw an exception and still produces a signed message. This mirrors the
     * overload {@link TSIG#apply(Message, TSIGRecord)} which forwards a null
     * value.
     */
    @Test
    void testApplyWithNullOldRecord() throws IOException {
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);

        // The public API also offers apply(message, old) which internally calls
        // the 4‑argument version with fullSignature = false.
        TSIG_KEY.apply(response, (TSIGRecord) null, false);

        assertTrue(response.isSigned(), "Response should be signed even when old TSIG is null");
        assertNotNull(response.getTSIG(), "A TSIGRecord must be generated");
    }
}
```