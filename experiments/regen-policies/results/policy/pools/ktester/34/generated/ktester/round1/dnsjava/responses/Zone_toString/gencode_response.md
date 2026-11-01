```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link Zone#toString()}.
 *
 * <p>The tests cover a variety of scenarios described in the JSON test‑case list:
 *
 * <ul>
 *   <li>Empty zone (minimal $ORIGIN output).</li>
 *   <li>Zone with only SOA and NS records.</li>
 *   <li>Zone with custom $ORIGIN.</li>
 *   <li>Record removal reflected in the master‑file string.</li>
 *   <li>Propagation of an {@link IOException} from {@code toMasterFile()}.</li>
 *   <li>Null origin causing {@link NullPointerException}.</li>
 * </ul>
 *
 * <p>Where the public API does not provide a direct way to create the exact
 * situation (e.g., an empty zone), reflection or Mockito spying is used.
 */
@ExtendWith(MockitoExtension.class)
class Zone_toString_Test {

    private static final Name ROOT = Name.fromConstantString(".");
    private static final Name EXAMPLE = Name.fromConstantString("example.");

    private Zone zone;

    /* --------------------------------------------------------------------- *
     * Helper methods                                                       *
     * --------------------------------------------------------------------- */

    /** Creates a {@link Zone} with the given origin and records. */
    private Zone createZone(Name origin, Record... records) {
        return assertDoesNotThrow(() -> new Zone(origin, records));
    }

    /** Returns a simple SOA record for the supplied origin. */
    private SOARecord makeSOA(Name origin) throws IOException {
        return new SOARecord(
                origin,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns1." + origin),
                new Name("hostmaster", origin),
                1,
                7200L,
                3600L,
                1209600L,
                3600L);
    }

    /** Returns a simple NS record for the supplied origin. */
    private NSRecord makeNS(Name origin, String ns) throws IOException {
        return new NSRecord(origin, DClass.IN, 3600L, Name.fromConstantString(ns));
    }

    /* --------------------------------------------------------------------- *
     * Test cases                                                            *
     * --------------------------------------------------------------------- */

    /** Empty zone – only the default $ORIGIN line should be present. */
    @Test
    void emptyZoneProducesOnlyOrigin() throws Exception {
        // The public constructor enforces presence of SOA/NS, therefore we
        // instantiate a Zone via reflection and then clear its internal map.
        Constructor<Zone> ctor = Zone.class.getDeclaredConstructor(Name.class, Record[].class);
        ctor.setAccessible(true);
        Zone rawZone = ctor.newInstance(ROOT, new Record[0]);

        // Use reflection to clear the internal RRset map (implementation detail).
        // The field name is inferred from the source – it is a ConcurrentSkipListMap.
        java.lang.reflect.Field mapField = Zone.class.getDeclaredField("rrsets");
        mapField.setAccessible(true);
        java.util.concurrent.ConcurrentSkipListMap<?, ?> emptyMap = new java.util.concurrent.ConcurrentSkipListMap<>();
        mapField.set(rawZone, emptyMap);

        String result = rawZone.toString(); // delegates to toMasterFile()
        assertThat(result).isEqualTo("$ORIGIN .\n");
    }

    /** Zone with only SOA and NS records – both must appear in order. */
    @Test
    void zoneWithSoaAndNsOnly() throws IOException {
        SOARecord soa = makeSOA(EXAMPLE);
        NSRecord ns = makeNS(EXAMPLE, "ns.example.");

        zone = createZone(EXAMPLE, soa, ns);

        String master = zone.toString();
        assertThat(master).contains(soa.toString());
        assertThat(master).contains(ns.toString());

        // Ensure the order: SOA first, then NS.
        int soaIdx = master.indexOf(soa.toString());
        int nsIdx = master.indexOf(ns.toString());
        assertTrue(soaIdx >= 0 && nsIdx > soaIdx,
                "SOA record should appear before NS record");
    }

    /** Custom $ORIGIN directive is correctly emitted. */
    @Test
    void customOriginIsPrinted() throws IOException {
        Name customOrigin = Name.fromConstantString("example.com.");
        SOARecord soa = makeSOA(customOrigin);
        zone = createZone(customOrigin, soa);

        String master = zone.toString();

        assertThat(master).startsWith("$ORIGIN example.com.\n");
        // The SOA line must follow the $ORIGIN line.
        assertThat(master).contains(soa.toString());
    }

    /** Removing a record must be reflected in the master‑file output. */
    @Test
    void recordRemovalIsReflected() throws IOException {
        // Build a zone with two NS records and an extra A record.
        NSRecord ns1 = makeNS(EXAMPLE, "ns1.example.");
        NSRecord ns2 = makeNS(EXAMPLE, "ns2.example.");
        ARecord a = new ARecord(new Name("example", EXAMPLE), DClass.IN, 3600,
                InetAddress.getByName("192.0.2.1"));
        zone = createZone(EXAMPLE, ns1, ns2, a);

        // Remove the A record.
        zone.removeRecord(a);

        String master = zone.toString();
        assertThat(master).contains(ns1.toString());
        assertThat(master).contains(ns2.toString());
        assertThat(master).doesNotContain(a.toString());
    }

    /** Propagation of IOException from toMasterFile() via a Mockito spy. */
    @Test
    void toStringPropagatesIOExceptionFromToMasterFile() throws IOException {
        // Create a normal zone and then spy it.
        SOARecord soa = makeSOA(EXAMPLE);
        NSRecord ns = makeNS(EXAMPLE, "ns.example.");
        Zone realZone = createZone(EXAMPLE, soa, ns);
        Zone spyZone = Mockito.spy(realZone);

        // Stub the private toMasterFile() method to throw IOException.
        // The method is package‑private (no modifier) in the source snippet,
        // so we can mock it directly.
        Mockito.doThrow(new IOException("forced")).when(spyZone).toMasterFile();

        // Verify that calling toString() results in the same IOException wrapped
        // in an unchecked exception (RuntimeException) because toString() cannot
        // declare a checked exception.
        RuntimeException ex = assertThrows(RuntimeException.class, spyZone::toString);
        assertThat(ex).hasCauseInstanceOf(IOException.class)
                     .hasMessageContaining("forced");
    }

    /** Creating a Zone with a null origin should throw NullPointerException. */
    @Test
    void nullOriginThrowsNPE() {
        // The public constructor expects a non‑null Name; passing null must NPE.
        assertThrows(NullPointerException.class,
                () -> new Zone(null, new Record[0]));
    }

    /** Verify that a fully populated zone (the one used in the original test) contains all records. */
    @Test
    void toStringContainsAllRecords() throws IOException {
        // Build the deterministic zone as in the original test.
        Name zoneName = EXAMPLE;

        SOARecord soa = makeSOA(zoneName);
        NSRecord ns1 = makeNS(zoneName, "ns1.example.");
        NSRecord ns2 = makeNS(zoneName, "ns2.example.");

        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");

        ARecord aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600,
                InetAddress.getByName("127.0.0.3"));
        ARecord aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
        AAAARecord aaaa1 = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6a);
        AAAARecord aaaa2 = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6b);
        ARecord aWild = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600,
                InetAddress.getByName("127.0.0.2"));
        TXTRecord txtWild = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");

        zone = createZone(zoneName,
                soa, ns1, ns2,
                aUnique, aTest, aaaa1, aaaa2,
                aWild, txtWild);

        String masterFile = zone.toString();

        // Verify each record's textual representation appears.
        List<Record> all = List.of(soa, ns1, ns2,
                aUnique, aTest, aaaa1, aaaa2,
                aWild, txtWild);
        for (Record r : all) {
            assertThat(masterFile).contains(r.toString());
        }

        // Basic sanity checks.
        assertThat(masterFile).isNotEmpty();
        assertThat(masterFile.trim()).startsWith("$ORIGIN");
    }
}
```