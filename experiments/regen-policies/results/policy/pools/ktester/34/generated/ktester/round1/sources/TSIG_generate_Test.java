package org.xbill.DNS;

import java.time.Clock;
import java.time.Instant;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Random;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TSIG_generate_Test {

    /**
     * Fixed instant used for deterministic TSIG timestamps.
     */
    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

    /**
     * The TSIG instance under test – HMAC‑SHA256 algorithm with a known key.
     */
    private TSIG tsig;

    /**
     * A simple DNS query message used as the basis for TSIG generation.
     */
    private Message queryMessage;

    /**
     * Wire representation of {@link #queryMessage} (without TSIG).
     */
    private byte[] queryWire;

    /**
     * Supported algorithm names (as they appear in TSIG records).
     */
    public static final String HMAC_SHA256 = "hmac-sha256.";

    public static final String HMAC_SHA1 = "hmac-sha1.";

    public static final String HMAC_SHA384 = "hmac-sha384.";

    public static final String HMAC_MD5 = "hmac-md5.";

    /**
     * Maximum time‑skew allowed (RFC‑2845).
     */
    public static final Duration FUDGE = Duration.ofSeconds(300);

    /**
     * Minimum key length for MD5 (the RFC requires ≥ 8 bytes).
     */
    private static final int MIN_MD5_KEY = 8;

    /**
     * Mapping from TSIG algorithm name to Java JCE algorithm name.
     */
    private static final Map<String, String> JAVA_MAC = new HashMap<>();

    /**
     * The algorithm name used for this TSIG object (e.g. "hmac-sha256.").
     */
    private final String algorithm;

    /**
     * The key name (owner name of the TSIG record).
     */
    private final Name name;

    /**
     * The binary secret key.
     */
    private final byte[] key;

    /**
     * Clock used to obtain the current instant – injected in tests.
     */
    private Clock clock = Clock.systemUTC();

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Use a deterministic clock so that the generated TSIGRecord has a known
        // timeSigned value.
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        // Construct a TSIG with the HMAC‑SHA256 algorithm, name "example.", and a short key.
        // The constructor that accepts a {@link Clock} is package‑private, therefore we
        // create the object via the public constructor and then replace its clock via
        // reflection (a technique allowed in tests but not part of production code).
        // "secretkey" base64‑encoded
        tsig = new TSIG(TSIG.HMAC_SHA256, "example.", "c2VjcmV0a2V5");
        try {
            java.lang.reflect.Field clockField = TSIG.class.getDeclaredField("clock");
            clockField.setAccessible(true);
            clockField.set(tsig, fixedClock);
        } catch (ReflectiveOperationException e) {
            fail("Failed to inject fixed Clock into TSIG instance", e);
        }
        // Build a minimal query message (type A for www.example.).
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        // Serialize the message – this is the byte array that will be signed.
        queryWire = queryMessage.toWire(Message.MAXLENGTH);
    }

    /**
     * Helper to replace the private {@code clock} field of a {@link TSIG}.
     */
    private static void injectClock(TSIG target, Clock clock) {
        try {
            Field f = TSIG.class.getDeclaredField("clock");
            f.setAccessible(true);
            f.set(target, clock);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a TSIG record for a rendered message.
     *
     * @param m     the DNS message (may be a mock – {@code null} header is tolerated)
     * @param b     the rendered wire format of {@code m}
     * @param error the RCODE to embed in the TSIG record
     * @param old   the TSIG record from the previous message, or {@code null}
     * @return a new {@link TSIGRecord}
     */
    public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old) {
        /* ----- argument validation ----- */
        if (m == null) {
            throw new NullPointerException("Message may not be null");
        }
        if (b == null) {
            throw new NullPointerException("Wire array may not be null");
        }
        if (error < 0 || error > 0xFFFF) {
            throw new IllegalArgumentException("Error code out of range");
        }
        /* ----- old‑record validation (if present) ----- */
        if (old != null) {
            if (old.getAlgorithm() == null) {
                throw new IllegalArgumentException("Old TSIGRecord has null algorithm");
            }
            if (!old.getAlgorithm().equals(this.algorithm)) {
                throw new IllegalArgumentException("Algorithm mismatch between old and new TSIG");
            }
            if (old.getName() == null) {
                throw new IllegalArgumentException("Old TSIGRecord has null name");
            }
            if (!old.getName().equals(this.name)) {
                throw new IllegalArgumentException("Name mismatch between old and new TSIG");
            }
            if (old.getTimeSigned() == null) {
                throw new IllegalArgumentException("Old TSIGRecord has null timeSigned");
            }
        }
        /* ----- time handling ----- */
        Instant now = clock.instant();
        Instant timeSigned;
        int finalError = error;
        if (old != null) {
            Instant oldTime = old.getTimeSigned();
            // Check fudge window
            if (oldTime.isBefore(now.minus(FUDGE)) || oldTime.isAfter(now.plus(FUDGE))) {
                finalError = Rcode.BADTIME;
                // any value is fine when BADTIME
                timeSigned = now;
            } else {
                timeSigned = oldTime.plus(FUDGE);
            }
        } else {
            timeSigned = now;
        }
        /* ----- original message ID ----- */
        int originalId = 0;
        if (m.getHeader() != null) {
            originalId = m.getHeader().getID();
        }
        /* ----- MAC calculation ----- */
        byte[] mac = computeMac(b, (old != null) ? old.getSignature() : null);
        if (!fullSignature) {
            // RFC‑2845 permits a maximum of 10 bytes when truncated
            int trunc = Math.min(10, mac.length);
            byte[] tmp = new byte[trunc];
            System.arraycopy(mac, 0, tmp, 0, trunc);
            mac = tmp;
        }
        /* ----- build the TSIGRecord ----- */
        Name algName = Name.fromString(this.algorithm);
        // Use the constructor that accepts Instant and Duration (fudge)
        return new TSIGRecord(// owner name
        this.name, // class (irrelevant for TSIG)
        DClass.ANY, // TTL (always 0)
        0L, // algorithm name
        algName, // time signed (Instant)
        timeSigned, // fudge (Duration)
        FUDGE, // MAC
        mac, // original message ID
        originalId, // error code
        finalError, // other data (empty)
        new byte[0]);
    }

    /**
     * Same as {@link #generate(Message, byte[], int, TSIGRecord)} but allows the
     * caller to request a truncated MAC.
     *
     * @param fullSignature {@code true} for a full‑length MAC, {@code false}
     *                      for a 10‑byte truncated MAC
     */
    public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old, boolean fullSignature) {
        /* ----- argument validation ----- */
        if (m == null) {
            throw new NullPointerException("Message may not be null");
        }
        if (b == null) {
            throw new NullPointerException("Wire array may not be null");
        }
        if (error < 0 || error > 0xFFFF) {
            throw new IllegalArgumentException("Error code out of range");
        }
        /* ----- old‑record validation (if present) ----- */
        if (old != null) {
            if (old.getAlgorithm() == null) {
                throw new IllegalArgumentException("Old TSIGRecord has null algorithm");
            }
            if (!old.getAlgorithm().equals(this.algorithm)) {
                throw new IllegalArgumentException("Algorithm mismatch between old and new TSIG");
            }
            if (old.getName() == null) {
                throw new IllegalArgumentException("Old TSIGRecord has null name");
            }
            if (!old.getName().equals(this.name)) {
                throw new IllegalArgumentException("Name mismatch between old and new TSIG");
            }
            if (old.getTimeSigned() == null) {
                throw new IllegalArgumentException("Old TSIGRecord has null timeSigned");
            }
        }
        /* ----- time handling ----- */
        Instant now = clock.instant();
        Instant timeSigned;
        int finalError = error;
        if (old != null) {
            Instant oldTime = old.getTimeSigned();
            // Check fudge window
            if (oldTime.isBefore(now.minus(FUDGE)) || oldTime.isAfter(now.plus(FUDGE))) {
                finalError = Rcode.BADTIME;
                // any value is fine when BADTIME
                timeSigned = now;
            } else {
                timeSigned = oldTime.plus(FUDGE);
            }
        } else {
            timeSigned = now;
        }
        /* ----- original message ID ----- */
        int originalId = 0;
        if (m.getHeader() != null) {
            originalId = m.getHeader().getID();
        }
        /* ----- MAC calculation ----- */
        byte[] mac = computeMac(b, (old != null) ? old.getSignature() : null);
        if (!fullSignature) {
            // RFC‑2845 permits a maximum of 10 bytes when truncated
            int trunc = Math.min(10, mac.length);
            byte[] tmp = new byte[trunc];
            System.arraycopy(mac, 0, tmp, 0, trunc);
            mac = tmp;
        }
        /* ----- build the TSIGRecord ----- */
        Name algName = Name.fromString(this.algorithm);
        long timeSignedSec = timeSigned.getEpochSecond();
        short fudge = (short) FUDGE.getSeconds();
        return new // owner name
        // class (irrelevant for TSIG)
        TSIGRecord(// TTL (always 0)
        this.name, // algorithm name
        DClass.ANY, // time signed (seconds since epoch)
        0L, // fudge
        algName, // MAC
        timeSignedSec, // original message ID
        fudge, // error code
        mac, // other data (empty)
        originalId, finalError, new byte[0]);
    }

    /**
     * Compute the HMAC over the wire data, optionally chaining the previous MAC.
     */
    private byte[] computeMac(byte[] wire, byte[] previousMac) {
        try {
            String jceAlg = JAVA_MAC.get(this.algorithm);
            Mac mac = Mac.getInstance(jceAlg);
            mac.init(new SecretKeySpec(this.key, jceAlg));
            mac.update(wire);
            if (previousMac != null) {
                mac.update(previousMac);
            }
            return mac.doFinal();
        } catch (Exception e) {
            // In practice this should never happen with the supported algorithms.
            throw new RuntimeException("Failed to compute MAC", e);
        }
    }

    /**
     * Verify that a normal (no‑error) TSIG record is generated with the expected fields.
     */
    @Test
    void generateNormalTsigRecord() throws Exception {
        // Act: generate a TSIG record for the query message.
        TSIGRecord tsigRecord = tsig.generate(queryMessage, queryWire, Rcode.NOERROR, null);
        // Assert: the record is non‑null and contains the expected metadata.
        assertNotNull(tsigRecord, "TSIGRecord should not be null");
        assertEquals(TSIG.HMAC_SHA256, tsigRecord.getAlgorithm(), "Algorithm name mismatch");
        assertEquals(Name.fromString("example."), tsigRecord.getName(), "TSIG key name mismatch");
        assertEquals(Rcode.NOERROR, tsigRecord.getError(), "Error code should be NOERROR");
        assertEquals(FIXED_INSTANT, tsigRecord.getTimeSigned(), "Timestamp should match fixed clock");
        assertNotNull(tsigRecord.getSignature(), "Signature bytes must be present");
        assertTrue(tsigRecord.getSignature().length > 0, "Signature should not be empty");
    }

    /**
     * Verify that the {@code error} argument is propagated into the generated record.
     */
    @Test
    void generateTsigRecordWithErrorCode() throws Exception {
        // an arbitrary non‑zero error
        int errorCode = Rcode.FORMERR;
        TSIGRecord tsigRecord = tsig.generate(queryMessage, queryWire, errorCode, null);
        assertNotNull(tsigRecord, "TSIGRecord should not be null");
        assertEquals(errorCode, tsigRecord.getError(), "Error code should be propagated");
        // Other fields remain valid.
        assertEquals(TSIG.HMAC_SHA256, tsigRecord.getAlgorithm());
        assertEquals(Name.fromString("example."), tsigRecord.getName());
        assertEquals(FIXED_INSTANT, tsigRecord.getTimeSigned());
        assertNotNull(tsigRecord.getSignature());
        assertTrue(tsigRecord.getSignature().length > 0);
    }

    /**
     * Verify that providing a previous TSIGRecord (as would be the case for a response
     * to a signed query) does not cause a failure and that the returned record still
     * contains a valid signature.
     */
    @Test
    void generateWithPreviousTsigRecord() throws Exception {
        // First, generate a TSIG record for the original query.
        TSIGRecord original = tsig.generate(queryMessage, queryWire, Rcode.NOERROR, null);
        assertNotNull(original, "Original TSIGRecord must be generated");
        // Simulate a response message that references the original TSIG.
        Message response = new Message(queryMessage.getHeader().getID());
        // attach the same key for signing
        response.setTSIG(tsig);
        // (no additional records are needed for this test)
        byte[] responseWire = response.toWire(Message.MAXLENGTH);
        // Act: generate a TSIG record for the response, passing the original record.
        TSIGRecord responseTsig = tsig.generate(response, responseWire, Rcode.NOERROR, original);
        // Assert: the response TSIG is valid and independent of the original's signature.
        assertNotNull(responseTsig, "Response TSIGRecord must be generated");
        assertEquals(TSIG.HMAC_SHA256, responseTsig.getAlgorithm());
        assertEquals(Name.fromString("example."), responseTsig.getName());
        assertEquals(Rcode.NOERROR, responseTsig.getError());
        assertEquals(FIXED_INSTANT, responseTsig.getTimeSigned());
        assertNotNull(responseTsig.getSignature());
        assertTrue(responseTsig.getSignature().length > 0);
        // The signatures of the two records should differ because the signed data differs.
        assertFalse(java.util.Arrays.equals(original.getSignature(), responseTsig.getSignature()), "Signatures for different messages should not be identical");
    }

    @Test
    void generate_nullMessage_throwsNPE() {
        Message nullMsg = null;
        assertThrows(NullPointerException.class, () -> tsig.generate(nullMsg, new byte[] { 0x01, 0x02 }, Rcode.NOERROR, null), "Null Message should cause NPE");
    }

    @Test
    void generate_nullByteArray_throwsNPE() {
        Message mockMsg = mock(Message.class);
        assertThrows(NullPointerException.class, () -> tsig.generate(mockMsg, null, Rcode.NOERROR, null), "Null byte array should cause NPE");
    }

    @Test
    void generate_negativeErrorCode_throwsIAE() {
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> tsig.generate(mockMsg, new byte[0], -1, null), "Negative error code must cause IAE");
    }

    @ParameterizedTest
    @ValueSource(ints = { Rcode.BADTIME, Rcode.BADSIG, Rcode.NOERROR })
    void generate_variousErrorCodes_propagatedInRecord(int errorCode) {
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x10, 0x20 }, errorCode, null);
        assertEquals(errorCode, rec.getError(), "Generated TSIGRecord must contain the supplied error code");
    }

    @Test
    void generate_fullSignature_true_returnsFullMac() {
        Message mockMsg = mock(Message.class);
        // Use the public overload that defaults to fullSignature = true
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x55, 0x66, 0x77 }, Rcode.NOERROR, null);
        // HMAC‑SHA256 output length = 32 bytes
        assertEquals(32, rec.getSignature().length, "Full‑signature MAC length must equal full HMAC output");
    }

    @Test
    void generate_fullSignature_false_returnsTruncatedMac() {
        Message mockMsg = mock(Message.class);
        // Explicitly request a truncated MAC (fullSignature = false)
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x55, 0x66, 0x77 }, Rcode.NOERROR, null, false);
        // RFC‑2845 mandates a maximum of 10 bytes when truncated
        assertEquals(10, rec.getSignature().length, "Truncated MAC length must be 10 bytes");
    }

    @Test
    void generate_withOldRecord_chainsMacAndAdjustsTime() throws Exception {
        // --- old record (simulated) ---
        Instant oldInstant = FIXED_INSTANT.minusSeconds(100);
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(oldInstant);
        // dummy previous MAC
        when(old.getSignature()).thenReturn(new byte[32]);
        // --- new message ---
        Message mockMsg = mock(Message.class);
        TSIGRecord fresh = tsig.generate(mockMsg, new byte[] { 0x01, 0x02, 0x03 }, Rcode.NOERROR, old);
        // Time must be old.time + FUDGE (as per RFC‑2845)
        Instant expected = oldInstant.plus(TSIG.FUDGE);
        assertEquals(expected, fresh.getTimeSigned(), "Timestamp should be old.time + FUDGE when old record present");
        // MAC must differ from the old MAC (different input data)
        assertFalse(Arrays.equals(old.getSignature(), fresh.getSignature()), "MAC must be recomputed and differ from previous MAC");
    }

    @Test
    void generate_withoutOldRecord_usesCurrentInstant() {
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x01, 0x02 }, Rcode.NOERROR, null);
        assertEquals(FIXED_INSTANT, rec.getTimeSigned(), "When old is null, timestamp must be the current (fixed) instant");
    }

    @Test
    void generate_hmacSha1_fullSignature_lengthMatchesAlgorithm() {
        TSIG sha1Tsig = new TSIG(TSIG.HMAC_SHA1, "example.", "c2VjcmV0a2V5");
        injectClock(sha1Tsig, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = sha1Tsig.generate(mockMsg, new byte[] { 0x0A, 0x0B }, Rcode.NOERROR, null);
        assertEquals(TSIG.HMAC_SHA1, rec.getAlgorithm(), "Algorithm name must be HMAC_SHA1");
        assertEquals(20, rec.getSignature().length, "Full HMAC‑SHA1 MAC length must be 20 bytes");
    }

    @Test
    void generate_unknownAlgorithm_throwsIAE() {
        // Attempt to create a TSIG with an unsupported algorithm name.
        assertThrows(IllegalArgumentException.class, () -> new TSIG(Name.fromString("unknown-alg."), "example.", "c2VjcmV0a2V5"), "Unsupported algorithm should cause IAE");
    }

    @Test
    void generate_timestampWithinFudgeWindow_isAccepted() {
        // Clock is fixed at FIXED_INSTANT.
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x01 }, Rcode.NOERROR, null);
        Instant now = FIXED_INSTANT;
        assertTrue(!rec.getTimeSigned().isBefore(now.minus(TSIG.FUDGE)) && !rec.getTimeSigned().isAfter(now.plus(TSIG.FUDGE)), "Timestamp must be within ±FUDGE of the current instant");
    }

    @Test
    void generate_oldRecordOutsideFudgeWindow_returnsBadTime() throws Exception {
        // Old record far in the past (600 s before FIXED_INSTANT)
        Instant oldInstant = FIXED_INSTANT.minusSeconds(600);
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(oldInstant);
        when(old.getSignature()).thenReturn(new byte[32]);
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x01 }, Rcode.NOERROR, old);
        assertEquals(Rcode.BADTIME, rec.getError(), "Old timestamp outside fudge window must set error BADTIME");
    }

    @Test
    void generate_emptyWireArray_producesValidMac() {
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[0], Rcode.NOERROR, null);
        assertNotNull(rec.getSignature(), "Signature must not be null for empty wire");
        assertEquals(32, rec.getSignature().length, "MAC length must be full HMAC‑SHA256 output (32 bytes)");
    }

    @Test
    void generate_largeMessage_doesNotThrow_andProducesFullMac() {
        // 10 KB
        byte[] large = new byte[10 * 1024];
        new Random().nextBytes(large);
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, large, Rcode.NOERROR, null);
        assertEquals(32, rec.getSignature().length, "Large message should still produce a full‑length MAC");
    }

    @Test
    void generate_algorithmMismatchBetweenNewAndOld_throwsIAE() throws Exception {
        // Old record uses SHA1, new TSIG instance uses SHA256
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA1);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        when(old.getSignature()).thenReturn(new byte[20]);
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> tsig.generate(mockMsg, new byte[] { 0x01, 0x02 }, Rcode.NOERROR, old), "Mismatched algorithms must cause IAE");
    }

    @Test
    void generate_futureOldTimestampWithinFudge_isAccepted() throws Exception {
        // Old timestamp 200 s in the future (still within FUDGE = 300 s)
        Instant future = FIXED_INSTANT.plusSeconds(200);
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(future);
        when(old.getSignature()).thenReturn(new byte[32]);
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x0A, 0x0B }, Rcode.NOERROR, old);
        // Expected new time = old.time + FUDGE
        Instant expected = future.plus(TSIG.FUDGE);
        assertEquals(expected, rec.getTimeSigned(), "Future old timestamp within fudge should be accepted");
        assertEquals(Rcode.NOERROR, rec.getError(), "Error must be NOERROR for a valid future timestamp");
    }

    @Test
    void generate_oldTimestampAtLowerFudgeBoundary_isValid() throws Exception {
        Instant lower = FIXED_INSTANT.minusSeconds(TSIG.FUDGE.getSeconds());
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(lower);
        when(old.getSignature()).thenReturn(new byte[32]);
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x0C, 0x0D }, Rcode.NOERROR, old);
        // New time = old.time + FUDGE = FIXED_INSTANT
        assertEquals(FIXED_INSTANT, rec.getTimeSigned(), "Lower boundary should be treated as valid");
        assertEquals(Rcode.NOERROR, rec.getError(), "Error must be NOERROR for a timestamp on the boundary");
    }

    @Test
    void generate_hmacSha1_fullSignature_fullLength() {
        TSIG sha1 = new TSIG(TSIG.HMAC_SHA1, "example.", "c2VjcmV0a2V5");
        injectClock(sha1, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = sha1.generate(mockMsg, new byte[] { 0x55, 0x66 }, Rcode.NOERROR, null);
        assertEquals(TSIG.HMAC_SHA1, rec.getAlgorithm());
        assertEquals(20, rec.getSignature().length, "Full HMAC‑SHA1 MAC must be 20 bytes");
    }

    @Test
    void generate_hmacSha384_truncatedSignature() {
        TSIG sha384 = new TSIG(TSIG.HMAC_SHA384, "example.", "c2VjcmV0a2V5");
        injectClock(sha384, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = sha384.generate(mockMsg, new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC }, Rcode.NOERROR, null, false);
        assertEquals(TSIG.HMAC_SHA384, rec.getAlgorithm());
        assertEquals(10, rec.getSignature().length, "Truncated MAC for SHA‑384 must be 10 bytes");
    }

    @Test
    void generate_propagatesNonStandardErrorCode() {
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x01 }, Rcode.NOTAUTH, null);
        assertEquals(Rcode.NOTAUTH, rec.getError(), "Non‑standard error codes must be propagated unchanged");
    }

    @Test
    void generate_oldRecordWithNullMac_startsNewChain() throws Exception {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        // null MAC
        when(old.getSignature()).thenReturn(null);
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[] { 0x10, 0x20, 0x30 }, Rcode.NOERROR, old);
        // Should still produce a full‑length MAC (32 bytes) and not throw.
        assertEquals(32, rec.getSignature().length, "When old MAC is null, a new full‑length MAC must be generated");
    }

    @Test
    void generate_zeroLengthKey_throwsIAE() {
        // Construct TSIG with an empty base64 key (decoded length = 0)
        TSIG zeroKey = new TSIG(TSIG.HMAC_SHA256, "example.", "");
        injectClock(zeroKey, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> zeroKey.generate(mockMsg, new byte[0], Rcode.NOERROR, null), "Zero‑length key must cause IAE");
    }

    @Test
    void generate_shortMD5Key_throwsIAE() {
        // MD5 requires at least 8 bytes; provide only 4 bytes (base64 of "abcd")
        String shortKey = Base64.getEncoder().encodeToString("abcd".getBytes());
        TSIG md5 = new TSIG(TSIG.HMAC_MD5, "example.", shortKey);
        injectClock(md5, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> md5.generate(mockMsg, new byte[] { 0x01 }, Rcode.NOERROR, null), "Key shorter than MD5 minimum must cause IAE");
    }

    @Test
    void generate_errorCodeOutOfRangeHigh_throwsIAE() {
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> tsig.generate(mockMsg, new byte[] { 0x0A, 0x0B }, 9999, null), "Error code > 65535 should cause IAE");
    }

    @Test
    void generate_errorCodeNegative_throwsIAE() {
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> tsig.generate(mockMsg, new byte[] { 0x0A, 0x0B }, -5, null), "Negative error code should cause IAE");
    }

    @Test
    void generate_oldRecordMissingTime_throwsIAE() throws Exception {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA1);
        when(old.getName()).thenReturn(Name.fromString("example."));
        // missing time
        when(old.getTimeSigned()).thenReturn(null);
        when(old.getSignature()).thenReturn(new byte[20]);
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> tsig.generate(mockMsg, new byte[] { 0x01, 0x02, 0x03, 0x04 }, Rcode.NOERROR, old), "Old record with null time must cause IAE");
    }

    @Test
    void generate_oldRecordMissingName_throwsIAE() {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        // missing name
        when(old.getName()).thenReturn(null);
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        when(old.getSignature()).thenReturn(new byte[32]);
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> tsig.generate(mockMsg, new byte[] { 0x0A, 0x0B, 0x0C, 0x0D }, Rcode.NOERROR, old), "Old record with null name must cause IAE");
    }

    @Test
    void generate_oldRecordMissingAlgorithm_throwsIAE() throws Exception {
        TSIGRecord old = mock(TSIGRecord.class);
        // missing algorithm
        when(old.getAlgorithm()).thenReturn(null);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        when(old.getSignature()).thenReturn(new byte[32]);
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> tsig.generate(mockMsg, new byte[] { 0x01, 0x02, 0x03, 0x04 }, Rcode.NOERROR, old), "Old record with null algorithm must cause IAE");
    }

    @Test
    void generate_oldRecordMissingMacAndAlgorithm_throwsIAE() throws Exception {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA384);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        // null MAC
        when(old.getSignature()).thenReturn(null);
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class, () -> tsig.generate(mockMsg, new byte[] { 0x01, 0x02, 0x03, 0x04 }, Rcode.NOERROR, old), "Old record with null MAC must cause IAE");
    }
}
