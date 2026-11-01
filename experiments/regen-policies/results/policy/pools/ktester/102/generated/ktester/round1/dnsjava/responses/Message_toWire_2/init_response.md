```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#toWire()}.
 *
 * The tests verify that the byte array produced by {@code toWire()} can be parsed back
 * into an equivalent {@link Message} instance and that the internal {@code size}
 * field is updated correctly (indirectly verified by the length of the output).
 *
 * No private members are accessed; the public API is used exclusively.
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_2_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that an empty {@link Message} can be serialized and deserialized
     * without loss of information.
     */
    @Test
    void toWire_emptyMessage() throws IOException {
        Message original = new Message();               // default constructor
        byte[] wire = original.toWire();                // serialize

        // The wire format must contain at least the header (12 bytes) + the length field
        // used by the DNS protocol.  The exact size is not important for the test,
        // only that it is non‑zero and that deserialization succeeds.
        assertNotNull(wire);
        assertTrue(wire.length > 0, "Wire representation should not be empty");

        Message parsed = new Message(wire);             // deserialize
        // Header IDs should match
        assertEquals(original.getHeader().getID(), parsed.getHeader().getID(),
                "Header IDs must be equal after round‑trip");

        // All four sections must be empty
        for (int i = 0; i < 4; i++) {
            assertTrue(parsed.getSection(i).isEmpty(),
                    "Section " + i + " should be empty after round‑trip");
        }
    }

    /**
     * Verify that a message containing a question and an answer round‑trips
     * correctly through {@code toWire()}.
     */
    @Test
    void toWire_messageWithQuestionAndAnswer() throws IOException, UnknownHostException {
        // Build a simple query message
        Name name = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message query = Message.newQuery(question);

        // Add an answer record
        ARecord answer = new ARecord(name, DClass.IN, 300, InetAddress.getByName("127.0.0.1"));
        query.addRecord(answer, Section.ANSWER);

        // Serialize
        byte[] wire = query.toWire();

        // Deserialize
        Message parsed = new Message(wire);

        // Header ID must be preserved
        assertEquals(query.getHeader().getID(), parsed.getHeader().getID(),
                "Header ID must survive serialization");

        // Question section
        List<Record> parsedQuestion = parsed.getSection(Section.QUESTION);
        assertEquals(1, parsedQuestion.size(), "Exactly one question record expected");
        assertEquals(question.getName(), parsedQuestion.get(0).getName(),
                "Question name must be preserved");
        assertEquals(question.getType(), parsedQuestion.get(0).getType(),
                "Question type must be preserved");

        // Answer section
        List<Record> parsedAnswer = parsed.getSection(Section.ANSWER);
        assertEquals(1, parsedAnswer.size(), "Exactly one answer record expected");
        Record rec = parsedAnswer.get(0);
        assertTrue(rec instanceof ARecord, "Answer should be an ARecord");
        ARecord aRec = (ARecord) rec;
        assertEquals(answer.getAddress(), aRec.getAddress(),
                "Answer address must be preserved");
        assertEquals(answer.getTTL(), aRec.getTTL(),
                "Answer TTL must be preserved");
    }

    /**
     * Verify that {@code toWire(int maxLength)} respects the supplied maximum
     * length and throws {@link MessageSizeExceededException} when the message
     * would exceed it.
     */
    @Test
    void toWire_respectsMaxLength() throws IOException, UnknownHostException {
        // Build a message that is larger than a tiny limit.
        Name name = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);

        // Add many answer records to increase size.
        for (int i = 0; i < 20; i++) {
            ARecord a = new ARecord(name, DClass.IN, 300,
                    InetAddress.getByName("192.0.2." + i));
            msg.addRecord(a, Section.ANSWER);
        }

        // The default max length is 65535, so this should succeed.
        assertDoesNotThrow(() -> msg.toWire(65535));

        // Use a deliberately small limit; we expect an exception.
        assertThrows(MessageSizeExceededException.class,
                () -> msg.toWire(50),
                "MessageSizeExceededException should be thrown when maxLength is too small");
    }
}
```