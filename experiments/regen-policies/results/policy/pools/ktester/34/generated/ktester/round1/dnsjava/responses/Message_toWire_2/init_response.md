```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Message#toWire()}.
 *
 * The tests create a {@link Message}, populate it with a question and a few records,
 * obtain the wire format via {@code toWire()}, rebuild a new {@code Message}
 * from the resulting byte array and verify that the two messages are equivalent.
 *
 * No private members of {@code Message} or its dependencies are accessed.
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_2_Test {

    /** A reusable {@link Message} instance used by the tests. */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh, empty message for each test.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verify that {@code toWire()} produces a byte array that can be parsed back
     * into an equivalent {@link Message}.
     */
    @Test
    void toWire_roundTrip_basicMessage() throws IOException {
        // --- given -----------------------------------------------------------
        // Build a simple query message: question + one answer record.
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);

        // Add an answer record.
        Record answer = new ARecord(qname, DClass.IN, 3600,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(answer, Section.ANSWER);

        // Add an OPT record (EDNS) to the additional section.
        Record opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        // --- when ------------------------------------------------------------
        byte[] wire = message.toWire();

        // --- then ------------------------------------------------------------
        assertNotNull(wire, "Wire format must not be null");
        assertTrue(wire.length > 0, "Wire format must contain data");

        // Re‑parse the wire data.
        Message parsed = new Message(wire);

        // Header ID should be the same (generated randomly for the default ctor).
        assertEquals(message.getHeader().getID(), parsed.getHeader().getID(),
                "Header IDs must match after round‑trip");

        // Question section must contain exactly the original question.
        List<Record> parsedQuestion = parsed.getSection(Section.QUESTION);
        assertEquals(1, parsedQuestion.size(), "Exactly one question expected");
        assertEquals(question, parsedQuestion.get(0), "Question record must be preserved");

        // Answer section must contain exactly the original answer.
        List<Record> parsedAnswer = parsed.getSection(Section.ANSWER);
        assertEquals(1, parsedAnswer.size(), "Exactly one answer expected");
        assertEquals(answer, parsedAnswer.get(0), "Answer record must be preserved");

        // Additional section must contain the OPT record (and nothing else).
        List<Record> parsedAdditional = parsed.getSection(Section.ADDITIONAL);
        assertEquals(1, parsedAdditional.size(), "Exactly one additional record expected");
        assertTrue(parsedAdditional.get(0) instanceof OPTRecord,
                "Additional record must be an OPTRecord");
    }

    /**
     * Verify that {@code toWire()} respects the maximum length parameter
     * when the overloaded {@code toWire(int)} method is used.
     *
     * The test creates a message that would exceed the supplied limit and
     * checks that the returned byte array is truncated to the requested size.
     */
    @Test
    void toWire_withMaxLength_truncates() throws IOException {
        // --- given -----------------------------------------------------------
        // Build a message with a relatively large TXT record.
        Name qname = Name.fromConstantString("large.example.");
        Record txt = new TXTRecord(qname, DClass.IN, 3600,
                "This is a very long TXT payload that will force the wire format to exceed the limit");
        message.addRecord(txt, Section.ANSWER);

        // --- when ------------------------------------------------------------
        // Request a very small maximum length.
        int maxLength = 50;
        byte[] limitedWire = message.toWire(maxLength);

        // --- then ------------------------------------------------------------
        assertNotNull(limitedWire);
        assertTrue(limitedWire.length <= maxLength,
                "Wire data must not exceed the requested max length");
    }
}
```