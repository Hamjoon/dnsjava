```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ConcurrentModificationException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link Message#getOPT()}.
 *
 * <p>The tests cover all scenarios described in the JSON test‑case specification:
 *
 * <ul>
 *   <li>retrieving the first {@link OPTRecord} when present,
 *   <li>returning {@code null} when no OPT record exists,
 *   <li>handling empty or {@code null} sections,
 *   <li>behaviour with {@code null} entries inside the section list,
 *   <li>propagation of unchecked exceptions thrown by {@code getSection(int)} or its iterator.
 * </ul>
 *
 * <p>No private members of {@link Message} are accessed directly; where required a
 * subclass overrides {@code getSection(int)} to simulate edge‑cases.
 */
class Message_getOPT_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------
     *  Basic happy‑path tests (already present in the original template)
     * -------------------------------------------------------------- */

    @Test
    void getOPT_noOptRecord_returnsNull() {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        msg.addRecord(aRecord, Section.ADDITIONAL);

        OPTRecord result = msg.getOPT();

        assertNull(result, "Expected getOPT() to return null when no OPTRecord is present");
    }

    @Test
    void getOPT_optRecordPresent_returnsSameInstance() {
        Message msg = new Message();
        OPTRecord opt = new OPTRecord(0, 0, 0, 0, Collections.emptyList());
        msg.addRecord(opt, Section.ADDITIONAL);

        OPTRecord result = msg.getOPT();

        assertNotNull(result, "Expected getOPT() to return a non‑null OPTRecord");
        assertSame(opt, result, "Expected getOPT() to return the same OPTRecord instance that was added");
    }

    /* --------------------------------------------------------------
     *  Additional records before the OPT record
     * -------------------------------------------------------------- */

    @Test
    void getOPT_optRecordAfterOtherRecords_returnsFirstOPT() {
        Message msg = new Message();

        // Add a few non‑OPT records first.
        Name name = Name.fromConstantString("example.com.");
        Record a = Record.newRecord(name, Type.A, DClass.IN);
        Record aaaa = Record.newRecord(name, Type.AAAA, DClass.IN);
        msg.addRecord(a, Section.ADDITIONAL);
        msg.addRecord(aaaa, Section.ADDITIONAL);

        // Add the OPT record we expect to be returned.
        OPTRecord opt = new OPTRecord(0, 0, 0, 0, Collections.emptyList());
        msg.addRecord(opt, Section.ADDITIONAL);

        // Add another OPT record – it should never be reached.
        OPTRecord opt2 = new OPTRecord(1234, 0, 0, 0, Collections.emptyList());
        msg.addRecord(opt2, Section.ADDITIONAL);

        OPTRecord result = msg.getOPT();

        assertSame(opt, result, "Expected the first OPTRecord in the ADDITIONAL section to be returned");
    }

    @Test
    void getOPT_multipleOPTRecords_returnsFirstOne() {
        Message msg = new Message();

        OPTRecord first = new OPTRecord(0, 0, 0, 0, Collections.emptyList());
        OPTRecord second = new OPTRecord(4096, 0, 0, 0, Collections.emptyList());

        msg.addRecord(first, Section.ADDITIONAL);
        msg.addRecord(second, Section.ADDITIONAL);

        OPTRecord result = msg.getOPT();

        assertSame(first, result, "Expected the first OPTRecord to be returned even when multiple are present");
    }

    @Test
    void getOPT_noRecordsInAdditional_returnsNull() {
        Message msg = new Message(); // default Message has empty sections
        assertTrue(msg.getSection(Section.ADDITIONAL).isEmpty(),
                "Pre‑condition: the ADDITIONAL section should be empty");

        OPTRecord result = msg.getOPT();

        assertNull(result, "Expected null when the ADDITIONAL section is empty");
    }

    /* --------------------------------------------------------------
     *  Null entries inside the list returned by getSection()
     * -------------------------------------------------------------- */

    @Test
    void getOPT_nullEntryFollowedByOPT_returnsFirstNonNullOPT() {
        // Build a list containing a leading null and then an OPTRecord.
        List<Record> customList = new ArrayList<>();
        customList.add(null);
        OPTRecord opt = new OPTRecord(0, 0, 0, 0, Collections.emptyList());
        customList.add(opt);
        customList.add(Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN));

        Message msg = new MessageWithCustomSection(customList);

        OPTRecord result = msg.getOPT();

        assertSame(opt, result, "Expected the first non‑null OPTRecord to be returned");
    }

    @Test
    void getOPT_allNullEntries_returnsNull() {
        List<Record> customList = new ArrayList<>();
        customList.add(null);
        customList.add(null);

        Message msg = new MessageWithCustomSection(customList);

        OPTRecord result = msg.getOPT();

        assertNull(result, "Expected null when the ADDITIONAL section contains only null entries");
    }

    /* --------------------------------------------------------------
     *  getSection returns null → NullPointerException
     * -------------------------------------------------------------- */

    @Test
    void getOPT_sectionReturnsNull_throwsNPE() {
        Message msg = new MessageSectionReturnsNull();

        assertThrows(NullPointerException.class, msg::getOPT,
                "Expected NullPointerException when getSection(ADDITIONAL) returns null");
    }

    /* --------------------------------------------------------------
     *  getSection throws unchecked exception → propagates
     * -------------------------------------------------------------- */

    @Test
    void getOPT_sectionThrowsIllegalStateException_propagates() {
        Message msg = new MessageSectionThrowsException();

        assertThrows(IllegalStateException.class, msg::getOPT,
                "Expected IllegalStateException to propagate from overridden getSection()");
    }

    /* --------------------------------------------------------------
     *  Iterator throws ConcurrentModificationException
     * -------------------------------------------------------------- */

    @Test
    void getOPT_iteratorThrowsConcurrentModificationException_propagates() {
        Message msg = new MessageWithBadIterator();

        assertThrows(ConcurrentModificationException.class, msg::getOPT,
                "Expected ConcurrentModificationException to propagate from the iterator");
    }

    /* --------------------------------------------------------------
     *  Helper subclasses to simulate edge‑cases
     * -------------------------------------------------------------- */

    /**
     * Subclass that returns a custom list for the ADDITIONAL section.
     */
    private static class MessageWithCustomSection extends Message {
        private final List<Record> customSection;

        MessageWithCustomSection(List<Record> customSection) {
            this.customSection = customSection;
        }

        @Override
        public List<Record> getSection(int section) {
            if (section == Section.ADDITIONAL) {
                return customSection;
            }
            return super.getSection(section);
        }
    }

    /**
     * Subclass that returns {@code null} for the ADDITIONAL section.
     */
    private static class MessageSectionReturnsNull extends Message {
        @Override
        public List<Record> getSection(int section) {
            if (section == Section.ADDITIONAL) {
                return null;
            }
            return super.getSection(section);
        }
    }

    /**
     * Subclass that throws an unchecked exception when the ADDITIONAL
     * section is requested.
     */
    private static class MessageSectionThrowsException extends Message {
        @Override
        public List<Record> getSection(int section) {
            if (section == Section.ADDITIONAL) {
                throw new IllegalStateException("simulated failure");
            }
            return super.getSection(section);
        }
    }

    /**
     * Subclass that returns a List whose iterator throws
     * {@link ConcurrentModificationException} on the first call to {@code hasNext()}.
     */
    private static class MessageWithBadIterator extends Message {
        @Override
        public List<Record> getSection(int section) {
            if (section == Section.ADDITIONAL) {
                return new BadIteratorList<>();
            }
            return super.getSection(section);
        }

        /**
         * Minimal List implementation that only provides an iterator which
         * throws {@link ConcurrentModificationException} immediately.
         */
        private static class BadIteratorList<E> implements List<E> {
            @Override
            public Iterator<E> iterator() {
                return new Iterator<E>() {
                    @Override
                    public boolean hasNext() {
                        throw new ConcurrentModificationException("simulated concurrent modification");
                    }

                    @Override
                    public E next() {
                        throw new ConcurrentModificationException("simulated concurrent modification");
                    }
                };
            }

            // All other List methods are irrelevant for this test and can
            // delegate to an empty ArrayList to satisfy the interface contract.
            private final List<E> delegate = Collections.emptyList();

            @Override public int size() { return delegate.size(); }
            @Override public boolean isEmpty() { return delegate.isEmpty(); }
            @Override public boolean contains(Object o) { return delegate.contains(o); }
            @Override public Object[] toArray() { return delegate.toArray(); }
            @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
            @Override public boolean add(E e) { return delegate.add(e); }
            @Override public boolean remove(Object o) { return delegate.remove(o); }
            @Override public boolean containsAll(java.util.Collection<?> c) { return delegate.containsAll(c); }
            @Override public boolean addAll(java.util.Collection<? extends E> c) { return delegate.addAll(c); }
            @Override public boolean addAll(int index, java.util.Collection<? extends E> c) { return delegate.addAll(index, c); }
            @Override public boolean removeAll(java.util.Collection<?> c) { return delegate.removeAll(c); }
            @Override public boolean retainAll(java.util.Collection<?> c) { return delegate.retainAll(c); }
            @Override public void clear() { delegate.clear(); }
            @Override public E get(int index) { return delegate.get(index); }
            @Override public E set(int index, E element) { return delegate.set(index, element); }
            @Override public void add(int index, E element) { delegate.add(index, element); }
            @Override public E remove(int index) { return delegate.remove(index); }
            @Override public int indexOf(Object o) { return delegate.indexOf(o); }
            @Override public int lastIndexOf(Object o) { return delegate.lastIndexOf(o); }
            @Override public java.util.ListIterator<E> listIterator() { return delegate.listIterator(); }
            @Override public java.util.ListIterator<E> listIterator(int index) { return delegate.listIterator(index); }
            @Override public List<E> subList(int fromIndex, int toIndex) { return delegate.subList(fromIndex, toIndex); }
            @Override public boolean equals(Object o) { return delegate.equals(o); }
            @Override public int hashCode() { return delegate.hashCode(); }
        }
    }
}
```