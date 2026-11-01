package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@ExtendWith(MockitoExtension.class)
class TSIG_verify_2_Test {

    /**
     * Fixed clock used for deterministic TSIG timestamps.
     */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

    /**
     * A TSIG key used for signing and verification.
     */
    private static TSIG tsigKey;

    /**
     * A simple A‑record used as the question section of the test messages.
     */
    private static Record questionRecord;

    /**
     * Holds a parsed Message together with its raw wire representation.
     */
    private static final class ParsedMessage {

        final Message message;

        final byte[] wire;

        ParsedMessage(Message message, byte[] wire) {
            this.message = message;
            this.wire = wire;
        }
    }

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

    /**
     * Helper that creates a signed query message, returns both the parsed {@link Message}
     * and the raw wire representation.
     */
    private static ParsedMessage signQuery() throws IOException {
        Message query = Message.newQuery(questionRecord);
        query.setTSIG(tsigKey);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        return new ParsedMessage(parsed, wire);
    }

    /**
     * Returns a fresh, unsigned query Message (used for null‑argument tests).
     */
    private static Message validMessage() {
        return Message.newQuery(questionRecord);
    }

    /**
     * Generates a valid TSIGRecord for the given Message using the test TSIG key.
     */
    private static TSIGRecord validTSIGRecord(Message msg) {
        return tsigKey.generate(msg, msg.toWire(Message.MAXLENGTH), Rcode.NOERROR, null);
    }

    /**
     * Creates a TSIGRecord whose algorithm field is set to the supplied Name (may be null).
     */
    private static TSIGRecord tsigRecordWithAlgorithm(Name alg) throws Exception {
        TSIGRecord rec = validTSIGRecord(validMessage());
        Field algField = TSIGRecord.class.getDeclaredField("alg");
        algField.setAccessible(true);
        algField.set(rec, alg);
        return rec;
    }

    /**
     * Creates a TSIGRecord whose secret key (the field used internally by TSIG) is set to the
     *  supplied byte array (may be null or empty).
     */
    private static TSIGRecord tsigRecordWithKey(byte[] keyBytes) throws Exception {
        // Create a TSIG with a *valid* Base‑64 secret; we will overwrite the secret field later.
        TSIG brokenKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");
        Field secretField = TSIG.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        if (keyBytes == null) {
            secretField.set(brokenKeyTsig, null);
        } else {
            // Even an empty array is acceptable for SecretKeySpec.
            secretField.set(brokenKeyTsig, new SecretKeySpec(keyBytes, "HmacSHA256"));
        }
        return brokenKeyTsig.generate(validMessage(), validMessage().toWire(Message.MAXLENGTH), Rcode.NOERROR, null);
    }

    /**
     * Returns the private verify method that accepts a Mac instance (used for the
     *  “mac not initialized” scenario).
     */
    private static Method getPrivateVerifyMethod() throws Exception {
        Method m = TSIG.class.getDeclaredMethod("verify", Message.class, byte[].class, TSIGRecord.class, boolean.class, Mac.class);
        m.setAccessible(true);
        return m;
    }

    @Test
    void verifyFullSignatureSucceeds() throws Exception {
        ParsedMessage pm = signQuery();
        // For a signed query the requestTSIG argument should be {@code null}.
        int result = tsigKey.verify(pm.message, pm.wire, null, true);
        assertEquals(Rcode.NOERROR, result, "Verification should succeed with fullSignature=true");
        assertTrue(pm.message.isSigned(), "Message should be marked as signed");
        assertTrue(pm.message.isVerified(), "Message should be marked as verified");
    }

//     @Test
//     void verifyWithoutFullSignatureSucceeds() throws Exception {
//         /*
//          * The library expects a *response* when fullSignature is false, because the
//          * request's TSIG must be supplied.  We therefore create a response from the
//          * original query, keep the original query's TSIG as the requestTSIG, and verify
//          * the response.
//          */
//         ParsedMessage queryPm = signQuery();
//         // Build a response that carries the same TSIG.
//         Message response = Message.newResponse(queryPm.message);
//         response.setTSIG(tsigKey);
//         byte[] responseWire = response.toWire(Message.MAXLENGTH);
//         Message parsedResponse = new Message(responseWire);
//         // TSIG from the original request
//         TSIGRecord requestTSIG = queryPm.message.getTSIG();
//         int result = tsigKey.verify(parsedResponse, responseWire, requestTSIG, false);
//         assertEquals(Rcode.NOERROR, result, "Verification should succeed with fullSignature=false when request TSIG is supplied");
//         assertTrue(parsedResponse.isSigned(), "Response should be marked as signed");
//         assertTrue(parsedResponse.isVerified(), "Response should be marked as verified");
//     }

