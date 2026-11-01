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
Here is the code of focal method, `getSection(int)`, with the focal class `Message`:
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
    public List<Record> getSection(int section) {
        Section.check(section);
        if (sections[section] == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(sections[section]);
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
    @SuppressWarnings("java:S1119")
public List<RRset> getSectionRRsets(int section);
    public TSIGRecord getTSIG();
    public boolean isSigned();
    boolean isTypeAllowedInSection(int type, int section);
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
 

api document of method getSection(int): 

   * Returns all records in the given section, or an empty list if the section is empty.
   *
   * @see Record
   * @see Section
   

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
	related functions:
		method `public boolean findRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.getSection(int)`, api document: 
   * Determines if the given record is already present in the given section.
   *
   * @see Record
   * @see Section
   ;
		method `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.getSection(int)`, api document: 
   * Removes a record from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.getSection(int)`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.getSection(int)`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.getSection(int)`, api document: 
   * Returns an array containing all records in the given section grouped into RRsets.
   *
   * @see RRset
   * @see Section
   ;
		method `public void removeAllRecords(int)` returns `void`, related with `org.xbill.DNS.Message.getSection(int)`, api document: 
   * Removes all records from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
class `org.xbill.DNS.OPTRecord`:
	related functions:
		method `public List<EDNSOption> getOptions()` returns `java.util.List<org.xbill.DNS.EDNSOption>`, related with `org.xbill.DNS.Message.getSection(int)`, api document:  Gets all options in the OPTRecord. This returns a list of EDNSOptions. ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.