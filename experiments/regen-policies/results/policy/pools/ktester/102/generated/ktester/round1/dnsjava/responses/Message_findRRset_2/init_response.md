```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_findRRset_2_Test {

    /** Message under test – recreated before each test method. */
    private Message message;

    /** Common name used for the records in the tests. */
    private Name testName;

    /** A‑record for the test name (type A, class IN). */
    private Record aRecord;

    /** AAAA‑record for the test name (type AAAA, class IN). */
    private Record aaaaRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException, TextParseException {
        // Fresh message for every test – the default constructor creates an empty header.
        message = new Message();

        // Use a simple, absolute name.
        testName = Name.fromConstantString("example.com.");

        // Create two different records so we can test type‑sensitivity.
        aRecord = Record.newRecord(testName, Type.A, DClass.IN);
        aaaaRecord = Record.newRecord(testName, Type.AAAA, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly necessary but keeps the pattern clear.
        message = null;
        testName = null;
        aRecord = null;
        aaaaRecord = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /** The method should return {@code false} when the message contains no records. */
    @Test
    void findRRset_emptyMessage_returnsFalse() {
        assertFalse(message.findRRset(testName, Type.A),
                "findRRset should be false for an empty message");
    }

    /** The method should locate a matching RRset in the ANSWER section. */
    @Test
    void findRRset_recordInAnswer_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue(message.findRRset(testName, Type.A),
                "findRRset should be true when the record is in the ANSWER section");
    }

    /** The method should locate a matching RRset in the AUTHORITY section. */
    @Test
    void findRRset_recordInAuthority_returnsTrue() {
        message.addRecord(aRecord, Section.AUTHORITY);
        assertTrue(message.findRRset(testName, Type.A),
                "findRRset should be true when the record is in the AUTHORITY section");
    }

    /** The method should locate a matching RRset in the ADDITIONAL section. */
    @Test
    void findRRset_recordInAdditional_returnsTrue() {
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRRset(testName, Type.A),
                "findRRset should be true when the record is in the ADDITIONAL section");
    }

    /** The method must respect the requested type – a different type should not be found. */
    @Test
    void findRRset_wrongType_returnsFalse() {
        // Add an AAAA record but query for type A.
        message.addRecord(aaaaRecord, Section.ANSWER);
        assertFalse(message.findRRset(testName, Type.A),
                "findRRset should be false when only a different type is present");
    }

    /** When the same name exists with multiple types, the method should succeed for any matching type. */
    @Test
    void findRRset_multipleTypes_oneMatches_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(aaaaRecord, Section.ANSWER);
        assertTrue(message.findRRset(testName, Type.AAAA),
                "findRRset should be true for the AAAA type when both A and AAAA records are present");
    }

    /** Verify that a name that does not exist in any section yields {@code false}. */
    @Test
    void findRRset_nonexistentName_returnsFalse() throws TextParseException {
        Name otherName = Name.fromConstantString("other.example.com.");
        message.addRecord(aRecord, Section.ANSWER);
        assertFalse(message.findRRset(otherName, Type.A),
                "findRRset should be false for a name that is not present in any section");
    }
}
```