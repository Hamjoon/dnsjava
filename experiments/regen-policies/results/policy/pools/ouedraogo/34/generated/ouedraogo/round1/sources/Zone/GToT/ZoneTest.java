/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Zone}.
 *
 * The tests are organised as follows:
 *
 * 1.  Extraction of all public methods (including constructors) is documented in the
 *     comments of each test method.
 * 2.  A basic test verifies the normal behaviour of each method.
 * 3.  Additional tests cover edge cases, illegal arguments and the special
 *     exception‑handling paths required by the implementation.
 *
 * The test class uses only the public API that is present in the supplied source
 * code or in the dependency signatures given in the task description.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive test cases for {@link Zone}.
 */
public class ZoneTest {

  /** Common objects used by many tests. */
  private Name zoneName;
  private SOARecord soaRecord;
  private Record nsRecord;
  private Record aRecord;
  private Zone basicZone;

  /**
   * Helper: creates a minimal, valid zone containing an SOA and an NS record.
   *
   * @return a {@link Zone} ready for testing
   * @throws IOException if zone validation fails (should never happen in the helper)
   */
  private Zone createBasicZone() throws IOException {
    // The constructor that accepts an array of records does all validation.
    return new Zone(zoneName, soaRecord, nsRecord);
  }

  /** Initialise objects used by several tests. */
  @Before
  public void setUp() throws Exception {
    zoneName = Name.fromString("example.com.");

    // SOA record – mandatory for a zone.
    soaRecord = new SOARecord(
        zoneName,               // name (must be zone origin)
        DClass.IN,              // class
        3600,                   // ttl
        Name.fromString("ns1.example.com."), // primary NS
        Name.fromString("hostmaster.example.com."), // responsible party
        2023100101L,           // serial
        7200,                  // refresh
        3600,                  // retry
        1209600,               // expire
        3600);                 // minimum

    // NS record – at least one is required.
    nsRecord = Record.newRecord(zoneName, Type.NS, DClass.IN, 3600);

    // An additional A record used by some tests.
    aRecord = Record.newRecord(Name.fromString("www.example.com."), Type.A, DClass.IN, 3600);

    basicZone = createBasicZone();
  }

  /* -----------------------------------------------------------------------
   * Public method list (extracted from the source):
   *
   *   int               getDClass()
   *   RRset             getNS()
   *   Iterator<RRset>   iterator()
   *   Iterator<RRset>   AXFR()
   *   <T extends Record> void addRecord(T r)
   *   void              removeRecord(Record r)
   *   void              addRRset(RRset rrset)
   *   void              removeRRset(Name name, int type)
   *   RRset             findExactMatch(Name name, int type)
   *   SetResponse       findRecords(Name name, int type)
   *   String            toMasterFile()
   *   String            toString()
   *
   * Constructors (public):
   *
   *   Zone(Name zone, String file)                     throws IOException
   *   Zone(Name zone, Record... records)                throws IOException
   *   Zone(ZoneTransferIn xfrin)                       throws IOException,
   *                                                    ZoneTransferException
   *   Zone(Name zone, int dclass, String remote)       throws IOException,
   *                                                    ZoneTransferException,
   *                                                    InvalidDClassException
   * ----------------------------------------------------------------------- */

  /* -------------------------- Basic functionality tests -------------------------- */

  /** Test that the DClass of a zone is always IN (as specified by getDClass()). */
  @Test
  public void testGetDClass() {
    assertEquals(DClass.IN, basicZone.getDClass());
  }

  /** Verify that getNS() returns a copy of the NS RRset containing exactly one NS record. */
  @Test
  public void testGetNS() {
    RRset ns = basicZone.getNS();
    assertNotNull(ns);
    assertEquals(Type.NS, ns.getType());
    assertEquals(1, ns.size());
    // Ensure the returned RRset is a copy (modifying it does not affect the zone)
    ns.addRR(Record.newRecord(zoneName, Type.NS, DClass.IN, 3600));
    assertEquals(1, basicZone.getNS().size());
  }

  /** iterator() must start with the SOA set followed by the NS set. */
  @Test
  public void testIteratorOrder() {
    Iterator<RRset> it = basicZone.iterator();
    assertTrue(it.hasNext());

    RRset first = it.next();
    assertEquals(Type.SOA, first.getType());

    assertTrue(it.hasNext());
    RRset second = it.next();
    assertEquals(Type.NS, second.getType());

    // No further elements in this minimal zone.
    assertFalse(it.hasNext());
  }

  /** AXFR iterator must return the same sequence as iterator() but with the SOA at the end. */
  @Test
  public void testAXFRIterator() {
    Iterator<RRset> it = basicZone.AXFR();
    assertTrue(it.hasNext());
    RRset first = it.next();
    assertEquals(Type.SOA, first.getType()); // first SOA

    assertTrue(it.hasNext());
    RRset second = it.next();
    assertEquals(Type.NS, second.getType());

    // The iterator should now be exhausted except for the final SOA.
    assertTrue(it.hasNext());
    RRset last = it.next();
    assertEquals(Type.SOA, last.getType());

    assertFalse(it.hasNext());
  }

