package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link Message}.
 *
 * <p>The tests use only the public API that is declared in the supplied source snippets.
 * No mocking frameworks are used – real DNS objects are created via the library’s factory
 * methods.</p>
 */
public class MessageTest {

    private Name name;
    private Record aRecord;
    private Record cnameRecord;
    private Record mxRecord;
    private OPTRecord optRecord;
    private Header header;

    @Before
    public void setUp() throws Exception {
        name = Name.fromString("www.example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN, 300);
        cnameRecord = Record.newRecord(name, Type.CNAME, DClass.IN, 300);
        mxRecord = Record.newRecord(name, Type.MX, DClass.IN, 300);
        optRecord = new OPTRecord(4096, 5, 0, 0); // extended rcode = 5
        header = new Header(0x1234);
    }

    /* ---------- Constructor & basic factory methods ---------- */

    @Test
    public void testDefaultConstructor() {
        Message m = new Message();
        assertNotNull(m.getHeader());
    }

    @Test
    public void testIdConstructor() {
        Message m = new Message(0xABCD);
        assertEquals(0xABCD, m.getHeader().getID());
    }

    @Test
    public void testByteArrayConstructor() throws IOException {
        Message original = new Message();
        original.addRecord(aRecord, Section.ANSWER);
        byte[] wire = original.toWire();
        Message fromWire = new Message(wire);
        assertEquals(1, fromWire.getHeader().getCount(Section.ANSWER));
        assertEquals(aRecord, fromWire.getSectionArray(Section.ANSWER)[0]);
    }

    @Test
    public void testNewQueryFactory() {
        Message query = Message.newQuery(aRecord);
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode());
        assertTrue(query.getHeader().getFlag(Flags.RD));
        assertEquals(aRecord, query.getQuestion());
    }

    /* ---------- Record manipulation ---------- */

