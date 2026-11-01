```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Zone#getDClass()}.
 *
 * The method is trivial – it always returns {@link DClass#IN}.  The tests therefore
 * focus on exercising the method on a properly constructed {@link Zone} instance
 * and verifying that the returned value is the expected constant.
 *
 * No private members or implementation details are accessed.
 */
@ExtendWith(MockitoExtension.class)
class Zone_getDClass_Test {

    private static final String ZONE_TEXT = "example.";
    private Name zoneName;
    private SOARecord soa;
    private NSRecord ns;
    private Zone zone;

    /** Create a minimal, valid zone before each test. */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build the zone name (absolute)
        zoneName = Name.fromConstantString(ZONE_TEXT);

        // Minimal required records: one SOA and at least one NS.
        // The values are arbitrary but must be syntactically correct.
        soa = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostmaster", zoneName),
                1,
                7200L,
                3600L,
                1209600L,
                3600L);

        ns = new NSRecord(
                zoneName,
                DClass.IN,
                300L,
                Name.fromConstantString("ns.example."));

        // Construct the zone using the var‑args constructor.
        zone = new Zone(zoneName, soa, ns);
    }

    /** Verify that {@link Zone#getDClass()} always returns {@link DClass#IN}. */
    @Test
    void getDClassReturnsIN() {
        int dclass = zone.getDClass();
        assertEquals(DClass.IN, dclass,
                "Zone#getDClass() should return DClass.IN");
    }

    /** Calling the method repeatedly must keep returning the same constant. */
    @Test
    void getDClassIsIdempotent() {
        assertAll(
                () -> assertEquals(DClass.IN, zone.getDClass()),
                () -> assertEquals(DClass.IN, zone.getDClass()),
                () -> assertEquals(DClass.IN, zone.getDClass())
        );
    }

    /** Ensure that the test fixture itself does not leak resources. */
    @AfterEach
    void teardownAfterEach() {
        // No explicit resources to release; method kept for symmetry with the template.
        zone = null;
        soa = null;
        ns = null;
        zoneName = null;
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global fixtures required for this test suite.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }
}
```