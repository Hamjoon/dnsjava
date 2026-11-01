```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#getSection(int)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>the default empty sections of a newly‑created {@link Message};
 *   <li>the behaviour when an invalid section index is supplied;
 *   <li>that the returned list is immutable;
 *   <li>that records added via {@link Message#addRecord(Record, int)} are visible through
 *       {@code getSection}.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_getSection_Test {

    private Message message;
    private Record aRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        // Create a fresh Message for each test to avoid cross‑test contamination.
        message = new Message();

        // Prepare a simple A record that can be added to any section.
        Name name = Name.fromConstantString("www.example.");
        aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – Message does not hold external resources.
        message = null;
        aRecord = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void getSection_returnsEmptyUnmodifiableLists_forNewMessage() {
        // All four DNS sections must be present and empty.
        for (int i = 0; i < 4; i++) {
            List<Record> section = message.getSection(i);
            assertTrue(section.isEmpty(), "Section " + i + " should be empty");
            // The list must be unmodifiable – any attempt to modify it should fail.
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> section.add(aRecord),
                    "Section list must be immutable");
        }

        // Section indices outside the valid range must cause IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> message.getSection(4));
        assertThrows(IllegalArgumentException.class, () -> message.getSection(-1));
    }

    @Test
    void getSection_reflectsAddedRecords_andRemainsUnmodifiable() {
        // Add the same record to three different sections.
        message.addRecord(aRecord, Section.QUESTION);
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(aRecord, Section.AUTHORITY);

        // Verify visibility.
        List<Record> q = message.getSection(Section.QUESTION);
        List<Record> a = message.getSection(Section.ANSWER);
        List<Record> auth = message.getSection(Section.AUTHORITY);

        assertEquals(1, q.size(), "QUESTION section should contain one record");
        assertEquals(1, a.size(), "ANSWER section should contain one record");
        assertEquals(1, auth.size(), "AUTHORITY section should contain one record");

        assertSame(aRecord, q.get(0));
        assertSame(aRecord, a.get(0));
        assertSame(aRecord, auth.get(0));

        // The returned lists must still be immutable.
        assertThrows(
                UnsupportedOperationException.class,
                () -> q.clear(),
                "Returned list must stay immutable after records are added");
    }

    @Test
    void getSection_returnsEmptyList_whenSectionNeverInitialised() {
        // The ADDITIONAL section has never been touched; it should be empty
        // and not the same instance as any previously returned list.
        List<Record> additionalFirst = message.getSection(Section.ADDITIONAL);
        assertTrue(additionalFirst.isEmpty());

        // Add a record to a different section and ensure the ADDITIONAL list
        // is still empty and a new empty list instance is returned each call.
        message.addRecord(aRecord, Section.ANSWER);
        List<Record> additionalSecond = message.getSection(Section.ADDITIONAL);
        assertTrue(additionalSecond.isEmpty());
        assertNotSame(additionalFirst, additionalSecond, "Each call should return a fresh empty list");
    }

    @Test
    void getSection_throwsWhenSectionConstantIsInvalid() {
        // Section constants are defined in org.xbill.DNS.Section (0‑3). Anything else is illegal.
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> message.getSection(5)),
                () -> assertThrows(IllegalArgumentException.class, () -> message.getSection(-2)));
    }
}
```