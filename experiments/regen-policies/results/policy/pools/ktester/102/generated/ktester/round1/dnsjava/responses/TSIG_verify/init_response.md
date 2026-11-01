```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#verify(Message, byte[], TSIGRecord)}.
 *
 * <p>The tests cover the happy‑path (valid signature) and a few error conditions such as
 * a tampered message and a mismatching key.  A fixed {@link Clock} is used so that the
 * generated TSIG timestamps are deterministic.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_verify_Test {

    /** Fixed clock used for deterministic TSIG timestamps. */
    private static Clock fixedClock;

    /** The TSIG key used for signing and verification. */
    private static TSIG tsigKey;

    /** The raw key bytes (base‑64 decoded) used to build the {@link SecretKey}. */
    private static final byte[] KEY_BYTES = "12345678".getBytes(); // simple test key

    /** Message that will be signed (query). */
    private Message queryMessage;

    /** Wire format of the signed query. */
    private byte[] queryWire;

    /** The TSIG record that was generated on the signed query. */
    private TSIGRecord queryTSIG;

    @BeforeAll
    static void setupBeforeAll() {
        // a deterministic instant – 2023‑01‑01T00:00:00Z
        fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

        // Build a SecretKeySpec with the HMAC‑SHA256 algorithm (the one used by TSIG.HMAC_SHA256)
        SecretKey secretKey = new SecretKeySpec(KEY_BYTES, "HmacSHA256");

        // Construct the TSIG object with the fixed clock so that timestamps are reproducible.
        tsigKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example.", Name.root), secretKey, fixedClock);
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a simple A‑record query.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);

        // Attach the TSIG key – this will cause the TSIG record to be generated.
        queryMessage.setTSIG(tsigKey);

        // Serialize the message (this also finalises the TSIG signature).
        queryWire = queryMessage.toWire(Message.MAXLENGTH);

        // Parse the wire back so that we have a fresh Message instance that contains the TSIGRecord.
        Message parsed = new Message(queryWire);
        queryTSIG = parsed.getTSIG(); // the generated TSIG record
        assertNotNull(queryTSIG, "TSIGRecord should be present after signing");
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable static state to clean up; method present for completeness.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No resources to release.
    }

    /**
     * Verify that a correctly signed message is accepted (returns {@link Rcode#NOERROR}).
     */
    @Test
    void verifyValidSignature() throws IOException {
        // Parse the wire back again – this mimics the receiver side.
        Message received = new Message(queryWire);

        // The original TSIG (generated on the query) is passed as the third argument.
        int result = tsigKey.verify(received, queryWire, queryTSIG);

        assertEquals(Rcode.NOERROR, result, "Valid TSIG should verify with NOERROR");
        assertTrue(received.isSigned(), "Message should be marked as signed");
        assertTrue(received.isVerified(), "Message should be marked as verified");
    }

    /**
     * Tamper with the wire data (flip a single byte) and ensure verification fails.
     */
    @Test
    void verifyTamperedMessageFails() throws IOException {
        // Clone the original wire and corrupt a byte somewhere in the question section.
        byte[] tampered = queryWire.clone();
        // Change the first byte of the QNAME (skip the header – 12 bytes)
        tampered[12] = (byte) (tampered[12] ^ 0xFF);

        Message received = new Message(tampered);
        int result = tsigKey.verify(received, tampered, queryTSIG);

        // Any error code other than NOERROR indicates failure; the exact value is implementation‑defined.
        assertNotEquals(Rcode.NOERROR, result, "Tampered message must not verify");
    }

    /**
     * Verify that using a different TSIG key (wrong secret) results in a verification error.
     */
    @Test
    void verifyWithWrongKeyFails() throws IOException {
        // Build a second TSIG instance with a different secret.
        SecretKey wrongKey = new SecretKeySpec("different".getBytes(), "HmacSHA256");
        TSIG otherTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example.", Name.root), wrongKey, fixedClock);

        // The message was signed with {@code tsigKey}, but we attempt verification with {@code otherTsig}.
        Message received = new Message(queryWire);
        int result = otherTsig.verify(received, queryWire, queryTSIG);

        assertNotEquals(Rcode.NOERROR, result, "Verification with a wrong key must fail");
    }

    /**
     * Verify that a message whose TSIG timestamp is outside the allowed fudge window
     * is rejected.  We create a TSIGRecord with a timestamp far in the past.
     */
    @Test
    void verifyExpiredTimestampFails() throws IOException {
        // Create a TSIGRecord manually with an old timestamp (1 hour ago).
        Instant oldInstant = fixedClock.instant().minus(Duration.ofHours(1));
        TSIGRecord oldTsigRecord = new TSIGRecord(
                queryTSIG.getName(),
                queryTSIG.getDClass(),
                queryTSIG.getTTL(),
                queryTSIG.getAlgorithm(),
                oldInstant,
                TSIG.FUDGE,
                queryTSIG.getSignature(),
                queryTSIG.getOriginalID(),
                queryTSIG.getError(),
                queryTSIG.getOther()
        );

        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, oldTsigRecord);

        assertNotEquals(Rcode.NOERROR, result, "Expired TSIG timestamp should cause verification failure");
    }
}
```