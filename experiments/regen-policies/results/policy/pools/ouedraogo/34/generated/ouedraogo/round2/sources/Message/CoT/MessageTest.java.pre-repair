package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link Message}.  The tests cover every public method,
 * basic functionality, edge‑case handling and selected exception scenarios.
 *
 * The test class follows the steps requested in the prompt:
 *   1. List of public methods (see comments above each test).
 *   2. Basic test case for each method.
 *   3. Identification of edge cases / exception handling.
 *   4. Specific tests for those edge cases.
 *   5. All tests merged into a single file.
 *
 * NOTE: The dnsjava library supplies many supporting classes (Type, DClass,
 * Section, Opcode, Flags, Rcode, etc.).  They are used directly because they are
 * part of the public API referenced by {@link Message}.
 */
public class MessageTest {

  /* ---------------------------------------------------------------------- */
  /*  Helper methods used by several tests                                   */
  /* ---------------------------------------------------------------------- */

  /** Creates a simple A record for the given name. */
  private Record makeARecord(String name) throws TextParseException {
    Name n = Name.fromString(name);
    // Record.newRecord(name, type, dclass, ttl, data) – use empty data for A.
    return Record.newRecord(n, Type.A, DClass.IN, 3600);
  }

  /** Creates a simple AAAA record for the given name. */
  private Record makeAAAARecord(String name) throws TextParseException {
    Name n = Name.fromString(name);
    return Record.newRecord(n, Type.AAAA, DClass.IN, 3600);
  }

  /** Creates a generic OPT record (used for EDNS). */
  private OPTRecord makeOPTRecord() {
    // payloadSize, extendedRcode, version, flags – simple values are fine.
    return new OPTRecord(4096, 0, 0, 0);
  }

  /** Creates a query message with a single question record. */
  private Message makeQueryMessage(String qname) throws TextParseException {
    Record q = Record.newRecord(Name.fromString(qname), Type.A, DClass.IN, 0);
    return Message.newQuery(q);
  }

  /* ---------------------------------------------------------------------- */
  /*  1. Constructors & static factories                                      */
  /* ---------------------------------------------------------------------- */

  /** public Message(int id) */
  @Test
  public void testConstructorWithId() {
    int id = 0x1234;
    Message m = new Message(id);
    assertEquals(id, m.getHeader().getID());
  }

  /** public Message() – random ID */
  @Test
  public void testDefaultConstructor() {
    Message m = new Message();
    // The ID should be within the unsigned 16‑bit range.
    int id = m.getHeader().getID();
    assertTrue(id >= 0 && id <= 0xFFFF);
  }

