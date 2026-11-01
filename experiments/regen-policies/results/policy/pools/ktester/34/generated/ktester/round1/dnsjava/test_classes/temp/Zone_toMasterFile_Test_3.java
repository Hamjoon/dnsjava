package org.xbill.DNS;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.ArrayList;
import java.util.List;

class Zone_toMasterFile_Test {

    private Name zoneName;

    private SOARecord soa;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aRecord;

    private AAAARecord aaaaRecord;

    private ARecord wildA;

    private TXTRecord wildTXT;

    private Zone zone;

    /**
     * The origin name of the zone.
     */
    private Name origin;

    /**
     * Optional node that represents the origin (SOA/NS).
     */
    private Object originNode;

    /**
     * Map from name → node (Record or List&lt;Record&gt;).
     */
    private final Map<Name, Object> data = new ConcurrentSkipListMap<>();

    /* --------------------------------------------------------------------- */
    private static final class DummyNode {

        private final String marker;

        DummyNode(String marker) {
            this.marker = marker;
        }

        @Override
        public String toString() {
            return marker;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a deterministic zone – the same data that is used in the original
        // ZoneTest class, but limited to a few records that are easy to verify.
        zoneName = Name.fromConstantString("example.");
        soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        aRecord = new ARecord(new Name("www", zoneName), DClass.IN, 3600, localhost4);
        aaaaRecord = new AAAARecord(new Name("www", zoneName), DClass.IN, 3600, InetAddress.getByName("::1"));
        wildA = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        wildTXT = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");
        Record[] records = new Record[] { soa, ns1, ns2, aRecord, aaaaRecord, wildA, wildTXT };
        zone = new Zone(zoneName, records);
        zoneName = Name.fromConstantString("example.");
        /* Create a zone with *no* records – we will inject everything we need
         * via reflection in each test case. */
        zone = new Zone(zoneName, new Record[0]);
    }

    /* --------------------------------------------------------------------- */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /* -----------------------------------------------------------------
     *  Helper used by the constructor to populate the internal map.
     * ----------------------------------------------------------------- */
    private void maybeAddRecord(Record r) {
        if (r == null) {
            return;
        }
        Name name = r.getName();
        Object existing = data.get(name);
        if (existing == null) {
            data.put(name, r);
        } else if (existing instanceof Record) {
            List<Record> list = new ArrayList<>();
            list.add((Record) existing);
            list.add(r);
            data.put(name, list);
        } else if (existing instanceof List) {
            @SuppressWarnings("unchecked")
            List<Record> list = (List<Record>) existing;
            list.add(r);
        } else {
            // Should never happen with the test suite, but we keep the
            // behaviour deterministic.
            throw new IllegalStateException("Unexpected node type in zone data map");
        }
    }

    /**
     * Returns the contents of the zone in master‑file format.
     *
     * The format required by the tests is a simple concatenation of the
     * {@code toString()} representation of each node, separated by a
     * single space.  The origin node (if present) is rendered first,
     * followed by the entries in {@code data} ordered by {@code Name}.
     *
     * @return a master‑file representation of the zone
     * @throws NullPointerException if the zone {@code origin} is {@code null}
     *                              or if a node value in the map is {@code null}
     * @throws ClassCastException   if a key in the map is not a {@code Name}
     */
    public String toMasterFile() {
        if (origin == null) {
            throw new NullPointerException("origin is null");
        }
        StringBuilder sb = new StringBuilder();
        // Render the explicit origin node first (if any)
        if (originNode != null) {
            sb.append(originNode.toString()).append(' ');
        }
        // Iterate over the map in natural order of Name
        for (Map.Entry<Name, Object> e : data.entrySet()) {
            // may throw ClassCastException
            Name key = e.getKey();
            // may be null → NPE
            Object node = e.getValue();
            if (node == null) {
                throw new NullPointerException("node value is null for name " + key);
            }
            // Skip the entry whose key equals the zone origin – the origin
            // records are represented by originNode.
            if (origin.equals(key)) {
                continue;
            }
            // The node can be a single Record, a List<Record>, or any
            // other object (e.g., DummyNode used in the tests).
            if (node instanceof Record) {
                sb.append(node.toString()).append(' ');
            } else if (node instanceof List) {
                @SuppressWarnings("unchecked")
                List<Record> list = (List<Record>) node;
                for (Record r : list) {
                    sb.append(r.toString()).append(' ');
                }
            } else {
                sb.append(node.toString()).append(' ');
            }
        }
        return sb.toString();
    }

    /**
     * Returns the zone origin.
     */
    public Name getOrigin() {
        return origin;
    }

    /**
     * Returns the origin node (used via reflection in tests).
     */
    public Object getOriginNode() {
        return originNode;
    }

    /**
     * Allows the tests to replace the origin node via reflection.
     */
    public void setOriginNode(Object node) {
        this.originNode = node;
    }

    /**
     * Allows the tests to replace the origin via reflection.
     */
    public void setOrigin(Name origin) {
        this.origin = origin;
    }

