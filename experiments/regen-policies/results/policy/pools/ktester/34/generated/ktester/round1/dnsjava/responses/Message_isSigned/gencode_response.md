```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for {@link Message#isSigned()}.
 *
 * <p>The {@code isSigned()} method returns {@code true} when the internal {@code tsigState}
 * indicates that a TSIG record has been added, verified or that verification has failed.
 * The state is normally changed via the public {@code setTSIG} APIs, but for exhaustive
 * testing we also set the {@code tsigState} field directly (using reflection) to cover
 * all defined and undefined values.</p>
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

    /* --------------------------------------------------------------------- */
    /* Simple behavioural tests that use the public API                     */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* Parameterised tests that manipulate the internal tsigState directly */
    /* --------------------------------------------------------------------- */

    /**
     * Parameterised test covering all documented and undocumented {@code tsigState}
     * values. The {@code tsigState} field is package‑private; we set it via reflection
     * to avoid any accidental visibility issues and to keep the test robust if the
     * implementation changes its access level.
     *
     * @param state       the value to assign to {@code tsigState}
     * @param expected    the expected result of {@link Message#isSigned()}
     * @param description textual description of the test case (used in the assertion message)
     */
    @ParameterizedTest(name = "[{index}] tsigState={0} → expected={1}")
    @MethodSource("provideTsigStateCases")
    void isSigned_variousStates(int state, boolean expected, String description) throws Exception {
        // Set the internal tsigState field via reflection.
        Field field = Message.class.getDeclaredField("tsigState");
        field.setAccessible(true);
        field.setInt(message, state);

        assertEquals(expected, message.isSigned(),
                () -> "isSigned() with tsigState=" + state + ": " + description);
    }

    /**
     * Supplies the test data for {@link #isSigned_variousStates(int, boolean, String)}.
     *
     * The data mirrors the JSON test cases supplied in the task description.
     */
    private static Stream<Arguments> provideTsigStateCases() {
        return Stream.of(
                // Normal defined states
                arguments(0, false, "TSIG_UNSIGNED (0) → unsigned"),
                arguments(2, false, "TSIG_INTERMEDIATE (2) → unsigned"),
                arguments(3, true,  "TSIG_SIGNED (3) → signed"),
                arguments(1, true,  "TSIG_VERIFIED (1) → signed"),
                arguments(4, true,  "TSIG_FAILED (4) → signed"),
                // Undefined but within int range
                arguments(99, false, "Undefined value (99) → treated as unsigned"),
                // Edge cases
                arguments(-1, false, "Negative value (-1) → unsigned"),
                arguments(5, false, "Just above highest defined constant (5) → unsigned"),
                arguments(Integer.MAX_VALUE, false, "Integer.MAX_VALUE → unsigned"),
                arguments(Integer.MIN_VALUE, false, "Integer.MIN_VALUE → unsigned")
        );
    }
}
```