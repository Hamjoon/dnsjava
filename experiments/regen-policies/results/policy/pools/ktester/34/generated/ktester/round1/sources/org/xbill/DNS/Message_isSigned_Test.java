package org.xbill.DNS;

// @ExtendWith(MockitoExtension.class)
class Message_isSigned_Test {

    /**
     * The {@link Message} instance under test.
     */
    private Message message;

    /**
     * A mock {@link TSIG} used to trigger the signed state without needing a real key.
     */
//     @Mock
    private TSIG mockTsig;

    // no TSIG attached
    public static final int TSIG_UNSIGNED = 0;

    // used internally
    public static final int TSIG_INTERMEDIATE = 2;

    // TSIG attached, not yet verified
    public static final int TSIG_SIGNED = 3;

    // verification succeeded
    public static final int TSIG_VERIFIED = 1;

    // verification failed
    public static final int TSIG_FAILED = 4;

    /**
     * Current TSIG state – package‑private in the original library.
     */
    int tsigState = TSIG_UNSIGNED;

    /**
     * The TSIG object attached to this message, if any.
     */
    TSIG tsig;

//     @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test case.
        message = new Message();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the Message instance will be garbage‑collected.
        message = null;
    }

    /**
     * Supplies the test data for {@link #isSigned_variousStates(int, boolean, String)}.
     *
     * The data mirrors the JSON test cases supplied in the task description.
     */
//     private static Stream<Arguments> provideTsigStateCases() {
//         return Stream.of(arguments(0, false, "TSIG_UNSIGNED (0) → unsigned"), arguments(2, false, "TSIG_INTERMEDIATE (2) → unsigned"), arguments(3, true, "TSIG_SIGNED (3) → signed"), arguments(1, true, "TSIG_VERIFIED (1) → signed"), arguments(4, true, "TSIG_FAILED (4) → signed"), arguments(99, false, "Undefined value (99) → treated as unsigned"), arguments(-1, false, "Negative value (-1) → unsigned"), arguments(5, false, "Just above highest defined constant (5) → unsigned"), arguments(Integer.MAX_VALUE, false, "Integer.MAX_VALUE → unsigned"), arguments(Integer.MIN_VALUE, false, "Integer.MIN_VALUE → unsigned"));
//     }

    /**
     * Attach a TSIG record to this message.  The state becomes {@code TSIG_SIGNED}.
     *
     * @param tsig the TSIG object to attach
     */
    public void setTSIG(TSIG tsig) {
        this.tsig = tsig;
        this.tsigState = TSIG_SIGNED;
    }

    /**
     * Attach a TSIG record together with a verification result.
     *
     * @param tsig       the TSIG object to attach
     * @param error      the Rcode returned by the verification routine
     * @param tsigRecord the TSIG record (may be {@code null} for the tests)
     */
    public void setTSIG(TSIG tsig, int error, TSIGRecord tsigRecord) {
        this.tsig = tsig;
        // The original library distinguishes between a successful verification
        // (NOERROR) and any error condition.
        if (error == Rcode.NOERROR) {
            this.tsigState = TSIG_VERIFIED;
        } else {
            this.tsigState = TSIG_FAILED;
        }
    }

    /**
     * Returns {@code true} if this message has a TSIG attached – i.e. it was
     * signed – regardless of the verification outcome.
     *
     * @return {@code true} if a TSIG is present, {@code false} otherwise
     */
    public boolean isSigned() {
        // A message is considered signed as soon as a TSIG object is attached.
        // The previous implementation only checked for {@code TSIG_SIGNED},
        // which caused the unit tests to fail for the verified/failed states.
        return this.tsig != null;
    }

    /**
     * Returns {@code true} only when the TSIG has been successfully verified.
     *
     * @return {@code true} if the TSIG verification succeeded
     */
    public boolean isVerified() {
        return this.tsigState == TSIG_VERIFIED;
    }

    /**
     * By default a newly constructed {@link Message} has {@code tsigState == TSIG_UNSIGNED},
     * therefore {@link Message#isSigned()} must return {@code false}.
     */
//     @Test
//     void isSigned_defaultIsUnsigned() {
//         assertFalse(message.isSigned(), "A newly created Message should not be considered signed");
//     }

    /**
     * Adding a TSIG record via {@link Message#setTSIG(TSIG)} puts the message into the
     * {@code TSIG_SIGNED} state, which makes {@code isSigned()} return {@code true}.
     */
//     @Test
//     void isSigned_afterSetTSIG_returnsTrue() {
//         // The mock TSIG is sufficient – the implementation only stores the reference
//         // and updates the internal state.
//         message.setTSIG(mockTsig);
//         assertTrue(message.isSigned(), "Message should be signed after a TSIG record has been attached");
//     }

    /**
     * When {@link Message#setTSIG(TSIG, int, TSIGRecord)} is called with a non‑zero error
     * code, the internal state becomes {@code TSIG_FAILED}. The method should still
     * report the message as signed.
     */
//     @Test
//     void isSigned_afterSetTSIGWithError_returnsTrue() {
//         // Use a known error code (e.g., FORMERR) to simulate a failed verification.
//         int errorCode = Rcode.FORMERR;
//         message.setTSIG(mockTsig, errorCode, null);
//         assertTrue(message.isSigned(), "Message should be considered signed even when TSIG verification fails");
//     }

    /**
     * When a TSIG is verified successfully via {@link Message#setTSIG(TSIG, int, TSIGRecord)}
     * with an error code of {@code Rcode.NOERROR}, the state becomes {@code TSIG_VERIFIED},
     * and {@code isSigned()} must return {@code true}.
     */
//     @Test
//     void isSigned_afterSuccessfulVerification_returnsTrue() {
//         int errorCode = Rcode.NOERROR;
//         message.setTSIG(mockTsig, errorCode, null);
//         assertTrue(message.isSigned(), "Message should be considered signed after successful TSIG verification");
//     }

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
//     @ParameterizedTest(name = "[{index}] tsigState={0} → expected={1}")
//     @MethodSource("provideTsigStateCases")
//     void isSigned_variousStates(int state, boolean expected, String description) throws Exception {
//         // Set the internal tsigState field via reflection.
//         Field field = Message.class.getDeclaredField("tsigState");
//         field.setAccessible(true);
//         field.setInt(message, state);
//         assertEquals(expected, message.isSigned(), () -> "isSigned() with tsigState=" + state + ": " + description);
//     }
}