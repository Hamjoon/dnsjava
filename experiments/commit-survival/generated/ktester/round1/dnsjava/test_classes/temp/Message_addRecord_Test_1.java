package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedList;

@ExtendWith(MockitoExtension.class)
class Message_addRecord_Test {

    private Message message;

    private Record txtRecord1;

    private Record txtRecord2;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Use the no‑arg constructor – it creates a fresh Header and empty sections.
        message = new Message();
        // Prepare two simple TXT records that will be added to the message.
        Name name = Name.fromConstantString("example.com.");
        txtRecord1 = new TXTRecord(name, DClass.IN, 3600, "first");
        txtRecord2 = new TXTRecord(name, DClass.IN, 3600, "second");
    }

    /**
     * Helper to set the raw {@code sections} array via reflection.
     */
    private void setRawSectionArray(List<Record>[] newArray) {
        try {
            Field f = Message.class.getDeclaredField("sections");
            f.setAccessible(true);
            f.set(message, newArray);
        } catch (ReflectiveOperationException e) {
            fail("Unable to set Message.sections field", e);
        }
    }

    @Test
    void addRecord_createsSectionWhenNull_andIncrementsHeaderCount() {
        // Section.ADDITIONAL is initially null inside the Message.
        assertNull(getRawSectionArray()[Section.ADDITIONAL]);
        // Add the first record.
        message.addRecord(txtRecord1, Section.ADDITIONAL);
        // The section list should now be instantiated and contain the record.
        List<Record> section = message.getSection(Section.ADDITIONAL);
        assertNotNull(section, "Section list should be created");
        assertEquals(1, section.size(), "Section should contain exactly one record");
        assertSame(txtRecord1, section.get(0), "The stored record must be the one added");
        // Header count for the ADDITIONAL section must be incremented to 1.
        assertEquals(1, message.getHeader().getCount(Section.ADDITIONAL));
    }

    @Test
    void addRecord_appendsToExistingSection_andUpdatesCount() {
        // First insertion – creates the list.
        message.addRecord(txtRecord1, Section.ANSWER);
        // Second insertion – should append to the already‑created list.
        message.addRecord(txtRecord2, Section.ANSWER);
        List<Record> answerSection = message.getSection(Section.ANSWER);
        assertEquals(2, answerSection.size(), "Two records should be present");
        assertSame(txtRecord1, answerSection.get(0));
        assertSame(txtRecord2, answerSection.get(1));
        // Header count must reflect both additions.
        assertEquals(2, message.getHeader().getCount(Section.ANSWER));
    }

    @Test
    void addRecord_doesNotAffectOtherSections() {
        // Add a record to the QUESTION section.
        message.addRecord(txtRecord1, Section.QUESTION);
        // Verify QUESTION section count.
        assertEquals(1, message.getHeader().getCount(Section.QUESTION));
        // All other sections should still be empty / null.
        for (int i = 0; i < 4; i++) {
            if (i == Section.QUESTION)
                continue;
            List<Record> sec = message.getSection(i);
            assertTrue(sec.isEmpty(), "Section " + i + " should be empty");
            assertEquals(0, message.getHeader().getCount(i), "Header count for section " + i + " should be zero");
        }
    }

    @Test
    void addRecord_invalidSection_throwsArrayIndexOutOfBoundsException() {
        // Section indices are defined 0‑3. Using 5 should trigger an exception.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.addRecord(txtRecord1, 5));
    }

    /**
     * Helper to expose the raw {@code sections} array via reflection (used only for the first test).
     */
    @SuppressWarnings("unchecked")
    private List<Record>[] getRawSectionArray() {
        try {
            java.lang.reflect.Field f = Message.class.getDeclaredField("sections");
            f.setAccessible(true);
            return (List<Record>[]) f.get(message);
        } catch (ReflectiveOperationException e) {
            fail("Unable to access Message.sections field", e);
            // unreachable
            return null;
        }
    }

    @Test
    void addRecord_sectionZeroInitiallyNull_createsListAndIncrementsHeader() {
        // Section 0 (QUESTION) is null initially.
        assertNull(getRawSectionArray()[Section.QUESTION]);
        Record r = new ARecord(Name.fromString("example.com."), DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        message.addRecord(r, Section.QUESTION);
        List<Record> sec = message.getSection(Section.QUESTION);
        assertNotNull(sec);
        assertEquals(1, sec.size());
        assertSame(r, sec.get(0));
        assertEquals(1, message.getHeader().getCount(Section.QUESTION));
    }

    @Test
    void addRecord_negativeSectionIndex_throwsArrayIndexOutOfBoundsException() {
        Record r = new MXRecord(Name.fromString("bad.net."), DClass.IN, 300, 10, Name.fromString("mail.bad.net."));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.addRecord(r, -1));
    }

    @Test
    void addRecord_nullRecord_isAddedAndCountIncremented() {
        // Ensure the target section is null before the call.
        assertNull(getRawSectionArray()[Section.AUTHORITY]);
        message.addRecord(null, Section.AUTHORITY);
        List<Record> sec = message.getSection(Section.AUTHORITY);
        assertNotNull(sec);
        assertEquals(1, sec.size());
        assertNull(sec.get(0));
        assertEquals(1, message.getHeader().getCount(Section.AUTHORITY));
    }

    @Test
    void addRecord_duplicateRecord_isStoredTwice() {
        Record r = new ARecord(Name.fromString("dup.example."), DClass.IN, 3600, InetAddress.getByName("9.9.9.9"));
        // First insertion
        message.addRecord(r, Section.QUESTION);
        // Second insertion (duplicate)
        message.addRecord(r, Section.QUESTION);
        List<Record> sec = message.getSection(Section.QUESTION);
        assertEquals(2, sec.size());
        assertSame(r, sec.get(0));
        assertSame(r, sec.get(1));
        assertEquals(2, message.getHeader().getCount(Section.QUESTION));
    }

    @Test
    void addRecord_recordWithZeroTTL_isAccepted() {
        Record r = new ARecord(Name.fromString("zero.ttl."), DClass.IN, 0, InetAddress.getByName("10.0.0.1"));
        message.addRecord(r, Section.AUTHORITY);
        List<Record> sec = message.getSection(Section.AUTHORITY);
        assertEquals(1, sec.size());
        assertSame(r, sec.get(0));
        assertEquals(0, ((ARecord) r).getTTL());
        assertEquals(1, message.getHeader().getCount(Section.AUTHORITY));
    }

    @Test
    void addRecord_headerIsNull_throwsNullPointerException() {
        // Explicitly set header to null.
        message.setHeader(null);
        Record r = new ARecord(Name.fromString("nullheader.example."), DClass.IN, 300, InetAddress.getByName("11.11.11.11"));
        assertThrows(NullPointerException.class, () -> message.addRecord(r, Section.ANSWER));
    }

    @Test
    void addRecord_sectionIndexEqualArrayLength_throwsArrayIndexOutOfBoundsException() {
        Record r = new NSRecord(Name.fromString("out.of.bounds."), DClass.IN, 3600, Name.fromString("ns.out.of.bounds."));
        // The sections array length is 4; index 4 is out of bounds.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.addRecord(r, 4));
    }

    @Test
    void addRecord_sectionsArrayIsNull_throwsNullPointerException() throws Exception {
        // Set the private 'sections' field to null via reflection.
        setRawSectionArray(null);
        Record r = new ARecord(Name.fromString("nullsections.example."), DClass.IN, 300, InetAddress.getByName("12.12.12.12"));
        assertThrows(NullPointerException.class, () -> message.addRecord(r, Section.QUESTION));
    }

    @Test
    void addRecord_immutableSectionList_throwsUnsupportedOperationException() throws Exception {
        // Prepare an unmodifiable list for section 1 (ANSWER).
        List<Record> immutable = Collections.unmodifiableList(new LinkedList<>());
        List<Record>[] raw = getRawSectionArray();
        raw[Section.ANSWER] = immutable;
        Record r = new ARecord(Name.fromString("immutable.example."), DClass.IN, 300, InetAddress.getByName("13.13.13.13"));
        int countBefore = message.getHeader().getCount(Section.ANSWER);
        assertThrows(UnsupportedOperationException.class, () -> message.addRecord(r, Section.ANSWER));
        // Header count is incremented before the exception is thrown.
        assertEquals(countBefore + 1, message.getHeader().getCount(Section.ANSWER));
    }

    @Test
    void addRecord_headerIncCountThrowsIllegalArgumentException_propagates() {
        // Mock a Header that throws IllegalArgumentException for section 2.
        Header mockHeader = mock(Header.class);
        doThrow(new IllegalArgumentException("illegal section")).when(mockHeader).incCount(Section.AUTHORITY);
        // Replace the real header.
        message.setHeader(mockHeader);
        Record r = new ARecord(Name.fromString("header.illegal.arg.example."), DClass.IN, 300, InetAddress.getByName("14.14.14.14"));
        // The exception should be thrown before any list manipulation.
        assertThrows(IllegalArgumentException.class, () -> message.addRecord(r, Section.AUTHORITY));
        // Verify that no list entry was created.
        List<Record> sec = message.getSection(Section.AUTHORITY);
        assertTrue(sec.isEmpty(), "Section should remain empty after header failure");
    }
}
