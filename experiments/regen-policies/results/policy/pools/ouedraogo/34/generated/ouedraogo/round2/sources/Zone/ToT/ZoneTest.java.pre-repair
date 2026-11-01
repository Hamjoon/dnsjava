package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link Zone}.  The tests exercise the public API, covering
 * normal usage, edge cases and error handling.  All objects are created via the
 * constructors and factory methods that are part of the supplied source – no
 * mocking or subclassing is used.
 */
public class ZoneTest {

  /** Common names used throughout the tests. */
  private Name origin;
  private Name nsTarget;
  private Name www;
  private Name cnameTarget;
  private SOARecord soa;

  /** A minimal, valid zone used as a baseline for many tests. */
  private Zone baseZone;

  /** Helper that creates a simple SOA record for the given origin. */
  private SOARecord makeSOA(Name origin) {
    try {
      return new SOARecord(
          origin,               // name (owner)
          DClass.IN,            // class
          3600,                 // ttl
          Name.fromString("ns1.example.com."), // primary name server
          Name.fromString("hostmaster.example.com."), // admin mailbox
          2023100101L,         // serial
          7200,                 // refresh
          3600,                 // retry
          1209600,              // expire
          3600);                // minimum
    } catch (TextParseException e) {
      // Should never happen for hard‑coded strings
      throw new RuntimeException(e);
    }
  }

  /** Helper that creates a generic NS record for the given name. */
  private Record makeNS(Name name) {
    // Data part of NS record is the target name; we use a simple Record factory.
    return Record.newRecord(name, Type.NS, DClass.IN, 3600);
  }

  /** Helper that creates a generic A record for the given name. */
  private Record makeA(Name name) {
    return Record.newRecord(name, Type.A, DClass.IN, 3600);
  }

  /** Helper that creates a generic CNAME record for the given name. */
  private Record makeCNAME(Name name) {
    return Record.newRecord(name, Type.CNAME, DClass.IN, 3600);
  }

  @Before
  public void setUp() throws Exception {
    origin = Name.fromString("example.com.");
    nsTarget = Name.fromString("ns1.example.com.");
    www = Name.fromString("www.example.com.");
    cnameTarget = Name.fromString("alias.example.com.");

    soa = makeSOA(origin);
    Record ns = makeNS(origin);
    // Build a zone that contains a mandatory SOA and NS record.
    baseZone = new Zone(origin, soa, ns);
  }

  // ------------------------------------------------------------------------
  // Constructor tests
  // ------------------------------------------------------------------------

