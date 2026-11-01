###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

/**
 * Comprehensive unit tests for {@link Message}.
 *
 * The tests use only the public API that is declared in the supplied source
 * code and the minimal set of DNSJava classes listed in the prompt.
 */
public class MessageTest {

  /** Helper to create a simple A‑record for the given name. */
  private Record aRecord(String name) throws IOException {
    Name n = Name.fromString(name);
    // Record.newRecord(Name, int type, int dclass, long ttl) creates a generic record.
    return Record.newRecord(n, Type.A, DClass.IN, 0);
  }

  /** Helper to create an OPT record with a given extended rcode. */
  private OPTRecord optRecord(int extendedRcode) {
    // payloadSize, xrcode (extended rcode), version, flags
    return new OPTRecord(512, extendedRcode, 0, 0);
  }

  /** Helper to create a very small dummy resolver for setResolver / getResolver tests. */
  private static class DummyResolver implements Resolver {
    @Override public void setPort(int port) {}
    @Override public void setTCP(boolean flag) {}
    @Override public void setIgnoreTruncation(boolean flag) {}
    @Override public void setEDNS(int version) {}
    @Override public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
    @Override public void setEDNS(int version, int payloadSize, int flags, EDNSOption... options) {}
    @Override public void setTSIGKey(TSIG key) {}
    @Override public void setTimeout(Duration timeout) {}
    @Override public void setTimeout(int secs, int msecs) {}
    @Override public void setTimeout(int secs) {}
    @Override public Message send(Message query) throws IOException { return null; }
    @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
    @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query,
        java.util.concurrent.Executor executor) { return null; }
    @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
  }

  @Test
  public void testAddFindAndRemoveRecord() throws IOException {
    Message msg = new Message(0x1234);
    Record rec = aRecord("example.com.");

    // initially not present
    assertFalse(msg.findRecord(rec, Section.ANSWER));
    assertFalse(msg.findRecord(rec));

    // add to ANSWER section
    msg.addRecord(rec, Section.ANSWER);
    assertTrue(msg.findRecord(rec, Section.ANSWER));
    assertTrue(msg.findRecord(rec));
    assertEquals(1, msg.getHeader().getCount(Section.ANSWER));

    // remove the record
    assertTrue(msg.removeRecord(rec, Section.ANSWER));
    assertFalse(msg.findRecord(rec, Section.ANSWER));
    assertEquals(0, msg.getHeader().getCount(Section.ANSWER));

    // removing a non‑existent record returns false
    assertFalse(msg.removeRecord(rec, Section.ANSWER));
  }

  @Test
  public void testRemoveAllRecords() throws IOException {
    Message msg = new Message();
    Record rec1 = aRecord("a.example.");
    Record rec2 = aRecord("b.example.");

    msg.addRecord(rec1, Section.AUTHORITY);
    msg.addRecord(rec2, Section.AUTHORITY);
    assertEquals(2, msg.getHeader().getCount(Section.AUTHORITY));

    msg.removeAllRecords(Section.AUTHORITY);
    assertEquals(0, msg.getHeader().getCount(Section.AUTHORITY));
    assertTrue(msg.getSection(Section.AUTHORITY).isEmpty());
  }

  @Test
  public void testFindRRset() throws IOException {
    Message msg = new Message();
    Name n = Name.fromString("host.example.");
    Record r1 = Record.newRecord(n, Type.A, DClass.IN, 0);
    Record r2 = Record.newRecord(n, Type.A, DClass.IN, 0);
    Record r3 = Record.newRecord(Name.fromString("other.example."), Type.A, DClass.IN, 0);

    msg.addRecord(r1, Section.ANSWER);
    msg.addRecord(r3, Section.ANSWER);
    // same name/type as r1 but added later
    msg.addRecord(r2, Section.ANSWER);

    assertTrue(msg.findRRset(n, Type.A, Section.ANSWER));
    assertFalse(msg.findRRset(Name.fromString("missing.example."), Type.A, Section.ANSWER));
  }

  @Test
  public void testGetQuestion() throws IOException {
    Message msg = new Message();
    Record q = aRecord("question.example.");
    msg.addRecord(q, Section.QUESTION);
    assertEquals(q, msg.getQuestion());

    // empty QUESTION section returns null
    Message empty = new Message();
    assertNull(empty.getQuestion());
  }

  @Test
  public void testGetOPTAndRcode() throws IOException {
    Message msg = new Message();
    // set a base rcode
    msg.getHeader().setRcode(Rcode.SERVFAIL);
    // add an OPT record with extended rcode = 1
    OPTRecord opt = optRecord(1);
    msg.addRecord(opt, Section.ADDITIONAL);

    // getOPT should return the same instance
    assertSame(opt, msg.getOPT());

    // getRcode adds extended rcode << 4
    int expected = Rcode.SERVFAIL + (1 << 4);
    assertEquals(expected, msg.getRcode());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testGetSectionArrayDeprecated() throws IOException {
    Message msg = new Message();
    // empty section should return the shared empty array
    Record[] empty = msg.getSectionArray(Section.ANSWER);
    assertNotNull(empty);
    assertEquals(0, empty.length);

    // after adding a record the array should contain it
    Record rec = aRecord("array.example.");
    msg.addRecord(rec, Section.ANSWER);
    Record[] arr = msg.getSectionArray(Section.ANSWER);
    assertEquals(1, arr.length);
    assertEquals(rec, arr[0]);
  }

  @Test
  public void testGetSectionAndSectionRRsets() throws IOException {
    Message msg = new Message();
    Name n = Name.fromString("rrset.example.");
    Record r1 = Record.newRecord(n, Type.A, DClass.IN, 0);
    Record r2 = Record.newRecord(n, Type.A, DClass.IN, 0);
    Record r3 = Record.newRecord(Name.fromString("other.example."), Type.A, DClass.IN, 0);

    msg.addRecord(r1, Section.ANSWER);
    msg.addRecord(r2, Section.ANSWER);
    msg.addRecord(r3, Section.ANSWER);

    List<Record> list = msg.getSection(Section.ANSWER);
    assertEquals(3, list.size());
    assertTrue(list.contains(r1));
    assertTrue(list.contains(r2));
    assertTrue(list.contains(r3));

    List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
    // Two RRsets: one for name n (containing r1 & r2), one for other.example.
    assertEquals(2, rrsets.size());

    // Verify that the RRset for n contains both records
    RRset setForN = null;
    for (RRset rs : rrsets) {
      if (rs.getName().equals(n) && rs.getType() == Type.A) {
        setForN = rs;
        break;
      }
    }
    assertNotNull(setForN);
    assertEquals(2, setForN.size());
  }

  @Test
  public void testToWireAndNumBytes() throws IOException {
    Message msg = new Message();
    msg.addRecord(aRecord("wire.example."), Section.QUESTION);
    byte[] wire = msg.toWire();
    assertNotNull(wire);
    assertTrue(wire.length > 0);
    // numBytes should reflect the size of the generated wire format
    assertEquals(wire.length, msg.numBytes());
  }

  @Test
  public void testToWireWithTruncation() throws IOException {
    Message msg = new Message();
    // Create a record with a very large rdata payload to force truncation.
    // Use a generic record with a large byte array as rdata.
    Name n = Name.fromString("big.example.");
    byte[] hugeData = new byte[Message.MAXLENGTH]; // fill with zeros
    Record big = Record.newRecord(n, Type.TXT, DClass.IN, 0, hugeData);
    msg.addRecord(big, Section.ANSWER);

    // Use a maxLength far smaller than the message size to trigger truncation.
    byte[] truncated = msg.toWire(100);
    // The truncated wire should be non‑empty and its header TC flag must be set.
    assertTrue(truncated.length > 0);
    // Verify the TC flag (bit 9) in the header flags byte.
    // Header format: first 12 bytes are the DNS header; bytes 2‑3 contain flags.
    int flags = ((truncated[2] & 0xFF) << 8) | (truncated[3] & 0xFF);
    assertTrue((flags & Flags.TC) != 0);
  }

  @Test
  public void testCloneIndependence() throws IOException {
    Message original = new Message();
    Record rec = aRecord("clone.example.");
    original.addRecord(rec, Section.ANSWER);

    Message clone = original.clone();
    // The clone should contain the same record initially.
    assertTrue(clone.findRecord(rec, Section.ANSWER));
    assertEquals(original.getHeader().getCount(Section.ANSWER),
                 clone.getHeader().getCount(Section.ANSWER));

    // Modify the original; clone must remain unchanged.
    original.removeAllRecords(Section.ANSWER);
    assertFalse(original.findRecord(rec, Section.ANSWER));
    assertTrue(clone.findRecord(rec, Section.ANSWER));
  }

  @Test
  public void testResolverSetAndGet() {
    Message msg = new Message();
    DummyResolver resolver = new DummyResolver();
    msg.setResolver(resolver);
    Optional<Resolver> opt = msg.getResolver();
    assertTrue(opt.isPresent());
    assertSame(resolver, opt.get());
  }

  @Test
  public void testSectionToStringAndToString() throws IOException {
    Message msg = new Message();
    Record q = aRecord("string.example.");
    msg.addRecord(q, Section.QUESTION);
    String questionStr = msg.sectionToString(Section.QUESTION);
    assertNotNull(questionStr);
    assertTrue(questionStr.contains("string.example."));
    // The full message string should contain the header and the section titles.
    String full = msg.toString();
    assertTrue(full.contains(";; QUESTION"));
    assertTrue(full.contains(";; ANSWER"));
    assertTrue(full.contains(";; AUTHORITY"));
    assertTrue(full.contains(";; ADDITIONAL"));
    // Message size line should be present.
    assertTrue(full.matches("(?s).*;; Message size: \\d+ bytes$"));
  }
}
###Test END##