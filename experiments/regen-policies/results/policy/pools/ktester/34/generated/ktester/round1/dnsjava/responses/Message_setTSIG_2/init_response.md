```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#setTSIG(TSIG, int, TSIGRecord)} and its overload
 * {@link Message#setTSIG(TSIG)}.
 *
 * The tests verify that the public {@link Message#getTSIG()} method reflects the
 * values supplied to {@code setTSIG}.  No private fields or methods are accessed
 * directly – the verification is performed solely through the public API.
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_2_Test {

    private Message message;
    private TSIG tsigKey;
    private TSIGRecord tsigRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a fresh Message for each test.
        message = new Message();

        // Build a TSIG key – algorithm “hmac-md5.sig-alg.reg.int.” is the
        // default algorithm used in many examples.
        Name alg = Name.fromString("hmac-md5.sig-alg.reg.int.");
        Name name = Name.fromString("example.com.");
        // The key string is arbitrary; the library decodes it from base64.
        tsigKey = new TSIG(alg, name, "YWJjZGVmZ2hpamtsbW5vcA=="); // “abcdefghijklmnop” in base64

        // Build a TSIGRecord that could be attached to a query.
        tsigRecord = new TSIGRecord(
                name,                     // owner name
                DClass.IN,                // class
                0L,                       // ttl (ignored for TSIG)
                alg,                      // algorithm name
                Instant.now(),            // time signed
                Duration.ofSeconds(300), // fudge
                new byte[]{0x01, 0x02},   // dummy signature
                0x1234,                   // original ID
                Rcode.NOERROR,            // error code
                new byte[0]               // other data
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verify that the three‑argument {@code setTSIG} stores the supplied
     * TSIG key, error code and TSIGRecord.  The public {@link Message#getTSIG()}
     * method should return the TSIGRecord that was passed.
     */
    @Test
    void setTSIG_threeArguments_storesValues() {
        // Act
        message.setTSIG(tsigKey, Rcode.BADKEY, tsigRecord);

        // Assert – the TSIGRecord we supplied must be returned by getTSIG()
        assertSame(tsigRecord, message.getTSIG(),
                "Message#getTSIG() should return the TSIGRecord supplied to setTSIG");
    }

    /**
     * Verify the overload {@code setTSIG(TSIG)} delegates to the three‑argument
     * version with {@code Rcode.NOERROR} and a {@code null} TSIGRecord.
     */
    @Test
    void setTSIG_oneArgument_delegatesCorrectly() {
        // Act – use the overload that only supplies the key
        message.setTSIG(tsigKey);

        // Assert – because the overload passes a null TSIGRecord, getTSIG()
        // must return null.
        assertNull(message.getTSIG(),
                "Message#getTSIG() should be null when setTSIG(TSIG) is used without a TSIGRecord");
    }

    /**
     * Ensure that calling {@code setTSIG} twice updates the stored TSIGRecord.
     */
    @Test
    void setTSIG_calledMultipleTimes_updatesRecord() {
        // First call with a record
        message.setTSIG(tsigKey, Rcode.NOERROR, tsigRecord);
        assertSame(tsigRecord, message.getTSIG(),
                "First call should store the initial TSIGRecord");

        // Create a second, different TSIGRecord
        TSIGRecord secondRecord = new TSIGRecord(
                Name.fromString("other.example.com."),
                DClass.IN,
                0L,
                Name.fromString("hmac-sha256."),
                Instant.now(),
                Duration.ofSeconds(300),
                new byte[]{0x0A, 0x0B},
                0x5678,
                Rcode.NOERROR,
                new byte[0]
        );

        // Second call replaces the previous record
        message.setTSIG(tsigKey, Rcode.NOERROR, secondRecord);
        assertSame(secondRecord, message.getTSIG(),
                "Second call should replace the previously stored TSIGRecord");
    }

    /**
     * Verify that passing {@code null} for the TSIGRecord does not cause a
     * {@link NullPointerException} and that {@code getTSIG()} returns {@code null}.
     */
    @Test
    void setTSIG_nullRecord_returnsNullTSIG() {
        // Act – explicitly pass a null TSIGRecord
        message.setTSIG(tsigKey, Rcode.NOERROR, null);

        // Assert – the public getter must reflect the null value.
        assertNull(message.getTSIG(),
                "Message#getTSIG() should be null when setTSIG is called with a null TSIGRecord");
    }
}
```