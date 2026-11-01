package org.xbill.DNS.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.xbill.DNS.*;

public class MessageTest {

  /* -------------------------------------------------------------
   * Helper methods
   * ------------------------------------------------------------- */
  private static Record createARecord(String name) throws TextParseException {
    Name n = Name.fromString(name);
    // A record (type 1), IN class (1), ttl 3600
    return Record.newRecord(n, Type.A, DClass.IN, 3600);
  }

  private static Record createCNAMERecord(String name, String target) throws TextParseException {
    Name n = Name.fromString(name);
    Name t = Name.fromString(target);
    // CNAME type = 5
    return Record.newRecord(n, Type.CNAME, DClass.IN, 3600, 0, new byte[0]) {
      @Override
      protected void rrFromWire(DNSInput in) {}
      @Override
      protected void rrToWire(DNSOutput out, Compression c, boolean canonical) {}
      @Override
      protected String rrToString() {
        return target;
      }
    };
  }

  /* -------------------------------------------------------------
   * 1. Constructors and static factories
   * ------------------------------------------------------------- */
  @Test
  public void testDefaultConstructorAndHeader() {
    Message m = new Message();
    assertNotNull(m.getHeader());
    // default ID should be non‑negative
    assertTrue(m.getHeader().getID() >= 0);
  }

  @Test
  public void testIdConstructor() {
    Message m = new Message(0x1234);
    assertEquals(0x1234, m.getHeader().getID());
  }

  @Test
  public void testNewQueryFactory() throws TextParseException {
    Record q = createARecord("example.com.");
    Message query = Message.newQuery(q);
    assertEquals(Opcode.QUERY, query.getHeader().getOpcode());
    assertTrue(query.getHeader().getFlag(Flags.RD));
    assertEquals(q, query.getQuestion());
  }

  @Test
  public void testNewUpdateFactory() throws TextParseException {
    Name zone = Name.fromString("example.com.");
    Message upd = Message.newUpdate(zone);
    assertNotNull(upd);
    // the returned object is a subclass of Message (Update)
    assertTrue(upd instanceof Message);
  }

  @Test
  public void testByteArrayConstructor() throws IOException, TextParseException {
    // create a simple message, turn it into wire, then re‑parse
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.QUESTION);
    byte[] wire = m.toWire();