  /** addRecord() should add a new A record to the zone. */
  @Test
  public void testAddRecord() throws IOException {
    Zone z = createBasicZone();
    Name www = Name.fromString("www.example.com.");
    Record a = Record.newRecord(www, Type.A, DClass.IN, 3600);
    z.addRecord(a);

    RRset fetched = z.findExactMatch(www, Type.A);
    assertNotNull(fetched);
    assertEquals(1, fetched.size());
    assertEquals(a, fetched.first());
  }

  /** removeRecord() should delete an existing A record, leaving the zone otherwise unchanged. */
  @Test
  public void testRemoveRecord() throws IOException {
    Zone z = createBasicZone();
    Name www = Name.fromString("www.example.com.");
    Record a = Record.newRecord(www, Type.A, DClass.IN, 3600);
    z.addRecord(a);
    assertNotNull(z.findExactMatch(www, Type.A));

    z.removeRecord(a);
    assertNull(z.findExactMatch(www, Type.A));
  }

  /** addRRset() replaces an existing RRset of the same name/type. */
  @Test
  public void testAddRRsetReplace() throws IOException {
    Zone z = createBasicZone();

    // Create a new NS RRset with a different NS name.
    Record ns2 = Record.newRecord(zoneName, Type.NS, DClass.IN, 3600);
    RRset newNsSet = new RRset(ns2);

    z.addRRset(newNsSet);
    RRset fetched = z.getNS();
    assertEquals(1, fetched.size());
    assertEquals(ns2, fetched.first());
  }

  /** removeRRset() removes a non‑SOA/NS RRset correctly. */
  @Test
  public void testRemoveRRset() throws IOException {
    Zone z = createBasicZone();

    // Add a TXT record set that we will later delete.
    Record txt = Record.newRecord(Name.fromString("txt.example.com."), Type.TXT, DClass.IN, 3600);
    RRset txtSet = new RRset(txt);
    z.addRRset(txtSet);

    // Verify it exists.
    assertNotNull(z.findExactMatch(txt.getName(), Type.TXT));

    // Remove it.
    z.removeRRset(txt.getName(), Type.TXT);
    assertNull(z.findExactMatch(txt.getName(), Type.TXT));
  }

  /** findExactMatch() returns a copy of the underlying RRset. */
  @Test
  public void testFindExactMatchCopy() {
    RRset rrset = basicZone.findExactMatch(zoneName, Type.SOA);
    assertNotNull(rrset);
    assertEquals(soaRecord, rrset.first());

    // Modifying the returned copy must not affect the zone.
    rrset.addRR(Record.newRecord(zoneName, Type.SOA, DClass.IN, 3600));
    RRset again = basicZone.findExactMatch(zoneName, Type.SOA);
    assertEquals(1, again.size());
  }

  /** findRecords() for a name inside the zone returns a successful SetResponse. */
  @Test
  public void testFindRecordsSuccess() throws IOException {
    Zone z = createBasicZone();
    z.addRecord(aRecord);
    SetResponse resp = z.findRecords(Name.fromString("www.example.com."), Type.A);
    assertTrue(resp.isSuccessful());
    List<RRset> answers = resp.answers();
    assertEquals(1, answers.size());
    assertEquals(Type.A, answers.get(0).getType());
  }

  /** findRecords() for a name outside the zone returns NXDOMAIN. */
  @Test
  public void testFindRecordsNxDomain() {
    SetResponse resp = basicZone.findRecords(Name.fromString("outside.com."), Type.A);
    assertTrue(resp.isNXDOMAIN());
  }

  /** toMasterFile() returns a non‑empty string containing the SOA and NS records. */
  @Test
  public void testToMasterFile() {
    String master = basicZone.toMasterFile();
    assertNotNull(master);
    assertTrue(master.contains("SOA"));
    assertTrue(master.contains("NS"));
    assertTrue(master.length() > 0);
  }

  /** toString() delegates to toMasterFile(). */
  @Test
  public void testToString() {
    assertEquals(basicZone.toMasterFile(), basicZone.toString());
  }

  /* -------------------------- Edge case & exception tests -------------------------- */

  /** Constructor with null zone name must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullName() throws IOException {
    new Zone(null, new Record[] { soaRecord, nsRecord });
  }

  /** Constructor with null record array must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullRecords() throws IOException {
    new Zone(zoneName, (Record[]) null);
  }

  /** Constructor must reject a zone that lacks an SOA record. */
  @Test(expected = IOException.class)
  public void testConstructorMissingSOA() throws IOException {
    new Zone(zoneName, nsRecord);
  }

