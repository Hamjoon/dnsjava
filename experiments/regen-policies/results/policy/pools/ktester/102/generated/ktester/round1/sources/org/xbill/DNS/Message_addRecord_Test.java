package org.xbill.DNS;

import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
// import static org.junit.Assert.*;
// import org.junit.Before;
// import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// @ExtendWith(MockitoExtension.class)
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Message_addRecord_Test {

    private Message message;

    private Header header;

    //     @BeforeEach
    void setupBeforeEach() {
        // The no‑arg constructor creates a fresh Header and empty sections.
        message = new Message();
        header = message.getHeader();
    }

    /**
     * Sets a private field on the given target object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Helper that puts a specific List instance into {@code message.sections[section]}.
     */
    private static void setSectionList(Message msg, int section, List<Record> list) throws Exception {
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        if (sections == null) {
            sections = (List<Record>[]) new List[4];
            sectionsField.set(msg, sections);
        }
        sections[section] = list;
    }

    @Test
    void addRecordCreatesSectionAndIncrementsCount() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        int section = Section.ANSWER;
        assertEquals(0, header.getCount(section));
        assertTrue(message.getSection(section).isEmpty());
        message.addRecord(aRecord, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());
        assertSame(aRecord, message.getSection(section).get(0));
        assertTrue(message.getSection(section) instanceof java.util.LinkedList);
    }

    @Test
    void addRecordAppendsWhenSectionAlreadyExists() throws UnknownHostException {
        Name name = Name.fromConstantString("example.org.");
        Record first = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Record second = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        int section = Section.AUTHORITY;
        message.addRecord(first, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());
        message.addRecord(second, section);
        assertEquals(2, header.getCount(section));
        assertEquals(2, message.getSection(section).size());
        assertSame(first, message.getSection(section).get(0));
        assertSame(second, message.getSection(section).get(1));
    }

    @Test
    void addRecordInvalidSectionThrowsException() {
        Record dummy = Record.newRecord(Name.fromConstantString("invalid.example."), Type.A, DClass.IN);
        // valid sections are 0‑3
        int invalidSection = 4;
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.addRecord(dummy, invalidSection));
    }

    @Test
    void addRecordNullRecordThrowsNullPointerException() {
        Record nullRecord = null;
        int section = Section.QUESTION;
        assertThrows(NullPointerException.class, () -> message.addRecord(nullRecord, section));
    }

    @Test
    void addRecordWithNullHeaderThrowsNullPointerException() throws Exception {
        setPrivateField(message, "header", null);
        Name name = Name.fromConstantString("nullheader.example.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        int section = Section.AUTHORITY;
        assertThrows(NullPointerException.class, () -> message.addRecord(aRecord, section));
    }

    @Test
    void addRecordWithNullSectionsArrayThrowsNullPointerException() throws Exception {
        setPrivateField(message, "sections", null);
        Name name = Name.fromConstantString("nullsections.example.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.55"));
        int section = Section.ANSWER;
        assertThrows(NullPointerException.class, () -> message.addRecord(aRecord, section));
    }

    @Test
    void addRecordUsesExistingArrayListImplementation() throws Exception {
        List<Record> arrayList = new ArrayList<>();
        setSectionList(message, Section.ANSWER, arrayList);
        Name name = Name.fromConstantString("arraylist.example.");
        Record aRecord = new ARecord(name, DClass.IN, 300, InetAddress.getByName("192.0.2.10"));
        int section = Section.ANSWER;
        message.addRecord(aRecord, section);
        assertSame(arrayList, message.getSection(section));
        assertEquals(1, arrayList.size());
        assertEquals(1, header.getCount(section));
    }

    @Test
    void addRecordToUnmodifiableListThrowsUnsupportedOperationException() throws Exception {
        List<Record> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        setSectionList(message, Section.ANSWER, unmodifiable);
        Name name = Name.fromConstantString("unmod.example.");
        Record aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.20"));
        int section = Section.ANSWER;
        assertThrows(UnsupportedOperationException.class, () -> message.addRecord(aRecord, section));
    }

    @Test
    void addRecordToQuestionSectionWorksLikeOtherSections() throws UnknownHostException {
        Name name = Name.fromConstantString("lookup.example.");
        // Question records have a null address
        Record question = new ARecord(name, DClass.IN, 0, (InetAddress) null);
        int section = Section.QUESTION;
        assertEquals(0, header.getCount(section));
        assertTrue(message.getSection(section).isEmpty());
        message.addRecord(question, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());
        assertSame(question, message.getSection(section).get(0));
    }

    @Test
    void addRecordHeaderIncCountThrowsIllegalArgumentException() throws Exception {
        Header explodingHeader = new Header() {

            @Override
            public void incCount(int section) {
                if (section == Section.AUTHORITY) {
                    throw new IllegalArgumentException("bad section");
                }
                super.incCount(section);
            }
        };
        message.setHeader(explodingHeader);
        Name name = Name.fromConstantString("badheader.example.");
        Record aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.99"));
        int section = Section.AUTHORITY;
        assertThrows(IllegalArgumentException.class, () -> message.addRecord(aRecord, section));
    }

    @Test
    void addRecordMaximumValidSectionIndexBoundary() throws UnknownHostException {
        int section = Section.ADDITIONAL;
        Name name = Name.fromConstantString("boundary.example.");
        Record cname = new CNAMERecord(name, DClass.IN, 0, Name.fromConstantString("target.example."));
        assertEquals(0, header.getCount(section));
        assertTrue(message.getSection(section).isEmpty());
        message.addRecord(cname, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());
        assertSame(cname, message.getSection(section).get(0));
    }

    @Test
    void addRecordVerifiesHeaderIncCountInvocationWithMockito() throws UnknownHostException {
        Header spyHeader = spy(message.getHeader());
        message.setHeader(spyHeader);
        Name name = Name.fromConstantString("mockheader.example.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getLoopbackAddress());
        int section = Section.ANSWER;
        message.addRecord(aRecord, section);
        verify(spyHeader, times(1)).incCount(section);
    }

    @Test
    void addRecordMultipleTimesCumulativeHeaderCount() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a1 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        Record a2 = new AAAARecord(name, DClass.IN, 7200, InetAddress.getByName("2001:db8::1"));
        int section = Section.ANSWER;
        message.addRecord(a1, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());
        message.addRecord(a2, section);
        assertEquals(2, header.getCount(section));
        assertEquals(2, message.getSection(section).size());
        assertSame(a1, message.getSection(section).get(0));
        assertSame(a2, message.getSection(section).get(1));
    }

    @Test
    void addRecordOutOfBoundsSectionThrowsExceptionNegative() throws UnknownHostException {
        Record aRecord = new ARecord(Name.fromConstantString("outofbounds.example."), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.addRecord(aRecord, -1));
    }

    @Test
    void addRecordOutOfBoundsSectionThrowsExceptionUpper() throws UnknownHostException {
        Record aRecord = new ARecord(Name.fromConstantString("outofbounds.example."), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.addRecord(aRecord, 4));
    }
}
