```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#normalize(Message, boolean)}.
 *
 * <p>Each JSON “group” from the specification is represented by a dedicated test
 * method (or a parameterised test).  The tests use real {@link Message},
 * {@link Record} and RRset objects – only the {@link Message#isTypeAllowedInSection}
 * method is stubbed where required via a Mockito spy.</p>
 *
 * <p>All tests are written with JUnit 5 and Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_normalize_Test {

    /** Helper to create a simple A‑record used as the query. */
    private static Record queryA(String name) throws Exception {
        return Record.newRecord(Name.fromString(name, Name.root), Type.A, DClass.IN);
    }

    /** Helper to create a Message that contains only the QUESTION section. */
    private static Message newQueryMessage(Record q) {
        Message m = Message.newQuery(q);
        return m;
    }

    /** Helper to create a spy that can override {@code isTypeAllowedInSection}. */
    private static Message spyMessage(Message m) {
        Message spy = spy(m);
        // By default delegate to the real method for all types
        doCallRealMethod().when(spy).isTypeAllowedInSection(anyInt(), anyInt());
        return spy;
    }

    /* ---------------------------------------------------------------------- */
    /*  Early exit based on RCODE                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_earlyExit_notNoErrorOrNxDomain() throws Exception {
        Message response = new Message();
        response.addRecord(queryA("example.com."), Section.QUESTION);
        response.getHeader().setRcode(Rcode.FORMERR); // not NOERROR/NXDOMAIN

        Message result = response.normalize(newQueryMessage(queryA("example.com.")), false);
        assertSame(response, result, "Should return the original message when RCODE is not NOERROR/NXDOMAIN");
    }

    @Test
    void normalize_earlyExit_nxdomainContinues() throws Exception {
        Message response = new Message();
        response.addRecord(queryA("example.com."), Section.QUESTION);
        response.getHeader().setRcode(Rcode.NXDOMAIN); // allowed – must continue

        // No answer records – after normalisation we still get a new Message (not the same instance)
        Message result = response.normalize(newQueryMessage(queryA("example.com.")), false);
        assertNotSame(response, result);
        assertEquals(Rcode.NXDOMAIN, result.getHeader().getRcode());
    }

    /* ---------------------------------------------------------------------- */
    /*  DNAME handling – multiple entries                                    */
    /* ---------------------------------------------------------------------- */

    @ParameterizedTest(name = "throwOnIrrelevant={0} → expect {1}")
    @CsvSource({
        "true, exception",
        "false, null"
    })
    void normalize_dnameMultipleEntries(boolean throwOnIrrelevant, String expectation) throws Exception {
        Message response = new Message();
        Record q = queryA("example.com.");
        response.addRecord(q, Section.QUESTION);

        Name owner = Name.fromString("example.com.", Name.root);
        DNAMERecord d1 = new DNAMERecord(owner, DClass.IN, 300,
                Name.fromString("target1.example.com.", Name.root));
        DNAMERecord d2 = new DNAMERecord(owner, DClass.IN, 300,
                Name.fromString("target2.example.com.", Name.root));
        response.addRecord(d1, Section.ANSWER);
        response.addRecord(d2, Section.ANSWER);

        if (throwOnIrrelevant) {
            assertThrows(WireParseException.class,
                    () -> response.normalize(newQueryMessage(q), true));
        } else {
            Message result = response.normalize(newQueryMessage(q), false);
            assertNull(result, "Method should return null when DNAME RRset has >1 entries and throwOnIrrelevant=false");
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  DNAME handling – query type is DNAME (no synthesis)                  */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_dnameQueryTypeDname_noCnameSynthesis() throws Exception {
        Record q = new Record(Name.fromString("alias.example.com.", Name.root), Type.DNAME, DClass.IN) {
            @Override public String toString() { return ""; }
        };
        Message query = Message.newQuery(q);

        Message response = new Message();
        response.addRecord(q, Section.QUESTION);
        DNAMERecord dname = new DNAMERecord(Name.fromString("alias.example.com.", Name.root),
                DClass.IN, 300, Name.fromString("target.example.com.", Name.root));
        response.addRecord(dname, Section.ANSWER);

        Message norm = response.normalize(query, false);
        assertNotNull(norm);
        List<Record> ans = norm.getSection(Section.ANSWER);
        assertEquals(1, ans.size(), "Only the original DNAME must be present");
        assertTrue(ans.get(0) instanceof DNAMERecord);
    }

    /* ---------------------------------------------------------------------- */
    /*  DNAME handling – synthesize CNAME when appropriate                    */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_dnameSynthesisesCname() throws Exception {
        Record q = queryA("www.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        DNAMERecord dname = new DNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("target.com.", Name.root));
        resp.addRecord(dname, Section.ANSWER);

        Message norm = resp.normalize(query, false);
        assertNotNull(norm);
        List<Record> ans = norm.getSection(Section.ANSWER);
        assertEquals(2, ans.size(), "DNAME + synthesized CNAME expected");
        assertTrue(ans.stream().anyMatch(r -> r.getType() == Type.CNAME));
        CNAMERecord cname = (CNAMERecord) ans.stream()
                .filter(r -> r.getType() == Type.CNAME).findFirst().get();
        assertEquals(Name.fromString("www.example.com.", Name.root), cname.getName());
        assertEquals(Name.fromString("target.com.", Name.root), cname.getTarget());
        assertEquals(0, cname.getTTL(), "Synthesised CNAME must have TTL=0");
    }

    @Test
    void normalize_dnameExistingMatchingCnameNotDuplicated() throws Exception {
        Record q = queryA("www.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        DNAMERecord dname = new DNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("target.com.", Name.root));
        resp.addRecord(dname, Section.ANSWER);
        // Correct CNAME already present
        CNAMERecord cname = new CNAMERecord(Name.fromString("www.example.com.", Name.root),
                DClass.IN, 123, Name.fromString("target.com.", Name.root));
        resp.addRecord(cname, Section.ANSWER);

        Message norm = resp.normalize(query, false);
        List<Record> ans = norm.getSection(Section.ANSWER);
        // DNAME + existing CNAME (no extra synthesized)
        assertEquals(2, ans.size());
        long cnameCount = ans.stream().filter(r -> r.getType() == Type.CNAME).count();
        assertEquals(1, cnameCount);
    }

    @Test
    void normalize_dnameAnyQueryKeepsExtraRecords() throws Exception {
        Record q = new Record(Name.fromString("www.example.com.", Name.root), Type.ANY, DClass.IN) {
            @Override public String toString() { return ""; }
        };
        Message query = Message.newQuery(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        DNAMERecord dname = new DNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("target.com.", Name.root));
        resp.addRecord(dname, Section.ANSWER);
        // unrelated RR after DNAME (same original name)
        ARecord extra = new ARecord(Name.fromString("old.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        resp.addRecord(extra, Section.ANSWER);

        Message norm = resp.normalize(query, false);
        List<Record> ans = norm.getSection(Section.ANSWER);
        // DNAME + synthesized CNAME + extra A
        assertEquals(3, ans.size());
        assertTrue(ans.stream().anyMatch(r -> r.getType() == Type.A));
    }

    @Test
    void normalize_dnameNameTooLongThrowsWhenFlagTrue() throws Exception {
        Record q = queryA("www.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        // Create a DNAME whose target is deliberately too long (>255 bytes) to trigger NameTooLongException
        StringBuilder longTarget = new StringBuilder();
        for (int i = 0; i < 260; i++) longTarget.append('a');
        Name tooLong = Name.fromString(longTarget.toString() + ".example.com.", Name.root);
        DNAMERecord dname = new DNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, tooLong);
        resp.addRecord(dname, Section.ANSWER);

        assertThrows(WireParseException.class,
                () -> resp.normalize(query, true));
    }

    @Test
    void normalize_dnameNameTooLongReturnsNullWhenFlagFalse() throws Exception {
        Record q = queryA("www.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        StringBuilder longTarget = new StringBuilder();
        for (int i = 0; i < 260; i++) longTarget.append('a');
        Name tooLong = Name.fromString(longTarget.toString() + ".example.com.", Name.root);
        DNAMERecord dname = new DNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, tooLong);
        resp.addRecord(dname, Section.ANSWER);

        Message norm = resp.normalize(query, false);
        assertNull(norm);
    }

    /* ---------------------------------------------------------------------- */
    /*  Irrelevant records in ANSWER (name mismatch)                         */
    /* ---------------------------------------------------------------------- */

    @ParameterizedTest(name = "throw={0} → expect {1}")
    @CsvSource({
        "false, omitted",
        "true, exception"
    })
    void normalize_irrelevantAnswerName(boolean throwOnIrrelevant, String expectation) throws Exception {
        Record q = queryA("wanted.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        ARecord other = new ARecord(Name.fromString("other.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        resp.addRecord(other, Section.ANSWER);

        if (throwOnIrrelevant) {
            assertThrows(WireParseException.class,
                    () -> resp.normalize(query, true));
        } else {
            Message norm = resp.normalize(query, false);
            assertTrue(norm.getSection(Section.ANSWER).isEmpty(),
                    "Irrelevant answer should be removed");
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  CNAME handling – multiple CNAME RRs in one RRset                      */
    /* ---------------------------------------------------------------------- */

    @ParameterizedTest(name = "throw={0} → expect {1}")
    @CsvSource({
        "false, trimmed",
        "true, exception"
    })
    void normalize_multipleCnameRrs(boolean throwOnIrrelevant, String expectation) throws Exception {
        Record q = queryA("alias.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        // Build an RRset with two CNAME records (same name, different targets)
        CNAMERecord c1 = new CNAMERecord(Name.fromString("alias.example.com.", Name.root),
                DClass.IN, 300, Name.fromString("target1.example.com.", Name.root));
        CNAMERecord c2 = new CNAMERecord(Name.fromString("alias.example.com.", Name.root),
                DClass.IN, 300, Name.fromString("target2.example.com.", Name.root));
        // addRecord adds each record individually – they will end up in the same RRset
        resp.addRecord(c1, Section.ANSWER);
        resp.addRecord(c2, Section.ANSWER);

        if (throwOnIrrelevant) {
            assertThrows(WireParseException.class,
                    () -> resp.normalize(query, true));
        } else {
            Message norm = resp.normalize(query, false);
            List<Record> ans = norm.getSection(Section.ANSWER);
            assertEquals(1, ans.size(), "Only the first CNAME should be kept");
            CNAMERecord kept = (CNAMERecord) ans.get(0);
            assertEquals(Name.fromString("target1.example.com.", Name.root), kept.getTarget(),
                    "First CNAME must be retained");
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  CNAME handling – ANY query with extra records after CNAME             */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_anyQueryKeepsRecordsAfterCname() throws Exception {
        Record q = new Record(Name.fromString("alias.example.com.", Name.root), Type.ANY, DClass.IN) {
            @Override public String toString() { return ""; }
        };
        Message query = Message.newQuery(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        CNAMERecord cname = new CNAMERecord(Name.fromString("alias.example.com.", Name.root),
                DClass.IN, 300, Name.fromString("target.example.com.", Name.root));
        resp.addRecord(cname, Section.ANSWER);
        ARecord a = new ARecord(Name.fromString("alias.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("5.6.7.8"));
        resp.addRecord(a, Section.ANSWER);

        Message norm = resp.normalize(query, false);
        List<Record> ans = norm.getSection(Section.ANSWER);
        assertEquals(2, ans.size(), "Both CNAME and following A must be retained for ANY query");
    }

    /* ---------------------------------------------------------------------- */
    /*  Record type mismatch in ANSWER (non‑ANY query)                       */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_answerTypeMismatchIsIgnored() throws Exception {
        Record q = queryA("host.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        AAAARecord aaaa = new AAAARecord(Name.fromString("host.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("::1"));
        resp.addRecord(aaaa, Section.ANSWER);

        Message norm = resp.normalize(query, false);
        assertTrue(norm.getSection(Section.ANSWER).isEmpty(),
                "AAAA RRset should be omitted for A query");
    }

    /* ---------------------------------------------------------------------- */
    /*  Authority section – forbidden types (switch case)                     */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_authorityForbiddenTypesAreIgnored() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        // DNAME, CNAME, A, AAAA in authority – all must be ignored
        DNAMERecord dname = new DNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("t.example.com.", Name.root));
        CNAMERecord cname = new CNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("c.example.com.", Name.root));
        ARecord a = new ARecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        AAAARecord aaaa = new AAAARecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("::1"));

        resp.addRecord(dname, Section.AUTHORITY);
        resp.addRecord(cname, Section.AUTHORITY);
        resp.addRecord(a, Section.AUTHORITY);
        resp.addRecord(aaaa, Section.AUTHORITY);

        Message norm = resp.normalize(query, false);
        assertTrue(norm.getSection(Section.AUTHORITY).isEmpty(),
                "Forbidden types must be removed from AUTHORITY");
    }

    @Test
    void normalize_authorityForbiddenTypesThrowWhenFlagTrue() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        DNAMERecord dname = new DNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("t.example.com.", Name.root));
        resp.addRecord(dname, Section.AUTHORITY);

        assertThrows(WireParseException.class,
                () -> resp.normalize(query, true));
    }

    /* ---------------------------------------------------------------------- */
    /*  Authority section – disallowed type for section (e.g. SRV)           */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_authorityDisallowedTypeIsIgnored() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        // SRV is not allowed in AUTHORITY for this test
        SRVRecord srv = new SRVRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, 0, 5, 5060,
                Name.fromString("srv.example.com.", Name.root));
        resp.addRecord(srv, Section.AUTHORITY);

        // Spy to force isTypeAllowedInSection to return false for SRV
        Message spy = spyMessage(resp);
        doReturn(false).when(spy).isTypeAllowedInSection(eq(Type.SRV), eq(Section.AUTHORITY));

        Message norm = spy.normalize(query, false);
        assertTrue(norm.getSection(Section.AUTHORITY).isEmpty(),
                "Disallowed SRV RRset must be omitted");
    }

    @Test
    void normalize_authorityDisallowedTypeThrowsWhenFlagTrue() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        SRVRecord srv = new SRVRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, 0, 5, 5060,
                Name.fromString("srv.example.com.", Name.root));
        resp.addRecord(srv, Section.AUTHORITY);

        Message spy = spyMessage(resp);
        doReturn(false).when(spy).isTypeAllowedInSection(eq(Type.SRV), eq(Section.AUTHORITY));

        assertThrows(WireParseException.class,
                () -> spy.normalize(query, true));
    }

    /* ---------------------------------------------------------------------- */
    /*  Authority NS handling – subdomain check                               */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_authorityNsNotSubdomainIsIgnored() throws Exception {
        Record q = queryA("sub.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        NSRecord ns = new NSRecord(Name.fromString("other.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns.other.com.", Name.root));
        resp.addRecord(ns, Section.AUTHORITY);

        Message norm = resp.normalize(query, false);
        assertTrue(norm.getSection(Section.AUTHORITY).isEmpty(),
                "NS not subdomain of query should be ignored");
    }

    @Test
    void normalize_authorityNsNotSubdomainThrowsWhenFlagTrue() throws Exception {
        Record q = queryA("sub.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        NSRecord ns = new NSRecord(Name.fromString("other.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns.other.com.", Name.root));
        resp.addRecord(ns, Section.AUTHORITY);

        assertThrows(WireParseException.class,
                () -> resp.normalize(query, true));
    }

    /* ---------------------------------------------------------------------- */
    /*  Authority NS handling – NXDOMAIN or NODATA suppression                */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_authorityNsSuppressedOnNxDomain() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.getHeader().setRcode(Rcode.NXDOMAIN);
        resp.addRecord(q, Section.QUESTION);
        NSRecord ns = new NSRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns.example.com.", Name.root));
        resp.addRecord(ns, Section.AUTHORITY);

        Message norm = resp.normalize(query, false);
        assertTrue(norm.getSection(Section.AUTHORITY).isEmpty(),
                "NS must be omitted for NXDOMAIN responses");
    }

    @Test
    void normalize_authorityNsSuppressedOnNoErrorNodata() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.getHeader().setRcode(Rcode.NOERROR);
        resp.addRecord(q, Section.QUESTION);
        // No answer records – NODATA scenario
        SOARecord soa = new SOARecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300,
                Name.fromString("ns.example.com.", Name.root),
                Name.fromString("hostmaster.example.com.", Name.root),
                1, 7200, 3600, 1209600, 3600);
        NSRecord ns = new NSRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns.example.com.", Name.root));
        resp.addRecord(soa, Section.AUTHORITY);
        resp.addRecord(ns, Section.AUTHORITY);

        Message norm = resp.normalize(query, false);
        // NS must be removed, SOA kept
        List<Record> auth = norm.getSection(Section.AUTHORITY);
        assertEquals(1, auth.size());
        assertEquals(Type.SOA, auth.get(0).getType());
    }

    /* ---------------------------------------------------------------------- */
    /*  Authority NS handling – duplicate NS RRsets                           */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_authorityDuplicateNsKeepsFirstOnly() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        NSRecord ns1 = new NSRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns1.example.com.", Name.root));
        NSRecord ns2 = new NSRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns2.example.com.", Name.root));
        resp.addRecord(ns1, Section.AUTHORITY);
        resp.addRecord(ns2, Section.AUTHORITY);

        Message norm = resp.normalize(query, false);
        List<Record> auth = norm.getSection(Section.AUTHORITY);
        assertEquals(1, auth.size(), "Only the first NS RRset should be kept");
        assertEquals(Name.fromString("ns1.example.com.", Name.root), ((NSRecord) auth.get(0)).getTarget());
    }

    @Test
    void normalize_authorityDuplicateNsThrowsWhenFlagTrue() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        NSRecord ns1 = new NSRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns1.example.com.", Name.root));
        NSRecord ns2 = new NSRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns2.example.com.", Name.root));
        resp.addRecord(ns1, Section.AUTHORITY);
        resp.addRecord(ns2, Section.AUTHORITY);

        assertThrows(WireParseException.class,
                () -> resp.normalize(query, true));
    }

    /* ---------------------------------------------------------------------- */
    /*  Successful normalisation – overall flow                               */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_successfulOverallFlow() throws Exception {
        // Query
        Record q = queryA("www.example.com.");
        Message query = newQueryMessage(q);

        // Response
        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);

        // CNAME chain
        CNAMERecord cname = new CNAMERecord(Name.fromString("www.example.com.", Name.root),
                DClass.IN, 300, Name.fromString("alias.example.com.", Name.root));
        resp.addRecord(cname, Section.ANSWER);
        ARecord a = new ARecord(Name.fromString("alias.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("5.6.7.8"));
        resp.addRecord(a, Section.ANSWER);

        // Authority NS
        NSRecord ns = new NSRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns1.example.com.", Name.root));
        resp.addRecord(ns, Section.AUTHORITY);

        // Additional A for the NS host
        ARecord addA = new ARecord(Name.fromString("ns1.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        resp.addRecord(addA, Section.ADDITIONAL);

        Message norm = resp.normalize(query, false);
        // Verify answer section
        List<Record> ans = norm.getSection(Section.ANSWER);
        assertEquals(2, ans.size());
        assertTrue(ans.stream().anyMatch(r -> r.getType() == Type.CNAME));
        assertTrue(ans.stream().anyMatch(r -> r.getType() == Type.A));

        // Verify authority section
        List<Record> auth = norm.getSection(Section.AUTHORITY);
        assertEquals(1, auth.size());
        assertEquals(Type.NS, auth.get(0).getType());

        // Verify additional section
        List<Record> add = norm.getSection(Section.ADDITIONAL);
        assertEquals(1, add.size());
        assertEquals(Type.A, add.get(0).getType());

        // Header counts must match the sections
        Header hdr = norm.getHeader();
        assertEquals(ans.size(), hdr.getCount(Section.ANSWER));
        assertEquals(auth.size(), hdr.getCount(Section.AUTHORITY));
        assertEquals(add.size(), hdr.getCount(Section.ADDITIONAL));
    }

    /* ---------------------------------------------------------------------- */
    /*  CNAME query type – do not follow CNAME                               */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_cnameQueryDoesNotFollow() throws Exception {
        Record q = new Record(Name.fromString("alias.example.com.", Name.root), Type.CNAME, DClass.IN) {
            @Override public String toString() { return ""; }
        };
        Message query = Message.newQuery(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        CNAMERecord cname = new CNAMERecord(Name.fromString("alias.example.com.", Name.root),
                DClass.IN, 300, Name.fromString("target.example.com.", Name.root));
        ARecord a = new ARecord(Name.fromString("alias.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        resp.addRecord(cname, Section.ANSWER);
        resp.addRecord(a, Section.ANSWER);

        Message norm = resp.normalize(query, false);
        List<Record> ans = norm.getSection(Section.ANSWER);
        // Only the CNAME should survive; the A record is ignored because query type is CNAME
        assertEquals(1, ans.size());
        assertEquals(Type.CNAME, ans.get(0).getType());
    }

    /* ---------------------------------------------------------------------- */
    /*  DNAME with mismatching existing CNAME – synthesize new CNAME           */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_dnameMismatchingCnameSynthesisesCorrectOne() throws Exception {
        Record q = queryA("www.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        DNAMERecord dname = new DNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("target.com.", Name.root));
        resp.addRecord(dname, Section.ANSWER);
        // Wrong CNAME after DNAME
        CNAMERecord wrongCname = new CNAMERecord(Name.fromString("www.example.com.", Name.root),
                DClass.IN, 300, Name.fromString("wrong.example.com.", Name.root));
        resp.addRecord(wrongCname, Section.ANSWER);

        Message norm = resp.normalize(query, false);
        List<Record> ans = norm.getSection(Section.ANSWER);
        // Should contain DNAME + synthesized correct CNAME (wrong one omitted)
        assertEquals(2, ans.size());
        CNAMERecord synthesized = (CNAMERecord) ans.stream()
                .filter(r -> r.getType() == Type.CNAME).findFirst().get();
        assertEquals(Name.fromString("target.com.", Name.root), synthesized.getTarget());
    }

    /* ---------------------------------------------------------------------- */
    /*  Authority section – allowed non‑NS type and related additional record */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_authorityAllowedMxKeepsAdditional() throws Exception {
        // Query for MX
        Record q = new Record(Name.fromString("mail.example.com.", Name.root), Type.MX, DClass.IN) {
            @Override public String toString() { return ""; }
        };
        Message query = Message.newQuery(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        MXRecord mx = new MXRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, 10,
                Name.fromString("mailhost.example.com.", Name.root));
        resp.addRecord(mx, Section.AUTHORITY);
        // Additional A for the MX host
        ARecord addA = new ARecord(Name.fromString("mailhost.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("9.9.9.9"));
        resp.addRecord(addA, Section.ADDITIONAL);

        // Spy to make MX allowed in AUTHORITY
        Message spy = spyMessage(resp);
        doReturn(true).when(spy).isTypeAllowedInSection(eq(Type.MX), eq(Section.AUTHORITY));

        Message norm = spy.normalize(query, false);
        List<Record> auth = norm.getSection(Section.AUTHORITY);
        assertEquals(1, auth.size());
        assertEquals(Type.MX, auth.get(0).getType());

        List<Record> add = norm.getSection(Section.ADDITIONAL);
        assertEquals(1, add.size());
        assertEquals(Type.A, add.get(0).getType());
    }

    /* ---------------------------------------------------------------------- */
    /*  Empty ANSWER section – only AUTHORITY and ADDITIONAL processed       */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_emptyAnswerProcessesAuthorityAndAdditional() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        NSRecord ns = new NSRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns1.example.com.", Name.root));
        resp.addRecord(ns, Section.AUTHORITY);
        ARecord addA = new ARecord(Name.fromString("ns1.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        resp.addRecord(addA, Section.ADDITIONAL);

        Message norm = resp.normalize(query, false);
        assertTrue(norm.getSection(Section.ANSWER).isEmpty());
        assertEquals(1, norm.getSection(Section.AUTHORITY).size());
        assertEquals(1, norm.getSection(Section.ADDITIONAL).size());
    }

    /* ---------------------------------------------------------------------- */
    /*  ANY query – retain all answer types regardless of query type           */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_anyQueryKeepsAllAnswerTypes() throws Exception {
        Record q = new Record(Name.fromString("host.example.com.", Name.root), Type.ANY, DClass.IN) {
            @Override public String toString() { return ""; }
        };
        Message query = Message.newQuery(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        ARecord a = new ARecord(Name.fromString("host.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("1.1.1.1"));
        AAAARecord aaaa = new AAAARecord(Name.fromString("host.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("::1"));
        TXTRecord txt = new TXTRecord(Name.fromString("host.example.com.", Name.root),
                DClass.IN, 300, "hello");
        resp.addRecord(a, Section.ANSWER);
        resp.addRecord(aaaa, Section.ANSWER);
        resp.addRecord(txt, Section.ANSWER);

        Message norm = resp.normalize(query, false);
        List<Record> ans = norm.getSection(Section.ANSWER);
        assertEquals(3, ans.size(), "ALL three RRsets must be retained for ANY query");
    }

    /* ---------------------------------------------------------------------- */
    /*  Irrelevant NS in AUTHORITY with throwOnIrrelevantRecord = true        */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_irrelevantNsInAuthorityThrowsWhenFlagTrue() throws Exception {
        Record q = queryA("sub.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        NSRecord ns = new NSRecord(Name.fromString("other.com.", Name.root),
                DClass.IN, 300, Name.fromString("ns.other.com.", Name.root));
        resp.addRecord(ns, Section.AUTHORITY);

        assertThrows(WireParseException.class,
                () -> resp.normalize(query, true));
    }

    /* ---------------------------------------------------------------------- */
    /*  Answer section – type mismatch with throwOnIrrelevantRecord true      */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_answerTypeMismatchThrowsWhenFlagTrue() throws Exception {
        Record q = queryA("host.example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        AAAARecord aaaa = new AAAARecord(Name.fromString("host.example.com.", Name.root),
                DClass.IN, 300, InetAddress.getByName("::1"));
        resp.addRecord(aaaa, Section.ANSWER);

        assertThrows(WireParseException.class,
                () -> resp.normalize(query, true));
    }

    /* ---------------------------------------------------------------------- */
    /*  Authority section – forbidden types with throwOnIrrelevantRecord true   */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_authorityForbiddenTypesThrowWhenFlagTrue() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        DNAMERecord dname = new DNAMERecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, Name.fromString("t.example.com.", Name.root));
        resp.addRecord(dname, Section.AUTHORITY);

        assertThrows(WireParseException.class,
                () -> resp.normalize(query, true));
    }

    /* ---------------------------------------------------------------------- */
    /*  Authority section – disallowed type with throwOnIrrelevantRecord true   */
    /* ---------------------------------------------------------------------- */

    @Test
    void normalize_authorityDisallowedTypeThrowsWhenFlagTrue() throws Exception {
        Record q = queryA("example.com.");
        Message query = newQueryMessage(q);

        Message resp = new Message();
        resp.addRecord(q, Section.QUESTION);
        SRVRecord srv = new SRVRecord(Name.fromString("example.com.", Name.root),
                DClass.IN, 300, 0, 5, 5060,
                Name.fromString("srv.example.com.", Name.root));
        resp.addRecord(srv, Section.AUTHORITY);

        Message spy = spyMessage(resp);
        doReturn(false).when(spy).isTypeAllowedInSection(eq(Type.SRV), eq(Section.AUTHORITY));

        assertThrows(WireParseException.class,
                () -> spy.normalize(query, true));
    }
}
```