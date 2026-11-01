package org.xbill.DNS;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;

/**
 * Comprehensive JUnit‑4 test suite for {@link Zone}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction and validation (null zone, missing SOA, multiple SOA, SOA name mismatch)</li>
 *   <li>Public getters (origin, SOA, NS, DClass)</li>
 *   <li>Lookup behaviour (exact match, NXRRSET, NXDOMAIN, CNAME, DNAME, delegation, ANY, wildcard)</li>
 *   <li>Record manipulation (addRecord, addRRset, removeRecord)</li>
 *   <li>Iterators (order, AXFR final SOA, remove() unsupported)</li>
 *   <li>String representation (toMasterFile / toString)</li>
 *   <li>Edge cases (records outside the zone, wildcard flag, findExactMatch null)</li>
 * </ul>
 *
 * The test uses only the public API shown in the problem statement together with the
 * {@code Type} and {@code DClass} constants from the dnsjava library.
 */
public class ZoneTest {

    /* --------------------------------------------------------------------- */
    /*  Test fixtures – names used in many tests                              */
    /* --------------------------------------------------------------------- */
    private static final Name ORIGIN;
    private static final Name SUBDOMAIN;
    private static final Name WILDCARD;
    private static final Name CNAME_NAME;
    private static final Name CNAME_TARGET;
    private static final Name DNAME_NAME;
    private static final Name DNAME_TARGET;
    private static final Name A_NAME;
    private static final Name OUTSIDE;

