```java
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

/**
 * Unit‑tests for {@link TSIG#verify(Message, byte[], TSIGRecord, boolean)}.
 *
 * <p>The tests cover the happy‑path (valid signature) and a failure case
 * (tampered message).  A fixed {@link Clock} is used so that the
 * {@code fudge} window does not affect the outcome.</p>
 */
@ExtendWith(MockitoExtension.class)
class TSIG_verify_2_Test {

    /** Algorithm to use for the TSIG key. */
    private static final Name ALG = TSIG.HMAC_SHA256;
    /** Name of the TSIG key. */
    private static final Name KEY_NAME = Name.fromString("example.");
    /** Base‑64 encoded key (the same value used in existing TSIGTest). */
    private static final String KEY_STRING = "12345678";

    /** The TSIG instance under test. */
    private static TSIG tsig;

    /** Fixed clock to make the timestamp deterministic. */
    private static Clock fixedClock;

    @BeforeAll
    static void setupBeforeAll() {
        // a deterministic instant – 2020‑01‑01T00:00:00Z
        fixedClock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        // construct TSIG with the fixed clock (uses the constructor that accepts a Clock)
        tsig = new TSIG(ALG, KEY_NAME, new SecretKeySpec(base64.fromString(KEY_STRING), "HmacSHA256"), fixedClock);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Verify that a correctly signed query is accepted.
     */
    @Test
    void verify_validSignature_returnsNoError() throws IOException {
        // Build a simple query message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(tsig);                       // sign the message

        // Serialize the signed message.
        byte[] wire = query.toWire(Message.MAXLENGTH);

        // Parse it back – this mimics what a resolver would receive.
        Message parsed = new Message(wire);

        // The public verify method (fullSignature defaults to true) should succeed.
        int result = tsig.verify(parsed, wire, null);
        assertEquals(Rcode.NOERROR, result, "Valid TSIG should verify with NOERROR");
        assertTrue(parsed.isSigned(), "Parsed message should be marked as signed");
        assertTrue(parsed.isVerified(), "Parsed message should be marked as verified");
    }

    /**
     * Verify that a tampered message fails verification.
     */
    @Test
    void verify_tamperedMessage_returnsError() throws IOException {
        // Create and sign a query as in the happy‑path test.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(tsig);
        byte[] originalWire = query.toWire(Message.MAXLENGTH);

        // Tamper with a single byte (do not touch the TSIG signature bytes directly;
        // flipping any earlier byte is enough to break the MAC).
        byte[] tamperedWire = originalWire.clone();
        tamperedWire[10] ^= 0xFF;   // arbitrary mutation

        // Parse the tampered bytes.
        Message parsedTampered = new Message(tamperedWire);

        // Verification must now fail – the exact Rcode is implementation‑defined,
        // but it must not be NOERROR.
        int result = tsig.verify(parsedTampered, tamperedWire, null);
        assertNotEquals(Rcode.NOERROR, result,
                "Tampered TSIG should not verify successfully");
    }

    /**
     * Verify the {@code fullSignature = false} overload.  The behaviour is the same
     * for a valid message, but the test ensures the overload can be called without
     * throwing an unexpected exception.
     */
    @Test
    void verify_fullSignatureFalse_validMessage() throws IOException {
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(tsig);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);

        int result = tsig.verify(parsed, wire, null, false);
        assertEquals(Rcode.NOERROR, result,
                "verify(..., fullSignature=false) should still succeed for a valid message");
    }
}
```