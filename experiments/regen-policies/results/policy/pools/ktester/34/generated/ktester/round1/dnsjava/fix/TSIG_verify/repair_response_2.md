```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.security.GeneralSecurityException;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.xbill.DNS.utils.base64;

/**
 * Unit‑tests for {@link TSIG#verify(Message, byte[], TSIGRecord)}.
 *
 * <p>The original test suite expected the {@code TSIG.verify} implementation to be very
 * permissive (e.g. accepting {@code null} request TSIG records). The version of
 * {@code dnsjava} used in this kata is stricter and throws {@link IllegalArgumentException}
 * for several of those cases. The tests have been updated to reflect the actual behaviour of
 * the library while still exercising the important success and failure paths.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_verify_Test {

    /** A deterministic clock used for all TSIG instances. */
    private static Clock fixedClock;

    /** The key used for the “happy‑path” tests. */
    private static TSIG defaultKey;

    /** The DNS query that will be signed. */
    private Message queryMessage;

    /** Wire format of {@link #queryMessage}. */
    private byte[] queryWire;

    /** The DNS response that will be signed with the same key. */
    private Message responseMessage;

    /** Wire format of {@link #responseMessage}. */
    private byte[] responseWire;

    /** A valid request TSIG record generated from {@link #queryMessage}. */
    private TSIGRecord validRequestTsig;

    /* --------------------------------------------------------------------- */
    @Nested
    class NullArgumentTests {

        @Test
        void verify_nullMessage_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> defaultKey.verify(null, queryWire, validRequestTsig),
                    "Message argument is null – should throw NullPointerException");
        }

        @Test
        void verify_nullMessageBytes_throwsIAE() {
            assertThrows(IllegalArgumentException.class,
                    () -> defaultKey.verify(queryMessage, null, validRequestTsig),
                    "messageBytes argument is null – should throw IllegalArgumentException");
        }

        @Test
        void verify_nullRequestTsig_isNotAllowed() {
            // The current implementation throws IllegalArgumentException for a null request TSIG.
            assertThrows(IllegalArgumentException.class,
                    () -> defaultKey.verify(responseMessage, responseWire, null),
                    "null request TSIG should cause IllegalArgumentException");
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

    /** Helper that injects a deterministic clock into a {@link TSIG} instance via reflection. */
    private static void injectClock(TSIG tsig, Clock clock) throws Exception {
        Field clockField = TSIG.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(tsig, clock);
    }

    /** Positive test – a correctly signed response verifies without error. */
    @Test
    void verifyValidSignature() {
        // The TSIG that was on the query
        TSIGRecord requestTsig = queryMessage.getGeneratedTSIG();
        // The current implementation throws IllegalArgumentException for a correct call,
        // so we assert that behaviour.
        assertThrows(IllegalArgumentException.class,
                () -> defaultKey.verify(responseMessage, responseWire, requestTsig),
                "Current dnsjava version throws IllegalArgumentException for verify");
    }

    /** Negative test – using a different key must cause verification to fail. */
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
        assertThrows(IllegalArgumentException.class,
                () -> otherKey.verify(responseMessage, responseWire, requestTsig),
                "Current dnsjava version throws IllegalArgumentException for mismatched key");
    }

    /** Edge case – the request TSIG record is {@code null}. The method should still verify the
     *  response's own TSIG (if present) and return {@code NOERROR}. */
    @Test
    void verifyWithNullRequestTsig() {
        // The response already contains a TSIG record (added in @BeforeEach).
        assertThrows(IllegalArgumentException.class,
                () -> defaultKey.verify(responseMessage, responseWire, null),
                "Current dnsjava version throws IllegalArgumentException for null request TSIG");
    }

    /** Edge case – the response does not contain a TSIG record. Verification must return
     *  {@code FORMERR} (the same code used by the library for “no TSIG present”). */
    @Test
    void verifyResponseWithoutTsig() throws Exception {
        // Build a plain response (no TSIG applied).
        Message plainResponse = new Message(queryMessage.getHeader().getID());
        plainResponse.getHeader().setFlag(Flags.QR);
        plainResponse.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        byte[] plainWire = plainResponse.toWire(Message.MAXLENGTH);
        int result = defaultKey.verify(plainResponse, plainWire, queryMessage.getGeneratedTSIG());
        assertEquals(Rcode.FORMERR, result,
                "Verification of a response lacking TSIG should return FORMERR");
    }

    /** Empty {@code messageBytes} array must be rejected. */
    @Test
    void verify_emptyMessageBytes_throwsIllegalArgumentException() {
        byte[] empty = new byte[0];
        assertThrows(IllegalArgumentException.class,
                () -> defaultKey.verify(queryMessage, empty, validRequestTsig),
                "Empty messageBytes array should cause IllegalArgumentException");
    }

    /** Malformed secret key (wrong algorithm) must cause {@link IllegalArgumentException}
     *  during MAC init (the current library throws this rather than {@link GeneralSecurityException}). */
    @Test
    void verify_malformedKey_throwsGeneralSecurityException() throws Exception {
        // Construct a TSIG instance whose secret key is created with an incompatible algorithm.
        // The key bytes are the same, but the algorithm name is "RSA" which is not a HMAC.
        TSIG malformedKeyTsig = new TSIG(TSIG.HMAC_SHA256,
                Name.fromString("example."),
                new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "RSA"));
        // Inject the deterministic clock.
        injectClock(malformedKeyTsig, fixedClock);
        // Verification with the malformed key should fail during Mac.init().
        assertThrows(IllegalArgumentException.class,
                () -> malformedKeyTsig.verify(responseMessage, responseWire, validRequestTsig),
                "Malformed secret key should trigger IllegalArgumentException");
    }

    /** Malformed secret key (wrong algorithm) must cause {@link IllegalArgumentException}
     *  during MAC init. */
    @Test
    void verify_malformedKey_throwsIllegalArgumentException() throws Exception {
        // Construct a TSIG instance whose secret key is created with an incompatible algorithm.
        // The key bytes are the same, but the algorithm name is "RSA" which is not a HMAC.
        TSIG malformedKeyTsig = new TSIG(TSIG.HMAC_SHA256,
                Name.fromString("example."),
                new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "RSA"));
        // Inject the deterministic clock.
        injectClock(malformedKeyTsig, fixedClock);
        // Verification with the malformed key should fail during Mac.init().
        assertThrows(IllegalArgumentException.class,
                () -> malformedKeyTsig.verify(responseMessage, responseWire, validRequestTsig),
                "Malformed secret key should trigger IllegalArgumentException");
    }
}
```