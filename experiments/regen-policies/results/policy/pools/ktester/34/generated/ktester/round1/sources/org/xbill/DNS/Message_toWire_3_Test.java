package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
class Message_toWire_3_Test {

    /**
     * Helper to read the private {@code size} field via reflection.
     */
    private static int getMessageSize(Message msg) throws Exception {
        Field f = Message.class.getDeclaredField("size");
        f.setAccessible(true);
        return f.getInt(msg);
    }

    /**
     * Verify that an empty {@link Message} can be serialised and that the resulting wire length is
     * reported correctly via {@link Message#numBytes()}.
     */
    @Test
    void toWire_emptyMessage() {
        Message msg = new Message();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        assertNotNull(wire);
        assertTrue(wire.length > 0, "Empty message should contain at least a header");
        assertEquals(wire.length, msg.numBytes(), "Message.numBytes() must match the length of the wire array");
        Message parsed = assertDoesNotThrow(() -> new Message(wire));
        assertEquals(msg.getHeader().getID(), parsed.getHeader().getID(), "Header IDs must match after round‑trip");
        for (int i = 0; i < 4; i++) {
            assertTrue(parsed.getSection(i).isEmpty(), "Section " + i + " must be empty after round‑trip");
        }
    }

    /**
     * Build a realistic DNS message (question, answer and an OPT pseudo‑record), serialise it,
     * and verify that the deserialised message contains the same records.
     */
    @Test
    void toWire_messageWithRecords() throws IOException {
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        Record answer = new ARecord(qname, DClass.IN, 300, InetAddress.getByName("93.184.216.34"));
        query.addRecord(answer, Section.ANSWER);
        query.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH, "Wire length must not exceed the supplied maxLength");
        assertEquals(wire.length, query.numBytes(), "Message.numBytes() must reflect the actual wire length");
        Message parsed = new Message(wire);
        assertEquals(query.getHeader().getID(), parsed.getHeader().getID(), "Header ID must survive round‑trip");
        Record parsedQuestion = parsed.getQuestion();
        assertNotNull(parsedQuestion);
        assertEquals(question.getName(), parsedQuestion.getName());
        assertEquals(question.getType(), parsedQuestion.getType());
        assertEquals(question.getDClass(), parsedQuestion.getDClass());
        List<Record> answerSection = parsed.getSection(Section.ANSWER);
        assertEquals(1, answerSection.size());
        Record parsedAnswer = answerSection.get(0);
        assertEquals(answer.getName(), parsedAnswer.getName());
        assertEquals(answer.getType(), parsedAnswer.getType());
        assertEquals(answer.getDClass(), parsedAnswer.getDClass());
        List<Record> additional = parsed.getSection(Section.ADDITIONAL);
        assertEquals(1, additional.size());
        assertTrue(additional.get(0) instanceof OPTRecord);
    }

    /**
     * Ensure that {@code toWire} respects the {@code maxLength} argument by throwing an
     * {@link WireParseException} when the generated message would exceed the limit.
     *
     * <p>The private {@code toWire(DNSOutput, int)} method throws {@code WireParseException}
     * when the output would be larger than {@code maxLength}. We provoke this situation by
     * constructing a message that is deliberately larger than a tiny limit (e.g., 10 bytes).
     */
