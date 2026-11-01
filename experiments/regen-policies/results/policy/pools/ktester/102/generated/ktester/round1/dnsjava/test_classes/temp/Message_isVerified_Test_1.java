package org.xbill.DNS;

import org.mockito.Mock;
import java.lang.reflect.Field;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
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
            Field f = Message.class.getDeclaredField("tsigState");
            f.setAccessible(true);
            f.setInt(message, state);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to set tsigState reflectively", e);
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
        Assert.assertFalse("A freshly constructed Message should not be verified", message.isVerified());
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
        Assert.assertFalse("Message with a TSIG error should not be verified", message.isVerified());
    }

    /**
     * When a TSIG is attached with {@code Rcode.NOERROR} the Message marks the
     * TSIG as verified. Consequently {@code isVerified()} must return {@code true}.
     */
    @Test
    void isVerified_tsigVerified_returnsTrue(@Mock TSIG mockTsig) {
        // Rcode.NOERROR (0) signals a successful verification.
        message.setTSIG(mockTsig, Rcode.NOERROR, null);
        Assert.assertTrue("Message with a successfully verified TSIG should be verified", message.isVerified());
    }

    /**
     * Verifies that {@code isVerified()} returns {@code true} only when
     * {@code tsigState == TSIG_VERIFIED}.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("trueBranchProvider")
    void isVerified_trueBranch(int tsigState, boolean expected, String description) {
        // TSIG_VERIFIED constant is defined as 1 in Message
        setTsigState(1);
        Assert.assertTrue("When tsigState == TSIG_VERIFIED, isVerified() should be true", message.isVerified());
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
        Assert.assertTrue(message.isVerified());
        // second call must still be true
        Assert.assertTrue(message.isVerified());
    }

    @Test
    void isVerified_idempotent_unverifiedState() {
        // TSIG_UNSIGNED
        setTsigState(0);
        Assert.assertFalse(message.isVerified());
        // second call must still be false
        Assert.assertFalse(message.isVerified());
    }

    @Before
    public void setUp() {
        // fresh Message for every test to avoid state leakage
        message = new Message();
    }

    @After
    public void tearDown() {
        message = null;
    }

    /**
     * Verifies that {@code isVerified()} returns {@code false} for a range of
     * non‑verified or out‑of‑range {@code tsigState} values.
     */
    @Test
    public void isVerified_falseBranches() {
        List<Integer> nonVerifiedStates = Arrays.asList(// TSIG_UNSIGNED
        0, // TSIG_INTERMEDIATE
        2, // TSIG_SIGNED
        3, // TSIG_FAILED
        4, -1, 5);
        for (int state : nonVerifiedStates) {
            setTsigState(state);
            Assert.assertFalse("When tsigState == " + state + ", isVerified() should be false", message.isVerified());
        }
    }
}
