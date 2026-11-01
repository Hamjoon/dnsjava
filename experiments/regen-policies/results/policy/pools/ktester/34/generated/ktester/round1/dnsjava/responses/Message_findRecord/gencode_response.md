```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for {@link Message#findRecord(Record, int)}.
 *
 * <p>The test suite covers the scenarios described in the JSON test cases as well as the
 * basic scenarios already present in the original framework.</p>
 *
 * <ul>
 *   <li>Empty (null) section → {@code false}</li>
 *   <li>Non‑null section that does not contain the record → {@code false}</li>
 *   <li>Section that contains the record → {@code true}</li>
 *   <li>Invalid section indices (negative / out of range) → {@link IllegalArgumentException}</li>
 * </ul>
 *
 * <p>Mockito is used to create lightweight {@link Record} mocks; the same mock instance is
 * added to the {@link Message} when we want the list to contain the record.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_findRecord_Test {

    private Message message;
    private Record aRecord;          // Record that will be added to the message
    private Record differentRecord; // Record that will NOT be added

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Fresh Message for each test case.
        message = new Message();

        // Build two simple A‑records (type A, class IN) with distinct names.
        // These are used by the original non‑parameterised tests.
        Name nameA = Name.fromConstantString("host1.example.com.");
        Name nameB = Name.fromConstantString("host2.example.com.");

        aRecord = Record.newRecord(nameA, Type.A, DClass.IN);
        differentRecord = Record.newRecord(nameB, Type.A, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------------- */
    /* Original explicit tests (kept for completeness)                      */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* Parameterised tests derived from the JSON test cases                */
    /* --------------------------------------------------------------------- */

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
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideFindRecordCases() {
        return Stream.of(
                // 1. Valid section – empty list (null) → false
                arguments(
                        "Valid section – empty list (null) → false",
                        mockRecord(1),
                        Section.ANSWER,
                        false),

                // 2. Valid section – list present but does not contain record → false
                arguments(
                        "Valid section – list present but does not contain record → false",
                        mockRecord(2),
                        Section.AUTHORITY,
                        false),

                // 3. Valid section – list contains record → true
                arguments(
                        "Valid section – list contains record → true",
                        mockRecord(3),
                        Section.ADDITIONAL,
                        true),

                // 4. Invalid section (negative) → IllegalArgumentException
                arguments(
                        "Invalid section (negative) → IllegalArgumentException",
                        mockRecord(4),
                        -1,
                        IllegalArgumentException.class),

                // 5. Invalid section (out of range) → IllegalArgumentException
                arguments(
                        "Invalid section (out of range) → IllegalArgumentException",
                        mockRecord(5),
                        5,
                        IllegalArgumentException.class)
        );
    }

    /**
     * Parameterised test that executes all scenarios defined in the JSON test cases.
     *
     * @param description human‑readable description of the scenario
     * @param record      the {@link Record} supplied to {@code findRecord}
     * @param section     the section index supplied to {@code findRecord}
     * @param expected    either {@link Boolean} (expected return value) or a
     *                    {@link Class} denoting the expected exception type
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideFindRecordCases")
    void testFindRecordParameterized(String description,
                                     Record record,
                                     int section,
                                     Object expected) {
        // -----------------------------------------------------------------
        // Prepare the Message state according to the scenario description.
        // -----------------------------------------------------------------
        if (section >= 0 && section < Section.MAX) {
            // For the "list present but does not contain" case we need a *different*
            // record in the section. For the "list contains" case we add the same record.
            if (description.contains("list present but does not contain")) {
                // Add a dummy record that is *not* the one we will query for.
                Record dummy = mockRecord(99);
                message.addRecord(dummy, section);
            } else if (description.contains("list contains")) {
                // Add the exact record we will later query.
                message.addRecord(record, section);
            }
            // The "empty list (null)" case requires no preparation – the section
            // remains null, which is the default state of a freshly created Message.
        }

        // -----------------------------------------------------------------
        // Execute and verify the expected outcome.
        // -----------------------------------------------------------------
        if (expected instanceof Class) {
            // Expected an exception.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass,
                    () -> message.findRecord(record, section),
                    () -> description + " should throw " + exClass.getSimpleName());
        } else {
            // Expected a boolean return value.
            boolean result = message.findRecord(record, section);
            assertEquals((Boolean) expected, result,
                    () -> description + " – expected " + expected + " but got " + result);
        }
    }

    /**
     * Helper that creates a Mockito mock of {@link Record}. The mock is given a
     * meaningful {@code toString()} implementation to aid debugging.
     *
     * @param id identifier used only for readability
     * @return a mock {@link Record}
     */
    private static Record mockRecord(int id) {
        Record mock = Mockito.mock(Record.class, "RecordMock{id=" + id + "}");
        // Provide a stable toString for log messages / test failures.
        Mockito.when(mock.toString()).thenReturn("RecordMock{id=" + id + "}");
        return mock;
    }
}
```