```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class Message_getHeader_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verify that the no‑arg constructor creates a non‑null Header and that the
     * Header’s ID is within the valid unsigned‑16‑bit range.
     */
    @Test
    void getHeader_noArgConstructor_returnsValidHeader() {
        Message msg = new Message();               // uses Header()
        Header hdr = msg.getHeader();

        assertNotNull(hdr, "Header should not be null");
        // Header IDs are unsigned 16‑bit values (0 .. 0xFFFF)
        int id = hdr.getID();
        assertTrue(id >= 0 && id <= 0xFFFF,
                "Header ID should be within unsigned 16‑bit range, but was " + id);
    }

    /**
     * Verify that the constructor taking an explicit ID creates a Header whose
     * ID matches the supplied value.
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
     * Verify that {@code setHeader(Header)} replaces the existing Header
     * instance and that {@code getHeader()} returns the newly set instance.
     */
    @Test
    void setHeader_replacesExistingHeader() {
        Message msg = new Message();               // starts with a random Header
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
     * Verify that a Message created from a {@link DNSInput} (i.e. from wire
     * data) correctly exposes the Header that was parsed from the input.
     *
     * The wire data is a minimal DNS query with ID 0x1a2b.  The test builds the
     * byte array manually to avoid any external resources.
     */
    @Test
    void getHeader_fromWireData_matchesParsedId() throws Exception {
        // Header fields (ID, flags, QDCOUNT, ANCOUNT, NSCOUNT, ARCOUNT)
        // ID = 0x1a2b, flags = 0x0100 (standard query), all counts = 0
        byte[] wire = {
                0x1a, 0x2b, // ID
                0x01, 0x00, // flags
                0x00, 0x00, // QDCOUNT
                0x00, 0x00, // ANCOUNT
                0x00, 0x00, // NSCOUNT
                0x00, 0x00  // ARCOUNT
        };
        Message msg = new Message(wire);
        Header hdr = msg.getHeader();

        assertNotNull(hdr, "Header should be parsed and not null");
        assertEquals(0x1a2b, hdr.getID(),
                "Header ID parsed from wire data should be 0x1a2b");
    }
}
```