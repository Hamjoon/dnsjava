```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#removeAllRecords(int)}.
 *
 * <p>The test suite uses JUnit 5 (Jupiter) and Mockito for possible future extensions.
 * The {@code @BeforeEach} method creates a fresh {@link Message} instance populated
 * with a single record in each DNS section. The parameterized tests then verify
 * correct behaviour for valid sections (both non‑empty and already empty) as well
 * as for invalid section indices.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_removeAllRecords_Test {

    private Message message;

    private static final Name TEST_NAME = Name.fromConstantString("example.com.");

    /**
     * Populate a fresh {@link Message} with one record in each of the four sections.
     */
    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
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
        java.lang.reflect.Field f = Message.class.getDeclaredField("sections");
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
                        "Remove records from a section that is already empty (null) and has a zero count.")
        );
    }

    /**
     * Data provider for invalid negative section index.
     */
    private static Stream<Arguments> provideNegativeInvalidSections() {
        return Stream.of(
                Arguments.of(-1, "Negative index should be rejected."),
                Arguments.of(-42, "Another negative index.")
        );
    }

    /**
     * Data provider for out‑of‑range section indices (>= Section.count).
     */
    private static Stream<Arguments> provideOutOfRangeInvalidSections() {
        // Section constants are 0‑3; 4 is the first invalid value.
        return Stream.of(
                Arguments.of(4, "Index equal to number of sections should be invalid."),
                Arguments.of(5, "Index greater than number of sections should be invalid.")
        );
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
}
```