    static {
        try {
            ORIGIN = Name.fromString("example.com.");
            SUBDOMAIN = Name.fromString("sub.example.com.");
            WILDCARD = Name.fromString("*.example.com.");
            CNAME_NAME = Name.fromString("cname.example.com.");
            CNAME_TARGET = Name.fromString("target.example.com.");
            DNAME_NAME = Name.fromString("dname.example.com.");
            DNAME_TARGET = Name.fromString("other.example.com.");
            A_NAME = Name.fromString("www.example.com.");
            OUTSIDE = Name.fromString("outside.com.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods to create records                                      */
    /* --------------------------------------------------------------------- */
    private SOARecord createSOA(Name name) {
        // Serial numbers etc. are arbitrary but must be positive.
        return new SOARecord(name, DClass.IN, 3600,
                Name.fromString("ns1.example.com."),
                Name.fromString("hostmaster.example.com."),
                2021010101L, 7200, 3600, 1209600, 3600);
    }

    private Record createNS(Name name) {
        // Use the generic factory – we only need a record of type NS.
        return Record.newRecord(name, Type.NS, DClass.IN, 3600);
    }

    private Record createA(Name name) {
        return Record.newRecord(name, Type.A, DClass.IN, 3600);
    }

    private Record createCNAME(Name name) {
        return Record.newRecord(name, Type.CNAME, DClass.IN, 3600);
    }

    private Record createDNAME(Name name) {
        return Record.newRecord(name, Type.DNAME, DClass.IN, 3600);
    }

    /** A small zone containing SOA, NS, an A and an MX record at the origin. */
    private Zone createSimpleZone() throws IOException {
        Record[] records = new Record[] {
                createSOA(ORIGIN),
                createNS(ORIGIN),
                createA(A_NAME),
                // MX record – generic factory with type 15 (MX)
                Record.newRecord(Name.fromString("mail.example.com."), Type.MX, DClass.IN, 3600)
        };
        return new Zone(ORIGIN, records);
    }

    /* --------------------------------------------------------------------- */
    /*  Construction / validation tests                                       */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullZone() throws IOException {
        new Zone(null, new Record[0]);
    }

    @Test
    public void testConstructorMissingSOA() {
        Record[] records = new Record[] {
                createNS(ORIGIN),
                createA(A_NAME)
        };
        try {
            new Zone(ORIGIN, records);
            fail("Expected IOException because the zone has no SOA");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testConstructorMultipleSOA() {
        Record[] records = new Record[] {
                createSOA(ORIGIN),
                createSOA(ORIGIN), // second SOA – illegal
                createNS(ORIGIN)
        };
        try {
            new Zone(ORIGIN, records);
            fail("Expected IOException because the zone contains more than one SOA");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testConstructorSOANameMismatch() {
        // SOA owner != zone origin
        SOARecord mismatched = new SOARecord(Name.fromString("mismatch.example.com."),
                DClass.IN, 3600,
                Name.fromString("ns1.example.com."),
                Name.fromString("hostmaster.example.com."),
                2021010101L, 7200, 3600, 1209600, 3600);
        Record[] records = new Record[] {
                mismatched,
                createNS(ORIGIN)
        };
        try {
            new Zone(ORIGIN, records);
            fail("Expected IOException because the SOA owner does not match the zone origin");
        } catch (IOException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Basic getters                                                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetters() throws IOException {
        Zone zone = createSimpleZone();
        assertEquals("Origin must match the one supplied to the constructor", ORIGIN, zone.getOrigin());
        assertNotNull("SOA must be present", zone.getSOA());
        assertEquals("NS set must be present", Type.NS, zone.getNS().getType());
        assertEquals("DClass must be IN", DClass.IN, zone.getDClass());
    }

    /* --------------------------------------------------------------------- */
    /*  Lookup – exact match, NXRRSET, NXDOMAIN, ANY, CNAME, DNAME, etc.    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFindRecordsExactMatch() throws IOException {
        Zone zone = createSimpleZone();
        SetResponse resp = zone.findRecords(A_NAME, Type.A);
        assertTrue("Exact A lookup should be successful", resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        RRset rrset = resp.answers().get(0);
        assertEquals(Type.A, rrset.getType());
        assertEquals(A_NAME, rrset.first().getName());
    }

    @Test
    public void testFindRecordsNxrrset() throws IOException {
        Zone zone = createSimpleZone();
        // www.example.com. has an A record but no MX record
        SetResponse resp = zone.findRecords(A_NAME, Type.MX);
        assertTrue("MX lookup for a name that lacks MX should be NXRRSET", resp.isNXRRSET());
    }

    @Test
    public void testFindRecordsNxdomain() throws IOException {
        Zone zone = createSimpleZone();
        SetResponse resp = zone.findRecords(OUTSIDE, Type.A);
        assertTrue("Lookup for a name outside the zone must be NXDOMAIN", resp.isNXDOMAIN());
    }

    @Test
    public void testFindRecordsCname() throws IOException {
        Record[] records = new Record[] {
                createSOA(ORIGIN),
                createNS(ORIGIN),
                createCNAME(CNAME_NAME)
        };
        Zone zone = new Zone(ORIGIN, records);
        SetResponse resp = zone.findRecords(CNAME_NAME, Type.A);
        assertTrue("CNAME lookup should report a CNAME response", resp.isCNAME());
        assertNotNull("CNAME RRset should be available", resp.getCNAME());
        assertEquals(CNAME_NAME, resp.getCNAME().getName());
    }

    @Test
    public void testFindRecordsDname() throws IOException {
        Record[] records = new Record[] {
                createSOA(ORIGIN),
                createNS(ORIGIN),
                createDNAME(DNAME_NAME)
        };
        Zone zone = new Zone(ORIGIN, records);
        // Query a child of the DNAME name – e.g. foo.dname.example.com.
        Name child = Name.fromString("foo.dname.example.com.");
        SetResponse resp = zone.findRecords(child, Type.A);
        assertTrue("DNAME lookup should report a DNAME response", resp.isDNAME());
        assertNotNull("DNAME RRset should be returned", resp.getDNAME());
        assertEquals(DNAME_NAME, resp.getDNAME().getName());
    }

    @Test
    public void testFindRecordsDelegation() throws IOException {
        Record[] records = new Record[] {
                createSOA(ORIGIN),
                createNS(ORIGIN),
                createNS(SUBDOMAIN) // delegation point
        };
        Zone zone = new Zone(ORIGIN, records);
        SetResponse resp = zone.findRecords(SUBDOMAIN, Type.NS);
        assertTrue("Sub‑domain with NS should be a delegation", resp.isDelegation());
        assertNotNull("Delegation NS set must be present", resp.getNS());
        assertEquals(SUBDOMAIN, resp.getNS().getName());
    }

    @Test
    public void testFindRecordsAny() throws IOException {
        Zone zone = createSimpleZone();
        // ANY on the origin name – should return SOA, NS and any other RRsets at the origin.
        SetResponse resp = zone.findRecords(ORIGIN, Type.ANY);
        assertTrue("ANY query at the zone apex must be successful", resp.isSuccessful());
        // We expect at least SOA and NS; MX and A are attached to other names, so they are not part of the ANY response.
        boolean sawSOA = false, sawNS = false;
        for (RRset r : resp.answers()) {
            if (r.getType() == Type.SOA) sawSOA = true;
            if (r.getType() == Type.NS) sawNS = true;
        }
        assertTrue("SOA must be part of ANY response at the apex", sawSOA);
        assertTrue("NS must be part of ANY response at the apex", sawNS);
    }

    @Test
    public void testWildcardLookup() throws IOException {
        Record[] records = new Record[] {
                createSOA(ORIGIN),
                createNS(ORIGIN),
                createA(WILDCARD) // *.example.com. A record
        };
        Zone zone = new Zone(ORIGIN, records);
        Name query = Name.fromString("foo.example.com.");
        SetResponse resp = zone.findRecords(query, Type.A);
        assertTrue("Wildcard lookup should be successful", resp.isSuccessful());
        RRset rrset = resp.answers().get(0);
        // The record inside the RRset must be renamed to the queried name.
        assertEquals(query, rrset.first().getName());
    }

    /* --------------------------------------------------------------------- */
    /*  Record manipulation – add / remove                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAddRecordMergesIntoExistingRRset() throws IOException {
        Zone zone = createSimpleZone();
        // Add a second A record for the same name
        Record extra = createA(A_NAME);
        zone.addRecord(extra);
        RRset rs = zone.findExactMatch(A_NAME, Type.A);
        assertNotNull("RRset must exist after adding a second record", rs);
        assertEquals("RRset should contain two A records now", 2, rs.size());
    }

    @Test
    public void testRemoveRecordRemovesRRsetWhenLastRecord() throws IOException {
        Zone zone = createSimpleZone();
        RRset before = zone.findExactMatch(A_NAME, Type.A);
        assertNotNull(before);
        assertEquals(1, before.size());

        // Remove the sole A record
        zone.removeRecord(before.first());
        RRset after = zone.findExactMatch(A_NAME, Type.A);
        assertNull("RRset must disappear after its only record is removed", after);
    }

    @Test
    public void testRemoveRecordFromMultiRecordRRset() throws IOException {
        // Build a zone with two A records at the same name.
        Record[] records = new Record[] {
                createSOA(ORIGIN),
                createNS(ORIGIN),
                createA(A_NAME),
                createA(A_NAME) // second A
        };
        Zone zone = new Zone(ORIGIN, records);
        RRset before = zone.findExactMatch(A_NAME, Type.A);
        assertEquals(2, before.size());

        // Remove one of them.
        zone.removeRecord(before.first());
        RRset after = zone.findExactMatch(A_NAME, Type.A);
        assertNotNull(after);
        assertEquals(1, after.size());
    }

    @Test
    public void testAddRRsetAddsNewRRset() throws IOException {
        Zone zone = createSimpleZone();
        Name newName = Name.fromString("new.example.com.");
        RRset newRRset = new RRset(createA(newName));
        zone.addRRset(newRRset);
        RRset fetched = zone.findExactMatch(newName, Type.A);
        assertNotNull("Newly added RRset must be retrievable", fetched);
        assertEquals(1, fetched.size());
        assertEquals(newName, fetched.first().getName());
    }

    /* --------------------------------------------------------------------- */
    /*  Iterators – ordering, AXFR final SOA, unsupported remove                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIteratorOrder() throws IOException {
        Zone zone = createSimpleZone();
        Iterator<RRset> it = zone.iterator();

        // 0 – SOA
        assertTrue(it.hasNext());
        RRset r0 = it.next();
        assertEquals(Type.SOA, r0.getType());

        // 1 – NS
        assertTrue(it.hasNext());
        RRset r1 = it.next();
        assertEquals(Type.NS, r1.getType());

        // 2 – A (www.example.com.)
        assertTrue(it.hasNext());
        RRset r2 = it.next();
        assertEquals(Type.A, r2.getType());

        // 3 – MX (mail.example.com.)
        assertTrue(it.hasNext());
        RRset r3 = it.next();
        assertEquals(Type.MX, r3.getType());

        assertFalse("No more RRsets should be present", it.hasNext());
    }

    @Test
    public void testAXFRIteratorReturnsFinalSOA() throws IOException {
        Zone zone = createSimpleZone();
        Iterator<RRset> axfr = zone.AXFR();

        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
        }
        // The final RRset returned by AXFR must be the SOA.
        assertNotNull("AXFR iterator must return at least one RRset", last);
        assertEquals("Last RRset of AXFR must be the SOA", Type.SOA, last.getType());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveUnsupported() throws IOException {
        Zone zone = createSimpleZone();
        Iterator<RRset> it = zone.iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove(); // must throw
    }

    /* --------------------------------------------------------------------- */
    /*  String representation                                                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToMasterFileContainsCoreRecords() throws IOException {
        Zone zone = createSimpleZone();
        String master = zone.toMasterFile();

        // Very coarse checks – we only need to see that the textual lines are present.
        assertTrue(master.contains(ORIGIN.toString()));
        assertTrue(master.contains("SOA"));
        assertTrue(master.contains("NS"));
        assertTrue(master.contains("www.example.com."));
    }

    @Test
    public void testToStringDelegatesToMasterFile() throws IOException {
        Zone zone = createSimpleZone();
        assertEquals("toString() must be identical to toMasterFile()", zone.toMasterFile(), zone.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Edge cases – findExactMatch, records outside the zone, wildcard flag */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFindExactMatchNonexistent() throws IOException {
        Zone zone = createSimpleZone();
        assertNull("findExactMatch must return null for unknown names", zone.findExactMatch(OUTSIDE, Type.A));
    }

    @Test
    public void testRecordOutsideZoneIsIgnored() throws IOException {
        Record[] records = new Record[] {
                createSOA(ORIGIN),
                createNS(ORIGIN),
                // This record is not a sub‑domain of the zone and must be ignored.
                Record.newRecord(OUTSIDE, Type.A, DClass.IN, 3600)
        };
        Zone zone = new Zone(ORIGIN, records);
        assertNull("Record outside the zone must not be present", zone.findExactMatch(OUTSIDE, Type.A));
    }

    @Test
    public void testWildcardEffectIndirectly() throws IOException {
        // Same fixture as the wildcard test but we also verify that a non‑matching name is NXDOMAIN.
        Record[] records = new Record[] {
                createSOA(ORIGIN),
                createNS(ORIGIN),
                createA(WILDCARD)
        };
        Zone zone = new Zone(ORIGIN, records);

        // Matching wildcard
        Name q1 = Name.fromString("foo.example.com.");
        SetResponse sr1 = zone.findRecords(q1, Type.A);
        assertTrue(sr1.isSuccessful());

        // Non‑matching name (no data and no wildcard covers it)
        Name q2 = Name.fromString("bar.sub.example.com.");
        SetResponse sr2 = zone.findRecords(q2, Type.A);
        assertTrue(sr2.isNXDOMAIN());
    }

    /* --------------------------------------------------------------------- */
    /*  End of test class                                                    */
    /* --------------------------------------------------------------------- */
}
