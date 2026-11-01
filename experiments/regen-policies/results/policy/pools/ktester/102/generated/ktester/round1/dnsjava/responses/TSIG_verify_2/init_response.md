```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#verify(Message, byte[], TSIGRecord, boolean)} (the overload that
 * ultimately delegates to the private verification implementation).
 *
 * The tests are kept in the same package as {@code TSIG} so that the package‑private
 * {@code verify(...,boolean)} method is accessible.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_verify_2_Test {

    /** A deterministic clock used for TSIG construction – ensures reproducible timestamps. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /** The TSIG key used for all test messages. */
    private static TSIG tsigKey;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a TSIG instance with a known algorithm, name and key.
        // Using the same key as the upstream test suite (example., 12345678).
        tsigKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");
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
        // No global cleanup required.
    }

    /**
     * Verify that a correctly signed query is accepted (returns {@link Rcode#NOERROR}).
     */
    @Test
    void verify_validSignature_returnsNoError() throws IOException {
        // Build a simple query.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(tsigKey);                     // sign the query

        // Serialise and re‑parse to simulate a real network round‑trip.
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);

        // The TSIG record is attached to the parsed message.
        TSIGRecord tsigRecord = parsed.getTSIG();

        // Verify using the public overload that forwards to the package‑private method.
        int result = tsigKey.verify(parsed, wire, tsigRecord, true);
        assertEquals(Rcode.NOERROR, result, "Valid TSIG should verify with NOERROR");
        assertTrue(parsed.isSigned(), "Message should be marked as signed");
        assertTrue(parsed.isVerified(), "Message should be marked as verified");
    }

    /**
     * Verify that tampering with the signed message causes verification to fail.
     */
    @Test
    void verify_tamperedMessage_returnsError() throws IOException {
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(tsigKey);
        byte[] wire = query.toWire(Message.MAXLENGTH);

        // Corrupt a byte that is *not* part of the TSIG record (e.g., the first byte of the header).
        wire[0] ^= 0xFF;

        Message parsed = new Message(wire);
        TSIGRecord tsigRecord = parsed.getTSIG();

        int result = tsigKey.verify(parsed, wire, tsigRecord, true);
        assertNotEquals(Rcode.NOERROR, result, "Tampered message should not verify");
    }

    /**
     * Verify that the overload which accepts a {@code null} request TSIG works for a signed
     * query (the original library permits a {@code null} argument for queries).
     */
    @Test
    void verify_nullRequestTSIG_returnsNoError() throws IOException {
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(tsigKey);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);

        // Pass {@code null} as the request TSIG – this mirrors the behaviour exercised in
        // the upstream test suite.
        int result = tsigKey.verify(parsed, wire, null);
        assertEquals(Rcode.NOERROR, result, "Verification with null requestTSIG should succeed");
    }

    /**
     * Verify the behaviour when {@code fullSignature} is {@code false}. In this mode the
     * verification checks only the MAC over the message (excluding the TSIG record's
     * signature field). For a correctly signed message the result should still be
     * {@link Rcode#NOERROR}.
     */
    @Test
    void verify_fullSignatureFalse_returnsNoError() throws IOException {
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(tsigKey);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        TSIGRecord tsigRecord = parsed.getTSIG();

        // Call the package‑private overload directly with fullSignature = false.
        int result = tsigKey.verify(parsed, wire, tsigRecord, false);
        assertEquals(Rcode.NOERROR, result, "Verification with fullSignature=false should still succeed");
    }
}
```