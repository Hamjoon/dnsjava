package org.xbill.DNS;

import java.util.List;
import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
class Message_toWire_Test {

    /**
     * A reusable {@link DNSOutput} instance used by the tests.
     */
    private DNSOutput dnsOutput;

    /**
     * Header of the message – may be null until set.
     */
    Header header;

    /**
     * Sections of the message.  The array length is the number of
     *  sections (normally 4, but the tests use arbitrary lengths).
     */
    @SuppressWarnings("unchecked")
    List<Record>[] sections;

    /**
     * Maximum DNS message length (as defined by the DNS spec).
     */
    public static final int MAXLENGTH = 65535;

    /**
     * Stub Header – only the toWire method used by Message.
     */
    public static class Header {

        private int id = (int) (Math.random() * 0xFFFF);

        public int getID() {
            return id;
        }

        public void toWire(DNSOutput out) {
            // Very small dummy implementation – just write the ID and a few
            // zeroed fields so that the resulting byte array is parsable by
            // the real Message(byte[]) constructor used in the tests.
            // ID
            out.writeU16(id);
            // flags
            out.writeU16(0);
            // QDCOUNT
            out.writeU16(0);
            // ANCOUNT
            out.writeU16(0);
            // NSCOUNT
            out.writeU16(0);
            // ARCOUNT
            out.writeU16(0);
        }
    }

    /**
     * Stub Compression – empty placeholder (real implementation handles name
     *  compression).
     */
    public static class Compression {
        // No state needed for the tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // default size is sufficient for our tiny messages
        dnsOutput = new DNSOutput();
    }

    /**
     * Helper method to obtain the underlying byte array from a {@link DNSOutput}.
     * The {@code DNSOutput} class does not expose the array directly, but it provides
     * a {@code toByteArray()} method in the upstream library. If such a method does
     * not exist, we fall back to reflection (which is safe in test code).
     */
    private static byte[] toByteArray(DNSOutput out) {
        try {
            // The library defines a public method; use it if present.
            return (byte[]) out.getClass().getMethod("toByteArray").invoke(out);
        } catch (Exception e) {
            // Fallback: use reflection to read the private field.
            try {
                java.lang.reflect.Field f = out.getClass().getDeclaredField("array");
                f.setAccessible(true);
                byte[] arr = (byte[]) f.get(out);
                int pos = (int) out.getClass().getDeclaredField("pos").get(out);
                byte[] result = new byte[pos];
                System.arraycopy(arr, 0, result, 0, pos);
                return result;
            } catch (Exception ex) {
                throw new RuntimeException("Unable to extract byte array from DNSOutput", ex);
            }
        }
    }

    /**
     * Sets a private field on the given target object.
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
     * Adds a record to the specified section and updates the header.
     */
    public void addRecord(Record rec, int section) {
        if (rec == null) {
            throw new NullPointerException("record is null");
        }
        if (sections == null) {
            throw new NullPointerException("sections array is null");
        }
        if (section < 0 || section >= sections.length) {
            throw new IllegalArgumentException("invalid section index");
        }
        List<Record> list = sections[section];
        if (list == null) {
            list = new ArrayList<>();
            sections[section] = list;
        }
        list.add(rec);
        // The real library updates the header counters; for the tests it
        // is sufficient to leave the header unchanged.
    }

    /**
     * Replaces the current header with a new one.
     */
    public void setHeader(Header h) {
        if (h == null) {
            throw new NullPointerException("header is null");
        }
        this.header = h;
    }

    /**
     * Returns the header (used by the tests).
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Returns the records stored in a given section.
     */
    public List<Record> getSection(int i) {
        if (sections == null) {
            throw new NullPointerException("sections array is null");
        }
        if (i < 0 || i >= sections.length) {
            throw new IllegalArgumentException("invalid section index");
        }
        List<Record> list = sections[i];
        return list == null ? new ArrayList<>() : list;
    }

    /**
     * Serialises the message to the supplied DNSOutput.
     */
    public void toWire(DNSOutput out) {
        // ----- argument validation -------------------------------------------------
        if (out == null) {
            throw new NullPointerException("DNSOutput is null");
        }
        if (header == null) {
            throw new NullPointerException("Header is null");
        }
        if (sections == null) {
            throw new NullPointerException("Sections array is null");
        }
        // ----- write the header ----------------------------------------------------
        // may itself throw – let it propagate
        header.toWire(out);
        // ----- name compression is shared across all sections ----------------------
        Compression comp = new Compression();
        // ----- iterate over each section in order ---------------------------------
        for (int secIdx = 0; secIdx < sections.length; secIdx++) {
            List<Record> list = sections[secIdx];
            if (list == null) {
                // null section – nothing to serialise, continue
                continue;
            }
            for (Record r : list) {
                if (r == null) {
                    // The test suite expects a NullPointerException for a null
                    // record inside a non‑null list.
                    throw new NullPointerException("null Record in section " + secIdx);
                }
                // Record may throw; we let the exception propagate after the
                // header has already been written (as required by the tests).
                r.toWire(out, secIdx, comp);
            }
        }
    }

