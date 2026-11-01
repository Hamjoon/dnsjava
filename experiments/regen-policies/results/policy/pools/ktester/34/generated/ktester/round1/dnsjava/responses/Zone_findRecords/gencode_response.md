```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#findRecords(Name, int)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>validation of arguments (null name, illegal type);
 *   <li>behaviour when the queried name is not a sub‑domain of the zone origin (NXDOMAIN);
 *   <li>successful look‑ups for exact matches;
 *   <li>wild‑card handling;
 *   <li>edge‑cases such as a {@code Zone} with a {@code null} origin.
 * </ul>
 *
 * The {@link Zone} instance is created once per test method using the constructor that accepts a
 * {@link Name} and an array of {@link Record}s – this avoids any external file or network access.
 */
@ExtendWith(MockitoExtension.class)
class Zone_findRecords_Test {

  /** Common objects used by all tests. */
  private Name zoneName;
  private SOARecord soa;
  private NSRecord ns1;
  private NSRecord ns2;
  private ARecord aUnique;
  private ARecord aTest;
  private AAAARecord aaaaTest;
  private ARecord aWild;
  private TXTRecord txtWild;
  private Zone zone;

  @BeforeAll
  static void setupBeforeAll() {
    // No global resources are required for these tests.
  }

  @BeforeEach
  void setupBeforeEach() throws IOException {
    // ----------------------------------------------------------------------
    // Build a minimal but realistic zone (the same data used in the original
    // ZoneTest class).  All records are absolute and belong to the zone
    // "example.".
    // ----------------------------------------------------------------------
    zoneName = Name.fromConstantString("example.");

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

    ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
    ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

    InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
    InetAddress localhost6a = InetAddress.getByName("::1");

    aUnique =
        new ARecord(new Name("unique", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
    aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
    aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6a);

    aWild =
        new ARecord(
            new Name("*.wild", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
    txtWild = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");

    Record[] records = {
      soa, ns1, ns2, aUnique, aTest, aaaaTest, aWild, txtWild,
    };
    zone = new Zone(zoneName, records);
  }

  @AfterEach
  void teardownAfterEach() {
    // Nothing to clean up – the zone is in‑memory only.
  }

  @AfterAll
  static void teardownAfterAll() {
    // No global resources to release.
  }

  // ----------------------------------------------------------------------
  //  Argument validation
  // ----------------------------------------------------------------------
  @Test
  void findRecords_nullName_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> zone.findRecords(null, Type.A),
        "name must not be null");
  }

  @ParameterizedTest
  @ValueSource(ints = { -1, 65536 })
  void findRecords_invalidType_throwsInvalidTypeException(int invalidType) {
    // Type.check throws InvalidTypeException (a subclass of IllegalArgumentException)
    // for values that are not defined by the DNS protocol.
    assertThrows(
        InvalidTypeException.class,
        () -> zone.findRecords(Name.fromConstantString("example.com."), invalidType));
  }

  // ----------------------------------------------------------------------
  //  Sub‑domain check – name outside the zone should yield NXDOMAIN
  // ----------------------------------------------------------------------
  @Test
  void findRecords_nameNotInZone_returnsNxDomain() {
    Name external = Name.fromConstantString("outside.example.");
    SetResponse response = zone.findRecords(external, Type.A);
    assertNotNull(response);
    assertEquals(SetResponseType.NXDOMAIN, response.getType());
  }

  // ----------------------------------------------------------------------
  //  Successful exact‑match lookup
  // ----------------------------------------------------------------------
  @Test
  void findRecords_exactMatch_returnsSuccess() {
    SetResponse response = zone.findRecords(aTest.getName(), Type.A);
    assertNotNull(response);
    assertEquals(SetResponseType.SUCCESS, response.getType());

    // The returned RRset should be the one that contains the queried A record.
    RRset rrset = response.getRRset();
    assertNotNull(rrset);
    assertTrue(rrset.containsRR(aTest));
  }

  // ----------------------------------------------------------------------
  //  Wild‑card handling – a query that does not have an exact match should be
  //  satisfied by the *.wild record.
  // ----------------------------------------------------------------------
  @Test
  void findRecords_wildcardMatch_returnsSuccess() throws IOException {
    Name query = Name.fromConstantString("foo.wild.example.");
    SetResponse response = zone.findRecords(query, Type.A);
    assertNotNull(response);
    assertEquals(SetResponseType.SUCCESS, response.getType());

    RRset rrset = response.getRRset();
    assertNotNull(rrset);
    // The RRset must contain the wildcard A record.
    assertTrue(rrset.containsRR(aWild));
  }

  // ----------------------------------------------------------------------
  //  Edge case – Zone with a null origin should propagate NullPointerException
  // ----------------------------------------------------------------------
  @Test
  void findRecords_zoneWithNullOrigin_throwsNullPointerException() throws Exception {
    // Use reflection to set the private 'origin' field to null after construction.
    Field originField = Zone.class.getDeclaredField("origin");
    originField.setAccessible(true);
    originField.set(zone, null);

    // The call to name.subdomain(origin) inside findRecords will now NPE.
    Name query = Name.fromConstantString("sub.example.com.");
    assertThrows(NullPointerException.class, () -> zone.findRecords(query, Type.A));
  }
}
```