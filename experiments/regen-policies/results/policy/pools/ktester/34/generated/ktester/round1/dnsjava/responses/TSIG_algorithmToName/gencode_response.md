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

/**
 * Unit tests for {@link TSIG#algorithmToName(String)}.
 *
 * The tests cover all scenarios described in the JSON test case specification:
 *
 * <ul>
 *   <li>Null input</li>
 *   <li>Alias for HMAC‑MD5 (case‑variations, with/without trailing dot)</li>
 *   <li>Exact RFC name matches (with/without trailing dot)</li>
 *   <li>Case‑insensitive RFC name matches</li>
 *   <li>Java algorithm name matches (including case‑insensitive variants)</li>
 *   <li>Various malformed inputs that must raise {@link IllegalArgumentException}
 *       with the expected message.</li>
 * </ul>
 *
 * No mocking is required because {@code algorithmToName} is a pure static method.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_algorithmToName_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
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

    /** {@code null} algorithm must cause an {@link IllegalArgumentException} with the correct message. */
    @Test
    void nullAlgorithmThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TSIG.algorithmToName(null));
        assertEquals("Null algorithm", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Alias for HMAC‑MD5
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] alias ''{0}'' → HMAC_MD5")
    @MethodSource("aliasForHmacMd5Provider")
    void aliasForHmacMd5(String input, Name expected) {
        assertEquals(expected, TSIG.algorithmToName(input));
    }

    static Stream<Arguments> aliasForHmacMd5Provider() {
        return Stream.of(
                Arguments.of("HMAC-MD5", TSIG.HMAC_MD5),
                Arguments.of("HMAC-MD5.", TSIG.HMAC_MD5),
                Arguments.of("hmac-md5", TSIG.HMAC_MD5),
                Arguments.of("hmac-md5.", TSIG.HMAC_MD5),
                Arguments.of("Hmac-Md5.", TSIG.HMAC_MD5) // mixed case with trailing dot
        );
    }

    /* --------------------------------------------------------------------- *
     *  Exact RFC name matches (with and without trailing dot)
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] RFC name ''{0}'' → {1}")
    @MethodSource("rfcNameProvider")
    void rfcNamesMatch(String input, Name expected) {
        assertEquals(expected, TSIG.algorithmToName(input));
    }

    static Stream<Arguments> rfcNameProvider() {
        return Stream.of(
                // SHA‑1
                Arguments.of("hmac-sha1", TSIG.HMAC_SHA1),
                Arguments.of("hmac-sha1.", TSIG.HMAC_SHA1),
                // SHA‑224
                Arguments.of("hmac-sha224", TSIG.HMAC_SHA224),
                Arguments.of("hmac-sha224.", TSIG.HMAC_SHA224),
                // SHA‑256
                Arguments.of("hmac-sha256", TSIG.HMAC_SHA256),
                Arguments.of("hmac-sha256.", TSIG.HMAC_SHA256),
                // SHA‑384
                Arguments.of("hmac-sha384", TSIG.HMAC_SHA384),
                Arguments.of("hmac-sha384.", TSIG.HMAC_SHA384),
                // SHA‑512
                Arguments.of("hmac-sha512", TSIG.HMAC_SHA512),
                Arguments.of("hmac-sha512.", TSIG.HMAC_SHA512)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Case‑insensitive RFC name matches (mixed case)
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] mixed‑case RFC ''{0}'' → {1}")
    @MethodSource("mixedCaseRfcProvider")
    void mixedCaseRfcNames(String input, Name expected) {
        assertEquals(expected, TSIG.algorithmToName(input));
    }

    static Stream<Arguments> mixedCaseRfcProvider() {
        return Stream.of(
                Arguments.of("Hmac-Sha256", TSIG.HMAC_SHA256),
                Arguments.of("Hmac-Sha256.", TSIG.HMAC_SHA256),
                Arguments.of("Hmac-Sha512.", TSIG.HMAC_SHA512),
                Arguments.of("Hmac-Sha224.", TSIG.HMAC_SHA224)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Java algorithm name matches (including case‑insensitive variants)
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] Java name ''{0}'' → {1}")
    @MethodSource("javaAlgorithmNameProvider")
    void javaAlgorithmNamesMatch(String input, Name expected) {
        assertEquals(expected, TSIG.algorithmToName(input));
    }

    static Stream<Arguments> javaAlgorithmNameProvider() {
        return Stream.of(
                Arguments.of("HmacMD5", TSIG.HMAC_MD5),
                Arguments.of("HmacSHA1", TSIG.HMAC_SHA1),
                Arguments.of("HmacSHA224", TSIG.HMAC_SHA224),
                Arguments.of("HmacSHA256", TSIG.HMAC_SHA256),
                Arguments.of("HmacSHA384", TSIG.HMAC_SHA384),
                Arguments.of("HmacSHA512", TSIG.HMAC_SHA512),
                // case‑insensitive variants
                Arguments.of("hmacsha256", TSIG.HMAC_SHA256),
                Arguments.of("HMACSHA384", TSIG.HMAC_SHA384)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Invalid / malformed inputs – expect IllegalArgumentException with
     *  specific messages.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] invalid ''{0}'' → exception")
    @MethodSource("invalidInputProvider")
    void invalidInputsThrow(String input, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TSIG.algorithmToName(input));
        assertEquals(expectedMessage, ex.getMessage());
    }

    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(
                // Empty string
                Arguments.of("", "Unknown algorithm: "),
                // Whitespace not trimmed
                Arguments.of("  hmac-sha256  ", "Unknown algorithm:   hmac-sha256  "),
                // Leading dot
                Arguments.of(".hmac-sha1", "Unknown algorithm: .hmac-sha1"),
                // Trailing dot on Java name
                Arguments.of("HmacSHA384.", "Unknown algorithm: HmacSHA384."),
                // Leading dot with Java name
                Arguments.of(".HmacSHA256", "Unknown algorithm: .HmacSHA256"),
                // Double trailing dot
                Arguments.of("hmac-sha1..", "Unknown algorithm: hmac-sha1.."),
                // Extra suffix
                Arguments.of("hmac-sha256-extra", "Unknown algorithm: hmac-sha256-extra"),
                // Newline character
                Arguments.of("hmac-sha256\n", "Unknown algorithm: hmac-sha256\n"),
                // Tab character
                Arguments.of("hmac-sha256\t", "Unknown algorithm: hmac-sha256\t"),
                // Single dot only
                Arguments.of(".", "Unknown algorithm: ."),
                // Internal whitespace
                Arguments.of("hmac sha256", "Unknown algorithm: hmac sha256"),
                // Completely unknown algorithm
                Arguments.of("UNKNOWN-ALG", "Unknown algorithm: UNKNOWN-ALG")
        );
    }
}
```