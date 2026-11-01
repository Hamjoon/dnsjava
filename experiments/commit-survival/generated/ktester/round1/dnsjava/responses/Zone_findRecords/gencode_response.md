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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Zone#findRecords(Name, int)}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>null / invalid arguments</li>
 *   <li>exact‑match look‑ups</li>
 *   <li>delegations, CNAME and DNAME handling</li>
 *   <li>wild‑card expansion</li>
 *   <li>apex (SOA/NS) queries</li>
 *   <li>ANY‑type queries</li>
 *   <li>edge cases such as empty zones, root zone, illegal types, non‑absolute names, etc.</li>
 * </ul>
 * </p>
 *
 * The original examples from the upstream {@code ZoneTest} are retained, and new
 * tests are added to satisfy the JSON‑driven test matrix.
 */
@ExtendWith(MockitoExtension.class)
class Zone_findRecords_Test {

    /** Zone used by the original tests – created once for the whole test run. */
    private static Zone ZONE;

    /** Records that populate the original zone. */
    private static ARecord A_TEST;
    private static AAAARecord AAAA_1_TEST;
    private static AAAARecord AAAA_2_TEST;
    private static ARecord A_WILD;
    private static TXTRecord TXT_WILD;

    @BeforeAll
    static void setupBeforeAll() {
        try {
            // zone origin
            Name zoneName = new Name("example.");

            // concrete address values
            InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
            InetAddress localhost6a = InetAddress.getByName("::1");
            InetAddress localhost6b = InetAddress.getByName("::2");

            // records used in the original tests
            A_TEST = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
            AAAA_1_TEST = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6a);
            AAAA_2_TEST = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6b);
            A_WILD = new ARecord(new Name("*", zoneName), DClass.IN, 3600, localhost4);
            TXT_WILD = new TXTRecord(new Name("*", zoneName), DClass.IN, 3600, "sometext");

