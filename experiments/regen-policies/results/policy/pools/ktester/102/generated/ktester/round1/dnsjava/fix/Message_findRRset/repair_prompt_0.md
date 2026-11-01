
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class Message_findRRset_Test {

    private Message message;

    private Name nameA;

    private Name nameB;

    private Record aRecord;

    private Record mxRecord;

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Fresh Message for every test
        message = new Message();
        // Two distinct names for positive / negative cases
        nameA = Name.fromConstantString("example.com.");
        nameB = Name.fromConstantString("other.com.");
        // Records of different types for the same name
        aRecord = Record.newRecord(nameA, Type.A, DClass.IN);
        mxRecord = Record.newRecord(nameA, Type.MX, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Message has no external resources
        message = null;
    }

    /**
     * Data provider for the matching‑record test.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> matchingRecordProvider() throws TextParseException {
        Name name = Name.fromConstantString("example.net.");
        Record matching = Record.newRecord(name, Type.AAAA, DClass.IN);
        Record other1 = Record.newRecord(name, Type.A, DClass.IN);
        Record other2 = Record.newRecord(Name.fromConstantString("other.net."), Type.AAAA, DClass.IN);
        // Case 1 – the matching record is the first element
        List<Record> case1 = List.of(matching, other1, other2);
        // Case 2 – the matching record is not the first element
        List<Record> case2 = List.of(other1, other2, matching);
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(name, Type.AAAA, Section.AUTHORITY, case1), org.junit.jupiter.params.provider.Arguments.of(name, Type.AAAA, Section.AUTHORITY, case2));
    }

    @Test
    void findRRset_sectionEmpty_returnsFalse() {
        // No records have been added; all sections are null/empty
        assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
        assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    void findRRset_recordPresentInAnswer_returnsTrue() {
        // Add an A record to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        // Positive match in the correct section
        assertTrue(message.findRRset(nameA, Type.A, Section.ANSWER));
        // Same record should not be found in other sections
        assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    void findRRset_nameMismatch_returnsFalse() {
        // Add an A record for nameA to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        // Query with a different name – should not match
        assertFalse(message.findRRset(nameB, Type.A, Section.ANSWER));
    }

    @Test
    void findRRset_typeMismatch_returnsFalse() {
        // Add an A record for nameA to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        // Query with the same name but a different type – should not match
        assertFalse(message.findRRset(nameA, Type.MX, Section.ANSWER));
    }

    @Test
    void findRRset_multipleSections_correctSectionDetected() {
        // Add records to two different sections
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(mxRecord, Section.AUTHORITY);
        // Verify each is found only in its own section
        assertTrue(message.findRRset(nameA, Type.A, Section.ANSWER));
        assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        assertTrue(message.findRRset(nameA, Type.MX, Section.AUTHORITY));
        assertFalse(message.findRRset(nameA, Type.MX, Section.ANSWER));
    }

    @Test
    void findRRset_invalidSection_throwsIllegalArgumentException() {
        // Section.check throws IllegalArgumentException for out‑of‑range values
        // valid sections are 0‑3
        int invalidSection = 5;
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(nameA, Type.A, invalidSection));
    }

    /**
     * Sets the private {@code sections} field of {@link Message}.
     */
    @SuppressWarnings("unchecked")
    private void setSectionsArray(List<Record>[] sections) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(message, sections);
    }

    /**
     * Returns the current {@code sections} array (for cloning/modifying).
     */
    @SuppressWarnings("unchecked")
    private List<Record>[] getSectionsArray() throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        return (List<Record>[]) f.get(message);
    }

    /**
     * Group: Valid scenarios - section empty
     */
    @Test
    void valid_sectionEmpty_returnsFalse() {
        // No records have been added; all sections are null
        assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
        assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    /**
     * Group: Valid scenarios - no matching record
     */
    @Test
    void valid_noMatchingRecord_returnsFalse() throws Exception {
        // Add a record with a different name to the ANSWER section
        Record other = Record.newRecord(nameB, Type.A, DClass.IN);
        message.addRecord(other, Section.ANSWER);
        // Query for nameA – should not match
        assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
    }

    /**
     * Group: Valid scenarios - matching record found (multiple cases)
     */
    @ParameterizedTest
    @MethodSource("matchingRecordProvider")
    void valid_matchingRecordFound_returnsTrue(Name queryName, int queryType, int querySection, List<Record> recordsToAdd) throws Exception {
        // Populate the target section with the supplied records
        for (Record r : recordsToAdd) {
            message.addRecord(r, querySection);
        }
        // The method should return true because at least one record matches
        assertTrue(message.findRRset(queryName, queryType, querySection));
    }

    /**
     * Group: Invalid inputs - type validation failure
     */
    @Test
    void invalid_typeValidation_failureThrowsIllegalArgumentException() {
        // Type.check throws IllegalArgumentException for negative values
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(nameA, -1, Section.QUESTION));
    }

    /**
     * Group: Invalid inputs - section validation failure
     */
    @Test
    void invalid_sectionValidation_failureThrowsIllegalArgumentException() {
        // Section.check throws IllegalArgumentException for out‑of‑range values
        // valid sections are 0‑3
        int invalidSection = 10;
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(nameA, Type.A, invalidSection));
    }

    /**
     * Group: Valid scenarios - empty list section
     */
    @Test
    void valid_emptyListSection_returnsFalse() throws Exception {
        // Initialise the sections array (Message constructor may already do this)
        List<Record>[] sections = getSectionsArray();
        if (sections == null) {
            sections = new List[Section.MAX];
            setSectionsArray(sections);
        }
        // Set SECTION.ADDITIONAL to an empty list (non‑null)
        sections[Section.ADDITIONAL] = new ArrayList<>();
        setSectionsArray(sections);
        assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    /**
     * Group: Valid scenarios - match first record
     */
    @Test
    void valid_matchFirstRecord_returnsTrueImmediately() {
        Record match = Record.newRecord(Name.fromConstantString("first.example."), Type.MX, DClass.IN);
        Record other = Record.newRecord(Name.fromConstantString("other.example."), Type.A, DClass.IN);
        // Add a non‑matching record first, then the matching one
        message.addRecord(other, Section.ANSWER);
        message.addRecord(match, Section.ANSWER);
        // The method should return true without scanning beyond the first match
        assertTrue(message.findRRset(match.getName(), Type.MX, Section.ANSWER));
    }

    /**
     * Group: Valid scenarios - match last record
     */
    @Test
    void valid_matchLastRecord_returnsTrueAfterFullScan() {
        Record nonMatch1 = Record.newRecord(Name.fromConstantString("a.example."), Type.A, DClass.IN);
        Record nonMatch2 = Record.newRecord(Name.fromConstantString("b.example."), Type.A, DClass.IN);
        Record match = Record.newRecord(Name.fromConstantString("last.example."), Type.NS, DClass.IN);
        // Add two non‑matching records followed by the matching one
        message.addRecord(nonMatch1, Section.AUTHORITY);
        message.addRecord(nonMatch2, Section.AUTHORITY);
        message.addRecord(match, Section.AUTHORITY);
        assertTrue(message.findRRset(match.getName(), Type.NS, Section.AUTHORITY));
    }

    /**
     * Group: Valid scenarios - different type same name
     */
    @Test
    void valid_differentTypeSameName_returnsFalse() {
        // Same name but different type (SOA vs MX)
        Record soa = Record.newRecord(Name.fromConstantString("mixed.example."), Type.SOA, DClass.IN);
        Record mx = Record.newRecord(Name.fromConstantString("mixed.example."), Type.MX, DClass.IN);
        message.addRecord(soa, Section.ANSWER);
        // Query for MX – should be false because only SOA is present
        assertFalse(message.findRRset(soa.getName(), Type.MX, Section.ANSWER));
    }

    /**
     * Group: Valid scenarios - different name same type
     */
    @Test
    void valid_differentNameSameType_returnsFalse() {
        // Same type (TXT) but different name
        Record txt1 = Record.newRecord(Name.fromConstantString("existent.example."), Type.TXT, DClass.IN);
        Record txt2 = Record.newRecord(Name.fromConstantString("another.example."), Type.TXT, DClass.IN);
        message.addRecord(txt1, Section.ADDITIONAL);
        // Query for a name that does not exist
        assertFalse(message.findRRset(Name.fromConstantString("nonexistent.example."), Type.TXT, Section.ADDITIONAL));
    }

    /**
     * Group: Invalid inputs - null name
     */
    @Test
    void invalid_nullName_throwsNullPointerException() {
        // The method does not guard against null, so a NPE is expected
        assertThrows(NullPointerException.class, () -> message.findRRset(null, Type.A, Section.QUESTION));
    }

    /**
     * Group: Exception scenarios - null sections array
     */
    @Test
    void exception_nullSectionsArray_throwsNullPointerException() throws Exception {
        // Force the internal sections array to null
        setSectionsArray(null);
        assertThrows(NullPointerException.class, () -> message.findRRset(nameA, Type.A, Section.ANSWER));
    }

    /**
     * Group: Exception scenarios - null record in section list
     */
    @Test
    void exception_nullRecordInSectionList_throwsNullPointerException() throws Exception {
        // Prepare a section list that contains a null element
        List<Record>[] sections = getSectionsArray();
        if (sections == null) {
            sections = new List[Section.MAX];
        }
        List<Record> list = new ArrayList<>();
        // the problematic null record
        list.add(null);
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);
        assertThrows(NullPointerException.class, () -> message.findRRset(nameA, Type.A, Section.AUTHORITY));
    }

    /**
     * Group: Exception scenarios - sections array shorter than expected
     */
    @Test
    void exception_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
        // Replace the sections array with a shorter one (length 2)
        List<Record>[] shortArray = new List[2];
        setSectionsArray(shortArray);
        // Section.ADDITIONAL == 3, which is out of bounds for the new array
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.findRRset(nameA, Type.AAAA, Section.ADDITIONAL));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_findRRset_Test.java:197: error: cannot find symbol
            sections = new List[Section.MAX];
                                       ^
  symbol:   variable MAX
  location: class Section
src/test/java/org/xbill/DNS/Message_findRRset_Test.java:288: error: cannot find symbol
            sections = new List[Section.MAX];
                                       ^
  symbol:   variable MAX
  location: class Section
Note: src/test/java/org/xbill/DNS/Message_findRRset_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method findRRset(Name, int, int)': '\n   * Determines if an RRset with the given name and type is already present in the given section.\n   *\n   * @see RRset\n   * @see Section\n   ', 'return type': 'boolean', 'dependent classes': 'class `org.xbill.DNS.Name`:\n\t\nclass `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\tjava.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;\n\trelated functions:\n\t\tmethod `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`;\n\t\tmethod `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Returns an array containing all records in the given section grouped into RRsets.\n   *\n   * @see RRset\n   * @see Section\n   ;\n\t\tmethod `public boolean findRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Determines if the given record is already present in the given section.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Adds a record to a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Removes a record from a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public List<Record> getSection(int)` returns `java.util.List<org.xbill.DNS.Record>`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Returns all records in the given section, or an empty list if the section is empty.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public void removeAllRecords(int)` returns `void`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Removes all records from a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\nclass `org.xbill.DNS.Zone`:\n\trelated functions:\n\t\tmethod `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


