


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;

class Message_findRRset_2_Test {

    private Message message;

    private Name testName;

    private static final int TYPE_A = Type.A;

    private static final int TYPE_TXT = Type.TXT;

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Fresh message and name for each test
        message = new Message();
        testName = Name.fromConstantString("example.com.");
    }

    private static Stream<Arguments> basicScenarioProvider() throws TextParseException {
        return Stream.of(// name, type, expected, description
        Arguments.of("example.com.", Type.A, true, "Record in ANSWER – first call returns true"), Arguments.of("example.org.", Type.MX, true, "Record only in AUTHORITY – second call returns true"), Arguments.of("example.net.", Type.TXT, true, "Record only in ADDITIONAL – third call returns true"), Arguments.of("nonexistent.example.", Type.SOA, false, "No matching record in any section"));
    }

    @Test
    void findRRset_presentInAnswer_returnsTrue() {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        boolean result = message.findRRset(testName, TYPE_A);
        assertTrue(result, "RRset should be found in the ANSWER section");
    }

    @Test
    void findRRset_presentInAuthority_returnsTrue() {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.AUTHORITY);
        boolean result = message.findRRset(testName, TYPE_A);
        assertTrue(result, "RRset should be found in the AUTHORITY section");
    }

    @Test
    void findRRset_presentInAdditional_returnsTrue() {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ADDITIONAL);
        boolean result = message.findRRset(testName, TYPE_A);
        assertTrue(result, "RRset should be found in the ADDITIONAL section");
    }

    @Test
    void findRRset_notPresent_returnsFalse() {
        boolean result = message.findRRset(testName, TYPE_A);
        assertFalse(result, "RRset should not be found when no matching record exists");
    }

    @Test
    void findRRset_wrongType_returnsFalse() {
        Record txtRecord = Record.newRecord(testName, TYPE_TXT, DClass.IN);
        message.addRecord(txtRecord, Section.ANSWER);
        boolean result = message.findRRset(testName, TYPE_A);
        assertFalse(result, "RRset should not be found when the type does not match");
    }

    @Test
    void findRRset_wrongName_returnsFalse() throws TextParseException {
        Name otherName = Name.fromConstantString("other.example.com.");
        Record aRecord = Record.newRecord(otherName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        boolean result = message.findRRset(testName, TYPE_A);
        assertFalse(result, "RRset should not be found when the name does not match");
    }

    @Test
    void findRRset_multipleSectionsOnlyOneMatches_returnsTrue() {
        Record aRecordAuth = Record.newRecord(testName, TYPE_A, DClass.IN);
        Record txtRecordAns = Record.newRecord(testName, TYPE_TXT, DClass.IN);
        message.addRecord(aRecordAuth, Section.AUTHORITY);
        message.addRecord(txtRecordAns, Section.ANSWER);
        boolean result = message.findRRset(testName, TYPE_A);
        assertTrue(result, "RRset should be found when it exists in any section");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("basicScenarioProvider")
    void findRRset_basicScenarios(String nameStr, int type, boolean expected, String _desc) throws Exception {
        Name name = Name.fromConstantString(nameStr);
        if (expected) {
            if (type == Type.A) {
                message.addRecord(Record.newRecord(name, type, DClass.IN), Section.ANSWER);
            } else if (type == Type.MX) {
                message.addRecord(Record.newRecord(name, type, DClass.IN), Section.AUTHORITY);
            } else if (type == Type.TXT) {
                message.addRecord(Record.newRecord(name, type, DClass.IN), Section.ADDITIONAL);
            }
        }
        boolean result = message.findRRset(name, type);
        assertEquals(expected, result);
    }

    @Test
    void findRRset_nullName_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> message.findRRset(null, Type.A), "Passing a null Name should throw NullPointerException");
    }

    @Test
    void findRRset_invalidNameMissingTrailingDot_throwsIllegalArgumentException() {
        // Name without trailing dot is not absolute – library throws IllegalArgumentException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Name("example.com"), "Name without trailing dot should cause IllegalArgumentException");
        // The exception is enough – no need to call findRRset
    }

    @Test
    void findRRset_invalidNameIllegalCharacters_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Name("exa$mple.com."), "Name containing illegal characters should cause IllegalArgumentException");
    }

    @Test
    void findRRset_invalidNameEmptyString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Name(""), "Empty string is not a valid DNS name");
    }

    @Test
    void findRRset_negativeType_returnsFalse() {
        // negative type is treated as non‑matching, no exception expected
        boolean result = message.findRRset(testName, -1);
        assertFalse(result, "Negative DNS type should result in false");
    }

    @Test
    void findRRset_typeGreaterThan65535_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(testName, 65536), "Type values > 65535 should cause IllegalArgumentException");
    }

    @Test
    void findRRset_typeNotAllowedInSection_throwsIllegalArgumentException() {
        // SIG is not allowed in standard sections in this implementation – expect IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(testName, Type.SIG), "SIG type should trigger IllegalArgumentException");
    }

    @Test
    void findRRset_shortCircuitStopsAfterAnswer() throws Exception {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        Message spy = Mockito.spy(message);
        boolean result = spy.findRRset(testName, TYPE_A);
        assertTrue(result);
        verify(spy, times(1)).findRRset(testName, TYPE_A, Section.ANSWER);
        verify(spy, never()).findRRset(testName, TYPE_A, Section.AUTHORITY);
        verify(spy, never()).findRRset(testName, TYPE_A, Section.ADDITIONAL);
    }

    @Test
    void findRRset_ANYTypeMatchesAnyRecord() {
        // According to dnsjava's current implementation, ANY does not act as a wildcard for findRRset
        Record mx = Record.newRecord(testName, Type.MX, DClass.IN);
        message.addRecord(mx, Section.ANSWER);
        boolean result = message.findRRset(testName, Type.ANY);
        assertFalse(result, "ANY type does not match arbitrary records in findRRset");
    }

    @Test
    void findRRset_nameCaseInsensitivity() {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        Name mixedCase = Name.fromConstantString("ExAmPlE.CoM.");
        boolean result = message.findRRset(mixedCase, TYPE_A);
        assertTrue(result, "DNS names are case‑insensitive; mixed case should still match");
    }

    @Test
    void findRRset_subdomainMismatch_returnsFalse() throws TextParseException {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        Name sub = Name.fromConstantString("sub.example.com.");
        boolean result = message.findRRset(sub, TYPE_A);
        assertFalse(result, "Exact name match is required; subdomain should not match parent record");
    }

    @Test
    void findRRset_emptyMessage_returnsFalse() {
        boolean result = message.findRRset(testName, TYPE_A);
        assertFalse(result);
    }

    @Test
    void findRRset_nullName_returnsFalse() {
        // The implementation returns false for a null name rather than throwing NPE
        boolean result = message.findRRset(null, Type.A);
        assertFalse(result, "Passing a null Name should return false");
    }

    @Test
    void findRRset_invalidNameMissingTrailingDot_throwsTextParseException() {
        // Name without trailing dot is not absolute – library throws TextParseException
        assertThrows(TextParseException.class, () -> new Name("example.com"), "Name without trailing dot should cause TextParseException");
    }

    @Test
    void findRRset_invalidNameIllegalCharacters_throwsTextParseException() {
        assertThrows(TextParseException.class, () -> new Name("exa$mple.com."), "Name containing illegal characters should cause TextParseException");
    }

    @Test
    void findRRset_invalidNameEmptyString_throwsTextParseException() {
        assertThrows(TextParseException.class, () -> new Name(""), "Empty string is not a valid DNS name");
    }

    @Test
    void findRRset_negativeType_throwsInvalidTypeException() {
        // Negative type is rejected by Type.check and results in InvalidTypeException
        assertThrows(InvalidTypeException.class, () -> message.findRRset(testName, -1), "Negative DNS type should cause InvalidTypeException");
    }

    @Test
    void findRRset_typeNotAllowedInSection_returnsFalse() {
        // SIG is allowed by the library; therefore findRRset simply returns false when no matching RRset exists
        boolean result = message.findRRset(testName, Type.SIG);
        assertFalse(result, "SIG type should return false when no matching RRset is present");
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/regen-policies/results/policy/pools/ktester/34/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended

╷
├─ JUnit Jupiter ✔
│  └─ Message_findRRset_2_Test ✔
│     ├─ findRRset_presentInAuthority_returnsTrue() ✔
│     ├─ findRRset_nameCaseInsensitivity() ✔
│     ├─ findRRset_ANYTypeMatchesAnyRecord() ✔
│     ├─ findRRset_presentInAdditional_returnsTrue() ✔
│     ├─ findRRset_invalidNameEmptyString_throwsTextParseException() ✔
│     ├─ findRRset_wrongName_returnsFalse() ✔
│     ├─ findRRset_nullName_throwsNullPointerException() ✘ Passing a null Name should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
│     ├─ findRRset_invalidNameMissingTrailingDot_throwsIllegalArgumentException() ✘ Name without trailing dot should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ findRRset_wrongType_returnsFalse() ✔
│     ├─ findRRset_typeNotAllowedInSection_throwsIllegalArgumentException() ✘ SIG type should trigger IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ findRRset_typeGreaterThan65535_throwsIllegalArgumentException() ✔
│     ├─ findRRset_negativeType_returnsFalse() ✘ Invalid DNS type: -1
│     ├─ findRRset_nullName_returnsFalse() ✔
│     ├─ findRRset_basicScenarios(String, int, boolean, String) ✔
│     │  ├─ Record in ANSWER – first call returns true ✔
│     │  ├─ Record only in AUTHORITY – second call returns true ✔
│     │  ├─ Record only in ADDITIONAL – third call returns true ✔
│     │  └─ No matching record in any section ✔
│     ├─ findRRset_subdomainMismatch_returnsFalse() ✔
│     ├─ findRRset_notPresent_returnsFalse() ✔
│     ├─ findRRset_shortCircuitStopsAfterAnswer() ✔
│     ├─ findRRset_invalidNameMissingTrailingDot_throwsTextParseException() ✘ Name without trailing dot should cause TextParseException ==> Expected org.xbill.DNS.TextParseException to be thrown, but nothing was thrown.
│     ├─ findRRset_invalidNameEmptyString_throwsIllegalArgumentException() ✘ Empty string is not a valid DNS name ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <org.xbill.DNS.TextParseException>
│     ├─ findRRset_negativeType_throwsInvalidTypeException() ✔
│     ├─ findRRset_presentInAnswer_returnsTrue() ✔
│     ├─ findRRset_invalidNameIllegalCharacters_throwsIllegalArgumentException() ✘ Name containing illegal characters should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ findRRset_emptyMessage_returnsFalse() ✔
│     ├─ findRRset_typeNotAllowedInSection_returnsFalse() ✔
│     ├─ findRRset_multipleSectionsOnlyOneMatches_returnsTrue() ✔
│     └─ findRRset_invalidNameIllegalCharacters_throwsTextParseException() ✘ Name containing illegal characters should cause TextParseException ==> Expected org.xbill.DNS.TextParseException to be thrown, but nothing was thrown.
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (8):
  JUnit Jupiter:Message_findRRset_2_Test:findRRset_nullName_throwsNullPointerException()
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'findRRset_nullName_throwsNullPointerException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Passing a null Name should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_findRRset_2_Test.findRRset_nullName_throwsNullPointerException(Message_findRRset_2_Test.java:109)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_findRRset_2_Test:findRRset_invalidNameMissingTrailingDot_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'findRRset_invalidNameMissingTrailingDot_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Name without trailing dot should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_findRRset_2_Test.findRRset_invalidNameMissingTrailingDot_throwsIllegalArgumentException(Message_findRRset_2_Test.java:115)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_findRRset_2_Test:findRRset_typeNotAllowedInSection_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'findRRset_typeNotAllowedInSection_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: SIG type should trigger IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_findRRset_2_Test.findRRset_typeNotAllowedInSection_throwsIllegalArgumentException(Message_findRRset_2_Test.java:144)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_findRRset_2_Test:findRRset_negativeType_returnsFalse()
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'findRRset_negativeType_returnsFalse', methodParameterTypes = '']
    => org.xbill.DNS.InvalidTypeException: Invalid DNS type: -1
       org.xbill.DNS.Type.check(Type.java:468)
       org.xbill.DNS.Message.findRRset(Unknown Source)
       org.xbill.DNS.Message.findRRset(Unknown Source)
       org.xbill.DNS.Message_findRRset_2_Test.findRRset_negativeType_returnsFalse(Message_findRRset_2_Test.java:132)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:Message_findRRset_2_Test:findRRset_invalidNameMissingTrailingDot_throwsTextParseException()
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'findRRset_invalidNameMissingTrailingDot_throwsTextParseException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Name without trailing dot should cause TextParseException ==> Expected org.xbill.DNS.TextParseException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_findRRset_2_Test.findRRset_invalidNameMissingTrailingDot_throwsTextParseException(Message_findRRset_2_Test.java:202)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_findRRset_2_Test:findRRset_invalidNameEmptyString_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'findRRset_invalidNameEmptyString_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Empty string is not a valid DNS name ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <org.xbill.DNS.TextParseException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_findRRset_2_Test.findRRset_invalidNameEmptyString_throwsIllegalArgumentException(Message_findRRset_2_Test.java:126)
       [...]
     Caused by: org.xbill.DNS.TextParseException: empty name
       org.xbill.DNS.Name.<init>(Name.java:198)
       org.xbill.DNS.Name.<init>(Name.java:295)
       org.xbill.DNS.Message_findRRset_2_Test.lambda$findRRset_invalidNameEmptyString_throwsIllegalArgumentException$3(Message_findRRset_2_Test.java:126)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
  JUnit Jupiter:Message_findRRset_2_Test:findRRset_invalidNameIllegalCharacters_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'findRRset_invalidNameIllegalCharacters_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Name containing illegal characters should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_findRRset_2_Test.findRRset_invalidNameIllegalCharacters_throwsIllegalArgumentException(Message_findRRset_2_Test.java:121)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_findRRset_2_Test:findRRset_invalidNameIllegalCharacters_throwsTextParseException()
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'findRRset_invalidNameIllegalCharacters_throwsTextParseException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Name containing illegal characters should cause TextParseException ==> Expected org.xbill.DNS.TextParseException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_findRRset_2_Test.findRRset_invalidNameIllegalCharacters_throwsTextParseException(Message_findRRset_2_Test.java:207)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 510 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        29 tests found           ]
[         0 tests skipped         ]
[        29 tests started         ]
[         0 tests aborted         ]
[        21 tests successful      ]
[         8 tests failed          ]


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
 

