```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Additional unit tests for {@link Message#findRecord(Record, int)}.
 *
 * <p>The original test class already contains a few basic scenarios.
 * The tests below cover every case described in the JSON test‑case list,
 * including edge cases that require reflection to manipulate the private
 * {@code sections} field.</p>
 */
class Message_findRecord_Test {

    /* --------------------------------------------------------------------- */
    /* Existing fixtures – they are reused from the original test class.    */
    /* --------------------------------------------------------------------- */

    /** Message instance used in each test */
    private Message message;

    /** A generic A‑record used for the tests */
    private Record aRecord;

    @org.junit.jupiter.api.BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Create a fresh Message with an empty header
        message = new Message();

        // Build a simple A‑record: www.example.com.  IN  A  127.0.0.1
        Name name = Name.fromConstantString("www.example.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
    }

    @org.junit.jupiter.api.AfterEach
    void teardownAfterEach() {
        message = null;
        aRecord = null;
    }

    /* --------------------------------------------------------------------- */
    /* 1. Null record argument – list present (should return false)         */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_NullRecord_ListPresent_ReturnsFalse() {
        // Initialise a non‑null list by adding aRecord to QUESTION
        message.addRecord(aRecord, Section.QUESTION);

        // Pass a null record – List.contains(null) is false
        assertFalse(message.findRecord(null, Section.QUESTION),
                "findRecord should return false when the supplied record is null even if the section list exists");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Null record argument – list itself null (should return false)      */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_NullRecord_NullList_ReturnsFalse() {
        // No record has been added to QUESTION, therefore sections[QUESTION] is null
        assertFalse(message.findRecord(null, Section.QUESTION),
                "findRecord should safely return false when both the record and the internal list are null");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Duplicate records in a section – should still return true          */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_DuplicateRecords_ReturnsTrue() {
        // Add the same record twice to ANSWER
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(aRecord, Section.ANSWER); // duplicate

        assertTrue(message.findRecord(aRecord, Section.ANSWER),
                "findRecord must return true when the record appears more than once in the section");
    }

    /* --------------------------------------------------------------------- */
    /* 4. Different instance with identical fields – equality based lookup   */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_IdenticalButDifferentInstance_ReturnsTrue() throws Exception {
        // Add aRecord (created in @BeforeEach) to ADDITIONAL
        message.addRecord(aRecord, Section.ADDITIONAL);

        // Create a new ARecord with the same fields
        Name name = Name.fromConstantString("www.example.");
        Record another = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));

        assertTrue(message.findRecord(another, Section.ADDITIONAL),
                "findRecord should return true for a different Record instance that is equal according to Record.equals");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Same name/type but different rdata – should return false           */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_SameNameTypeDifferentData_ReturnsFalse() throws Exception {
        // Store aRecord (127.0.0.1) in AUTHORITY
        message.addRecord(aRecord, Section.AUTHORITY);

        // Create a record with same name/type but different address
        Name name = Name.fromConstantString("www.example.");
        Record different = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("9.9.9.9"));

        assertFalse(message.findRecord(different, Section.AUTHORITY),
                "findRecord must return false when the record differs in rdata even if name and type match");
    }

    /* --------------------------------------------------------------------- */
    /* 6. Record present in a different section – should return false        */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_RecordInDifferentSection_ReturnsFalse() {
        // Add aRecord to ANSWER
        message.addRecord(aRecord, Section.ANSWER);

        // Query AUTHORITY – should be false
        assertFalse(message.findRecord(aRecord, Section.AUTHORITY),
                "findRecord must only look at the supplied section");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Invalid section (negative) – IllegalArgumentException               */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_InvalidSection_Negative_ThrowsIllegalArgumentException() {
        int invalidSection = -1;
        assertThrows(IllegalArgumentException.class,
                () -> message.findRecord(aRecord, invalidSection),
                "findRecord should reject negative section indexes");
    }

    /* --------------------------------------------------------------------- */
    /* 8. Invalid section (out of range) – IllegalArgumentException          */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_InvalidSection_OutOfRange_ThrowsIllegalArgumentException() {
        int invalidSection = 10; // valid sections are 0‑3
        assertThrows(IllegalArgumentException.class,
                () -> message.findRecord(aRecord, invalidSection),
                "findRecord should reject section indexes larger than the maximum allowed");
    }

    /* --------------------------------------------------------------------- */
    /* 9. Valid section – empty (size 0) list – should return false           */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_EmptyInitializedList_ReturnsFalse() throws Exception {
        // Force an empty, non‑null list via reflection
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(message);
        // Ensure the array itself exists
        if (sections == null) {
            // The constructor creates the array; however, guard against null for safety
            sections = (List<Record>[]) new List[Section.values().length];
            sectionsField.set(message, sections);
        }
        // Initialise an empty ArrayList for SECTION.ANSWER
        sections[Section.ANSWER] = new ArrayList<>();

        // The list is empty – method must return false
        Record someRecord = new ARecord(Name.fromString("example.com."), DClass.IN, 3600,
                InetAddress.getByName("1.2.3.4"));
        assertFalse(message.findRecord(someRecord, Section.ANSWER),
                "findRecord should return false when the section list exists but contains no records");
    }

    /* --------------------------------------------------------------------- */
    /* 10. Uninitialized sections array – NullPointerException                */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_UninitializedSectionsArray_ThrowsNullPointerException() throws Exception {
        // Set the private 'sections' field to null via reflection
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);

        Record someRecord = new ARecord(Name.fromString("example.com."), DClass.IN, 3600,
                InetAddress.getByName("1.2.3.4"));

        assertThrows(NullPointerException.class,
                () -> message.findRecord(someRecord, Section.ANSWER),
                "When the internal sections array is null, findRecord should propagate a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 11. Valid section – internal list is null (never allocated) – false   */
    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_SectionListNull_ReturnsFalse() {
        // No records have been added to SECTION.ADDITIONAL, so sections[ADDITIONAL] stays null
        Record someRecord = new ARecord(Name.fromString("example.com."), DClass.IN, 3600,
                InetAddress.getByName("1.2.3.4"));
        assertFalse(message.findRecord(someRecord, Section.ADDITIONAL),
                "findRecord must return false when the internal list for the section is null");
    }

    /* --------------------------------------------------------------------- */
    /* 12. Valid section – list contains the record – true (already covered)*/
    /* --------------------------------------------------------------------- */
    // The original test `findRecord_RecordPresentInSection_ReturnsTrue` already covers this case.
}
```