###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Zone}.
 *
 * <p>All tests use only the public API of the DNS library as required. No mocking frameworks are
 * employed; real {@link Name}, {@link Record}, {@link RRset}, {@link SOARecord}, etc. objects are
 * constructed.
 */
public class ZoneTest {

  private Name origin;
  private SOARecord soa;
  private Record nsRecord;

  /**
   * Creates a minimal valid zone (origin, SOA, one NS) that can be reused by many tests.
   */
  @Before
  public void setUp() throws Exception {
    origin = Name.fromString("example.com.");
    // SOA: mname = ns.example.com., rname = hostmaster.example.com.
    Name mname = Name.fromString("ns.example.com.");
    Name rname = Name.fromString("hostmaster.example.com.");
    soa = new SOARecord(
        origin,
        DClass.IN,
        3600L,
        mname,
        rname,
        2023100101L,
        7200L,
        3600L,
        1209600L,
        3600L);
    // NS record for the zone
    nsRecord = Record.newRecord(origin, Type.NS, DClass.IN, 3600L);
  }

  /** Helper to build a Zone with the minimal required records. */
  private Zone createMinimalZone() throws IOException {
    return new Zone(origin, soa, nsRecord);
  }

  @Test
  public void testConstructorValid() throws Exception {
    Zone zone = createMinimalZone();
    assertEquals(origin, zone.getOrigin());
    assertNotNull(zone.getSOA());
    assertEquals(soa, zone.getSOA());
    assertNotNull(zone.getNS());
    assertEquals(1, zone.getNS().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullOrigin() throws Exception {
    new Zone(null, soa, nsRecord);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullRecordsArray() throws Exception {
    new Zone(origin, (Record[]) null);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingSOA() throws Exception {
    new Zone(origin, nsRecord);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingNS() throws Exception {
    new Zone(origin, soa);
  }

  @Test
  public void testGetDClass() throws Exception {
    Zone zone = createMinimalZone();
    assertEquals(DClass.IN, zone.getDClass());
  }

  @Test
  public void testAddRecordCreatesNewRRset() throws Exception {
    Zone zone = createMinimalZone();
    Name www = Name.fromString("www.example.com.");
    Record aRecord = Record.newRecord(www, Type.A, DClass.IN, 300L);
    zone.addRecord(aRecord);

    RRset rs = zone.findExactMatch(www, Type.A);
    assertNotNull(rs);
    assertEquals(1, rs.size());
    assertEquals(aRecord, rs.first());
  }

  @Test
  public void testAddRecordReplaceSOA() throws Exception {
    Zone zone = createMinimalZone();
    // New SOA with a different serial number
    SOARecord newSoa =
        new SOARecord(
            origin,
            DClass.IN,
            3600L,
            Name.fromString("ns2.example.com."),
            Name.fromString("hostmaster.example.com."),
            2023100201L,
            7200L,
            3600L,
            1209600L,
            3600L);
    zone.addRecord(newSoa);
    assertEquals(newSoa, zone.getSOA());
  }

  @Test
  public void testRemoveRecordRemovesExisting() throws Exception {
    Zone zone = createMinimalZone();
    Name mail = Name.fromString("mail.example.com.");
    Record aRecord = Record.newRecord(mail, Type.A, DClass.IN, 300L);
    zone.addRecord(aRecord);
    assertNotNull(zone.findExactMatch(mail, Type.A));

    zone.removeRecord(aRecord);
    assertNull(zone.findExactMatch(mail, Type.A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordRefuseSOA() throws Exception {
    Zone zone = createMinimalZone();
    zone.removeRecord(soa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordRefuseLastNS() throws Exception {
    Zone zone = createMinimalZone();
    // The zone only has a single NS record; removal must be rejected.
    zone.removeRecord(nsRecord);
  }

  @Test
  public void testAddRRsetReplaceExisting() throws Exception {
    Zone zone = createMinimalZone();
    Name host = Name.fromString("host.example.com.");
    Record a1 = Record.newRecord(host, Type.A, DClass.IN, 300L);
    Record a2 = Record.newRecord(host, Type.A, DClass.IN, 300L);
    RRset firstSet = new RRset(a1);
    zone.addRRset(firstSet);

    // Verify first addition
    RRset fetched = zone.findExactMatch(host, Type.A);
    assertEquals(1, fetched.size());

    // Replace with a new RRset containing two A records
    RRset replacement = new RRset(a1, a2);
    zone.addRRset(replacement);
    RRset afterReplace = zone.findExactMatch(host, Type.A);
    assertEquals(2, afterReplace.size());
  }

  @Test
  public void testRemoveRRsetDeletesOnlySpecifiedType() throws Exception {
    Zone zone = createMinimalZone();
    Name host = Name.fromString("host.example.com.");
    Record a = Record.newRecord(host, Type.A, DClass.IN, 300L);
    Record txt = Record.newRecord(host, Type.TXT, DClass.IN, 300L);
    zone.addRecord(a);
    zone.addRecord(txt);

    // Remove the A RRset
    zone.removeRRset(host, Type.A);
    assertNull(zone.findExactMatch(host, Type.A));
    // TXT should still be present
    assertNotNull(zone.findExactMatch(host, Type.TXT));
  }

  @Test
  public void testFindExactMatch() throws Exception {
    Zone zone = createMinimalZone();
    Name www = Name.fromString("www.example.com.");
    Record a = Record.newRecord(www, Type.A, DClass.IN, 300L);
    zone.addRecord(a);

    RRset rs = zone.findExactMatch(www, Type.A);
    assertNotNull(rs);
    assertEquals(1, rs.size());
    assertEquals(a, rs.first());
  }

  @Test
  public void testFindRecordsSuccessful() throws Exception {
    Zone zone = createMinimalZone();
    Name www = Name.fromString("www.example.com.");
    Record a = Record.newRecord(www, Type.A, DClass.IN, 300L);
    zone.addRecord(a);

    SetResponse resp = zone.findRecords(www, Type.A);
    assertTrue(resp.isSuccessful());
    List<RRset> answers = resp.answers();
    assertEquals(1, answers.size());
    assertEquals(a, answers.get(0).first());
  }

  @Test
  public void testFindRecordsCNAME() throws Exception {
    Zone zone = createMinimalZone();
    Name alias = Name.fromString("alias.example.com.");
    Name target = Name.fromString("target.example.com.");
    Record cname = Record.newRecord(alias, Type.CNAME, DClass.IN, 300L);
    // The generic Record created by newRecord does not automatically set the CNAME target,
    // but for the purpose of this test we only need the type to be CNAME.
    zone.addRecord(cname);

    SetResponse resp = zone.findRecords(alias, Type.A);
    assertTrue(resp.isCNAME());
    assertEquals(cname, resp.getCNAME());
  }

  @Test
  public void testFindRecordsDelegation() throws Exception {
    Zone zone = createMinimalZone();
    // child.example.com. is a delegated zone (has its own NS)
    Name child = Name.fromString("child.example.com.");
    Record childNS = Record.newRecord(child, Type.NS, DClass.IN, 300L);
    zone.addRecord(childNS);

    SetResponse resp = zone.findRecords(child, Type.A);
    assertTrue(resp.isDelegation());
    assertEquals(childNS, resp.getNS().first());
  }

  @Test
  public void testFindRecordsWildcard() throws Exception {
    Zone zone = createMinimalZone();
    // Add a wildcard A record: *.example.com. A 1.2.3.4
    Name wildcard = Name.fromString("*.example.com.");
    Record aWildcard = Record.newRecord(wildcard, Type.A, DClass.IN, 300L);
    zone.addRecord(aWildcard);

    Name qname = Name.fromString("foo.example.com.");
    SetResponse resp = zone.findRecords(qname, Type.A);
    assertTrue(resp.isSuccessful());
    RRset rrset = resp.answers().get(0);
    // The expanded record should have the queried name, not the wildcard name.
    Record expanded = rrset.first();
    assertEquals(qname, expanded.getName());
  }

  @Test
  public void testIteratorOrderAndContents() throws Exception {
    Zone zone = createMinimalZone();
    Name www = Name.fromString("www.example.com.");
    Record a = Record.newRecord(www, Type.A, DClass.IN, 300L);
    zone.addRecord(a);
    Name mail = Name.fromString("mail.example.com.");
    Record mx = Record.newRecord(mail, Type.MX, DClass.IN, 300L);
    zone.addRecord(mx);

    Iterator<RRset> it = zone.iterator();
    // First should be SOA
    assertTrue(it.hasNext());
    RRset first = it.next();
    assertEquals(Type.SOA, first.getType());

    // Second should be NS
    assertTrue(it.hasNext());
    RRset second = it.next();
    assertEquals(Type.NS, second.getType());

    // Remaining sets (order not guaranteed) should contain A and MX
    boolean foundA = false;
    boolean foundMX = false;
    while (it.hasNext()) {
      RRset rs = it.next();
      if (rs.getType() == Type.A) {
        foundA = true;
        assertEquals(a, rs.first());
      } else if (rs.getType() == Type.MX) {
        foundMX = true;
        assertEquals(mx, rs.first());
      }
    }
    assertTrue(foundA);
    assertTrue(foundMX);
  }

  @Test
  public void testAXFRIteratorEndsWithSOA() throws Exception {
    Zone zone = createMinimalZone();
    Name www = Name.fromString("www.example.com.");
    Record a = Record.newRecord(www, Type.A, DClass.IN, 300L);
    zone.addRecord(a);

    Iterator<RRset> it = zone.AXFR();
    RRset last = null;
    while (it.hasNext()) {
      last = it.next();
    }
    // The last RRset returned by AXFR must be the SOA.
    assertNotNull(last);
    assertEquals(Type.SOA, last.getType());
  }

  @Test
  public void testToMasterFileContainsAllRecords() throws Exception {
    Zone zone = createMinimalZone();
    Name www = Name.fromString("www.example.com.");
    Record a = Record.newRecord(www, Type.A, DClass.IN, 300L);
    zone.addRecord(a);

    String master = zone.toMasterFile();
    // The master file representation should contain the textual form of the A record
    assertTrue(master.contains(a.toString()));
    // It should also contain the SOA and NS records
    assertTrue(master.contains(soa.toString()));
    assertTrue(master.contains(nsRecord.toString()));
  }

  @Test
  public void testZoneIteratorRemove() throws Exception {
    Zone zone = createMinimalZone();
    Name host = Name.fromString("host.example.com.");
    Record a = Record.newRecord(host, Type.A, DClass.IN, 300L);
    zone.addRecord(a);

    Iterator<RRset> it = zone.iterator();
    // Skip SOA and NS
    it.next(); // SOA
    it.next(); // NS
    // Next should be the A record we added
    assertTrue(it.hasNext());
    RRset rs = it.next();
    assertEquals(Type.A, rs.getType());
    // Remove via iterator
    it.remove();

    // After removal the record should no longer be present
    assertNull(zone.findExactMatch(host, Type.A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordRejectsNonSubdomain() throws Exception {
    Zone zone = createMinimalZone();
    Name external = Name.fromString("other.com.");
    Record a = Record.newRecord(external, Type.A, DClass.IN, 300L);
    zone.addRecord(a);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetRejectsInvalidSOAName() throws Exception {
    Zone zone = createMinimalZone();
    Name other = Name.fromString("other.com.");
    SOARecord badSoa =
        new SOARecord(
            other,
            DClass.IN,
            3600L,
            Name.fromString("ns.other.com."),
            Name.fromString("hostmaster.other.com."),
            2023100101L,
            7200L,
            3600L,
            1209600L,
            3600L);
    RRset rrset = new RRset(badSoa);
    zone.addRRset(rrset);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetRejectsSOA() throws Exception {
    Zone zone = createMinimalZone();
    zone.removeRRset(origin, Type.SOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetRejectsNS() throws Exception {
    Zone zone = createMinimalZone();
    zone.removeRRset(origin, Type.NS);
  }
}
###Test END##