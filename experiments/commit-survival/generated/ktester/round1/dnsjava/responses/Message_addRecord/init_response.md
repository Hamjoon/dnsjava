```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#addRecord(Record, int)}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>a new section list is created when the target section is {@code null};
 *   <li>the header count for the section is incremented;
 *   <li>records are correctly appended to an existing section; and
 *   <li>an {@link ArrayIndexOutOfBoundsException} is thrown for an illegal section index.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_addRecord_Test {

    private Message message;
    private Record txtRecord1;
    private Record txtRecord2;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Use the no‑arg constructor – it creates a fresh Header and empty sections.
        message = new Message();

        // Prepare two simple TXT records that will be added to the message.
        Name name = Name.fromConstantString("example.com.");
        txtRecord1 = new TXTRecord(name, DClass.IN, 3600, "first");
        txtRecord2 = new TXTRecord(name, DClass.IN, 3600, "second");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for these unit tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void addRecord_createsSectionWhenNull_andIncrementsHeaderCount() {
        // Section.ADDITIONAL is initially null inside the Message.
        assertNull(getRawSectionArray()[Section.ADDITIONAL]);

        // Add the first record.
        message.addRecord(txtRecord1, Section.ADDITIONAL);

        // The section list should now be instantiated and contain the record.
        List<Record> section = message.getSection(Section.ADDITIONAL);
        assertNotNull(section, "Section list should be created");
        assertEquals(1, section.size(), "Section should contain exactly one record");
        assertSame(txtRecord1, section.get(0), "The stored record must be the one added");

        // Header count for the ADDITIONAL section must be incremented to 1.
        assertEquals(1, message.getHeader().getCount(Section.ADDITIONAL));
    }

    @Test
    void addRecord_appendsToExistingSection_andUpdatesCount() {
        // First insertion – creates the list.
        message.addRecord(txtRecord1, Section.ANSWER);
        // Second insertion – should append to the already‑created list.
        message.addRecord(txtRecord2, Section.ANSWER);

        List<Record> answerSection = message.getSection(Section.ANSWER);
        assertEquals(2, answerSection.size(), "Two records should be present");
        assertSame(txtRecord1, answerSection.get(0));
        assertSame(txtRecord2, answerSection.get(1));

        // Header count must reflect both additions.
        assertEquals(2, message.getHeader().getCount(Section.ANSWER));
    }

    @Test
    void addRecord_doesNotAffectOtherSections() {
        // Add a record to the QUESTION section.
        message.addRecord(txtRecord1, Section.QUESTION);
        // Verify QUESTION section count.
        assertEquals(1, message.getHeader().getCount(Section.QUESTION));

        // All other sections should still be empty / null.
        for (int i = 0; i < 4; i++) {
            if (i == Section.QUESTION) continue;
            List<Record> sec = message.getSection(i);
            assertTrue(sec.isEmpty(), "Section " + i + " should be empty");
            assertEquals(0, message.getHeader().getCount(i), "Header count for section " + i + " should be zero");
        }
    }

    @Test
    void addRecord_invalidSection_throwsArrayIndexOutOfBoundsException() {
        // Section indices are defined 0‑3. Using 5 should trigger an exception.
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.addRecord(txtRecord1, 5));
    }

    /** Helper to expose the raw {@code sections} array via reflection (used only for the first test). */
    @SuppressWarnings("unchecked")
    private List<Record>[] getRawSectionArray() {
        try {
            java.lang.reflect.Field f = Message.class.getDeclaredField("sections");
            f.setAccessible(true);
            return (List<Record>[]) f.get(message);
        } catch (ReflectiveOperationException e) {
            fail("Unable to access Message.sections field", e);
            return null; // unreachable
        }
    }
}
```