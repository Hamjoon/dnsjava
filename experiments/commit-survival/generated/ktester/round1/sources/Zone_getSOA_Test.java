package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Iterator;

@ExtendWith(MockitoExtension.class)
class Zone_getSOA_Test {

    /**
     * Zone instance used for all tests.
     */
    private static Zone zone;

    /**
     * The SOARecord that we expect {@link Zone#getSOA()} to return.
     */
    private static SOARecord expectedSOA;

    @BeforeAll
    static void setupBeforeAll() throws IOException, TextParseException {
        // Create the zone name (example.)
        Name zoneName = new Name("example.");
        // Build the SOA record that will be stored inside the Zone.
        // Parameters: name, dclass, ttl, primary, admin, serial, refresh, retry, expire, minimum
        expectedSOA = new SOARecord(zoneName, DClass.IN, 3600L, // primary name server
        Name.fromConstantString("ns.example."), // responsible person
        new Name("hostmaster", zoneName), // serial
        1, // refresh
        21600L, // retry
        7200L, // expire
        2160000L, // minimum
        3600L);
        // Initialise the Zone with only the SOA record.
        zone = new Zone(zoneName, new Record[] { expectedSOA });
    }

    /**
     * Helper to build a simple SOARecord with deterministic values.
     */
    private static SOARecord buildSOA(Name zoneName, long serial) throws TextParseException {
        return new SOARecord(zoneName, DClass.IN, 3600L, // primary name server
        Name.fromConstantString("ns.example."), // responsible person
        new Name("hostmaster", zoneName), // serial
        serial, // refresh
        21600L, // retry
        7200L, // expire
        2160000L, // minimum
        3600L);
    }

    /**
     * Helper to build a generic ARecord.
     */
    private static ARecord buildARecord(Name name) throws IOException {
        return new ARecord(name, DClass.IN, 300L, InetAddress.getByName("192.0.2.1"));
    }

    @Test
    void getSOA_returnsTheSOARecordProvidedAtConstruction() {
        SOARecord actual = zone.getSOA();
        assertNotNull(actual, "Zone#getSOA() should never return null for a valid zone");
        assertEquals(expectedSOA, actual, "Zone#getSOA() must return the SOA record supplied to the constructor");
    }

