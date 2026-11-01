package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Zone_getDClass_Test {

    /**
     * Zone instance used by the tests.
     */
    private Zone zone;

    /**
     * A simple absolute name that can be used for constructing a {@link Zone}.
     */
    private static Name zoneName;

    @BeforeAll
    static void setupBeforeAll() {
        try {
            // “example.” is a fully‑qualified name (absolute) and is safe for tests.
            zoneName = new Name("example.");
        } catch (Exception e) {
            // Any failure here would make the whole test suite invalid.
            throw new RuntimeException("Failed to create test zone name", e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        try {
            // Construct a Zone with no records – the constructor validates the name
            // and does not perform any I/O when an empty record array is supplied.
            zone = new Zone(zoneName, new Record[0]);
        } catch (IOException e) {
            // Propagate as unchecked so the test framework can report the failure.
            throw new RuntimeException("Failed to create Zone instance", e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable resources to clean up for this simple test.
        zone = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release at the class level.
        zoneName = null;
    }

    @Test
    void getDClass_returnsIN() {
        // GIVEN – a Zone instance created in @BeforeEach
        // WHEN
        int dclass = zone.getDClass();
        // THEN
        assertEquals(DClass.IN, dclass, "Zone#getDClass() should return DClass.IN");
    }

    /**
     * Basic contract: a newly created Zone must return DClass.IN.
     */
    @Test
    void getDClass_returnsIN_forNewZone() {
        // GIVEN – a freshly constructed Zone (handled by @BeforeEach)
        // WHEN
        int dclass = zone.getDClass();
        // THEN
        assertEquals(DClass.IN, dclass, "Zone#getDClass() should return DClass.IN for a newly created zone");
    }

    /**
     * Repeated calls must always yield the same constant.
     */
    @Test
    void getDClass_isIdempotent_onSameInstance() {
        // GIVEN – a Zone instance (already created)
        // WHEN
        int first = zone.getDClass();
        int second = zone.getDClass();
        int third = zone.getDClass();
        // THEN
        assertAll(() -> assertEquals(DClass.IN, first, "First call should return DClass.IN"), () -> assertEquals(DClass.IN, second, "Second call should return DClass.IN"), () -> assertEquals(DClass.IN, third, "Third call should return DClass.IN"));
    }

    /**
     * Mutating the zone (adding/removing records) must not change the value
     * returned by {@link Zone#getDClass()}.
     */
    @Test
    void getDClass_unchanged_afterMutatingZone() throws UnknownHostException {
        // GIVEN – a Zone instance and a simple ARecord to add/remove
        Name hostName = Name.fromString("host.example.");
        InetAddress addr = InetAddress.getByName("127.0.0.1");
        ARecord aRecord = new ARecord(hostName, DClass.IN, 3600, addr);
        // WHEN – modify the zone
        // add a record
        zone.addRecord(aRecord);
        // then remove it again
        zone.removeRecord(aRecord);
        // THEN – getDClass must still be DClass.IN
        int dclassAfterMutation = zone.getDClass();
        assertEquals(DClass.IN, dclassAfterMutation, "Zone#getDClass() must remain DClass.IN after add/remove operations");
    }
}
