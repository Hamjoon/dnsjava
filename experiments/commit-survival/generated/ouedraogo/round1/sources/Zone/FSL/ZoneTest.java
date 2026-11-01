package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive tests for {@link Zone}.
 *
 * The tests construct a realistic zone using real {@link Name},
 * {@link Record} and {@link SOARecord} objects (no mocking) and
 * verify the behaviour of all public methods, the iterator
 * implementations and the master‑file rendering.
 */
public class ZoneTest {

    private static final int A = Type.A;
    private static final int NS = Type.NS;
    private static final int CNAME = Type.CNAME;
    private static final int DNAME = Type.DNAME;
    private static final int ANY = Type.ANY;
    private static final int IN = DClass.IN;

    private Name origin;
    private Name www;
    private Name alias;
    private Name sub;
    private Name nsSub;
    private Name wildcard;
    private Name dnameNode;
    private SOARecord soaRecord;
    private Zone zone;

    @Before
    public void setUp() throws IOException, TextParseException {
        // zone origin: example.com.
        origin = Name.fromString("example.com.");
        www = Name.fromString("www.example.com.");
        alias = Name.fromString("alias.example.com.");
        sub = Name.fromString("sub.example.com.");
        nsSub = Name.fromString("ns.sub.example.com.");
        wildcard = Name.fromString("*.wild.example.com.");
        dnameNode = Name.fromString("d.example.com.");

        // Build the mandatory SOA record
        soaRecord = new SOARecord(
                origin,
                IN,
                3600,
                Name.fromString("ns.example.com."),
                Name.fromString("hostmaster.example.com."),
                1,          // serial
                7200,       // refresh
                3600,       // retry
                1209600,   // expire
                3600);      // minimum

        // Build an array of records that will be fed to the Zone constructor
        Record[] records = new Record[] {
                // mandatory SOA at the origin
                soaRecord,
                // NS at the origin (delegation point for the zone itself)
                Record.newRecord(origin, NS, IN, 3600),
                // A record for www.example.com.
                Record.newRecord(www, A, IN, 3600),
                // CNAME that points alias.example.com. → www.example.com.
                Record.newRecord(alias, CNAME, IN, 3600),
                // NS record that creates a delegation for sub.example.com.
                Record.newRecord(sub, NS, IN, 3600),
                // DNAME record for d.example.com.
                Record.newRecord(dnameNode, DNAME, IN, 3600),
                // Wildcard A record: *.wild.example.com.
                Record.newRecord(wildcard, A, IN, 3600)
        };

        // Create the Zone from the record array
        zone = new Zone(origin, records);
    }

    @Test
    public void testBasicAccessors() {
        assertEquals("Origin must be the one supplied", origin, zone.getOrigin());
        assertNotNull("SOA must be present", zone.getSOA());
        assertEquals("SOA record must be the one we supplied", soaRecord, zone.getSOA());
        assertNotNull("NS set at the origin must be present", zone.getNS());
        assertEquals("Zone class must be IN", IN, zone.getDClass());
    }

    @Test
    public void testFindExactMatch() {
        RRset aSet = zone.findExactMatch(www, A);
        assertNotNull("Exact A RRset for www must exist", aSet);
        assertEquals("RRset type must be A", A, aSet.getType());
        assertEquals("RRset name must be www.example.com.", www, aSet.getName());
    }

