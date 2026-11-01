package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link Message}. The tests cover construction, section handling,
 * wire‑format conversion (including truncation), TSIG handling, cloning and the
 * {@code normalize()} logic.
 *
 * <p>All tests use only the public (or package‑private) API that is present in the
 * source snippet supplied. No mocking frameworks are used – real DNS objects are
 * created via the factory methods provided by the library.
 */
public class MessageTest {

  private Name nameExample;
  private Name nameAlias;
  private Name nameDname;
  private Record aRecord;
  private Record cnameRecord;
  private Record dnameRecord;
  private Record optRecord;

  @Before
  public void setUp() throws Exception {
    nameExample = Name.fromString("example.com.");
    nameAlias = Name.fromString("alias.example.com.");
    nameDname = Name.fromString("dname.example.org.");

    // A record for example.com.
    aRecord = Record.fromString(nameExample, Type.A, DClass.IN, 3600, "192.0.2.1", nameExample);

    // CNAME record: example.com. CNAME alias.example.com.
    cnameRecord = Record.fromString(nameExample, Type.CNAME, DClass.IN, 3600,
        "alias.example.com.", nameExample);

    // DNAME record: dname.example.org. DNAME target.example.net.
    Name dnameTarget = Name.fromString("target.example.net.");
    dnameRecord = Record.fromString(nameDname, Type.DNAME, DClass.IN, 3600,
        "target.example.net.", nameDname);

    // OPT record with extended rcode = 2 (SERVFAIL)
    optRecord = new OPTRecord(4096, 2, 0, 0);
  }

  /** Basic constructor and header handling. */
  @Test
  public void testConstructorAndHeader() {
    Message m = new Message(0x1234);
    assertEquals(0x1234, m.getHeader().getID());

    Header h = new Header(0xABCD);
    m.setHeader(h);
    assertSame(h, m.getHeader());
  }

  /** newQuery creates a message with the proper opcode, RD flag and a QUESTION record. */
  @Test
  public void testNewQuery() {
    Message q = Message.newQuery(aRecord);
    assertEquals(Opcode.QUERY, q.getHeader().getOpcode());
    assertTrue(q.getHeader().getFlag(Flags.RD));
    assertEquals(aRecord, q.getQuestion());
    assertEquals(1, q.getHeader().getCount(Section.QUESTION));
  }

