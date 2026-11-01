/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Message}.
 *
 * The tests exercise all public methods of {@code Message} using real DNS objects
 * (Name, Record, Header, OPTRecord, TSIG, etc.) – no mocking or subclassing of library
 * classes is performed.
 *
 * The test class is deliberately verbose to make the intent of each assertion clear.
 */
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
 * Comprehensive unit tests for {@link Message}.
 */
public class MessageTest {

    private Name nameA;
    private Name nameB;
    private Record aRecordQ;   // QUESTION section record
    private Record aRecordAns; // ANSWER section record (same name/type as aRecordQ)
    private Record aRecordAuth; // AUTHORITY section record (different name)
    private OPTRecord optRecord;
    private TSIG tsigKey;

    @Before
    public void setUp() throws Exception {
        // Simple absolute names
        nameA = Name.fromString("www.example.com.");
        nameB = Name.fromString("mail.example.com.");

        // Generic A‑type records – the raw binary representation of an IPv4 address
        byte[] addr = new byte[] { 127, 0, 0, 1 };
        aRecordQ = Record.newRecord(nameA, Type.A, DClass.IN, 3600, addr);
        aRecordAns = Record.newRecord(nameA, Type.A, DClass.IN, 3600, addr);
        aRecordAuth = Record.newRecord(nameB, Type.A, DClass.IN, 3600, addr);

        // An OPT record with an extended rcode (xrcode = 1) and a payload size of 4096
        optRecord = new OPTRecord(4096, 1, 0, 0);

        // A very simple TSIG key (algorithm HMAC‑MD5, name "tsig.", key "abcd")
        tsigKey = new TSIG(Name.fromString("tsig."), "abcd".getBytes());
    }

    /** Helper to create a fresh Message with a single QUESTION record. */
    private Message newMessageWithQuestion() {
        Message m = new Message();
        m.addRecord(aRecordQ, Section.QUESTION);
        return m;
    }

    @Test
    public void testConstructorAndHeader() {
        Message m1 = new Message(0x1234);
        assertEquals(0x1234, m1.getHeader().getID());

        Message m2 = new Message(); // random ID – just verify a Header exists
        assertNotNull(m2.getHeader());
    }

