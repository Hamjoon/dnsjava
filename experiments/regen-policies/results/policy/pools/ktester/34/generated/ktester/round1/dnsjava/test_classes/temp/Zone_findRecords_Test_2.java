package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class Zone_findRecords_Test {

    /**
     * Common objects used by all tests.
     */
    private Name zoneName;

    private SOARecord soa;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aUnique;

    private ARecord aTest;

    private AAAARecord aaaaTest;

    private ARecord aWild;

    private TXTRecord txtWild;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a minimal but realistic zone (the same data used in the original
        // ZoneTest class). All records are absolute and belong to the zone
        // "example.".
        zoneName = Name.fromConstantString("example.");
        soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
        aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6a);
        aWild = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        txtWild = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");
        Record[] records = { soa, ns1, ns2, aUnique, aTest, aaaaTest, aWild, txtWild };
        zone = new Zone(zoneName, records);
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> zone.findRecords(null, Type.A), "name must not be null");
    }

    @Test
    void findRecords_invalidType_throwsInvalidTypeException() {
        // an arbitrary type value that is not defined by the DNS protocol
        int invalidType = 9999;
        assertThrows(InvalidTypeException.class, () -> zone.findRecords(Name.fromConstantString("example.com."), invalidType));
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_nameNotInZone_returnsNxDomain() {
        Name external = Name.fromConstantString("outside.example.");
        SetResponse response = zone.findRecords(external, Type.A);
        assertNotNull(response);
        assertTrue(response.isNXDOMAIN(), "The response should indicate NXDOMAIN for a name outside the zone");
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_exactMatch_returnsSuccess() {
        SetResponse response = zone.findRecords(aTest.getName(), Type.A);
        assertNotNull(response);
        // In the current dnsjava API a successful lookup is indicated by
        // !isNXDOMAIN() and !isCNAME() etc.  The most direct way is to check that
        // the response is not NXDOMAIN.
        assertFalse(response.isNXDOMAIN(), "The response for an exact match should not be NXDOMAIN");
        // Retrieve the RRset directly from the zone and verify it contains the
        // expected record.
        RRset rrset = zone.findExactMatch(aTest.getName(), Type.A);
        assertNotNull(rrset);
        // RRset provides a list of records via the rrs() method.
        List<Record> rrRecords = rrset.rrs();
        assertTrue(rrRecords.contains(aTest), "The RRset returned by the zone should contain the queried A record");
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_wildcardMatch_returnsSuccess() throws IOException {
        Name query = Name.fromConstantString("foo.wild.example.");
        SetResponse response = zone.findRecords(query, Type.A);
        assertNotNull(response);
        assertFalse(response.isNXDOMAIN(), "Wildcard lookup should not result in NXDOMAIN");
        // The wildcard A record should be returned.
        RRset rrset = zone.findExactMatch(Name.fromConstantString("*.wild.example."), Type.A);
        assertNotNull(rrset);
        List<Record> rrRecords = rrset.rrs();
        assertTrue(rrRecords.contains(aWild), "The RRset must contain the wildcard A record");
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_zoneWithNullOrigin_throwsNullPointerException() throws Exception {
        // Use reflection to set the private 'origin' field to null after construction.
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        originField.set(zone, null);
        // The call to name.subdomain(origin) inside findRecords will now NPE.
        Name query = Name.fromConstantString("sub.example.com.");
        assertThrows(NullPointerException.class, () -> zone.findRecords(query, Type.A));
    }
}
