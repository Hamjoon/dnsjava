package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListMap;

@ExtendWith(MockitoExtension.class)
class Zone_AXFR_Test {

    /* --------------------------------------------------------------------- *
     *  Shared test data – created once per test method (see @BeforeEach).   *
     * --------------------------------------------------------------------- */
    private Name zoneName;

    private SOARecord soa;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aTest;

    private AAAARecord aaaaTest;

    private ARecord aUnique;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        zoneName = Name.fromConstantString("example.");
        soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600L, localhost);
        aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600L, InetAddress.getByName("::1"));
        aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600L, InetAddress.getByName("127.0.0.3"));
        Record[] records = new Record[] { soa, ns1, ns2, aTest, aaaaTest, aUnique };
        zone = new Zone(zoneName, records);
    }

    /**
     * The AXFR iterator must return a snapshot containing every RRset in the zone.
     */
    @Test
    void axfrIteratesAllRRsets() {
        // Collect RRsets via the normal iterator (reference snapshot)
        List<RRset> expected = new ArrayList<>();
        for (Iterator<RRset> it = zone.iterator(); it.hasNext(); ) {
            expected.add(it.next());
        }
        // Collect RRsets via the AXFR iterator
        List<RRset> actual = new ArrayList<>();
        for (Iterator<RRset> it = zone.AXFR(); it.hasNext(); ) {
            actual.add(it.next());
        }
        assertThat(actual).as("AXFR iterator should return the same number of RRsets as the regular iterator").hasSameSizeAs(expected);
        // The order of RRsets is defined by ZoneIterator – we compare element‑wise.
        assertThat(actual).as("AXFR iterator should return RRsets in the same order as the regular iterator").containsExactlyElementsOf(expected);
    }

    /**
     * {@code remove()} is not supported on the AXFR iterator.
     */
    @Test
    void axfrIteratorRemoveNotSupported() {
        Iterator<RRset> it = zone.AXFR();
        assertThatThrownBy(it::remove).isInstanceOf(IllegalStateException.class).hasMessageContaining("remove");
    }

    /**
     * After the iterator is exhausted, {@code next()} must throw {@link NoSuchElementException}.
     */
    @Test
    void axfrIteratorNextAfterLastFails() {
        Iterator<RRset> it = zone.AXFR();
        while (it.hasNext()) {
            it.next();
        }
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class).hasMessageContaining("No more elements");
    }

    /**
     * The iterator returned by {@code AXFR()} is weakly‑consistent.
     *
     * <p>We obtain the iterator, then add a new record to the zone and continue iterating.
     * The iterator must not throw {@link java.util.ConcurrentModificationException}.
     * The newly added record may or may not appear – both behaviours are allowed – but the
     * iterator must remain functional.
     */
    @Test
    void axfrIsWeaklyConsistent() {
        Iterator<RRset> it = zone.AXFR();
        // Consume the first RRset (SOA) to ensure the iterator is in use.
        assertThat(it.hasNext()).isTrue();
        RRset first = it.next();
        assertThat(first).isNotNull();
        // Add a new record after the iterator has been created.
        ARecord extra = new ARecord(new Name("extra", zoneName), DClass.IN, 3600L, InetAddress.getByName("127.0.0.99"));
        assertDoesNotThrow(() -> zone.addRecord(extra));
        // Continue iterating – we only assert that no CME is thrown.
        assertDoesNotThrow(() -> {
            while (it.hasNext()) {
                it.next();
            }
        });
    }

    /* --------------------------------------------------------------------- *
     *  1. Verify that AXFR() creates a ZoneIterator with the AXFR flag true. *
     * --------------------------------------------------------------------- */
    @Test
    void axfrCreatesZoneIteratorWithFlagEnabled() throws Exception {
        Iterator<RRset> iterator = zone.AXFR();
        // The concrete class is a private inner class – we check its name.
        String iteratorClassName = iterator.getClass().getSimpleName();
        assertThat(iteratorClassName).as("AXFR should return an instance of ZoneIterator").contains("ZoneIterator");
        // The ZoneIterator has a private boolean field (often named 'axfr' or similar).
        // Use reflection to read it.
        Field axfrFlag = iterator.getClass().getDeclaredField("axfr");
        axfrFlag.setAccessible(true);
        boolean flagValue = (boolean) axfrFlag.get(iterator);
        assertThat(flagValue).as("The internal AXFR flag of the iterator must be true").isTrue();
    }

    /* --------------------------------------------------------------------- *
     *  2. Empty zone – iterator must be empty.                               *
     * --------------------------------------------------------------------- */
    @Test
    void axfrOnEmptyZoneReturnsEmptyIterator() throws Exception {
        // Create a zone with no records (the constructor accepts an empty array).
        Zone emptyZone = new Zone(Name.fromConstantString("empty."));
        Iterator<RRset> it = emptyZone.AXFR();
        assertThat(it.hasNext()).as("AXFR iterator for an empty zone should have no elements").isFalse();
        assertThatThrownBy(it::next).as("Calling next() on an empty iterator must throw NoSuchElementException").isInstanceOf(NoSuchElementException.class);
    }

    /* --------------------------------------------------------------------- *
     *  3. Single RRset – iterator yields exactly that RRset.                  *
     * --------------------------------------------------------------------- */
    @Test
    void axfrIteratesSingleRRset() throws IOException {
        Zone singleZone = new Zone(zoneName);
        // Add a single A record (which creates an RRset internally).
        singleZone.addRecord(aTest);
        Iterator<RRset> it = singleZone.AXFR();
        assertThat(it.hasNext()).isTrue();
        RRset first = it.next();
        assertThat(first).isNotNull();
        // The RRset should contain exactly the A record we added.
        assertThat(first.size()).isEqualTo(1);
        assertThat(first.getRRs()).extracting(Record::rdataToString).containsExactly(aTest.rdataToString());
        assertThat(it.hasNext()).isFalse();
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
    }

    /* --------------------------------------------------------------------- *
     *  4. Multiple RRsets – order must match insertion order.               *
     * --------------------------------------------------------------------- */
    @Test
    void axfrIteratesMultipleRRsetsInInsertionOrder() throws IOException {
        Zone multiZone = new Zone(zoneName);
        // Insert records in a known order.
        // SOA
        multiZone.addRecord(soa);
        // NS
        multiZone.addRecord(ns1);
        // A
        multiZone.addRecord(aTest);
        // AAAA
        multiZone.addRecord(aaaaTest);
        // Expected order using the normal iterator (snapshot).
        List<RRset> expected = new ArrayList<>();
        for (Iterator<RRset> it = multiZone.iterator(); it.hasNext(); ) {
            expected.add(it.next());
        }
        // Collect via AXFR iterator.
        List<RRset> actual = new ArrayList<>();
        for (Iterator<RRset> it = multiZone.AXFR(); it.hasNext(); ) {
            actual.add(it.next());
        }
        assertThat(actual).as("AXFR iterator should return all RRsets").hasSameSizeAs(expected);
        assertThat(actual).as("AXFR iterator must preserve insertion order").containsExactlyElementsOf(expected);
    }

    /* --------------------------------------------------------------------- *
     *  5. remove() on AXFR iterator must throw UnsupportedOperationException *
     * --------------------------------------------------------------------- */
    @Test
    void axfrIteratorRemoveThrowsUnsupportedOperationException() {
        Iterator<RRset> it = zone.AXFR();
        assertThatThrownBy(it::remove).as("remove() on AXFR iterator must be unsupported").isInstanceOf(UnsupportedOperationException.class);
    }

    /* --------------------------------------------------------------------- *
     *  6. Snapshot semantics – addition after iterator creation is ignored. *
     * --------------------------------------------------------------------- */
    @Test
    void axfrSnapshotIgnoresLaterAdditions() throws Exception {
        // Prepare a zone with three RRsets.
        Zone snapZone = new Zone(zoneName);
        snapZone.addRecord(soa);
        snapZone.addRecord(ns1);
        snapZone.addRecord(aTest);
        Iterator<RRset> it = snapZone.AXFR();
        // Add a new record *after* obtaining the iterator.
        snapZone.addRecord(new ARecord(new Name("newhost", zoneName), DClass.IN, 3600L, InetAddress.getByName("127.0.0.55")));
        // Collect what the iterator yields.
        List<RRset> yielded = new ArrayList<>();
        while (it.hasNext()) {
            yielded.add(it.next());
        }
        // The newly added RRset must not appear.
        assertThat(yielded).as("AXFR iterator must not see records added after its creation").hasSize(3);
    }

    /* --------------------------------------------------------------------- *
     *  7. Snapshot semantics – removal after iterator creation is ignored. *
     * --------------------------------------------------------------------- */
    @Test
    void axfrSnapshotIgnoresLaterRemovals() throws Exception {
        Zone snapZone = new Zone(zoneName);
        snapZone.addRecord(soa);
        snapZone.addRecord(ns1);
        snapZone.addRecord(aTest);
        Iterator<RRset> it = snapZone.AXFR();
        // Remove one RRset after iterator creation.
        snapZone.removeRecord(aTest);
        // The iterator should still return the original three RRsets.
        List<RRset> yielded = new ArrayList<>();
        while (it.hasNext()) {
            yielded.add(it.next());
        }
        assertThat(yielded).hasSize(3);
    }

    /* --------------------------------------------------------------------- *
     *  8. Null reference invocation – should throw NullPointerException.   *
     * --------------------------------------------------------------------- */
    @Test
    void axfrOnNullZoneThrowsNullPointerException() {
        Zone nullZone = null;
        assertThatThrownBy(() -> nullZone.AXFR()).as("Calling AXFR on a null Zone reference must throw NPE").isInstanceOf(NullPointerException.class);
    }

    /* --------------------------------------------------------------------- *
     *  9. Corrupted internal state – iterator should raise IllegalStateException. *
     * --------------------------------------------------------------------- */
    @Test
    void axfrWithCorruptedInternalStateThrowsIllegalStateException() throws Exception {
        // Use reflection to nullify the internal map that holds records.
        // name may differ; adjust if needed
        Field mapField = Zone.class.getDeclaredField("records");
        mapField.setAccessible(true);
        // corrupt the zone
        mapField.set(zone, null);
        Iterator<RRset> it = zone.AXFR();
        assertThatThrownBy(it::hasNext).as("hasNext() on a corrupted iterator must throw IllegalStateException").isInstanceOf(IllegalStateException.class);
    }
}
