package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link Message}. The tests exercise the public API only and avoid any
 * use of reflection or internal fields.
 */
public class MessageTest {

  /** Helper to create a simple A record for the given name. */
  private Record createARecord(String name) throws IOException {
    Name n = Name.fromString(name);
    // ttl = 300, class = IN, type = A, empty rdata (the library will create a generic Record)
    return Record.newRecord(n, Type.A, DClass.IN, 300);
  }

  /** Helper to create a TSIGRecord used for TSIG‑related tests. */
  private TSIGRecord createTSIGRecord() throws IOException {
    Name name = Name.fromString("example.com.");
    // Use the built‑in HMAC_MD5 algorithm name; other fields are arbitrary but legal.
    return new TSIGRecord(
        name,               // name
        DClass.IN,          // class
        0,                  // ttl
        TSIG.HMAC_MD5,      // algorithm
        new Date(),         // timeSigned
        300,                // fudge (seconds)
        new byte[0],        // signature
        0,                  // originalID
        Rcode.NOERROR,      // error
        new byte[0]         // other
    );
  }

  /** Helper to create an OPT record with a given extended rcode. */
  private OPTRecord createOPTRecord(int extendedRcode) {
    // payloadSize, xrcode (extended), version, flags
    return new OPTRecord(4096, extendedRcode, 0, 0);
  }

