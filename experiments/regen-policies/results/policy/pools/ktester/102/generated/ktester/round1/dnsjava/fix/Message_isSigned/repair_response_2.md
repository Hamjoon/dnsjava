```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link Message#isSigned()}.
 *
 * <p>The tests manipulate the package‑private {@code tsigState} field via reflection
 * to verify the behaviour of {@code isSigned()} for all defined and edge states.</p>
 */
class Message_isSigned_Test {

    /** A fresh {@link Message} instance for each test. */
    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        // The zero‑argument constructor creates an empty, unsigned message
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference – helps the GC in long test runs
        message = null;
    }

    /** Standard TSIG state values and the expected result of {@code isSigned()}. */
    private static Stream<Arguments> provideStandardStates() {
        return Stream.of(
                // TSIG_UNSIGNED
                Arguments.of(0, false),
                // TSIG_INTERMEDIATE (not signed)
                Arguments.of(2, false),
                // TSIG_SIGNED
                Arguments.of(3, true),
                // TSIG_VERIFIED
                Arguments.of(1, true),
                // TSIG_FAILED (treated as signed)
                Arguments.of(4, true),
                // an unexpected positive value – the implementation treats unknown values as unsigned
                Arguments.of(99, false),
                // another unknown value – also false
                Arguments.of(99, false)
        );
    }

    /** Edge‑case values that lie outside the defined range. */
    private static Stream<Arguments> provideEdgeStates() {
        return Stream.of(
                Arguments.of(-1, false),
                Arguments.of(5, false),
                Arguments.of(Integer.MAX_VALUE, false),
                Arguments.of(Integer.MIN_VALUE, false)
        );
    }

    /**
     * Helper that uses reflection to set the package‑private {@code tsigState}
     * field on a {@link Message} instance.
     *
     * @param msg   the message whose state we want to manipulate
     * @param value the integer value to assign to {@code tsigState}
     */
    private static void setTsigState(Message msg, int value) throws Exception {
        Field field = Message.class.getDeclaredField("tsigState");
        field.setAccessible(true);
        field.setInt(msg, value);
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
    void setTSIG_changesStateToSigned_isSignedReturnsTrue() {
        // Create a minimal TSIG with a dummy key.
        TSIG tsig = new TSIG("example.", "abcd1234");
        // Act – attach the TSIG key
        message.setTSIG(tsig);
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
    void setTSIGWithError_marksFailed_isSignedReturnsTrue() throws IOException {
        TSIG tsig = new TSIG("example.", "abcd1234");
        // Use a non‑zero error code (e.g., 1) to trigger the FAILED state.
        int errorCode = 1;
        // Act – attach the TSIG with an error; the third argument (the query TSIG record) can be null.
        message.setTSIG(tsig, errorCode, null);
        // Assert – even in the FAILED state the method reports true.
        assertTrue(message.isSigned(),
                "Message should be considered signed when TSIG state is FAILED");
    }

    /**
     * Parameterised test that sets {@code tsigState} to each of the defined
     * constants (and an arbitrary unknown value) and verifies the result of
     * {@link Message#isSigned()}.
     */
    @ParameterizedTest(name = "tsigState={0} ⇒ isSigned={1}")
    @MethodSource("provideStandardStates")
    void isSigned_standardStates(int tsigState, boolean expected) throws Exception {
        setTsigState(message, tsigState);
        assertEquals(expected, message.isSigned(),
                () -> String.format("tsigState=%d should yield isSigned=%b", tsigState, expected));
    }

    /**
     * Edge‑case values that are outside the defined range are also expected to
     * return {@code false}. These values are injected via reflection because
     * they cannot be produced through the public API.
     */
    @ParameterizedTest(name = "edge tsigState={0} ⇒ isSigned={1}")
    @MethodSource("provideEdgeStates")
    void isSigned_edgeStates(int tsigState, boolean expected) throws Exception {
        setTsigState(message, tsigState);
        assertEquals(expected, message.isSigned(),
                () -> String.format("Edge tsigState=%d should yield isSigned=%b", tsigState, expected));
    }
}
```