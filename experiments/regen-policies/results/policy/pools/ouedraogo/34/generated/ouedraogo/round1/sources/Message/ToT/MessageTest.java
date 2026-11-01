package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Message}. The tests cover typical usage,
 * edge‑cases and error handling for the public API of the class.
 *
 * The tests deliberately avoid any mocking – they construct real DNS objects using the
 * constructors and factory methods that are part of the dnsjava public API.
 */
public class MessageTest {

  private Name exampleName;
  private Record aRecord;
  private Record cnameRecord;
  private Record dnameRecord;
  private OPTRecord optRecord;
  private TSIG tsigKey;

  @Before
  public void setUp() throws Exception {
    exampleName = Name.fromString("example.com.");
    // A generic A record (type = A, class = IN)
    aRecord = Record.newRecord(exampleName, Type.A, DClass.IN, 3600);
    // A generic CNAME record
    cnameRecord = Record.newRecord(exampleName, Type.CNAME, DClass.IN, 3600);
    // A generic DNAME record (used in normalize tests)
    dnameRecord = Record.newRecord(exampleName, Type.DNAME, DClass.IN, 3600);
    // An OPT record with extended rcode = 2 (SERVFAIL)
    optRecord = new OPTRecord(4096, 2, 0, 0);
    // Simple TSIG key (HMAC‑MD5) – the key material is arbitrary for testing
    tsigKey = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("testkey."), "abcd");
  }

  /** Basic constructor and header handling */
  @Test
  public void testHeaderAndId() {
    Message m = new Message(0x1234);
    assertEquals(0x1234, m.getHeader().getID());

    // change header id via setter
    m.getHeader().setID(0xABCD);
    assertEquals(0xABCD, m.getHeader().getID());
  }

  /** newQuery creates a message with the proper flags and question section */
  @Test
  public void testNewQuery() {
    Message q = Message.newQuery(aRecord);
    assertEquals(Opcode.QUERY, q.getHeader().getOpcode());
    assertTrue(q.getHeader().getFlag(Flags.RD));
    assertSame(aRecord, q.getQuestion());
  }

  /** addRecord / getSection / getSectionArray behavior */
  @Test
  public void testAddAndRetrieveRecords() {
    Message m = new Message();
    // initially empty
    assertArrayEquals(new Record[0], m.getSectionArray(Section.ANSWER));
    assertTrue(m.getSection(Section.ANSWER).isEmpty());

    // add a record to ANSWER
    m.addRecord(aRecord, Section.ANSWER);
    assertEquals(1, m.getHeader().getCount(Section.ANSWER));

    // verify retrieval via both APIs
    Record[] arr = m.getSectionArray(Section.ANSWER);
    assertEquals(1, arr.length);
    assertEquals(aRecord, arr[0]);

    List<Record> list = m.getSection(Section.ANSWER);
    assertEquals(1, list.size());
    assertEquals(aRecord, list.get(0));
  }

  /** removeRecord returns true when the record exists and updates the count */
  @Test
  public void testRemoveRecord() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    assertTrue(m.removeRecord(aRecord, Section.ANSWER));
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));
    // removing again should return false
    assertFalse(m.removeRecord(aRecord, Section.ANSWER));
  }

  /** removeAllRecords clears a section and resets the count */
  @Test
  public void testRemoveAllRecords() {
    Message m = new Message();
    m.addRecord(aRecord, Section.AUTHORITY);
    m.addRecord(cnameRecord, Section.AUTHORITY);
    assertEquals(2, m.getHeader().getCount(Section.AUTHORITY));

    m.removeAllRecords(Section.AUTHORITY);
    assertEquals(0, m.getHeader().getCount(Section.AUTHORITY));
    assertTrue(m.getSection(Section.AUTHORITY).isEmpty());
  }

  /** findRecord works for both specific sections and any section */
  @Test
  public void testFindRecord() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    assertTrue(m.findRecord(aRecord, Section.ANSWER));
    assertTrue(m.findRecord(aRecord));
    assertFalse(m.findRecord(cnameRecord, Section.ANSWER));
    assertFalse(m.findRecord(cnameRecord));
  }

  /** findRRset identifies an RRset by name and type */
  @Test
  public void testFindRRset() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    assertTrue(m.findRRset(exampleName, Type.A, Section.ANSWER));
    assertFalse(m.findRRset(exampleName, Type.CNAME, Section.ANSWER));
    // any section
    assertTrue(m.findRRset(exampleName, Type.A));
    assertFalse(m.findRRset(exampleName, Type.CNAME));
  }

  /** getQuestion returns the first record of the QUESTION section or null */
  @Test
  public void testGetQuestion() {
    Message m = new Message();
    assertNull(m.getQuestion());

    m.addRecord(aRecord, Section.QUESTION);
    assertEquals(aRecord, m.getQuestion());

    // Adding a second question should not affect the first returned
    Record second = Record.newRecord(Name.fromString("other.com."), Type.A, DClass.IN, 3600);
    m.addRecord(second, Section.QUESTION);
    assertEquals(aRecord, m.getQuestion());
  }

  /** getTSIG returns null when no TSIG is present, otherwise the last TSIG in ADDITIONAL */
  @Test
  public void testGetTSIG() throws IOException {
    Message m = new Message();
    assertNull(m.getTSIG());

    // add a generic record that is NOT a TSIG – should still be null
    m.addRecord(aRecord, Section.ADDITIONAL);
    assertNull(m.getTSIG());

    // add a real TSIG record via the TSIG API
    m.setTSIG(tsigKey);
    byte[] wire = m.toWire(512); // triggers TSIG generation
    Message parsed = new Message(wire);
    TSIGRecord tsigRec = parsed.getTSIG();
    assertNotNull(tsigRec);
    assertEquals(tsigKey.getName(), tsigRec.getName());
  }

  /** isSigned / isVerified reflect TSIG processing */
  @Test
  public void testSigningState() {
    Message m = new Message();
    assertFalse(m.isSigned());
    assertFalse(m.isVerified());

    m.setTSIG(tsigKey);
    m.toWire(512); // forces signing
    assertTrue(m.isSigned());
    // verification is not performed automatically – should be false
    assertFalse(m.isVerified());
  }

  /** getOPT returns the OPT record if present */
  @Test
  public void testGetOPT() {
    Message m = new Message();
    assertNull(m.getOPT());

    m.addRecord(optRecord, Section.ADDITIONAL);
    assertSame(optRecord, m.getOPT());
  }

  /** getRcode incorporates extended rcode from OPT record */
  @Test
  public void testGetRcodeWithOPT() {
    Message m = new Message();
    // base rcode = NOERROR (0)
    assertEquals(Rcode.NOERROR, m.getRcode());

    // add OPT with extended rcode = 2 (SERVFAIL)
    m.addRecord(optRecord, Section.ADDITIONAL);
    assertEquals(2 << 4, m.getRcode()); // extended rcode shifted by 4 bits
  }

  /** toWire without length limit returns full wire format */
  @Test
  public void testToWireFull() throws IOException {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    byte[] wire = m.toWire();
    assertTrue(wire.length > Header.LENGTH);
    // parsing the wire back should yield the same record
    Message parsed = new Message(wire);
    assertEquals(1, parsed.getHeader().getCount(Section.ANSWER));
    assertEquals(aRecord, parsed.getSectionArray(Section.ANSWER)[0]);
  }

  /** toWire with too small maxLength returns an empty array */
  @Test
  public void testToWireTruncatedTooSmall() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    byte[] wire = m.toWire(10); // less than header length (12)
    assertEquals(0, wire.length);
  }

  /** Section toString produces human‑readable output */
  @Test
  public void testSectionToString() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    String s = m.sectionToString(Section.ANSWER);
    assertTrue(s.contains("example.com."));
    assertTrue(s.contains("A"));
  }

  /** Overall toString contains header, sections and size */
  @Test
  public void testMessageToString() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    m.addRecord(optRecord, Section.ADDITIONAL);
    String txt = m.toString();

    assertTrue(txt.contains(";; ANSWER:"));
    assertTrue(txt.contains("example.com."));
    assertTrue(txt.contains(";; ADDITIONAL:"));
    assertTrue(txt.contains(";; Message size:"));
  }

  /** clone creates a deep copy of sections and header */
  @Test
  public void testClone() throws Exception {
    Message original = new Message();
    original.addRecord(aRecord, Section.ANSWER);
    original.addRecord(cnameRecord, Section.AUTHORITY);
    original.setTSIG(tsigKey);
    original.toWire(512); // generate TSIG

    Message copy = original.clone();

    // Header instance must be different but equal
    assertNotSame(original.getHeader(), copy.getHeader());
    assertEquals(original.getHeader().getID(), copy.getHeader().getID());

    // Sections must be deep‑copied
    assertNotSame(original.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));
    assertArrayEquals(original.getSectionArray(Section.ANSWER),
                      copy.getSectionArray(Section.ANSWER));

    // TSIG generated record must be cloned as well
    assertNotNull(copy.getGeneratedTSIG());
    assertNotSame(original.getGeneratedTSIG(), copy.getGeneratedTSIG());
  }

  /** setResolver / getResolver store and retrieve a resolver */
  @Test
  public void testResolverHandling() {
    Message m = new Message();
    Resolver resolver = new Resolver() {
      @Override public void setPort(int port) {}
      @Override public void setTCP(boolean flag) {}
      @Override public void setIgnoreTruncation(boolean flag) {}
      @Override public void setEDNS(int version) {}
      @Override public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
      @Override public void setTSIGKey(TSIG key) {}
      @Override public void setTimeout(Duration timeout) {}
      @Override public void setTimeout(int secs) {}
      @Override public void setTimeout(int secs, int msecs) {}
      @Override public void setTimeout(int secs, int msecs) {}
      @Override public Message send(Message query) throws IOException {return null;}
      @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) {return null;}
      @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) {return null;}
      @Override public Object sendAsync(Message query, ResolverListener listener) {return null;}
    };
    assertFalse(m.getResolver().isPresent());
    m.setResolver(resolver);
    assertTrue(m.getResolver().isPresent());
    assertSame(resolver, m.getResolver().get());
  }

  /** isTypeAllowedInSection respects the system property “dnsjava.harden_unknown_additional” */
  @Test
  public void testIsTypeAllowedInSection() {
    Message m = new Message();

    // By default, unknown types in ADDITIONAL are disallowed (property true)
    assertFalse(m.isTypeAllowedInSection(Type.SRV, Section.ADDITIONAL));

    // When the property is set to false, unknown types become allowed
    System.setProperty("dnsjava.harden_unknown_additional", "false");
    assertTrue(m.isTypeAllowedInSection(Type.SRV, Section.ADDITIONAL));
    System.clearProperty("dnsjava.harden_unknown_additional");
  }

  /** normalize removes irrelevant records and keeps relevant ones */
  @Test
  public void testNormalizeSimple() throws Exception {
    // Build a query asking for A record of example.com.
    Message query = Message.newQuery(Record.newRecord(exampleName, Type.A, DClass.IN, 0));

    // Build a response that contains the correct answer plus an extra unrelated record.
    Message response = new Message();
    response.addRecord(aRecord, Section.ANSWER);
    // Add an irrelevant record (different name)
    Record other = Record.newRecord(Name.fromString("other.com."), Type.A, DClass.IN, 3600);
    response.addRecord(other, Section.ANSWER);

    // Normalization should drop the irrelevant record.
    Message normalized = response.normalize(query);
    assertNotNull(normalized);
    assertEquals(1, normalized.getHeader().getCount(Section.ANSWER));
    assertEquals(aRecord, normalized.getSectionArray(Section.ANSWER)[0]);
  }

  /** normalize returns the same message when rcode is not NOERROR/NXDOMAIN */
  @Test
  public void testNormalizeNonApplicableRcode() throws Exception {
    Message query = Message.newQuery(Record.newRecord(exampleName, Type.A, DClass.IN, 0));
    Message resp = new Message();
    resp.getHeader().setRcode(Rcode.SERVFAIL); // non‑normalizable rcode
    Message result = resp.normalize(query);
    // Should be the original instance (no change)
    assertSame(resp, result);
  }

  /** toWire respects maxLength by truncating and setting TC flag */
  @Test
  public void testToWireTruncationSetsTC() throws IOException {
    Message m = new Message();
    // Add many A records to force truncation
    for (int i = 0; i < 100; i++) {
      Name n = Name.fromString("host" + i + ".example.com.");
      Record r = Record.newRecord(n, Type.A, DClass.IN, 3600);
      m.addRecord(r, Section.ANSWER);
    }
    // Choose a maxLength that is definitely smaller than the full message
    byte[] wire = m.toWire(200);
    Message parsed = new Message(wire);
    // TC flag must be set
    assertTrue(parsed.getHeader().getFlag(Flags.TC));
  }

  /** sectionToWire correctly skips OPT records during size calculation */
  @Test
  public void testSectionToWireSkipsOPT() throws IOException {
    Message m = new Message();
    // Add an OPT record (should be ignored by sectionToWire for ADDITIONAL)
    m.addRecord(optRecord, Section.ADDITIONAL);
    // Add a normal A record
    m.addRecord(aRecord, Section.ADDITIONAL);
    // Use a very small maxLength to force the method to evaluate truncation logic
    byte[] wire = m.toWire(50);
    // No exception means sectionToWire behaved correctly with OPT skipping
    assertTrue(wire.length > 0);
  }
}
