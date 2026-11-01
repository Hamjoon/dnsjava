```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Zone#addRRset(RRset)}.
 *
 * The tests create a fresh {@link Zone} instance for each test, add a new {@link RRset}
 * via the public {@code addRRset} method and then verify that the records can be
 * retrieved through the public lookup methods.
 */
class Zone_addRRset_Test {

    private static Name zoneName;
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() {
        try {
            // the zone name used for all tests
            zoneName = new Name("example.");
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        try {
            // Minimal zone: only SOA and NS records – enough for a valid Zone
            Record[] minimalRecords = new Record[] {
                new SOARecord(
                    zoneName,
                    DClass.IN,
                    3600L,
                    Name.fromConstantString("ns.example."),
                    new Name("hostmaster", zoneName),
                    1,
                    7200L,
                    3600L,
                    1209600L,
                    3600L),
                new NSRecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."))
            };
            zone = new Zone(zoneName, minimalRecords);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up for the current tests.
        zone = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up at the class level.
    }

    /** Helper to build a List<RRset> from var‑args. */
    private static List<RRset> listOf(RRset... rrsets) {
        return Stream.of(rrsets).collect(Collectors.toList());
    }

    /** Helper to build a singleton List<RRset> containing the given records. */
    private static List<RRset> oneRRset(Record... records) {
        return Collections.singletonList(new RRset(records));
    }

    @Test
    void addSingleARecord() throws Exception {
        // Arrange – create an A record and wrap it in an RRset
        Name aName = new Name("host", zoneName);
        InetAddress ip = InetAddress.getByName("192.0.2.1");
        ARecord aRecord = new ARecord(aName, DClass.IN, 300, ip);
        RRset rrset = new RRset(aRecord);

        // Act – add the RRset to the zone
        zone.addRRset(rrset);

        // Assert – the record is now findable
        SetResponse resp = zone.findRecords(aName, Type.A);
        assertTrue(resp.isSuccessful(), "Response should be successful");
        assertEquals(oneRRset(aRecord), resp.answers(),
                "The added A record should be returned by findRecords");
    }

    @Test
    void addMultipleAaaaRecordsSameName() throws Exception {
        // Arrange – two AAAA records for the same name
        Name name = new Name("host", zoneName);
        InetAddress ip6a = InetAddress.getByName("2001:db8::1");
        InetAddress ip6b = InetAddress.getByName("2001:db8::2");
        AAAARecord aaaa1 = new AAAARecord(name, DClass.IN, 300, ip6a);
        AAAARecord aaaa2 = new AAAARecord(name, DClass.IN, 300, ip6b);
        RRset rrset = new RRset(aaaa1, aaaa2);

        // Act
        zone.addRRset(rrset);

        // Assert
        SetResponse resp = zone.findRecords(name, Type.AAAA);
        assertTrue(resp.isSuccessful(), "Response should be successful");
        assertEquals(oneRRset(aaaa1, aaaa2), resp.answers(),
                "Both AAAA records should be returned");
    }

    @Test
    void addWildcardARecordAndLookupUndefinedName() throws Exception {
        // Arrange – a wildcard A record
        Name wildcardName = new Name("*", zoneName);
        InetAddress ip = InetAddress.getByName("203.0.113.5");
        ARecord wildA = new ARecord(wildcardName, DClass.IN, 300, ip);
        RRset rrset = new RRset(wildA);

        // Act
        zone.addRRset(rrset);

        // Lookup a name that does not exist explicitly
        Name queryName = new Name("nonexistent", zoneName);
        SetResponse resp = zone.findRecords(queryName, Type.A);

        // Assert – the wildcard should be returned with the queried name substituted
        assertTrue(resp.isSuccessful(), "Wildcard lookup should be successful");
        ARecord expected = (ARecord) wildA.withName(queryName);
        assertEquals(oneRRset(expected), resp.answers(),
                "Wildcard A record should be returned with the queried name");
    }

    @Test
    void addWildcardTxtRecordAndLookupUndefinedName() throws Exception {
        // Arrange – a wildcard TXT record
        Name wildcardName = new Name("*", zoneName);
        TXTRecord wildTxt = new TXTRecord(wildcardName, DClass.IN, 300, "wildtext");
        RRset rrset = new RRset(wildTxt);

        // Act
        zone.addRRset(rrset);

        // Lookup a name that does not exist explicitly
        Name queryName = new Name("another", zoneName);
        SetResponse resp = zone.findRecords(queryName, Type.TXT);

        // Assert
        assertTrue(resp.isSuccessful(), "Wildcard TXT lookup should be successful");
        TXTRecord expected = (TXTRecord) wildTxt.withName(queryName);
        assertEquals(oneRRset(expected), resp.answers(),
                "Wildcard TXT record should be returned with the queried name");
    }
}
```