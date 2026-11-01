```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Tests for {@link Zone#getDClass()}.
 *
 * The method is trivial – it always returns {@link DClass#IN}.  The test verifies that the
 * returned value matches the constant and that a {@link Zone} instance can be created
 * without side‑effects.
 */
class Zone_getDClass_Test {

    private static final String ZONE_STRING = "example.";
    private Name zoneName;
    private Zone zone;

    /**
     * Create a minimal, valid zone that contains the mandatory SOA and at least one NS record.
     * This mirrors the setup used in other {@code ZoneTest} cases.
     */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        zoneName = Name.fromConstantString(ZONE_STRING);

        // Minimal SOA record (values are not important for this test)
        SOARecord soa = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                1,          // serial
                21600L,     // refresh
                7200L,      // retry
                2160000L,   // expire
                3600L);     // minimum

        // Minimal NS record
        NSRecord ns = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns.example."));

        // Construct the zone with the two mandatory records
        zone = new Zone(zoneName, soa, ns);
    }

    @Test
    void getDClass_returnsIN() {
        int dclass = zone.getDClass();
        assertEquals(DClass.IN, dclass, "Zone#getDClass() should return DClass.IN");
    }

    // No special teardown required for this simple test
    @AfterEach
    void teardownAfterEach() {
        // placeholder for symmetry with the template
    }

    @BeforeAll
    static void setupBeforeAll() {
        // placeholder – nothing to initialise globally
    }

    @AfterAll
    static void teardownAfterAll() {
        // placeholder – nothing to clean up globally
    }
}
```