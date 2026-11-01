```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#apply(Message, TSIGRecord, boolean)}.
 *
 * <p>The tests verify that the method adds a TSIG record to a {@link Message},
 * that the record contains the expected algorithm and a signature of the correct length,
 * and that it works both with a full‑signature (including the original ID) and without.
 *
 * <p>All objects are created with a fixed {@link Clock} so that the {@code timeSigned}
 * field of the generated {@link TSIGRecord} is deterministic.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_2_Test {

    /** Fixed instant used for all TSIG operations in the test suite. */
    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

    /** The clock that always returns {@link #FIXED_INSTANT}. */
    private static Clock fixedClock;

    /** A TSIG instance that uses HMAC‑SHA256 and a deterministic key. */
    private static TSIG tsig;

    /** Algorithm name constant for HMAC‑SHA256 (defined in {@link TSIG}). */
    private static Name algName;

    /** Owner name for the TSIG key (e.g. {@code example.}). */
    private static Name keyName;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // deterministic algorithm/name/key
        algName = TSIG.HMAC_SHA256;                         // algorithm constant
        keyName = Name.fromString("example.");              // key owner name

        // 32‑byte key for HMAC‑SHA256 (all zeroes – content does not matter for the test)
        byte[] rawKey = new byte[32];
        SecretKey secretKey = new SecretKeySpec(rawKey, "HmacSHA256");

        // fixed clock → deterministic timeSigned field
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

        // Build the TSIG object using the constructor that accepts a Clock
        tsig = new TSIG(algName, keyName, secretKey, fixedClock);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Verify that {@code apply(message, old, true)} (full signature) adds a TSIG record
     * with the expected algorithm and a 32‑byte HMAC‑SHA256 signature.
     */
    @Test
    void applyAddsFullSignatureTsigRecord() {
        // --------------------------------------------------------------------
        // given – a signed query that will serve as the “old” TSIGRecord
        // --------------------------------------------------------------------
        Message query = new Message(0);
        // Sign the query with a full signature (the boolean flag is true)
        assertDoesNotThrow(() -> tsig.apply(query, null, true));
        TSIGRecord old = query.getTSIG();
        assertNotNull(old, "The query must contain a TSIG record after signing");

        // --------------------------------------------------------------------
        // when – we apply TSIG to a fresh response message using the old record
        // --------------------------------------------------------------------
        Message response = new Message(0);
        assertDoesNotThrow(() -> tsig.apply(response, old, true));

        // --------------------------------------------------------------------
        // then – the response must contain a TSIG record with expected properties
        // --------------------------------------------------------------------
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response should contain a TSIG record after apply");
        assertEquals(algName, respTsig.getAlgorithm(), "Algorithm name must match the TSIG key");
        assertEquals(keyName, respTsig.getName(), "Owner name must match the TSIG key");

        // Signature length for HMAC‑SHA256 is 32 bytes
        assertNotNull(respTsig.getSignature(), "Signature must not be null");
        assertEquals(32, respTsig.getSignature().length,
                "Signature length for HMAC‑SHA256 should be 32 bytes");

        // The timeSigned field must be the fixed instant (rounded to seconds)
        assertEquals(FIXED_INSTANT.getEpochSecond(), respTsig.getTimeSigned().getEpochSecond(),
                "timeSigned must be derived from the fixed clock");
    }

    /**
     * Verify that {@code apply(message, old, false)} (no full signature) also adds a TSIG
     * record with the correct algorithm and signature length.
     */
    @Test
    void applyAddsPartialSignatureTsigRecord() {
        // --------------------------------------------------------------------
        // given – a signed query (full signature) to obtain an old TSIGRecord
        // --------------------------------------------------------------------
        Message query = new Message(0);
        assertDoesNotThrow(() -> tsig.apply(query, null, true));
        TSIGRecord old = query.getTSIG();
        assertNotNull(old, "Old TSIGRecord must be present");

        // --------------------------------------------------------------------
        // when – we apply TSIG to a new message but request a partial signature
        // --------------------------------------------------------------------
        Message response = new Message(0);
        assertDoesNotThrow(() -> tsig.apply(response, old, false));

        // --------------------------------------------------------------------
        // then – the response must contain a valid TSIG record
        // --------------------------------------------------------------------
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response should contain a TSIG record");
        assertEquals(algName, respTsig.getAlgorithm(), "Algorithm name must match the TSIG key");
        assertEquals(keyName, respTsig.getName(), "Owner name must match the TSIG key");
        assertEquals(32, respTsig.getSignature().length,
                "Signature length for HMAC‑SHA256 should be 32 bytes");
        // The timestamp is still derived from the fixed clock, regardless of the flag
        assertEquals(FIXED_INSTANT.getEpochSecond(), respTsig.getTimeSigned().getEpochSecond(),
                "timeSigned must be derived from the fixed clock");
    }
}
```