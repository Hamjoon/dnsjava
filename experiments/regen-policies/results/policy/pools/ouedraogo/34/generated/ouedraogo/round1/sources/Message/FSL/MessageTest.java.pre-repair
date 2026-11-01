package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Message}.
 *
 * <p>The tests exercise the public API as well as package‑private behaviour
 * (accessed via reflection) to obtain high coverage of the class.
 */
public class MessageTest {

  /** Helper to create a {@link Name} from a string. */
  private static Name name(String s) {
    try {
      return Name.fromString(s);
    } catch (TextParseException e) {
      throw new RuntimeException(e);
    }
  }

  /** Helper to create a generic {@link Record} (type A, class IN) */
  private static Record aRecord(Name name) {
    // Record.newRecord(Name name, int type, int dclass, long ttl)
    return Record.newRecord(name, Type.A, DClass.IN, 300);
  }

  /** Helper to create an NS {@link Record}. */
  private static Record nsRecord(Name name, Name target) {
    // NS record data is just the target name encoded as rdata.
    // The library provides a convenient factory for NS via Record.newRecord with
    // the target name encoded as a Name in the rdata byte array.
    // For simplicity we use the generic factory with a zero‑length rdata;
    // the actual content is irrelevant for most tests that only
    // manipulate sections and counts.
    return Record.newRecord(name, Type.NS, DClass.IN, 300);
  }

  /** Helper to create a TSIG {@link Record}. */
  private static TSIGRecord tsigRecord() {
    try {
      Name name = Name.fromString("example.com.");
      Name alg = TSIG.HMAC_SHA256;
      Instant now = Instant.now();
      Duration fudge = Duration.ofSeconds(300);
      byte[] sig = new byte[] {0x01, 0x02, 0x03};
      return new TSIGRecord(name, DClass.IN, 0, alg, now, fudge, sig, 0, Rcode.NOERROR, new byte[0]);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Helper to create an OPT record. */
  private static OPTRecord optRecord() {
    return new OPTRecord(4096, 0, 0, 0);
  }

  @Test
  public void testConstructorAndHeaderId() {
    Message m = new Message(0x1234);
    assertEquals(0x1234, m.getHeader().getID());

    Message random = new Message();
    // ID is random; just verify that a Header object exists
    assertNotNull(random.getHeader());
  }

  @Test
  public void testAddAndRetrieveRecords() {
    Message m = new Message();
    Record a = aRecord(name("www.example.com."));
    m.addRecord(a, Section.ANSWER);

    // Verify count in header
    assertEquals(1, m.getHeader().getCount(Section.ANSWER));

    // Verify getSection returns an unmodifiable list containing the record
    List<Record> answer = m.getSection(Section.ANSWER);
    assertEquals(1, answer.size());
    assertSame(a, answer.get(0));

    // Verify deprecated array accessor
    Record[] arr = m.getSectionArray(Section.ANSWER);
    assertArrayEquals(new Record[] {a}, arr);
  }

  @Test
  public void testRemoveRecordAndRemoveAll() {
    Message m = new Message();
    Record a1 = aRecord(name("a.example.com."));
    Record a2 = aRecord(name("b.example.com."));
    m.addRecord(a1, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);

    assertTrue(m.removeRecord(a1, Section.ANSWER));
    assertEquals(1, m.getHeader().getCount(Section.ANSWER));
    assertFalse(m.findRecord(a1, Section.ANSWER));

    // Remove all
    m.removeAllRecords(Section.ANSWER);
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));
    assertTrue(m.getSection(Section.ANSWER).isEmpty());
  }

  @Test
  public void testFindRecordAndFindRRset() {
    Message m = new Message();
    Record a = aRecord(name("host.example.com."));
    Record ns = nsRecord(name("example.com."), name("ns1.example.com."));
    m.addRecord(a, Section.ANSWER);
    m.addRecord(ns, Section.AUTHORITY);

    assertTrue(m.findRecord(a, Section.ANSWER));
    assertTrue(m.findRecord(ns, Section.AUTHORITY));
    assertTrue(m.findRecord(a)); // any section

    // RRset lookup: same name and type
    assertTrue(m.findRRset(name("host.example.com."), Type.A, Section.ANSWER));
    assertFalse(m.findRRset(name("host.example.com."), Type.NS, Section.ANSWER));
  }

