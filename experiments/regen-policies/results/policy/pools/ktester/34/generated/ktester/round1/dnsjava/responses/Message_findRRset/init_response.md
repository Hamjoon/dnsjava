```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.UnknownHostException;

/**
 * Unit tests for {@link Message#findRRset(Name, int, int)}.
 *
 * The tests cover:
 * <ul>
 *   <li>successful lookup when the RRset is present in the requested section</li>
 *   <li>negative lookup when the RRset is absent or the section is empty</li>
 *   <li>validation of the {@code section} argument (illegal values)</li>
 * </ul>
 *
 * No private members of {@link Message} or its collaborators are accessed.
 */
@ExtendWith(MockitoExtension.class)
class Message_findRRset_Test {

    /** Re‑usable name used in the tests. */
    private static Name TEST_NAME;

    /** Message instance created for each test. */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // a simple absolute name – the library guarantees that this never throws for a valid DNS name
        TEST_NAME = Name.fromConstantString("example.com.");
    }

    @BeforeEach
    void setupBeforeEach() {
        // a fresh Message with an empty header for every test case
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up; the Message instance will be GC‑ed
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // no global resources to release
    }

    /**
     * Verify that {@code findRRset} returns {@code true} when a matching record
     * exists in the queried section.
     */
    @Test
    void findRRset_presentInSection_returnsTrue() throws UnknownHostException {
        // arrange – add an A record to the ANSWER section
        Record aRecord = Record.newRecord(
                TEST_NAME,
                Type.A,
                DClass.IN,
                3600,
                InetAddress.getByName("127.0.0.1"));
        message.addRecord(aRecord, Section.ANSWER);

        // act
        boolean found = message.findRRset(TEST_NAME, Type.A, Section.ANSWER);

        // assert
        assertTrue(found, "RRset should be found in the ANSWER section");
    }

    /**
     * Verify that {@code findRRset} returns {@code false} when the section does not
     * contain a matching record, even though the name exists elsewhere.
     */
    @Test
    void findRRset_presentInOtherSection_returnsFalse() throws UnknownHostException {
        // arrange – add an A record to the AUTHORITY section only
        Record aRecord = Record.newRecord(
                TEST_NAME,
                Type.A,
                DClass.IN,
                3600,
                InetAddress.getByName("127.0.0.1"));
        message.addRecord(aRecord, Section.AUTHORITY);

        // act – query the ANSWER section, which is empty
        boolean found = message.findRRset(TEST_NAME, Type.A, Section.ANSWER);

        // assert
        assertFalse(found, "RRset should not be found in a section that does not contain it");
    }

    /**
     * Verify that {@code findRRset} returns {@code false} when the requested
     * section is empty (i.e., {@code sections[section] == null} internally).
     */
    @Test
    void findRRset_emptySection_returnsFalse() {
        // act – query a section that has never been initialised (e.g., SECTION.ADDITIONAL)
        boolean found = message.findRRset(TEST_NAME, Type.A, Section.ADDITIONAL);

        // assert
        assertFalse(found, "RRset should be false when the section is empty");
    }

    /**
     * Verify that an illegal section index causes {@link Section#check(int)} to
     * throw {@link IllegalArgumentException}.
     */
    @Test
    void findRRset_invalidSection_throwsIllegalArgumentException() {
        // arrange – an out‑of‑range section index (valid values are 0‑3)
        int invalidSection = 5;

        // act & assert
        assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(TEST_NAME, Type.A, invalidSection),
                "Calling findRRset with an invalid section should throw IllegalArgumentException");
    }

    /**
     * Verify the overloaded convenience method {@code findRRset(Name, int)} which
     * checks the ANSWER, AUTHORITY and ADDITIONAL sections.
     */
    @Test
    void findRRset_overloadedMethod_checksAllRelevantSections() throws UnknownHostException {
        // arrange – add a TXT record to the AUTHORITY section
        Record txtRecord = new TXTRecord(TEST_NAME, DClass.IN, 3600, "sample");
        message.addRecord(txtRecord, Section.AUTHORITY);

        // act – use the 2‑arg overload; it should locate the record in AUTHORITY
        boolean found = message.findRRset(TEST_NAME, Type.TXT);

        // assert
        assertTrue(found, "Overloaded findRRset should locate the TXT RRset in AUTHORITY");
    }
}
```