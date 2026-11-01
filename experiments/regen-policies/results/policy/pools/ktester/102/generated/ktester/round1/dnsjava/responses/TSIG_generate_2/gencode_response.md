```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import javax.crypto.Mac;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit‑tests for {@link TSIG#generate(Message, byte[], int, TSIGRecord, boolean)}.
 *
 * <p>The public {@code generate} method decides whether to initialise an {@link Mac}
 * instance (via the private {@code initHmac()} method) based on the supplied {@code error}
 * code.  The observable effect is whether the resulting {@link TSIGRecord} contains a
 * non‑null, non‑empty signature.
 *
 * <p>These tests cover:
 *
 * <ul>
 *   <li>All three error codes that trigger HMAC creation (NOERROR, BADTIME, BADTRUNC).</li>
 *   <li>Various “bad” error codes that must skip HMAC creation.</li>
 *   <li>Behaviour with {@code null} arguments (Message, byte[]).</li>
 *   <li>Propagation of a {@link GeneralSecurityException} when HMAC initialisation fails.</li>
 * </ul>
 *
 * <p>Mockito is used to create lightweight {@link Message} and {@link TSIGRecord}
 * collaborators; no reflection is required because we validate the observable
 * contract (signature presence) rather than the private method call itself.
 */
class TSIG_generate_2_Test {

    /** A TSIG instance that uses a deterministic clock so that timestamps are stable. */
    private TSIG tsig;

    /** A simple query message used for the “happy‑path” tests. */
    private Message queryMessage;

    /** Wire format of {@link #queryMessage}. */
    private byte[] queryWire;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Fixed clock => deterministic timeSigned values.
        Clock fixedClock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        tsig = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678", fixedClock);

        // Build a minimal query message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        queryMessage.setTSIG(tsig);
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

    /* --------------------------------------------------------------------- */
    /*  Existing explicit tests (kept for documentation / readability)      */
    /* --------------------------------------------------------------------- */

    /**
     * When the error code indicates a successful or recoverable situation
     * (NOERROR, BADTIME, BADTRUNC) the TSIG implementation must initialise an HMAC
     * and embed a non‑empty signature in the resulting {@link TSIGRecord}.
     */
    @Test
    void generate_withGoodError_initialisesHmac_andProducesSignature() throws IOException {
        int error = Rcode.NOERROR;
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
        int error = Rcode.FORMERR; // any code other than NOERROR, BADTIME, BADTRUNC
        TSIGRecord result = tsig.generate(queryMessage, queryWire, error, null, true);

        assertNotNull(result, "generate should still return a TSIGRecord even for bad errors");
        assertEquals(error, result.getError(), "TSIGRecord.error must reflect the supplied error code");
        assertNull(result.getSignature(),
                "Signature must be null when HMAC is not initialised for the given error code");
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests covering the exhaustive JSON specification        */
    /* --------------------------------------------------------------------- */

    /** Provides all combinations where the error code *should* trigger HMAC creation. */
    private static Stream<Arguments> goodErrorProvider() {
        return Stream.of(
                // error, fullSignature, oldRecord (null or mock), byteArray description
                Arguments.of(Rcode.NOERROR, true, null, new byte[]{0x01, 0x02}),
                Arguments.of(Rcode.BADTIME, false, null, new byte[]{0x0A, 0x0B, 0x0C}),
                Arguments.of(Rcode.BADTRUNC, true, null, new byte[]{}),
                Arguments.of(Rcode.NOERROR, false, mock(TSIGRecord.class), new byte[]{0x01}),
                Arguments.of(Rcode.BADTIME, true, mock(TSIGRecord.class), new byte[]{0x11, 0x22, 0x33}),
                Arguments.of(Rcode.BADTRUNC, false, mock(TSIGRecord.class), new byte[]{(byte) 0xAA, (byte) 0xBB}),
                // edge: empty payload but good error
                Arguments.of(Rcode.NOERROR, false, null, new byte[]{})
        );
    }

    /** Provides error codes that must *not* initialise an HMAC. */
    private static Stream<Arguments> badErrorProvider() {
        return Stream.of(
                Arguments.of(Rcode.FORMERR, false, null, new byte[]{0x55, 0x66, 0x77}),
                Arguments.of(Rcode.FORMERR, true, mock(TSIGRecord.class), new byte[]{0x55}),
                Arguments.of(Rcode.SERVFAIL, true, null, new byte[]{0x01, 0x02, 0x03})
        );
    }

    /** Provides null‑argument edge cases. */
    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(
                // null byte array with a good error (should NPE)
                Arguments.of(mock(Message.class), null, Rcode.NOERROR, null, true, NullPointerException.class),
                // null byte array with a bad error (still NPE)
                Arguments.of(mock(Message.class), null, Rcode.BADTIME, null, true, NullPointerException.class),
                // null Message (regardless of other args) – NPE
                Arguments.of(null, new byte[]{0x01, 0x02}, Rcode.NOERROR, null, true, NullPointerException.class)
        );
    }

    /** Provides a case where HMAC initialisation fails (invalid algorithm). */
    private static Stream<Arguments> initHmacFailureProvider() {
        return Stream.of(
                Arguments.of(Rcode.NOERROR, true, null, new byte[]{0x0A, 0x0B})
        );
    }

    @ParameterizedTest(name = "[{index}] goodError={0}, fullSig={1}, old={2}")
    @MethodSource("goodErrorProvider")
    void generate_goodError_createsSignature(int error, boolean fullSignature,
                                               TSIGRecord old, byte[] payload) throws IOException {
        // Arrange: mock Message – its internals are not needed for the signature check.
        Message mockMsg = mock(Message.class);
        // Act
        TSIGRecord result = tsig.generate(mockMsg, payload, error, old, fullSignature);
        // Assert
        assertNotNull(result, "Resulting TSIGRecord must not be null");
        assertEquals(error, result.getError(), "Error code must be propagated");
        assertNotNull(result.getSignature(),
                "Signature must be non‑null when error triggers HMAC creation");
        assertTrue(result.getSignature().length > 0,
                "Signature byte array must contain data");
    }

    @ParameterizedTest(name = "[{index}] badError={0}, fullSig={1}, old={2}")
    @MethodSource("badErrorProvider")
    void generate_badError_skipsHmac_andSignatureIsNull(int error, boolean fullSignature,
                                                        TSIGRecord old, byte[] payload) throws IOException {
        Message mockMsg = mock(Message.class);
        TSIGRecord result = tsig.generate(mockMsg, payload, error, old, fullSignature);
        assertNotNull(result, "Resulting TSIGRecord must not be null even for bad errors");
        assertEquals(error, result.getError(), "Error code must be propagated");
        assertNull(result.getSignature(),
                "Signature must be null when HMAC is not initialised for error code " + error);
    }

    @ParameterizedTest(name = "[{index}] null argument case")
    @MethodSource("nullArgumentProvider")
    void generate_nullArguments_throwNPE(Message msg, byte[] payload, int error,
                                         TSIGRecord old, boolean fullSignature,
                                         Class<? extends Throwable> expected) {
        // The TSIG instance used here is the same as the one created in @BeforeEach.
        assertThrows(expected, () -> tsig.generate(msg, payload, error, old, fullSignature));
    }

    @Test
    void generate_initHmacThrows_propagatesException() {
        // Use an invalid algorithm name – initHmac() will try to obtain a Mac instance
        // for a non‑existent algorithm and therefore throw a GeneralSecurityException.
        Clock fixedClock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        TSIG brokenTsig = new TSIG(Name.fromString("invalid-alg."), "example.", "key", fixedClock);

        Message mockMsg = mock(Message.class);
        byte[] payload = new byte[]{0x0A, 0x0B};

        // The error code is one that forces HMAC creation.
        assertThrows(GeneralSecurityException.class,
                () -> brokenTsig.generate(mockMsg, payload, Rcode.NOERROR, null, false));
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods (if any)                                            */
    /* --------------------------------------------------------------------- */

    // No additional helpers needed – the test logic is straightforward.
}
```