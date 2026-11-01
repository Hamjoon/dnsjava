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
Here is the code of focal method, `isTypeAllowedInSection(int, int)`, with the focal class `Message`:
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
    int tsigstart;
    int tsigState;
    int sig0start;
    static final int TSIG_UNSIGNED = 0;
    static final int TSIG_VERIFIED = 1;
    static final int TSIG_INTERMEDIATE = 2;
    static final int TSIG_SIGNED = 3;
    static final int TSIG_FAILED = 4;
    boolean isTypeAllowedInSection(int type, int section) {
        Type.check(type);
        Section.check(section);
        switch (section) {
            case Section.AUTHORITY :
                if (((((type == Type.SOA) || (type == Type.NS)) || (type == Type.DS)) || (type == Type.NSEC)) || (type == Type.NSEC3)) {
                    return true;
                }
                break;
            case Section.ADDITIONAL :
                if ((type == Type.A) || (type == Type.AAAA)) {
                    return true;
                }
                break;
        }
        return !Boolean.parseBoolean(System.getProperty("dnsjava.harden_unknown_additional", "true"));
    }
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
    public boolean isVerified();
    public static Message newQuery(Record r);
    public static Message newUpdate(Name zone);
    public Message normalize(Message query);
    public Message normalize(Message query, boolean throwOnIrrelevantRecord) throws WireParseException;
    public int numBytes();
    public void removeAllRecords(int section);
    public boolean removeRecord(Record r, int section);
    public String sectionToString(int section);
    public void setHeader(Header h);
    public void setResolver(Resolver resolver);
    public void setTSIG(TSIG key);
    public void setTSIG(TSIG key, int error, TSIGRecord querytsig);
    @Override
public String toString();
    public byte[] toWire();
    public byte[] toWire(int maxLength);
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
 

api document of method isTypeAllowedInSection(int, int): 

   * Checks if a record {@link Type} is allowed within a {@link Section}.
   *
   * @return {@code true} if the type is allowed, {@code false} otherwise.
   

return type: 
boolean

dependent classes: 
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ADDITIONAL;
		int org.xbill.DNS.Section.AUTHORITY;
	
class `org.xbill.DNS.Type`:
	dependent fields:
		int org.xbill.DNS.Type.DS;
		int org.xbill.DNS.Type.A;
		int org.xbill.DNS.Type.NSEC;
		int org.xbill.DNS.Type.AAAA;
		int org.xbill.DNS.Type.NSEC3;
		int org.xbill.DNS.Type.SOA;
		int org.xbill.DNS.Type.NS;
	
class `org.xbill.DNS.Generator`:
	related functions:
		method `public static boolean supportedType(int)` returns `boolean`, related with `org.xbill.DNS.Message.isTypeAllowedInSection(int, int)`, api document: 
   * Indicates whether generation is supported for this type.
   *
   * @throws InvalidTypeException The type is out of range.
   ;
class `org.xbill.DNS.Type.TypeMnemonic`:
	related functions:
		method `public void check(int)` returns `void`, related with `org.xbill.DNS.Message.isTypeAllowedInSection(int, int)`;
class `org.xbill.DNS.Message`:
	related functions:
		method `public void removeAllRecords(int)` returns `void`, related with `org.xbill.DNS.Message.isTypeAllowedInSection(int, int)`, api document: 
   * Removes all records from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.isTypeAllowedInSection(int, int)`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
		method `public Message normalize(Message, boolean)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.isTypeAllowedInSection(int, int)`, api document: 
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
   ;
		method `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.isTypeAllowedInSection(int, int)`, api document: 
   * Removes a record from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.