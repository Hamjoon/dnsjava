```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
 * The tests verify that the computed length matches the expected value for a
 * selection of algorithms and that the difference between two algorithms
 * corresponds exactly to the difference in their MAC sizes.
 */
class TSIG_recordLength_Test {

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

    @Test
    void recordLengthMatchesFormulaForMD5() throws Exception {
        Name keyName = Name.fromString("example.");
        TSIG tsig = new TSIG(TSIG.HMAC_MD5, keyName, "12345678");
        int macLen = algLengthMap.get(TSIG.HMAC_MD5);
        int expected = expectedRecordLength(keyName, TSIG.HMAC_MD5, macLen);
        assertEquals(expected, tsig.recordLength(),
                "recordLength for HMAC‑MD5 does not match the expected formula");
    }

    @Test
    void recordLengthMatchesFormulaForSHA256() throws Exception {
        Name keyName = Name.fromString("example.");
        TSIG tsig = new TSIG(TSIG.HMAC_SHA256, keyName, "12345678");
        int macLen = algLengthMap.get(TSIG.HMAC_SHA256);
        int expected = expectedRecordLength(keyName, TSIG.HMAC_SHA256, macLen);
        assertEquals(expected, tsig.recordLength(),
                "recordLength for HMAC‑SHA256 does not match the expected formula");
    }

    @Test
    void recordLengthDifferenceReflectsMacSizeDifference() throws Exception {
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
    void recordLengthIsIndependentOfKeyBytes() throws Exception {
        Name keyName = Name.fromString("example.");
        // Two different keys for the same algorithm/name
        TSIG tsig1 = new TSIG(TSIG.HMAC_SHA1, keyName, "AAAAAAAA");
        TSIG tsig2 = new TSIG(TSIG.HMAC_SHA1, keyName, "BBBBBBBB");

        assertEquals(tsig1.recordLength(), tsig2.recordLength(),
                "recordLength should not depend on the actual key material");
    }

    @Test
    void recordLengthWorksWithShortAlgorithmName() throws Exception {
        // Use the shortened algorithm name "hmac-sha256." which is also a constant in TSIG
        Name keyName = Name.fromString("example.");
        TSIG tsig = new TSIG(TSIG.HMAC_SHA256, keyName, "12345678");
        int macLen = algLengthMap.get(TSIG.HMAC_SHA256);
        int expected = expectedRecordLength(keyName, TSIG.HMAC_SHA256, macLen);
        assertEquals(expected, tsig.recordLength(),
                "recordLength should be correct for the short algorithm name constant");
    }
}
```