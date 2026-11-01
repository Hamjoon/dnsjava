```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Tests for {@link Zone#getOrigin()}.
 *
 * The tests verify that the origin supplied to the various {@link Zone}
 * constructors is returned unchanged by {@code getOrigin()}, and that the
 * constructor validates its arguments (i.e., it throws {@link IllegalArgumentException}
 * when a {@code null} zone name is supplied).
 */
@ExtendWith(MockitoExtension.class)
class Zone_getOrigin_Test {

    /** A reusable zone name used for the successful construction of a {@link Zone}. */
    private static Name zoneName;

    /** The {@link Zone} instance created once for the whole test suite. */
    private static Zone zone;

    /** Create a common {@link Name} and {@link Zone} that will be used by the tests. */
    @BeforeAll
    static void setupBeforeAll() {
        try {
            zoneName = new Name("example.");               // absolute name
            // an empty record array is sufficient – we are only interested in the origin
            zone = new Zone(zoneName, new Record[0]);
        } catch (IOException e) {
            // The constructors used in this test do not perform I/O, so any IOException is unexpected.
            throw new RuntimeException("Failed to create test Zone", e);
        }
    }

    /** No per‑test setup required at the moment. */
    @BeforeEach
    void setupBeforeEach() {
        // placeholder for future per‑test initialisation
    }

    /** No per‑test teardown required at the moment. */
    @AfterEach
    void teardownAfterEach() {
        // placeholder for future per‑test cleanup
    }

    /** No global teardown required at the moment. */
    @AfterAll
    static void teardownAfterAll() {
        // placeholder for future global cleanup
    }

    /** Verify that {@code getOrigin()} returns the exact {@link Name} supplied to the constructor. */
    @Test
    void getOriginReturnsSuppliedName() {
        assertEquals(zoneName, zone.getOrigin(),
                "Zone.getOrigin() should return the Name that was passed to the constructor");
    }

    /** Verify that the constructor rejects a {@code null} zone name. */
    @Test
    void constructorRejectsNullOrigin() {
        assertThrows(IllegalArgumentException.class,
                () -> new Zone((Name) null, new Record[0]),
                "Zone constructor must throw IllegalArgumentException when the zone name is null");
    }

    /** Verify that a different constructor (the one that takes a file) also preserves the origin. */
    @Test
    void getOriginPreservedWhenUsingFileConstructor() throws IOException {
        // Create a minimal zone file in‑memory using the temporary file API.
        // The content only needs a SOA record; the actual parsing is exercised by the constructor.
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("zone", ".txt");
        try {
            String zoneFileContent =
                    "$ORIGIN example.\n" +
                    "@ 3600 IN SOA ns.example. hostmaster.example. 1 7200 3600 1209600 3600\n";
            java.nio.file.Files.writeString(tempFile, zoneFileContent);

            Zone fileZone = new Zone(zoneName, tempFile.toString());
            assertEquals(zoneName, fileZone.getOrigin(),
                    "Zone created from a file should retain the supplied origin");
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }
}
```