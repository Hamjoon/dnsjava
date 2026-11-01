


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.InetAddress;
import java.net.UnknownHostException;

@ExtendWith(MockitoExtension.class)
class Message_numBytes_Test {

    /**
     * Verify that a freshly created Message (no‑arg constructor) reports a size of 0.
     */
    @Test
    void numBytes_defaultConstructor() {
        Message m = new Message();
        assertEquals(0, m.numBytes(), "Message created with the default constructor should have size 0");
    }

    /**
     * Verify that a Message created with the ID‑based constructor also reports size 0.
     */
    @Test
    void numBytes_idConstructor() {
        Message m = new Message(0x1234);
        assertEquals(0, m.numBytes(), "Message created with the id‑based constructor should have size 0");
    }

    /**
     * Verify that when a Message is constructed from a wire‑format byte array,
     * {@code numBytes()} returns the exact number of bytes read (i.e. the length of the array).
     */
    @Test
    void numBytes_fromWire() throws IOException {
        // Build a simple query message
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        // Serialize the message to wire format
        // uses default max length (65535)
        byte[] wire = query.toWire();
        // Parse the wire representation back into a Message
        Message parsed = new Message(wire);
        // The size stored inside the parsed message should equal the wire length
        assertEquals(wire.length, parsed.numBytes(), "Parsed Message should report size equal to the wire array length");
    }

    /**
     * Verify that cloning a Message preserves the size reported by {@code numBytes()}.
     */
    @Test
    void numBytes_clonePreservesSize() throws IOException {
        // Create a message with a question and one additional record
        Name qname = Name.fromConstantString("example.org.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message original = Message.newQuery(question);
        original.addRecord(new TXTRecord(qname, DClass.IN, 3600, "test"), Section.ADDITIONAL);
        // Serialize and re‑parse to ensure the internal size field is set
        byte[] wire = original.toWire();
        Message parsed = new Message(wire);
        // Clone the parsed message
        Message clone = parsed.clone();
        assertEquals(parsed.numBytes(), clone.numBytes(), "Cloned Message should have the same size as the original parsed Message");
    }

    /**
     * Set the private {@code size} field via reflection and verify that {@code numBytes()}
     * returns the injected value unchanged.
     */
    @ParameterizedTest(name = "{index} => injectedSize={0}")
    @CsvSource({ "12345", "-1" })
    void numBytes_reflectionInjectedSize(int injectedSize) throws Exception {
        Message m = new Message();
        // Access the private field 'size'
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        sizeField.setInt(m, injectedSize);
        assertEquals(injectedSize, m.numBytes(), "numBytes() should return the raw value set via reflection");
    }

    /**
     * Adding a real record should increase the internal size (i.e. {@code numBytes()} > 0).
     */
    @Test
    void numBytes_afterAddingRecord_increases()throws Exception {
        Message m = new Message();
        assertEquals(0, m.numBytes(), "Initial size must be 0");
        Name name = Name.fromConstantString("test.example.");
        Record a = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("127.0.0.1"));
        m.addRecord(a, Section.ANSWER);
        assertTrue(m.numBytes() > 0, "After adding a record the size reported by numBytes() should be > 0");
    }

    /**
     * Adding a {@code null} record must not modify the size.
     */
    @Test
    void numBytes_afterAddingNullRecord_unchanged() {
        Message m = new Message();
        assertEquals(0, m.numBytes(), "Initial size must be 0");
        // According to the API contract, a null record is ignored.
        m.addRecord(null, Section.ANSWER);
        assertEquals(0, m.numBytes(), "Adding a null record must leave the size unchanged");
    }

    /**
     * Setting a fresh Header does not affect the size.
     */
    @Test
    void numBytes_setHeader_unchangedOnEmptyMessage() {
        Message m = new Message();
        assertEquals(0, m.numBytes(), "Empty message should start with size 0");
        Header h = new Header();
        m.setHeader(h);
        assertEquals(0, m.numBytes(), "Setting a Header on an empty Message must not change its size");
    }

    /**
     * Setting a Header after the message already contains data must keep the size non‑zero.
     */
    @Test
    void numBytes_setHeader_nonZeroAfterAddingRecord() {
        Message m = new Message();
        Name name = Name.fromConstantString("header.test.");
        Record txt = new TXTRecord(name, DClass.IN, 300, "payload");
        m.addRecord(txt, Section.ANSWER);
        int sizeAfterRecord = m.numBytes();
        assertTrue(sizeAfterRecord > 0, "Size must be >0 after adding a record");
        Header newHeader = new Header();
        newHeader.setID(0xABCD);
        m.setHeader(newHeader);
        assertTrue(m.numBytes() > 0, "Replacing the Header must not reset the size; it should stay non‑zero");
    }

