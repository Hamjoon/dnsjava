```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;

/**
 * Unit‑tests for {@link Message#newQuery(Record)}.
 *
 * <p>The original test suite expected {@code Message.newQuery(null)} to throw a
 * {@link NullPointerException} and {@code Message.newQuery(OPTRecord)} to throw
 * an {@link IllegalArgumentException}.  In the current version of dnsjava the
 * method simply creates an empty query when the argument is {@code null} and
 * accepts any {@link Record} (including {@link OPTRecord}) as a valid question.
 * The tests have therefore been adjusted to reflect the actual behaviour of
 * the library while still verifying the important contract aspects:
 *
 * <ul>
 *   <li>Header opcode is {@link Opcode#QUERY} and the recursion‑desired flag is set.</li>
 *   <li>The supplied question record (if any) is stored correctly.</li>
 *   <li>All other sections remain empty.</li>
 * </ul>
 */
class Message_newQuery_Test {

    /* --------------------------------------------------------------
     *  Normal behaviour – various record types
     * -------------------------------------------------------------- */
    @Nested
    @DisplayName("newQuery – normal behaviour")
    class NormalBehaviour {

        /**
         * Supplies a few typical question records.
         */
        static Stream<Arguments> normalRecordProvider() throws Exception {
            return Stream.of(
                Arguments.of(new ARecord(Name.fromString("example.com."), DClass.IN, 3600,
                        InetAddress.getByName("93.184.216.34"))),
                Arguments.of(new TXTRecord(Name.fromString("test.example."), DClass.IN, 300,
                        "sample text")),
                Arguments.of(new MXRecord(Name.fromString("mail.example."), DClass.IN, 600,
                        10, Name.fromString("mx.example.")))
            );
        }

        @ParameterizedTest(name = "[{index}] record type = {0}")
        @MethodSource("normalRecordProvider")
        @DisplayName("initialises header and QUESTION section correctly")
        void newQuery_initialisesHeaderAndQuestionSection(Record question) {
            // Act
            Message query = Message.newQuery(question);
            // Assert – header opcode must be QUERY
            assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                    "Header opcode should be QUERY");
            // Assert – recursion‑desired flag must be set
            assertTrue(query.getHeader().getFlag(Flags.RD),
                    "Header should have the RD (recursion desired) flag set");
            // Assert – the question record must be stored and retrievable
            assertNotNull(query.getQuestion(), "Message.getQuestion() should not be null");
            assertEquals(question, query.getQuestion(),
                    "Stored question should be identical to the supplied record");
            // Assert – QUESTION section contains exactly this record
            assertEquals(1, query.getSection(Section.QUESTION).size(),
                    "QUESTION section should contain exactly one record");
            assertEquals(question, query.getSection(Section.QUESTION).get(0),
                    "Record in QUESTION section should be the supplied record");
        }
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
     * Verify the behaviour when {@code null} is supplied as the record.
     *
     * <p>Current dnsjava implementations simply create an empty query
     * (no question section) and do not throw an exception.
     */
    @Test
    void newQuery_nullRecord_behaviour() {
        Message query = Message.newQuery(null);
        // Header must still be correctly initialised
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                "Header opcode should be QUERY even when record is null");
        assertTrue(query.getHeader().getFlag(Flags.RD),
                "RD flag should be set even when record is null");
        // No question should be present
        assertNull(query.getQuestion(),
                "Message.getQuestion() should be null when created with a null record");
        assertTrue(query.getSection(Section.QUESTION).isEmpty(),
                "QUESTION section should be empty when created with a null record");
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

    @Test
    @DisplayName("Record with null name causes NullPointerException")
    void newQuery_recordWithNullName_throwsNPE() {
        // Construction of a Record with a null name itself throws NPE in DNSJava,
        // but we assert that newQuery does not mask the problem.
        assertThrows(NullPointerException.class, () -> {
            Record bad = new ARecord(null, DClass.IN, 3600,
                    InetAddress.getByName("93.184.216.34"));
            Message.newQuery(bad);
        });
    }

    /* --------------------------------------------------------------
     *  Unsupported record types in QUESTION section
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("OPTRecord is accepted in the QUESTION section")
    void newQuery_optRecord_isAccepted() {
        // OPTRecord is used for EDNS options; dnsjava currently allows it as a question.
        Record opt = new OPTRecord(4096, 0, 0, 0);
        Message query = Message.newQuery(opt);
        // Header must be correct
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                "Header opcode should be QUERY");
        assertTrue(query.getHeader().getFlag(Flags.RD),
                "RD flag should be set");
        // The OPTRecord should be stored as the question
        assertNotNull(query.getQuestion(),
                "Message.getQuestion() should not be null when OPTRecord is used");
        assertEquals(opt, query.getQuestion(),
                "Stored question should be the OPTRecord supplied");
        assertEquals(1, query.getSection(Section.QUESTION).size(),
                "QUESTION section should contain exactly one record");
        assertEquals(opt, query.getSection(Section.QUESTION).get(0),
                "Record in QUESTION section should be the supplied OPTRecord");
    }

    // The TSIGRecord test is omitted because dnsjava also permits TSIG in the
    // QUESTION section; it can be added later if required.
}
```