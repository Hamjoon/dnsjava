```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
 * Comprehensive tests for {@link Zone#addRRset(RRset)}.
 *
 * <p>The test suite covers normal behaviour, edge‑cases and error handling as described
 * in the supplied JSON test matrix.  Where the {@code Zone} implementation validates
 * arguments (null checks, name checks, type checks, duplicate handling, …) the tests
 * assert that the appropriate exception is thrown.  For successful additions the
 * records are retrieved via {@link Zone#findExactMatch(Name, int)} or
 * {@link Zone#findRecords(Name, int)} and compared with the expected {@link RRset}
 * content.</p>
 *
 * <p>Mockito is used only for a few artificial {@code RRset} instances where the
 * library does not provide a public constructor that allows us to create an invalid
 * state (e.g. a set with a null name, a null record inside, or an unsupported type).</p>
 */
@ExtendWith(MockitoExtension.class)
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
        zone = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up
    }

    /** Helper to build a List<RRset> from var‑args. */
    private static List<RRset> listOf(RRset... rrsets) {
        return Stream.of(rrsets).collect(Collectors.toList());
    }

    /** Helper to build a singleton List<RRset> containing the given records. */
    private static List<RRset> oneRRset(Record... records) {
        return Collections.singletonList(new RRset(records));
    }

    /* --------------------------------------------------------------------- */
    /* Existing positive tests (kept from the original scaffold)            */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* New tests derived from the JSON matrix                               */
    /* --------------------------------------------------------------------- */

    // -----------------------------------------------------------------
    // Null handling
    // -----------------------------------------------------------------
    @Test
    void addRRset_nullArgument_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> zone.addRRset(null),
                "Calling addRRset with null should throw NullPointerException");
    }

    @Test
    void addRRset_rrsetWithNullName_throwsNPE() {
        // Build a mock RRset whose getName() returns null.
        RRset mock = mock(RRset.class);
        when(mock.getName()).thenReturn(null);
        // The mock still needs to behave like a normal RRset for other calls.
        when(mock.iterator()).thenReturn(Collections.emptyIterator());

        assertThrows(NullPointerException.class,
                () -> zone.addRRset(mock),
                "RRset with a null Name should cause NullPointerException");
    }

    // -----------------------------------------------------------------
    // Normal addition
    // -----------------------------------------------------------------
    @Test
    void addRRset_normalARecord_isFindableViaExactMatch() throws Exception {
        Name name = new Name("example.com.", zoneName);
        ARecord a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("93.184.216.34"));
        RRset rrset = new RRset(a);

        zone.addRRset(rrset);

        RRset found = zone.findExactMatch(name, Type.A);
        assertNotNull(found, "findExactMatch should return a non‑null RRset");
        assertEquals(rrset, found, "The stored RRset should be identical to the one added");
    }

    // -----------------------------------------------------------------
    // Merge duplicate RRset
    // -----------------------------------------------------------------
    @Test
    void addRRset_duplicateSameRRset_mergesWithoutDuplicates() throws Exception {
        Name name = new Name("www.example.com.", zoneName);
        TXTRecord txt = new TXTRecord(name, DClass.IN, 300, "v=spf1 -all");
        RRset rrset = new RRset(txt);

        // first addition
        zone.addRRset(rrset);
        // second addition – same instance
        zone.addRRset(rrset);

        RRset found = zone.findExactMatch(name, Type.TXT);
        assertNotNull(found);
        // The RRset should contain exactly one record (no duplicate)
        assertEquals(1, found.size(), "RRset should contain a single TXT record after merging");
        assertEquals(txt, found.first(), "The stored record must be the original TXT record");
    }

    // -----------------------------------------------------------------
    // Empty RRset
    // -----------------------------------------------------------------
    @Test
    void addRRset_emptyRRset_throwsIllegalArgumentException() {
        RRset empty = new RRset(); // constructs an empty set
        assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(empty),
                "Adding an empty RRset should throw IllegalArgumentException");
    }

    // -----------------------------------------------------------------
    // Name outside zone origin
    // -----------------------------------------------------------------
    @Test
    void addRRset_nameOutsideZoneOrigin_throwsIllegalArgumentException() throws Exception {
        Name outside = new Name("outside.com.");
        ARecord a = new ARecord(outside, DClass.IN, 600, InetAddress.getByName("1.2.3.4"));
        RRset rrset = new RRset(a);

        assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(rrset),
                "RRset whose name is not a sub‑domain of the zone origin must be rejected");
    }

    // -----------------------------------------------------------------
    // Different types same name
    // -----------------------------------------------------------------
    @Test
    void addRRset_sameNameDifferentTypes_storedIndependently() throws Exception {
        Name name = new Name("host.example.com.", zoneName);
        ARecord a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        AAAARecord aaaa = new AAAARecord(name, DClass.IN, 3600, InetAddress.getByName("2001:db8::1"));

        zone.addRRset(new RRset(a));
        zone.addRRset(new RRset(aaaa));

        RRset aFound = zone.findExactMatch(name, Type.A);
        RRset aaaaFound = zone.findExactMatch(name, Type.AAAA);

        assertNotNull(aFound);
        assertNotNull(aaaaFound);
        assertEquals(a, aFound.first());
        assertEquals(aaaa, aaaaFound.first());
    }

    // -----------------------------------------------------------------
    // Adding SOA record
    // -----------------------------------------------------------------
    @Test
    void addRRset_additionalSOA_updatesSOA() throws Exception {
        // The zone already has an SOA (created in @BeforeEach). Adding another
        // SOA for the same origin should replace it (implementation defined). The
        // test checks that getSOA() now returns the newly added record.
        SOARecord newSOA = new SOARecord(
                zoneName,
                DClass.IN,
                3600,
                Name.fromConstantString("ns1.example.com."),
                new Name("hostmaster", zoneName),
                2023101501L,
                7200,
                3600,
                1209600,
                3600);
        RRset rrset = new RRset(newSOA);

        zone.addRRset(rrset);

        assertEquals(newSOA, zone.getSOA(), "getSOA() should return the newly added SOA record");
    }

    // -----------------------------------------------------------------
    // Adding NS record
    // -----------------------------------------------------------------
    @Test
    void addRRset_additionalNS_returnsAllNSRecords() throws Exception {
        // Build a new NS RRset containing two NS records.
        NSRecord ns1 = new NSRecord(zoneName, DClass.IN, 3600, Name.fromConstantString("ns1.example.com."));
        NSRecord ns2 = new NSRecord(zoneName, DClass.IN, 3600, Name.fromConstantString("ns2.example.com."));
        RRset nsRRset = new RRset(ns1, ns2);

        zone.addRRset(nsRRset);

        RRset stored = zone.getNS();
        assertNotNull(stored);
        // The original NS from the minimal zone plus the two we just added = 3 records.
        assertEquals(3, stored.size(), "NS RRset should contain three NS records");
        assertTrue(stored.contains(ns1));
        assertTrue(stored.contains(ns2));
    }

    // -----------------------------------------------------------------
    // Large RRset
    // -----------------------------------------------------------------
    @Test
    void addRRset_largeRRset_isHandledCorrectly() throws Exception {
        Name name = new Name("big.example.com.", zoneName);
        Record[] aRecs = new Record[10];
        for (int i = 0; i < 10; i++) {
            InetAddress ip = InetAddress.getByName("10.0.0." + (i + 1));
            aRecs[i] = new ARecord(name, DClass.IN, 300, ip);
        }
        RRset bigSet = new RRset(aRecs);

        zone.addRRset(bigSet);

        RRset found = zone.findExactMatch(name, Type.A);
        assertNotNull(found);
        assertEquals(10, found.size(), "All ten A records should be present");
        // Ensure each record we added is present in the stored set.
        for (Record r : aRecs) {
            assertTrue(found.contains(r));
        }
    }

    // -----------------------------------------------------------------
    // Record name mismatch
    // -----------------------------------------------------------------
    @Test
    void addRRset_recordNameMismatch_throwsIllegalArgumentException() throws Exception {
        // Mock an RRset whose declared name is host.example.com. but whose iterator
        // yields a record whose owner is other.example.com.
        Name rrsetName = new Name("host.example.com.", zoneName);
        Name mismatchedName = new Name("other.example.com.", zoneName);
        ARecord mismatchedRecord = new ARecord(mismatchedName, DClass.IN, 3600,
                InetAddress.getByName("192.0.2.1"));

        RRset mock = mock(RRset.class);
        when(mock.getName()).thenReturn(rrsetName);
        when(mock.getType()).thenReturn(Type.A);
        when(mock.iterator()).thenReturn(Collections.singletonList(mismatchedRecord).iterator());

        assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(mock),
                "RRset containing a record whose name differs from the RRset name must be rejected");
    }

    // -----------------------------------------------------------------
    // Null record inside RRset
    // -----------------------------------------------------------------
    @Test
    void addRRset_nullRecordInsideRRset_throwsNullPointerException() {
        // Mock an RRset that returns a null Record from its iterator.
        Name name = new Name("host.example.com.", zoneName);
        RRset mock = mock(RRset.class);
        when(mock.getName()).thenReturn(name);
        when(mock.getType()).thenReturn(Type.A);
        when(mock.iterator()).thenReturn(Collections.singletonList(null).iterator());

        assertThrows(NullPointerException.class,
                () -> zone.addRRset(mock),
                "RRset containing a null Record should cause NullPointerException");
    }

    // -----------------------------------------------------------------
    // Record type mismatch
    // -----------------------------------------------------------------
    @Test
    void addRRset_recordTypeMismatch_throwsIllegalArgumentException() throws Exception {
        // RRset claims to be type A but contains an AAAARecord.
        Name name = new Name("host.example.com.", zoneName);
        AAAARecord aaaa = new AAAARecord(name, DClass.IN, 3600,
                InetAddress.getByName("2001:db8::1"));

        RRset mock = mock(RRset.class);
        when(mock.getName()).thenReturn(name);
        when(mock.getType()).thenReturn(Type.A);
        when(mock.iterator()).thenReturn(Collections.singletonList(aaaa).iterator());

        assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(mock),
                "RRset whose records have a different type than the RRset type must be rejected");
    }

    // -----------------------------------------------------------------
    // Duplicate SOA record
    // -----------------------------------------------------------------
    @Test
    void addRRset_duplicateSOA_throwsIllegalArgumentException() throws Exception {
        // The zone already contains an SOA (created in @BeforeEach). Adding another
        // SOA for the same origin should be illegal.
        SOARecord anotherSOA = new SOARecord(
                zoneName,
                DClass.IN,
                3600,
                Name.fromConstantString("ns2.example.com."),
                new Name("hostmaster", zoneName),
                2023101502L,
                7200,
                3600,
                1209600,
                3600);
        RRset rrset = new RRset(anotherSOA);

        assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(rrset),
                "Adding a second SOA RRset should raise IllegalArgumentException");
    }

    // -----------------------------------------------------------------
    // Duplicate NS record
    // -----------------------------------------------------------------
    @Test
    void addRRset_duplicateNSRecord_throwsIllegalArgumentException() throws Exception {
        // Create an NS RRset that contains the same NS record twice.
        NSRecord ns = new NSRecord(zoneName, DClass.IN, 3600, Name.fromConstantString("ns.example."));
        RRset dupNsSet = new RRset(ns, ns); // duplicate entries

        assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(dupNsSet),
                "NS RRset with duplicate NS records should be rejected");
    }

    // -----------------------------------------------------------------
    // Relative (non‑absolute) name
    // -----------------------------------------------------------------
    @Test
    void addRRset_relativeName_throwsIllegalArgumentException() throws Exception {
        // Name without a trailing dot is relative.
        Name relative = new Name("relative"); // this is a relative name
        ARecord a = new ARecord(relative, DClass.IN, 3600,
                InetAddress.getByName("192.0.2.1"));
        RRset rrset = new RRset(a);

        assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(rrset),
                "RRset with a relative (non‑absolute) name must be rejected");
    }

    // -----------------------------------------------------------------
    // Unsupported record type (ANY)
    // -----------------------------------------------------------------
    @Test
    void addRRset_unsupportedANYType_throwsIllegalArgumentException() {
        // Mock an RRset that pretends to be of type ANY.
        RRset mock = mock(RRset.class);
        when(mock.getName()).thenReturn(zoneName);
        when(mock.getType()).thenReturn(Type.ANY);
        // Provide at least one concrete record so that iteration works.
        ARecord a = new ARecord(zoneName, DClass.IN, 3600, InetAddress.getLoopbackAddress());
        when(mock.iterator()).thenReturn(Collections.singletonList(a).iterator());

        assertThrows(IllegalArgumentException.class,
                () -> zone.addRRset(mock),
                "Adding an RRset of type ANY should raise IllegalArgumentException");
    }
}
```