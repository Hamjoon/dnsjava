package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link Zone}.  The tests cover:
 *
 * <ul>
 *   <li>All public methods (including constructors)</li>
 *   <li>Normal behaviour</li>
 *   <li>Edge‑cases and exception handling (null arguments, illegal owners,
 *       removal of mandatory records, invalid types, etc.)</li>
 *   <li>Iteration (normal iterator and AXFR iterator)</li>
 *   <li>Wildcard, CNAME and delegation handling</li>
 * </ul>
 *
 * The test class uses only the public API that is present in the supplied
 * source code and the additional record classes that are part of the same
 * library (NSRecord, ARecord, CNAMERecord, DNAMERecord, etc.).
 */
public class ZoneTest {

  /** Common zone origin used by most tests. */
  private static final Name ORIGIN = Name.fromString("example.com.");

  /** Minimal required records for a valid zone: one SOA and at least one NS. */
  private SOARecord soa;
  private NSRecord ns1;
  private Zone zone; // the zone under test

  /**
   * Build a fresh, valid zone before each test.  The zone is created using the
   * {@link Zone#Zone(Name, Record...)} constructor which validates the presence
   * of exactly one SOA and at least one NS.
   */
  @Before
  public void setUp() throws IOException {
    // SOA: primary name server = ns1.example.com., admin = hostmaster.example.com.
    Name primary = Name.fromString("ns1.example.com.");
    Name admin = Name.fromString("hostmaster.example.com.");
    soa = new SOARecord(
        ORIGIN,
        DClass.IN,
        3600,
        primary,
        admin,
        2023100101L,
        7200,
        3600,
        1209600,
        3600);

    // NS record for the zone
    ns1 = new NSRecord(ORIGIN, DClass.IN, 3600, primary);

    // Create the zone with the mandatory records
    zone = new Zone(ORIGIN, soa, ns1);
  }

