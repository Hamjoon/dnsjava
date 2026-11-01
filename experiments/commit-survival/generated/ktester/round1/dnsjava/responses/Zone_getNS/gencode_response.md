```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link Zone#getNS()}.
 *
 * <p>The tests cover the following scenarios:</p>
 * <ul>
 *   <li>Zone created without any NS records – {@code getNS()} returns {@code null}.</li>
 *   <li>Injecting an {@link RRset} directly into the private {@code NS} field (via reflection).</li>
 *   <li>Adding a non‑NS {@link RRset} – the NS field stays {@code null}.</li>
 *   <li>Adding an NS {@link RRset} via {@code addRRset} – the same instance is returned.</li>
 *   <li>Adding an NS {@link Record} via {@code addRecord} – an {@link RRset} containing that record is returned.</li>
 *   <li>Adding other RRsets after an NS RRset – the original NS RRset is unchanged.</li>
 *   <li>Removing the sole NS record – the NS field becomes {@code null}.</li>
 *   <li>Adding multiple NS records – a single RRset containing all of them is returned.</li>
 * </ul>
 *
 * <p>No mocking is required because {@link Zone} is a concrete class with no external
 * collaborators.  All setup is performed with real DNS record objects.</p>
 */
class Zone_getNS_Test {

    /** Common zone name used for all test zones. */
    private static Name ZONE_NAME;

    /** Minimal SOA record required for a valid zone. */
    private static SOARecord SOA;

    /** A fresh Zone instance for each test method. */
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Example zone name.
        ZONE_NAME = new Name("example.");

        // Construct a minimal SOA record (mandatory for a valid Zone).
        SOA = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),          // primary name server
                new Name("hostmaster", ZONE_NAME),               // hostmaster email
                1,                                               // serial
                21600L,                                          // refresh
                7200L,                                           // retry
                2160000L,                                        // expire
                3600L                                            // minimum TTL
        );
    }

    @BeforeEach
    void setupBeforeEach() {
        // Every test starts with a brand‑new Zone that contains only the SOA.
        zone = new Zone(ZONE_NAME, new Record[]{SOA});
    }

    /* --------------------------------------------------------------------- */
    /* 1. Newly created Zone – no NS records                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void getNS_returnsNullWhenZoneNeverHadNS() {
        // The Zone has been constructed with only an SOA record.
        assertNull(zone.getNS(),
                "Expected null when a freshly created Zone has never had an NS RRset added");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Direct injection of a non‑null RRset into the private field NS    */
    /* --------------------------------------------------------------------- */
    @Test
    void getNS_returnsInjectedRRsetViaReflection() throws Exception {
        // Build an NS RRset manually.
        RRset injected = new RRset(
                new NSRecord(
                        ZONE_NAME,
                        DClass.IN,
                        300,
                        Name.fromConstantString("ns1.example.")
                )
        );

        // Inject the RRset into the private field 'NS' using reflection.
        Field nsField = Zone.class.getDeclaredField("NS");
        nsField.setAccessible(true);
        nsField.set(zone, injected);

        // Verify that getNS() returns the exact same instance.
        RRset result = zone.getNS();
        assertSame(injected, result,
                "getNS() should return the exact RRset instance that was injected via reflection");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Adding a non‑NS RRset (type A) – NS field stays null               */
    /* --------------------------------------------------------------------- */
    @Test
    void getNS_staysNullAfterAddingNonNSRRset() {
        RRset aRRset = new RRset(
                new ARecord(
                        ZONE_NAME,
                        DClass.IN,
                        300,
                        java.net.InetAddress.getLoopbackAddress()
                )
        );
        zone.addRRset(aRRset);
        assertNull(zone.getNS(),
                "Adding a non‑NS RRset must not initialise the NS field; getNS() should remain null");
    }

    /* --------------------------------------------------------------------- */
    /* 4. Adding an NS RRset via addRRset – same instance returned           */
    /* --------------------------------------------------------------------- */
    @Test
    void getNS_returnsSameRRsetWhenAddedViaAddRRset() {
        RRset nsRRset = new RRset(
                new NSRecord(
                        ZONE_NAME,
                        DClass.IN,
                        300,
                        Name.fromConstantString("ns1.example.")
                )
        );
        zone.addRRset(nsRRset);
        RRset result = zone.getNS();
        assertSame(nsRRset, result,
                "When an NS RRset is added via addRRset, getNS() must return that exact instance");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Adding a single NS Record via addRecord – RRset contains the record */
    /* --------------------------------------------------------------------- */
    @Test
    void getNS_returnsRRsetContainingAddedNSRecord() {
        NSRecord nsRecord = new NSRecord(
                ZONE_NAME,
                DClass.IN,
                300,
                Name.fromConstantString("ns1.example.")
        );
        zone.addRecord(nsRecord);

        RRset nsRRset = zone.getNS();
        assertNotNull(nsRRset, "RRset should be non‑null after adding an NS record");
        assertEquals(1, nsRRset.size(),
                "RRset should contain exactly one record after a single NS record is added");
        assertSame(nsRecord, nsRRset.first(),
                "The RRset should contain the exact NSRecord that was added");
    }

    /* --------------------------------------------------------------------- */
    /* 6. Adding an NS RRset then a different RRset – NS RRset unchanged    */
    /* --------------------------------------------------------------------- */
    @Test
    void getNS_unchangedAfterAddingOtherRRset() {
        // First, add an NS RRset.
        RRset nsRRset = new RRset(
                new NSRecord(
                        ZONE_NAME,
                        DClass.IN,
                        300,
                        Name.fromConstantString("ns1.example.")
                )
        );
        zone.addRRset(nsRRset);

        // Then, add an unrelated MX RRset.
        RRset mxRRset = new RRset(
                new MXRecord(
                        ZONE_NAME,
                        DClass.IN,
                        300,
                        10,
                        Name.fromConstantString("mail.example.")
                )
        );
        zone.addRRset(mxRRset);

        // Verify that getNS() still returns the original NS RRset instance.
        RRset result = zone.getNS();
        assertSame(nsRRset, result,
                "Adding a non‑NS RRset must not replace the existing NS RRset");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Removing the sole NS record – NS field becomes null                */
    /* --------------------------------------------------------------------- */
    @Test
    void getNS_becomesNullAfterRemovingOnlyNSRecord() {
        NSRecord nsRecord = new NSRecord(
                ZONE_NAME,
                DClass.IN,
                300,
                Name.fromConstantString("ns1.example.")
        );
        zone.addRecord(nsRecord);
        assertNotNull(zone.getNS(), "Pre‑condition: NS RRset must be present after adding record");

        // Remove the record.
        zone.removeRecord(nsRecord);

        assertNull(zone.getNS(),
                "After removing the only NS record, getNS() should return null");
    }

    /* --------------------------------------------------------------------- */
    /* 8. Adding multiple NS records – RRset contains all in insertion order */
    /* --------------------------------------------------------------------- */
    @Test
    void getNS_containsAllAddedNSRecordsInInsertionOrder() {
        NSRecord ns1 = new NSRecord(
                ZONE_NAME,
                DClass.IN,
                300,
                Name.fromConstantString("ns1.example.")
        );
        NSRecord ns2 = new NSRecord(
                ZONE_NAME,
                DClass.IN,
                300,
                Name.fromConstantString("ns2.example.")
        );

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
    /* 9. Parameterized test for null return when no NS after various adds  */
    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(strings = {"A", "MX", "CNAME"})
    void getNS_staysNullWhenOnlyNonNSRecordsAdded(String type) throws Exception {
        Record rec;
        switch (type) {
            case "A":
                rec = new ARecord(
                        ZONE_NAME,
                        DClass.IN,
                        300,
                        java.net.InetAddress.getLoopbackAddress()
                );
                break;
            case "MX":
                rec = new MXRecord(
                        ZONE_NAME,
                        DClass.IN,
                        300,
                        10,
                        Name.fromConstantString("mail.example.")
                );
                break;
            case "CNAME":
                rec = new CNAMERecord(
                        ZONE_NAME,
                        DClass.IN,
                        300,
                        Name.fromConstantString("cname.example.")
                );
                break;
            default:
                throw new IllegalArgumentException("Unsupported type");
        }
        zone.addRecord(rec);
        assertNull(zone.getNS(),
                "After adding only a " + type + " record, getNS() must still return null");
    }
}
```