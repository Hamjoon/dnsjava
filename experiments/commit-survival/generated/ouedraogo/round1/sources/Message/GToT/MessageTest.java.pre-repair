/**
 * Comprehensive JUnit‑4 test suite for {@link org.xbill.DNS.Message}.
 *
 * The tests are organised as the result of a “three‑expert” review:
 *   1. List all public methods of {@code Message}.
 *   2. Provide a basic test for the normal behaviour of each method.
 *   3. Identify edge‑cases / exceptional situations and add dedicated tests.
 *   4. Merge everything into a single test class.
 *
 * Only the public API of the DNS Java library is used – no mocking or subclassing of
 * library classes.  All objects are created through their real constructors or
 * factory methods.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

/**
 * JUnit‑4 test class for {@link Message}.
 */
public class MessageTest {

  /* ------------------------------------------------------------------------
   * Helper methods – create simple DNS objects used throughout the tests.
   * ---------------------------------------------------------------------- */

  /** Creates a Name “example.com.” (throws if the name is malformed). */
  private static Name name(String s) throws IOException {
    return new Name(s);
  }

  /** Creates a generic A‑record (type = A, class = IN) with a TTL of 3600 seconds. */
  private static Record aRecord(Name n) {
    return Record.newRecord(n, Type.A, DClass.IN, 3600);
  }

