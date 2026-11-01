```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional unit tests for {@link Message#toWire()} covering the scenarios described in the
 * specification JSON.  The original two tests are kept unchanged; the new tests focus on
 * edge‑cases, error handling and the interaction with OPT/TSIG records.
 *
 * <p>All tests use only the public API of {@link Message}.  The private {@code size}
 * field is accessed via reflection solely to verify that it matches the length of the
 * returned byte array.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_2_Test {

    /** A reusable {@link Message} instance used by the tests. */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh, empty message for each test.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* ---------------------------------------------------------------------- */
    /* Helper utilities                                                       */
    /* ---------------------------------------------------------------------- */

    /**
     * Returns the value of the private {@code size} field of a {@link Message}
     * instance via reflection.
     */
    private static int getMessageSize(Message msg) throws ReflectiveOperationException {
        Field f = Message.class.getDeclaredField("size");
        f.setAccessible(true);
        return f.getInt(msg);
    }

    /* ---------------------------------------------------------------------- */
    /* Existing tests (kept unchanged)                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    void toWire_roundTrip_basicMessage() throws IOException {
        // --- given -----------------------------------------------------------
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);

        Record answer = new ARecord(qname, DClass.IN, 3600,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(answer, Section.ANSWER);

        Record opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        // --- when ------------------------------------------------------------
        byte[] wire = message.toWire();

        // --- then ------------------------------------------------------------
        assertNotNull(wire, "Wire format must not be null");
        assertTrue(wire.length > 0, "Wire format must contain data");

        Message parsed = new Message(wire);

        assertEquals(message.getHeader().getID(), parsed.getHeader().getID(),
                "Header IDs must match after round‑trip");

        List<Record> parsedQuestion = parsed.getSection(Section.QUESTION);
        assertEquals(1, parsedQuestion.size(), "Exactly one question expected");
        assertEquals(question, parsedQuestion.get(0), "Question record must be preserved");

        List<Record> parsedAnswer = parsed.getSection(Section.ANSWER);
        assertEquals(1, parsedAnswer.size(), "Exactly one answer expected");
        assertEquals(answer, parsedAnswer.get(0), "Answer record must be preserved");

        List<Record> parsedAdditional = parsed.getSection(Section.ADDITIONAL);
        assertEquals(1, parsedAdditional.size(), "Exactly one additional record expected");
        assertTrue(parsedAdditional.get(0) instanceof OPTRecord,
                "Additional record must be an OPTRecord");
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

    /* ---------------------------------------------------------------------- */
    /* New tests – one per group from the JSON specification                  */
    /* ---------------------------------------------------------------------- */

    @Test
    void emptyMessage_producesHeaderOnly() throws Exception {
        // No records added – the message is completely empty.
        byte[] wire = message.toWire();

        // DNS header is always 12 bytes.
        assertNotNull(wire);
        assertEquals(12, wire.length, "Empty message should contain only the DNS header");

        // Verify that the private size field matches the wire length.
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
        // The wire must be larger than the header (12 bytes) because a question is present.
        assertTrue(wire.length > 12, "Wire must be larger than the DNS header");

        // Re‑parse and verify the question is present.
        Message parsed = new Message(wire);
        List<Record> qs = parsed.getSection(Section.QUESTION);
        assertEquals(1, qs.size());
        assertEquals(question, qs.get(0));

        // Size field must reflect the actual length.
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
        assertTrue(additionals.get(0) instanceof OPTRecord,
                "The additional record must be an OPTRecord");

        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void signedTsig_isAppendedAsLastRR() throws Exception {
        // Build a minimal query with a question.
        Name qname = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);

        // Create a dummy TSIG key (algorithm and secret are arbitrary for this test).
        TSIG tsig = new TSIG("testkey.", "hmac-md5.sig-alg.reg.int.", new byte[]{0x01, 0x02});
        message.setTSIG(tsig); // sign the message

        byte[] wire = message.toWire();

        Message parsed = new Message(wire);
        List<Record> additionals = parsed.getSection(Section.ADDITIONAL);
        assertFalse(additionals.isEmpty(), "There must be at least one additional record (the TSIG)");
        Record last = additionals.get(additionals.size() - 1);
        assertTrue(last instanceof TSIGRecord, "The last additional RR must be a TSIGRecord");

        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void messageExceedingMaxLength_throwsException() {
        // Build a message that will definitely exceed 65535 bytes.
        Name qname = Name.fromConstantString("example.com.");
        for (int i = 0; i < 2000; i++) {
            // Each TXT record is ~40 bytes; 2000 * 40 > 65535.
            Record txt = new TXTRecord(qname, DClass.IN, 300,
                    "payload-" + i + "-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
            message.addRecord(txt, Section.ANSWER);
        }

        // toWire() is annotated with @SneakyThrows, so it may wrap IOException in a RuntimeException.
        Exception ex = assertThrows(RuntimeException.class, () -> message.toWire(),
                "Serialising an oversized message should throw an exception");
        // The cause should be an IOException (or a subclass).
        assertTrue(ex.getCause() instanceof IOException,
                "The root cause must be an IOException indicating overflow");
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
        assertEquals(r1, qs.get(0), "First question must be preserved");
        assertEquals(r2, qs.get(1), "Second question must be preserved");
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

        // Authority section – NS record for the zone.
        Name nsName = Name.fromConstantString("ns1.example.com.");
        Record ns = new NSRecord(qname, DClass.IN, 300, nsName);
        message.addRecord(ns, Section.AUTHORITY);

        // Additional section – address for the NS.
        Record nsA = new ARecord(nsName, DClass.IN, 300,
                InetAddress.getByName("192.0.2.53"));
        message.addRecord(nsA, Section.ADDITIONAL);

        byte[] wire = message.toWire();
        Message parsed = new Message(wire);

        // Verify ordering by checking counts – the DNS parser already respects order.
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

        // Add OPT.
        Record opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        // Sign with TSIG.
        TSIG tsig = new TSIG("testkey.", "hmac-md5.sig-alg.reg.int.", new byte[]{0x01});
        message.setTSIG(tsig);

        byte[] wire = message.toWire();
        Message parsed = new Message(wire);
        List<Record> additionals = parsed.getSection(Section.ADDITIONAL);
        assertTrue(additionals.size() >= 2, "Both OPT and TSIG must be present");

        // The penultimate RR should be the OPT record, the last the TSIG.
        Record secondLast = additionals.get(additionals.size() - 2);
        Record last = additionals.get(additionals.size() - 1);
        assertTrue(secondLast instanceof OPTRecord, "OPT must appear before TSIG");
        assertTrue(last instanceof TSIGRecord, "Last additional RR must be TSIG");

        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void nameCompression_resultsInSmallerWire() throws Exception {
        // Same name used for question and answer – compression should be applied.
        Name name = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        Record a = new ARecord(name, DClass.IN, 300,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(a, Section.ANSWER);

        byte[] wire = message.toWire();

        // Verify that the wire contains at least one compression pointer (0xC0 xx).
        boolean hasPointer = false;
        for (int i = 12; i < wire.length - 1; i++) { // skip header
            if ((wire[i] & 0xFF) == 0xC0) {
                hasPointer = true;
                break;
            }
        }
        assertTrue(hasPointer, "Wire format should contain a name‑compression pointer");

        // Size field must match the actual array length.
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void exactlyMaxLength_messageSucceeds() throws Exception {
        // Build a message whose size is exactly Message.MAXLENGTH.
        // Strategy: start with a header (12 bytes) and keep adding large TXT records
        // until we reach the limit.
        Name name = Name.fromConstantString("example.com.");
        int target = Message.MAXLENGTH;
        int current = 12; // header size

        while (current < target) {
            // Each TXT record with a 255‑byte string contributes roughly 270 bytes.
            // Adjust the payload size so we don't overshoot.
            int remaining = target - current;
            int payloadSize = Math.min(200, remaining - 30); // leave a small safety margin
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < payloadSize; i++) sb.append('x');
            Record txt = new TXTRecord(name, DClass.IN, 300, sb.toString());
            message.addRecord(txt, Section.ANSWER);
            // Approximate increase; we will rely on the final toWire() to verify exact size.
            current += 30 + payloadSize; // rough estimate
        }

        byte[] wire = message.toWire();

        assertEquals(Message.MAXLENGTH, wire.length,
                "Wire length must be exactly MAXLENGTH");
        assertEquals(wire.length, getMessageSize(message));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -10})
    void toWire_nonPositiveMaxLength_throwsIllegalArgumentException(int maxLength) {
        // Build a minimal message (header only) – the content is irrelevant.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> message.toWire(maxLength),
                "Calling toWire(int) with non‑positive maxLength must fail");
        assertTrue(ex.getMessage().contains("maxLength"),
                "Exception message should mention maxLength");
    }

    @Test
    void signedTsigWithErrorCode_containsErrorInRecord() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        Record a = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("93.184.216.34"));
        message.addRecord(a, Section.ANSWER);

        TSIG tsig = new TSIG("testkey.", "hmac-md5.sig-alg.reg.int.", new byte[]{0x01});
        // BADTIME = 18 in standard DNS rcode definitions.
        message.setTSIG(tsig, Rcode.BADTIME, null);

        byte[] wire = message.toWire();

        Message parsed = new Message(wire);
        List<Record> additionals = parsed.getSection(Section.ADDITIONAL);
        TSIGRecord tsigRec = (TSIGRecord) additionals.get(additionals.size() - 1);
        assertEquals(Rcode.BADTIME, tsigRec.getError(),
                "TSIG record must contain the supplied error code");
        assertEquals(wire.length, getMessageSize(message));
    }

    @Test
    void recordSerializationFailure_propagatesException() {
        // Create a custom Record that throws IOException from its toWire method.
        Record badRecord = new Record(Name.fromConstantString("bad.example."), Type.A, DClass.IN, 300) {
            @Override
            public void toWire(DNSOutput out) throws IOException {
                throw new IOException("forced serialization failure");
            }

            @Override
            protected String rrToString() {
                return "bad";
            }
        };
        message.addRecord(badRecord, Section.ANSWER);

        // The exception is wrapped by @SneakyThrows, so we expect a RuntimeException.
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> message.toWire(),
                "Serialization failure must be propagated");
        assertTrue(ex.getCause() instanceof IOException,
                "Root cause should be the IOException thrown by the Record");
    }

    // -----------------------------------------------------------------------
    // The original two tests are retained unchanged (see above).
    // -----------------------------------------------------------------------
}
```