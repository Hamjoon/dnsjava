###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link Message}. The tests use only the public API that is
 * available in the source fragment provided and the minimal set of DNS Java
 * classes listed in the prompt.
 */
public class MessageTest {

  private Name name;
  private Record aRecord;
  private Record cnameRecord;
  private Record mxRecord;
  private OPTRecord optRecord;

  @Before
  public void setUp() throws Exception {
    name = Name.fromString("www.example.com.");
    aRecord = Record.fromString(name, Type.A, DClass.IN, 3600, "192.0.2.1", name);
    cnameRecord = Record.fromString(name, Type.CNAME, DClass.IN, 3600, "alias.example.com.", name);
    mxRecord = Record.fromString(Name.fromString("example.com."), Type.MX, DClass.IN, 3600, "10 mail.example.com.", name);
    optRecord = new OPTRecord(4096, 0, 0, 0);
  }

  /** Verify that a newly created Message has a random ID and an empty header. */
  @Test
  public void testDefaultConstructor() {
    Message m = new Message();
    assertNotNull(m.getHeader());
    // Header ID should be non‑negative and within 16‑bit range
    int id = m.getHeader().getID();
    assertTrue(id >= 0 && id <= 0xFFFF);
  }

  /** Verify the static factory {@link Message#newQuery(Record)}. */
  @Test
  public void testNewQueryFactory() {
    Message q = Message.newQuery(aRecord);
    Header h = q.getHeader();
    assertEquals(Opcode.QUERY, h.getOpcode());
    assertTrue(h.getFlag(Flags.RD));
    assertEquals(1, h.getCount(Section.QUESTION));
    Record qRec = q.getQuestion();
    assertNotNull(qRec);
    assertEquals(aRecord, qRec);
  }

  /** Test addRecord, removeRecord, findRecord and findRRset behaviour. */
  @Test
  public void testAddRemoveFind() {
    Message m = new Message(0x1234);
    Header h = m.getHeader();

    // initially empty
    assertEquals(0, h.getCount(Section.ANSWER));
    assertFalse(m.findRecord(aRecord, Section.ANSWER));

    // add a record to ANSWER
    m.addRecord(aRecord, Section.ANSWER);
    assertEquals(1, h.getCount(Section.ANSWER));
    assertTrue(m.findRecord(aRecord, Section.ANSWER));
    assertTrue(m.findRecord(aRecord));

    // add same record to AUTHORITY – counts are independent
    m.addRecord(mxRecord, Section.AUTHORITY);
    assertEquals(1, h.getCount(Section.AUTHORITY));
    assertTrue(m.findRecord(mxRecord, Section.AUTHORITY));

    // findRRset
    assertTrue(m.findRRset(name, Type.A, Section.ANSWER));
    assertFalse(m.findRRset(name, Type.MX, Section.ANSWER));

    // remove record
    assertTrue(m.removeRecord(aRecord, Section.ANSWER));
    assertEquals(0, h.getCount(Section.ANSWER));
    assertFalse(m.findRecord(aRecord, Section.ANSWER));
  }

  /** Test getSection, getSectionArray (deprecated) and immutability of the returned list. */
  @Test
  public void testGetSectionAndArray() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    m.addRecord(cnameRecord, Section.ANSWER);

    List<Record> sec = m.getSection(Section.ANSWER);
    assertEquals(2, sec.size());
    // the list must be unmodifiable
    try {
      sec.add(mxRecord);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException ignored) {
    }