  @Test
  public void testConstructorWithValidRecords() throws Exception {
    Zone z = new Zone(origin, soa, makeNS(origin));
    assertNotNull("SOA should be set", z.getSOA());
    assertEquals("Origin must match", origin, z.getOrigin());
    assertNotNull("NS set must be present", z.getNS());
    assertEquals("Exactly one NS record expected", 1, z.getNS().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullOrigin() throws Exception {
    new Zone(null, soa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullRecordsArray() throws Exception {
    new Zone(origin, (Record[]) null);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingSOA() throws Exception {
    // Only NS, no SOA → validation should fail.
    new Zone(origin, makeNS(origin));
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingNS() throws Exception {
    // Only SOA, no NS → validation should fail.
    new Zone(origin, soa);
  }

  // ------------------------------------------------------------------------
  // Basic getters
  // ------------------------------------------------------------------------

  @Test
  public void testGetDClass() {
    assertEquals("Zone must report IN class", DClass.IN, baseZone.getDClass());
  }

  @Test
  public void testGetNSReturnsCopy() {
    RRset nsCopy = baseZone.getNS();
    assertNotSame("getNS should return a copy", nsCopy, baseZone.getNS());
    assertEquals("NS set size must be 1", 1, nsCopy.size());
  }

  // ------------------------------------------------------------------------
  // addRecord / removeRecord
  // ------------------------------------------------------------------------

  @Test
  public void testAddRecordCreatesNewRRset() throws Exception {
    Record a = makeA(www);
    baseZone.addRecord(a);

    RRset rs = baseZone.findExactMatch(www, Type.A);
    assertNotNull("RRset for www should exist", rs);
    assertEquals("RRset must contain the added A record", 1, rs.size());
    assertEquals("Returned record must be the one added", a, rs.first());
  }

  @Test
  public void testAddRecordReplacesSOA() throws Exception {
    SOARecord newSoa = new SOARecord(
        origin, DClass.IN, 7200,
        Name.fromString("ns2.example.com."),
        Name.fromString("hostmaster2.example.com."), 2023100201L,
        7200, 3600, 1209600, 3600);
    baseZone.addRecord(newSoa);
    assertEquals("SOA should be replaced", newSoa, baseZone.getSOA());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordWithNameOutsideZone() throws Exception {
    Name external = Name.fromString("outside.com.");
    Record a = makeA(external);
    baseZone.addRecord(a);
  }

  @Test
  public void testRemoveRecordDeletesExisting() throws Exception {
    Record a = makeA(www);
    baseZone.addRecord(a);
    assertNotNull("Record must be present before removal", baseZone.findExactMatch(www, Type.A));

    baseZone.removeRecord(a);
    assertNull("Record must be gone after removal", baseZone.findExactMatch(www, Type.A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordThrowsOnSOA() throws Exception {
    baseZone.removeRecord(soa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordLastNSForbidden() throws Exception {
    // The zone has exactly one NS at the origin.
    Record ns = makeNS(origin);
    baseZone.removeRecord(ns);
  }

  // ------------------------------------------------------------------------
  // addRRset / removeRRset
  // ------------------------------------------------------------------------

  @Test
  public void testAddRRsetReplacesExisting() throws Exception {
    // Create a new NS RRset with a different target.
    Record ns2 = Record.newRecord(origin, Type.NS, DClass.IN, 3600);
    RRset newNsSet = new RRset(ns2);
    baseZone.addRRset(newNsSet);

    RRset fetched = baseZone.getNS();
    assertEquals("NS set should be replaced", 1, fetched.size());
    assertEquals("The stored NS record must be the new one", ns2, fetched.first());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNull() throws Exception {
    baseZone.addRRset(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetInvalidSOAName() throws Exception {
    // SOA owner not equal to zone origin.
    SOARecord badSoa = new SOARecord(
        Name.fromString("bad.example.com."), DClass.IN, 3600,
        Name.fromString("ns1.example.com."), Name.fromString("hostmaster.example.com."),
        1L, 7200, 3600, 1209600, 3600);
    RRset rrset = new RRset(badSoa);
    baseZone.addRRset(rrset);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetDisallowSOA() throws Exception {
    baseZone.removeRRset(origin, Type.SOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetDisallowAllNS() throws Exception {
    baseZone.removeRRset(origin, Type.NS);
  }

  @Test
  public void testRemoveRRsetValid() throws Exception {
    // Add a second type (A) at a sub‑domain, then remove it.
    Record a = makeA(www);
    baseZone.addRecord(a);
    assertNotNull(baseZone.findExactMatch(www, Type.A));

    baseZone.removeRRset(www, Type.A);
    assertNull("A RRset must be removed", baseZone.findExactMatch(www, Type.A));
  }

  // ------------------------------------------------------------------------
  // findExactMatch – copy semantics
  // ------------------------------------------------------------------------

  @Test
  public void testFindExactMatchReturnsCopy() throws Exception {
    Record a = makeA(www);
    baseZone.addRecord(a);
    RRset copy = baseZone.findExactMatch(www, Type.A);
    assertNotSame("Returned RRset must be a copy", copy, baseZone.findExactMatch(www, Type.A));

    // Mutate the copy – the original zone must stay unchanged.
    copy.addRR(makeA(Name.fromString("other.example.com.")));
    RRset original = baseZone.findExactMatch(www, Type.A);
    assertEquals("Original RRset size must stay 1", 1, original.size());
  }

  // ------------------------------------------------------------------------
  // findRecords – various response types
  // ------------------------------------------------------------------------

  @Test
  public void testFindRecordsExactSuccessful() throws Exception {
    Record a = makeA(www);
    baseZone.addRecord(a);
    SetResponse resp = baseZone.findRecords(www, Type.A);
    assertTrue("Response should be successful", resp.isSuccessful());
    assertEquals("Exactly one answer RRset expected", 1, resp.answers().size());
    assertEquals("Returned RRset must be the A set", a, resp.answers().get(0).first());
  }

  @Test
  public void testFindRecordsCNAMEFallback() throws Exception {
    // Add a CNAME at www pointing to alias.example.com.
    Record cname = Record.newRecord(www, Type.CNAME, DClass.IN, 3600);
    baseZone.addRecord(cname);

    SetResponse resp = baseZone.findRecords(www, Type.A);
    assertTrue("Response should be a CNAME", resp.isCNAME());
    assertEquals("CNAME RRset must be returned", cname, resp.getCNAME().first());
  }

  @Test
  public void testFindRecordsNxDomain() throws Exception {
    Name outside = Name.fromString("outside.com.");
    SetResponse resp = baseZone.findRecords(outside, Type.A);
    assertTrue("Response must be NXDOMAIN for name outside zone", resp.isNXDOMAIN());
  }

  @Test
  public void testFindRecordsAnyType() throws Exception {
    // Add an A and a TXT record at www.
    Record a = makeA(www);
    Record txt = Record.newRecord(www, Type.TXT, DClass.IN, 3600);
    baseZone.addRecord(a);
    baseZone.addRecord(txt);

    SetResponse resp = baseZone.findRecords(www, Type.ANY);
    assertTrue("ANY query should be successful", resp.isSuccessful());
    // Expect both A and TXT RRsets.
    assertEquals("Two RRsets expected for ANY", 2, resp.answers().size());
  }

  // ------------------------------------------------------------------------
  // Iterator and AXFR iterator
  // ------------------------------------------------------------------------

  @Test
  public void testIteratorOrderSOAFirstThenNS() throws Exception {
    Iterator<RRset> it = baseZone.iterator();
    assertTrue("Iterator must have first element (SOA)", it.hasNext());
    RRset first = it.next();
    assertEquals("First RRset must be SOA", Type.SOA, first.getType());

    assertTrue("Iterator must have second element (NS)", it.hasNext());
    RRset second = it.next();
    assertEquals("Second RRset must be NS", Type.NS, second.getType());
  }

  @Test
  public void testAXFRIteratorReturnsSOAAtEnd() throws Exception {
    // Add an extra A record to make the iterator longer.
    Record a = makeA(www);
    baseZone.addRecord(a);

    Iterator<RRset> axfr = baseZone.AXFR();
    RRset first = axfr.next();               // SOA at start
    assertEquals("First RRset must be SOA", Type.SOA, first.getType());

    // Consume everything else.
    while (axfr.hasNext()) {
      axfr.next();
    }

    // After the loop the iterator should have returned the final SOA.
    // The implementation returns it when hasNext() becomes true after the
    // internal list is exhausted.
    // To verify, we request next() once more (should be the final SOA).
    // The iterator throws NoSuchElementException if we call next() again.
    // Therefore we recreate the iterator and walk to the last element.
    Iterator<RRset> axfr2 = baseZone.AXFR();
    RRset last = null;
    while (axfr2.hasNext()) {
      last = axfr2.next();
    }
    assertNotNull("Last returned RRset must be non‑null", last);
    assertEquals("Last RRset must be SOA", Type.SOA, last.getType());
    assertEquals("SOA at end must equal original SOA", first.first(), last.first());
  }

  @Test
  public void testIteratorRemoveDeletesRRset() throws Exception {
    // Add a second A record at www.
    Record a = makeA(www);
    baseZone.addRecord(a);
    Iterator<RRset> it = baseZone.iterator();

    // Skip SOA and NS
    it.next(); // SOA
    it.next(); // NS

    // Next should be the A RRset we added.
    assertTrue(it.hasNext());
    RRset aSet = it.next();
    assertEquals("Expected A RRset", Type.A, aSet.getType());

    // Remove it via iterator.
    it.remove();

    // Verify removal.
    assertNull("A RRset must be gone after iterator.remove()", baseZone.findExactMatch(www, Type.A));
  }

  // ------------------------------------------------------------------------
  // toMasterFile
  // ------------------------------------------------------------------------

  @Test
  public void testToMasterFileContainsAllRecords() {
    String master = baseZone.toMasterFile();
    // The master file is a simple textual representation; we check for presence
    // of the SOA and NS owners.
    assertTrue("Master output must contain the zone origin", master.contains(origin.toString()));
    assertTrue("Master output must contain the NS target", master.contains(nsTarget.toString()));
  }

  // ------------------------------------------------------------------------
  // Edge case: adding a record with a wildcard name
  // ------------------------------------------------------------------------

  @Test
  public void testWildcardRecordExpansion() throws Exception {
    // Create a wildcard A record: *.example.com.
    Name wildcard = Name.fromString("*.example.com.");
    Record wA = makeA(wildcard);
    baseZone.addRecord(wA);

    // Query for a concrete name that matches the wildcard.
    Name qname = Name.fromString("foo.example.com.");
    SetResponse resp = baseZone.findRecords(qname, Type.A);
    assertTrue("Wildcard match should be successful", resp.isSuccessful());
    assertEquals("Returned RRset must be the expanded wildcard", wA, resp.answers().get(0).first());
    assertEquals("Expanded name must be the queried name",
        qname, resp.answers().get(0).first().getName());
  }

  // ------------------------------------------------------------------------
  // Invalid inputs handling
  // ------------------------------------------------------------------------

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchNullName() throws Exception {
    baseZone.findExactMatch(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordsNullName() throws Exception {
    baseZone.findRecords(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetNullName() throws Exception {
    baseZone.removeRRset(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordNull() throws Exception {
    baseZone.removeRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordNull() throws Exception {
    baseZone.addRecord(null);
  }
}
