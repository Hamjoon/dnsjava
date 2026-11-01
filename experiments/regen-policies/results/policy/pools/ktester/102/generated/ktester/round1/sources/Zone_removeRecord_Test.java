package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.InetAddress;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

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

    @BeforeEach
    void setupBeforeEach() throws IOException {
        zoneName = Name.fromConstantString(ZONE);
        soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        ns = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns.example."));
        // Two A records with the same name – used to test removal of one record from a multi‑record RRset
        Name dupName = new Name("dup", zoneName);
        a1 = new ARecord(dupName, DClass.IN, 3600, InetAddress.getByName("127.0.0.10"));
        a2 = new ARecord(dupName, DClass.IN, 3600, InetAddress.getByName("127.0.0.11"));
        // A single AAAA record – used to test removal of the only record in an RRset
        aaaa = new AAAARecord(new Name("only", zoneName), DClass.IN, 3600, InetAddress.getByName("::1"));
        // Build a zone containing SOA, NS and the above records
        Record[] records = new Record[] { soa, ns, a1, a2, aaaa };
        zone = new Zone(zoneName, records);
    }

    /**
     * Helper that creates a mock {@link Record} with the requested type and name.
     */
    private static Record mockRecord(int type, Name name) {
        Record r = org.mockito.Mockito.mock(Record.class);
        org.mockito.Mockito.when(r.getType()).thenReturn(type);
        org.mockito.Mockito.when(r.getRRsetType()).thenReturn(type);
        org.mockito.Mockito.when(r.getName()).thenReturn(name);
        return r;
    }

    /**
     * Provides the data for the "removeRecord" group (excluding the already‑tested cases).
     */
    private static Stream<Arguments> removeRecordProvider() {
        Name example = Name.fromConstantString(ZONE);
        Name other = Name.fromConstantString("other.example.");
        // 1. No RRset – name/type not present in the zone
        Record noRRset = mockRecord(Type.A, other);
        // 2. Multi‑record A RRset – we already have a1/a2 in the zone; use a1 as the target
        // real record; zone contains two A records
        Record multiA = a1;
        // 3. Single A record (no NS) – we will construct a fresh zone that only contains an A RRset
        //    (plus the mandatory SOA and NS).  The test method will rebuild the zone.
        // 4. Record not present in an existing RRset – same name/type as a1 but a different instance
        Record notPresent = mockRecord(Type.A, a1.getName());
        // 5. TXT single record that is the only RRset for its name
        Record txt = new TXTRecord(Name.fromConstantString("orphan.example." + ZONE), DClass.IN, 300, "only txt");
        // 6. NS multi‑record – we will add an extra NS to the zone in the test method.
        Record extraNs = new NSRecord(Name.fromConstantString("extra.example." + ZONE), DClass.IN, 300, Name.fromConstantString("ns2.example."));
        return Stream.of(Arguments.of("NoRRset", noRRset, false, false), Arguments.of("MultiA", multiA, true, false), // special handling in test
        Arguments.of("SingleA_NoNS", null, false, true), Arguments.of("RecordNotInRRset", notPresent, true, false), Arguments.of("TXT_SingleOrphan", txt, false, true), Arguments.of("NS_Multi", extraNs, true, false));
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
        Record notInZone = new ARecord(new Name("ghost", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.99"));
        // Should not throw and the zone should stay unchanged.
        assertDoesNotThrow(() -> zone.removeRecord(notInZone));
        // Verify that existing records are still present.
        assertNotNull(zone.findExactMatch(a1.getName(), Type.A));
        assertNotNull(zone.findExactMatch(aaaa.getName(), Type.AAAA));
    }

    /**
     * Parameterised test that exercises the various logical branches of {@code removeRecord}.
     *
     * <p>The {@code expectsDeletion} flag indicates whether the RRset should still exist after the
     * call (i.e. the record was deleted from a multi‑record set).  The {@code expectsRRsetRemoval}
     * flag indicates that the whole RRset (and possibly the name) must be removed.</p>
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("removeRecordProvider")
    void removeRecord_variousScenarios(String caseId, Record target, boolean expectsDeletion, boolean expectsRRsetRemoval) throws Exception {
        // -----------------------------------------------------------------
        // 1. Prepare a zone that matches the scenario requirements
        // -----------------------------------------------------------------
        // start from the baseline zone created in @BeforeEach
        Zone testZone = zone;
        switch(caseId) {
            case "SingleA_NoNS":
                // Build a zone that contains only a single A record (plus mandatory SOA/NS)
                Name aName = Name.fromConstantString("single.example." + ZONE);
                ARecord soleA = new ARecord(aName, DClass.IN, 300, InetAddress.getByName("127.0.0.42"));
                testZone = new Zone(zoneName, new Record[] { soa, ns, soleA });
                target = soleA;
                break;
            case "NS_Multi":
                // Add an extra NS record so that the NS RRset size becomes > 1
                testZone.addRecord((NSRecord) extraNs);
                // remove one of the NS records
                target = ns;
                break;
            case "TXT_SingleOrphan":
                // Add a TXT record that lives under a distinct name; the zone already has SOA/NS
                testZone.addRecord((TXTRecord) txt);
                break;
            default:
                // other cases already have a suitable zone
                break;
        }
        // -----------------------------------------------------------------
        // 2. Invoke the method under test
        // -----------------------------------------------------------------
        assertDoesNotThrow(() -> testZone.removeRecord(target));
        // -----------------------------------------------------------------
        // 3. Verify the observable outcome
        // -----------------------------------------------------------------
        int type = target.getRRsetType();
        Name name = target.getName();
        RRset rrsetAfter = testZone.findExactMatch(name, type);
        if (expectsRRsetRemoval) {
            // The whole RRset (and possibly the name) must have vanished
            assertNull(rrsetAfter, "RRset should have been removed for case " + caseId);
        } else if (expectsDeletion) {
            // The RRset must still exist but the specific record should be gone.
            assertNotNull(rrsetAfter, "RRset must still be present for case " + caseId);
            // For real Record instances we can check that the RRset no longer contains it.
            // For mock records we cannot inspect the internal collection, so we simply assert
            // that the RRset size is smaller than before.
            // A simple heuristic: if the zone originally had two A records, after removal
            // the size should be 1.
            if (type == Type.A && name.equals(a1.getName())) {
                assertEquals(1, rrsetAfter.size(), "RRset size should have decreased after deleting one A record");
            }
        } else {
            // No change expected – the RRset should be exactly as before.
            assertNotNull(rrsetAfter, "RRset must still be present for case " + caseId);
        }
        // Additional sanity checks for special cases
        if (caseId.equals("TXT_SingleOrphan")) {
            // The orphan name should no longer be present in the zone iterator.
            boolean nameFound = false;
            for (RRset rs : testZone) {
                if (rs.getName().equals(name)) {
                    nameFound = true;
                    break;
                }
            }
            assertFalse(nameFound, "Orphan name should have been removed after TXT deletion");
        }
        if (caseId.equals("NS_Multi")) {
            // The NS RRset must still contain at least one NS record after removal.
            assertNotNull(rrsetAfter);
            assertTrue(rrsetAfter.size() >= 1, "NS RRset must still contain records");
        }
    }

    /**
     * Test that a record belonging to an RRset that also contains a DNSSEC signature
     * is removed via {@code RRset.deleteRR} and the RRset itself is **not** removed.
     *
     * <p>Because the real {@link RRset} class does not expose a mutable {@code sigSize()}
     * directly, we create a mocked {@link RRset} and inject it into the zone's internal map
     * using reflection.  The mock reports {@code size() == 1} and {@code sigSize() == 1},
     * which forces the {@code removeRecord} logic to take the {@code deleteRR} path.</p>
     */
    @Test
    void removeRecord_SingleRecordWithSignature_deletesOnlyRecord() throws Exception {
        // -----------------------------------------------------------------
        // 1. Build a minimal zone that contains a single A record (no NS/ SOA required for this test)
        // -----------------------------------------------------------------
        Name aName = Name.fromConstantString("sig.example." + ZONE);
        ARecord aRecord = new ARecord(aName, DClass.IN, 300, InetAddress.getByName("127.0.0.55"));
        Zone testZone = new Zone(zoneName, new Record[] { soa, ns, aRecord });
        // -----------------------------------------------------------------
        // 2. Create a mocked RRset that pretends to have a signature (sigSize > 0)
        // -----------------------------------------------------------------
        RRset mockedRRset = org.mockito.Mockito.mock(RRset.class);
        org.mockito.Mockito.when(mockedRRset.size()).thenReturn(1);
        org.mockito.Mockito.when(mockedRRset.sigSize()).thenReturn(1);
        // deleteRR should be a no‑op for the mock; we only want to verify that it was called.
        org.mockito.Mockito.doNothing().when(mockedRRset).deleteRR(org.mockito.Mockito.any());
        // -----------------------------------------------------------------
        // 3. Inject the mocked RRset into the zone's private map.
        //    The implementation of Zone stores RRsets in a field named "rrsets"
        //    (a ConcurrentSkipListMap<Name, Map<Integer, RRset>>).  We locate that field
        //    via reflection and put our mock in place.
        // -----------------------------------------------------------------
        Field rrsetsField = Zone.class.getDeclaredField("rrsets");
        rrsetsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentSkipListMap<Name, Map<Integer, RRset>> rrsetsMap = (ConcurrentSkipListMap<Name, Map<Integer, RRset>>) rrsetsField.get(testZone);
        Map<Integer, RRset> typeMap = new HashMap<>();
        typeMap.put(Type.A, mockedRRset);
        rrsetsMap.put(aName, typeMap);
        // -----------------------------------------------------------------
        // 4. Invoke removeRecord – it should call deleteRR on the mocked RRset.
        // -----------------------------------------------------------------
        testZone.removeRecord(aRecord);
        // -----------------------------------------------------------------
        // 5. Verify interactions.
        // -----------------------------------------------------------------
        org.mockito.Mockito.verify(mockedRRset, org.mockito.Mockito.times(1)).deleteRR(org.mockito.Mockito.eq(aRecord));
        // The RRset must still be present (size unchanged) – we check the map directly.
        assertTrue(rrsetsMap.get(aName).containsKey(Type.A), "RRset with signature must not have been removed");
    }
}
