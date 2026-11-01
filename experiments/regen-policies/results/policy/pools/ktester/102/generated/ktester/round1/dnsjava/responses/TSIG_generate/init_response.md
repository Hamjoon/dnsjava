```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Basic unit‑tests for {@link TSIG#generate(Message, byte[], int, TSIGRecord)}.
 *
 * <p>The tests use a deterministic {@link Clock} so that the {@code timeSigned}
 * field of the generated {@link TSIGRecord} is reproducible.
 *
 * <p>Only public APIs are exercised – no reflection or access to private members.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_generate_Test {

    /** TSIG instance that will be used for all tests. */
    private static TSIG tsig;

    /** Fixed clock used to make the generated TSIGRecord deterministic. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneId.of("UTC"));

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a TSIG key with HMAC‑SHA256 algorithm, name "example.", and a dummy key.
        // The key string is base64‑encoded; "12345678" is a short, valid base64 string.
        Name algorithm = TSIG.HMAC_SHA256;
        Name name = Name.fromString("example.");
        SecretKey key = new SecretKeySpec(base64.fromString("12345678"), "HmacSHA256");
        tsig = new TSIG(algorithm, name, key, FIXED_CLOCK);
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
     * Verify that {@code generate} returns a non‑null {@link TSIGRecord},
     * that the algorithm name matches the key's algorithm, and that the
     * signature length corresponds to the underlying HMAC‑SHA256 algorithm
     * (32 bytes). The {@code error} argument is also checked.
     */
    @Test
    void generateCreatesValidTSIGRecord() throws IOException {
        // Build a simple DNS query message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);

        // Convert the message to wire format – this is the byte[] argument for generate().
        byte[] wire = query.toWire(Message.MAXLENGTH);

        // No previous TSIG record (first message in a transaction) and no error.
        int errorCode = Rcode.NOERROR;
        TSIGRecord previous = null;

        // Invoke the method under test.
        TSIGRecord tsigRecord = tsig.generate(query, wire, errorCode, previous);

        // Basic sanity checks.
        assertNotNull(tsigRecord, "generate should return a TSIGRecord");
        assertEquals(TSIG.HMAC_SHA256, tsigRecord.getAlgorithm(), "Algorithm name should match the key");
        assertEquals(errorCode, tsigRecord.getError(), "Error code should be propagated");
        assertEquals(32, tsigRecord.getSignature().length, "SHA‑256 signature must be 32 bytes long");

        // The timeSigned field must be derived from the fixed clock.
        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), tsigRecord.getTimeSigned(),
                "timeSigned should be based on the fixed clock");
    }
}
```