package org.xbill.DNS.test;

import org.xbill.DNS.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Message}. The tests exercise the public API only
 * and avoid any mocking – real DNS objects are constructed using the
 * constructors and factory methods that are part of the library.
 */
public class MessageTest {

    private static final Name EXAMPLE = Name.fromString("example.com.");
    private static final Name ZONE = Name.fromString("example.org.");
    private static final Name ROOT = Name.root;

    /** Reset the system property that influences {@link Message#isTypeAllowedInSection}. */
    @Before
    public void setUp() {
        System.setProperty("dnsjava.harden_unknown_additional", "false");
    }

    @After
    public void tearDown() {
        System.clearProperty("dnsjava.harden_unknown_additional");
    }

    /** Helper to create a minimal A record (type = A, class = IN). */
    private Record aRecord(Name name) {
        return Record.newRecord(name, Type.A, DClass.IN, 300);
    }

    /** Helper to create a minimal SOA record for authority‑section tests. */
    private Record soaRecord(Name name) {
        return Record.newRecord(name, Type.SOA, DClass.IN, 3600);
    }

    /** Helper to create a minimal OPT record. */
    private OPTRecord optRecord() {
        return new OPTRecord(4096, 0, 0, 0);
    }

    @Test
    public void testConstructionAndHeader() {
        Message m = new Message(0x1234);
        assertEquals(0x1234, m.getHeader().getID());

        Message empty = new Message();
        assertNotNull(empty.getHeader());
    }

