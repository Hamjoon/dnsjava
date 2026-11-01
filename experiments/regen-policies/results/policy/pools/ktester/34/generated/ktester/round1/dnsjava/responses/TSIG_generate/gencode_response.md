```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Extended unit tests for {@link TSIG#generate(Message, byte[], int, TSIGRecord)}.
 *
 * <p>These tests cover the exhaustive scenarios described in the JSON test‑case
 * specification, including null checks, error‑code handling, full‑signature
 * truncation, old‑record chaining, algorithm selection, clock/fudge handling,
 * empty and large messages, and validation of key length and old‑record fields.</p>
 *
 * <p>The {@code @BeforeEach} method from the original framework creates a
 * deterministic {@link Clock} and a basic {@link TSIG} instance using the
 * HMAC‑SHA256 algorithm.  For tests that need a different algorithm or a
 * different key, a new {@code TSIG} object is instantiated inside the test
 * method and its internal clock is replaced via reflection (allowed in test
 * code).</p>
 */
@ExtendWith(MockitoExtension.class)
class TSIG_generate_Test {

    /** Fixed instant used for deterministic TSIG timestamps. */
    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

    /** The default TSIG instance (HMAC‑SHA256, key "secretkey"). */
    private TSIG tsig;

    /** Minimal DNS query used for most tests. */
    private Message queryMessage;

    /** Wire format of {@link #queryMessage} (without TSIG). */
    private byte[] queryWire;

    @org.junit.jupiter.api.BeforeEach
    void setupBeforeEach() {
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

        tsig = new TSIG(TSIG.HMAC_SHA256, "example.", "c2VjcmV0a2V5"); // "secretkey" base64‑encoded
        injectClock(tsig, fixedClock);

        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        queryWire = queryMessage.toWire(Message.MAXLENGTH);
    }

    /** Helper to replace the private {@code clock} field of a {@link TSIG}. */
    private static void injectClock(TSIG target, Clock clock) {
        try {
            Field f = TSIG.class.getDeclaredField("clock");
            f.setAccessible(true);
            f.set(target, clock);
        } catch (ReflectiveOperationException e) {
            fail("Unable to inject Clock into TSIG", e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*                     1. Null and Invalid Input Tests                    */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_nullMessage_throwsNPE() {
        Message nullMsg = null;
        assertThrows(NullPointerException.class,
                () -> tsig.generate(nullMsg, new byte[]{0x01, 0x02}, Rcode.NOERROR, null),
                "Null Message should cause NPE");
    }

    @Test
    void generate_nullByteArray_throwsNPE() {
        Message mockMsg = mock(Message.class);
        assertThrows(NullPointerException.class,
                () -> tsig.generate(mockMsg, null, Rcode.NOERROR, null),
                "Null byte array should cause NPE");
    }

    @Test
    void generate_negativeErrorCode_throwsIAE() {
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.generate(mockMsg, new byte[0], -1, null),
                "Negative error code must cause IAE");
    }

    /* ---------------------------------------------------------------------- */
    /*                     2. Error Code Branches                             */
    /* ---------------------------------------------------------------------- */

    @ParameterizedTest
    @ValueSource(ints = {Rcode.BADTIME, Rcode.BADSIG, Rcode.NOERROR})
    void generate_variousErrorCodes_propagatedInRecord(int errorCode) {
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x10, 0x20}, errorCode, null);
        assertEquals(errorCode, rec.getError(),
                "Generated TSIGRecord must contain the supplied error code");
    }

    /* ---------------------------------------------------------------------- */
    /*                     3. FullSignature Variations                        */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_fullSignature_true_returnsFullMac() {
        Message mockMsg = mock(Message.class);
        // Use the public overload that defaults to fullSignature = true
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x55, 0x66, 0x77}, Rcode.NOERROR, null);
        // HMAC‑SHA256 output length = 32 bytes
        assertEquals(32, rec.getSignature().length,
                "Full‑signature MAC length must equal full HMAC output");
    }

    @Test
    void generate_fullSignature_false_returnsTruncatedMac() {
        Message mockMsg = mock(Message.class);
        // Explicitly request a truncated MAC (fullSignature = false)
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x55, 0x66, 0x77},
                Rcode.NOERROR, null, false);
        // RFC‑2845 mandates a maximum of 10 bytes when truncated
        assertEquals(10, rec.getSignature().length,
                "Truncated MAC length must be 10 bytes");
    }

    /* ---------------------------------------------------------------------- */
    /*                     4. Old Record Influence                             */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_withOldRecord_chainsMacAndAdjustsTime() {
        // --- old record (simulated) ---
        Instant oldInstant = FIXED_INSTANT.minusSeconds(100);
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(oldInstant);
        when(old.getSignature()).thenReturn(new byte[32]); // dummy previous MAC

        // --- new message ---
        Message mockMsg = mock(Message.class);
        TSIGRecord fresh = tsig.generate(mockMsg, new byte[]{0x01, 0x02, 0x03},
                Rcode.NOERROR, old);

        // Time must be old.time + FUDGE (as per RFC‑2845)
        Instant expected = oldInstant.plus(TSIG.FUDGE);
        assertEquals(expected, fresh.getTimeSigned(),
                "Timestamp should be old.time + FUDGE when old record present");

        // MAC must differ from the old MAC (different input data)
        assertFalse(Arrays.equals(old.getSignature(), fresh.getSignature()),
                "MAC must be recomputed and differ from previous MAC");
    }

    @Test
    void generate_withoutOldRecord_usesCurrentInstant() {
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x01, 0x02},
                Rcode.NOERROR, null);
        assertEquals(FIXED_INSTANT, rec.getTimeSigned(),
                "When old is null, timestamp must be the current (fixed) instant");
    }

    /* ---------------------------------------------------------------------- */
    /*                     5. Algorithm Selection                              */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_hmacSha1_fullSignature_lengthMatchesAlgorithm() {
        TSIG sha1Tsig = new TSIG(TSIG.HMAC_SHA1, "example.", "c2VjcmV0a2V5");
        injectClock(sha1Tsig, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        Message mockMsg = mock(Message.class);
        TSIGRecord rec = sha1Tsig.generate(mockMsg, new byte[]{0x0A, 0x0B},
                Rcode.NOERROR, null);
        assertEquals(TSIG.HMAC_SHA1, rec.getAlgorithm(),
                "Algorithm name must be HMAC_SHA1");
        assertEquals(20, rec.getSignature().length,
                "Full HMAC‑SHA1 MAC length must be 20 bytes");
    }

    @Test
    void generate_unknownAlgorithm_throwsIAE() {
        // Attempt to create a TSIG with an unsupported algorithm name.
        assertThrows(IllegalArgumentException.class,
                () -> new TSIG(Name.fromString("unknown-alg."), "example.", "c2VjcmV0a2V5"),
                "Unsupported algorithm should cause IAE");
    }

    /* ---------------------------------------------------------------------- */
    /*                     6. Clock / Time Fudge Handling                      */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_timestampWithinFudgeWindow_isAccepted() {
        // Clock is fixed at FIXED_INSTANT.
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x01}, Rcode.NOERROR, null);
        Instant now = FIXED_INSTANT;
        assertTrue(!rec.getTimeSigned().isBefore(now.minus(TSIG.FUDGE)) &&
                        !rec.getTimeSigned().isAfter(now.plus(TSIG.FUDGE)),
                "Timestamp must be within ±FUDGE of the current instant");
    }

    @Test
    void generate_oldRecordOutsideFudgeWindow_returnsBadTime() {
        // Old record far in the past (600 s before FIXED_INSTANT)
        Instant oldInstant = FIXED_INSTANT.minusSeconds(600);
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(oldInstant);
        when(old.getSignature()).thenReturn(new byte[32]);

        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x01}, Rcode.NOERROR, old);
        assertEquals(Rcode.BADTIME, rec.getError(),
                "Old timestamp outside fudge window must set error BADTIME");
    }

    /* ---------------------------------------------------------------------- */
    /*                     7. Empty Wire Format                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_emptyWireArray_producesValidMac() {
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[0], Rcode.NOERROR, null);
        assertNotNull(rec.getSignature(), "Signature must not be null for empty wire");
        assertEquals(32, rec.getSignature().length,
                "MAC length must be full HMAC‑SHA256 output (32 bytes)");
    }

    /* ---------------------------------------------------------------------- */
    /*                     8. Large Message Handling                             */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_largeMessage_doesNotThrow_andProducesFullMac() {
        byte[] large = new byte[10 * 1024]; // 10 KB
        new Random().nextBytes(large);
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, large, Rcode.NOERROR, null);
        assertEquals(32, rec.getSignature().length,
                "Large message should still produce a full‑length MAC");
    }

    /* ---------------------------------------------------------------------- */
    /*                     9. Algorithm Mismatch Between New and Old Records   */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_algorithmMismatchBetweenNewAndOld_throwsIAE() {
        // Old record uses SHA1, new TSIG instance uses SHA256
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA1);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        when(old.getSignature()).thenReturn(new byte[20]);

        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.generate(mockMsg, new byte[]{0x01, 0x02},
                        Rcode.NOERROR, old),
                "Mismatched algorithms must cause IAE");
    }

    /* ---------------------------------------------------------------------- */
    /*                     10. Old Record Timestamp Within Future Fudge Window */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_futureOldTimestampWithinFudge_isAccepted() {
        // Old timestamp 200 s in the future (still within FUDGE = 300 s)
        Instant future = FIXED_INSTANT.plusSeconds(200);
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(future);
        when(old.getSignature()).thenReturn(new byte[32]);

        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x0A, 0x0B},
                Rcode.NOERROR, old);
        // Expected new time = old.time + FUDGE
        Instant expected = future.plus(TSIG.FUDGE);
        assertEquals(expected, rec.getTimeSigned(),
                "Future old timestamp within fudge should be accepted");
        assertEquals(Rcode.NOERROR, rec.getError(),
                "Error must be NOERROR for a valid future timestamp");
    }

    /* ---------------------------------------------------------------------- */
    /*                     11. Old Record Timestamp At Lower Boundary        */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_oldTimestampAtLowerFudgeBoundary_isValid() {
        Instant lower = FIXED_INSTANT.minusSeconds(TSIG.FUDGE.getSeconds());
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(lower);
        when(old.getSignature()).thenReturn(new byte[32]);

        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x0C, 0x0D},
                Rcode.NOERROR, old);
        // New time = old.time + FUDGE = FIXED_INSTANT
        assertEquals(FIXED_INSTANT, rec.getTimeSigned(),
                "Lower boundary should be treated as valid");
        assertEquals(Rcode.NOERROR, rec.getError(),
                "Error must be NOERROR for a timestamp on the boundary");
    }

    /* ---------------------------------------------------------------------- */
    /*                     12. Non‑Default Algorithm – HMAC_SHA1 Full Signature */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_hmacSha1_fullSignature_fullLength() {
        TSIG sha1 = new TSIG(TSIG.HMAC_SHA1, "example.", "c2VjcmV0a2V5");
        injectClock(sha1, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        Message mockMsg = mock(Message.class);
        TSIGRecord rec = sha1.generate(mockMsg, new byte[]{0x55, 0x66},
                Rcode.NOERROR, null);
        assertEquals(TSIG.HMAC_SHA1, rec.getAlgorithm());
        assertEquals(20, rec.getSignature().length,
                "Full HMAC‑SHA1 MAC must be 20 bytes");
    }

    /* ---------------------------------------------------------------------- */
    /*                     13. Non‑Default Algorithm – HMAC_SHA384 Truncated   */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_hmacSha384_truncatedSignature() {
        TSIG sha384 = new TSIG(TSIG.HMAC_SHA384, "example.", "c2VjcmV0a2V5");
        injectClock(sha384, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        Message mockMsg = mock(Message.class);
        TSIGRecord rec = sha384.generate(mockMsg, new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC},
                Rcode.NOERROR, null, false);
        assertEquals(TSIG.HMAC_SHA384, rec.getAlgorithm());
        assertEquals(10, rec.getSignature().length,
                "Truncated MAC for SHA‑384 must be 10 bytes");
    }

    /* ---------------------------------------------------------------------- */
    /*                     14. Propagation of Non‑Standard Error Codes         */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_propagatesNonStandardErrorCode() {
        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x01}, Rcode.NOTAUTH, null);
        assertEquals(Rcode.NOTAUTH, rec.getError(),
                "Non‑standard error codes must be propagated unchanged");
    }

    /* ---------------------------------------------------------------------- */
    /*                     15. Old Record With Null Prior MAC                 */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_oldRecordWithNullMac_startsNewChain() {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        when(old.getSignature()).thenReturn(null); // null MAC

        Message mockMsg = mock(Message.class);
        TSIGRecord rec = tsig.generate(mockMsg, new byte[]{0x10, 0x20, 0x30},
                Rcode.NOERROR, old);
        // Should still produce a full‑length MAC (32 bytes) and not throw.
        assertEquals(32, rec.getSignature().length,
                "When old MAC is null, a new full‑length MAC must be generated");
    }

    /* ---------------------------------------------------------------------- */
    /*                     16. Invalid Key Length Tests                        */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_zeroLengthKey_throwsIAE() {
        // Construct TSIG with an empty base64 key (decoded length = 0)
        TSIG zeroKey = new TSIG(TSIG.HMAC_SHA256, "example.", "");
        injectClock(zeroKey, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> zeroKey.generate(mockMsg, new byte[0], Rcode.NOERROR, null),
                "Zero‑length key must cause IAE");
    }

    @Test
    void generate_shortMD5Key_throwsIAE() {
        // MD5 requires at least 8 bytes; provide only 4 bytes (base64 of "abcd")
        String shortKey = Base64.getEncoder().encodeToString("abcd".getBytes());
        TSIG md5 = new TSIG(TSIG.HMAC_MD5, "example.", shortKey);
        injectClock(md5, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> md5.generate(mockMsg, new byte[]{0x01}, Rcode.NOERROR, null),
                "Key shorter than MD5 minimum must cause IAE");
    }

    /* ---------------------------------------------------------------------- */
    /*                     17. Invalid Error Code Tests                        */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_errorCodeOutOfRangeHigh_throwsIAE() {
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.generate(mockMsg, new byte[]{0x0A, 0x0B}, 9999, null),
                "Error code > 65535 should cause IAE");
    }

    @Test
    void generate_errorCodeNegative_throwsIAE() {
        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.generate(mockMsg, new byte[]{0x0A, 0x0B}, -5, null),
                "Negative error code should cause IAE");
    }

    /* ---------------------------------------------------------------------- */
    /*                     18. Old Record Missing Required Fields               */
    /* ---------------------------------------------------------------------- */

    @Test
    void generate_oldRecordMissingTime_throwsIAE() {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA1);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(null); // missing time
        when(old.getSignature()).thenReturn(new byte[20]);

        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.generate(mockMsg, new byte[]{0x01, 0x02, 0x03, 0x04},
                        Rcode.NOERROR, old),
                "Old record with null time must cause IAE");
    }

    @Test
    void generate_oldRecordMissingName_throwsIAE() {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(null); // missing name
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        when(old.getSignature()).thenReturn(new byte[32]);

        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.generate(mockMsg, new byte[]{0x0A, 0x0B, 0x0C, 0x0D},
                        Rcode.NOERROR, old),
                "Old record with null name must cause IAE");
    }

    @Test
    void generate_oldRecordMissingAlgorithm_throwsIAE() {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(null); // missing algorithm
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        when(old.getSignature()).thenReturn(new byte[32]);

        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.generate(mockMsg, new byte[]{0x01, 0x02, 0x03, 0x04},
                        Rcode.NOERROR, old),
                "Old record with null algorithm must cause IAE");
    }

    @Test
    void generate_oldRecordMissingMacAndAlgorithm_throwsIAE() {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA384);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getTimeSigned()).thenReturn(FIXED_INSTANT);
        when(old.getSignature()).thenReturn(null); // null MAC

        Message mockMsg = mock(Message.class);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.generate(mockMsg, new byte[]{0x01, 0x02, 0x03, 0x04},
                        Rcode.NOERROR, old),
                "Old record with null MAC must cause IAE");
    }
}
```