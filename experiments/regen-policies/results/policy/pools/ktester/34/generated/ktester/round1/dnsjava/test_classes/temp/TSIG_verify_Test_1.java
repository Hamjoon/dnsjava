package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xbill.DNS.utils.base64;
import java.security.GeneralSecurityException;

@ExtendWith(MockitoExtension.class)
class TSIG_verify_Test {

    /**
     * A deterministic clock used for all TSIG instances.
     */
    private static Clock fixedClock;

    /**
     * The key used for the “happy‑path” tests.
     */
    private static TSIG defaultKey;

    /**
     * The DNS query that will be signed.
     */
    private Message queryMessage;

    /**
     * Wire format of {@link #queryMessage}.
     */
    private byte[] queryWire;

    /**
     * The DNS response that will be signed with the same key.
     */
    private Message responseMessage;

    /**
     * Wire format of {@link #responseMessage}.
     */
    private byte[] responseWire;

    /**
     * A valid request TSIG record generated from {@link #queryMessage}.
     */
    private TSIGRecord validRequestTsig;

    /**
     * Null argument tests – each argument must cause a NullPointerException.
     */
    @Nested
    class NullArgumentTests {

        @Test
        void verify_nullMessage_throwsNPE() {
            assertThrows(NullPointerException.class, () -> defaultKey.verify(null, queryWire, validRequestTsig), "Message argument is null – should throw NullPointerException");
        }

        @Test
        void verify_nullMessageBytes_throwsNPE() {
            assertThrows(NullPointerException.class, () -> defaultKey.verify(queryMessage, null, validRequestTsig), "messageBytes argument is null – should throw NullPointerException");
        }