    @Test
    public void testNewQueryFactory() {
        Record q = aRecord(EXAMPLE);
        Message query = Message.newQuery(q);
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode());
        assertTrue(query.getHeader().getFlag(Flags.RD));
        assertSame(q, query.getQuestion());
        assertEquals(1, query.getHeader().getCount(Section.QUESTION));
    }

    @Test
    public void testAddAndRemoveRecord() {
        Message m = new Message();
        Record a = aRecord(EXAMPLE);
        m.addRecord(a, Section.ANSWER);
        assertTrue(m.findRecord(a, Section.ANSWER));
        assertEquals(1, m.getHeader().getCount(Section.ANSWER));

        // remove succeeds
        assertTrue(m.removeRecord(a, Section.ANSWER));
        assertFalse(m.findRecord(a, Section.ANSWER));
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));

        // remove non‑existent record returns false
        assertFalse(m.removeRecord(a, Section.ANSWER));
    }

    @Test
    public void testFindRecordInAnySection() {
        Message m = new Message();
        Record a = aRecord(EXAMPLE);
        Record soa = soaRecord(ZONE);
        m.addRecord(a, Section.ANSWER);
        m.addRecord(soa, Section.AUTHORITY);

        assertTrue(m.findRecord(a));
        assertTrue(m.findRecord(soa));
        Record other = aRecord(Name.fromString("other.com."));
        assertFalse(m.findRecord(other));
    }

    @Test
    public void testFindRRsetAndRRsetGrouping() {
        Message m = new Message();
        Record a1 = aRecord(EXAMPLE);
        Record a2 = aRecord(EXAMPLE); // same name & type – should be same RRset
        Record soa = soaRecord(ZONE);

        m.addRecord(a1, Section.ANSWER);
        m.addRecord(a2, Section.ANSWER);
        m.addRecord(soa, Section.AUTHORITY);

        assertTrue(m.findRRset(EXAMPLE, Type.A, Section.ANSWER));
        assertTrue(m.findRRset(ZONE, Type.SOA, Section.AUTHORITY));
        assertFalse(m.findRRset(EXAMPLE, Type.SOA, Section.ANSWER));

        // getSectionRRsets must return a single RRset for the two A records
        List<RRset> answerSets = m.getSectionRRsets(Section.ANSWER);
        assertEquals(1, answerSets.size());
        RRset aSet = answerSets.get(0);
        assertEquals(Type.A, aSet.getType());
        assertEquals(2, aSet.size());
    }

    @Test
    public void testGetOPTAndExtendedRcode() {
        Message m = new Message();
        OPTRecord opt = optRecord(); // default extended rcode = 0
        m.addRecord(opt, Section.ADDITIONAL);
        assertSame(opt, m.getOPT());

        // set an extended rcode via the OPT record
        OPTRecord opt2 = new OPTRecord(4096, 5, 0, 0); // extended rcode = 5
        Message m2 = new Message();
        m2.addRecord(opt2, Section.ADDITIONAL);
        assertEquals(5, m2.getRcode()); // no base rcode set, so just 5
    }

    @Test
    public void testSectionArrayAndListAccessors() {
        Message m = new Message();
        Record a = aRecord(EXAMPLE);
        m.addRecord(a, Section.ANSWER);

        // deprecated array accessor
        Record[] arr = m.getSectionArray(Section.ANSWER);
        assertEquals(1, arr.length);
        assertSame(a, arr[0]);

        // empty section returns the shared empty array
        assertSame(Message.getSectionArray(Section.AUTHORITY), m.getSectionArray(Section.AUTHORITY));

        // list accessor returns an unmodifiable list
        List<Record> list = m.getSection(Section.ANSWER);
        assertEquals(1, list.size());
        assertSame(a, list.get(0));
        try {
            list.add(a);
            fail("List should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testToWireAndTruncation() throws IOException {
        Message m = new Message();
        // add enough records to exceed a tiny maxLength and force truncation
        for (int i = 0; i < 20; i++) {
            m.addRecord(aRecord(Name.fromString("host" + i + ".example.com.")), Section.ANSWER);
        }

        byte[] full = m.toWire();
        assertTrue(full.length > Header.LENGTH);

        // a maxLength smaller than the header must yield an empty array
        byte[] empty = m.toWire(Header.LENGTH - 1);
        assertEquals(0, empty.length);

        // a reasonable maxLength should produce a truncated message with the TC flag set
        byte[] truncated = m.toWire(200);
        assertTrue(truncated.length <= 200);
        // Verify that the TC flag is set in the wire representation
        DNSInput in = new DNSInput(truncated);
        Header hdr = new Header(in);
        assertTrue(hdr.getFlag(Flags.TC));
    }

    @Test
    public void testTSIGRelatedMethods() {
        Message m = new Message();
        // No TSIG set – isSigned should be false
        assertFalse(m.isSigned());
        assertFalse(m.isVerified());

        // The TSIG state cannot be changed via public API, but setTSIG does not affect isSigned
        TSIG tsig = TSIG.fromString("hmac-md5.sig-alg.reg.int. key");
        m.setTSIG(tsig, Rcode.NOERROR, null);
        assertFalse(m.isSigned()); // still false because tsigState remains TSIG_UNSIGNED
    }

    @Test
    public void testSectionToStringAndToString() {
        Message m = new Message();
        Record a = aRecord(EXAMPLE);
        m.addRecord(a, Section.ANSWER);
        m.addRecord(optRecord(), Section.ADDITIONAL);

        String secStr = m.sectionToString(Section.ANSWER);
        assertTrue(secStr.contains(EXAMPLE.toString()));

        String full = m.toString();
        // Header line must be present
        assertTrue(full.contains(";;"));
        // Answer section header
        assertTrue(full.contains(Section.longString(Section.ANSWER) + ":"));
        // Record string representation
        assertTrue(full.contains(EXAMPLE.toString()));
        // Message size line appears at the end
        assertTrue(full.matches("(?s).*Message size: \\d+ bytes$"));
    }

    @Test
    public void testCloneIndependence() {
        Message original = new Message();
        Record a = aRecord(EXAMPLE);
        original.addRecord(a, Section.ANSWER);
        original.setResolver(new Resolver() {
            // minimal anonymous implementation – only needed for the test
            public void setPort(int port) {}
            public void setTCP(boolean flag) {}
            public void setIgnoreTruncation(boolean flag) {}
            public void setEDNS(int version) {}
            public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
            public void setTSIGKey(TSIG key) {}
            public void setTimeout(Duration timeout) {}
            public Duration getTimeout() { return null; }
            public Message send(Message query) { return null; }
            public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
            public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
            public Object sendAsync(Message query, ResolverListener listener) { return null; }
        });

        Message clone = original.clone();
        // The clone must have the same record but a different list instance
        assertEquals(1, clone.getHeader().getCount(Section.ANSWER));
        assertNotSame(original.getSection(Section.ANSWER), clone.getSection(Section.ANSWER));

        // Changing the clone must not affect the original
        Record a2 = aRecord(Name.fromString("other.example.com."));
        clone.addRecord(a2, Section.ANSWER);
        assertEquals(2, clone.getHeader().getCount(Section.ANSWER));
        assertEquals(1, original.getHeader().getCount(Section.ANSWER));

        // Resolver is cloned correctly (Optional should be present)
        assertTrue(clone.getResolver().isPresent());
    }

    @Test
    public void testResolverSetterGetter() {
        Message m = new Message();
        Resolver r = new Resolver() {
            public void setPort(int port) {}
            public void setTCP(boolean flag) {}
            public void setIgnoreTruncation(boolean flag) {}
            public void setEDNS(int version) {}
            public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {}
            public void setTSIGKey(TSIG key) {}
            public void setTimeout(Duration timeout) {}
            public Duration getTimeout() { return null; }
            public Message send(Message query) { return null; }
            public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
            public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
            public Object sendAsync(Message query, ResolverListener listener) { return null; }
        };
        m.setResolver(r);
        assertTrue(m.getResolver().isPresent());
        assertSame(r, m.getResolver().get());
    }

    @Test
    public void testIsTypeAllowedInSection() {
        Message m = new Message();

        // AUTHORITY: SOA is allowed
        assertTrue(m.isTypeAllowedInSection(Type.SOA, Section.AUTHORITY));

        // AUTHORITY: A is NOT allowed
        assertFalse(m.isTypeAllowedInSection(Type.A, Section.AUTHORITY));

        // ADDITIONAL: A is allowed
        assertTrue(m.isTypeAllowedInSection(Type.A, Section.ADDITIONAL));

        // ADDITIONAL: ANY unknown type should be allowed because we disabled hardening
        assertTrue(m.isTypeAllowedInSection(Type.SRV + 1000, Section.ADDITIONAL));
    }

    @Test
    public void testNormalizeSimpleAnswer() throws IOException {
        // Build a query message
        Record q = Record.newRecord(EXAMPLE, Type.A, DClass.IN, 0);
        Message query = Message.newQuery(q);

        // Build a response that contains exactly the answer we asked for
        Message response = new Message();
        response.addRecord(q, Section.QUESTION);
        response.addRecord(aRecord(EXAMPLE), Section.ANSWER);
        response.getHeader().setRcode(Rcode.NOERROR);
        response.getHeader().setOpcode(Opcode.QUERY);
        response.getHeader().incCount(Section.QUESTION);
        response.getHeader().incCount(Section.ANSWER);

        // Normalization should return a message with the same answer RRset
        Message normalized = response.normalize(query);
        assertNotNull(normalized);
        List<RRset> answerSets = normalized.getSectionRRsets(Section.ANSWER);
        assertEquals(1, answerSets.size());
        RRset set = answerSets.get(0);
        assertEquals(EXAMPLE, set.getName());
        assertEquals(Type.A, set.getType());
    }

    /**
     * Utility to invoke the package‑private {@code sectionToString(int)} method via reflection.
     * This allows verification that the method does not throw and returns a non‑null string.
     */
    private String invokeSectionToString(Message m, int section) {
        try {
            java.lang.reflect.Method mtd = Message.class.getDeclaredMethod("sectionToString", int.class);
            mtd.setAccessible(true);
            return (String) mtd.invoke(m, section);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPrivateSectionToStringViaReflection() {
        Message m = new Message();
        Record a = aRecord(EXAMPLE);
        m.addRecord(a, Section.ANSWER);
        String s = invokeSectionToString(m, Section.ANSWER);
        assertNotNull(s);
        assertTrue(s.contains(EXAMPLE.toString()));
    }
}
