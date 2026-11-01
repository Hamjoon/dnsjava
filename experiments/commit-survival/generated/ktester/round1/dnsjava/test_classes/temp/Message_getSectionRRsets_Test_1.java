package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import org.mockito.Mockito;

class Message_getSectionRRsets_Test {

    /**
     * Returns {@code true} if the given {@code RRset} contains a record that
     * is {@code .equals()} to the supplied record.
     */
    private static boolean containsRecord(RRset rrset, Record record) {
        for (Record r : rrset) {
            if (r.equals(record)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the last record in the {@code RRset} according to its iterator.
     * If the set is empty, {@code null} is returned.
     */
    private static Record getLastRecord(RRset rrset) {
        Record last = null;
        for (Record r : rrset) {
            last = r;
        }
        return last;
    }

    /**
     * Empty section should yield an empty list of RRsets.
     */
    @Test
    void emptySectionReturnsEmptyList() {
        // default constructor creates empty sections
        Message msg = new Message();
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertNotNull(rrsets);
        assertTrue(rrsets.isEmpty(), "RRset list should be empty for an empty section");
    }

    /**
     * A single record creates exactly one RRset containing that record.
     */
    @Test
    void singleRecordCreatesOneRRset() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "single");
        msg.addRecord(txt, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Should have exactly one RRset");
        RRset rs = rrsets.get(0);
        assertEquals(name, rs.getName());
        assertEquals(Type.TXT, rs.getType());
        assertEquals(DClass.IN, rs.getDClass());
        assertEquals(1, rs.size());
        assertEquals(txt, rs.first());
    }

    /**
     * Multiple records with the same name, type and class are merged into a single RRset.
     */
    @Test
    void sameNameSameTypeMergedIntoOneRRset() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("merge.example.");
        Record txt1 = new TXTRecord(name, DClass.IN, 3600, "first");
        Record txt2 = new TXTRecord(name, DClass.IN, 3600, "second");
        msg.addRecord(txt1, Section.ANSWER);
        msg.addRecord(txt2, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Records with same name/type/class should be merged");
        RRset rs = rrsets.get(0);
        assertEquals(2, rs.size(), "RRset should contain both records");
        assertTrue(containsRecord(rs, txt1));
        assertTrue(containsRecord(rs, txt2));
    }

    /**
     * Records with the same name but different types produce separate RRsets.
     */
    @Test
    void sameNameDifferentTypeSeparateRRsets() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("mixed.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "text");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        msg.addRecord(txt, Section.ANSWER);
        msg.addRecord(a, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Different types should yield two RRsets");
        // Verify that each RRset contains the correct record type
        boolean foundTxt = false, foundA = false;
        for (RRset rs : rrsets) {
            if (rs.getType() == Type.TXT) {
                foundTxt = true;
                assertEquals(1, rs.size());
                assertEquals(txt, rs.first());
            } else if (rs.getType() == Type.A) {
                foundA = true;
                assertEquals(1, rs.size());
                assertEquals(a, rs.first());
            }
        }
        assertTrue(foundTxt, "TXT RRset not found");
        assertTrue(foundA, "A RRset not found");
    }

    /**
     * Records with different names produce separate RRsets even if type and class match.
     */
    @Test
    void differentNamesSeparateRRsets() throws IOException {
        Message msg = new Message();
        Name name1 = Name.fromConstantString("first.example.");
        Name name2 = Name.fromConstantString("second.example.");
        Record txt1 = new TXTRecord(name1, DClass.IN, 3600, "one");
        Record txt2 = new TXTRecord(name2, DClass.IN, 3600, "two");
        msg.addRecord(txt1, Section.ANSWER);
        msg.addRecord(txt2, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Different names should yield two RRsets");
        // Ensure each RRset corresponds to the correct name
        boolean foundFirst = false, foundSecond = false;
        for (RRset rs : rrsets) {
            if (rs.getName().equals(name1)) {
                foundFirst = true;
                assertEquals(txt1, rs.first());
            } else if (rs.getName().equals(name2)) {
                foundSecond = true;
                assertEquals(txt2, rs.first());
            }
        }
        assertTrue(foundFirst, "RRset for first.example. not found");
        assertTrue(foundSecond, "RRset for second.example. not found");
    }

    /**
     * Section index that has never been initialized (null entry) returns Collections.emptyList().
     */
    @Test
    void sectionNullHandlingReturnsEmptyList() {
        Message msg = new Message();
        // Section 0 is guaranteed to be null in a freshly constructed Message
        List<RRset> rrsets = msg.getSectionRRsets(0);
        assertSame(Collections.emptyList(), rrsets, "Should return the immutable empty list when section is null");
    }

    /**
     * Calling with a negative index throws ArrayIndexOutOfBoundsException.
     */
    @Test
    void negativeSectionIndexThrows() {
        Message msg = new Message();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> msg.getSectionRRsets(-1), "Negative section index should throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Calling with an index beyond the internal array length throws ArrayIndexOutOfBoundsException.
     */
    @Test
    void outOfBoundsSectionIndexThrows() throws Exception {
        Message msg = new Message();
        // Use reflection to discover the length of the internal sections array
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        // one past the last valid index
        int outOfBounds = sections.length;
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> msg.getSectionRRsets(outOfBounds), "Out‑of‑bounds section index should throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Empty but initialized section (empty List) yields an empty mutable list (size 0).
     */
    @Test
    void emptyInitializedSectionReturnsMutableEmptyList() throws Exception {
        Message msg = new Message();
        // Force sections[1] to be an empty ArrayList (simulating an initialized but empty section)
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        // Section 1 now non‑null but empty
        sections[1] = new ArrayList<>();
        List<RRset> rrsets = msg.getSectionRRsets(1);
        assertNotNull(rrsets);
        assertTrue(rrsets.isEmpty(), "Should return an empty list for an empty initialized section");
        // Verify the list is mutable (adding an element does not throw)
        assertDoesNotThrow(() -> rrsets.add(Mockito.mock(RRset.class)));
    }

    /**
     * Same name & type but different DClass values produce separate RRsets.
     */
    @Test
    void sameNameDifferentDClassSeparateRRsets() throws Exception {
        Message msg = new Message();
        Name name = Name.fromConstantString("dclass.example.");
        Record inRecord = new TXTRecord(name, DClass.IN, 3600, "in");
        Record chRecord = new TXTRecord(name, DClass.CH, 3600, "ch");
        msg.addRecord(inRecord, Section.ANSWER);
        msg.addRecord(chRecord, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Different DClass values should yield two RRsets");
        boolean foundIn = false, foundCh = false;
        for (RRset rs : rrsets) {
            if (rs.getDClass() == DClass.IN) {
                foundIn = true;
                assertEquals(inRecord, rs.first());
            } else if (rs.getDClass() == DClass.CH) {
                foundCh = true;
                assertEquals(chRecord, rs.first());
            }
        }
        assertTrue(foundIn, "IN DClass RRset not found");
        assertTrue(foundCh, "CH DClass RRset not found");
    }

    /**
     * Non‑consecutive matching records are merged correctly.
     */
    @Test
    void nonConsecutiveMatchingRecordMerges() throws Exception {
        Message msg = new Message();
        Name nameX = Name.fromConstantString("x.example.");
        Name nameY = Name.fromConstantString("y.example.");
        Record a1 = new ARecord(nameX, DClass.IN, 3600, InetAddress.getByName("1.1.1.1"));
        Record a2 = new ARecord(nameY, DClass.IN, 3600, InetAddress.getByName("2.2.2.2"));
        Record a3 = new ARecord(nameX, DClass.IN, 3600, InetAddress.getByName("3.3.3.3"));
        msg.addRecord(a1, Section.ANSWER);
        msg.addRecord(a2, Section.ANSWER);
        msg.addRecord(a3, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Should produce two RRsets");
        RRset rsX = null, rsY = null;
        for (RRset rs : rrsets) {
            if (rs.getName().equals(nameX)) {
                rsX = rs;
            } else if (rs.getName().equals(nameY)) {
                rsY = rs;
            }
        }
        assertNotNull(rsX, "RRset for name X missing");
        assertNotNull(rsY, "RRset for name Y missing");
        assertEquals(2, rsX.size(), "RRset X should contain both matching records");
        assertTrue(containsRecord(rsX, a1));
        assertTrue(containsRecord(rsX, a3));
        assertEquals(1, rsY.size());
        assertTrue(containsRecord(rsY, a2));
    }

    /**
     * Adding the same Record instance twice results in a RRset with duplicate entries.
     */
    @Test
    void duplicateRecordInstanceCreatesDuplicateEntry() throws Exception {
        Message msg = new Message();
        Name name = Name.fromConstantString("dup.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "dup");
        msg.addRecord(txt, Section.ANSWER);
        // same instance added again
        msg.addRecord(txt, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Should still be a single RRset");
        RRset rs = rrsets.get(0);
        assertEquals(2, rs.size(), "RRset should contain two entries (duplicates)");
        // The RRset stores the same instance twice; both positions should be equal to txt
        assertEquals(txt, rs.first());
        assertEquals(txt, getLastRecord(rs));
    }

    /**
     * Mixed types and DClasses produce the expected number of distinct RRsets.
     */
    @Test
    void mixedTypesAndDClassesComplexScenario() throws Exception {
        Message msg = new Message();
        Name nameX = Name.fromConstantString("x.example.");
        Name nameZ = Name.fromConstantString("z.example.");
        // Two IN A records
        Record a1 = new ARecord(nameX, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Record a2 = new ARecord(nameX, DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        // One CH A record
        Record a3 = new ARecord(nameX, DClass.CH, 3600, InetAddress.getByName("10.0.0.3"));
        // One IN TXT record
        Record txt = new TXTRecord(nameX, DClass.IN, 3600, "hello");
        // One IN A record with a different name
        Record a4 = new ARecord(nameZ, DClass.IN, 3600, InetAddress.getByName("10.0.0.4"));
        msg.addRecord(a1, Section.ANSWER);
        msg.addRecord(a2, Section.ANSWER);
        msg.addRecord(a3, Section.ANSWER);
        msg.addRecord(txt, Section.ANSWER);
        msg.addRecord(a4, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(3, rrsets.size(), "Expected three distinct RRsets");
        // Helper to locate an RRset
        RRset rsA_IN = null, rsA_CH = null, rsTXT = null;
        for (RRset rs : rrsets) {
            if (rs.getName().equals(nameX) && rs.getType() == Type.A && rs.getDClass() == DClass.IN) {
                rsA_IN = rs;
            } else if (rs.getName().equals(nameX) && rs.getType() == Type.A && rs.getDClass() == DClass.CH) {
                rsA_CH = rs;
            } else if (rs.getName().equals(nameX) && rs.getType() == Type.TXT && rs.getDClass() == DClass.IN) {
                rsTXT = rs;
            }
        }
        assertNotNull(rsA_IN, "IN A RRset missing");
        assertNotNull(rsA_CH, "CH A RRset missing");
        assertNotNull(rsTXT, "IN TXT RRset missing");
        assertEquals(2, rsA_IN.size(), "IN A RRset should contain two records");
        assertTrue(containsRecord(rsA_IN, a1));
        assertTrue(containsRecord(rsA_IN, a2));
        assertEquals(1, rsA_CH.size());
        assertTrue(containsRecord(rsA_CH, a3));
        assertEquals(1, rsTXT.size());
        assertTrue(containsRecord(rsTXT, txt));
    }

    /**
     * Adding a null Record to a section causes NullPointerException during processing.
     */
    @Test
    void nullRecordHandlingThrowsNPE() throws Exception {
        Message msg = new Message();
        // Force a null entry into the list for section 6
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        sections[6] = new ArrayList<>();
        // insert null record
        sections[6].add(null);
        assertThrows(NullPointerException.class, () -> msg.getSectionRRsets(6), "Iterating over a null record should throw NullPointerException");
    }

    /**
     * A Record whose getName() returns null triggers NullPointerException.
     */
    @Test
    void recordWithNullNameThrowsNPE() throws Exception {
        Message msg = new Message();
        // Create a mock Record that returns null for getName()
        Record mock = Mockito.mock(Record.class);
        Mockito.when(mock.getName()).thenReturn(null);
        // other required methods to avoid additional NPEs
        Mockito.when(mock.getRRsetType()).thenReturn(Type.A);
        Mockito.when(mock.getDClass()).thenReturn(DClass.IN);
        // Insert the mock into an initialized section via reflection
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        sections[7] = new ArrayList<>();
        sections[7].add(mock);
        assertThrows(NullPointerException.class, () -> msg.getSectionRRsets(7), "Record with null name should cause NullPointerException");
    }

    /**
     * If the internal sections array itself is null, any call results in NullPointerException.
     */
    @Test
    void internalSectionsArrayNullThrowsNPE() throws Exception {
        Message msg = new Message();
        // Set the private 'sections' field to null via reflection
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, null);
        assertThrows(NullPointerException.class, () -> msg.getSectionRRsets(0), "Null internal sections array should cause NullPointerException");
    }
}
