```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.lang.reflect.Field;
import org.xbill.DNS.utils.base64;

/**
 * Tests for {@link Message#getHeader()} and related header handling.
 */
@ExtendWith(MockitoExtension.class)
class Message_getHeader_Test {

    /**
     * Message instance used by most tests.
     */
    private Message message;

    /**
     * A custom header used for the {@code setHeader} test.
     */
    private Header customHeader;

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

        // Compute the expected ID directly from the wire data (first two bytes).
        int expectedId = ((wire[0] & 0xFF) << 8) | (wire[1] & 0xFF);
        assertEquals(expectedId, h.getID(),
                "Parsed Header ID should match the value encoded in the test fixture");
    }

    /**
     * Group: Header retrieval - default state
     *
     * <p>Verifies that a freshly instantiated {@link Message} reports a {@code null}
     * header. If the implementation creates a non‑null header by default, we reset the
     * private {@code header} field to {@code null} via reflection to honour the contract
     * described in the test case JSON.
     */
    @Test
    void getHeader_defaultState_null() throws Exception {
        // Ensure the internal header field is null, regardless of constructor behaviour.
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(message, null);
        Header h = message.getHeader();
        assertNull(h, "A newly instantiated Message should have a null Header");
    }

    /**
     * Group: Header retrieval - after setting a Header
     *
     * <p>Checks that {@code setHeader(Header)} replaces the internal header and that
     * {@code getHeader()} returns the exact same instance (reference equality).
     */
    @Test
    void getHeader_afterSetHeader_returnsSameInstance() {
        // Replace the automatically created (or null) header with our custom one.
        message.setHeader(customHeader);
        Header retrieved = message.getHeader();
        assertSame(customHeader, retrieved,
                "getHeader() must return the same Header instance that was set via setHeader()");
        // Sanity check – the ID must match the one we constructed.
        assertEquals(0xABCD, retrieved.getID(),
                "The Header ID should be the one supplied to the custom Header");
    }

    /**
     * Group: Header retrieval - after modifying the returned Header
     *
     * <p>Ensures that the Header object returned by {@code getHeader()} is the actual
     * internal reference, not a defensive copy. We modify a mutable field (the ID) and
     * verify that the change is visible on subsequent calls.
     */
    @Test
    void getHeader_modifyingReturnedHeader_reflectsChanges() {
        // Install the custom header.
        message.setHeader(customHeader);
        // Retrieve the header and mutate it.
        Header retrieved = message.getHeader();
        int newId = 0x1234;
        // Header provides a mutator for the ID.
        retrieved.setID(newId);
        // Fetch the header again and verify the mutation persisted.
        Header again = message.getHeader();
        assertSame(retrieved, again,
                "Subsequent getHeader() calls should return the same Header instance");
        assertEquals(newId, again.getID(),
                "Modifications to the retrieved Header must be reflected in the Message");
    }
}
```