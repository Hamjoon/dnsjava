package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link Zone}.  The tests only use the public API that is
 * listed in the problem statement (no additional DNS record classes are referenced directly;
 * generic {@link Record} objects are created via {@code Record.newRecord(...)}).
 */
public class ZoneTest {

  private Name origin;
  private SOARecord soa1;
  private Record nsRec;
  private Record aRec;
  private Record cnameRec;
  private Record wildARec;
  private Zone zone;

  @Before
  public void setUp() throws Exception {
    /* -------------------------------------------------------------
     * Build a minimal zone:
     *   origin               = example.com.
     *   SOA                  = example.com. (serial 1)
     *   NS                   = example.com.
     *   A    www.example.com. = 127.0.0.1
     *   CNAME mail.example.com. -> www.example.com.
     *   wildcard *.example.com. A = 1.2.3.4
     * ------------------------------------------------------------- */
    origin = new Name("example.com.");

    // SOA (serial = 1)
    soa1 = new SOARecord(
        origin,
        DClass.IN,
        3600,
        new Name("ns1.example.com."),
        new Name("hostmaster.example.com."),
        1L,          // serial
        7200, 3600, 1209600, 3600);

    // NS record (generic Record, empty RDATA – sufficient for the tests)
    nsRec = Record.newRecord(origin, Type.NS, DClass.IN, 3600, new byte[]{0});

    // A record for www.example.com.
    aRec = Record.newRecord(
        new Name("www.example.com."),
        Type.A,
        DClass.IN,
        3600,
        new byte[]{127, 0, 0, 1});

    // CNAME record for mail.example.com. → www.example.com.
    cnameRec = Record.newRecord(
        new Name("mail.example.com."),
        Type.CNAME,
        DClass.IN,
        3600,
        new byte[]{0}); // content not interpreted by the tests

    // Wildcard A record *.example.com.
    wildARec = Record.newRecord(
        new Name("*.example.com."),
        Type.A,
        DClass.IN,
        3600,
        new byte[]{1, 2, 3, 4});

    // Create the zone using the var‑args constructor.
    zone = new Zone(origin, soa1, nsRec, aRec, cnameRec, wildARec);
  }

  /* -------------------------------------------------------------
   *  Basic getters
   * ------------------------------------------------------------- */
  @Test
  public void testGetters() {
    assertEquals(origin, zone.getNS().getName());
    assertEquals(soa1, zone.getSOA());
    assertEquals(DClass.IN, zone.getDClass());
  }

  /* -------------------------------------------------------------
   *  findExactMatch – positive and negative cases
   * ------------------------------------------------------------- */
  @Test
  public void testFindExactMatch() {
    RRset wwwA = zone.findExactMatch(new Name("www.example.com."), Type.A);
    assertNotNull(wwwA);
    assertEquals(Type.A, wwwA.getType());
    assertEquals(1, wwwA.size());

    // No exact match for a non‑existent name
    RRset notFound = zone.findExactMatch(new Name("nosuch.example.com."), Type.A);
    assertNull(notFound);
  }

  /* -------------------------------------------------------------
   *  findRecords – normal name, CNAME, wildcard, and out‑of‑zone
   * ------------------------------------------------------------- */
  @Test
  public void testFindRecords() {
    // Direct A lookup – successful
    SetResponse respA = zone.findRecords(new Name("www.example.com."), Type.A);
    assertTrue(respA.isSuccessful());
    assertEquals(1, respA.answers().size());
    assertEquals(Type.A, respA.answers().get(0).getType());

    // CNAME lookup – should return a CNAME SetResponse
    SetResponse respCNAME = zone.findRecords(new Name("mail.example.com."), Type.A);
    assertTrue(respCNAME.isCNAME());
    assertEquals(Type.CNAME, respCNAME.getCNAME().getType());

    // Wildcard expansion – request A for a name that only matches the wildcard
    SetResponse respWildcard = zone.findRecords(new Name("test.example.com."), Type.A);
    assertTrue(respWildcard.isSuccessful());
    assertEquals(1, respWildcard.answers().size());
    RRset expanded = respWildcard.answers().get(0);
    assertEquals(Type.A, expanded.getType());
    // the name of the returned RRset must be the queried name (expanded)
    assertEquals(new Name("test.example.com."), expanded.getName());

    // Name outside the zone – NXDOMAIN
    SetResponse respOut = zone.findRecords(new Name("example.org."), Type.A);
    assertTrue(respOut.isNXDOMAIN());
  }

  /* -------------------------------------------------------------
   *  addRecord – replace SOA and reject illegal arguments
   * ------------------------------------------------------------- */
  @Test
  public void testAddRecordReplaceSOA() throws IOException {
    // New SOA with a different serial
    SOARecord soa2 = new SOARecord(
        origin,
        DClass.IN,
        3600,
        new Name("ns2.example.com."),
        new Name("hostmaster.example.com."),
        2L,          // new serial
        7200, 3600, 1209600, 3600);

    zone.addRecord(soa2);
    assertEquals(soa2, zone.getSOA());

    // Adding a null record must throw IllegalArgumentException
    try {
      zone.addRecord(null);
      fail("Expected IllegalArgumentException for null record");
    } catch (IllegalArgumentException expected) {
    }

    // Adding a SOA with a different name must be rejected
    Record otherSOA = Record.newRecord(
        new Name("other.example.com."),
        Type.SOA,
        DClass.IN,
        3600,
        new byte[]{0});
    try {
      zone.addRecord(otherSOA);
      fail("Expected IllegalArgumentException for SOA with wrong owner");
    } catch (IllegalArgumentException expected) {
    }
  }

