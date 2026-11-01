```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

/**
 * Unit tests for {@link Zone#getDClass()}.
 *
 * The method is trivial – it always returns {@link DClass#IN}.  The test therefore
 * only needs to verify that the public API behaves as documented and does not
 * depend on any internal state of {@link Zone}.
 */
@ExtendWith(MockitoExtension.class)
class Zone_getDClass_Test {

    /** Zone instance used by the tests. */
    private Zone zone;

    /** A simple absolute name that can be used for constructing a {@link Zone}. */
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
}
```