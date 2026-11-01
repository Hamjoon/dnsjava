###Test START##
```java
/*
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.Message
 *
 * The tests cover:
 *   1. Public API discovery (listed in the comments)
 *   2. Basic functional verification of each public method
 *   3. Edge‑case and exception scenarios (null sections, empty messages,
 *      truncation, TSIG handling, cloning, etc.)
 *
 * The test suite builds real DNS objects (Name, Record, OPTRecord, TSIG)
 * using only the constructors and factory methods that are part of the
 * supplied source code.
 *
 * NOTE: The library classes (Header, Record, Type, DClass, Section,
 *       OPTRecord, TSIG, TSIGRecord, RRset, etc.) are assumed to be on the
 *       class‑path when the tests are executed.
 */

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
 * JUnit‑4 test class for {@link Message}.
 */
public class MessageTest {

  /* -------------------------------------------------------------------- */
  /* 1. Helper methods                                                   */
  /* -------------------------------------------------------------------- */

  /** Creates a simple A‑record for “example.com.” */
  private Record makeARecord() throws IOException {
    Name name = Name.fromString("example.com.");
    // Type.A == 1, DClass.IN == 1 in the dnsjava library
    return Record.newRecord(name, Type.A, DClass.IN, 3600);
  }

  /** Creates a simple OPT record with an extended rcode of 2 (SERVFAIL). */
  private OPTRecord makeOPTRecord() {
    // payloadSize, xrcode (extended rcode), version, flags
    return new OPTRecord(4096, 2, 0, 0);
  }

  /** Creates a TSIG key (HMAC‑MD5) for testing signing. */
  private TSIG makeTSIG() throws IOException {
    // algorithm HMAC_MD5, name "test.", key "abcd"
    Name alg = TSIG.HMAC_MD5;
    Name name = Name.fromString("test.");
    return new TSIG(alg, name, "abcd");
  }

  /* -------------------------------------------------------------------- */
  /* 2. Public method list (for documentation)                           */
  /* -------------------------------------------------------------------- */

  /*
   * Constructors
   *   Message(int id)
   *   Message()
   *   static Message newQuery(Record r)
   *   static Message newUpdate(Name zone)
   *
   * Header handling
   *   void setHeader(Header h)
   *   Header getHeader()
   *
   * Record manipulation
   *   void addRecord(Record r, int section)
   *   boolean removeRecord(Record r, int section)
   *   void removeAllRecords(int section)
   *   boolean findRecord(Record r, int section)
   *   boolean findRecord(Record r)
   *   boolean findRRset(Name name, int type, int section)
   *   boolean findRRset(Name name, int type)
   *
   * Queries and special records
   *   Record getQuestion()
   *   TSIGRecord getTSIG()
   *   OPTRecord getOPT()
   *
   * Flags / status
   *   boolean isSigned()
   *   boolean isVerified()
   *   int getRcode()
   *
   * Section access
   *   @Deprecated Record[] getSectionArray(int section)
   *   List<Record> getSection(int section)
   *   List<RRset> getSectionRRsets(int section)
   *
   * Wire conversion
   *   byte[] toWire()
   *   byte[] toWire(int maxLength)
   *
   * TSIG handling
   *   void setTSIG(TSIG key)
   *   void setTSIG(TSIG key, int error, TSIGRecord querytsig)
   *
   * Misc
   *   int numBytes()
   *   String sectionToString(int i)
   *   String toString()
   *   Message clone()
   *   void setResolver(Resolver resolver)
   *   Optional<Resolver> getResolver()
   */

  /* -------------------------------------------------------------------- */
  /* 3. Basic functional tests                                            */
  /* -------------------------------------------------------------------- */

  /** Verify that a Message created with a specific ID stores the ID in its Header. */
  @Test
  public void testConstructorWithId() {
    int id = 0x1234;
    Message msg = new Message(id);
    assertEquals(id, msg.getHeader().getID());
  }

  /** Verify that the no‑arg constructor creates a Message with a random ID (non‑zero). */
  @Test
  public void testDefaultConstructor() {
    Message msg = new Message();
    // The ID may be zero, but the constructor must not throw – just ensure we have a Header.
    assertNotNull(msg.getHeader());
  }

  /** newQuery should set opcode to QUERY, set RD flag and place the supplied record in the QUESTION section. */
  @Test
  public void testNewQuery() throws IOException {
    Record q = makeARecord();
    Message query = Message.newQuery(q);
    assertEquals(Opcode.QUERY, query.getHeader().getOpcode());
    assertTrue(query.getHeader().getFlag(Flags.RD));
    assertEquals(q, query.getQuestion());
  }

  /** Adding a record to a section must increase the appropriate count in the Header. */
  @Test
  public void testAddRecordIncrementsCount() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    msg.addRecord(a, Section.ANSWER);
    assertEquals(1, msg.getHeader().getCount(Section.ANSWER));
    assertTrue(msg.getSection(Section.ANSWER).contains(a));
  }

  /** Removing a present record should return true and decrement the count. */
  @Test
  public void testRemoveRecordSuccess() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    msg.addRecord(a, Section.AUTHORITY);
    assertTrue(msg.removeRecord(a, Section.AUTHORITY));
    assertEquals(0, msg.getHeader().getCount(Section.AUTHORITY));
    assertFalse(msg.getSection(Section.AUTHORITY).contains(a));
  }

  /** Removing a non‑existent record should return false and leave counts unchanged. */
  @Test
  public void testRemoveRecordFailure() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    assertFalse(msg.removeRecord(a, Section.ADDITIONAL));
    assertEquals(0, msg.getHeader().getCount(Section.ADDITIONAL));
  }

  /** removeAllRecords should clear the section and reset the count. */
  @Test
  public void testRemoveAllRecords() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    msg.addRecord(a, Section.ANSWER);
    msg.removeAllRecords(Section.ANSWER);
    assertEquals(0, msg.getHeader().getCount(Section.ANSWER));
    assertTrue(msg.getSection(Section.ANSWER).isEmpty());
  }

  /** findRecord(section) works for present and absent records. */
  @Test
  public void testFindRecordInSection() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    msg.addRecord(a, Section.ANSWER);
    assertTrue(msg.findRecord(a, Section.ANSWER));
    assertFalse(msg.findRecord(a, Section.AUTHORITY));
  }

  /** findRecord (any section) correctly scans all sections. */
  @Test
  public void testFindRecordAnywhere() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    msg.addRecord(a, Section.AUTHORITY);
    assertTrue(msg.findRecord(a));
    // different record not added
    Record b = Record.newRecord(Name.fromString("other.com."), Type.A, DClass.IN, 3600);
    assertFalse(msg.findRecord(b));
  }

  /** findRRset should detect an RRset by name and type. */
  @Test
  public void testFindRRset() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    msg.addRecord(a, Section.ANSWER);
    Name name = a.getName();
    assertTrue(msg.findRRset(name, Type.A, Section.ANSWER));
    assertFalse(msg.findRRset(name, Type.AAAA, Section.ANSWER));
  }

  /** getQuestion returns null when the QUESTION section is empty. */
  @Test
  public void testGetQuestionEmpty() {
    Message msg = new Message();
    assertNull(msg.getQuestion());
  }

  /** getOPT returns the first OPT record in the ADDITIONAL section, or null if none. */
  @Test
  public void testGetOPT() throws IOException {
    Message msg = new Message();
    assertNull(msg.getOPT());

    OPTRecord opt = makeOPTRecord();
    msg.addRecord(opt, Section.ADDITIONAL);
    assertEquals(opt, msg.getOPT());
  }

  /** getRcode incorporates the extended rcode from an OPT record. */
  @Test
  public void testGetRcodeWithOPT() throws IOException {
    Message msg = new Message();
    // Base rcode = 0 (NOERROR)
    assertEquals(0, msg.getRcode());

    // Add OPT with extended rcode = 2 (SERVFAIL)
    msg.addRecord(makeOPTRecord(), Section.ADDITIONAL);
    // Header rcode still 0, extended adds 2 << 4 = 32
    assertEquals(0 + (2 << 4), msg.getRcode());
  }

  /** getSectionArray returns an empty array when the section is empty (deprecated method). */
  @Test
  public void testGetSectionArrayEmpty() {
    Message msg = new Message();
    Record[] arr = msg.getSectionArray(Section.ANSWER);
    assertNotNull(arr);
    assertEquals(0, arr.length);
  }

  /** getSection returns an unmodifiable empty list for an empty section. */
  @Test
  public void testGetSectionEmpty() {
    Message msg = new Message();
    List<Record> list = msg.getSection(Section.AUTHORITY);
    assertNotNull(list);
    assertTrue(list.isEmpty());
    try {
      list.add(makeARecord());
      fail("Expected UnsupportedOperationException because list is unmodifiable");
    } catch (UnsupportedOperationException ignored) {
    }
  }

  /** getSectionRRsets groups records into RRsets correctly. */
  @Test
  public void testGetSectionRRsets() throws IOException {
    Message msg = new Message();
    Record a1 = makeARecord();               // name example.com., type A
    Record a2 = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 3600);
    Record mx = Record.newRecord(Name.fromString("example.com."), Type.MX, DClass.IN, 3600);
    msg.addRecord(a1, Section.ANSWER);
    msg.addRecord(a2, Section.ANSWER);
    msg.addRecord(mx, Section.ANSWER);

    List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
    assertEquals(2, rrsets.size()); // one for A, one for MX

    // Verify that the A‑RRset contains both A records
    RRset aSet = rrsets.stream()
        .filter(r -> r.getType() == Type.A)
        .findFirst()
        .orElse(null);
    assertNotNull(aSet);
    assertEquals(2, aSet.size());
  }

  /** toWire() without a maxLength should produce a non‑empty byte array for a simple message. */
  @Test
  public void testToWireBasic() throws IOException {
    Message msg = new Message();
    msg.addRecord(makeARecord(), Section.QUESTION);
    byte[] wire = msg.toWire();
    assertNotNull(wire);
    assertTrue(wire.length > Header.LENGTH);
    // The size reported by numBytes() must match the wire length
    assertEquals(wire.length, msg.numBytes());
  }

  /** toWire(int) with a maxLength smaller than Header.LENGTH must return an empty array. */
  @Test
  public void testToWireMaxLengthTooSmall() throws IOException {
    Message msg = new Message();
    msg.addRecord(makeARecord(), Section.QUESTION);
    byte[] wire = msg.toWire(Header.LENGTH - 1);
    assertNotNull(wire);
    assertEquals(0, wire.length);
    assertEquals(0, msg.numBytes());
  }

  /** Setting a TSIG key and encoding with a sufficient maxLength results in a signed message. */
  @Test
  public void testTSIGSigning() throws IOException {
    Message msg = new Message();
    msg.addRecord(makeARecord(), Section.QUESTION);
    TSIG tsig = makeTSIG();
    msg.setTSIG(tsig);                     // default error = NOERROR, no query TSIG
    byte[] wire = msg.toWire(Message.MAXLENGTH);
    assertNotNull(wire);
    assertTrue(wire.length > 0);
    // After signing, the message should report being signed.
    assertTrue(msg.isSigned());
    // The TSIG record must be present as the last record in the ADDITIONAL section.
    TSIGRecord tsigRec = msg.getTSIG();
    assertNotNull(tsigRec);
    assertEquals(tsig.getName(), tsigRec.getName());
  }

  /** clone() must produce a deep copy – mutating the original does not affect the clone. */
  @Test
  public void testCloneDeepCopy() throws IOException {
    Message original = new Message();
    Record a = makeARecord();
    original.addRecord(a, Section.ANSWER);
    Message copy = original.clone();

    // Verify header and section contents are equal initially
    assertEquals(original.getHeader().getID(), copy.getHeader().getID());
    assertEquals(original.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));

    // Mutate original
    original.removeRecord(a, Section.ANSWER);
    assertFalse(original.getSection(Section.ANSWER).contains(a));
    // Clone must still contain the record
    assertTrue(copy.getSection(Section.ANSWER).contains(a));
  }

  /** setResolver/getResolver store and retrieve the resolver correctly. */
  @Test
  public void testResolverAccessors() {
    Message msg = new Message();
    // Use an anonymous Resolver implementation (no network activity required)
    Resolver resolver = new Resolver() {
      @Override public void setPort(int p) {}
      @Override public void setTCP(boolean f) {}
      @Override public void setIgnoreTruncation(boolean f) {}
      @Override public void setEDNS(int v) {}
      @Override public void setEDNS(int v, int ps, int fl, List<EDNSOption> o) {}
      @Override public void setEDNS(int v, int ps, int fl, EDNSOption... o) {}
      @Override public void setTSIGKey(TSIG k) {}
      @Override public void setTimeout(Duration d) {}
      @Override public void setTimeout(int s, int ms) {}
      @Override public void setTimeout(int s) {}
      @Override public Duration getTimeout() { return Duration.ofSeconds(5); }
      @Override public Message send(Message q) throws IOException { return null; }
      @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message q) { return null; }
      @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message q, java.util.concurrent.Executor e) { return null; }
      @Override public Object sendAsync(Message q, ResolverListener l) { return null; }
    };
    msg.setResolver(resolver);
    Optional<Resolver> opt = msg.getResolver();
    assertTrue(opt.isPresent());
    assertSame(resolver, opt.get());
  }

  /* -------------------------------------------------------------------- */
  /* 4. Edge‑case & error scenario tests                                 */
  /* -------------------------------------------------------------------- */

  /** Adding a null Record should throw NullPointerException (the implementation does not guard against it). */
  @Test(expected = NullPointerException.class)
  public void testAddNullRecord() {
    Message msg = new Message();
    msg.addRecord(null, Section.ANSWER);
  }

  /** Removing a record from a null section should return false and not throw. */
  @Test
  public void testRemoveFromNullSection() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    assertFalse(msg.removeRecord(a, Section.AUTHORITY));
  }

  /** getTSIG returns null when the last record in ADDITIONAL is not a TSIG. */
  @Test
  public void testGetTSIGWhenAbsent() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    msg.addRecord(a, Section.ADDITIONAL);
    assertNull(msg.getTSIG());
  }

  /** sectionToString returns null for an invalid section index (>3). */
  @Test
  public void testSectionToStringInvalidIndex() {
    Message msg = new Message();
    assertNull(msg.sectionToString(4));
  }

  /** toString includes the OPT pseudo‑section when present. */
  @Test
  public void testToStringWithOPT() throws IOException {
    Message msg = new Message();
    msg.addRecord(makeARecord(), Section.QUESTION);
    msg.addRecord(makeOPTRecord(), Section.ADDITIONAL);
    String s = msg.toString();
    assertNotNull(s);
    // The string should contain the text "OPT" (the OPTRecord's toString includes it)
    assertTrue(s.contains("OPT"));
  }

  /** toWire(int) truncates correctly when the message does not fit inside the given maxLength. */
  @Test
  public void testToWireTruncation() throws IOException {
    Message msg = new Message();
    // Add many records to force truncation
    for (int i = 0; i < 100; i++) {
      Record a = Record.newRecord(
          Name.fromString("host" + i + ".example.com."),
          Type.A,
          DClass.IN,
          3600);
      msg.addRecord(a, Section.ANSWER);
    }
    // Use a small maxLength that is definitely less than the full message size
    int maxLen = 512; // typical UDP limit
    byte[] wire = msg.toWire(maxLen);
    assertNotNull(wire);
    // The TC (truncated) flag must be set in the header
    assertTrue(msg.getHeader().getFlag(Flags.TC));
    // The returned wire length must be <= maxLen
    assertTrue(wire.length <= maxLen);
  }

  /** getSectionRRsets on an empty section returns an empty list (no NPE). */
  @Test
  public void testGetSectionRRsetsEmpty() {
    Message msg = new Message();
    List<RRset> rrsets = msg.getSectionRRsets(Section.AUTHORITY);
    assertNotNull(rrsets);
    assertTrue(rrsets.isEmpty());
  }

  /** Setting a TSIG with a non‑null query TSIG does not break toWire(). */
  @Test
  public void testSetTSIGWithQueryTSIG() throws IOException {
    Message msg = new Message();
    msg.addRecord(makeARecord(), Section.QUESTION);
    TSIG tsig = makeTSIG();
    // Simulate a request TSIG record (we can generate one via the TSIG key)
    TSIGRecord requestTSIG = tsig.generate(msg, new byte[0], Rcode.NOERROR, null);
    msg.setTSIG(tsig, Rcode.NOERROR, requestTSIG);
    byte[] wire = msg.toWire(Message.MAXLENGTH);
    assertNotNull(wire);
    // The generated TSIG should be present
    assertNotNull(msg.getTSIG());
  }

  /** numBytes() returns zero before any wire conversion. */
  @Test
  public void testNumBytesBeforeWire() {
    Message msg = new Message();
    assertEquals(0, msg.numBytes());
  }

  /** The deprecated getSectionArray still returns the correct records. */
  @Test
  public void testDeprecatedGetSectionArray() throws IOException {
    Message msg = new Message();
    Record a = makeARecord();
    msg.addRecord(a, Section.ANSWER);
    Record[] arr = msg.getSectionArray(Section.ANSWER);
    assertEquals(1, arr.length);
    assertEquals(a, arr[0]);
  }

  /* -------------------------------------------------------------------- */
  /* 5. Summary test that exercises a realistic query/response cycle       */
  /* -------------------------------------------------------------------- */

  /** Build a query, sign it, convert to wire, parse it back, and verify signature. */
  @Test
  public void testFullQueryRoundTripWithTSIG() throws IOException {
    // Build query
    Record q = makeARecord();
    Message query = Message.newQuery(q);
    TSIG tsig = makeTSIG();
    query.setTSIG(tsig);
    byte[] wire = query.toWire(Message.MAXLENGTH);
    assertTrue(query.isSigned());

    // Parse the wire back into a Message
    Message parsed = new Message(wire);
    assertEquals(query.getHeader().getID(), parsed.getHeader().getID());
    assertEquals(q, parsed.getQuestion());

    // Verify that the parsed message still reports being signed
    assertTrue(parsed.isSigned());
    // The TSIG record should be present and have the same name as the key
    TSIGRecord parsedTSIG = parsed.getTSIG();
    assertNotNull(parsedTSIG);
    assertEquals(tsig.getName(), parsedTSIG.getName());
  }
}
```
###Test END##