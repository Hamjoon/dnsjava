```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.xbill.DNS.Name;
import org.xbill.DNS.Type;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Section;

/**
 * Unit tests for {@link Message#removeRecord(Record, int)}.
 *
 * The tests manipulate the internal {@code sections} array and {@code header}
 * field of {@link Message} via reflection so that edge‑cases (null sections,
 * immutable lists, etc.) can be exercised without relying on the public API
 * to create those states.
 */
@ExtendWith(MockitoExtension.class)
class Message_removeRecord_Test {

    private Message message;
    private Record aRecord;
    private Record txtRecord;

    @BeforeEach
    void setupBeforeEach() {
        // fresh Message for each test
        message = new Message();
        // generic records: A and TXT types for the same name
        aRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
        txtRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.TXT, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
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
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER), "Header count should be 1 after adding the record");

        boolean removed = message.removeRecord(aRecord, Section.ANSWER);

        assertTrue(removed, "removeRecord should return true when the record exists");
        assertEquals(0, header.getCount(Section.ANSWER), "Header count should be decremented after removal");
        assertTrue(message.getSection(Section.ANSWER).isEmpty(), "Section should be empty after the record is removed");
    }

    @Test
    void removeRecord_recordNotPresent_returnsFalseAndHeaderUnchanged() {
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER));

        boolean removed = message.removeRecord(txtRecord, Section.ANSWER);

        assertFalse(removed, "removeRecord should return false when the record is absent");
        assertEquals(1, header.getCount(Section.ANSWER), "Header count must remain unchanged when removal fails");
        assertEquals(1, message.getSection(Section.ANSWER).size(), "The original record should still be present in the section");
        assertSame(aRecord, message.getSection(Section.ANSWER).get(0));
    }

    @Test
    void removeRecord_sectionNeverInitialized_returnsFalse() {
        // no records added – sections are still null
        boolean removed = message.removeRecord(aRecord, Section.AUTHORITY);
        assertFalse(removed, "removeRecord must return false when the section list is null");
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
    }

    @Test
    void removeRecord_invalidSection_throwsIllegalArgumentException() {
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertThrows(IllegalArgumentException.class,
                () -> message.removeRecord(aRecord, 5),
                "Section.check should reject an out‑of‑range section index");
    }

    @Test
    void removeRecord_invalidUpperBoundSection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, 5));
    }

    @Test
    void removeRecord_emptySectionList_returnsFalse() throws Exception {
        List<Record>[] sections = new List[4];
        sections[Section.ANSWER] = new ArrayList<>();
        setSectionsArray(sections);

        boolean removed = message.removeRecord(aRecord, Section.ANSWER);

        assertFalse(removed);
        assertEquals(0, message.getHeader().getCount(Section.ANSWER));
    }

    @Test
    void removeRecord_duplicateRecord_removesOneAndDecrementsHeaderOnce() throws Exception {
        Record dup = mock(Record.class);
        List<Record> list = new ArrayList<>(Arrays.asList(dup, dup));
        List<Record>[] sections = new List[4];
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);

        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.AUTHORITY)).thenReturn(2);
        setHeader(mockHeader);

        boolean removed = message.removeRecord(dup, Section.AUTHORITY);

        assertTrue(removed);
        assertEquals(1, list.size());
        verify(mockHeader, times(1)).decCount(Section.AUTHORITY);
    }

    @Test
    void removeRecord_nullRecordInList_successfulRemoval() throws Exception {
        List<Record> list = new ArrayList<>(Collections.singletonList(null));
        List<Record>[] sections = new List[4];
        sections[Section.ADDITIONAL] = list;
        setSectionsArray(sections);

        Header mockHeader = mock(Header.class);
        setHeader(mockHeader);

        boolean removed = message.removeRecord(null, Section.ADDITIONAL);

        assertTrue(removed);
        assertTrue(list.isEmpty());
        verify(mockHeader, times(1)).decCount(Section.ADDITIONAL);
    }

    @Test
    void removeRecord_immutableSectionList_throwsUnsupportedOperationException() throws Exception {
        List<Record>[] sections = new List[4];
        sections[Section.QUESTION] = Collections.emptyList(); // immutable
        setSectionsArray(sections);

        assertThrows(UnsupportedOperationException.class,
                () -> message.removeRecord(aRecord, Section.QUESTION));
    }

    @Test
    void removeRecord_nullSectionsArray_throwsNullPointerException() throws Exception {
        setSectionsArray(null);
        assertThrows(NullPointerException.class,
                () -> message.removeRecord(aRecord, Section.ANSWER));
    }

    @Test
    void removeRecord_nullHeader_throwsNullPointerException() throws Exception {
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);
        setHeader(null);

        assertThrows(NullPointerException.class,
                () -> message.removeRecord(aRecord, Section.AUTHORITY));
    }

    @Test
    void removeRecord_headerDecCountThrows_IllegalArgumentException() throws Exception {
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.ANSWER] = list;
        setSectionsArray(sections);

        Header mockHeader = mock(Header.class);
        doThrow(new IllegalArgumentException("count underflow"))
                .when(mockHeader).decCount(Section.ANSWER);
        setHeader(mockHeader);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> message.removeRecord(aRecord, Section.ANSWER));
        assertEquals("count underflow", ex.getMessage());
        assertTrue(list.isEmpty(), "record should have been removed before decCount failed");
    }

    @Test
    void removeRecord_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
        List<Record>[] shortArray = new List[2]; // valid indices 0,1
        setSectionsArray(shortArray);
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.removeRecord(aRecord, 3));
    }

    @ParameterizedTest(name = "[{index}] removeRecord null-section-list returns false")
    @CsvSource({
            "mockRecordA,2,false",
            "mockRecordB,1,false"
    })
    void removeRecord_nullSectionList_cases(String recordName, int section, boolean expected) throws Exception {
        Record mockRecord = mock(Record.class, recordName);
        List<Record>[] sections = new List[4];
        // leave sections[section] == null
        setSectionsArray(sections);
        boolean result = message.removeRecord(mockRecord, section);
        assertEquals(expected, result);
    }

    @Test
    void removeRecord_sectionListWithoutRecord_returnsFalseAndNoHeaderDec() throws Exception {
        Record present = mock(Record.class, "presentRecord");
        Record absent = mock(Record.class, "absentRecord");
        List<Record> list = new ArrayList<>(Collections.singletonList(present));
        List<Record>[] sections = new List[4];
        sections[Section.ANSWER] = list;
        setSectionsArray(sections);

        Header mockHeader = mock(Header.class);
        setHeader(mockHeader);

        boolean removed = message.removeRecord(absent, Section.ANSWER);

        assertFalse(removed);
        verify(mockHeader, never()).decCount(anyInt());
    }

    @Test
    void removeRecord_sectionContainsRecord_returnsTrueAndCallsHeader() throws Exception {
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.ADDITIONAL] = list;
        setSectionsArray(sections);

        Header mockHeader = mock(Header.class);
        setHeader(mockHeader);

        boolean removed = message.removeRecord(aRecord, Section.ADDITIONAL);

        assertTrue(removed);
        assertTrue(list.isEmpty());
        verify(mockHeader, times(1)).decCount(Section.ADDITIONAL);
    }

    @Test
    void removeRecord_negativeSection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> message.removeRecord(aRecord, -1));
    }
}
```