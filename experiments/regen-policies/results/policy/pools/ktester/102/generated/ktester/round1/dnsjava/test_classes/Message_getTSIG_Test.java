package org.xbill.DNS;

import java.io.IOException;
import static org.mockito.Mockito.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
// import static org.junit.Assert.*;
// import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// @ExtendWith(MockitoExtension.class)
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Message_getTSIG_Test {

    /**
     * When the message contains no records in the ADDITIONAL section,
     * {@code getTSIG()} must return {@code null}.
     */
    @Test
    void getTSIG_noAdditionalSection_returnsNull() {
        Message m = new Message();
        assertNull(m.getTSIG(), "TSIG should be null when no additional records exist");
    }

    /**
     * When the ADDITIONAL section has records but the last record is not a TSIG,
     * {@code getTSIG()} must return {@code null}.
     */
    @Test
    void getTSIG_nonTSIGLastRecord_returnsNull() throws IOException {
        Message m = new Message();
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        m.addRecord(aRecord, Section.ADDITIONAL);
        assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));
        assertEquals(Type.A, m.getSection(Section.ADDITIONAL).get(0).getType());
        assertNull(m.getTSIG(), "TSIG should be null when last additional record is not a TSIG");
    }

    /**
     * When a TSIG record is added (as the last record of the ADDITIONAL section)
     * {@code getTSIG()} must return that {@link TSIGRecord}.
     */
    @Test
    void getTSIG_tsigPresent_returnsTSIGRecord() throws IOException {
        Message m = new Message();
        Name keyName = Name.fromConstantString("mykey.");
        TSIG tsigKey = new TSIG(keyName, "hmac-md5", "secret");
        m.setTSIG(tsigKey);
        assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));
        TSIGRecord tsigRecord = m.getTSIG();
        assertNotNull(tsigRecord, "TSIGRecord should be returned when TSIG is present");
        assertEquals(Type.TSIG, tsigRecord.getType(), "Returned record must be of type TSIG");
        assertEquals(keyName, tsigRecord.getName(), "TSIGRecord name should match the key name");
    }

    /**
     * If the internal {@code header} field is {@code null}, {@code getTSIG()}
     * should throw a {@link NullPointerException}.
     */
    @Test
    void getTSIG_headerIsNull_throwsNPE() throws Exception {
        Message m = new Message();
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
    void getTSIG_sectionsArrayIsNull_throwsNPE() throws Exception {
        Message m = new Message();
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
    void getTSIG_sectionsArrayTooShort_throwsAIOOBE() throws Exception {
        Message m = new Message();
        // valid indices: 0 .. ADDITIONAL‑1
        int tooShort = Section.ADDITIONAL;
        @SuppressWarnings("unchecked")
        List<Record>[] shortArray = (List<Record>[]) Array.newInstance(List.class, tooShort);
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, shortArray);
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
    void getTSIG_headerCountGreaterThanListSize_throwsIOOBE() throws Exception {
        Message m = new Message();
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = new ArrayList<>();
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, sections);
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
    void getTSIG_lastElementNull_throwsNPE() throws Exception {
        Message m = new Message();
        List<Record> list = new ArrayList<>();
        list.add(null);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, sections);
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
    void getTSIG_lastRecordNotTSIGRecord_throwsClassCastException() throws Exception {
        Message m = new Message();
        Record mockRecord = mock(Record.class);
        Field typeField = Record.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.setInt(mockRecord, Type.TSIG);
        List<Record> list = new ArrayList<>();
        list.add(mockRecord);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, sections);
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, mockHeader);
        assertThrows(ClassCastException.class, m::getTSIG);
    }
}
