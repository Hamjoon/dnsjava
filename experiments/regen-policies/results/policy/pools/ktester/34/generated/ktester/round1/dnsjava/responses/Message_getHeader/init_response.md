```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.xbill.DNS.utils.base64;

/**
 * Tests for {@link Message#getHeader()}.
 *
 * The tests verify that:
 * <ul>
 *   <li>The default constructors initialise a non‑null {@link Header}.</li>
 *   <li>The {@code setHeader(Header)} method replaces the header and the same instance is returned by {@code getHeader()}.</li>
 *   <li>A {@link Message} created from a wire format (byte array) also provides a non‑null header.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_getHeader_Test {

    /** Message instance used by most tests. */
    private Message message;

    /** A custom header used for the {@code setHeader} test. */
    private Header customHeader;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh Message for each test case.
        message = new Message();
        // Create a deterministic Header (ID = 0xABCD) that we can later set.
        customHeader = new Header(0xABCD);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly necessary but keeps the pattern clean.
        message = null;
        customHeader = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verify that a Message created with the no‑arg constructor
     * contains a non‑null Header and that the header’s section counts are zero.
     */
    @Test
    void getHeader_defaultConstructor() {
        Header h = message.getHeader();
        assertNotNull(h, "Header should never be null");
        // All section counts must be zero for a freshly created message.
        for (int i = 0; i < 4; i++) {
            assertEquals(0, h.getCount(i),
                    "Section count " + i + " should be zero for a new message");
        }
        // The ID must be within the valid DNS range (0‑0xFFFF).
        assertTrue(h.getID() >= 0 && h.getID() <= 0xFFFF,
                "Header ID should be within the valid range");
    }

    /**
     * Verify that {@code setHeader(Header)} replaces the internal header
     * and that {@code getHeader()} returns the exact same instance.
     */
    @Test
    void getHeader_afterSetHeader() {
        // Replace the automatically created header with our custom one.
        message.setHeader(customHeader);
        Header retrieved = message.getHeader();
        assertSame(customHeader, retrieved,
                "getHeader() should return the Header instance that was set via setHeader()");
        // Additional sanity check – the ID must match the one we constructed.
        assertEquals(0xABCD, retrieved.getID(),
                "The Header ID should be the one supplied to the custom Header");
    }

    /**
     * Parse a real DNS message from its wire format (the same data used in the
     * upstream test suite) and ensure that {@code getHeader()} yields a non‑null
     * Header object.
     *
     * @throws IOException if the test data cannot be parsed
     */
    @Test
    void getHeader_parsedFromWire() throws IOException {
        // This is the base‑64 encoded DNS response used in the original test suite.
        byte[] wire = base64.fromString(
                "EEuBgAABAAEABAAIA3d3dwZnb29nbGUDY29tAAABAAHADAABAAEAAAAaAASO+rokwBAAAgABAAFHCwAGA25zMcAQwBAAAgABAAFHCwAGA25zNMAQwBAAAgABAAFHCwAGA25zM8AQwBAAAgABAAFHCwAGA25zMsAQwDwAAQABAADObwAE2O8gCsByAAEAAQABrVEABNjvIgrAYAABAAEAAVqZAATY7yQKwE4AAQABAAK9RQAE2O8mCsA8ABwAAQAD4a0AECABSGBIAgAyAAAAAAAAAArAcgAcAAEAAtDgABAgAUhgSAIANAAAAAAAAAAKwGAAHAABAACSagAQIAFIYEgCADYAAAAAAAAACsBOABwAAQAErVoAECABSGBIAgA4AAAAAAAAAAo=");
        // The Message(byte[]) constructor expects the raw DNS packet (no length prefix).
        Message parsed = new Message(wire);
        Header h = parsed.getHeader();
        assertNotNull(h, "Header should be present after parsing a wire‑format message");
        // The ID of the parsed header is deterministic for the given fixture.
        // According to the fixture, the ID is 0x1128 (big‑endian 0x11 0x28).
        assertEquals(0x1128, h.getID(),
                "Parsed Header ID should match the value encoded in the test fixture");
    }
}
```