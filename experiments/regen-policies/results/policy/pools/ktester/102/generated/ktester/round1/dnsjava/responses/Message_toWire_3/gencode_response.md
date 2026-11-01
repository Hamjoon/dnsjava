```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link Message#toWire(int)} covering the scenarios described in the
 * supplied JSON test matrix.
 *
 * <p>All tests use real {@link Message} objects – no internal collaborators are mocked.
 * The private {@code size} field is accessed via reflection where verification of its value
 * is required.
 */
@ExtendWith(MockitoExtension.class)
class MessageToWireTest {

    /** Re‑usable name for most test messages. */
    private static Name qname;

    /** A simple A‑record used as a building block. */
    private static Record aRecord;

    @BeforeAll
    static void initConstants() throws IOException {
        qname = Name.fromConstantString("www.example.");
        aRecord = new ARecord(qname, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    }

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                     */
    /* --------------------------------------------------------------------- */

    /** Returns the private {@code size} field of a {@link Message}. */
    private static int getMessageSize(Message msg) throws ReflectiveOperationException {
        Field f = Message.class.getDeclaredField("size");
        f.setAccessible(true);
        return (int) f.get(msg);
    }

    /** Builds a message containing {@code count} identical answer records. */
    private static Message buildMessageWithAnswers(int count) {
        Message m = new Message();
        m.getHeader().setOpcode(Opcode.QUERY);
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        m.addRecord(q, Section.QUESTION);
        for (int i = 0; i < count; i++) {
            m.addRecord(aRecord, Section.ANSWER);
        }
        return m;
    }

    /** Builds a complex message with records in all sections and an OPT record. */
    private static Message buildComplexMessage() throws IOException {
        Message m = new Message();
        m.getHeader().setOpcode(Opcode.QUERY);
        // QUESTION
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        m.addRecord(q, Section.QUESTION);
        // ANSWER
        m.addRecord(aRecord, Section.ANSWER);
        // AUTHORITY
        Record ns = new NSRecord(Name.fromConstantString("example.com."),
                DClass.IN, 0, Name.fromConstantString("ns1.example.com."));
        m.addRecord(ns, Section.AUTHORITY);
        // ADDITIONAL – an OPT pseudo‑record
        OPTRecord opt = new OPTRecord(4096, 0, 0);
        m.addRecord(opt, Section.ADDITIONAL);
        return m;
    }

    /** Creates a signed message (TSIG) with a single question. */
    private static Message buildSignedMessage() throws IOException {
        Message m = new Message();
        m.getHeader().setOpcode(Opcode.QUERY);
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        m.addRecord(q, Section.QUESTION);
        // Simple TSIG key (key name, algorithm, secret)
        TSIG tsig = new TSIG(Name.fromConstantString("testkey."), "hmac-md5", "secret".getBytes());
        m.setTSIG(tsig);
        return m;
    }

    /* --------------------------------------------------------------------- */
    /* Test groups – one method per JSON “group”                           */
    /* --------------------------------------------------------------------- */

    /** Successful conversion – normal size (maxLength 512). */
    @Test
    void successfulConversion_normalSize() throws Exception {
        Message msg = new Message();
        msg.getHeader().setOpcode(Opcode.QUERY);
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        msg.addRecord(q, Section.QUESTION);
        msg.addRecord(aRecord, Section.ANSWER);

        int maxLength = 512;
        byte[] wire = msg.toWire(maxLength);

        assertNotNull(wire);
        assertTrue(wire.length <= maxLength,
                () -> "wire length " + wire.length + " exceeds maxLength " + maxLength);
        assertEquals(wire.length, getMessageSize(msg),
                "Message.size must equal the length of the returned wire array");
    }

    /** Boundary condition – exact maxLength (128 bytes). */
    @Test
    void boundaryExactMaxLength() throws Exception {
        // Build a message, then discover its exact wire size.
        Message msg = buildComplexMessage(); // should be well under 128 bytes
        byte[] fullWire = msg.toWire(); // natural size
        int exactSize = fullWire.length;
        assertTrue(exactSize <= 128, "Pre‑condition: generated message must be ≤128 bytes");

        // Now call toWire with the exact size.
        byte[] wire = msg.toWire(exactSize);
        assertEquals(exactSize, wire.length,
                "wire length must equal the supplied maxLength when it matches the natural size");
        assertEquals(wire.length, getMessageSize(msg),
                "Message.size must be updated to the exact length");
    }

    /** Exceeding maxLength – truncation path (maxLength 64). */
    @Test
    void exceedingMaxLength_truncationPath() throws Exception {
        Message msg = buildMessageWithAnswers(30); // large enough to exceed 64 bytes
        int maxLength = 64;
        byte[] wire = msg.toWire(maxLength);

        assertNotNull(wire);
        assertEquals(maxLength, wire.length,
                "wire array must be exactly maxLength when truncation occurs");
        assertEquals(maxLength, getMessageSize(msg),
                "Message.size must reflect the truncated length");

        // Verify TC flag is set after truncation.
        Message parsed = new Message(wire);
        assertTrue(parsed.getHeader().getFlag(Flags.TC),
                "TC flag must be set on a truncated message");
    }

    /** Exceeding maxLength – exception path (maxLength 10). */
    @Test
    void exceedingMaxLength_exceptionPath() {
        Message msg = new Message();
        msg.getHeader().setOpcode(Opcode.QUERY);
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        msg.addRecord(q, Section.QUESTION);
        // Header is 12 bytes, question > 0, so 10 is insufficient.
        assertThrows(MessageSizeExceededException.class,
                () -> msg.toWire(10),
                "MessageSizeExceededException expected when maxLength is smaller than required");
    }

    /** Invalid argument – negative maxLength. */
    @Test
    void invalidArgument_negativeMaxLength() {
        Message msg = new Message();
        assertThrows(IllegalArgumentException.class,
                () -> msg.toWire(-1),
                "Negative maxLength must cause IllegalArgumentException");
    }

    /** Message with TSIG – signed message handling (maxLength 1024). */
    @Test
    void tsigSignedMessage_respectsMaxLength() throws Exception {
        Message msg = buildSignedMessage();
        int maxLength = 1024;
        byte[] wire = msg.toWire(maxLength);

        assertNotNull(wire);
        assertTrue(wire.length <= maxLength,
                "wire length must not exceed supplied maxLength");
        assertEquals(wire.length, getMessageSize(msg),
                "Message.size must equal the returned wire length");

        // Parse and verify TSIG presence.
        Message parsed = new Message(wire);
        assertNotNull(parsed.getTSIG(),
                "Parsed message should contain a TSIG record");
    }

    /** Minimal message – header only (maxLength 12). */
    @Test
    void minimalMessage_headerOnly() throws Exception {
        Message msg = new Message(); // only header, no question
        int maxLength = 12; // exact header size
        byte[] wire = msg.toWire(maxLength);

        assertNotNull(wire);
        assertEquals(12, wire.length,
                "Header‑only message must produce exactly 12 bytes");
        assertEquals(12, getMessageSize(msg),
                "Message.size must be 12 for a header‑only message");
    }

    /** Zero maxLength – argument validation. */
    @Test
    void zeroMaxLength_argumentValidation() {
        Message msg = new Message();
        assertThrows(IllegalArgumentException.class,
                () -> msg.toWire(0),
                "Zero maxLength must be rejected with IllegalArgumentException");
    }

    /** Maximum allowed size – MAXLENGTH boundary (65535). */
    @Test
    void maximumAllowedSize_boundary() throws Exception {
        // Construct a very large message (many records) but stay within 65535 bytes.
        Message msg = new Message();
        msg.getHeader().setOpcode(Opcode.QUERY);
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        msg.addRecord(q, Section.QUESTION);
        // Add 500 identical A records – should approach the limit.
        for (int i = 0; i < 500; i++) {
            msg.addRecord(aRecord, Section.ANSWER);
        }

        int maxLength = Message.MAXLENGTH;
        byte[] wire = msg.toWire(maxLength);

        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH,
                "wire length must not exceed Message.MAXLENGTH");
        assertEquals(wire.length, getMessageSize(msg),
                "Message.size must equal the actual wire length");
    }

    /** Complex message – multiple sections without truncation (maxLength 1024). */
    @Test
    void complexMessage_multipleSections() throws Exception {
        Message msg = buildComplexMessage();
        int maxLength = 1024;
        byte[] wire = msg.toWire(maxLength);

        assertNotNull(wire);
        assertTrue(wire.length <= maxLength,
                "Complex message must fit within the supplied maxLength");
        assertEquals(wire.length, getMessageSize(msg),
                "Message.size must match the wire length");

        // Verify that all sections survived the round‑trip.
        Message parsed = new Message(wire);
        assertEquals(msg.getQuestion(), parsed.getQuestion(),
                "Question section must be identical after parsing");
        assertEquals(msg.getSection(Section.ANSWER), parsed.getSection(Section.ANSWER),
                "Answer section must be identical after parsing");
        assertEquals(msg.getSection(Section.AUTHORITY), parsed.getSection(Section.AUTHORITY),
                "Authority section must be identical after parsing");
        assertEquals(msg.getSection(Section.ADDITIONAL), parsed.getSection(Section.ADDITIONAL),
                "Additional section must be identical after parsing");
    }

    /** Repeated toWire calls – size field consistency. */
    @Test
    void repeatedToWire_callsSizeConsistency() throws Exception {
        Message msg = new Message();
        msg.getHeader().setOpcode(Opcode.QUERY);
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        msg.addRecord(q, Section.QUESTION);
        msg.addRecord(aRecord, Section.ANSWER);

        // First conversion with a relatively small limit.
        int firstLimit = 512;
        byte[] firstWire = msg.toWire(firstLimit);
        int firstSize = getMessageSize(msg);
        assertEquals(firstWire.length, firstSize,
                "After first toWire, size must equal first wire length");

        // Add another answer record and convert with a larger limit.
        msg.addRecord(aRecord, Section.ANSWER);
        int secondLimit = 1024;
        byte[] secondWire = msg.toWire(secondLimit);
        int secondSize = getMessageSize(msg);
        assertEquals(secondWire.length, secondSize,
                "After second toWire, size must equal second wire length");
        assertTrue(secondSize > firstSize,
                "Size after second conversion must be larger than after first conversion");
    }

    /** TSIG present but maxLength too small – exception path (maxLength 30). */
    @Test
    void tsigTooSmallMaxLength_exception() throws Exception {
        Message msg = buildSignedMessage(); // contains TSIG (~20‑30 bytes)
        int maxLength = 30; // deliberately too small for header+question+TSIG
        assertThrows(MessageSizeExceededException.class,
                () -> msg.toWire(maxLength),
                "MessageSizeExceededException expected when TSIG cannot fit");
    }

    /** Very large maxLength – larger than any possible wire size (200 000). */
    @Test
    void veryLargeMaxLength_ignoredBeyondProtocolLimit() throws Exception {
        Message msg = buildComplexMessage(); // moderate size
        int oversized = 200_000;
        byte[] wire = msg.toWire(oversized);

        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH,
                "Resulting wire length must never exceed Message.MAXLENGTH");
        assertEquals(wire.length, getMessageSize(msg),
                "Message.size must equal the actual wire length");
    }

    /** Header only with insufficient maxLength for question (maxLength 12). */
    @Test
    void headerOnlyInsufficientForQuestion_exception() {
        Message msg = new Message();
        msg.getHeader().setOpcode(Opcode.QUERY);
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        msg.addRecord(q, Section.QUESTION);
        // Only 12 bytes allowed – not enough for the question.
        assertThrows(MessageSizeExceededException.class,
                () -> msg.toWire(12),
                "MessageSizeExceededException expected when question cannot be encoded");
    }

    /** maxLength exceeds DNS protocol limit (65536). */
    @Test
    void maxLengthExceedsProtocolLimit_illegalArgument() {
        Message msg = new Message();
        assertThrows(IllegalArgumentException.class,
                () -> msg.toWire(65536),
                "maxLength greater than Message.MAXLENGTH must cause IllegalArgumentException");
    }
}
```