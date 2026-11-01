```java
package org.xbill.DNS;

import java.net.UnknownHostException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

/**
 * Tests for {@link Message#normalize(Message, boolean)}.
 *
 * The original file contained a duplicated implementation of many
 * {@code Message} internals (Header, sections, normalize, …) which
 * conflicted with the real {@code org.xbill.DNS.Message} class from the
 * dnsjava library.  Those duplicated members caused compilation errors
 * such as “cannot convert Message_normalize_2_Test to Message” and
 * missing imports for {@code ArrayList}, {@code HashSet}, etc.
 *
 * The test suite only needs to exercise the library implementation,
 * therefore the duplicated code has been removed.  The class now
 * consists solely of the test methods that operate on the real
 * {@code Message} class.
 */
@ExtendWith(MockitoExtension.class)
class Message_normalize_2_Test {

    /**
     * When a DNAME RRset contains more than one record the method must abort with a
     * {@link WireParseException} (if {@code throwOnIrrelevantRecord} is {@code true}).
     */
    @Test
    void normalize_throwsOnMultipleDNAMERecords() throws UnknownHostException {
        Name qname = Name.fromConstantString("example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        DNAMERecord dname1 = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("target1.example.com."));
        DNAMERecord dname2 = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("target2.example.com."));
        response.addRecord(dname1, Section.ANSWER);
        response.addRecord(dname2, Section.ANSWER);
        assertThrows(WireParseException.class,
                () -> response.normalize(query, true));
    }

    /**
     * Irrelevant answer records (records whose owner name does not match the query name) are
     * ignored when {@code throwOnIrrelevantRecord} is {@code false}. The returned message should
     * contain no such records.
     */
    @Test
    void normalize_ignoresIrrelevantAnswerRecordWhenNotThrowing()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("example.com.");
        Record queryRecord = Record.newRecord(qname, Type.MX, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        Name otherName = Name.fromConstantString("other.com.");
        ARecord irrelevant = new ARecord(otherName, DClass.IN, 300,
                InetAddress.getByName("1.2.3.4"));
        response.addRecord(irrelevant, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        assertTrue(cleaned.getSection(Section.ANSWER).isEmpty(),
                "Irrelevant answer records should be removed");
    }

    /**
     * When a single valid DNAME record is present and the query type is *not* DNAME,
     * the method must synthesize a CNAME that points to the DNAME target.
     */
    @Test
    void normalize_synthesisesCNAMEFromDNAME()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        Name dnameTarget = Name.fromConstantString("target.example.org.");
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300, dnameTarget);
        response.addRecord(dname, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        assertEquals(2, cleaned.getSection(Section.ANSWER).size(),
                "Answer section should contain DNAME + synthesized CNAME");
        Record first = cleaned.getSection(Section.ANSWER).get(0);
        assertTrue(first instanceof DNAMERecord);
        assertEquals(dname, first);
        Record second = cleaned.getSection(Section.ANSWER).get(1);
        assertTrue(second instanceof CNAMERecord,
                "Second record should be a CNAME synthesized from the DNAME");
        CNAMERecord cname = (CNAMERecord) second;
        Name expectedCnameTarget = qname.fromDNAME(dname);
        assertEquals(expectedCnameTarget, cname.getTarget(),
                "Synthesised CNAME target must be derived from the DNAME");
    }

    /**
     * Early‑rcode check – response with a non‑normalised rcode should be returned unchanged.
     */
    @Test
    void normalize_earlyReturnOnUnsupportedRcode() throws Exception {
        Name qname = Name.fromConstantString("example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        response.getHeader().setRcode(Rcode.SERVFAIL);
        Message result = response.normalize(query, false);
        assertSame(response, result,
                "Message should be returned unchanged for unsupported rcode");
    }

    /**
     * DNAME handling – when the query itself asks for DNAME no CNAME synthesis occurs.
     */
    @Test
    void normalize_dnameQueryNoSynthesis()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("d.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.DNAME, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("target.example.com."));
        response.addRecord(dname, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        assertEquals(1, cleaned.getSection(Section.ANSWER).size(),
                "Only the original DNAME should be present");
        assertTrue(cleaned.getSection(Section.ANSWER).get(0) instanceof DNAMERecord);
    }

    /**
     * DNAME handling – multiple entries, throwOnIrrelevantRecord = false returns null.
     */
    @Test
    void normalize_multipleDNAMEReturnsNullWhenNotThrowing() throws Exception {
        Name qname = Name.fromConstantString("x.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        DNAMERecord d1 = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("t1.example.com."));
        DNAMERecord d2 = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("t2.example.com."));
        response.addRecord(d1, Section.ANSWER);
        response.addRecord(d2, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        assertNull(cleaned,
                "Method should return null when multiple DNAME records and not throwing");
    }

    /**
     * DNAME handling – existing matching CNAME prevents synthesis.
     */
    @Test
    void normalize_existingMatchingCNAMESkipsSynthesis()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("bar.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("d.example.com."));
        response.addRecord(dname, Section.ANSWER);
        Name expectedTarget = qname.fromDNAME(dname);
        CNAMERecord matchingCname = new CNAMERecord(qname, DClass.IN, 300,
                expectedTarget);
        response.addRecord(matchingCname, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        assertEquals(2, cleaned.getSection(Section.ANSWER).size(),
                "Answer should contain DNAME and the original matching CNAME only");
        assertTrue(cleaned.getSection(Section.ANSWER).contains(dname));
        assertTrue(cleaned.getSection(Section.ANSWER).contains(matchingCname));
    }

    /**
     * Irrelevant answer RRset with throwOnIrrelevantRecord = true triggers exception.
     */
    @Test
    void normalize_irrelevantAnswerThrowsWhenStrict()
            throws UnknownHostException {
        Name qname = Name.fromConstantString("target.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        Name other = Name.fromConstantString("other.example.com.");
        ARecord irrelevant = new ARecord(other, DClass.IN, 300,
                InetAddress.getByName("5.6.7.8"));
        response.addRecord(irrelevant, Section.ANSWER);
        assertThrows(WireParseException.class,
                () -> response.normalize(query, true),
                "Irrelevant RRset should cause WireParseException when strict");
    }

    /**
     * CNAME handling – multiple CNAME records, throwOnIrrelevantRecord = false trims to first.
     */
    @Test
    void normalize_cnameMultipleRecordsTrimmedWhenNotThrowing()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("alias.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        CNAMERecord c1 = new CNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("mid.example.com."));
        CNAMERecord c2 = new CNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("final.example.com."));
        response.addRecord(c1, Section.ANSWER);
        response.addRecord(c2, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        assertEquals(1, cleaned.getSection(Section.ANSWER).size(),
                "Only one CNAME should survive trimming");
        Record kept = cleaned.getSection(Section.ANSWER).get(0);
        assertTrue(kept instanceof CNAMERecord);
        assertEquals(c1.getTarget(),
                ((CNAMERecord) kept).getTarget(),
                "The kept CNAME must be the first one");
    }

    /**
     * Authority section – forbidden types cause exception when strict.
     */
    @Test
    void normalize_authorityForbiddenTypesThrowWhenStrict()
            throws UnknownHostException {
        Name qname = Name.fromConstantString("foo.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("target.example.com."));
        CNAMERecord cname = new CNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("cname.example.com."));
        ARecord a = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("2.2.2.2"));
        AAAARecord aaaa = new AAAARecord(qname, DClass.IN, 300,
                InetAddress.getByName("::1"));
        response.addRecord(dname, Section.AUTHORITY);
        response.addRecord(cname, Section.AUTHORITY);
        response.addRecord(a, Section.AUTHORITY);
        response.addRecord(aaaa, Section.AUTHORITY);
        assertThrows(WireParseException.class,
                () -> response.normalize(query, true),
                "Any forbidden authority RRset should cause exception when strict");
    }

    /**
     * Authority section – disallowed type (isTypeAllowedInSection = false)
     */
    @Test
    void normalize_authorityDisallowedTypeIgnored()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("foo.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = spy(new Message());
        int SRV = Type.SRV;
        doReturn(false).when(response).isTypeAllowedInSection(eq(SRV), eq(Section.AUTHORITY));
        Name srvName = Name.fromConstantString("_sip._tcp.example.com.");
        SRVRecord srv = new SRVRecord(srvName, DClass.IN, 300,
                10, 5, 5060,
                Name.fromConstantString("sipserver.example.com."));
        response.addRecord(srv, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertTrue(cleaned.getSection(Section.AUTHORITY).isEmpty(),
                "Disallowed SRV RRset must be ignored");
    }

    /**
     * Authority NS handling – subdomain mismatch (throwOnIrrelevantRecord = false).
     */
    @Test
    void normalize_authorityNsSubdomainMismatchIgnored()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("sub.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        Name nsName = Name.fromConstantString("unrelated.example.com.");
        NSRecord ns = new NSRecord(nsName, DClass.IN, 300,
                Name.fromConstantString("ns1.unrelated.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertTrue(cleaned.getSection(Section.AUTHORITY).isEmpty(),
                "NS not a subdomain should be ignored");
    }

    /**
     * Authority NS handling – duplicate NS records (second one ignored).
     */
    @Test
    void normalize_authorityDuplicateNsIgnored()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("www.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        NSRecord ns1 = new NSRecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns1.example.com."));
        NSRecord ns2 = new NSRecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns2.example.com."));
        response.addRecord(ns1, Section.AUTHORITY);
        response.addRecord(ns2, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertEquals(1, cleaned.getSection(Section.AUTHORITY).size(),
                "Only the first NS should be kept");
        Record kept = cleaned.getSection(Section.AUTHORITY).get(0);
        assertTrue(kept instanceof NSRecord);
        assertEquals(ns1.getTarget(),
                ((NSRecord) kept).getTarget(),
                "The kept NS must be the first one added");
    }

    /**
     * Authority NS handling – NXDOMAIN response suppresses NS records.
     */
    @Test
    void normalize_authorityNsSuppressedOnNxDomain()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("doesnotexist.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NXDOMAIN);
        NSRecord ns = new NSRecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertTrue(cleaned.getSection(Section.AUTHORITY).isEmpty(),
                "NS records must be suppressed for NXDOMAIN responses");
    }

    /**
     * Authority NS handling – NODATA response (NOERROR + SOA + empty answer) suppresses NS.
     */
    @Test
    void normalize_authorityNsSuppressedOnNoData()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("empty.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NOERROR);
        SOARecord soa = new SOARecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns.example.com."),
                Name.fromConstantString("hostmaster.example.com."),
                1, 7200, 3600, 1209600, 3600);
        response.addRecord(soa, Section.AUTHORITY);
        NSRecord ns = new NSRecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertEquals(1, cleaned.getSection(Section.AUTHORITY).size(),
                "Only SOA should be kept in NODATA response");
        assertTrue(cleaned.getSection(Section.AUTHORITY).get(0) instanceof SOARecord);
    }

    /**
     * Authority allowed type retained when isTypeAllowedInSection returns true (e.g., MX).
     */
    @Test
    void normalize_authorityAllowedTypeRetained()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("mail.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.MX, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        MXRecord mx = new MXRecord(Name.fromConstantString("example.com."), DClass.IN, 300,
                10, Name.fromConstantString("mail.example.com."));
        response.addRecord(mx, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertEquals(1, cleaned.getSection(Section.AUTHORITY).size(),
                "Allowed MX RRset should be retained");
        assertTrue(cleaned.getSection(Section.AUTHORITY).get(0) instanceof MXRecord);
    }

    /**
     * Additional section – only referenced records are kept.
     */
    @Test
    void normalize_additionalOnlyReferencedKept()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("host.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        ARecord a = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("1.2.3.4"));
        response.addRecord(a, Section.ANSWER);
        ARecord referenced = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("5.6.7.8"));
        ARecord unreferenced = new ARecord(Name.fromConstantString("unrelated.example.com."), DClass.IN, 300,
                InetAddress.getByName("9.10.11.12"));
        response.addRecord(referenced, Section.ADDITIONAL);
        response.addRecord(unreferenced, Section.ADDITIONAL);
        Message cleaned = response.normalize(query, false);
        assertEquals(1, cleaned.getSection(Section.ADDITIONAL).size(),
                "Only referenced additional records should be kept");
        assertEquals(referenced, cleaned.getSection(Section.ADDITIONAL).get(0));
    }

    /**
     * ANY query – after CNAME processing, subsequent records with original name are copied.
     */
    @Test
    void normalize_anyQueryCopiesFollowingRecords()
            throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("any.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.ANY, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        CNAMERecord cname = new CNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("alias.example.com."));
        response.addRecord(cname, Section.ANSWER);
        ARecord a = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("1.1.1.1"));
        response.addRecord(a, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        assertEquals(2, cleaned.getSection(Section.ANSWER).size(),
                "CNAME and following record with original name should be kept for ANY query");
        assertTrue(cleaned.getSection(Section.ANSWER).contains(cname));
        assertTrue(cleaned.getSection(Section.ANSWER).contains(a));
    }

    /**
     * Authority forbidden types – strict mode throws for each forbidden type.
     */
    @Test
    void normalize_authorityMultipleForbiddenTypesThrowWhenStrict()
            throws UnknownHostException {
        Name qname = Name.fromConstantString("foo.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("target.example.com."));
        CNAMERecord cname = new CNAMERecord(qname, DClass.IN, 300,
                Name.fromConstantString("cname.example.com."));
        ARecord a = new ARecord(qname, DClass.IN, 300,
                InetAddress.getByName("2.2.2.2"));
        AAAARecord aaaa = new AAAARecord(qname, DClass.IN, 300,
                InetAddress.getByName("::1"));
        response.addRecord(dname, Section.AUTHORITY);
        response.addRecord(cname, Section.AUTHORITY);
        response.addRecord(a, Section.AUTHORITY);
        response.addRecord(aaaa, Section.AUTHORITY);
        assertThrows(WireParseException.class,
                () -> response.normalize(query, true),
                "Any forbidden authority RRset should cause exception when strict");
    }

    /**
     * Authority NS handling – strict mode throws on subdomain mismatch.
     */
    @Test
    void normalize_authorityNsSubdomainMismatchThrowsWhenStrict()
            throws UnknownHostException {
        Name qname = Name.fromConstantString("sub.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        Name nsName = Name.fromConstantString("unrelated.example.com.");
        NSRecord ns = new NSRecord(nsName, DClass.IN, 300,
                Name.fromConstantString("ns.unrelated.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        assertThrows(WireParseException.class,
                () -> response.normalize(query, true),
                "NS subdomain mismatch should throw when strict");
    }

    /**
     * Authority NS handling – strict mode throws on duplicate NS.
     */
    @Test
    void normalize_authorityDuplicateNsThrowsWhenStrict()
            throws UnknownHostException {
        Name qname = Name.fromConstantString("www.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        NSRecord ns1 = new NSRecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns1.example.com."));
        NSRecord ns2 = new NSRecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns2.example.com."));
        response.addRecord(ns1, Section.AUTHORITY);
        response.addRecord(ns2, Section.AUTHORITY);
        assertThrows(WireParseException.class,
                () -> response.normalize(query, true),
                "Duplicate NS should cause exception when strict");
    }

    /**
     * Authority NS handling – strict mode throws on NXDOMAIN suppression.
     */
    @Test
    void normalize_authorityNsNxDomainThrowsWhenStrict()
            throws UnknownHostException {
        Name qname = Name.fromConstantString("doesnotexist.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NXDOMAIN);
        NSRecord ns = new NSRecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        assertThrows(WireParseException.class,
                () -> response.normalize(query, true),
                "NS in NXDOMAIN response should cause exception when strict");
    }

    /**
     * Authority NS handling – strict mode throws on NODATA suppression.
     */
    @Test
    void normalize_authorityNsNoDataThrowsWhenStrict()
            throws UnknownHostException {
        Name qname = Name.fromConstantString("empty.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NOERROR);
        SOARecord soa = new SOARecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns.example.com."),
                Name.fromConstantString("hostmaster.example.com."),
                1, 7200, 3600, 1209600, 3600);
        response.addRecord(soa, Section.AUTHORITY);
        NSRecord ns = new NSRecord(qname, DClass.IN, 300,
                Name.fromConstantString("ns.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        assertThrows(WireParseException.class,
                () -> response.normalize(query, true),
                "NS in NODATA response should cause exception when strict");
    }
}
```