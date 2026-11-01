package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.InetAddress;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class Zone_iterator_Test {

    /**
     * Zone used for all iterator tests.
     */
    private static Zone zone;

    /**
     * Records that are part of the test zone – kept for readability only.
     */
    private static ARecord A_TEST;

    private static AAAARecord AAAA_1_TEST;

    private static AAAARecord AAAA_2_TEST;

    private static ARecord A_WILD;

    private static TXTRecord TXT_WILD;

    @BeforeAll
    static void setupBeforeAll() {
        try {
            // ----- build the same zone that the existing ZoneTest uses -----
            Name nameZone = new Name("example.");
            InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
            InetAddress localhost6a = InetAddress.getByName("::1");
            InetAddress localhost6b = InetAddress.getByName("::2");
            A_TEST = new ARecord(new Name("test", nameZone), DClass.IN, 3600, localhost4);
            AAAA_1_TEST = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6a);
            AAAA_2_TEST = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6b);
            A_WILD = new ARecord(new Name("*", nameZone), DClass.IN, 3600, localhost4);
            TXT_WILD = new TXTRecord(new Name("*", nameZone), DClass.IN, 3600, "sometext");
            Record[] records = new Record[] { new SOARecord(nameZone, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostmaster", nameZone), 1, 21600L, 7200L, 2160000L, 3600L), new NSRecord(nameZone, DClass.IN, 300L, Name.fromConstantString("nameserver.")), A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD };
            zone = new Zone(nameZone, records);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise test zone", e);
        }
    }

    /**
     * Finds the first declared field of the given {@code type} in {@code clazz}
     * (including private fields). Returns {@code null} if none is found.
     */
    private static Field findFirstFieldOfType(Class<?> clazz, Class<?> type) {
        for (Field f : clazz.getDeclaredFields()) {
            if (type.isAssignableFrom(f.getType())) {
                return f;
            }
        }
        // Recursively search super‑classes (Zone does not extend anything relevant,
        // but the method is defensive.)
        Class<?> superCls = clazz.getSuperclass();
        if (superCls != null && !Object.class.equals(superCls)) {
            return findFirstFieldOfType(superCls, type);
        }
        return null;
    }

    /**
     * Verifies that {@link Zone#iterator()} returns an {@link Iterator} that yields
     * exactly one {@link RRset} for each distinct (name, type) pair present in the zone.
     */
    @Test
    void iteratorReturnsAllRRsets() {
        Iterator<RRset> it = zone.iterator();
        List<RRset> rrsets = new ArrayList<>();
        while (it.hasNext()) {
            rrsets.add(it.next());
        }
        // The test zone contains 6 distinct RRsets:
        //   SOA, NS, A (exact name), AAAA (exact name), A (wildcard), TXT (wildcard)
        assertEquals(6, rrsets.size(), "Iterator should produce 6 RRsets");
        // Simple sanity checks – each of the records we added must appear in some RRset.
        assertTrue(rrsets.stream().anyMatch(r -> r.contains(A_TEST)), "RRset with A_TEST missing");
        assertTrue(rrsets.stream().anyMatch(r -> r.contains(AAAA_1_TEST) && r.contains(AAAA_2_TEST)), "RRset with both AAAA records missing");
        assertTrue(rrsets.stream().anyMatch(r -> r.contains(A_WILD)), "RRset with wildcard A missing");
        assertTrue(rrsets.stream().anyMatch(r -> r.contains(TXT_WILD)), "RRset with wildcard TXT missing");
    }

    /**
     * Ensures that the iterator behaves according to the {@link Iterator} contract:
     * after {@code hasNext()} returns {@code false}, a subsequent {@code next()}
     * call must throw {@link NoSuchElementException}.
     */
    @Test
    void iteratorExhaustionThrowsNoSuchElementException() {
        Iterator<RRset> it = zone.iterator();
        // Exhaust the iterator
        while (it.hasNext()) {
            it.next();
        }
        assertFalse(it.hasNext(), "Iterator should report no more elements");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an exhausted iterator must throw NoSuchElementException");
    }

    /**
     * Verify that an empty {@link Zone} yields an iterator that reports no elements
     * and throws {@link NoSuchElementException} when {@code next()} is invoked.
     */
    @Test
    void emptyZoneIteratorBehaviour() throws IOException {
        Zone empty = new Zone(new Name("empty."), new Record[0]);
        Iterator<RRset> it = empty.iterator();
        assertFalse(it.hasNext(), "Iterator of an empty zone must report hasNext() == false");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    /**
     * Add a single RRset to a fresh zone and verify that the iterator returns
     * exactly that RRset in the order it was added.
     */
    @Test
    void iteratorSingleRRset() throws IOException {
        Zone single = new Zone(new Name("single."), new Record[0]);
        // Adding a single A record creates exactly one RRset.
        InetAddress addr = InetAddress.getByName("192.0.2.1");
        ARecord a = new ARecord(new Name("host", single.getOrigin()), DClass.IN, 300, addr);
        single.addRecord(a);
        Iterator<RRset> it = single.iterator();
        assertTrue(it.hasNext(), "Iterator should have one element");
        RRset rs = it.next();
        assertTrue(rs.contains(a), "Returned RRset must contain the added A record");
        assertFalse(it.hasNext(), "Iterator must be exhausted after the single element");
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * Add three distinct RRsets and verify that the iterator returns them in
     * insertion order.
     */
    @Test
    void iteratorMultipleRRsetsInsertionOrder() throws IOException {
        Zone multi = new Zone(new Name("multi."), new Record[0]);
        // 1) AAAA record
        InetAddress aaaaAddr = InetAddress.getByName("2001:db8::1");
        AAAARecord aaaa = new AAAARecord(new Name("host1", multi.getOrigin()), DClass.IN, 300, aaaaAddr);
        multi.addRecord(aaaa);
        // 2) A record
        InetAddress aAddr = InetAddress.getByName("192.0.2.2");
        ARecord a = new ARecord(new Name("host2", multi.getOrigin()), DClass.IN, 300, aAddr);
        multi.addRecord(a);
        // 3) TXT record
        TXTRecord txt = new TXTRecord(new Name("host3", multi.getOrigin()), DClass.IN, 300, "hello");
        multi.addRecord(txt);
        // Capture iteration order
        List<RRset> iterated = new ArrayList<>();
        for (Iterator<RRset> it = multi.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        assertEquals(3, iterated.size(), "Exactly three RRsets must be iterated");
        // Verify order by checking the first record of each RRset
        assertTrue(iterated.get(0).contains(aaaa), "First RRset should be AAAA");
        assertTrue(iterated.get(1).contains(a), "Second RRset should be A");
        assertTrue(iterated.get(2).contains(txt), "Third RRset should be TXT");
    }

    /**
     * Two successive calls to {@code iterator()} must return independent iterator
     * instances; actions on one should not affect the other.
     */
    @Test
    void iteratorInstancesAreIndependent() throws IOException {
        Zone independent = new Zone(new Name("independent."), new Record[0]);
        // Populate with two different RRsets
        ARecord a1 = new ARecord(new Name("a1", independent.getOrigin()), DClass.IN, 300, InetAddress.getByName("192.0.2.10"));
        ARecord a2 = new ARecord(new Name("a2", independent.getOrigin()), DClass.IN, 300, InetAddress.getByName("192.0.2.11"));
        independent.addRecord(a1);
        independent.addRecord(a2);
        Iterator<RRset> it1 = independent.iterator();
        Iterator<RRset> it2 = independent.iterator();
        // Advance first iterator by one element
        assertTrue(it1.hasNext());
        RRset firstFromIt1 = it1.next();
        // The second iterator must still see both elements
        List<RRset> remainingFromIt2 = new ArrayList<>();
        while (it2.hasNext()) {
            remainingFromIt2.add(it2.next());
        }
        assertEquals(2, remainingFromIt2.size(), "Second iterator should still see the full collection");
        assertTrue(remainingFromIt2.stream().anyMatch(r -> r.contains(a1)));
        assertTrue(remainingFromIt2.stream().anyMatch(r -> r.contains(a2)));
        // Exhaust first iterator and ensure it does not affect the second (already exhausted)
        while (it1.hasNext()) {
            it1.next();
        }
        assertFalse(it1.hasNext());
    }

    /**
     * The iterator returned by {@link Zone#iterator()} must not support {@code remove()}.
     */
    @Test
    void iteratorRemoveUnsupported() {
        Iterator<RRset> it = zone.iterator();
        assertThrows(UnsupportedOperationException.class, it::remove, "Calling remove() on Zone iterator must throw UnsupportedOperationException");
    }

    /**
     * After obtaining an iterator, adding a new RRset to the zone must cause the
     * iterator to become fail‑fast and throw {@link ConcurrentModificationException}
     * on subsequent operations.
     */
    @Test
    void iteratorFailsFastAfterAddition() throws IOException {
        Zone cm = new Zone(new Name("cmadd."), new Record[0]);
        // Populate with a single record so the iterator is non‑empty
        ARecord a = new ARecord(new Name("host", cm.getOrigin()), DClass.IN, 300, InetAddress.getByName("192.0.2.20"));
        cm.addRecord(a);
        Iterator<RRset> it = cm.iterator();
        // Modify the zone after iterator creation
        ARecord aNew = new ARecord(new Name("newhost", cm.getOrigin()), DClass.IN, 300, InetAddress.getByName("192.0.2.21"));
        cm.addRecord(aNew);
        // Any further call must throw CME
        assertThrows(ConcurrentModificationException.class, it::hasNext, "hasNext() after concurrent addition should throw ConcurrentModificationException");
        assertThrows(ConcurrentModificationException.class, it::next, "next() after concurrent addition should throw ConcurrentModificationException");
    }

    /**
     * After obtaining an iterator, removing an existing RRset from the zone must
     * cause the iterator to become fail‑fast and throw {@link ConcurrentModificationException}
     * on subsequent operations.
     */
    @Test
    void iteratorFailsFastAfterRemoval() throws IOException {
        Zone cm = new Zone(new Name("cmremove."), new Record[0]);
        // Two records to ensure a removable RRset exists
        ARecord a1 = new ARecord(new Name("host1", cm.getOrigin()), DClass.IN, 300, InetAddress.getByName("192.0.2.30"));
        ARecord a2 = new ARecord(new Name("host2", cm.getOrigin()), DClass.IN, 300, InetAddress.getByName("192.0.2.31"));
        cm.addRecord(a1);
        cm.addRecord(a2);
        Iterator<RRset> it = cm.iterator();
        // Remove one record via Zone API (which removes the underlying RRset)
        cm.removeRecord(a1);
        // Subsequent iterator operations must detect the concurrent modification
        assertThrows(ConcurrentModificationException.class, it::hasNext);
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    /**
     * Using reflection, set the internal collection that stores RRsets to {@code null}
     * and verify that invoking {@code iterator()} results in a {@link NullPointerException}.
     */
    @Test
    void iteratorConstructionFailsWhenInternalMapIsNull() throws Exception {
        // Create a fresh zone with a single record so the internal map is initialised.
        Zone corrupted = new Zone(new Name("corrupt."), new Record[0]);
        ARecord a = new ARecord(new Name("host", corrupted.getOrigin()), DClass.IN, 300, InetAddress.getByName("192.0.2.40"));
        corrupted.addRecord(a);
        // Find the first field of type Map (the internal RRset storage) and null it.
        Field mapField = findFirstFieldOfType(corrupted.getClass(), Map.class);
        assertNotNull(mapField, "Expected a Map field inside Zone");
        mapField.setAccessible(true);
        mapField.set(corrupted, null);
        // The iterator construction should now hit a NPE.
        assertThrows(NullPointerException.class, corrupted::iterator, "iterator() should throw NullPointerException when internal map is null");
    }

    /**
     * Insert a {@code null} element into the internal RRset collection via reflection.
     * The iterator must throw {@link NullPointerException} when it encounters the null.
     */
    @Test
    void iteratorThrowsWhenNullElementEncountered() throws Exception {
        // Build a zone with a single valid RRset.
        Zone withNull = new Zone(new Name("nulltest."), new Record[0]);
        ARecord a = new ARecord(new Name("host", withNull.getOrigin()), DClass.IN, 300, InetAddress.getByName("192.0.2.50"));
        withNull.addRecord(a);
        // Locate the internal collection that holds RRsets.
        // In the reference implementation it is a Map<Name, List<RRset>>.
        Field mapField = findFirstFieldOfType(withNull.getClass(), Map.class);
        assertNotNull(mapField, "Expected a Map field inside Zone");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Name, List<RRset>> rrMap = (Map<Name, List<RRset>>) mapField.get(withNull);
        // Insert a null RRset into one of the lists.
        // Choose an existing key (any key works).
        Name anyKey = rrMap.keySet().iterator().next();
        List<RRset> list = rrMap.get(anyKey);
        // corrupt the internal structure
        list.add(null);
        // Now iterating must hit the null entry and throw NPE.
        Iterator<RRset> it = withNull.iterator();
        // Consume elements until the NPE is triggered.
        assertThrows(NullPointerException.class, () -> {
            while (it.hasNext()) {
                // the null entry will cause NPE inside iterator.next()
                it.next();
            }
        });
    }
}
