package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Zone}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (signatures listed in the first section).</li>
 *   <li>Typical usage scenarios (creation from records, look‑ups, iteration).</li>
 *   <li>Edge‑case and error handling (null arguments, missing SOA/NS, illegal SOA owner,
 *       NXDOMAIN, NXRRSET, wildcard expansion, delegations).</li>
 * </ul>
 */
public class ZoneTest {

  /* ---------------------------------------------------------------------- */
  /* 1. List of public methods (extracted from the source)                    */
  /* ---------------------------------------------------------------------- */
  // Constructors
  //   Zone(Name zone, String file)                     throws IOException
  //   Zone(Name zone, Record[] records)                throws IOException
  //   Zone(ZoneTransferIn xfrin)                       throws IOException, ZoneTransferException
  //   Zone(Name zone, int dclass, String remote)       throws IOException, ZoneTransferException
  //
  // Accessors
  //   Name getOrigin()
  //   RRset getNS()
  //   SOARecord getSOA()
  //   int getDClass()
  //
  // Lookup helpers
  //   SetResponse findRecords(Name name, int type)
  //   RRset findExactMatch(Name name, int type)
  //
  // Modification helpers
  //   void addRRset(RRset rrset)
  //   <T extends Record> void addRecord(T r)
  //   void removeRecord(Record r)
  //
  // Iterators
  //   Iterator<RRset> iterator()
  //   Iterator<RRset> AXFR()
  //
  // Misc
  //   String toMasterFile()
  //   String toString()

  /* ---------------------------------------------------------------------- */
  /* 2. Test fixtures                                                       */
  /* ---------------------------------------------------------------------- */
  private Name origin;
  private SOARecord soa;
  private NSRecord ns;
  private Zone zone; // zone that contains only the mandatory SOA and NS

  /** Helper that creates a minimal zone (origin, SOA, NS) */
  private Zone createMinimalZone() throws IOException {
    origin = Name.fromString("example.com.");
    // SOA:  ns.example.com. hostmaster.example.com. 2024010101 7200 3600 1209600 3600
    Name mname = Name.fromString("ns.example.com.");
    Name rname = Name.fromString("hostmaster.example.com.");
    soa = new SOARecord(origin, DClass.IN, 3600L, mname, rname,
                         2024010101L, 7200L, 3600L, 1209600L, 3600L);
    // NS: ns.example.com.
    ns = new NSRecord(origin, DClass.IN, 3600L, mname);

    Record[] records = new Record[] { soa, ns };
    return new Zone(origin, records);
  }

  @Before
  public void setUp() throws IOException {
    zone = createMinimalZone();
  }

  /* ---------------------------------------------------------------------- */
  /* 3. Basic functionality tests                                           */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testGetOrigin() {
    assertEquals(origin, zone.getOrigin());
  }

  @Test
  public void testGetSOA() {
    assertEquals(soa, zone.getSOA());
  }

  @Test
  public void testGetNS() {
    assertEquals(ns, zone.getNS());
  }

  @Test
  public void testGetDClass() {
    assertEquals(DClass.IN, zone.getDClass());
  }

  @Test
  public void testFindExactMatch_SOA() {
    RRset result = zone.findExactMatch(origin, Type.SOA);
    assertNotNull(result);
    assertEquals(Type.SOA, result.getType());
    assertEquals(soa, result.first());
  }

  @Test
  public void testFindExactMatch_NS() {
    RRset result = zone.findExactMatch(origin, Type.NS);
    assertNotNull(result);
    assertEquals(Type.NS, result.getType());
    assertEquals(ns, result.first());
  }

  @Test
  public void testFindExactMatch_NonExisting() {
    Name qname = Name.fromString("nosuch.example.com.");
    RRset result = zone.findExactMatch(qname, Type.A);
    assertNull(result);
  }

  @Test
  public void testFindRecords_NXDOMAIN() {
    Name qname = Name.fromString("outside.com.");
    SetResponse sr = zone.findRecords(qname, Type.A);
    assertTrue(sr.isNXDOMAIN());
  }

