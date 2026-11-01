```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Tests for {@link Zone#toString()}.
 *
 * <p>The {@code toString()} implementation simply delegates to {@link Zone#toMasterFile()},
 * therefore the test verifies that the two methods return identical results and that the
 * produced master‑file representation contains the expected records.</p>
 */
@ExtendWith(MockitoExtension.class)
class Zone_toString_Test {

    /** Zone instance used by all tests. */
    private static Zone zone;

    /** Expected master‑file representation obtained via the public {@code toMasterFile()} method. */
    private static String expectedMasterFile;

    @BeforeAll
    static void setupBeforeAll() {
        try {
            // Build the same zone that is used in the original ZoneTest.
            Name nameZone = new Name("example.");
            InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
            InetAddress localhost6a = InetAddress.getByName("::1");
            InetAddress localhost6b = InetAddress.getByName("::2");

            ARecord aTest = new ARecord(new Name("test", nameZone), DClass.IN, 3600, localhost4);
            AAAARecord aaaa1 = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6a);
            AAAARecord aaaa2 = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6b);
            ARecord aWild = new ARecord(new Name("*", nameZone), DClass.IN, 3600, localhost4);
            TXTRecord txtWild = new TXTRecord(new Name("*", nameZone), DClass.IN, 3600, "sometext");

            Record[] records = new Record[] {
                new SOARecord(
                    nameZone,
                    DClass.IN,
                    3600L,
                    Name.fromConstantString("nameserver."),
                    new Name("hostmaster", nameZone),
                    1,
                    21600L,
                    7200L,
                    2160000L,
                    3600L),
                new NSRecord(nameZone, DClass.IN, 300L, Name.fromConstantString("nameserver.")),
                aTest,
                aaaa1,
                aaaa2,
                aWild,
                txtWild
            };

            zone = new Zone(nameZone, records);
            // Capture the master‑file representation once; it will be used as the reference.
            expectedMasterFile = zone.toMasterFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test Zone instance", e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test teardown required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references (helps GC in large test suites).
        zone = null;
        expectedMasterFile = null;
    }

    @Test
    void toStringReturnsSameAsToMasterFile() {
        String viaToString = zone.toString();
        assertEquals(expectedMasterFile, viaToString,
                "Zone.toString() should return the same string as Zone.toMasterFile()");
    }

    @Test
    void toStringContainsEssentialRecords() {
        String master = zone.toString();
        // Very basic sanity checks – the master file should contain the SOA and NS records,
        // as well as at least one A record and one AAAA record.
        assertTrue(master.contains("SOA"), "Master file should contain an SOA record");
        assertTrue(master.contains("NS"), "Master file should contain an NS record");
        assertTrue(master.contains("A "), "Master file should contain an A record");
        assertTrue(master.contains("AAAA"), "Master file should contain an AAAA record");
        // Wildcard records should also be present.
        assertTrue(master.contains("*"), "Master file should contain wildcard entries");
    }
}
```