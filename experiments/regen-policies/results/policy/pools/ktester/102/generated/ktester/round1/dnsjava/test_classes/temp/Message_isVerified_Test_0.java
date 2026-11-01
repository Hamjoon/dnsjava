package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_isVerified_Test {

    /**
     * The Message instance used in the tests.
     */
    private Message message;

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

    /**
     * Helper that sets the private {@code tsigState} field via reflection.
     *
     * @param state the value to assign to {@code tsigState}
     */
    private void setTsigState(int state) {
        try {
            Field field = Message.class.getDeclaredField("tsigState");
            field.setAccessible(true);
            field.setInt(message, state);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set tsigState via reflection", e);
        }
    }

    /**
     * Test data for the true branch – only TSIG_VERIFIED (1) should return true.
     */
    private static Stream<Arguments> trueBranchProvider() {
        return Stream.of(Arguments.of(1, true, "When the internal tsigState is set to TSIG_VERIFIED (1), isVerified() should return true."));
    }

    /**
     * Test data for the false branch – all non‑verified or out‑of‑range values.
     */
    private static Stream<Arguments> falseBranchProvider() {
        return Stream.of(Arguments.of(0, false, "TSIG_UNSIGNED (0) – not verified."), Arguments.of(2, false, "TSIG_INTERMEDIATE (2) – not verified."), Arguments.of(3, false, "TSIG_SIGNED (3) – not verified."), Arguments.of(4, false, "TSIG_FAILED (4) – not verified."), Arguments.of(-1, false, "Negative value – not verified."), Arguments.of(5, false, "Value beyond defined constants – not verified."));
    }

    /**
     * By default a newly created Message has {@code tsigState == TSIG_UNSIGNED},
     * therefore {@code isVerified()} must return {@code false}.
     */
    @Test
    void isVerified_defaultMessage_returnsFalse() {
        assertFalse(message.isVerified(), "A freshly constructed Message should not be verified");
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
        assertFalse(message.isVerified(), "Message with a TSIG error should not be verified");
    }

    /**
     * When a TSIG is attached with {@code Rcode.NOERROR} the Message marks the
     * TSIG as verified. Consequently {@code isVerified()} must return {@code true}.
     */
    @Test
    void isVerified_tsigVerified_returnsTrue(@Mock TSIG mockTsig) {
        // Rcode.NOERROR (0) signals a successful verification.
        message.setTSIG(mockTsig, Rcode.NOERROR, null);
        assertTrue(message.isVerified(), "Message with a successfully verified TSIG should be verified");
    }

    /**
     * Verifies that {@code isVerified()} returns {@code true} only when
     * {@code tsigState == TSIG_VERIFIED}.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("trueBranchProvider")
    void isVerified_trueBranch(int tsigState, boolean expected, String description) {
        setTsigState(tsigState);
        assertEquals(expected, message.isVerified(), "Unexpected result for tsigState = " + tsigState);
    }

    /**
     * Verifies that {@code isVerified()} returns {@code false} for all
     * non‑verified or out‑of‑range {@code tsigState} values.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("falseBranchProvider")
    void isVerified_falseBranch(int tsigState, boolean expected, String description) {
        setTsigState(tsigState);
        assertEquals(expected, message.isVerified(), "Unexpected result for tsigState = " + tsigState);
    }

    @Test
    void isVerified_idempotent_verifiedState() {
        // TSIG_VERIFIED
        setTsigState(1);
        assertTrue(message.isVerified(), "First call should be true");
        assertTrue(message.isVerified(), "Second call should still be true");
    }

    @Test
    void isVerified_idempotent_unverifiedState() {
        // TSIG_UNSIGNED
        setTsigState(0);
        assertFalse(message.isVerified(), "First call should be false");
        assertFalse(message.isVerified(), "Second call should still be false");
    }
}
