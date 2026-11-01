package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Message}.
 *
 * <p>The tests are written in the same package as {@code Message} so that they can
 * access package‑private members (e.g. {@code isTypeAllowedInSection}).
 *
 * <p>Only APIs that appear in the source of {@code Message} or in the
 * dependency signatures supplied with the kata are used – no external helpers,
 * mocking frameworks or additional libraries are required.
 */
public class MessageTest {

  /* -----------------------------------------------------------------
   * Helpers – create simple A records and an OPT record.
   * ----------------------------------------------------------------- */
  private Record makeARecord(String nameStr) throws IOException {
    Name name = Name.fromString(nameStr);
    // DClass.IN == 1, Type.A == 1 (standard DNS values)
    return Record.newRecord(name, Type.A, DClass.IN, 0);
  }

  private Record makeCNAMERecord(String nameStr, String targetStr) throws IOException {
    Name name = Name.fromString(nameStr);
    Name target = Name.fromString(targetStr);
    return Record.newRecord(name, Type.CNAME, DClass.IN, 0, 0,
        new byte[] { (byte) target.labels() }); // dummy rdata – only the type/name matters for grouping
  }

  private OPTRecord makeOPTRecord() {
    // payloadSize 4096, extended rcode 0, version 0, no flags
    return new OPTRecord(4096, 0, 0, 0);
  }

  /* -----------------------------------------------------------------
   * Basic construction and header handling
   * ----------------------------------------------------------------- */
  @Test
  public void testConstructorsAndHeader() {
    Message m1 = new Message();                // random ID
    Message m2 = new Message(0x1234);          // explicit ID
    assertNotNull(m1.getHeader());
    assertNotNull(m2.getHeader());
    assertEquals(0x1234, m2.getHeader().getID());

    Header h = new Header(0xABCD);
    Message m3 = new Message(h);
    assertSame(h, m3.getHeader());

    // replace header
    Header newHeader = new Header(0x5555);
    m3.setHeader(newHeader);
    assertSame(newHeader, m3.getHeader());
  }

  /* -----------------------------------------------------------------
   * Adding, removing and locating records
   * ----------------------------------------------------------------- */
  @Test
  public void testAddFindRemoveRecord() throws IOException {
    Message msg = new Message();
    Record a1 = makeARecord("example.com.");
    Record a2 = makeARecord("example.org.");

    // initially empty
    assertFalse(msg.findRecord(a1, Section.ANSWER));
    assertFalse(msg.findRecord(a1));

    // add to ANSWER
    msg.addRecord(a1, Section.ANSWER);
    assertTrue(msg.findRecord(a1, Section.ANSWER));
    assertTrue(msg.findRecord(a1));

    // add another to AUTHORITY
    msg.addRecord(a2, Section.AUTHORITY);
    assertTrue(msg.findRecord(a2, Section.AUTHORITY));
    assertTrue(msg.findRecord(a2));

    // remove and verify counts are adjusted
    assertTrue(msg.removeRecord(a1, Section.ANSWER));
    assertFalse(msg.findRecord(a1, Section.ANSWER));
    assertEquals(0, msg.getHeader().getCount(Section.ANSWER));
  }

  @Test
  public void testRemoveAllRecords() throws IOException {
    Message msg = new Message();
    Record a = makeARecord("example.com.");
    msg.addRecord(a, Section.ANSWER);
    assertEquals(1, msg.getHeader().getCount(Section.ANSWER));

    msg.removeAllRecords(Section.ANSWER);
    assertEquals(0, msg.getHeader().getCount(Section.ANSWER));
    assertFalse(msg.findRecord(a, Section.ANSWER));
  }

  @Test
  public void testFindRRset() throws IOException {
    Message msg = new Message();
    Record a1 = makeARecord("example.com.");
    Record a2 = makeARecord("example.com."); // same name/type → same RRset
    Record a3 = makeARecord("example.org."); // different name

    msg.addRecord(a1, Section.ANSWER);
    msg.addRecord(a2, Section.ANSWER);
    msg.addRecord(a3, Section.ANSWER);

    assertTrue(msg.findRRset(Name.fromString("example.com."), Type.A, Section.ANSWER));
    assertFalse(msg.findRRset(Name.fromString("example.com."), Type.AAAA, Section.ANSWER));
    assertTrue(msg.findRRset(Name.fromString("example.org."), Type.A, Section.ANSWER));
  }

