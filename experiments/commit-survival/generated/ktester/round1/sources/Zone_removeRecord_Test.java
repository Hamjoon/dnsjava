package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.NoSuchElementException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Zone_removeRecord_Test {

    /* ---------- static fixtures (shared across all tests) ---------- */
    private static Name ZONE_NAME;

    private static ARecord A_RECORD;

    private static AAAARecord AAAA1_RECORD;

    private static AAAARecord AAAA2_RECORD;

    private static SOARecord SOA_RECORD;

    private static NSRecord NS_RECORD;

    // a fresh Zone for each test
    private Zone zone;

    /* ---------- JUnit lifecycle ---------- */
    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // zone name "example."
        ZONE_NAME = new Name("example.");
        // common records used in every test
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");
        A_RECORD = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA1_RECORD = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA2_RECORD = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
        // minimal SOA and NS records – required for a valid Zone
        SOA_RECORD = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster", ZONE_NAME), 1, 21600L, 7200L, 2160000L, 3600L);
        NS_RECORD = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns.example."));
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        zone = createZone();
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up.
        zone = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nullify static references for GC friendliness.
        ZONE_NAME = null;
        A_RECORD = null;
        AAAA1_RECORD = null;
        AAAA2_RECORD = null;
        SOA_RECORD = null;
        NS_RECORD = null;
    }

    /**
     * Creates a fresh {@link Zone} instance populated with the static records.
     */
    private Zone createZone() throws IOException {
        Record[] records = new Record[] { SOA_RECORD, NS_RECORD, A_RECORD, AAAA1_RECORD, AAAA2_RECORD };
        return new Zone(ZONE_NAME, records);
    }

    @Test
    void removeSingleRecord_RemovesEntireRRset() {
        // Pre‑condition: the A record exists as a sole member of its RRset.
        RRset before = zone.findExactMatch(A_RECORD.getName(), Type.A);
        assertNotNull(before, "A RRset should exist before removal");
        assertEquals(1, before.size());
        // Act
        zone.removeRecord(A_RECORD);
        // Post‑condition: the RRset is gone.
        RRset after = zone.findExactMatch(A_RECORD.getName(), Type.A);
        assertNull(after, "RRset should be removed after deleting its only record");
    }

    @Test
    void removeRecordFromMultiRecordRRset_LeavesRemainingRecords() {
        // Pre‑condition: two AAAA records share the same name.
        RRset before = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);
        assertNotNull(before, "AAAA RRset should exist before removal");
        assertEquals(2, before.size());
        // Act – delete the first AAAA record.
        zone.removeRecord(AAAA1_RECORD);
        // Post‑condition: RRset still exists but contains only the second record.
        RRset after = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);
        assertNotNull(after, "RRset must still exist after removing one of multiple records");
        assertEquals(1, after.size());
        // Verify that the remaining record is AAAA2_RECORD.
        Record remaining = after.first();
        assertEquals(AAAA2_RECORD, remaining, "The remaining record should be the second AAAA entry");
    }

    @Test
    void removeNonExistingRecord_DoesNotAlterZone() throws IOException {
        // Create a record that was never added to the zone.
        InetAddress other = InetAddress.getByName("192.0.2.1");
        ARecord notInZone = new ARecord(new Name("absent", ZONE_NAME), DClass.IN, 3600, other);
        // Capture state before removal attempt.
        RRset aSetBefore = zone.findExactMatch(A_RECORD.getName(), Type.A);
        RRset aaaaSetBefore = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);
        // Act – attempt to remove a record that does not exist.
        zone.removeRecord(notInZone);
        // Verify that existing RRsets are unchanged.
        RRset aSetAfter = zone.findExactMatch(A_RECORD.getName(), Type.A);
        RRset aaaaSetAfter = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);
        assertEquals(aSetBefore, aSetAfter, "A RRset must remain unchanged");
        assertEquals(aaaaSetBefore, aaaaSetAfter, "AAAA RRset must remain unchanged");
    }

    @Test
    void removeRecord_RRsetNotFound_ZoneUnchanged() throws IOException {
        // Record type MX does not exist in the fixture zone.
        MXRecord mx = new MXRecord(new Name("mail", ZONE_NAME), DClass.IN, 3600, 10, new Name("mailhost", ZONE_NAME));
        // Capture a representation of the zone before the call.
        String before = zone.toMasterFile();
        // should simply return
        zone.removeRecord(mx);
        // The zone's textual representation must be identical.
        String after = zone.toMasterFile();
        assertEquals(before, after, "Zone must remain unchanged when RRset is not found");
    }

    @Test
    void removeRecord_NullArgument_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> zone.removeRecord(null), "Calling removeRecord(null) should throw NullPointerException");
    }

    @Test
    void removeRecord_RecordWithNullName_ThrowsNullPointerException() {
        // Mock a Record whose getName() returns null.
        Record mockRecord = mock(Record.class);
        when(mockRecord.getName()).thenReturn(null);
        // The other methods are irrelevant for this test, but we need getRRsetType()
        // to avoid additional NPEs inside the method.
        when(mockRecord.getRRsetType()).thenReturn(Type.A);
        assertThrows(NullPointerException.class, () -> zone.removeRecord(mockRecord), "Record with null name should cause NullPointerException");
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
        assertThrows(NoSuchElementException.class, () -> zone.removeRecord(notInRRset), "Removing a record not present in a multi‑record RRset must throw NoSuchElementException");
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
        assertThrows(NoSuchElementException.class, () -> zone.removeRecord(notInRRset), "Removing a record not present in a single‑record RRset must throw NoSuchElementException");
    }
}
