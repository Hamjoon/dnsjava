package org.xbill.DNS;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class ZoneTest {

  private Name zoneName;
  private SOARecord soa;
  private NSRecord ns1;
  private NSRecord ns2;
  private ARecord a1;
  private ARecord a2;
  private CNAMERecord cname;
  private DNAMERecord dname;
  private Name wildcardName;
  private ARecord wildA;

  @Before
  public void setUp() throws Exception {
    zoneName = new Name("example.com.");
    // SOA
    Name host = new Name("ns.example.com.");
    Name admin = new Name("admin.example.com.");
    soa = new SOARecord(zoneName, DClass.IN, 3600, host, admin, 2023101501L, 7200, 3600, 1209600, 3600);
    // NS records
    ns1 = new NSRecord(zoneName, DClass.IN, 3600, new Name("ns1.example.com."));
    ns2 = new NSRecord(zoneName, DClass.IN, 3600, new Name("ns2.example.com."));
    // A records
    a1 = new ARecord(new Name("www.example.com."), DClass.IN, 300, InetAddress.getByName("192.0.2.1"));
    a2 = new ARecord(new Name("mail.example.com."), DClass.IN, 300, InetAddress.getByName("192.0.2.2"));
    // CNAME
    cname = new CNAMERecord(new Name("ftp.example.com."), DClass.IN, 300, new Name("www.example.com."));
    // DNAME
    dname = new DNAMERecord(new Name("sub.example.com."), DClass.IN, 300, new Name("other.example.com."));
    // Wildcard
    wildcardName = new Name("*.example.com.");
    wildA = new ARecord(wildcardName, DClass.IN, 300, InetAddress.getByName("192.0.2.99"));
  }

  /** Helper to build a Zone with the minimal required records plus any extras. */
  private Zone buildZone(Record... extra) throws IOException {
    Record[] all = new Record[extra.length + 2];
    all[0] = soa;
    all[1] = ns1;
    System.arraycopy(extra, 0, all, 2, extra.length);
    return new Zone(zoneName, all);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullName() throws IOException {
    new Zone(null, new ByteArrayInputStream(new byte[0]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullInputStream() throws IOException {
    new Zone(zoneName, (InputStream) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullFile() throws IOException {
    new Zone(zoneName, (String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullRecordsArray() throws IOException {
    new Zone(zoneName, (Record[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorMissingSOA() throws IOException {
    new Zone(zoneName, ns1);
  }

  @Test
  public void testBasicAccessors() throws IOException {
    Zone z = buildZone();
    assertEquals(zoneName, z.getOrigin());
    assertEquals(DClass.IN, z.getDClass());
    assertEquals(soa, z.getSOA());
    RRset nsSet = z.getNS();
    assertNotNull(nsSet);
    assertEquals(1, nsSet.size());
    assertEquals(ns1, nsSet.first());
  }

  @Test
  public void testAddAndFindRecord() throws Exception {
    Zone z = buildZone(a1);
    // Verify that a1 is present
    RRset found = z.findExactMatch(a1.getName(), Type.A);
    assertNotNull(found);
    assertEquals(1, found.size());
    assertEquals(a1, found.first());

    // Add a second A record for same name
    z.addRecord(a2); // a2 has a different name, so just added
    RRset found2 = z.findExactMatch(a2.getName(), Type.A);
    assertNotNull(found2);
    assertEquals(a2, found2.first());

    // Add a CNAME and ensure findRecords returns CNAME response
    z.addRecord(cname);
    SetResponse resp = z.findRecords(cname.getName(), Type.A);
    assertTrue(resp.isCNAME());
    assertEquals(cname, resp.getCNAME());
  }

  @Test
  public void testRemoveRecordAndExceptions() throws Exception {
    Zone z = buildZone(a1, ns1);
    // Remove a1
    z.removeRecord(a1);
    assertNull(z.findExactMatch(a1.getName(), Type.A));

    // Trying to remove the only NS should throw
    try {
      z.removeRecord(ns1);
      fail("Expected IllegalArgumentException when removing last NS");
    } catch (IllegalArgumentException expected) {
      // expected
    }

    // Add a second NS and then remove one – should succeed
    z.addRecord(ns2);
    z.removeRecord(ns1);
    RRset nsSet = z.getNS();
    assertEquals(1, nsSet.size());
    assertEquals(ns2, nsSet.first());

    // Trying to remove SOA throws
    try {
      z.removeRecord(soa);
      fail("Expected IllegalArgumentException when removing SOA");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test
  public void testAddAndRemoveRRset() throws Exception {
    Zone z = buildZone();

    // Create an RRset with two A records
    RRset aSet = new RRset();
    aSet.addRR(a1);
    aSet.addRR(a2);
    z.addRRset(aSet);

    // Verify both records are present
    RRset got = z.findExactMatch(a1.getName(), Type.A);
    assertNotNull(got);
    assertEquals(1, got.size()); // a1 is alone in its own name set
    RRset got2 = z.findExactMatch(a2.getName(), Type.A);
    assertNotNull(got2);
    assertEquals(1, got2.size());

    // Remove the entire RRset for a1
    z.removeRRset(a1.getName(), Type.A);
    assertNull(z.findExactMatch(a1.getName(), Type.A));

    // Removing NS RRset (last NS) should throw
    try {
      z.removeRRset(zoneName, Type.NS);
      fail("Expected IllegalArgumentException when removing all NS");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test
  public void testFindRecordsVariousResponses() throws Exception {
    Zone z = buildZone(a1, cname, dname, wildA);

    // Exact match for A
    SetResponse r1 = z.findRecords(a1.getName(), Type.A);
    assertTrue(r1.isSuccessful());
    assertEquals(1, r1.answers().size());
    assertEquals(a1, r1.answers().get(0).first());

    // CNAME lookup for name with CNAME
    SetResponse r2 = z.findRecords(cname.getName(), Type.A);
    assertTrue(r2.isCNAME());
    assertEquals(cname, r2.getCNAME());

    // DNAME lookup for a name under DNAME
    SetResponse r3 = z.findRecords(new Name("foo.sub.example.com."), Type.A);
    assertTrue(r3.isDNAME());
    assertEquals(dname, r3.getDNAME());

    // Wildcard expansion
    Name wildTarget = new Name("host.example.com.");
    SetResponse r4 = z.findRecords(wildTarget, Type.A);
    assertTrue(r4.isSuccessful());
    assertEquals(1, r4.answers().size());
    RRset expanded = r4.answers().get(0);
    // The expanded set should contain the same address as the wildcard record but with the queried name
    Record expandedRec = expanded.first();
    assertEquals(wildTarget, expandedRec.getName());
    assertEquals(wildA.getAddress(), ((ARecord) expandedRec).getAddress());
  }

  @Test
  public void testIteratorOrderingAndAXFR() throws Exception {
    Zone z = buildZone(a1, a2, ns2);
    Iterator<RRset> it = z.iterator();

    // First should be SOA
    assertTrue(it.hasNext());
    RRset first = it.next();
    assertEquals(Type.SOA, first.getType());

    // Second should be NS (including both NS records)
    assertTrue(it.hasNext());
    RRset second = it.next();
    assertEquals(Type.NS, second.getType());
    assertEquals(2, second.size()); // ns1 + ns2

    // Subsequent entries should contain the A records (order not guaranteed)
    boolean a1Seen = false, a2Seen = false;
    while (it.hasNext()) {
      RRset cur = it.next();
      if (cur.getType() == Type.A) {
        Record r = cur.first();
        if (r.equals(a1)) a1Seen = true;
        if (r.equals(a2)) a2Seen = true;
      }
    }
    assertTrue(a1Seen && a2Seen);

    // AXFR iterator should return the SOA again at the end
    Iterator<RRset> axfr = z.AXFR();
    int count = 0;
    RRset last = null;
    while (axfr.hasNext()) {
      last = axfr.next();
      count++;
    }
    assertNotNull(last);
    assertEquals(Type.SOA, last.getType());
    // Count should be original count + 1 (extra SOA at end)
    assertEquals( (2 /*SOA+NS*/ + 2 /*two A records*/) + 1, count);
  }

  @Test
  public void testIteratorRemove() throws Exception {
    Zone z = buildZone(a1, a2);
    Iterator<RRset> it = z.iterator();

    // Skip SOA and NS
    it.next(); // SOA
    it.next(); // NS

    // Remove first A RRset via iterator
    RRset aSet = it.next();
    assertEquals(Type.A, aSet.getType());
    it.remove();

    // The removed name should no longer be present
    assertNull(z.findExactMatch(aSet.getName(), Type.A));
  }

  @Test
  public void testToMasterFileContainsAllRecords() throws Exception {
    Zone z = buildZone(a1, a2, ns2, cname, wildA);
    String master = z.toMasterFile();

    // Verify that each record's string representation appears in the master file output
    assertTrue(master.contains(soa.toString()));
    assertTrue(master.contains(ns1.toString()));
    assertTrue(master.contains(ns2.toString()));
    assertTrue(master.contains(a1.toString()));
    assertTrue(master.contains(a2.toString()));
    assertTrue(master.contains(cname.toString()));
    assertTrue(master.contains(wildA.toString()));
  }

  @Test
  public void testZoneTransferConstructorInvalid() {
    try {
      new Zone((ZoneTransferIn) null);
      fail("Expected IllegalArgumentException for null ZoneTransferIn");
    } catch (IllegalArgumentException | IOException | ZoneTransferException e) {
      // expected
    }
  }

  @Test
  public void testRemoveRRsetInvalidType() throws Exception {
    Zone z = buildZone();
    try {
      z.removeRRset(zoneName, Type.SOA);
      fail("Expected IllegalArgumentException when removing SOA via removeRRset");
    } catch (IllegalArgumentException expected) {
      // expected
    }
    try {
      z.removeRRset(zoneName, Type.NS);
      fail("Expected IllegalArgumentException when removing all NS via removeRRset");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }
}
