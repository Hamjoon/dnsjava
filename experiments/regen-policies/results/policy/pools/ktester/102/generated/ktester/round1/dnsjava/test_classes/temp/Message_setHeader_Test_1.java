package org.xbill.DNS;

import org.junit.*;
import static org.junit.Assert.*;

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
        // default constructor creates an empty Header
        message = new Message();
        // arbitrary non‑zero identifier
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
        assertNotNull("Message should have a non‑null default header before we replace it", message.getHeader());
        // when
        message.setHeader(headerA);
        // then
        assertSame("The header returned by getHeader() must be the same instance passed to setHeader()", headerA, message.getHeader());
        // The header's ID should match the one we created.
        assertEquals("Header ID should reflect the value supplied in the constructor", 0x1234, message.getHeader().getID());
    }

    /**
     * Verify that a second call to {@code setHeader} replaces the previous header.
     */
    @Test
    void setHeader_replacesExistingHeader() {
        // given
        message.setHeader(headerA);
        assertSame("Pre‑condition: headerA must be set first", headerA, message.getHeader());
        // when
        message.setHeader(headerB);
        // then
        assertSame("After a second setHeader call the header should be replaced with headerB", headerB, message.getHeader());
        assertNotSame("The original headerA instance should no longer be referenced", headerA, message.getHeader());
    }

    /**
     * Verify that assigning {@code null} does not throw and results in a null header.
     */
    @Test
    void setHeader_allowsNullHeader() {
        // when
        message.setHeader(null);
        // then
        assertNull("After assigning null the internal header reference should be null", message.getHeader());
    }

    /**
     * Verify that changing the header does not affect the message sections.
     */
    @Test
    void setHeader_doesNotAlterSections() {
        // given
        // All sections should be empty on a newly created Message.
        for (int i = 0; i < 4; i++) {
            assertTrue("Section " + i + " must be empty initially", message.getSection(i).isEmpty());
        }
        // when
        message.setHeader(headerA);
        // replace header
        message.setHeader(headerB);
        // then
        for (int i = 0; i < 4; i++) {
            assertTrue("Section " + i + " should remain empty after header changes", message.getSection(i).isEmpty());
        }
    }

    @Before
    public void setUp() {
        // Create a fresh Message and two Header objects before every test.
        // The default constructor creates an empty Header.
        message = new Message();
        // arbitrary non‑zero identifiers
        headerA = new Header(0x1234);
        headerB = new Header(0xABCD);
    }

    @After
    public void tearDown() {
        // Clear references to help GC (optional).
        message = null;
        headerA = null;
        headerB = null;
    }
}
