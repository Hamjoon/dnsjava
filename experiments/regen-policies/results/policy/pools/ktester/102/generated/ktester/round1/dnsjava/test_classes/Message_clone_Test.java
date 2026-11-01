package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class Message_clone_Test {

    /**
     * Shared objects that are reused by several tests.
     */
    private static Name qname;

    private static Record question;

    private static Record answerRecord;

    private static TSIG tsigKey;

    class BadCloneMessage extends Message {

        // Override to call Object.clone() directly, which will fail because this class
        // does not implement Cloneable (Message does, but we deliberately bypass it).
        @Override
        public Message clone() {
            try {
                // Force a CloneNotSupportedException via reflection on Object.clone()
                // this still uses Message.clone()
                return (Message) super.clone();
            } catch (CloneNotSupportedException e) {
                // Re‑throw to satisfy the test expectation
                throw new RuntimeException(e);
            }
        }
    }

    class BadCloneMessage extends Message {

        // Override to call Object.clone() directly, which will fail because this class
        // does not implement Cloneable (Message does, but we deliberately bypass it).
        @Override
        public Message clone() {
            try {
                // Force a CloneNotSupportedException via reflection on Object.clone()
                // this still uses Message.clone()
                return (Message) super.clone();
            } catch (CloneNotSupportedException e) {
                // Re‑throw to satisfy the test expectation
                throw new RuntimeException(e);
            }
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        qname = Name.fromConstantString("www.example.");
        question = Record.newRecord(qname, Type.A, DClass.IN);
        answerRecord = new ARecord(qname, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        // A simple TSIG key (algorithm HMAC-MD5, key "test")
        tsigKey = new TSIG("test.", "hmac-md5.", "test".getBytes());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = Message.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field f = Message.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Verify that {@code clone()} returns a new Message instance.
     */
    @Test
    void cloneCreatesNewInstance() {
        Message original = new Message();
        Message copy = original.clone();
        assertNotSame(original, copy, "clone should return a different Message object");
    }

    /**
     * Header must be deep‑copied; changes to the original header must not affect the clone.
     */
    @Test
    void cloneHeaderIsDeepCopy() {
        Message original = new Message(0x1234);
        Header originalHeader = original.getHeader();
        // set a flag on the original header
        originalHeader.setFlag(Flags.QR);
        Message copy = original.clone();
        Header copyHeader = copy.getHeader();
        // The IDs must be equal (they are copied)
        assertEquals(originalHeader.getID(), copyHeader.getID());
        // The flag we set on the original must NOT be present on the clone
        assertFalse(copyHeader.getFlag(Flags.QR), "Cloned header must not share flag state with original");
        // Conversely, the original still has the flag
        assertTrue(originalHeader.getFlag(Flags.QR));
    }

    /**
     * Sections must be deep‑copied; modifications to the original after cloning must not affect the copy.
     */
    @Test
    void cloneSectionsAreDeepCopy() {
        Message original = new Message();
        // Populate QUESTION and ANSWER sections
        original.addRecord(question, Section.QUESTION);
        original.addRecord(answerRecord, Section.ANSWER);
        Message copy = original.clone();
        // Verify that the sections contain the same records initially
        assertEquals(original.getSection(Section.QUESTION), copy.getSection(Section.QUESTION));
        assertEquals(original.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));
        // Mutate original: add another record to ANSWER and remove the question
        Record extra = new ARecord(qname, DClass.IN, 0, InetAddress.getLoopbackAddress());
        original.addRecord(extra, Section.ANSWER);
        original.removeRecord(question, Section.QUESTION);
        // The clone must remain unchanged
        List<Record> copyAnswer = copy.getSection(Section.ANSWER);
        assertEquals(1, copyAnswer.size(), "Clone's ANSWER section size must stay unchanged");
        assertEquals(answerRecord, copyAnswer.get(0));
        List<Record> copyQuestion = copy.getSection(Section.QUESTION);
        assertEquals(1, copyQuestion.size(), "Clone's QUESTION section size must stay unchanged");
        assertEquals(question, copyQuestion.get(0));
    }

    /**
     * When a TSIG key is set on the original message, the clone must contain a distinct TSIGRecord.
     */
    @Test
    void cloneCopiesTsigRecords() throws IOException {
        Message original = new Message();
        // Attach a TSIG to the message (this creates a generated TSIGRecord internally)
        original.setTSIG(tsigKey);
        // Also set a query‑time TSIGRecord via the overloaded method
        original.setTSIG(tsigKey, Rcode.NOERROR, null);
        // Ensure the original actually has TSIG records
        assertNotNull(original.getGeneratedTSIG(), "Original should have a generated TSIG record");
        assertNotNull(original.getTSIG(), "Original should have a query TSIG record");
        Message copy = original.clone();
        // The clone must have its own TSIG objects (different instances)
        TSIGRecord originalGen = original.getGeneratedTSIG();
        TSIGRecord copyGen = copy.getGeneratedTSIG();
        assertNotNull(copyGen);
        assertNotSame(originalGen, copyGen, "Generated TSIGRecord must be cloned, not shared");
        assertEquals(originalGen, copyGen, "Cloned generated TSIGRecord must be equal to original");
        TSIGRecord originalQuery = original.getTSIG();
        TSIGRecord copyQuery = copy.getTSIG();
        assertNotNull(copyQuery);
        assertNotSame(originalQuery, copyQuery, "Query TSIGRecord must be cloned, not shared");
        assertEquals(originalQuery, copyQuery, "Cloned query TSIGRecord must be equal to original");
    }

    /**
     * Cloning a message that has no records or TSIG must still succeed and preserve empty sections.
     */
    @Test
    void cloneEmptyMessage() {
        Message original = new Message();
        Message copy = original.clone();
        // All sections should be empty but non‑null
        for (int i = 0; i < 4; i++) {
            assertTrue(copy.getSection(i).isEmpty(), "Section " + i + " should be empty after cloning");
        }
        // No TSIG records should be present
        assertNull(copy.getGeneratedTSIG());
        assertNull(copy.getTSIG());
    }

    @Test
    void clone_fullObjectAllBranches() throws Exception {
        // Build a message with all sections non‑null and containing at least one record
        Message original = new Message();
        original.addRecord(Record.newRecord(Name.fromConstantString("a.example."), Type.A, DClass.IN), Section.QUESTION);
        original.addRecord(Record.newRecord(Name.fromConstantString("b.example."), Type.A, DClass.IN), Section.ANSWER);
        original.addRecord(Record.newRecord(Name.fromConstantString("c.example."), Type.A, DClass.IN), Section.AUTHORITY);
        original.addRecord(Record.newRecord(Name.fromConstantString("d.example."), Type.A, DClass.IN), Section.ADDITIONAL);
        // Attach both TSIG records
        TSIG tsig = new TSIG("key.", "hmac-md5.", "secret".getBytes());
        original.setTSIG(tsig);
        // creates querytsig
        original.setTSIG(tsig, Rcode.NOERROR, null);
        // Verify pre‑conditions
        assertNotNull(original.getGeneratedTSIG());
        assertNotNull(original.getTSIG());
        Message copy = original.clone();
        // Distinct object
        assertNotSame(original, copy);
        // Header deep copy
        assertNotSame(original.getHeader(), copy.getHeader());
        assertEquals(original.getHeader(), copy.getHeader());
        // Sections deep copy
        for (int i = 0; i < 4; i++) {
            List<Record> origSec = original.getSection(i);
            List<Record> copySec = copy.getSection(i);
            assertNotSame(origSec, copySec, "Section " + i + " list must be a different instance");
            assertEquals(origSec, copySec, "Section " + i + " content must be equal");
        }
        // TSIG deep copy
        assertNotSame(original.getGeneratedTSIG(), copy.getGeneratedTSIG());
        assertEquals(original.getGeneratedTSIG(), copy.getGeneratedTSIG());
        assertNotSame(original.getTSIG(), copy.getTSIG());
        assertEquals(original.getTSIG(), copy.getTSIG());
    }

    @Test
    void clone_someSectionsNull_TsigNull() throws Exception {
        Message original = new Message();
        // Manually craft sections array: [null, non‑null, null, non‑null]
        @SuppressWarnings("unchecked")
        List<Record>[] customSections = (List<Record>[]) new List<?>[4];
        customSections[1] = new LinkedList<>(Collections.singletonList(Record.newRecord(Name.fromConstantString("x.example."), Type.A, DClass.IN)));
        customSections[3] = new LinkedList<>(Collections.singletonList(Record.newRecord(Name.fromConstantString("y.example."), Type.A, DClass.IN)));
        setField(original, "sections", customSections);
        // Ensure TSIG fields are null
        setField(original, "querytsig", null);
        setField(original, "generatedTsig", null);
        Message copy = original.clone();
        // Verify section copying semantics
        for (int i = 0; i < 4; i++) {
            List<Record> orig = original.getSection(i);
            List<Record> cp = copy.getSection(i);
            if (customSections[i] == null) {
                assertNull(cp, "Section " + i + " should be null in the clone");
            } else {
                assertNotSame(orig, cp);
                assertEquals(orig, cp);
            }
        }
        // TSIG must stay null
        assertNull(copy.getGeneratedTSIG());
        assertNull(copy.getTSIG());
    }

    @Test
    void clone_queryTsigOnly() throws Exception {
        Message original = new Message();
        // Mock a TSIGRecord that clones correctly
        TSIGRecord mockQuery = Mockito.mock(TSIGRecord.class);
        Mockito.when(mockQuery.cloneRecord()).thenReturn(mockQuery);
        setField(original, "querytsig", mockQuery);
        setField(original, "generatedTsig", null);
        Message copy = original.clone();
        assertNotNull(copy.getTSIG());
        // just sanity
        assertSame(copy.getTSIG(), copy.getGeneratedTSIG() == null ? copy.getTSIG() : null);
        assertNotSame(mockQuery, copy.getTSIG());
        assertEquals(mockQuery, copy.getTSIG());
        assertNull(copy.getGeneratedTSIG());
    }

    @Test
    void clone_generatedTsigOnly() throws Exception {
        Message original = new Message();
        TSIGRecord mockGen = Mockito.mock(TSIGRecord.class);
        Mockito.when(mockGen.cloneRecord()).thenReturn(mockGen);
        setField(original, "generatedTsig", mockGen);
        setField(original, "querytsig", null);
        Message copy = original.clone();
        assertNotNull(copy.getGeneratedTSIG());
        assertNotSame(mockGen, copy.getGeneratedTSIG());
        assertEquals(mockGen, copy.getGeneratedTSIG());
        assertNull(copy.getTSIG());
    }

    @Test
    void clone_bothTsigNull() throws Exception {
        Message original = new Message();
        setField(original, "querytsig", null);
        setField(original, "generatedTsig", null);
        Message copy = original.clone();
        assertNull(copy.getTSIG());
        assertNull(copy.getGeneratedTSIG());
    }

    @Test
    void clone_headerNull_throwsNPE() throws Exception {
        Message original = new Message();
        setField(original, "header", null);
        assertThrows(NullPointerException.class, original::clone, "Cloning with a null header should throw NullPointerException");
    }

    @Test
    void clone_emptySectionsArray() throws Exception {
        Message original = new Message();
        // sections length 0
        @SuppressWarnings("unchecked")
        List<Record>[] empty = (List<Record>[]) Array.newInstance(List.class, 0);
        setField(original, "sections", empty);
        Message copy = original.clone();
        // Verify length is still 0 and no exception occurred
        assertEquals(0, copy.getSection(0).size(), "Section 0 should be empty (array length 0)");
        // Header must still be cloned
        assertNotSame(original.getHeader(), copy.getHeader());
    }

    @Test
    void clone_nullSectionsArray_throwsNPE() throws Exception {
        Message original = new Message();
        setField(original, "sections", null);
        assertThrows(NullPointerException.class, original::clone, "Cloning with a null sections array should throw NullPointerException");
    }

    @Test
    void clone_sectionIndependence_modifyClone() throws Exception {
        Message original = new Message();
        Record r1 = Record.newRecord(Name.fromConstantString("a.example."), Type.A, DClass.IN);
        original.addRecord(r1, Section.QUESTION);
        Message copy = original.clone();
        // Add a new record only to the clone's QUESTION section
        Record r2 = Record.newRecord(Name.fromConstantString("b.example."), Type.A, DClass.IN);
        copy.addRecord(r2, Section.QUESTION);
        // Original must remain unchanged
        List<Record> origSec = original.getSection(Section.QUESTION);
        assertEquals(1, origSec.size());
        assertEquals(r1, origSec.get(0));
        // Clone must have both records
        List<Record> cloneSec = copy.getSection(Section.QUESTION);
        assertEquals(2, cloneSec.size());
        assertEquals(r1, cloneSec.get(0));
        assertEquals(r2, cloneSec.get(1));
    }

    @Test
    void clone_emptyNonNullLists() throws Exception {
        Message original = new Message();
        @SuppressWarnings("unchecked")
        List<Record>[] custom = (List<Record>[]) new List<?>[4];
        for (int i = 0; i < 4; i++) {
            // non‑null empty list
            custom[i] = new LinkedList<>();
        }
        setField(original, "sections", custom);
        Message copy = original.clone();
        for (int i = 0; i < 4; i++) {
            List<Record> origList = original.getSection(i);
            List<Record> copyList = copy.getSection(i);
            assertNotSame(origList, copyList, "Section " + i + " list must be a different instance");
            assertTrue(copyList.isEmpty(), "Cloned list for section " + i + " should be empty");
        }
    }

    @Test
    void clone_preservesRecordOrder() throws Exception {
        Message original = new Message();
        Record a = Record.newRecord(Name.fromConstantString("a.example."), Type.A, DClass.IN);
        Record b = Record.newRecord(Name.fromConstantString("b.example."), Type.A, DClass.IN);
        Record c = Record.newRecord(Name.fromConstantString("c.example."), Type.A, DClass.IN);
        original.addRecord(a, Section.ANSWER);
        original.addRecord(b, Section.ANSWER);
        original.addRecord(c, Section.ANSWER);
        Message copy = original.clone();
        List<Record> origList = original.getSection(Section.ANSWER);
        List<Record> copyList = copy.getSection(Section.ANSWER);
        assertNotSame(origList, copyList);
        assertEquals(3, copyList.size());
        assertEquals(a, copyList.get(0));
        assertEquals(b, copyList.get(1));
        assertEquals(c, copyList.get(2));
    }

    @Test
    void clone_queryTsigCloneRecordThrowsIOException() throws Exception {
        Message original = new Message();
        TSIGRecord bad = Mockito.mock(TSIGRecord.class);
        Mockito.when(bad.cloneRecord()).thenThrow(new IOException("forced"));
        setField(original, "querytsig", bad);
        setField(original, "generatedTsig", null);
        IOException ex = assertThrows(IOException.class, original::clone, "IOException from querytsig.cloneRecord() must propagate");
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void clone_queryTsigCloneRecordThrowsRuntimeException() throws Exception {
        Message original = new Message();
        TSIGRecord bad = Mockito.mock(TSIGRecord.class);
        Mockito.when(bad.cloneRecord()).thenThrow(new IllegalStateException("runtime"));
        setField(original, "querytsig", bad);
        setField(original, "generatedTsig", null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, original::clone, "Unchecked exception from querytsig.cloneRecord() must propagate");
        assertEquals("runtime", ex.getMessage());
    }

    @Test
    void clone_generatedTsigCloneRecordThrowsIOException() throws Exception {
        Message original = new Message();
        TSIGRecord bad = Mockito.mock(TSIGRecord.class);
        Mockito.when(bad.cloneRecord()).thenThrow(new IOException("gen forced"));
        setField(original, "generatedTsig", bad);
        setField(original, "querytsig", null);
        IOException ex = assertThrows(IOException.class, original::clone);
        assertEquals("gen forced", ex.getMessage());
    }

    @Test
    void clone_generatedTsigCloneRecordThrowsNPE() throws Exception {
        Message original = new Message();
        TSIGRecord bad = Mockito.mock(TSIGRecord.class);
        Mockito.when(bad.cloneRecord()).thenThrow(new NullPointerException("null ptr"));
        setField(original, "generatedTsig", bad);
        setField(original, "querytsig", null);
        NullPointerException ex = assertThrows(NullPointerException.class, original::clone);
        assertEquals("null ptr", ex.getMessage());
    }

    @Test
    void clone_bothTsig_queryThrowsIOException() throws Exception {
        Message original = new Message();
        TSIGRecord queryBad = Mockito.mock(TSIGRecord.class);
        TSIGRecord genGood = Mockito.mock(TSIGRecord.class);
        Mockito.when(queryBad.cloneRecord()).thenThrow(new IOException("query fail"));
        Mockito.when(genGood.cloneRecord()).thenReturn(genGood);
        setField(original, "querytsig", queryBad);
        setField(original, "generatedTsig", genGood);
        IOException ex = assertThrows(IOException.class, original::clone);
        assertEquals("query fail", ex.getMessage());
    }

    @Test
    void clone_bothTsig_generatedThrowsRuntime() throws Exception {
        Message original = new Message();
        TSIGRecord queryGood = Mockito.mock(TSIGRecord.class);
        TSIGRecord genBad = Mockito.mock(TSIGRecord.class);
        Mockito.when(queryGood.cloneRecord()).thenReturn(queryGood);
        Mockito.when(genBad.cloneRecord()).thenThrow(new IllegalArgumentException("gen bad"));
        setField(original, "querytsig", queryGood);
        setField(original, "generatedTsig", genBad);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, original::clone);
        assertEquals("gen bad", ex.getMessage());
    }

    @Test
    void clone_sectionListCopyThrowsException() throws Exception {
        Message original = new Message();
        // Custom list that throws on iterator()
        List<Record> throwingList = new List<Record>() {

            @Override
            public int size() {
                return 0;
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
                throw new UnsupportedOperationException("iter");
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
            public boolean add(Record record) {
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
        List<Record>[] custom = (List<Record>[]) new List<?>[4];
        custom[0] = throwingList;
        setField(original, "sections", custom);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, original::clone);
        assertEquals("iter", ex.getMessage());
    }

    @Test
    void clone_subclassCloneNotSupportedException() {
        class BadCloneMessage extends Message {

            // Override to call Object.clone() directly, which will fail because this class
            // does not implement Cloneable (Message does, but we deliberately bypass it).
            @Override
            public Message clone() {
                try {
                    // Force a CloneNotSupportedException via reflection on Object.clone()
                    // this still uses Message.clone()
                    return (Message) super.clone();
                } catch (CloneNotSupportedException e) {
                    // Re‑throw to satisfy the test expectation
                    throw new RuntimeException(e);
                }
            }
        }
        BadCloneMessage msg = new BadCloneMessage();
        // Because BadCloneMessage does not add any special state, we expect the
        // overridden clone() to eventually throw CloneNotSupportedException wrapped
        // in RuntimeException (due to the catch above). The test verifies that the
        // exception propagates.
        RuntimeException ex = assertThrows(RuntimeException.class, msg::clone);
        assertTrue(ex.getCause() instanceof CloneNotSupportedException, "Cause should be CloneNotSupportedException");
    }
}
