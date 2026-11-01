```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#findRecord(Record)}.
 *
 * The method scans the ANSWER, AUTHORITY and ADDITIONAL sections of a {@link Message}
 * and returns {@code true} if the supplied {@link Record} is present in any of those
 * sections. It must return {@code false} when the record is absent or only present in
 * the QUESTION section.
 *
 * Additional edge‑case tests are included:
 * <ul>
 *   <li>When the internal {@code sections} array is {@code null} → {@link NullPointerException}</li>
 *   <li>When the {@code sections} array is shorter than the highest section index → {@link ArrayIndexOutOfBoundsException}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_findRecord_2_Test {

    private Message message;
    private Record aRecord;
    private Record txtRecord;
    private Name name;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message and some records for each test.
        message = new Message();                     // uses default Header
        name = Name.fromConstantString("example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references – not strictly required but keeps the test tidy.
        message = null;
        aRecord = null;
        txtRecord = null;
        name = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /*  Normal behaviour – record present in one of the examined sections   */
    /* --------------------------------------------------------------------- */

    /**
     * Parameter provider for the success group. Each argument consists of:
     * <ul>
     *   <li>a {@link Record} to be added</li>
     *   <li>the {@link Section} constant where it will be stored</li>
     * </ul>
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> successProvider() {
        return Stream.of(
                // ANSWER section
                org.junit.jupiter.params.provider.Arguments.of(
                        Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN),
                        Section.ANSWER),
                // AUTHORITY section
                org.junit.jupiter.params.provider.Arguments.of(
                        Record.newRecord(Name.fromConstantString("example.org."), Type.MX, DClass.IN),
                        Section.AUTHORITY),
                // ADDITIONAL section
                org.junit.jupiter.params.provider.Arguments.of(
                        Record.newRecord(Name.fromConstantString("example.net."), Type.TXT, DClass.IN),
                        Section.ADDITIONAL)
        );
    }

    @ParameterizedTest(name = "[{index}] Record in {1} section should be found")
    @MethodSource("successProvider")
    void findRecord_success(Record record, int section) {
        // Add the record to the requested section and verify that findRecord returns true.
        message.addRecord(record, section);
        assertTrue(message.findRecord(record),
                () -> "findRecord should return true when the record is in the " + Section.string(section) + " section");
    }

    /* --------------------------------------------------------------------- */
    /*  Record not present anywhere                                           */
    /* --------------------------------------------------------------------- */

    @Test
    void findRecord_notPresent_returnsFalse() {
        // No records have been added yet.
        assertFalse(message.findRecord(aRecord),
                "findRecord should return false when the record is absent");
    }

    /* --------------------------------------------------------------------- */
    /*  Record present only in QUESTION section (should be ignored)          */
    /* --------------------------------------------------------------------- */

    @Test
    void findRecord_presentOnlyInQuestion_returnsFalse() {
        // The QUESTION section is NOT examined by findRecord.
        message.addRecord(aRecord, Section.QUESTION);
        assertFalse(message.findRecord(aRecord),
                "findRecord must ignore records in the QUESTION section");
    }

    /* --------------------------------------------------------------------- */
    /*  Multiple sections – one matches                                        */
    /* --------------------------------------------------------------------- */

    @Test
    void findRecord_multipleSections_oneMatch_returnsTrue() {
        // Add a different record to ANSWER and the target record to ADDITIONAL.
        message.addRecord(txtRecord, Section.ANSWER);
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRecord(aRecord),
                "findRecord should return true when the record appears in any examined section");
    }

    /* --------------------------------------------------------------------- */
    /*  Null argument                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void findRecord_nullRecord_throwsNPE() {
        // The implementation does not guard against null, so a NullPointerException is expected.
        assertThrows(NullPointerException.class, () -> message.findRecord(null));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge case – internal sections array is null                          */
    /* --------------------------------------------------------------------- */

    @Test
    void findRecord_nullSections_throwsNPE() throws Exception {
        // Use reflection to set the private 'sections' field to null.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);

        // The method will attempt to read sections[i] and therefore throw NPE.
        assertThrows(NullPointerException.class, () -> message.findRecord(aRecord));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge case – sections array shorter than highest index                 */
    /* --------------------------------------------------------------------- */

    @Test
    void findRecord_shortSections_throwsArrayIndexOutOfBoundsException() throws Exception {
        // Create an array that is *shorter* than the highest section index (Section.ADDITIONAL = 3).
        // Length 3 gives valid indices 0..2; accessing index 3 will cause AIOOBE.
        @SuppressWarnings("unchecked")
        List<Record>[] shortArray = (List<Record>[]) new List[Section.ADDITIONAL]; // length = 3
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, shortArray);

        // Even though the array is short, we can still add a record to a valid section (e.g., ANSWER)
        // to avoid a NullPointerException before the loop reaches the out‑of‑range index.
        message.addRecord(aRecord, Section.ANSWER);

        // Expected: when the loop reaches Section.ADDITIONAL (index 3) an AIOOBE is thrown.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.findRecord(aRecord));
    }
}
```