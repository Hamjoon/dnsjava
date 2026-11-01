```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Tests for {@link Message#removeAllRecords(int)}.
 *
 * The tests cover:
 * <ul>
 *   <li>removing records from a populated section</li>
 *   <li>removing records from an already empty section</li>
 *   <li>behaviour when an invalid section index is supplied</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_removeAllRecords_Test {

    private Message message;
    private Header header;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor – it creates a fresh Header and empty sections.
        message = new Message();
        header = message.getHeader();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the Message instance will be GC‑ed.
        message = null;
        header = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void removeAllRecords_FromPopulatedSection_ClearsSectionAndHeaderCount() throws UnknownHostException {
        // Arrange – add a few records to the ANSWER section.
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = new ARecord(name, DClass.IN, 3600,
                InetAddress.getByName("127.0.0.1"));
        Record txtRecord = new TXTRecord(name, DClass.IN, 3600, "test");

        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(txtRecord, Section.ANSWER);

        // Verify pre‑condition.
        assertEquals(2, message.getSection(Section.ANSWER).size(),
                "pre‑condition: two records must be present");
        assertEquals(2, header.getCount(Section.ANSWER),
                "pre‑condition: header count must reflect the added records");

        // Act – remove all records from the ANSWER section.
        message.removeAllRecords(Section.ANSWER);

        // Assert – the section is empty and the header count is zero.
        assertTrue(message.getSection(Section.ANSWER).isEmpty(),
                "section should be empty after removal");
        assertEquals(0, header.getCount(Section.ANSWER),
                "header count should be reset to zero");
    }

    @Test
    void removeAllRecords_FromEmptySection_LeavesHeaderCountZero() {
        // Arrange – ensure the QUESTION section is empty (it is by default).
        assertTrue(message.getSection(Section.QUESTION).isEmpty(),
                "pre‑condition: QUESTION section must be empty");
        assertEquals(0, header.getCount(Section.QUESTION),
                "pre‑condition: header count must be zero");

        // Act – invoke removal on the empty section.
        message.removeAllRecords(Section.QUESTION);

        // Assert – still empty and count remains zero.
        assertTrue(message.getSection(Section.QUESTION).isEmpty(),
                "empty section should stay empty");
        assertEquals(0, header.getCount(Section.QUESTION),
                "header count should remain zero");
    }

    @Test
    void removeAllRecords_InvalidSection_ThrowsArrayIndexOutOfBoundsException() {
        // The valid section indices are 0‑3 (Section constants). Using 4 should cause
        // an ArrayIndexOutOfBoundsException because the implementation accesses the
        // internal array directly without bounds checking.
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.removeAllRecords(4),
                "removing records from an invalid section should throw");
    }
}
```