```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#removeRRset(Name, int)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>validation of arguments (null name, illegal type);
 *   <li>successful removal of an existing RRset;
 *   <li>removal of a non‑existing RRset (no side‑effects);
 *   <li>behaviour when the SOA RRset is removed.
 * </ul>
 *
 * The zone is built once per test in {@code @BeforeEach} using the same records that are used in the
 * existing {@code ZoneTest}. This guarantees that the zone is in a known state before each test
 * runs.
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

  /** No global resources are used, so the class‑level hooks are empty. */
  @BeforeAll
  static void setupBeforeAll() {}

  @AfterAll
  static void teardownAfterAll() {}

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

  @Test
  void removeRRset_invalidType_throwsIllegalArgumentException() {
    // Type.check throws IllegalArgumentException for unknown types
    assertThrows(
        IllegalArgumentException.class,
        () -> zone.removeRRset(Name.fromConstantString("test.example."), -1));
  }

  /* -------------------------------------------------------------------------
   * Successful removal of an existing RRset
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
   * Removing a non‑existing RRset must be a no‑op (no exception, no side‑effects)
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

  /* -------------------------------------------------------------------------
   * Removing the mandatory SOA RRset – the public API permits it, and the zone
   * should simply no longer return the SOA.
   * ---------------------------------------------------------------------- */

  @Test
  void removeRRset_SOA_isRemoved() {
    assertNotNull(zone.findExactMatch(zoneName, Type.SOA));

    zone.removeRRset(zoneName, Type.SOA);

    assertNull(zone.findExactMatch(zoneName, Type.SOA));
    // The zone still contains the NS records – we check one of them
    assertNotNull(zone.findExactMatch(zoneName, Type.NS));
  }
}
```