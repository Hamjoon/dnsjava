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

/**
 * Tests for {@link Message#toWire(int)}.
 *
 * <p>The tests focus on the public {@code toWire(int)} method – they do not reach into
 * private helpers.  The {@code Message} class is exercised with real DNS records, so no
 * mocking of internal collaborators is required.
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_3_Test {

    /** Common name used for the question and answer records. */
    private static Name qname;

    /** A simple A‑record used as an answer. */
    private static Record answerRecord;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Initialise reusable DNS objects once for the whole test suite.
        qname = Name.fromConstantString("www.example.");
        answerRecord = new ARecord(
                qname,
                DClass.IN,
                0,
                InetAddress.getByName("127.0.0.1"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verify that a normal query/response round‑trip produces a wire format whose length
     * does not exceed the supplied {@code maxLength} and that the message can be parsed
     * back to an equivalent {@link Message} instance.
     */
    @Test
    void toWire_basicMessage_respectsMaxLength() throws IOException {
        // Build a minimal query message.
        Message query = new Message();
        query.getHeader().setOpcode(Opcode.QUERY);
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        query.addRecord(question, Section.QUESTION);
        query.addRecord(answerRecord, Section.ANSWER);

        // Serialize with a generous limit.
        int maxLength = 512;
        byte[] wire = query.toWire(maxLength);

        assertNotNull(wire, "wire array must not be null");
        assertTrue(wire.length <= maxLength,
                () -> "wire length (" + wire.length + ") exceeds maxLength (" + maxLength + ")");

        // Re‑parse and verify that the essential parts survived the round‑trip.
        Message parsed = new Message(wire);
        assertEquals(question, parsed.getQuestion(),
                "question record should be identical after parsing");
        assertEquals(answerRecord, parsed.getSection(Section.ANSWER).get(0),
                "answer record should be identical after parsing");
    }

    /**
     * When {@code maxLength} is zero the implementation should return an empty byte
     * array (the header cannot be emitted because the caller explicitly asked for no data).
     * The method must not throw an exception.
     */
    @Test
    void toWire_zeroMaxLength_returnsEmptyArray() {
        Message empty = new Message(); // no records, just a header
        byte[] wire = empty.toWire(0);
        assertNotNull(wire, "wire must never be null");
        assertEquals(0, wire.length, "wire length must be zero when maxLength is zero");
    }

    /**
     * Build a message that is deliberately larger than the supplied {@code maxLength}
     * and verify that the resulting wire data is truncated and that the TC (truncated)
     * flag in the header is set.
     */
    @Test
    void toWire_messageLargerThanMaxLength_isTruncatedAndTCFlagSet() throws IOException {
        // Create a message with many answer records to exceed 256 bytes.
        Message large = new Message();
        large.getHeader().setOpcode(Opcode.QUERY);
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        large.addRecord(question, Section.QUESTION);

        // Add 30 identical A records – enough to blow past a tiny maxLength.
        List<Record> manyAnswers = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            manyAnswers.add(answerRecord);
        }
        manyAnswers.forEach(r -> large.addRecord(r, Section.ANSWER));

        int maxLength = 256; // intentionally small
        byte[] wire = large.toWire(maxLength);

        assertNotNull(wire);
        assertTrue(wire.length <= maxLength,
                () -> "wire length (" + wire.length + ") must not exceed maxLength (" + maxLength + ")");

        // Parse the truncated message and check the TC flag.
        Message parsed = new Message(wire);
        assertTrue(parsed.getHeader().getFlag(Flags.TC),
                "TC flag must be set when the message is truncated");
        // The number of answer records after truncation is implementation‑defined,
        // but it must be less than the original count.
        int originalCount = large.getHeader().getCount(Section.ANSWER);
        int truncatedCount = parsed.getHeader().getCount(Section.ANSWER);
        assertTrue(truncatedCount < originalCount,
                "truncated message should contain fewer answer records than the original");
    }
}
```