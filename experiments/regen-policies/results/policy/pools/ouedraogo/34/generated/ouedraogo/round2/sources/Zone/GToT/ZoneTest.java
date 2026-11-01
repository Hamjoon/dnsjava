package org.xbill.DNS.test;

import org.xbill.DNS.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class ZoneTest {

  private static final Name ORIGIN = Name.fromString("example.com.");
  private static final Name NS_TARGET = Name.fromString("ns1.example.com.");
  private static final Name CNAME_TARGET = Name.fromString("cname-target.example.com.");
  private static final Name DNAME_TARGET = Name.fromString("dname-target.example.com.");
  private static final long TTL = 3600L;

  private SOARecord soaRecord;
  private Record nsRecord;
  private Zone zone;

  /** Helper to create a minimal zone containing a SOA and a single NS record. */
  private Zone createBasicZone() throws IOException {
    soaRecord = new SOARecord(
        ORIGIN,
        DClass.IN,
        TTL,
        NS_TARGET,
        Name.fromString("hostmaster.example.com."),
        2023100101L,
        7200L,
        3600L,
        1209600L,
        3600L);
    nsRecord = Record.newRecord(ORIGIN, Type.NS, DClass.IN, TTL);
    return new Zone(ORIGIN, soaRecord, nsRecord);
  }

  @Before
  public void setUp() throws IOException {
    zone = createBasicZone();
  }

  // -------------------------------------------------------------------------
  // 1.  Public accessor methods
  // -------------------------------------------------------------------------

  @Test
  public void testGetDClass() {
    assertEquals(DClass.IN, zone.getDClass());
  }

  @Test
  public void testGetNS() {
    RRset ns = zone.getNS();
    assertNotNull(ns);
    assertEquals(Type.NS, ns.getType());
    assertEquals(1, ns.size()); // only the NS we added in setUp()
  }

  // -------------------------------------------------------------------------
  // 2.  Constructors – normal and error paths
  // -------------------------------------------------------------------------

  @Test
  public void testConstructorFromRecordsValid() throws IOException {
    Zone z = new Zone(ORIGIN, soaRecord, nsRecord);
    assertNotNull(z);
    assertEquals(soaRecord, z.SOA);
    assertNotNull(z.getNS());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullZone() throws IOException {
    new Zone(null, "dummy.file");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullFile() throws IOException {
    new Zone(ORIGIN, (String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullRecordsArray() throws IOException {
    new Zone(ORIGIN, (Record[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullZoneForArray() throws IOException {
    new Zone(null, soaRecord);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingSOA() throws IOException {
    Record aRecord = Record.newRecord(ORIGIN, Type.A, DClass.IN, TTL);
    new Zone(ORIGIN, aRecord, nsRecord); // no SOA
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingNS() throws IOException {
    new Zone(ORIGIN, soaRecord); // no NS
  }

  @Test(expected = IOException.class)
  public void testConstructorSOAOwnerMismatch() throws IOException {
    Name other = Name.fromString("other.com.");
    SOARecord badSoa = new SOARecord(
        other,
        DClass.IN,
        TTL,
        NS_TARGET,
        Name.fromString("hostmaster.other.com."),
        2023100101L,
        7200L,
        3600L,
        1209600L,
        3600L);
    new Zone(ORIGIN, badSoa, nsRecord);
  }

  // -------------------------------------------------------------------------
  // 3.  addRecord – normal and edge cases
  // -------------------------------------------------------------------------

  @Test
  public void testAddRecordValid() throws IOException {
    Record a = Record.newRecord(Name.fromString("www.example.com."), Type.A, DClass.IN, TTL);
    zone.addRecord(a);
    RRset found = zone.findExactMatch(Name.fromString("www.example.com."), Type.A);
    assertNotNull(found);
    assertEquals(1, found.size());
    assertEquals(a, found.first());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordNull() {
    zone.addRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordOutsideZone() {
    Record a = Record.newRecord(Name.fromString("outside.com."), Type.A, DClass.IN, TTL);
    zone.addRecord(a);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordInvalidSOAOwner() {
    Name bad = Name.fromString("bad.example.com.");
    SOARecord badSoa = new SOARecord(
        bad,
        DClass.IN,
        TTL,
        NS_TARGET,
        Name.fromString("hostmaster.example.com."),
        2023100102L,
        7200L,
        3600L,
        1209600L,
        3600L);
    zone.addRecord(badSoa);
  }

  // -------------------------------------------------------------------------
  // 4.  removeRecord – normal and edge cases
  // -------------------------------------------------------------------------

  @Test
  public void testRemoveRecordValid() throws IOException {
    Name host = Name.fromString("host.example.com.");
    Record a = Record.newRecord(host, Type.A, DClass.IN, TTL);
    zone.addRecord(a);
    zone.removeRecord(a);
    assertNull(zone.findExactMatch(host, Type.A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordNull() {
    zone.removeRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordSOA() {
    zone.removeRecord(soaRecord);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveLastNS() {
    zone.removeRecord(nsRecord); // the only NS present
  }

  // -------------------------------------------------------------------------
  // 5.  addRRset – normal and edge cases
  // -------------------------------------------------------------------------

  @Test
  public void testAddRRsetValid() throws IOException {
    Name mxName = Name.fromString("example.com.");
    Record mx = Record.newRecord(mxName, Type.MX, DClass.IN, TTL);
    RRset mxSet = new RRset(mx);
    zone.addRRset(mxSet);
    RRset retrieved = zone.findExactMatch(mxName, Type.MX);
    assertNotNull(retrieved);
    assertEquals(1, retrieved.size());
    assertEquals(mx, retrieved.first());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNull() {
    zone.addRRset(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetOutsideZone() {
    Name other = Name.fromString("outside.com.");
    Record a = Record.newRecord(other, Type.A, DClass.IN, TTL);
    RRset set = new RRset(a);
    zone.addRRset(set);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetInvalidSOA() {
    // create a SOA RRset with two SOA records – violates the “exactly 1 SOA” rule
    SOARecord soa2 = new SOARecord(
        ORIGIN,
        DClass.IN,
        TTL,
        NS_TARGET,
        Name.fromString("hostmaster.example.com."),
        2023100102L,
        7200L,
        3600L,
        1209600L,
        3600L);
    RRset badSoaSet = new RRset(soaRecord, soa2);
    zone.addRRset(badSoaSet);
  }

  // -------------------------------------------------------------------------
  // 6.  removeRRset – normal and edge cases
  // -------------------------------------------------------------------------

  @Test
  public void testRemoveRRsetValid() throws IOException {
    // add an extra MX RRset then remove it
    Name mxName = Name.fromString("example.com.");
    Record mx = Record.newRecord(mxName, Type.MX, DClass.IN, TTL);
    RRset mxSet = new RRset(mx);
    zone.addRRset(mxSet);
    assertNotNull(zone.findExactMatch(mxName, Type.MX));
    zone.removeRRset(mxName, Type.MX);
    assertNull(zone.findExactMatch(mxName, Type.MX));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetNullName() throws IOException {
    zone.removeRRset(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetSOA() throws IOException {
    zone.removeRRset(ORIGIN, Type.SOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetAllNS() throws IOException {
    // Only one NS exists; attempting to remove it should fail
    zone.removeRRset(ORIGIN, Type.NS);
  }

  // -------------------------------------------------------------------------
  // 7.  findExactMatch – normal and edge cases
  // -------------------------------------------------------------------------

  @Test
  public void testFindExactMatchFound() throws IOException {
    Name aName = Name.fromString("a.example.com.");
    Record a = Record.newRecord(aName, Type.A, DClass.IN, TTL);
    zone.addRecord(a);
    RRset rs = zone.findExactMatch(aName, Type.A);
    assertNotNull(rs);
    assertEquals(1, rs.size());
    assertEquals(a, rs.first());
  }

  @Test
  public void testFindExactMatchNotFound() {
    RRset rs = zone.findExactMatch(Name.fromString("nonexistent.example.com."), Type.A);
    assertNull(rs);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchNullName() {
    zone.findExactMatch(null, Type.A);
  }

  // -------------------------------------------------------------------------
  // 8.  findRecords – normal and edge cases
  // -------------------------------------------------------------------------

  @Test
  public void testFindRecordsNXDOMAIN() {
    SetResponse sr = zone.findRecords(Name.fromString("outside.com."), Type.A);
    assertTrue(sr.isNXDOMAIN());
  }

  @Test
  public void testFindRecordsCNAME() throws IOException {
    Name alias = Name.fromString("alias.example.com.");
    Record cname = Record.newRecord(alias, Type.CNAME, DClass.IN, TTL);
    zone.addRecord(cname);
    SetResponse sr = zone.findRecords(alias, Type.A);
    assertTrue(sr.isCNAME());
    assertNotNull(sr.getCNAME());
    assertEquals(alias, sr.getCNAME().getName());
  }

  @Test
  public void testFindRecordsWildcard() throws IOException {
    // Create a wildcard A record: *.example.com.
    Name wildcard = Name.fromString("*.example.com.");
    Record w = Record.newRecord(wildcard, Type.A, DClass.IN, TTL);
    zone.addRecord(w);

    // Query a name that does not exist explicitly but matches the wildcard
    Name q = Name.fromString("foo.example.com.");
    SetResponse sr = zone.findRecords(q, Type.A);
    assertTrue(sr.isSuccessful());
    assertEquals(1, sr.answers().size());
    RRset answer = sr.answers().get(0);
    assertEquals(Type.A, answer.getType());
    // The returned record should have the queried name (expanded)
    Record expanded = answer.first();
    assertEquals(q, expanded.getName());
  }

  @Test
  public void testFindRecordsDelegation() throws IOException {
    // Create a delegation by adding an NS set under a sub‑domain
    Name sub = Name.fromString("sub.example.com.");
    Record ns1 = Record.newRecord(sub, Type.NS, DClass.IN, TTL);
    zone.addRecord(ns1);
    SetResponse sr = zone.findRecords(Name.fromString("host.sub.example.com."), Type.A);
    assertTrue(sr.isDelegation());
    assertNotNull(sr.getNS());
    assertEquals(Type.NS, sr.getNS().getType());
  }

  // -------------------------------------------------------------------------
  // 9.  Iterators (iterator() and AXFR())
  // -------------------------------------------------------------------------

  @Test
  public void testIteratorContainsSOAandNS() {
    Iterator<RRset> it = zone.iterator();
    boolean sawSOA = false;
    boolean sawNS = false;
    while (it.hasNext()) {
      RRset rs = it.next();
      if (rs.getType() == Type.SOA) {
        sawSOA = true;
      } else if (rs.getType() == Type.NS) {
        sawNS = true;
      }
    }
    assertTrue("SOA not found in iterator", sawSOA);
    assertTrue("NS not found in iterator", sawNS);
  }

  @Test
  public void testAXFREndsWithSOA() {
    Iterator<RRset> it = zone.AXFR();
    RRset last = null;
    while (it.hasNext()) {
      last = it.next();
    }
    assertNotNull(last);
    assertEquals(Type.SOA, last.getType());
  }

  @Test(expected = NoSuchElementException.class)
  public void testIteratorNoSuchElement() {
    Iterator<RRset> it = zone.iterator();
    while (it.hasNext()) {
      it.next();
    }
    // one more call should throw
    it.next();
  }

  @Test
  public void testIteratorRemove() throws IOException {
    // add an extra TXT RRset so that removal is possible without touching SOA/NS
    Name txtName = Name.fromString("txt.example.com.");
    Record txt = Record.newRecord(txtName, Type.TXT, DClass.IN, TTL);
    RRset txtSet = new RRset(txt);
    zone.addRRset(txtSet);

    Iterator<RRset> it = zone.iterator();
    while (it.hasNext()) {
      RRset rs = it.next();
      if (rs.getType() == Type.TXT) {
        it.remove(); // should delete the TXT RRset
        break;
      }
    }
    assertNull(zone.findExactMatch(txtName, Type.TXT));
  }

  // -------------------------------------------------------------------------
  // 10.  toMasterFile / toString
  // -------------------------------------------------------------------------

  @Test
  public void testToMasterFileContainsOriginAndNS() {
    String mf = zone.toMasterFile();
    assertTrue(mf.contains(ORIGIN.toString()));
    assertTrue(mf.contains(nsRecord.toString()));
  }

  @Test
  public void testToStringDelegatesToMasterFile() {
    assertEquals(zone.toMasterFile(), zone.toString());
  }
}
