###Test START##
/*
 * JUnit 4 test cases for org.xbill.DNS.Message
 *
 * The tests cover:
 *  - Public method signatures
 *  - Basic functional verification
 *  - Edge‑case handling and exception scenarios
 *
 * NOTE: The test suite uses only the public API that is visible in the
 *       provided source code and the abbreviated dependency signatures.
 */

package org.xbill.DNS;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Test class for {@link Message}.
 */
public class MessageTest {

  private Name name;
  private Record aRecord;
  private Record aRecord2;
  private OPTRecord optRecord;

  @Before
  public void setUp() throws Exception {
    name = Name.fromString("example.com.");
    aRecord = Record.newRecord(name, Type.A, DClass.IN, 3600);
    aRecord2 = Record.newRecord(name, Type.A, DClass.IN, 3600);
    optRecord = new OPTRecord(4096, 0, 0, 0);
  }

  /* -----------------------------------------------------------------------
   *  Public method list (extracted from Message)
   * -----------------------------------------------------------------------
   *
   * public static final int MAXLENGTH
   * public Message(int id)
   * public Message()
   * public static Message newQuery(Record r)
   * public static Message newUpdate(Name zone)
   * public Message(byte[] b) throws IOException
   * public Message(ByteBuffer byteBuffer) throws IOException
   * public void setHeader(Header h)
   * public Header getHeader()
   * public void addRecord(Record r, int section)
   * public boolean removeRecord(Record r, int section)
   * public void removeAllRecords(int section)
   * public boolean findRecord(Record r, int section)
   * public boolean findRecord(Record r)
   * public boolean findRRset(Name name, int type, int section)
   * public boolean findRRset(Name name, int type)
   * public Record getQuestion()
   * public TSIGRecord getTSIG()
   * public boolean isSigned()
   * public boolean isVerified()
   * public OPTRecord getOPT()
   * public int getRcode()
   * @Deprecated public Record[] getSectionArray(int section)
   * public List<Record> getSection(int section)
   * public byte[] toWire()
   * public byte[] toWire(int maxLength)
   * public void setTSIG(TSIG key)
   * public void setTSIG(TSIG key, int error, TSIGRecord querytsig)
   * public int numBytes()
   * public String sectionToString(int i)
   * public String toString()
   * public Message clone()
   * public void setResolver(Resolver resolver)
   * public Optional<Resolver> getResolver()
   */

  /* -----------------------------------------------------------------------
   *  Basic functional tests
   * ----------------------------------------------------------------------- */

  @Test
  public void testConstructorWithId() {
    Message m = new Message(0x1234);
    assertEquals(0x1234, m.getHeader().getID());
  }

  @Test
  public void testDefaultConstructorCreatesRandomId() {
    Message m1 = new Message();
    Message m2 = new Message();
    // IDs are random; they may collide but extremely unlikely.
    // The test only verifies that an ID is present and within 0‑65535.
    int id1 = m1.getHeader().getID();
    int id2 = m2.getHeader().getID();
    assertTrue(id1 >= 0 && id1 <= 0xFFFF);
    assertTrue(id2 >= 0 && id2 <= 0xFFFF);
  }

  @Test
  public void testNewQueryCreatesCorrectHeaderAndQuestion() {
    Message q = Message.newQuery(aRecord);
    Header h = q.getHeader();
    assertEquals(Opcode.QUERY, h.getOpcode());
    assertTrue(h.getFlag(Flags.RD));
    Record question = q.getQuestion();
    assertNotNull(question);
    assertEquals(aRecord, question);
  }

  @Test
  public void testAddAndFindRecord() {
    Message m = new Message();
    assertFalse(m.findRecord(aRecord, Section.ANSWER));
    m.addRecord(aRecord, Section.ANSWER);
    assertTrue(m.findRecord(aRecord, Section.ANSWER));
    assertTrue(m.findRecord(aRecord)); // any section
  }

  @Test
  public void testRemoveRecord() {
    Message m = new Message();
    m.addRecord(aRecord, Section.AUTHORITY);
    assertTrue(m.removeRecord(aRecord, Section.AUTHORITY));
    assertFalse(m.findRecord(aRecord, Section.AUTHORITY));
    // Removing a non‑existing record should return false
    assertFalse(m.removeRecord(aRecord, Section.AUTHORITY));
  }

