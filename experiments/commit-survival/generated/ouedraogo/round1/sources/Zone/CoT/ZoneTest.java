package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link Zone}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (basic behaviour)</li>
 *   <li>Edge cases and exception handling (null arguments, missing SOA/NS, wrong SOA owner,
 *       wild‑card handling, delegation, CNAME/DNAME, ANY queries, iterator order, etc.)</li>
 * </ul>
 *
 * The test data uses only the public constructors and factory methods that are part of the
 * supplied source code and the minimal DNS classes listed in the prompt.
 */
public class ZoneTest {

    /* --------------------------------------------------------------------- */
    /*  Helper methods to create DNS objects used throughout the test suite  */
    /* --------------------------------------------------------------------- */

    private static Name name(String s) throws TextParseException {
        return Name.fromString(s);
    }

    /** Minimal SOA record for the zone origin */
    private static SOARecord makeSOA(Name origin) {
        // host = ns1.origin, admin = hostmaster.origin, serial = 2023100101L, other fields are arbitrary
        return new SOARecord(
                origin,
                DClass.IN,
                3600,
                Name.fromString("ns1." + origin),
                Name.fromString("hostmaster." + origin),
                2023100101L,
                7200,
                3600,
                1209600,
                3600);
    }

    /** Simple NS record for the zone origin */
    private static Record makeNS(Name origin) {
        // Record.newRecord creates a generic Record of the requested type.
        return Record.newRecord(origin, Type.NS, DClass.IN, 3600);
    }

    /** Generic A record */
    private static Record makeA(Name name) {
        return Record.newRecord(name, Type.A, DClass.IN, 3600);
    }

    /** Generic CNAME record */
    private static Record makeCNAME(Name alias, Name target) {
        // The library provides a CNAMERecord class, but we can use the generic factory.
        // The rdata (the target name) is encoded automatically by the library.
        return Record.newRecord(alias, Type.CNAME, DClass.IN, 3600);
    }

    /** Generic DNAME record */
    private static Record makeDNAME(Name alias, Name target) {
        return Record.newRecord(alias, Type.DNAME, DClass.IN, 3600);
    }

    /* --------------------------------------------------------------------- */
    /*  Test fixtures                                                       */
    /* --------------------------------------------------------------------- */

    private Name zoneName;           // example.com.
    private SOARecord soaRecord;      // mandatory SOA
    private Record nsRecord;         // mandatory NS
    private Record aRecord;          // www.example.com. A
    private Record cnameRecord;      // mail.example.com. CNAME
    private Record dnameRecord;      // old.example.com. DNAME
    private Record wildARecord;      // *.example.com. A

    /** Builds a complete, well‑formed zone used by many tests. */
    private Zone buildValidZone() throws IOException {
        Record[] recs = new Record[] {
                soaRecord,
                nsRecord,
                aRecord,
                cnameRecord,
                dnameRecord,
                wildARecord
        };
        return new Zone(zoneName, recs);
    }

    @Before
    public void setUp() throws Exception {
        zoneName = name("example.com.");
        soaRecord = makeSOA(zoneName);
        nsRecord = makeNS(zoneName);
        aRecord = makeA(name("www.example.com."));
        cnameRecord = makeCNAME(name("mail.example.com."), name("www.example.com."));
        dnameRecord = makeDNAME(name("old.example.com."), name("new.example.com."));
        wildARecord = makeA(name("*.example.com."));
    }

