```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Zone#addRecord(Record)}.
 *
 * <p>The tests cover the validation logic (null, SOA owner mismatch, name outside the zone)
 * and the functional behavior (adding a new RRset, adding to an existing RRset and replacing the
 * SOA record).</p>
 */
@ExtendWith(MockitoExtension.class)
class Zone_addRecord_Test {

    /* --------------------------------------------------------------------- */
    /*  Shared fixtures – created once for the whole test suite               */
    /* --------------------------------------------------------------------- */
    private static Name ZONE_NAME;
    private static SOARecord INITIAL_SOA;
    private static NSRecord NS1;
    private static NSRecord NS2;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Zone name "example."
        ZONE_NAME = Name.fromConstantString("example.");

        // Minimal SOA record – required for any zone
        INITIAL_SOA = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostmaster", ZONE_NAME),
                1,          // serial
                7200L,      // refresh
                3600L,      // retry
                1209600L,   // expire
                3600L);     // minimum

        // Two NS records – also required for a valid zone
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L,
                Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L,
                Name.fromConstantString("ns2.example."));
    }

    /* --------------------------------------------------------------------- */
    /*  Per‑test fixtures – a fresh Zone instance for each test               */
    /* --------------------------------------------------------------------- */
    private Zone zone;

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

    /* --------------------------------------------------------------------- */
    /*  Tests                                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void addRecord_nullRecord_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> zone.addRecord(null));
    }

    @Test
    void addRecord_SOAOwnerNotZoneOrigin_throwsIllegalArgumentException() throws IOException {
        // Create an SOA whose name is *not* the zone origin.
        SOARecord foreignSOA = new SOARecord(
                Name.fromConstantString("otherzone."),
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.otherzone."),
                new Name("hostmaster", Name.fromConstantString("otherzone.")),
                2, 7200L, 3600L, 1209600L, 3600L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> zone.addRecord(foreignSOA));
        assertTrue(ex.getMessage().contains("SOA owner"));
    }

    @Test
    void addRecord_NameOutsideZone_throwsIllegalArgumentException() throws IOException {
        // Name "outside.example.com." is not a subdomain of "example."
        Name outside = new Name("outside.example.com.", ZONE_NAME);
        ARecord a = new ARecord(outside, DClass.IN, 300L, InetAddress.getByName("192.0.2.1"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> zone.addRecord(a));
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
        SOARecord newSOA = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostmaster", ZONE_NAME),
                99,                // new serial
                7200L, 3600L, 1209600L, 3600L);

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
}
```