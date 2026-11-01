```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link Message#setTSIG(TSIG)} and the three‑argument overload
 * {@link Message#setTSIG(TSIG, int, TSIGRecord)}.
 *
 * <p>The tests cover normal signing, replacement, intermediate state handling,
 * verification (successful & failed), error handling, and illegal‑argument scenarios.
 * Private fields such as {@code tsigState} are accessed via reflection where required.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_Test {

    private Message message;
    private TSIG tsigKey;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal query message (question only).
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        message = Message.newQuery(question);

        // Build a default TSIG key (HMAC‑MD5) used by many tests.
        Name alg = Name.fromConstantString("hmac-md5.sig-alg.reg.int.");
        Name name = Name.fromConstantString("key.example.com.");
        byte[] keyBytes = Base64.getDecoder().decode("YWJjZDEyMw=="); // "abcd123"
        SecretKey secret = new SecretKeySpec(keyBytes, "HmacMD5");
        tsigKey = new TSIG(alg, name, secret, Clock.systemUTC());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /** Helper to read the private {@code tsigState} field via reflection. */
    private int getTsigState(Message m) throws ReflectiveOperationException {
        Field f = Message.class.getDeclaredField("tsigState");
        f.setAccessible(true);
        return f.getInt(m);
    }

    /** Helper to create a {@link TSIG} with given algorithm name and secret bytes. */
    private TSIG createTsig(String algName, byte[] secretBytes) {
        Name alg = Name.fromConstantString(algName);
        Name name = Name.fromConstantString("key.example.com.");
        SecretKey secret = new SecretKeySpec(secretBytes, "HmacMD5"); // algorithm name not important here
        return new TSIG(alg, name, secret, Clock.systemUTC());
    }

    /* --------------------------------------------------------------------- */
    /* Existing tests (kept unchanged)                                      */
    /* --------------------------------------------------------------------- */

    /** Verify that calling {@code setTSIG} adds a TSIG record to the additional section. */
    @Test
    void setTSIGAddsRecordToAdditionalSection() {
        assertTrue(message.getSection(Section.ADDITIONAL).isEmpty(),
                "Additional section should start empty");

        message.setTSIG(tsigKey);

        assertEquals(1, message.getSection(Section.ADDITIONAL).size(),
                "Additional section should contain one record after setTSIG");
        assertTrue(message.getSection(Section.ADDITIONAL).get(0) instanceof TSIGRecord,
                "Record in additional section should be a TSIGRecord");

        TSIGRecord generated = message.getTSIG();
        assertNotNull(generated, "Generated TSIGRecord should not be null");
        assertEquals(tsigKey.getName(), generated.getName(),
                "TSIGRecord name should match the key name");
        assertTrue(message.isSigned(), "Message should report being signed after setTSIG");
    }

    /** Verify that calling {@code setTSIG} with a {@code null} argument throws NPE. */
    @Test
    void setTSIGWithNullKeyThrowsException() {
        assertThrows(NullPointerException.class, () -> message.setTSIG(null),
                "setTSIG(null) should throw NullPointerException");
    }

    /** Verify that a second call to {@code setTSIG} replaces the previous TSIG record. */
    @Test
    void setTSIGReplacesExistingRecord() throws Exception {
        message.setTSIG(tsigKey);
        TSIGRecord first = message.getTSIG();
        assertNotNull(first, "First generated TSIGRecord should exist");

        // Build a different key (HMAC‑SHA256)
        Name otherAlg = Name.fromConstantString("hmac-sha256.");
        Name otherName = Name.fromConstantString("otherkey.example.com.");
        byte[] otherKeyBytes = Base64.getDecoder().decode("c2Vjb25kS2V5"); // "secondKey"
        SecretKey otherSecret = new SecretKeySpec(otherKeyBytes, "HmacSHA256");
        TSIG otherTsig = new TSIG(otherAlg, otherName, otherSecret, Clock.systemUTC());

        // Second TSIG – should replace the first one.
        message.setTSIG(otherTsig);
        TSIGRecord second = message.getTSIG();
        assertNotNull(second, "Second generated TSIGRecord should exist");
        assertNotSame(first, second, "TSIGRecord instance should be replaced");

        // Only one TSIGRecord must remain in the additional section.
        assertEquals(1, message.getSection(Section.ADDITIONAL).size(),
                "Only one TSIGRecord should be present after replacement");
        assertEquals(otherTsig.getName(), second.getName(),
                "Replaced TSIGRecord should reflect the new key name");
    }

    /* --------------------------------------------------------------------- */
    /* New tests covering the JSON test matrix                              */
    /* --------------------------------------------------------------------- */

    /** Valid TSIG addition (default NOERROR) – same expectations as the first test. */
    @Test
    void validTsigAdditionDefaultNoError() throws Exception {
        message.setTSIG(tsigKey);
        assertTrue(message.isSigned(), "Message should be signed");
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec, "TSIGRecord must not be null");
        assertEquals("hmac-md5.sig-alg.reg.int.", rec.getAlgorithm().toString(),
                "Algorithm should be hmac‑md5");
        assertEquals(Message.TSIG_SIGNED, getTsigState(message),
                "Internal state must be TSIG_SIGNED");
    }

    /** Null TSIG removal – calling setTSIG(null) should clear state. */
    @Test
    void nullTsigRemovalClearsState() throws Exception {
        // First attach a TSIG to have something to clear.
        message.setTSIG(tsigKey);
        assertTrue(message.isSigned());

        // Now remove it by passing null.
        message.setTSIG((TSIG) null);

        assertFalse(message.isSigned(), "Message should not be signed after null TSIG");
        assertNull(message.getTSIG(), "getTSIG() should return null after removal");
        assertEquals(Message.TSIG_UNSIGNED, getTsigState(message),
                "tsigState should be TSIG_UNSIGNED after null removal");
    }

    /** Error code handling (non‑NOERROR) using the three‑argument overload. */
    @Test
    void errorCodeNonNoErrorMarksFailed() throws Exception {
        message.setTSIG(tsigKey, Rcode.FORMERR, null);
        assertFalse(message.isSigned(), "Message must not be signed on error");
        assertEquals(Message.TSIG_FAILED, getTsigState(message),
                "State should be TSIG_FAILED for non‑NOERROR");
    }

    /** Existing TSIG replacement – second call with a different key. */
    @Test
    void existingTsigReplacementKeepsSignedState() throws Exception {
        message.setTSIG(tsigKey);
        TSIGRecord first = message.getTSIG();
        assertTrue(message.isSigned());

        // New key (same algorithm, different secret)
        TSIG newKey = createTsig("hmac-md5.sig-alg.reg.int.", "different".getBytes());
        message.setTSIG(newKey);
        TSIGRecord second = message.getTSIG();

        assertNotSame(first, second, "TSIGRecord should be replaced");
        assertTrue(message.isSigned(), "Message should remain signed");
        assertEquals("hmac-md5.sig-alg.reg.int.", second.getAlgorithm().toString(),
                "Algorithm should stay the same");
    }

    /** Successful verification with a query TSIGRecord. */
    @Test
    void verificationWithQueryTsigSuccessful() throws Exception {
        // Build a query TSIGRecord that matches the key.
        TSIGRecord queryTsig = new TSIGRecord(Name.fromString("example.com."),
                DClass.IN, 300, tsigKey, new byte[0]);

        message.setTSIG(tsigKey, Rcode.NOERROR, queryTsig);

        assertEquals(Message.TSIG_VERIFIED, getTsigState(message),
                "State should be TSIG_VERIFIED after successful verification");
        assertTrue(message.isVerified(), "isVerified() must be true");
    }

    /** Failed verification with a mismatching query TSIGRecord. */
    @Test
    void verificationWithQueryTsigFailed() throws Exception {
        // Build a query TSIGRecord with a different key.
        TSIG wrongKey = createTsig("hmac-md5.sig-alg.reg.int.", "wrong".getBytes());
        TSIGRecord queryTsig = new TSIGRecord(Name.fromString("example.com."),
                DClass.IN, 300, wrongKey, new byte[0]);

        message.setTSIG(tsigKey, Rcode.NOERROR, queryTsig);

        assertEquals(Message.TSIG_FAILED, getTsigState(message),
                "State should be TSIG_FAILED after verification failure");
        assertFalse(message.isVerified(), "isVerified() must be false");
    }

    /** Intermediate state handling (no query TSIG, no error). */
    @Test
    void intermediateStateWhenNoQueryTsig() throws Exception {
        message.setTSIG(tsigKey, Rcode.NOERROR, null);
        assertEquals(Message.TSIG_INTERMEDIATE, getTsigState(message),
                "State should be TSIG_INTERMEDIATE");
        assertFalse(message.isSigned(), "Message should not be signed in intermediate state");
    }

    /** Idempotent setTSIG on already signed message – same key twice. */
    @Test
    void idempotentSetTsigSameKey() throws Exception {
        message.setTSIG(tsigKey);
        TSIGRecord first = message.getTSIG();
        int stateAfterFirst = getTsigState(message);

        // Second call with the *same* TSIG instance.
        message.setTSIG(tsigKey);
        TSIGRecord second = message.getTSIG();

        assertSame(first, second, "TSIGRecord instance should not change on idempotent call");
        assertEquals(stateAfterFirst, getTsigState(message), "State must remain unchanged");
        assertTrue(message.isSigned(), "Message stays signed");
    }

    /** Set TSIG with zero‑length secret – still considered signed. */
    @Test
    void zeroLengthSecretIsAccepted() throws Exception {
        TSIG zeroSecretKey = createTsig("hmac-md5.sig-alg.reg.int.", new byte[0]);
        message.setTSIG(zeroSecretKey);
        assertTrue(message.isSigned(), "Message should be signed even with zero‑length secret");
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec);
        assertEquals(0, rec.getKey().length, "Secret length should be zero");
    }

    /** Unsupported algorithm should cause IllegalArgumentException. */
    @Test
    void unsupportedAlgorithmThrows() {
        TSIG unsupported = createTsig("unsupported-algo.", "secret".getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> message.setTSIG(unsupported),
                "Unsupported algorithm must cause IllegalArgumentException");
    }

    /** After a verified state, calling one‑arg setTSIG must reset verification flag. */
    @Test
    void setTsigAfterVerifiedResetsVerification() throws Exception {
        // Simulate a verified incoming TSIG.
        TSIGRecord queryTsig = new TSIGRecord(Name.fromString("example.com."),
                DClass.IN, 300, tsigKey, new byte[0]);
        message.setTSIG(tsigKey, Rcode.NOERROR, queryTsig);
        assertTrue(message.isVerified(), "Pre‑condition: message verified");

        // Now call the one‑argument overload.
        message.setTSIG(tsigKey);
        assertTrue(message.isSigned(), "Message must be signed after final setTSIG");
        assertFalse(message.isVerified(), "Verification flag must be cleared");
        assertEquals(Message.TSIG_SIGNED, getTsigState(message),
                "State must be TSIG_SIGNED after final signing");
    }

    /** Replace TSIG with same algorithm but different secret. */
    @Test
    void replaceTsigSameAlgorithmDifferentSecret() throws Exception {
        TSIG first = createTsig("hmac-sha1.", "first".getBytes());
        message.setTSIG(first);
        TSIGRecord rec1 = message.getTSIG();
        assertEquals("first".getBytes().length, rec1.getKey().length);

        TSIG second = createTsig("hmac-sha1.", "second".getBytes());
        message.setTSIG(second);
        TSIGRecord rec2 = message.getTSIG();
        assertEquals("second".getBytes().length, rec2.getKey().length);
        assertEquals("hmac-sha1.", rec2.getAlgorithm().toString());
        assertTrue(message.isSigned());
        assertEquals(Message.TSIG_SIGNED, getTsigState(message));
    }

    /** Transition from intermediate to signed state. */
    @Test
    void intermediateToSignedTransition() throws Exception {
        // Step 1 – put message into intermediate state.
        message.setTSIG(tsigKey, Rcode.NOERROR, null);
        assertEquals(Message.TSIG_INTERMEDIATE, getTsigState(message));
        assertFalse(message.isSigned());

        // Step 2 – finalize signing with the one‑argument overload.
        message.setTSIG(tsigKey);
        assertEquals(Message.TSIG_SIGNED, getTsigState(message));
        assertTrue(message.isSigned());
    }

    /** Null algorithm in TSIG constructor should throw IllegalArgumentException. */
    @Test
    void nullAlgorithmInTsigThrows() {
        Name name = Name.fromConstantString("key.example.com.");
        byte[] secretBytes = "secret".getBytes();
        SecretKey secret = new SecretKeySpec(secretBytes, "HmacMD5");
        assertThrows(IllegalArgumentException.class,
                () -> new TSIG(null, name, secret, Clock.systemUTC()),
                "Null algorithm must cause IllegalArgumentException");
    }

    /** Null secret in TSIG constructor should throw IllegalArgumentException. */
    @Test
    void nullSecretInTsigThrows() {
        Name alg = Name.fromConstantString("hmac-md5.sig-alg.reg.int.");
        Name name = Name.fromConstantString("key.example.com.");
        assertThrows(IllegalArgumentException.class,
                () -> new TSIG(alg, name, null, Clock.systemUTC()),
                "Null secret must cause IllegalArgumentException");
    }

    /** Invalid error code (outside Rcode range) should cause IllegalArgumentException. */
    @Test
    void invalidErrorCodeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> message.setTSIG(tsigKey, 99999, null),
                "Invalid error code must cause IllegalArgumentException");
    }

    /** TSIG attached to another Message should raise IllegalStateException. */
    @Test
    void tsigReusedAcrossMessagesThrows() {
        // Attach TSIG to first message.
        Message first = Message.newQuery(message.getQuestion());
        first.setTSIG(tsigKey);

        // Attempt to reuse the same TSIG instance on a second message.
        Message second = Message.newQuery(message.getQuestion());
        assertThrows(IllegalStateException.class,
                () -> second.setTSIG(tsigKey),
                "Reusing a TSIG already bound to another Message must throw IllegalStateException");
    }

    /* --------------------------------------------------------------------- */
    /* Parameterized tests for simple valid/invalid algorithm strings         */
    /* --------------------------------------------------------------------- */

    static Stream<String> unsupportedAlgorithms() {
        return Stream.of("hmac-foo.", "unknown-algo.", "hmac-md5-foo.");
    }

    @ParameterizedTest
    @MethodSource("unsupportedAlgorithms")
    void unsupportedAlgorithmParameterized(String alg) {
        TSIG tsig = createTsig(alg, "secret".getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> message.setTSIG(tsig),
                "Algorithm '" + alg + "' should be unsupported");
    }
}
```