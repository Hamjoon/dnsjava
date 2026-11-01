package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link Zone}.
 *
 * The tests construct real DNS objects (Name, Record, RRset, SOARecord, etc.) and avoid any
 * mocking. They cover construction, record manipulation, lookup, iterator behaviour, and error
 * handling.
 */
public class ZoneTest {

    private Name origin;
    private Name nsHost;
    private Name admin;
    private SOARecord soa;
    private Record nsRecord;
    private Record aRecord;
    private Record cnameRecord;
    private Record dnameRecord;
    private Record wildcardRecord;

    @Before
    public void setUp() throws Exception {
        origin = Name.fromString("example.com.");
        nsHost = Name.fromString("ns1.example.com.");
        admin = Name.fromString("hostmaster.example.com.");

        // Minimal SOA
        soa = new SOARecord(
                origin,
                DClass.IN,
                3600,
                nsHost,
                admin,
                2023101001L,
                7200,
                3600,
                1209600,
                3600);

        // NS at the zone apex
        nsRecord = Record.newRecord(origin, Type.NS, DClass.IN, 3600);

        // A record for the apex (used in various tests)
        aRecord = Record.newRecord(origin, Type.A, DClass.IN, 3600);

        // CNAME record (cname.example.com. -> target.example.com.)
        Name cnameName = Name.fromString("cname.example.com.");
        Name cnameTarget = Name.fromString("target.example.com.");
        cnameRecord = Record.newRecord(cnameName, Type.CNAME, DClass.IN, 3600);
        // The generic Record does not store target, but SetResponse only checks the type,
        // which is sufficient for our validation.

        // DNAME record (dname.example.com. -> newzone.example.org.)
        Name dnameName = Name.fromString("dname.example.com.");
        dnameRecord = Record.newRecord(dnameName, Type.DNAME, DClass.IN, 3600);

        // Wildcard A record (*.example.com.)
        Name wildcardName = Name.fromString("*.example.com.");
        wildcardRecord = Record.newRecord(wildcardName, Type.A, DClass.IN, 3600);
    }

    /** Helper to create a fresh Zone containing only the mandatory SOA and NS records. */
    private Zone createBasicZone() throws IOException {
        return new Zone(origin, soa, nsRecord);
    }

