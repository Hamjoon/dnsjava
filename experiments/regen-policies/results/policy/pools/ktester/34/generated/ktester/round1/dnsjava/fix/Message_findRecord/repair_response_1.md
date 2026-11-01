```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Tests for {@link Message#findRecord(Record, int)}.
 */
@ExtendWith(MockitoExtension.class)
class Message_findRecord_Test {

    private Message message;

    // Record that will be added to the message
    private Record aRecord;

    // Record that will NOT be added
    private Record differentRecord;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Fresh Message for each test case.
        message = new Message();
        // Build two simple A‑records (type A, class IN) with distinct names.
        Name nameA = Name.fromConstantString("host1.example.com.");
        Name nameB = Name.fromConstantString("host2.example.com.");
        aRecord = Record.newRecord(nameA, Type.A, DClass.IN);
        differentRecord = Record.newRecord(nameB, Type.A, DClass.IN);
    }

    /**
     * Provides arguments for {@link #testFindRecordParameterized(String, Record, int, Object)}.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>a description (used as the test name)</li>
     *   <li>a mocked {@link Record}</li>
     *   <li>the section index to query</li>
     *   <li>the expected outcome – either {@link Boolean} or an {@link Class}
     *       representing the expected exception type</li>
     * </ul>
     */
    private static Stream<Arguments> provideFindRecordCases() {
        return Stream.of(
                // 1. Valid section – empty list (null) → false
                arguments("Valid section – empty list (null) → false", mockRecord(1), Section.ANSWER, false),
                // 2. Valid section – list present but does not contain record → false
                arguments("Valid section – list present but does not contain record → false", mockRecord(2), Section.AUTHORITY, false),
                // 3. Valid section – list contains record → true
                arguments("Valid section – list contains record → true", mockRecord(3), Section.ADDITIONAL, true),
                // 4. Invalid section (negative) → IllegalArgumentException
                arguments("Invalid section (negative) → IllegalArgumentException", mockRecord(4), -1, IllegalArgumentException.class),
                // 5. Invalid section (out of range) → IllegalArgumentException
                arguments("Invalid section (out of range) → IllegalArgumentException", mockRecord(5), 5, IllegalArgumentException.class)
        );
    }

    /**
     * Helper that creates a Mockito mock of {@link Record}. The mock is given a
     * meaningful {@code toString()} implementation to aid debugging.
     *
     * @param id identifier used only for readability
     * @return a mock {@link Record}
     */
    private static Record mockRecord(int id) {
        // Simple mock without unnecessary stubbing; Mockito will generate a sensible
        // default toString, which is sufficient for the test.
        return Mockito.mock(Record.class);
    }

    @Test
    void findRecord_EmptySection_ReturnsFalse() {
        // No records have been added yet; all sections are empty (null).
        boolean result = message.findRecord(aRecord, Section.ANSWER);
        assertFalse(result, "findRecord should return false when the section is empty");
    }

    @Test
    void findRecord_RecordPresent_ReturnsTrue() {
        // Add aRecord to the ANSWER section.
        message.addRecord(aRecord, Section.ANSWER);
        // Now the same record should be found.
        boolean result = message.findRecord(aRecord, Section.ANSWER);
        assertTrue(result, "findRecord should return true when the record exists in the section");
    }

    @Test
    void findRecord_RecordNotPresent_ReturnsFalse() {
        // Add only aRecord to the ANSWER section.
        message.addRecord(aRecord, Section.ANSWER);
        // Query for a different record that was never added.
        boolean result = message.findRecord(differentRecord, Section.ANSWER);
        assertFalse(result, "findRecord should return false when the record is not in the section");
    }

    @Test
    void findRecord_InvalidSection_ThrowsIllegalArgumentException() {
        // Section indices are 0‑3; using 4 should trigger Section.check to fail.
        assertThrows(IllegalArgumentException.class,
                () -> message.findRecord(aRecord, 4),
                "findRecord should throw IllegalArgumentException for an invalid section index");
    }

    /**
     * Parameterised test that executes all scenarios defined in the test cases.
     *
     * @param description human‑readable description of the scenario
     * @param record      the {@link Record} supplied to {@code findRecord}
     * @param section     the section index supplied to {@code findRecord}
     * @param expected    either {@link Boolean} (expected return value) or a
     *                    {@link Class} denoting the expected exception type
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideFindRecordCases")
    void testFindRecordParameterized(String description, Record record, int section, Object expected) {
        // -----------------------------------------------------------------
        // Prepare the Message state according to the scenario description.
        // -----------------------------------------------------------------
        if (section >= Section.QUESTION && section <= Section.ADDITIONAL) {
            if (description.contains("list present but does not contain")) {
                // Add a dummy record that is *not* the one we will query for.
                Record dummy = mockRecord(99);
                message.addRecord(dummy, section);
            } else if (description.contains("list contains")) {
                // Add the exact record we will later query.
                message.addRecord(record, section);
            }
            // "empty list (null)" case needs no preparation.
        }

        // -----------------------------------------------------------------
        // Execute and verify the expected outcome.
        // -----------------------------------------------------------------
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass,
                    () -> message.findRecord(record, section),
                    () -> description + " should throw " + exClass.getSimpleName());
        } else {
            boolean result = message.findRecord(record, section);
            assertEquals((Boolean) expected,
                    result,
                    () -> description + " – expected " + expected + " but got " + result);
        }
    }
}
```