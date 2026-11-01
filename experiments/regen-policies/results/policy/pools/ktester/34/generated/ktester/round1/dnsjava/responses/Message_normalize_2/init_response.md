```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.UnknownHostException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#normalize(Message, boolean)}.
 *
 * The tests focus on the most important branches of the method:
 *
 * <ul>
 *   <li>handling of multiple DNAME records in a single RRset (exception path)</li>
 *   <li>ignoring irrelevant answer records when {@code throwOnIrrelevantRecord} is {@code false}</li>
 *   <li>synthesising a CNAME from a valid DNAME record</li>
 * </ul>
 *
 * The test class does **not** use any reflection or access to private members of {@link Message}
 * or its collaborators.
 */
@ExtendWith(MockitoExtension.class)
class Message_normalize_2_Test {

    /** No global resources are required for these tests. */
    @BeforeAll
    static void setupBeforeAll() {
        // nothing to initialise globally
    }

    /** Fresh objects are created per test, so no per‑test setup is needed. */
    @BeforeEach
    void setupBeforeEach() {
        // nothing to initialise per test
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up per test
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up globally
    }

    /**
     * When a DNAME RRset contains more than one record the method must abort with a
     * {@link WireParseException} (if {@code throwOnIrrelevantRecord} is {@code true}).
     */
    @Test
    void normalize_throwsOnMultipleDNAMERecords() throws UnknownHostException {
        // query: A record for example.com.
        Name qname = Name.fromConstantString("example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);

        // response: two DNAME records for the same owner name – this is illegal.
        Message response = new Message();
        DNAMERecord dname1 = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("target1.example.com."));
        DNAMERecord dname2 = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("target2.example.com."));
        response.addRecord(dname1, Section.ANSWER);
        response.addRecord(dname2, Section.ANSWER);

        // The method should throw a WireParseException because we asked it to be strict.
        assertThrows(WireParseException.class, () -> response.normalize(query, true));
    }

    /**
     * Irrelevant answer records (records whose owner name does not match the query name) are
     * ignored when {@code throwOnIrrelevantRecord} is {@code false}. The returned message should
     * contain no such records.
     */
    @Test
    void normalize_ignoresIrrelevantAnswerRecordWhenNotThrowing() throws UnknownHostException, WireParseException {
        // query: MX record for example.com.
        Name qname = Name.fromConstantString("example.com.");
        Record queryRecord = Record.newRecord(qname, Type.MX, DClass.IN);
        Message query = Message.newQuery(queryRecord);

        // response: an A record for a *different* name (irrelevant).
        Message response = new Message();
        Name otherName = Name.fromConstantString("other.com.");
        ARecord irrelevant = new ARecord(otherName, DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        response.addRecord(irrelevant, Section.ANSWER);

        Message cleaned = response.normalize(query, false);
        // The answer section must be empty because the only record was irrelevant.
        assertTrue(cleaned.getSection(Section.ANSWER).isEmpty(),
                "Irrelevant answer records should be removed");
    }

    /**
     * When a single valid DNAME record is present and the query type is *not* DNAME,
     * the method must synthesize a CNAME that points to the DNAME target.
     */
    @Test
    void normalize_synthesisesCNAMEFromDNAME() throws UnknownHostException, WireParseException {
        // query: A record for example.com.
        Name qname = Name.fromConstantString("example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);

        // response: a single DNAME record.
        Message response = new Message();
        Name dnameTarget = Name.fromConstantString("target.example.org.");
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300, dnameTarget);
        response.addRecord(dname, Section.ANSWER);

        Message cleaned = response.normalize(query, false);

        // The answer section should contain the original DNAME and the synthesized CNAME.
        assertEquals(2, cleaned.getSection(Section.ANSWER).size(),
                "Answer section should contain DNAME + synthesized CNAME");

        // Verify the first RR is the DNAME we added.
        Record first = cleaned.getSection(Section.ANSWER).get(0);
        assertTrue(first instanceof DNAMERecord);
        assertEquals(dname, first);

        // Verify the second RR is a CNAME whose target is the DNAME target.
        Record second = cleaned.getSection(Section.ANSWER).get(1);
        assertTrue(second instanceof CNAMERecord,
                "Second record should be a CNAME synthesized from the DNAME");
        CNAMERecord cname = (CNAMERecord) second;
        // The synthesized CNAME target is the result of Name.fromDNAME(dname)
        Name expectedCnameTarget = qname.fromDNAME(dname);
        assertEquals(expectedCnameTarget, cname.getTarget(),
                "Synthesised CNAME target must be derived from the DNAME");
    }
}
```