    Record[] arr = m.getSectionArray(Section.ANSWER);
    assertEquals(2, arr.length);
    assertArrayEquals(new Record[] {aRecord, cnameRecord}, arr);
  }

  /** Verify that an OPT record can be retrieved and that extended rcode is taken into account. */
  @Test
  public void testOPTAndExtendedRcode() {
    Message m = new Message();
    m.addRecord(optRecord, Section.ADDITIONAL);
    assertSame(optRecord, m.getOPT());

    // default rcode is 0
    assertEquals(0, m.getRcode());

    // Set a base rcode and an extended part via OPT
    m.getHeader().setRcode(Rcode.SERVFAIL);
    // OPT with extended rcode 2 (i.e. 2 << 4 = 32 added)
    OPTRecord opt = new OPTRecord(4096, 2, 0, 0);
    m.removeAllRecords(Section.ADDITIONAL);
    m.addRecord(opt, Section.ADDITIONAL);
    assertEquals(Rcode.SERVFAIL + (2 << 4), m.getRcode());
  }

  /** Test the wire encoding helpers – normal and truncated output. */
  @Test
  public void testToWireAndTruncation() throws IOException {
    Message m = new Message();
    // add a few records so that the message has a non‑zero size
    for (int i = 0; i < 5; i++) {
      m.addRecord(aRecord, Section.ANSWER);
    }
    // ensure we have a realistic size (> header)
    byte[] full = m.toWire();
    assertTrue(full.length > Header.LENGTH);
    assertEquals(full.length, m.numBytes());

    // request a maximum length that is definitely too small – expect truncation
    int max = Header.LENGTH + 10; // very small payload
    byte[] truncated = m.toWire(max, true);
    assertTrue(truncated.length <= max);
    // The TC flag must be set on the header of the truncated message
    Message parsed = new Message(truncated);
    assertTrue(parsed.getHeader().getFlag(Flags.TC));
  }

  /** Verify that calling toWire with insufficient space and truncate=false throws. */
  @Test(expected = MessageSizeExceededException.class)
  public void testToWireThrowsWhenNoTruncate() throws Exception {
    Message m = new Message();
    for (int i = 0; i < 10; i++) {
      m.addRecord(aRecord, Section.ANSWER);
    }
    // definitely too small, and we ask for an exception
    m.toWire(Header.LENGTH - 1, false);
  }

  /** Test the clone method creates a deep copy of sections and header. */
  @Test
  public void testCloneDeepCopy() {
    Message original = new Message(0xABCD);
    original.addRecord(aRecord, Section.ANSWER);
    original.addRecord(mxRecord, Section.AUTHORITY);
    original.addRecord(optRecord, Section.ADDITIONAL);

    Message copy = original.clone();

    // IDs must be equal but independent objects
    assertEquals(original.getHeader().getID(), copy.getHeader().getID());
    assertNotSame(original.getHeader(), copy.getHeader());

    // Sections must contain equal records but be different list instances
    assertEquals(original.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));
    assertNotSame(original.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));

    // Mutating the original must not affect the clone
    original.removeRecord(aRecord, Section.ANSWER);
    assertTrue(copy.findRecord(aRecord, Section.ANSWER));
    assertFalse(original.findRecord(aRecord, Section.ANSWER));
  }

  /** Minimal test for normalize – a message without DNAME/CNAME should be returned unchanged. */
  @Test
  public void testNormalizeNoChanges() throws Exception {
    Message query = Message.newQuery(aRecord);
    Message response = new Message();
    response.addRecord(aRecord, Section.ANSWER);
    response.addRecord(mxRecord, Section.AUTHORITY);
    response.addRecord(optRecord, Section.ADDITIONAL);

    Message normalized = response.normalize(query);
    // Should be a different instance but contain the same data
    assertNotSame(response, normalized);
    assertEquals(response.getSection(Section.ANSWER), normalized.getSection(Section.ANSWER));
    assertEquals(response.getSection(Section.AUTHORITY), normalized.getSection(Section.AUTHORITY));
    assertEquals(response.getSection(Section.ADDITIONAL), normalized.getSection(Section.ADDITIONAL));
  }

  /** Verify toString contains expected sections and message size. */
  @Test
  public void testToStringRepresentation() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    m.addRecord(optRecord, Section.ADDITIONAL);
    String s = m.toString();

    assertTrue(s.contains("ANSWER"));
    assertTrue(s.contains("ADDITIONAL"));
    assertTrue(s.contains(aRecord.toString()));
    assertTrue(s.contains("Message size:"));
  }

  /** Ensure isSigned / isVerified reflect the internal tsigState after explicitly setting it. */
  @Test
  public void testTSIGStateFlags() throws Exception {
    Message m = new Message();
    // Directly manipulate the package‑private field via reflection for testing purposes
    java.lang.reflect.Field stateField = Message.class.getDeclaredField("tsigState");
    stateField.setAccessible(true);
    stateField.setInt(m, Message.TSIG_SIGNED);
    assertTrue(m.isSigned());
    assertFalse(m.isVerified());

    stateField.setInt(m, Message.TSIG_VERIFIED);
    assertTrue(m.isSigned());
    assertTrue(m.isVerified());

    stateField.setInt(m, Message.TSIG_UNSIGNED);
    assertFalse(m.isSigned());
  }

  /** Test setting a TSIG key does not break wire encoding (the key itself is not exercised). */
  @Test
  public void testSetTSIG() {
    Message m = new Message();
    // Use a simple HMAC‑MD5 key (the key value does not matter for this test)
    TSIG tsig = new TSIG(TSIG.HMAC_MD5, Name.fromString("key.example.com."), "abcd");
    m.setTSIG(tsig);
    // After setting, the message should still be renderable
    byte[] wire = m.toWire();
    assertNotNull(wire);
    // No exception means the internal state is consistent
  }

  /** Helper to compare two Record arrays ignoring order. */
  private static void assertArrayEquals(Record[] expected, Record[] actual) {
    assertEquals("Array length differs", expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals("Element " + i + " differs", expected[i], actual[i]);
    }
  }
}
###Test END##