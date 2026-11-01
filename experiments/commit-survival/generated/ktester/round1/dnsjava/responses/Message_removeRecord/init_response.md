```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class Message_removeRecord_Test {

    /** Helper to create a simple TXT record used in the tests. */
    private static Record createTxtRecord(String nameStr, String txt) {
        Name name = Name.fromConstantString(nameStr);
        return new TXTRecord(name, DClass.IN, 3600, txt);
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void removeRecord_successfulRemoval_returnsTrueAndUpdatesHeader() {
        // Arrange – create a message and add a record to the ANSWER section.
        Message msg = new Message();
        Record txt = createTxtRecord("test.example.", "sample text");
        msg.addRecord(txt, Section.ANSWER);

        // Verify the header count was incremented by addRecord.
        Header hdr = msg.getHeader();
        assertEquals(1, hdr.getCount(Section.ANSWER),
                "Header count should be 1 after adding a record");

        // Act – remove the same record.
        boolean removed = msg.removeRecord(txt, Section.ANSWER);

        // Assert – removal succeeded, header count decremented, section empty.
        assertTrue(removed, "removeRecord should return true when the record exists");
        assertEquals(0, hdr.getCount(Section.ANSWER),
                "Header count should be decremented after removal");
        assertTrue(msg.getSection(Section.ANSWER).isEmpty(),
                "Section should be empty after the record is removed");
    }

    @Test
    void removeRecord_recordNotPresent_returnsFalseAndHeaderUnchanged() {
        // Arrange – a fresh message with no records in the AUTHORITY section.
        Message msg = new Message();
        Record txt = createTxtRecord("absent.example.", "does not exist");

        // Header count for the section should be zero initially.
        Header hdr = msg.getHeader();
        assertEquals(0, hdr.getCount(Section.AUTHORITY),
                "Header count should start at 0 for an empty section");

        // Act – attempt to remove a record that was never added.
        boolean removed = msg.removeRecord(txt, Section.AUTHORITY);

        // Assert – removal fails, header count stays the same, section remains null/empty.
        assertFalse(removed, "removeRecord should return false when the record is absent");
        assertEquals(0, hdr.getCount(Section.AUTHORITY),
                "Header count must remain unchanged when removal fails");
        // getSection returns an empty list even if the internal list was never created.
        assertTrue(msg.getSection(Section.AUTHORITY).isEmpty(),
                "Empty section should still return an empty list");
    }

    @Test
    void removeRecord_sectionListNull_returnsFalseWithoutException() {
        // Arrange – create a message and do NOT add any records to the ADDITIONAL section.
        Message msg = new Message();

        // The internal list for Section.ADDITIONAL is still null at this point.
        // Create a dummy record to pass to removeRecord.
        Record dummy = createTxtRecord("dummy.example.", "dummy");

        // Act – call removeRecord on a null‑initialized section.
        boolean result = msg.removeRecord(dummy, Section.ADDITIONAL);

        // Assert – method should safely return false and not throw.
        assertFalse(result, "removeRecord must return false when the section list is null");
        // Header count must stay zero.
        assertEquals(0, msg.getHeader().getCount(Section.ADDITIONAL));
    }
}
```