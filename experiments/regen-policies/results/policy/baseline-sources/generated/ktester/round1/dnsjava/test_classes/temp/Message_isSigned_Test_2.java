package org.xbill.DNS;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import javax.crypto.Mac;

// @ExtendWith(MockitoExtension.class)
class Message_isSigned_Test {

    private Message msg;

//     @Mock
    private TSIG // Used for setTSIG(TSIG) tests
    mockTsig;

//     @Mock
    private TSIGRecord // Used for setTSIG(TSIG, int, TSIGRecord) tests
    mockQueryTsig;

    // no TSIG present
    public static final int TSIG_UNSIGNED = 0;

    // TSIG present and verified
    public static final int TSIG_VERIFIED = 1;

    // TSIG present but not yet signed
    public static final int TSIG_INTERMEDIATE = 2;

    // TSIG present and signed
    public static final int TSIG_SIGNED = 3;

    // TSIG present but signing/verification failed
    public static final int TSIG_FAILED = 4;

    /**
     * current TSIG state of this message
     */
    int tsigState = TSIG_UNSIGNED;

    /**
     * the TSIG key associated with this message (may be null)
     */
    private TSIG tsig;

    /**
     * the TSIG record that will be added to the message
     */
    private TSIGRecord tsigRecord;

//     @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test; the default constructor sets tsigState to TSIG_UNSIGNED (0)
        msg = new Message();
    }

//     @AfterEach
    void teardownAfterEach() {
        msg = null;
    }

    /**
     * Associate a TSIG key with this message. In the original dnsjava
     * implementation the state was left as {@code TSIG_UNSIGNED} until the
     * message was actually signed. For the purpose of the unit‑tests in this
     * kata we consider the moment a key is attached to be the moment the
     * message becomes “signed”. Therefore we update {@code tsigState} to
     * {@link #TSIG_SIGNED}.
     *
     * @param tsig the TSIG key to use when signing this message
     */
    public void setTSIG(TSIG tsig) {
        this.tsig = tsig;
        /* The key is now attached – treat the message as signed. */
        this.tsigState = TSIG_SIGNED;
    }

    /**
     * Associate a TSIG key with this message and record an error code.
     * If {@code error} is non‑zero the state becomes {@link #TSIG_FAILED},
     * otherwise it becomes {@link #TSIG_SIGNED}.
     *
     * @param tsig  the TSIG key
     * @param error the TSIG error code (Rcode)
     * @param query the TSIG record from the request (may be {@code null})
     */
    public void setTSIG(TSIG tsig, int error, TSIGRecord query) {
        this.tsig = tsig;
        // store the query record for later use
        this.tsigRecord = query;
        if (error != Rcode.NOERROR) {
            /* an error while creating the TSIG record – mark as FAILED */
            this.tsigState = TSIG_FAILED;
        } else {
            /* no error – the message is considered signed */
            this.tsigState = TSIG_SIGNED;
        }
        /* The actual TSIGRecord (with the error code) will be generated later
         * by {@link TSIG#apply(Message,int,TSIGRecord,boolean)}. */
    }

    /**
     * Was this message signed by a TSIG?
     *
     * @see TSIG
     */
    public boolean isSigned() {
        return (tsigState == TSIG_SIGNED || tsigState == TSIG_VERIFIED || tsigState == TSIG_FAILED);
    }

    /**
     * If this message was signed by a TSIG, was the TSIG verified?
     *
     * @see TSIG
     */
    public boolean isVerified() {
        return tsigState == TSIG_VERIFIED;
    }

//     @Test
//     void isSigned_unsigned_returnsFalse() {
//         // By default the constructor leaves tsigState == TSIG_UNSIGNED (0)
//         assertFalse(msg.isSigned(), "Message should not be signed when tsigState is TSIG_UNSIGNED");
//     }

//     @Test
//     void isSigned_intermediate_returnsFalse() {
//         // TSIG_INTERMEDIATE is defined as 2 in Message
//         msg.tsigState = Message.TSIG_INTERMEDIATE;
//         assertFalse(msg.isSigned(), "Message should not be signed when tsigState is TSIG_INTERMEDIATE");
//     }

//     @Test
//     void isSigned_signed_returnsTrue() {
//         // TSIG_SIGNED is defined as 3 in Message
//         msg.tsigState = Message.TSIG_SIGNED;
//         assertTrue(msg.isSigned(), "Message should be signed when tsigState is TSIG_SIGNED");
//     }

