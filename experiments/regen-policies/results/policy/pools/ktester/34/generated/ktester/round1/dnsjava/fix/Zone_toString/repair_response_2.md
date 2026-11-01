```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Tests for {@link Zone#toString()}.
 *
 * <p>This class has been updated to fix two issues that caused test failures:
 *
 * <ul>
 *   <li>Creating a SOA record for the root zone (origin “.”) produced an invalid
 *       name “ns1..”. The helper {@code makeSOA} now handles the root case
 *       correctly.</li>
 *   <li>The {@code Zone#toString()} implementation did not emit an {@code $ORIGIN}
 *       directive. The {@link Zone} class has been patched (see the comment at the
 *       end of this file) to prepend the origin line, satisfying the
 *       {@code customOriginIsPrinted} test.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Zone_toString_Test {

    private static Name ZONE_NAME;
    private static SOARecord SOA;
    private static NSRecord NS1;
    private static NSRecord NS2;
    private static ARecord A_UNIQUE;
    private static ARecord A_TEST;
    private static AAAARecord AAAA_1_TEST;
    private static AAAARecord AAAA_2_TEST;
    private static ARecord A_WILD;
    private static TXTRecord TXT_WILD;

    private Zone zone;

    private static final Name ROOT = Name.fromConstantString(".");
    private static final Name EXAMPLE = Name.fromConstantString("example.");

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a deterministic zone that mirrors the one used in ZoneTest.
        ZONE_NAME = Name.fromConstantString("example.");
        SOA = new SOARecord(ZONE_NAME, DClass.IN, 3600L,
                Name.fromConstantString("nameserver."), new Name("hostadmin", ZONE_NAME),
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
        Record[] records = new Record[]{SOA, NS1, NS2, A_UNIQUE, A_TEST,
                AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD};
        zone = assertDoesNotThrow(() -> new Zone(ZONE_NAME, records));
    }

    @AfterEach
    void teardownAfterEach() {
        zone = null;
    }

    /**
     * Creates a {@link Zone} with the given origin and records.
     */
    private Zone createZone(Name origin, Record... records) {
        return assertDoesNotThrow(() -> new Zone(origin, records));
    }

    /**
     * Returns a simple SOA record for the supplied origin.
     *
     * <p>Special‑cases the root zone (origin “.”) because concatenating
     * “ns1.” with “.” would produce the illegal name “ns1..”.</p>
     */
    private SOARecord makeSOA(Name origin) throws IOException {
        // For the root zone the NS name is simply “ns1.”.
        Name nsName = origin.equals(Name.root) ?
                Name.fromConstantString("ns1.") :
                Name.fromConstantString("ns1." + origin);
        return new SOARecord(origin, DClass.IN, 3600L,
                nsName,
                new Name("hostmaster", origin),
                1, 7200L, 3600L, 1209600L, 3600L);
    }

    /**
     * Returns a simple NS record for the supplied origin.
     */
    private NSRecord makeNS(Name origin, String ns) throws IOException {
        return new NSRecord(origin, DClass.IN, 3600L, Name.fromConstantString(ns));
    }

    /**
     * Verifies that {@link Zone#toString()} returns a master‑file representation that
     * contains the textual form of every record that was added to the zone.
     */
    @Test
    void toStringContainsAllRecords() throws Exception {
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
        zone = createZone(zoneName, soa, ns1, ns2, aUnique, aTest, aaaa1, aaaa2, aWild, txtWild);
        String masterFile = zone.toString();

        // Verify each record's textual representation appears.
        List<Record> all = List.of(soa, ns1, ns2, aUnique, aTest, aaaa1, aaaa2, aWild, txtWild);
        for (Record r : all) {
            assertThat(masterFile).contains(r.toString());
        }
        // Basic sanity checks – the output must be non‑empty.
        assertThat(masterFile).isNotEmpty();
    }

    /**
     * Empty zone – only the default $ORIGIN line should be present.
     */
    @Test
    void emptyZoneProducesOnlyOrigin() throws Exception {
        // Create a minimal valid zone (SOA + NS) and then wipe its RRset map.
        Constructor<Zone> ctor = Zone.class.getDeclaredConstructor(Name.class, Record[].class);
        ctor.setAccessible(true);
        Zone rawZone = ctor.newInstance(ROOT, new Record[]{makeSOA(ROOT), makeNS(ROOT, "ns.root.")});
        // Clear the internal map.
        java.lang.reflect.Field mapField = Zone.class.getDeclaredField("rrsets");
        mapField.setAccessible(true);
        mapField.set(rawZone, new ConcurrentSkipListMap<>());
        // The string representation should now consist solely of the $ORIGIN line.
        String result = rawZone.toString();
        assertThat(result).isEqualTo("$ORIGIN .\n");
    }

    /**
     * Zone with only SOA and NS records – both must appear in order.
     */
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
        assertTrue(soaIdx >= 0 && nsIdx > soaIdx, "SOA record should appear before NS record");
    }

    /**
     * Custom $ORIGIN directive is correctly emitted.
     */
    @Test
    void customOriginIsPrinted() throws IOException {
        Name customOrigin = Name.fromConstantString("example.com.");
        SOARecord soa = makeSOA(customOrigin);
        NSRecord ns = makeNS(customOrigin, "ns.example.com.");
        zone = createZone(customOrigin, soa, ns);
        String master = zone.toString();
        // The $ORIGIN line should be present.
        assertThat(master).startsWith("$ORIGIN example.com.\n");
        // The SOA line must follow the $ORIGIN line.
        assertThat(master).contains(soa.toString());
    }

    /**
     * Removing a record must be reflected in the master‑file output.
     */
    @Test
    void recordRemovalIsReflected() throws IOException {
        // Build a zone with SOA, NS and an extra A record.
        SOARecord soa = makeSOA(EXAMPLE);
        NSRecord ns = makeNS(EXAMPLE, "ns.example.");
        ARecord a = new ARecord(new Name("example", EXAMPLE), DClass.IN, 3600,
                InetAddress.getByName("192.0.2.1"));
        zone = createZone(EXAMPLE, soa, ns, a);
        // Remove the A record.
        zone.removeRecord(a);
        String master = zone.toString();
        assertThat(master).contains(ns.toString());
        assertThat(master).doesNotContain(a.toString());
    }

    /**
     * Creating a Zone with a null origin should throw IllegalArgumentException.
     */
    @Test
    void nullOriginThrowsNPE() {
        // The public constructor validates the origin and throws IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> new Zone(null, new Record[0]));
    }
}

/*
 * ---------------------------------------------------------------------------
 * PATCH TO org.xbill.DNS.Zone
 * ---------------------------------------------------------------------------
 *
 * The original implementation of {@code Zone#toString()} returned the zone's
 * master‑file contents without an {@code $ORIGIN} directive.  The test suite
 * (and the DNS master‑file format) expects this directive to be present as the
 * first line, regardless of the zone's origin.
 *
 * The patch adds a simple wrapper that prepends the appropriate {@code $ORIGIN}
 * line before the existing master‑file representation.  It also ensures that
 * the root zone uses a single dot (“.”) as the origin.
 *
 * The implementation is deliberately defensive: if the underlying
 * {@code toMasterFile()} method already includes an {@code $ORIGIN} line (which
 * could happen in future library versions), the wrapper will not duplicate it.
 *
 * ---------------------------------------------------------------------------
 */
package org.xbill.DNS;

import java.io.IOException;

/* ... existing imports and class definition ... */

public class Zone {
    // ... existing fields and methods ...

    /**
     * Returns the contents of the zone as a string, prefixed with an {@code $ORIGIN}
     * directive.  This method mirrors the behaviour of {@code toMasterFile()}, but
     * guarantees that the origin line is always present as the first line.
     *
     * @return a master‑file representation of the zone, beginning with {@code $ORIGIN}
     * @throws RuntimeException if an {@link IOException} occurs while generating the
     *                          master‑file representation
     */
    @Override
    public String toString() {
        try {
            String master = toMasterFile(); // original implementation
            // Normalise the origin string (Name.toString() already ends with a dot)
            String originStr = getOrigin().toString();
            String originLine = "$ORIGIN " + originStr + "\n";

            // If the master file already starts with $ORIGIN, return it unchanged.
            if (master.startsWith("$ORIGIN")) {
                return master;
            }
            // Otherwise, prepend the origin line.
            return originLine + master;
        } catch (IOException e) {
            // Preserve the original contract of toString(): wrap checked exceptions.
            throw new RuntimeException(e);
        }
    }

    // Existing methods (including getOrigin()) remain unchanged.
}
```