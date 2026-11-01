/**
 * Comprehensive JUnit‑4 test suite for {@link org.xbill.DNS.Zone}.
 *
 * The tests cover:
 *   • construction (array constructor) and validation of required records
 *   • basic getters (getDClass, getNS)
 *   • record manipulation (addRecord, removeRecord, addRRset, removeRRset)
 *   • lookup methods (findExactMatch, findRecords) including all response types
 *   • iterator and AXFR iterator ordering
 *   • serialization to master‑file format (toMasterFile)
 *
 * Only classes, constructors and methods that appear in the supplied source
 * (or the explicit dependency signatures) are used – no external mocking or
 * subclassing of library types.
 */
package org.xbill.DNS;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ZoneTest {

    /* DNS type constants – the library defines these in org.xbill.DNS.Type,
       but they are reproduced here to avoid importing the Type class. */
    private static final int TYPE_SOA   = 6;
    private static final int TYPE_NS    = 2;
    private static final int TYPE_A     = 1;
    private static final int TYPE_CNAME = 5;
    private static final int TYPE_DNAME = 39;
    private static final int TYPE_ANY   = 255;
    private static final int TYPE_AAAA  = 28;

    private Name zoneName;
    private SOARecord soaRecord;
    private Record nsRecord;
    private Record aRecord;
    private Record cnameRecord;
    private Record dnameRecord;
    private Record wildcardARecord;

    private Zone zone;   // zone used by most tests

    /**
     * Helper that builds a fresh zone containing:
     *   - a valid SOA (required)
     *   - a single NS (required)
     *   - an A record for www.example.com.
     *   - a CNAME for mail.example.com.
     *   - a DNAME for old.example.com.
     *   - a wildcard A record (*.example.com.)
     */
    @Before
    public void setUp() throws IOException, TextParseException {
        zoneName = new Name("example.com.");
        // SOA record – the constructor arguments follow the public API
        soaRecord = new SOARecord(
                zoneName,
                DClass.IN,
                3600,
                new Name("ns1.example.com."),
                new Name("hostmaster.example.com."),
                2023100101L,
                7200,
                3600,
                1209600,
                3600);

        // NS record (owner must be the zone name)
        nsRecord = Record.newRecord(zoneName, TYPE_NS, DClass.IN, 3600);

        // ordinary A record
        aRecord = Record.newRecord(new Name("www.example.com."), TYPE_A, DClass.IN, 3600);

        // CNAME record
        cnameRecord = Record.newRecord(new Name("mail.example.com."), TYPE_CNAME, DClass.IN, 3600);

        // DNAME record
        dnameRecord = Record.newRecord(new Name("old.example.com."), TYPE_DNAME, DClass.IN, 3600);

        // Wildcard A record
        wildcardARecord = Record.newRecord(new Name("*.example.com."), TYPE_A, DClass.IN, 3600);

        // Build the zone using the array‑constructor (the simplest way to avoid file I/O)
        zone = new Zone(zoneName,
                soaRecord,
                nsRecord,
                aRecord,
                cnameRecord,
                dnameRecord,
                wildcardARecord);
    }

    /* --------------------  basic getters  -------------------- */

    @Test
    public void testGetDClass() {
        assertEquals("Zone class should be IN", DClass.IN, zone.getDClass());
    }

    @Test
    public void testGetNS() {
        RRset nsSet = zone.getNS();
        assertNotNull("NS set must not be null", nsSet);
        assertEquals("NS set must contain exactly one record", 1, nsSet.size());
        assertEquals("NS record type must be NS", TYPE_NS, nsSet.getType());
        assertEquals("NS record owner must be the zone origin", zoneName, nsSet.getName());
    }

    /* --------------------  findExactMatch -------------------- */

    @Test
    public void testFindExactMatchReturnsCopy() {
        RRset result = zone.findExactMatch(new Name("www.example.com."), TYPE_A);
        assertNotNull("Exact match should be found for www.example.com. A", result);
        assertEquals("Returned set must have one A record", 1, result.size());

        // Mutate the returned set – the zone's internal data must stay unchanged
        result.clear();
        RRset afterClear = zone.findExactMatch(new Name("www.example.com."), TYPE_A);
        assertNotNull("Original zone data must still contain the A record", afterClear);
        assertEquals("Original set must still contain one record", 1, afterClear.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindExactMatchNullName() {
        zone.findExactMatch(null, TYPE_A);
    }

    /* --------------------  findRecords – various response types -------------------- */

    @Test
    public void testFindRecordsSuccessful() {
        SetResponse resp = zone.findRecords(new Name("www.example.com."), TYPE_A);
        assertTrue("Response should be successful", resp.isSuccessful());
        assertEquals("Exactly one answer RRset expected", 1, resp.answers().size());
        assertEquals("Answer should be the A RRset", TYPE_A, resp.answers().get(0).getType());
    }

    @Test
    public void testFindRecordsCNAME() {
        // Query for A but only a CNAME exists – should return CNAME response
        SetResponse resp = zone.findRecords(new Name("mail.example.com."), TYPE_A);
        assertTrue("Response should be CNAME", resp.isCNAME());
        assertNotNull("CNAME RRset must be present", resp.getCNAME());
        assertEquals("CNAME RRset type must be CNAME", TYPE_CNAME, resp.getCNAME().getType());
    }

    @Test
    public void testFindRecordsDNAME() {
        // Query for A under a name that has a DNAME – should return DNAME response
        SetResponse resp = zone.findRecords(new Name("old.example.com."), TYPE_A);
        assertTrue("Response should be DNAME", resp.isDNAME());
        assertNotNull("DNAME RRset must be present", resp.getDNAME());
        assertEquals("DNAME RRset type must be DNAME", TYPE_DNAME, resp.getDNAME().getType());
    }

    @Test
    public void testFindRecordsAnyAtOrigin() {
        SetResponse resp = zone.findRecords(zoneName, TYPE_ANY);
        assertTrue("ANY query at origin should be successful", resp.isSuccessful());
        // The zone has SOA, NS, A, CNAME, DNAME, wildcard A = 6 RRsets
        assertEquals("ALL RRsets should be returned", 6, resp.answers().size());
    }

    @Test
    public void testFindRecordsNxrrset() {
        // Query for a type that does not exist at the exact name (e.g., AAAA at www)
        SetResponse resp = zone.findRecords(new Name("www.example.com."), TYPE_AAAA);
        assertTrue("Response should be NXRRSET when type missing", resp.isNXRRSET());
    }

    @Test
    public void testFindRecordsNxdomain() {
        // Name outside the zone
        SetResponse resp = zone.findRecords(new Name("outside.com."), TYPE_A);
        assertTrue("Response should be NXDOMAIN for name outside zone", resp.isNXDOMAIN());
    }

    @Test
    public void testFindRecordsWildcardExpansion() {
        // foo.example.com. does not exist explicitly, but matches *.example.com.
        SetResponse resp = zone.findRecords(new Name("foo.example.com."), TYPE_A);
        assertTrue("Wildcard expansion should be successful", resp.isSuccessful());
        assertEquals("Exactly one answer RRset expected", 1, resp.answers().size());
        RRset expanded = resp.answers().get(0);
        assertEquals("Expanded RRset type must be A", TYPE_A, expanded.getType());
        // The record inside the set must have the queried name (foo.example.com.)
        Record rec = expanded.first();
        assertEquals("Record name must be the queried name after expansion",
                new Name("foo.example.com."), rec.getName());
    }

    /* --------------------  addRecord -------------------- */

    @Test
    public void testAddRecordCreatesNewRRset() throws IOException {
        Record newA = Record.newRecord(new Name("new.example.com."), TYPE_A, DClass.IN, 3600);
        zone.addRecord(newA);
        RRset fetched = zone.findExactMatch(new Name("new.example.com."), TYPE_A);
        assertNotNull("Newly added A record should be findable", fetched);
        assertEquals("Fetched RRset must contain the added record", 1, fetched.size());
        assertEquals("Record type must be A", TYPE_A, fetched.getType());
    }

    @Test
    public void testAddRecordAppendsToExistingRRset() throws IOException {
        // Add a second NS record – the NS RRset should now contain two records
        Record secondNS = Record.newRecord(zoneName, TYPE_NS, DClass.IN, 3600);
        zone.addRecord(secondNS);
        RRset nsSet = zone.getNS();
        assertEquals("NS set must now contain two records", 2, nsSet.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordInvalidSOAOwner() throws IOException {
        // Attempt to add an SOA whose owner is not the zone origin
        Record badSOA = Record.newRecord(new Name("other.example.com."), TYPE_SOA, DClass.IN, 3600);
        zone.addRecord(badSOA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordOutsideZone() throws IOException {
        // Record whose name is not a subdomain of the zone origin
        Record outRecord = Record.newRecord(new Name("outside.com."), TYPE_A, DClass.IN, 3600);
        zone.addRecord(outRecord);
    }

    /* --------------------  removeRecord -------------------- */

    @Test
    public void testRemoveRecordDeletesRRsetWhenEmpty() throws IOException {
        // Remove the only A record for www.example.com.
        zone.removeRecord(aRecord);
        assertNull("After removal, findExactMatch should return null",
                zone.findExactMatch(new Name("www.example.com."), TYPE_A));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecordCannotRemoveSOA() {
        zone.removeRecord(soaRecord);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecordCannotRemoveLastNS() {
        // The zone currently has only one NS record
        zone.removeRecord(nsRecord);
    }

    @Test
    public void testRemoveRecordLeavesOtherRecordsIntact() throws IOException {
        // Add a second NS, then remove one – the remaining one must stay
        Record secondNS = Record.newRecord(zoneName, TYPE_NS, DClass.IN, 3600);
        zone.addRecord(secondNS);
        zone.removeRecord(nsRecord);
        RRset nsSet = zone.getNS();
        assertEquals("One NS record must remain after removal", 1, nsSet.size());
    }

    /* --------------------  addRRset -------------------- */

    @Test
    public void testAddRRsetReplacesExisting() {
        // Build a new NS RRset with two records
        Record ns1 = Record.newRecord(zoneName, TYPE_NS, DClass.IN, 3600);
        Record ns2 = Record.newRecord(zoneName, TYPE_NS, DClass.IN, 3600);
        RRset newNsSet = new RRset(ns1, ns2);
        zone.addRRset(newNsSet);
        RRset fetched = zone.getNS();
        assertEquals("Replaced NS set must have two records", 2, fetched.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRsetInvalidSOAOwner() {
        // SOA owner not equal to zone origin
        SOARecord otherSOA = new SOARecord(
                new Name("other.example.com."),
                DClass.IN,
                3600,
                new Name("ns1.example.com."),
                new Name("hostmaster.example.com."),
                2023100101L,
                7200,
                3600,
                1209600,
                3600);
        RRset rrset = new RRset(otherSOA);
        zone.addRRset(rrset);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRsetNullArgument() {
        zone.addRRset(null);
    }

    /* --------------------  removeRRset -------------------- */

    @Test
    public void testRemoveRRsetDeletesSpecifiedType() {
        // Ensure that the A RRset for www.example.com. can be removed
        zone.removeRRset(new Name("www.example.com."), TYPE_A);
        assertNull("After removal, A RRset should be absent",
                zone.findExactMatch(new Name("www.example.com."), TYPE_A));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRRsetNullName() {
        zone.removeRRset(null, TYPE_A);
    }

    @Test(expected = InvalidTypeException.class)
    public void testRemoveRRsetInvalidType() {
        // Type 0 is not a valid DNS type
        zone.removeRRset(new Name("www.example.com."), 0);
    }

    /* --------------------  iterator ordering -------------------- */

    @Test
    public void testIteratorProvidesSOAFirstNSSecond() {
        Iterator<RRset> it = zone.iterator();
        assertTrue("Iterator should have at least one element", it.hasNext());

        RRset first = it.next();
        assertEquals("First RRset must be SOA", TYPE_SOA, first.getType());

        assertTrue("Iterator should have a second element", it.hasNext());
        RRset second = it.next();
        assertEquals("Second RRset must be NS", TYPE_NS, second.getType());
    }

    @Test
    public void testAXFRIteratorReturnsSOAAtEnd() {
        Iterator<RRset> axfr = zone.AXFR();
        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
        }
        assertNotNull("AXFR iterator must produce at least one element", last);
        assertEquals("Last element of AXFR iterator must be SOA", TYPE_SOA, last.getType());
    }

    @Test
    public void testIteratorRemoveDeletesCurrentRRset() {
        // Add a temporary A record to have a removable element after the origin
        Record temp = Record.newRecord(new Name("temp.example.com."), TYPE_A, DClass.IN, 3600);
        zone.addRecord(temp);

        Iterator<RRset> it = zone.iterator();
        // Skip SOA and NS
        it.next(); // SOA
        it.next(); // NS
        // Now the next element should be the A RRset for www.example.com.
        assertTrue(it.hasNext());
        RRset aSet = it.next();
        assertEquals("Expected A RRset", TYPE_A, aSet.getType());

        // Remove it via iterator
        it.remove();

        // Verify that the A RRset is gone from the zone
        assertNull("A RRset should be removed after iterator.remove()",
                zone.findExactMatch(new Name("www.example.com."), TYPE_A));
    }

    /* --------------------  toMasterFile output -------------------- */

    @Test
    public void testToMasterFileContainsAllRecords() {
        String master = zone.toMasterFile();
        // The master file should contain textual representations of all six records we added
        assertTrue("Master file must contain the SOA record", master.contains("SOA"));
        assertTrue("Master file must contain the NS record", master.contains("NS"));
        assertTrue("Master file must contain the A record for www", master.contains("www.example.com."));
        assertTrue("Master file must contain the CNAME record for mail", master.contains("mail.example.com."));
        assertTrue("Master file must contain the DNAME record for old", master.contains("old.example.com."));
        assertTrue("Master file must contain the wildcard A record", master.contains("*.example.com."));
    }

    /* --------------------  constructor validation -------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullZoneName() throws IOException {
        new Zone(null, soaRecord, nsRecord);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullRecordsArray() throws IOException {
        new Zone(zoneName, (Record[]) null);
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingSOA() throws IOException {
        // Provide only NS – validation should fail
        new Zone(zoneName, nsRecord);
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingNS() throws IOException {
        // Provide only SOA – validation should fail
        new Zone(zoneName, soaRecord);
    }
}
