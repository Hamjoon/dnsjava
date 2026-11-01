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
 * <p>The tests cover the validation logic (null record, SOA owner mismatch,
 * name not a sub‑domain) and the functional behaviour (creating a new RRset,
 * adding to an existing RRset and replacing the zone SOA).</p>
 */
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

    /** Initialise static resources that are shared across all tests. */
    @BeforeAll
    static void setupBeforeAll() {
        // No static resources needed for these tests.
    }

    /** Create a fresh {@link Zone} instance before each test. */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        // zone name: example.
        zoneName = Name.fromConstantString("example.");

        // First SOA (will be replaced in one test)
        soa1 = new SOARecord(
                zoneName,
                DClass.IN,
                TTL,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);

        // Second SOA (used to replace the first one)
        soa2 = new SOARecord(
                zoneName,
                DClass.IN,
                TTL,
                Name.fromConstantString("ns.example."),
                new Name("zoneadmin", zoneName),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);

        // Two NS records (required for a valid zone)
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

        // A couple of A records for later tests
        aRecord = new ARecord(new Name("www", zoneName), DClass.IN, TTL,
                InetAddress.getByName("127.0.0.1"));
        aRecord2 = new ARecord(new Name("www", zoneName), DClass.IN, TTL,
                InetAddress.getByName("127.0.0.2"));

        // Initialise the zone with a valid SOA and NS records.
        zone = new Zone(zoneName, soa1, ns1, ns2);
    }

    /** Clean‑up after each test – not required for this class. */
    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static cleanup needed.
    }

    /** Adding a {@code null} record must throw {@link IllegalArgumentException}. */
    @Test
    void addRecord_nullRecord_throws() {
        assertThrows(IllegalArgumentException.class, () -> zone.addRecord(null));
    }

    /** Adding an SOA whose owner does not match the zone origin must fail. */
    @Test
    void addRecord_soaOwnerMismatch_throws() throws IOException {
        // Create an SOA whose name is *not* the zone origin.
        Name otherName = Name.fromConstantString("otherzone.");
        SOARecord mismatchedSOA = new SOARecord(
                otherName,
                DClass.IN,
                TTL,
                Name.fromConstantString("ns.otherzone."),
                new Name("hostadmin", otherName),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> zone.addRecord(mismatchedSOA));
        assertTrue(ex.getMessage().contains("SOA owner"));
    }

    /** Adding a record whose name is not a sub‑domain of the zone origin must fail. */
    @Test
    void addRecord_nameOutsideZone_throws() throws IOException {
        Name outside = Name.fromConstantString("outside.com.");
        ARecord outsideRecord = new ARecord(outside, DClass.IN, TTL,
                InetAddress.getByName("127.0.0.9"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> zone.addRecord(outsideRecord));
        assertTrue(ex.getMessage().contains("is absolute and not a subdomain"));
    }

    /** Adding a record for a name/type that does not yet exist creates a new RRset. */
    @Test
    void addRecord_newRRset_created() {
        // The zone currently has no A records.
        assertNull(zone.findExactMatch(aRecord.getName(), Type.A));

        zone.addRecord(aRecord);

        RRset rrset = zone.findExactMatch(aRecord.getName(), Type.A);
        assertNotNull(rrset);
        assertEquals(1, rrset.size());
        assertTrue(rrset.containsRR(aRecord));
    }

    /** Adding a record that matches an existing RRset appends to that RRset. */
    @Test
    void addRecord_existingRRset_appends() {
        // First add one A record.
        zone.addRecord(aRecord);
        // Then add a second A record with the same name/type.
        zone.addRecord(aRecord2);

        RRset rrset = zone.findExactMatch(aRecord.getName(), Type.A);
        assertNotNull(rrset);
        assertEquals(2, rrset.size());
        assertTrue(rrset.containsRR(aRecord));
        assertTrue(rrset.containsRR(aRecord2));
    }

    /** Adding a new SOA replaces the existing zone SOA. */
    @Test
    void addRecord_replaceSOA_success() {
        // Verify initial SOA.
        assertEquals(soa1, zone.getSOA());

        // Add a new SOA (same owner = zone origin).
        zone.addRecord(soa2);

        // The zone's SOA reference must now point to the new object.
        assertEquals(soa2, zone.getSOA());

        // The old SOA must no longer be present in the zone.
        RRset oldSOARRset = zone.findExactMatch(soa1.getName(), Type.SOA);
        assertNull(oldSOARRset);
    }
}
```