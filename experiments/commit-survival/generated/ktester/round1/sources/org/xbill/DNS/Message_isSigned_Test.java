package org.xbill.DNS;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    void setUp() {
        // a fresh Message for each test; the default constructor sets tsigState to TSIG_UNSIGNED (0)
        msg = new Message();
    }

    @AfterEach
    void tearDown() {
        msg = null;
    }

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

    @Test
    void isSigned_unsigned_returnsFalse() {
        // By default the constructor leaves tsigState == TSIG_UNSIGNED (0)
        assertFalse(msg.isSigned(), "Message should not be signed when tsigState is TSIG_UNSIGNED");
    }

    @Test
    void isSigned_intermediate_returnsFalse() {
        msg.tsigState = Message.TSIG_INTERMEDIATE;
        assertFalse(msg.isSigned(), "Message should not be signed when tsigState is TSIG_INTERMEDIATE");
    }

    @Test
    void isSigned_signed_returnsTrue() {
        msg.tsigState = Message.TSIG_SIGNED;
        assertTrue(msg.isSigned(), "Message should be signed when tsigState is TSIG_SIGNED");
    }

    @Test
    void isSigned_verified_returnsTrue() {
        msg.tsigState = Message.TSIG_VERIFIED;
        assertTrue(msg.isSigned(), "Message should be signed when tsigState is TSIG_VERIFIED");
    }

    @Test
    void isSigned_failed_returnsTrue() {
        msg.tsigState = Message.TSIG_FAILED;
        assertTrue(msg.isSigned(), "Message should be considered signed when tsigState is TSIG_FAILED");
    }

    @ParameterizedTest(name = "tsigState = {0} → isSigned() = false")
    @ValueSource(ints = { 5, 6, 7, 42 })
    void isSigned_unexpectedPositiveValues_returnsFalse(int unexpected) {
        msg.tsigState = unexpected;
        assertFalse(msg.isSigned(), "Unexpected positive tsigState should be treated as unsigned");
    }

    @Test
    void isSigned_negativeValue_returnsFalse() {
        msg.tsigState = -1;
        assertFalse(msg.isSigned(), "Negative tsigState should be treated as unsigned");
    }

    @Test
    void isSigned_integerMaxValue_returnsFalse() {
        msg.tsigState = Integer.MAX_VALUE;
        assertFalse(msg.isSigned(), "Integer.MAX_VALUE is not a defined signed state");
    }

    @Test
    void isSigned_integerMinValue_returnsFalse() {
        msg.tsigState = Integer.MIN_VALUE;
        assertFalse(msg.isSigned(), "Integer.MIN_VALUE is not a defined signed state");
    }

    @Test
    void isSigned_afterSetTSIG_key_stateRemainsUnsigned() {
        // According to dnsjava, setTSIG(TSIG) only stores the key and does NOT
        // change tsigState. The message therefore remains unsigned.
        msg.setTSIG(mockTsig);
        assertEquals(Message.TSIG_UNSIGNED, msg.tsigState, "setTSIG(TSIG) must leave tsigState as TSIG_UNSIGNED");
        assertFalse(msg.isSigned(), "Message should not be considered signed after setTSIG(TSIG) call");
    }

    @Test
    void isSigned_afterSetTSIG_keyError_queryTsig_stateRemainsUnsigned() {
        // Supplying an error code creates a TSIG record with the error but does
        // NOT transition the message to TSIG_FAILED – the state stays unsigned.
        int error = Rcode.FORMERR;
        msg.setTSIG(mockTsig, error, mockQueryTsig);
        assertEquals(Message.TSIG_UNSIGNED, msg.tsigState, "setTSIG(TSIG, error, query) must leave tsigState as TSIG_UNSIGNED");
        assertFalse(msg.isSigned(), "Message should not be considered signed when tsigState is TSIG_UNSIGNED");
    }
}
