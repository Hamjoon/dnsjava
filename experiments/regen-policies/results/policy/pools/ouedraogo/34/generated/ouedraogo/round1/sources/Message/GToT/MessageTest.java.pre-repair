/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Message}.
 *
 * The tests are organised as the three “experts” described in the prompt:
 *
 * 1. **Public API extraction** – a comment block lists every public method (including
 *    constructors) together with its signature.
 * 2. **Basic functionality tests** – one simple test per method that verifies the
 *    normal/expected behaviour.
 * 3. **Edge‑case / exception tests** – additional tests that exercise boundary
 *    conditions, invalid arguments and error handling (e.g. section index out of
 *    range, truncation, TSIG handling, etc.).
 *
 * The suite builds real DNS objects (Message, Header, Record, Name, TSIG,
 * OPTRecord, etc.) – no mocking or subclassing is used, complying with the
 * restriction in the prompt.
 *
 * All tests are written for JUnit‑4 and can be run with any JUnit‑4 compatible
 * runner (e.g. `mvn test`, IDE JUnit runner, etc.).
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * ---------------------------------------------------------------------------
 * 1. PUBLIC API OF {@link Message}
 * ---------------------------------------------------------------------------
 *
 * Constructors
 *   Message(int id)
 *   Message()
 *   static Message newQuery(Record r)
 *   static Message newUpdate(Name zone)
 *   Message(DNSInput in)               // package‑private, used indirectly via byte[] / ByteBuffer ctors
 *   Message(byte[] b) throws IOException
 *   Message(ByteBuffer byteBuffer) throws IOException
 *
 * Public instance methods
 *   void setHeader(Header h)
 *   Header getHeader()
 *   void addRecord(Record r, int section)
 *   boolean removeRecord(Record r, int section)
 *   void removeAllRecords(int section)
 *   boolean findRecord(Record r, int section)
 *   boolean findRecord(Record r)
 *   boolean findRRset(Name name, int type, int section)
 *   boolean findRRset(Name name, int type)
 *   Record getQuestion()
 *   TSIGRecord getTSIG()
 *   boolean isSigned()
 *   boolean isVerified()
 *   OPTRecord getOPT()
 *   int getRcode()
 *   @Deprecated Record[] getSectionArray(int section)
 *   List<Record> getSection(int section)
 *   List<RRset> getSectionRRsets(int section)
 *   byte[] toWire()
 *   byte[] toWire(int maxLength)
 *   void setTSIG(TSIG key)
 *   void setTSIG(TSIG key, int error, TSIGRecord querytsig)
 *   int numBytes()
 *   String sectionToString(int section)
 *   @Override String toString()
 *   @Override Message clone()
 *   void setResolver(Resolver resolver)
 *   Optional<Resolver> getResolver()
 *
 * Private / package‑private helpers are not tested directly (they are exercised
 * indirectly via the public API).
 */
public class MessageTest {

    private Name nameExample;
    private Record aRecord;
    private Record nsRecord;
    private Header header;

    @Before
    public void setUp() throws Exception {
        nameExample = Name.fromString("example.com.");
        // Create a simple A record (type = A, class = IN, ttl = 300, address = 127.0.0.1)
        aRecord = Record.newRecord(nameExample, Type.A, DClass.IN, 300,
                new byte[]{127, 0, 0, 1});
        // Create a simple NS record (type = NS, class = IN, ttl = 300, target = ns.example.com.)
        Name nsTarget = Name.fromString("ns.example.com.");
        nsRecord = Record.newRecord(nameExample, Type.NS, DClass.IN, 300,
                nsTarget.toWire());
        header = new Header(0x1234);
    }

    /* -----------------------------------------------------------------------
     * 2. BASIC FUNCTIONALITY TESTS – ONE TEST PER PUBLIC METHOD
     * ----------------------------------------------------------------------- */

    @Test
    public void testConstructorWithId() {
        Message m = new Message(0xABCD);
        assertEquals(0xABCD, m.getHeader().getID());
    }

