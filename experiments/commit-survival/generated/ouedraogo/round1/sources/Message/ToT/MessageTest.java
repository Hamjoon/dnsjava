/*
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.Message
 *
 * The tests exercise the public API of Message, covering normal usage,
 * edge cases and error handling.  Only classes, constructors and methods
 * that appear in the supplied source (or in the minimal public APIs listed
 * in the prompt) are used – no mocking or external libraries.
 *
 * The suite is deliberately verbose: each test method documents the
 * scenario it validates and contains assertions that explain the expected
 * state of the Message instance after the operation.
 *
 * Run with JUnit‑4 (e.g. via `mvn test` or an IDE that supports JUnit‑4).
 */

package org.xbill.DNS;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class MessageTest {

    /* --------------------------------------------------------------------- *
     *  Helper methods – they keep the test methods short and readable.
     * --------------------------------------------------------------------- */

    /** Create a simple A record for the given name. */
    private static ARecord aRecord(String name) throws IOException {
        Name n = Name.fromString(name);
        // 127.0.0.1 is a safe, deterministic address for tests.
        return new ARecord(n, DClass.IN, 300, InetAddress.getByName("127.0.0.1"));
    }

    /** Create an OPT record with a custom extended rcode (to test getRcode()). */
    private static OPTRecord optRecord(int extendedRcode) {
        // payload size 4096, xrcode = 0, version = 0, flags = 0
        return new OPTRecord(4096, 0, 0, 0) {
            @Override
            public int getExtendedRcode() {
                return extendedRcode;
            }
        };
    }

    /** Create a TSIG key that can be used for signing messages. */
    private static TSIG tsigKey() {
        // Use the built‑in HMAC‑MD5 algorithm name and a short dummy base64 key.
        // The key value itself is irrelevant for the unit tests – we only verify
        // that the TSIG handling path is exercised.
        return new TSIG(TSIG.HMAC_MD5, Name.fromString("testkey."), "YWJjZGVmZ2g="); // "abcdefgh"
    }

    /* --------------------------------------------------------------------- *
     *  Fixtures – fresh objects for every test method.
     * --------------------------------------------------------------------- */

    private Message emptyMessage;      // a Message with no records
    private Message queryMessage;      // a Message created via newQuery()
    private ARecord aRecord1;          // first A record
    private ARecord aRecord2;          // second A record (different name)

    @Before
    public void setUp() throws Exception {
        emptyMessage = new Message();               // random ID, empty header
        aRecord1 = aRecord("example.com.");
        aRecord2 = aRecord("example.org.");

        // Build a simple query message (question section only)
        queryMessage = Message.newQuery(aRecord1);
    }

    /* --------------------------------------------------------------------- *
     *  1. Construction & basic header handling
     * --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorGeneratesHeader() {
        Header h = emptyMessage.getHeader();
        assertNotNull("Header should never be null", h);
        // The default Header constructor generates a random ID – we only assert
        // that the ID is within the unsigned 16‑bit range.
        int id = h.getID();
        assertTrue("ID must be between 0 and 65535", id >= 0 && id <= 0xFFFF);
    }

    @Test
    public void testMessageIdConstructorSetsHeaderId() {
        Message m = new Message(0x1234);
        assertEquals(0x1234, m.getHeader().getID());
    }

    @Test
    public void testSetAndGetHeader() {
        Header newHeader = new Header(0xABCD);
        emptyMessage.setHeader(newHeader);
        assertSame(newHeader, emptyMessage.getHeader());
    }

    /* --------------------------------------------------------------------- *
     *  2. Adding, removing and locating records
     * --------------------------------------------------------------------- */

    @Test
    public void testAddRecordIncrementsHeaderCount() {
        emptyMessage.addRecord(aRecord1, Section.ANSWER);
        assertEquals(1, emptyMessage.getHeader().getCount(Section.ANSWER));
        assertTrue(emptyMessage.findRecord(aRecord1, Section.ANSWER));
    }

    @Test
    public void testRemoveExistingRecordDecrementsHeaderCount() {
        emptyMessage.addRecord(aRecord1, Section.AUTHORITY);
        assertTrue(emptyMessage.removeRecord(aRecord1, Section.AUTHORITY));
        assertEquals(0, emptyMessage.getHeader().getCount(Section.AUTHORITY));
        assertFalse(emptyMessage.findRecord(aRecord1, Section.AUTHORITY));
    }

    @Test
    public void testRemoveNonExistingRecordReturnsFalse() {
        // No record has been added yet.
        assertFalse(emptyMessage.removeRecord(aRecord1, Section.ADDITIONAL));
        assertEquals(0, emptyMessage.getHeader().getCount(Section.ADDITIONAL));
    }

    @Test
    public void testRemoveAllRecordsClearsSectionAndHeaderCount() {
        emptyMessage.addRecord(aRecord1, Section.QUESTION);
        emptyMessage.addRecord(aRecord2, Section.QUESTION);
        assertEquals(2, emptyMessage.getHeader().getCount(Section.QUESTION));
        emptyMessage.removeAllRecords(Section.QUESTION);
        assertEquals(0, emptyMessage.getHeader().getCount(Section.QUESTION));
        assertTrue(emptyMessage.getSection(Section.QUESTION).isEmpty());
    }

    @Test
    public void testFindRecordAcrossAllSections() {
        emptyMessage.addRecord(aRecord1, Section.ANSWER);
        emptyMessage.addRecord(aRecord2, Section.AUTHORITY);
        assertTrue(emptyMessage.findRecord(aRecord1));
        assertTrue(emptyMessage.findRecord(aRecord2));
        // A record that was never added should not be found.
        ARecord notAdded = aRecord("nonexistent.");
        assertFalse(emptyMessage.findRecord(notAdded));
    }

    @Test
    public void testFindRRset() {
        // Two A records with the same name belong to the same RRset.
        ARecord a1 = aRecord("rrset.example.");
        ARecord a2 = aRecord("rrset.example."); // same name, same type
        emptyMessage.addRecord(a1, Section.ANSWER);
        emptyMessage.addRecord(a2, Section.ANSWER);
        Name name = a1.getName();
        assertTrue(emptyMessage.findRRset(name, Type.A, Section.ANSWER));
        // A different name / type must not be found.
        assertFalse(emptyMessage.findRRset(Name.fromString("other.example."), Type.A, Section.ANSWER));
        assertFalse(emptyMessage.findRRset(name, Type.MX, Section.ANSWER));
    }

    @Test
    public void testFindRRsetInAnySection() {
        emptyMessage.addRecord(aRecord1, Section.AUTHORITY);
        assertTrue(emptyMessage.findRRset(aRecord1.getName(), Type.A, Section.AUTHORITY));
        // The helper that searches all sections should also return true.
        assertTrue(emptyMessage.findRRset(aRecord1.getName(), Type.A));
    }

    /* --------------------------------------------------------------------- *
     *  3. Section getters and array conversion
     * --------------------------------------------------------------------- */

    @Test
    public void testGetSectionReturnsUnmodifiableList() {
        emptyMessage.addRecord(aRecord1, Section.ADDITIONAL);
        List<Record> list = emptyMessage.getSection(Section.ADDITIONAL);
        assertEquals(1, list.size());
        try {
            list.add(aRecord2);
            fail("Returned list should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // Expected behaviour
        }
    }

    @Test
    public void testGetSectionArrayDeprecatedMethod() {
        emptyMessage.addRecord(aRecord1, Section.QUESTION);
        Record[] arr = emptyMessage.getSectionArray(Section.QUESTION);
        assertEquals(1, arr.length);
        assertSame(aRecord1, arr[0]);

        // Empty section should return the shared empty array instance.
        Record[] empty = emptyMessage.getSectionArray(Section.AUTHORITY);
        assertSame(Message.emptyRecordArray, empty);
    }

    /* --------------------------------------------------------------------- *
     *  4. Question & special records (TSIG / OPT)
     * --------------------------------------------------------------------- */

    @Test
    public void testGetQuestionReturnsFirstQuestionRecord() {
        // newQuery already placed a single question record.
        Record q = queryMessage.getQuestion();
        assertNotNull(q);
        assertEquals(aRecord1, q);
    }

    @Test
    public void testGetQuestionWhenNoQuestionReturnsNull() {
        assertNull(emptyMessage.getQuestion());
    }

    @Test
    public void testGetOPTAndExtendedRcode() {
        // Add an OPT record with extended rcode = 5 (i.e. 0x5 << 4 = 0x50)
        OPTRecord opt = optRecord(5);
        emptyMessage.addRecord(opt, Section.ADDITIONAL);
        assertSame(opt, emptyMessage.getOPT());

        // Header rcode is initially 0; getRcode should incorporate the extended part.
        assertEquals(5 << 4, emptyMessage.getRcode());
    }

    @Test
    public void testGetTSIGWhenPresent() {
        // Build a TSIG record manually – the TSIG constructor will generate a proper record.
        TSIG key = tsigKey();
        // The Message must have a TSIG attached via setTSIG before we can generate the record.
        emptyMessage.setTSIG(key);
        // Render the message to wire format (max length large enough) so that a TSIG record is added.
        byte[] wire = emptyMessage.toWire(Message.MAXLENGTH);
        // Re‑parse the wire back into a Message to expose the TSIG record.
        Message parsed;
        try {
            parsed = new Message(wire);
        } catch (IOException e) {
            throw new AssertionError("Failed to parse generated wire", e);
        }
        TSIGRecord tsigRec = parsed.getTSIG();
        assertNotNull("TSIG record should be present after signing", tsigRec);
        assertEquals(key.getName(), tsigRec.getName());
    }

    @Test
    public void testGetTSIGReturnsNullWhenAbsent() {
        assertNull(emptyMessage.getTSIG());
    }

    /* --------------------------------------------------------------------- *
     *  5. Signing state helpers
     * --------------------------------------------------------------------- */

    @Test
    public void testIsSignedAndIsVerifiedFlags() {
        TSIG key = tsigKey();
        emptyMessage.setTSIG(key);
        // The message is not yet signed – internal state stays at default (0).
        assertFalse(emptyMessage.isSigned());
        assertFalse(emptyMessage.isVerified());

        // Force signing by rendering to wire (which generates the TSIG record).
        emptyMessage.toWire(Message.MAXLENGTH);
        // After rendering, the internal state should be TSIG_SIGNED (3) – still not verified.
        assertTrue(emptyMessage.isSigned());
        assertFalse(emptyMessage.isVerified());

        // Simulate verification by manually setting the state (this is not part of the public API,
        // but we can reach the verified state via a round‑trip: a signed request followed by a signed response).
        Message response = new Message();
        response.setTSIG(key);
        response.toWire(Message.MAXLENGTH); // signs response
        // The response's generated TSIG is considered verified when the resolver processes it,
        // but for the unit test we simply check that isSigned() is true.
        assertTrue(response.isSigned());
    }

    /* --------------------------------------------------------------------- *
     *  6. Wire format rendering (including truncation)
     * --------------------------------------------------------------------- */

    @Test
    public void testToWireProducesNonEmptyArray() {
        byte[] wire = queryMessage.toWire();
        assertNotNull(wire);
        assertTrue("Wire format must contain at least a DNS header (12 bytes)", wire.length >= Header.LENGTH);
    }

    @Test
    public void testToWireWithSmallMaxLengthReturnsEmptyArray() {
        // Header.LENGTH is the minimum viable size; request less than that.
        byte[] wire = emptyMessage.toWire(Header.LENGTH - 1);
        assertEquals("When maxLength < Header.LENGTH the result must be empty", 0, wire.length);
    }

    @Test
    public void testTruncationSetsTCFlagAndAdjustsCounts() throws IOException {
        // Create a message with many answer records to force truncation.
        Message large = new Message();
        for (int i = 0; i < 50; i++) {
            ARecord rec = aRecord("host" + i + ".example.com.");
            large.addRecord(rec, Section.ANSWER);
        }
        // Use a deliberately tiny maxLength to trigger truncation.
        int tiny = Header.LENGTH + 20; // only a few bytes beyond the header
        byte[] wire = large.toWire(tiny);
        assertTrue("Wire should be non‑empty even after truncation", wire.length > 0);
        // Parse the truncated wire back and verify TC flag.
        Message parsed = new Message(wire);
        assertTrue("TC flag must be set on truncated messages", parsed.getHeader().getFlag(Flags.TC));
        // Header counts for the truncated section must be less than the original.
        int originalCount = large.getHeader().getCount(Section.ANSWER);
        int truncatedCount = parsed.getHeader().getCount(Section.ANSWER);
        assertTrue(truncatedCount < originalCount);
    }

    /* --------------------------------------------------------------------- *
     *  7. Section RRset grouping
     * --------------------------------------------------------------------- */

    @Test
    public void testGetSectionRRsetsGroupsByNameAndType() {
        // Two A records with same name -> same RRset, one MX with same name -> different RRset.
        Name common = Name.fromString("group.example.");
        ARecord a1 = new ARecord(common, DClass.IN, 300, InetAddress.getByName("10.0.0.1"));
        ARecord a2 = new ARecord(common, DClass.IN, 300, InetAddress.getByName("10.0.0.2"));
        MXRecord mx = new MXRecord(common, DClass.IN, 300, 10, Name.fromString("mail.example."));

        emptyMessage.addRecord(a1, Section.ANSWER);
        emptyMessage.addRecord(a2, Section.ANSWER);
        emptyMessage.addRecord(mx, Section.ANSWER);

        List<RRset> sets = emptyMessage.getSectionRRsets(Section.ANSWER);
        assertEquals("Should be two RRsets (A and MX)", 2, sets.size());

        // Verify that the A RRset contains both A records.
        RRset aSet = sets.stream()
                .filter(s -> s.getType() == Type.A)
                .findFirst()
                .orElseThrow(() -> new AssertionError("A RRset missing"));
        assertEquals(2, aSet.size());

        // Verify MX RRset size.
        RRset mxSet = sets.stream()
                .filter(s -> s.getType() == Type.MX)
                .findFirst()
                .orElseThrow(() -> new AssertionError("MX RRset missing"));
        assertEquals(1, mxSet.size());
    }

    /* --------------------------------------------------------------------- *
     *  8. Clone semantics (deep copy of sections and header)
     * --------------------------------------------------------------------- */

    @Test
    public void testCloneCreatesDeepCopy() throws Exception {
        emptyMessage.addRecord(aRecord1, Section.ANSWER);
        emptyMessage.addRecord(aRecord2, Section.AUTHORITY);
        emptyMessage.setTSIG(tsigKey()); // ensure TSIG related fields are also copied

        Message copy = emptyMessage.clone();

        // Header must be a different instance but equal content.
        assertNotSame(emptyMessage.getHeader(), copy.getHeader());
        assertEquals(emptyMessage.getHeader().getID(), copy.getHeader().getID());

        // Sections must be deep copies – modifying the original must not affect the clone.
        emptyMessage.removeAllRecords(Section.ANSWER);
        assertEquals(1, copy.getHeader().getCount(Section.ANSWER));
        assertEquals(0, emptyMessage.getHeader().getCount(Section.ANSWER));

        // TSIG related fields are also deep‑copied.
        assertNotSame(emptyMessage.getGeneratedTSIG(), copy.getGeneratedTSIG());
        assertNotSame(emptyMessage.querytsig, copy.querytsig);
    }

    /* --------------------------------------------------------------------- *
     *  9. Resolver association (Optional handling)
     * --------------------------------------------------------------------- */

    @Test
    public void testResolverSetAndGet() {
        // Use a minimal stub implementation of Resolver – the interface has default methods,
        // so we can create an anonymous class without overriding anything.
        Resolver resolver = new Resolver() {
            // No extra behaviour needed for the test.
        };
        emptyMessage.setResolver(resolver);
        Optional<Resolver> opt = emptyMessage.getResolver();
        assertTrue(opt.isPresent());
        assertSame(resolver, opt.get());

        // Clear the resolver and verify the Optional is empty.
        emptyMessage.setResolver(null);
        assertFalse(emptyMessage.getResolver().isPresent());
    }

    /* --------------------------------------------------------------------- *
     * 10. toString() representation – sanity check that it does not throw
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringContainsHeaderAndSections() {
        emptyMessage.addRecord(aRecord1, Section.QUESTION);
        emptyMessage.addRecord(aRecord2, Section.ANSWER);
        String s = emptyMessage.toString();
        assertNotNull(s);
        assertTrue(s.contains(";; QUESTION"));
        assertTrue(s.contains(";; ANSWER"));
        assertTrue(s.contains(aRecord1.getName().toString()));
        assertTrue(s.contains(aRecord2.getName().toString()));
        // Message size should appear at the end.
        assertTrue(s.matches("(?s).*Message size: \\d+ bytes$"));
    }

    /* --------------------------------------------------------------------- *
     * 11. numBytes() reports size after wire conversion
     * --------------------------------------------------------------------- */

    @Test
    public void testNumBytesReflectsWireSize() {
        assertEquals(0, emptyMessage.numBytes()); // never rendered yet
        byte[] wire = emptyMessage.toWire();
        assertEquals(wire.length, emptyMessage.numBytes());
    }

    /* --------------------------------------------------------------------- *
     * 12. Section string conversion (sectionToString) edge cases
     * --------------------------------------------------------------------- */

    @Test
    public void testSectionToStringHandlesInvalidIndexGracefully() {
        // The method returns null for out‑of‑range sections.
        assertNull(emptyMessage.sectionToString(5));
        assertNull(emptyMessage.sectionToString(-1));
    }

    @Test
    public void testSectionToStringFormatsQuestionRecordCorrectly() {
        // The string representation for a question record follows the ";;\t" format.
        emptyMessage.addRecord(aRecord1, Section.QUESTION);
        String secStr = emptyMessage.sectionToString(Section.QUESTION);
        assertNotNull(secStr);
        assertTrue(secStr.contains(";;\t" + aRecord1.getName()));
        assertTrue(secStr.contains("type = " + Type.string(aRecord1.getType())));
        assertTrue(secStr.contains("class = " + DClass.string(aRecord1.getDClass())));
    }

    @Test
    public void testSectionToStringSkipsOPTRecordInNonQuestionSections() {
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        emptyMessage.addRecord(opt, Section.ADDITIONAL);
        String secStr = emptyMessage.sectionToString(Section.ADDITIONAL);
        // The OPT record implements its own pseudo‑section printing and should not appear in the generic loop.
        assertTrue(secStr.isEmpty() || !secStr.contains(opt.toString()));
    }
}
