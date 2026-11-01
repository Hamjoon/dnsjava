package org.xbill.DNS;

// @ExtendWith(MockitoExtension.class)
class Message_removeRecord_Test {

    private Message message;

    private Record aRecord;

    private Record txtRecord;

    /**
     * The four DNS sections (question, answer, authority, additional).
     */
    @SuppressWarnings("unchecked")
//     private List<Record>[] sections = new List[Section.SECTION_COUNT];

    /**
     * Header that stores the per‑section record counts.
     */
    private Header header = new Header();

    /**
     * Simple representation of a DNS record – only the fields required by the tests.
     */
    class Record {

        private final Name name;

        private final int type;

        private final int dclass;

        private Record(Name name, int type, int dclass) {
            this.name = name;
            this.type = type;
            this.dclass = dclass;
        }

        public static Record newRecord(Name name, int type, int dclass) {
            return new Record(name, type, dclass);
        }
        // equals / hashCode are not required for the current tests – identity semantics are sufficient.
    }

    /**
     * Minimal DNS name holder.
     */
    class Name {

        private final String name;

        private Name(String name) {
            this.name = name;
        }

        public static Name fromConstantString(String s) {
            return new Name(s);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Header that tracks the number of records per section.
     */
    class Header {

        private final int[] counts = new int[Section.SECTION_COUNT];

        /**
         * Returns the current count for {@code section}.
         */
        public int getCount(int section) {
            Section.check(section);
            return counts[section];
        }

        /**
         * Increments the count for {@code section}.
         */
        public void incCount(int section) {
            Section.check(section);
            counts[section]++;
        }

        /**
         * Decrements the count for {@code section}.
         */
        public void decCount(int section) {
            Section.check(section);
            if (counts[section] == 0) {
                throw new IllegalArgumentException("count underflow");
            }
            counts[section]--;
        }
    }

    /**
     * Section constants and validation logic.
     */
    class Section {

        public static final int QUESTION = 0;

        public static final int ANSWER = 1;

        public static final int AUTHORITY = 2;

        public static final int ADDITIONAL = 3;

        public static final int SECTION_COUNT = 4;

        /**
         * Validates that {@code section} is within the legal range.
         */
        public static void check(int section) {
            if (section < 0 || section >= SECTION_COUNT) {
                throw new IllegalArgumentException("Invalid section: " + section);
            }
        }
    }

    /**
     * Placeholder for DNS type constants – values are irrelevant for the tests.
     */
    class Type {

        public static final int A = 1;

        public static final int TXT = 16;
    }

    /**
     * Placeholder for DNS class constants – values are irrelevant for the tests.
     */
    class DClass {

        public static final int IN = 1;
    }

//     @BeforeEach
    void setupBeforeEach() {
        // fresh Message for each test
        message = new Message();
        // generic records: A and TXT types for the same name
        aRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
        txtRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.TXT, DClass.IN);
    }

//     @AfterEach
    void teardownAfterEach() {
        message = null;
        aRecord = null;
        txtRecord = null;
    }

    /**
     * Replace the internal {@code sections} array via reflection.
     */
//     private void setSectionsArray(List<Record>[] sections) throws Exception {
//         Field f = Message.class.getDeclaredField("sections");
//         f.setAccessible(true);
//         f.set(message, sections);
//     }

    /**
     * Replace the internal {@code header} field via reflection.
     */
    private void setHeader(Header hdr) throws Exception {
        this.header = Objects.requireNonNull(hdr);
    }

    /**
     * Returns the internal {@link Header}.
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Returns the list of records for the supplied section (may be empty).
     */
//     public List<Record> getSection(int section) {
//         Section.check(section);
//         List<Record> list = sections[section];
//         return list == null ? Collections.emptyList() : list;
//     }

