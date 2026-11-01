###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

/**
 * Comprehensive tests for {@link Zone}. The tests construct real DNS objects
 * (Name, Record, RRset, SOARecord, etc.) and exercise the public API of Zone,
 * including lookup, iteration, wildcard handling and record manipulation.
 */
public class ZoneTest {

    /** Helper to create a Name instance from a string (dot‑terminated). */
    private static Name name(String s) throws TextParseException {
        return Name.fromString(s);
    }

    /** Helper to create a generic Record of the given type. */
    private static Record rec(Name n, int type) {
        // ttl = 3600, dclass = IN
        return Record.newRecord(n, type, DClass.IN, 3600);
    }

    /** Build a minimal zone (origin, SOA, NS) plus any extra records supplied. */
    private static Zone createZone(Record... extra) throws IOException {
        Name origin = name("example.com.");
        // Construct a proper SOA record (host = ns.example.com., admin = hostmaster.example.com.)
        Record soa = new SOARecord(
                origin,
                DClass.IN,
                3600,
                name("ns.example.com."),
                name("hostmaster.example.com."),
                2023101501L,   // serial
                7200,          // refresh
                3600,          // retry
                1209600,       // expire
                3600);         // minimum

        // NS record for the zone apex
        Record ns = rec(origin, Type.NS);
        // Assemble array: SOA, NS, then any extras
        Record[] all = new Record[2 + extra.length];
        all[0] = soa;
        all[1] = ns;
        System.arraycopy(extra, 0, all, 2, extra.length);
        return new Zone(origin, all);
    }

    @Test
    public void testConstructorAndBasicAccessors() throws IOException {
        Zone z = createZone();
        assertEquals(name("example.com."), z.getOrigin());
        assertNotNull(z.getSOA());
        assertNotNull(z.getNS());
        assertEquals(DClass.IN, z.getDClass());
    }

    @Test
    public void testFindExactMatch() throws IOException {
        Record a = rec(name("host.example.com."), Type.A);
        Zone z = createZone(a);
        RRset rs = z.findExactMatch(name("host.example.com."), Type.A);
        assertNotNull(rs);
        assertEquals(Type.A, rs.getType());
        assertEquals(1, rs.size());
    }

    @Test
    public void testFindRecordsSuccessfulLookup() throws IOException {
        Record a = rec(name("www.example.com."), Type.A);
        Zone z = createZone(a);
        SetResponse resp = z.findRecords(name("www.example.com."), Type.A);
        assertTrue(resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        assertEquals(Type.A, resp.answers().get(0).getType());
    }

    @Test
    public void testFindRecordsCNAME() throws IOException {
        // CNAME at alias.example.com -> target.example.com.
        Record cname = rec(name("alias.example.com."), Type.CNAME);
        Zone z = createZone(cname);
        SetResponse resp = z.findRecords(name("alias.example.com."), Type.A);
        assertTrue(resp.isCNAME());
        assertNotNull(resp.getCNAME());
        assertEquals(Type.CNAME, resp.getCNAME().getType());
    }

    @Test
    public void testFindRecordsDelegation() throws IOException {
        // NS record at sub.example.com creates a delegation.
        Record nsDeleg = rec(name("sub.example.com."), Type.NS);
        Zone z = createZone(nsDeleg);
        SetResponse resp = z.findRecords(name("sub.example.com."), Type.A);
        assertTrue(resp.isDelegation());
        assertNotNull(resp.getNS());
        assertEquals(Type.NS, resp.getNS().getType());
    }

    @Test
    public void testWildcardExpansion() throws IOException {
        // *.example.com A record
        Record wild = rec(name("*.example.com."), Type.A);
        Zone z = createZone(wild);
        // Query a name that does not exist explicitly but matches the wildcard
        SetResponse resp = z.findRecords(name("foo.example.com."), Type.A);
        assertTrue(resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        RRset expanded = resp.answers().get(0);
        // The returned RRset should contain a record whose name is the queried name
        assertEquals(name("foo.example.com."), expanded.first().getName());
    }

    @Test
    public void testNXDOMAIN() throws IOException {
        Zone z = createZone();
        SetResponse resp = z.findRecords(name("outside.com."), Type.A);
        assertTrue(resp.isNXDOMAIN());
    }

    @Test
    public void testNXRRSET() throws IOException {
        // Only an A record at host.example.com, request MX which does not exist.
        Record a = rec(name("host.example.com."), Type.A);
        Zone z = createZone(a);
        SetResponse resp = z.findRecords(name("host.example.com."), Type.MX);
        assertTrue(resp.isNXRRSET());
    }

    @Test
    public void testIteratorOrder() throws IOException {
        // Add an extra TXT record to check ordering after SOA and NS.
        Record txt = rec(name("example.com."), Type.TXT);
        Zone z = createZone(txt);
        Iterator<RRset> it = z.iterator();
        // First RRset must be SOA
        assertTrue(it.hasNext());
        RRset first = it.next();
        assertEquals(Type.SOA, first.getType());
        // Second must be NS
        assertTrue(it.hasNext());
        RRset second = it.next();
        assertEquals(Type.NS, second.getType());
        // Remaining RRsets follow (order not guaranteed, but we should see TXT somewhere)
        boolean foundTxt = false;
        while (it.hasNext()) {
            RRset rs = it.next();
            if (rs.getType() == Type.TXT) {
                foundTxt = true;
                break;
            }
        }
        assertTrue("TXT RRset should be present in iterator", foundTxt);
    }

    @Test
    public void testAXFRIteratorEndsWithSOA() throws IOException {
        // Add a second A record to make the zone non‑trivial.
        Record a = rec(name("host.example.com."), Type.A);
        Zone z = createZone(a);
        Iterator<RRset> axfr = z.AXFR();
        // Consume all RRsets, remembering the last one.
        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
        }
        assertNotNull(last);
        // The final RRset in an AXFR must be the zone’s SOA.
        assertEquals(Type.SOA, last.getType());
    }

    @Test
    public void testAddAndRemoveRecord() throws IOException {
        Zone z = createZone();
        Name n = name("newhost.example.com.");
        Record a = rec(n, Type.A);
        // Add the record
        z.addRecord(a);
        RRset rs = z.findExactMatch(n, Type.A);
        assertNotNull(rs);
        assertEquals(1, rs.size());
        // Remove the same record
        z.removeRecord(a);
        assertNull(z.findExactMatch(n, Type.A));
    }

    @Test
    public void testToMasterFileContainsAllRecords() throws IOException {
        Record a = rec(name("host.example.com."), Type.A);
        Record cname = rec(name("cname.example.com."), Type.CNAME);
        Zone z = createZone(a, cname);
        String master = z.toMasterFile();
        // The master file should contain the textual representation of the added records.
        assertTrue(master.contains("host.example.com."));
        assertTrue(master.contains("cname.example.com."));
        // It should also contain the SOA and NS at the apex.
        assertTrue(master.contains("example.com."));
    }
}
###Test END##