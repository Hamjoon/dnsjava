```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#setHeader(Header)}.
 *
 * <p>The tests verify that:
 * <ul>
 *   <li>a non‑null {@link Header} can be stored and retrieved,</li>
 *   <li>a second call replaces the previous header,</li>
 *   <li>assigning {@code null} is allowed and results in a {@code null} header,</li>
 *   <li>changing the header does not unintentionally modify any message sections.</li>
 * </ul>
 *
 * <p>No private fields are accessed directly; the public {@code getHeader()} method is used
 * to observe the effect of {@code setHeader}. The test class follows the supplied framework
 * and does not modify the {@code @BeforeEach}/{@AfterEach} lifecycle methods.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_setHeader_Test {

    /** Message instance used in each test. */
    private Message message;

    /** Two distinct Header instances used for replacement checks. */
    private Header headerA;
    private Header headerB;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message and two Header objects before every test.
        message = new Message();          // default constructor creates an empty Header
        headerA = new Header(0x1234);     // arbitrary non‑zero identifier
        headerB = new Header(0xABCD);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC (optional).
        message = null;
        headerA = null;
        headerB = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /** Verify that {@code setHeader} stores the supplied header and that {@code getHeader} returns it. */
    @Test
    void setHeader_assignsHeader() {
        // given
        assertNotNull(message.getHeader(),
                "Message should have a non‑null default header before we replace it");

        // when
        message.setHeader(headerA);

        // then
        assertSame(headerA, message.getHeader(),
                "The header returned by getHeader() must be the same instance passed to setHeader()");
        // The header's ID should match the one we created.
        assertEquals(0x1234, message.getHeader().getID(),
                "Header ID should reflect the value supplied in the constructor");
    }

    /** Verify that a second call to {@code setHeader} replaces the previous header. */
    @Test
    void setHeader_replacesExistingHeader() {
        // given
        message.setHeader(headerA);
        assertSame(headerA, message.getHeader(),
                "Pre‑condition: headerA must be set first");

        // when
        message.setHeader(headerB);

        // then
        assertSame(headerB, message.getHeader(),
                "After a second setHeader call the header should be replaced with headerB");
        assertNotSame(headerA, message.getHeader(),
                "The original headerA instance should no longer be referenced");
    }

    /** Verify that assigning {@code null} does not throw and results in a null header. */
    @Test
    void setHeader_allowsNullHeader() {
        // when
        message.setHeader(null);

        // then
        assertNull(message.getHeader(),
                "After assigning null the internal header reference should be null");
    }

    /** Verify that changing the header does not affect the message sections. */
    @Test
    void setHeader_doesNotAlterSections() {
        // given
        // All sections should be empty on a newly created Message.
        for (int i = 0; i < 4; i++) {
            assertTrue(message.getSection(i).isEmpty(),
                    "Section " + i + " must be empty initially");
        }

        // when
        message.setHeader(headerA);
        message.setHeader(headerB); // replace header

        // then
        for (int i = 0; i < 4; i++) {
            assertTrue(message.getSection(i).isEmpty(),
                    "Section " + i + " should remain empty after header changes");
        }
    }
}
```