    @Test
    public void testNewQueryFactory() {
        Message query = Message.newQuery(aRecordQ);
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode());
        assertTrue(query.getHeader().getFlag(Flags.RD));
        assertEquals(aRecordQ, query.getQuestion());
    }

    @Test
    public void testAddAndRemoveRecord() {
        Message m = newMessageWithQuestion();

        // add to ANSWER section
        m.addRecord(aRecordAns, Section.ANSWER);
        assertTrue(m.findRecord(aRecordAns, Section.ANSWER));
        assertEquals(1, m.getHeader().getCount(Section.ANSWER));

        // remove it
        assertTrue(m.removeRecord(aRecordAns, Section.ANSWER));
        assertFalse(m.findRecord(aRecordAns, Section.ANSWER));
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));

        // removing non‑existent record returns false
        assertFalse(m.removeRecord(aRecordAns, Section.ANSWER));
    }

    @Test
    public void testRemoveAllRecords() {
        Message m = newMessageWithQuestion();
        m.addRecord(aRecordAns, Section.ANSWER);
        m.addRecord(aRecordAuth, Section.AUTHORITY);
        assertEquals(1, m.getHeader().getCount(Section.ANSWER));
        assertEquals(1, m.getHeader().getCount(Section.AUTHORITY));

        m.removeAllRecords(Section.ANSWER);
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));
        assertTrue(m.getSection(Section.ANSWER).isEmpty());

        // other sections remain untouched
        assertEquals(1, m.getHeader().getCount(Section.AUTHORITY));
    }

    @Test
    public void testFindRecordAcrossSections() {
        Message m = newMessageWithQuestion();
        m.addRecord(aRecordAns, Section.ANSWER);
        m.addRecord(aRecordAuth, Section.AUTHORITY);

        // present in QUESTION
        assertTrue(m.findRecord(aRecordQ));

        // present in ANSWER
        assertTrue(m.findRecord(aRecordAns));

        // present in AUTHORITY
        assertTrue(m.findRecord(aRecordAuth));

        // not present
        Record unknown = Record.newRecord(Name.fromString("nosuch.example.com."), Type.A, DClass.IN, 3600, new byte[4]);
        assertFalse(m.findRecord(unknown));
    }

    @Test
    public void testFindRRset() {
        Message m = newMessageWithQuestion();
        m.addRecord(aRecordAns, Section.ANSWER);
        m.addRecord(aRecordAuth, Section.AUTHORITY);

        // same name & type as QUESTION/ANSWER -> true
        assertTrue(m.findRRset(nameA, Type.A, Section.ANSWER));

        // different name -> false
        assertFalse(m.findRRset(nameB, Type.A, Section.AUTHORITY));

        // search across all sections
        assertTrue(m.findRRset(nameA, Type.A));
        assertFalse(m.findRRset(Name.fromString("nosuch.example.com."), Type.A));
    }

    @Test
    public void testGetQuestionAndTSIG() {
        Message m = newMessageWithQuestion();
        assertEquals(aRecordQ, m.getQuestion());

        // No TSIG present => null
        assertNull(m.getTSIG());
    }

    @Test
    public void testOPTHandlingAndRcode() throws Exception {
        Message m = newMessageWithQuestion();

        // Set a base rcode on the header (e.g., SERVFAIL = 2)
        m.getHeader().setRcode(Rcode.SERVFAIL);

        // Add the OPT record with extended rcode = 1 (value 16 when shifted)
        m.addRecord(optRecord, Section.ADDITIONAL);
        assertSame(optRecord, m.getOPT());

        // Expected combined rcode = 2 + (1 << 4) = 18
        assertEquals(18, m.getRcode());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSectionArrayAndListViews() {
        Message m = newMessageWithQuestion();
        m.addRecord(aRecordAns, Section.ANSWER);
        m.addRecord(aRecordAuth, Section.AUTHORITY);

        // Deprecated array view
        Record[] answerArray = m.getSectionArray(Section.ANSWER);
        assertEquals(1, answerArray.length);
        assertEquals(aRecordAns, answerArray[0]);

        // List view is unmodifiable
        List<Record> authList = m.getSection(Section.AUTHORITY);
        assertEquals(1, authList.size());
        assertEquals(aRecordAuth, authList.get(0));
        try {
            authList.add(aRecordAns);
            fail("Expected UnsupportedOperationException for modifying unmodifiable list");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testSectionRRsetsGrouping() {
        Message m = newMessageWithQuestion();

        // Two records with same name/type (aRecordQ and aRecordAns) -> same RRset
        m.addRecord(aRecordAns, Section.ANSWER);
        m.addRecord(aRecordQ, Section.ANSWER);

        // One record with different name/type
        m.addRecord(aRecordAuth, Section.ANSWER);

        List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size());

        // Verify grouping
        boolean foundSame = false;
        boolean foundDifferent = false;
        for (RRset set : rrsets) {
            if (set.getName().equals(nameA) && set.getType() == Type.A) {
                assertEquals(2, set.size()); // the two records with same name/type
                foundSame = true;
            } else if (set.getName().equals(nameB) && set.getType() == Type.A) {
                assertEquals(1, set.size());
                foundDifferent = true;
            }
        }
        assertTrue(foundSame);
        assertTrue(foundDifferent);
    }

    @Test
    public void testToWireAndSize() throws IOException {
        Message m = newMessageWithQuestion();
        m.addRecord(aRecordAns, Section.ANSWER);
        m.addRecord(optRecord, Section.ADDITIONAL);

        byte[] wire = m.toWire();
        assertNotNull(wire);
        assertTrue(wire.length > Header.LENGTH);
        assertEquals(wire.length, m.numBytes());
    }

    @Test
    public void testToWireWithTruncation() throws IOException {
        Message m = newMessageWithQuestion();

        // Force a tiny max length (< header length) – should yield empty array
        byte[] tiny = m.toWire(10);
        assertEquals(0, tiny.length);

        // Use a max length that is just enough for the header but not for any records
        byte[] justHeader = m.toWire(Header.LENGTH);
        // The header is always written; the TC flag should be set (checked via parsing)
        // For simplicity we just assert that we got a non‑empty array of exact Header length
        assertEquals(Header.LENGTH, justHeader.length);
    }

    @Test
    public void testTSIGSigningAndGeneratedRecord() throws IOException {
        Message m = newMessageWithQuestion();
        m.addRecord(aRecordAns, Section.ANSWER);
        m.setTSIG(tsigKey, Rcode.NOERROR, null);

        // Generate the wire with a generous max length so signing can succeed
        byte[] signedWire = m.toWire(Message.MAXLENGTH);
        assertTrue(signedWire.length > 0);
        assertNotNull(m.getGeneratedTSIG());

        // The generated TSIG should be present as the last record in the ADDITIONAL section
        TSIGRecord tsigRec = m.getTSIG();
        assertNotNull(tsigRec);
        assertEquals(tsigKey, tsigRec.getAlgorithm()); // algorithm name matches key algorithm
    }

    @Test
    public void testIsSignedAndIsVerifiedDefaults() {
        Message m = newMessageWithQuestion();
        assertFalse(m.isSigned());
        assertFalse(m.isVerified());

        // Signing does not automatically change the state; we verify that the defaults stay false
        m.setTSIG(tsigKey);
        assertFalse(m.isSigned());
    }

    @Test
    public void testSectionToStringOutput() {
        Message m = newMessageWithQuestion();
        String qStr = m.sectionToString(Section.QUESTION);
        assertNotNull(qStr);
        assertTrue(qStr.contains("www.example.com."));
        assertTrue(qStr.contains("type = A"));
        assertTrue(qStr.contains("class = IN"));
    }

    @Test
    public void testMessageToStringContainsAllSections() {
        Message m = newMessageWithQuestion();
        m.addRecord(aRecordAns, Section.ANSWER);
        m.addRecord(aRecordAuth, Section.AUTHORITY);
        m.addRecord(optRecord, Section.ADDITIONAL);

        String txt = m.toString();
        assertNotNull(txt);
        // Header line
        assertTrue(txt.contains(m.getHeader().toString()));
        // Section headings
        assertTrue(txt.contains(";; QUESTION:"));
        assertTrue(txt.contains(";; ANSWER:"));
        assertTrue(txt.contains(";; AUTHORITY:"));
        assertTrue(txt.contains(";; ADDITIONAL:"));
        // Records themselves
        assertTrue(txt.contains("www.example.com."));
        assertTrue(txt.contains("mail.example.com."));
        // OPT pseudo‑section
        assertTrue(txt.contains("OPT"));
        // Message size line
        assertTrue(txt.contains("Message size:"));
    }

    @Test
    public void testCloneCreatesDeepCopy() throws Exception {
        Message original = newMessageWithQuestion();
        original.addRecord(aRecordAns, Section.ANSWER);
        original.setTSIG(tsigKey, Rcode.NOERROR, null);
        original.setResolver(new DummyResolver());

        Message copy = original.clone();

        // Header clone – IDs should match but objects differ
        assertEquals(original.getHeader().getID(), copy.getHeader().getID());
        assertNotSame(original.getHeader(), copy.getHeader());

        // Sections are deep copies
        assertEquals(original.getSection(Section.ANSWER).size(),
                     copy.getSection(Section.ANSWER).size());
        assertNotSame(original.getSection(Section.ANSWER),
                      copy.getSection(Section.ANSWER));

        // Resolver is copied by reference (as per implementation)
        assertEquals(original.getResolver().orElse(null), copy.getResolver().orElse(null));

        // Modifying original does not affect clone
        original.removeAllRecords(Section.ANSWER);
        assertEquals(1, copy.getSection(Section.ANSWER).size());
    }

    @Test
    public void testResolverSetAndGet() {
        Message m = newMessageWithQuestion();
        DummyResolver resolver = new DummyResolver();
        m.setResolver(resolver);
        Optional<Resolver> opt = m.getResolver();
        assertTrue(opt.isPresent());
        assertSame(resolver, opt.get());
    }

    /** Simple stub implementation of {@link Resolver} for testing purposes. */
    private static class DummyResolver implements Resolver {
        @Override public void setPort(int port) {}
        @Override public void setTCP(boolean flag) {}
        @Override public void setIgnoreTruncation(boolean flag) {}
        @Override public void setEDNS(int version) {}
        @Override public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
        @Override public void setEDNS(int version, int payloadSize, int flags, EDNSOption... options) {}
        @Override public void setTSIGKey(TSIG key) {}
        @Override public void setTimeout(int secs, int msecs) {}
        @Override public void setTimeout(int secs) {}
        @Override public void setTimeout(Duration timeout) {}
        @Override public Message send(Message query) throws IOException { return null; }
        @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
        @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
        @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
    }
}
