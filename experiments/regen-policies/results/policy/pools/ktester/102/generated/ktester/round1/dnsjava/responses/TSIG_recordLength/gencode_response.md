```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Map;
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
 * Unit tests for {@link TSIG#recordLength()}.
 *
 * The method computes the size of a TSIG record that will be added to a DNS message.
 * The formula (as defined in the source) is:
 *
 * <pre>
 * name.length()                // the owner name of the TSIG key
 * + 10                         // fixed fields (type, class, ttl, rdlength)
 * + alg.length()               // algorithm name length
 * + 8                          // time signed (48‑bit) + fudge (16‑bit)
 * + 2                          // MAC length field (2 bytes)
 * + algLengthMap.get(alg)      // MAC size for the algorithm
 * + 4                          // original message id + error code (2+2)
 * + 8                          // error length (2) + max error field (6)
 * </pre>
 *
 * The tests verify the formula against a collection of concrete cases (including
 * edge‑cases such as null arguments and unknown algorithms) and also keep a few
 * explicit tests for readability.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_recordLength_Test {

    /** The private static map that holds the MAC size for each algorithm. */
    private static Map<Name, Integer> algLengthMap; // accessed via reflection

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // The map is private static final; use reflection to obtain it once for all tests.
        Field mapField = TSIG.class.getDeclaredField("algLengthMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Name, Integer> map = (Map<Name, Integer>) mapField.get(null);
        algLengthMap = map;
        assertNotNull(algLengthMap, "algLengthMap should be initialised");
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Helper that reproduces the formula used inside {@code TSIG.recordLength()}.
     */
    private static int expectedRecordLength(Name keyName, Name algName, int macLength) {
        int nameLen = keyName.length();
        int algLen = algName.length();
        // 10 (type, class, ttl, rdlength) + 8 (time signed + fudge) + 2 (MAC length field)
        // + macLength + 4 (original id + error) + 8 (error length + max error field)
        return nameLen + 10 + algLen + 8 + 2 + macLength + 4 + 8;
    }

    /* --------------------------------------------------------------------- */
    /*  Existing explicit tests (kept for documentation purposes)          */
    /* --------------------------------------------------------------------- */

    @Test
    void recordLengthMatchesFormulaForMD5() {
        Name keyName = Name.fromString("example.");
        TSIG tsig = new TSIG(TSIG.HMAC_MD5, keyName, "12345678");
        int macLen = algLengthMap.get(TSIG.HMAC_MD5);
        int expected = expectedRecordLength(keyName, TSIG.HMAC_MD5, macLen);
        assertEquals(expected, tsig.recordLength(),
                "recordLength for HMAC‑MD5 does not match the expected formula");
    }

    @Test
    void recordLengthMatchesFormulaForSHA256() {
        Name keyName = Name.fromString("example.");
        TSIG tsig = new TSIG(TSIG.HMAC_SHA256, keyName, "12345678");
        int macLen = algLengthMap.get(TSIG.HMAC_SHA256);
        int expected = expectedRecordLength(keyName, TSIG.HMAC_SHA256, macLen);
        assertEquals(expected, tsig.recordLength(),
                "recordLength for HMAC‑SHA256 does not match the expected formula");
    }

    @Test
    void recordLengthDifferenceReflectsMacSizeDifference() {
        Name keyName = Name.fromString("example.");

        TSIG md5 = new TSIG(TSIG.HMAC_MD5, keyName, "12345678");
        TSIG sha256 = new TSIG(TSIG.HMAC_SHA256, keyName, "12345678");

        int md5Len = md5.recordLength();
        int sha256Len = sha256.recordLength();

        int md5Mac = algLengthMap.get(TSIG.HMAC_MD5);
        int sha256Mac = algLengthMap.get(TSIG.HMAC_SHA256);

        assertEquals(sha256Mac - md5Mac, sha256Len - md5Len,
                "Difference between record lengths should equal the difference in MAC sizes");
    }

    @Test
    void recordLengthIsIndependentOfKeyBytes() {
        Name keyName = Name.fromString("example.");
        // Two different keys for the same algorithm/name
        TSIG tsig1 = new TSIG(TSIG.HMAC_SHA1, keyName, "AAAAAAAA");
        TSIG tsig2 = new TSIG(TSIG.HMAC_SHA1, keyName, "BBBBBBBB");

        assertEquals(tsig1.recordLength(), tsig2.recordLength(),
                "recordLength should not depend on the actual key material");
    }

    @Test
    void recordLengthWorksWithShortAlgorithmName() {
        // Use the shortened algorithm name "hmac-sha256." which is also a constant in TSIG
        Name keyName = Name.fromString("example.");
        TSIG tsig = new TSIG(TSIG.HMAC_SHA256, keyName, "12345678");
        int macLen = algLengthMap.get(TSIG.HMAC_SHA256);
        int expected = expectedRecordLength(keyName, TSIG.HMAC_SHA256, macLen);
        assertEquals(expected, tsig.recordLength(),
                "recordLength should be correct for the short algorithm name constant");
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests derived from the JSON test matrix               */
    /* --------------------------------------------------------------------- */

    /**
     * Supplies the test matrix defined in the JSON document.
     *
     * Each {@link Arguments} instance contains:
     * <ul>
     *   <li>name string (or special placeholder for a mocked 255‑byte name)</li>
     *   <li>algorithm string (or {@code null})</li>
     *   <li>expected result – either an {@link Integer} length or the
     *        {@link Class} of an expected exception</li>
     *   <li>human readable description (used only for readability)</li>
     * </ul>
     */
    private static Stream<Arguments> recordLengthCases() {
        return Stream.of(
                // Standard case with MD5 algorithm and a typical domain name.
                Arguments.of("example.com.", "HMAC-MD5.SIG-ALG.REG.INT.", 87, "Standard MD5"),
                // Shortest possible domain name with SHA‑256.
                Arguments.of("a.", "hmac-sha256.", 80, "Shortest name + SHA‑256"),
                // Three‑label name with truncated SHA‑256‑128.
                Arguments.of("longdomainname.example.org.", "hmac-sha256-128.", 93,
                        "Three‑label name + SHA‑256‑128"),
                // GSS‑TSIG (MAC length 0).
                Arguments.of("example.com.", "gss-tsig.", 55, "GSS‑TSIG (zero MAC)"),
                // Maximum‑size DNS name (255‑byte wire format) with SHA‑1.
                Arguments.of("<255-byte-wire-format-name>", "hmac-sha1.", 318,
                        "Maximum‑size name + SHA‑1"),
                // Null name → NPE.
                Arguments.of(null, "hmac-sha256.", NullPointerException.class,
                        "Null name should throw NPE"),
                // Null algorithm → NPE.
                Arguments.of("example.com.", null, NullPointerException.class,
                        "Null algorithm should throw NPE"),
                // Unknown algorithm → NPE (map returns null → autounboxing NPE).
                Arguments.of("example.com.", "unknown-alg.", NullPointerException.class,
                        "Unknown algorithm should throw NPE")
        );
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("recordLengthCases")
    void recordLengthParameterized(String nameStr,
                                   String algStr,
                                   Object expected,
                                   String description) {
        // -----------------------------------------------------------------
        // Build the Name instance for the key name.
        // -----------------------------------------------------------------
        Name name;
        if (nameStr == null) {
            name = null;
        } else if ("<255-byte-wire-format-name>".equals(nameStr)) {
            // Use a Mockito mock that pretends to be a 255‑byte name.
            name = mock(Name.class);
            when(name.length()).thenReturn(255);
        } else {
            name = Name.fromString(nameStr);
        }

        // -----------------------------------------------------------------
        // Build the algorithm Name instance.
        // -----------------------------------------------------------------
        Name alg;
        if (algStr == null) {
            alg = null;
        } else {
            // Map the string to the corresponding constant when possible.
            switch (algStr) {
                case "HMAC-MD5.SIG-ALG.REG.INT.":
                    alg = TSIG.HMAC_MD5;
                    break;
                case "hmac-sha1.":
                    alg = TSIG.HMAC_SHA1;
                    break;
                case "hmac-sha256.":
                    alg = TSIG.HMAC_SHA256;
                    break;
                case "hmac-sha256-128.":
                    alg = TSIG.HMAC_SHA256_128;
                    break;
                case "hmac-sha384.":
                    alg = TSIG.HMAC_SHA384;
                    break;
                case "hmac-sha384-192.":
                    alg = TSIG.HMAC_SHA384_192;
                    break;
                case "hmac-sha512.":
                    alg = TSIG.HMAC_SHA512;
                    break;
                case "hmac-sha512-256.":
                    alg = TSIG.HMAC_SHA512_256;
                    break;
                case "gss-tsig.":
                    alg = TSIG.GSS_TSIG;
                    break;
                default:
                    // For an unknown algorithm we create a Name instance from the literal.
                    alg = Name.fromString(algStr);
                    break;
            }
        }

        // -----------------------------------------------------------------
        // Execute and verify.
        // -----------------------------------------------------------------
        if (expected instanceof Integer) {
            // Normal case – we expect a concrete length.
            int expectedLen = (Integer) expected;
            TSIG tsig = new TSIG(alg, name, "dummy-key");
            int actual = tsig.recordLength();
            assertEquals(expectedLen, actual,
                    () -> "recordLength mismatch for case: " + description);
        } else if (expected instanceof Class) {
            // Exception case – we expect the given exception type.
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            TSIG tsig = new TSIG(alg, name, "dummy-key");
            assertThrows(exClass, tsig::recordLength,
                    () -> "Expected " + exClass.getSimpleName() + " for case: " + description);
        } else {
            fail("Test data error: unexpected expected value type");
        }
    }
}
```