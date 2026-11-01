###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link Message}.
 *
 * The tests are written in the same package (org.xbill.DNS) so that package‑private
 * members (e.g. {@code getGeneratedTSIG()}) are accessible.
 */
public class MessageTest {

    /* Helper to create a simple A record */
    private Record aRecord(String name) throws TextParseException {
        Name n = Name.fromString(name);
        return Record.newRecord(n, Type.A, DClass.IN, 3600);
    }

    /* Helper to create a TSIG record */
    private TSIGRecord tsigRecord() throws TextParseException {
        Name name = Name.fromString("example.com.");
        Name alg = TSIG.HMAC_MD5; // algorithm name constant
        Date now = new Date();
        return new TSIGRecord(name, DClass.IN, 0, alg,
                now, 300, new byte[0], 0, Rcode.NOERROR, new byte[0]);
    }

    @Test
    public void testHeaderAndConstructor() {
        Message m1 = new Message(0x1234);
        assertEquals(0x1234, m1.getHeader().getID());

        Message m2 = new Message(); // random ID, just ensure not null
        assertNotNull(m2.getHeader());

        Message q = Message.newQuery(aRecord("example.com."));
        assertEquals(Opcode.QUERY, q.getHeader().getOpcode());
        assertTrue(q.getHeader().getFlag(Flags.RD));
        assertEquals(aRecord("example.com."), q.getQuestion());
    }

    @Test
    public void testAddFindRemoveRecord() throws TextParseException {
        Message m = new Message();
        Record r1 = aRecord("example.com.");
        Record r2 = aRecord("test.org.");

        // initially empty
        assertFalse(m.findRecord(r1, Section.ANSWER));
        assertFalse(m.findRecord(r1));

        // add to ANSWER
        m.addRecord(r1, Section.ANSWER);
        assertTrue(m.findRecord(r1, Section.ANSWER));
        assertTrue(m.findRecord(r1));
        assertEquals(1, m.getHeader().getCount(Section.ANSWER));

        // add another to AUTHORITY
        m.addRecord(r2, Section.AUTHORITY);
        assertTrue(m.findRecord(r2, Section.AUTHORITY));
        assertTrue(m.findRecord(r2));
        assertEquals(1, m.getHeader().getCount(Section.AUTHORITY));

        // remove from ANSWER
        assertTrue(m.removeRecord(r1, Section.ANSWER));
        assertFalse(m.findRecord(r1));
        assertEquals(0, m.getHeader().getCount(Section.ANSWER));

        // remove non‑existent
        assertFalse(m.removeRecord(r1, Section.ANSWER));
    }

    @Test
    public void testFindRRset() throws TextParseException {
        Message m = new Message();
        Record a1 = aRecord("example.com.");
        Record a2 = aRecord("example.com."); // same name/type/class
        Record mx = Record.newRecord(Name.fromString("example.com."), Type.MX, DClass.IN, 3600);

        m.addRecord(a1, Section.ANSWER);
        m.addRecord(mx, Section.ANSWER);
        m.addRecord(a2, Section.ANSWER);

        assertTrue(m.findRRset(Name.fromString("example.com."), Type.A, Section.ANSWER));
        assertTrue(m.findRRset(Name.fromString("example.com."), Type.MX, Section.ANSWER));
        assertFalse(m.findRRset(Name.fromString("example.com."), Type.SOA, Section.ANSWER));
    }

    @Test
    public void testGetQuestionAndGetSectionArray() throws TextParseException {
        Message m = new Message();
        Record q = aRecord("example.com.");
        m.addRecord(q, Section.QUESTION);

        assertEquals(q, m.getQuestion());

        // Section array for empty section should be empty (not null)
        Record[] empty = m.getSectionArray(Section.ANSWER);
        assertNotNull(empty);
        assertEquals(0, empty.length);
    }

    @Test
    public void testOPTRecordHandling() throws TextParseException {
        Message m = new Message();
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        m.addRecord(opt, Section.ADDITIONAL);

        assertEquals(opt, m.getOPT());

        // getRcode should incorporate extended rcode (0 in this case)
        assertEquals(0, m.getRcode());

        // set an extended rcode via OPT and verify
        OPTRecord opt2 = new OPTRecord(4096, 2, 0, 0); // extended rcode = 2
        m.removeAllRecords(Section.ADDITIONAL);
        m.addRecord(opt2, Section.ADDITIONAL);
        assertEquals(2 << 4, m.getRcode());
    }

    @Test
    public void testTSIGHandling() throws TextParseException, IOException {
        Message m = new Message();
        TSIGRecord tsig = tsigRecord();
        m.addRecord(tsig, Section.ADDITIONAL);

        assertEquals(tsig, m.getTSIG());

        // initially unsigned
        assertFalse(m.isSigned());
        assertFalse(m.isVerified());

        // set TSIG key and generate wire (which signs the message)
        TSIG key = new TSIG(Name.fromString("hmac-md5.sig-alg.reg.int."), Name.fromString("key."), "abcd");
        m.setTSIG(key);
        byte[] wire = m.toWire(); // triggers signing
        assertTrue(m.isSigned());
        assertFalse(m.isVerified()); // we didn't verify, just signed

        // generated TSIG should be present after signing
        assertNotNull(m.getGeneratedTSIG());
        assertEquals(key, m.getGeneratedTSIG().getAlgorithm()); // algorithm name matches key's algorithm
    }

