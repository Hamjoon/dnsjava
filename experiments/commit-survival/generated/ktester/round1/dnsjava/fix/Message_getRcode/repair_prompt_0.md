


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class Message_getRcode_Test {

    private Message msg;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test; the default constructor creates a Header with a random ID.
        msg = new Message();
    }

    /**
     * Supplies the data from the JSON test matrix. Each entry contains:
     * <ul>
     *   <li>headerRcode – the value returned by {@code Header.getRcode()}</li>
     *   <li>optExtended – the value returned by {@code OPTRecord.getExtendedRcode()}</li>
     *   <li>expected – the expected result of {@code Message.getRcode()}</li>
     *   <li>description – human readable explanation (used only for test naming)</li>
     * </ul>
     */
    private static Stream<Arguments> rcodeMatrix() {
        return Stream.of(// group: Message.getRcode
        Arguments.of(0, null, 0, "Header 0, no OPT → 0"), Arguments.of(3, null, 3, "Header 3, no OPT → 3"), Arguments.of(0, 1, 16, "Header 0, extended 1 → 1<<4 = 16"), Arguments.of(2, 5, 82, "Header 2, extended 5 → 5<<4 + 2 = 82"), Arguments.of(15, 15, 255, "Header 15, extended 15 → 15<<4 + 15 = 255"), Arguments.of(20, 20, 340, "Header 20, extended 20 → 20<<4 + 20 = 340"), Arguments.of(-5, null, -5, "Header -5, no OPT → -5"), // group: Message.getRcode - additional and edge cases
        Arguments.of(7, 0, 7, "Header 7, extended 0 → unchanged"), Arguments.of(255, 255, 4335, "Header 255, extended 255 → 255 + (255<<4) = 4335"), Arguments.of(-3, 2, 29, "Header -3, extended 2 → -3 + (2<<4) = 29"), Arguments.of(Integer.MAX_VALUE, 1, // overflow expected
        Integer.MAX_VALUE + (1 << 4), "Header MAX_INT, extended 1 → overflow"), Arguments.of(10, -1, -6, "Header 10, extended -1 → 10 + (-1<<4) = -6"));
    }

    /**
     * Verify that getRcode() returns the header's rcode when no OPT record is present.
     */
    @Test
    void getRcode_withoutOPT_returnsHeaderRcode() {
        Header header = msg.getHeader();
        // set a known rcode (e.g., SERVFAIL)
        header.setRcode(Rcode.SERVFAIL);
        assertEquals(Rcode.SERVFAIL, header.getRcode(), "Header rcode should have been set to SERVFAIL");
        int rcode = msg.getRcode();
        assertEquals(Rcode.SERVFAIL, rcode, "Message.getRcode() must return the header rcode when no OPT record exists");
    }

    /**
     * Verify that getRcode() adds the extended rcode from an OPT record (shifted left by 4 bits)
     * to the header's rcode.
     */
    @Test
    void getRcode_withOPT_addsExtendedRcode() {
        Header header = msg.getHeader();
        // Use a base rcode that is easy to verify (e.g., NOERROR = 0)
        header.setRcode(Rcode.NOERROR);
        // Create an OPT record with an extended rcode of 5 (binary 0101)
        // The constructor arguments are: payloadSize, extendedRcode, version, flags
        OPTRecord opt = new OPTRecord(512, 5, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        // extendedRcode shifted left 4 bits
        int expected = (5 << 4) + Rcode.NOERROR;
        int actual = msg.getRcode();
        assertEquals(expected, actual, "Message.getRcode() should combine header rcode and extended rcode from OPT");
    }

    /**
     * Verify that when both header rcode and extended rcode are non‑zero,
     * they are combined correctly.
     */
    @Test
    void getRcode_withOPT_andHeaderRcode_combinesBoth() {
        Header header = msg.getHeader();
        // 1
        header.setRcode(Rcode.FORMERR);
        // extended rcode = 3 (binary 0011)
        OPTRecord opt = new OPTRecord(1024, 3, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        // (3*16) + 1 = 49
        int expected = (3 << 4) + Rcode.FORMERR;
        int actual = msg.getRcode();
        assertEquals(expected, actual, "Message.getRcode() must correctly combine header rcode and extended rcode");
    }

    /**
     * Verify that an OPT record with an extended rcode of zero does not affect the result.
     */
    @Test
    void getRcode_withOPT_extendedZero_isUnchanged() {
        Header header = msg.getHeader();
        // 4
        header.setRcode(Rcode.NOTIMP);
        OPTRecord opt = new OPTRecord(256, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        int actual = msg.getRcode();
        assertEquals(Rcode.NOTIMP, actual, "When OPT extended rcode is zero, Message.getRcode() should equal the header rcode");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("rcodeMatrix")
    void getRcode_parameterised(int headerRcode, Integer optExtended, int expected, String _desc) {
        // -----------------------------------------------------------------
        // Build a spy of Message so we can stub getHeader() and getOPT()
        // -----------------------------------------------------------------
        Message spyMsg = Mockito.spy(new Message());
        // ----- Header ----------------------------------------------------
        Header mockHeader = mock(Header.class);
        when(mockHeader.getRcode()).thenReturn(headerRcode);
        doReturn(mockHeader).when(spyMsg).getHeader();
        // ----- OPTRecord ------------------------------------------------
        if (optExtended != null) {
            OPTRecord mockOpt = mock(OPTRecord.class);
            when(mockOpt.getExtendedRcode()).thenReturn(optExtended);
            doReturn(mockOpt).when(spyMsg).getOPT();
        } else {
            // No OPT record
            doReturn(null).when(spyMsg).getOPT();
        }
        // -----------------------------------------------------------------
        // Execute and verify
        // -----------------------------------------------------------------
        int actual = spyMsg.getRcode();
        assertEquals(expected, actual, () -> "Unexpected rcode for header=" + headerRcode + ", optExtended=" + optExtended);
    }

    @Test
    void getRcode_headerIsNull_throwsNPE() throws Exception {
        // Use reflection to set the private 'header' field to null.
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(msg, null);
        // The method should now throw NPE when trying to call header.getRcode()
        assertThrows(NullPointerException.class, msg::getRcode, "Calling getRcode() with a null Header must throw NullPointerException");
    }

    @Test
    void getRcode_optExtendedThrows_propagatesException() {
        // Spy the message to inject a mock OPTRecord that throws.
        Message spyMsg = Mockito.spy(new Message());
        // Normal header returning 0
        Header mockHeader = mock(Header.class);
        when(mockHeader.getRcode()).thenReturn(0);
        doReturn(mockHeader).when(spyMsg).getHeader();
        // OPTRecord whose getExtendedRcode throws IllegalStateException
        OPTRecord badOpt = mock(OPTRecord.class);
        when(badOpt.getExtendedRcode()).thenThrow(new IllegalStateException("bad opt"));
        doReturn(badOpt).when(spyMsg).getOPT();
        IllegalStateException ex = assertThrows(IllegalStateException.class, spyMsg::getRcode, "Exception from OPTRecord.getExtendedRcode() must propagate");
        assertEquals("bad opt", ex.getMessage());
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/commit-survival/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]

╷
├─ JUnit Jupiter ✔
│  └─ Message_getRcode_Test ✔
│     ├─ getRcode_withOPT_extendedZero_isUnchanged() ✔
│     ├─ getRcode_withOPT_addsExtendedRcode() ✔
│     ├─ getRcode_withoutOPT_returnsHeaderRcode() ✔
│     ├─ getRcode_optExtendedThrows_propagatesException() ✘ 
│     │        Unnecessary stubbings detected.
│     │        Clean & maintainable test code requires zero unnecessary code.
│     │        Following stubbings are unnecessary (click to navigate to relevant line of code):
│     │          1. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_optExtendedThrows_propagatesException(Message_getRcode_Test.java:148)
│     │          2. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_optExtendedThrows_propagatesException(Message_getRcode_Test.java:149)
│     │        Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
│     ├─ getRcode_withOPT_andHeaderRcode_combinesBoth() ✔
│     ├─ getRcode_parameterised(int, Integer, int, String) ✔
│     │  ├─ Header 0, no OPT → 0 ✘ 
│     │  │        Unnecessary stubbings detected.
│     │  │        Clean & maintainable test code requires zero unnecessary code.
│     │  │        Following stubbings are unnecessary (click to navigate to relevant line of code):
│     │  │          1. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:114)
│     │  │          2. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:115)
│     │  │        Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
│     │  ├─ Header 3, no OPT → 3 ✘ Unexpected rcode for header=3, optExtended=null ==> expected: <3> but was: <0>
│     │  ├─ Header 0, extended 1 → 1<<4 = 16 ✘ 
│     │  │        Unnecessary stubbings detected.
│     │  │        Clean & maintainable test code requires zero unnecessary code.
│     │  │        Following stubbings are unnecessary (click to navigate to relevant line of code):
│     │  │          1. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:114)
│     │  │          2. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:115)
│     │  │        Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
│     │  ├─ Header 2, extended 5 → 5<<4 + 2 = 82 ✘ Unexpected rcode for header=2, optExtended=5 ==> expected: <82> but was: <80>
│     │  ├─ Header 15, extended 15 → 15<<4 + 15 = 255 ✘ Unexpected rcode for header=15, optExtended=15 ==> expected: <255> but was: <240>
│     │  ├─ Header 20, extended 20 → 20<<4 + 20 = 340 ✘ Unexpected rcode for header=20, optExtended=20 ==> expected: <340> but was: <320>
│     │  ├─ Header -5, no OPT → -5 ✘ Unexpected rcode for header=-5, optExtended=null ==> expected: <-5> but was: <0>
│     │  ├─ Header 7, extended 0 → unchanged ✘ Unexpected rcode for header=7, optExtended=0 ==> expected: <7> but was: <0>
│     │  ├─ Header 255, extended 255 → 255 + (255<<4) = 4335 ✘ Unexpected rcode for header=255, optExtended=255 ==> expected: <4335> but was: <4080>
│     │  ├─ Header -3, extended 2 → -3 + (2<<4) = 29 ✘ Unexpected rcode for header=-3, optExtended=2 ==> expected: <29> but was: <32>
│     │  ├─ Header MAX_INT, extended 1 → overflow ✘ Unexpected rcode for header=2147483647, optExtended=1 ==> expected: <-2147483633> but was: <16>
│     │  └─ Header 10, extended -1 → 10 + (-1<<4) = -6 ✘ Unexpected rcode for header=10, optExtended=-1 ==> expected: <-6> but was: <-16>
│     └─ getRcode_headerIsNull_throwsNPE() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (13):
  JUnit Jupiter:Message_getRcode_Test:getRcode_optExtendedThrows_propagatesException()
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_optExtendedThrows_propagatesException', methodParameterTypes = '']
    => org.mockito.exceptions.misusing.UnnecessaryStubbingException: 
