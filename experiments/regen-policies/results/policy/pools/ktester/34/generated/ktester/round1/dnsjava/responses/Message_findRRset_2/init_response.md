```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class Message_findRRset_2_Test {

    private Message message;
    private Name testName;
    private static final int TYPE_A = Type.A;
    private static final int TYPE_TXT = Type.TXT;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Fresh message and name for each test
        message = new Message();
        testName = Name.fromConstantString("example.com.");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed
    }

    @Test
    void findRRset_presentInAnswer_returnsTrue() {
        // arrange: add an A record to the ANSWER section
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);

        // act
        boolean result = message.findRRset(testName, TYPE_A);

        // assert
        assertTrue(result, "RRset should be found in the ANSWER section");
    }

    @Test
    void findRRset_presentInAuthority_returnsTrue() {
        // arrange: add an A record to the AUTHORITY section
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.AUTHORITY);

        // act
        boolean result = message.findRRset(testName, TYPE_A);

        // assert
        assertTrue(result, "RRset should be found in the AUTHORITY section");
    }

    @Test
    void findRRset_presentInAdditional_returnsTrue() {
        // arrange: add an A record to the ADDITIONAL section
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ADDITIONAL);

        // act
        boolean result = message.findRRset(testName, TYPE_A);

        // assert
        assertTrue(result, "RRset should be found in the ADDITIONAL section");
    }

    @Test
    void findRRset_notPresent_returnsFalse() {
        // arrange: message is empty – no records added

        // act
        boolean result = message.findRRset(testName, TYPE_A);

        // assert
        assertFalse(result, "RRset should not be found when no matching record exists");
    }

    @Test
    void findRRset_wrongType_returnsFalse() {
        // arrange: add a TXT record (different type) to ANSWER
        Record txtRecord = Record.newRecord(testName, TYPE_TXT, DClass.IN);
        message.addRecord(txtRecord, Section.ANSWER);

        // act
        boolean result = message.findRRset(testName, TYPE_A); // looking for A, not TXT

        // assert
        assertFalse(result, "RRset should not be found when the type does not match");
    }

    @Test
    void findRRset_wrongName_returnsFalse() throws TextParseException {
        // arrange: add an A record for a different name
        Name otherName = Name.fromConstantString("other.example.com.");
        Record aRecord = Record.newRecord(otherName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);

        // act
        boolean result = message.findRRset(testName, TYPE_A); // looking for testName

        // assert
        assertFalse(result, "RRset should not be found when the name does not match");
    }

    @Test
    void findRRset_multipleSectionsOnlyOneMatches_returnsTrue() {
        // arrange: add matching record to AUTHORITY, non‑matching to ANSWER
        Record aRecordAuth = Record.newRecord(testName, TYPE_A, DClass.IN);
        Record txtRecordAns = Record.newRecord(testName, TYPE_TXT, DClass.IN);
        message.addRecord(aRecordAuth, Section.AUTHORITY);
        message.addRecord(txtRecordAns, Section.ANSWER);

        // act
        boolean result = message.findRRset(testName, TYPE_A);

        // assert
        assertTrue(result, "RRset should be found when it exists in any section");
    }
}
```