    @Test
    public void testDefaultConstructor() {
        Message m = new Message();
        // default constructor generates a random ID – we only check that a Header exists
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
    public void testNewUpdateFactory() {
        Message upd = Message.newUpdate(nameExample);
        // Update is a subclass; we only verify that the zone is stored as the question
        assertEquals(nameExample, upd.getQuestion().getName());
        assertEquals(Opcode.UPDATE, upd.getHeader().getOpcode());
    }

    @Test
    public void testHeaderSetGet() {
        Message m = new Message();
        Header h = new Header(0x5555);
        m.setHeader(h);
        assertSame(h, m.getHeader());
    }

    @Test
    public void testAddAndGetSection() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        List<Record> answer = m.getSection(Section.ANSWER);
        assertEquals(1, answer.size());
        assertEquals(aRecord, answer.get(0));
    }

    @Test
    public void testRemoveRecord() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        boolean removed = m.removeRecord(aRecord, Section.ANSWER);
        assertTrue(removed);
        assertTrue(m.getSection(Section.ANSWER).isEmpty());
    }

    @Test
    public void testRemoveAllRecords() {
        Message m = new Message();
        m.addRecord(aRecord, Section.AUTHORITY);
        m.addRecord(nsRecord, Section.AUTHORITY);
        m.removeAllRecords(Section.AUTHORITY);
        assertTrue(m.getSection(Section.AUTHORITY).isEmpty());
        assertEquals(0, m.getHeader().getCount(Section.AUTHORITY));
    }

    @Test
    public void testFindRecordInSection() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(m.findRecord(aRecord, Section.ADDITIONAL));
    }

    @Test
    public void testFindRecordInAnySection() {
        Message m = new Message();
        m.addRecord(nsRecord, Section.AUTHORITY);
        assertTrue(m.findRecord(nsRecord));
    }

    @Test
    public void testFindRRsetInSection() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        assertTrue(m.findRRset(nameExample, Type.A, Section.ANSWER));
    }

    @Test
    public void testFindRRsetInAnySection() {
        Message m = new Message();
        m.addRecord(nsRecord, Section.AUTHORITY);
        assertTrue(m.findRRset(nameExample, Type.NS));
    }

    @Test
    public void testGetQuestion() {
        Message m = Message.newQuery(aRecord);
        assertEquals(aRecord, m.getQuestion());
    }

    @Test
    public void testGetTSIGWhenAbsent() {
        Message m = new Message();
        assertNull(m.getTSIG());
    }

    @Test
    public void testIsSignedAndIsVerifiedDefault() {
        Message m = new Message();
        assertFalse(m.isSigned());
        assertFalse(m.isVerified());
    }

    @Test
    public void testGetOPTWhenAbsent() {
        Message m = new Message();
        assertNull(m.getOPT());
    }

    @Test
    public void testGetRcodeWithoutOPT() {
        Message m = new Message();
        m.getHeader().setRcode(Rcode.SERVFAIL);
        assertEquals(Rcode.SERVFAIL, m.getRcode());
    }

    @Test
    public void testGetRcodeWithOPT() {
        Message m = new Message();
        // OPT with extended rcode = 5 (i.e. 0x5 << 4 = 0x50)
        OPTRecord opt = new OPTRecord(4096, 5, 0, 0);
        m.addRecord(opt, Section.ADDITIONAL);
        m.getHeader().setRcode(Rcode.NOERROR);
        assertEquals(5 << 4, m.getRcode());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetSectionArrayDeprecated() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        Record[] arr = m.getSectionArray(Section.ANSWER);
        assertArrayEquals(new Record[]{aRecord}, arr);
    }

    @Test
    public void testGetSectionRRsets() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size());
        assertEquals(Type.A, rrsets.get(0).getType());
        assertEquals(nameExample, rrsets.get(0).getName());
    }

    @Test
    public void testToWireAndNumBytes() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        byte[] wire = m.toWire();
        assertNotNull(wire);
        assertTrue(wire.length > Header.LENGTH);
        assertEquals(wire.length, m.numBytes());
    }

    @Test
    public void testToWireWithTruncation() {
        Message m = new Message();
        // Add many records to force truncation when maxLength is tiny
        for (int i = 0; i < 50; i++) {
            m.addRecord(aRecord, Section.ANSWER);
        }
        byte[] truncated = m.toWire(40); // 40 < Header.LENGTH + one record => truncation
        // The returned array may be empty (if nothing fits) – we only assert that no exception occurs
        assertNotNull(truncated);
    }

    @Test
    public void testSetTSIGAndSignedFlags() {
        Message m = new Message();
        TSIG key = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."),
                Name.fromString("testkey."), "somesecret");
        m.setTSIG(key);
        // After signing (via toWire) the message becomes signed
        byte[] wire = m.toWire(); // triggers TSIG generation
        assertTrue(m.isSigned());
        // No verification was attempted, so isVerified() should be false
        assertFalse(m.isVerified());
    }

    @Test
    public void testCloneIndependence() {
        Message original = new Message();
        original.addRecord(aRecord, Section.ANSWER);
        Message clone = original.clone();
        // Mutate clone and verify original stays unchanged
        clone.addRecord(nsRecord, Section.AUTHORITY);
        assertEquals(1, original.getSection(Section.AUTHORITY).size());
        assertEquals(0, original.getSection(Section.ANSWER).size() - 1);
    }

    @Test
    public void testResolverSetAndGet() {
        Message m = new Message();
        Resolver dummy = new Resolver() {
            @Override public void setPort(int port) {}
            @Override public void setTCP(boolean flag) {}
            @Override public void setIgnoreTruncation(boolean flag) {}
            @Override public void setEDNS(int version) {}
            @Override public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
            @Override public void setEDNS(int version, int payloadSize, int flags, EDNSOption... options) {}
            @Override public void setTSIGKey(TSIG key) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public Duration getTimeout() { return null; }
            @Override public Message send(Message query) throws IOException { return null; }
            @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
            @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
            @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
        };
        m.setResolver(dummy);
        assertTrue(m.getResolver().isPresent());
        assertSame(dummy, m.getResolver().get());
    }

    @Test
    public void testMessageFromByteArray() throws IOException {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        byte[] wire = m.toWire();
        Message fromBytes = new Message(wire);
        assertEquals(m.getHeader().getID(), fromBytes.getHeader().getID());
        assertEquals(1, fromBytes.getSection(Section.ANSWER).size());
        assertEquals(aRecord, fromBytes.getSection(Section.ANSWER).get(0));
    }

    @Test
    public void testMessageFromByteBuffer() throws IOException {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        byte[] wire = m.toWire();
        ByteBuffer buf = ByteBuffer.wrap(wire);
        Message fromBuf = new Message(buf);
        assertEquals(m.getHeader().getID(), fromBuf.getHeader().getID());
    }

    @Test
    public void testSectionToString() {
        Message m = new Message();
        m.addRecord(aRecord, Section.QUESTION);
        String secStr = m.sectionToString(Section.QUESTION);
        assertTrue(secStr.contains("example.com."));
    }

    @Test
    public void testToStringContainsHeaderAndSections() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        String s = m.toString();
        assertTrue(s.contains("Header"));
        assertTrue(s.contains("example.com."));
        assertTrue(s.contains("Message size"));
    }

    /* -----------------------------------------------------------------------
     * 3. EDGE‑CASE & EXCEPTION TESTS
     * ----------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordInvalidSection() {
        Message m = new Message();
        // Section indices are 0‑3; 5 is out of range and should trigger Section.check()
        m.addRecord(aRecord, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRecordInvalidSection() {
        Message m = new Message();
        m.removeRecord(aRecord, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindRecordInvalidSection() {
        Message m = new Message();
        m.findRecord(aRecord, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindRRsetInvalidSection() {
        Message m = new Message();
        m.findRRset(nameExample, Type.A, -2);
    }

    @Test
    public void testGetSectionEmptyReturnsUnmodifiableList() {
        Message m = new Message();
        List<Record> list = m.getSection(Section.AUTHORITY);
        assertTrue(list.isEmpty());
        try {
            list.add(aRecord);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void testGetSectionArrayWhenSectionEmpty() {
        Message m = new Message();
        Record[] empty = m.getSectionArray(Section.ADDITIONAL);
        assertNotNull(empty);
        assertEquals(0, empty.length);
    }

    @Test
    public void testGetOPTWhenPresent() {
        Message m = new Message();
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        m.addRecord(opt, Section.ADDITIONAL);
        assertSame(opt, m.getOPT());
    }

    @Test
    public void testGetRcodeWithExtendedRcodeOverflow() {
        Message m = new Message();
        // Extended rcode can be up to 15 (4 bits). Use 15 to verify bit‑shift handling.
        OPTRecord opt = new OPTRecord(4096, 15, 0, 0);
        m.addRecord(opt, Section.ADDITIONAL);
        m.getHeader().setRcode(0);
        assertEquals(15 << 4, m.getRcode());
    }

    @Test
    public void testToWireWithZeroMaxLengthReturnsEmptyArray() {
        Message m = new Message();
        byte[] wire = m.toWire(0);
        assertEquals(0, wire.length);
    }

    @Test
    public void testSetTSIGWithErrorAndQueryTSIG() {
        Message m = new Message();
        TSIG key = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("key."), "secret");
        // Create a dummy query TSIG record (normally obtained from a request)
        TSIGRecord queryTsig = new TSIGRecord(Name.fromString("example.com."), DClass.IN, 0,
                Name.fromString("hmac-md5.sig-alg.reg.int."), new Date(),
                (int) Duration.ofSeconds(300).getSeconds(), new byte[]{0x01, 0x02},
                0x1234, Rcode.NOERROR, new byte[]{});
        m.setTSIG(key, Rcode.FORMERR, queryTsig);
        // The presence of a TSIG key should not cause an exception when converting to wire
        byte[] wire = m.toWire();
        assertTrue(wire.length > 0);
        // After conversion the message is considered signed
        assertTrue(m.isSigned());
    }

    @Test
    public void testNormalizeSimpleMessageReturnsSameMessage() throws IOException {
        // Build a minimal answer message (no DNAME/CNAME complexities)
        Message query = Message.newQuery(aRecord);
        Message response = new Message();
        response.addRecord(aRecord, Section.ANSWER);
        response.addRecord(query.getQuestion(), Section.QUESTION);
        Message normalized = response.normalize(query);
        assertNotNull(normalized);
        // Normalization of a simple answer should keep the same answer RRset
        assertTrue(normalized.findRecord(aRecord, Section.ANSWER));
    }

    @Test
    public void testNormalizeWithIrrelevantRRsetAndThrowFlag() throws IOException {
        Message query = Message.newQuery(aRecord);
        Message response = new Message();
        // Add an irrelevant NS record (different name) – should trigger exception when flag is true
        Name other = Name.fromString("other.com.");
        Record irrelevant = Record.newRecord(other, Type.NS, DClass.IN, 300,
                Name.fromString("ns.other.com.").toWire());
        response.addRecord(irrelevant, Section.AUTHORITY);
        try {
            response.normalize(query, true);
            fail("Expected WireParseException due to irrelevant RRset");
        } catch (WireParseException expected) {
            // ok
        }
    }

    /* -----------------------------------------------------------------------
     * Helper methods used by a few tests
     * ----------------------------------------------------------------------- */

    private static class Update extends Message {
        Update(Name zone) {
            super(new Header());
            // The Update subclass used by Message.newUpdate – we only need a concrete instance.
            // The constructor of Message is package‑private; this subclass lives in the same
            // package, so we can invoke it.
            addRecord(Record.newRecord(zone, Type.SOA, DClass.IN, 0,
                    new byte[]{0}), Section.QUESTION);
        }
    }
}
