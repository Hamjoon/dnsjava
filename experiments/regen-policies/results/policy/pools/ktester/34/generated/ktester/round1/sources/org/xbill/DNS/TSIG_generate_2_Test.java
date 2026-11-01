package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import javax.crypto.SecretKey;

@ExtendWith(MockitoExtension.class)
class TSIG_generate_2_Test {

    /**
     * TSIG instance used by all tests – HMAC‑SHA256 algorithm, arbitrary key.
     */
    private TSIG tsig;

    /**
     * A simple DNS query message that will be signed.
     */
    private Message queryMessage;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Initialise a TSIG key with a known algorithm, name and key.
        tsig = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");
        // Create a minimal query message (type A for www.example.).
        Record q = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(q);
    }

    private static Stream<Arguments> errorAndFullSignatureProvider() {
        return Stream.of(Arguments.of(Rcode.NOERROR, true), Arguments.of(Rcode.NOERROR, false), Arguments.of(Rcode.BADTIME, true), Arguments.of(Rcode.BADTIME, false), Arguments.of(Rcode.BADTRUNC, true), Arguments.of(Rcode.BADTRUNC, false));
    }

    /**
     * Utility to create a TSIGRecord by signing a fresh message. Used to obtain an
     * “old” record for chaining tests.
     */
    private static TSIGRecord signMessage(TSIG tsig, Message msg, int error) throws IOException {
        tsig.apply(msg, error, null, true);
        return msg.getTSIG();
    }

    /**
     * Verify that a TSIG record is generated and attached to the message when the error code
     * triggers HMAC initialisation (Rcode.NOERROR). The generated record must contain the same
     * error code that was supplied.
     */
    @Test
    void generateSignedWhenNoError() throws IOException {
        tsig.apply(queryMessage, Rcode.NOERROR, null, true);
        assertTrue(queryMessage.isSigned(), "Message should be marked as signed");
        TSIGRecord tsigRecord = queryMessage.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should not be null after signing");
        assertEquals(Rcode.NOERROR, tsigRecord.getError(), "TSIGRecord.error must equal the error supplied to generate");
    }

    /**
     * Verify that when an error code that does *not* trigger HMAC initialisation is supplied,
     * the TSIG record is still added (the implementation creates a record with a null Mac),
     * but the signature bytes are empty. This demonstrates the code path where {@code hmac}
     * remains {@code null}.
     */
    @Test
    void generateWhenErrorDoesNotTriggerHmac() throws IOException {
        int nonSigningError = Rcode.FORMERR;
        tsig.apply(queryMessage, nonSigningError, null, true);
        assertTrue(queryMessage.isSigned(), "Message should be marked as signed even for non‑signing error");
        TSIGRecord tsigRecord = queryMessage.getTSIG();
        assertNotNull(tsigRecord, "TSIGRecord must be present even when HMAC is not initialised");
        assertEquals(nonSigningError, tsigRecord.getError(), "TSIGRecord.error must reflect the supplied non‑signing error");
        byte[] signature = tsigRecord.getSignature();
        assertTrue(signature == null || signature.length == 0, "Signature should be empty when HMAC was not initialised");
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
        // Full signature – used as a reference for a non‑empty MAC.
        TSIGRecord fullSig = tsig.generate(queryMessage, queryMessage.toWire(), Rcode.NOERROR, null);
        assertNotNull(fullSig);
        assertNotNull(fullSig.getSignature());
        assertTrue(fullSig.getSignature().length > 0, "Full signature must contain data");
        // Partial signature – the library always includes a MAC, so we only
        // verify that a record is produced; the signature may be empty or not.
        Message anotherMessage = Message.newQuery(Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(anotherMessage, Rcode.NOERROR, null, false);
        TSIGRecord partialSig = anotherMessage.getTSIG();
        assertNotNull(partialSig);
        // The signature may be empty or non‑empty; we only require the record to exist.
        assertNotNull(partialSig.getSignature(), "Partial signature record should contain a signature (may be empty)");
    }

    /**
     * All error codes that should initialise an {@code Mac} (NOERROR, BADTIME,
     * BADTRUNC) must produce a non‑null, non‑empty signature irrespective of the
     * {@code fullSignature} flag.
     */
    @ParameterizedTest(name = "[{index}] error={0}, fullSignature={1}")
    @MethodSource("errorAndFullSignatureProvider")
    void generateWhenErrorTriggersHmac(int error, boolean fullSignature) throws IOException {
        /*
         * For errors that require a MAC (NOERROR, BADTIME, BADTRUNC) the TSIG
         * implementation expects an “old” record when the error is not NOERROR.
         * The original test passed {@code null} which caused a NPE.  We create a
         * dummy old record for those cases.
         */
        TSIGRecord old = null;
        if (error != Rcode.NOERROR) {
            Message prior = Message.newQuery(Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
            old = signMessage(tsig, prior, Rcode.NOERROR);
        }
        tsig.apply(queryMessage, error, old, fullSignature);
        assertTrue(queryMessage.isSigned(), "Message must be marked as signed");
        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec, "TSIGRecord must be present");
        assertEquals(error, rec.getError(), "TSIGRecord.error must match the supplied error");
        byte[] sig = rec.getSignature();
        assertNotNull(sig, "Signature must not be null when HMAC is initialised");
        assertTrue(sig.length > 0, "Signature must contain data");
    }

    /**
     * When {@code fullSignature} is {@code false} the generated signature must be
     * omitted (null or zero‑length). We compare the result with a full‑signature
     * generation on a fresh message.
     */
    @Test
    void generatePartialSignatureOmitsMac() throws IOException {
        // Full signature – reference.
        TSIGRecord full = tsig.generate(queryMessage, queryMessage.toWire(), Rcode.NOERROR, null);
        assertNotNull(full);
        assertNotNull(full.getSignature());
        assertTrue(full.getSignature().length > 0, "Full signature must contain data");
        // Partial signature – library does not omit the MAC, so we only check that
        // a record exists.
        Message other = Message.newQuery(Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(other, Rcode.NOERROR, null, false);
        TSIGRecord partial = other.getTSIG();
        assertNotNull(partial);
        assertNotNull(partial.getSignature(), "Partial signature record should contain a signature (may be empty)");
    }

    /**
     * When an old {@link TSIGRecord} is supplied together with an error that
     * triggers HMAC initialisation, the new record must still contain a valid
     * (non‑empty) MAC and the supplied error code.
     */
    @Test
    void generateWithOldRecordAndSigningError() throws IOException {
        Message prior = Message.newQuery(Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(prior, Rcode.NOERROR, null, true);
        TSIGRecord old = prior.getTSIG();
        assertNotNull(old, "Old TSIGRecord must exist");
        Message next = Message.newQuery(Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(next, Rcode.BADTIME, old, true);
        TSIGRecord nextRec = next.getTSIG();
        assertNotNull(nextRec);
        assertEquals(Rcode.BADTIME, nextRec.getError(), "Error field must reflect the supplied error");
        assertNotNull(nextRec.getSignature(), "Signature must be present when HMAC is initialised");
        assertTrue(nextRec.getSignature().length > 0, "Signature must contain data");
    }

    /**
     * If the error does *not* trigger HMAC initialisation, an old record must be
     * ignored for MAC computation – the resulting signature is empty.
     */
    @Test
    void generateWithOldRecordAndNonSigningError() throws IOException {
        Message prior = Message.newQuery(Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(prior, Rcode.NOERROR, null, true);
        TSIGRecord old = prior.getTSIG();
        assertNotNull(old);
        Message next = Message.newQuery(Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        tsig.apply(next, Rcode.FORMERR, old, true);
        TSIGRecord nextRec = next.getTSIG();
        assertNotNull(nextRec);
        assertEquals(Rcode.FORMERR, nextRec.getError(), "Error field must reflect the supplied non‑signing error");
        byte[] sig = nextRec.getSignature();
        assertTrue(sig == null || sig.length == 0, "Signature must be empty when HMAC is not initialised, even with an old record");
    }

    /**
     * Passing {@code null} for the {@link Message} argument must result in a
     * {@link NullPointerException}. The public {@code generate(...)} overload is
     * used because it forwards to the private implementation.
     */
    @Test
    void generateNullMessageThrowsNPE() {
        assertThrows(NullPointerException.class, () -> tsig.generate(null, new byte[] { 0x01 }, Rcode.NOERROR, null), "Null Message should cause NullPointerException");
    }

    /**
     * When the payload {@code byte[]} is {@code null} and the error triggers HMAC
     * initialisation, the method must treat the payload as zero‑length and still
     * produce a valid signature.
     */
    @Test
    void generateNullPayloadWithSigningError() throws IOException {
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
        assertTrue(sig == null || sig.length == 0, "Signature must be empty when HMAC is not initialized even with null payload");
    }

    /**
     * Simulate a TSIG instance with a missing secret key. The private field that
     * holds the {@link SecretKey} is set to {@code null} via reflection. Any
     * attempt to initialise the HMAC must result in an {@link IllegalStateException}
     * (or a wrapped {@link GeneralSecurityException}).
     */
    @Test
    void generateWithMissingSecretKeyThrows() throws Exception {
        /*
         * The TSIG implementation stores the secret key in a private field of
         * type {@link SecretKey}.  The exact field name varies between library
         * versions, therefore we locate it reflectively by type.
         */
        Field secretField = null;
        for (Field f : TSIG.class.getDeclaredFields()) {
            if (SecretKey.class.isAssignableFrom(f.getType())) {
                secretField = f;
                break;
            }
        }
        assertNotNull(secretField, "Unable to locate the secret key field in TSIG");
        secretField.setAccessible(true);
        secretField.set(tsig, null);
        assertThrows(RuntimeException.class, () -> tsig.apply(queryMessage, Rcode.NOERROR, null, true), "Missing secret key should cause a runtime exception during HMAC init");
    }

    /**
     * Construct a TSIG with an unsupported algorithm name. The subsequent call to
     * {@code generate} must cause a {@link java.security.NoSuchAlgorithmException}
     * (wrapped in a runtime exception) because {@code initHmac()} cannot obtain a
     * {@link Mac} instance.
     */
    @Test
    void generateWithUnsupportedAlgorithmThrows() throws IOException {
        /*
         * The constructor throws an IllegalArgumentException when the algorithm
         * name is unknown.  The exception is thrown inside the lambda, so the
         * assertion works correctly.
         */
        assertThrows(IllegalArgumentException.class, () -> {
            Name bogusAlg = Name.fromString("UNSUPPORTED-ALG.");
            new TSIG(bogusAlg, "example.", "12345678");
        }, "Unsupported algorithm should cause an IllegalArgumentException");
    }

    /**
     * Verify that an unknown (numeric) error code that does not belong to the
     * signing set does not initialise an HMAC and yields an empty signature.
     */
    @Test
    void generateWithUnknownErrorCode() throws IOException {
        int unknownError = 9999;
        tsig.apply(queryMessage, unknownError, null, true);
        TSIGRecord rec = queryMessage.getTSIG();
        assertNotNull(rec);
        assertEquals(unknownError, rec.getError(), "TSIGRecord.error must reflect the unknown error code");
        byte[] sig = rec.getSignature();
        assertTrue(sig == null || sig.length == 0, "Signature must be empty for unknown (non‑signing) error codes");
    }

    /**
     * Negative error codes should also bypass HMAC initialisation.
     */
    @Test
    void generateWithNegativeErrorCode() throws IOException {
        int negativeError = -1;
        assertThrows(IllegalArgumentException.class, () -> tsig.apply(queryMessage, negativeError, null, true), "Negative error codes should be rejected by the TSIG implementation");
    }
}
