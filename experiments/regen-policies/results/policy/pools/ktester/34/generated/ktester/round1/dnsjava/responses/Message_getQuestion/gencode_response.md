```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Message#getQuestion()} covering normal and edge‑case behaviours.
 *
 * <p>Scenarios exercised:
 * <ul>
 *   <li>QUESTION section is {@code null} → {@code null} is returned.</li>
 *   <li>QUESTION section is an empty {@link List} → {@code null} is returned.</li>
 *   <li>First element of the QUESTION list is {@code null} → {@code null} is returned.</li>
 *   <li>Multiple question records → the first record is returned.</li>
 *   <li>Factory method {@link Message#newQuery(Record)} correctly populates the question.</li>
 *   <li>Internal {@code sections} array is {@code null} → {@link NullPointerException} propagates.</li>
 *   <li>Internal {@code sections} array is too short → {@link ArrayIndexOutOfBoundsException} propagates.</li>
 *   <li>Underlying list implementation throws an exception on {@code get(0)} → the same exception propagates.</li>
 * </ul>
 *
 * <p>Reflection is used only for the tests that need to tamper with the private {@code sections}
 * field; production code never accesses private members directly.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_getQuestion_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /*  Normal / simple behaviours                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void getQuestion_EmptySection_ReturnsNull() {
        Message msg = new Message();               // all sections are empty by default
        assertNull(msg.getQuestion(), "Expected null when QUESTION section is null or empty");
    }

    @Test
    void getQuestion_SingleRecord_ReturnsThatRecord() {
        Name name = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = new Message();
        msg.addRecord(question, Section.QUESTION);

        Record result = msg.getQuestion();
        assertNotNull(result, "Expected a non‑null Record");
        assertEquals(question, result, "The returned Record should be the one that was added");
    }

    @Test
    void getQuestion_MultipleRecords_ReturnsFirstRecord() {
        Name name1 = Name.fromConstantString("first.example.com.");
        Name name2 = Name.fromConstantString("second.example.com.");
        Record q1 = Record.newRecord(name1, Type.A, DClass.IN);
        Record q2 = Record.newRecord(name2, Type.AAAA, DClass.IN);

        Message msg = new Message();
        msg.addRecord(q1, Section.QUESTION);
        msg.addRecord(q2, Section.QUESTION); // second question record

        Record result = msg.getQuestion();
        assertNotNull(result);
        assertEquals(q1, result, "The first question record should be returned");
    }

    @Test
    void getQuestion_NewQueryFactory_ReturnsProvidedQuestion() {
        Name name = Name.fromConstantString("www.google.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);

        Message query = Message.newQuery(question);
        Record result = query.getQuestion();

        assertNotNull(result);
        assertEquals(question, result);
    }

    /* --------------------------------------------------------------------- */
    /*  Edge cases involving the internal 'sections' array                  */
    /* --------------------------------------------------------------------- */

    @Test
    void getQuestion_QuestionListFirstElementNull_ReturnsNull() throws Exception {
        // Build a Message with a QUESTION list whose first entry is null.
        Message msg = new Message();

        // Use reflection to replace the QUESTION section with a custom list.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);

        List<Record> customList = new ArrayList<>();
        customList.add(null);                                   // first element = null
        customList.add(Record.newRecord(Name.fromConstantString("valid.example."), Type.A, DClass.IN));

        // Ensure the array is large enough (Section.QUESTION is 0 in the library)
        if (sections == null || sections.length <= Section.QUESTION) {
            // recreate a correctly sized array if the constructor left it null
            sections = (List<Record>[]) new List[Section.MAX];
            sectionsField.set(msg, sections);
        }
        sections[Section.QUESTION] = customList;

        // The method should see a non‑empty list and return the first element (null)
        Record result = msg.getQuestion();
        assertNull(result, "When the first element of the QUESTION list is null, getQuestion() should return null");
    }

    @Test
    void getQuestion_InternalSectionsArrayIsNull_ThrowsNullPointerException() throws Exception {
        Message msg = new Message();

        // Force the private 'sections' field to be null.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(msg, null);

        // Expect a NPE because the method tries to index into a null array.
        assertThrows(NullPointerException.class,
                msg::getQuestion,
                "Accessing getQuestion() with a null internal sections array should throw NullPointerException");
    }

    @Test
    void getQuestion_InternalSectionsArrayTooShort_ThrowsArrayIndexOutOfBoundsException() throws Exception {
        Message msg = new Message();

        // Replace with an empty array (length 0). Section.QUESTION is 0, so indexing fails.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] emptyArray = (List<Record>[]) new List[0];
        sectionsField.set(msg, emptyArray);

        assertThrows(ArrayIndexOutOfBoundsException.class,
                msg::getQuestion,
                "Accessing getQuestion() when sections array is too short should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void getQuestion_UnderlyingListThrows_PropagatesException() throws Exception {
        Message msg = new Message();

        // Mock a List that pretends to contain one element but throws on get(0).
        @SuppressWarnings("unchecked")
        List<Record> throwingList = mock(List.class);
        when(throwingList.isEmpty()).thenReturn(false);
        when(throwingList.get(0)).thenThrow(new IllegalStateException("boom"));
        // size() is not used by getQuestion(), but we stub it for completeness.
        when(throwingList.size()).thenReturn(1);

        // Inject the mocked list into the QUESTION section via reflection.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        if (sections == null || sections.length <= Section.QUESTION) {
            sections = (List<Record>[]) new List[Section.MAX];
            sectionsField.set(msg, sections);
        }
        sections[Section.QUESTION] = throwingList;

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                msg::getQuestion,
                "Exception from the underlying list should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  Helper constants (Section.MAX is not part of the public API; we use a
    /*  safe fallback size when we need to create a new array).                 */
    /* --------------------------------------------------------------------- */

    /**
     * The maximum number of sections defined by the library.
     * The actual value is internal; we use a small safe constant (4) which
     * matches the known sections: QUESTION, ANSWER, AUTHORITY, ADDITIONAL.
     */
    private static final int SECTION_ARRAY_SIZE = 4;
}
```