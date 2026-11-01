```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Tests for {@link Zone#toString()}.
 *
 * <p>The {@code toString()} implementation simply delegates to {@link Zone#toMasterFile()},
 * therefore the test verifies that the two methods produce identical output and that the
 * representation is well‑formed for a typical zone containing an SOA, NS and a few records.
 */
@ExtendWith(MockitoExtension.class)
class Zone_toString_Test {

    private static Name ZONE_NAME;
    private static SOARecord SOA;
    private static NSRecord NS1;
    private static NSRecord NS2;
    private static ARecord A_UNIQUE;
    private static ARecord A_TEST;
    private static AAAARecord AAAA_1_TEST;
    private static AAAARecord AAAA_2_TEST;
    private static ARecord A_WILD;
    private static TXTRecord TXT_WILD;

    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // zone name used by all tests
        ZONE_NAME = Name.fromConstantString("example.");
        // common records – they are immutable, so we can create them once
        SOA = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("nameserver."),
                new Name("hostadmin", ZONE_NAME),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);

        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");

        A_UNIQUE = new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        A_TEST   = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
        A_WILD = new ARecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a fresh Zone for each test – this mirrors the construction used in the original
        // ZoneTest class.
        Record[] records = new Record[] {
                SOA, NS1, NS2,
                A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST,
                A_WILD, TXT_WILD
        };
        zone = new Zone(ZONE_NAME, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up.
        zone = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release at the class level.
    }

    @Test
    void toStringDelegatesToToMasterFile() {
        // The contract of Zone#toString is to return the same string as toMasterFile().
        String viaToString = zone.toString();
        String viaToMasterFile = zone.toMasterFile();

        assertNotNull(viaToString, "Zone#toString() must not return null");
        assertEquals(viaToMasterFile, viaToString,
                "Zone#toString() should produce exactly the same output as toMasterFile()");
    }

    @Test
    void toStringReflectsChangesAfterAddingRecord() throws IOException {
        // Add an additional A record and ensure the string representation changes accordingly.
        ARecord extra = new ARecord(new Name("extra", ZONE_NAME), DClass.IN, 3600,
                InetAddress.getByName("127.0.0.99"));
        zone.addRecord(extra);

        String masterFile = zone.toMasterFile();
        String stringForm = zone.toString();

        assertEquals(masterFile, stringForm,
                "After mutation, toString must still mirror toMasterFile");

        // The newly added record should be present in the textual representation.
        assertTrue(masterFile.contains("extra"), "Master file should contain the newly added name");
    }
}
```