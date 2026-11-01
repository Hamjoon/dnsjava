package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

class Message_newQuery_Test {

    /**
     * Supplies a stream of arguments for {@link #newQuery_variousRecordTypes_createsCorrectMessage}.
     *
     * @return a stream of {@link Arguments} each containing a type description and a {@link Record}
     */
    static Stream<Arguments> provideRecordTypes() {
        Name name = Name.fromConstantString("example.com.");
        return Stream.of(Arguments.of("A", Record.newRecord(name, Type.A, DClass.IN)), Arguments.of("AAAA", Record.newRecord(name, Type.AAAA, DClass.IN)), Arguments.of("MX", Record.newRecord(name, Type.MX, DClass.IN)));
    }

    /**
     * Helper that performs the common assertions for a given record type.
     */
    private void assertStandardQuery(Message query, Record record, String typeName) {
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(), "Header opcode should be QUERY for " + typeName + " record");
        assertTrue(query.getHeader().getFlag(Flags.RD), "RD flag should be set for " + typeName + " record");
        assertEquals(record, query.getQuestion(), "Question record should match the supplied " + typeName + " record");
        assertEquals(record, query.getSection(Section.QUESTION).get(0), "QUESTION section should contain the supplied " + typeName + " record");
        assertEquals(1, query.getHeader().getCount(Section.QUESTION), "Header count for QUESTION should be 1 for " + typeName + " record");
        for (int sec = Section.ANSWER; sec <= Section.ADDITIONAL; sec++) {
            assertTrue(query.getSection(sec).isEmpty(), "Section " + sec + " should be empty for " + typeName + " record");
            assertEquals(0, query.getHeader().getCount(sec), "Header count for section " + sec + " should be zero for " + typeName + " record");
        }
    }

    /**
     * Verifies that {@code Message.newQuery(record)} creates a message with the correct header
     * fields and places the record in the QUESTION section.
     */
    @Test
    void newQuery_createsMessageWithCorrectHeaderAndQuestion() {
        // Arrange: a simple A‑record question
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        // Act: create the query message
        Message query = Message.newQuery(question);
        // Assert: header opcode must be QUERY
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(), "Header opcode should be QUERY");
        // Assert: Recursion Desired flag must be set
        assertTrue(query.getHeader().getFlag(Flags.RD), "Header should have the RD (recursion desired) flag set");
        // Assert: the question record is stored correctly
        assertEquals(question, query.getQuestion(), "The question record returned by getQuestion() must match the supplied record");
        assertEquals(question, query.getSection(Section.QUESTION).get(0), "The QUESTION section should contain the supplied record as its first entry");
        // Assert: header count for QUESTION section is 1
        assertEquals(1, query.getHeader().getCount(Section.QUESTION), "Header count for QUESTION section should be 1");
        // Assert: all other sections are empty and counts are zero
        for (int sec = Section.ANSWER; sec <= Section.ADDITIONAL; sec++) {
            assertTrue(query.getSection(sec).isEmpty(), "Section " + sec + " should be empty");
            assertEquals(0, query.getHeader().getCount(sec), "Header count for section " + sec + " should be zero");
        }
    }

    /**
     * Verifies that passing {@code null} as the record argument results in a {@link NullPointerException}.
     */
    @Test
    void newQuery_nullRecord_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Message.newQuery(null), "Message.newQuery(null) should throw NullPointerException");
    }

    /**
     * Ensures that the created message is independent of later modifications to the original
     * {@link Record} (the record is immutable in the DNS library, but this test guards against
     * accidental coupling).
     */
    @Test
    void newQuery_messageIsIndependentFromOriginalRecord() {
        Name qname = Name.fromConstantString("host.example.");
        Record original = Record.newRecord(qname, Type.AAAA, DClass.IN);
        Message query = Message.newQuery(original);
        // The library's Record objects are immutable, but we can still verify that the
        // message holds a reference that equals the original.
        assertEquals(original, query.getQuestion(), "Message should contain a question equal to the original record");
    }

    /**
     * Parameterized test that creates queries for several different record types and verifies
     * that the resulting message always has the correct header fields, question placement and
     * empty non‑question sections.
     *
     * @param typeName a human‑readable name for the record type (used only for test reporting)
     * @param record   the Record instance to be used as the query question
     */
    @ParameterizedTest(name = "newQuery with {0} record")
    @MethodSource("provideRecordTypes")
    void newQuery_variousRecordTypes_createsCorrectMessage(String typeName, Record record) {
        // Act: create a query for the supplied record
        Message query = Message.newQuery(record);
        // Header must be a standard query with recursion desired
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(), "Header opcode should be QUERY for " + typeName + " record");
        assertTrue(query.getHeader().getFlag(Flags.RD), "RD flag should be set for " + typeName + " record");
        // The question must be exactly the supplied record
        assertEquals(record, query.getQuestion(), "Question record should match the supplied " + typeName + " record");
        assertEquals(record, query.getSection(Section.QUESTION).get(0), "QUESTION section should contain the supplied " + typeName + " record");
        // Header count for QUESTION must be 1
        assertEquals(1, query.getHeader().getCount(Section.QUESTION), "Header count for QUESTION should be 1 for " + typeName + " record");
        // All other sections must be empty and have a count of zero
        for (int sec = Section.ANSWER; sec <= Section.ADDITIONAL; sec++) {
            assertTrue(query.getSection(sec).isEmpty(), "Section " + sec + " should be empty for " + typeName + " record");
            assertEquals(0, query.getHeader().getCount(sec), "Header count for section " + sec + " should be zero for " + typeName + " record");
        }
    }

    /**
     * A‑record query
     */
    @Test
    void newQuery_ARecord_createsCorrectMessage() {
        Name name = Name.fromConstantString("example.com.");
        Record a = Record.newRecord(name, Type.A, DClass.IN);
        Message query = Message.newQuery(a);
        assertStandardQuery(query, a, "A");
    }

    /**
     * AAAA‑record query
     */
    @Test
    void newQuery_AAAARecord_createsCorrectMessage() {
        Name name = Name.fromConstantString("example.com.");
        Record aaaa = Record.newRecord(name, Type.AAAA, DClass.IN);
        Message query = Message.newQuery(aaaa);
        assertStandardQuery(query, aaaa, "AAAA");
    }

    /**
     * MX‑record query
     */
    @Test
    void newQuery_MXRecord_createsCorrectMessage() {
        Name name = Name.fromConstantString("example.com.");
        Record mx = Record.newRecord(name, Type.MX, DClass.IN);
        Message query = Message.newQuery(mx);
        assertStandardQuery(query, mx, "MX");
    }
}