  /** Constructor must reject a zone that lacks an NS record. */
  @Test(expected = IOException.class)
  public void testConstructorMissingNS() throws IOException {
    new Zone(zoneName, soaRecord);
  }

  /** addRecord(null) must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordNull() {
    basicZone.addRecord(null);
  }

  /** addRecord() with a SOA whose owner does not match the zone origin must fail. */
  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordMismatchedSOA() {
    Name other = Name.fromString("other.com.");
    SOARecord otherSOA = new SOARecord(
        other, DClass.IN, 3600,
        Name.fromString("ns1.other.com."),
        Name.fromString("hostmaster.other.com."),
        1, 7200, 3600, 1209600, 3600);
    basicZone.addRecord(otherSOA);
  }

  /** addRecord() with a name that is not a subdomain of the zone must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordOutsideZone() {
    Record r = Record.newRecord(Name.fromString("outside.com."), Type.A, DClass.IN, 3600);
    basicZone.addRecord(r);
  }

  /** removeRecord(null) must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordNull() {
    basicZone.removeRecord(null);
  }

  /** removeRecord() with the SOA record must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordSOA() {
    basicZone.removeRecord(soaRecord);
  }

  /** removeRecord() that would delete the last NS must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testRemoveLastNS() {
    // The zone currently has exactly one NS record.
    basicZone.removeRecord(nsRecord);
  }

  /** addRRset(null) must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNull() {
    basicZone.addRRset(null);
  }

  /** addRRset() with an SOA RRset of size != 1 must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetInvalidSOASize() {
    // Create an SOA RRset containing two identical SOA records.
    RRset badSOASet = new RRset(soaRecord);
    badSOASet.addRR(soaRecord);
    basicZone.addRRset(badSOASet);
  }

  /** addRRset() with a name that is not a subdomain of the zone must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetOutsideZone() {
    Record outside = Record.newRecord(Name.fromString("outside.com."), Type.A, DClass.IN, 3600);
    RRset set = new RRset(outside);
    basicZone.addRRset(set);
  }

  /** removeRRset(null, type) must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetNullName() throws IOException {
    basicZone.removeRRset(null, Type.A);
  }

  /** removeRRset() with an invalid type (e.g., 0) must throw InvalidTypeException (wrapped as IllegalArgumentException). */
  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetInvalidType() throws IOException {
    basicZone.removeRRset(zoneName, 0);
  }

  /** findExactMatch(null, type) must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchNullName() {
    basicZone.findExactMatch(null, Type.A);
  }

  /** findExactMatch() with an invalid type must throw InvalidTypeException (wrapped as IllegalArgumentException). */
  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatchInvalidType() {
    basicZone.findExactMatch(zoneName, 0);
  }

  /** findRecords(null, type) must throw IllegalArgumentException. */
  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordsNullName() {
    basicZone.findRecords(null, Type.A);
  }

  /** findRecords() with an invalid type must throw InvalidTypeException (wrapped as IllegalArgumentException). */
  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordsInvalidType() {
    basicZone.findRecords(zoneName, 0);
  }

  /** iterator().remove() before any next() call must throw IllegalStateException. */
  @Test(expected = IllegalStateException.class)
  public void testIteratorRemoveWithoutNext() {
    Iterator<RRset> it = basicZone.iterator();
    it.remove();
  }

  /** iterator().remove() on the SOA set must not throw (SOA removal is allowed via iterator). */
  @Test
  public void testIteratorRemoveSOA() {
    Iterator<RRset> it = basicZone.iterator();
    assertTrue(it.hasNext());
    RRset first = it.next(); // SOA
    // Removing the SOA via iterator is allowed; the zone will no longer be valid but the method
    // itself should not throw.
    it.remove();
    // After removal, the zone's internal SOA reference is cleared; further operations may fail,
    // but this test only checks that no exception is thrown here.
  }

  /* -------------------------- File‑based constructor test -------------------------- */

  /** Zone(Name, String) reads a master file; we create a temporary file with minimal content. */
  @Test
  public void testFileConstructor() throws Exception {
    // Build a tiny master file containing an SOA and an NS record.
    File tmp = File.createTempFile("zone", ".txt");
    tmp.deleteOnExit();
    try (FileWriter fw = new FileWriter(tmp)) {
      fw.write("$ORIGIN example.com.\n");
      fw.write("@ 3600 IN SOA ns1.example.com. hostmaster.example.com. 2023100101 7200 3600 1209600 3600\n");
      fw.write("@ 3600 IN NS ns1.example.com.\n");
    }

    Zone z = new Zone(Name.fromString("example.com."), tmp.getAbsolutePath());
    assertNotNull(z.findExactMatch(zoneName, Type.SOA));
    assertNotNull(z.findExactMatch(zoneName, Type.NS));
  }

}
