package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Message_getSectionRRsets_Test {

    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh, empty message for each test.
        message = new Message();
    }

    /**
     * Empty section must yield an empty list.
     */
    @Test
    void getSectionRRsets_emptySection_returnsEmptyList() {
        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertNotNull(rrsets, "Result should never be null");
        assertTrue(rrsets.isEmpty(), "Empty section should produce an empty list");
        // Ensure the list is immutable (as returned by Collections.emptyList())
        assertThrows(UnsupportedOperationException.class, () -> rrsets.add(new RRset()));
    }

    /**
     * Single record should produce a single RRset containing that record.
     */
    @Test
    void getSectionRRsets_singleRecord_returnsOneRRset() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        message.addRecord(a, Section.ANSWER);
        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Exactly one RRset expected");
        RRset set = rrsets.get(0);
        assertEquals(name, set.getName(), "RRset name must match the record name");
        assertEquals(Type.A, set.getType(), "RRset type must be A");
        assertEquals(DClass.IN, set.getDClass(), "RRset class must be IN");
        assertEquals(1, set.size(), "RRset should contain the single record");
        assertSame(a, set.first(), "The stored record should be the one we added");
    }

    /**
     * Multiple records with identical name, type and class must be merged into one RRset.
     */
    @Test
    void getSectionRRsets_multipleSameRRset_recordsMerged() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a1 = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        Record a2 = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.1"));
        message.addRecord(a1, Section.ANSWER);
        message.addRecord(a2, Section.ANSWER);
        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Both A records should be in the same RRset");
        RRset set = rrsets.get(0);
        assertEquals(2, set.size(), "RRset should contain both records");
        assertTrue(set.contains(a1), "First A record must be present");
        assertTrue(set.contains(a2), "Second A record must be present");
    }

    /**
     * Records that differ by type must result in distinct RRsets.
     */
    @Test
    void getSectionRRsets_differentTypes_separateRRsets() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        Record mx = new MXRecord(name, DClass.IN, 0, 10, Name.fromConstantString("mail.example.com."));
        message.addRecord(a, Section.ANSWER);
        message.addRecord(mx, Section.ANSWER);
        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "A and MX records should be in separate RRsets");
        // Verify that each RRset contains the appropriate record.
        boolean foundA = false, foundMX = false;
        for (RRset set : rrsets) {
            if (set.getType() == Type.A) {
                foundA = true;
                assertEquals(1, set.size());
                assertTrue(set.contains(a));
            } else if (set.getType() == Type.MX) {
                foundMX = true;
                assertEquals(1, set.size());
                assertTrue(set.contains(mx));
            }
        }
        assertTrue(foundA, "RRset for A records not found");
        assertTrue(foundMX, "RRset for MX records not found");
    }

    /**
     * An illegal section index must cause IllegalArgumentException.
     */
    @Test
    void getSectionRRsets_invalidSection_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> message.getSectionRRsets(invalidSection), "Section.check should reject out‑of‑range values");
    }

    /**
     * Three distinct records in AUTHORITY section should yield three RRsets.
     */
    @Test
    void getSectionRRsets_distinctRRsets_inAuthority() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        Record mx = new MXRecord(name, DClass.IN, 0, 5, Name.fromConstantString("mail.example.com."));
        Record ns = new NSRecord(name, DClass.IN, 0, Name.fromConstantString("ns1.example.com."));
        message.addRecord(a, Section.AUTHORITY);
        message.addRecord(mx, Section.AUTHORITY);
        message.addRecord(ns, Section.AUTHORITY);
        List<RRset> rrsets = message.getSectionRRsets(Section.AUTHORITY);
        assertEquals(3, rrsets.size(), "Three distinct records should produce three RRsets");
        // Verify each RRset contains exactly its own record
        for (RRset set : rrsets) {
            assertEquals(1, set.size(), "Each RRset must contain exactly one record");
            Record rec = set.first();
            assertTrue(rec.equals(a) || rec.equals(mx) || rec.equals(ns));
        }
    }

    /**
     * Mixed same and different records in ADDITIONAL section should produce two RRsets.
     */
    @Test
    void getSectionRRsets_mixedRRsets_inAdditional() throws UnknownHostException {
        // Two A records with same name + one TXT record with a different name
        Name aName = Name.fromConstantString("example.com.");
        Name txtName = Name.fromConstantString("other.com.");
        Record a1 = new ARecord(aName, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        Record a2 = new ARecord(aName, DClass.IN, 0, InetAddress.getByName("192.0.2.1"));
        Record txt = new TXTRecord(txtName, DClass.IN, 0, Collections.singletonList("hello"));
        message.addRecord(a1, Section.ADDITIONAL);
        message.addRecord(a2, Section.ADDITIONAL);
        message.addRecord(txt, Section.ADDITIONAL);
        List<RRset> rrsets = message.getSectionRRsets(Section.ADDITIONAL);
        assertEquals(2, rrsets.size(), "Should produce two RRsets (merged A's + TXT)");
        RRset aSet = null;
        RRset txtSet = null;
        for (RRset set : rrsets) {
            if (set.getType() == Type.A) {
                aSet = set;
            } else if (set.getType() == Type.TXT) {
                txtSet = set;
            }
        }
        assertNotNull(aSet, "A RRset not found");
        assertNotNull(txtSet, "TXT RRset not found");
        assertEquals(2, aSet.size(), "A RRset must contain both A records");
        assertTrue(aSet.contains(a1) && aSet.contains(a2), "Both A records should be present");
        assertEquals(1, txtSet.size(), "TXT RRset must contain the single TXT record");
        assertTrue(txtSet.contains(txt), "TXT record should be present");
    }

    /**
     * Inserting a null Record into a section should cause NullPointerException during iteration.
     */
    @Test
    void getSectionRRsets_nullRecordInSection_throwsNPE() throws Exception {
        // Use reflection to access the private 'sections' array.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(message);
        // Ensure the array is instantiated (Message constructor does this, but we guard anyway)
        if (sections == null) {
            // Force initialization via a dummy add (adds to section 0)
            message.addRecord(new ARecord(Name.fromConstantString("dummy."), DClass.IN, 0, InetAddress.getByName("127.0.0.1")), Section.QUESTION);
            sections = (List<Record>[]) sectionsField.get(message);
        }
        // Create a list containing a single null element for the QUESTION section (index 0)
        List<Record> nullList = new ArrayList<>();
        nullList.add(null);
        sections[Section.QUESTION] = nullList;
        // The method should now throw NPE when it attempts rec.sameRRset(...)
        assertThrows(NullPointerException.class, () -> message.getSectionRRsets(Section.QUESTION), "Iterating over a null Record must cause NullPointerException");
    }
}
