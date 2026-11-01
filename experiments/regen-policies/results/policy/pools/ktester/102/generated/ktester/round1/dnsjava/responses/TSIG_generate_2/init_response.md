```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.Mac;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Basic unit‑tests for {@link TSIG#generate(Message, byte[], int, TSIGRecord, boolean)}.
 *
 * <p>The method under test decides whether to initialise an HMAC instance based on the
 * {@code error} argument.  When the error is {@link Rcode#NOERROR},
 * {@link Rcode#BADTIME} or {@link Rcode#BADTRUNC} an {@link Mac} is created via
 * {@code initHmac()}; otherwise {@code null} is passed to the private overload.
 *
 * <p>These tests verify the observable behaviour of the public {@code generate} method:
 *
 * <ul>
 *   <li>When a “good” error code is supplied the returned {@link TSIGRecord} contains a
 *       non‑empty signature (the HMAC was applied).</li>
 *   <li>When a “bad” error code is supplied the returned {@link TSIGRecord} contains a
 *       {@code null} signature (the HMAC step was skipped).</li>
 * </ul>
 *
 * <p>Only public APIs are used – no reflection on private members – in accordance with the
 * requirements.
 */
class TSIG_generate_2_Test {

    /** A TSIG instance that uses a deterministic clock so that the generated timestamps are stable. */
    private TSIG tsig;

    /** A simple query message used for all tests. */
    private Message queryMessage;

    /** The byte representation of {@link #queryMessage}. */
    private byte[] queryWire;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Use a fixed clock (UTC epoch) so that the generated TSIGRecord has a predictable
        // timeSigned value – this makes the test deterministic and avoids flaky timing issues.
        Clock fixedClock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        tsig = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678", fixedClock);

        // Build a minimal query message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        queryMessage.setTSIG(tsig);               // attach the key so that toWire() includes a TSIG
        queryWire = queryMessage.toWire(Message.MAXLENGTH);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * When the error code indicates a successful or recoverable situation
     * (NOERROR, BADTIME, BADTRUNC) the TSIG implementation must initialise an HMAC
     * and embed a non‑empty signature in the resulting {@link TSIGRecord}.
     */
    @Test
    void generate_withGoodError_initialisesHmac_andProducesSignature() throws IOException {
        // Use a “good” error code – NOERROR.
        int error = Rcode.NOERROR;

        // The old TSIGRecord is not relevant for this test; pass {@code null}.
        TSIGRecord result = tsig.generate(queryMessage, queryWire, error, null, true);

        assertNotNull(result, "generate should return a TSIGRecord");
        assertEquals(error, result.getError(), "TSIGRecord.error must match the supplied error code");
        assertNotNull(result.getSignature(), "Signature must be present when HMAC is initialised");
        assertTrue(result.getSignature().length > 0,
                "Signature byte array should not be empty when HMAC is used");
    }

    /**
     * When the error code is something other than the three accepted values
     * (e.g. FORMERR) the implementation must *not* initialise an HMAC.
     * In that case the generated {@link TSIGRecord} contains a {@code null}
     * signature.
     */
    @Test
    void generate_withBadError_skipsHmac_andProducesEmptySignature() throws IOException {
        // Use an error code that is not one of the three that trigger HMAC creation.
        int error = Rcode.FORMERR; // any code other than NOERROR, BADTIME, BADTRUNC

        TSIGRecord result = tsig.generate(queryMessage, queryWire, error, null, true);

        assertNotNull(result, "generate should still return a TSIGRecord even for bad errors");
        assertEquals(error, result.getError(), "TSIGRecord.error must reflect the supplied error code");
        // According to the current implementation a bad error leads to a null signature.
        // The public contract does not guarantee a non‑null value, so we assert that it is null.
        assertNull(result.getSignature(),
                "Signature must be null when HMAC is not initialised for the given error code");
    }
}
```