```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#findRecord(Record)}.
 *
 * The method scans the ANSWER, AUTHORITY and ADDITIONAL sections of a {@link Message}
 * and returns {@code true} if the supplied {@link Record} is present in any of those
 * sections. It must return {@code false} when the record is absent or only present in
 * the QUESTION section.
 */
@ExtendWith(MockitoExtension.class)
class Message_findRecord_2_Test {

    private Message message;
    private Record aRecord;
    private Record txtRecord;
    private Name name;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message and some records for each test.
        message = new Message();                     // uses default Header
        name = Name.fromConstantString("example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references – not strictly required but keeps the test tidy.
        message = null;
        aRecord = null;
        txtRecord = null;
        name = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void findRecord_notPresent_returnsFalse() {
        // No records have been added yet.
        assertFalse(message.findRecord(aRecord), "findRecord should return false when the record is absent");
    }

    @Test
    void findRecord_presentInAnswer_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue(message.findRecord(aRecord), "findRecord should return true when the record is in the ANSWER section");
    }

    @Test
    void findRecord_presentInAuthority_returnsTrue() {
        message.addRecord(aRecord, Section.AUTHORITY);
        assertTrue(message.findRecord(aRecord), "findRecord should return true when the record is in the AUTHORITY section");
    }

    @Test
    void findRecord_presentInAdditional_returnsTrue() {
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRecord(aRecord), "findRecord should return true when the record is in the ADDITIONAL section");
    }

    @Test
    void findRecord_presentOnlyInQuestion_returnsFalse() {
        // The QUESTION section is NOT examined by findRecord.
        message.addRecord(aRecord, Section.QUESTION);
        assertFalse(message.findRecord(aRecord), "findRecord must ignore records in the QUESTION section");
    }

    @Test
    void findRecord_multipleSections_oneMatch_returnsTrue() {
        // Add a different record to ANSWER and the target record to ADDITIONAL.
        message.addRecord(txtRecord, Section.ANSWER);
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRecord(aRecord), "findRecord should return true when the record appears in any examined section");
    }

    @Test
    void findRecord_nullRecord_throwsNPE() {
        // The implementation does not guard against null, so a NullPointerException is expected.
        assertThrows(NullPointerException.class, () -> message.findRecord(null));
    }
}
```