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
Here is the code of focal method, `addRRset(RRset)`, with the focal class `Zone`:
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
    public void addRRset(RRset rrset) {
        Name name = rrset.getName();
        addRRset(name, rrset);
    }
public Iterator<RRset> AXFR();
    private synchronized void addRRset(Name name, RRset rrset);
    public <T extends Record> void addRecord(T r);
    public RRset findExactMatch(Name name, int type);
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
 

api document of method addRRset(RRset): 

   * Adds an RRset to the Zone
   *
   * @param rrset The RRset to be added
   * @see RRset
   

dependent classes: 
class `org.xbill.DNS.RRset`:
	
class `org.xbill.DNS.Zone`:
	related functions:
		method `public void addRRset(RRset)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Adds an RRset to the Zone
   *
   * @param rrset The RRset to be added
   * @see RRset
   ;
		method `public void addRecord(T)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Adds a Record to the Zone
   *
   * @param r The record to be added
   * @see Record
   ;
class `org.xbill.DNS.dnssec.SRRset`:
	related functions:
		method `public Name getName()` returns `org.xbill.DNS.Name`, related with `org.xbill.DNS.Zone.addRRset(RRset)`;
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method `private void addNS(Message, Zone, int)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`;
class `org.xbill.DNS.Update`:
	related functions:
		method `public void replace(RRset)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Indicates that all of the records in the rrset should be inserted into the zone replacing any
   * other records with the same name and type.
   ;
class `org.xbill.DNS.Cache`:
	related functions:
		method `public synchronized void addRRset(RRset, int)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Adds an RRset to the Cache.
   *
   * @param rrset The RRset to be added
   * @param cred The credibility of these records
   * @see RRset
   ;
class `org.xbill.DNS.dnssec.ValUtils`:
	related functions:
		method `public static Name rrsetWildcard(RRset)` returns `org.xbill.DNS.Name`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Determine by looking at a signed RRset whether or not the RRset name was the result of a
   * wildcard expansion. If so, return the name of the generating wildcard.
   *
   * @param rrset The rrset to chedck.
   * @return the wildcard name, if the rrset was synthesized from a wildcard. null if not.
   ;
class `org.xbill.DNS.DNSSEC`:
	related functions:
		method `public static RRSIGRecord sign(RRset, DNSKEYRecord, PrivateKey, Instant, Instant, String)` returns `org.xbill.DNS.RRSIGRecord`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Generate a DNSSEC signature. key and privateKey must refer to the same underlying cryptographic
   * key.
   *
   * @param rrset The data to be signed
   * @param key The DNSKEY record to use as part of signing
   * @param privkey The PrivateKey to use when signing
   * @param inception The time at which the signatures should become valid
   * @param expiration The time at which the signatures should expire
   * @param provider The name of the JCA provider. If non-null, it will be passed to JCA
   *     getInstance() methods.
   * @return The generated signature
   * @throws UnsupportedAlgorithmException The algorithm is unknown
   * @throws MalformedKeyException The key is malformed
   * @throws DNSSECException Some other error occurred.
   ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.