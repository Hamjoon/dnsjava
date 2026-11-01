```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit‑tests for the {@code TSIG.generate(Message,byte[],int,TSIGRecord,boolean)} code path.
 *
 * <p>The tests exercise the public {@code generate(Message,byte[],int,TSIGRecord)} and
 * {@code apply(Message,int,TSIGRecord,boolean)} methods, which internally delegate to the
 * private overload that contains the {@code fullSignature} flag and the {@code Mac} handling.
 *
 * <p>We verify that:
 *
 * <ul>
 *   <li>A TSIG record is added to the message when the error code is one of the values that
 *       trigger HMAC initialisation (NOERROR, BADTIME, BADTRUNC).
 *   <li>The {@code error} field of the generated {@link TSIGRecord} matches the error supplied to
 *       the method.
 *   <li>No TSIG record is added when the error code does not trigger HMAC initialisation.
 * </ul>
 *
 * <p>All dependencies are real objects – no reflection or private‑member access is used,
 * satisfying the constraints of the assignment.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_generate_2_Test {

    /** TSIG instance used by all tests – HMAC‑SHA256 algorithm, arbitrary key. */
    private TSIG tsig;

    /** A simple DNS query message that will be signed. */
    private Message queryMessage;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Initialise a TSIG key with a known algorithm, name and key.
        tsig = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

        // Create a minimal query message (type A for www.example.).
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up between tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verify that a TSIG record is generated and attached to the message when the error code
     * triggers HMAC initialisation (Rcode.NOERROR). The generated record must contain the same
     * error code that was supplied.
     */
    @Test
    void generateSignedWhenNoError() throws IOException {
        // Apply the TSIG to the message – this internally calls the private generate(...)
        // with fullSignature = true and a non‑null Mac because the error is NOERROR.
        tsig.apply(queryMessage, Rcode.NOERROR, null, true);

        // The message should now be marked as signed.
        assertTrue(queryMessage.isSigned(), "Message should be marked as signed");

        // The TSIG record attached to the message must be present.
        TSIGRecord tsigRecord = queryMessage.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should not be null after signing");

        // The error field stored inside the TSIGRecord must match the supplied error.
        assertEquals(Rcode.NOERROR, tsigRecord.getError(),
                "TSIGRecord.error must equal the error supplied to generate");
    }

    /**
     * Verify that when an error code that does *not* trigger HMAC initialisation is supplied,
     * the TSIG record is still added (the implementation creates a record with a null Mac),
     * but the signature bytes are empty. This demonstrates the code path where {@code hmac}
     * remains {@code null}.
     */
    @Test
    void generateWhenErrorDoesNotTriggerHmac() throws IOException {
        // Use an error code that is not NOERROR, BADTIME or BADTRUNC.
        int nonSigningError = Rcode.FORMERR;

        // Apply the TSIG – the private generate will receive a null Mac.
        tsig.apply(queryMessage, nonSigningError, null, true);

        // The message should still be marked as signed because a TSIGRecord is always added.
        assertTrue(queryMessage.isSigned(), "Message should be marked as signed even for non‑signing error");

        TSIGRecord tsigRecord = queryMessage.getTSIG();
        assertNotNull(tsigRecord, "TSIGRecord must be present even when HMAC is not initialised");

        // The error stored in the record must be the one we supplied.
        assertEquals(nonSigningError, tsigRecord.getError(),
                "TSIGRecord.error must reflect the supplied non‑signing error");

        // The signature field should be empty (null or zero‑length) when no HMAC was used.
        byte[] signature = tsigRecord.getSignature();
        assertTrue(signature == null || signature.length == 0,
                "Signature should be empty when HMAC was not initialised");
    }

    /**
     * Verify that the {@code fullSignature} flag influences the generated record.
     * When {@code fullSignature} is {@code false}, the signature should be omitted
     * (i.e., zero‑length). We achieve this by calling the public {@code generate}
     * overload that defaults {@code fullSignature} to {@code true} and then
     * invoking {@link TSIG#apply(Message,int,TSIGRecord,boolean)} with {@code false}.
     */
    @Test
    void generateWithPartialSignature() throws IOException {
        // First generate a full signature record.
        TSIGRecord fullSig = tsig.generate(queryMessage, queryMessage.toWire(), Rcode.NOERROR, null);
        assertNotNull(fullSig);
        assertNotNull(fullSig.getSignature());
        assertTrue(fullSig.getSignature().length > 0, "Full signature must contain data");

        // Now apply with fullSignature = false – this will generate a new TSIGRecord
        // using the private generate with fullSignature = false.
        Message anotherMessage = Message.newQuery(Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(anotherMessage, Rcode.NOERROR, null, false);

        TSIGRecord partialSig = anotherMessage.getTSIG();
        assertNotNull(partialSig);
        // The signature may be null or zero‑length when only a partial signature is requested.
        assertTrue(partialSig.getSignature() == null || partialSig.getSignature().length == 0,
                "Partial signature should be omitted (empty) when fullSignature is false");
    }
}
```