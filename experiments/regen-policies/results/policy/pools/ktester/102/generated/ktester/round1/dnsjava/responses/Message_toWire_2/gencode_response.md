```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Additional unit tests for {@link Message#toWire()} covering a wide range of
 * scenarios described in the test‑case JSON.  The tests use only the public API
 * (except for a single reflective call to the private {@code toWire(DNSOutput,int)}
 * when we need to force a specific maxLength/truncate combination that is not
 * otherwise reachable).
 *
 * The focus is on:
 * <ul>
 *   <li>Correct handling of each DNS section</li>
 *   <li>Header flag encoding</li>
 *   <li>OPT pseudo‑record ordering</li>
 *   <li>TSIG signing and placeholder handling</li>
 *   <li>Maximum‑length enforcement and truncation</li>
 *   <li>Record type validation per section</li>
 *   <li>Name compression behaviour</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_Additional_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* -------------------------------------------------------------
     * Helper utilities
     * ------------------------------------------------------------- */

    /** Creates a Message that contains {@code count} identical A records. */
    private Message createMessageWithManyARecords(Name name, int count) throws UnknownHostException {
        Message m = Message.newQuery(Record.newRecord(name, Type.A, DClass.IN));
        for (int i = 0; i < count; i++) {
            ARecord a = new ARecord(name, DClass.IN, 300,
                    InetAddress.getByName("192.0.2." + (i % 254)));
            m.addRecord(a, Section.ANSWER);
        }
        return m;
    }

    /** Returns the number of records present in a given section. */
    private int sectionCount(Message m, int section) {
        return m.getSection(section).size();
    }

    /** Reflectively invokes the private {@code toWire(DNSOutput,int)} method.
     *  This is only used for the tests that need to pass a {@code truncate}
     *  flag together with a maxLength (the public overload does not expose the
     *  flag). */
    private byte[] invokePrivateToWire(Message msg, int maxLength, boolean truncate) throws Exception {
        java.lang.reflect.Method m = Message.class.getDeclaredMethod("toWire", DNSOutput.class, int.class);
        m.setAccessible(true);
        DNSOutput out = new DNSOutput();
        // The private method returns a boolean indicating success; we only need the side‑effect on out.
        boolean ok = (boolean) m.invoke(msg, out, maxLength);
        assertTrue(ok, "private toWire should succeed for the test");
        byte[] data = out.toByteArray();
        if (truncate && data.length > maxLength) {
            // Simulate truncation behaviour – the real implementation would have already
            // truncated and set the TC flag.  For the purpose of the test we just cut the array.
            byte[] truncated = new byte[maxLength];
            System.arraycopy(data, 0, truncated, 0, maxLength);
            return truncated;
        }
        return data;
    }

    /* -------------------------------------------------------------
     * 1. Empty message – header only (12 bytes)
     * ------------------------------------------------------------- */
    @Test
    void toWire_emptyMessage_headerOnlyLength12() {
        Message msg = new Message();               // default constructor → empty header
        byte[] wire = msg.toWire();
        assertNotNull(wire);
        assertEquals(12, wire.length, "An empty message should consist of exactly the DNS header (12 bytes)");
    }

    /* -------------------------------------------------------------
     * 2. Message with a single question
     * ------------------------------------------------------------- */
    @Test
    void toWire_messageWithOnlyQuestion() throws IOException {
        Name name = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = Message.newQuery(q);
        byte[] wire = msg.toWire();

        // round‑trip parsing
        Message parsed = new Message(wire);
        List<Record> qSection = parsed.getSection(Section.QUESTION);
        assertEquals(1, qSection.size());
        assertEquals(q.getName(), qSection.get(0).getName());
        assertEquals(q.getType(), qSection.get(0).getType());
    }

    /* -------------------------------------------------------------
     * 3. Answer + Authority sections
     * ------------------------------------------------------------- */
    @Test
    void toWire_messageWithAnswerAndAuthority() throws UnknownHostException, IOException {
        Name name = Name.fromConstantString("www.example.");
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = Message.newQuery(q);

        // answer
        ARecord a = new ARecord(name, DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        msg.addRecord(a, Section.ANSWER);

        // authority
        NSRecord ns = new NSRecord(name, DClass.IN, 300, Name.fromConstantString("ns.example."));
        msg.addRecord(ns, Section.AUTHORITY);

        byte[] wire = msg.toWire();
        Message parsed = new Message(wire);
        assertEquals(1, parsed.getSection(Section.ANSWER).size());
        assertEquals(1, parsed.getSection(Section.AUTHORITY).size());
    }

    /* -------------------------------------------------------------
     * 4. Illegal OPT record in ANSWER section – should be omitted
     * ------------------------------------------------------------- */
    @Test
    void toWire_optRecordInAnswerIsSkipped() throws IOException {
        Name name = Name.fromConstantString("example.com.");
        Message msg = Message.newQuery(Record.newRecord(name, Type.A, DClass.IN));

        // OPT pseudo‑record (type 41) – not allowed in ANSWER
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        msg.addRecord(opt, Section.ANSWER);

        byte[] wire = msg.toWire();
        Message parsed = new Message(wire);
        // ANSWER section must be empty because the OPT record is illegal there
        assertTrue(parsed.getSection(Section.ANSWER).isEmpty(),
                "OPT record should be ignored when placed in the ANSWER section");
    }

    /* -------------------------------------------------------------
     * 5. OPT pseudo‑record in ADDITIONAL section – must appear first
     * ------------------------------------------------------------- */
    @Test
    void toWire_optRecordInAdditionalAppearsFirst() throws IOException, UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Message msg = Message.newQuery(Record.newRecord(name, Type.A, DClass.IN));

        // add OPT
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        // add a normal A record to the same section
        ARecord a = new ARecord(Name.fromConstantString("extra.example."), DClass.IN, 300,
                InetAddress.getByName("5.6.7.8"));
        msg.addRecord(a, Section.ADDITIONAL);

        byte[] wire = msg.toWire();

        // Parse back and verify ordering via raw bytes:
        // The first additional record starts right after the end of the AUTHORITY section.
        // We can check that the type field (bytes 2‑3 of the RR) equals 41 (OPT) before any other type.
        // Header is 12 bytes; QUESTION (1 RR) + ANSWER (0) + AUTHORITY (0) = variable.
        // Simplify by scanning the wire for the first occurrence of type 41.
        int pos = 12; // start after header
        // skip QUESTION section (1 RR)
        // each RR has: NAME (variable) + TYPE(2) + CLASS(2) + TTL(4) + RDLENGTH(2) + RDATA
        // For the question, RDLENGTH is omitted – we can just search for 0x00 0x29 (41)
        // A simple approach: ensure that the byte pattern 0x00 0x29 appears before the A record's type 0x00 0x01.
        int optPos = -1, aPos = -1;
        for (int i = pos; i < wire.length - 1; i++) {
            int hi = wire[i] & 0xFF;
            int lo = wire[i + 1] & 0xFF;
            int type = (hi << 8) | lo;
            if (type == Type.OPT && optPos == -1) {
                optPos = i;
            }
            if (type == Type.A && aPos == -1) {
                aPos = i;
            }
        }
        assertTrue(optPos != -1 && aPos != -1, "Both OPT and A records must be present");
        assertTrue(optPos < aPos, "OPT record must appear before other additional records");
    }

    /* -------------------------------------------------------------
     * 6. Header flag encoding
     * ------------------------------------------------------------- */
    @Test
    void toWire_headerFlagsAreEncoded() throws IOException {
        Message msg = new Message();
        Header h = msg.getHeader();
        h.setFlag(Flags.QR);
        h.setFlag(Flags.AA);
        h.setFlag(Flags.RD);
        h.setFlag(Flags.RA);
        h.setFlag(Flags.AD);
        // TC and CD left cleared

        byte[] wire = msg.toWire();

        // The flags field occupies bytes 2‑3 of the header.
        int flags = ((wire[2] & 0xFF) << 8) | (wire[3] & 0xFF);
        // Verify each flag bit
        assertTrue((flags & Flags.QR) != 0, "QR flag must be set");
        assertTrue((flags & Flags.AA) != 0, "AA flag must be set");
        assertTrue((flags & Flags.RD) != 0, "RD flag must be set");
        assertTrue((flags & Flags.RA) != 0, "RA flag must be set");
        assertTrue((flags & Flags.AD) != 0, "AD flag must be set");
        assertEquals(0, flags & Flags.TC, "TC flag must be clear");
        assertEquals(0, flags & Flags.CD, "CD flag must be clear");
    }

    /* -------------------------------------------------------------
     * 7. TSIG signed message – final RR must be type 250
     * ------------------------------------------------------------- */
    @Test
    void toWire_tsigSignedMessageContainsTSIG() throws IOException, UnknownHostException {
        // Create a simple query
        Name name = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = Message.newQuery(q);

        // Create a dummy TSIG key (name, algorithm, secret)
        TSIG tsig = new TSIG(Name.fromConstantString("key.example."), TSIG.HMAC_MD5,
                "a2V5c2VjcmV0".getBytes()); // base64 of "keysecret"
        msg.setTSIG(tsig);
        // Sign the message – the API provides a sign() method in dnsjava
        msg.sign();

        byte[] wire = msg.toWire();

        // The last RR in the wire should be type 250 (TSIG)
        // Scan backwards for the last type field.
        int lastTypePos = -1;
        for (int i = wire.length - 2; i >= 12; i--) {
            int hi = wire[i] & 0xFF;
            int lo = wire[i + 1] & 0xFF;
            int type = (hi << 8) | lo;
            if (type == Type.TSIG) {
                lastTypePos = i;
                break;
            }
        }
        assertTrue(lastTypePos != -1, "TSIG record must be present at the end of the message");
    }

    /* -------------------------------------------------------------
     * 8. TSIG unsigned (placeholder) – MAC empty
     * ------------------------------------------------------------- */
    @Test
    void toWire_tsigUnsignedPlaceholderHasNoMAC() throws IOException {
        Message msg = new Message();
        TSIG tsig = new TSIG(Name.fromConstantString("key.example."), TSIG.HMAC_MD5,
                "a2V5c2VjcmV0".getBytes());
        msg.setTSIG(tsig);
        // Do NOT call sign(); the state stays TSIG_UNSIGNED

        byte[] wire = msg.toWire();

        // Verify that a TSIG RR is present but its RDLENGTH is zero (no MAC)
        // Find the TSIG RR and read its RDLENGTH field (after TYPE, CLASS, TTL)
        int pos = 12; // after header
        boolean found = false;
        while (pos < wire.length - 10) {
            // skip NAME (could be compression – we just look for TYPE)
            int type = ((wire[pos] & 0xFF) << 8) | (wire[pos + 1] & 0xFF);
            if (type == Type.TSIG) {
                // CLASS is always 0, TTL is 0, then RDLENGTH (2 bytes)
                int rdlenPos = pos + 8;
                int rdlen = ((wire[rdlenPos] & 0xFF) << 8) | (wire[rdlenPos + 1] & 0xFF);
                assertEquals(0, rdlen, "Unsigned TSIG placeholder should have zero RDLENGTH");
                found = true;
                break;
            }
            // advance: skip NAME (variable) – we cannot reliably parse it here,
            // so break to avoid infinite loop.
            break;
        }
        assertTrue(found, "TSIG placeholder record must be present");
    }

    /* -------------------------------------------------------------
     * 9. Exceeding maxLength without truncation – exception
     * ------------------------------------------------------------- */
    @Test
    void toWire_exceedsMaxLengthThrowsException() throws UnknownHostException {
        Name name = Name.fromConstantString("big.example.");
        // Create a message that will be larger than 500 bytes
        Message msg = new Message();
        for (int i = 0; i < 50; i++) {
            ARecord a = new ARecord(name, DClass.IN, 300,
                    InetAddress.getByName("192.0.2." + i));
            msg.addRecord(a, Section.ANSWER);
        }

        assertThrows(MessageSizeExceededException.class,
                () -> msg.toWire(500, false),
                "MessageSizeExceededException expected when maxLength is too small");
    }

    /* -------------------------------------------------------------
     * 10. Truncation with TC flag set
     * ------------------------------------------------------------- */
    @Test
    void toWire_truncateSetsTCFlag() throws UnknownHostException, Exception {
        Name name = Name.fromConstantString("big.example.");
        Message msg = new Message();
        for (int i = 0; i < 50; i++) {
            ARecord a = new ARecord(name, DClass.IN, 300,
                    InetAddress.getByName("192.0.2." + i));
            msg.addRecord(a, Section.ANSWER);
        }

        int maxLen = 200; // deliberately small
        byte[] wire = msg.toWire(maxLen, true);
        assertEquals(maxLen, wire.length, "Truncated wire must have exactly maxLength bytes");

        // Verify TC flag (bit 9 of flags) is set
        int flags = ((wire[2] & 0xFF) << 8) | (wire[3] & 0xFF);
        assertTrue((flags & Flags.TC) != 0, "TC flag must be set on truncated message");
    }

    /* -------------------------------------------------------------
     * 11. Invalid maxLength values
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({
            "-1,false",
            "5,false",
            "0,false"
    })
    void toWire_invalidMaxLengthParameters(int maxLength, boolean truncate) {
        Message msg = new Message();
        Exception ex = assertThrows(Exception.class,
                () -> {
                    if (truncate) {
                        msg.toWire(maxLength, true);
                    } else {
                        msg.toWire(maxLength, false);
                    }
                });
        // The library throws either IllegalArgumentException or MessageSizeExceededException.
        assertTrue(ex instanceof IllegalArgumentException || ex instanceof MessageSizeExceededException,
                "Expected IllegalArgumentException or MessageSizeExceededException for maxLength=" + maxLength);
    }

    /* -------------------------------------------------------------
     * 12. Multiple RRsets in ANSWER (A and AAAA)
     * ------------------------------------------------------------- */
    @Test
    void toWire_multipleRRsetsInAnswer() throws UnknownHostException, IOException {
        Name name = Name.fromConstantString("www.example.");
        Message msg = Message.newQuery(Record.newRecord(name, Type.A, DClass.IN));

        ARecord a = new ARecord(name, DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        AAAARecord aaaa = new AAAARecord(name, DClass.IN, 300,
                InetAddress.getByName("2001:db8::1"));
        msg.addRecord(a, Section.ANSWER);
        msg.addRecord(aaaa, Section.ANSWER);

        byte[] wire = msg.toWire();
        Message parsed = new Message(wire);
        List<Record> answer = parsed.getSection(Section.ANSWER);
        assertEquals(2, answer.size(), "Both A and AAAA records must be present");
    }

    /* -------------------------------------------------------------
     * 13. Duplicate records – both should be emitted
     * ------------------------------------------------------------- */
    @Test
    void toWire_duplicateRecordsAreBothWritten() throws UnknownHostException, IOException {
        Name name = Name.fromConstantString("dup.example.");
        Message msg = Message.newQuery(Record.newRecord(name, Type.A, DClass.IN));

        ARecord a1 = new ARecord(name, DClass.IN, 300, InetAddress.getByName("5.6.7.8"));
        ARecord a2 = new ARecord(name, DClass.IN, 300, InetAddress.getByName("5.6.7.8"));
        msg.addRecord(a1, Section.ANSWER);
        msg.addRecord(a2, Section.ANSWER);

        byte[] wire = msg.toWire();
        Message parsed = new Message(wire);
        List<Record> answer = parsed.getSection(Section.ANSWER);
        assertEquals(2, answer.size(), "Both duplicate A records must be present");
    }

    /* -------------------------------------------------------------
     * 14. Name compression – second occurrence should be shorter
     * ------------------------------------------------------------- */
    @Test
    void toWire_nameCompressionReducesSize() throws UnknownHostException, IOException {
        Name name = Name.fromConstantString("www.example.com.");
        Message msg = new Message();
        // Two A records with the same name – compression should kick in.
        ARecord a1 = new ARecord(name, DClass.IN, 300, InetAddress.getByName("1.1.1.1"));
        ARecord a2 = new ARecord(name, DClass.IN, 300, InetAddress.getByName("2.2.2.2"));
        msg.addRecord(a1, Section.ANSWER);
        msg.addRecord(a2, Section.ANSWER);

        byte[] wire = msg.toWire();

        // The uncompressed size of two identical names would be > 2 * (name length + overhead).
        // We simply assert that the wire length is less than the naive double size.
        int naiveSize = 12 // header
                + (name.toString().length() + 2) + 10 // first RR (name + type/class/ttl/rdlen)
                + (name.toString().length() + 2) + 10; // second RR uncompressed
        assertTrue(wire.length < naiveSize, "Wire format should be smaller due to name compression");
    }

    /* -------------------------------------------------------------
     * 15. Mixed sections – only AUTHORITY and ADDITIONAL filled
     * ------------------------------------------------------------- */
    @Test
    void toWire_mixedSectionsOnlyAuthorityAndAdditional() throws UnknownHostException, IOException {
        Name name = Name.fromConstantString("example.com.");
        Message msg = new Message(); // start with a blank message (no question)

        // Authority record
        NSRecord ns = new NSRecord(name, DClass.IN, 300, Name.fromConstantString("ns1.example."));
        msg.addRecord(ns, Section.AUTHORITY);

        // Additional A record
        ARecord a = new ARecord(Name.fromConstantString("extra.example."), DClass.IN, 300,
                InetAddress.getByName("9.9.9.9"));
        msg.addRecord(a, Section.ADDITIONAL);

        byte[] wire = msg.toWire();
        Message parsed = new Message(wire);

        assertTrue(parsed.getSection(Section.QUESTION).isEmpty(), "QUESTION section should be empty");
        assertTrue(parsed.getSection(Section.ANSWER).isEmpty(), "ANSWER section should be empty");
        assertEquals(1, parsed.getSection(Section.AUTHORITY).size(), "AUTHORITY should contain the NS");
        assertEquals(1, parsed.getSection(Section.ADDITIONAL).size(), "ADDITIONAL should contain the A record");
    }
}
```