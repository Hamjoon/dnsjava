```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
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
 *   <li>Edge‑cases such as a {@code null} internal NS, an empty NS set and concurrent access are
 *       exercised.
 * </ul>
 *
 * <p>All other behaviour of {@link Zone} (parsing, validation, etc.) is exercised in the existing
 * {@code ZoneTest} class – this class focuses exclusively on {@code getNS()}.
 */
@ExtendWith(MockitoExtension.class)
class Zone_getNS_Test {

  private static final long TTL = 300L;
  private static Name zoneName;
  private static NSRecord ns1;
  private static NSRecord ns2;
  private static NSRecord ns3;
  private static NSRecord nsExtra;
  private Zone zone;

  /** Initialise static resources that are shared across all test methods. */
  @BeforeAll
  static void setupBeforeAll() throws IOException {
    // zone name – a fully‑qualified domain name
    zoneName = Name.fromConstantString("example.");

    // three NS records that will be part of the zone
    ns1 = new NSRecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns1.example."));
    ns2 = new NSRecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns2.example."));
    ns3 = new NSRecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns3.example."));

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

  /* -------------------------------------------------------------------------
   *  Basic functionality already covered by the original test suite
   * ----------------------------------------------------------------------- */

  /** The NS {@link RRset} returned by {@code getNS()} must contain exactly the NS records supplied. */
  @Test
  void getNS_containsAllNsRecords() {
    RRset nsSet = zone.getNS();
    assertThat(nsSet).isNotNull();
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
    assertThat(afterMutation.contains(nsExtra)).isFalse();
  }

  /**
   * The {@code getNS()} method should never return {@code null} even when the zone contains a
   * single NS record.
   */
  @Test
  void getNS_singleNsRecordNeverNull() throws IOException {
    Zone singleNsZone = new Zone(zoneName, new Record[] {ns1});
    RRset nsSet = singleNsZone.getNS();

    assertThat(nsSet).isNotNull();
    assertThat(nsSet).isEqualTo(new RRset(ns1));
  }

  /**
   * Verify that the {@link RRset} returned by {@code getNS()} is immutable with respect to the
   * zone – attempts to remove a record via the iterator's {@code remove()} must not affect
   * the zone's NS set.
   */
  @Test
  void getNS_iteratorRemoveDoesNotAffectZone() {
    RRset nsSet = zone.getNS();
    var it = nsSet.iterator();
    assertTrue(it.hasNext());
    it.next(); // advance to first NS
    it.remove(); // remove from the copy

    // Zone's NS set should still contain both original records
    RRset after = zone.getNS();
    assertThat(after).isEqualTo(new RRset(ns1, ns2));
  }

  /* -------------------------------------------------------------------------
   *  Additional test cases derived from the JSON specification
   * ----------------------------------------------------------------------- */

  /**
   * When the internal {@code NS} field is {@code null}, {@code getNS()} should propagate the
   * {@link NullPointerException} that occurs inside the {@link RRset} constructor.
   */
  @Test
  void getNS_internalNsNull_throwsNPE() throws Exception {
    // Use reflection to set the private field to null
    Field nsField = Zone.class.getDeclaredField("NS");
    nsField.setAccessible(true);
    nsField.set(zone, null);

    // The method is expected to throw NPE because new RRset(null) is illegal
    assertThrows(NullPointerException.class, zone::getNS);
  }

  /**
   * When the internal {@code NS} is an empty {@link RRset}, {@code getNS()} must return a new
   * empty {@link RRset} (size 0).
   */
  @Test
  void getNS_internalNsEmpty_returnsEmptySet() throws Exception {
    // Replace the internal NS with an empty RRset
    Field nsField = Zone.class.getDeclaredField("NS");
    nsField.setAccessible(true);
    nsField.set(zone, new RRset()); // empty set

    RRset result = zone.getNS();
    assertThat(result).isNotNull();
    assertThat(result.size()).isZero();
  }

  /**
   * Snapshot semantics – a copy obtained before the internal NS is mutated must remain unchanged
   * after the mutation.
   */
  @Test
  void getNS_snapshotSemantics_afterInternalMutation() throws Exception {
    // Obtain a snapshot before mutation
    RRset snapshot = zone.getNS();

    // Mutate the internal NS directly (add a third NS record)
    Field nsField = Zone.class.getDeclaredField("NS");
    nsField.setAccessible(true);
    RRset internal = (RRset) nsField.get(zone);
    internal.addRRset(new RRset(ns3));

    // The previously obtained snapshot must still contain only the original two records
    RRset expected = new RRset(ns1, ns2);
    assertThat(snapshot).isEqualTo(expected);
    assertThat(snapshot).isNotEqualTo(internal);
  }

  /**
   * Verify that when multiple NS records are present (three in this case) the returned RRset
   * contains *all* of them and is a distinct object.
   */
  @Test
  void getNS_multipleNsRecords_allCopied() throws Exception {
    // Build a zone with three NS records via reflection (add ns3)
    Field nsField = Zone.class.getDeclaredField("NS");
    nsField.setAccessible(true);
    RRset internal = (RRset) nsField.get(zone);
    internal.addRRset(new RRset(ns3)); // now internal contains ns1, ns2, ns3

    RRset result = zone.getNS();

    // Verify content
    RRset expected = new RRset(ns1, ns2, ns3);
    assertThat(result).isEqualTo(expected);

    // Verify distinct instance
    assertNotSame(result, internal);
  }

  /**
   * Concurrent access – spawn several threads that call {@code getNS()} simultaneously.
   * All invocations must complete without exceptions and each must return an equal (but distinct)
   * {@link RRset}.
   */
  @Test
  void getNS_concurrentAccess_returnsValidCopies() throws Exception {
    int threadCount = 10;
    ExecutorService exec = Executors.newFixedThreadPool(threadCount);
    List<Future<RRset>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(exec.submit(() -> zone.getNS()));
    }

    // Gather results
    List<RRset> results = new ArrayList<>();
    for (Future<RRset> f : futures) {
      RRset rs = f.get(2, TimeUnit.SECONDS); // will throw if something went wrong
      assertThat(rs).isNotNull();
      results.add(rs);
    }

    exec.shutdownNow();

    // All results must be equal in content to the expected set
    RRset expected = new RRset(ns1, ns2);
    for (RRset rs : results) {
      assertThat(rs).isEqualTo(expected);
    }

    // Ensure that each result is a distinct object (identity check)
    for (int i = 0; i < results.size(); i++) {
      for (int j = i + 1; j < results.size(); j++) {
        assertNotSame(results.get(i), results.get(j));
      }
    }
  }

  /**
   * Removing a {@link Record} from the {@link RRset} returned by {@code getNS()} must not affect the
   * internal NS stored in the {@link Zone}.
   */
  @Test
  void getNS_removeRecordFromReturnedSetDoesNotAffectZone() {
    RRset copy = zone.getNS();
    // Remove one of the records from the copy
    copy.removeRRset(new RRset(ns1));

    // The zone's internal NS should still contain both original records
    RRset after = zone.getNS();
    assertThat(after).isEqualTo(new RRset(ns1, ns2));
  }
}
```