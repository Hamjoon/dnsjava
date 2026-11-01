```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* Additional imports required for the fixes */
import org.xbill.DNS.DNSOutput;
import org.xbill.DNS.DNSInput;
import org.xbill.DNS.Compression;

/**
 * Test suite for {@link Message#toWire()} (variant 2). The tests exercise a
 * variety of message constructions, header flag settings, TSIG handling,
 * length limits and error conditions.
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_2_Test {

    private static final Name QUESTION_NAME = Name.fromConstantString("www.example.");

    private static final Name ADDITIONAL_NAME = Name.fromConstantString("test.example.");

    // 256‑character name for the “overly long domain” test
    private static final Name LONG_NAME;

    static {
        // Build a label of 256 characters (which exceeds the DNS limit of 255)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append('a');
        }
        // Append a trailing dot to make it a fully‑qualified name
        LONG_NAME = Name.fromString(sb.toString() + ".");
    }

    /**
     * Helper to read a private int field via reflection.
     */
    private static int getPrivateInt(Message msg, String fieldName) {
        try {
            Field f = Message.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.getInt(msg);
        } catch (Exception e) {
            throw new AssertionError("Unable to read private field " + fieldName, e);
        }
    }

    /**
     * Verify that an empty message can be serialized and deserialized without
     * loss of information.
     */
    @Test
    void toWire_emptyMessage() throws IOException {
        // default header, no sections
        Message original = new Message();
        byte[] wire = original.toWire();
        // The wire format must contain at least the 12‑byte DNS header.
        assertNotNull(wire);
        // just >0
        assertTrue(wire.length >= Message.MAXLENGTH - Message.MAXLENGTH + 12);
        Message parsed = new Message(wire);
        // Header IDs should be identical.
        assertEquals(original.getHeader().getID(), parsed.getHeader().getID());
        // All sections must be empty.
        for (int i = 0; i < 4; i++) {
            assertTrue(original.getSection(i).isEmpty(), "original section " + i + " should be empty");
            assertTrue(parsed.getSection(i).isEmpty(), "parsed section " + i + " should be empty");
        }
    }

    /**
     * Verify that a message containing a question and several additional
     * records round‑trips correctly through {@code toWire()}.
     */
    @Test
    void toWire_messageWithRecords() throws IOException {
        // Build a message with a question and a handful of additional records.
        Message original = new Message(0x1234);
        Record question = Record.newRecord(QUESTION_NAME, Type.A, DClass.IN);
        original.addRecord(question, Section.QUESTION);
        // Add a series of TXT records to the ADDITIONAL section.
        for (int i = 1; i <= 5; i++) {
            String txt = "sample-" + i;
            Record txtRec = new TXTRecord(ADDITIONAL_NAME, DClass.IN, 300, txt);
            original.addRecord(txtRec, Section.ADDITIONAL);
        }
        // Add an OPT record (EDNS0) as the last additional record.
        original.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        // Serialize to wire format.
        byte[] wire = original.toWire();
        assertNotNull(wire);
        assertTrue(wire.length > 0, "Wire data should not be empty");
        // Deserialize and compare.
        Message parsed = new Message(wire);
        // Header ID must survive the round‑trip.
        assertEquals(original.getHeader().getID(), parsed.getHeader().getID());
        // Question section must contain the same record.
        List<Record> parsedQuestion = parsed.getSection(Section.QUESTION);
        assertEquals(1, parsedQuestion.size());
        assertEquals(question, parsedQuestion.get(0));
        // Additional section must contain exactly the records we added (order preserved).
        List<Record> parsedAdditional = parsed.getSection(Section.ADDITIONAL);
        // 5 TXT records + 1 OPT record = 6
        assertEquals(6, parsedAdditional.size());
        // Verify the first additional record is a TXTRecord and matches the original.
        assertTrue(parsedAdditional.get(0) instanceof TXTRecord);
        assertEquals("sample-1", ((TXTRecord) parsedAdditional.get(0)).getStrings().get(0));
        // Verify the last additional record is the OPTRecord.
        assertTrue(parsedAdditional.get(5) instanceof OPTRecord);
    }

    /**
     * Ensure that {@code toWire()} produces a byte array whose length matches
     * the value reported by {@link Message#numBytes()} after serialization.
     */
    @Test
    void toWire_byteArrayLengthMatchesNumBytes() throws IOException {
        Message msg = new Message(0xABCD);
        // Add a few records to make the message non‑trivial.
        msg.addRecord(Record.newRecord(QUESTION_NAME, Type.AAAA, DClass.IN), Section.QUESTION);
        msg.addRecord(new TXTRecord(ADDITIONAL_NAME, DClass.IN, 60, "hello"), Section.ADDITIONAL);
        byte[] wire = msg.toWire();
        int reported = msg.numBytes();
        assertEquals(reported, wire.length, "Message.numBytes() should report the exact length of the wire array");
    }

    /**
     * Empty Message – verify that the internal {@code size} field equals the
     * length of the generated wire array (12 bytes for a header‑only message).
     */
    @Test
    @DisplayName("Empty Message – size field equals 12")
    void emptyMessage_sizeEqualsHeaderLength() throws Exception {
        // no sections, no TSIG
        Message msg = new Message();
        byte[] wire = msg.toWire();
        assertEquals(12, wire.length, "Header‑only message must be exactly 12 bytes");
        // Access private field 'size' via reflection.
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        int sizeValue = (int) sizeField.get(msg);
        assertEquals(wire.length, sizeValue, "Message.size should be set to the actual wire length");
    }

    /**
     * Message with only a question section (created via {@code Message.newQuery}).
     */
    @Test
    @DisplayName("Message with Question Section Only")
    void messageWithOnlyQuestion() throws IOException {
        Record q = Record.newRecord(QUESTION_NAME, Type.A, DClass.IN);
        // creates a query with a single question
        Message msg = Message.newQuery(q);
        byte[] wire = msg.toWire();
        assertTrue(wire.length > 12, "Wire must be larger than the header");
        // Verify size field matches array length
        Field sizeField;
        try {
            sizeField = Message.class.getDeclaredField("size");
            sizeField.setAccessible(true);
            int size = (int) sizeField.get(msg);
            assertEquals(wire.length, size, "Message.size must equal wire length");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection on size field failed", e);
        }
        // Re‑parse and ensure the question is present.
        Message parsed = new Message(wire);
        List<Record> qSection = parsed.getSection(Section.QUESTION);
        assertEquals(1, qSection.size());
        assertEquals(q, qSection.get(0));
    }

    /**
     * Message with a single answer record.
     */
    @Test
    @DisplayName("Message with Answer Section")
    void messageWithAnswerSection() throws IOException {
        Record q = Record.newRecord(QUESTION_NAME, Type.A, DClass.IN);
        Message msg = Message.newQuery(q);
        Record answer = new ARecord(QUESTION_NAME, DClass.IN, 300,
                InetAddress.getByName("1.2.3.4"));
        msg.addRecord(answer, Section.ANSWER);
        byte[] wire = msg.toWire();
        assertTrue(wire.length > 12, "Wire must contain header + question + answer");
        // Verify size field.
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        assertEquals(wire.length, sizeField.getInt(msg));
        // Parse back and verify answer.
        Message parsed = new Message(wire);
        List<Record> ans = parsed.getSection(Section.ANSWER);
        assertEquals(1, ans.size());
        assertEquals(answer, ans.get(0));
    }

    /**
     * Message with records in Answer, Authority and Additional sections.
     */
    @Test
    @DisplayName("Message with Multiple Sections (Answer, Authority, Additional)")
    void messageWithMultipleSections() throws IOException {
        Record q = Record.newRecord(Name.fromConstantString("example.com."), Type.MX, DClass.IN);
        Message msg = Message.newQuery(q);
        // Answer
        Record a = new ARecord(Name.fromConstantString("example.com."), DClass.IN, 300,
                InetAddress.getByName("5.6.7.8"));
        msg.addRecord(a, Section.ANSWER);
        // Authority
        Record ns = new NSRecord(Name.fromConstantString("example.com."), DClass.IN, 300,
                Name.fromConstantString("ns1.example.com."));
        msg.addRecord(ns, Section.AUTHORITY);
        // Additional
        Record txt = new TXTRecord(Name.fromConstantString("example.com."), DClass.IN, 300, "info");
        msg.addRecord(txt, Section.ADDITIONAL);
        byte[] wire = msg.toWire();
        assertTrue(wire.length > 12, "Wire must contain all three populated sections");
        // Verify size field.
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        assertEquals(wire.length, sizeField.getInt(msg));
        // Parse back and verify each section count.
        Message parsed = new Message(wire);
        assertEquals(1, parsed.getSection(Section.ANSWER).size());
        assertEquals(1, parsed.getSection(Section.AUTHORITY).size());
        assertEquals(1, parsed.getSection(Section.ADDITIONAL).size());
    }

    /**
     * Message with an unsigned TSIG record (added via {@code setTSIG(TSIG)}).
     */
    @Test
    @DisplayName("Message with unsigned TSIG")
    void messageWithUnsignedTSIG() throws IOException {
        // Create a simple query.
        Record q = Record.newRecord(QUESTION_NAME, Type.A, DClass.IN);
        Message msg = Message.newQuery(q);
        // Construct a TSIG key (algorithm HMAC-MD5, secret "test").
        TSIG tsig = new TSIG(Name.fromConstantString("key.example."), TSIG.HMAC_MD5, "test".getBytes());
        // Add the TSIG key without signing.
        msg.setTSIG(tsig);
        byte[] wire = msg.toWire();
        assertTrue(wire.length > 12, "Wire must contain the TSIG pseudo‑RR");
        // Verify that the additional section now contains exactly one TSIGRecord.
        List<Record> additional = msg.getSection(Section.ADDITIONAL);
        assertEquals(1, additional.size());
        assertTrue(additional.get(0) instanceof TSIGRecord);
        // In dnsjava an unsigned TSIG has error code Rcode.NOERROR.
        assertEquals(Rcode.NOERROR, ((TSIGRecord) additional.get(0)).getError(),
                "Unsigned TSIG should have NOERROR error code");
        // Size field consistency.
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        assertEquals(wire.length, sizeField.getInt(msg));
    }

    /**
     * Message with a signed TSIG record (using {@code setTSIG(key, error, querytsig)}).
     */
    @Test
    @DisplayName("Message with signed TSIG")
    void messageWithSignedTSIG() throws IOException {
        Record q = Record.newRecord(QUESTION_NAME, Type.A, DClass.IN);
        Message msg = Message.newQuery(q);
        TSIG tsig = new TSIG(Name.fromConstantString("key.example."), TSIG.HMAC_MD5, "secret".getBytes());
        // Sign the message (error code 0, no prior query TSIG).
        msg.setTSIG(tsig, Rcode.NOERROR, null);
        byte[] wire = msg.toWire();
        assertTrue(wire.length > 12, "Wire must contain the signed TSIG");
        // Verify TSIG state.
        assertEquals(Message.TSIG_SIGNED, getPrivateInt(msg, "tsigState"));
        // Verify TSIG appears as the last additional record.
        List<Record> additional = msg.getSection(Section.ADDITIONAL);
        assertFalse(additional.isEmpty());
        Record last = additional.get(additional.size() - 1);
        assertTrue(last instanceof TSIGRecord);
    }

    /**
     * Message that exceeds the maximum allowed DNS wire length (65535 bytes).
     * Expect an {@link IOException} (or {@link IllegalArgumentException}) to be thrown.
     */
    @Test
    @DisplayName("Message Exceeding Maximum Length")
    void messageExceedingMaximumLength() {
        Message msg = new Message();
        // Add a huge number of TXT records to blow past 65535 bytes.
        for (int i = 0; i < 2000; i++) {
            String txt = "payload-" + i + "-".repeat(10);
            Record rec = new TXTRecord(ADDITIONAL_NAME, DClass.IN, 300, txt);
            msg.addRecord(rec, Section.ADDITIONAL);
        }
        // The library is expected to throw an exception when the limit is crossed.
        assertThrows(IOException.class, msg::toWire, "Serializing a >65535‑byte message should throw IOException");
    }

    /**
     * Message containing an OPT (EDNS0) record.
     */
    @Test
    @DisplayName("Message with OPT Record")
    void messageWithOPTRecord() throws IOException {
        Record q = Record.newRecord(QUESTION_NAME, Type.A, DClass.IN);
        Message msg = Message.newQuery(q);
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        byte[] wire = msg.toWire();
        assertTrue(wire.length > 12, "Wire must contain the OPT pseudo‑RR");
        List<Record> additional = msg.getSection(Section.ADDITIONAL);
        assertEquals(1, additional.size());
        assertTrue(additional.get(0) instanceof OPTRecord);
    }

    /**
     * Message with the Truncated (TC) flag set in the header.
     */
    @Test
    @DisplayName("Message with Truncated Flag Set")
    void messageWithTruncatedFlag() throws IOException {
        Message msg = new Message();
        msg.getHeader().setFlag(Flags.TC);
        byte[] wire = msg.toWire();
        // The TC flag occupies the 2nd byte (bit 1). Verify it is set.
        int flags = ((wire[2] & 0xFF) << 8) | (wire[3] & 0xFF);
        assertTrue((flags & Flags.TC) != 0, "TC flag must be set in the wire header");
    }

    /**
     * Message with multiple question records.
     */
    @Test
    @DisplayName("Message with Multiple Question Records")
    void messageWithMultipleQuestions() throws IOException {
        Message msg = new Message();
        Record q1 = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
        Record q2 = Record.newRecord(Name.fromConstantString("example.org."), Type.AAAA, DClass.IN);
        msg.addRecord(q1, Section.QUESTION);
        msg.addRecord(q2, Section.QUESTION);
        byte[] wire = msg.toWire();
        assertTrue(wire.length > 12, "Wire must contain two question entries");
        Message parsed = new Message(wire);
        List<Record> qs = parsed.getSection(Section.QUESTION);
        assertEquals(2, qs.size());
        assertEquals(q1, qs.get(0));
        assertEquals(q2, qs.get(1));
    }

    /**
     * Message with OPT followed by a signed TSIG – verify ordering.
     */
    @Test
    @DisplayName("Message with OPT and Signed TSIG Order")
    void messageWithOPTAndSignedTSIGOrder() throws IOException {
        Message msg = new Message();
        // Add a dummy question.
        msg.addRecord(Record.newRecord(QUESTION_NAME, Type.A, DClass.IN), Section.QUESTION);
        // OPT first.
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        // Sign with TSIG.
        TSIG tsig = new TSIG(Name.fromConstantString("key.example."), TSIG.HMAC_MD5, "secret".getBytes());
        msg.setTSIG(tsig, Rcode.NOERROR, null);
        byte[] wire = msg.toWire();
        // Verify ordering in the additional section.
        List<Record> additional = msg.getSection(Section.ADDITIONAL);
        assertTrue(additional.size() >= 2, "At least OPT and TSIG must be present");
        assertTrue(additional.get(0) instanceof OPTRecord, "OPT must be first");
        assertTrue(additional.get(additional.size() - 1) instanceof TSIGRecord,
                "TSIG must be the last additional record");
    }

    /**
     * Message with various header flags (RD, AA, RCODE = NXDOMAIN).
     */
    @Test
    @DisplayName("Message with Various Header Flags")
    void messageWithHeaderFlags() throws IOException {
        Message msg = new Message();
        Header h = msg.getHeader();
        h.setFlag(Flags.RD);
        h.setFlag(Flags.AA);
        h.setRcode(Rcode.NXDOMAIN);
        msg.addRecord(Record.newRecord(QUESTION_NAME, Type.A, DClass.IN), Section.QUESTION);
        byte[] wire = msg.toWire();
        // Decode the flags from the wire header.
        int flags = ((wire[2] & 0xFF) << 8) | (wire[3] & 0xFF);
        assertTrue((flags & Flags.RD) != 0, "RD flag must be set");
        assertTrue((flags & Flags.AA) != 0, "AA flag must be set");
        assertEquals(Rcode.NXDOMAIN, flags & 0xF, "RCODE must be NXDOMAIN (3)");
    }

    /**
     * Verify that a manually altered {@code size} field is overwritten by {@code toWire()}.
     */
    @Test
    @DisplayName("Message Size Field Overwrite Verification")
    void messageSizeFieldOverwriteVerification() throws Exception {
        Message msg = new Message();
        // Force a bogus size via reflection.
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        sizeField.setInt(msg, 999);
        // Serialize.
        byte[] wire = msg.toWire();
        // After serialization the size field must equal the actual wire length.
        int sizeAfter = sizeField.getInt(msg);
        assertEquals(wire.length, sizeAfter, "Message.size must be overwritten with the actual wire length");
    }

    /**
     * Message that exactly reaches the maximum allowed length (65535 bytes).
     */
    @Test
    @DisplayName("Message at Maximum Allowed Length (65535 bytes)")
    void messageAtMaximumAllowedLength() throws IOException {
        Message msg = new Message();
        // Add a question (mandatory).
        msg.addRecord(Record.newRecord(QUESTION_NAME, Type.A, DClass.IN), Section.QUESTION);
        // Fill the additional section with many TXT records until we are just under the limit.
        // Approximate: each TXT record with a short string is ~30 bytes.
        // header size
        int target = Message.MAXLENGTH - 12;
        int added = 0;
        int i = 0;
        while (added < target - 100) {
            // leave a small margin for the final record
            String txt = "payload-" + i;
            Record rec = new TXTRecord(ADDITIONAL_NAME, DClass.IN, 300, txt);
            msg.addRecord(rec, Section.ADDITIONAL);
            // rough estimate using DNSOutput
            DNSOutput out = new DNSOutput();
            rec.rdataToWire(out);
            added += out.current();
            i++;
        }
        // Serialize.
        byte[] wire = msg.toWire();
        assertEquals(Message.MAXLENGTH, wire.length, "Wire length must be exactly the maximum allowed size");
    }

    /**
     * Message with an overly long domain name (>255 bytes) – expect an exception.
     */
    @Test
    @DisplayName("Message with Overly Long Domain Name")
    void messageWithOverlyLongDomainName() {
        Message msg = new Message();
        Record rec = new ARecord(LONG_NAME, DClass.IN, 300,
                InetAddress.getByName("1.2.3.4"));
        msg.addRecord(rec, Section.ANSWER);
        assertThrows(IOException.class, msg::toWire, "Serializing a name >255 bytes must throw IOException");
    }

    /**
     * Message with an unsupported RR type (e.g., type 0xFFFF).
     */
    @Test
    @DisplayName("Message with Unsupported RR Type")
    void messageWithUnsupportedRRType() {
        Message msg = new Message();
        // Create a generic Record with an unknown type.
        Record unknown = new Record(QUESTION_NAME, 0xFFFF, DClass.IN) {

            @Override
            public String rdataToString() {
                return "";
            }

            @Override
            protected void rrToWire(DNSOutput out, Compression c, boolean canonical) {
                // No data
            }

            @Override
            protected void rrFromWire(DNSInput in, Compression c, boolean canonical) {
                // No data
            }
        };
        msg.addRecord(unknown, Section.ANSWER);
        assertThrows(IOException.class, msg::toWire, "Serializing an unknown RR type should throw IOException");
    }

    /**
     * Message with a null Record added to a section – expect a NullPointerException.
     */
    @Test
    @DisplayName("Message with Null Record")
    void messageWithNullRecord() {
        Message msg = new Message();
        // The library permits adding null; toWire() should then NPE.
        msg.addRecord(null, Section.QUESTION);
        assertThrows(NullPointerException.class, msg::toWire, "Serializing a message containing a null Record should throw NPE");
    }

    /**
     * Invocation of the overloaded {@code toWire(int)} with an insufficient
     * maximum length (<12) – expect {@link IllegalArgumentException}.
     */
    @ParameterizedTest
    @ValueSource(ints = { 0, 5, 11 })
    @DisplayName("toWire with Insufficient Max Length")
    void toWireWithInsufficientMaxLength(int maxLength) {
        Message msg = new Message();
        assertThrows(IllegalArgumentException.class, () -> msg.toWire(maxLength),
                "maxLength smaller than DNS header size must cause IllegalArgumentException");
    }
}
```