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
Here is the code of focal method, `removeRecord(Record)`, with the focal class `Zone`:
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
    public void removeRecord(Record r) {
        Name name = r.getName();
        int rtype = r.getRRsetType();
        synchronized(this) {
            RRset rrset = findRRset(name, rtype);
            if (rrset == null) {
                return;
            }
            if ((rrset.size() == 1) && rrset.first().equals(r)) {
                removeRRset(name, rtype);
            } else {
                rrset.deleteRR(r);
            }
        }
    }
public Iterator<RRset> AXFR();
    public void addRRset(RRset rrset);
    public <T extends Record> void addRecord(T r);
    public RRset findExactMatch(Name name, int type);
    private synchronized RRset findRRset(Name name, int type);
    public SetResponse findRecords(Name name, int type);
    public int getDClass();
    public RRset getNS();
    public Name getOrigin();
    public SOARecord getSOA();
    public Iterator<RRset> iterator();
    private synchronized void removeRRset(Name name, int type);
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
 

api document of method removeRecord(Record): 

   * Removes a record from the Zone
   *
   * @param r The record to be removed
   * @see Record
   

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Zone`:
	related functions:
		method `public void removeRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
   * Removes a record from the Zone
   *
   * @param r The record to be removed
   * @see Record
   ;
		method `public void addRecord(T)` returns `void`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
   * Adds a Record to the Zone
   *
   * @param r The record to be added
   * @see Record
   ;
class `org.xbill.DNS.RRset`:
	related functions:
		method `public int getDClass()` returns `int`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
   * Returns the class of the records
   *
   * @see DClass
   ;
		method `public Name getName()` returns `org.xbill.DNS.Name`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
   * Returns the name of the records
   *
   * @see Name
   ;
class `org.xbill.DNS.Message`:
	related functions:
		method `private static boolean sameSet(Record, Record)` returns `boolean`, related with `org.xbill.DNS.Zone.removeRecord(Record)`;
class `org.xbill.DNS.dnssec.NSEC3ValUtils`:
	related functions:
		method `private NSEC3Record findMatchingNSEC3(Name, Name, List<SRRset>)` returns `org.xbill.DNS.NSEC3Record`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
   * Find the NSEC3Record that matches a hash of a name.
   *
   * @param name The name to find.
   * @param zonename The name of the zone that the NSEC3s are from.
   * @param nsec3s A list of NSEC3Records from a given message.
   * @return The matching NSEC3Record if one is present, null otherwise.
   ;
class `org.xbill.DNS.dnssec.ValUtils`:
	related functions:
		method `public static void setCanonicalNsecOwner(SRRset, RRSIGRecord)` returns `void`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
   * Set the owner name of NSEC RRsets to the canonical name, i.e. the name that is <b>not</b>
   * expanded from a wildcard label.
   *
   * @param set The RRset to canonicalize.
   * @param sig The signature that validated this RRset.
   ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.