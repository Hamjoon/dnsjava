```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class Message_findRecord_2_Test {

    private Message message;
    private Record aRecord;
    private Record txtRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a fresh Message for each test
        message = new Message();

        // Prepare a couple of records that can be used in the tests
        Name name = Name.fromConstantString("www.example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
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
    void findRecord_returnsTrueWhenRecordIsInAnswerSection() {
        // GIVEN: a record added to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);

        // WHEN
        boolean found = message.findRecord(aRecord);

        // THEN
        assertTrue(found, "findRecord should return true for a record present in the ANSWER section");
    }

    @Test
    void findRecord_returnsFalseWhenRecordIsNotPresent() {
        // GIVEN: a message with no records (all sections are null/empty)

        // WHEN
        boolean found = message.findRecord(txtRecord);

        // THEN
        assertFalse(found, "findRecord should return false when the record is not present in any section");
    }

    @Test
    void findRecord_checksAllSections() {
        // GIVEN: the same record added to QUESTION and ADDITIONAL sections
        message.addRecord(aRecord, Section.QUESTION);
        message.addRecord(aRecord, Section.ADDITIONAL);

        // WHEN
        boolean found = message.findRecord(aRecord);

        // THEN
        assertTrue(found, "findRecord should locate the record regardless of which section it resides in");
    }

    @Test
    void findRecord_returnsFalseWhenRecordExistsInDifferentSection() {
        // GIVEN: a different record is present in the ANSWER section
        message.addRecord(txtRecord, Section.ANSWER);

        // WHEN
        boolean found = message.findRecord(aRecord);

        // THEN
        assertFalse(found, "findRecord should return false when the exact Record instance is not present");
    }
}
```