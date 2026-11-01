
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

class Message_getSectionRRsets_Test {

    private Message message;

    /**
     * A Record subclass that deliberately throws an exception from {@code sameRRset}
     * to test exception propagation.
     */
    private static class ExplodingRecord extends Record {

        ExplodingRecord(Name name) {
            super(name, Type.A, DClass.IN, 0);
        }

        @Override
        public String rdataToString() {
            return "";
        }

        @Override
        protected void rrToWire(DNSOutput out, Compression c, boolean canonical) {
            // no‑op for the test
        }

        @Override
        public Record withNewName(Name name) {
            return this;
        }

        @Override
        public boolean sameRRset(RRset set) {
            throw new IllegalStateException("boom");
        }

        @Override
        protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
            // no‑op – the record never parses RDATA in these tests
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Start each test with a fresh, empty Message.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent.
        message = null;
    }

    /**
     * Sets the internal {@code sections} array to the supplied value via reflection.
     */
    private void setSectionsArray(List<Record>[] sections) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(message, sections);
    }

    private static Stream<Arguments> invalidSectionProvider() {
        return Stream.of(Arguments.of(-1), Arguments.of(5));
    }

    /**
     * Empty section must yield an immutable empty list.
     */
    @Test
    void emptySectionReturnsEmptyList() {
        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertNotNull(rrsets, "Returned list must not be null");
        assertTrue(rrsets.isEmpty(), "Empty section should produce an empty list");
        assertThrows(UnsupportedOperationException.class, () -> rrsets.add(null), "The empty list should be immutable (Collections.emptyList())");
    }

    /**
     * A single record creates a single RRset containing that record.
     */
    @Test
    void singleRecordCreatesOneRRset() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        message.addRecord(a, Section.ANSWER);
        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Exactly one RRset expected");
        RRset set = rrsets.get(0);
        assertEquals(1, set.size(), "RRset should contain the single record");
        assertTrue(set.containsRR(a), "RRset must contain the added record");
    }

    /**
     * Multiple records that belong to the same RRset are aggregated.
     */
    @Test
    void multipleSameRRsetRecordsAggregated() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a1 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        Record a2 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        message.addRecord(a1, Section.ANSWER);
        message.addRecord(a2, Section.ANSWER);
        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertEquals(1, rrsets.size(), "Records of the same name/type/class must be in one RRset");
        RRset set = rrsets.get(0);
        assertEquals(2, set.size(), "RRset should contain both A records");
        assertTrue(set.containsRR(a1) && set.containsRR(a2), "Both records must be present");
    }

    /**
     * Records that differ by type (or name) produce distinct RRsets.
     */
    @Test
    void differentRecordsCreateSeparateRRsets() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        Record mx = new MXRecord(name, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com."));
        message.addRecord(a, Section.ANSWER);
        message.addRecord(mx, Section.ANSWER);
        List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
        assertEquals(2, rrsets.size(), "Different types should result in two RRsets");
        boolean aFound = false, mxFound = false;
        for (RRset set : rrsets) {
            if (set.getType() == Type.A) {
                aFound = true;
                assertEquals(1, set.size());
                assertTrue(set.containsRR(a));
            } else if (set.getType() == Type.MX) {
                mxFound = true;
                assertEquals(1, set.size());
                assertTrue(set.containsRR(mx));
            }
        }
        assertTrue(aFound && mxFound, "Both A and MX RRsets must be present");
    }

    /**
     * Section index validation – passing an invalid section should throw IllegalArgumentException.
     */
    @Test
    void invalidSectionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> message.getSectionRRsets(4), "Section index outside 0‑3 must cause IllegalArgumentException");
    }

    /**
     * Returns the current {@code sections} array (may be {@code null}).
     */
    @SuppressWarnings("unchecked")
    private List<Record>[] getSectionsArray() throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        return (List<Record>[]) f.get(message);
    }

    @ParameterizedTest(name = "invalid section {0} throws IllegalArgumentException")
    @MethodSource("invalidSectionProvider")
    void invalidSectionHandling(int invalidSection) {
        assertThrows(IllegalArgumentException.class, () -> message.getSectionRRsets(invalidSection), "Section.check should reject out‑of‑range values");
    }

    @Test
    @DisplayName("Null internal sections array causes NullPointerException")
    void nullSectionsArrayCausesNPE() throws Exception {
        // simulate un‑initialised internal array
        setSectionsArray(null);
        assertThrows(NullPointerException.class, () -> message.getSectionRRsets(0), "Accessing a null sections array must raise NPE");
    }

    @Test
    @DisplayName("Insufficient sections array length causes ArrayIndexOutOfBoundsException")
    void insufficientArrayLengthCausesAIOOBE() throws Exception {
        // Create an array shorter than the maximum index (4)
        @SuppressWarnings("unchecked")
        List<Record>[] shortArray = (List<Record>[]) new List[2];
        setSectionsArray(shortArray);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.getSectionRRsets(2), "Indexing beyond the array length must raise AIOOBE");
    }

    @Test
    @DisplayName("Null record inside a section list causes NullPointerException")
    void nullRecordInSectionListCausesNPE() throws Exception {
        List<Record>[] arr = (List<Record>[]) new List[Message.MAXLENGTH];
        List<Record> list = new ArrayList<>();
        // deliberately insert a null element
        list.add(null);
        arr[1] = list;
        setSectionsArray(arr);
        assertThrows(NullPointerException.class, () -> message.getSectionRRsets(1), "Iterating over a null Record must raise NPE");
    }

    @Test
    @DisplayName("Exception thrown by Record.sameRRset propagates")
    void sameRRsetExceptionPropagates() throws Exception {
        List<Record>[] arr = (List<Record>[]) new List[4];
        List<Record> list = new ArrayList<>();
        list.add(new ExplodingRecord(Name.fromConstantString("bad.example.")));
        arr[0] = list;
        setSectionsArray(arr);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> message.getSectionRRsets(0), "RuntimeException from sameRRset must not be swallowed");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("Empty non‑null section returns mutable empty list")
    void emptyButNonNullSectionReturnsMutableList() throws Exception {
        List<Record>[] arr = (List<Record>[]) new List[4];
        // non‑null but empty
        arr[0] = new ArrayList<>();
        setSectionsArray(arr);
        List<RRset> rrsets = message.getSectionRRsets(0);
        assertNotNull(rrsets);
        assertTrue(rrsets.isEmpty(), "Should be empty because there are no records");
        // The list must be mutable – adding a dummy element should not throw.
        RRset dummy = new RRset(new ARecord(Name.fromConstantString("dummy."), DClass.IN, 0, InetAddress.getByName("1.1.1.1")));
        assertDoesNotThrow(() -> rrsets.add(dummy), "Returned list must be a fresh mutable LinkedList, not Collections.emptyList()");
        assertEquals(1, rrsets.size(), "Mutable list should now contain the dummy RRset");
    }

    @Test
    @DisplayName("All records distinct → three RRsets")
    void allDistinctRecordsCreateSeparateRRsets() throws UnknownHostException, Exception {
        List<Record>[] arr = (List<Record>[]) new List[4];
        List<Record> list = new ArrayList<>();
        // Record 1: A example.com.
        Name n1 = Name.fromConstantString("example.com.");
        Record a1 = new ARecord(n1, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        // Record 2: MX example.org.
        Name n2 = Name.fromConstantString("example.org.");
        Record mx = new MXRecord(n2, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.org."));
        // Record 3: TXT example.net.
        Name n3 = Name.fromConstantString("example.net.");
        Record txt = new TXTRecord(n3, DClass.IN, 3600, "hello");
        list.add(a1);
        list.add(mx);
        list.add(txt);
        arr[0] = list;
        setSectionsArray(arr);
        List<RRset> rrsets = message.getSectionRRsets(0);
        assertEquals(3, rrsets.size(), "Each distinct record should produce its own RRset");
        // Verify each RRset contains exactly one record and matches the type.
        assertTrue(rrsets.stream().anyMatch(r -> r.getType() == Type.A && r.size() == 1));
        assertTrue(rrsets.stream().anyMatch(r -> r.getType() == Type.MX && r.size() == 1));
        assertTrue(rrsets.stream().anyMatch(r -> r.getType() == Type.TXT && r.size() == 1));
    }

    @Test
    @DisplayName("Two records share RRset, third is distinct")
    void someRecordsShareRRset() throws UnknownHostException, Exception {
        List<Record>[] arr = (List<Record>[]) new List[4];
        List<Record> list = new ArrayList<>();
        Name name = Name.fromConstantString("shared.example.");
        Record a1 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Record a2 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        // Distinct record
        Record txt = new TXTRecord(Name.fromConstantString("other.example."), DClass.IN, 3600, "solo");
        list.add(a1);
        list.add(a2);
        list.add(txt);
        arr[1] = list;
        setSectionsArray(arr);
        List<RRset> rrsets = message.getSectionRRsets(1);
        assertEquals(2, rrsets.size(), "Expected two RRsets: one aggregated, one distinct");
        // Find the aggregated A RRset
        RRset aSet = rrsets.stream().filter(r -> r.getType() == Type.A).findFirst().orElseThrow(() -> new AssertionError("A RRset missing"));
        assertEquals(2, aSet.size(), "Aggregated A RRset must contain both A records");
        assertTrue(aSet.containsRR(a1) && aSet.containsRR(a2));
        // Find the TXT RRset
        RRset txtSet = rrsets.stream().filter(r -> r.getType() == Type.TXT).findFirst().orElseThrow(() -> new AssertionError("TXT RRset missing"));
        assertEquals(1, txtSet.size());
        assertTrue(txtSet.containsRR(txt));
    }

    @Test
    @DisplayName("Three same‑type records + one distinct → two RRsets")
    void multipleRecordsSameRRsetAfterIterations() throws UnknownHostException, Exception {
        List<Record>[] arr = (List<Record>[]) new List[4];
        List<Record> list = new ArrayList<>();
        Name shared = Name.fromConstantString("shared.example.");
        Record a1 = new ARecord(shared, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Record a2 = new ARecord(shared, DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        Record a3 = new ARecord(shared, DClass.IN, 3600, InetAddress.getByName("10.0.0.3"));
        Record txt = new TXTRecord(Name.fromConstantString("other.example."), DClass.IN, 3600, "solo");
        list.add(a1);
        list.add(a2);
        list.add(a3);
        list.add(txt);
        arr[3] = list;
        setSectionsArray(arr);
        List<RRset> rrsets = message.getSectionRRsets(3);
        assertEquals(2, rrsets.size(), "Should be two RRsets: aggregated A and distinct TXT");
        RRset aSet = rrsets.stream().filter(r -> r.getType() == Type.A).findFirst().orElseThrow(() -> new AssertionError("A RRset missing"));
        assertEquals(3, aSet.size(), "A RRset must contain three records");
        assertTrue(aSet.containsRR(a1) && aSet.containsRR(a2) && aSet.containsRR(a3));
        RRset txtSet = rrsets.stream().filter(r -> r.getType() == Type.TXT).findFirst().orElseThrow(() -> new AssertionError("TXT RRset missing"));
        assertEquals(1, txtSet.size());
        assertTrue(txtSet.containsRR(txt));
    }

    @Test
    @DisplayName("RRset order respects first appearance of distinct RRsets")
    void rrsetOrderFollowsFirstOccurrence() throws UnknownHostException, Exception {
        List<Record>[] arr = (List<Record>[]) new List[4];
        List<Record> list = new ArrayList<>();
        // R1 and R2 are A records with same name/type/class
        Name aName = Name.fromConstantString("a.example.");
        Record r1 = new ARecord(aName, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Record r2 = new ARecord(aName, DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        // R3 and R4 are MX records with same name/type/class (different from A)
        Name mxName = Name.fromConstantString("mx.example.");
        Record r3 = new MXRecord(mxName, DClass.IN, 3600, 5, Name.fromConstantString("mail1.mx.example."));
        Record r4 = new MXRecord(mxName, DClass.IN, 3600, 10, Name.fromConstantString("mail2.mx.example."));
        // Insert in the required order
        list.add(r1);
        list.add(r2);
        list.add(r3);
        list.add(r4);
        arr[2] = list;
        setSectionsArray(arr);
        List<RRset> rrsets = message.getSectionRRsets(2);
        assertEquals(2, rrsets.size(), "Two distinct RRsets expected");
        // First RRset must be the A‑type one (created when R1 is seen)
        RRset first = rrsets.get(0);
        assertEquals(Type.A, first.getType(), "First RRset should be of type A");
        assertEquals(2, first.size());
        assertTrue(first.containsRR(r1) && first.containsRR(r2));
        // Second RRset must be the MX‑type one
        RRset second = rrsets.get(1);
        assertEquals(Type.MX, second.getType(), "Second RRset should be of type MX");
        assertEquals(2, second.size());
        assertTrue(second.containsRR(r3) && second.containsRR(r4));
    }

    @Test
    @DisplayName("Each call returns a new mutable list")
    void freshListReturnedEachCall() throws UnknownHostException, Exception {
        // Populate section 0 with a single A record
        Name name = Name.fromConstantString("fresh.example.");
        Record a = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        message.addRecord(a, Section.ANSWER);
        List<RRset> firstCall = message.getSectionRRsets(Section.ANSWER);
        assertEquals(1, firstCall.size());
        // Modify the list returned by the first call
        RRset dummy = new RRset(new ARecord(Name.fromConstantString("dummy."), DClass.IN, 0, InetAddress.getByName("1.1.1.1")));
        firstCall.add(dummy);
        assertEquals(2, firstCall.size(), "First call list should be mutable");
        // Second call must not see the dummy RRset and must be a different object
        List<RRset> secondCall = message.getSectionRRsets(Section.ANSWER);
        assertNotSame(firstCall, secondCall, "Each invocation must produce a new List instance");
        assertEquals(1, secondCall.size(), "Second call should reflect original state only");
        assertFalse(secondCall.contains(dummy), "Dummy RRset must not be present in second call");
    }

    @Test
    @DisplayName("Upper valid section index (3) with null slot returns empty immutable list")
    void upperValidSectionNullSlotReturnsEmpty() {
        // By default, sections[3] is null for a freshly constructed Message
        List<RRset> rrsets = message.getSectionRRsets(3);
        assertNotNull(rrsets);
        assertTrue(rrsets.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> rrsets.add(null), "Should be the immutable Collections.emptyList()");
    }

    @Test
    @DisplayName("Identical name/type/class but different RDATA → single RRset")
    void identicalNameTypeClassDifferentRdataAggregated() throws UnknownHostException, Exception {
        List<Record>[] arr = (List<Record>[]) new List[4];
        List<Record> list = new ArrayList<>();
        Name name = Name.fromConstantString("multi.example.");
        Record a1 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Record a2 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        Record a3 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.3"));
        list.add(a1);
        list.add(a2);
        list.add(a3);
        arr[1] = list;
        setSectionsArray(arr);
        List<RRset> rrsets = message.getSectionRRsets(1);
        assertEquals(1, rrsets.size(), "All three A records should be aggregated into one RRset");
        RRset set = rrsets.get(0);
        assertEquals(Type.A, set.getType());
        assertEquals(3, set.size());
        assertTrue(set.containsRR(a1) && set.containsRR(a2) && set.containsRR(a3));
    }

    @Test
    @DisplayName("Null section list returns empty immutable collection")
    void nullSectionListReturnsEmptyCollection() throws Exception {
        // Ensure sections[2] is null
        List<Record>[] arr = (List<Record>[]) new List[4];
        setSectionsArray(arr);
        List<RRset> rrsets = message.getSectionRRsets(2);
        assertNotNull(rrsets);
        assertTrue(rrsets.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> rrsets.add(null));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:32: error: ExplodingRecord is not abstract and does not override abstract method rrToString() in Record
    private static class ExplodingRecord extends Record {
                   ^
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:48: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:112: error: cannot find symbol
        assertTrue(set.containsRR(a), "RRset must contain the added record");
                      ^
  symbol:   method containsRR(Record)
  location: variable set of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:129: error: cannot find symbol
        assertTrue(set.containsRR(a1) && set.containsRR(a2), "Both records must be present");
                      ^
  symbol:   method containsRR(Record)
  location: variable set of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:129: error: cannot find symbol
        assertTrue(set.containsRR(a1) && set.containsRR(a2), "Both records must be present");
                                            ^
  symbol:   method containsRR(Record)
  location: variable set of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:149: error: cannot find symbol
                assertTrue(set.containsRR(a));
                              ^
  symbol:   method containsRR(Record)
  location: variable set of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:153: error: cannot find symbol
                assertTrue(set.containsRR(mx));
                              ^
  symbol:   method containsRR(Record)
  location: variable set of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:288: error: cannot find symbol
        assertTrue(aSet.containsRR(a1) && aSet.containsRR(a2));
                       ^
  symbol:   method containsRR(Record)
  location: variable aSet of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:288: error: cannot find symbol
        assertTrue(aSet.containsRR(a1) && aSet.containsRR(a2));
                                              ^
  symbol:   method containsRR(Record)
  location: variable aSet of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:292: error: cannot find symbol
        assertTrue(txtSet.containsRR(txt));
                         ^
  symbol:   method containsRR(Record)
  location: variable txtSet of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:315: error: cannot find symbol
        assertTrue(aSet.containsRR(a1) && aSet.containsRR(a2) && aSet.containsRR(a3));
                       ^
  symbol:   method containsRR(Record)
  location: variable aSet of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:315: error: cannot find symbol
        assertTrue(aSet.containsRR(a1) && aSet.containsRR(a2) && aSet.containsRR(a3));
                                              ^
  symbol:   method containsRR(Record)
  location: variable aSet of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:315: error: cannot find symbol
        assertTrue(aSet.containsRR(a1) && aSet.containsRR(a2) && aSet.containsRR(a3));
                                                                     ^
  symbol:   method containsRR(Record)
  location: variable aSet of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:318: error: cannot find symbol
        assertTrue(txtSet.containsRR(txt));
                         ^
  symbol:   method containsRR(Record)
  location: variable txtSet of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:347: error: cannot find symbol
        assertTrue(first.containsRR(r1) && first.containsRR(r2));
                        ^
  symbol:   method containsRR(Record)
  location: variable first of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:347: error: cannot find symbol
        assertTrue(first.containsRR(r1) && first.containsRR(r2));
                                                ^
  symbol:   method containsRR(Record)
  location: variable first of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:352: error: cannot find symbol
        assertTrue(second.containsRR(r3) && second.containsRR(r4));
                         ^
  symbol:   method containsRR(Record)
  location: variable second of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:352: error: cannot find symbol
        assertTrue(second.containsRR(r3) && second.containsRR(r4));
                                                  ^
  symbol:   method containsRR(Record)
  location: variable second of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:404: error: cannot find symbol
        assertTrue(set.containsRR(a1) && set.containsRR(a2) && set.containsRR(a3));
                      ^
  symbol:   method containsRR(Record)
  location: variable set of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:404: error: cannot find symbol
        assertTrue(set.containsRR(a1) && set.containsRR(a2) && set.containsRR(a3));
                                            ^
  symbol:   method containsRR(Record)
  location: variable set of type RRset
src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java:404: error: cannot find symbol
        assertTrue(set.containsRR(a1) && set.containsRR(a2) && set.containsRR(a3));
                                                                  ^
  symbol:   method containsRR(Record)
  location: variable set of type RRset
Note: src/test/java/org/xbill/DNS/Message_getSectionRRsets_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
21 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method getSectionRRsets(int)': '\n   * Returns an array containing all records in the given section grouped into RRsets.\n   *\n   * @see RRset\n   * @see Section\n   ', 'dependent classes': 'class `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\tjava.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;\n\trelated functions:\n\t\tmethod `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`;\n\t\tmethod `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: \n   * Determines if an RRset with the given name and type is already present in the given section.\n   *\n   * @see RRset\n   * @see Section\n   ;\n\t\tmethod `public boolean findRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: \n   * Determines if the given record is already present in the given section.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public List<Record> getSection(int)` returns `java.util.List<org.xbill.DNS.Record>`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: \n   * Returns all records in the given section, or an empty list if the section is empty.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public void removeAllRecords(int)` returns `void`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: \n   * Removes all records from a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: \n   * Adds a record to a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`, api document: \n   * Removes a record from a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\nclass `org.xbill.DNS.ZoneTransferIn.BasicHandler`:\n\trelated functions:\n\t\tmethod `public void startIXFRAdds(Record)` returns `void`, related with `org.xbill.DNS.Message.getSectionRRsets(int)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


