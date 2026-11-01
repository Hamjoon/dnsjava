```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Message#getHeader()} and related header handling.
 *
 * The tests focus on the expected behaviour of the {@code Message} class as
 * defined by dnsjava:
 * <ul>
 *   <li>The no‑arg constructor creates a {@link Header} with a random ID.</li>
 *   <li>The {@code Message(int id)} constructor creates a {@link Header}
 *       whose ID matches the supplied value.</li>
 *   <li>{@code setHeader(Header)} replaces the current header with the given
 *       instance.</li>
 *   <li>A {@code Message} created from wire data parses the header correctly.</li>
 * </ul>
 */
class Message_getHeader_Test {

    /**
     * Verify that the no‑arg constructor creates a non‑null {@link Header}
     * and that the header ID is within the unsigned‑16‑bit range.
     */
    @Test
    void getHeader_noArgConstructor_returnsValidHeader() {
        Message msg = new Message();               // uses Header()
        Header hdr = msg.getHeader();
        assertNotNull(hdr, "Header should not be null");
        int id = hdr.getID();
        assertTrue(id >= 0 && id <= 0xFFFF,
                "Header ID should be within unsigned 16‑bit range, but was " + id);
    }

    /**
     * Verify that the constructor taking an explicit ID creates a {@link Header}
     * whose ID matches the supplied value.
     */
    @Test
    void getHeader_intArgConstructor_matchesSuppliedId() {
        int expectedId = 0x1234;
        Message msg = new Message(expectedId);
        Header hdr = msg.getHeader();
        assertNotNull(hdr, "Header should not be null");
        assertEquals(expectedId, hdr.getID(),
                "Header ID should match the value supplied to the constructor");
    }

    /**
     * Verify that {@code setHeader(Header)} replaces the existing header and
     * that {@code getHeader()} returns the newly set instance.
     */
    @Test
    void setHeader_replacesExistingHeader() {
        Message msg = new Message();                 // starts with a random header
        Header original = msg.getHeader();

        Header replacement = new Header(0xABCD);
        msg.setHeader(replacement);

        Header after = msg.getHeader();
        assertSame(replacement, after,
                "After calling setHeader, getHeader should return the exact Header instance that was set");
        assertNotSame(original, after,
                "The Header instance should have been replaced, not mutated");
        assertEquals(0xABCD, after.getID(),
                "The new Header should retain the ID it was constructed with");
    }

    /**
     * Verify that a {@link Message} created from wire data correctly parses the
     * header ID.
     *
     * The wire data represents a minimal DNS query with ID 0x1a2b.
     */
    @Test
    void getHeader_fromWireData_matchesParsedId() throws Exception {
        // Minimal DNS query:
        //   Header (12 bytes) + empty question section (root label, QTYPE A, QCLASS IN)
        byte[] wire = {
                // Header (12 bytes)
                0x1a, 0x2b, // ID = 0x1a2b
                0x01, 0x00, // flags (standard query)
                0x00, 0x01, // QDCOUNT = 1
                0x00, 0x00, // ANCOUNT = 0
                0x00, 0x00, // NSCOUNT = 0
                0x00, 0x00, // ARCOUNT = 0
                // Question section (root name, type A, class IN)
                0x00,       // QNAME = root (zero‑length label)
                0x00, 0x01, // QTYPE = A
                0x00, 0x01  // QCLASS = IN
        };

        Message msg = new Message(wire);
        Header hdr = msg.getHeader();

        assertNotNull(hdr, "Header should be parsed and not null");
        assertEquals(0x1a2b, hdr.getID(),
                "Header ID parsed from wire data should be 0x1a2b");
    }
}
```