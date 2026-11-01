package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class Message_toWire_Test {

    private Message message;

    private Header mockHeader;

    private DNSOutput mockOut;

    @BeforeEach
    void setUp() throws Exception {
        // fresh instance per test
        message = new Message();
        mockHeader = mock(Header.class);
        mockOut = mock(DNSOutput.class);
        // inject the mocked header
        var headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(message, mockHeader);
    }

    /**
     * Helper to inject a custom {@code sections} array into the Message instance.
     */
    private void setSections(List<Record>[] sections) throws Exception {
        var sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, sections);
    }

    /**
     * An empty {@link Message} should serialize to the same byte array whether we
     * use the public {@code toWire()} method or invoke the package‑private
     * {@code toWire(DNSOutput)} directly.
     */
    @Test
    void toWire_emptyMessage_matchesPublicVersion() {
        // default constructor → empty sections
        Message message = new Message();
        // default size is sufficient for an empty message
        DNSOutput out = new DNSOutput();
        // invoke the package‑private method
        message.toWire(out);
        byte[] viaOutput = out.toByteArray();
        // invoke the public convenience method
        byte[] viaPublic = message.toWire();
        assertArrayEquals(viaPublic, viaOutput, "Serialization of an empty Message should be identical");
    }

    /**
     * A message that contains a question and an answer record should also be
     * serialized identically by both pathways.
     */
    @Test
    void toWire_messageWithRecords_matchesPublicVersion() throws UnknownHostException {
        // Build a simple query message (question section)
        Name name = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message message = Message.newQuery(question);
        // Add an answer record
        ARecord answer = new ARecord(name, DClass.IN, 300, InetAddress.getByName("127.0.0.1"));
        message.addRecord(answer, Section.ANSWER);
        // Serialize using the package‑private method
        DNSOutput out = new DNSOutput();
        message.toWire(out);
        byte[] viaOutput = out.toByteArray();
        // Serialize using the public method
        byte[] viaPublic = message.toWire();
        assertArrayEquals(viaPublic, viaOutput, "Serialization with question and answer records should be identical");
    }

    /**
     * Verify that the byte array produced by {@code toWire(DNSOutput)} has the
     * expected length (i.e., does not exceed the DNS maximum length) for a
     * moderately sized message.
     */
    @Test
    void toWire_messageLengthWithinMaximum() throws IOException {
        // Create a message with several additional records to increase size
        Message message = new Message();
        Name name = Name.fromConstantString("test.example.");
        // add a question
        Record q = Record.newRecord(name, Type.MX, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        // add a handful of TXT records in the additional section
        for (int i = 0; i < 5; i++) {
            TXTRecord txt = new TXTRecord(name, DClass.IN, 0, "txt-" + i);
            message.addRecord(txt, Section.ADDITIONAL);
        }
        DNSOutput out = new DNSOutput();
        message.toWire(out);
        byte[] data = out.toByteArray();
        // The DNS protocol limits a message to 65535 bytes
        assertTrue(data.length <= Message.MAXLENGTH, "Serialized message must not exceed the DNS maximum length");
    }

    @Test
    void toWire_allSectionsNull_callsHeaderOnly() throws Exception {
        // sections array with all entries null
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        setSections(secs);
        // invoke
        message.toWire(mockOut);
        // verify interactions
        verify(mockHeader, times(1)).toWire(mockOut);
        verifyNoMoreInteractions(mockHeader);
        // no Record should have been touched
        verifyNoInteractions(mockOut);
    }

    @Test
    void toWire_singleRecordInSection0() throws Exception {
        Record rec = mock(Record.class);
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        secs[0] = Collections.singletonList(rec);
        setSections(secs);
        message.toWire(mockOut);
        verify(mockHeader).toWire(mockOut);
        verify(rec).toWire(mockOut, 0, any(Compression.class));
        verifyNoMoreInteractions(rec);
    }

    @Test
    void toWire_mixedNullAndNonNullSections() throws Exception {
        Record r0 = mock(Record.class);
        Record r2a = mock(Record.class);
        Record r2b = mock(Record.class);
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        // section 0
        secs[0] = Collections.singletonList(r0);
        // section 1 null
        secs[1] = null;
        List<Record> sec2 = new ArrayList<>();
        sec2.add(r2a);
        sec2.add(r2b);
        // section 2 with two records
        secs[2] = sec2;
        setSections(secs);
        message.toWire(mockOut);
        verify(mockHeader).toWire(mockOut);
        verify(r0).toWire(mockOut, 0, any(Compression.class));
        verify(r2a).toWire(mockOut, 2, any(Compression.class));
        verify(r2b).toWire(mockOut, 2, any(Compression.class));
    }

    @Test
    void toWire_nonNullEmptyList() throws Exception {
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        // non‑null but empty
        secs[1] = Collections.emptyList();
        setSections(secs);
        message.toWire(mockOut);
        verify(mockHeader).toWire(mockOut);
        // No Record interaction expected
        verifyNoMoreInteractions(mockHeader);
    }

    @Test
    void toWire_emptySectionsArray() throws Exception {
        @SuppressWarnings("unchecked")
        List<Record>[] // length 0
        secs = new List[0];
        setSections(secs);
        message.toWire(mockOut);
        verify(mockHeader).toWire(mockOut);
        verifyNoMoreInteractions(mockHeader);
    }

    @Test
    void toWire_headerInvokedBeforeRecords() throws Exception {
        Record rec = mock(Record.class);
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        secs[1] = Collections.singletonList(rec);
        setSections(secs);
        message.toWire(mockOut);
        InOrder inOrder = inOrder(mockHeader, rec);
        inOrder.verify(mockHeader).toWire(mockOut);
        inOrder.verify(rec).toWire(mockOut, 1, any(Compression.class));
    }

    @Test
    void toWire_sameCompressionInstanceReused() throws Exception {
        Record r0 = mock(Record.class);
        Record r2 = mock(Record.class);
        Record r3 = mock(Record.class);
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        secs[0] = Collections.singletonList(r0);
        secs[2] = Collections.singletonList(r2);
        secs[3] = Collections.singletonList(r3);
        setSections(secs);
        // capture the Compression argument passed to each Record
        ArgumentCaptor<Compression> compCaptor = ArgumentCaptor.forClass(Compression.class);
        message.toWire(mockOut);
        verify(r0).toWire(eq(mockOut), eq(0), compCaptor.capture());
        verify(r2).toWire(eq(mockOut), eq(2), compCaptor.capture());
        verify(r3).toWire(eq(mockOut), eq(3), compCaptor.capture());
        List<Compression> captured = compCaptor.getAllValues();
        assertEquals(3, captured.size(), "Three records should have received a Compression");
        // All captured references must be the same object
        Compression first = captured.get(0);
        assertTrue(captured.stream().allMatch(c -> c == first), "The same Compression instance must be reused for all Record.toWire calls");
    }

    @Test
    void toWire_recordThrowsIOException_propagates() throws Exception {
        Record rec = mock(Record.class);
        doThrow(new IOException("simulated")).when(rec).toWire(any(), anyInt(), any());
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        secs[0] = Collections.singletonList(rec);
        setSections(secs);
        Executable call = () -> message.toWire(mockOut);
        IOException ex = assertThrows(IOException.class, call);
        assertEquals("simulated", ex.getMessage());
    }

    @Test
    void toWire_nullSectionsArray_throwsNPE() throws Exception {
        // force sections field to null
        var sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);
        Executable call = () -> message.toWire(mockOut);
        assertThrows(NullPointerException.class, call);
    }

    @Test
    void toWire_nullHeader_throwsNPE() throws Exception {
        // set header to null
        var headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(message, null);
        // sections can be empty – they are irrelevant for this NPE
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        setSections(secs);
        Executable call = () -> message.toWire(mockOut);
        assertThrows(NullPointerException.class, call);
    }

    @Test
    void toWire_listContainsNullRecord_throwsNPE() throws Exception {
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        List<Record> list = new ArrayList<>();
        // null element inside the list
        list.add(null);
        secs[0] = list;
        setSections(secs);
        Executable call = () -> message.toWire(mockOut);
        assertThrows(NullPointerException.class, call);
    }

    @Test
    void toWire_nullOutParameter_throwsNPE() {
        Executable call = () -> message.toWire(null);
        assertThrows(NullPointerException.class, call);
    }

    @Test
    void toWire_headerThrowsIOException_propagates() throws Exception {
        doThrow(new IOException("header failure")).when(mockHeader).toWire(any());
        Executable call = () -> message.toWire(mockOut);
        IOException ex = assertThrows(IOException.class, call);
        assertEquals("header failure", ex.getMessage());
    }

    @Test
    void toWire_headerThrowsRuntimeException_propagates() throws Exception {
        RuntimeException rte = new RuntimeException("runtime header error");
        doThrow(rte).when(mockHeader).toWire(any());
        Executable call = () -> message.toWire(mockOut);
        RuntimeException thrown = assertThrows(RuntimeException.class, call);
        assertSame(rte, thrown);
    }

    @Test
    void toWire_recordThrowsRuntimeException_propagates() throws Exception {
        Record rec = mock(Record.class);
        RuntimeException rte = new RuntimeException("record runtime error");
        doThrow(rte).when(rec).toWire(any(), anyInt(), any());
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        secs[2] = Collections.singletonList(rec);
        setSections(secs);
        Executable call = () -> message.toWire(mockOut);
        RuntimeException thrown = assertThrows(RuntimeException.class, call);
        assertSame(rte, thrown);
    }

    @Test
    void toWire_sectionListIteratorThrowsRuntimeException_propagates() throws Exception {
        // custom List whose iterator throws RuntimeException on next()
        List<Record> badList = new List<>() {

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
                return new Iterator<>() {

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Record next() {
                        throw new RuntimeException("iterator failure");
                    }
                };
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return a;
            }

            @Override
            public boolean add(Record e) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(java.util.Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(java.util.Collection<? extends Record> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, java.util.Collection<? extends Record> c) {
                return false;
            }

            @Override
            public boolean removeAll(java.util.Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(java.util.Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
            }

            @Override
            public Record get(int index) {
                return null;
            }

            @Override
            public Record set(int index, Record element) {
                return null;
            }

            @Override
            public void add(int index, Record element) {
            }

            @Override
            public Record remove(int index) {
                return null;
            }

            @Override
            public int indexOf(Object o) {
                return -1;
            }

            @Override
            public int lastIndexOf(Object o) {
                return -1;
            }

            @Override
            public ListIterator<Record> listIterator() {
                return null;
            }

            @Override
            public ListIterator<Record> listIterator(int index) {
                return null;
            }

            @Override
            public List<Record> subList(int fromIndex, int toIndex) {
                return null;
            }
        };
        @SuppressWarnings("unchecked")
        List<Record>[] secs = new List[4];
        secs[1] = badList;
        setSections(secs);
        Executable call = () -> message.toWire(mockOut);
        RuntimeException thrown = assertThrows(RuntimeException.class, call);
        assertEquals("iterator failure", thrown.getMessage());
    }
}
