```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#sectionToString(int)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Behaviour on empty sections – the method should return an empty string.</li>
 *   <li>Behaviour with records – the returned string should contain a textual representation
 *       of the records that were added to the requested section.</li>
 *   <li>Validation of the {@code section} argument – an {@link IllegalArgumentException}
 *       must be thrown for values outside the range {@code 0‑3}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_sectionToString_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor – it creates a fresh Header and empty sections.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the Message instance will be garbage‑collected.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    @Test
    void sectionToString_returnsEmptyStringForEmptySections() {
        // All four sections are empty right after construction.
        for (int sec = Section.QUESTION; sec <= Section.ADDITIONAL; sec++) {
            String result = message.sectionToString(sec);
            assertNotNull(result, "Result must not be null for section " + sec);
            assertTrue(result.isEmpty(),
                    "Expected empty string for empty section " + sec + " but got: \"" + result + "\"");
        }
    }

    @Test
    void sectionToString_includesRecordStringRepresentation() throws UnknownHostException {
        // Prepare a question record (Section.QUESTION)
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);

        // Prepare an answer record (Section.ANSWER)
        Record answer = new ARecord(qname, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        message.addRecord(answer, Section.ANSWER);

        // Verify QUESTION section string contains the question's textual form
        String questionStr = message.sectionToString(Section.QUESTION);
        assertTrue(questionStr.contains(qname.toString()),
                "QUESTION section string should contain the name");
        assertTrue(questionStr.contains("A"),
                "QUESTION section string should contain the type A");

        // Verify ANSWER section string contains the answer's textual form
        String answerStr = message.sectionToString(Section.ANSWER);
        assertTrue(answerStr.contains(qname.toString()),
                "ANSWER section string should contain the name");
        assertTrue(answerStr.contains("A"),
                "ANSWER section string should contain the type A");
        assertTrue(answerStr.contains("127.0.0.1"),
                "ANSWER section string should contain the address");
    }

    @Test
    void sectionToString_throwsExceptionForInvalidSection() {
        // Section indices are defined 0‑3; 4 is out of range.
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(4),
                "Expected IllegalArgumentException for section index 4");

        // Negative values are also invalid.
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(-1),
                "Expected IllegalArgumentException for negative section index");
    }

    @Test
    void sectionToString_handlesMultipleRecordsInSameSection() throws IOException {
        // Add two TXT records to the ADDITIONAL section.
        Name name = Name.fromConstantString("example.com.");
        Record txt1 = new TXTRecord(name, DClass.IN, 0, "first");
        Record txt2 = new TXTRecord(name, DClass.IN, 0, "second");
        message.addRecord(txt1, Section.ADDITIONAL);
        message.addRecord(txt2, Section.ADDITIONAL);

        String additionalStr = message.sectionToString(Section.ADDITIONAL);
        assertTrue(additionalStr.contains("first"),
                "Additional section string should contain first TXT record");
        assertTrue(additionalStr.contains("second"),
                "Additional section string should contain second TXT record");
        // Ensure both records appear (simple count check)
        int occurrences = additionalStr.split("TXT").length - 1;
        assertEquals(2, occurrences,
                "Additional section string should contain exactly two TXT records");
    }
}
```