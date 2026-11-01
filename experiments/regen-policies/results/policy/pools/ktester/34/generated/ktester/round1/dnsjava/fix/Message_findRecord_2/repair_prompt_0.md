


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_findRecord_2_Test {

    private Message message;

    private Record aRecord;

    private Record txtRecord;

    private Name name;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message and some records for each test.
        // uses default Header
        message = new Message();
        name = Name.fromConstantString("example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references – not strictly required but keeps the test tidy.
        message = null;
        aRecord = null;
        txtRecord = null;
        name = null;
    }

    /**
     * Parameter provider for the success group. Each argument consists of:
     * <ul>
     *   <li>a {@link Record} to be added</li>
     *   <li>the {@link Section} constant where it will be stored</li>
     * </ul>
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> successProvider() {
        return Stream.of(// ANSWER section
        org.junit.jupiter.params.provider.Arguments.of(Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN), Section.ANSWER), // AUTHORITY section
        org.junit.jupiter.params.provider.Arguments.of(Record.newRecord(Name.fromConstantString("example.org."), Type.MX, DClass.IN), Section.AUTHORITY), // ADDITIONAL section
        org.junit.jupiter.params.provider.Arguments.of(Record.newRecord(Name.fromConstantString("example.net."), Type.TXT, DClass.IN), Section.ADDITIONAL));
    }

    @Test
    void findRecord_notPresent_returnsFalse() {
        // No records have been added yet.
        assertFalse(message.findRecord(aRecord), "findRecord should return false when the record is absent");
    }

    @Test
    void findRecord_presentInAnswer_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue(message.findRecord(aRecord), "findRecord should return true when the record is in the ANSWER section");
    }

    @Test
    void findRecord_presentInAuthority_returnsTrue() {
        message.addRecord(aRecord, Section.AUTHORITY);
        assertTrue(message.findRecord(aRecord), "findRecord should return true when the record is in the AUTHORITY section");
    }

    @Test
    void findRecord_presentInAdditional_returnsTrue() {
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRecord(aRecord), "findRecord should return true when the record is in the ADDITIONAL section");
    }

    @Test
    void findRecord_presentOnlyInQuestion_returnsFalse() {
        // The QUESTION section is NOT examined by findRecord.
        message.addRecord(aRecord, Section.QUESTION);
        assertFalse(message.findRecord(aRecord), "findRecord must ignore records in the QUESTION section");
    }

    @Test
    void findRecord_multipleSections_oneMatch_returnsTrue() {
        // Add a different record to ANSWER and the target record to ADDITIONAL.
        message.addRecord(txtRecord, Section.ANSWER);
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRecord(aRecord), "findRecord should return true when the record appears in any examined section");
    }

    @Test
    void findRecord_nullRecord_throwsNPE() {
        // The implementation does not guard against null, so a NullPointerException is expected.
        assertThrows(NullPointerException.class, () -> message.findRecord(null));
    }

    @ParameterizedTest(name = "[{index}] Record in {1} section should be found")
    @MethodSource("successProvider")
    void findRecord_success(Record record, int section) {
        // Add the record to the requested section and verify that findRecord returns true.
        message.addRecord(record, section);
        assertTrue(message.findRecord(record), () -> "findRecord should return true when the record is in the " + Section.string(section) + " section");
    }

    @Test
    void findRecord_nullSections_throwsNPE() throws Exception {
        // Use reflection to set the private 'sections' field to null.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);
        // The method will attempt to read sections[i] and therefore throw NPE.
        assertThrows(NullPointerException.class, () -> message.findRecord(aRecord));
    }

    @Test
    void findRecord_shortSections_throwsArrayIndexOutOfBoundsException() throws Exception {
        // Create an array that is *shorter* than the highest section index (Section.ADDITIONAL = 3).
        // Length 3 gives valid indices 0..2; accessing index 3 will cause AIOOBE.
        @SuppressWarnings("unchecked")
        List<Record>[] // length = 3
        shortArray = (List<Record>[]) new List[Section.ADDITIONAL];
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, shortArray);
        // Even though the array is short, we can still add a record to a valid section (e.g., ANSWER)
        // to avoid a NullPointerException before the loop reaches the out‑of‑range index.
        message.addRecord(aRecord, Section.ANSWER);
        // Expected: when the loop reaches Section.ADDITIONAL (index 3) an AIOOBE is thrown.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.findRecord(aRecord));
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
│  └─ Message_findRecord_2_Test ✔
│     ├─ findRecord_notPresent_returnsFalse() ✔
│     ├─ findRecord_presentInAuthority_returnsTrue() ✔
│     ├─ findRecord_presentInAnswer_returnsTrue() ✔
│     ├─ findRecord_multipleSections_oneMatch_returnsTrue() ✔
│     ├─ findRecord_shortSections_throwsArrayIndexOutOfBoundsException() ✘ Expected java.lang.ArrayIndexOutOfBoundsException to be thrown, but nothing was thrown.
│     ├─ findRecord_nullRecord_throwsNPE() ✘ Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
│     ├─ findRecord_presentInAdditional_returnsTrue() ✔
│     ├─ findRecord_success(Record, int) ✔
│     │  ├─ [1] Record in 1 section should be found ✔
│     │  ├─ [2] Record in 2 section should be found ✔
│     │  └─ [3] Record in 3 section should be found ✔
│     ├─ findRecord_presentOnlyInQuestion_returnsFalse() ✔
│     └─ findRecord_nullSections_throwsNPE() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:Message_findRecord_2_Test:findRecord_shortSections_throwsArrayIndexOutOfBoundsException()
    MethodSource [className = 'org.xbill.DNS.Message_findRecord_2_Test', methodName = 'findRecord_shortSections_throwsArrayIndexOutOfBoundsException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected java.lang.ArrayIndexOutOfBoundsException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       org.xbill.DNS.Message_findRecord_2_Test.findRecord_shortSections_throwsArrayIndexOutOfBoundsException(Message_findRecord_2_Test.java:134)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_findRecord_2_Test:findRecord_nullRecord_throwsNPE()
    MethodSource [className = 'org.xbill.DNS.Message_findRecord_2_Test', methodName = 'findRecord_nullRecord_throwsNPE', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       org.xbill.DNS.Message_findRecord_2_Test.findRecord_nullRecord_throwsNPE(Message_findRecord_2_Test.java:99)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 422 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        12 tests found           ]
[         0 tests skipped         ]
[        12 tests started         ]
[         0 tests aborted         ]
[        10 tests successful      ]
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
 

api document of method findRecord(Record): 

   * Determines if the given record is already present in any section.
   *
   * @see Record
   * @see Section
   

return type: 
boolean

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ADDITIONAL;
		int org.xbill.DNS.Section.ANSWER;
	
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
	related functions:
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record)`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
		method `public boolean findRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record)`, api document: 
   * Determines if the given record is already present in the given section.
   *
   * @see Record
   * @see Section
   ;
		method `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.findRecord(Record)`;
		method `public boolean findRRset(Name, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record)`, api document: 
   * Determines if an RRset with the given name and type is already present in any section.
   *
   * @see RRset
   * @see Section
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
