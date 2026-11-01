```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Tests for {@link Message#findRecord(Record, int)}.
 */
class Message_findRecord_Test {

    private Message message;
    private Record txtRecord;
    private Record aRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        // Create a fresh Message for each test.
        message = new Message();

        // Prepare some records that will be used in the tests.
        Name txtName = Name.fromConstantString("test.example.");
        txtRecord = new TXTRecord(txtName, DClass.IN, 3600, "sample text");

        Name aName = Name.fromConstantString("host.example.");
        aRecord = new ARecord(aName, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Message does not hold external resources.
        message = null;
        txtRecord = null;
        aRecord = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void findRecord_sectionEmpty_returnsFalse() {
        // No records have been added to the ANSWER section yet.
        assertFalse(message.findRecord(txtRecord, Section.ANSWER),
                "findRecord should return false when the section list is empty");
    }

    @Test
    void findRecord_recordPresent_returnsTrue() {
        // Add the TXT record to the ANSWER section.
        message.addRecord(txtRecord, Section.ANSWER);
        assertTrue(message.findRecord(txtRecord, Section.ANSWER),
                "findRecord should return true when the record is present in the specified section");
    }

    @Test
    void findRecord_recordAbsentInSection_returnsFalse() {
        // Add the TXT record to the ANSWER section.
        message.addRecord(txtRecord, Section.ANSWER);
        // Query for a different record in the same section.
        assertFalse(message.findRecord(aRecord, Section.ANSWER),
                "findRecord should return false when the record is not present in the specified section");
    }

    @Test
    void findRecord_recordPresentInDifferentSection_returnsFalse() {
        // Add the TXT record to the AUTHORITY section.
        message.addRecord(txtRecord, Section.AUTHORITY);
        // Query the same record in the ANSWER section.
        assertFalse(message.findRecord(txtRecord, Section.ANSWER),
                "findRecord should return false when the record is present in a different section");
    }

    @Test
    void findRecord_invalidSection_throwsArrayIndexOutOfBoundsException() {
        // The Message implementation stores sections in an array of length 4 (0‑3).
        // Accessing an invalid index should propagate the ArrayIndexOutOfBoundsException.
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.findRecord(txtRecord, 5),
                "findRecord should throw ArrayIndexOutOfBoundsException for an invalid section index");
    }
}
```