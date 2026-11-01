```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#verify(Message, byte[], TSIGRecord, boolean)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Successful verification when the full signature is required.</li>
 *   <li>Successful verification when the full signature flag is {@code false}.</li>
 *   <li>Verification failure when the wire data is tampered with.</li>
 * </ul>
 *
 * The {@code TSIG} instance is created with a deterministic {@link Clock} so that the
 * {@code timeSigned} field of the generated {@link TSIGRecord} is predictable.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_verify_2_Test {

    /** Fixed clock used for deterministic TSIG timestamps. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

    /** A TSIG key used for signing and verification. */
    private static TSIG tsigKey;

    /** A simple A‑record used as the question section of the test messages. */
    private static Record questionRecord;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Use the SHA‑256 algorithm (the most common default in the library)
        tsigKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");
        // Replace the internal clock with a fixed one so that the generated
        // TSIGRecord has a known timeSigned value.
        java.lang.reflect.Field clockField = TSIG.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(tsigKey, FIXED_CLOCK);

        // Prepare a generic DNS question record.
        questionRecord = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
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
     * Helper that creates a signed query message, returns both the parsed {@link Message}
     * and the raw wire representation.
     */
    private static ParsedMessage signQuery() throws IOException {
        Message query = Message.newQuery(questionRecord);
        // The Message#setTSIG method signs the message using the supplied TSIG key.
        query.setTSIG(tsigKey);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        return new ParsedMessage(parsed, wire);
    }

    /** Simple holder for a parsed message and its wire representation. */
    private static final class ParsedMessage {
        final Message message;
        final byte[] wire;

        ParsedMessage(Message message, byte[] wire) {
            this.message = message;
            this.wire = wire;
        }
    }

    @Test
    void verifyFullSignatureSucceeds() throws Exception {
        ParsedMessage pm = signQuery();

        // The request TSIG record is the one that was attached to the original query.
        TSIGRecord requestTSIG = pm.message.getTSIG();

        int result = tsigKey.verify(pm.message, pm.wire, requestTSIG, true);
        assertEquals(Rcode.NOERROR, result, "Verification should succeed with fullSignature=true");
        assertTrue(pm.message.isSigned(), "Message should be marked as signed");
        assertTrue(pm.message.isVerified(), "Message should be marked as verified");
    }

    @Test
    void verifyWithoutFullSignatureSucceeds() throws Exception {
        ParsedMessage pm = signQuery();

        TSIGRecord requestTSIG = pm.message.getTSIG();

        // The library treats fullSignature=false as “only verify the MAC over the
        // non‑TSIG part”. For a correctly signed message this still succeeds.
        int result = tsigKey.verify(pm.message, pm.wire, requestTSIG, false);
        assertEquals(Rcode.NOERROR, result, "Verification should succeed with fullSignature=false");
        assertTrue(pm.message.isSigned(), "Message should be marked as signed");
        assertTrue(pm.message.isVerified(), "Message should be marked as verified");
    }

    @Test
    void verifyFailsWhenWireIsTampered() throws Exception {
        ParsedMessage pm = signQuery();

        // Corrupt a single byte in the wire representation (e.g., flip the first byte of the
        // question section). This should cause the MAC verification to fail.
        byte[] corruptedWire = pm.wire.clone();
        // Ensure we modify a byte that is not part of the TSIG signature itself.
        // Byte index 12 is the start of the question name length field in a typical DNS query.
        corruptedWire[12] ^= 0xFF;

        TSIGRecord requestTSIG = pm.message.getTSIG();

        int result = tsigKey.verify(pm.message, corruptedWire, requestTSIG, true);
        // Any non‑NOERROR code indicates failure; the exact code is implementation‑defined.
        assertNotEquals(Rcode.NOERROR, result, "Verification should fail for tampered wire data");
    }

    @Test
    void verifyWithNullRequestTSIGTreatsAsUnsigned() throws Exception {
        // When requestTSIG is null the method should behave as if the incoming message
        // is not a TSIG‑protected request (i.e., verification is a no‑op and returns NOERROR).
        Message query = Message.newQuery(questionRecord);
        query.setTSIG(tsigKey);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);

        int result = tsigKey.verify(parsed, wire, null, true);
        assertEquals(Rcode.NOERROR, result, "Verification with null requestTSIG should return NOERROR");
    }
}
```