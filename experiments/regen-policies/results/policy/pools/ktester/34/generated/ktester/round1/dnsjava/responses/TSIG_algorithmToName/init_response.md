```java
package org.xbill.DNS;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TSIG#algorithmToName(String)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Null argument handling</li>
 *   <li>Special‑case alias for HMAC‑MD5</li>
 *   <li>Case‑insensitive matching of RFC names (with and without trailing dot)</li>
 *   <li>Matching of the Java algorithm names stored in {@code algMap}</li>
 *   <li>Exception for unknown algorithms</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TSIG_algorithmToName_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these static method tests.
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

    /** {@code null} algorithm must cause an {@link IllegalArgumentException}. */
    @Test
    void nullAlgorithmThrows() {
        assertThrows(IllegalArgumentException.class, () -> TSIG.algorithmToName(null));
    }

    /** The alias “HMAC‑MD5” (with or without a trailing dot) must resolve to {@link TSIG#HMAC_MD5}. */
    @Test
    void aliasHmacMd5() {
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5."));
        // case‑insensitivity
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5"));
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    }

    /** RFC names are matched case‑insensitively and with or without a trailing dot. */
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

    /** Java algorithm names stored as values in {@code algMap} must also resolve to the correct {@link Name}. */
    @Test
    void javaAlgorithmNamesMatch() {
        // The mapping is defined in TSIG’s static initializer; the known Java names are
        // "HmacMD5", "HmacSHA1", "HmacSHA224", "HmacSHA256", "HmacSHA384", "HmacSHA512".
        assertEquals(TSIG.HMAC_MD5,    TSIG.algorithmToName("HmacMD5"));
        assertEquals(TSIG.HMAC_SHA1,   TSIG.algorithmToName("HmacSHA1"));
        assertEquals(TSIG.HMAC_SHA224, TSIG.algorithmToName("HmacSHA224"));
        assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
        assertEquals(TSIG.HMAC_SHA384, TSIG.algorithmToName("HmacSHA384"));
        assertEquals(TSIG.HMAC_SHA512, TSIG.algorithmToName("HmacSHA512"));
    }

    /** An unknown algorithm name must cause an {@link IllegalArgumentException}. */
    @Test
    void unknownAlgorithmThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TSIG.algorithmToName("unknown-algorithm"));
    }
}
```