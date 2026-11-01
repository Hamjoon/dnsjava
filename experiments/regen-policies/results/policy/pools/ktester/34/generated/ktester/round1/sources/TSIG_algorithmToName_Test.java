package org.xbill.DNS;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class TSIG_algorithmToName_Test {

    static Stream<Arguments> aliasForHmacMd5Provider() {
        return Stream.of(Arguments.of("HMAC-MD5", TSIG.HMAC_MD5), Arguments.of("HMAC-MD5.", TSIG.HMAC_MD5), Arguments.of("hmac-md5", TSIG.HMAC_MD5), Arguments.of("hmac-md5.", TSIG.HMAC_MD5), // mixed case with trailing dot
        Arguments.of("Hmac-Md5.", TSIG.HMAC_MD5));
    }

    static Stream<Arguments> rfcNameProvider() {
        return Stream.of(// SHA‑1
        Arguments.of("hmac-sha1", TSIG.HMAC_SHA1), Arguments.of("hmac-sha1.", TSIG.HMAC_SHA1), // SHA‑224
        Arguments.of("hmac-sha224", TSIG.HMAC_SHA224), Arguments.of("hmac-sha224.", TSIG.HMAC_SHA224), // SHA‑256
        Arguments.of("hmac-sha256", TSIG.HMAC_SHA256), Arguments.of("hmac-sha256.", TSIG.HMAC_SHA256), // SHA‑384
        Arguments.of("hmac-sha384", TSIG.HMAC_SHA384), Arguments.of("hmac-sha384.", TSIG.HMAC_SHA384), // SHA‑512
        Arguments.of("hmac-sha512", TSIG.HMAC_SHA512), Arguments.of("hmac-sha512.", TSIG.HMAC_SHA512));
    }

    static Stream<Arguments> mixedCaseRfcProvider() {
        return Stream.of(Arguments.of("Hmac-Sha256", TSIG.HMAC_SHA256), Arguments.of("Hmac-Sha256.", TSIG.HMAC_SHA256), Arguments.of("Hmac-Sha512.", TSIG.HMAC_SHA512), Arguments.of("Hmac-Sha224.", TSIG.HMAC_SHA224));
    }

    static Stream<Arguments> javaAlgorithmNameProvider() {
        return Stream.of(Arguments.of("HmacMD5", TSIG.HMAC_MD5), Arguments.of("HmacSHA1", TSIG.HMAC_SHA1), Arguments.of("HmacSHA224", TSIG.HMAC_SHA224), Arguments.of("HmacSHA256", TSIG.HMAC_SHA256), Arguments.of("HmacSHA384", TSIG.HMAC_SHA384), Arguments.of("HmacSHA512", TSIG.HMAC_SHA512), // case‑insensitive variants
        Arguments.of("hmacsha256", TSIG.HMAC_SHA256), Arguments.of("HMACSHA384", TSIG.HMAC_SHA384));
    }

    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Empty string
        Arguments.of("", "Unknown algorithm: "), // Whitespace not trimmed
        Arguments.of("  hmac-sha256  ", "Unknown algorithm:   hmac-sha256  "), // Leading dot
        Arguments.of(".hmac-sha1", "Unknown algorithm: .hmac-sha1"), // Trailing dot on Java name
        Arguments.of("HmacSHA384.", "Unknown algorithm: HmacSHA384."), // Leading dot with Java name
        Arguments.of(".HmacSHA256", "Unknown algorithm: .HmacSHA256"), // Double trailing dot
        Arguments.of("hmac-sha1..", "Unknown algorithm: hmac-sha1.."), // Extra suffix
        Arguments.of("hmac-sha256-extra", "Unknown algorithm: hmac-sha256-extra"), // Newline character
        Arguments.of("hmac-sha256\n", "Unknown algorithm: hmac-sha256\n"), // Tab character
        Arguments.of("hmac-sha256\t", "Unknown algorithm: hmac-sha256\t"), // Single dot only
        Arguments.of(".", "Unknown algorithm: ."), // Internal whitespace
        Arguments.of("hmac sha256", "Unknown algorithm: hmac sha256"), // Completely unknown algorithm
        Arguments.of("UNKNOWN-ALG", "Unknown algorithm: UNKNOWN-ALG"));
    }

    /**
     * {@code null} algorithm must cause an {@link IllegalArgumentException}.
     */
    @Test
    void nullAlgorithmThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TSIG.algorithmToName(null));
        assertEquals("Null algorithm", ex.getMessage());
    }

    /**
     * The alias “HMAC‑MD5” (with or without a trailing dot) must resolve to {@link TSIG#HMAC_MD5}.
     */
    @Test
    void aliasHmacMd5() {
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5."));
        // case‑insensitivity
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5"));
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    }

    /**
     * RFC names are matched case‑insensitively and with or without a trailing dot.
     */
    @Test
    void rfcNamesMatch() {
        // SHA‑256 examples – the same pattern applies to the other algorithms
        assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
        assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));
        assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HMAC-SHA256"));
        assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HMAC-SHA256."));
        // SHA‑1
        assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1"));
        assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1."));
        // SHA‑224
        assertEquals(TSIG.HMAC_SHA224, TSIG.algorithmToName("hmac-sha224"));
        assertEquals(TSIG.HMAC_SHA224, TSIG.algorithmToName("hmac-sha224."));
        // SHA‑384
        assertEquals(TSIG.HMAC_SHA384, TSIG.algorithmToName("hmac-sha384"));
        assertEquals(TSIG.HMAC_SHA384, TSIG.algorithmToName("hmac-sha384."));
        // SHA‑512
        assertEquals(TSIG.HMAC_SHA512, TSIG.algorithmToName("hmac-sha512"));
        assertEquals(TSIG.HMAC_SHA512, TSIG.algorithmToName("hmac-sha512."));
    }

    /**
     * Java algorithm names stored as values in {@code algMap} must also resolve to the correct {@link Name}.
     */
    @Test
    void javaAlgorithmNamesMatch() {
        // The mapping is defined in TSIG’s static initializer; the known Java names are
        // "HmacMD5", "HmacSHA1", "HmacSHA224", "HmacSHA256", "HmacSHA384", "HmacSHA512".
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HmacMD5"));
        assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("HmacSHA1"));
        assertEquals(TSIG.HMAC_SHA224, TSIG.algorithmToName("HmacSHA224"));
        assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
        assertEquals(TSIG.HMAC_SHA384, TSIG.algorithmToName("HmacSHA384"));
        assertEquals(TSIG.HMAC_SHA512, TSIG.algorithmToName("HmacSHA512"));
    }

    /**
     * An unknown algorithm name must cause an {@link IllegalArgumentException}.
     */
    @Test
    void unknownAlgorithmThrows() {
        assertThrows(IllegalArgumentException.class, () -> TSIG.algorithmToName("unknown-algorithm"));
    }

    /* --------------------------------------------------------------------- *
     *  Alias for HMAC‑MD5
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] alias ''{0}'' → HMAC_MD5")
    @MethodSource("aliasForHmacMd5Provider")
    void aliasForHmacMd5(String input, Name expected) {
        assertEquals(expected, TSIG.algorithmToName(input));
    }

    /* --------------------------------------------------------------------- *
     *  Case‑insensitive RFC name matches (mixed case)
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] mixed‑case RFC ''{0}'' → {1}")
    @MethodSource("mixedCaseRfcProvider")
    void mixedCaseRfcNames(String input, Name expected) {
        assertEquals(expected, TSIG.algorithmToName(input));
    }

    /* --------------------------------------------------------------------- *
     *  Invalid / malformed inputs – expect IllegalArgumentException with
     *  specific messages.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] invalid ''{0}'' → exception")
    @MethodSource("invalidInputProvider")
    void invalidInputsThrow(String input, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TSIG.algorithmToName(input));
        assertEquals(expectedMessage, ex.getMessage());
    }
}
