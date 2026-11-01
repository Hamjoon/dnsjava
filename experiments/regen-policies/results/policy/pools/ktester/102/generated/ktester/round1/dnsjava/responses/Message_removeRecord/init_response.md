```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class Message_removeRecord_Test {

    private Message message;
    private Record aRecord;
    private Record txtRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test
        message = new Message();

        // Create simple records to work with.
        // Record.newRecord creates a generic Record (type A for the first, TXT for the second)
        aRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
        txtRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.TXT, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        message = null;
        aRecord = null;
        txtRecord = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    @Test
    void removeRecord_successfulRemoval_decrementsHeaderCount() {
        // GIVEN: a record added to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER),
                "Header count should be 1 after adding the record");

        // WHEN: we remove the same record
        boolean removed = message.removeRecord(aRecord, Section.ANSWER);

        // THEN: removal succeeds, header count is decremented, and the section is empty
        assertTrue(removed, "removeRecord should return true when the record exists");
        assertEquals(0, header.getCount(Section.ANSWER),
                "Header count should be decremented after removal");
        assertTrue(message.getSection(Section.ANSWER).isEmpty(),
                "Section should be empty after the record is removed");
    }

    @Test
    void removeRecord_recordNotPresent_returnsFalseAndHeaderUnchanged() {
        // GIVEN: a record added to the ANSWER section, but we will try to remove a different record
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER));

        // WHEN: we attempt to remove a record that was never added
        boolean removed = message.removeRecord(txtRecord, Section.ANSWER);

        // THEN: removal fails, header count stays the same, and the original record remains
        assertFalse(removed, "removeRecord should return false when the record is absent");
        assertEquals(1, header.getCount(Section.ANSWER),
                "Header count must remain unchanged when removal fails");
        assertEquals(1, message.getSection(Section.ANSWER).size(),
                "The original record should still be present in the section");
        assertSame(aRecord, message.getSection(Section.ANSWER).get(0));
    }

    @Test
    void removeRecord_sectionNeverInitialized_returnsFalse() {
        // GIVEN: a fresh Message with no sections initialized (no records added)
        // No explicit action needed – the constructor leaves all section lists null.

        // WHEN: we try to remove a record from a section that has never been created
        boolean removed = message.removeRecord(aRecord, Section.AUTHORITY);

        // THEN: removal should simply return false without throwing
        assertFalse(removed, "removeRecord must return false when the section list is null");
        // Header count for that section should still be zero
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
    }

    @Test
    void removeRecord_invalidSection_throwsIllegalArgumentException() {
        // GIVEN: a valid record added to a valid section
        message.addRecord(aRecord, Section.ADDITIONAL);

        // WHEN / THEN: passing an invalid section index should cause Section.check to throw
        assertThrows(IllegalArgumentException.class,
                () -> message.removeRecord(aRecord, 5),
                "Section.check should reject an out‑of‑range section index");
    }
}
```