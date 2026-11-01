
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.mockito.Mock;
import java.lang.reflect.Field;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;

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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_isVerified_Test.java:10: error: ';' expected
import org.junit.jupiter.params.provider.MethodSource
                                                     ^
1 error

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method isVerified()': '\n   * If this message was signed by a TSIG, was the TSIG verified?\n   *\n   * @see TSIG\n   ', 'return type': 'boolean', 'dependent classes': "class `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Message.tsigState;\n\t\tint org.xbill.DNS.Message.TSIG_VERIFIED;\n\trelated functions:\n\t\tmethod `public boolean isVerified()` returns `boolean`, related with `org.xbill.DNS.Message.isVerified()`, api document: \n   * If this message was signed by a TSIG, was the TSIG verified?\n   *\n   * @see TSIG\n   ;\n\t\tmethod `public boolean isSigned()` returns `boolean`, related with `org.xbill.DNS.Message.isVerified()`, api document: \n   * Was this message signed by a TSIG?\n   *\n   * @see TSIG\n   ;\nclass `org.xbill.DNS.TSIG`:\n\trelated functions:\n\t\tmethod `public void apply(Message, int, TSIGRecord, boolean)` returns `void`, related with `org.xbill.DNS.Message.isVerified()`, api document: \n   * Generates a TSIG record with a specific error for a message and adds it to the message.\n   *\n   * @param m The message\n   * @param error The error\n   * @param old If this message is a response, the TSIG from the request\n   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and\n   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for\n   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).\n   * @since 3.2\n   ;\n\t\tmethod `private int verify(Message, byte[], TSIGRecord, boolean, Mac)` returns `int`, related with `org.xbill.DNS.Message.isVerified()`, api document: \n   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a\n   * TSIG is expected to be present, it is an error if one is not present. After calling this\n   * routine, Message.isVerified() may be called on this message.\n   *\n   * @param m The message to verify\n   * @param messageBytes An array containing the message in unparsed form. This is necessary since\n   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with\n   *     the same name compression).\n   * @param requestTSIG If this message is a response, the TSIG from the request\n   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and\n   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for\n   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).\n   * @return The result of the verification (as an Rcode)\n   * @see Rcode\n   ;\nclass `org.xbill.DNS.TSIG.StreamGenerator`:\n\trelated functions:\n\t\tmethod ` void generate(Message, boolean)` returns `void`, related with `org.xbill.DNS.Message.isVerified()`;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.isVerified()`, api document:  Converts a numeric extended flag into a String ;"}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


