package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class TSIG_apply_2_Test {

    /**
     * A deterministic key used for all tests.
     */
    private static TSIG tsigKey;

    /**
     * The query message that carries the original TSIG record.
     */
    private Message queryMessage;

    /**
     * The TSIG record extracted from {@link #queryMessage}.
     */
    private TSIGRecord queryTSIG;

    @BeforeAll
    static void setupBeforeAll() {
        /* Use the SHA‑256 algorithm (the most common choice) and a short,
         * base‑64‑compatible key.  The constructor validates the arguments,
         * so if this succeeds we know the key is usable for the tests. */
        tsigKey = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");
    }

    @BeforeEach
    void setupBeforeEach() throws IOException, TextParseException {
        /* Build a minimal query (A record for www.example.) and sign it.
         * The signed query is then parsed back so that we obtain a genuine
         * {@link TSIGRecord} instance that would be present on a real
         * request. */
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        queryMessage.setTSIG(tsigKey);
        // Serialize and parse back to obtain the TSIGRecord produced by the library.
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        queryTSIG = parsed.getTSIG();
        // Sanity check – the query must be signed for the subsequent tests.
        assertNotNull(queryTSIG, "The generated query should contain a TSIG record");
        assertTrue(parsed.isSigned(), "Parsed query should report as signed");
    }

    /**
     * Create a TSIGRecord with the supplied parameters.
     */
    private static TSIGRecord buildTSIGRecord(Name algorithm, long timeSigned, byte[] mac, byte[] otherData) {
        // name of the TSIG RR is always the owner name of the key (example.)
        Name owner = Name.fromString("example.");
        return new TSIGRecord(owner, DClass.ANY, 0L, algorithm, timeSigned, (int) TSIG.FUDGE.getSeconds(), mac, otherData, new byte[0]);
    }

    /**
     * Build a TSIGRecord that deliberately lacks a MAC (mac == null).
     */
    private static TSIGRecord buildTSIGRecordWithoutMac(Name algorithm, long timeSigned) {
        return buildTSIGRecord(algorithm, timeSigned, null, new byte[0]);
    }

    /**
     * Build a TSIGRecord with an unsupported algorithm name.
     */
    private static TSIGRecord buildTSIGRecordWithUnsupportedAlg(long timeSigned) {
        Name unknownAlg = Name.fromString("unknown-alg.");
        return buildTSIGRecord(unknownAlg, timeSigned, new byte[10], new byte[0]);
    }

    /**
     * Build a TSIGRecord whose secret key length is zero – simulated by an
     *  empty secret in the TSIG key used to sign the message.
     */
    private static TSIG createTSIGWithEmptySecret() {
        // empty secret
        return new TSIG(TSIG.HMAC_SHA256, "example.", "");
    }

    /**
     * Verify that {@code apply(..., true)} (full‑signature) adds a TSIG record
     * to the response and marks the message as signed.
     */
    @Test
    void applyAddsFullSignatureTSIG() throws IOException {
        // Create a response that mirrors the query ID and contains the original question.
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // Apply the TSIG using the “full‑signature” flag.
        tsigKey.apply(response, queryTSIG, true);
        // The response must now be signed and contain a TSIG record.
        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTSIG = response.getTSIG();
        assertNotNull(respTSIG, "Response should contain a TSIG record");
    }

    /**
     * Verify that {@code apply(..., false)} (partial‑signature) also adds a
     * TSIG record and marks the message as signed.
     */
    @Test
    void applyAddsPartialSignatureTSIG() throws IOException {
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // Apply the TSIG using the “partial‑signature” flag.
        tsigKey.apply(response, queryTSIG, false);
        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTSIG = response.getTSIG();
        assertNotNull(respTSIG, "Response should contain a TSIG record");
    }

    /**
     * Ensure that applying a TSIG with a {@code null} previous record does not
     * throw an exception and still signs the message.  The library treats a
     * missing old record as a “new” transaction.
     */
    @Test
    void applyWithNullOldRecordSignsMessage() throws IOException {
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // The method overload that accepts a null old record is legal; we test the
        // three‑argument version directly with a null reference.
        tsigKey.apply(response, null, true);
        assertTrue(response.isSigned(), "Response should be signed even when old TSIG is null");
        assertNotNull(response.getTSIG(), "A TSIG record should be generated");
    }

    /**
     * Group: Valid inputs – normal signature (partial)
     */
    @Test
    void validInputsNormalSignature() throws IOException {
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, queryTSIG, false);
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig, "TSIG record must be present");
        assertEquals(Rcode.NOERROR, tsig.getError(), "Error code should be NOERROR");
        // default truncated MAC length is 10 bytes for HMAC‑SHA256
        assertEquals(10, tsig.getMac().length, "MAC should be truncated to the default 10‑byte length");
    }

    /**
     * Group: Valid inputs – full signature
     */
    @Test
    void validInputsFullSignature() throws IOException {
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, queryTSIG, true);
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig, "TSIG record must be present");
        assertEquals(Rcode.NOERROR, tsig.getError(), "Error code should be NOERROR");
        // full MAC length for SHA‑256 is 32 bytes
        assertEquals(32, tsig.getMac().length, "Full‑signature must contain the complete 32‑byte MAC");
    }

    /**
     * Group: Old TSIGRecord is null – generate new record
     */
    @Test
    void oldRecordNullGeneratesNewTSIG() throws IOException {
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, null, false);
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig, "A new TSIG record must be generated");
        assertEquals(Rcode.NOERROR, tsig.getError(), "Error should be NOERROR");
    }

    /**
     * Group: Null Message argument – expect NullPointerException
     */
    @Test
    void nullMessageThrowsNPE() {
        assertThrows(NullPointerException.class, () -> tsigKey.apply(null, queryTSIG, false), "Applying to a null Message should throw NPE");
    }

    /**
     * Group: Unsupported algorithm in old TSIGRecord
     */
    @Test
    void unsupportedAlgorithmThrowsIllegalArgument() throws IOException {
        TSIGRecord unknownAlgRecord = buildTSIGRecordWithUnsupportedAlg(Instant.now().getEpochSecond());
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tsigKey.apply(response, unknownAlgRecord, false));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupported algorithm"), "Exception message should mention unsupported algorithm");
    }

    /**
     * Group: Non‑zero error code handling – use overloaded apply
     */
    @Test
    void nonZeroErrorCodeIsPropagated() throws IOException {
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // use the overload that accepts an explicit error code
        tsigKey.apply(response, Rcode.FORMERR, queryTSIG);
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig, "TSIG must be present");
        assertEquals(Rcode.FORMERR, tsig.getError(), "TSIG error field must reflect the supplied non‑zero error code");
    }

    /**
     * Group: Full signature requested but old record lacks MAC
     */
    @Test
    void fullSignatureWithMissingMacThrowsIllegalState() throws IOException {
        TSIGRecord noMac = buildTSIGRecordWithoutMac(TSIG.HMAC_SHA256, Instant.now().getEpochSecond());
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> tsigKey.apply(response, noMac, true));
        assertTrue(ex.getMessage().toLowerCase().contains("mac missing"), "Exception should mention missing MAC");
    }

    /**
     * Group: Fudge time window exceeded – future timestamp
     */
    @Test
    void fudgeWindowExceededFutureTimestamp() throws IOException {
        long future = Instant.now().plusSeconds(TSIG.FUDGE.getSeconds() + 1).getEpochSecond();
        TSIGRecord futureTSIG = buildTSIGRecord(TSIG.HMAC_SHA256, future, new byte[10], new byte[0]);
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, futureTSIG, false);
        TSIGRecord result = response.getTSIG();
        assertEquals(Rcode.BADTIME, result.getError(), "Timestamp beyond fudge window must yield BADTIME");
    }

    /**
     * Group: Fudge time window exceeded – past timestamp
     */
    @Test
    void fudgeWindowExceededPastTimestamp() throws IOException {
        long past = Instant.now().minusSeconds(TSIG.FUDGE.getSeconds() + 1).getEpochSecond();
        TSIGRecord pastTSIG = buildTSIGRecord(TSIG.HMAC_SHA256, past, new byte[10], new byte[0]);
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, pastTSIG, false);
        TSIGRecord result = response.getTSIG();
        assertEquals(Rcode.BADTIME, result.getError(), "Timestamp before fudge window must yield BADTIME");
    }

    /**
     * Group: Fudge boundary – exactly at upper limit (+300s)
     */
    @Test
    void fudgeBoundaryUpperLimit() throws IOException {
        long upper = Instant.now().plusSeconds(TSIG.FUDGE.getSeconds()).getEpochSecond();
        TSIGRecord tsig = buildTSIGRecord(TSIG.HMAC_SHA256, upper, new byte[10], new byte[0]);
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, tsig, false);
        TSIGRecord result = response.getTSIG();
        assertEquals(Rcode.NOERROR, result.getError(), "Timestamp exactly at +FUDGE must be accepted");
    }

    /**
     * Group: Fudge boundary – exactly at lower limit (‑300s)
     */
    @Test
    void fudgeBoundaryLowerLimit() throws IOException {
        long lower = Instant.now().minusSeconds(TSIG.FUDGE.getSeconds()).getEpochSecond();
        TSIGRecord tsig = buildTSIGRecord(TSIG.HMAC_SHA256, lower, new byte[10], new byte[0]);
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, tsig, false);
        TSIGRecord result = response.getTSIG();
        assertEquals(Rcode.NOERROR, result.getError(), "Timestamp exactly at -FUDGE must be accepted");
    }

    /**
     * Group: Message already contains a TSIG – replacement
     */
    @Test
    void existingTSIGIsReplaced() throws IOException {
        // first, create a response that already has a TSIG (using the same key)
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // adds an initial TSIG
        response.setTSIG(tsigKey);
        TSIGRecord original = response.getTSIG();
        assertNotNull(original, "Initial TSIG must exist");
        // now apply again – the old record we pass is the original one
        tsigKey.apply(response, original, false);
        TSIGRecord after = response.getTSIG();
        assertNotNull(after, "A TSIG must still be present after replacement");
        assertNotSame(original, after, "The TSIG record should have been replaced");
        // ensure only one TSIG exists
        assertEquals(1, response.getSectionArray(Section.ADDITIONAL).length, "Message must contain exactly one TSIG record");
    }

    /**
     * Group: FullSignature false – MAC truncation
     */
    @Test
    void macTruncationWhenFullSignatureFalse() throws IOException {
        // Build an old record with a full‑length SHA‑1 MAC (20 bytes)
        byte[] fullMac = new byte[20];
        TSIGRecord old = buildTSIGRecord(TSIG.HMAC_SHA1, Instant.now().getEpochSecond(), fullMac, new byte[0]);
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, old, false);
        TSIGRecord result = response.getTSIG();
        assertEquals(10, result.getMac().length, "When fullSignature is false the MAC must be truncated to 10 bytes");
    }

    /**
     * Group: FullSignature true – full MAC retained
     */
    @Test
    void fullMacRetainedWhenFullSignatureTrue() throws IOException {
        byte[] fullMac = new byte[20];
        TSIGRecord old = buildTSIGRecord(TSIG.HMAC_SHA1, Instant.now().getEpochSecond(), fullMac, new byte[0]);
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, old, true);
        TSIGRecord result = response.getTSIG();
        assertEquals(20, result.getMac().length, "When fullSignature is true the full 20‑byte MAC must be kept");
    }

    /**
     * Group: Empty secret key – illegal argument
     */
    @Test
    void emptySecretKeyThrowsIllegalArgument() {
        TSIG emptyKey = createTSIGWithEmptySecret();
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> emptyKey.apply(response, null, false));
        assertTrue(ex.getMessage().toLowerCase().contains("secret key"), "Exception should mention secret key length");
    }

    /**
     * Group: Algorithm HMAC‑SHA256 handling – default truncation
     */
    @Test
    void hmacSha256DefaultTruncation() throws IOException {
        // Use an old record that already contains a full‑length SHA‑256 MAC (32 bytes)
        byte[] fullMac = new byte[32];
        TSIGRecord old = buildTSIGRecord(TSIG.HMAC_SHA256, Instant.now().getEpochSecond(), fullMac, new byte[0]);
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, old, false);
        TSIGRecord result = response.getTSIG();
        assertEquals(10, result.getMac().length, "Default truncation for SHA‑256 must produce a 10‑byte MAC");
        assertEquals(TSIG.HMAC_SHA256, result.getAlgorithm(), "Algorithm name must be preserved");
    }

    /**
     * Group: Algorithm HMAC‑SHA256 handling – full signature
     */
    @Test
    void hmacSha256FullSignature() throws IOException {
        byte[] fullMac = new byte[32];
        TSIGRecord old = buildTSIGRecord(TSIG.HMAC_SHA256, Instant.now().getEpochSecond(), fullMac, new byte[0]);
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsigKey.apply(response, old, true);
        TSIGRecord result = response.getTSIG();
        assertEquals(32, result.getMac().length, "Full‑signature must retain the 32‑byte SHA‑256 MAC");
        assertEquals(TSIG.HMAC_SHA256, result.getAlgorithm(), "Algorithm name must be preserved");
    }

    /**
     * Group: Null secret key in TSIGRecord – illegal argument
     */
    @Test
    void nullSecretKeyInRecordThrowsIllegalArgument() throws IOException {
        // The TSIGRecord itself does not store the secret; we simulate the situation
        // by creating a TSIG with a null secret (via reflection) and using it.
        TSIG nullKey = new TSIG(TSIG.HMAC_SHA256, "example.", null);
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> nullKey.apply(response, null, false));
        assertTrue(ex.getMessage().toLowerCase().contains("secret key"), "Exception should mention that the secret key is null");
    }

    /**
     * Group: Null algorithm name in TSIGRecord – illegal argument
     */
    @Test
    void nullAlgorithmNameThrowsIllegalArgument() throws IOException {
        // Build a TSIGRecord with a null algorithm (via reflection)
        TSIGRecord nullAlgRecord;
        try {
            java.lang.reflect.Constructor<TSIGRecord> ctor = TSIGRecord.class.getDeclaredConstructor(Name.class, int.class, long.class, Name.class, long.class, int.class, byte[].class, byte[].class, byte[].class);
            ctor.setAccessible(true);
            nullAlgRecord = ctor.newInstance(Name.fromString("example."), DClass.ANY, 0L, null, Instant.now().getEpochSecond(), (int) TSIG.FUDGE.getSeconds(), new byte[10], new byte[0], new byte[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tsigKey.apply(response, nullAlgRecord, false));
        assertTrue(ex.getMessage().toLowerCase().contains("algorithm"), "Exception should mention null algorithm name");
    }

    /**
     * Group: Invalid secret key length for HMAC‑SHA256 – GeneralSecurityException
     */
    @Test
    void shortKeyForSha256ThrowsGeneralSecurityException() {
        // Create a TSIG key with a deliberately short secret (less than required by JCE)
        // 1‑byte secret
        TSIG shortKey = new TSIG(TSIG.HMAC_SHA256, "example.", "A");
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // The exception is thrown during MAC initialization inside apply()
        assertThrows(GeneralSecurityException.class, () -> shortKey.apply(response, null, false), "A too‑short key for SHA‑256 must cause GeneralSecurityException");
    }

    /**
     * Group: Unsupported MAC algorithm causing JCE failure – GeneralSecurityException
     */
    @Test
    void unsupportedMacAlgorithmThrowsGeneralSecurityException() {
        // Use a known algorithm name that is not available in the test runtime
        // (e.g., HMAC‑SHA384).  The TSIG key is created with that algorithm.
        TSIG unsupported = new TSIG(TSIG.HMAC_SHA384, "example.", "abcd1234");
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        assertThrows(GeneralSecurityException.class, () -> unsupported.apply(response, null, false), "Unavailable MAC algorithm must result in GeneralSecurityException");
    }

    /**
     * Group: Immutable Message – cannot attach TSIG
     */
    @Test
    void immutableMessageThrowsIllegalStateException() throws IOException, NoSuchFieldException, IllegalAccessException {
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // Mark the message as immutable by setting the private "isImmutable" flag via reflection.
        java.lang.reflect.Field immutField = Message.class.getDeclaredField("isImmutable");
        immutField.setAccessible(true);
        immutField.setBoolean(response, true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> tsigKey.apply(response, null, false));
        assertTrue(ex.getMessage().toLowerCase().contains("immutable"), "Exception should mention that the Message is immutable");
    }
}
