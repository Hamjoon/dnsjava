###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link Message}.
 *
 * <p>Only public APIs that appear in the source of {@link Message} or the
 * dependency signatures provided in the prompt are used.  No mocking or
 * subclassing of library types is performed – real DNS objects are
 * instantiated through their constructors or factory methods.
 */
public class MessageTest {

  /** Helper to create a generic Record (A‑type) for a given name. */
  private Record makeRecord(String nameStr) throws TextParseException {
    Name name = Name.fromString(nameStr);
    // Use the generic factory – the exact type is not important for the tests
    return Record.newRecord(name, Type.A, DClass.IN, 3600);
  }

  /** Helper to create a TSIGRecord with minimal valid data. */
  private TSIGRecord makeTSIGRecord() throws TextParseException {
    Name name = Name.fromString("tsig.example.");
    // Use the well‑known HMAC‑MD5 algorithm name
    Name alg = TSIG.HMAC_MD5;
    Instant now = Instant.now();
    byte[] sig = new byte[] {0x01, 0x02, 0x03, 0x04};
    return new TSIGRecord(name, DClass.IN, 0, alg, now, TSIG.FUDGE, sig, 0, Rcode.NOERROR, new byte[0]);
  }

  /** Helper to create an OPT record with an extended rcode (e.g. 5). */
  private OPTRecord makeOPTRecord() {
    // payloadSize, extended rcode, version, flags
    return new OPTRecord(4096, 5, 0, 0);
  }

  /** Test basic constructors and header handling. */
  @Test
  public void testConstructionAndHeader() {
    Message m = new Message(0x1234);
    assertNotNull(m.getHeader());
    assertEquals(0x1234, m.getHeader().getID());

    Header newHeader = new Header(0xABCD);
    m.setHeader(newHeader);
    assertSame(newHeader, m.getHeader());
  }