  @Test
  public void testFindRecords_NXRRSET() {
    // Query for a type that does not exist at the origin (e.g., MX)
    SetResponse sr = zone.findRecords(origin, Type.MX);
    assertTrue(sr.isNXRRSET());
  }

  @Test
  public void testAddAndRemoveRecord() throws IOException {
    // Add an A record for www.example.com.
    Name www = Name.fromString("www.example.com.");
    InetAddress addr = InetAddress.getByName("192.0.2.1");
    ARecord a = new ARecord(www, DClass.IN, 300L, addr);
    zone.addRecord(a);

    RRset fetched = zone.findExactMatch(www, Type.A);
    assertNotNull(fetched);
    assertEquals(1, fetched.size());
    assertEquals(a, fetched.first());

    // Remove the same record and verify it disappears
    zone.removeRecord(a);
    assertNull(zone.findExactMatch(www, Type.A));
  }

  @Test
  public void testIteratorOrder() {
    Iterator<RRset> it = zone.iterator();
    assertTrue(it.hasNext());

    // First element must be the SOA
    RRset first = it.next();
    assertEquals(Type.SOA, first.getType());

    // Second element must be the NS
    assertTrue(it.hasNext());
    RRset second = it.next();
    assertEquals(Type.NS, second.getType());

    // No more elements in this minimal zone
    assertFalse(it.hasNext());
  }

  @Test
  public void testAXFRIteratorIncludesTrailingSOA() {
    Iterator<RRset> axfr = zone.AXFR();
    // The iterator returns SOA first, then NS, then (nothing), then trailing SOA
    assertTrue(axfr.hasNext());
    assertEquals(Type.SOA, axfr.next().getType()); // first SOA
    assertTrue(axfr.hasNext());
    assertEquals(Type.NS, axfr.next().getType());  // NS

    // No other RRsets, so next should be the trailing SOA
    assertTrue(axfr.hasNext());
    RRset trailing = axfr.next();
    assertEquals(Type.SOA, trailing.getType());

    // After trailing SOA iterator must be exhausted
    assertFalse(axfr.hasNext());
  }

  @Test
  public void testToMasterFileContainsSOAAndNS() {
    String master = zone.toMasterFile();
    assertTrue(master.contains(soa.toString()));
    assertTrue(master.contains(ns.toString()));
  }

  @Test
  public void testToStringDelegatesToMasterFile() {
    assertEquals(zone.toMasterFile(), zone.toString());
  }