  @Test
  public void testRemoveAllRecords() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ADDITIONAL);
    m.removeAllRecords(Section.ADDITIONAL);
    assertFalse(m.findRecord(aRecord, Section.ADDITIONAL));
    assertEquals(0, m.getHeader().getCount(Section.ADDITIONAL));
  }

  @Test
  public void testFindRRset() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    m.addRecord(aRecord2, Section.ANSWER); // same name/type
    assertTrue(m.findRRset(name, Type.A, Section.ANSWER));
    assertTrue(m.findRRset(name, Type.A));
  }

  @Test
  public void testGetOPT() {
    Message m = new Message();
    m.addRecord(optRecord, Section.ADDITIONAL);
    OPTRecord retrieved = m.getOPT();
    assertNotNull(retrieved);
    assertEquals(optRecord, retrieved);
  }

  @Test
  public void testGetRcodeWithExtendedRcode() {
    Message m = new Message();
    // Normal rcode = 0 (NOERROR)
    assertEquals(0, m.getRcode());
    // Add an OPT record with extended rcode = 2 (SERVFAIL extended)
    OPTRecord opt = new OPTRecord(4096, 2, 0, 0);
    m.addRecord(opt, Section.ADDITIONAL);
    // Expected: base rcode (0) + (2 << 4) = 32
    assertEquals(32, m.getRcode());
  }

  @Test
  public void testToWireAndSize() {
    Message m = new Message();
    m.addRecord(aRecord, Section.QUESTION);
    byte[] wire = m.toWire();
    assertNotNull(wire);
    assertTrue(wire.length > 0);
    assertEquals(wire.length, m.numBytes());
  }

  @Test
  public void testToWireWithMaxLengthTruncation() {
    Message m = new Message();
    // Add many records to force truncation
    for (int i = 0; i < 50; i++) {
      m.addRecord(aRecord, Section.ANSWER);
    }
    // Use a very small maxLength – smaller than the DNS header (12 bytes)
    byte[] shortWire = m.toWire(10);
    assertEquals(0, shortWire.length);
  }

  @Test
  public void testSetAndGetResolver() {
    Message m = new Message();
    Resolver resolver = new Resolver() {
      public void setPort(int port) {}
      public void setTCP(boolean flag) {}
      public void setIgnoreTruncation(boolean flag) {}
      public void setEDNS(int version) {}
      public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
      public void setEDNS(int version, int payloadSize, int flags, EDNSOption... options) {}
      public void setTSIGKey(TSIG key) {}
      public void setTimeout(Duration timeout) {}
      public void setTimeout(int secs, int msecs) {}
      public void setTimeout(int secs) {}
      public Message send(Message query) throws IOException { return null; }
      public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
      public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
      public Object sendAsync(Message query, ResolverListener listener) { return null; }
    };
    m.setResolver(resolver);
    Optional<Resolver> opt = m.getResolver();
    assertTrue(opt.isPresent());
    assertEquals(resolver, opt.get());
  }

  @Test
  public void testSectionToStringInvalidIndex() {
    Message m = new Message();
    assertNull(m.sectionToString(5)); // index > 3 should return null
  }

  @Test
  public void testCloneCreatesDeepCopy() {
    Message original = new Message();
    original.addRecord(aRecord, Section.ANSWER);
    original.setTSIG(new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("test."), "abcd"));
    Message copy = original.clone();

    // Header should be equal but not the same instance
    assertEquals(original.getHeader().getID(), copy.getHeader().getID());
    assertNotSame(original.getHeader(), copy.getHeader());

    // Sections must be deep‑copied
    assertTrue(copy.findRecord(aRecord, Section.ANSWER));
    copy.removeRecord(aRecord, Section.ANSWER);
    assertTrue(original.findRecord(aRecord, Section.ANSWER));
    assertFalse(copy.findRecord(aRecord, Section.ANSWER));

    // TSIG related fields are also cloned (generatedTsig and querytsig are null here,
    // but the tsigkey reference is copied)
    assertFalse(copy.isSigned()); // default state after clone
  }

  @Test
  public void testGetSectionArrayDeprecated() {
    Message m = new Message();
    Record[] empty = m.getSectionArray(Section.ANSWER);
    assertNotNull(empty);
    assertEquals(0, empty.length);
    m.addRecord(aRecord, Section.ANSWER);
    Record[] arr = m.getSectionArray(Section.ANSWER);
    assertEquals(1, arr.length);
    assertEquals(aRecord, arr[0]);
  }

  /* -----------------------------------------------------------------------
   *  Edge‑case / exception tests
   * ----------------------------------------------------------------------- */

  @Test(expected = IOException.class)
  public void testMessageFromMalformedByteArrayThrowsIOException() throws Exception {
    // An empty byte array cannot be parsed into a valid DNS message.
    new Message(new byte[0]);
  }

  @Test
  public void testGetTSIGWhenAbsentReturnsNull() {
    Message m = new Message();
    assertNull(m.getTSIG());
  }

  @Test
  public void testIsSignedAndIsVerifiedDefault() {
    Message m = new Message();
    assertFalse(m.isSigned());
    assertFalse(m.isVerified());
  }

  @Test
  public void testSetTSIGAndSignedStateAfterWire() throws Exception {
    Message m = new Message();
    TSIG key = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("test."), "abcd");
    m.setTSIG(key);
    // Before rendering, isSigned() is false because the message hasn't been signed yet.
    assertFalse(m.isSigned());
    // Rendering with a sufficient maxLength signs the message.
    byte[] wire = m.toWire(Message.MAXLENGTH);
    assertTrue(m.isSigned()); // after toWire the internal state should be TSIG_SIGNED
  }

  @Test
  public void testSectionToStringForEmptySection() {
    Message m = new Message();
    // No records added – should return an empty string (not null)
    String s = m.sectionToString(Section.ANSWER);
    assertNotNull(s);
    assertEquals("", s.trim());
  }

  @Test
  public void testGetHeaderAndSetHeader() {
    Message m = new Message();
    Header originalHeader = m.getHeader();
    Header newHeader = new Header(0x9999);
    m.setHeader(newHeader);
    assertEquals(newHeader, m.getHeader());
    assertNotEquals(originalHeader, m.getHeader());
  }

  @Test
  public void testFindRecordAcrossAllSections() {
    Message m = new Message();
    m.addRecord(aRecord, Section.QUESTION);
    m.addRecord(aRecord, Section.ANSWER);
    assertTrue(m.findRecord(aRecord));
  }

  @Test
  public void testFindRecordWhenSectionIsNull() {
    Message m = new Message();
    // No records added; sections are null internally.
    assertFalse(m.findRecord(aRecord, Section.AUTHORITY));
  }

  @Test
  public void testRemoveAllRecordsResetsHeaderCount() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ADDITIONAL);
    assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));
    m.removeAllRecords(Section.ADDITIONAL);
    assertEquals(0, m.getHeader().getCount(Section.ADDITIONAL));
    assertNull(m.getSection(Section.ADDITIONAL));
  }
}
###Test END##