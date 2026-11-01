```java
package org.xbill.DNS;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link Message#isVerified()}.
 *
 * <p>These tests are written with JUnit 4 to avoid version‑mismatch problems that can
 * arise when the JUnit 5 platform is not available on the class‑path.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class Message_isVerified_Test {

    /** The {@link Message} instance used in each test. */
    private Message message;

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
     * By default a newly created {@link Message} has {@code tsigState == TSIG_UNSIGNED},
     * therefore {@code isVerified()} must return {@code false}.
     */
    @Test
    public void isVerified_defaultMessage_returnsFalse() {
        Assert.assertFalse(
                "A freshly constructed Message should not be verified",
                message.isVerified());
    }

    /**
     * When a TSIG is attached with a non‑zero error code the internal state is
     * set to {@code TSIG_FAILED}. In that situation {@code isVerified()} must
     * still return {@code false}.
     */
    @Test
    public void isVerified_tsigSignedWithError_returnsFalse(@Mock TSIG mockTsig) {
        // Rcode.FORMERR (1) is a typical error code indicating a bad TSIG.
        message.setTSIG(mockTsig, Rcode.FORMERR, null);
        Assert.assertFalse(
                "Message with a TSIG error should not be verified",
                message.isVerified());
    }

    /**
     * When a TSIG is attached with {@code Rcode.NOERROR} the Message marks the
     * TSIG as verified. Consequently {@code isVerified()} must return {@code true}.
     */
    @Test
    public void isVerified_tsigVerified_returnsTrue(@Mock TSIG mockTsig) {
        // Rcode.NOERROR (0) signals a successful verification.
        message.setTSIG(mockTsig, Rcode.NOERROR, null);
        Assert.assertTrue(
                "Message with a successfully verified TSIG should be verified",
                message.isVerified());
    }

    /**
     * Verifies that {@code isVerified()} returns {@code true} only when
     * {@code tsigState == TSIG_VERIFIED}.
     */
    @Test
    public void isVerified_trueBranch() {
        // TSIG_VERIFIED constant is defined as 1 in Message
        setTsigState(1);
        Assert.assertTrue(
                "When tsigState == TSIG_VERIFIED, isVerified() should be true",
                message.isVerified());
    }

    /**
     * Verifies that {@code isVerified()} returns {@code false} for a range of
     * non‑verified or out‑of‑range {@code tsigState} values.
     */
    @Test
    public void isVerified_falseBranches() {
        List<Integer> nonVerifiedStates = Arrays.asList(
                0, // TSIG_UNSIGNED
                2, // TSIG_INTERMEDIATE
                3, // TSIG_SIGNED
                4, // TSIG_FAILED
                -1,
                5
        );

        for (int state : nonVerifiedStates) {
            setTsigState(state);
            Assert.assertFalse(
                    "When tsigState == " + state + ", isVerified() should be false",
                    message.isVerified());
        }
    }

    @Test
    public void isVerified_idempotent_verifiedState() {
        setTsigState(1); // TSIG_VERIFIED
        Assert.assertTrue(message.isVerified());
        Assert.assertTrue(message.isVerified()); // second call must still be true
    }

    @Test
    public void isVerified_idempotent_unverifiedState() {
        setTsigState(0); // TSIG_UNSIGNED
        Assert.assertFalse(message.isVerified());
        Assert.assertFalse(message.isVerified()); // second call must still be false
    }
}
```