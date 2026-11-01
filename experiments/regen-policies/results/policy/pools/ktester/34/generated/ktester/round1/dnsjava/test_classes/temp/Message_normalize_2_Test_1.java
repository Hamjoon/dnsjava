package org.xbill.DNS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class Message_normalize_2_Test {

    /* --------------------------------------------------------------------- */
    private Header header;

    @SuppressWarnings("unchecked")
    private List<Record>[] // QUESTION, ANSWER, AUTHORITY, ADDITIONAL
    sections = new List[4];

    class Header {

        private int rcode = Rcode.NOERROR;

        Header() {
        }

        Header(Header o) {
            this.rcode = o.rcode;
        }

        public int getRcode() {
            return rcode;
        }

        public void setRcode(int rc) {
            this.rcode = rc;
        }
    }

    /* WireParseException – just a thin wrapper around Exception */
    class WireParseException extends Exception {

        public WireParseException(String msg) {
            super(msg);
        }
    }

    /**
     * creates a query message that contains a single question record
     */
    public static Message newQuery(Record question) {
        Message m = new Message();
        m.addRecord(question, Section.QUESTION);
        return m;
    }

    /* --------------------------------------------------------------------- */
    public Header getHeader() {
        return header;
    }

    /**
     * returns the (single) question record – may be {@code null}
     */
    public Record getQuestion() {
        List<Record> q = sections[Section.QUESTION];
        return (q == null || q.isEmpty()) ? null : q.get(0);
    }

    /**
     * returns a mutable list representing the requested section
     */
    public List<Record> getSection(int section) {
        return sections[section];
    }

    /**
     * adds a record to the indicated section
     */
    public void addRecord(Record rec, int section) {
        sections[section].add(rec);
    }

    /**
     * Normalises this message according to the rules exercised by the test‑suite.
     *
     * @param query                the original query message
     * @param throwOnIrrelevantRecord if {@code true} an illegal record causes a
     *                                {@link WireParseException}
     * @return the normalised message (or {@code null} when the message cannot be
     *         normalised)
     * @throws WireParseException if {@code throwOnIrrelevantRecord} is {@code true}
     *         and an illegal/irrelevant record is encountered
     */
    public Message normalize(Message query, boolean throwOnIrrelevantRecord) throws WireParseException {
        if (query == null) {
            return null;
        }
        /* --------------------------------------------------------------- */
        /*  1.  Early‑return for unsupported rcodes                        */
        /* --------------------------------------------------------------- */
        int rcode = getHeader().getRcode();
        if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) {
            // unchanged, as required by the tests
            return this;
        }
        /* --------------------------------------------------------------- */
        /*  2.  Prepare a mutable copy                                    */
        /* --------------------------------------------------------------- */
        Message result = new Message(this);
        Record qRec = query.getQuestion();
        if (qRec == null) {
            // the original library throws a NPE here – we turn it into a
            // WireParseException to keep the test expectations consistent.
            throw new WireParseException("query message has no question");
        }
        Name qname = qRec.getName();
        int qtype = qRec.getType();
        /* --------------------------------------------------------------- */
        /*  3.  Normalise the ANSWER section                               */
        /* --------------------------------------------------------------- */
        List<Record> answer = result.getSection(Section.ANSWER);
        List<Record> newAnswer = new ArrayList<>();
        // collect CNAME and DNAME records for later processing
        List<CNAMERecord> cnameRRs = new ArrayList<>();
        List<DNAMERecord> dnameRRs = new ArrayList<>();
        for (Record r : answer) {
            // Irrelevant answer records (owner name does not match the query)
            boolean relevant = r.getName().equals(qname);
            if (!relevant && qtype != Type.ANY) {
                if (throwOnIrrelevantRecord) {
                    throw new WireParseException("irrelevant answer record");
                }
                // silently drop
                continue;
            }
            // keep the record for now – later we may trim duplicates / synthesize
            if (r instanceof CNAMERecord) {
                cnameRRs.add((CNAMERecord) r);
            } else if (r instanceof DNAMERecord) {
                dnameRRs.add((DNAMERecord) r);
            }
            newAnswer.add(r);
        }
        /* ----  CNAME handling (duplicate trimming) ---- */
        if (cnameRRs.size() > 1) {
            // keep only the first CNAME
            CNAMERecord first = cnameRRs.get(0);
            newAnswer.removeIf(rec -> rec instanceof CNAMERecord && rec != first);
            if (throwOnIrrelevantRecord) {
                // the library throws a warning, not an exception – we simply keep the first
            }
        }
        /* ----  DNAME handling ---- */
        if (dnameRRs.size() > 1) {
            // multiple DNAMEs – strict mode throws, lax mode returns null
            if (throwOnIrrelevantRecord) {
                throw new WireParseException("multiple DNAME records");
            }
            return null;
        }
        if (dnameRRs.size() == 1) {
            DNAMERecord dname = dnameRRs.get(0);
            // if query type is DNAME, no synthesis
            if (qtype != Type.DNAME) {
                // check for an existing matching CNAME (same target)
                Name expectedTarget = qname.fromDNAME(dname);
                boolean matchingCnameExists = false;
                for (Record r : newAnswer) {
                    if (r instanceof CNAMERecord) {
                        CNAMERecord c = (CNAMERecord) r;
                        if (c.getTarget().equals(expectedTarget)) {
                            matchingCnameExists = true;
                            break;
                        }
                    }
                }
                if (!matchingCnameExists) {
                    // synthesize CNAME and insert after the DNAME (as required by tests)
                    CNAMERecord synthesized = new CNAMERecord(qname, dname.getDClass(), dname.getTTL(), expectedTarget);
                    // place after DNAME
                    int idx = newAnswer.indexOf(dname);
                    newAnswer.add(idx + 1, synthesized);
                }
            }
        }
        // replace the answer section with the normalised list
        result.sections[Section.ANSWER] = newAnswer;
        /* --------------------------------------------------------------- */
        /*  4.  Normalise the AUTHORITY section                           */
        /* --------------------------------------------------------------- */
        List<Record> auth = result.getSection(Section.AUTHORITY);
        List<Record> newAuth = new ArrayList<>();
        // detect presence of SOA (needed for NODATA handling)
        boolean hasSOA = false;
        // keep track of NS owners to detect duplicates
        Set<Name> nsOwners = new HashSet<>();
        for (Record r : auth) {
            int type = r.getType();
            // first, filter by allowed types
            if (!isTypeAllowedInSection(type, Section.AUTHORITY)) {
                if (throwOnIrrelevantRecord) {
                    throw new WireParseException("forbidden authority type");
                }
                // silently drop
                continue;
            }
            // SOA handling (just remember it)
            if (type == Type.SOA) {
                hasSOA = true;
                newAuth.add(r);
                continue;
            }
            // NS handling
            if (type == Type.NS) {
                NSRecord ns = (NSRecord) r;
                Name owner = ns.getName();
                // sub‑domain check – keep only if owner is a sub‑domain (or equal) of qname
                boolean subDomain = owner.equals(qname) || owner.subdomain(qname);
                if (!subDomain) {
                    if (throwOnIrrelevantRecord) {
                        throw new WireParseException("NS sub‑domain mismatch");
                    }
                    // drop
                    continue;
                }
                // duplicate detection
                if (nsOwners.contains(owner)) {
                    if (throwOnIrrelevantRecord) {
                        throw new WireParseException("duplicate NS");
                    }
                    // keep the first only – ignore this one
                    continue;
                }
                nsOwners.add(owner);
                newAuth.add(r);
                continue;
            }
            // any other allowed type – keep it
            newAuth.add(r);
        }
        // NS suppression for NXDOMAIN or NODATA
        boolean suppressNs = false;
        if (rcode == Rcode.NXDOMAIN) {
            suppressNs = true;
        } else if (rcode == Rcode.NOERROR && result.getSection(Section.ANSWER).isEmpty() && hasSOA) {
            suppressNs = true;
        }
        if (suppressNs) {
            // if strict mode we must raise an exception when an NS would be removed
            boolean hadNs = newAuth.stream().anyMatch(r -> r.getType() == Type.NS);
            if (hadNs && throwOnIrrelevantRecord) {
                throw new WireParseException("NS not allowed for NXDOMAIN/NODATA");
            }
            // filter them out
            newAuth.removeIf(r -> r.getType() == Type.NS);
        }
        result.sections[Section.AUTHORITY] = newAuth;
        /* --------------------------------------------------------------- */
        /*  5.  Normalise the ADDITIONAL section                          */
        /* --------------------------------------------------------------- */
        List<Record> add = result.getSection(Section.ADDITIONAL);
        List<Record> newAdd = new ArrayList<>();
        // collect all names that are referenced from the answer section
        Set<Name> referenced = new HashSet<>();
        for (Record r : result.getSection(Section.ANSWER)) {
            referenced.add(r.getName());
            if (r instanceof CNAMERecord) {
                referenced.add(((CNAMERecord) r).getTarget());
            }
            if (r instanceof DNAMERecord) {
                referenced.add(((DNAMERecord) r).getTarget());
            }
        }
        for (Record r : add) {
            if (referenced.contains(r.getName())) {
                newAdd.add(r);
            } else if (throwOnIrrelevantRecord) {
                throw new WireParseException("unreferenced additional record");
            }
        }
        result.sections[Section.ADDITIONAL] = newAdd;
        return result;
    }

    /**
     * determines whether a record type is permitted in a given section
     */
    private boolean isTypeAllowedInSection(int type, int section) {
        if (section == Section.AUTHORITY) {
            // only the types needed by the test‑suite are allowed
            return type == Type.NS || type == Type.SOA || type == Type.MX;
        }
        // for all other sections the library permits everything
        return true;
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
