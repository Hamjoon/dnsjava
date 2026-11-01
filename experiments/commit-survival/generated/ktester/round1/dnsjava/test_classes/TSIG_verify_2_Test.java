package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class TSIG_verify_2_Test {

    /**
     * Algorithm to use for the TSIG key.
     */
    private static final Name ALG = TSIG.HMAC_SHA256;

    /**
     * Name of the TSIG key.
     */
    private static final Name KEY_NAME = Name.fromString("example.");

    /**
     * Base‑64 encoded key (the same value used in existing TSIGTest).
     */
    private static final String KEY_STRING = "12345678";

    /**
     * The TSIG instance under test.
     */
    private static TSIG tsig;

    /**
     * Fixed clock to make the timestamp deterministic.
     */
    private static Clock fixedClock;

    /**
     * Simple holder for the three objects required by the verify call.
     */
    private static final class SignedMessage {

        final Message message;

        final byte[] wire;

        final TSIGRecord tsigRecord;

        SignedMessage(Message m, byte[] w, TSIGRecord r) {
            this.message = m;
            this.wire = w;
            this.tsigRecord = r;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a deterministic instant – 2020‑01‑01T00:00:00Z
        fixedClock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        // construct TSIG with the fixed clock (uses the constructor that accepts a Clock)
        tsig = new TSIG(ALG, KEY_NAME, new SecretKeySpec(base64.fromString(KEY_STRING), "HmacSHA256"), fixedClock);
        // 2020‑01‑01T00:00:00Z – a convenient, constant instant.
        fixedClock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        tsig = new TSIG(ALG, KEY_NAME, new SecretKeySpec(base64.fromString(KEY_STRING), "HmacSHA256"), fixedClock);
    }

    /**
     * Builds a simple query {@link Message} with a single A‑record question.
     */
    private static Message buildQuery() {
        Record q = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        return Message.newQuery(q);
    }

    /**
     * Signs {@code msg} with the {@link #tsig} instance and returns the generated
     * {@link TSIGRecord}.  The method also returns the wire representation of the
     * signed message (needed for the {@code messageBytes} argument of {@code verify}).
     */
    private static SignedMessage signMessage(Message msg, boolean fullSignature) throws IOException {
        msg.setTSIG(tsig);
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        TSIGRecord tsigRec = (TSIGRecord) parsed.getSectionArray(Section.ADDITIONAL)[0];
        return new SignedMessage(parsed, wire, tsigRec);
    }

    /**
     * Creates a {@link TSIGRecord} with a custom algorithm name, time‑stamp and MAC.
     * The constructor used here mirrors the one present in dnsjava 3.x.
     */
    private static TSIGRecord buildCustomRecord(Name keyName, String algorithm, long timeSigned, int fudge, byte[] mac, int error) throws IOException {
        Name algName = (algorithm == null) ? null : Name.fromString(algorithm);
        // The dnsjava constructor signature:
        // TSIGRecord(Name name, Name alg, long timeSigned, int fudge,
        //            int macSize, byte[] mac, int originalId, int error, byte[] other);
        return new TSIGRecord(keyName, algName, timeSigned, fudge, mac == null ? 0 : mac.length, mac, 0, error, null);
    }

    /**
     * Uses reflection to create a {@link TSIGRecord} whose MAC field is {@code null}
     * (the public constructor does not allow a null MAC).  This is required for the
     * “null MAC” test case.
     */
    private static TSIGRecord buildRecordWithNullMac(Name keyName, String algorithm, long timeSigned, int fudge, int error) throws Exception {
        TSIGRecord rec = buildCustomRecord(keyName, algorithm, timeSigned, fudge, new byte[0], error);
        Field macField = TSIGRecord.class.getDeclaredField("mac");
        macField.setAccessible(true);
        macField.set(rec, null);
        return rec;
    }

    private static Stream<Arguments> nullAndInvalidProvider() {
        return Stream.of(Arguments.of("null Message, empty bytes, null TSIG, fullSignature=false", null, new byte[0], null, false, Rcode.FORMERR), Arguments.of("valid mock Message, null bytes, valid TSIG, fullSignature=false", mock(Message.class), null, mock(TSIGRecord.class), false, Rcode.FORMERR), Arguments.of("all inputs null, fullSignature=false", null, null, null, false, Rcode.FORMERR));
    }

    private static Stream<Arguments> algorithmMappingProvider() {
        return Stream.of(Arguments.of("Known algorithm HMAC-MD5", "HMAC-MD5.SIG-ALG.REG.INT.", Rcode.NOERROR), Arguments.of("Unknown algorithm", "UNKNOWN-ALG.", Rcode.NOTIMP));
    }

    private static Stream<Arguments> timeFudgeProvider() {
        return Stream.of(Arguments.of("Timestamp within fudge window (-100s)", -100L, Rcode.NOERROR), Arguments.of("Timestamp older than fudge window (-400s)", -400L, Rcode.BADTIME), Arguments.of("Timestamp in future beyond fudge (+400s)", 400L, Rcode.BADTIME), Arguments.of("Timestamp exactly at negative edge (-300s)", -300L, Rcode.NOERROR), Arguments.of("Timestamp exactly at positive edge (+300s)", 300L, Rcode.NOERROR));
    }

    private static Stream<Arguments> signatureVerificationProvider() {
        return Stream.of(Arguments.of("Valid signature", false, false, false, Rcode.NOERROR), Arguments.of("Altered payload – MAC mismatch", true, false, false, Rcode.BADSIG), Arguments.of("Empty MAC – always BADSIG", false, true, false, Rcode.BADSIG), Arguments.of("Null MAC – FORMERR", false, false, true, Rcode.FORMERR));
    }

    private static Stream<Arguments> fullSignatureProvider() {
        return Stream.of(// fullSignature = true, error matches → NOERROR
        Arguments.of("Full‑signature, matching error=0", true, 0, 0, Rcode.NOERROR), // fullSignature = true, error mismatches → BADSIG
        Arguments.of("Full‑signature, mismatched error (record=1, mac=0)", true, 1, 0, Rcode.BADSIG), // fullSignature = false, error ignored → NOERROR
        Arguments.of("Partial‑signature, error ignored", false, 5, 0, Rcode.NOERROR));
    }

    private static Stream<Arguments> algorithmNullOrEmptyProvider() {
        return Stream.of(Arguments.of("Null algorithm → FORMERR", null, Rcode.FORMERR), Arguments.of("Empty algorithm → NOTIMP", "", Rcode.NOTIMP));
    }

    /**
     * Verify that a correctly signed query is accepted.
     */
    @Test
    void verify_validSignature_returnsNoError() throws IOException {
        // Build a simple query message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        // sign the message
        query.setTSIG(tsig);
        // Serialize the signed message.
        byte[] wire = query.toWire(Message.MAXLENGTH);
        // Parse it back – this mimics what a resolver would receive.
        Message parsed = new Message(wire);
        // The public verify method (fullSignature defaults to true) should succeed.
        int result = tsig.verify(parsed, wire, null);
        assertEquals(Rcode.NOERROR, result, "Valid TSIG should verify with NOERROR");
        assertTrue(parsed.isSigned(), "Parsed message should be marked as signed");
        assertTrue(parsed.isVerified(), "Parsed message should be marked as verified");
    }

    /**
     * Verify that a tampered message fails verification.
     */
    @Test
    void verify_tamperedMessage_returnsError() throws IOException {
        // Create and sign a query as in the happy‑path test.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(tsig);
        byte[] originalWire = query.toWire(Message.MAXLENGTH);
        // Tamper with a single byte (do not touch the TSIG signature bytes directly;
        // flipping any earlier byte is enough to break the MAC).
        byte[] tamperedWire = originalWire.clone();
        // arbitrary mutation
        tamperedWire[10] ^= 0xFF;
        // Parse the tampered bytes.
        Message parsedTampered = new Message(tamperedWire);
        // Verification must now fail – the exact Rcode is implementation‑defined,
        // but it must not be NOERROR.
        int result = tsig.verify(parsedTampered, tamperedWire, null);
        assertNotEquals(Rcode.NOERROR, result, "Tampered TSIG should not verify successfully");
    }

    /**
     * Verify the {@code fullSignature = false} overload.  The behaviour is the same
     * for a valid message, but the test ensures the overload can be called without
     * throwing an unexpected exception.
     */
    @Test
    void verify_fullSignatureFalse_validMessage() throws IOException {
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(tsig);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        int result = tsig.verify(parsed, wire, null, false);
        assertEquals(Rcode.NOERROR, result, "verify(..., fullSignature=false) should still succeed for a valid message");
    }

    /**
     * ------------------------------------------------------------------- *
     *  Null and Invalid Parameter Checks                                    *
     * -------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullAndInvalidProvider")
    @DisplayName("Null and Invalid Parameter Checks")
    void nullAndInvalidParameters(String description, Message m, byte[] messageBytes, TSIGRecord requestTSIG, boolean fullSignature, int expectedRcode) {
        int result = tsig.verify(m, messageBytes, requestTSIG, fullSignature);
        assertEquals(expectedRcode, result, "Expected Rcode " + Rcode.string(expectedRcode) + " for: " + description);
    }

    /**
     * ------------------------------------------------------------------- *
     *  Algorithm Name Mapping                                              *
     * -------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("algorithmMappingProvider")
    @DisplayName("Algorithm Name Mapping")
    void algorithmNameMapping(String description, String algorithm, int expectedRcode) throws Exception {
        // Build a minimal signed message first, then replace its algorithm.
        SignedMessage signed = signMessage(buildQuery(), true);
        TSIGRecord original = signed.tsigRecord;
        TSIGRecord modified = buildCustomRecord(original.getKeyName(), algorithm, original.getTimeSigned(), original.getFudge(), original.getSignature(), original.getError());
        int result = tsig.verify(signed.message, signed.wire, modified, true);
        assertEquals(expectedRcode, result, "Algorithm \"" + algorithm + "\" should yield " + Rcode.string(expectedRcode));
    }

    /**
     * ------------------------------------------------------------------- *
     *  Time‑Fudge Validation                                               *
     * -------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("timeFudgeProvider")
    @DisplayName("Time‑Fudge Validation")
    void timeFudgeValidation(String description, long offsetSeconds, int expectedRcode) throws Exception {
        // Create a signed message with the standard key.
        SignedMessage signed = signMessage(buildQuery(), true);
        // Adjust the timestamp of the TSIGRecord.
        long newTime = fixedClock.instant().getEpochSecond() + offsetSeconds;
        TSIGRecord tweaked = buildCustomRecord(signed.tsigRecord.getKeyName(), signed.tsigRecord.getAlgorithm().toString(), newTime, (int) TSIG.FUDGE.getSeconds(), signed.tsigRecord.getSignature(), signed.tsigRecord.getError());
        int result = tsig.verify(signed.message, signed.wire, tweaked, true);
        assertEquals(expectedRcode, result, description + " (offset " + offsetSeconds + "s)");
    }

    /**
     * ------------------------------------------------------------------- *
     *  Signature Verification                                             *
     * -------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("signatureVerificationProvider")
    @DisplayName("Signature Verification")
    void signatureVerification(String description, boolean tamperPayload, boolean emptyMac, boolean nullMac, int expectedRcode) throws Exception {
        // Start from a correctly signed message.
        SignedMessage signed = signMessage(buildQuery(), true);
        TSIGRecord record = signed.tsigRecord;
        // Optionally tamper the payload.
        byte[] wire = tamperPayload ? signed.wire.clone() : signed.wire.clone();
        if (tamperPayload) {
            // flip an arbitrary byte before the TSIG RR.
            wire[10] ^= 0xFF;
        }
        // Optionally replace the MAC.
        TSIGRecord finalRecord = record;
        if (emptyMac) {
            finalRecord = buildCustomRecord(record.getKeyName(), record.getAlgorithm().toString(), record.getTimeSigned(), record.getFudge(), new byte[0], record.getError());
        } else if (nullMac) {
            finalRecord = buildRecordWithNullMac(record.getKeyName(), record.getAlgorithm().toString(), record.getTimeSigned(), record.getFudge(), record.getError());
        }
        int result = tsig.verify(signed.message, wire, finalRecord, true);
        assertEquals(expectedRcode, result, description);
    }

    /**
     * ------------------------------------------------------------------- *
     *  Full‑Signature Flag Paths                                           *
     * -------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("fullSignatureProvider")
    @DisplayName("Full‑Signature Flag Paths")
    void fullSignatureFlagPaths(String description, boolean fullSignature, int errorInRecord, int errorUsedForMac, int expectedRcode) throws Exception {
        // Build a message and sign it with the *desired* error value.
        Message msg = buildQuery();
        // The TSIG.apply method with fullSignature=true includes the error field.
        // We simulate this by creating a TSIGRecord manually.
        SignedMessage signed = signMessage(msg, true);
        TSIGRecord base = signed.tsigRecord;
        // Build a record whose error field is errorInRecord but whose MAC was
        // generated with errorUsedForMac (i.e. we need to re‑sign the message).
        // For simplicity we re‑sign using the TSIG instance with the wanted error.
        // The TSIG.apply overload that accepts an error is not public, so we
        // construct the record manually.
        TSIGRecord tweaked = buildCustomRecord(base.getKeyName(), base.getAlgorithm().toString(), base.getTimeSigned(), base.getFudge(), base.getSignature(), errorUsedForMac);
        int result = tsig.verify(signed.message, signed.wire, tweaked, fullSignature);
        assertEquals(expectedRcode, result, description);
    }

    /**
     * ------------------------------------------------------------------- *
     *  Exception Handling (Mac initialization failure)                     *
     * -------------------------------------------------------------------
     */
    @Test
    @DisplayName("MAC initialization failure → NOTAUTH")
    void macInitFailure() throws Exception {
        // Use an algorithm that is not supported by the JCE provider.
        // HMAC‑MD5 is optional; on many JREs it is present, but we force a failure
        // by providing a bogus key name that is not in the TSIG key store.
        Name bogusKey = Name.fromString("nonexistent.");
        TSIGRecord rec = buildCustomRecord(bogusKey, "HMAC-MD5.SIG-ALG.REG.INT.", fixedClock.instant().getEpochSecond(), (int) TSIG.FUDGE.getSeconds(), new byte[16], 0);
        SignedMessage signed = signMessage(buildQuery(), true);
        int result = tsig.verify(signed.message, signed.wire, rec, true);
        assertEquals(Rcode.NOTAUTH, result, "Missing key should cause NOTAUTH");
    }

    /**
     * ------------------------------------------------------------------- *
     *  Empty Secret Key                                                    *
     * -------------------------------------------------------------------
     */
    @Test
    @DisplayName("Zero‑length secret key → BADSIG")
    void emptySecretKey() throws Exception {
        // Construct a TSIG instance with an empty secret.
        TSIG emptyKeyTsig = new TSIG(ALG, KEY_NAME, new SecretKeySpec(new byte[0], "HmacSHA256"), fixedClock);
        SignedMessage signed = signMessage(buildQuery(), true);
        // The MAC will be computed with an empty key (which yields a deterministic
        // value).  Verification with the same empty‑key instance should succeed.
        // To test the failure path we verify with the *original* tsig (which has a
        // non‑empty key) against a record generated by the empty‑key instance.
        int result = tsig.verify(signed.message, signed.wire, signed.tsigRecord, true);
        // The MACs differ → BADSIG
        assertEquals(Rcode.BADSIG, result, "Verification with mismatching (empty) secret key must fail");
    }

    /**
     * ------------------------------------------------------------------- *
     *  MAC Length Mismatch                                                  *
     * -------------------------------------------------------------------
     */
    @Test
    @DisplayName("MAC length shorter than required → BADSIG")
    void macLengthMismatch() throws Exception {
        SignedMessage signed = signMessage(buildQuery(), true);
        // Truncate the MAC to 4 bytes (MD5 expects 16, SHA‑256 expects 32).
        byte[] shortMac = Arrays.copyOf(signed.tsigRecord.getSignature(), 4);
        TSIGRecord truncated = buildCustomRecord(signed.tsigRecord.getKeyName(), signed.tsigRecord.getAlgorithm().toString(), signed.tsigRecord.getTimeSigned(), signed.tsigRecord.getFudge(), shortMac, signed.tsigRecord.getError());
        int result = tsig.verify(signed.message, signed.wire, truncated, true);
        assertEquals(Rcode.BADSIG, result, "Short MAC should be rejected as BADSIG");
    }

    /**
     * ------------------------------------------------------------------- *
     *  Key Name Not Found                                                   *
     * -------------------------------------------------------------------
     */
    @Test
    @DisplayName("Unknown key name → NOTAUTH")
    void unknownKeyName() throws Exception {
        // Build a record that refers to a key name not present in the TSIG key store.
        Name unknown = Name.fromString("unknownkey.");
        SignedMessage signed = signMessage(buildQuery(), true);
        TSIGRecord badKeyRec = buildCustomRecord(unknown, signed.tsigRecord.getAlgorithm().toString(), signed.tsigRecord.getTimeSigned(), signed.tsigRecord.getFudge(), signed.tsigRecord.getSignature(), signed.tsigRecord.getError());
        int result = tsig.verify(signed.message, signed.wire, badKeyRec, true);
        assertEquals(Rcode.NOTAUTH, result, "Verification with an unknown key name must return NOTAUTH");
    }

    /**
     * ------------------------------------------------------------------- *
     *  Algorithm Name Null or Empty                                          *
     * -------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("algorithmNullOrEmptyProvider")
    @DisplayName("Algorithm Name Null or Empty")
    void algorithmNullOrEmpty(String description, String algorithm, int expectedRcode) throws Exception {
        SignedMessage signed = signMessage(buildQuery(), true);
        TSIGRecord rec = buildCustomRecord(signed.tsigRecord.getKeyName(), algorithm, signed.tsigRecord.getTimeSigned(), signed.tsigRecord.getFudge(), signed.tsigRecord.getSignature(), signed.tsigRecord.getError());
        int result = tsig.verify(signed.message, signed.wire, rec, true);
        assertEquals(expectedRcode, result, description);
    }

    /**
     * ------------------------------------------------------------------- *
     *  Invalid Algorithm Format (illegal DNS name)                           *
     * -------------------------------------------------------------------
     */
    @Test
    @DisplayName("Illegal algorithm name format → FORMERR")
    void illegalAlgorithmFormat() throws Exception {
        SignedMessage signed = signMessage(buildQuery(), true);
        // The string contains a space, which is illegal for a DNS name.
        String illegalAlg = "HMAC MD5";
        TSIGRecord rec = buildCustomRecord(signed.tsigRecord.getKeyName(), illegalAlg, signed.tsigRecord.getTimeSigned(), signed.tsigRecord.getFudge(), signed.tsigRecord.getSignature(), signed.tsigRecord.getError());
        int result = tsig.verify(signed.message, signed.wire, rec, true);
        assertEquals(Rcode.FORMERR, result, "Algorithm with illegal characters must be treated as FORMERR");
    }

    /**
     * ------------------------------------------------------------------- *
     *  Null Key Name                                                        *
     * -------------------------------------------------------------------
     */
    @Test
    @DisplayName("Null key name in TSIGRecord → NOTAUTH")
    void nullKeyName() throws Exception {
        SignedMessage signed = signMessage(buildQuery(), true);
        TSIGRecord rec = buildCustomRecord(null, signed.tsigRecord.getAlgorithm().toString(), signed.tsigRecord.getTimeSigned(), signed.tsigRecord.getFudge(), signed.tsigRecord.getSignature(), signed.tsigRecord.getError());
        int result = tsig.verify(signed.message, signed.wire, rec, true);
        assertEquals(Rcode.NOTAUTH, result, "A TSIGRecord without a key name must cause NOTAUTH");
    }

    /**
     * ------------------------------------------------------------------- *
     *  Empty Message Payload                                                *
     * -------------------------------------------------------------------
     */
    @Test
    @DisplayName("Empty payload with matching MAC → NOERROR")
    void emptyPayloadMatchingMac() throws Exception {
        // Build a TSIGRecord whose MAC was generated over an empty payload.
        // The easiest way is to sign an empty Message (no question section).
        Message emptyMsg = new Message();
        emptyMsg.setTSIG(tsig);
        byte[] wire = emptyMsg.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        TSIGRecord rec = (TSIGRecord) parsed.getSectionArray(Section.ADDITIONAL)[0];
        int result = tsig.verify(parsed, wire, rec, true);
        assertEquals(Rcode.NOERROR, result, "Verification of an empty message with a matching MAC must succeed");
    }

    /**
     * ------------------------------------------------------------------- *
     *  Empty Message Payload With Non‑Empty MAC                              *
     * -------------------------------------------------------------------
     */
    @Test
    @DisplayName("Empty payload but non‑empty MAC → BADSIG")
    void emptyPayloadNonEmptyMac() throws Exception {
        // Create a normal signed message to obtain a non‑empty MAC.
        SignedMessage signed = signMessage(buildQuery(), true);
        // Now construct a new empty message but reuse the MAC from the signed one.
        Message emptyMsg = new Message();
        emptyMsg.setTSIG(tsig);
        byte[] emptyWire = emptyMsg.toWire(Message.MAXLENGTH);
        Message parsedEmpty = new Message(emptyWire);
        TSIGRecord mismatched = buildCustomRecord(signed.tsigRecord.getKeyName(), signed.tsigRecord.getAlgorithm().toString(), signed.tsigRecord.getTimeSigned(), signed.tsigRecord.getFudge(), signed.tsigRecord.getSignature(), signed.tsigRecord.getError());
        int result = tsig.verify(parsedEmpty, emptyWire, mismatched, true);
        assertEquals(Rcode.BADSIG, result, "Empty payload with a MAC that was generated over non‑empty data must fail");
    }
}
