```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#findRRset(Name, int, int)}.
 *
 * <p>The tests cover the full range of scenarios described in the JSON test
 * specification, including normal operation, boundary conditions and
 * exceptional situations.  Reflection is used only where the test needs to
 * manipulate the internal {@code sections} array in ways that the public API
 * does not allow (e.g. setting it to {@code null} or to a shorter array).</p>
 */
class Message_findRRset_Test {

    private Message message;
    private Name nameA;
    private Name nameB;
    private Record aRecord;
    private Record mxRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

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

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods for reflection based manipulation of the private field */
    /* --------------------------------------------------------------------- */

    /** Sets the private {@code sections} field of {@link Message}. */
    @SuppressWarnings("unchecked")
    private void setSectionsArray(List<Record>[] sections) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(message, sections);
    }

    /** Returns the current {@code sections} array (for cloning/modifying). */
    @SuppressWarnings("unchecked")
    private List<Record>[] getSectionsArray() throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        return (List<Record>[]) f.get(message);
    }

    /* --------------------------------------------------------------------- */
    /* Test groups – one method per group as required by the specification */
    /* --------------------------------------------------------------------- */

    /** Group: Valid scenarios - section empty */
    @Test
    void valid_sectionEmpty_returnsFalse() {
        // No records have been added; all sections are null
        assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
        assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    /** Group: Valid scenarios - no matching record */
    @Test
    void valid_noMatchingRecord_returnsFalse() throws Exception {
        // Add a record with a different name to the ANSWER section
        Record other = Record.newRecord(nameB, Type.A, DClass.IN);
        message.addRecord(other, Section.ANSWER);

        // Query for nameA – should not match
        assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
    }

    /** Group: Valid scenarios - matching record found (multiple cases) */
    @ParameterizedTest
    @MethodSource("matchingRecordProvider")
    void valid_matchingRecordFound_returnsTrue(Name queryName, int queryType, int querySection,
                                               List<Record> recordsToAdd) throws Exception {
        // Populate the target section with the supplied records
        for (Record r : recordsToAdd) {
            message.addRecord(r, querySection);
        }
        // The method should return true because at least one record matches
        assertTrue(message.findRRset(queryName, queryType, querySection));
    }

    /** Data provider for the matching‑record test. */
    private static Stream<org.junit.jupiter.params.provider.Arguments> matchingRecordProvider() throws TextParseException {
        Name name = Name.fromConstantString("example.net.");
        Record matching = Record.newRecord(name, Type.AAAA, DClass.IN);
        Record other1 = Record.newRecord(name, Type.A, DClass.IN);
        Record other2 = Record.newRecord(Name.fromConstantString("other.net."), Type.AAAA, DClass.IN);

        // Case 1 – the matching record is the first element
        List<Record> case1 = List.of(matching, other1, other2);
        // Case 2 – the matching record is not the first element
        List<Record> case2 = List.of(other1, other2, matching);

        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(name, Type.AAAA, Section.AUTHORITY, case1),
                org.junit.jupiter.params.provider.Arguments.of(name, Type.AAAA, Section.AUTHORITY, case2)
        );
    }

    /** Group: Invalid inputs - type validation failure */
    @Test
    void invalid_typeValidation_failureThrowsIllegalArgumentException() {
        // Type.check throws IllegalArgumentException for negative values
        assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(nameA, -1, Section.QUESTION));
    }

    /** Group: Invalid inputs - section validation failure */
    @Test
    void invalid_sectionValidation_failureThrowsIllegalArgumentException() {
        // Section.check throws IllegalArgumentException for out‑of‑range values
        int invalidSection = 10; // valid sections are 0‑3
        assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(nameA, Type.A, invalidSection));
    }

    /** Group: Valid scenarios - empty list section */
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

    /** Group: Valid scenarios - match first record */
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

    /** Group: Valid scenarios - match last record */
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

    /** Group: Valid scenarios - different type same name */
    @Test
    void valid_differentTypeSameName_returnsFalse() {
        // Same name but different type (SOA vs MX)
        Record soa = Record.newRecord(Name.fromConstantString("mixed.example."), Type.SOA, DClass.IN);
        Record mx = Record.newRecord(Name.fromConstantString("mixed.example."), Type.MX, DClass.IN);
        message.addRecord(soa, Section.ANSWER);
        // Query for MX – should be false because only SOA is present
        assertFalse(message.findRRset(soa.getName(), Type.MX, Section.ANSWER));
    }

    /** Group: Valid scenarios - different name same type */
    @Test
    void valid_differentNameSameType_returnsFalse() {
        // Same type (TXT) but different name
        Record txt1 = Record.newRecord(Name.fromConstantString("existent.example."), Type.TXT, DClass.IN);
        Record txt2 = Record.newRecord(Name.fromConstantString("another.example."), Type.TXT, DClass.IN);
        message.addRecord(txt1, Section.ADDITIONAL);
        // Query for a name that does not exist
        assertFalse(message.findRRset(Name.fromConstantString("nonexistent.example."), Type.TXT, Section.ADDITIONAL));
    }

    /** Group: Invalid inputs - null name */
    @Test
    void invalid_nullName_throwsNullPointerException() {
        // The method does not guard against null, so a NPE is expected
        assertThrows(NullPointerException.class,
                () -> message.findRRset(null, Type.A, Section.QUESTION));
    }

    /** Group: Exception scenarios - null sections array */
    @Test
    void exception_nullSectionsArray_throwsNullPointerException() throws Exception {
        // Force the internal sections array to null
        setSectionsArray(null);
        assertThrows(NullPointerException.class,
                () -> message.findRRset(nameA, Type.A, Section.ANSWER));
    }

    /** Group: Exception scenarios - null record in section list */
    @Test
    void exception_nullRecordInSectionList_throwsNullPointerException() throws Exception {
        // Prepare a section list that contains a null element
        List<Record>[] sections = getSectionsArray();
        if (sections == null) {
            sections = new List[Section.MAX];
        }
        List<Record> list = new ArrayList<>();
        list.add(null); // the problematic null record
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);

        assertThrows(NullPointerException.class,
                () -> message.findRRset(nameA, Type.A, Section.AUTHORITY));
    }

    /** Group: Exception scenarios - sections array shorter than expected */
    @Test
    void exception_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
        // Replace the sections array with a shorter one (length 2)
        List<Record>[] shortArray = new List[2];
        setSectionsArray(shortArray);

        // Section.ADDITIONAL == 3, which is out of bounds for the new array
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.findRRset(nameA, Type.AAAA, Section.ADDITIONAL));
    }
}
```