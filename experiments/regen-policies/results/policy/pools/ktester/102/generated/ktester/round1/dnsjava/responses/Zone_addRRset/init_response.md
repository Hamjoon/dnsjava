```java
// SPDX-License-Identifier: BSD-3-Clause
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Zone_addRRset_Test {

    private static final String ZONE_NAME_STR = "example.";
    private Name zoneName;
    private SOARecord soaRecord;
    private NSRecord nsRecord;
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
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
        // Nothing to clean up per test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

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
}
```