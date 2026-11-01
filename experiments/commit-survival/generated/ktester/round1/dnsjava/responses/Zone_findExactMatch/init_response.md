```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;

class Zone_findExactMatch_Test {

    private static Zone zone;
    private static Name nameZone;
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
}
```