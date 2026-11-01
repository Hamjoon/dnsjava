###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive tests for {@link Zone}.
 *
 * The tests use only the public API that is present in the supplied source code
 * and the minimal set of dependent classes (Name, Record, RRset, SOARecord,
 * SetResponse, Type, DClass, etc.).
 */
public class ZoneTest {

    private Name origin;
    private SOARecord soa;
    private Record nsOrigin;
    private Record aHost;
    private Record cnameHost;
    private Record dnameHost;
    private Record delegationNS;
    private Record wildcardA;
    private Zone zone;

    @Before
    public void setUp() throws Exception {
        // zone origin
        origin = Name.fromString("example.com.");

        // mandatory SOA
        Name host = Name.fromString("ns1.example.com.");
        Name admin = Name.fromString("admin.example.com.");
        soa = new SOARecord(origin, DClass.IN, 3600, host, admin,
                2023100101L, 7200, 3600, 1209600, 3600);
        // mandatory NS at origin
        nsOrigin = Record.newRecord(origin, Type.NS, DClass.IN, 3600);

        // additional records
        aHost = Record.newRecord(Name.fromString("host.example.com."), Type.A, DClass.IN, 3600);
        cnameHost = Record.newRecord(Name.fromString("cname.example.com."), Type.CNAME, DClass.IN, 3600);
        dnameHost = Record.newRecord(Name.fromString("example.com."), Type.DNAME, DClass.IN, 3600);
        delegationNS = Record.newRecord(Name.fromString("deleg.example.com."), Type.NS, DClass.IN, 3600);
        wildcardA = Record.newRecord(Name.fromString("*.example.com."), Type.A, DClass.IN, 3600);

        // Build the zone with all records
        zone = new Zone(origin,
                soa,
                nsOrigin,
                aHost,
                cnameHost,
                dnameHost,
                delegationNS,
                wildcardA);
    }

    // -----------------------------------------------------------------------
    // Basic getters
    // -----------------------------------------------------------------------
    @Test
    public void testGetters() {
        assertEquals(origin, zone.getOrigin());
        assertEquals(soa, zone.getSOA());
        assertEquals(DClass.IN, zone.getDClass());

        RRset nsSet = zone.getNS();
        assertNotNull(nsSet);
        assertEquals(Type.NS, nsSet.getType());
        // The NS set should contain exactly the NS record we added at the origin
        assertEquals(1, nsSet.size());
        assertEquals(nsOrigin, nsSet.first());
    }

