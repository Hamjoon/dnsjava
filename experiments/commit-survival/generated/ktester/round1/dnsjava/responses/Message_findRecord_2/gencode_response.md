```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#findRecord(Record)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>normal behaviour – record present in each of the four sections,
 *       absent, present in multiple sections, present only in QUESTION (should be ignored),
 *       null argument.</li>
 *   <li>branch coverage for empty / non‑null sections.</li>
 *   <li>edge cases involving removal of a record.</li>
 *   <li>exception scenarios created via reflection (null {@code sections},
 *       undersized array, custom list that throws).</li>
 * </ul>
 *
 * <p>All fixtures are built using the public API {@link Message#addRecord(Record, int)}
 * and {@link Message#removeRecord(Record, int)}; private fields are accessed only
 * when deliberately constructing an invalid state for exception tests.
 */
@ExtendWith(MockitoExtension.class)
class Message_findRecord_2_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor which creates an empty header and empty sections.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /* --------------------------------------------------------------------- */
    /*                     Normal / branch coverage tests                    */
    /* --------------------------------------------------------------------- */

    @Test
    void findRecord_true_whenInAnswerSection() throws IOException {
        Name name = Name.fromConstantString("example.com.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        message.addRecord(a, Section.ANSWER);
        assertTrue(message.findRecord(a), "Record should be found in ANSWER section");
    }

    @Test
    void findRecord_true_whenInAuthoritySection() throws IOException {
        Name name = Name.fromConstantString("authority.example.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("5.6.7.8"));
        message.addRecord(a, Section.AUTHORITY);
        assertTrue(message.findRecord(a), "Record should be found in AUTHORITY section");
    }

    @Test
    void findRecord_true_whenInAdditionalSection() throws IOException {
        Name name = Name.fromConstantString("additional.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "additional");
        message.addRecord(txt, Section.ADDITIONAL);
        assertTrue(message.findRecord(txt), "Record should be found in ADDITIONAL section");
    }

    @Test
    void findRecord_false_whenAbsent() throws IOException {
        Name name1 = Name.fromConstantString("present.com.");
        Record present = new ARecord(name1, DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        message.addRecord(present, Section.ANSWER);

        Name name2 = Name.fromConstantString("absent.com.");
        Record absent = new ARecord(name2, DClass.IN, 3600, InetAddress.getByName("9.9.9.9"));

        assertFalse(message.findRecord(absent), "Record not added anywhere must return false");
    }

    @Test
    void findRecord_true_whenRecordInMultipleSections() throws IOException {
        Name name = Name.fromConstantString("multi.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "multi");
        message.addRecord(txt, Section.ANSWER);
        message.addRecord(txt, Section.ADDITIONAL);
        assertTrue(message.findRecord(txt), "Record present in more than one section must return true");
    }

    @Test
    void findRecord_true_whenOnlyInQuestionSection() throws IOException {
        Name name = Name.fromConstantString("question.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "question");
        // QUESTION section is index 0, but findRecord starts at Section.ANSWER.
        message.addRecord(txt, Section.QUESTION);
        assertFalse(message.findRecord(txt), "Record only in QUESTION section must be ignored");
    }

    @Test
    void findRecord_nullArgument_returnsFalse() {
        // According to the implementation, List.contains(null) returns false.
        assertFalse(message.findRecord(null), "Calling findRecord(null) should return false");
    }

    @Test
    void findRecord_true_whenAnswerSectionEmptyButRecordInAuthority() throws IOException {
        // ANSWER section is created (non‑null) but left empty.
        // Authority contains the searched record.
        Name name = Name.fromConstantString("authority.only.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("5.6.7.8"));
        // Force creation of an empty list for ANSWER via addRecord with a different record.
        Name dummy = Name.fromConstantString("dummy.com.");
        Record dummyRec = new ARecord(dummy, DClass.IN, 3600, InetAddress.getByName("1.1.1.1"));
        message.addRecord(dummyRec, Section.ANSWER);
        message.removeRecord(dummyRec, Section.ANSWER); // now ANSWER list is empty but non‑null
        message.addRecord(a, Section.AUTHORITY);
        assertTrue(message.findRecord(a), "Record in AUTHORITY should be found even if ANSWER is empty");
    }

    @Test
    void findRecord_false_whenRecordRemoved() throws IOException {
        Name name = Name.fromConstantString("to.remove.com.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        message.addRecord(a, Section.ANSWER);
        // Ensure removal works
        assertTrue(message.removeRecord(a, Section.ANSWER), "Record should be removed successfully");
        assertFalse(message.findRecord(a), "After removal the record must not be found");
    }

    /* --------------------------------------------------------------------- */
    /*                     Exception scenario tests                         */
    /* --------------------------------------------------------------------- */

    @Test
    void findRecord_throws_NullPointerException_whenSectionsArrayIsNull() throws Exception {
        // Create a Message instance via reflection that has a null 'sections' field.
        Message broken = new Message();
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(broken, null); // force null

        Record a = new ARecord(Name.fromConstantString("example.com."),
                DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));

        assertThrows(NullPointerException.class,
                () -> broken.findRecord(a),
                "When internal sections array is null, findRecord should throw NPE");
    }

    @Test
    void findRecord_throws_ArrayIndexOutOfBoundsException_whenSectionsArrayTooSmall() throws Exception {
        // Build a Message whose 'sections' array length is 2 (valid indices are 0‑1)
        Message broken = new Message();
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] shortArray = (List<Record>[]) new List[2];
        sectionsField.set(broken, shortArray);

        Record a = new ARecord(Name.fromConstantString("example.org."),
                DClass.IN, 3600, InetAddress.getByName("5.6.7.8"));

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> broken.findRecord(a),
                "When sections array is undersized, accessing Section.ADDITIONAL should throw AIOOBE");
    }

    @Test
    void findRecord_propagates_IllegalStateException_fromCustomList() throws Exception {
        // Insert a custom List implementation that throws from contains()
        Message custom = new Message();
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] arr = (List<Record>[]) new List[Section.MAX];
        // Fill with nulls first
        for (int i = 0; i < arr.length; i++) {
            arr[i] = null;
        }
        // Place the hostile list in the ANSWER slot
        arr[Section.ANSWER] = new ArrayList<Record>() {
            @Override
            public boolean contains(Object o) {
                throw new IllegalStateException("boom");
            }
        };
        sectionsField.set(custom, arr);

        Record txt = new TXTRecord(Name.fromConstantString("example.net."),
                DClass.IN, 3600, "boom");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> custom.findRecord(txt),
                "Custom list's exception should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*                     Helper constants (if needed)                     */
    /* --------------------------------------------------------------------- */

    // Section.MAX is not part of the public API; we approximate the needed size.
    // The dnsjava library defines Section.MAX = 4 (QUESTION, ANSWER, AUTHORITY, ADDITIONAL).
    // If not available, we fallback to 4.
    private static final class Section {
        static final int QUESTION = 0;
        static final int ANSWER = 1;
        static final int AUTHORITY = 2;
        static final int ADDITIONAL = 3;
        static final int MAX = 4;
    }
}
```