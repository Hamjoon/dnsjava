###Test START##
```java
package org.xbill.DNS;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

/**
 * JUnit‑4 test suite for {@link Message}. The tests cover all public methods,
 * basic functionality and identified edge‑cases (invalid arguments, truncation,
 * cloning, etc.).
 */
public class MessageTest {

    private Message emptyMessage;
    private Name testName;
    private Record aRecord;
    private Record mxRecord;

    @Before
    public void setUp() throws Exception {
        emptyMessage = new Message();                     // random id, empty header
        testName = Name.fromString("example.com.");        // a valid domain name
        aRecord = Record.newRecord(testName, Type.A, DClass.IN, 3600);
        mxRecord = Record.newRecord(testName, Type.MX, DClass.IN, 3600);
    }

    /* --------------------------------------------------------------------
     *  Basic constructors and static factories
     * -------------------------------------------------------------------- */

    @Test
    public void testMessageIdConstructor() {
        Message m = new Message(0x1234);
        Assert.assertEquals(0x1234, m.getHeader().getID());
    }

    @Test
    public void testMessageByteArrayConstructor() throws IOException {
        byte[] wire = emptyMessage.toWire();   // simple valid wire representation
        Message m = new Message(wire);
        Assert.assertEquals(emptyMessage.getHeader().getID(), m.getHeader().getID());
    }

    @Test(expected = IOException.class)
    public void testMessageByteArrayConstructor_invalid() throws IOException {
        // malformed data – too short to contain a DNS header
        new Message(new byte[] {0x00, 0x01});
    }

    @Test
    public void testMessageByteBufferConstructor() throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(emptyMessage.toWire());
        Message m = new Message(buf);
        Assert.assertEquals(emptyMessage.getHeader().getID(), m.getHeader().getID());
    }

    @Test
    public void testNewQueryFactory() {
        Message q = Message.newQuery(aRecord);
        Assert.assertEquals(Opcode.QUERY, q.getHeader().getOpcode());
        Assert.assertTrue(q.getHeader().getFlag(Flags.RD));
        Assert.assertEquals(aRecord, q.getQuestion());
    }

    @Test
    public void testNewUpdateFactory() {
        Message upd = Message.newUpdate(testName);
        Assert.assertTrue(upd instanceof Update);
    }

    /* --------------------------------------------------------------------
     *  Header handling
     * -------------------------------------------------------------------- */

    @Test
    public void testSetAndGetHeader() {
        Header h = new Header(0xABCD);
        emptyMessage.setHeader(h);
        Assert.assertSame(h, emptyMessage.getHeader());
    }

    /* --------------------------------------------------------------------
     *  Record manipulation (add / remove / find)
     * -------------------------------------------------------------------- */

    @Test
    public void testAddAndFindRecord() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        Assert.assertTrue(emptyMessage.findRecord(aRecord, Section.ANSWER));
        Assert.assertTrue(emptyMessage.findRecord(aRecord));   // any section
    }

    @Test
    public void testRemoveRecordSuccess() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        boolean removed = emptyMessage.removeRecord(aRecord, Section.ANSWER);
        Assert.assertTrue(removed);
        Assert.assertFalse(emptyMessage.findRecord(aRecord, Section.ANSWER));
    }

    @Test
    public void testRemoveRecordFailure() {
        boolean removed = emptyMessage.removeRecord(aRecord, Section.ANSWER);
        Assert.assertFalse(removed);
    }

    @Test
    public void testRemoveAllRecords() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        emptyMessage.addRecord(mxRecord, Section.ANSWER);
        emptyMessage.removeAllRecords(Section.ANSWER);
        Assert.assertFalse(emptyMessage.findRecord(aRecord, Section.ANSWER));
        Assert.assertFalse(emptyMessage.findRecord(mxRecord, Section.ANSWER));
        Assert.assertEquals(0, emptyMessage.getHeader().getCount(Section.ANSWER));
    }

    @Test
    public void testFindRRset() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        Assert.assertTrue(emptyMessage.findRRset(testName, Type.A, Section.ANSWER));
        Assert.assertTrue(emptyMessage.findRRset(testName, Type.A));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindRRset_invalidType() {
        // Type.check throws IllegalArgumentException for illegal values
        emptyMessage.findRRset(testName, -1, Section.ANSWER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindRRset_invalidSection() {
        emptyMessage.findRRset(testName, Type.A, 99);
    }

    /* --------------------------------------------------------------------
     *  Section getters
     * -------------------------------------------------------------------- */

    @Test
    public void testGetSectionAndArray() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        List<Record> list = emptyMessage.getSection(Section.ANSWER);
        Assert.assertEquals(1, list.size());
        Assert.assertSame(aRecord, list.get(0));

        Record[] arr = emptyMessage.getSectionArray(Section.ANSWER);
        Assert.assertEquals(1, arr.length);
        Assert.assertSame(aRecord, arr[0]);
    }

    @Test
    public void testGetSectionRRsets() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        List<RRset> rrsets = emptyMessage.getSectionRRsets(Section.ANSWER);
        Assert.assertEquals(1, rrsets.size());
        Assert.assertEquals(testName, rrsets.get(0).getName());
        Assert.assertEquals(Type.A, rrsets.get(0).getType());
    }

    @Test
    public void testGetQuestionEmpty() {
        Assert.assertNull(emptyMessage.getQuestion());
    }

    @Test
    public void testGetTSIGAbsent() {
        Assert.assertNull(emptyMessage.getTSIG());
    }

    @Test
    public void testGetOPTAbsent() {
        Assert.assertNull(emptyMessage.getOPT());
    }

    @Test
    public void testGetRcodeWithoutOPT() {
        // default Rcode is NOERROR (0)
        Assert.assertEquals(Rcode.NOERROR, emptyMessage.getRcode());
    }

    @Test
    public void testGetRcodeWithOPT() throws IOException {
        // Create an OPT record with extended rcode = 2 (=> value 2 << 4 = 32)
        OPTRecord opt = new OPTRecord(4096, 2, 0, 0);
        emptyMessage.addRecord(opt, Section.ADDITIONAL);
        Assert.assertEquals(Rcode.NOERROR + (2 << 4), emptyMessage.getRcode());
    }

    /* --------------------------------------------------------------------
     *  Wire conversion edge cases
     * -------------------------------------------------------------------- */

    @Test
    public void testToWireBasic() {
        byte[] wire = emptyMessage.toWire();
        Assert.assertTrue(wire.length > Header.LENGTH);
    }

    @Test
    public void testToWireWithMaxLengthEnough() {
        byte[] wire = emptyMessage.toWire(65535);
        Assert.assertTrue(wire.length > 0);
    }

    @Test
    public void testToWireWithMaxLengthTooSmall() {
        // Header.LENGTH is the minimum size of a DNS message
        int tooSmall = Header.LENGTH - 1;
        byte[] wire = emptyMessage.toWire(tooSmall);
        Assert.assertEquals(0, wire.length);
    }

    @Test(expected = MessageSizeExceededException.class)
    public void testToWireTruncateFalseThrows() throws MessageSizeExceededException {
        // Force a truncation by using a very small maxLength and request no truncation
        emptyMessage.toWire(Header.LENGTH, false);
    }

    @Test
    public void testToWireTruncateTrueDoesNotThrow() throws Exception {
        // Same small size but with truncate=true – should return a (possibly empty) array
        byte[] wire = emptyMessage.toWire(Header.LENGTH, true);
        Assert.assertNotNull(wire);
    }

    /* --------------------------------------------------------------------
     *  TSIG handling (basic state checks)
     * -------------------------------------------------------------------- */

    @Test
    public void testTSIGStateDefaults() {
        Assert.assertFalse(emptyMessage.isSigned());
        Assert.assertFalse(emptyMessage.isVerified());
    }

    @Test
    public void testSetTSIGDoesNotChangeSignedStateImmediately() {
        TSIG tsig = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."),
                Name.fromString("key.example.com."), "testkey");
        emptyMessage.setTSIG(tsig);
        // The message is still considered unsigned until it is rendered
        Assert.assertFalse(emptyMessage.isSigned());
    }

    /* --------------------------------------------------------------------
     *  Section string conversion
     * -------------------------------------------------------------------- */

    @Test
    public void testSectionToStringEmpty() {
        String s = emptyMessage.sectionToString(Section.ANSWER);
        Assert.assertEquals("", s);
    }

    @Test
    public void testSectionToStringWithRecord() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        String s = emptyMessage.sectionToString(Section.ANSWER);
        Assert.assertTrue(s.contains("example.com."));
        Assert.assertTrue(s.contains("A"));
    }

    /* --------------------------------------------------------------------
     *  toString() – sanity check that it produces a non‑empty string
     * -------------------------------------------------------------------- */

    @Test
    public void testToStringContainsHeaderAndSize() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        String txt = emptyMessage.toString();
        Assert.assertTrue(txt.contains("Message size"));
        Assert.assertTrue(txt.contains(";; ANSWER"));
    }

    /* --------------------------------------------------------------------
     *  Clone – deep copy verification
     * -------------------------------------------------------------------- */

    @Test
    public void testCloneDeepCopy() {
        emptyMessage.addRecord(aRecord, Section.ANSWER);
        Message clone = emptyMessage.clone();

        // The clone must contain the same record
        Assert.assertTrue(clone.findRecord(aRecord, Section.ANSWER));

        // Modify original – clone should stay unchanged
        emptyMessage.removeAllRecords(Section.ANSWER);
        Assert.assertFalse(emptyMessage.findRecord(aRecord, Section.ANSWER));
        Assert.assertTrue(clone.findRecord(aRecord, Section.ANSWER));
    }

    /* --------------------------------------------------------------------
     *  Resolver association
     * -------------------------------------------------------------------- */

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
        @Override public void setTimeout(java.time.Duration timeout) {}
        @Override public java.time.Duration getTimeout() { return null; }
        @Override public Message send(Message query) throws IOException { return null; }
        @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
        @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
        @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
    }

    @Test
    public void testSetAndGetResolver() {
        DummyResolver resolver = new DummyResolver();
        emptyMessage.setResolver(resolver);
        Optional<Resolver> opt = emptyMessage.getResolver();
        Assert.assertTrue(opt.isPresent());
        Assert.assertSame(resolver, opt.get());
    }

    /* --------------------------------------------------------------------
     *  Normalization – simple successful path
     * -------------------------------------------------------------------- */

    @Test
    public void testNormalizeSimple() throws Exception {
        // Build a query message
        Message query = Message.newQuery(aRecord);
        // Build a response that simply echoes the question
        Message response = new Message();
        response.addRecord(aRecord, Section.QUESTION);
        response.addRecord(aRecord, Section.ANSWER);
        // Normalization should succeed and keep the answer record
        Message normalized = response.normalize(query);
        Assert.assertNotNull(normalized);
        Assert.assertTrue(normalized.findRecord(aRecord, Section.ANSWER));
    }

    @Test
    public void testNormalizeReturnsSameForNon‑NOERROR() throws Exception {
        Message query = Message.newQuery(aRecord);
        Message resp = new Message();
        resp.getHeader().setRcode(Rcode.SERVFAIL);   // non‑NOERROR/NXDOMAIN
        Message norm = resp.normalize(query);
        Assert.assertSame(resp, norm);
    }
}
```
###Test END##