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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class Message_getSection_Test {

    private Message message;

    private Record aRecord;

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        // Create a fresh Message for each test to avoid cross‑test contamination.
        message = new Message();
        // Prepare a simple A record that can be added to any section.
        Name name = Name.fromConstantString("www.example.");
        aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        message = new Message();
        Name name = Name.fromConstantString("www.example.");
        aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – Message does not hold external resources.
        message = null;
        aRecord = null;
        message = null;
        aRecord = null;
    }

    /**
     * Helper that creates a distinct ARecord for a given IP string.
     */
    private Record createDummyRecord(String ip) throws UnknownHostException {
        Name name = Name.fromConstantString("dummy.example.");
        return new ARecord(name, DClass.IN, 0, InetAddress.getByName(ip));
    }

    @Test
    void getSection_returnsEmptyUnmodifiableLists_forNewMessage() {
        // All four DNS sections must be present and empty.
        for (int i = 0; i < 4; i++) {
            List<Record> section = message.getSection(i);
            assertTrue(section.isEmpty(), "Section " + i + " should be empty");
            // The list must be unmodifiable – any attempt to modify it should fail.
            assertThrows(UnsupportedOperationException.class, () -> section.add(aRecord), "Section list must be immutable");
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
        assertThrows(UnsupportedOperationException.class, () -> q.clear(), "Returned list must stay immutable after records are added");
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
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> message.getSection(5)), () -> assertThrows(IllegalArgumentException.class, () -> message.getSection(-2)));
    }

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
        assertThrows(UnsupportedOperationException.class, () -> answer.add(aRecord), "Returned list must be immutable");
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
        assertThrows(UnsupportedOperationException.class, () -> auth.remove(0), "Returned list must be immutable");
    }

    @ParameterizedTest
    @CsvSource({ "-1", "4", "100" })
    void getSection_invalidSection_throwsIllegalArgumentException(int invalidSection) {
        assertThrows(IllegalArgumentException.class, () -> message.getSection(invalidSection), "Section index " + invalidSection + " should be rejected");
    }

    @ParameterizedTest
    @CsvSource({ // Using the Section constants values (QUESTION=0, ANSWER=1, AUTHORITY=2, ADDITIONAL=3)
    "0", "1", "2", "3" })
    void getSection_emptySection_returnsEmptyUnmodifiableList(int sectionIdx) {
        List<Record> section = message.getSection(sectionIdx);
        assertTrue(section.isEmpty(), "Section " + sectionIdx + " should be empty on a fresh Message");
        assertThrows(UnsupportedOperationException.class, () -> section.add(aRecord), "Empty list must be immutable");
    }

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
        assertThrows(UnsupportedOperationException.class, () -> additional.add(createDummyRecord("10.0.2.3")), "Returned list must stay immutable even after records are added");
    }

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
        assertFalse(authority.contains(ans1) || authority.contains(ans2), "AUTHORITY must not contain ANSWER records");
        // Verify ADDITIONAL remains empty and independent.
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        assertTrue(additional.isEmpty(), "ADDITIONAL should stay empty");
    }
}
