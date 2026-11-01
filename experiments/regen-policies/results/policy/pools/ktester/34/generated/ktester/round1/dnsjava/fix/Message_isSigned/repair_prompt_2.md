


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for {@link Message#isSigned()} and related TSIG state handling.
 */
@ExtendWith(MockitoExtension.class)
class Message_isSigned_Test {

    /**
     * The {@link Message} instance under test.
     */
    private Message message;

    /**
     * A mock {@link TSIG} used to trigger the signed state without needing a real key.
     */
    @Mock
    private TSIG mockTsig;

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
        return Stream.of(
                arguments(0, false, "TSIG_UNSIGNED (0) → unsigned"),
                arguments(2, false, "TSIG_INTERMEDIATE (2) → unsigned"),
                arguments(3, true, "TSIG_SIGNED (3) → signed"),
                arguments(1, true, "TSIG_VERIFIED (1) → signed"),
                arguments(4, true, "TSIG_FAILED (4) → signed"),
                arguments(99, false, "Undefined value (99) → treated as unsigned"),
                arguments(-1, false, "Negative value (-1) → unsigned"),
                arguments(5, false, "Just above highest defined constant (5) → unsigned"),
                arguments(Integer.MAX_VALUE, false, "Integer.MAX_VALUE → unsigned"),
                arguments(Integer.MIN_VALUE, false, "Integer.MIN_VALUE → unsigned")
        );
    }

    /**
     * By default a newly constructed {@link Message} has {@code tsigState == TSIG_UNSIGNED},
     * therefore {@link Message#isSigned()} must return {@code false}.
     */
    @Test
    void isSigned_defaultIsUnsigned() {
        assertFalse(message.isSigned(),
                "A newly created Message should not be considered signed");
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
        assertTrue(message.isSigned(),
                "Message should be signed after a TSIG record has been attached");
    }

    /**
     * When {@link Message#setTSIG(TSIG, int, TSIGRecord)} is called with a non‑zero error
     * code, the internal state becomes {@code TSIG_FAILED}. The method should still
     * report the message as signed.
     */
    @Test
    void isSigned_afterSetTSIGWithError_returnsTrue() {
        // Use a known error code (e.g., FORMERR) to simulate a failed verification.
        int errorCode = Rcode.FORMERR;
        message.setTSIG(mockTsig, errorCode, null);
        assertTrue(message.isSigned(),
                "Message should be considered signed even when TSIG verification fails");
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
        assertTrue(message.isSigned(),
                "Message should be considered signed after successful TSIG verification");
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
        assertEquals(expected, message.isSigned(),
                () -> "isSigned() with tsigState=" + state + ": " + description);
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/regen-policies/results/policy/pools/ktester/34/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]

╷
├─ JUnit Jupiter ✔
│  └─ Message_isSigned_Test ✔
│     ├─ isSigned_afterSetTSIGWithError_returnsTrue() ✘ Message should be considered signed even when TSIG verification fails ==> expected: <true> but was: <false>
│     ├─ isSigned_defaultIsUnsigned() ✔
│     ├─ isSigned_variousStates(int, boolean, String) ✔
│     │  ├─ [1] tsigState=0 → expected=false ✔
│     │  ├─ [2] tsigState=2 → expected=false ✔
│     │  ├─ [3] tsigState=3 → expected=true ✔
│     │  ├─ [4] tsigState=1 → expected=true ✔
│     │  ├─ [5] tsigState=4 → expected=true ✔
│     │  ├─ [6] tsigState=99 → expected=false ✔
│     │  ├─ [7] tsigState=-1 → expected=false ✔
│     │  ├─ [8] tsigState=5 → expected=false ✔
│     │  ├─ [9] tsigState=2147483647 → expected=false ✔
│     │  └─ [10] tsigState=-2147483648 → expected=false ✔
│     ├─ isSigned_afterSuccessfulVerification_returnsTrue() ✘ Message should be considered signed after successful TSIG verification ==> expected: <true> but was: <false>
│     └─ isSigned_afterSetTSIG_returnsTrue() ✘ Message should be signed after a TSIG record has been attached ==> expected: <true> but was: <false>
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (3):
  JUnit Jupiter:Message_isSigned_Test:isSigned_afterSetTSIGWithError_returnsTrue()
    MethodSource [className = 'org.xbill.DNS.Message_isSigned_Test', methodName = 'isSigned_afterSetTSIGWithError_returnsTrue', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Message should be considered signed even when TSIG verification fails ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_isSigned_Test.isSigned_afterSetTSIGWithError_returnsTrue(Message_isSigned_Test.java:101)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_isSigned_Test:isSigned_afterSuccessfulVerification_returnsTrue()
    MethodSource [className = 'org.xbill.DNS.Message_isSigned_Test', methodName = 'isSigned_afterSuccessfulVerification_returnsTrue', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Message should be considered signed after successful TSIG verification ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_isSigned_Test.isSigned_afterSuccessfulVerification_returnsTrue(Message_isSigned_Test.java:114)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_isSigned_Test:isSigned_afterSetTSIG_returnsTrue()
    MethodSource [className = 'org.xbill.DNS.Message_isSigned_Test', methodName = 'isSigned_afterSetTSIG_returnsTrue', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Message should be signed after a TSIG record has been attached ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_isSigned_Test.isSigned_afterSetTSIG_returnsTrue(Message_isSigned_Test.java:87)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 486 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        14 tests found           ]
[         0 tests skipped         ]
[        14 tests started         ]
[         0 tests aborted         ]
[        11 tests successful      ]
[         3 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class org.xbill.DNS.Message: 

 * A DNS Message. A message is the basic unit of communication between the client and server of a
 * DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 

api document of method isSigned(): 

   * Was this message signed by a TSIG?
   *
   * @see TSIG
   

return type: 
boolean

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		int org.xbill.DNS.Message.TSIG_FAILED;
		int org.xbill.DNS.Message.TSIG_VERIFIED;
		int org.xbill.DNS.Message.tsigState;
		int org.xbill.DNS.Message.TSIG_SIGNED;
	related functions:
		method `public boolean isVerified()` returns `boolean`, related with `org.xbill.DNS.Message.isSigned()`, api document: 
   * If this message was signed by a TSIG, was the TSIG verified?
   *
   * @see TSIG
   ;
		method `public boolean isSigned()` returns `boolean`, related with `org.xbill.DNS.Message.isSigned()`, api document: 
   * Was this message signed by a TSIG?
   *
   * @see TSIG
   ;
class `org.xbill.DNS.TSIG`:
	related functions:
		method `public void apply(Message, int, TSIGRecord, boolean)` returns `void`, related with `org.xbill.DNS.Message.isSigned()`, api document: 
   * Generates a TSIG record with a specific error for a message and adds it to the message.
   *
   * @param m The message
   * @param error The error
   * @param old If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and
   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for
   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).
   * @since 3.2
   ;
		method `private int verify(Message, byte[], TSIGRecord, boolean, Mac)` returns `int`, related with `org.xbill.DNS.Message.isSigned()`, api document: 
   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a
   * TSIG is expected to be present, it is an error if one is not present. After calling this
   * routine, Message.isVerified() may be called on this message.
   *
   * @param m The message to verify
   * @param messageBytes An array containing the message in unparsed form. This is necessary since
   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with
   *     the same name compression).
   * @param requestTSIG If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and
   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for
   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).
   * @return The result of the verification (as an Rcode)
   * @see Rcode
   ;
class `org.xbill.DNS.TSIG.StreamGenerator`:
	related functions:
		method ` void generate(Message, boolean)` returns `void`, related with `org.xbill.DNS.Message.isSigned()`;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.isSigned()`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
