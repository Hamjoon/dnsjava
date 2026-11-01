```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#AXFR()}.
 *
 * <p>The tests build a {@link Zone} from an in‑memory array of {@link Record}s (the same
 * records used in {@code ZoneTest}) and verify that the iterator returned by {@code AXFR()}
 * behaves like the regular {@code iterator()} of a {@code Zone}. In addition we check the
 * contract of {@link Iterator#hasNext()}, {@link Iterator#next()} and the expected
 * {@link NoSuchElementException} when the iterator is exhausted.
 */
@ExtendWith(MockitoExtension.class)
class Zone_AXFR_Test {

  /** The zone used by all tests. */
  private static Zone zone;

  /** Records that populate the zone – kept identical to those in {@code ZoneTest}. */
  private static ARecord A_TEST;
  private static AAAARecord AAAA_1_TEST;
  private static AAAARecord AAAA_2_TEST;
  private static ARecord A_WILD;
  private static TXTRecord TXT_WILD;

  /** Build the shared {@link Zone} once for the whole test class. */
  @BeforeAll
  static void setupBeforeAll() {
    try {
      // ----------------------------------------------------------------------
      // 1️⃣ Build the records (mirrors the static block of ZoneTest)
      // ----------------------------------------------------------------------
      Name nameZone = new Name("example.");
      InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
      InetAddress localhost6a = InetAddress.getByName("::1");
      InetAddress localhost6b = InetAddress.getByName("::2");

      A_TEST = new ARecord(new Name("test", nameZone), DClass.IN, 3600, localhost4);
      AAAA_1_TEST = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6a);
      AAAA_2_TEST = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6b);
      A_WILD = new ARecord(new Name("*", nameZone), DClass.IN, 3600, localhost4);
      TXT_WILD = new TXTRecord(new Name("*", nameZone), DClass.IN, 3600, "sometext");

      // ----------------------------------------------------------------------
      // 2️⃣ Assemble the zone
      // ----------------------------------------------------------------------
      Record[] records = new Record[] {
          new SOARecord(
              nameZone,
              DClass.IN,
              3600L,
              Name.fromConstantString("nameserver."),
              new Name("hostmaster", nameZone),
              1,
              21600L,
              7200L,
              2160000L,
              3600L),
          new NSRecord(nameZone, DClass.IN, 300L, Name.fromConstantString("nameserver.")),
          A_TEST,
          AAAA_1_TEST,
          AAAA_2_TEST,
          A_WILD,
          TXT_WILD,
      };
      zone = new Zone(nameZone, records);
    } catch (IOException e) {
      // If the zone cannot be built the whole test suite would be meaningless.
      throw new RuntimeException("Failed to initialise test zone", e);
    }
  }

  @BeforeEach
  void setupBeforeEach() {
    // No per‑test state needed at the moment.
  }

  @AfterEach
  void teardownAfterEach() {
    // No per‑test cleanup required.
  }

  @AfterAll
  static void teardownAfterAll() {
    // The static {@code zone} does not hold external resources, so nothing to clean.
    zone = null;
  }

  /** Helper that consumes an {@link Iterator} into a {@link List}. */
  private static List<RRset> iteratorToList(Iterator<RRset> it) {
    List<RRset> list = new ArrayList<>();
    while (it.hasNext()) {
      list.add(it.next());
    }
    return list;
  }

  /** Verify that the AXFR iterator returns the same RRsets as the standard iterator. */
  @Test
  void axfrIteratorMatchesStandardIterator() {
    Iterator<RRset> axfrIt = zone.AXFR();          // method under test
    Iterator<RRset> normalIt = zone.iterator();   // reference iterator

    List<RRset> axfrList = iteratorToList(axfrIt);
    List<RRset> normalList = iteratorToList(normalIt);

    // The two lists should be equal in size and order.
    assertEquals(normalList.size(), axfrList.size(),
        "AXFR iterator returned a different number of RRsets");
    assertEquals(normalList, axfrList,
        "AXFR iterator returned RRsets in a different order or with different content");
  }

  /** Ensure that the AXFR iterator follows the {@link Iterator} contract. */
  @Test
  void axfrIteratorContract() {
    Iterator<RRset> it = zone.AXFR();

    // The iterator must initially have a next element (the SOA record is always present)
    assertTrue(it.hasNext(), "AXFR iterator should have at least one element");

    // Walk through all elements and count them.
    int count = 0;
    while (it.hasNext()) {
      RRset rs = it.next();
      assertNotNull(rs, "RRset returned by AXFR iterator must not be null");
      count++;
    }

    // After exhaustion, hasNext must be false.
    assertFalse(it.hasNext(), "AXFR iterator should report no more elements after exhaustion");

    // Calling next() now must throw NoSuchElementException.
    assertThrows(java.util.NoSuchElementException.class, it::next,
        "Calling next() on an exhausted AXFR iterator must throw NoSuchElementException");

    // The count should match the total number of RRsets in the zone.
    int expected = iteratorToList(zone.iterator()).size();
    assertEquals(expected, count,
        "AXFR iterator did not iterate over the expected number of RRsets");
  }
}
```