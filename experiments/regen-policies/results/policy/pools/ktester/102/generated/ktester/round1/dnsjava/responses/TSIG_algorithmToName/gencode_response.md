```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xbill.DNS.TextParseException;

/**
 * Unit tests for {@link TSIG#algorithmToName(String)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>Null argument handling.
 *   <li>Conversion of RFC‑style algorithm strings (with and without trailing dot).
 *   <li>Conversion of Java‑style algorithm names (e.g. {@code HmacSHA256/128}).
 *   <li>Special alias handling for {@code hmac‑md5}.
 *   <li>Rejection of unknown or malformed algorithms.
 * </ul>
 *
 * No private members of {@link TSIG} are accessed; the public API is used exclusively.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_algorithmToName_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global state required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
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
    /*  Explicit tests that were already present in the original scaffold   */
    /* --------------------------------------------------------------------- */

    @Test
    void algorithmToName_nullArgument_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> TSIG.algorithmToName(null));
        assertEquals("Null algorithm", ex.getMessage());
    }

    @Test
    void algorithmToName_rfcStyle_withoutTrailingDot_returnsCorrectName() {
        // Input without trailing dot – method should append it.
        Name result = TSIG.algorithmToName("hmac-sha256");
        assertEquals(TSIG.HMAC_SHA256, result);
    }

    @Test
    void algorithmToName_rfcStyle_withTrailingDot_returnsCorrectName() {
        Name result = TSIG.algorithmToName("hmac-sha384.");
        assertEquals(TSIG.HMAC_SHA384, result);
    }

    @Test
    void algorithmToName_javaStyle_withoutLength_returnsCorrectName() {
        // Java name "HmacSHA1" → "hmac-sha1."
        Name result = TSIG.algorithmToName("HmacSHA1");
        assertEquals(TSIG.HMAC_SHA1, result);
    }

    @Test
    void algorithmToName_javaStyle_withLength_returnsCorrectName() {
        // Java name "HmacSHA256/128" → "hmac-sha256-128."
        Name result = TSIG.algorithmToName("HmacSHA256/128");
        assertEquals(TSIG.HMAC_SHA256_128, result);
    }

    @Test
    void algorithmToName_javaStyle_lowerCaseWithSlash_returnsCorrectName() {
        // Accept lower‑case variant as well.
        Name result = TSIG.algorithmToName("hmacsha256/128");
        assertEquals(TSIG.HMAC_SHA256_128, result);
    }

    @Test
    void algorithmToName_hmacMd5Alias_returnsHmacMd5Constant() {
        // "hmac-md5" is an alias for the RFC name.
        Name result = TSIG.algorithmToName("hmac-md5");
        assertEquals(TSIG.HMAC_MD5, result);
    }

    @Test
    void algorithmToName_unknownAlgorithm_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> TSIG.algorithmToName("unknown-alg"));
        // The message contains the unknown name; we only verify the prefix.
        assertTrue(ex.getMessage().startsWith("Unknown algorithm:"));
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterized tests generated from the exhaustive JSON specification */
    /* --------------------------------------------------------------------- */

    /**
     * Successful conversions – the method should return a {@link Name}
     * equal to the expected DNS name.
     */
    @ParameterizedTest(name = "[{index}] algorithmToName(''{0}'') → ''{1}''")
    @MethodSource("successfulCases")
    void algorithmToName_success(String alg, String expectedDnsName) throws Exception {
        Name actual = TSIG.algorithmToName(alg);
        Name expected = Name.fromString(expectedDnsName);
        assertEquals(expected, actual,
                () -> "Expected Name " + expected + " but got " + actual);
    }

    /**
     * Cases that must result in an {@link IllegalArgumentException}.
     * The expected string encodes the kind of validation we need:
     * <ul>
     *   <li>starts with "Null algorithm" – exact message check</li>
     *   <li>contains "TextParseException" – the cause must be a {@link TextParseException}</li>
     *   <li>starts with "Unknown algorithm:" – message must start with that prefix</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] algorithmToName(''{0}'') throws {1}")
    @MethodSource("exceptionCases")
    void algorithmToName_exceptions(String alg, String expectedPattern) {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> TSIG.algorithmToName(alg));

        if (expectedPattern.startsWith("Null algorithm")) {
            assertEquals("Null algorithm", ex.getMessage(),
                    "Message should be exactly 'Null algorithm'");
        } else if (expectedPattern.contains("TextParseException")) {
            assertNotNull(ex.getCause(),
                    "Cause should be present for TextParseException cases");
            assertTrue(ex.getCause() instanceof TextParseException,
                    "Cause must be a TextParseException");
        } else if (expectedPattern.startsWith("Unknown algorithm:")) {
            assertTrue(ex.getMessage().startsWith("Unknown algorithm:"),
                    "Message should start with 'Unknown algorithm:'");
        } else {
            fail("Unrecognised expected pattern: " + expectedPattern);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Method sources supplying the data for the parameterized tests        */
    /* --------------------------------------------------------------------- */

    /** Successful conversion test data. */
    private static Stream<Arguments> successfulCases() {
        return Stream.of(
                // RFC‑style names (dot added automatically)
                Arguments.of("hmac-sha256", "hmac-sha256."),
                Arguments.of("hmac-sha384.", "hmac-sha384."),
                // Java‑style names without length
                Arguments.of("HmacSHA1", "hmac-sha1."),
                Arguments.of("HmacSHA256", "hmac-sha256."),
                Arguments.of("HMACSHA1", "hmac-sha1."),
                Arguments.of("hmacsha256", "hmac-sha256."),
                // Java‑style names with length
                Arguments.of("HmacSHA256/128", "hmac-sha256-128."),
                Arguments.of("hmacsha256/128", "hmac-sha256-128."),
                // Mixed‑case RFC style
                Arguments.of("HMAC-SHA384", "hmac-sha384."),
                // Alias for MD5 (with and without trailing dot)
                Arguments.of("hmac-md5", "HMAC-MD5.SIG-ALG.REG.INT."),
                Arguments.of("hmac-md5.", "HMAC-MD5.SIG-ALG.REG.INT.")
        );
    }

    /** Exception‑throwing test data. */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(
                // Null input
                Arguments.of(null, "Null algorithm"),
                // Non‑matching Java name → unknown algorithm after dot added
                Arguments.of("foobar", "Unknown algorithm:"),
                // Invalid DNS name (contains space)
                Arguments.of("  hmac-sha256  ", "TextParseException"),
                // Invalid DNS name (double dot)
                Arguments.of("hmac-sha256..", "TextParseException"),
                // Java name with unsupported truncated length (algorithm not in map)
                Arguments.of("HmacSHA1/256", "Unknown algorithm:"),
                // Hyphenated name with unsupported length suffix (not in map)
                Arguments.of("hmac-sha256-128", "Unknown algorithm:"),
                // Hyphenated unknown algorithm
                Arguments.of("hmac-foo", "Unknown algorithm:"),
                // Empty string → becomes "." after dot‑append, not in map
                Arguments.of("", "Unknown algorithm:"),
                // Root name dot
                Arguments.of(".", "Unknown algorithm:"),
                // Hyphen‑only string
                Arguments.of("-", "TextParseException"),
                // Trailing hyphen
                Arguments.of("hmac-", "TextParseException"),
                // Hyphenated name containing a slash (illegal DNS character)
                Arguments.of("hmac-sha256/128", "TextParseException"),
                // Illegal character '$'
                Arguments.of("hmac$sha256", "TextParseException"),
                // Unsupported SHA number in Java style
                Arguments.of("HmacSHA999", "Unknown algorithm:")
        );
    }
}
```