```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional tests for {@link TSIG#apply(Message, int, TSIGRecord)} that cover
 * edge‑cases and error handling as described in the JSON test specification.
 *
 * <p>These tests are kept separate from the functional tests that verify a
 * successful signature/verification flow.</p>
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_3_Errors_Test {

    /** A TSIG key used for all tests. */
    private static TSIG tsigKey;

    /** A simple DNS query message used as the base for the tests. */
    private Message queryMessage;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the HMAC‑SHA256 algorithm, a dummy domain name and a short base‑64 key.
        tsigKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a minimal query:  www.example.  IN  A
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    /* -------------------------------------------------------------
     *  Group: apply_nullMessage
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] null Message should throw NPE")
    @MethodSource("nullMessageProvider")
    void apply_nullMessage(Message m, int error, TSIGRecord old) {
        assertThrows(NullPointerException.class,
                () -> tsigKey.apply(m, error, old),
                "Calling apply with a null Message must throw NullPointerException");
    }

    static Stream<Arguments> nullMessageProvider() {
        return Stream.of(
                // case 1: all nulls
                Arguments.of(null, 0, null),
                // case 2: null Message but non‑null old TSIGRecord
                Arguments.of(null, 0,
                        new TSIGRecord(Name.fromString("example.com."), TSIG.HMAC_SHA256,
                                0, new byte[0], 0, 0, 0, new byte[0]))
        );
    }

    /* -------------------------------------------------------------
     *  Group: apply_invalidErrorCode
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] error={1} should throw IllegalArgumentException")
    @MethodSource("invalidErrorCodeProvider")
    void apply_invalidErrorCode(Message m, int error, TSIGRecord old) {
        assertThrows(IllegalArgumentException.class,
                () -> tsigKey.apply(m, error, old),
                "Negative or out‑of‑range error codes must cause IllegalArgumentException");
    }

    static Stream<Arguments> invalidErrorCodeProvider() {
        return Stream.of(
                // negative error code
                Arguments.of(new Message(), -1, null),
                // error code far beyond defined TSIG errors (RFC 2845 defines up to 16)
                Arguments.of(new Message(), 9999, null)
        );
    }

    /* -------------------------------------------------------------
     *  Group: apply_unsupportedAlgorithm
     * ------------------------------------------------------------- */
    @Test
    void apply_unsupportedAlgorithm_throwsGeneralSecurityException() {
        // Build a TSIGRecord that references an unknown algorithm name.
        TSIGRecord unsupported = new TSIGRecord(
                Name.fromString("example.com."),
                Name.fromString("unsupported-alg."), // algorithm not recognised by TSIG
                0, new byte[0], 0, 0, 0, new byte[0]);

        Message m = new Message(); // empty message
        assertThrows(GeneralSecurityException.class,
                () -> tsigKey.apply(m, Rcode.NOERROR, unsupported),
                "Using an unsupported algorithm should raise GeneralSecurityException");
    }

    /* -------------------------------------------------------------
     *  Group: apply_messageAlreadySigned
     * ------------------------------------------------------------- */
    @Test
    void apply_messageAlreadySigned_throwsIllegalStateException() throws IOException {
        // First sign the message – this adds a TSIG record.
        tsigKey.apply(queryMessage, Rcode.NOERROR, null);
        assertNotNull(queryMessage.getTSIG(), "Message should contain a TSIG after first apply");

        // Attempt to apply a second TSIG to the same message.
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> tsigKey.apply(queryMessage, Rcode.NOERROR, null),
                "Applying TSIG to a message that already has a TSIG must throw IllegalStateException");

        // Optional sanity check on the exception message (implementation‑specific).
        assertTrue(ex.getMessage().toLowerCase().contains("already signed"),
                "Exception message should indicate the message is already signed");
    }
}
```