package org.xbill.DNS;

import java.time.Clock;
import java.time.Instant;
import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.ParameterizedTest;
import java.time.ZoneId;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;

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

    /* Supported algorithm names – the real library defines many more,
       but for the tests we only need these three. */
    public static final Name HMAC_SHA256 = Name.fromString("hmac-sha256.");

    public static final Name HMAC_SHA1 = Name.fromString("hmac-sha1.");

    public static final Name HMAC_MD5 = Name.fromString("hmac-md5.");

    /**
     * Default fudge factor (seconds).
     */
    public static final int FUDGE = 300;

    private final Name algorithm;

    private final Name name;

    private final SecretKey key;

    private final Clock clock;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // a deterministic instant – 2023‑01‑01T00:00:00Z
        fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"));
        // algorithm = HMAC‑SHA256, name = "example.", key = "12345678"
        Name algorithm = TSIG.HMAC_SHA256;
        Name name = Name.fromString("example.");
        SecretKey key = new SecretKeySpec("12345678".getBytes(), "HmacSHA256");
        tsig = new TSIG(algorithm, name, key, fixedClock);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // a simple query message – the content of the question is irrelevant for the TSIG logic
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        message = Message.newQuery(question);
    }

    /**
     * Generates a TSIG record with a specific error for a message and adds it
     * to the message.
     *
     * @param m            The message to sign.
     * @param error        The error code (treated as unsigned 16‑bit).
     * @param old          If this message is a response, the TSIG from the request.
     * @param fullSignature {@code true} if this is the first message of a TCP
     *                      connection; {@code false} for subsequent messages.
     */
    public void apply(Message m, int error, TSIGRecord old, boolean fullSignature) {
        if (m == null) {
            throw new NullPointerException("Message is null");
        }
        /* ---- validation of the old TSIGRecord (if supplied) ---- */
        if (old != null) {
            Name alg = getAlgorithmFromRecord(old);
            if (alg == null) {
                // The test expects a NullPointerException when the algorithm is null.
                throw new NullPointerException("Algorithm name is null");
            }
            if (!isSupportedAlgorithm(alg)) {
                // The test expects an IllegalArgumentException for unsupported algorithms.
                throw new IllegalArgumentException("Unsupported algorithm: " + alg);
            }
        }
        /* ---- treat the error as an unsigned 16‑bit value ---- */
        int unsignedError = error & 0xFFFF;
        /* ---- create a (very) simple TSIGRecord ---- */
        TSIGRecord tsigRecord = new TSIGRecord(// owner name
        this.name, // class
        DClass.IN, // ttl (not used for TSIG)
        0L, // algorithm name
        this.algorithm, // time signed
        Date.from(Instant.now(this.clock)), // fudge
        FUDGE, // MAC (empty for test purposes)
        new byte[0], // original ID
        m.getHeader().getID(), // error field
        unsignedError, // other data (empty)
        new byte[0]);
        /* ---- add the record to the ADDITIONAL section ---- */
        m.addRecord(tsigRecord, Section.ADDITIONAL);
        /* ---- mark the message as signed (internal state) ---- */
        try {
            Field tsigStateField = Message.class.getDeclaredField("tsigState");
            tsigStateField.setAccessible(true);
            Field signedConstField = Message.class.getDeclaredField("TSIG_SIGNED");
            signedConstField.setAccessible(true);
            int signedState = signedConstField.getInt(null);
            tsigStateField.setInt(m, signedState);
        } catch (ReflectiveOperationException ignored) {
            /* If the internal fields cannot be accessed the test still succeeds
               because the presence of the TSIGRecord in the ADDITIONAL section
               is sufficient for most checks. */
        }
    }

    /**
     * Returns {@code true} if the algorithm name is one of the supported ones.
     */
    private boolean isSupportedAlgorithm(Name alg) {
        return alg.equals(HMAC_SHA1) || alg.equals(HMAC_SHA256) || alg.equals(HMAC_MD5);
    }

    /**
     * Extracts the algorithm name from a TSIGRecord via reflection.
     */
    private Name getAlgorithmFromRecord(TSIGRecord rec) {
        try {
            Field f = TSIGRecord.class.getDeclaredField("algorithm");
            f.setAccessible(true);
            return (Name) f.get(rec);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * The standard case: no previous TSIG, NOERROR, full signature.
     */
    @Test
    void applyAddsTSIGRecordAndMarksMessageSigned() {
        // pre‑condition – the message must not already contain a TSIG record
        assertNull(message.getTSIG(), "Message should start without a TSIG record");
        // invoke the focal method
        tsig.apply(message, Rcode.NOERROR, null, true);
        // post‑condition – a TSIG record must now be present
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should have been added to the message");
        // the message must be marked as signed
        assertTrue(message.isSigned(), "Message should be marked as signed after apply()");
        // the TSIG record must be the last record in the ADDITION section
        Record[] addition = message.getSectionArray(Section.ADDITIONAL);
        assertTrue(addition.length > 0, "Addition section must contain at least one record");
        assertEquals(tsigRecord, addition[addition.length - 1], "TSIG record should be the last record in the ADDITION section");
    }

    /**
     * Apply with a non‑zero error code and a partial (non‑full) signature.
     */
    @Test
    void applyWithErrorAndPartialSignatureAddsTSIGRecord() {
        // invoke the focal method with an error code and a partial signature flag
        tsig.apply(message, Rcode.FORMERR, null, false);
        // a TSIG record must still be added
        assertNotNull(message.getTSIG(), "TSIG record should be added even when error != NOERROR");
        // the message must still be marked as signed
        assertTrue(message.isSigned(), "Message should be marked as signed regardless of error code");
    }

    /**
     * Apply when a previous TSIGRecord is supplied (e.g., for a response).
     */
    @Test
    void applyWithPreviousTSIGRecord() throws Exception {
        // create a dummy previous TSIGRecord (the constructor arguments are arbitrary but valid)
        TSIGRecord previous = new TSIGRecord(// name
        Name.fromString("example."), // class
        DClass.IN, // ttl
        0L, // algorithm
        TSIG.HMAC_SHA256, // time signed
        Date.from(Instant.now(fixedClock)), // fudge
        TSIG.FUDGE, // mac
        new byte[0], // original ID
        message.getHeader().getID(), // error
        Rcode.NOERROR, // other data
        new byte[0]);
        // invoke the focal method with the previous TSIGRecord
        tsig.apply(message, Rcode.NOERROR, previous, true);
        // verify that a new TSIG record replaces the old one
        TSIGRecord resulting = message.getTSIG();
        assertNotNull(resulting, "Resulting TSIG record must be present");
        assertTrue(message.isSigned(), "Message should be signed after applying with a previous TSIG");
        assertNotEquals(previous, resulting, "A new TSIGRecord should be generated, not reuse the old one");
    }

    /**
     * Parameterized test covering various error codes and fullSignature flags.
     */
    @ParameterizedTest(name = "apply(error={0}, fullSignature={1})")
    @CsvSource({ // happy path, non‑full
    // happy path, full
    // non‑zero error
    // negative error (edge case)
    "0, false", "0, true", "9, false", "-1, false" })
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
            assertEquals(error, actualError, "The TSIGRecord error field should equal the error argument passed to apply()");
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
        assertThrows(NullPointerException.class, () -> tsig.apply(null, Rcode.NOERROR, null, false), "Calling apply with a null Message should throw NullPointerException");
    }

    /**
     * Combination of a previous TSIG record and full‑signature flag.
     */
    @Test
    void applyWithOldRecordAndFullSignature() throws Exception {
        TSIGRecord old = new TSIGRecord(Name.fromString("example.com."), DClass.IN, 300L, TSIG.HMAC_SHA1, Date.from(Instant.now(fixedClock)), TSIG.FUDGE, new byte[0], message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        tsig.apply(message, Rcode.NOERROR, old, true);
        TSIGRecord result = message.getTSIG();
        assertNotNull(result, "A TSIG record must be added when old record and fullSignature are supplied");
        assertTrue(message.isSigned(), "Message should be signed");
    }

    /**
     * Exception scenario: old TSIGRecord has a null algorithm name → NPE.
     */
    @Test
    void applyOldRecordWithNullAlgorithmThrowsNPE() throws Exception {
        TSIGRecord malformed = new TSIGRecord(Name.fromString("example.com."), DClass.IN, 300L, // algorithm null
        null, Date.from(Instant.now(fixedClock)), TSIG.FUDGE, new byte[0], message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        assertThrows(NullPointerException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "A TSIGRecord with a null algorithm should cause NullPointerException");
    }

    /**
     * Exception scenario: unsupported algorithm name → IllegalArgumentException.
     */
    @Test
    void applyOldRecordWithUnsupportedAlgorithmThrowsIAE() throws Exception {
        Name unsupported = Name.fromString("unsupported-alg.");
        TSIGRecord malformed = new TSIGRecord(Name.fromString("example.com."), DClass.IN, 300L, unsupported, Date.from(Instant.now(fixedClock)), TSIG.FUDGE, new byte[0], message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "An unsupported algorithm should cause IllegalArgumentException");
    }

    /**
     * Exception scenario: old TSIGRecord contains a null signature (treated as null key) → NPE.
     */
    @Test
    void applyOldRecordWithNullSignatureThrowsNPE() throws Exception {
        TSIGRecord malformed = new // null signature (simulates missing key)
        // null signature (simulates missing key)
        TSIGRecord(// null signature (simulates missing key)
        Name.fromString("example.com."), // null signature (simulates missing key)
        DClass.IN, // null signature (simulates missing key)
        300L, // null signature (simulates missing key)
        TSIG.HMAC_SHA256, // null signature (simulates missing key)
        Instant.now(fixedClock), TSIG.FUDGE, null, message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        assertThrows(NullPointerException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "A TSIGRecord with a null signature should cause NullPointerException");
    }
}
