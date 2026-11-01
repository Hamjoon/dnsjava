
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class Message_findRecord_2_Test {

    private Message message;

    private Record aRecord;

    private Record txtRecord;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a fresh Message for each test
        message = new Message();
        // Prepare a couple of records that can be used in the tests
        Name name = Name.fromConstantString("www.example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        message = null;
        aRecord = null;
        txtRecord = null;
    }

    private static Stream<Arguments> provideFoundInVariousSections() {
        return Stream.of(// target in ANSWER
        Arguments.of(Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN), Section.ANSWER, "The target record is added to the ANSWER section. The loop checks Section.ANSWER first, finds a non‑null list containing the record, and returns true immediately."), // target in AUTHORITY, ANSWER null
        Arguments.of(Record.newRecord(Name.fromConstantString("example.org."), Type.MX, DClass.IN), Section.AUTHORITY, "The ANSWER section is null, the AUTHORITY section contains the target record. The method skips the null ANSWER, evaluates AUTHORITY, finds the record and returns true."), // target in ADDITIONAL, previous sections empty or null
        Arguments.of(Record.newRecord(Name.fromConstantString("example.net."), Type.TXT, DClass.IN), Section.ADDITIONAL, "Both ANSWER and AUTHORITY sections are either null or do not contain the record, but the ADDITIONAL section holds the record. The loop reaches ADDITIONAL, finds the record and returns true."), // target in ADDITIONAL, ANSWER empty list, AUTHORITY non‑null but not containing
        Arguments.of(Record.newRecord(Name.fromConstantString("example.edu."), Type.AAAA, DClass.IN), Section.ADDITIONAL, "The ANSWER section contains an empty list, the AUTHORITY section contains a non‑null list that does not contain the record, and the ADDITIONAL section contains the record. The method correctly continues past empty/non‑matching sections and finally returns true."));
    }

    private static Stream<Arguments> provideNotFoundCases() {
        return Stream.of(// all sections null
        Arguments.of(Record.newRecord(Name.fromConstantString("missing.com."), Type.SRV, DClass.IN), "All three sections (ANSWER, AUTHORITY, ADDITIONAL) are null. The loop never enters the inner condition and returns false after completing all iterations."), // sections non‑null but without the record
        Arguments.of(Record.newRecord(Name.fromConstantString("absent.org."), Type.NS, DClass.IN), "Each section is non‑null but contains lists that do not include the target record. The method checks each list, finds `contains(r)` false each time, and finally returns false."), // empty lists and null sections
        Arguments.of(Record.newRecord(Name.fromConstantString("empty.net."), Type.CNAME, DClass.IN), "ANSWER section is an empty list, AUTHORITY section is null, ADDITIONAL section is an empty list. No `contains` check succeeds, so the method returns false."));
    }

    private static Stream<Arguments> provideMultipleSectionCases() {
        return Stream.of(Arguments.of(Record.newRecord(Name.fromConstantString("multi.com."), Type.A, DClass.IN), "The target record is added to both the ANSWER and AUTHORITY sections. The method should return true after checking the ANSWER section and must not continue to later sections."), Arguments.of(Record.newRecord(Name.fromConstantString("multi.org."), Type.MX, DClass.IN), "The target record appears in ANSWER, AUTHORITY and ADDITIONAL sections simultaneously. The loop should stop at the first match (ANSWER) and return true."));
    }

    private static Stream<Arguments> provideNullRecordCases() {
        return Stream.of(Arguments.of("A null Record is passed. All non‑null sections contain regular records, but `contains(null)` never matches, so the method should safely return false without throwing an exception."), Arguments.of("All three sections are null and the argument is null. The method should simply iterate, skip null sections and finally return false."));
    }

    /**
     * Clears all sections (sets each entry to {@code null}) using reflection.
     */
    private static void clearAllSections(Message msg) throws Exception {
        @SuppressWarnings("unchecked")
        List<Record>[] empty = (List<Record>[]) new List[Section.MAX];
        setSectionsArray(msg, empty);
    }

    /**
     * Sets a specific section list via reflection.
     *
     * @param msg     Message instance
     * @param section Section constant (e.g. {@link Section#ANSWER})
     * @param list    List to store in that section (may be {@code null})
     */
    private static void setSectionList(Message msg, int section, List<Record> list) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        if (sections == null) {
            // initialise with the proper size if it was null
            sections = (List<Record>[]) new List[Section.MAX];
            f.set(msg, sections);
        }
        sections[section] = list;
    }

    /**
     * Directly replaces the whole {@code sections} array.
     *
     * @param msg      Message instance
     * @param newArray New array (may be {@code null})
     */
    private static void setSectionsArray(Message msg, List<Record>[] newArray) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, newArray);
    }

    @Test
    void findRecord_returnsTrueWhenRecordIsInAnswerSection() {
        // GIVEN: a record added to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        // WHEN
        boolean found = message.findRecord(aRecord);
        // THEN
        assertTrue(found, "findRecord should return true for a record present in the ANSWER section");
    }

    @Test
    void findRecord_returnsFalseWhenRecordIsNotPresent() {
        // GIVEN: a message with no records (all sections are null/empty)
        // WHEN
        boolean found = message.findRecord(txtRecord);
        // THEN
        assertFalse(found, "findRecord should return false when the record is not present in any section");
    }

    @Test
    void findRecord_checksAllSections() {
        // GIVEN: the same record added to QUESTION and ADDITIONAL sections
        message.addRecord(aRecord, Section.QUESTION);
        message.addRecord(aRecord, Section.ADDITIONAL);
        // WHEN
        boolean found = message.findRecord(aRecord);
        // THEN
        assertTrue(found, "findRecord should locate the record regardless of which section it resides in");
    }

    @Test
    void findRecord_returnsFalseWhenRecordExistsInDifferentSection() {
        // GIVEN: a different record is present in the ANSWER section
        message.addRecord(txtRecord, Section.ANSWER);
        // WHEN
        boolean found = message.findRecord(aRecord);
        // THEN
        assertFalse(found, "findRecord should return false when the exact Record instance is not present");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideFoundInVariousSections")
    void findRecord_returnsTrueWhenRecordIsFoundInVariousSections(Record target, int sectionToAdd, String description) throws Exception {
        // Ensure all sections are null first
        clearAllSections(message);
        // Add the target record to the requested section
        message.addRecord(target, sectionToAdd);
        // Act
        boolean found = message.findRecord(target);
        // Assert
        assertTrue(found, description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("provideNotFoundCases")
    void findRecord_returnsFalseWhenRecordIsAbsent(Record target, String description) throws Exception {
        // Prepare sections: make them non‑null with unrelated records or empty lists
        clearAllSections(message);
        // ANSWER: unrelated record
        Record unrelated = Record.newRecord(Name.fromConstantString("other.com."), Type.A, DClass.IN);
        message.addRecord(unrelated, Section.ANSWER);
        // AUTHORITY: empty list (add then remove)
        // we simply leave it null – it's acceptable for this case
        // ADDITIONAL: empty list
        setSectionList(message, Section.ADDITIONAL, Collections.emptyList());
        // Act
        boolean found = message.findRecord(target);
        // Assert
        assertFalse(found, description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("provideMultipleSectionCases")
    void findRecord_earlyExitWhenRecordInMultipleSections(Record target, String description) throws Exception {
        // Add to all three sections (ANSWER, AUTHORITY, ADDITIONAL)
        message.addRecord(target, Section.ANSWER);
        message.addRecord(target, Section.AUTHORITY);
        message.addRecord(target, Section.ADDITIONAL);
        // Act
        boolean found = message.findRecord(target);
        // Assert
        assertTrue(found, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNullRecordCases")
    void findRecord_returnsFalseWhenArgumentIsNull(String description) throws Exception {
        // Ensure sections have some data for the first case
        clearAllSections(message);
        Record dummy = Record.newRecord(Name.fromConstantString("dummy.com."), Type.A, DClass.IN);
        message.addRecord(dummy, Section.ANSWER);
        message.addRecord(dummy, Section.AUTHORITY);
        message.addRecord(dummy, Section.ADDITIONAL);
        // Act
        boolean found = message.findRecord((Record) null);
        // Assert
        assertFalse(found, description);
    }

    @Test
    void findRecord_returnsTrueWhenListContainsNullBeforeTarget() throws Exception {
        // Prepare a list with a leading null and then the target record
        Record target = Record.newRecord(Name.fromConstantString("nullentry.com."), Type.TXT, DClass.IN);
        List<Record> list = new ArrayList<>(Arrays.asList(null, target));
        setSectionList(message, Section.ANSWER, list);
        // Act
        boolean found = message.findRecord(target);
        // Assert
        assertTrue(found, "The ANSWER list contains a `null` element followed by the target record. `List.contains` correctly identifies the record despite the preceding null, and the method returns true.");
    }

    @Test
    void findRecord_returnsFalseWhenOnlyNullInList() throws Exception {
        // List contains only a null element
        List<Record> list = new ArrayList<>(Collections.singletonList(null));
        setSectionList(message, Section.ANSWER, list);
        Record target = Record.newRecord(Name.fromConstantString("onlynull.com."), Type.AAAA, DClass.IN);
        // Act
        boolean found = message.findRecord(target);
        // Assert
        assertFalse(found, "The ANSWER list contains only a `null` element; the target record is not present in any section. The method should return false.");
    }

    @Test
    void findRecord_findsRecordInAdditionalAfterNullAndEmptySections() throws Exception {
        // ANSWER = null (default)
        // AUTHORITY = empty list
        setSectionList(message, Section.AUTHORITY, Collections.emptyList());
        // ADDITIONAL = contains target
        Record target = Record.newRecord(Name.fromConstantString("last.com."), Type.SRV, DClass.IN);
        message.addRecord(target, Section.ADDITIONAL);
        // Act
        boolean found = message.findRecord(target);
        // Assert
        assertTrue(found, "ANSWER section is null, AUTHORITY section is an empty list, ADDITIONAL section contains the target record. The method must continue past null and empty sections and finally return true.");
    }

    @Test
    void findRecord_throwsNullPointerExceptionWhenSectionsArrayIsNull() throws Exception {
        // Force the private 'sections' field to null via reflection
        setSectionsArray(message, null);
        Record target = Record.newRecord(Name.fromConstantString("test.com."), Type.A, DClass.IN);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> message.findRecord(target), "When the internal sections array is null, findRecord should throw NullPointerException.");
    }

    @Test
    void findRecord_throwsArrayIndexOutOfBoundsExceptionWhenSectionsArrayTooSmall() throws Exception {
        // Create an array that is smaller than Section.ADDITIONAL + 1 (which is 4)
        @SuppressWarnings("unchecked")
        List<Record>[] tiny = (List<Record>[]) new List[2];
        setSectionsArray(message, tiny);
        Record target = Record.newRecord(Name.fromConstantString("tiny.com."), Type.TXT, DClass.IN);
        // Act & Assert
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.findRecord(target), "When the internal sections array is too small, findRecord should throw ArrayIndexOutOfBoundsException.");
    }

    @Test
    void findRecord_propagatesExceptionFromRecordEquals() throws Exception {
        // Create a mock Record whose equals method throws RuntimeException
        Record problematic = org.mockito.Mockito.mock(Record.class);
        org.mockito.Mockito.when(problematic.equals(org.mockito.Mockito.any())).thenThrow(new RuntimeException("equals exploded"));
        // Put a normal record in a section so that the list is non‑null
        Record dummy = Record.newRecord(Name.fromConstantString("dummy.com."), Type.A, DClass.IN);
        message.addRecord(dummy, Section.ANSWER);
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> message.findRecord(problematic), "When Record.equals throws, findRecord should propagate the RuntimeException.");
        assertEquals("equals exploded", ex.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_findRecord_2_Test.java:66: error: cannot find symbol
        List<Record>[] empty = (List<Record>[]) new List[Section.MAX];
                                                                ^
  symbol:   variable MAX
  location: class Section
src/test/java/org/xbill/DNS/Message_findRecord_2_Test.java:84: error: cannot find symbol
            sections = (List<Record>[]) new List[Section.MAX];
                                                        ^
  symbol:   variable MAX
  location: class Section
Note: src/test/java/org/xbill/DNS/Message_findRecord_2_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method findRecord(Record)': '\n   * Determines if the given record is already present in any section.\n   *\n   * @see Record\n   * @see Section\n   ', 'return type': 'boolean', 'dependent classes': 'class `org.xbill.DNS.Record`:\n\t\nclass `org.xbill.DNS.Section`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Section.ADDITIONAL;\n\t\tint org.xbill.DNS.Section.ANSWER;\n\t\nclass `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\tjava.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;\n\trelated functions:\n\t\tmethod `public boolean findRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record)`, api document: \n   * Determines if the given record is already present in the given section.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.findRecord(Record)`;\n\t\tmethod `public boolean findRRset(Name, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record)`, api document: \n   * Determines if an RRset with the given name and type is already present in any section.\n   *\n   * @see RRset\n   * @see Section\n   ;\n\t\tmethod `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record)`, api document: \n   * Determines if an RRset with the given name and type is already present in the given section.\n   *\n   * @see RRset\n   * @see Section\n   ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


