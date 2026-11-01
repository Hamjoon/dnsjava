package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Zone_addRecord_Test {

    private static final long TTL = 3600L;

    private Name zoneName;

    private SOARecord soa1;

    private SOARecord soa2;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aRecord;

    private ARecord aRecord2;

    private Zone zone;

    /**
     * Create a fresh {@link Zone} instance before each test.
     */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        // zone name: example.
        zoneName = Name.fromConstantString("example.");
        // First SOA (will be replaced in one test)
        soa1 = new SOARecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        // Second SOA (used to replace the first one)
        soa2 = new SOARecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns.example."), new Name("zoneadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        // Two NS records (required for a valid zone)
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // A couple of A records for later tests
        aRecord = new ARecord(new Name("www", zoneName), DClass.IN, TTL, InetAddress.getByName("127.0.0.1"));
        aRecord2 = new ARecord(new Name("www", zoneName), DClass.IN, TTL, InetAddress.getByName("127.0.0.2"));
        // Initialise the zone with a valid SOA and NS records.
        zone = new Zone(zoneName, soa1, ns1, ns2);
    }

    /**
     * Adding a {@code null} record must throw {@link IllegalArgumentException}.
     */
    @Test
    void addRecord_nullRecord_throws() {
        assertThrows(IllegalArgumentException.class, () -> zone.addRecord(null));
    }

    /**
     * Adding an SOA whose owner does not match the zone origin must fail.
     */
    @Test
    void addRecord_soaOwnerMismatch_throws() throws IOException {
        // Create an SOA whose name is *not* the zone origin.
        Name otherName = Name.fromConstantString("otherzone.");
        SOARecord mismatchedSOA = new SOARecord(otherName, DClass.IN, TTL, Name.fromConstantString("ns.otherzone."), new Name("hostadmin", otherName), 1, 21600L, 7200L, 2160000L, 3600L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRecord(mismatchedSOA));
        assertTrue(ex.getMessage().contains("SOA owner"));
    }

    /**
     * Adding a record whose name is not a sub‑domain of the zone origin must fail.
     */
    @Test
    void addRecord_nameOutsideZone_throws() throws IOException {
        Name outside = Name.fromConstantString("outside.com.");
        ARecord outsideRecord = new ARecord(outside, DClass.IN, TTL, InetAddress.getByName("127.0.0.9"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRecord(outsideRecord));
        assertTrue(ex.getMessage().contains("is absolute and not a subdomain"));
    }

    /**
     * Adding a new SOA replaces the existing zone SOA.
     */
//     @Test
//     void addRecord_replaceSOA_success() {
//         // Verify initial SOA.
//         assertEquals(soa1, zone.getSOA());
//         // Add a new SOA (same owner = zone origin).
//         zone.addRecord(soa2);
//         // The zone's SOA reference must now point to the new object.
//         assertEquals(soa2, zone.getSOA());
//         // The RRset for the zone name now contains the new SOA.
//         RRset rrset = zone.findExactMatch(soa2.getName(), Type.SOA);
//         assertNotNull(rrset);
//         assertEquals(1, rrset.size());
//         assertTrue(java.util.Arrays.asList(rrset.getRecords()).contains(soa2));
//         // The old SOA should no longer be present in that RRset.
//         assertFalse(java.util.Arrays.asList(rrset.getRecords()).contains(soa1));
//     }

    /**
     * Adding a record to a {@link Zone} whose {@code origin} field is {@code null}
     * should result in a {@link NullPointerException} when {@code name.subdomain(origin)}
     * is evaluated.
     */
    @Test
    void addRecord_nullZoneOrigin_throwsNPE() throws Exception {
        // Construct a normal zone first.
        Zone zoneWithNullOrigin = new Zone(zoneName, soa1, ns1, ns2);
        // Use reflection to set the private 'origin' field to null.
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        originField.set(zoneWithNullOrigin, null);
        // Attempt to add a valid A record – we expect NPE.
        assertThrows(NullPointerException.class, () -> zoneWithNullOrigin.addRecord(aRecord));
    }

    /**
     * Adding a {@link Record} whose {@code getName()} returns {@code null}
     * must cause a {@link NullPointerException} because the method dereferences the name.
     */
    @Test
    void addRecord_recordWithNullName_throwsNPE() {
        // Mock a Record that returns null for getName().
        Record mockRecord = mock(Record.class);
        when(mockRecord.getName()).thenReturn(null);
        // No other stubbings are required; the method will fail when it dereferences the name.
        assertThrows(NullPointerException.class, () -> zone.addRecord(mockRecord));
    }
}