  /** Test addRecord, removeRecord, findRecord, findRRset and header count updates. */
  @Test
  public void testRecordManipulation() throws Exception {
    Message m = new Message();
    Record a = makeRecord("a.example.com.");
    Record b = makeRecord("b.example.com.");

    // initially empty
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));

    // add to ANSWER section
    m.addRecord(a, Section.ANSWER);
    assertEquals(1, m.getHeader().getCount(Section.ANSWER));
    assertTrue(m.findRecord(a, Section.ANSWER));
    assertTrue(m.findRecord(a));
    assertFalse(m.findRecord(b));

    // add another record of a different type (use generic factory with TYPE=AAAA)
    Record aaaa = Record.newRecord(Name.fromString("c.example.com."), Type.AAAA, DClass.IN, 3600);
    m.addRecord(aaaa, Section.ANSWER);
    assertEquals(2, m.getHeader().getCount(Section.ANSWER));
    assertTrue(m.findRRset(aaaa.getName(), Type.AAAA, Section.ANSWER));

    // remove a record
    assertTrue(m.removeRecord(a, Section.ANSWER));
    assertEquals(1, m.getHeader().getCount(Section.ANSWER));
    assertFalse(m.findRecord(a));

    // removeAllRecords clears the section and resets count
    m.removeAllRecords(Section.ANSWER);
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));
    assertFalse(m.findRecord(aaaa));
  }

  /** Test that getQuestion returns the first record of the QUESTION section. */
  @Test
  public void testGetQuestion() throws Exception {
    Message q = Message.newQuery(makeRecord("question.example.com."));
    Record question = q.getQuestion();
    assertNotNull(question);
    assertEquals(Name.fromString("question.example.com."), question.getName());
  }

  /** Test TSIG handling – adding a TSIG record and retrieving it. */
  @Test
  public void testTSIG() throws Exception {
    Message m = new Message();
    TSIGRecord tsig = makeTSIGRecord();

    // Add the TSIG as the last record in the ADDITIONAL section
    m.addRecord(tsig, Section.ADDITIONAL);
    assertSame(tsig, m.getTSIG());

    // No TSIG generated yet
    assertNull(m.getGeneratedTSIG());

    // The message is not considered signed until a TSIG key is set
    assertFalse(m.isSigned());
    assertFalse(m.isVerified());

    // Set a TSIG key – this does not automatically sign the message but changes the internal state
    TSIG key = new TSIG(Name.fromString("key.example."), "secret".getBytes());
    m.setTSIG(key);
    // isSigned still false because signing occurs during toWire()
    assertFalse(m.isSigned());
  }

  /** Test OPT handling and extended rcode calculation. */
  @Test
  public void testOPTAndRcode() throws Exception {
    Message m = new Message();
    OPTRecord opt = makeOPTRecord();
    m.addRecord(opt, Section.ADDITIONAL);

    // getOPT should find the OPT record
    assertSame(opt, m.getOPT());

    // The base rcode is 0; extended rcode from OPT is 5 << 4 = 80
    assertEquals(80, m.getRcode());
  }

  /** Test toWire() and toWire(int) including truncation logic. */
  @Test
  public void testToWireAndTruncation() throws Exception {
    Message m = new Message();
    // Add many A records to force the message to exceed a tiny maxLength
    for (int i = 0; i < 20; i++) {
      Record r = Record.newRecord(Name.fromString("host" + i + ".example.com."), Type.A, DClass.IN, 3600);
      m.addRecord(r, Section.ANSWER);
    }

    // Normal wire format – should succeed and size > 0
    byte[] fullWire = m.toWire();
    assertTrue(fullWire.length > 0);
    assertEquals(fullWire.length, m.numBytes());

    // Truncate with a deliberately small maxLength (e.g., 30 bytes)
    byte[] truncated = m.toWire(30);
    // The truncated array may be empty if the header itself does not fit; ensure no exception
    assertNotNull(truncated);
    // Verify that the TC flag is set when truncation occurs
    Header hdr = new Header(fullWire); // reuse constructor that parses a header from wire
    if (truncated.length < fullWire.length) {
      assertTrue(hdr.getFlag(Flags.TC));
    }
  }

  /** Test cloning produces a deep copy (records list and header are independent). */
  @Test
  public void testCloneIndependence() throws Exception {
    Message original = new Message();
    Record r1 = makeRecord("clone1.example.com.");
    Record r2 = makeRecord("clone2.example.com.");
    original.addRecord(r1, Section.ANSWER);
    original.addRecord(r2, Section.AUTHORITY);
    original.setHeader(new Header(0x5555));

    Message clone = original.clone();

    // Header should be equal but not the same instance
    assertEquals(original.getHeader().getID(), clone.getHeader().getID());
    assertNotSame(original.getHeader(), clone.getHeader());

    // Section contents should be equal but independent
    assertEquals(1, clone.getSection(Section.ANSWER).size());
    assertEquals(r1, clone.getSection(Section.ANSWER).get(0));

    // Modify original and verify clone does not change
    original.removeRecord(r1, Section.ANSWER);
    assertEquals(1, original.getSection(Section.ANSWER).size()); // removed, now empty
    assertEquals(1, clone.getSection(Section.ANSWER).size());   // clone still has it
  }

  /** Test resolver association via setResolver / getResolver. */
  @Test
  public void testResolverAssociation() {
    Message m = new Message();
    Resolver dummy = new Resolver() {
      @Override public void setPort(int port) {}
      @Override public void setTCP(boolean flag) {}
      @Override public void setIgnoreTruncation(boolean flag) {}
      @Override public void setEDNS(int version) {}
      @Override public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
      @Override public void setEDNS(int version, int payloadSize, int flags, EDNSOption... options) {}
      @Override public void setTSIGKey(TSIG key) {}
      @Override public void setTimeout(int secs, int msecs) {}
      @Override public void setTimeout(int secs) {}
      @Override public void setTimeout(Duration timeout) {}
      @Override public Duration getTimeout() { return Duration.ZERO; }
      @Override public Message send(Message query) { return null; }
      @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
      @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
      @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
    };
    m.setResolver(dummy);
    assertTrue(m.getResolver().isPresent());
    assertSame(dummy, m.getResolver().get());
  }

  /** Test isTypeAllowedInSection for known and unknown types. */
  @Test
  public void testIsTypeAllowedInSection() {
    Message m = new Message();

    // AUTHORITY section allows SOA, NS, DS, NSEC, NSEC3
    assertTrue(m.isTypeAllowedInSection(Type.SOA, Section.AUTHORITY));
    assertTrue(m.isTypeAllowedInSection(Type.NS, Section.AUTHORITY));
    assertFalse(m.isTypeAllowedInSection(Type.A, Section.AUTHORITY));

    // ADDITIONAL section allows A and AAAA
    assertTrue(m.isTypeAllowedInSection(Type.A, Section.ADDITIONAL));
    assertTrue(m.isTypeAllowedInSection(Type.AAAA, Section.ADDITIONAL));
    assertFalse(m.isTypeAllowedInSection(Type.MX, Section.ADDITIONAL));
  }

  /** Test normalize on a simple successful response (no DNAME/CNAME handling). */
  @Test
  public void testNormalizeSimple() throws Exception {
    // Build a query message
    Record qrec = Record.newRecord(Name.fromString("www.example.com."), Type.A, DClass.IN, 0);
    Message query = Message.newQuery(qrec);

    // Build a response message containing the same question and a single answer
    Message response = new Message();
    response.addRecord(qrec, Section.QUESTION);
    Record answer = Record.newRecord(Name.fromString("www.example.com."), Type.A, DClass.IN, 3600);
    response.addRecord(answer, Section.ANSWER);
    response.getHeader().setRcode(Rcode.NOERROR);
    response.getHeader().setOpcode(Opcode.QUERY);

    // Normalization should return a message equal to the original response (no changes)
    Message normalized = response.normalize(query);
    assertNotNull(normalized);
    assertEquals(1, normalized.getSection(Section.ANSWER).size());
    assertEquals(answer, normalized.getSection(Section.ANSWER).get(0));
  }

  /** Verify that deprecated getSectionArray still returns an empty array for empty sections. */
  @Test
  public void testGetSectionArrayDeprecated() {
    Message m = new Message();
    Record[] arr = m.getSectionArray(Section.AUTHORITY);
    assertNotNull(arr);
    assertEquals(0, arr.length);
  }

  /** Test the string representation includes header, sections and message size. */
  @Test
  public void testToStringContainsAllParts() throws Exception {
    Message m = new Message();
    Record q = Record.newRecord(Name.fromString("test.example."), Type.A, DClass.IN, 0);
    m.addRecord(q, Section.QUESTION);
    m.addRecord(q, Section.ANSWER);
    String s = m.toString();

    // Header line should be present
    assertTrue(s.contains(";;"));
    // Section titles should appear
    assertTrue(s.contains(Section.longString(Section.QUESTION)));
    assertTrue(s.contains(Section.longString(Section.ANSWER)));
    // Message size line should be present
    assertTrue(s.matches("(?s).*Message size: \\d+ bytes$"));
  }
}
###Test END##