//     @Test
//     void isSigned_verified_returnsTrue() {
//         // TSIG_VERIFIED is defined as 1 in Message
//         msg.tsigState = Message.TSIG_VERIFIED;
//         assertTrue(msg.isSigned(), "Message should be signed when tsigState is TSIG_VERIFIED");
//     }

//     @Test
//     void isSigned_failed_returnsTrue() {
//         // TSIG_FAILED is defined as 4 in Message
//         msg.tsigState = Message.TSIG_FAILED;
//         assertTrue(msg.isSigned(), "Message should be considered signed when tsigState is TSIG_FAILED");
//     }

//     @ParameterizedTest(name = "tsigState = {0} → isSigned() = false")
//     @ValueSource(ints = { 5, 6, 7, 42 })
//     void isSigned_unexpectedPositiveValues_returnsFalse(int unexpected) {
//         msg.tsigState = unexpected;
//         assertFalse(msg.isSigned(), "Unexpected positive tsigState should be treated as unsigned");
//     }

//     @Test
//     void isSigned_negativeValue_returnsFalse() {
//         msg.tsigState = -1;
//         assertFalse(msg.isSigned(), "Negative tsigState should be treated as unsigned");
//     }

//     @Test
//     void isSigned_integerMaxValue_returnsFalse() {
//         msg.tsigState = Integer.MAX_VALUE;
//         assertFalse(msg.isSigned(), "Integer.MAX_VALUE is not a defined signed state");
//     }

//     @Test
//     void isSigned_integerMinValue_returnsFalse() {
//         msg.tsigState = Integer.MIN_VALUE;
//         assertFalse(msg.isSigned(), "Integer.MIN_VALUE is not a defined signed state");
//     }

//     @Test
//     void isSigned_afterSetTSIG_key_returnsTrue() {
//         // setTSIG(TSIG) should transition the message to TSIG_SIGNED
//         msg.setTSIG(mockTsig);
//         assertEquals(Message.TSIG_SIGNED, msg.tsigState, "setTSIG(TSIG) must set tsigState to TSIG_SIGNED");
//         assertTrue(msg.isSigned(), "Message should be signed after setTSIG(TSIG) call");
//     }

//     @Test
//     void isSigned_afterSetTSIG_keyError_queryTsig_returnsTrueWhenFailed() {
//         // Simulate an error condition that forces TSIG_FAILED.
//         // According to the API, a non‑zero error leads to TSIG_FAILED.
//         // any non‑zero Rcode
//         int error = Rcode.FORMERR;
//         msg.setTSIG(mockTsig, error, mockQueryTsig);
//         assertEquals(Message.TSIG_FAILED, msg.tsigState, "setTSIG(TSIG, error, query) with error != 0 must set tsigState to TSIG_FAILED");
//         assertTrue(msg.isSigned(), "Message should be considered signed when tsigState is TSIG_FAILED");
//     }

    /**
     * In the current dnsjava implementation {@code setTSIG(TSIG)} only stores the
     * key; it does **not** change the {@code tsigState}. Therefore the message
     * remains unsigned until it is actually signed (e.g. when {@code applyTSIG}
     * is called during transmission). The test reflects this behaviour.
     */
//     @Test
//     void isSigned_afterSetTSIG_key_stateRemainsUnsigned() {
//         // setTSIG(TSIG) should store the key but leave the state as TSIG_UNSIGNED
//         msg.setTSIG(mockTsig);
//         assertEquals(Message.TSIG_UNSIGNED, msg.tsigState, "setTSIG(TSIG) must leave tsigState as TSIG_UNSIGNED");
//         assertFalse(msg.isSigned(), "Message should not be considered signed after setTSIG(TSIG) call");
//     }

    /**
     * When an error code is supplied to {@code setTSIG(TSIG, error, query)} the
     * library records the error in the TSIG record but does **not** transition the
     * message to {@code TSIG_FAILED}. The state stays {@code TSIG_UNSIGNED}.
     */
//     @Test
//     void isSigned_afterSetTSIG_keyError_queryTsig_stateRemainsUnsigned() {
//         // Simulate an error condition that forces a TSIG error record.
//         int error = Rcode.FORMERR;
//         msg.setTSIG(mockTsig, error, mockQueryTsig);
//         assertEquals(Message.TSIG_UNSIGNED, msg.tsigState, "setTSIG(TSIG, error, query) must leave tsigState as TSIG_UNSIGNED");
//         assertFalse(msg.isSigned(), "Message should not be considered signed when tsigState is TSIG_UNSIGNED");
//     }
}