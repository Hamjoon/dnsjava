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
import java.lang.reflect.Field;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class TSIG_apply_4_Test {

    /**
     * Fixed clock so that the TSIG timestamp does not vary between test runs.
     */
    private static Clock fixedClock;

    /**
     * The TSIG instance under test.
     */
    private static TSIG tsig;

    /**
     * Message used in each test case.
     */
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

    /**
     * The standard case: no previous TSIG, NOERROR, full signature.
     */
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
        Record[] addition = message.getSectionArray(Section.ADDITIONAL);
        assertTrue(addition.length > 0, "Addition section must contain at least one record");
        assertEquals(tsigRecord, addition[addition.length - 1],
                "TSIG record should be the last record in the ADDITION section");
    }

    /**
     * Apply with a non‑zero error code and a partial (non‑full) signature.
     */
    @Test
    void applyWithErrorAndPartialSignatureAddsTSIGRecord() {
        // invoke the focal method with an error code and a partial signature flag
        tsig.apply(message, Rcode.FORMERR, /* old */ null, /* fullSignature */ false);
        // a TSIG record must still be added
        assertNotNull(message.getTSIG(), "TSIG record should be added even when error != NOERROR");
        // the message must still be marked as signed
        assertTrue(message.isSigned(), "Message should be marked as signed regardless of error code");
    }

    /**
     * Apply when a previous TSIGRecord is supplied (e.g., for a response).
     */
    @Test
    void applyWithPreviousTSIGRecord() {
        // create a dummy previous TSIGRecord (the constructor arguments are arbitrary but valid)
        TSIGRecord previous = new TSIGRecord(
                Name.fromString("example."), // name
                DClass.IN,                   // dclass
                0L,                          // ttl
                TSIG.HMAC_SHA256,            // algorithm
                Instant.now(fixedClock),    // timeSigned
                TSIG.FUDGE,                  // fudge
                new byte[0],                 // mac (signature)
                message.getHeader().getID(), // original ID
                Rcode.NOERROR,               // error
                new byte[0]                  // other data
        );
        // invoke the focal method with the previous TSIGRecord
        tsig.apply(message, Rcode.NOERROR, previous, true);
        // verify that a new TSIG record replaces the old one
        TSIGRecord resulting = message.getTSIG();
        assertNotNull(resulting, "Resulting TSIG record must be present");
        assertTrue(message.isSigned(), "Message should be signed after applying with a previous TSIG");
        assertNotEquals(previous, resulting,
                "A new TSIGRecord should be generated, not reuse the old one");
    }

    /**
     * Parameterized test covering various error codes and fullSignature flags.
     */
    @ParameterizedTest(name = "apply(error={0}, fullSignature={1})")
    @CsvSource({ // happy path, non‑full
            "0, false", // happy path, full
            "0, true", // non‑zero error
            "9, false", // negative error (edge case)
            "-1, false" })
    void applyVariousErrorAndSignature(int error, boolean fullSignature) {
        tsig.apply(message, error, null, fullSignature);
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "TSIG record must be present");
        // Verify that the error field inside the generated TSIGRecord matches the supplied value.
        // The TSIGRecord class does not expose a public getter for the error, so we use reflection.
        try {
            Field errorField = TSIGRecord.class.getDeclaredField("error");
            errorField.setAccessible(true);
            int actualError = (int) errorField.get(tsigRecord);
            assertEquals(error, actualError,
                    "The TSIGRecord error field should equal the error argument passed to apply()");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to access TSIGRecord.error field via reflection", e);
        }
        assertTrue(message.isSigned(), "Message should be marked as signed");
    }

    /**
     * Passing a null Message should raise NullPointerException.
     */
    @Test
    void applyWithNullMessageThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> tsig.apply(null, Rcode.NOERROR, null, false),
                "Calling apply with a null Message should throw NullPointerException");
    }

    /**
     * Combination of a previous TSIG record and full‑signature flag.
     */
    @Test
    void applyWithOldRecordAndFullSignature() {
        TSIGRecord old = new TSIGRecord(
                Name.fromString("example.com."), DClass.IN, 300L,
                TSIG.HMAC_SHA1, Instant.now(fixedClock), TSIG.FUDGE,
                new byte[0], message.getHeader().getID(),
                Rcode.NOERROR, new byte[0]);
        tsig.apply(message, Rcode.NOERROR, old, true);
        TSIGRecord result = message.getTSIG();
        assertNotNull(result, "A TSIG record must be added when old record and fullSignature are supplied");
        assertTrue(message.isSigned(), "Message should be signed");
    }

    /**
     * Exception scenario: old TSIGRecord has a null algorithm name → NPE.
     */
    @Test
    void applyOldRecordWithNullAlgorithmThrowsNPE() {
        TSIGRecord malformed = new TSIGRecord(
                Name.fromString("example.com."), DClass.IN, 300L,
                null,                            // null algorithm name
                Instant.now(fixedClock), TSIG.FUDGE,
                new byte[0], message.getHeader().getID(),
                Rcode.NOERROR, new byte[0]);
        assertThrows(NullPointerException.class,
                () -> tsig.apply(message, Rcode.NOERROR, malformed, false),
                "A TSIGRecord with a null algorithm should cause NullPointerException");
    }

    /**
     * Exception scenario: unsupported algorithm name → IllegalArgumentException.
     */
    @Test
    void applyOldRecordWithUnsupportedAlgorithmThrowsIAE() {
        Name unsupported = Name.fromString("unsupported-alg.");
        TSIGRecord malformed = new TSIGRecord(
                Name.fromString("example.com."), DClass.IN, 300L,
                unsupported, Instant.now(fixedClock), TSIG.FUDGE,
                new byte[0], message.getHeader().getID(),
                Rcode.NOERROR, new byte[0]);
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(message, Rcode.NOERROR, malformed, false),
                "An unsupported algorithm should cause IllegalArgumentException");
    }

    /**
     * Exception scenario: old TSIGRecord contains a null signature (treated as null key) → NPE.
     */
    @Test
    void applyOldRecordWithNullSignatureThrowsNPE() {
        TSIGRecord malformed = new TSIGRecord(
                Name.fromString("example.com."), DClass.IN, 300L,
                TSIG.HMAC_SHA256, Instant.now(fixedClock), TSIG.FUDGE,
                null,                     // null signature (simulates missing key)
                message.getHeader().getID(),
                Rcode.NOERROR, new byte[0]);
        assertThrows(NullPointerException.class,
                () -> tsig.apply(message, Rcode.NOERROR, malformed, false),
                "A TSIGRecord with a null signature should cause NullPointerException");
    }
}
```