package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class Zone_getNS_Test {

    private static final long TTL = 300L;

    private static Name zoneName;

    private static NSRecord ns1;

    private static NSRecord ns2;

    private static NSRecord nsExtra;

    private Zone zone;

    private static NSRecord ns3;

    /**
     * Initialise static resources that are shared across all test methods.
     */
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

    /**
     * Build a fresh {@link Zone} instance before each test.
     */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        // The constructor that accepts a var‑args array of {@link Record}s is convenient for tests.
        Record[] records = new Record[] { ns1, ns2 };
        zone = new Zone(zoneName, records);
    }

    /**
     * The NS {@link RRset} returned by {@code getNS()} must contain exactly the NS records supplied.
     */
    @Test
    void getNS_containsAllNsRecords() {
        RRset nsSet = zone.getNS();
        assertThat(nsSet).isNotNull();
        // The RRset constructor with var‑args creates an equivalent set.
        RRset expected = new RRset(ns1, ns2);
        assertThat(nsSet).isEqualTo(expected);
    }

    /**
     * Each call to {@code getNS()} must return a new {@link RRset} instance (no shared mutable state).
     */
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
//     @Test
//     void getNS_modifyingReturnedSetDoesNotAlterZoneState() {
//         // Obtain a copy and mutate it
//         RRset mutableCopy = zone.getNS();
//         mutableCopy.addRR(nsExtra);
//         // The zone's own NS set must remain unchanged
//         RRset afterMutation = zone.getNS();
//         // original records only
//         RRset expected = new RRset(ns1, ns2);
//         assertThat(afterMutation).isEqualTo(expected);
//         // Ensure the extra record is **not** present
//         assertThat(afterMutation.containsRR(nsExtra)).isFalse();
//     }

    /**
     * The {@code getNS()} method should never return {@code null} even when the zone contains a
     * single NS record.
     */
    @Test
    void getNS_singleNsRecordNeverNull() throws IOException {
        // Build a zone with only one NS record
        Zone singleNsZone = new Zone(zoneName, new Record[] { ns1 });
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
        // advance to first NS
        it.next();
        // remove from the copy
        it.remove();
        // Zone's NS set should still contain both original records
        RRset after = zone.getNS();
        assertThat(after).isEqualTo(new RRset(ns1, ns2));
    }

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
        // empty set
        nsField.set(zone, new RRset());
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
        internal.addRR(ns3);
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
        // now internal contains ns1, ns2, ns3
        internal.addRR(ns3);
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
            // will throw if something went wrong
            RRset rs = f.get(2, TimeUnit.SECONDS);
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
        copy.deleteRR(ns1);
        // The zone's internal NS should still contain both original records
        RRset after = zone.getNS();
        assertThat(after).isEqualTo(new RRset(ns1, ns2));
    }

    /**
     * Modifying the {@link RRset} returned by {@code getNS()} must not affect the internal NS set of
     * the {@link Zone}. The zone should continue to return the original NS records.
     */
//     @Test
//     void getNS_modifyingReturnedSetDoesNotAlterZone() {
//         // Obtain a copy and mutate it
//         RRset mutableCopy = zone.getNS();
//         mutableCopy.addRR(nsExtra);
//         // The zone's own NS set must remain unchanged
//         RRset afterMutation = zone.getNS();
//         // original records only
//         RRset expected = new RRset(ns1, ns2);
//         assertThat(afterMutation).isEqualTo(expected);
//         // Ensure the extra record is **not** present
//         assertThat(afterMutation.getRRs()).doesNotContain(nsExtra);
//     }
}