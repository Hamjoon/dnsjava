```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#getSection(int)}.
 *
 * <p>The method should:
 *
 * <ul>
 *   <li>return an empty, immutable list when the requested section has never been created;
 *   <li>return an immutable view of the underlying list when records have been added;
 *   <li>throw {@link ArrayIndexOutOfBoundsException} for invalid section indexes (outside 0‑3);
 *   <li>propagate a {@link NullPointerException} if the internal {@code sections} array itself is {@code null}.
 * </ul>
 *
 * <p>The tests use real {@link Message}, {@link Header}, {@link TXTRecord} and other DNS classes;
 * no private members are accessed except where explicitly required to simulate a corrupted state.
 */
@ExtendWith(MockitoExtension.class)
class Message_getSection_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh message for each test – uses the no‑arg constructor which creates an empty header.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Message does not hold external resources.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /** Verify that a newly created Message returns empty immutable lists for all sections. */
    @Test
    void getSection_EmptyMessage_ReturnsEmptyImmutableLists() {
        for (int sec = 0; sec < 4; sec++) {
            List<Record> secList = message.getSection(sec);
            assertTrue(secList.isEmpty(), "Section " + sec + " should be empty");
            // The list must be immutable – any modification attempt should throw.
            assertThrows(UnsupportedOperationException.class, () -> secList.add(null));
        }
    }

    /** Verify that an invalid section index results in ArrayIndexOutOfBoundsException. */
    @ParameterizedTest
    @ValueSource(ints = { -1, 4, 100 })
    void getSection_InvalidIndex_ThrowsArrayIndexOutOfBoundsException(int invalidIndex) {
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.getSection(invalidIndex),
                "Calling getSection(" + invalidIndex + ") should throw ArrayIndexOutOfBoundsException");
    }

    /** Add a record to a specific section and verify that getSection returns an immutable view. */
    @Test
    void getSection_WithRecords_ReturnsImmutableViewContainingRecords() throws IOException {
        Name name = Name.fromConstantString("example.com.");
        TXTRecord txt = new TXTRecord(name, DClass.IN, 3600, "test");
        // Add the record to the ANSWER section (index = Section.ANSWER == 1)
        message.addRecord(txt, Section.ANSWER);

        List<Record> answerSection = message.getSection(Section.ANSWER);
        assertEquals(1, answerSection.size(), "Answer section should contain one record");
        assertSame(txt, answerSection.get(0), "The record returned should be the same instance added");

        // The returned list must be unmodifiable.
        assertThrows(UnsupportedOperationException.class, () -> answerSection.add(null));
        assertThrows(UnsupportedOperationException.class, () -> answerSection.clear());
    }

    /** Ensure that after adding multiple records the returned list reflects all of them and remains immutable. */
    @Test
    void getSection_MultipleRecords_ImmutableAndComplete() throws IOException {
        Name name = Name.fromConstantString("multi.example.");
        for (int i = 0; i < 5; i++) {
            TXTRecord txt = new TXTRecord(name, DClass.IN, 3600, "txt-" + i);
            message.addRecord(txt, Section.ADDITIONAL);
        }

        List<Record> additional = message.getSection(Section.ADDITIONAL);
        assertEquals(5, additional.size(), "Additional section should contain five records");
        // Verify order is preserved (records are stored in insertion order)
        for (int i = 0; i < 5; i++) {
            Record r = additional.get(i);
            assertTrue(r instanceof TXTRecord);
            assertEquals("txt-" + i, ((TXTRecord) r).getStrings().get(0));
        }

        // Immutability check
        assertThrows(UnsupportedOperationException.class, () -> additional.remove(0));
    }

    /** Verify that the empty list returned for a never‑used section is the same instance as Collections.emptyList(). */
    @Test
    void getSection_EmptySection_ReturnsStandardEmptyList() {
        List<Record> empty = message.getSection(Section.QUESTION);
        assertSame(Collections.emptyList(), empty,
                "Empty sections should return Collections.emptyList()");
    }

    /** After adding two records to section 2, verify that getSection returns an unmodifiable list in insertion order. */
    @Test
    void getSection_TwoRecords_ReturnsUnmodifiableListInInsertionOrder() throws IOException {
        Name name = Name.fromConstantString("authority.example.");
        TXTRecord recA = new TXTRecord(name, DClass.IN, 3600, "recordA");
        TXTRecord recB = new TXTRecord(name, DClass.IN, 3600, "recordB");

        // Section 2 corresponds to AUTHORITY in dnsjava
        message.addRecord(recA, Section.AUTHORITY);
        message.addRecord(recB, Section.AUTHORITY);

        List<Record> authority = message.getSection(Section.AUTHORITY);
        assertEquals(2, authority.size(), "Authority section should contain two records");
        assertSame(recA, authority.get(0), "First record should be recA");
        assertSame(recB, authority.get(1), "Second record should be recB");

        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> authority.add(new TXTRecord(name, DClass.IN, 3600, "new")));
        assertThrows(UnsupportedOperationException.class, () -> authority.remove(0));
    }

    /** Verify that attempting to remove an element from an empty immutable list throws UnsupportedOperationException. */
    @Test
    void getSection_EmptyList_RemoveThrowsUnsupportedOperationException() {
        List<Record> empty = message.getSection(Section.ANSWER); // never added to
        assertTrue(empty.isEmpty(), "Section should be empty");
        assertThrows(UnsupportedOperationException.class, () -> empty.remove(0));
    }

    /**
     * Simulate a corrupted internal state where the {@code sections} array itself is {@code null}.
     * The method should then throw a {@link NullPointerException} when it tries to index the array.
     */
    @Test
    void getSection_InternalSectionsNull_ThrowsNullPointerException() throws Exception {
        // Use reflection to set the private 'sections' field to null.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);

        assertThrows(NullPointerException.class, () -> message.getSection(0),
                "When internal sections array is null, getSection should throw NullPointerException");
    }
}
```