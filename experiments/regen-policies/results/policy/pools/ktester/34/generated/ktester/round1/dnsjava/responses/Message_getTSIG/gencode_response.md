```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link Message#getTSIG()}.
 *
 * <p>The tests cover the normal behaviour as well as many edge‑cases such as
 * mismatched header counts, null internal structures and type/class contract
 * violations.</p>
 */
class Message_getTSIG_Test {

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Create a minimal TSIG key – the actual algorithm/secret are irrelevant. */
    private static TSIG createTestTSIG() {
        return new TSIG("test-key.", "hmac-md5", new byte[] {0x00});
    }

    /** Create a TSIGRecord that can be added to a Message. */
    private static TSIGRecord createTestTSIGRecord() {
        Name name = Name.fromConstantString("example.com.");
        return new TSIGRecord(name, DClass.IN, 0, createTestTSIG(), 0, new byte[0]);
    }

    /** Create a generic ARecord for non‑TSIG tests. */
    private static ARecord createTestARecord() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        return new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    }

    /** Set a private field via reflection. */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** Simple Record subclass whose type can be set manually (used for ClassCast tests). */
    private static class PlainRecord extends Record {
        public PlainRecord(Name name, int type, int dclass, long ttl) {
            super(name, type, dclass, ttl);
        }

        @Override
        public String getAdditionalName() {
            return null;
        }

        @Override
        public Record getObject() {
            return null;
        }

        @Override
        public String rrToString() {
            return null;
        }

        @Override
        public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        }
    }

    /* --------------------------------------------------------------------- */
    /* Existing basic tests (kept unchanged)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void getTSIG_NoAdditionalSection_ReturnsNull() {
        Message msg = new Message();               // empty message, no records
        assertNull(msg.getTSIG(), "TSIG should be null when ADDITIONAL count is 0");
    }

    @Test
    void getTSIG_LastAdditionalNotTSIG_ReturnsNull() throws UnknownHostException {
        Message msg = new Message();
        // Add a normal A record to the ADDITIONAL section.
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);
        // Header count for ADDITIONAL is now 1, but the record is not TSIG.
        assertEquals(1, msg.getHeader().getCount(Section.ADDITIONAL));
        assertNull(msg.getTSIG(), "TSIG should be null when last ADDITIONAL record is not TSIG");
    }

    @Test
    void getTSIG_LastAdditionalIsTSIG_ReturnsRecord() throws UnknownHostException {
        Message msg = new Message();
        // First add a normal record (optional, to ensure TSIG is truly the last one).
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);
        // Then add the TSIG record as the final additional record.
        TSIGRecord tsigRec = createTestTSIGRecord();
        msg.addRecord(tsigRec, Section.ADDITIONAL);

        // Verify header count reflects both records.
        assertEquals(2, msg.getHeader().getCount(Section.ADDITIONAL));

        TSIGRecord result = msg.getTSIG();
        assertNotNull(result, "TSIG should be returned when the last ADDITIONAL record is TSIG");
        assertSame(tsigRec, result, "Returned TSIGRecord should be the one that was added");
    }

    /* --------------------------------------------------------------------- */
    /* Additional edge‑case tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Multiple additional records, TSIG not last -> returns null")
    void getTSIG_TSIGNotLast_ReturnsNull() throws Exception {
        Message msg = new Message();
        // A, TSIG, another A – TSIG is not the last record.
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);
        msg.addRecord(createTestTSIGRecord(), Section.ADDITIONAL);
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);

        assertEquals(3, msg.getHeader().getCount(Section.ADDITIONAL));
        assertNull(msg.getTSIG(), "TSIG should be null because the last additional record is not TSIG");
    }

    @Test
    @DisplayName("Header count > actual list size -> IndexOutOfBoundsException")
    void getTSIG_HeaderCountGreaterThanListSize_ThrowsIndexOutOfBounds() throws Exception {
        Message msg = new Message();

        // Prepare a sections array with a single (non‑empty) list of size 1.
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.values().length];
        List<Record> additional = new ArrayList<>();
        additional.add(createTestARecord());
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);

        // Mock Header to return count 2 (greater than list size 1).
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(2);
        setField(msg, "header", mockHeader);

        assertThrows(IndexOutOfBoundsException.class, msg::getTSIG,
                "Accessing index count‑1 should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("Header count zero but internal list non‑empty -> returns null")
    void getTSIG_HeaderCountZeroListNonEmpty_ReturnsNull() throws Exception {
        Message msg = new Message();

        // Internal list contains a record.
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.values().length];
        List<Record> additional = new ArrayList<>();
        additional.add(createTestARecord());
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);

        // Header reports zero.
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(0);
        setField(msg, "header", mockHeader);

        assertNull(msg.getTSIG(),
                "Method should ignore the list and return null when header count is 0");
    }

    @Test
    @DisplayName("Header count smaller than list size, TSIG at counted index -> returns that TSIG")
    void getTSIG_CountSmallerThanList_TSIGAtCountedIndex_ReturnsTSIG() throws Exception {
        Message msg = new Message();

        // Build a list of three records: A, TSIG, A.
        List<Record> additional = new ArrayList<>();
        additional.add(createTestARecord());          // index 0
        TSIGRecord tsig = createTestTSIGRecord();    // index 1 (count‑1)
        additional.add(tsig);
        additional.add(createTestARecord());          // index 2
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.values().length];
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);

        // Header reports count = 2 (so we look at index 1).
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(2);
        setField(msg, "header", mockHeader);

        TSIGRecord result = msg.getTSIG();
        assertNotNull(result, "TSIG should be returned from the counted position");
        assertSame(tsig, result, "Returned TSIGRecord must be the one at index count‑1");
    }

    @Test
    @DisplayName("Last element in list is null -> NullPointerException")
    void getTSIG_LastElementNull_ThrowsNPE() throws Exception {
        Message msg = new Message();

        List<Record> additional = new ArrayList<>();
        additional.add(null); // only element is null
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.values().length];
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);

        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setField(msg, "header", mockHeader);

        assertThrows(NullPointerException.class, msg::getTSIG,
                "Accessing rec.type on a null record should throw NPE");
    }

    @Test
    @DisplayName("Record type TSIG but not a TSIGRecord instance -> ClassCastException")
    void getTSIG_TypeTSIGButNotTSIGRecord_ThrowsClassCast() throws Exception {
        Message msg = new Message();

        // Create a plain Record whose type field is set to Type.TSIG.
        PlainRecord plain = new PlainRecord(Name.fromConstantString("example.com."),
                Type.TSIG, DClass.IN, 0);
        List<Record> additional = new ArrayList<>();
        additional.add(plain);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.values().length];
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);

        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setField(msg, "header", mockHeader);

        assertThrows(ClassCastException.class, msg::getTSIG,
                "Casting a non‑TSIGRecord with type TSIG should throw ClassCastException");
    }

    @Test
    @DisplayName("Header is null -> NullPointerException")
    void getTSIG_HeaderNull_ThrowsNPE() throws Exception {
        Message msg = new Message();

        // Ensure sections array is non‑null to reach the header call.
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.values().length];
        sections[Section.ADDITIONAL] = new ArrayList<>();
        setField(msg, "sections", sections);

        // Set header to null.
        setField(msg, "header", null);

        assertThrows(NullPointerException.class, msg::getTSIG,
                "Calling header.getCount on a null header should throw NPE");
    }

    @Test
    @DisplayName("Sections array is null -> NullPointerException")
    void getTSIG_SectionsArrayNull_ThrowsNPE() throws Exception {
        Message msg = new Message();

        // Header with a positive count to force access to sections.
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setField(msg, "header", mockHeader);

        // Set sections field to null.
        setField(msg, "sections", null);

        assertThrows(NullPointerException.class, msg::getTSIG,
                "Accessing a null sections array should throw NPE");
    }

    @Test
    @DisplayName("Sections array too short -> ArrayIndexOutOfBoundsException")
    void getTSIG_SectionsArrayTooShort_ThrowsAIOOBE() throws Exception {
        Message msg = new Message();

        // Create an array of length 1 (Section.ADDITIONAL is 3 in dnsjava).
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[1];
        setField(msg, "sections", sections);

        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(0);
        setField(msg, "header", mockHeader);

        assertThrows(ArrayIndexOutOfBoundsException.class, msg::getTSIG,
                "Accessing sections[Section.ADDITIONAL] with a short array should throw AIOOBE");
    }

    @Test
    @DisplayName("Additional section list is null -> NullPointerException")
    void getTSIG_AdditionalListNull_ThrowsNPE() throws Exception {
        Message msg = new Message();

        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.values().length];
        sections[Section.ADDITIONAL] = null; // explicit null entry
        setField(msg, "sections", sections);

        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setField(msg, "header", mockHeader);

        assertThrows(NullPointerException.class, msg::getTSIG,
                "Accessing a null List for the ADDITIONAL section should throw NPE");
    }

    @Test
    @DisplayName("Header reports negative count -> IndexOutOfBoundsException")
    void getTSIG_NegativeHeaderCount_ThrowsIOOBE() throws Exception {
        Message msg = new Message();

        List<Record> additional = new ArrayList<>();
        additional.add(createTestARecord());
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.values().length];
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);

        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(-2);
        setField(msg, "header", mockHeader);

        assertThrows(IndexOutOfBoundsException.class, msg::getTSIG,
                "Negative count should lead to an invalid list index and throw IOOBE");
    }

    /* --------------------------------------------------------------------- */
    /* Lifecycle methods (unchanged)                                         */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }
}
```