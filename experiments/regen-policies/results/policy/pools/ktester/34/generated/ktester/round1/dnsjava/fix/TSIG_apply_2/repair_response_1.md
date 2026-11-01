```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// Allows non‑static @MethodSource methods
@TestInstance(Lifecycle.PER_CLASS)
class TSIG_apply_2_Test {

    /**
     * A TSIG key used for all tests.
     */
    private static final TSIG TSIG_KEY = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

    /**
     * The TSIGRecord generated from a signed query – reused as the “old” record.
     */
    private TSIGRecord queryTsigRecord;

    /**
     * A simple query message that will be signed to produce {@link #queryTsigRecord}.
     */
    private Message signedQuery;

    /**
     * A generic TSIG key (HMAC‑SHA256) used for most tests.
     */
    private static final TSIG DEFAULT_TSIG = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // 1️⃣ Build a signed query – this also creates a TSIGRecord we can reuse.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        signedQuery = Message.newQuery(question);
        // attach TSIG key
        signedQuery.setTSIG(DEFAULT_TSIG);
        // forces signing
        byte[] wire = signedQuery.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        // assign to field
        queryTsigRecord = parsed.getTSIG();
        assertNotNull(queryTsigRecord, "The query must contain a TSIGRecord after signing");
    }

    // non‑static because the test class uses PER_CLASS lifecycle
    private Stream<Arguments> nullArgumentProvider() {
        return Stream.of(
                Arguments.of(null, null, false),
                Arguments.of(null, queryTsigRecord, true)
        );
    }

    /* --------------------------------------------------------------------- */
    private static byte[] computeMac(String algorithm, byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        mac.update(data);
        return mac.doFinal();
    }

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
        assertNotEquals(0, respTsig.getSignature().length, "Signature bytes should be present");
        // Verify the MAC using the same key – a successful verification proves that
        // the full‑signature MAC was correctly calculated.
        int rcode = DEFAULT_TSIG.verify(response, response.toWire(Message.MAXLENGTH), queryTsigRecord, true);
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
        assertNotEquals(0, respTsig.getSignature().length, "Signature bytes should be present even for a partial signature");
        int rcode = DEFAULT_TSIG.verify(response, response.toWire(Message.MAXLENGTH), queryTsigRecord, false);
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
        // The public API also offers apply(message, old) which internally calls
        // the 4‑argument version with fullSignature = false.
        TSIG_KEY.apply(response, (TSIGRecord) null, false);
        assertTrue(response.isSigned(), "Response should be signed even when old TSIG is null");
        assertNotNull(response.getTSIG(), "A TSIGRecord must be generated");
    }

    @ParameterizedTest(name = "[{index}] m={0}, old={1}, full={2}")
    @MethodSource("nullArgumentProvider")
    void testApplyNullMessageThrows(Message m, TSIGRecord old, boolean fullSignature) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DEFAULT_TSIG.apply(m, old, fullSignature),
                "apply should reject a null Message");
        assertTrue(ex.getMessage().toLowerCase().contains("message"));
    }

    @Test
    void testApplyUnknownAlgorithmThrows() throws IOException {
        // Build a TSIG key with an algorithm name that the implementation does not know.
        TSIG unknownAlgKey = new TSIG("unknown-alg.", "example.", "secret");
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> unknownAlgKey.apply(response, queryTsigRecord, false));
        assertTrue(ex.getMessage().toLowerCase().contains("algorithm"));
    }

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
        mac.update(wire);
        byte[] expected = mac.doFinal();
        assertArrayEquals(expected, tsig.getSignature(), "Signature generated with empty secret must match manual HMAC");
    }

    @Test
    void testApplyUnsupportedMacAlgorithmThrows() throws IOException {
        // The name follows the “HMAC‑…” pattern but the JCE provider does not support it.
        TSIG unsupportedKey = new TSIG("HMAC-SHA999", "example.", "secret");
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> unsupportedKey.apply(response, queryTsigRecord, false));
        assertTrue(ex.getCause() instanceof GeneralSecurityException,
                "Root cause should be a GeneralSecurityException");
    }

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
        int rcode = newKey.verify(response, response.toWire(Message.MAXLENGTH), oldRecord, true);
        assertEquals(Rcode.NOERROR, rcode, "Verification with the new key must succeed");
    }

    @Test
    void testApplyWithExistingTsigAndNullOld() throws IOException {
        // Attach a TSIG to the message using the default key.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        // this creates a TSIGRecord internally
        response.setTSIG(DEFAULT_TSIG);
        TSIGRecord existing = response.getTSIG();
        assertNotNull(existing);
        // Now call apply with old == null – the method should re‑sign using the
        // attached TSIG (i.e., the same key name).
        DEFAULT_TSIG.apply(response, (TSIGRecord) null, true);
        TSIGRecord newRecord = response.getTSIG();
        assertNotNull(newRecord);
        assertNotEquals(existing.getSignature(), newRecord.getSignature(), "Signature must be refreshed");
        assertEquals(existing.getName(), newRecord.getName(), "Key name should stay the same when old is null");
    }

    @Test
    void testFullSignatureVerificationAgainstRequest() throws IOException {
        // Build a response that mirrors the signed query.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        // Apply with fullSignature = true.
        DEFAULT_TSIG.apply(response, queryTsigRecord, true);
        // Verify using the original request TSIG (queryTsigRecord) as the “old”.
        int rcode = DEFAULT_TSIG.verify(response, response.toWire(Message.MAXLENGTH), queryTsigRecord, true);
        assertEquals(Rcode.NOERROR, rcode, "Full‑signature verification should succeed with the original request TSIG");
    }

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
        long signed = tsig.getTimeSigned().getEpochSecond();
        assertTrue(signed <= now + TSIG.FUDGE.getSeconds(),
                "time‑signed must be within the fudge window");
    }
}
```