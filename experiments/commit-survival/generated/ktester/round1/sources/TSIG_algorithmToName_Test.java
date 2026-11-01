package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;

class TSIG_algorithmToName_Test {

    /**
     * 1. Null argument must raise IllegalArgumentException.
     */
    @Test
    void algorithmToName_nullArgument_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TSIG.algorithmToName(null));
        assertEquals("Null algorithm", ex.getMessage());
    }

    /**
     * 2. Special‑case alias “HMAC‑MD5” (with or without trailing dot) must return the RFC name.
     */
    @Test
    void algorithmToName_hmacMd5Alias_returnsRfcName() {
        assertSame(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
        assertSame(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5."));
        // also verify case‑insensitivity
        assertSame(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5"));
        assertSame(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    }

    /**
     * 3. RFC algorithm names (case‑insensitive, with/without trailing dot) map to the correct Name constant.
     */
    @ParameterizedTest
    @CsvSource({ // algorithm string , expected TSIG constant name
    "hmac-sha1,          HMAC_SHA1", "hmac-sha1.,         HMAC_SHA1", "HMAC-SHA1,          HMAC_SHA1", "hmac-sha224,        HMAC_SHA224", "hmac-sha224.,       HMAC_SHA224", "hmac-sha256,        HMAC_SHA256", "hmac-sha256.,       HMAC_SHA256", "hmac-sha384,        HMAC_SHA384", "hmac-sha384.,       HMAC_SHA384", "hmac-sha512,        HMAC_SHA512", "hmac-sha512.,       HMAC_SHA512" })
    void algorithmToName_rfcNames_matchConstants(String alg, String expectedConstant) throws Exception {
        // Resolve the expected constant via reflection (e.g., TSIG.HMAC_SHA256)
        java.lang.reflect.Field field = TSIG.class.getField(expectedConstant);
        Name expectedName = (Name) field.get(null);
        assertSame(expectedName, TSIG.algorithmToName(alg));
    }

    /**
     * 4. Java algorithm names (the values stored in the map) must also resolve to the proper Name.
     */
    @Test
    void algorithmToName_javaNames_matchConstants() {
        // The map stores the Java names as values; for the SHA‑256 algorithm the Java name is "HmacSHA256".
        // Verify that both the canonical Java name and a mixed‑case variant work.
        assertSame(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
        // case‑insensitive
        assertSame(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmacsha256"));
    }

    /**
     * 5. Unknown algorithm strings must raise IllegalArgumentException with the proper message.
     */
    @Test
    void algorithmToName_unknownAlgorithm_throws() {
        String unknown = "not-a-valid-alg";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TSIG.algorithmToName(unknown));
        assertEquals("Unknown algorithm: " + unknown, ex.getMessage());
    }

    /**
     * Alias handling is case‑insensitive beyond the simple forms.
     */
    @ParameterizedTest
    @CsvSource({ "Hmac-Md5,   HMAC_MD5", "hmac-md5.,  HMAC_MD5" })
    void algorithmToName_aliasCaseInsensitive(String alg, String expectedConstant) throws Exception {
        Field f = TSIG.class.getField(expectedConstant);
        Name expected = (Name) f.get(null);
        assertSame(expected, TSIG.algorithmToName(alg));
    }

    @ParameterizedTest
    @CsvSource({ // double trailing dot
    "'hmac-sha1..',      hmac-sha1..", // leading whitespace
    "' HMAC-SHA256',    ' HMAC-SHA256'", // empty string
    "'',                ''", // double dot on alias
    "'hmac-md5..',      hmac-md5..", // full RFC name (not an alias)
    "'HMAC-MD5.SIG-ALG.REG.INT.', HMAC-MD5.SIG-ALG.REG.INT.'", // Java name with trailing dot
    "'HmacSHA256.',    HmacSHA256.'", "'hmacsha256.',    hmacsha256.'", // alias with extra characters
    "'HMAC-MD5-EXTRA', HMAC-MD5-EXTRA", "'hmac-md5extra',  hmac-md5extra", // non‑algorithm strings
    "'12345',          12345", "'null',           null", "'!',              !", // whitespace edge cases
    "'HMAC-SHA256 ',   HMAC-SHA256 '", "' HMAC-SHA256 ',  ' HMAC-SHA256 '", "'   ',            '   '", "'HMAC SHA256',    HMAC SHA256" })
    void algorithmToName_invalidInputs_throws(String input, String displayed) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TSIG.algorithmToName(input));
        assertEquals("Unknown algorithm: " + displayed, ex.getMessage());
    }
}
