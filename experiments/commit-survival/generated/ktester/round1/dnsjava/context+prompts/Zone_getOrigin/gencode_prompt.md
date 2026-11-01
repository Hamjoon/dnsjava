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
Here is the code of focal method, `getOrigin()`, with the focal class `Zone`:
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
    private Name origin;
    public Name getOrigin() {
        return origin;
    }
public Iterator<RRset> AXFR();
    public void addRRset(RRset rrset);
    public <T extends Record> void addRecord(T r);
    public RRset findExactMatch(Name name, int type);
    public SetResponse findRecords(Name name, int type);
    public int getDClass();
    public RRset getNS();
    public SOARecord getSOA();
    public Iterator<RRset> iterator();
    public void removeRecord(Record r);
    public synchronized String toMasterFile();
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
 

api document of method getOrigin(): 
 Returns the Zone's origin 

return type: 
org.xbill.DNS.Name

dependent classes: 
class `org.xbill.DNS.Zone`:
	dependent fields:
		org.xbill.DNS.Name org.xbill.DNS.Zone.origin;
	related functions:
		method `public synchronized String toMasterFile()` returns `java.lang.String`, related with `org.xbill.DNS.Zone.getOrigin()`, api document:  Returns the contents of the Zone in master file format. ;
		method `public Name getOrigin()` returns `org.xbill.DNS.Name`, related with `org.xbill.DNS.Zone.getOrigin()`, api document:  Returns the Zone's origin ;
		method `private void validate()` returns `void`, related with `org.xbill.DNS.Zone.getOrigin()`;
		method `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.getOrigin()`;
		method `private synchronized SetResponse lookup(Name, int)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Zone.getOrigin()`;
class `org.xbill.DNS.Zone.ZoneIterator`:
	related functions:
		method `public RRset next()` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Zone.getOrigin()`;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Zone.getOrigin()`, api document:  Converts a numeric extended flag into a String ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.