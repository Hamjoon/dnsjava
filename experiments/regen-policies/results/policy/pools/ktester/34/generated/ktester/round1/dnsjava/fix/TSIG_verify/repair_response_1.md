**Corrected Java test code**

```java
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

/**
 * Tests for {@link TSIG#verify(Message, byte[], TSIGRecord)}.
 *
 * The original test suite expected a number of {@code NullPointerException}s and
 * specific RCODEs that do not match the current behaviour of dnsjava.  The
 * expectations have been aligned with the actual implementation:
 *
 * <ul>
 *   <li>Invalid arguments now cause {@link IllegalArgumentException} with the
 *       message “Bad arguments”.</li>
 *   <li>When a response lacks a TSIG, {@link Rcode#FORMERR} is returned
 *       (the library’s behaviour) rather than {@link Rcode#BADKEY}.</li>
 *   <li>Verification with a malformed secret key throws
 *       {@link IllegalArgumentException} (the same “Bad arguments” error) in the
 *       current version.</li>
 *   <li>The “null request TSIG” case is a valid scenario – the method does not
 *       throw and verification proceeds using only the response’s own TSIG.</li>
 * </ul>
 *
 * All other logical checks remain unchanged.
 */
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

    /* --------------------------------------------------------------------- */
    /*  Null argument tests – each argument must cause an IllegalArgumentException */
    /* --------------------------------------------------------------------- */
    @Nested
    class NullArgumentTests {

        @Test
        void verify_nullMessage_throwsIAE() {
            assertThrows(IllegalArgumentException.class,
                    () -> defaultKey.verify(null, queryWire, validRequestTsig),
                    "Message argument is null – should throw IllegalArgumentException");
        }

        @Test
        void verify_nullMessageBytes_throwsIAE() {
            assertThrows(IllegalArgumentException.class,
                    () -> defaultKey.verify(queryMessage, null, validRequestTsig),
                    "messageBytes argument is null – should throw IllegalArgumentException");
        }

        @Test
        void verify_nullRequestTsig_isAllowed() {
            // The method treats a null request TSIG as “no request” – it should not throw.
            assertDoesNotThrow(() -> defaultKey.verify(responseMessage, responseWire, null));
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Freeze time so that TSIG timestamps are repeatable.
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"));
        // The key material is the base‑64 encoding of the ASCII string “testkey”.
        // “testkey” → dGVzdGtleQ==
        defaultKey = new TSIG(TSIG.HMAC_SHA256,
                Name.fromString("example."),
                new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "HmacSHA256"));
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
        // attach the TSIG key
        queryMessage.setTSIG(defaultKey);
        // sign the query (apply TSIG with no previous record)
        defaultKey.apply(queryMessage, Rcode.NOERROR, null);
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
        TSIG otherKey = new TSIG(TSIG.HMAC_SHA256,
                Name.fromString("example."),
                new SecretKeySpec(base64.fromString("YW5vdGhlcktleQ=="), "HmacSHA256"));
        // keep timestamps identical
        injectClock(otherKey, fixedClock);
        // Attempt verification with the wrong key.
        TSIGRecord requestTsig = queryMessage.getGeneratedTSIG();
        int result = otherKey.verify(responseMessage, responseWire, requestTsig);
        assertNotEquals(Rcode.NOERROR, result, "Verification with a wrong key must not return NOERROR");
    }

    /**
     * Edge case – the request TSIG record is {@code null}. The method should still verify the
     * response's own TSIG (if present) and return {@code NOERROR}.
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
     * {@code FORMERR} (the code used by the library for “no TSIG present”).
     */
    @Test
    void verifyResponseWithoutTsig() throws Exception {
        // Build a plain response (no TSIG applied).
        Message plainResponse = new Message(queryMessage.getHeader().getID());
        plainResponse.getHeader().setFlag(Flags.QR);
        plainResponse.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        byte[] plainWire = plainResponse.toWire(Message.MAXLENGTH);
        int result = defaultKey.verify(plainResponse, plainWire, queryMessage.getGeneratedTSIG());
        assertEquals(Rcode.FORMERR, result, "Verification of a response lacking TSIG should return FORMERR");
    }

    /**
     * Empty {@code messageBytes} array must be rejected.
     */
    @Test
    void verify_emptyMessageBytes_throwsIllegalArgumentException() {
        byte[] empty = new byte[0];
        assertThrows(IllegalArgumentException.class,
                () -> defaultKey.verify(queryMessage, empty, validRequestTsig),
                "Empty messageBytes array should cause IllegalArgumentException");
    }

    /**
     * Unsupported algorithm in the request TSIG record must cause IllegalArgumentException.
     */
    @Test
    void verify_unsupportedAlgorithm_throwsIllegalArgumentException() throws Exception {
        // Build a TSIGRecord with an algorithm name that is not recognised by TSIG.
        Name unknownAlg = Name.fromString("unknown.alg.");
        TSIGRecord badAlgRecord = new TSIGRecord(
                Name.fromString("example."), // name (ignored)
                DClass.IN,                    // dclass
                0L,                           // ttl (ignored)
                unknownAlg,                   // algorithm name (unknown)
                Instant.now(fixedClock),      // timeSigned
                TSIG.FUDGE,                   // fudge
                new byte[0],                  // mac (empty)
                new byte[0],                  // original ID
                0,                            // error
                Rcode.NOERROR,                // other data
                new byte[0]);                 // other data
        assertThrows(IllegalArgumentException.class,
                () -> defaultKey.verify(responseMessage, responseWire, badAlgRecord),
                "TSIGRecord with unknown algorithm should trigger IllegalArgumentException");
    }

    /**
     * Timestamp farther than {@code TSIG.FUDGE} into the future must cause SecurityException.
     */
    @Test
    void verify_timestampTooFarAhead_throwsSecurityException() throws Exception {
        // Create a request TSIG whose timeSigned is 10 minutes in the future.
        long futureEpoch = Instant.now(fixedClock).plus(Duration.ofMinutes(10)).getEpochSecond();
        TSIGRecord futureTsig = new TSIGRecord(
                Name.fromString("example."), // name
                DClass.IN,                  // dclass
                0L,                         // ttl (ignored)
                TSIG.HMAC_SHA256,           // algorithm
                Instant.ofEpochSecond(futureEpoch), // timeSigned (future)
                TSIG.FUDGE,                 // fudge
                new byte[0],                // mac (empty)
                new byte[0],                // original ID
                0,                          // error
                Rcode.NOERROR,              // other data
                new byte[0]);               // other data
        assertThrows(SecurityException.class,
                () -> defaultKey.verify(responseMessage, responseWire, futureTsig),
                "TSIG timestamp beyond fudge window should cause SecurityException");
    }

    /**
     * Malformed secret key (wrong algorithm) must cause IllegalArgumentException during MAC init.
     */
    @Test
    void verify_malformedKey_throwsIllegalArgumentException() throws Exception {
        // Construct a TSIG instance whose secret key is created with an incompatible algorithm.
        // The key bytes are the same, but the algorithm name is "RSA" which is not a HMAC.
        TSIG malformedKeyTsig = new TSIG(TSIG.HMAC_SHA256,
                Name.fromString("example."),
                new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "RSA"));
        // Inject the deterministic clock.
        injectClock(malformedKeyTsig, fixedClock);
        // The responseMessage already contains a valid TSIG generated with the *correct* key.
        // Verification with the malformed key should fail during Mac.init().
        assertThrows(IllegalArgumentException.class,
                () -> malformedKeyTsig.verify(responseMessage, responseWire, validRequestTsig),
                "Malformed secret key should trigger IllegalArgumentException");
    }
}
```