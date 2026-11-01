```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Message#isVerified()}.
 *
 * <p>The {@code isVerified()} method simply returns {@code true} when the internal
 * TSIG state equals {@link Message#TSIG_VERIFIED}.  The tests below manipulate the
 * {@code tsigState} field directly (via reflection) to verify the behaviour for
 * all possible values, including out‑of‑range values.</p>
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

    /* ---------------------------------------------------------------------- */
    /* Helper to set the package‑private tsigState field via reflection.       */
    private void setTsigState(Message m, int state) throws Exception {
        Field f = Message.class.getDeclaredField("tsigState");
        f.setAccessible(true);
        f.setInt(m, state);
    }

    /* ---------------------------------------------------------------------- */
    /* True when the state equals TSIG_VERIFIED                               */
    private static Stream<Integer> verifiedStateProvider() {
        return Stream.of(Message.TSIG_VERIFIED);
    }

    @ParameterizedTest(name = "isVerified returns true when tsigState={0}")
    @MethodSource("verifiedStateProvider")
    void isVerified_whenStateIsVerified_returnsTrue(int state) throws Exception {
        Message m = new Message();
        setTsigState(m, state);
        assertTrue(m.isVerified(),
                "isVerified() should be true only when tsigState equals TSIG_VERIFIED");
    }

    /* ---------------------------------------------------------------------- */
    /* False for all defined non‑verified states                             */
    private static Stream<Integer> nonVerifiedStateProvider() {
        return Stream.of(
                Message.TSIG_UNSIGNED,      // 0
                Message.TSIG_INTERMEDIATE,  // 2
                Message.TSIG_SIGNED,        // 3
                Message.TSIG_FAILED         // 4
        );
    }

    @ParameterizedTest(name = "isVerified returns false for tsigState={0}")
    @MethodSource("nonVerifiedStateProvider")
    void isVerified_whenStateIsNonVerified_returnsFalse(int state) throws Exception {
        Message m = new Message();
        setTsigState(m, state);
        assertFalse(m.isVerified(),
                "isVerified() must be false for non‑verified tsigState values");
    }

    /* ---------------------------------------------------------------------- */
    /* False for out‑of‑range values                                           */
    private static Stream<Integer> outOfRangeStateProvider() {
        return Stream.of(
                -1,
                5,
                Integer.MAX_VALUE
        );
    }

    @ParameterizedTest(name = "isVerified returns false for out‑of‑range tsigState={0}")
    @MethodSource("outOfRangeStateProvider")
    void isVerified_whenStateIsOutOfRange_returnsFalse(int state) throws Exception {
        Message m = new Message();
        setTsigState(m, state);
        assertFalse(m.isVerified(),
                "isVerified() must be false for out‑of‑range tsigState values");
    }

    /* ---------------------------------------------------------------------- */
    /* Verify that the method reflects the *current* state after changes.      */
    @Test
    void isVerified_stateChanges_reflectCurrentValue() throws Exception {
        Message m = new Message();

        // start with VERIFIED → true
        setTsigState(m, Message.TSIG_VERIFIED);
        assertTrue(m.isVerified(), "Should be true after setting VERIFIED");

        // change to UNSIGNED → false
        setTsigState(m, Message.TSIG_UNSIGNED);
        assertFalse(m.isVerified(), "Should be false after setting UNSIGNED");

        // back to VERIFIED → true again
        setTsigState(m, Message.TSIG_VERIFIED);
        assertTrue(m.isVerified(), "Should be true after resetting to VERIFIED");
    }
}
```