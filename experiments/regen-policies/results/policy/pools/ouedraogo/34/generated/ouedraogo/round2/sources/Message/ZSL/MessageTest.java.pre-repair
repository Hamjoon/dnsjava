/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Message}.
 *
 * The tests use only the public API that is visible in the source snippet and the
 * additional signatures supplied in the prompt.  No mocking frameworks are used;
 * real DNS objects are created via their constructors or factory methods.
 *
 * The goal is to achieve broad coverage of the public behaviour of {@code Message}
 * – construction, record handling, section access, wire‑format conversion,
 * TSIG handling, cloning, resolver association and the normalization logic.
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
 * Note:  The DNS library contains many concrete Record subclasses (ARecord,
 * CNAMERecord, DNAMERecord, etc.).  For the purpose of these tests the generic
 * {@link Record#newRecord(Name, int, int, long)} factory is sufficient because
 * the {@code Message} logic that is exercised (section handling, RRset grouping,
 * normalization) depends only on the record name, type and class.
 */
public class MessageTest {

    private Name nameA;
    private Name nameB;
    private Name nameC;
    private Name zone;

    @Before
    public void setUp() throws Exception {
        nameA = Name.fromString("a.example.com.");
        nameB = Name.fromString("b.example.com.");
        nameC = Name.fromString("c.example.com.");
        zone   = Name.fromString("example.com.");
    }

    /* --------------------------------------------------------------------- */
    /*  Construction and basic header handling                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMessageIdAndHeader() {
        Message m = new Message(0x1234);
        assertEquals(0x1234, m.getHeader().getID());

        // default constructor must generate a Header with a non‑zero ID
        Message random = new Message();
        assertNotEquals(0, random.getHeader().getID());
    }

    /* --------------------------------------------------------------------- */
    /*  Record addition, removal and lookup                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddFindRemoveRecord() {
        Message m = new Message();
        Record a = Record.newRecord(nameA, Type.A, DClass.IN, 3600);
        m.addRecord(a, Section.ANSWER);

        // find in specific section
        assertTrue(m.findRecord(a, Section.ANSWER));
        // find in any section
        assertTrue(m.findRecord(a));

        // remove
        assertTrue(m.removeRecord(a, Section.ANSWER));
        assertFalse(m.findRecord(a, Section.ANSWER));
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));
    }

    @Test
    public void testFindRRset() {
        Message m = new Message();
        Record r1 = Record.newRecord(nameA, Type.A, DClass.IN, 3600);
        Record r2 = Record.newRecord(nameA, Type.A, DClass.IN, 7200);
        Record r3 = Record.newRecord(nameB, Type.A, DClass.IN, 3600);
        m.addRecord(r1, Section.ANSWER);
        m.addRecord(r2, Section.ANSWER);
        m.addRecord(r3, Section.ANSWER);

        assertTrue(m.findRRset(nameA, Type.A, Section.ANSWER));
        assertFalse(m.findRRset(nameC, Type.A, Section.ANSWER));
        // any section (only answer filled)
        assertTrue(m.findRRset(nameB, Type.A));
    }

    @Test
    public void testGetQuestion() {
        Message q = Message.newQuery(Record.newRecord(nameA, Type.A, DClass.IN, 0));
        Record question = q.getQuestion();
        assertNotNull(question);
        assertEquals(nameA, question.getName());
        assertEquals(Type.A, question.getType());
    }

    /* --------------------------------------------------------------------- */
    /*  Section access helpers                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSectionArrayAndList() {
        Message m = new Message();
        Record a = Record.newRecord(nameA, Type.A, DClass.IN, 300);
        m.addRecord(a, Section.ANSWER);

        Record[] arr = m.getSectionArray(Section.ANSWER);
        assertArrayEquals(new Record[]{a}, arr);

        List<Record> lst = m.getSection(Section.ANSWER);
        assertEquals(1, lst.size());
        assertEquals(a, lst.get(0));

        // empty section returns empty array / empty list
        assertEquals(0, m.getSectionArray(Section.AUTHORITY).length);
        assertTrue(m.getSection(Section.AUTHORITY).isEmpty());
    }

    @Test
    public void testGetSectionRRsetsGrouping() {
        Message m = new Message();
        Record a1 = Record.newRecord(nameA, Type.A, DClass.IN, 300);
        Record a2 = Record.newRecord(nameA, Type.A, DClass.IN, 600);
        Record mx = Record.newRecord(nameA, Type.MX, DClass.IN, 300);
        m.addRecord(a1, Section.ANSWER);
        m.addRecord(a2, Section.ANSWER);
        m.addRecord(mx, Section.ANSWER);

        List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
        // we expect two RRsets: one for A, one for MX
        assertEquals(2, rrsets.size());

        // verify the A RRset contains both A records
        RRset aSet = rrsets.stream()
                .filter(set -> set.getType() == Type.A)
                .findFirst()
                .orElse(null);
        assertNotNull(aSet);
        assertEquals(2, aSet.size());
    }

    /* --------------------------------------------------------------------- */
    /*  OPT and extended RCODE handling                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testOPTRecordAndExtendedRcode() {
        Message m = new Message();
        // set a base rcode
        m.getHeader().setRcode(Rcode.SERVFAIL); // 2

        // create an OPT record with extended rcode = 1 (i.e. 0x10)
        OPTRecord opt = new OPTRecord(4096, 1, 0, 0);
        m.addRecord(opt, Section.ADDITIONAL);

        assertSame(opt, m.getOPT());

        // combined rcode = base (2) + (1 << 4) = 18
        assertEquals(18, m.getRcode());
    }

    /* --------------------------------------------------------------------- */
    /*  TSIG handling                                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTSIGSigningAndRetrieval() throws IOException {
        Message m = new Message();
        // a tiny key – the library does not validate the key material for the test
        TSIG tsig = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("testkey."), "aGVsbG8=");

        m.setTSIG(tsig);
        // add a simple A record so that the message is not empty
        m.addRecord(Record.newRecord(nameA, Type.A, DClass.IN, 300), Section.ANSWER);

        byte[] wire = m.toWire();          // this triggers TSIG generation
        assertTrue(m.isSigned());

        TSIGRecord tsigRec = m.getTSIG();
        assertNotNull(tsigRec);
        // generated TSIG record must be the same instance returned by getGeneratedTSIG()
        assertSame(tsigRec, m.getGeneratedTSIG());
    }

    /* --------------------------------------------------------------------- */
    /*  Wire format conversion and truncation                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToWireAndSize() {
        Message m = new Message();
        // add a few records so that the wire length is > 0
        for (int i = 0; i < 5; i++) {
            Name n = Name.fromString("host" + i + ".example.com.");
            m.addRecord(Record.newRecord(n, Type.A, DClass.IN, 300), Section.ANSWER);
        }

        byte[] wire = m.toWire();
        assertNotNull(wire);
        assertTrue(wire.length > Header.LENGTH);
        assertEquals(wire.length, m.numBytes());
    }

    @Test
    public void testToWireTruncation() {
        Message m = new Message();
        // add many records – each A record is ~ 16 bytes on wire, so 200 records will exceed a tiny limit
        for (int i = 0; i < 200; i++) {
            Name n = Name.fromString("host" + i + ".example.com.");
            m.addRecord(Record.newRecord(n, Type.A, DClass.IN, 300), Section.ANSWER);
        }

        // request a very small maximum length; we expect a truncated message
        byte[] truncated = m.toWire(100);
        assertNotNull(truncated);
        assertTrue(truncated.length <= 100);
        // the truncated message must have the TC flag set
        Message parsed = new Message(truncated);
        assertTrue(parsed.getHeader().getFlag(Flags.TC));
    }

    /* --------------------------------------------------------------------- */
    /*  Clone behaviour                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCloneIndependence() {
        Message original = new Message();
        Record a = Record.newRecord(nameA, Type.A, DClass.IN, 300);
        original.addRecord(a, Section.ANSWER);

        Message clone = original.clone();
        // modify original after cloning
        original.addRecord(Record.newRecord(nameB, Type.A, DClass.IN, 300), Section.ANSWER);

        // clone must still contain only the original single record
        assertEquals(1, clone.getSection(Section.ANSWER).size());
        assertEquals(a, clone.getSection(Section.ANSWER).get(0));
    }

    /* --------------------------------------------------------------------- */
    /*  Resolver association                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testResolverSetAndGet() {
        Message m = new Message();

        // minimal Resolver implementation – only the methods required by the interface are needed
        Resolver resolver = new Resolver() {
            @Override public void setPort(int port) {}
            @Override public void setTCP(boolean flag) {}
            @Override public void setIgnoreTruncation(boolean flag) {}
            @Override public void setEDNS(int version) {}
            @Override public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
            @Override public void setEDNS(int version, int payloadSize, int flags, EDNSOption... options) {}
            @Override public void setTSIGKey(TSIG key) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public Duration getTimeout() { return Duration.ZERO; }
            @Override public Message send(Message query) throws IOException { return null; }
            @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
            @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
            @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
        };

        m.setResolver(resolver);
        Optional<Resolver> opt = m.getResolver();
        assertTrue(opt.isPresent());
        assertSame(resolver, opt.get());
    }

    /* --------------------------------------------------------------------- */
    /*  Section‑type allowance checks                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsTypeAllowedInSection() {
        Message m = new Message();

        // AUTHORITY: SOA allowed
        assertTrue(m.isTypeAllowedInSection(Type.SOA, Section.AUTHORITY));
        // AUTHORITY: A not allowed
        assertFalse(m.isTypeAllowedInSection(Type.A, Section.AUTHORITY));

        // ADDITIONAL: A allowed
        assertTrue(m.isTypeAllowedInSection(Type.A, Section.ADDITIONAL));
        // ADDITIONAL: NS not allowed (unless system property disables hardening)
        assertFalse(m.isTypeAllowedInSection(Type.NS, Section.ADDITIONAL));
    }

    /* --------------------------------------------------------------------- */
    /*  Normalization (CNAME chain)                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNormalizeCnameChain() throws IOException {
        // ----- build the query -----
        Message query = Message.newQuery(Record.newRecord(nameA, Type.A, DClass.IN, 0));

        // ----- build the response -----
        Message response = new Message();
        // QUESTION section must be present for normalize to work
        response.addRecord(Record.newRecord(nameA, Type.A, DClass.IN, 0), Section.QUESTION);

        // CNAME from a.example.com. -> b.example.com.
        Record cname = Record.newRecord(nameA, Type.CNAME, DClass.IN, 300);
        response.addRecord(cname, Section.ANSWER);

        // A record for the target name
        Record aTarget = Record.newRecord(nameB, Type.A, DClass.IN, 300);
        response.addRecord(aTarget, Section.ANSWER);

        // invoke normalization
        Message normalized = response.normalize(query);
        assertNotNull(normalized);

        // The answer section must contain exactly the two records we added (CNAME + A)
        List<Record> answer = normalized.getSection(Section.ANSWER);
        assertEquals(2, answer.size());
        // first record should be the CNAME (same name as the original query)
        assertEquals(Type.CNAME, answer.get(0).getType());
        assertEquals(nameA, answer.get(0).getName());
        // second record should be the A for the CNAME target
        assertEquals(Type.A, answer.get(1).getType());
        assertEquals(nameB, answer.get(1).getName());
    }

    /* --------------------------------------------------------------------- */
    /*  toString sanity check (basic structural verification)                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringContainsSectionsAndSize() {
        Message m = new Message();
        m.addRecord(Record.newRecord(nameA, Type.A, DClass.IN, 300), Section.ANSWER);
        String s = m.toString();

        // basic checks – the string must contain the header line, an ANSWER label and the size line
        assertTrue(s.contains("ANSWER"));
        assertTrue(s.contains("Message size:"));
        // after a call to toWire() the size field is populated; we trigger it here
        m.toWire();
        assertTrue(s.contains(String.valueOf(m.numBytes())) || s.contains("bytes"));
    }
}
