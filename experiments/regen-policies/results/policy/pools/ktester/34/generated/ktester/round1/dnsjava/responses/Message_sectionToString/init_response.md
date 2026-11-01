```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_sectionToString_Test {

    /** Message instance used by the tests. */
    private Message message;

    /* --------------------------------------------------------------------- */
    /*  Lifecycle methods                                                    */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh, empty Message for every test.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the Message instance will be garbage‑collected.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Message.sectionToString(int)                               */
    /* --------------------------------------------------------------------- */

    @Test
    void sectionToString_emptySection_returnsEmptyString() {
        // GIVEN an empty message (no records in any section)
        // WHEN we request the string representation of the QUESTION section
        String result = message.sectionToString(Section.QUESTION);
        // THEN the result is an empty string
        assertEquals("", result);
    }

    @Test
    void sectionToString_sectionWithOneRecord_containsRecordString() {
        // GIVEN a message that contains a single A record in the ANSWER section
        Name name = Name.fromConstantString("www.example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN, 3600);
        message.addRecord(aRecord, Section.ANSWER);

        // WHEN we request the string representation of the ANSWER section
        String result = message.sectionToString(Section.ANSWER);

        // THEN the result contains the textual representation of the record
        // Record.toString() is used internally by Message.sectionToString,
        // so we compare against that.
        String expected = aRecord.toString();
        assertTrue(result.contains(expected),
                () -> "Expected section string to contain: " + expected + " but was: " + result);
    }

    @Test
    void sectionToString_invalidSection_throwsIllegalArgumentException() {
        // GIVEN an invalid section index (outside 0‑3)
        int invalidSection = 4;
        // WHEN we invoke sectionToString with the invalid index
        // THEN an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(invalidSection));
    }
}
```