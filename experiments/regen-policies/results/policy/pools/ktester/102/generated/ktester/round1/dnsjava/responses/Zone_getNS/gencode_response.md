```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional tests for {@link Zone#getNS()} covering edge‑cases, concurrency,
 * serialization and internal‑state handling.
 *
 * <p>The original basic tests are kept unchanged; the new tests extend the
 * verification matrix defined in the JSON test‑case description.
 */
@ExtendWith(MockitoExtension.class)
class Zone_getNS_Test {

    private static Name ZONE_NAME;
    private static SOARecord SOA;
    private static NSRecord NS1;
    private static NSRecord NS2;
    private Zone zone;

    /** Create objects that are shared across all tests. */
    @BeforeAll
    static void setupBeforeAll() throws Exception {
        ZONE_NAME = Name.fromConstantString("example.");
        SOA = new SOARecord(
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
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L,
                Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L,
                Name.fromConstantString("ns2.example."));
    }

    /** Build a fresh {@link Zone} instance before each test. */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The Zone constructor that accepts var‑args of Record ensures the zone contains the
        // required SOA and at least one NS record.
        Record[] records = new Record[] { SOA, NS1, NS2 };
        zone = new Zone(ZONE_NAME, records);
    }

    /** No special teardown is required for the current tests. */
    @AfterEach
    void teardownAfterEach() {
        zone = null;
    }

    /** No special global teardown is required. */
    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up
    }

    /* --------------------------------------------------------------------- */
    /* Existing basic tests (kept unchanged)                                 */
    /* --------------------------------------------------------------------- */

    /** Verify that {@code getNS()} returns a copy containing the original NS records. */
    @Test
    void getNS_returnsCopyContainingAllNSRecords() {
        RRset nsCopy = zone.getNS();

        assertThat(nsCopy).isNotNull();
        // The copy must contain exactly the two NS records we added.
        assertThat(nsCopy.size()).isEqualTo(2);
        assertThat(nsCopy).anyMatch(r -> r.equals(NS1));
        assertThat(nsCopy).anyMatch(r -> r.equals(NS2));
    }

    /**
     * Modifying the {@link RRset} returned by {@code getNS()} must not affect the zone's internal
     * NS set.
     */
    @Test
    void modificationsToReturnedRRsetDoNotAffectZone() {
        RRset nsCopy = zone.getNS();

        // Add a third NS record only to the copy.
        NSRecord extra = new NSRecord(
                ZONE_NAME,
                DClass.IN,
                300L,
                Name.fromConstantString("ns3.example."));
        nsCopy.addRR(extra);

        // The zone itself should still be unaware of the extra record.
        assertThat(zone.findExactMatch(extra.getName(), Type.NS)).isNull();

        // The original copy still contains the two original NS records.
        assertThat(nsCopy.size()).isEqualTo(3);
        assertThat(nsCopy).anyMatch(r -> r.equals(extra));
    }

    /**
     * Each call to {@code getNS()} must return an independent copy.
     */
    @Test
    void eachInvocationReturnsIndependentCopy() {
        RRset firstCopy = zone.getNS();
        RRset secondCopy = zone.getNS();

        // The two copies must be different objects.
        assertThat(firstCopy).isNotSameAs(secondCopy);

        // Mutate the first copy.
        NSRecord extra = new NSRecord(
                ZONE_NAME,
                DClass.IN,
                300L,
                Name.fromConstantString("ns4.example."));
        firstCopy.addRR(extra);

        // The second copy must remain unchanged.
        assertThat(secondCopy.size()).isEqualTo(2);
        assertThat(secondCopy).noneMatch(r -> r.equals(extra));
    }

    /* --------------------------------------------------------------------- */
    /* New test groups from the JSON description                             */
    /* --------------------------------------------------------------------- */

    /**
     * NS RRset absent – returns an empty RRset.
     *
     * The zone initially contains NS records; we remove them via {@code removeRRset}
     * and then verify that {@code getNS()} returns an empty set rather than throwing.
     */
    @Test
    void getNS_returnsEmptyWhenNsRRsetRemoved() {
        // Remove the NS RRset that was added by the constructor.
        zone.removeRRset(ZONE_NAME, Type.NS);

        RRset nsCopy = zone.getNS();

        assertThat(nsCopy).isNotNull();
        assertThat(nsCopy.size()).isZero();
    }

    /**
     * Multiple NS records – returns a full copy.
     *
     * Adds a third NS record and checks that the returned copy contains all three.
     */
    @Test
    void getNS_returnsCopyWithAllMultipleNsRecords() {
        NSRecord ns3 = new NSRecord(
                ZONE_NAME,
                DClass.IN,
                300L,
                Name.fromConstantString("ns3.example."));
        zone.addRecord(ns3); // add directly to the zone

        RRset nsCopy = zone.getNS();

        assertThat(nsCopy.size()).isEqualTo(3);
        assertThat(nsCopy).anyMatch(r -> r.equals(NS1));
        assertThat(nsCopy).anyMatch(r -> r.equals(NS2));
        assertThat(nsCopy).anyMatch(r -> r.equals(ns3));
    }

    /**
     * Modification of internal nsRRset after getNS does not affect previous copy.
     *
     * Verifies snapshot semantics.
     */
    @Test
    void snapshotIsImmutableAfterInternalChange() {
        // First snapshot (contains only NS1 and NS2)
        RRset firstCopy = zone.getNS();

        // Add a new NS record directly to the zone.
        NSRecord ns3 = new NSRecord(
                ZONE_NAME,
                DClass.IN,
                300L,
                Name.fromConstantString("ns3.example."));
        zone.addRecord(ns3);

        // Second snapshot should see three records.
        RRset secondCopy = zone.getNS();

        // First snapshot must stay unchanged.
        assertThat(firstCopy.size()).isEqualTo(2);
        assertThat(firstCopy).noneMatch(r -> r.equals(ns3));

        // Second snapshot must contain the new record.
        assertThat(secondCopy.size()).isEqualTo(3);
        assertThat(secondCopy).anyMatch(r -> r.equals(ns3));
    }

    /**
     * Serialization round‑trip preserves getNS behavior.
     *
     * The zone is serialized to a byte array and deserialized; the new instance must
     * return an RRset equal in content but distinct in reference.
     */
    @Test
    void getNS_afterSerializationReturnsCorrectCopy() throws IOException, ClassNotFoundException {
        // Serialize the zone
        byte[] data;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(zone);
            out.flush();
            data = bos.toByteArray();
        }

        // Deserialize into a new instance
        Zone deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            deserialized = (Zone) in.readObject();
        }

        // Verify that getNS works on the deserialized zone
        RRset nsCopy = deserialized.getNS();

        assertThat(nsCopy).isNotNull();
        assertThat(nsCopy.size()).isEqualTo(2);
        assertThat(nsCopy).anyMatch(r -> r.equals(NS1));
        assertThat(nsCopy).anyMatch(r -> r.equals(NS2));
    }

    /**
     * Thread‑safety – concurrent invocations of {@code getNS()} do not deadlock.
     *
     * Spawns many threads that call {@code getNS()} simultaneously on a zone that
     * already has NS records. All returned copies must be non‑null and contain the
     * expected number of records.
     */
    @Test
    void concurrentGetNSCallsDoNotDeadlock() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Callable<RRset>> tasks = IntStream.range(0, threadCount)
                    .mapToObj(i -> (Callable<RRset>) () -> zone.getNS())
                    .collect(Collectors.toList());

            List<Future<RRset>> futures = executor.invokeAll(tasks);
            for (Future<RRset> f : futures) {
                RRset copy = f.get(2, TimeUnit.SECONDS);
                assertThat(copy).isNotNull();
                assertThat(copy.size()).isEqualTo(2);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Concurrent modification while reading – getNS returns a consistent snapshot.
     *
     * One writer thread adds NS records with a short pause; several reader threads
     * repeatedly invoke {@code getNS()}. The size observed by readers must be either the
     * previous size or the new size – never a partially‑constructed size.
     */
    @Test
    void getNSConsistentDuringConcurrentModification() throws Exception {
        final int additions = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(4);

        // Writer: adds a new NS record every 100 ms.
        Runnable writer = () -> {
            try {
                startLatch.await();
                for (int i = 1; i <= additions; i++) {
                    NSRecord extra = new NSRecord(
                            ZONE_NAME,
                            DClass.IN,
                            300L,
                            Name.fromConstantString("ns" + (i + 2) + ".example."));
                    zone.addRecord(extra);
                    Thread.sleep(100);
                }
            } catch (Exception ignored) {
            }
        };

        // Reader: repeatedly calls getNS() for a short period.
        Callable<List<Integer>> reader = () -> {
            List<Integer> observed = new ArrayList<>();
            startLatch.await();
            long end = System.currentTimeMillis() + 800; // run a bit longer than writer
            while (System.currentTimeMillis() < end) {
                RRset copy = zone.getNS();
                observed.add(copy.size());
                Thread.sleep(30);
            }
            return observed;
        };

        executor.submit(writer);
        Future<List<Integer>> f1 = executor.submit(reader);
        Future<List<Integer>> f2 = executor.submit(reader);
        startLatch.countDown(); // start both writer and readers

        List<Integer> sizes1 = f1.get(2, TimeUnit.SECONDS);
        List<Integer> sizes2 = f2.get(2, TimeUnit.SECONDS);

        // All observed sizes must be either the initial size (2) or any later size
        // (2 + n) where n is the number of records added so far.
        Set<Integer> allowed = new HashSet<>();
        for (int i = 0; i <= additions; i++) {
            allowed.add(2 + i);
        }

        assertThat(sizes1).allMatch(allowed::contains);
        assertThat(sizes2).allMatch(allowed::contains);
        executor.shutdownNow();
    }

    /**
     * Re‑entrancy of {@code withReadLock} – calling {@code getNS()} inside another
     * read‑locked operation must not deadlock.
     *
     * We obtain the private {@code withReadLock} method via reflection and invoke it
     * with a lambda that calls {@code getNS()}.
     */
    @Test
    void getNSInsideWithReadLockDoesNotDeadlock() throws Exception {
        Method withReadLock = Zone.class.getDeclaredMethod("withReadLock", Supplier.class);
        withReadLock.setAccessible(true);

        @SuppressWarnings("unchecked")
        RRset result = (RRset) withReadLock.invoke(zone, (Supplier<RRset>) zone::getNS);

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).anyMatch(r -> r.equals(NS1));
        assertThat(result).anyMatch(r -> r.equals(NS2));
    }

    /**
     * Zone contains other RRsets but no NS – getNS returns empty RRset.
     *
     * We remove the NS RRset and then add A and MX records. The call must still
     * return an empty RRset.
     */
    @Test
    void getNSReturnsEmptyWhenOnlyOtherRRsetsExist() {
        // Remove NS records first.
        zone.removeRRset(ZONE_NAME, Type.NS);

        // Add non‑NS records.
        ARecord a = new ARecord(Name.fromConstantString("host.example."), DClass.IN, 300L,
                InetAddress.getLoopbackAddress());
        MXRecord mx = new MXRecord(Name.fromConstantString("example."), DClass.IN, 300L,
                10, Name.fromConstantString("mail.example."));
        zone.addRecord(a);
        zone.addRecord(mx);

        RRset nsCopy = zone.getNS();

        assertThat(nsCopy).isNotNull();
        assertThat(nsCopy.size()).isZero();
    }

    /**
     * Adding a non‑NS record does not affect getNS output.
     *
     * Starts from a zone that already has NS records, adds a TXT record and verifies
     * that the NS copy remains unchanged.
     */
    @Test
    void addingNonNsRecordDoesNotChangeNsCopy() {
        TXTRecord txt = new TXTRecord(ZONE_NAME, DClass.IN, 300L, "some text");
        zone.addRecord(txt);

        RRset nsCopy = zone.getNS();

        assertThat(nsCopy.size()).isEqualTo(2);
        assertThat(nsCopy).anyMatch(r -> r.equals(NS1));
        assertThat(nsCopy).anyMatch(r -> r.equals(NS2));
    }

    /**
     * NS RRset absent – returns empty RRset when the zone is constructed without any NS.
     *
     * Some Zone implementations allow construction with only an SOA. We simulate this
     * by creating a Zone via the protected constructor (via reflection) that does not
     * populate nsRRset.
     */
    @Test
    void getNSWhenZoneCreatedWithoutNsReturnsEmpty() throws Exception {
        // Use reflection to invoke the protected constructor that accepts only Name and SOA.
        Constructor<Zone> ctor = Zone.class.getDeclaredConstructor(Name.class, Record[].class);
        ctor.setAccessible(true);
        Record[] onlySoa = new Record[] { SOA };
        Zone zoneWithoutNs = ctor.newInstance(ZONE_NAME, onlySoa);

        RRset nsCopy = zoneWithoutNs.getNS();

        assertThat(nsCopy).isNotNull();
        assertThat(nsCopy.size()).isZero();
    }
}
```