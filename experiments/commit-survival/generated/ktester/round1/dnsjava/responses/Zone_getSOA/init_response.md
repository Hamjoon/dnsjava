```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Tests for {@link Zone#getSOA()}.
 *
 * The test creates a {@link Zone} instance using the constructor that accepts a {@link Name}
 * and an array of {@link Record}s. The array contains a single {@link SOARecord} which is the
 * expected value returned by {@code getSOA()}.
 *
 * No private members of {@link Zone} or its dependencies are accessed.
 */
@ExtendWith(MockitoExtension.class)
class Zone_getSOA_Test {

    /** Zone instance used for all tests. */
    private static Zone zone;

    /** The SOARecord that we expect {@link Zone#getSOA()} to return. */
    private static SOARecord expectedSOA;

    @BeforeAll
    static void setupBeforeAll() throws IOException, TextParseException {
        // Create the zone name (example.)
        Name zoneName = new Name("example.");

        // Build the SOA record that will be stored inside the Zone.
        // Parameters: name, dclass, ttl, primary, admin, serial, refresh, retry, expire, minimum
        expectedSOA = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),          // primary name server
                new Name("hostmaster", zoneName),                // responsible person
                1,                                               // serial
                21600L,                                          // refresh
                7200L,                                           // retry
                2160000L,                                        // expire
                3600L);                                          // minimum

        // Initialise the Zone with only the SOA record.
        zone = new Zone(zoneName, new Record[]{expectedSOA});
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test teardown required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void getSOA_returnsTheSOARecordProvidedAtConstruction() {
        SOARecord actual = zone.getSOA();
        assertNotNull(actual, "Zone#getSOA() should never return null for a valid zone");
        assertEquals(expectedSOA, actual, "Zone#getSOA() must return the SOA record supplied to the constructor");
    }

    @Test
    void getSOA_isConsistentAcrossMultipleCalls() {
        SOARecord first = zone.getSOA();
        SOARecord second = zone.getSOA();
        assertSame(first, second, "Repeated calls to Zone#getSOA() should return the same instance");
    }
}
```