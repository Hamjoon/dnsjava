```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link Message#removeRecord(Record, int)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>successful removal and header count decrement,</li>
 *   <li>unsuccessful removal when the record is absent or the section list is {@code null},</li>
 *   <li>validation of illegal section indexes,</li>
 *   <li>behaviour with {@code null} records,</li>
 *   <li>edge‑cases such as a {@code null} header or a {@code null} internal sections array,</li>
 *   <li>duplicate records and equality‑based removal.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_removeRecord_Test {

    private Message message;
    private Header header;
    private static final int SECTION = Section.ANSWER;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for each test.
        message = new Message();          // creates a random‑ID header
        header = message.getHeader();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /* ---------------------------------------------------------------------- */
    /*  Existing tests (kept unchanged)                                      */
    /* ---------------------------------------------------------------------- */

    /**
     * Removing a record that exists in the section should succeed,
     * return {@code true}, and decrement the header count.
     */
    @Test
    void removeExistingRecord_returnsTrue_andUpdatesHeader() throws Exception {
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        message.addRecord(aRecord, SECTION);

        assertEquals(1, header.getCount(SECTION));
        assertTrue(message.getSection(SECTION).contains(aRecord));

        boolean removed = message.removeRecord(aRecord, SECTION);

        assertTrue(removed, "removeRecord should return true for an existing record");
        assertEquals(0, header.getCount(SECTION), "Header count must be decremented");
        assertFalse(message.getSection(SECTION).contains(aRecord),
                "Section must no longer contain the removed record");
    }

    /**
     * Removing a record that is not present should return {@code false}
     * and leave the header count unchanged.
     */
    @Test
    void removeNonExistingRecord_returnsFalse_andHeaderUnchanged() throws Exception {
        Name name = Name.fromConstantString("example.org.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);

        assertEquals(0, header.getCount(SECTION));
        assertTrue(message.getSection(SECTION).isEmpty());

        boolean removed = message.removeRecord(aRecord, SECTION);

        assertFalse(removed, "removeRecord should return false when record is absent");
        assertEquals(0, header.getCount(SECTION), "Header count must remain unchanged");
        assertTrue(message.getSection(SECTION).isEmpty(),
                "Section must remain empty after failed removal");
    }

    /**
     * If the internal list for a section is {@code null} (i.e. no records have ever been added
     * to that section), {@code removeRecord} must safely return {@code false}.
     */
    @Test
    void removeWhenSectionListIsNull_returnsFalse() throws Exception {
        assertTrue(message.getSection(SECTION).isEmpty());

        Name name = Name.fromConstantString("nonexistent.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);

        boolean removed = message.removeRecord(aRecord, SECTION);

        assertFalse(removed, "removeRecord should return false when section list is null");
        assertEquals(0, header.getCount(SECTION), "Header count must stay at zero");
    }

    /* ---------------------------------------------------------------------- */
    /*  New tests – covering the JSON‑driven scenarios                        */
    /* ---------------------------------------------------------------------- */

    /**
     * Section.check must reject illegal section indexes.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, -1, Section.ADDITIONAL + 1})
    void illegalSectionIndex_throwsIllegalArgumentException(int illegalSection) throws Exception {
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        assertThrows(IllegalArgumentException.class,
                () -> message.removeRecord(aRecord, illegalSection),
                "removeRecord should validate the section index before any other logic");
    }

    /**
     * Removing a {@code null} record when the target section list has never been instantiated
     * should simply return {@code false} (no NPE).
     */
    @Test
    void nullRecordWithNeverInstantiatedSection_returnsFalse() throws Exception {
        // The ANSWER section list is still null (Message#getSection returns an empty list).
        assertTrue(message.getSection(SECTION).isEmpty());

        boolean removed = message.removeRecord(null, SECTION);
        assertFalse(removed, "removeRecord should return false for null record when section list is null");
        assertEquals(0, header.getCount(SECTION), "Header count must remain unchanged");
    }

    /**
     * When the section list exists but does not contain the supplied record,
     * removal must return {@code false} and leave the header untouched.
     */
    @Test
    void nonMatchingRecordInNonEmptySection_returnsFalse() throws Exception {
        // Populate ANSWER section with a different record.
        Name name1 = Name.fromConstantString("example.com.");
        Record aRecord1 = Record.newRecord(name1, Type.A, DClass.IN);
        message.addRecord(aRecord1, SECTION);
        assertEquals(1, header.getCount(SECTION));

        // Record that is NOT present.
        Name name2 = Name.fromConstantString("other.com.");
        Record aRecord2 = Record.newRecord(name2, Type.A, DClass.IN);

        boolean removed = message.removeRecord(aRecord2, SECTION);
        assertFalse(removed, "removeRecord should return false when record is not in the list");
        assertEquals(1, header.getCount(SECTION), "Header count must stay unchanged");
    }

    /**
     * Removing one of two duplicate records should succeed, decrement the header once,
     * and leave exactly one duplicate behind.
     */
    @Test
    void duplicateRecords_removeOne_successful() throws Exception {
        Name name = Name.fromConstantString("example.com.");
        Record dup1 = Record.newRecord(name, Type.A, DClass.IN);
        Record dup2 = Record.newRecord(name, Type.A, DClass.IN); // equal to dup1

        // Add both – the internal list will contain two equal objects.
        message.addRecord(dup1, SECTION);
        message.addRecord(dup2, SECTION);
        assertEquals(2, header.getCount(SECTION));
        List<Record> list = message.getSection(SECTION);
        assertEquals(2, list.size());

        // Remove one (any) duplicate.
        boolean removed = message.removeRecord(dup1, SECTION);
        assertTrue(removed, "removeRecord should succeed when a duplicate is present");
        assertEquals(1, header.getCount(SECTION), "Header count must be decremented once");
        assertEquals(1, list.size(), "Exactly one duplicate should remain");
        assertTrue(list.contains(dup2), "The remaining record should be the second duplicate");
    }

    /**
     * Removal based on {@code equals()} – a different instance but equal content – must work.
     */
    @Test
    void equalButDifferentInstance_removalSucceeds() throws Exception {
        Name name = Name.fromConstantString("example.com.");
        Record stored = Record.newRecord(name, Type.A, DClass.IN);
        message.addRecord(stored, SECTION);
        assertEquals(1, header.getCount(SECTION));

        // Create a new instance with identical fields.
        Record equalInstance = Record.newRecord(name, Type.A, DClass.IN);
        assertEquals(stored, equalInstance, "Records should be equal by value");

        boolean removed = message.removeRecord(equalInstance, SECTION);
        assertTrue(removed, "removeRecord must succeed for an equal record instance");
        assertEquals(0, header.getCount(SECTION), "Header count must be decremented");
        assertTrue(message.getSection(SECTION).isEmpty(), "Section should be empty after removal");
    }

    /**
     * Attempt to remove a record from a section whose internal list is {@code null}
     * (simulated via reflection) must result in a {@link NullPointerException}.
     */
    @Test
    void sectionsArrayNull_throwsNullPointerException() throws Exception {
        // Force the private 'sections' field to null.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);

        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);

        assertThrows(NullPointerException.class,
                () -> message.removeRecord(aRecord, SECTION),
                "Accessing a null sections array should throw NPE");
    }

    /**
     * If the header is {@code null}, a successful removal will still attempt to
     * decrement the header count, resulting in a {@link NullPointerException}.
     */
    @Test
    void headerNull_throwsNullPointerExceptionAfterSuccessfulRemoval() throws Exception {
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        message.addRecord(aRecord, SECTION);
        assertEquals(1, header.getCount(SECTION));

        // Replace the header with null via the public setter.
        message.setHeader(null);

        assertThrows(NullPointerException.class,
                () -> message.removeRecord(aRecord, SECTION),
                "Removing a record with a null header should cause NPE when decCount is invoked");
    }

    /**
     * Removing a record from the ADDITIONAL section that does not exist must return {@code false}
     * and leave the additional count unchanged.
     */
    @Test
    void removeNonMatchingRecordFromAdditional_returnsFalse() throws Exception {
        // Populate ADDITIONAL with a couple of A records.
        Name nameA = Name.fromConstantString("a.example.com.");
        Record aRec = Record.newRecord(nameA, Type.A, DClass.IN);
        message.addRecord(aRec, Section.ADDITIONAL);
        assertEquals(1, header.getCount(Section.ADDITIONAL));

        // Attempt to remove an MX record that is not present.
        Name mxName = Name.fromConstantString("example.com.");
        Record mxRec = new MXRecord(mxName, DClass.IN, 3600, 10,
                Name.fromConstantString("mail.example.com."));

        boolean removed = message.removeRecord(mxRec, Section.ADDITIONAL);
        assertFalse(removed, "removeRecord should return false for non‑matching MX record");
        assertEquals(1, header.getCount(Section.ADDITIONAL), "Header count must stay unchanged");
    }

    /**
     * Removing a record from an empty but instantiated section (e.g., AUTHORITY) should
     * return {@code false} without touching the header.
     */
    @Test
    void emptyInstantiatedSection_returnsFalse() throws Exception {
        // Force creation of the AUTHORITY list without adding any records.
        message.getSection(Section.AUTHORITY); // triggers lazy allocation inside Message
        assertTrue(message.getSection(Section.AUTHORITY).isEmpty());

        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);

        boolean removed = message.removeRecord(aRecord, Section.AUTHORITY);
        assertFalse(removed, "removeRecord should return false when the list is empty");
        assertEquals(0, header.getCount(Section.AUTHORITY), "Header count must stay at zero");
    }
}
```