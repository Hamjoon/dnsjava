###Test START##
```java
/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Message}.
 *
 * The tests are written to exercise every public method of {@code Message},
 * covering normal behaviour, edge cases and expected exception handling.
 *
 * NOTE: The test code relies only on the public API that is listed in the
 * problem statement.  All DNS objects (Name, Record, Header, TSIG, …) are
 * created using their public constructors or factory methods – no mocking
 * or subclassing is performed.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link Message}.
 */
public class MessageTest {

  private Name exampleName;
  private Record aRecord;
  private Record cnameRecord;
  private Record tsigRecord; // a dummy TSIG record (type = TSIG)
  private Header header;

  @Before
  public void setUp() throws Exception {
    exampleName = Name.fromString("example.com.");
    // a simple A record (type = A, class = IN, ttl = 300)
    aRecord = Record.newRecord(exampleName, Type.A, DClass.IN, 300);
    // a simple CNAME record
    cnameRecord = Record.newRecord(exampleName, Type.CNAME, DClass.IN, 300);
    // a dummy TSIG record – we can create it via the TSIGRecord constructor
    tsigRecord = new TSIGRecord(Name.fromString("tsig.example.com."), DClass.IN, 0,
        TSIG.HMAC_MD5, java.util.Date.from(java.time.Instant.now()), 300,
        new byte[]{0x01, 0x02}, 0, Rcode.NOERROR, new byte[0]);

    header = new Header(0x1234);
  }

  /* ---------------------------------------------------------------------- */
  /* 1.  Constructors & static factories                                    */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testDefaultConstructorCreatesHeader() {
    Message m = new Message();
    assertNotNull("Header must be created", m.getHeader());
  }

  @Test
  public void testIdConstructorSetsHeaderId() {
    int id = 0xABCD;
    Message m = new Message(id);
    assertEquals("Header ID must match constructor argument", id, m.getHeader().getID());
  }

  @Test
  public void testNewQueryCreatesMessageWithQuestionAndFlags() {
    Message q = Message.newQuery(aRecord);
    assertEquals("Opcode must be QUERY", Opcode.QUERY, q.getHeader().getOpcode());
    assertTrue("Recursion Desired flag must be set", q.getHeader().getFlag(Flags.RD));
    assertSame("The question record must be stored", aRecord, q.getQuestion());
  }

  @Test
  public void testNewUpdateReturnsUpdateSubclass() {
    Message upd = Message.newUpdate(exampleName);
    assertTrue("newUpdate must return an Update (subclass of Message)",
        upd instanceof Update);
  }

  @Test
  public void testByteArrayConstructorParsesWireFormat() throws IOException {
    // Build a minimal message (header + one question)
    Message original = new Message();
    original.addRecord(aRecord, Section.QUESTION);
    byte[] wire = original.toWire();

    Message parsed = new Message(wire);
    assertEquals("Parsed message must have same ID", original.getHeader().getID(),
                 parsed.getHeader().getID());
    assertEquals("Parsed question must equal original", aRecord, parsed.getQuestion());
  }

  @Test
  public void testByteBufferConstructorParsesWireFormat() throws IOException {
    Message original = new Message();
    original.addRecord(aRecord, Section.QUESTION);
    ByteBuffer buffer = ByteBuffer.wrap(original.toWire());
    Message parsed = new Message(buffer);
    assertEquals("Parsed header ID must match", original.getHeader().getID(),
                 parsed.getHeader().getID());
  }

  /* ---------------------------------------------------------------------- */
  /* 2.  Header getters / setters                                            */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testSetAndGetHeader() {
    Message m = new Message();
    Header newHeader = new Header(0x5555);
    m.setHeader(newHeader);
    assertSame("Header returned by getHeader must be the one set", newHeader, m.getHeader());
  }

  /* ---------------------------------------------------------------------- */
  /* 3.  Record manipulation methods                                        */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAddRecordIncrementsCount() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    assertEquals("Answer count must be 1 after addRecord", 1,
                 m.getHeader().getCount(Section.ANSWER));
    assertTrue("Record must be findable after addition",
               m.findRecord(aRecord, Section.ANSWER));
  }

  @Test
  public void testRemoveRecordDecrementsCount() {
    Message m = new Message();
    m.addRecord(aRecord, Section.AUTHORITY);
    boolean removed = m.removeRecord(aRecord, Section.AUTHORITY);
    assertTrue("removeRecord should return true for existing record", removed);
    assertEquals("Authority count must be 0 after removal", 0,
                 m.getHeader().getCount(Section.AUTHORITY));
  }

  @Test
  public void testRemoveRecordReturnsFalseWhenMissing() {
    Message m = new Message();
    boolean removed = m.removeRecord(aRecord, Section.ADDITIONAL);
    assertFalse("removeRecord should return false when record is absent", removed);
  }

  @Test
  public void testRemoveAllRecordsClearsSection() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    m.addRecord(cnameRecord, Section.ANSWER);
    m.removeAllRecords(Section.ANSWER);
    assertEquals("Count must be zero after removeAllRecords", 0,
                 m.getHeader().getCount(Section.ANSWER));
    assertTrue("Section list must be empty", m.getSection(Section.ANSWER).isEmpty());
  }

  @Test
  public void testFindRecordAcrossAllSections() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    m.addRecord(cnameRecord, Section.AUTHORITY);
    assertTrue("findRecord should locate record in any section", m.findRecord(aRecord));
    assertTrue("findRecord should locate record in any section", m.findRecord(cnameRecord));
  }

  @Test
  public void testFindRRsetPositiveAndNegative() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    assertTrue("RRset for name/type A must be found", m.findRRset(exampleName, Type.A, Section.ANSWER));
    assertFalse("RRset for non‑existing type MX must not be found",
                m.findRRset(exampleName, Type.MX, Section.ANSWER));
  }

  @Test
  public void testGetQuestionWhenEmptyReturnsNull() {
    Message m = new Message();
    assertNull("getQuestion must return null when no question is present", m.getQuestion());
  }

  @Test
  public void testGetTSIGWhenAbsentReturnsNull() {
    Message m = new Message();
    assertNull("getTSIG must return null when no TSIG record is present", m.getTSIG());
  }

  @Test
  public void testGetTSIGWhenPresent() {
    Message m = new Message();
    m.addRecord(tsigRecord, Section.ADDITIONAL);
    TSIGRecord retrieved = m.getTSIG();
    assertNotNull("TSIG record must be returned", retrieved);
    assertEquals("Returned TSIG must be the one added",
                 tsigRecord.getName(), retrieved.getName());
  }

  @Test
  public void testIsSignedAndIsVerifiedDefault() {
    Message m = new Message();
    assertFalse("Message should not be signed initially", m.isSigned());
    assertFalse("Message should not be verified initially", m.isVerified());
  }

  @Test
  public void testGetOPTAndExtendedRcode() {
    Message m = new Message();
    // Create an OPT record with extended rcode = 2 (i.e. 2 << 4)
    OPTRecord opt = new OPTRecord(4096, 2, 0, 0);
    m.addRecord(opt, Section.ADDITIONAL);
    assertSame("getOPT must return the OPT record added", opt, m.getOPT());

    // Set base rcode to 1 (FORMERR) and expect combined value 1 + (2 << 4) = 33
    m.getHeader().setRcode(Rcode.FORMERR);
    assertEquals("Extended rcode must be combined correctly",
                 Rcode.FORMERR + (2 << 4), m.getRcode());
  }

  @Test
  public void testGetSectionArrayAndGetSection() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    Record[] arr = m.getSectionArray(Section.ANSWER);
    assertEquals("Array length must be 1", 1, arr.length);
    assertSame("Array element must be the added record", aRecord, arr[0]);

    List<Record> list = m.getSection(Section.ANSWER);
    assertEquals("List size must be 1", 1, list.size());
    assertSame("List element must be the added record", aRecord, list.get(0));

    // The returned list must be unmodifiable – attempting to add throws
    try {
      list.add(cnameRecord);
      fail("Returned section list should be unmodifiable");
    } catch (UnsupportedOperationException expected) {
      // expected
    }
  }

  @Test
  public void testGetSectionRRsetsCreatesRRset() {
    Message m = new Message();
    m.addRecord(aRecord, Section.ANSWER);
    List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
    assertEquals("One RRset must be created", 1, rrsets.size());
    RRset set = rrsets.get(0);
    assertEquals("RRset type must match record type", Type.A, set.getType());
    assertEquals("RRset must contain exactly one record", 1, set.size());
  }

  /* ---------------------------------------------------------------------- */
  /* 4.  Wire format helpers                                                */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testToWireWithoutLengthLimit() {
    Message m = new Message();
    m.addRecord(aRecord, Section.QUESTION);
    byte[] wire = m.toWire();
    assertNotNull("toWire must return a non‑null array", wire);
    assertTrue("Wire length must be > 0", wire.length > 0);
    // The size reported by numBytes must match the length of the wire array
    assertEquals("numBytes must equal wire length", wire.length, m.numBytes());
  }

  @Test
  public void testToWireWithTooSmallMaxLengthReturnsEmptyArray() {
    Message m = new Message();
    m.addRecord(aRecord, Section.QUESTION);
    // Header length is Header.LENGTH (public static). Use a value smaller than that.
    int tooSmall = Header.LENGTH - 1;
    byte[] wire = m.toWire(tooSmall);
    assertEquals("When maxLength < Header.LENGTH, toWire must return empty array", 0, wire.length);
  }

  @Test
  public void testToWireWithTruncationSetsTCFlag() {
    Message m = new Message();
    // Fill the message with many records so that it will not fit into a short limit.
    for (int i = 0; i < 200; i++) {
      Name n = Name.fromString("host" + i + ".example.com.");
      Record r = Record.newRecord(n, Type.A, DClass.IN, 300);
      m.addRecord(r, Section.ANSWER);
    }
    // Choose a max length that forces truncation (e.g. 512 bytes, typical DNS limit)
    byte[] wire = m.toWire(512);
    // The TC flag is bit 1 of the flags byte (Flags.TC)
    boolean tc = (wire[2] & Flags.TC) != 0;
    assertTrue("TC flag must be set when truncation occurs", tc);
  }

  /* ---------------------------------------------------------------------- */
  /* 5.  TSIG handling                                                      */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testSetTSIGAndIsSignedAfterWire() throws IOException {
    Message m = new Message();
    m.addRecord(aRecord, Section.QUESTION);
    // Use a simple TSIG key (algorithm HMAC_MD5, name "key.example.", key "abcd")
    TSIG key = new TSIG(TSIG.HMAC_MD5, Name.fromString("key.example."), "abcd");
    m.setTSIG(key);
    // Rendering the message with a sufficient length will cause the TSIG to be generated
    byte[] wire = m.toWire(1500);
    // After rendering, the internal state changes to TSIG_SIGNED
    assertTrue("Message should be considered signed after toWire with TSIG", m.isSigned());
    // The generated TSIG record can be retrieved via the private getter via reflection
    // (reflection is acceptable in a test environment)
    try {
      java.lang.reflect.Method genMethod = Message.class.getDeclaredMethod("getGeneratedTSIG");
      genMethod.setAccessible(true);
      TSIGRecord generated = (TSIGRecord) genMethod.invoke(m);
      assertNotNull("Generated TSIG record must be present", generated);
    } catch (Exception e) {
      fail("Reflection error while accessing getGeneratedTSIG: " + e);
    }
  }

  /* ---------------------------------------------------------------------- */
  /* 6.  Resolver association                                                */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testSetAndGetResolver() {
    Message m = new Message();
    Resolver dummy = new Resolver() {
      // All methods are no‑ops – we only need an instance.
      public void setPort(int port) {}
      public void setTCP(boolean flag) {}
      public void setIgnoreTruncation(boolean flag) {}
      public void setEDNS(int version) {}
      public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
      public void setEDNS(int version, int payloadSize, int flags, EDNSOption... options) {}
      public void setTSIGKey(TSIG key) {}
      public void setTimeout(int secs, int msecs) {}
      public void setTimeout(int secs) {}
      public void setTimeout(Duration timeout) {}
      public Duration getTimeout() { return Duration.ZERO; }
      public Message send(Message query) throws IOException { return null; }
      public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
      public java.util.concurrent.CompletionStage<Message> sendAsync(Message query,
                                                                    java.util.concurrent.Executor executor) { return null; }
      public Object sendAsync(Message query, ResolverListener listener) { return null; }
    };
    m.setResolver(dummy);
    Optional<Resolver> opt = m.getResolver();
    assertTrue("Resolver must be present after setResolver", opt.isPresent());
    assertSame("Returned resolver must be the same instance", dummy, opt.get());
  }

  /* ---------------------------------------------------------------------- */
  /* 7.  Section validation (edge cases)                                    */
  /* ---------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testFindRecordInvalidSectionThrows() {
    Message m = new Message();
    // Section.check throws IllegalArgumentException for out‑of‑range values
    m.findRecord(aRecord, -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindRRsetInvalidSectionThrows() {
    Message m = new Message();
    m.findRRset(exampleName, Type.A, 10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetSectionArrayInvalidSectionThrows() {
    Message m = new Message();
    m.getSectionArray(-5);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetSectionInvalidSectionThrows() {
    Message m = new Message();
    m.getSection(999);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSectionToStringInvalidSectionThrows() {
    Message m = new Message();
    m.sectionToString(-2);
  }

  /* ---------------------------------------------------------------------- */
  /* 8.  clone() – shallow vs deep copy                                      */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testCloneCreatesIndependentCopy() throws Exception {
    Message original = new Message();
    original.addRecord(aRecord, Section.ANSWER);
    original.addRecord(cnameRecord, Section.AUTHORITY);
    Message copy = original.clone();

    // The two messages must be equal in content but not the same object
    assertNotSame("clone must return a different Message instance", original, copy);
    assertEquals("Header must be equal after cloning",
                 original.getHeader().getID(), copy.getHeader().getID());

    // Modifying the original must not affect the clone
    original.removeAllRecords(Section.ANSWER);
    assertEquals("Original answer count must be 0 after removal",
                 0, original.getHeader().getCount(Section.ANSWER));
    assertEquals("Clone must still have the original answer record",
                 1, copy.getHeader().getCount(Section.ANSWER));
  }

  /* ---------------------------------------------------------------------- */
  /* 9.  isTypeAllowedInSection – edge cases                               */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testIsTypeAllowedInSectionDefaultBehaviour() throws Exception {
    Message m = new Message();
    // By default unknown types are disallowed in ADDITIONAL (hardening enabled)
    boolean allowed = (boolean) java.lang.reflect.Method.class
        .cast(Message.class.getDeclaredMethod("isTypeAllowedInSection", int.class, int.class))
        .invoke(m, Type.SOA, Section.ADDITIONAL);
    assertFalse("SOA should not be allowed in ADDITIONAL by default", allowed);
  }

  @Test
  public void testIsTypeAllowedInSectionWhenSystemPropertyDisablesHarden() throws Exception {
    // Disable the hardening property for this test
    System.setProperty("dnsjava.harden_unknown_additional", "false");
    Message m = new Message();
    boolean allowed = (boolean) java.lang.reflect.Method.class
        .cast(Message.class.getDeclaredMethod("isTypeAllowedInSection", int.class, int.class))
        .invoke(m, Type.SOA, Section.ADDITIONAL);
    assertTrue("When hardening is disabled, unknown types are allowed", allowed);
    // Clean up
    System.clearProperty("dnsjava.harden_unknown_additional");
  }

  /* ---------------------------------------------------------------------- */
  /* 10. normalize – simple successful path (no DNAME/SIG handling)        */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testNormalizeReturnsIdenticalMessageWhenNoNormalizationNeeded() throws Exception {
    Message query = Message.newQuery(aRecord);
    Message response = new Message();
    response.addRecord(aRecord, Section.ANSWER);
    response.addRecord(cnameRecord, Section.AUTHORITY);
    // The response has NOERROR rcode by default
    Message normalized = response.normalize(query);
    assertNotNull("normalize should not return null for a valid response", normalized);
    // The normalized message should contain the same records
    assertTrue("Answer section must still contain the A record",
               normalized.findRecord(aRecord, Section.ANSWER));
    assertTrue("Authority section must still contain the CNAME record",
               normalized.findRecord(cnameRecord, Section.AUTHORITY));
  }

  /* ---------------------------------------------------------------------- */
  /* 11. toString – basic sanity check                                       */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testToStringContainsHeaderAndSections() {
    Message m = new Message();
    m.addRecord(aRecord, Section.QUESTION);
    String txt = m.toString();
    assertTrue("toString must contain the header ID",
               txt.contains(Integer.toString(m.getHeader().getID())));
    assertTrue("toString must contain the string representation of the question record",
               txt.contains("example.com."));
  }
}
```
###Test END##