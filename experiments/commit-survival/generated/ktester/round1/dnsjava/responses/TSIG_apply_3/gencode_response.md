```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprehensive tests for {@link TSIG#apply(Message, int, TSIGRecord)}.
 *
 * <p>The test suite covers the JSON‑driven scenarios supplied in the
 * specification.  Each scenario is expressed as a separate test method or a
 * parameterized test group.  The {@code @BeforeEach} method creates a fresh
 * {@link TSIG} instance with a deterministic clock and a known secret key so
 * that timestamps are reproducible.</p>
 *
 * <p>Only public APIs are used; when a {@link TSIGRecord} with specific
 * properties is required, a reflective helper creates an instance via the
 * public constructor of {@code TSIGRecord}.</p>
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_3_Test {

    /** A deterministic clock to make the generated TSIG timestamps predictable. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /** The TSIG key used for all tests. */
    private TSIG tsig;

    /** A simple DNS query message used as the base for signing. */
    private Message queryMessage;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Initialise a TSIG instance with a known key and a fixed clock.
        // Using the public constructor that accepts algorithm, key name, secret key and clock.
        byte[] keyBytes = base64.fromString("12345678");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), keySpec, FIXED_CLOCK);

        // Build a minimal query message (A record for www.example.).
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------
     * Helper methods
     * -------------------------------------------------------------- */

    /**
     * Creates a {@link TSIGRecord} with the supplied parameters using reflection.
     *
     * The concrete constructor signature of {@code TSIGRecord} in dnsjava is:
     * <pre>
     *   TSIGRecord(Name name, Name algorithm, byte[] mac,
     *              long timeSigned, int fudge, int error)
     * </pre>
     * If the library version changes, this helper can be adapted without
     * touching the test logic.
     */
    private static TSIGRecord createTSIGRecord(Name keyName,
                                                Name algorithm,
                                                Instant time,
                                                int error) {
        try {
            Constructor<TSIGRecord> ctor = TSIGRecord.class.getDeclaredConstructor(
                    Name.class, Name.class, byte[].class, long.class, int.class, int.class);
            ctor.setAccessible(true);
            // MAC can be empty – it will be replaced by {@code apply}.
            byte[] emptyMac = new byte[0];
            long epochSeconds = time.getEpochSecond();
            int fudge = (int) TSIG.FUDGE.getSeconds();
            return ctor.newInstance(keyName, algorithm, emptyMac, epochSeconds, fudge, error);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create TSIGRecord via reflection", e);
        }
    }

    /**
     * Returns a fresh {@link Message} with a single random A record.
     * Useful for large‑message tests.
     */
    private static Message createRandomMessage(int recordCount) {
        Message msg = new Message();
        Random rnd = new Random(0);
        for (int i = 0; i < recordCount; i++) {
            Name name = Name.fromString("host" + i + ".example.");
            Record rec = Record.newRecord(name, Type.A, DClass.IN, 300,
                    new byte[]{(byte) rnd.nextInt(256), (byte) rnd.nextInt(256),
                               (byte) rnd.nextInt(256), (byte) rnd.nextInt(256)});
            msg.addRecord(rec, Section.ANSWER);
        }
        return msg;
    }

    /* --------------------------------------------------------------
     * 1. Null Parameters
     * -------------------------------------------------------------- */

    @Test
    void apply_nullMessage_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(null, 0, null),
                "Calling apply with a null Message should throw IllegalArgumentException");
    }

    @Test
    void apply_nullOldCreatesFreshTSIGRecord() throws IOException {
        // The method should succeed and attach a TSIGRecord with the default algorithm.
        tsig.apply(queryMessage, Rcode.NOERROR, null);
        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec, "A TSIGRecord must be present after apply with old == null");
        assertEquals(TSIG.HMAC_SHA256, rec.getAlgorithm(),
                "Default algorithm should be HMAC_SHA256");
        assertEquals(0, rec.getError(),
                "Error field should reflect the supplied error (NOERROR)");
    }

    /* --------------------------------------------------------------
     * 2. Error Code Handling
     * -------------------------------------------------------------- */

    @ParameterizedTest(name = "apply_error_{1}_createsRecordWithError")
    @MethodSource("errorCodeProvider")
    void apply_errorCodePropagation(int errorCode, String description) throws IOException {
        tsig.apply(queryMessage, errorCode, null);
        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec, "TSIGRecord must be created");
        assertEquals(errorCode, rec.getError(),
                "TSIGRecord error field must equal the supplied error code");
    }

    private static Stream<Arguments> errorCodeProvider() {
        return Stream.of(
                Arguments.of(Rcode.BADTIME, "BADTIME error code"),
                Arguments.of(Rcode.BADKEY, "BADKEY error code"),
                Arguments.of(16, "Additional valid DNS error code")
        );
    }

    @Test
    void apply_negativeErrorCode_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), -1, null),
                "Negative error codes must cause IllegalArgumentException");
    }

    @Test
    void apply_errorCodeTooLarge_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), 65535, null),
                "Error codes beyond the protocol maximum must be rejected");
    }

    /* --------------------------------------------------------------
     * 3. Full Signature Flag
     * -------------------------------------------------------------- */

    @Test
    void apply_fullSignatureTrue_generatesFullMac() throws IOException {
        // The public overload defaults to fullSignature = true.
        tsig.apply(queryMessage, Rcode.NOERROR, null);
        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec);
        // The MAC length for HMAC‑SHA256 should be 32 bytes.
        assertEquals(32, rec.getSignature().length,
                "Full signature must contain the complete 32‑byte MAC for SHA‑256");
    }

    @Test
    void apply_fullSignatureFalse_generatesTruncatedMac() throws IOException {
        // Use the overload that accepts the flag.
        tsig.apply(queryMessage, Rcode.NOERROR, null, false);
        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec);
        // According to the TSIG spec the truncated MAC is 10 bytes.
        assertEquals(10, rec.getSignature().length,
                "When fullSignature = false the MAC must be truncated to 10 bytes");
    }

    /* --------------------------------------------------------------
     * 4. Old Record Reuse
     * -------------------------------------------------------------- */

    @Test
    void apply_reusesKeyNameAndAlgorithmFromOldRecord() throws IOException {
        Name oldKey = Name.fromString("custom.example.");
        Name oldAlg = TSIG.HMAC_SHA1;
        Instant oldTime = Instant.now(FIXED_CLOCK);
        TSIGRecord old = createTSIGRecord(oldKey, oldAlg, oldTime, Rcode.NOERROR);

        tsig.apply(queryMessage, Rcode.NOERROR, old);
        TSIGRecord newRec = queryMessage.getTSIG();

        assertNotNull(newRec);
        assertEquals(oldKey, newRec.getName(),
                "New TSIGRecord must reuse the key name from the old record");
        assertEquals(oldAlg, newRec.getAlgorithm(),
                "New TSIGRecord must reuse the algorithm from the old record");
        // Timestamp should be refreshed to the fixed clock time.
        assertEquals(FIXED_CLOCK.instant().getEpochSecond(),
                newRec.getTimeSigned(),
                "Timestamp must be refreshed to the current (fixed) time");
    }

    @Test
    void apply_oldRecordTooOld_throwsIllegalArgumentException() {
        Name key = Name.fromString("example.com.");
        Instant tooOld = Instant.now(FIXED_CLOCK).minusSeconds(TSIG.FUDGE.getSeconds() + 1);
        TSIGRecord old = createTSIGRecord(key, TSIG.HMAC_SHA256, tooOld, Rcode.NOERROR);

        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), Rcode.NOERROR, old),
                "Old TSIGRecord older than the fudge window must be rejected");
    }

    @Test
    void apply_oldRecordExactlyAtFudgeBoundary_isAccepted() throws IOException {
        Name key = Name.fromString("example.com.");
        Instant boundary = Instant.now(FIXED_CLOCK).minusSeconds(TSIG.FUDGE.getSeconds());
        TSIGRecord old = createTSIGRecord(key, TSIG.HMAC_SHA256, boundary, Rcode.NOERROR);

        tsig.apply(new Message(), Rcode.NOERROR, old);
        // No exception means the test passed.
    }

    @Test
    void apply_oldRecordInFuture_throwsIllegalArgumentException() {
        Name key = Name.fromString("example.com.");
        Instant future = Instant.now(FIXED_CLOCK).plusSeconds(10);
        TSIGRecord old = createTSIGRecord(key, TSIG.HMAC_SHA256, future, Rcode.NOERROR);

        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), Rcode.NOERROR, old),
                "Future timestamps must be rejected");
    }

    /* --------------------------------------------------------------
     * 5. Algorithm Name Mapping
     * -------------------------------------------------------------- */

    @Test
    void apply_algorithmNameMD5_usesHmacMD5() throws IOException {
        // Use an old record whose algorithm name maps to MD5.
        Name key = Name.fromString("example.com.");
        TSIGRecord old = createTSIGRecord(key, TSIG.HMAC_MD5, Instant.now(FIXED_CLOCK), Rcode.NOERROR);

        tsig.apply(queryMessage, Rcode.NOERROR, old);
        TSIGRecord rec = queryMessage.getTSIG();
        assertEquals(TSIG.HMAC_MD5, rec.getAlgorithm(),
                "Algorithm should be resolved to HMAC‑MD5");
    }

    @Test
    void apply_unknownAlgorithmName_throwsIllegalArgumentException() {
        // Build a Name that does not correspond to any supported algorithm.
        Name unknownAlg = Name.fromString("unknown-alg.");
        Name key = Name.fromString("example.com.");
        TSIGRecord old = createTSIGRecord(key, unknownAlg, Instant.now(FIXED_CLOCK), Rcode.NOERROR);

        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), Rcode.NOERROR, old),
                "Unsupported algorithm names must cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     * 6. Key Name Validation
     * -------------------------------------------------------------- */

    @Test
    void apply_emptyKeyName_throwsIllegalArgumentException() {
        Name emptyKey = Name.fromString(".");
        TSIGRecord old = createTSIGRecord(emptyKey, TSIG.HMAC_SHA256,
                Instant.now(FIXED_CLOCK), Rcode.NOERROR);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), Rcode.NOERROR, old),
                "Empty key names are invalid");
    }

    @Test
    void apply_nullKeyName_throwsIllegalArgumentException() {
        // Create a TSIGRecord with a null key name via reflection.
        TSIGRecord old = createTSIGRecord(null, TSIG.HMAC_SHA256,
                Instant.now(FIXED_CLOCK), Rcode.NOERROR);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), Rcode.NOERROR, old),
                "Null key name must be rejected");
    }

    @Test
    void apply_invalidKeyNameFormat_throwsIllegalArgumentException() {
        // Name without trailing dot is considered invalid by dnsjava.
        Name badKey = Name.fromString("invalid_name", Name.root);
        TSIGRecord old = createTSIGRecord(badKey, TSIG.HMAC_SHA256,
                Instant.now(FIXED_CLOCK), Rcode.NOERROR);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), Rcode.NOERROR, old),
                "Key name not a fully‑qualified domain name must be rejected");
    }

    /* --------------------------------------------------------------
     * 7. Secret Key Validation
     * -------------------------------------------------------------- */

    @Test
    void apply_emptySecretKey_throwsIllegalArgumentException() {
        // Construct a TSIG with an empty secret key.
        SecretKeySpec emptyKey = new SecretKeySpec(new byte[0], "HmacSHA256");
        TSIG tsigEmpty = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), emptyKey, FIXED_CLOCK);
        assertThrows(IllegalArgumentException.class,
                () -> tsigEmpty.apply(new Message(), Rcode.NOERROR, null),
                "TSIG with empty secret key must reject apply");
    }

    @Test
    void apply_nullSecretKey_throwsIllegalArgumentException() {
        // Construct a TSIG with a null secret key (will throw at construction time).
        assertThrows(NullPointerException.class,
                () -> new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), null, FIXED_CLOCK),
                "Constructing TSIG with null secret key should fail");
    }

    /* --------------------------------------------------------------
     * 8. Message Already Contains TSIG
     * -------------------------------------------------------------- */

    @Test
    void apply_overwritesExistingTSIG() throws IOException {
        // First apply a TSIG (oldMac will be generated automatically).
        tsig.apply(queryMessage, Rcode.NOERROR, null);
        TSIGRecord first = queryMessage.getTSIG();
        byte[] firstMac = first.getSignature();

        // Apply again – the MAC should change and there must still be exactly one TSIG.
        tsig.apply(queryMessage, Rcode.NOERROR, null);
        TSIGRecord second = queryMessage.getTSIG();
        byte[] secondMac = second.getSignature();

        assertNotNull(second);
        assertEquals(1, queryMessage.getSectionCount(Section.ADDITIONAL),
                "Message must contain exactly one TSIG record after overwrite");
        assertFalse(java.util.Arrays.equals(firstMac, secondMac),
                "The new MAC must differ from the previous one (signature overwritten)");
    }

    /* --------------------------------------------------------------
     * 9. Large Message Handling
     * -------------------------------------------------------------- */

    @Test
    void apply_largeMessageSucceeds() throws IOException {
        Message largeMsg = createRandomMessage(1000);
        tsig.apply(largeMsg, Rcode.NOERROR, null);
        TSIGRecord rec = largeMsg.getTSIG();
        assertNotNull(rec, "TSIGRecord must be added to a large message");
        // For SHA‑256 the MAC length is 32 bytes.
        assertEquals(32, rec.getSignature().length,
                "MAC length must match the digest size of the selected algorithm");
    }

    /* --------------------------------------------------------------
     * 10. Algorithm Variety (SHA‑512)
     * -------------------------------------------------------------- */

    @Test
    void apply_sha512Algorithm_generates64ByteMac() throws IOException {
        // Build an old record that forces SHA‑512.
        Name key = Name.fromString("example.org.");
        TSIGRecord old = createTSIGRecord(key, TSIG.HMAC_SHA512,
                Instant.now(FIXED_CLOCK), Rcode.NOERROR);

        tsig.apply(new Message(), Rcode.NOERROR, old);
        TSIGRecord rec = queryMessage.getTSIG();
        // The MAC length for HMAC‑SHA‑512 is 64 bytes.
        assertEquals(64, rec.getSignature().length,
                "SHA‑512 MAC must be 64 bytes long");
    }

    @Test
    void apply_nullAlgorithmInOldRecord_throwsIllegalArgumentException() {
        Name key = Name.fromString("example.org.");
        TSIGRecord old = createTSIGRecord(key, null,
                Instant.now(FIXED_CLOCK), Rcode.NOERROR);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), Rcode.NOERROR, old),
                "Null algorithm in old TSIGRecord must cause failure");
    }

    @Test
    void apply_unsupportedAlgorithmByJCE_throwsIllegalArgumentException() {
        // Use a bogus algorithm name that JCE cannot instantiate.
        Name unsupported = Name.fromString("HMAC-UNSUPPORTED.");
        Name key = Name.fromString("example.com.");
        TSIGRecord old = createTSIGRecord(key, unsupported,
                Instant.now(FIXED_CLOCK), Rcode.NOERROR);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), Rcode.NOERROR, old),
                "Algorithm not supported by JCE must cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     * 11. Default Algorithm When Old Is Null
     * -------------------------------------------------------------- */

    @Test
    void apply_nullOldUsesDefaultAlgorithm() throws IOException {
        tsig.apply(queryMessage, Rcode.NOERROR, null);
        TSIGRecord rec = queryMessage.getTSIG();
        assertEquals(TSIG.HMAC_SHA256, rec.getAlgorithm(),
                "When old is null the default algorithm should be HMAC_SHA256");
    }

    /* --------------------------------------------------------------
     * 12. Timestamp Null Checks
     * -------------------------------------------------------------- */

    @Test
    void apply_nullTimestampInOldRecord_throwsIllegalArgumentException() {
        Name key = Name.fromString("example.com.");
        TSIGRecord old = createTSIGRecord(key, TSIG.HMAC_SHA256, null, Rcode.NOERROR);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(new Message(), Rcode.NOERROR, old),
                "Old record with null timestamp must be rejected");
    }
}
```