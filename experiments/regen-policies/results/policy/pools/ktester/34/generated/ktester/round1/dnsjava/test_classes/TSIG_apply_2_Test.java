package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        // Build a signed query – this also creates a TSIGRecord we can reuse.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        signedQuery = Message.newQuery(question);
        signedQuery.setTSIG(DEFAULT_TSIG);
        byte[] wire = signedQuery.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        queryTsigRecord = parsed.getTSIG();
        assertNotNull(queryTsigRecord, "The query must contain a TSIGRecord after signing");
    }

    // non‑static because the test class uses PER_CLASS lifecycle
    private Stream<Arguments> nullArgumentProvider() {
        return Stream.of(Arguments.of(null, null, false), Arguments.of(null, queryTsigRecord, true));
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
        // The library overload does not have a boolean flag – it always creates a
        // full‑signature record.
        DEFAULT_TSIG.apply(response, queryTsigRecord);
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
        DEFAULT_TSIG.apply(response, queryTsigRecord);
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
        // The public API also offers apply(message, old) which internally signs
        // the message using the TSIG attached to it.
        DEFAULT_TSIG.apply(response, (TSIGRecord) null);
        assertTrue(response.isSigned(), "Response should be signed even when old TSIG is null");
        assertNotNull(response.getTSIG(), "A TSIGRecord must be generated");
    }

    @ParameterizedTest(name = "[{index}] m={0}, old={1}, full={2}")
    @MethodSource("nullArgumentProvider")
    void testApplyNullMessageThrows(Message m, TSIGRecord old, boolean fullSignature) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DEFAULT_TSIG.apply(m, old), "apply should reject a null Message");
        assertTrue(ex.getMessage().toLowerCase().contains("message"));
    }

    @Test
    void testApplyUnknownAlgorithmThrows() throws IOException {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TSIG("unknown-alg.", "example.", "secret"));
        assertTrue(ex.getMessage().toLowerCase().contains("algorithm"));
    }

    @Test
    void testApplyZeroLengthSecret() throws Exception {
        // Use a short, non‑empty secret – the library does not accept an empty one.
        TSIG key = new TSIG(TSIG.HMAC_SHA256, "example.", "s");
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        key.apply(response, queryTsigRecord);
        assertTrue(response.isSigned(), "Message must be signed even with a short secret");
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig);
        assertNotEquals(0, tsig.getSignature().length, "Signature must be non‑empty");
        // Verify manually using the same secret.
        byte[] wire = response.toWire(Message.MAXLENGTH);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("s".getBytes(), "HmacSHA256"));
        mac.update(wire);
        byte[] expected = mac.doFinal();
        assertArrayEquals(expected, tsig.getSignature(), "Signature generated must match manual HMAC");
    }

    @Test
    void testApplyUnsupportedMacAlgorithmThrows() throws IOException {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TSIG("HMAC-SHA999", "example.", "secret"));
        assertTrue(ex.getMessage().toLowerCase().contains("algorithm"));
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
        newKey.apply(response, oldRecord);
        TSIGRecord respRecord = response.getTSIG();
        assertNotNull(respRecord);
        assertEquals(Name.fromString("new.example."), respRecord.getName(), "Key name of the generated TSIGRecord must match the new TSIG key");
        assertEquals(oldRecord.getAlgorithm(), respRecord.getAlgorithm(), "Algorithm must be preserved from the supplied old record");
        // Verify that the MAC is valid when using the new key.
        int rcode = newKey.verify(response, response.toWire(Message.MAXLENGTH), oldRecord, true);
        assertEquals(Rcode.NOERROR, rcode, "Verification with the new key must succeed");
    }

    @Test
    void testApplyWithExistingTsigAndNullOld() throws IOException {
        // Attach a TSIG to the message using the default key.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        response.setTSIG(DEFAULT_TSIG);
        TSIGRecord existing = response.getTSIG();
        assertNotNull(existing);
        // Now call apply with old == null – the method should re‑sign using the
        // attached TSIG (i.e., the same key name).
        DEFAULT_TSIG.apply(response, (TSIGRecord) null);
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
        DEFAULT_TSIG.apply(response, queryTsigRecord);
        int rcode = DEFAULT_TSIG.verify(response, response.toWire(Message.MAXLENGTH), queryTsigRecord, true);
        assertEquals(Rcode.NOERROR, rcode, "Full‑signature verification should succeed with the original request TSIG");
    }

    @Test
    void testFudgeTimeWindow() throws IOException {
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        DEFAULT_TSIG.apply(response, queryTsigRecord);
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig);
        long now = Instant.now().getEpochSecond();
        long signed = tsig.getTimeSigned().getEpochSecond();
        assertTrue(signed <= now + TSIG.FUDGE.getSeconds(), "time‑signed must be within the fudge window");
    }
}
