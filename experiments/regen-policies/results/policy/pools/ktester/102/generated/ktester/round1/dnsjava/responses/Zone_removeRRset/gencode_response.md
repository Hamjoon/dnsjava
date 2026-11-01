```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#removeRRset(Name, int)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>validation of arguments (null name, illegal type);
 *   <li>successful removal of an existing RRset (single‑record and multi‑record);
 *   <li>type‑specific removal when multiple RRsets share the same name;
 *   <li>removal of a non‑existing RRset (no side‑effects);
 *   <li>behaviour when critical zone records (SOA, NS) are removed.
 * </ul>
 *
 * The zone is rebuilt before each test using a minimal but well‑known set of records.
 */
@ExtendWith(MockitoExtension.class)
class Zone_removeRRset_Test {

  private static final String ZONE_NAME_STR = "example.";
  private Name zoneName;
  private SOARecord soa;
  private NSRecord ns1;
  private NSRecord ns2;
  private ARecord aTest;
  private AAAARecord aaaaTest;
  private Zone zone;

  /** Create a fresh {@link Zone} with a handful of records before each test. */
  @BeforeEach
  void setupBeforeEach() throws IOException {
    zoneName = Name.fromConstantString(ZONE_NAME_STR);

    // SOA – required for every zone
    soa =
        new SOARecord(
            zoneName,
            DClass.IN,
            3600L,
            Name.fromConstantString("nameserver."),
            new Name("hostadmin", zoneName),
            1,
            21600L,
            7200L,
            2160000L,
            3600L);

    // two NS records (also required)
    ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
    ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

    // A and AAAA records that we will later try to remove
    InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
    InetAddress localhost6 = InetAddress.getByName("::1");
    aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
    aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6);

    Record[] records = new Record[] {soa, ns1, ns2, aTest, aaaaTest};
    zone = new Zone(zoneName, records);
  }

  /** Clean up references after each test – not strictly required but keeps the test tidy. */
  @AfterEach
  void teardownAfterEach() {
    zone = null;
    zoneName = null;
    soa = null;
    ns1 = null;
    ns2 = null;
    aTest = null;
    aaaaTest = null;
  }

  /* -------------------------------------------------------------------------
   * Argument validation
   * ---------------------------------------------------------------------- */

  @Test
  void removeRRset_nullName_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> zone.removeRRset(null, Type.A),
        "name must not be null");
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 65536})
  void removeRRset_invalidType_throwsIllegalArgumentException(int invalidType) {
    // Type.check throws IllegalArgumentException for out‑of‑range values
    assertThrows(
        IllegalArgumentException.class,
        () -> zone.removeRRset(Name.fromConstantString("example.com."), invalidType));
  }

  /* -------------------------------------------------------------------------
   * Successful removal of an existing RRset (single record)
   * ---------------------------------------------------------------------- */

  @Test
  void removeRRset_existingARecord_isRemoved() {
    // pre‑condition: the A RRset is present
    assertNotNull(zone.findExactMatch(aTest.getName(), Type.A));

    // act
    zone.removeRRset(aTest.getName(), Type.A);

    // post‑condition: the RRset is gone
    assertNull(zone.findExactMatch(aTest.getName(), Type.A));

    // The iterator must also reflect the removal (size reduced by one)
    int count = 0;
    Iterator<RRset> it = zone.iterator();
    while (it.hasNext()) {
      it.next();
      count++;
    }
    // original zone had 5 RRsets (SOA, NS, NS, A, AAAA)
    assertEquals(4, count);
  }

  @Test
  void removeRRset_existingAAAARecord_isRemoved() {
    assertNotNull(zone.findExactMatch(aaaaTest.getName(), Type.AAAA));

    zone.removeRRset(aaaaTest.getName(), Type.AAAA);

    assertNull(zone.findExactMatch(aaaaTest.getName(), Type.AAAA));
  }

  /* -------------------------------------------------------------------------
   * Successful removal when multiple RRsets share the same name
   * ---------------------------------------------------------------------- */

  @Test
  void removeRRset_MXRemoved_ARecordStays() throws IOException {
    // Build a name that already exists (example.org.) and add both A and MX RRsets.
    Name name = Name.fromConstantString("example.org.");

    // A record
    ARecord a = new ARecord(name, DClass.IN, 300, InetAddress.getByName("192.0.2.1"));
    zone.addRecord(a);

    // MX record
    MXRecord mx = new MXRecord(name, DClass.IN, 300, 10, Name.fromConstantString("mail.example.org."));
    zone.addRecord(mx);

    // Verify both are present
    assertNotNull(zone.findExactMatch(name, Type.A));
    assertNotNull(zone.findExactMatch(name, Type.MX));

    // Remove only the MX RRset
    zone.removeRRset(name, Type.MX);

    // MX must be gone, A must stay
    assertNull(zone.findExactMatch(name, Type.MX));
    assertNotNull(zone.findExactMatch(name, Type.A));
  }

  /* -------------------------------------------------------------------------
   * Removal of a multi‑record RRset
   * ---------------------------------------------------------------------- */

  @Test
  void removeRRset_ARecordSetWithMultipleRecords_isRemovedCompletely() throws IOException {
    Name name = Name.fromConstantString("multirecord.com.");

    // Create three distinct A records for the same name
    List<ARecord> aRecords = new ArrayList<>();
    aRecords.add(new ARecord(name, DClass.IN, 300, InetAddress.getByName("192.0.2.10")));
    aRecords.add(new ARecord(name, DClass.IN, 300, InetAddress.getByName("192.0.2.11")));
    aRecords.add(new ARecord(name, DClass.IN, 300, InetAddress.getByName("192.0.2.12")));

    // Add each record – they will be merged into a single RRset internally
    for (ARecord r : aRecords) {
      zone.addRecord(r);
    }

    // Pre‑condition: the RRset exists and contains three records
    RRset rrset = zone.findExactMatch(name, Type.A);
    assertNotNull(rrset);
    assertEquals(3, rrset.size());

    // Act: remove the whole A RRset
    zone.removeRRset(name, Type.A);

    // Post‑condition: the RRset is gone
    assertNull(zone.findExactMatch(name, Type.A));
  }

  /* -------------------------------------------------------------------------
   * No‑op removal when RRset absent
   * ---------------------------------------------------------------------- */

  @Test
  void removeRRset_nonExistingName_doesNothing() {
    Name nonExisting = Name.fromConstantString("doesnotexist.example.");

    // Ensure the name truly does not exist
    assertNull(zone.findExactMatch(nonExisting, Type.A));

    // Act – should not throw
    assertDoesNotThrow(() -> zone.removeRRset(nonExisting, Type.A));

    // Verify that existing data is untouched
    assertNotNull(zone.findExactMatch(aTest.getName(), Type.A));
    assertNotNull(zone.findExactMatch(aaaaTest.getName(), Type.AAAA));
  }

  @Test
  void removeRRset_nonExistingSRV_doesNothing() throws IOException {
    // Add an A record for example.net. but no SRV record
    Name name = Name.fromConstantString("example.net.");
    ARecord a = new ARecord(name, DClass.IN, 300, InetAddress.getByName("192.0.2.20"));
    zone.addRecord(a);

    // Confirm the A RRset exists and SRV does not
    assertNotNull(zone.findExactMatch(name, Type.A));
    assertNull(zone.findExactMatch(name, Type.SRV));

    // Attempt to remove the non‑existent SRV RRset
    assertDoesNotThrow(() -> zone.removeRRset(name, Type.SRV));

    // A RRset must still be present
    assertNotNull(zone.findExactMatch(name, Type.A));
  }

  /* -------------------------------------------------------------------------
   * Removal of critical zone records (SOA, NS)
   * ---------------------------------------------------------------------- */

  @Test
  void removeRRset_SOA_isRemoved() {
    assertNotNull(zone.findExactMatch(zoneName, Type.SOA));

    zone.removeRRset(zoneName, Type.SOA);

    assertNull(zone.findExactMatch(zoneName, Type.SOA));
    // The zone still contains the NS records – we check one of them
    assertNotNull(zone.findExactMatch(zoneName, Type.NS));
  }

  @Test
  void removeRRset_NS_isRemoved() {
    // Pre‑condition: NS RRset is present at the zone apex
    assertNotNull(zone.findExactMatch(zoneName, Type.NS));

    zone.removeRRset(zoneName, Type.NS);

    // After removal the zone should no longer return an NS RRset
    assertNull(zone.findExactMatch(zoneName, Type.NS));
    // SOA must still be present
    assertNotNull(zone.findExactMatch(zoneName, Type.SOA));
  }

  /* -------------------------------------------------------------------------
   * Additional sanity check – iterator reflects removals
   * ---------------------------------------------------------------------- */

  @Test
  void iteratorReflectsAllRemovals() {
    // Initial count (SOA + 2×NS + A + AAAA = 5)
    int initial = 0;
    for (Iterator<RRset> it = zone.iterator(); it.hasNext(); it.next()) initial++;
    assertEquals(5, initial);

    // Remove A and AAAA
    zone.removeRRset(aTest.getName(), Type.A);
    zone.removeRRset(aaaaTest.getName(), Type.AAAA);

    // Count after removals should be 3 (SOA + 2×NS)
    int after = 0;
    for (Iterator<RRset> it = zone.iterator(); it.hasNext(); it.next()) after++;
    assertEquals(3, after);
  }
}
```