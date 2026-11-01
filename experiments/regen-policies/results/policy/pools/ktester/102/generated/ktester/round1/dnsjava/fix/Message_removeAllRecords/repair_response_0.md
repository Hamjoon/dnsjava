```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xbill.DNS.TextParseException;   // <-- added import

@ExtendWith(MockitoExtension.class)
class Message_removeAllRecords_Test {

    private Message message;

    private static final Name TEST_NAME = Name.fromConstantString("example.com.");

    /**
     * Populate a fresh {@link Message} with one record in each of the four sections.
     */
    @BeforeEach
    void setupBeforeEach() throws UnknownHostException, TextParseException { // <-- added TextParseException
        // uses a random ID, fine for our purposes
        message = new Message();
        // QUESTION section
        Record q = Record.newRecord(TEST_NAME, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        // ANSWER section
        Record a = new ARecord(TEST_NAME, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        message.addRecord(a, Section.ANSWER);
        // AUTHORITY section
        Record ns = Record.newRecord(TEST_NAME, Type.NS, DClass.IN);
        message.addRecord(ns, Section.AUTHORITY);
        // ADDITIONAL section (use a simple TXT record)
        Record txt = new TXTRecord(TEST_NAME, DClass.IN, 0, "test");
        message.addRecord(txt, Section.ADDITIONAL);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up.
        message = null;
    }

    /* -------------------------------------------------------------
     * Helper to inspect the private {@code sections} array via reflection.
     * ------------------------------------------------------------- */
    private List<Record> getInternalSectionArray(int section) throws ReflectiveOperationException {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] arr = (List<Record>[]) f.get(message);
        return arr[section];
    }

    /**
     * Data provider for valid sections – both non‑empty and already empty.
     */
    private static Stream<Arguments> provideValidSectionScenarios() {
        return Stream.of(
            // Non‑empty sections (populated by @BeforeEach)
            Arguments.of(Section.ANSWER, false,
                "Remove records from a section that initially contains a non‑empty list of records."),
            // Already empty sections – we will clear them first in the test body
            Arguments.of(Section.ADDITIONAL, true,
                "Remove records from a section that is already empty (null) and has a zero count."));
    }

    /**
     * Data provider for invalid negative section index.
     */
    private static Stream<Arguments> provideNegativeInvalidSections() {
        return Stream.of(
            Arguments.of(-1, "Negative index should be rejected."),
            Arguments.of(-42, "Another negative index."));
    }

    /**
     * Data provider for out‑of‑range section indices (>= Section.count).
     */
    private static Stream<Arguments> provideOutOfRangeInvalidSections() {
        // Section constants are 0‑3; 4 is the first invalid value.
        return Stream.of(
            Arguments.of(4, "Index equal to number of sections should be invalid."),
            Arguments.of(5, "Index greater than number of sections should be invalid."));
    }

    @Test
    void removeAllRecords_clearsSpecifiedSection_andLeavesOthersIntact() throws Exception {
        // Verify pre‑condition: each section contains exactly one record
        assertEquals(1, message.getSection(Section.QUESTION).size());
        assertEquals(1, message.getSection(Section.ANSWER).size());
        assertEquals(1, message.getSection(Section.AUTHORITY).size());
        assertEquals(1, message.getSection(Section.ADDITIONAL).size());
        // Remove ANSWER section
        message.removeAllRecords(Section.ANSWER);
        // ANSWER section must be empty and header count zero
        List<Record> answer = message.getSection(Section.ANSWER);
        assertTrue(answer.isEmpty(), "ANSWER section should be empty after removal");
        assertEquals(0, message.getHeader().getCount(Section.ANSWER));
        // internal array entry must be null
        assertNull(getInternalSectionArray(Section.ANSWER), "Internal sections[ANSWER] should be null after removal");
        // Other sections must stay unchanged
        assertEquals(1, message.getSection(Section.QUESTION).size());
        assertEquals(1, message.getSection(Section.AUTHORITY).size());
        assertEquals(1, message.getSection(Section.ADDITIONAL).size());
    }

    @Test
    void removeAllRecords_isIdempotent() throws Exception {
        // First removal
        message.removeAllRecords(Section.AUTHORITY);
        assertTrue(message.getSection(Section.AUTHORITY).isEmpty());
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
        assertNull(getInternalSectionArray(Section.AUTHORITY));
        // Second removal – should not throw and state stays the same
        message.removeAllRecords(Section.AUTHORITY);
        assertTrue(message.getSection(Section.AUTHORITY).isEmpty(),
            "Second call should keep AUTHORITY section empty");
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
        assertNull(getInternalSectionArray(Section.AUTHORITY));
    }

    @Test
    void removeAllRecords_invalidSection_throwsException() {
        // Section indices are 0‑3; 4 is invalid and should cause IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> message.removeAllRecords(4));
        // Negative index also invalid
        assertThrows(IllegalArgumentException.class, () -> message.removeAllRecords(-1));
    }

    @Test
    void removeAllRecords_allSectionsSequentially() throws Exception {
        // Remove each section one after another and verify the cumulative effect
        for (int sec = 0; sec < 4; sec++) {
            message.removeAllRecords(sec);
            // The just‑removed section must be empty
            assertTrue(message.getSection(sec).isEmpty(),
                "Section " + sec + " should be empty after removal");
            assertEquals(0, message.getHeader().getCount(sec),
                "Header count for section " + sec + " should be zero");
            assertNull(getInternalSectionArray(sec));
        }
        // After all removals, the message should contain no records at all
        for (int sec = 0; sec < 4; sec++) {
            assertTrue(message.getSection(sec).isEmpty(),
                "All sections should be empty after removing all");
        }
    }

    @ParameterizedTest(name = "[{index}] removeAllRecords on section {0} (alreadyEmpty={1})")
    @MethodSource("provideValidSectionScenarios")
    void removeAllRecords_validSections(int section, boolean alreadyEmpty, String description) throws Exception {
        // If the scenario requires an already‑empty section, clear it first.
        if (alreadyEmpty) {
            // Ensure the internal array entry is null and header count is zero.
            message.removeAllRecords(section);
            assertTrue(message.getSection(section).isEmpty(),
                "Pre‑condition: section should be empty before the actual test.");
            assertEquals(0, message.getHeader().getCount(section));
            assertNull(getInternalSectionArray(section));
        } else {
            // Verify pre‑condition: the section contains exactly one record.
            assertEquals(1, message.getSection(section).size(),
                "Pre‑condition: section should contain one record.");
            assertNotEquals(0, message.getHeader().getCount(section));
        }
        // Act
        message.removeAllRecords(section);
        // Assert: internal array entry must be null and header count zero.
        assertNull(getInternalSectionArray(section),
            "Internal sections[" + section + "] must be null after removal.");
        assertEquals(0, message.getHeader().getCount(section),
            "Header count for section " + section + " must be zero after removal.");
        assertTrue(message.getSection(section).isEmpty(),
            "Public API getSection must return an empty list after removal.");
    }

    @ParameterizedTest(name = "[{index}] removeAllRecords with invalid negative section {0}")
    @MethodSource("provideNegativeInvalidSections")
    void removeAllRecords_invalidSection_negative(int invalidSection, String description) {
        assertThrows(IllegalArgumentException.class,
            () -> message.removeAllRecords(invalidSection),
            "Expected IllegalArgumentException for section " + invalidSection);
    }

    @ParameterizedTest(name = "[{index}] removeAllRecords with out‑of‑range section {0}")
    @MethodSource("provideOutOfRangeInvalidSections")
    void removeAllRecords_invalidSection_outOfRange(int invalidSection, String description) {
        assertThrows(IllegalArgumentException.class,
            () -> message.removeAllRecords(invalidSection),
            "Expected IllegalArgumentException for out‑of‑range section " + invalidSection);
    }

    /**
     * Idempotent test for multiple calls on the same section (ADDITIONAL).
     */
    @Test
    void removeAllRecords_idempotent_multipleCalls() throws Exception {
        int sec = Section.ADDITIONAL;
        // First call – should clear the section.
        message.removeAllRecords(sec);
        assertNull(getInternalSectionArray(sec));
        assertEquals(0, message.getHeader().getCount(sec));
        assertTrue(message.getSection(sec).isEmpty());
        // Second call – state must stay the same and no exception should be thrown.
        message.removeAllRecords(sec);
        assertNull(getInternalSectionArray(sec));
        assertEquals(0, message.getHeader().getCount(sec));
        assertTrue(message.getSection(sec).isEmpty());
    }
}
```