package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

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

    @Test
    void toMasterFile_containsAllRecordsInCorrectOrder() {
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
        Field f = Zone.class.getDeclaredField("data");
        f.setAccessible(true);
        return (Map<Name, Object>) f.get(z);
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
