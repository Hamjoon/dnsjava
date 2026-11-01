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
 * <p>The tests are based on the examples from the original {@code ZoneTest} class and
 * cover exact‑name look‑ups, wildcard handling and the {@code ANY} query type.</p>
 */
@ExtendWith(MockitoExtension.class)
class Zone_findRecords_Test {

    /** Zone used by all tests – created once for the whole test run. */
    private static Zone ZONE;

    /** Records that populate the zone. */
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

            // records used in the tests
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
    /* Actual test cases                                                      */
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
}
```