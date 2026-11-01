package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link Zone}. The tests use only the public API
 * that is declared in the supplied source and the minimal helper classes listed
 * in the problem statement.  All records are created with {@link Record#newRecord}
 * (or the concrete {@link SOARecord} constructor) so that the tests focus on the
 * behaviour of the {@code Zone} class rather than on the individual record
 * implementations.
 */
public class ZoneTest {

    /** Helper to build a minimal valid zone (SOA + one NS) */
    private Zone createBasicZone() throws IOException {
        Name origin = Name.fromString("example.com.");
        SOARecord soa = new SOARecord(
                origin,
                DClass.IN,
                3600,
                Name.fromString("ns.example.com."),
                Name.fromString("hostmaster.example.com."),
                1, 7200, 3600, 1209600, 3600);
        Record ns = Record.newRecord(origin, Type.NS, DClass.IN, 3600);
        return new Zone(origin, soa, ns);
    }

    /** Helper to build a richer zone used by many tests */
    private Zone createRichZone() throws IOException {
        Zone zone = createBasicZone();

        // www A record
        Record wwwA = Record.newRecord(
                Name.fromString("www.example.com."),
                Type.A,
                DClass.IN,
                3600);
        zone.addRecord(wwwA);

        // alias CNAME record
        Record cname = Record.newRecord(
                Name.fromString("alias.example.com."),
                Type.CNAME,
                DClass.IN,
                3600);
        zone.addRecord(cname);

        // deep DNAME record
        Record dname = Record.newRecord(
                Name.fromString("deep.example.com."),
                Type.DNAME,
                DClass.IN,
                3600);
        zone.addRecord(dname);

        // sub‑domain delegation NS
        Record subNs = Record.newRecord(
                Name.fromString("sub.example.com."),
                Type.NS,
                DClass.IN,
                3600);
        zone.addRecord(subNs);

        // wildcard A record
        Record wildA = Record.newRecord(
                Name.fromString("*.example.com."),
                Type.A,
                DClass.IN,
                3600);
        zone.addRecord(wildA);

        return zone;
    }

    /* --------------------------------------------------------------------- */
    /* Constructor validation                                               */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullZone() throws IOException {
        new Zone(null, "dummy.zone");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullFile() throws IOException {
        Name origin = Name.fromString("example.com.");
        new Zone(origin, (String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullRecordsArray() throws IOException {
        Name origin = Name.fromString("example.com.");
        new Zone(origin, (Record[]) null);
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingSOA() throws IOException {
        Name origin = Name.fromString("example.com.");
        Record ns = Record.newRecord(origin, Type.NS, DClass.IN, 3600);
        new Zone(origin, ns); // no SOA -> validate() throws IOException
    }

    @Test(expected = IOException.class)
    public void testConstructorMissingNS() throws IOException {
        Name origin = Name.fromString("example.com.");
        SOARecord soa = new SOARecord(
                origin,
                DClass.IN,
                3600,
                Name.fromString("ns.example.com."),
                Name.fromString("hostmaster.example.com."),
                1, 7200, 3600, 1209600, 3600);
        new Zone(origin, soa); // no NS -> validate() throws IOException
    }

    /* --------------------------------------------------------------------- */
    /* addRecord / replace SOA                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddRecordAndReplaceSOA() throws IOException {
        Zone zone = createBasicZone();

        // add a new A record
        Record a = Record.newRecord(
                Name.fromString("host.example.com."),
                Type.A,
                DClass.IN,
                3600);
        zone.addRecord(a);

        RRset set = zone.findExactMatch(Name.fromString("host.example.com."), Type.A);
        assertNotNull(set);
        assertEquals(1, set.size());
        assertEquals(Type.A, set.getType());

        // replace the existing SOA with a new one (same name, different serial)
        SOARecord newSOA = new SOARecord(
                zone.getOrigin(),
                DClass.IN,
                3600,
                Name.fromString("ns2.example.com."),
                Name.fromString("hostmaster.example.com."),
                2, 7200, 3600, 1209600, 3600);
        zone.addRecord(newSOA);
        assertEquals(2, zone.getSOA().getSerial());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordNull() throws IOException {
        Zone zone = createBasicZone();
        zone.addRecord(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordOutsideZone() throws IOException {
        Zone zone = createBasicZone();
        Record out = Record.newRecord(
                Name.fromString("outside.com."),
                Type.A,
                DClass.IN,
                3600);
        zone.addRecord(out);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordSOAOwnerMismatch() throws IOException {
        Zone zone = createBasicZone();
        Name other = Name.fromString("other.com.");
        SOARecord badSOA = new SOARecord(
                other,
                DClass.IN,
                3600,
                Name.fromString("ns.other.com."),
                Name.fromString("hostmaster.other.com."),
                1, 7200, 3600, 1209600, 3600);
        zone.addRecord(badSOA);
    }

    /* --------------------------------------------------------------------- */
    /* removeRecord – normal and illegal cases                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRemoveRecordNormal() throws IOException {
        Zone zone = createRichZone();

        // remove the www A record
        Record www = Record.newRecord(
                Name.fromString("www.example.com."),
                Type.A,
                DClass.IN,
                3600);
        zone.removeRecord(www);
        assertNull(zone.findExactMatch(Name.fromString("www.example.com."), Type.A));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecordNull() throws IOException {
        Zone zone = createBasicZone();
        zone.removeRecord(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecordSOA() throws IOException {
        Zone zone = createBasicZone();
        zone.removeRecord(zone.getSOA());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveLastNS() throws IOException {
        Zone zone = createBasicZone();
        // zone contains exactly one NS at origin; attempt to delete it
        Record ns = Record.newRecord(zone.getOrigin(), Type.NS, DClass.IN, 3600);
        zone.removeRecord(ns);
    }

    /* --------------------------------------------------------------------- */
    /* addRRset / removeRRset                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddRRsetReplace() throws IOException {
        Zone zone = createBasicZone();

        // create a new NS set with two records
        Record ns1 = Record.newRecord(zone.getOrigin(), Type.NS, DClass.IN, 3600);
        Record ns2 = Record.newRecord(zone.getOrigin(), Type.NS, DClass.IN, 3600);
        RRset newNsSet = new RRset(ns1, ns2);
        zone.addRRset(newNsSet);

        RRset fetched = zone.getNS();
        assertEquals(2, fetched.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRsetNull() throws IOException {
        Zone zone = createBasicZone();
        zone.addRRset(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRRsetSOAOwnerMismatch() throws IOException {
        Zone zone = createBasicZone();
        Name other = Name.fromString("other.com.");
        SOARecord soa = new SOARecord(
                other,
                DClass.IN,
                3600,
                Name.fromString("ns.other.com."),
                Name.fromString("hostmaster.other.com."),
                1, 7200, 3600, 1209600, 3600);
        RRset rrset = new RRset(soa);
        zone.addRRset(rrset);
    }

    @Test
    public void testRemoveRRset() throws IOException {
        Zone zone = createRichZone();

        // remove the wildcard A RRset
        zone.removeRRset(Name.fromString("*.example.com."), Type.A);
        assertNull(zone.findExactMatch(Name.fromString("*.example.com."), Type.A));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRRsetNullName() throws IOException {
        Zone zone = createBasicZone();
        zone.removeRRset(null, Type.A);
    }

    @Test(expected = InvalidTypeException.class)
    public void testRemoveRRsetInvalidType() throws IOException {
        Zone zone = createBasicZone();
        zone.removeRRset(zone.getOrigin(), Type.ANY); // ANY is not removable
    }

    /* --------------------------------------------------------------------- */
    /* findExactMatch – copy semantics                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFindExactMatchCopy() throws IOException {
        Zone zone = createRichZone();
        RRset original = zone.findExactMatch(Name.fromString("www.example.com."), Type.A);
        RRset copy = zone.findExactMatch(Name.fromString("www.example.com."), Type.A);
        assertNotSame(original, copy);
        // modifications to copy must not affect the zone
        copy.addRR(Record.newRecord(Name.fromString("www.example.com."), Type.A, DClass.IN, 3600));
        assertEquals(1, original.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindExactMatchNullName() throws IOException {
        Zone zone = createBasicZone();
        zone.findExactMatch(null, Type.A);
    }

    /* --------------------------------------------------------------------- */
    /* findRecords – various response types                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFindRecordsExactSuccess() throws IOException {
        Zone zone = createRichZone();
        SetResponse resp = zone.findRecords(Name.fromString("www.example.com."), Type.A);
        assertTrue(resp.isSuccessful());
        assertEquals(1, resp.answers().size());
        assertEquals(Type.A, resp.answers().get(0).getType());
    }

    @Test
    public void testFindRecordsCNAME() throws IOException {
        Zone zone = createRichZone();
        SetResponse resp = zone.findRecords(Name.fromString("alias.example.com."), Type.A);
        assertTrue(resp.isCNAME());
        assertNotNull(resp.getCNAME());
        assertEquals(Type.CNAME, resp.getCNAME().getType());
    }

    @Test
    public void testFindRecordsDNAME() throws IOException {
        Zone zone = createRichZone();
        // query a name beneath the DNAME owner
        SetResponse resp = zone.findRecords(Name.fromString("x.deep.example.com."), Type.A);
        assertTrue(resp.isDNAME());
        assertNotNull(resp.getDNAME());
    }

    @Test
    public void testFindRecordsDelegation() throws IOException {
        Zone zone = createRichZone();
        SetResponse resp = zone.findRecords(Name.fromString("sub.example.com."), Type.A);
        assertTrue(resp.isDelegation());
        assertNotNull(resp.getNS());
        assertEquals(Type.NS, resp.getNS().getType());
    }

    @Test
    public void testFindRecordsAny() throws IOException {
        Zone zone = createRichZone();
        SetResponse resp = zone.findRecords(Name.fromString("www.example.com."), Type.ANY);
        assertTrue(resp.isSuccessful());
        // should contain at least the A record and the SOA/NS of the origin
        assertTrue(resp.answers().size() >= 3);
    }

    @Test
    public void testFindRecordsWildcardExpansion() throws IOException {
        Zone zone = createRichZone();
        Name query = Name.fromString("foo.example.com.");
        SetResponse resp = zone.findRecords(query, Type.A);
        assertTrue(resp.isSuccessful());
        RRset rrset = resp.answers().get(0);
        // the returned RRset must have the queried name, not the literal "*"
        assertEquals(query, rrset.getName());
    }

    @Test
    public void testFindRecordsNxDomain() throws IOException {
        Zone zone = createBasicZone();
        SetResponse resp = zone.findRecords(Name.fromString("unknown.other.com."), Type.A);
        assertTrue(resp.isNXDOMAIN());
    }

    /* --------------------------------------------------------------------- */
    /* Iterator behaviour (normal and AXFR)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIteratorOrderAndRemove() throws IOException {
        Zone zone = createRichZone();
        Iterator<RRset> it = zone.iterator();

        // first two RRsets must be the SOA and the NS of the origin
        assertTrue(it.hasNext());
        RRset first = it.next();
        assertEquals(Type.SOA, first.getType());

        assertTrue(it.hasNext());
        RRset second = it.next();
        assertEquals(Type.NS, second.getType());

        // remove the NS set via iterator.remove()
        it.remove();

        // verify that the NS is gone from the zone
        assertNull(zone.getNS());
        assertTrue(zone.findExactMatch(zone.getOrigin(), Type.NS) == null);
    }

    @Test(expected = NoSuchElementException.class)
    public void testIteratorBeyondEnd() throws IOException {
        Zone zone = createBasicZone();
        Iterator<RRset> it = zone.iterator();
        // two elements (SOA, NS) – consume them
        it.next();
        it.next();
        // now iterator should be exhausted
        it.next(); // should throw NoSuchElementException
    }

    @Test
    public void testAXFRIteratorReturnsLastSOA() throws IOException {
        Zone zone = createRichZone();
        Iterator<RRset> axfr = zone.AXFR();

        // consume all elements; the last one should be the SOA again
        RRset last = null;
        while (axfr.hasNext()) {
            last = axfr.next();
        }
        assertNotNull(last);
        assertEquals(Type.SOA, last.getType());
    }

    /* --------------------------------------------------------------------- */
    /* toMasterFile – basic sanity check                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToMasterFileContainsRecords() throws IOException {
        Zone zone = createRichZone();
        String master = zone.toMasterFile();

        // The SOA line must be present (it contains the serial number)
        assertTrue(master.contains("1")); // serial from basic SOA is 1

        // The wildcard line must appear
        assertTrue(master.contains("*.example.com."));

        // The NS delegation for sub.example.com. must appear
        assertTrue(master.contains("sub.example.com."));
    }

    /* --------------------------------------------------------------------- */
    /* getDClass – should always be IN                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetDClass() throws IOException {
        Zone zone = createBasicZone();
        assertEquals(DClass.IN, zone.getDClass());
    }
}
