```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.stream.Stream;

import javax.crypto.Mac;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit‑tests for {@link TSIG#generate(Message, byte[], int, TSIGRecord, boolean)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>error‑code branches that decide whether an {@code Mac} is created,
 *   <li>handling of the {@code fullSignature} flag,
 *   <li>behaviour when an old {@link TSIGRecord} is supplied,
 *   <li>null arguments (Message, payload, secret key, algorithm) and the resulting
 *       exceptions,
 *   <li>the effect of an unsupported algorithm on {@code initHmac()}.
 * </ul>
 *
 * <p>All tests use the public API ( {@code generate(...)} and {@code apply(...)} ) – no
 * reflection is used to access private members except when we deliberately need to inject a
 * malformed state (e.g. a {@code null} secret key).
 */
@ExtendWith(MockitoExtension.class)
class TSIGGenerateTest {

    /** TSIG instance used by most tests – SHA‑256 algorithm, deterministic key. */
    private TSIG tsig;

    /** A minimal DNS query message used as the base for signing. */
    private Message queryMessage;

    @BeforeEach
    void setUp() throws IOException {
        // algorithm, name, and base64‑encoded secret key
        tsig = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");
        Record q = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(q);
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests for the three error codes that trigger HMAC init */
    /* --------------------------------------------------------------------- */

    /**
     * All error codes that should initialise an {@code Mac} (NOERROR, BADTIME,
     * BADTRUNC) must produce a non‑null, non‑empty signature irrespective of the
     * {@code fullSignature} flag.
     */
    @ParameterizedTest(name = "[{index}] error={0}, fullSignature={1}")
    @MethodSource("errorAndFullSignatureProvider")
    void generateWhenErrorTriggersHmac(int error, boolean fullSignature) throws IOException {
        // Apply TSIG – this triggers the private generate(...) with the supplied flag.
        tsig.apply(queryMessage, error, null, fullSignature);

        assertTrue(queryMessage.isSigned(), "Message must be marked as signed");
        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec, "TSIGRecord must be present");
        assertEquals(error, rec.getError(), "TSIGRecord.error must match the supplied error");

        byte[] sig = rec.getSignature();
        assertNotNull(sig, "Signature must not be null when HMAC is initialised");
        assertTrue(sig.length > 0, "Signature must contain data");
    }

    private static Stream<Arguments> errorAndFullSignatureProvider() {
        return Stream.of(
                // error, fullSignature
                Arguments.of(Rcode.NOERROR, true),
                Arguments.of(Rcode.NOERROR, false),
                Arguments.of(Rcode.BADTIME, true),
                Arguments.of(Rcode.BADTIME, false),
                Arguments.of(Rcode.BADTRUNC, true),
                Arguments.of(Rcode.BADTRUNC, false)
        );
    }

    /**
     * Errors that do *not* belong to the HMAC‑initialisation set must result in a
     * TSIGRecord whose signature is empty (null or zero‑length).
     */
    @Test
    void generateWhenErrorDoesNotTriggerHmac() throws IOException {
        int error = Rcode.FORMERR; // not in {NOERROR, BADTIME, BADTRUNC}
        tsig.apply(queryMessage, error, null, true);

        assertTrue(queryMessage.isSigned(), "Message should still be marked as signed");
        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec, "TSIGRecord must be present");
        assertEquals(error, rec.getError(), "Error code must be propagated");

        byte[] sig = rec.getSignature();
        assertTrue(sig == null || sig.length == 0,
                "Signature must be empty when HMAC is not initialised");
    }

    /* --------------------------------------------------------------------- */
    /*  Full‑signature flag variations */
    /* --------------------------------------------------------------------- */

    /**
     * When {@code fullSignature} is {@code false} the generated signature must be
     * omitted (null or zero‑length). We compare the result with a full‑signature
     * generation on a fresh message.
     */
    @Test
    void generatePartialSignatureOmitsMac() throws IOException {
        // Full signature – used as a reference for a non‑empty MAC.
        TSIGRecord full = tsig.generate(queryMessage, queryMessage.toWire(),
                Rcode.NOERROR, null);
        assertNotNull(full);
        assertNotNull(full.getSignature());
        assertTrue(full.getSignature().length > 0, "Full signature must contain data");

        // Partial signature – should produce an empty MAC.
        Message other = Message.newQuery(
                Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(other, Rcode.NOERROR, null, false);

        TSIGRecord partial = other.getTSIG();
        assertNotNull(partial);
        byte[] sig = partial.getSignature();
        assertTrue(sig == null || sig.length == 0,
                "Partial signature should be empty when fullSignature=false");
    }

    /* --------------------------------------------------------------------- */
    /*  Old TSIGRecord handling */
    /* --------------------------------------------------------------------- */

    /**
     * When an old {@link TSIGRecord} is supplied together with an error that
     * triggers HMAC initialisation, the new record must still contain a valid
     * (non‑empty) MAC and the supplied error code.
     */
    @Test
    void generateWithOldRecordAndSigningError() throws IOException {
        // Create an old TSIGRecord by signing a preliminary message.
        Message prior = Message.newQuery(
                Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(prior, Rcode.NOERROR, null, true);
        TSIGRecord old = prior.getTSIG();
        assertNotNull(old, "Old TSIGRecord must exist");

        // Now generate a new record using the old one.
        Message next = Message.newQuery(
                Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(next, Rcode.BADTIME, old, true);

        TSIGRecord nextRec = next.getTSIG();
        assertNotNull(nextRec);
        assertEquals(Rcode.BADTIME, nextRec.getError(),
                "Error field must reflect the supplied error");
        assertNotNull(nextRec.getSignature(), "Signature must be present when HMAC is initialised");
        assertTrue(nextRec.getSignature().length > 0, "Signature must contain data");
    }

    /**
     * If the error does *not* trigger HMAC initialisation, an old record must be
     * ignored for MAC computation – the resulting signature is empty.
     */
    @Test
    void generateWithOldRecordAndNonSigningError() throws IOException {
        // Old record (generated with a signing error)
        Message prior = Message.newQuery(
                Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(prior, Rcode.NOERROR, null, true);
        TSIGRecord old = prior.getTSIG();
        assertNotNull(old);

        // Use a non‑signing error (FORMERR)
        Message next = Message.newQuery(
                Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(next, Rcode.FORMERR, old, true);

        TSIGRecord nextRec = next.getTSIG();
        assertNotNull(nextRec);
        assertEquals(Rcode.FORMERR, nextRec.getError(),
                "Error field must reflect the supplied non‑signing error");
        byte[] sig = nextRec.getSignature();
        assertTrue(sig == null || sig.length == 0,
                "Signature must be empty when HMAC is not initialised, even with an old record");
    }

    /* --------------------------------------------------------------------- */
    /*  Null handling and exceptional paths */
    /* --------------------------------------------------------------------- */

    /**
     * Passing {@code null} for the {@link Message} argument must result in a
     * {@link NullPointerException}. The public {@code generate(...)} overload is
     * used because it forwards to the private implementation.
     */
    @Test
    void generateNullMessageThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> tsig.generate(null, new byte[]{0x01}, Rcode.NOERROR, null),
                "Null Message should cause NullPointerException");
    }

    /**
     * When the payload {@code byte[]} is {@code null} and the error triggers HMAC
     * initialisation, the method must treat the payload as zero‑length and still
     * produce a valid signature.
     */
    @Test
    void generateNullPayloadWithSigningError() throws IOException {
        // The public generate defaults fullSignature=true.
        TSIGRecord rec = tsig.generate(queryMessage, null, Rcode.NOERROR, null);
        assertNotNull(rec, "Record must be returned even with null payload");
        assertEquals(Rcode.NOERROR, rec.getError());
        assertNotNull(rec.getSignature(), "Signature must be generated despite null payload");
        assertTrue(rec.getSignature().length > 0, "Signature must contain data");
    }

    /**
     * When the payload is {@code null} but the error does not require HMAC, the
     * method must not throw an exception and must return a record with an empty
     * signature.
     */
    @Test
    void generateNullPayloadWithoutSigningError() throws IOException {
        TSIGRecord rec = tsig.generate(queryMessage, null, Rcode.FORMERR, null);
        assertNotNull(rec);
        assertEquals(Rcode.FORMERR, rec.getError());
        byte[] sig = rec.getSignature();
        assertTrue(sig == null || sig.length == 0,
                "Signature must be empty when HMAC is not initialised even with null payload");
    }

    /**
     * Simulate a TSIG instance with a missing secret key. The private field that
     * holds the {@link SecretKey} is set to {@code null} via reflection. Any
     * attempt to initialise the HMAC must result in an {@link IllegalStateException}
     * (or a wrapped {@link GeneralSecurityException}).
     */
    @Test
    void generateWithMissingSecretKeyThrows() throws Exception {
        // Use reflection to nullify the secret key inside the TSIG instance.
        Field secretField = TSIG.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(tsig, null);

        // The error triggers HMAC creation; we expect an exception.
        assertThrows(RuntimeException.class,
                () -> tsig.apply(queryMessage, Rcode.NOERROR, null, true),
                "Missing secret key should cause a runtime exception during HMAC init");
    }

    /**
     * Construct a TSIG with an unsupported algorithm name. The subsequent call to
     * {@code generate} must cause a {@link java.security.NoSuchAlgorithmException}
     * (wrapped in a runtime exception) because {@code initHmac()} cannot obtain a
     * {@link Mac} instance.
     */
    @Test
    void generateWithUnsupportedAlgorithmThrows() throws IOException {
        // Create a TSIG with a bogus algorithm name.
        Name bogusAlg = Name.fromString("UNSUPPORTED-ALG.");
        TSIG badAlgoTSIG = new TSIG(bogusAlg, "example.", "12345678");

        // Expect an exception when trying to sign.
        assertThrows(RuntimeException.class,
                () -> badAlgoTSIG.apply(queryMessage, Rcode.NOERROR, null, true),
                "Unsupported algorithm should cause a runtime exception during HMAC init");
    }

    /**
     * Verify that an unknown (numeric) error code that does not belong to the
     * signing set does not initialise an HMAC and yields an empty signature.
     */
    @Test
    void generateWithUnknownErrorCode() throws IOException {
        int unknownError = 9999; // not a defined Rcode constant
        tsig.apply(queryMessage, unknownError, null, true);

        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec);
        assertEquals(unknownError, rec.getError(),
                "TSIGRecord.error must reflect the unknown error code");
        byte[] sig = rec.getSignature();
        assertTrue(sig == null || sig.length == 0,
                "Signature must be empty for unknown (non‑signing) error codes");
    }

    /**
     * Negative error codes should also bypass HMAC initialisation.
     */
    @Test
    void generateWithNegativeErrorCode() throws IOException {
        int negativeError = -1;
        tsig.apply(queryMessage, negativeError, null, true);

        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec);
        assertEquals(negativeError, rec.getError(),
                "TSIGRecord.error must reflect the negative error code");
        byte[] sig = rec.getSignature();
        assertTrue(sig == null || sig.length == 0,
                "Signature must be empty for negative error codes");
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods (if needed) */
    /* --------------------------------------------------------------------- */

    /**
     * Utility to create a TSIGRecord by signing a fresh message. Used to obtain an
     * “old” record for chaining tests.
     */
    private static TSIGRecord signMessage(TSIG tsig, Message msg, int error) throws IOException {
        tsig.apply(msg, error, null, true);
        return msg.getTSIG();
    }
}
```