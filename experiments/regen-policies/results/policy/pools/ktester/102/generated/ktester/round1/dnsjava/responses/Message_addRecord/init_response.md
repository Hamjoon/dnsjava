```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#addRecord(Record, int)}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>a new section list is created when the target section is empty,
 *   <li>the header count for the section is incremented,
 *   <li>records are appended in the order they are added, and
 *   <li>an {@link ArrayIndexOutOfBoundsException} is thrown for an invalid section index.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_addRecord_Test {

    private Message message;
    private Header header;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor which creates a fresh Header and empty sections.
        message = new Message();
        header = message.getHeader();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up between tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void addRecordCreatesSectionAndIncrementsCount() throws UnknownHostException {
        // Given
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        int section = Section.ANSWER;

        // Pre‑condition checks
        assertEquals(0, header.getCount(section));
        assertTrue(message.getSection(section).isEmpty());

        // When
        message.addRecord(aRecord, section);

        // Then
        assertEquals(1, header.getCount(section), "Header count for the section should be incremented");
        assertEquals(1, message.getSection(section).size(), "Section should contain exactly one record");
        assertSame(aRecord, message.getSection(section).get(0), "The added record should be stored in the section");
    }

    @Test
    void addRecordAppendsWhenSectionAlreadyExists() throws UnknownHostException {
        // Given
        Name name = Name.fromConstantString("example.org.");
        Record first = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Record second = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        int section = Section.AUTHORITY;

        // Add the first record
        message.addRecord(first, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());

        // When – add a second record to the same section
        message.addRecord(second, section);

        // Then
        assertEquals(2, header.getCount(section), "Header count should reflect two records");
        assertEquals(2, message.getSection(section).size(), "Section should contain two records");
        assertSame(first, message.getSection(section).get(0), "First record should stay at index 0");
        assertSame(second, message.getSection(section).get(1), "Second record should be appended at index 1");
    }

    @Test
    void addRecordInvalidSectionThrowsException() {
        // Given
        Record dummy = Record.newRecord(
                Name.fromConstantString("invalid.example."),
                Type.A,
                DClass.IN);
        int invalidSection = 4; // valid sections are 0‑3

        // When / Then
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.addRecord(dummy, invalidSection),
                "Adding a record to an invalid section index should throw ArrayIndexOutOfBoundsException");
    }
}
```