    // -----------------------------------------------------------------------
    // addRecord / removeRecord edge cases
    // -----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testAddRecord_Null() {
        zone.addRecord(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecord_SoaNotAtOrigin() throws IOException {
        Name other = Name.fromString("other.com.");
        SOARecord badSoa = new SOARecord(other, DClass.IN, 3600,
                Name.fromString("ns.other.com."), Name.fromString("admin.other.com."),
                1, 7200, 3600, 1209600, 3600);
        zone.addRecord(badSoa);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecord_NameOutsideZone() throws IOException {
        Record out = Record.newRecord(Name.fromString("outside.com."), Type.A, DClass.IN, 3600);
        zone.addRecord(out);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecord_Null() {
        zone.removeRecord(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecord_Soa() {
        zone.removeRecord(soa);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecord_LastNs() {
        // The zone contains exactly one NS at the origin.
        // Attempting to remove it must fail.
        zone.removeRecord(nsOrigin);
    }

    @Test
    public void testAddAndRemoveNonCriticalRecord() throws IOException {
        Record extra = Record.newRecord(Name.fromString("extra.example.com."), Type.A, DClass.IN, 3600);
        zone.addRecord(extra);
        RRset found = zone.findExactMatch(Name.fromString("extra.example.com."), Type.A);
        assertNotNull(found);
        assertEquals(1, found.size());

        zone.removeRecord(extra);
        assertNull(zone.findExactMatch(Name.fromString("extra.example.com."), Type.A));
    }

    // -----------------------------------------------------------------------
    // addRRset / removeRRset API
    // -----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testAddRRset_Null() {
        zone.addRRset(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRset_SoaWrongOrigin() {
        RRset bad = new RRset(new SOARecord(Name.fromString("bad.example.com."), DClass.IN, 3600,
                Name.fromString("ns.bad.example.com."), Name.fromString("admin.bad.example.com."),
                1, 7200, 3600, 1209600, 3600));
        zone.addRRset(bad);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRset_SoaMultiple() {
        RRset multiSoa = new RRset(soa);
        // add a second (dummy) SOA record – still the same name, but size > 1
        SOARecord another = new SOARecord(origin, DClass.IN, 3600,
                Name.fromString("ns2.example.com."), Name.fromString("admin.example.com."),
                2, 7200, 3600, 1209600, 3600);
        multiSoa.addRR(another);
        zone.addRRset(multiSoa);
    }

    @Test
    public void testAddRRset_ReplaceExistingType() throws IOException {
        // Create a new A record for the same name as aHost but with a different TTL.
        Record newA = Record.newRecord(Name.fromString("host.example.com."), Type.A, DClass.IN, 7200);
        RRset newSet = new RRset(newA);
        zone.addRRset(newSet);

        RRset after = zone.findExactMatch(Name.fromString("host.example.com."), Type.A);
        assertNotNull(after);
        assertEquals(1, after.size());
        assertEquals(7200, after.first().getTTL());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRRset_NullName() {
        zone.removeRRset(null, Type.A);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRRset_InvalidType_SOA() {
        zone.removeRRset(origin, Type.SOA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRRset_InvalidType_NS() {
        zone.removeRRset(origin, Type.NS);
    }

    @Test
    public void testRemoveRRset_Existing() throws IOException {
        // Add a TXT record and then remove it via removeRRset
        Record txt = Record.newRecord(Name.fromString("txt.example.com."), Type.TXT, DClass.IN, 3600);
        zone.addRecord(txt);
        assertNotNull(zone.findExactMatch(Name.fromString("txt.example.com."), Type.TXT));

        zone.removeRRset(Name.fromString("txt.example.com."), Type.TXT);
        assertNull(zone.findExactMatch(Name.fromString("txt.example.com."), Type.TXT));
    }

    // -----------------------------------------------------------------------
    // findExactMatch
    // -----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testFindExactMatch_NullName() {
        zone.findExactMatch(null, Type.A);
    }

    @Test
    public void testFindExactMatch_Existing() {
        RRset rs = zone.findExactMatch(Name.fromString("host.example.com."), Type.A);
        assertNotNull(rs);
        assertEquals(1, rs.size());
        assertEquals(aHost, rs.first());

        // Ensure a copy is returned (modifying it must not affect the zone)
        rs.clear();
        assertEquals(1, zone.findExactMatch(Name.fromString("host.example.com."), Type.A).size());
    }

    @Test
    public void testFindExactMatch_NonExisting() {
        assertNull(zone.findExactMatch(Name.fromString("nosuch.example.com."), Type.A));
    }

    // -----------------------------------------------------------------------
    // findRecords – various response types
    // -----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testFindRecords_NullName() {
        zone.findRecords(null, Type.A);
    }

    @Test
    public void testFindRecords_SuccessfulExact() {
        SetResponse sr = zone.findRecords(Name.fromString("host.example.com."), Type.A);
        assertTrue(sr.isSuccessful());
        assertEquals(1, sr.answers().size());
        assertEquals(aHost, sr.answers().get(0).first());
    }

    @Test
    public void testFindRecords_CNAMEFallback() {
        SetResponse sr = zone.findRecords(Name.fromString("cname.example.com."), Type.A);
        assertTrue(sr.isCNAME());
        assertEquals(cnameHost, sr.getCNAME());
    }

    @Test
    public void testFindRecords_DNAMEFallback() {
        // DNAME is at the origin; any non‑origin name that is not exact should hit it.
        SetResponse sr = zone.findRecords(Name.fromString("anysub.example.com."), Type.A);
        assertTrue(sr.isDNAME());
        assertEquals(dnameHost, sr.getDNAME());
    }

    @Test
    public void testFindRecords_Delegation() {
        SetResponse sr = zone.findRecords(Name.fromString("deleg.example.com."), Type.A);
        assertTrue(sr.isDelegation());
        RRset nsSet = sr.getNS();
        assertNotNull(nsSet);
        assertEquals(Type.NS, nsSet.getType());
        assertEquals(delegationNS, nsSet.first());
    }

    @Test
    public void testFindRecords_NXRRSET() {
        // host.example.com. exists, but we ask for AAAA which is not present.
        SetResponse sr = zone.findRecords(Name.fromString("host.example.com."), Type.AAAA);
        assertTrue(sr.isNXRRSET());
    }

    @Test
    public void testFindRecords_NXDOMAIN() {
        // Name outside the zone
        SetResponse sr = zone.findRecords(Name.fromString("outside.com."), Type.A);
        assertTrue(sr.isNXDOMAIN());
    }

    @Test
    public void testFindRecords_WildcardExpansion() {
        // Query a name that matches the wildcard *.example.com.
        SetResponse sr = zone.findRecords(Name.fromString("wildhost.example.com."), Type.A);
        assertTrue(sr.isSuccessful());
        assertEquals(1, sr.answers().size());
        RRset expanded = sr.answers().get(0);
        // The returned RRset must contain a record whose name is the queried name,
        // not the literal wildcard name.
        Record expandedRec = expanded.first();
        assertEquals(Name.fromString("wildhost.example.com."), expandedRec.getName());
    }

    @Test
    public void testFindRecords_AnyType() {
        SetResponse sr = zone.findRecords(Name.fromString("host.example.com."), Type.ANY);
        assertTrue(sr.isSuccessful());
        // ANY should return all RRsets for that name (A only in this test)
        assertEquals(1, sr.answers().size());
        assertEquals(Type.A, sr.answers().get(0).getType());
    }

    // -----------------------------------------------------------------------
    // iterator and AXFR
    // -----------------------------------------------------------------------
    @Test
    public void testIterator_Order() {
        Iterator<RRset> it = zone.iterator();
        List<RRset> list = new ArrayList<>();
        while (it.hasNext()) {
            list.add(it.next());
        }

        // The first two RRsets must be SOA and NS of the origin (in that order)
        assertEquals(Type.SOA, list.get(0).getType());
        assertEquals(Type.NS, list.get(1).getType());

        // Verify that the zone's SOA instance is the same as the one we added
        assertEquals(soa, list.get(0).first());
    }

    @Test
    public void testAXFR_LastSOA() {
        Iterator<RRset> axfr = zone.AXFR();
        List<RRset> all = new ArrayList<>();
        while (axfr.hasNext()) {
            all.add(axfr.next());
        }

        // The last element must be the SOA again
        assertEquals(Type.SOA, all.get(all.size() - 1).getType());
        // The first element is also the SOA
        assertEquals(Type.SOA, all.get(0).getType());
    }

    @Test
    public void testIteratorRemove() throws IOException {
        // Add a temporary TXT record so we have something removable
        Record txt = Record.newRecord(Name.fromString("removable.example.com."), Type.TXT, DClass.IN, 3600);
        zone.addRecord(txt);

        Iterator<RRset> it = zone.iterator();
        RRset target = null;
        while (it.hasNext()) {
            RRset rs = it.next();
            if (rs.getType() == Type.TXT) {
                target = rs;
                break;
            }
        }
        assertNotNull(target);
        // remove via iterator
        it.remove();

        // The TXT RRset should no longer be present
        assertNull(zone.findExactMatch(Name.fromString("removable.example.com."), Type.TXT));
    }

    // -----------------------------------------------------------------------
    // toMasterFile / toString
    // -----------------------------------------------------------------------
    @Test
    public void testToMasterFile_IncludesAllRecords() {
        String master = zone.toMasterFile();
        // Simple sanity checks – the string should contain the textual representation of
        // the SOA, the NS at the origin, and the wildcard A record.
        assertTrue(master.contains(soa.toString()));
        assertTrue(master.contains(nsOrigin.toString()));
        assertTrue(master.contains(wildcardA.toString()));
    }

    @Test
    public void testToString_DelegatesToMasterFile() {
        assertEquals(zone.toMasterFile(), zone.toString());
    }
}
###Test END##