    @Test
    public void testFindRecordsExact() {
        SetResponse resp = zone.findRecords(www, A);
        assertTrue("Lookup of existing A should be successful", resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        RRset set = resp.answers().get(0);
        assertEquals(A, set.getType());
        assertEquals(www, set.getName());
    }

    @Test
    public void testFindRecordsCNAME() {
        SetResponse resp = zone.findRecords(alias, A);
        assertTrue("CNAME response expected", resp.isCNAME());
        assertNotNull("CNAME RRset must be present", resp.getCNAME());
        assertEquals(CNAME, resp.getCNAME().getType());
        assertEquals(alias, resp.getCNAME().getName());
    }

    @Test
    public void testFindRecordsDelegation() {
        // Query a name inside the delegated subtree (sub.example.com.)
        SetResponse resp = zone.findRecords(Name.fromString("host.sub.example.com."), A);
        assertTrue("Delegation response expected", resp.isDelegation());
        RRset nsSet = resp.getNS();
        assertNotNull("Delegation must contain NS set", nsSet);
        assertEquals(NS, nsSet.getType());
        assertEquals(sub, nsSet.getName());
    }

    @Test
    public void testFindRecordsWildcard() {
        // Query a name that matches the wildcard *.wild.example.com.
        Name qname = Name.fromString("foo.wild.example.com.");
        SetResponse resp = zone.findRecords(qname, A);
        assertTrue("Wildcard expansion should be successful", resp.isSuccessful());
        RRset set = resp.answers().get(0);
        assertEquals(A, set.getType());
        assertEquals(qname, set.getName()); // name must be expanded to the queried name
    }

    @Test
    public void testFindRecordsAny() {
        // ANY query at the zone apex should return all RRsets present at the apex.
        SetResponse resp = zone.findRecords(origin, ANY);
        assertTrue("ANY query should be successful", resp.isSuccessful());
        List<RRset> ans = resp.answers();
        // We expect at least SOA and NS at the apex.
        boolean hasSOA = false, hasNS = false;
        for (RRset rs : ans) {
            if (rs.getType() == Type.SOA) hasSOA = true;
            if (rs.getType() == Type.NS)  hasNS = true;
        }
        assertTrue("SOA must be present in ANY response", hasSOA);
        assertTrue("NS must be present in ANY response", hasNS);
    }

    @Test
    public void testFindRecordsNxrrset() {
        // Query for a type that does not exist at an existing name.
        SetResponse resp = zone.findRecords(www, NS);
        assertTrue("NXRRSET expected when type missing", resp.isNXRRSET());
    }

    @Test
    public void testFindRecordsNxdomain() {
        // Query for a name completely outside the zone.
        SetResponse resp = zone.findRecords(Name.fromString("outside.com."), A);
        assertTrue("NXDOMAIN expected for out‑of‑zone name", resp.isNXDOMAIN());
    }

    @Test
    public void testIteratorOrder() {
        Iterator<RRset> it = zone.iterator();
        assertTrue("Iterator must have first element (SOA)", it.hasNext());
        RRset first = it.next();
        assertEquals(Type.SOA, first.getType());

        assertTrue("Iterator must have second element (NS)", it.hasNext());
        RRset second = it.next();
        assertEquals(Type.NS, second.getType());

        // The rest of the iterator should contain the remaining RRsets.
        boolean foundA = false, foundCNAME = false, foundDNAME = false, foundWildcard = false;
        while (it.hasNext()) {
            RRset rs = it.next();
            switch (rs.getType()) {
                case A:      foundA = true; break;
                case CNAME:  foundCNAME = true; break;
                case DNAME:  foundDNAME = true; break;
                case Type.A: // wildcard A is still type A
                    if (rs.getName().isWild()) foundWildcard = true;
                    break;
                default: break;
            }
        }
        assertTrue("A record must be present", foundA);
        assertTrue("CNAME record must be present", foundCNAME);
        assertTrue("DNAME record must be present", foundDNAME);
        assertTrue("Wildcard A record must be present", foundWildcard);
    }

    @Test
    public void testAXFRIteratorIncludesLastSOA() {
        Iterator<RRset> it = zone.AXFR();
        RRset last = null;
        while (it.hasNext()) {
            last = it.next();
        }
        assertNotNull("AXFR iterator must return at least one RRset", last);
        assertEquals("Last RRset of AXFR must be the zone SOA", Type.SOA, last.getType());
        assertEquals("Last SOA must belong to the zone apex", origin, last.getName());
    }

    @Test
    public void testAddAndRemoveRecord() throws IOException {
        Name newName = Name.fromString("new.example.com.");
        Record aRecord = Record.newRecord(newName, A, IN, 3600);
        zone.addRecord(aRecord);

        RRset fetched = zone.findExactMatch(newName, A);
        assertNotNull("Record added must be retrievable", fetched);
        assertEquals(1, fetched.size());

        // Now remove it
        zone.removeRecord(aRecord);
        RRset afterRemoval = zone.findExactMatch(newName, A);
        assertNull("Record should be gone after removal", afterRemoval);
    }

    @Test
    public void testToMasterFileContainsRecords() {
        String master = zone.toMasterFile();
        assertTrue("Master file should contain the SOA line",
                master.contains("example.com.") && master.contains("SOA"));
        assertTrue("Master file should contain an A record for www.example.com.",
                master.contains("www.example.com.") && master.contains("A"));
        assertTrue("Master file should contain the wildcard record",
                master.contains("*.wild.example.com.") && master.contains("A"));
    }
}