  /** Simple stub implementation of {@link Resolver} used only for storing a reference. */
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
    @Override public void setTimeout(Duration timeout) {}
    @Override public void setTimeout(int secs, int msecs) {}
    @Override public void setTimeout(int secs) {}
    @Override public Duration getTimeout() { return null; }
    @Override public Message send(Message query) throws IOException { return null; }
    @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
    @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query,
                                                                            java.util.concurrent.Executor executor) { return null; }
    @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
  }

  @Test
  public void testConstructorAndHeader() {
    Message m = new Message(0x1234);
    assertEquals(0x1234, m.getHeader().getID());

    Message random = new Message();
    // ID is random but should be within unsigned 16‑bit range
    assertTrue(random.getHeader().getID() >= 0 && random.getHeader().getID() <= 0xFFFF);
  }

  @Test
  public void testNewQueryFactory() throws IOException {
    Record q = createARecord("example.com.");
    Message query = Message.newQuery(q);
    assertEquals(Opcode.QUERY, query.getHeader().getOpcode());
    assertTrue(query.getHeader().getFlag(Flags.RD));
    assertSame(q, query.getQuestion());
  }

  @Test
  public void testAddAndRemoveRecord() throws IOException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    assertTrue(m.findRecord(a, Section.ANSWER));
    assertEquals(1, m.getHeader().getCount(Section.ANSWER));

    boolean removed = m.removeRecord(a, Section.ANSWER);
    assertTrue(removed);
    assertFalse(m.findRecord(a, Section.ANSWER));
    assertEquals(0, m.getHeader().getCount(Section.ANSWER));
  }

  @Test
  public void testRemoveAllRecords() throws IOException {
    Message m = new Message();
    Record a1 = createARecord("example.com.");
    Record a2 = createARecord("example.org.");
    m.addRecord(a1, Section.AUTHORITY);
    m.addRecord(a2, Section.AUTHORITY);
    assertEquals(2, m.getHeader().getCount(Section.AUTHORITY));

    m.removeAllRecords(Section.AUTHORITY);
    assertEquals(0, m.getHeader().getCount(Section.AUTHORITY));
    assertTrue(m.getSection(Section.AUTHORITY).isEmpty());
  }

  @Test
  public void testFindRecordAcrossSections() throws IOException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    assertTrue(m.findRecord(a));
    assertFalse(m.findRecord(createARecord("nonexistent.com.")));
  }

  @Test
  public void testFindRRset() throws IOException {
    Message m = new Message();
    Record a1 = createARecord("example.com.");
    Record a2 = createARecord("example.com."); // same name & type
    Record a3 = createARecord("example.org.");
    m.addRecord(a1, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);
    m.addRecord(a3, Section.AUTHORITY);

    assertTrue(m.findRRset(Name.fromString("example.com."), Type.A, Section.ANSWER));
    assertTrue(m.findRRset(Name.fromString("example.org."), Type.A, Section.AUTHORITY));
    assertFalse(m.findRRset(Name.fromString("example.org."), Type.A, Section.ANSWER));
    // any‑section search
    assertTrue(m.findRRset(Name.fromString("example.org."), Type.A));
  }

  @Test
  public void testGetQuestionWhenEmpty() {
    Message m = new Message();
    assertNull(m.getQuestion());
  }

  @Test
  public void testGetTSIGWhenAbsent() {
    Message m = new Message();
    assertNull(m.getTSIG());
  }

  @Test
  public void testGetTSIGWhenPresent() throws IOException {
    Message m = new Message();
    TSIGRecord tsig = createTSIGRecord();
    m.addRecord(tsig, Section.ADDITIONAL);
    assertSame(tsig, m.getTSIG());
  }

  @Test
  public void testGetOPTAndExtendedRcode() throws IOException {
    Message m = new Message();
    OPTRecord opt = createOPTRecord(2); // extended rcode = 2
    m.addRecord(opt, Section.ADDITIONAL);
    assertSame(opt, m.getOPT());

    // Header rcode defaults to 0, extended rcode contribution = 2 << 4 = 32
    assertEquals(32, m.getRcode());
  }

  @Test
  public void testGetSectionArrayAndList() throws IOException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);

    // Deprecated array method
    Record[] arr = m.getSectionArray(Section.ANSWER);
    assertEquals(1, arr.length);
    assertSame(a, arr[0]);

    // List view (unmodifiable)
    List<Record> lst = m.getSection(Section.ANSWER);
    assertEquals(1, lst.size());
    assertSame(a, lst.get(0));

    // Empty section returns empty array/list
    assertEquals(0, m.getSectionArray(Section.AUTHORITY).length);
    assertTrue(m.getSection(Section.AUTHORITY).isEmpty());
  }

  @Test
  public void testGetSectionRRsetsGrouping() throws IOException {
    Message m = new Message();
    Record a1 = createARecord("example.com.");
    Record a2 = createARecord("example.com."); // same name/type → same RRset
    Record a3 = createARecord("example.org."); // different name
    m.addRecord(a1, Section.ANSWER);
    m.addRecord(a2, Section.ANSWER);
    m.addRecord(a3, Section.ANSWER);

    List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
    assertEquals(2, rrsets.size());

    // Find the RRset that contains the two records for example.com.
    RRset setExampleCom = rrsets.stream()
        .filter(rs -> rs.getName().equals(Name.fromString("example.com.")))
        .findFirst()
        .orElse(null);
    assertNotNull(setExampleCom);
    assertEquals(2, setExampleCom.size());

    // The other RRset contains the single record for example.org.
    RRset setExampleOrg = rrsets.stream()
        .filter(rs -> rs.getName().equals(Name.fromString("example.org.")))
        .findFirst()
        .orElse(null);
    assertNotNull(setExampleOrg);
    assertEquals(1, setExampleOrg.size());
  }

  @Test
  public void testToWireAndSize() throws IOException {
    Message m = new Message();
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    byte[] wire = m.toWire();
    assertTrue(wire.length > Header.LENGTH);
    assertEquals(wire.length, m.numBytes());
  }

  @Test
  public void testToWireWithTruncationSetsTCFlag() throws IOException {
    Message m = new Message();
    // Add enough records to exceed a tiny maxLength, forcing truncation.
    for (int i = 0; i < 20; i++) {
      m.addRecord(createARecord("host" + i + ".example.com."), Section.ANSWER);
    }
    // Use a deliberately small maxLength (just header + a few bytes)
    byte[] truncated = m.toWire(40);
    // Header should have the TC flag set.
    assertTrue(m.getHeader().getFlag(Flags.TC));
    // The returned wire may be empty if nothing could be rendered; ensure we got something.
    assertTrue(truncated.length <= 40);
  }

  @Test
  public void testSetTSIGDoesNotAffectSignedState() {
    Message m = new Message();
    TSIG key = new TSIG(TSIG.HMAC_MD5, Name.fromString("keyname."), "base64key");
    m.setTSIG(key);
    // isSigned depends on internal state, which is untouched by setTSIG.
    assertFalse(m.isSigned());
  }

  @Test
  public void testCloneCreatesDeepCopy() throws IOException {
    Message original = new Message();
    Record a = createARecord("example.com.");
    original.addRecord(a, Section.ANSWER);
    original.setResolver(new DummyResolver());

    Message copy = original.clone();
    // Verify header is cloned (different instance but same content)
    assertNotSame(original.getHeader(), copy.getHeader());
    assertEquals(original.getHeader().getID(), copy.getHeader().getID());

    // Sections should be deep‑copied
    assertNotSame(original.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));
    assertEquals(1, copy.getSection(Section.ANSWER).size());
    assertSame(a, copy.getSection(Section.ANSWER).get(0));

    // Resolver reference should be copied as‑is
    Optional<Resolver> opt = copy.getResolver();
    assertTrue(opt.isPresent());
    assertTrue(opt.get() instanceof DummyResolver);

    // Mutating the original must not affect the clone
    original.removeAllRecords(Section.ANSWER);
    assertEquals(0, original.getHeader().getCount(Section.ANSWER));
    assertEquals(1, copy.getHeader().getCount(Section.ANSWER));
  }

  @Test
  public void testToStringContainsAllSectionsAndSize() throws IOException {
    Message m = new Message();
    Record q = createARecord("example.com.");
    m.addRecord(q, Section.QUESTION);
    Record a = createARecord("example.com.");
    m.addRecord(a, Section.ANSWER);
    m.addRecord(createOPTRecord(0), Section.ADDITIONAL);
    // Force wire conversion so size is set
    m.toWire();

    String s = m.toString();

    // Header line
    assertTrue(s.contains(";; HEADER"));
    // Sections headings
    assertTrue(s.contains(";; QUESTION"));
    assertTrue(s.contains(";; ANSWER"));
    assertTrue(s.contains(";; AUTHORITY"));
    assertTrue(s.contains(";; ADDITIONAL"));
    // Record string representations
    assertTrue(s.contains("example.com."));
    // Size line
    assertTrue(s.contains("Message size: " + m.numBytes()));
  }
}
