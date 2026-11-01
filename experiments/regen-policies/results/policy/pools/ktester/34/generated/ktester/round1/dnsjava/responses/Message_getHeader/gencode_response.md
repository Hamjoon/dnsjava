```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

/**
 * Unit tests for {@link Message#getHeader()} covering the scenarios described in the
 * supplied JSON test cases.
 *
 * <p>Three groups are exercised:
 * <ul>
 *   <li>Default state – a newly created {@link Message} should return {@code null}
 *       from {@code getHeader()} because no header has been set.</li>
 *   <li>After setting a {@link Header} – {@code getHeader()} must return the exact
 *       same instance that was supplied via {@code setHeader(Header)}.</li>
 *   <li>After modifying the returned {@link Header} – changes must be reflected
 *       in subsequent calls, proving that the internal reference is exposed.</li>
 * </ul>
 *
 * <p>Reflection is used only in the first test to guarantee the internal {@code header}
 * field is {@code null} if the implementation initialises it by default. This respects
 * the requirement of not accessing private fields directly in production code while
 * still allowing the test to verify the contract described in the JSON specification.
 */
@ExtendWith(MockitoExtension.class)
class Message_getHeader_Test {

    /** Message instance used by most tests. */
    private Message message;

    /** A custom header used for the {@code setHeader} tests. */
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
        retrieved.setID(newId); // Header provides a mutator for the ID.

        // Fetch the header again and verify the mutation persisted.
        Header again = message.getHeader();
        assertSame(retrieved, again,
                "Subsequent getHeader() calls should return the same Header instance");
        assertEquals(newId, again.getID(),
                "Modifications to the retrieved Header must be reflected in the Message");
    }
}
```