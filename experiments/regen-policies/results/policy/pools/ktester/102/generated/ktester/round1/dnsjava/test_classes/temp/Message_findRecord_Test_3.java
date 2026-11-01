package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

class Message_findRecord_Test {

    /**
     * Message instance used in each test
     */
    private Message message;

    /**
     * A generic A‑record used for the tests
     */
    private Record aRecord;

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Create a fresh Message with an empty header
        message = new Message();
        // Build a simple A‑record: www.example.com.  IN  A  127.0.0.1
        Name name = Name.fromConstantString("www.example.");
//         aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent
        message = null;
        aRecord = null;
    }

    @Test
    void findRecord_EmptySection_ReturnsFalse() {
        // Section.ANSWER is empty at this point
        boolean found = message.findRecord(aRecord, Section.ANSWER);
        assertFalse(found, "findRecord should return false when the section is empty");
    }

    @Test
    void findRecord_RecordPresentInSection_ReturnsTrue() {
        // Add the record to the QUESTION section
        message.addRecord(aRecord, Section.QUESTION);
        // The same record should now be found in the QUESTION section
        assertTrue(message.findRecord(aRecord, Section.QUESTION), "findRecord should return true when the record is present in the given section");
    }

    @Test
    void findRecord_RecordPresentInOtherSection_ReturnsFalse() {
        // Add the record to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        // Query a different section (QUESTION) – should be false
        assertFalse(message.findRecord(aRecord, Section.QUESTION), "findRecord should return false when the record exists but in a different section");
    }

    @Test
    void findRecord_InvalidSection_ThrowsIllegalArgumentException() {
        // Section.check throws IllegalArgumentException for out‑of‑range values
        // valid sections are 0‑3
        int invalidSection = 5;
        assertThrows(IllegalArgumentException.class, () -> message.findRecord(aRecord, invalidSection), "findRecord should throw IllegalArgumentException for an invalid section index");
    }

    @Test
    void findRecord_NullSectionArray_TreatedAsEmpty() {
        // Remove all records from the ADDITIONAL section – this leaves the internal
        // list for that section empty (or null depending on implementation)
        message.removeAllRecords(Section.ADDITIONAL);
        // The method should safely return false even if the internal list is null.
        assertFalse(message.findRecord(aRecord, Section.ADDITIONAL), "findRecord should return false when the internal list for the section is null/empty");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_NullRecord_ListPresent_ReturnsFalse() {
        // Initialise a non‑null list by adding aRecord to QUESTION
        message.addRecord(aRecord, Section.QUESTION);
        // Pass a null record – List.contains(null) is false
        assertFalse(message.findRecord(null, Section.QUESTION), "findRecord should return false when the supplied record is null even if the section list exists");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_NullRecord_NullList_ReturnsFalse() {
        // No record has been added to QUESTION, therefore sections[QUESTION] is null
        assertFalse(message.findRecord(null, Section.QUESTION), "findRecord should safely return false when both the record and the internal list are null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_DuplicateRecords_ReturnsTrue() {
        // Add the same record twice to ANSWER
        message.addRecord(aRecord, Section.ANSWER);
        // duplicate
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue(message.findRecord(aRecord, Section.ANSWER), "findRecord must return true when the record appears more than once in the section");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_IdenticalButDifferentInstance_ReturnsTrue() throws Exception {
        // Add aRecord (created in @BeforeEach) to ADDITIONAL
        message.addRecord(aRecord, Section.ADDITIONAL);
        // Create a new ARecord with the same fields
        Name name = Name.fromConstantString("www.example.");
        Record another = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        assertTrue(message.findRecord(another, Section.ADDITIONAL), "findRecord should return true for a different Record instance that is equal according to Record.equals");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_SameNameTypeDifferentData_ReturnsFalse() throws Exception {
        // Store aRecord (127.0.0.1) in AUTHORITY
        message.addRecord(aRecord, Section.AUTHORITY);
        // Create a record with same name/type but different address
        Name name = Name.fromConstantString("www.example.");
        Record different = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("9.9.9.9"));
        assertFalse(message.findRecord(different, Section.AUTHORITY), "findRecord must return false when the record differs in rdata even if name and type match");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_RecordInDifferentSection_ReturnsFalse() {
        // Add aRecord to ANSWER
        message.addRecord(aRecord, Section.ANSWER);
        // Query AUTHORITY – should be false
        assertFalse(message.findRecord(aRecord, Section.AUTHORITY), "findRecord must only look at the supplied section");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_InvalidSection_Negative_ThrowsIllegalArgumentException() {
        int invalidSection = -1;
        assertThrows(IllegalArgumentException.class, () -> message.findRecord(aRecord, invalidSection), "findRecord should reject negative section indexes");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_InvalidSection_OutOfRange_ThrowsIllegalArgumentException() {
        // valid sections are 0‑3
        int invalidSection = 10;
        assertThrows(IllegalArgumentException.class, () -> message.findRecord(aRecord, invalidSection), "findRecord should reject section indexes larger than the maximum allowed");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_EmptyInitializedList_ReturnsFalse() throws Exception {
        // Force an empty, non‑null list via reflection
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(message);
        // Ensure the array itself exists; if it is null, create a new one.
        if (sections == null) {
            sections = (List<Record>[]) new List[4];
            sectionsField.set(message, sections);
        }
        // Initialise an empty ArrayList for SECTION.ANSWER
        sections[Section.ANSWER] = new ArrayList<>();
        // The list is empty – method must return false
        Record someRecord = new ARecord(Name.fromString("example.com."), DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        assertFalse(message.findRecord(someRecord, Section.ANSWER), "findRecord should return false when the section list exists but contains no records");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_UninitializedSectionsArray_ThrowsNullPointerException() throws Exception {
        // Set the private 'sections' field to null via reflection
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);
        Record someRecord = new ARecord(Name.fromString("example.com."), DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        assertThrows(NullPointerException.class, () -> message.findRecord(someRecord, Section.ANSWER), "When the internal sections array is null, findRecord should propagate a NullPointerException");
    }
}