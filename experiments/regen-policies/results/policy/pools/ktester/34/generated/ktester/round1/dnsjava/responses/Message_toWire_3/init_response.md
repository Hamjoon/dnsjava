```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#toWire(int)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>Serialisation of an empty message.
 *   <li>Serialisation of a message that contains a question, an answer and an OPT record.
 *   <li>Verification that the byte array length reported by {@link Message#numBytes()} matches the
 *       actual wire length.
 *   <li>Round‑trip: the wire format produced by {@code toWire} can be parsed back into an
 *       equivalent {@link Message}.
 * </ul>
 *
 * <p>No private members of {@link Message} or its collaborators are accessed; all interactions are
 * performed through the public API.
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_3_Test {

  @BeforeAll
  static void setupBeforeAll() {
    // No global resources required for these tests.
  }

  @BeforeEach
  void setupBeforeEach() {
    // Nothing to initialise before each test.
  }

  @AfterEach
  void teardownAfterEach() {
    // No per‑test cleanup needed.
  }

  @AfterAll
  static void teardownAfterAll() {
    // No global cleanup required.
  }

  /**
   * Verify that an empty {@link Message} can be serialised and that the resulting wire length is
   * reported correctly via {@link Message#numBytes()}.
   */
  @Test
  void toWire_emptyMessage() {
    Message msg = new Message(); // uses a random ID
    byte[] wire = msg.toWire(Message.MAXLENGTH);
    assertNotNull(wire, "Wire representation must not be null");
    assertTrue(wire.length > 0, "Wire representation of an empty message should contain at least a header");
    assertEquals(wire.length, msg.numBytes(), "Message.numBytes() must match the length of the wire array");
    // Parsing the wire back should give an equivalent message (same header ID and empty sections)
    Message parsed = assertDoesNotThrow(() -> new Message(wire));
    assertEquals(msg.getHeader().getID(), parsed.getHeader().getID(), "Header IDs must match after round‑trip");
    for (int i = 0; i < 4; i++) {
      assertTrue(parsed.getSection(i).isEmpty(), "Section " + i + " must be empty after round‑trip");
    }
  }

  /**
   * Build a realistic DNS message (question, answer and an OPT pseudo‑record), serialise it,
   * and verify that the deserialised message contains the same records.
   */
  @Test
  void toWire_messageWithRecords() throws IOException {
    // --- build the original message -------------------------------------------------
    Name qname = Name.fromConstantString("www.example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message query = Message.newQuery(question);

    // add an answer record
    Record answer =
        new ARecord(qname, DClass.IN, 300, InetAddress.getByName("93.184.216.34"));
    query.addRecord(answer, Section.ANSWER);

    // add an OPT record (EDNS0) – the constructor creates a default OPT record
    query.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);

    // --- serialise ---------------------------------------------------------------
    byte[] wire = query.toWire(Message.MAXLENGTH);
    assertNotNull(wire);
    assertTrue(wire.length <= Message.MAXLENGTH, "Wire length must not exceed the supplied maxLength");
    assertEquals(wire.length, query.numBytes(), "Message.numBytes() must reflect the actual wire length");

    // --- deserialise ---------------------------------------------------------------
    Message parsed = new Message(wire);

    // --- assertions ---------------------------------------------------------------
    // Header ID must be preserved
    assertEquals(query.getHeader().getID(), parsed.getHeader().getID(), "Header ID must survive round‑trip");

    // Question section
    Record parsedQuestion = parsed.getQuestion();
    assertNotNull(parsedQuestion, "Parsed message must contain a question");
    assertEquals(question.getName(), parsedQuestion.getName(), "Question name must match");
    assertEquals(question.getType(), parsedQuestion.getType(), "Question type must match");
    assertEquals(question.getDClass(), parsedQuestion.getDClass(), "Question class must match");

    // Answer section
    List<Record> answerSection = parsed.getSection(Section.ANSWER);
    assertEquals(1, answerSection.size(), "Exactly one answer record expected");
    Record parsedAnswer = answerSection.get(0);
    assertEquals(answer.getName(), parsedAnswer.getName(), "Answer name must match");
    assertEquals(answer.getType(), parsedAnswer.getType(), "Answer type must match");
    assertEquals(answer.getDClass(), parsedAnswer.getDClass(), "Answer class must match");

    // Additional section – should contain the OPT record
    List<Record> additional = parsed.getSection(Section.ADDITIONAL);
    assertEquals(1, additional.size(), "Exactly one additional record expected");
    assertTrue(additional.get(0) instanceof OPTRecord, "Additional record must be an OPTRecord");
  }

  /**
   * Ensure that {@code toWire} respects the {@code maxLength} argument by throwing an
   * {@link WireParseException} when the generated message would exceed the limit.
   *
   * <p>The private {@code toWire(DNSOutput, int)} method throws {@code WireParseException}
   * when the output would be larger than {@code maxLength}. We provoke this situation by
   * constructing a message that is deliberately larger than a tiny limit (e.g., 10 bytes).
   */
  @Test
  void toWire_exceedsMaxLength_throwsWireParseException() {
    // Build a message with a single question – this already exceeds a 10‑byte limit.
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);

    // The limit is intentionally tiny to force the exception.
    int tinyLimit = 10;
    assertThrows(
        WireParseException.class,
        () -> msg.toWire(tinyLimit),
        "Serialising with a maxLength smaller than the message size must throw WireParseException");
  }
}
```