###Test START##
```java
/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.Message}.
 *
 * <p>The tests exercise the public API of {@code Message} using only the
 * constructors and methods that are part of the supplied source or the
 * explicitly listed dependency signatures.  No mocking frameworks are used;
 * real objects from the dnsjava library are created.</p>
 *
 * <p>Each test focuses on a distinct functional aspect (typical use,
 * edge case, error handling) and together they provide reasonably comprehensive
 * coverage of the class.</p>
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

/**
 * Test cases for {@link Message}.
 */
public class MessageTest {

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Creates a simple A record for the given name. */
    private Record makeARecord(String name) throws IOException {
        Name n = new Name(name);
        // Type 1 = A, DClass 1 = IN, ttl = 300, address = 127.0.0.1
        return Record.newRecord(n, 1, 1, 300, InetAddress.getByName("127.0.0.1").getAddress());
    }

    /** Creates a generic record of the given type. */
    private Record makeRecord(String name, int type) throws IOException {
        return Record.newRecord(new Name(name), type, 1, 300);
    }

    /** Returns a fresh Message containing a single QUESTION record. */
    private Message makeQueryMessage() throws IOException {
        Record q = makeRecord("example.com.", 1); // type A
        Message m = new Message();
        m.addRecord(q, 0); // section 0 = QUESTION
        return m;
    }

    /* --------------------------------------------------------------------- */
    /* 1. Message construction and basic header handling                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNewQueryCreatesMessageWithQuestionAndFlags() throws IOException {
        Record q = makeRecord("example.com.", 1);
        Message m = Message.newQuery(q);

        // QUESTION section must contain exactly the supplied record
        assertEquals(q, m.getQuestion());

        // Header opcode for a query is 0 (Opcode.QUERY) and the recursion‑desired
        // flag must be set (Flags.RD).  The library defines 0 for QUERY.
        assertEquals(0, m.getHeader().getOpcode());

        // The recursion‑desired flag is bit 8 (value 0x0100) in the DNS header.
        // Header#setFlag works with the bit mask directly.
        assertTrue(m.getHeader().getFlag(0x0100));
    }

    @Test
    public void testHeaderReplace() {
        Header h1 = new Header(0x1234);
        Header h2 = new Header(0xABCD);
        Message m = new Message(h1);
        assertEquals(0x1234, m.getHeader().getID());

        m.setHeader(h2);
        assertEquals(0xABCD, m.getHeader().getID());
    }

    /* --------------------------------------------------------------------- */
    /* 2. Record addition / removal and header count adjustments             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemoveRecordUpdatesCounts() throws IOException {
        Message m = new Message();
        Record a = makeARecord("a.example.com.");
        Record ns = makeRecord("example.com.", 2); // type NS

        // initially zero counts
        for (int i = 0; i < 4; i++) {
            assertEquals(0, m.getHeader().getCount(i));
        }

        // add to ANSWER (section 1) and AUTHORITY (section 2)
        m.addRecord(a, 1);
        m.addRecord(ns, 2);

        assertEquals(1, m.getHeader().getCount(1));
        assertEquals(1, m.getHeader().getCount(2));

        // remove NS record
        assertTrue(m.removeRecord(ns, 2));
        assertEquals(0, m.getHeader().getCount(2));

        // removing non‑existent record returns false and does not change count
        assertFalse(m.removeRecord(ns, 2));
        assertEquals(0, m.getHeader().getCount(2));
    }

    @Test
    public void testRemoveAllRecordsClearsSection() throws IOException {
        Message m = new Message();
        m.addRecord(makeARecord("a.example.com."), 1);
        m.addRecord(makeARecord("b.example.com."), 1);
        assertEquals(2, m.getHeader().getCount(1));

        m.removeAllRecords(1);
        assertEquals(0, m.getHeader().getCount(1));
        assertTrue(m.getSection(1).isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 3. Find record / RRset utilities                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFindRecordAndFindRRset() throws IOException {
        Message m = new Message();
        Record a1 = makeARecord("a.example.com.");
        Record a2 = makeARecord("b.example.com.");
        Record ns = makeRecord("example.com.", 2); // NS

        m.addRecord(a1, 1);
        m.addRecord(a2, 1);
        m.addRecord(ns, 2);

        assertTrue(m.findRecord(a1, 1));
        assertFalse(m.findRecord(a1, 2));

        // a1 and a2 share type 1 (A) but have different names – both should be found
        assertTrue(m.findRRset(new Name("a.example.com."), 1, 1));
        assertTrue(m.findRRset(new Name("b.example.com."), 1, 1));
        // NS in authority section
        assertTrue(m.findRRset(new Name("example.com."), 2, 2));
        // non‑existent type
        assertFalse(m.findRRset(new Name("example.com."), 5, 2));
    }

    /* --------------------------------------------------------------------- */
    /* 4. Section and record retrieval                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetQuestionReturnsFirstInSection() throws IOException {
        Message m = new Message();
        Record q1 = makeRecord("example.com.", 1);
        Record q2 = makeRecord("example.org.", 1);
        m.addRecord(q1, 0);
        m.addRecord(q2, 0);
        assertEquals(q1, m.getQuestion());
    }

    @Test
    public void testGetTSIGReturnsNullWhenAbsent() throws IOException {
        Message m = new Message();
        assertNull(m.getTSIG());
    }

    @Test
    public void testGetOPTPresentAndAbsent() throws IOException {
        Message m = new Message();
        assertNull(m.getOPT());

        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        m.addRecord(opt, 3); // ADDITIONAL
        assertSame(opt, m.getOPT());
    }

    @Test
    public void testGetRcodeWithExtendedRcodeFromOPT() throws IOException {
        Message m = new Message();
        // base rcode = 0 (NOERROR)
        assertEquals(0, m.getRcode());

        // OPT with extended rcode = 3 (i.e. 3 << 4 = 48)
        OPTRecord opt = new OPTRecord(4096, 3, 0, 0);
        m.addRecord(opt, 3);
        assertEquals(48, m.getRcode());
    }

    @Test
    public void testDeprecatedGetSectionArrayReturnsEmptyArrayWhenNoSection() {
        Message m = new Message();
        Record[] arr = m.getSectionArray(1); // ANSWER is empty
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test
    public void testGetSectionReturnsUnmodifiableList() throws IOException {
        Message m = new Message();
        Record a = makeARecord("a.example.com.");
        m.addRecord(a, 1);
        List<Record> list = m.getSection(1);
        assertEquals(1, list.size());

        try {
            list.add(makeARecord("b.example.com."));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void testGetSectionRRsetsGroupsByRRset() throws IOException {
        Message m = new Message();
        Record a1 = makeARecord("a.example.com.");
        Record a2 = makeARecord("a.example.com."); // same name/type => same RRset
        Record ns = makeRecord("example.com.", 2);
        m.addRecord(a1, 1);
        m.addRecord(a2, 1);
        m.addRecord(ns, 2);

        List<RRset> answerSets = m.getSectionRRsets(1);
        assertEquals(1, answerSets.size()); // both A records in one RRset

        List<RRset> authSets = m.getSectionRRsets(2);
        assertEquals(1, authSets.size());
        assertEquals(2, authSets.get(0).getType());
    }

    /* --------------------------------------------------------------------- */
    /* 5. Wire format generation and truncation                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToWireAndNumBytes() throws IOException {
        Message m = new Message();
        Record a = makeARecord("a.example.com.");
        m.addRecord(a, 1);
        byte[] wire = m.toWire();
        assertNotNull(wire);
        assertTrue(wire.length > Header.LENGTH);
        assertEquals(wire.length, m.numBytes());
    }

    @Test
    public void testToWireWithMaxLengthTruncatesAndSetsTCFlag() throws IOException {
        Message m = new Message();
        // add many records to force truncation
        for (int i = 0; i < 50; i++) {
            m.addRecord(makeARecord("host" + i + ".example.com."), 1);
        }
        // Use a deliberately small max length
        byte[] wire = m.toWire(100);
        assertTrue(wire.length <= 100);
        // Verify that the TC flag (bit 9, value 0x0200) is set in the header
        Header hdr = new Header(wire);
        assertTrue(hdr.getFlag(0x0200));
    }

    /* --------------------------------------------------------------------- */
    /* 6. TSIG handling (signing state)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetTSIGAndSignedState() throws IOException {
        Message m = new Message();
        // Simple HMAC-MD5 key
        TSIG tsig = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("key.example."), "abcd");
        m.setTSIG(tsig);
        // before conversion to wire the message is considered signed
        assertTrue(m.isSigned());
        // verification state defaults to not verified
        assertFalse(m.isVerified());
    }

    /* --------------------------------------------------------------------- */
    /* 7. Clone deep copy verification                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCloneCreatesIndependentCopy() throws IOException {
        Message original = new Message();
        Record a = makeARecord("a.example.com.");
        original.addRecord(a, 1);

        Message copy = original.clone();
        // modify copy
        copy.addRecord(makeARecord("b.example.com."), 1);
        copy.getHeader().setID(0x9999);

        // original must stay unchanged
        assertEquals(1, original.getHeader().getCount(1));
        assertEquals(2, copy.getHeader().getCount(1));
        assertNotEquals(original.getHeader().getID(), copy.getHeader().getID());
    }

    /* --------------------------------------------------------------------- */
    /* 8. Resolver association                                                 */
    /* --------------------------------------------------------------------- */

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
        @Override public Duration getTimeout() { return Duration.ZERO; }
        @Override public Message send(Message query) throws IOException { return null; }
        @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
        @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
        @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
    }

    @Test
    public void testResolverSetAndGet() {
        Message m = new Message();
        Resolver r = new DummyResolver();
        m.setResolver(r);
        Optional<Resolver> opt = m.getResolver();
        assertTrue(opt.isPresent());
        assertSame(r, opt.get());
    }

    /* --------------------------------------------------------------------- */
    /* 9. Type‑allowed‑in‑section logic                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsTypeAllowedInSectionDefaults() {
        Message m = new Message();
        // AUTHORITY section allows NS (type 2)
        assertTrue(m.isTypeAllowedInSection(2, 2));
        // AUTHORITY disallows A (type 1)
        assertFalse(m.isTypeAllowedInSection(1, 2));

        // ADDITIONAL allows A (type 1) and AAAA (type 28)
        assertTrue(m.isTypeAllowedInSection(1, 3));
        assertTrue(m.isTypeAllowedInSection(28, 3));

        // ADDITIONAL disallows MX (type 15) unless system property disables hardening
        boolean hardening = Boolean.parseBoolean(System.getProperty("dnsjava.harden_unknown_additional", "true"));
        assertEquals(!hardening, m.isTypeAllowedInSection(15, 3));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Normalization – simple pass‑through case                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNormalizeReturnsSameMessageWhenNoChangesNeeded() throws IOException {
        // Build a minimal successful answer (NOERROR) with a single A record.
        Message query = Message.newQuery(makeRecord("example.com.", 1));
        Message response = new Message();
        response.header.setRcode(0); // NOERROR
        response.addRecord(query.getQuestion(), 0); // copy QUESTION
        response.addRecord(makeARecord("example.com."), 1); // ANSWER

        Message normalized = response.normalize(query);
        // Since the response is already valid, normalize should return a copy equal to the original
        assertNotNull(normalized);
        assertEquals(response.getHeader().getID(), normalized.getHeader().getID());
        assertEquals(response.getSection(1).size(), normalized.getSection(1).size());
        assertEquals(response.getSection(0).size(), normalized.getSection(0).size());
    }

    /* --------------------------------------------------------------------- */
    /* 11. toString produces a non‑null representation                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringProducesReadableOutput() throws IOException {
        Message m = new Message();
        m.addRecord(makeRecord("example.com.", 1), 0);
        m.addRecord(makeARecord("example.com."), 1);
        String s = m.toString();
        assertNotNull(s);
        assertTrue(s.contains(";; QUESTION"));
        assertTrue(s.contains("example.com."));
    }
}
```
###Test END##