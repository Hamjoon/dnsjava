package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_isVerified_Test {

    private Message message;

    @Mock
    private TSIG // a mock TSIG key – its behaviour is not needed for the test
    mockTsig;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message instance for each test case
        message = new Message();
    }

    /**
     * Supplies the combinations of {@code tsigState}, expected result and a human‑readable
     * description.  The constants are taken from {@link Message}.
     */
    private static Stream<Arguments> provideTsigStates() {
        return Stream.of(// Verified state – should return true
        Arguments.of(Message.TSIG_VERIFIED, true, "TSIG_VERIFIED (1) – verification succeeded"), // All other defined states – should return false
        Arguments.of(Message.TSIG_UNSIGNED, false, "TSIG_UNSIGNED (0) – never verified"), Arguments.of(Message.TSIG_INTERMEDIATE, false, "TSIG_INTERMEDIATE (2) – intermediate state"), Arguments.of(Message.TSIG_SIGNED, false, "TSIG_SIGNED (3) – signed but not verified"), Arguments.of(Message.TSIG_FAILED, false, "TSIG_FAILED (4) – verification failed"), // An undefined value – should also return false
        Arguments.of(99, false, "Undefined state (99) – should be treated as not verified"));
    }

    @Test
    void isVerified_defaultIsFalse() {
        // A newly created Message has tsigState == TSIG_UNSIGNED (0)
        assertFalse(message.isVerified(), "A freshly constructed Message should not be verified");
    }

    @Test
    void isVerified_afterSuccessfulSetTSIG_returnsTrue() {
        // According to Message#setTSIG(TSIG, int, TSIGRecord), an error code of 0
        // marks the TSIG as verified.
        message.setTSIG(mockTsig, 0, null);
        assertTrue(message.isVerified(), "Message should be verified when setTSIG is called with error code 0");
    }

    @Test
    void isVerified_afterFailedSetTSIG_returnsFalse() {
        // Any non‑zero error code should leave the message in a non‑verified state.
        message.setTSIG(mockTsig, 1, null);
        assertFalse(message.isVerified(), "Message should not be verified when setTSIG is called with a non‑zero error code");
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
        assertEquals(expected, message.isVerified(), () -> "When tsigState = " + state + " (" + description + "), isVerified() should return " + expected);
    }
}
