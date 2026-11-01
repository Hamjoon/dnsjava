


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
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class Message_findRecord_Test {

    private Message message;

    // Record that will be added to the message
    private Record aRecord;

    // Record that will NOT be added
    private Record differentRecord;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Fresh Message for each test case.
        message = new Message();
        // Build two simple A‑records (type A, class IN) with distinct names.
        // These are used by the original non‑parameterised tests.
        Name nameA = Name.fromConstantString("host1.example.com.");
        Name nameB = Name.fromConstantString("host2.example.com.");
        aRecord = Record.newRecord(nameA, Type.A, DClass.IN);
        differentRecord = Record.newRecord(nameB, Type.A, DClass.IN);
    }

    /**
     * Provides arguments for {@link #testFindRecordParameterized(String, Record, int, Object)}.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>a description (used as the test name)</li>
     *   <li>a mocked {@link Record}</li>
     *   <li>the section index to query</li>
     *   <li>the expected outcome – either {@link Boolean} or an {@link Class}
     *       representing the expected exception type</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideFindRecordCases() {
        return Stream.of(// 1. Valid section – empty list (null) → false
        arguments("Valid section – empty list (null) → false", mockRecord(1), Section.ANSWER, false), // 2. Valid section – list present but does not contain record → false
        arguments("Valid section – list present but does not contain record → false", mockRecord(2), Section.AUTHORITY, false), // 3. Valid section – list contains record → true
        arguments("Valid section – list contains record → true", mockRecord(3), Section.ADDITIONAL, true), // 4. Invalid section (negative) → IllegalArgumentException
        arguments("Invalid section (negative) → IllegalArgumentException", mockRecord(4), -1, IllegalArgumentException.class), // 5. Invalid section (out of range) → IllegalArgumentException
        arguments("Invalid section (out of range) → IllegalArgumentException", mockRecord(5), 5, IllegalArgumentException.class));
    }

    /**
     * Helper that creates a Mockito mock of {@link Record}. The mock is given a
     * meaningful {@code toString()} implementation to aid debugging.
     *
     * @param id identifier used only for readability
     * @return a mock {@link Record}
     */
    private static Record mockRecord(int id) {
        Record mock = Mockito.mock(Record.class, "RecordMock{id=" + id + "}");
        // Provide a stable toString for log messages / test failures.
        Mockito.when(mock.toString()).thenReturn("RecordMock{id=" + id + "}");
        return mock;
    }

    @Test
    void findRecord_EmptySection_ReturnsFalse() {
        // No records have been added yet; all sections are empty (null).
        boolean result = message.findRecord(aRecord, Section.ANSWER);
        assertFalse(result, "findRecord should return false when the section is empty");
    }

    @Test
    void findRecord_RecordPresent_ReturnsTrue() {
        // Add aRecord to the ANSWER section.
        message.addRecord(aRecord, Section.ANSWER);
        // Now the same record should be found.
        boolean result = message.findRecord(aRecord, Section.ANSWER);
        assertTrue(result, "findRecord should return true when the record exists in the section");
    }

    @Test
    void findRecord_RecordNotPresent_ReturnsFalse() {
        // Add only aRecord to the ANSWER section.
        message.addRecord(aRecord, Section.ANSWER);
        // Query for a different record that was never added.
        boolean result = message.findRecord(differentRecord, Section.ANSWER);
        assertFalse(result, "findRecord should return false when the record is not in the section");
    }

    @Test
    void findRecord_InvalidSection_ThrowsIllegalArgumentException() {
        // Section indices are 0‑3; using 4 should trigger Section.check to fail.
        assertThrows(IllegalArgumentException.class, () -> message.findRecord(aRecord, 4), "findRecord should throw IllegalArgumentException for an invalid section index");
    }

    /**
     * Parameterised test that executes all scenarios defined in the JSON test cases.
     *
     * @param description human‑readable description of the scenario
     * @param record      the {@link Record} supplied to {@code findRecord}
     * @param section     the section index supplied to {@code findRecord}
     * @param expected    either {@link Boolean} (expected return value) or a
     *                    {@link Class} denoting the expected exception type
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideFindRecordCases")
    void testFindRecordParameterized(String description, Record record, int section, Object expected) {
        // -----------------------------------------------------------------
        // Prepare the Message state according to the scenario description.
        // -----------------------------------------------------------------
        if (section >= Section.QUESTION && section <= Section.ADDITIONAL) {
            // For the "list present but does not contain" case we need a *different*
            // record in the section. For the "list contains" case we add the same record.
            if (description.contains("list present but does not contain")) {
                // Add a dummy record that is *not* the one we will query for.
                Record dummy = mockRecord(99);
                message.addRecord(dummy, section);
            } else if (description.contains("list contains")) {
                // Add the exact record we will later query.
                message.addRecord(record, section);
            }
            // The "empty list (null)" case requires no preparation – the section
            // remains null, which is the default state of a freshly created Message.
        }
        // -----------------------------------------------------------------
        // Execute and verify the expected outcome.
        // -----------------------------------------------------------------
        if (expected instanceof Class) {
            // Expected an exception.
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> message.findRecord(record, section), () -> description + " should throw " + exClass.getSimpleName());
        } else {
            // Expected a boolean return value.
            boolean result = message.findRecord(record, section);
            assertEquals((Boolean) expected, result, () -> description + " – expected " + expected + " but got " + result);
        }
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
│  └─ Message_findRecord_Test ✔
│     ├─ findRecord_RecordNotPresent_ReturnsFalse() ✔
│     ├─ findRecord_RecordPresent_ReturnsTrue() ✔
│     ├─ findRecord_EmptySection_ReturnsFalse() ✔
│     ├─ findRecord_InvalidSection_ThrowsIllegalArgumentException() ✔
│     └─ testFindRecordParameterized(String, Record, int, Object) ✔
│        ├─ 1: Valid section – empty list (null) → false ✔
│        ├─ 2: Valid section – list present but does not contain record → false ✘ 
│        │        Unnecessary stubbings detected.
│        │        Clean & maintainable test code requires zero unnecessary code.
│        │        Following stubbings are unnecessary (click to navigate to relevant line of code):
│        │          1. -> at org.xbill.DNS.Message_findRecord_Test.mockRecord(Message_findRecord_Test.java:67)
│        │        Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
│        ├─ 3: Valid section – list contains record → true ✔
│        ├─ 4: Invalid section (negative) → IllegalArgumentException ✔
│        └─ 5: Invalid section (out of range) → IllegalArgumentException ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:Message_findRecord_Test:testFindRecordParameterized(String, Record, int, Object):2: Valid section – list present but does not contain record → false
    MethodSource [className = 'org.xbill.DNS.Message_findRecord_Test', methodName = 'testFindRecordParameterized', methodParameterTypes = 'java.lang.String, org.xbill.DNS.Record, int, java.lang.Object']
    => org.mockito.exceptions.misusing.UnnecessaryStubbingException: 
Unnecessary stubbings detected.
Clean & maintainable test code requires zero unnecessary code.
Following stubbings are unnecessary (click to navigate to relevant line of code):
  1. -> at org.xbill.DNS.Message_findRecord_Test.mockRecord(Message_findRecord_Test.java:67)
Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
       org.mockito.junit.jupiter.MockitoExtension.afterEach(MockitoExtension.java:197)
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

Test run finished after 535 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[         9 tests found           ]
[         0 tests skipped         ]
[         9 tests started         ]
[         0 tests aborted         ]
[         8 tests successful      ]
[         1 tests failed          ]


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
 

api document of method findRecord(Record, int): 

   * Determines if the given record is already present in the given section.
   *
   * @see Record
   * @see Section
   

return type: 
boolean

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
	related functions:
		method `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Returns an array containing all records in the given section grouped into RRsets.
   *
   * @see RRset
   * @see Section
   ;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
		method `public boolean findRecord(Record)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Determines if the given record is already present in any section.
   *
   * @see Record
   * @see Section
   ;
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Removes a record from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public List<Record> getSection(int)` returns `java.util.List<org.xbill.DNS.Record>`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Returns all records in the given section, or an empty list if the section is empty.
   *
   * @see Record
   * @see Section
   ;
		method `public void removeAllRecords(int)` returns `void`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Removes all records from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
