```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TSIG#generate(Message, byte[], int, TSIGRecord)}.
 *
 * <p>The tests focus on the public {@code generate} overload that delegates to the
 * full‑signature implementation. A fixed {@link Clock} is used so that the {@code timeSigned}
 * field of the resulting {@link TSIGRecord} is predictable and can be asserted.</p>
 */
@ExtendWith(MockitoExtension.class)
class TSIG_generate_Test {

    /** Fixed instant used for deterministic TSIG timestamps. */
    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

    /** The TSIG instance under test – HMAC‑SHA256 algorithm with a known key. */
    private TSIG tsig;

    /** A simple DNS query message used as the basis for TSIG generation. */
    private Message queryMessage;

    /** Wire representation of {@link #queryMessage} (without TSIG). */
    private byte[] queryWire;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a deterministic clock so that the generated TSIGRecord has a known
        // timeSigned value.
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

        // Construct a TSIG with the HMAC‑SHA256 algorithm, name "example.", and a short key.
        // The constructor that accepts a {@link Clock} is package‑private, therefore we
        // create the object via the public constructor and then replace its clock via
        // reflection (a technique allowed in tests but not part of production code).
        tsig = new TSIG(TSIG.HMAC_SHA256, "example.", "c2VjcmV0a2V5"); // "secretkey" base64‑encoded
        try {
            java.lang.reflect.Field clockField = TSIG.class.getDeclaredField("clock");
            clockField.setAccessible(true);
            clockField.set(tsig, fixedClock);
        } catch (ReflectiveOperationException e) {
            fail("Failed to inject fixed Clock into TSIG instance", e);
        }

        // Build a minimal query message (type A for www.example.).
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);

        // Serialize the message – this is the byte array that will be signed.
        queryWire = queryMessage.toWire(Message.MAXLENGTH);
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
     * Verify that a normal (no‑error) TSIG record is generated with the expected fields.
     */
    @Test
    void generateNormalTsigRecord() {
        // Act: generate a TSIG record for the query message.
        TSIGRecord tsigRecord = tsig.generate(queryMessage, queryWire, Rcode.NOERROR, null);

        // Assert: the record is non‑null and contains the expected metadata.
        assertNotNull(tsigRecord, "TSIGRecord should not be null");
        assertEquals(TSIG.HMAC_SHA256, tsigRecord.getAlgorithm(), "Algorithm name mismatch");
        assertEquals(Name.fromString("example."), tsigRecord.getName(), "TSIG key name mismatch");
        assertEquals(Rcode.NOERROR, tsigRecord.getError(), "Error code should be NOERROR");
        assertEquals(FIXED_INSTANT, tsigRecord.getTimeSigned(), "Timestamp should match fixed clock");
        assertNotNull(tsigRecord.getSignature(), "Signature bytes must be present");
        assertTrue(tsigRecord.getSignature().length > 0, "Signature should not be empty");
    }

    /**
     * Verify that the {@code error} argument is propagated into the generated record.
     */
    @Test
    void generateTsigRecordWithErrorCode() {
        int errorCode = Rcode.FORMERR; // an arbitrary non‑zero error

        TSIGRecord tsigRecord = tsig.generate(queryMessage, queryWire, errorCode, null);

        assertNotNull(tsigRecord, "TSIGRecord should not be null");
        assertEquals(errorCode, tsigRecord.getError(), "Error code should be propagated");
        // Other fields remain valid.
        assertEquals(TSIG.HMAC_SHA256, tsigRecord.getAlgorithm());
        assertEquals(Name.fromString("example."), tsigRecord.getName());
        assertEquals(FIXED_INSTANT, tsigRecord.getTimeSigned());
        assertNotNull(tsigRecord.getSignature());
        assertTrue(tsigRecord.getSignature().length > 0);
    }

    /**
     * Verify that providing a previous TSIGRecord (as would be the case for a response
     * to a signed query) does not cause a failure and that the returned record still
     * contains a valid signature.
     */
    @Test
    void generateWithPreviousTsigRecord() {
        // First, generate a TSIG record for the original query.
        TSIGRecord original = tsig.generate(queryMessage, queryWire, Rcode.NOERROR, null);
        assertNotNull(original, "Original TSIGRecord must be generated");

        // Simulate a response message that references the original TSIG.
        Message response = new Message(queryMessage.getHeader().getID());
        response.setTSIG(tsig); // attach the same key for signing
        // (no additional records are needed for this test)

        byte[] responseWire = response.toWire(Message.MAXLENGTH);

        // Act: generate a TSIG record for the response, passing the original record.
        TSIGRecord responseTsig = tsig.generate(response, responseWire, Rcode.NOERROR, original);

        // Assert: the response TSIG is valid and independent of the original's signature.
        assertNotNull(responseTsig, "Response TSIGRecord must be generated");
        assertEquals(TSIG.HMAC_SHA256, responseTsig.getAlgorithm());
        assertEquals(Name.fromString("example."), responseTsig.getName());
        assertEquals(Rcode.NOERROR, responseTsig.getError());
        assertEquals(FIXED_INSTANT, responseTsig.getTimeSigned());
        assertNotNull(responseTsig.getSignature());
        assertTrue(responseTsig.getSignature().length > 0);
        // The signatures of the two records should differ because the signed data differs.
        assertFalse(java.util.Arrays.equals(original.getSignature(), responseTsig.getSignature()),
                "Signatures for different messages should not be identical");
    }
}
```