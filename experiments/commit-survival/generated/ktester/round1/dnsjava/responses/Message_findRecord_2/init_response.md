```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Unit tests for {@link Message#findRecord(Record)}.
 *
 * <p>The tests verify that the method correctly scans all four DNS sections
 * (QUESTION, ANSWER, AUTHORITY, ADDITIONAL) and returns {@code true} when the
 * supplied record is present in any of them, and {@code false} otherwise.
 *
 * <p>No private members of {@link Message} are accessed; the public API
 * {@link Message#addRecord(Record, int)} and {@link Message#getSection(int)} are
 * used to set up the test fixtures.
 */
@ExtendWith(MockitoExtension.class)
class Message_findRecord_2_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor which creates an empty header and empty sections.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /**
     * Verify that {@code findRecord} returns {@code true} when the record is
     * present in the ANSWER section.
     */
    @Test
    void findRecord_true_whenInAnswerSection() throws IOException {
        Name name = Name.fromConstantString("example.com.");
        // Create a simple TXT record.
        Record txt = new TXTRecord(name, DClass.IN, 3600, "test");
        // Add the record to the ANSWER section.
        message.addRecord(txt, Section.ANSWER);

        assertTrue(message.findRecord(txt),
                "findRecord should return true when the record is in the ANSWER section");
    }

    /**
     * Verify that {@code findRecord} returns {@code true} when the record is
     * present in the ADDITIONAL section.
     */
    @Test
    void findRecord_true_whenInAdditionalSection() throws IOException {
        Name name = Name.fromConstantString("additional.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "additional");
        message.addRecord(txt, Section.ADDITIONAL);

        assertTrue(message.findRecord(txt),
                "findRecord should return true when the record is in the ADDITIONAL section");
    }

    /**
     * Verify that {@code findRecord} returns {@code false} when the record is
     * not present in any section.
     */
    @Test
    void findRecord_false_whenAbsent() throws IOException {
        Name name1 = Name.fromConstantString("present.com.");
        Record present = new TXTRecord(name1, DClass.IN, 3600, "present");
        message.addRecord(present, Section.ANSWER);

        // Record that will *not* be added.
        Name name2 = Name.fromConstantString("absent.com.");
        Record absent = new TXTRecord(name2, DClass.IN, 3600, "absent");

        assertFalse(message.findRecord(absent),
                "findRecord should return false when the record is not in any section");
    }

    /**
     * Verify that {@code findRecord} scans all sections (QUESTION, ANSWER,
     * AUTHORITY, ADDITIONAL). The record is placed in the AUTHORITY section,
     * which is the third section the method checks.
     */
    @Test
    void findRecord_true_whenInAuthoritySection() throws IOException {
        Name name = Name.fromConstantString("authority.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "authority");
        message.addRecord(txt, Section.AUTHORITY);

        assertTrue(message.findRecord(txt),
                "findRecord should return true when the record is in the AUTHORITY section");
    }

    /**
     * Verify that the method works correctly when the same record appears in
     * multiple sections – it should still return {@code true}.
     */
    @Test
    void findRecord_true_whenRecordInMultipleSections() throws IOException {
        Name name = Name.fromConstantString("multi.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "multi");

        message.addRecord(txt, Section.ANSWER);
        message.addRecord(txt, Section.ADDITIONAL);

        assertTrue(message.findRecord(txt),
                "findRecord should return true when the record appears in more than one section");
    }
}
```