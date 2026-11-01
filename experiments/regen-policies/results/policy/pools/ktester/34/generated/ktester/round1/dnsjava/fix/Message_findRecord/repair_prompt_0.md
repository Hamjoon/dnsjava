
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
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
        if (section >= 0 && section < Section.MAX) {
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

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_findRecord_Test.java:117: error: cannot find symbol
        if (section >= 0 && section < Section.MAX) {
                                             ^
  symbol:   variable MAX
  location: class Section
Note: src/test/java/org/xbill/DNS/Message_findRecord_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
1 error

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method findRecord(Record, int)': '\n   * Determines if the given record is already present in the given section.\n   *\n   * @see Record\n   * @see Section\n   ', 'return type': 'boolean', 'dependent classes': 'class `org.xbill.DNS.Record`:\n\t\nclass `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\tjava.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;\n\trelated functions:\n\t\tmethod `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: \n   * Returns an array containing all records in the given section grouped into RRsets.\n   *\n   * @see RRset\n   * @see Section\n   ;\n\t\tmethod `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: \n   * Determines if an RRset with the given name and type is already present in the given section.\n   *\n   * @see RRset\n   * @see Section\n   ;\n\t\tmethod `public boolean findRecord(Record)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: \n   * Determines if the given record is already present in any section.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: \n   * Adds a record to a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: \n   * Removes a record from a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public List<Record> getSection(int)` returns `java.util.List<org.xbill.DNS.Record>`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: \n   * Returns all records in the given section, or an empty list if the section is empty.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public void removeAllRecords(int)` returns `void`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: \n   * Removes all records from a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


