```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;

class Zone_toMasterFile_Test {

    private Name zoneName;
    private SOARecord soa;
    private NSRecord ns1;
    private NSRecord ns2;
    private ARecord aRecord;
    private AAAARecord aaaaRecord;
    private ARecord wildA;
    private TXTRecord wildTXT;
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a deterministic zone – the same data that is used in the original
        // ZoneTest class, but limited to a few records that are easy to verify.
        zoneName = Name.fromConstantString("example.");

        soa = new SOARecord(
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

        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        aRecord = new ARecord(new Name("www", zoneName), DClass.IN, 3600, localhost4);
        aaaaRecord = new AAAARecord(new Name("www", zoneName), DClass.IN, 3600,
                InetAddress.getByName("::1"));
        wildA = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600,
                InetAddress.getByName("127.0.0.2"));
        wildTXT = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");

        Record[] records = new Record[] {
                soa, ns1, ns2, aRecord, aaaaRecord, wildA, wildTXT
        };
        zone = new Zone(zoneName, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this test suite
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    @Test
    void toMasterFile_containsAllRecordsInCorrectOrder() {
        String masterFile = zone.toMasterFile();

        // The master file should start with the SOA record (origin node)
        assertThat(masterFile).startsWith(soa.toString());

        // It must contain the two NS records
        assertThat(masterFile).contains(ns1.toString());
        assertThat(masterFile).contains(ns2.toString());

        // It must contain the A and AAAA records for www.example.
        assertThat(masterFile).contains(aRecord.toString());
        assertThat(masterFile).contains(aaaaRecord.toString());

        // It must contain the wildcard records
        assertThat(masterFile).contains(wildA.toString());
        assertThat(masterFile).contains(wildTXT.toString());

        // Ensure that the origin name appears only once (the SOA line)
        long occurrence = masterFile.lines()
                .filter(line -> line.contains(zoneName.toString()))
                .count();
        assertThat(occurrence).isEqualTo(1L);
    }

    @Test
    void toMasterFile_isIdempotent() {
        String first = zone.toMasterFile();
        String second = zone.toMasterFile();

        // The method should be pure – repeated calls must return identical strings
        assertThat(first).isEqualTo(second);
    }
}
```