    Message parsed = new Message(wire);
    assertEquals(m.getHeader().getID(), parsed.getHeader().getID());
    assertEquals(a, parsed.getQuestion());
  }

  @Test
  public void testByteBufferConstructor() throws IOException, TextParseException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.QUESTION);
    byte[] wire = m.toWire();

    Message parsed = new Message(ByteBuffer.wrap(wire));
    assertEquals(m.getHeader().getID(), parsed.getHeader().getID());
    assertEquals(a, parsed.getQuestion());
  }

  /* -------------------------------------------------------------
   * 2. Header handling
   * ------------------------------------------------------------- */
  @Test
  public void testSetAndGetHeader() {
    Message m = new Message();
    Header h = new Header(0x4321);
    m.setHeader(h);
    assertSame(h, m.getHeader());
  }

  /* -------------------------------------------------------------
   * 3. Record manipulation (add / remove / find)
   * ------------------------------------------------------------- */
  @Test
  public void testAddAndFindRecord() throws TextParseException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    assertTrue(m.findRecord(a, Section.ANSWER));
    assertTrue(m.findRecord(a)); // any section
  }

  @Test
  public void testRemoveRecord() throws TextParseException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.AUTHORITY);
    assertTrue(m.removeRecord(a, Section.AUTHORITY));
    assertFalse(m.findRecord(a, Section.AUTHORITY));
  }

  @Test
  public void testRemoveAllRecords() throws TextParseException {
    Message m = new Message();
    Record a1 = createARecord("example.com.");
    Record a2 = createARecord("example.org.");
    m.addRecord(a1, Section.ADDITIONAL);
    m.addRecord(a2, Section.ADDITIONAL);
    m.removeAllRecords(Section.ADDITIONAL);
    assertFalse(m.findRecord(a1, Section.ADDITIONAL));
    assertEquals(0, m.getHeader().getCount(Section.ADDITIONAL));
  }

  @Test
  public void testFindRRset() throws TextParseException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    Record a2 = createARecord("example.com."); // same name/type, different instance
    m.addRecord(a, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);
    assertTrue(m.findRRset(Name.fromString("example.com."), Type.A, Section.ANSWER));
    // different type should be false
    assertFalse(m.findRRset(Name.fromString("example.com."), Type.CNAME, Section.ANSWER));
  }

  @Test
  public void testFindRRsetAnySection() throws TextParseException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    assertTrue(m.findRRset(Name.fromString("example.com."), Type.A));
  }

  /* -------------------------------------------------------------
   * 4. Section getters
   * ------------------------------------------------------------- */
  @Test
  public void testGetQuestion() throws TextParseException {
    Message m = new Message();
    assertNull(m.getQuestion()); // empty
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.QUESTION);
    assertEquals(a, m.getQuestion());
  }

  @Test
  public void testGetTSIGWhenAbsent() {
    Message m = new Message();
    assertNull(m.getTSIG());
  }

  @Test
  public void testGetOPT() throws TextParseException {
    Message m = new Message();
    OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
    m.addRecord(opt, Section.ADDITIONAL);
    assertSame(opt, m.getOPT());
  }

  @Test
  public void testGetRcodeWithExtended() throws TextParseException {
    Message m = new Message();
    m.getHeader().setRcode(0);
    OPTRecord opt = new OPTRecord(4096, 1, 0, 0); // extended rcode = 1
    m.addRecord(opt, Section.ADDITIONAL);
    // 1 << 4 = 16
    assertEquals(16, m.getRcode());
  }

  @Test
  public void testGetSectionArrayDeprecated() throws TextParseException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    Record[] arr = m.getSectionArray(Section.ANSWER);
    assertEquals(1, arr.length);
    assertEquals(a, arr[0]);

    // empty section returns the shared empty array
    Record[] empty = m.getSectionArray(Section.AUTHORITY);
    assertSame(empty, Message.class.getDeclaredField("emptyRecordArray").get(null));
  }

  @Test
  public void testGetSectionUnmodifiable() throws TextParseException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    List<Record> list = m.getSection(Section.ANSWER);
    assertEquals(1, list.size());
    try {
      list.add(a);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException ignored) {
    }
  }

  @Test
  public void testGetSectionRRsetsGrouping() throws TextParseException {
    Message m = new Message();
    Record a1 = createARecord("example.com.");
    Record a2 = createARecord("example.com."); // same name/type
    Record cname = Record.newRecord(Name.fromString("example.com."), Type.CNAME, DClass.IN, 3600);
    m.addRecord(a1, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);
    m.addRecord(cname, Section.ANSWER);
    List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
    // should be 2 RRsets: one for A, one for CNAME
    assertEquals(2, rrsets.size());
    // find the A RRset and verify it contains both A records
    RRset aSet = null;
    for (RRset rs : rrsets) {
      if (rs.getType() == Type.A) {
        aSet = rs;
        break;
      }
    }
    assertNotNull(aSet);
    assertEquals(2, aSet.size());
  }

  /* -------------------------------------------------------------
   * 5. Wire format helpers
   * ------------------------------------------------------------- */
  @Test
  public void testToWireBasic() throws IOException, TextParseException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.QUESTION);
    byte[] wire = m.toWire();
    assertNotNull(wire);
    assertTrue(wire.length > Header.LENGTH);
  }

  @Test
  public void testToWireWithMaxLengthEnough() throws IOException, TextParseException {
    Message m = new Message();
    m.addRecord(createARecord("example.com."), Section.QUESTION);
    byte[] wire = m.toWire(65535);
    assertTrue(wire.length > 0);
  }

  @Test
  public void testToWireWithTooSmallLengthReturnsEmptyArray() throws IOException, TextParseException {
    Message m = new Message();
    m.addRecord(createARecord("example.com."), Section.QUESTION);
    // Header.LENGTH is the minimal size; use one byte less
    byte[] wire = m.toWire(Header.LENGTH - 1);
    assertEquals(0, wire.length);
  }

  @Test(expected = MessageSizeExceededException.class)
  public void testToWireTruncateFalseThrows() throws IOException, MessageSizeExceededException, TextParseException {
    Message m = new Message();
    // add many records to guarantee overflow
    for (int i = 0; i < 1000; i++) {
      m.addRecord(createARecord("host" + i + ".example.com."), Section.ANSWER);
    }
    // very small limit, truncation not allowed → exception
    m.toWire(100, false);
  }

  @Test
  public void testToWireTruncateTrueDoesNotThrow() throws IOException, MessageSizeExceededException, TextParseException {
    Message m = new Message();
    for (int i = 0; i < 100; i++) {
      m.addRecord(createARecord("host" + i + ".example.com."), Section.ANSWER);
    }
    byte[] wire = m.toWire(200, true);
    // May be empty, but no exception
    assertNotNull(wire);
  }

  /* -------------------------------------------------------------
   * 6. TSIG handling (basic state checks)
   * ------------------------------------------------------------- */
  @Test
  public void testTSIGStateDefaults() {
    Message m = new Message();
    // No TSIG set → not signed
    assertFalse(m.isSigned());
    assertFalse(m.isVerified());
  }

  @Test
  public void testSetTSIGDoesNotChangeSignedFlagImmediately() {
    Message m = new Message();
    // Use a dummy TSIG (algorithm HMAC_MD5, name "key.", key "abcd")
    TSIG tsig = new TSIG(Name.fromString("key."), "abcd".getBytes());
    m.setTSIG(tsig);
    // The message is still considered unsigned until it is rendered
    assertFalse(m.isSigned());
  }

  /* -------------------------------------------------------------
   * 7. Resolver association
   * ------------------------------------------------------------- */
  @Test
  public void testResolverSetAndGet() {
    Message m = new Message();
    Resolver resolver = new Resolver() {
      // Minimal anonymous implementation – all methods are no‑ops
      public void setPort(int port) {}
      public void setTCP(boolean flag) {}
      public void setIgnoreTruncation(boolean flag) {}
      public void setTSIGKey(TSIG key) {}
    };
    m.setResolver(resolver);
    Optional<Resolver> opt = m.getResolver();
    assertTrue(opt.isPresent());
    assertSame(resolver, opt.get());
  }

  /* -------------------------------------------------------------
   * 8. Clone behavior
   * ------------------------------------------------------------- */
  @Test
  public void testCloneCreatesDeepCopy() throws TextParseException {
    Message original = new Message();
    Record a = createARecord("example.com.");
    original.addRecord(a, Section.ANSWER);
    // modify header after clone to ensure independence
    original.getHeader().setRcode(Rcode.SERVFAIL);

    Message copy = original.clone();
    // Header should be equal but independent
    assertEquals(original.getHeader().getRcode(), copy.getHeader().getRcode());
    copy.getHeader().setRcode(Rcode.NOERROR);
    assertNotEquals(original.getHeader().getRcode(), copy.getHeader().getRcode());

    // Records list should be independent
    assertTrue(copy.findRecord(a, Section.ANSWER));
    copy.removeRecord(a, Section.ANSWER);
    assertTrue(original.findRecord(a, Section.ANSWER));
    assertFalse(copy.findRecord(a, Section.ANSWER));
  }

  /* -------------------------------------------------------------
   * 9. Normalization (basic sanity)
   * ------------------------------------------------------------- */
  @Test
  public void testNormalizeSimpleMessageReturnsCopy() throws TextParseException, IOException {
    Message query = Message.newQuery(createARecord("example.com."));
    Message response = new Message();
    response.addRecord(createARecord("example.com."), Section.ANSWER);
    response.addRecord(query.getQuestion(), Section.QUESTION);

    Message normalized = response.normalize(query);
    // For a simple answer the normalized message should be equivalent (same ID, same answer)
    assertEquals(response.getHeader().getID(), normalized.getHeader().getID());
    assertTrue(normalized.findRecord(createARecord("example.com."), Section.ANSWER));
  }

  /* -------------------------------------------------------------
   * 10. Edge cases for validation methods
   * ------------------------------------------------------------- */
  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordInvalidSectionThrows() {
    Message m = new Message();
    Record a = createARecord("example.com.");
    // Section value 99 is invalid → Section.check should throw IllegalArgumentException
    m.findRecord(a, 99);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindRRsetInvalidTypeThrows() throws TextParseException {
    Message m = new Message();
    // Type value -1 is invalid → Type.check throws IllegalArgumentException
    m.findRRset(Name.fromString("example.com."), -1, Section.ANSWER);
  }

  @Test
  public void testSectionToStringContainsRecord() throws TextParseException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    String s = m.sectionToString(Section.ANSWER);
    assertTrue(s.contains("example.com."));
  }

  @Test
  public void testToStringContainsHeaderAndSize() {
    Message m = new Message();
    String s = m.toString();
    assertTrue(s.contains("Message size:"));
    assertTrue(s.contains(m.getHeader().toString()));
  }
}
