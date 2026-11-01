```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import java.net.UnknownHostException;

/**
 * Tests for {@link Message#removeRecord(Record, int)}.
 *
 * The tests verify the contract of the method:
 * <ul>
 *   <li>When the record is present in the specified section the method returns {@code true},
 *       the record is removed from the section and the header count for that section is
 *       decremented.</li>
 *   <li>When the record is not present (or the section list is {@code null}) the method returns
 *       {@code false} and the header count stays unchanged.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_removeRecord_Test {

    private Message message;
    private Header header;
    private static final int SECTION = Section.ANSWER;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for each test.
        message = new Message();          // uses a random ID header
        header = message.getHeader();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Removing a record that exists in the section should succeed,
     * return {@code true}, and decrement the header count.
     */
    @Test
    void removeExistingRecord_returnsTrue_andUpdatesHeader() throws UnknownHostException {
        // Arrange – create a simple A record and add it to the ANSWER section.
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        message.addRecord(aRecord, SECTION);

        // Verify pre‑condition: header count is 1 and the section contains the record.
        assertEquals(1, header.getCount(SECTION));
        assertTrue(message.getSection(SECTION).contains(aRecord));

        // Act – attempt to remove the same record.
        boolean removed = message.removeRecord(aRecord, SECTION);

        // Assert – removal succeeded, header count decreased, and record no longer present.
        assertTrue(removed, "removeRecord should return true for an existing record");
        assertEquals(0, header.getCount(SECTION), "Header count must be decremented");
        assertFalse(message.getSection(SECTION).contains(aRecord),
                "Section must no longer contain the removed record");
    }

    /**
     * Removing a record that is not present should return {@code false}
     * and leave the header count unchanged.
     */
    @Test
    void removeNonExistingRecord_returnsFalse_andHeaderUnchanged() throws UnknownHostException {
        // Arrange – create a record but do NOT add it to the message.
        Name name = Name.fromConstantString("example.org.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);

        // Ensure the section list is still null/empty and header count is 0.
        assertEquals(0, header.getCount(SECTION));
        assertTrue(message.getSection(SECTION).isEmpty());

        // Act – attempt to remove the non‑existent record.
        boolean removed = message.removeRecord(aRecord, SECTION);

        // Assert – removal failed, header count unchanged, section still empty.
        assertFalse(removed, "removeRecord should return false when record is absent");
        assertEquals(0, header.getCount(SECTION), "Header count must remain unchanged");
        assertTrue(message.getSection(SECTION).isEmpty(),
                "Section must remain empty after failed removal");
    }

    /**
     * If the internal list for a section is {@code null} (i.e. no records have ever been added
     * to that section), {@code removeRecord} must safely return {@code false}.
     */
    @Test
    void removeWhenSectionListIsNull_returnsFalse() throws UnknownHostException {
        // Arrange – a fresh message has a null list for each section.
        // Verify that the internal list for the chosen section is indeed null.
        // (We cannot access the private field directly, but the contract is that
        // getSection returns an empty list when the internal list is null.)
        assertTrue(message.getSection(SECTION).isEmpty());

        Name name = Name.fromConstantString("nonexistent.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);

        // Act – attempt removal.
        boolean removed = message.removeRecord(aRecord, SECTION);

        // Assert – method returns false and nothing crashes.
        assertFalse(removed, "removeRecord should return false when section list is null");
        assertEquals(0, header.getCount(SECTION), "Header count must stay at zero");
    }
}
```