package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class Message_setTSIG_2_Test {

    private Message message;

    private TSIG tsigKey;

    private TSIGRecord tsigRecord;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a fresh Message for each test.
        message = new Message();
        // Build a TSIG key – algorithm “hmac‑md5.sig‑alg.reg.int.” is the default algorithm used in many examples.
        Name alg = Name.fromString("hmac-md5.sig-alg.reg.int.");
        Name name = Name.fromString("example.com.");
        // The key string is arbitrary; the library decodes it from base64.
        // “abcdefghijklmnop” in base64
        tsigKey = new TSIG(alg, name, "YWJjZGVmZ2hpamtsbW5vcA==");
        // Build a TSIGRecord that could be attached to a query.
        tsigRecord = new TSIGRecord(// owner name
        name, // class
        DClass.IN, // ttl (ignored for TSIG)
        0L, // algorithm name
        alg, // time signed
        Instant.now(), // fudge
        Duration.ofSeconds(300), // MAC (dummy)
        new byte[] { 0x01, 0x02 }, // original ID
        new byte[] { 0x01, 0x02 }, // error code
        Rcode.NOERROR, // other data
        new byte[0]);
    }

    /**
     * Reads the private {@code tsigerror} field via reflection.
     */
    private static int getTsigError(Message m) throws Exception {
        Field f = Message.class.getDeclaredField("tsigerror");
        f.setAccessible(true);
        return (int) f.get(m);
    }

    /**
     * Creates a TSIG instance from an algorithm string and a raw secret byte array.
     */
    private static TSIG createTSIG(String algorithm, byte[] secret) throws Exception {
        Name algName = Name.fromString(algorithm.endsWith(".") ? algorithm : algorithm + ".");
        Name owner = Name.fromString("keyowner.example.");
        String b64 = Base64.getEncoder().encodeToString(secret);
        return new TSIG(algName, owner, b64);
    }

    /**
     * Creates a TSIGRecord with the supplied parameters.
     */
    private static TSIGRecord createTSIGRecord(Name owner, int dclass, long ttl, TSIG key) {
        try {
            Name alg = getAlgorithmFromTSIG(key);
            byte[] mac = new byte[] { 0x0A, 0x0B };
            byte[] originalID = new byte[] { 0x0A, 0x0B };
            return new TSIGRecord(owner, dclass, ttl, alg, Instant.now(), Duration.ofSeconds(300), mac, originalID, Rcode.NOERROR, new byte[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* ---------- basic field assignment ---------- */
    private static Stream<Arguments> basicFieldAssignmentProvider() throws Exception {
        return Stream.of(// Standard usage with non‑null key and query TSIG, error 0
        Arguments.of(createTSIG("hmac-md5", "secret".getBytes()), 0, createTSIGRecord(Name.fromString("example.com."), DClass.IN, 300, new TSIG(Name.fromString("hmac-md5."), Name.fromString("example.com."), "c2VjcmV0")), true, true, 0), // Both key and query TSIG null, error 0
        Arguments.of(null, 0, null, false, false, 0), // Negative error code, non‑null key, null query TSIG
        Arguments.of(createTSIG("hmac-sha256", "key".getBytes()), -1, null, true, false, -1), // Null key, large positive error, non‑null query TSIG
        Arguments.of(null, 65535, createTSIGRecord(Name.fromString("example.org."), DClass.IN, 600, new TSIG(Name.fromString("hmac-sha1."), Name.fromString("example.org."), "YW5vdGhlcg==")), false, true, 65535));
    }

    /* ---------- extreme error codes ---------- */
    private static Stream<Arguments> extremeErrorCodesProvider() {
        return Stream.of(// Integer.MAX_VALUE with non‑null key
        Arguments.of(createTSIG("hmac-sha512", "bigkey".getBytes()), Integer.MAX_VALUE, null, true, false, Integer.MAX_VALUE), // Integer.MIN_VALUE with null key
        Arguments.of(null, Integer.MIN_VALUE, null, false, false, Integer.MIN_VALUE));
    }

    /* ---------- empty algorithm and secret handling ---------- */
    private static Stream<Arguments> emptyAlgorithmAndSecretProvider() throws Exception {
        return Stream.of(// Empty algorithm string and empty secret
        Arguments.of(createTSIG("", new byte[0]), 0, null, true, false, 0), // Non‑empty algorithm, empty secret, with a TSIGRecord containing the same empty secret
        Arguments.of(createTSIG("hmac-md5", new byte[0]), 0, createTSIGRecord(Name.fromString("emptysecret.example."), DClass.IN, 300, createTSIG("hmac-md5", new byte[0])), true, true, 0));
    }

    /* ---------- zero and maximum TTL in query TSIG ---------- */
    private static Stream<Arguments> ttlEdgeCasesProvider() throws Exception {
        return Stream.of(// TTL zero
        Arguments.of(createTSIG("hmac-sha256", "key".getBytes()), 0, createTSIGRecord(Name.fromString("zero.ttl.example."), DClass.IN, 0, createTSIG("hmac-sha256", "key".getBytes())), true, true, 0), // TTL Integer.MAX_VALUE
        Arguments.of(createTSIG("hmac-sha256", "key".getBytes()), 0, createTSIGRecord(Name.fromString("max.ttl.example."), DClass.IN, Integer.MAX_VALUE, createTSIG("hmac-sha256", "key".getBytes())), true, true, 0));
    }

    /**
     * Reads the private {@code tsigkey} field via reflection.
     */
    private static TSIG getMessageTSIGKey(Message m) throws Exception {
        Field f = Message.class.getDeclaredField("tsigkey");
        f.setAccessible(true);
        return (TSIG) f.get(m);
    }

    /**
     * Reads the private {@code algorithm} field from a {@link TSIG}.
     */
    private static Name getAlgorithmFromTSIG(TSIG key) throws Exception {
        Field f = TSIG.class.getDeclaredField("algorithm");
        f.setAccessible(true);
        return (Name) f.get(key);
    }

    /**
     * Verify that the three‑argument {@code setTSIG} stores the supplied
     * TSIG key, error code and TSIGRecord.  The public {@link Message#getTSIG()}
     * method should return the TSIGRecord that was passed.
     */
    @Test
    void setTSIG_threeArguments_storesValues() {
        // Act
        message.setTSIG(tsigKey, Rcode.BADKEY, tsigRecord);
        // Assert – the TSIGRecord we supplied must be returned by getTSIG()
        assertSame(tsigRecord, message.getTSIG(), "Message#getTSIG() should return the TSIGRecord supplied to setTSIG");
    }

    /**
     * Verify the overload {@code setTSIG(TSIG)} delegates to the three‑argument
     * version with {@code Rcode.NOERROR} and a {@code null} TSIGRecord.
     */
    @Test
    void setTSIG_oneArgument_delegatesCorrectly() {
        // Act – use the overload that only supplies the key
        message.setTSIG(tsigKey);
        // Assert – because the overload passes a null TSIGRecord, getTSIG()
        // must return null.
        assertNull(message.getTSIG(), "Message#getTSIG() should be null when setTSIG(TSIG) is used without a TSIGRecord");
    }

    /**
     * Ensure that calling {@code setTSIG} twice updates the stored TSIGRecord.
     */
    @Test
    void setTSIG_calledMultipleTimes_updatesRecord() throws Exception {
        // First call with a record
        message.setTSIG(tsigKey, Rcode.NOERROR, tsigRecord);
        assertSame(tsigRecord, message.getTSIG(), "First call should store the initial TSIGRecord");
        // Create a second, different TSIGRecord
        TSIGRecord secondRecord = new TSIGRecord(Name.fromString("other.example.com."), DClass.IN, 0L, Name.fromString("hmac-sha256."), Instant.now(), Duration.ofSeconds(300), new byte[] { 0x0A, 0x0B }, new byte[] { 0x56, 0x78 }, Rcode.NOERROR, new byte[0]);
        // Second call replaces the previous record
        message.setTSIG(tsigKey, Rcode.NOERROR, secondRecord);
        assertSame(secondRecord, message.getTSIG(), "Second call should replace the previously stored TSIGRecord");
    }

    /**
     * Verify that passing {@code null} for the TSIGRecord does not cause a
     * {@link NullPointerException} and that {@code getTSIG()} returns {@code null}.
     */
    @Test
    void setTSIG_nullRecord_returnsNullTSIG() {
        // Act – explicitly pass a null TSIGRecord
        message.setTSIG(tsigKey, Rcode.NOERROR, null);
        // Assert – the public getter must reflect the null value.
        assertNull(message.getTSIG(), "Message#getTSIG() should be null when setTSIG is called with a null TSIGRecord");
    }

    @ParameterizedTest(name = "[{index}] key={0 != null}, error={1}, queryTSIG={2 != null}")
    @MethodSource("basicFieldAssignmentProvider")
    void setTSIG_basicFieldAssignment(TSIG key, int error, TSIGRecord query, boolean expectKey, boolean expectRecord, int expectedError) throws Exception {
        message.setTSIG(key, error, query);
        assertEquals(query, message.getTSIG(), "Message#getTSIG() must reflect the supplied query TSIG (or null)");
        assertEquals(query, message.getGeneratedTSIG(), "Message#getGeneratedTSIG() must reflect the supplied query TSIG (or null)");
        assertEquals(expectedError, getTsigError(message), "Message#tsigerror must store the exact error value supplied");
    }

    @ParameterizedTest(name = "[{index}] error={1}")
    @MethodSource("extremeErrorCodesProvider")
    void setTSIG_extremeErrorCodes(TSIG key, int error, TSIGRecord query, boolean expectKey, boolean expectRecord, int expectedError) throws Exception {
        message.setTSIG(key, error, query);
        assertEquals(query, message.getTSIG());
        assertEquals(query, message.getGeneratedTSIG());
        assertEquals(expectedError, getTsigError(message));
    }

    @ParameterizedTest(name = "[{index}] empty algo/secret")
    @MethodSource("emptyAlgorithmAndSecretProvider")
    void setTSIG_emptyAlgorithmAndSecret(TSIG key, int error, TSIGRecord query, boolean expectKey, boolean expectRecord, int expectedError) throws Exception {
        message.setTSIG(key, error, query);
        assertEquals(query, message.getTSIG());
        assertEquals(query, message.getGeneratedTSIG());
        assertEquals(expectedError, getTsigError(message));
    }

    @ParameterizedTest(name = "[{index}] TTL edge case")
    @MethodSource("ttlEdgeCasesProvider")
    void setTSIG_ttlEdgeCases(TSIG key, int error, TSIGRecord query, boolean expectKey, boolean expectRecord, int expectedError) throws Exception {
        message.setTSIG(key, error, query);
        assertEquals(query, message.getTSIG());
        assertEquals(query, message.getGeneratedTSIG());
        assertEquals(expectedError, getTsigError(message));
    }

    /* ---------- shared TSIG instance between key and query ---------- */
    @Test
    void setTSIG_sharedTSIGInstance_referenceEquality() throws Exception {
        TSIG shared = createTSIG("hmac-md5", "shared".getBytes());
        TSIGRecord record = createTSIGRecord(Name.fromString("shared.example."), DClass.IN, 300, shared);
        message.setTSIG(shared, 0, record);
        // Verify that the TSIG key stored inside the Message is the same instance we passed.
        assertSame(shared, getMessageTSIGKey(message), "The TSIG key stored in the Message must be the exact same instance supplied");
        // Also verify the internal error field
        assertEquals(0, getTsigError(message));
    }

    /* ---------- clearing query TSIG on subsequent call ---------- */
    @Test
    void setTSIG_clearingQueryTSIG_overwritesPreviousRecord() throws Exception {
        // First call – non‑null query TSIG
        TSIG key1 = createTSIG("hmac-md5", "first".getBytes());
        TSIGRecord rec1 = createTSIGRecord(Name.fromString("first.example."), DClass.IN, 100, key1);
        message.setTSIG(key1, 0, rec1);
        assertNotNull(message.getGeneratedTSIG(), "First call must store a non‑null generated TSIG");
        // Second call – null query TSIG, different key
        TSIG key2 = createTSIG("hmac-sha1", "second".getBytes());
        message.setTSIG(key2, 0, null);
        assertNull(message.getGeneratedTSIG(), "Second call with null query TSIG must clear the previously stored record");
        // Verify that the TSIG key inside the Message has been updated.
        assertSame(key2, getMessageTSIGKey(message), "The TSIG key should be updated to the second key");
    }
}
