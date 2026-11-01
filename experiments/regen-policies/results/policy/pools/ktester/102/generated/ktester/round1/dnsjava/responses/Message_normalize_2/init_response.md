```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#normalize(Message, boolean)}. The tests focus on the
 * most important branches of the method without touching private members.
 */
@ExtendWith(MockitoExtension.class)
class Message_normalize_2_Test {

    /** A simple A‑record used as the query in most tests. */
    private Record queryRecord;

    /** The query Message created from {@link #queryRecord}. */
    private Message query;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        queryRecord = Record.newRecord(
                Name.fromConstantString("example.com."),
                Type.A,
                DClass.IN);
        query = Message.newQuery(queryRecord);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * When the response RCODE is neither NOERROR nor NXDOMAIN the method must
     * return the original Message instance unchanged.
     */
    @Test
    void normalize_returnsSelfWhenRcodeNotNoErrorOrNxDomain() throws WireParseException {
        Message response = new Message();
        response.addRecord(queryRecord, Section.QUESTION);
        // Force a non‑NOERROR RCODE (e.g. SERVFAIL = 2)
        response.getHeader().setRcode(Rcode.SERVFAIL);

        Message result = response.normalize(query, false);
        assertSame(response, result, "Message should be returned unchanged for non‑NOERROR/NXDOMAIN");
        assertEquals(Rcode.SERVFAIL, result.getHeader().getRcode());
    }

    /**
     * A DNAME RRset that contains more than one record is considered malformed.
     * With {@code throwOnIrrelevantRecord=true} the method must throw a
     * {@link WireParseException}.
     */
    @Test
    void normalize_throwsWhenDnameRrsetHasMultipleEntries() {
        Message response = new Message();
        response.addRecord(queryRecord, Section.QUESTION);

        // Build a DNAME RRset with two entries (same name, different targets)
        Name dnameOwner = Name.fromConstantString("example.com.");
        DNAMERecord d1 = new DNAMERecord(dnameOwner, DClass.IN, 300,
                Name.fromConstantString("target1.example.com."));
        DNAMERecord d2 = new DNAMERecord(dnameOwner, DClass.IN, 300,
                Name.fromConstantString("target2.example.com."));
        response.addRecord(d1, Section.ANSWER);
        response.addRecord(d2, Section.ANSWER);

        assertThrows(WireParseException.class,
                () -> response.normalize(query, true),
                "Multiple DNAME records should cause a WireParseException when throwOnIrrelevantRecord is true");
    }

    /**
     * When a single DNAME is present in the answer section and the query is
     * *not* for a DNAME, the method must synthesize a CNAME that points to the
     * DNAME target. The synthesized CNAME must appear in the normalized
     * answer section.
     */
    @Test
    void normalize_synthesizesCnameFromDname() throws WireParseException, UnknownHostException {
        Message response = new Message();
        response.addRecord(queryRecord, Section.QUESTION);

        // Single DNAME record
        Name dnameOwner = Name.fromConstantString("example.com.");
        Name dnameTarget = Name.fromConstantString("alias.example.org.");
        DNAMERecord dname = new DNAMERecord(dnameOwner, DClass.IN, 300, dnameTarget);
        response.addRecord(dname, Section.ANSWER);

        Message normalized = response.normalize(query, false);
        // The answer should now contain the original DNAME and the synthesized CNAME
        assertEquals(2, normalized.getSection(Section.ANSWER).size(),
                "Answer section must contain DNAME + synthesized CNAME");

        // Verify the synthesized CNAME
        Record synthesized = normalized.getSection(Section.ANSWER).stream()
                .filter(r -> r.getType() == Type.CNAME)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Synthesized CNAME not found"));
        assertEquals(dnameOwner, synthesized.getName(),
                "CNAME name must be the original query name");
        assertEquals(dnameTarget, ((CNAMERecord) synthesized).getTarget(),
                "CNAME target must be the DNAME target");
    }

    /**
     * When the response contains records that are irrelevant to the query and
     * {@code throwOnIrrelevantRecord} is {@code false}, they must simply be
     * ignored. The normalized message should contain no answer records.
     */
    @Test
    void normalize_ignoresIrrelevantRecordsWhenThrowFalse() throws WireParseException, UnknownHostException {
        Message response = new Message();
        response.addRecord(queryRecord, Section.QUESTION);

        // Irrelevant A record for a different name
        Record other = new ARecord(
                Name.fromConstantString("other.example.com."),
                DClass.IN,
                300,
                InetAddress.getByName("127.0.0.2"));
        response.addRecord(other, Section.ANSWER);

        Message normalized = response.normalize(query, false);
        assertTrue(normalized.getSection(Section.ANSWER).isEmpty(),
                "Irrelevant answer records must be removed when throwOnIrrelevantRecord is false");
    }
}
```