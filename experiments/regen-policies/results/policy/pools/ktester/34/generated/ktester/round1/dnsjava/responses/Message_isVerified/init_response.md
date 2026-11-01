```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#isVerified()}.
 *
 * The {@code isVerified()} method simply returns {@code true} when the internal
 * TSIG state is {@link Message#TSIG_VERIFIED}.  The state can be changed through
 * the public {@code setTSIG(TSIG, int, TSIGRecord)} method.  Supplying an
 * {@code error} code of {@link Rcode#NOERROR} is the normal way the library
 * marks a TSIG as verified.
 */
@ExtendWith(MockitoExtension.class)
class Message_isVerified_Test {

    /** No global resources are required for these tests. */
    @BeforeAll
    static void setupBeforeAll() {
        // nothing to do
    }

    /** Prepare a fresh {@link Message} for each test. */
    @BeforeEach
    void setupBeforeEach() {
        // nothing to do
    }

    /** No per‑test clean‑up is required. */
    @AfterEach
    void teardownAfterEach() {
        // nothing to do
    }

    /** No global clean‑up is required. */
    @AfterAll
    static void teardownAfterAll() {
        // nothing to do
    }

    /** By default a newly‑created {@link Message} is *not* verified. */
    @Test
    void isVerified_defaultIsFalse() {
        Message m = new Message();          // uses the no‑arg constructor
        assertFalse(m.isVerified(),
                "A freshly created Message should not be verified");
    }

    /**
     * When a TSIG is attached with a successful verification result
     * ({@link Rcode#NOERROR}), {@code isVerified()} must return {@code true}.
     *
     * <p>The test uses a Mockito mock for {@link TSIG} because the verification
     * logic inside {@code setTSIG} only relies on the presence of a non‑null
     * key; the actual key material is irrelevant for this unit test.</p>
     */
    @Test
    void isVerified_afterSuccessfulSetTSIG_returnsTrue() {
        Message m = new Message();

        // Create a mock TSIG – we do not care about its behaviour here.
        TSIG mockKey = mock(TSIG.class);

        // The library marks the message as verified when the error code is NOERROR.
        m.setTSIG(mockKey, Rcode.NOERROR, null);

        assertTrue(m.isVerified(),
                "Message should be verified after setTSIG with Rcode.NOERROR");
    }

    /**
     * When a TSIG is attached with an error code other than {@link Rcode#NOERROR},
     * the message must *not* be considered verified.
     */
    @Test
    void isVerified_afterFailedSetTSIG_returnsFalse() {
        Message m = new Message();
        TSIG mockKey = mock(TSIG.class);

        // Use a non‑zero error code to simulate a verification failure.
        m.setTSIG(mockKey, Rcode.FORMERR, null);

        assertFalse(m.isVerified(),
                "Message should not be verified when setTSIG is called with an error code");
    }
}
```