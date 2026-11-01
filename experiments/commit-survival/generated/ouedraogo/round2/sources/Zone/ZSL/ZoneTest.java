package org.xbill.DNS;

import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Comprehensive JUnit4 tests for {@link Zone}.
 */
public class ZoneTest {

  /** Helper to create a generic Record of a given type. */
  private Record makeRecord(Name name, int type) throws IOException {
    // Use IN class (value 1) and a TTL of 3600 seconds.
    return Record.newRecord(name, type, 1, 3600);
  }

  /** Builds a minimal zone containing the mandatory SOA and NS records. */
  private Zone createBasicZone() throws IOException {
    Name origin = Name.fromString("example.com.");
    // SOA record (type 6)
    Name mname = Name.fromString("ns1.example.com.");
    Name rname = Name.fromString("hostmaster.example.com.");
    SOARecord soa = new SOARecord(origin, 1, 3600, mname, rname,
        2023101501L, 7200, 3600, 1209600, 3600);
    // NS record (type 2)
    Record ns = makeRecord(Name.fromString("example.com."), 2);
    // Build array of records
    Record[] records = new Record[] { soa, ns };
    return new Zone(origin, records);
  }

  @Test
  public void testZoneConstructionFromRecords() throws IOException {
    Zone zone = createBasicZone();
    Assert.assertEquals(Name.fromString("example.com."), zone.getOrigin());
    Assert.assertNotNull(zone.getSOA());
    Assert.assertNotNull(zone.getNS());

    // Exact match for the SOA should be found
    RRset soaSet = zone.findExactMatch(Name.fromString("example.com."), 6);
    Assert.assertNotNull(soaSet);
    Assert.assertEquals(6, soaSet.getType());

    // Exact match for the NS should be found
    RRset nsSet = zone.findExactMatch(Name.fromString("example.com."), 2);
    Assert.assertNotNull(nsSet);
    Assert.assertEquals(2, nsSet.getType());
  }

  @Test
  public void testAddAndRemoveRecord() throws IOException {
    Zone zone = createBasicZone();

    // Add an A record for www.example.com.
    Name www = Name.fromString("www.example.com.");
    Record aRecord = makeRecord(www, 1); // A record (type 1)
    zone.addRecord(aRecord);

    // Verify it can be retrieved exactly.
    RRset aSet = zone.findExactMatch(www, 1);
    Assert.assertNotNull(aSet);
    Assert.assertEquals(1, aSet.getType());
    Assert.assertEquals(1, aSet.size());

    // Remove the record and verify it disappears.
    zone.removeRecord(aRecord);
    Assert.assertNull(zone.findExactMatch(www, 1));
  }

  @Test
  public void testCNAMELookup() throws IOException {
    Zone zone = createBasicZone();

    // Add a CNAME for alias.example.com. -> www.example.com.
    Name alias = Name.fromString("alias.example.com.");
    Record cname = makeRecord(alias, 5); // CNAME type = 5
    zone.addRecord(cname);

    // Lookup should return a CNAME SetResponse.
    SetResponse resp = zone.findRecords(alias, 1); // request A, but CNAME present
    Assert.assertTrue(resp.isCNAME());
    Assert.assertEquals(cname, resp.getCNAME());
  }

  @Test
  public void testDNAMELookup() throws IOException {
    Zone zone = createBasicZone();

    // Add a DNAME for dname.example.com.
    Name dnameNode = Name.fromString("dname.example.com.");
    Record dname = makeRecord(dnameNode, 39); // DNAME type = 39
    zone.addRecord(dname);

    // Lookup for a deeper name should return a DNAME response.
    Name target = Name.fromString("foo.dname.example.com.");
    SetResponse resp = zone.findRecords(target, 1);
    Assert.assertTrue(resp.isDNAME());
    Assert.assertEquals(dname, resp.getDNAME());
  }

  @Test
  public void testWildcardExpansion() throws IOException {
    Zone zone = createBasicZone();

    // Add a wildcard A record *.example.com.
    Name wild = Name.fromString("*.example.com.");
    Record wildA = makeRecord(wild, 1);
    zone.addRecord(wildA);

    // Query a name that does not exist explicitly.
    Name qname = Name.fromString("foo.example.com.");
    SetResponse resp = zone.findRecords(qname, 1);
    Assert.assertTrue(resp.isSuccessful());
    List<RRset> answers = resp.answers();
    Assert.assertEquals(1, answers.size());
    RRset expanded = answers.get(0);
    // The returned record should have its name changed to the queried name.
    Assert.assertEquals(qname, expanded.getName());
  }

