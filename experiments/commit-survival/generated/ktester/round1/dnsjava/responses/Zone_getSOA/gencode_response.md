```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional unit tests for {@link Zone#getSOA()} covering the scenarios described in the
 * supplied JSON test matrix.
 *
 * <p>These tests are deliberately independent from the static {@code zone} instance created
 * in {@link Zone_getSOA_Test}. Each test builds its own {@link Zone} so that state does not
 * leak between cases.</p>
 */
@ExtendWith(MockitoExtension.class)
class Zone_getSOA_AdditionalTest {

    /** Helper to build a simple SOARecord with deterministic values. */
    private static SOARecord buildSOA(Name zoneName, long serial) throws TextParseException {
        return new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),          // primary name server
                new Name("hostmaster", zoneName),                // responsible person
                serial,                                          // serial
                21600L,                                          // refresh
                7200L,                                           // retry
                2160000L,                                        // expire
                3600L);                                          // minimum
    }

    /** Helper to build a generic ARecord. */
    private static ARecord buildARecord(Name name) throws IOException {
        return new ARecord(name, DClass.IN, 300L, InetAddress.getByName("192.0.2.1"));
    }

    /* --------------------------------------------------------------------- */
    /* 1. No SOA at construction                                                */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("When a Zone is created without an SOA record, getSOA() returns null")
    void getSOA_returnsNullWhenZoneHasNoSOA() throws IOException, TextParseException {
        Name zoneName = new Name("emptyzone.");
        Zone zone = new Zone(zoneName, new Record[]{}); // no records at all
        assertNull(zone.getSOA(), "Expected null SOA for a freshly created empty zone");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Adding a SOA via addRecord                                           */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("After adding a valid SOARecord via addRecord, getSOA() returns that instance")
    void getSOA_returnsAddedSOARecord() throws IOException, TextParseException {
        Name zoneName = new Name("addsoa.");
        Zone zone = new Zone(zoneName, new Record[]{});
        SOARecord soa = buildSOA(zoneName, 1);
        zone.addRecord(soa);
        assertSame(soa, zone.getSOA(), "The SOA returned should be the exact instance added");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Field integrity after addition                                       */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Returned SOARecord must preserve all field values of the original")
    void getSOA_preservesAllFields() throws IOException, TextParseException {
        Name zoneName = new Name("fieldcheck.");
        Zone zone = new Zone(zoneName, new Record[]{});
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
    /* 4. Removal of the SOA record                                            */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Removing the SOARecord clears the internal reference")
    void getSOA_returnsNullAfterSOARemoval() throws IOException, TextParseException {
        Name zoneName = new Name("removesoa.");
        Zone zone = new Zone(zoneName, new Record[]{});
        SOARecord soa = buildSOA(zoneName, 7);
        zone.addRecord(soa);
        assertNotNull(zone.getSOA(), "Pre‑condition: SOA must be present before removal");

        zone.removeRecord(soa);
        assertNull(zone.getSOA(), "SOA should be null after the record is removed");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Replacement behavior (second SOA overwrites first)                 */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Adding a second SOARecord replaces the first one")
    void getSOA_replacesWithSecondSOA() throws IOException, TextParseException {
        Name zoneName = new Name("replace.");
        Zone zone = new Zone(zoneName, new Record[]{});
        SOARecord first = buildSOA(zoneName, 1);
        SOARecord second = buildSOA(zoneName, 2); // different serial

        zone.addRecord(first);
        assertSame(first, zone.getSOA(), "First addition should be reflected");

        zone.addRecord(second);
        assertSame(second, zone.getSOA(), "Second addition should replace the first SOA");
    }

    /* --------------------------------------------------------------------- */
    /* 6. Adding an SOA via RRset                                               */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("addRRset containing an SOARecord updates the zone's SOA")
    void getSOA_updatesWhenAddedViaRRset() throws IOException, TextParseException {
        Name zoneName = new Name("rrsetsoa.");
        Zone zone = new Zone(zoneName, new Record[]{});
        SOARecord soa = buildSOA(zoneName, 99);

        // Build an RRset that contains the SOARecord
        RRset rrset = new RRset();
        rrset.addRR(soa);
        zone.addRRset(rrset);

        assertSame(soa, zone.getSOA(), "SOA from RRset should become the zone's SOA");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Non‑SOA records do not affect SOA                                    */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Adding only non‑SOA records leaves getSOA() returning null")
    void getSOA_unchangedByNonSOARecords() throws IOException, TextParseException {
        Name zoneName = new Name("nonsoa.");
        Zone zone = new Zone(zoneName, new Record[]{});
        ARecord a = buildARecord(new Name("www", zoneName));
        zone.addRecord(a);
        assertNull(zone.getSOA(), "SOA should remain null after adding a non‑SOA record");
    }

    /* --------------------------------------------------------------------- */
    /* 8. Removing unrelated records does not affect the SOA                 */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Removing a non‑SOA record does not clear the existing SOA")
    void getSOA_unchangedWhenRemovingUnrelatedRecord() throws IOException, TextParseException {
        Name zoneName = new Name("mixed.");
        Zone zone = new Zone(zoneName, new Record[]{});
        SOARecord soa = buildSOA(zoneName, 5);
        ARecord a = buildARecord(new Name("host", zoneName));

        zone.addRecord(soa);
        zone.addRecord(a);
        assertSame(soa, zone.getSOA(), "SOA must be present before removal of ARecord");

        zone.removeRecord(a);
        assertSame(soa, zone.getSOA(), "SOA should remain after unrelated record removal");
    }

    /* --------------------------------------------------------------------- */
    /* 9. Clearing the zone via iterator removal                               */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Removing all RRsets via iterator results in getSOA() returning null")
    void getSOA_nullAfterIteratorClearsZone() throws IOException, TextParseException {
        Name zoneName = new Name("clearzone.");
        Zone zone = new Zone(zoneName, new Record[]{});
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
```