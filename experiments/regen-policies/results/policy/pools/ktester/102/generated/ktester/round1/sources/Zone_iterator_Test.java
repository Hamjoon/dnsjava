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
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class Zone_iterator_Test {

    /**
     * Shared immutable objects used to build a {@link Zone} for each test.
     */
    private static Name ZONE_NAME;

    private static SOARecord SOA1;

    private static NSRecord NS1;

    private static NSRecord NS2;

    private static ARecord A_TEST;

    /**
     * Fresh instance for every test method.
     */
    private Zone zone;

    private static AAAARecord AAAA_TEST;

    private static MXRecord MX1;

    private static MXRecord MX2;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // origin of the zone
        ZONE_NAME = Name.fromConstantString("example.");
        // minimal required records: SOA + two NS + one A
        SOA1 = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster", ZONE_NAME), 1, 21600L, 7200L, 2160000L, 3600L);
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        InetAddress localhost6 = InetAddress.getByName("::1");
        AAAA_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6);
        MX1 = new MXRecord(new Name("mail", ZONE_NAME), DClass.IN, 300, 10, Name.fromConstantString("mail1.example."));
        MX2 = new MXRecord(new Name("mail", ZONE_NAME), DClass.IN, 300, 20, Name.fromConstantString("mail2.example."));
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

    /**
     * Verify that calling {@code remove()} before any {@code next()} throws {@link IllegalStateException}.
     */
    @Test
    void iteratorRemoveBeforeNextFails() {
        Iterator<RRset> it = zone.iterator();
        IllegalStateException ex = assertThrows(IllegalStateException.class, it::remove);
        assertTrue(ex.getMessage().contains("Not at"));
    }

    /**
     * Removing a record via the iterator should delete it from the underlying zone.
     */
    @Test
    void iteratorRemoveASucceeds() {
        Iterator<RRset> it = zone.iterator();
        // 0: SOA, 1: NS, 2: NS, 3: A (our test record)
        // SOA
        it.next();
        // NS1
        it.next();
        // NS2
        it.next();
        // A_TEST
        it.next();
        // remove the A record
        it.remove();
        // After removal the zone must no longer contain the A record.
        assertNull(zone.findExactMatch(A_TEST.getName(), Type.A), "A record should have been removed by iterator.remove()");
    }

    /**
     * After iterating to the end, {@code next()} must throw {@link NoSuchElementException}.
     */
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

    /**
     * Normal iteration should return all RRsets in insertion order.
     */
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

    /**
     * Empty zone – iterator is non‑null and reports no elements.
     */
    @Test
    void emptyZoneIteratorHasNoElements() throws IOException {
        // constructor with no records
        Zone empty = new Zone(ZONE_NAME);
        Iterator<RRset> it = empty.iterator();
        assertNotNull(it, "Iterator must not be null");
        assertFalse(it.hasNext(), "hasNext() should be false for an empty zone");
        assertThrows(NoSuchElementException.class, it::next, "next() on empty iterator must throw");
    }

    /**
     * Single RRset – iterator returns it and then reports exhaustion.
     */
    @Test
    void iteratorSingleRRset() throws IOException {
        Zone single = new Zone(ZONE_NAME);
        single.addRRset(new RRset(A_TEST));
        Iterator<RRset> it = single.iterator();
        assertTrue(it.hasNext(), "Iterator should have one element");
        RRset first = it.next();
        assertEquals(A_TEST, first.first(), "Returned RRset must contain the added A record");
        assertFalse(it.hasNext(), "Iterator must be exhausted after the single element");
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * Iterator traverses three RRsets in insertion order.
     */
    @Test
    void iteratorThreeRRsetsOrder() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        RRset r1 = new RRset(NS1);
        RRset r2 = new RRset(MX1);
        RRset r3 = new RRset(A_TEST);
        z.addRRset(r1);
        z.addRRset(r2);
        z.addRRset(r3);
        Iterator<RRset> it = z.iterator();
        List<RRset> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertEquals(List.of(r1, r2, r3), collected, "Iterator must respect insertion order");
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * Two iterators obtained from the same zone operate independently.
     */
    @Test
    void independentIterators() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        z.addRRset(new RRset(NS1));
        z.addRRset(new RRset(NS2));
        Iterator<RRset> itA = z.iterator();
        Iterator<RRset> itB = z.iterator();
        // consumes first element for iterator A
        RRset firstA = itA.next();
        assertEquals(NS1, firstA.first());
        // iterator B must still see the first element
        RRset firstB = itB.next();
        assertEquals(NS1, firstB.first());
    }

    /**
     * Calling {@code next()} on an empty iterator throws {@link NoSuchElementException}.
     */
    @Test
    void nextOnEmptyThrows() throws IOException {
        Zone empty = new Zone(ZONE_NAME);
        Iterator<RRset> it = empty.iterator();
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * Calling {@code remove()} before {@code next()} throws {@link IllegalStateException}
     *  (the implementation follows the standard iterator contract).
     */
    @Test
    void removeBeforeNextThrowsIllegalState() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        z.addRRset(new RRset(A_TEST));
        Iterator<RRset> it = z.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    /**
     * Iterator does not see RRsets added after its creation (weakly‑consistent).
     */
    @Test
    void iteratorIgnoresLaterAdditions() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        RRset existing = new RRset(NS1);
        z.addRRset(existing);
        // snapshot taken here
        Iterator<RRset> it = z.iterator();
        // add a new RRset after iterator creation
        RRset later = new RRset(A_TEST);
        z.addRRset(later);
        // iterator should only return the original RRset
        assertTrue(it.hasNext());
        assertEquals(existing, it.next());
        assertFalse(it.hasNext(), "Iterator must not see RRsets added after creation");
    }

    /**
     * Iterator still returns RRsets that were removed after iterator creation.
     */
    @Test
    void iteratorSeesRemovedAfterCreation() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        RRset r1 = new RRset(NS1);
        RRset r2 = new RRset(A_TEST);
        z.addRRset(r1);
        z.addRRset(r2);
        // snapshot
        Iterator<RRset> it = z.iterator();
        // remove r2 from the zone after iterator creation
        z.removeRRset(r2.getName(), Type.A);
        // iterator must still yield both RRsets
        assertTrue(it.hasNext());
        assertEquals(r1, it.next());
        assertTrue(it.hasNext());
        assertEquals(r2, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Multiple calls to {@code hasNext()} before {@code next()} do not advance the iterator.
     */
    @Test
    void hasNextIdempotentBeforeNext() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        RRset r1 = new RRset(NS1);
        RRset r2 = new RRset(A_TEST);
        z.addRRset(r1);
        z.addRRset(r2);
        Iterator<RRset> it = z.iterator();
        // three consecutive hasNext() calls – all must return true and not move cursor
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        // now consume the first element
        assertEquals(r1, it.next());
    }

    /**
     * After exhaustion, repeated {@code hasNext()} calls always return {@code false}.
     */
    @Test
    void hasNextAfterExhaustionRemainsFalse() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        z.addRRset(new RRset(A_TEST));
        Iterator<RRset> it = z.iterator();
        assertTrue(it.hasNext());
        // consume the only element
        it.next();
        // iterator is now exhausted
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
    }

    /**
     * Calling {@code remove()} after a successful {@code next()} throws {@link UnsupportedOperationException}.
     */
    @Test
    void removeAfterNextThrowsUnsupported() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        z.addRRset(new RRset(A_TEST));
        Iterator<RRset> it = z.iterator();
        // advance to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /**
     * Enhanced for‑each loop traverses all RRsets in the order defined by ZoneIterator.
     */
    @Test
    void forEachTraversalOrder() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        // Names chosen so that lexical order is A, MX, NS
        RRset a = new RRset(A_TEST);
        RRset mx = new RRset(MX1);
        RRset ns = new RRset(NS1);
        z.addRRset(a);
        z.addRRset(mx);
        z.addRRset(ns);
        List<RRset> collected = new ArrayList<>();
        for (RRset r : z) {
            collected.add(r);
        }
        // The internal ordering of ZoneIterator for this simple test follows insertion order.
        assertEquals(List.of(a, mx, ns), collected, "For‑each should iterate over all RRsets in the same order as the iterator");
    }

    /**
     * Iterator returns separate RRsets for the same name but different types (A and AAAA).
     */
    @Test
    void iteratorHandlesSameNameDifferentTypes() throws IOException {
        Zone z = new Zone(ZONE_NAME);
        // Both records share the same owner name "test.example."
        z.addRecord(A_TEST);
        z.addRecord(AAAA_TEST);
        Iterator<RRset> it = z.iterator();
        RRset first = it.next();
        RRset second = it.next();
        // Verify that both types are present (order is implementation‑defined; we just check both appear)
        List<Record> types = List.of(first.first(), second.first());
        assertTrue(types.contains(A_TEST), "A record must be present in iteration");
        assertTrue(types.contains(AAAA_TEST), "AAAA record must be present in iteration");
        assertFalse(it.hasNext(), "Only two RRsets should be iterated");
    }
}
