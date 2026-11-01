###Test START##
```java
/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Zone}.
 *
 * The tests cover:
 * <ul>
 *   <li>construction edge cases and validation of required records (SOA, NS)</li>
 *   <li>record and RRset manipulation (add/remove)</li>
 *   <li>lookup methods {@code findExactMatch} and {@code findRecords}</li>
 *   <li>iterator and AXFR behaviour</li>
 *   <li>string representation via {@code toMasterFile}</li>
 * </ul>
 *
 * The tests use only the public API that is listed in the prompt and avoid any mocking.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ZoneTest {

  private Name origin;
  private SOARecord soa;
  private Record nsOrigin;
  private Record aRecord;
  private Record cnameRecord;
  private Record dnameRecord;
  private Record wildcardA;
  private Record subNsRecord;

  /**
   * Build a minimal but complete zone used by many tests.
   *
   * <pre>
   * $ORIGIN example.com.
   * @       3600 IN SOA  ns.example.com. admin.example.com. 1 7200 3600 1209600 3600
   * @       3600 IN NS   ns.example.com.
   * host    3600 IN A    192.0.2.1
   * cname   3600 IN CNAME host.example.com.
   * dname   3600 IN DNAME other.example.com.
   * *.example.com. 3600 IN A 192.0.2.99
   * sub.example.com. 3600 IN NS ns.sub.example.com.
   * </pre>
   */
  @Before
  public void setUp() throws Exception {
    origin = Name.fromString("example.com.");

    // --- SOA --------------------------------------------------------------
    Name mname = Name.fromString("ns.example.com.");
    Name rname = Name.fromString("admin.example.com.");
    soa = new SOARecord(
        origin,
        DClass.IN,
        3600,
        mname,
        rname,
        1L,          // serial
        7200L,       // refresh
        3600L,       // retry
        1209600L,    // expire
        3600L);      // minimum

    // --- NS at the zone apex ------------------------------------------------
    nsOrigin = Record.newRecord(origin, Type.NS, DClass.IN, 3600);

    // --- A record for a host -------------------------------------------------
    aRecord = Record.newRecord(Name.fromString("host.example.com."), Type.A, DClass.IN, 3600);

    // --- CNAME record -------------------------------------------------------
    cnameRecord = Record.newRecord(Name.fromString("cname.example.com."), Type.CNAME, DClass.IN, 3600);

    // --- DNAME record -------------------------------------------------------
    dnameRecord = Record.newRecord(Name.fromString("dname.example.com."), Type.DNAME, DClass.IN, 3600);

    // --- Wildcard A record ---------------------------------------------------
    wildcardA = Record.newRecord(Name.fromString("*.example.com."), Type.A, DClass.IN, 3600);

    // --- Delegation NS for sub.example.com. ---------------------------------
    subNsRecord = Record.newRecord(Name.fromString("sub.example.com."), Type.NS, DClass.IN, 3600);
  }

  /** Helper to create a fresh zone with the common records. */
  private Zone createBasicZone() throws IOException {
    return new Zone(
        origin,
        soa,
        nsOrigin,
        aRecord,
        cnameRecord,
        dnameRecord,
        wildcardA,
        subNsRecord);
  }

  // -------------------------------------------------------------------------
  // Constructor validation
  // -------------------------------------------------------------------------

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullZone() throws IOException {
    new Zone(null, new Record[] {soa, nsOrigin});
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullRecordsArray() throws IOException {
    new Zone(origin, (Record[]) null);
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingSOA() throws IOException {
    new Zone(origin, nsOrigin); // only NS, no SOA
  }

  @Test(expected = IOException.class)
  public void testConstructorMissingNS() throws IOException {
    new Zone(origin, soa); // only SOA, no NS
  }

  // -------------------------------------------------------------------------
  // addRecord / removeRecord
  // -------------------------------------------------------------------------

  @Test
  public void testAddRecordAndReplaceSOA() throws IOException {
    Zone z = createBasicZone();

    // create a new SOA with a different serial
    SOARecord newSoa = new SOARecord(
        origin,
        DClass.IN,
        3600,
        Name.fromString("ns2.example.com."),
        Name.fromString("admin2.example.com."),
        2L, 7200L, 3600L, 1209600L, 3600L);

    // add the new SOA – it should replace the old one
    z.addRecord(newSoa);
    assertEquals(newSoa, z.getSOA());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordNull() throws IOException {
    Zone z = createBasicZone();
    z.addRecord(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordSofiaNotAtOrigin() throws IOException {
    Zone z = createBasicZone();
    // SOA whose name is not the zone origin
    SOARecord badSoa = new SOARecord(
        Name.fromString("other.example.com."),
        DClass.IN,
        3600,
        Name.fromString("ns.example.com."),
        Name.fromString("admin.example.com."),
        1L, 7200L, 3600L, 1209600L, 3600L);
    z.addRecord(badSoa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRecordNameNotSubdomain() throws IOException {
    Zone z = createBasicZone();
    // Absolute name outside the zone
    Record outOfZone = Record.newRecord(Name.fromString("outside.com."), Type.A, DClass.IN, 3600);
    z.addRecord(outOfZone);
  }

  @Test
  public void testRemoveRecordNonSoa() throws IOException {
    Zone z = createBasicZone();

    // remove the host A record – should disappear
    z.removeRecord(aRecord);
    assertNull(z.findExactMatch(Name.fromString("host.example.com."), Type.A));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordSoaNotAllowed() throws IOException {
    Zone z = createBasicZone();
    z.removeRecord(soa);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveLastNsNotAllowed() throws IOException {
    Zone z = createBasicZone();

    // zone has only one NS at the apex; removing it must fail
    z.removeRecord(nsOrigin);
  }

  @Test
  public void testRemoveRecordMiddleNsAllowed() throws IOException {
    Zone z = createBasicZone();

    // add a second NS at the apex, then remove one of them
    Record secondNs = Record.newRecord(origin, Type.NS, DClass.IN, 3600);
    z.addRecord(secondNs);
    // now we can remove the original NS
    z.removeRecord(nsOrigin);
    RRset remaining = z.findExactMatch(origin, Type.NS);
    assertNotNull(remaining);
    assertEquals(1, remaining.size());
  }

  // -------------------------------------------------------------------------
  // addRRset / removeRRset
  // -------------------------------------------------------------------------

  @Test
  public void testAddRRsetReplacesExisting() throws IOException {
    Zone z = createBasicZone();

    // Build a new RRset for host.example.com. with two A records
    Record a1 = Record.newRecord(Name.fromString("host.example.com."), Type.A, DClass.IN, 3600);
    Record a2 = Record.newRecord(Name.fromString("host.example.com."), Type.A, DClass.IN, 3600);
    RRset newSet = new RRset(a1, a2);

    z.addRRset(newSet);
    RRset fetched = z.findExactMatch(Name.fromString("host.example.com."), Type.A);
    assertNotNull(fetched);
    assertEquals(2, fetched.size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetNull() throws IOException {
    Zone z = createBasicZone();
    z.addRRset(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetSoaWrongName() throws IOException {
    Zone z = createBasicZone();
    // SOA with a wrong owner
    SOARecord bad = new SOARecord(
        Name.fromString("bad.example.com."),
        DClass.IN,
        3600,
        Name.fromString("ns.example.com."),
        Name.fromString("admin.example.com."),
        1L, 7200L, 3600L, 1209600L, 3600L);
    RRset set = new RRset(bad);
    z.addRRset(set);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRRsetSoaMultipleRecords() throws IOException {
    Zone z = createBasicZone();
    Record another = Record.newRecord(origin, Type.SOA, DClass.IN, 3600);
    RRset set = new RRset(soa, another);
    z.addRRset(set);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetNullName() throws IOException {
    Zone z = createBasicZone();
    z.removeRRset(null, Type.A);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetInvalidTypeSoa() throws IOException {
    Zone z = createBasicZone();
    z.removeRRset(origin, Type.SOA);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRRsetInvalidTypeNs() throws IOException {
    Zone z = createBasicZone();
    z.removeRRset(origin, Type.NS);
  }

  @Test
  public void testRemoveRRsetSuccess() throws IOException {
    Zone z = createBasicZone();

    // add an extra A record set for foo.example.com.
    Name foo = Name.fromString("foo.example.com.");
    Record aFoo = Record.newRecord(foo, Type.A, DClass.IN, 3600);
    RRset set = new RRset(aFoo);
    z.addRRset(set);

    // now remove it
    z.removeRRset(foo, Type.A);
    assertNull(z.findExactMatch(foo, Type.A));
  }

  // -------------------------------------------------------------------------
  // findExactMatch – copy semantics
  // -------------------------------------------------------------------------

  @Test
  public void testFindExactMatchReturnsCopy() throws IOException {
    Zone z = createBasicZone();

    RRset original = z.findExactMatch(Name.fromString("host.example.com."), Type.A);
    assertNotNull(original);
    int originalSize = original.size();

    // mutate the returned RRset – it must not affect the zone
    original.clear();
    assertEquals(0, original.size());

    RRset after = z.findExactMatch(Name.fromString("host.example.com."), Type.A);
    assertEquals(originalSize, after.size());
  }

  // -------------------------------------------------------------------------
  // findRecords – various response types
  // -------------------------------------------------------------------------

  @Test
  public void testFindRecordsExactSuccess() throws IOException {
    Zone z = createBasicZone();
    SetResponse resp = z.findRecords(Name.fromString("host.example.com."), Type.A);
    assertTrue(resp.isSuccessful());
    assertEquals(1, resp.answers().size());
    assertEquals(Type.A, resp.answers().get(0).getType());
  }

  @Test
  public void testFindRecordsCnameFallback() throws IOException {
    Zone z = createBasicZone();
    SetResponse resp = z.findRecords(Name.fromString("cname.example.com."), Type.A);
    assertTrue(resp.isCNAME());
    assertNotNull(resp.getCNAME());
  }

  @Test
  public void testFindRecordsDnameFallback() throws IOException {
    Zone z = createBasicZone();
    SetResponse resp = z.findRecords(Name.fromString("dname.example.com."), Type.A);
    assertTrue(resp.isDNAME());
    assertNotNull(resp.getDNAME());
  }

  @Test
  public void testFindRecordsDelegation() throws IOException {
    Zone z = createBasicZone();
    SetResponse resp = z.findRecords(Name.fromString("sub.example.com."), Type.A);
    assertTrue(resp.isDelegation());
    assertNotNull(resp.getNS());
    assertEquals(Type.NS, resp.getNS().getType());
  }

  @Test
  public void testFindRecordsAnyReturnsAll() throws IOException {
    Zone z = createBasicZone();
    SetResponse resp = z.findRecords(Name.fromString("host.example.com."), Type.ANY);
    assertTrue(resp.isSuccessful());

    // should contain at least the A RRset and possibly others (CNAME, DNAME etc.)
    boolean hasA = false;
    for (RRset set : resp.answers()) {
      if (set.getType() == Type.A) {
        hasA = true;
        break;
      }
    }
    assertTrue("ANY response must contain the A set", hasA);
  }

  @Test
  public void testFindRecordsNxrrset() throws IOException {
    Zone z = createBasicZone();
    // query a name that exists (host.example.com.) but for a type that does not (TXT)
    SetResponse resp = z.findRecords(Name.fromString("host.example.com."), Type.TXT);
    assertTrue(resp.isNXRRSET());
  }

  @Test
  public void testFindRecordsNxdomain() throws IOException {
    Zone z = createBasicZone();
    // name outside the zone
    SetResponse resp = z.findRecords(Name.fromString("outside.com."), Type.A);
    assertTrue(resp.isNXDOMAIN());
  }

  @Test
  public void testFindRecordsWildcardExpansion() throws IOException {
    Zone z = createBasicZone();

    // query a name that matches the wildcard *.example.com.
    Name q = Name.fromString("foo.example.com.");
    SetResponse resp = z.findRecords(q, Type.A);
    assertTrue(resp.isSuccessful());

    RRset set = resp.answers().get(0);
    // The returned RRset must have the queried name (not the wildcard name)
    assertEquals(q, set.getName());
    assertEquals(Type.A, set.getType());
  }

  // -------------------------------------------------------------------------
  // iterator and AXFR behaviour
  // -------------------------------------------------------------------------

  @Test
  public void testIteratorOrderAndContents() throws IOException {
    Zone z = createBasicZone();

    List<RRset> list = new ArrayList<>();
    for (RRset r : z) {
      list.add(r);
    }

    // First element must be the SOA of the zone origin
    assertEquals(Type.SOA, list.get(0).getType());
    // Second element must be the NS of the zone origin
    assertEquals(Type.NS, list.get(1).getType());

    // The wildcard and other records must also appear somewhere
    boolean foundWildcard = false;
    for (RRset r : list) {
      if (r.getName().isWild()) {
        foundWildcard = true;
        break;
      }
    }
    assertTrue("Wildcard RRset should be present", foundWildcard);
  }

  @Test
  public void testAXFRIteratorReturnsSOATwice() throws IOException {
    Zone z = createBasicZone();

    Iterator<RRset> axfr = z.AXFR();
    List<RRset> list = new ArrayList<>();
    while (axfr.hasNext()) {
      list.add(axfr.next());
    }

    // The first RRset is the SOA, the last one must also be a SOA (same name & type)
    assertEquals(Type.SOA, list.get(0).getType());
    assertEquals(Type.SOA, list.get(list.size() - 1).getType());

    // Ensure that the last SOA is a *different* object instance (a copy)
    assertNotSame(list.get(0), list.get(list.size() - 1));
  }

  @Test
  public void testIteratorRemoveDeletesRRset() throws IOException {
    Zone z = createBasicZone();

    // add a temporary A record for removal via iterator
    Name tmp = Name.fromString("tmp.example.com.");
    Record tmpA = Record.newRecord(tmp, Type.A, DClass.IN, 3600);
    z.addRecord(tmpA);

    // iterate until we encounter the temporary record and remove it
    Iterator<RRset> it = z.iterator();
    while (it.hasNext()) {
      RRset rs = it.next();
      if (rs.getName().equals(tmp) && rs.getType() == Type.A) {
        it.remove();
        break;
      }
    }

    // after removal the record must be gone
    assertNull(z.findExactMatch(tmp, Type.A));
  }

  // -------------------------------------------------------------------------
  // toMasterFile / toString
  // -------------------------------------------------------------------------

  @Test
  public void testToMasterFileContainsAllRecords() throws IOException {
    Zone z = createBasicZone();
    String master = z.toMasterFile();

    // Check that essential components appear in the textual output
    assertTrue(master.contains("example.com."));
    assertTrue(master.contains("SOA"));
    assertTrue(master.contains("NS"));
    assertTrue(master.contains("A"));
    assertTrue(master.contains("CNAME"));
    assertTrue(master.contains("DNAME"));
    assertTrue(master.contains("*")); // wildcard
  }

  @Test
  public void testToStringDelegatesToMasterFile() throws IOException {
    Zone z = createBasicZone();
    assertEquals(z.toMasterFile(), z.toString());
  }
}
```
###Test END##