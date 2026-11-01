```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
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
}
```