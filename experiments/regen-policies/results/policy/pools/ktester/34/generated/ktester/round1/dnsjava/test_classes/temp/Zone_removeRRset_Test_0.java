package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class Zone_removeRRset_Test {

    /* --------------------------------------------------------------------- *
   *  Shared test fixtures
   * --------------------------------------------------------------------- */
    private static Name ZONE_NAME;

    private static SOARecord SOA1;

    private static NSRecord NS1;

    private static NSRecord NS2;

    private static ARecord A_UNIQUE;

    private static ARecord A_TEST;

    private static AAAARecord AAAA_1_TEST;

    private static AAAARecord AAAA_2_TEST;

    private static ARecord A_WILD;

    private static TXTRecord TXT_WILD;

    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // zone name and the mandatory SOA/NS records
        ZONE_NAME = Name.fromConstantString("example.");
        SOA1 = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostadmin", ZONE_NAME), 1, 21600L, 7200L, 2160000L, 3600L);
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // some additional records used by the tests
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");
        A_UNIQUE = new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
        A_WILD = new ARecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a fresh zone for each test – this guarantees test isolation.
        Record[] records = { SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD };
        zone = new Zone(ZONE_NAME, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up.
        zone = null;
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of(-1), // larger than 16‑bit DNS type range
        Arguments.of(65536));
    }

    @Test
    void removeRRset_nameIsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> zone.removeRRset(null, Type.A), "Expected IllegalArgumentException when name is null");
    }

    @Test
    void removeRRset_invalidType_throwsInvalidTypeException() {
        // The Zone method forwards the type check to Type.check(...).
        assertThrows(InvalidTypeException.class, () -> zone.removeRRset(ZONE_NAME, -1), "Expected InvalidTypeException for an illegal type value");
    }

    @Test
    void removeRRset_existingARecord_successfulRemoval() {
        // Pre‑condition: the A record is present.
        assertNotNull(zone.findExactMatch(A_TEST.getName(), Type.A));
        // Act
        zone.removeRRset(A_TEST.getName(), Type.A);
        // Post‑condition: the RRset has disappeared.
        assertNull(zone.findExactMatch(A_TEST.getName(), Type.A));
    }

    @Test
    void removeRRset_removeAllNS_throwsIllegalStateException() {
        // The zone always needs at least one NS RRset; attempting to remove it should fail.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> zone.removeRRset(ZONE_NAME, Type.NS), "Removing the sole NS RRset must be prohibited");
        // The exact message used by dnsjava is "remove all NS". We verify that the message contains it.
        assertTrue(ex.getMessage().toLowerCase().contains("remove all ns"), "Exception message should mention removal of all NS records");
    }

    @Test
    void removeRRset_removeSOA_throwsIllegalStateException() {
        // The zone must always contain an SOA record.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> zone.removeRRset(ZONE_NAME, Type.SOA), "Removing the SOA RRset must be prohibited");
        assertTrue(ex.getMessage().toLowerCase().contains("remove soa"), "Exception message should mention removal of the SOA record");
    }

    @Test
    void removeRRset_nonExistingRRset_noExceptionAndStateUnchanged() {
        Name nonExisting = Name.fromString("nonexistent.example.");
        // Ensure the RRset truly does not exist before the call.
        assertNull(zone.findExactMatch(nonExisting, Type.A));
        // Act – should not throw.
        assertDoesNotThrow(() -> zone.removeRRset(nonExisting, Type.A));
        // Verify state unchanged.
        assertNull(zone.findExactMatch(nonExisting, Type.A));
    }

    @Test
    void removeRRset_multipleTypesSameName_onlyAIsRemoved() throws IOException {
        // Add a TXT record for the same owner name as an existing A record.
        Name multiName = Name.fromString("multi.example.");
        ARecord aRec = new ARecord(multiName, DClass.IN, 300, InetAddress.getByName("10.0.0.1"));
        TXTRecord txtRec = new TXTRecord(multiName, DClass.IN, 300, "txtdata");
        zone.addRecord(aRec);
        zone.addRecord(txtRec);
        // Sanity checks.
        assertNotNull(zone.findExactMatch(multiName, Type.A));
        assertNotNull(zone.findExactMatch(multiName, Type.TXT));
        // Remove only the A RRset.
        zone.removeRRset(multiName, Type.A);
        // Verify A is gone, TXT remains.
        assertNull(zone.findExactMatch(multiName, Type.A));
        assertNotNull(zone.findExactMatch(multiName, Type.TXT));
    }

    @Test
    void removeRRset_caseInsensitiveName_removesRRset() {
        Name upperCase = Name.fromString("HOST.EXAMPLE.");
        // Pre‑condition: lower‑case version exists.
        assertNotNull(zone.findExactMatch(A_TEST.getName(), Type.A));
        // Act – use upper‑case name.
        zone.removeRRset(upperCase, Type.A);
        // Post‑condition: the RRset is gone irrespective of case.
        assertNull(zone.findExactMatch(A_TEST.getName(), Type.A));
    }

    @Test
    void removeRRset_iteratorReflectsRemoval() {
        // Add a distinct RRset that we will later remove.
        Name target = Name.fromString("iter.example.");
        ARecord aRec = new ARecord(target, DClass.IN, 300, InetAddress.getLoopbackAddress());
        zone.addRecord(aRec);
        assertNotNull(zone.findExactMatch(target, Type.A));
        // Remove it.
        zone.removeRRset(target, Type.A);
        // Build a list of remaining RRset names from the iterator.
        List<Name> remainingNames = new ArrayList<>();
        Iterator<RRset> it = zone.iterator();
        while (it.hasNext()) {
            remainingNames.add(it.next().getName());
        }
        // The removed name must not be present.
        assertFalse(remainingNames.contains(target));
    }

    @Test
    void removeRRset_onEmptyZone_isNoOp() throws IOException {
        // Create an empty zone (only SOA and NS are mandatory – we create a zone with just those).
        Record[] minimal = { SOA1, NS1 };
        Zone emptyZone = new Zone(Name.fromString("empty.example."), minimal);
        Name anyName = Name.fromString("nothing.example.");
        // Ensure zone is empty for the queried name/type.
        assertNull(emptyZone.findExactMatch(anyName, Type.A));
        // Act – should not throw.
        assertDoesNotThrow(() -> emptyZone.removeRRset(anyName, Type.A));
        // Verify still empty.
        assertNull(emptyZone.findExactMatch(anyName, Type.A));
    }

    @Test
    void removeRRset_removeSOA_throwsInvalidTypeException() {
        assertThatThrownBy(() -> zone.removeRRset(ZONE_NAME, Type.SOA)).isInstanceOf(InvalidTypeException.class).hasMessageContaining("SOA");
    }

    @Test
    void removeRRset_removeNS_throwsInvalidTypeException() {
        assertThatThrownBy(() -> zone.removeRRset(ZONE_NAME, Type.NS)).isInstanceOf(InvalidTypeException.class).hasMessageContaining("NS");
    }
}
