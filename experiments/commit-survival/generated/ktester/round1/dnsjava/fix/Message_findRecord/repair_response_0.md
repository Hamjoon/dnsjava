```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class Message_findRecord_Test {

    private Message message;

    private Record txtRecord;

    private Record aRecord;

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        // Create a fresh Message for each test.
        message = new Message();
        // Prepare some records that will be used in the tests.
        Name txtName = Name.fromConstantString("test.example.");
        txtRecord = new TXTRecord(txtName, DClass.IN, 3600, "sample text");
        Name aName = Name.fromConstantString("host.example.");
        aRecord = new ARecord(aName, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Message does not hold external resources.
        message = null;
        txtRecord = null;
        aRecord = null;
    }

    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(
                // case 0: section 0 never initialized → false
                Arguments.of("recordA", 0, false,
                        "The target section (0) has never been initialized, so sections[0] is null."),
                // case 1: section 1 non‑null but does not contain recordB → false
                Arguments.of("recordB", 1, false,
                        "Section 1 is non‑null (a list was created by adding a different record), but it does not contain recordB."),
                // case 2: section 2 contains recordC → true
                Arguments.of("recordC", 2, true,
                        "Section 2 contains recordC (added beforehand)."));
    }

    /**
     * Creates a {@link Record} instance based on an identifier.
     * For simplicity we map identifiers to either TXT or A records with deterministic content.
     */
    private Record createRecordById(String id) throws UnknownHostException {
        switch (id) {
            case "recordA":
            case "recordB":
            case "recordC":
            case "recordD":
            case "recordE":
            case "recordF":
            case "recordG":
            case "recordI":
            case "recordX":
                // Use a TXT record with the identifier as the text.
                Name txtName = Name.fromConstantString(id.toLowerCase() + ".example.");
                return new TXTRecord(txtName, DClass.IN, 3600, "data-" + id);
            case "recordH1":
            case "recordH2":
                // Two distinct TXTRecord objects with identical content.
                Name hName = Name.fromConstantString("equal.example.");
                return new TXTRecord(hName, DClass.IN, 3600, "equal-data");
            case "otherForSection1":
                // A dummy record for section 1.
                Name dummy1 = Name.fromConstantString("dummy1.example.");
                return new ARecord(dummy1, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
            case "dummyForSection3":
                Name dummy3 = Name.fromConstantString("dummy3.example.");
                return new ARecord(dummy3, DClass.IN, 3600, InetAddress.getByName("192.0.2.3"));
            default:
                // Fallback to a generic TXT record.
                Name defName = Name.fromConstantString("default.example.");
                return new TXTRecord(defName, DClass.IN, 3600, "default");
        }
    }

    @Test
    void findRecord_sectionEmpty_returnsFalse() {
        // No records have been added to the ANSWER section yet.
        assertFalse(message.findRecord(txtRecord, Section.ANSWER),
                "findRecord should return false when the section list is empty");
    }

    @Test
    void findRecord_recordPresent_returnsTrue() {
        // Add the TXT record to the ANSWER section.
        message.addRecord(txtRecord, Section.ANSWER);
        assertTrue(message.findRecord(txtRecord, Section.ANSWER),
                "findRecord should return true when the record is present in the specified section");
    }

    @Test
    void findRecord_recordAbsentInSection_returnsFalse() {
        // Add the TXT record to the ANSWER section.
        message.addRecord(txtRecord, Section.ANSWER);
        // Query for a different record in the same section.
        assertFalse(message.findRecord(aRecord, Section.ANSWER),
                "findRecord should return false when the record is not present in the specified section");
    }

    @Test
    void findRecord_recordPresentInDifferentSection_returnsFalse() {
        // Add the TXT record to the AUTHORITY section.
        message.addRecord(txtRecord, Section.AUTHORITY);
        // Query the same record in the ANSWER section.
        assertFalse(message.findRecord(txtRecord, Section.ANSWER),
                "findRecord should return false when the record is present in a different section");
    }

    @Test
    void findRecord_invalidSection_throwsArrayIndexOutOfBoundsException() {
        // The Message implementation stores sections in an array of length 4 (0‑3).
        // Accessing an invalid index should propagate the ArrayIndexOutOfBoundsException.
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.findRecord(txtRecord, 5),
                "findRecord should throw ArrayIndexOutOfBoundsException for an invalid section index");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicBehaviorProvider")
    void findRecord_basicBehavior(String recordId, int section, boolean expected, String description) throws Exception {
        // Prepare distinct records for each identifier.
        Record record = createRecordById(recordId);
        // Set up the required state according to the test description.
        switch (section) {
            case 0:
                // leave section[0] null – nothing to do
                break;
            case 1:
                // create a list with a *different* record
                Record other = createRecordById("otherForSection1");
                message.addRecord(other, section);
                break;
            case 2:
                // add the target record to the section
                message.addRecord(record, section);
                break;
            default:
        }
        boolean actual = message.findRecord(record, section);
        assertEquals(expected, actual, description);
    }

    @Test
    void findRecord_negativeSection_throwsArrayIndexOutOfBoundsException() throws UnknownHostException {
        Record record = createRecordById("recordD");
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.findRecord(record, -1),
                "Negative section index should cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void findRecord_sectionTooLarge_throwsArrayIndexOutOfBoundsException() throws UnknownHostException {
        Record record = createRecordById("recordE");
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.findRecord(record, 5),
                "Section index larger than internal array size should cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void findRecord_nullRecord_returnsFalse() throws Exception {
        // Ensure section 3 exists (by adding a dummy record) but query with null.
        Record dummy = createRecordById("dummyForSection3");
        message.addRecord(dummy, 3);
        assertFalse(message.findRecord(null, 3),
                "Passing a null record should return false even when the section list exists");
    }

    @Test
    void findRecord_emptySectionList_returnsFalse() throws Exception {
        // Initialise section 1 to an empty list via reflection.
        setSectionList(1, new ArrayList<>());
        Record record = createRecordById("recordF");
        assertFalse(message.findRecord(record, 1),
                "An empty list for the section should cause findRecord to return false");
    }

    @Test
    void findRecord_duplicateRecords_returnsTrue() throws Exception {
        Record record = createRecordById("recordG");
        // Add the same instance twice.
        message.addRecord(record, 2);
        message.addRecord(record, 2);
        assertTrue(message.findRecord(record, 2),
                "When the same record instance appears multiple times, findRecord should still return true");
    }

    @Test
    void findRecord_equalButDistinctInstance_returnsTrue() throws Exception {
        // recordH1 will be added; recordH2 is a different object but equal.
        Record recordH1 = createRecordById("recordH1");
        // same content as H1
        Record recordH2 = createRecordById("recordH2");
        message.addRecord(recordH1, 0);
        assertTrue(message.findRecord(recordH2, 0),
                "findRecord should rely on equals(), therefore a distinct but equal instance must be found");
    }

    @Test
    void findRecord_nullSectionAndNullRecord_returnsFalse() throws Exception {
        // Ensure section 3 is null (default state) and query with null record.
        assertFalse(message.findRecord(null, 3),
                "When both the target section array entry and the record are null, the method should short‑circuit and return false");
    }

    @Test
    void findRecord_validSectionWithNullList_returnsFalse() throws Exception {
        // Section 3 has never been initialized (null entry).
        Record record = createRecordById("recordI");
        assertFalse(message.findRecord(record, 3),
                "A null list for a valid section index should cause findRecord to return false without exception");
    }

    @Test
    void findRecord_nullSectionsArray_throwsNullPointerException() throws Exception {
        // Force the private 'sections' field to null via reflection.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);
        Record record = createRecordById("recordX");
        assertThrows(NullPointerException.class,
                () -> message.findRecord(record, 0),
                "If the internal sections array is null, any access should result in NullPointerException");
    }

    /**
     * Directly sets the internal list for a given section index using reflection.
     *
     * @param index the section index (0‑3)
     * @param list  the list to set (may be empty)
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    private void setSectionList(int index, List<Record> list) throws Exception {
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) sectionsField.get(message);
        // If the array itself is null (unlikely), create it.
        if (sections == null) {
            sections = (List<Record>[]) new List[4];
            sectionsField.set(message, sections);
        }
        sections[index] = list;
    }
}
```