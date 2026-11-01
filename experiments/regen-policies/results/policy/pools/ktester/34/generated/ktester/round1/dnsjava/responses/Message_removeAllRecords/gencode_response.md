```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link Message#removeAllRecords(int)}.
 *
 * <p>The tests verify that:
 * <ul>
 *   <li>the specified section is cleared and the header count for that section is set to zero;</li>
 *   <li>other sections remain untouched;</li>
 *   <li>an {@link IllegalArgumentException} is thrown for an invalid section index;</li>
 *   <li>a {@link NullPointerException} is thrown when the internal {@code header} or {@code sections}
 *       array is {@code null} (simulating corrupted state).</li>
 * </ul>
 *
 * <p>Mockito is used to replace the {@code Header} with a mock so that we can verify that
 * {@code Header#setCount(int,int)} is invoked exactly once with the expected arguments.
 */
class Message_removeAllRecords_Test {

    private Message msg;
    private Record aRecord;
    private Record txtRecord;
    private Record mxRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        // Build a fresh Message with one record in each of the four sections
        msg = new Message();

        Name name = Name.fromConstantString("www.example.");

        // QUESTION section (0)
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        msg.addRecord(aRecord, Section.QUESTION);

        // ANSWER section (1)
        txtRecord = new TXTRecord(name, DClass.IN, 3600, "test");
        msg.addRecord(txtRecord, Section.ANSWER);

        // AUTHORITY section (2)
        mxRecord = new MXRecord(name, DClass.IN, 3600, 10,
                Name.fromConstantString("mail.example."));
        msg.addRecord(mxRecord, Section.AUTHORITY);

        // ADDITIONAL section (3)
        Record additional = new ARecord(name, DClass.IN, 3600,
                InetAddress.getByName("127.0.0.1"));
        msg.addRecord(additional, Section.ADDITIONAL);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* -------------------------------------------------------------
     *  Existing straightforward tests (kept unchanged)
     * ------------------------------------------------------------- */

    @Test
    void removeAllRecords_clearsSpecifiedSection_andLeavesOthersIntact() {
        // Verify initial counts are 1 for each section
        Header header = msg.getHeader();
        assertEquals(1, header.getCount(Section.QUESTION));
        assertEquals(1, header.getCount(Section.ANSWER));
        assertEquals(1, header.getCount(Section.AUTHORITY));
        assertEquals(1, header.getCount(Section.ADDITIONAL));

        // Remove all records from the ANSWER section
        msg.removeAllRecords(Section.ANSWER);

        // ANSWER section should now be empty and its count zero
        List<Record> answerSec = msg.getSection(Section.ANSWER);
        assertTrue(answerSec.isEmpty(), "ANSWER section should be empty after removal");
        assertEquals(0, header.getCount(Section.ANSWER));

        // Other sections must retain their records and counts
        assertEquals(1, header.getCount(Section.QUESTION));
        assertEquals(1, header.getCount(Section.AUTHORITY));
        assertEquals(1, header.getCount(Section.ADDITIONAL));

        assertEquals(1, msg.getSection(Section.QUESTION).size());
        assertEquals(1, msg.getSection(Section.AUTHORITY).size());
        assertEquals(1, msg.getSection(Section.ADDITIONAL).size());
    }

    @Test
    void removeAllRecords_multipleCalls_clearsAllSections() {
        // Remove each section one by one
        for (int i = 0; i < 4; i++) {
            msg.removeAllRecords(i);
        }

        // All sections should be empty and header counts zero
        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(),
                    "Section " + i + " should be empty after removeAllRecords");
            assertEquals(0, msg.getHeader().getCount(i),
                    "Header count for section " + i + " should be zero after removal");
        }
    }

    @Test
    void removeAllRecords_invalidSection_throwsIllegalArgumentException() {
        // Section indices are 0‑3; using 4 should trigger an exception from Section.check
        assertThrows(IllegalArgumentException.class, () -> msg.removeAllRecords(4));
        // Negative index also invalid
        assertThrows(IllegalArgumentException.class, () -> msg.removeAllRecords(-1));
    }

    /* -------------------------------------------------------------
     *  Parameterized test for the four valid sections – also verifies
     *  that Header#setCount is called with the correct arguments.
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(ints = {Section.QUESTION, Section.ANSWER, Section.AUTHORITY, Section.ADDITIONAL})
    void removeAllRecords_validSection_updatesHeaderAndClearsArray(int section) throws Exception {
        // Replace the real Header with a Mockito mock so we can verify interaction
        Header mockHeader = mock(Header.class);
        // Preserve the original header count for later sanity‑check (not strictly required)
        when(mockHeader.getCount(anyInt())).thenReturn(1);
        setPrivateField(msg, "header", mockHeader);

        // Ensure the sections array is present (it is created by the constructor)
        // No need to modify it – we just want to verify the array entry becomes null.

        // Act
        msg.removeAllRecords(section);

        // Verify that Header#setCount(section,0) was invoked exactly once
        verify(mockHeader, times(1)).setCount(section, 0);

        // Using reflection, check that the internal sections array entry is now null
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        assertNull(sections[section],
                "Internal sections[" + section + "] should be null after removal");

        // The public API should also report an empty list for that section
        List<Record> publicList = msg.getSection(section);
        assertTrue(publicList.isEmpty(),
                "Message.getSection(" + section + ") should return an empty list");
    }

    /* -------------------------------------------------------------
     *  Null‑header handling – expect NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void removeAllRecords_nullHeader_throwsNullPointerException() throws Exception {
        // Force the private header field to null via reflection
        setPrivateField(msg, "header", null);

        // Any valid section index should now cause a NPE when header.setCount is called
        Executable call = () -> msg.removeAllRecords(Section.QUESTION);
        assertThrows(NullPointerException.class, call,
                "removeAllRecords should throw NPE when header is null");
    }

    /* -------------------------------------------------------------
     *  Null‑sections array handling – expect NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void removeAllRecords_nullSectionsArray_throwsNullPointerException() throws Exception {
        // Force the private sections field to null via reflection
        setPrivateField(msg, "sections", null);

        // The method will attempt to assign to sections[section] and thus NPE
        Executable call = () -> msg.removeAllRecords(Section.ANSWER);
        assertThrows(NullPointerException.class, call,
                "removeAllRecords should throw NPE when sections array is null");
    }

    /* -------------------------------------------------------------
     *  Helper to set private fields via reflection.
     * ------------------------------------------------------------- */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
```