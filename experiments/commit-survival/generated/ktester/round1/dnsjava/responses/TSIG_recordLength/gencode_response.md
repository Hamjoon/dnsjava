```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TSIG#recordLength()}.
 *
 * The method is a pure calculation based on the lengths of the {@code Name} objects
 * supplied to the {@link TSIG} constructor.  The expected value is:
 *
 * <pre>
 *   name.length() + alg.length() + 48
 * </pre>
 *
 * (10 bytes for the signed time, 8 bytes for the fudge,
 * 18 bytes for the MAC length + MAC,
 * 4 bytes for the original ID and error,
 * 8 bytes for the error length and max error field.)
 *
 * The test suite covers:
 * <ul>
 *   <li>normal names and algorithms,</li>
 *   <li>algorithm‑length variations,</li>
 *   <li>edge‑case name lengths (root and maximum‑size name),</li>
 *   <li>behaviour when {@code name} or {@code alg} is {@code null},</li>
 *   <li>idempotence of the method, and</li>
 *   <li>that a supplied {@link Clock} does not influence the result.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TSIG_recordLength_Test {

    /** Simple key material – the actual value is irrelevant for the length calculation. */
    private static final byte[] DUMMY_KEY = new byte[16];

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the dummy key with deterministic data.
        new SecureRandom().nextBytes(DUMMY_KEY);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                    */
    /* --------------------------------------------------------------------- */

    /** Shortcut to create a TSIG instance using the constructor that accepts a byte[] key. */
    private TSIG createTSIG(Name alg, Name name) {
        return new TSIG(alg, name, DUMMY_KEY);
    }

    /** Sets a private field of a TSIG instance via reflection. */
    private void setPrivateField(TSIG tsig, String fieldName, Object value) throws Exception {
        Field f = TSIG.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(tsig, value);
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised tests for the normal behaviour groups                  */
    /* --------------------------------------------------------------------- */

    /** Normal behaviour – typical names with the default HMAC‑SHA256 algorithm. */
    private static Stream<Arguments> normalBehaviourProvider() {
        return Stream.of(
                // example.com.  -> length 13 (labels: 7+3+1 + root)
                Arguments.of("example.com.", TSIG.HMAC_SHA256, 74),
                // minimal name "a." -> length 3
                Arguments.of("a.", TSIG.HMAC_SHA256, 64),
                // long name
                Arguments.of("this.is.a.very.long.domain.name.example.", TSIG.HMAC_SHA256, 102)
        );
    }

    @ParameterizedTest(name = "[{index}] name=''{0}'' alg=HMAC_SHA256 expected={2}")
    @MethodSource("normalBehaviourProvider")
    void testRecordLengthNormalBehaviour(String nameStr, Name alg, int expected) throws Exception {
        Name name = Name.fromString(nameStr, Name.root);
        TSIG tsig = createTSIG(alg, name);
        assertEquals(expected, tsig.recordLength(),
                "recordLength() should follow name.length()+alg.length()+48");
    }

    /** Algorithm length variations – same name, different algorithm constants. */
    private static Stream<Arguments> algorithmLengthProvider() {
        return Stream.of(
                Arguments.of(TSIG.HMAC_MD5, 87),      // 13 + 26 + 48
                Arguments.of(TSIG.GSS_TSIG, 71),     // 13 + 10 + 48
                Arguments.of(TSIG.HMAC_SHA1, 72)     // 13 + 11 + 48
        );
    }

    @ParameterizedTest(name = "[{index}] alg=''{0}'' expected={1}")
    @MethodSource("algorithmLengthProvider")
    void testRecordLengthAlgorithmVariations(Name alg, int expected) throws Exception {
        Name name = Name.fromString("example.com.", Name.root); // length 13
        TSIG tsig = createTSIG(alg, name);
        assertEquals(expected, tsig.recordLength(),
                "Algorithm length variations must affect the total length");
    }

    /** Edge name length cases – root name and maximum‑size name. */
    private static Stream<Arguments> edgeNameProvider() {
        // root name "."
        Name root = Name.fromString(".", Name.root);
        // maximum size name: a single label of length 254 + root = 255 wire length
        StringBuilder sb = new StringBuilder(254);
        for (int i = 0; i < 254; i++) {
            sb.append('a');
        }
        Name max = Name.fromString(sb.toString() + ".", Name.root);
        return Stream.of(
                Arguments.of(root, TSIG.HMAC_SHA256, 62), // 1 + 13 + 48
                Arguments.of(root, TSIG.GSS_TSIG, 59),    // 1 + 10 + 48
                Arguments.of(max, TSIG.HMAC_SHA256, 316) // 255 + 13 + 48
        );
    }

    @ParameterizedTest(name = "[{index}] name length={0} alg={1} expected={2}")
    @MethodSource("edgeNameProvider")
    void testRecordLengthEdgeNames(Name name, Name alg, int expected) throws Exception {
        TSIG tsig = createTSIG(alg, name);
        assertEquals(expected, tsig.recordLength(),
                "Edge name lengths must be handled correctly");
    }

    /* --------------------------------------------------------------------- */
    /* Exception cases – null fields                                         */
    /* --------------------------------------------------------------------- */

    @Test
    void testRecordLengthNullName() throws Exception {
        TSIG tsig = createTSIG(TSIG.HMAC_SHA256,
                Name.fromString("example.", Name.root));
        setPrivateField(tsig, "name", null);
        assertThrows(NullPointerException.class, tsig::recordLength,
                "recordLength() should throw NPE when name is null");
    }

    @Test
    void testRecordLengthNullAlg() throws Exception {
        TSIG tsig = createTSIG(TSIG.HMAC_SHA256,
                Name.fromString("example.", Name.root));
        setPrivateField(tsig, "alg", null);
        assertThrows(NullPointerException.class, tsig::recordLength,
                "recordLength() should throw NPE when alg is null");
    }

    @Test
    void testRecordLengthBothNull() throws Exception {
        TSIG tsig = createTSIG(TSIG.HMAC_SHA256,
                Name.fromString("example.", Name.root));
        setPrivateField(tsig, "name", null);
        setPrivateField(tsig, "alg", null);
        assertThrows(NullPointerException.class, tsig::recordLength,
                "recordLength() should throw NPE when both fields are null");
    }

    /* --------------------------------------------------------------------- */
    /* Idempotent behaviour                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    void testRecordLengthIdempotent() throws Exception {
        Name name = Name.fromString("example.com.", Name.root);
        TSIG tsig = createTSIG(TSIG.HMAC_SHA256, name);
        int first = tsig.recordLength();
        int second = tsig.recordLength();
        assertEquals(74, first, "First call should return the expected length");
        assertEquals(first, second, "Subsequent calls must return the same value");
    }

    /* --------------------------------------------------------------------- */
    /* Clock‑independence test (already present in the original scaffold)   */
    /* --------------------------------------------------------------------- */

    @Test
    void testRecordLengthIgnoresClock() throws Exception {
        Name alg = TSIG.HMAC_SHA1;
        Name name = Name.fromString("example.", Name.root);
        Clock fixedClock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        TSIG tsig = new TSIG(alg, name,
                new SecretKeySpec(DUMMY_KEY, "HmacSHA1"), fixedClock);

        int expected = name.length() + alg.length() + 48;
        assertEquals(expected, tsig.recordLength(),
                "recordLength() must not be affected by the Clock instance");
    }
}
```