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
Here is the code of focal method, `addRecord(T)`, with the focal class `Zone`:
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
    public <T extends Record> void addRecord(T r) {
        Name name = r.getName();
        int rtype = r.getRRsetType();
        synchronized(this) {
            RRset rrset = findRRset(name, rtype);
            if (rrset == null) {
                rrset = new RRset(r);
                addRRset(name, rrset);
            } else {
                rrset.addRR(r);
            }
        }
    }
public Iterator<RRset> AXFR();
    private synchronized void addRRset(Name name, RRset rrset);
    public void addRRset(RRset rrset);
    public RRset findExactMatch(Name name, int type);
    private synchronized RRset findRRset(Name name, int type);
    public SetResponse findRecords(Name name, int type);
    public int getDClass();
    public RRset getNS();
    public Name getOrigin();
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
 

api document of method addRecord(T): 

   * Adds a Record to the Zone
   *
   * @param r The record to be added
   * @see Record
   

dependent classes: 
class `org.xbill.DNS.Zone`:
	related functions:
		method `public void addRecord(T)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: 
   * Adds a Record to the Zone
   *
   * @param r The record to be added
   * @see Record
   ;
class `org.xbill.DNS.Cache`:
	related functions:
		method `public synchronized void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: 
   * Adds a record to the Cache.
   *
   * @param r The record to be added
   * @param cred The credibility of the record
   * @see Record
   ;
class `org.xbill.DNS.Message`:
	related functions:
		method `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: 
   * Returns an array containing all records in the given section grouped into RRsets.
   *
   * @see RRset
   * @see Section
   ;
		method `private static boolean sameSet(Record, Record)` returns `boolean`, related with `org.xbill.DNS.Zone.addRecord(T)`;
class `org.xbill.DNS.RRset`:
	related functions:
		method `public Name getName()` returns `org.xbill.DNS.Name`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: 
   * Returns the name of the records
   *
   * @see Name
   ;
class `org.xbill.DNS.dnssec.DefaultTrustAnchorStore`:
	related functions:
		method `public void store(RRset)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: 
   * Stores the given RRset as known trusted keys. Existing keys for the same name and class are
   * overwritten.
   *
   * @param rrset The key set to store as trusted.
   ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.