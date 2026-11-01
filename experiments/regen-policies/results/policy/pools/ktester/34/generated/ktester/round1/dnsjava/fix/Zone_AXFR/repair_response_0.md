```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

/**
 * Tests for the AXFR iterator functionality of {@link Zone}.
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

    private SOARecord SOA;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a simple zone that contains an SOA, two NS records and a few other records.
        ZONE_NAME = Name.fromConstantString("example.");
        SOA1 = new SOARecord(ZONE_NAME, DClass.IN, 3600L,
                Name.fromConstantString("nameserver."),
                new Name("hostadmin", ZONE_NAME),
                1, 21600L, 7200L, 2160000L, 3600L);
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("nameserver1."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("nameserver2."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");
        A_UNIQUE = new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600,
                InetAddress.getByName("127.0.0.3"));
        A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
        A_WILD = new ARecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600,
                InetAddress.getByName("127.0.0.2"));
        TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");
        Record[] zoneRecords = new Record[]{
                SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD
        };
        ZONE = new Zone(ZONE_NAME, zoneRecords);
        ZONE_NAME = Name.fromConstantString("example.");
        // Minimal required records for a valid zone.
        SOA = new SOARecord(ZONE_NAME, DClass.IN, 3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostmaster", ZONE_NAME),
                1, 7200L, 3600L, 1209600L, 3600L);
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // Start each test with a zone that only contains the mandatory SOA and two NS records.
        zone = new Zone(ZONE_NAME, new Record[]{SOA, NS1, NS2});
    }

    /**
     * The AXFR iterator should return the same RRsets as the normal iterator (in the same order).
     */
    @Test
    void axfrIteratorReturnsAllRRsetsInOrder() {
        Iterator<RRset> axfrIter = ZONE.AXFR();
        assertNotNull(axfrIter, "AXFR iterator must not be null");
        // Collect the RRsets produced by AXFR.
        List<RRset> axfrList = Stream.generate(() -> {
            if (axfrIter.hasNext()) {
                return axfrIter.next();
            }
            return null;
        }).takeWhile(Objects::nonNull).collect(Collectors.toList());

        // Collect the RRsets produced by the regular iterator for comparison.
        List<RRset> normalList = Stream.generate(() -> {
            Iterator<RRset> it = ZONE.iterator();
            if (it.hasNext()) {
                return it.next();
            }
            return null;
        }).takeWhile(Objects::nonNull).collect(Collectors.toList());

        assertEquals(normalList, axfrList,
                "AXFR iterator must produce the same sequence of RRsets as the regular iterator");
    }

    /**
     * The AXFR iterator must not support element removal.
     */
    @Test
    void axfrIteratorRemoveIsUnsupported() {
        Iterator<RRset> axfrIter = ZONE.AXFR();
        assertTrue(axfrIter.hasNext(), "Iterator should have at least the SOA element");
        // consume first element (SOA)
        axfrIter.next();
        assertThrows(UnsupportedOperationException.class, axfrIter::remove,
                "Calling remove() on an AXFR iterator should throw UnsupportedOperationException");
    }

    /**
     * After the last element, calling next() must throw NoSuchElementException.
     */
    @Test
    void axfrIteratorNextAfterLastFails() {
        Iterator<RRset> axfrIter = ZONE.AXFR();
        while (axfrIter.hasNext()) {
            axfrIter.next();
        }
        assertThrows(NoSuchElementException.class, axfrIter::next,
                "Calling next() after the iterator is exhausted must throw NoSuchElementException");
    }

    /**
     * AXFR on an empty zone (no additional RRsets beyond SOA/NS).
     */
    @Test
    void axfrOnEmptyZone() {
        Iterator<RRset> it = zone.AXFR();
        // The iterator must contain the SOA at the start, the two NS RRsets, and the SOA again at
        // the end – total of 4 RRsets.
        List<RRset> collected = Stream.generate(() -> {
            if (it.hasNext()) {
                return it.next();
            }
            return null;
        }).takeWhile(Objects::nonNull).collect(Collectors.toList());
        assertEquals(4, collected.size(), "Empty zone should yield 4 RRsets (SOA, NS, NS, SOA)");
        assertTrue(collected.get(0).getName().equals(SOA.getName()), "First RRset must be SOA");
        assertTrue(collected.get(collected.size() - 1).getName().equals(SOA.getName()),
                "Last RRset must be SOA (AXFR duplicate)");
        assertThrows(NoSuchElementException.class, it::next,
                "Calling next() after iterator exhaustion must throw NoSuchElementException");
    }

    /**
     * AXFR with a single additional RRset (an A record).
     */
    @Test
    void axfrWithSingleRRset() throws IOException {
        // Add a single A record.
        ARecord aRecord = new ARecord(new Name("www", ZONE_NAME), DClass.IN, 3600,
                InetAddress.getByName("192.0.2.1"));
        zone.addRecord(aRecord);
        Iterator<RRset> it = zone.AXFR();
        List<RRset> rrsets = Stream.generate(() -> {
            if (it.hasNext()) {
                return it.next();
            }
            return null;
        }).takeWhile(Objects::nonNull).collect(Collectors.toList());

        // Expected order: SOA, NS1, NS2, A, SOA (duplicate at end) => 5 RRsets.
        assertEquals(5, rrsets.size(), "Zone with one extra RRset should yield 5 RRsets");
        assertEquals(SOA.getName(), rrsets.get(0).getName(), "First element must be SOA");
        assertEquals(aRecord.getName(), rrsets.get(3).getName(), "Fourth element must be the A record");
        assertEquals(SOA.getName(), rrsets.get(4).getName(), "Last element must be the closing SOA");
    }

    /**
     * AXFR with multiple RRsets and verification of lexical ordering by name and type.
     */
    @Test
    void axfrWithMultipleRRsetsOrdering() throws IOException {
        // Records with differing names and types.
        ARecord aB = new ARecord(new Name("b", ZONE_NAME), DClass.IN, 3600,
                InetAddress.getByName("192.0.2.2"));
        AAAARecord aAAA = new AAAARecord(new Name("a", ZONE_NAME), DClass.IN, 3600,
                InetAddress.getByName("2001:db8::1"));
        MXRecord mx = new MXRecord(new Name("a", ZONE_NAME), DClass.IN, 3600,
                10, Name.fromConstantString("mail.example."));
        NSRecord nsExtra = new NSRecord(new Name("c", ZONE_NAME), DClass.IN, 3600,
                Name.fromConstantString("ns3.example."));

        // Add them in a random order to ensure iterator sorting is exercised.
        zone.addRecord(aB);
        zone.addRecord(mx);
        zone.addRecord(aAAA);
        zone.addRecord(nsExtra);

        Iterator<RRset> it = zone.AXFR();
        List<RRset> rrsets = new ArrayList<>();
        while (it.hasNext()) {
            rrsets.add(it.next());
        }

        // Expected order (lexicographic name, then numeric type):
        // 1. SOA (example.)
        // 2. NS1 (example.)
        // 3. NS2 (example.)
        // 4. NS extra (c.example.)
        // 5. MX a.example.   (type 15)
        // 6. AAAA a.example. (type 28)
        // 7. A b.example.    (type 1)
        // 8. SOA again (closing)

        List<String> expected = List.of(
                "SOA", // apex SOA
                "NS",  // NS1
                "NS",  // NS2
                "NS",  // nsExtra (c.example.)
                "MX",  // mx (a.example.)
                "AAAA",// aAAA (a.example.)
                "A",   // aB (b.example.)
                "SOA"  // closing SOA
        );

        List<String> actual = rrsets.stream()
                .map(rr -> {
                    int t = rr.getType();
                    if (t == Type.SOA) return "SOA";
                    if (t == Type.NS) return "NS";
                    if (t == Type.MX) return "MX";
                    if (t == Type.AAAA) return "AAAA";
                    if (t == Type.A) return "A";
                    return "OTHER";
                })
                .collect(Collectors.toList());

        assertEquals(expected, actual, "AXFR iterator must follow lexical name then type order");
    }

    /**
     * Two consecutive AXFR calls must return independent iterator instances.
     */
    @Test
    void axfrIteratorIndependence() {
        Iterator<RRset> it1 = zone.AXFR();
        Iterator<RRset> it2 = zone.AXFR();
        assertNotSame(it1, it2, "Two calls to AXFR must return distinct iterator objects");

        // Advance the first iterator once.
        assertTrue(it1.hasNext());
        RRset firstFromIt1 = it1.next();

        // The second iterator must still be at the beginning (its first element equals the first
        // element of it1 before we advanced it).
        assertTrue(it2.hasNext());
        RRset firstFromIt2 = it2.next();

        assertEquals(firstFromIt1, firstFromIt2,
                "Both iterators should start with the same first RRset");
    }

    /**
     * Verify runtime type of the iterator and that remove() is unsupported.
     */
    @Test
    void axfrIteratorTypeAndUnsupportedRemove() {
        Iterator<RRset> it = zone.AXFR();
        // The concrete class is a private inner class named ZoneIterator.
        String className = it.getClass().getSimpleName();
        assertTrue(className.contains("ZoneIterator"),
                "AXFR iterator should be an instance of Zone.ZoneIterator");
        assertThrows(UnsupportedOperationException.class, it::remove,
                "remove() on AXFR iterator must throw UnsupportedOperationException");
    }

    /**
     * ConcurrentModificationException should be thrown if the zone is modified after iterator
     * creation.
     */
    @Test
    void axfrIteratorDetectsConcurrentModification() throws IOException {
        Iterator<RRset> it = zone.AXFR();
        // Modify the zone after the iterator has been obtained.
        ARecord newRec = new ARecord(new Name("new", ZONE_NAME), DClass.IN, 3600,
                InetAddress.getByName("192.0.2.99"));
        zone.addRecord(newRec);
        // The next call to the iterator is expected to notice the structural change.
        assertThrows(ConcurrentModificationException.class, it::next,
                "Iterator should throw ConcurrentModificationException after zone modification");
    }

    /**
     * Inserting a null RRset into the internal storage should cause a NullPointerException during
     * iteration.
     */
    @Test
    void axfrIteratorThrowsNullPointerWhenNullRRsetPresent() throws Exception {
        // Use reflection to obtain the internal map that stores RRsets.
        Field mapField = Zone.class.getDeclaredField("rrsets");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentSkipListMap<Name, List<RRset>> map =
                (ConcurrentSkipListMap<Name, List<RRset>>) mapField.get(zone);
        // Insert a null entry for a synthetic name.
        Name nullName = Name.fromConstantString("null.example.");
        // deliberately corrupt internal state
        map.put(nullName, null);
        Iterator<RRset> it = zone.AXFR();
        // Traversal should encounter the null entry and raise NPE.
        assertThrows(NullPointerException.class, it::next,
                "Iterating over a zone containing a null RRset entry must throw NullPointerException");
    }

    /**
     * After the iterator has been exhausted, a further call to {@code next()} should raise an
     * {@link IllegalStateException}. (The implementation may also throw {@link
     * NoSuchElementException}; the test accepts either.)
     */
    @Test
    void axfrIteratorIllegalStateAfterExhaustion() {
        Iterator<RRset> it = zone.AXFR();
        // Exhaust the iterator.
        while (it.hasNext()) {
            it.next();
        }
        // The next call should indicate an illegal state.
        try {
            it.next();
            fail("Expected either IllegalStateException or NoSuchElementException after exhaustion");
        } catch (IllegalStateException | NoSuchElementException expected) {
            // Test passes.
        }
    }
}
```