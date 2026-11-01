package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.Iterator;

class Zone_getNS_Test {

    /**
     * Common zone name used for all test zones.
     */
    private static Name ZONE_NAME;

    /**
     * An NS record that will be added to the zone that contains NS records.
     */
    private static NSRecord NS_RECORD;

    /**
     * A zone that contains an NS record.
     */
    private static Zone ZONE_WITH_NS;

    /**
     * A zone that does not contain any NS records.
     */
    private static Zone ZONE_WITHOUT_NS;

    /**
     * Minimal SOA record required for a valid zone.
     */
    private static SOARecord SOA;

    /**
     * A fresh Zone instance for each test method.
     */
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create the zone name (example.)
        ZONE_NAME = new Name("example.");
        // Create an NS record for the zone (example. NS ns1.example.)
        NS_RECORD = new NSRecord(// name
        ZONE_NAME, // class
        DClass.IN, // TTL
        300, // target name server
        Name.fromConstantString("ns1.example."));
        // A minimal SOA record required for a valid zone.
        SOARecord soa = new SOARecord(ZONE_NAME, DClass.IN, 3600L, // primary name server
        Name.fromConstantString("nameserver."), // hostmaster email
        new Name("hostmaster", ZONE_NAME), // serial
        1, // refresh
        21600L, // retry
        7200L, // expire
        2160000L, // minimum TTL
        3600L);
        // Zone that contains an NS record (plus the mandatory SOA).
        ZONE_WITH_NS = new Zone(ZONE_NAME, new Record[] { soa, NS_RECORD });
        // Zone that contains only the SOA record – no NS records at all.
        ZONE_WITHOUT_NS = new Zone(ZONE_NAME, new Record[] { soa });
    }

    @BeforeEach
    void setupBeforeEach() {
        // Every test starts with a brand‑new Zone that contains only the SOA.
        zone = new Zone(ZONE_NAME, new Record[] { SOA });
    }

    @Test
    void getNS_returnsRRsetWhenNSRecordPresent() {
        RRset ns = ZONE_WITH_NS.getNS();
        assertNotNull(ns, "Expected non‑null RRset when NS record is present");
        // Build the expected RRset containing exactly the NS_RECORD.
        RRset expected = new RRset(NS_RECORD);
        assertEquals(expected, ns, "RRset returned by getNS() does not match expected NS record set");
    }

    @Test
    void getNS_returnsNullWhenNoNSRecord() {
        RRset ns = ZONE_WITHOUT_NS.getNS();
        assertNull(ns, "Expected null RRset when zone contains no NS records");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNS_returnsNullWhenZoneNeverHadNS() {
        // The Zone has been constructed with only an SOA record.
        assertNull(zone.getNS(), "Expected null when a freshly created Zone has never had an NS RRset added");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNS_returnsInjectedRRsetViaReflection() throws Exception {
        // Build an NS RRset manually.
        RRset injected = new RRset(new NSRecord(ZONE_NAME, DClass.IN, 300, Name.fromConstantString("ns1.example.")));
        // Inject the RRset into the private field 'NS' using reflection.
        Field nsField = Zone.class.getDeclaredField("NS");
        nsField.setAccessible(true);
        nsField.set(zone, injected);
        // Verify that getNS() returns the exact same instance.
        RRset result = zone.getNS();
        assertSame(injected, result, "getNS() should return the exact RRset instance that was injected via reflection");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNS_staysNullAfterAddingNonNSRRset() {
        RRset aRRset = new RRset(new ARecord(ZONE_NAME, DClass.IN, 300, java.net.InetAddress.getLoopbackAddress()));
        zone.addRRset(aRRset);
        assertNull(zone.getNS(), "Adding a non‑NS RRset must not initialise the NS field; getNS() should remain null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNS_returnsSameRRsetWhenAddedViaAddRRset() {
        RRset nsRRset = new RRset(new NSRecord(ZONE_NAME, DClass.IN, 300, Name.fromConstantString("ns1.example.")));
        zone.addRRset(nsRRset);
        RRset result = zone.getNS();
        assertSame(nsRRset, result, "When an NS RRset is added via addRRset, getNS() must return that exact instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNS_returnsRRsetContainingAddedNSRecord() {
        NSRecord nsRecord = new NSRecord(ZONE_NAME, DClass.IN, 300, Name.fromConstantString("ns1.example."));
        zone.addRecord(nsRecord);
        RRset nsRRset = zone.getNS();
        assertNotNull(nsRRset, "RRset should be non‑null after adding an NS record");
        assertEquals(1, nsRRset.size(), "RRset should contain exactly one record after a single NS record is added");
        assertSame(nsRecord, nsRRset.first(), "The RRset should contain the exact NSRecord that was added");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNS_unchangedAfterAddingOtherRRset() {
        // First, add an NS RRset.
        RRset nsRRset = new RRset(new NSRecord(ZONE_NAME, DClass.IN, 300, Name.fromConstantString("ns1.example.")));
        zone.addRRset(nsRRset);
        // Then, add an unrelated MX RRset.
        RRset mxRRset = new RRset(new MXRecord(ZONE_NAME, DClass.IN, 300, 10, Name.fromConstantString("mail.example.")));
        zone.addRRset(mxRRset);
        // Verify that getNS() still returns the original NS RRset instance.
        RRset result = zone.getNS();
        assertSame(nsRRset, result, "Adding a non‑NS RRset must not replace the existing NS RRset");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNS_becomesNullAfterRemovingOnlyNSRecord() {
        NSRecord nsRecord = new NSRecord(ZONE_NAME, DClass.IN, 300, Name.fromConstantString("ns1.example."));
        zone.addRecord(nsRecord);
        assertNotNull(zone.getNS(), "Pre‑condition: NS RRset must be present after adding record");
        // Remove the record.
        zone.removeRecord(nsRecord);
        assertNull(zone.getNS(), "After removing the only NS record, getNS() should return null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNS_containsAllAddedNSRecordsInInsertionOrder() {
        NSRecord ns1 = new NSRecord(ZONE_NAME, DClass.IN, 300, Name.fromConstantString("ns1.example."));
        NSRecord ns2 = new NSRecord(ZONE_NAME, DClass.IN, 300, Name.fromConstantString("ns2.example."));
        // Add records one after another.
        zone.addRecord(ns1);
        zone.addRecord(ns2);
        RRset nsRRset = zone.getNS();
        assertNotNull(nsRRset, "RRset must be non‑null after adding NS records");
        assertEquals(2, nsRRset.size(), "RRset should contain both NS records");
        // Verify insertion order – the iterator of RRset yields records in the order added.
        Iterator<Record> it = nsRRset.rrs();
        assertEquals(ns1, it.next(), "First record should be the first NS added");
        assertEquals(ns2, it.next(), "Second record should be the second NS added");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(strings = { "A", "MX", "CNAME" })
    void getNS_staysNullWhenOnlyNonNSRecordsAdded(String type) throws Exception {
        Record rec;
        switch(type) {
            case "A":
                rec = new ARecord(ZONE_NAME, DClass.IN, 300, java.net.InetAddress.getLoopbackAddress());
                break;
            case "MX":
                rec = new MXRecord(ZONE_NAME, DClass.IN, 300, 10, Name.fromConstantString("mail.example."));
                break;
            case "CNAME":
                rec = new CNAMERecord(ZONE_NAME, DClass.IN, 300, Name.fromConstantString("cname.example."));
                break;
            default:
                throw new IllegalArgumentException("Unsupported type");
        }
        zone.addRecord(rec);
        assertNull(zone.getNS(), "After adding only a " + type + " record, getNS() must still return null");
    }
}