        @Test
        void verify_nullRequestTsig_throwsNPE() {
            // According to the JSON spec this case should throw NPE.
            assertThrows(NullPointerException.class, () -> defaultKey.verify(queryMessage, queryWire, null), "requestTSIG argument is null – should throw NullPointerException");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Freeze time so that TSIG timestamps are repeatable.
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"));
        // The key material is the base‑64 encoding of the ASCII string “testkey”.
        // “testkey” → dGVzdGtleQ==
        defaultKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "HmacSHA256"));
        // inject the deterministic clock via reflection
        injectClock(defaultKey, fixedClock);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // Build a signed query
        // --------------------------------------------------------------------
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        // adds a TSIG record
        queryMessage.setTSIG(defaultKey);
        // wire format for verification
        queryWire = queryMessage.toWire(Message.MAXLENGTH);
        validRequestTsig = queryMessage.getGeneratedTSIG();
        // --------------------------------------------------------------------
        // Build a signed response that references the query's TSIG
        // --------------------------------------------------------------------
        responseMessage = new Message(queryMessage.getHeader().getID());
        // mark as a response
        responseMessage.getHeader().setFlag(Flags.QR);
        responseMessage.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // The TSIG.apply method adds a TSIG record to the response, using the request
        // TSIG (the one we just generated) as the “previous” record.
        defaultKey.apply(responseMessage, Rcode.NOERROR, validRequestTsig);
        responseWire = responseMessage.toWire(Message.MAXLENGTH);
    }

    /**
     * Helper that injects a deterministic clock into a {@link TSIG} instance via reflection.
     */
    private static void injectClock(TSIG tsig, Clock clock) throws Exception {
        Field clockField = TSIG.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(tsig, clock);
    }

    /**
     * Positive test – a correctly signed response verifies without error.
     */
    @Test
    void verifyValidSignature() {
        // the TSIG that was on the query
        TSIGRecord requestTsig = queryMessage.getGeneratedTSIG();
        int result = defaultKey.verify(responseMessage, responseWire, requestTsig);
        assertEquals(Rcode.NOERROR, result, "Valid TSIG should verify with NOERROR");
        assertTrue(responseMessage.isSigned(), "Response should be marked as signed");
        assertTrue(responseMessage.isVerified(), "Response should be marked as verified");
    }

    /**
     * Negative test – using a different key must cause verification to fail.
     */
    @Test
    void verifyWithWrongKeyFails() throws Exception {
        // Create a second key with different secret material.
        TSIG otherKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(base64.fromString("YW5vdGhlcktleQ=="), "HmacSHA256"));
        // keep timestamps identical
        injectClock(otherKey, fixedClock);
        // Attempt verification with the wrong key.
        TSIGRecord requestTsig = queryMessage.getGeneratedTSIG();
        int result = otherKey.verify(responseMessage, responseWire, requestTsig);
        assertNotEquals(Rcode.NOERROR, result, "Verification with a wrong key must not return NOERROR");
    }

    /**
     * Edge case – the request TSIG record is {@code null}. The method should still verify the
     *  response's own TSIG (if present) and return {@code NOERROR}.
     */
    @Test
    void verifyWithNullRequestTsig() {
        // The response already contains a TSIG record (added in @BeforeEach).
        int result = defaultKey.verify(responseMessage, responseWire, null);
        assertEquals(Rcode.NOERROR, result, "Verification without a request TSIG should succeed");
        assertTrue(responseMessage.isSigned());
        assertTrue(responseMessage.isVerified());
    }

    /**
     * Edge case – the response does not contain a TSIG record. Verification must return
     *  {@code BADKEY} (the same code used by the library for “no TSIG present”).
     */
    @Test
    void verifyResponseWithoutTsig() throws Exception {
        // Build a plain response (no TSIG applied).
        Message plainResponse = new Message(queryMessage.getHeader().getID());
        plainResponse.getHeader().setFlag(Flags.QR);
        plainResponse.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        byte[] plainWire = plainResponse.toWire(Message.MAXLENGTH);
        int result = defaultKey.verify(plainResponse, plainWire, queryMessage.getGeneratedTSIG());
        assertEquals(Rcode.BADKEY, result, "Verification of a response lacking TSIG should return BADKEY");
    }

    /**
     * Empty {@code messageBytes} array must be rejected.
     */
    @Test
    void verify_emptyMessageBytes_throwsIllegalArgumentException() {
        byte[] empty = new byte[0];
        assertThrows(IllegalArgumentException.class, () -> defaultKey.verify(queryMessage, empty, validRequestTsig), "Empty messageBytes array should cause IllegalArgumentException");
    }

    /**
     * Unsupported algorithm in the request TSIG record must cause IllegalArgumentException.
     */
    @Test
    void verify_unsupportedAlgorithm_throwsIllegalArgumentException() throws Exception {
        // Build a TSIGRecord with an algorithm name that is not recognised by TSIG.
        Name unknownAlg = Name.fromString("unknown.alg.");
        TSIGRecord badAlgRecord = new TSIGRecord(// name (ignored)
        Name.fromString("example."), // dclass
        DClass.IN, // ttl
        0L, // algorithm (unknown)
        unknownAlg, // timeSigned
        Instant.now(fixedClock), // fudge
        TSIG.FUDGE, // mac (empty)
        new byte[0], // original ID
        0, // error
        Rcode.NOERROR, // other data
        new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> defaultKey.verify(responseMessage, responseWire, badAlgRecord), "TSIGRecord with unknown algorithm should trigger IllegalArgumentException");
    }

    /**
     * Timestamp farther than {@code TSIG.FUDGE} into the future must cause SecurityException.
     */
    @Test
    void verify_timestampTooFarAhead_throwsSecurityException() throws Exception {
        // Create a request TSIG whose timeSigned is 10 minutes in the future.
        long futureEpoch = Instant.now(fixedClock).plus(Duration.ofMinutes(10)).getEpochSecond();
        TSIGRecord futureTsig = new TSIGRecord(// name
        Name.fromString("example."), // dclass
        DClass.IN, // ttl
        0L, // algorithm
        TSIG.HMAC_SHA256, // timeSigned (future)
        Instant.ofEpochSecond(futureEpoch), // fudge
        TSIG.FUDGE, // mac
        new byte[0], // original ID
        0, // error
        Rcode.NOERROR, // other data
        new byte[0]);
        assertThrows(SecurityException.class, () -> defaultKey.verify(responseMessage, responseWire, futureTsig), "TSIG timestamp beyond fudge window should cause SecurityException");
    }

    /**
     * Malformed secret key (wrong algorithm) must cause GeneralSecurityException during MAC init.
     */
    @Test
    void verify_malformedKey_throwsGeneralSecurityException() throws Exception {
        // Construct a TSIG instance whose secret key is created with an incompatible algorithm.
        // The key bytes are the same, but the algorithm name is "RSA" which is not a HMAC.
        TSIG malformedKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "RSA"));
        // Inject the deterministic clock.
        injectClock(malformedKeyTsig, fixedClock);
        // The responseMessage already contains a valid TSIG generated with the *correct* key.
        // Verification with the malformed key should fail during Mac.init().
        assertThrows(GeneralSecurityException.class, () -> malformedKeyTsig.verify(responseMessage, responseWire, validRequestTsig), "Malformed secret key should trigger GeneralSecurityException");
    }
}
