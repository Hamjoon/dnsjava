###Test START##
```java
/*
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.Zone
 *
 * The tests cover:
 *   • construction (valid and invalid)
 *   • basic getters
 *   • record lookup (exact, CNAME, DNAME, delegation, wildcard, ANY, NX* cases)
 *   • mutation APIs (addRRset, addRecord, removeRecord)
 *   • iterator and AXFR iterator ordering
 *   • master‑file rendering
 *
 * The test suite uses only the public API listed in the prompt:
 *   – org.xbill.DNS.Name
 *   – org.xbill.DNS.Record (via Record.newRecord)
 *   – org.xbill.DNS.SOARecord
 *   – org.xbill.DNS.RRset
 *   – org.xbill.DNS.Type, DClass, SetResponse, ZoneTransferIn, ZoneTransferException
 *
 * No mocking or subclassing is performed – all objects are real instances.
 */

package org.xbill.DNS;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ZoneTest {

    /* ---------------------------------------------------------------------- */
    /* Helper utilities                                                       */
    /* ---------------------------------------------------------------------- */

    /** Create a generic Record of the given type with empty rdata. */
    private static Record makeRecord(String name, int type) throws IOException {
        Name n = Name.fromString(name);
        // Empty rdata is sufficient for the logic exercised in Zone.
        return Record.newRecord(n, type, DClass.IN, 3600, new byte[0]);
    }

    /** Convenience wrapper for creating an A‑record. */
    private static Record makeA(String name) throws IOException {
        return makeRecord(name, Type.A);
    }

    /** Convenience wrapper for creating a CNAME‑record. */
    private static Record makeCNAME(String name) throws IOException {
        return makeRecord(name, Type.CNAME);
    }

    /** Convenience wrapper for creating a DNAME‑record. */
    private static Record makeDNAME(String name) throws IOException {
        return makeRecord(name, Type.DNAME);
    }

    /** Convenience wrapper for creating an NS‑record. */
    private static Record makeNS(String name) throws IOException {
        return makeRecord(name, Type.NS);
    }

    /** Convenience wrapper for creating a generic ANY‑type record (used only for tests). */
    private static Record makeANY(String name) throws IOException {
        return makeRecord(name, Type.ANY);
    }

    /** Build a minimal, valid SOA record for the supplied zone name. */
    private static SOARecord makeSOA(Name zone) {
        // Use the zone itself as the primary name server and admin.
        Name host = zone;               // primary master
        Name admin = zone;              // admin (usually hostmaster.zone)
        long serial = 2023100101L;
        long refresh = 7200;
        long retry = 3600;
        long expire = 1209600;
        long minimum = 3600;
        return new SOARecord(zone, DClass.IN, 3600, host, admin,
                             serial, refresh, retry, expire, minimum);
    }

    /* ---------------------------------------------------------------------- */
    /* Test data (shared across tests)                                        */
    /* ---------------------------------------------------------------------- */

    private Name zoneName;          // "example.com."
    private SOARecord soa;          // SOA for the zone
    private Record ns;              // NS at the zone apex
    private Record a_www;           // A record for www.example.com.
    private Record cname_alias;     // CNAME for alias.example.com.
    private Record dname_sub;       // DNAME for sub.example.com.
    private Record wildcard;        // A wildcard A record (*.example.com.)

    @Before
    public void setUp() throws IOException {
        zoneName = Name.fromString("example.com.");
        soa = makeSOA(zoneName);
        ns = makeNS("example.com.");
        a_www = makeA("www.example.com.");
        cname_alias = makeCNAME("alias.example.com.");
        dname_sub = makeDNAME("sub.example.com.");
        wildcard = makeA("*.example.com.");
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Construction and basic getters                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructorFromRecordArray_valid() throws IOException {
        Record[] records = new Record[] { soa, ns, a_www };
        Zone zone = new Zone(zoneName, records);

        assertEquals("Origin must be the supplied name", zoneName, zone.getOrigin());
        assertSame("SOA must be the one we supplied", soa, zone.getSOA());
        assertNotNull("NS set must be present", zone.getNS());
        assertEquals("Class must be IN", DClass.IN, zone.getDClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullZone_throws() throws IOException {
        new Zone(null, new Record[0]);
    }

    @Test(expected = IOException.class)
    public void testConstructor_missingSOA_throws() throws IOException {
        // No SOA record – constructor must reject the zone.
        new Zone(zoneName, new Record[] { ns, a_www });
    }

    @Test(expected = IOException.class)
    public void testConstructor_multipleSOA_throws() throws IOException {
        // Two SOA records – violates “exactly 1 SOA” rule.
        SOARecord soa2 = makeSOA(Name.fromString("example.org."));
        new Zone(zoneName, new Record[] { soa, soa2, ns });
    }

    @Test(expected = IOException.class)
    public void testMaybeAddRecord_SOAOwnerMismatch_throws() throws IOException {
        // Attempt to create a zone where the SOA owner is not the zone origin.
        Record badSOA = makeRecord("bad.example.com.", Type.SOA);
        new Zone(zoneName, new Record[] { badSOA, ns });
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Exact match lookup (findExactMatch)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindExactMatch_present() throws IOException {
        Record[] records = new Record[] { soa, ns, a_www };
        Zone zone = new Zone(zoneName, records);

        RRset result = zone.findExactMatch(Name.fromString("www.example.com."), Type.A);
        assertNotNull("Exact A record must be found", result);
        assertEquals("Result type must be A", Type.A, result.getType());
    }

    @Test
    public void testFindExactMatch_absent_returnsNull() throws IOException {
        Record[] records = new Record[] { soa, ns };
        Zone zone = new Zone(zoneName, records);

        RRset result = zone.findExactMatch(Name.fromString("nosuch.example.com."), Type.A);
        assertNull("Missing name should return null", result);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. General lookup (findRecords) – various scenarios                  */
    /* ---------------------------------------------------------------------- */

    private Zone buildComplexZone() throws IOException {
        // Build a zone containing:
        //   • SOA, NS (origin)
        //   • A www.example.com.
        //   • CNAME alias.example.com. → www.example.com.
        //   • DNAME sub.example.com. → other.example.com.
        //   • Wildcard *.example.com. → A 1.2.3.4
        Record[] recs = new Record[] {
                soa, ns,
                a_www,
                cname_alias,
                dname_sub,
                wildcard
        };
        return new Zone(zoneName, recs);
    }

    @Test
    public void testFindRecords_exactA_successful() throws IOException {
        Zone zone = buildComplexZone();
        SetResponse resp = zone.findRecords(Name.fromString("www.example.com."), Type.A);
        assertTrue("Lookup should be successful", resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        assertEquals(Type.A, resp.answers().get(0).getType());
    }

    @Test
    public void testFindRecords_cnameRedirect() throws IOException {
        Zone zone = buildComplexZone();
        SetResponse resp = zone.findRecords(Name.fromString("alias.example.com."), Type.A);
        // The name exists as a CNAME, so the response must be CNAME.
        assertTrue("CNAME response expected", resp.isCNAME());
        assertNotNull(resp.getCNAME());
        assertEquals("alias.example.com.", resp.getCNAME().getName().toString());
    }

    @Test
    public void testFindRecords_dnameRedirect() throws IOException {
        Zone zone = buildComplexZone();
        SetResponse resp = zone.findRecords(Name.fromString("foo.sub.example.com."), Type.A);
        // The closest ancestor with a DNAME is sub.example.com.
        assertTrue("DNAME response expected", resp.isDNAME());
        assertNotNull(resp.getDNAME());
        assertEquals("sub.example.com.", resp.getDNAME().getName().toString());
    }

    @Test
    public void testFindRecords_delegation_NS() throws IOException {
        // Add a delegation NS for delegated.example.com.
        Record delegationNS = makeNS("delegated.example.com.");
        Record[] recs = new Record[] { soa, ns, delegationNS };
        Zone zone = new Zone(zoneName, recs);

        SetResponse resp = zone.findRecords(Name.fromString("delegated.example.com."), Type.A);
        assertTrue("Delegation response expected", resp.isDelegation());
        assertNotNull(resp.getNS());
        assertEquals(Type.NS, resp.getNS().getType());
    }

    @Test
    public void testFindRecords_wildcardExpansion() throws IOException {
        Zone zone = buildComplexZone();
        // Query for a name that matches the wildcard but has no explicit record.
        SetResponse resp = zone.findRecords(Name.fromString("random.example.com."), Type.A);
        assertTrue("Wildcard expansion should succeed", resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        RRset expanded = resp.answers().get(0);
        // The returned RRset must contain a record whose name is the queried name.
        assertEquals("random.example.com.", expanded.getName().toString());
    }

    @Test
    public void testFindRecords_ANYReturnsAllAtExactName() throws IOException {
        Zone zone = buildComplexZone();
        SetResponse resp = zone.findRecords(Name.fromString("www.example.com."), Type.ANY);
        assertTrue("ANY lookup should be successful", resp.isSuccessful());
        // Expect exactly one RRset (the A record) because that's the only type at that name.
        assertEquals(1, resp.answers().size());
        assertEquals(Type.A, resp.answers().get(0).getType());
    }

    @Test
    public void testFindRecords_NXDOMAIN() throws IOException {
        Zone zone = buildComplexZone();
        SetResponse resp = zone.findRecords(Name.fromString("outside.com."), Type.A);
        assertTrue("Query outside zone should be NXDOMAIN", resp.isNXDOMAIN());
    }

    @Test
    public void testFindRecords_NXRRSET() throws IOException {
        Zone zone = buildComplexZone();
        // www exists but not as MX.
        SetResponse resp = zone.findRecords(Name.fromString("www.example.com."), Type.MX);
        assertTrue("Missing type should be NXRRSET", resp.isNXRRSET());
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Mutating the zone (addRRset, addRecord, removeRecord)               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddRecord_createsNewRRset() throws IOException {
        Zone zone = new Zone(zoneName, new Record[] { soa, ns });
        Record newA = makeA("newhost.example.com.");
        zone.addRecord(newA);

        RRset rs = zone.findExactMatch(Name.fromString("newhost.example.com."), Type.A);
        assertNotNull("New RRset must be present after addRecord", rs);
        assertEquals(1, rs.size());
        assertEquals(newA, rs.first());
    }

    @Test
    public void testAddRecord_mergesIntoExistingRRset() throws IOException {
        Zone zone = new Zone(zoneName, new Record[] { soa, ns, a_www });
        Record secondA = makeA("www.example.com."); // same name & type, different (empty) rdata
        zone.addRecord(secondA);

        RRset rs = zone.findExactMatch(Name.fromString("www.example.com."), Type.A);
        assertNotNull(rs);
        assertEquals("RRset should now contain two records", 2, rs.size());
    }

    @Test
    public void testRemoveRecord_deletesRRsetWhenLast() throws IOException {
        Zone zone = new Zone(zoneName, new Record[] { soa, ns, a_www });
        zone.removeRecord(a_www);

        RRset rs = zone.findExactMatch(Name.fromString("www.example.com."), Type.A);
        assertNull("RRset should be removed after deleting its sole record", rs);
    }

    @Test
    public void testRemoveRecord_decrementsRRsetSize() throws IOException {
        Zone zone = new Zone(zoneName, new Record[] { soa, ns, a_www });
        // Add a second A record to the same name.
        Record a2 = makeA("www.example.com.");
        zone.addRecord(a2);
        // Now remove one of them.
        zone.removeRecord(a_www);
        RRset rs = zone.findExactMatch(Name.fromString("www.example.com."), Type.A);
        assertNotNull(rs);
        assertEquals("RRset should contain exactly one remaining record", 1, rs.size());
        assertEquals(a2, rs.first());
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Iterators (standard and AXFR)                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIterator_ordering() throws IOException {
        Zone zone = buildComplexZone();
        Iterator<RRset> it = zone.iterator();

        // First must be the SOA at origin.
        assertTrue(it.hasNext());
        RRset first = it.next();
        assertEquals(Type.SOA, first.getType());

        // Second must be the NS at origin.
        assertTrue(it.hasNext());
        RRset second = it.next();
        assertEquals(Type.NS, second.getType());

        // Subsequent RRsets can be in any order; we simply ensure we see the A, CNAME, DNAME, and wildcard.
        int seen = 0;
        while (it.hasNext()) {
            RRset rs = it.next();
            int t = rs.getType();
            if (t == Type.A || t == Type.CNAME || t == Type.DNAME) {
                seen++;
            }
        }
        assertEquals("Should have seen three non‑origin RRsets (A, CNAME, DNAME)", 3, seen);
    }

    @Test
    public void testAXFRIterator_includesFinalSOA() throws IOException {
        Zone zone = buildComplexZone();
        Iterator<RRset> axfr = zone.AXFR();

        // Consume all entries and capture the last one.
        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
        }
        assertNotNull("AXFR iterator must produce at least one RRset", last);
        assertEquals("The final RRset returned by AXFR must be the SOA", Type.SOA, last.getType());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIterator_remove_notSupported() throws IOException {
        Zone zone = new Zone(zoneName, new Record[] { soa, ns });
        Iterator<RRset> it = zone.iterator();
        it.remove(); // should throw
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Master‑file rendering (toMasterFile / toString)                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMasterFile_containsAllRecords() throws IOException {
        Zone zone = buildComplexZone();
        String master = zone.toMasterFile();

        // Verify that each original record appears (as its toString representation)
        // The Record.toString() implementation includes the name, ttl, class, type, and rdata.
        assertTrue(master.contains(soa.toString()));
        assertTrue(master.contains(ns.toString()));
        assertTrue(master.contains(a_www.toString()));
        assertTrue(master.contains(cname_alias.toString()));
        assertTrue(master.contains(dname_sub.toString()));
        assertTrue(master.contains(wildcard.toString()));
    }

    @Test
    public void testToString_delegatesToMasterFile() throws IOException {
        Zone zone = new Zone(zoneName, new Record[] { soa, ns });
        assertEquals("toString should be identical to toMasterFile", zone.toMasterFile(), zone.toString());
    }
}
```
###Test END##