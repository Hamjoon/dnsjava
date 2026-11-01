package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class Message_removeRecord_Test {

    /**
     * Helper to create a simple TXT record used in the tests.
     */
    private static Record createTxtRecord(String nameStr, String txt) {
        Name name = Name.fromConstantString(nameStr);
        return new TXTRecord(name, DClass.IN, 3600, txt);
    }

    /**
     * Helper to create a simple A record (used when a non‑TXT type is required).
     */
    private static Record createARecord(String nameStr) throws Exception {
        Name name = Name.fromConstantString(nameStr);
        InetAddress addr = InetAddress.getByName("127.0.0.1");
        return new ARecord(name, DClass.IN, 3600, addr);
    }

    @Test
    void removeRecord_successfulRemoval_returnsTrueAndUpdatesHeader() {
        // Arrange – create a message and add a record to the ANSWER section.
        Message msg = new Message();
        Record txt = createTxtRecord("test.example.", "sample text");
        msg.addRecord(txt, Section.ANSWER);
        // Verify the header count was incremented by addRecord.
        Header hdr = msg.getHeader();
        assertEquals(1, hdr.getCount(Section.ANSWER), "Header count should be 1 after adding a record");
        // Act – remove the same record.
        boolean removed = msg.removeRecord(txt, Section.ANSWER);
        // Assert – removal succeeded, header count decremented, section empty.
        assertTrue(removed, "removeRecord should return true when the record exists");
        assertEquals(0, hdr.getCount(Section.ANSWER), "Header count should be decremented after removal");
        assertTrue(msg.getSection(Section.ANSWER).isEmpty(), "Section should be empty after the record is removed");
    }

    @Test
    void removeRecord_recordNotPresent_returnsFalseAndHeaderUnchanged() {
        // Arrange – a fresh message with no records in the AUTHORITY section.
        Message msg = new Message();
        Record txt = createTxtRecord("absent.example.", "does not exist");
        // Header count for the section should be zero initially.
        Header hdr = msg.getHeader();
        assertEquals(0, hdr.getCount(Section.AUTHORITY), "Header count should start at 0 for an empty section");
        // Act – attempt to remove a record that was never added.
        boolean removed = msg.removeRecord(txt, Section.AUTHORITY);
        // Assert – removal fails, header count stays the same, section remains null/empty.
        assertFalse(removed, "removeRecord should return false when the record is absent");
        assertEquals(0, hdr.getCount(Section.AUTHORITY), "Header count must remain unchanged when removal fails");
        // getSection returns an empty list even if the internal list was never created.
        assertTrue(msg.getSection(Section.AUTHORITY).isEmpty(), "Empty section should still return an empty list");
    }

    @Test
    void removeRecord_sectionListNull_returnsFalseWithoutException() {
        // Arrange – create a message and do NOT add any records to the ADDITIONAL section.
        Message msg = new Message();
        // The internal list for Section.ADDITIONAL is still null at this point.
        // Create a dummy record to pass to removeRecord.
        Record dummy = createTxtRecord("dummy.example.", "dummy");
        // Act – call removeRecord on a null‑initialized section.
        boolean result = msg.removeRecord(dummy, Section.ADDITIONAL);
        // Assert – method should safely return false and not throw.
        assertFalse(result, "removeRecord must return false when the section list is null");
        // Header count must stay zero.
        assertEquals(0, msg.getHeader().getCount(Section.ADDITIONAL));
    }

    /* -------------------------------------------------------------
     *  Invalid section indexes – expect ArrayIndexOutOfBoundsException
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(ints = { -1, 10 })
    void removeRecord_invalidSectionIndex_throwsArrayIndexOutOfBounds(int invalidIndex) {
        Message msg = new Message();
        Record rec = createTxtRecord("any.example.", "any");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> msg.removeRecord(rec, invalidIndex), "Accessing an invalid section index should throw ArrayIndexOutOfBoundsException");
    }

    /* -------------------------------------------------------------
     *  Empty but non‑null list
     * ------------------------------------------------------------- */
    @Test
    void removeRecord_emptySectionList_returnsFalse() throws Exception {
        Message msg = new Message();
        // Force creation of an empty list for ANSWER section
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        // empty list
        sections[Section.ANSWER] = new ArrayList<>();
        Record txt = createTxtRecord("empty.com.", "empty");
        Header hdr = msg.getHeader();
        int before = hdr.getCount(Section.ANSWER);
        boolean removed = msg.removeRecord(txt, Section.ANSWER);
        assertFalse(removed, "Removal should fail when the list exists but is empty");
        assertEquals(before, hdr.getCount(Section.ANSWER), "Header count must stay unchanged for empty list removal");
    }

    /* -------------------------------------------------------------
     *  Null record argument – List.remove(null) returns false
     * ------------------------------------------------------------- */
    @Test
    void removeRecord_nullRecordParameter_returnsFalse() {
        Message msg = new Message();
        Record txt = createTxtRecord("some.example.", "some");
        msg.addRecord(txt, Section.AUTHORITY);
        Header hdr = msg.getHeader();
        int before = hdr.getCount(Section.AUTHORITY);
        boolean removed = msg.removeRecord(null, Section.AUTHORITY);
        assertFalse(removed, "removeRecord should return false when record argument is null");
        assertEquals(before, hdr.getCount(Section.AUTHORITY), "Header count must remain unchanged when null record is passed");
    }

    /* -------------------------------------------------------------
     *  Duplicate records – only one is removed, header.decCount once
     * ------------------------------------------------------------- */
    @Test
    void removeRecord_duplicateRecords_oneRemovedAndHeaderDecrementedOnce() throws Exception {
        Message msg = new Message();
        Record dup = createARecord("dup.com.");
        // Add the same record twice (list will contain two equal objects)
        msg.addRecord(dup, Section.ADDITIONAL);
        msg.addRecord(dup, Section.ADDITIONAL);
        Header mockHeader = mock(Header.class);
        // make decCount behave normally
        doNothing().when(mockHeader).decCount(Section.ADDITIONAL);
        msg.setHeader(mockHeader);
        boolean removed = msg.removeRecord(dup, Section.ADDITIONAL);
        assertTrue(removed, "removeRecord should succeed when duplicate records exist");
        List<Record> list = msg.getSection(Section.ADDITIONAL);
        assertEquals(1, list.size(), "Exactly one duplicate should remain after removal");
        // Verify decCount invoked exactly once
        verify(mockHeader, times(1)).decCount(Section.ADDITIONAL);
    }

    /* -------------------------------------------------------------
     *  Removal from QUESTION_SECTION (index 0)
     * ------------------------------------------------------------- */
    @Test
    void removeRecord_fromQuestionSection_successful() {
        Message msg = new Message();
        Record q = createTxtRecord("question.com.", "qtxt");
        msg.addRecord(q, Section.QUESTION);
        Header hdr = msg.getHeader();
        assertEquals(1, hdr.getCount(Section.QUESTION));
        boolean removed = msg.removeRecord(q, Section.QUESTION);
        assertTrue(removed, "removeRecord should succeed for QUESTION section");
        assertEquals(0, hdr.getCount(Section.QUESTION));
        assertTrue(msg.getSection(Section.QUESTION).isEmpty(), "QUESTION section should be empty after removal");
    }

    /* -------------------------------------------------------------
     *  Header is null – NPE after successful List.remove
     * ------------------------------------------------------------- */
    @Test
    void removeRecord_headerIsNull_throwsNullPointerException() throws Exception {
        Message msg = new Message();
        Record txt = createTxtRecord("noheader.com.", "data");
        msg.addRecord(txt, Section.ANSWER);
        // Force header to null via reflection (or setHeader(null) if allowed)
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(msg, null);
        assertThrows(NullPointerException.class, () -> msg.removeRecord(txt, Section.ANSWER), "When header is null, decCount should cause NPE");
    }

    /* -------------------------------------------------------------
     *  Sections array is null – NPE before any logic
     * ------------------------------------------------------------- */
    @Test
    void removeRecord_sectionsArrayIsNull_throwsNullPointerException() throws Exception {
        Message msg = new Message();
        Record txt = createTxtRecord("nullsections.com.", "data");
        // Set the internal sections array to null via reflection
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(msg, null);
        assertThrows(NullPointerException.class, () -> msg.removeRecord(txt, Section.ANSWER), "Accessing a null sections array should throw NPE");
    }

    /* -------------------------------------------------------------
     *  Unmodifiable list – UnsupportedOperationException propagates
     * ------------------------------------------------------------- */
    @Test
    void removeRecord_unmodifiableSectionList_throwsUnsupportedOperationException() throws Exception {
        Message msg = new Message();
        Record txt = createTxtRecord("unmod.com.", "data");
        msg.addRecord(txt, Section.ADDITIONAL);
        // Replace the mutable list with an unmodifiable view
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        List<Record> unmod = Collections.unmodifiableList(new ArrayList<>(sections[Section.ADDITIONAL]));
        sections[Section.ADDITIONAL] = unmod;
        assertThrows(UnsupportedOperationException.class, () -> msg.removeRecord(txt, Section.ADDITIONAL), "removeRecord should propagate UnsupportedOperationException from unmodifiable list");
    }

    /* -------------------------------------------------------------
     *  Header.decCount throws IllegalArgumentException – propagates
     * ------------------------------------------------------------- */
    @Test
    void removeRecord_headerDecCountThrows_exceptionPropagates(@Mock Header mockHeader) {
        Message msg = new Message();
        Record txt = createTxtRecord("badheader.com.", "data");
        msg.addRecord(txt, Section.AUTHORITY);
        // Configure mock to throw when decCount is called
        doThrow(new IllegalArgumentException("bad count")).when(mockHeader).decCount(Section.AUTHORITY);
        msg.setHeader(mockHeader);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> msg.removeRecord(txt, Section.AUTHORITY), "Exception from Header.decCount should propagate");
        assertEquals("bad count", thrown.getMessage());
    }
}