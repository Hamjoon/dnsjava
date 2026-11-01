package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedList;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Message_clone_Test {

    /**
     * Uses reflection to set a private field on the given object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves a private field via reflection.
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Verify that {@code clone()} creates a deep copy of the section lists.
     * Adding or removing records from the original after cloning must not affect the clone.
     */
    @Test
    void cloneCreatesDeepCopyOfSections() throws UnknownHostException {
        // Build a message with records in all four sections.
        Message original = new Message();
        Name name = Name.fromConstantString("example.com.");
        // QUESTION
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        original.addRecord(q, Section.QUESTION);
        // ANSWER
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        original.addRecord(a, Section.ANSWER);
        // AUTHORITY
        Record ns = Record.newRecord(name, Type.NS, DClass.IN);
        original.addRecord(ns, Section.AUTHORITY);
        // ADDITIONAL
        Record txt = new TXTRecord(name, DClass.IN, 3600, "additional");
        original.addRecord(txt, Section.ADDITIONAL);
        // Clone the message.
        Message clone = original.clone();
        // Verify that each section list is equal but not the same object.
        for (int sec = 0; sec < 4; sec++) {
            List<Record> origSec = original.getSection(sec);
            List<Record> cloneSec = clone.getSection(sec);
            assertEquals(origSec, cloneSec, "Section " + sec + " should contain the same records after cloning");
            assertNotSame(origSec, cloneSec, "Section list object should be a different instance after cloning");
        }
        // Mutate the original: add a new record to ANSWER and remove one from QUESTION.
        Record extraA = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("8.8.8.8"));
        original.addRecord(extraA, Section.ANSWER);
        original.removeRecord(q, Section.QUESTION);
        // The clone must stay unchanged.
        assertEquals(1, clone.getSection(Section.QUESTION).size(), "Clone's QUESTION section must remain unchanged");
        assertEquals(1, clone.getSection(Section.ANSWER).size(), "Clone's ANSWER section must remain unchanged");
        assertEquals(a, clone.getSection(Section.ANSWER).get(0), "Clone's ANSWER record must be the original one");
    }

    /**
     * Verify that the {@link Header} of a cloned message is a separate instance.
     * Changing a flag on the original header must not affect the cloned header.
     */
    @Test
    void cloneHeaderIsIndependent() {
        Message original = new Message(0x1234);
        // Set a flag on the original header.
        original.getHeader().setFlag(Flags.QR);
        // Clone the message.
        Message clone = original.clone();
        // The cloned header must have the same flag state initially.
        assertTrue(clone.getHeader().getFlag(Flags.QR));
        // Change the flag on the original header.
        original.getHeader().unsetFlag(Flags.QR);
        // The cloned header must retain the flag.
        assertTrue(clone.getHeader().getFlag(Flags.QR), "Cloned header should retain the flag state independent of the original");
        assertFalse(original.getHeader().getFlag(Flags.QR), "Original header flag should have been cleared");
    }

    /**
     * Verify that cloning a message that contains a TSIG (query TSIG) copies the TSIG record.
     * The cloned TSIG objects must not be the same instance as those in the original.
     */
    @Test
    void cloneCopiesTSIGRecords() throws IOException {
        // Prepare a simple query message.
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        // Create a TSIG key and associate it with the query.
        // The TSIG constructor used here matches the public API of dnsjava.
        TSIG tsigKey = new TSIG("testkey", "hmac-md5", "secret".getBytes());
        // creates a query TSIG record
        query.setTSIG(tsigKey, Rcode.NOERROR, null);
        // Ensure the query has a TSIG attached.
        assertNotNull(query.getTSIG(), "Original message should have a TSIG record");
        // Clone the query.
        Message clone = query.clone();
        // Both messages must have a TSIG, but they must be distinct objects.
        assertNotNull(clone.getTSIG(), "Cloned message should also have a TSIG record");
        assertNotSame(query.getTSIG(), clone.getTSIG(), "Cloned TSIG record must be a different instance from the original");
        // The contents (key name, MAC algorithm) should be equal.
        assertEquals(query.getTSIG().getName(), clone.getTSIG().getName(), "Cloned TSIG should have the same name");
        assertEquals(query.getTSIG().getAlgorithm(), clone.getTSIG().getAlgorithm(), "Cloned TSIG should have the same algorithm");
    }

    /**
     * Verify that a cloned response retains the same question and answer section records.
     */
    @Test
    void clonePreservesQuestionAndAnswer() throws UnknownHostException {
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message response = new Message();
        response.getHeader().setFlag(Flags.QR);
        response.addRecord(question, Section.QUESTION);
        Record answer = new ARecord(qname, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        response.addRecord(answer, Section.ANSWER);
        Message clone = response.clone();
        assertEquals(response.getQuestion(), clone.getQuestion(), "Cloned message must have the same question record");
        assertEquals(response.getSection(Section.ANSWER), clone.getSection(Section.ANSWER), "Cloned message must have the same answer section");
    }

    @Test
    void cloneWhenSectionsEmptyAndNoTSIG() throws Exception {
        // Message with an empty sections array (length 0) and no TSIG records.
        Message original = new Message();
        // empty array
        setPrivateField(original, "sections", new List[0]);
        // Header must be non‑null for the clone to succeed.
        setPrivateField(original, "header", new Header());
        Message clone = original.clone();
        // sections array length must be 0
        List<?>[] clonedSections = (List<?>[]) getPrivateField(clone, "sections");
        assertEquals(0, clonedSections.length, "Cloned sections array should be length 0");
        // header must be a distinct instance
        Header originalHeader = (Header) getPrivateField(original, "header");
        Header clonedHeader = (Header) getPrivateField(clone, "header");
        assertNotSame(originalHeader, clonedHeader, "Header should be cloned");
        // TSIG fields must remain null
        assertNull(getPrivateField(clone, "querytsig"));
        assertNull(getPrivateField(clone, "generatedTsig"));
    }

    @Test
    void cloneWithMixedNullAndNonNullSections() throws Exception {
        // Prepare a message whose sections array has a null entry and a non‑null list.
        Message original = new Message();
        List<Record>[] sections = (List<Record>[]) new List[2];
        // null entry
        sections[0] = null;
        sections[1] = new LinkedList<>();
        Name name = Name.fromConstantString("example.com.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        sections[1].add(a);
        setPrivateField(original, "sections", sections);
        setPrivateField(original, "header", new Header());
        Message clone = original.clone();
        List<Record>[] clonedSections = (List<Record>[]) getPrivateField(clone, "sections");
        assertEquals(2, clonedSections.length);
        assertNull(clonedSections[0], "Index 0 should stay null");
        assertNotNull(clonedSections[1], "Index 1 should be a new list");
        assertNotSame(sections[1], clonedSections[1], "List instance must be different");
        assertEquals(sections[1], clonedSections[1], "List contents must be equal");
    }

    @Test
    void cloneWithNonNullQueryTSIGOnly() throws Exception {
        // Build a message with a query TSIG (querytsig) but no generated TSIG.
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message original = Message.newQuery(question);
        TSIG tsigKey = new TSIG("keyname", "hmac-md5", "secret".getBytes());
        // creates querytsig
        original.setTSIG(tsigKey, Rcode.NOERROR, null);
        // Verify pre‑condition
        assertNotNull(original.getTSIG());
        Message clone = original.clone();
        // Both messages must have a TSIG record.
        TSIGRecord origTsig = original.getTSIG();
        TSIGRecord clonedTsig = clone.getTSIG();
        assertNotNull(clonedTsig);
        assertNotSame(origTsig, clonedTsig, "TSIGRecord must be cloned, not shared");
        assertEquals(origTsig.getName(), clonedTsig.getName());
        assertEquals(origTsig.getAlgorithm(), clonedTsig.getAlgorithm());
        // generatedTsig should stay null
        assertNull(getPrivateField(clone, "generatedTsig"));
    }

    @Test
    void cloneWithBothTSIGRecords() throws Exception {
        // Create a message and manually inject both querytsig and generatedTsig using mocks.
        Message original = new Message();
        setPrivateField(original, "header", new Header());
        // Mock TSIGRecord that returns a clone of itself.
        TSIGRecord mockQuery = mock(TSIGRecord.class);
        TSIGRecord mockGenerated = mock(TSIGRecord.class);
        when(mockQuery.cloneRecord()).thenReturn(mockQuery);
        when(mockGenerated.cloneRecord()).thenReturn(mockGenerated);
        setPrivateField(original, "querytsig", mockQuery);
        setPrivateField(original, "generatedTsig", mockGenerated);
        Message clone = original.clone();
        // Verify that clone() called cloneRecord() on both mocks.
        verify(mockQuery, times(1)).cloneRecord();
        verify(mockGenerated, times(1)).cloneRecord();
        // The cloned fields must be the objects returned by cloneRecord()
        assertSame(mockQuery, getPrivateField(clone, "querytsig"));
        assertSame(mockGenerated, getPrivateField(clone, "generatedTsig"));
    }

    @Test
    void cloneDeepCopyIndependenceMultipleSections() throws Exception {
        // Message with three non‑null sections.
        Message original = new Message();
        List<Record>[] sections = (List<Record>[]) new List[3];
        for (int i = 0; i < 3; i++) {
            sections[i] = new LinkedList<>();
        }
        Name name = Name.fromConstantString("example.com.");
        Record r = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("5.6.7.8"));
        sections[0].add(r);
        setPrivateField(original, "sections", sections);
        setPrivateField(original, "header", new Header());
        Message clone = original.clone();
        // Verify each list is a different instance but equal content.
        List<Record>[] clonedSections = (List<Record>[]) getPrivateField(clone, "sections");
        for (int i = 0; i < 3; i++) {
            assertNotSame(sections[i], clonedSections[i], "Section list " + i + " must be a new instance");
            assertEquals(sections[i], clonedSections[i], "Section list " + i + " must contain the same records");
        }
        // Mutate original's first section and ensure clone is unaffected.
        Record extra = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("9.9.9.9"));
        sections[0].add(extra);
        assertEquals(1, clonedSections[0].size(), "Clone's first section must remain unchanged after original mutation");
    }

    @Test
    void cloneEmptyListSectionIsRecreated() throws Exception {
        // One section contains an empty list (not null).
        Message original = new Message();
        List<Record>[] sections = (List<Record>[]) new List[2];
        // empty list
        sections[0] = new LinkedList<>();
        sections[1] = null;
        setPrivateField(original, "sections", sections);
        setPrivateField(original, "header", new Header());
        Message clone = original.clone();
        List<Record>[] clonedSections = (List<Record>[]) getPrivateField(clone, "sections");
        assertNotNull(clonedSections[0], "Empty list must be cloned, not become null");
        assertNotSame(sections[0], clonedSections[0], "Empty list instance must be different");
        assertTrue(clonedSections[0].isEmpty(), "Cloned empty list should remain empty");
    }

    @Test
    void cloneCopiesPrimitiveFieldsIndependently() throws Exception {
        Message original = new Message();
        // Directly set primitive fields via reflection.
        setPrivateField(original, "tsigstart", 42);
        setPrivateField(original, "tsigState", Message.TSIG_SIGNED);
        setPrivateField(original, "sig0start", 99);
        setPrivateField(original, "header", new Header());
        Message clone = original.clone();
        // Verify values are equal immediately after cloning.
        assertEquals(42, getPrivateField(clone, "tsigstart"));
        assertEquals(Message.TSIG_SIGNED, getPrivateField(clone, "tsigState"));
        assertEquals(99, getPrivateField(clone, "sig0start"));
        // Change original primitives and ensure clone stays unchanged.
        setPrivateField(original, "tsigstart", 0);
        setPrivateField(original, "tsigState", Message.TSIG_UNSIGNED);
        setPrivateField(original, "sig0start", 0);
        assertEquals(42, getPrivateField(clone, "tsigstart"));
        assertEquals(Message.TSIG_SIGNED, getPrivateField(clone, "tsigState"));
        assertEquals(99, getPrivateField(clone, "sig0start"));
    }

    @Test
    void cloneCopiesGeneratedTsigOnly() throws Exception {
        Message original = new Message();
        setPrivateField(original, "header", new Header());
        // Mock a generated TSIGRecord.
        TSIGRecord mockGenerated = mock(TSIGRecord.class);
        when(mockGenerated.cloneRecord()).thenReturn(mockGenerated);
        setPrivateField(original, "generatedTsig", mockGenerated);
        // querytsig stays null.
        Message clone = original.clone();
        verify(mockGenerated, times(1)).cloneRecord();
        assertNull(getPrivateField(clone, "querytsig"));
        assertSame(mockGenerated, getPrivateField(clone, "generatedTsig"), "generatedTsig must be the cloned instance");
    }

    @Test
    void cloneThrowsWhenSectionsArrayIsNull() throws Exception {
        Message original = new Message();
        // force NPE scenario
        setPrivateField(original, "sections", null);
        setPrivateField(original, "header", new Header());
        assertThrows(NullPointerException.class, original::clone, "Cloning with a null sections array should throw NPE");
    }

    @Test
    void cloneThrowsWhenHeaderIsNull() throws Exception {
        Message original = new Message();
        // Provide a valid (empty) sections array to avoid earlier NPE.
        setPrivateField(original, "sections", new List[0]);
        // force NPE scenario
        setPrivateField(original, "header", null);
        assertThrows(NullPointerException.class, original::clone, "Cloning with a null header should throw NPE");
    }
}