    /**
     * Serialises an empty {@link Message} and checks that a round‑trip via the
     * {@link Message#Message(byte[])} constructor yields an equivalent object.
     */
    @Test
    void toWire_emptyMessage_roundTrip() throws IOException {
        // header with random ID, no records
        Message empty = new Message();
        // invoke the package‑private method
        empty.toWire(dnsOutput);
        byte[] wire = dnsOutput.toByteArray();
        // The wire format must be parsable
        Message parsed = new Message(wire);
        // Header IDs should match (the random ID is stored in both objects)
        assertEquals(empty.getHeader().getID(), parsed.getHeader().getID(), "Header ID should survive a round‑trip");
        // All sections must be empty
        for (int i = 0; i < 4; i++) {
            assertTrue(parsed.getSection(i).isEmpty(), "Section " + i + " should be empty after round‑trip");
        }
        // The total size must not exceed the DNS maximum length
        assertTrue(wire.length <= Message.MAXLENGTH, "Serialized message must respect MAXLENGTH");
    }

    /**
     * Builds a message containing a question, an answer and an additional OPT record,
     * serialises it with {@code toWire}, deserialises the result and verifies that the
     * records are present in the correct sections and retain their data.
     */
    @Test
    void toWire_withRecords_roundTrip() throws Exception {
        // --- Build the original message -------------------------------------------------
        Name name = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = new Message();
        msg.addRecord(question, Section.QUESTION);
        // Answer: an A record pointing to 127.0.0.1
        ARecord aRecord = new ARecord(name, DClass.IN, 300, InetAddress.getByName("127.0.0.1"));
        msg.addRecord(aRecord, Section.ANSWER);
        // Additional: an OPT record (EDNS)
        OPTRecord opt = new OPTRecord(4096, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        // --- Serialise ---------------------------------------------------------------
        msg.toWire(dnsOutput);
        byte[] wire = dnsOutput.toByteArray();
        // --- Deserialise -------------------------------------------------------------
        Message parsed = new Message(wire);
        // Verify header ID is preserved
        assertEquals(msg.getHeader().getID(), parsed.getHeader().getID(), "Header ID should be identical after round‑trip");
        // Verify question section
        List<Record> qSec = parsed.getSection(Section.QUESTION);
        assertEquals(1, qSec.size(), "Exactly one question record expected");
        assertEquals(question.getName(), qSec.get(0).getName(), "Question name should match");
        assertEquals(question.getType(), qSec.get(0).getType(), "Question type should match");
        // Verify answer section
        List<Record> aSec = parsed.getSection(Section.ANSWER);
        assertEquals(1, aSec.size(), "Exactly one answer record expected");
        assertTrue(aSec.get(0) instanceof ARecord, "Answer should be an ARecord");
        ARecord parsedA = (ARecord) aSec.get(0);
        assertEquals(aRecord.getAddress(), parsedA.getAddress(), "ARecord address should survive round‑trip");
        // Verify additional section (OPT)
        List<Record> addSec = parsed.getSection(Section.ADDITIONAL);
        assertEquals(1, addSec.size(), "Exactly one additional record expected");
        assertTrue(addSec.get(0) instanceof OPTRecord, "Additional should be an OPTRecord");
        OPTRecord parsedOpt = (OPTRecord) addSec.get(0);
        assertEquals(opt.getPayloadSize(), parsedOpt.getPayloadSize(), "OPT payload size should be preserved");
    }

    @Test
    void toWire_allSectionsNull_callsHeaderOnly() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        // sections array of length 3, all entries null
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[3];
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        verifyNoMoreInteractions(mockHeader);
        // No Record should have been touched – there are none.
        verifyNoInteractions(mockOut);
    }

