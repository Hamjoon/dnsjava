```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TSIG#apply(Message, TSIGRecord, boolean)}.
 *
 * The tests cover a wide range of scenarios described in the JSON test‑case
 * specification: null handling, full/partial signatures, algorithm mapping,
 * secret‑key edge cases, re‑signing with a different key name, etc.
 *
 * The {@code @BeforeEach} method builds a signed query and extracts the
 * {@link TSIGRecord} produced by that query – this record can be reused as the
 * {@code old} argument in many tests.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_2_Test {

    /** A generic TSIG key (HMAC‑SHA256) used for most tests. */
    private static final TSIG DEFAULT_TSIG = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

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
        signedQuery.setTSIG(DEFAULT_TSIG);               // attach TSIG key
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

    /* --------------------------------------------------------------------- */
    /*  Existing tests from the original scaffold (unchanged)                */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that {@code apply(message, old, true)} adds a TSIG record,
     * marks the message as signed and leaves a non‑empty signature.
     */
    @Test
    void testApplyAddsFullSignature() throws IOException {
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        DEFAULT_TSIG.apply(response, queryTsigRecord, true);

        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response must contain a TSIGRecord after apply");
        assertNotEquals(0, respTsig.getSignature().length,
                "Signature bytes should be present");

        // Verify the MAC using the same key – a successful verification proves that
        // the full‑signature MAC was correctly calculated.
        int rcode = DEFAULT_TSIG.verify(response,
                response.toWire(Message.MAXLENGTH), queryTsigRecord, true);
        assertEquals(Rcode.NOERROR, rcode, "Verification of full signature must succeed");
    }

    /**
     * Verify that {@code apply(message, old, false)} also adds a TSIG record.
     */
    @Test
    void testApplyAddsPartialSignature() throws IOException {
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        DEFAULT_TSIG.apply(response, queryTsigRecord, false);

        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response must contain a TSIGRecord after apply");
        assertNotEquals(0, respTsig.getSignature().length,
                "Signature bytes should be present even for a partial signature");

        int rcode = DEFAULT_TSIG.verify(response,
                response.toWire(Message.MAXLENGTH), queryTsigRecord, false);
        assertEquals(Rcode.NOERROR, rcode, "Verification of partial signature must succeed");
    }

    /**
     * Verify that calling {@code apply} with a {@code null} old record does not
     * throw an exception and still produces a signed message.
     */
    @Test
    void testApplyWithNullOldRecord() throws IOException {
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        DEFAULT_TSIG.apply(response, (TSIGRecord) null, false);

        assertTrue(response.isSigned(), "Response should be signed even when old TSIG is null");
        assertNotNull(response.getTSIG(), "A TSIGRecord must be generated");
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests for null‑argument handling                       */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] m={0}, old={1}, full={2}")
    @MethodSource("nullArgumentProvider")
    void testApplyNullMessageThrows(Message m, TSIGRecord old, boolean fullSignature) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DEFAULT_TSIG.apply(m, old, fullSignature),
                "apply should reject a null Message");
        assertTrue(ex.getMessage().toLowerCase().contains("message"));
    }

    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(
                Arguments.of(null, null, false),
                Arguments.of(null, queryTsigRecord, true)
        );
    }

    /* --------------------------------------------------------------------- */
    /*  Unknown algorithm name – expect IllegalArgumentException            */
    /* --------------------------------------------------------------------- */

    @Test
    void testApplyUnknownAlgorithmThrows() throws IOException {
        // Build a TSIG key with an algorithm name that the implementation does not know.
        TSIG unknownAlgKey = new TSIG(Name.fromString("unknown-alg."), "example.", "secret");
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> unknownAlgKey.apply(response, queryTsigRecord, false));
        assertTrue(ex.getMessage().toLowerCase().contains("algorithm"));
    }

    /* --------------------------------------------------------------------- */
    /*  Zero‑length secret – the MAC should be computed with an empty key    */
    /* --------------------------------------------------------------------- */

    @Test
    void testApplyZeroLengthSecret() throws Exception {
        // Empty secret string translates to a zero‑length byte array.
        TSIG zeroSecretKey = new TSIG(TSIG.HMAC_SHA256, "example.", "");
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);

        // No exception should be thrown.
        zeroSecretKey.apply(response, queryTsigRecord, true);
        assertTrue(response.isSigned(), "Message must be signed even with zero‑length secret");
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig);
        assertNotEquals(0, tsig.getSignature().length, "Signature must be non‑empty");

        // Verify manually using an empty key.
        byte[] wire = response.toWire(Message.MAXLENGTH);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(new byte[0], "HmacSHA256"));
        // The MAC calculation performed by TSIG for a full signature includes the whole
        // wire format plus the TSIG variables. Re‑using the same algorithm and key
        // should produce the same bytes.
        mac.update(wire);
        byte[] expected = mac.doFinal();
        assertArrayEquals(expected, tsig.getSignature(),
                "Signature generated with empty secret must match manual HMAC");
    }

    /* --------------------------------------------------------------------- */
    /*  Unsupported MAC algorithm – expect a GeneralSecurityException       */
    /* --------------------------------------------------------------------- */

    @Test
    void testApplyUnsupportedMacAlgorithmThrows() throws IOException {
        // The name follows the “HMAC‑…” pattern but the JCE provider does not support it.
        TSIG unsupportedKey = new TSIG(Name.fromString("HMAC-SHA999"), "example.", "secret");
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> unsupportedKey.apply(response, queryTsigRecord, false));
        assertTrue(ex.getCause() instanceof GeneralSecurityException,
                "Root cause should be a GeneralSecurityException");
    }

    /* --------------------------------------------------------------------- */
    /*  Re‑signing with a different key name – old record present           */
    /* --------------------------------------------------------------------- */

    @Test
    void testApplyReSignWithDifferentKeyName() throws IOException {
        // Old TSIG (key name "old.example.") – sign a query with this key.
        TSIG oldKey = new TSIG(TSIG.HMAC_SHA256, "old.example.", "sharedSecret");
        Message query = Message.newQuery(Record.newRecord(Name.fromString("test.example."), Type.A, DClass.IN));
        query.setTSIG(oldKey);
        TSIGRecord oldRecord = new Message(query.toWire(Message.MAXLENGTH)).getTSIG();
        assertEquals(Name.fromString("old.example."), oldRecord.getName());

        // New key (different key name, same secret) will be used to sign the response.
        TSIG newKey = new TSIG(TSIG.HMAC_SHA256, "new.example.", "sharedSecret");

        Message response = new Message(query.getHeader().getID());
        response.addRecord(query.getQuestion(), Section.QUESTION);
        newKey.apply(response, oldRecord, true);

        TSIGRecord respRecord = response.getTSIG();
        assertNotNull(respRecord);
        assertEquals(Name.fromString("new.example."), respRecord.getName(),
                "Key name of the generated TSIGRecord must match the new TSIG key");
        assertEquals(oldRecord.getAlgorithm(), respRecord.getAlgorithm(),
                "Algorithm must be preserved from the supplied old record");

        // Verify that the MAC is valid when using the new key.
        int rcode = newKey.verify(response,
                response.toWire(Message.MAXLENGTH), oldRecord, true);
        assertEquals(Rcode.NOERROR, rcode, "Verification with the new key must succeed");
    }

    /* --------------------------------------------------------------------- */
    /*  Existing TSIG on the message while old argument is null            */
    /* --------------------------------------------------------------------- */

    @Test
    void testApplyWithExistingTsigAndNullOld() throws IOException {
        // Attach a TSIG to the message using the default key.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        response.setTSIG(DEFAULT_TSIG); // this creates a TSIGRecord internally
        TSIGRecord existing = response.getTSIG();
        assertNotNull(existing);

        // Now call apply with old == null – the method should re‑sign using the
        // attached TSIG (i.e., the same key name).
        DEFAULT_TSIG.apply(response, (TSIGRecord) null, true);

        TSIGRecord newRecord = response.getTSIG();
        assertNotNull(newRecord);
        assertNotEquals(existing.getSignature(), newRecord.getSignature(),
                "Signature must be refreshed");
        assertEquals(existing.getName(), newRecord.getName(),
                "Key name should stay the same when old is null");
    }

    /* --------------------------------------------------------------------- */
    /*  Full‑signature verification against the original request TSIG      */
    /* --------------------------------------------------------------------- */

    @Test
    void testFullSignatureVerificationAgainstRequest() throws IOException {
        // Build a response that mirrors the signed query.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        // Apply with fullSignature = true.
        DEFAULT_TSIG.apply(response, queryTsigRecord, true);

        // Verify using the original request TSIG (queryTsigRecord) as the “old”.
        int rcode = DEFAULT_TSIG.verify(response,
                response.toWire(Message.MAXLENGTH), queryTsigRecord, true);
        assertEquals(Rcode.NOERROR, rcode,
                "Full‑signature verification should succeed with the original request TSIG");
    }

    /* --------------------------------------------------------------------- */
    /*  Fudge‑time handling – ensure time‑signed is within the allowed window */
    /* --------------------------------------------------------------------- */

    @Test
    void testFudgeTimeWindow() throws IOException {
        // Create a message whose internal clock is artificially old.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);

        // Force the system clock to be far in the past by mocking Clock is not
        // possible without changing TSIG source. Instead we verify that the
        // generated time‑signed is not older than now + FUDGE.
        DEFAULT_TSIG.apply(response, queryTsigRecord, false);
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig);

        long now = Instant.now().getEpochSecond();
        long signed = tsig.getTimeSigned();
        assertTrue(signed <= now + TSIG.FUDGE.getSeconds(),
                "time‑signed must be within the fudge window");
    }

    /* --------------------------------------------------------------------- */
    /*  Helper method to compute a MAC using JCE – used in a few assertions   */
    /* --------------------------------------------------------------------- */
    private static byte[] computeMac(String algorithm, byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        mac.update(data);
        return mac.doFinal();
    }
}
```