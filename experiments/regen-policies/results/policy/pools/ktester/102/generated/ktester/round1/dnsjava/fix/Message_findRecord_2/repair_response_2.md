```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Unit tests for {@link Message#findRecord(Record)}.
 *
 * The original test class contained many commented‑out sections,
 * duplicate variable declarations and a mixture of JUnit 4/JUnit 5
 * annotations which prevented the code from compiling and being
 * discovered by the test engine.  The class below keeps only the
 * essential tests, uses JUnit 5 annotations consistently and
 * provides the small reflection helpers that the tests need.
 */
class MessageFindRecordTest {

    private Message message;
    private Record aRecord;
    private Record txtRecord;

    @BeforeEach
    void setUp() throws Exception {
        message = new Message();
        Name name = Name.fromConstantString("www.example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
    }

    @AfterEach
    void tearDown() {
        message = null;
        aRecord = null;
        txtRecord = null;
    }

    /* --------------------------------------------------------------------- *
     *  Reflection helpers – they manipulate the private {@code sections}
     *  array inside {@link Message}.  The production code does not expose
     *  these operations, but the tests need them to set up edge cases.
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
     *  Test cases
     * --------------------------------------------------------------------- */

    @Test
    void findRecord_returnsTrueWhenRecordIsInAnswerSection() {
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue(message.findRecord(aRecord));
    }

    @Test
    void findRecord_returnsFalseWhenRecordIsNotPresent() {
        assertFalse(message.findRecord(txtRecord));
    }

    @Test
    void findRecord_checksAllSections() {
        message.addRecord(aRecord, Section.QUESTION);
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRecord(aRecord));
    }

    @Test
    void findRecord_returnsFalseWhenRecordExistsInDifferentSection() {
        message.addRecord(txtRecord, Section.ANSWER);
        assertFalse(message.findRecord(aRecord));
    }

    @Test
    void findRecord_earlyExitWhenRecordInMultipleSections() throws Exception {
        Record target = Record.newRecord(Name.fromConstantString("multi.com."), Type.A, DClass.IN);
        message.addRecord(target, Section.ANSWER);
        message.addRecord(target, Section.AUTHORITY);
        message.addRecord(target, Section.ADDITIONAL);
        assertTrue(message.findRecord(target));
    }

    @Test
    void findRecord_returnsFalseWhenArgumentIsNull() throws Exception {
        clearAllSections(message);
        Record dummy = Record.newRecord(Name.fromConstantString("dummy.com."), Type.A, DClass.IN);
        message.addRecord(dummy, Section.ANSWER);
        message.addRecord(dummy, Section.AUTHORITY);
        message.addRecord(dummy, Section.ADDITIONAL);
        assertFalse(message.findRecord((Record) null));
    }

    @Test
    void findRecord_returnsTrueWhenListContainsNullBeforeTarget() throws Exception {
        Record target = Record.newRecord(Name.fromConstantString("nullentry.com."), Type.TXT, DClass.IN);
        List<Record> list = new ArrayList<>(Arrays.asList(null, target));
        setSectionList(message, Section.ANSWER, list);
        assertTrue(message.findRecord(target));
    }

    @Test
    void findRecord_returnsFalseWhenOnlyNullInList() throws Exception {
        List<Record> list = new ArrayList<>(Collections.singletonList(null));
        setSectionList(message, Section.ANSWER, list);
        Record target = Record.newRecord(Name.fromConstantString("onlynull.com."), Type.AAAA, DClass.IN);
        assertFalse(message.findRecord(target));
    }

    @Test
    void findRecord_findsRecordInAdditionalAfterNullAndEmptySections() throws Exception {
        setSectionList(message, Section.AUTHORITY, Collections.emptyList());
        Record target = Record.newRecord(Name.fromConstantString("last.com."), Type.SRV, DClass.IN);
        message.addRecord(target, Section.ADDITIONAL);
        assertTrue(message.findRecord(target));
    }

    @Test
    void findRecord_throwsNullPointerExceptionWhenSectionsArrayIsNull() throws Exception {
        setSectionsArray(message, null);
        Record target = Record.newRecord(Name.fromConstantString("test.com."), Type.A, DClass.IN);
        assertThrows(NullPointerException.class, () -> message.findRecord(target));
    }

    @Test
    void findRecord_throwsArrayIndexOutOfBoundsExceptionWhenSectionsArrayTooSmall() throws Exception {
        @SuppressWarnings("unchecked")
        List<Record>[] tiny = (List<Record>[]) new List[2];
        setSectionsArray(message, tiny);
        Record target = Record.newRecord(Name.fromConstantString("tiny.com."), Type.TXT, DClass.IN);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.findRecord(target));
    }
}
```