  /* -------------------------------------------------------------
   *  removeRecord – normal removal and protected records
   * ------------------------------------------------------------- */
  @Test
  public void testRemoveRecord() {
    // Remove the A record for www.example.com.
    zone.removeRecord(aRec);
    assertNull(zone.findExactMatch(new Name("www.example.com."), Type.A));

    // Attempt to remove the SOA – must fail
    try {
      zone.removeRecord(soa1);
      fail("Expected IllegalArgumentException when removing SOA");
    } catch (IllegalArgumentException expected) {
    }

    // Attempt to remove the only NS record – must fail
    try {
      zone.removeRecord(nsRec);
      fail("Expected IllegalArgumentException when removing last NS");
    } catch (IllegalArgumentException expected) {
    }
  }

  /* -------------------------------------------------------------
   *  addRRset / removeRRset – DNAME handling
   * ------------------------------------------------------------- */
  @Test
  public void testAddAndRemoveRRset() throws IOException {
    // Build a DNAME RRset for alias.example.com.
    Record dnameRec = Record.newRecord(
        new Name("alias.example.com."),
        Type.DNAME,
        DClass.IN,
        3600,
        new byte[]{0});
    RRset dnameSet = new RRset(dnameRec);

    // Add the RRset
    zone.addRRset(dnameSet);

    // Lookup a name beneath the DNAME – should return a DNAME SetResponse
    SetResponse resp = zone.findRecords(new Name("foo.alias.example.com."), Type.A);
    assertTrue(resp.isDNAME());
    assertEquals(dnameSet.getName(), resp.getDNAME().getName());

    // Remove the DNAME set
    zone.removeRRset(dnameSet.getName(), Type.DNAME);
    SetResponse afterRemoval = zone.findRecords(new Name("foo.alias.example.com."), Type.A);
    assertTrue(afterRemoval.isNXDOMAIN());
  }

  /* -------------------------------------------------------------
   *  Iterator order – first SOA, then NS, then other RRsets
   * ------------------------------------------------------------- */
  @Test
  public void testIteratorOrder() {
    Iterator<RRset> it = zone.iterator();
    assertTrue(it.hasNext());

    RRset first = it.next();               // SOA
    assertEquals(Type.SOA, first.getType());

    RRset second = it.next();              // NS
    assertEquals(Type.NS, second.getType());

    // The remaining RRsets can appear in any order but must be present
    int count = 2;
    while (it.hasNext()) {
      RRset r = it.next();
      assertNotNull(r);
      count++;
    }
    assertEquals(5, count); // SOA, NS, A, CNAME, wildcard A
  }

  /* -------------------------------------------------------------
   *  AXFR iterator – last element must be the SOA again
   * ------------------------------------------------------------- */
  @Test
  public void testAXFRIterator() {
    Iterator<RRset> axfr = zone.AXFR();
    RRset last = null;
    while (axfr.hasNext()) {
      last = axfr.next();
    }
    assertNotNull(last);
    assertEquals(Type.SOA, last.getType());
    // The SOA returned by AXFR must be identical to the zone's SOA
    assertEquals(zone.getSOA(), last.first());
  }

  /* -------------------------------------------------------------
   *  toMasterFile – basic sanity check of output content
   * ------------------------------------------------------------- */
  @Test
  public void testToMasterFile() {
    String master = zone.toMasterFile();
    // The master file must contain the SOA and NS owners.
    assertTrue(master.contains(origin.toString()));
    assertTrue(master.contains("ns1.example.com."));
    // It must also contain the wildcard entry.
    assertTrue(master.contains("*.example.com."));
  }

  /* -------------------------------------------------------------
   *  Constructor validation – null arguments
   * ------------------------------------------------------------- */
  @Test
  public void testConstructorValidation() {
    try {
      new Zone(null, soa1);
      fail("Expected IllegalArgumentException for null origin");
    } catch (IllegalArgumentException expected) {
    }

    try {
      new Zone(origin, (Record[]) null);
      fail("Expected IllegalArgumentException for null records array");
    } catch (IllegalArgumentException expected) {
    }

    // Missing SOA or NS should cause IOException – construct minimal bad zone
    try {
      new Zone(origin, nsRec); // no SOA
      fail("Expected IOException for zone without SOA");
    } catch (IOException expected) {
    }

    try {
      new Zone(origin, soa1); // no NS
      fail("Expected IOException for zone without NS");
    } catch (IOException expected) {
    }
  }

  /* -------------------------------------------------------------
   *  Zone created from a Master (using an InputStream)
   * ------------------------------------------------------------- */
  @Test
  public void testZoneFromMasterStream() throws Exception {
    // Build a tiny master file in memory
    String masterData =
        "$ORIGIN example.com.\n" +
        "@ 3600 IN SOA ns1.example.com. hostmaster.example.com. 1 7200 3600 1209600 3600\n" +
        "@ 3600 IN NS ns1.example.com.\n" +
        "www 3600 IN A 127.0.0.1\n";

    ByteArrayInputStream in = new ByteArrayInputStream(masterData.getBytes("UTF-8"));
    Zone fromMaster = new Zone(origin, in);

    // Verify that the zone contains the records we wrote
    assertNotNull(fromMaster.findExactMatch(new Name("www.example.com."), Type.A));
    assertEquals(soa1.getSerial(), fromMaster.getSOA().getSerial());
    assertEquals(1, fromMaster.getNS().size());
  }
}
