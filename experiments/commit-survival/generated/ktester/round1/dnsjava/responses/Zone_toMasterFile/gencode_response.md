```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Zone#toMasterFile()} covering normal behaviour as well as a
 * comprehensive set of edge‑cases (null fields, empty collections, etc.).
 *
 * <p>The test class follows the template that was provided.  All tests keep the
 * {@code @BeforeAll}/{@code @AfterAll} lifecycle methods untouched and only add new
 * test methods for the groups described in the JSON test case specification.</p>
 */
@ExtendWith(MockitoExtension.class)   // kept for compatibility with the original template
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

    /* ----------------------------------------------------------------------
     * Existing sanity checks (unchanged)
     * ---------------------------------------------------------------------- */

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
    void toMasterFileOnEmptyZone() throws Exception {
        Name emptyName = new Name("empty.");
        Zone emptyZone = new Zone(emptyName, new Record[0]);

        String master = emptyZone.toMasterFile();
        // The implementation iterates over the origin node first; for an empty zone the
        // origin node contains only the SOA (which we did not add), so the result should be empty.
        assertNotNull(master, "Result must not be null");
        assertTrue(master.isEmpty(), "Empty zone should produce an empty master file string");
    }

    /* ----------------------------------------------------------------------
     * Helper utilities for reflection based manipulation
     * ---------------------------------------------------------------------- */

    /** Sets a private field on the given {@link Zone} instance. */
    private static void setPrivateField(Zone z, String fieldName, Object value) throws Exception {
        Field f = Zone.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(z, value);
    }

    /** Retrieves a private field value from a {@link Zone} instance. */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Zone z, String fieldName) throws Exception {
        Field f = Zone.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(z);
    }

    /* ----------------------------------------------------------------------
     * Edge‑case tests derived from the JSON specification
     * ---------------------------------------------------------------------- */

    /**
     * Group: Empty zone
     * A newly created Zone with no origin and no data should produce an empty string.
     */
    @Test
    void emptyZoneProducesEmptyString() throws Exception {
        // Construct a Zone with null origin (by reflection) and an empty data map.
        Zone z = new Zone(new Name("empty."), new Record[0]);

        // Force both origin and originNode to null.
        setPrivateField(z, "origin", null);
        setPrivateField(z, "originNode", null);
        setPrivateField(z, "data", new TreeMap<>()); // empty map

        String result = z.toMasterFile();
        assertEquals("", result, "Empty zone should return an empty string");
    }

    /**
     * Group: Origin node only
     * Zone with a non‑null originNode and an empty data map.
     */
    @Test
    void originNodeOnly() throws Exception {
        // Create a zone that only contains the SOA (origin node) – no other records.
        Name name = new Name("solo.");
        SOARecord soa = new SOARecord(
                name,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.solo."),
                new Name("hostmaster", name),
                1, 7200L, 3600L, 1209600L, 3600L);
        Zone z = new Zone(name, new Record[]{soa});

        // The data map will be empty because the constructor only adds the SOA to the origin node.
        String master = z.toMasterFile();

        // The output must contain the SOA line and nothing else.
        assertTrue(master.contains("solo. 3600 IN SOA"), "SOA line missing");
        // No NS record was added, so ensure only one line (SOA) is present.
        assertEquals(1, master.trim().split("\n").length, "Only the origin node should be serialized");
    }

    /**
     * Group: Origin node plus one additional node
     * Zone where originNode is set and data contains a single entry whose key is different from the origin.
     */
    @Test
    void originNodePlusOneAdditionalNode() throws Exception {
        Name origin = new Name("origin.");
        Name other = new Name("other.origin.");

        // Build a zone with a SOA (origin node) and an extra A record.
        SOARecord soa = new SOARecord(
                origin,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.origin."),
                new Name("hostmaster", origin),
                1, 7200L, 3600L, 1209600L, 3600L);
        ARecord a = new ARecord(other, DClass.IN, 300, InetAddress.getByName("10.0.0.1"));
        Zone z = new Zone(origin, new Record[]{soa, a});

        String master = z.toMasterFile();

        // Verify ordering: SOA first, then the A record.
        int soaIdx = master.indexOf("SOA");
        int aIdx = master.indexOf("A 10.0.0.1");
        assertTrue(soaIdx >= 0 && aIdx >= 0, "Both SOA and A records must be present");
        assertTrue(soaIdx < aIdx, "Origin node must be serialized before additional nodes");
    }

    /**
     * Group: Multiple nodes with origin excluded
     * Zone with originNode and three distinct data entries (keys A, B, C) none equal to the origin.
     */
    @Test
    void multipleNodesOriginExcluded() throws Exception {
        Name origin = new Name("zone.");
        Name a = new Name("a.zone.");
        Name b = new Name("b.zone.");
        Name c = new Name("c.zone.");

        SOARecord soa = new SOARecord(
                origin,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.zone."),
                new Name("hostmaster", origin),
                1, 7200L, 3600L, 1209600L, 3600L);
        ARecord aRec = new ARecord(a, DClass.IN, 300, InetAddress.getByName("10.0.0.10"));
        ARecord bRec = new ARecord(b, DClass.IN, 300, InetAddress.getByName("10.0.0.11"));
        ARecord cRec = new ARecord(c, DClass.IN, 300, InetAddress.getByName("10.0.0.12"));

        Zone z = new Zone(origin, new Record[]{soa, aRec, bRec, cRec});

        String master = z.toMasterFile();

        // Expected order: SOA, then records for a.zone., b.zone., c.zone. (TreeMap natural order)
        List<String> lines = Arrays.asList(master.split("\n"));
        assertTrue(lines.get(0).contains("SOA"), "First line must be the SOA");
        assertTrue(lines.get(1).contains("a.zone."), "Second line must be a.zone.");
        assertTrue(lines.get(2).contains("b.zone."), "Third line must be b.zone.");
        assertTrue(lines.get(3).contains("c.zone."), "Fourth line must be c.zone.");
    }

    /**
     * Group: Data contains only the origin key
     * Zone where data contains an entry whose key equals the origin.
     * The loop must skip this entry, resulting in output that contains only the originNode serialization.
     */
    @Test
    void dataContainsOnlyOriginKey() throws Exception {
        Name origin = new Name("solo2.");
        // Only SOA and NS records (both belong to the origin name)
        SOARecord soa = new SOARecord(
                origin,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.solo2."),
                new Name("hostmaster", origin),
                1, 7200L, 3600L, 1209600L, 3600L);
        NSRecord ns = new NSRecord(origin, DClass.IN, 300L, Name.fromConstantString("ns.solo2."));

        Zone z = new Zone(origin, new Record[]{soa, ns});

        // The data map will contain an entry for the origin (the NS record). The method should skip it.
        String master = z.toMasterFile();

        // Both SOA and NS are part of the origin node, so they appear, but no duplicate line for the same key.
        assertTrue(master.contains("SOA"), "SOA must be present");
        assertTrue(master.contains("NS"), "NS must be present");
        // Ensure there is exactly one occurrence of the origin name.
        long count = master.lines().filter(l -> l.startsWith("solo2.")).count();
        assertEquals(2, count, "Only the origin node (SOA+NS) should be emitted");
    }

    /**
     * Group: Null originNode handling
     * Zone with a null originNode (nodeToString should safely ignore null) and a single data entry X.
     */
    @Test
    void nullOriginNodeHandledGracefully() throws Exception {
        Name origin = new Name("nullorigin.");
        Name x = new Name("x.nullorigin.");

        // Build a zone with a single A record (no SOA, so originNode will be null after reflection).
        ARecord a = new ARecord(x, DClass.IN, 300, InetAddress.getByName("10.1.2.3"));
        Zone z = new Zone(origin, new Record[]{a});

        // Force originNode to null.
        setPrivateField(z, "originNode", null);

        String master = z.toMasterFile();

        // The output should contain only the A record for x.nullorigin.
        assertTrue(master.contains("x.nullorigin. 300 IN A 10.1.2.3"), "A record should be present");
        // No SOA line because originNode was null.
        assertFalse(master.contains("SOA"), "SOA line must not be present when originNode is null");
    }

    /**
     * Group: Null origin and null originNode
     * Both {@code origin} and {@code originNode} are null and data map is empty.
     */
    @Test
    void nullOriginAndOriginNodeEmptyData() throws Exception {
        Zone z = new Zone(new Name("dummy."), new Record[0]);

        // Nullify both fields.
        setPrivateField(z, "origin", null);
        setPrivateField(z, "originNode", null);
        setPrivateField(z, "data", new TreeMap<>());

        String master = z.toMasterFile();
        assertEquals("", master, "Result should be empty when both origin and originNode are null and no data");
    }

    /**
     * Group: Null data map
     * Setting the internal {@code data} map to {@code null} must cause a {@link NullPointerException}.
     */
    @Test
    void nullDataMapThrowsNPE() throws Exception {
        Zone z = new Zone(new Name("bad."), new Record[0]);

        // Nullify the data map.
        setPrivateField(z, "data", null);

        assertThrows(NullPointerException.class, z::toMasterFile,
                "Calling toMasterFile with a null data map should throw NPE");
    }

    /**
     * Group: Null origin with populated data
     * The method should throw {@link NullPointerException} when {@code origin} is {@code null}
     * but the data map contains entries.
     */
    @Test
    void nullOriginWithDataThrowsNPE() throws Exception {
        Name origin = new Name("origin.");
        Name other = new Name("other.origin.");

        ARecord a = new ARecord(other, DClass.IN, 300, InetAddress.getByName("10.0.0.5"));
        Zone z = new Zone(origin, new Record[]{a});

        // Nullify origin.
        setPrivateField(z, "origin", null);

        assertThrows(NullPointerException.class, z::toMasterFile,
                "Null origin with non‑empty data should cause NPE");
    }

    /**
     * Group: Null node value in data map
     * If a map entry's value (node) is {@code null}, {@code nodeToString} will be invoked with {@code null}
     * and we expect a {@link NullPointerException}.
     */
    @Test
    void nullNodeValueInDataMapThrowsNPE() throws Exception {
        Name origin = new Name("origin.");
        Name key = new Name("key.origin.");

        // Create a Zone with a valid SOA so that originNode is non‑null.
        SOARecord soa = new SOARecord(
                origin,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.origin."),
                new Name("hostmaster", origin),
                1, 7200L, 3600L, 1209600L, 3600L);
        Zone z = new Zone(origin, new Record[]{soa});

        // Replace the internal data map with one that contains a null value.
        TreeMap<Name, Object> map = new TreeMap<>();
        map.put(key, null); // null node
        setPrivateField(z, "data", map);

        assertThrows(NullPointerException.class, z::toMasterFile,
                "Null node value in data map should cause NPE");
    }

    /**
     * Group: Null origin and null originNode with non‑empty data
     * Both {@code origin} and {@code originNode} are {@code null}, but the data map contains entries.
     * The first call to {@code nodeToString} is safe (null node), but the loop will try
     * {@code origin.equals(entry.getKey())} and throw {@link NullPointerException}.
     */
    @Test
    void nullOriginAndOriginNodeWithDataThrowsNPE() throws Exception {
        Name key = new Name("some.origin.");

        // Build a zone with a dummy record to get a non‑null data map.
        ARecord dummy = new ARecord(key, DClass.IN, 300, InetAddress.getByName("10.0.0.9"));
        Zone z = new Zone(key, new Record[]{dummy});

        // Nullify origin and originNode.
        setPrivateField(z, "origin", null);
        setPrivateField(z, "originNode", null);

        assertThrows(NullPointerException.class, z::toMasterFile,
                "Both origin and originNode null with data should cause NPE");
    }
}
```