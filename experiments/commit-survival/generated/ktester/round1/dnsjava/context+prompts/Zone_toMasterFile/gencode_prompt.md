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
Here is the code of focal method, `toMasterFile()`, with the focal class `Zone`:
```Java
package org.xbill.DNS;
import java.util.LinkedList;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Map;
import java.io.Serializable;
import java.util.TreeMap;
public class Zone implements Serializable {
    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    private Map<Name, Object> data;
    private Name origin;
    private Object originNode;
    public synchronized String toMasterFile() {
        StringBuffer sb = new StringBuffer();
        nodeToString(sb, originNode);
        for (Map.Entry<Name, Object> entry : data.entrySet()) {
            if (!origin.equals(entry.getKey())) {
                nodeToString(sb, entry.getValue());
            }
        }
        return sb.toString();
    }
public Iterator<RRset> AXFR();
    public void addRRset(RRset rrset);
    public <T extends Record> void addRecord(T r);
    public RRset findExactMatch(Name name, int type);
    public SetResponse findRecords(Name name, int type);
    public int getDClass();
    public RRset getNS();
    public Name getOrigin();
    public SOARecord getSOA();
    public Iterator<RRset> iterator();
    private void nodeToString(StringBuffer sb, Object node);
    public void removeRecord(Record r);
    @Override
public String toString();
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

api document of class org.xbill.DNS.Zone: 

 * A DNS Zone. This encapsulates all data related to a Zone, and provides convenient lookup methods.
 *
 * @author Brian Wellington
 

api document of method toMasterFile(): 
 Returns the contents of the Zone in master file format. 

dependent classes: 
class `org.xbill.DNS.Zone`:
	dependent fields:
		java.util.Map<org.xbill.DNS.Name, java.lang.Object> org.xbill.DNS.Zone.data;
		org.xbill.DNS.Name org.xbill.DNS.Zone.origin;
		java.lang.Object org.xbill.DNS.Zone.originNode;
	related functions:
		method `private void fromXFR(ZoneTransferIn)` returns `void`, related with `org.xbill.DNS.Zone.toMasterFile()`;
		method `public Name getOrigin()` returns `org.xbill.DNS.Name`, related with `org.xbill.DNS.Zone.toMasterFile()`, api document:  Returns the Zone's origin ;
		method `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.toMasterFile()`;
class `org.xbill.DNS.Zone.ZoneIterator`:
	related functions:
		method `public RRset next()` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Zone.toMasterFile()`;
class `org.xbill.DNS.SVCBBase`:
	related functions:
		method `protected String rrToString()` returns `java.lang.String`, related with `org.xbill.DNS.Zone.toMasterFile()`;
		method `protected void rrToWire(DNSOutput, Compression, boolean)` returns `void`, related with `org.xbill.DNS.Zone.toMasterFile()`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.