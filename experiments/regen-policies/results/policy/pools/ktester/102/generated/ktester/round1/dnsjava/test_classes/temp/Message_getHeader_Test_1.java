package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Message_getHeader_Test {

    /**
     * Verify that the no‑arg constructor creates a non‑null Header and that the
     * Header’s ID is within the valid unsigned‑16‑bit range.
     */
    @Test
    void getHeader_noArgConstructor_returnsValidHeader() {
        // uses Header()
        Message msg = new Message();
        Header hdr = msg.getHeader();
        assertNotNull(hdr, "Header should not be null");
        int id = hdr.getID();
        assertTrue(id >= 0 && id <= 0xFFFF, "Header ID should be within unsigned 16‑bit range, but was " + id);
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
        assertEquals(expectedId, hdr.getID(), "Header ID should match the value supplied to the constructor");
    }

    /**
     * Verify that {@code setHeader(Header)} replaces the existing Header
     * instance and that {@code getHeader()} returns the newly set instance.
     */
    @Test
    void setHeader_replacesExistingHeader() {
        // starts with a random Header
        Message msg = new Message();
        Header original = msg.getHeader();
        Header replacement = new Header(0xABCD);
        msg.setHeader(replacement);
        Header after = msg.getHeader();
        assertSame(replacement, after, "After calling setHeader, getHeader should return the exact Header instance that was set");
        assertNotSame(original, after, "The Header instance should have been replaced, not mutated");
        assertEquals(0xABCD, after.getID(), "The new Header should retain the ID it was constructed with");
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
        byte[] wire = { // ID
        0x1a, // ID
        0x2b, // flags
        0x01, // flags
        0x00, // QDCOUNT
        0x00, // QDCOUNT
        0x00, // ANCOUNT
        0x00, // ANCOUNT
        0x00, // NSCOUNT
        0x00, // NSCOUNT
        0x00, // ARCOUNT
        0x00, // ARCOUNT
        0x00 };
        Message msg = new Message(wire);
        Header hdr = msg.getHeader();
        assertNotNull(hdr, "Header should be parsed and not null");
        assertEquals(0x1a2b, hdr.getID(), "Header ID parsed from wire data should be 0x1a2b");
    }

    /**
     * {@code Message()} creates a message with no Header assigned.
     * {@link Message#getHeader()} is therefore expected to return {@code null}.
     */
    @Test
    void GetHeader_NoHeaderSet_returnsNull() {
        // default ctor
        Message msg = new Message();
        Header hdr = msg.getHeader();
        assertNull(hdr, "A newly created Message should have a null Header");
    }

    /**
     * After a non‑null {@link Header} is set via {@code setHeader},
     * {@code getHeader()} must return exactly that instance.
     */
    @Test
    void GetHeader_AfterSetHeader_returnsProvidedHeader() {
        Message msg = new Message();
        Header newHeader = new Header(0x1A2B);
        msg.setHeader(newHeader);
        Header obtained = msg.getHeader();
        assertNotNull(obtained, "Header should be non‑null after setHeader");
        assertSame(newHeader, obtained, "getHeader should return the same Header instance that was set");
    }

    /**
     * The Header returned by {@code getHeader()} must be the exact same object
     * supplied to {@code setHeader()}, confirming no defensive copy is made.
     */
    @Test
    void GetHeader_ReturnsSameInstance() {
        Message msg = new Message();
        Header hdr = new Header(0xDEAD);
        msg.setHeader(hdr);
        Header first = msg.getHeader();
        Header second = msg.getHeader();
        assertSame(first, second, "Multiple calls to getHeader should return the same Header reference");
        assertSame(hdr, first, "The Header instance returned must be the one that was set");
    }

    /**
     * Passing {@code null} to {@code setHeader} must clear the internal
     * Header reference so that subsequent {@code getHeader()} calls return
     * {@code null}.
     */
    @Test
    void GetHeader_SetNullHeader_resultsInNull() {
        Message msg = new Message();
        // Ensure we have a non‑null Header first
        msg.setHeader(new Header(0x1234));
        assertNotNull(msg.getHeader(), "Pre‑condition: Header should be non‑null");
        // Now clear it
        msg.setHeader(null);
        assertNull(msg.getHeader(), "After setHeader(null) the Header should be cleared (null)");
    }

    /**
     * Verify that the Header instance returned by {@code getHeader()} is live.
     * Changing a mutable property (e.g., the ID) on the retrieved Header must be
     * reflected by subsequent {@code getHeader()} calls.
     */
    @Test
    void GetHeader_MutabilityCheck_reflectsChanges() {
        Message msg = new Message();
        Header hdr = new Header(0x0000);
        msg.setHeader(hdr);
        Header retrieved = msg.getHeader();
        assertNotNull(retrieved, "Header should not be null before mutation");
        // mutate ID
        retrieved.setID(0xBEEF);
        // Retrieve again and verify the change persisted
        Header afterMutation = msg.getHeader();
        assertEquals(0xBEEF, afterMutation.getID(), "Mutating the Header should be visible through subsequent getHeader calls");
    }

    /**
     * The default constructor creates a Message with a non‑null Header.
     */
    @Test
    void GetHeader_DefaultConstructor_returnsNonNullHeader() {
        Message msg = new Message();
        Header hdr = msg.getHeader();
        assertNotNull(hdr, "A newly created Message should have a non‑null Header");
    }
}