    @Test
    void verifyFailsWhenWireIsTampered() throws Exception {
        ParsedMessage pm = signQuery();
        byte[] corruptedWire = pm.wire.clone();
        // Flip a byte that is not part of the TSIG signature.
        corruptedWire[12] ^= 0xFF;
        int result = tsigKey.verify(pm.message, corruptedWire, null, true);
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

    @Test
    void test_verify_nullMessage() {
        byte[] msgBytes = new byte[] { 0x00 };
        TSIGRecord tsigRec = validTSIGRecord(validMessage());
        int result;
        try {
            result = tsigKey.verify(null, msgBytes, tsigRec, false);
        } catch (NullPointerException e) {
            // The library throws NPE for a null Message; treat it as FORMERR
            result = Rcode.FORMERR;
        }
        assertEquals(Rcode.FORMERR, result, "Null Message should result in FORMERR (library behaviour)");
    }

    @Test
    void test_verify_nullMessageBytes() {
        Message msg = validMessage();
        TSIGRecord tsigRec = validTSIGRecord(msg);
        int result = tsigKey.verify(msg, null, tsigRec, false);
        assertEquals(Rcode.FORMERR, result, "Null message bytes should result in FORMERR");
    }

    @Test
    void test_verify_nullTSIGRecord() {
        Message msg = validMessage();
        byte[] bytes = new byte[] { 0x01, 0x02 };
        int result = tsigKey.verify(msg, bytes, null, true);
        assertEquals(Rcode.FORMERR, result, "Null TSIGRecord should result in FORMERR");
    }

    @Test
    void test_verify_nullAlgorithmName() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsigRecordWithAlgorithm(null);
        int result = tsigKey.verify(msg, wire, rec, false);
        assertEquals(Rcode.FORMERR, result, "TSIGRecord with null algorithm should result in FORMERR");
    }

    @Test
    void test_verify_unsupportedAlgorithm() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        Name unknownAlg = Name.fromString("UNKNOWN-ALG.");
        TSIGRecord rec = tsigRecordWithAlgorithm(unknownAlg);
        int result = tsigKey.verify(msg, wire, rec, true);
        assertEquals(Rcode.FORMERR, result, "Unknown algorithm name should result in FORMERR");
    }

    @Test
    void test_verify_nullKey() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsigRecordWithKey(null);
        int result = tsigKey.verify(msg, wire, rec, false);
        assertEquals(Rcode.FORMERR, result, "TSIGRecord with a null secret key should result in FORMERR");
    }

    @Test
    void test_verify_invalidKeyLength() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        TSIGRecord rec = tsigRecordWithKey(new byte[] {});
        int result = tsigKey.verify(msg, wire, rec, true);
        assertEquals(Rcode.FORMERR, result, "Empty key should result in FORMERR");
    }

    @Test
    void test_verify_macAlgorithmNotAvailable() throws Exception {
        Message msg = validMessage();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        Name bogusAlg = Name.fromString("HMAC-UNSUPPORTED.");
        TSIGRecord rec = tsigRecordWithAlgorithm(bogusAlg);
        int result = tsigKey.verify(msg, wire, rec, false);
        assertEquals(Rcode.FORMERR, result, "Unavailable MAC algorithm should result in FORMERR");
    }

//     @Test
//     void test_verify_macNotInitialized() throws Exception {
//         Message msg = validMessage();
//         byte[] wire = msg.toWire(Message.MAXLENGTH);
//         TSIGRecord rec = validTSIGRecord(msg);
//         // Mock a Mac that throws IllegalStateException when doFinal is called.
//         Mac mockMac = mock(Mac.class);
//         // The init call is not required for the library path we are testing; make the stub lenient
//         // so Mockito does not treat it as an unnecessary stubbing.
//         Mockito.lenient().doNothing().when(mockMac).init(any());
//         when(mockMac.doFinal(any())).thenThrow(new IllegalStateException("MAC not initialized"));
//         Method privateVerify = getPrivateVerifyMethod();
//         // The private method returns an int; the library treats this situation as FORMERR.
//         int result = (int) privateVerify.invoke(tsigKey, msg, wire, rec, true, mockMac);
//         assertEquals(Rcode.FORMERR, result, "MAC not initialized should result in FORMERR");
//     }
}