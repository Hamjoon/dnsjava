/**
 * Comprehensive JUnit‑4 test suite for {@code org.xbill.DNS.Message}.
 *
 * The test class follows the workflow described in the prompt:
 *   1. List all public methods (see the large comment block at the top).
 *   2. Provide a basic test for each method that checks the “happy‑path”.
 *   3. Identify edge‑case / exception scenarios and add dedicated tests.
 *   4. Merge everything into a single JUnit‑4 test file.
 *
 * The tests use only the public API that is part of the supplied source
 * (no external mocking frameworks).  Real DNS objects (Header, Name,
 * Record, OPTRecord, TSIG, etc.) are created via their public constructors
 * or factory methods.
 *
 * NOTE: The DNS library is fairly large; many methods are thin wrappers
 * around other classes.  For brevity the tests focus on observable
 * behaviour (state changes, return values, thrown exceptions) rather than
 * internal implementation details.
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
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

/**
 * Public methods of {@code Message} (including constructors) that are covered:
 *
 * <pre>
 * Message(int id)
 * Message()
 * static Message newQuery(Record r)
 * static Message newUpdate(Name zone)
 * Message(DNSInput in)                // package‑private – not instantiated directly
 * Message(byte[] b) throws IOException
 * Message(ByteBuffer byteBuffer) throws IOException
 * void setHeader(Header h)
 * Header getHeader()
 * void addRecord(Record r, int section)
 * boolean removeRecord(Record r, int section)
 * void removeAllRecords(int section)
 * boolean findRecord(Record r, int section)
 * boolean findRecord(Record r)
 * boolean findRRset(Name name, int type, int section)
 * boolean findRRset(Name name, int type)
 * Record getQuestion()
 * TSIGRecord getTSIG()
 * TSIGRecord getGeneratedTSIG()              // package‑private, accessed via reflection
 * boolean isSigned()
 * boolean isVerified()
 * OPTRecord getOPT()
 * int getRcode()
 * @Deprecated Record[] getSectionArray(int section)
 * List<Record> getSection(int section)
 * List<RRset> getSectionRRsets(int section)
 * byte[] toWire()
 * byte[] toWire(int maxLength)
 * byte[] toWire(int maxLength, boolean truncate) throws MessageSizeExceededException
 * void setTSIG(TSIG key)
 * void setTSIG(TSIG key, int error, TSIGRecord querytsig)
 * int numBytes()
 * String sectionToString(int section)
 * String toString()
 * Message clone()
 * void setResolver(Resolver resolver)
 * Optional<Resolver> getResolver()
 * Message normalize(Message query)
 * Message normalize(Message query, boolean throwOnIrrelevantRecord) throws WireParseException
 * </pre>
 */
public class MessageTest {

    private Name exampleName;
    private Record aRecord;
    private Record txtRecord;
    private Header header;

    @Before
    public void setUp() throws Exception {
        exampleName = Name.fromString("example.com.");
        aRecord = Record.newRecord(exampleName, Type.A, DClass.IN, 3600);
        txtRecord = Record.newRecord(exampleName, Type.TXT, DClass.IN, 3600, "test".getBytes());
        header = new Header(0x1234);
    }