    @Test
    public void testToWireTruncation() throws TextParseException {
        Message m = new Message();
        // Fill ANSWER with many records to exceed a tiny maxLength
        for (int i = 0; i < 50; i++) {
            Record r = Record.newRecord(Name.fromString("host" + i + ".example.com."), Type.A, DClass.IN, 3600);
            m.addRecord(r, Section.ANSWER);
        }

        // maxLength smaller than header + one record => force truncation
        byte[] truncated = m.toWire(30);
        assertTrue(truncated.length <= 30);
        // TC flag should be set
        assertTrue(m.getHeader().getFlag(Flags.TC));
    }

    @Test
    public void testGetSectionRRsetsGrouping() throws TextParseException {
        Message m = new Message();
        Record a1 = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 3600);
        Record a2 = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 3600);
        Record mx = Record.newRecord(Name.fromString("example.com."), Type.MX, DClass.IN, 3600);
        Record aOther = Record.newRecord(Name.fromString("other.com."), Type.A, DClass.IN, 3600);

        m.addRecord(a1, Section.ANSWER);
        m.addRecord(a2, Section.ANSWER);
        m.addRecord(mx, Section.ANSWER);
        m.addRecord(aOther, Section.ANSWER);

        List<RRset> sets = m.getSectionRRsets(Section.ANSWER);
        assertEquals(3, sets.size()); // (example.com.,A), (example.com.,MX), (other.com.,A)

        // Verify each RRset contains the correct records
        boolean foundExampleA = false, foundExampleMX = false, foundOtherA = false;
        for (RRset rs : sets) {
            if (rs.getName().equals(Name.fromString("example.com.")) && rs.getType() == Type.A) {
                assertEquals(2, rs.size());
                foundExampleA = true;
            } else if (rs.getName().equals(Name.fromString("example.com.")) && rs.getType() == Type.MX) {
                assertEquals(1, rs.size());
                foundExampleMX = true;
            } else if (rs.getName().equals(Name.fromString("other.com.")) && rs.getType() == Type.A) {
                assertEquals(1, rs.size());
                foundOtherA = true;
            }
        }
        assertTrue(foundExampleA && foundExampleMX && foundOtherA);
    }

    @Test
    public void testSectionToStringAndToString() throws TextParseException {
        Message m = new Message();
        Record q = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 0);
        m.addRecord(q, Section.QUESTION);
        Record a = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 3600);
        m.addRecord(a, Section.ANSWER);

        String questionStr = m.sectionToString(Section.QUESTION);
        assertNotNull(questionStr);
        assertTrue(questionStr.contains("example.com."));
        assertTrue(questionStr.contains("type = A"));

        String full = m.toString();
        assertTrue(full.contains(";; QUESTION"));
        assertTrue(full.contains(";; ANSWER"));
        assertTrue(full.contains("example.com."));
    }

    @Test
    public void testCloneDeepCopy() throws TextParseException, CloneNotSupportedException {
        Message original = new Message();
        Record a = aRecord("example.com.");
        original.addRecord(a, Section.ANSWER);
        original.setResolver(new Resolver() {
            public void setPort(int p) {}
            public void setTCP(boolean f) {}
            public void setIgnoreTruncation(boolean f) {}
            public void setTSIGKey(TSIG key) {}
        });

        Message copy = original.clone();

        // Verify header cloned (different object)
        assertNotSame(original.getHeader(), copy.getHeader());
        assertEquals(original.getHeader().getID(), copy.getHeader().getID());

        // Verify sections are deep copied
        assertNotSame(original.getSection(Section.ANSWER), copy.getSection(Section.ANSWER));
        assertEquals(1, copy.getSection(Section.ANSWER).size());
        assertEquals(a, copy.getSection(Section.ANSWER).get(0));

        // Verify resolver is copied correctly (optional)
        assertTrue(copy.getResolver().isPresent());
        assertSame(original.getResolver().get(), copy.getResolver().get());
    }

    @Test
    public void testResolverOptional() {
        Message m = new Message();
        assertFalse(m.getResolver().isPresent());

        Resolver dummy = new Resolver() {
            public void setPort(int p) {}
            public void setTCP(boolean f) {}
            public void setIgnoreTruncation(boolean f) {}
            public void setTSIGKey(TSIG key) {}
        };
        m.setResolver(dummy);
        assertTrue(m.getResolver().isPresent());
        assertSame(dummy, m.getResolver().get());
    }

    @Test
    public void testRemoveAllRecordsAndCounts() throws TextParseException {
        Message m = new Message();
        Record a = aRecord("example.com.");
        m.addRecord(a, Section.AUTHORITY);
        assertEquals(1, m.getHeader().getCount(Section.AUTHORITY));

        m.removeAllRecords(Section.AUTHORITY);
        assertEquals(0, m.getHeader().getCount(Section.AUTHORITY));
        assertTrue(m.getSection(Section.AUTHORITY).isEmpty());
    }

    @Test
    public void testNumBytesAfterWire() throws TextParseException {
        Message m = new Message();
        m.addRecord(aRecord("example.com."), Section.ANSWER);
        byte[] wire = m.toWire();
        assertTrue(m.numBytes() > 0);
        assertEquals(wire.length, m.numBytes());
    }
}
###Test END##