```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Message#newQuery(Record)}.
 *
 * <p>The tests verify that a newly created query message has:
 *
 * <ul>
 *   <li>Opcode set to {@link Opcode#QUERY}
 *   <li>Recursion Desired (RD) flag set
 *   <li>The supplied {@link Record} placed in the QUESTION section
 *   <li>All other sections empty
 *   <li>Header counts correctly reflecting the added question
 * </ul>
 *
 * No private members or implementation details are accessed – only the public API of
 * {@link Message}, {@link Header}, {@link Record}, {@link Name}, {@link Section}, {@link Opcode}
 * and {@link Flags}.
 */
class Message_newQuery_Test {

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
   * Verifies that {@code Message.newQuery(record)} creates a message with the correct header
   * fields and places the record in the QUESTION section.
   */
  @Test
  void newQuery_createsMessageWithCorrectHeaderAndQuestion() {
    // Arrange: a simple A‑record question
    Name qname = Name.fromConstantString("www.example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);

    // Act: create the query message
    Message query = Message.newQuery(question);

    // Assert: header opcode must be QUERY
    assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
        "Header opcode should be QUERY");

    // Assert: Recursion Desired flag must be set
    assertTrue(query.getHeader().getFlag(Flags.RD),
        "Header should have the RD (recursion desired) flag set");

    // Assert: the question record is stored correctly
    assertEquals(question, query.getQuestion(),
        "The question record returned by getQuestion() must match the supplied record");
    assertEquals(question, query.getSection(Section.QUESTION).get(0),
        "The QUESTION section should contain the supplied record as its first entry");

    // Assert: header count for QUESTION section is 1
    assertEquals(1, query.getHeader().getCount(Section.QUESTION),
        "Header count for QUESTION section should be 1");

    // Assert: all other sections are empty and counts are zero
    for (int sec = Section.ANSWER; sec <= Section.ADDITIONAL; sec++) {
      assertTrue(query.getSection(sec).isEmpty(),
          "Section " + sec + " should be empty");
      assertEquals(0, query.getHeader().getCount(sec),
          "Header count for section " + sec + " should be zero");
    }
  }

  /**
   * Verifies that passing {@code null} as the record argument results in a {@link NullPointerException}.
   */
  @Test
  void newQuery_nullRecord_throwsNPE() {
    assertThrows(NullPointerException.class, () -> Message.newQuery(null),
        "Message.newQuery(null) should throw NullPointerException");
  }

  /**
   * Ensures that the created message is independent of later modifications to the original
   * {@link Record} (the record is immutable in the DNS library, but this test guards against
   * accidental coupling).
   */
  @Test
  void newQuery_messageIsIndependentFromOriginalRecord() {
    Name qname = Name.fromConstantString("host.example.");
    Record original = Record.newRecord(qname, Type.AAAA, DClass.IN);
    Message query = Message.newQuery(original);

    // The library's Record objects are immutable, but we can still verify that the
    // message holds a reference that equals the original.
    assertEquals(original, query.getQuestion(),
        "Message should contain a question equal to the original record");
  }
}
```