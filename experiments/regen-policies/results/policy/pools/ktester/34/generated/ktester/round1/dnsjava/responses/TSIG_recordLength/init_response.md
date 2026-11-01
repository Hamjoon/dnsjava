```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.SecureRandom;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TSIG#recordLength()}.
 *
 * <p>The method is a pure calculation based on the lengths of the {@code Name} objects
 * supplied to the {@link TSIG} constructor. The expected formula (derived from the source) is:
 *
 * <pre>
 * recordLength = name.length() + alg.length() + 48
 * </pre>
 *
 * The tests construct {@link TSIG} instances with known {@code Name} values and verify that
 * {@code recordLength()} returns the correct result.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_recordLength_Test {

    /** A dummy key used for all TSIG instances – its content is irrelevant for recordLength(). */
    private static final String DUMMY_KEY = "aGVsbG93b3JsZA=="; // base64 for "helloworld"

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
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

    /**
     * Verify the calculation for a simple, fully‑qualified name "example."
     * and the standard HMAC‑SHA256 algorithm name.
     */
    @Test
    void recordLength_simpleName() throws TextParseException {
        Name alg = TSIG.HMAC_SHA256;                     // "hmac-sha256."
        Name name = Name.fromString("example.");        // "example."
        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);

        // Expected: name.length() = 9, alg.length() = 12, constant part = 48
        int expected = 9 + 12 + 48;
        assertEquals(expected, tsig.recordLength(),
                "recordLength() should equal name.length() + alg.length() + 48");
    }

    /**
     * Verify the calculation for a multi‑label name "a.example."
     * and the same algorithm.
     */
    @Test
    void recordLength_multiLabelName() throws TextParseException {
        Name alg = TSIG.HMAC_SHA256;                     // "hmac-sha256."
        Name name = Name.fromString("a.example.");      // "a.example."
        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);

        // Expected: name.length() = 11 (1+1 + 1+7 + 1), alg.length() = 12, constant part = 48
        int expected = 11 + 12 + 48;
        assertEquals(expected, tsig.recordLength(),
                "recordLength() should correctly handle multi‑label names");
    }

    /**
     * Verify that the calculation works with a different algorithm name
     * (HMAC‑MD5) and a longer domain name.
     */
    @Test
    void recordLength_differentAlgorithmAndLongName() throws TextParseException {
        Name alg = TSIG.HMAC_MD5;                        // "HMAC-MD5.SIG-ALG.REG.INT."
        Name name = Name.fromString("sub.domain.example."); // three labels
        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);

        // Compute expected lengths using the Name API
        int nameLen = name.length(); // wire length of "sub.domain.example."
        int algLen = alg.length();   // wire length of "HMAC-MD5.SIG-ALG.REG.INT."
        int expected = nameLen + algLen + 48;

        assertEquals(expected, tsig.recordLength(),
                "recordLength() must use the actual Name lengths for any algorithm");
    }

    /**
     * Ensure that the method does not depend on the key material.
     * Two TSIG objects with the same names but different keys must yield the same length.
     */
    @Test
    void recordLength_keyIrrelevant() throws TextParseException {
        Name alg = TSIG.HMAC_SHA256;
        Name name = Name.fromString("example.");

        TSIG tsig1 = new TSIG(alg, name, DUMMY_KEY);
        TSIG tsig2 = new TSIG(alg, name, "c2VjcmV0a2V5"); // different base64 key

        assertEquals(tsig1.recordLength(), tsig2.recordLength(),
                "recordLength() must be independent of the secret key");
    }
}
```