package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_getOPT_Test {

    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test case
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Message does not hold external resources
        message = null;
    }

    /**
     * Verify that getOPT() returns {@code null} when the ADDITIONAL section does not contain
     * an {@link OPTRecord}.
     */
    @Test
    void getOPT_noOPTRecord_returnsNull() {
        // Add a regular TXT record to the ADDITIONAL section
        Name name = Name.fromConstantString("example.com.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "sample text");
        message.addRecord(txt, Section.ADDITIONAL);
        // No OPT record present – should return null
        assertNull(message.getOPT(), "Expected getOPT() to return null when no OPTRecord is present");
    }

    /**
     * Verify that getOPT() returns the OPT record that has been added to the ADDITIONAL section.
     */
    @Test
    void getOPT_withOPTRecord_returnsRecord() {
        // Create and add an OPT record
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        // The method should return the same instance
        OPTRecord result = message.getOPT();
        assertNotNull(result, "Expected getOPT() to return a non‑null OPTRecord");
        assertSame(opt, result, "Expected getOPT() to return the exact OPTRecord that was added");
    }

    /**
     * Verify that when multiple records are present in the ADDITIONAL section,
     * getOPT() returns the first {@link OPTRecord} encountered.
     */
    @Test
    void getOPT_multipleRecords_returnsFirstOPT() {
        Name name = Name.fromConstantString("example.org.");
        // Add a non‑OPT record first
        Record txt1 = new TXTRecord(name, DClass.IN, 3600, "first");
        message.addRecord(txt1, Section.ADDITIONAL);
        // Add the OPT record
        OPTRecord opt = new OPTRecord(1232, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        // Add another non‑OPT record after the OPT
        Record txt2 = new TXTRecord(name, DClass.IN, 3600, "second");
        message.addRecord(txt2, Section.ADDITIONAL);
        // getOPT should return the OPT we added (the only OPT in the list)
        assertSame(opt, message.getOPT(), "Expected getOPT() to return the first OPTRecord in the section");
    }

    /**
     * Verify that an OPT record can be retrieved even when it is the only record
     * in the ADDITIONAL section.
     */
    @Test
    void getOPT_onlyOPTRecord_returnsIt() {
        OPTRecord opt = new OPTRecord(512, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        OPTRecord retrieved = message.getOPT();
        assertNotNull(retrieved, "Expected a non‑null OPTRecord");
        assertEquals(opt, retrieved, "Expected the retrieved OPTRecord to be equal to the one added");
    }

    /**
     * Group: OPTRecord present - first record
     * The OPTRecord is the very first element in the ADDITIONAL section.
     */
    @Test
    void getOPT_OPTRecordFirst_returnsFirstOPT() {
        OPTRecord opt = new OPTRecord(1024, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        // add a normal record after it – it should not affect the result
        Name name = Name.fromConstantString("example.net.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "after");
        message.addRecord(txt, Section.ADDITIONAL);
        OPTRecord result = message.getOPT();
        assertNotNull(result);
        assertSame(opt, result, "The first OPTRecord should be returned");
    }

    /**
     * Group: OPTRecord present - not first record (single OPT after non‑OPT).
     */
    @Test
    void getOPT_OPTRecordNotFirst_returnsFirstOPT() {
        Name name = Name.fromConstantString("example.org.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "first");
        message.addRecord(txt, Section.ADDITIONAL);
        OPTRecord opt = new OPTRecord(2048, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        assertSame(opt, message.getOPT(), "When OPTRecord is not first, getOPT() must still locate and return it");
    }

    /**
     * Group: OPTRecord present - not first record (multiple OPTs).
     * Ensures the first encountered OPTRecord is returned.
     */
    @Test
    void getOPT_multipleOPTRecords_returnsFirstEncountered() {
        Name name = Name.fromConstantString("example.org.");
        // non‑OPT records before first OPT
        Record txt1 = new TXTRecord(name, DClass.IN, 3600, "a");
        message.addRecord(txt1, Section.ADDITIONAL);
        // first OPT
        OPTRecord opt1 = new OPTRecord(1500, 0, 0, 0);
        message.addRecord(opt1, Section.ADDITIONAL);
        // some more non‑OPT
        Record txt2 = new TXTRecord(name, DClass.IN, 3600, "b");
        message.addRecord(txt2, Section.ADDITIONAL);
        // second OPT (should be ignored)
        OPTRecord opt2 = new OPTRecord(2500, 0, 0, 0);
        message.addRecord(opt2, Section.ADDITIONAL);
        assertSame(opt1, message.getOPT(), "The first OPTRecord encountered should be returned, later ones ignored");
    }

    /**
     * Group: No OPTRecord in ADDITIONAL section – only non‑OPT records.
     */
    @Test
    void getOPT_noOPTOnlyNonOPT_returnsNull() {
        Name name = Name.fromConstantString("example.com.");
        for (int i = 0; i < 5; i++) {
            Record txt = new TXTRecord(name, DClass.IN, 3600, "txt" + i);
            message.addRecord(txt, Section.ADDITIONAL);
        }
        assertNull(message.getOPT(), "When ADDITIONAL contains only non‑OPT records, getOPT() must return null");
    }

    /**
     * Group: No OPTRecord in ADDITIONAL section – empty section.
     */
    @Test
    void getOPT_emptyAdditional_returnsNull() {
        // No records added at all.
        assertNull(message.getOPT(), "Empty ADDITIONAL section should result in null from getOPT()");
    }

    /**
     * Group: OPTRecord in non‑ADDITIONAL section – ANSWER.
     */
    @Test
    void getOPT_OPTInAnswerSection_ignored() {
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ANSWER);
        assertNull(message.getOPT(), "OPTRecord in ANSWER section must be ignored by getOPT()");
    }

    /**
     * Group: OPTRecord in non‑ADDITIONAL section – QUESTION (pseudo‑record).
     */
    @Test
    void getOPT_OPTInQuestionSection_ignored() {
        // The Question section normally holds a single query Record.
        // We simulate adding an OPTRecord there.
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.QUESTION);
        assertNull(message.getOPT(), "OPTRecord in QUESTION section must be ignored by getOPT()");
    }

    /**
     * Group: Null entries in ADDITIONAL section – only nulls.
     */
    @Test
    void getOPT_nullEntriesOnly_returnsNull() {
        // Spy the Message and stub getSection to return a list containing nulls.
        Message spy = spy(message);
        List<Record> listWithNulls = new ArrayList<>();
        listWithNulls.add(null);
        listWithNulls.add(null);
        doReturn(listWithNulls).when(spy).getSection(Section.ADDITIONAL);
        assertNull(spy.getOPT(), "When ADDITIONAL contains only null entries, getOPT() should return null");
    }

    /**
     * Group: Null entries in ADDITIONAL section – null followed by a valid OPTRecord.
     */
    @Test
    void getOPT_nullThenOPT_returnsOPT() {
        Message spy = spy(message);
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        List<Record> list = new ArrayList<>();
        // malformed entry
        list.add(null);
        // valid OPTRecord after null
        list.add(opt);
        doReturn(list).when(spy).getSection(Section.ADDITIONAL);
        OPTRecord result = spy.getOPT();
        assertNotNull(result);
        assertSame(opt, result, "getOPT() must skip null entries and return the first real OPTRecord");
    }

    /**
     * Group: Large ADDITIONAL list with late OPTRecord.
     * Creates 10,000 dummy TXT records before the OPTRecord.
     */
    @Test
    void getOPT_largeListLateOPT_returnsOPT() {
        Name name = Name.fromConstantString("example.com.");
        for (int i = 0; i < 10_000; i++) {
            Record txt = new TXTRecord(name, DClass.IN, 3600, "txt-" + i);
            message.addRecord(txt, Section.ADDITIONAL);
        }
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        OPTRecord result = message.getOPT();
        assertNotNull(result);
        assertSame(opt, result, "In a large list, getOPT() should still locate and return the late OPTRecord");
    }

    /**
     * Group: Multiple OPTRecords with later one earlier in list.
     * Ensures the first encountered OPTRecord is returned even if a later one appears earlier
     * in the iteration order (i.e., list order is preserved).
     */
    @Test
    void getOPT_multipleOPT_firstEncounteredReturned() {
        Name name = Name.fromConstantString("example.org.");
        // Non‑OPT records before first OPT
        for (int i = 0; i < 3; i++) {
            Record txt = new TXTRecord(name, DClass.IN, 3600, "pre-" + i);
            message.addRecord(txt, Section.ADDITIONAL);
        }
        OPTRecord firstOpt = new OPTRecord(1000, 0, 0, 0);
        message.addRecord(firstOpt, Section.ADDITIONAL);
        // More non‑OPT records
        for (int i = 0; i < 2; i++) {
            Record txt = new TXTRecord(name, DClass.IN, 3600, "mid-" + i);
            message.addRecord(txt, Section.ADDITIONAL);
        }
        // Second OPT (should be ignored)
        OPTRecord secondOpt = new OPTRecord(2000, 0, 0, 0);
        message.addRecord(secondOpt, Section.ADDITIONAL);
        assertSame(firstOpt, message.getOPT(), "The first OPTRecord encountered must be returned, regardless of later OPTs");
    }

    /**
     * Group: Empty ADDITIONAL section after removal.
     */
    @Test
    void getOPT_afterRemoval_returnsNull() {
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        // Remove everything from the ADDITIONAL section
        message.removeAllRecords(Section.ADDITIONAL);
        assertNull(message.getOPT(), "After removal of all ADDITIONAL records, getOPT() should return null");
    }

    /**
     * Group: getSection throws IllegalArgumentException.
     * We spy the Message and force {@code getSection(Section.ADDITIONAL)} to throw.
     */
    @Test
    void getOPT_sectionThrowsIllegalArgumentException_propagates() {
        Message spy = spy(message);
        doThrow(new IllegalArgumentException("forced for test")).when(spy).getSection(Section.ADDITIONAL);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, spy::getOPT, "getOPT() must propagate IllegalArgumentException from getSection");
        assertEquals("forced for test", ex.getMessage());
    }
}
