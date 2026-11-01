```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#removeRRset(Name, int)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>validation of arguments (null name, invalid type);
 *   <li>successful removal of a regular RRset;
 *   <li>protection against removal of the mandatory NS RRset.
 * </ul>
 *
 * The zone used in the tests is built from a small, deterministic set of records
 * (identical to the one used in {@link ZoneTest}) so that the behaviour of
 * {@code removeRRset} can be observed without external resources.
 */
@ExtendWith(MockitoExtension.class)
class Zone_removeRRset_Test {

  /* --------------------------------------------------------------------- *
   *  Shared test fixtures
   * --------------------------------------------------------------------- */
  private static Name ZONE_NAME;
  private static SOARecord SOA1;
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
    // zone name and the mandatory SOA/NS records
    ZONE_NAME = Name.fromConstantString("example.");

    SOA1 =
        new SOARecord(
            ZONE_NAME,
            DClass.IN,
            3600L,
            Name.fromConstantString("nameserver."),
            new Name("hostadmin", ZONE_NAME),
            1,
            21600L,
            7200L,
            2160000L,
            3600L);

    NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
    NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

    // some additional records used by the tests
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
    // Build a fresh zone for each test – this guarantees test isolation.
    Record[] records = {
      SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD
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
    // Nothing to release at the class level.
  }

  /* --------------------------------------------------------------------- *
   *  Argument validation tests
   * --------------------------------------------------------------------- */

  @Test
  void removeRRset_nameIsNull_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> zone.removeRRset(null, Type.A),
        "Expected IllegalArgumentException when name is null");
  }

  @Test
  void removeRRset_invalidType_throwsInvalidTypeException() {
    // The Zone method forwards the type check to Type.check(...).
    assertThrows(
        InvalidTypeException.class,
        () -> zone.removeRRset(ZONE_NAME, -1),
        "Expected InvalidTypeException for an illegal type value");
  }

  /* --------------------------------------------------------------------- *
   *  Functional behaviour tests
   * --------------------------------------------------------------------- */

  @Test
  void removeRRset_existingARecord_successfulRemoval() {
    // Pre‑condition: the A record is present.
    assertNotNull(zone.findExactMatch(A_TEST.getName(), Type.A));

    // Act
    zone.removeRRset(A_TEST.getName(), Type.A);

    // Post‑condition: the RRset has disappeared.
    assertNull(zone.findExactMatch(A_TEST.getName(), Type.A));
  }

  @Test
  void removeRRset_removeAllNS_throwsIllegalStateException() {
    // The zone always needs at least one NS RRset; attempting to remove it should fail.
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> zone.removeRRset(ZONE_NAME, Type.NS),
            "Removing the sole NS RRset must be prohibited");

    // The exact message used by dnsjava is "remove all NS". We verify that the message contains it.
    assertTrue(
        ex.getMessage().toLowerCase().contains("remove all ns"),
        "Exception message should mention removal of all NS records");
  }

  @Test
  void removeRRset_removeSOA_throwsIllegalStateException() {
    // The zone must always contain an SOA record.
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> zone.removeRRset(ZONE_NAME, Type.SOA),
            "Removing the SOA RRset must be prohibited");

    assertTrue(
        ex.getMessage().toLowerCase().contains("remove soa"),
        "Exception message should mention removal of the SOA record");
  }
}
```