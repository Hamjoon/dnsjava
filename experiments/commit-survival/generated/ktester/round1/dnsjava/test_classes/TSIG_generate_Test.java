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
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class TSIG_generate_Test {

    /**
     * TSIG instance under test – HMAC‑SHA256 algorithm, name {@code example.}
     */
    private TSIG tsig;

    /**
     * Message used as the basis for the TSIG generation.
     */
    private Message queryMessage;

    /**
     * Fixed instant used for deterministic signatures.
     */
    private static final Instant FIXED_INSTANT = Instant.parse("2020-01-01T00:00:00Z");

    @Nested
    class NullChecks {

        static Stream<Arguments> nullArgumentsProvider() {
            return Stream.of(// null Message
            Arguments.of(null, new byte[] { 1, 2, 3 }, 0, null, "Message"), // null byte[] key
            Arguments.of(Message.newQuery(Record.newRecord(Name.fromString("a."), Type.A, DClass.IN)), null, 0, null, "key byte[]"), // both non‑null (happy path)
            Arguments.of(Message.newQuery(Record.newRecord(Name.fromString("a."), Type.A, DClass.IN)), new byte[] { 1, 2, 3 }, 0, null, null));
        }

        @ParameterizedTest(name = "[{index}] {4}")
        @MethodSource("nullArgumentsProvider")
        void generate_nullArguments(Message m, byte[] key, int err, TSIGRecord old, String description) {
            if ("Message".equals(description) || "key byte[]".equals(description)) {
                assertThrows(NullPointerException.class, () -> tsig.generate(m, key, err, old), "Expected NPE when " + description + " is null");
            } else {
                // happy path – should not throw
                TSIGRecord rec = tsig.generate(m, key, err, old);
                assertNotNull(rec, "TSIGRecord must be produced for non‑null arguments");
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a deterministic clock so that the generated TSIGRecord always has the same
        // timeSigned value.  This avoids flaky tests caused by the system clock.
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        // Build the TSIG key (algorithm, name, secret).  The secret is a short Base64‑compatible
        // string; the actual value is not important for the test – only that it is non‑null.
        SecretKeySpec secret = new SecretKeySpec(base64.fromString("12345678"), TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
        // Use the constructor that accepts a Clock so that we can inject the fixed clock.
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), secret, fixedClock);
        // Build a simple DNS query message (A record for www.example.).
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        // Build a deterministic clock.
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        // Use a short, non‑null secret – the concrete value is irrelevant.
        SecretKeySpec secret = new SecretKeySpec(base64.fromString("12345678"), TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
        // Create a TSIG that uses HMAC‑SHA256; the algorithm can be changed in individual tests.
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), secret, fixedClock);
        // Build a minimal query message (A record for www.example.).
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    /**
     * Extract the “other data” (previous MAC) field from a {@link TSIGRecord}.
     */
    private static byte[] getOtherData(TSIGRecord rec) throws Exception {
        Field f = TSIGRecord.class.getDeclaredField("other");
        f.setAccessible(true);
        return (byte[]) f.get(rec);
    }

    /**
     * Create a {@link TSIGRecord} with the supplied parameters via reflection.
     */
    private static TSIGRecord makeTSIGRecord(Name name, Name alg, byte[] sig, long timeSigned, short fudge, byte[] other) throws Exception {
        // The constructor signature in dnsjava is:
        // TSIGRecord(Name name, int dclass, long ttl, Name alg, byte[] sig,
        //            long timeSigned, short fudge, byte[] other)
        return (TSIGRecord) TSIGRecord.class.getConstructor(Name.class, int.class, long.class, Name.class, byte[].class, long.class, short.class, byte[].class).newInstance(name, DClass.IN, 0L, alg, sig, timeSigned, fudge, other);
    }

    /**
     * Verify that a normal (no‑error) TSIG record is generated and that the
     * {@code error} field of the record matches the supplied value.
     */
    @Test
    void generate_NoErrorRecord() throws IOException {
        // Convert the message to wire format – this is the byte[] that the TSIG
        // generation routine signs.
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        // Call the overload that does not expose the {@code fullSignature} flag.
        TSIGRecord tsigRecord = tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        assertNotNull(tsigRecord, "generate() should return a TSIGRecord");
        assertEquals(Rcode.NOERROR, tsigRecord.getError(), "TSIGRecord.error must reflect the supplied error code");
        assertNotNull(tsigRecord.getSignature(), "Signature must not be null");
        assertTrue(tsigRecord.getSignature().length > 0, "Signature byte array should contain data");
        assertEquals(FIXED_INSTANT, tsigRecord.getTimeSigned(), "timeSigned should be the fixed instant used for the clock");
    }

    /**
     * Verify that an error code supplied to {@code generate} propagates to the
     * resulting {@link TSIGRecord}.
     */
    @Test
    void generate_WithErrorCode() throws IOException {
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        // arbitrary non‑zero error
        int errorCode = Rcode.FORMERR;
        TSIGRecord tsigRecord = tsig.generate(queryMessage, wire, errorCode, null);
        assertNotNull(tsigRecord, "generate() should return a TSIGRecord even on error");
        assertEquals(errorCode, tsigRecord.getError(), "TSIGRecord.error must match the error code supplied to generate()");
        // Even when an error is indicated, a signature is still produced.
        assertNotNull(tsigRecord.getSignature(), "Signature must be present even on error");
        assertTrue(tsigRecord.getSignature().length > 0, "Signature should contain data");
    }

    /**
     * Verify that providing a previous TSIGRecord (the {@code old} argument)
     * does not cause a {@link NullPointerException} and that the new record
     * is still generated correctly.
     */
    @Test
    void generate_WithPreviousRecord() throws IOException {
        // First generate a baseline TSIGRecord that will act as the "old" record.
        byte[] firstWire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord oldRecord = tsig.generate(queryMessage, firstWire, Rcode.NOERROR, null);
        // Now generate a second TSIGRecord using the previous one.
        byte[] secondWire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord newRecord = tsig.generate(queryMessage, secondWire, Rcode.NOERROR, oldRecord);
        assertNotNull(newRecord, "Second generate() call should return a TSIGRecord");
        assertEquals(Rcode.NOERROR, newRecord.getError(), "Error code should still be NOERROR");
        assertNotNull(newRecord.getSignature(), "Signature must be present");
        assertTrue(newRecord.getSignature().length > 0, "Signature should contain data");
        // The new record's timeSigned should be the same fixed instant (deterministic clock).
        assertEquals(FIXED_INSTANT, newRecord.getTimeSigned(), "timeSigned should be deterministic");
    }

    @Test
    void generate_nonZeroErrorCode_propagated() throws IOException {
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        // arbitrary non‑zero error
        int errorCode = Rcode.FORMERR;
        TSIGRecord rec = tsig.generate(queryMessage, wire, errorCode, null);
        assertEquals(errorCode, rec.getError(), "The generated TSIGRecord must contain the supplied error code");
        assertNotNull(rec.getSignature(), "Signature must still be present");
    }

    @Test
    void generate_nonZeroErrorWithOldRecord_preservesOldMac() throws Exception {
        // create an old record first
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord old = tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        // generate a new record with a non‑zero error and the old record supplied
        int errorCode = 9;
        TSIGRecord newRec = tsig.generate(queryMessage, wire, errorCode, old);
        assertEquals(errorCode, newRec.getError(), "Error code must be propagated even when an old record is supplied");
        // The “other data” field of the new record should contain the old signature
        byte[] other = getOtherData(newRec);
        assertArrayEquals(old.getSignature(), other, "The new record must embed the previous MAC (old signature)");
    }

    @Test
    void generate_oldIsNull_noPreviousMac() throws IOException, Exception {
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        byte[] other = getOtherData(rec);
        assertEquals(0, other.length, "When old==null the other‑data field must be empty");
    }

    @Test
    void generate_oldNotNull_copiesPreviousMac() throws IOException, Exception {
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord old = tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        TSIGRecord rec = tsig.generate(queryMessage, wire, Rcode.NOERROR, old);
        byte[] other = getOtherData(rec);
        assertArrayEquals(old.getSignature(), other, "The other‑data field must contain the previous MAC");
    }

    @Test
    void generate_fullSignatureAlwaysTrue_fullMacReturned() throws IOException {
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        // The signature length for HMAC‑SHA256 is 32 bytes (full length)
        assertEquals(32, rec.getSignature().length, "Full signature must be the complete HMAC output (32 bytes for SHA‑256)");
    }

    @Test
    void generate_algorithm_SHA256_usesSHA256() throws IOException {
        // TSIG already configured with HMAC_SHA256 in beforeEach()
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        assertEquals(TSIG.HMAC_SHA256, rec.getAlgorithm(), "Algorithm name of the generated record must be HMAC‑SHA256");
    }

    @Test
    void generate_algorithm_MD5_usesMD5() throws IOException {
        // Re‑create TSIG with MD5 algorithm
        SecretKeySpec secret = new SecretKeySpec(base64.fromString("12345678"), TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        TSIG md5Tsig = new TSIG(TSIG.HMAC_MD5, Name.fromString("example."), secret, fixedClock);
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord rec = md5Tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        assertEquals(TSIG.HMAC_MD5, rec.getAlgorithm(), "Algorithm name must be HMAC‑MD5 when TSIG is created with that algorithm");
    }

    @Test
    void generate_algorithm_SHA1_usesSHA1() throws IOException {
        // Re‑create TSIG with SHA‑1 algorithm
        SecretKeySpec secret = new SecretKeySpec(base64.fromString("12345678"), TSIG.nameToAlgorithm(TSIG.HMAC_SHA1));
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        TSIG sha1Tsig = new TSIG(TSIG.HMAC_SHA1, Name.fromString("example."), secret, fixedClock);
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord rec = sha1Tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        assertEquals(TSIG.HMAC_SHA1, rec.getAlgorithm(), "Algorithm name must be HMAC‑SHA1 when TSIG is created with that algorithm");
    }

    @Test
    void generate_unsupportedAlgorithm_throwsGeneralSecurityException() {
        // Construct a TSIG with an algorithm name that cannot be mapped.
        Name bogusAlg = Name.fromString("bogus-alg.");
        SecretKeySpec secret = new SecretKeySpec(new byte[] { 1, 2, 3 }, // this will throw IllegalArgumentException
        TSIG.nameToAlgorithm(bogusAlg));
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        // The constructor itself may already fail; we therefore use reflection to bypass it.
        assertThrows(GeneralSecurityException.class, () -> {
            // Force creation via reflection to simulate unsupported algorithm at generate time.
            TSIG tsig = (TSIG) TSIG.class.getConstructor(Name.class, Name.class, SecretKeySpec.class, Clock.class).newInstance(bogusAlg, Name.fromString("example."), secret, fixedClock);
            byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
            tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        }, "Unsupported algorithm should result in GeneralSecurityException");
    }

    @Test
    void generate_emptyKey_producesRecord() throws IOException {
        // Empty secret (zero‑length key)
        SecretKeySpec emptySecret = new SecretKeySpec(new byte[0], TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
        TSIG emptyKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), emptySecret, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord rec = emptyKeyTsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        assertNotNull(rec, "Record must be generated even with an empty key");
        assertNotNull(rec.getSignature(), "Signature must be present");
        assertTrue(rec.getSignature().length > 0, "Signature should not be empty");
    }

    @Test
    void generate_largeKey_256Bytes_works() throws IOException {
        byte[] largeKey = new byte[256];
        for (int i = 0; i < largeKey.length; i++) {
            largeKey[i] = (byte) i;
        }
        SecretKeySpec largeSecret = new SecretKeySpec(largeKey, TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
        TSIG largeKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), largeSecret, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord rec = largeKeyTsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        assertNotNull(rec, "Record must be generated with a 256‑byte key");
        assertNotNull(rec.getSignature());
    }

    @Test
    void generate_emptyMessage_producesRecord() throws IOException {
        // no sections
        Message empty = new Message();
        byte[] wire = empty.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsig.generate(empty, wire, Rcode.NOERROR, null);
        assertNotNull(rec);
        assertNotNull(rec.getSignature());
    }

    @Test
    void generate_messageWithOnlyQuestion_usesQuestionOnly() throws IOException {
        Record q = Record.newRecord(Name.fromString("only.example."), Type.A, DClass.IN);
        Message onlyQuestion = Message.newQuery(q);
        byte[] wire = onlyQuestion.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsig.generate(onlyQuestion, wire, Rcode.NOERROR, null);
        assertNotNull(rec);
        assertNotNull(rec.getSignature());
    }

    @Test
    void generate_errorCodeMaximumUnsigned16Bit() throws IOException {
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        // 65535
        int max = 0xFFFF;
        TSIGRecord rec = tsig.generate(queryMessage, wire, max, null);
        assertEquals(max, rec.getError(), "Maximum unsigned 16‑bit error code must be stored unchanged");
    }

    @Test
    void generate_errorCodeNegativeValue() throws IOException {
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        int negative = -1;
        TSIGRecord rec = tsig.generate(queryMessage, wire, negative, null);
        assertEquals(negative, rec.getError(), "Negative error codes should be stored verbatim");
    }

    @Test
    void generate_oldRecordWithEmptySignature_embedsEmptyPreviousMac() throws Exception {
        // Build an old record with an empty signature array.
        TSIGRecord old = makeTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256, new byte[0], FIXED_INSTANT.getEpochSecond(), (short) TSIG.FUDGE.getSeconds(), new byte[0]);
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsig.generate(queryMessage, wire, Rcode.NOERROR, old);
        byte[] other = getOtherData(rec);
        assertEquals(0, other.length, "When the old record has an empty signature, the other‑data field must be empty");
    }

    @Test
    void generate_oldRecordWithDifferentAlgorithm_copiesMacOnly() throws Exception {
        // Old record created with MD5 algorithm, new TSIG uses SHA‑256.
        SecretKeySpec md5Secret = new SecretKeySpec(base64.fromString("12345678"), TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
        TSIG md5Tsig = new TSIG(TSIG.HMAC_MD5, Name.fromString("example."), md5Secret, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord old = md5Tsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        // New TSIG (SHA‑256) generates a record using the old MAC.
        TSIGRecord rec = tsig.generate(queryMessage, wire, Rcode.NOERROR, old);
        assertArrayEquals(old.getSignature(), getOtherData(rec), "The previous MAC must be copied unchanged even if algorithms differ");
        assertEquals(TSIG.HMAC_SHA256, rec.getAlgorithm(), "The new record must use the algorithm of the current TSIG instance");
    }

    @Test
    void generate_oldRecordWithNullSignature_throwsNPE() {
        // Mock a TSIGRecord that returns null for getSignature()
        TSIGRecord mockOld = org.mockito.Mockito.mock(TSIGRecord.class);
        org.mockito.Mockito.when(mockOld.getSignature()).thenReturn(null);
        org.mockito.Mockito.when(mockOld.getName()).thenReturn(Name.fromString("example."));
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        assertThrows(NullPointerException.class, () -> tsig.generate(queryMessage, wire, Rcode.NOERROR, mockOld), "Null signature in old record should cause NPE");
    }

    @Test
    void generate_oldRecordWithNullName_throwsNPE() {
        // Mock a TSIGRecord that returns null for getName()
        TSIGRecord mockOld = org.mockito.Mockito.mock(TSIGRecord.class);
        org.mockito.Mockito.when(mockOld.getSignature()).thenReturn(new byte[] { 1, 2 });
        org.mockito.Mockito.when(mockOld.getName()).thenReturn(null);
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        assertThrows(NullPointerException.class, () -> tsig.generate(queryMessage, wire, Rcode.NOERROR, mockOld), "Null name in old record should cause NPE");
    }

    @Test
    void generate_messageWithoutAlgorithm_throwsIllegalArgumentException() throws IOException {
        // Create a message that has no TSIG record attached.
        Message noAlgMsg = Message.newQuery(Record.newRecord(Name.fromString("noalg.example."), Type.A, DClass.IN));
        // Ensure the message does not contain a TSIG RR.
        assertNull(noAlgMsg.getTSIG(), "Message must not contain a TSIG record");
        byte[] wire = noAlgMsg.toWire(Message.MAXLENGTH);
        assertThrows(IllegalArgumentException.class, () -> tsig.generate(noAlgMsg, wire, Rcode.NOERROR, null), "Missing algorithm in the message should trigger IllegalArgumentException");
    }

    @Test
    void generate_messageWithEmptyAlgorithmString_throwsGeneralSecurityException() throws IOException {
        // Simulate a TSIGRecord with an empty algorithm string attached to the message.
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("emptyalg.example."), Type.A, DClass.IN));
        // Attach a TSIG RR whose algorithm name is the empty string.
        TSIGRecord emptyAlg = new TSIGRecord(Name.fromString("emptyalg.example."), DClass.IN, 0, Name.fromString(""), new byte[0], FIXED_INSTANT.getEpochSecond(), (short) TSIG.FUDGE.getSeconds(), new byte[0]);
        msg.addRecord(emptyAlg, Section.ADDITIONAL);
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        assertThrows(GeneralSecurityException.class, () -> tsig.generate(msg, wire, Rcode.NOERROR, null), "Empty algorithm string should cause GeneralSecurityException");
    }

    @Test
    void generate_invalidKey_throwsGeneralSecurityException() {
        // Construct a key that is deliberately malformed for SHA‑256 (e.g., null bytes are fine,
        // but we simulate a provider that rejects it by using a zero‑length algorithm name).
        SecretKeySpec badSecret = new SecretKeySpec(new byte[] { 1, 2, 3 }, // invalid algorithm name
        "HmacSHA256-INVALID");
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        TSIG badTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), badSecret, fixedClock);
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        assertThrows(GeneralSecurityException.class, () -> badTsig.generate(queryMessage, wire, Rcode.NOERROR, null), "Invalid key should propagate as GeneralSecurityException");
    }

    @Test
    void generate_timeSignedWithinFudgeWindow() throws IOException {
        // Use a clock that returns the real current time (not the fixed one) for this test.
        Clock realClock = Clock.systemUTC();
        SecretKeySpec secret = new SecretKeySpec(base64.fromString("12345678"), TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
        TSIG realTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), secret, realClock);
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        TSIGRecord rec = realTsig.generate(queryMessage, wire, Rcode.NOERROR, null);
        Instant now = Instant.now();
        Instant signed = rec.getTimeSigned();
        Duration diff = Duration.between(signed, now).abs();
        assertTrue(diff.compareTo(TSIG.FUDGE) <= 0, "timeSigned must be within the FUDGE window of the current time");
    }
}
