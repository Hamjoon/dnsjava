```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Zone#getNS()}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>The method returns an {@link RRset} that contains all NS records supplied to the zone.
 *   <li>A fresh copy is returned on each call (no shared mutable state with the internal NS set).
 *   <li>Modifying the returned {@link RRset} does not affect the zone's internal NS records.
 * </ul>
 *
 * <p>All other behaviour of {@link Zone} (parsing, validation, etc.) is exercised in the existing
 * {@code ZoneTest} class – this class focuses solely on the contract of {@code getNS()}.
 */
@ExtendWith(MockitoExtension.class)
class Zone_getNS_Test {

  private static final long TTL = 300L;
  private static Name zoneName;
  private static NSRecord ns1;
  private static NSRecord ns2;
  private static NSRecord nsExtra;
  private Zone zone;

  /** Initialise static resources that are shared across all test methods. */
  @BeforeAll
  static void setupBeforeAll() throws IOException {
    // zone name – a fully‑qualified domain name
    zoneName = Name.fromConstantString("example.");

    // two NS records that will be part of the zone
    ns1 = new NSRecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns1.example."));
    ns2 = new NSRecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns2.example."));

    // an extra NS record used only for mutation tests
    nsExtra = new NSRecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns-extra.example."));
  }

  /** Build a fresh {@link Zone} instance before each test. */
  @BeforeEach
  void setupBeforeEach() throws IOException {
    // The constructor that accepts a var‑args array of {@link Record}s is convenient for tests.
    Record[] records = new Record[] {ns1, ns2};
    zone = new Zone(zoneName, records);
  }

  /** No resources need explicit cleanup for these tests. */
  @AfterEach
  void teardownAfterEach() {
    // nothing to clean up
  }

  @AfterAll
  static void teardownAfterAll() {
    // nothing to clean up
  }

  /** The NS {@link RRset} returned by {@code getNS()} must contain exactly the NS records supplied. */
  @Test
  void getNS_containsAllNsRecords() {
    RRset nsSet = zone.getNS();
    assertThat(nsSet).isNotNull();
    // The RRset constructor with var‑args creates an equivalent set.
    RRset expected = new RRset(ns1, ns2);
    assertThat(nsSet).isEqualTo(expected);
  }

  /** Each call to {@code getNS()} must return a new {@link RRset} instance (no shared mutable state). */
  @Test
  void getNS_returnsNewInstanceOnEachCall() {
    RRset first = zone.getNS();
    RRset second = zone.getNS();

    // Different object references
    assertNotSame(first, second);

    // But they are equal in content
    assertEquals(first, second);
  }

  /**
   * Modifying the {@link RRset} returned by {@code getNS()} must not affect the internal NS set of
   * the {@link Zone}. The zone should continue to return the original NS records.
   */
  @Test
  void getNS_modifyingReturnedSetDoesNotAlterZoneState() {
    // Obtain a copy and mutate it
    RRset mutableCopy = zone.getNS();
    mutableCopy.addRRset(new RRset(nsExtra));

    // The zone's own NS set must remain unchanged
    RRset afterMutation = zone.getNS();
    RRset expected = new RRset(ns1, ns2); // original records only
    assertThat(afterMutation).isEqualTo(expected);

    // Ensure the extra record is **not** present
    assertThat(afterMutation.contains(nsExtra)).isFalse();
  }

  /**
   * The {@code getNS()} method should never return {@code null} even when the zone contains a
   * single NS record.
   */
  @Test
  void getNS_singleNsRecordNeverNull() throws IOException {
    // Build a zone with only one NS record
    Zone singleNsZone = new Zone(zoneName, new Record[] {ns1});
    RRset nsSet = singleNsZone.getNS();

    assertThat(nsSet).isNotNull();
    assertThat(nsSet).isEqualTo(new RRset(ns1));
  }

  /**
   * Verify that the {@link RRset} returned by {@code getNS()} is immutable with respect to the
   * zone – attempts to remove a record via the iterator's {@code remove()} must not affect the
   * zone's NS set.
   */
  @Test
  void getNS_iteratorRemoveDoesNotAffectZone() {
    RRset nsSet = zone.getNS();
    // The iterator of RRset supports remove, but it should operate only on the copy.
    var it = nsSet.iterator();
    assertTrue(it.hasNext());
    it.next(); // advance to first NS
    it.remove(); // remove from the copy

    // Zone's NS set should still contain both original records
    RRset after = zone.getNS();
    assertThat(after).isEqualTo(new RRset(ns1, ns2));
  }
}
```