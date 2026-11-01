```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Tests for {@link Message#toString()}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic message with QUESTION and ANSWER sections</li>
 *   <li>message that contains an OPT pseudo‑section</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_toString_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verify that the string representation contains the expected
     * section headings and the question/answer records.
     */
    @Test
    void toString_basicMessage_containsSectionsAndRecords() throws Exception {
        Message msg = new Message();                     // empty header, empty sections
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        msg.addRecord(question, Section.QUESTION);

        // add an answer record
        Record answer = new ARecord(qname, DClass.IN, 3600,
                InetAddress.getByName("1.2.3.4"));
        msg.addRecord(answer, Section.ANSWER);

        String out = msg.toString();

        // Section headings
        assertTrue(out.contains(";; QUESTION:"), "Missing QUESTION heading");
        assertTrue(out.contains(";; ANSWER:"),   "Missing ANSWER heading");

        // The question name should appear in the output
        assertTrue(out.contains(qname.toString()), "Question name not present");

        // The answer address should appear (ARecord prints the address)
        assertTrue(out.contains("1.2.3.4"), "Answer address not present");
    }

    /**
     * Verify that an OPT record (pseudo‑section) is rendered.
     */
    @Test
    void toString_withOPT_containsPseudoSection() {
        Message msg = new Message();
        // Create a minimal OPT record (payload size 4096, no other fields)
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        String out = msg.toString();

        // The OPT pseudo‑section is printed by OPTRecord.printPseudoSection().
        // The exact format is implementation dependent, but the string will
        // contain the word "OPT" and the payload size.
        assertTrue(out.contains("OPT"), "OPT pseudo‑section header missing");
        assertTrue(out.contains("4096"), "OPT payload size not rendered");
    }
}
```