    /* ----------------------------------------------------------------------
     *  Constructors and factory methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorWithId() {
        Message m = new Message(0xABCD);
        assertEquals(0xABCD, m.getHeader().getID());
    }

    @Test
    public void testDefaultConstructor() {
        Message m = new Message();
        // The default constructor generates a random ID – just verify that a header exists
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
        Message upd = Message.newUpdate(exampleName);
        // Update messages are instances of the subclass Update; we only verify that it is not null
        assertNotNull(upd);
        assertEquals(Opcode.UPDATE, upd.getHeader().getOpcode());
    }

    @Test
    public void testByteArrayConstructor() throws IOException {
        Message m = new Message(aRecord.toWire(Section.QUESTION));
        // The message should contain the record we just encoded
        assertEquals(aRecord, m.getQuestion());
    }

    @Test
    public void testByteBufferConstructor() throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(aRecord.toWire(Section.QUESTION));
        Message m = new Message(buf);
        assertEquals(aRecord, m.getQuestion());
    }

    /* ----------------------------------------------------------------------
     *  Header handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetAndGetHeader() {
        Message m = new Message();
        Header newHeader = new Header(0x9999);
        m.setHeader(newHeader);
        assertSame(newHeader, m.getHeader());
    }

    /* ----------------------------------------------------------------------
     *  Record manipulation (add / remove / find)
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddRecordAndFindRecord() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        assertTrue(m.findRecord(aRecord, Section.ANSWER));
        assertTrue(m.findRecord(aRecord)); // any‑section overload
    }

    @Test
    public void testRemoveRecordSuccess() {
        Message m = new Message();
        m.addRecord(aRecord, Section.AUTHORITY);
        assertTrue(m.removeRecord(aRecord, Section.AUTHORITY));
        assertFalse(m.findRecord(aRecord, Section.AUTHORITY));
    }

    @Test
    public void testRemoveRecordFailure() {
        Message m = new Message();
        // Removing a non‑existent record must return false
        assertFalse(m.removeRecord(aRecord, Section.ADDITIONAL));
    }

    @Test
    public void testRemoveAllRecords() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        m.addRecord(txtRecord, Section.ANSWER);
        m.removeAllRecords(Section.ANSWER);
        assertFalse(m.findRecord(aRecord, Section.ANSWER));
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));
    }

    @Test
    public void testFindRecordInvalidSection() {
        Message m = new Message();
        try {
            m.findRecord(aRecord, 99); // invalid section
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    /* ----------------------------------------------------------------------
     *  RRset lookup
     * ---------------------------------------------------------------------- */

    @Test
    public void testFindRRset() {
        Message m = new Message();
        m.addRecord(aRecord, Section.ANSWER);
        assertTrue(m.findRRset(exampleName, Type.A, Section.ANSWER));
        assertTrue(m.findRRset(exampleName, Type.A));
    }

    @Test
    public void testFindRRsetNotPresent() {
        Message m = new Message();
        assertFalse(m.findRRset(exampleName, Type.A, Section.ANSWER));
        assertFalse(m.findRRset(exampleName, Type.A));
    }

    /* ----------------------------------------------------------------------
     *  Section getters
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetQuestionWhenEmpty() {
        Message m = new Message();
        assertNull(m.getQuestion());
    }

    @Test
    public void testGetSectionAndArray() {
        Message m = new Message();
        m.addRecord(aRecord, Section.QUESTION);
        List<Record> list = m.getSection(Section.QUESTION);
        assertEquals(1, list.size());
        assertEquals(aRecord, list.get(0));

        // deprecated array view
        Record[] arr = m.getSectionArray(Section.QUESTION);
        assertEquals(1, arr.length);
        assertEquals(aRecord, arr[0]);
    }

    @Test
    public void testGetSectionEmptyReturnsImmutableList() {
        Message m = new Message();
        List<Record> empty = m.getSection(Section.ANSWER);
        assertTrue(empty.isEmpty());
        try {
            empty.add(aRecord);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // pass
        }
    }

    @Test
    public void testGetSectionRRsetsGrouping() {
        Message m = new Message();
        // Two A records with the same name/type form a single RRset
        Record a1 = Record.newRecord(exampleName, Type.A, DClass.IN, 3600, new byte[]{127, 0, 0, 1});
        Record a2 = Record.newRecord(exampleName, Type.A, DClass.IN, 3600, new byte[]{127, 0, 0, 2});
        m.addRecord(a1, Section.ANSWER);
        m.addRecord(a2, Section.ANSWER);
        List<RRset> rrsets = m.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size());
        RRset set = rrsets.get(0);
        assertEquals(Type.A, set.getType());
        assertEquals(2, set.size());
    }

    /* ----------------------------------------------------------------------
     *  OPT and TSIG handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetOPTWhenAbsent() {
        Message m = new Message();
        assertNull(m.getOPT());
    }

    @Test
    public void testGetOPTWhenPresent() {
        Message m = new Message();
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        m.addRecord(opt, Section.ADDITIONAL);
        assertSame(opt, m.getOPT());
    }

    @Test
    public void testGetTSIG() {
        Message m = new Message();
        // Build a minimal TSIGRecord
        TSIGRecord tsig = new TSIGRecord(
                Name.fromString("tsig.example.com."),
                DClass.IN,
                0,
                TSIG.HMAC_MD5,
                new Date(),
                300,
                new byte[]{0x01},
                0,
                Rcode.NOERROR,
                new byte[0]);
        m.addRecord(tsig, Section.ADDITIONAL);
        assertSame(tsig, m.getTSIG());
    }

    @Test
    public void testIsSignedAndIsVerifiedDefault() {
        Message m = new Message();
        assertFalse(m.isSigned());
        assertFalse(m.isVerified());
    }

    /* ----------------------------------------------------------------------
     *  Rcode and extended Rcode handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetRcodeWithoutOPT() {
        Message m = new Message();
        m.getHeader().setRcode(Rcode.SERVFAIL);
        assertEquals(Rcode.SERVFAIL, m.getRcode());
    }

    @Test
    public void testGetRcodeWithOPT() {
        Message m = new Message();
        m.getHeader().setRcode(Rcode.NOERROR);
        OPTRecord opt = new OPTRecord(0, 5, 0, 0); // extended rcode = 5
        m.addRecord(opt, Section.ADDITIONAL);
        // Expected: base rcode (0) + (5 << 4) = 80
        assertEquals(5 << 4, m.getRcode());
    }

    /* ----------------------------------------------------------------------
     *  Wire format conversion
     * ---------------------------------------------------------------------- */

