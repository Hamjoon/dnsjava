```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Zone_removeRecord_Test {

    private static final String ZONE = "example.";
    private Name zoneName;
    private SOARecord soa;
    private NSRecord ns;
    private ARecord a1;
    private ARecord a2;
    private AAAARecord aaaa;
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        zoneName = Name.fromConstantString(ZONE);
        soa = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);
        ns = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns.example."));

        // Two A records with the same name – used to test removal of one record from a multi‑record RRset
        Name dupName = new Name("dup", zoneName);
        a1 = new ARecord(dupName, DClass.IN, 3600, InetAddress.getByName("127.0.0.10"));
        a2 = new ARecord(dupName, DClass.IN, 3600, InetAddress.getByName("127.0.0.11"));

        // A single AAAA record – used to test removal of the only record in an RRset
        aaaa = new AAAARecord(new Name("only", zoneName), DClass.IN, 3600,
                InetAddress.getByName("::1"));

        // Build a zone containing SOA, NS and the above records
        Record[] records = new Record[]{soa, ns, a1, a2, aaaa};
        zone = new Zone(zoneName, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void removeRecord_NullArgument_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> zone.removeRecord(null));
    }

    @Test
    void removeRecord_SOARecord_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> zone.removeRecord(soa));
    }

    @Test
    void removeRecord_LastNSRecord_throwsIllegalArgumentException() {
        // The zone currently has only one NS record (ns). Trying to remove it must fail.
        assertThrows(IllegalArgumentException.class, () -> zone.removeRecord(ns));
    }

    @Test
    void removeRecord_OneOfMultipleSameNameRecords_leavesRRsetIntact() {
        // Remove a1; a2 should still be present.
        zone.removeRecord(a1);
        Record remaining = zone.findExactMatch(a2.getName(), Type.A);
        assertNotNull(remaining);
        assertEquals(a2, remaining);
    }

    @Test
    void removeRecord_OnlyRecordInRRset_removesRRset() {
        // Remove the sole AAAA record; the RRset should disappear.
        zone.removeRecord(aaaa);
        Record after = zone.findExactMatch(aaaa.getName(), Type.AAAA);
        assertNull(after);
    }

    @Test
    void removeRecord_NonExistingRecord_isNoOp() {
        // Create a record that is not part of the zone.
        Record notInZone = new ARecord(new Name("ghost", zoneName), DClass.IN, 3600,
                InetAddress.getByName("127.0.0.99"));
        // Should not throw and the zone should stay unchanged.
        assertDoesNotThrow(() -> zone.removeRecord(notInZone));

        // Verify that existing records are still present.
        assertNotNull(zone.findExactMatch(a1.getName(), Type.A));
        assertNotNull(zone.findExactMatch(aaaa.getName(), Type.AAAA));
    }
}
```