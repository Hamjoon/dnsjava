```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional unit tests for {@link Message#clone()} that complement the
 * existing {@link Message_clone_Test} suite.
 *
 * <p>These tests focus on branch coverage, deep‑copy independence of internal
 * mutable fields, primitive field copying, and the behaviour when internal
 * fields are {@code null}.</p>
 */
@ExtendWith(MockitoExtension.class)
class MessageCloneAdditionalTest {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
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

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                    */
    /* --------------------------------------------------------------------- */

    /**
     * Uses reflection to set a private field on the given object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value)
            throws Exception {
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

    /* --------------------------------------------------------------------- */
    /* Branch‑coverage tests                                                */
    /* --------------------------------------------------------------------- */

    @Test
    void cloneWhenSectionsEmptyAndNoTSIG() throws Exception {
        // Message with an empty sections array (length 0) and no TSIG records.
        Message original = new Message();
        setPrivateField(original, "sections", new List[0]); // empty array
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
        sections[0] = null; // null entry
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
        original.setTSIG(tsigKey, Rcode.NOERROR, null); // creates querytsig

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

    /* --------------------------------------------------------------------- */
    /* Deep‑copy independence tests                                         */
    /* --------------------------------------------------------------------- */

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
            assertNotSame(sections[i], clonedSections[i],
                    "Section list " + i + " must be a new instance");
            assertEquals(sections[i], clonedSections[i],
                    "Section list " + i + " must contain the same records");
        }

        // Mutate original's first section and ensure clone is unaffected.
        Record extra = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("9.9.9.9"));
        sections[0].add(extra);
        assertEquals(1, clonedSections[0].size(),
                "Clone's first section must remain unchanged after original mutation");
    }

    @Test
    void cloneEmptyListSectionIsRecreated() throws Exception {
        // One section contains an empty list (not null).
        Message original = new Message();
        List<Record>[] sections = (List<Record>[]) new List[2];
        sections[0] = new LinkedList<>(); // empty list
        sections[1] = null;
        setPrivateField(original, "sections", sections);
        setPrivateField(original, "header", new Header());

        Message clone = original.clone();

        List<Record>[] clonedSections = (List<Record>[]) getPrivateField(clone, "sections");
        assertNotNull(clonedSections[0], "Empty list must be cloned, not become null");
        assertNotSame(sections[0], clonedSections[0], "Empty list instance must be different");
        assertTrue(clonedSections[0].isEmpty(), "Cloned empty list should remain empty");
    }

    /* --------------------------------------------------------------------- */
    /* Primitive fields copy test                                           */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* generatedTsig‑only test                                              */
    /* --------------------------------------------------------------------- */

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
        assertSame(mockGenerated, getPrivateField(clone, "generatedTsig"),
                "generatedTsig must be the cloned instance");
    }

    /* --------------------------------------------------------------------- */
    /* Null‑field exceptional behaviour tests                               */
    /* --------------------------------------------------------------------- */

    @Test
    void cloneThrowsWhenSectionsArrayIsNull() throws Exception {
        Message original = new Message();
        setPrivateField(original, "sections", null); // force NPE scenario
        setPrivateField(original, "header", new Header());

        assertThrows(NullPointerException.class, original::clone,
                "Cloning with a null sections array should throw NPE");
    }

    @Test
    void cloneThrowsWhenHeaderIsNull() throws Exception {
        Message original = new Message();
        // Provide a valid (empty) sections array to avoid earlier NPE.
        setPrivateField(original, "sections", new List[0]);
        setPrivateField(original, "header", null); // force NPE scenario

        assertThrows(NullPointerException.class, original::clone,
                "Cloning with a null header should throw NPE");
    }
}
```