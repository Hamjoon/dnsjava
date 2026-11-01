```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#normalize(Message)}.
 *
 * <p>The tests focus on the public contract of {@code normalize(Message)}:
 *
 * <ul>
 *   <li>Irrelevant records (those not belonging to the answer or additional sections that are
 *       required for the response) are removed.
 *   <li>The method never mutates the supplied query message.
 *   <li>A {@code NullPointerException} is thrown when a {@code null} query is supplied (the
 *       implementation does not guard against it).
 * </ul>
 *
 * <p>No private members or implementation details are accessed – only the public API of
 * {@link Message} and related DNS classes.
 */
@ExtendWith(MockitoExtension.class)
class Message_normalize_Test {

  /** A fresh response message created before each test. */
  private Message response;

  /** A simple MX query used in several test cases. */
  private Message query;

  @BeforeAll
  static void setupBeforeAll() {
    // No global resources required for these tests.
  }

  @BeforeEach
  void setupBeforeEach() throws IOException {
    // Build a minimal query:   example.com.  MX  IN
    Record queryRecord =
        Record.newRecord(Name.fromConstantString("example.com."), Type.MX, DClass.IN);
    query = Message.newQuery(queryRecord);

    // Build a response that contains extra records which should be stripped by normalize().
    response = new Message();
    // Add the question record (should stay untouched after normalize)
    response.addRecord(queryRecord, Section.QUESTION);
    // Add an answer record that is *relevant* – it matches the query type.
    response.addRecord(
        Record.newRecord(Name.fromConstantString("example.com."), Type.MX, DClass.IN),
        Section.ANSWER);
    // Add an additional record that is *irrelevant* – e.g., a TXT record.
    response.addRecord(
        Record.newRecord(Name.fromConstantString("example.com."), Type.TXT, DClass.IN),
        Section.ADDITIONAL);
  }

  @AfterEach
  void teardownAfterEach() {
    // Help GC – not strictly necessary but keeps the test class tidy.
    response = null;
    query = null;
  }

  @AfterAll
  static void teardownAfterAll() {
    // No global resources to clean up.
  }

  /**
   * Verifies that {@code normalize(Message)} removes records that are not part of the answer
   * section and clears the additional section when it does not contain required records.
   */
  @Test
  void normalize_removesIrrelevantSections() throws WireParseException {
    // Act – normalize the response with respect to the original query.
    Message normalized = response.normalize(query);

    // The answer section should still contain the MX record we added.
    assertThat(normalized.getSection(Section.ANSWER)).hasSize(1);
    assertEquals(Type.MX, normalized.getSection(Section.ANSWER).get(0).getType());

    // The additional section must be empty because the only record we added there was a TXT
    // record, which is not required for an MX response.
    assertThat(normalized.getSection(Section.ADDITIONAL)).isEmpty();
    assertEquals(0, normalized.getHeader().getCount(Section.ADDITIONAL));

    // The question section must remain unchanged.
    assertThat(normalized.getSection(Section.QUESTION)).hasSize(1);
    assertEquals(query.getQuestion(), normalized.getQuestion());
  }

  /**
   * Ensures that the supplied query message is not altered by {@code normalize(Message)}.
   */
  @Test
  void normalize_doesNotModifyQuery() throws WireParseException {
    // Keep a copy of the query's header and question for later comparison.
    Header originalHeader = query.getHeader().clone();
    Record originalQuestion = query.getQuestion();

    // Normalise the response – the method should not touch the query.
    Message normalized = response.normalize(query);

    // Verify that the query's header and question are still identical to the originals.
    assertEquals(originalHeader.toString(), query.getHeader().toString(),
        "Query header must remain unchanged");
    assertEquals(originalQuestion, query.getQuestion(),
        "Query question must remain unchanged");
    // Also verify that the returned Message is a different instance.
    assertNotSame(response, normalized);
  }

  /**
   * The implementation does not guard against a {@code null} argument, therefore a
   * {@code NullPointerException} is expected.
   */
  @Test
  void normalize_nullQuery_throwsNPE() {
    assertThrows(NullPointerException.class, () -> response.normalize(null));
  }
}
```