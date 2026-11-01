###Test START##
```java
package org.xbill.DNS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Zone}.  The tests cover:
 * <ul>
 *   <li>Public API signatures</li>
 *   <li>Normal functional behaviour</li>
 *   <li>Edge cases and exception handling</li>
 *   <li>Iterators (normal and AXFR)</li>
 * </ul>
 *
 * The test data builds a minimal, valid zone consisting of:
 * <pre>
 *   example.com.      IN  SOA  ns1.example.com. hostmaster.example.com. (1 3600 600 1209600 300)
 *   example.com.      IN  NS   ns1.example.com.
 * </pre>
 * Additional records (A, CNAME, NS for a delegated sub‑zone) are added in individual tests.
 */
public class ZoneTest {

  private Name origin;
  private SOARecord soa;
  private Record nsRecord;
  private Zone zone;

  /** Helper to create a minimal valid zone used by most tests. */
  @Before
  public void setUp() throws IOException {
    origin = Name.fromString("example.com.");

    // SOA record (required)
    Name host = Name.fromString("ns1.example.com.");
    Name admin = Name.fromString("hostmaster.example.com.");
    soa = new SOARecord(
        origin,
        DClass.IN,
        3600,
        host,
        admin,
        1,          // serial
        3600,       // refresh
        600,        // retry
        1209600,    // expire
        300);       // minimum

    // One NS record (required)
    nsRecord = Record.newRecord(origin, Type.NS, DClass.IN, 3600);

    // Build the zone via the (Name, Record...) constructor
    zone = new Zone(origin, soa, nsRecord);
  }

  // -------------------------------------------------------------------------
  // 1.  Public method signatures (extracted for reference)
  // -------------------------------------------------------------------------
  //   public int getDClass()
  //   public RRset getNS()
  //   public Iterator<RRset> iterator()
  //   public Iterator<RRset> AXFR()
  //   public <T extends Record> void addRecord(T r)
  //   public void removeRecord(Record r)
  //   public void addRRset(RRset rrset)
  //   public void removeRRset(Name name, int type)
  //   public RRset findExactMatch(Name name, int type)
  //   public SetResponse findRecords(Name name, int type)
  //   public String toMasterFile()
  //   public String toString()

  // -------------------------------------------------------------------------
  // 2.  Basic functional tests
  // -------------------------------------------------------------------------

  @Test
  public void testGetters() {
    assertEquals(origin, zone.getOrigin());
    assertEquals(soa, zone.getSOA());
    assertNotNull(zone.getNS());
    assertEquals(DClass.IN, zone.getDClass());
  }

  @Test
  public void testAddAndFindExactMatch() throws IOException {
    // Add an A record for www.example.com.
    Name www = Name.fromString("www.example.com.");
    Record a = Record.newRecord(www, Type.A, DClass.IN, 3600);
    zone.addRecord(a);

    RRset rrset = zone.findExactMatch(www, Type.A);
    assertNotNull(rrset);
    assertEquals(1, rrset.size());
    assertEquals(a, rrset.first());
  }

  @Test
  public void testRemoveRecord() throws IOException {
    // Add then remove an A record.
    Name host = Name.fromString("host.example.com.");
    Record a = Record.newRecord(host, Type.A, DClass.IN, 3600);
    zone.addRecord(a);
    assertNotNull(zone.findExactMatch(host, Type.A));

    zone.removeRecord(a);
    assertNull(zone.findExactMatch(host, Type.A));
  }

  @Test
  public void testAddRRsetAndFind() throws IOException {
    Name mail = Name.fromString("mail.example.com.");
    Record a1 = Record.newRecord(mail, Type.A, DClass.IN, 3600);
    Record a2 = Record.newRecord(mail, Type.A, DClass.IN, 3600);
    RRset rrset = new RRset(a1, a2);

    zone.addRRset(rrset);

    RRset found = zone.findExactMatch(mail, Type.A);
    assertNotNull(found);
    assertEquals(2, found.size());
    assertTrue(found.rrs().contains(a1));
    assertTrue(found.rrs().contains(a2));
  }

  @Test
  public void testRemoveRRset() throws IOException {
    Name srv = Name.fromString("srv.example.com.");
    Record a = Record.newRecord(srv, Type.A, DClass.IN, 3600);
    RRset rrset = new RRset(a);
    zone.addRRset(rrset);

    // Ensure it exists
    assertNotNull(zone.findExactMatch(srv, Type.A));

    // Remove it
    zone.removeRRset(srv, Type.A);
    assertNull(zone.findExactMatch(srv, Type.A));
  }

  @Test
  public void testFindRecordsNXDOMAIN() {
    Name unknown = Name.fromString("unknown.example.com.");
    SetResponse resp = zone.findRecords(unknown, Type.A);
    assertTrue(resp.isNXDOMAIN());
  }

  @Test
  public void testFindRecordsDelegation() throws IOException {
    // Create a delegated sub‑zone: sub.example.com. NS ns2.example.com.
    Name sub = Name.fromString("sub.example.com.");
    Record nsSub = Record.newRecord(sub, Type.NS, DClass.IN, 3600);
    zone.addRecord(nsSub);

    SetResponse resp = zone.findRecords(sub, Type.A);
    assertTrue(resp.isDelegation());
    assertNotNull(resp.getNS());
    assertEquals(Type.NS, resp.getNS().getType());
  }

  @Test
  public void testIteratorOrderAndAXFR() throws IOException {
    // Add an extra A record so we have more than SOA+NS.
    Name extra = Name.fromString("extra.example.com.");
    Record a = Record.newRecord(extra, Type.A, DClass.IN, 3600);
    zone.addRecord(a);

    // ----- normal iterator -----
    Iterator<RRset> it = zone.iterator();
    List<RRset> collected = new ArrayList<>();
    while (it.hasNext()) {
      collected.add(it.next());
    }
    // The first two RRsets must be SOA then NS (origin order)
    assertEquals(Type.SOA, collected.get(0).getType());
    assertEquals(Type.NS, collected.get(1).getType());

    // ----- AXFR iterator -----
    Iterator<RRset> axfr = zone.AXFR();
    List<RRset> axfrCollected = new ArrayList<>();
    while (axfr.hasNext()) {
      axfrCollected.add(axfr.next());
    }
    // The last element must be the SOA again
    assertEquals(Type.SOA, axfrCollected.get(axfrCollected.size() - 1).getType());
  }

  @Test
  public void testToMasterFileContainsSOAAndNS() {
    String master = zone.toMasterFile();
    assertTrue(master.contains("SOA"));
    assertTrue(master.contains("NS"));
    assertTrue(master.contains(origin.toString()));
  }

  // -------------------------------------------------------------------------
  // 3.  Edge‑case and exception handling tests
  // -------------------------------------------------------------------------

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullZone() throws IOException {
    new Zone(null, "dummyfile");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullFile() throws IOException {
    new Zone(origin, (String) null);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingSOA() throws IOException {
    // Only NS, no SOA → should fail validation
    Record nsOnly = Record.newRecord(origin, Type.NS, DClass.IN, 3600);
    new Zone(origin, nsOnly);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordNull() {
    zone.addRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordSOAWithWrongOwner() throws IOException {
    Name other = Name.fromString("other.com.");
    SOARecord badSoa = new SOARecord(
        other,
        DClass.IN,
        3600,
        Name.fromString("ns1.other.com."),
        Name.fromString("hostmaster.other.com."),
        1, 3600, 600, 1209600, 300);
    zone.addRecord(badSoa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordOutsideZone() throws IOException {
    Name external = Name.fromString("outside.com.");
    Record a = Record.newRecord(external, Type.A, DClass.IN, 3600);
    zone.addRecord(a);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordNull() {
    zone.removeRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveSOARecord() {
    zone.removeRecord(soa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveLastNSRecord() {
    // The zone currently contains exactly one NS record.
    zone.removeRecord(nsRecord);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNull() {
    zone.addRRset(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetSOAWithWrongName() throws IOException {
    Name wrong = Name.fromString("wrong.example.com.");
    SOARecord badSoa = new SOARecord(
        wrong,
        DClass.IN,
        3600,
        Name.fromString("ns1.example.com."),
        Name.fromString("hostmaster.example.com."),
        2, 3600, 600, 1209600, 300);
    RRset rrset = new RRset(badSoa);
    zone.addRRset(rrset);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetNullName() {
    zone.removeRRset(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetSOA() {
    zone.removeRRset(origin, Type.SOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetAllNS() {
    zone.removeRRset(origin, Type.NS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchNullName() {
    zone.findExactMatch(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchInvalidType() {
    // Type 0 is not a valid DNS type; Type.check will throw InvalidTypeException,
    // which is a subclass of IllegalArgumentException in this library.
    zone.findExactMatch(origin, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordsNullName() {
    zone.findRecords(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordsInvalidType() {
    zone.findRecords(origin, -1);
  }

  @Test
  public void testIteratorRemoveCurrentRRset() throws IOException {
    // Add a second A record so we have something removable.
    Name host = Name.fromString("removable.example.com.");
    Record a = Record.newRecord(host, Type.A, DClass.IN, 3600);
    zone.addRecord(a);

    Iterator<RRset> it = zone.iterator();
    // Skip SOA and NS
    it.next(); // SOA
    it.next(); // NS
    RRset aSet = it.next(); // the A RRset we just added
    assertEquals(Type.A, aSet.getType());

    // Remove via iterator
    it.remove();

    // The record should no longer be present.
    assertNull(zone.findExactMatch(host, Type.A));
  }

}
```
###Test END##