```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.security.SecureRandom;
import java.util.Arrays;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TSIG#recordLength()}.
 *
 * The method is a pure calculation based on the lengths of the {@code Name} objects
 * supplied to the {@link TSIG} constructor.  The expected value is:
 *
 * <pre>
 *   name.length() + alg.length() + 48
 * </pre>
 *
 * (10 bytes for the signed time, 8 bytes for the fudge,
 * 18 bytes for the MAC length + MAC,
 * 4 bytes for the original ID and error,
 * 8 bytes for the error length and max error field.)
 *
 * The tests create {@link TSIG} instances with known {@code Name} values and verify that
 * {@code recordLength()} returns the correct result.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_recordLength_Test {

    /** Simple key material – the actual value is irrelevant for the length calculation. */
    private static final byte[] DUMMY_KEY = new byte[16];

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the dummy key with deterministic data.
        new SecureRandom().nextBytes(DUMMY_KEY);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
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
     * Verify the calculation for a typical domain name and the default HMAC‑SHA256 algorithm.
     */
    @Test
    void testRecordLengthTypical() throws Exception {
        Name alg = TSIG.HMAC_SHA256;                     // algorithm name constant
        Name name = Name.fromString("example.", Name.root); // "example." (7‑char label)

        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);
        int expected = name.length() + alg.length() + 48;
        assertEquals(expected, tsig.recordLength(),
                "recordLength() should equal name.length + alg.length + 48");
    }

    /**
     * Verify that the root name (\".\") is handled correctly.
     */
    @Test
    void testRecordLengthRootName() throws Exception {
        Name alg = TSIG.HMAC_MD5;                       // a different algorithm constant
        Name rootName = Name.fromString(".", Name.root); // the DNS root name

        TSIG tsig = new TSIG(alg, rootName, DUMMY_KEY);
        int expected = rootName.length() + alg.length() + 48;
        assertEquals(expected, tsig.recordLength(),
                "recordLength() with the root name should still follow the formula");
    }

    /**
     * Verify that varying the algorithm name length changes the result accordingly.
     */
    @Test
    void testRecordLengthDifferentAlgorithmLengths() throws Exception {
        // Two algorithms with noticeably different name lengths.
        Name algShort = TSIG.HMAC_MD5;   // relatively short
        Name algLong  = TSIG.HMAC_SHA512; // longer

        Name name = Name.fromString("sub.example.", Name.root);

        TSIG tsigShort = new TSIG(algShort, name, DUMMY_KEY);
        TSIG tsigLong  = new TSIG(algLong,  name, DUMMY_KEY);

        int expectedShort = name.length() + algShort.length() + 48;
        int expectedLong  = name.length() + algLong.length()  + 48;

        assertEquals(expectedShort, tsigShort.recordLength(),
                "Short algorithm name should produce the correct length");
        assertEquals(expectedLong,  tsigLong.recordLength(),
                "Long algorithm name should produce the correct length");
        assertNotEquals(tsigShort.recordLength(), tsigLong.recordLength(),
                "Different algorithm name lengths must yield different recordLength values");
    }

    /**
     * Ensure that the calculation does not depend on the clock supplied to the constructor.
     */
    @Test
    void testRecordLengthIgnoresClock() throws Exception {
        Name alg = TSIG.HMAC_SHA1;
        Name name = Name.fromString("example.", Name.root);
        // Use a fixed clock – the value should be irrelevant.
        Clock fixedClock = Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC);
        TSIG tsig = new TSIG(alg, name, new javax.crypto.spec.SecretKeySpec(DUMMY_KEY, "HmacSHA1"), fixedClock);

        int expected = name.length() + alg.length() + 48;
        assertEquals(expected, tsig.recordLength(),
                "recordLength() must not be affected by the Clock instance");
    }
}
```