```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#sectionToString(int)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Empty sections – the method must return an empty string.</li>
 *   <li>Single records – the string must contain the record’s textual form followed by a newline.</li>
 *   <li>Multiple records – order of insertion must be preserved and each record must be on its own line.</li>
 *   <li>Different record types (A, AAAA, CNAME, MX, NS, OPT).</li>
 *   <li>Removal of a record – the section becomes empty afterwards.</li>
 *   <li>Invalid section indices – {@link IllegalArgumentException} must be thrown.</li>
 * </ul>
 *
 * <p>All tests use the public API only; no reflection is required because the
 * focal method is public.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_sectionToString_Test {

    /** Message instance used by the tests. */
    private Message message;

    /* --------------------------------------------------------------------- */
    /*  Lifecycle methods                                                    */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh, empty Message for every test.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the Message instance will be garbage‑collected.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Message.sectionToString(int)                               */
    /* --------------------------------------------------------------------- */

    @Test
    void sectionToString_emptyAnswerSection_returnsEmptyString() {
        // GIVEN an empty message (no records in any section)
        // WHEN we request the string representation of the ANSWER section
        String result = message.sectionToString(Section.ANSWER);
        // THEN the result is an empty string
        assertEquals("", result);
    }

    @Test
    void sectionToString_emptyQuestionSection_returnsEmptyString() {
        // GIVEN a freshly created Message
        // WHEN we request the string representation of the QUESTION section
        String result = message.sectionToString(Section.QUESTION);
        // THEN the result is an empty string
        assertEquals("", result);
    }

    @Test
    void sectionToString_singleARecordInQuestion_containsCorrectString() {
        // GIVEN a message that contains a single A record in the QUESTION section
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN, 0); // TTL = 0 for QUESTION
        message.addRecord(aRecord, Section.QUESTION);

        // WHEN we request the string representation of the QUESTION section
        String result = message.sectionToString(Section.QUESTION);

        // THEN the result contains the textual representation of the record followed by a newline
        String expected = aRecord.toString() + "\n";
        assertEquals(expected, result);
    }

    @Test
    void sectionToString_multipleRecordsInAdditional_preserveOrder() {
        // GIVEN an OPT pseudo‑record and an NS record added to the ADDITIONAL section
        Name optName = Name.fromConstantString("opt.example.com.");
        // OPTRecord does not have a public constructor that takes a name, so we use the
        // generic Record.newRecord to create a pseudo‑record that mimics the expected output.
        Record optRecord = Record.newRecord(optName, Type.OPT, DClass.IN, 0);
        // NS record
        Name nsName = Name.fromConstantString("ns.example.com.");
        Name target = Name.fromConstantString("ns1.example.com.");
        Record nsRecord = Record.newRecord(nsName, Type.NS, DClass.IN, 0);
        // Manually set the target for the NS record (dnsjava provides NSRecord, but we keep it simple)
        // In real code you would use: new NSRecord(nsName, DClass.IN, 0, target);
        // For the purpose of this test we rely on toString() of the generic Record – it will still contain the name and type.
        // Add in the required order
        message.addRecord(optRecord, Section.ADDITIONAL);
        message.addRecord(nsRecord, Section.ADDITIONAL);

        // WHEN we request the string representation of the ADDITIONAL section
        String result = message.sectionToString(Section.ADDITIONAL);

        // THEN both records appear in the same order, each terminated by a newline
        List<String> expectedLines = List.of(optRecord.toString(), nsRecord.toString());
        String[] actualLines = result.split("\n");
        assertEquals(expectedLines.size(), actualLines.length, "Number of lines does not match");
        for (int i = 0; i < expectedLines.size(); i++) {
            assertEquals(expectedLines.get(i), actualLines[i],
                    "Line " + (i + 1) + " does not match");
        }
    }

    @Test
    void sectionToString_multipleARecordsInAnswer_preserveInsertionOrder() {
        // GIVEN two A records added to the ANSWER section
        Name name = Name.fromConstantString("example.com.");
        Record a1 = Record.newRecord(name, Type.A, DClass.IN, 3600);
        Record a2 = Record.newRecord(name, Type.A, DClass.IN, 3600);
        message.addRecord(a1, Section.ANSWER);
        message.addRecord(a2, Section.ANSWER);

        // WHEN we request the string representation of the ANSWER section
        String result = message.sectionToString(Section.ANSWER);

        // THEN both records appear, each on its own line, preserving insertion order
        String expected = a1.toString() + "\n" + a2.toString() + "\n";
        assertEquals(expected, result);
    }

    @Test
    void sectionToString_mxRecordInAuthority_isFormattedCorrectly() {
        // GIVEN an MX record with explicit TTL in the AUTHORITY section
        Name name = Name.fromConstantString("example.com.");
        Name target = Name.fromConstantString("mail.example.com.");
        // MXRecord constructor: MXRecord(Name name, int dclass, long ttl, int preference, Name target)
        MXRecord mx = new MXRecord(name, DClass.IN, 3600, 10, target);
        message.addRecord(mx, Section.AUTHORITY);

        // WHEN we request the string representation of the AUTHORITY section
        String result = message.sectionToString(Section.AUTHORITY);

        // THEN the result contains the MX record string followed by a newline
        String expected = mx.toString() + "\n";
        assertEquals(expected, result);
    }

    @Test
    void sectionToString_singleOPTRecordOnly() {
        // GIVEN only an OPT pseudo‑record in the ADDITIONAL section
        Name optName = Name.fromConstantString("opt.example.com.");
        Record opt = Record.newRecord(optName, Type.OPT, DClass.IN, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        // WHEN we request the string representation of the ADDITIONAL section
        String result = message.sectionToString(Section.ADDITIONAL);

        // THEN the result contains exactly that record followed by a newline
        String expected = opt.toString() + "\n";
        assertEquals(expected, result);
    }

    @Test
    void sectionToString_orderPreservationInAnswer_threeDifferentRecords() {
        // GIVEN A, AAAA and CNAME records added in a specific order to the ANSWER section
        Name name = Name.fromConstantString("example.com.");
        Record a = Record.newRecord(name, Type.A, DClass.IN, 3600);
        Record aaaa = Record.newRecord(name, Type.AAAA, DClass.IN, 3600);
        Record cname = Record.newRecord(name, Type.CNAME, DClass.IN, 3600);
        message.addRecord(a, Section.ANSWER);
        message.addRecord(aaaa, Section.ANSWER);
        message.addRecord(cname, Section.ANSWER);

        // WHEN we request the string representation of the ANSWER section
        String result = message.sectionToString(Section.ANSWER);

        // THEN the output preserves the insertion order, each line terminated by a newline
        String expected = a.toString() + "\n" + aaaa.toString() + "\n" + cname.toString() + "\n";
        assertEquals(expected, result);
    }

    @Test
    void sectionToString_removalEffect_recordIsNoLongerPresent() {
        // GIVEN a record added to the ANSWER section and then removed
        Name name = Name.fromConstantString("example.com.");
        Record a = Record.newRecord(name, Type.A, DClass.IN, 3600);
        message.addRecord(a, Section.ANSWER);
        assertTrue(message.removeRecord(a, Section.ANSWER), "Record should be removed successfully");

        // WHEN we request the string representation of the ANSWER section
        String result = message.sectionToString(Section.ANSWER);

        // THEN the result is an empty string
        assertEquals("", result);
    }

    @Test
    void sectionToString_invalidNegativeSection_throwsIllegalArgumentException() {
        // GIVEN an invalid negative section index
        int invalidSection = -1;
        // WHEN we invoke sectionToString with the invalid index
        // THEN an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(invalidSection));
    }

    @Test
    void sectionToString_invalidOutOfRangeSection_throwsIllegalArgumentException() {
        // GIVEN an out‑of‑range section index (greater than the highest defined)
        int invalidSection = 5;
        // WHEN we invoke sectionToString with the invalid index
        // THEN an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(invalidSection));
    }

    @Test
    void sectionToString_invalidIntegerMaxValue_throwsIllegalArgumentException() {
        // GIVEN Integer.MAX_VALUE as section index
        int invalidSection = Integer.MAX_VALUE;
        // THEN an IllegalArgumentException must be thrown
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(invalidSection));
    }

    @Test
    void sectionToString_invalidIntegerMinValue_throwsIllegalArgumentException() {
        // GIVEN Integer.MIN_VALUE as section index
        int invalidSection = Integer.MIN_VALUE;
        // THEN an IllegalArgumentException must be thrown
        assertThrows(IllegalArgumentException.class,
                () -> message.sectionToString(invalidSection));
    }
}
```