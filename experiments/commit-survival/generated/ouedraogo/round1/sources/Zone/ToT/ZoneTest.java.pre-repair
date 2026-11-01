/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Zone}.
 *
 * The tests exercise the public API of {@code Zone} and several internal
 * behaviours (validation, look‑ups, iterators, wildcard handling, etc.).
 *
 * Only classes, constructors and methods that appear in the supplied source
 * or in the listed dependency signatures are used – no mocks or external
 * libraries are required.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive tests for {@link Zone}.
 */
public class ZoneTest {

    /** Common constants used by many tests. */
    private static final Name ORIGIN;
    private static final Name WWW;
    private static final Name ALIAS;
    private static final Name WILD;
    private static final Name SUBZONE;
    private static final Name NS_NAME;
    private static final RRset SOA_RRSET;
    private static final RRset NS_RRSET;

    static {
        try {
            ORIGIN = Name.fromString("example.com.");
            WWW = Name.fromString("www.example.com.");
            ALIAS = Name.fromString("alias.example.com.");
            WILD = Name.fromString("*.example.com.");
            SUBZONE = Name.fromString("sub.example.com.");
            NS_NAME = Name.fromString("ns1.example.com.");

            // Create a minimal SOA record (required for every zone)
            SOA_RRSET = new RRset(
                    new SOARecord(
                            ORIGIN,
                            DClass.IN,
                            3600,
                            Name.fromString("ns1.example.com."),
                            Name.fromString("hostmaster.example.com."),
                            2023100101L,
                            7200,
                            3600,
                            1209600,
                            3600));

            // Create an NS set for the zone apex
            NS_RRSET = new RRset(
                    new NSRecord(
                            ORIGIN,
                            DClass.IN,
                            3600,
                            NS_NAME));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Helper to build a zone from an arbitrary list of records. */
    private Zone buildZone(Record... records) throws IOException {
        // prepend the mandatory SOA and NS records
        Record[] all = new Record[records.length + 2];
        all[0] = SOA_RRSET.first();          // SOA
        all[1] = NS_RRSET.first();           // NS
        System.arraycopy(records, 0, all, 2, records.length);
        return new Zone(ORIGIN, all);
    }

    /** --------------------------------------------------------------------- *
     *  Constructor validation tests
     *  --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullZoneThrows() throws IOException {
        new Zone(null, new Record[0]);
    }

    @Test
    public void testValidateMissingSOAThrows() {
        try {
            // Build a zone that contains only an NS record → validation must fail
            Record[] recs = new Record[] {
                    // No SOA record at the origin
                    Record.newRecord(ORIGIN, Type.NS, DClass.IN, 3600)
            };
            new Zone(ORIGIN, recs);
            fail("Expected IOException because the zone has no SOA");
        } catch (IOException expected) {
            // expected
        }
    }

    @Test
    public void testValidateMultipleSOAThrows() {
        try {
            // Two SOA records – they will be placed in the same RRset (size == 2)
            SOARecord soa1 = new SOARecord(
                    ORIGIN, DClass.IN, 3600,
                    Name.fromString("ns1.example.com."),
                    Name.fromString("hostmaster.example.com."),
                    2023100101L, 7200, 3600, 1209600, 3600);
            SOARecord soa2 = new SOARecord(
                    ORIGIN, DClass.IN, 3600,
                    Name.fromString("ns2.example.com."),
                    Name.fromString("hostmaster.example.com."),
                    2023100102L, 7200, 3600, 1209600, 3600);
            Record[] recs = new Record[] { soa1, soa2, NS_RRSET.first() };
            new Zone(ORIGIN, recs);
            fail("Expected IOException because there are multiple SOA records");
        } catch (IOException expected) {
            // expected
        }
    }

    @Test
    public void testValidateMissingNSThrows() {
        try {
            // Only SOA, no NS at the apex
            SOARecord soa = (SOARecord) SOA_RRSET.first();
            Record[] recs = new Record[] { soa };
            new Zone(ORIGIN, recs);
            fail("Expected IOException because the zone lacks an NS set");
        } catch (IOException expected) {
            // expected
        }
    }

    /** --------------------------------------------------------------------- *
     *  Basic look‑up tests (exact matches)
     *  --------------------------------------------------------------------- */

    @Test
    public void testFindExactMatch() throws IOException {
        // A record for www.example.com.
        Record a = Record.newRecord(WWW, Type.A, DClass.IN, 300);
        Zone z = buildZone(a);

        RRset result = z.findExactMatch(WWW, Type.A);
        assertNotNull("RRset should be found", result);
        assertEquals("RRset type should be A", Type.A, result.getType());
        assertEquals("Exactly one record in the set", 1, result.size());
        assertEquals("Returned record must be the one we added", a, result.first());
    }

    @Test
    public void testFindExactMatchMissing() throws IOException {
        Zone z = buildZone(); // only SOA+NS
        RRset result = z.findExactMatch(WWW, Type.A);
        assertNull("No A record exists, should return null", result);
    }

    /** --------------------------------------------------------------------- *
     *  findRecords – successful, CNAME, delegation, wildcard, NXDOMAIN
     *  --------------------------------------------------------------------- */

    @Test
    public void testFindRecordsExactSuccess() throws IOException {
        Record a = Record.newRecord(WWW, Type.A, DClass.IN, 300);
        Zone z = buildZone(a);

        SetResponse resp = z.findRecords(WWW, Type.A);
        assertTrue("Response must be successful", resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        assertEquals(Type.A, resp.answers().get(0).getType());
    }

    @Test
    public void testFindRecordsCNAME() throws IOException {
        // CNAME from alias.example.com. -> www.example.com.
        Record cname = new CNAMERecord(ALIAS, DClass.IN, 300, WWW);
        Zone z = buildZone(cname, Record.newRecord(WWW, Type.A, DClass.IN, 300));

        SetResponse resp = z.findRecords(ALIAS, Type.A);
        assertTrue("CNAME response expected", resp.isCNAME());
        assertNotNull("CNAME RRset must be present", resp.getCNAME());
        assertEquals(ALIAS, resp.getCNAME().getName());
        assertEquals(WWW, resp.getCNAME().getTarget());
    }

    @Test
    public void testFindRecordsDelegation() throws IOException {
        // Delegation: sub.example.com. has its own NS set
        Record nsSub = new NSRecord(SUBZONE, DClass.IN, 300, NS_NAME);
        Zone z = buildZone(nsSub);

        SetResponse resp = z.findRecords(SUBZONE, Type.A);
        assertTrue("Delegation response expected", resp.isDelegation());
        assertNotNull("Delegation NS set must be present", resp.getNS());
        assertEquals(SUBZONE, resp.getNS().getName());
    }

    @Test
    public void testFindRecordsWildcard() throws IOException {
        // Wildcard A record for *.example.com.
        Record wildA = Record.newRecord(WILD, Type.A, DClass.IN, 300);
        Zone z = buildZone(wildA);

        Name qname = Name.fromString("foo.example.com.");
        SetResponse resp = z.findRecords(qname, Type.A);
        assertTrue("Wildcard expansion should be successful", resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        RRset expanded = resp.answers().get(0);
        // The name of the returned RRset must be the queried name, not the wildcard.
        assertEquals(qname, expanded.getName());
        // The underlying record should have been renamed.
        assertEquals(qname, expanded.first().getName());
    }

    @Test
    public void testFindRecordsNxDomain() throws IOException {
        Zone z = buildZone(); // minimal zone
        Name outside = Name.fromString("outside.com.");
        SetResponse resp = z.findRecords(outside, Type.A);
        assertTrue("Query outside the zone must be NXDOMAIN", resp.isNXDOMAIN());
    }

    /** --------------------------------------------------------------------- *
     *  Iterator and AXFR iterator tests
     *  --------------------------------------------------------------------- */

    @Test
    public void testIteratorOrder() throws IOException {
        // Add an extra A record to ensure more than the mandatory two sets.
        Record a = Record.newRecord(WWW, Type.A, DClass.IN, 300);
        Zone z = buildZone(a);

        Iterator<RRset> it = z.iterator();
        assertTrue("Iterator must have at least one element", it.hasNext());

        // First element must be the SOA of the origin
        RRset first = it.next();
        assertEquals(Type.SOA, first.getType());

        // Second element must be the NS set of the origin
        RRset second = it.next();
        assertEquals(Type.NS, second.getType());

        // Subsequent elements must contain the A record we added
        boolean foundA = false;
        while (it.hasNext()) {
            RRset cur = it.next();
            if (cur.getType() == Type.A && cur.getName().equals(WWW)) {
                foundA = true;
                break;
            }
        }
        assertTrue("Iterator should contain the A record for www", foundA);
    }

    @Test
    public void testAXFRIteratorIncludesTrailingSOA() throws IOException {
        // Add a single extra record so that the zone is non‑trivial.
        Record a = Record.newRecord(WWW, Type.A, DClass.IN, 300);
        Zone z = buildZone(a);

        Iterator<RRset> axfr = z.AXFR();
        // Consume all elements; the last one should be a SOA.
        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
        }
        assertNotNull("There must be at least one element", last);
        assertEquals("The final element of AXFR must be a SOA", Type.SOA, last.getType());
    }

    /** --------------------------------------------------------------------- *
     *  Record addition / removal tests
     *  --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemoveRecord() throws IOException {
        Zone z = buildZone();

        // Add an A record
        Record a = Record.newRecord(WWW, Type.A, DClass.IN, 300);
        z.addRecord(a);
        RRset afterAdd = z.findExactMatch(WWW, Type.A);
        assertNotNull("A record should be present after addition", afterAdd);
        assertEquals(1, afterAdd.size());

        // Remove the same A record
        z.removeRecord(a);
        RRset afterRemove = z.findExactMatch(WWW, Type.A);
        assertNull("A record should be gone after removal", afterRemove);
    }

    @Test
    public void testRemoveRecordWhenMultipleInSet() throws IOException {
        // Two A records for the same name
        Record a1 = Record.newRecord(WWW, Type.A, DClass.IN, 300);
        Record a2 = Record.newRecord(WWW, Type.A, DClass.IN, 300);
        Zone z = buildZone(a1, a2);

        RRset set = z.findExactMatch(WWW, Type.A);
        assertEquals("Two records should be present initially", 2, set.size());

        // Remove one; the RRset should still exist with one record left.
        z.removeRecord(a1);
        RRset afterOneRemoval = z.findExactMatch(WWW, Type.A);
        assertNotNull(afterOneRemoval);
        assertEquals(1, afterOneRemoval.size());
        assertEquals(a2, afterOneRemoval.first());

        // Remove the last one; the RRset should disappear.
        z.removeRecord(a2);
        assertNull(z.findExactMatch(WWW, Type.A));
    }

    /** --------------------------------------------------------------------- *
     *  toMasterFile representation tests
     *  --------------------------------------------------------------------- */

    @Test
    public void testToMasterFileContainsAllRecords() throws IOException {
        Record a = Record.newRecord(WWW, Type.A, DClass.IN, 300);
        Record cname = new CNAMERecord(ALIAS, DClass.IN, 300, WWW);
        Zone z = buildZone(a, cname);

        String master = z.toMasterFile();
        // The master file must contain the textual representation of each record.
        assertTrue(master.contains(WWW.toString()));
        assertTrue(master.contains("A"));
        assertTrue(master.contains(ALIAS.toString()));
        assertTrue(master.contains("CNAME"));
        // Also must contain the mandatory SOA and NS.
        assertTrue(master.contains(ORIGIN.toString()));
        assertTrue(master.contains("SOA"));
        assertTrue(master.contains("NS"));
    }

    /** --------------------------------------------------------------------- *
     *  maybeAddRecord validation (SOA at wrong name)
     *  --------------------------------------------------------------------- */

    @Test
    public void testMaybeAddRecordRejectsSofiaAtWrongOwner() throws IOException {
        // Build a record that is a SOA but not at the zone origin.
        SOARecord wrongSofa = new SOARecord(
                WWW,               // owner not equal to origin
                DClass.IN,
                3600,
                Name.fromString("ns1.example.com."),
                Name.fromString("hostmaster.example.com."),
                2023100101L, 7200, 3600, 1209600, 3600);
        Record[] recs = new Record[] { wrongSofa, NS_RRSET.first() };
        try {
            new Zone(ORIGIN, recs);
            fail("Expected IOException because SOA owner does not match zone origin");
        } catch (IOException expected) {
            // expected
        }
    }

    /** --------------------------------------------------------------------- *
     *  Miscellaneous getters
     *  --------------------------------------------------------------------- */

    @Test
    public void testGetters() throws IOException {
        Zone z = buildZone();
        assertEquals("Origin getter must return the zone origin", ORIGIN, z.getOrigin());
        assertEquals("SOA getter must return the zone's SOA record", SOA_RRSET.first(), z.getSOA());
        assertEquals("NS getter must return the zone's NS set", NS_RRSET.first(), z.getNS().first());
        assertEquals("DClass must be IN", DClass.IN, z.getDClass());
    }
}