    /**
     * Adds a record to the given section and updates the header count.
     */
    public void addRecord(Record r, int section) {
        Section.check(section);
        Objects.requireNonNull(r);
        List<Record> list = sections[section];
        if (list == null) {
            list = new ArrayList<>();
            sections[section] = list;
        }
        list.add(r);
        header.incCount(section);
    }

    /**
     * Removes {@code r} from {@code section}.  If the record is found it is
     * removed, the header count is decremented and {@code true} is returned.
     * If the section is {@code null} or the record is not present {@code false}
     * is returned.  Any exception from {@link Header#decCount(int)} is allowed
     * to propagate.
     */
    public boolean removeRecord(Record r, int section) {
        Section.check(section);
        // sections array must exist
        if (sections == null) {
            throw new NullPointerException("sections array is null");
        }
        List<Record> list = sections[section];
        // a null list means the section has never been initialised
        if (list == null) {
            return false;
        }
        // attempt removal – List.remove(Object) removes the first occurrence
        boolean removed = list.remove(r);
        if (removed) {
            // header must exist
            if (header == null) {
                throw new NullPointerException("header is null");
            }
            header.decCount(section);
        }
        return removed;
    }

//     @Test
//     void removeRecord_successfulRemoval_decrementsHeaderCount() {
//         message.addRecord(aRecord, Section.ANSWER);
//         Header header = message.getHeader();
//         assertEquals(1, header.getCount(Section.ANSWER), "Header count should be 1 after adding the record");
//         boolean removed = message.removeRecord(aRecord, Section.ANSWER);
//         assertTrue(removed, "removeRecord should return true when the record exists");
//         assertEquals(0, header.getCount(Section.ANSWER), "Header count should be decremented after removal");
//         assertTrue(message.getSection(Section.ANSWER).isEmpty(), "Section should be empty after the record is removed");
//     }

//     @Test
//     void removeRecord_recordNotPresent_returnsFalseAndHeaderUnchanged() {
//         message.addRecord(aRecord, Section.ANSWER);
//         Header header = message.getHeader();
//         assertEquals(1, header.getCount(Section.ANSWER));
//         boolean removed = message.removeRecord(txtRecord, Section.ANSWER);
//         assertFalse(removed, "removeRecord should return false when the record is absent");
//         assertEquals(1, header.getCount(Section.ANSWER), "Header count must remain unchanged when removal fails");
//         assertEquals(1, message.getSection(Section.ANSWER).size(), "The original record should still be present in the section");
//         assertSame(aRecord, message.getSection(Section.ANSWER).get(0));
//     }

//     @Test
//     void removeRecord_sectionNeverInitialized_returnsFalse() {
//         // no records added – sections are still null
//         boolean removed = message.removeRecord(aRecord, Section.AUTHORITY);
//         assertFalse(removed, "removeRecord must return false when the section list is null");
//         assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
//     }

//     @Test
//     void removeRecord_invalidSection_throwsIllegalArgumentException() {
//         message.addRecord(aRecord, Section.ADDITIONAL);
//         assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, 5), "Section.check should reject an out‑of‑range section index");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_invalidUpperBoundSection_throwsIllegalArgumentException() {
//         assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, 5));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_emptySectionList_returnsFalse() throws Exception {
//         List<Record>[] sections = new List[4];
//         sections[Section.ANSWER] = new ArrayList<>();
//         setSectionsArray(sections);
//         boolean removed = message.removeRecord(aRecord, Section.ANSWER);
//         assertFalse(removed);
//         assertEquals(0, message.getHeader().getCount(Section.ANSWER));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_duplicateRecord_removesOneAndDecrementsHeaderOnce() throws Exception {
//         Record dup = mock(Record.class);
//         List<Record> list = new ArrayList<>(Arrays.asList(dup, dup));
//         List<Record>[] sections = new List[4];
//         sections[Section.AUTHORITY] = list;
//         setSectionsArray(sections);
//         Header mockHeader = mock(Header.class);
//         when(mockHeader.getCount(Section.AUTHORITY)).thenReturn(2);
//         setHeader(mockHeader);
//         boolean removed = message.removeRecord(dup, Section.AUTHORITY);
//         assertTrue(removed);
//         assertEquals(1, list.size());
//         verify(mockHeader, times(1)).decCount(Section.AUTHORITY);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_nullRecordInList_successfulRemoval() throws Exception {
//         List<Record> list = new ArrayList<>(Collections.singletonList(null));
//         List<Record>[] sections = new List[4];
//         sections[Section.ADDITIONAL] = list;
//         setSectionsArray(sections);
//         Header mockHeader = mock(Header.class);
//         setHeader(mockHeader);
//         boolean removed = message.removeRecord(null, Section.ADDITIONAL);
//         assertTrue(removed);
//         assertTrue(list.isEmpty());
//         verify(mockHeader, times(1)).decCount(Section.ADDITIONAL);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_immutableSectionList_throwsUnsupportedOperationException() throws Exception {
//         List<Record>[] sections = new List[4];
//         // immutable
//         sections[Section.QUESTION] = Collections.emptyList();
//         setSectionsArray(sections);
//         assertThrows(UnsupportedOperationException.class, () -> message.removeRecord(aRecord, Section.QUESTION));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_nullSectionsArray_throwsNullPointerException() throws Exception {
//         setSectionsArray(null);
//         assertThrows(NullPointerException.class, () -> message.removeRecord(aRecord, Section.ANSWER));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_nullHeader_throwsNullPointerException() throws Exception {
//         List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
//         List<Record>[] sections = new List[4];
//         sections[Section.AUTHORITY] = list;
//         setSectionsArray(sections);
//         setHeader(null);
//         assertThrows(NullPointerException.class, () -> message.removeRecord(aRecord, Section.AUTHORITY));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_headerDecCountThrows_IllegalArgumentException() throws Exception {
//         List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
//         List<Record>[] sections = new List[4];
//         sections[Section.ANSWER] = list;
//         setSectionsArray(sections);
//         Header mockHeader = mock(Header.class);
//         doThrow(new IllegalArgumentException("count underflow")).when(mockHeader).decCount(Section.ANSWER);
//         setHeader(mockHeader);
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, Section.ANSWER));
//         assertEquals("count underflow", ex.getMessage());
//         assertTrue(list.isEmpty(), "record should have been removed before decCount failed");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
//         // valid indices 0,1
//         List<Record>[] shortArray = new List[2];
//         setSectionsArray(shortArray);
//         assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.removeRecord(aRecord, 3));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_sectionListWithoutRecord_returnsFalseAndNoHeaderDec() throws Exception {
//         Record present = mock(Record.class, "presentRecord");
//         Record absent = mock(Record.class, "absentRecord");
//         List<Record> list = new ArrayList<>(Collections.singletonList(present));
//         List<Record>[] sections = new List[4];
//         sections[Section.ANSWER] = list;
//         setSectionsArray(sections);
//         Header mockHeader = mock(Header.class);
//         setHeader(mockHeader);
//         boolean removed = message.removeRecord(absent, Section.ANSWER);
//         assertFalse(removed);
//         verify(mockHeader, never()).decCount(anyInt());
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_sectionContainsRecord_returnsTrueAndCallsHeader() throws Exception {
//         List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
//         List<Record>[] sections = new List[4];
//         sections[Section.ADDITIONAL] = list;
//         setSectionsArray(sections);
//         Header mockHeader = mock(Header.class);
//         setHeader(mockHeader);
//         boolean removed = message.removeRecord(aRecord, Section.ADDITIONAL);
//         assertTrue(removed);
//         assertTrue(list.isEmpty());
//         verify(mockHeader, times(1)).decCount(Section.ADDITIONAL);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_negativeSection_throwsIllegalArgumentException() {
//         assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, -1));
//     }
}