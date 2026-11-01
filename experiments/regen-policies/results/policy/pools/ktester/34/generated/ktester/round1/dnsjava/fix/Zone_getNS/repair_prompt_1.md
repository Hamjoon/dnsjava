
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
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
    @Test
    void getNS_modifyingReturnedSetDoesNotAlterZoneState() {
        // Obtain a copy and mutate it
        RRset mutableCopy = zone.getNS();
        mutableCopy.addRR(nsExtra);
        // The zone's own NS set must remain unchanged
        RRset afterMutation = zone.getNS();
        // original records only
        RRset expected = new RRset(ns1, ns2);
        assertThat(afterMutation).isEqualTo(expected);
        // Ensure the extra record is **not** present
        assertThat(afterMutation.containsRR(nsExtra)).isFalse();
    }

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
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Zone_getNS_Test.java:97: error: cannot find symbol
        assertThat(afterMutation.containsRR(nsExtra)).isFalse();
                                ^
  symbol:   method containsRR(NSRecord)
  location: variable afterMutation of type RRset
1 error

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Zone': '\n * A DNS zone. This encapsulates all data related to a zone, and provides convenient lookup methods.\n * A zone always contains a {@link SOARecord} and at least one {@link NSRecord}.\n *\n * @implNote This class uses a {@link java.util.concurrent.locks.ReadWriteLock} to ensure access and\n *     manipulation is safe from multiple threads. Iterators are "weakly consistent" (see the\n *     package info from {@code java.util.concurrent}).\n * @author Brian Wellington\n ', 'api document of method getNS()': " Returns the zone origin's {@link NSRecord NS records}. ", 'return type': 'org.xbill.DNS.RRset', 'dependent classes': 'class `org.xbill.DNS.Zone`:\n\tdependent fields:\n\t\torg.xbill.DNS.RRset org.xbill.DNS.Zone.NS;\n\trelated functions:\n\t\tmethod `private void validate()` returns `void`, related with `org.xbill.DNS.Zone.getNS()`;\n\t\tmethod `public SetResponse findRecords(Name, int)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Zone.getNS()`, api document: \n   * Looks up Records in the zone. The answer can be a {@code CNAME} instead of the actual requested\n   * type and wildcards are expanded.\n   *\n   * @param name The name to look up\n   * @param type The type to look up\n   * @return A SetResponse object\n   * @throws IllegalArgumentException if {@code name} is {@code null}.\n   * @throws InvalidTypeException if the specified {@code type} is invalid.\n   * @see SetResponse\n   ;\n\t\tmethod `public RRset findExactMatch(Name, int)` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Zone.getNS()`, api document: \n   * Looks up Records in the zone, finding exact matches only.\n   *\n   * @param name The name to look up\n   * @param type The type to look up\n   * @return The matching RRset or {@code null} if no exact match is found.\n   * @throws IllegalArgumentException if {@code name} is {@code null}.\n   * @throws InvalidTypeException if the specified {@code type} is invalid.\n   * @see RRset\n   ;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Zone.getNS()`, api document:  Converts a numeric extended flag into a String ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