  @Test
  public void testAnyLookup() throws IOException {
    Zone zone = createBasicZone();

    // Add an A and an MX record for host.example.com.
    Name host = Name.fromString("host.example.com.");
    Record a = makeRecord(host, 1);
    Record mx = makeRecord(host, 15); // MX type = 15
    zone.addRecord(a);
    zone.addRecord(mx);

    // ANY query should return both RRsets.
    SetResponse resp = zone.findRecords(host, 255); // ANY = 255
    Assert.assertTrue(resp.isSuccessful());
    List<RRset> answers = resp.answers();
    // Expect three RRsets: SOA, NS (origin) and the two added (A, MX) = 4 total.
    // However, the ANY lookup for an exact name returns *all* RRsets at that name,
    // not the origin's SOA/NS. So we expect A and MX only.
    Assert.assertEquals(2, answers.size());
    boolean hasA = false, hasMX = false;
    for (RRset rs : answers) {
      if (rs.getType() == 1) hasA = true;
      if (rs.getType() == 15) hasMX = true;
    }
    Assert.assertTrue(hasA && hasMX);
  }

  @Test
  public void testDelegationDetection() throws IOException {
    Zone zone = createBasicZone();

    // Add NS record for child.example.com. (delegation point)
    Name child = Name.fromString("child.example.com.");
    Record childNs = makeRecord(child, 2);
    zone.addRecord(childNs);

    // Query a name under the delegated child.
    Name q = Name.fromString("foo.child.example.com.");
    SetResponse resp = zone.findRecords(q, 1);
    Assert.assertTrue(resp.isDelegation());
    // The NS set returned should be the one we added.
    Assert.assertEquals(childNs, resp.getNS().first());
  }

  @Test
  public void testIteratorOrderAndAXFR() throws IOException {
    Zone zone = createBasicZone();

    // Add extra records for testing order.
    Name www = Name.fromString("www.example.com.");
    Record a = makeRecord(www, 1);
    zone.addRecord(a);

    // Normal iterator: first two should be SOA then NS.
    Iterator<RRset> it = zone.iterator();
    Assert.assertTrue(it.hasNext());
    RRset first = it.next();
    Assert.assertEquals(6, first.getType()); // SOA
    Assert.assertTrue(it.hasNext());
    RRset second = it.next();
    Assert.assertEquals(2, second.getType()); // NS
    // Subsequent sets include the A record we added.
    boolean foundA = false;
    while (it.hasNext()) {
      RRset rs = it.next();
      if (rs.getType() == 1) {
        foundA = true;
        break;
      }
    }
    Assert.assertTrue(foundA);

    // AXFR iterator should return an extra SOA at the end.
    Iterator<RRset> axfr = zone.AXFR();
    RRset last = null;
    while (axfr.hasNext()) {
      last = axfr.next();
    }
    Assert.assertNotNull(last);
    Assert.assertEquals(6, last.getType()); // final SOA
  }

  @Test
  public void testToMasterFileContainsRecords() throws IOException {
    Zone zone = createBasicZone();

    // Add a couple of records.
    Name www = Name.fromString("www.example.com.");
    Record a = makeRecord(www, 1);
    zone.addRecord(a);
    Record mx = makeRecord(Name.fromString("example.com."), 15);
    zone.addRecord(mx);

    String master = zone.toMasterFile();
    // The string should contain the textual representation of our records.
    Assert.assertTrue(master.contains("www.example.com."));
    Assert.assertTrue(master.contains("example.com."));
    // Ensure the SOA and NS are present.
    Assert.assertTrue(master.contains("SOA"));
    Assert.assertTrue(master.contains("NS"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRejectsNullOrigin() throws IOException {
    new Zone(null, new Record[0]);
  }

  @Test(expected = IOException.class)
  public void testValidateFailsWithoutSOA() throws IOException {
    // Build a zone with only NS, no SOA.
    Name origin = Name.fromString("nosoa.com.");
    Record ns = makeRecord(origin, 2);
    new Zone(origin, new Record[] { ns });
  }

  @Test(expected = IOException.class)
  public void testValidateFailsWithoutNS() throws IOException {
    // Build a zone with only SOA, no NS.
    Name origin = Name.fromString("nons.com.");
    Name mname = Name.fromString("ns1.nons.com.");
    Name rname = Name.fromString("hostmaster.nons.com.");
    SOARecord soa = new SOARecord(origin, 1, 3600, mname, rname,
        2023101501L, 7200, 3600, 1209600, 3600);
    new Zone(origin, new Record[] { soa });
  }

  @Test
  public void testLookupOutsideZoneReturnsNXDOMAIN() throws IOException {
    Zone zone = createBasicZone();
    Name external = Name.fromString("outside.org.");
    SetResponse resp = zone.findRecords(external, 1);
    Assert.assertTrue(resp.isNXDOMAIN());
  }

  @Test
  public void testLookupMissingTypeReturnsNXRRSET() throws IOException {
    Zone zone = createBasicZone();
    // Query for an A record at the zone apex where only SOA and NS exist.
    SetResponse resp = zone.findRecords(Name.fromString("example.com."), 1);
    Assert.assertTrue(resp.isNXRRSET());
  }
}
