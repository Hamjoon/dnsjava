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
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
        static Stream<Arguments> normalRecordProvider() throws UnknownHostException {
            return Stream.of(
                Arguments.of(
                    new ARecord(
                        Name.fromString("example.com."),
                        DClass.IN,
                        3600,
                        InetAddress.getByName("93.184.216.34")
                    )
                ),
                Arguments.of(
                    new TXTRecord(
                        Name.fromString("test.example."),
                        DClass.IN,
                        300,
                        "sample text"
                    )
                ),
                Arguments.of(
                    new MXRecord(
                        Name.fromString("mail.example."),
                        DClass.IN,
                        600,
                        10,
                        Name.fromString("mx.example.")
                    )
                )
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
            assertNotNull(query.getQuestion(),
                    "Message.getQuestion() should not be null");
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
        assertNotNull(query.getQuestion(),
                "Message.getQuestion() should not be null");
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
        assertThrows(NullPointerException.class,
                () -> Message.newQuery(null),
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

    @Test
    @DisplayName("Record with null name causes NullPointerException")
    void newQuery_recordWithNullName_throwsNPE() throws UnknownHostException {
        // Construction of a Record with a null name itself throws NPE in DNSJava,
        // but we assert that newQuery does not mask the problem.
        assertThrows(NullPointerException.class, () -> {
            Record bad = new ARecord(null, DClass.IN, 3600,
                    InetAddress.getByName("93.184.216.34"));
            Message.newQuery(bad);
        });
    }

    /* --------------------------------------------------------------
     *  Header verification independent of record type
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("header always initialised correctly irrespective of record type")
    void newQuery_headerInitializationVerification() {
        Record txt = new TXTRecord(Name.fromString("test.example."), DClass.IN,
                300, "sample text");
        Message query = Message.newQuery(txt);
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                "Header opcode must be QUERY");
        assertTrue(query.getHeader().getFlag(Flags.RD),
                "Header must have RD flag set");
    }

    /* --------------------------------------------------------------
     *  Independence of multiple invocations
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("multiple invocations produce independent Message instances")
    void newQuery_multipleInvocationsProduceIndependentMessages() {
        Record mx = new MXRecord(Name.fromString("mail.example."), DClass.IN,
                600, 10, Name.fromString("mx.example."));
        Message first = Message.newQuery(mx);
        Message second = Message.newQuery(mx);
        // Different object references – no shared mutable state
        assertNotSame(first, second,
                "Each call should return a distinct Message instance");
        // Header settings must be identical
        assertEquals(first.getHeader().getOpcode(),
                second.getHeader().getOpcode(),
                "Opcode should be identical");
        assertEquals(first.getHeader().getFlag(Flags.RD),
                second.getHeader().getFlag(Flags.RD),
                "RD flag should be identical");
        // QUESTION sections must contain equal records but be separate lists
        assertEquals(first.getSection(Section.QUESTION),
                second.getSection(Section.QUESTION),
                "QUESTION sections should contain the same record");
        assertNotSame(first.getSection(Section.QUESTION),
                second.getSection(Section.QUESTION),
                "QUESTION section lists must be distinct objects");
    }

    /* --------------------------------------------------------------
     *  Unsupported record types in QUESTION section
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("OPTRecord is not allowed in the QUESTION section")
    void newQuery_optRecord_throwsIllegalArgumentException() {
        // OPTRecord is used for EDNS options and must not appear in QUESTION
        Record opt = new OPTRecord(4096, 0, 0, 0);
        assertThrows(IllegalArgumentException.class,
                () -> Message.newQuery(opt),
                "Adding an OPTRecord to the QUESTION section should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("TSIGRecord is not allowed in the QUESTION section")
    void newQuery_tsigRecord_throwsIllegalArgumentException() {
        // Construct a minimal TSIGRecord (parameters chosen arbitrarily)
        Record tsig = new TSIGRecord(
                Name.fromString("example.com."),
                DClass.ANY,
                0L,
                Name.fromString("hmac-md5.sig-alg.reg.int."),
                new Date(),          // timeSigned
                0,                   // fudge
                new byte[]{0x01, 0x02}, // mac
                0,                   // originalID
                0,                   // error
                new byte[]{}         // otherData
        );
        assertThrows(IllegalArgumentException.class,
                () -> Message.newQuery(tsig),
                "Adding a TSIGRecord to the QUESTION section should throw IllegalArgumentException");
    }
}
```