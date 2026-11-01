```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#addRecord(Record, int)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Lazy initialisation of a section list.</li>
 *   <li>Appending to an already‑initialised section.</li>
 *   <li>Header count updates.</li>
 *   <li>Behaviour with {@code null} records, {@code null} header, and {@code null} sections array.</li>
 *   <li>Invalid section indices causing {@link ArrayIndexOutOfBoundsException}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_addRecord_Test {

    private Message message;
    private Record sampleRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for every test.
        message = new Message();

        // Simple A record used by many tests.
        Name name = Name.fromConstantString("example.com.");
        sampleRecord = Record.newRecord(name, Type.A, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        message = null;
        sampleRecord = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    /* ---------------------------------------------------------------------- */
    /*  Existing tests (kept unchanged)                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    void addRecord_initializesSectionAndIncrementsCount() {
        // Pre‑condition: the ANSWER section is null (lazy‑initialized).
        assertNull(getInternalSectionArray(message)[Section.ANSWER]);

        // Act: add a record to the ANSWER section.
        message.addRecord(sampleRecord, Section.ANSWER);

        // Verify the section list is now non‑null and contains the record.
        List<Record> answerSection = message.getSection(Section.ANSWER);
        assertNotNull(answerSection, "Section should be initialized");
        assertEquals(1, answerSection.size(), "Section should contain exactly one record");
        assertSame(sampleRecord, answerSection.get(0), "The stored record should be the one added");

        // Verify the header count for the ANSWER section has been incremented.
        assertEquals(1, message.getHeader().getCount(Section.ANSWER),
                "Header count for ANSWER should be 1");
    }

    @Test
    void addRecord_multipleAddsAccumulateAndUpdateCount() {
        // Add three records to the ADDITIONAL section.
        Record r1 = Record.newRecord(Name.fromConstantString("a.example.com."), Type.A, DClass.IN);
        Record r2 = Record.newRecord(Name.fromConstantString("b.example.com."), Type.A, DClass.IN);
        Record r3 = Record.newRecord(Name.fromConstantString("c.example.com."), Type.A, DClass.IN);

        message.addRecord(r1, Section.ADDITIONAL);
        message.addRecord(r2, Section.ADDITIONAL);
        message.addRecord(r3, Section.ADDITIONAL);

        // Verify all three records are present in insertion order.
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        assertEquals(3, additional.size(), "Section should contain three records");
        assertEquals(r1, additional.get(0));
        assertEquals(r2, additional.get(1));
        assertEquals(r3, additional.get(2));

        // Verify the header count matches the number of records added.
        assertEquals(3, message.getHeader().getCount(Section.ADDITIONAL),
                "Header count for ADDITIONAL should be 3");
    }

    @Test
    void addRecord_toDifferentSectionsKeepsCountsSeparate() {
        Record q = Record.newRecord(Name.fromConstantString("q.example.com."), Type.A, DClass.IN);
        Record a = Record.newRecord(Name.fromConstantString("a.example.com."), Type.A, DClass.IN);
        Record auth = Record.newRecord(Name.fromConstantString("auth.example.com."), Type.A, DClass.IN);

        message.addRecord(q, Section.QUESTION);
        message.addRecord(a, Section.ANSWER);
        message.addRecord(auth, Section.AUTHORITY);

        // Verify each section contains exactly its record.
        assertEquals(1, message.getSection(Section.QUESTION).size());
        assertEquals(1, message.getSection(Section.ANSWER).size());
        assertEquals(1, message.getSection(Section.AUTHORITY).size());

        // Verify header counts are independent.
        assertEquals(1, message.getHeader().getCount(Section.QUESTION));
        assertEquals(1, message.getHeader().getCount(Section.ANSWER));
        assertEquals(1, message.getHeader().getCount(Section.AUTHORITY));
    }

    /* ---------------------------------------------------------------------- */
    /*  New tests derived from the JSON test cases                           */
    /* ---------------------------------------------------------------------- */

    /**
     * Group: addRecord_appendsToExistingSectionAndIncrementsHeader
     * The section already contains two records; adding a third should
     * append it and increase the header count by one.
     */
    @Test
    void addRecord_appendsToExistingSectionAndIncrementsHeader() {
        // Populate SECTION 2 (ANSWER) with two records.
        Record r1 = Record.newRecord(Name.fromConstantString("first.example.org."), Type.A, DClass.IN);
        Record r2 = Record.newRecord(Name.fromConstantString("second.example.org."), Type.A, DClass.IN);
        message.addRecord(r1, Section.ANSWER);
        message.addRecord(r2, Section.ANSWER);

        // New record to be appended.
        Record newRec = Record.newRecord(Name.fromConstantString("third.example.org."), Type.A, DClass.IN);
        message.addRecord(newRec, Section.ANSWER);

        // Verify three records are present and order is preserved.
        List<Record> answer = message.getSection(Section.ANSWER);
        assertEquals(3, answer.size(), "Section should contain three records after append");
        assertEquals(r1, answer.get(0));
        assertEquals(r2, answer.get(1));
        assertEquals(newRec, answer.get(2));

        // Header count must be 3.
        assertEquals(3, message.getHeader().getCount(Section.ANSWER),
                "Header count for ANSWER should be 3 after three inserts");
    }

    /**
     * Group: addRecord_invalidSectionThrowsException
     * Verify that using an out‑of‑range section index triggers
     * {@link ArrayIndexOutOfBoundsException}.
     */
    @ParameterizedTest
    @ValueSource(ints = { -1, 5 }) // -1 is negative, 5 exceeds the internal array size (4)
    void addRecord_invalidSectionThrowsException(int invalidSection) {
        // The method accesses the internal array directly, so an invalid index
        // should bubble up as ArrayIndexOutOfBoundsException.
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.addRecord(sampleRecord, invalidSection),
                "Adding a record to section " + invalidSection + " should throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Group: addRecord_nullRecordHandled
     * The method does not guard against a {@code null} record; it should still
     * create the list (if necessary), increment the header count and store the
     * {@code null} reference.
     */
    @Test
    void addRecord_nullRecordHandled() {
        // Ensure the QUESTION section is initially null.
        assertNull(getInternalSectionArray(message)[Section.QUESTION]);

        // Add a null record to the QUESTION section.
        message.addRecord(null, Section.QUESTION);

        // Section list must be created and contain a single null element.
        List<Record> question = message.getSection(Section.QUESTION);
        assertNotNull(question, "Section list should be initialised even for null record");
        assertEquals(1, question.size(), "Section should contain exactly one element");
        assertNull(question.get(0), "The stored element should be null");

        // Header count must have been incremented.
        assertEquals(1, message.getHeader().getCount(Section.QUESTION),
                "Header count for QUESTION should be 1 even when the record is null");
    }

    /**
     * Group: addRecord_nullHeaderThrowsException
     * When the {@code header} field is {@code null}, the call to
     * {@code header.incCount(section)} must throw a {@link NullPointerException}.
     */
    @Test
    void addRecord_nullHeaderThrowsException() {
        // Explicitly set the header to null via the public setter.
        message.setHeader(null);

        // Adding any record should now result in NPE.
        assertThrows(NullPointerException.class,
                () -> message.addRecord(sampleRecord, Section.QUESTION),
                "addRecord should throw NullPointerException when header is null");
    }

    /**
     * Group: addRecord_nullSectionsArrayThrowsException
     * If the internal {@code sections} array itself is {@code null},
     * accessing {@code sections[section]} will cause a {@link NullPointerException}.
     */
    @Test
    void addRecord_nullSectionsArrayThrowsException() throws Exception {
        // Use reflection to set the private 'sections' field to null.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);

        // Any attempt to add a record now triggers NPE.
        assertThrows(NullPointerException.class,
                () -> message.addRecord(sampleRecord, Section.ANSWER),
                "addRecord should throw NullPointerException when internal sections array is null");
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper utilities                                                       */
    /* ---------------------------------------------------------------------- */

    /**
     * Exposes the private {@code sections} array via reflection.
     * Used by a few tests to assert lazy initialisation or to corrupt the
     * internal state deliberately.
     */
    private static List<Record>[] getInternalSectionArray(Message msg) {
        try {
            Field f = Message.class.getDeclaredField("sections");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Record>[] arr = (List<Record>[]) f.get(msg);
            return arr;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
```