  @Test
  public void testGetQuestion() {
    Message m = new Message();
    Record q = Record.newRecord(name("query.example.com."), Type.A, DClass.IN, 0);
    m.addRecord(q, Section.QUESTION);
    assertSame(q, m.getQuestion());

    Message empty = new Message();
    assertNull(empty.getQuestion());
  }

  @Test
  public void testGetOPTAndExtendedRcode() {
    Message m = new Message();
    OPTRecord opt = optRecord();
    // set an extended rcode via OPT (e.g., 2 << 4 = 32)
    // The constructor does not expose a setter; however, we can
    // create an OPT with a non‑zero extended rcode using the
    // 4‑argument constructor.
    OPTRecord optWithRcode = new OPTRecord(4096, 2, 0, 0);
    m.addRecord(optWithRcode, Section.ADDITIONAL);
    assertSame(optWithRcode, m.getOPT());

    // Header rcode is 0; extended rcode should be added
    assertEquals(2 << 4, m.getRcode());
  }

  @Test
  public void testGetTSIG() {
    Message m = new Message();
    TSIGRecord tsig = tsigRecord();
    m.addRecord(tsig, Section.ADDITIONAL);
    assertSame(tsig, m.getTSIG());

    // When the last additional record is not a TSIG, getTSIG returns null
    Message m2 = new Message();
    m2.addRecord(aRecord(name("a.example.com.")), Section.ADDITIONAL);
    assertNull(m2.getTSIG());
  }

  @Test
  public void testIsSignedAndIsVerifiedViaReflection() throws Exception {
    Message m = new Message();
    // Access package‑private field tsigState
    Field state = Message.class.getDeclaredField("tsigState");
    state.setAccessible(true);

    // Not signed initially
    assertFalse(m.isSigned());
    assertFalse(m.isVerified());

    // Set to SIGNED
    state.setInt(m, Message.TSIG_SIGNED);
    assertTrue(m.isSigned());
    assertFalse(m.isVerified());

    // Set to VERIFIED
    state.setInt(m, Message.TSIG_VERIFIED);
    assertTrue(m.isSigned());
    assertTrue(m.isVerified());

    // Set to FAILED
    state.setInt(m, Message.TSIG_FAILED);
    assertTrue(m.isSigned());
    assertFalse(m.isVerified());
  }

  @Test
  public void testCloneCreatesDeepCopy() {
    Message original = new Message();
    Record a = aRecord(name("clone.example.com."));
    original.addRecord(a, Section.ANSWER);
    original.getHeader().setOpcode(Opcode.QUERY);
    original.setTSIG(new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("key.example."), "abcd"));

    Message copy = original.clone();

    // Header is cloned – changes to original header do not affect copy
    original.getHeader().setOpcode(Opcode.UPDATE);
    assertEquals(Opcode.QUERY, copy.getHeader().getOpcode());

    // Sections are deep‑copied – modifying original list does not affect copy
    original.removeAllRecords(Section.ANSWER);
    assertEquals(1, copy.getHeader().getCount(Section.ANSWER));
    assertEquals(0, original.getHeader().getCount(Section.ANSWER));

