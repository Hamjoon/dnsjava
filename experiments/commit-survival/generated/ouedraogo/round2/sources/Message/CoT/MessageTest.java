package org.xbill.DNS.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.xbill.DNS.*;

public class MessageTest {

  private Name name;
  private Record aRecord;
  private Record txtRecord;
  private OPTRecord optRecord;

  @Before
  public void setUp() throws Exception {
    // a simple domain name used in many tests
    name = Name.fromString("example.com.");
    // simple A and TXT records (no rdata)
    aRecord = Record.newRecord(name, Type.A, DClass.IN, 3600);
    txtRecord = Record.newRecord(name, Type.TXT, DClass.IN, 3600);
    // an OPT record for EDNS tests
    optRecord = new OPTRecord(4096, 0, 0, 0);
  }

  /** 1. Constructors and basic header handling */
  @Test
  public void testConstructorsAndHeader() {
    Message m1 = new Message(); // random id
    assertNotNull(m1.getHeader());

    Message m2 = new Message(12345);
    assertEquals(12345, m2.getHeader().getID());

    Header custom = new Header(999);
    Message m3 = new Message(custom.getID());
    m3.setHeader(custom);
    assertSame(custom, m3.getHeader());
  }

  /** 2. addRecord / findRecord / getSection */
  @Test
  public void testAddFindRecord() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    assertTrue(m.findRecord(aRecord, Section.ANSWER));
    assertTrue(m.findRecord(aRecord)); // any section
    assertFalse(m.findRecord(txtRecord, Section.ANSWER));

