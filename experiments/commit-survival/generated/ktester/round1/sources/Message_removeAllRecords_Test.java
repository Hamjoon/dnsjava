package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_removeAllRecords_Test {

    private Message message;

    private Header header;

    @BeforeEach
    void setupBeforeEach() {
        // The default constructor creates a fresh Header and an empty sections array.
        message = new Message();
        header = message.getHeader();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the Message instance will be GC‑ed.
        message = null;
        header = null;
    }

    /**
     * Utility to set a private field via reflection.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void removeAllRecords_FromPopulatedSection_ClearsSectionAndHeaderCount() throws UnknownHostException {
        // Arrange – add a few records to the ANSWER section.
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        Record txtRecord = new TXTRecord(name, DClass.IN, 3600, "test");
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(txtRecord, Section.ANSWER);
        // Verify pre‑condition.
        assertEquals(2, message.getSection(Section.ANSWER).size(), "pre‑condition: two records must be present");
        assertEquals(2, header.getCount(Section.ANSWER), "pre‑condition: header count must reflect the added records");
        // Act – remove all records from the ANSWER section.
        message.removeAllRecords(Section.ANSWER);
        // Assert – the section is empty and the header count is zero.
        assertTrue(message.getSection(Section.ANSWER).isEmpty(), "section should be empty after removal");
        assertEquals(0, header.getCount(Section.ANSWER), "header count should be reset to zero");
    }

    @Test
    void removeAllRecords_FromEmptySection_LeavesHeaderCountZero() {
        // Arrange – ensure the QUESTION section is empty (it is by default).
        assertTrue(message.getSection(Section.QUESTION).isEmpty(), "pre‑condition: QUESTION section must be empty");
        assertEquals(0, header.getCount(Section.QUESTION), "pre‑condition: header count must be zero");
        // Act – invoke removal on the empty section.
        message.removeAllRecords(Section.QUESTION);
        // Assert – still empty and count remains zero.
        assertTrue(message.getSection(Section.QUESTION).isEmpty(), "empty section should stay empty");
        assertEquals(0, header.getCount(Section.QUESTION), "header count should remain zero");
    }

    @Test
    void removeAllRecords_InvalidSection_ThrowsArrayIndexOutOfBoundsException() {
        // The valid section indices are 0‑3 (Section constants). Using 4 should cause
        // an ArrayIndexOutOfBoundsException because the implementation accesses the
        // internal array directly without bounds checking.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.removeAllRecords(4), "removing records from an invalid section should throw");
    }

    @ParameterizedTest(name = "removeAllRecords on valid section {0}")
    @ValueSource(ints = { Section.QUESTION, Section.ADDITIONAL })
    void removeAllRecords_ValidSection_ClearsInternalArrayAndResetsHeader(int section) throws Exception {
        // Populate the section first to ensure the internal array entry is non‑null.
        Name name = Name.fromConstantString("test.com.");
        Record rec = new ARecord(name, DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        message.addRecord(rec, section);
        // Verify pre‑condition.
        assertNotNull(getInternalSectionsArray()[section], "pre‑condition: internal sections entry must be non‑null");
        assertEquals(1, header.getCount(section));
        // Act
        message.removeAllRecords(section);
        // Assert internal array entry is now null.
        assertNull(getInternalSectionsArray()[section], "sections[" + section + "] should be null after removal");
        // Header count must be zero.
        assertEquals(0, header.getCount(section), "header count for section " + section + " should be reset to zero");
    }

    @ParameterizedTest(name = "removeAllRecords with invalid index {0}")
    @ValueSource(ints = { -1, 5 })
    void removeAllRecords_InvalidSectionIndices_ThrowArrayIndexOutOfBounds(int invalidIdx) {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.removeAllRecords(invalidIdx), "Invalid section index should cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void removeAllRecords_NullHeader_ThrowsNullPointerException() throws Exception {
        // Use reflection to set the private header field to null.
        setPrivateField(message, "header", null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> message.removeAllRecords(Section.ANSWER), "Calling removeAllRecords with a null header should throw NPE");
    }

    @Test
    void removeAllRecords_AlreadyEmptySection_IsIdempotent() throws Exception {
        // Ensure SECTION 2 (AUTHORITY) is empty and header count is zero.
        assertTrue(message.getSection(Section.AUTHORITY).isEmpty(), "pre‑condition: authority section must be empty");
        assertEquals(0, header.getCount(Section.AUTHORITY));
        // Act – call removal.
        message.removeAllRecords(Section.AUTHORITY);
        // Assert – still empty and count remains zero.
        assertTrue(message.getSection(Section.AUTHORITY).isEmpty(), "section should stay empty after idempotent removal");
        assertEquals(0, header.getCount(Section.AUTHORITY));
    }

    @Test
    void removeAllRecords_AfterAddingRecords_SectionClearedAndHeaderReset() throws Exception {
        // Add two records to ANSWER.
        Name name = Name.fromConstantString("example.org.");
        Record rec1 = new ARecord(name, DClass.IN, 300, InetAddress.getByName("10.0.0.1"));
        Record rec2 = new TXTRecord(name, DClass.IN, 300, "hello");
        message.addRecord(rec1, Section.ANSWER);
        message.addRecord(rec2, Section.ANSWER);
        assertEquals(2, message.getSection(Section.ANSWER).size(), "pre‑condition: two records should be present");
        assertEquals(2, header.getCount(Section.ANSWER));
        // Act
        message.removeAllRecords(Section.ANSWER);
        // Assert
        assertTrue(message.getSection(Section.ANSWER).isEmpty(), "section should be empty after removal");
        assertEquals(0, header.getCount(Section.ANSWER));
    }

    @Test
    void removeAllRecords_HeaderSetCountInvokedExactlyOnce_Section0(@Mock Header mockHeader) {
        // Replace the real header with a mock.
        message.setHeader(mockHeader);
        // Act
        message.removeAllRecords(Section.QUESTION);
        // Verify that setCount was called once with (0,0).
        verify(mockHeader, times(1)).setCount(Section.QUESTION, 0);
    }

    @Test
    void removeAllRecords_HeaderSetCountInvokedExactlyOnce_Section3(@Mock Header mockHeader) {
        message.setHeader(mockHeader);
        message.removeAllRecords(Section.ADDITIONAL);
        verify(mockHeader, times(1)).setCount(Section.ADDITIONAL, 0);
    }

    @Test
    void removeAllRecords_NullSectionsArray_ThrowsNullPointerException() throws Exception {
        // Set the private 'sections' field to null.
        setPrivateField(message, "sections", null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> message.removeAllRecords(Section.ANSWER), "When internal sections array is null, NPE should be thrown");
    }

    @Test
    void removeAllRecords_HeaderSetCountThrows_ExceptionPropagates(@Mock Header mockHeader) {
        // Configure mock to throw IllegalStateException when setCount is called.
        doThrow(new IllegalStateException("forced")).when(mockHeader).setCount(anyInt(), anyInt());
        // Replace header with the misbehaving mock.
        message.setHeader(mockHeader);
        // Act & Assert – the IllegalStateException must bubble up.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> message.removeAllRecords(Section.QUESTION));
        assertEquals("forced", ex.getMessage());
    }

    /**
     * Retrieves the private {@code sections} array from the Message instance.
     */
    @SuppressWarnings("unchecked")
    private List<Record>[] getInternalSectionsArray() throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        return (List<Record>[]) f.get(message);
    }
}
