package org.xbill.DNS;

import org.junit.jupiter.api.AfterEach;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_setHeader_Test {

    /**
     * Message instance used in each test.
     */
    private Message message;

    /**
     * Two distinct Header instances used for replacement checks.
     */
    private Header headerA;

    private Header headerB;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message and two Header objects before every test.
        // The default constructor creates an empty Header.
        message = new Message();
        // arbitrary non‑zero identifiers
        headerA = new Header(0x1234);
        headerB = new Header(0xABCD);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC (optional).
        message = null;
        headerA = null;
        headerB = null;
    }

    /**
     * Verify that {@code setHeader} stores the supplied header and that {@code getHeader} returns it.
     */
    @Test
    void setHeader_assignsHeader() {
        // given
        assertNotNull(message.getHeader(), "Message should have a non‑null default header before we replace it");
        // when
        message.setHeader(headerA);
        // then
        assertSame(headerA, message.getHeader(), "The header returned by getHeader() must be the same instance passed to setHeader()");
        // The header's ID should match the one we created.
        assertEquals(0x1234, message.getHeader().getID(), "Header ID should reflect the value supplied in the constructor");
    }

    /**
     * Verify that a second call to {@code setHeader} replaces the previous header.
     */
    @Test
    void setHeader_replacesExistingHeader() {
        // given
        message.setHeader(headerA);
        assertSame(headerA, message.getHeader(), "Pre‑condition: headerA must be set first");
        // when
        message.setHeader(headerB);
        // then
        assertSame(headerB, message.getHeader(), "After a second setHeader call the header should be replaced with headerB");
        assertNotSame(headerA, message.getHeader(), "The original headerA instance should no longer be referenced");
    }

    /**
     * Verify that assigning {@code null} does not throw and results in a null header.
     */
    @Test
    void setHeader_allowsNullHeader() {
        // when
        message.setHeader(null);
        // then
        assertNull(message.getHeader(), "After assigning null the internal header reference should be null");
    }

    /**
     * Verify that changing the header does not affect the message sections.
     */
    @Test
    void setHeader_doesNotAlterSections() {
        // given
        // All sections should be empty on a newly created Message.
        for (int i = 0; i < 4; i++) {
            assertTrue(message.getSection(i).isEmpty(), "Section " + i + " must be empty initially");
        }
        // when
        message.setHeader(headerA);
        // replace header
        message.setHeader(headerB);
        // then
        for (int i = 0; i < 4; i++) {
            assertTrue(message.getSection(i).isEmpty(), "Section " + i + " should remain empty after header changes");
        }
    }
}
