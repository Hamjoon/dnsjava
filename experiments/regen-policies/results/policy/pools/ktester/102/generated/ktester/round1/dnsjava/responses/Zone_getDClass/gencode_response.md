```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;

import java.io.*;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Zone#getDClass()}.
 *
 * The method is trivial – it always returns {@link DClass#IN}.  The tests verify that the
 * returned value is constant under a variety of state‑changing operations (adding/removing
 * records, serialization, iteration, etc.).
 *
 * The {@code @BeforeEach} method builds a minimal, valid {@link Zone} containing the mandatory
 * SOA and NS records.  Each test either re‑uses this instance or creates its own fresh instance
 * when isolation is required.
 */
class Zone_getDClass_Test {

    private static final String ZONE_STRING = "example.";
    private Name zoneName;
    private Zone zone;

    /** Create a minimal, valid zone that contains the mandatory SOA and at least one NS record. */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        zoneName = Name.fromConstantString(ZONE_STRING);

        // Minimal SOA record (values are not important for the getDClass() contract)
        SOARecord soa = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                1,          // serial
                21600L,     // refresh
                7200L,      // retry
                2160000L,   // expire
                3600L);     // minimum

        // Minimal NS record
        NSRecord ns = new NSRecord(zoneName, DClass.IN, 300L,
                Name.fromConstantString("ns.example."));

        // Construct the zone with the two mandatory records
        zone = new Zone(zoneName, soa, ns);
    }

    @AfterEach
    void teardownAfterEach() {
        // placeholder – nothing to clean up per test
    }

    @BeforeAll
    static void setupBeforeAll() {
        // placeholder – nothing to initialise globally
    }

    @AfterAll
    static void teardownAfterAll() {
        // placeholder – nothing to clean up globally
    }

    /* --------------------------------------------------------------------- */
    /* Basic contract tests */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Zone#getDClass() returns DClass.IN")
    void getDClass_returnsIN() {
        int dclass = zone.getDClass();
        assertEquals(DClass.IN, dclass,
                "Zone#getDClass() should return DClass.IN");
    }

    @Test
    @DisplayName("Two consecutive calls return the same constant")
    void getDClass_twiceSameResult() {
        int first = zone.getDClass();
        int second = zone.getDClass();
        assertEquals(DClass.IN, first, "First call should return DClass.IN");
        assertEquals(DClass.IN, second, "Second call should also return DClass.IN");
        assertEquals(first, second, "Both calls must return the identical value");
    }

    @Test
    @DisplayName("Two independent Zone instances each return DClass.IN")
    void getDClass_twoIndependentZones() throws IOException {
        // First zone (already created by @BeforeEach)
        int firstZoneClass = zone.getDClass();

        // Second zone – built from fresh records
        Name otherName = Name.fromConstantString("other.");
        SOARecord soa = new SOARecord(
                otherName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.other."),
                new Name("hostadmin", otherName),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);
        NSRecord ns = new NSRecord(otherName, DClass.IN, 300L,
                Name.fromConstantString("ns.other."));
        Zone otherZone = new Zone(otherName, soa, ns);

        int secondZoneClass = otherZone.getDClass();

        assertEquals(DClass.IN, firstZoneClass,
                "First zone must return DClass.IN");
        assertEquals(DClass.IN, secondZoneClass,
                "Second zone must also return DClass.IN");
    }

    /* --------------------------------------------------------------------- */
    /* Serialization */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("getDClass() after Java serialization")
    void getDClass_afterSerialization() throws IOException, ClassNotFoundException {
        // Serialize the zone to a byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(zone);
        }

        // Deserialize back into a new Zone instance
        Zone deserialized;
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()))) {
            deserialized = (Zone) in.readObject();
        }

        assertNotNull(deserialized, "Deserialized Zone should not be null");
        assertEquals(DClass.IN, deserialized.getDClass(),
                "Deserialized Zone must still return DClass.IN");
    }

    /* --------------------------------------------------------------------- */
    /* Mutating the zone – RRset */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("addRRset() does not affect getDClass()")
    void getDClass_afterAddingRRset() {
        // Build a dummy RRset (e.g., an A record)
        ARecord a = new ARecord(Name.fromConstantString("host.example."),
                DClass.IN, 300L, InetAddress.getLoopbackAddress());
        RRset rrset = new RRset();
        rrset.addRR(a);

        // Mutate the zone
        zone.addRRset(rrset);

        // Verify the class constant is unchanged
        assertEquals(DClass.IN, zone.getDClass(),
                "Adding an RRset must not change the DClass");
    }

    @Test
    @DisplayName("addRecord() does not affect getDClass()")
    void getDClass_afterAddingRecord() {
        // Simple A record
        ARecord a = new ARecord(Name.fromConstantString("host.example."),
                DClass.IN, 300L, InetAddress.getLoopbackAddress());

        zone.addRecord(a);

        assertEquals(DClass.IN, zone.getDClass(),
                "Adding a Record must not change the DClass");
    }

    @Test
    @DisplayName("removeRRset() does not affect getDClass()")
    void getDClass_afterRemovingRRset() {
        // Create and add a RRset first
        ARecord a = new ARecord(Name.fromConstantString("host.example."),
                DClass.IN, 300L, InetAddress.getLoopbackAddress());
        RRset rrset = new RRset();
        rrset.addRR(a);
        zone.addRRset(rrset);

        // Now remove it
        zone.removeRRset(a.getName(), a.getType());

        assertEquals(DClass.IN, zone.getDClass(),
                "Removing an RRset must not change the DClass");
    }

    /* --------------------------------------------------------------------- */
    /* Iteration */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Iterating over the zone does not affect getDClass()")
    void getDClass_afterIteration() {
        Iterator<RRset> it = zone.iterator();
        while (it.hasNext()) {
            // Consume iterator – we don't care about the content for this test
            it.next();
        }

        assertEquals(DClass.IN, zone.getDClass(),
                "Iterating the zone must not change the DClass");
    }

    /* --------------------------------------------------------------------- */
    /* toMasterFile() */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("toMasterFile() does not affect getDClass()")
    void getDClass_afterToMasterFile() {
        // The method returns a String representation of the zone; we ignore the content.
        String master = zone.toMasterFile();
        assertNotNull(master, "toMasterFile() should return a non‑null string");

        // Verify DClass is still IN
        assertEquals(DClass.IN, zone.getDClass(),
                "Calling toMasterFile() must not change the DClass");
    }
}
```