


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
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class Message_toWire_3_Test {

    /**
     * Helper to read the private {@code size} field via reflection.
     */
    private static int getMessageSize(Message msg) throws Exception {
        Field f = Message.class.getDeclaredField("size");
        f.setAccessible(true);
        return f.getInt(msg);
    }

    /**
     * Verify that an empty {@link Message} can be serialised and that the resulting wire length is
     * reported correctly via {@link Message#numBytes()}.
     */
    @Test
    void toWire_emptyMessage() {
        // uses a random ID
        Message msg = new Message();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        assertNotNull(wire, "Wire representation must not be null");
        assertTrue(wire.length > 0, "Wire representation of an empty message should contain at least a header");
        assertEquals(wire.length, msg.numBytes(), "Message.numBytes() must match the length of the wire array");
        // Parsing the wire back should give an equivalent message (same header ID and empty sections)
        Message parsed = assertDoesNotThrow(() -> new Message(wire));
        assertEquals(msg.getHeader().getID(), parsed.getHeader().getID(), "Header IDs must match after round‑trip");
        for (int i = 0; i < 4; i++) {
            assertTrue(parsed.getSection(i).isEmpty(), "Section " + i + " must be empty after round‑trip");
        }
    }

    /**
     * Build a realistic DNS message (question, answer and an OPT pseudo‑record), serialise it,
     * and verify that the deserialised message contains the same records.
     */
    @Test
    void toWire_messageWithRecords() throws IOException {
        // --- build the original message -------------------------------------------------
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        // add an answer record
        Record answer = new ARecord(qname, DClass.IN, 300, InetAddress.getByName("93.184.216.34"));
        query.addRecord(answer, Section.ANSWER);
        // add an OPT record (EDNS0) – the constructor creates a default OPT record
        query.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        // --- serialise ---------------------------------------------------------------
        byte[] wire = query.toWire(Message.MAXLENGTH);
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH, "Wire length must not exceed the supplied maxLength");
        assertEquals(wire.length, query.numBytes(), "Message.numBytes() must reflect the actual wire length");
        // --- deserialise ---------------------------------------------------------------
        Message parsed = new Message(wire);
        // --- assertions ---------------------------------------------------------------
        // Header ID must be preserved
        assertEquals(query.getHeader().getID(), parsed.getHeader().getID(), "Header ID must survive round‑trip");
        // Question section
        Record parsedQuestion = parsed.getQuestion();
        assertNotNull(parsedQuestion, "Parsed message must contain a question");
        assertEquals(question.getName(), parsedQuestion.getName(), "Question name must match");
        assertEquals(question.getType(), parsedQuestion.getType(), "Question type must match");
        assertEquals(question.getDClass(), parsedQuestion.getDClass(), "Question class must match");
        // Answer section
        List<Record> answerSection = parsed.getSection(Section.ANSWER);
        assertEquals(1, answerSection.size(), "Exactly one answer record expected");
        Record parsedAnswer = answerSection.get(0);
        assertEquals(answer.getName(), parsedAnswer.getName(), "Answer name must match");
        assertEquals(answer.getType(), parsedAnswer.getType(), "Answer type must match");
        assertEquals(answer.getDClass(), parsedAnswer.getDClass(), "Answer class must match");
        // Additional section – should contain the OPT record
        List<Record> additional = parsed.getSection(Section.ADDITIONAL);
        assertEquals(1, additional.size(), "Exactly one additional record expected");
        assertTrue(additional.get(0) instanceof OPTRecord, "Additional record must be an OPTRecord");
    }

    /**
     * Ensure that {@code toWire} respects the {@code maxLength} argument by throwing an
     * {@link WireParseException} when the generated message would exceed the limit.
     *
     * <p>The private {@code toWire(DNSOutput, int)} method throws {@code WireParseException}
     * when the output would be larger than {@code maxLength}. We provoke this situation by
     * constructing a message that is deliberately larger than a tiny limit (e.g., 10 bytes).
     */
    @Test
    void toWire_exceedsMaxLength_throwsWireParseException() {
        // Build a message with a single question – this already exceeds a 10‑byte limit.
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        // The limit is intentionally tiny to force the exception.
        int tinyLimit = 10;
        assertThrows(WireParseException.class, () -> msg.toWire(tinyLimit), "Serialising with a maxLength smaller than the message size must throw WireParseException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Normal operation – typical DNS message size
     * --------------------------------------------------------------------------
     */
    @ParameterizedTest(name = "toWire with maxLength={0}")
    @CsvSource({ "512", "65535" })
    void toWire_normalOperation(int maxLength) throws Exception {
        // Build a standard query message (question only)
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(maxLength);
        assertNotNull(wire, "Returned wire must not be null");
        assertTrue(wire.length <= maxLength, "Wire length must not exceed supplied maxLength");
        assertEquals(wire.length, getMessageSize(msg), "Message.size must equal the length of the returned array");
    }

    /**
     * -------------------------------------------------------------------------
     *  Edge cases – zero and negative limits
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_zeroMaxLength_returnsEmptyArray() throws Exception {
        // empty message (only header)
        Message msg = new Message();
        byte[] wire = msg.toWire(0);
        assertNotNull(wire);
        assertEquals(0, wire.length, "Zero maxLength should produce an empty array");
        assertEquals(0, getMessageSize(msg), "Message.size must be 0 for empty output");
    }

    @Test
    void toWire_negativeMaxLength_throwsIllegalArgumentException() {
        Message msg = new Message();
        assertThrows(IllegalArgumentException.class, () -> msg.toWire(-1), "Negative maxLength must cause IllegalArgumentException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Boundary – maxLength larger than the class constant
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_maxLengthBeyondConstant_isCapped() throws Exception {
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        // > Message.MAXLENGTH (65535)
        int huge = 70_000;
        byte[] wire = msg.toWire(huge);
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH, "Wire length must be capped at Message.MAXLENGTH");
        assertEquals(wire.length, getMessageSize(msg), "Message.size must match the actual wire length");
    }

    /**
     * -------------------------------------------------------------------------
     *  State verification – size field reflects output length
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_sizeReflectsReturnedLength() throws Exception {
        Name qname = Name.fromConstantString("example.org.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(1024);
        assertEquals(wire.length, getMessageSize(msg), "After toWire, Message.size must equal the length of the returned byte array");
    }

    /**
     * -------------------------------------------------------------------------
     *  Exact fit – maxLength equals required wire size
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_exactFit_success() throws Exception {
        // Minimal query: header (12) + question (16) = 28 bytes (as documented)
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(28);
        assertNotNull(wire);
        assertEquals(28, wire.length, "Wire length must be exactly 28 bytes");
        assertEquals(28, getMessageSize(msg), "Message.size must be 28");
    }

    @Test
    void toWire_exactFit_tooSmall_throwsIOException() {
        // Header (12) + question (16) = 28, so 12 bytes is insufficient for the question.
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        assertThrows(IOException.class, () -> msg.toWire(12), "Providing maxLength equal to header size only must cause an IOException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Message with TSIG – maxLength just sufficient for signed message
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_tsigExactFit_success() throws Exception {
        // Build a simple signed query (question + TSIG).  The exact size varies with the key,
        // but we can let the library compute it and then use that size as the limit.
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        // Create a dummy TSIG key (the secret can be empty – the library still creates a TSIG RR)
        // secret as empty string
        TSIG key = new TSIG("key.", "hmac-md5.", "");
        msg.setTSIG(key);
        // Serialise once to discover the required size
        byte[] firstWire = msg.toWire(Message.MAXLENGTH);
        int required = firstWire.length;
        // Now serialise with exactly that limit – should succeed
        byte[] exactWire = msg.toWire(required);
        assertEquals(required, exactWire.length, "Wire length must equal the exact required size when limit matches");
        assertEquals(required, getMessageSize(msg), "Message.size must reflect the exact wire length");
    }

    @Test
    void toWire_tsigInsufficient_throwsIOException() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        // secret as empty string
        TSIG key = new TSIG("key.", "hmac-md5.", "");
        msg.setTSIG(key);
        byte[] full = msg.toWire(Message.MAXLENGTH);
        int required = full.length;
        // One byte short should trigger an exception
        assertThrows(IOException.class, () -> msg.toWire(required - 1), "One byte short of the required size must cause an IOException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Message with OPT (EDNS0) – insufficient maxLength
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_optInsufficient_throwsIOException() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        msg.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        // 20 bytes is known to be too small for the OPT RR
        assertThrows(IOException.class, () -> msg.toWire(20), "Insufficient maxLength for OPT record must throw IOException");
    }

    @Test
    void toWire_optExactFit_success() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        msg.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        // Determine required size first
        byte[] full = msg.toWire(Message.MAXLENGTH);
        int required = full.length;
        byte[] wire = msg.toWire(required);
        assertEquals(required, wire.length, "Exact fit for OPT‑containing message must succeed");
        assertEquals(required, getMessageSize(msg), "Message.size must match exact length");
    }

    /**
     * -------------------------------------------------------------------------
     *  Multiple invocations – size field reflects latest serialization
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_multipleInvocations_sizeUpdates() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        // First serialization
        byte[] first = msg.toWire(512);
        int firstSize = getMessageSize(msg);
        assertEquals(first.length, firstSize, "First size must match first wire length");
        // Add an answer record and serialize again with a larger limit
        Record answer = new ARecord(qname, DClass.IN, 300, InetAddress.getByName("93.184.216.34"));
        msg.addRecord(answer, Section.ANSWER);
        byte[] second = msg.toWire(1024);
        int secondSize = getMessageSize(msg);
        assertEquals(second.length, secondSize, "Second size must match second wire length");
        assertTrue(second.length > first.length, "Second wire length must be larger after adding a record");
    }

    /**
     * -------------------------------------------------------------------------
     *  Empty Message – only DNS header present
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_emptyMessageExactHeader_success() throws Exception {
        // no question, no records
        Message msg = new Message();
        byte[] wire = msg.toWire(12);
        assertEquals(12, wire.length, "Header‑only message with maxLength=12 must produce 12 bytes");
        assertEquals(12, getMessageSize(msg), "Message.size must be 12");
    }

    @Test
    void toWire_emptyMessageTooSmall_throwsIOException() {
        Message msg = new Message();
        assertThrows(IOException.class, () -> msg.toWire(11), "Header cannot fit into 11 bytes – should throw IOException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Extreme integer limits for maxLength
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_intMin_throwsIllegalArgumentException() {
        Message msg = new Message();
        assertThrows(IllegalArgumentException.class, () -> msg.toWire(Integer.MIN_VALUE), "Integer.MIN_VALUE should be treated as illegal");
    }

    @Test
    void toWire_intMax_isCapped() throws Exception {
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(Integer.MAX_VALUE);
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH, "When maxLength exceeds MAXLENGTH the result must be capped");
        assertEquals(wire.length, getMessageSize(msg), "Message.size must match the capped wire length");
    }

    /**
     * -------------------------------------------------------------------------
     *  Malformed record – name exceeds DNS label length
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_malformedRecord_throwsWireParseException()throws Exception {
        // Build a name with a label > 63 characters
        StringBuilder longLabel = new StringBuilder();
        for (int i = 0; i < 70; i++) {
            longLabel.append('a');
        }
        String nameStr = longLabel + ".example.com.";
        Name badName = Name.fromString(nameStr);
        Record badRecord = Record.newRecord(badName, Type.A, DClass.IN);
        Message msg = Message.newQuery(badRecord);
        assertThrows(WireParseException.class, () -> msg.toWire(512), "Serialising a record with an over‑long label must throw WireParseException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Invalid record type for section
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_invalidRecordTypeForSection_throwsIllegalArgumentException() {
        Name qname = Name.fromConstantString("example.com.");
        // TXT is not allowed in the QUESTION section
        Record txt = new TXTRecord(qname, DClass.IN, 0, "invalid");
        Message msg = new Message();
        // Directly add to QUESTION (section 0) – the API should reject it
        assertThrows(IllegalArgumentException.class, () -> msg.addRecord(txt, Section.QUESTION), "Adding a TXT record to the QUESTION section must throw IllegalArgumentException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Null record in a section (via reflection)
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_nullRecordInSection_throwsNullPointerException() throws Exception {
        Message msg = Message.newQuery(Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN));
        // Access the internal List for the ANSWER section (index 1) and insert null.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        sections[Section.ANSWER].add(null);
        assertThrows(NullPointerException.class, () -> msg.toWire(512), "Presence of a null record in a section must cause NullPointerException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Invalid TSIG state during serialization (via reflection)
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_invalidTsigState_throwsIllegalStateException() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        // Add a legitimate TSIG so that the serializer will attempt to write it
        // secret as empty string
        TSIG key = new TSIG("key.", "hmac-md5.", "");
        msg.setTSIG(key);
        // Corrupt the private tsigState field
        Field tsigStateField = Message.class.getDeclaredField("tsigState");
        tsigStateField.setAccessible(true);
        // illegal value
        tsigStateField.setInt(msg, 99);
        assertThrows(IllegalStateException.class, () -> msg.toWire(512), "Unexpected tsigState must trigger IllegalStateException");
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
│  └─ Message_toWire_3_Test ✔
│     ├─ toWire_emptyMessageTooSmall_throwsIOException() ✘ Header cannot fit into 11 bytes – should throw IOException ==> Expected java.io.IOException to be thrown, but nothing was thrown.
│     ├─ toWire_emptyMessageExactHeader_success() ✔
│     ├─ toWire_zeroMaxLength_returnsEmptyArray() ✔
│     ├─ toWire_sizeReflectsReturnedLength() ✔
│     ├─ toWire_intMax_isCapped() ✔
│     ├─ toWire_maxLengthBeyondConstant_isCapped() ✔
│     ├─ toWire_tsigExactFit_success() ✘ Unknown algorithm: key.
│     ├─ toWire_tsigInsufficient_throwsIOException() ✘ Unknown algorithm: key.
│     ├─ toWire_intMin_throwsIllegalArgumentException() ✘ Integer.MIN_VALUE should be treated as illegal ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ toWire_normalOperation(int) ✔
│     │  ├─ toWire with maxLength=512 ✔
│     │  └─ toWire with maxLength=65535 ✔
│     ├─ toWire_exactFit_success() ✘ Wire length must be exactly 28 bytes ==> expected: <28> but was: <12>
│     ├─ toWire_negativeMaxLength_throwsIllegalArgumentException() ✘ Negative maxLength must cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ toWire_nullRecordInSection_throwsNullPointerException() ✘ Cannot invoke "java.util.List.add(Object)" because "<local3>[1]" is null
│     ├─ toWire_optInsufficient_throwsIOException() ✘ Insufficient maxLength for OPT record must throw IOException ==> Expected java.io.IOException to be thrown, but nothing was thrown.
│     ├─ toWire_invalidTsigState_throwsIllegalStateException() ✘ Unknown algorithm: key.
│     ├─ toWire_invalidRecordTypeForSection_throwsIllegalArgumentException() ✘ Adding a TXT record to the QUESTION section must throw IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ toWire_exceedsMaxLength_throwsWireParseException() ✘ Serialising with a maxLength smaller than the message size must throw WireParseException ==> Expected org.xbill.DNS.WireParseException to be thrown, but nothing was thrown.
│     ├─ toWire_emptyMessage() ✔
│     ├─ toWire_optExactFit_success() ✔
│     ├─ toWire_malformedRecord_throwsWireParseException() ✘ 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.example.com.': label too long
│     ├─ toWire_exactFit_tooSmall_throwsIOException() ✘ Providing maxLength equal to header size only must cause an IOException ==> Expected java.io.IOException to be thrown, but nothing was thrown.
│     ├─ toWire_multipleInvocations_sizeUpdates() ✔
│     └─ toWire_messageWithRecords() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (13):
  JUnit Jupiter:Message_toWire_3_Test:toWire_emptyMessageTooSmall_throwsIOException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_emptyMessageTooSmall_throwsIOException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Header cannot fit into 11 bytes – should throw IOException ==> Expected java.io.IOException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_toWire_3_Test.toWire_emptyMessageTooSmall_throwsIOException(Message_toWire_3_Test.java:314)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_tsigExactFit_success()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_tsigExactFit_success', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: key.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_toWire_3_Test.toWire_tsigExactFit_success(Message_toWire_3_Test.java:220)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_tsigInsufficient_throwsIOException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_tsigInsufficient_throwsIOException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: key.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_toWire_3_Test.toWire_tsigInsufficient_throwsIOException(Message_toWire_3_Test.java:237)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_intMin_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_intMin_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Integer.MIN_VALUE should be treated as illegal ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_toWire_3_Test.toWire_intMin_throwsIllegalArgumentException(Message_toWire_3_Test.java:325)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_exactFit_success()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_exactFit_success', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Wire length must be exactly 28 bytes ==> expected: <28> but was: <12>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:563)
       org.xbill.DNS.Message_toWire_3_Test.toWire_exactFit_success(Message_toWire_3_Test.java:193)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_negativeMaxLength_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_negativeMaxLength_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Negative maxLength must cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_toWire_3_Test.toWire_negativeMaxLength_throwsIllegalArgumentException(Message_toWire_3_Test.java:145)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_nullRecordInSection_throwsNullPointerException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_nullRecordInSection_throwsNullPointerException', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "java.util.List.add(Object)" because "<local3>[1]" is null
       org.xbill.DNS.Message_toWire_3_Test.toWire_nullRecordInSection_throwsNullPointerException(Message_toWire_3_Test.java:386)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_optInsufficient_throwsIOException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_optInsufficient_throwsIOException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Insufficient maxLength for OPT record must throw IOException ==> Expected java.io.IOException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_toWire_3_Test.toWire_optInsufficient_throwsIOException(Message_toWire_3_Test.java:257)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_invalidTsigState_throwsIllegalStateException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_invalidTsigState_throwsIllegalStateException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: key.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.Message_toWire_3_Test.toWire_invalidTsigState_throwsIllegalStateException(Message_toWire_3_Test.java:402)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_invalidRecordTypeForSection_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_invalidRecordTypeForSection_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Adding a TXT record to the QUESTION section must throw IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_toWire_3_Test.toWire_invalidRecordTypeForSection_throwsIllegalArgumentException(Message_toWire_3_Test.java:370)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_exceedsMaxLength_throwsWireParseException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_exceedsMaxLength_throwsWireParseException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Serialising with a maxLength smaller than the message size must throw WireParseException ==> Expected org.xbill.DNS.WireParseException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_toWire_3_Test.toWire_exceedsMaxLength_throwsWireParseException(Message_toWire_3_Test.java:106)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_malformedRecord_throwsWireParseException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_malformedRecord_throwsWireParseException', methodParameterTypes = '']
    => org.xbill.DNS.TextParseException: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.example.com.': label too long
       org.xbill.DNS.Name.<init>(Name.java:259)
       org.xbill.DNS.Name.fromString(Name.java:314)
       org.xbill.DNS.Name.fromString(Name.java:326)
       org.xbill.DNS.Message_toWire_3_Test.toWire_malformedRecord_throwsWireParseException(Message_toWire_3_Test.java:352)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:Message_toWire_3_Test:toWire_exactFit_tooSmall_throwsIOException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_3_Test', methodName = 'toWire_exactFit_tooSmall_throwsIOException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Providing maxLength equal to header size only must cause an IOException ==> Expected java.io.IOException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_toWire_3_Test.toWire_exactFit_tooSmall_throwsIOException(Message_toWire_3_Test.java:203)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 430 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        24 tests found           ]
[         0 tests skipped         ]
[        24 tests started         ]
[         0 tests aborted         ]
[        11 tests successful      ]
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
 

api document of method toWire(int): 

   * Returns an array containing the wire format representation of the Message with the specified
   * maximum length. This will generate a truncated message (with the TC bit) if the message doesn't
   * fit, and will also sign the message with the TSIG key set by a call to {@link #setTSIG(TSIG,
   * int, TSIGRecord)}. This method may return an empty byte array if the message could not be
   * rendered at all; this could happen if maxLength is smaller than a DNS header, for example.
   *
   * <p>Do NOT use this method in conjunction with {@link TSIG#apply(Message, TSIGRecord)}, it
   * produces inconsistent results! Use {@link #setTSIG(TSIG, int, TSIGRecord)} instead.
   *
   * @param maxLength The maximum length of the message.
   * @return The wire format of the message, or an empty array if the message could not be rendered
   *     into the specified length.
   * @see Flags
   * @see TSIG
   

return type: 
byte[]

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		int org.xbill.DNS.Message.size;
	related functions:
		method `public byte[] toWire()` returns `byte[]`, related with `org.xbill.DNS.Message.toWire(int)`, api document: 
   * Returns an array containing the wire format representation of the {@link Message}, but does not
   * do any additional processing (e.g. OPT/TSIG records, truncation).
   *
   * <p>Do NOT use this to actually transmit a message, use {@link #toWire(int)} instead.
   ;
		method `public int numBytes()` returns `int`, related with `org.xbill.DNS.Message.toWire(int)`, api document: 
   * Returns the size of the message. Only valid if the message has been converted to or from wire
   * format.
   ;
class `org.xbill.DNS.SVCBBase.ParameterMandatory`:
	related functions:
		method `public byte[] toWire()` returns `byte[]`, related with `org.xbill.DNS.Message.toWire(int)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
