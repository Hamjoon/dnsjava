```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Message#getSectionRRsets(int)}.
 *
 * <p>These tests cover the following scenarios:
 *
 * <ul>
 *   <li>Empty section returns an empty list.
 *   <li>A single record creates a single RRset.
 *   <li>Multiple records with the same name, type and class are merged into one RRset.
 *   <li>Records with the same name but different type are placed in separate RRsets.
 *   <li>Records with different names are placed in separate RRsets.
 * </ul>
 *
 * The tests use only the public API of {@link Message} and related DNS classes, without accessing
 * any private members.
 */
class Message_getSectionRRsets_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
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

    /** Empty section should yield an empty list of RRsets. */
    @Test
    void emptySectionReturnsEmptyList() {
        Message msg = new Message(); // default constructor creates empty sections
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertNotNull(rrsets);
        assertTrue(rrsets.isEmpty(), "RRset list should be empty for an empty section");
    }

    /** A single record creates exactly one RRset containing that record. */
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

    /** Multiple records with the same name, type and class are merged into a single RRset. */
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
        assertTrue(rs.contains(txt1));
        assertTrue(rs.contains(txt2));
    }

    /** Records with the same name but different types produce separate RRsets. */
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

    /** Records with different names produce separate RRsets even if type and class match. */
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
}
```