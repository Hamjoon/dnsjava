You are a professional Java software testing expert. Your task is to generate high-quality unit test code for a focal method, using **JUnit5** and **Mockito** libraries. 

You'll be provided with:
1. The focal method to test.
2. The target class's structure (fields and methods).
3. A pre-defined test class framework with `@Before`/`@After` methods.
4. Test cases in JSON format.
5. Any relevant context.

**Requirements:**

Code Quality:
- Include clear comments explaining each test scenario.
- Ensure code is clean, understandable, and maintainable.
- Do not directly access private methods or fields. **Use reflection if the focal method is private.**
- Include all necessary imports; code must be free of syntax errors.

**Test Structure:**
- Follow the given test class framework; do not modify `@Before`/`@After` methods.
- Create one test function per test case group, ensuring all cases are covered.
- Use parameterized tests where appropriate.
- Reinitialize variables inside test functions if `@Setup` values don't fit.
- Properly mock and set up dependencies.

@input{focal method and target class}
Here is the code of focal method, `normalize(Message, boolean)`, with the focal class `Message`:
```Java
package org.xbill.DNS;
import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import lombok.SneakyThrows;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.Collections;
@Slf4j
public class Message implements Cloneable {
    public static final int MAXLENGTH = 65535;
    private List<Record>[] sections;
    int tsigstart;
    int tsigState;
    int sig0start;
    static final int TSIG_UNSIGNED = 0;
    static final int TSIG_VERIFIED = 1;
    static final int TSIG_INTERMEDIATE = 2;
    static final int TSIG_SIGNED = 3;
    static final int TSIG_FAILED = 4;
    public Message normalize(Message query, boolean throwOnIrrelevantRecord) throws WireParseException {
        if ((getRcode() != Rcode.NOERROR) && (getRcode() != Rcode.NXDOMAIN)) {
            return this;
        }
        Name sname = query.getQuestion().getName();
        List<RRset> answerSectionSets = getSectionRRsets(Section.ANSWER);
        List<RRset> additionalSectionSets = getSectionRRsets(Section.ADDITIONAL);
        List<RRset> authoritySectionSets = getSectionRRsets(Section.AUTHORITY);
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<RRset>[] cleanedSection = new ArrayList[4];
        cleanedSection[Section.ANSWER] = new ArrayList<>();
        cleanedSection[Section.AUTHORITY] = new ArrayList<>();
        cleanedSection[Section.ADDITIONAL] = new ArrayList<>();
        boolean hadNsInAuthority = false;
        // For the ANSWER section, remove all "irrelevant" records and add synthesized CNAMEs from
        // DNAMEs. This will strip out-of-order CNAMEs as well.
        for (int i = 0; i < answerSectionSets.size(); i++) {
            RRset rrset = answerSectionSets.get(i);
            Name oldSname = sname;
            if ((rrset.getType() == Type.DNAME) && sname.subdomain(rrset.getName())) {
                if (rrset.size() > 1) {
                    String template = "Normalization failed in response to <{}/{}/{}> (id {}), found {} entries (instead of just one) in DNAME RRSet <{}/{}>";
                    if (throwOnIrrelevantRecord) {
                        throw new WireParseException(template.replace("{}", "%s"));
                    }
                    log.warn(template, sname, Type.string(query.getQuestion().getType()), DClass.string(query.getQuestion().getDClass()), getHeader().getID(), rrset.size(), rrset.getName(), DClass.string(rrset.getDClass()));
                    return null;
                }
                // If DNAME was queried, don't attempt to synthesize CNAME
                if (query.getQuestion().getType() != Type.DNAME) {
                    // The DNAME is valid, accept it
                    cleanedSection[Section.ANSWER].add(rrset);
                    // Check if the next rrset is correct CNAME, otherwise synthesize a CNAME
                    RRset nextRRSet = (answerSectionSets.size() >= (i + 2)) ? answerSectionSets.get(i + 1) : null;
                    DNAMERecord dname = ((DNAMERecord) (rrset.first()));
                    try {
                        // Validate that an existing CNAME matches what we would synthesize
                        if (((nextRRSet != null) && (nextRRSet.getType() == Type.CNAME)) && nextRRSet.getName().equals(sname)) {
                            Name expected = Name.concatenate(nextRRSet.getName().relativize(dname.getName()), dname.getTarget());
                            if (expected.equals(((CNAMERecord) (nextRRSet.first())).getTarget())) {
                                continue;
                            }
                        }
                        // Add a synthesized CNAME; TTL=0 to avoid caching
                        Name dnameTarget = sname.fromDNAME(dname);
                        cleanedSection[Section.ANSWER].add(new RRset(new CNAMERecord(sname, dname.getDClass(), 0, dnameTarget)));
                        sname = dnameTarget;
                        // In DNAME ANY response, can have data after DNAME
                        if (query.getQuestion().getType() == Type.ANY) {
                            for (i++; i < answerSectionSets.size(); i++) {
                                rrset = answerSectionSets.get(i);
                                if (rrset.getName().equals(oldSname)) {
                                    cleanedSection[Section.ANSWER].add(rrset);
                                } else {
                                    break;
                                }
                            }
                        }
                        continue;
                    } catch (NameTooLongException e) {
                        String template = "Normalization failed in response to <{}/{}/{}> (id {}), could not synthesize CNAME for DNAME <{}/{}>";
                        if (throwOnIrrelevantRecord) {
                            throw new WireParseException(template.replace("{}", "%s"), e);
                        }
                        log.warn(template, sname, Type.string(query.getQuestion().getType()), DClass.string(query.getQuestion().getDClass()), getHeader().getID(), rrset.getName(), DClass.string(rrset.getDClass()));
                        return null;
                    }
                }
            }
            // Ignore irrelevant records
            if (!sname.equals(rrset.getName())) {
                logOrThrow(throwOnIrrelevantRecord, "Ignoring irrelevant RRset <{}/{}/{}> in response to <{}/{}/{}> (id {})", rrset, sname, query);
                continue;
            }
            // Follow CNAMEs
            if ((rrset.getType() == Type.CNAME) && (query.getQuestion().getType() != Type.CNAME)) {
                if (rrset.size() > 1) {
                    String template = "Found {} CNAMEs in <{}/{}> response to <{}/{}/{}> (id {}), removing all but the first";
                    if (throwOnIrrelevantRecord) {
                        throw new WireParseException(String.format(template.replace("{}", "%s"), rrset.rrs(false).size(), rrset.getName(), DClass.string(rrset.getDClass()), sname, Type.string(query.getQuestion().getType()), DClass.string(query.getQuestion().getDClass()), getHeader().getID()));
                    }
                    log.warn(template, rrset.rrs(false).size(), rrset.getName(), DClass.string(rrset.getDClass()), sname, Type.string(query.getQuestion().getType()), DClass.string(query.getQuestion().getDClass()), getHeader().getID());
                    List<Record> cnameRRset = rrset.rrs(false);
                    for (int cnameIndex = 1; cnameIndex < cnameRRset.size(); cnameIndex++) {
                        rrset.deleteRR(cnameRRset.get(i));
                    }
                }
                sname = ((CNAMERecord) (rrset.first())).getTarget();
                cleanedSection[Section.ANSWER].add(rrset);
                // In CNAME ANY response, can have data after CNAME
                if (query.getQuestion().getType() == Type.ANY) {
                    for (i++; i < answerSectionSets.size(); i++) {
                        rrset = answerSectionSets.get(i);
                        if (rrset.getName().equals(oldSname)) {
                            cleanedSection[Section.ANSWER].add(rrset);
                        } else {
                            break;
                        }
                    }
                }
                continue;
            }
            // Remove records that don't match the queried type
            int qtype = getQuestion().getType();
            if ((qtype != Type.ANY) && (rrset.getActualType() != qtype)) {
                logOrThrow(throwOnIrrelevantRecord, "Ignoring irrelevant RRset <{}/{}/{}> in ANSWER section response to <{}/{}/{}> (id {})", rrset, sname, query);
                continue;
            }
            // Mark the additional names from relevant RRset as OK
            cleanedSection[Section.ANSWER].add(rrset);
            if (sname.equals(rrset.getName())) {
                addAdditionalRRset(rrset, additionalSectionSets, cleanedSection[Section.ADDITIONAL]);
            }
        }
        for (RRset rrset : authoritySectionSets) {
            switch (rrset.getType()) {
                case Type.DNAME :
                case Type.CNAME :
                case Type.A :
                case Type.AAAA :
                    logOrThrow(throwOnIrrelevantRecord, "Ignoring forbidden RRset <{}/{}/{}> in AUTHORITY section response to <{}/{}/{}> (id {})", rrset, sname, query);
                    continue;
            }
            if (!isTypeAllowedInSection(rrset.getType(), Section.AUTHORITY)) {
                logOrThrow(throwOnIrrelevantRecord, "Ignoring disallowed RRset <{}/{}/{}> in AUTHORITY section response to <{}/{}/{}> (id {})", rrset, sname, query);
                continue;
            }
            if (rrset.getType() == Type.NS) {
                // NS set must be pertinent to the query
                if (!sname.subdomain(rrset.getName())) {
                    logOrThrow(throwOnIrrelevantRecord, "Ignoring disallowed RRset <{}/{}/{}> in AUTHORITY section response to <{}/{}/{}> (id {}), not a subdomain of the query", rrset, sname, query);
                    continue;
                }
                // We don't want NS sets for NODATA or NXDOMAIN answers, because they could contain
                // poisonous contents, from e.g. fragmentation attacks, inserted after long RRSIGs in the
                // packet get to the packet border and such
                if ((getRcode() == Rcode.NXDOMAIN) || (((getRcode() == Rcode.NOERROR) && authoritySectionSets.stream().anyMatch(set -> set.getType() == Type.SOA)) && (sections[Section.ANSWER] == null))) {
                    logOrThrow(throwOnIrrelevantRecord, "Ignoring disallowed RRset <{}/{}/{}> in AUTHORITY section response to <{}/{}/{}> (id {}), NXDOMAIN or NODATA", rrset, sname, query);
                    continue;
                }
                if (!hadNsInAuthority) {
                    hadNsInAuthority = true;
                } else {
                    logOrThrow(throwOnIrrelevantRecord, "Ignoring disallowed RRset <{}/{}/{}> in AUTHORITY section response to <{}/{}/{}> (id {}), already seen another NS", rrset, sname, query);
                    continue;
                }
            }
            cleanedSection[Section.AUTHORITY].add(rrset);
            addAdditionalRRset(rrset, additionalSectionSets, cleanedSection[Section.ADDITIONAL]);
        }
        Message cleanedMessage = new Message(this.getHeader());
        cleanedMessage.sections[Section.QUESTION] = this.sections[Section.QUESTION];
        for (int section : new int[]{ Section.ANSWER, Section.AUTHORITY, Section.ADDITIONAL }) {
            cleanedMessage.sections[section] = rrsetListToRecords(cleanedSection[section]);
            // Fixup counts in the header
            cleanedMessage.getHeader().setCount(section, cleanedMessage.sections[section] == null ? 0 : cleanedMessage.sections[section].size());
        }
        return cleanedMessage;
    }
private void addAdditionalRRset(RRset rrset, List<RRset> additionalSectionSets, List<RRset> cleanedAdditionalSection);
    public void addRecord(Record r, int section);
    @Override
@SneakyThrows(CloneNotSupportedException.class)
@SuppressWarnings(;
    public boolean findRRset(Name name, int type);
    public boolean findRRset(Name name, int type, int section);
    public boolean findRecord(Record r);
    public boolean findRecord(Record r, int section);
    TSIGRecord getGeneratedTSIG();
    public Header getHeader();
    public OPTRecord getOPT();
    public Record getQuestion();
    public int getRcode();
    public Optional<Resolver> getResolver();
    public List<Record> getSection(int section);
    @SuppressWarnings("java:S1119")
public List<RRset> getSectionRRsets(int section);
    public TSIGRecord getTSIG();
    public boolean isSigned();
    boolean isTypeAllowedInSection(int type, int section);
    public boolean isVerified();
    private void logOrThrow(boolean throwOnIrrelevantRecord, String format, RRset rrset, Name sname, Message query) throws WireParseException;
    public static Message newQuery(Record r);
    public static Message newUpdate(Name zone);
    public Message normalize(Message query);
    public int numBytes();
    public void removeAllRecords(int section);
    public boolean removeRecord(Record r, int section);
    private List<Record> rrsetListToRecords(List<RRset> rrsets);
    public String sectionToString(int section);
    public void setHeader(Header h);
    public void setResolver(Resolver resolver);
    public void setTSIG(TSIG key);
    public void setTSIG(TSIG key, int error, TSIGRecord querytsig);
    @Override
public String toString();
    public byte[] toWire();
    public byte[] toWire(int maxLength);
    public byte[] toWire(int maxLength, boolean truncate) throws MessageSizeExceededException;
    void toWire(DNSOutput out);
}
```

