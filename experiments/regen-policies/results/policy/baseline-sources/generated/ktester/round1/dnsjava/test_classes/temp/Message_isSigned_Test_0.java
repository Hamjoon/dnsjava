package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// No mocks are required but kept for consistency with template
@ExtendWith(MockitoExtension.class)
class Message_isSigned_Test {

    private Message msg;

    @Mock
    private TSIG // Used for setTSIG(TSIG) tests
    mockTsig;

    @Mock
    private TSIGRecord // Used for setTSIG(TSIG, int, TSIGRecord) tests
    mockQueryTsig;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test; the default constructor sets tsigState to TSIG_UNSIGNED (0)
        msg = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference – not strictly necessary but mirrors the template
        msg = null;
    }

    @Test
    void isSigned_unsigned_returnsFalse() {
        // By default the constructor leaves tsigState == TSIG_UNSIGNED (0)
        assertFalse(msg.isSigned(), "Message should not be signed when tsigState is TSIG_UNSIGNED");
    }

    @Test
    void isSigned_intermediate_returnsFalse() {
        // TSIG_INTERMEDIATE is defined as 2 in Message
        msg.tsigState = Message.TSIG_INTERMEDIATE;
        assertFalse(msg.isSigned(), "Message should not be signed when tsigState is TSIG_INTERMEDIATE");
    }

    @Test
    void isSigned_signed_returnsTrue() {
        // TSIG_SIGNED is defined as 3 in Message
        msg.tsigState = Message.TSIG_SIGNED;
        assertTrue(msg.isSigned(), "Message should be signed when tsigState is TSIG_SIGNED");
    }

    @Test
    void isSigned_verified_returnsTrue() {
        // TSIG_VERIFIED is defined as 1 in Message
        msg.tsigState = Message.TSIG_VERIFIED;
        assertTrue(msg.isSigned(), "Message should be signed when tsigState is TSIG_VERIFIED");
    }

    @Test
    void isSigned_failed_returnsTrue() {
        // TSIG_FAILED is defined as 4 in Message
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
    void isSigned_afterSetTSIG_key_returnsTrue() {
        // setTSIG(TSIG) should transition the message to TSIG_SIGNED
        msg.setTSIG(mockTsig);
        assertEquals(Message.TSIG_SIGNED, msg.tsigState, "setTSIG(TSIG) must set tsigState to TSIG_SIGNED");
        assertTrue(msg.isSigned(), "Message should be signed after setTSIG(TSIG) call");
    }

    @Test
    void isSigned_afterSetTSIG_keyError_queryTsig_returnsTrueWhenFailed() {
        // Simulate an error condition that forces TSIG_FAILED.
        // According to the API, a non‑zero error leads to TSIG_FAILED.
        // any non‑zero Rcode
        int error = Rcode.FORMERR;
        msg.setTSIG(mockTsig, error, mockQueryTsig);
        assertEquals(Message.TSIG_FAILED, msg.tsigState, "setTSIG(TSIG, error, query) with error != 0 must set tsigState to TSIG_FAILED");
        assertTrue(msg.isSigned(), "Message should be considered signed when tsigState is TSIG_FAILED");
    }
}
