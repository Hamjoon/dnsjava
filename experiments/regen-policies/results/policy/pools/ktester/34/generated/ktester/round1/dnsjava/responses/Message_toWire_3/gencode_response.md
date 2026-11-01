```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link Message#toWire(int)}.
 *
 * <p>The test suite covers normal operation, edge‑cases, boundary conditions, and internal
 * state verification.  Private fields are accessed only via reflection where necessary (e.g.
 * the {@code size} field).  No direct access to private members is performed.
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

  /** Helper to read the private {@code size} field via reflection. */
  private static int getMessageSize(Message msg) throws Exception {
    Field f = Message.class.getDeclaredField("size");
    f.setAccessible(true);
    return f.getInt(msg);
  }

  /** -------------------------------------------------------------------------
   *  Normal operation – typical DNS message size
   * -------------------------------------------------------------------------- */
  @ParameterizedTest(name = "toWire with maxLength={0}")
  @CsvSource({
      "512",
      "65535"
  })
  void toWire_normalOperation(int maxLength) throws Exception {
    // Build a standard query message (question only)
    Name qname = Name.fromConstantString("www.example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);

    byte[] wire = msg.toWire(maxLength);
    assertNotNull(wire, "Returned wire must not be null");
    assertTrue(wire.length <= maxLength,
        "Wire length must not exceed supplied maxLength");
    assertEquals(wire.length, getMessageSize(msg),
        "Message.size must equal the length of the returned array");
  }

  /** -------------------------------------------------------------------------
   *  Edge cases – zero and negative limits
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_zeroMaxLength_returnsEmptyArray() throws Exception {
    Message msg = new Message(); // empty message (only header)
    byte[] wire = msg.toWire(0);
    assertNotNull(wire);
    assertEquals(0, wire.length, "Zero maxLength should produce an empty array");
    assertEquals(0, getMessageSize(msg), "Message.size must be 0 for empty output");
  }

  @Test
  void toWire_negativeMaxLength_throwsIllegalArgumentException() {
    Message msg = new Message();
    assertThrows(IllegalArgumentException.class,
        () -> msg.toWire(-1),
        "Negative maxLength must cause IllegalArgumentException");
  }

  /** -------------------------------------------------------------------------
   *  Edge cases – maxLength smaller than required for the message
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_exceedsMaxLength_throwsWireParseException() {
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    // 10 bytes is far smaller than a minimal DNS query
    assertThrows(WireParseException.class,
        () -> msg.toWire(10),
        "Serialising with a maxLength smaller than required must throw WireParseException");
  }

  /** -------------------------------------------------------------------------
   *  Boundary – maxLength larger than the class constant
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_maxLengthBeyondConstant_isCapped() throws Exception {
    Name qname = Name.fromConstantString("www.example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    int huge = 70_000; // > Message.MAXLENGTH (65535)
    byte[] wire = msg.toWire(huge);
    assertNotNull(wire);
    assertTrue(wire.length <= Message.MAXLENGTH,
        "Wire length must be capped at Message.MAXLENGTH");
    assertEquals(wire.length, getMessageSize(msg),
        "Message.size must match the actual wire length");
  }

  /** -------------------------------------------------------------------------
   *  State verification – size field reflects output length
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_sizeReflectsReturnedLength() throws Exception {
    Name qname = Name.fromConstantString("example.org.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    byte[] wire = msg.toWire(1024);
    assertEquals(wire.length, getMessageSize(msg),
        "After toWire, Message.size must equal the length of the returned byte array");
  }

  /** -------------------------------------------------------------------------
   *  Exact fit – maxLength equals required wire size
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_exactFit_success() throws Exception {
    // Minimal query: header (12) + question (16) = 28 bytes (as documented)
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    byte[] wire = msg.toWire(28);
    assertNotNull(wire);
    assertEquals(28, wire.length, "Wire length must be exactly 28 bytes");
    assertEquals(28, getMessageSize(msg), "Message.size must be 28");
  }

  @Test
  void toWire_exactFit_tooSmall_throwsIOException() {
    // Header (12) + question (16) = 28, so 12 bytes is insufficient for the question.
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    assertThrows(IOException.class,
        () -> msg.toWire(12),
        "Providing maxLength equal to header size only must cause an IOException");
  }

  /** -------------------------------------------------------------------------
   *  Message with TSIG – maxLength just sufficient for signed message
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_tsigExactFit_success() throws Exception {
    // Build a simple signed query (question + TSIG).  The exact size varies with the key,
    // but we can let the library compute it and then use that size as the limit.
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);

    // Create a dummy TSIG key (the secret can be empty – the library still creates a TSIG RR)
    TSIG key = new TSIG("key.", "hmac-md5.", new byte[0]);
    msg.setTSIG(key);

    // Serialise once to discover the required size
    byte[] firstWire = msg.toWire(Message.MAXLENGTH);
    int required = firstWire.length;

    // Now serialise with exactly that limit – should succeed
    byte[] exactWire = msg.toWire(required);
    assertEquals(required, exactWire.length,
        "Wire length must equal the exact required size when limit matches");
    assertEquals(required, getMessageSize(msg),
        "Message.size must reflect the exact wire length");
  }

  @Test
  void toWire_tsigInsufficient_throwsIOException() throws Exception {
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    TSIG key = new TSIG("key.", "hmac-md5.", new byte[0]);
    msg.setTSIG(key);
    byte[] full = msg.toWire(Message.MAXLENGTH);
    int required = full.length;
    // One byte short should trigger an exception
    assertThrows(IOException.class,
        () -> msg.toWire(required - 1),
        "One byte short of the required size must cause an IOException");
  }

  /** -------------------------------------------------------------------------
   *  Message with OPT (EDNS0) – insufficient maxLength
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_optInsufficient_throwsIOException() throws Exception {
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    msg.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
    // 20 bytes is known to be too small for the OPT RR
    assertThrows(IOException.class,
        () -> msg.toWire(20),
        "Insufficient maxLength for OPT record must throw IOException");
  }

  @Test
  void toWire_optExactFit_success() throws Exception {
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    msg.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
    // Determine required size first
    byte[] full = msg.toWire(Message.MAXLENGTH);
    int required = full.length;
    byte[] wire = msg.toWire(required);
    assertEquals(required, wire.length, "Exact fit for OPT‑containing message must succeed");
    assertEquals(required, getMessageSize(msg), "Message.size must match exact length");
  }

  /** -------------------------------------------------------------------------
   *  Multiple invocations – size field reflects latest serialization
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_multipleInvocations_sizeUpdates() throws Exception {
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);

    // First serialization
    byte[] first = msg.toWire(512);
    int firstSize = getMessageSize(msg);
    assertEquals(first.length, firstSize, "First size must match first wire length");

    // Add an answer record and serialize again with a larger limit
    Record answer = new ARecord(qname, DClass.IN, 300,
        InetAddress.getByName("93.184.216.34"));
    msg.addRecord(answer, Section.ANSWER);
    byte[] second = msg.toWire(1024);
    int secondSize = getMessageSize(msg);
    assertEquals(second.length, secondSize, "Second size must match second wire length");
    assertTrue(second.length > first.length,
        "Second wire length must be larger after adding a record");
  }

  /** -------------------------------------------------------------------------
   *  Empty Message – only DNS header present
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_emptyMessageExactHeader_success() throws Exception {
    Message msg = new Message(); // no question, no records
    byte[] wire = msg.toWire(12);
    assertEquals(12, wire.length, "Header‑only message with maxLength=12 must produce 12 bytes");
    assertEquals(12, getMessageSize(msg), "Message.size must be 12");
  }

  @Test
  void toWire_emptyMessageTooSmall_throwsIOException() {
    Message msg = new Message();
    assertThrows(IOException.class,
        () -> msg.toWire(11),
        "Header cannot fit into 11 bytes – should throw IOException");
  }

  /** -------------------------------------------------------------------------
   *  Extreme integer limits for maxLength
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_intMin_throwsIllegalArgumentException() {
    Message msg = new Message();
    assertThrows(IllegalArgumentException.class,
        () -> msg.toWire(Integer.MIN_VALUE),
        "Integer.MIN_VALUE should be treated as illegal");
  }

  @Test
  void toWire_intMax_isCapped() throws Exception {
    Name qname = Name.fromConstantString("www.example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    byte[] wire = msg.toWire(Integer.MAX_VALUE);
    assertNotNull(wire);
    assertTrue(wire.length <= Message.MAXLENGTH,
        "When maxLength exceeds MAXLENGTH the result must be capped");
    assertEquals(wire.length, getMessageSize(msg),
        "Message.size must match the capped wire length");
  }

  /** -------------------------------------------------------------------------
   *  Malformed record – name exceeds DNS label length
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_malformedRecord_throwsWireParseException() {
    // Build a name with a label > 63 characters
    StringBuilder longLabel = new StringBuilder();
    for (int i = 0; i < 70; i++) {
      longLabel.append('a');
    }
    String nameStr = longLabel + ".example.com.";
    Name badName = Name.fromString(nameStr);
    Record badRecord = Record.newRecord(badName, Type.A, DClass.IN);
    Message msg = Message.newQuery(badRecord);
    assertThrows(WireParseException.class,
        () -> msg.toWire(512),
        "Serialising a record with an over‑long label must throw WireParseException");
  }

  /** -------------------------------------------------------------------------
   *  Invalid record type for section
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_invalidRecordTypeForSection_throwsIllegalArgumentException() {
    Name qname = Name.fromConstantString("example.com.");
    // TXT is not allowed in the QUESTION section
    Record txt = new TXTRecord(qname, DClass.IN, 0, "invalid");
    Message msg = new Message();
    // Directly add to QUESTION (section 0) – the API should reject it
    assertThrows(IllegalArgumentException.class,
        () -> msg.addRecord(txt, Section.QUESTION),
        "Adding a TXT record to the QUESTION section must throw IllegalArgumentException");
  }

  /** -------------------------------------------------------------------------
   *  Null record in a section (via reflection)
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_nullRecordInSection_throwsNullPointerException() throws Exception {
    Message msg = Message.newQuery(Record.newRecord(Name.fromConstantString("example.com."),
        Type.A, DClass.IN));

    // Access the internal List for the ANSWER section (index 1) and insert null.
    Field sectionsField = Message.class.getDeclaredField("sections");
    sectionsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
    sections[Section.ANSWER].add(null);

    assertThrows(NullPointerException.class,
        () -> msg.toWire(512),
        "Presence of a null record in a section must cause NullPointerException");
  }

  /** -------------------------------------------------------------------------
   *  Invalid TSIG state during serialization (via reflection)
   * -------------------------------------------------------------------------- */
  @Test
  void toWire_invalidTsigState_throwsIllegalStateException() throws Exception {
    Name qname = Name.fromConstantString("example.com.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message msg = Message.newQuery(question);
    // Add a legitimate TSIG so that the serializer will attempt to write it
    TSIG key = new TSIG("key.", "hmac-md5.", new byte[0]);
    msg.setTSIG(key);

    // Corrupt the private tsigState field
    Field tsigStateField = Message.class.getDeclaredField("tsigState");
    tsigStateField.setAccessible(true);
    tsigStateField.setInt(msg, 99); // illegal value

    assertThrows(IllegalStateException.class,
        () -> msg.toWire(512),
        "Unexpected tsigState must trigger IllegalStateException");
  }

  // ---------------------------------------------------------------------------
  // Existing tests from the original scaffold (kept unchanged)
  // ---------------------------------------------------------------------------

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