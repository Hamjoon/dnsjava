```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
 *   <li>Rejection of unknown algorithms.
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
}
```