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
 * JUnit‑4 test suite for {@link Zone}. The tests cover:
 * <ul>
 *   <li>All public methods (basic functionality)</li>
 *   <li>Edge‑cases and exception handling</li>
 *   <li>Iterators (normal and AXFR)</li>
 *   <li>Record manipulation (add/remove)</li>
 *   <li>Lookup methods (exact match, generic lookup, wildcard, delegation, CNAME)</li>
 * </ul>
 */
public class ZoneTest {

  /** Common names used in the test zone. */
  private Name origin;
  private Name nsName;
  private Name aName;
  private Name cnameName;
  private Name wildName;
  private Name delegationName;

  /** Records that form the minimal valid zone (SOA + NS). */
  private SOARecord soa;
  private NSRecord ns;

  /** The zone under test – built from the records above. */
  private Zone zone;

  /** Helper to create a simple zone with a set of records. */
  private Zone createZone(Record... records) throws IOException {
    return new Zone(origin, records);
  }

  @Before
  public void setUp() throws Exception {
    // origin = example.com.
    origin = Name.fromString("example.com.");

    // NS record for the zone apex
    nsName = Name.fromString("ns1.example.com.");
    ns = new NSRecord(origin, DClass.IN, 3600, nsName);

    // SOA record (owner must be the origin)
    Name mname = Name.fromString("master.example.com.");
    Name rname = Name.fromString("hostmaster.example.com.");
    soa = new SOARecord(
        origin,
        DClass.IN,
        3600,
        mname,
        rname,
        2023101501L,
        7200,
        3600,
        1209600,
        3600);

    // Additional records used in tests
    aName = Name.fromString("www.example.com.");
    cnameName = Name.fromString("alias.example.com.");
    wildName = Name.fromString("*.example.com."); // wildcard entry
    delegationName = Name.fromString("sub.example.com.");

    // Build a zone that contains SOA, NS, an A, a CNAME, a wildcard A and a delegated NS.
    Record aRecord = new ARecord(aName, DClass.IN, 300, java.net.InetAddress.getByName("192.0.2.1"));
    Record cnameRecord = new CNAMERecord(cnameName, DClass.IN, 300,
        Name.fromString("www.example.com."));
    Record wildARecord = new ARecord(wildName, DClass.IN, 300,
        java.net.InetAddress.getByName("192.0.2.99"));
    // Delegation: NS at sub.example.com. (different from zone apex)
    NSRecord delegationNs = new NSRecord(delegationName, DClass.IN, 300,
        Name.fromString("ns.sub.example.com."));

    zone = createZone(soa, ns, aRecord, cnameRecord, wildARecord, delegationNs);
  }

  /* -------------------------------------------------------------------------
   *  1.  Simple accessor methods
   * ------------------------------------------------------------------------- */

  @Test
  public void testGetDClass() {
    assertEquals(DClass.IN, zone.getDClass());
  }

  @Test
  public void testGetSOA() {
    assertNotNull(zone.getSOA());
    assertEquals(soa, zone.getSOA());
  }

  @Test
  public void testGetNS() {
    RRset nsSet = zone.getNS();
    assertNotNull(nsSet);
    assertEquals(Type.NS, nsSet.getType());
    assertEquals(1, nsSet.size());
    assertEquals(ns, nsSet.first());
  }

  /* -------------------------------------------------------------------------
   *  2.  Constructors – argument validation
   * ------------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullZoneName() throws IOException {
    new Zone(null, (InputStream) new ByteArrayInputStream(new byte[0]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullInputStream() throws IOException {
    new Zone(origin, (InputStream) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullZoneArrayRecords() throws IOException {
    new Zone(null, new Record[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullRecordsArray() throws IOException {
    new Zone(origin, (Record[]) null);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingSOA() throws IOException {
    // zone with only NS – validation should fail
    new Zone(origin, ns);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingNS() throws IOException {
    // zone with only SOA – validation should fail
    new Zone(origin, soa);
  }

  /* -------------------------------------------------------------------------
   *  3.  addRecord – normal and edge cases
   * ------------------------------------------------------------------------- */

  @Test
  public void testAddRecordNewType() throws Exception {
    Name txtName = Name.fromString("txt.example.com.");
    TXTRecord txt = new TXTRecord(txtName, DClass.IN, 300, "test");
    zone.addRecord(txt);
    RRset found = zone.findExactMatch(txtName, Type.TXT);
    assertNotNull(found);
    assertEquals(1, found.size());
    assertEquals(txt, found.first());
  }

