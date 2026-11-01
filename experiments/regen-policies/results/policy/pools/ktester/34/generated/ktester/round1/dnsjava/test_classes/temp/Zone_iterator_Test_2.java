package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;
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
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class Zone_iterator_Test {

    /* --------------------------------------------------------------------- *
     *  Test fixtures – created once per test method (via @BeforeEach)      *
     * --------------------------------------------------------------------- */
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

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // zone name
        ZONE_NAME = Name.fromConstantString("example.");
        // mandatory SOA
        SOA1 = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostadmin", ZONE_NAME), 1, 21600L, 7200L, 2160000L, 3600L);
        // two NS records – the iterator will combine them into a single RRset
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // a few address records
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");
        A_UNIQUE = new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
        A_WILD = new ARecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");
        // build the zone – the constructor validates the presence of SOA & NS
        Record[] records = new Record[] { SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD };
        ZONE = new Zone(ZONE_NAME, records);
    }

    /**
     * Creates a {@link Zone} that contains only the mandatory SOA and a single NS
     * record, plus any additional {@link Record}s supplied.
     */
    private static Zone createZone(Record... extra) throws IOException {
        Name zoneName = Name.fromConstantString("emptyzone.");
        SOARecord soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.emptyzone."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        NSRecord ns = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns.emptyzone."));
        List<Record> list = new ArrayList<>();
        list.add(soa);
        list.add(ns);
        for (Record r : extra) {
            list.add(r);
        }
        return new Zone(zoneName, list.toArray(new Record[0]));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> zoneWithThreeRrsetsProvider() throws IOException {
        Name zoneName = Name.fromConstantString("orderzone.");
        SOARecord soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.orderzone."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        NSRecord ns = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns.orderzone."));
        // three additional records in a known order
        ARecord a1 = new ARecord(new Name("a1", zoneName), DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        TXTRecord txt = new TXTRecord(new Name("txt", zoneName), DClass.IN, 3600, "hello");
        AAAARecord aaaa = new AAAARecord(new Name("aaaa", zoneName), DClass.IN, 3600, InetAddress.getByName("::1"));
        List<Record> order = List.of(a1, txt, aaaa);
        List<Record> all = new ArrayList<>();
        all.add(soa);
        all.add(ns);
        all.addAll(order);
        Zone zone = new Zone(zoneName, all.toArray(new Record[0]));
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(zone, order));
    }

    @Test
    void iteratorOrderAndContent() {
        Iterator<RRset> it = ZONE.iterator();
        // first RRset must be the SOA
        assertThat(it).isNotNull();
        assertThat(it.hasNext()).isTrue();
        RRset first = it.next();
        assertThat(first).isEqualTo(new RRset(SOA1));
        // second RRset must contain both NS records
        assertThat(it.hasNext()).isTrue();
        RRset second = it.next();
        assertThat(second).isEqualTo(ZONE.getNS());
        // third RRset – we know there is at least one A record (A_UNIQUE)
        assertThat(it.hasNext()).isTrue();
        RRset third = it.next();
        // we cannot guarantee order of the remaining RRsets, but the set must contain the A_UNIQUE record
        assertThat(third).contains(A_UNIQUE);
    }

    @Test
    void iteratorRemoveBeforeNextFails() {
        Iterator<RRset> it = ZONE.iterator();
        assertThatThrownBy(it::remove).isInstanceOf(IllegalStateException.class).hasMessageContaining("Not at");
    }

    @Test
    void iteratorRemoveNsFails() {
        Iterator<RRset> it = ZONE.iterator();
        // advance to the NS RRset (second element)
        // SOA
        it.next();
        assertThat(it.hasNext()).isTrue();
        RRset nsSet = it.next();
        assertThat(nsSet).isEqualTo(ZONE.getNS());
        // attempt to remove the NS set – the implementation forbids this
        assertThatThrownBy(it::remove).hasMessageContaining("remove all NS");
    }

    @Test
    void iteratorRemoveARecordSucceeds() {
        Iterator<RRset> it = ZONE.iterator();
        // skip SOA and NS
        // SOA
        it.next();
        // NS
        it.next();
        // next RRset contains A records (including A_TEST)
        assertThat(it.hasNext()).isTrue();
        RRset aSet = it.next();
        assertThat(aSet).contains(A_TEST);
        // remove this RRset – the underlying zone should no longer have the A record
        it.remove();
        // verify removal via the public API
        assertThat(ZONE.findExactMatch(A_TEST.getName(), Type.A)).isNull();
    }

    @Test
    void iteratorNextAfterExhaustionFails() {
        Iterator<RRset> it = ZONE.iterator();
        // exhaust the iterator
        while (it.hasNext()) {
            it.next();
        }
        // now next() must throw NoSuchElementException
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class).hasMessageContaining("No more elements");
    }

    /**
     * The zone always contains at least an SOA and one NS record.  This test
     * verifies that an iterator over a zone that has *no additional* records
     * correctly reports exactly two RRsets (SOA + NS) and then terminates.
     */
    @Test
    void iteratorOverMinimalZone() throws IOException {
        // only SOA + one NS
        Zone minimal = createZone();
        Iterator<RRset> it = minimal.iterator();
        // first element – SOA
        assertThat(it.hasNext()).isTrue();
        RRset soa = it.next();
        // retrieve the SOA record from the zone (findExactMatch returns an RRset)
        RRset soaRRset = minimal.findExactMatch(minimal.getName(), Type.SOA);
        Record soaRecord = soaRRset.first();
        assertThat(soa).contains(soaRecord);
        // second element – NS set
        assertThat(it.hasNext()).isTrue();
        RRset ns = it.next();
        assertThat(ns).isEqualTo(minimal.getNS());
        // no further elements
        assertThat(it.hasNext()).isFalse();
    }

    /**
     * Verify that an iterator over a zone with a *single* additional RRset
     * yields exactly three RRsets (SOA, NS, and the extra set) and then stops.
     */
    @Test
    void iteratorSingleAdditionalRrset() throws IOException {
        ARecord extraA = new ARecord(new Name("single", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Zone zone = createZone(extraA);
        Iterator<RRset> it = zone.iterator();
        // SOA
        assertThat(it.next()).contains(SOA1);
        // NS
        assertThat(it.next()).isEqualTo(zone.getNS());
        // the extra A record
        RRset aSet = it.next();
        assertThat(aSet).contains(extraA);
        // exhausted
        assertThat(it.hasNext()).isFalse();
    }

    /**
     * The iterator must preserve the insertion order of RRsets (after the
     * mandatory SOA and NS).  This parameterised test builds zones with three
     * distinct RRsets and checks that the iterator returns them in the order
     * they were added.
     */
    @ParameterizedTest
    @MethodSource("zoneWithThreeRrsetsProvider")
    void iteratorPreservesInsertionOrder(Zone zone, List<Record> expectedOrder) {
        Iterator<RRset> it = zone.iterator();
        // skip mandatory SOA and NS – they are always first
        assertThat(it.next()).contains(SOA1);
        assertThat(it.next()).isEqualTo(zone.getNS());
        // now verify the remaining RRsets appear in the expected order
        for (Record r : expectedOrder) {
            assertThat(it.hasNext()).isTrue();
            RRset rs = it.next();
            assertThat(rs).contains(r);
        }
        // no more elements
        assertThat(it.hasNext()).isFalse();
    }

    /**
     * The iterator returned by {@link Zone#iterator()} is read‑only.  Invoking
     * {@code remove()} after a successful {@code next()} must raise an
     * {@link UnsupportedOperationException}.
     */
    @Test
    void iteratorRemoveAfterNextThrowsUnsupported() {
        Iterator<RRset> it = ZONE.iterator();
        // advance once (SOA)
        it.next();
        assertThatThrownBy(it::remove).isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Two iterators obtained from the same {@link Zone} must operate
     * independently – advancing one must not affect the other.
     */
    @Test
    void twoIteratorsAreIndependent() {
        Iterator<RRset> it1 = ZONE.iterator();
        Iterator<RRset> it2 = ZONE.iterator();
        // advance first iterator by one element (SOA)
        assertThat(it1.next()).contains(SOA1);
        // second iterator should still see the SOA as its first element
        assertThat(it2.next()).contains(SOA1);
        // and both iterators should now be positioned at the same subsequent element (NS)
        assertThat(it1.next()).isEqualTo(ZONE.getNS());
        assertThat(it2.next()).isEqualTo(ZONE.getNS());
    }

    /**
     * Verify that the iterator is weakly consistent: adding a new RRset after the
     * iterator has been obtained must not cause a {@link java.util.ConcurrentModificationException}.
     */
    @Test
    void iteratorDoesNotThrowOnConcurrentAdd() throws IOException {
        // minimal zone
        Zone mutable = createZone();
        Iterator<RRset> it = mutable.iterator();
        // add a new A record after iterator creation
        // obtain the zone's name via reflection (to avoid compile‑time dependency on getName())
        Method getNameMethod = mutable.getClass().getMethod("getName");
        Name zoneName = (Name) getNameMethod.invoke(mutable);
        ARecord added = new ARecord(new Name("added", zoneName), DClass.IN, 3600, InetAddress.getByName("10.0.0.5"));
        mutable.addRecord(added);
        // iterator should still be usable – we simply iterate to the end
        while (it.hasNext()) {
            // no CME expected
            it.next();
        }
        // the newly added RRset may or may not appear; we only assert no exception
        assertThat(mutable.findExactMatch(added.getName(), Type.A)).isNotNull();
    }

    /**
     * Verify that the iterator is weakly consistent when an existing RRset is
     * removed after the iterator has been obtained.
     */
    @Test
    void iteratorDoesNotThrowOnConcurrentRemove() throws IOException {
        // minimal + one A record
        Zone mutable = createZone(A_UNIQUE);
        Iterator<RRset> it = mutable.iterator();
        // remove the A record while iterating
        mutable.removeRecord(A_UNIQUE);
        // iterator must continue without throwing CME
        while (it.hasNext()) {
            // no CME expected
            it.next();
        }
        // confirm removal
        assertThat(mutable.findExactMatch(A_UNIQUE.getName(), Type.A)).isNull();
    }

    /**
     * Calling {@code next()} on an iterator of an empty zone (i.e. a zone that
     * contains only the mandatory SOA and NS) should throw
     * {@link NoSuchElementException} after the two required RRsets have been
     * consumed.
     */
    @Test
    void nextOnExhaustedMinimalZoneThrows() throws IOException {
        // only SOA + NS
        Zone minimal = createZone();
        Iterator<RRset> it = minimal.iterator();
        // consume SOA and NS
        it.next();
        it.next();
        // now iterator is exhausted
        assertThat(it.hasNext()).isFalse();
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
    }
}
