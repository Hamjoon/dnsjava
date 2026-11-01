```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Message#getOPT()}.
 *
 * <p>The tests cover the scenarios described in the JSON test‑case specification:
 *
 * <ul>
 *   <li>OPTRecord present (first, middle, multiple, late in a large list)</li>
 *   <li>No OPTRecord (empty section, only non‑OPT records)</li>
 *   <li>OPTRecord in a non‑ADDITIONAL section</li>
 *   <li>Null entries inside the ADDITIONAL list</li>
 *   <li>Section removal before the call</li>
 *   <li>Propagation of an {@link IllegalArgumentException} thrown by {@link Message#getSection(int)}</li>
 * </ul>
 *
 * <p>All tests use the provided {@code @BeforeEach} setup that creates a fresh {@link Message}
 * instance. Mockito is used only where we need to stub or spy on the {@code Message}
 * implementation (e.g., to force an exception or to inject a custom list containing {@code null}).
 */
@ExtendWith(MockitoExtension.class)
class Message_getOPT_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for every test.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- *
     *  Existing tests (kept unchanged)                                      *
     * --------------------------------------------------------------------- */

    @Test
    void getOPT_noOPTRecord_returnsNull() {
        Name name = Name.fromConstantString("example.com.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "sample text");
        message.addRecord(txt, Section.ADDITIONAL);
        assertNull(message.getOPT(), "Expected getOPT() to return null when no OPTRecord is present");
    }

    @Test
    void getOPT_withOPTRecord_returnsRecord() {
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        OPTRecord result = message.getOPT();
        assertNotNull(result, "Expected getOPT() to return a non‑null OPTRecord");
        assertSame(opt, result, "Expected getOPT() to return the exact OPTRecord that was added");
    }

    @Test
    void getOPT_multipleRecords_returnsFirstOPT() {
        Name name = Name.fromConstantString("example.org.");

        Record txt1 = new TXTRecord(name, DClass.IN, 3600, "first");
        message.addRecord(txt1, Section.ADDITIONAL);

        OPTRecord opt = new OPTRecord(1232, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        Record txt2 = new TXTRecord(name, DClass.IN, 3600, "second");
        message.addRecord(txt2, Section.ADDITIONAL);

        assertSame(opt, message.getOPT(),
                "Expected getOPT() to return the first OPTRecord in the section");
    }

    @Test
    void getOPT_onlyOPTRecord_returnsIt() {
        OPTRecord opt = new OPTRecord(512, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        OPTRecord retrieved = message.getOPT();
        assertNotNull(retrieved, "Expected a non‑null OPTRecord");
        assertEquals(opt, retrieved,
                "Expected the retrieved OPTRecord to be equal to the one added");
    }

    /* --------------------------------------------------------------------- *
     *  Additional tests derived from the JSON specification                  *
     * --------------------------------------------------------------------- */

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

        assertSame(opt, message.getOPT(),
                "When OPTRecord is not first, getOPT() must still locate and return it");
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

        assertSame(opt1, message.getOPT(),
                "The first OPTRecord encountered should be returned, later ones ignored");
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
        assertNull(message.getOPT(),
                "When ADDITIONAL contains only non‑OPT records, getOPT() must return null");
    }

    /**
     * Group: No OPTRecord in ADDITIONAL section – empty section.
     */
    @Test
    void getOPT_emptyAdditional_returnsNull() {
        // No records added at all.
        assertNull(message.getOPT(),
                "Empty ADDITIONAL section should result in null from getOPT()");
    }

    /**
     * Group: OPTRecord in non‑ADDITIONAL section – ANSWER.
     */
    @Test
    void getOPT_OPTInAnswerSection_ignored() {
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ANSWER);
        assertNull(message.getOPT(),
                "OPTRecord in ANSWER section must be ignored by getOPT()");
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
        assertNull(message.getOPT(),
                "OPTRecord in QUESTION section must be ignored by getOPT()");
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

        assertNull(spy.getOPT(),
                "When ADDITIONAL contains only null entries, getOPT() should return null");
    }

    /**
     * Group: Null entries in ADDITIONAL section – null followed by a valid OPTRecord.
     */
    @Test
    void getOPT_nullThenOPT_returnsOPT() {
        Message spy = spy(message);
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        List<Record> list = new ArrayList<>();
        list.add(null);          // malformed entry
        list.add(opt);           // valid OPTRecord after null
        doReturn(list).when(spy).getSection(Section.ADDITIONAL);

        OPTRecord result = spy.getOPT();
        assertNotNull(result);
        assertSame(opt, result,
                "getOPT() must skip null entries and return the first real OPTRecord");
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
        assertSame(opt, result,
                "In a large list, getOPT() should still locate and return the late OPTRecord");
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

        assertSame(firstOpt, message.getOPT(),
                "The first OPTRecord encountered must be returned, regardless of later OPTs");
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
        assertNull(message.getOPT(),
                "After removal of all ADDITIONAL records, getOPT() should return null");
    }

    /**
     * Group: getSection throws IllegalArgumentException.
     * We spy the Message and force {@code getSection(Section.ADDITIONAL)} to throw.
     */
    @Test
    void getOPT_sectionThrowsIllegalArgumentException_propagates() {
        Message spy = spy(message);
        doThrow(new IllegalArgumentException("forced for test"))
                .when(spy).getSection(Section.ADDITIONAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                spy::getOPT,
                "getOPT() must propagate IllegalArgumentException from getSection");
        assertEquals("forced for test", ex.getMessage());
    }
}
```