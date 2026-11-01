


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
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class Message_toWire_Test {

    /**
     * A reusable {@link DNSOutput} instance used by the tests.
     */
    private DNSOutput dnsOutput;

    @BeforeEach
    void setupBeforeEach() {
        // default size is sufficient for our tiny messages
        dnsOutput = new DNSOutput();
    }

    /**
     * Helper method to obtain the underlying byte array from a {@link DNSOutput}.
     * The {@code DNSOutput} class does not expose the array directly, but it provides
     * a {@code toByteArray()} method in the upstream library. If such a method does
     * not exist, we fall back to reflection (which is safe in test code).
     */
    private static byte[] toByteArray(DNSOutput out) {
        try {
            // The library defines a public method; use it if present.
            return (byte[]) out.getClass().getMethod("toByteArray").invoke(out);
        } catch (Exception e) {
            // Fallback: use reflection to read the private field.
            try {
                java.lang.reflect.Field f = out.getClass().getDeclaredField("array");
                f.setAccessible(true);
                byte[] arr = (byte[]) f.get(out);
                int pos = (int) out.getClass().getDeclaredField("pos").get(out);
                byte[] result = new byte[pos];
                System.arraycopy(arr, 0, result, 0, pos);
                return result;
            } catch (Exception ex) {
                throw new RuntimeException("Unable to extract byte array from DNSOutput", ex);
            }
        }
    }

    /**
     * Sets a private field on the given target object.
     */
    private static void setPrivateField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialises an empty {@link Message} and checks that a round‑trip via the
     * {@link Message#Message(byte[])} constructor yields an equivalent object.
     */
    @Test
    void toWire_emptyMessage_roundTrip() throws IOException {
        // header with random ID, no records
        Message empty = new Message();
        // invoke the package‑private method
        empty.toWire(dnsOutput);
        byte[] wire = dnsOutput.toByteArray();
        // The wire format must be parsable
        Message parsed = new Message(wire);
        // Header IDs should match (the random ID is stored in both objects)
        assertEquals(empty.getHeader().getID(), parsed.getHeader().getID(), "Header ID should survive a round‑trip");
        // All sections must be empty
        for (int i = 0; i < 4; i++) {
            assertTrue(parsed.getSection(i).isEmpty(), "Section " + i + " should be empty after round‑trip");
        }
        // The total size must not exceed the DNS maximum length
        assertTrue(wire.length <= Message.MAXLENGTH, "Serialized message must respect MAXLENGTH");
    }

    /**
     * Builds a message containing a question, an answer and an additional OPT record,
     * serialises it with {@code toWire}, deserialises the result and verifies that the
     * records are present in the correct sections and retain their data.
     */
    @Test
    void toWire_withRecords_roundTrip() throws Exception {
        // --- Build the original message -------------------------------------------------
        Name name = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = new Message();
        msg.addRecord(question, Section.QUESTION);
        // Answer: an A record pointing to 127.0.0.1
        ARecord aRecord = new ARecord(name, DClass.IN, 300, InetAddress.getByName("127.0.0.1"));
        msg.addRecord(aRecord, Section.ANSWER);
        // Additional: an OPT record (EDNS)
        OPTRecord opt = new OPTRecord(4096, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        // --- Serialise ---------------------------------------------------------------
        msg.toWire(dnsOutput);
        byte[] wire = dnsOutput.toByteArray();
        // --- Deserialise -------------------------------------------------------------
        Message parsed = new Message(wire);
        // Verify header ID is preserved
        assertEquals(msg.getHeader().getID(), parsed.getHeader().getID(), "Header ID should be identical after round‑trip");
        // Verify question section
        List<Record> qSec = parsed.getSection(Section.QUESTION);
        assertEquals(1, qSec.size(), "Exactly one question record expected");
        assertEquals(question.getName(), qSec.get(0).getName(), "Question name should match");
        assertEquals(question.getType(), qSec.get(0).getType(), "Question type should match");
        // Verify answer section
        List<Record> aSec = parsed.getSection(Section.ANSWER);
        assertEquals(1, aSec.size(), "Exactly one answer record expected");
        assertTrue(aSec.get(0) instanceof ARecord, "Answer should be an ARecord");
        ARecord parsedA = (ARecord) aSec.get(0);
        assertEquals(aRecord.getAddress(), parsedA.getAddress(), "ARecord address should survive round‑trip");
        // Verify additional section (OPT)
        List<Record> addSec = parsed.getSection(Section.ADDITIONAL);
        assertEquals(1, addSec.size(), "Exactly one additional record expected");
        assertTrue(addSec.get(0) instanceof OPTRecord, "Additional should be an OPTRecord");
        OPTRecord parsedOpt = (OPTRecord) addSec.get(0);
        assertEquals(opt.getPayloadSize(), parsedOpt.getPayloadSize(), "OPT payload size should be preserved");
    }

    @Test
    void toWire_allSectionsNull_callsHeaderOnly() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        // sections array of length 3, all entries null
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[3];
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        verifyNoMoreInteractions(mockHeader);
        // No Record should have been touched – there are none.
        verifyNoInteractions(mockOut);
    }

    @Test
    void toWire_nonNullSections_recordsSerializedInOrder() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        Record r0a = mock(Record.class);
        Record r0b = mock(Record.class);
        Record r1a = mock(Record.class);
        List<Record> sec0 = new ArrayList<>();
        sec0.add(r0a);
        sec0.add(r0b);
        List<Record> sec1 = new ArrayList<>();
        sec1.add(r1a);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[2];
        sections[0] = sec0;
        sections[1] = sec1;
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert header called once
        verify(mockHeader, times(1)).toWire(mockOut);
        // Capture the Compression instances passed to each Record
        ArgumentCaptor<Compression> compCaptor = ArgumentCaptor.forClass(Compression.class);
        verify(r0a).toWire(eq(mockOut), eq(0), compCaptor.capture());
        verify(r0b).toWire(eq(mockOut), eq(0), compCaptor.capture());
        verify(r1a).toWire(eq(mockOut), eq(1), compCaptor.capture());
        // All captured Compression objects must be the same instance
        List<Compression> captured = compCaptor.getAllValues();
        assertEquals(3, captured.size(), "Three Compression arguments should be captured");
        Compression first = captured.get(0);
        for (Compression c : captured) {
            assertSame(first, c, "All Record.toWire calls must receive the same Compression instance");
        }
    }

    @Test
    void toWire_mixedNullAndNonNullSections_processesOnlyExistingRecords() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        Record r1 = mock(Record.class);
        List<Record> sec1 = new ArrayList<>();
        sec1.add(r1);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[3];
        sections[0] = null;
        sections[1] = sec1;
        sections[2] = null;
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        verify(r1, times(1)).toWire(eq(mockOut), eq(1), any(Compression.class));
        // No other Record interactions
        verifyNoMoreInteractions(r1);
    }

    @Test
    void toWire_nullOutput_throwsNullPointerException() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        setPrivateField(msg, "header", mockHeader);
        // empty sections – irrelevant
        setPrivateField(msg, "sections", new List[0]);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> msg.toWire(null));
    }

    @Test
    void toWire_nullSectionsArray_throwsNullPointerException() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        setPrivateField(msg, "header", mockHeader);
        // explicitly null
        setPrivateField(msg, "sections", null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> msg.toWire(mockOut));
    }

    @Test
    void toWire_emptySectionsArray_onlyHeaderSerialized() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        setPrivateField(msg, "header", mockHeader);
        // length 0
        setPrivateField(msg, "sections", new List[0]);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        // No Compression created → no Record interactions (there are none)
    }

    @Test
    void toWire_nonNullEmptyLists_noRecordToWireCalls() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[2];
        // empty list
        sections[0] = new ArrayList<>();
        // empty list
        sections[1] = new ArrayList<>();
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        // No Record should be invoked because the lists are empty
    }

    @Test
    void toWire_thirdSectionRecord_passesCorrectSectionIndex() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        Record r2 = mock(Record.class);
        List<Record> sec2 = new ArrayList<>();
        sec2.add(r2);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[3];
        sections[0] = null;
        sections[1] = null;
        sections[2] = sec2;
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        verify(r2, times(1)).toWire(eq(mockOut), eq(2), any(Compression.class));
    }

    @Test
    void toWire_recordThrowsException_propagatesUnchanged() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        Record badRec = mock(Record.class);
        doThrow(new IllegalStateException("boom")).when(badRec).toWire(any(DNSOutput.class), anyInt(), any(Compression.class));
        List<Record> sec0 = new ArrayList<>();
        sec0.add(badRec);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[2];
        sections[0] = sec0;
        sections[1] = new ArrayList<>();
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> msg.toWire(mockOut));
        assertEquals("boom", ex.getMessage());
        // Header should still have been written before the exception
        verify(mockHeader, times(1)).toWire(mockOut);
    }

    @Test
    void toWire_nullHeader_throwsNullPointerException() {
        // Arrange
        Message msg = new Message();
        // header left as null
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[1];
        sections[0] = new ArrayList<>();
        setPrivateField(msg, "sections", sections);
        DNSOutput mockOut = mock(DNSOutput.class);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> msg.toWire(mockOut));
    }

    @Test
    void toWire_headerThrowsException_propagates() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        doThrow(new IllegalStateException("header fail")).when(mockHeader).toWire(any(DNSOutput.class));
        setPrivateField(msg, "header", mockHeader);
        // empty sections
        setPrivateField(msg, "sections", new List[0]);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> msg.toWire(mockOut));
        assertEquals("header fail", ex.getMessage());
    }

    @Test
    void toWire_nullRecordInSection_throwsNullPointerException() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        List<Record> sec0 = new ArrayList<>();
        // deliberately insert a null Record
        sec0.add(null);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[1];
        sections[0] = sec0;
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> msg.toWire(mockOut));
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
│  └─ Message_toWire_Test ✔
│     ├─ toWire_recordThrowsException_propagatesUnchanged() ✔
│     ├─ toWire_allSectionsNull_callsHeaderOnly() ✔
│     ├─ toWire_nullSectionsArray_throwsNullPointerException() ✔
│     ├─ toWire_emptyMessage_roundTrip() ✔
│     ├─ toWire_nullOutput_throwsNullPointerException() ✘ Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
│     ├─ toWire_nullHeader_throwsNullPointerException() ✘ Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
│     ├─ toWire_nullRecordInSection_throwsNullPointerException() ✔
│     ├─ toWire_headerThrowsException_propagates() ✔
│     ├─ toWire_mixedNullAndNonNullSections_processesOnlyExistingRecords() ✔
│     ├─ toWire_emptySectionsArray_onlyHeaderSerialized() ✔
│     ├─ toWire_withRecords_roundTrip() ✔
│     ├─ toWire_nonNullEmptyLists_noRecordToWireCalls() ✔
│     ├─ toWire_nonNullSections_recordsSerializedInOrder() ✔
│     └─ toWire_thirdSectionRecord_passesCorrectSectionIndex() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:Message_toWire_Test:toWire_nullOutput_throwsNullPointerException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_Test', methodName = 'toWire_nullOutput_throwsNullPointerException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       org.xbill.DNS.Message_toWire_Test.toWire_nullOutput_throwsNullPointerException(Message_toWire_Test.java:227)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_toWire_Test:toWire_nullHeader_throwsNullPointerException()
    MethodSource [className = 'org.xbill.DNS.Message_toWire_Test', methodName = 'toWire_nullHeader_throwsNullPointerException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       org.xbill.DNS.Message_toWire_Test.toWire_nullHeader_throwsNullPointerException(Message_toWire_Test.java:337)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 550 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        14 tests found           ]
[         0 tests skipped         ]
[        14 tests started         ]
[         0 tests aborted         ]
[        12 tests successful      ]
[         2 tests failed          ]


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
 

dependent classes: 
class `org.xbill.DNS.DNSOutput`:
	
class `org.xbill.DNS.Message`:
	dependent fields:
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
	related functions:
		method `private void toWire(DNSOutput, int)` returns `void`, related with `org.xbill.DNS.Message.toWire(DNSOutput)`;
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.toWire(DNSOutput)`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public void setHeader(Header)` returns `void`, related with `org.xbill.DNS.Message.toWire(DNSOutput)`, api document: 
   * Replaces the Header with a new one.
   *
   * @see Header
   ;
		method `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.toWire(DNSOutput)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