    @Test
    void getSOA_isConsistentAcrossMultipleCalls() {
        SOARecord first = zone.getSOA();
        SOARecord second = zone.getSOA();
        assertSame(first, second, "Repeated calls to Zone#getSOA() should return the same instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("When a Zone is created without an SOA record, getSOA() returns null")
    void getSOA_returnsNullWhenZoneHasNoSOA() throws IOException, TextParseException {
        Name zoneName = new Name("emptyzone.");
        // no records at all
        Zone zone = new Zone(zoneName, new Record[] {});
        assertNull(zone.getSOA(), "Expected null SOA for a freshly created empty zone");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("After adding a valid SOARecord via addRecord, getSOA() returns that instance")
    void getSOA_returnsAddedSOARecord() throws IOException, TextParseException {
        Name zoneName = new Name("addsoa.");
        Zone zone = new Zone(zoneName, new Record[] {});
        SOARecord soa = buildSOA(zoneName, 1);
        zone.addRecord(soa);
        assertSame(soa, zone.getSOA(), "The SOA returned should be the exact instance added");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Returned SOARecord must preserve all field values of the original")
    void getSOA_preservesAllFields() throws IOException, TextParseException {
        Name zoneName = new Name("fieldcheck.");
        Zone zone = new Zone(zoneName, new Record[] {});
        SOARecord original = buildSOA(zoneName, 42);
        zone.addRecord(original);
        SOARecord retrieved = zone.getSOA();
        assertNotNull(retrieved, "SOA should not be null after addition");
        // Verify each accessor matches the original values
        assertEquals(original.getHost(), retrieved.getHost(), "MNAME (primary) must match");
        assertEquals(original.getAdmin(), retrieved.getAdmin(), "RNAME (admin) must match");
        assertEquals(original.getSerial(), retrieved.getSerial(), "Serial must match");
        assertEquals(original.getRefresh(), retrieved.getRefresh(), "Refresh must match");
        assertEquals(original.getRetry(), retrieved.getRetry(), "Retry must match");
        assertEquals(original.getExpire(), retrieved.getExpire(), "Expire must match");
        assertEquals(original.getMinimum(), retrieved.getMinimum(), "Minimum must match");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Removing the SOARecord clears the internal reference")
    void getSOA_returnsNullAfterSOARemoval() throws IOException, TextParseException {
        Name zoneName = new Name("removesoa.");
        Zone zone = new Zone(zoneName, new Record[] {});
        SOARecord soa = buildSOA(zoneName, 7);
        zone.addRecord(soa);
        assertNotNull(zone.getSOA(), "Pre‑condition: SOA must be present before removal");
        zone.removeRecord(soa);
        assertNull(zone.getSOA(), "SOA should be null after the record is removed");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Adding a second SOARecord replaces the first one")
    void getSOA_replacesWithSecondSOA() throws IOException, TextParseException {
        Name zoneName = new Name("replace.");
        Zone zone = new Zone(zoneName, new Record[] {});
        SOARecord first = buildSOA(zoneName, 1);
        // different serial
        SOARecord second = buildSOA(zoneName, 2);
        zone.addRecord(first);
        assertSame(first, zone.getSOA(), "First addition should be reflected");
        zone.addRecord(second);
        assertSame(second, zone.getSOA(), "Second addition should replace the first SOA");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("addRRset containing an SOARecord updates the zone's SOA")
    void getSOA_updatesWhenAddedViaRRset() throws IOException, TextParseException {
        Name zoneName = new Name("rrsetsoa.");
        Zone zone = new Zone(zoneName, new Record[] {});
        SOARecord soa = buildSOA(zoneName, 99);
        // Build an RRset that contains the SOARecord
        RRset rrset = new RRset();
        rrset.addRR(soa);
        zone.addRRset(rrset);
        assertSame(soa, zone.getSOA(), "SOA from RRset should become the zone's SOA");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Adding only non‑SOA records leaves getSOA() returning null")
    void getSOA_unchangedByNonSOARecords() throws IOException, TextParseException {
        Name zoneName = new Name("nonsoa.");
        Zone zone = new Zone(zoneName, new Record[] {});
        ARecord a = buildARecord(new Name("www", zoneName));
        zone.addRecord(a);
        assertNull(zone.getSOA(), "SOA should remain null after adding a non‑SOA record");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Removing a non‑SOA record does not clear the existing SOA")
    void getSOA_unchangedWhenRemovingUnrelatedRecord() throws IOException, TextParseException {
        Name zoneName = new Name("mixed.");
        Zone zone = new Zone(zoneName, new Record[] {});
        SOARecord soa = buildSOA(zoneName, 5);
        ARecord a = buildARecord(new Name("host", zoneName));
        zone.addRecord(soa);
        zone.addRecord(a);
        assertSame(soa, zone.getSOA(), "SOA must be present before removal of ARecord");
        zone.removeRecord(a);
        assertSame(soa, zone.getSOA(), "SOA should remain after unrelated record removal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Removing all RRsets via iterator results in getSOA() returning null")
    void getSOA_nullAfterIteratorClearsZone() throws IOException, TextParseException {
        Name zoneName = new Name("clearzone.");
        Zone zone = new Zone(zoneName, new Record[] {});
        SOARecord soa = buildSOA(zoneName, 10);
        ARecord a = buildARecord(new Name("host", zoneName));
        zone.addRecord(soa);
        zone.addRecord(a);
        assertNotNull(zone.getSOA(), "Pre‑condition: zone must have an SOA before clearing");
        // Iterate over all RRsets and remove each record individually.
        Iterator<RRset> it = zone.iterator();
        while (it.hasNext()) {
            RRset rrset = it.next();
            // Remove every record in the RRset from the zone.
            for (Record r : rrset.rrs()) {
                zone.removeRecord(r);
            }
        }
        assertNull(zone.getSOA(), "SOA should be null after the zone has been emptied");
    }
}
