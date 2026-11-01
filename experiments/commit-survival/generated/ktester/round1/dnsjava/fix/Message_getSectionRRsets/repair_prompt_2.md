


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import org.mockito.Mockito;

class Message_getSectionRRsets_Test {

    /**
     * Returns {@code true} if the given {@code RRset} contains a record that
     * is {@code .equals()} to the supplied record.
     */
    private static boolean containsRecord(RRset rrset, Record record) {
        // RRset does not implement Iterable in the version used for this project,
        // but it provides a {@code rrs()} method that returns a {@code List<Record>}.
        for (Record r : rrset.rrs()) {
            if (r.equals(record)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the last record in the {@code RRset} according to its iterator.
     * If the set is empty, {@code null} is returned.
     */
    private static Record getLastRecord(RRset rrset) {
        Record last = null;
        for (Record r : rrset.rrs()) {
            last = r;
        }
        return last;
    }

    /**
     * Empty section should yield an empty list of RRsets.
     */
    @Test
    void emptySectionReturnsEmptyList() {
        // default constructor creates empty sections
        Message msg = new Message();
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertNotNull(rrsets);
        assertTrue(rrsets.isEmpty(), "RRset list should be empty for an empty section");
    }

    /**
     * A single record creates exactly one RRset containing that record.
     */
    @Test
    void singleRecordCreatesOneRRset() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "single");
        msg.addRecord(txt, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Should have exactly one RRset");
        RRset rs = rrsets.get(0);
        assertEquals(name, rs.getName());
        assertEquals(Type.TXT, rs.getType());
        assertEquals(DClass.IN, rs.getDClass());
        assertEquals(1, rs.size());
        assertEquals(txt, rs.first());
    }

    /**
     * Multiple records with the same name, type and class are merged into a single RRset.
     */
    @Test
    void sameNameSameTypeMergedIntoOneRRset() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("merge.example.");
        Record txt1 = new TXTRecord(name, DClass.IN, 3600, "first");
        Record txt2 = new TXTRecord(name, DClass.IN, 3600, "second");
        msg.addRecord(txt1, Section.ANSWER);
        msg.addRecord(txt2, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Records with same name/type/class should be merged");
        RRset rs = rrsets.get(0);
        assertEquals(2, rs.size(), "RRset should contain both records");
        assertTrue(containsRecord(rs, txt1));
        assertTrue(containsRecord(rs, txt2));
    }

    /**
     * Records with the same name but different types produce separate RRsets.
     */
    @Test
    void sameNameDifferentTypeSeparateRRsets() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("mixed.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "text");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        msg.addRecord(txt, Section.ANSWER);
        msg.addRecord(a, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Different types should yield two RRsets");
        // Verify that each RRset contains the correct record type
        boolean foundTxt = false, foundA = false;
        for (RRset rs : rrsets) {
            if (rs.getType() == Type.TXT) {
                foundTxt = true;
                assertEquals(1, rs.size());
                assertEquals(txt, rs.first());
            } else if (rs.getType() == Type.A) {
                foundA = true;
                assertEquals(1, rs.size());
                assertEquals(a, rs.first());
            }
        }
        assertTrue(foundTxt, "TXT RRset not found");
        assertTrue(foundA, "A RRset not found");
    }

    /**
     * Records with different names produce separate RRsets even if type and class match.
     */
    @Test
    void differentNamesSeparateRRsets() throws IOException {
        Message msg = new Message();
        Name name1 = Name.fromConstantString("first.example.");
        Name name2 = Name.fromConstantString("second.example.");
        Record txt1 = new TXTRecord(name1, DClass.IN, 3600, "one");
        Record txt2 = new TXTRecord(name2, DClass.IN, 3600, "two");
        msg.addRecord(txt1, Section.ANSWER);
        msg.addRecord(txt2, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Different names should yield two RRsets");
        // Ensure each RRset corresponds to the correct name
        boolean foundFirst = false, foundSecond = false;
        for (RRset rs : rrsets) {
            if (rs.getName().equals(name1)) {
                foundFirst = true;
                assertEquals(txt1, rs.first());
            } else if (rs.getName().equals(name2)) {
                foundSecond = true;
                assertEquals(txt2, rs.first());
            }
        }
        assertTrue(foundFirst, "RRset for first.example. not found");
        assertTrue(foundSecond, "RRset for second.example. not found");
    }

    /**
     * Section index that has never been initialized (null entry) returns Collections.emptyList().
     */
    @Test
    void sectionNullHandlingReturnsEmptyList() {
        Message msg = new Message();
        // Section 0 is guaranteed to be null in a freshly constructed Message
        List<RRset> rrsets = msg.getSectionRRsets(0);
        assertSame(Collections.emptyList(), rrsets, "Should return the immutable empty list when section is null");
    }

    /**
     * Calling with a negative index throws ArrayIndexOutOfBoundsException.
     */
    @Test
    void negativeSectionIndexThrows() {
        Message msg = new Message();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> msg.getSectionRRsets(-1), "Negative section index should throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Calling with an index beyond the internal array length throws ArrayIndexOutOfBoundsException.
     */
    @Test
    void outOfBoundsSectionIndexThrows() throws Exception {
        Message msg = new Message();
        // Use reflection to discover the length of the internal sections array
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        // one past the last valid index
        int outOfBounds = sections.length;
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> msg.getSectionRRsets(outOfBounds), "Out‑of‑bounds section index should throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Empty but initialized section (empty List) yields an empty mutable list (size 0).
     */
    @Test
    void emptyInitializedSectionReturnsMutableEmptyList() throws Exception {
        Message msg = new Message();
        // Force sections[1] to be an empty ArrayList (simulating an initialized but empty section)
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        // Section 1 now non‑null but empty
        sections[1] = new ArrayList<>();
        List<RRset> rrsets = msg.getSectionRRsets(1);
        assertNotNull(rrsets);
        assertTrue(rrsets.isEmpty(), "Should return an empty list for an empty initialized section");
        // Verify the list is mutable (adding an element does not throw)
        assertDoesNotThrow(() -> rrsets.add(Mockito.mock(RRset.class)));
    }

    /**
     * Same name & type but different DClass values produce separate RRsets.
     */
    @Test
    void sameNameDifferentDClassSeparateRRsets() throws Exception {
        Message msg = new Message();
        Name name = Name.fromConstantString("dclass.example.");
        Record inRecord = new TXTRecord(name, DClass.IN, 3600, "in");
        Record chRecord = new TXTRecord(name, DClass.CH, 3600, "ch");
        msg.addRecord(inRecord, Section.ANSWER);
        msg.addRecord(chRecord, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Different DClass values should yield two RRsets");
        boolean foundIn = false, foundCh = false;
        for (RRset rs : rrsets) {
            if (rs.getDClass() == DClass.IN) {
                foundIn = true;
                assertEquals(inRecord, rs.first());
            } else if (rs.getDClass() == DClass.CH) {
                foundCh = true;
                assertEquals(chRecord, rs.first());
            }
        }
        assertTrue(foundIn, "IN DClass RRset not found");
        assertTrue(foundCh, "CH DClass RRset not found");
    }

    /**
     * Non‑consecutive matching records are merged correctly.
     */
    @Test
    void nonConsecutiveMatchingRecordMerges() throws Exception {
        Message msg = new Message();
        Name nameX = Name.fromConstantString("x.example.");
        Name nameY = Name.fromConstantString("y.example.");
        Record a1 = new ARecord(nameX, DClass.IN, 3600, InetAddress.getByName("1.1.1.1"));
        Record a2 = new ARecord(nameY, DClass.IN, 3600, InetAddress.getByName("2.2.2.2"));
        Record a3 = new ARecord(nameX, DClass.IN, 3600, InetAddress.getByName("3.3.3.3"));
        msg.addRecord(a1, Section.ANSWER);
        msg.addRecord(a2, Section.ANSWER);
        msg.addRecord(a3, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Should produce two RRsets");
        RRset rsX = null, rsY = null;
        for (RRset rs : rrsets) {
            if (rs.getName().equals(nameX)) {
                rsX = rs;
            } else if (rs.getName().equals(nameY)) {
                rsY = rs;
            }
        }
        assertNotNull(rsX, "RRset for name X missing");
        assertNotNull(rsY, "RRset for name Y missing");
        assertEquals(2, rsX.size(), "RRset X should contain both matching records");
        assertTrue(containsRecord(rsX, a1));
        assertTrue(containsRecord(rsX, a3));
        assertEquals(1, rsY.size());
        assertTrue(containsRecord(rsY, a2));
    }

    /**
     * Adding the same Record instance twice results in a RRset with duplicate entries.
     */
    @Test
    void duplicateRecordInstanceCreatesDuplicateEntry() throws Exception {
        Message msg = new Message();
        Name name = Name.fromConstantString("dup.example.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "dup");
        msg.addRecord(txt, Section.ANSWER);
        // same instance added again
        msg.addRecord(txt, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Should still be a single RRset");
        RRset rs = rrsets.get(0);
        assertEquals(2, rs.size(), "RRset should contain two entries (duplicates)");
        // The RRset stores the same instance twice; both positions should be equal to txt
        assertEquals(txt, rs.first());
        assertEquals(txt, getLastRecord(rs));
    }

    /**
     * Mixed types and DClasses produce the expected number of distinct RRsets.
     */
    @Test
    void mixedTypesAndDClassesComplexScenario() throws Exception {
        Message msg = new Message();
        Name nameX = Name.fromConstantString("x.example.");
        Name nameZ = Name.fromConstantString("z.example.");
        // Two IN A records
        Record a1 = new ARecord(nameX, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Record a2 = new ARecord(nameX, DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        // One CH A record
        Record a3 = new ARecord(nameX, DClass.CH, 3600, InetAddress.getByName("10.0.0.3"));
        // One IN TXT record
        Record txt = new TXTRecord(nameX, DClass.IN, 3600, "hello");
        // One IN A record with a different name
        Record a4 = new ARecord(nameZ, DClass.IN, 3600, InetAddress.getByName("10.0.0.4"));
        msg.addRecord(a1, Section.ANSWER);
        msg.addRecord(a2, Section.ANSWER);
        msg.addRecord(a3, Section.ANSWER);
        msg.addRecord(txt, Section.ANSWER);
        msg.addRecord(a4, Section.ANSWER);
        List<RRset> rrsets = msg.getSectionRRsets(Section.ANSWER);
        assertEquals(3, rrsets.size(), "Expected three distinct RRsets");
        // Helper to locate an RRset
        RRset rsA_IN = null, rsA_CH = null, rsTXT = null;
        for (RRset rs : rrsets) {
            if (rs.getName().equals(nameX) && rs.getType() == Type.A && rs.getDClass() == DClass.IN) {
                rsA_IN = rs;
            } else if (rs.getName().equals(nameX) && rs.getType() == Type.A && rs.getDClass() == DClass.CH) {
                rsA_CH = rs;
            } else if (rs.getName().equals(nameX) && rs.getType() == Type.TXT && rs.getDClass() == DClass.IN) {
                rsTXT = rs;
            }
        }
        assertNotNull(rsA_IN, "IN A RRset missing");
        assertNotNull(rsA_CH, "CH A RRset missing");
        assertNotNull(rsTXT, "IN TXT RRset missing");
        assertEquals(2, rsA_IN.size(), "IN A RRset should contain two records");
        assertTrue(containsRecord(rsA_IN, a1));
        assertTrue(containsRecord(rsA_IN, a2));
        assertEquals(1, rsA_CH.size());
        assertTrue(containsRecord(rsA_CH, a3));
        assertEquals(1, rsTXT.size());
        assertTrue(containsRecord(rsTXT, txt));
    }

    /**
     * Adding a null Record to a section causes NullPointerException during processing.
     */
    @Test
    void nullRecordHandlingThrowsNPE() throws Exception {
        Message msg = new Message();
        // Force a null entry into the list for section 6
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        sections[6] = new ArrayList<>();
        // insert null record
        sections[6].add(null);
        assertThrows(NullPointerException.class, () -> msg.getSectionRRsets(6), "Iterating over a null record should throw NullPointerException");
    }

    /**
     * A Record whose getName() returns null triggers NullPointerException.
     */
    @Test
    void recordWithNullNameThrowsNPE() throws Exception {
        Message msg = new Message();
        // Create a mock Record that returns null for getName()
        Record mock = Mockito.mock(Record.class);
        Mockito.when(mock.getName()).thenReturn(null);
        // other required methods to avoid additional NPEs
        Mockito.when(mock.getRRsetType()).thenReturn(Type.A);
        Mockito.when(mock.getDClass()).thenReturn(DClass.IN);
        // Insert the mock into an initialized section via reflection
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        sections[7] = new ArrayList<>();
        sections[7].add(mock);
        assertThrows(NullPointerException.class, () -> msg.getSectionRRsets(7), "Record with null name should cause NullPointerException");
    }

    /**
     * If the internal sections array itself is null, any call results in NullPointerException.
     */
    @Test
    void internalSectionsArrayNullThrowsNPE() throws Exception {
        Message msg = new Message();
        // Set the private 'sections' field to null via reflection
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, null);
        assertThrows(NullPointerException.class, () -> msg.getSectionRRsets(0), "Null internal sections array should cause NullPointerException");
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/commit-survival/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended

╷
├─ JUnit Jupiter ✔
│  └─ Message_getSectionRRsets_Test ✔
│     ├─ duplicateRecordInstanceCreatesDuplicateEntry() ✘ RRset should contain two entries (duplicates) ==> expected: <2> but was: <1>
│     ├─ mixedTypesAndDClassesComplexScenario() ✘ Expected three distinct RRsets ==> expected: <3> but was: <4>
│     ├─ outOfBoundsSectionIndexThrows() ✔
│     ├─ sameNameSameTypeMergedIntoOneRRset() ✔
│     ├─ emptyInitializedSectionReturnsMutableEmptyList() ✔
│     ├─ emptySectionReturnsEmptyList() ✔
│     ├─ recordWithNullNameThrowsNPE() ✘ Index 7 out of bounds for length 4
│     ├─ negativeSectionIndexThrows() ✔
│     ├─ sameNameDifferentTypeSeparateRRsets() ✔
│     ├─ nonConsecutiveMatchingRecordMerges() ✔
│     ├─ nullRecordHandlingThrowsNPE() ✘ Index 6 out of bounds for length 4
│     ├─ internalSectionsArrayNullThrowsNPE() ✔
│     ├─ differentNamesSeparateRRsets() ✔
│     ├─ singleRecordCreatesOneRRset() ✔
│     ├─ sectionNullHandlingReturnsEmptyList() ✔
│     └─ sameNameDifferentDClassSeparateRRsets() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (4):
  JUnit Jupiter:Message_getSectionRRsets_Test:duplicateRecordInstanceCreatesDuplicateEntry()
    MethodSource [className = 'org.xbill.DNS.Message_getSectionRRsets_Test', methodName = 'duplicateRecordInstanceCreatesDuplicateEntry', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: RRset should contain two entries (duplicates) ==> expected: <2> but was: <1>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:563)
       org.xbill.DNS.Message_getSectionRRsets_Test.duplicateRecordInstanceCreatesDuplicateEntry(Message_getSectionRRsets_Test.java:279)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getSectionRRsets_Test:mixedTypesAndDClassesComplexScenario()
    MethodSource [className = 'org.xbill.DNS.Message_getSectionRRsets_Test', methodName = 'mixedTypesAndDClassesComplexScenario', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected three distinct RRsets ==> expected: <3> but was: <4>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:563)
       org.xbill.DNS.Message_getSectionRRsets_Test.mixedTypesAndDClassesComplexScenario(Message_getSectionRRsets_Test.java:308)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_getSectionRRsets_Test:recordWithNullNameThrowsNPE()
    MethodSource [className = 'org.xbill.DNS.Message_getSectionRRsets_Test', methodName = 'recordWithNullNameThrowsNPE', methodParameterTypes = '']
    => java.lang.ArrayIndexOutOfBoundsException: Index 7 out of bounds for length 4
       org.xbill.DNS.Message_getSectionRRsets_Test.recordWithNullNameThrowsNPE(Message_getSectionRRsets_Test.java:364)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:Message_getSectionRRsets_Test:nullRecordHandlingThrowsNPE()
    MethodSource [className = 'org.xbill.DNS.Message_getSectionRRsets_Test', methodName = 'nullRecordHandlingThrowsNPE', methodParameterTypes = '']
    => java.lang.ArrayIndexOutOfBoundsException: Index 6 out of bounds for length 4
       org.xbill.DNS.Message_getSectionRRsets_Test.nullRecordHandlingThrowsNPE(Message_getSectionRRsets_Test.java:342)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]

Test run finished after 720 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        16 tests found           ]
[         0 tests skipped         ]
[        16 tests started         ]
[         0 tests aborted         ]
[        12 tests successful      ]
[         4 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class org.xbill.DNS.Message: 

 * A DNS Message. A message is the basic unit of communication between the client and server of a
 * DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 

api document of method getSectionRRsets(int): 

   * Returns an array containing all records in the given section grouped into RRsets.
   *
   * @see RRset
   * @see Section
   

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
	related functions:
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public List<Record> getSection(int)` returns `java.util.List<org.xbill.DNS.Record>`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: 
   * Returns all records in the given section, or an empty list if the section is empty.
   *
   * @see Record
   * @see Section
   ;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
		method `public Record getQuestion()` returns `org.xbill.DNS.Record`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: 
   * Returns the first record in the QUESTION section.
   *
   * @see Record
   * @see Section
   ;
		method `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`;
		method `public boolean findRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: 
   * Determines if the given record is already present in the given section.
   *
   * @see Record
   * @see Section
   ;
		method `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: 
   * Removes a record from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
class `org.xbill.DNS.dnssec.ValidatingResolver`:
	related functions:
		method `public void loadTrustAnchors(InputStream)` returns `void`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: 
   * Load the trust anchor file into the trust anchor store. The trust anchors are currently stored
   * in a zone file format list of DNSKEY or DS records.
   *
   * @param data The trust anchor data.
   * @throws IOException when the trust anchor data could not be read.
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
