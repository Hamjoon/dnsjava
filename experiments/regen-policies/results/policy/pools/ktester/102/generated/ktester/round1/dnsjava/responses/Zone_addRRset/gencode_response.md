```java
// SPDX-License-Identifier: BSD-3-Clause
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Zone#addRRset(RRset)}.
 *
 * <p>This class covers the validation logic, successful additions,
 * and edge‑cases such as type mismatches or null values.
 *
 * <p>Mockito is used to create lightweight {@link RRset} mocks for
 * scenarios where constructing a real {@code RRset} would be cumbersome
 * (e.g., a zero‑size RRset or a SOA RRset whose first record is not an
 * {@link SOARecord}).
 */
@ExtendWith(MockitoExtension.class)
class Zone_addRRset_Test {

    private static final String ZONE_NAME_STR = "example.";
    private Name zoneName;
    private SOARecord soaRecord;
    private NSRecord nsRecord;
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Origin of the zone
        zoneName = Name.fromConstantString(ZONE_NAME_STR);

        // Minimal required records: SOA + at least one NS
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

        nsRecord = new NSRecord(
                zoneName,
                DClass.IN,
                300L,
                Name.fromConstantString("ns.example."));

        // Initialise the zone with the mandatory records
        zone = new Zone(zoneName, soaRecord, nsRecord);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------------- *
     *  Existing tests from the original test suite (kept unchanged)      *
     * --------------------------------------------------------------------- */

    @Test
    void addRRset_NullArgument_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(null));
        assertTrue(ex.getMessage().contains("rrset must not be null"));
    }

    @Test
    void addRRset_SoaOwnerDoesNotMatchOrigin_Throws() throws IOException {
        // Create a SOA whose name is different from the zone origin
        Name otherName = Name.fromConstantString("other.example.");
        SOARecord otherSoa = new SOARecord(
                otherName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);
        RRset rrset = new RRset(otherSoa);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(rrset));
        assertTrue(ex.getMessage().contains("does not match zone origin"));
    }

    @Test
    void addRRset_SoaWithIncorrectSize_Throws() throws IOException {
        // Two SOA records with the same correct owner – size will be 2
        SOARecord soa1 = soaRecord;
        SOARecord soa2 = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                2,
                21600L,
                7200L,
                2160000L,
                3600L);
        RRset rrset = new RRset(soa1, soa2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(rrset));
        assertTrue(ex.getMessage().contains("exactly 1 SOA"));
    }

    @Test
    void addRRset_NameNotSubdomainOfOrigin_Throws() throws IOException {
        // Absolute name that is not a subdomain of the zone origin
        Name foreignName = Name.fromConstantString("outside.");
        ARecord aRecord = new ARecord(foreignName, DClass.IN, 300L,
                InetAddress.getByName("192.0.2.1"));
        RRset rrset = new RRset(aRecord);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(rrset));
        assertTrue(ex.getMessage().contains("is absolute and not a subdomain"));
    }

    @Test
    void addRRset_ValidARecord_AddsSuccessfully() throws IOException {
        Name hostName = new Name("host", zoneName);
        ARecord aRecord = new ARecord(hostName, DClass.IN, 300L,
                InetAddress.getByName("192.0.2.42"));
        RRset rrset = new RRset(aRecord);

        assertDoesNotThrow(() -> zone.addRRset(rrset));

        RRset found = zone.findExactMatch(hostName, Type.A);
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(aRecord, found.first());
    }

    @Test
    void addRRset_ValidSoa_ReplacesExistingSoa() throws IOException {
        // New SOA with a different serial number
        SOARecord newSoa = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                99,                     // new serial
                21600L,
                7200L,
                2160000L,
                3600L);
        RRset rrset = new RRset(newSoa);

        assertDoesNotThrow(() -> zone.addRRset(rrset));

        // The zone's SOA should now be the newly added one
        assertEquals(newSoa, zone.getSOA());
        // And findExactMatch should return it
        RRset found = zone.findExactMatch(zoneName, Type.SOA);
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(newSoa, found.first());
    }

    /* --------------------------------------------------------------------- *
     *  Additional tests derived from the JSON specification                *
     * --------------------------------------------------------------------- */

    /**
     * The method casts the first record of a SOA RRset to {@link SOARecord}.
     * If the record is of another type a {@link ClassCastException} is expected.
     */
    @Test
    void addRRset_SoaFirstRecordNotSOA_ThrowsClassCastException() {
        // Mock an RRset that claims to be a SOA set but contains an ARecord.
        RRset mockRrset = mock(RRset.class);
        when(mockRrset.getName()).thenReturn(zoneName);
        when(mockRrset.getType()).thenReturn(Type.SOA);
        when(mockRrset.size()).thenReturn(1);
        // Return a generic Record (ARecord) as the first element.
        ARecord aRecord = new ARecord(zoneName, DClass.IN, 300L, InetAddress.getLoopbackAddress());
        when(mockRrset.first()).thenReturn(aRecord);

        // The validation passes (owner matches, size==1), but the cast fails.
        assertThrows(ClassCastException.class, () -> zone.addRRset(mockRrset));
    }

    /**
     * Non‑SOA RRsets are not size‑validated. An empty RRset should be accepted.
     */
    @Test
    void addRRset_NonSOAZeroRecords_AddsSuccessfully() {
        RRset mockRrset = mock(RRset.class);
        Name subName = new Name("www", zoneName);
        when(mockRrset.getName()).thenReturn(subName);
        when(mockRrset.getType()).thenReturn(Type.A);
        when(mockRrset.size()).thenReturn(0);
        // first() should never be called for size 0, but stub it to avoid NPE if it is.
        when(mockRrset.first()).thenReturn(null);

        assertDoesNotThrow(() -> zone.addRRset(mockRrset));

        // Verify that the RRset is now stored (even though empty) and that SOA unchanged.
        RRset stored = zone.findExactMatch(subName, Type.A);
        assertNotNull(stored);
        assertEquals(0, stored.size());
        assertEquals(soaRecord, zone.getSOA());
    }

    /**
     * Adding a non‑SOA RRset whose name equals the zone origin should be allowed.
     */
    @Test
    void addRRset_NonSOANameEqualsOrigin_AddsSuccessfully() {
        // Use an NSRecord at the zone origin (already present, but we add a new one)
        NSRecord extraNs = new NSRecord(zoneName, DClass.IN, 300L,
                Name.fromConstantString("ns2.example."));
        RRset rrset = new RRset(extraNs);

        assertDoesNotThrow(() -> zone.addRRset(rrset));

        RRset found = zone.findExactMatch(zoneName, Type.NS);
        assertNotNull(found);
        // The zone now contains two NS records (original + extra)
        assertEquals(2, found.size());
        assertTrue(found.contains(extraNs));
    }

    /**
     * Adding a second valid SOA RRset should replace the previous one.
     */
    @Test
    void addRRset_OverwriteExistingSOA_UpdatesSOA() throws IOException {
        // First SOA (already present) has serial 1.
        assertEquals(1, zone.getSOA().getSerial());

        // New SOA with serial 2
        SOARecord newSoa = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                2,
                21600L,
                7200L,
                2160000L,
                3600L);
        RRset rrset = new RRset(newSoa);

        assertDoesNotThrow(() -> zone.addRRset(rrset));
        assertEquals(2, zone.getSOA().getSerial());
    }

    /**
     * Domain name comparison must be case‑insensitive.
     */
    @Test
    void addRRset_CaseInsensitiveSubdomain_AddsSuccessfully() throws IOException {
        // Mixed‑case name that is a subdomain of the origin.
        Name mixedCase = Name.fromConstantString("WWW.Example.Com.");
        ARecord aRecord = new ARecord(mixedCase, DClass.IN, 300L,
                InetAddress.getByName("192.0.2.55"));
        RRset rrset = new RRset(aRecord);

        assertDoesNotThrow(() -> zone.addRRset(rrset));

        RRset found = zone.findExactMatch(mixedCase, Type.A);
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(aRecord, found.first());
    }

    /**
     * If the RRset returns {@code null} for its name, a {@link NullPointerException}
     * should be thrown before any lock acquisition.
     */
    @Test
    void addRRset_NullRRsetName_ThrowsNullPointerException() {
        RRset mockRrset = mock(RRset.class);
        when(mockRrset.getName()).thenReturn(null);
        when(mockRrset.getType()).thenReturn(Type.A);
        when(mockRrset.size()).thenReturn(1);
        when(mockRrset.first()).thenReturn(mock(Record.class));

        assertThrows(NullPointerException.class, () -> zone.addRRset(mockRrset));
    }

    /**
     * If the {@link Zone}'s origin field is {@code null}, any call to {@code addRRset}
     * should result in a {@link NullPointerException}.
     */
    @Test
    void addRRset_ZoneWithNullOrigin_ThrowsNullPointerException() throws Exception {
        // Use reflection to set the private 'origin' field to null.
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        originField.set(zone, null);

        Name name = new Name("host", zoneName);
        ARecord aRecord = new ARecord(name, DClass.IN, 300L,
                InetAddress.getByName("192.0.2.99"));
        RRset rrset = new RRset(aRecord);

        assertThrows(NullPointerException.class, () -> zone.addRRset(rrset));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for groups of similar invalid inputs            *
     * --------------------------------------------------------------------- */

    /**
     * Grouped invalid‑input tests (null RRset, null name, etc.).
     */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("invalidInputProvider")
    void addRRset_InvalidInputs_ThrowsException(String description, RRset rrset, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> zone.addRRset(rrset), description);
    }

    static Stream<Arguments> invalidInputProvider() throws IOException {
        // 1. Null RRset – already covered, but kept for completeness.
        RRset nullRrset = null;

        // 2. RRset with null name.
        RRset rrsetNullName = mock(RRset.class);
        when(rrsetNullName.getName()).thenReturn(null);
        when(rrsetNullName.getType()).thenReturn(Type.A);
        when(rrsetNullName.size()).thenReturn(1);
        when(rrsetNullName.first()).thenReturn(mock(Record.class));

        // 3. SOA RRset with wrong owner (different origin).
        Name other = Name.fromConstantString("other.example.");
        SOARecord otherSoa = new SOARecord(other, DClass.IN, 3600L,
                Name.fromConstantString("ns.example."), new Name("hostadmin", other),
                1, 21600L, 7200L, 2160000L, 3600L);
        RRset rrsetWrongOwner = new RRset(otherSoa);

        // 4. SOA RRset with size != 1.
        RRset rrsetWrongSize = new RRset(soaRecord, new SOARecord(
                zoneName, DClass.IN, 3600L,
                Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName),
                2, 21600L, 7200L, 2160000L, 3600L));

        // 5. Name not subdomain.
        Name foreign = Name.fromConstantString("outside.");
        ARecord aForeign = new ARecord(foreign, DClass.IN, 300L,
                InetAddress.getByName("192.0.2.1"));
        RRset rrsetOutside = new RRset(aForeign);

        return Stream.of(
                Arguments.of("null RRset argument", nullRrset, IllegalArgumentException.class),
                Arguments.of("RRset with null name", rrsetNullName, NullPointerException.class),
                Arguments.of("SOA RRset with owner not matching origin", rrsetWrongOwner,
                        IllegalArgumentException.class),
                Arguments.of("SOA RRset with size != 1", rrsetWrongSize,
                        IllegalArgumentException.class),
                Arguments.of("RRset name not subdomain of origin", rrsetOutside,
                        IllegalArgumentException.class)
        );
    }
}
```