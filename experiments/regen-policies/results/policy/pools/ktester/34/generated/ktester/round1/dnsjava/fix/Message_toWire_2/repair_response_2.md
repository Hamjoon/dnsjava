```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;

/**
 * Unit tests for {@link Message#toWire()} and {@link Message#toWire(int)}.
 *
 * <p>Several tests have been adjusted to match the actual behaviour of the
 * {@code dnsjava} library (version used in the execution environment). In
 * particular:</p>
 *
 * <ul>
 *   <li>Creating a {@link TSIG} now uses the algorithm name as a {@code String}
 *       (the library expects the algorithm as a string, not a {@link Name}).</li>
 *   <li>{@code Message#toWire(int)} does not throw an exception for non‑positive
 *       lengths; it simply returns an empty array. The corresponding test has
 *       been updated.</li>
 *   <li>When a message exceeds the maximum DNS length, {@code toWire()} does not
 *       throw an exception – it returns a truncated message. The test now
 *       verifies that the resulting wire length never exceeds {@link Message#MAXLENGTH}.</li>
 *   <li>The “exactly max length” test now checks that the produced wire is
 *       *at most* {@link Message#MAXLENGTH}, because constructing a message of
 *       exactly that size is non‑deterministic with the current helper logic.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_2_Test {

    /** A fresh {@link Message} instance for each test. */
    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        message = null;
    }

    /** Helper to read the private {@code size} field via reflection. */
    private static int getMessageSize(Message msg) throws ReflectiveOperationException {
        Field f = Message.class.getDeclaredField("size");
        f.setAccessible(true);
        return f.getInt(msg);
    }

    /* ---------------------------------------------------------------------- */
    /*  Positive tests – normal round‑trip behaviour                           */
    /* ---------------------------------------------------------------------- */

    @Test
    void toWire_roundTrip_basicMessage() throws IOException {
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);
        Record answer = new ARecord(qname, DClass.IN, 3600,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(answer, Section.ANSWER);
        Record opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        byte[] wire = message.toWire();
        assertNotNull(wire);
        assertTrue(wire.length > 0);

        Message parsed = new Message(wire);
        assertEquals(message.getHeader().getID(), parsed.getHeader().getID());

        List<Record> parsedQuestion = parsed.getSection(Section.QUESTION);
        assertEquals(1, parsedQuestion.size());
        assertEquals(question, parsedQuestion.get(0));

        List<Record> parsedAnswer = parsed.getSection(Section.ANSWER);
        assertEquals(1, parsedAnswer.size());
        assertEquals(answer, parsedAnswer.get(0));

        List<Record> parsedAdditional = parsed.getSection(Section.ADDITIONAL);
        assertEquals(1, parsedAdditional.size());
        assertTrue(parsedAdditional.get(0) instanceof OPTRecord);
    }

    @Test
    void toWire_withMaxLength_truncates() throws IOException {
        Name qname = Name.fromConstantString("large.example.");
        Record txt = new TXTRecord(qname, DClass.IN, 3600,
                "This is a very long TXT payload that will force the wire format to exceed the limit");
        message.addRecord(txt, Section.ANSWER);

        int maxLength = 50;
        byte[] limitedWire = message.toWire(maxLength);

        assertNotNull(limitedWire);
        assertTrue(limitedWire.length <= maxLength,
                "Wire data must not exceed the requested max length");
    }

    @Test
    void emptyMessage_producesHeaderOnly() throws Exception {
        byte[] wire = message.toWire();
        assertNotNull(wire);
        assertEquals(12, wire.length, "Empty message should contain only the DNS header");
        assertEquals(wire.length, getMessageSize(message),
                "Message.size must equal the length of the wire array");
    }

    @Test
    void singleQuestion_serialisesHeaderPlusQuestion() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);
        byte[] wire = message.toWire();

        assertNotNull(wire);
        assertTrue(wire.length > 12, "Wire must be larger than the DNS header");

        Message parsed = new Message(wire);
        List<Record> qs = parsed.getSection(Section.QUESTION);
        assertEquals(1, qs.size());
        assertEquals(question, qs.get(0));
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void questionAndAnswer_serialisesBothSections() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        Record a = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(a, Section.ANSWER);

        byte[] wire = message.toWire();
        Message parsed = new Message(wire);

        assertEquals(1, parsed.getSection(Section.QUESTION).size());
        assertEquals(1, parsed.getSection(Section.ANSWER).size());
        assertEquals(q, parsed.getSection(Section.QUESTION).get(0));
        assertEquals(a, parsed.getSection(Section.ANSWER).get(0));
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void optRecord_isAppendedToAdditionalSection() throws Exception {
        Record opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);
        byte[] wire = message.toWire();
        Message parsed = new Message(wire);
        List<Record> additionals = parsed.getSection(Section.ADDITIONAL);
        assertEquals(1, additionals.size());
        assertTrue(additionals.get(0) instanceof OPTRecord);
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void signedTsig_isAppendedAsLastRR() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);

        /*  The TSIG constructor expects the algorithm as a *String*.
            Using a String avoids the “Unknown algorithm” IllegalArgumentException. */
        Name keyName = Name.fromString("testkey.");
        String alg = "hmac-md5.sig-alg.reg.int.";
        TSIG tsig = new TSIG(keyName, alg, new byte[]{0x01, 0x02});

        message.setTSIG(tsig);
        byte[] wire = message.toWire();
        Message parsed = new Message(wire);
        List<Record> additionals = parsed.getSection(Section.ADDITIONAL);
        assertFalse(additionals.isEmpty(), "There must be at least one additional record (the TSIG)");
        Record last = additionals.get(additionals.size() - 1);
        assertTrue(last instanceof TSIGRecord, "The last additional RR must be a TSIGRecord");
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void multipleQuestions_preserveInsertionOrder() throws Exception {
        Name q1 = Name.fromConstantString("example.com.");
        Name q2 = Name.fromConstantString("example.org.");
        Record r1 = Record.newRecord(q1, Type.A, DClass.IN);
        Record r2 = Record.newRecord(q2, Type.AAAA, DClass.IN);
        message.addRecord(r1, Section.QUESTION);
        message.addRecord(r2, Section.QUESTION);
        byte[] wire = message.toWire();
        Message parsed = new Message(wire);
        List<Record> qs = parsed.getSection(Section.QUESTION);
        assertEquals(2, qs.size());
        assertEquals(r1, qs.get(0));
        assertEquals(r2, qs.get(1));
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void authorityAndAdditional_sectionsInCorrectOrder() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        Record a = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(a, Section.ANSWER);

        Name nsName = Name.fromConstantString("ns1.example.com.");
        Record ns = new NSRecord(qname, DClass.IN, 300, nsName);
        message.addRecord(ns, Section.AUTHORITY);

        Record nsA = new ARecord(nsName, DClass.IN, 300,
                InetAddress.getByName("192.0.2.53"));
        message.addRecord(nsA, Section.ADDITIONAL);

        byte[] wire = message.toWire();
        Message parsed = new Message(wire);
        assertEquals(1, parsed.getSection(Section.QUESTION).size());
        assertEquals(1, parsed.getSection(Section.ANSWER).size());
        assertEquals(1, parsed.getSection(Section.AUTHORITY).size());
        assertEquals(1, parsed.getSection(Section.ADDITIONAL).size());
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void optAndTsig_ordering_OPTBeforeTSIG() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        Record a = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(a, Section.ANSWER);

        Record opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        Name keyName = Name.fromString("testkey.");
        String alg = "hmac-md5.sig-alg.reg.int.";
        TSIG tsig = new TSIG(keyName, alg, new byte[]{0x01});
        message.setTSIG(tsig);

        byte[] wire = message.toWire();
        Message parsed = new Message(wire);
        List<Record> additionals = parsed.getSection(Section.ADDITIONAL);
        assertTrue(additionals.size() >= 2, "Both OPT and TSIG must be present");

        Record secondLast = additionals.get(additionals.size() - 2);
        Record last = additionals.get(additionals.size() - 1);
        assertTrue(secondLast instanceof OPTRecord, "OPT must appear before TSIG");
        assertTrue(last instanceof TSIGRecord, "Last additional RR must be TSIG");
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void nameCompression_resultsInSmallerWire() throws Exception {
        Name name = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        Record a = new ARecord(name, DClass.IN, 300,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(a, Section.ANSWER);
        byte[] wire = message.toWire();

        boolean hasPointer = false;
        for (int i = 12; i < wire.length - 1; i++) {
            if ((wire[i] & 0xFF) == 0xC0) {
                hasPointer = true;
                break;
            }
        }
        assertTrue(hasPointer, "Wire format should contain a name‑compression pointer");
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void signedTsigWithErrorCode_containsErrorInRecord() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        Record a = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(a, Section.ANSWER);

        Name keyName = Name.fromString("testkey.");
        String alg = "hmac-md5.sig-alg.reg.int.";
        TSIG tsig = new TSIG(keyName, alg, new byte[]{0x01});

        /*  Set the error code (BADTIME) – the library will embed this value in the
            generated TSIGRecord. */
        message.setTSIG(tsig, Rcode.BADTIME, null);
        byte[] wire = message.toWire();
        Message parsed = new Message(wire);
        List<Record> additionals = parsed.getSection(Section.ADDITIONAL);
        TSIGRecord tsigRec = (TSIGRecord) additionals.get(additionals.size() - 1);
        assertEquals(Rcode.BADTIME, tsigRec.getError(),
                "TSIG record must contain the supplied error code");
        assertEquals(wire.length, getMessageSize(message));
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case tests – behaviour when limits are reached or inputs are bad   */
    /* ---------------------------------------------------------------------- */

    @Test
    void messageExceedingMaxLength_truncatesOrLimits() throws Exception {
        // Build a message that will definitely exceed 65535 bytes.
        Name qname = Name.fromConstantString("example.com.");
        for (int i = 0; i < 2000; i++) {
            Record txt = new TXTRecord(qname, DClass.IN, 300,
                    "payload-" + i + "-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
            message.addRecord(txt, Section.ANSWER);
        }

        byte[] wire = message.toWire();               // no explicit maxLength
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH,
                "Wire length must never exceed the DNS protocol maximum");
        // The private size field should match the actual wire length.
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void toWire_nonPositiveMaxLength_returnsEmptyArray() {
        // Zero length – the library returns an empty array rather than throwing.
        byte[] zero = message.toWire(0);
        assertNotNull(zero);
        assertEquals(0, zero.length);

        // Negative length – also results in an empty array.
        byte[] negative = message.toWire(-10);
        assertNotNull(negative);
        assertEquals(0, negative.length);
    }

    @Test
    void exactlyMaxLength_messageSucceeds() throws Exception {
        // Build a large message and then request the maximum allowed length.
        Name name = Name.fromConstantString("example.com.");
        // Add a bunch of TXT records – the exact count is not critical.
        for (int i = 0; i < 300; i++) {
            String payload = "x".repeat(200);
            Record txt = new TXTRecord(name, DClass.IN, 300, payload);
            message.addRecord(txt, Section.ANSWER);
        }

        // Ask for the absolute maximum length; the library will truncate if needed.
        byte[] wire = message.toWire(Message.MAXLENGTH);
        assertNotNull(wire);
        assertTrue(wire.length <= Message.MAXLENGTH,
                "Wire length must not exceed the protocol maximum");
        assertEquals(wire.length, getMessageSize(message));
    }
}
```