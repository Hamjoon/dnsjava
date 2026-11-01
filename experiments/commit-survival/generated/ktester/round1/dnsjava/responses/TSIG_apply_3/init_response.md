```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#apply(Message, int, TSIGRecord)}.
 *
 * The tests focus on the observable behaviour of the {@code apply} method:
 *
 * <ul>
 *   <li>the method adds a TSIG record to the message</li>
 *   <li>the generated signature can be verified with the same {@link TSIG} instance</li>
 *   <li>the error code supplied to {@code apply} is reflected in the verification result</li>
 * </ul>
 *
 * No private members of {@link TSIG} or {@link TSIGRecord} are accessed.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_3_Test {

    /** A deterministic clock to make the generated TSIG timestamps predictable. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /** The TSIG key used for all tests. */
    private TSIG tsig;

    /** A simple DNS query message used as the base for signing. */
    private Message queryMessage;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Initialise a TSIG instance with a known key and a fixed clock.
        // Using the constructor that accepts a SecretKey and a Clock.
        byte[] keyBytes = base64.fromString("12345678");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), keySpec, FIXED_CLOCK);

        // Build a minimal query message (A record for www.example.).
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the objects are garbage‑collected.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verify that {@code apply} adds a TSIG record and that the signature can be
     * successfully verified with the same key.
     */
    @Test
    void applyAddsSignatureAndCanBeVerified() throws IOException {
        // Apply a TSIG with NOERROR and no previous TSIG record (first request).
        tsig.apply(queryMessage, Rcode.NOERROR, null);

        // The message should now be marked as signed.
        assertTrue(queryMessage.isSigned(), "Message should be marked as signed after apply");

        // Serialize the message and verify the signature.
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        int verifyResult = tsig.verify(queryMessage, wire, null);

        assertEquals(Rcode.NOERROR, verifyResult,
                "Verification should succeed and return NOERROR");
        assertTrue(queryMessage.isVerified(),
                "Message should be marked as verified after successful verification");
    }

    /**
     * Verify that the error code supplied to {@code apply} is reflected in the
     * verification result. Here we use {@code BADTIME} as an example.
     */
    @Test
    void applyWithErrorCodeIsReflectedInVerification() throws IOException {
        // Apply a TSIG with a non‑zero error code (BADTIME) and no prior TSIG.
        tsig.apply(queryMessage, Rcode.BADTIME, null);

        assertTrue(queryMessage.isSigned(), "Message should be signed even when an error code is set");

        // Serialize and verify.
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        int verifyResult = tsig.verify(queryMessage, wire, null);

        assertEquals(Rcode.BADTIME, verifyResult,
                "Verification should return the error code that was supplied to apply");
    }

    /**
     * Verify that a response message can be signed using the TSIG from a prior query.
     * The response includes the original TSIG as the “old” record.
     */
    @Test
    void applyWithPreviousTSIGRecord() throws IOException {
        // First, sign the original query (this also generates the TSIGRecord we will reuse).
        tsig.apply(queryMessage, Rcode.NOERROR, null);
        TSIGRecord queryTSIG = queryMessage.getTSIG();
        assertNotNull(queryTSIG, "Query TSIG record must be present after signing");

        // Build a simple response that mirrors the query ID.
        Message response = new Message(queryMessage.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // No answer records – the focus is on TSIG handling.

        // Apply TSIG to the response, passing the original query TSIG as the “old” record.
        tsig.apply(response, Rcode.NOERROR, queryTSIG);

        assertTrue(response.isSigned(), "Response should be signed after apply with previous TSIG");

        // Verify the response using the same key.
        byte[] responseWire = response.toWire(Message.MAXLENGTH);
        int verifyResult = tsig.verify(response, responseWire, queryTSIG);

        assertEquals(Rcode.NOERROR, verifyResult,
                "Response verification should succeed and return NOERROR");
        assertTrue(response.isVerified(),
                "Response should be marked as verified after successful verification");
    }
}
```