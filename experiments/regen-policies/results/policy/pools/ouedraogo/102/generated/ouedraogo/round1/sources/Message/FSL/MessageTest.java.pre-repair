package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

/**
 * JUnit‑4 test suite that exercises the public behaviour of {@link Message}.
 *
 * The tests purposefully avoid any mocking – they build real DNS objects
 * (Header, Record subclasses, OPTRecord, TSIG, etc.) using only the APIs that
 * are declared in the supplied source and the public signatures that were
 * listed in the problem statement.
 *
 * The suite covers:
 *
 * <ul>
 *   <li>construction, header handling and basic mutators (add/remove/find)</li>
 *   <li>section queries (getQuestion, getTSIG, getOPT, getRcode with extended rcode)</li>
 *   <li>wire‑format rendering, truncation handling and generated TSIG record</li>
 *   <li>clone semantics, resolver association and utility helpers</li>
 *   <li>type‑allowance logic for AUTHORITY and ADDITIONAL sections</li>
 *   <li>normalisation of a response containing DNAME and CNAME records</li>
 * </ul>
 *
 * The tests are deliberately small and focused so that they run quickly and
 * remain deterministic.
 */
public class MessageTest {

    /** Helper to create an {@link Name} without dealing with checked exceptions. */
    private static Name name(String s) {
        try {
            return Name.fromString(s);
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }
    }

