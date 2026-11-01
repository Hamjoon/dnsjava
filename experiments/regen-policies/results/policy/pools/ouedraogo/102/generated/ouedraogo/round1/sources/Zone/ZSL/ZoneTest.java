/*
 * ZoneTest.java - JUnit4 test suite for org.xbill.DNS.Zone
 *
 * The tests exercise all public behaviour of Zone, including constructors,
 * record manipulation, look‑ups, iterators and the textual representation.
 *
 * NOTE: The test uses only the classes and members listed in the problem
 * statement (Zone, RRset, Record, Name, SOARecord, ZoneTransferIn,
 * Master, SetResponse and the inner ZoneIterator).  All DNS type constants
 * are expressed as their integer values to avoid a direct dependency on the
 * org.xbill.DNS.Type class.
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link Zone}.
 */
public class ZoneTest {

    /** Common zone origin used in most tests. */
    private Name origin;

    /** A minimal valid SOA record for the test zone. */
    private SOARecord soa;

    /** A minimal NS record for the origin. */
    private Record nsRecord;

    /** Helper to create a fresh zone with a single SOA and a single NS. */
    private Zone createBasicZone() throws IOException {
        return new Zone(origin, soa, nsRecord);
    }

    @Before
    public void setUp() throws Exception {
        // origin = example.com.
        origin = Name.fromString("example.com.");

        // SOA: name = origin, host = ns.example.com., admin = hostmaster.example.com.
        Name host = Name.fromString("ns.example.com.");
        Name admin = Name.fromString("hostmaster.example.com.");
        soa = new SOARecord(
                origin,          // name
                DClass.IN,      // class
                3600,           // ttl
                host,           // primary name server
                admin,          // responsible person
                2023100101L,    // serial
                7200,           // refresh
                3600,           // retry
                1209600,        // expire
                3600);          // minimum

        // NS record for the zone origin
        nsRecord = Record.newRecord(origin, 2 /* Type.NS */, DClass.IN, 3600);
    }

