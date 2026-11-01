```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit‑tests for {@link TSIG#apply(Message, TSIGRecord)}.
 *
 * <p>The tests focus on the observable side‑effects of the method:
 *
 * <ul>
 *   <li>the {@link Message} receives a {@link TSIGRecord}
 *   <li>the message is marked as signed
 *   <li>the generated TSIG record contains the expected algorithm and key name
 *   <li>the method works when an {@code old} TSIG record is supplied (e.g. signing a response)
 * </ul>
 *
 * <p>A fixed {@link Clock} is used so that the {@code timeSigned} field of the generated
 * {@code TSIGRecord} is deterministic and the test does not depend on the current system time.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_Test {

    /** TSIG instance used for all tests – created with a deterministic clock. */
    private static TSIG tsig;

    /** Fixed clock to make the signature timestamp predictable. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /** Simple base‑64 encoded key (the string “1234567890”). */
    private static final String B64_KEY = "MTIzNDU2Nzg5MA==";

    @BeforeAll
    static void setupBeforeAll() {
        // algorithm = HMAC‑SHA256, name = "example."
        Name algorithm = TSIG.HMAC_SHA256;
        Name name = Name.fromString("example.");
        SecretKey key = new SecretKeySpec(base64.fromString(B64_KEY), "HmacSHA256");
        tsig = new TSIG(algorithm, name, key, FIXED_CLOCK);
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
     * Verify that {@code apply(message, null)} adds a TSIG record to the message,
     * marks the message as signed and populates the expected fields.
     */
    @Test
    void applyAddsTsigWhenOldIsNull() throws Exception {
        // Build a minimal query message (a single question record)
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);

        // Pre‑condition: the message must not already be signed
        assertFalse(query.isSigned(), "Message should start unsigned");

        // Exercise the method under test
        tsig.apply(query, null);

        // Post‑condition checks
        TSIGRecord tsigRecord = query.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should have been added");
        assertTrue(query.isSigned(), "Message must be marked as signed");
        assertEquals(TSIG.HMAC_SHA256, tsigRecord.getAlgorithm(), "Algorithm name mismatch");
        assertEquals(Name.fromString("example."), tsigRecord.getName(), "Key name mismatch");
        // The timestamp should be the fixed instant used by the clock
        assertEquals(Instant.parse("2023-01-01T00:00:00Z"), tsigRecord.getTimeSigned(),
                "Timestamp should come from the fixed clock");
    }

    /**
     * Verify that {@code apply(message, old)} works when an existing TSIG record is supplied.
     * This mimics signing a response that references the request’s TSIG.
     */
    @Test
    void applyWithExistingOldRecord() throws Exception {
        // 1️⃣ Create a request message and sign it – this gives us a valid “old” TSIGRecord.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message request = Message.newQuery(question);
        tsig.apply(request, null);
        TSIGRecord old = request.getTSIG();
        assertNotNull(old, "Old TSIG record must exist for the test");

        // 2️⃣ Build a fresh response message that references the request.
        Message response = new Message();
        response.getHeader().setID(request.getHeader().getID()); // keep same ID for simplicity
        response.addRecord(question, Section.QUESTION);
        // The response is not signed yet.
        assertFalse(response.isSigned(), "Response should start unsigned");

        // 3️⃣ Apply TSIG using the old record (fullSignature = true is the default behaviour of
        //    the overloaded method we are testing).
        tsig.apply(response, old);

        // 4️⃣ Verify that the response now carries a TSIG record and that the original ID
        //    stored in the TSIGRecord matches the request ID.
        TSIGRecord responseTsig = response.getTSIG();
        assertNotNull(responseTsig, "Response must contain a TSIG record after apply");
        assertTrue(response.isSigned(), "Response must be marked as signed");
        assertEquals(request.getHeader().getID(), responseTsig.getOriginalID(),
                "Original ID in response TSIG must match request ID");
        // The algorithm and key name must be identical to the one used to construct the TSIG object.
        assertEquals(TSIG.HMAC_SHA256, responseTsig.getAlgorithm());
        assertEquals(Name.fromString("example."), responseTsig.getName());
    }

    /**
     * A regression test that ensures the method does not throw when the message already
     * contains a TSIG record (the usual case when a resolver re‑signs a message).
     */
    @Test
    void applyDoesNotThrowWhenMessageAlreadyHasTsig() throws Exception {
        // Prepare a message that already carries a TSIG record.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        tsig.apply(msg, null); // first signing – creates a TSIGRecord

        // The second call should replace the existing TSIG without throwing.
        assertDoesNotThrow(() -> tsig.apply(msg, null),
                "Applying TSIG a second time should not throw");
        // The message should still be signed and contain exactly one TSIGRecord.
        TSIGRecord tsigRecord = msg.getTSIG();
        assertNotNull(tsigRecord);
        List<Record> additionals = msg.getSection(Section.ADDITIONAL);
        long tsigCount = additionals.stream().filter(r -> r.getType() == Type.TSIG).count();
        assertEquals(1, tsigCount, "Message must contain exactly one TSIG record after re‑apply");
    }
}
```