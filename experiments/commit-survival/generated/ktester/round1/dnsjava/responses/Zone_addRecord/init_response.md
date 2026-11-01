```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#addRecord(Record)}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>adding a record for a name/type that does not yet exist creates a new {@link RRset}
 *   <li>adding a record for a name/type that already exists appends the record to the existing
 *       {@link RRset}
 * </ul>
 *
 * No private members are accessed and no external resources are required.
 */
@ExtendWith(MockitoExtension.class)
class Zone_addRecord_Test {

    /** Common zone origin used for all tests. */
    private static Name origin;

    /** A sample IPv4 address used for {@link ARecord}s. */
    private static InetAddress localhost4;

    /** First A record used in the “existing RRset” test. */
    private static ARecord aRecord1;

    /** Second A record (same name/type) used in the “existing RRset” test. */
    private static ARecord aRecord2;

    /** Fresh zone instance for each test. */
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        origin = new Name("example.");
        localhost4 = InetAddress.getByName("127.0.0.1");

        // Records share the same name and type (A) but have different TTLs to make them distinct.
        aRecord1 = new ARecord(new Name("test", origin), DClass.IN, 3600, localhost4);
        aRecord2 = new ARecord(new Name("test", origin), DClass.IN, 7200, localhost4);
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create an empty zone (no initial records) for each test.
        zone = new Zone(origin, new Record[0]);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release; method present for symmetry with the template.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static resources to clean up.
    }

    /** Adding a record when no RRset exists should create a new RRset containing that record. */
    @Test
    void addRecordCreatesNewRRset() throws IOException {
        // Act
        zone.addRecord(aRecord1);

        // Verify
        SetResponse response = zone.findRecords(aRecord1.getName(), Type.A);
        assertTrue(response.isSuccessful(), "Response should be successful");
        assertEquals(oneRRset(aRecord1), response.answers(),
                "A new RRset containing the added record should be returned");
    }

    /**
     * Adding a record for a name/type that already exists should add the record to the existing
     * RRset.
     */
    @Test
    void addRecordAppendsToExistingRRset() throws IOException {
        // Arrange: first add aRecord1 so that an RRset exists
        zone.addRecord(aRecord1);

        // Act: add a second record with the same name and type
        zone.addRecord(aRecord2);

        // Verify: the RRset now contains both records
        SetResponse response = zone.findRecords(aRecord1.getName(), Type.A);
        assertTrue(response.isSuccessful(), "Response should be successful");
        assertEquals(oneRRset(aRecord1, aRecord2), response.answers(),
                "RRset should contain both added records");
    }

    /** Helper that mirrors the one used in the original ZoneTest for convenience. */
    private static java.util.List<RRset> oneRRset(Record... records) {
        return java.util.Collections.singletonList(new RRset(records));
    }
}
```