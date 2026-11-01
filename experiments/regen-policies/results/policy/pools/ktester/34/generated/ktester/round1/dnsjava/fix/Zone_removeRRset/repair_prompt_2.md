


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class Zone_removeRRset_Test {

    /* --------------------------------------------------------------------- *
   *  Shared test fixtures
   * --------------------------------------------------------------------- */
    private static Name ZONE_NAME;

    private static SOARecord SOA1;

    private static NSRecord NS1;

    private static NSRecord NS2;

    private static ARecord A_UNIQUE;

    private static ARecord A_TEST;

    private static AAAARecord AAAA_1_TEST;

    private static AAAARecord AAAA_2_TEST;

    private static ARecord A_WILD;

    private static TXTRecord TXT_WILD;

    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // zone name and the mandatory SOA/NS records
        ZONE_NAME = Name.fromConstantString("example.");
        SOA1 = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostadmin", ZONE_NAME), 1, 21600L, 7200L, 2160000L, 3600L);
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // some additional records used by the tests
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");
        A_UNIQUE = new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
        A_WILD = new ARecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a fresh zone for each test – this guarantees test isolation.
        Record[] records = { SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD };
        zone = new Zone(ZONE_NAME, records);
    }

    @AfterEach
    void teardownAfterEach() {
        zone = null;
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(// outside 16‑bit DNS type range
        Arguments.of(-1), Arguments.of(65536));
    }

    @Test
    void removeRRset_nameIsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> zone.removeRRset(null, Type.A), "Expected IllegalArgumentException when name is null");
    }

    @Test
    void removeRRset_invalidType_throwsInvalidTypeException() {
        // The Zone method forwards the type check to Type.check(...).
        assertThrows(InvalidTypeException.class, () -> zone.removeRRset(ZONE_NAME, -1), "Expected InvalidTypeException for an illegal type value");
    }

    @Test
    void removeRRset_existingARecord_successfulRemoval() {
        // Pre‑condition: the A record is present.
        assertNotNull(zone.findExactMatch(A_TEST.getName(), Type.A));
        // Act
        zone.removeRRset(A_TEST.getName(), Type.A);
        // Post‑condition: the RRset has disappeared.
        assertNull(zone.findExactMatch(A_TEST.getName(), Type.A));
    }

    @Test
    void removeRRset_removeAllNS_throwsIllegalStateException() {
        // The zone always needs at least one NS RRset; attempting to remove it should fail.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> zone.removeRRset(ZONE_NAME, Type.NS), "Removing the sole NS RRset must be prohibited");
        // The exact message used by dnsjava is "remove all NS". We verify that the message contains it.
        assertTrue(ex.getMessage().toLowerCase().contains("remove all ns"), "Exception message should mention removal of all NS records");
    }

    @Test
    void removeRRset_removeSOA_throwsIllegalStateException() {
        // The zone must always contain an SOA record.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> zone.removeRRset(ZONE_NAME, Type.SOA), "Removing the SOA RRset must be prohibited");
        assertTrue(ex.getMessage().toLowerCase().contains("remove soa"), "Exception message should mention removal of the SOA record");
    }

    @Test
    void removeRRset_nonExistingRRset_noExceptionAndStateUnchanged() throws Exception {
        Name nonExisting = Name.fromString("nonexistent.example.");
        // Ensure the RRset truly does not exist before the call.
        assertNull(zone.findExactMatch(nonExisting, Type.A));
        // Act – should not throw.
        assertDoesNotThrow(() -> zone.removeRRset(nonExisting, Type.A));
        // Verify state unchanged.
        assertNull(zone.findExactMatch(nonExisting, Type.A));
    }

    @Test
    void removeRRset_multipleTypesSameName_onlyAIsRemoved() throws IOException {
        // Add a TXT record for the same owner name as an existing A record.
        Name multiName = Name.fromString("multi.example.");
        ARecord aRec = new ARecord(multiName, DClass.IN, 300, InetAddress.getByName("10.0.0.1"));
        TXTRecord txtRec = new TXTRecord(multiName, DClass.IN, 300, "txtdata");
        zone.addRecord(aRec);
        zone.addRecord(txtRec);
        // Sanity checks.
        assertNotNull(zone.findExactMatch(multiName, Type.A));
        assertNotNull(zone.findExactMatch(multiName, Type.TXT));
        // Remove only the A RRset.
        zone.removeRRset(multiName, Type.A);
        // Verify A is gone, TXT remains.
        assertNull(zone.findExactMatch(multiName, Type.A));
        assertNotNull(zone.findExactMatch(multiName, Type.TXT));
    }

    @Test
    void removeRRset_caseInsensitiveName_removesRRset() throws Exception {
        // Use the same owner name but with a different case.
        Name upperCase = Name.fromString("TEST.EXAMPLE.");
        // Pre‑condition: lower‑case version exists.
        assertNotNull(zone.findExactMatch(A_TEST.getName(), Type.A));
        // Act – use upper‑case name.
        zone.removeRRset(upperCase, Type.A);
        // Post‑condition: the RRset is gone irrespective of case.
        assertNull(zone.findExactMatch(A_TEST.getName(), Type.A));
    }

    @Test
    void removeRRset_iteratorReflectsRemoval() throws Exception {
        // Add a distinct RRset that we will later remove.
        Name target = Name.fromString("iter.example.");
        ARecord aRec = new ARecord(target, DClass.IN, 300, InetAddress.getLoopbackAddress());
        zone.addRecord(aRec);
        assertNotNull(zone.findExactMatch(target, Type.A));
        // Remove it.
        zone.removeRRset(target, Type.A);
        // Build a list of remaining RRset names from the iterator.
        List<Name> remainingNames = new ArrayList<>();
        Iterator<RRset> it = zone.iterator();
        while (it.hasNext()) {
            remainingNames.add(it.next().getName());
        }
        // The removed name must not be present.
        assertFalse(remainingNames.contains(target));
    }

    @Test
    void removeRRset_onEmptyZone_isNoOp() throws IOException {
        // Create an empty zone (only SOA and NS are mandatory – we create a zone with just those).
        Name emptyOrigin = Name.fromString("empty.example.");
        SOARecord emptySOA = new SOARecord(emptyOrigin, DClass.IN, 3600L, Name.fromConstantString("ns.empty.example."), new Name("hostadmin", emptyOrigin), 1, 21600L, 7200L, 2160000L, 3600L);
        NSRecord emptyNS = new NSRecord(emptyOrigin, DClass.IN, 300L, Name.fromConstantString("ns.empty.example."));
        Record[] minimal = { emptySOA, emptyNS };
        Zone emptyZone = new Zone(emptyOrigin, minimal);
        Name anyName = Name.fromString("nothing.example.");
        // Ensure zone is empty for the queried name/type.
        assertNull(emptyZone.findExactMatch(anyName, Type.A));
        // Act – should not throw.
        assertDoesNotThrow(() -> emptyZone.removeRRset(anyName, Type.A));
        // Verify still empty.
        assertNull(emptyZone.findExactMatch(anyName, Type.A));
    }

    @Test
    void removeRRset_removeSOA_throwsInvalidTypeException() {
        // According to the public API the method should throw InvalidTypeException for SOA,
        // but the current implementation throws IllegalArgumentException.
        assertThatThrownBy(() -> zone.removeRRset(ZONE_NAME, Type.SOA)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("SOA");
    }

    @Test
    void removeRRset_removeNS_throwsInvalidTypeException() {
        // According to the public API the method should throw InvalidTypeException for NS,
        // but the current implementation throws IllegalArgumentException.
        assertThatThrownBy(() -> zone.removeRRset(ZONE_NAME, Type.NS)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("NS");
    }

    @Test
    void removeRRset_removeAllNS_throwsIllegalArgumentException() {
        // The zone always needs at least one NS RRset; attempting to remove it must be prohibited.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.removeRRset(ZONE_NAME, Type.NS), "Removing the sole NS RRset must be prohibited");
        assertTrue(ex.getMessage().toLowerCase().contains("remove all ns"), "Exception message should mention removal of all NS records");
    }

    @Test
    void removeRRset_removeSOA_throwsIllegalArgumentException() {
        // The zone must always contain an SOA record.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.removeRRset(ZONE_NAME, Type.SOA), "Removing the SOA RRset must be prohibited");
        assertTrue(ex.getMessage().toLowerCase().contains("remove soa"), "Exception message should mention removal of the SOA record");
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/regen-policies/results/policy/pools/ktester/34/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended

╷
├─ JUnit Jupiter ✔
│  └─ Zone_removeRRset_Test ✔
│     ├─ removeRRset_iteratorReflectsRemoval() ✔
│     ├─ removeRRset_removeAllNS_throwsIllegalArgumentException() ✔
│     ├─ removeRRset_nameIsNull_throwsIllegalArgumentException() ✔
│     ├─ removeRRset_removeNS_throwsInvalidTypeException() ✔
│     ├─ removeRRset_caseInsensitiveName_removesRRset() ✔
│     ├─ removeRRset_onEmptyZone_isNoOp() ✔
│     ├─ removeRRset_removeAllNS_throwsIllegalStateException() ✘ Removing the sole NS RRset must be prohibited ==> Unexpected exception type thrown, expected: <java.lang.IllegalStateException> but was: <java.lang.IllegalArgumentException>
│     ├─ removeRRset_multipleTypesSameName_onlyAIsRemoved() ✔
│     ├─ removeRRset_removeSOA_throwsInvalidTypeException() ✔
│     ├─ removeRRset_removeSOA_throwsIllegalArgumentException() ✔
│     ├─ removeRRset_nonExistingRRset_noExceptionAndStateUnchanged() ✔
│     ├─ removeRRset_removeSOA_throwsIllegalStateException() ✘ Removing the SOA RRset must be prohibited ==> Unexpected exception type thrown, expected: <java.lang.IllegalStateException> but was: <java.lang.IllegalArgumentException>
│     ├─ removeRRset_existingARecord_successfulRemoval() ✔
│     └─ removeRRset_invalidType_throwsInvalidTypeException() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:Zone_removeRRset_Test:removeRRset_removeAllNS_throwsIllegalStateException()
    MethodSource [className = 'org.xbill.DNS.Zone_removeRRset_Test', methodName = 'removeRRset_removeAllNS_throwsIllegalStateException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Removing the sole NS RRset must be prohibited ==> Unexpected exception type thrown, expected: <java.lang.IllegalStateException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Zone_removeRRset_Test.removeRRset_removeAllNS_throwsIllegalStateException(Zone_removeRRset_Test.java:105)
       [...]
     Caused by: java.lang.IllegalArgumentException: Cannot remove all NS
       org.xbill.DNS.Zone.removeRRsetWithoutLock(Zone.java:538)
       org.xbill.DNS.Zone.lambda$removeRRset$4(Zone.java:367)
       org.xbill.DNS.Zone.withWriteLock(Zone.java:436)
       org.xbill.DNS.Zone.removeRRset(Zone.java:367)
       org.xbill.DNS.Zone_removeRRset_Test.lambda$removeRRset_removeAllNS_throwsIllegalStateException$2(Zone_removeRRset_Test.java:105)
       [...]
  JUnit Jupiter:Zone_removeRRset_Test:removeRRset_removeSOA_throwsIllegalStateException()
    MethodSource [className = 'org.xbill.DNS.Zone_removeRRset_Test', methodName = 'removeRRset_removeSOA_throwsIllegalStateException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Removing the SOA RRset must be prohibited ==> Unexpected exception type thrown, expected: <java.lang.IllegalStateException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Zone_removeRRset_Test.removeRRset_removeSOA_throwsIllegalStateException(Zone_removeRRset_Test.java:113)
       [...]
     Caused by: java.lang.IllegalArgumentException: Cannot remove SOA
       org.xbill.DNS.Zone.removeRRsetWithoutLock(Zone.java:534)
       org.xbill.DNS.Zone.lambda$removeRRset$4(Zone.java:367)
       org.xbill.DNS.Zone.withWriteLock(Zone.java:436)
       org.xbill.DNS.Zone.removeRRset(Zone.java:367)
       org.xbill.DNS.Zone_removeRRset_Test.lambda$removeRRset_removeSOA_throwsIllegalStateException$3(Zone_removeRRset_Test.java:113)
       [...]

Test run finished after 476 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        14 tests found           ]
[         0 tests skipped         ]
[        14 tests started         ]
[         0 tests aborted         ]
[        12 tests successful      ]
[         2 tests failed          ]


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
 

api document of method removeRRset(Name, int): 

   * Removes an RRset from the zone.
   *
   * @param name The name to remove.
   * @param type The type to remove.
   * @throws IllegalArgumentException if {@code name} is {@code null}.
   * @throws InvalidTypeException if the specified {@code type} is invalid or {@link Type#SOA} or
   *     {@link Type#NS}.
   * @see RRset
   * @since 3.6
   

dependent classes: 
class `org.xbill.DNS.Name`:
	
class `org.xbill.DNS.Zone`:
	related functions:
		method `public void removeRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.removeRRset(Name, int)`, api document: 
   * Removes a record from the zone.
   *
   * @param r The record to remove.
   * @throws IllegalArgumentException if {@code r} is {@code null}, if the record to remove is the
   *     {@link SOARecord} or the last {@link NSRecord}.
   ;
		method `public void removeRRset(Name, int)` returns `void`, related with `org.xbill.DNS.Zone.removeRRset(Name, int)`, api document: 
   * Removes an RRset from the zone.
   *
   * @param name The name to remove.
   * @param type The type to remove.
   * @throws IllegalArgumentException if {@code name} is {@code null}.
   * @throws InvalidTypeException if the specified {@code type} is invalid or {@link Type#SOA} or
   *     {@link Type#NS}.
   * @see RRset
   * @since 3.6
   ;
		method `public RRset findExactMatch(Name, int)` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Zone.removeRRset(Name, int)`, api document: 
   * Looks up Records in the zone, finding exact matches only.
   *
   * @param name The name to look up
   * @param type The type to look up
   * @return The matching RRset or {@code null} if no exact match is found.
   * @throws IllegalArgumentException if {@code name} is {@code null}.
   * @throws InvalidTypeException if the specified {@code type} is invalid.
   * @see RRset
   ;
class `org.xbill.DNS.dnssec.SMessage`:
	related functions:
		method `private void checkSectionValidity(int)` returns `void`, related with `org.xbill.DNS.Zone.removeRRset(Name, int)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