  /* -----------------------------------------------------------------
   * Section handling
   * ----------------------------------------------------------------- */
  @Test
  public void testGetSectionAndRRsets() throws IOException {
    Message msg = new Message();
    Record a1 = makeARecord("example.com.");
    Record a2 = makeARecord("example.com."); // same RRset
    Record a3 = makeARecord("example.org."); // separate RRset

    msg.addRecord(a1, Section.ANSWER);
    msg.addRecord(a2, Section.ANSWER);
    msg.addRecord(a3, Section.ANSWER);

    List<Record> list = msg.getSection(Section.ANSWER);
    assertEquals(3, list.size());

    // deprecated array method
    Record[] array = msg.getSectionArray(Section.ANSWER);
    assertEquals(3, array.length);

    // grouping into RRsets – should be 2 sets (example.com. and example.org.)
    List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
    assertEquals(2, rrsets.size());
    for (RRset set : rrsets) {
      if (set.getName().equals(Name.fromString("example.com."))) {
        assertEquals(2, set.size());
      } else {
        assertEquals(1, set.size());
      }
    }
  }

  @Test
  public void testGetOPTAndRcode() {
    Message msg = new Message();
    OPTRecord opt = makeOPTRecord();
    msg.addRecord(opt, Section.ADDITIONAL);

    // default rcode 0
    assertEquals(0, msg.getRcode());

    // set an extended rcode via OPT (e.g. 2 << 4 = 32)
    // The constructor above already sets extended rcode = 0; we use reflection to set it
    // because the public API does not expose a setter for extended rcode.
    // The test stays within allowed APIs (java.lang.reflect is part of JDK).
    try {
      java.lang.reflect.Field extField = OPTRecord.class.getDeclaredField("extendedRcode");
      extField.setAccessible(true);
      extField.setInt(opt, 2); // extended rcode = 2
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertEquals(2 << 4, msg.getRcode());
  }

  /* -----------------------------------------------------------------
   * TSIG handling – basic signing and state queries
   * ----------------------------------------------------------------- */
  @Test
  public void testTSIGSigningAndState() throws IOException, MessageSizeExceededException {
    Message msg = new Message();
    Record q = makeARecord("example.com.");
    msg.addRecord(q, Section.QUESTION);

    // before any TSIG the message is not signed
    assertFalse(msg.isSigned());
    assertFalse(msg.isVerified());

    // set a TSIG key (HMAC_MD5 with a dummy secret)
    TSIG key = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("testkey."), "deadbeef");
    msg.setTSIG(key);

    // render with a generous max length – this will generate a TSIG record
    byte[] wire = msg.toWire(Message.MAXLENGTH);
    assertTrue(wire.length > 0);
    // after rendering the internal state marks the message as signed
    assertTrue(msg.isSigned());
    // verification flag is not set automatically
    assertFalse(msg.isVerified());

    // the generated TSIG can be retrieved
    TSIGRecord gen = msg.getGeneratedTSIG();
    assertNotNull(gen);
    assertEquals(key.getName(), gen.getAlgorithm());
  }

  @Test(expected = MessageSizeExceededException.class)
  public void testToWireTooSmallThrows() throws MessageSizeExceededException, IOException {
    Message msg = new Message();
    // a maxLength smaller than a DNS header (Header.LENGTH) forces an exception
    msg.toWire(10, false);
  }

