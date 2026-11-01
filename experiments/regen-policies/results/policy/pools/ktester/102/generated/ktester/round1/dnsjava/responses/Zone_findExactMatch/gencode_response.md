```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Zone#findExactMatch(Name, int)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>validation of arguments (null name, invalid types)</li>
 *   <li>exact match look‑ups for existing records (A, AAAA, SOA)</li>
 *   <li>case‑insensitive name handling</li>
 *   <li>behaviour when no matching RRset exists (null result)</li>
 *   <li>defensive copying – the returned RRset must be a distinct object that does not reflect
 *       later mutations of the internal RRset</li>
 *   <li>boundary checks for type values (0, 255 (ANY), > 65535)</li>
 * </ul>
 *
 * <p>All tests use a real {@link Zone} instance built in {@code @BeforeEach}. No private fields or
 * methods are accessed directly; where necessary reflection is used only to invoke the private
 * {@code findRRsetWithoutLock} method to obtain the internal RRset reference for a strict
 * {@code assertNotSame} check.
 */
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

        nsRecord = new NSRecord(zoneName, DClass.IN, 300L,
                Name.fromConstantString("ns.example."));

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

        // Build the zone using the var‑args constructor (origin, SOA, NS, records...)
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

    /* --------------------------------------------------------------------- *
     *  Argument validation tests
     * --------------------------------------------------------------------- */

    @Test
    void findExactMatch_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> zone.findExactMatch(null, Type.A),
                "null name must trigger IllegalArgumentException");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 65536, Integer.MAX_VALUE})
    void findExactMatch_invalidType_throwsIllegalArgumentException(int invalidType) {
        assertThrows(IllegalArgumentException.class,
                () -> zone.findExactMatch(zoneName, invalidType),
                "type " + invalidType + " must be rejected by Type.check");
    }

    /* --------------------------------------------------------------------- *
     *  Successful look‑ups – exact matches
     * --------------------------------------------------------------------- */

    @Test
    void findExactMatch_existingARecord_returnsCopy() throws Exception {
        RRset result = zone.findExactMatch(aRecord.getName(), Type.A);
        assertNotNull(result, "RRset should be returned for an existing A record");
        assertTrue(result.contains(aRecord), "Returned RRset must contain the original A record");

        // Obtain the *internal* RRset via reflection to verify that a defensive copy was made
        RRset internal = getInternalRRsetViaReflection(aRecord.getName(), Type.A);
        assertNotSame(internal, result, "Returned RRset must be a copy, not the internal instance");
    }

    @Test
    void findExactMatch_existingAAAARecord_returnsCopy() throws Exception {
        RRset result = zone.findExactMatch(aaaaRecord.getName(), Type.AAAA);
        assertNotNull(result, "RRset should be returned for an existing AAAA record");
        assertTrue(result.contains(aaaaRecord), "Returned RRset must contain the original AAAA record");

        RRset internal = getInternalRRsetViaReflection(aaaaRecord.getName(), Type.AAAA);
        assertNotSame(internal, result, "Returned RRset must be a copy, not the internal instance");
    }

    @Test
    void findExactMatch_existingSOARecord_returnsCopy() throws Exception {
        RRset result = zone.findExactMatch(zoneName, Type.SOA);
        assertNotNull(result, "RRset should be returned for the zone's SOA record");
        assertTrue(result.contains(soaRecord), "Returned RRset must contain the SOA record");

        RRset internal = getInternalRRsetViaReflection(zoneName, Type.SOA);
        assertNotSame(internal, result, "Returned RRset must be a defensive copy");
    }

    @Test
    void findExactMatch_caseInsensitiveNameLookup_returnsCopy() throws Exception {
        // Store a record under a lower‑case name
        Name lowerCaseName = Name.fromString("host.example.com.");
        ARecord lowerCaseA = new ARecord(lowerCaseName, DClass.IN, 3600,
                InetAddress.getByName("192.0.2.55"));
        zone.addRecord(lowerCaseA);

        // Query with mixed‑case name – DNS names are case‑insensitive
        Name mixedCaseName = Name.fromString("HOST.Example.COM.");
        RRset result = zone.findExactMatch(mixedCaseName, Type.A);
        assertNotNull(result, "Lookup should succeed irrespective of name case");
        assertTrue(result.contains(lowerCaseA), "Result must contain the originally added record");

        RRset internal = getInternalRRsetViaReflection(lowerCaseName, Type.A);
        assertNotSame(internal, result, "Result must be a defensive copy");
    }

    /* --------------------------------------------------------------------- *
     *  Negative look‑ups – null results
     * --------------------------------------------------------------------- */

    @Test
    void findExactMatch_nonExistingRecord_returnsNull() {
        Name missing = Name.fromString("missing.example.");
        RRset result = zone.findExactMatch(missing, Type.A);
        assertNull(result, "Should return null when the name/type combination does not exist");
    }

    @Test
    void findExactMatch_missingAAAARecord_returnsNull() {
        // The zone only has an AAAA record for "www.example.", not for "mail.example."
        Name name = Name.fromString("mail.example.");
        RRset result = zone.findExactMatch(name, Type.AAAA);
        assertNull(result, "Should return null when a valid type exists but no RRset for that name");
    }

    @Test
    void findExactMatch_ANYType_returnsNull() {
        // ANY (255) is a legal type but the zone does not store an RRset under that type.
        RRset result = zone.findExactMatch(aRecord.getName(), Type.ANY);
        assertNull(result, "ANY type should return null when no exact RRset of type ANY exists");
    }

    /* --------------------------------------------------------------------- *
     *  Defensive copy semantics
     * --------------------------------------------------------------------- */

    @Test
    void findExactMatch_copyIsImmutableAfterOriginalIsMutated() throws Exception {
        // Obtain a copy of the existing A RRset
        RRset copyBefore = zone.findExactMatch(aRecord.getName(), Type.A);
        assertNotNull(copyBefore);
        assertTrue(copyBefore.contains(aRecord));

        // Mutate the underlying RRset by adding a second A record
        ARecord extra = new ARecord(aRecord.getName(), DClass.IN, 3600,
                InetAddress.getByName("192.0.2.99"));
        zone.addRecord(extra);

        // The previously obtained copy must NOT reflect the new record
        assertFalse(copyBefore.contains(extra),
                "Defensive copy must remain unchanged after the internal RRset is mutated");
        assertEquals(1, copyBefore.size(), "Copy should still contain only the original record");
    }

    /* --------------------------------------------------------------------- *
     *  Helper utilities
     * --------------------------------------------------------------------- */

    /**
     * Uses reflection to invoke the private {@code findRRsetWithoutLock} method.
     * This gives us direct access to the internal {@link RRset} instance stored in the Zone,
     * allowing a strict {@code assertNotSame} comparison with the public copy returned by
     * {@code findExactMatch}.
     */
    private RRset getInternalRRsetViaReflection(Name name, int type) throws Exception {
        java.lang.reflect.Method m = Zone.class.getDeclaredMethod("findRRsetWithoutLock", Name.class, int.class);
        m.setAccessible(true);
        return (RRset) m.invoke(zone, name, type);
    }
}
```