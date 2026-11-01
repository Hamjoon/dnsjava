```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Tests for {@link Zone#getNS()}.
 *
 * The tests verify that:
 * <ul>
 *   <li>When a zone contains an {@code NS} record, {@code getNS()} returns an {@link RRset}
 *       containing that record.</li>
 *   <li>When a zone does not contain any {@code NS} records, {@code getNS()} returns {@code null}.</li>
 * </ul>
 */
class Zone_getNS_Test {

    /** Common zone name used for all test zones. */
    private static Name ZONE_NAME;

    /** An NS record that will be added to the zone that contains NS records. */
    private static NSRecord NS_RECORD;

    /** A zone that contains an NS record. */
    private static Zone ZONE_WITH_NS;

    /** A zone that does not contain any NS records. */
    private static Zone ZONE_WITHOUT_NS;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create the zone name (example.)
        ZONE_NAME = new Name("example.");

        // Create an NS record for the zone (example. NS ns1.example.)
        NS_RECORD = new NSRecord(
                ZONE_NAME,                // name
                DClass.IN,                // class
                300,                      // TTL
                Name.fromConstantString("ns1.example.") // target name server
        );

        // A minimal SOA record required for a valid zone.
        SOARecord soa = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("nameserver."),          // primary name server
                new Name("hostmaster", ZONE_NAME),               // hostmaster email
                1,                                               // serial
                21600L,                                          // refresh
                7200L,                                           // retry
                2160000L,                                        // expire
                3600L                                            // minimum TTL
        );

        // Zone that contains an NS record (plus the mandatory SOA).
        ZONE_WITH_NS = new Zone(ZONE_NAME, new Record[]{soa, NS_RECORD});

        // Zone that contains only the SOA record – no NS records at all.
        ZONE_WITHOUT_NS = new Zone(ZONE_NAME, new Record[]{soa});
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getNS_returnsRRsetWhenNSRecordPresent() {
        RRset ns = ZONE_WITH_NS.getNS();
        assertNotNull(ns, "Expected non‑null RRset when NS record is present");
        // Build the expected RRset containing exactly the NS_RECORD.
        RRset expected = new RRset(NS_RECORD);
        assertEquals(expected, ns, "RRset returned by getNS() does not match expected NS record set");
    }

    @Test
    void getNS_returnsNullWhenNoNSRecord() {
        RRset ns = ZONE_WITHOUT_NS.getNS();
        assertNull(ns, "Expected null RRset when zone contains no NS records");
    }
}
```