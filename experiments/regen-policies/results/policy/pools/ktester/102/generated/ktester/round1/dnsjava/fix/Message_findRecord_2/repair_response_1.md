```java
package org.xbill.DNS;

import org.junit.*;
import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * JUnit‑4 version of the original test suite.  The original suite used
 * JUnit‑5 (Jupiter) which caused a version‑mismatch error in the execution
 * environment.  All tests have been rewritten with JUnit‑4 annotations while
 * preserving the original intent.
 */
public class Message_findRecord_2_Test {

    private Message message;
    private Record aRecord;
    private Record txtRecord;

    @Before
    public void setup() throws Exception {
        message = new Message();
        Name name = Name.fromConstantString("www.example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
    }

    @After
    public void teardown() {
        message = null;
        aRecord = null;
        txtRecord = null;
    }

    /* --------------------------------------------------------------------- *
     * Helper methods that manipulate the private {@code sections} field of
     * {@link Message} via reflection.  They are identical to the ones used in
     * the original JUnit‑5 version.
     * --------------------------------------------------------------------- */
    private static void clearAllSections(Message msg) throws Exception {
        @SuppressWarnings("unchecked")
        List<Record>[] empty = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        setSectionsArray(msg, empty);
    }

    private static void setSectionList(Message msg, int section, List<Record> list) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        if (sections == null) {
            sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
            f.set(msg, sections);
        }
        sections[section] = list;
    }

    private static void setSectionsArray(Message msg, List<Record>[] newArray) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, newArray);
    }

    /* --------------------------------------------------------------------- *
     * Simple (non‑parameterised) tests that cover the same scenarios as the
     * original suite.
     * --------------------------------------------------------------------- */

    @Test
    public void findRecord_returnsTrueWhenRecordIsInAnswerSection() {
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue("findRecord should return true for a record present in the ANSWER section",
                message.findRecord(aRecord));
    }

    @Test
    public void findRecord_returnsFalseWhenRecordIsNotPresent() {
        assertFalse("findRecord should return false when the record is not present in any section",
                message.findRecord(txtRecord));
    }

    @Test
    public void findRecord_checksAllSections() {
        message.addRecord(aRecord, Section.QUESTION);
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue("findRecord should locate the record regardless of which section it resides in",
                message.findRecord(aRecord));
    }

    @Test
    public void findRecord_returnsFalseWhenRecordExistsInDifferentSection() {
        message.addRecord(txtRecord, Section.ANSWER);
        assertFalse("findRecord should return false when the exact Record instance is not present",
                message.findRecord(aRecord));
    }

    @Test
    public void findRecord_returnsTrueWhenRecordIsFoundInVariousSections() throws Exception {
        // target in ANSWER
        Record target = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
        clearAllSections(message);
        message.addRecord(target, Section.ANSWER);
        assertTrue(message.findRecord(target));

        // target in AUTHORITY (ANSWER null)
        target = Record.newRecord(Name.fromConstantString("example.org."), Type.MX, DClass.IN);
        clearAllSections(message);
        message.addRecord(target, Section.AUTHORITY);
        assertTrue(message.findRecord(target));

        // target in ADDITIONAL (ANSWER & AUTHORITY empty/null)
        target = Record.newRecord(Name.fromConstantString("example.net."), Type.TXT, DClass.IN);
        clearAllSections(message);
        message.addRecord(target, Section.ADDITIONAL);
        assertTrue(message.findRecord(target));

        // target in ADDITIONAL, ANSWER empty list, AUTHORITY non‑null but not containing
        target = Record.newRecord(Name.fromConstantString("example.edu."), Type.AAAA, DClass.IN);
        clearAllSections(message);
        setSectionList(message, Section.ANSWER, Collections.emptyList());
        setSectionList(message, Section.AUTHORITY, Collections.singletonList(
                Record.newRecord(Name.fromConstantString("other.edu."), Type.A, DClass.IN)));
        message.addRecord(target, Section.ADDITIONAL);
        assertTrue(message.findRecord(target));
    }

    @Test
    public void findRecord_returnsFalseWhenRecordIsAbsent() throws Exception {
        // all sections null
        clearAllSections(message);
        Record target = Record.newRecord(Name.fromConstantString("missing.com."), Type.SRV, DClass.IN);
        assertFalse(message.findRecord(target));

        // sections non‑null but without the record
        clearAllSections(message);
        Record unrelated = Record.newRecord(Name.fromConstantString("other.com."), Type.A, DClass.IN);
        message.addRecord(unrelated, Section.ANSWER);
        setSectionList(message, Section.ADDITIONAL, Collections.emptyList());
        target = Record.newRecord(Name.fromConstantString("absent.org."), Type.NS, DClass.IN);
        assertFalse(message.findRecord(target));

        // empty lists and null sections
        clearAllSections(message);
        setSectionList(message, Section.ANSWER, Collections.emptyList());
        setSectionList(message, Section.AUTHORITY, null);
        setSectionList(message, Section.ADDITIONAL, Collections.emptyList());
        target = Record.newRecord(Name.fromConstantString("empty.net."), Type.CNAME, DClass.IN);
        assertFalse(message.findRecord(target));
    }

    @Test
    public void findRecord_earlyExitWhenRecordInMultipleSections() {
        Record target = Record.newRecord(Name.fromConstantString("multi.com."), Type.A, DClass.IN);
        message.addRecord(target, Section.ANSWER);
        message.addRecord(target, Section.AUTHORITY);
        message.addRecord(target, Section.ADDITIONAL);
        assertTrue("The method should stop at the first match (ANSWER) and return true",
                message.findRecord(target));
    }

    @Test
    public void findRecord_returnsFalseWhenArgumentIsNull() throws Exception {
        clearAllSections(message);
        Record dummy = Record.newRecord(Name.fromConstantString("dummy.com."), Type.A, DClass.IN);
        message.addRecord(dummy, Section.ANSWER);
        message.addRecord(dummy, Section.AUTHORITY);
        message.addRecord(dummy, Section.ADDITIONAL);
        assertFalse(message.findRecord((Record) null));
    }

    @Test
    public void findRecord_returnsTrueWhenListContainsNullBeforeTarget() throws Exception {
        Record target = Record.newRecord(Name.fromConstantString("nullentry.com."), Type.TXT, DClass.IN);
        List<Record> list = new ArrayList<>(Arrays.asList(null, target));
        setSectionList(message, Section.ANSWER, list);
        assertTrue(message.findRecord(target));
    }

    @Test
    public void findRecord_returnsFalseWhenOnlyNullInList() throws Exception {
        List<Record> list = new ArrayList<>(Collections.singletonList(null));
        setSectionList(message, Section.ANSWER, list);
        Record target = Record.newRecord(Name.fromConstantString("onlynull.com."), Type.AAAA, DClass.IN);
        assertFalse(message.findRecord(target));
    }

    @Test
    public void findRecord_findsRecordInAdditionalAfterNullAndEmptySections() throws Exception {
        setSectionList(message, Section.AUTHORITY, Collections.emptyList());
        Record target = Record.newRecord(Name.fromConstantString("last.com."), Type.SRV, DClass.IN);
        message.addRecord(target, Section.ADDITIONAL);
        assertTrue(message.findRecord(target));
    }

    @Test
    public void findRecord_throwsNullPointerExceptionWhenSectionsArrayIsNull() throws Exception {
        setSectionsArray(message, null);
        Record target = Record.newRecord(Name.fromConstantString("test.com."), Type.A, DClass.IN);
        try {
            message.findRecord(target);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void findRecord_throwsArrayIndexOutOfBoundsExceptionWhenSectionsArrayTooSmall() throws Exception {
        @SuppressWarnings("unchecked")
        List<Record>[] tiny = (List<Record>[]) new List[2];
        setSectionsArray(message, tiny);
        Record target = Record.newRecord(Name.fromConstantString("tiny.com."), Type.TXT, DClass.IN);
        try {
            message.findRecord(target);
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    @Test
    public void findRecord_propagatesExceptionFromRecordEquals() throws Exception {
        // Mockito is available in the classpath of the original project.
        Record problematic = org.mockito.Mockito.mock(Record.class);
        org.mockito.Mockito.when(problematic.equals(org.mockito.Mockito.any()))
                .thenThrow(new RuntimeException("equals exploded"));
        Record dummy = Record.newRecord(Name.fromConstantString("dummy.com."), Type.A, DClass.IN);
        message.addRecord(dummy, Section.ANSWER);
        try {
            message.findRecord(problematic);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("equals exploded", e.getMessage());
        }
    }
}
```