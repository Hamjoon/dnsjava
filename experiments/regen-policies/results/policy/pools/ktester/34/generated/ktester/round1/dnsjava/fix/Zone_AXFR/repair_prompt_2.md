


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
import java.net.InetAddress;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentSkipListMap;

@ExtendWith(MockitoExtension.class)
class Zone_AXFR_Test {

    private Name ZONE_NAME;

    private SOARecord SOA1;

    private NSRecord NS1;

    private NSRecord NS2;

    private ARecord A_UNIQUE;

    private ARecord A_TEST;

    private AAAARecord AAAA_1_TEST;

    private AAAARecord AAAA_2_TEST;

    private ARecord A_WILD;

    private TXTRecord TXT_WILD;

    private Zone ZONE;

    private SOARecord SOA;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a simple zone that contains an SOA, two NS records and a few other records.
        ZONE_NAME = Name.fromConstantString("example.");
        SOA1 = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostadmin", ZONE_NAME), 1, 21600L, 7200L, 2160000L, 3600L);
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("nameserver1."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("nameserver2."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");
        A_UNIQUE = new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
        A_WILD = new ARecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");
        Record[] zoneRecords = new Record[] { SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD };
        ZONE = new Zone(ZONE_NAME, zoneRecords);
        // Minimal required records for a valid zone.
        ZONE_NAME = Name.fromConstantString("example.");
        SOA = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster", ZONE_NAME), 1, 7200L, 3600L, 1209600L, 3600L);
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // Start each test with a zone that only contains the mandatory SOA and two NS records.
        zone = new Zone(ZONE_NAME, new Record[] { SOA, NS1, NS2 });
    }

    /**
     * The AXFR iterator should return the same RRsets as the normal iterator (in the same order).
     */
    @Test
    void axfrIteratorReturnsAllRRsetsInOrder() {
        Iterator<RRset> axfrIter = ZONE.AXFR();
        assertNotNull(axfrIter, "AXFR iterator must not be null");
        // Collect the RRsets produced by AXFR.
        List<RRset> axfrList = new ArrayList<>();
        while (axfrIter.hasNext()) {
            axfrList.add(axfrIter.next());
        }
        // Collect the RRsets produced by the regular iterator for comparison.
        Iterator<RRset> normalIter = ZONE.iterator();
        List<RRset> normalList = new ArrayList<>();
        while (normalIter.hasNext()) {
            normalList.add(normalIter.next());
        }
        assertEquals(normalList, axfrList, "AXFR iterator must produce the same sequence of RRsets as the regular iterator");
    }

    /**
     * The AXFR iterator must not support element removal.
     */
    @Test
    void axfrIteratorRemoveIsUnsupported() {
        Iterator<RRset> axfrIter = ZONE.AXFR();
        assertTrue(axfrIter.hasNext(), "Iterator should have at least the SOA element");
        // consume first element (SOA)
        axfrIter.next();
        assertThrows(UnsupportedOperationException.class, axfrIter::remove, "Calling remove() on an AXFR iterator should throw UnsupportedOperationException");
    }

    /**
     * After the last element, calling next() must throw NoSuchElementException.
     */
    @Test
    void axfrIteratorNextAfterLastFails() {
        Iterator<RRset> axfrIter = ZONE.AXFR();
        while (axfrIter.hasNext()) {
            axfrIter.next();
        }
        assertThrows(NoSuchElementException.class, axfrIter::next, "Calling next() after the iterator is exhausted must throw NoSuchElementException");
    }

    /**
     * AXFR on an empty zone (no additional RRsets beyond SOA/NS).
     */
    @Test
    void axfrOnEmptyZone() {
        Iterator<RRset> it = zone.AXFR();
        // The iterator must contain the SOA at the start, the two NS RRsets, and the SOA again at
        // the end – total of 4 RRsets.
        List<RRset> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertEquals(4, collected.size(), "Empty zone should yield 4 RRsets (SOA, NS, NS, SOA)");
        assertEquals(SOA.getName(), collected.get(0).getName(), "First RRset must be SOA");
        assertEquals(SOA.getName(), collected.get(collected.size() - 1).getName(), "Last RRset must be SOA (AXFR duplicate)");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after iterator exhaustion must throw NoSuchElementException");
    }

    /**
     * AXFR with a single additional RRset (an A record).
     */
    @Test
    void axfrWithSingleRRset() throws IOException {
        // Add a single A record.
        ARecord aRecord = new ARecord(new Name("www", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        zone.addRecord(aRecord);
        Iterator<RRset> it = zone.AXFR();
        List<RRset> rrsets = new ArrayList<>();
        while (it.hasNext()) {
            rrsets.add(it.next());
        }
        // Expected order: SOA, NS1, NS2, A, SOA (duplicate at end) => 5 RRsets.
        assertEquals(5, rrsets.size(), "Zone with one extra RRset should yield 5 RRsets");
        assertEquals(SOA.getName(), rrsets.get(0).getName(), "First element must be SOA");
        assertEquals(aRecord.getName(), rrsets.get(3).getName(), "Fourth element must be the A record");
        assertEquals(SOA.getName(), rrsets.get(4).getName(), "Last element must be the closing SOA");
    }

    /**
     * AXFR with multiple RRsets and verification of lexical ordering by name and type.
     */
    @Test
    void axfrWithMultipleRRsetsOrdering() throws IOException {
        // Records with differing names and types.
        ARecord aB = new ARecord(new Name("b", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("192.0.2.2"));
        AAAARecord aAAA = new AAAARecord(new Name("a", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("2001:db8::1"));
        MXRecord mx = new MXRecord(new Name("a", ZONE_NAME), DClass.IN, 3600, 10, Name.fromConstantString("mail.example."));
        NSRecord nsExtra = new NSRecord(new Name("c", ZONE_NAME), DClass.IN, 3600, Name.fromConstantString("ns3.example."));
        // Add them in a random order to ensure iterator sorting is exercised.
        zone.addRecord(aB);
        zone.addRecord(mx);
        zone.addRecord(aAAA);
        zone.addRecord(nsExtra);
        Iterator<RRset> it = zone.AXFR();
        List<RRset> rrsets = new ArrayList<>();
        while (it.hasNext()) {
            rrsets.add(it.next());
        }
        // Expected order (lexicographic name, then numeric type):
        // 1. SOA (example.)
        // 2. NS1 (example.)
        // 3. NS2 (example.)
        // 4. NS extra (c.example.)
        // 5. MX a.example.
        // 6. AAAA a.example.
        // 7. A b.example.
        // 8. SOA again (closing)
        List<String> expected = List.of("SOA", "NS", "NS", "NS", "MX", "AAAA", "A", "SOA");
        List<String> actual = rrsets.stream().map(rr -> {
            int t = rr.getType();
            if (t == Type.SOA)
                return "SOA";
            if (t == Type.NS)
                return "NS";
            if (t == Type.MX)
                return "MX";
            if (t == Type.AAAA)
                return "AAAA";
            if (t == Type.A)
                return "A";
            return "OTHER";
        }).collect(Collectors.toList());
        assertEquals(expected, actual, "AXFR iterator must follow lexical name then type order");
    }

    /**
     * Two consecutive AXFR calls must return independent iterator instances.
     */
    @Test
    void axfrIteratorIndependence() {
        Iterator<RRset> it1 = zone.AXFR();
        Iterator<RRset> it2 = zone.AXFR();
        assertNotSame(it1, it2, "Two calls to AXFR must return distinct iterator objects");
        // Advance the first iterator once.
        assertTrue(it1.hasNext());
        RRset firstFromIt1 = it1.next();
        // The second iterator must still be at the beginning (its first element equals the first
        // element of it1 before we advanced it).
        assertTrue(it2.hasNext());
        RRset firstFromIt2 = it2.next();
        assertEquals(firstFromIt1, firstFromIt2, "Both iterators should start with the same first RRset");
    }

    /**
     * Verify runtime type of the iterator and that remove() is unsupported.
     */
    @Test
    void axfrIteratorTypeAndUnsupportedRemove() {
        Iterator<RRset> it = zone.AXFR();
        // The concrete class is a private inner class named ZoneIterator.
        String className = it.getClass().getSimpleName();
        assertTrue(className.contains("ZoneIterator"), "AXFR iterator should be an instance of Zone.ZoneIterator");
        assertThrows(UnsupportedOperationException.class, it::remove, "remove() on AXFR iterator must throw UnsupportedOperationException");
    }

    /**
     * ConcurrentModificationException should be thrown if the zone is modified after iterator
     * creation.
     */
    @Test
    void axfrIteratorDetectsConcurrentModification() throws IOException {
        Iterator<RRset> it = zone.AXFR();
        // Modify the zone after the iterator has been obtained.
        ARecord newRec = new ARecord(new Name("new", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("192.0.2.99"));
        zone.addRecord(newRec);
        // The next call to the iterator is expected to notice the structural change.
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw ConcurrentModificationException after zone modification");
    }

    /**
     * Inserting a null RRset into the internal storage should cause a NullPointerException during
     * iteration.
     */
//     @Test
//     void axfrIteratorThrowsNullPointerWhenNullRRsetPresent() throws Exception {
//         // Use reflection to obtain the internal map that stores RRsets.
//         Field mapField = Zone.class.getDeclaredField("rrsets");
//         mapField.setAccessible(true);
//         @SuppressWarnings("unchecked")
//         ConcurrentSkipListMap<Name, List<RRset>> map = (ConcurrentSkipListMap<Name, List<RRset>>) mapField.get(zone);
//         // Insert a null entry for a synthetic name.
//         Name nullName = Name.fromConstantString("null.example.");
//         // deliberately corrupt internal state
//         map.put(nullName, null);
//         Iterator<RRset> it = zone.AXFR();
//         // Traversal should encounter the null entry and raise NPE.
//         assertThrows(NullPointerException.class, it::next, "Iterating over a zone containing a null RRset entry must throw NullPointerException");
//     }

    /**
     * After the iterator has been exhausted, a further call to {@code next()} should raise an
     * {@link IllegalStateException}. (The implementation may also throw {@link
     * NoSuchElementException}; the test accepts either.)
     */
    @Test
    void axfrIteratorIllegalStateAfterExhaustion() {
        Iterator<RRset> it = zone.AXFR();
        // Exhaust the iterator.
        while (it.hasNext()) {
            it.next();
        }
        // The next call should indicate an illegal state.
        try {
            it.next();
            fail("Expected either IllegalStateException or NoSuchElementException after exhaustion");
        } catch (IllegalStateException | NoSuchElementException expected) {
            // Test passes.
        }
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/regen-policies/results/policy/pools/ktester/34/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]

╷
├─ JUnit Jupiter ✔
│  └─ Zone_AXFR_Test ✔
│     ├─ axfrIteratorIndependence() ✔
│     ├─ axfrIteratorTypeAndUnsupportedRemove() ✘ remove() on AXFR iterator must throw UnsupportedOperationException ==> Unexpected exception type thrown, expected: <java.lang.UnsupportedOperationException> but was: <java.lang.IllegalStateException>
│     ├─ axfrWithSingleRRset() ✘ Zone with one extra RRset should yield 5 RRsets ==> expected: <5> but was: <4>
│     ├─ axfrIteratorDetectsConcurrentModification() ✘ Iterator should throw ConcurrentModificationException after zone modification ==> Expected java.util.ConcurrentModificationException to be thrown, but nothing was thrown.
│     ├─ axfrIteratorRemoveIsUnsupported() ✘ Calling remove() on an AXFR iterator should throw UnsupportedOperationException ==> Unexpected exception type thrown, expected: <java.lang.UnsupportedOperationException> but was: <java.lang.IllegalArgumentException>
│     ├─ axfrIteratorNextAfterLastFails() ✔
│     ├─ axfrIteratorReturnsAllRRsetsInOrder() ✘ AXFR iterator must produce the same sequence of RRsets as the regular iterator ==> expected: <[{ example. 3600 IN SOA [nameserver. hostadmin.example. 1 21600 7200 2160000 3600] }, { example. 300 IN NS [nameserver1.] [nameserver2.] }, { test.example. 3600 IN A [127.0.0.1] }, { test.example. 3600 IN AAAA [0:0:0:0:0:0:0:1] [0:0:0:0:0:0:0:2] }, { unique.example. 3600 IN A [127.0.0.3] }, { *.wild.example. 3600 IN A [127.0.0.2] }, { *.wild.example. 3600 IN TXT ["sometext"] }]> but was: <[{ example. 3600 IN SOA [nameserver. hostadmin.example. 1 21600 7200 2160000 3600] }, { example. 300 IN NS [nameserver1.] [nameserver2.] }, { test.example. 3600 IN A [127.0.0.1] }, { test.example. 3600 IN AAAA [0:0:0:0:0:0:0:1] [0:0:0:0:0:0:0:2] }, { unique.example. 3600 IN A [127.0.0.3] }, { *.wild.example. 3600 IN A [127.0.0.2] }, { *.wild.example. 3600 IN TXT ["sometext"] }, { example. 3600 IN SOA [nameserver. hostadmin.example. 1 21600 7200 2160000 3600] }]>
│     ├─ axfrOnEmptyZone() ✘ Empty zone should yield 4 RRsets (SOA, NS, NS, SOA) ==> expected: <4> but was: <3>
│     ├─ axfrIteratorIllegalStateAfterExhaustion() ✔
│     └─ axfrWithMultipleRRsetsOrdering() ✘ AXFR iterator must follow lexical name then type order ==> expected: <[SOA, NS, NS, NS, MX, AAAA, A, SOA]> but was: <[SOA, NS, MX, AAAA, A, NS, SOA]>
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (7):
  JUnit Jupiter:Zone_AXFR_Test:axfrIteratorTypeAndUnsupportedRemove()
    MethodSource [className = 'org.xbill.DNS.Zone_AXFR_Test', methodName = 'axfrIteratorTypeAndUnsupportedRemove', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: remove() on AXFR iterator must throw UnsupportedOperationException ==> Unexpected exception type thrown, expected: <java.lang.UnsupportedOperationException> but was: <java.lang.IllegalStateException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Zone_AXFR_Test.axfrIteratorTypeAndUnsupportedRemove(Zone_AXFR_Test.java:227)
       [...]
     Caused by: java.lang.IllegalStateException: Not at an element
       org.xbill.DNS.Zone$ZoneIterator.remove(Zone.java:805)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
  JUnit Jupiter:Zone_AXFR_Test:axfrWithSingleRRset()
    MethodSource [className = 'org.xbill.DNS.Zone_AXFR_Test', methodName = 'axfrWithSingleRRset', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Zone with one extra RRset should yield 5 RRsets ==> expected: <5> but was: <4>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:563)
       org.xbill.DNS.Zone_AXFR_Test.axfrWithSingleRRset(Zone_AXFR_Test.java:147)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Zone_AXFR_Test:axfrIteratorDetectsConcurrentModification()
    MethodSource [className = 'org.xbill.DNS.Zone_AXFR_Test', methodName = 'axfrIteratorDetectsConcurrentModification', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Iterator should throw ConcurrentModificationException after zone modification ==> Expected java.util.ConcurrentModificationException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Zone_AXFR_Test.axfrIteratorDetectsConcurrentModification(Zone_AXFR_Test.java:241)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Zone_AXFR_Test:axfrIteratorRemoveIsUnsupported()
    MethodSource [className = 'org.xbill.DNS.Zone_AXFR_Test', methodName = 'axfrIteratorRemoveIsUnsupported', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Calling remove() on an AXFR iterator should throw UnsupportedOperationException ==> Unexpected exception type thrown, expected: <java.lang.UnsupportedOperationException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Zone_AXFR_Test.axfrIteratorRemoveIsUnsupported(Zone_AXFR_Test.java:100)
       [...]
     Caused by: java.lang.IllegalArgumentException: Cannot remove SOA
       org.xbill.DNS.Zone.removeRRsetWithoutLock(Zone.java:534)
       org.xbill.DNS.Zone.access$400(Zone.java:30)
       org.xbill.DNS.Zone$ZoneIterator.lambda$remove$2(Zone.java:808)
       org.xbill.DNS.Zone.withWriteLock(Zone.java:436)
       org.xbill.DNS.Zone.access$300(Zone.java:30)
       [...]
  JUnit Jupiter:Zone_AXFR_Test:axfrIteratorReturnsAllRRsetsInOrder()
    MethodSource [className = 'org.xbill.DNS.Zone_AXFR_Test', methodName = 'axfrIteratorReturnsAllRRsetsInOrder', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: AXFR iterator must produce the same sequence of RRsets as the regular iterator ==> expected: <[{ example. 3600 IN SOA [nameserver. hostadmin.example. 1 21600 7200 2160000 3600] }, { example. 300 IN NS [nameserver1.] [nameserver2.] }, { test.example. 3600 IN A [127.0.0.1] }, { test.example. 3600 IN AAAA [0:0:0:0:0:0:0:1] [0:0:0:0:0:0:0:2] }, { unique.example. 3600 IN A [127.0.0.3] }, { *.wild.example. 3600 IN A [127.0.0.2] }, { *.wild.example. 3600 IN TXT ["sometext"] }]> but was: <[{ example. 3600 IN SOA [nameserver. hostadmin.example. 1 21600 7200 2160000 3600] }, { example. 300 IN NS [nameserver1.] [nameserver2.] }, { test.example. 3600 IN A [127.0.0.1] }, { test.example. 3600 IN AAAA [0:0:0:0:0:0:0:1] [0:0:0:0:0:0:0:2] }, { unique.example. 3600 IN A [127.0.0.3] }, { *.wild.example. 3600 IN A [127.0.0.2] }, { *.wild.example. 3600 IN TXT ["sometext"] }, { example. 3600 IN SOA [nameserver. hostadmin.example. 1 21600 7200 2160000 3600] }]>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1156)
       org.xbill.DNS.Zone_AXFR_Test.axfrIteratorReturnsAllRRsetsInOrder(Zone_AXFR_Test.java:88)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Zone_AXFR_Test:axfrOnEmptyZone()
    MethodSource [className = 'org.xbill.DNS.Zone_AXFR_Test', methodName = 'axfrOnEmptyZone', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Empty zone should yield 4 RRsets (SOA, NS, NS, SOA) ==> expected: <4> but was: <3>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:563)
       org.xbill.DNS.Zone_AXFR_Test.axfrOnEmptyZone(Zone_AXFR_Test.java:127)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Zone_AXFR_Test:axfrWithMultipleRRsetsOrdering()
    MethodSource [className = 'org.xbill.DNS.Zone_AXFR_Test', methodName = 'axfrWithMultipleRRsetsOrdering', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: AXFR iterator must follow lexical name then type order ==> expected: <[SOA, NS, NS, NS, MX, AAAA, A, SOA]> but was: <[SOA, NS, MX, AAAA, A, NS, SOA]>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1156)
       org.xbill.DNS.Zone_AXFR_Test.axfrWithMultipleRRsetsOrdering(Zone_AXFR_Test.java:197)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 451 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        10 tests found           ]
[         0 tests skipped         ]
[        10 tests started         ]
[         0 tests aborted         ]
[         3 tests successful      ]
[         7 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class org.xbill.DNS.Zone: 

 * A DNS zone. This encapsulates all data related to a zone, and provides convenient lookup methods.
 * A zone always contains a {@link SOARecord} and at least one {@link NSRecord}.
 *
 * @implNote This class uses a {@link java.util.concurrent.locks.ReadWriteLock} to ensure access and
 *     manipulation is safe from multiple threads. Iterators are "weakly consistent" (see the
 *     package info from {@code java.util.concurrent}).
 * @author Brian Wellington
 

api document of method AXFR(): 

   * Returns an Iterator over the {@link RRset RRsets} in the zone that can be used to construct an
   * AXFR response. This is identical to {@link #iterator} except that the SOA is returned at the
   * end as well as the beginning.
   

dependent classes: 
class `org.xbill.DNS.Zone`:
	related functions:
		method `public Iterator<RRset> iterator()` returns `java.util.Iterator<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Zone.AXFR()`, api document:  Returns an iterator over the {@link RRset RRsets} in the zone. ;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Zone.AXFR()`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
