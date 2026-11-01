package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Zone}.
 *
 * <p>The tests exercise:
 *
 * <ul>
 *   <li>All public constructors (including error paths)
 *   <li>Simple getters (getDClass, getNS, getSOA)
 *   <li>Record manipulation (addRecord, removeRecord, addRRset, removeRRset)
 *   <li>Lookup APIs (findExactMatch, findRecords) – covering exact, wildcard, CNAME,
 *       delegation and NX* cases
 *   <li>Iterators (iterator() and AXFR())
 *   <li>Serialisation to master‑file format (toMasterFile / toString)
 * </ul>
 *
 * <p>Only classes, constructors and methods that appear in the supplied source or in the listed
 * dependency signatures are used.  DNS type and class constants are supplied as raw integers
 * (the library defines the same values) to avoid depending on the {@code Type} and {@code DClass}
 * helper classes which are not part of the allowed API.
 */
public class ZoneTest {

  /* DNS numeric constants (as defined in the library) */
  private static final int TYPE_SOA = 6;
  private static final int TYPE_NS = 2;
  private static final int TYPE_A = 1;
  private static final int TYPE_CNAME = 5;
  private static final int TYPE_DNAME = 39;
  private static final int TYPE_ANY = 255;

  private static final int CLASS_IN = 1;

  private Name origin;
  private SOARecord soa;
  private Record nsRecord;
  private Record aRecord;
  private Record cnameRecord;
  private Record wildARecord;
  private Record delegatedNsRecord;

  @Before
  public void setUp() throws Exception {
    origin = new Name("example.com.");

    // SOA (owner must be the zone origin)
    soa = new SOARecord(
        origin,
        CLASS_IN,
        3600,
        new Name("ns1.example.com."),
        new Name("hostmaster.example.com."),
        2023100101L,
        7200,
        3600,
        1209600,
        3600);

    // NS at the zone apex
    nsRecord = Record.newRecord(origin, TYPE_NS, CLASS_IN, 3600);

    // A record for a sub‑domain (exact match tests)
    aRecord = Record.newRecord(new Name("www.example.com."), TYPE_A, CLASS_IN, 300);

    // CNAME for another sub‑domain (CNAME lookup test)
    cnameRecord = Record.newRecord(new Name("mail.example.com."), TYPE_CNAME, CLASS_IN, 300);

    // Wildcard A record (*.example.com)
    wildARecord = Record.newRecord(new Name("*.example.com."), TYPE_A, CLASS_IN, 300);

    // Delegation NS for a child zone (sub.example.com)
    delegatedNsRecord = Record.newRecord(new Name("sub.example.com."), TYPE_NS, CLASS_IN, 300);
  }

  /** Helper to build a Zone from an array of records. */
  private Zone buildZone(Record... extra) throws IOException {
    // The zone must contain exactly one SOA and at least one NS.
    Record[] all = new Record[2 + extra.length];
    all[0] = soa;
    all[1] = nsRecord;
    System.arraycopy(extra, 0, all, 2, extra.length);
    return new Zone(origin, all);
  }

  /* -------------------------------------------------------------------------
   * Constructor tests
   * ---------------------------------------------------------------------- */

