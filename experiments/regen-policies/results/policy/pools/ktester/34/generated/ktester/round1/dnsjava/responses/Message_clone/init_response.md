```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Tests for {@link Message#clone()}.
 *
 * The tests verify that:
 * <ul>
 *   <li>The cloned {@link Message} has an equal but distinct {@link Header}.</li>
 *   <li>The question record is correctly copied.</li>
 *   <li>All sections are deep‑copied (the lists themselves are new objects).</li>
 *   <li>Modifying the clone does not affect the original message.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_clone_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that cloning a query message copies the header ID and the question record,
     * while producing distinct Header and Message objects.
     */
    @Test
    void testCloneQuestionMessage() {
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);

        Message clone = query.clone();

        // Header ID must be equal but Header objects must be different
        assertEquals(query.getHeader().getID(), clone.getHeader().getID(),
                "Header IDs should be equal");
        assertNotSame(query.getHeader(), clone.getHeader(),
                "Header instances must be distinct");

        // Question record must be equal (same name, type, class) but not the same object
        assertEquals(query.getQuestion(), clone.getQuestion(),
                "Question records should be equal");
        assertNotSame(query.getQuestion(), clone.getQuestion(),
                "Question record instances must be distinct");
    }

    /**
     * Verify that cloning a response message copies all sections deeply.
     * Adding a record to the cloned message must not affect the original.
     */
    @Test
    void testCloneResponseDeepCopySections() throws UnknownHostException {
        // Build a response with records in QUESTION, ANSWER and ADDITIONAL sections
        Name name = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Record answer = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        Record additional = new TXTRecord(name, DClass.IN, 0, "extra");

        Message response = new Message();
        response.getHeader().setFlag(Flags.QR); // mark as response
        response.addRecord(question, Section.QUESTION);
        response.addRecord(answer, Section.ANSWER);
        response.addRecord(additional, Section.ADDITIONAL);

        Message clone = response.clone();

        // Verify that each section list is a different object
        for (int sec = 0; sec < 4; sec++) {
            List<Record> originalSec = response.getSection(sec);
            List<Record> cloneSec = clone.getSection(sec);
            assertNotSame(originalSec, cloneSec,
                    "Section list for section " + sec + " must be a new instance");
            assertEquals(originalSec, cloneSec,
                    "Contents of section " + sec + " must be equal after cloning");
        }

        // Mutate the clone and ensure original stays unchanged
        Record newAnswer = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.1"));
        clone.addRecord(newAnswer, Section.ANSWER);
        assertEquals(1, response.getSection(Section.ANSWER).size(),
                "Original answer section must remain unchanged");
        assertEquals(2, clone.getSection(Section.ANSWER).size(),
                "Clone must contain the newly added record");
    }
}
```