    @Test
    public void testZoneConstructionAndBasicAccessors() throws Exception {
        Zone zone = createBasicZone();

        // Origin and class
        assertEquals(origin, zone.getOrigin());
        assertEquals(DClass.IN, zone.getDClass());

        // SOA and NS should be present
        assertNotNull(zone.getSOA());
        assertEquals(soa, zone.getSOA());

        RRset nsSet = zone.getNS();
        assertNotNull(nsSet);
        assertEquals(Type.NS, nsSet.getType());
        assertEquals(1, nsSet.size());
        assertEquals(nsRecord, nsSet.first());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullName() throws Exception {
        new Zone(null, (Record[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullRecordsArray() throws Exception {
        new Zone(origin, (Record[]) null);
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingSOA() throws Exception {
        // Only NS, missing SOA – should fail validation
        new Zone(origin, nsRecord);
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingNS() throws Exception {
        // Only SOA, missing NS – should fail validation
        new Zone(origin, soa);
    }

    @Test
    public void testAddAndFindExactMatch() throws Exception {
        Zone zone = createBasicZone();

        // Add an A record at the zone apex
        zone.addRecord(aRecord);

        RRset result = zone.findExactMatch(origin, Type.A);
        assertNotNull(result);
        assertEquals(Type.A, result.getType());
        assertEquals(1, result.size());
        assertEquals(aRecord, result.first());

        // Ensure the returned RRset is a copy (modifying it does not affect the zone)
        result.addRR(Record.newRecord(origin, Type.A, DClass.IN, 3600));
        RRset after = zone.findExactMatch(origin, Type.A);
        assertEquals(1, after.size()); // original zone still has only one A record
    }

    @Test
    public void testRemoveRecordAndErrorConditions() throws Exception {
        Zone zone = createBasicZone();

        // Add two A records
        Record a1 = Record.newRecord(origin, Type.A, DClass.IN, 3600);
        Record a2 = Record.newRecord(origin, Type.A, DClass.IN, 3600);
        zone.addRecord(a1);
        zone.addRecord(a2);

        // Remove one A record – the set should still exist
        zone.removeRecord(a1);
        RRset aSet = zone.findExactMatch(origin, Type.A);
        assertNotNull(aSet);
        assertEquals(1, aSet.size());
        assertEquals(a2, aSet.first());

        // Remove the last A record – the RRset should be removed entirely
        zone.removeRecord(a2);
        assertNull(zone.findExactMatch(origin, Type.A));

        // Attempt to remove the SOA – should throw IllegalArgumentException
        try {
            zone.removeRecord(soa);
            fail("Expected IllegalArgumentException when removing SOA");
        } catch (IllegalArgumentException ignored) {
        }

        // Attempt to remove the only NS – should throw IllegalArgumentException
        try {
            zone.removeRecord(nsRecord);
            fail("Expected IllegalArgumentException when removing last NS");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testAddRRsetReplacement() throws Exception {
        Zone zone = createBasicZone();

        // Initial A set with one record
        RRset aSet = new RRset(aRecord);
        zone.addRRset(aSet);
        RRset fetched = zone.findExactMatch(origin, Type.A);
        assertNotNull(fetched);
        assertEquals(1, fetched.size());

        // Replace with a new RRset containing two A records
        Record a2 = Record.newRecord(origin, Type.A, DClass.IN, 3600);
        RRset newASet = new RRset(aRecord, a2);
        zone.addRRset(newASet);
        RRset after = zone.findExactMatch(origin, Type.A);
        assertNotNull(after);
        assertEquals(2, after.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRsetNull() throws Exception {
        Zone zone = createBasicZone();
        zone.addRRset(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRsetInvalidSOAOwner() throws Exception {
        Zone zone = createBasicZone();
        // SOA with a different owner name
        Name other = Name.fromString("other.com.");
        SOARecord badSoa = new SOARecord(other, DClass.IN, 3600, nsHost, admin,
                2023101002L, 7200, 3600, 1209600, 3600);
        RRset badSet = new RRset(badSoa);
        zone.addRRset(badSet);
    }

    @Test
    public void testRemoveRRsetAndInvalidRemovals() throws Exception {
        Zone zone = createBasicZone();

        // Add an A RRset then remove it
        zone.addRecord(aRecord);
        zone.removeRRset(origin, Type.A);
        assertNull(zone.findExactMatch(origin, Type.A));

        // Attempt to remove SOA via removeRRset – should throw IllegalArgumentException
        try {
            zone.removeRRset(origin, Type.SOA);
            fail("Expected IllegalArgumentException when removing SOA RRset");
        } catch (IllegalArgumentException ignored) {
        }

        // Attempt to remove NS via removeRRset – should also throw IllegalArgumentException
        try {
            zone.removeRRset(origin, Type.NS);
            fail("Expected IllegalArgumentException when removing NS RRset");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testFindRecordsExactMatchSuccess() throws Exception {
        Zone zone = createBasicZone();
        zone.addRecord(aRecord);

        SetResponse resp = zone.findRecords(origin, Type.A);
        assertTrue(resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        assertEquals(Type.A, resp.answers().get(0).getType());
    }

    @Test
    public void testFindRecordsCNAMEResponse() throws Exception {
        Zone zone = createBasicZone();
        zone.addRecord(cnameRecord);

        SetResponse resp = zone.findRecords(cnameRecord.getName(), Type.A);
        assertTrue(resp.isCNAME());
        assertNotNull(resp.getCNAME());
        assertEquals(cnameRecord.getName(), resp.getCNAME().getName());
    }

    @Test
    public void testFindRecordsDNAMEResponse() throws Exception {
        Zone zone = createBasicZone();
        zone.addRecord(dnameRecord);

        // Query a name beneath the DNAME owner
        Name query = Name.fromString("foo.dname.example.com.");
        SetResponse resp = zone.findRecords(query, Type.A);
        assertTrue(resp.isDNAME());
        assertNotNull(resp.getDNAME());
        assertEquals(dnameRecord.getName(), resp.getDNAME().getName());
    }

    @Test
    public void testFindRecordsDelegation() throws Exception {
        Zone zone = createBasicZone();

        // Create a delegated subzone "sub.example.com." with its own NS record
        Name sub = Name.fromString("sub.example.com.");
        Record subNs = Record.newRecord(sub, Type.NS, DClass.IN, 3600);
        zone.addRecord(subNs);

        SetResponse resp = zone.findRecords(sub, Type.A);
        assertTrue(resp.isDelegation());
        assertNotNull(resp.getNS());
        assertEquals(1, resp.getNS().size());
        assertEquals(subNs, resp.getNS().first());
    }

    @Test
    public void testFindRecordsWildcardExpansion() throws Exception {
        Zone zone = createBasicZone();

        // Add wildcard A record
        zone.addRecord(wildcardRecord);

        // Query a name that matches the wildcard
        Name query = Name.fromString("any.example.com.");
        SetResponse resp = zone.findRecords(query, Type.A);
        assertTrue(resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        RRset expanded = resp.answers().get(0);
        // The expanded set should contain a record whose name is the queried name
        assertEquals(query, expanded.first().getName());
    }

    @Test
    public void testFindRecordsAnyQuery() throws Exception {
        Zone zone = createBasicZone();
        zone.addRecord(aRecord);
        zone.addRecord(cnameRecord); // add another type

        SetResponse resp = zone.findRecords(origin, Type.ANY);
        assertTrue(resp.isSuccessful());
        // Should contain at least SOA, NS, A, CNAME
        assertTrue(resp.answers().size() >= 4);
    }

    @Test
    public void testFindRecordsNxDomain() throws Exception {
        Zone zone = createBasicZone();

        Name external = Name.fromString("outside.com.");
        SetResponse resp = zone.findRecords(external, Type.A);
        assertTrue(resp.isNXDOMAIN());
    }

    @Test
    public void testIteratorOrderAndRemoval() throws Exception {
        Zone zone = createBasicZone();
        zone.addRecord(aRecord); // extra record after NS

        Iterator<RRset> it = zone.iterator();

        // First should be SOA
        assertTrue(it.hasNext());
        RRset first = it.next();
        assertEquals(Type.SOA, first.getType());

        // Second should be NS
        assertTrue(it.hasNext());
        RRset second = it.next();
        assertEquals(Type.NS, second.getType());

        // Third should be the A record we added
        assertTrue(it.hasNext());
        RRset third = it.next();
        assertEquals(Type.A, third.getType());

        // No more elements
        assertFalse(it.hasNext());

        // Remove the A RRset via iterator
        it = zone.iterator(); // reset iterator
        it.next(); // SOA
        it.next(); // NS
        RRset toRemove = it.next(); // A
        it.remove();

        // After removal, A should no longer be present
        assertNull(zone.findExactMatch(origin, Type.A));
    }

    @Test
    public void testAXFRIteratorIncludesTrailingSOA() throws Exception {
        Zone zone = createBasicZone();
        zone.addRecord(aRecord);

        Iterator<RRset> axfr = zone.AXFR();

        // Consume all elements
        int count = 0;
        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
            count++;
        }

        // The last element must be the SOA set again
        assertNotNull(last);
        assertEquals(Type.SOA, last.getType());
        // Total count should be original count + 1 (the duplicated SOA)
        assertEquals(4, count); // SOA, NS, A, SOA
    }

    @Test
    public void testToMasterFileContainsAllRecords() throws Exception {
        Zone zone = createBasicZone();
        zone.addRecord(aRecord);
        zone.addRecord(cnameRecord);
        zone.addRecord(dnameRecord);
        zone.addRecord(wildcardRecord);

        String master = zone.toMasterFile();
        // Verify that each record's textual representation appears in the master file string
        assertTrue(master.contains(origin.toString()));
        assertTrue(master.contains(nsRecord.toString()));
        assertTrue(master.contains(aRecord.toString()));
        assertTrue(master.contains(cnameRecord.toString()));
        assertTrue(master.contains(dnameRecord.toString()));
        assertTrue(master.contains(wildcardRecord.toString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordNull() throws Exception {
        Zone zone = createBasicZone();
        zone.addRecord(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordInvalidSubdomain() throws Exception {
        Zone zone = createBasicZone();
        // Record whose name is not a subdomain of the zone origin
        Name external = Name.fromString("outside.com.");
        Record externalRecord = Record.newRecord(external, Type.A, DClass.IN, 3600);
        zone.addRecord(externalRecord);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordSOAOwnerMismatch() throws Exception {
        Zone zone = createBasicZone();
        Name other = Name.fromString("other.com.");
        SOARecord badSoa = new SOARecord(other, DClass.IN, 3600, nsHost, admin,
                2023101002L, 7200, 3600, 1209600, 3600);
        zone.addRecord(badSoa);
    }
}
