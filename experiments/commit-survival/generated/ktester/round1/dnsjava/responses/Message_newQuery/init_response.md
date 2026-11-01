```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class Message_newQuery_Test {

    /** No global resources are required for the tests, but the methods are kept for completeness. */
    @BeforeAll
    static void setupBeforeAll() {
        // No static initialization needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization needed.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static cleanup needed.
    }

    /**
     * Verify that {@link Message#newQuery(Record)} creates a message whose header has the
     * {@code QUERY} opcode and the recursion‑desired (RD) flag set.
     */
    @Test
    void newQuery_setsHeaderOpcodeAndRDFlag() {
        Name name = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message query = Message.newQuery(question);

        Header header = query.getHeader();
        assertEquals(Opcode.QUERY, header.getOpcode(),
                "Header opcode should be QUERY");
        assertTrue(header.getFlag(Flags.RD),
                "Header should have the recursion‑desired (RD) flag set");
    }

    /**
     * Verify that the supplied {@link Record} is placed in the QUESTION section of the
     * newly created message.
     */
    @Test
    void newQuery_addsRecordToQuestionSection() {
        Name name = Name.fromConstantString("host.example.");
        Record question = Record.newRecord(name, Type.AAAA, DClass.IN);
        Message query = Message.newQuery(question);

        // The QUESTION section is index {@link Section#QUESTION}
        assertEquals(1, query.getHeader().getCount(Section.QUESTION),
                "Header count for QUESTION section should be 1");
        assertEquals(1, query.getSection(Section.QUESTION).size(),
                "QUESTION section should contain exactly one record");
        assertSame(question, query.getSection(Section.QUESTION).get(0),
                "The record stored in the QUESTION section should be the same instance passed in");
        // Convenience accessor
        assertEquals(question, query.getQuestion(),
                "Message#getQuestion() should return the supplied record");
    }

    /**
     * Ensure that a {@code null} argument results in a {@link NullPointerException}.
     * The implementation of {@code newQuery} does not guard against null, so the
     * exception is expected from the subsequent {@code addRecord} call.
     */
    @Test
    void newQuery_nullRecord_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Message.newQuery(null));
    }

    /**
     * Verify that the created message is otherwise a fresh instance:
     * all other sections are empty and the header counts are zero except for the
     * QUESTION section.
     */
    @Test
    void newQuery_otherSectionsAreEmpty() {
        Name name = Name.fromConstantString("test.example.");
        Record question = Record.newRecord(name, Type.MX, DClass.IN);
        Message query = Message.newQuery(question);

        // Sections 0 (ANSWER), 2 (AUTHORITY), 3 (ADDITIONAL) must be empty
        assertTrue(query.getSection(Section.ANSWER).isEmpty(),
                "ANSWER section should be empty");
        assertTrue(query.getSection(Section.AUTHORITY).isEmpty(),
                "AUTHORITY section should be empty");
        assertTrue(query.getSection(Section.ADDITIONAL).isEmpty(),
                "ADDITIONAL section should be empty");

        // Header counts for those sections must be zero
        assertEquals(0, query.getHeader().getCount(Section.ANSWER));
        assertEquals(0, query.getHeader().getCount(Section.AUTHORITY));
        assertEquals(0, query.getHeader().getCount(Section.ADDITIONAL));
    }
}
```