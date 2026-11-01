```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Unit tests for {@link Message#addRecord(Record, int)}.
 *
 * The tests verify that:
 * <ul>
 *   <li>The appropriate section list is created lazily when it is {@code null}.</li>
 *   <li>The record is added to the requested section.</li>
 *   <li>The header count for the section is incremented correctly.</li>
 *   <li>Multiple calls accumulate records and keep the count accurate.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_addRecord_Test {

    private Message message;
    private Record sampleRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message instance for each test.
        message = new Message();

        // Build a simple A record to use in the tests.
        Name name = Name.fromConstantString("example.com.");
        sampleRecord = Record.newRecord(name, Type.A, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; let the GC handle the objects.
        message = null;
        sampleRecord = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    @Test
    void addRecord_initializesSectionAndIncrementsCount() {
        // Pre‑condition: the ANSWER section is null (lazy‑initialized).
        assertNull(getInternalSectionArray(message)[Section.ANSWER]);

        // Act: add a record to the ANSWER section.
        message.addRecord(sampleRecord, Section.ANSWER);

        // Verify the section list is now non‑null and contains the record.
        List<Record> answerSection = message.getSection(Section.ANSWER);
        assertNotNull(answerSection, "Section should be initialized");
        assertEquals(1, answerSection.size(), "Section should contain exactly one record");
        assertSame(sampleRecord, answerSection.get(0), "The stored record should be the one added");

        // Verify the header count for the ANSWER section has been incremented.
        assertEquals(1, message.getHeader().getCount(Section.ANSWER),
                "Header count for ANSWER should be 1");
    }

    @Test
    void addRecord_multipleAddsAccumulateAndUpdateCount() {
        // Add three records to the ADDITIONAL section.
        Record r1 = Record.newRecord(Name.fromConstantString("a.example.com."), Type.A, DClass.IN);
        Record r2 = Record.newRecord(Name.fromConstantString("b.example.com."), Type.A, DClass.IN);
        Record r3 = Record.newRecord(Name.fromConstantString("c.example.com."), Type.A, DClass.IN);

        message.addRecord(r1, Section.ADDITIONAL);
        message.addRecord(r2, Section.ADDITIONAL);
        message.addRecord(r3, Section.ADDITIONAL);

        // Verify all three records are present in insertion order.
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        assertEquals(3, additional.size(), "Section should contain three records");
        assertEquals(r1, additional.get(0));
        assertEquals(r2, additional.get(1));
        assertEquals(r3, additional.get(2));

        // Verify the header count matches the number of records added.
        assertEquals(3, message.getHeader().getCount(Section.ADDITIONAL),
                "Header count for ADDITIONAL should be 3");
    }

    @Test
    void addRecord_toDifferentSectionsKeepsCountsSeparate() {
        Record q = Record.newRecord(Name.fromConstantString("q.example.com."), Type.A, DClass.IN);
        Record a = Record.newRecord(Name.fromConstantString("a.example.com."), Type.A, DClass.IN);
        Record auth = Record.newRecord(Name.fromConstantString("auth.example.com."), Type.A, DClass.IN);

        message.addRecord(q, Section.QUESTION);
        message.addRecord(a, Section.ANSWER);
        message.addRecord(auth, Section.AUTHORITY);

        // Verify each section contains exactly its record.
        assertEquals(1, message.getSection(Section.QUESTION).size());
        assertEquals(1, message.getSection(Section.ANSWER).size());
        assertEquals(1, message.getSection(Section.AUTHORITY).size());

        // Verify header counts are independent.
        assertEquals(1, message.getHeader().getCount(Section.QUESTION));
        assertEquals(1, message.getHeader().getCount(Section.ANSWER));
        assertEquals(1, message.getHeader().getCount(Section.AUTHORITY));
    }

    /**
     * Helper to expose the internal {@code sections} array via reflection.
     * This is only used to assert the lazy‑initialisation behaviour without
     * accessing private fields directly in production code.
     */
    private static List<Record>[] getInternalSectionArray(Message msg) {
        try {
            java.lang.reflect.Field f = Message.class.getDeclaredField("sections");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Record>[] arr = (List<Record>[]) f.get(msg);
            return arr;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
```