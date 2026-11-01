```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TSIG_apply_3_Test {

    /** Fixed clock so that the generated TSIG timestamps are deterministic. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /** TSIG instance used by all tests. */
    private static TSIG tsig;

    /** Message that will be signed in each test. */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a TSIG key with a deterministic clock.
        // Algorithm HMAC_SHA256, name "example.", key "12345678" (base‑64 string).
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678", FIXED_CLOCK);
    }

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Build a simple DNS query message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        message = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up for this test class.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release at the class level.
        tsig = null;
    }

    /**
     * Verify that {@code TSIG.apply(Message, int, TSIGRecord)} attaches a TSIG record,
     * marks the message as signed, and that the signature can be verified successfully.
     */
    @Test
    void applyAddsSignatureAndVerifies() throws Exception {
        // Apply a TSIG signature with NOERROR and no previous TSIG record.
        tsig.apply(message, Rcode.NOERROR, null);

        // The message should now be marked as signed.
        assertTrue(message.isSigned(), "Message should be marked as signed after apply()");
        // A TSIG record must be present.
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should be present after apply()");

        // The error field inside the TSIG record must reflect the supplied error code.
        assertEquals(Rcode.NOERROR, tsigRecord.getError(), "TSIG record error should be NOERROR");

        // Verify the signature using the same TSIG instance.
        byte[] wire = message.toWire(Message.MAXLENGTH);
        int verifyResult = tsig.verify(message, wire, null);
        assertEquals(Rcode.NOERROR, verifyResult, "Verification should succeed with NOERROR");
        assertTrue(message.isVerified(), "Message should be marked as verified after successful verify()");
    }

    /**
     * Ensure that a non‑zero error code is correctly stored in the generated TSIG record.
     */
    @Test
    void applyWithErrorCode() throws Exception {
        int errorCode = Rcode.BADKEY; // an arbitrary non‑zero error
        tsig.apply(message, errorCode, null);

        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should be present");
        assertEquals(errorCode, tsigRecord.getError(), "TSIG record must contain the supplied error code");
    }
}
```