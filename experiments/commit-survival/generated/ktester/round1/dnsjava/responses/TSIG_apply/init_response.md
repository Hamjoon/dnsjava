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
 * Tests for {@link TSIG#apply(Message, TSIGRecord)} (the overload that delegates to the
 * full‑signature implementation). The tests verify that a TSIG record is added to a message,
 * that the record contains the expected algorithm and error code, and that the message can be
 * verified using the same {@link TSIG} instance.
 *
 * <p>The test class follows the template supplied in the task description and does not attempt
 * to access any private members of {@link TSIG} or {@link TSIGRecord}.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_Test {

    /** A TSIG instance that uses HMAC‑SHA256 and a simple base‑64 key. */
    private static TSIG tsig;

    /** A fresh DNS query message used by each test. */
    private Message queryMessage;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a deterministic clock so that the generated TSIG timestamps are repeatable.
        Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        // The public TSIG constructor that accepts (Name algorithm, Name name, SecretKey key, Clock)
        // is used to inject the fixed clock.
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(
                base64.fromString("12345678"), "HmacSHA256"), fixedClock);
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a minimal query (A record for www.example.) – the same construction used in the
        // library's own tests.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up; method present for symmetry with the template.
        queryMessage = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
        tsig = null;
    }

    /**
     * Verifies that {@code TSIG.apply(Message, TSIGRecord)} adds a TSIG record to the
     * message, sets the error code to {@link Rcode#NOERROR}, and that the resulting message
     * can be verified with the same TSIG key.
     */
    @Test
    void applyAddsTSIGRecordAndVerifiesSuccessfully() throws IOException {
        // Apply the TSIG (old record is null – typical for an unsigned query)
        tsig.apply(queryMessage, null);

        // The message should now contain a TSIG record.
        TSIGRecord tsigRecord = queryMessage.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should be present after apply()");
        assertTrue(queryMessage.isSigned(), "Message.isSigned() must be true after apply()");
        assertEquals(Rcode.NOERROR, tsigRecord.getError(), "TSIG error code should be NOERROR");

        // Serialize and deserialize to emulate a wire‑level round‑trip.
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);

        // Verify the signature using the same TSIG instance.
        int result = tsig.verify(parsed, wire, null);
        assertEquals(Rcode.NOERROR, result, "Verification should succeed (NOERROR)");
        assertTrue(parsed.isVerified(), "Parsed message should be marked as verified");
    }

    /**
     * Ensures that applying a TSIG to a message that already carries a TSIG (the “old” record)
     * does not throw and produces a valid signature. This mirrors the typical client‑side
     * behaviour where a request is signed, sent, and the response is signed using the request’s
     * TSIG as the “old” record.
     */
    @Test
    void applyWithExistingTSIGRecordDoesNotThrowAndVerifies() throws IOException {
        // First, sign the original query – this creates the “old” TSIG record.
        tsig.apply(queryMessage, null);
        TSIGRecord oldRecord = queryMessage.getTSIG();
        assertNotNull(oldRecord, "Initial TSIG record must exist");

        // Simulate a response message that will be signed using the old TSIG.
        Message response = new Message(queryMessage.getHeader().getID());
        response.getHeader().setFlag(Flags.QR); // mark as a response
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // Apply TSIG to the response, passing the old record.
        tsig.apply(response, oldRecord);

        // Verify that the response now carries a TSIG record.
        TSIGRecord responseTSIG = response.getTSIG();
        assertNotNull(responseTSIG, "Response should contain a TSIG record after apply()");
        assertTrue(response.isSigned(), "Response.isSigned() must be true");

        // Round‑trip and verify.
        byte[] wire = response.toWire(Message.MAXLENGTH);
        Message parsedResponse = new Message(wire);
        int verifyResult = tsig.verify(parsedResponse, wire, oldRecord);
        assertEquals(Rcode.NOERROR, verifyResult, "Response verification should succeed");
        assertTrue(parsedResponse.isVerified(), "Parsed response should be marked as verified");
    }
}
```