```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class Message_findRecord_Test {

    /** Message instance used in each test */
    private Message message;

    /** A generic A‑record used for the tests */
    private Record aRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Create a fresh Message with an empty header
        message = new Message();

        // Build a simple A‑record: www.example.com.  IN  A  127.0.0.1
        Name name = Name.fromConstantString("www.example.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent
        message = null;
        aRecord = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    @Test
    void findRecord_EmptySection_ReturnsFalse() {
        // Section.ANSWER is empty at this point
        boolean found = message.findRecord(aRecord, Section.ANSWER);
        assertFalse(found, "findRecord should return false when the section is empty");
    }

    @Test
    void findRecord_RecordPresentInSection_ReturnsTrue() {
        // Add the record to the QUESTION section
        message.addRecord(aRecord, Section.QUESTION);

        // The same record should now be found in the QUESTION section
        assertTrue(message.findRecord(aRecord, Section.QUESTION),
                "findRecord should return true when the record is present in the given section");
    }

    @Test
    void findRecord_RecordPresentInOtherSection_ReturnsFalse() {
        // Add the record to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);

        // Query a different section (QUESTION) – should be false
        assertFalse(message.findRecord(aRecord, Section.QUESTION),
                "findRecord should return false when the record exists but in a different section");
    }

    @Test
    void findRecord_InvalidSection_ThrowsIllegalArgumentException() {
        // Section.check throws IllegalArgumentException for out‑of‑range values
        int invalidSection = 5; // valid sections are 0‑3
        assertThrows(IllegalArgumentException.class,
                () -> message.findRecord(aRecord, invalidSection),
                "findRecord should throw IllegalArgumentException for an invalid section index");
    }

    @Test
    void findRecord_NullSectionArray_TreatedAsEmpty() {
        // Construct a Message via the DNSInput constructor that leaves a section null.
        // The simplest way is to use the no‑arg constructor and then manually clear a section.
        // Since we must not access private fields, we simulate the situation by removing all records.
        message.removeAllRecords(Section.ADDITIONAL); // ensures the internal list is null or empty
        // The method should safely return false even if the internal list is null.
        assertFalse(message.findRecord(aRecord, Section.ADDITIONAL),
                "findRecord should return false when the internal list for the section is null/empty");
    }
}
```