  /** public static Message newQuery(Record r) */
  @Test
  public void testNewQuery() throws TextParseException {
    Record q = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 0);
    Message m = Message.newQuery(q);
    assertEquals(Opcode.QUERY, m.getHeader().getOpcode());
    assertTrue(m.getHeader().getFlag(Flags.RD));
    assertSame(q, m.getQuestion());
  }

  /** public static Message newUpdate(Name zone) */
  @Test
  public void testNewUpdate() throws TextParseException {
    Name zone = Name.fromString("example.com.");
    Message m = Message.newUpdate(zone);
    // Update messages are instances of the subclass Update.
    assertTrue(m instanceof Update);
  }

  /** public Message(byte[] b) and public Message(ByteBuffer) – round‑trip test */
  @Test
  public void testWireRoundTrip() throws IOException, TextParseException {
    Message original = makeQueryMessage("example.com.");
    byte[] wire = original.toWire();
    Message fromBytes = new Message(wire);
    assertEquals(original.getHeader().getID(), fromBytes.getHeader().getID());
    assertEquals(original.getQuestion(), fromBytes.getQuestion());
  }

  /* ---------------------------------------------------------------------- */
  /*  2. Header handling                                                      */
  /* ---------------------------------------------------------------------- */

  /** public void setHeader(Header h) / public Header getHeader() */
  @Test
  public void testSetAndGetHeader() {
    Header h = new Header(0xABCD);
    Message m = new Message();
    m.setHeader(h);
    assertSame(h, m.getHeader());
  }

  /* ---------------------------------------------------------------------- */
  /*  3. Record manipulation methods                                          */
  /* ---------------------------------------------------------------------- */

  /** public void addRecord(Record r, int section) */
  @Test
  public void testAddRecord() throws TextParseException {
    Message m = new Message();
    Record a = makeARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    assertTrue(m.findRecord(a, Section.ANSWER));
    assertEquals(1, m.getHeader().getCount(Section.ANSWER));
  }

  /** public boolean removeRecord(Record r, int section) */
  @Test
  public void testRemoveRecord() throws TextParseException {
    Message m = new Message();
    Record a = makeARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    assertTrue(m.removeRecord(a, Section.ANSWER));
    assertFalse(m.findRecord(a, Section.ANSWER));
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));
  }

  /** removeRecord – attempt to remove non‑existent record (edge case) */
  @Test
  public void testRemoveRecordNotPresent() throws TextParseException {
    Message m = new Message();
    Record a = makeARecord("example.com.");
    assertFalse(m.removeRecord(a, Section.ANSWER));
  }

  /** public void removeAllRecords(int section) */
  @Test
  public void testRemoveAllRecords() throws TextParseException {
    Message m = new Message();
    Record a = makeARecord("example.com.");
    Record b = makeAAAARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    m.addRecord(b, Section.ANSWER);
    m.removeAllRecords(Section.ANSWER);
    assertFalse(m.findRecord(a, Section.ANSWER));
    assertFalse(m.findRecord(b, Section.ANSWER));
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));
    // The returned list should be empty.
    assertTrue(m.getSection(Section.ANSWER).isEmpty());
  }

  /** public boolean findRecord(Record r, int section) */
  @Test
  public void testFindRecordInSection() throws TextParseException {
    Message m = new Message();
    Record a = makeARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    assertTrue(m.findRecord(a, Section.ANSWER));
    assertFalse(m.findRecord(a, Section.AUTHORITY));
  }

  /** public boolean findRecord(Record r) – search all sections */
  @Test
  public void testFindRecordAnywhere() throws TextParseException {
    Message m = new Message();
    Record a = makeARecord("example.com.");
    m.addRecord(a, Section.AUTHORITY);
    assertTrue(m.findRecord(a));
    m.removeAllRecords(Section.AUTHORITY);
    assertFalse(m.findRecord(a));
  }

  /** public boolean findRRset(Name name, int type, int section) */
  @Test
  public void testFindRRsetInSection() throws TextParseException {
    Message m = new Message();
    Name n = Name.fromString("example.com.");
    Record a = Record.newRecord(n, Type.A, DClass.IN, 3600);
    m.addRecord(a, Section.ANSWER);
    assertTrue(m.findRRset(n, Type.A, Section.ANSWER));
    assertFalse(m.findRRset(n, Type.AAAA, Section.ANSWER));
  }

  /** public boolean findRRset(Name name, int type) – any section */
  @Test
  public void testFindRRsetAnywhere() throws TextParseException {
    Message m = new Message();
    Name n = Name.fromString("example.com.");
    Record a = Record.newRecord(n, Type.A, DClass.IN, 3600);
    m.addRecord(a, Section.AUTHORITY);
    assertTrue(m.findRRset(n, Type.A));
    assertFalse(m.findRRset(n, Type.AAAA));
  }

  /* ---------------------------------------------------------------------- */
  /*  4. Accessors for special sections / flags                               */
  /* ---------------------------------------------------------------------- */

  /** public Record getQuestion() – normal case */
  @Test
  public void testGetQuestionPresent() throws TextParseException {
    Message q = makeQueryMessage("example.com.");
    Record qrec = q.getQuestion();
    assertNotNull(qrec);
    assertEquals(Name.fromString("example.com."), qrec.getName());
    assertEquals(Type.A, qrec.getType());
  }

  /** getQuestion – when no question is present (edge case) */
  @Test
  public void testGetQuestionAbsent() {
    Message m = new Message();
    assertNull(m.getQuestion());
  }

  /** public TSIGRecord getTSIG() – no TSIG present */
  @Test
  public void testGetTSIGAbsent() {
    Message m = new Message();
    assertNull(m.getTSIG());
  }

  /** public boolean isSigned() / isVerified() – default unsigned */
  @Test
  public void testSigningStateDefaults() {
    Message m = new Message();
    assertFalse(m.isSigned());
    assertFalse(m.isVerified());
  }

  /** public OPTRecord getOPT() – absent and present */
  @Test
  public void testGetOPT() {
    Message m = new Message();
    assertNull(m.getOPT());

    OPTRecord opt = makeOPTRecord();
    m.addRecord(opt, Section.ADDITIONAL);
    assertSame(opt, m.getOPT());
  }

  /** public int getRcode() – with and without OPT extended rcode */
  @Test
  public void testGetRcode() {
    Message m = new Message();
    // Base rcode = 0 (NOERROR)
    assertEquals(Rcode.NOERROR, m.getRcode());

    // Add an OPT with extended rcode = 3 (e.g. BADVERS)
    OPTRecord opt = new OPTRecord(4096, 3, 0, 0);
    m.addRecord(opt, Section.ADDITIONAL);
    // Expected: 0 + (3 << 4) = 48
    assertEquals(3 << 4, m.getRcode());
  }

  /** public Record[] getSectionArray(int section) – deprecated but still functional */
  @Test
  public void testGetSectionArray() throws TextParseException {
    Message m = new Message();
    Record a = makeARecord("example.com.");
    Record b = makeAAAARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    m.addRecord(b, Section.ANSWER);
    Record[] arr = m.getSectionArray(Section.ANSWER);
    assertEquals(2, arr.length);
    // Order should be insertion order.
    assertSame(a, arr[0]);
    assertSame(b, arr[1]);

    // Empty section returns shared empty array.
    Record[] empty = m.getSectionArray(Section.AUTHORITY);
    assertSame(empty, Message.emptyRecordArray);
  }

  /** public List<Record> getSection(int section) – immutable view */
  @Test
  public void testGetSectionImmutable() throws TextParseException {
    Message m = new Message();
    Record a = makeARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    List<Record> list = m.getSection(Section.ANSWER);
    assertEquals(1, list.size());
    assertSame(a, list.get(0));
    // Attempting to modify should throw UnsupportedOperationException.
    try {
      list.add(makeAAAARecord("example.com."));
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // ok
    }
  }

  /** public List<RRset> getSectionRRsets(int section) – grouping */
  @Test
  public void testGetSectionRRsetsGrouping() throws TextParseException {
    Message m = new Message();
    Name n = Name.fromString("example.com.");
    // Two A records with same name/type should be grouped.
    Record a1 = Record.newRecord(n, Type.A, DClass.IN, 3600);
    Record a2 = Record.newRecord(n, Type.A, DClass.IN, 3600);
    // One AAAA record – separate set.
    Record aaaa = Record.newRecord(n, Type.AAAA, DClass.IN, 3600);
    m.addRecord(a1, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);
    m.addRecord(aaaa, Section.ANSWER);
    List<RRset> sets = m.getSectionRRsets(Section.ANSWER);
    assertEquals(2, sets.size());
    // Verify types.
    boolean hasA = false, hasAAAA = false;
    for (RRset set : sets) {
      if (set.getType() == Type.A) {
        hasA = true;
        assertEquals(2, set.size());
      } else if (set.getType() == Type.AAAA) {
        hasAAAA = true;
        assertEquals(1, set.size());
      }
    }
    assertTrue(hasA);
    assertTrue(hasAAAA);
  }

  /* ---------------------------------------------------------------------- */
  /*  5. Wire format conversion                                                */
  /* ---------------------------------------------------------------------- */

  /** public byte[] toWire() – basic conversion */
  @Test
  public void testToWireBasic() throws IOException, TextParseException {
    Message m = makeQueryMessage("example.com.");
    byte[] wire = m.toWire();
    assertNotNull(wire);
    assertTrue(wire.length > Header.LENGTH); // must contain header + question
    assertEquals(wire.length, m.numBytes());
  }

  /** public byte[] toWire(int maxLength) – truncation when maxLength too small */
  @Test
  public void testToWireWithMaxLengthTruncation() throws IOException, TextParseException {
    Message m = makeQueryMessage("example.com.");
    // Header length is Header.LENGTH (12). Provide a maxLength smaller than that.
    byte[] wire = m.toWire(Header.LENGTH - 1);
    assertEquals(0, wire.length);
  }

  /* ---------------------------------------------------------------------- */
  /*  6. TSIG handling – limited testing (no real signing)                    */
  /* ---------------------------------------------------------------------- */

  /** public void setTSIG(TSIG key) and setTSIG(TSIG, int, TSIGRecord) */
  @Test
  public void testSetTSIG() throws TextParseException {
    Message m = new Message();
    // Use a dummy TSIG key – algorithm name does not need to be valid for this test.
    TSIG key = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("keyname."), "abcd");
    m.setTSIG(key);
    // The internal state is not directly visible; verify that calling toWire(int) does not
    // throw and that isSigned() becomes true after rendering.
    byte[] wire = m.toWire(65535);
    assertTrue(m.isSigned());
    // Generated TSIG should be stored internally.
    // Use reflection to access the package‑private method.
    TSIGRecord gen = null;
    try {
      java.lang.reflect.Method mth = Message.class.getDeclaredMethod("getGeneratedTSIG");
      mth.setAccessible(true);
      gen = (TSIGRecord) mth.invoke(m);
    } catch (Exception e) {
      fail("Reflection failed: " + e);
    }
    assertNotNull(gen);
  }

  /* ---------------------------------------------------------------------- */
  /*  7. Resolver association                                                   */
  /* ---------------------------------------------------------------------- */

  /** public void setResolver(Resolver) / public Optional<Resolver> getResolver() */
  @Test
  public void testResolverAssociation() {
    Message m = new Message();
    Resolver resolver = new Resolver() {
      // Minimal anonymous implementation – all methods are default.
    };
    m.setResolver(resolver);
    Optional<Resolver> opt = m.getResolver();
    assertTrue(opt.isPresent());
    assertSame(resolver, opt.get());
  }

  /* ---------------------------------------------------------------------- */
  /*  8. Cloning                                                               */
  /* ---------------------------------------------------------------------- */

  /** public Message clone() – deep copy verification */
  @Test
  public void testCloneDeepCopy() throws TextParseException, CloneNotSupportedException {
    Message original = new Message();
    Record a = makeARecord("example.com.");
    original.addRecord(a, Section.ANSWER);
    Message copy = original.clone();

    // Header should be equal but not the same instance.
    assertEquals(original.getHeader().getID(), copy.getHeader().getID());
    assertNotSame(original.getHeader(), copy.getHeader());

    // Section list should be a deep copy.
    assertTrue(copy.findRecord(a, Section.ANSWER));
    // Removing from original must not affect copy.
    original.removeRecord(a, Section.ANSWER);
    assertFalse(original.findRecord(a, Section.ANSWER));
    assertTrue(copy.findRecord(a, Section.ANSWER));
  }

  /* ---------------------------------------------------------------------- */
  /*  9. Normalization – simple successful case                              */
  /* ---------------------------------------------------------------------- */

  /** public Message normalize(Message query) – simple matching answer */
  @Test
  public void testNormalizeSimpleMatch() throws Exception {
    // Query asking for A record.
    Message query = makeQueryMessage("example.com.");

    // Response with a matching A answer.
    Message response = new Message();
    Record a = makeARecord("example.com.");
    response.addRecord(a, Section.ANSWER);
    // Add the same question section (required for normalize to work).
    response.addRecord(query.getQuestion(), Section.QUESTION);

    Message normalized = response.normalize(query);
    // Normalized message should contain the same answer.
    assertNotNull(normalized);
    assertTrue(normalized.findRecord(a, Section.ANSWER));
  }

  /* ---------------------------------------------------------------------- */
  /* 10. Normalization – irrelevant record triggers exception when flag is true */
  /* ---------------------------------------------------------------------- */

  /** public Message normalize(Message query, boolean throwOnIrrelevantRecord) */
  @Test
  public void testNormalizeThrowsOnIrrelevantRecord() throws Exception {
    // Query for A.
    Message query = makeQueryMessage("example.com.");

    // Response contains an AAAA answer (irrelevant for A query).
    Message response = new Message();
    Record aaaa = makeAAAARecord("example.com.");
    response.addRecord(aaaa, Section.ANSWER);
    response.addRecord(query.getQuestion(), Section.QUESTION);

    try {
      response.normalize(query, true);
      fail("Expected WireParseException due to irrelevant RRset");
    } catch (WireParseException expected) {
      // Expected exception – message contains details.
    }
  }

  /* ---------------------------------------------------------------------- */
  /* 11. Edge case – removeRecord with null section (invalid) – expect exception */
  /* ---------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveRecordInvalidSection() throws TextParseException {
    Message m = new Message();
    Record a = makeARecord("example.com.");
    // Section constants are 0‑3; use -1 to trigger Section.check failure.
    m.removeRecord(a, -1);
  }

  /* ---------------------------------------------------------------------- */
  /* 12. Edge case – findRRset with invalid type – expect exception */
  /* ---------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testFindRRsetInvalidType() throws TextParseException {
    Message m = new Message();
    Name n = Name.fromString("example.com.");
    // Type.check throws IllegalArgumentException for out‑of‑range values.
    m.findRRset(n, -1, Section.ANSWER);
  }

  /* ---------------------------------------------------------------------- */
  /* 13. Edge case – sectionToString with invalid section – expect exception */
  /* ---------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testSectionToStringInvalidSection() {
    Message m = new Message();
    // Section.check will fail for invalid index.
    m.sectionToString(-1);
  }

  /* ---------------------------------------------------------------------- */
  /* 14. toString – basic sanity check (contains header and section labels) */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testToStringContainsHeaders() throws TextParseException {
    Message m = makeQueryMessage("example.com.");
    String s = m.toString();
    assertTrue(s.contains(";; QUESTION:"));
    assertTrue(s.contains(";; ANSWER:"));
    // Header string should be present.
    assertTrue(s.contains(m.getHeader().toString()));
  }
}