    @Test
    public void testToWireDefault() {
        Message m = new Message();
        m.addRecord(aRecord, Section.QUESTION);
        byte[] wire = m.toWire();
        assertNotNull(wire);
        assertTrue(wire.length > Header.LENGTH);
        assertEquals(wire.length, m.numBytes());
    }

    @Test
    public void testToWireWithMaxLengthSufficient() {
        Message m = new Message();
        m.addRecord(aRecord, Section.QUESTION);
        byte[] wire = m.toWire(1500);
        assertTrue(wire.length > 0);
        assertEquals(wire.length, m.numBytes());
    }

    @Test
    public void testToWireWithMaxLengthTooSmallReturnsEmptyArray() {
        Message m = new Message();
        // Header.LENGTH is the minimum size a DNS message can have
        byte[] wire = m.toWire(Header.LENGTH - 1);
        assertEquals(0, wire.length);
        assertEquals(0, m.numBytes()); // size stays at 0 because nothing could be rendered
    }

    @Test
    public void testToWireTruncateFlag() {
        Message m = new Message();
        // Fill the message with a very large TXT record to force truncation
        byte[] bigData = new byte[2000];
        Record bigTxt = Record.newRecord(exampleName, Type.TXT, DClass.IN, 3600, bigData);
        m.addRecord(bigTxt, Section.ANSWER);
        try {
            m.toWire(500, false);
            fail("Expected MessageSizeExceededException");
        } catch (MessageSizeExceededException e) {
            // expected because truncation is disabled
        }

        // With truncation enabled we should get a (possibly empty) array but no exception
        try {
            byte[] truncated = m.toWire(500, true);
            assertNotNull(truncated);
        } catch (MessageSizeExceededException e) {
            fail("Did not expect exception when truncate=true");
        }
    }

    /* ----------------------------------------------------------------------
     *  TSIG configuration (does not affect isSigned until the message is signed)
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetTSIG() {
        Message m = new Message();
        TSIG key = new TSIG(TSIG.HMAC_MD5, Name.fromString("key.example.com."), "secret");
        m.setTSIG(key);
        // No direct public getter for the key; we verify that calling toWire does not throw
        byte[] wire = m.toWire();
        assertNotNull(wire);
    }

    @Test
    public void testSetTSIGWithErrorAndQueryTSIG() {
        Message m = new Message();
        TSIG key = new TSIG(TSIG.HMAC_MD5, Name.fromString("key.example.com."), "secret");
        TSIGRecord queryTsig = new TSIGRecord(
                Name.fromString("tsig.example.com."),
                DClass.IN,
                0,
                TSIG.HMAC_MD5,
                new Date(),
                300,
                new byte[]{0x02},
                0,
                Rcode.NOERROR,
                new byte[0]);
        m.setTSIG(key, Rcode.FORMERR, queryTsig);
        // Rendering should still succeed
        byte[] wire = m.toWire();
        assertNotNull(wire);
    }

    /* ----------------------------------------------------------------------
     *  Resolver association
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetAndGetResolver() {
        Message m = new Message();
        Resolver resolver = new SimpleResolver(); // SimpleResolver is part of dnsjava and has a public no‑arg ctor
        m.setResolver(resolver);
        Optional<Resolver> opt = m.getResolver();
        assertTrue(opt.isPresent());
        assertSame(resolver, opt.get());
    }

    @Test
    public void testGetResolverWhenNotSet() {
        Message m = new Message();
        assertFalse(m.getResolver().isPresent());
    }

    /* ----------------------------------------------------------------------
     *  Clone / copy semantics
     * ---------------------------------------------------------------------- */