  @Test
  public void testConstructorWithRecords_success() throws Exception {
    Zone z = buildZone();
    assertEquals(origin, z.getSOA().getName());
    assertEquals(origin, z.getNS().getName());
    assertEquals(TYPE_SOA, z.getSOA().getType());
    assertEquals(TYPE_NS, z.getNS().getType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullZone_throws() throws Exception {
    new Zone(null, (Record[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullRecords_throws() throws Exception {
    new Zone(origin, (Record[]) null);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingSOA_throws() throws Exception {
    new Zone(origin, nsRecord); // only NS, no SOA
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingNS_throws() throws Exception {
    new Zone(origin, soa); // only SOA, no NS
  }

  @Test
  public void testConstructorFromMasterFile_success() throws Exception {
    // Build a tiny master file in memory:
    //   $ORIGIN example.com.
    //   @   3600 IN SOA ns1.example.com. hostmaster.example.com. (
    //               2023100101 7200 3600 1209600 3600 )
    //   @   3600 IN NS ns1.example.com.
    //   www 300 IN A 192.0.2.1
    String master =
        "$ORIGIN example.com.\n"
            + "@ 3600 IN SOA ns1.example.com. hostmaster.example.com. (\n"
            + " 2023100101 7200 3600 1209600 3600 )\n"
            + "@ 3600 IN NS ns1.example.com.\n"
            + "www 300 IN A 192.0.2.1\n";

    ByteArrayInputStream in = new ByteArrayInputStream(master.getBytes("US-ASCII"));
    Zone z = new Zone(origin, in);
    assertNotNull(z.getSOA());
    assertNotNull(z.getNS());
    RRset www = z.findExactMatch(new Name("www.example.com."), TYPE_A);
    assertNotNull(www);
    assertEquals(1, www.size());
  }

  /* -------------------------------------------------------------------------
   * Simple getters
   * ---------------------------------------------------------------------- */

  @Test
  public void testGetDClass() throws Exception {
    Zone z = buildZone();
    assertEquals(CLASS_IN, z.getDClass());
  }

  @Test
  public void testGetNS_returnsCopy() throws Exception {
    Zone z = buildZone();
    RRset copy = z.getNS();
    // The returned set should be a copy – mutating it must not affect the zone.
    copy.addRR(Record.newRecord(origin, TYPE_NS, CLASS_IN, 3600));
    assertEquals(1, z.getNS().size());
  }

  @Test
  public void testGetSOA_isSameInstance() throws Exception {
    Zone z = buildZone();
    assertSame(z.getSOA(), z.getSOA());
  }

  /* -------------------------------------------------------------------------
   * addRecord / removeRecord
   * ---------------------------------------------------------------------- */

  @Test
  public void testAddRecordAndFindExactMatch() throws Exception {
    Zone z = buildZone();
    z.addRecord(aRecord);
    RRset rrset = z.findExactMatch(new Name("www.example.com."), TYPE_A);
    assertNotNull(rrset);
    assertEquals(1, rrset.size());
    assertEquals(aRecord, rrset.first());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecord_null_throws() throws Exception {
    Zone z = buildZone();
    z.addRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecord_SOAWithWrongOwner_throws() throws Exception {
    Zone z = buildZone();
    // Create a second SOA whose owner is not the zone origin
    SOARecord badSoa =
        new SOARecord(
            new Name("bad.example.com."),
            CLASS_IN,
            3600,
            new Name("ns1.bad.example.com."),
            new Name("hostmaster.bad.example.com."),
            2023100102L,
            7200,
            3600,
            1209600,
            3600);
    z.addRecord(badSoa);
  }

  @Test
  public void testRemoveRecord_simple() throws Exception {
    Zone z = buildZone(aRecord);
    z.removeRecord(aRecord);
    assertNull(z.findExactMatch(new Name("www.example.com."), TYPE_A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecord_null_throws() throws Exception {
    Zone z = buildZone();
    z.removeRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecord_SOA_notAllowed() throws Exception {
    Zone z = buildZone();
    z.removeRecord(soa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecord_lastNS_notAllowed() throws Exception {
    Zone z = buildZone();
    // The zone only has one NS record (the one we added in setUp)
    z.removeRecord(nsRecord);
  }

  /* -------------------------------------------------------------------------
   * addRRset / removeRRset
   * ---------------------------------------------------------------------- */

  @Test
  public void testAddRRset_replacesExisting() throws Exception {
    Zone z = buildZone();
    RRset rrset = new RRset(aRecord);
    z.addRRset(rrset);
    // Adding another RRset of same name/type should replace the previous one
    Record a2 = Record.newRecord(new Name("www.example.com."), TYPE_A, CLASS_IN, 300);
    RRset rrset2 = new RRset(a2);
    z.addRRset(rrset2);
    RRset found = z.findExactMatch(new Name("www.example.com."), TYPE_A);
    assertNotNull(found);
    assertEquals(1, found.size());
    assertEquals(a2, found.first());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRset_null_throws() throws Exception {
    Zone z = buildZone();
    z.addRRset(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRset_SOAWrongOwner_throws() throws Exception {
    Zone z = buildZone();
    RRset rrset = new RRset(
        new SOARecord(
            new Name("bad.example.com."),
            CLASS_IN,
            3600,
            new Name("ns1.bad.example.com."),
            new Name("hostmaster.bad.example.com."),
            2023100102L,
            7200,
            3600,
            1209600,
            3600));
    z.addRRset(rrset);
  }

  @Test
  public void testRemoveRRset_success() throws Exception {
    Zone z = buildZone(aRecord);
    z.removeRRset(new Name("www.example.com."), TYPE_A);
    assertNull(z.findExactMatch(new Name("www.example.com."), TYPE_A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRset_nullName_throws() throws Exception {
    Zone z = buildZone();
    z.removeRRset(null, TYPE_A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRset_removeSOA_notAllowed() throws Exception {
    Zone z = buildZone();
    z.removeRRset(origin, TYPE_SOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRset_removeAllNS_notAllowed() throws Exception {
    Zone z = buildZone();
    z.removeRRset(origin, TYPE_NS);
  }

  /* -------------------------------------------------------------------------
   * findExactMatch
   * ---------------------------------------------------------------------- */

  @Test
  public void testFindExactMatch_exists() throws Exception {
    Zone z = buildZone(aRecord);
    RRset rrset = z.findExactMatch(new Name("www.example.com."), TYPE_A);
    assertNotNull(rrset);
    assertEquals(1, rrset.size());
  }

  @Test
  public void testFindExactMatch_missing_returnsNull() throws Exception {
    Zone z = buildZone();
    assertNull(z.findExactMatch(new Name("nosuch.example.com."), TYPE_A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindExactMatch_nullName_throws() throws Exception {
    Zone z = buildZone();
    z.findExactMatch(null, TYPE_A);
  }

  /* -------------------------------------------------------------------------
   * findRecords – various response types
   * ---------------------------------------------------------------------- */

  @Test
  public void testFindRecords_exactSuccess() throws Exception {
    Zone z = buildZone(aRecord);
    SetResponse sr = z.findRecords(new Name("www.example.com."), TYPE_A);
    assertTrue(sr.isSuccessful());
    assertEquals(1, sr.answers().size());
    assertEquals(aRecord, sr.answers().get(0).first());
  }

  @Test
  public void testFindRecords_cnamePath() throws Exception {
    Zone z = buildZone(cnameRecord);
    SetResponse sr = z.findRecords(new Name("mail.example.com."), TYPE_A);
    assertTrue(sr.isCNAME());
    assertEquals(cnameRecord, sr.getCNAME());
  }

  @Test
  public void testFindRecords_wildcardExpansion() throws Exception {
    Zone z = buildZone(wildARecord);
    SetResponse sr = z.findRecords(new Name("any.example.com."), TYPE_A);
    assertTrue(sr.isSuccessful());
    RRset expanded = sr.answers().get(0);
    // The name of the returned record must be the queried name, not the wildcard.
    assertEquals(new Name("any.example.com."), expanded.first().getName());
    assertEquals(TYPE_A, expanded.getType());
  }

  @Test
  public void testFindRecords_delegation() throws Exception {
    Zone z = buildZone(delegatedNsRecord);
    SetResponse sr = z.findRecords(new Name("sub.example.com."), TYPE_A);
    assertTrue(sr.isDelegation());
    assertEquals(delegatedNsRecord, sr.getNS().first());
  }

  @Test
  public void testFindRecords_nxdomainOutsideZone() throws Exception {
    Zone z = buildZone();
    SetResponse sr = z.findRecords(new Name("outside.com."), TYPE_A);
    assertTrue(sr.isNXDOMAIN());
  }

  @Test
  public void testFindRecords_nxrrset() throws Exception {
    Zone z = buildZone();
    // The name exists (origin) but there is no A record.
    SetResponse sr = z.findRecords(origin, TYPE_A);
    assertTrue(sr.isNXRRSET());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindRecords_nullName_throws() throws Exception {
    Zone z = buildZone();
    z.findRecords(null, TYPE_A);
  }

  /* -------------------------------------------------------------------------
   * iterator and AXFR
   * ---------------------------------------------------------------------- */

  @Test
  public void testIterator_orderAndContent() throws Exception {
    // Add an extra A record to verify ordering after SOA and NS.
    Zone z = buildZone(aRecord);
    Iterator<RRset> it = z.iterator();

    // First RRset must be the SOA
    RRset first = it.next();
    assertEquals(TYPE_SOA, first.getType());

    // Second must be the NS of the zone apex
    RRset second = it.next();
    assertEquals(TYPE_NS, second.getType());

    // Third should be the A record we added
    RRset third = it.next();
    assertEquals(TYPE_A, third.getType());

    // No more entries
    assertFalse(it.hasNext());
  }

  @Test
  public void testAXFR_includesTrailingSOA() throws Exception {
    Zone z = buildZone(aRecord);
    Iterator<RRset> it = z.AXFR();

    // Consume all normal entries
    RRset last = null;
    while (it.hasNext()) {
      last = it.next();
    }

    // The very last RRset returned by AXFR must be the SOA again.
    assertNotNull(last);
    assertEquals(TYPE_SOA, last.getType());
    assertEquals(z.getSOA().getName(), last.getName());
  }

  @Test(expected = NoSuchElementException.class)
  public void testIterator_noMoreElements_throws() throws Exception {
    Zone z = buildZone();
    Iterator<RRset> it = z.iterator();
    // There are at least SOA and NS, so advance past them.
    it.next(); // SOA
    it.next(); // NS
    // Next should be absent -> throws
    it.next();
  }

  @Test
  public void testIterator_removeViaIterator() throws Exception {
    Zone z = buildZone(aRecord);
    Iterator<RRset> it = z.iterator();

    // Skip SOA and NS
    it.next(); // SOA
    it.next(); // NS

    // Remove the A RRset via iterator.remove()
    it.next(); // A
    it.remove();

    // The A record should be gone from the zone.
    assertNull(z.findExactMatch(new Name("www.example.com."), TYPE_A));
  }

  /* -------------------------------------------------------------------------
   * toMasterFile / toString
   * ---------------------------------------------------------------------- */

  @Test
  public void testToMasterFile_containsAllRecords() throws Exception {
    Zone z = buildZone(aRecord, cnameRecord, wildARecord);
    String master = z.toMasterFile();

    // The master file must contain the string representation of each record.
    assertTrue(master.contains(soa.toString()));
    assertTrue(master.contains(nsRecord.toString()));
    assertTrue(master.contains(aRecord.toString()));
    assertTrue(master.contains(cnameRecord.toString()));
    assertTrue(master.contains(wildARecord.toString()));
  }

  @Test
  public void testToString_isAliasForToMasterFile() throws Exception {
    Zone z = buildZone(aRecord);
    assertEquals(z.toMasterFile(), z.toString());
  }
}
