package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.InetAddress;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_sectionToString_Test {

    /**
     * Message instance used by the tests.
     */
    private Message msg;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh, empty message for each test.
        msg = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required.
        msg = null;
    }

    private static Stream<Arguments> emptySectionProvider() {
        return Stream.of(// 0
        Arguments.of(Section.QUESTION), // 1
        Arguments.of(Section.ANSWER), // 2
        Arguments.of(Section.AUTHORITY), // 3
        Arguments.of(Section.ADDITIONAL));
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
        // the exact format may vary; we look for key parts
        String optExpected = "OPT\t4096";
        return Stream.of(Arguments.of(Section.ANSWER, aRecord, aExpected), Arguments.of(Section.AUTHORITY, mxRecord, mxExpected), Arguments.of(Section.ADDITIONAL, optRecord, optExpected));
    }

    @Test
    void sectionToString_invalidSection_returnsNull() {
        // Section indices larger than 3 are considered invalid.
        assertNull(msg.sectionToString(4), "sectionToString should return null for an invalid index");
        assertNull(msg.sectionToString(99), "sectionToString should return null for an invalid index");
        assertNull(msg.sectionToString(-1), "sectionToString should return null for a negative index");
    }

    @Test
    void sectionToString_validEmptySections_returnEmptyString() {
        // All sections are empty right after construction.
        for (int i = 0; i <= 3; i++) {
            String result = msg.sectionToString(i);
            assertNotNull(result, "Result must not be null for a valid section");
            assertEquals("", result, "Empty section should produce an empty string");
        }
    }

    @Test
    void sectionToString_sectionWithOneRecord_containsRecordString() {
        // Prepare a TXT record and add it to the ANSWER section.
        Name name = Name.fromConstantString("example.com.");
        TXTRecord txt = new TXTRecord(name, DClass.IN, 3600, "sample text");
        msg.addRecord(txt, Section.ANSWER);
        String result = msg.sectionToString(Section.ANSWER);
        assertNotNull(result, "Result must not be null");
        assertFalse(result.isEmpty(), "Result should not be empty when the section has records");
        // The Record implementation provides a useful toString; we expect it to appear.
        assertTrue(result.contains(txt.toString()), "The string representation of the added record must be present");
    }

    @Test
    void sectionToString_multipleSectionsContainCorrectContent() {
        // QUESTION section
        Name qName = Name.fromConstantString("www.example.org.");
        Record question = Record.newRecord(qName, Type.A, DClass.IN);
        msg.addRecord(question, Section.QUESTION);
        // ANSWER section
        Name aName = Name.fromConstantString("www.example.org.");
        ARecord aRec = new ARecord(aName, DClass.IN, 300, java.net.InetAddress.getLoopbackAddress());
        msg.addRecord(aRec, Section.ANSWER);
        // AUTHORITY section
        Name authName = Name.fromConstantString("example.org.");
        NSRecord ns = new NSRecord(authName, DClass.IN, 300, Name.fromConstantString("ns1.example.org."));
        msg.addRecord(ns, Section.AUTHORITY);
        // ADDITIONAL section
        Name addName = Name.fromConstantString("ns1.example.org.");
        ARecord nsA = new ARecord(addName, DClass.IN, 300, java.net.InetAddress.getLoopbackAddress());
        msg.addRecord(nsA, Section.ADDITIONAL);
        // Verify each section's string contains its own records and nothing from others.
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

    /* --------------------------------------------------------------------- */
    @Test
    void sectionToString_negativeSection_throwsIllegalArgumentException() {
        // The private helper validates the range and throws IllegalArgumentException
        // for values < 0.
        assertThrows(IllegalArgumentException.class, () -> msg.sectionToString(-1), "Negative section index should cause IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> msg.sectionToString(-100), "Large negative index should also cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "sectionToString_emptySection_{0}_returnsEmptyString")
    @MethodSource("emptySectionProvider")
    void sectionToString_emptySections_returnEmptyString(int section) {
        // All sections are empty right after construction.
        String result = msg.sectionToString(section);
        assertNotNull(result, "Result must not be null for a valid section");
        assertEquals("", result, "Empty section should produce an empty string");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "sectionToString_section_{0}_containsExpectedRecord")
    @MethodSource("recordSectionProvider")
    void sectionToString_withRecords_containsExpectedString(int section, Record record, String expectedFragment) throws Exception {
        // Add the supplied record to the requested section.
        msg.addRecord(record, section);
        String result = msg.sectionToString(section);
        assertNotNull(result, "Result must not be null when a record is present");
        assertFalse(result.isEmpty(), "Result should not be empty for a non‑empty section");
        assertTrue(result.contains(expectedFragment), () -> "Result should contain the expected fragment: " + expectedFragment);
    }
}
