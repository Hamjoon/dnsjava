```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#isSigned()}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All defined {@code tsigState} constants.</li>
 *   <li>Unexpected/edge values (negative, extreme ints, values not defined).</li>
 *   <li>State transitions triggered by {@code setTSIG(...)} overloads.</li>
 * </ul>
 *
 * <p>Only public API is used; the {@code tsigState} field is package‑private,
 * therefore it can be set directly in the test without reflection.
 */
@ExtendWith(MockitoExtension.class)   // Keeps the template; mocks are used for TSIG related tests
class Message_isSigned_Test {

    private Message msg;

    @Mock
    private TSIG mockTsig;          // Used for setTSIG(TSIG) tests

    @Mock
    private TSIGRecord mockQueryTsig; // Used for setTSIG(TSIG, int, TSIGRecord) tests

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for each test; default constructor leaves tsigState == TSIG_UNSIGNED (0)
        msg = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        msg = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the officially defined TSIG states                         */
    /* --------------------------------------------------------------------- */

    @Test
    void isSigned_unsigned_returnsFalse() {
        // Default state is TSIG_UNSIGNED (0)
        assertFalse(msg.isSigned(),
                "Message should not be signed when tsigState is TSIG_UNSIGNED");
    }

    @Test
    void isSigned_intermediate_returnsFalse() {
        msg.tsigState = Message.TSIG_INTERMEDIATE; // value 2
        assertFalse(msg.isSigned(),
                "Message should not be signed when tsigState is TSIG_INTERMEDIATE");
    }

    @Test
    void isSigned_signed_returnsTrue() {
        msg.tsigState = Message.TSIG_SIGNED; // value 3
        assertTrue(msg.isSigned(),
                "Message should be signed when tsigState is TSIG_SIGNED");
    }

    @Test
    void isSigned_verified_returnsTrue() {
        msg.tsigState = Message.TSIG_VERIFIED; // value 1
        assertTrue(msg.isSigned(),
                "Message should be signed when tsigState is TSIG_VERIFIED");
    }

    @Test
    void isSigned_failed_returnsTrue() {
        msg.tsigState = Message.TSIG_FAILED; // value 4
        assertTrue(msg.isSigned(),
                "Message should be considered signed when tsigState is TSIG_FAILED");
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case / unexpected tsigState values                               */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /*  State changes caused by the public setTSIG overloads                  */
    /* --------------------------------------------------------------------- */

    @Test
    void isSigned_afterSetTSIG_key_returnsTrue() {
        // setTSIG(TSIG) should transition the message to TSIG_SIGNED
        msg.setTSIG(mockTsig);
        assertEquals(Message.TSIG_SIGNED, msg.tsigState,
                "setTSIG(TSIG) must set tsigState to TSIG_SIGNED");
        assertTrue(msg.isSigned(),
                "Message should be signed after setTSIG(TSIG) call");
    }

    @Test
    void isSigned_afterSetTSIG_keyError_queryTsig_returnsTrueWhenFailed() {
        // Simulate an error condition that forces TSIG_FAILED.
        // According to the API, a non‑zero error leads to TSIG_FAILED.
        int error = Rcode.FORMERR; // any non‑zero Rcode
        msg.setTSIG(mockTsig, error, mockQueryTsig);
        assertEquals(Message.TSIG_FAILED, msg.tsigState,
                "setTSIG(TSIG, error, query) with error != 0 must set tsigState to TSIG_FAILED");
        assertTrue(msg.isSigned(),
                "Message should be considered signed when tsigState is TSIG_FAILED");
    }
}
```