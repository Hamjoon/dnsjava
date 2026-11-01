```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#isSigned()}.
 *
 * <p>The tests cover the three possible outcomes of the method:
 *
 * <ul>
 *   <li>unsigned (default state)
 *   <li>signed (state set by {@link Message#setTSIG(TSIG)})
 *   <li>failed (state set by {@link Message#setTSIG(TSIG, int, TSIGRecord)} with a non‑zero error)
 * </ul>
 *
 * <p>Only public APIs are used – no reflection or access to package‑private fields.
 */
@ExtendWith(MockitoExtension.class)
class Message_isSigned_Test {

    /** No global resources are required for the whole test suite. */
    @BeforeAll
    static void setupBeforeAll() {
        // nothing to initialise globally
    }

    /** A fresh {@link Message} instance for each test. */
    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        // the zero‑argument constructor creates an empty, unsigned message
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // clear reference – helps the GC in long test runs
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up globally
    }

    /**
     * By default a newly created {@code Message} has {@code tsigState == TSIG_UNSIGNED},
     * therefore {@code isSigned()} must return {@code false}.
     */
    @Test
    void unsignedMessage_isSigned_returnsFalse() {
        assertFalse(message.isSigned(),
                "A freshly constructed Message should not be considered signed");
    }

    /**
     * When a TSIG key is attached via {@link Message#setTSIG(TSIG)} the internal
     * state changes to {@code TSIG_SIGNED}. The public method {@code isSigned()}
     * must therefore return {@code true}.
     */
    @Test
    void setTSIG_changesStateToSigned_isSignedReturnsTrue(@Mock TSIG mockTsig) {
        // Act – attach a (mocked) TSIG key
        message.setTSIG(mockTsig);

        // Assert – the message now reports being signed
        assertTrue(message.isSigned(),
                "Message should be signed after setTSIG(TSIG) is invoked");
    }

    /**
     * When a TSIG key is attached together with a non‑zero error code,
     * {@link Message#setTSIG(TSIG, int, TSIGRecord)} puts the message into the
     * {@code TSIG_FAILED} state. {@code isSigned()} must still return {@code true}
     * because the method treats {@code FAILED} as a signed condition.
     */
    @Test
    void setTSIGWithError_marksFailed_isSignedReturnsTrue(
            @Mock TSIG mockTsig,
            @Mock TSIGRecord mockQueryTsig) throws IOException {

        // Use a non‑zero error code (e.g., 1) to trigger the FAILED state
        int errorCode = 1;

        // Act – attach the TSIG with an error
        message.setTSIG(mockTsig, errorCode, mockQueryTsig);

        // Assert – even in the FAILED state the method reports true
        assertTrue(message.isSigned(),
                "Message should be considered signed when TSIG state is FAILED");
    }
}
```