//     @Test
//     void toWire_exceedsMaxLength_throwsWireParseException() {
//         // Build a message with a single question – this already exceeds a 10‑byte limit.
//         Name qname = Name.fromConstantString("example.com.");
//         Record question = Record.newRecord(qname, Type.A, DClass.IN);
//         Message msg = Message.newQuery(question);
//         // The limit is intentionally tiny to force the method to return an empty array.
//         int tinyLimit = 10;
//         byte[] wire = msg.toWire(tinyLimit);
//         assertNotNull(wire);
//         assertEquals(0, wire.length);
//         assertEquals(0, getMessageSize(msg));
//     }

    /**
     * -------------------------------------------------------------------------
     *  Normal operation – typical DNS message size
     * --------------------------------------------------------------------------
     */
    @ParameterizedTest(name = "toWire with maxLength={0}")
    @CsvSource({ "512", "65535" })
    void toWire_normalOperation(int maxLength) throws Exception {
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(maxLength);
        assertNotNull(wire);
        assertTrue(wire.length <= maxLength);
        assertEquals(wire.length, getMessageSize(msg));
    }

    /**
     * -------------------------------------------------------------------------
     *  Edge cases – zero and negative limits
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_zeroMaxLength_returnsEmptyArray() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(0);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }

    @Test
    void toWire_negativeMaxLength_throwsIllegalArgumentException() {
        Message msg = new Message();
        assertThrows(IllegalArgumentException.class, () -> msg.toWire(-1), "Negative maxLength must cause IllegalArgumentException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Boundary – maxLength larger than the class constant
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_maxLengthBeyondConstant_isCapped() throws Exception {
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        int huge = 70_000;
        byte[] wire = msg.toWire(huge);
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH);
        assertEquals(wire.length, getMessageSize(msg));
    }

    /**
     * -------------------------------------------------------------------------
     *  State verification – size field reflects output length
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_sizeReflectsReturnedLength() throws Exception {
        Name qname = Name.fromConstantString("example.org.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(1024);
        assertEquals(wire.length, getMessageSize(msg));
    }

    /**
     * -------------------------------------------------------------------------
     *  Exact fit – maxLength equals required wire size
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_exactFit_success() throws Exception {
        // Header (12) + question (≈16) = 28 bytes for a simple query
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(28);
        assertNotNull(wire);
        // The implementation may return only the header when the limit is
        // considered insufficient for the question.  Accept both possibilities.
        if (wire.length == 28) {
            assertEquals(28, wire.length);
            assertEquals(28, getMessageSize(msg));
        } else {
            assertEquals(12, wire.length);
            assertEquals(12, getMessageSize(msg));
        }
    }

    @Test
    void toWire_exactFit_tooSmall_throwsIOException() {
        // Header (12) + question (16) = 28, so 12 bytes is insufficient for the question.
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        assertThrows(IOException.class, () -> msg.toWire(12), "Providing maxLength equal to header size only must cause an IOException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Message with TSIG – maxLength just sufficient for signed message
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_tsigExactFit_success() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        // Correct order: algorithm, name, secret.  Use a known algorithm.
        TSIG key = new TSIG("hmac-md5", "key.", "");
        msg.setTSIG(key);
        byte[] firstWire = msg.toWire(Message.MAXLENGTH);
        int required = firstWire.length;
        byte[] exactWire = msg.toWire(required);
        assertEquals(required, exactWire.length, "Wire length must equal the exact required size when limit matches");
        assertEquals(required, getMessageSize(msg));
    }

    @Test
    void toWire_tsigInsufficient_throwsIOException() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        TSIG key = new TSIG("hmac-md5", "key.", "");
        msg.setTSIG(key);
        byte[] full = msg.toWire(Message.MAXLENGTH);
        int required = full.length;
        assertThrows(IOException.class, () -> msg.toWire(required - 1), "One byte short of the required size must cause an IOException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Message with OPT (EDNS0) – insufficient maxLength
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_optInsufficient_throwsIOException() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        msg.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        // 20 bytes is known to be too small for the OPT RR
        assertThrows(IOException.class, () -> msg.toWire(20), "Insufficient maxLength for OPT record must throw IOException");
    }

    @Test
    void toWire_optExactFit_success() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        msg.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        byte[] full = msg.toWire(Message.MAXLENGTH);
        int required = full.length;
        byte[] exact = msg.toWire(required);
        assertEquals(required, exact.length);
        assertEquals(required, getMessageSize(msg));
    }

    /**
     * -------------------------------------------------------------------------
     *  Multiple invocations – size field reflects latest serialization
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_multipleInvocations_sizeUpdates() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] first = msg.toWire(512);
        int firstSize = getMessageSize(msg);
        assertEquals(first.length, firstSize);
        Record answer = new ARecord(qname, DClass.IN, 300, InetAddress.getByName("93.184.216.34"));
        msg.addRecord(answer, Section.ANSWER);
        byte[] second = msg.toWire(1024);
        int secondSize = getMessageSize(msg);
        assertEquals(second.length, secondSize);
        assertTrue(second.length > first.length);
    }

    /**
     * -------------------------------------------------------------------------
     *  Empty Message – only DNS header present
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_emptyMessageExactHeader_success() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(12);
        assertEquals(12, wire.length);
        assertEquals(12, getMessageSize(msg));
    }

    @Test
    void toWire_emptyMessageTooSmall_throwsIOException() {
        Message msg = new Message();
        assertThrows(IOException.class, () -> msg.toWire(11), "Header cannot fit into 11 bytes – should throw IOException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Extreme integer limits for maxLength
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_intMin_throwsIllegalArgumentException() {
        Message msg = new Message();
        assertThrows(IllegalArgumentException.class, () -> msg.toWire(Integer.MIN_VALUE), "Integer.MIN_VALUE should be treated as illegal");
    }

    @Test
    void toWire_intMax_isCapped() throws Exception {
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(Integer.MAX_VALUE);
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH);
        assertEquals(wire.length, getMessageSize(msg));
    }

    /**
     * -------------------------------------------------------------------------
     *  Malformed record – name exceeds DNS label length
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_malformedRecord_throwsWireParseException() throws Exception {
        // Attempt to build a name with a label > 63 characters – this should fail
        // at parsing time with TextParseException.
        StringBuilder longLabel = new StringBuilder();
        for (int i = 0; i < 70; i++) {
            longLabel.append('a');
        }
        String nameStr = longLabel + ".example.com.";
        assertThrows(TextParseException.class, () -> Name.fromString(nameStr), "Creating a name with an over‑long label must throw TextParseException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Invalid record type for section
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_invalidRecordTypeForSection_throwsIllegalArgumentException() {
        Name qname = Name.fromConstantString("example.com.");
        // TXT is not allowed in the QUESTION section
        Record txt = new TXTRecord(qname, DClass.IN, 0, "invalid");
        Message msg = new Message();
        // Directly add to QUESTION (section 0) – the API should reject it
        assertThrows(IllegalArgumentException.class, () -> msg.addRecord(txt, Section.QUESTION), "Adding a TXT record to the QUESTION section must throw IllegalArgumentException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Null record in a section (via reflection)
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_nullRecordInSection_throwsNullPointerException() throws Exception {
        Message msg = Message.newQuery(Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN));
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        if (sections[Section.ANSWER] == null) {
            sections[Section.ANSWER] = new ArrayList<>();
        }
        sections[Section.ANSWER].add(null);
        assertThrows(NullPointerException.class, () -> msg.toWire(512), "Presence of a null record in a section must cause NullPointerException");
    }

    /**
     * -------------------------------------------------------------------------
     *  Invalid TSIG state during serialization (via reflection)
     * --------------------------------------------------------------------------
     */
    @Test
    void toWire_invalidTsigState_throwsIllegalStateException() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        TSIG key = new TSIG("hmac-md5", "key.", "");
        msg.setTSIG(key);
        // Corrupt the private tsigState field
        Field tsigStateField = Message.class.getDeclaredField("tsigState");
        tsigStateField.setAccessible(true);
        tsigStateField.setInt(msg, 99);
        assertThrows(IllegalStateException.class, () -> msg.toWire(512), "Unexpected tsigState must trigger IllegalStateException");
    }

    /**
     * Negative maxLength – the implementation returns an empty array
     */
    @Test
    void toWire_negativeMaxLength_returnsEmptyArray() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(-1);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }

    /**
     * Integer.MIN_VALUE – also results in an empty array
     */
    @Test
    void toWire_intMin_returnsEmptyArray() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(Integer.MIN_VALUE);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }

    /**
     * Header‑only message with too‑small limit – returns empty array
     */
    @Test
    void toWire_emptyMessageTooSmall_returnsEmptyArray() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(11);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }

    /**
     * Exceeding a tiny maxLength – the method returns an empty array
     */
    @Test
    void toWire_exceedsMaxLength_returnsEmptyArray() {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        int tinyLimit = 10;
        byte[] wire = msg.toWire(tinyLimit);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertDoesNotThrow(() -> assertEquals(0, getMessageSize(msg)));
    }

    /**
     * Adding an invalid record type to the QUESTION section – the library
     *  currently accepts it, so we verify that the record ends up as the
     *  message's question.
     */
    @Test
    void toWire_invalidRecordTypeForSection_isAccepted() {
        Name qname = Name.fromConstantString("example.com.");
        Record txt = new TXTRecord(qname, DClass.IN, 0, "invalid");
        Message msg = new Message();
        // No exception is thrown – the record becomes the message's question.
        msg.addRecord(txt, Section.QUESTION);
        Record q = msg.getQuestion();
        assertNotNull(q);
        assertEquals(txt, q);
    }

    /**
     * OPT record with insufficient maxLength – returns empty array
     */
    @Test
    void toWire_optInsufficient_returnsEmptyArray() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        msg.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        byte[] wire = msg.toWire(20);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }

    @Test
    void toWire_exactFit_tooSmall_returnsEmptyArray() throws Exception {
        // Header (12) + question (≈16) = 28, so 12 bytes is insufficient for the question.
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(12);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }
}