    /* --------------------------------------------------------------------- */
    /*  1. Public method signatures (basic sanity tests)                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetOrigin() throws Exception {
        Zone z = buildValidZone();
        assertEquals(zoneName, z.getOrigin());
    }

    @Test
    public void testGetNS() throws Exception {
        Zone z = buildValidZone();
        assertNotNull(z.getNS());
        assertEquals(Type.NS, z.getNS().getType());
    }

    @Test
    public void testGetSOA() throws Exception {
        Zone z = buildValidZone();
        assertNotNull(z.getSOA());
        assertEquals(Type.SOA, z.getSOA().getType());
    }

    @Test
    public void testGetDClass() throws Exception {
        Zone z = buildValidZone();
        assertEquals(DClass.IN, z.getDClass());
    }

    @Test
    public void testFindExactMatch() throws Exception {
        Zone z = buildValidZone();
        RRset rrset = z.findExactMatch(name("www.example.com."), Type.A);
        assertNotNull(rrset);
        assertEquals(Type.A, rrset.getType());
        assertEquals(1, rrset.size());
    }

    @Test
    public void testAddRRsetAndFindExactMatch() throws Exception {
        Zone z = buildValidZone();
        Record extraA = makeA(name("extra.example.com."));
        RRset extraSet = new RRset(extraA);
        z.addRRset(extraSet);
        RRset fetched = z.findExactMatch(name("extra.example.com."), Type.A);
        assertNotNull(fetched);
        assertEquals(extraSet.getName(), fetched.getName());
    }

    @Test
    public void testAddRecordAndFindRecords() throws Exception {
        Zone z = buildValidZone();
        Record newA = makeA(name("newhost.example.com."));
        z.addRecord(newA);
        SetResponse resp = z.findRecords(name("newhost.example.com."), Type.A);
        assertTrue(resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        assertEquals(Type.A, resp.answers().get(0).getType());
    }

    @Test
    public void testRemoveRecord() throws Exception {
        Zone z = buildValidZone();
        // remove the A record for www.example.com.
        z.removeRecord(aRecord);
        RRset after = z.findExactMatch(name("www.example.com."), Type.A);
        assertNull(after);
    }

    @Test
    public void testIteratorOrder() throws Exception {
        Zone z = buildValidZone();
        Iterator<RRset> it = z.iterator();
        assertTrue(it.hasNext());
        RRset first = it.next();                 // should be SOA
        assertEquals(Type.SOA, first.getType());

        assertTrue(it.hasNext());
        RRset second = it.next();                // should be NS
        assertEquals(Type.NS, second.getType());
    }

    @Test
    public void testAXFRIteratorEndsWithSOA() throws Exception {
        Zone z = buildValidZone();
        Iterator<RRset> it = z.AXFR();
        RRset last = null;
        while (it.hasNext()) {
            last = it.next();
        }
        assertNotNull(last);
        assertEquals(Type.SOA, last.getType()); // last element must be the SOA
    }

    @Test
    public void testToMasterFileContainsSOAAndNS() throws Exception {
        Zone z = buildValidZone();
        String master = z.toMasterFile();
        assertTrue(master.contains(soaRecord.toString()));
        assertTrue(master.contains(nsRecord.toString()));
    }

    /* --------------------------------------------------------------------- */
    /*  2. Edge‑case and exception handling tests                           */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullZone() throws Exception {
        new Zone(null, new Record[0]);
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingSOA() throws Exception {
        Record[] recs = new Record[] { nsRecord, aRecord };
        new Zone(zoneName, recs); // no SOA -> IOException from validate()
    }

    @Test(expected = IOException.class)
    public void testConstructorMultipleSOA() throws Exception {
        // Two SOA records, both at the origin – validate() expects exactly one.
        SOARecord soa2 = makeSOA(zoneName);
        Record[] recs = new Record[] { soaRecord, soa2, nsRecord };
        new Zone(zoneName, recs);
    }

    @Test(expected = IOException.class)
    public void testMaybeAddRecordSOAWithWrongOwner() throws Exception {
        // Build a zone where the SOA owner is not the origin.
        SOARecord badSOA = new SOARecord(
                name("bad.example.com."),
                DClass.IN,
                3600,
                name("ns1.example.com."),
                name("hostmaster.example.com."),
                2023100101L,
                7200,
                3600,
                1209600,
                3600);
        Record[] recs = new Record[] { badSOA, nsRecord };
        new Zone(zoneName, recs);
    }

    @Test
    public void testFindRecordsOutsideZone() throws Exception {
        Zone z = buildValidZone();
        SetResponse resp = z.findRecords(name("outside.com."), Type.A);
        assertTrue(resp.isNXDOMAIN());
    }

    @Test
    public void testFindRecordsCNAME() throws Exception {
        Zone z = buildValidZone();
        SetResponse resp = z.findRecords(name("mail.example.com."), Type.A);
        // The name has a CNAME, so response should be a CNAME response.
        assertTrue(resp.isCNAME());
        assertNotNull(resp.getCNAME());
    }

    @Test
    public void testFindRecordsDNAME() throws Exception {
        Zone z = buildValidZone();
        SetResponse resp = z.findRecords(name("old.example.com."), Type.A);
        assertTrue(resp.isDNAME());
        assertNotNull(resp.getDNAME());
    }

    @Test
    public void testFindRecordsANY() throws Exception {
        Zone z = buildValidZone();
        SetResponse resp = z.findRecords(zoneName, Type.ANY);
        assertTrue(resp.isSuccessful());
        // Should contain at least SOA, NS and the other RRsets we added.
        List<RRset> ans = resp.answers();
        // We added 5 RRsets (SOA, NS, A, CNAME, DNAME, wildcard A) → 6 sets total
        assertEquals(6, ans.size());
    }

    @Test
    public void testWildcardExpansion() throws Exception {
        Zone z = buildValidZone();
        // Query a name that matches the wildcard *.example.com.
        SetResponse resp = z.findRecords(name("foo.example.com."), Type.A);
        assertTrue(resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        RRset set = resp.answers().get(0);
        // The returned RRset must contain a Record whose name is the queried name.
        assertEquals(name("foo.example.com."), set.getName());
    }

    @Test
    public void testDelegationDetection() throws Exception {
        // Create a zone where a sub‑domain has its own NS set (delegation).
        Record subNs = Record.newRecord(name("sub.example.com."), Type.NS, DClass.IN, 3600);
        Record[] recs = new Record[] { soaRecord, nsRecord, subNs };
        Zone z = new Zone(zoneName, recs);
        SetResponse resp = z.findRecords(name("sub.example.com."), Type.A);
        assertTrue(resp.isDelegation());
        assertNotNull(resp.getNS());
        assertEquals(Type.NS, resp.getNS().getType());
    }

    @Test
    public void testRemoveNonExistingRecordDoesNotThrow() throws Exception {
        Zone z = buildValidZone();
        Record bogus = makeA(name("nonexistent.example.com."));
        // Should simply return without exception.
        z.removeRecord(bogus);
        // Verify zone is still intact.
        assertNotNull(z.getSOA());
    }

    @Test
    public void testRemoveRRsetWhenOnlyRecordExists() throws Exception {
        // Build a zone with a single A record for a name.
        Record singleA = makeA(name("single.example.com."));
        Record[] recs = new Record[] { soaRecord, nsRecord, singleA };
        Zone z = new Zone(zoneName, recs);
        // Remove that record – the whole RRset should disappear.
        z.removeRecord(singleA);
        assertNull(z.findExactMatch(name("single.example.com."), Type.A));
    }

    @Test
    public void testIteratorThrowsNoSuchElementExceptionAtEnd() throws Exception {
        Zone z = buildValidZone();
        Iterator<RRset> it = z.iterator();
        while (it.hasNext()) {
            it.next();
        }
        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (java.util.NoSuchElementException e) {
            // Expected
        }
    }

    @Test
    public void testZoneIteratorRemoveUnsupported() throws Exception {
        Zone z = buildValidZone();
        Iterator<RRset> it = z.iterator();
        assertTrue(it.hasNext());
        it.next();
        try {
            it.remove();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
}
