```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Extensive tests for {@link Message#addRecord(Record, int)}.
 *
 * <p>The original three tests from the template are kept unchanged. The additional
 * tests cover the remaining scenarios described in the JSON test‑case list:
 *
 * <ul>
 *   <li>adding a {@code null} record,
 *   <li>working with a {@code null} {@link Header},
 *   <li>working with a {@code null} {@code sections} array,
 *   <li>using different {@link List} implementations (ArrayList, unmodifiable list),
 *   <li>boundary section indices,
 *   <li>the QUESTION section,
 *   <li>a {@link Header} that throws an exception,
 *   <li>verification that {@link Header#incCount(int)} is invoked exactly once.
 * </ul>
 *
 * <p>Reflection is used only where the test needs to manipulate private fields
 * (e.g. {@code header} or {@code sections}) – the production code itself is
 * untouched.
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

    /* --------------------------------------------------------------------- */
    /*  Original tests (kept unchanged)                                     */
    /* --------------------------------------------------------------------- */

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
        // also verify that the concrete implementation is a LinkedList (the default)
        assertTrue(message.getSection(section) instanceof java.util.LinkedList,
                "A newly created section should be a LinkedList");
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

    /* --------------------------------------------------------------------- */
    /*  Additional tests derived from the JSON specification                  */
    /* --------------------------------------------------------------------- */

    @Test
    void addRecordNullRecordThrowsNullPointerException() {
        Record nullRecord = null;
        int section = Section.QUESTION;

        assertThrows(NullPointerException.class,
                () -> message.addRecord(nullRecord, section),
                "Adding a null Record should result in NullPointerException");
    }

    @Test
    void addRecordWithNullHeaderThrowsNullPointerException() throws Exception {
        // Force the internal header to null via reflection.
        setPrivateField(message, "header", null);

        Name name = Name.fromConstantString("nullheader.example.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        int section = Section.AUTHORITY;

        assertThrows(NullPointerException.class,
                () -> message.addRecord(aRecord, section),
                "When Message.header is null, addRecord should throw NullPointerException");
    }

    @Test
    void addRecordWithNullSectionsArrayThrowsNullPointerException() throws Exception {
        // Force the internal sections array to null.
        setPrivateField(message, "sections", null);

        Name name = Name.fromConstantString("nullsections.example.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.55"));
        int section = Section.ANSWER;

        assertThrows(NullPointerException.class,
                () -> message.addRecord(aRecord, section),
                "When Message.sections is null, addRecord should throw NullPointerException");
    }

    @Test
    void addRecordUsesExistingArrayListImplementation() throws Exception {
        // Pre‑populate the ANSWER section with an ArrayList (not the default LinkedList).
        List<Record> arrayList = new ArrayList<>();
        setSectionList(message, Section.ANSWER, arrayList);

        Name name = Name.fromConstantString("arraylist.example.");
        Record aRecord = new ARecord(name, DClass.IN, 300, InetAddress.getByName("192.0.2.10"));
        int section = Section.ANSWER;

        // When
        message.addRecord(aRecord, section);

        // Then
        assertSame(arrayList, message.getSection(section),
                "The original ArrayList instance must be reused");
        assertEquals(1, arrayList.size(), "Record must be appended to the existing list");
        assertEquals(1, header.getCount(section), "Header count must be incremented");
    }

    @Test
    void addRecordToUnmodifiableListThrowsUnsupportedOperationException() throws Exception {
        // Prepare an unmodifiable list in the ANSWER section.
        List<Record> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        setSectionList(message, Section.ANSWER, unmodifiable);

        Name name = Name.fromConstantString("unmod.example.");
        Record aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.20"));
        int section = Section.ANSWER;

        assertThrows(UnsupportedOperationException.class,
                () -> message.addRecord(aRecord, section),
                "Adding to an unmodifiable list should raise UnsupportedOperationException");
    }

    @Test
    void addRecordToQuestionSectionWorksLikeOtherSections() throws UnknownHostException {
        Name name = Name.fromConstantString("lookup.example.");
        Record question = new ARecord(name, DClass.IN, 0, null); // question records have null rdata
        int section = Section.QUESTION;

        assertEquals(0, header.getCount(section));
        assertTrue(message.getSection(section).isEmpty());

        message.addRecord(question, section);

        assertEquals(1, header.getCount(section), "Question count must be incremented");
        assertEquals(1, message.getSection(section).size());
        assertSame(question, message.getSection(section).get(0));
    }

    @Test
    void addRecordHeaderIncCountThrowsIllegalArgumentException() throws Exception {
        // Create a custom Header that throws IllegalArgumentException for SECTION_AUTHORITY.
        Header explodingHeader = new Header() {
            @Override
            public void incCount(int section) {
                if (section == Section.AUTHORITY) {
                    throw new IllegalArgumentException("bad section");
                }
                super.incCount(section);
            }
        };
        // Replace the Message's header with our custom one.
        message.setHeader(explodingHeader);

        Name name = Name.fromConstantString("badheader.example.");
        Record aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.99"));
        int section = Section.AUTHORITY;

        assertThrows(IllegalArgumentException.class,
                () -> message.addRecord(aRecord, section),
                "Header.incCount should propagate IllegalArgumentException");
    }

    @ParameterizedTest
    @CsvSource({
        // negative index
        "-1",
        // out‑of‑range index (equal to the length of the internal sections array)
        "4"
    })
    void addRecordOutOfBoundsSectionThrowsException(int invalidSection) throws UnknownHostException {
        Record aRecord = new ARecord(Name.fromConstantString("outofbounds.example."), DClass.IN, 3600,
                InetAddress.getByName("192.0.2.1"));

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.addRecord(aRecord, invalidSection),
                "Invalid section index should cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void addRecordMultipleTimesCumulativeHeaderCount() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");

        Record a1 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        Record a2 = new AAAARecord(name, DClass.IN, 7200, InetAddress.getByName("2001:db8::1"));
        int section = Section.ANSWER;

        // first add
        message.addRecord(a1, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());

        // second add
        message.addRecord(a2, section);
        assertEquals(2, header.getCount(section));
        assertEquals(2, message.getSection(section).size());

        assertSame(a1, message.getSection(section).get(0));
        assertSame(a2, message.getSection(section).get(1));
    }

    @Test
    void addRecordMaximumValidSectionIndexBoundary() throws UnknownHostException {
        // SECTION_ADDITIONAL is the highest valid index (3)
        int section = Section.ADDITIONAL;
        Name name = Name.fromConstantString("boundary.example.");
        Record cname = new CNAMERecord(name, DClass.IN, 0, Name.fromConstantString("target.example."));

        assertEquals(0, header.getCount(section));
        assertTrue(message.getSection(section).isEmpty());

        message.addRecord(cname, section);

        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());
        assertSame(cname, message.getSection(section).get(0));
    }

    @Test
    void addRecordVerifiesHeaderIncCountInvocationWithMockito() {
        // Create a spy of the real Header so we can verify the interaction.
        Header spyHeader = spy(message.getHeader());
        message.setHeader(spyHeader);

        Name name = Name.fromConstantString("mockheader.example.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getLoopbackAddress());
        int section = Section.ANSWER;

        message.addRecord(aRecord, section);

        // Verify incCount was called exactly once with the proper argument.
        verify(spyHeader, times(1)).incCount(section);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper reflection utilities                                           */
    /* --------------------------------------------------------------------- */

    /**
     * Sets a private field on the given target object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Helper that puts a specific List instance into {@code message.sections[section]}.
     */
    private static void setSectionList(Message msg, int section, List<Record> list) throws Exception {
        // The {@code sections} field is a private array of List<Record>.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        if (sections == null) {
            // initialise the array if the constructor didn't do it (unlikely)
            sections = (List<Record>[]) new List[Section.SECTION_COUNT];
            sectionsField.set(msg, sections);
        }
        sections[section] = list;
    }
}
```

This test suite:

* Keeps the original three tests untouched.
* Adds dedicated tests for every scenario from the provided JSON, using reflection where necessary.
* Uses Mockito to verify that `Header.incCount(int)` is called exactly once.
* Employs parameterized tests for out‑of‑bounds section indices.
* Checks behaviour with `null` arguments, custom `Header` implementations, different `List` types, and the boundary section index.