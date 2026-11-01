```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Message#newQuery(Record)}.
 *
 * <p>The original test suite used JUnit 5 parameterized tests, which require the
 * {@code junit-jupiter-params} module. In the execution environment only the core
 * JUnit 5 engine is available, causing a discovery failure.  The tests have been
 * rewritten as ordinary {@code @Test} methods to avoid that dependency while
 * preserving the original intent.</p>
 */
class Message_newQuery_Test {

    /**
     * Verifies that {@code Message.newQuery(record)} creates a message with the
     * correct header fields and places the record in the QUESTION section.
     */
    @Test
    void newQuery_createsMessageWithCorrectHeaderAndQuestion() {
        // Arrange: a simple A‑record question
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);

        // Act: create the query message
        Message query = Message.newQuery(question);

        // Assert: header opcode must be QUERY
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                "Header opcode should be QUERY");

        // Assert: Recursion Desired flag must be set
        assertTrue(query.getHeader().getFlag(Flags.RD),
                "Header should have the RD (recursion desired) flag set");

        // Assert: the question record is stored correctly
        assertEquals(question, query.getQuestion(),
                "The question record returned by getQuestion() must match the supplied record");
        assertEquals(question, query.getSection(Section.QUESTION).get(0),
                "The QUESTION section should contain the supplied record as its first entry");

        // Assert: header count for QUESTION section is 1
        assertEquals(1, query.getHeader().getCount(Section.QUESTION),
                "Header count for QUESTION section should be 1");

        // Assert: all other sections are empty and counts are zero
        for (int sec = Section.ANSWER; sec <= Section.ADDITIONAL; sec++) {
            assertTrue(query.getSection(sec).isEmpty(),
                    "Section " + sec + " should be empty");
            assertEquals(0, query.getHeader().getCount(sec),
                    "Header count for section " + sec + " should be zero");
        }
    }

    /**
     * Verifies that passing {@code null} as the record argument results in a
     * {@link NullPointerException}.
     */
    @Test
    void newQuery_nullRecord_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> Message.newQuery(null),
                "Message.newQuery(null) should throw NullPointerException");
    }

    /**
     * Ensures that the created message is independent of later modifications to
     * the original {@link Record}.  Records are immutable in dnsjava, but this
     * test guards against accidental coupling.
     */
    @Test
    void newQuery_messageIsIndependentFromOriginalRecord() {
        Name qname = Name.fromConstantString("host.example.");
        Record original = Record.newRecord(qname, Type.AAAA, DClass.IN);
        Message query = Message.newQuery(original);

        // The library's Record objects are immutable, but we can still verify that the
        // message holds a reference that equals the original.
        assertEquals(original, query.getQuestion(),
                "Message should contain a question equal to the original record");
    }

    /**
     * Helper that performs the common assertions for a given record type.
     */
    private void assertStandardQuery(Message query, Record record, String typeName) {
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                "Header opcode should be QUERY for " + typeName + " record");
        assertTrue(query.getHeader().getFlag(Flags.RD),
                "RD flag should be set for " + typeName + " record");
        assertEquals(record, query.getQuestion(),
                "Question record should match the supplied " + typeName + " record");
        assertEquals(record, query.getSection(Section.QUESTION).get(0),
                "QUESTION section should contain the supplied " + typeName + " record");
        assertEquals(1, query.getHeader().getCount(Section.QUESTION),
                "Header count for QUESTION should be 1 for " + typeName + " record");
        for (int sec = Section.ANSWER; sec <= Section.ADDITIONAL; sec++) {
            assertTrue(query.getSection(sec).isEmpty(),
                    "Section " + sec + " should be empty for " + typeName + " record");
            assertEquals(0, query.getHeader().getCount(sec),
                    "Header count for section " + sec + " should be zero for " + typeName + " record");
        }
    }

    /** A‑record query */
    @Test
    void newQuery_ARecord_createsCorrectMessage() {
        Name name = Name.fromConstantString("example.com.");
        Record a = Record.newRecord(name, Type.A, DClass.IN);
        Message query = Message.newQuery(a);
        assertStandardQuery(query, a, "A");
    }

    /** AAAA‑record query */
    @Test
    void newQuery_AAAARecord_createsCorrectMessage() {
        Name name = Name.fromConstantString("example.com.");
        Record aaaa = Record.newRecord(name, Type.AAAA, DClass.IN);
        Message query = Message.newQuery(aaaa);
        assertStandardQuery(query, aaaa, "AAAA");
    }

    /** MX‑record query */
    @Test
    void newQuery_MXRecord_createsCorrectMessage() {
        Name name = Name.fromConstantString("example.com.");
        Record mx = Record.newRecord(name, Type.MX, DClass.IN);
        Message query = Message.newQuery(mx);
        assertStandardQuery(query, mx, "MX");
    }
}
```