  /* ---------------------------------------------------------------------- */
  /* 4. Edge‑case / exception tests                                         */
  /* ---------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_NullZoneName() throws IOException {
    new Zone(null, new Record[0]);
  }

  @Test(expected = IOException.class)
  public void testConstructor_MissingSOA() throws IOException {
    // Only NS, no SOA
    Name origin = Name.fromString("example.org.");
    NSRecord ns = new NSRecord(origin, DClass.IN, 3600L,
                               Name.fromString("ns.example.org."));
    new Zone(origin, new Record[] { ns });
  }

  @Test(expected = IOException.class)
  public void testConstructor_MissingNS() throws IOException {
    // Only SOA, no NS
    Name origin = Name.fromString("example.org.");
    Name mname = Name.fromString("ns.example.org.");
    Name rname = Name.fromString("hostmaster.example.org.");
    SOARecord soa = new SOARecord(origin, DClass.IN, 3600L, mname, rname,
                                  2024010101L, 7200L, 3600L, 1209600L, 3600L);
    new Zone(origin, new Record[] { soa });
  }

  @Test(expected = IOException.class)
  public void testMaybeAddRecord_SOAOwnerMismatch() throws IOException {
    // Build a zone with origin "example.com." but try to add an SOA for "other.com."
    Name origin = Name.fromString("example.com.");
    Name other = Name.fromString("other.com.");
    SOARecord badSOA = new SOARecord(other, DClass.IN, 3600L,
                                    Name.fromString("ns.other.com."),
                                    Name.fromString("hostmaster.other.com."),
                                    2024010101L, 7200L, 3600L, 1209600L, 3600L);
    // The constructor will invoke maybeAddRecord which should throw
    new Zone(origin, new Record[] { badSOA });
  }

  @Test
  public void testWildcardExpansion() throws IOException {
    // Add a wildcard A record: *.example.com. 192.0.2.55
    Name wild = Name.fromString("*.example.com.");
    InetAddress addr = InetAddress.getByName("192.0.2.55");
    ARecord wildcardA = new ARecord(wild, DClass.IN, 300L, addr);
    zone.addRecord(wildcardA);

    // Query for www.example.com. (does not exist explicitly)
    Name www = Name.fromString("www.example.com.");
    SetResponse sr = zone.findRecords(www, Type.A);
    assertTrue(sr.isSuccessful());
    List<RRset> answers = sr.answers();
    assertEquals(1, answers.size());
    RRset expanded = answers.get(0);
    assertEquals(Type.A, expanded.getType());
    // The returned record must have the queried name (www.example.com.) not the wildcard name
    Record returned = expanded.first();
    assertEquals(www, returned.getName());
  }

  @Test
  public void testDelegationLookup() throws IOException {
    // Create a delegated sub‑zone "sub.example.com." with its own NS set
    Name sub = Name.fromString("sub.example.com.");
    Name subNs = Name.fromString("ns.sub.example.com.");
    NSRecord subNS = new NSRecord(sub, DClass.IN, 3600L, subNs);
    zone.addRecord(subNS);

    // Lookup for any name under sub.example.com. should return a delegation
    Name q = Name.fromString("host.sub.example.com.");
    SetResponse sr = zone.findRecords(q, Type.A);
    assertTrue(sr.isDelegation());
    assertEquals(subNS, sr.getNS().first());
  }

  @Test
  public void testLookupCNAME() throws IOException {
    // Add a CNAME for alias.example.com. -> www.example.com.
    Name alias = Name.fromString("alias.example.com.");
    Name target = Name.fromString("www.example.com.");
    CNAMERecord cname = new CNAMERecord(alias, DClass.IN, 300L, target);
    zone.addRecord(cname);

    // Query for the alias for any type (e.g., A)
    SetResponse sr = zone.findRecords(alias, Type.A);
    assertTrue(sr.isCNAME());
    assertEquals(cname, sr.getCNAME());
  }

  @Test
  public void testLookupDNAME() throws IOException {
    // Add a DNAME for dsub.example.com. -> other.example.org.
    Name dsub = Name.fromString("dsub.example.com.");
    Name dtarget = Name.fromString("other.example.org.");
    DNAMERecord dname = new DNAMERecord(dsub, DClass.IN, 300L, dtarget);
    zone.addRecord(dname);

    // Query for a name beneath dsub.example.com.
    Name q = Name.fromString("foo.dsub.example.com.");
    SetResponse sr = zone.findRecords(q, Type.A);
    assertTrue(sr.isDNAME());
    assertEquals(dname, sr.getDNAME());
  }

  @Test
  public void testLookupAnyType() throws IOException {
    // Add an A and an MX record for host.example.com.
    Name host = Name.fromString("host.example.com.");
    InetAddress addr = InetAddress.getByName("192.0.2.10");
    ARecord a = new ARecord(host, DClass.IN, 300L, addr);
    MXRecord mx = new MXRecord(host, DClass.IN, 300L, 10,
                               Name.fromString("mail.example.com."));
    zone.addRecord(a);
    zone.addRecord(mx);

    SetResponse sr = zone.findRecords(host, Type.ANY);
    assertTrue(sr.isSuccessful());
    // The response must contain both the A and MX RRsets
    assertEquals(2, sr.answers().size());
    // Verify presence of both types
    boolean hasA = false, hasMX = false;
    for (RRset rs : sr.answers()) {
      if (rs.getType() == Type.A) hasA = true;
      if (rs.getType() == Type.MX) hasMX = true;
    }
    assertTrue(hasA && hasMX);
  }

  /* ---------------------------------------------------------------------- */
  /* 5. Helper methods (used only inside this test class)                    */
  /* ---------------------------------------------------------------------- */

  /** Creates a simple A record for the given name and IPv4 address. */
  private ARecord makeA(Name name, String ip) throws IOException {
    return new ARecord(name, DClass.IN, 300L, InetAddress.getByName(ip));
  }
}
