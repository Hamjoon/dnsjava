```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link Zone#toString()}.
 *
 * <p>The {@code toString()} method is a thin delegate to {@link Zone#toMasterFile()}.
 * These tests verify that delegation works correctly, that the textual representation
 * follows the master‑file format, and that exceptional conditions are propagated
 * unchanged.
 */
@ExtendWith(MockitoExtension.class)
class Zone_toString_Test {

    private static final Name EXAMPLE = Name.fromConstantString("example.");
    private static final Name EXAMPLE_COM = Name.fromConstantString("example.com.");
    private static final Name EXAMPLE_ORG = Name.fromConstantString("example.org.");
    private static final Name EXAMPLE_NET = Name.fromConstantString("example.net.");

    private Zone zone;

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    /** Creates a fresh {@link Zone} with the supplied records. */
    private static Zone createZone(Name name, Record... records) throws IOException {
        return new Zone(name, records);
    }

    /** Returns a string with a trailing newline for each line supplied. */
    private static String lines(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l).append('\n');
        }
        return sb.toString();
    }

    /* ---------------------------------------------------------------------- */
    /* Test cases                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void toStringDelegatesToToMasterFile() throws IOException {
        zone = createZone(EXAMPLE,
                new SOARecord(EXAMPLE, DClass.IN, 3600,
                        Name.fromConstantString("ns1.example."),
                        new Name("hostmaster", EXAMPLE),
                        2023101501L, 7200, 3600, 1209600, 3600),
                new NSRecord(EXAMPLE, DClass.IN, 3600, Name.fromConstantString("ns1.example.")),
                new NSRecord(EXAMPLE, DClass.IN, 3600, Name.fromConstantString("ns2.example.")));
        String viaToString = zone.toString();
        String viaToMasterFile = zone.toMasterFile();

        assertNotNull(viaToString);
        assertEquals(viaToMasterFile, viaToString,
                "Zone#toString() must return exactly the same string as toMasterFile()");
    }

    @Test
    void emptyZoneProducesEmptyString() throws IOException {
        zone = createZone(EXAMPLE);
        assertEquals("", zone.toString(),
                "An empty zone should produce an empty master‑file representation");
    }

    @Test
    void zoneWithSingleSOARecord() throws IOException {
        SOARecord soa = new SOARecord(
                EXAMPLE_COM, DClass.IN, 3600,
                Name.fromConstantString("ns1.example.com."),
                new Name("hostmaster", EXAMPLE_COM),
                2023101501L, 7200, 3600, 1209600, 3600);
        zone = createZone(EXAMPLE_COM, soa);
        String expected = "example.com. 3600 IN SOA ns1.example.com. hostmaster.example.com. 2023101501 7200 3600 1209600 3600\n";
        assertEquals(expected, zone.toString(),
                "Single SOA record should be rendered as a single line ending with a newline");
    }

    @Test
    void zoneWithNSAndARecords() throws IOException {
        NSRecord ns1 = new NSRecord(EXAMPLE_ORG, DClass.IN, 3600, Name.fromConstantString("ns1.example.org."));
        NSRecord ns2 = new NSRecord(EXAMPLE_ORG, DClass.IN, 3600, Name.fromConstantString("ns2.example.org."));
        ARecord a = new ARecord(Name.fromConstantString("www.example.org."), DClass.IN, 3600,
                InetAddress.getByName("192.0.2.1"));
        zone = createZone(EXAMPLE_ORG, ns1, ns2, a);
        String expected = lines(
                "example.org. 3600 IN NS ns1.example.org.",
                "example.org. 3600 IN NS ns2.example.org.",
                "www.example.org. 3600 IN A 192.0.2.1");
        assertEquals(expected, zone.toString(),
                "NS and A records must appear in the order they were added");
    }

    @Test
    void zoneWithMixedRecordTypes() throws IOException {
        SOARecord soa = new SOARecord(
                EXAMPLE_NET, DClass.IN, 7200,
                Name.fromConstantString("ns1.example.net."),
                new Name("hostmaster", EXAMPLE_NET),
                2023110101L, 10800, 3600, 604800, 10800);
        NSRecord ns = new NSRecord(EXAMPLE_NET, DClass.IN, 7200,
                Name.fromConstantString("ns1.example.net."));
        MXRecord mx = new MXRecord(EXAMPLE_NET, DClass.IN, 7200, 10,
                Name.fromConstantString("mail.example.net."));
        TXTRecord txt = new TXTRecord(EXAMPLE_NET, DClass.IN, 7200,
                "v=spf1 include:_spf.example.net ~all");
        zone = createZone(EXAMPLE_NET, soa, ns, mx, txt);
        String expected = lines(
                "example.net. 7200 IN SOA ns1.example.net. hostmaster.example.net. 2023110101 10800 3600 604800 10800",
                "example.net. 7200 IN NS ns1.example.net.",
                "example.net. 7200 IN MX 10 mail.example.net.",
                "example.net. 7200 IN TXT \"v=spf1 include:_spf.example.net ~all\"");
        assertEquals(expected, zone.toString(),
                "Mixed record types must be concatenated preserving order and spacing");
    }

    @Test
    void exceptionPropagation_ioException() {
        Zone faulty = new Zone(EXAMPLE) {
            @Override
            public String toMasterFile() throws IOException {
                throw new IOException("forced");
            }
        };
        IOException ex = assertThrows(IOException.class, faulty::toString,
                "IOException from toMasterFile must propagate through toString()");
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void idempotentToString() throws IOException {
        zone = createZone(EXAMPLE,
                new SOARecord(EXAMPLE, DClass.IN, 3600,
                        Name.fromConstantString("ns1.example."),
                        new Name("hostmaster", EXAMPLE),
                        2023101501L, 7200, 3600, 1209600, 3600),
                new NSRecord(EXAMPLE, DClass.IN, 3600, Name.fromConstantString("ns1.example.")),
                new ARecord(Name.fromConstantString("host.example."), DClass.IN, 3600,
                        InetAddress.getByName("192.0.2.10")));
        String first = zone.toString();
        String second = zone.toString();
        assertEquals(first, second,
                "Consecutive calls to toString must return identical strings and not mutate state");
    }

    @Test
    void zoneAfterRemovalOfRecord() throws IOException {
        NSRecord ns1 = new NSRecord(EXAMPLE_ORG, DClass.IN, 3600, Name.fromConstantString("ns1.example.org."));
        NSRecord ns2 = new NSRecord(EXAMPLE_ORG, DClass.IN, 3600, Name.fromConstantString("ns2.example.org."));
        ARecord a = new ARecord(Name.fromConstantString("host.example.org."), DClass.IN, 3600,
                InetAddress.getByName("192.0.2.5"));
        zone = createZone(EXAMPLE_ORG, ns1, ns2, a);
        zone.removeRecord(a);
        String expected = lines(
                "example.org. 3600 IN NS ns1.example.org.",
                "example.org. 3600 IN NS ns2.example.org.");
        assertEquals(expected, zone.toString(),
                "After removal, only remaining records should be rendered");
    }

    @Test
    void multipleRecordsSameNameAndType() throws IOException {
        ARecord a1 = new ARecord(Name.fromConstantString("www.example.com."), DClass.IN, 300,
                InetAddress.getByName("192.0.2.1"));
        ARecord a2 = new ARecord(Name.fromConstantString("www.example.com."), DClass.IN, 300,
                InetAddress.getByName("192.0.2.2"));
        zone = createZone(EXAMPLE_COM, a1, a2);
        String expected = lines(
                "www.example.com. 300 IN A 192.0.2.1",
                "www.example.com. 300 IN A 192.0.2.2");
        assertEquals(expected, zone.toString(),
                "Both A records must appear in insertion order");
    }

    @Test
    void orderPreservationAddRRsetThenAddRecord() throws IOException {
        // Build an RRset containing a single MX record
        MXRecord mx = new MXRecord(EXAMPLE_NET, DClass.IN, 7200, 10,
                Name.fromConstantString("mail.example.net."));
        RRset mxSet = new RRset();
        mxSet.addRR(mx);
        // Add the RRset and then an A record
        ARecord a = new ARecord(Name.fromConstantString("example.net."), DClass.IN, 7200,
                InetAddress.getByName("203.0.113.5"));
        zone = createZone(EXAMPLE_NET);
        zone.addRRset(mxSet);
        zone.addRecord(a);
        String expected = lines(
                "example.net. 7200 IN MX 10 mail.example.net.",
                "example.net. 7200 IN A 203.0.113.5");
        assertEquals(expected, zone.toString(),
                "Insertion order across addRRset and addRecord must be preserved");
    }

    @Test
    void zeroTtlRecordHandling() throws IOException {
        ARecord zeroTtl = new ARecord(Name.fromConstantString("host.example.org."), DClass.IN, 0,
                InetAddress.getByName("198.51.100.7"));
        zone = createZone(Name.fromConstantString("example.org."), zeroTtl);
        String expected = "host.example.org. 0 IN A 198.51.100.7\n";
        assertEquals(expected, zone.toString(),
                "Zero TTL must be rendered as the literal \"0\"");
    }

    @Test
    void largeZoneLineCountVerification() throws IOException {
        List<Record> records = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String name = "host" + i + ".example.com.";
            ARecord a = new ARecord(Name.fromConstantString(name), DClass.IN, 3600,
                    InetAddress.getByName("192.0.2." + i));
            records.add(a);
        }
        zone = createZone(Name.fromConstantString("example.com."), records.toArray(new Record[0]));
        String master = zone.toString();
        long newlineCount = master.chars().filter(ch -> ch == '\n').count();
        assertEquals(50, newlineCount,
                "A zone with 50 distinct A records must contain exactly 50 newline‑terminated lines");
    }

    @Test
    void consistencyWithToMasterFile() throws IOException {
        zone = createZone(EXAMPLE,
                new SOARecord(EXAMPLE, DClass.IN, 3600,
                        Name.fromConstantString("ns1.example."),
                        new Name("hostmaster", EXAMPLE),
                        2023101501L, 7200, 3600, 1209600, 3600),
                new NSRecord(EXAMPLE, DClass.IN, 3600, Name.fromConstantString("ns1.example.")),
                new ARecord(Name.fromConstantString("www.example."), DClass.IN, 3600,
                        InetAddress.getByName("192.0.2.1")));
        assertEquals(zone.toMasterFile(), zone.toString(),
                "toString must always be identical to toMasterFile");
    }

    @Test
    void runtimeExceptionPropagation() {
        Zone faulty = new Zone(EXAMPLE) {
            @Override
            public String toMasterFile() {
                throw new IllegalStateException("forced");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, faulty::toString,
                "Unchecked exceptions must propagate unchanged");
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void concurrentModificationExceptionPropagation() {
        Zone faulty = new Zone(EXAMPLE) {
            @Override
            public String toMasterFile() {
                throw new java.util.ConcurrentModificationException("forced");
            }
        };
        java.util.ConcurrentModificationException ex =
                assertThrows(java.util.ConcurrentModificationException.class, faulty::toString,
                        "ConcurrentModificationException must propagate");
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void nullPointerExceptionFromInternalState() {
        Zone faulty = new Zone(EXAMPLE) {
            @Override
            public String toMasterFile() {
                // Simulate a null internal map leading to NPE
                throw new NullPointerException("forced");
            }
        };
        NullPointerException ex = assertThrows(NullPointerException.class, faulty::toString,
                "NullPointerException must propagate");
        assertEquals("forced", ex.getMessage());
    }
}
```