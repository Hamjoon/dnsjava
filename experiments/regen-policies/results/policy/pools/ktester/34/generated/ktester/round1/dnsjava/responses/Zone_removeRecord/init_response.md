```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#removeRecord(Record)}.
 *
 * The tests cover:
 * <ul>
 *   <li>validation of arguments (null, SOA, removing the last NS)</li>
 *   <li>removing a record that is part of a multi‑record RRset</li>
 *   <li>removing a record that is the sole member of its RRset</li>
 *   <li>attempting to remove a record that does not exist</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Zone_removeRecord_Test {

  /* --------------------------------------------------------------------- *
   *  Shared test data – created before each test method                     *
   * --------------------------------------------------------------------- */
  private Name ZONE_NAME;
  private SOARecord SOA1;
  private NSRecord NS1;
  private NSRecord NS2;
  private ARecord A_UNIQUE;
  private ARecord A_TEST;
  private AAAARecord AAAA_1_TEST;
  private AAAARecord AAAA_2_TEST;
  private TXTRecord TXT_WILD;
  private Zone ZONE;

  @BeforeAll
  static void setupBeforeAll() {
    // No global resources needed for these tests.
  }

  @BeforeEach
  void setupBeforeEach() throws IOException {
    // zone name: example.
    ZONE_NAME = Name.fromConstantString("example.");

    // mandatory SOA record
    SOA1 =
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

    // two NS records – required by the Zone contract
    NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
    NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

    // a few address records
    InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
    InetAddress localhost6a = InetAddress.getByName("::1");
    InetAddress localhost6b = InetAddress.getByName("::2");

    A_UNIQUE =
        new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
    A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
    AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
    AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
    TXT_WILD = new TXTRecord(new Name("wild", ZONE_NAME), DClass.IN, 3600, "some text");

    // Build the zone with a typical set of records
    Record[] records = {
        SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, TXT_WILD,
    };
    ZONE = new Zone(ZONE_NAME, records);
  }

  @AfterEach
  void teardownAfterEach() {
    // Nothing to clean up – the Zone instance is garbage‑collected.
  }

  @AfterAll
  static void teardownAfterAll() {
    // No global resources to release.
  }

  /* --------------------------------------------------------------------- *
   *  Test cases                                                            *
   * --------------------------------------------------------------------- */

  @Test
  void removeRecord_nullArgument_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> ZONE.removeRecord(null));
  }

  @Test
  void removeRecord_SOArecord_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> ZONE.removeRecord(SOA1));
  }

  @Test
  void removeRecord_lastNS_throwsIllegalArgumentException() throws IOException {
    // Build a zone that contains exactly one NS (plus the mandatory SOA).
    Record[] minimal = {SOA1, NS1};
    Zone singleNsZone = new Zone(ZONE_NAME, minimal);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> singleNsZone.removeRecord(NS1));
    assertTrue(ex.getMessage().contains("Cannot remove all NS"));
  }

  @Test
  void removeRecord_recordInMultiRRset_deletesOnlyThatRecord() {
    // Both A_UNIQUE and A_TEST share the same type (A) but have different names.
    // Removing A_UNIQUE must not affect A_TEST.
    assertNotNull(ZONE.findExactMatch(A_UNIQUE.getName(), Type.A));
    assertNotNull(ZONE.findExactMatch(A_TEST.getName(), Type.A));

    assertDoesNotThrow(() -> ZONE.removeRecord(A_UNIQUE));

    assertNull(ZONE.findExactMatch(A_UNIQUE.getName(), Type.A));
    assertNotNull(ZONE.findExactMatch(A_TEST.getName(), Type.A));
  }

  @Test
  void removeRecord_soleRecordRemovesRRset() {
    // TXT_WILD is the only TXT record for its name.
    assertNotNull(ZONE.findExactMatch(TXT_WILD.getName(), Type.TXT));

    assertDoesNotThrow(() -> ZONE.removeRecord(TXT_WILD));

    // After removal the exact match must be null.
    assertNull(ZONE.findExactMatch(TXT_WILD.getName(), Type.TXT));
  }

  @Test
  void removeRecord_nonExistentRecord_isNoOp() throws IOException {
    // Create a record that is *not* present in the zone.
    ARecord notInZone =
        new ARecord(new Name("doesnotexist", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));

    // Ensure it truly does not exist before the call.
    assertNull(ZONE.findExactMatch(notInZone.getName(), Type.A));

    // The call must not throw and must leave the zone unchanged.
    assertDoesNotThrow(() -> ZONE.removeRecord(notInZone));

    // Verify that an existing record (A_TEST) is still present.
    assertNotNull(ZONE.findExactMatch(A_TEST.getName(), Type.A));
  }
}
```