
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

@ExtendWith(MockitoExtension.class)
class Message_isSigned_Test {

    /**
     * The {@link Message} instance under test.
     */
    private Message message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.Mock;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.AfterEach;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;

    /**
     * A mock {@link TSIG} used to trigger the signed state without needing a real key.
     */
    @Mock
    private TSIG mockTsig;

    /**
     * No TSIG attached.
     */
    public static final int TSIG_UNSIGNED = 0;

    /**
     * TSIG attached and verification succeeded.
     */
    public static final int TSIG_VERIFIED = 1;

    /**
     * Intermediate state used while a TSIG is being generated.
     */
    public static final int TSIG_INTERMEDIATE = 2;

    /**
     * TSIG attached but not yet verified.
     */
    public static final int TSIG_SIGNED = 3;

    /**
     * TSIG attached and verification failed.
     */
    public static final int TSIG_FAILED = 4;

    /**
     * The TSIG object (may be {@code null}).
     */
    private TSIG tsig;

    /**
     * Current TSIG state – package‑private so the test can reach it via reflection.
     */
    int tsigState = TSIG_UNSIGNED;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test case.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the Message instance will be garbage‑collected.
        message = null;
    }

    /**
     * Supplies the test data for {@link #isSigned_variousStates(int, boolean, String)}.
     *
     * The data mirrors the JSON test cases supplied in the task description.
     */
    private static Stream<Arguments> provideTsigStateCases() {
        return // Normal defined states
        Stream.// Normal defined states
        of(// Undefined but within int range
        arguments(0, false, "TSIG_UNSIGNED (0) → unsigned"), // Undefined but within int range
        arguments(2, false, "TSIG_INTERMEDIATE (2) → unsigned"), // Undefined but within int range
        arguments(3, true, "TSIG_SIGNED (3) → signed"), // Undefined but within int range
        arguments(1, true, "TSIG_VERIFIED (1) → signed"), // Undefined but within int range
        arguments(4, true, "TSIG_FAILED (4) → signed"), // Edge cases
        arguments(99, false, "Undefined value (99) → treated as unsigned"), arguments(-1, false, "Negative value (-1) → unsigned"), arguments(5, false, "Just above highest defined constant (5) → unsigned"), arguments(Integer.MAX_VALUE, false, "Integer.MAX_VALUE → unsigned"), arguments(Integer.MIN_VALUE, false, "Integer.MIN_VALUE → unsigned"));
    }

    /**
     * Attach a TSIG record to this message. The message is now considered
     * {@code TSIG_SIGNED}.
     *
     * @param tsig the TSIG object to attach
     */
    public void setTSIG(TSIG tsig) {
        this.tsig = tsig;
        /* When a TSIG is attached without an explicit verification step we
         * treat the message as signed. */
        this.tsigState = TSIG_SIGNED;
    }

    /**
     * Attach a TSIG record together with a verification result.
     *
     * @param tsig  the TSIG object to attach
     * @param error the verification result (Rcode)
     * @param old   the TSIG from the request, if this is a response (may be {@code null})
     */
    public void setTSIG(TSIG tsig, int error, TSIGRecord old) {
        this.tsig = tsig;
        if (error == Rcode.NOERROR) {
            /* Successful verification – the message is in the VERIFIED state. */
            this.tsigState = TSIG_VERIFIED;
        } else {
            /* Any non‑zero error means verification failed, but the message
             * is still considered signed. */
            this.tsigState = TSIG_FAILED;
        }
    }

    /**
     * Was this message signed by a TSIG?
     *
     * @return {@code true} for any state other than {@code TSIG_UNSIGNED}
     *         or {@code TSIG_INTERMEDIATE}
     */
    public boolean isSigned() {
        return tsigState != TSIG_UNSIGNED && tsigState != TSIG_INTERMEDIATE;
    }

    /**
     * If this message was signed by a TSIG, was the TSIG verified?
     *
     * @return {@code true} only when the state is {@code TSIG_VERIFIED}
     */
    public boolean isVerified() {
        return tsigState == TSIG_VERIFIED;
    }

    /**
     * By default a newly constructed {@link Message} has {@code tsigState == TSIG_UNSIGNED},
     * therefore {@link Message#isSigned()} must return {@code false}.
     */
    @Test
    void isSigned_defaultIsUnsigned() {
        assertFalse(message.isSigned(), "A newly created Message should not be considered signed");
    }

    /**
     * Adding a TSIG record via {@link Message#setTSIG(TSIG)} puts the message into the
     * {@code TSIG_SIGNED} state, which makes {@code isSigned()} return {@code true}.
     */
    @Test
    void isSigned_afterSetTSIG_returnsTrue() {
        // The mock TSIG is sufficient – the implementation only stores the reference
        // and updates the internal state.
        message.setTSIG(mockTsig);
        assertTrue(message.isSigned(), "Message should be signed after a TSIG record has been attached");
    }

    /**
     * When {@link Message#setTSIG(TSIG, int, TSIGRecord)} is called with a non‑zero error
     * code, the internal state becomes {@code TSIG_FAILED}.  The method should still
     * report the message as signed.
     */
    @Test
    void isSigned_afterSetTSIGWithError_returnsTrue() {
        // Use a known error code (e.g., FORMERR) to simulate a failed verification.
        int errorCode = Rcode.FORMERR;
        message.setTSIG(mockTsig, errorCode, null);
        assertTrue(message.isSigned(), "Message should be considered signed even when TSIG verification fails");
    }

    /**
     * When a TSIG is verified successfully via {@link Message#setTSIG(TSIG, int, TSIGRecord)}
     * with an error code of {@code Rcode.NOERROR}, the state becomes {@code TSIG_VERIFIED},
     * and {@code isSigned()} must return {@code true}.
     */
    @Test
    void isSigned_afterSuccessfulVerification_returnsTrue() {
        int errorCode = Rcode.NOERROR;
        message.setTSIG(mockTsig, errorCode, null);
        assertTrue(message.isSigned(), "Message should be considered signed after successful TSIG verification");
    }

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
    @ParameterizedTest(name = "[{index}] tsigState={0} → expected={1}")
    @MethodSource("provideTsigStateCases")
    void isSigned_variousStates(int state, boolean expected, String description) throws Exception {
        // Set the internal tsigState field via reflection.
        Field field = Message.class.getDeclaredField("tsigState");
        field.setAccessible(true);
        field.setInt(message, state);
        assertEquals(expected, message.isSigned(), () -> "isSigned() with tsigState=" + state + ": " + description);
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:10: error: illegal start of type
import org.junit.jupiter.api.BeforeEach;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:10: error: <identifier> expected
import org.junit.jupiter.api.BeforeEach;
                                       ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:11: error: illegal start of type
import org.junit.jupiter.params.provider.MethodSource
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:11: error: <identifier> expected
import org.junit.jupiter.params.provider.MethodSource
                                                     ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:12: error: <identifier> expected
import org.junit.jupiter.api.extension.ExtendWith;
                                                 ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:13: error: illegal start of type
import java.util.stream.Stream;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:13: error: <identifier> expected
import java.util.stream.Stream;
                              ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:14: error: illegal start of type
import org.junit.jupiter.params.provider.Arguments;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:14: error: <identifier> expected
import org.junit.jupiter.params.provider.Arguments;
                                                  ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:15: error: illegal start of type
import org.junit.jupiter.api.Test;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:15: error: <identifier> expected
import org.junit.jupiter.api.Test;
                                 ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:16: error: illegal start of type
import org.mockito.*;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:16: error: <identifier> expected
import org.mockito.*;
                   ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:17: error: illegal start of type
import org.mockito.Mock;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:17: error: <identifier> expected
import org.mockito.Mock;
                       ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:18: error: illegal start of type
import org.junit.jupiter.api.*;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:18: error: <identifier> expected
import org.junit.jupiter.api.*;
                             ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:19: error: illegal start of type
import org.junit.jupiter.api.AfterEach;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:19: error: <identifier> expected
import org.junit.jupiter.api.AfterEach;
                                      ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:20: error: illegal start of type
import org.mockito.junit.jupiter.MockitoExtension;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:20: error: <identifier> expected
import org.mockito.junit.jupiter.MockitoExtension;
                                                 ^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:21: error: illegal start of type
import org.junit.jupiter.params.ParameterizedTest;
^
src/test/java/org/xbill/DNS/Message_isSigned_Test.java:21: error: <identifier> expected
import org.junit.jupiter.params.ParameterizedTest;
                                                 ^
23 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method isSigned()': '\n   * Was this message signed by a TSIG?\n   *\n   * @see TSIG\n   ', 'return type': 'boolean', 'dependent classes': "class `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Message.TSIG_FAILED;\n\t\tint org.xbill.DNS.Message.TSIG_VERIFIED;\n\t\tint org.xbill.DNS.Message.tsigState;\n\t\tint org.xbill.DNS.Message.TSIG_SIGNED;\n\trelated functions:\n\t\tmethod `public boolean isVerified()` returns `boolean`, related with `org.xbill.DNS.Message.isSigned()`, api document: \n   * If this message was signed by a TSIG, was the TSIG verified?\n   *\n   * @see TSIG\n   ;\n\t\tmethod `public boolean isSigned()` returns `boolean`, related with `org.xbill.DNS.Message.isSigned()`, api document: \n   * Was this message signed by a TSIG?\n   *\n   * @see TSIG\n   ;\nclass `org.xbill.DNS.TSIG`:\n\trelated functions:\n\t\tmethod `public void apply(Message, int, TSIGRecord, boolean)` returns `void`, related with `org.xbill.DNS.Message.isSigned()`, api document: \n   * Generates a TSIG record with a specific error for a message and adds it to the message.\n   *\n   * @param m The message\n   * @param error The error\n   * @param old If this message is a response, the TSIG from the request\n   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and\n   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for\n   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).\n   * @since 3.2\n   ;\n\t\tmethod `private int verify(Message, byte[], TSIGRecord, boolean, Mac)` returns `int`, related with `org.xbill.DNS.Message.isSigned()`, api document: \n   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a\n   * TSIG is expected to be present, it is an error if one is not present. After calling this\n   * routine, Message.isVerified() may be called on this message.\n   *\n   * @param m The message to verify\n   * @param messageBytes An array containing the message in unparsed form. This is necessary since\n   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with\n   *     the same name compression).\n   * @param requestTSIG If this message is a response, the TSIG from the request\n   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and\n   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for\n   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).\n   * @return The result of the verification (as an Rcode)\n   * @see Rcode\n   ;\nclass `org.xbill.DNS.TSIG.StreamGenerator`:\n\trelated functions:\n\t\tmethod ` void generate(Message, boolean)` returns `void`, related with `org.xbill.DNS.Message.isSigned()`;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.isSigned()`, api document:  Converts a numeric extended flag into a String ;"}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


