


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.UnknownHostException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.net.InetAddress;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class Message_normalize_2_Test {

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
        DNAMERecord dname1 = new DNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("target1.example.com."));
        DNAMERecord dname2 = new DNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("target2.example.com."));
        response.addRecord(dname1, Section.ANSWER);
        response.addRecord(dname2, Section.ANSWER);
        // The method should throw a WireParseException because we asked it to be strict.
        assertThrows(WireParseException.class, () -> response.normalize(query, true));
    }

    /**
     * Irrelevant answer records (records whose owner name Message_normalize_2_Testnot match the query name) are
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
        assertTrue(cleaned.getSection(Section.ANSWER).isEmpty(), "Irrelevant answer records should be removed");
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
        assertEquals(2, cleaned.getSection(Section.ANSWER).size(), "Answer section should contain DNAME + synthesized CNAME");
        // Verify the first RR is the DNAME we added.
        Record first = cleaned.getSection(Section.ANSWER).get(0);
        assertTrue(first instanceof DNAMERecord);
        assertEquals(dname, first);
        // Verify the second RR is a CNAME whose target is the DNAME target.
        Record second = cleaned.getSection(Section.ANSWER).get(1);
        assertTrue(second instanceof CNAMERecord, "Second record should be a CNAME synthesized from the DNAME");
        CNAMERecord cname = (CNAMERecord) second;
        // The synthesized CNAME target is the result of Name.fromDNAME(dname)
        Name expectedCnameTarget = qname.fromDNAME(dname);
        assertEquals(expectedCnameTarget, cname.getTarget(), "Synthesised CNAME target must be derived from the DNAME");
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
        // not NOERROR / NXDOMAIN
        response.getHeader().setRcode(Rcode.SERVFAIL);
        Message result = response.normalize(query, false);
        // The method returns the *same* instance when it early‑returns.
        assertSame(response, result, "Message should be returned unchanged for unsupported rcode");
    }

    /**
     * DNAME handling – when the query itself asks for DNAME no CNAME synthesis occurs.
     */
    @Test
    void normalize_dnameQueryNoSynthesis() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("d.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.DNAME, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("target.example.com."));
        response.addRecord(dname, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        assertEquals(1, cleaned.getSection(Section.ANSWER).size(), "Only the original DNAME should be present");
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
        DNAMERecord d1 = new DNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("t1.example.com."));
        DNAMERecord d2 = new DNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("t2.example.com."));
        response.addRecord(d1, Section.ANSWER);
        response.addRecord(d2, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        assertNull(cleaned, "Method should return null when multiple DNAME records and not throwing");
    }

    /**
     * DNAME handling – existing matching CNAME prevents synthesis.
     */
    @Test
    void normalize_existingMatchingCNAMESkipsSynthesis() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("bar.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        // DNAME that maps bar.example.com. -> d.example.com.
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("d.example.com."));
        response.addRecord(dname, Section.ANSWER);
        // Expected CNAME that would be synthesized.
        Name expectedTarget = qname.fromDNAME(dname);
        CNAMERecord matchingCname = new CNAMERecord(qname, DClass.IN, 300, expectedTarget);
        response.addRecord(matchingCname, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        // Should contain exactly the two records we added – no extra synthesized CNAME.
        assertEquals(2, cleaned.getSection(Section.ANSWER).size(), "Answer should contain DNAME and the original matching CNAME only");
        assertTrue(cleaned.getSection(Section.ANSWER).contains(dname));
        assertTrue(cleaned.getSection(Section.ANSWER).contains(matchingCname));
    }

    /**
     * Irrelevant answer RRset with throwOnIrrelevantRecord = true triggers exception.
     */
    @Test
    void normalize_irrelevantAnswerThrowsWhenStrict() throws UnknownHostException {
        Name qname = Name.fromConstantString("target.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        Name other = Name.fromConstantString("other.example.com.");
        ARecord irrelevant = new ARecord(other, DClass.IN, 300, InetAddress.getByName("5.6.7.8"));
        response.addRecord(irrelevant, Section.ANSWER);
        assertThrows(WireParseException.class, () -> response.normalize(query, true), "Irrelevant RRset should cause WireParseException when strict");
    }

    /**
     * CNAME handling – multiple CNAME records, throwOnIrrelevantRecord = false trims to first.
     */
    @Test
    void normalize_cnameMultipleRecordsTrimmedWhenNotThrowing() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("alias.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        CNAMERecord c1 = new CNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("mid.example.com."));
        CNAMERecord c2 = new CNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("final.example.com."));
        // Manually create an RRset with two CNAMEs.
        RRset rrset = new RRset();
        rrset.addRR(c1);
        rrset.addRR(c2);
        response.addRecord(c1, Section.ANSWER);
        // both end up in the same RRset internally
        response.addRecord(c2, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        // After normalization only the first CNAME should remain.
        assertEquals(1, cleaned.getSection(Section.ANSWER).size(), "Only one CNAME should survive trimming");
        Record kept = cleaned.getSection(Section.ANSWER).get(0);
        assertTrue(kept instanceof CNAMERecord);
        assertEquals(c1.getTarget(), ((CNAMERecord) kept).getTarget(), "The kept CNAME must be the first one");
    }

    /**
     * Authority section – forbidden types cause exception when strict.
     */
    @Test
    void normalize_authorityForbiddenTypesThrowWhenStrict() throws UnknownHostException {
        Name qname = Name.fromConstantString("foo.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        // Add a DNAME in authority – forbidden by the switch.
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("target.example.com."));
        response.addRecord(dname, Section.AUTHORITY);
        assertThrows(WireParseException.class, () -> response.normalize(query, true), "Forbidden authority RRset should cause exception when strict");
    }

    /**
     * Authority section – disallowed type (isTypeAllowedInSection = false)
     */
    @Test
    void normalize_authorityDisallowedTypeIgnored() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("foo.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        // Spy on the response to stub isTypeAllowedInSection for SRV (type 33)
        Message response = spy(new Message());
        // SRV type constant
        int SRV = Type.SRV;
        // Force the stub to return false for SRV in AUTHORITY.
        doReturn(false).when(response).isTypeAllowedInSection(eq(SRV), eq(Section.AUTHORITY));
        // Add an SRV RRset to authority.
        Name srvName = Name.fromConstantString("_sip._tcp.example.com.");
        SRVRecord srv = new SRVRecord(srvName, DClass.IN, 300, 10, 5, 5060, Name.fromConstantString("sipserver.example.com."));
        response.addRecord(srv, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        // The SRV RRset should be omitted.
        assertTrue(cleaned.getSection(Section.AUTHORITY).isEmpty(), "Disallowed SRV RRset must be ignored");
    }

    /**
     * Authority NS handling – subdomain mismatch (throwOnIrrelevantRecord = false).
     */
    @Test
    void normalize_authorityNsSubdomainMismatchIgnored() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("sub.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        // NS name not a subdomain of the query name.
        Name nsName = Name.fromConstantString("unrelated.example.com.");
        NSRecord ns = new NSRecord(nsName, DClass.IN, 300, Name.fromConstantString("ns1.unrelated.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertTrue(cleaned.getSection(Section.AUTHORITY).isEmpty(), "NS not a subdomain should be ignored");
    }

    /**
     * Authority NS handling – duplicate NS records (second one ignored).
     */
    @Test
    void normalize_authorityDuplicateNsIgnored() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("www.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        // First NS
        NSRecord ns1 = new NSRecord(qname, DClass.IN, 300, Name.fromConstantString("ns1.example.com."));
        // Second NS (duplicate)
        NSRecord ns2 = new NSRecord(qname, DClass.IN, 300, Name.fromConstantString("ns2.example.com."));
        response.addRecord(ns1, Section.AUTHORITY);
        response.addRecord(ns2, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertEquals(1, cleaned.getSection(Section.AUTHORITY).size(), "Only the first NS should be kept");
        Record kept = cleaned.getSection(Section.AUTHORITY).get(0);
        assertTrue(kept instanceof NSRecord);
        assertEquals(ns1.getTarget(), ((NSRecord) kept).getTarget(), "The kept NS must be the first one added");
    }

    /**
     * Authority NS handling – NXDOMAIN response suppresses NS records.
     */
    @Test
    void normalize_authorityNsSuppressedOnNxDomain() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("doesnotexist.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NXDOMAIN);
        NSRecord ns = new NSRecord(qname, DClass.IN, 300, Name.fromConstantString("ns.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertTrue(cleaned.getSection(Section.AUTHORITY).isEmpty(), "NS records must be suppressed for NXDOMAIN responses");
    }

    /**
     * Authority NS handling – NODATA response (NOERROR + SOA + empty answer) suppresses NS.
     */
    @Test
    void normalize_authorityNsSuppressedOnNoData() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("empty.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NOERROR);
        // SOA in authority (indicates NODATA)
        SOARecord soa = new SOARecord(qname, DClass.IN, 300, Name.fromConstantString("ns.example.com."), Name.fromConstantString("hostmaster.example.com."), 1, 7200, 3600, 1209600, 3600);
        response.addRecord(soa, Section.AUTHORITY);
        // NS that should be ignored because answer is empty.
        NSRecord ns = new NSRecord(qname, DClass.IN, 300, Name.fromConstantString("ns.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        // Only the SOA should survive.
        assertEquals(1, cleaned.getSection(Section.AUTHORITY).size(), "Only SOA should be kept in NODATA response");
        assertTrue(cleaned.getSection(Section.AUTHORITY).get(0) instanceof SOARecord);
    }

    /**
     * Authority allowed type retained when isTypeAllowedInSection returns true (e.g., MX).
     */
    @Test
    void normalize_authorityAllowedTypeRetained() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("mail.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.MX, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        // MX record in authority – assumed allowed by default implementation.
        MXRecord mx = new MXRecord(Name.fromConstantString("example.com."), DClass.IN, 300, 10, Name.fromConstantString("mail.example.com."));
        response.addRecord(mx, Section.AUTHORITY);
        Message cleaned = response.normalize(query, false);
        assertEquals(1, cleaned.getSection(Section.AUTHORITY).size(), "Allowed MX RRset should be retained");
        assertTrue(cleaned.getSection(Section.AUTHORITY).get(0) instanceof MXRecord);
    }

    /**
     * Additional section – only referenced records are kept.
     */
    @Test
    void normalize_additionalOnlyReferencedKept() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("host.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        // Answer A record for host.example.com.
        ARecord a = new ARecord(qname, DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        response.addRecord(a, Section.ANSWER);
        // Additional: one referenced (same name) and one unreferenced.
        ARecord referenced = new ARecord(qname, DClass.IN, 300, InetAddress.getByName("5.6.7.8"));
        ARecord unreferenced = new ARecord(Name.fromConstantString("unrelated.example.com."), DClass.IN, 300, InetAddress.getByName("9.10.11.12"));
        response.addRecord(referenced, Section.ADDITIONAL);
        response.addRecord(unreferenced, Section.ADDITIONAL);
        Message cleaned = response.normalize(query, false);
        // Additional section should contain only the referenced record.
        assertEquals(1, cleaned.getSection(Section.ADDITIONAL).size(), "Only referenced additional records should be kept");
        assertEquals(referenced, cleaned.getSection(Section.ADDITIONAL).get(0));
    }

    /**
     * ANY query – after CNAME processing, subsequent records with original name are copied.
     */
    @Test
    void normalize_anyQueryCopiesFollowingRecords() throws UnknownHostException, WireParseException {
        Name qname = Name.fromConstantString("any.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.ANY, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        // CNAME for any.example.com -> alias.example.com
        CNAMERecord cname = new CNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("alias.example.com."));
        response.addRecord(cname, Section.ANSWER);
        // Following A record that has the *original* name (oldSname) – should be copied.
        ARecord a = new ARecord(qname, DClass.IN, 300, InetAddress.getByName("1.1.1.1"));
        response.addRecord(a, Section.ANSWER);
        Message cleaned = response.normalize(query, false);
        // Both CNAME and the extra A should be present.
        assertEquals(2, cleaned.getSection(Section.ANSWER).size(), "CNAME and following record with original name should be kept for ANY query");
        assertTrue(cleaned.getSection(Section.ANSWER).contains(cname));
        assertTrue(cleaned.getSection(Section.ANSWER).contains(a));
    }

    /**
     * Authority forbidden types – strict mode throws for each forbidden type.
     */
    @Test
    void normalize_authorityMultipleForbiddenTypesThrowWhenStrict() throws UnknownHostException {
        Name qname = Name.fromConstantString("foo.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        // Add several forbidden types.
        DNAMERecord dname = new DNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("target.example.com."));
        CNAMERecord cname = new CNAMERecord(qname, DClass.IN, 300, Name.fromConstantString("cname.example.com."));
        ARecord a = new ARecord(qname, DClass.IN, 300, InetAddress.getByName("2.2.2.2"));
        AAAARecord aaaa = new AAAARecord(qname, DClass.IN, 300, InetAddress.getByName("::1"));
        response.addRecord(dname, Section.AUTHORITY);
        response.addRecord(cname, Section.AUTHORITY);
        response.addRecord(a, Section.AUTHORITY);
        response.addRecord(aaaa, Section.AUTHORITY);
        assertThrows(WireParseException.class, () -> response.normalize(query, true), "Any forbidden authority RRset should cause exception when strict");
    }

    /**
     * Authority NS handling – strict mode throws on subdomain mismatch.
     */
    @Test
    void normalize_authorityNsSubdomainMismatchThrowsWhenStrict() throws UnknownHostException {
        Name qname = Name.fromConstantString("sub.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        // NS not a subdomain.
        Name nsName = Name.fromConstantString("unrelated.example.com.");
        NSRecord ns = new NSRecord(nsName, DClass.IN, 300, Name.fromConstantString("ns.unrelated.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        assertThrows(WireParseException.class, () -> response.normalize(query, true), "NS subdomain mismatch should throw when strict");
    }

    /**
     * Authority NS handling – strict mode throws on duplicate NS.
     */
    @Test
    void normalize_authorityDuplicateNsThrowsWhenStrict() throws UnknownHostException {
        Name qname = Name.fromConstantString("www.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        NSRecord ns1 = new NSRecord(qname, DClass.IN, 300, Name.fromConstantString("ns1.example.com."));
        NSRecord ns2 = new NSRecord(qname, DClass.IN, 300, Name.fromConstantString("ns2.example.com."));
        response.addRecord(ns1, Section.AUTHORITY);
        response.addRecord(ns2, Section.AUTHORITY);
        assertThrows(WireParseException.class, () -> response.normalize(query, true), "Duplicate NS should cause exception when strict");
    }

    /**
     * Authority NS handling – strict mode throws on NXDOMAIN suppression.
     */
    @Test
    void normalize_authorityNsNxDomainThrowsWhenStrict() throws UnknownHostException {
        Name qname = Name.fromConstantString("doesnotexist.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NXDOMAIN);
        NSRecord ns = new NSRecord(qname, DClass.IN, 300, Name.fromConstantString("ns.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        assertThrows(WireParseException.class, () -> response.normalize(query, true), "NS in NXDOMAIN response should cause exception when strict");
    }

    /**
     * Authority NS handling – strict mode throws on NODATA suppression.
     */
    @Test
    void normalize_authorityNsNoDataThrowsWhenStrict() throws UnknownHostException {
        Name qname = Name.fromConstantString("empty.example.com.");
        Record queryRecord = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NOERROR);
        // SOA indicates NODATA.
        SOARecord soa = new SOARecord(qname, DClass.IN, 300, Name.fromConstantString("ns.example.com."), Name.fromConstantString("hostmaster.example.com."), 1, 7200, 3600, 1209600, 3600);
        response.addRecord(soa, Section.AUTHORITY);
        // NS that should be suppressed.
        NSRecord ns = new NSRecord(qname, DClass.IN, 300, Name.fromConstantString("ns.example.com."));
        response.addRecord(ns, Section.AUTHORITY);
        assertThrows(WireParseException.class, () -> response.normalize(query, true), "NS in NODATA response should cause exception when strict");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/regen-policies/results/policy/pools/ktester/34/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
[main] WARN org.xbill.DNS.Message - Found 2 CNAMEs in <alias.example.com./IN> response to <alias.example.com./A/IN> (id 58786), removing all but the first
[main] WARN org.xbill.DNS.Message - Normalization failed in response to <x.example.com./A/IN> (id 9802), found 2 entries (instead of just one) in DNAME RRSet <x.example.com./IN>

╷
├─ JUnit Jupiter ✔
│  └─ Message_normalize_2_Test ✔
│     ├─ normalize_existingMatchingCNAMESkipsSynthesis() ✔
│     ├─ normalize_earlyReturnOnUnsupportedRcode() ✔
│     ├─ normalize_authorityNsSubdomainMismatchThrowsWhenStrict() ✔
│     ├─ normalize_throwsOnMultipleDNAMERecords() ✔
│     ├─ normalize_authorityDuplicateNsIgnored() ✘ Only the first NS should be kept ==> expected: <1> but was: <2>
│     ├─ normalize_cnameMultipleRecordsTrimmedWhenNotThrowing() ✘ The kept CNAME must be the first one ==> expected: <mid.example.com.> but was: <final.example.com.>
│     ├─ normalize_multipleDNAMEReturnsNullWhenNotThrowing() ✔
│     ├─ normalize_authorityNsSubdomainMismatchIgnored() ✔
│     ├─ normalize_authorityDuplicateNsThrowsWhenStrict() ✘ Duplicate NS should cause exception when strict ==> Expected org.xbill.DNS.WireParseException to be thrown, but nothing was thrown.
│     ├─ normalize_authorityNsNxDomainThrowsWhenStrict() ✔
│     ├─ normalize_authorityMultipleForbiddenTypesThrowWhenStrict() ✔
│     ├─ normalize_authorityNsSuppressedOnNxDomain() ✔
│     ├─ normalize_ignoresIrrelevantAnswerRecordWhenNotThrowing() ✔
│     ├─ normalize_anyQueryCopiesFollowingRecords() ✔
│     ├─ normalize_authorityNsSuppressedOnNoData() ✔
│     ├─ normalize_authorityNsNoDataThrowsWhenStrict() ✔
│     ├─ normalize_synthesisesCNAMEFromDNAME() ✔
│     ├─ normalize_irrelevantAnswerThrowsWhenStrict() ✔
│     ├─ normalize_authorityForbiddenTypesThrowWhenStrict() ✔
│     ├─ normalize_additionalOnlyReferencedKept() ✘ java.lang.NullPointerException
│     ├─ normalize_authorityAllowedTypeRetained() ✘ Allowed MX RRset should be retained ==> expected: <1> but was: <0>
│     ├─ normalize_authorityDisallowedTypeIgnored() ✔
│     └─ normalize_dnameQueryNoSynthesis() ✘ Cannot invoke "org.xbill.DNS.Record.getType()" because the return value of "org.xbill.DNS.Message.getQuestion()" is null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (6):
  JUnit Jupiter:Message_normalize_2_Test:normalize_authorityDuplicateNsIgnored()
    MethodSource [className = 'org.xbill.DNS.Message_normalize_2_Test', methodName = 'normalize_authorityDuplicateNsIgnored', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Only the first NS should be kept ==> expected: <1> but was: <2>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:563)
       org.xbill.DNS.Message_normalize_2_Test.normalize_authorityDuplicateNsIgnored(Message_normalize_2_Test.java:272)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_normalize_2_Test:normalize_cnameMultipleRecordsTrimmedWhenNotThrowing()
    MethodSource [className = 'org.xbill.DNS.Message_normalize_2_Test', methodName = 'normalize_cnameMultipleRecordsTrimmedWhenNotThrowing', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: The kept CNAME must be the first one ==> expected: <mid.example.com.> but was: <final.example.com.>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1156)
       org.xbill.DNS.Message_normalize_2_Test.normalize_cnameMultipleRecordsTrimmedWhenNotThrowing(Message_normalize_2_Test.java:198)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_normalize_2_Test:normalize_authorityDuplicateNsThrowsWhenStrict()
    MethodSource [className = 'org.xbill.DNS.Message_normalize_2_Test', methodName = 'normalize_authorityDuplicateNsThrowsWhenStrict', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Duplicate NS should cause exception when strict ==> Expected org.xbill.DNS.WireParseException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_normalize_2_Test.normalize_authorityDuplicateNsThrowsWhenStrict(Message_normalize_2_Test.java:428)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_normalize_2_Test:normalize_additionalOnlyReferencedKept()
    MethodSource [className = 'org.xbill.DNS.Message_normalize_2_Test', methodName = 'normalize_additionalOnlyReferencedKept', methodParameterTypes = '']
    => java.lang.NullPointerException
       org.xbill.DNS.Message.normalize(Unknown Source)
       org.xbill.DNS.Message_normalize_2_Test.normalize_additionalOnlyReferencedKept(Message_normalize_2_Test.java:350)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       [...]
  JUnit Jupiter:Message_normalize_2_Test:normalize_authorityAllowedTypeRetained()
    MethodSource [className = 'org.xbill.DNS.Message_normalize_2_Test', methodName = 'normalize_authorityAllowedTypeRetained', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Allowed MX RRset should be retained ==> expected: <1> but was: <0>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:563)
       org.xbill.DNS.Message_normalize_2_Test.normalize_authorityAllowedTypeRetained(Message_normalize_2_Test.java:329)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_normalize_2_Test:normalize_dnameQueryNoSynthesis()
    MethodSource [className = 'org.xbill.DNS.Message_normalize_2_Test', methodName = 'normalize_dnameQueryNoSynthesis', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "org.xbill.DNS.Record.getType()" because the return value of "org.xbill.DNS.Message.getQuestion()" is null
       org.xbill.DNS.Message.normalize(Message.java:934)
       org.xbill.DNS.Message_normalize_2_Test.normalize_dnameQueryNoSynthesis(Message_normalize_2_Test.java:115)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       [...]

Test run finished after 472 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        23 tests found           ]
[         0 tests skipped         ]
[        23 tests started         ]
[         0 tests aborted         ]
[        17 tests successful      ]
[         6 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class org.xbill.DNS.Message: 

 * A DNS Message. A message is the basic unit of communication between the client and server of a
 * DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 

api document of method normalize(Message, boolean): 

   * Creates a normalized copy of this message by following xNAME chains, synthesizing CNAMEs from
   * DNAMEs if necessary, and removing illegal RRsets from {@link Section#AUTHORITY} and {@link
   * Section#ADDITIONAL}.
   *
   * <p>Normalization is only applied to {@link Rcode#NOERROR} and {@link Rcode#NXDOMAIN} responses.
   *
   * @param query The query that produced this message.
   * @param throwOnIrrelevantRecord If {@code true}, throw an exception instead of silently ignoring
   *     irrelevant records.
   * @return {@code null} if the message could not be normalized or is otherwise invalid.
   * @throws WireParseException when {@code throwOnIrrelevantRecord} is {@code true} and an invalid
   *     or irrelevant record was found.
   * @since 3.6
   

return type: 
org.xbill.DNS.Message

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
	related functions:
		method `private void logOrThrow(boolean, String, RRset, Name, Message)` returns `void`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ADDITIONAL;
		int org.xbill.DNS.Section.QUESTION;
		int org.xbill.DNS.Section.ANSWER;
		int org.xbill.DNS.Section.AUTHORITY;
	
class `org.xbill.DNS.Type`:
	dependent fields:
		int org.xbill.DNS.Type.ANY;
		int org.xbill.DNS.Type.A;
		int org.xbill.DNS.Type.DNAME;
		int org.xbill.DNS.Type.CNAME;
		int org.xbill.DNS.Type.AAAA;
		int org.xbill.DNS.Type.SOA;
		int org.xbill.DNS.Type.NS;
	
class `org.xbill.DNS.Rcode`:
	dependent fields:
		int org.xbill.DNS.Rcode.NOERROR;
		int org.xbill.DNS.Rcode.NXDOMAIN;
	
class `org.xbill.DNS.Cache`:
	related functions:
		method `public SetResponse addMessage(Message)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`, api document: 
   * Adds all data from a Message into the Cache. Each record is added with the appropriate
   * credibility, and negative answers are cached as such.
   *
   * @param in The Message to be added
   * @return A SetResponse that reflects what would be returned from a cache lookup, or null if
   *     nothing useful could be cached from the message.
   * @see Message
   ;
class `org.xbill.DNS.dnssec.ValUtils`:
	related functions:
		method `public static ResponseClassification classifyResponse(Message, SMessage)` returns `org.xbill.DNS.dnssec.ResponseClassification`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`, api document: 
   * Given a response, classify ANSWER responses into a subtype.
   *
   * @param request The original query message.
   * @param m The response to classify.
   * @return A subtype ranging from UNKNOWN to NAMEERROR.
   ;
class `org.xbill.DNS.dnssec.ValidatingResolver`:
	related functions:
		method `private CompletionStage<Boolean> validateAnswerAndGetWildcardsRecursive(SMessage, int, Map<Name, Name>, AtomicInteger, Executor)` returns `java.util.concurrent.CompletionStage<java.lang.Boolean>`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;
		method `private void removeSpuriousAuthority(SMessage)` returns `void`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`, api document: 
   * For messages that are not referrals, if the chase reply contains an unsigned NS record in the
   * authority section it could have been inserted by a (BIND) forwarder that thinks the zone is
   * insecure, and that has an NS record without signatures in cache. Remove the NS record since the
   * reply does not hinge on that record (in the authority section), but do not remove it if it
   * removes the last record from the answer+authority sections.
   *
   * @param response: the chased reply, we have a key for this contents, so we should have
   *     signatures for these rrsets and not having signatures means it will be bogus.
   ;
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method ` byte addAnswer(Message, Name, int, int, int, int)` returns `byte`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;
		method ` byte[] generateReply(Message, byte[], Socket)` returns `byte[]`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;
class `org.xbill.DNS.lookup.LookupSession`:
	related functions:
		method `private CompletionStage<LookupResult> maybeFollowRedirectsInAnswer(LookupResult, Record, int)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.lookup.LookupResult>`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;
		method `private Message maybeAddToCache(Message)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
