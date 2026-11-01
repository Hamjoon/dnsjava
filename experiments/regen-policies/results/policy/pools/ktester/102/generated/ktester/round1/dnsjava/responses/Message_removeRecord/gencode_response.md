**Generated unit‑test class**

```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link Message#removeRecord(Record, int)}.
 *
 * The test suite covers the normal behaviour as well as a large set of edge‑cases
 * described in the JSON test‑case specification (null sections array, immutable
 * section list, duplicate records, header failures, etc.).
 *
 * The {@code @BeforeEach} method creates a fresh {@link Message} instance and two
 * simple {@link Record}s that are used by the majority of the tests.
 *
 * For the more exotic scenarios we use reflection to replace the private
 * {@code sections} array or the {@code header} field, and we employ Mockito to
 * provide a mock {@link Header} whose {@code decCount} method can be verified or
 * forced to throw.
 */
@ExtendWith(MockitoExtension.class)
class Message_removeRecord_Test {

    private Message message;
    private Record aRecord;
    private Record txtRecord;

    /* --------------------------------------------------------------------- */
    /*  Common test fixture                                                 */
    /* --------------------------------------------------------------------- */
    @BeforeEach
    void setupBeforeEach() {
        // fresh Message for every test
        message = new Message();

        // simple concrete records (type A and TXT)
        aRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
        txtRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.TXT, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        message = null;
        aRecord = null;
        txtRecord = null;
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods to manipulate private fields                          */
    /* --------------------------------------------------------------------- */
    /** Replace the internal {@code sections} array via reflection. */
    private void setSectionsArray(List<Record>[] sections) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(message, sections);
    }

    /** Replace the internal {@code header} field via reflection. */
    private void setHeader(Header hdr) throws Exception {
        Field f = Message.class.getDeclaredField("header");
        f.setAccessible(true);
        f.set(message, hdr);
    }

    /* --------------------------------------------------------------------- */
    /*  1. Normal successful removal                                         */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_successfulRemoval_decrementsHeaderCount() {
        // GIVEN
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER));

        // WHEN
        boolean removed = message.removeRecord(aRecord, Section.ANSWER);

        // THEN
        assertTrue(removed);
        assertEquals(0, header.getCount(Section.ANSWER));
        assertTrue(message.getSection(Section.ANSWER).isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  2. Record not present                                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_recordNotPresent_returnsFalseAndHeaderUnchanged() {
        // GIVEN
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER));

        // WHEN
        boolean removed = message.removeRecord(txtRecord, Section.ANSWER);

        // THEN
        assertFalse(removed);
        assertEquals(1, header.getCount(Section.ANSWER));
        assertEquals(1, message.getSection(Section.ANSWER).size());
        assertSame(aRecord, message.getSection(Section.ANSWER).get(0));
    }

    /* --------------------------------------------------------------------- */
    /*  3. Section never initialised (null list)                             */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_sectionNeverInitialized_returnsFalse() throws Exception {
        // GIVEN – a Message with the default (null) section list for AUTHORITY
        // (no additional setup required)

        // WHEN
        boolean removed = message.removeRecord(aRecord, Section.AUTHORITY);

        // THEN
        assertFalse(removed);
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
    }

    /* --------------------------------------------------------------------- */
    /*  4. Invalid section index (negative)                                  */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_invalidSection_throwsIllegalArgumentException() {
        // GIVEN
        message.addRecord(aRecord, Section.ADDITIONAL);

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> message.removeRecord(aRecord, -1));
    }

    /* --------------------------------------------------------------------- */
    /*  5. Invalid section index (upper bound)                               */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_invalidUpperBoundSection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> message.removeRecord(aRecord, 5));
    }

    /* --------------------------------------------------------------------- */
    /*  6. Section list exists but is empty                                  */
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
    /*  7. Duplicate record – only first occurrence removed                  */
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
        assertEquals(1, list.size()); // one occurrence remains
        verify(mockHeader, times(1)).decCount(Section.AUTHORITY);
    }

    /* --------------------------------------------------------------------- */
    /*  8. Null record in section list, caller passes null                  */
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
    /*  9. Immutable section list – removal throws UnsupportedOperationException */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_immutableSectionList_throwsUnsupportedOperationException() throws Exception {
        // GIVEN – immutable list for section 0 (QUESTION)
        List<Record>[] sections = new List[4];
        sections[Section.QUESTION] = Collections.emptyList(); // immutable
        setSectionsArray(sections);

        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class,
                () -> message.removeRecord(aRecord, Section.QUESTION));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Internal `sections` array is null                                   */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_nullSectionsArray_throwsNullPointerException() throws Exception {
        // GIVEN – force the internal sections array to null
        setSectionsArray(null);

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> message.removeRecord(aRecord, Section.ANSWER));
    }

    /* --------------------------------------------------------------------- */
    /* 11. Header field is null                                                */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_nullHeader_throwsNullPointerException() throws Exception {
        // GIVEN – a mutable list containing the record
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);
        setHeader(null); // make header null

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> message.removeRecord(aRecord, Section.AUTHORITY));
    }

    /* --------------------------------------------------------------------- */
    /* 12. Header.decCount throws IllegalArgumentException (count underflow) */
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
        doThrow(new IllegalArgumentException("count underflow"))
                .when(mockHeader).decCount(Section.ANSWER);
        setHeader(mockHeader);

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> message.removeRecord(aRecord, Section.ANSWER));
        assertEquals("count underflow", ex.getMessage());

        // The record has already been removed from the list (method performs remove before decCount)
        assertTrue(list.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 13. Sections array too short (ArrayIndexOutOfBoundsException)          */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
        // GIVEN – sections array length = 2 (valid indices 0,1), we will use index 3
        List<Record>[] shortArray = new List[2];
        setSectionsArray(shortArray);

        // WHEN / THEN
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.removeRecord(aRecord, 3));
    }

    /* --------------------------------------------------------------------- */
    /* 14. The mockRecord cases from the original JSON (section 2 null list) */
    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] removeRecord null-section-list returns false")
    @CsvSource({
            // record value is irrelevant because section list is null
            "mockRecordA,2,false",
            "mockRecordB,1,false"
    })
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
    /* 15. Section list exists but does not contain supplied record           */
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
    /* 16. Section list contains the record (standard success)               */
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
    /* 17. Invalid negative section (already covered, retained for completeness) */
    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_negativeSection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> message.removeRecord(aRecord, -1));
    }

}
```