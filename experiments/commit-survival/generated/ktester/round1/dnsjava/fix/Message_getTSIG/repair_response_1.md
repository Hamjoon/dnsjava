```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.mockito.MockedStatic;
import java.net.InetAddress;

/**
 * Tests for {@link Message#getTSIG()}.
 *
 * The original test suite used {@code Section.MAX_SECTIONS} which does not exist
 * in the version of dnsjava bundled with the kata.  The constant represents the
 * total number of DNS sections (question, answer, authority, additional) and is
 * equal to {@code Section.ADDITIONAL + 1}.  All occurrences have been replaced
 * with that expression so the code compiles against the current library.
 */
class Message_getTSIG_Test {

    /**
     * Utility to set a private field via reflection.
     */
    private static void setPrivateField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * When the ADDITIONAL section is empty, getTSIG() must return {@code null}.
     */
    @Test
    void getTSIG_noAdditionalSection_returnsNull() {
        // empty message, no records
        Message msg = new Message();
        assertNull(msg.getTSIG(), "TSIG should be null when no additional records are present");
    }

    /**
     * When the ADDITIONAL section contains records but the last one is not a TSIG,
     * getTSIG() must return {@code null}.
     */
    @Test
    void getTSIG_lastRecordNotTSIG_returnsNull() throws IOException {
        Message msg = new Message();
        // header reports two records
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(2);
        setPrivateField(msg, "header", mockHeader);
        // build additional section: ARecord then MXRecord (MX is not TSIG)
        List<Record> list = new ArrayList<>();
        Name name = Name.fromConstantString("example.com.");
        list.add(new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1")));
        list.add(new MXRecord(name, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com.")));
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        setPrivateField(msg, "sections", sections);
        assertNull(msg.getTSIG(), "TSIG should be null when the last additional record is not a TSIG");
    }

    /**
     * When a TSIG record is the last entry in the ADDITIONAL section,
     * getTSIG() must return that TSIGRecord.
     */
    @Test
    void getTSIG_lastRecordIsTSIG_returnsTSIGRecord() throws IOException {
        Message msg = new Message();
        // create a dummy TSIG key (algorithm and secret are arbitrary for the test)
        Name keyName = Name.fromConstantString("key.example.");
        Name algName = Name.fromConstantString("hmac-md5.sig-alg.reg.int.");
        TSIG tsigKey = new TSIG(keyName, algName, new byte[] { 0x01, 0x02, 0x03, 0x04 });
        // apply the TSIG to the message – this adds a TSIGRecord as the last additional record
        tsigKey.apply(msg, null);
        // sanity check: the header should now report one record in the ADDITIONAL section
        assertEquals(1, msg.getHeader().getCount(Section.ADDITIONAL));
        // retrieve the TSIG via the method under test
        TSIGRecord retrieved = msg.getTSIG();
        assertNotNull(retrieved, "TSIG should be returned when present as the last additional record");
        assertSame(TSIGRecord.class, retrieved.getClass(), "Returned record must be a TSIGRecord");
        // the retrieved record should be the same instance that was added by TSIG.apply()
        TSIGRecord expected = (TSIGRecord) msg.getSection(Section.ADDITIONAL).get(0);
        assertSame(expected, retrieved, "Returned TSIGRecord should be the one stored in the message");
    }

    /**
     * When multiple records exist in the ADDITIONAL section and the last one is a TSIG,
     * getTSIG() must still return the TSIGRecord.
     */
    @Test
    void getTSIG_multipleAdditionalRecords_lastIsTSIG_returnsTSIGRecord() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");
        // add a few non‑TSIG records first
        msg.addRecord(new TXTRecord(name, DClass.IN, 3600, "first"), Section.ADDITIONAL);
        msg.addRecord(new TXTRecord(name, DClass.IN, 3600, "second"), Section.ADDITIONAL);
        // now add a TSIG record
        Name keyName = Name.fromConstantString("key.example.");
        Name algName = Name.fromConstantString("hmac-md5.sig-alg.reg.int.");
        TSIG tsigKey = new TSIG(keyName, algName, new byte[] { 0x0A, 0x0B, 0x0C });
        tsigKey.apply(msg, null);
        // header count should reflect three additional records
        assertEquals(3, msg.getHeader().getCount(Section.ADDITIONAL));
        // getTSIG should return the last record (the TSIG)
        TSIGRecord tsig = msg.getTSIG();
        assertNotNull(tsig, "TSIG should be returned when it is the last additional record");
        assertEquals(Type.TSIG, tsig.getType(), "Returned record must have type TSIG");
        // ensure it is indeed the last element in the section list
        Record last = msg.getSection(Section.ADDITIONAL).get(2);
        assertSame(tsig, last, "TSIGRecord returned by getTSIG must be the last record in the ADDITIONAL section");
    }

    /**
     * When the internal {@code header} field is {@code null},
     * {@code getTSIG()} must throw a {@link NullPointerException}
     * because it attempts to call {@code header.getCount(...)}.
     */
    @Test
    void getTSIG_nullHeader_throwsNPE() {
        Message msg = new Message();
        // force header to null
        setPrivateField(msg, "header", null);
        assertThrows(NullPointerException.class, msg::getTSIG, "Calling getTSIG() with a null header should throw NPE");
    }

    /**
     * When the {@code sections[ADDITIONAL]} entry itself is {@code null},
     * {@code getTSIG()} must throw a {@link NullPointerException}
     * while trying to retrieve the list.
     */
    @Test
    void getTSIG_nullAdditionalSection_throwsNPE() {
        Message msg = new Message();
        // mock header to report one record in ADDITIONAL
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setPrivateField(msg, "header", mockHeader);
        // create an empty sections array but leave the ADDITIONAL slot null
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        setPrivateField(msg, "sections", sections);
        assertThrows(NullPointerException.class, msg::getTSIG, "Null additional section should result in NPE");
    }

    /**
     * If the header reports more records than are actually stored in the
     * {@code sections[ADDITIONAL]} list, {@code getTSIG()} will try to
     * access an invalid index and throw {@link IndexOutOfBoundsException}.
     */
    @Test
    void getTSIG_headerCountMismatch_throwsIOOBE() {
        Message msg = new Message();
        // header claims 5 records, but we only store 2
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(5);
        setPrivateField(msg, "header", mockHeader);
        // create a list with two generic records
        List<Record> list = new ArrayList<>();
        Name name = Name.fromConstantString("example.com.");
        list.add(new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1")));
        list.add(new MXRecord(name, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com.")));
        // install the list into the sections array
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        setPrivateField(msg, "sections", sections);
        assertThrows(IndexOutOfBoundsException.class, msg::getTSIG, "Header count larger than actual list size should cause IOBE");
    }

    /**
     * When the last record **is** a TSIGRecord, {@code getTSIG()} must return
     * that exact instance.
     */
    @Test
    void getTSIG_lastRecordIsTSIG_returnsInstance() {
        Message msg = new Message();
        // header reports one record
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setPrivateField(msg, "header", mockHeader);
        // create a TSIGRecord (using the real TSIG.apply would also work,
        // but we construct it manually to keep the test focused)
        Name keyName = Name.fromConstantString("key.example.");
        TSIGRecord tsigRecord = mock(TSIGRecord.class);
        // force the public 'type' field to be Type.TSIG
        setPrivateField(tsigRecord, "type", Type.TSIG);
        List<Record> list = new ArrayList<>();
        list.add(tsigRecord);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        setPrivateField(msg, "sections", sections);
        TSIGRecord result = msg.getTSIG();
        assertNotNull(result, "TSIG should be returned when present as the last record");
        assertSame(tsigRecord, result, "Returned TSIGRecord must be the same instance stored in the section");
    }

    /**
     * When the ADDITIONAL section contains several records and the **last**
     * one is a TSIGRecord, {@code getTSIG()} must still return that TSIGRecord.
     */
    @Test
    void getTSIG_multipleRecords_lastIsTSIG_returnsTSIG() {
        Message msg = new Message();
        // header reports three records
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(3);
        setPrivateField(msg, "header", mockHeader);
        // first two are generic, third is TSIG
        List<Record> list = new ArrayList<>();
        Name name = Name.fromConstantString("example.com.");
        list.add(new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1")));
        list.add(new MXRecord(name, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com.")));
        TSIGRecord tsigRecord = mock(TSIGRecord.class);
        setPrivateField(tsigRecord, "type", Type.TSIG);
        list.add(tsigRecord);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        setPrivateField(msg, "sections", sections);
        TSIGRecord result = msg.getTSIG();
        assertNotNull(result, "TSIG should be returned when it is the last additional record");
        assertSame(tsigRecord, result, "Returned TSIGRecord must be the last element in the list");
    }

    /**
     * A TSIGRecord that is **not** the last element must be ignored;
     * {@code getTSIG()} should return {@code null}.
     */
    @Test
    void getTSIG_TSIGNotLast_returnsNull() {
        Message msg = new Message();
        // header reports two records
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(2);
        setPrivateField(msg, "header", mockHeader);
        // first is TSIG, second is ARecord
        List<Record> list = new ArrayList<>();
        TSIGRecord tsigRecord = mock(TSIGRecord.class);
        setPrivateField(tsigRecord, "type", Type.TSIG);
        list.add(tsigRecord);
        Name name = Name.fromConstantString("example.com.");
        list.add(new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1")));
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        setPrivateField(msg, "sections", sections);
        assertNull(msg.getTSIG(), "TSIG not being the last record must result in null");
    }
}
```