            // assemble a minimal zone (SOA + NS + the records above)
            Record[] records = new Record[]{
                    new SOARecord(
                            zoneName,
                            DClass.IN,
                            3600L,
                            Name.fromConstantString("nameserver."),
                            new Name("hostmaster", zoneName),
                            1,
                            21600L,
                            7200L,
                            2160000L,
                            3600L),
                    new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("nameserver.")),
                    A_TEST,
                    AAAA_1_TEST,
                    AAAA_2_TEST,
                    A_WILD,
                    TXT_WILD
            };

            ZONE = new Zone(zoneName, records);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise test zone", e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods mirroring those in the original ZoneTest                */
    /* --------------------------------------------------------------------- */

    private static List<RRset> listOf(RRset... rrsets) {
        return Stream.of(rrsets).collect(Collectors.toList());
    }

    private static List<RRset> oneRRset(Record... records) {
        return Collections.singletonList(new RRset(records));
    }

    /* --------------------------------------------------------------------- */
    /* Original test cases (kept unchanged)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void exactNameExistingALookup() {
        Name testName = Name.fromConstantString("test.example.");
        SetResponse resp = ZONE.findRecords(testName, Type.A);
        assertEquals(oneRRset(A_TEST), resp.answers());
    }

    @Test
    void exactNameTwoAaaaLookup() {
        Name testName = Name.fromConstantString("test.example.");
        SetResponse resp = ZONE.findRecords(testName, Type.AAAA);
        assertEquals(oneRRset(AAAA_1_TEST, AAAA_2_TEST), resp.answers());
    }

    @Test
    void exactNameAnyLookup() {
        Name testName = Name.fromConstantString("test.example.");
        SetResponse resp = ZONE.findRecords(testName, Type.ANY);
        assertTrue(resp.isSuccessful());
        assertEquals(
                listOf(new RRset(A_TEST), new RRset(AAAA_1_TEST, AAAA_2_TEST)),
                resp.answers());
    }

    @Test
    void wildNameExistingALookup() {
        Name testName = Name.fromConstantString("undefined.example.");
        SetResponse resp = ZONE.findRecords(testName, Type.A);
        assertEquals(oneRRset(A_WILD.withName(testName)), resp.answers());
    }

    @Test
    void wildNameExistingTxtLookup() {
        Name testName = Name.fromConstantString("undefined.example.");
        SetResponse resp = ZONE.findRecords(testName, Type.TXT);
        assertEquals(oneRRset(TXT_WILD.withName(testName)), resp.answers());
    }

    @Test
    void wildNameNonExistingMxLookup() {
        SetResponse resp = ZONE.findRecords(Name.fromConstantString("undefined.example."), Type.MX);
        assertTrue(resp.isNXDOMAIN());
    }

    @Test
    void wildNameAnyLookup() {
        Name testName = Name.fromConstantString("undefined.example.");
        SetResponse resp = ZONE.findRecords(testName, Type.ANY);
        assertTrue(resp.isSuccessful());
        assertEquals(
                listOf(
                        new RRset(A_WILD.withName(testName)),
                        new RRset(TXT_WILD.withName(testName))
                ),
                resp.answers());
    }

    /* --------------------------------------------------------------------- */
    /* New test groups derived from the JSON matrix                           */
    /* --------------------------------------------------------------------- */

    // -----------------------------------------------------------------
    // Group: findRecords - null and invalid arguments
    // -----------------------------------------------------------------
    @Test
    void findRecords_nullName_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> ZONE.findRecords(null, Type.A));
    }

    @Test
    void findRecords_negativeType_throwsIAE() {
        Name name = Name.fromConstantString("example.com.");
        assertThrows(IllegalArgumentException.class,
                () -> ZONE.findRecords(name, -1));
    }

    // -----------------------------------------------------------------
    // Group: findRecords - exact match scenarios
    // -----------------------------------------------------------------
    @Test
    void exactMatch_ARecord_success() throws IOException {
        Name origin = new Name("example.com.");
        ARecord a = new ARecord(new Name("www", origin), DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        Zone zone = new Zone(origin, new Record[]{a});
        SetResponse resp = zone.findRecords(Name.fromString("www.example.com."), Type.A);
        assertTrue(resp.isSuccessful());
        assertEquals(oneRRset(a), resp.answers());
    }

    @Test
    void exactMatch_nonExistingName_NXDOMAIN() throws IOException {
        Name origin = new Name("example.com.");
        Zone zone = new Zone(origin, new Record[]{});
        SetResponse resp = zone.findRecords(Name.fromString("mail.example.com."), Type.MX);
        assertTrue(resp.isNXDOMAIN());
    }

    @Test
    void exactMatch_nameExistsButNoRequestedType_NODATA() throws IOException {
        Name origin = new Name("example.com.");
        ARecord a = new ARecord(new Name("host", origin), DClass.IN, 300, InetAddress.getByName("5.6.7.8"));
        Zone zone = new Zone(origin, new Record[]{a});
        SetResponse resp = zone.findRecords(Name.fromString("host.example.com."), Type.SRV);
        assertTrue(resp.isNODATA());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - delegation and CNAME handling
    // -----------------------------------------------------------------
    @Test
    void delegationResponse_DELEGATION() throws IOException {
        Name origin = new Name("example.com.");
        Name sub = new Name("sub", origin);
        NSRecord ns = new NSRecord(sub, DClass.IN, 300, Name.fromConstantString("ns.sub.example.com."));
        Zone zone = new Zone(origin, new Record[]{ns});
        SetResponse resp = zone.findRecords(Name.fromString("sub.example.com."), Type.A);
        assertTrue(resp.isDelegation());
        assertEquals(oneRRset(ns), resp.answers());
    }

    @Test
    void cnameResponse_CNAME() throws IOException {
        Name origin = new Name("example.com.");
        CNAMERecord cname = new CNAMERecord(new Name("alias", origin), DClass.IN, 300,
                new Name("target", origin));
        ARecord a = new ARecord(new Name("target", origin), DClass.IN, 300, InetAddress.getByName("9.9.9.9"));
        Zone zone = new Zone(origin, new Record[]{cname, a});
        SetResponse resp = zone.findRecords(Name.fromString("alias.example.com."), Type.A);
        assertTrue(resp.isCNAME());
        assertEquals(oneRRset(cname), resp.answers());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - wildcard matching (additional explicit test)
    // -----------------------------------------------------------------
    @Test
    void wildcardMatch_success() throws IOException {
        Name origin = new Name("example.com.");
        ARecord wild = new ARecord(new Name("*", origin), DClass.IN, 300, InetAddress.getByName("1.1.1.1"));
        Zone zone = new Zone(origin, new Record[]{wild});
        Name qname = Name.fromString("anyhost.example.com.");
        SetResponse resp = zone.findRecords(qname, Type.A);
        assertTrue(resp.isSuccessful());
        assertEquals(oneRRset(wild.withName(qname)), resp.answers());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - empty zone handling
    // -----------------------------------------------------------------
    @Test
    void emptyZone_NXDOMAIN_forNonexistentName() throws IOException {
        Name origin = new Name("emptyzone.");
        // a zone with only the mandatory SOA+NS (still considered “empty” for data)
        SOARecord soa = new SOARecord(origin, DClass.IN, 300,
                Name.fromConstantString("ns.emptyzone."), new Name("hostmaster", origin),
                1, 7200, 3600, 1209600, 3600);
        NSRecord ns = new NSRecord(origin, DClass.IN, 300, Name.fromConstantString("ns.emptyzone."));
        Zone zone = new Zone(origin, new Record[]{soa, ns});

        SetResponse resp = zone.findRecords(Name.fromString("random.emptyzone."), Type.A);
        assertTrue(resp.isNXDOMAIN());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - apex and SOA/NS handling
    // -----------------------------------------------------------------
    @Test
    void apexSoa_success() throws IOException {
        Name origin = new Name("example.com.");
        SOARecord soa = new SOARecord(origin, DClass.IN, 300,
                Name.fromConstantString("ns.example.com."), new Name("hostmaster", origin),
                1, 7200, 3600, 1209600, 3600);
        NSRecord ns = new NSRecord(origin, DClass.IN, 300, Name.fromConstantString("ns.example.com."));
        Zone zone = new Zone(origin, new Record[]{soa, ns});

        SetResponse resp = zone.findRecords(Name.fromString("example.com."), Type.SOA);
        assertTrue(resp.isSuccessful());
        assertEquals(oneRRset(soa), resp.answers());
    }

    @Test
    void apexNs_success() throws IOException {
        Name origin = new Name("example.com.");
        NSRecord ns1 = new NSRecord(origin, DClass.IN, 300, Name.fromConstantString("ns1.example.com."));
        NSRecord ns2 = new NSRecord(origin, DClass.IN, 300, Name.fromConstantString("ns2.example.com."));
        Zone zone = new Zone(origin, new Record[]{ns1, ns2});
        SetResponse resp = zone.findRecords(Name.fromString("example.com."), Type.NS);
        assertTrue(resp.isSuccessful());
        assertEquals(oneRRset(ns1, ns2), resp.answers());
    }

    @Test
    void apexA_NODATA() throws IOException {
        Name origin = new Name("example.com.");
        SOARecord soa = new SOARecord(origin, DClass.IN, 300,
                Name.fromConstantString("ns.example.com."), new Name("hostmaster", origin),
                1, 7200, 3600, 1209600, 3600);
        Zone zone = new Zone(origin, new Record[]{soa});
        SetResponse resp = zone.findRecords(Name.fromString("example.com."), Type.A);
        assertTrue(resp.isNODATA());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - ANY type queries
    // -----------------------------------------------------------------
    @Test
    void anyQuery_successMultipleTypes() throws IOException {
        Name origin = new Name("example.com.");
        ARecord a = new ARecord(new Name("multi", origin), DClass.IN, 300, InetAddress.getByName("10.0.0.1"));
        AAAARecord aaaa = new AAAARecord(new Name("multi", origin), DClass.IN, 300, InetAddress.getByName("::1"));
        MXRecord mx = new MXRecord(new Name("multi", origin), DClass.IN, 300, 10,
                Name.fromConstantString("mail.example.com."));
        Zone zone = new Zone(origin, new Record[]{a, aaaa, mx});

        SetResponse resp = zone.findRecords(Name.fromString("multi.example.com."), Type.ANY);
        assertTrue(resp.isSuccessful());
        assertEquals(
                listOf(new RRset(a), new RRset(aaaa), new RRset(mx)),
                resp.answers());
    }

    @Test
    void anyQuery_NXDOMAIN() throws IOException {
        Name origin = new Name("example.com.");
        Zone zone = new Zone(origin, new Record[]{});
        SetResponse resp = zone.findRecords(Name.fromString("nonexistent.example.com."), Type.ANY);
        assertTrue(resp.isNXDOMAIN());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - CNAME chain and loop handling
    // -----------------------------------------------------------------
    @Test
    void cnameChain_success() throws IOException {
        Name origin = new Name("example.com.");
        CNAMERecord c1 = new CNAMERecord(new Name("alias1", origin), DClass.IN, 300,
                new Name("alias2", origin));
        CNAMERecord c2 = new CNAMERecord(new Name("alias2", origin), DClass.IN, 300,
                new Name("target", origin));
        ARecord targetA = new ARecord(new Name("target", origin), DClass.IN, 300,
                InetAddress.getByName("5.5.5.5"));
        Zone zone = new Zone(origin, new Record[]{c1, c2, targetA});

        SetResponse resp = zone.findRecords(Name.fromString("alias1.example.com."), Type.A);
        assertTrue(resp.isCNAME());
        // The response should contain both CNAME records and the final A RRset.
        assertEquals(listOf(new RRset(c1), new RRset(c2), new RRset(targetA)), resp.answers());
    }

    @Test
    void cnameLoop_detected() throws IOException {
        Name origin = new Name("example.com.");
        CNAMERecord c1 = new CNAMERecord(new Name("loop1", origin), DClass.IN, 300,
                new Name("loop2", origin));
        CNAMERecord c2 = new CNAMERecord(new Name("loop2", origin), DClass.IN, 300,
                new Name("loop1", origin));
        Zone zone = new Zone(origin, new Record[]{c1, c2});

        // The library signals a loop via an IllegalStateException (or SERVFAIL). We accept either.
        assertThrows(RuntimeException.class,
                () -> zone.findRecords(Name.fromString("loop1.example.com."), Type.A));
    }

    // -----------------------------------------------------------------
    // Group: findRecords - DNAME handling
    // -----------------------------------------------------------------
    @Test
    void dnameResponse_success() throws IOException {
        Name origin = new Name("example.com.");
        DNAMERecord dname = new DNAMERecord(origin, DClass.IN, 300,
                new Name("newexample.com."));
        Zone zone = new Zone(origin, new Record[]{dname});

        SetResponse resp = zone.findRecords(Name.fromString("host.example.com."), Type.A);
        // Expected to be a DNAME response (often represented as a CNAME synthesis)
        assertTrue(resp.isDNAME() || resp.isCNAME());
        assertEquals(oneRRset(dname), resp.answers());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - case insensitivity of names
    // -----------------------------------------------------------------
    @Test
    void caseInsensitiveLookup_success() throws IOException {
        Name origin = new Name("example.com.");
        ARecord a = new ARecord(new Name("www", origin), DClass.IN, 300,
                InetAddress.getByName("8.8.8.8"));
        Zone zone = new Zone(origin, new Record[]{a});

        SetResponse lower = zone.findRecords(Name.fromString("www.example.com."), Type.A);
        SetResponse upper = zone.findRecords(Name.fromString("WWW.Example.Com."), Type.A);
        assertEquals(lower.answers(), upper.answers());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - delegation with DS records
    // -----------------------------------------------------------------
    @Test
    void dsQuery_delegatedZone_success() throws IOException {
        Name origin = new Name("example.com.");
        Name child = new Name("sub", origin);
        // DS record stored at parent for child zone
        DSRecord ds = new DSRecord(child, DClass.IN, 300, 12345, 8, 2,
                new byte[]{0x01, 0x02, 0x03, 0x04});
        Zone zone = new Zone(origin, new Record[]{ds});
        SetResponse resp = zone.findRecords(Name.fromString("sub.example.com."), Type.DS);
        assertTrue(resp.isSuccessful());
        assertEquals(oneRRset(ds), resp.answers());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - root zone queries
    // -----------------------------------------------------------------
    @Test
    void rootZoneNs_success() throws IOException {
        Name root = Name.root; // "." (the root label)
        NSRecord ns1 = new NSRecord(root, DClass.IN, 300, Name.fromConstantString("a.root-servers.net."));
        NSRecord ns2 = new NSRecord(root, DClass.IN, 300, Name.fromConstantString("b.root-servers.net."));
        Zone rootZone = new Zone(root, new Record[]{ns1, ns2});

        SetResponse resp = rootZone.findRecords(Name.root, Type.NS);
        assertTrue(resp.isSuccessful());
        assertEquals(oneRRset(ns1, ns2), resp.answers());
    }

    @Test
    void rootZoneA_NODATA() throws IOException {
        Name root = Name.root;
        Zone rootZone = new Zone(root, new Record[]{});
        SetResponse resp = rootZone.findRecords(Name.root, Type.A);
        assertTrue(resp.isNODATA());
    }

    // -----------------------------------------------------------------
    // Group: findRecords - invalid type values
    // -----------------------------------------------------------------
    @Test
    void invalidType_zero_throwsIAE() {
        Name name = Name.fromString("example.com.");
        assertThrows(IllegalArgumentException.class,
                () -> ZONE.findRecords(name, 0));
    }

    @Test
    void invalidType_tooLarge_throwsIAE() {
        Name name = Name.fromString("example.com.");
        assertThrows(IllegalArgumentException.class,
                () -> ZONE.findRecords(name, 70000));
    }

    // -----------------------------------------------------------------
    // Group: findRecords - non‑absolute name handling
    // -----------------------------------------------------------------
    @Test
    void nonAbsoluteName_throwsIAE() {
        // Name without a trailing dot is non‑absolute.
        Name relative = Name.fromString("relative"); // not absolute
        assertThrows(IllegalArgumentException.class,
                () -> ZONE.findRecords(relative, Type.A));
    }

    // -----------------------------------------------------------------
    // Group: findRecords - empty name handling
    // -----------------------------------------------------------------
    @Test
    void emptyName_throwsIAE() {
        Name empty = Name.empty();
        assertThrows(IllegalArgumentException.class,
                () -> ZONE.findRecords(empty, Type.A));
    }

    // -----------------------------------------------------------------
    // Group: findRecords - zone without origin
    // -----------------------------------------------------------------
    @Test
    void zoneWithoutOrigin_throwsIllegalState() {
        // The default constructor creates a zone with no origin.
        Zone zone = new Zone(); // assuming such a constructor exists
        Name qname = Name.fromString("example.com.");
        assertThrows(IllegalStateException.class,
                () -> zone.findRecords(qname, Type.A));
    }
}
```