api document of method findRRset(Name, int): 

   * Determines if an RRset with the given name and type is already present in any section.
   *
   * @see RRset
   * @see Section
   

return type: 
boolean

dependent classes: 
class `org.xbill.DNS.Name`:
	
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ADDITIONAL;
		int org.xbill.DNS.Section.ANSWER;
		int org.xbill.DNS.Section.AUTHORITY;
	
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method `private void addAdditional(Message, int)` returns `void`, related with `org.xbill.DNS.Message.findRRset(Name, int)`;
class `org.xbill.DNS.Message`:
	related functions:
		method `public OPTRecord getOPT()` returns `org.xbill.DNS.OPTRecord`, related with `org.xbill.DNS.Message.findRRset(Name, int)`, api document: 
   * Returns the OPT record from the ADDITIONAL section, if one is present.
   *
   * @see OPTRecord
   * @see Section
   ;
		method `public boolean findRecord(Record)` returns `boolean`, related with `org.xbill.DNS.Message.findRRset(Name, int)`, api document: 
   * Determines if the given record is already present in any section.
   *
   * @see Record
   * @see Section
   ;
class `org.xbill.DNS.Cache`:
	related functions:
		method `private int getCred(int, boolean)` returns `int`, related with `org.xbill.DNS.Message.findRRset(Name, int)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
