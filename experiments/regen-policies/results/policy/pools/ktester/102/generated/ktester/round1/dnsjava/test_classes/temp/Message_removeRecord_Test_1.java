package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class Message_removeRecord_Test {

    private Message message;

    private Record aRecord;

    private Record txtRecord;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test
        message = new Message();
        // Create simple records to work with.
        // Record.newRecord creates a generic Record (type A for the first, TXT for the second)
        aRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
        txtRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.TXT, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        message = null;
        aRecord = null;
        txtRecord = null;
    }

    /**
     * Replace the internal {@code sections} array via reflection.
     */
    private void setSectionsArray(List<Record>[] sections) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(message, sections);
    }

    /**
     * Replace the internal {@code header} field via reflection.
     */
    private void setHeader(Header hdr) throws Exception {
        Field f = Message.class.getDeclaredField("header");
        f.setAccessible(true);
        f.set(message, hdr);
    }

    @Test
    void removeRecord_successfulRemoval_decrementsHeaderCount() {
        // GIVEN: a record added to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER), "Header count should be 1 after adding the record");
        // WHEN: we remove the same record
        boolean removed = message.removeRecord(aRecord, Section.ANSWER);
        // THEN: removal succeeds, header count is decremented, and the section is empty
        assertTrue(removed, "removeRecord should return true when the record exists");
        assertEquals(0, header.getCount(Section.ANSWER), "Header count should be decremented after removal");
        assertTrue(message.getSection(Section.ANSWER).isEmpty(), "Section should be empty after the record is removed");
    }

    @Test
    void removeRecord_recordNotPresent_returnsFalseAndHeaderUnchanged() {
        // GIVEN: a record added to the ANSWER section, but we will try to remove a different record
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER));
        // WHEN: we attempt to remove a record that was never added
        boolean removed = message.removeRecord(txtRecord, Section.ANSWER);
        // THEN: removal fails, header count stays the same, and the original record remains
        assertFalse(removed, "removeRecord should return false when the record is absent");
        assertEquals(1, header.getCount(Section.ANSWER), "Header count must remain unchanged when removal fails");
        assertEquals(1, message.getSection(Section.ANSWER).size(), "The original record should still be present in the section");
        assertSame(aRecord, message.getSection(Section.ANSWER).get(0));
    }

    @Test
    void removeRecord_sectionNeverInitialized_returnsFalse() {
        // GIVEN: a fresh Message with no sections initialized (no records added)
        // No explicit action needed – the constructor leaves all section lists null.
        // WHEN: we try to remove a record from a section that has never been created
        boolean removed = message.removeRecord(aRecord, Section.AUTHORITY);
        // THEN: removal should simply return false without throwing
        assertFalse(removed, "removeRecord must return false when the section list is null");
        // Header count for that section should still be zero
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
    }

    @Test
    void removeRecord_invalidSection_throwsIllegalArgumentException() {
        // GIVEN: a valid record added to a valid section
        message.addRecord(aRecord, Section.ADDITIONAL);
        // WHEN / THEN: passing an invalid section index should cause Section.check to throw
        assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, 5), "Section.check should reject an out‑of‑range section index");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_invalidUpperBoundSection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, 5));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_emptySectionList_returnsFalse() throws Exception {
        // GIVEN – initialise an empty mutable list for SECTION=2 (ANSWER)
        List<Record>[] sections = new List[4];
        sections[Section.ANSWER] = new ArrayList<>();
        setSectionsArray(sections);
        // WHEN
        boolean removed = message.removeRecord(aRecord, Section.ANSWER);
        // THEN
        assertFalse(removed);
        // Header should stay untouched – count remains 0
        assertEquals(0, message.getHeader().getCount(Section.ANSWER));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_duplicateRecord_removesOneAndDecrementsHeaderOnce() throws Exception {
        // GIVEN – list with the same mock record twice
        Record dup = mock(Record.class);
        List<Record> list = new ArrayList<>(Arrays.asList(dup, dup));
        List<Record>[] sections = new List[4];
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);
        // Ensure header count reflects two records
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.AUTHORITY)).thenReturn(2);
        setHeader(mockHeader);
        // WHEN
        boolean removed = message.removeRecord(dup, Section.AUTHORITY);
        // THEN
        assertTrue(removed);
        // one occurrence remains
        assertEquals(1, list.size());
        verify(mockHeader, times(1)).decCount(Section.AUTHORITY);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_nullRecordInList_successfulRemoval() throws Exception {
        // GIVEN – list contains a single null entry
        List<Record> list = new ArrayList<>(Collections.singletonList(null));
        List<Record>[] sections = new List[4];
        sections[Section.ADDITIONAL] = list;
        setSectionsArray(sections);
        // Mock header to verify decCount call
        Header mockHeader = mock(Header.class);
        setHeader(mockHeader);
        // WHEN
        boolean removed = message.removeRecord(null, Section.ADDITIONAL);
        // THEN
        assertTrue(removed);
        assertTrue(list.isEmpty());
        verify(mockHeader, times(1)).decCount(Section.ADDITIONAL);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_immutableSectionList_throwsUnsupportedOperationException() throws Exception {
        // GIVEN – immutable list for section 0 (QUESTION)
        List<Record>[] sections = new List[4];
        sections[Section.QUESTION] = Collections.emptyList();
        setSectionsArray(sections);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> message.removeRecord(aRecord, Section.QUESTION));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_nullSectionsArray_throwsNullPointerException() throws Exception {
        // GIVEN – force the internal sections array to null
        setSectionsArray(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> message.removeRecord(aRecord, Section.ANSWER));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_nullHeader_throwsNullPointerException() throws Exception {
        // GIVEN – a mutable list containing the record
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);
        // make header null
        setHeader(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> message.removeRecord(aRecord, Section.AUTHORITY));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_headerDecCountThrows_IllegalArgumentException() throws Exception {
        // GIVEN – list with the record
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.ANSWER] = list;
        setSectionsArray(sections);
        // Mock header whose decCount throws
        Header mockHeader = mock(Header.class);
        doThrow(new IllegalArgumentException("count underflow")).when(mockHeader).decCount(Section.ANSWER);
        setHeader(mockHeader);
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, Section.ANSWER));
        assertEquals("count underflow", ex.getMessage());
        // The record has already been removed from the list (method performs remove before decCount)
        assertTrue(list.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
        // GIVEN – sections array length = 2 (valid indices 0,1), we will use index 3
        List<Record>[] shortArray = new List[2];
        setSectionsArray(shortArray);
        // WHEN / THEN
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.removeRecord(aRecord, 3));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] removeRecord null-section-list returns false")
    @CsvSource({ // record value is irrelevant because section list is null
    "mockRecordA,2,false", "mockRecordB,1,false" })
    void removeRecord_nullSectionList_cases(String recordName, int section, boolean expected) throws Exception {
        // Resolve a mock Record (the actual identity does not matter)
        Record mockRecord = mock(Record.class, recordName);
        // Ensure the required section list is null, others stay untouched
        List<Record>[] sections = new List[4];
        // leave sections[section] == null
        setSectionsArray(sections);
        boolean result = message.removeRecord(mockRecord, section);
        assertEquals(expected, result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_sectionListWithoutRecord_returnsFalseAndNoHeaderDec() throws Exception {
        // GIVEN – mutable list with a different mock record
        Record present = mock(Record.class, "presentRecord");
        Record absent = mock(Record.class, "absentRecord");
        List<Record> list = new ArrayList<>(Collections.singletonList(present));
        List<Record>[] sections = new List[4];
        sections[Section.ANSWER] = list;
        setSectionsArray(sections);
        Header mockHeader = mock(Header.class);
        setHeader(mockHeader);
        // WHEN
        boolean removed = message.removeRecord(absent, Section.ANSWER);
        // THEN
        assertFalse(removed);
        verify(mockHeader, never()).decCount(anyInt());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_sectionContainsRecord_returnsTrueAndCallsHeader() throws Exception {
        // GIVEN – mutable list with the record
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.ADDITIONAL] = list;
        setSectionsArray(sections);
        Header mockHeader = mock(Header.class);
        setHeader(mockHeader);
        // WHEN
        boolean removed = message.removeRecord(aRecord, Section.ADDITIONAL);
        // THEN
        assertTrue(removed);
        assertTrue(list.isEmpty());
        verify(mockHeader, times(1)).decCount(Section.ADDITIONAL);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_negativeSection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, -1));
    }
}
