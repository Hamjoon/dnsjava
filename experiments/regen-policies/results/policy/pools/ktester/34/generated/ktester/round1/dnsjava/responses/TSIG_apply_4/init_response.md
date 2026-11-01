```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#apply(Message, int, TSIGRecord, boolean)}.
 *
 * The tests verify that:
 * <ul>
 *   <li>A TSIG record is added to the message.</li>
 *   <li>The message is marked as signed (Message.TSIG_SIGNED).</li>
 *   <li>The TSIG record appears in the ADDITION section as the last record.</li>
 * </ul>
 *
 * A fixed {@link Clock} is used so that the generated TSIG time‑stamp is deterministic.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_4_Test {

    /** Fixed clock so that the TSIG timestamp does not vary between test runs. */
    private static Clock fixedClock;

    /** The TSIG instance under test. */
    private static TSIG tsig;

    /** Message used in each test case. */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // a deterministic instant – 2023‑01‑01T00:00:00Z
        fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"));

        // algorithm = HMAC‑SHA256, name = "example.", key = "12345678"
        Name algorithm = TSIG.HMAC_SHA256;
        Name name = Name.fromString("example.");
        SecretKey key = new SecretKeySpec("12345678".getBytes(), "HmacSHA256");

        tsig = new TSIG(algorithm, name, key, fixedClock);
    }

    @BeforeEach
    void setupBeforeEach() {
        // a simple query message – the content of the question is irrelevant for the TSIG logic
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        message = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up for the current tests
    }

    @AfterAll
    static void teardownAfterAll() {
        // no global resources to release
    }

    /** The standard case: no previous TSIG, NOERROR, full signature. */
    @Test
    void applyAddsTSIGRecordAndMarksMessageSigned() {
        // pre‑condition – the message must not already contain a TSIG record
        assertNull(message.getTSIG(), "Message should start without a TSIG record");

        // invoke the focal method
        tsig.apply(message, Rcode.NOERROR, /* old */ null, /* fullSignature */ true);

        // post‑condition – a TSIG record must now be present
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should have been added to the message");

        // the message must be marked as signed
        assertTrue(message.isSigned(), "Message should be marked as signed after apply()");

        // the TSIG record must be the last record in the ADDITION section
        Record[] addition = message.getSectionArray(Section.ADDITION);
        assertTrue(addition.length > 0, "Addition section must contain at least one record");
        assertEquals(tsigRecord, addition[addition.length - 1],
                "TSIG record should be the last record in the ADDITION section");
    }

    /** Apply with a non‑zero error code and a partial (non‑full) signature. */
    @Test
    void applyWithErrorAndPartialSignatureAddsTSIGRecord() {
        // invoke the focal method with an error code and a partial signature flag
        tsig.apply(message, Rcode.FORMERR, /* old */ null, /* fullSignature */ false);

        // a TSIG record must still be added
        assertNotNull(message.getTSIG(), "TSIG record should be added even when error != NOERROR");

        // the message must still be marked as signed
        assertTrue(message.isSigned(), "Message should be marked as signed regardless of error code");
    }

    /** Apply when a previous TSIGRecord is supplied (e.g., for a response). */
    @Test
    void applyWithPreviousTSIGRecord() {
        // create a dummy previous TSIGRecord (the constructor arguments are arbitrary but valid)
        TSIGRecord previous = new TSIGRecord(
                Name.fromString("example."),      // name
                DClass.IN,                         // dclass
                0L,                               // ttl
                TSIG.HMAC_SHA256,                 // algorithm name
                Instant.now(fixedClock),          // timeSigned
                TSIG.FUDGE,                       // fudge
                new byte[0],                      // empty signature
                message.getHeader().getID(),      // original ID
                Rcode.NOERROR,                    // error
                new byte[0]                       // other data
        );

        // invoke the focal method with the previous TSIGRecord
        tsig.apply(message, Rcode.NOERROR, previous, true);

        // verify that a new TSIG record replaces the old one
        TSIGRecord resulting = message.getTSIG();
        assertNotNull(resulting, "Resulting TSIG record must be present");
        assertTrue(message.isSigned(), "Message should be signed after applying with a previous TSIG");
        assertNotEquals(previous, resulting, "A new TSIGRecord should be generated, not reuse the old one");
    }
}
```