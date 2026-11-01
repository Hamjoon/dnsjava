As a professional Java software testing expert, your task is to generate test cases for a focal method.

You will be provided with:
1. The focal method that needs testing.
2. Basic information of the target class to which the focus method belongs, including fields and methods.

Ensure the generated test cases meet the following requirements:

Test Coverage Requirements:
- Create test cases to cover all conditional branches in the target method, including `if`, `else if`, `else`, `switch`, and `case` statements.

Output Format:
- Organize the test cases by its functions logically.
- Include input parameters, expected output, and a description for each test case.
- Use empty list `[]` for the input when a test case does not require input parameters.
- Provide an expected return value for each test case, or specify the behavior that should be verified if there's no return value.
- Clearly describe the intention of each test case in the description.

Follow the JSON format provided here for the output:
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
import java.io.InputStream;
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
    private SOARecord soaRecord;
    public <T extends Record> void addRecord(T r) {
        if (r == null) {
            throw new IllegalArgumentException("r must not be null");
        }
        Name name = r.getName();
        int rtype = r.getRRsetType();
        int actualType = r.getType();
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
                if (actualType == Type.SOA) {
                    rrset.deleteRR(soaRecord);
                    soaRecord = ((SOARecord) (r));
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
    public SOARecord getSOA();
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

@output{test cases}: output the test cases in the JSON format: