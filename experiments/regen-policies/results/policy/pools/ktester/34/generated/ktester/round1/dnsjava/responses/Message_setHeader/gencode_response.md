```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#setHeader(Header)}.
 *
 * <p>The tests cover the behaviours described in the supplied JSON test cases:
 * <ul>
 *   <li>Storing a freshly created {@link Header} and retrieving it.</li>
 *   <li>Passing {@code null} clears the header field.</li>
 *   <li>Calling {@code setHeader} twice replaces the previous header.</li>
 *   <li>The stored header is the same instance supplied – subsequent external
 *       modifications are visible through {@link Message#getHeader()}.</li>
 * </ul>
 *
 * <p>Additional tests from the original scaffold are retained to ensure that
 * changing the header does not affect other parts of the {@link Message}
 * (sections, IDs, etc.).
 */
@ExtendWith(MockitoExtension.class)
class Message_setHeader_Test {

    private Message message;
    private Header header1;
    private Header header2;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for each test
        message = new Message();

        // Two distinct Header instances with different IDs – used by several tests
        header1 = new Header(1234);
        header2 = new Header(5678);
    }

    @AfterEach
    void teardownAfterEach() {
        message = null;
        header1 = null;
        header2 = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    /* -----------------------------------------------------------------------
     * Original scaffold tests (kept unchanged)
     * ----------------------------------------------------------------------- */

    @Test
    void setHeader_replacesHeader_correctly() {
        Header originalHeader = message.getHeader();
        assertNotNull(originalHeader, "Message should always have a non‑null Header");

        message.setHeader(header1);
        assertSame(header1, message.getHeader(),
                "Message.getHeader() should return the Header set via setHeader");

        message.setHeader(header2);
        assertSame(header2, message.getHeader(),
                "Subsequent calls to setHeader should replace the previous Header");
        assertNotSame(header1, message.getHeader(),
                "The earlier Header instance should no longer be referenced");
    }

    @Test
    void setHeader_affectsHeaderId() {
        message.setHeader(header1);
        assertEquals(1234, message.getHeader().getID(),
                "Header ID should match the ID of the Header supplied to setHeader");

        message.setHeader(header2);
        assertEquals(5678, message.getHeader().getID(),
                "Header ID should be updated after calling setHeader with a different Header");
    }

    @Test
    void setHeader_doesNotAlterMessageSections() {
        Name name = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Record answer = Record.newRecord(name, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);
        message.addRecord(answer, Section.ANSWER);

        assertEquals(1, message.getSection(Section.QUESTION).size(),
                "Question section should contain one record before setHeader");
        assertEquals(1, message.getSection(Section.ANSWER).size(),
                "Answer section should contain one record before setHeader");

        message.setHeader(header1);
        assertEquals(1, message.getSection(Section.QUESTION).size(),
                "Question section size should remain unchanged after setHeader");
        assertEquals(1, message.getSection(Section.ANSWER).size(),
                "Answer section size should remain unchanged after setHeader");
    }

    /* -----------------------------------------------------------------------
     * Tests derived from the JSON test cases
     * ----------------------------------------------------------------------- */

    /**
     * Verify that a freshly created Header object is stored correctly and can be
     * retrieved via {@link Message#getHeader()}.
     */
    @Test
    void setHeader_storesFreshHeaderInstance() {
        Header fresh = new Header();                     // uses default constructor
        message.setHeader(fresh);
        assertSame(fresh, message.getHeader(),
                "The Header instance passed to setHeader should be returned by getHeader");
    }

    /**
     * Ensure that passing {@code null} to {@code setHeader} clears the internal
     * header field and {@link Message#getHeader()} returns {@code null}.
     */
    @Test
    void setHeader_nullClearsHeader() {
        // Initialise with a non‑null header first (the default constructor already does)
        assertNotNull(message.getHeader(), "Precondition: header should be non‑null");

        // Now set to null
        message.setHeader(null);
        assertNull(message.getHeader(),
                "After setting a null header, getHeader() must return null");
    }

    /**
     * Test that calling {@code setHeader} twice replaces the previous Header with
     * the most recently supplied one.
     */
    @Test
    void setHeader_secondCallOverridesFirst() {
        Header first = new Header(1111);
        Header second = new Header(2222);

        message.setHeader(first);
        assertSame(first, message.getHeader(),
                "First header should be stored after first setHeader call");

        message.setHeader(second);
        assertSame(second, message.getHeader(),
                "Second header should replace the first one");
        assertNotSame(first, message.getHeader(),
                "The earlier Header instance must no longer be referenced");
    }

    /**
     * Confirm that {@code setHeader} stores a reference to the supplied Header
     * object, so later modifications of that Header are visible through the
     * Message.
     */
    @Test
    void setHeader_keepsReferenceAndReflectsExternalModifications() {
        Header mutable = new Header();        // default ID, Rcode = 0
        mutable.setRcode(2);                  // change state before setting

        message.setHeader(mutable);
        assertEquals(2, message.getHeader().getRcode(),
                "Rcode should reflect the value set on the Header before setHeader");

        // Modify the Header after it has been stored in the Message
        mutable.setRcode(5);
        assertEquals(5, message.getHeader().getRcode(),
                "Changes to the Header after setHeader must be visible via getHeader");
    }

    /* -----------------------------------------------------------------------
     * Parameterised version of the above cases (optional, shows alternative style)
     * ----------------------------------------------------------------------- */

    private static Stream<HeaderScenario> headerScenarios() {
        return Stream.of(
                // fresh header
                new HeaderScenario(new Header(), (msg, hdr) -> {
                    assertSame(hdr, msg.getHeader(),
                            "Fresh Header should be stored and returned unchanged");
                }),

                // null header – clears field
                new HeaderScenario(null, (msg, hdr) -> {
                    assertNull(msg.getHeader(), "Header should be null after assigning null");
                })
        );
    }

    @ParameterizedTest
    @MethodSource("headerScenarios")
    void setHeader_parameterised(HeaderScenario scenario) {
        scenario.apply(message);
    }

    /** Helper holder for a parameterised scenario. */
    private static final class HeaderScenario {
        final Header header;
        final BiConsumer<Message, Header> assertions;

        HeaderScenario(Header header, BiConsumer<Message, Header> assertions) {
            this.header = header;
            this.assertions = assertions;
        }

        void apply(Message msg) {
            msg.setHeader(header);
            assertions.accept(msg, header);
        }

        @Override
        public String toString() {
            return "HeaderScenario{" + (header == null ? "null" : "Header@" + System.identityHashCode(header)) + '}';
        }
    }
}
```