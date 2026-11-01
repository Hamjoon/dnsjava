```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class Message_findRRset_Test {

    private Message message;
    private Name nameA;
    private Name nameB;
    private Record aRecord;
    private Record mxRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Fresh Message for every test
        message = new Message();

        // Two distinct names for positive / negative cases
        nameA = Name.fromConstantString("example.com.");
        nameB = Name.fromConstantString("other.com.");

        // Records of different types for the same name
        aRecord = Record.newRecord(nameA, Type.A, DClass.IN);
        mxRecord = Record.newRecord(nameA, Type.MX, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Message has no external resources
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed
    }

    @Test
    void findRRset_sectionEmpty_returnsFalse() {
        // No records have been added; all sections are null/empty
        assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
        assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    void findRRset_recordPresentInAnswer_returnsTrue() {
        // Add an A record to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);

        // Positive match in the correct section
        assertTrue(message.findRRset(nameA, Type.A, Section.ANSWER));

        // Same record should not be found in other sections
        assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    void findRRset_nameMismatch_returnsFalse() {
        // Add an A record for nameA to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);

        // Query with a different name – should not match
        assertFalse(message.findRRset(nameB, Type.A, Section.ANSWER));
    }

    @Test
    void findRRset_typeMismatch_returnsFalse() {
        // Add an A record for nameA to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);

        // Query with the same name but a different type – should not match
        assertFalse(message.findRRset(nameA, Type.MX, Section.ANSWER));
    }

    @Test
    void findRRset_multipleSections_correctSectionDetected() {
        // Add records to two different sections
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(mxRecord, Section.AUTHORITY);

        // Verify each is found only in its own section
        assertTrue(message.findRRset(nameA, Type.A, Section.ANSWER));
        assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));

        assertTrue(message.findRRset(nameA, Type.MX, Section.AUTHORITY));
        assertFalse(message.findRRset(nameA, Type.MX, Section.ANSWER));
    }

    @Test
    void findRRset_invalidSection_throwsIllegalArgumentException() {
        // Section.check throws IllegalArgumentException for out‑of‑range values
        int invalidSection = 5; // valid sections are 0‑3
        assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(nameA, Type.A, invalidSection));
    }
}
```