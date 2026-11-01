package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class Zone_toString_Test {

    /**
     * Zone instance used by all tests.
     */
    private static Zone zone;

    /**
     * Expected master‑file representation obtained via the public {@code toMasterFile()} method.
     */
    private static String expectedMasterFile;

    class BadRecord extends Record {

        BadRecord(Name name) {
            super(name, Type.TXT, DClass.IN, 3600);
        }

        @Override
        public String rdataToString() throws IOException {
            throw new IOException("simulated I/O failure");
        }

        @Override
        public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
            // No‑op – not needed for this test
        }
    }

    class BadRecord extends Record {

        BadRecord(Name name) {
            super(name, Type.TXT, DClass.IN, 3600);
        }

        @Override
        public String rdataToString() throws IOException {
            throw new IOException("simulated I/O failure");
        }

        @Override
        public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
            // No‑op – not needed for this test
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        try {
            // Build the same zone that is used in the original ZoneTest.
            Name nameZone = new Name("example.");
            InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
            InetAddress localhost6a = InetAddress.getByName("::1");
            InetAddress localhost6b = InetAddress.getByName("::2");
            ARecord aTest = new ARecord(new Name("test", nameZone), DClass.IN, 3600, localhost4);
            AAAARecord aaaa1 = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6a);
            AAAARecord aaaa2 = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6b);
            ARecord aWild = new ARecord(new Name("*", nameZone), DClass.IN, 3600, localhost4);
            TXTRecord txtWild = new TXTRecord(new Name("*", nameZone), DClass.IN, 3600, "sometext");
            Record[] records = new Record[] { new SOARecord(nameZone, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostmaster", nameZone), 1, 21600L, 7200L, 2160000L, 3600L), new NSRecord(nameZone, DClass.IN, 300L, Name.fromConstantString("nameserver.")), aTest, aaaa1, aaaa2, aWild, txtWild };
            zone = new Zone(nameZone, records);
            // Capture the master‑file representation once; it will be used as the reference.
            expectedMasterFile = zone.toMasterFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test Zone instance", e);
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references (helps GC in large test suites).
        zone = null;
        expectedMasterFile = null;
    }

    @Test
    void toStringReturnsSameAsToMasterFile() {
        String viaToString = zone.toString();
        assertEquals(expectedMasterFile, viaToString, "Zone.toString() should return the same string as Zone.toMasterFile()");
    }

    @Test
    void toStringContainsEssentialRecords() {
        String master = zone.toString();
        // Very basic sanity checks – the master file should contain the SOA and NS records,
        // as well as at least one A record and one AAAA record.
        assertTrue(master.contains("SOA"), "Master file should contain an SOA record");
        assertTrue(master.contains("NS"), "Master file should contain an NS record");
        assertTrue(master.contains("A "), "Master file should contain an A record");
        assertTrue(master.contains("AAAA"), "Master file should contain an AAAA record");
        // Wildcard records should also be present.
        assertTrue(master.contains("*"), "Master file should contain wildcard entries");
    }

    /**
     * Empty Zone – toString() must return an empty string.
     */
    @Test
    void emptyZoneReturnsEmptyString() throws IOException {
        Name origin = new Name("empty.");
        // constructor without records
        Zone empty = new Zone(origin);
        assertEquals("", empty.toString(), "A newly created empty Zone should produce an empty master‑file string");
        assertEquals("", empty.toMasterFile(), "toMasterFile() must also return an empty string for an empty zone");
    }

    /**
     * Single Record Zone – only the A record should appear.
     */
    @Test
    void singleRecordZoneContainsOnlyThatRecord() throws IOException {
        Name origin = new Name("example.com.");
        Zone single = new Zone(origin);
        ARecord a = new ARecord(new Name("www", origin), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        single.addRecord(a);
        String master = single.toString();
        // The string must contain the A record line and nothing else (except optional $ORIGIN/$TTL)
        assertTrue(master.contains("www.example.com.\t3600\tIN\tA\t192.0.2.1"), "Master file must contain the added A record");
        // No MX, NS, SOA etc. – we simply assert that they are absent.
        assertFalse(master.contains("MX"), "No MX record should be present");
        assertFalse(master.contains("NS"), "No NS record should be present");
        assertFalse(master.contains("SOA"), "No SOA record should be present");
    }

    /**
     * Multiple Records Zone – verify that all added records appear.
     */
    @Test
    void multipleRecordsZoneListsAllRecords() throws IOException {
        Name origin = new Name("example.org.");
        Zone multi = new Zone(origin);
        // SOA (mandatory for a well‑formed zone)
        SOARecord soa = new SOARecord(origin, DClass.IN, 3600, Name.fromConstantString("ns1.example.org."), new Name("hostmaster", origin), 1, 7200, 3600, 1209600, 3600);
        multi.addRecord(soa);
        // NS
        NSRecord ns = new NSRecord(origin, DClass.IN, 3600, Name.fromConstantString("ns1.example.org."));
        multi.addRecord(ns);
        // A
        ARecord a = new ARecord(new Name("www", origin), DClass.IN, 3600, InetAddress.getByName("192.0.2.10"));
        multi.addRecord(a);
        // MX
        MXRecord mx = new MXRecord(origin, DClass.IN, 3600, 10, new Name("mail.example.org."));
        multi.addRecord(mx);
        String master = multi.toString();
        // Verify presence of each record type (order is not strictly enforced here)
        assertAll("All mandatory records must be present", () -> assertTrue(master.contains("SOA"), "SOA line missing"), () -> assertTrue(master.contains("NS"), "NS line missing"), () -> assertTrue(master.contains("A "), "A line missing"), () -> assertTrue(master.contains("MX"), "MX line missing"));
    }

    /**
     * Zone with only mandatory SOA and NS – ensure they appear in the correct order.
     */
    @Test
    void zoneWithOnlySOAandNSStartsWithSOA() throws IOException {
        Name origin = new Name("mandatory.");
        Zone minimal = new Zone(origin);
        SOARecord soa = new SOARecord(origin, DClass.IN, 3600, Name.fromConstantString("ns.mandatory."), new Name("hostmaster", origin), 1, 7200, 3600, 1209600, 3600);
        NSRecord ns = new NSRecord(origin, DClass.IN, 3600, Name.fromConstantString("ns.mandatory."));
        minimal.addRecord(soa);
        minimal.addRecord(ns);
        String master = minimal.toString();
        // The first non‑directive line should be the SOA record.
        String[] lines = master.split("\n");
        int firstRecordIdx = 0;
        while (firstRecordIdx < lines.length && lines[firstRecordIdx].startsWith("$")) {
            firstRecordIdx++;
        }
        assertTrue(lines[firstRecordIdx].contains("SOA"), "The master file should start with the SOA record");
        assertTrue(master.contains("NS"), "NS record must be present");
    }

    /**
     * After removing a TXT record, toString() must no longer contain it.
     */
    @Test
    void zoneAfterRecordRemovalDoesNotContainRemovedRecord() throws IOException {
        Name origin = new Name("removal.");
        Zone z = new Zone(origin);
        TXTRecord txt = new TXTRecord(new Name("info", origin), DClass.IN, 3600, "hello");
        z.addRecord(txt);
        // Ensure it is there
        assertTrue(z.toString().contains("info.removal.\t3600\tIN\tTXT\thello"));
        // Remove and verify disappearance
        z.removeRecord(txt);
        assertFalse(z.toString().contains("TXT"), "Removed TXT record must not appear in the master file");
    }

    /**
     * Duplicate A records (same name, different data) must both be represented.
     */
    @Test
    void zoneWithDuplicateARecordsShowsBothAddresses() throws IOException {
        Name origin = new Name("duplicate.");
        Zone dup = new Zone(origin);
        ARecord a1 = new ARecord(new Name("www", origin), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        ARecord a2 = new ARecord(new Name("www", origin), DClass.IN, 3600, InetAddress.getByName("192.0.2.2"));
        dup.addRecord(a1);
        dup.addRecord(a2);
        String master = dup.toString();
        // The record line may list both addresses separated by spaces or appear on two lines.
        assertTrue(master.contains("192.0.2.1") && master.contains("192.0.2.2"), "Both IP addresses of duplicate A records must be present");
    }

    /**
     * Stress test with 1 000 A records – ensure no truncation and correct line count.
     */
    @Test
    // guard against runaway execution
    @Timeout(10)
    void largeZoneStressTest() throws IOException {
        Name origin = new Name("stress.");
        Zone big = new Zone(origin);
        // Add a mandatory SOA and NS first (required by toMasterFile)
        SOARecord soa = new SOARecord(origin, DClass.IN, 3600, Name.fromConstantString("ns.stress."), new Name("hostmaster", origin), 1, 7200, 3600, 1209600, 3600);
        NSRecord ns = new NSRecord(origin, DClass.IN, 3600, Name.fromConstantString("ns.stress."));
        big.addRecord(soa);
        big.addRecord(ns);
        for (int i = 0; i < 1000; i++) {
            String host = "host" + i;
            ARecord a = new ARecord(new Name(host, origin), DClass.IN, 3600, InetAddress.getByName("192.0.2." + (i % 254 + 1)));
            big.addRecord(a);
        }
        String master = big.toString();
        // Verify first and last host lines are present
        assertTrue(master.contains("host0.stress."), "First host line missing");
        assertTrue(master.contains("host999.stress."), "Last host line missing");
        // Count newline characters – should be records + 2 (SOA + NS) + possible directives
        long newlineCount = master.chars().filter(ch -> ch == '\n').count();
        assertTrue(newlineCount >= 1002, "Number of lines should be at least number of records added plus SOA/NS");
    }

    /**
     * General consistency: toString() must always equal toMasterFile().
     */
    @Test
    void toStringAlwaysEqualsToMasterFile() throws IOException {
        // Re‑use the zone created in @BeforeAll (it already contains many records)
        assertEquals(zone.toMasterFile(), zone.toString(), "toString() must be identical to toMasterFile() for any zone state");
    }

    /**
     * Setting the internal origin to null must cause a NullPointerException.
     */
    @Test
    void nullOriginLeadsToNullPointerException() throws Exception {
        // Clone the original zone to avoid contaminating other tests
        Zone copy = new Zone(zone.getOrigin(), new Record[0]);
        // Populate it with the same records (simpler: use reflection to copy the map)
        // guessed name
        Field rrsetsField = Zone.class.getDeclaredField("rrsets");
        rrsetsField.setAccessible(true);
        rrsetsField.set(copy, rrsetsField.get(zone));
        // Now force the private origin field to null
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        originField.set(copy, null);
        Executable call = copy::toString;
        assertThrows(NullPointerException.class, call, "toString() should throw NPE when the zone's origin is null");
    }

    /**
     * Nullifying the internal RRset map must cause a NullPointerException.
     */
    @Test
    void nullRRsetMapLeadsToNullPointerException() throws Exception {
        Zone copy = new Zone(zone.getOrigin(), new Record[0]);
        // Populate with at least one record so that toString() attempts to iterate.
        copy.addRecord(new ARecord(new Name("a", copy.getOrigin()), DClass.IN, 3600, InetAddress.getByName("127.0.0.1")));
        // Set the map that holds RRsets to null (field name guessed as "rrsets")
        Field rrsetsField = Zone.class.getDeclaredField("rrsets");
        rrsetsField.setAccessible(true);
        rrsetsField.set(copy, null);
        Executable call = copy::toString;
        assertThrows(NullPointerException.class, call, "toString() should throw NPE when the internal RRset map is null");
    }

    /**
     * Adding a null Record and then calling toString() must raise NullPointerException.
     */
    @Test
    void addNullRecordLeadsToExceptionOnToString() {
        Zone z = new Zone(new Name("nullrecord."));
        // Directly add a null record – the API does not guard against it.
        z.addRecord(null);
        Executable call = z::toString;
        assertThrows(NullPointerException.class, call, "toString() should throw NPE when a null Record was added");
    }

    /**
     * Adding a Record whose name is null should result in IllegalArgumentException during toString().
     */
    @Test
    void recordWithNullNameLeadsToIllegalArgumentException() {
        // Create a mock Record that returns null for getName()
        Record malformed = Assertions.assertDoesNotThrow(() -> {
            Record r = org.mockito.Mockito.mock(Record.class);
            org.mockito.Mockito.when(r.getName()).thenReturn(null);
            // other methods can return dummy values
            org.mockito.Mockito.when(r.getType()).thenReturn(Type.A);
            org.mockito.Mockito.when(r.getTTL()).thenReturn(3600);
            org.mockito.Mockito.when(r.getClassValue()).thenReturn(DClass.IN);
            return r;
        });
        Zone z = new Zone(new Name("badname."));
        z.addRecord(malformed);
        Executable call = z::toString;
        assertThrows(IllegalArgumentException.class, call, "toString() should throw IllegalArgumentException for a Record with a null name");
    }

    /**
     * Missing mandatory SOA must cause IllegalStateException.
     */
    @Test
    void missingSOAThrowsIllegalStateException() {
        Zone z = new Zone(new Name("nosoa."));
        // Add only an NS record, no SOA.
        NSRecord ns = new NSRecord(z.getOrigin(), DClass.IN, 3600, Name.fromConstantString("ns.nosoa."));
        z.addRecord(ns);
        Executable call = z::toString;
        assertThrows(IllegalStateException.class, call, "toString() (via toMasterFile) should throw IllegalStateException when no SOA is present");
    }

    /**
     * A Record that throws IOException while being rendered must be wrapped in RuntimeException.
     */
    @Test
    void malformedRecordCausesRuntimeExceptionWrappingIOException() {
        // Create a custom Record subclass that throws IOException from rdataToString()
        class BadRecord extends Record {

            BadRecord(Name name) {
                super(name, Type.TXT, DClass.IN, 3600);
            }

            @Override
            public String rdataToString() throws IOException {
                throw new IOException("simulated I/O failure");
            }

            @Override
            public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
                // No‑op – not needed for this test
            }
        }
        Zone z = new Zone(new Name("badrecord."));
        BadRecord bad = new BadRecord(new Name("broken", z.getOrigin()));
        z.addRecord(bad);
        Executable call = z::toString;
        RuntimeException ex = assertThrows(RuntimeException.class, call, "toString() should wrap IOException in a RuntimeException");
        assertTrue(ex.getCause() instanceof IOException, "The cause of the RuntimeException must be the original IOException");
    }

    /**
     * Concurrent modification while building the master file may raise CME.
     */
    @Test
    void concurrentModificationDuringToStringMayThrowCME() throws Exception {
        Name origin = new Name("concurrent.");
        Zone shared = new Zone(origin);
        // Add a mandatory SOA so that toString() can start iterating.
        SOARecord soa = new SOARecord(origin, DClass.IN, 3600, Name.fromConstantString("ns.concurrent."), new Name("hostmaster", origin), 1, 7200, 3600, 1209600, 3600);
        shared.addRecord(soa);
        // Flag to stop the writer thread
        AtomicBoolean keepRunning = new AtomicBoolean(true);
        Thread writer = new Thread(() -> {
            int counter = 0;
            while (keepRunning.get()) {
                try {
                    ARecord a = new ARecord(new Name("host" + counter, origin), DClass.IN, 3600, InetAddress.getByName("192.0.2." + (counter % 254 + 1)));
                    shared.addRecord(a);
                    counter++;
                } catch (Exception ignored) {
                }
            }
        });
        writer.start();
        // Give the writer a moment to populate the zone
        Thread.sleep(100);
        // The toString() call may or may not see a concurrent modification.
        // We accept either a successful string or a CME.
        try {
            // may succeed
            shared.toString();
        } catch (ConcurrentModificationException e) {
            // Expected in the race condition scenario
        } finally {
            keepRunning.set(false);
            writer.join();
        }
    }
}
