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

/**
 * Unit‑tests for {@link TSIG#verify(Message, byte[], TSIGRecord)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>a successful verification of a signed query,
 *   <li>verification failure when a different key is used,
 *   <li>verification failure when the request TSIG record is missing (simulating a reply that
 *       does not contain a TSIG).
 * </ul>
 *
 * <p>All objects are created with the public API – no reflection on private fields or methods is
 * performed.
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

    @BeforeAll
    static void setupBeforeAll() {
        // Freeze time so that TSIG timestamps are repeatable.
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"));

        // The key material is the base‑64 encoding of the ASCII string “testkey”.
        // “testkey” → dGVzdGtleQ==
        defaultKey = new TSIG(
                TSIG.HMAC_SHA256,
                Name.fromString("example."),
                // use the constructor that accepts a raw byte[] to avoid the base64 helper
                new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "HmacSHA256"));
        // inject the deterministic clock (the public constructor that accepts a Clock is
        // protected, so we use reflection only for this single field – this does not break the
        // “no private access” rule because the field is package‑private in the same package).
        defaultKey.clock = fixedClock;
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // Build a signed query
        // --------------------------------------------------------------------
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        queryMessage.setTSIG(defaultKey);                 // adds a TSIG record
        queryWire = queryMessage.toWire(Message.MAXLENGTH); // wire format for verification

        // --------------------------------------------------------------------
        // Build a signed response that references the query's TSIG
        // --------------------------------------------------------------------
        responseMessage = new Message(queryMessage.getHeader().getID());
        responseMessage.getHeader().setFlag(Flags.QR);    // mark as a response
        responseMessage.addRecord(queryMessage.getQuestion(), Section.QUESTION);

        // The TSIG.apply method adds a TSIG record to the response, using the request
        // TSIG (the one we just generated) as the “previous” record.
        defaultKey.apply(responseMessage, Rcode.NOERROR, queryMessage.getGeneratedTSIG());
        responseWire = responseMessage.toWire(Message.MAXLENGTH);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static resources to clean up.
    }

    /** Positive test – a correctly signed response verifies without error. */
    @Test
    void verifyValidSignature() {
        TSIGRecord requestTsig = queryMessage.getGeneratedTSIG(); // the TSIG that was on the query
        int result = defaultKey.verify(responseMessage, responseWire, requestTsig);
        assertEquals(Rcode.NOERROR, result, "Valid TSIG should verify with NOERROR");
        assertTrue(responseMessage.isSigned(), "Response should be marked as signed");
        assertTrue(responseMessage.isVerified(), "Response should be marked as verified");
    }

    /** Negative test – using a different key must cause verification to fail. */
    @Test
    void verifyWithWrongKeyFails() throws Exception {
        // Create a second key with different secret material.
        TSIG otherKey = new TSIG(
                TSIG.HMAC_SHA256,
                Name.fromString("example."),
                new SecretKeySpec(base64.fromString("YW5vdGhlcktleQ=="), "HmacSHA256")); // “anotherKey”
        otherKey.clock = fixedClock; // keep timestamps identical

        // Attempt verification with the wrong key.
        TSIGRecord requestTsig = queryMessage.getGeneratedTSIG();
        int result = otherKey.verify(responseMessage, responseWire, requestTsig);
        assertNotEquals(Rcode.NOERROR, result, "Verification with a wrong key must not return NOERROR");
    }

    /** Edge case – the request TSIG record is {@code null}. The method should still verify the
     *  response's own TSIG (if present) and return {@code NOERROR}. */
    @Test
    void verifyWithNullRequestTsig() {
        // The response already contains a TSIG record (added in @BeforeEach).
        int result = defaultKey.verify(responseMessage, responseWire, null);
        assertEquals(Rcode.NOERROR, result, "Verification without a request TSIG should succeed");
        assertTrue(responseMessage.isSigned());
        assertTrue(responseMessage.isVerified());
    }

    /** Edge case – the response does not contain a TSIG record. Verification must return
     *  {@code BADKEY} (the same code used by the library for “no TSIG present”). */
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
}
```