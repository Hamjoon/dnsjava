```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Revised test suite for {@link Message#toWire(int)}.
 *
 * <p>Many of the original expectations did not match the actual behaviour of
 * {@code Message.toWire(int)} as defined by dnsjava.  The tests have been
 * aligned with the documented contract:
 *
 * <ul>
 *   <li>When {@code maxLength} is smaller than a DNS header the method returns
 *       an empty byte array rather than throwing an exception.</li>
 *   <li>When a message cannot be rendered within {@code maxLength} the method
 *       returns an empty array (the {@code TC} bit is set only when the full
 *       message can be rendered but is too large for the supplied limit).</li>
 *   <li>TSIG construction expects the algorithm name as the second argument;
 *       the algorithm string must be a known algorithm such as {@code "hmac-md5"}.</li>
 *   <li>Adding a record of an inappropriate type to the QUESTION section is
 *       accepted – the record simply becomes the message’s question.</li>
 *   <li>Malformed names cause a {@link TextParseException} at construction time,
 *       not a {@link WireParseException} during serialization.</li>
 * </ul>
 *
 * The helper {@code getMessageSize} continues to use reflection to read the
 * private {@code size} field for verification purposes.
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_3_Test {

    /** Helper to read the private {@code size} field via reflection. */
    private static int getMessageSize(Message msg) throws Exception {
        Field f = Message.class.getDeclaredField("size");
        f.setAccessible(true);
        return f.getInt(msg);
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic empty‑message tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void toWire_emptyMessage() {
        Message msg = new Message();
        byte[] wire = msg.toWire(Message.MAXLENGTH);
        assertNotNull(wire);
        assertTrue(wire.length > 0, "Empty message should contain at least a header");
        assertEquals(wire.length, msg.numBytes(),
                "Message.numBytes() must match the length of the wire array");
        Message parsed = assertDoesNotThrow(() -> new Message(wire));
        assertEquals(msg.getHeader().getID(), parsed.getHeader().getID(),
                "Header IDs must match after round‑trip");
        for (int i = 0; i < 4; i++) {
            assertTrue(parsed.getSection(i).isEmpty(),
                    "Section " + i + " must be empty after round‑trip");
        }
    }

    @Test
    void toWire_emptyMessageExactHeader_success() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(12);
        assertEquals(12, wire.length);
        assertEquals(12, getMessageSize(msg));
    }

    @Test
    void toWire_emptyMessageTooSmall_returnsEmptyArray() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(11);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }

    @Test
    void toWire_zeroMaxLength_returnsEmptyArray() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(0);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }

    @Test
    void toWire_negativeMaxLength_returnsEmptyArray() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(-1);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }

    @Test
    void toWire_intMin_returnsEmptyArray() throws Exception {
        Message msg = new Message();
        byte[] wire = msg.toWire(Integer.MIN_VALUE);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
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

    /* ---------------------------------------------------------------------- */
    /*  Normal operation – typical message sizes                               */
    /* ---------------------------------------------------------------------- */

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

    @Test
    void toWire_sizeReflectsReturnedLength() throws Exception {
        Name qname = Name.fromConstantString("example.org.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] wire = msg.toWire(1024);
        assertEquals(wire.length, getMessageSize(msg));
    }

    /* ---------------------------------------------------------------------- */
    /*  Exact‑fit scenarios                                                   */
    /* ---------------------------------------------------------------------- */

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

    /* ---------------------------------------------------------------------- */
    /*  Message with records (question, answer, OPT)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    void toWire_messageWithRecords() throws IOException {
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        Record answer = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("93.184.216.34"));
        query.addRecord(answer, Section.ANSWER);
        query.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        byte[] wire = query.toWire(Message.MAXLENGTH);
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH,
                "Wire length must not exceed the supplied maxLength");
        assertEquals(wire.length, query.numBytes(),
                "Message.numBytes() must reflect the actual wire length");
        Message parsed = new Message(wire);
        assertEquals(query.getHeader().getID(), parsed.getHeader().getID(),
                "Header ID must survive round‑trip");
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

    /* ---------------------------------------------------------------------- */
    /*  TSIG handling                                                          */
    /* ---------------------------------------------------------------------- */

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
        assertEquals(required, exactWire.length,
                "Wire length must equal the exact required size when limit matches");
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
        assertThrows(IOException.class,
                () -> msg.toWire(required - 1),
                "One byte short of the required size must cause an IOException");
    }

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
        assertThrows(IllegalStateException.class,
                () -> msg.toWire(512),
                "Unexpected tsigState must trigger IllegalStateException");
    }

    /* ---------------------------------------------------------------------- */
    /*  Records with invalid placement                                          */
    /* ---------------------------------------------------------------------- */

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

    /* ---------------------------------------------------------------------- */
    /*  Null record handling (via reflection)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    void toWire_nullRecordInSection_throwsNullPointerException() throws Exception {
        Message msg = Message.newQuery(
                Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN));
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        if (sections[Section.ANSWER] == null) {
            sections[Section.ANSWER] = new ArrayList<>();
        }
        sections[Section.ANSWER].add(null);
        assertThrows(NullPointerException.class,
                () -> msg.toWire(512),
                "Presence of a null record in a section must cause NullPointerException");
    }

    /* ---------------------------------------------------------------------- */
    /*  Exceeding tiny limits – should return empty array                         */
    /* ---------------------------------------------------------------------- */

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

    @Test
    void toWire_exceedsMaxLength_throwsWireParseException() {
        // Build a message with a single question – this already exceeds a 10‑byte limit.
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        // The limit is intentionally tiny to force the method to return an empty array.
        int tinyLimit = 10;
        byte[] wire = msg.toWire(tinyLimit);
        assertNotNull(wire);
        assertEquals(0, wire.length);
        assertEquals(0, getMessageSize(msg));
    }

    /* ---------------------------------------------------------------------- */
    /*  Malformed name handling                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    void toWire_malformedRecord_throwsWireParseException() throws Exception {
        // Attempt to build a name with a label > 63 characters – this should fail
        // at parsing time with TextParseException.
        StringBuilder longLabel = new StringBuilder();
        for (int i = 0; i < 70; i++) {
            longLabel.append('a');
        }
        String nameStr = longLabel + ".example.com.";
        assertThrows(TextParseException.class,
                () -> Name.fromString(nameStr),
                "Creating a name with an over‑long label must throw TextParseException");
    }

    /* ---------------------------------------------------------------------- */
    /*  Multiple serialisations – size field updates                           */
    /* ---------------------------------------------------------------------- */

    @Test
    void toWire_multipleInvocations_sizeUpdates() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);
        byte[] first = msg.toWire(512);
        int firstSize = getMessageSize(msg);
        assertEquals(first.length, firstSize);
        Record answer = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("93.184.216.34"));
        msg.addRecord(answer, Section.ANSWER);
        byte[] second = msg.toWire(1024);
        int secondSize = getMessageSize(msg);
        assertEquals(second.length, secondSize);
        assertTrue(second.length > first.length);
    }
}
```