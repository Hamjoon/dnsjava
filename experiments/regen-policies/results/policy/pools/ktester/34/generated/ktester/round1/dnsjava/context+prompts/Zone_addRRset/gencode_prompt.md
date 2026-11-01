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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Getter;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
public class Zone implements Serializable , Iterable<RRset> {
    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    @Getter
private Name origin;
    @Getter
private SOARecord SOA;
    public void addRRset(RRset rrset) {
        if (rrset == null) {
            throw new IllegalArgumentException("rrset must not be null");
        }
        Name name = rrset.getName();
        int type = rrset.getType();
        if (type == Type.SOA) {
            if (!name.equals(origin)) {
                throw new IllegalArgumentException((("SOA owner " + name) + " does not match zone origin ") + origin);
            }
            if (rrset.size() != 1) {
                throw new IllegalArgumentException(origin + ": exactly 1 SOA must be specified");
            }
        }
        if (!name.subdomain(origin)) {
            throw new IllegalArgumentException((("name " + name) + " is absolute and not a subdomain of ") + origin);
        }
        withWriteLock(() -> {
            addRRsetWithoutLock(name, rrset);
            if (type == Type.SOA) {
                SOA = ((SOARecord) (rrset.first()));
            }
        });
    }
public Iterator<RRset> AXFR();
    private void addRRsetWithoutLock(Name name, RRset rrset);
    public <T extends Record> void addRecord(T r);
    public RRset findExactMatch(Name name, int type);
    public SetResponse findRecords(Name name, int type);
    public int getDClass();
    public RRset getNS();
    @Override
public Iterator<RRset> iterator();
    public void removeRRset(Name name, int type);
    public void removeRecord(Record r);
    public String toMasterFile();
    @Override
public String toString();
    private void withWriteLock(Runnable callable);
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

 * A DNS zone. This encapsulates all data related to a zone, and provides convenient lookup methods.
 * A zone always contains a {@link SOARecord} and at least one {@link NSRecord}.
 *
 * @implNote This class uses a {@link java.util.concurrent.locks.ReadWriteLock} to ensure access and
 *     manipulation is safe from multiple threads. Iterators are "weakly consistent" (see the
 *     package info from {@code java.util.concurrent}).
 * @author Brian Wellington
 

api document of method addRRset(RRset): 

   * Adds an RRset to the zone.
   *
   * @implNote An existing {@link RRset} of the same {@link Name} and {@link Type} is
   *     <b>replaced</b>.
   * @param rrset The RRset to add.
   * @see RRset
   * @throws IllegalArgumentException if {@code rrset} is {@code null} or if the rrset name is not a
   *     {@link Name#subdomain(Name) subdomain} of the zone origin (or, in case of a SOA, is not
   *     equal to the zone origin).
   

dependent classes: 
class `org.xbill.DNS.RRset`:
	
class `org.xbill.DNS.Type`:
	dependent fields:
		int org.xbill.DNS.Type.SOA;
	
class `org.xbill.DNS.Zone`:
	dependent fields:
		org.xbill.DNS.SOARecord org.xbill.DNS.Zone.SOA;
		org.xbill.DNS.Name org.xbill.DNS.Zone.origin;
	related functions:
		method `private void validate()` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`;
		method `public void removeRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Removes a record from the zone.
   *
   * @param r The record to remove.
   * @throws IllegalArgumentException if {@code r} is {@code null}, if the record to remove is the
   *     {@link SOARecord} or the last {@link NSRecord}.
   ;
		method `private void fromXFR(ZoneTransferIn)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`;
		method `public SetResponse findRecords(Name, int)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Looks up Records in the zone. The answer can be a {@code CNAME} instead of the actual requested
   * type and wildcards are expanded.
   *
   * @param name The name to look up
   * @param type The type to look up
   * @return A SetResponse object
   * @throws IllegalArgumentException if {@code name} is {@code null}.
   * @throws InvalidTypeException if the specified {@code type} is invalid.
   * @see SetResponse
   ;
		method `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`;
		method `public void addRecord(T)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Adds a record to the zone. If there is an existing {@link RRset} of the same {@link Name} and
   * {@link Type}, the record is <b>added</b> to this set.
   *
   * @param r The record to add.
   * @throws IllegalArgumentException if {@code name} is {@code null} or if the rrset name does not
   *     match the zone origin.
   ;
class `org.xbill.DNS.Record`:
	related functions:
		method `public boolean sameRRset(RRset)` returns `boolean`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Determines if this Record could be part of the passed RRset. This compares the name, type, and
   * class of the Record and the set.
   *
   * @since 3.6
   ;
class `org.xbill.DNS.dnssec.ValUtils`:
	related functions:
		method `public static boolean nsecProvesNameError(SRRset, NSECRecord, Name)` returns `boolean`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: 
   * Determine if the given NSEC proves a NameError (NXDOMAIN) for a given qname.
   *
   * @param set The RRset that contains the NSEC.
   * @param nsec The NSEC to check.
   * @param qname The qname to check against.
   * @return true if the NSEC proves the condition.
   ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.