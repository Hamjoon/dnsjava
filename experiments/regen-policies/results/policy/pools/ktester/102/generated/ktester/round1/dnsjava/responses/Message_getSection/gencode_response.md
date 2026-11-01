```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Additional unit tests for {@link Message#getSection(int)} that cover the
 * scenarios described in the JSON test‑case specification.
 *
 * <p>These tests complement the basic tests that already exist in the
 * {@code Message_getSection_Test} class.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_getSection_AdditionalTest {

    private Message message;
    private Record aRecord; // a reusable simple A record

    @BeforeEach
    void setUp() throws UnknownHostException {
        message = new Message();
        Name name = Name.fromConstantString("www.example.");
        aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    }

    @AfterEach
    void tearDown() {
        message = null;
        aRecord = null;
    }

    /** Helper that creates a distinct ARecord for a given IP string. */
    private Record createDummyRecord(String ip) throws UnknownHostException {
        Name name = Name.fromConstantString("dummy.example.");
        return new ARecord(name, DClass.IN, 0, InetAddress.getByName(ip));
    }

    /* --------------------------------------------------------------------- */
    /*  Valid sections with existing records                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void getSection_validSection_answerContainsTwoRecords_andIsUnmodifiable() throws Exception {
        // Add two distinct records to the ANSWER section.
        Record r1 = createDummyRecord("10.0.0.1");
        Record r2 = createDummyRecord("10.0.0.2");
        message.addRecord(r1, Section.ANSWER);
        message.addRecord(r2, Section.ANSWER);

        List<Record> answer = message.getSection(Section.ANSWER);
        // Verify size and content.
        assertEquals(2, answer.size(), "ANSWER section should contain exactly two records");
        assertTrue(answer.contains(r1) && answer.contains(r2), "Both added records must be present");

        // Verify immutability.
        assertThrows(UnsupportedOperationException.class,
                () -> answer.add(aRecord),
                "Returned list must be immutable");
    }

    @Test
    void getSection_validSection_authorityContainsOneRecord_andIsUnmodifiable() throws Exception {
        // Add a single record to the AUTHORITY section.
        Record r = createDummyRecord("10.0.1.1");
        message.addRecord(r, Section.AUTHORITY);

        List<Record> auth = message.getSection(Section.AUTHORITY);
        assertEquals(1, auth.size(), "AUTHORITY section should contain exactly one record");
        assertSame(r, auth.get(0), "The record returned must be the one that was added");

        // Verify immutability.
        assertThrows(UnsupportedOperationException.class,
                () -> auth.remove(0),
                "Returned list must be immutable");
    }

    /* --------------------------------------------------------------------- */
    /*  Invalid section indices                                              */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest
    @CsvSource({
            "-1",
            "4",
            "100"
    })
    void getSection_invalidSection_throwsIllegalArgumentException(int invalidSection) {
        assertThrows(IllegalArgumentException.class,
                () -> message.getSection(invalidSection),
                "Section index " + invalidSection + " should be rejected");
    }

    /* --------------------------------------------------------------------- */
    /*  Empty sections return empty immutable list                           */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest
    @CsvSource({
            // Using the Section constants values (QUESTION=0, ANSWER=1, AUTHORITY=2, ADDITIONAL=3)
            "0",
            "1",
            "2",
            "3"
    })
    void getSection_emptySection_returnsEmptyUnmodifiableList(int sectionIdx) {
        List<Record> section = message.getSection(sectionIdx);
        assertTrue(section.isEmpty(), "Section " + sectionIdx + " should be empty on a fresh Message");
        assertThrows(UnsupportedOperationException.class,
                () -> section.add(aRecord),
                "Empty list must be immutable");
    }

    /* --------------------------------------------------------------------- */
    /*  Unmodifiable list behavior after records have been added            */
    /* --------------------------------------------------------------------- */

    @Test
    void getSection_unmodifiableAfterAddingRecords() throws Exception {
        // Populate ADDITIONAL with two records.
        Record r1 = createDummyRecord("10.0.2.1");
        Record r2 = createDummyRecord("10.0.2.2");
        message.addRecord(r1, Section.ADDITIONAL);
        message.addRecord(r2, Section.ADDITIONAL);

        List<Record> additional = message.getSection(Section.ADDITIONAL);
        assertEquals(2, additional.size(), "ADDITIONAL should contain the two added records");

        // Attempt to modify the returned list – must fail.
        assertThrows(UnsupportedOperationException.class,
                () -> additional.add(createDummyRecord("10.0.2.3")),
                "Returned list must stay immutable even after records are added");
    }

    /* --------------------------------------------------------------------- */
    /*  Section independence – records are isolated per section               */
    /* --------------------------------------------------------------------- */

    @Test
    void getSection_sectionIndependence_betweenAnswerAndAuthority() throws Exception {
        // Two records in ANSWER, one in AUTHORITY.
        Record ans1 = createDummyRecord("10.1.0.1");
        Record ans2 = createDummyRecord("10.1.0.2");
        Record auth = createDummyRecord("10.1.1.1");

        message.addRecord(ans1, Section.ANSWER);
        message.addRecord(ans2, Section.ANSWER);
        message.addRecord(auth, Section.AUTHORITY);

        // Verify ANSWER section.
        List<Record> answer = message.getSection(Section.ANSWER);
        assertEquals(2, answer.size(), "ANSWER should contain exactly two records");
        assertTrue(answer.contains(ans1) && answer.contains(ans2), "ANSWER must contain its own records only");
        assertFalse(answer.contains(auth), "ANSWER must not contain AUTHORITY records");

        // Verify AUTHORITY section.
        List<Record> authority = message.getSection(Section.AUTHORITY);
        assertEquals(1, authority.size(), "AUTHORITY should contain exactly one record");
        assertSame(auth, authority.get(0), "AUTHORITY must contain its own record only");
        assertFalse(authority.contains(ans1) || authority.contains(ans2),
                "AUTHORITY must not contain ANSWER records");

        // Verify ADDITIONAL remains empty and independent.
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        assertTrue(additional.isEmpty(), "ADDITIONAL should stay empty");
    }
}
```