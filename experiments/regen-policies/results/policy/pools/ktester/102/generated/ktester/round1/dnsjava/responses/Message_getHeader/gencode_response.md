```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#getHeader()} covering the behaviour described
 * in the supplied JSON test cases.
 *
 * <p>These tests do not rely on any external resources. They only verify the
 * contract of {@code getHeader()} and its interaction with {@code setHeader()}
 * and the mutability of the returned {@link Header} instance.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_getHeader_Test {

    /* --------------------------------------------------------------------- */
    /*  No‑arg constructor – should start with a <code>null</code> header.   */
    /* --------------------------------------------------------------------- */

    /**
     * {@code Message()} creates a message with no Header assigned.
     * {@link Message#getHeader()} is therefore expected to return {@code null}.
     */
    @Test
    void GetHeader_NoHeaderSet_returnsNull() {
        Message msg = new Message();                // default ctor
        Header hdr = msg.getHeader();

        assertNull(hdr, "A newly created Message should have a null Header");
    }

    /* --------------------------------------------------------------------- */
    /*  setHeader(Header) – basic positive path                               */
    /* --------------------------------------------------------------------- */

    /**
     * After a non‑null {@link Header} is set via {@code setHeader},
     * {@code getHeader()} must return exactly that instance.
     */
    @Test
    void GetHeader_AfterSetHeader_returnsProvidedHeader() {
        Message msg = new Message();                // starts with null Header
        Header newHeader = new Header(0x1A2B);      // arbitrary ID

        msg.setHeader(newHeader);
        Header obtained = msg.getHeader();

        assertNotNull(obtained, "Header should be non‑null after setHeader");
        assertSame(newHeader, obtained,
                "getHeader should return the same Header instance that was set");
    }

    /* --------------------------------------------------------------------- */
    /*  setHeader(Header) – ensure reference identity is preserved            */
    /* --------------------------------------------------------------------- */

    /**
     * The Header returned by {@code getHeader()} must be the exact same object
     * supplied to {@code setHeader()}, confirming no defensive copy is made.
     */
    @Test
    void GetHeader_ReturnsSameInstance() {
        Message msg = new Message();
        Header hdr = new Header(0xDEAD);
        msg.setHeader(hdr);

        // First retrieval
        Header first = msg.getHeader();
        // Second retrieval – should be the very same object
        Header second = msg.getHeader();

        assertSame(first, second,
                "Multiple calls to getHeader should return the same Header reference");
        assertSame(hdr, first,
                "The Header instance returned must be the one that was set");
    }

    /* --------------------------------------------------------------------- */
    /*  setHeader(null) – clearing the header field                           */
    /* --------------------------------------------------------------------- */

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
        assertNull(msg.getHeader(),
                "After setHeader(null) the Header should be cleared (null)");
    }

    /* --------------------------------------------------------------------- */
    /*  Mutability check – modifications on the returned Header are live     */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that the Header instance returned by {@code getHeader()} is live.
     * Changing a mutable property (e.g., the ID) on the retrieved Header must be
     * reflected by subsequent {@code getHeader()} calls.
     */
    @Test
    void GetHeader_MutabilityCheck_reflectsChanges() {
        Message msg = new Message();
        Header hdr = new Header(0x0000); // start with ID 0
        msg.setHeader(hdr);

        // Obtain the Header and mutate it
        Header retrieved = msg.getHeader();
        assertNotNull(retrieved, "Header should not be null before mutation");
        retrieved.setID(0xBEEF); // mutate ID

        // Retrieve again and verify the change persisted
        Header afterMutation = msg.getHeader();
        assertEquals(0xBEEF, afterMutation.getID(),
                "Mutating the Header should be visible through subsequent getHeader calls");
    }
}
```