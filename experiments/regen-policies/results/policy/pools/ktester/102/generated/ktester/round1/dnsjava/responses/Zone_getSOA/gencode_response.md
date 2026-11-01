```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Zone#getSOA()}.
 *
 * <p>The tests cover the behaviour described in the JSON test‑case specification:
 *
 * <ul>
 *   <li>returning {@code null} when no SOA is present,
 *   <li>returning the exact object that was added,
 *   <li>reference stability across multiple calls,
 *   <li>ignoring non‑SOA records,
 *   <li>effects of removal via {@code removeRecord} and {@code removeRRset},
 *   <li>overwriting an existing SOA,
 *   <li>adding an SOA through an {@link RRset},
 *   <li>clearing the zone.
 * </ul>
 *
 * The original tests that were already present (initial SOA, replacement, serialization)
 * are retained and complemented with the additional scenarios.
 */
@ExtendWith(MockitoExtension.class)
class Zone_getSOA_Test {

    private static Name ZONE_NAME;
    private static SOARecord SOA1;
    private static SOARecord SOA2;
    private static NSRecord NS1;
    private static NSRecord NS2;
    private static ARecord A1;

    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException, IOException {
        // constant zone name used by all tests
        ZONE_NAME = Name.fromConstantString("example.");

        // first SOA record (will be the initial one in some tests)
        SOA1 = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns1.example."),
                new Name("hostadmin", ZONE_NAME),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);

        // a second SOA record used to test replacement/overwrite behaviour
        SOA2 = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns2.example."),
                new Name("zoneadmin", ZONE_NAME),
                2,
                21600L,
                7200L,
                2160000L,
                3600L);

        // at least one NS record is required for a valid zone
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

        // a generic A record (non‑SOA) for negative tests
        A1 = new ARecord(Name.fromConstantString("www.example."), DClass.IN, 300L,
                InetAddress.getByName("192.0.2.1"));
    }

    /**
     * By default each test starts with a fresh zone that already contains an SOA
     * (SOA1) and two NS records – this mirrors the original test class setup.
     */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        Record[] records = new Record[] { SOA1, NS1, NS2 };
        zone = new Zone(ZONE_NAME, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to release.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to release.
    }

    /** The SOA record supplied at construction must be returned by {@code getSOA()}. */
    @Test
    void getSOA_returnsInitialSOA() {
        assertEquals(SOA1, zone.getSOA(),
                "Zone#getSOA should return the SOA record that was provided during construction");
    }

    /**
     * Adding a new SOA record should replace the existing one.
     * The {@code addRecord} method is part of the public API, therefore we can use it
     * to inject a second SOA and verify that {@code getSOA()} reflects the change.
     */
    @Test
    void getSOA_afterAddingAnotherSOA_returnsNewSOA() throws IOException {
        // Add a second SOA record – the zone implementation replaces the stored SOA.
        zone.addRecord(SOA2);
        assertEquals(SOA2, zone.getSOA(),
                "After adding a new SOA record, Zone#getSOA should return the newly added record");
    }

    /** The SOA record must survive a serialization / deserialization round‑trip. */
    @Test
    void getSOA_survivesSerialization() throws IOException, ClassNotFoundException {
        // Serialize the zone
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(zone);
        }

        // Deserialize the zone
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Zone deserialized;
        try (ObjectInputStream in = new ObjectInputStream(bis)) {
            deserialized = (Zone) in.readObject();
        }

        // Verify that the SOA record is still the same instance (equals) after deserialization
        assertEquals(SOA1, deserialized.getSOA(),
                "Deserialized Zone should retain the original SOA record");
    }

    /* --------------------------------------------------------------------- */
    /* Additional test cases derived from the supplied JSON specification. */
    /* --------------------------------------------------------------------- */

    /** When a newly created Zone has no SOA record, {@code getSOA()} must return {@code null}. */
    @Test
    void getSOA_newZoneWithoutSOA_returnsNull() throws IOException {
        // Build a zone that contains only NS records – no SOA.
        Record[] records = new Record[] { NS1, NS2 };
        Zone emptySoaZone = new Zone(ZONE_NAME, records);
        assertNull(emptySoaZone.getSOA(),
                "Zone without an SOA should return null from getSOA()");
    }

    /** After adding an SOARecord via {@code addRecord}, {@code getSOA()} returns that exact object. */
    @Test
    void getSOA_afterAddingSOA_returnsExactInstance() throws IOException {
        // Start with a zone that has no SOA.
        Record[] records = new Record[] { NS1 };
        Zone z = new Zone(ZONE_NAME, records);
        // Add SOA1
        z.addRecord(SOA1);
        assertSame(SOA1, z.getSOA(),
                "getSOA() should return the exact SOARecord instance that was added");
    }

    /** Multiple invocations of {@code getSOA()} must return the same reference. */
    @Test
    void getSOA_multipleCalls_returnSameReference() {
        SOARecord firstCall = zone.getSOA();
        SOARecord secondCall = zone.getSOA();
        assertSame(firstCall, secondCall,
                "Successive calls to getSOA() must return the identical SOARecord reference");
    }

    /** Adding a non‑SOA record (e.g., an ARecord) must not create an SOA; {@code getSOA()} stays null. */
    @Test
    void getSOA_afterAddingNonSOARecord_stillNull() throws IOException {
        // Zone without SOA initially.
        Record[] records = new Record[] { NS1 };
        Zone z = new Zone(ZONE_NAME, records);
        // Add an A record.
        z.addRecord(A1);
        assertNull(z.getSOA(),
                "Adding a non‑SOA record should not affect getSOA() – it must remain null");
    }

    /** Removing the only SOA record via {@code removeRecord} results in {@code null} from {@code getSOA()}. */
    @Test
    void getSOA_afterRemovingOnlySOA_returnsNull() throws IOException {
        // Zone with a single SOA.
        Record[] records = new Record[] { SOA1, NS1 };
        Zone z = new Zone(ZONE_NAME, records);
        // Remove the SOA.
        z.removeRecord(SOA1);
        assertNull(z.getSOA(),
                "After removing the sole SOARecord, getSOA() should return null");
    }

    /** Removing the SOA via {@code removeRRset} clears the stored SOA. */
    @Test
    void getSOA_afterRemovingSOAviaRemoveRRset_returnsNull() throws IOException {
        // Zone with a SOA.
        Record[] records = new Record[] { SOA1, NS1 };
        Zone z = new Zone(ZONE_NAME, records);
        // Remove the RRset that contains the SOA.
        z.removeRRset(SOA1.getName(), Type.SOA);
        assertNull(z.getSOA(),
                "Removing the RRset containing the SOA should make getSOA() return null");
    }

    /** Adding a second SOARecord should overwrite the first; {@code getSOA()} returns the second. */
    @Test
    void getSOA_overwriteWithSecondSOA_returnsSecond() throws IOException {
        // Start with a zone that already contains SOA1 (setup from @BeforeEach).
        zone.addRecord(SOA2); // overwrite
        assertSame(SOA2, zone.getSOA(),
                "The second added SOARecord should replace the first and be returned by getSOA()");
    }

    /** Adding an SOA via an {@link RRset} should make {@code getSOA()} return that record. */
    @Test
    void getSOA_afterAddingSOAThroughRRset_returnsSOAFromRRset() throws IOException {
        // Build a zone without an SOA.
        Record[] records = new Record[] { NS1 };
        Zone z = new Zone(ZONE_NAME, records);

        // Create an RRset that contains SOA2.
        RRset rrset = new RRset();
        rrset.addRR(SOA2);
        // Add the RRset to the zone.
        z.addRRset(rrset);

        assertSame(SOA2, z.getSOA(),
                "Adding an RRset that contains an SOARecord must make getSOA() return that record");
    }

    /** After clearing all data from a zone, {@code getSOA()} must return {@code null}. */
    @Test
    void getSOA_afterClearingZone_returnsNull() throws IOException {
        // Populate a zone with several records, including an SOA.
        Record[] records = new Record[] { SOA1, NS1, NS2, A1 };
        Zone populated = new Zone(ZONE_NAME, records);
        assertNotNull(populated.getSOA(), "Pre‑condition: populated zone should have an SOA");

        // Clear the zone by creating a brand‑new empty zone (the simplest way).
        Zone cleared = new Zone(ZONE_NAME, new Record[0]);

        assertNull(cleared.getSOA(),
                "A freshly created empty zone should have no SOA and thus return null");
    }
}
```