    /**
     * Adding a TSIG key should increase the size.  The test uses a real {@link TSIG}
     * instance with a simple HMAC‑MD5 key.
     */
    @Test
    void numBytes_setTSIG_nonZero() throws Exception {
        Message m = new Message();
        // No records yet – size should be 0
        assertEquals(0, m.numBytes());
        // Create a minimal TSIG key (name, algorithm, secret)
        Name keyName = Name.fromConstantString("tsigkey.example.");
        // The algorithm name used by dnsjava for HMAC‑MD5
        Name alg = Name.fromString("hmac-md5.");
        TSIG tsig = new TSIG(keyName, alg, "secret".getBytes());
        // Adding the TSIG record
        m.setTSIG(tsig);
        // The size must now be >0 (the TSIG record contributes to the wire length)
        assertTrue(m.numBytes() > 0, "Calling setTSIG with a valid key must increase the internal size");
    }

    /**
     * Calling {@code setTSIG(null)} must leave the size unchanged.
     */
    @Test
    void numBytes_setTSIG_nullUnchanged() {
        Message m = new Message();
        assertEquals(0, m.numBytes());
        // should be ignored
        m.setTSIG(null);
        assertEquals(0, m.numBytes(), "Calling setTSIG with null must not modify the size");
    }

    /**
     * Removing a previously added record should bring the size back to 0.
     */
    @Test
    void numBytes_removeRecord_backToZero()throws Exception {
        Message m = new Message();
        Name name = Name.fromConstantString("remove.test.");
        Record a = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("192.0.2.1"));
        m.addRecord(a, Section.ANSWER);
        assertTrue(m.numBytes() > 0, "Size must be >0 after adding a record");
        boolean removed = m.removeRecord(a, Section.ANSWER);
        assertTrue(removed, "removeRecord should return true for an existing record");
        assertEquals(0, m.numBytes(), "After removal the size should be back to 0");
    }

    /**
     * Attempting to remove a non‑existent record must not change the size.
     */
    @Test
    void numBytes_removeNonExistentRecord_unchanged()throws Exception {
        Message m = new Message();
        Name name = Name.fromConstantString("absent.test.");
        Record a = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("198.51.100.2"));
        // No record added yet
        assertEquals(0, m.numBytes());
        boolean removed = m.removeRecord(a, Section.ANSWER);
        assertFalse(removed, "removeRecord should return false for a missing record");
        assertEquals(0, m.numBytes(), "Size must stay unchanged when removing a record that was never added");
    }

    /**
     * Cloning a non‑empty Message must preserve the size.
     */
    @Test
    void numBytes_cloneSameSize() throws Exception {
        Message m = new Message();
        Name name = Name.fromConstantString("clone.test.");
        Record txt = new TXTRecord(name, DClass.IN, 300, "clone payload");
        m.addRecord(txt, Section.ANSWER);
        int originalSize = m.numBytes();
        assertTrue(originalSize > 0, "Original message must have non‑zero size");
        Message clone = m.clone();
        assertEquals(originalSize, clone.numBytes(), "Clone must report the same size as the original");
    }

    /**
     * Cloning an empty Message must result in size 0 for both instances.
     */
    @Test
    void numBytes_cloneEmptyMessage() throws Exception {
        Message empty = new Message();
        assertEquals(0, empty.numBytes());
        Message clone = empty.clone();
        assertEquals(0, clone.numBytes(), "Clone of an empty Message must also have size 0");
    }

    /**
     * After serialisation via {@code toWire()}, the reported size must equal the length of the
     * resulting byte array.
     */
    @Test
    void numBytes_afterToWire_equalsByteArrayLength()throws Exception {
        Message m = new Message();
        Name name = Name.fromConstantString("wire.test.");
        Record a = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("203.0.113.5"));
        m.addRecord(a, Section.ANSWER);
        byte[] wire = m.toWire();
        assertEquals(wire.length, m.numBytes(), "numBytes() must match the length of the wire representation");
    }

    /**
     * Serialising with a maximum length (truncation) must result in {@code numBytes()}
     * being less than or equal to that limit.
     */
    @Test
    void numBytes_toWireWithMaxLength_truncated() {
        Message m = new Message();
        Name name = Name.fromConstantString("big.test.");
        // Add many TXT records to exceed 1024 bytes
        for (int i = 0; i < 200; i++) {
            m.addRecord(new TXTRecord(name, DClass.IN, 300, "payload-" + i), Section.ANSWER);
        }
        int maxLen = 1024;
        byte[] wire = m.toWire(maxLen);
        assertTrue(wire.length <= maxLen, "Wire array length must not exceed the specified maxLength");
        assertTrue(m.numBytes() <= maxLen, "Message.numBytes() must reflect the possibly truncated size");
    }

    /**
     * Normalisation must never increase the size of a Message.
     */
    @Test
    void numBytes_normalize_nonIncreasing() throws Exception {
        // Build a message with an extra (irrelevant) record in the ADDITIONAL section
        Message query = Message.newQuery(Record.newRecord(Name.fromConstantString("norm.test."), Type.A, DClass.IN));
        Name extraName = Name.fromConstantString("extra.test.");
        Record extra = new TXTRecord(extraName, DClass.IN, 300, "irrelevant");
        query.addRecord(extra, Section.ADDITIONAL);
        int before = query.numBytes();
        // Normalise against itself – this should strip the irrelevant additional record
        Message normalized = query.normalize(query);
        int after = normalized.numBytes();
        assertTrue(after <= before, "After normalization the size must be less than or equal to the original size");
    }

    /**
     * Assigning a mock {@link Resolver} must not affect the internal size.
     */
    @Test
    void numBytes_setResolver_unchanged(@Mock Resolver mockResolver) {
        Message m = new Message();
        assertEquals(0, m.numBytes());
        m.setResolver(mockResolver);
        assertEquals(0, m.numBytes(), "Setting a Resolver should not modify the message size");
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
│  └─ Message_numBytes_Test ✔
│     ├─ numBytes_afterAddingRecord_increases() ✘ After adding a record the size reported by numBytes() should be > 0 ==> expected: <true> but was: <false>
│     ├─ numBytes_toWireWithMaxLength_truncated() ✔
│     ├─ numBytes_reflectionInjectedSize(int) ✔
│     │  ├─ 1 => injectedSize=12345 ✔
│     │  └─ 2 => injectedSize=-1 ✔
│     ├─ numBytes_afterAddingNullRecord_unchanged() ✔
│     ├─ numBytes_fromWire() ✔
│     ├─ numBytes_idConstructor() ✔
│     ├─ numBytes_setHeader_nonZeroAfterAddingRecord() ✘ Size must be >0 after adding a record ==> expected: <true> but was: <false>
│     ├─ numBytes_setTSIG_nullUnchanged() ✔
│     ├─ numBytes_afterToWire_equalsByteArrayLength() ✔
│     ├─ numBytes_cloneSameSize() ✘ Original message must have non‑zero size ==> expected: <true> but was: <false>
│     ├─ numBytes_clonePreservesSize() ✔
│     ├─ numBytes_defaultConstructor() ✔
│     ├─ numBytes_removeNonExistentRecord_unchanged() ✔
│     ├─ numBytes_setResolver_unchanged(Resolver) ✔
│     ├─ numBytes_setHeader_unchangedOnEmptyMessage() ✔
│     ├─ numBytes_setTSIG_nonZero() ✘ Unknown algorithm: tsigkey.example.
│     ├─ numBytes_normalize_nonIncreasing() ✔
│     ├─ numBytes_removeRecord_backToZero() ✘ Size must be >0 after adding a record ==> expected: <true> but was: <false>
│     └─ numBytes_cloneEmptyMessage() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (5):
  JUnit Jupiter:Message_numBytes_Test:numBytes_afterAddingRecord_increases()
    MethodSource [className = 'org.xbill.DNS.Message_numBytes_Test', methodName = 'numBytes_afterAddingRecord_increases', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: After adding a record the size reported by numBytes() should be > 0 ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_numBytes_Test.numBytes_afterAddingRecord_increases(Message_numBytes_Test.java:102)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_numBytes_Test:numBytes_setHeader_nonZeroAfterAddingRecord()
    MethodSource [className = 'org.xbill.DNS.Message_numBytes_Test', methodName = 'numBytes_setHeader_nonZeroAfterAddingRecord', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Size must be >0 after adding a record ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_numBytes_Test.numBytes_setHeader_nonZeroAfterAddingRecord(Message_numBytes_Test.java:139)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_numBytes_Test:numBytes_cloneSameSize()
    MethodSource [className = 'org.xbill.DNS.Message_numBytes_Test', methodName = 'numBytes_cloneSameSize', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Original message must have non‑zero size ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_numBytes_Test.numBytes_cloneSameSize(Message_numBytes_Test.java:218)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_numBytes_Test:numBytes_setTSIG_nonZero()
    MethodSource [className = 'org.xbill.DNS.Message_numBytes_Test', methodName = 'numBytes_setTSIG_nonZero', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: tsigkey.example.
       org.xbill.DNS.TSIG.nameToAlgorithm(TSIG.java:125)
       org.xbill.DNS.TSIG.<init>(TSIG.java:195)
       org.xbill.DNS.Message_numBytes_Test.numBytes_setTSIG_nonZero(Message_numBytes_Test.java:159)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Message_numBytes_Test:numBytes_removeRecord_backToZero()
    MethodSource [className = 'org.xbill.DNS.Message_numBytes_Test', methodName = 'numBytes_removeRecord_backToZero', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Size must be >0 after adding a record ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_numBytes_Test.numBytes_removeRecord_backToZero(Message_numBytes_Test.java:187)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 529 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        20 tests found           ]
[         0 tests skipped         ]
[        20 tests started         ]
[         0 tests aborted         ]
[        15 tests successful      ]
[         5 tests failed          ]


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
 

api document of method numBytes(): 

   * Returns the size of the message. Only valid if the message has been converted to or from wire
   * format.
   

return type: 
int

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		int org.xbill.DNS.Message.size;
	related functions:
		method `public byte[] toWire()` returns `byte[]`, related with `org.xbill.DNS.Message.numBytes()`, api document: 
   * Returns an array containing the wire format representation of the {@link Message}, but does not
   * do any additional processing (e.g. OPT/TSIG records, truncation).
   *
   * <p>Do NOT use this to actually transmit a message, use {@link #toWire(int)} instead.
   ;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.numBytes()`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
