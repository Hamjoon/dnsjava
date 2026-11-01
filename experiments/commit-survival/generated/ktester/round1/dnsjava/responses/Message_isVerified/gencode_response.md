```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Message#isVerified()}.
 *
 * The {@code isVerified()} method simply checks whether the internal {@code tsigState}
 * equals {@link Message#TSIG_VERIFIED}.  The state can be any of the defined constants
 * or even an undefined value.  This test suite uses a parameterized test to verify the
 * behaviour for each possible state, without relying on the public {@code setTSIG}
 * overload (which only distinguishes between success (error = 0) and failure (error ≠ 0)).
 *
 * Because {@code tsigState} has package‑private visibility, the test class (which lives
 * in the same package) can set it directly.  This keeps the test simple and avoids
 * reflection overhead.
 */
@ExtendWith(MockitoExtension.class)
class Message_isVerified_Test {

    private Message message;

    @Mock
    private TSIG mockTsig; // Not used directly in these tests but kept for completeness

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message instance for each test case
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up
    }

    /**
     * Parameterized test covering all relevant {@code tsigState} values.
     *
     * @param state       the value to assign to {@code message.tsigState}
     * @param expected    the expected result of {@code message.isVerified()}
     * @param description a short description used as the assertion message
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideTsigStates")
    void isVerified_variousStates_returnsExpected(int state, boolean expected, String description) {
        // Directly manipulate the package‑private field to simulate the internal state
        message.tsigState = state;

        assertEquals(expected, message.isVerified(),
                () -> "When tsigState = " + state + " (" + description + "), isVerified() should return " + expected);
    }

    /**
     * Supplies the combinations of {@code tsigState}, expected result and a human‑readable
     * description.  The constants are taken from {@link Message}.
     */
    private static Stream<Arguments> provideTsigStates() {
        return Stream.of(
                // Verified state – should return true
                Arguments.of(Message.TSIG_VERIFIED, true,
                        "TSIG_VERIFIED (1) – verification succeeded"),
                // All other defined states – should return false
                Arguments.of(Message.TSIG_UNSIGNED, false,
                        "TSIG_UNSIGNED (0) – never verified"),
                Arguments.of(Message.TSIG_INTERMEDIATE, false,
                        "TSIG_INTERMEDIATE (2) – intermediate state"),
                Arguments.of(Message.TSIG_SIGNED, false,
                        "TSIG_SIGNED (3) – signed but not verified"),
                Arguments.of(Message.TSIG_FAILED, false,
                        "TSIG_FAILED (4) – verification failed"),
                // An undefined value – should also return false
                Arguments.of(99, false,
                        "Undefined state (99) – should be treated as not verified")
        );
    }
}
```