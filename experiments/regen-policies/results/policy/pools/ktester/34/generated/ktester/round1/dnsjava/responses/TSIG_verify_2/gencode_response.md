```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.Mac;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional tests for {@link TSIG#verify(Message, byte[], TSIGRecord, boolean)} that cover
 * the edge‑case scenarios described in the JSON test specifications.
 *
 * <p>These tests focus on:
 * <ul>
 *   <li>Null arguments (Message, messageBytes, TSIGRecord)</li>
 *   <li>Invalid TSIGRecord data (null algorithm, unsupported algorithm, null/invalid key)</li>
 *   <li>MAC‑related failures (unsupported algorithm, MAC not initialized)</li>
 * </ul>
 *
 * The original functional tests (full signature, tampered wire, etc.) remain unchanged.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_verify_2_Test {

    /** Fixed clock used for deterministic TSIG timestamps. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

    /** A TSIG key used for signing and verification. */
    private static TSIG tsigKey;

    /** A simple A‑record used as the question section of the test messages. */
    private static Record questionRecord;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Use the SHA‑256 algorithm (the most common default in the library)
        tsigKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");
        // Replace the internal clock with a fixed one so that the generated
        // TSIGRecord has a known timeSigned value.
        Field clockField = TSIG.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(tsigKey, FIXED_CLOCK);

        // Prepare a generic DNS question record.
        questionRecord = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
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
        // No global cleanup required.
    }

    /* ---------------------------------------------------------------------- */
    /* Helper utilities                                                       */
    /* ---------------------------------------------------------------------- */

    /** Holds a parsed Message together with its raw wire representation. */
    private static final class ParsedMessage {
        final Message message;
        final byte[] wire;

        ParsedMessage(Message message, byte[] wire) {
            this.message = message;
            this.wire = wire;
        }
    }

    /** Creates a signed query and returns both the parsed Message and its wire bytes. */
    private static ParsedMessage signQuery() throws IOException {
        Message query = Message.newQuery(questionRecord);
        query.setTSIG(tsigKey);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        return new ParsedMessage(parsed, wire);
    }

    /** Returns a fresh, unsigned query Message (used for null‑argument tests). */
    private static Message validMessage() {
        return Message.newQuery(questionRecord);
    }

    /** Generates a valid TSIGRecord for the given Message using the test TSIG key. */
    private static TSIGRecord validTSIGRecord(Message msg) {
        // The generate() method creates a TSIGRecord that matches the supplied message.
        return tsigKey.generate(msg, msg.toWire(Message.MAXLENGTH), Rcode.NOERROR, null);
    }

    /** Creates a TSIGRecord whose algorithm field is set to the supplied Name (may be null). */
    private static TSIGRecord tsigRecordWithAlgorithm(Name alg) throws Exception {
        TSIGRecord rec = validTSIGRecord(validMessage());
        Field algField = TSIGRecord.class.getDeclaredField("alg");
        algField.setAccessible(true);
        algField.set(rec, alg);
        return rec;
    }

    /** Creates a TSIGRecord whose secret key (the field used internally by TSIG) is set to the
     *  supplied byte array (may be null or empty). */
    private static TSIGRecord tsigRecordWithKey(byte[] keyBytes) throws Exception {
        // The secret key is stored inside the TSIG object, not the TSIGRecord.
        // To simulate a “null key” situation we create a new TSIG instance with a null key
        // string and then generate a record from it.
        TSIG brokenKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), (String) null);
        // Force the internal SecretKeySpec to be null via reflection.
        Field secretField = TSIG.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(brokenKeyTsig, keyBytes == null ? null : new SecretKeySpec(keyBytes, "HmacSHA256"));
        return brokenKeyTsig.generate(validMessage(),
                validMessage().toWire(Message.MAXLENGTH), Rcode.NOERROR, null);
    }

    /** Returns the private verify method that accepts a Mac instance (used for the
     *  “mac not initialized” scenario). */
    private static Method getPrivateVerifyMethod() throws Exception {
        Method m = TSIG.class.getDeclaredMethod(
                "verify", Message.class, byte[].class, TSIGRecord.class, boolean.class, Mac.class);
        m.setAccessible(true);
        return m;
    }

    /* ---------------------------------------------------------------------- */
    /* Existing functional tests (unchanged)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    void verifyFullSignatureSucceeds() throws Exception {
        ParsedMessage pm = signQuery();

        TSIGRecord requestTSIG = pm.message.getTSIG();

        int result = tsigKey.verify(pm.message, pm.wire, requestTSIG, true);
        assertEquals(Rcode.NOERROR, result, "Verification should succeed with fullSignature=true");
        assertTrue(pm.message.isSigned(), "Message should be marked as signed");
        assertTrue(pm.message.isVerified(), "Message should be marked as verified");
    }

    @Test
    void verifyWithoutFullSignatureSucceeds() throws Exception {
        ParsedMessage pm = signQuery();

        TSIGRecord requestTSIG = pm.message.getTSIG();

        int result = tsigKey.verify(pm.message, pm.wire, requestTSIG, false);
        assertEquals(Rcode.NOERROR, result, "Verification should succeed with fullSignature=false");
        assertTrue(pm.message.isSigned(), "Message should be marked as signed");
        assertTrue(pm.message.isVerified(), "Message should be marked as verified");
    }

    @Test
    void verifyFailsWhenWireIsTampered() throws Exception {
        ParsedMessage pm = signQuery();

        byte[] corruptedWire = pm.wire.clone();
        corruptedWire[12] ^= 0xFF; // flip a byte outside the TSIG MAC

        TSIGRecord requestTSIG = pm.message.getTSIG();

        int result = tsigKey.verify(pm.message, corruptedWire, requestTSIG, true);
        assertNotEquals(Rcode.NOERROR, result, "Verification should fail for tampered wire data");
    }

    @Test
    void verifyWithNullRequestTSIGTreatsAsUnsigned() throws Exception {
        Message query = Message.newQuery(questionRecord);
        query.setTSIG(tsigKey);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);

        int result = tsigKey.verify(parsed, wire, null, true);
        assertEquals(Rcode.NOERROR, result, "Verification with null requestTSIG should return NOERROR");
    }

    /* ---------------------------------------------------------------------- */
    /* New edge‑case tests generated from the JSON specification                */
    /* ---------------------------------------------------------------------- */

    @Test
    void test_verify_nullMessage() {
        byte[] msgBytes = new byte[]{0x00};
        TSIGRecord tsigRec = validTSIGRecord(validMessage());

        assertThrows(NullPointerException.class,
                () -> tsigKey.verify(null, msgBytes, tsigRec, false),
                "Calling verify with a null Message should throw NullPointerException.");
    }

    @Test
    void test_verify_nullMessageBytes() {
        Message msg = validMessage();
        TSIGRecord tsigRec = validTSIGRecord(msg);

        assertThrows(NullPointerException.class,
                () -> tsigKey.verify(msg, null, tsigRec, false),
                "Calling verify with null messageBytes should throw NullPointerException.");
    }

    @Test
    void test_verify_nullTSIGRecord() {
        Message msg = validMessage();
        byte[] bytes = new byte[]{0x01, 0x02};

        assertThrows(NullPointerException.class,
                () -> tsigKey.verify(msg, bytes, null, true),
                "Calling verify with a null TSIGRecord should throw NullPointerException.");
    }

    @Test
    void test_verify_nullAlgorithmName() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        // Create a TSIGRecord whose algorithm field is null.
        TSIGRecord rec = tsigRecordWithAlgorithm(null);

        assertThrows(IllegalArgumentException.class,
                () -> tsigKey.verify(msg, wire, rec, false),
                "TSIGRecord with a null algorithm name should cause IllegalArgumentException.");
    }

    @Test
    void test_verify_unsupportedAlgorithm() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        // Use an algorithm name that is not mapped to any known TSIG algorithm.
        Name unknownAlg = Name.fromString("UNKNOWN-ALG.");
        TSIGRecord rec = tsigRecordWithAlgorithm(unknownAlg);

        assertThrows(IllegalArgumentException.class,
                () -> tsigKey.verify(msg, wire, rec, true),
                "Using an algorithm name that is not mapped to a known TSIG algorithm should throw IllegalArgumentException.");
    }

    @Test
    void test_verify_nullKey() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        // Simulate a TSIGRecord whose associated secret key is null.
        TSIGRecord rec = tsigRecordWithKey(null);

        assertThrows(IllegalArgumentException.class,
                () -> tsigKey.verify(msg, wire, rec, false),
                "TSIGRecord with a null secret key should cause IllegalArgumentException.");
    }

    @Test
    void test_verify_invalidKeyLength() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        // Empty key (zero‑length) – many HMAC implementations reject this when creating a SecretKeySpec.
        TSIGRecord rec = tsigRecordWithKey(new byte[]{});

        assertThrows(IllegalArgumentException.class,
                () -> tsigKey.verify(msg, wire, rec, true),
                "Providing an empty key should raise IllegalArgumentException when the SecretKeySpec is created.");
    }

    @Test
    void test_verify_macAlgorithmNotAvailable() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        // Use a bogus algorithm name that will be mapped to a non‑existent MAC algorithm.
        Name bogusAlg = Name.fromString("HMAC-UNSUPPORTED.");
        TSIGRecord rec = tsigRecordWithAlgorithm(bogusAlg);

        // The verification path should wrap the NoSuchAlgorithmException in a GeneralSecurityException.
        assertThrows(GeneralSecurityException.class,
                () -> tsigKey.verify(msg, wire, rec, false),
                "When the algorithm string maps to an unavailable MAC algorithm, a GeneralSecurityException should be thrown.");
    }

    @Test
    void test_verify_macNotInitialized() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        TSIGRecord rec = validTSIGRecord(msg);

        // Mock a Mac that throws IllegalStateException when doFinal is called.
        Mac mockMac = mock(Mac.class);
        // init() succeeds (do nothing)
        doNothing().when(mockMac).init(any());
        // doFinal throws IllegalStateException to simulate an un‑initialized MAC.
        when(mockMac.doFinal(any())).thenThrow(new IllegalStateException("MAC not initialized"));

        Method privateVerify = getPrivateVerifyMethod();

        // Invoke the private overload directly with the mocked Mac.
        assertThrows(IllegalStateException.class,
                () -> privateVerify.invoke(tsigKey, msg, wire, rec, true, mockMac),
                "If the MAC instance fails to be initialized, an IllegalStateException should be thrown.");
    }
}
```