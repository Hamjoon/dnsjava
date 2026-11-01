###Test START##
```java
/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Message}.
 *
 * <p>This file was produced by a simulated “three‑expert” review process that:
 *
 * <ul>
 *   <li>extracted every public method of {@code Message};
 *   <li>generated a basic test for each method;
 *   <li>identified edge‑case and exception scenarios;
 *   <li>added dedicated tests for those scenarios; and
 *   <li>merged everything into a single, self‑contained test class.
 * </ul>
 *
 * <p>The tests use only the public API that is visible in the supplied source code
 * (and the minimal supporting classes listed in the problem statement). No mocking
 * frameworks are used – all objects are created via real constructors or factory
 * methods.
 *
 * <p>Running the suite requires JUnit‑4 on the classpath together with the
 * {@code dnsjava} library that provides the classes referenced above.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link Message}.
 */
public class MessageTest {

    /* ---------------------------------------------------------------------- */
    /* 1.  List of public methods (extracted from the source)                  */
    /* ---------------------------------------------------------------------- */
    // Constructors
    //   Message(int id)
    //   Message()
    //   static Message newQuery(Record r)
    //   static Message newUpdate(Name zone)
    //   Message(byte[] b) throws IOException
    //   Message(ByteBuffer byteBuffer) throws IOException
    //
    // Header accessors
    //   void setHeader(Header h)
    //   Header getHeader()
    //
    // Record manipulation
    //   void addRecord(Record r, int section)
    //   boolean removeRecord(Record r, int section)
    //   void removeAllRecords(int section)
    //   boolean findRecord(Record r, int section)
    //   boolean findRecord(Record r)
    //   boolean findRRset(Name name, int type, int section)
    //   boolean findRRset(Name name, int type)
    //
    // Section getters
    //   Record getQuestion()
    //   TSIGRecord getTSIG()
    //   TSIGRecord getGeneratedTSIG()
    //   boolean isSigned()
    //   boolean isVerified()
    //   OPTRecord getOPT()
    //   int getRcode()
    //   @Deprecated Record[] getSectionArray(int section)
    //   List<Record> getSection(int section)
    //   List<RRset> getSectionRRsets(int section)
    //
    // Wire conversion
    //   byte[] toWire()
    //   byte[] toWire(int maxLength)
    //   byte[] toWire(int maxLength, boolean truncate) throws MessageSizeExceededException
    //
    // TSIG handling
    //   void setTSIG(TSIG key)
    //   void setTSIG(TSIG key, int error, TSIGRecord querytsig)
    //
    // Misc
    //   int numBytes()
    //   String sectionToString(int section)
    //   @Override String toString()
    //   @Override Message clone()
    //   void setResolver(Resolver resolver)
    //   Optional<Resolver> getResolver()
    //   boolean isTypeAllowedInSection(int type, int section)
    //   Message normalize(Message query)
    //   Message normalize(Message query, boolean throwOnIrrelevantRecord) throws WireParseException

    /* ---------------------------------------------------------------------- */
    /* 2.  Test fixtures                                                       */
    /* ---------------------------------------------------------------------- */
    private Message emptyMessage;          // freshly constructed, no records
    private Name nameExample;              // "example.com."
    private Record aRecord;                // a simple A record
    private Record txtRecord;              // a generic TXT‑type record (used for
                                           // edge‑case tests where the type is
                                           // irrelevant)
    private TSIG tsigKey;                  // a deterministic TSIG key

    @Before
    public void setUp() throws Exception {
        emptyMessage = new Message();               // random ID, empty header
        nameExample = Name.fromString("example.com.");
        aRecord = Record.newRecord(nameExample, Type.A, DClass.IN, 300,
                new byte[] { (byte)192, (byte)0, (byte)2, (byte)1 }); // 192.0.2.1
        txtRecord = Record.newRecord(Name.fromString("txt.example.com."),
                Type.TXT, DClass.IN, 300, new byte[] { 0, 'h', 'i' });

        // Use the built‑in HMAC‑MD5 algorithm; the key material is arbitrary but
        // deterministic so that generated TSIG records are repeatable.
        tsigKey = new TSIG(TSIG.HMAC_MD5,
                Name.fromString("tsig-test-key."),
                "aW5pdGlhbC10c2lnLWtleQ=="); // base64 for "initial-tsig-key"
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Basic tests for each public method                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructorWithId() {
        Message m = new Message(0x1234);
        assertEquals(0x1234, m.getHeader().getID());
    }

    @Test
    public void testDefaultConstructor() {
        Message m = new Message();
        // The default constructor creates a Header with a random ID.
        // We only verify that the Header instance is non‑null.
        assertNotNull(m.getHeader());
    }

    @Test
    public void testNewQueryFactory() {
        Message q = Message.newQuery(aRecord);
        assertEquals(Opcode.QUERY, q.getHeader().getOpcode());
        assertTrue(q.getHeader().getFlag(Flags.RD));
        assertEquals(aRecord, q.getQuestion());
    }

    @Test
    public void testNewUpdateFactory() throws Exception {
        Message upd = Message.newUpdate(nameExample);
        assertTrue(upd instanceof Update);
        assertEquals(nameExample, upd.getSection(Section.QUESTION).get(0).getName());
    }

    @Test
    public void testHeaderAccessors() {
        Header h = new Header(0xABCD);
        emptyMessage.setHeader(h);
        assertSame(h, emptyMessage.getHeader());
    }

    @Test
    public void testAddAndFindRecord() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        assertTrue(emptyMessage.findRecord(aRecord, Section.ANSWER));
        assertTrue(emptyMessage.findRecord(aRecord));
    }

    @Test
    public void testRemoveRecord() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        assertTrue(emptyMessage.removeRecord(aRecord, Section.ANSWER));
        assertFalse(emptyMessage.findRecord(aRecord, Section.ANSWER));
        // Removing a non‑existing record should return false
        assertFalse(emptyMessage.removeRecord(aRecord, Section.ANSWER));
    }

    @Test
    public void testRemoveAllRecords() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        emptyMessage.addRecord(txtRecord, Section.ANSWER);
        emptyMessage.removeAllRecords(Section.ANSWER);
        assertFalse(emptyMessage.findRecord(aRecord, Section.ANSWER));
        assertEquals(0, emptyMessage.getHeader().getCount(Section.ANSWER));
    }

    @Test
    public void testFindRRset() {
        // Build two records with the same name and type to form an RRset
        Record r1 = Record.newRecord(nameExample, Type.A, DClass.IN, 300,
                new byte[] { (byte)10, 0, 0, 1 });
        Record r2 = Record.newRecord(nameExample, Type.A, DClass.IN, 300,
                new byte[] { (byte)10, 0, 0, 2 });
        emptyMessage.addRecord(r1, Section.ANSWER);
        emptyMessage.addRecord(r2, Section.ANSWER);
        assertTrue(emptyMessage.findRRset(nameExample, Type.A, Section.ANSWER));
        assertTrue(emptyMessage.findRRset(nameExample, Type.A));
    }

    @Test
    public void testGetQuestionWhenEmpty() {
        assertNull(emptyMessage.getQuestion());
    }

    @Test
    public void testGetTSIGWhenAbsent() {
        assertNull(emptyMessage.getTSIG());
    }

    @Test
    public void testGeneratedTSIGInitiallyNull() {
        assertNull(emptyMessage.getGeneratedTSIG());
    }

    @Test
    public void testIsSignedAndIsVerifiedDefault() {
        // No TSIG has been attached – both should be false.
        assertFalse(emptyMessage.isSigned());
        assertFalse(emptyMessage.isVerified());
    }

    @Test
    public void testGetOPTAbsent() {
        assertNull(emptyMessage.getOPT());
    }

    @Test
    public void testGetRcodeWithNoOPT() {
        // Header default rcode is 0 (NOERROR)
        assertEquals(0, emptyMessage.getRcode());
    }

    @Test
    @Deprecated
    public void testGetSectionArrayEmpty() {
        Record[] arr = emptyMessage.getSectionArray(Section.ANSWER);
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test
    public void testGetSectionEmpty() {
        List<Record> sec = emptyMessage.getSection(Section.ANSWER);
        assertNotNull(sec);
        assertTrue(sec.isEmpty());
    }

    @Test
    public void testGetSectionRRsetsEmpty() {
        List<RRset> rrsets = emptyMessage.getSectionRRsets(Section.ANSWER);
        assertNotNull(rrsets);
        assertTrue(rrsets.isEmpty());
    }

    @Test
    public void testToWireWithoutRecords() {
        byte[] wire = emptyMessage.toWire();
        // The wire must contain at least the DNS header (12 bytes)
        assertNotNull(wire);
        assertTrue(wire.length >= Header.LENGTH);
        assertEquals(wire.length, emptyMessage.numBytes());
    }

    @Test
    public void testToWireWithMaxLengthEnough() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        byte[] wire = emptyMessage.toWire(Message.MAXLENGTH);
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH);
    }

    @Test
    public void testToWireWithTruncationFalseThrows() {
        // Build a message that is guaranteed to exceed a tiny limit.
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        // Each A record is ~16 bytes on the wire, so 20 bytes is too small
        try {
            emptyMessage.toWire(20, false);
            fail("Expected MessageSizeExceededException");
        } catch (MessageSizeExceededException e) {
            // expected
        }
    }

    @Test
    public void testToWireWithTruncationTrueDoesNotThrow() throws Exception {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        byte[] wire = emptyMessage.toWire(20, true);
        assertNotNull(wire);
        // The TC flag must be set in the header when truncation occurs.
        Header h = emptyMessage.getHeader();
        assertTrue(Header.getFlag(h.getFlagsByte(), Flags.TC));
    }

    @Test
    public void testSetTSIGAndGeneratedTSIG() throws Exception {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        emptyMessage.setTSIG(tsigKey);
        // toWire(int) triggers TSIG generation
        byte[] wire = emptyMessage.toWire(Message.MAXLENGTH);
        assertNotNull(wire);
        TSIGRecord gen = emptyMessage.getGeneratedTSIG();
        assertNotNull(gen);
        // After generation the message should be considered signed.
        assertTrue(emptyMessage.isSigned());
        // Verification flag is false because we never verified the response.
        assertFalse(emptyMessage.isVerified());
    }

    @Test
    public void testNumBytesAfterWireConversion() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        emptyMessage.toWire();
        assertTrue(emptyMessage.numBytes() > 0);
    }

    @Test
    public void testSectionToString() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        String s = emptyMessage.sectionToString(Section.ANSWER);
        assertNotNull(s);
        assertTrue(s.contains("example.com."));
    }

    @Test
    public void testToStringContainsHeaderAndSection() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        String s = emptyMessage.toString();
        assertNotNull(s);
        assertTrue(s.contains("HEADER"));
        assertTrue(s.contains("example.com."));
        assertTrue(s.contains("Message size:"));
    }

    @Test
    public void testCloneCreatesDeepCopy() throws Exception {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        emptyMessage.setResolver(new DummyResolver());
        Message copy = emptyMessage.clone();

        // Header must be equal but not the same instance
        assertEquals(emptyMessage.getHeader().getID(), copy.getHeader().getID());
        assertNotSame(emptyMessage.getHeader(), copy.getHeader());

        // Sections must contain equal records but be distinct lists
        assertEquals(emptyMessage.getSection(Section.ANSWER).size(),
                copy.getSection(Section.ANSWER).size());
        assertNotSame(emptyMessage.getSection(Section.ANSWER),
                copy.getSection(Section.ANSWER));

        // Resolver should be copied
        assertTrue(copy.getResolver().isPresent());
        assertTrue(copy.getResolver().get() instanceof DummyResolver);
    }

    @Test
    public void testResolverAccessors() {
        DummyResolver resolver = new DummyResolver();
        emptyMessage.setResolver(resolver);
        Optional<Resolver> opt = emptyMessage.getResolver();
        assertTrue(opt.isPresent());
        assertSame(resolver, opt.get());
    }

    @Test
    public void testIsTypeAllowedInSection() {
        // AUTHORITY section allows NS, SOA, DS, NSEC, NSEC3
        assertTrue(emptyMessage.isTypeAllowedInSection(Type.NS, Section.AUTHORITY));
        // AUTHORITY does NOT allow A
        assertFalse(emptyMessage.isTypeAllowedInSection(Type.A, Section.AUTHORITY));

        // ADDITIONAL allows A and AAAA
        assertTrue(emptyMessage.isTypeAllowedInSection(Type.A, Section.ADDITIONAL));
        // ADDITIONAL does NOT allow MX by default (depends on system property)
        boolean defaultAdditional = Boolean.parseBoolean(
                System.getProperty("dnsjava.harden_unknown_additional", "true"));
        assertEquals(!defaultAdditional,
                emptyMessage.isTypeAllowedInSection(Type.MX, Section.ADDITIONAL));
    }

    @Test
    public void testNormalizeSimpleAnswer() throws Exception {
        // Build a minimal successful answer (NOERROR) with a single A record.
        Message query = Message.newQuery(Record.newRecord(nameExample, Type.A, DClass.IN, 0));
        Message answer = new Message();
        answer.addRecord(query.getQuestion(), Section.QUESTION);
        answer.addRecord(aRecord, Section.ANSWER);
        // Normalization should return a new Message equal to the original answer.
        Message normalized = answer.normalize(query);
        assertNotNull(normalized);
        assertEquals(answer.getHeader().getID(), normalized.getHeader().getID());
        assertEquals(1, normalized.getSection(Section.ANSWER).size());
        assertEquals(aRecord, normalized.getSection(Section.ANSWER).get(0));
    }

    @Test
    public void testNormalizeWithIrrelevantRecordThrowsWhenFlagged() throws Exception {
        // Create a response that contains an extra TXT record that is irrelevant
        // to the original query (type A). The method should throw when the flag
        // throwOnIrrelevantRecord is true.
        Message query = Message.newQuery(Record.newRecord(nameExample, Type.A, DClass.IN, 0));
        Message answer = new Message();
        answer.addRecord(query.getQuestion(), Section.QUESTION);
        answer.addRecord(aRecord, Section.ANSWER);
        answer.addRecord(txtRecord, Section.ANSWER); // irrelevant

        try {
            answer.normalize(query, true);
            fail("Expected WireParseException because of irrelevant record");
        } catch (WireParseException e) {
            // Expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  Helper classes                                                      */
    /* ---------------------------------------------------------------------- */

    /**
     * Minimal {@link Resolver} implementation used only to verify that the
     * {@code setResolver}/{@code getResolver} pair stores and retrieves the same
     * instance. All other methods throw {@link UnsupportedOperationException}
     * because they are not needed for the tests.
     */
    private static class DummyResolver implements Resolver {
        @Override public void setPort(int port) { throw new UnsupportedOperationException(); }
        @Override public void setTCP(boolean flag) { throw new UnsupportedOperationException(); }
        @Override public void setIgnoreTruncation(boolean flag) { throw new UnsupportedOperationException(); }
        @Override public void setEDNS(int version) { throw new UnsupportedOperationException(); }
        @Override public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) { throw new UnsupportedOperationException(); }
        @Override public void setEDNS(int version, int payloadSize, int flags, EDNSOption... options) { throw new UnsupportedOperationException(); }
        @Override public void setTSIGKey(TSIG key) { throw new UnsupportedOperationException(); }
        @Override public void setTimeout(Duration timeout) { throw new UnsupportedOperationException(); }
        @Override public Duration getTimeout() { throw new UnsupportedOperationException(); }
        @Override public Message send(Message query) throws IOException { throw new UnsupportedOperationException(); }
        @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { throw new UnsupportedOperationException(); }
        @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { throw new UnsupportedOperationException(); }
        @Override public Object sendAsync(Message query, ResolverListener listener) { throw new UnsupportedOperationException(); }
    }
}
```
###Test END##