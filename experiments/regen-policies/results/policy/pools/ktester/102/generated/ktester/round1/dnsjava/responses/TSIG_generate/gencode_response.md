```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit‑tests for {@link TSIG#generate(Message, byte[], int, TSIGRecord)} and its
 * overloads.  The tests cover normal operation, error handling and edge‑cases
 * described in the supplied JSON test matrix.
 *
 * <p>All tests use only the public API – private members are accessed via
 * reflection only when the public overload with the {@code fullSignature}
 * flag is required (the flag‑less overload always forces {@code true}).
 */
@ExtendWith(MockitoExtension.class)
class TSIG_generate_Test {

    /** Fixed clock used for deterministic timestamps. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /** A TSIG instance that uses HMAC‑SHA256 and a known key. */
    private static TSIG tsigSha256;

    /** Helper: compute HMAC using the JCE provider. */
    private static byte[] hmac(String algorithm, SecretKey key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(key);
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void beforeAll() {
        // key = base64("12345678") → 8‑byte array {0xD7,0x9E,0xF5,0xB5,0xF5,0xC2,0xF5,0xE5}
        SecretKey key = new SecretKeySpec(base64.fromString("12345678"), "HmacSHA256");
        tsigSha256 = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), key, FIXED_CLOCK);
    }

    @BeforeEach
    void beforeEach() {
        // No per‑test state needed.
    }

    @AfterEach
    void afterEach() {
        // No cleanup required.
    }

    @AfterAll
    static void afterAll() {
        // Nothing to clean up globally.
    }

    /* --------------------------------------------------------------------- */
    /* 1.  Basic happy‑path test – already present in the starter code.      */
    /* --------------------------------------------------------------------- */
    @Test
    void generateCreatesValidTSIGRecord() throws IOException {
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsigSha256.generate(query, wire, Rcode.NOERROR, null);

        assertNotNull(rec);
        assertEquals(TSIG.HMAC_SHA256, rec.getAlgorithm());
        assertEquals(Rcode.NOERROR, rec.getError());
        assertEquals(32, rec.getSignature().length);
        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), rec.getTimeSigned());
    }

    /* --------------------------------------------------------------------- */
    /* 2.  Exception during MAC initialization (unsupported algorithm).      */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_unsupportedAlgorithm_throwsRuntimeException() {
        // old TSIGRecord with an algorithm that cannot be mapped to a JCE MAC.
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(Name.fromString("unsupported-alg."));
        when(old.getKey()).thenReturn(new byte[]{0x01, 0x02}); // dummy key

        Message msg = Message.newQuery(
                Record.newRecord(Name.fromString("test.example."), Type.A, DClass.IN));

        byte[] wire = msg.toWire(Message.MAXLENGTH);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tsigSha256.generate(msg, wire, Rcode.NOERROR, old));
        assertTrue(ex.getCause() instanceof GeneralSecurityException,
                "Root cause should be a GeneralSecurityException");
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Normal generation without prior TSIG (full signature).           */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_withoutPriorTSIG_fullSignature() throws IOException {
        Message msg = Message.newQuery(
                Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN));
        byte[] wire = msg.toWire(Message.MAXLENGTH);

        TSIGRecord rec = tsigSha256.generate(msg, wire, Rcode.NOERROR, null);

        // Expected MAC = HMAC‑SHA256(key, wire)
        SecretKey key = new SecretKeySpec(base64.fromString("12345678"), "HmacSHA256");
        byte[] expectedMac = hmac("HmacSHA256", key, wire);

        assertArrayEquals(expectedMac, rec.getSignature(),
                "MAC must match the full HMAC‑SHA256 of the wire format");
        assertEquals(0, rec.getError());
        assertEquals(TSIG.HMAC_SHA256, rec.getAlgorithm());
        assertTrue(Duration.between(rec.getTimeSigned(), FIXED_CLOCK.instant()).abs()
                .compareTo(TSIG.FUDGE) <= 0,
                "timeSigned must be within the TSIG fudge window");
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Normal generation with prior TSIG (full signature).              */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_withPriorTSIG_fullSignature() throws IOException {
        // First generate a record that will become the “old” record.
        Message first = Message.newQuery(
                Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN));
        byte[] firstWire = first.toWire(Message.MAXLENGTH);
        TSIGRecord old = tsigSha256.generate(first, firstWire, Rcode.NOERROR, null);

        // Second message – the MAC must incorporate the previous MAC.
        Message second = Message.newQuery(
                Record.newRecord(Name.fromString("example.org."), Type.A, DClass.IN));
        byte[] secondWire = second.toWire(Message.MAXLENGTH);

        TSIGRecord next = tsigSha256.generate(second, secondWire, Rcode.NOERROR, old);

        // Expected MAC = HMAC‑SHA256(key, secondWire || oldMac)
        SecretKey key = new SecretKeySpec(base64.fromString("12345678"), "HmacSHA256");
        byte[] concat = new byte[secondWire.length + old.getSignature().length];
        System.arraycopy(secondWire, 0, concat, 0, secondWire.length);
        System.arraycopy(old.getSignature(), 0, concat, secondWire.length,
                old.getSignature().length);
        byte[] expected = hmac("HmacSHA256", key, concat);

        assertArrayEquals(expected, next.getSignature(),
                "MAC must include the previous MAC when a prior TSIGRecord is supplied");
    }

    /* --------------------------------------------------------------------- */
    /* 5.  Generation with a non‑zero error code.                           */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_nonZeroErrorCode_isPreserved() throws IOException {
        Message msg = Message.newQuery(
                Record.newRecord(Name.fromString("www.example.org."), Type.A, DClass.IN));
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        int error = 9; // BADKEY

        TSIGRecord rec = tsigSha256.generate(msg, wire, error, null);
        assertEquals(error, rec.getError(),
                "Error code supplied to generate must be stored in the TSIGRecord");
    }

    /* --------------------------------------------------------------------- */
    /* 6.  Generation with an empty message payload.                        */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_emptyMessagePayload() throws IOException {
        Message empty = new Message(); // no sections
        byte[] wire = empty.toWire(Message.MAXLENGTH);
        // Use a different key to make the expected MAC easy to verify.
        SecretKey key = new SecretKeySpec(new byte[]{(byte) 0xAA, (byte) 0xBB}, "HmacSHA256");
        TSIG tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("empty."), key, FIXED_CLOCK);

        TSIGRecord rec = tsig.generate(empty, wire, Rcode.NOERROR, null);
        byte[] expected = hmac("HmacSHA256", key, wire);
        assertArrayEquals(expected, rec.getSignature(),
                "MAC of an empty message must be the HMAC of an empty byte array");
        assertEquals(32, rec.getSignature().length);
        assertEquals(TSIG.HMAC_SHA256, rec.getAlgorithm());
    }

    /* --------------------------------------------------------------------- */
    /* 7.  Generation with fullSignature = false (truncated MAC).           */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_truncatedMac_whenFullSignatureFalse() throws IOException {
        Message msg = Message.newQuery(
                Record.newRecord(Name.fromString("test.example."), Type.TXT, DClass.IN));
        byte[] wire = msg.toWire(Message.MAXLENGTH);

        // Call the overload that accepts the fullSignature flag.
        TSIGRecord rec = tsigSha256.generate(msg, wire, Rcode.NOERROR, null, false);

        assertEquals(10, rec.getSignature().length,
                "When fullSignature is false the MAC must be truncated to 10 bytes");
        // Verify that the first 10 bytes match the full HMAC.
        SecretKey key = new SecretKeySpec(base64.fromString("12345678"), "HmacSHA256");
        byte[] full = hmac("HmacSHA256", key, wire);
        byte[] expected = new byte[10];
        System.arraycopy(full, 0, expected, 0, 10);
        assertArrayEquals(expected, rec.getSignature(),
                "Truncated MAC must be the leading 10 bytes of the full HMAC");
    }

    /* --------------------------------------------------------------------- */
    /* 8.  Null Message argument handling.                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_nullMessage_throwsNPE() {
        byte[] dummy = new byte[]{0x00};
        assertThrows(NullPointerException.class,
                () -> tsigSha256.generate(null, dummy, Rcode.NOERROR, null));
    }

    /* --------------------------------------------------------------------- */
    /* 9.  Algorithm mapping verification – HMAC‑SHA1.                      */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_hmacSha1AlgorithmMapping() throws IOException {
        SecretKey key = new SecretKeySpec(new byte[]{0x11, 0x22, 0x33, 0x44}, "HmacSHA1");
        TSIG tsigSha1 = new TSIG(TSIG.HMAC_SHA1, Name.fromString("sha1.example."), key, FIXED_CLOCK);

        Message msg = Message.newQuery(
                Record.newRecord(Name.fromString("mail.example.com."), Type.MX, DClass.IN));
        byte[] wire = msg.toWire(Message.MAXLENGTH);

        TSIGRecord rec = tsigSha1.generate(msg, wire, Rcode.NOERROR, null);
        assertEquals(TSIG.HMAC_SHA1, rec.getAlgorithm(),
                "Algorithm name in the generated record must match HMAC‑SHA1");

        byte[] expected = hmac("HmacSHA1", key, wire);
        assertArrayEquals(expected, rec.getSignature(),
                "MAC must be computed with HMAC‑SHA1");
    }

    /* --------------------------------------------------------------------- */
    /* 10.  Fudge window boundary test (real clock).                       */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_timeSignedWithinFudgeWindow() throws IOException {
        // Use the system clock so the timestamp is “now”.
        SecretKey key = new SecretKeySpec(base64.fromString("12345678"), "HmacSHA256");
        TSIG tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("fudge.example."), key,
                Clock.systemUTC());

        Message msg = Message.newQuery(
                Record.newRecord(Name.fromString("example.org."), Type.SOA, DClass.IN));
        byte[] wire = msg.toWire(Message.MAXLENGTH);

        TSIGRecord rec = tsig.generate(msg, wire, Rcode.NOERROR, null);
        Instant now = Instant.now();
        Duration diff = Duration.between(rec.getTimeSigned(), now).abs();

        assertTrue(diff.compareTo(TSIG.FUDGE) <= 0,
                "timeSigned must be within the TSIG.FUDGE window of the current instant");
    }

    /* --------------------------------------------------------------------- */
    /* 11.  Old TSIGRecord with null MAC – should raise NPE.                 */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_oldRecordWithNullMac_throwsNPE() throws IOException {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getSignature()).thenReturn(null); // MAC is null

        Message msg = Message.newQuery(
                Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN));
        byte[] wire = msg.toWire(Message.MAXLENGTH);

        assertThrows(NullPointerException.class,
                () -> tsigSha256.generate(msg, wire, Rcode.NOERROR, old));
    }

    /* --------------------------------------------------------------------- */
    /* 12.  Old TSIGRecord with null algorithm – should raise NPE.          */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_oldRecordWithNullAlgorithm_throwsNPE() throws IOException {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(null);
        when(old.getSignature()).thenReturn(new byte[32]); // dummy MAC

        Message msg = Message.newQuery(
                Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN));
        byte[] wire = msg.toWire(Message.MAXLENGTH);

        assertThrows(NullPointerException.class,
                () -> tsigSha256.generate(msg, wire, Rcode.NOERROR, old));
    }

    /* --------------------------------------------------------------------- */
    /* 13.  Invalid error code handling (negative and >255).                */
    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(ints = {-1, 300})
    void generate_invalidErrorCode_throwsIllegalArgumentException(int badError) throws IOException {
        Message msg = Message.newQuery(
                Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN));
        byte[] wire = msg.toWire(Message.MAXLENGTH);

        assertThrows(IllegalArgumentException.class,
                () -> tsigSha256.generate(msg, wire, badError, null));
    }

    /* --------------------------------------------------------------------- */
    /* 14.  Message serialization exception handling.                       */
    /* --------------------------------------------------------------------- */
    @Test
    void generate_messageToWireThrowsIOException_wrappedInRuntimeException() throws Exception {
        // Mock a Message whose toWire() throws IOException.
        Message mockMsg = mock(Message.class);
        when(mockMsg.toWire(anyInt())).thenThrow(new IOException("simulated I/O failure"));

        byte[] dummyKey = new byte[]{0x55, 0x66, 0x77, 0x88};
        SecretKey key = new SecretKeySpec(dummyKey, "HmacSHA256");
        TSIG tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("io.example."), key, FIXED_CLOCK);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tsig.generate(mockMsg, new byte[]{0x55, 0x66, 0x77, 0x88}, Rcode.NOERROR, null));
        assertTrue(ex.getCause() instanceof IOException,
                "The cause of the RuntimeException should be the original IOException");
    }

    /* --------------------------------------------------------------------- */
    /* 15.  Null key array handling – constructor should reject null key.     */
    /* --------------------------------------------------------------------- */
    @Test
    void construct_TSIGWithNullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new TSIG(TSIG.HMAC_SHA256, Name.fromString("nullkey.example."), null, FIXED_CLOCK));
    }
}
```