  /** Minimal {@link Resolver} implementation used only for set/get tests. */
  private static class DummyResolver implements Resolver {
    @Override public void setPort(int port) {}
    @Override public void setTCP(boolean flag) {}
    @Override public void setIgnoreTruncation(boolean flag) {}
    @Override public void setEDNS(int version) {}
    @Override public void setEDNS(int version, int payloadSize, int flags,
                                 List<EDNSOption> options) {}
    @Override public void setEDNS(int version, int payloadSize, int flags,
                                 EDNSOption... options) {}
    @Override public void setTSIGKey(TSIG key) {}
    @Override public void setTimeout(int secs, int msecs) {}
    @Override public void setTimeout(int secs) {}
    @Override public void setTimeout(java.time.Duration timeout) {}
    @Override public java.time.Duration getTimeout() { return null; }
    @Override public Message send(Message query) throws IOException { return null; }
    @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
    @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query,
                                                                         java.util.concurrent.Executor executor) { return null; }
    @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
  }

  /* ------------------------------------------------------------------------
   * 1.  Public constructors / static factories
   * ---------------------------------------------------------------------- */

  @Test
  public void testConstructorWithId() throws Exception {
    Message m = new Message(0xABCD);
    assertEquals(0xABCD, m.getHeader().getID());
  }

  @Test
  public void testDefaultConstructor() throws Exception {
    Message m = new Message();
    // ID is random – just ensure we got a Message and a Header.
    assertNotNull(m.getHeader());
  }

  @Test
  public void testNewQueryFactory() throws Exception {
    Name n = name("example.com.");
    Record q = Record.newRecord(n, Type.A, DClass.IN, 0);
    Message query = Message.newQuery(q);
    assertEquals(Opcode.QUERY, query.getHeader().getOpcode());
    assertTrue(query.getHeader().getFlag(Flags.RD));
    assertEquals(q, query.getQuestion());
  }

  @Test
  public void testNewUpdateFactory() throws Exception {
    Name zone = name("example.com.");
    Message upd = Message.newUpdate(zone);
    // The concrete class is Update (a subclass of Message); we only check type.
    assertTrue(upd instanceof Message);
    // Update messages have opcode UPDATE.
    assertEquals(Opcode.UPDATE, upd.getHeader().getOpcode());
  }

  /* ------------------------------------------------------------------------
   * 2.  Basic functional tests for each public method
   * ---------------------------------------------------------------------- */

  @Test
  public void testSetAndGetHeader() throws Exception {
    Message m = new Message();
    Header h = new Header(0x1234);
    m.setHeader(h);
    assertSame(h, m.getHeader());
  }

  @Test
  public void testAddFindAndGetQuestion() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record r = aRecord(n);
    m.addRecord(r, Section.QUESTION);
    assertTrue(m.findRecord(r, Section.QUESTION));
    assertEquals(r, m.getQuestion());
  }

  @Test
  public void testRemoveRecord() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record r = aRecord(n);
    m.addRecord(r, Section.ANSWER);
    assertTrue(m.removeRecord(r, Section.ANSWER));
    assertFalse(m.findRecord(r, Section.ANSWER));
    // Header count for ANSWER must be decreased to zero.
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));
  }

  @Test
  public void testRemoveAllRecords() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record r1 = aRecord(n);
    Record r2 = aRecord(n);
    m.addRecord(r1, Section.AUTHORITY);
    m.addRecord(r2, Section.AUTHORITY);
    assertEquals(2, m.getHeader().getCount(Section.AUTHORITY));
    m.removeAllRecords(Section.AUTHORITY);
    assertEquals(0, m.getHeader().getCount(Section.AUTHORITY));
    assertFalse(m.findRecord(r1, Section.AUTHORITY));
    assertTrue(m.getSection(Section.AUTHORITY).isEmpty());
  }

  @Test
  public void testFindRecordAcrossSections() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record q = Record.newRecord(n, Type.A, DClass.IN, 0);
    Record a = aRecord(n);
    m.addRecord(q, Section.QUESTION);
    m.addRecord(a, Section.ANSWER);
    assertTrue(m.findRecord(q));          // found in QUESTION
    assertTrue(m.findRecord(a));          // found in ANSWER
  }

  @Test
  public void testFindRRset() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record a1 = aRecord(n);
    Record a2 = aRecord(n);
    m.addRecord(a1, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);
    assertTrue(m.findRRset(n, Type.A, Section.ANSWER));
    assertTrue(m.findRRset(n, Type.A));
  }

  @Test
  public void testGetTSIGWhenAbsent() throws Exception {
    Message m = new Message();
    assertNull(m.getTSIG());
  }

  @Test
  public void testGetOPTWhenAbsent() throws Exception {
    Message m = new Message();
    assertNull(m.getOPT());
  }

  @Test
  public void testGetRcodeDefault() throws Exception {
    Message m = new Message();
    assertEquals(0, m.getRcode());
  }

  @Test
  public void testGetSectionAndArray() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record r = aRecord(n);
    m.addRecord(r, Section.ADDITIONAL);
    List<Record> list = m.getSection(Section.ADDITIONAL);
    assertEquals(1, list.size());
    assertEquals(r, list.get(0));

    Record[] arr = m.getSectionArray(Section.ADDITIONAL);
    assertEquals(1, arr.length);
    assertEquals(r, arr[0]);
  }

  @Test
  public void testGetSectionRRsets() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record a = aRecord(n);
    Record txt = Record.newRecord(n, Type.TXT, DClass.IN, 3600);
    m.addRecord(a, Section.ANSWER);
    m.addRecord(txt, Section.ANSWER);
    List<RRset> sets = m.getSectionRRsets(Section.ANSWER);
    // Two different types => two RRsets
    assertEquals(2, sets.size());
  }

  @Test
  public void testToWireAndNumBytes() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record q = Record.newRecord(n, Type.A, DClass.IN, 0);
    m.addRecord(q, Section.QUESTION);
    byte[] wire = m.toWire();
    assertNotNull(wire);
    assertTrue(wire.length > 0);
    assertEquals(wire.length, m.numBytes());
  }

  @Test
  public void testToWireWithMaxLengthTruncation() throws Exception {
    Message m = new Message();
    // Fill the message with many records to force truncation.
    Name n = name("example.com.");
    for (int i = 0; i < 100; i++) {
      Record r = Record.newRecord(name("host" + i + ".example.com."), Type.A, DClass.IN, 3600);
      m.addRecord(r, Section.ANSWER);
    }
    // Force a very small maxLength – smaller than a DNS header (12 bytes)
    byte[] wire = m.toWire(10);
    // According to the implementation, an empty array is returned when it cannot render.
    assertEquals(0, wire.length);
  }

  @Test
  public void testSetTSIGAndSignedState() throws Exception {
    Message m = new Message();
    // Use a simple HMAC‑MD5 TSIG key (algorithm name is a constant in TSIG)
    TSIG tsig = new TSIG(TSIG.HMAC_MD5, name("mykey.example.com."), "base64key==");
    m.setTSIG(tsig);
    // The TSIG state is not changed until the message is signed (toWire).
    // After toWire() it should be considered signed.
    m.toWire(); // triggers TSIG generation if a key is present
    assertTrue(m.isSigned());
    // Verification flag is false because we have not verified a response.
    assertFalse(m.isVerified());
  }

  @Test
  public void testSectionToStringInvalidIndex() throws Exception {
    Message m = new Message();
    assertNull(m.sectionToString(5)); // index > 3 returns null
  }

  @Test
  public void testToStringContainsHeaderAndSections() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record q = Record.newRecord(n, Type.A, DClass.IN, 0);
    m.addRecord(q, Section.QUESTION);
    String s = m.toString();
    assertNotNull(s);
    // Header line must be present
    assertTrue(s.contains("ID"));
    // QUESTION section heading must be present
    assertTrue(s.contains(";; QUESTION"));
    // The question record string must appear
    assertTrue(s.contains("example.com."));
  }

  @Test
  public void testCloneCreatesDeepCopy() throws Exception {
    Message original = new Message();
    Name n = name("example.com.");
    Record r = aRecord(n);
    original.addRecord(r, Section.ANSWER);

    Message copy = original.clone();
    // Verify that the cloned message has the same content initially.
    assertEquals(1, copy.getHeader().getCount(Section.ANSWER));
    assertTrue(copy.findRecord(r, Section.ANSWER));

    // Modify the original – the clone must stay unchanged.
    Record r2 = aRecord(name("other.example.com."));
    original.addRecord(r2, Section.ANSWER);
    assertEquals(2, original.getHeader().getCount(Section.ANSWER));
    assertEquals(1, copy.getHeader().getCount(Section.ANSWER));
    assertFalse(copy.findRecord(r2, Section.ANSWER));
  }

  @Test
  public void testSetAndGetResolver() throws Exception {
    Message m = new Message();
    DummyResolver resolver = new DummyResolver();
    m.setResolver(resolver);
    Optional<Resolver> opt = m.getResolver();
    assertTrue(opt.isPresent());
    assertSame(resolver, opt.get());
  }

  /* ------------------------------------------------------------------------
   * 3.  Edge‑case / exception tests
   * ---------------------------------------------------------------------- */

  @Test(expected = IOException.class)
  public void testMessageFromInvalidWireThrows() throws Exception {
    // Corrupt wire data – header expects at least 12 bytes.
    byte[] bad = new byte[] {0, 1, 2, 3};
    new Message(bad); // should raise IOException (via DNSInput)
  }

  @Test
  public void testAddRecordToUninitialisedSectionCreatesLinkedList() throws Exception {
    Message m = new Message();
    // The internal array entry for SECTION.ANSWER is initially null.
    assertNull(m.getSection(Section.ANSWER));
    Name n = name("example.com.");
    Record r = aRecord(n);
    m.addRecord(r, Section.ANSWER);
    // After adding, the section must contain the record.
    assertEquals(1, m.getSection(Section.ANSWER).size());
  }

  @Test
  public void testRemoveRecordFromEmptySectionReturnsFalse() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    Record r = aRecord(n);
    assertFalse(m.removeRecord(r, Section.AUTHORITY));
  }

  @Test
  public void testGetSectionArrayWhenSectionIsEmptyReturnsEmptyArray() throws Exception {
    Message m = new Message();
    Record[] arr = m.getSectionArray(Section.AUTHORITY);
    assertNotNull(arr);
    assertEquals(0, arr.length);
  }

  @Test
  public void testGetOPTWhenPresent() throws Exception {
    Message m = new Message();
    // Create a simple OPT record (EDNS payload 4096, no options).
    OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
    m.addRecord(opt, Section.ADDITIONAL);
    assertSame(opt, m.getOPT());
  }

  @Test
  public void testGetRcodeWithExtendedRcodeFromOPT() throws Exception {
    Message m = new Message();
    // Normal header rcode = 2 (SERVFAIL)
    m.getHeader().setRcode(2);
    // Add an OPT with extended rcode = 3 (i.e. 0x30 after shift)
    OPTRecord opt = new OPTRecord(4096, 3, 0, 0);
    m.addRecord(opt, Section.ADDITIONAL);
    // Expected: 2 + (3 << 4) = 2 + 48 = 50
    assertEquals(50, m.getRcode());
  }

  @Test
  public void testSectionToWireTruncationStopsAtMaxLength() throws Exception {
    Message m = new Message();
    Name n = name("example.com.");
    // Add many A records to overflow a small maxLength.
    for (int i = 0; i < 20; i++) {
      Record r = Record.newRecord(name("host" + i + ".example.com."), Type.A, DClass.IN, 3600);
      m.addRecord(r, Section.ANSWER);
    }
    // Use a maxLength that is larger than the header but smaller than the full message.
    byte[] wire = m.toWire(200);
    assertTrue(wire.length <= 200);
    // The TC (truncation) flag should be set – verify via header flags.
    assertTrue(m.getHeader().getFlag(Flags.TC));
  }

  /* ------------------------------------------------------------------------
   * End of test suite
   * ---------------------------------------------------------------------- */
}