  /** Adding and removing records updates the header counts correctly. */
  @Test
  public void testAddRemoveFindRecord() {
    Message m = new Message();
    assertFalse(m.findRecord(aRecord, Section.ANSWER));

    m.addRecord(aRecord, Section.ANSWER);
    assertTrue(m.findRecord(aRecord, Section.ANSWER));
    assertEquals(1, m.getHeader().getCount(Section.ANSWER));

    boolean removed = m.removeRecord(aRecord, Section.ANSWER);
    assertTrue(removed);
    assertFalse(m.findRecord(aRecord, Section.ANSWER));
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));

    // Removing a non‑existing record should return false and not modify counts
    assertFalse(m.removeRecord(aRecord, Section.ANSWER));
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));
  }

  /** getSectionArray (deprecated) returns an empty array for empty sections. */
  @Test
  public void testGetSectionArrayEmpty() {
    Message m = new Message();
    Record[] empty = m.getSectionArray(Section.AUTHORITY);
    assertNotNull(empty);
    assertEquals(0, empty.length);
  }

  /** getSection returns an unmodifiable list; attempts to modify it throw an exception. */
  @Test
  public void testGetSectionUnmodifiable() {
    Message m = new Message();
    m.addRecord(aRecord, Section.AUTHORITY);
    List<Record> list = m.getSection(Section.AUTHORITY);
    assertEquals(1, list.size());
    try {
      list.add(aRecord);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  /** getSectionRRsets groups records that belong to the same RRset. */
  @Test
  public void testGetSectionRRsetsGrouping() {
    Message m = new Message();
    // Two A records with the same name – should be grouped into one RRset
    Record a2 = Record.fromString(nameExample, Type.A, DClass.IN, 3600, "192.0.2.2", nameExample);
    m.addRecord(aRecord, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);

    List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
    assertEquals(1, rrsets.size());
    RRset set = rrsets.get(0);
    assertEquals(Type.A, set.getType());
    assertEquals(2, set.size());
  }

  /** getOPT returns the OPT record when present and null otherwise. */
  @Test
  public void testGetOPT() {
    Message m = new Message();
    assertNull(m.getOPT());

    m.addRecord(optRecord, Section.ADDITIONAL);
    assertSame(optRecord, m.getOPT());
  }

  /** getRcode incorporates the extended rcode from an OPT record. */
  @Test
  public void testGetRcodeWithExtended() {
    Message m = new Message();
    // Base rcode = 0 (NOERROR)
    assertEquals(Rcode.NOERROR, m.getRcode());

    m.addRecord(optRecord, Section.ADDITIONAL);
    // OPT has extended rcode = 2, so total should be 2 << 4 = 32
    assertEquals(2 << 4, m.getRcode());
  }

  /** toWire without limits produces a non‑empty byte array and sets size. */
  @Test
  public void testToWireSimple() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    byte[] wire = m.toWire();
    assertNotNull(wire);
    assertTrue(wire.length > Header.LENGTH);
    assertEquals(wire.length, m.numBytes());
  }

  /** toWire with a small maxLength and truncate = true returns a truncated message (TC flag set). */
  @Test
  public void testToWireTruncate() throws IOException {
    Message m = new Message();
    // Fill the message with many A records to guarantee truncation
    for (int i = 0; i < 100; i++) {
      Name n = Name.fromString("host" + i + ".example.com.");
      Record r = Record.fromString(n, Type.A, DClass.IN, 3600, "192.0.2." + i, n);
      m.addRecord(r, Section.ANSWER);
    }

    // Choose a maxLength that is smaller than the full message
    int maxLength = 512;
    byte[] truncated = m.toWire(maxLength, true);
    assertTrue(truncated.length <= maxLength);
    // Verify that the TC flag is set in the header of the truncated message
    Message parsed = new Message(truncated);
    assertTrue(parsed.getHeader().getFlag(Flags.TC));
  }

  /** toWire with truncate = false throws MessageSizeExceededException when the message is too large. */
  @Test(expected = MessageSizeExceededException.class)
  public void testToWireExceedsWithoutTruncate() throws Exception {
    Message m = new Message();
    for (int i = 0; i < 200; i++) {
      Name n = Name.fromString("host" + i + ".example.com.");
      Record r = Record.fromString(n, Type.A, DClass.IN, 3600, "192.0.2." + i, n);
      m.addRecord(r, Section.ANSWER);
    }
    // Very small limit – should trigger the exception
    m.toWire(256, false);
  }

  /** Setting a TSIG key makes the message signed; after wire conversion a generated TSIG record is stored. */
  @Test
  public void testTSIGSigning() throws Exception {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    TSIG key = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("keyname."),
        "testkey");
    m.setTSIG(key);
    assertFalse(m.isSigned()); // not yet rendered

    byte[] wire = m.toWire(1500);
    // After rendering the TSIG should be attached and isSigned() true
    assertTrue(m.isSigned());
    assertNotNull(m.getGeneratedTSIG());
    // Verify that the generated TSIG appears as the last record in the ADDITIONAL section
    TSIGRecord tsig = m.getTSIG();
    assertNotNull(tsig);
    assertEquals(key.getName(), tsig.getName());
  }

  /** clone creates a deep copy – modifications to the original do not affect the clone. */
  @Test
  public void testCloneIndependence() throws Exception {
    Message original = new Message();
    original.addRecord(aRecord, Section.ANSWER);
    original.addRecord(cnameRecord, Section.ANSWER);
    original.setHeader(new Header(0xBEEF));

    Message copy = original.clone();

    // Change original's header and sections
    original.getHeader().setID(0x1111);
    original.removeRecord(cnameRecord, Section.ANSWER);

    // Verify clone unchanged
    assertEquals(0xBEEF, copy.getHeader().getID());
    assertTrue(copy.findRecord(cnameRecord, Section.ANSWER));
    assertEquals(2, copy.getHeader().getCount(Section.ANSWER));
    assertEquals(1, original.getHeader().getCount(Section.ANSWER));
  }

  /** normalize removes irrelevant records and follows CNAME chains. */
  @Test
  public void testNormalizeCnameChain() throws Exception {
    // Query: A record for example.com.
    Message query = Message.newQuery(aRecord);

    // Response: CNAME example.com. -> alias.example.com. and A alias.example.com.
    Message response = new Message();
    response.addRecord(cnameRecord, Section.ANSWER);
    Record aAlias = Record.fromString(nameAlias, Type.A, DClass.IN, 3600, "198.51.100.5", nameAlias);
    response.addRecord(aAlias, Section.ANSWER);
    // Add an unrelated record that should be discarded by normalize
    Name unrelated = Name.fromString("unrelated.com.");
    Record unrelatedA = Record.fromString(unrelated, Type.A, DClass.IN, 3600, "203.0.113.9", unrelated);
    response.addRecord(unrelatedA, Section.ANSWER);

    Message normalized = response.normalize(query);
    assertNotNull(normalized);

    // The normalized answer section should contain exactly the CNAME and the A record for the alias
    List<Record> answer = normalized.getSection(Section.ANSWER);
    assertEquals(2, answer.size());
    assertTrue(answer.contains(cnameRecord));
    assertTrue(answer.contains(aAlias));
    // Unrelated record must be gone
    assertFalse(answer.contains(unrelatedA));
  }

  /** isTypeAllowedInSection respects the system property “dnsjava.harden_unknown_additional”. */
  @Test
  public void testIsTypeAllowedInSectionProperty() {
    // By default the property is true, so unknown types are disallowed in ADDITIONAL
    Message m = new Message();
    assertFalse(m.isTypeAllowedInSection(Type.TXT, Section.ADDITIONAL));

    // Disable hardening and verify that unknown types become allowed
    System.setProperty("dnsjava.harden_unknown_additional", "false");
    assertTrue(m.isTypeAllowedInSection(Type.TXT, Section.ADDITIONAL));

    // Clean up the property for other tests
    System.clearProperty("dnsjava.harden_unknown_additional");
  }

  /** toString contains expected sections and the message size. */
  @Test
  public void testToStringContainsSectionsAndSize() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    String s = m.toString();

    assertTrue(s.contains(";; ANSWER:"));
    assertTrue(s.contains(aRecord.toString()));
    assertTrue(s.contains("Message size: " + m.numBytes()));
  }
}
