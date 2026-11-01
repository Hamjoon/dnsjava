```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#getQuestion()}.
 *
 * <p>The tests cover the behaviours described in the JSON test‑case specification,
 * including edge cases that require reflective manipulation of the private {@code sections}
 * array.</p>
 *
 * <p>All tests use only the public API of {@link Message} except the few that need to
 * simulate illegal internal states; those use reflection as required.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_getQuestion_Test {

    /** A fresh {@link Message} instance for each test. */
    private Message message;

    /** A reusable {@link Name} for creating {@link Record}s. */
    private Name testName;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a clean Message and a constant Name before each test.
        message = new Message();                     // uses the no‑arg constructor
        testName = Name.fromConstantString("example.com.");
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly drop references – helps the GC in long test suites.
        message = null;
        testName = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /** The QUESTION section is {@code null} → {@code getQuestion()} must return {@code null}. */
    @Test
    void getQuestion_sectionIsNull_returnsNull() {
        // The no‑arg constructor leaves all sections as {@code null}.
        assertNull(message.getQuestion(),
                "When the QUESTION section is null, getQuestion() should return null");
    }

    /** The QUESTION section exists but is empty → {@code getQuestion()} must return {@code null}. */
    @Test
    void getQuestion_sectionIsEmpty_returnsNull() {
        // Ensure the QUESTION list exists and is empty.
        message.addRecord(null, Section.QUESTION); // addRecord safely ignores null
        message.removeAllRecords(Section.QUESTION);
        assertTrue(message.getSection(Section.QUESTION).isEmpty(),
                "Pre‑condition: QUESTION section should be empty");
        assertNull(message.getQuestion(),
                "When the QUESTION section is empty, getQuestion() should return null");
    }

    /** When a single question record is present, it must be returned. */
    @Test
    void getQuestion_singleRecord_returnsThatRecord() {
        Record question = Record.newRecord(testName, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);
        assertSame(question, message.getQuestion(),
                "The single question record should be returned by getQuestion()");
    }

    /** When multiple records are present, the first one added must be returned. */
    @Test
    void getQuestion_multipleRecords_returnsFirstRecord() {
        Record first = Record.newRecord(testName, Type.A, DClass.IN);
        Record second = Record.newRecord(testName, Type.AAAA, DClass.IN);
        message.addRecord(first, Section.QUESTION);
        message.addRecord(second, Section.QUESTION);
        assertSame(first, message.getQuestion(),
                "The first record added to the QUESTION section should be returned");
    }

    /** When the internal {@code sections} array itself is {@code null},
     * calling {@code getQuestion()} should throw a {@link NullPointerException}. */
    @Test
    void getQuestion_nullSectionsArray_throwsNPE() throws Exception {
        setPrivateField(message, "sections", null);
        assertThrows(NullPointerException.class, () -> message.getQuestion(),
                "With a null sections array, getQuestion() must throw NPE");
    }

    /** When the {@code sections} array is too short (no entry for {@code Section.QUESTION}),
     * calling {@code getQuestion()} should throw {@link ArrayIndexOutOfBoundsException}. */
    @Test
    void getQuestion_insufficientSectionsArray_throwsAIOOBE() throws Exception {
        // Create an empty array (length 0) and inject it.
        @SuppressWarnings("unchecked")
        List<Record>[] tiny = (List<Record>[]) new List[0];
        setPrivateField(message, "sections", tiny);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.getQuestion(),
                "With an undersized sections array, getQuestion() must throw AIOOBE");
    }

    /** When the QUESTION list exists but its first element is {@code null},
     * {@code getQuestion()} should return {@code null}. */
    @Test
    void getQuestion_firstElementNull_returnsNull() throws Exception {
        // Ensure the QUESTION list exists.
        List<Record> qList = new ArrayList<>();
        qList.add(null); // first element is null
        injectSectionList(message, Section.QUESTION, qList);
        assertNull(message.getQuestion(),
                "If the first element in QUESTION list is null, getQuestion() must return null");
    }

    /** After adding a record and then removing all records from the QUESTION section,
     * {@code getQuestion()} must return {@code null}. */
    @Test
    void getQuestion_afterRemoval_returnsNull() {
        Record r = Record.newRecord(testName, Type.A, DClass.IN);
        message.addRecord(r, Section.QUESTION);
        // Verify it is there first.
        assertSame(r, message.getQuestion(),
                "Pre‑condition: record should be returned before removal");
        // Remove all.
        message.removeAllRecords(Section.QUESTION);
        assertNull(message.getQuestion(),
                "After removal of all QUESTION records, getQuestion() should return null");
    }

    /** When a specific {@link Record} instance is added, {@code getQuestion()}
     * must return the exact same object (reference equality). */
    @Test
    void getQuestion_referenceEquality_returnsSameInstance() {
        Record r = Record.newRecord(testName, Type.A, DClass.IN);
        message.addRecord(r, Section.QUESTION);
        Record returned = message.getQuestion();
        assertSame(r, returned,
                "The returned Record must be the same instance that was added");
    }

    /* --------------------------------------------------------------------- *
     * Helper methods for reflective manipulation of the private `sections` *
     * field.                                                               *
     * --------------------------------------------------------------------- */

    /**
     * Sets a private field on the given object via reflection.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the private field
     * @param value     the new value to assign
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Injects a {@link List<Record>} into the {@code sections} array at the given
     * index (section). If the {@code sections} array is {@code null} or too short,
     * it is created with a size large enough to hold the requested index.
     *
     * @param msg       the {@link Message} instance to manipulate
     * @param section   the section index (e.g., {@link Section#QUESTION})
     * @param list      the list to store in that slot
     */
    @SuppressWarnings("unchecked")
    private static void injectSectionList(Message msg, int section, List<Record> list) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        if (sections == null || sections.length <= section) {
            // create a new array large enough
            List<Record>[] newSections = (List<Record>[]) new List[section + 1];
            if (sections != null) {
                System.arraycopy(sections, 0, newSections, 0, sections.length);
            }
            sections = newSections;
            f.set(msg, sections);
        }
        sections[section] = list;
    }
}
```