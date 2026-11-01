```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#sectionToString(int)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Behaviour when an invalid section index is supplied.</li>
 *   <li>Behaviour when a valid but empty section is requested.</li>
 *   <li>Behaviour when a section contains records – the returned string must contain the
 *       textual representation of those records.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_sectionToString_Test {

    /** Message instance used by the tests. */
    private Message msg;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

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

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
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
        assertTrue(result.contains(txt.toString()),
                "The string representation of the added record must be present");
    }

    @Test
    void sectionToString_multipleSectionsContainCorrectContent() {
        // QUESTION section
        Name qName = Name.fromConstantString("www.example.org.");
        Record question = Record.newRecord(qName, Type.A, DClass.IN);
        msg.addRecord(question, Section.QUESTION);

        // ANSWER section
        Name aName = Name.fromConstantString("www.example.org.");
        ARecord aRec = new ARecord(aName, DClass.IN, 300, 
                                   java.net.InetAddress.getLoopbackAddress());
        msg.addRecord(aRec, Section.ANSWER);

        // AUTHORITY section
        Name authName = Name.fromConstantString("example.org.");
        NSRecord ns = new NSRecord(authName, DClass.IN, 300, Name.fromConstantString("ns1.example.org."));
        msg.addRecord(ns, Section.AUTHORITY);

        // ADDITIONAL section
        Name addName = Name.fromConstantString("ns1.example.org.");
        ARecord nsA = new ARecord(addName, DClass.IN, 300,
                                  java.net.InetAddress.getLoopbackAddress());
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
}
```