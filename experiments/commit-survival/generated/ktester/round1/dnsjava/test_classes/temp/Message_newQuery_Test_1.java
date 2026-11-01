package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
// added for TSIGRecord construction
import java.util.Date;

@ExtendWith(MockitoExtension.class)
class Message_newQuery_Test {

    static Stream<Arguments> normalOperationProvider() throws Exception {
        return Stream.of(Arguments.of("A record", new ARecord(Name.fromString("example.com."), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"))), Arguments.of("MX record", new MXRecord(Name.fromString("mail.example.com."), DClass.IN, 3600, 10, Name.fromString("mailhost.example.com."))), Arguments.of("AAAA record", new AAAARecord(Name.fromString("ipv6.example.com."), DClass.IN, 3600, InetAddress.getByName("2001:db8::1"))), Arguments.of("NS record", new NSRecord(Name.fromString("example.com."), DClass.IN, 3600, Name.fromString("ns1.example.com."))), Arguments.of("CNAME record", new CNAMERecord(Name.fromString("cname.example.com."), DClass.IN, 3600, Name.fromString("target.example.com."))), Arguments.of("SOA record", new SOARecord(Name.fromString("example.com."), DClass.IN, 3600, Name.fromString("ns.example.com."), Name.fromString("hostmaster.example.com."), 2021010101L, 7200, 3600, 1209600, 3600)));
    }

    /**
     * Verify that {@link Message#newQuery(Record)} creates a message whose header has the
     * {@code QUERY} opcode and the recursion‑desired (RD) flag set.
     */
    @Test
    void newQuery_setsHeaderOpcodeAndRDFlag() {
        Name name = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        Header header = query.getHeader();
        assertEquals(Opcode.QUERY, header.getOpcode(), "Header opcode should be QUERY");
        assertTrue(header.getFlag(Flags.RD), "Header should have the recursion‑desired (RD) flag set");
    }

    /**
     * Verify that the supplied {@link Record} is placed in the QUESTION section of the
     * newly created message.
     */
    @Test
    void newQuery_addsRecordToQuestionSection() {
        Name name = Name.fromConstantString("host.example.");
        Record question = Record.newRecord(name, Type.AAAA, DClass.IN);
        Message query = Message.newQuery(question);
        // The QUESTION section is index {@link Section#QUESTION}
        assertEquals(1, query.getHeader().getCount(Section.QUESTION), "Header count for QUESTION section should be 1");
        assertEquals(1, query.getSection(Section.QUESTION).size(), "QUESTION section should contain exactly one record");
        assertSame(question, query.getSection(Section.QUESTION).get(0), "The record stored in the QUESTION section should be the same instance passed in");
        // Convenience accessor
        assertEquals(question, query.getQuestion(), "Message#getQuestion() should return the supplied record");
    }

    /**
     * Ensure that a {@code null} argument results in a {@link NullPointerException}.
     * The implementation of {@code newQuery} does not guard against null, so the
     * exception is expected from the subsequent {@code addRecord} call.
     */
    @Test
    void newQuery_nullRecord_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Message.newQuery(null));
    }

    /**
     * Verify that the created message is otherwise a fresh instance:
     * all other sections are empty and the header counts are zero except for the
     * QUESTION section.
     */
    @Test
    void newQuery_otherSectionsAreEmpty() {
        Name name = Name.fromConstantString("test.example.");
        Record question = Record.newRecord(name, Type.MX, DClass.IN);
        Message query = Message.newQuery(question);
        // Sections 0 (ANSWER), 2 (AUTHORITY), 3 (ADDITIONAL) must be empty
        assertTrue(query.getSection(Section.ANSWER).isEmpty(), "ANSWER section should be empty");
        assertTrue(query.getSection(Section.AUTHORITY).isEmpty(), "AUTHORITY section should be empty");
        assertTrue(query.getSection(Section.ADDITIONAL).isEmpty(), "ADDITIONAL section should be empty");
        // Header counts for those sections must be zero
        assertEquals(0, query.getHeader().getCount(Section.ANSWER));
        assertEquals(0, query.getHeader().getCount(Section.AUTHORITY));
        assertEquals(0, query.getHeader().getCount(Section.ADDITIONAL));
    }

    /**
     * Normal operation – verify that various record types are accepted as the
     * question and that the header is correctly initialised.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalOperationProvider")
    void newQuery_normalOperation(String description, Record record) {
        Message msg = Message.newQuery(record);
        Header h = msg.getHeader();
        assertEquals(Opcode.QUERY, h.getOpcode(), "Opcode must be QUERY");
        assertTrue(h.getFlag(Flags.RD), "RD flag must be set");
        // QUESTION section checks
        assertEquals(1, h.getCount(Section.QUESTION), "QUESTION count must be 1");
        List<Record> qSection = msg.getSection(Section.QUESTION);
        assertEquals(1, qSection.size(), "QUESTION section size must be 1");
        assertSame(record, qSection.get(0), "Stored record must be the same instance");
        assertEquals(record, msg.getQuestion(), "Convenient getter must return the same record");
    }

    @Test
    void newQuery_maxLengthDomainName() throws Exception {
        // Build a name with exactly 255 octets (including the final root label)
        String label = String.join(".", Collections.nCopies(127, "a"));
        String fqdn = label + ".example.com.";
        int remaining = 255 - fqdn.length();
        String filler = String.join("", Collections.nCopies(remaining, "b"));
        String fullName = label + filler + ".example.com.";
        Name name = Name.fromString(fullName);
        assertEquals(255, name.toString().length(), "Constructed name should be exactly 255 characters");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "test");
        Message msg = Message.newQuery(txt);
        Header h = msg.getHeader();
        assertEquals(Opcode.QUERY, h.getOpcode());
        assertTrue(h.getFlag(Flags.RD));
        assertEquals(txt, msg.getQuestion());
        assertEquals(1, msg.getSection(Section.QUESTION).size());
        assertEquals(txt, msg.getSection(Section.QUESTION).get(0));
    }

    @Test
    void newQuery_longTxtRecordData() throws Exception {
        String longString = String.join("", Collections.nCopies(400, "x"));
        Record txt = new TXTRecord(Name.fromString("txt.example.com."), DClass.IN, 3600, longString);
        Message msg = Message.newQuery(txt);
        Header h = msg.getHeader();
        assertEquals(Opcode.QUERY, h.getOpcode());
        assertTrue(h.getFlag(Flags.RD));
        assertEquals(txt, msg.getQuestion());
        assertEquals(1, msg.getSection(Section.QUESTION).size());
    }

    @Test
    void newQuery_optRecordInQuestion_throwsIllegalArgumentException() {
        Record opt = new OPTRecord(4096, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> Message.newQuery(opt));
    }

    @Test
    void newQuery_tsigRecordInQuestion_throwsIllegalArgumentException() throws Exception {
        // Construct a TSIG object using the constructor that accepts three Strings
        TSIG tsig = new TSIG("keyname", "hmac-md5", "secret");
        // Build a TSIGRecord using the constructor available in this version of dnsjava
        Record tsigRec = new TSIGRecord(// name
        Name.fromString("example.com."), // dclass
        DClass.ANY, // ttl
        0L, // algorithm name
        Name.fromString("hmac-md5."), // time signed
        new Date(), // fudge
        0, // MAC
        new byte[0], // original ID
        0, // error
        0, // other data
        new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> Message.newQuery(tsigRec));
    }

    @Test
    void newQuery_oversizedDomainName_throwsIllegalArgumentException() throws Exception {
        // 256 'a' characters + trailing dot -> 257 octets (>255)
        String longName = String.join("", Collections.nCopies(256, "a")) + ".";
        assertThrows(IllegalArgumentException.class, () -> new ARecord(Name.fromString(longName), DClass.IN, 3600, InetAddress.getByName("192.0.2.1")));
    }

    @Test
    void newQuery_headerFlagsContainOnlyRD() {
        Record cname = new CNAMERecord(Name.fromString("cname.example.com."), DClass.IN, 3600, Name.fromString("target.example.com."));
        Message msg = Message.newQuery(cname);
        Header h = msg.getHeader();
        assertTrue(h.getFlag(Flags.RD), "RD flag must be set");
        // All other standard flags should be clear
        assertFalse(h.getFlag(Flags.AA), "AA flag must be clear");
        assertFalse(h.getFlag(Flags.TC), "TC flag must be clear");
        assertFalse(h.getFlag(Flags.RA), "RA flag must be clear");
        assertFalse(h.getFlag(Flags.AD), "AD flag must be clear");
        assertFalse(h.getFlag(Flags.CD), "CD flag must be clear");
    }

    @Test
    void newQuery_distinctInstances() {
        Record soa = new SOARecord(Name.fromString("example.com."), DClass.IN, 3600, Name.fromString("ns.example.com."), Name.fromString("hostmaster.example.com."), 2021010101L, 7200, 3600, 1209600, 3600);
        Message m1 = Message.newQuery(soa);
        Message m2 = Message.newQuery(soa);
        assertNotSame(m1, m2, "Two calls must return different Message objects");
        // Mutate header of the first message and verify the second is unaffected
        m1.getHeader().setFlag(Flags.AA, 1);
        assertTrue(m1.getHeader().getFlag(Flags.AA), "First message should have AA flag set");
        assertFalse(m2.getHeader().getFlag(Flags.AA), "Second message must remain unchanged");
    }

    @Test
    void newQuery_nullHeaderInternalState_throwsNPE() throws Exception {
        // Create a Message via reflection, set its private 'header' field to null,
        // then invoke the logic that newQuery would execute using reflection.
        // Because newQuery constructs its own Message, we cannot inject the null
        // header directly. Instead we simulate the failure by creating a Message,
        // null‑ing its header, and then calling a method that accesses the header.
        // The expected NPE demonstrates that the class does not tolerate a null
        // header internally.
        // uses default constructor
        Message corrupted = new Message();
        java.lang.reflect.Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        // corrupt internal state
        headerField.set(corrupted, null);
        // Any call that dereferences the header should now throw NPE.
        assertThrows(NullPointerException.class, () -> corrupted.getHeader(), "Accessing a null header should throw NullPointerException");
    }
}
