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
Here is the code of focal method, `getDClass()`, with the focal class `Zone`:
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
    public int getDClass() {
        return DClass.IN;
    }
public Iterator<RRset> AXFR();
    public void addRRset(RRset rrset);
    public <T extends Record> void addRecord(T r);
    public RRset findExactMatch(Name name, int type);
    public SetResponse findRecords(Name name, int type);
    public RRset getNS();
    @Override
public Iterator<RRset> iterator();
    public void removeRRset(Name name, int type);
    public void removeRecord(Record r);
    public String toMasterFile();
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

 * A DNS zone. This encapsulates all data related to a zone, and provides convenient lookup methods.
 * A zone always contains a {@link SOARecord} and at least one {@link NSRecord}.
 *
 * @implNote This class uses a {@link java.util.concurrent.locks.ReadWriteLock} to ensure access and
 *     manipulation is safe from multiple threads. Iterators are "weakly consistent" (see the
 *     package info from {@code java.util.concurrent}).
 * @author Brian Wellington
 

api document of method getDClass(): 
 Returns the zone's {@link DClass class}. 

return type: 
int

dependent classes: 
class `org.xbill.DNS.DClass`:
	dependent fields:
		int org.xbill.DNS.DClass.IN;
	
class `org.xbill.DNS.lookup.LookupSession`:
	related functions:
		method `public static LookupSessionBuilder defaultBuilder()` returns `org.xbill.DNS.lookup.LookupSession.LookupSessionBuilder`, related with `org.xbill.DNS.Zone.getDClass()`, api document: 
   * Returns a {@link LookupSessionBuilder} instance initialized with defaults.
   *
   * <ul>
   *   <li>Resolver: an {@link org.xbill.DNS.ExtendedResolver} initialized with the system's default
   *       DNS servers as determined by {@link org.xbill.DNS.ResolverConfig}.
   *   <li>ndots: as determined by {@link org.xbill.DNS.ResolverConfig}.
   *   <li>Cache: A cache for the {@code IN} class is installed.
   *   <li>Hosts: The local host database file is used.
   * </ul>
   ;
		method `private LookupResult lookupWithHosts(List<Name>, int)` returns `org.xbill.DNS.lookup.LookupResult`, related with `org.xbill.DNS.Zone.getDClass()`;
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method `public void addSecondaryZone(String, String)` returns `void`, related with `org.xbill.DNS.Zone.getDClass()`;
class `org.xbill.DNS.Record`:
	related functions:
		method `public String toString()` returns `java.lang.String`, related with `org.xbill.DNS.Zone.getDClass()`, api document:  Converts a Record into a String representation ;
class `org.xbill.DNS.Lookup`:
	related functions:
		method `private void checkDone()` returns `void`, related with `org.xbill.DNS.Zone.getDClass()`;
class `org.xbill.DNS.Master`:
	related functions:
		method `private void parseTTLClassAndType()` returns `void`, related with `org.xbill.DNS.Zone.getDClass()`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.