@input{initial test class framework}
```Java
<initial_class>
```

@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```


@input{context information}

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
		int org.xbill.DNS.Section.AUTHORITY;
		int org.xbill.DNS.Section.ANSWER;
		int org.xbill.DNS.Section.QUESTION;
	
class `org.xbill.DNS.Type`:
	dependent fields:
		int org.xbill.DNS.Type.A;
		int org.xbill.DNS.Type.NS;
		int org.xbill.DNS.Type.AAAA;
		int org.xbill.DNS.Type.ANY;
		int org.xbill.DNS.Type.DNAME;
		int org.xbill.DNS.Type.CNAME;
		int org.xbill.DNS.Type.SOA;
	
class `org.xbill.DNS.Rcode`:
	dependent fields:
		int org.xbill.DNS.Rcode.NXDOMAIN;
		int org.xbill.DNS.Rcode.NOERROR;
	
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
		method `private CompletionStage<Boolean> validateAnswerAndGetWildcardsRecursive(SMessage, int, Map<Name, Name>, AtomicInteger, Executor)` returns `java.util.concurrent.CompletionStage<java.lang.Boolean>`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method ` byte addAnswer(Message, Name, int, int, int, int)` returns `byte`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;
		method ` byte[] generateReply(Message, byte[], Socket)` returns `byte[]`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;
class `org.xbill.DNS.lookup.LookupSession`:
	related functions:
		method `private CompletionStage<LookupResult> maybeFollowRedirectsInAnswer(LookupResult, Record, int)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.lookup.LookupResult>`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;
		method `private Message maybeAddToCache(Message)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.normalize(Message, boolean)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.