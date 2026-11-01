###Test START##
```java
/*
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.Zone.
 *
 * The tests cover:
 *   – all public constructors (null arguments, successful creation)
 *   – record manipulation (addRecord, removeRecord, addRRset, removeRRset)
 *   – lookup methods (findExactMatch, findRecords)
 *   – iterator behaviour (iterator() and AXFR())
 *   – serialization of the zone (toMasterFile)
 *
 * Only the APIs that are visible in the supplied source and the listed
 * dependency signatures are used – no additional mocking or subclassing.
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 tests for {@link Zone}.
 */
public class ZoneTest {

  /* -----------------------------------------------------------------------
   *  Test data shared by many test methods
   * ----------------------------------------------------------------------- */

  private Name origin;          // example.com.
  private SOARecord soa;        // SOA for the zone
  private Record ns1;          // NS record for the zone (ns1.example.com.)
  private Record ns2;          // Second NS record (ns2.example.com.)

  /**
   * Creates the basic objects (origin, SOA, NS) that are required for a valid
   * zone. All tests start from a clean zone built from these records.
   */
  @Before
  public void setUp() throws Exception {
    origin = Name.fromString("example.com.");
    Name mname = Name.fromString("ns1.example.com.");
    Name rname = Name.fromString("hostmaster.example.com.");

    // ttl = 3600, serial = 2023100101, refresh = 7200, retry = 1800,
    // expire = 1209600, minimum = 3600
    soa = new SOARecord(
        origin,
        DClass.IN,
        3600,
        mname,
        rname,
        2023100101L,
        7200,
        1800,
        1209600,
        3600);

    // two NS records – they will be added as separate Record instances
    ns1 = Record.fromString(
        origin,
        Type.NS,
        DClass.IN,
        3600,
        "ns1.example.com.",
        origin);
    ns2 = Record.fromString(
        origin,
        Type.NS,
        DClass.IN,
        3600,
        "ns2.example.com.",
        origin);
  }

  /* -----------------------------------------------------------------------
   *  Constructor tests
   * ----------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_NullZone() throws IOException {
    new Zone(null, new ByteArrayInputStream(new byte[0]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_NullInputStream() throws IOException {
    new Zone(origin, (InputStream) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_NullFileName() throws IOException {
    new Zone(origin, (String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_NullRecordArray() throws IOException {
    new Zone(origin, (Record[]) null);
  }

  @Test(expected = IOException.class)
  public void testConstructor_MissingSOA() throws IOException {
    // only NS records – should fail validation
    new Zone(origin, ns1, ns2);
  }

  @Test(expected = IOException.class)
  public void testConstructor_MissingNS() throws IOException {
    // only SOA – should fail validation
    new Zone(origin, soa);
  }

  @Test
  public void testConstructor_FromRecords_Success() throws IOException {
    Zone zone = new Zone(origin, soa, ns1, ns2);
    assertNotNull("SOA must be present", zone.getSOA());
    assertEquals("SOA instance must be the one supplied", soa, zone.getSOA());

    RRset nsSet = zone.getNS();
    assertNotNull("NS set must be present", nsSet);
    assertEquals("Two NS records expected", 2, nsSet.size());
  }

  /* -----------------------------------------------------------------------
   *  addRecord / removeRecord tests
   * ----------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecord_Null() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    zone.addRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecord_NameOutsideZone() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name external = Name.fromString("outside.com.");
    Record a = Record.fromString(
        external,
        Type.A,
        DClass.IN,
        300,
        "192.0.2.123",
        origin);
    zone.addRecord(a);
  }

  @Test
  public void testAddRecord_ARecord() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name host = Name.fromString("www.example.com.");
    Record a = Record.fromString(host, Type.A, DClass.IN, 300, "192.0.2.55", origin);
    zone.addRecord(a);

    RRset result = zone.findExactMatch(host, Type.A);
    assertNotNull("A record should be found after addition", result);
    assertEquals(1, result.size());
    assertEquals(a, result.first());
  }

  @Test
  public void testAddRecord_ReplaceSOA() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    // new SOA with a different serial
    SOARecord newSoa = new SOARecord(
        origin,
        DClass.IN,
        3600,
        Name.fromString("ns2.example.com."),
        Name.fromString("hostmaster.example.com."),
        2023100201L,
        7200,
        1800,
        1209600,
        3600);
    zone.addRecord(newSoa);
    assertEquals("SOA should be replaced by the new one", newSoa, zone.getSOA());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecord_Null() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    zone.removeRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecord_RemoveSOA() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    zone.removeRecord(soa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecord_RemoveLastNS() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    zone.removeRecord(ns1); // this would leave the zone without any NS
  }

  @Test
  public void testRemoveRecord_ARecord() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name host = Name.fromString("mail.example.com.");
    Record a = Record.fromString(host, Type.A, DClass.IN, 300, "192.0.2.77", origin);
    zone.addRecord(a);
    assertNotNull(zone.findExactMatch(host, Type.A));

    zone.removeRecord(a);
    assertNull("A record should be gone after removal", zone.findExactMatch(host, Type.A));
  }

  /* -----------------------------------------------------------------------
   *  addRRset / removeRRset tests
   * ----------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRset_Null() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    zone.addRRset(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRset_NameOutsideZone() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name external = Name.fromString("outside.com.");
    Record a = Record.fromString(external, Type.A, DClass.IN, 300, "10.0.0.1", origin);
    RRset rrset = new RRset(a);
    zone.addRRset(rrset);
  }

  @Test
  public void testAddRRset_NewASet() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name host = Name.fromString("srv.example.com.");
    Record a1 = Record.fromString(host, Type.A, DClass.IN, 300, "192.0.2.10", origin);
    Record a2 = Record.fromString(host, Type.A, DClass.IN, 300, "192.0.2.11", origin);
    RRset aSet = new RRset(a1);
    aSet.addRR(a2);
    zone.addRRset(aSet);

    RRset found = zone.findExactMatch(host, Type.A);
    assertNotNull(found);
    assertEquals(2, found.size());
  }

  @Test
  public void testAddRRset_ReplaceNSSet() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    // create a completely new NS RRset (different target)
    Record newNs = Record.fromString(
        origin,
        Type.NS,
        DClass.IN,
        3600,
        "ns-new.example.com.",
        origin);
    RRset newNsSet = new RRset(newNs);
    zone.addRRset(newNsSet);

    RRset nsSet = zone.getNS();
    assertEquals(1, nsSet.size());
    assertEquals(newNs, nsSet.first());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRset_NullName() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    zone.removeRRset(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRset_RemoveSOA() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    zone.removeRRset(origin, Type.SOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRset_RemoveAllNS() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    zone.removeRRset(origin, Type.NS);
  }

  @Test
  public void testRemoveRRset_ASet() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name host = Name.fromString("app.example.com.");
    Record a = Record.fromString(host, Type.A, DClass.IN, 300, "192.0.2.33", origin);
    zone.addRecord(a);
    assertNotNull(zone.findExactMatch(host, Type.A));

    zone.removeRRset(host, Type.A);
    assertNull(zone.findExactMatch(host, Type.A));
  }

  /* -----------------------------------------------------------------------
   *  Lookup tests (findExactMatch / findRecords)
   * ----------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatch_NullName() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    zone.findExactMatch(null, Type.A);
  }

  @Test
  public void testFindExactMatch_Found() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name host = Name.fromString("db.example.com.");
    Record a = Record.fromString(host, Type.A, DClass.IN, 300, "192.0.2.88", origin);
    zone.addRecord(a);

    RRset set = zone.findExactMatch(host, Type.A);
    assertNotNull(set);
    assertEquals(1, set.size());
    assertEquals(a, set.first());
  }

  @Test
  public void testFindExactMatch_NotFound() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name unknown = Name.fromString("unknown.example.com.");
    assertNull(zone.findExactMatch(unknown, Type.A));
  }

  @Test
  public void testFindRecords_ExactSuccess() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name host = Name.fromString("mail.example.com.");
    Record a = Record.fromString(host, Type.A, DClass.IN, 300, "192.0.2.44", origin);
    zone.addRecord(a);

    SetResponse resp = zone.findRecords(host, Type.A);
    assertTrue("Response should be successful", resp.isSuccessful());
    assertEquals(1, resp.answers().size());
    assertEquals(a, resp.answers().get(0).first());
  }

  @Test
  public void testFindRecords_CNAMEFallback() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name alias = Name.fromString("alias.example.com.");
    Name target = Name.fromString("real.example.com.");
    Record cname = Record.fromString(alias, Type.CNAME, DClass.IN, 300, "real.example.com.", origin);
    Record a = Record.fromString(target, Type.A, DClass.IN, 300, "192.0.2.99", origin);
    zone.addRecord(cname);
    zone.addRecord(a);

    SetResponse resp = zone.findRecords(alias, Type.A);
    assertTrue("CNAME response expected", resp.isCNAME());
    assertEquals(cname, resp.getCNAME());
  }

  @Test
  public void testFindRecords_NXDOMAIN() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name out = Name.fromString("out.example.org."); // not a sub‑domain of origin
    SetResponse resp = zone.findRecords(out, Type.A);
    assertTrue("NXDOMAIN expected for out‑of‑zone name", resp.isNXDOMAIN());
  }

  @Test
  public void testFindRecords_WildcardExpansion() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    // create a wildcard NS: *.example.com. NS ns-wild.example.com.
    Name wild = Name.fromString("*.example.com.");
    Record wildNs = Record.fromString(
        wild,
        Type.NS,
        DClass.IN,
        3600,
        "ns-wild.example.com.",
        origin);
    zone.addRecord(wildNs);

    // query a name that matches the wildcard
    Name qname = Name.fromString("foo.example.com.");
    SetResponse resp = zone.findRecords(qname, Type.NS);
    assertTrue("Wildcard expansion should be successful", resp.isSuccessful());
    assertEquals(1, resp.answers().size());
    RRset expanded = resp.answers().get(0);
    // the returned RRset must contain the wildcard record with the queried name
    Record returned = expanded.first();
    assertEquals(qname, returned.getName());
    assertEquals("ns-wild.example.com.", returned.rdataToString().trim());
  }

  /* -----------------------------------------------------------------------
   *  Iterator tests (iterator() and AXFR())
   * ----------------------------------------------------------------------- */

  @Test
  public void testIterator_OrderAndContent() throws IOException {
    Zone zone = new Zone(origin, soa, ns1, ns2);
    // add an extra A record to verify ordering after SOA/NS
    Name host = Name.fromString("host.example.com.");
    Record a = Record.fromString(host, Type.A, DClass.IN, 300, "192.0.2.22", origin);
    zone.addRecord(a);

    Iterator<RRset> it = zone.iterator();
    assertTrue(it.hasNext());

    // 1st RRset must be SOA
    RRset first = it.next();
    assertEquals(Type.SOA, first.getType());
    assertEquals(soa, first.first());

    // 2nd RRset must be NS (the zone's NS set)
    assertTrue(it.hasNext());
    RRset second = it.next();
    assertEquals(Type.NS, second.getType());
    // the NS set contains both ns1 and ns2
    assertEquals(2, second.size());

    // 3rd RRset should be the A record we added
    assertTrue(it.hasNext());
    RRset third = it.next();
    assertEquals(Type.A, third.getType());
    assertEquals(a, third.first());

    // No more elements
    assertFalse(it.hasNext());
  }

  @Test
  public void testAXFR_EndsWithSOA() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    // add a dummy A record
    Name host = Name.fromString("a.example.com.");
    Record a = Record.fromString(host, Type.A, DClass.IN, 300, "10.0.0.5", origin);
    zone.addRecord(a);

    Iterator<RRset> axfr = zone.AXFR();

    // Consume all elements except the final SOA
    RRset last = null;
    while (axfr.hasNext()) {
      last = axfr.next();
    }
    // The last element must be the SOA (again)
    assertNotNull(last);
    assertEquals(Type.SOA, last.getType());
    assertEquals(soa, last.first());
  }

  @Test
  public void testIterator_RemoveCurrentRRset() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    Name host = Name.fromString("remove.example.com.");
    Record a = Record.fromString(host, Type.A, DClass.IN, 300, "10.0.0.9", origin);
    zone.addRecord(a);

    Iterator<RRset> it = zone.iterator();

    // advance to the A record (third element)
    it.next(); // SOA
    it.next(); // NS
    RRset aSet = it.next(); // A
    assertEquals(Type.A, aSet.getType());

    // remove via iterator
    it.remove();

    // After removal the A record should no longer be found
    assertNull(zone.findExactMatch(host, Type.A));
  }

  /* -----------------------------------------------------------------------
   *  Serialization test (toMasterFile)
   * ----------------------------------------------------------------------- */

  @Test
  public void testToMasterFile_ContainsSOAAndNS() throws IOException {
    Zone zone = new Zone(origin, soa, ns1, ns2);
    String master = zone.toMasterFile();

    // The master file representation must contain the SOA serial and the NS hostnames
    assertTrue("SOA serial must appear in master file", master.contains(Long.toString(soa.getSerial())));
    assertTrue("ns1 must appear in master file", master.contains("ns1.example.com."));
    assertTrue("ns2 must appear in master file", master.contains("ns2.example.com."));
  }

  @Test
  public void testToString_DelegatesToMasterFile() throws IOException {
    Zone zone = new Zone(origin, soa, ns1);
    assertEquals(zone.toMasterFile(), zone.toString());
  }
}
```
###Test END##