    @Test
    void toMasterFile_containsAllRecordsInCorrectOrder() throws Exception {
        // Build a realistic zone using the public constructor (records defined in the original suite)
        // This test is kept for regression – it validates normal behaviour.
        SOARecord soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        NSRecord ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NSRecord ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        ARecord aRecord = new ARecord(new Name("www", zoneName), DClass.IN, 3600, InetAddress.getLoopbackAddress());
        AAAARecord aaaaRecord = new AAAARecord(new Name("www", zoneName), DClass.IN, 3600, InetAddress.getByName("::1"));
        ARecord wildA = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        TXTRecord wildTXT = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");
        Record[] records = new Record[] { soa, ns1, ns2, aRecord, aaaaRecord, wildA, wildTXT };
        Zone realZone = new Zone(zoneName, records);
        String masterFile = realZone.toMasterFile();
        assertThat(masterFile).startsWith(soa.toString());
        assertThat(masterFile).contains(ns1.toString(), ns2.toString(), aRecord.toString(), aaaaRecord.toString(), wildA.toString(), wildTXT.toString());
        long occurrence = masterFile.lines().filter(line -> line.contains(zoneName.toString())).count();
        assertThat(occurrence).isEqualTo(1L);
    }

    @Test
    void toMasterFile_isIdempotent() {
        String first = zone.toMasterFile();
        String second = zone.toMasterFile();
        // The method should be pure – repeated calls must return identical strings
        assertThat(first).isEqualTo(second);
    }

    @SuppressWarnings("unchecked")
    private static Map<Name, Object> getDataMap(Zone z) throws Exception {
        return data;
    }

    /* --------------------------------------------------------------------- */
    @Test
    void originNode_alwaysRendered_whenNoOtherRecords() throws Exception {
        // originNode = DummyNode("ORIGIN:originNode; ")
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // data map stays empty
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("ORIGIN:originNode; ");
    }

    @Test
    void originNode_notRenderedForEntryMatchingOrigin() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // put a single entry whose key equals the zone origin
        Map<Name, Object> data = getDataMap(zone);
        data.put(zoneName, new DummyNode("NODE:shouldNotAppear; "));
        String master = zone.toMasterFile();
        // only the origin marker must be present
        assertThat(master).isEqualTo("ORIGIN:originNode; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nonOriginEntry_isRendered() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        Map<Name, Object> data = getDataMap(zone);
        Name a = Name.fromConstantString("a.example.");
        data.put(a, new DummyNode("NODE:nodeA; "));
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("ORIGIN:originNode; NODE:nodeA; ");
    }

    @Test
    void multipleNonOriginEntries_areRenderedInMapOrder() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        Map<Name, Object> data = getDataMap(zone);
        Name a = Name.fromConstantString("a.example.");
        Name b = Name.fromConstantString("b.example.");
        data.put(a, new DummyNode("NODE:nodeA; "));
        data.put(b, new DummyNode("NODE:nodeB; "));
        String master = zone.toMasterFile();
        // ConcurrentSkipListMap preserves natural order of Name,
        // which for these names is a.example. then b.example.
        assertThat(master).isEqualTo("ORIGIN:originNode; NODE:nodeA; NODE:nodeB; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptyZone_containsOnlyOrigin() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // data map is already empty
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("ORIGIN:originNode; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullOriginNode_andEmptyData_returnsEmptyString() throws Exception {
        setField(zone, "originNode", null);
        // data empty
        String master = zone.toMasterFile();
        assertThat(master).isEmpty();
    }

    @Test
    void nullOriginNode_withNonOriginEntry_rendersOnlyEntry() throws Exception {
        setField(zone, "originNode", null);
        Map<Name, Object> data = getDataMap(zone);
        Name a = Name.fromConstantString("a.example.");
        data.put(a, new DummyNode("NODE:nodeA; "));
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("NODE:nodeA; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mixedEntries_originKeySkipped_othersRendered() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        Map<Name, Object> data = getDataMap(zone);
        // entry that matches origin – should be skipped
        data.put(zoneName, new DummyNode("NODE:originKey; "));
        // two genuine non‑origin entries
        Name a = Name.fromConstantString("a.example.");
        Name b = Name.fromConstantString("b.example.");
        data.put(a, new DummyNode("NODE:nodeA; "));
        data.put(b, new DummyNode("NODE:nodeB; "));
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("ORIGIN:originNode; NODE:nodeA; NODE:nodeB; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullOrigin_throwsNullPointerException() throws Exception {
        // force origin to null
        setField(zone, "origin", null);
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // any iteration over data will hit the NPE
        assertThatThrownBy(() -> zone.toMasterFile()).isInstanceOf(NullPointerException.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullNodeValue_throwsNullPointerException() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        Map<Name, Object> data = getDataMap(zone);
        Name a = Name.fromConstantString("a.example.");
        // deliberately null node
        data.put(a, null);
        assertThatThrownBy(() -> zone.toMasterFile()).isInstanceOf(NullPointerException.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void incompatibleKeyType_throwsClassCastException() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // bypass the generic type safety via raw map access
        @SuppressWarnings("unchecked")
        Map rawMap = getDataMap(zone);
        rawMap.put("not-a-Name-key", new DummyNode("NODE:nodeA; "));
        assertThatThrownBy(() -> zone.toMasterFile()).isInstanceOf(ClassCastException.class);
    }
}
