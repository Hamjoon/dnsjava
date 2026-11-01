```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TSIG#algorithmToName(String)}.
 *
 * The test suite follows the JSON test‑case description supplied with the
 * kata.  Each logical group from the JSON is represented by a dedicated test
 * method (or a parameterised test) so that the intent of every case is
 * explicit and maintainable.
 *
 * No mocking is required – the method under test is pure static and only
 * depends on the internal {@code algMap} which is populated by the TSIG class
 * itself.
 */
class TSIG_algorithmToName_Test {

    // -------------------------------------------------------------------------
    // 1. Null argument handling
    // -------------------------------------------------------------------------

    /** Null argument must raise IllegalArgumentException. */
    @Test
    void algorithmToName_nullArgument_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TSIG.algorithmToName(null));
        assertEquals("Null algorithm", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // 2. Alias for RFC name (HMAC‑MD5)
    // -------------------------------------------------------------------------

    @Test
    void algorithmToName_hmacMd5Alias_returnsRfcName() {
        assertSame(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
        assertSame(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5."));
        // case‑insensitivity
        assertSame(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5"));
        assertSame(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    }

    /** Alias handling is case‑insensitive beyond the simple forms. */
    @ParameterizedTest
    @CsvSource({
            "Hmac-Md5,   HMAC_MD5",
            "hmac-md5.,  HMAC_MD5"
    })
    void algorithmToName_aliasCaseInsensitive(String alg, String expectedConstant) throws Exception {
        Field f = TSIG.class.getField(expectedConstant);
        Name expected = (Name) f.get(null);
        assertSame(expected, TSIG.algorithmToName(alg));
    }

    // -------------------------------------------------------------------------
    // 3. RFC algorithm names (keys of algMap)
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            // algorithm string , expected TSIG constant name
            "hmac-sha1,          HMAC_SHA1",
            "hmac-sha1.,         HMAC_SHA1",
            "HMAC-SHA1,          HMAC_SHA1",
            "hmac-sha224,        HMAC_SHA224",
            "hmac-sha224.,       HMAC_SHA224",
            "hmac-sha256,        HMAC_SHA256",
            "hmac-sha256.,       HMAC_SHA256",
            "hmac-sha384,        HMAC_SHA384",
            "hmac-sha384.,       HMAC_SHA384",
            "hmac-sha512,        HMAC_SHA512",
            "hmac-sha512.,       HMAC_SHA512"
    })
    void algorithmToName_rfcNames_matchConstants(String alg, String expectedConstant) throws Exception {
        Field field = TSIG.class.getField(expectedConstant);
        Name expectedName = (Name) field.get(null);
        assertSame(expectedName, TSIG.algorithmToName(alg));
    }

    // -------------------------------------------------------------------------
    // 4. Java algorithm names (values of algMap)
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "HmacSHA1,   HMAC_SHA1",
            "hmacsha224, HMAC_SHA224",
            "HmacSHA384, HMAC_SHA384",
            "HmacSHA512, HMAC_SHA512",
            "HmacSHA256, HMAC_SHA256",
            "hmacsha256, HMAC_SHA256"
    })
    void algorithmToName_javaNames_matchConstants(String alg, String expectedConstant) throws Exception {
        Field field = TSIG.class.getField(expectedConstant);
        Name expected = (Name) field.get(null);
        assertSame(expected, TSIG.algorithmToName(alg));
    }

    // -------------------------------------------------------------------------
    // 5. Invalid / unknown algorithm strings
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            // double trailing dot
            "'hmac-sha1..',      hmac-sha1..",
            // leading whitespace
            "' HMAC-SHA256',    ' HMAC-SHA256'",
            // empty string
            "'',                ''",
            // double dot on alias
            "'hmac-md5..',      hmac-md5..",
            // full RFC name (not an alias)
            "'HMAC-MD5.SIG-ALG.REG.INT.', HMAC-MD5.SIG-ALG.REG.INT.'",
            // Java name with trailing dot
            "'HmacSHA256.',    HmacSHA256.'",
            "'hmacsha256.',    hmacsha256.'",
            // alias with extra characters
            "'HMAC-MD5-EXTRA', HMAC-MD5-EXTRA",
            "'hmac-md5extra',  hmac-md5extra",
            // non‑algorithm strings
            "'12345',          12345",
            "'null',           null",
            "'!',              !",
            // whitespace edge cases
            "'HMAC-SHA256 ',   HMAC-SHA256 '",
            "' HMAC-SHA256 ',  ' HMAC-SHA256 '",
            "'   ',            '   '",
            "'HMAC SHA256',    HMAC SHA256"
    })
    void algorithmToName_invalidInputs_throws(String input, String displayed) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TSIG.algorithmToName(input));
        assertEquals("Unknown algorithm: " + displayed, ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // No special setup/teardown is required for these pure static tests.
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test resources to initialise.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }
}
```