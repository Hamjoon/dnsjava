package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Iterator;

@ExtendWith(MockitoExtension.class)
class Zone_addRecord_Test {

    /* --------------------------------------------------------------------- */
    private static Name ZONE_NAME;

    private static SOARecord INITIAL_SOA;

    private static NSRecord NS1;

    private static NSRecord NS2;

    /* --------------------------------------------------------------------- */
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Zone name "example."
        ZONE_NAME = Name.fromConstantString("example.");
        // Minimal SOA record – required for any zone
        INITIAL_SOA = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster", ZONE_NAME), // serial
        1, // refresh
        7200L, // retry
        3600L, // expire
        1209600L, // minimum
        3600L);
        // Two NS records – also required for a valid zone
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Construct a zone that already contains a valid SOA and two NS records.
        // No other records are added – each test can start from this clean state.
        Record[] initial = new Record[] { INITIAL_SOA, NS1, NS2 };
        zone = new Zone(ZONE_NAME, initial);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to release; method kept for symmetry with template.
        zone = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up at class level.
        ZONE_NAME = null;
        INITIAL_SOA = null;
        NS1 = null;
        NS2 = null;
    }

    /**
     * Helper that creates a new {@link Zone} with the supplied initial records.
     */
    private Zone newZone(Record... initial) throws IOException {
        return new Zone(ZONE_NAME, initial);
    }

    @Test
    void addRecord_nullRecord_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> zone.addRecord(null));
    }

    @Test
    void addRecord_SOAOwnerNotZoneOrigin_throwsIllegalArgumentException() throws IOException {
        // Create an SOA whose name is *not* the zone origin.
        SOARecord foreignSOA = new SOARecord(Name.fromConstantString("otherzone."), DClass.IN, 3600L, Name.fromConstantString("ns.otherzone."), new Name("hostmaster", Name.fromConstantString("otherzone.")), 2, 7200L, 3600L, 1209600L, 3600L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRecord(foreignSOA));
        assertTrue(ex.getMessage().contains("SOA owner"));
    }

    @Test
    void addRecord_NameOutsideZone_throwsIllegalArgumentException() throws IOException {
        // Name "outside.example.com." is not a subdomain of "example."
        Name outside = new Name("outside.example.com.", ZONE_NAME);
        ARecord a = new ARecord(outside, DClass.IN, 300L, InetAddress.getByName("192.0.2.1"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRecord(a));
        assertTrue(ex.getMessage().contains("is absolute and not a subdomain"));
    }

    @Test
    void addRecord_NewName_createsNewRRset() throws IOException {
        Name newName = new Name("newhost", ZONE_NAME);
        ARecord a = new ARecord(newName, DClass.IN, 300L, InetAddress.getByName("192.0.2.42"));
        assertDoesNotThrow(() -> zone.addRecord(a));
        // The newly added A record should be retrievable via findExactMatch
        RRset rrset = zone.findExactMatch(newName, Type.A);
        assertNotNull(rrset);
        assertEquals(1, rrset.size());
        assertEquals(a, rrset.first());
    }

    @Test
    void addRecord_ExistingName_addsToSameRRset() throws IOException {
        // First add an A record for "dup.example."
        Name dupName = new Name("dup", ZONE_NAME);
        ARecord a1 = new ARecord(dupName, DClass.IN, 300L, InetAddress.getByName("192.0.2.10"));
        zone.addRecord(a1);
        // Add a second A record for the same name (different address)
        ARecord a2 = new ARecord(dupName, DClass.IN, 300L, InetAddress.getByName("192.0.2.11"));
        zone.addRecord(a2);
        RRset rrset = zone.findExactMatch(dupName, Type.A);
        assertNotNull(rrset);
        assertEquals(2, rrset.size());
        // Both records must be present (order is not guaranteed)
        assertTrue(rrset.contains(a1));
        assertTrue(rrset.contains(a2));
    }

    @Test
    void addRecord_ReplaceSOA_updatesZoneSOA() throws IOException {
        // Create a new SOA with the same owner (zone origin) but different serial
        SOARecord newSOA = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster", ZONE_NAME), // new serial
        99, 7200L, 3600L, 1209600L, 3600L);
        // Adding the new SOA should replace the existing one
        zone.addRecord(newSOA);
        // The zone's cached SOA reference must now be the new instance
        assertEquals(newSOA, zone.getSOA());
        // The RRset for the SOA type should also contain only the new record
        RRset soaRRset = zone.findExactMatch(ZONE_NAME, Type.SOA);
        assertNotNull(soaRRset);
        assertEquals(1, soaRRset.size());
        assertEquals(newSOA, soaRRset.first());
    }

    @Test
    void addRecord_FirstSOAAddition_createsSOARRsetAndUpdatesSoaField() throws IOException {
        // Create a zone without any records.
        Zone emptyZone = newZone();
        // Build a brand‑new SOA that matches the zone origin.
        SOARecord newSOA = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster", ZONE_NAME), 2023100101L, 3600L, 600L, 604800L, 86400L);
        // Adding the first SOA must succeed and set the internal field.
        assertDoesNotThrow(() -> emptyZone.addRecord(newSOA));
        // Verify the internal soaRecord reference.
        assertEquals(newSOA, emptyZone.getSOA());
        // Verify the RRset for the apex SOA.
        RRset soaRRset = emptyZone.findExactMatch(ZONE_NAME, Type.SOA);
        assertNotNull(soaRRset);
        assertEquals(1, soaRRset.size());
        assertEquals(newSOA, soaRRset.first());
    }

    @Test
    void addRecord_ApexNSRecord_isStoredInOwnRRset() throws IOException {
        // Start with a zone that only has the SOA (no NS at the apex).
        Zone zoneWithoutNS = newZone(INITIAL_SOA);
        NSRecord apexNS = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        // Adding the NS record at the zone apex should succeed.
        assertDoesNotThrow(() -> zoneWithoutNS.addRecord(apexNS));
        // The SOA reference must stay unchanged.
        assertEquals(INITIAL_SOA, zoneWithoutNS.getSOA());
        // The NS RRset must now exist and contain exactly the added NS.
        RRset nsRRset = zoneWithoutNS.findExactMatch(ZONE_NAME, Type.NS);
        assertNotNull(nsRRset);
        assertEquals(1, nsRRset.size());
        assertEquals(apexNS, nsRRset.first());
    }

    @Test
    void addRecord_DuplicateRecordIgnored_RRsetSizeUnchanged() throws IOException {
        Name dupName = new Name("dup", ZONE_NAME);
        ARecord a = new ARecord(dupName, DClass.IN, 300L, InetAddress.getByName("192.0.2.1"));
        // First addition creates the RRset.
        zone.addRecord(a);
        RRset rrset = zone.findExactMatch(dupName, Type.A);
        assertNotNull(rrset);
        assertEquals(1, rrset.size());
        // Adding the *same* Record instance again must not increase the size.
        zone.addRecord(a);
        RRset rrsetAfter = zone.findExactMatch(dupName, Type.A);
        assertEquals(1, rrsetAfter.size());
        assertTrue(rrsetAfter.contains(a));
    }

    @Test
    void addRecord_DeepSubdomainRecord_isAccepted() throws IOException {
        Name deepName = new Name("host.deep.sub", ZONE_NAME);
        ARecord a = new ARecord(deepName, DClass.IN, 300L, InetAddress.getByName("198.51.100.42"));
        assertDoesNotThrow(() -> zone.addRecord(a));
        RRset rrset = zone.findExactMatch(deepName, Type.A);
        assertNotNull(rrset);
        assertEquals(1, rrset.size());
        assertEquals(a, rrset.first());
    }

    @Test
    void addRecord_RRsetSizeIncrement_multipleARecords() throws IOException {
        Name name = new Name("multi", ZONE_NAME);
        ARecord a1 = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("203.0.113.5"));
        ARecord a2 = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("203.0.113.6"));
        zone.addRecord(a1);
        zone.addRecord(a2);
        RRset rrset = zone.findExactMatch(name, Type.A);
        assertNotNull(rrset);
        assertEquals(2, rrset.size());
        assertTrue(rrset.contains(a1));
        assertTrue(rrset.contains(a2));
    }

    @Test
    void addRecord_NullOrigin_throwsNullPointerException() throws Exception {
        // Create a normal zone then nulify its private 'origin' field.
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        // now origin == null
        originField.set(zone, null);
        // Any record will now cause a NPE when subdomain() is invoked.
        Name someName = new Name("any", ZONE_NAME);
        ARecord a = new ARecord(someName, DClass.IN, 300L, InetAddress.getByName("192.0.2.99"));
        assertThrows(NullPointerException.class, () -> zone.addRecord(a));
    }

    @Test
    void addRecord_SOAReplacementWithNullInternalSoaRecord_throwsNullPointerException() throws Exception {
        // The zone already contains a valid SOA; we deliberately clear the internal field.
        Field soaField = Zone.class.getDeclaredField("soaRecord");
        soaField.setAccessible(true);
        // now zone's soaRecord == null
        soaField.set(zone, null);
        // Prepare a new SOA (same owner) to trigger the replacement path.
        SOARecord replacement = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster", ZONE_NAME), 99L, 7200L, 3600L, 1209600L, 3600L);
        // Because zone.soaRecord is null, rrset.deleteRR(null) will cause NPE.
        assertThrows(NullPointerException.class, () -> zone.addRecord(replacement));
    }
}
