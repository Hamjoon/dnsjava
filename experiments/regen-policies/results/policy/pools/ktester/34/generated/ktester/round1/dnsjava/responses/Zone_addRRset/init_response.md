```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Zone#addRRset(RRset)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>validation of null arguments,
 *   <li>SOA specific validation (owner name, size),
 *   <li>sub‑domain validation,
 *   <li>successful addition of a regular RRset,
 *   <li>successful replacement of the zone SOA.
 * </ul>
 */
class Zone_addRRset_Test {

  private static final String ZONE = "example.";
  private Name zoneName;
  private SOARecord soaRecord;
  private NSRecord nsRecord;
  private Zone zone;

  @BeforeAll
  static void setupBeforeAll() {
    // No global resources required.
  }

  @BeforeEach
  void setupBeforeEach() throws IOException {
    // Create a minimal valid zone (origin + SOA + at least one NS)
    zoneName = Name.fromConstantString(ZONE);
    soaRecord =
        new SOARecord(
            zoneName,
            DClass.IN,
            3600L,
            Name.fromConstantString("ns.example."),
            new Name("hostmaster", zoneName),
            1,
            10800L,
            3600L,
            604800L,
            3600L);
    nsRecord = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns.example."));

    // Initialise the zone with the mandatory records.
    zone = new Zone(zoneName, new Record[] {soaRecord, nsRecord});
  }

  @AfterEach
  void teardownAfterEach() {
    // No per‑test cleanup required.
  }

  @AfterAll
  static void teardownAfterAll() {
    // No global cleanup required.
  }

  @Test
  void addRRset_NullArgument_Throws() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> zone.addRRset(null),
            "addRRset(null) should throw IllegalArgumentException");
    assertTrue(ex.getMessage().contains("rrset must not be null"));
  }

  @Test
  void addRRset_SOAOwnerMismatch_Throws() throws IOException {
    // SOA with a different owner name (not the zone origin)
    Name otherName = Name.fromConstantString("other.example.");
    SOARecord otherSOA =
        new SOARecord(
            otherName,
            DClass.IN,
            3600L,
            Name.fromConstantString("ns.example."),
            new Name("hostmaster", zoneName),
            1,
            10800L,
            3600L,
            604800L,
            3600L);
    RRset rrset = new RRset(otherSOA);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> zone.addRRset(rrset),
            "SOA owner not matching zone origin must throw");
    assertTrue(
        ex.getMessage().contains("SOA owner"),
        "Exception message should mention SOA owner mismatch");
  }

  @Test
  void addRRset_SOAInvalidSize_Throws() throws IOException {
    // Create two SOA records with the same origin – this violates the “exactly 1 SOA” rule.
    SOARecord soa2 =
        new SOARecord(
            zoneName,
            DClass.IN,
            3600L,
            Name.fromConstantString("ns.example."),
            new Name("hostmaster2", zoneName),
            2,
            10800L,
            3600L,
            604800L,
            3600L);
    RRset rrset = new RRset(soaRecord, soa2);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> zone.addRRset(rrset),
            "Adding an RRset with more than one SOA should fail");
    assertTrue(
        ex.getMessage().contains("exactly 1 SOA"),
        "Exception message should mention the exact‑one‑SOA requirement");
  }

  @Test
  void addRRset_NameNotSubdomain_Throws() throws IOException {
    // Record whose name is not a sub‑domain of the zone origin.
    Name externalName = Name.fromConstantString("outside.example.com.");
    ARecord aRecord = new ARecord(externalName, DClass.IN, 300L, InetAddress.getByName("1.2.3.4"));
    RRset rrset = new RRset(aRecord);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> zone.addRRset(rrset),
            "Adding a record outside the zone should throw");
    assertTrue(
        ex.getMessage().contains("is absolute and not a subdomain"),
        "Exception message should mention sub‑domain validation");
  }

  @Test
  void addRRset_RegularRecord_Succeeds() throws IOException {
    // Add a new A record under the zone.
    Name aName = new Name("newhost", zoneName);
    ARecord aRecord = new ARecord(aName, DClass.IN, 300L, InetAddress.getByName("10.0.0.1"));
    RRset rrset = new RRset(aRecord);

    assertDoesNotThrow(() -> zone.addRRset(rrset), "addRRset should succeed for a valid RRset");

    // Verify the record can be retrieved.
    RRset fetched = zone.findExactMatch(aName, Type.A);
    assertNotNull(fetched, "The added RRset should be findable");
    assertEquals(1, fetched.size(), "Exactly one record should be stored");
    assertEquals(aRecord, fetched.first(), "Stored record must equal the added one");
  }

  @Test
  void addRRset_NewSOA_UpdatesZoneSOA() throws IOException {
    // Build a brand‑new SOA record (same owner as the zone origin).
    SOARecord newSOA =
        new SOARecord(
            zoneName,
            DClass.IN,
            3600L,
            Name.fromConstantString("ns2.example."),
            new Name("hostmaster", zoneName),
            42,
            10800L,
            3600L,
            604800L,
            3600L);
    RRset rrset = new RRset(newSOA);

    // Adding the new SOA should replace the internal SOA reference.
    assertDoesNotThrow(() -> zone.addRRset(rrset), "Adding a valid SOA RRset should not throw");
    assertEquals(newSOA, zone.getSOA(), "Zone SOA should be updated to the newly added record");
  }
}
```