  @Test
  public void testAddRecordReplaceSOA() throws Exception {
    // Build a different SOA (same owner)
    SOARecord newSOA = new SOARecord(
        origin,
        DClass.IN,
        3600,
        Name.fromString("master2.example.com."),
        Name.fromString("hostmaster2.example.com."),
        2023101502L,
        7200,
        3600,
        1209600,
        3600);
    zone.addRecord(newSOA);
    assertEquals(newSOA, zone.getSOA());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordNull() {
    zone.addRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordSOAOwnerMismatch() throws Exception {
    // SOA whose name is not the zone origin
    Name other = Name.fromString("other.com.");
    SOARecord badSOA = new SOARecord(
        other,
        DClass.IN,
        3600,
        Name.fromString("m.other.com."),
        Name.fromString("h.other.com."),
        1,
        1,
        1,
        1,
        1);
    zone.addRecord(badSOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordOutsideSubdomain() throws Exception {
    Name external = Name.fromString("outside.com.");
    ARecord a = new ARecord(external, DClass.IN, 300,
        java.net.InetAddress.getByName("203.0.113.5"));
    zone.addRecord(a);
  }

  /* -------------------------------------------------------------------------
   *  4.  removeRecord – normal and edge cases
   * ------------------------------------------------------------------------- */

  @Test
  public void testRemoveRecordExisting() throws Exception {
    // Remove the A record we added in setUp()
    Record aRec = new ARecord(aName, DClass.IN, 300,
        java.net.InetAddress.getByName("192.0.2.1"));
    zone.removeRecord(aRec);
    assertNull(zone.findExactMatch(aName, Type.A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordNull() {
    zone.removeRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordSOA() {
    zone.removeRecord(soa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveLastNS() {
    // The zone only has a single NS at the apex; trying to remove it must fail.
    zone.removeRecord(ns);
  }

  /* -------------------------------------------------------------------------
   *  5.  addRRset – normal and edge cases
   * ------------------------------------------------------------------------- */

  @Test
  public void testAddRRsetReplace() throws Exception {
    // Build a new NS RRset (same name, same type) with a different target
    NSRecord ns2 = new NSRecord(origin, DClass.IN, 3600,
        Name.fromString("ns2.example.com."));
    RRset newNsSet = new RRset(ns2);
    zone.addRRset(newNsSet);
    RRset fetched = zone.getNS();
    assertEquals(1, fetched.size());
    assertEquals(ns2, fetched.first());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNull() {
    zone.addRRset(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetSOAOwnerMismatch() throws Exception {
    Name other = Name.fromString("other.com.");
    SOARecord badSOA = new SOARecord(
        other,
        DClass.IN,
        3600,
        Name.fromString("m.other.com."),
        Name.fromString("h.other.com."),
        1,
        1,
        1,
        1,
        1);
    RRset set = new RRset(badSOA);
    zone.addRRset(set);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetSOAWrongSize() throws Exception {
    // Create an RRset that contains two SOA records – illegal
    SOARecord soa2 = new SOARecord(
        origin,
        DClass.IN,
        3600,
        Name.fromString("master2.example.com."),
        Name.fromString("hostmaster2.example.com."),
        2,
        7200,
        3600,
        1209600,
        3600);
    RRset set = new RRset(soa, soa2);
    zone.addRRset(set);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetOutsideSubdomain() throws Exception {
    Name external = Name.fromString("outside.com.");
    ARecord a = new ARecord(external, DClass.IN, 300,
        java.net.InetAddress.getByName("203.0.113.5"));
    RRset set = new RRset(a);
    zone.addRRset(set);
  }

  /* -------------------------------------------------------------------------
   *  6.  removeRRset – normal and edge cases
   * ------------------------------------------------------------------------- */

  @Test
  public void testRemoveRRsetExisting() throws Exception {
    // Remove the CNAME RRset we added in setUp()
    zone.removeRRset(cnameName, Type.CNAME);
    assertNull(zone.findExactMatch(cnameName, Type.CNAME));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetNullName() throws Exception {
    zone.removeRRset(null, Type.A);
  }

  @Test(expected = InvalidTypeException.class)
  public void testRemoveRRsetInvalidType() throws Exception {
    // Type.SOA is not allowed to be removed via removeRRset
    zone.removeRRset(origin, Type.SOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetRemoveAllNS() throws Exception {
    // Attempt to remove the sole NS RRset at the zone apex
    zone.removeRRset(origin, Type.NS);
  }

  /* -------------------------------------------------------------------------
   *  7.  findExactMatch – normal and edge cases
   * ------------------------------------------------------------------------- */

  @Test
  public void testFindExactMatchExists() {
    RRset rr = zone.findExactMatch(aName, Type.A);
    assertNotNull(rr);
    assertEquals(1, rr.size());
  }

  @Test
  public void testFindExactMatchMissing() {
    Name missing = Name.fromString("missing.example.com.");
    assertNull(zone.findExactMatch(missing, Type.A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchNullName() {
    zone.findExactMatch(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchInvalidTypeANY() {
    zone.findExactMatch(aName, Type.ANY);
  }

  /* -------------------------------------------------------------------------
   *  8.  findRecords – various response types
   * ------------------------------------------------------------------------- */

  @Test
  public void testFindRecordsExactSuccess() {
    SetResponse resp = zone.findRecords(aName, Type.A);
    assertTrue(resp.isSuccessful());
    assertEquals(1, resp.answers().size());
    assertEquals(Type.A, resp.answers().get(0).getType());
  }

  @Test
  public void testFindRecordsCNAME() {
    SetResponse resp = zone.findRecords(cnameName, Type.A);
    assertTrue(resp.isCNAME());
    assertNotNull(resp.getCNAME());
    assertEquals(cnameName, resp.getCNAME().getName());
  }

  @Test
  public void testFindRecordsDelegation() {
    SetResponse resp = zone.findRecords(delegationName, Type.A);
    assertTrue(resp.isDelegation());
    assertNotNull(resp.getNS());
    assertEquals(Type.NS, resp.getNS().getType());
  }

  @Test
  public void testFindRecordsWildcard() {
    Name wildcardTarget = Name.fromString("wildcarded.example.com.");
    SetResponse resp = zone.findRecords(wildcardTarget, Type.A);
    assertTrue(resp.isSuccessful());
    RRset ans = resp.answers().get(0);
    // The answer must be the wildcard A record expanded to the queried name
    assertEquals(wildcardTarget, ans.getName());
    assertEquals(Type.A, ans.getType());
    assertEquals(1, ans.size());
  }

  @Test
  public void testFindRecordsAny() {
    SetResponse resp = zone.findRecords(aName, Type.ANY);
    assertTrue(resp.isSuccessful());
    // ANY should return all RRsets for the exact name (A and possibly SIGs)
    assertFalse(resp.answers().isEmpty());
    boolean foundA = false;
    for (RRset r : resp.answers()) {
      if (r.getType() == Type.A) {
        foundA = true;
        break;
      }
    }
    assertTrue(foundA);
  }

  @Test
  public void testFindRecordsNXDOMAINOutsideZone() {
    Name outside = Name.fromString("outside.com.");
    SetResponse resp = zone.findRecords(outside, Type.A);
    assertTrue(resp.isNXDOMAIN());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordsNullName() {
    zone.findRecords(null, Type.A);
  }

  /* -------------------------------------------------------------------------
   *  9.  Iterator behavior (regular and AXFR)
   * ------------------------------------------------------------------------- */

  @Test
  public void testIteratorOrderFirstSOAThenNS() {
    Iterator<RRset> it = zone.iterator();
    assertTrue(it.hasNext());
    RRset first = it.next();
    assertEquals(Type.SOA, first.getType());

    assertTrue(it.hasNext());
    RRset second = it.next();
    assertEquals(Type.NS, second.getType());
  }

  @Test
  public void testAXFRIteratorEndsWithSOA() {
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
    // Exhaust the iterator
    while (it.hasNext()) {
      it.next();
    }
    // One more call must throw
    it.next();
  }

  @Test
  public void testIteratorRemoveWorks() {
    // Add a temporary TXT record so we can remove it via iterator
    Name txtName = Name.fromString("tmp.example.com.");
    TXTRecord txt = new TXTRecord(txtName, DClass.IN, 300, "temp");
    zone.addRecord(txt);

    Iterator<RRset> it = zone.iterator();
    boolean removed = false;
    while (it.hasNext()) {
      RRset set = it.next();
      if (set.getType() == Type.TXT && set.getName().equals(txtName)) {
        it.remove();
        removed = true;
        break;
      }
    }
    assertTrue("TXT RRset should have been removed via iterator", removed);
    assertNull(zone.findExactMatch(txtName, Type.TXT));
  }

  @Test(expected = IllegalStateException.class)
  public void testIteratorRemoveWithoutNext() {
    Iterator<RRset> it = zone.iterator();
    it.remove(); // should throw IllegalStateException
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIteratorRemoveSOANotAllowed() {
    Iterator<RRset> it = zone.iterator();
    // First element is the SOA – attempt to remove it
    it.next(); // returns SOA
    it.remove(); // must raise IllegalArgumentException inside ZoneIterator.remove()
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIteratorRemoveNSNotAllowed() {
    Iterator<RRset> it = zone.iterator();
    it.next(); // SOA
    it.next(); // NS
    it.remove(); // removing the NS at the apex is illegal
  }

  /* -------------------------------------------------------------------------
   * 10.  toMasterFile / toString – basic sanity check
   * ------------------------------------------------------------------------- */

  @Test
  public void testToMasterFileContainsSOAAndNS() {
    String master = zone.toMasterFile();
    assertTrue(master.contains("SOA"));
    assertTrue(master.contains("NS"));
    // The string representation should be identical to toMasterFile()
    assertEquals(master, zone.toString());
  }
}
```
###Test END##