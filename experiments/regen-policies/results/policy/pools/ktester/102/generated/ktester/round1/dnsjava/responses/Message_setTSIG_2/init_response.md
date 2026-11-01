```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#setTSIG(TSIG, int, TSIGRecord)}.
 *
 * The tests verify that the public accessor {@link Message#getTSIG()} reflects the
 * {@code querytsig} argument passed to {@code setTSIG}.  The overload
 * {@link Message#setTSIG(TSIG)} is also exercised to ensure it delegates correctly.
 *
 * No private fields or methods are accessed directly; the behaviour is validated
 * solely through the public API.
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_2_Test {

    private Message message;
    private TSIG tsigKey;
    private TSIGRecord queryTsigRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a fresh Message for each test.
        message = new Message();

        // Build a TSIG key – algorithm, name and base‑64 encoded key.
        Name alg = Name.fromString("hmac-md5.sig-alg.reg.int.");
        Name name = Name.fromString("example.com.");
        // a short dummy key; the actual value is irrelevant for this test.
        tsigKey = new TSIG(alg, name, "YWJjZGVmZ2hpamtsbW5vcA=="); // "abcdefghijklmno" base64

        // Build a TSIGRecord that could be attached to a query.
        Name recordName = Name.fromString("example.com.");
        int dclass = DClass.IN;
        long ttl = 0L;
        Instant now = Instant.now();
        Duration fudge = Duration.ofSeconds(300);
        byte[] signature = new byte[]{0x01, 0x02};
        int originalID = 0;
        int error = Rcode.NOERROR;
        byte[] other = new byte[0];

        queryTsigRecord = new TSIGRecord(
                recordName,
                dclass,
                ttl,
                alg,
                now,
                fudge,
                signature,
                originalID,
                error,
                other);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void setTSIG_withAllParameters_setsQueryTSIG() {
        // GIVEN a Message and a fully‑populated TSIGRecord
        // WHEN setTSIG is invoked with a key, error code and the TSIGRecord
        message.setTSIG(tsigKey, Rcode.NOERROR, queryTsigRecord);

        // THEN the Message should expose the same TSIGRecord via getTSIG()
        assertSame(queryTsigRecord, message.getTSIG(),
                "Message#getTSIG() should return the TSIGRecord supplied to setTSIG");
    }

    @Test
    void setTSIG_withNullQueryTSIG_clearsTSIGRecord() {
        // GIVEN a Message that already has a TSIGRecord attached
        message.setTSIG(tsigKey, Rcode.NOERROR, queryTsigRecord);
        assertNotNull(message.getTSIG(), "Pre‑condition: TSIGRecord should be present");

        // WHEN setTSIG is called with a null queryTSIG
        message.setTSIG(tsigKey, Rcode.NOERROR, null);

        // THEN getTSIG() must return null
        assertNull(message.getTSIG(),
                "Message#getTSIG() should be null after setTSIG is called with a null queryTSIG");
    }

    @Test
    void setTSIG_overload_delegatesToThreeArgVersion() {
        // The overload setTSIG(TSIG) should behave as if it called
        // setTSIG(key, Rcode.NOERROR, null).

        // WHEN the overload is used
        message.setTSIG(tsigKey);

        // THEN no TSIGRecord is attached
        assertNull(message.getTSIG(),
                "Message#getTSIG() should be null when the one‑arg overload is used");
    }
}
```