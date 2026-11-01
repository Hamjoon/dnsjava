```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.util.stream.Stream;

/**
 * Unit tests for {@link Message#sectionToString(int)}.
 *
 * <p>The test suite verifies:
 *
 * <ul>
 *   <li>that an out‑of‑range index (>3) returns {@code null};
 *   <li>that a negative index triggers an {@link IllegalArgumentException};
 *   <li>that empty sections return an empty {@link String};
 *   <li>that sections containing records produce a string that includes the textual
 *       representation of those records;
 *   <li>that each section is isolated – records from one section never appear in another.
 * </ul>
 *
 * <p>All tests use real DNS record implementations (A, MX, OPT, etc.) – no mocking is required
 * for the {@code Message} class itself.
 */
@ExtendWith(MockitoExtension.class)
class Message_sectionToString_Test {

    /** Fresh {@link Message} instance for each test. */
    private Message msg;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        msg = new Message(); // empty message
    }

    @AfterEach
    void teardownAfterEach() {
        msg = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /* --------------------------------------------------------------------- */
    /*  Invalid section indices – upper bound (i > 3)                        */
    /* --------------------------------------------------------------------- */
    @Test
    void sectionToString_invalidSection_returnsNull() {
        // Section indices larger than 3 are considered invalid.
        assertNull(msg.sectionToString(4), "sectionToString should return null for an invalid index");
        assertNull(msg.sectionToString(10), "sectionToString should return null for an invalid index");
        assertNull(msg.sectionToString(99), "sectionToString should return null for an invalid index");
    }

    /* --------------------------------------------------------------------- */
    /*  Invalid section indices – negative values                              */
    /* --------------------------------------------------------------------- */
    @Test
    void sectionToString_negativeSection_throwsIllegalArgumentException() {
        // The private helper validates the range and throws IllegalArgumentException
        // for values < 0.
        assertThrows(IllegalArgumentException.class,
                () -> msg.sectionToString(-1),
                "Negative section index should cause IllegalArgumentException");
        assertThrows(IllegalArgumentException.class,
                () -> msg.sectionToString(-100),
                "Large negative index should also cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /*  Empty sections – should produce an empty string                        */
    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "sectionToString_emptySection_{0}_returnsEmptyString")
    @MethodSource("emptySectionProvider")
    void sectionToString_emptySections_returnEmptyString(int section) {
        // All sections are empty right after construction.
        String result = msg.sectionToString(section);
        assertNotNull(result, "Result must not be null for a valid section");
        assertEquals("", result, "Empty section should produce an empty string");
    }

    private static Stream<Arguments> emptySectionProvider() {
        return Stream.of(
                Arguments.of(Section.QUESTION),   // 0
                Arguments.of(Section.ANSWER),    // 1
                Arguments.of(Section.AUTHORITY), // 2
                Arguments.of(Section.ADDITIONAL) // 3
        );
    }

    /* --------------------------------------------------------------------- */
    /*  Sections containing records – verify expected textual fragments       */
    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "sectionToString_section_{0}_containsExpectedRecord")
    @MethodSource("recordSectionProvider")
    void sectionToString_withRecords_containsExpectedString(int section, Record record, String expectedFragment) throws Exception {
        // Add the supplied record to the requested section.
        msg.addRecord(record, section);

        String result = msg.sectionToString(section);
        assertNotNull(result, "Result must not be null when a record is present");
        assertFalse(result.isEmpty(), "Result should not be empty for a non‑empty section");
        assertTrue(result.contains(expectedFragment),
                () -> "Result should contain the expected fragment: " + expectedFragment);
    }

    private static Stream<Arguments> recordSectionProvider() throws Exception {
        // ANSWER section – A record
        Name aName = Name.fromConstantString("example.com.");
        InetAddress aAddr = InetAddress.getByName("1.2.3.4");
        ARecord aRecord = new ARecord(aName, DClass.IN, 3600, aAddr);
        String aExpected = "example.com.\t3600\tIN\tA\t1.2.3.4";

        // AUTHORITY section – MX record
        Name mxName = Name.fromConstantString("example.com.");
        Name mxTarget = Name.fromConstantString("mail.example.com.");
        MXRecord mxRecord = new MXRecord(mxName, DClass.IN, 3600, 10, mxTarget);
        String mxExpected = "example.com.\t3600\tIN\tMX\t10 mail.example.com.";

        // ADDITIONAL section – OPT record (EDNS)
        // OPTRecord payload size is 4096; its toString contains the payload size.
        OPTRecord optRecord = new OPTRecord(4096);
        String optExpected = "OPT\t4096"; // the exact format may vary; we look for key parts

        return Stream.of(
                Arguments.of(Section.ANSWER, aRecord, aExpected),
                Arguments.of(Section.AUTHORITY, mxRecord, mxExpected),
                Arguments.of(Section.ADDITIONAL, optRecord, optExpected)
        );
    }

    /* --------------------------------------------------------------------- */
    /*  Cross‑section isolation – ensure records stay in their own section   */
    /* --------------------------------------------------------------------- */
    @Test
    void sectionToString_multipleSectionsContainCorrectContent() throws Exception {
        // QUESTION section
        Name qName = Name.fromConstantString("www.example.org.");
        Record question = Record.newRecord(qName, Type.A, DClass.IN);
        msg.addRecord(question, Section.QUESTION);

        // ANSWER section
        Name aName = Name.fromConstantString("www.example.org.");
        ARecord aRec = new ARecord(aName, DClass.IN, 300,
                InetAddress.getLoopbackAddress());
        msg.addRecord(aRec, Section.ANSWER);

        // AUTHORITY section
        Name authName = Name.fromConstantString("example.org.");
        NSRecord ns = new NSRecord(authName, DClass.IN, 300,
                Name.fromConstantString("ns1.example.org."));
        msg.addRecord(ns, Section.AUTHORITY);

        // ADDITIONAL section
        Name addName = Name.fromConstantString("ns1.example.org.");
        ARecord nsA = new ARecord(addName, DClass.IN, 300,
                InetAddress.getLoopbackAddress());
        msg.addRecord(nsA, Section.ADDITIONAL);

        // Verify each section's string contains only its own records.
        String qStr = msg.sectionToString(Section.QUESTION);
        assertTrue(qStr.contains(question.toString()), "Question section should contain the question record");
        assertFalse(qStr.contains(aRec.toString()), "Question section must not contain answer records");

        String aStr = msg.sectionToString(Section.ANSWER);
        assertTrue(aStr.contains(aRec.toString()), "Answer section should contain the A record");
        assertFalse(aStr.contains(question.toString()), "Answer section must not contain the question");

        String authStr = msg.sectionToString(Section.AUTHORITY);
        assertTrue(authStr.contains(ns.toString()), "Authority section should contain the NS record");
        assertFalse(authStr.contains(aRec.toString()), "Authority section must not contain answer records");

        String addStr = msg.sectionToString(Section.ADDITIONAL);
        assertTrue(addStr.contains(nsA.toString()), "Additional section should contain the A record for the NS");
        assertFalse(addStr.contains(ns.toString()), "Additional section must not contain authority records");
    }
}
```