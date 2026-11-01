**ZoneTest.java**

```java
###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link org.xbill.DNS.Zone}.
 *
 * The tests cover:
 *   • public API signatures
 *   • normal use‑cases
 *   • edge‑cases and exception handling
 *   • iterator behaviour (including AXFR iterator)
 *   • wildcard expansion
 *
 * All objects are created via the public constructors / factory methods that are
 * part of the supplied API – no mocking or subclassing is performed.
 */
public class ZoneTest {

    /** Common names used in the tests */
    private Name origin;          // example.com.
    private Name nsName;          // ns1.example.com.
    private Name aName;           // host.example.com.
    private Name cnameName;       // alias.example.com.
    private Name wildcardName;    // *.example.com.

    /** Records that form a minimal valid zone (SOA + NS) */
    private Record soaRecord;
    private Record nsRecord;

    /** Helper to create a generic Record (no RDATA) */
    private Record newRecord(String nameStr, int type) throws IOException {
        Name name = Name.fromString(nameStr);
        // Record.newRecord(name, type, dclass, ttl) creates an empty RDATA record.
        // For SOA and NS we will create proper concrete records later.
        return Record.newRecord(name, type, DClass.IN, 3600);
    }

    @Before
    public void setUp() throws Exception {
        origin = Name.fromString("example.com.");
        nsName = Name.fromString("ns1.example.com.");
        aName = Name.fromString("host.example.com.");
        cnameName = Name.fromString("alias.example.com.");
        wildcardName = Name.fromString("*.example.com.");

        /* ---- Build a proper SOA record ---- */
        Name host = Name.fromString("ns1.example.com.");
        Name admin = Name.fromString("admin.example.com.");
        soaRecord = new SOARecord(origin, DClass.IN, 3600,
                host, admin,
                2023100101L,   // serial
                7200,          // refresh
                3600,          // retry
                1209600,       // expire
                3600);         // minimum

        /* ---- Build a proper NS record ---- */
        nsRecord = Record.newRecord(origin, Type.NS, DClass.IN, 3600);
        // The NS RRset will contain a single NS record; we add the name later
        // via addRecord().
        // For simplicity we use Record.newRecord and later replace with a concrete
        // NSRecord if needed; the Zone class only cares about type/value.
    }

    /** --------------------------------------------------------------------
     *  1.  Constructor validation tests
     * -------------------------------------------------------------------*/

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullOrigin() throws IOException {
        new Zone(null, new Record[0]);
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingSOA() throws IOException {
        // Only NS, no SOA → validation should fail
        Record[] recs = new Record[] { nsRecord };
        new Zone(origin, recs);
    }

    @Test(expected = IOException.class)
    public void testConstructorMultipleSOA() throws IOException {
        // Two SOA records → validation should fail (exactly one required)
        Record[] recs = new Record[] { soaRecord, soaRecord, nsRecord };
        new Zone(origin, recs);
    }

    @Test(expected = IOException.class)
    public void testConstructorSOAOutsideOrigin() throws IOException {
        // SOA whose owner is not the zone origin → IOException from maybeAddRecord
        Record badSoa = new SOARecord(Name.fromString("bad.example.com."), DClass.IN,
                3600, Name.fromString("ns1.example.com."),
                Name.fromString("admin.example.com."),
                2023100101L, 7200, 3600, 1209600, 3600);
        Record[] recs = new Record[] { badSoa, nsRecord };
        new Zone(origin, recs);
    }

    @Test
    public void testConstructorValidZone() throws IOException {
        // Minimal valid zone: one SOA and one NS
        Record[] recs = new Record[] { soaRecord, nsRecord };
        Zone zone = new Zone(origin, recs);
        assertEquals(origin, zone.getOrigin());
        assertNotNull(zone.getSOA());
        assertNotNull(zone.getNS());
        assertEquals(DClass.IN, zone.getDClass());
    }

    /** --------------------------------------------------------------------
     *  2.  Basic lookup / add / remove tests
     * -------------------------------------------------------------------*/

    private Zone buildFullZone() throws IOException {
        // Build a zone containing:
        //   • SOA, NS (already required)
        //   • A record for host.example.com.
        //   • CNAME record for alias.example.com. → host.example.com.
        //   • Wildcard A record (*.example.com.) → 1.2.3.4
        Record aRec = Record.newRecord(aName.toString(), Type.A);
        Record cnameRec = Record.newRecord(cnameName.toString(), Type.CNAME);
        Record wildcardRec = Record.newRecord(wildcardName.toString(), Type.A);

        Record[] recs = new Record[] { soaRecord, nsRecord, aRec, cnameRec, wildcardRec };
        return new Zone(origin, recs);
    }

    @Test
    public void testFindExactMatch() throws IOException {
        Zone zone = buildFullZone();
        RRset rrset = zone.findExactMatch(aName, Type.A);
        assertNotNull(rrset);
        assertEquals(Type.A, rrset.getType());
        assertEquals(aName, rrset.getName());
    }

    @Test
    public void testFindExactMatchMissing() throws IOException {
        Zone zone = buildFullZone();
        RRset rrset = zone.findExactMatch(Name.fromString("nosuch.example.com."), Type.A);
        assertNull(rrset);
    }

    @Test
    public void testFindRecordsCNAME() throws IOException {
        Zone zone = buildFullZone();
        SetResponse resp = zone.findRecords(cnameName, Type.A);
        assertTrue(resp.isCNAME());
        assertNotNull(resp.getCNAME());
        assertEquals(cnameName, resp.getCNAME().getName());
    }

    @Test
    public void testFindRecordsWildcardExpansion() throws IOException {
        Zone zone = buildFullZone();
        Name query = Name.fromString("foo.example.com.");
        SetResponse resp = zone.findRecords(query, Type.A);
        assertTrue(resp.isSuccessful());
        List<RRset> answers = resp.answers();
        assertEquals(1, answers.size());

        RRset expanded = answers.get(0);
        // The expanded set should contain a record whose name is the queried name.
        assertEquals(query, expanded.getName());
        assertEquals(Type.A, expanded.getType());
    }

    @Test
    public void testFindRecordsOutsideZone() throws IOException {
        Zone zone = buildFullZone();
        Name external = Name.fromString("outside.com.");
        SetResponse resp = zone.findRecords(external, Type.A);
        assertTrue(resp.isNXDOMAIN());
    }

    @Test
    public void testAddAndRemoveRecord() throws IOException {
        Zone zone = buildFullZone();

        // Add a new A record for newhost.example.com.
        Name newHost = Name.fromString("newhost.example.com.");
        Record newA = Record.newRecord(newHost.toString(), Type.A);
        zone.addRecord(newA);

        RRset rr = zone.findExactMatch(newHost, Type.A);
        assertNotNull(rr);
        assertEquals(1, rr.size());

        // Remove it again
        zone.removeRecord(newA);
        assertNull(zone.findExactMatch(newHost, Type.A));
    }

    @Test
    public void testRemoveRecordWhenMultipleRRsInSet() throws IOException {
        Zone zone = buildFullZone();

        // Add two A records for the same name
        Record a1 = Record.newRecord(aName.toString(), Type.A);
        Record a2 = Record.newRecord(aName.toString(), Type.A);
        zone.addRecord(a1);
        zone.addRecord(a2);

        RRset set = zone.findExactMatch(aName, Type.A);
        assertNotNull(set);
        assertEquals(2, set.size());

        // Remove one of them – the RRset should still exist with size 1
        zone.removeRecord(a1);
        RRset after = zone.findExactMatch(aName, Type.A);
        assertNotNull(after);
        assertEquals(1, after.size());

        // Remove the last one – the RRset should be removed completely
        zone.removeRecord(a2);
        assertNull(zone.findExactMatch(aName, Type.A));
    }

    /** --------------------------------------------------------------------
     *  3.  Iterator tests
     * -------------------------------------------------------------------*/

    @Test
    public void testIteratorTraversesAllRRsets() throws IOException {
        Zone zone = buildFullZone();
        Iterator<RRset> it = zone.iterator();

        int count = 0;
        while (it.hasNext()) {
            RRset rs = it.next();
            assertNotNull(rs);
            count++;
        }
        // Expected RRsets:
        //   • SOA
        //   • NS
        //   • A (host)
        //   • CNAME
        //   • wildcard A
        // Total = 5
        assertEquals(5, count);
    }

    @Test
    public void testAXFRIteratorReturnsSOAAtBothEnds() throws IOException {
        Zone zone = buildFullZone();
        Iterator<RRset> axfr = zone.AXFR();

        // First element must be SOA
        assertTrue(axfr.hasNext());
        RRset first = axfr.next();
        assertEquals(Type.SOA, first.getType());

        // Iterate to the end; capture the last element
        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
        }
        assertNotNull(last);
        assertEquals(Type.SOA, last.getType());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveUnsupported() throws IOException {
        Zone zone = buildFullZone();
        Iterator<RRset> it = zone.iterator();
        it.next();
        it.remove();   // should throw
    }

    /** --------------------------------------------------------------------
     *  4.  toMasterFile / toString tests
     * -------------------------------------------------------------------*/

    @Test
    public void testToMasterFileContainsSOAAndNS() throws IOException {
        Zone zone = buildFullZone();
        String master = zone.toMasterFile();
        assertTrue(master.contains(soaRecord.getName().toString()));
        assertTrue(master.contains(nsRecord.getName().toString()));
    }

    @Test
    public void testToStringEqualsToMasterFile() throws IOException {
        Zone zone = buildFullZone();
        assertEquals(zone.toMasterFile(), zone.toString());
    }
}
###Test END##
```