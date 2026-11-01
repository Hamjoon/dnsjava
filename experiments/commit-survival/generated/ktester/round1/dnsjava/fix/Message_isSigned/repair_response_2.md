```java
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

/**
 * Unit tests for {@link Message#isSigned()}.
 *
 * The tests are placed in the same package as {@link Message} so that they can
 * access the package‑private {@code tsigState} field.
 */
@ExtendWith(MockitoExtension.class)
class MessageIsSignedTest {

    private Message msg;

    @Mock
    private TSIG mockTsig;               // used for {@code setTSIG(TSIG)} tests

    @Mock
    private TSIGRecord mockQueryTsig;    // used for {@code setTSIG(TSIG, int, TSIGRecord)} tests

    @BeforeEach
    void setUp() {
        // a fresh Message for each test; the default constructor sets tsigState to TSIG_UNSIGNED (0)
        msg = new Message();
    }

    @AfterEach
    void tearDown() {
        msg = null;
    }

    @Test
    void isSigned_unsigned_returnsFalse() {
        // By default the constructor leaves tsigState == TSIG_UNSIGNED (0)
        assertFalse(msg.isSigned(),
                "Message should not be signed when tsigState is TSIG_UNSIGNED");
    }

    @Test
    void isSigned_intermediate_returnsFalse() {
        msg.tsigState = Message.TSIG_INTERMEDIATE;
        assertFalse(msg.isSigned(),
                "Message should not be signed when tsigState is TSIG_INTERMEDIATE");
    }

    @Test
    void isSigned_signed_returnsTrue() {
        msg.tsigState = Message.TSIG_SIGNED;
        assertTrue(msg.isSigned(),
                "Message should be signed when tsigState is TSIG_SIGNED");
    }

    @Test
    void isSigned_verified_returnsTrue() {
        msg.tsigState = Message.TSIG_VERIFIED;
        assertTrue(msg.isSigned(),
                "Message should be signed when tsigState is TSIG_VERIFIED");
    }

    @Test
    void isSigned_failed_returnsTrue() {
        msg.tsigState = Message.TSIG_FAILED;
        assertTrue(msg.isSigned(),
                "Message should be considered signed when tsigState is TSIG_FAILED");
    }

    @ParameterizedTest(name = "tsigState = {0} → isSigned() = false")
    @ValueSource(ints = {5, 6, 7, 42})
    void isSigned_unexpectedPositiveValues_returnsFalse(int unexpected) {
        msg.tsigState = unexpected;
        assertFalse(msg.isSigned(),
                "Unexpected positive tsigState should be treated as unsigned");
    }

    @Test
    void isSigned_negativeValue_returnsFalse() {
        msg.tsigState = -1;
        assertFalse(msg.isSigned(),
                "Negative tsigState should be treated as unsigned");
    }

    @Test
    void isSigned_integerMaxValue_returnsFalse() {
        msg.tsigState = Integer.MAX_VALUE;
        assertFalse(msg.isSigned(),
                "Integer.MAX_VALUE is not a defined signed state");
    }

    @Test
    void isSigned_integerMinValue_returnsFalse() {
        msg.tsigState = Integer.MIN_VALUE;
        assertFalse(msg.isSigned(),
                "Integer.MIN_VALUE is not a defined signed state");
    }

    @Test
    void isSigned_afterSetTSIG_key_stateRemainsUnsigned() {
        // According to dnsjava, setTSIG(TSIG) only stores the key and does NOT
        // change tsigState. The message therefore remains unsigned.
        msg.setTSIG(mockTsig);
        assertEquals(Message.TSIG_UNSIGNED, msg.tsigState,
                "setTSIG(TSIG) must leave tsigState as TSIG_UNSIGNED");
        assertFalse(msg.isSigned(),
                "Message should not be considered signed after setTSIG(TSIG) call");
    }

    @Test
    void isSigned_afterSetTSIG_keyError_queryTsig_stateRemainsUnsigned() {
        // Supplying an error code creates a TSIG record with the error but does
        // NOT transition the message to TSIG_FAILED – the state stays unsigned.
        int error = Rcode.FORMERR;
        msg.setTSIG(mockTsig, error, mockQueryTsig);
        assertEquals(Message.TSIG_UNSIGNED, msg.tsigState,
                "setTSIG(TSIG, error, query) must leave tsigState as TSIG_UNSIGNED");
        assertFalse(msg.isSigned(),
                "Message should not be considered signed when tsigState is TSIG_UNSIGNED");
    }
}
```