```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#removeAllRecords(int)}.
 *
 * The tests verify that:
 * <ul>
 *   <li>the specified section is cleared and its count in the header is set to zero</li>
 *   <li>other sections remain untouched</li>
 *   <li>calling the method with an invalid section index throws {@link IllegalArgumentException}</li>
 *   <li>the method is idempotent (second call does not change the state further)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_removeAllRecords_Test {

    private Message message;
    private static final Name TEST_NAME = Name.fromConstantString("example.com.");

    /** Populate a fresh {@link Message} with one record in each of the four sections. */
    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        message = new Message(); // uses a random ID, fine for our purposes

        // QUESTION section
        Record q = Record.newRecord(TEST_NAME, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);

        // ANSWER section
        Record a = new ARecord(TEST_NAME, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        message.addRecord(a, Section.ANSWER);

        // AUTHORITY section
        Record ns = Record.newRecord(TEST_NAME, Type.NS, DClass.IN);
        message.addRecord(ns, Section.AUTHORITY);

        // ADDITIONAL section (use a simple TXT record)
        Record txt = new TXTRecord(TEST_NAME, DClass.IN, 0, "test");
        message.addRecord(txt, Section.ADDITIONAL);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up.
        message = null;
    }

    @Test
    void removeAllRecords_clearsSpecifiedSection_andLeavesOthersIntact() {
        // Verify pre‑condition: each section contains exactly one record
        assertEquals(1, message.getSection(Section.QUESTION).size());
        assertEquals(1, message.getSection(Section.ANSWER).size());
        assertEquals(1, message.getSection(Section.AUTHORITY).size());
        assertEquals(1, message.getSection(Section.ADDITIONAL).size());

        // Remove ANSWER section
        message.removeAllRecords(Section.ANSWER);

        // ANSWER section must be empty and header count zero
        List<Record> answer = message.getSection(Section.ANSWER);
        assertTrue(answer.isEmpty(), "ANSWER section should be empty after removal");
        assertEquals(0, message.getHeader().getCount(Section.ANSWER));

        // Other sections must stay unchanged
        assertEquals(1, message.getSection(Section.QUESTION).size());
        assertEquals(1, message.getSection(Section.AUTHORITY).size());
        assertEquals(1, message.getSection(Section.ADDITIONAL).size());
    }

    @Test
    void removeAllRecords_isIdempotent() {
        // First removal
        message.removeAllRecords(Section.AUTHORITY);
        assertTrue(message.getSection(Section.AUTHORITY).isEmpty());
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));

        // Second removal – should not throw and state stays the same
        message.removeAllRecords(Section.AUTHORITY);
        assertTrue(message.getSection(Section.AUTHORITY).isEmpty(),
                "Second call should keep AUTHORITY section empty");
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
    }

    @Test
    void removeAllRecords_invalidSection_throwsException() {
        // Section indices are 0‑3; 4 is invalid and should cause IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> message.removeAllRecords(4));
        // Negative index also invalid
        assertThrows(IllegalArgumentException.class, () -> message.removeAllRecords(-1));
    }

    @Test
    void removeAllRecords_allSectionsSequentially() {
        // Remove each section one after another and verify the cumulative effect
        for (int sec = 0; sec < 4; sec++) {
            message.removeAllRecords(sec);
            // The just‑removed section must be empty
            assertTrue(message.getSection(sec).isEmpty(),
                    "Section " + sec + " should be empty after removal");
            assertEquals(0, message.getHeader().getCount(sec),
                    "Header count for section " + sec + " should be zero");
        }

        // After all removals, the message should contain no records at all
        for (int sec = 0; sec < 4; sec++) {
            assertTrue(message.getSection(sec).isEmpty(),
                    "All sections should be empty after removing all");
        }
    }

    // -------------------------------------------------------------------------
    // Boilerplate lifecycle methods (kept for completeness – they do nothing)
    // -------------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }
}
```