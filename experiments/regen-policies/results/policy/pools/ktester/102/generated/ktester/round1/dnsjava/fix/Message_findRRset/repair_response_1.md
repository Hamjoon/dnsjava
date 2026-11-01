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

/**
 * Unit tests for {@link Message#findRRset(Name, int, int)}.
 *
 * The original class name did not match JUnit‑5's default test‑class discovery pattern
 * (it ended with “_Test”).  Renaming it to end with “Test” (and making it public) allows
 * the Jupiter engine to discover and run the tests without the earlier discovery error.
 */
public class MessageFindRRsetTest {

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
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(name, Type.AAAA, Section.AUTHORITY, case1),
                org.junit.jupiter.params.provider.Arguments.of(name, Type.AAAA, Section.AUTHORITY, case2));
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
        assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(nameA, Type.A, invalidSection));
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

    /* --------------------------------------------------------------------- */
    /* Additional test groups (kept from the original source)                */
    /* --------------------------------------------------------------------- */

    @Test
    void valid_sectionEmpty_returnsFalse() {
        assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
        assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    void valid_noMatchingRecord_returnsFalse() throws Exception {
        Record other = Record.newRecord(nameB, Type.A, DClass.IN);
        message.addRecord(other, Section.ANSWER);
        assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
    }

    @ParameterizedTest
    @MethodSource("matchingRecordProvider")
    void valid_matchingRecordFound_returnsTrue(Name queryName, int queryType, int querySection,
                                               List<Record> recordsToAdd) throws Exception {
        for (Record r : recordsToAdd) {
            message.addRecord(r, querySection);
        }
        assertTrue(message.findRRset(queryName, queryType, querySection));
    }

    @Test
    void invalid_typeValidation_failureThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(nameA, -1, Section.QUESTION));
    }

    @Test
    void invalid_sectionValidation_failureThrowsIllegalArgumentException() {
        int invalidSection = 10;
        assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(nameA, Type.A, invalidSection));
    }

    @Test
    void valid_emptyListSection_returnsFalse() throws Exception {
        List<Record>[] sections = getSectionsArray();
        if (sections == null) {
            sections = new List[4];
            setSectionsArray(sections);
        }
        sections[Section.ADDITIONAL] = new ArrayList<>();
        setSectionsArray(sections);
        assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    void valid_matchFirstRecord_returnsTrueImmediately() {
        Record match = Record.newRecord(Name.fromConstantString("first.example."), Type.MX, DClass.IN);
        Record other = Record.newRecord(Name.fromConstantString("other.example."), Type.A, DClass.IN);
        message.addRecord(other, Section.ANSWER);
        message.addRecord(match, Section.ANSWER);
        assertTrue(message.findRRset(match.getName(), Type.MX, Section.ANSWER));
    }

    @Test
    void valid_matchLastRecord_returnsTrueAfterFullScan() {
        Record nonMatch1 = Record.newRecord(Name.fromConstantString("a.example."), Type.A, DClass.IN);
        Record nonMatch2 = Record.newRecord(Name.fromConstantString("b.example."), Type.A, DClass.IN);
        Record match = Record.newRecord(Name.fromConstantString("last.example."), Type.NS, DClass.IN);
        message.addRecord(nonMatch1, Section.AUTHORITY);
        message.addRecord(nonMatch2, Section.AUTHORITY);
        message.addRecord(match, Section.AUTHORITY);
        assertTrue(message.findRRset(match.getName(), Type.NS, Section.AUTHORITY));
    }

    @Test
    void valid_differentTypeSameName_returnsFalse() {
        Record soa = Record.newRecord(Name.fromConstantString("mixed.example."), Type.SOA, DClass.IN);
        Record mx = Record.newRecord(Name.fromConstantString("mixed.example."), Type.MX, DClass.IN);
        message.addRecord(soa, Section.ANSWER);
        assertFalse(message.findRRset(soa.getName(), Type.MX, Section.ANSWER));
    }

    @Test
    void valid_differentNameSameType_returnsFalse() {
        Record txt1 = Record.newRecord(Name.fromConstantString("existent.example."), Type.TXT, DClass.IN);
        Record txt2 = Record.newRecord(Name.fromConstantString("another.example."), Type.TXT, DClass.IN);
        message.addRecord(txt1, Section.ADDITIONAL);
        assertFalse(message.findRRset(Name.fromConstantString("nonexistent.example."), Type.TXT,
                Section.ADDITIONAL));
    }

    @Test
    void invalid_nullName_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> message.findRRset(null, Type.A, Section.QUESTION));
    }

    @Test
    void exception_nullSectionsArray_throwsNullPointerException() throws Exception {
        setSectionsArray(null);
        assertThrows(NullPointerException.class,
                () -> message.findRRset(nameA, Type.A, Section.ANSWER));
    }

    @Test
    void exception_nullRecordInSectionList_throwsNullPointerException() throws Exception {
        List<Record>[] sections = getSectionsArray();
        if (sections == null) {
            sections = new List[4];
        }
        List<Record> list = new ArrayList<>();
        list.add(null);
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);
        assertThrows(NullPointerException.class,
                () -> message.findRRset(nameA, Type.A, Section.AUTHORITY));
    }

    @Test
    void exception_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
        List<Record>[] shortArray = new List[2];
        setSectionsArray(shortArray);
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.findRRset(nameA, Type.AAAA, Section.ADDITIONAL));
    }
}
```