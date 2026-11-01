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
Here is the code of focal method, `newQuery(Record)`, with the focal class `Message`:
```Java
package org.xbill.DNS;
import java.util.LinkedList;
import java.util.Set;
import java.util.Optional;
import lombok.SneakyThrows;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Collections;
public class Message implements Cloneable {
    public static final int MAXLENGTH = 65535;
    private Header header;
    int tsigstart;
    int tsigState;
    int sig0start;
    static final int TSIG_UNSIGNED = 0;
    static final int TSIG_VERIFIED = 1;
    static final int TSIG_INTERMEDIATE = 2;
    static final int TSIG_SIGNED = 3;
    static final int TSIG_FAILED = 4;
    public static Message newQuery(Record r) {
        Message m = new Message();
        m.header.setOpcode(Opcode.QUERY);
        m.header.setFlag(Flags.RD);
        m.addRecord(r, Section.QUESTION);
        return m;
    }
public void addRecord(Record r, int section);
    @Override
@SneakyThrows(CloneNotSupportedException.class)
@SuppressWarnings("unchecked")
public Message clone();
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
    public List<RRset> getSectionRRsets(int section);
    public TSIGRecord getTSIG();
    public boolean isSigned();
    public boolean isVerified();
    public static Message newUpdate(Name zone);
    public int numBytes();
    public void removeAllRecords(int section);
    public boolean removeRecord(Record r, int section);
    public String sectionToString(int i);
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
 

api document of method newQuery(Record): 

   * Creates a new Message with a random Message ID suitable for sending as a query.
   *
   * @param r A record containing the question
   

return type: 
org.xbill.DNS.Message

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.QUESTION;
	
class `org.xbill.DNS.Message`:
	dependent fields:
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	
class `org.xbill.DNS.Opcode`:
	dependent fields:
		int org.xbill.DNS.Opcode.QUERY;
	
class `org.xbill.DNS.Flags`:
	dependent fields:
		byte org.xbill.DNS.Flags.RD;
	
class `org.xbill.DNS.tools.update`:
	related functions:
		method `public Message newMessage()` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method ` byte[] buildErrorMessage(Header, int, Record)` returns `byte[]`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.ZoneTransferIn`:
	related functions:
		method `private void sendQuery()` returns `void`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.dnssec.SMessage`:
	related functions:
		method `public Message getMessage()` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newQuery(Record)`, api document: 
   * Gets this message as a standard DNSJAVA message.
   *
   * @return This message as a standard DNSJAVA message.
   ;
class `org.xbill.DNS.Update`:
	related functions:
		method `private void newPrereq(Record)` returns `void`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.SimpleResolver`:
	related functions:
		method `private void applyEDNS(Message)` returns `void`, related with `org.xbill.DNS.Message.newQuery(Record)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.