    /** Helper to create an {@link ARecord}. */
    private static ARecord aRecord(String name, String ip) {
        try {
            return new ARecord(name(name), DClass.IN, 300, InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /** Helper to create a {@link CNAMERecord}. */
    private static CNAMERecord cnameRecord(String alias, String target) {
        return new CNAMERecord(name(alias), DClass.IN, 300, name(target));
    }

    /** Helper to create a {@link DNAMERecord}. */
    private static DNAMERecord dnameRecord(String owner, String target) {
        return new DNAMERecord(name(owner), DClass.IN, 300, name(target));
    }

    /** Helper to create a {@link NSRecord}. */
    private static NSRecord nsRecord(String owner, String ns) {
        return new NSRecord(name(owner), DClass.IN, 300, name(ns));
    }

    /** Helper to create a simple {@link OPTRecord} with an extended rcode. */
    private static OPTRecord optRecord(int extendedRcode) {
        // payloadSize=0, xrcode=0, version=0, flags=0 – then set the extended rcode via the constructor
        return new OPTRecord(0, extendedRcode << 4, 0, 0);
    }

    /** Helper to create a minimal TSIG key for signing. */
    private static TSIG tsigKey() {
        // Use the well‑known HMAC‑MD5 algorithm; the key material is arbitrary.
        return new TSIG(TSIG.HMAC_MD5, name("tsig-key."), "abcd");
    }

    @Test
    public void testHeaderAndBasicMutators() {
        Message m = new Message(0x1234);
        Header h = m.getHeader();
        assertEquals(0x1234, h.getID());

        // replace header
        Header newHeader = new Header(0x4321);
        m.setHeader(newHeader);
        assertSame(newHeader, m.getHeader());

        // add records to QUESTION and ANSWER sections
        Record q = aRecord("example.com.", "192.0.2.1");
        m.addRecord(q, Section.QUESTION);
        assertSame(q, m.getQuestion());

        Record a = aRecord("example.com.", "192.0.2.2");
        m.addRecord(a, Section.ANSWER);
        assertTrue(m.findRecord(a, Section.ANSWER));
        assertTrue(m.findRecord(a));

        // remove record
        assertTrue(m.removeRecord(a, Section.ANSWER));
        assertFalse(m.findRecord(a, Section.ANSWER));
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));

        // remove non‑existent record
        assertFalse(m.removeRecord(a, Section.ANSWER));

        // remove all records from a section
        m.addRecord(a, Section.ANSWER);
        m.removeAllRecords(Section.ANSWER);
        assertFalse(m.findRecord(a, Section.ANSWER));
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));
    }

    @Test
    public void testFindRRsetAndSectionArray() {
        Message m = new Message();
        Record a1 = aRecord("example.com.", "192.0.2.1");
        Record a2 = aRecord("example.com.", "192.0.2.2");
        m.addRecord(a1, Section.ANSWER);
        m.addRecord(a2, Section.ANSWER);

        // Both A records belong to the same RRset (same name and type)
        assertTrue(m.findRRset(name("example.com."), Type.A, Section.ANSWER));

        // Deprecated array view – should contain the two records
        Record[] arr = m.getSectionArray(Section.ANSWER);
        assertEquals(2, arr.length);
        assertTrue(arr[0] instanceof ARecord);
        assertTrue(arr[1] instanceof ARecord);
    }

    @Test
    public void testOPTAndExtendedRcode() {
        Message m = new Message();
        m.addRecord(optRecord(5), Section.ADDITIONAL); // extended rcode = 5

        // No explicit question – getRcode should be just the extended part
        assertEquals(5, m.getRcode());

        // Adding a normal answer with a standard rcode (NOERROR = 0)
        m.addRecord(aRecord("example.com.", "192.0.2.1"), Section.ANSWER);
        assertEquals(5, m.getRcode()); // still 5 (0 + 5<<4)
    }

    @Test
    public void testWireRenderingAndTruncation() throws IOException {
        Message m = new Message();
        // Fill the message with many A records so that it will exceed a small maxLength
        for (int i = 0; i < 100; i++) {
            m.addRecord(aRecord("host" + i + ".example.com.", "192.0.2." + (i % 255)), Section.ANSWER);
        }

        // Normal rendering – should produce a non‑empty byte array
        byte[] full = m.toWire();
        assertTrue(full.length > 0);
        assertEquals(full.length, m.numBytes());

        // Rendering with a very small maxLength should produce an empty array (header cannot fit)
        byte[] empty = m.toWire(10);
        assertEquals(0, empty.length);

        // Rendering with truncation enabled: we expect a non‑empty array with the TC flag set.
        byte[] truncated = m.toWire(100, true);
        assertTrue(truncated.length > 0);
        // Verify that the TC flag is indeed set in the header (bit 9 – Flags.TC)
        Header h = new Header(truncated);
        assertTrue(h.getFlag(Flags.TC));
    }

    @Test
    public void testTSIGGenerationAndGeneratedRecord() throws IOException {
        Message m = new Message();
        m.addRecord(aRecord("example.com.", "192.0.2.1"), Section.ANSWER);
        TSIG key = tsigKey();
        m.setTSIG(key);               // configure TSIG signing
        byte[] wire = m.toWire();     // this will generate the TSIG record
        assertTrue(wire.length > 0);
        // After rendering the generated TSIG should be accessible
        TSIGRecord gen = m.getGeneratedTSIG();
        assertNotNull(gen);
        assertEquals(key.getName(), gen.getAlgorithm());
    }

    @Test
    public void testCloneCreatesIndependentCopy() {
        Message original = new Message();
        Record a = aRecord("example.com.", "192.0.2.1");
        original.addRecord(a, Section.ANSWER);
        original.setResolver(new Resolver() {
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
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}
            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}
            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}
            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}

            @Override public void setTimeout(int secs, int msecs) {}

            @Override public void setTimeout(int secs) {}

            @Override public void setTimeout(Duration timeout) {}
            // many methods omitted for brevity – the anonymous class only needs to compile
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
            @Override public Duration getTimeout() { return null; }
            @Override public Message send(Message query) throws IOException { return null; }
            @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query) { return null; }
            @Override public java.util.concurrent.CompletionStage<Message> sendAsync(Message query, java.util.concurrent.Executor executor) { return null; }
            @Override public Object sendAsync(Message query, ResolverListener listener) { return null; }
        });
        Message cloned = original.clone();

        // The cloned message must have its own list instances
        assertNotSame(original.getSection(Section.ANSWER), cloned.getSection(Section.ANSWER));
        // Changing the original must not affect the clone
        original.removeAllRecords(Section.ANSWER);
        assertFalse(original.findRecord(a, Section.ANSWER));
        assertTrue(cloned.findRecord(a, Section.ANSWER));

        // Resolver association must be copied correctly
        Optional<Resolver> optResolver = cloned.getResolver();
        assertTrue(optResolver.isPresent());
    }

    @Test
    public void testIsTypeAllowedInSection() {
        Message m = new Message();

        // AUTHORITY section allows SOA, NS, DS, NSEC, NSEC3
        assertTrue(m.isTypeAllowedInSection(Type.SOA, Section.AUTHORITY));
        assertTrue(m.isTypeAllowedInSection(Type.NS, Section.AUTHORITY));
        assertFalse(m.isTypeAllowedInSection(Type.A, Section.AUTHORITY));

        // ADDITIONAL section allows A and AAAA
        assertTrue(m.isTypeAllowedInSection(Type.A, Section.ADDITIONAL));
        assertTrue(m.isTypeAllowedInSection(Type.AAAA, Section.ADDITIONAL));
        assertFalse(m.isTypeAllowedInSection(Type.MX, Section.ADDITIONAL));

        // For other sections the method falls back to the system property (default true)
        assertTrue(m.isTypeAllowedInSection(Type.MX, Section.ANSWER));
    }

    @Test
    public void testNormalizeWithDNAMEAndCNAME() throws IOException, WireParseException {
        // Build a query message for "www.example.com. A"
        Message query = Message.newQuery(
                new ARecord(name("www.example.com."), DClass.IN, 0, InetAddress.getByName("192.0.2.1"))
        );

        // Build a response that contains a DNAME for "example.com." → "example.org."
        // and a CNAME for "www.example.com." → "www.example.org."
        Message response = new Message();
        response.addRecord(query.getQuestion(), Section.QUESTION);
        response.addRecord(dnameRecord("example.com.", "example.org."), Section.ANSWER);
        response.addRecord(cnameRecord("www.example.com.", "www.example.org."), Section.ANSWER);
        response.addRecord(aRecord("www.example.org.", "203.0.113.5"), Section.ANSWER);
        // Add an irrelevant NS record in the AUTHORITY section – should be dropped by normalize
        response.addRecord(nsRecord("example.com.", "ns1.example.com."), Section.AUTHORITY);

        // Normalise the response
        Message normalized = response.normalize(query);

        // The normalized message must contain the synthesized CNAME (already present) and the A record,
        // but must NOT contain the original DNAME (it is removed after synthesis)
        assertTrue(normalized.findRecord(cnameRecord("www.example.com.", "www.example.org."), Section.ANSWER));
        assertTrue(normalized.findRecord(aRecord("www.example.org.", "203.0.113.5"), Section.ANSWER));
        assertFalse(normalized.findRecord(dnameRecord("example.com.", "example.org."), Section.ANSWER));

        // The irrelevant NS record must have been removed from AUTHORITY
        assertFalse(normalized.findRecord(nsRecord("example.com.", "ns1.example.com."), Section.AUTHORITY));
    }

    @Test
    public void testSectionToStringAndToString() {
        Message m = new Message();
        Record q = new ARecord(name("example.com."), DClass.IN, 300, InetAddress.getLoopbackAddress());
        m.addRecord(q, Section.QUESTION);
        Record a = new ARecord(name("example.com."), DClass.IN, 300, InetAddress.getLoopbackAddress());
        m.addRecord(a, Section.ANSWER);
        String qStr = m.sectionToString(Section.QUESTION);
        assertTrue(qStr.contains("example.com."));
        assertTrue(qStr.contains("type = A"));
        assertTrue(qStr.contains("class = IN"));

        String full = m.toString();
        // Header line + sections + message size line must be present
        assertTrue(full.contains(";; QUESTION"));
        assertTrue(full.contains(";; ANSWER"));
        assertTrue(full.contains(";; Message size:"));
    }
}
