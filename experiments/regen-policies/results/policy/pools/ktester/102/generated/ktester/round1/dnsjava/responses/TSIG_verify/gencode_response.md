```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Extended tests for {@link TSIG#verify(Message, byte[], TSIGRecord)}.
 *
 * <p>The test suite covers the exhaustive JSON test matrix supplied in the
 * specification.  Helper methods are used to build mock {@link Message}
 * instances, raw wire‑format byte arrays and {@link TSIGRecord}s with
 * particular properties (algorithm, timestamp, signature, etc.).
 *
 * <p>All tests run with a deterministic {@link Clock} so that timestamps are
 * reproducible.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_verify_Test {

    /** Fixed clock used for deterministic TSIG timestamps. */
    private static Clock fixedClock;

    /** The TSIG key used for signing and verification (HMAC‑SHA256). */
    private static TSIG tsigKey;

    /** Simple secret key bytes (not cryptographically strong – only for testing). */
    private static final byte[] KEY_BYTES = "12345678".getBytes();

    /** A signed query message – created in {@link #setupBeforeEach()}. */
    private Message queryMessage;

    /** Wire format of the signed query – passed to {@code verify}. */
    private byte[] queryWire;

    /** The TSIG record generated on {@code queryMessage}. */
    private TSIGRecord queryTSIG;

    @BeforeAll
    static void setupBeforeAll() {
        // deterministic instant – 2023‑01‑01T00:00:00Z
        fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

        SecretKey secretKey = new SecretKeySpec(KEY_BYTES, "HmacSHA256");
        tsigKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example.", Name.root), secretKey, fixedClock);
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // simple A‑record query
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        queryMessage.setTSIG(tsigKey);

        // serialise – this also generates the TSIG signature
        queryWire = queryMessage.toWire(Message.MAXLENGTH);

        // parse back to obtain a fresh Message that contains the TSIGRecord
        Message parsed = new Message(queryWire);
        queryTSIG = parsed.getTSIG();
        assertNotNull(queryTSIG, "TSIGRecord should be present after signing");
    }

    /* --------------------------------------------------------------------- *
     *  Existing happy‑path tests (kept unchanged)                           *
     * --------------------------------------------------------------------- */

    @Test
    void verifyValidSignature() throws IOException {
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, queryTSIG);
        assertEquals(Rcode.NOERROR, result);
        assertTrue(received.isSigned());
        assertTrue(received.isVerified());
    }

    @Test
    void verifyTamperedMessageFails() throws IOException {
        byte[] tampered = queryWire.clone();
        tampered[12] = (byte) (tampered[12] ^ 0xFF); // corrupt QNAME byte
        Message received = new Message(tampered);
        int result = tsigKey.verify(received, tampered, queryTSIG);
        assertNotEquals(Rcode.NOERROR, result);
    }

    @Test
    void verifyWithWrongKeyFails() throws IOException {
        SecretKey wrongKey = new SecretKeySpec("different".getBytes(), "HmacSHA256");
        TSIG otherTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example.", Name.root), wrongKey, fixedClock);
        Message received = new Message(queryWire);
        int result = otherTsig.verify(received, queryWire, queryTSIG);
        assertNotEquals(Rcode.NOERROR, result);
    }

    @Test
    void verifyExpiredTimestampFails() throws IOException {
        Instant oldInstant = fixedClock.instant().minus(Duration.ofHours(1));
        TSIGRecord oldTsigRecord = new TSIGRecord(
                queryTSIG.getName(),
                queryTSIG.getDClass(),
                queryTSIG.getTTL(),
                queryTSIG.getAlgorithm(),
                oldInstant,
                TSIG.FUDGE,
                queryTSIG.getSignature(),
                queryTSIG.getOriginalID(),
                queryTSIG.getError(),
                queryTSIG.getOther()
        );
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, oldTsigRecord);
        assertNotEquals(Rcode.NOERROR, result);
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods for building test fixtures                             *
     * --------------------------------------------------------------------- */

    /** Returns a simple mock {@link Message}. */
    private static Message mockMessage() {
        return mock(Message.class);
    }

    /** Returns a {@link Message} that throws {@link IOException} when {@code toWire} is called. */
    private static Message mockMessageThrowsIOException() throws IOException {
        Message m = mock(Message.class);
        when(m.toWire(anyInt())).thenThrow(new IOException("forced"));
        return m;
    }

    /** Generates a minimal valid wire‑format byte array (a simple query). */
    private static byte[] validMessageBytes() throws IOException {
        Record q = Record.newRecord(Name.fromString("example."), Type.A, DClass.IN);
        Message msg = Message.newQuery(q);
        return msg.toWire(Message.MAXLENGTH);
    }

    /** Builds a {@link TSIGRecord} with a *valid* signature using the current {@code tsigKey}. */
    private static TSIGRecord tsigRecordWithValidSignature() throws IOException {
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("example."), Type.A, DClass.IN));
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        // generate a TSIGRecord (full signature) – this uses the same key as tsigKey
        return tsigKey.generate(msg, wire, Rcode.NOERROR, null, true);
    }

    /** Creates a copy of a valid record but with a corrupted signature. */
    private static TSIGRecord tsigRecordWithInvalidSignature() throws IOException {
        TSIGRecord good = tsigRecordWithValidSignature();
        byte[] badSig = good.getSignature().clone();
        // flip first byte to make it invalid
        badSig[0] = (byte) (badSig[0] ^ 0xFF);
        return new TSIGRecord(
                good.getName(),
                good.getDClass(),
                good.getTTL(),
                good.getAlgorithm(),
                good.getTimeSigned(),
                good.getFudge(),
                badSig,
                good.getOriginalID(),
                good.getError(),
                good.getOther()
        );
    }

    /** Returns a TSIGRecord whose algorithm name is the supplied string. */
    private static TSIGRecord tsigRecordWithAlgorithm(String alg) throws IOException {
        // use a valid signature (generated with the correct key) – the algorithm is swapped afterwards
        TSIGRecord base = tsigRecordWithValidSignature();
        Name algName = Name.fromString(alg);
        return new TSIGRecord(
                base.getName(),
                base.getDClass(),
                base.getTTL(),
                algName,
                base.getTimeSigned(),
                base.getFudge(),
                base.getSignature(),
                base.getOriginalID(),
                base.getError(),
                base.getOther()
        );
    }

    /** Returns a TSIGRecord with a timestamp that is {@code instant}. */
    private static TSIGRecord tsigRecordWithTime(Instant instant) throws IOException {
        TSIGRecord base = tsigRecordWithValidSignature();
        return new TSIGRecord(
                base.getName(),
                base.getDClass(),
                base.getTTL(),
                base.getAlgorithm(),
                instant,
                base.getFudge(),
                base.getSignature(),
                base.getOriginalID(),
                base.getError(),
                base.getOther()
        );
    }

    /** Returns a TSIGRecord whose signature is truncated to {@code length} bytes. */
    private static TSIGRecord tsigRecordWithTruncatedSignature(int length) throws IOException {
        TSIGRecord base = tsigRecordWithValidSignature();
        byte[] sig = base.getSignature();
        byte[] truncated = new byte[length];
        System.arraycopy(sig, 0, truncated, 0, Math.min(length, sig.length));
        return new TSIGRecord(
                base.getName(),
                base.getDClass(),
                base.getTTL(),
                base.getAlgorithm(),
                base.getTimeSigned(),
                base.getFudge(),
                truncated,
                base.getOriginalID(),
                base.getError(),
                base.getOther()
        );
    }

    /** Returns a TSIGRecord with a {@code null} algorithm field. */
    private static TSIGRecord tsigRecordWithNullAlgorithm() throws IOException {
        TSIGRecord base = tsigRecordWithValidSignature();
        return new TSIGRecord(
                base.getName(),
                base.getDClass(),
                base.getTTL(),
                null, // algorithm
                base.getTimeSigned(),
                base.getFudge(),
                base.getSignature(),
                base.getOriginalID(),
                base.getError(),
                base.getOther()
        );
    }

    /** Returns a TSIGRecord with a {@code null} key name. */
    private static TSIGRecord tsigRecordWithNullKeyName() throws IOException {
        TSIGRecord base = tsigRecordWithValidSignature();
        return new TSIGRecord(
                null, // key name
                base.getDClass(),
                base.getTTL(),
                base.getAlgorithm(),
                base.getTimeSigned(),
                base.getFudge(),
                base.getSignature(),
                base.getOriginalID(),
                base.getError(),
                base.getOther()
        );
    }

    /** Returns a TSIGRecord with a {@code null} signature. */
    private static TSIGRecord tsigRecordWithNullSignature() throws IOException {
        TSIGRecord base = tsigRecordWithValidSignature();
        return new TSIGRecord(
                base.getName(),
                base.getDClass(),
                base.getTTL(),
                base.getAlgorithm(),
                base.getTimeSigned(),
                base.getFudge(),
                null, // signature
                base.getOriginalID(),
                base.getError(),
                base.getOther()
        );
    }

    /** Returns a TSIGRecord with an empty signature array. */
    private static TSIGRecord tsigRecordWithEmptySignature() throws IOException {
        TSIGRecord base = tsigRecordWithValidSignature();
        return new TSIGRecord(
                base.getName(),
                base.getDClass(),
                base.getTTL(),
                base.getAlgorithm(),
                base.getTimeSigned(),
                base.getFudge(),
                new byte[0],
                base.getOriginalID(),
                base.getError(),
                base.getOther()
        );
    }

    /** Returns a TSIGRecord with a zero‑length key (i.e., the TSIG object is constructed with an empty secret). */
    private static TSIG tsigWithZeroLengthKey() {
        SecretKey emptyKey = new SecretKeySpec(new byte[0], "HmacSHA256");
        return new TSIG(TSIG.HMAC_SHA256, Name.fromString("example.", Name.root), emptyKey, fixedClock);
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests for the JSON matrix                               *
     * --------------------------------------------------------------------- */

    /* ---------- Null and Invalid Input Handling ---------- */
    private static Stream<Arguments> nullInputProvider() {
        return Stream.of(
                Arguments.of(null, null, null, "All arguments null"),
                Arguments.of(mockMessage(), null, tsigRecordWithValidSignature(), "messageBytes null"),
                Arguments.of(mockMessage(), new byte[0], null, "requestTSIG null")
        );
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullInputProvider")
    void verifyNullAndInvalidInputs(Message m, byte[] bytes, TSIGRecord tsigRec, String _desc) {
        assertThrows(NullPointerException.class,
                () -> tsigKey.verify(m, bytes, tsigRec),
                "Method should throw NPE for null arguments");
    }

    /* ---------- Algorithm Name Validation ---------- */
    @Test
    void verifyUnknownAlgorithmReturnsBadKey() throws IOException {
        TSIGRecord unknownAlg = tsigRecordWithAlgorithm("UNKNOWN-ALG.");
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, unknownAlg);
        assertEquals(Rcode.BADKEY, result, "Unknown algorithm must yield BADKEY");
    }

    @Test
    void verifySupportedAlgorithmProceeds() throws IOException {
        // use the algorithm that tsigKey was constructed with (HMAC‑SHA256)
        TSIGRecord goodAlg = tsigRecordWithAlgorithm(TSIG.HMAC_SHA256.toString());
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, goodAlg);
        assertNotEquals(Rcode.BADKEY, result, "Supported algorithm should not return BADKEY");
    }

    /* ---------- Time Fudge Checks ---------- */
    @Test
    void verifyTimestampBeyondFudgeReturnsBadTime() throws IOException {
        Instant future = fixedClock.instant().plusSeconds(TSIG.FUDGE.getSeconds() + 1);
        TSIGRecord futureTsig = tsigRecordWithTime(future);
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, futureTsig);
        assertEquals(Rcode.BADTIME, result, "Timestamp beyond fudge must yield BADTIME");
    }

    @Test
    void verifyTimestampWithinFudgeContinuesVerification() throws IOException {
        Instant past = fixedClock.instant().minusSeconds(TSIG.FUDGE.getSeconds() - 10);
        TSIGRecord pastTsig = tsigRecordWithTime(past);
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, pastTsig);
        assertNotEquals(Rcode.BADTIME, result, "Timestamp inside fudge should not return BADTIME");
    }

    /* ---------- Signature Verification ---------- */
    @Test
    void verifyValidSignatureReturnsNoError() throws IOException {
        TSIGRecord good = tsigRecordWithValidSignature();
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, good);
        assertEquals(Rcode.NOERROR, result, "Valid signature must verify with NOERROR");
    }

    @Test
    void verifyInvalidSignatureReturnsBadSig() throws IOException {
        TSIGRecord bad = tsigRecordWithInvalidSignature();
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, bad);
        assertEquals(Rcode.BADSIG, result, "Invalid signature must yield BADSIG");
    }

    /* ---------- Full Signature Flag Influence ---------- */
    @Test
    void verifyFullSignatureTrueSucceeds() throws IOException {
        TSIGRecord good = tsigRecordWithValidSignature();
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, good, true);
        assertEquals(Rcode.NOERROR, result);
    }

    @Test
    void verifyFullSignatureFalseWithTruncatedMacFails() throws IOException {
        // truncate to 10 bytes – this mimics the "short MAC" case
        TSIGRecord truncated = tsigRecordWithTruncatedSignature(10);
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, truncated, false);
        assertEquals(Rcode.BADSIG, result, "Truncated MAC with fullSignature=false must yield BADSIG");
    }

    /* ---------- Error Propagation from MAC Initialization ---------- */
    @Test
    void verifyUnsupportedAlgorithmReturnsBadKey() throws IOException {
        // Use a deliberately unsupported algorithm name
        TSIGRecord unsupported = tsigRecordWithAlgorithm("HMAC-UNSUPPORTED.");
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, unsupported);
        assertEquals(Rcode.BADKEY, result, "Unsupported algorithm should map to BADKEY");
    }

    /* ---------- Null Field Inside TSIGRecord ---------- */
    @Test
    void verifyNullAlgorithmInRecordReturnsBadKey() throws IOException {
        TSIGRecord rec = tsigRecordWithNullAlgorithm();
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, rec);
        assertEquals(Rcode.BADKEY, result);
    }

    @Test
    void verifyNullKeyNameInRecordReturnsBadKey() throws IOException {
        TSIGRecord rec = tsigRecordWithNullKeyName();
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, rec);
        assertEquals(Rcode.BADKEY, result);
    }

    @Test
    void verifyNullSignatureInRecordReturnsBadSig() throws IOException {
        TSIGRecord rec = tsigRecordWithNullSignature();
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, rec);
        assertEquals(Rcode.BADSIG, result);
    }

    @Test
    void verifyEmptySignatureInRecordReturnsBadSig() throws IOException {
        TSIGRecord rec = tsigRecordWithEmptySignature();
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, queryWire, rec);
        assertEquals(Rcode.BADSIG, result);
    }

    /* ---------- Invalid Key Data ---------- */
    @Test
    void verifyZeroLengthKeyReturnsBadKey() throws IOException {
        TSIG zeroKeyTsig = tsigWithZeroLengthKey();
        // generate a TSIGRecord using the *valid* tsigKey (so we have a proper signature)
        TSIGRecord good = tsigRecordWithValidSignature();
        Message received = new Message(queryWire);
        int result = zeroKeyTsig.verify(received, queryWire, good);
        assertEquals(Rcode.BADKEY, result, "Zero‑length secret should cause BADKEY");
    }

    /* ---------- Empty Message Bytes ---------- */
    @Test
    void verifyEmptyMessageBytesReturnsBadSig() throws IOException {
        TSIGRecord good = tsigRecordWithValidSignature();
        Message received = new Message(queryWire);
        int result = tsigKey.verify(received, new byte[0], good);
        assertEquals(Rcode.BADSIG, result, "Empty wire array should not match a non‑empty signature");
    }

    /* ---------- Message Serialization Failure ---------- */
    @Test
    void verifyMessageToWireIOExceptionReturnsBadSig() throws IOException {
        Message badMsg = mockMessageThrowsIOException();
        TSIGRecord good = tsigRecordWithValidSignature();
        // Even though we pass a valid byte[] (the original wire), the implementation may
        // attempt to re‑serialize the Message and encounter the forced IOException.
        int result = tsigKey.verify(badMsg, queryWire, good);
        assertEquals(Rcode.BADSIG, result, "IOException during Message serialization should yield BADSIG");
    }
}
```