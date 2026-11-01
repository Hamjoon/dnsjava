package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class Message_getOPT_Test {

    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message instance for each test
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse
        message = null;
    }

    @Test
    void getOPT_whenNoOPTRecord_returnsNull() {
        assertNull(message.getOPT(), "Expected getOPT() to return null when no OPTRecord is present");
    }

    @Test
    void getOPT_whenOPTRecordInAdditionalSection_returnsOPT() {
        // Create a mock OPTRecord – it will behave as an instance of OPTRecord
        OPTRecord opt = mock(OPTRecord.class);
        // Add it to the ADDITIONAL section
        message.addRecord(opt, Section.ADDITIONAL);
        // Verify that getOPT() returns the same instance
        assertSame(opt, message.getOPT(), "Expected getOPT() to return the OPTRecord that was added");
    }

    @Test
    void getOPT_whenOPTRecordInOtherSection_returnsNull() {
        OPTRecord opt = mock(OPTRecord.class);
        // Add the OPTRecord to a non‑additional section (e.g., ANSWER)
        message.addRecord(opt, Section.ANSWER);
        assertNull(message.getOPT(), "Expected getOPT() to return null when OPTRecord is not in the ADDITIONAL section");
    }

    @Test
    void getOPT_whenMultipleRecords_firstOPTReturned() {
        // Non‑OPT record (mocked as generic Record)
        Record nonOpt = mock(Record.class);
        // Two different OPTRecord mocks
        OPTRecord opt1 = mock(OPTRecord.class);
        OPTRecord opt2 = mock(OPTRecord.class);
        // Insert records in the order: non‑OPT, opt1, opt2
        message.addRecord(nonOpt, Section.ADDITIONAL);
        message.addRecord(opt1, Section.ADDITIONAL);
        message.addRecord(opt2, Section.ADDITIONAL);
        // getOPT should return the first OPTRecord encountered (opt1)
        assertSame(opt1, message.getOPT(), "Expected getOPT() to return the first OPTRecord in the ADDITIONAL section");
    }

    @Test
    void getOPT_whenSingleOPTRecordPresent_returnsInstance() {
        // The ADDITIONAL section contains exactly one OPTRecord.
        OPTRecord opt = mock(OPTRecord.class);
        message.addRecord(opt, Section.ADDITIONAL);
        assertSame(opt, message.getOPT(), "Expected getOPT() to return the sole OPTRecord present in the ADDITIONAL section");
    }

    @Test
    void getOPT_whenFirstRecordIsOPT_returnsFirstOPT() {
        // ADDITIONAL section: OPT, then other records.
        OPTRecord opt = mock(OPTRecord.class);
        Record a = mock(Record.class);
        Record txt = mock(Record.class);
        message.addRecord(opt, Section.ADDITIONAL);
        message.addRecord(a, Section.ADDITIONAL);
        message.addRecord(txt, Section.ADDITIONAL);
        assertSame(opt, message.getOPT(), "Expected getOPT() to return the first OPTRecord (the very first element)");
    }

    @Test
    void getOPT_whenOPTNotFirstButPresent_returnsOPT() {
        // ADDITIONAL section: non‑OPT, non‑OPT, OPT.
        Record a = mock(Record.class);
        Record txt = mock(Record.class);
        OPTRecord opt = mock(OPTRecord.class);
        message.addRecord(a, Section.ADDITIONAL);
        message.addRecord(txt, Section.ADDITIONAL);
        message.addRecord(opt, Section.ADDITIONAL);
        assertSame(opt, message.getOPT(), "Expected getOPT() to locate and return the OPTRecord even when it is not the first element");
    }

    @Test
    void getOPT_whenMultipleOPTRecordsPresent_returnsFirstOPT() {
        // ADDITIONAL section: OPT1, OPT2, OPT3.
        OPTRecord opt1 = mock(OPTRecord.class);
        OPTRecord opt2 = mock(OPTRecord.class);
        OPTRecord opt3 = mock(OPTRecord.class);
        message.addRecord(opt1, Section.ADDITIONAL);
        message.addRecord(opt2, Section.ADDITIONAL);
        message.addRecord(opt3, Section.ADDITIONAL);
        assertSame(opt1, message.getOPT(), "Expected getOPT() to return the first OPTRecord encountered and ignore the rest");
    }

    @Test
    void getOPT_whenNoOPTRecordPresent_returnsNull() {
        // ADDITIONAL section contains only non‑OPT records.
        Record a = mock(Record.class);
        Record mx = mock(Record.class);
        message.addRecord(a, Section.ADDITIONAL);
        message.addRecord(mx, Section.ADDITIONAL);
        assertNull(message.getOPT(), "Expected getOPT() to return null when the ADDITIONAL section has no OPTRecord");
    }

    @Test
    void getOPT_whenAdditionalSectionIsEmpty_returnsNull() {
        // No records added at all – the section list is empty.
        assertNull(message.getOPT(), "Expected getOPT() to return null when the ADDITIONAL section is empty");
    }

    @Test
    void getOPT_whenOPTRecordInNonAdditionalSection_returnsNull() {
        // OPTRecord added to ANSWER section; should not be found.
        OPTRecord opt = mock(OPTRecord.class);
        message.addRecord(opt, Section.ANSWER);
        assertNull(message.getOPT(), "Expected getOPT() to return null when OPTRecord resides outside the ADDITIONAL section");
    }

    @Test
    void getOPT_whenOPTRecordRemovedAfterAddition_returnsNull() {
        // Add then remove the OPTRecord.
        OPTRecord opt = mock(OPTRecord.class);
        message.addRecord(opt, Section.ADDITIONAL);
        assertSame(opt, message.getOPT(), "Pre‑condition: OPTRecord must be found after addition");
        boolean removed = message.removeRecord(opt, Section.ADDITIONAL);
        assertTrue(removed, "Pre‑condition: removeRecord should report successful removal");
        assertNull(message.getOPT(), "Expected getOPT() to return null after the OPTRecord has been removed");
    }

    @Test
    void getOPT_whenAdditionalContainsNullsOnly_returnsNull() {
        // Stub getSection to return a list containing a single null element.
        Message spy = spy(new Message());
        List<Record> listWithNull = Collections.singletonList(null);
        when(spy.getSection(Section.ADDITIONAL)).thenReturn(listWithNull);
        assertNull(spy.getOPT(), "Expected getOPT() to safely skip null entries and return null when no OPTRecord is present");
    }

    @Test
    void getOPT_whenAdditionalContainsNullsAndLaterOPT_returnsOPT() {
        // List: null, non‑OPT, OPT.
        Record nonOpt = mock(Record.class);
        OPTRecord opt = mock(OPTRecord.class);
        List<Record> custom = new ArrayList<>(Arrays.asList(null, nonOpt, opt));
        Message spy = spy(new Message());
        when(spy.getSection(Section.ADDITIONAL)).thenReturn(custom);
        assertSame(opt, spy.getOPT(), "Expected getOPT() to ignore leading nulls and return the later OPTRecord");
    }

    @Test
    void getOPT_whenAdditionalSectionListIsNull_throwsNullPointerException() {
        // Stub getSection to return null – the for‑each loop will throw NPE.
        Message spy = spy(new Message());
        when(spy.getSection(Section.ADDITIONAL)).thenReturn(null);
        assertThrows(NullPointerException.class, spy::getOPT, "Expected getOPT() to propagate NullPointerException when the internal list is null");
    }

    @Test
    void getOPT_whenAdditionalSectionListThrowsConcurrentModification_throwsException() {
        // Custom list whose iterator throws ConcurrentModificationException on next().
        List<Record> throwingList = new List<Record>() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<Record> iterator() {
                return new Iterator<Record>() {

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Record next() {
                        throw new ConcurrentModificationException("simulated concurrent modification");
                    }

                    @Override
                    public void remove() {
                    }
                };
            }

            // Remaining List methods are not used by getOPT(); they can throw UnsupportedOperationException.
            @Override
            public Object[] toArray() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean add(Record record) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends Record> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(int index, Collection<? extends Record> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Record get(int index) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Record set(int index, Record element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(int index, Record element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Record remove(int index) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int indexOf(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int lastIndexOf(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ListIterator<Record> listIterator() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ListIterator<Record> listIterator(int index) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Record> subList(int fromIndex, int toIndex) {
                throw new UnsupportedOperationException();
            }
        };
        Message spy = spy(new Message());
        when(spy.getSection(Section.ADDITIONAL)).thenReturn(throwingList);
        assertThrows(ConcurrentModificationException.class, spy::getOPT, "Expected getOPT() to propagate ConcurrentModificationException from the underlying list");
    }

    @Test
    void getOPT_whenGetSectionThrowsIllegalArgumentException_propagatesException() {
        // Stub getSection to throw IllegalArgumentException.
        Message spy = spy(new Message());
        when(spy.getSection(Section.ADDITIONAL)).thenThrow(new IllegalArgumentException("bad section"));
        assertThrows(IllegalArgumentException.class, spy::getOPT, "Expected getOPT() to propagate IllegalArgumentException thrown by getSection");
    }
}
