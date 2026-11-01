###Test START##
```java
/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Zone}.
 *
 * The tests cover:
 *   • all public constructors and methods,
 *   • normal behaviour,
 *   • edge cases and exception handling.
 *
 * The test data is built using the public DNS library classes
 * (Name, Record, RRset, SOARecord, etc.) – no mocking is performed.
 */
package org.xbill.DNS.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xbill.DNS.*;

public class ZoneTest {

    /* -----------------------------------------------------------------
     * Helper constants and methods used by many tests
     * ----------------------------------------------------------------- */
    private static final Name ZONE_ORIGIN = Name.fromString("example.com.");
    private static final Name NS_NAME = Name.fromString("ns1.example.com.");
    private static final Name SOA_HOST = Name.fromString("ns1.example.com.");
    private static final Name SOA_ADMIN = Name.fromString("hostmaster.example.com.");
    private static final long TTL = 3600L;
    private static final int DCLASS = DClass.IN;

    /** Build a minimal valid SOA record for the test zone. */
    private static SOARecord makeSOA() {
        return new SOARecord(
                ZONE_ORIGIN,
                DCLASS,
                TTL,
                SOA_HOST,
                SOA_ADMIN,
                2023101501L,   // serial
                7200,          // refresh
                3600,          // retry
                1209600,       // expire
                3600);         // minimum
    }

    /** Build a minimal NS record for the zone origin. */
    private static Record makeNS() throws IOException {
        return Record.fromString(
                ZONE_ORIGIN,
                Type.NS,
                DCLASS,
                TTL,
                NS_NAME.toString(),
                Name.empty);
    }

    /** Build a generic A record for a given name. */
    private static Record makeA(Name name, String ip) throws IOException {
        return Record.fromString(
                name,
                Type.A,
                DCLASS,
                TTL,
                ip,
                Name.empty);
    }

    /** Build a CNAME record (alias → target). */
    private static Record makeCNAME(Name alias, Name target) throws IOException {
        return Record.fromString(
                alias,
                Type.CNAME,
                DCLASS,
                TTL,
                target.toString(),
                Name.empty);
    }

    /** Build a wildcard A record (*.example.com). */
    private static Record makeWildcardA(String ip) throws IOException {
        Name wild = Name.fromString("*.example.com.");
        return makeA(wild, ip);
    }

    /* -----------------------------------------------------------------
     * Test data that is reused across several tests
     * ----------------------------------------------------------------- */
    private Zone minimalZone;   // zone containing only SOA and NS

    @Before
    public void setUp() throws IOException {
        Record[] records = new Record[] {
                makeSOA(),
                makeNS()
        };
        minimalZone = new Zone(ZONE_ORIGIN, records);
    }

    /* -----------------------------------------------------------------
     * 1. Constructor tests
     * ----------------------------------------------------------------- */

    @Test
    public void testConstructorFromRecordsValid() throws IOException {
        Record a = makeA(Name.fromString("www.example.com."), "192.0.2.1");
        Record[] recs = new Record[] { makeSOA(), makeNS(), a };
        Zone z = new Zone(ZONE_ORIGIN, recs);

        assertEquals("origin must match", ZONE_ORIGIN, z.getOrigin());
        assertNotNull("SOA must be present", z.getSOA());
        assertNotNull("NS must be present", z.getNS());

        RRset aSet = z.findExactMatch(Name.fromString("www.example.com."), Type.A);
        assertNotNull("A RRset should be found", aSet);
        assertEquals("A RRset must contain exactly one record", 1, aSet.size());
        assertEquals("Record data must match", a, aSet.first());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullZoneThrows() throws IOException {
        new Zone(null, new Record[0]);
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingSOAThrows() throws IOException {
        Record[] recs = new Record[] { makeNS() };
        new Zone(ZONE_ORIGIN, recs);
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingNSThrows() throws IOException {
        Record[] recs = new Record[] { makeSOA() };
        new Zone(ZONE_ORIGIN, recs);
    }

    /* -----------------------------------------------------------------
     * 2. addRecord / findRecords / findExactMatch
     * ----------------------------------------------------------------- */

    @Test
    public void testAddRecordAndFindExact() throws IOException {
        Zone z = new Zone(ZONE_ORIGIN, new Record[] { makeSOA(), makeNS() });
        Name www = Name.fromString("www.example.com.");
        Record a = makeA(www, "198.51.100.42");

        z.addRecord(a);
        RRset found = z.findExactMatch(www, Type.A);
        assertNotNull("Added A record should be found", found);
        assertEquals("Exactly one A record expected", 1, found.size());
        assertEquals("Record must be the one added", a, found.first());
    }

    @Test
    public void testFindRecordsCNAME() throws IOException {
        Zone z = new Zone(ZONE_ORIGIN, new Record[] { makeSOA(), makeNS() });
        Name alias = Name.fromString("mail.example.com.");
        Name target = Name.fromString("mailserver.example.com.");
        Record cname = makeCNAME(alias, target);
        z.addRecord(cname);

        SetResponse resp = z.findRecords(alias, Type.A);
        assertTrue("Response should be a CNAME", resp.isCNAME());
        assertEquals("CNAME target must match", target, resp.getCNAME().getTarget());
    }

    @Test
    public void testWildcardExpansion() throws IOException {
        Zone z = new Zone(ZONE_ORIGIN, new Record[] { makeSOA(), makeNS() });
        Record wild = makeWildcardA("203.0.113.5");
        z.addRecord(wild);

        Name query = Name.fromString("foo.example.com.");
        SetResponse resp = z.findRecords(query, Type.A);
        assertTrue("Wildcard expansion should be successful", resp.isSuccessful());
        assertEquals("Exactly one answer RRset expected", 1, resp.answers().size());

        RRset expanded = resp.answers().get(0);
        assertEquals("Expanded RRset must retain original type", Type.A, expanded.getType());
        assertEquals("Expanded RRset name must be the queried name",
                query, expanded.getName());
    }

    @Test
    public void testFindExactMatchNonExisting() {
        RRset rs = minimalZone.findExactMatch(Name.fromString("nosuch.example.com."), Type.A);
        assertNull("Non‑existent name should return null", rs);
    }

    /* -----------------------------------------------------------------
     * 3. removeRecord
     * ----------------------------------------------------------------- */

    @Test
    public void testRemoveRecordSingleRRset() throws IOException {
        Zone z = new Zone(ZONE_ORIGIN, new Record[] { makeSOA(), makeNS() });
        Name www = Name.fromString("www.example.com.");
        Record a = makeA(www, "192.0.2.99");
        z.addRecord(a);

        // Ensure it is present
        assertNotNull(z.findExactMatch(www, Type.A));

        // Remove it
        z.removeRecord(a);
        assertNull("Record should be removed", z.findExactMatch(www, Type.A));
    }

    @Test
    public void testRemoveRecordFromMultiRRset() throws IOException {
        Zone z = new Zone(ZONE_ORIGIN, new Record[] { makeSOA(), makeNS() });
        Name www = Name.fromString("www.example.com.");
        Record a1 = makeA(www, "192.0.2.1");
        Record a2 = makeA(www, "192.0.2.2");
        z.addRecord(a1);
        z.addRecord(a2);

        // Remove only a1
        z.removeRecord(a1);
        RRset rs = z.findExactMatch(www, Type.A);
        assertNotNull("RRset must still exist after removing one record", rs);
        assertEquals("One record should remain", 1, rs.size());
        assertEquals("Remaining record must be a2", a2, rs.first());
    }

    /* -----------------------------------------------------------------
     * 4. iterator and AXFR iterator
     * ----------------------------------------------------------------- */

    @Test
    public void testIteratorContainsAllRRsets() throws IOException {
        Record a = makeA(Name.fromString("www.example.com."), "203.0.113.10");
        Record[] recs = new Record[] { makeSOA(), makeNS(), a };
        Zone z = new Zone(ZONE_ORIGIN, recs);

        Iterator<RRset> it = z.iterator();
        int count = 0;
        boolean sawSOA = false, sawNS = false, sawA = false;
        while (it.hasNext()) {
            RRset rs = it.next();
            count++;
            if (rs.getType() == Type.SOA) sawSOA = true;
            if (rs.getType() == Type.NS)  sawNS = true;
            if (rs.getType() == Type.A)   sawA = true;
        }
        assertEquals("Exactly three RRsets should be iterated", 3, count);
        assertTrue("SOA must be present", sawSOA);
        assertTrue("NS must be present", sawNS);
        assertTrue("A must be present", sawA);
    }

    @Test
    public void testAXFRIteratorEndsWithSOA() throws IOException {
        Record a = makeA(Name.fromString("www.example.com."), "203.0.113.11");
        Record[] recs = new Record[] { makeSOA(), makeNS(), a };
        Zone z = new Zone(ZONE_ORIGIN, recs);

        Iterator<RRset> axfr = z.AXFR();
        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
        }
        assertNotNull("AXFR iterator should return at least one RRset", last);
        assertEquals("The last RRset returned by AXFR must be the SOA", Type.SOA, last.getType());
    }

    /* -----------------------------------------------------------------
     * 5. findRecords with ANY and out‑of‑zone queries
     * ----------------------------------------------------------------- */

    @Test
    public void testFindRecordsAnyType() throws IOException {
        Record a = makeA(Name.fromString("www.example.com."), "203.0.113.12");
        Record txt = Record.fromString(
                Name.fromString("www.example.com."),
                Type.TXT,
                DCLASS,
                TTL,
                "\"sample text\"",
                Name.empty);
        Record[] recs = new Record[] { makeSOA(), makeNS(), a, txt };
        Zone z = new Zone(ZONE_ORIGIN, recs);

        SetResponse resp = z.findRecords(ZONE_ORIGIN, Type.ANY);
        assertTrue("ANY query should be successful", resp.isSuccessful());
        List<RRset> answers = resp.answers();
        // Expect at least SOA, NS, A, TXT
        assertTrue("At least 4 RRsets expected", answers.size() >= 4);
    }

    @Test
    public void testFindRecordsOutsideZoneReturnsNXDOMAIN() {
        Name external = Name.fromString("example.org.");
        SetResponse resp = minimalZone.findRecords(external, Type.A);
        assertTrue("Query outside zone must be NXDOMAIN", resp.isNXDOMAIN());
    }

    /* -----------------------------------------------------------------
     * 6. toMasterFile / toString representation
     * ----------------------------------------------------------------- */

    @Test
    public void testToMasterFileContainsSOAAndNS() {
        String master = minimalZone.toMasterFile();
        assertTrue("Output must contain the SOA name", master.contains(ZONE_ORIGIN.toString()));
        assertTrue("Output must contain the NS name", master.contains(NS_NAME.toString()));
    }

    @Test
    public void testToStringDelegatesToMasterFile() {
        assertEquals("toString should delegate to toMasterFile",
                minimalZone.toMasterFile(),
                minimalZone.toString());
    }
}
```
###Test END##