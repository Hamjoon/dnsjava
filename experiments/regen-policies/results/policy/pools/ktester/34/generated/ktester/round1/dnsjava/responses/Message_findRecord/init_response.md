```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#findRecord(Record, int)}.
 *
 * The tests cover:
 * <ul>
 *   <li>searching in an empty section (should return {@code false})</li>
 *   <li>searching for a record that is present (should return {@code true})</li>
 *   <li>searching with an invalid section index (should throw {@link IllegalArgumentException})</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_findRecord_Test {

    private Message message;
    private Record aRecord;          // Record that will be added to the message
    private Record differentRecord; // Record that will NOT be added

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a fresh Message for each test
        message = new Message();

        // Build two simple A‑records (type A, class IN) with distinct names
        Name nameA = Name.fromConstantString("host1.example.com.");
        Name nameB = Name.fromConstantString("host2.example.com.");

        aRecord = Record.newRecord(nameA, Type.A, DClass.IN);
        differentRecord = Record.newRecord(nameB, Type.A, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the Message instance will be garbage‑collected
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    @Test
    void findRecord_EmptySection_ReturnsFalse() {
        // No records have been added yet; all sections are empty.
        boolean result = message.findRecord(aRecord, Section.ANSWER);
        assertFalse(result, "findRecord should return false when the section is empty");
    }

    @Test
    void findRecord_RecordPresent_ReturnsTrue() {
        // Add aRecord to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);

        // Now the same record should be found
        boolean result = message.findRecord(aRecord, Section.ANSWER);
        assertTrue(result, "findRecord should return true when the record exists in the section");
    }

    @Test
    void findRecord_RecordNotPresent_ReturnsFalse() {
        // Add only aRecord to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);

        // Query for a different record that was never added
        boolean result = message.findRecord(differentRecord, Section.ANSWER);
        assertFalse(result, "findRecord should return false when the record is not in the section");
    }

    @Test
    void findRecord_InvalidSection_ThrowsIllegalArgumentException() {
        // Section indices are 0‑3; using 4 should trigger Section.check to fail.
        assertThrows(IllegalArgumentException.class,
                () -> message.findRecord(aRecord, 4),
                "findRecord should throw IllegalArgumentException for an invalid section index");
    }
}
```