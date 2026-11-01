```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#getQuestion()}.
 *
 * <p>The tests verify the contract of {@code getQuestion()} under the following scenarios:
 *
 * <ul>
 *   <li>When the QUESTION section is {@code null} or empty the method returns {@code null}.
 *   <li>When a single question record is present the same record is returned.
 *   <li>When multiple records are present in the QUESTION section the first one is returned.
 *   <li>The method never interferes with records stored in other sections.
 * </ul>
 *
 * <p>No private fields or methods of {@link Message} are accessed directly; the public API is
 * used to set up the test fixtures.
 */
@ExtendWith(MockitoExtension.class)
class Message_getQuestion_Test {

  /** Message instance used by the tests. */
  private Message message;

  /** A sample question record used by several tests. */
  private Record questionRecord;

  /** A second question record to test ordering. */
  private Record secondQuestionRecord;

  @BeforeAll
  static void setupBeforeAll() {
    // No global resources required for these tests.
  }

  @BeforeEach
  void setupBeforeEach() throws UnknownHostException {
    // Create a fresh Message for each test.
    message = new Message();

    // Build a simple A‑record question: www.example.com. IN A
    Name qname = Name.fromConstantString("www.example.com.");
    questionRecord = Record.newRecord(qname, Type.A, DClass.IN);

    // A second question record (different name) to verify ordering.
    Name qname2 = Name.fromConstantString("mail.example.com.");
    secondQuestionRecord = Record.newRecord(qname2, Type.A, DClass.IN);
  }

  @AfterEach
  void teardownAfterEach() {
    // Help GC – not strictly required but keeps the pattern symmetrical.
    message = null;
    questionRecord = null;
    secondQuestionRecord = null;
  }

  @AfterAll
  static void teardownAfterAll() {
    // No global cleanup needed.
  }

  @Test
  void getQuestion_whenQuestionSectionIsEmpty_returnsNull() {
    // By default a newly constructed Message has an empty QUESTION section.
    assertNull(message.getQuestion(), "Expected null when QUESTION section is empty");
  }

  @Test
  void getQuestion_whenQuestionSectionIsNull_returnsNull() {
    // Simulate a null QUESTION section by directly removing all records.
    // The public API does not expose a way to set the internal array to null,
    // but removing all records leaves the list empty, which is equivalent for the
    // contract of getQuestion().
    message.removeAllRecords(Section.QUESTION);
    assertNull(message.getQuestion(), "Expected null after removing all QUESTION records");
  }

  @Test
  void getQuestion_withSingleRecord_returnsThatRecord() {
    // Add a single question record.
    message.addRecord(questionRecord, Section.QUESTION);
    Record result = message.getQuestion();

    assertNotNull(result, "Result should not be null when a question is present");
    assertEquals(questionRecord, result, "Returned record should be the one that was added");
    assertEquals(questionRecord.getName(), result.getName(),
        "Returned record should have the same name as the added question");
  }

  @Test
  void getQuestion_withMultipleRecords_returnsFirstRecord() {
    // Add two question records; the first added should be returned.
    message.addRecord(questionRecord, Section.QUESTION);
    message.addRecord(secondQuestionRecord, Section.QUESTION);

    Record result = message.getQuestion();

    assertNotNull(result, "Result should not be null when multiple questions are present");
    assertEquals(questionRecord, result,
        "The first record added to the QUESTION section must be returned");
  }

  @Test
  void getQuestion_doesNotReturnRecordsFromOtherSections() throws IOException {
    // Add a question record to the QUESTION section.
    message.addRecord(questionRecord, Section.QUESTION);

    // Add a record to the ANSWER section – it must not affect getQuestion().
    Name answerName = Name.fromConstantString("www.example.com.");
    Record answerRecord = new ARecord(answerName, DClass.IN, 0,
        InetAddress.getByName("127.0.0.1"));
    message.addRecord(answerRecord, Section.ANSWER);

    Record result = message.getQuestion();

    assertEquals(questionRecord, result,
        "Only records from the QUESTION section should be considered");
  }

  @Test
  void getQuestion_afterCloningPreservesQuestion() throws CloneNotSupportedException {
    // Build a query message with a question, then clone it.
    message.addRecord(questionRecord, Section.QUESTION);
    Message clone = message.clone();

    assertNotNull(clone.getQuestion(), "Cloned message should retain the question");
    assertEquals(message.getQuestion(), clone.getQuestion(),
        "Cloned message's question must be equal to the original's");
  }

  @Test
  void getQuestion_parsesFromWireAndReturnsCorrectRecord() throws IOException {
    // Construct a message, serialize it, then deserialize to ensure the wire format
    // correctly restores the question.
    message.addRecord(questionRecord, Section.QUESTION);
    byte[] wire = message.toWire();

    Message fromWire = new Message(wire);
    Record fromWireQuestion = fromWire.getQuestion();

    assertNotNull(fromWireQuestion, "Deserialized message should contain a question");
    assertEquals(questionRecord.getName(), fromWireQuestion.getName(),
        "Deserialized question name must match the original");
    assertEquals(questionRecord.getType(), fromWireQuestion.getType(),
        "Deserialized question type must match the original");
  }
}
```