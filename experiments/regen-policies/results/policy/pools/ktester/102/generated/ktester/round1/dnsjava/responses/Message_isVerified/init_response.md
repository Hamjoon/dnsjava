```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_isVerified_Test {

    /** The Message instance used in the tests. */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test to avoid state leakage.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * By default a newly created Message has {@code tsigState == TSIG_UNSIGNED},
     * therefore {@code isVerified()} must return {@code false}.
     */
    @Test
    void isVerified_defaultMessage_returnsFalse() {
        assertFalse(message.isVerified(),
                "A freshly constructed Message should not be verified");
    }

    /**
     * When a TSIG is attached with a non‑zero error code the internal state is
     * set to {@code TSIG_FAILED}. In that situation {@code isVerified()} must
     * still return {@code false}.
     */
    @Test
    void isVerified_tsigSignedWithError_returnsFalse(@Mock TSIG mockTsig) {
        // Rcode.FORMERR (1) is a typical error code indicating a bad TSIG.
        message.setTSIG(mockTsig, Rcode.FORMERR, null);
        assertFalse(message.isVerified(),
                "Message with a TSIG error should not be verified");
    }

    /**
     * When a TSIG is attached with {@code Rcode.NOERROR} the Message marks the
     * TSIG as verified. Consequently {@code isVerified()} must return {@code true}.
     */
    @Test
    void isVerified_tsigVerified_returnsTrue(@Mock TSIG mockTsig) {
        // Rcode.NOERROR (0) signals a successful verification.
        message.setTSIG(mockTsig, Rcode.NOERROR, null);
        assertTrue(message.isVerified(),
                "Message with a successfully verified TSIG should be verified");
    }
}
```