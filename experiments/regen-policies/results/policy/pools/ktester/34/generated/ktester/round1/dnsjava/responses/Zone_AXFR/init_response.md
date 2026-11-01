```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 * <p>The tests verify that the iterator returned by {@code AXFR()}:
 *
 * <ul>
 *   <li>is not {@code null};
 *   <li>produces the same sequence of {@link RRset}s as the normal {@link Zone#iterator()}
 *       (SOA, NS, then the remaining RRsets);
 *   <li>does not support {@code remove()};
 *   <li>throws {@link NoSuchElementException} when {@code next()} is called after the last
 *       element.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Zone_AXFR_Test {

  private Name ZONE_NAME;
  private SOARecord SOA1;
  private NSRecord NS1;
  private NSRecord NS2;
  private ARecord A_UNIQUE;
  private ARecord A_TEST;
  private AAAARecord AAAA_1_TEST;
  private AAAARecord AAAA_2_TEST;
  private ARecord A_WILD;
  private TXTRecord TXT_WILD;
  private Zone ZONE;

  @BeforeAll
  static void setupBeforeAll() {
    // No global resources needed for these tests.
  }

  @BeforeEach
  void setupBeforeEach() throws IOException {
    // Build a simple zone that contains an SOA, two NS records and a few other records.
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

    NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("nameserver1."));
    NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("nameserver2."));

    InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
    InetAddress localhost6a = InetAddress.getByName("::1");
    InetAddress localhost6b = InetAddress.getByName("::2");

    A_UNIQUE =
        new ARecord(
            new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
    A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
    AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
    AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
    A_WILD =
        new ARecord(
            new Name("*.wild", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
    TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");

    Record[] zoneRecords =
        new Record[] {
          SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD,
        };
    ZONE = new Zone(ZONE_NAME, zoneRecords);
  }

  @AfterEach
  void teardownAfterEach() {
    // No per‑test cleanup required.
  }

  @AfterAll
  static void teardownAfterAll() {
    // No global cleanup required.
  }

  /** The AXFR iterator should return the same RRsets as the normal iterator (in the same order). */
  @Test
  void axfrIteratorReturnsAllRRsetsInOrder() {
    Iterator<RRset> axfrIter = ZONE.AXFR();
    assertNotNull(axfrIter, "AXFR iterator must not be null");

    // Collect the RRsets produced by AXFR.
    List<RRset> axfrList =
        Stream.generate(
                () -> {
                  if (axfrIter.hasNext()) {
                    return axfrIter.next();
                  }
                  return null;
                })
            .takeWhile(r -> r != null)
            .collect(Collectors.toList());

    // Collect the RRsets produced by the regular iterator for comparison.
    List<RRset> normalList =
        Stream.generate(
                () -> {
                  Iterator<RRset> it = ZONE.iterator();
                  if (it.hasNext()) {
                    return it.next();
                  }
                  return null;
                })
            .takeWhile(r -> r != null)
            .collect(Collectors.toList());

    assertEquals(
        normalList,
        axfrList,
        "AXFR iterator must produce the same sequence of RRsets as the regular iterator");
  }

  /** The AXFR iterator must not support element removal. */
  @Test
  void axfrIteratorRemoveIsUnsupported() {
    Iterator<RRset> axfrIter = ZONE.AXFR();
    assertTrue(axfrIter.hasNext(), "Iterator should have at least the SOA element");
    axfrIter.next(); // consume first element (SOA)

    assertThrows(
        UnsupportedOperationException.class,
        axfrIter::remove,
        "Calling remove() on an AXFR iterator should throw UnsupportedOperationException");
  }

  /** After the last element, calling next() must throw NoSuchElementException. */
  @Test
  void axfrIteratorNextAfterLastFails() {
    Iterator<RRset> axfrIter = ZONE.AXFR();
    while (axfrIter.hasNext()) {
      axfrIter.next();
    }

    assertThrows(
        NoSuchElementException.class,
        axfrIter::next,
        "Calling next() after the iterator is exhausted must throw NoSuchElementException");
  }
}
```