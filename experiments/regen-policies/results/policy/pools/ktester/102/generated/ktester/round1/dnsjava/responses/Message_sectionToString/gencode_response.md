```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link Message#sectionToString(int)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>Valid sections with a single record (question, answer, authority, additional).</li>
 *   <li>Empty sections – the method must return an empty string.</li>
 *   <li>Invalid section indexes – an {@link IllegalArgumentException} must be thrown.</li>
 *   <li>Multiple records in the same section, preserving insertion order.</li>
 *   <li>Different DNS record types in one section.</li>
 *   <li>Stress test with a large number of records.</li>
 *   <li>Immutability of the returned string (snapshot semantics).</li>
 * </ul>
 *
 * <p>All tests use the real {@link Message} implementation; no mocking of internal
 * dependencies is required. The {@code @BeforeEach} method creates a fresh
 * {@link Message} instance for each test case.
 */
@ExtendWith(MockitoExtension.class)
class Message_sectionToString_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for every test.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    /* --------------------------------------------------------------------- */
    /*  Existing basic tests (kept unchanged)                               */
    /* --------------------------------------------------------------------- */

    @Test
    void sectionToString_returnsEmptyStringForEmptySections() {
        for (int sec = Section.QUESTION; sec <= Section.ADDITIONAL; sec++) {
            String result = message.sectionToString(sec);
            assertNotNull(result, "Result must not be null for section " + sec);
            assertTrue(result.isEmpty(),
                    "Expected empty string for empty section " + sec + " but got: \"" + result + "\"");
        }
    }

    @Test
    void sectionToString_includesRecordStringRepresentation() throws UnknownHostException {
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);

        Record answer = new ARecord(qname, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        message.addRecord(answer, Section.ANSWER);

        String questionStr = message.sectionToString(Section.QUESTION);
        assertTrue(questionStr.contains(qname.toString()));
        assertTrue(questionStr.contains("A"));

        String answerStr = message.sectionToString(Section.ANSWER);
        assertTrue(answerStr.contains(qname.toString()));
        assertTrue(answerStr.contains("A"));
        assertTrue(answerStr.contains("127.0.0.1"));
    }

    @Test
    void sectionToString_throwsExceptionForInvalidSection() {
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(4));
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(-1));
    }

    @Test
    void sectionToString_handlesMultipleRecordsInSameSection() throws IOException {
        Name name = Name.fromConstantString("example.com.");
        Record txt1 = new TXTRecord(name, DClass.IN, 0, "first");
        Record txt2 = new TXTRecord(name, DClass.IN, 0, "second");
        message.addRecord(txt1, Section.ADDITIONAL);
        message.addRecord(txt2, Section.ADDITIONAL);

        String additionalStr = message.sectionToString(Section.ADDITIONAL);
        assertTrue(additionalStr.contains("first"));
        assertTrue(additionalStr.contains("second"));
        int occurrences = additionalStr.split("TXT").length - 1;
        assertEquals(2, occurrences);
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterized tests for valid sections (single record)                */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: section={0}")
    @MethodSource("validSectionSingleRecordProvider")
    void sectionToString_validSections_singleRecord(int section, Record record, String expectedSnippet) {
        // Add the record to the requested section.
        message.addRecord(record, section);

        String result = message.sectionToString(section);
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Result should not be empty for section " + section);
        // Verify that the result contains a fragment that uniquely identifies the record.
        assertTrue(result.contains(expectedSnippet),
                () -> "Result for section " + section + " should contain \"" + expectedSnippet + "\" but was: \"" + result + "\"");
    }

    private static Stream<Arguments> validSectionSingleRecordProvider() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Record answer = new ARecord(name, DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        Record authority = new NSRecord(name, DClass.IN, 300, Name.fromConstantString("ns1.example.com."));
        Record additional = new ARecord(Name.fromConstantString("ns1.example.com."), DClass.IN, 300,
                InetAddress.getByName("5.6.7.8"));

        return Stream.of(
                Arguments.of(Section.QUESTION, question, "example.com."),
                Arguments.of(Section.ANSWER, answer, "1.2.3.4"),
                Arguments.of(Section.AUTHORITY, authority, "ns1.example.com."),
                Arguments.of(Section.ADDITIONAL, additional, "5.6.7.8")
        );
    }

    /* --------------------------------------------------------------------- */
    /*  Invalid section indexes (parameterized)                              */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: invalidSection={0}")
    @MethodSource("invalidSectionProvider")
    void sectionToString_invalidSection_throws(int invalidSection) {
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(invalidSection),
                "Expected IllegalArgumentException for section " + invalidSection);
    }

    private static Stream<Integer> invalidSectionProvider() {
        return Stream.of(-1, 4, -2147483648, 2147483647, 5, 100);
    }

    /* --------------------------------------------------------------------- */
    /*  Multiple records in the same section, preserving order               */
    /* --------------------------------------------------------------------- */

    @Test
    void sectionToString_multipleRecords_preserveInsertionOrder() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = new ARecord(name, DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        Record aaaaRecord = new AAAARecord(name, DClass.IN, 300, InetAddress.getByName("2001:db8::1"));
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(aaaaRecord, Section.ANSWER);

        String result = message.sectionToString(Section.ANSWER);
        assertAll(
                () -> assertTrue(result.contains("1.2.3.4"), "Result must contain the A record address"),
                () -> assertTrue(result.contains("2001:db8::1"), "Result must contain the AAAA record address")
        );

        // Verify order: A record should appear before AAAA record.
        int indexA = result.indexOf("1.2.3.4");
        int indexAAAA = result.indexOf("2001:db8::1");
        assertTrue(indexA < indexAAAA,
                "A record should appear before AAAA record in the string representation");
    }

    /* --------------------------------------------------------------------- */
    /*  Different record types in one section                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void sectionToString_variousRecordTypes_inAuthoritySection() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record mx = new MXRecord(name, DClass.IN, 3600, 10,
                Name.fromConstantString("mail.example.com."));
        Record txt = new TXTRecord(name, DClass.IN, 3600, "v=spf1 -all");
        message.addRecord(mx, Section.AUTHORITY);
        message.addRecord(txt, Section.AUTHORITY);

        String result = message.sectionToString(Section.AUTHORITY);
        assertAll(
                () -> assertTrue(result.contains("MX"), "Result must contain MX keyword"),
                () -> assertTrue(result.contains("mail.example.com."), "Result must contain MX target"),
                () -> assertTrue(result.contains("TXT"), "Result must contain TXT keyword"),
                () -> assertTrue(result.contains("v=spf1 -all"), "Result must contain TXT data")
        );
    }

    /* --------------------------------------------------------------------- */
    /*  Large section stress test (100 A records)                            */
    /* --------------------------------------------------------------------- */

    @Test
    void sectionToString_largeSection_handlesHundredRecords() throws UnknownHostException {
        Name base = Name.fromConstantString("host.example.com.");
        List<Record> records = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String ip = "10.0." + (i / 256) + "." + (i % 256);
            Record a = new ARecord(base, DClass.IN, 0, InetAddress.getByName(ip));
            records.add(a);
            message.addRecord(a, Section.ADDITIONAL);
        }

        String result = message.sectionToString(Section.ADDITIONAL);
        // Expect 100 lines → 99 newline characters.
        long newlineCount = result.chars().filter(ch -> ch == '\n').count();
        assertEquals(99, newlineCount, "There should be 99 newline characters for 100 records");

        // Spot‑check a few records to ensure they are present.
        assertTrue(result.contains("10.0.0.0"));
        assertTrue(result.contains("10.0.0.99"));
        assertTrue(result.contains("10.0.0.255"));
    }

    /* --------------------------------------------------------------------- */
    /*  Immutability – snapshot semantics of the returned string              */
    /* --------------------------------------------------------------------- */

    @Test
    void sectionToString_immutability_snapshotBehavior() throws UnknownHostException {
        Name qname = Name.fromConstantString("example.com.");
        Record q1 = Record.newRecord(qname, Type.A, DClass.IN);
        message.addRecord(q1, Section.QUESTION);

        String snapshot = message.sectionToString(Section.QUESTION);
        assertTrue(snapshot.contains("example.com."), "Snapshot must contain the first question record");

        // Add a second question record.
        Record q2 = Record.newRecord(Name.fromConstantString("other.example.com."), Type.A, DClass.IN);
        message.addRecord(q2, Section.QUESTION);

        // The previously captured snapshot must stay unchanged.
        assertFalse(snapshot.contains("other.example.com."),
                "Snapshot should not reflect later modifications to the Message");
        // New call reflects both records.
        String afterAddition = message.sectionToString(Section.QUESTION);
        assertTrue(afterAddition.contains("other.example.com."));
    }
}
```