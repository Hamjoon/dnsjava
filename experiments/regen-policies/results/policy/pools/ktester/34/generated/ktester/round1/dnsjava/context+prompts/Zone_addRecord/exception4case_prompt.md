You are a professional software tester. Write test cases for a focal method in a target class.

**Instructions:**
1. Analyze method behavior and identify ALL possible exception scenarios
2. Design test cases that trigger each identified exception
3. Follow existing test case structure, Avoid duplication and create new test cases for uncovered exception scenarios

Test Case Format:
1. Group test cases by test function name
2. Each test case MUST include:
   - Input parameters with values (use empty list `[]` if none)
   - Expected exception or observable behavior
   - Clear description of test scenario
3. Return empty array if no exception scenarios found

Output Schema (STRICT JSON format):
```json
[
    {
        "group": "test name",
        "cases": [
            {
                "input": [
                    {
                        "parameter": "param name",
                        "value": "param value"
                    }
                ],
                "expected": "expected exception or behavior",
                "description": "test scenario description"
            }
        ]
    },
    {...}
]
```

@input{focal method and target class}
Here is the code of focal method, `addRecord(T)`, with the focal class `Zone`:
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
    public <T extends Record> void addRecord(T r) {
        if (r == null) {
            throw new IllegalArgumentException("r must not be null");
        }
        Name name = r.getName();
        int rtype = r.getRRsetType();
        if ((rtype == Type.SOA) && (!name.equals(origin))) {
            throw new IllegalArgumentException((("SOA owner " + name) + " does not match zone origin ") + origin);
        }
        if (!name.subdomain(origin)) {
            throw new IllegalArgumentException((("name " + name) + " is absolute and not a subdomain of ") + origin);
        }
        withWriteLock(() -> {
            RRset rrset = findRRsetWithoutLock(name, rtype);
            if (rrset == null) {
                rrset = new RRset(r);
                addRRsetWithoutLock(name, rrset);
            } else {
                // Adding a SOA must replace any existing record. We validated before that the zone name
                // didn't change
                if (rtype == Type.SOA) {
                    rrset.deleteRR(SOA);
                    SOA = ((SOARecord) (r));
                }
                rrset.addRR(r);
            }
        });
    }
public Iterator<RRset> AXFR();
    public void addRRset(RRset rrset);
    private void addRRsetWithoutLock(Name name, RRset rrset);
    public RRset findExactMatch(Name name, int type);
    private RRset findRRsetWithoutLock(Name name, int type);
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
@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: