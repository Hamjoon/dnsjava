```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprehensive tests for {@link Message#setTSIG(TSIG, int, TSIGRecord)}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>basic field assignment (including nulls and negative/large error codes)</li>
 *   <li>extreme error code values</li>
 *   <li>empty algorithm / empty secret handling</li>
 *   <li>TTL edge cases in the supplied {@link TSIGRecord}</li>
 *   <li>reference‑equality when the same {@link TSIG} instance is used for both key and record</li>
 *   <li>over‑writing behaviour when {@code setTSIG} is called multiple times</li>
 * </ul>
 *
 * <p>
 * Verification is performed via the public {@link Message#getTSIG()} and
 * {@link Message#getGeneratedTSIG()} methods and, for the {@code tsigerror}
 * field, via reflection (as there is no public accessor).
 *
 * @author OpenAI ChatGPT
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_2_Test {

    private Message message;
    private TSIG tsigKey;
    private TSIGRecord tsigRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        message = new Message();

        // a default key/record used by a few tests – individual tests can replace them
        Name alg = Name.fromString("hmac-md5.");
        Name name = Name.fromString("example.com.");
        tsigKey = new TSIG(alg, name, "YWJjZGVmZ2hpamtsbW5vcA=="); // "abcdefghijklmnop"

        tsigRecord = new TSIGRecord(
                name,
                DClass.IN,
                0L,
                alg,
                Instant.now(),
                Duration.ofSeconds(300),
                new byte[]{0x01, 0x02},
                0x1234,
                Rcode.NOERROR,
                new byte[0]
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                    */
    /* --------------------------------------------------------------------- */

    /** Reads the private {@code tsigerror} field via reflection. */
    private static int getTsigError(Message m) throws Exception {
        Field f = Message.class.getDeclaredField("tsigerror");
        f.setAccessible(true);
        return (int) f.get(m);
    }

    /** Creates a TSIG instance from an algorithm string and a raw secret byte array. */
    private static TSIG createTSIG(String algorithm, byte[] secret) {
        // The TSIG constructor used in the library accepts (Name algorithm, Name name, String base64Secret).
        // For the purpose of these tests the actual name does not matter, so we use a constant.
        Name algName = Name.fromString(algorithm.endsWith(".") ? algorithm : algorithm + ".");
        Name owner = Name.fromString("keyowner.example.");
        String b64 = Base64.getEncoder().encodeToString(secret);
        return new TSIG(algName, owner, b64);
    }

    /** Creates a TSIGRecord with the supplied parameters. */
    private static TSIGRecord createTSIGRecord(Name owner, int dclass, long ttl, TSIG key) {
        return new TSIGRecord(
                owner,
                dclass,
                ttl,
                key.getAlgorithm(),
                Instant.now(),
                Duration.ofSeconds(300),
                new byte[]{0x0A, 0x0B},
                0x5678,
                Rcode.NOERROR,
                new byte[0]
        );
    }

    /* --------------------------------------------------------------------- */
    /* Existing simple tests (kept unchanged)                                */
    /* --------------------------------------------------------------------- */

    @Test
    void setTSIG_threeArguments_storesValues() {
        message.setTSIG(tsigKey, Rcode.BADKEY, tsigRecord);
        assertSame(tsigRecord, message.getTSIG(),
                "Message#getTSIG() should return the TSIGRecord supplied to setTSIG");
    }

    @Test
    void setTSIG_oneArgument_delegatesCorrectly() {
        message.setTSIG(tsigKey);
        assertNull(message.getTSIG(),
                "Message#getTSIG() should be null when setTSIG(TSIG) is used without a TSIGRecord");
    }

    @Test
    void setTSIG_calledMultipleTimes_updatesRecord() {
        message.setTSIG(tsigKey, Rcode.NOERROR, tsigRecord);
        assertSame(tsigRecord, message.getTSIG(),
                "First call should store the initial TSIGRecord");

        TSIGRecord secondRecord = new TSIGRecord(
                Name.fromString("other.example.com."),
                DClass.IN,
                0L,
                Name.fromString("hmac-sha256."),
                Instant.now(),
                Duration.ofSeconds(300),
                new byte[]{0x0A, 0x0B},
                0x5678,
                Rcode.NOERROR,
                new byte[0]
        );

        message.setTSIG(tsigKey, Rcode.NOERROR, secondRecord);
        assertSame(secondRecord, message.getTSIG(),
                "Second call should replace the previously stored TSIGRecord");
    }

    @Test
    void setTSIG_nullRecord_returnsNullTSIG() {
        message.setTSIG(tsigKey, Rcode.NOERROR, null);
        assertNull(message.getTSIG(),
                "Message#getTSIG() should be null when setTSIG is called with a null TSIGRecord");
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised tests for the JSON‑driven scenarios                    */
    /* --------------------------------------------------------------------- */

    /* ---------- basic field assignment ---------- */
    private static Stream<Arguments> basicFieldAssignmentProvider() {
        return Stream.of(
                // Standard usage with non‑null key and query TSIG, error 0
                Arguments.of(
                        createTSIG("hmac-md5", "secret".getBytes()),
                        0,
                        createTSIGRecord(Name.fromString("example.com."), DClass.IN, 300,
                                new TSIG(Name.fromString("hmac-md5."), Name.fromString("example.com."), "c2VjcmV0")),
                        true, true, 0),

                // Both key and query TSIG null, error 0
                Arguments.of(null, 0, null, false, false, 0),

                // Negative error code, non‑null key, null query TSIG
                Arguments.of(
                        createTSIG("hmac-sha256", "key".getBytes()),
                        -1,
                        null,
                        true, false, -1),

                // Null key, large positive error, non‑null query TSIG
                Arguments.of(
                        null,
                        65535,
                        createTSIGRecord(Name.fromString("example.org."), DClass.IN, 600,
                                new TSIG(Name.fromString("hmac-sha1."), Name.fromString("example.org."), "YW5vdGhlcg==")),
                        false, true, 65535)
        );
    }

    @ParameterizedTest(name = "[{index}] key={0 != null}, error={1}, queryTSIG={2 != null}")
    @MethodSource("basicFieldAssignmentProvider")
    void setTSIG_basicFieldAssignment(TSIG key, int error, TSIGRecord query, boolean expectKey,
                                        boolean expectRecord, int expectedError) throws Exception {
        message.setTSIG(key, error, query);

        // getTSIG() returns the query TSIG (or null)
        assertEquals(query, message.getTSIG(),
                "Message#getTSIG() must reflect the supplied query TSIG (or null)");

        // getGeneratedTSIG() is expected to return the same instance as setTSIG stores
        assertEquals(query, message.getGeneratedTSIG(),
                "Message#getGeneratedTSIG() must reflect the supplied query TSIG (or null)");

        // verify the internal error field via reflection
        assertEquals(expectedError, getTsigError(message),
                "Message#tsigerror must store the exact error value supplied");
    }

    /* ---------- extreme error codes ---------- */
    private static Stream<Arguments> extremeErrorCodesProvider() {
        return Stream.of(
                // Integer.MAX_VALUE with non‑null key
                Arguments.of(
                        createTSIG("hmac-sha512", "bigkey".getBytes()),
                        Integer.MAX_VALUE,
                        null,
                        true, false, Integer.MAX_VALUE),

                // Integer.MIN_VALUE with null key
                Arguments.of(
                        null,
                        Integer.MIN_VALUE,
                        null,
                        false, false, Integer.MIN_VALUE)
        );
    }

    @ParameterizedTest(name = "[{index}] error={1}")
    @MethodSource("extremeErrorCodesProvider")
    void setTSIG_extremeErrorCodes(TSIG key, int error, TSIGRecord query,
                                   boolean expectKey, boolean expectRecord, int expectedError) throws Exception {
        message.setTSIG(key, error, query);
        assertEquals(query, message.getTSIG());
        assertEquals(query, message.getGeneratedTSIG());
        assertEquals(expectedError, getTsigError(message));
    }

    /* ---------- empty algorithm and secret handling ---------- */
    private static Stream<Arguments> emptyAlgorithmAndSecretProvider() {
        return Stream.of(
                // Empty algorithm string and empty secret
                Arguments.of(
                        createTSIG("", new byte[0]),
                        0,
                        null,
                        true, false, 0),

                // Non‑empty algorithm, empty secret, with a TSIGRecord containing the same empty secret
                Arguments.of(
                        createTSIG("hmac-md5", new byte[0]),
                        0,
                        createTSIGRecord(Name.fromString("emptysecret.example."), DClass.IN, 300,
                                createTSIG("hmac-md5", new byte[0])),
                        true, true, 0)
        );
    }

    @ParameterizedTest(name = "[{index}] empty algo/secret")
    @MethodSource("emptyAlgorithmAndSecretProvider")
    void setTSIG_emptyAlgorithmAndSecret(TSIG key, int error, TSIGRecord query,
                                        boolean expectKey, boolean expectRecord, int expectedError) throws Exception {
        message.setTSIG(key, error, query);
        assertEquals(query, message.getTSIG());
        assertEquals(query, message.getGeneratedTSIG());
        assertEquals(expectedError, getTsigError(message));
    }

    /* ---------- zero and maximum TTL in query TSIG ---------- */
    private static Stream<Arguments> ttlEdgeCasesProvider() {
        return Stream.of(
                // TTL zero
                Arguments.of(
                        createTSIG("hmac-sha256", "key".getBytes()),
                        0,
                        createTSIGRecord(Name.fromString("zero.ttl.example."), DClass.IN, 0,
                                createTSIG("hmac-sha256", "key".getBytes())),
                        true, true, 0),

                // TTL Integer.MAX_VALUE
                Arguments.of(
                        createTSIG("hmac-sha256", "key".getBytes()),
                        0,
                        createTSIGRecord(Name.fromString("max.ttl.example."), DClass.IN, Integer.MAX_VALUE,
                                createTSIG("hmac-sha256", "key".getBytes())),
                        true, true, 0)
        );
    }

    @ParameterizedTest(name = "[{index}] TTL edge case")
    @MethodSource("ttlEdgeCasesProvider")
    void setTSIG_ttlEdgeCases(TSIG key, int error, TSIGRecord query,
                             boolean expectKey, boolean expectRecord, int expectedError) throws Exception {
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

        // The TSIGRecord stored should reference the same TSIG instance as the key
        assertSame(shared, message.getTSIG().getKey(),
                "The TSIGRecord's key must be the exact same instance supplied as the TSIG key");
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
        assertNull(message.getGeneratedTSIG(),
                "Second call with null query TSIG must clear the previously stored record");
        assertSame(key2, message.getTSIG().getKey(),
                "The TSIG key should be updated to the second key");
    }
}
```