    @Test
    void toWire_nonNullSections_recordsSerializedInOrder() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        Record r0a = mock(Record.class);
        Record r0b = mock(Record.class);
        Record r1a = mock(Record.class);
        List<Record> sec0 = new ArrayList<>();
        sec0.add(r0a);
        sec0.add(r0b);
        List<Record> sec1 = new ArrayList<>();
        sec1.add(r1a);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[2];
        sections[0] = sec0;
        sections[1] = sec1;
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert header called once
        verify(mockHeader, times(1)).toWire(mockOut);
        // Capture the Compression instances passed to each Record
        ArgumentCaptor<Compression> compCaptor = ArgumentCaptor.forClass(Compression.class);
        verify(r0a).toWire(eq(mockOut), eq(0), compCaptor.capture());
        verify(r0b).toWire(eq(mockOut), eq(0), compCaptor.capture());
        verify(r1a).toWire(eq(mockOut), eq(1), compCaptor.capture());
        // All captured Compression objects must be the same instance
        List<Compression> captured = compCaptor.getAllValues();
        assertEquals(3, captured.size(), "Three Compression arguments should be captured");
        Compression first = captured.get(0);
        for (Compression c : captured) {
            assertSame(first, c, "All Record.toWire calls must receive the same Compression instance");
        }
    }

    @Test
    void toWire_mixedNullAndNonNullSections_processesOnlyExistingRecords() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        Record r1 = mock(Record.class);
        List<Record> sec1 = new ArrayList<>();
        sec1.add(r1);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[3];
        sections[0] = null;
        sections[1] = sec1;
        sections[2] = null;
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        verify(r1, times(1)).toWire(eq(mockOut), eq(1), any(Compression.class));
        // No other Record interactions
        verifyNoMoreInteractions(r1);
    }

    @Test
    void toWire_nullOutput_throwsNullPointerException() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        setPrivateField(msg, "header", mockHeader);
        // empty sections – irrelevant
        setPrivateField(msg, "sections", new List[0]);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> msg.toWire(null));
    }

    @Test
    void toWire_nullSectionsArray_throwsNullPointerException() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        setPrivateField(msg, "header", mockHeader);
        // explicitly null
        setPrivateField(msg, "sections", null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> msg.toWire(mockOut));
    }

    @Test
    void toWire_emptySectionsArray_onlyHeaderSerialized() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        setPrivateField(msg, "header", mockHeader);
        // length 0
        setPrivateField(msg, "sections", new List[0]);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        // No Compression created → no Record interactions (there are none)
    }

    @Test
    void toWire_nonNullEmptyLists_noRecordToWireCalls() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[2];
        // empty list
        sections[0] = new ArrayList<>();
        // empty list
        sections[1] = new ArrayList<>();
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        // No Record should be invoked because the lists are empty
    }

    @Test
    void toWire_thirdSectionRecord_passesCorrectSectionIndex() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        Record r2 = mock(Record.class);
        List<Record> sec2 = new ArrayList<>();
        sec2.add(r2);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[3];
        sections[0] = null;
        sections[1] = null;
        sections[2] = sec2;
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act
        msg.toWire(mockOut);
        // Assert
        verify(mockHeader, times(1)).toWire(mockOut);
        verify(r2, times(1)).toWire(eq(mockOut), eq(2), any(Compression.class));
    }

    @Test
    void toWire_recordThrowsException_propagatesUnchanged() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        Record badRec = mock(Record.class);
        doThrow(new IllegalStateException("boom")).when(badRec).toWire(any(DNSOutput.class), anyInt(), any(Compression.class));
        List<Record> sec0 = new ArrayList<>();
        sec0.add(badRec);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[2];
        sections[0] = sec0;
        sections[1] = new ArrayList<>();
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> msg.toWire(mockOut));
        assertEquals("boom", ex.getMessage());
        // Header should still have been written before the exception
        verify(mockHeader, times(1)).toWire(mockOut);
    }

    @Test
    void toWire_nullHeader_throwsNullPointerException() {
        // Arrange
        Message msg = new Message();
        // header left as null
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[1];
        sections[0] = new ArrayList<>();
        setPrivateField(msg, "sections", sections);
        DNSOutput mockOut = mock(DNSOutput.class);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> msg.toWire(mockOut));
    }

    @Test
    void toWire_headerThrowsException_propagates() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        doThrow(new IllegalStateException("header fail")).when(mockHeader).toWire(any(DNSOutput.class));
        setPrivateField(msg, "header", mockHeader);
        // empty sections
        setPrivateField(msg, "sections", new List[0]);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> msg.toWire(mockOut));
        assertEquals("header fail", ex.getMessage());
    }

    @Test
    void toWire_nullRecordInSection_throwsNullPointerException() {
        // Arrange
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        DNSOutput mockOut = mock(DNSOutput.class);
        List<Record> sec0 = new ArrayList<>();
        // deliberately insert a null Record
        sec0.add(null);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[1];
        sections[0] = sec0;
        setPrivateField(msg, "header", mockHeader);
        setPrivateField(msg, "sections", sections);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> msg.toWire(mockOut));
    }
}
