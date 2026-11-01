```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Message#getSectionRRsets(int)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>Returning an empty list when the requested section has no records.
 *   <li>Creating a single {@link RRset} for a single record.
 *   <li>Aggregating multiple records that belong to the same RRset.
 *   <li>Returning distinct RRsets for records that differ by name, type or class.
 * </ul>
 *
 * No private members of {@link Message} or its collaborators are accessed directly.
 */
class Message_getSectionRRsets_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Start each test with a fresh, empty Message.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /** Empty section must yield an immutable empty list. */
    @Test
    void emptySectionReturnsEmptyList() {
        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertNotNull(rrsets, "Returned list must not be null");
        assertTrue(rrsets.isEmpty(), "Empty section should produce an empty list");
    }

    /** A single record creates a single RRset containing that record. */
    @Test
    void singleRecordCreatesOneRRset() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        message.addRecord(a, Section.ANSWER);

        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Exactly one RRset expected");
        RRset set = rrsets.get(0);
        assertEquals(1, set.size(), "RRset should contain the single record");
        assertTrue(set.contains(a), "RRset must contain the added record");
    }

    /** Multiple records that belong to the same RRset are aggregated. */
    @Test
    void multipleSameRRsetRecordsAggregated() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a1 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        Record a2 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        message.addRecord(a1, Section.ANSWER);
        message.addRecord(a2, Section.ANSWER);

        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Records of the same name/type/class must be in one RRset");
        RRset set = rrsets.get(0);
        assertEquals(2, set.size(), "RRset should contain both A records");
        assertTrue(set.contains(a1) && set.contains(a2), "Both records must be present");
    }

    /** Records that differ by type (or name) produce distinct RRsets. */
    @Test
    void differentRecordsCreateSeparateRRsets() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        Record mx = new MXRecord(name, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com."));
        message.addRecord(a, Section.ANSWER);
        message.addRecord(mx, Section.ANSWER);

        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Different types should result in two RRsets");

        // Verify that each RRset contains exactly its own record.
        boolean aFound = false, mxFound = false;
        for (RRset set : rrsets) {
            if (set.getType() == Type.A) {
                aFound = true;
                assertEquals(1, set.size());
                assertTrue(set.contains(a));
            } else if (set.getType() == Type.MX) {
                mxFound = true;
                assertEquals(1, set.size());
                assertTrue(set.contains(mx));
            }
        }
        assertTrue(aFound && mxFound, "Both A and MX RRsets must be present");
    }

    /** Section index validation – passing an invalid section should throw IllegalArgumentException. */
    @Test
    void invalidSectionThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> message.getSectionRRsets(4),
                "Section index outside 0‑3 must cause IllegalArgumentException");
    }
}
```