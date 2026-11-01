```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Message#newQuery(Record)}.
 *
 * The method should:
 * <ul>
 *   <li>create a {@link Message} with opcode set to {@link Opcode#QUERY}</li>
 *   <li>set the recursion‑desired flag ({@link Flags#RD})</li>
 *   <li>add the supplied {@link Record} to the QUESTION section</li>
 *   <li>expose that record via {@link Message#getQuestion()}</li>
 * </ul>
 */
class Message_newQuery_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verify that a normal query Message is correctly initialised.
     */
    @Test
    void newQuery_initialisesHeaderAndQuestionSection() {
        // Arrange – create a simple A record question
        Name name = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);

        // Act – build the query Message
        Message query = Message.newQuery(question);

        // Assert – header opcode must be QUERY
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                "Header opcode should be QUERY");

        // Assert – recursion‑desired flag must be set
        assertTrue(query.getHeader().getFlag(Flags.RD),
                "Header should have the RD (recursion desired) flag set");

        // Assert – the question record must be stored
        assertNotNull(query.getQuestion(), "Message.getQuestion() should not be null");
        assertEquals(question, query.getQuestion(),
                "Stored question should be identical to the supplied record");

        // Assert – the QUESTION section must contain exactly this record
        assertEquals(1, query.getSection(Section.QUESTION).size(),
                "QUESTION section should contain exactly one record");
        assertEquals(question, query.getSection(Section.QUESTION).get(0),
                "Record in QUESTION section should be the supplied record");
    }

    /**
     * Verify that passing {@code null} as the record results in a {@link NullPointerException}.
     * The implementation does not guard against null, so the NPE is expected.
     */
    @Test
    void newQuery_nullRecord_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Message.newQuery(null),
                "Message.newQuery(null) should throw NullPointerException");
    }

    /**
     * Ensure that the generated Message does not contain any records in other sections.
     */
    @Test
    void newQuery_otherSectionsAreEmpty() {
        Name name = Name.fromConstantString("test.example.");
        Record question = Record.newRecord(name, Type.MX, DClass.IN);
        Message query = Message.newQuery(question);

        // Sections other than QUESTION must be empty
        assertTrue(query.getSection(Section.ANSWER).isEmpty(),
                "ANSWER section should be empty");
        assertTrue(query.getSection(Section.AUTHORITY).isEmpty(),
                "AUTHORITY section should be empty");
        assertTrue(query.getSection(Section.ADDITIONAL).isEmpty(),
                "ADDITIONAL section should be empty");
    }
}
```