```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Message#isSigned()} and the interaction with the TSIG‑related
 * {@code setTSIG} methods.
 *
 * The original expectations that {@code setTSIG(TSIG)} immediately puts the
 * message into the {@code TSIG_SIGNED} state were based on a different version
 * of dnsjava. In the version used for this kata the TSIG key is stored but the
 * state remains {@code TSIG_UNSIGNED} until the message is actually signed
 * (e.g. when it is sent). Therefore the tests have been adjusted to reflect the
 * real behaviour of the library.
 */
@ExtendWith(MockitoExtension.class)
class Message_isSigned_Test {

    private Message msg;

    @Mock
    private TSIG mockTsig;               // Used for setTSIG(TSIG) tests

    @Mock
    private TSIGRecord mockQueryTsig;    // Used for setTSIG(TSIG, int, TSIGRecord) tests

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test; the default constructor sets tsigState to TSIG_UNSIGNED (0)
        msg = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
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

    /**
     * In the current dnsjava implementation {@code setTSIG(TSIG)} only stores the
     * key; it does **not** change the {@code tsigState}. Therefore the message
     * remains unsigned until it is actually signed (e.g. when {@code applyTSIG}
     * is called during transmission). The test reflects this behaviour.
     */
    @Test
    void isSigned_afterSetTSIG_key_stateRemainsUnsigned() {
        // setTSIG(TSIG) should store the key but leave the state as TSIG_UNSIGNED
        msg.setTSIG(mockTsig);
        assertEquals(Message.TSIG_UNSIGNED, msg.tsigState,
                "setTSIG(TSIG) must leave tsigState as TSIG_UNSIGNED");
        assertFalse(msg.isSigned(),
                "Message should not be considered signed after setTSIG(TSIG) call");
    }

    /**
     * When an error code is supplied to {@code setTSIG(TSIG, error, query)} the
     * library records the error in the TSIG record but does **not** transition the
     * message to {@code TSIG_FAILED}. The state stays {@code TSIG_UNSIGNED}.
     */
    @Test
    void isSigned_afterSetTSIG_keyError_queryTsig_stateRemainsUnsigned() {
        // Simulate an error condition that forces a TSIG error record.
        int error = Rcode.FORMERR;
        msg.setTSIG(mockTsig, error, mockQueryTsig);
        assertEquals(Message.TSIG_UNSIGNED, msg.tsigState,
                "setTSIG(TSIG, error, query) must leave tsigState as TSIG_UNSIGNED");
        assertFalse(msg.isSigned(),
                "Message should not be considered signed when tsigState is TSIG_UNSIGNED");
    }
}
```