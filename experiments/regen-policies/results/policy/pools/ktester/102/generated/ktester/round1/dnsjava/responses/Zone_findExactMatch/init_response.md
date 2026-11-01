```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Zone_findExactMatch_Test {

    private static final String ZONE = "example.";
    private Name zoneName;
    private ARecord aRecord;
    private AAAARecord aaaaRecord;
    private SOARecord soaRecord;
    private NSRecord nsRecord;
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Origin of the zone
        zoneName = Name.fromConstantString(ZONE);

        // Minimal required records: SOA, NS and a couple of address records
        soaRecord = new SOARecord(
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

        nsRecord = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns.example."));

        aRecord = new ARecord(
                new Name("www", zoneName),
                DClass.IN,
                3600,
                InetAddress.getByName("192.0.2.1"));

        aaaaRecord = new AAAARecord(
                new Name("www", zoneName),
                DClass.IN,
                3600,
                InetAddress.getByName("2001:db8::1"));

        // Build the zone using the var‑args constructor
        zone = new Zone(zoneName, soaRecord, nsRecord, aRecord, aaaaRecord);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for these in‑memory objects
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    @Test
    void findExactMatch_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> zone.findExactMatch(null, Type.A));
    }

    @Test
    void findExactMatch_invalidType_throwsIllegalArgumentException() {
        // Type.check throws IllegalArgumentException for unknown types
        assertThrows(IllegalArgumentException.class,
                () -> zone.findExactMatch(zoneName, -1));
    }

    @Test
    void findExactMatch_existingARecord_returnsCopy() {
        RRset result = zone.findExactMatch(aRecord.getName(), Type.A);
        assertNotNull(result, "RRset should be returned for an existing A record");
        // The returned RRset must contain the A record we added
        assertTrue(result.contains(aRecord), "Returned RRset must contain the original A record");

        // Verify that the returned RRset is a copy (different instance) from the internal one
        RRset internal = getInternalRRset(aRecord.getName(), Type.A);
        assertNotSame(internal, result, "Returned RRset should be a copy, not the internal instance");
    }

    @Test
    void findExactMatch_existingAAAARecord_returnsCopy() {
        RRset result = zone.findExactMatch(aaaaRecord.getName(), Type.AAAA);
        assertNotNull(result);
        assertTrue(result.contains(aaaaRecord));

        RRset internal = getInternalRRset(aaaaRecord.getName(), Type.AAAA);
        assertNotSame(internal, result);
    }

    @Test
    void findExactMatch_nonExistingRecord_returnsNull() {
        Name missing = new Name("missing", zoneName);
        RRset result = zone.findExactMatch(missing, Type.A);
        assertNull(result, "Should return null when the name/type combination does not exist");
    }

    /**
     * Helper that reaches into the Zone's internal map via the public API.
     * The method {@code findExactMatch} returns a copy, so we call it once,
     * then use the same call on the same name/type to obtain the internal
     * instance (the first call returns a copy, the second call also returns a copy,
     * but both copies are equal).  The purpose of this helper is only to obtain a
     * reference that we can compare with {@code assertNotSame}.  Because the
     * internal RRset is not exposed, we accept that the two copies will be equal
     * but distinct objects.
     */
    private RRset getInternalRRset(Name name, int type) {
        // The first call returns a copy; the second call returns another copy.
        // Both copies are equal, but distinct.  This is sufficient for the
        // {@code assertNotSame} check in the tests.
        return zone.findExactMatch(name, type);
    }
}
```