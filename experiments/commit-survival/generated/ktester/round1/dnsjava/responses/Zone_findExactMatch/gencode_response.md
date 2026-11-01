```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprehensive unit tests for {@link Zone#findExactMatch(Name, int)}.
 *
 * <p>These tests cover the behaviour described in the JSON test‑case specification
 * as well as the original hand‑written tests.  Where a test needs a specific
 * zone configuration a fresh {@link Zone} instance is created locally – the
 * global {@code zone} created in {@code @BeforeAll} is left untouched.</p>
 *
 * <p>All tests use only the public API of {@link Zone}.  For the
 * “internal state corruption” scenario we employ reflection to set the
 * private {@code rrsets} map to {@code null} – this mimics a corrupted
 * internal structure and forces the method to throw {@link IllegalStateException}
 * as documented.</p>
 */
class Zone_findExactMatch_Test {

    private static Zone zone;                 // zone with a typical set of records
    private static Name nameZone;             // origin name (example.)
    private static ARecord aRecord;
    private static AAAARecord aaaaRecord1;
    private static AAAARecord aaaaRecord2;
    private static ARecord wildARecord;
    private static TXTRecord wildTxtRecord;

    @BeforeAll
    static void setupBeforeAll() {
        try {
            nameZone = new Name("example.");
            InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
            InetAddress localhost6a = InetAddress.getByName("::1");
            InetAddress localhost6b = InetAddress.getByName("::2");

            aRecord = new ARecord(new Name("test", nameZone), DClass.IN, 3600, localhost4);
            aaaaRecord1 = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6a);
            aaaaRecord2 = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6b);
            wildARecord = new ARecord(new Name("*", nameZone), DClass.IN, 3600, localhost4);
            wildTxtRecord = new TXTRecord(new Name("*", nameZone), DClass.IN, 3600, "sometext");

            Record[] records = new Record[] {
                new SOARecord(
                    nameZone,
                    DClass.IN,
                    3600L,
                    Name.fromConstantString("nameserver."),
                    new Name("hostmaster", nameZone),
                    1,
                    21600L,
                    7200L,
                    2160000L,
                    3600L),
                new NSRecord(nameZone, DClass.IN, 300L, Name.fromConstantString("nameserver.")),
                aRecord,
                aaaaRecord1,
                aaaaRecord2,
                wildARecord,
                wildTxtRecord
            };

            zone = new Zone(nameZone, records);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- */
    /* Existing hand‑written tests (kept unchanged)                         */
    /* --------------------------------------------------------------------- */

    @Test
    void exactMatchSingleARecord() {
        Name query = Name.fromConstantString("test.example.");
        RRset result = zone.findExactMatch(query, Type.A);
        assertNotNull(result, "Result should not be null for an existing A record");
        RRset expected = new RRset(aRecord);
        assertEquals(expected, result, "RRset returned should contain the expected A record");
    }

    @Test
    void exactMatchMultipleAaaaRecords() {
        Name query = Name.fromConstantString("test.example.");
        RRset result = zone.findExactMatch(query, Type.AAAA);
        assertNotNull(result, "Result should not be null for existing AAAA records");
        RRset expected = new RRset(aaaaRecord1, aaaaRecord2);
        assertEquals(expected, result, "RRset should contain both AAAA records");
    }

    @Test
    void exactMatchNonExistingTypeReturnsNull() {
        Name query = Name.fromConstantString("test.example.");
        RRset result = zone.findExactMatch(query, Type.MX);
        assertNull(result, "Result should be null when the name exists but the type does not");
    }

    @Test
    void exactMatchNonExistingNameReturnsNull() {
        Name query = Name.fromConstantString("nosuch.example.");
        RRset result = zone.findExactMatch(query, Type.A);
        assertNull(result, "Result should be null when the name does not exist in the zone");
    }

    @Test
    void exactMatchWildcardIsNotReturned() {
        // findExactMatch does not perform wildcard expansion, it should return null
        Name query = Name.fromConstantString("anything.example.");
        RRset result = zone.findExactMatch(query, Type.A);
        assertNull(result, "Wildcard records are not returned by findExactMatch");
    }

    /* --------------------------------------------------------------------- */
    /* Additional tests generated from the JSON specification                */
    /* --------------------------------------------------------------------- */

    /** Helper to create a Zone with an arbitrary set of Records. */
    private static Zone createZone(Record... records) throws IOException {
        return new Zone(nameZone, records);
    }

    /** Test group: name not present (exactName returns null). */
    @Test
    void findExactMatch_nameNotPresent_returnsNull() {
        // The global 'zone' does not contain "nonexistent.example."
        Name query = new Name("nonexistent.example.");
        RRset result = zone.findExactMatch(query, Type.A);
        assertNull(result, "When the name is absent, findExactMatch must return null");
    }

    /** Test group: exact type match – simple A record. */
    @Test
    void findExactMatch_exactTypeMatch_returnsRRset() throws IOException {
        ARecord wwwA = new ARecord(new Name("www", nameZone), DClass.IN, 300, InetAddress.getByName("192.0.2.1"));
        Zone z = createZone(wwwA);
        RRset result = z.findExactMatch(Name.fromConstantString("www.example."), Type.A);
        assertNotNull(result);
        RRset expected = new RRset(wwwA);
        assertEquals(expected, result);
    }

    /** Test group: name present but requested type absent. */
    @Test
    void findExactMatch_namePresent_typeAbsent_returnsNull() throws IOException {
        // MX record for mail.example.
        MXRecord mx = new MXRecord(new Name("mail", nameZone), DClass.IN, 300, 10,
                new Name("mailhost.example."));
        Zone z = createZone(mx);
        RRset result = z.findExactMatch(Name.fromConstantString("mail.example."), Type.SRV);
        assertNull(result, "Existing name with no matching type should yield null");
    }

    /** Test group: null name argument – expect NPE. */
    @Test
    void findExactMatch_nullName_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> zone.findExactMatch(null, Type.A),
                "Passing null as the name must throw NullPointerException");
    }

    /** Test group: origin name with SOA type. */
    @Test
    void findExactMatch_originSOA_returnsRRset() {
        RRset result = zone.findExactMatch(nameZone, Type.SOA);
        assertNotNull(result);
        // The zone constructor added exactly one SOA record.
        SOARecord soa = (SOARecord) zone.getSOA();
        RRset expected = new RRset(soa);
        assertEquals(expected, result);
    }

    /** Test group: origin name with NS type. */
    @Test
    void findExactMatch_originNS_returnsRRset() {
        RRset result = zone.findExactMatch(nameZone, Type.NS);
        assertNotNull(result);
        // The zone constructor added exactly one NS record.
        NSRecord ns = zone.getNS();
        RRset expected = new RRset(ns);
        assertEquals(expected, result);
    }

    /** Parameterized test for a name that has both A and AAAA records. */
    @ParameterizedTest(name = "{index} => type={1}")
    @MethodSource("provideNameWithMultipleRRsets")
    void findExactMatch_nameWithMultipleRRsets(Name query, int type, RRset expected) throws IOException {
        // Create a zone that contains both A and AAAA records for www.example.
        ARecord a = new ARecord(new Name("www", nameZone), DClass.IN, 300, InetAddress.getByName("192.0.2.1"));
        AAAARecord aaaa = new AAAARecord(new Name("www", nameZone), DClass.IN, 300,
                InetAddress.getByName("2001:db8::1"));
        Zone z = createZone(a, aaaa);

        RRset result = z.findExactMatch(query, type);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideNameWithMultipleRRsets() throws IOException {
        Name query = Name.fromConstantString("www.example.");
        ARecord a = new ARecord(new Name("www", nameZone), DClass.IN, 300, InetAddress.getByName("192.0.2.1"));
        AAAARecord aaaa = new AAAARecord(new Name("www", nameZone), DClass.IN, 300,
                InetAddress.getByName("2001:db8::1"));
        RRset aSet = new RRset(a);
        RRset aaaaSet = new RRset(aaaa);
        return Stream.of(
                Arguments.of(query, Type.A, aSet),
                Arguments.of(query, Type.AAAA, aaaaSet)
        );
    }

    /** Test group: request TYPE_ANY (0) – should return null. */
    @Test
    void findExactMatch_typeAny_returnsNull() {
        Name query = Name.fromConstantString("test.example.");
        RRset result = zone.findExactMatch(query, Type.ANY);
        assertNull(result, "TYPE_ANY is not a concrete type and must return null");
    }

    /** Test group: request negative type – should return null (no exception). */
    @Test
    void findExactMatch_negativeType_returnsNull() {
        Name query = Name.fromConstantString("test.example.");
        RRset result = zone.findExactMatch(query, -1);
        assertNull(result, "Negative type values are invalid and should result in null");
    }

    /** Test group: name with CNAME – request CNAME returns RRset. */
    @Test
    void findExactMatch_cnamePresent_returnsRRset() throws IOException {
        CNAMERecord cname = new CNAMERecord(new Name("alias", nameZone), DClass.IN, 300,
                new Name("target.example."));
        Zone z = createZone(cname);
        RRset result = z.findExactMatch(Name.fromConstantString("alias.example."), Type.CNAME);
        assertNotNull(result);
        RRset expected = new RRset(cname);
        assertEquals(expected, result);
    }

    /** Test group: name with CNAME – request A returns null. */
    @Test
    void findExactMatch_cnamePresent_wrongType_returnsNull() throws IOException {
        CNAMERecord cname = new CNAMERecord(new Name("alias", nameZone), DClass.IN, 300,
                new Name("target.example."));
        Zone z = createZone(cname);
        RRset result = z.findExactMatch(Name.fromConstantString("alias.example."), Type.A);
        assertNull(result, "A request for a CNAME‑only name must return null");
    }

    /** Test group: empty zone – any query returns null. */
    @Test
    void findExactMatch_emptyZone_returnsNull() throws IOException {
        Zone empty = new Zone(nameZone, new Record[0]);
        RRset result = empty.findExactMatch(Name.fromConstantString("any.example."), Type.A);
        assertNull(result, "An empty zone should never return a non‑null RRset");
    }

    /** Test group: name with DNSSEC RRSIG records – request RRSIG returns RRset. */
    @Test
    void findExactMatch_rrsigPresent_returnsRRset() throws IOException {
        // Simplified RRSIGRecord – the constructor is verbose; we only need a valid instance.
        // Using the public constructor that mirrors the one in dnsjava.
        RRSIGRecord rrsig = new RRSIGRecord(
                new Name("www", nameZone), DClass.IN, 300,
                Type.A, // type covered
                1,      // algorithm
                0,      // labels
                3600,   // original TTL
                0,      // signature expiration
                0,      // signature inception
                new Name("key.example."), // signer name
                new byte[]{0}); // dummy signature
        Zone z = createZone(rrsig);
        RRset result = z.findExactMatch(Name.fromConstantString("www.example."), Type.RRSIG);
        assertNotNull(result);
        RRset expected = new RRset(rrsig);
        assertEquals(expected, result);
    }

    /** Test group: type value exceeds DNS maximum – expect IllegalArgumentException. */
    @Test
    void findExactMatch_typeTooLarge_throwsIllegalArgumentException() {
        Name query = Name.fromConstantString("test.example.");
        assertThrows(IllegalArgumentException.class,
                () -> zone.findExactMatch(query, 70000),
                "Type values > 65535 must cause IllegalArgumentException");
    }

    /** Test group: type value is Integer.MAX_VALUE – expect IllegalArgumentException. */
    @Test
    void findExactMatch_typeIntegerMax_throwsIllegalArgumentException() {
        Name query = Name.fromConstantString("example.");
        assertThrows(IllegalArgumentException.class,
                () -> zone.findExactMatch(query, Integer.MAX_VALUE),
                "Extremely large type values must be rejected");
    }

    /** Test group: non‑absolute Name argument – expect IllegalArgumentException. */
    @Test
    void findExactMatch_relativeName_throwsIllegalArgumentException() {
        // The Name constructor with 'false' creates a relative name.
        Name relative = new Name("relative", false);
        assertThrows(IllegalArgumentException.class,
                () -> zone.findExactMatch(relative, Type.A),
                "Relative names must be rejected with IllegalArgumentException");
    }

    /** Test group: internal state corruption – simulate null internal map. */
    @Test
    void findExactMatch_corruptedInternalState_throwsIllegalStateException() throws Exception {
        // Use reflection to corrupt the private map that backs exactName().
        // The actual field name in dnsjava is "rrsets". Adjust if the implementation changes.
        Field mapField = Zone.class.getDeclaredField("rrsets");
        mapField.setAccessible(true);
        // Create a normal zone first.
        Zone corrupted = new Zone(nameZone, new Record[0]);
        // Corrupt the internal map.
        mapField.set(corrupted, null);

        // Now invoke findExactMatch – we expect IllegalStateException due to the null map.
        assertThrows(IllegalStateException.class,
                () -> corrupted.findExactMatch(Name.fromConstantString("any.example."), Type.A),
                "A null internal RRset map should cause IllegalStateException");
    }
}
```