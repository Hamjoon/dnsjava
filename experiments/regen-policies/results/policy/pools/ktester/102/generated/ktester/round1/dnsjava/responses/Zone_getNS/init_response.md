```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#getNS()}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>The method returns a copy of the internal NS {@link RRset}.
 *   <li>Modifications to the returned {@link RRset} do not affect the {@link Zone}'s own data.
 *   <li>Each invocation returns a fresh copy (i.e., two calls are independent).
 * </ul>
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
}
```