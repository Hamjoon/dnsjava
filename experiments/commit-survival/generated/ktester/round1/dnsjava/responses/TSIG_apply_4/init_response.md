```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#apply(Message, int, TSIGRecord, boolean)}.
 *
 * The tests verify that:
 * <ul>
 *   <li>A TSIGRecord is added to the {@link Section#ADDITIONAL} section of the message.</li>
 *   <li>The message’s {@code tsigState} is set to {@link Message#TSIG_SIGNED}.</li>
 *   <li>The generated TSIGRecord contains the expected error code.</li>
 * </ul>
 *
 * The test class lives in the same package as the production code, allowing direct access to
 * package‑private fields such as {@code Message.tsigState}.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_4_Test {

    /** A deterministic clock so that the generated TSIG timestamps are repeatable. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

    /** TSIG instance used in all tests. */
    private TSIG tsig;

    /** A simple query message used as the target of {@code apply}. */
    private Message queryMessage;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Use HMAC‑SHA256 algorithm with a short base‑64 key.
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");
        // Replace the internal clock with a fixed one to make the signature deterministic.
        // The constructor that accepts a Clock is package‑private, so we use reflection.
        try {
            java.lang.reflect.Field clockField = TSIG.class.getDeclaredField("clock");
            clockField.setAccessible(true);
            clockField.set(tsig, FIXED_CLOCK);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        // Build a minimal DNS query message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verify that {@code apply} adds a TSIGRecord to the additional section and sets the
     * {@code tsigState} flag.
     */
    @Test
    void applyAddsTSIGRecordAndSetsState() {
        // Act
        tsig.apply(queryMessage, Rcode.NOERROR, null, true);

        // Assert that the message state has been updated.
        assertEquals(Message.TSIG_SIGNED, queryMessage.tsigState,
                "Message tsigState should be TSIG_SIGNED after apply");

        // The additional section must contain exactly one record and it must be a TSIGRecord.
        Record[] additionals = queryMessage.getSectionArray(Section.ADDITIONAL);
        assertEquals(1, additionals.length, "There should be one additional record");
        assertTrue(additionals[0] instanceof TSIGRecord,
                "The additional record must be a TSIGRecord");

        TSIGRecord tsigRecord = (TSIGRecord) additionals[0];
        assertEquals(Rcode.NOERROR, tsigRecord.getError(),
                "TSIGRecord error field should match the supplied error code");
    }

    /**
     * Verify that {@code apply} works when an existing TSIGRecord (the “old” record) is supplied
     * and that the {@code fullSignature} flag does not prevent record creation.
     */
    @Test
    void applyWithOldRecordAndPartialSignature() throws TextParseException {
        // Create an old TSIGRecord – this mimics a previous request/response pair.
        TSIGRecord old = new TSIGRecord(
                Name.fromString("example."),          // name
                DClass.IN,                            // dclass
                0L,                                   // ttl
                TSIG.HMAC_SHA256,                     // algorithm name
                Instant.now(FIXED_CLOCK),             // timeSigned (deterministic)
                TSIG.FUDGE,                           // fudge
                new byte[0],                          // empty signature
                0,                                    // originalID
                Rcode.NOERROR,                        // error
                new byte[0]                           // other
        );

        // Act – use a partial signature (fullSignature = false)
        tsig.apply(queryMessage, Rcode.NOERROR, old, false);

        // Assert the same invariants as the previous test.
        assertEquals(Message.TSIG_SIGNED, queryMessage.tsigState,
                "Message tsigState should be TSIG_SIGNED after apply with old record");

        Record[] additionals = queryMessage.getSectionArray(Section.ADDITIONAL);
        assertEquals(1, additionals.length, "There should be one additional record after apply with old record");
        assertTrue(additionals[0] instanceof TSIGRecord,
                "The additional record must be a TSIGRecord after apply with old record");

        TSIGRecord generated = (TSIGRecord) additionals[0];
        assertEquals(Rcode.NOERROR, generated.getError(),
                "Generated TSIGRecord error should match supplied error code");
    }
}
```