    // TSIG key reference is not cloned; however, generatedTSIG is null until toWire()
    // Ensure that generatedTSIG in the clone is null as well
    assertNull(copy.getGeneratedTSIG());
  }

  @Test
  public void testIsTypeAllowedInSectionViaReflection() throws Exception {
    Message m = new Message();

    Method method = Message.class.getDeclaredMethod("isTypeAllowedInSection", int.class, int.class);
    method.setAccessible(true);

    // AUTHORITY section allows NS, SOA, DS, NSEC, NSEC3
    assertTrue((Boolean) method.invoke(m, Type.NS, Section.AUTHORITY));
    assertTrue((Boolean) method.invoke(m, Type.SOA, Section.AUTHORITY));
    assertFalse((Boolean) method.invoke(m, Type.A, Section.AUTHORITY));

    // ADDITIONAL section allows A and AAAA
    assertTrue((Boolean) method.invoke(m, Type.A, Section.ADDITIONAL));
    assertTrue((Boolean) method.invoke(m, Type.AAAA, Section.ADDITIONAL));
    assertFalse((Boolean) method.invoke(m, Type.NS, Section.ADDITIONAL));

    // When the system property dnsjava.harden_unknown_additional is false,
    // unknown types are allowed.
    System.setProperty("dnsjava.harden_unknown_additional", "false");
    assertTrue((Boolean) method.invoke(m, Type.MX, Section.ADDITIONAL));
    System.clearProperty("dnsjava.harden_unknown_additional");
  }

  @Test
  public void testToStringContainsSectionsAndSize() {
    Message m = new Message();
    Record a = aRecord(name("txt.example.com."));
    m.addRecord(a, Section.ANSWER);
    m.addRecord(optRecord(), Section.ADDITIONAL);
    // Force wire conversion to set size
    byte[] wire = m.toWire();
    String s = m.toString();

    assertTrue(s.contains("ANSWER"));
    assertTrue(s.contains("ADDITIONAL"));
    assertTrue(s.contains("Message size: " + m.numBytes()));
    // The question section should be empty -> no ";; QUESTION" lines
    assertFalse(s.contains(";; QUESTION"));
  }

  @Test
  public void testToWireTruncationSetsTCFlag() {
    Message m = new Message();
    // Build a message that exceeds a small maxLength to force truncation
    for (int i = 0; i < 50; i++) {
      Record a = aRecord(Name.fromString("host" + i + ".example.com."));
      m.addRecord(a, Section.ANSWER);
    }

    // Use a deliberately small maxLength (e.g., 100 bytes)
    byte[] wire = m.toWire(100);
    // Header should have the TC flag set – Flags.TC = 0x02 (from dnsjava)
    // Since we cannot reference Flags directly, we inspect the flag byte:
    // The flag byte is the 3rd byte of the wire format (index 2)
    assertTrue(wire.length >= 3);
    int flagsByte = wire[2] & 0xFF;
    // TC flag is bit 1 (0x02)
    assertEquals(0x02, flagsByte & 0x02);
  }

  @Test
  public void testSectionToStringFormatsRecords() {
    Message m = new Message();
    Record a = aRecord(name("example.com."));
    m.addRecord(a, Section.ANSWER);
    String secStr = m.sectionToString(Section.ANSWER);
    assertTrue(secStr.contains("example.com."));
    // Question section format includes name, type and class
    Record q = Record.newRecord(name("q.example.com."), Type.A, DClass.IN, 0);
    m.addRecord(q, Section.QUESTION);
    String qStr = m.sectionToString(Section.QUESTION);
    assertTrue(qStr.contains(";;\tq.example.com."));
    assertTrue(qStr.contains("type = A"));
    assertTrue(qStr.contains("class = IN"));
  }

  @Test
  public void testRemoveNonexistentRecordReturnsFalse() {
    Message m = new Message();
    Record a = aRecord(name("nonexistent.example.com."));
    assertFalse(m.removeRecord(a, Section.ANSWER));
  }

  @Test
  public void testAddAndRetrieveMultipleSections() {
    Message m = new Message();
    Record a = aRecord(name("a.example.com."));
    Record ns = nsRecord(name("example.com."), name("ns1.example.com."));
    Record opt = optRecord();

    m.addRecord(a, Section.ANSWER);
    m.addRecord(ns, Section.AUTHORITY);
    m.addRecord(opt, Section.ADDITIONAL);

    assertEquals(1, m.getHeader().getCount(Section.ANSWER));
    assertEquals(1, m.getHeader().getCount(Section.AUTHORITY));
    assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));

    assertSame(opt, m.getOPT());
  }

  @Test
  public void testGetSectionRRsetsGroupsRecords() {
    Message m = new Message();
    Name name = name("group.example.com.");
    Record a1 = Record.newRecord(name, Type.A, DClass.IN, 300);
    Record a2 = Record.newRecord(name, Type.A, DClass.IN, 300);
    Record cname = Record.newRecord(name, Type.CNAME, DClass.IN, 300);
    m.addRecord(a1, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);
    m.addRecord(cname, Section.ANSWER);

    List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
    // Expect two RRsets: one for A records (combined) and one for CNAME
    assertEquals(2, rrsets.size());
    // Verify that the A RRset contains both A records
    RRset aSet = rrsets.stream().filter(r -> r.getType() == Type.A).findFirst().orElse(null);
    assertNotNull(aSet);
    assertEquals(2, aSet.size());
  }
}
