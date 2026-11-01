```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional tests for {@link Zone#removeRecord(Record)} that cover edge‑cases
 * described in the JSON test specification.
 *
 * <p>The original three scenarios (single‑record removal, multi‑record removal,
 * and removal of a non‑existent record) are already present in the base test
 * class.  The tests below focus on:</p>
 * <ul>
 *   <li>RRset not found – the method should exit silently.</li>
 *   <li>Null argument – a {@link NullPointerException} is expected.</li>
 *   <li>Record with a {@code null} name – also a {@link NullPointerException}.</li>
 *   <li>Record not present in an existing RRset of size &gt; 1 – {@link NoSuchElementException}.</li>
 *   <li>Record not present in an existing RRset of size = 1 – {@link NoSuchElementException}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Zone_removeRecord_EdgeCases_Test {

    /* ---------- static fixtures (shared across all tests) ---------- */
    private static Name ZONE_NAME;
    private static ARecord A_RECORD;
    private static AAAARecord AAAA1_RECORD;
    private static AAAARecord AAAA2_RECORD;
    private static SOARecord SOA_RECORD;
    private static NSRecord NS_RECORD;

    /* ---------- per‑test fixture ---------- */
    private Zone zone; // a fresh Zone for each test

    /** Creates a fresh {@link Zone} instance populated with the static records. */
    private Zone createZone() throws IOException {
        Record[] records = new Record[] {
                SOA_RECORD,
                NS_RECORD,
                A_RECORD,
                AAAA1_RECORD,
                AAAA2_RECORD
        };
        return new Zone(ZONE_NAME, records);
    }

    /* ---------- JUnit lifecycle ---------- */
    @BeforeAll
    static void setupBeforeAll() throws Exception {
        ZONE_NAME = new Name("example.");

        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");

        A_RECORD = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA1_RECORD = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA2_RECORD = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);

        SOA_RECORD = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostmaster", ZONE_NAME),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);
        NS_RECORD = new NSRecord(ZONE_NAME, DClass.IN, 300L,
                Name.fromConstantString("ns.example."));
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        zone = createZone();
    }

    @AfterEach
    void teardownAfterEach() {
        zone = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        ZONE_NAME = null;
        A_RECORD = null;
        AAAA1_RECORD = null;
        AAAA2_RECORD = null;
        SOA_RECORD = null;
        NS_RECORD = null;
    }

    /* ---------- existing baseline tests (unchanged) ---------- */

    @Test
    void removeSingleRecord_RemovesEntireRRset() {
        RRset before = zone.findExactMatch(A_RECORD.getName(), Type.A);
        assertNotNull(before);
        assertEquals(1, before.size());

        zone.removeRecord(A_RECORD);

        RRset after = zone.findExactMatch(A_RECORD.getName(), Type.A);
        assertNull(after);
    }

    @Test
    void removeRecordFromMultiRecordRRset_LeavesRemainingRecords() {
        RRset before = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);
        assertNotNull(before);
        assertEquals(2, before.size());

        zone.removeRecord(AAAA1_RECORD);

        RRset after = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);
        assertNotNull(after);
        assertEquals(1, after.size());
        assertEquals(AAAA2_RECORD, after.first());
    }

    @Test
    void removeNonExistingRecord_DoesNotAlterZone() throws IOException {
        InetAddress other = InetAddress.getByName("192.0.2.1");
        ARecord notInZone = new ARecord(new Name("absent", ZONE_NAME), DClass.IN, 3600, other);

        RRset aSetBefore = zone.findExactMatch(A_RECORD.getName(), Type.A);
        RRset aaaaSetBefore = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);

        zone.removeRecord(notInZone);

        RRset aSetAfter = zone.findExactMatch(A_RECORD.getName(), Type.A);
        RRset aaaaSetAfter = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);

        assertEquals(aSetBefore, aSetAfter);
        assertEquals(aaaaSetBefore, aaaaSetAfter);
    }

    /* ---------- edge‑case tests from the JSON specification ---------- */

    @Test
    void removeRecord_RRsetNotFound_ZoneUnchanged() throws IOException {
        // Record type MX does not exist in the fixture zone.
        MXRecord mx = new MXRecord(new Name("mail", ZONE_NAME), DClass.IN, 3600, 10,
                new Name("mailhost", ZONE_NAME));

        // Capture a representation of the zone before the call.
        String before = zone.toMasterFile();

        zone.removeRecord(mx); // should simply return

        // The zone's textual representation must be identical.
        String after = zone.toMasterFile();
        assertEquals(before, after, "Zone must remain unchanged when RRset is not found");
    }

    @Test
    void removeRecord_NullArgument_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> zone.removeRecord(null),
                "Calling removeRecord(null) should throw NullPointerException");
    }

    @Test
    void removeRecord_RecordWithNullName_ThrowsNullPointerException() {
        // Mock a Record whose getName() returns null.
        Record mockRecord = mock(Record.class);
        when(mockRecord.getName()).thenReturn(null);
        // The other methods are irrelevant for this test, but we need getRRsetType()
        // to avoid additional NPEs inside the method.
        when(mockRecord.getRRsetType()).thenReturn(Type.A);

        assertThrows(NullPointerException.class, () -> zone.removeRecord(mockRecord),
                "Record with null name should cause NullPointerException");
    }

    @Test
    void removeRecord_NotInRRset_MultiSize_ThrowsNoSuchElementException() {
        // The zone already contains two AAAA records for the same name.
        // Create a third AAAA record with a different address – not present in the RRset.
        InetAddress other = InetAddress.getByName("2001:db8::dead");
        AAAARecord notInRRset = new AAAARecord(AAAA1_RECORD.getName(), DClass.IN, 3600, other);

        // Verify pre‑condition.
        RRset rrset = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);
        assertNotNull(rrset);
        assertEquals(2, rrset.size());

        // The delete operation should throw NoSuchElementException.
        assertThrows(NoSuchElementException.class, () -> zone.removeRecord(notInRRset),
                "Removing a record not present in a multi‑record RRset must throw NoSuchElementException");
    }

    @Test
    void removeRecord_NotInRRset_SingleSize_ThrowsNoSuchElementException() {
        // The zone contains a single A record.
        // Create another A record with same name but different address.
        InetAddress other = InetAddress.getByName("192.0.2.123");
        ARecord notInRRset = new ARecord(A_RECORD.getName(), DClass.IN, 3600, other);

        // Verify pre‑condition.
        RRset rrset = zone.findExactMatch(A_RECORD.getName(), Type.A);
        assertNotNull(rrset);
        assertEquals(1, rrset.size());

        // Expect NoSuchElementException from deleteRR.
        assertThrows(NoSuchElementException.class, () -> zone.removeRecord(notInRRset),
                "Removing a record not present in a single‑record RRset must throw NoSuchElementException");
    }
}
```