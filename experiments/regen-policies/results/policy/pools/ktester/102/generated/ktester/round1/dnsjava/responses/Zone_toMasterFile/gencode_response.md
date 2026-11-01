```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;

class Zone_toMasterFile_Test {

  private static Name ZONE_NAME;
  private static SOARecord SOA;
  private static NSRecord NS1;
  private static NSRecord NS2;
  private static ARecord A_UNIQUE;
  private static ARecord A_TEST;
  private static AAAARecord AAAA_1_TEST;
  private static AAAARecord AAAA_2_TEST;
  private static ARecord A_WILD;
  private static TXTRecord TXT_WILD;
  private Zone zone;

  @BeforeAll
  static void setupBeforeAll() throws IOException {
    // common immutable objects for all tests
    ZONE_NAME = Name.fromConstantString("example.");
    SOA =
        new SOARecord(
            ZONE_NAME,
            DClass.IN,
            3600L,
            Name.fromConstantString("ns.example."),
            new Name("hostadmin", ZONE_NAME),
            1,
            21600L,
            7200L,
            2160000L,
            3600L);
    NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
    NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
    InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
    InetAddress localhost6a = InetAddress.getByName("::1");
    InetAddress localhost6b = InetAddress.getByName("::2");

    A_UNIQUE =
        new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
    A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
    AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
    AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
    A_WILD =
        new ARecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
    TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");
  }

  @BeforeEach
  void setupBeforeEach() throws IOException {
    // Build a zone that contains a variety of record types.
    Record[] records = {
      SOA,
      NS1,
      NS2,
      A_UNIQUE,
      A_TEST,
      AAAA_1_TEST,
      AAAA_2_TEST,
      A_WILD,
      TXT_WILD
    };
    zone = new Zone(ZONE_NAME, records);
  }

  @AfterEach
  void teardownAfterEach() {
    // No external resources to clean up.
    zone = null;
  }

  @AfterAll
  static void teardownAfterAll() {
    // Nothing to clean up at class level.
  }

  /**
   * Verifies that {@link Zone#toMasterFile()} returns a non‑empty string that contains a textual
   * representation of every record that was added to the zone. The exact formatting of each line
   * is delegated to the {@code Record#toString()} implementation, which is already exercised in
   * other tests, so we assert on the presence of those substrings.
   */
  @Test
  void toMasterFileContainsAllRecords() {
    String master = zone.toMasterFile();

    // The master file must contain the origin SOA first.
    int soaIdx = master.indexOf(SOA.toString());
    assertTrue(soaIdx >= 0, "SOA record should be present");

    // NS records must be present (order after SOA is not strict, but they must exist)
    assertTrue(master.contains(NS1.toString()), "NS1 should be present");
    assertTrue(master.contains(NS2.toString()), "NS2 should be present");

    // Various address records
    assertTrue(master.contains(A_UNIQUE.toString()), "Unique A record should be present");
    assertTrue(master.contains(A_TEST.toString()), "A test record should be present");
    assertTrue(master.contains(AAAA_1_TEST.toString()), "AAAA test record #1 should be present");
    assertTrue(master.contains(AAAA_2_TEST.toString()), "AAAA test record #2 should be present");
    assertTrue(master.contains(A_WILD.toString()), "Wildcard A record should be present");
    assertTrue(master.contains(TXT_WILD.toString()), "Wildcard TXT record should be present");

    // Ensure the SOA appears before any other record (origin node is written first)
    List<String> otherRecords =
        List.of(
            NS1.toString(),
            NS2.toString(),
            A_UNIQUE.toString(),
            A_TEST.toString(),
            AAAA_1_TEST.toString(),
            AAAA_2_TEST.toString(),
            A_WILD.toString(),
            TXT_WILD.toString());
    for (String rec : otherRecords) {
      int idx = master.indexOf(rec);
      assertTrue(idx > soaIdx, "Record \"" + rec + "\" should appear after the SOA");
    }
  }

  /**
   * {@link Zone#toString()} is defined to delegate to {@link Zone#toMasterFile()}. This test
   * confirms that contract.
   */
  @Test
  void toStringDelegatesToToMasterFile() {
    String viaToString = zone.toString();
    String viaMasterFile = zone.toMasterFile();
    assertEquals(viaMasterFile, viaToString, "Zone.toString() must return the same value as toMasterFile()");
  }

  /**
   * A minimal zone (SOA + a single NS) should still produce a valid master‑file string.
   */
  @Test
  void minimalZoneProducesNonEmptyMasterFile() throws IOException {
    Record[] minimal = {SOA, NS1};
    Zone minimalZone = new Zone(ZONE_NAME, minimal);
    String master = minimalZone.toMasterFile();

    assertFalse(master.isBlank(), "Master file for a minimal zone should not be blank");
    assertTrue(master.contains(SOA.toString()), "SOA must be present");
    assertTrue(master.contains(NS1.toString()), "NS must be present");
  }

  /* -------------------------------------------------------------------------
   * Additional test cases derived from the JSON specification
   * -------------------------------------------------------------------------
   */

  /**
   * Verify that toMasterFile() returns an empty string when the Zone has no origin node and
   * no data entries.
   */
  @Test
  void emptyZoneReturnsEmptyString() {
    Zone emptyZone = new Zone(ZONE_NAME, new Record[0]); // no records => originNode stays null
    String master = emptyZone.toMasterFile();
    assertTrue(master.isEmpty(), "Master file should be empty for a zone without originNode and data");
  }

  /**
   * Create a Zone that only contains the origin node (SOA + NS records) and verify that
   * the master file contains only those records.
   */
  @Test
  void originOnlyContainsOriginRecords() {
    Record[] originOnly = {SOA, NS1, NS2};
    Zone originOnlyZone = new Zone(ZONE_NAME, originOnly);
    String master = originOnlyZone.toMasterFile();

    // Expected records
    assertTrue(master.contains(SOA.toString()), "SOA must be present");
    assertTrue(master.contains(NS1.toString()), "NS1 must be present");
    assertTrue(master.contains(NS2.toString()), "NS2 must be present");

    // No other records should appear
    assertFalse(master.contains(A_UNIQUE.toString()), "Unexpected A record found");
    assertFalse(master.contains(TXT_WILD.toString()), "Unexpected TXT record found");
  }

  /**
   * Zone with a non‑origin entry only. The origin node is null, so only the non‑origin record
   * should be serialized.
   */
  @Test
  void singleNonOriginEntrySerializesOnlyThatRecord() throws Exception {
    // start with an empty zone (originNode null)
    Zone z = new Zone(ZONE_NAME, new Record[0]);

    // create a non‑origin RRset (www.example.)
    Name www = new Name("www", ZONE_NAME);
    ARecord wwwA = new ARecord(www, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
    RRset rrset = new RRset();
    rrset.addRR(wwwA);
    z.addRRset(rrset);

    String master = z.toMasterFile();

    // Should contain the www A record but not the SOA/NS (originNode is null)
    assertTrue(master.contains(wwwA.toString()), "Non‑origin A record should be present");
    assertFalse(master.contains(SOA.toString()), "SOA should not be present when originNode is null");
    assertFalse(master.contains(NS1.toString()), "NS should not be present when originNode is null");
  }

  /**
   * Origin present and duplicate entry in data map – the origin must appear only once.
   */
  @Test
  void duplicateOriginEntryIsIgnored() throws Exception {
    // Build a normal zone with origin records
    Zone z = new Zone(ZONE_NAME, new Record[] {SOA, NS1, NS2});

    // Add a duplicate RRset for the origin (same name as zone origin)
    RRset dupOrigin = new RRset();
    dupOrigin.addRR(SOA); // adding the same SOA again
    z.addRRset(dupOrigin); // this will place another entry for the origin in the data map

    // Add a distinct RRset for mail.example.
    Name mail = new Name("mail", ZONE_NAME);
    ARecord mailA = new ARecord(mail, DClass.IN, 3600, InetAddress.getByName("192.0.2.2"));
    RRset mailRRset = new RRset();
    mailRRset.addRR(mailA);
    z.addRRset(mailRRset);

    String master = z.toMasterFile();

    // Origin records should appear exactly once
    int firstSOA = master.indexOf(SOA.toString());
    int secondSOA = master.indexOf(SOA.toString(), firstSOA + 1);
    assertEquals(-1, secondSOA, "SOA should appear only once in the master file");

    // Mail record must be present
    assertTrue(master.contains(mailA.toString()), "Mail A record should be present");
  }

  /**
   * Verify that entries are ordered alphabetically after the origin node.
   */
  @Test
  void multipleOrderedEntriesRespectAlphabeticalOrder() throws Exception {
    Zone z = new Zone(ZONE_NAME, new Record[] {SOA, NS1, NS2});

    // a.example.
    Name a = new Name("a", ZONE_NAME);
    ARecord aRec = new ARecord(a, DClass.IN, 3600, InetAddress.getByName("192.0.2.10"));
    RRset aSet = new RRset();
    aSet.addRR(aRec);
    z.addRRset(aSet);

    // m.example.
    Name m = new Name("m", ZONE_NAME);
    ARecord mRec = new ARecord(m, DClass.IN, 3600, InetAddress.getByName("192.0.2.20"));
    RRset mSet = new RRset();
    mSet.addRR(mRec);
    z.addRRset(mSet);

    // z.example.
    Name zz = new Name("z", ZONE_NAME);
    ARecord zRec = new ARecord(zz, DClass.IN, 3600, InetAddress.getByName("192.0.2.30"));
    RRset zSet = new RRset();
    zSet.addRR(zRec);
    z.addRRset(zSet);

    String master = z.toMasterFile();

    // Extract the lines containing our three names
    List<String> lines = List.of(master.split("\n"));
    List<String> filtered = new ArrayList<>();
    for (String line : lines) {
      if (line.contains(aRec.toString()) || line.contains(mRec.toString()) || line.contains(zRec.toString())) {
        filtered.add(line);
      }
    }

    // Verify order a -> m -> z
    assertEquals(3, filtered.size(), "Exactly three non‑origin records expected");
    assertTrue(filtered.get(0).contains(aRec.toString()), "First record should be a.example");
    assertTrue(filtered.get(1).contains(mRec.toString()), "Second record should be m.example");
    assertTrue(filtered.get(2).contains(zRec.toString()), "Third record should be z.example");
  }

  /**
   * When the origin field is null, toMasterFile() should throw a NullPointerException.
   */
  @Test
  void nullOriginLeadsToNPE() throws Exception {
    Zone z = new Zone(ZONE_NAME, new Record[] {SOA, NS1});
    // Force origin to null via reflection
    setPrivateField(z, "origin", null);
    assertThrows(NullPointerException.class, z::toMasterFile,
        "Calling toMasterFile() with a null origin should throw NPE");
  }

  /**
   * Setting originNode to an unexpected type should cause a ClassCastException from
   * nodeToString().
   */
  @Test
  void invalidOriginNodeTypeThrowsClassCastException() throws Exception {
    Zone z = new Zone(ZONE_NAME, new Record[] {SOA, NS1});
    // Replace originNode with a plain String
    setPrivateField(z, "originNode", "I am not a node");
    assertThrows(ClassCastException.class, z::toMasterFile,
        "Invalid originNode type should propagate ClassCastException");
  }

  /**
   * Inserting a map entry whose value type is incompatible with nodeToString() should cause a
   * ClassCastException.
   */
  @Test
  void invalidDataMapValueTypeThrowsClassCastException() throws Exception {
    Zone z = new Zone(ZONE_NAME, new Record[] {SOA, NS1});
    // Obtain the private data map and inject an invalid value
    @SuppressWarnings("unchecked")
    Map<Name, Object> dataMap = (Map<Name, Object>) getPrivateField(z, "data");
    dataMap.put(new Name("bad", ZONE_NAME), Integer.valueOf(42));
    assertThrows(ClassCastException.class, z::toMasterFile,
        "Invalid data map value type should cause ClassCastException");
  }

  /**
   * Adding a null key to the data map leads to a NullPointerException when origin.equals(key)
   * is evaluated.
   */
  @Test
  void nullKeyInDataMapThrowsNPE() throws Exception {
    Zone z = new Zone(ZONE_NAME, new Record[] {SOA, NS1});
    @SuppressWarnings("unchecked")
    Map<Name, Object> dataMap = (Map<Name, Object>) getPrivateField(z, "data");
    dataMap.put(null, SOA); // value can be anything non‑null
    assertThrows(NullPointerException.class, z::toMasterFile,
        "Null key in data map should cause NPE during iteration");
  }

  /**
   * Adding a null value to the data map causes a NullPointerException inside nodeToString().
   */
  @Test
  void nullValueInDataMapThrowsNPE() throws Exception {
    Zone z = new Zone(ZONE_NAME, new Record[] {SOA, NS1});
    @SuppressWarnings("unchecked")
    Map<Name, Object> dataMap = (Map<Name, Object>) getPrivateField(z, "data");
    dataMap.put(new Name("nullvalue", ZONE_NAME), null);
    assertThrows(NullPointerException.class, z::toMasterFile,
        "Null value in data map should cause NPE inside nodeToString");
  }

  /* -------------------------------------------------------------------------
   * Helper reflection utilities
   * -------------------------------------------------------------------------
   */

  /**
   * Sets a private (or package‑private) field on the given target object.
   *
   * @param target    the object whose field should be modified
   * @param fieldName the name of the field
   * @param value     the new value
   */
  private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  /**
   * Retrieves the value of a private field.
   *
   * @param target    the object containing the field
   * @param fieldName the name of the field
   * @return the field's value
   */
  private static Object getPrivateField(Object target, String fieldName) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    return f.get(target);
  }
}
```