  /* -----------------------------------------------------------------------
   *  Public getters
   * ----------------------------------------------------------------------- */

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
    assertEquals(1, nsSet.size());
    assertEquals(Type.NS, nsSet.getType());
    assertEquals(ns1, nsSet.first());
  }

  /* -----------------------------------------------------------------------
   *  Constructors – normal and exceptional paths
   * ----------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullName() throws IOException {
    new Zone(null, (Record[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullRecordsArray() throws IOException {
    new Zone(ORIGIN, (Record[]) null);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingSOA() throws IOException {
    new Zone(ORIGIN, ns1); // only NS, no SOA
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingNS() throws IOException {
    new Zone(ORIGIN, soa); // only SOA, no NS
  }

  @Test
  public void testConstructorFromInputStream() throws IOException {
    // Build a tiny master file in memory – SOA + NS
    String master =
        "$ORIGIN example.com.\n"
            + "example.com. 3600 IN SOA ns1.example.com. hostmaster.example.com. 2023100101 7200 3600 1209600 3600\n"
            + "example.com. 3600 IN NS ns1.example.com.\n";
    ByteArrayInputStream in = new ByteArrayInputStream(master.getBytes());
    Zone z = new Zone(ORIGIN, in);
    assertNotNull(z.getSOA());
    assertNotNull(z.getNS());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorFromInputStreamNullStream() throws IOException {
    new Zone(ORIGIN, (InputStream) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorFromFileNullFile() throws IOException {
    new Zone(ORIGIN, (String) null);
  }

  /* -----------------------------------------------------------------------
   *  addRecord – normal and error cases
   * ----------------------------------------------------------------------- */

  @Test
  public void testAddRecordSimpleA() throws IOException {
    Name aName = Name.fromString("www.example.com.");
    ARecord a = new ARecord(aName, DClass.IN, 300, InetAddress.getByName("192.0.2.1"));
    zone.addRecord(a);
    RRset result = zone.findExactMatch(aName, Type.A);
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(a, result.first());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordNull() {
    zone.addRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordOutsideZone() {
    Name external = Name.fromString("outside.com.");
    NSRecord ns = new NSRecord(external, DClass.IN, 3600, Name.fromString("ns.outside.com."));
    zone.addRecord(ns);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordSOAOwnerMismatch() throws IOException {
    Name other = Name.fromString("other.com.");
    SOARecord badSoa =
        new SOARecord(
            other,
            DClass.IN,
            3600,
            Name.fromString("ns.other.com."),
            Name.fromString("hostmaster.other.com."),
            2023100102L,
            7200,
            3600,
            1209600,
            3600);
    zone.addRecord(badSoa);
  }

  /* -----------------------------------------------------------------------
   *  removeRecord – normal and error cases
   * ----------------------------------------------------------------------- */

  @Test
  public void testRemoveRecordA() throws IOException {
    Name aName = Name.fromString("mail.example.com.");
    ARecord a = new ARecord(aName, DClass.IN, 300, InetAddress.getByName("192.0.2.10"));
    zone.addRecord(a);
    assertNotNull(zone.findExactMatch(aName, Type.A));

    zone.removeRecord(a);
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
    // Zone currently has exactly one NS (ns1). Attempting to remove it must fail.
    zone.removeRecord(ns1);
  }

  /* -----------------------------------------------------------------------
   *  addRRset – normal and error cases
   * ----------------------------------------------------------------------- */

  @Test
  public void testAddRRsetReplace() throws IOException {
    // Create a second NS record for the same name – it should replace the existing one.
    NSRecord ns2 = new NSRecord(ORIGIN, DClass.IN, 3600, Name.fromString("ns2.example.com."));
    RRset newSet = new RRset(ns2);
    zone.addRRset(newSet);
    RRset stored = zone.getNS();
    assertEquals(1, stored.size());
    assertEquals(ns2, stored.first());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNull() {
    zone.addRRset(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNonSubdomain() {
    Name other = Name.fromString("other.com.");
    NSRecord ns = new NSRecord(other, DClass.IN, 3600, Name.fromString("ns.other.com."));
    zone.addRRset(new RRset(ns));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetSOAWithMultipleRecords() throws IOException {
    // Build an RRset that contains two SOA records – illegal.
    SOARecord soa2 =
        new SOARecord(
            ORIGIN,
            DClass.IN,
            3600,
            Name.fromString("ns2.example.com."),
            Name.fromString("hostmaster.example.com."),
            2023100102L,
            7200,
            3600,
            1209600,
            3600);
    RRset badSet = new RRset(soa, soa2);
    zone.addRRset(badSet);
  }

  /* -----------------------------------------------------------------------
   *  removeRRset – normal and error cases
   * ----------------------------------------------------------------------- */

  @Test
  public void testRemoveRRsetExisting() throws IOException {
    // Add an A record set first
    Name aName = Name.fromString("host.example.com.");
    ARecord a = new ARecord(aName, DClass.IN, 300, InetAddress.getByName("192.0.2.20"));
    zone.addRecord(a);
    assertNotNull(zone.findExactMatch(aName, Type.A));

    // Remove the whole A RRset
    zone.removeRRset(aName, Type.A);
    assertNull(zone.findExactMatch(aName, Type.A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetNullName() {
    zone.removeRRset(null, Type.A);
  }

  @Test(expected = InvalidTypeException.class)
  public void testRemoveRRsetInvalidType() {
    // Type 0 is not a valid DNS type
    zone.removeRRset(ORIGIN, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetSOAProhibited() {
    zone.removeRRset(ORIGIN, Type.SOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetAllNSProhibited() {
    zone.removeRRset(ORIGIN, Type.NS);
  }

  /* -----------------------------------------------------------------------
   *  findExactMatch – normal and edge cases
   * ----------------------------------------------------------------------- */

  @Test
  public void testFindExactMatchExists() throws IOException {
    Name aName = Name.fromString("srv.example.com.");
    ARecord a = new ARecord(aName, DClass.IN, 300, InetAddress.getByName("192.0.2.30"));
    zone.addRecord(a);
    RRset rr = zone.findExactMatch(aName, Type.A);
    assertNotNull(rr);
    assertEquals(1, rr.size());
    assertEquals(a, rr.first());
  }

  @Test
  public void testFindExactMatchMissing() {
    Name unknown = Name.fromString("nosuch.example.com.");
    assertNull(zone.findExactMatch(unknown, Type.A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchNullName() {
    zone.findExactMatch(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchTypeANY() {
    zone.findExactMatch(ORIGIN, Type.ANY);
  }

  /* -----------------------------------------------------------------------
   *  findRecords – normal, wildcard, CNAME, delegation, NXDOMAIN
   * ----------------------------------------------------------------------- */

  @Test
  public void testFindRecordsExactA() throws IOException {
    Name aName = Name.fromString("mail.example.com.");
    ARecord a = new ARecord(aName, DClass.IN, 300, InetAddress.getByName("192.0.2.40"));
    zone.addRecord(a);
    SetResponse resp = zone.findRecords(aName, Type.A);
    assertTrue(resp.isSuccessful());
    assertEquals(1, resp.answers().size());
    assertEquals(a, resp.answers().get(0).first());
  }

  @Test
  public void testFindRecordsCNAMEFallback() throws IOException {
    // alias.example.com -> target.example.com
    Name alias = Name.fromString("alias.example.com.");
    Name target = Name.fromString("target.example.com.");
    CNAMERecord cname = new CNAMERecord(alias, DClass.IN, 300, target);
    zone.addRecord(cname);
    SetResponse resp = zone.findRecords(alias, Type.A);
    assertTrue(resp.isCNAME());
    assertEquals(cname, resp.getCNAME());
  }

  @Test
  public void testFindRecordsWildcard() throws IOException {
    // *.example.com. 300 IN A 192.0.2.99
    Name wild = Name.fromString("*.example.com.");
    ARecord wildcardA = new ARecord(wild, DClass.IN, 300, InetAddress.getByName("192.0.2.99"));
    zone.addRecord(wildcardA);
    // Query for foo.example.com – should expand the wildcard
    Name query = Name.fromString("foo.example.com.");
    SetResponse resp = zone.findRecords(query, Type.A);
    assertTrue(resp.isSuccessful());
    assertEquals(1, resp.answers().size());
    Record expanded = resp.answers().get(0).first();
    assertEquals(query, expanded.getName());
    assertEquals(Type.A, expanded.getType());
    assertEquals(wildcardA, expanded.withName(query));
  }

  @Test
  public void testFindRecordsDelegation() throws IOException {
    // sub.example.com. NS ns.sub.example.com.
    Name sub = Name.fromString("sub.example.com.");
    NSRecord subNs = new NSRecord(sub, DClass.IN, 3600, Name.fromString("ns.sub.example.com."));
    zone.addRecord(subNs);
    SetResponse resp = zone.findRecords(sub, Type.A);
    assertTrue(resp.isDelegation());
    assertEquals(subNs, resp.getNS().first());
  }

  @Test
  public void testFindRecordsNxDomain() {
    Name out = Name.fromString("outside.example.org.");
    SetResponse resp = zone.findRecords(out, Type.A);
    assertTrue(resp.isNXDOMAIN());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordsNullName() {
    zone.findRecords(null, Type.A);
  }

  /* -----------------------------------------------------------------------
   *  Iterator and AXFR iterator
   * ----------------------------------------------------------------------- */

  @Test
  public void testIteratorOrder() throws IOException {
    // Add an extra A record to make the iterator return more than the mandatory 2 sets.
    Name aName = Name.fromString("a.example.com.");
    ARecord a = new ARecord(aName, DClass.IN, 300, InetAddress.getByName("192.0.2.55"));
    zone.addRecord(a);

    Iterator<RRset> it = zone.iterator();

    // First set must be the SOA
    assertTrue(it.hasNext());
    RRset first = it.next();
    assertEquals(Type.SOA, first.getType());

    // Second set must be the NS for the zone origin
    assertTrue(it.hasNext());
    RRset second = it.next();
    assertEquals(Type.NS, second.getType());

    // Remaining sets (order not guaranteed) – we just verify that the A record appears.
    boolean foundA = false;
    while (it.hasNext()) {
      RRset set = it.next();
      if (set.getType() == Type.A && set.getName().equals(aName)) {
        foundA = true;
        assertEquals(a, set.first());
      }
    }
    assertTrue("A record set should be present in iterator", foundA);
  }

  @Test
  public void testAXFRIteratorReturnsSOATwice() throws IOException {
    // Add a dummy TXT record to increase count
    Name txtName = Name.fromString("info.example.com.");
    TXTRecord txt = new TXTRecord(txtName, DClass.IN, 300, "test");
    zone.addRecord(txt);

    Iterator<RRset> axfr = zone.AXFR();

    // First sets: SOA, NS, then others
    assertTrue(axfr.hasNext());
    RRset first = axfr.next();
    assertEquals(Type.SOA, first.getType());

    assertTrue(axfr.hasNext());
    RRset second = axfr.next();
    assertEquals(Type.NS, second.getType());

    // Consume remaining sets
    while (axfr.hasNext()) {
      axfr.next();
    }

    // The iterator should now return the SOA again (the "last SOA")
    assertTrue(axfr.hasNext());
    RRset last = axfr.next();
    assertEquals(Type.SOA, last.getType());
    assertEquals(soa, last.first());

    // No more elements after the final SOA
    assertFalse(axfr.hasNext());
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
  public void testIteratorRemove() throws IOException {
    // Add an A record set that we will later remove via the iterator.
    Name aName = Name.fromString("removeme.example.com.");
    ARecord a = new ARecord(aName, DClass.IN, 300, InetAddress.getByName("192.0.2.66"));
    zone.addRecord(a);

    Iterator<RRset> it = zone.iterator();

    // Walk until we find the A set
    while (it.hasNext()) {
      RRset set = it.next();
      if (set.getType() == Type.A && set.getName().equals(aName)) {
        it.remove();
        break;
      }
    }

    // The set should now be gone
    assertNull(zone.findExactMatch(aName, Type.A));
  }

  @Test(expected = IllegalStateException.class)
  public void testIteratorRemoveWithoutNext() {
    Iterator<RRset> it = zone.iterator();
    it.remove(); // illegal – remove called before next()
  }

  /* -----------------------------------------------------------------------
   *  toMasterFile / toString
   * ----------------------------------------------------------------------- */

  @Test
  public void testToMasterFileContainsSOAandNS() {
    String master = zone.toMasterFile();
    assertTrue(master.contains("SOA"));
    assertTrue(master.contains("NS"));
    // The origin should appear as the first name
    assertTrue(master.trim().startsWith(ORIGIN.toString()));
  }

  @Test
  public void testToStringDelegatesToMasterFile() {
    assertEquals(zone.toMasterFile(), zone.toString());
  }
}