    @Test
    public void testAddAndRemoveRecord() {
        Message m = new Message();
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));
        m.addRecord(aRecord, Section.ANSWER);
        assertEquals(1, m.getHeader().getCount(Section.ANSWER));
        assertTrue(m.findRecord(aRecord, Section.ANSWER));

        boolean removed = m.removeRecord(aRecord, Section.ANSWER);
        assertTrue(removed);
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));
        assertFalse(m.findRecord(aRecord, Section.ANSWER));
    }

    @Test
    public void testRemoveAllRecords() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        m.addRecord(cnameRecord, Section.ANSWER);
        m.removeAllRecords(Section.ANSWER);
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));
        assertTrue(m.getSection(Section.ANSWER).isEmpty());
    }

    @Test
    public void testFindRecordInAnySection() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        assertTrue(m.findRecord(aRecord));
        assertFalse(m.findRecord(mxRecord));
    }

    @Test
    public void testFindRRset() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        assertTrue(m.findRRset(name, Type.A, Section.ANSWER));
        assertFalse(m.findRRset(name, Type.MX, Section.ANSWER));
    }

    @Test
    public void testFindRRsetAnySection() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        m.addRecord(mxRecord, Section.AUTHORITY);
        assertTrue(m.findRRset(name, Type.A));
        assertTrue(m.findRRset(name, Type.MX));
    }

    /* ---------- Section accessors ---------- */

    @Test
    public void testGetQuestion() {
        Message m = new Message();
        m.addRecord(cnameRecord, Section.QUESTION);
        assertEquals(cnameRecord, m.getQuestion());
    }

    @Test
    public void testGetOPT() {
        Message m = new Message();
        m.addRecord(optRecord, Section.ADDITIONAL);
        assertEquals(optRecord, m.getOPT());
    }

    @Test
    public void testGetRcodeWithExtended() {
        Message m = new Message();
        m.addRecord(optRecord, Section.ADDITIONAL);
        // Header rcode is 0, extended rcode from OPT is 5 → 5 << 4 = 80
        assertEquals(80, m.getRcode());
    }

    @Test
    public void testGetSectionArrayAndEmpty() {
        Message m = new Message();
        Record[] empty = m.getSectionArray(Section.AUTHORITY);
        assertNotNull(empty);
        assertEquals(0, empty.length);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSectionUnmodifiable() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        List<Record> list = m.getSection(Section.ANSWER);
        list.add(mxRecord); // should throw
    }

    @Test
    public void testGetSectionRRsetsGrouping() {
        Message m = new Message();
        // two A records with same name → one RRset of size 2
        Record a1 = Record.newRecord(name, Type.A, DClass.IN, 300);
        Record a2 = Record.newRecord(name, Type.A, DClass.IN, 300);
        m.addRecord(a1, Section.ANSWER);
        m.addRecord(a2, Section.ANSWER);
        List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size());
        assertEquals(2, rrsets.get(0).size());
    }

    /* ---------- Wire format ---------- */

    @Test
    public void testToWireEmptyMessage() {
        Message m = new Message();
        byte[] wire = m.toWire();
        // Minimum size is the DNS header length
        assertTrue(wire.length >= Header.LENGTH);
    }

    @Test
    public void testToWireWithTooSmallMaxLengthReturnsEmpty() {
        Message m = new Message();
        byte[] result = m.toWire(10); // less than Header.LENGTH
        assertEquals(0, result.length);
    }

    @Test
    public void testToWireTruncationException() {
        Message m = new Message();
        // Add a large number of records to guarantee overflow
        for (int i = 0; i < 200; i++) {
            m.addRecord(Record.newRecord(Name.fromString("host" + i + ".example.com."), Type.A, DClass.IN, 300), Section.ANSWER);
        }
        try {
            m.toWire(20, false);
            fail("Expected MessageSizeExceededException");
        } catch (MessageSizeExceededException e) {
            // expected
        } catch (IOException e) {
            fail("Unexpected IOException: " + e);
        }
    }

    @Test
    public void testToWireTruncateTrueDoesNotThrow() {
        Message m = new Message();
        for (int i = 0; i < 200; i++) {
            m.addRecord(Record.newRecord(Name.fromString("host" + i + ".example.com."), Type.A, DClass.IN, 300), Section.ANSWER);
        }
        try {
            byte[] data = m.toWire(20, true);
            assertNotNull(data);
        } catch (MessageSizeExceededException | IOException e) {
            fail("Did not expect exception: " + e);
        }
    }

    /* ---------- TSIG related checks (state based) ---------- */

    @Test
    public void testIsSignedAndIsVerifiedDefault() {
        Message m = new Message();
        assertFalse(m.isSigned());
        assertFalse(m.isVerified());
    }

    /* ---------- Resolver handling ---------- */

    @Test
    public void testResolverSetAndGet() {
        Message m = new Message();
        Resolver dummyResolver = new Resolver() {
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
            @Override public Duration getTimeout() { return null; }
            @Override public Message send(Message query) { return null; }
            @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
            @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
            @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
        };
        m.setResolver(dummyResolver);
        Optional<Resolver> opt = m.getResolver();
        assertTrue(opt.isPresent());
        assertSame(dummyResolver, opt.get());
    }

    /* ---------- Section type allowance ---------- */

    @Test
    public void testIsTypeAllowedInSection() {
        Message m = new Message();
        // AUTHORITY: SOA allowed
        assertTrue(m.isTypeAllowedInSection(Type.SOA, Section.AUTHORITY));
        // ADDITIONAL: A allowed
        assertTrue(m.isTypeAllowedInSection(Type.A, Section.ADDITIONAL));
        // ADDITIONAL: MX not allowed by default (harden_unknown_additional = true)
        assertFalse(m.isTypeAllowedInSection(Type.MX, Section.ADDITIONAL));
    }

    /* ---------- Normalization (simple pass‑through) ---------- */

    @Test
    public void testNormalizeSimpleMessage() throws Exception {
        // Query message
        Message query = new Message();
        query.addRecord(Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 300), Section.QUESTION);

        // Response message with a matching answer
        Message response = new Message();
        response.addRecord(Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 300), Section.ANSWER);
        response.addRecord(query.getQuestion(), Section.QUESTION);

        Message normalized = response.normalize(query);
        assertNotNull(normalized);
        // Normalization should not change counts for this simple case
        assertEquals(response.getHeader().getCount(Section.ANSWER), normalized.getHeader().getCount(Section.ANSWER));
    }

    /* ---------- Clone functionality ---------- */

    @Test
    public void testCloneCreatesDeepCopy() throws Exception {
        Message original = new Message();
        original.addRecord(aRecord, Section.ANSWER);
        original.addRecord(optRecord, Section.ADDITIONAL);
        Message copy = original.clone();

        // Verify header is cloned (different instance)
        assertNotSame(original.getHeader(), copy.getHeader());

        // Verify sections are deep‑copied
        assertEquals(1, copy.getHeader().getCount(Section.ANSWER));
        assertEquals(1, copy.getHeader().getCount(Section.ADDITIONAL));

        // Mutate original and ensure copy is unchanged
        original.removeAllRecords(Section.ANSWER);
        assertEquals(1, copy.getHeader().getCount(Section.ANSWER));
    }
}
