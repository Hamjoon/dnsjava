```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Message#isSigned()}.
 *
 * <p>The {@code isSigned()} method returns {@code true} when the internal {@code tsigState}
 * indicates that a TSIG record has been added, verified or that verification has failed.
 * The state is modified only through the public {@code setTSIG} APIs, therefore the tests
 * exercise those public entry points rather than accessing private fields directly.
 */
@ExtendWith(MockitoExtension.class)
class Message_isSigned_Test {

    /** The {@link Message} instance under test. */
    private Message message;

    /** A mock {@link TSIG} used to trigger the signed state without needing a real key. */
    @Mock
    private TSIG mockTsig;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test case.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the Message instance will be garbage‑collected.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * By default a newly constructed {@link Message} has {@code tsigState == TSIG_UNSIGNED},
     * therefore {@link Message#isSigned()} must return {@code false}.
     */
    @Test
    void isSigned_defaultIsUnsigned() {
        assertFalse(message.isSigned(),
                "A newly created Message should not be considered signed");
    }

    /**
     * Adding a TSIG record via {@link Message#setTSIG(TSIG)} puts the message into the
     * {@code TSIG_SIGNED} state, which makes {@code isSigned()} return {@code true}.
     */
    @Test
    void isSigned_afterSetTSIG_returnsTrue() {
        // The mock TSIG is sufficient – the implementation only stores the reference
        // and updates the internal state.
        message.setTSIG(mockTsig);
        assertTrue(message.isSigned(),
                "Message should be signed after a TSIG record has been attached");
    }

    /**
     * When {@link Message#setTSIG(TSIG, int, TSIGRecord)} is called with a non‑zero error
     * code, the internal state becomes {@code TSIG_FAILED}.  The method should still
     * report the message as signed.
     */
    @Test
    void isSigned_afterSetTSIGWithError_returnsTrue() {
        // Use a known error code (e.g., FORMERR) to simulate a failed verification.
        int errorCode = Rcode.FORMERR;
        message.setTSIG(mockTsig, errorCode, null);
        assertTrue(message.isSigned(),
                "Message should be considered signed even when TSIG verification fails");
    }

    /**
     * When a TSIG is verified successfully via {@link Message#setTSIG(TSIG, int, TSIGRecord)}
     * with an error code of {@code Rcode.NOERROR}, the state becomes {@code TSIG_VERIFIED},
     * and {@code isSigned()} must return {@code true}.
     */
    @Test
    void isSigned_afterSuccessfulVerification_returnsTrue() {
        int errorCode = Rcode.NOERROR;
        message.setTSIG(mockTsig, errorCode, null);
        assertTrue(message.isSigned(),
                "Message should be considered signed after successful TSIG verification");
    }
}
```