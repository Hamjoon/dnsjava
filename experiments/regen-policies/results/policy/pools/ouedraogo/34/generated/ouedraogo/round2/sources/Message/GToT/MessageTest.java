/*
 * JUnit‑4 test suite for org.xbill.DNS.Message
 *
 * The tests cover:
 *   – extraction of all public methods,
 *   – basic functional verification,
 *   – edge‑case handling (null sections, empty messages, truncation, TSIG handling),
 *   – exception scenarios (malformed wire data, illegal sections),
 *   – cloning and resolver association,
 *   – normalization behaviour.
 *
 * NOTE: The test suite uses only the public API listed in the prompt and the
 *       dnsjava classes that are part of the same library version.
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive test cases for {@link Message}.
 */
public class MessageTest {

    private static final Name EXAMPLE_COM;
    private static final Name ZONE_EXAMPLE_COM;
    private static final Record A_RECORD;
    private static final Record CNAME_RECORD;
    private static final Record TXT_RECORD;
    private static final Record DNAME_RECORD;
    private static final OPTRecord OPT_RECORD;
    private static final TSIG TSIG_KEY;

    static {
        try {
            EXAMPLE_COM = Name.fromString("example.com.");
            ZONE_EXAMPLE_COM = Name.fromString("example.com.");
            // A record (type A, IPv4 127.0.0.1)
            A_RECORD = Record.newRecord(EXAMPLE_COM, Type.A, DClass.IN, 3600,
                    new byte[]{127, 0, 0, 1});
            // CNAME record pointing to www.example.com.
            CNAME_RECORD = Record.newRecord(EXAMPLE_COM, Type.CNAME, DClass.IN, 3600,
                    Name.fromString("www.example.com."));
            // TXT record with simple text
            TXT_RECORD = Record.newRecord(EXAMPLE_COM, Type.TXT, DClass.IN, 3600,
                    "test".getBytes("UTF-8"));
            // DNAME record (example.com. -> dname.example.org.)
            DNAME_RECORD = Record.newRecord(EXAMPLE_COM, Type.DNAME, DClass.IN, 3600,
                    Name.fromString("dname.example.org."));
            // Minimal OPT record (payload 4096, no options)
            OPT_RECORD = new OPTRecord(4096, 0, 0, 0);
            // Simple TSIG key (HMAC-MD5, name "testkey.", secret "abcd")
            TSIG_KEY = TSIG.fromString("hmac-md5.sig-alg.reg.int.:testkey.:abcd");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Message emptyMessage;
    private Message queryMessage;

    @Before
    public void setUp() {
        emptyMessage = new Message();                     // random ID, empty sections
        queryMessage = Message.newQuery(A_RECORD);         // a typical query message
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Constructors & static factories                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMessageIdConstructor() {
        Message m = new Message(0x1234);
        assertEquals(0x1234, m.getHeader().getID());
    }

    @Test
    public void testMessageDefaultConstructor() {
        Message m = new Message();
        // Header is always non‑null and contains a valid ID
        assertNotNull(m.getHeader());
    }

    @Test
    public void testMessageFromByteArray() throws IOException {
        byte[] wire = queryMessage.toWire();
        Message m = new Message(wire);
        assertEquals(queryMessage.getHeader().getID(), m.getHeader().getID());
        assertEquals(1, m.getHeader().getCount(Section.QUESTION));
        assertNotNull(m.getQuestion());
        assertEquals(A_RECORD, m.getQuestion());
    }

    @Test
    public void testMessageFromByteBuffer() throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(queryMessage.toWire());
        Message m = new Message(buf);
        assertEquals(queryMessage.getHeader().getID(), m.getHeader().getID());
    }

    @Test(expected = IOException.class)
    public void testMessageFromMalformedByteArray() throws IOException {
        // Random garbage – should cause an IOException or WireParseException
        byte[] bad = new byte[]{0, 1, 2, 3, 4, 5};
        new Message(bad);
    }

    @Test
    public void testNewQueryFactory() {
        Message m = Message.newQuery(TXT_RECORD);
        assertEquals(Opcode.QUERY, m.getHeader().getOpcode());
        assertTrue(m.getHeader().getFlag(Flags.RD));
        assertEquals(TXT_RECORD, m.getQuestion());
    }

    @Test
    public void testNewUpdateFactory() {
        Message m = Message.newUpdate(ZONE_EXAMPLE_COM);
        assertEquals(Opcode.UPDATE, m.getHeader().getOpcode());
        // The Update subclass adds its own behaviour – we only verify the zone is stored
        assertEquals(ZONE_EXAMPLE_COM, m.getQuestion().getName());
    }

    /* ---------------------------------------------------------------------- */
    /*  2. Header handling                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetAndGetHeader() {
        Header h = new Header(0xABCD);
        emptyMessage.setHeader(h);
        assertSame(h, emptyMessage.getHeader());
    }

    /* ---------------------------------------------------------------------- */
    /*  3. Record manipulation (add / remove / find)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddRecordCreatesSection() {
        assertFalse(emptyMessage.findRecord(A_RECORD, Section.ANSWER));
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        assertTrue(emptyMessage.findRecord(A_RECORD, Section.ANSWER));
        assertEquals(1, emptyMessage.getHeader().getCount(Section.ANSWER));
    }

    @Test
    public void testRemoveRecordSuccess() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        assertTrue(emptyMessage.removeRecord(A_RECORD, Section.ANSWER));
        assertFalse(emptyMessage.findRecord(A_RECORD, Section.ANSWER));
        assertEquals(0, emptyMessage.getHeader().getCount(Section.ANSWER));
    }

    @Test
    public void testRemoveRecordFailure() {
        // Record not present – should return false and not change counts
        assertFalse(emptyMessage.removeRecord(A_RECORD, Section.ANSWER));
        assertEquals(0, emptyMessage.getHeader().getCount(Section.ANSWER));
    }

    @Test
    public void testRemoveAllRecords() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        emptyMessage.addRecord(CNAME_RECORD, Section.ANSWER);
        emptyMessage.removeAllRecords(Section.ANSWER);
        assertEquals(0, emptyMessage.getHeader().getCount(Section.ANSWER));
        assertTrue(emptyMessage.getSection(Section.ANSWER).isEmpty());
    }

    @Test
    public void testFindRecordAcrossSections() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        emptyMessage.addRecord(CNAME_RECORD, Section.AUTHORITY);
        assertTrue(emptyMessage.findRecord(A_RECORD));
        assertTrue(emptyMessage.findRecord(CNAME_RECORD));
    }

    @Test
    public void testFindRRset() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        assertTrue(emptyMessage.findRRset(EXAMPLE_COM, Type.A, Section.ANSWER));
        assertFalse(emptyMessage.findRRset(EXAMPLE_COM, Type.CNAME, Section.ANSWER));
    }

    @Test
    public void testFindRRsetAcrossSections() {
        emptyMessage.addRecord(CNAME_RECORD, Section.AUTHORITY);
        assertTrue(emptyMessage.findRRset(EXAMPLE_COM, Type.CNAME));
    }

    @Test
    public void testGetQuestionWhenMissing() {
        Message m = new Message();
        assertNull(m.getQuestion());
    }

    @Test
    public void testGetTSIGWhenAbsent() {
        assertNull(emptyMessage.getTSIG());
    }

    /* ---------------------------------------------------------------------- */
    /*  4. TSIG handling                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetTSIGAndIsSigned() {
        emptyMessage.setTSIG(TSIG_KEY);
        // No wire conversion yet – isSigned reflects that a TSIG is attached
        assertTrue(emptyMessage.isSigned());
        // Verification state is not set until after toWire; default is unsigned
        assertFalse(emptyMessage.isVerified());
    }

    @Test
    public void testToWireWithTSIG() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        emptyMessage.setTSIG(TSIG_KEY);
        byte[] wire = emptyMessage.toWire();
        // The generated wire must contain at least one additional record (the TSIG)
        Message parsed = new Message(wire);
        assertNotNull(parsed.getTSIG());
    }

    @Test
    public void testToWireWithZeroMaxLength() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        byte[] wire = emptyMessage.toWire(0);
        assertEquals(0, wire.length);
    }

    @Test
    public void testToWireTruncation() {
        // Build a message that is larger than a tiny maxLength to force truncation
        for (int i = 0; i < 20; i++) {
            emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        }
        byte[] wire = emptyMessage.toWire(50); // 50 bytes is smaller than a full message
        // The TC flag must be set
        Message parsed = new Message(wire);
        assertTrue(parsed.getHeader().getFlag(Flags.TC));
    }

    /* ---------------------------------------------------------------------- */
    /*  5. OPT handling and extended RCODE                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetOPT() {
        emptyMessage.addRecord(OPT_RECORD, Section.ADDITIONAL);
        assertSame(OPT_RECORD, emptyMessage.getOPT());
    }

    @Test
    public void testGetRcodeWithExtendedRcode() {
        // Normal NOERROR = 0
        assertEquals(0, emptyMessage.getRcode());

        // Add an OPT with extended rcode = 5 (=> 5 << 4 = 80)
        OPTRecord opt = new OPTRecord(4096, 5, 0, 0);
        emptyMessage.addRecord(opt, Section.ADDITIONAL);
        assertEquals(80, emptyMessage.getRcode());
    }

    /* ---------------------------------------------------------------------- */
    /*  6. Section access (array, list, RRset)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSectionArrayDeprecated() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        Record[] arr = emptyMessage.getSectionArray(Section.ANSWER);
        assertEquals(1, arr.length);
        assertEquals(A_RECORD, arr[0]);
    }

    @Test
    public void testGetSectionList() {
        emptyMessage.addRecord(CNAME_RECORD, Section.AUTHORITY);
        List<Record> list = emptyMessage.getSection(Section.AUTHORITY);
        assertEquals(1, list.size());
        assertEquals(CNAME_RECORD, list.get(0));
    }

    @Test
    public void testGetSectionRRsets() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        emptyMessage.addRecord(A_RECORD, Section.ANSWER); // duplicate -> same RRset
        List<RRset> rrsets = emptyMessage.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size());
        RRset set = rrsets.get(0);
        assertEquals(Type.A, set.getType());
        assertEquals(2, set.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  7. toString / sectionToString                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSectionToString() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        String s = emptyMessage.sectionToString(Section.ANSWER);
        assertTrue(s.contains("example.com."));
        assertTrue(s.contains("A"));
    }

    @Test
    public void testMessageToString() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        emptyMessage.addRecord(OPT_RECORD, Section.ADDITIONAL);
        String out = emptyMessage.toString();
        // Header line, section titles and size line must be present
        assertTrue(out.contains(";; ANSWER:"));
        assertTrue(out.contains(";; ADDITIONAL:"));
        assertTrue(out.contains("Message size:"));
    }

    /* ---------------------------------------------------------------------- */
    /*  8. numBytes tracking                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNumBytesAfterWire() {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        byte[] wire = emptyMessage.toWire();
        assertEquals(wire.length, emptyMessage.numBytes());
    }

    /* ---------------------------------------------------------------------- */
    /*  9. Cloning                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloneCreatesDeepCopy() throws Exception {
        emptyMessage.addRecord(A_RECORD, Section.ANSWER);
        emptyMessage.setTSIG(TSIG_KEY);
        Message copy = emptyMessage.clone();

        // IDs equal but objects distinct
        assertEquals(emptyMessage.getHeader().getID(), copy.getHeader().getID());
        assertNotSame(emptyMessage.getHeader(), copy.getHeader());

        // Sections are deep‑copied
        assertNotSame(emptyMessage.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));
        assertEquals(1, copy.getSection(Section.ANSWER).size());

        // TSIG state is preserved
        assertTrue(copy.isSigned());
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Resolver association                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetAndGetResolver() {
        Resolver resolver = new Resolver() {
            public void setPort(int p) {}
            public void setTCP(boolean f) {}
            public void setIgnoreTruncation(boolean f) {}
            public void setEDNS(int v) {}
            public void setEDNS(int v, int ps, int f, List<EDNSOption> o) {}
            public void setTSIGKey(TSIG k) {}
            public void setTimeout(Duration d) {}
            public Message send(Message q) { return null; }
            public java.util.concurrent.CompletionStage<Message> sendAsync(Message q) { return null; }
            public java.util.concurrent.CompletionStage<Message> sendAsync(Message q, java.util.concurrent.Executor e) { return null; }
            public Object sendAsync(Message q, ResolverListener l) { return null; }
        };
        emptyMessage.setResolver(resolver);
        Optional<Resolver> opt = emptyMessage.getResolver();
        assertTrue(opt.isPresent());
        assertSame(resolver, opt.get());
    }

    /* ---------------------------------------------------------------------- */
    /* 11. Normalization (basic success path)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNormalizeSimpleAnswer() throws Exception {
        // Build a minimal answer: question + A record matching the query name
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        response.addRecord(A_RECORD, Section.ANSWER);
        Message normalized = response.normalize(queryMessage);
        // Normalized message should retain the answer RRset
        assertTrue(normalized.findRRset(EXAMPLE_COM, Type.A, Section.ANSWER));
    }

    @Test
    public void testNormalizeIgnoresIrrelevantRRsetWhenNotThrowing() throws Exception {
        // Answer contains an extra CNAME for a different name – should be ignored
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        response.addRecord(A_RECORD, Section.ANSWER);
        Record otherCname = Record.newRecord(Name.fromString("other.example.com."), Type.CNAME,
                DClass.IN, 3600, Name.fromString("else.example.com."));
        response.addRecord(otherCname, Section.ANSWER);
        Message normalized = response.normalize(queryMessage, false);
        // The unrelated CNAME must not be present
        assertFalse(normalized.findRecord(otherCname, Section.ANSWER));
        assertTrue(normalized.findRecord(A_RECORD, Section.ANSWER));
    }

    @Test(expected = WireParseException.class)
    public void testNormalizeThrowsOnIrrelevantWhenRequested() throws Exception {
        // Same as previous test but with throwOnIrrelevantRecord = true
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        response.addRecord(A_RECORD, Section.ANSWER);
        Record otherCname = Record.newRecord(Name.fromString("other.example.com."), Type.CNAME,
                DClass.IN, 3600, Name.fromString("else.example.com."));
        response.addRecord(otherCname, Section.ANSWER);
        response.normalize(queryMessage, true); // should raise
    }

    /* ---------------------------------------------------------------------- */
    /* 12. Edge cases for Section validation                                   */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testFindRRsetInvalidSection() {
        emptyMessage.findRRset(EXAMPLE_COM, Type.A, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordInvalidSection() {
        emptyMessage.addRecord(A_RECORD, 5); // Section values are 0‑3
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSectionToStringInvalid() {
        emptyMessage.sectionToString(99);
    }

    /* ---------------------------------------------------------------------- */
    /* 13. MAXLENGTH constant sanity check                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMaxLengthConstant() {
        assertTrue(Message.MAXLENGTH > 0);
        assertEquals(65535, Message.MAXLENGTH);
    }
}