    List<Record> answer = m.getSection(Section.ANSWER);
    assertEquals(1, answer.size());
    assertSame(aRecord, answer.get(0));
  }

  /** 3. removeRecord */
  @Test
  public void testRemoveRecord() {
    Message m = new Message();
    m.addRecord(aRecord, Section.AUTHORITY);
    assertTrue(m.removeRecord(aRecord, Section.AUTHORITY));
    assertFalse(m.findRecord(aRecord, Section.AUTHORITY));
    // removing non‑existing record returns false
    assertFalse(m.removeRecord(txtRecord, Section.AUTHORITY));
  }

  /** 4. removeAllRecords */
  @Test
  public void testRemoveAllRecords() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ADDITIONAL);
    m.addRecord(txtRecord, Section.ADDITIONAL);
    m.removeAllRecords(Section.ADDITIONAL);
    assertEquals(0, m.getHeader().getCount(Section.ADDITIONAL));
    assertTrue(m.getSection(Section.ADDITIONAL).isEmpty());
  }

  /** 5. findRRset */
  @Test
  public void testFindRRset() {
    Message m = new Message();
    Record a1 = Record.newRecord(name, Type.A, DClass.IN, 3600);
    Record a2 = Record.newRecord(name, Type.A, DClass.IN, 3600);
    m.addRecord(a1, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);
    assertTrue(m.findRRset(name, Type.A, Section.ANSWER));
    assertFalse(m.findRRset(name, Type.TXT, Section.ANSWER));
    // also test the overload that searches all sections
    assertTrue(m.findRRset(name, Type.A));
  }

  /** 6. getQuestion */
  @Test
  public void testGetQuestion() {
    Message m = new Message();
    assertNull(m.getQuestion()); // no question yet
    Record q = Record.newRecord(name, Type.A, DClass.IN, 0);
    m.addRecord(q, Section.QUESTION);
    assertSame(q, m.getQuestion());
  }

  /** 7. getTSIG (absence) */
  @Test
  public void testGetTSIGAbsent() {
    Message m = new Message();
    assertNull(m.getTSIG());
  }

  /** 8. isSigned / isVerified default state */
  @Test
  public void testTSIGFlagsDefault() {
    Message m = new Message();
    assertFalse(m.isSigned());
    assertFalse(m.isVerified());
  }

  /** 9. getOPT */
  @Test
  public void testGetOPT() {
    Message m = new Message();
    m.addRecord(optRecord, Section.ADDITIONAL);
    assertSame(optRecord, m.getOPT());
  }

  /** 10. getRcode with extended rcode from OPT */
  @Test
  public void testGetRcodeWithOPT() {
    Message m = new Message();
    // default rcode = 0
    assertEquals(0, m.getRcode());

    // create an OPT with extended rcode = 2 (binary 0010)
    OPTRecord opt = new OPTRecord(4096, 2, 0, 0);
    m.addRecord(opt, Section.ADDITIONAL);
    // base rcode (0) + (2 << 4) = 32
    assertEquals(32, m.getRcode());
  }

  /** 11. getSectionArray (deprecated) – empty case */
  @Test
  public void testGetSectionArrayEmpty() {
    Message m = new Message();
    Record[] arr = m.getSectionArray(Section.AUTHORITY);
    assertNotNull(arr);
    assertEquals(0, arr.length);
  }

  /** 12. getSection – empty list is immutable */
  @Test
  public void testGetSectionEmpty() {
    Message m = new Message();
    List<Record> list = m.getSection(Section.AUTHORITY);
    assertNotNull(list);
    assertTrue(list.isEmpty());
    try {
      list.add(aRecord);
      fail("Returned list should be immutable");
    } catch (UnsupportedOperationException expected) {
      // expected
    }
  }

  /** 13. getSectionRRsets – grouping behavior */
  @Test
  public void testGetSectionRRsetsGrouping() {
    Message m = new Message();
    // two A records with same name/type -> one RRset
    Record a1 = Record.newRecord(name, Type.A, DClass.IN, 3600);
    Record a2 = Record.newRecord(name, Type.A, DClass.IN, 3600);
    // one TXT record -> second RRset
    Record txt = Record.newRecord(name, Type.TXT, DClass.IN, 3600);
    m.addRecord(a1, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);
    m.addRecord(txt, Section.ANSWER);

    List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
    assertEquals(2, rrsets.size());

    // find the A‑type RRset and verify it contains both A records
    RRset aSet = null;
    RRset txtSet = null;
    for (RRset set : rrsets) {
      if (set.getType() == Type.A) {
        aSet = set;
      } else if (set.getType() == Type.TXT) {
        txtSet = set;
      }
    }
    assertNotNull(aSet);
    assertEquals(2, aSet.size());
    assertNotNull(txtSet);
    assertEquals(1, txtSet.size());
  }

  /** 14. toWire – normal conversion */
  @Test
  public void testToWireBasic() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    byte[] wire = m.toWire();
    assertNotNull(wire);
    assertTrue(wire.length > Header.LENGTH);
    assertEquals(wire.length, m.numBytes());
  }

  /** 15. toWire with maxLength smaller than header – should return empty array */
  @Test
  public void testToWireMaxLengthTooSmall() {
    Message m = new Message();
    int tooSmall = Header.LENGTH - 1;
    byte[] wire = m.toWire(tooSmall);
    assertNotNull(wire);
    assertEquals(0, wire.length);
    assertEquals(0, m.numBytes());
  }

  /** 16. clone – deep copy verification */
  @Test
  public void testCloneIndependence() {
    Message original = new Message();
    original.addRecord(aRecord, Section.ANSWER);
    original.setHeader(new Header(555));
    Message copy = original.clone();

    // modify original after cloning
    original.addRecord(txtRecord, Section.ANSWER);
    original.setHeader(new Header(777));

    // clone must retain its own state
    assertEquals(1, copy.getSection(Section.ANSWER).size());
    assertEquals(aRecord, copy.getSection(Section.ANSWER).get(0));
    assertEquals(555, copy.getHeader().getID());

    // original reflects the modifications
    assertEquals(2, original.getSection(Section.ANSWER).size());
    assertEquals(777, original.getHeader().getID());
  }

  /** 17. setResolver / getResolver */
  @Test
  public void testResolverOptional() {
    Message m = new Message();

    // simple Resolver implementation (all methods throw UnsupportedOperationException)
    Resolver dummyResolver = new Resolver() {
      public void setPort(int port) {}
      public void setTCP(boolean flag) {}
      public void setIgnoreTruncation(boolean flag) {}
      public void setEDNS(int version) {}
      public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
      public void setEDNS(int version, int payloadSize, int flags, EDNSOption... options) {}
      public void setTSIGKey(TSIG key) {}
      public void setTimeout(Duration timeout) {}
      public Duration getTimeout() { return null; }
      public Message send(Message query) throws IOException { return null; }
      public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
      public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
      public Object sendAsync(Message query, ResolverListener listener) { return null; }
    };

    m.setResolver(dummyResolver);
    Optional<Resolver> opt = m.getResolver();
    assertTrue(opt.isPresent());
    assertSame(dummyResolver, opt.get());
  }

  /** 18. sectionToString with invalid index returns null */
  @Test
  public void testSectionToStringInvalid() {
    Message m = new Message();
    assertNull(m.sectionToString(5));
  }

  /** 19. addRecord with null should throw NPE */
  @Test(expected = NullPointerException.class)
  public void testAddRecordNull() {
    Message m = new Message();
    m.addRecord(null, Section.ANSWER);
  }

  /** 20. Message(byte[]) constructor – round‑trip test */
  @Test
  public void testMessageFromByteArray() throws IOException {
    Message m1 = new Message();
    m1.addRecord(aRecord, Section.ANSWER);
    byte[] wire = m1.toWire();

    Message m2 = new Message(wire);
    assertEquals(1, m2.getSection(Section.ANSWER).size());
    assertEquals(aRecord.getName(), m2.getSection(Section.ANSWER).get(0).getName());
    assertEquals(aRecord.getType(), m2.getSection(Section.ANSWER).get(0).getType());
  }

  /** 21. Message(ByteBuffer) constructor – same as byte[] test */
  @Test
  public void testMessageFromByteBuffer() throws IOException {
    Message m1 = new Message();
    m1.addRecord(txtRecord, Section.AUTHORITY);
    byte[] wire = m1.toWire();

    Message m2 = new Message(ByteBuffer.wrap(wire));
    assertEquals(1, m2.getSection(Section.AUTHORITY).size());
    assertEquals(txtRecord.getName(), m2.getSection(Section.AUTHORITY).get(0).getName());
    assertEquals(txtRecord.getType(), m2.getSection(Section.AUTHORITY).get(0).getType());
  }

  /** 22. toString includes header and section markers */
  @Test
  public void testToStringContent() {
    Message m = new Message(0xABCD);
    m.addRecord(aRecord, Section.QUESTION);
    String s = m.toString();
    assertTrue(s.contains(";; QUESTION"));
    assertTrue(s.contains("example.com."));
    assertTrue(s.contains("ABCD")); // header ID appears in its toString()
  }
}
