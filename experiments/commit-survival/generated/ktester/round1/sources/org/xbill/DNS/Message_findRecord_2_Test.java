package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class Message_findRecord_2_Test {

    private Message message;

    // If not available, we fallback to 4.
    private static final class Section {

        static final int QUESTION = 0;

        static final int ANSWER = 1;

        static final int AUTHORITY = 2;

        static final int ADDITIONAL = 3;

        static final int MAX = 4;
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

    /**
     * Verify that {@code findRecord} returns {@code true} when the record is
     * present in the ANSWER section.
     */
    @Test
    void findRecord_true_whenInAnswerSection() throws IOException {
        Name name = Name.fromConstantString("example.com.");
        // Create a simple TXT record.
        Record txt = new TXTRecord(name, DClass.IN, 3600, "test");
        // Add the record to the ANSWER section.
        message.addRecord(txt, Section.ANSWER);
        assertTrue(message.findRecord(txt), "findRecord should return true when the record is in the ANSWER section");
    }

    /**
     * Verify that {@code findRecord} returns {@code true} when the record is
     * present in the ADDITIONAL section.
     */
    @Test
    void findRecord_true_whenInAdditionalSection() throws IOException {
        Name name = Name.fromConstantString("additional.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "additional");
        message.addRecord(txt, Section.ADDITIONAL);
        assertTrue(message.findRecord(txt), "findRecord should return true when the record is in the ADDITIONAL section");
    }

    /**
     * Verify that {@code findRecord} returns {@code false} when the record is
     * not present in any section.
     */
    @Test
    void findRecord_false_whenAbsent() throws IOException {
        Name name1 = Name.fromConstantString("present.com.");
        Record present = new TXTRecord(name1, DClass.IN, 3600, "present");
        message.addRecord(present, Section.ANSWER);
        // Record that will *not* be added.
        Name name2 = Name.fromConstantString("absent.com.");
        Record absent = new TXTRecord(name2, DClass.IN, 3600, "absent");
        assertFalse(message.findRecord(absent), "findRecord should return false when the record is not in any section");
    }

    /**
     * Verify that {@code findRecord} scans all sections (QUESTION, ANSWER,
     * AUTHORITY, ADDITIONAL). The record is placed in the AUTHORITY section,
     * which is the third section the method checks.
     */
    @Test
    void findRecord_true_whenInAuthoritySection() throws IOException {
        Name name = Name.fromConstantString("authority.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "authority");
        message.addRecord(txt, Section.AUTHORITY);
        assertTrue(message.findRecord(txt), "findRecord should return true when the record is in the AUTHORITY section");
    }

    /**
     * Verify that the method works correctly when the same record appears in
     * multiple sections – it should still return {@code true}.
     */
    @Test
    void findRecord_true_whenRecordInMultipleSections() throws IOException {
        Name name = Name.fromConstantString("multi.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "multi");
        message.addRecord(txt, Section.ANSWER);
        message.addRecord(txt, Section.ADDITIONAL);
        assertTrue(message.findRecord(txt), "findRecord should return true when the record appears in more than one section");
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
        // now ANSWER list is empty but non‑null
        message.removeRecord(dummyRec, Section.ANSWER);
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

    @Test
    void findRecord_throws_NullPointerException_whenSectionsArrayIsNull() throws Exception {
        // Create a Message instance via reflection that has a null 'sections' field.
        Message broken = new Message();
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        // force null
        sectionsField.set(broken, null);
        Record a = new ARecord(Name.fromConstantString("example.com."), DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        assertThrows(NullPointerException.class, () -> broken.findRecord(a), "When internal sections array is null, findRecord should throw NPE");
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
        Record a = new ARecord(Name.fromConstantString("example.org."), DClass.IN, 3600, InetAddress.getByName("5.6.7.8"));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> broken.findRecord(a), "When sections array is undersized, accessing Section.ADDITIONAL should throw AIOOBE");
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
        Record txt = new TXTRecord(Name.fromConstantString("example.net."), DClass.IN, 3600, "boom");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> custom.findRecord(txt), "Custom list's exception should propagate");
        assertEquals("boom", ex.getMessage());
    }
}
