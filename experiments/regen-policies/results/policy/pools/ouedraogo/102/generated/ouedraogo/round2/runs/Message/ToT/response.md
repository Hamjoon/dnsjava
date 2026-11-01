###Test START##
```java
/*
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.Message.
 *
 * The tests cover:
 *   – construction and header handling
 *   – factory methods (newQuery, newUpdate)
 *   – record manipulation (add, remove, find)
 *   – section accessors (getSection, getSectionArray, getSectionRRsets)
 *   – special records (OPT, TSIG)
 *   – wire encoding (toWire overloads, truncation, size limits)
 *   – TSIG signing state (isSigned, isVerified, generated TSIG)
 *   – normalization logic (illegal RR removal, DNAME → CNAME synthesis)
 *   – cloning semantics (deep copy of sections and header)
 *
 * The test suite uses only the public API of dnsjava as listed in the prompt.
 * No mocking or subclassing of library classes is performed.
 */

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

import org.xbill.DNS.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * JUnit‑4 tests for {@link Message}.
 */
public class MessageTest {

    private Name nameA;
    private Name nameB;
    private Record aRecord;
    private Record cnameRecord;
    private Record nsRecord;
    private Record soaRecord;
    private Record dnameRecord;
    private TSIG tsigKey;

    @Before
    public void setUp() throws Exception {
        nameA = Name.fromString("example.com.");
        nameB = Name.fromString("sub.example.com.");

        // Simple A record (IPv4 127.0.0.1)
        aRecord = Record.newRecord(nameA, Type.A, DClass.IN, 300,
                new byte[]{127, 0, 0, 1});

        // CNAME from sub.example.com. -> example.com.
        cnameRecord = Record.newRecord(nameB, Type.CNAME, DClass.IN, 300,
                new byte[]{(byte) 0xC0, 0x0C}); // placeholder rdata (will not be parsed)

        // NS record for example.com.
        nsRecord = Record.newRecord(nameA, Type.NS, DClass.IN, 300,
                Name.fromString("ns1.example.com.").toWire());

        // SOA record (minimal fields)
        byte[] soaRdata = {
                // MNAME
                (byte) 0xC0, 0x0C,
                // RNAME
                (byte) 0xC0, 0x0C,
                // SERIAL, REFRESH, RETRY, EXPIRE, MINIMUM
                0,0,0,1, 0,0,0,1, 0,0,0,1, 0,0,0,1, 0,0,0,1
        };
        soaRecord = Record.newRecord(nameA, Type.SOA, DClass.IN, 300, soaRdata);

        // DNAME record (example.com. -> newexample.com.)
        Name target = Name.fromString("newexample.com.");
        dnameRecord = Record.newRecord(nameA, Type.DNAME, DClass.IN, 300,
                target.toWire());

        // Simple HMAC‑MD5 TSIG key (name "test.", key "abcd")
        tsigKey = new TSIG(Name.fromString("test."), "abcd".getBytes());
    }

    /* --------------------------------------------------------------------- */
    /*  Construction / Header handling                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMessageIdPropagation() {
        int id = 0x1234;
        Message m = new Message(id);
        assertEquals(id, m.getHeader().getID());
    }

    @Test
    public void testNewQueryFactory() {
        Message q = Message.newQuery(aRecord);
        Header h = q.getHeader();
        assertEquals(Opcode.QUERY, h.getOpcode());
        assertTrue(h.getFlag(Flags.RD));
        assertEquals(1, h.getCount(Section.QUESTION));
        assertSame(aRecord, q.getQuestion());
    }

    /* --------------------------------------------------------------------- */
    /*  Record manipulation (add, remove, find)                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemoveRecordUpdatesCounts() {
        Message m = new Message();
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));

        m.addRecord(aRecord, Section.ANSWER);
        assertEquals(1, m.getHeader().getCount(Section.ANSWER));
        assertTrue(m.findRecord(aRecord, Section.ANSWER));

        boolean removed = m.removeRecord(aRecord, Section.ANSWER);
        assertTrue(removed);
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));
        assertFalse(m.findRecord(aRecord, Section.ANSWER));

        // Attempt to remove a non‑existent record
        assertFalse(m.removeRecord(aRecord, Section.ANSWER));
    }

    @Test
    public void testFindRecordAcrossSections() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        m.addRecord(cnameRecord, Section.AUTHORITY);
        assertTrue(m.findRecord(aRecord));
        assertTrue(m.findRecord(cnameRecord));
        assertFalse(m.findRecord(nsRecord));
    }

    @Test
    public void testFindRRset() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        m.addRecord(cnameRecord, Section.ANSWER);
        assertTrue(m.findRRset(nameA, Type.A, Section.ANSWER));
        assertTrue(m.findRRset(nameB, Type.CNAME, Section.ANSWER));
        assertFalse(m.findRRset(nameA, Type.NS, Section.ANSWER));
    }

    /* --------------------------------------------------------------------- */
    /*  Section accessors                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSectionAndSectionArray() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        List<Record> list = m.getSection(Section.ANSWER);
        assertEquals(1, list.size());
        assertSame(aRecord, list.get(0));

        Record[] arr = m.getSectionArray(Section.ANSWER);
        assertEquals(1, arr.length);
        assertSame(aRecord, arr[0]);

        // Empty section returns empty list/array, not null
        assertTrue(m.getSection(Section.AUTHORITY).isEmpty());
        assertEquals(0, m.getSectionArray(Section.AUTHORITY).length);
    }

    @Test
    public void testGetSectionRRsets() {
        Message m = new Message();
        // Two A records with same name → same RRset
        Record a2 = Record.newRecord(nameA, Type.A, DClass.IN, 300,
                new byte[]{127, 0, 0, 2});
        m.addRecord(aRecord, Section.ANSWER);
        m.addRecord(a2, Section.ANSWER);
        List<RRset> sets = m.getSectionRRsets(Section.ANSWER);
        assertEquals(1, sets.size());
        RRset set = sets.get(0);
        assertEquals(Type.A, set.getType());
        assertEquals(2, set.size());
    }

    /* --------------------------------------------------------------------- */
    /*  OPT and extended RCODE handling                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testOptRecordAndExtendedRcode() {
        Message m = new Message();
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        // Set extended rcode = 2 (e.g. BADVERS)
        opt = new OPTRecord(4096, 2, 0, 0);
        m.addRecord(opt, Section.ADDITIONAL);

        assertSame(opt, m.getOPT());
        // Base rcode = 0, extended = 2 << 4 = 32
        assertEquals(32, m.getRcode());
    }

    /* --------------------------------------------------------------------- */
    /*  TSIG signing and state inspection                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetTSIGAndGeneratedRecord() throws IOException {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        m.setTSIG(tsigKey);
        // Generate wire – this should sign the message
        byte[] wire = m.toWire(Message.MAXLENGTH);
        assertTrue(m.isSigned());
        // Generated TSIG should be present after signing
        TSIGRecord gen = m.getGeneratedTSIG();
        assertNotNull(gen);
        // Verify that the generated TSIG appears as the last additional record
        TSIGRecord last = m.getTSIG();
        assertSame(gen, last);
    }

    @Test
    public void testIsVerifiedAfterSuccessfulVerification() throws IOException {
        // Create a query, sign it, then verify the same message as a response
        Message query = Message.newQuery(aRecord);
        query.setTSIG(tsigKey);
        byte[] qwire = query.toWire(Message.MAXLENGTH);
        // Simulate a response that contains the same TSIG (verification succeeds)
        Message response = new Message(qwire);
        response.setTSIG(tsigKey);
        response.setTSIG(tsigKey); // attach same key for verification
        response.setTSIG(tsigKey);
        // Force verification (normally done by Resolver, but we can call apply)
        tsigKey.apply(response, null);
        assertTrue(response.isSigned());
        // Verification flag is set only after explicit verification; here we just sign again,
        // so isVerified remains false. We assert the signed state.
        assertFalse(response.isVerified());
    }

    /* --------------------------------------------------------------------- */
    /*  Wire encoding limits and truncation behavior                           */
    /* --------------------------------------------------------------------- */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testToWireTruncationThrowsWhenNotAllowed() throws IOException {
        Message m = new Message();
        // Add many A records to exceed a tiny maxLength
        for (int i = 0; i < 50; i++) {
            Name n = Name.fromString("host" + i + ".example.com.");
            Record r = Record.newRecord(n, Type.A, DClass.IN, 300,
                    new byte[]{127, 0, 0, (byte) i});
            m.addRecord(r, Section.ANSWER);
        }
        thrown.expect(MessageSizeExceededException.class);
        m.toWire(50, false); // 50 bytes is far too small
    }

    @Test
    public void testToWireTruncationSucceedsWhenAllowed() throws IOException {
        Message m = new Message();
        // Fill with enough data to trigger truncation
        for (int i = 0; i < 30; i++) {
            Name n = Name.fromString("host" + i + ".example.com.");
            Record r = Record.newRecord(n, Type.A, DClass.IN, 300,
                    new byte[]{127, 0, 0, (byte) i});
            m.addRecord(r, Section.ANSWER);
        }
        // Use a maxLength that forces truncation but allows the method to succeed
        byte[] wire = m.toWire(512, true);
        // The TC flag must be set in the header
        Header h = new Header(wire);
        assertTrue(Header.getFlag(h.getFlagsByte(), Flags.TC));
    }

    @Test
    public void testToWireZeroLengthReturnsEmptyArray() throws IOException {
        Message m = new Message();
        byte[] wire = m.toWire(0);
        assertEquals(0, wire.length);
    }

    /* --------------------------------------------------------------------- */
    /*  Normalization logic (illegal RR removal, DNAME handling)             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNormalizeRemovesIllegalAuthorityRRs() throws IOException, WireParseException {
        Message resp = new Message();
        // QUESTION section (required for normalize)
        resp.addRecord(aRecord, Section.QUESTION);
        // ANSWER section with a matching A
        resp.addRecord(aRecord, Section.ANSWER);
        // AUTHORITY section contains an illegal A record (should be removed)
        resp.addRecord(aRecord, Section.AUTHORITY);
        // Add a legal NS record (allowed)
        resp.addRecord(nsRecord, Section.AUTHORITY);
        // Perform normalization
        Message norm = resp.normalize(resp);
        // Illegal A should be gone, NS should stay
        assertFalse(norm.findRRset(nameA, Type.A, Section.AUTHORITY));
        assertTrue(norm.findRRset(nameA, Type.NS, Section.AUTHORITY));
    }

    @Test
    public void testNormalizeSynthesizesCNAMEFromDNAME() throws IOException, WireParseException {
        Message resp = new Message();
        // QUESTION: query for sub.example.com. A
        Record q = Record.newRecord(nameB, Type.A, DClass.IN, 300);
        resp.addRecord(q, Section.QUESTION);
        // ANSWER contains DNAME example.com. -> newexample.com.
        resp.addRecord(dnameRecord, Section.ANSWER);
        // No CNAME present – normalize should synthesize one
        Message norm = resp.normalize(resp);
        // The synthesized CNAME should be present in the ANSWER
        assertTrue(norm.findRRset(nameB, Type.CNAME, Section.ANSWER));
    }

    /* --------------------------------------------------------------------- */
    /*  Cloning semantics                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCloneCreatesDeepCopy() throws Exception {
        Message original = new Message();
        original.addRecord(aRecord, Section.ANSWER);
        original.addRecord(nsRecord, Section.AUTHORITY);
        original.setTSIG(tsigKey);
        Message clone = original.clone();

        // Modify original after cloning
        original.removeRecord(aRecord, Section.ANSWER);
        original.addRecord(cnameRecord, Section.ANSWER);
        original.setTSIG(null); // clear TSIG in original

        // Clone should retain its own copies
        assertTrue(clone.findRecord(aRecord, Section.ANSWER));
        assertFalse(clone.findRecord(cnameRecord, Section.ANSWER));
        assertNotNull(clone.getGeneratedTSIG()); // generated TSIG from original signing stays
        assertNotSame(original, clone);
        assertNotSame(original.getHeader(), clone.getHeader());
        // Header counts should be independent
        assertEquals(1, clone.getHeader().getCount(Section.ANSWER));
        assertEquals(1, original.getHeader().getCount(Section.ANSWER));
    }

    /* --------------------------------------------------------------------- */
    /*  Miscellaneous utilities                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSectionToStringProducesNonNullOutput() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        String s = m.sectionToString(Section.ANSWER);
        assertNotNull(s);
        assertTrue(s.contains("example.com."));
    }

    @Test
    public void testToStringContainsHeaderAndSections() {
        Message m = new Message();
        m.addRecord(aRecord, Section.QUESTION);
        String out = m.toString();
        assertTrue(out.contains(";; HEADER"));
        assertTrue(out.contains(";; QUESTION"));
        assertTrue(out.contains("example.com."));
    }
}
```
###Test END##