Unnecessary stubbings detected.
Clean & maintainable test code requires zero unnecessary code.
Following stubbings are unnecessary (click to navigate to relevant line of code):
  1. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_optExtendedThrows_propagatesException(Message_getRcode_Test.java:148)
  2. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_optExtendedThrows_propagatesException(Message_getRcode_Test.java:149)
Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
       org.mockito.junit.jupiter.MockitoExtension.afterEach(MockitoExtension.java:192)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAfterEachCallbacks$12(TestMethodTestDescriptor.java:261)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAllAfterMethodsOrCallbacks$13(TestMethodTestDescriptor.java:277)
       org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAllAfterMethodsOrCallbacks$14(TestMethodTestDescriptor.java:277)
       org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder(CollectionUtils.java:217)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeAllAfterMethodsOrCallbacks(TestMethodTestDescriptor.java:276)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeAfterEachCallbacks(TestMethodTestDescriptor.java:260)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:145)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header 0, no OPT → 0
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.mockito.exceptions.misusing.UnnecessaryStubbingException: 
Unnecessary stubbings detected.
Clean & maintainable test code requires zero unnecessary code.
Following stubbings are unnecessary (click to navigate to relevant line of code):
  1. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:114)
  2. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:115)
Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
       org.mockito.junit.jupiter.MockitoExtension.afterEach(MockitoExtension.java:192)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAfterEachCallbacks$12(TestMethodTestDescriptor.java:261)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAllAfterMethodsOrCallbacks$13(TestMethodTestDescriptor.java:277)
       org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAllAfterMethodsOrCallbacks$14(TestMethodTestDescriptor.java:277)
       org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder(CollectionUtils.java:217)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeAllAfterMethodsOrCallbacks(TestMethodTestDescriptor.java:276)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeAfterEachCallbacks(TestMethodTestDescriptor.java:260)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:145)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header 3, no OPT → 3
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=3, optExtended=null ==> expected: <3> but was: <0>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header 0, extended 1 → 1<<4 = 16
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.mockito.exceptions.misusing.UnnecessaryStubbingException: 
Unnecessary stubbings detected.
Clean & maintainable test code requires zero unnecessary code.
Following stubbings are unnecessary (click to navigate to relevant line of code):
  1. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:114)
  2. -> at org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:115)
Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
       org.mockito.junit.jupiter.MockitoExtension.afterEach(MockitoExtension.java:192)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAfterEachCallbacks$12(TestMethodTestDescriptor.java:261)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAllAfterMethodsOrCallbacks$13(TestMethodTestDescriptor.java:277)
       org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAllAfterMethodsOrCallbacks$14(TestMethodTestDescriptor.java:277)
       org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder(CollectionUtils.java:217)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeAllAfterMethodsOrCallbacks(TestMethodTestDescriptor.java:276)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeAfterEachCallbacks(TestMethodTestDescriptor.java:260)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:145)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header 2, extended 5 → 5<<4 + 2 = 82
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=2, optExtended=5 ==> expected: <82> but was: <80>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header 15, extended 15 → 15<<4 + 15 = 255
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=15, optExtended=15 ==> expected: <255> but was: <240>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header 20, extended 20 → 20<<4 + 20 = 340
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=20, optExtended=20 ==> expected: <340> but was: <320>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header -5, no OPT → -5
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=-5, optExtended=null ==> expected: <-5> but was: <0>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header 7, extended 0 → unchanged
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=7, optExtended=0 ==> expected: <7> but was: <0>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header 255, extended 255 → 255 + (255<<4) = 4335
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=255, optExtended=255 ==> expected: <4335> but was: <4080>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header -3, extended 2 → -3 + (2<<4) = 29
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=-3, optExtended=2 ==> expected: <29> but was: <32>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header MAX_INT, extended 1 → overflow
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=2147483647, optExtended=1 ==> expected: <-2147483633> but was: <16>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getRcode_Test:getRcode_parameterised(int, Integer, int, String):Header 10, extended -1 → 10 + (-1<<4) = -6
    MethodSource [className = 'org.xbill.DNS.Message_getRcode_Test', methodName = 'getRcode_parameterised', methodParameterTypes = 'int, java.lang.Integer, int, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected rcode for header=10, optExtended=-1 ==> expected: <-6> but was: <-16>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:156)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:598)
       org.xbill.DNS.Message_getRcode_Test.getRcode_parameterised(Message_getRcode_Test.java:129)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 638 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        18 tests found           ]
[         0 tests skipped         ]
[        18 tests started         ]
[         0 tests aborted         ]
[         5 tests successful      ]
[        13 tests failed          ]


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
 

api document of method getRcode(): 
 Returns the message's rcode (error code). This incorporates the EDNS extended rcode. 

return type: 
int

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	related functions:
		method `public String toString()` returns `java.lang.String`, related with `org.xbill.DNS.Message.getRcode()`, api document:  Converts the Message to a String. ;
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.getRcode()`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public void setHeader(Header)` returns `void`, related with `org.xbill.DNS.Message.getRcode()`, api document: 
   * Replaces the Header with a new one.
   *
   * @see Header
   ;
class `org.xbill.DNS.SimpleResolver`:
	related functions:
		method `private void applyEDNS(Message)` returns `void`, related with `org.xbill.DNS.Message.getRcode()`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
