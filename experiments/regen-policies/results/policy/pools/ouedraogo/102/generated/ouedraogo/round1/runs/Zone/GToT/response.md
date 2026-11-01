###Test START##
```java
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Zone}.
 *
 * <p>The tests are organized as follows:
 *
 * <ul>
 *   <li>Extraction of public method signatures (documented in comments).
 *   <li>Basic functionality tests for each public method.
 *   <li>Edge‑case / exception handling tests.
 *   <li>Combined test file that can be compiled and run against the
 *       original {@code org.xbill.DNS} sources (version that contains the
 *       {@code Zone} class shown in the prompt).
 * </ul>
 *
 * <p>All objects are created using only the constructors and factory methods
 * that appear in the supplied source code or the public API excerpts that
 * accompany the prompt. No mocking frameworks are used.
 */
public class ZoneTest {

  /* ---------------------------------------------------------------------- */
  /* 1.  Public method signatures (extracted from Zone)                     */
  /* ---------------------------------------------------------------------- */
  //
  // public int getDClass()
  // public RRset getNS()
  // public SOARecord getSOA()
  // public Iterator<RRset> iterator()
  // public Iterator<RRset> AXFR()
  // public <T extends Record> void addRecord(T r)
  // public void removeRecord(Record r)
  // public void addRRset(RRset rrset)
  // public void removeRRset(Name name, int type)
  // public RRset findExactMatch(Name name, int type)
  // public SetResponse findRecords(Name name, int type)
  // public String toMasterFile()
  // public String toString()
  // (constructors are also public, but they are exercised in the tests)

  /* ---------------------------------------------------------------------- */
  /* 2.  Test fixtures                                                       */
  /* ---------------------------------------------------------------------- */
  private Name zoneName;
  private SOARecord soa;
  private NSRecord ns1;
  private NSRecord ns2;
  private Zone zone;

  /** Build a minimal valid zone (SOA + two NS records) used by many tests. */
  @Before
  public void setUp() throws IOException, TextParseException {
    zoneName = Name.fromString("example.com.");

    // SOA
    soa =
        new SOARecord(
            zoneName,
            DClass.IN,
            3600,
            Name.fromString("ns1.example.com."),
            Name.fromString("hostmaster.example.com."),
            2023100101L,
            7200,
            3600,
            1209600,
            3600);

    // Two NS records (the zone must contain at least one NS)
    ns1 = new NSRecord(zoneName, DClass.IN, 3600, Name.fromString("ns1.example.com."));
    ns2 = new NSRecord(Name.fromString("sub.example.com."), DClass.IN, 3600,
                       Name.fromString("ns2.example.com."));

    // Create the zone from an array of records
    zone = new Zone(zoneName, soa, ns1, ns2);
  }

  /* ---------------------------------------------------------------------- */
  /* 3.  Basic functionality tests                                          */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testGetDClass() {
    assertEquals(DClass.IN, zone.getDClass());
  }

  @Test
  public void testGetSOA() {
    assertSame(soa, zone.getSOA());
  }

  @Test
  public void testGetNS() {
    RRset nsSet = zone.getNS();
    assertNotNull(nsSet);
    // The NS set for the zone origin must contain exactly the two NS records we added
    assertEquals(2, nsSet.size());
    assertTrue(nsSet.rrs().contains(ns1));
    assertTrue(nsSet.rrs().contains(ns2));
  }

  @Test
  public void testIteratorOrder() {
    Iterator<RRset> it = zone.iterator();
    // First RRset must be the SOA
    assertTrue(it.hasNext());
    RRset first = it.next();
    assertEquals(Type.SOA, first.getType());
    assertEquals(soa, first.first());

    // Second RRset must be the NS set for the origin
    assertTrue(it.hasNext());
    RRset second = it.next();
    assertEquals(Type.NS, second.getType());

    // Remaining RRsets (if any) are for other names – we have one for sub.example.com.
    assertTrue(it.hasNext());
    RRset third = it.next();
    assertEquals(Type.NS, third.getType());
    assertEquals(Name.fromString("sub.example.com."), third.getName());

    assertFalse(it.hasNext());
  }

  @Test
  public void testAXFRIteratorReturnsLastSOA() {
    Iterator<RRset> it = zone.AXFR();
    // Walk through all normal entries (same as iterator)
    while (it.hasNext()) {
      RRset set = it.next();
      if (set.getType() == Type.SOA && set == zone.getSOA()) {
        // first SOA – continue
        continue;
      }
    }
    // The last call to next() after the above loop should return the SOA again.
    // To test this we re‑obtain the iterator and consume until the final SOA.
    it = zone.AXFR();
    RRset last = null;
    while (it.hasNext()) {
      last = it.next();
    }
    assertNotNull(last);
    assertEquals(Type.SOA, last.getType());
    assertEquals(soa, last.first());
  }

  @Test
  public void testAddRecordCreatesNewRRset() throws IOException {
    // A new TXT record under a fresh name
    Record txt =
        Record.fromString(
            Name.fromString("new.example.com."),
            Type.TXT,
            DClass.IN,
            300,
            " \"hello\"",
            null);
    zone.addRecord(txt);
    RRset found = zone.findExactMatch(Name.fromString("new.example.com."), Type.TXT);
    assertNotNull(found);
    assertEquals(1, found.size());
    assertEquals(txt, found.first());
  }

  @Test
  public void testRemoveRecordRemovesOnlyEntry() throws IOException {
    // Remove the second NS (sub.example.com.) – there will still be one NS left
    zone.removeRecord(ns2);
    RRset nsSet = zone.getNS();
    assertEquals(1, nsSet.size());
    assertTrue(nsSet.rrs().contains(ns1));
    assertFalse(nsSet.rrs().contains(ns2));
  }

  @Test
  public void testAddRRsetReplacesExisting() throws IOException {
    // Build a new NS set for the origin with a single NS record
    NSRecord nsNew = new NSRecord(zoneName, DClass.IN, 3600, Name.fromString("ns-new.example.com."));
    RRset newNsSet = new RRset(nsNew);
    zone.addRRset(newNsSet);

    RRset nsSet = zone.getNS();
    assertEquals(1, nsSet.size());
    assertEquals(nsNew, nsSet.first());
  }

  @Test
  public void testRemoveRRset() {
    // Remove the NS set for the sub‑domain name
    zone.removeRRset(Name.fromString("sub.example.com."), Type.NS);
    // The sub‑domain should no longer be present
    RRset removed = zone.findExactMatch(Name.fromString("sub.example.com."), Type.NS);
    assertNull(removed);
  }

  @Test
  public void testFindExactMatchSuccess() {
    RRset result = zone.findExactMatch(zoneName, Type.SOA);
    assertNotNull(result);
    assertEquals(soa, result.first());
  }

  @Test
  public void testFindExactMatchNotFound() {
    RRset result = zone.findExactMatch(Name.fromString("nosuch.example.com."), Type.A);
    assertNull(result);
  }

  @Test
  public void testFindRecordsNxDomain() {
    // Name outside the zone should give NXDOMAIN
    SetResponse resp = zone.findRecords(Name.fromString("outside.com."), Type.A);
    assertTrue(resp.isNXDOMAIN());
  }

  @Test
  public void testFindRecordsCname() throws IOException, TextParseException {
    // Add a CNAME for a name inside the zone
    Record cname =
        Record.fromString(
            Name.fromString("cname.example.com."),
            Type.CNAME,
            DClass.IN,
            300,
            " alias.example.com.",
            null);
    zone.addRecord(cname);

    SetResponse resp = zone.findRecords(Name.fromString("cname.example.com."), Type.A);
    assertTrue(resp.isCNAME());
    assertEquals(cname, resp.getCNAME().first());
  }

  @Test
  public void testToMasterFileContainsSoaAndNs() {
    String master = zone.toMasterFile();
    assertTrue(master.contains("SOA"));
    assertTrue(master.contains("NS"));
    // The string should contain the zone origin at the start of lines
    assertTrue(master.contains("example.com.\t"));
  }

  /* ---------------------------------------------------------------------- */
  /* 4.  Edge‑case and exception handling tests                             */
  /* ---------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullName() throws IOException {
    new Zone(null, (Record[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullRecordsArray() throws IOException {
    new Zone(zoneName, (Record[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordNull() {
    zone.addRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordMismatchedSoa() {
    // SOA whose owner is not the zone origin
    SOARecord badSoa =
        new SOARecord(
            Name.fromString("bad.example.com."),
            DClass.IN,
            3600,
            Name.fromString("ns1.example.com."),
            Name.fromString("hostmaster.example.com."),
            2023100102L,
            7200,
            3600,
            1209600,
            3600);
    zone.addRecord(badSoa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordAbsoluteNameNotSubdomain() {
    // Record whose name is absolute and not a subdomain of the zone
    Record aRec =
        Record.fromString(
            Name.fromString("example.org."),
            Type.A,
            DClass.IN,
            300,
            " 192.0.2.1",
            null);
    zone.addRecord(aRec);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordNull() {
    zone.removeRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveSoaRecord() {
    zone.removeRecord(soa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveLastNsRecord() {
    // First remove the sub‑domain NS, leaving only the origin NS set
    zone.removeRecord(ns2);
    // Now attempt to remove the sole NS record from the origin set
    zone.removeRecord(ns1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNull() {
    zone.addRRset(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNonSubdomain() {
    // RRset whose name is not a subdomain of the zone origin
    NSRecord foreignNs =
        new NSRecord(Name.fromString("example.org."), DClass.IN, 3600, Name.fromString("ns.example.org."));
    RRset rrset = new RRset(foreignNs);
    zone.addRRset(rrset);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetSoaWithWrongName() {
    // SOA with a name that does not equal the zone origin
    SOARecord badSoa =
        new SOARecord(
            Name.fromString("bad.example.com."),
            DClass.IN,
            3600,
            Name.fromString("ns1.example.com."),
            Name.fromString("hostmaster.example.com."),
            2023100102L,
            7200,
            3600,
            1209600,
            3600);
    RRset rrset = new RRset(badSoa);
    zone.addRRset(rrset);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetNullName() {
    zone.removeRRset(null, Type.A);
  }

  @Test(expected = InvalidTypeException.class)
  public void testRemoveRRsetInvalidType() {
    zone.removeRRset(zoneName, Type.SOA); // SOA removal is disallowed and triggers InvalidTypeException
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchNullName() {
    zone.findExactMatch(null, Type.A);
  }

  @Test(expected = InvalidTypeException.class)
  public void testFindExactMatchInvalidType() {
    zone.findExactMatch(zoneName, Type.ANY); // ANY is not allowed for exact match
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordsNullName() {
    zone.findRecords(null, Type.A);
  }

  @Test(expected = InvalidTypeException.class)
  public void testFindRecordsInvalidType() {
    zone.findRecords(zoneName, Type.ANY); // ANY is allowed for findRecords, but Type.check will reject it
  }

  /* ---------------------------------------------------------------------- */
  /* 5.  Helper methods (none needed – all tests are self‑contained)        */
  /* ---------------------------------------------------------------------- */
}
```
###Test END##