


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.net.InetAddress;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Message_setTSIG_Test {

    private Message message;

    private TSIG tsigKey;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // fresh message for each test
        message = new Message();
        // TSIG constructor: (name, algorithm, secret) – all as strings
        tsigKey = new TSIG("example.com.", "hmac-md5", "ABCDABCDABCDABCDABCDAB==");
    }

    private static Stream<Arguments> validKeyProvider() {
        // normal key – base‑64 secret
        TSIG key1 = new TSIG("secret.example.", "hmac-md5", "ABCDABCDABCDABCDABCDAB==");
        // empty secret – an empty string is accepted (represents zero‑length secret)
        TSIG key2 = new TSIG("empty.example.", "hmac-sha256", "");
        return Stream.of(Arguments.of("Normal TSIG key (base64 secret)", key1), Arguments.of("TSIG with empty secret", key2));
    }

    /* -------------------------------------------------------------
     *  Helper to generate a long label (used in name‑too‑long test).
     * ------------------------------------------------------------- */
    private static String generateLongLabel(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append('x');
        return sb.toString();
    }

    /* ---------------------------------------------------------------------- */
    private static Name tsigName(TSIG key) {
        try {
            Field f = TSIG.class.getDeclaredField("name");
            f.setAccessible(true);
            return (Name) f.get(key);
        } catch (Exception e) {
            throw new AssertionError("Unable to access TSIG.name via reflection", e);
        }
    }

    private static Name tsigAlgorithm(TSIG key) {
        try {
            Field f = TSIG.class.getDeclaredField("algorithm");
            f.setAccessible(true);
            return (Name) f.get(key);
        } catch (Exception e) {
            throw new AssertionError("Unable to access TSIG.algorithm via reflection", e);
        }
    }

    @Test
    void setTSIG_addsTSIGRecordToAdditionalSection() {
        // Pre‑condition: no TSIG present
        assertNull(message.getTSIG(), "Message should not have a TSIG before setTSIG");
        // Act
        message.setTSIG(tsigKey);
        // Verify that a TSIGRecord is now attached to the message
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "Message.getTSIG() should return a non‑null record after setTSIG");
        // The TSIGRecord must be present in the ADDITIONAL section
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        assertTrue(additional.contains(tsigRecord), "Additional section should contain the generated TSIGRecord");
        // Verify that the TSIGRecord fields match the key we supplied
        assertEquals(tsigName(tsigKey), tsigRecord.getName(), "TSIGRecord name should match the TSIG key name");
        assertEquals(tsigAlgorithm(tsigKey), tsigRecord.getAlgorithm(), "TSIGRecord algorithm should match the TSIG key algorithm");
        // Header count for the ADDITIONAL section should have been incremented
        assertEquals(1, message.getHeader().getCount(Section.ADDITIONAL), "Header count for ADDITIONAL should be 1 after adding TSIG");
    }

    @Test
    void setTSIG_multipleCallsReplaceExistingTSIG() throws IOException {
        // First call
        TSIG firstKey = new TSIG("first.example.", "hmac-md5", "ABCDABCDABCDABCDABCDAB==");
        message.setTSIG(firstKey);
        TSIGRecord firstRecord = message.getTSIG();
        assertNotNull(firstRecord);
        assertEquals(tsigName(firstKey), firstRecord.getName());
        // Second call with a different key
        TSIG secondKey = new TSIG("second.example.", "hmac-sha256", "YWJjZGVmZ2hpamtsbW5vcA==");
        message.setTSIG(secondKey);
        TSIGRecord secondRecord = message.getTSIG();
        assertNotNull(secondRecord);
        assertNotSame(firstRecord, secondRecord, "A new TSIGRecord must replace the old one");
        assertEquals(tsigName(secondKey), secondRecord.getName());
        // Exactly one TSIGRecord in the ADDITIONAL section
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        long tsigCount = additional.stream().filter(r -> r instanceof TSIGRecord).count();
        assertEquals(1, tsigCount, "Only one TSIGRecord should remain after replacement");
        // Calling setTSIG(null) after a signed message must clear the state
        message.setTSIG(null);
        assertFalse(message.isSigned());
        assertEquals(Message.TSIG_UNSIGNED, message.tsigState);
        assertNull(message.getTSIG());
    }

    @Test
    void setTSIG_nullKeyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> message.setTSIG(null), "Calling setTSIG with a null key should throw NPE");
    }

    /* -------------------------------------------------------------
     *  Null key handling – the method should clear any existing TSIG
     *  and leave the message unsigned without throwing.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_nullKey_clearsState() {
        // Pre‑condition: message is unsigned
        assertFalse(message.isSigned());
        assertEquals(Message.TSIG_UNSIGNED, message.tsigState);
        assertNull(message.getTSIG());
        // Act – pass a null key
        message.setTSIG(null);
        // Verify that the message stays unsigned and internal state is cleared
        assertFalse(message.isSigned(), "Message must remain unsigned after setTSIG(null)");
        assertEquals(Message.TSIG_UNSIGNED, message.tsigState, "tsigState should be TSIG_UNSIGNED (0) after null key");
        assertNull(message.getTSIG(), "No TSIGRecord should be present after null key");
    }

    /* -------------------------------------------------------------
     *  Valid keys – normal signing behaviour.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validKeyProvider")
    void setTSIG_validKey_signsMessage(String description, TSIG key) {
        // Act
        message.setTSIG(key);
        // Assertions common to all valid keys
        assertTrue(message.isSigned(), "Message should be signed");
        assertEquals(Message.TSIG_SIGNED, message.tsigState, "tsigState must be TSIG_SIGNED (3)");
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "A TSIGRecord must be generated");
        // compare against the key via reflection
        assertEquals(tsigName(key), tsigRecord.getName(), "Generated TSIGRecord name must match the key name");
        assertEquals(tsigAlgorithm(key), tsigRecord.getAlgorithm(), "Generated TSIGRecord algorithm must match the key algorithm");
    }

    /* -------------------------------------------------------------
     *  Exception safety – a TSIG that throws IOException should not
     *  leak the checked exception.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_exceptionSafety_handlesIOExceptionGracefully() throws IOException {
        // Mock a TSIG whose apply() throws IOException
        TSIG brokenKey = Mockito.mock(TSIG.class);
        Mockito.doThrow(new IOException("simulated IO failure")).when(brokenKey).apply(Mockito.any(Message.class), Mockito.isNull());
        // Act – the method must swallow the IOException
        message.setTSIG(brokenKey);
        // Verify that the message is marked as failed and not signed
        assertFalse(message.isSigned(), "Message must not be signed after a failing TSIG");
        assertEquals(Message.TSIG_FAILED, message.tsigState, "tsigState should be TSIG_FAILED (4) on IOException");
        assertNull(message.getTSIG(), "No TSIGRecord should be present after failure");
    }

    /* -------------------------------------------------------------
     *  Unicode key name handling.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_unicodeKeyName_isAccepted() {
//         // Use the string‑based constructor – it accepts Unicode in the name.
//         TSIG unicodeKey = new TSIG("ñame.example.", "hmac-sha256", "unicodeSecret");
//         message.setTSIG(unicodeKey);
//         assertTrue(message.isSigned());
//         assertEquals(Message.TSIG_SIGNED, message.tsigState);
//         TSIGRecord rec = message.getTSIG();
//         assertNotNull(rec);
//         assertEquals(Name.fromString("ñame.example."), rec.getName(), "Unicode name must be preserved in the generated TSIGRecord");
//     }

    /* -------------------------------------------------------------
     *  Large secret handling – should stay within MAXLENGTH.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_largeSecret_isHandledWithoutOverflow() {
//         byte[] largeSecret = new byte[5_000];
//         // Use the (Name,Name,byte[]) constructor for a raw secret.
//         TSIG largeKey = new TSIG(Name.fromString("large.example."), Name.fromString("hmac-sha256"), largeSecret);
//         message.setTSIG(largeKey);
//         assertTrue(message.isSigned());
//         assertEquals(Message.TSIG_SIGNED, message.tsigState);
//         assertNotNull(message.getTSIG());
//         assertTrue(message.numBytes() <= Message.MAXLENGTH, "Message size must not exceed MAXLENGTH after adding a large secret");
//     }

    /* -------------------------------------------------------------
     *  Null algorithm – should result in failure state.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_nullAlgorithm_handling_resultsInFailure() {
//         // Passing a null algorithm Name should trigger failure (IllegalArgumentException may be thrown at construction,
//         // but we want to ensure setTSIG handles it gracefully if construction succeeds).
//         TSIG nullAlgKey = new TSIG(Name.fromString("nullalg.example."), (Name) null, "secret");
//         assertDoesNotThrow(() -> message.setTSIG(nullAlgKey), "setTSIG should not propagate a checked exception");
//         assertFalse(message.isSigned());
//         assertEquals(Message.TSIG_FAILED, message.tsigState);
//         assertNull(message.getTSIG());
//     }

    /* -------------------------------------------------------------
     *  Header preservation – ID must remain unchanged.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_preservesHeaderFields() {
        Header originalHeader = message.getHeader();
        int originalID = originalHeader.getID();
        message.setTSIG(tsigKey);
        Header afterHeader = message.getHeader();
        assertEquals(originalID, afterHeader.getID(), "Message ID must be unchanged after setTSIG");
    }

    /* -------------------------------------------------------------
     *  No side‑effects on other sections.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_noSideEffectOnSections() {
//         // Populate non‑TSIG sections with dummy records
//         Record answerRec = new ARecord(Name.fromString("a.example."), DClass.IN, 300, InetAddress.getLoopbackAddress());
//         Record authRec = new NSRecord(Name.fromString("example."), DClass.IN, 300, Name.fromString("ns.example."));
//         Record addRec = new TXTRecord(Name.fromString("txt.example."), DClass.IN, 300, "hello");
//         message.addRecord(answerRec, Section.ANSWER);
//         message.addRecord(authRec, Section.AUTHORITY);
//         // non‑TSIG addition
//         message.addRecord(addRec, Section.ADDITIONAL);
//         // Keep snapshots of the sections
//         List<Record> answerBefore = new ArrayList<>(message.getSection(Section.ANSWER));
//         List<Record> authBefore = new ArrayList<>(message.getSection(Section.AUTHORITY));
//         List<Record> addBefore = new ArrayList<>(message.getSection(Section.ADDITIONAL));
//         // Act
//         message.setTSIG(tsigKey);
//         // Verify sections other than the generated TSIG are unchanged
//         assertEquals(answerBefore, message.getSection(Section.ANSWER), "ANSWER section must stay unchanged");
//         assertEquals(authBefore, message.getSection(Section.AUTHORITY), "AUTHORITY section must stay unchanged");
//         // The ADDITIONAL section should contain the previous records plus exactly one TSIGRecord
//         List<Record> addAfter = message.getSection(Section.ADDITIONAL);
//         assertEquals(addBefore.size() + 1, addAfter.size(), "ADDITIONAL section size should increase by one for the TSIGRecord");
//         assertTrue(addAfter.containsAll(addBefore), "Existing ADDITIONAL records must be retained");
//     }

    /* -------------------------------------------------------------
     *  Invalid key constructions – expect IllegalArgumentException.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TSIG((String) null, "hmac-md5", "secret"), "Constructing TSIG with null name must fail");
    }

    @Test
    void setTSIG_nonAbsoluteName_throwsIllegalArgumentException() {
        // No trailing dot -> not absolute
        assertThrows(IllegalArgumentException.class, () -> new TSIG("relative", "hmac-md5", "secret"), "Non‑absolute TSIG name must be rejected");
    }

    @Test
    void setTSIG_nameTooLong_throwsIllegalArgumentException() {
        // Build a label of 256 characters (exceeds DNS 255‑octet limit)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) sb.append('a');
        String longLabel = sb.toString();
        assertThrows(IllegalArgumentException.class, () -> new TSIG(longLabel + ".example.", "hmac-sha256", "secret"), "TSIG name exceeding 255 octets must be rejected");
    }

    @Test
    void setTSIG_nullSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TSIG("nullsecret.example.", "hmac-md5", (String) null), "TSIG with null secret must be rejected");
    }

    @Test
    void setTSIG_unsupportedAlgorithm_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TSIG("badalg.example.", "unsupported-algo", "secret"), "Unsupported algorithm must cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Overly large secret – should cause TSIG_FAILED without throwing.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_overlyLargeSecret_causesFailureOnWire() {
//         // larger than MAXLENGTH – use raw‑byte constructor
//         byte[] hugeSecret = new byte[70_000];
//         TSIG hugeKey = new TSIG(Name.fromString("bigsecret.example."), Name.fromString("hmac-sha256"), hugeSecret);
//         // setTSIG must not throw a checked exception
//         assertDoesNotThrow(() -> message.setTSIG(hugeKey));
//         // Expected failure state because the resulting message would be too big
//         assertEquals(Message.TSIG_FAILED, message.tsigState, "tsigState should be TSIG_FAILED for an oversized secret");
//         assertFalse(message.isSigned(), "Message must not be marked as signed");
//         assertNull(message.getTSIG(), "No TSIGRecord should be generated on failure");
//     }
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
│  └─ Message_setTSIG_Test ✔
│     ├─ setTSIG_nameTooLong_throwsIllegalArgumentException() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_addsTSIGRecordToAdditionalSection() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_nullKeyThrowsNullPointerException() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_multipleCallsReplaceExistingTSIG() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_preservesHeaderFields() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_nullSecret_throwsIllegalArgumentException() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_nonAbsoluteName_throwsIllegalArgumentException() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_nullKey_clearsState() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_nullName_throwsIllegalArgumentException() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_exceptionSafety_handlesIOExceptionGracefully() ✘ Unknown algorithm: example.com.
│     ├─ setTSIG_validKey_signsMessage(String, TSIG) ✘ Unknown algorithm: secret.example.
│     └─ setTSIG_unsupportedAlgorithm_throwsIllegalArgumentException() ✘ Unknown algorithm: example.com.
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (12):
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_nameTooLong_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_nameTooLong_throwsIllegalArgumentException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_addsTSIGRecordToAdditionalSection()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_addsTSIGRecordToAdditionalSection', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_nullKeyThrowsNullPointerException()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_nullKeyThrowsNullPointerException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_multipleCallsReplaceExistingTSIG()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_multipleCallsReplaceExistingTSIG', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_preservesHeaderFields()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_preservesHeaderFields', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_nullSecret_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_nullSecret_throwsIllegalArgumentException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_nonAbsoluteName_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_nonAbsoluteName_throwsIllegalArgumentException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_nullKey_clearsState()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_nullKey_clearsState', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_nullName_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_nullName_throwsIllegalArgumentException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_exceptionSafety_handlesIOExceptionGracefully()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_exceptionSafety_handlesIOExceptionGracefully', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_validKey_signsMessage(String, TSIG)
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_validKey_signsMessage', methodParameterTypes = 'java.lang.String, org.xbill.DNS.TSIG']
    => java.lang.IllegalArgumentException: Unknown algorithm: secret.example.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       [...]
       Suppressed: org.junit.platform.commons.PreconditionViolationException: Configuration error: You must configure at least one set of arguments for this @ParameterizedTest
         org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:299)
         org.junit.jupiter.params.ParameterizedTestExtension.lambda$provideTestTemplateInvocationContexts$5(ParameterizedTestExtension.java:98)
         java.base/java.util.stream.AbstractPipeline.close(AbstractPipeline.java:323)
         java.base/java.util.stream.ReferencePipeline$7$1.accept(ReferencePipeline.java:273)
         [...]
  JUnit Jupiter:Message_setTSIG_Test:setTSIG_unsupportedAlgorithm_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_setTSIG_Test', methodName = 'setTSIG_unsupportedAlgorithm_throwsIllegalArgumentException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: example.com.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_setTSIG_Test.setupBeforeEach(Message_setTSIG_Test.java:31)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]

Test run finished after 648 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         1 containers failed     ]
[        11 tests found           ]
[         0 tests skipped         ]
[        11 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[        11 tests failed          ]


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
 

api document of method setTSIG(TSIG): 

   * Sets the TSIG key to sign a message.
   *
   * @param key The TSIG key.
   * @since 3.5.1
   

dependent classes: 
class `org.xbill.DNS.TSIG`:
	related functions:
		method `public TSIGRecord generate(Message, byte[], int, TSIGRecord, boolean)` returns `org.xbill.DNS.TSIGRecord`, related with `org.xbill.DNS.Message.setTSIG(TSIG)`, api document: 
   * Generates a TSIG record with a specific error for a message that has been rendered.
   *
   * @param m The message
   * @param b The rendered message
   * @param error The error
   * @param old If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this {@link TSIGRecord} is the to be added to the first of
   *     many messages in a TCP connection and all TSIG variables (rfc2845, 3.4.2.) should be
   *     included in the signature. {@code false} for subsequent messages with reduced TSIG
   *     variables set (rfc2845, 4.4.).
   * @return The TSIG record to be added to the message
   * @since 3.2
   ;
		method `public void apply(Message, TSIGRecord)` returns `void`, related with `org.xbill.DNS.Message.setTSIG(TSIG)`, api document: 
   * Generates a TSIG record for a message and adds it to the message
   *
   * @param m The message
   * @param old If this message is a response, the TSIG from the request
   ;
class `org.xbill.DNS.Rcode`:
	dependent fields:
		int org.xbill.DNS.Rcode.NOERROR;
	
class `org.xbill.DNS.SimpleResolver`:
	related functions:
		method `public CompletionStage<Message> sendAsync(Message, Executor)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.Message>`, related with `org.xbill.DNS.Message.setTSIG(TSIG)`, api document: 
   * Asynchronously sends a message to a single server.
   *
   * @param query The query to send
   * @param executor The service to use for async operations.
   * @return A future that completes when the response has arrived.
   ;
class `org.xbill.DNS.dnssec.ValidatingResolver`:
	related functions:
		method `public CompletionStage<Message> sendAsync(Message, Executor)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.Message>`, related with `org.xbill.DNS.Message.setTSIG(TSIG)`, api document: 
   * Asynchronously sends a message and validates the response with DNSSEC before returning it.
   *
   * @param query The query to send.
   * @param executor The service to use for async operations.
   * @return A future that completes when the query is finished.
   ;
class `org.xbill.DNS.lookup.LookupSession`:
	related functions:
		method `private static LookupResult buildResult(Message, List<Name>, Record)` returns `org.xbill.DNS.lookup.LookupResult`, related with `org.xbill.DNS.Message.setTSIG(TSIG)`, api document: 
   * Returns a LookupResult if this response was a non-exceptional empty result, throws otherwise.
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
