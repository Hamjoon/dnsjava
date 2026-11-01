```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Extended tests for {@link TSIG#apply(Message, TSIGRecord)} and the related overloads.
 *
 * <p>These tests cover the scenarios described in the JSON test‑case specification, including
 * error handling, signature length variations, algorithm validation and immutability guarantees.
 *
 * <p>All tests use a deterministic {@link Clock} so that timestamps are repeatable.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_ExtendedTest {

    /** TSIG instance used by most tests – HMAC‑SHA256 with a known key. */
    private static TSIG tsig;

    /** Fixed clock for reproducible timestamps. */
    private static Clock fixedClock;

    /** Fresh query message for each test. */
    private Message queryMessage;

    /** Helper to create a minimal TSIGRecord. */
    private static TSIGRecord createTSIGRecord(Name name, Name algorithm, long timeSigned,
                                               byte[] keyData, int error, byte[] mac) {
        // The constructor signature follows dnsjava's TSIGRecord:
        // TSIGRecord(Name name, int dclass, long ttl, Name algorithm, long timeSigned,
        //            int fudge, byte[] mac, int originalID, int error, byte[] other)
        return new TSIGRecord(name, DClass.IN, 0L, algorithm, timeSigned,
                (int) TSIG.FUDGE.getSeconds(), mac, 0, error, null) {
            // Override the method that returns the key data – dnsjava stores it in the record.
            @Override
            public byte[] getKey() {
                return keyData;
            }
        };
    }

    @BeforeAll
    static void beforeAll() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(
                base64.fromString("12345678"), "HmacSHA256"), fixedClock);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        Record q = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(q);
    }

    @AfterEach
    void afterEach() {
        queryMessage = null;
    }

    @AfterAll
    static void afterAll() {
        tsig = null;
    }

    /* --------------------------------------------------------------------- */
    /* Existing simple tests (kept for completeness)                        */
    /* --------------------------------------------------------------------- */

    @Test
    void applyAddsTSIGRecordAndVerifiesSuccessfully() throws IOException {
        tsig.apply(queryMessage, null);
        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec);
        assertTrue(queryMessage.isSigned());
        assertEquals(Rcode.NOERROR, rec.getError());

        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        int result = tsig.verify(parsed, wire, null);
        assertEquals(Rcode.NOERROR, result);
        assertTrue(parsed.isVerified());
    }

    @Test
    void applyWithExistingTSIGRecordDoesNotThrowAndVerifies() throws IOException {
        tsig.apply(queryMessage, null);
        TSIGRecord old = queryMessage.getTSIG();
        assertNotNull(old);

        Message response = new Message(queryMessage.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        tsig.apply(response, old);

        TSIGRecord respRec = response.getTSIG();
        assertNotNull(respRec);
        assertTrue(response.isSigned());

        byte[] wire = response.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        int verify = tsig.verify(parsed, wire, old);
        assertEquals(Rcode.NOERROR, verify);
        assertTrue(parsed.isVerified());
    }

    /* --------------------------------------------------------------------- */
    /* New test groups derived from the JSON specification                    */
    /* --------------------------------------------------------------------- */

    @Nested
    class FullSignatureTrue {

        @Test
        void apply_nullOld_fullSignature_true() throws IOException {
            // old == null, expect NOERROR and a full‑length MAC
            tsig.apply(queryMessage, null);
            TSIGRecord rec = queryMessage.getTSIG();
            assertNotNull(rec);
            assertEquals(Rcode.NOERROR, rec.getError());

            // MAC length for SHA‑256 is 32 bytes
            assertEquals(32, rec.getSignature().length,
                    "Full signature should contain the complete 32‑byte MAC");
        }

        @Test
        void apply_nonNullOld_fullSignature_true() throws IOException {
            // Build an old TSIGRecord with a known key/algorithm
            byte[] key = base64.fromString("dGVzdGtleQ=="); // "testkey"
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            // Apply using the old record – the algorithm/key must be reused
            tsig.apply(queryMessage, old);
            TSIGRecord newRec = queryMessage.getTSIG();
            assertNotNull(newRec);
            assertEquals(Rcode.NOERROR, newRec.getError());
            assertEquals(old.getAlgorithm(), newRec.getAlgorithm(),
                    "Algorithm should be copied from old record");
            assertArrayEquals(old.getKey(), newRec.getKey(),
                    "Key data should be identical to the old record");
            assertEquals(32, newRec.getSignature().length,
                    "Full signature length for SHA‑256 must be 32 bytes");
        }
    }

    @Nested
    class NonZeroErrorCode {

        @Test
        void apply_errorCode_nonZero_fullSignature_true() throws IOException {
            byte[] key = base64.fromString("dGVzdGtleQ==");
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            // Use the 4‑arg overload with a non‑zero error code
            tsig.apply(queryMessage, Rcode.FORMERR, old, true);
            TSIGRecord rec = queryMessage.getTSIG();
            assertNotNull(rec);
            assertEquals(Rcode.FORMERR, rec.getError(),
                    "Error code supplied to overload must be reflected in the record");
            assertEquals(32, rec.getSignature().length,
                    "Full signature should still be 32 bytes when fullSignature=true");
        }

        @Test
        void apply_errorCode_nonZero_fullSignature_false() throws IOException {
            byte[] key = base64.fromString("dGVzdGtleQ==");
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            // false => truncated MAC (first 10 bytes per RFC)
            tsig.apply(queryMessage, Rcode.FORMERR, old, false);
            TSIGRecord rec = queryMessage.getTSIG();
            assertNotNull(rec);
            assertEquals(Rcode.FORMERR, rec.getError());
            assertEquals(10, rec.getSignature().length,
                    "When fullSignature=false the MAC must be truncated to 10 bytes");
        }
    }

    @Nested
    class InvalidParameters {

        @Test
        void apply_invalidAlgorithm_throwsException() {
            byte[] key = base64.fromString("dGVzdGtleQ==");
            // Use a bogus algorithm name
            Name bogus = Name.fromString("unsupported-alg.");
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), bogus,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> tsig.apply(queryMessage, old));
            assertTrue(ex.getMessage().contains("algorithm"));
        }

        @Test
        void apply_keyTooShort_throwsException() {
            // HMAC‑SHA256 requires at least 16 bytes; provide 1 byte
            byte[] shortKey = new byte[]{0x01};
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), shortKey, Rcode.NOERROR, new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> tsig.apply(queryMessage, old));
            assertTrue(ex.getMessage().contains("key"));
        }

        @Test
        void apply_oldRecordNullAlgorithm_throwsException() {
            byte[] key = base64.fromString("dGVzdGtleQ==");
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), null,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> tsig.apply(queryMessage, old));
            assertTrue(ex.getMessage().contains("algorithm"));
        }

        @Test
        void apply_oldRecordNullKeyName_throwsException() {
            byte[] key = base64.fromString("dGVzdGtleQ==");
            TSIGRecord old = createTSIGRecord(null, TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> tsig.apply(queryMessage, old));
            assertTrue(ex.getMessage().contains("key name"));
        }

        @Test
        void apply_oldRecordNullKeyData_throwsException() {
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), null, Rcode.NOERROR, new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> tsig.apply(queryMessage, old));
            assertTrue(ex.getMessage().contains("key"));
        }

        @Test
        void apply_oldRecordEmptyKeyData_throwsException() {
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), new byte[0], Rcode.NOERROR, new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> tsig.apply(queryMessage, old));
            assertTrue(ex.getMessage().contains("key"));
        }

        @Test
        void apply_nullMessage_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> tsig.apply(null, (TSIGRecord) null));
        }
    }

    @Nested
    class TimestampFudge {

        @Test
        void apply_futureTimestampWithinFudge() throws IOException {
            // old record timestamp = now + FUDGE - 1s (still acceptable)
            long future = Instant.now(fixedClock).plusSeconds(TSIG.FUDGE.getSeconds() - 1).getEpochSecond();
            byte[] key = base64.fromString("dGVzdGtleQ==");
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    future, key, Rcode.NOERROR, new byte[0]);

            tsig.apply(queryMessage, old);
            TSIGRecord rec = queryMessage.getTSIG();
            assertNotNull(rec);
            // The generated timestamp should be the current (fixed) time, not the future one
            assertEquals(Instant.now(fixedClock).getEpochSecond(), rec.getTimeSigned(),
                    "Generated TSIG timestamp must be the current time, not the future old time");
        }

        @Test
        void apply_futureTimestampBeyondFudge_throwsException() {
            long tooFuture = Instant.now(fixedClock).plusSeconds(TSIG.FUDGE.getSeconds() + 1).getEpochSecond();
            byte[] key = base64.fromString("dGVzdGtleQ==");
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    tooFuture, key, Rcode.NOERROR, new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> tsig.apply(queryMessage, old));
            assertTrue(ex.getMessage().contains("fudge"));
        }

        @Test
        void apply_clockSkewBeyondFudge_throwsException() {
            // old timestamp = now - (FUDGE + 10 minutes)
            long past = Instant.now(fixedClock).minusSeconds(TSIG.FUDGE.getSeconds() + 600).getEpochSecond();
            byte[] key = base64.fromString("dGVzdGtleQ==");
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    past, key, Rcode.NOERROR, new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> tsig.apply(queryMessage, old));
            assertTrue(ex.getMessage().contains("fudge"));
        }
    }

    @Nested
    class AlgorithmSpecific {

        @Test
        void apply_algorithmHMAC_MD5() throws IOException {
            // Use HMAC‑MD5 (legacy)
            byte[] key = base64.fromString("bWQ1a2V5"); // "md5key"
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_MD5,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            // Construct a TSIG instance that uses MD5 algorithm
            TSIG md5Tsig = new TSIG(TSIG.HMAC_MD5, Name.fromString("example."),
                    new SecretKeySpec(key, "HmacMD5"), fixedClock);
            md5Tsig.apply(queryMessage, old);
            TSIGRecord rec = queryMessage.getTSIG();
            assertNotNull(rec);
            assertEquals(16, rec.getSignature().length,
                    "HMAC‑MD5 MAC length must be 16 bytes");
        }

        @Test
        void apply_keyExactMinimumLength_SHA256() throws IOException {
            // 16‑byte key is the documented minimum for SHA‑256 in dnsjava
            byte[] key = new byte[16];
            for (int i = 0; i < 16; i++) key[i] = (byte) i;
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            tsig.apply(queryMessage, old);
            TSIGRecord rec = queryMessage.getTSIG();
            assertNotNull(rec);
            assertEquals(32, rec.getSignature().length,
                    "SHA‑256 MAC length must be 32 bytes even with minimum‑size key");
        }

        @Test
        void apply_emptyMessageFullSignatureLength() throws IOException {
            // Empty message (no sections)
            Message empty = new Message();
            byte[] key = base64.fromString("ZW1wdHlta2V5"); // "emptykey"
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA384,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            TSIG sha384Tsig = new TSIG(TSIG.HMAC_SHA384, Name.fromString("example."),
                    new SecretKeySpec(key, "HmacSHA384"), fixedClock);
            sha384Tsig.apply(empty, old);
            TSIGRecord rec = empty.getTSIG();
            assertNotNull(rec);
            assertEquals(48, rec.getSignature().length,
                    "Full HMAC‑SHA384 MAC length must be 48 bytes");
        }
    }

    @Nested
    class RecordImmutability {

        @Test
        void apply_oldRecordImmutability() throws IOException {
            byte[] key = base64.fromString("aW1tdXRhYmxl"); // "immutable"
            byte[] mac = new byte[]{(byte) 0xAA, (byte) 0xBB};
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA1,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, mac);

            // Capture old values
            Name oldAlg = old.getAlgorithm();
            Name oldName = old.getName();
            long oldTime = old.getTimeSigned();
            int oldError = old.getError();
            byte[] oldKey = Arrays.copyOf(old.getKey(), old.getKey().length);
            byte[] oldMac = Arrays.copyOf(old.getSignature(), old.getSignature().length);

            tsig.apply(queryMessage, old);

            // Verify the original record has not changed
            assertEquals(oldAlg, old.getAlgorithm());
            assertEquals(oldName, old.getName());
            assertEquals(oldTime, old.getTimeSigned());
            assertEquals(oldError, old.getError());
            assertArrayEquals(oldKey, old.getKey());
            assertArrayEquals(oldMac, old.getSignature(),
                    "The MAC stored in the supplied old record must remain unchanged");
        }

        @Test
        void apply_messageWithExistingTSIG_replacesTSIG() throws IOException {
            // First, add an initial TSIG (algorithm HMAC_SHA1)
            TSIG sha1Tsig = new TSIG(TSIG.HMAC_SHA1, Name.fromString("example."),
                    new SecretKeySpec(base64.fromString("b2xkS2V5"), "HmacSHA1"), fixedClock);
            sha1Tsig.apply(queryMessage, null);
            TSIGRecord first = queryMessage.getTSIG();
            assertNotNull(first);
            assertEquals(TSIG.HMAC_SHA1, first.getAlgorithm());

            // Now apply a new TSIG using a different algorithm/key (SHA‑256)
            byte[] newKey = base64.fromString("bmV3S2V5"); // "newKey"
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), newKey, Rcode.NOERROR, new byte[0]);

            tsig.apply(queryMessage, old);
            TSIGRecord second = queryMessage.getTSIG();
            assertNotNull(second);
            assertEquals(TSIG.HMAC_SHA256, second.getAlgorithm(),
                    "The original TSIGRecord should be replaced by the newly generated one");
            // Ensure only one TSIG section exists
            assertEquals(1, queryMessage.getSectionArray(Section.ADDITIONAL).length,
                    "Message must contain exactly one TSIG record after replacement");
        }
    }

    @Nested
    class RepeatedApply {

        @Test
        void apply_repeatApplyDifferentSignature() throws IOException, InterruptedException {
            byte[] key = base64.fromString("cmVwZXRrZXk="); // "repeatkey"
            TSIGRecord old = createTSIGRecord(Name.fromString("example."), TSIG.HMAC_SHA256,
                    Instant.now(fixedClock).getEpochSecond(), key, Rcode.NOERROR, new byte[0]);

            // First apply
            tsig.apply(queryMessage, old);
            TSIGRecord first = queryMessage.getTSIG();
            long firstTime = first.getTimeSigned();
            byte[] firstMac = Arrays.copyOf(first.getSignature(), first.getSignature().length);

            // Simulate clearing the previous TSIG (as a client would before re‑signing)
            queryMessage.removeAllRecords(Section.ADDITIONAL);
            // Apply again with the same old record
            tsig.apply(queryMessage, old);
            TSIGRecord second = queryMessage.getTSIG();

            // Timestamps must differ (the fixed clock is the same, but the implementation adds
            // a one‑second increment for each call – this is verified by the library contract)
            assertNotEquals(firstTime, second.getTimeSigned(),
                    "Each apply call must generate a new timestamp");
            assertFalse(Arrays.equals(firstMac, second.getSignature()),
                    "MAC values must differ because the timestamp is part of the signed data");
        }
    }

    @Nested
    class ImmutableMessage {

        @Test
        void apply_messageImmutable_throwsException() throws IOException {
            // Serialize the message – dnsjava marks the Message as immutable after toWire()
            byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
            Message immutable = new Message(wire); // this instance is now read‑only

            // Attempt to apply a TSIG – expected to fail
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> tsig.apply(immutable, null));
            assertTrue(ex.getMessage().contains("immutable") || ex.getMessage().contains("sealed"));
        }
    }
}
```