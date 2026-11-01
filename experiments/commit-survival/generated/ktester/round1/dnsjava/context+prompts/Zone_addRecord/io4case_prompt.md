As a professional Java software testing expert, your task is to generate test cases for a focal method.

Guidelines:
1. Analyze the method's functionality and identify possible input combinations including normal cases and boundary conditions.
2. Review the provided existing test cases. Avoid duplication and create new test cases for untested input combinations.

Context Provided:
1. The focal method that needs testing.
2. Relevant code and metadata for the class containing the method, including fields and other methods.
3. The existing test cases (in JSON format) to help avoid redundancy.

Output Instructions:
- Organize the test cases by the name of the test function (group).
- Each test case must include:
    - input: A list of parameter names and values. Use an empty list `[]` if no input is required.
    - expected: The return value or expected observable behavior.
    - description: A clear and concise explanation of the scenario and the intent of the test case.

Maintain the following strict JSON format:
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

@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: