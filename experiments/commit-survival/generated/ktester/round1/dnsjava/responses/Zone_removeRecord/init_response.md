```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Tests for {@link Zone#removeRecord(Record)}.
 *
 * The tests cover three scenarios:
 * <ul>
 *   <li>Removing a record that is the sole member of its RRset – the RRset should be removed.</li>
 *   <li>Removing a record from an RRset that contains multiple records – only that record is removed.</li>
 *   <li>Attempting to remove a record that does not exist – the zone must remain unchanged.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Zone_removeRecord_Test {

    /* ---------- static fixtures (shared across all tests) ---------- */
    private static Name ZONE_NAME;
    private static ARecord A_RECORD;
    private static AAAARecord AAAA1_RECORD;
    private static AAAARecord AAAA2_RECORD;
    private static SOARecord SOA_RECORD;
    private static NSRecord NS_RECORD;

    /* ---------- per‑test fixture ---------- */
    private Zone zone; // a fresh Zone for each test

    /* ---------- helpers ---------- */
    /** Creates a fresh {@link Zone} instance populated with the static records. */
    private Zone createZone() throws IOException {
        Record[] records = new Record[] {
                SOA_RECORD,
                NS_RECORD,
                A_RECORD,
                AAAA1_RECORD,
                AAAA2_RECORD
        };
        return new Zone(ZONE_NAME, records);
    }

    /* ---------- JUnit lifecycle ---------- */
    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // zone name "example."
        ZONE_NAME = new Name("example.");

        // common records used in every test
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");

        A_RECORD = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA1_RECORD = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA2_RECORD = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);

        // minimal SOA and NS records – required for a valid Zone
        SOA_RECORD = new SOARecord(
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
        NS_RECORD = new NSRecord(ZONE_NAME, DClass.IN, 300L,
                Name.fromConstantString("ns.example."));
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        zone = createZone();
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up.
        zone = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nullify static references for GC friendliness.
        ZONE_NAME = null;
        A_RECORD = null;
        AAAA1_RECORD = null;
        AAAA2_RECORD = null;
        SOA_RECORD = null;
        NS_RECORD = null;
    }

    /* ---------- tests ---------- */

    @Test
    void removeSingleRecord_RemovesEntireRRset() {
        // Pre‑condition: the A record exists as a sole member of its RRset.
        RRset before = zone.findExactMatch(A_RECORD.getName(), Type.A);
        assertNotNull(before, "A RRset should exist before removal");
        assertEquals(1, before.size());

        // Act
        zone.removeRecord(A_RECORD);

        // Post‑condition: the RRset is gone.
        RRset after = zone.findExactMatch(A_RECORD.getName(), Type.A);
        assertNull(after, "RRset should be removed after deleting its only record");
    }

    @Test
    void removeRecordFromMultiRecordRRset_LeavesRemainingRecords() {
        // Pre‑condition: two AAAA records share the same name.
        RRset before = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);
        assertNotNull(before, "AAAA RRset should exist before removal");
        assertEquals(2, before.size());

        // Act – delete the first AAAA record.
        zone.removeRecord(AAAA1_RECORD);

        // Post‑condition: RRset still exists but contains only the second record.
        RRset after = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);
        assertNotNull(after, "RRset must still exist after removing one of multiple records");
        assertEquals(1, after.size());

        // Verify that the remaining record is AAAA2_RECORD.
        Record remaining = after.first();
        assertEquals(AAAA2_RECORD, remaining, "The remaining record should be the second AAAA entry");
    }

    @Test
    void removeNonExistingRecord_DoesNotAlterZone() throws IOException {
        // Create a record that was never added to the zone.
        InetAddress other = InetAddress.getByName("192.0.2.1");
        ARecord notInZone = new ARecord(new Name("absent", ZONE_NAME), DClass.IN, 3600, other);

        // Capture state before removal attempt.
        RRset aSetBefore = zone.findExactMatch(A_RECORD.getName(), Type.A);
        RRset aaaaSetBefore = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);

        // Act – attempt to remove a record that does not exist.
        zone.removeRecord(notInZone);

        // Verify that existing RRsets are unchanged.
        RRset aSetAfter = zone.findExactMatch(A_RECORD.getName(), Type.A);
        RRset aaaaSetAfter = zone.findExactMatch(AAAA1_RECORD.getName(), Type.AAAA);

        assertEquals(aSetBefore, aSetAfter, "A RRset must remain unchanged");
        assertEquals(aaaaSetBefore, aaaaSetAfter, "AAAA RRset must remain unchanged");
    }
}
```