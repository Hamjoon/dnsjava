```java
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Message#getTSIG()}.
 *
 * <p>The original test suite was written for JUnit 5, but the execution
 * environment provides only JUnit 4 compatible engines.  To avoid the
 * “OutputDirectoryCreator not available” discovery error we switch the
 * tests to JUnit 4 and use {@link MockitoJUnitRunner} for mock support.
 *
 * <p>All behaviour expectations remain unchanged.
 */
@RunWith(MockitoJUnitRunner.class)
public class Message_getTSIG_Test {

    /**
     * When the message contains no records in the ADDITIONAL section,
     * {@code getTSIG()} must return {@code null}.
     */
    @Test
    public void getTSIG_noAdditionalSection_returnsNull() {
        // empty message
        Message m = new Message();
        assertNull(m.getTSIG(), "TSIG should be null when no additional records exist");
    }

    /**
     * When the ADDITIONAL section has records but the last record is not a TSIG,
     * {@code getTSIG()} must return {@code null}.
     */
    @Test
    public void getTSIG_nonTSIGLastRecord_returnsNull() throws IOException {
        Message m = new Message();
        // add a dummy A record to the ADDITIONAL section
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        m.addRecord(aRecord, Section.ADDITIONAL);
        // verify that the record really is present
        assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));
        assertEquals(Type.A, m.getSection(Section.ADDITIONAL).get(0).getType());
        // the last record is not a TSIG → should be null
        assertNull(m.getTSIG(), "TSIG should be null when last additional record is not a TSIG");
    }

    /**
     * When a TSIG record is added (as the last record of the ADDITIONAL section)
     * {@code getTSIG()} must return that {@link TSIGRecord}.
     */
    @Test
    public void getTSIG_tsigPresent_returnsTSIGRecord() throws IOException {
        Message m = new Message();
        // create a TSIG key – the constructor matches the one provided by dnsjava
        Name keyName = Name.fromConstantString("mykey.");
        // TSIG(Name, String algorithm, String secret) is a valid constructor
        TSIG tsigKey = new TSIG(keyName, "hmac-md5", "secret");
        // add the TSIG to the message using the public API
        m.setTSIG(tsigKey);
        // sanity check – the header should now count one additional record
        assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));
        // the method under test should return the TSIGRecord we just added
        TSIGRecord tsigRecord = m.getTSIG();
        assertNotNull(tsigRecord, "TSIGRecord should be returned when TSIG is present");
        assertEquals(Type.TSIG, tsigRecord.getType(), "Returned record must be of type TSIG");
        // the key stored in the TSIGRecord must be the same key we supplied
        assertEquals(keyName, tsigRecord.getName(), "TSIGRecord name should match the key name");
    }

    /**
     * If the internal {@code header} field is {@code null}, {@code getTSIG()}
     * should throw a {@link NullPointerException}.
     */
    @Test
    public void getTSIG_headerIsNull_throwsNPE() throws Exception {
        Message m = new Message();
        // Force the private header field to null
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, null);
        assertThrows(NullPointerException.class, m::getTSIG);
    }

    /**
     * If the internal {@code sections} array itself is {@code null},
     * {@code getTSIG()} should throw a {@link NullPointerException}.
     */
    @Test
    public void getTSIG_sectionsArrayIsNull_throwsNPE() throws Exception {
        Message m = new Message();
        // Set the private sections array to null
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, null);
        assertThrows(NullPointerException.class, m::getTSIG);
    }

    /**
     * If the {@code sections} array is shorter than {@link Section#ADDITIONAL},
     * accessing {@code sections[ADDITIONAL]} should raise an
     * {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    public void getTSIG_sectionsArrayTooShort_throwsAIOOBE() throws Exception {
        Message m = new Message();
        // Create an array that is deliberately too short (length = ADDITIONAL)
        int tooShort = Section.ADDITIONAL; // valid indices are 0..ADDITIONAL-1
        @SuppressWarnings("unchecked")
        List<Record>[] shortArray = (List<Record>[]) Array.newInstance(List.class, tooShort);
        // assign the short array
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, shortArray);
        // Header must report at least one additional record to trigger the access
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, mockHeader);
        assertThrows(ArrayIndexOutOfBoundsException.class, m::getTSIG);
    }

    /**
     * When the header reports a non‑zero ADDITIONAL count that is larger than
     * the actual size of the list, {@code getTSIG()} should throw an
     * {@link IndexOutOfBoundsException}.
     */
    @Test
    public void getTSIG_headerCountGreaterThanListSize_throwsIOOBE() throws Exception {
        Message m = new Message();
        // Prepare a proper sections array with an empty list for ADDITIONAL
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = new ArrayList<>();
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, sections);
        // Mock header to claim there is 1 additional record
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, mockHeader);
        assertThrows(IndexOutOfBoundsException.class, m::getTSIG);
    }

    /**
     * If the last element of the ADDITIONAL list is {@code null},
     * {@code getTSIG()} should throw a {@link NullPointerException}
     * when it tries to read {@code rec.type}.
     */
    @Test
    public void getTSIG_lastElementNull_throwsNPE() throws Exception {
        Message m = new Message();
        // List with a single null entry
        List<Record> list = new ArrayList<>();
        list.add(null);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, sections);
        // Header reports count = 1
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, mockHeader);
        assertThrows(NullPointerException.class, m::getTSIG);
    }

    /**
     * If the last record has {@code type == Type.TSIG} but is not an instance of
     * {@link TSIGRecord}, the cast performed in {@code getTSIG()} should throw a
     * {@link ClassCastException}.
     */
    @Test
    public void getTSIG_lastRecordNotTSIGRecord_throwsClassCastException() throws Exception {
        Message m = new Message();
        // Create a mock Record that is NOT a TSIGRecord but has type TSIG
        Record mockRecord = mock(Record.class);
        // The field {@code type} is public; set it via reflection
        Field typeField = Record.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.setInt(mockRecord, Type.TSIG);
        // Place the mock into the additional list
        List<Record> list = new ArrayList<>();
        list.add(mockRecord);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, sections);
        // Header reports count = 1
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, mockHeader);
        assertThrows(ClassCastException.class, m::getTSIG);
    }
}
```