```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)   // no mocks are actually used, but keep the annotation as in the template
class Zone_toMasterFile_Test {

    /** Zone used for the majority of the tests – it contains an SOA, NS, A, two AAAA and two wildcard records. */
    private static Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Build the same zone that the original ZoneTest uses.
        Name zoneName = new Name("example.");
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");

        ARecord A_TEST = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
        AAAARecord AAAA_1_TEST = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6a);
        AAAARecord AAAA_2_TEST = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6b);
        ARecord A_WILD = new ARecord(new Name("*", zoneName), DClass.IN, 3600, localhost4);
        TXTRecord TXT_WILD = new TXTRecord(new Name("*", zoneName), DClass.IN, 3600, "sometext");

        Record[] records = new Record[]{
                new SOARecord(
                        zoneName,
                        DClass.IN,
                        3600L,
                        Name.fromConstantString("nameserver."),
                        new Name("hostmaster", zoneName),
                        1,
                        21600L,
                        7200L,
                        2160000L,
                        3600L),
                new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("nameserver.")),
                A_TEST,
                AAAA_1_TEST,
                AAAA_2_TEST,
                A_WILD,
                TXT_WILD,
        };

        zone = new Zone(zoneName, records);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Verify that {@link Zone#toMasterFile()} returns a string that contains all records
     * added to the zone and that the origin (SOA) appears before any other records.
     */
    @Test
    void toMasterFileContainsAllRecordsInCorrectOrder() {
        String master = zone.toMasterFile();

        // Basic sanity – the result must not be empty.
        assertFalse(master.isEmpty(), "master file string should not be empty");

        // Presence of the mandatory SOA and NS records.
        assertTrue(master.contains("example. 3600 IN SOA"), "SOA record missing");
        assertTrue(master.contains("example. 300 IN NS"), "NS record missing");

        // Presence of the A and AAAA records for the explicit name "test.example."
        assertTrue(master.contains("test.example. 3600 IN A 127.0.0.1"), "A record missing");
        assertTrue(master.contains("test.example. 3600 IN AAAA ::1"), "first AAAA record missing");
        assertTrue(master.contains("test.example. 3600 IN AAAA ::2"), "second AAAA record missing");

        // Presence of the wildcard records (they are stored with the wildcard name '*')
        assertTrue(master.contains("*.example. 3600 IN A 127.0.0.1"), "wildcard A record missing");
        assertTrue(master.contains("*.example. 3600 IN TXT \"sometext\""), "wildcard TXT record missing");

        // Verify that the SOA line appears before the NS line (origin node is emitted first)
        int soaIdx = master.indexOf("SOA");
        int nsIdx = master.indexOf("NS");
        assertTrue(soaIdx >= 0 && nsIdx >= 0, "Could not locate SOA or NS in the output");
        assertTrue(soaIdx < nsIdx, "SOA should appear before NS in the master file output");
    }

    /**
     * A minimal sanity check that an empty zone (no additional records) still produces a
     * non‑null, possibly empty, master‑file representation.
     */
    @Test
    void toMasterFileOnEmptyZone() throws IOException {
        Name emptyName = new Name("empty.");
        Zone emptyZone = new Zone(emptyName, new Record[0]);

        String master = emptyZone.toMasterFile();
        // The implementation iterates over the origin node first; for an empty zone the
        // origin node contains only the SOA (which we did not add), so the result should be empty.
        assertNotNull(master, "Result must not be null");
        assertTrue(master.isEmpty(), "Empty zone should produce an empty master file string");
    }
}
```