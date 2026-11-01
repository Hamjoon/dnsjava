```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Iterator;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional unit tests for {@link Zone#addRecord(Record)}.
 *
 * <p>This class extends the basic test suite that already validates the
 * null‑check, SOA owner validation, sub‑domain validation, creation of a new
 * {@link RRset}, addition to an existing set and SOA replacement.  The new
 * tests cover edge‑cases described in the specification JSON:</p>
 *
 * <ul>
 *   <li>Adding the very first SOA to an empty zone.</li>
 *   <li>Adding an apex {@link NSRecord}.</li>
 *   <li>Duplicate record handling (no duplicate entries).</li>
 *   <li>Deep sub‑domain acceptance.</li>
 *   <li>RRset size increment for multiple records of the same name/type.</li>
 *   <li>Behaviour when the zone origin is {@code null} (expected NPE).</li>
 *   <li>Behaviour when {@code soaRecord} is {@code null} during a SOA replace
 *       (expected NPE).</li>
 * </ul>
 *
 * <p>All tests use the public API only; private fields are accessed via
 * reflection where required.</p>
 */
@ExtendWith(MockitoExtension.class)
class Zone_addRecord_AdditionalTest {

    /* --------------------------------------------------------------------- */
    /*  Shared fixtures – created once for the whole test suite               */
    /* --------------------------------------------------------------------- */
    private static Name ZONE_NAME;
    private static SOARecord INITIAL_SOA;
    private static NSRecord NS1;
    private static NSRecord NS2;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        ZONE_NAME = Name.fromConstantString("example."); // zone apex

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

        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L,
                Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L,
                Name.fromConstantString("ns2.example."));
    }

    /* --------------------------------------------------------------------- */
    /*  Per‑test fixtures – a fresh Zone instance for each test               */
    /* --------------------------------------------------------------------- */
    private Zone zone;

    /**
     * Helper that creates a new {@link Zone} with the supplied initial records.
     */
    private Zone newZone(Record... initial) throws IOException {
        return new Zone(ZONE_NAME, initial);
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Default zone contains a proper SOA + two NS records.
        zone = newZone(INITIAL_SOA, NS1, NS2);
    }

    @AfterEach
    void teardownAfterEach() {
        zone = null;
    }

    /* --------------------------------------------------------------------- */
    /*  Tests                                                                */
    /* --------------------------------------------------------------------- */

    @Test
    void addRecord_FirstSOAAddition_createsSOARRsetAndUpdatesSoaField() throws IOException {
        // Create a zone without any records.
        Zone emptyZone = newZone();

        // Build a brand‑new SOA that matches the zone origin.
        SOARecord newSOA = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostmaster", ZONE_NAME),
                2023100101L,
                3600L, 600L, 604800L, 86400L);

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

        NSRecord apexNS = new NSRecord(
                ZONE_NAME,
                DClass.IN,
                300L,
                Name.fromConstantString("ns1.example."));

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
        originField.set(zone, null); // now origin == null

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
        soaField.set(zone, null); // now zone's soaRecord == null

        // Prepare a new SOA (same owner) to trigger the replacement path.
        SOARecord replacement = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostmaster", ZONE_NAME),
                99L,
                7200L, 3600L, 1209600L, 3600L);

        // Because zone.soaRecord is null, rrset.deleteRR(null) will cause NPE.
        assertThrows(NullPointerException.class, () -> zone.addRecord(replacement));
    }
}
```