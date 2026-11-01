```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;

import javax.crypto.Mac;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TSIG#verify(Message, byte[], TSIGRecord, boolean)}.
 *
 * <p>The test class lives in the same package as {@code TSIG} so that the package‑private
 * overload can be accessed directly.  Where the public API does not allow us to create a
 * particular {@link TSIGRecord} state (e.g. null MAC, unknown algorithm), reflection is used
 * to mutate the internal fields – this is acceptable for a white‑box test of a library class.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_verify_2_Test {

    /** Fixed clock to make timestamps deterministic. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /** A TSIG key with a known algorithm and secret – used for the “happy path”. */
    private static TSIG tsigKey;

    /** A second TSIG key with the same algorithm but a different secret – used for mismatched‑key tests. */
    private static TSIG tsigKeyOther;

    @BeforeAll
    static void setupBeforeAll() {
        // The secret is supplied as a base‑64 string – “12345678” is the same value used in the
        // upstream dnsjava test suite.
        tsigKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");
        tsigKeyOther = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "87654321");
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test mutable state required.
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
    /* Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    /** Build a simple DNS query with a TSIG attached using the supplied key. */
    private static Message buildSignedQuery(TSIG key) throws IOException {
        Record q = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message msg = Message.newQuery(q);
        msg.setTSIG(key); // this signs the message
        return msg;
    }

    /** Extract the TSIGRecord from a parsed message (may be null). */
    private static TSIGRecord getTSIGRecord(Message msg) {
        return msg.getTSIG();
    }

    /** Use reflection to replace a private field value. */
    private static void setPrivateField(Object target, String fieldName, Object newValue) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, newValue);
    }

    /** Return a copy of the wire‑format bytes for a message. */
    private static byte[] wireBytes(Message msg) throws IOException {
        return msg.toWire(Message.MAXLENGTH);
    }

    /** Create a TSIGRecord with a custom algorithm name (may be unknown). */
    private static TSIGRecord cloneWithAlgorithm(TSIGRecord original, Name newAlg) throws Exception {
        TSIGRecord clone = (TSIGRecord) original.clone();
        setPrivateField(clone, "algorithm", newAlg);
        return clone;
    }

    /** Create a TSIGRecord with a custom MAC (may be null or zero‑length). */
    private static TSIGRecord cloneWithMac(TSIGRecord original, byte[] mac) throws Exception {
        TSIGRecord clone = (TSIGRecord) original.clone();
        setPrivateField(clone, "mac", mac);
        return clone;
    }

    /** Create a TSIGRecord with a custom timestamp (seconds since epoch). */
    private static TSIGRecord cloneWithTime(TSIGRecord original, long epochSeconds) throws Exception {
        TSIGRecord clone = (TSIGRecord) original.clone();
        setPrivateField(clone, "timeSigned", epochSeconds);
        return clone;
    }

    /** Create a TSIGRecord with a null algorithm name. */
    private static TSIGRecord cloneWithNullAlgorithm(TSIGRecord original) throws Exception {
        TSIGRecord clone = (TSIGRecord) original.clone();
        setPrivateField(clone, "algorithm", null);
        return clone;
    }

    /** Create a TSIGRecord with a null MAC field. */
    private static TSIGRecord cloneWithNullMac(TSIGRecord original) throws Exception {
        TSIGRecord clone = (TSIGRecord) original.clone();
        setPrivateField(clone, "mac", null);
        return clone;
    }

    /** Create a TSIG instance whose secret key field is null (to provoke a cryptographic error). */
    private static TSIG createKeyWithNullSecret(Name name) throws Exception {
        TSIG broken = new TSIG(TSIG.HMAC_SHA256, name, "12345678");
        // The underlying secret is stored in a private byte[] field called “key”.
        setPrivateField(broken, "key", null);
        return broken;
    }

    /* ---------------------------------------------------------------------- */
    /* Happy‑path tests                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("Successful verification (full signature)")
    void verify_successfulFullSignature() throws IOException {
        Message query = buildSignedQuery(tsigKey);
        byte[] wire = wireBytes(query);
        Message parsed = new Message(wire);
        TSIGRecord tsigRec = getTSIGRecord(parsed);

        int result = tsigKey.verify(parsed, wire, tsigRec, true);
        assertEquals(Rcode.NOERROR, result);
        assertTrue(parsed.isSigned());
        assertTrue(parsed.isVerified());
    }

    @Test
    @DisplayName("Successful verification (partial signature – fullSignature = false)")
    void verify_successfulPartialSignature() throws IOException {
        Message query = buildSignedQuery(tsigKey);
        byte[] wire = wireBytes(query);
        Message parsed = new Message(wire);
        TSIGRecord tsigRec = getTSIGRecord(parsed);

        int result = tsigKey.verify(parsed, wire, tsigRec, false);
        assertEquals(Rcode.NOERROR, result);
    }

    /* ---------------------------------------------------------------------- */
    /* Parameterised negative‑scenario tests                                 */
    /* ---------------------------------------------------------------------- */

    /** Arguments: description, Message, wire bytes, TSIGRecord (may be null), fullSignature, expected Rcode */
    private static Stream<Arguments> negativeScenarios() throws Exception {
        // Base signed message used for many scenarios
        Message baseMsg = buildSignedQuery(tsigKey);
        byte[] baseWire = wireBytes(baseMsg);
        Message baseParsed = new Message(baseWire);
        TSIGRecord baseRec = getTSIGRecord(baseParsed);

        // 1. Signature mismatch – tamper with MAC
        TSIGRecord macTampered = cloneWithMac(baseRec, new byte[]{0x00, 0x01, 0x02});
        // 2. Bad time – timestamp older than now‑FUDGE‑1
        long oldTime = Instant.now(FIXED_CLOCK).minus(TSIG.FUDGE).minusSeconds(1).getEpochSecond();
        TSIGRecord oldTimeRec = cloneWithTime(baseRec, oldTime);
        // 3. Bad time – timestamp newer than now+FUDGE+1
        long futureTime = Instant.now(FIXED_CLOCK).plus(TSIG.FUDGE).plusSeconds(1).getEpochSecond();
        TSIGRecord futureTimeRec = cloneWithTime(baseRec, futureTime);
        // 4. Unsupported algorithm
        TSIGRecord unknownAlgRec = cloneWithAlgorithm(baseRec, Name.fromString("unknown-alg."));
        // 5. Empty raw message bytes
        byte[] emptyBytes = new byte[0];
        // 6. Zero‑length MAC
        TSIGRecord zeroMacRec = cloneWithMac(baseRec, new byte[0]);
        // 7. Unsupported algorithm – MD5 (valid case)
        Message md5Msg = buildSignedQuery(new TSIG(TSIG.HMAC_MD5, Name.fromString("example."), "12345678"));
        byte[] md5Wire = wireBytes(md5Msg);
        Message md5Parsed = new Message(md5Wire);
        TSIGRecord md5Rec = getTSIGRecord(md5Parsed);
        // 8. Mismatched secret key – sign with key A, verify with key B
        Message otherKeyMsg = buildSignedQuery(tsigKeyOther);
        byte[] otherWire = wireBytes(otherKeyMsg);
        Message otherParsed = new Message(otherWire);
        TSIGRecord otherRec = getTSIGRecord(otherParsed);
        // 9. Message without TSIG but requestTSIG supplied
        Message noTsigMsg = Message.newQuery(Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN));
        byte[] noTsigWire = wireBytes(noTsigMsg);
        // 10. Null algorithm name in TSIGRecord
        TSIGRecord nullAlgRec = cloneWithNullAlgorithm(baseRec);
        // 11. Null MAC field
        TSIGRecord nullMacRec = cloneWithNullMac(baseRec);
        // 12. TSIG with empty secret key (InvalidKeyException)
        TSIG emptyKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), ""); // empty base64 -> empty byte[]
        // 13. Null secret key (via reflection)
        TSIG nullSecretKey = createKeyWithNullSecret(Name.fromString("example."));
        // 14. Null Message object
        // 15. Null requestTSIG (already covered by existing test, but we include here for completeness)

        return Stream.of(
                // description,          Message,          wire,          requestTSIG,          fullSig, expected
                Arguments.of("Signature mismatch (bad MAC)", baseParsed, baseWire, macTampered, true, Rcode.BADSIG),
                Arguments.of("Bad time – older than now‑FUDGE", baseParsed, baseWire, oldTimeRec, true, Rcode.BADTIME),
                Arguments.of("Bad time – newer than now+FUDGE", baseParsed, baseWire, futureTimeRec, true, Rcode.BADTIME),
                Arguments.of("Unsupported algorithm", baseParsed, baseWire, unknownAlgRec, true, Rcode.FORMERR),
                Arguments.of("Null messageBytes", baseParsed, null, baseRec, true, Rcode.FORMERR),
                Arguments.of("Empty raw message bytes", baseParsed, emptyBytes, baseRec, false, Rcode.FORMERR),
                Arguments.of("Zero‑length MAC", baseParsed, baseWire, zeroMacRec, true, Rcode.BADSIG),
                Arguments.of("Supported algorithm HMAC‑MD5", md5Parsed, md5Wire, md5Rec, true, Rcode.NOERROR),
                Arguments.of("Mismatched secret key", otherParsed, otherWire, otherRec, true, Rcode.BADSIG),
                Arguments.of("Message without TSIG but requestTSIG supplied", noTsigMsg, noTsigWire, baseRec, true, Rcode.FORMERR),
                Arguments.of("Null algorithm name in TSIGRecord", baseParsed, baseWire, nullAlgRec, true, Rcode.FORMERR),
                Arguments.of("Null MAC field in TSIGRecord", baseParsed, baseWire, nullMacRec, true, Rcode.FORMERR),
                Arguments.of("Empty secret key (InvalidKeyException)", baseParsed, baseWire, baseRec, true, Rcode.FORMERR, emptyKey),
                Arguments.of("Null secret key (cryptographic error)", baseParsed, baseWire, baseRec, true, Rcode.FORMERR, nullSecretKey),
                Arguments.of("Null Message object", null, baseWire, baseRec, true, Rcode.FORMERR)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("negativeScenarios")
    void verify_negativeScenarios(String description,
                                  Message m,
                                  byte[] messageBytes,
                                  TSIGRecord requestTSIG,
                                  boolean fullSignature,
                                  int expectedRcode) throws IOException {
        // Choose the appropriate TSIG instance for the test:
        // - For the two cases that need a special TSIG (empty key / null secret), we use the provided instance.
        // - Otherwise we use the default tsigKey.
        TSIG verifier = (description.contains("Empty secret key") || description.contains("Null secret key"))
                ? (description.contains("Empty secret key") ? new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "") : createKeyWithNullSecret(Name.fromString("example.")))
                : tsigKey;

        // Guard against a null Message – the method should return FORMERR without throwing NPE.
        if (m == null) {
            int rc = verifier.verify(m, messageBytes, requestTSIG, fullSignature);
            assertEquals(expectedRcode, rc, description);
            return;
        }

        int rc = verifier.verify(m, messageBytes, requestTSIG, fullSignature);
        assertEquals(expectedRcode, rc, description);
    }

    /* ---------------------------------------------------------------------- */
    /* Additional explicit tests (not covered by the parameterised set)     */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("Null requestTSIG should return FORMERR")
    void verify_nullRequestTSIG_returnsFormErr() throws IOException {
        Message query = buildSignedQuery(tsigKey);
        byte[] wire = wireBytes(query);
        Message parsed = new Message(wire);
        int rc = tsigKey.verify(parsed, wire, null, true);
        assertEquals(Rcode.FORMERR, rc);
    }

    @Test
    @DisplayName("Exact fudge boundary – lower limit (now‑FUDGE)")
    void verify_exactFudgeLowerBoundary() throws IOException, Exception {
        Message query = buildSignedQuery(tsigKey);
        byte[] wire = wireBytes(query);
        Message parsed = new Message(wire);
        TSIGRecord rec = getTSIGRecord(parsed);

        long lower = Instant.now(FIXED_CLOCK).minus(TSIG.FUDGE).getEpochSecond();
        TSIGRecord adjusted = cloneWithTime(rec, lower);

        int rc = tsigKey.verify(parsed, wire, adjusted, true);
        assertEquals(Rcode.NOERROR, rc);
    }

    @Test
    @DisplayName("Exact fudge boundary – upper limit (now+FUDGE)")
    void verify_exactFudgeUpperBoundary() throws IOException, Exception {
        Message query = buildSignedQuery(tsigKey);
        byte[] wire = wireBytes(query);
        Message parsed = new Message(wire);
        TSIGRecord rec = getTSIGRecord(parsed);

        long upper = Instant.now(FIXED_CLOCK).plus(TSIG.FUDGE).getEpochSecond();
        TSIGRecord adjusted = cloneWithTime(rec, upper);

        int rc = tsigKey.verify(parsed, wire, adjusted, true);
        assertEquals(Rcode.NOERROR, rc);
    }

    @Test
    @DisplayName("Message bytes length of one (insufficient data) should return FORMERR")
    void verify_oneByteMessageFails() throws IOException {
        Message query = buildSignedQuery(tsigKey);
        byte[] oneByte = new byte[1];
        int rc = tsigKey.verify(query, oneByte, getTSIGRecord(query), true);
        assertEquals(Rcode.FORMERR, rc);
    }
}
```