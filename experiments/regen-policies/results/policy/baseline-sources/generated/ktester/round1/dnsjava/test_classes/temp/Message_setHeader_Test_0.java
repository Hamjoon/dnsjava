package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_setHeader_Test {

    private Message message;

    private Header headerA;

    private Header headerB;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for each test – the no‑arg constructor creates a default Header.
        message = new Message();
        // Two distinct Header instances with different IDs for replacement tests.
        headerA = new Header(0x1234);
        headerB = new Header(0xABCD);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly drop references – helps the GC when many tests run.
        message = null;
        headerA = null;
        headerB = null;
    }

    /**
     * Supplies a stream of arguments representing the different header configurations
     * described in the JSON test cases.
     *
     * <p>Each {@code Arguments} entry contains:
     * <ul>
     *   <li>A {@link Header} instance (or {@code null}).</li>
     *   <li>An {@link java.util.function.Consumer} that performs the assertions
     *       specific to that configuration.</li>
     * </ul>
     */
    private static Stream<Arguments> headerProvider() {
        // 1. Freshly created, non‑null Header.
        // default constructor
        Header freshHeader = new Header();
        // 2. Null header.
        Header nullHeader = null;
        // 3. Header with RCODE = 2 (SERVFAIL).
        Header rcodeHeader = Header.builder().setRcode(// SERVFAIL
        2).build();
        // 4. Header with specific ID and QR flag set.
        Header idFlagHeader = Header.builder().setID(0xABCD).setFlag(// assume builder toggles the flag on
        Header.FLAG_QR).build();
        return Stream.of(Arguments.of(freshHeader, (Runnable) () -> {
            // getHeader must return the same instance.
            assertSame(freshHeader, freshHeader.getMessage().getHeader(), "The stored header instance should be identical to the one passed.");
        }), Arguments.of(nullHeader, (Runnable) () -> {
            // getHeader must return null.
            Message m = new Message();
            m.setHeader(null);
            assertNull(m.getHeader(), "Header should be null after setting null.");
        }), Arguments.of(rcodeHeader, (Runnable) () -> {
            Message m = new Message();
            m.setHeader(rcodeHeader);
            // Message.getRcode() delegates to the header's RCODE.
            assertEquals(2, m.getRcode(), "Message.getRcode() must reflect the RCODE of the set header.");
        }), Arguments.of(idFlagHeader, (Runnable) () -> {
            Message m = new Message();
            m.setHeader(idFlagHeader);
            // Verify ID.
            assertEquals(0xABCD, m.getHeader().getID(), "Header ID should be preserved.");
            // Verify QR flag.
            assertTrue(m.getHeader().getFlag(Header.FLAG_QR), "Header QR flag must be true after setting the header.");
        }));
    }

    @Test
    void setHeader_replacesExistingHeader() {
        // Given: a Message with its default header (created by the no‑arg ctor).
        Header originalHeader = message.getHeader();
        assertNotNull(originalHeader, "Default header should not be null");
        // When: we set a new header.
        message.setHeader(headerA);
        // Then: the message should return the newly set header instance.
        assertSame(headerA, message.getHeader(), "Message should return the header that was set via setHeader");
        // When: we replace it again with a different header.
        message.setHeader(headerB);
        // Then: the new header should be reflected.
        assertSame(headerB, message.getHeader(), "Message should reflect the most recently set header");
    }

    @Test
    void setHeader_preservesSectionState() {
        // Given: a freshly created message (all sections are empty).
        for (int i = 0; i < 4; i++) {
            assertTrue(message.getSection(i).isEmpty(), "Section " + i + " should be empty before setting header");
        }
        // When: we set a new header.
        message.setHeader(headerA);
        // Then: sections remain unchanged (still empty).
        for (int i = 0; i < 4; i++) {
            assertTrue(message.getSection(i).isEmpty(), "Section " + i + " should remain empty after setting header");
        }
    }

    @Test
    void setHeader_allowsNullHeader() {
        // When: we explicitly set a null header.
        message.setHeader(null);
        // Then: getHeader should return null (no IllegalArgumentException is defined).
        assertNull(message.getHeader(), "Message.getHeader() should return null after setting a null header");
    }

    @Test
    void setHeader_headerIdIsReflectedInMessage() {
        // When: we set a header with a known ID.
        message.setHeader(headerA);
        // Then: the ID reported by the message's header matches the header we set.
        assertEquals(headerA.getID(), message.getHeader().getID(), "Header ID should be preserved after setHeader");
    }

    /**
     * Executes the parameterised test cases defined in {@link #headerProvider()}.
     *
     * <p>The test body delegates to the {@code verifier} lambda supplied with each
     * argument, keeping the method concise while still providing a separate
     * description for each scenario.</p>
     */
    @ParameterizedTest(name = "[{index}] setHeader with header = {0}")
    @MethodSource("headerProvider")
    void setHeader_parameterisedTests(Header header, Runnable verifier) {
        // The verifier encapsulates the assertions for the particular case.
        verifier.run();
    }
}
