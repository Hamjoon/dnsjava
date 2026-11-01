```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#isVerified()}.
 *
 * The {@code isVerified()} method returns {@code true} only when the internal
 * {@code tsigState} field equals {@link Message#TSIG_VERIFIED}.  The state can be
 * changed via the public {@code setTSIG} overload that accepts an error code.
 * An error code of {@code 0} indicates a successful verification, while any
 * non‑zero value indicates a failure.
 *
 * These tests exercise the default (unsigned) state, a verified state, and a
 * failed verification state without accessing any private members.
 */
@ExtendWith(MockitoExtension.class)
class Message_isVerified_Test {

    private Message message;

    @Mock
    private TSIG mockTsig; // a mock TSIG key – its behaviour is not needed for the test

    @BeforeEach
    void setupBeforeEach() {
        // a fresh Message for each test case
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up for now
    }

    @Test
    void isVerified_defaultIsFalse() {
        // A newly created Message has tsigState == TSIG_UNSIGNED (0)
        assertFalse(message.isVerified(),
                "A freshly constructed Message should not be verified");
    }

    @Test
    void isVerified_afterSuccessfulSetTSIG_returnsTrue() {
        // According to Message#setTSIG(TSIG, int, TSIGRecord), an error code of 0
        // marks the TSIG as verified.
        message.setTSIG(mockTsig, 0, null);
        assertTrue(message.isVerified(),
                "Message should be verified when setTSIG is called with error code 0");
    }

    @Test
    void isVerified_afterFailedSetTSIG_returnsFalse() {
        // Any non‑zero error code should leave the message in a non‑verified state.
        message.setTSIG(mockTsig, 1, null);
        assertFalse(message.isVerified(),
                "Message should not be verified when setTSIG is called with a non‑zero error code");
    }
}
```