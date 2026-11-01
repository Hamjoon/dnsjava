```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#getQuestion()}.
 *
 * The tests cover the three possible behaviours of the method:
 *
 * <ul>
 *   <li>When the QUESTION section is {@code null} – the method must return {@code null}.</li>
 *   <li>When the QUESTION section exists but is empty – the method must return {@code null}.</li>
 *   <li>When the QUESTION section contains one or more records – the method must return the first
 *       record that was added.</li>
 * </ul>
 *
 * No private members of {@link Message} are accessed; the public API
 * {@link Message#addRecord(Record, int)} and the constructors are used to set up the test
 * scenarios.
 */
@ExtendWith(MockitoExtension.class)
class Message_getQuestion_Test {

    /** A fresh {@link Message} instance for each test. */
    private Message message;

    /** A reusable {@link Name} for creating {@link Record}s. */
    private Name testName;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a clean Message and a constant Name before each test.
        message = new Message();                     // uses the no‑arg constructor
        testName = Name.fromConstantString("example.com.");
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly drop references – helps the GC in long test suites.
        message = null;
        testName = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /** The QUESTION section is {@code null} → {@code getQuestion()} must return {@code null}. */
    @Test
    void getQuestion_sectionIsNull_returnsNull() {
        // The no‑arg constructor leaves all sections as {@code null}.
        assertNull(message.getQuestion(),
                "When the QUESTION section is null, getQuestion() should return null");
    }

    /** The QUESTION section exists but is empty → {@code getQuestion()} must return {@code null}. */
    @Test
    void getQuestion_sectionIsEmpty_returnsNull() {
        // Initialise the QUESTION section with an empty list.
        // This mimics the state after construction followed by a call to getSection(Section.QUESTION)
        // which lazily creates an empty list.
        message.addRecord(null, Section.QUESTION); // addRecord handles null safely (no record added)
        // Remove the null that may have been added; ensure the list is empty.
        message.removeAllRecords(Section.QUESTION);
        assertTrue(message.getSection(Section.QUESTION).isEmpty(),
                "Pre‑condition: QUESTION section should be empty");
        assertNull(message.getQuestion(),
                "When the QUESTION section is empty, getQuestion() should return null");
    }

    /** When a single question record is present, it must be returned. */
    @Test
    void getQuestion_singleRecord_returnsThatRecord() {
        Record question = Record.newRecord(testName, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);
        assertSame(question, message.getQuestion(),
                "The single question record should be returned by getQuestion()");
    }

    /** When multiple records are present, the first one added must be returned. */
    @Test
    void getQuestion_multipleRecords_returnsFirstRecord() {
        Record first = Record.newRecord(testName, Type.A, DClass.IN);
        Record second = Record.newRecord(testName, Type.AAAA, DClass.IN);
        message.addRecord(first, Section.QUESTION);
        message.addRecord(second, Section.QUESTION);
        assertSame(first, message.getQuestion(),
                "The first record added to the QUESTION section should be returned");
    }
}
```