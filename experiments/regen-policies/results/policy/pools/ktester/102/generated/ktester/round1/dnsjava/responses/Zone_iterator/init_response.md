```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#iterator()}.
 *
 * <p>The tests mirror the behaviour verified in {@code ZoneTest} but are isolated in this
 * dedicated class. They cover normal iteration, removal of records via the iterator, and the
 * expected exceptions when the iterator contract is violated.</p>
 */
@ExtendWith(MockitoExtension.class)
class Zone_iterator_Test {

  /** Shared immutable objects used to build a {@link Zone} for each test. */
  private static Name ZONE_NAME;
  private static SOARecord SOA1;
  private static NSRecord NS1;
  private static NSRecord NS2;
  private static ARecord A_TEST;

  /** Fresh instance for every test method. */
  private Zone zone;

  @BeforeAll
  static void setupBeforeAll() throws IOException {
    // origin of the zone
    ZONE_NAME = Name.fromConstantString("example.");

    // minimal required records: SOA + two NS + one A
    SOA1 = new SOARecord(
        ZONE_NAME,
        DClass.IN,
        3600L,
        Name.fromConstantString("ns.example."),
        new Name("hostmaster", ZONE_NAME),
        1,
        21600L,
        7200L,
        2160000L,
        3600L);

    NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
    NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

    InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
    A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
  }

  @BeforeEach
  void setupBeforeEach() throws IOException {
    // Build a fresh Zone for each test – records are immutable, so we can reuse the static ones.
    Record[] records = new Record[] { SOA1, NS1, NS2, A_TEST };
    zone = new Zone(ZONE_NAME, records);
  }

  @AfterEach
  void teardownAfterEach() {
    // No external resources to release; method present for symmetry with the template.
    zone = null;
  }

  @AfterAll
  static void teardownAfterAll() {
    // Clear static references – not strictly required but keeps the test class tidy.
    ZONE_NAME = null;
    SOA1 = null;
    NS1 = null;
    NS2 = null;
    A_TEST = null;
  }

  /** Verify that calling {@code remove()} before any {@code next()} throws {@link IllegalStateException}. */
  @Test
  void iteratorRemoveBeforeNextFails() {
    Iterator<RRset> it = zone.iterator();
    IllegalStateException ex = assertThrows(IllegalStateException.class, it::remove);
    assertTrue(ex.getMessage().contains("Not at"));
  }

  /** Removing a record via the iterator should delete it from the underlying zone. */
  @Test
  void iteratorRemoveASucceeds() {
    Iterator<RRset> it = zone.iterator();

    // 0: SOA, 1: NS, 2: NS, 3: A (our test record)
    it.next(); // SOA
    it.next(); // NS1
    it.next(); // NS2
    it.next(); // A_TEST
    it.remove(); // remove the A record

    // After removal the zone must no longer contain the A record.
    assertNull(zone.findExactMatch(A_TEST.getName(), Type.A),
        "A record should have been removed by iterator.remove()");
  }

  /** After iterating to the end, {@code next()} must throw {@link NoSuchElementException}. */
  @Test
  void iteratorNextAfterLastFails() {
    Iterator<RRset> it = zone.iterator();

    // Exhaust the iterator
    while (it.hasNext()) {
      it.next();
    }

    NoSuchElementException ex = assertThrows(NoSuchElementException.class, it::next);
    assertTrue(ex.getMessage().contains("No more elements"));
  }

  /** Normal iteration should return all RRsets in insertion order. */
  @Test
  void iteratorIteratesAllElements() {
    Iterator<RRset> it = zone.iterator();

    // Expected order: SOA, NS1, NS2, A_TEST
    RRset rrset0 = it.next();
    assertEquals(SOA1, rrset0.first());

    RRset rrset1 = it.next();
    assertEquals(NS1, rrset1.first());

    RRset rrset2 = it.next();
    assertEquals(NS2, rrset2.first());

    RRset rrset3 = it.next();
    assertEquals(A_TEST, rrset3.first());

    assertFalse(it.hasNext(), "Iterator should be exhausted after all elements are read");
  }
}
```