    /* ----------------------------------------------------------------------
     * Constructor validation
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullOrigin() throws IOException {
        new Zone(null, (Record[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullRecordsArray() throws IOException {
        new Zone(origin, (Record[]) null);
    }

    @Test(expected = IOException.class)
    public void testConstructor_MissingSOA() throws IOException {
        // Only NS, no SOA – should fail validation.
        new Zone(origin, nsRecord);
    }

    @Test(expected = IOException.class)
    public void testConstructor_MissingNS() throws IOException {
        // Only SOA, no NS – should fail validation.
        new Zone(origin, soa);
    }

    @Test
    public void testConstructor_ValidFromRecordsArray() throws IOException {
        Zone z = createBasicZone();
        assertNotNull(z);
        assertEquals(origin, z.getSOA().getName());
        assertEquals(soa, z.getSOA());
        assertNotNull(z.getNS());
        assertEquals(1, z.getNS().size());
    }

    @Test
    public void testConstructor_FromMasterInputStream() throws IOException {
        // Build a tiny master file in memory: SOA then NS.
        String masterData =
                "; tiny master file\n" +
                "example.com. 3600 IN SOA ns.example.com. hostmaster.example.com. (\n" +
                "                2023100101 7200 3600 1209600 3600 )\n" +
                "example.com. 3600 IN NS ns.example.com.\n";
        ByteArrayInputStream in = new ByteArrayInputStream(masterData.getBytes());
        Zone z = new Zone(origin, in);
        assertEquals(soa.getSerial(), z.getSOA().getSerial());
        assertEquals(1, z.getNS().size());
    }

    /* ----------------------------------------------------------------------
     * addRecord / removeRecord
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddRecord_NewType() throws IOException {
        Zone z = createBasicZone();

        // Add an A record at www.example.com.
        Name www = Name.fromString("www.example.com.");
        Record a = Record.newRecord(www, 1 /* Type.A */, DClass.IN, 300);
        z.addRecord(a);

        RRset rs = z.findExactMatch(www, 1);
        assertNotNull(rs);
        assertEquals(1, rs.size());
        assertEquals(a, rs.first());
    }

    @Test
    public void testAddRecord_ReplaceSOA() throws IOException {
        Zone z = createBasicZone();

        // New SOA with different serial.
        SOARecord newSoa = new SOARecord(
                origin,
                DClass.IN,
                3600,
                Name.fromString("ns2.example.com."),
                Name.fromString("admin.example.com."),
                2023100201L,
                7200, 3600, 1209600, 3600);
        z.addRecord(newSoa);

        assertEquals(2023100201L, z.getSOA().getSerial());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecord_NullRecord() throws IOException {
        Zone z = createBasicZone();
        z.addRecord(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecord_SOAWithWrongOwner() throws IOException {
        Zone z = createBasicZone();
        // SOA owner does NOT match zone origin.
        Name other = Name.fromString("other.com.");
        SOARecord badSoa = new SOARecord(
                other,
                DClass.IN,
                3600,
                Name.fromString("ns.other.com."),
                Name.fromString("admin.other.com."),
                1L,
                7200, 3600, 1209600, 3600);
        z.addRecord(badSoa);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecord_RecordOutsideZone() throws IOException {
        Zone z = createBasicZone();
        Name external = Name.fromString("outside.com.");
        Record a = Record.newRecord(external, 1 /* A */, DClass.IN, 300);
        z.addRecord(a);
    }

    @Test
    public void testRemoveRecord_NonLastNS() throws IOException {
        Zone z = createBasicZone();

        // Add a second NS so we can remove the first one safely.
        Name extraNs = Name.fromString("ns2.example.com.");
        Record extra = Record.newRecord(origin, 2 /* NS */, DClass.IN, 3600);
        z.addRecord(extra);

        // Now remove the original NS.
        z.removeRecord(nsRecord);
        RRset nsSet = z.getNS();
        assertEquals(1, nsSet.size());
        assertEquals(extra, nsSet.first());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecord_LastNS() throws IOException {
        Zone z = createBasicZone();
        // Attempt to remove the only NS – should fail.
        z.removeRecord(nsRecord);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecord_SOA() throws IOException {
        Zone z = createBasicZone();
        // Attempt to remove the SOA – should fail.
        z.removeRecord(soa);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecord_Null() throws IOException {
        Zone z = createBasicZone();
        z.removeRecord(null);
    }

    /* ----------------------------------------------------------------------
     * Exact‑match look‑up
     * ---------------------------------------------------------------------- */

    @Test
    public void testFindExactMatch_Found() throws IOException {
        Zone z = createBasicZone();
        Name www = Name.fromString("www.example.com.");
        Record a = Record.newRecord(www, 1 /* A */, DClass.IN, 300);
        z.addRecord(a);

        RRset rs = z.findExactMatch(www, 1);
        assertNotNull(rs);
        assertEquals(1, rs.size());
        assertEquals(a, rs.first());
    }

    @Test
    public void testFindExactMatch_NotFound() throws IOException {
        Zone z = createBasicZone();
        Name notThere = Name.fromString("nosuch.example.com.");
        assertNull(z.findExactMatch(notThere, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindExactMatch_NullName() throws IOException {
        Zone z = createBasicZone();
        z.findExactMatch(null, 1);
    }

    /* ----------------------------------------------------------------------
     * General findRecords (covers CNAME, DNAME, wildcards, NXDOMAIN, etc.)
     * ---------------------------------------------------------------------- */

    @Test
    public void testFindRecords_NXDOMAIN() throws IOException {
        Zone z = createBasicZone();
        Name out = Name.fromString("outside.example.org.");
        SetResponse resp = z.findRecords(out, 1);
        assertTrue(resp.isNXDOMAIN());
    }

    @Test
    public void testFindRecords_SuccessfulExact() throws IOException {
        Zone z = createBasicZone();
        Name www = Name.fromString("www.example.com.");
        Record a = Record.newRecord(www, 1 /* A */, DClass.IN, 300);
        z.addRecord(a);

        SetResponse resp = z.findRecords(www, 1);
        assertTrue(resp.isSuccessful());
        List<RRset> ans = resp.answers();
        assertEquals(1, ans.size());
        assertEquals(a, ans.get(0).first());
    }

    @Test
    public void testFindRecords_CNAME() throws IOException {
        Zone z = createBasicZone();
        Name alias = Name.fromString("alias.example.com.");
        Name target = Name.fromString("target.example.com.");
        // CNAME record
        Record cname = Record.newRecord(alias, 5 /* CNAME */, DClass.IN, 300);
        z.addRecord(cname);
        // A record for the target
        Record a = Record.newRecord(target, 1 /* A */, DClass.IN, 300);
        z.addRecord(a);

        SetResponse resp = z.findRecords(alias, 1);
        assertTrue(resp.isCNAME());
        assertEquals(cname, resp.getCNAME());
    }

    @Test
    public void testFindRecords_DNAME() throws IOException {
        Zone z = createBasicZone();
        Name sub = Name.fromString("sub.example.com.");
        // DNAME record at example.com. pointing to new.example.org.
        Name dnameTarget = Name.fromString("new.example.org.");
        Record dname = Record.newRecord(origin, 39 /* DNAME */, DClass.IN, 300);
        z.addRecord(dname);
        // DNAME lookup for a name beneath the origin should return DNAME.
        SetResponse resp = z.findRecords(sub, 1);
        assertTrue(resp.isDNAME());
        assertEquals(dname, resp.getDNAME());
    }

    @Test
    public void testFindRecords_WildcardExpansion() throws IOException {
        Zone z = createBasicZone();
        // Add a wildcard A record: *.example.com. 300 IN A
        Name wildcard = Name.fromString("*.example.com.");
        Record aWildcard = Record.newRecord(wildcard, 1 /* A */, DClass.IN, 300);
        z.addRecord(aWildcard);

        // Query a concrete name beneath the origin.
        Name foo = Name.fromString("foo.example.com.");
        SetResponse resp = z.findRecords(foo, 1);
        assertTrue(resp.isSuccessful());

        // The answer should be a copy of the wildcard record with the queried name.
        RRset ans = resp.answers().get(0);
        Record expanded = ans.first();
        assertEquals(foo, expanded.getName());
        assertEquals(1, expanded.getType());
    }

    @Test
    public void testFindRecords_AnyType() throws IOException {
        Zone z = createBasicZone();
        Name www = Name.fromString("www.example.com.");
        Record a = Record.newRecord(www, 1 /* A */, DClass.IN, 300);
        Record txt = Record.newRecord(www, 16 /* TXT */, DClass.IN, 300);
        z.addRecord(a);
        z.addRecord(txt);

        SetResponse resp = z.findRecords(www, 255 /* Type.ANY */);
        assertTrue(resp.isSuccessful());
        // ANY should return both RRsets (A and TXT)
        assertEquals(2, resp.answers().size());
    }

    /* ----------------------------------------------------------------------
     * Iterator and AXFR behaviour
     * ---------------------------------------------------------------------- */

    @Test
    public void testIterator_OrderAndContents() throws IOException {
        Zone z = createBasicZone();

        // Add a third RRset (A) at www.example.com.
        Name www = Name.fromString("www.example.com.");
        Record a = Record.newRecord(www, 1 /* A */, DClass.IN, 300);
        z.addRecord(a);

        Iterator<RRset> it = z.iterator();

        // First element must be the SOA of the origin.
        assertTrue(it.hasNext());
        RRset first = it.next();
        assertEquals(Type.SOA, first.getType());

        // Second element must be the NS of the origin.
        assertTrue(it.hasNext());
        RRset second = it.next();
        assertEquals(Type.NS, second.getType());

        // Remaining elements (order not specified) must include the A record.
        boolean foundA = false;
        while (it.hasNext()) {
            RRset rs = it.next();
            if (rs.getType() == 1 /* A */) {
                foundA = true;
                assertEquals(www, rs.getName());
            }
        }
        assertTrue("A record should be present in iterator", foundA);
    }

    @Test
    public void testAXFR_EndsWithSOA() throws IOException {
        Zone z = createBasicZone();

        // Add a second RRset to make the iteration non‑trivial.
        Name www = Name.fromString("www.example.com.");
        Record a = Record.newRecord(www, 1 /* A */, DClass.IN, 300);
        z.addRecord(a);

        Iterator<RRset> axfr = z.AXFR();

        // Consume all elements, remember the last one.
        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
        }

        // The final RRset must be the SOA (again).
        assertNotNull(last);
        assertEquals(Type.SOA, last.getType());
        assertEquals(origin, last.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void testIterator_RemoveWithoutNext() throws IOException {
        Zone z = createBasicZone();
        Iterator<RRset> it = z.iterator();
        // Directly call remove() before any next().
        it.remove();
    }

    @Test
    public void testIterator_RemoveNSViaIterator() throws IOException {
        Zone z = createBasicZone();

        // Add a second NS to allow removal of the first via iterator.
        Record ns2 = Record.newRecord(origin, 2 /* NS */, DClass.IN, 3600);
        z.addRecord(ns2);

        Iterator<RRset> it = z.iterator();

        // First is SOA, second is NS – remove the NS.
        it.next(); // SOA
        RRset nsSet = it.next(); // NS
        it.remove();

        // After removal only one NS (the second we added) should remain.
        RRset remainingNs = z.getNS();
        assertEquals(1, remainingNs.size());
        assertEquals(ns2, remainingNs.first());
    }

    /* ----------------------------------------------------------------------
     * toMasterFile / toString
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMasterFile_ContainsSOAAndNS() throws IOException {
        Zone z = createBasicZone();
        String master = z.toMasterFile();

        // The master file must contain the textual representation of the SOA and NS.
        assertTrue(master.contains("SOA"));
        assertTrue(master.contains("NS"));
        // It must also contain the zone origin.
        assertTrue(master.contains(origin.toString()));
    }

    @Test
    public void testToString_DelegatesToMasterFile() throws IOException {
        Zone z = createBasicZone();
        assertEquals(z.toMasterFile(), z.toString());
    }

    /* ----------------------------------------------------------------------
     * addRRset / removeRRset (direct RRset manipulation)
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddRRset_ReplacesExisting() throws IOException {
        Zone z = createBasicZone();

        // Build a new NS RRset (different NS name) and add it.
        Record newNs = Record.newRecord(origin, 2 /* NS */, DClass.IN, 3600);
        RRset nsSet = new RRset(newNs);
        z.addRRset(nsSet);

        // The zone's NS set should now contain only the new record.
        RRset stored = z.getNS();
        assertEquals(1, stored.size());
        assertEquals(newNs, stored.first());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRset_Null() throws IOException {
        Zone z = createBasicZone();
        z.addRRset(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRset_NSNotSubdomain() throws IOException {
        Zone z = createBasicZone();
        // Name outside the zone.
        Name other = Name.fromString("other.com.");
        Record ns = Record.newRecord(other, 2 /* NS */, DClass.IN, 3600);
        RRset rrset = new RRset(ns);
        z.addRRset(rrset);
    }

    @Test
    public void testRemoveRRset_Valid() throws IOException {
        Zone z = createBasicZone();

        // Add a second type (A) to ensure the removal of it does not affect NS or SOA.
        Name www = Name.fromString("www.example.com.");
        Record a = Record.newRecord(www, 1 /* A */, DClass.IN, 300);
        z.addRecord(a);

        // Remove the A RRset.
        z.removeRRset(www, 1 /* A */);

        // Verify that the A record is gone, while NS and SOA remain.
        assertNull(z.findExactMatch(www, 1));
        assertNotNull(z.getNS());
        assertNotNull(z.getSOA());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRRset_NullName() throws IOException {
        Zone z = createBasicZone();
        z.removeRRset(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRRset_RemoveSOA() throws IOException {
        Zone z = createBasicZone();
        z.removeRRset(origin, Type.SOA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRRset_RemoveAllNS() throws IOException {
        Zone z = createBasicZone();
        // Attempt to remove the only NS set.
        z.removeRRset(origin, Type.NS);
    }

    /* ----------------------------------------------------------------------
     * Internal sanity checks (ensure no unexpected exceptions)
     * ---------------------------------------------------------------------- */

    @Test
    public void testFindExactMatch_ReturnCopy() throws IOException {
        Zone z = createBasicZone();
        Name www = Name.fromString("www.example.com.");
        Record a = Record.newRecord(www, 1 /* A */, DClass.IN, 300);
        z.addRecord(a);

        RRset first = z.findExactMatch(www, 1);
        RRset second = z.findExactMatch(www, 1);
        // The two RRset objects must be distinct instances (copy semantics).
        assertNotSame(first, second);
    }
}