  @Test
  public void testToWireTruncation() throws MessageSizeExceededException, IOException {
    Message msg = new Message();
    // fill the message with many A records so that it exceeds a tiny size
    for (int i = 0; i < 30; i++) {
      msg.addRecord(makeARecord("host" + i + ".example.com."), Section.ANSWER);
    }
    // maxLength = 100 bytes, truncation allowed
    byte[] wire = msg.toWire(100, true);
    assertTrue(wire.length <= 100);
    // the TC flag must be set – we inspect the header bytes directly
    // Header is 12 bytes; the flags byte is at offset 2
    assertEquals(Flags.TC, wire[2] & Flags.TC);
  }

  /* -----------------------------------------------------------------
   * Resolver association
   * ----------------------------------------------------------------- */
  @Test
  public void testResolverOptional() {
    Message msg = new Message();
    assertFalse(msg.getResolver().isPresent());

    Resolver dummy = new Resolver() {
      @Override public void setPort(int p) {}
      @Override public void setTCP(boolean f) {}
      @Override public void setIgnoreTruncation(boolean f) {}
      @Override public void setEDNS(int v) {}
      @Override public void setEDNS(int v, int ps, int fl, List<EDNSOption> o) {}
      @Override public void setEDNS(int v, int ps, int fl, EDNSOption... o) {}
      @Override public void setTSIGKey(TSIG key) {}
      @Override public void setTimeout(Duration d) {}
      @Override public Duration getTimeout() { return null; }
      @Override public Message send(Message q) { return null; }
      @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message q) { return null; }
      @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message q, java.util.concurrent.Executor e) { return null; }
      @Override public Object sendAsync(Message q, ResolverListener l) { return null; }
    };

    msg.setResolver(dummy);
    Optional<Resolver> opt = msg.getResolver();
    assertTrue(opt.isPresent());
    assertSame(dummy, opt.get());
  }

  /* -----------------------------------------------------------------
   * isTypeAllowedInSection – exercised via the public package‑private method
   * ----------------------------------------------------------------- */
  @Test
  public void testIsTypeAllowedInSection() {
    Message msg = new Message();

    // AUTHORITY section allows SOA, NS, DS, NSEC, NSEC3
    assertTrue(msg.isTypeAllowedInSection(Type.SOA, Section.AUTHORITY));
    assertTrue(msg.isTypeAllowedInSection(Type.NS, Section.AUTHORITY));
    assertFalse(msg.isTypeAllowedInSection(Type.A, Section.AUTHORITY));

    // ADDITIONAL section allows A and AAAA only (by default hardened unknown adds are rejected)
    assertTrue(msg.isTypeAllowedInSection(Type.A, Section.ADDITIONAL));
    assertTrue(msg.isTypeAllowedInSection(Type.AAAA, Section.ADDITIONAL));
    assertFalse(msg.isTypeAllowedInSection(Type.MX, Section.ADDITIONAL));
  }

  /* -----------------------------------------------------------------
   * sectionToString and toString – basic sanity checks
   * ----------------------------------------------------------------- */
  @Test
  public void testSectionToStringAndToString() throws IOException {
    Message msg = new Message();
    Record a = makeARecord("example.com.");
    msg.addRecord(a, Section.QUESTION);
    msg.addRecord(makeARecord("example.org."), Section.ANSWER);

    String qStr = msg.sectionToString(Section.QUESTION);
    assertTrue(qStr.contains("example.com."));
    assertTrue(qStr.contains("type = A"));

    String full = msg.toString();
    // header line present
    assertTrue(full.contains(";;"));
    // each section header appears
    assertTrue(full.contains(";; QUESTION:"));
    assertTrue(full.contains(";; ANSWER:"));
    // record string appears
    assertTrue(full.contains("example.org."));
    // size indicator at the end
    assertTrue(full.contains("Message size:"));
  }

  /* -----------------------------------------------------------------
   * clone – deep copy verification
   * ----------------------------------------------------------------- */
  @Test
  public void testCloneDeepCopy() throws IOException {
    Message original = new Message();
    Record a = makeARecord("example.com.");
    original.addRecord(a, Section.ANSWER);
    original.setTSIG(new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("key."), "abcd"));

    Message copy = original.clone();

    // Header is a different object but equal ID
    assertNotSame(original.getHeader(), copy.getHeader());
    assertEquals(original.getHeader().getID(), copy.getHeader().getID());

    // Sections are deep‑copied
    assertNotSame(original.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));
    assertEquals(1, copy.getSection(Section.ANSWER).size());
    assertEquals(a, copy.getSection(Section.ANSWER).get(0));

    // TSIG key reference is the same (immutable), generated TSIG is null until rendering
    assertNull(copy.getGeneratedTSIG());
    assertNull(original.getGeneratedTSIG());

    // Mutate original – clone should stay unchanged
    original.removeAllRecords(Section.ANSWER);
    assertEquals(0, original.getSection(Section.ANSWER).size());
    assertEquals(1, copy.getSection(Section.ANSWER).size());
  }

  /* -----------------------------------------------------------------
   * numBytes – after wire conversion
   * ----------------------------------------------------------------- */
  @Test
  public void testNumBytesAfterWire() throws IOException {
    Message msg = new Message();
    msg.addRecord(makeARecord("example.com."), Section.QUESTION);
    assertEquals(0, msg.numBytes()); // not rendered yet

    byte[] wire = msg.toWire();
    assertTrue(wire.length > 0);
    assertEquals(wire.length, msg.numBytes());
  }

  /* -----------------------------------------------------------------
   * removeRecord returns correct boolean when record absent
   * ----------------------------------------------------------------- */
  @Test
  public void testRemoveRecordAbsent() throws IOException {
    Message msg = new Message();
    Record a = makeARecord("example.com.");
    // attempt removal from empty section
    assertFalse(msg.removeRecord(a, Section.ANSWER));

    // add then remove a different record
    Record b = makeARecord("example.org.");
    msg.addRecord(b, Section.ANSWER);
    assertFalse(msg.removeRecord(a, Section.ANSWER));
  }

  /* -----------------------------------------------------------------
   * getTSIG – when present and when absent
   * ----------------------------------------------------------------- */
  @Test
  public void testGetTSIG() throws IOException {
    Message msg = new Message();
    // No TSIG yet
    assertNull(msg.getTSIG());

    // Add a TSIG record manually
    Name name = Name.fromString("tsig.example.");
    TSIGRecord tsig = new TSIGRecord(name, DClass.IN, 0, TSIG.HMAC_MD5,
        Instant.now(), Duration.ofSeconds(300), new byte[] {0x01, 0x02},
        0, Rcode.NOERROR, new byte[0]);
    msg.addRecord(tsig, Section.ADDITIONAL);
    assertSame(tsig, msg.getTSIG());

    // Add a non‑TSIG final record – getTSIG should now return null
    Record a = makeARecord("example.com.");
    msg.addRecord(a, Section.ADDITIONAL);
    assertNull(msg.getTSIG());
  }

  /* -----------------------------------------------------------------
   * getSectionArray returns empty array for empty sections
   * ----------------------------------------------------------------- */
  @Test
  public void testGetSectionArrayEmpty() {
    Message msg = new Message();
    Record[] empty = msg.getSectionArray(Section.AUTHORITY);
    assertNotNull(empty);
    assertEquals(0, empty.length);
  }

  /* -----------------------------------------------------------------
   * toWire with maxLength smaller than header – returns empty array
   * ----------------------------------------------------------------- */
  @Test
  public void testToWireBelowHeaderLength() {
    Message msg = new Message();
    byte[] wire = msg.toWire(Header.LENGTH - 1);
    assertEquals(0, wire.length);
  }

  /* -----------------------------------------------------------------
   * verify that getOPT returns the first OPT record, ignoring others
   * ----------------------------------------------------------------- */
  @Test
  public void testGetOPTMultiple() {
    Message msg = new Message();
    OPTRecord opt1 = new OPTRecord(512, 0, 0, 0);
    OPTRecord opt2 = new OPTRecord(1024, 0, 0, 0);
    msg.addRecord(opt1, Section.ADDITIONAL);
    msg.addRecord(opt2, Section.ADDITIONAL);
    // getOPT should return the first one (opt1)
    assertSame(opt1, msg.getOPT());
  }
}