    @Test
    public void testCloneIndependence() {
        Message original = new Message();
        original.addRecord(aRecord, Section.ANSWER);
        Message copy = original.clone();

        // The copy must contain the same record
        assertTrue(copy.findRecord(aRecord, Section.ANSWER));

        // Modifying the copy must not affect the original
        copy.removeAllRecords(Section.ANSWER);
        assertTrue(original.findRecord(aRecord, Section.ANSWER));
        assertFalse(copy.findRecord(aRecord, Section.ANSWER));
    }

    /* ----------------------------------------------------------------------
     *  normalize() – basic success path (no illegal records)
     * ---------------------------------------------------------------------- */

    @Test
    public void testNormalizeSimpleAnswer() throws Exception {
        Message query = Message.newQuery(aRecord);
        Message response = new Message();
        response.addRecord(aRecord, Section.ANSWER);
        response.addRecord(query.getQuestion(), Section.QUESTION);

        Message normalized = response.normalize(query);
        // The normalized message should be equal (by content) to the original response
        assertEquals(response.getHeader().getID(), normalized.getHeader().getID());
        assertTrue(normalized.findRecord(aRecord, Section.ANSWER));
    }

    /* ----------------------------------------------------------------------
     *  Edge‑case validation for Section.check – invalid section numbers
     * ---------------------------------------------------------------------- */

    @Test
    public void testSectionToStringInvalidSection() {
        Message m = new Message();
        try {
            m.sectionToString(-1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    /* ----------------------------------------------------------------------
     *  toString() sanity checks
     * ---------------------------------------------------------------------- */

    @Test
    public void testToStringContainsHeaderAndSize() {
        Message m = new Message();
        m.addRecord(aRecord, Section.QUESTION);
        String s = m.toString();
        assertTrue(s.contains("Header"));
        assertTrue(s.contains("Message size:"));
    }

    /* ----------------------------------------------------------------------
     *  Private helper – reflection access to package‑private getGeneratedTSIG()
     * ---------------------------------------------------------------------- */

    @Test
    public void testGeneratedTSIGAfterSigning() throws Exception {
        Message m = new Message();
        // Set a TSIG key and force a wire conversion which will generate the TSIGRecord
        TSIG key = new TSIG(TSIG.HMAC_MD5, Name.fromString("key.example.com."), "secret");
        m.setTSIG(key);
        m.addRecord(aRecord, Section.QUESTION);
        m.toWire(); // triggers TSIG generation internally

        // Use reflection to invoke the package‑private getter
        java.lang.reflect.Method getter = Message.class.getDeclaredMethod("getGeneratedTSIG");
        getter.setAccessible(true);
        TSIGRecord generated = (TSIGRecord) getter.invoke(m);
        assertNotNull(generated);
        assertEquals(key, generatedTsigKey(key, generated));
    }

    // Helper that extracts the TSIG key used to generate a TSIGRecord via reflection
    private TSIG generatedTsigKey(TSIG expectedKey, TSIGRecord record) throws Exception {
        // The TSIG key is not stored in the TSIGRecord, but we can verify that the
        // algorithm name matches the key's algorithm (a reasonable proxy).
        return expectedKey.getAlgorithm().equals(record.getAlgorithm()) ? expectedKey : null;
    }
}
