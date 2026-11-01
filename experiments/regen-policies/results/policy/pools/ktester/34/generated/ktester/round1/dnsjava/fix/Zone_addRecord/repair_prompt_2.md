


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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class Zone_addRecord_Test {

    private static final long TTL = 3600L;

    private Name zoneName;

    private SOARecord soa1;

    private SOARecord soa2;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aRecord;

    private ARecord aRecord2;

    private Zone zone;

    /**
     * Create a fresh {@link Zone} instance before each test.
     */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        // zone name: example.
        zoneName = Name.fromConstantString("example.");
        // First SOA (will be replaced in one test)
        soa1 = new SOARecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        // Second SOA (used to replace the first one)
        soa2 = new SOARecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns.example."), new Name("zoneadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        // Two NS records (required for a valid zone)
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // A couple of A records for later tests
        aRecord = new ARecord(new Name("www", zoneName), DClass.IN, TTL, InetAddress.getByName("127.0.0.1"));
        aRecord2 = new ARecord(new Name("www", zoneName), DClass.IN, TTL, InetAddress.getByName("127.0.0.2"));
        // Initialise the zone with a valid SOA and NS records.
        zone = new Zone(zoneName, soa1, ns1, ns2);
    }

    /**
     * Adding a {@code null} record must throw {@link IllegalArgumentException}.
     */
    @Test
    void addRecord_nullRecord_throws() {
        assertThrows(IllegalArgumentException.class, () -> zone.addRecord(null));
    }

    /**
     * Adding an SOA whose owner does not match the zone origin must fail.
     */
    @Test
    void addRecord_soaOwnerMismatch_throws() throws IOException {
        // Create an SOA whose name is *not* the zone origin.
        Name otherName = Name.fromConstantString("otherzone.");
        SOARecord mismatchedSOA = new SOARecord(otherName, DClass.IN, TTL, Name.fromConstantString("ns.otherzone."), new Name("hostadmin", otherName), 1, 21600L, 7200L, 2160000L, 3600L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRecord(mismatchedSOA));
        assertTrue(ex.getMessage().contains("SOA owner"));
    }

    /**
     * Adding a record whose name is not a sub‑domain of the zone origin must fail.
     */
    @Test
    void addRecord_nameOutsideZone_throws() throws IOException {
        Name outside = Name.fromConstantString("outside.com.");
        ARecord outsideRecord = new ARecord(outside, DClass.IN, TTL, InetAddress.getByName("127.0.0.9"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRecord(outsideRecord));
        assertTrue(ex.getMessage().contains("is absolute and not a subdomain"));
    }

    /**
     * Adding a record for a name/type that does not yet exist creates a new RRset.
     */
//     @Test
//     void addRecord_newRRset_created() {
//         // The zone currently has no A records.
//         assertNull(zone.findExactMatch(aRecord.getName(), Type.A));
//         zone.addRecord(aRecord);
//         RRset rrset = zone.findExactMatch(aRecord.getName(), Type.A);
//         assertNotNull(rrset);
//         assertEquals(1, rrset.size());
//         // RRset does not expose a containsRR method in this version of dnsjava,
//         // so we check the underlying record array.
//         assertTrue(Arrays.asList(rrset.getRecords()).contains(aRecord));
//     }

    /**
     * Adding a record that matches an existing RRset appends to that RRset.
     */
//     @Test
//     void addRecord_existingRRset_appends() {
//         // First add one A record.
//         zone.addRecord(aRecord);
//         // Then add a second A record with the same name/type.
//         zone.addRecord(aRecord2);
//         RRset rrset = zone.findExactMatch(aRecord.getName(), Type.A);
//         assertNotNull(rrset);
//         assertEquals(2, rrset.size());
//         assertTrue(Arrays.asList(rrset.getRecords()).contains(aRecord));
//         assertTrue(Arrays.asList(rrset.getRecords()).contains(aRecord2));
//     }

    /**
     * Adding a new SOA replaces the existing zone SOA.
     */
    @Test
    void addRecord_replaceSOA_success() {
        // Verify initial SOA.
        assertEquals(soa1, zone.getSOA());
        // Add a new SOA (same owner = zone origin).
        zone.addRecord(soa2);
        // The zone's SOA reference must now point to the new object.
        assertEquals(soa2, zone.getSOA());
        // The old SOA must no longer be present in the zone.
        RRset oldSOARRset = zone.findExactMatch(soa1.getName(), Type.SOA);
        assertNull(oldSOARRset);
    }

    /**
     * Adding a record to a {@link Zone} whose {@code origin} field is {@code null}
     * should result in a {@link NullPointerException} when {@code name.subdomain(origin)}
     * is evaluated.
     */
    @Test
    void addRecord_nullZoneOrigin_throwsNPE() throws Exception {
        // Construct a normal zone first.
        Zone zoneWithNullOrigin = new Zone(zoneName, soa1, ns1, ns2);
        // Use reflection to set the private 'origin' field to null.
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        originField.set(zoneWithNullOrigin, null);
        // Attempt to add a valid A record – we expect NPE.
        assertThrows(NullPointerException.class, () -> zoneWithNullOrigin.addRecord(aRecord));
    }

    /**
     * Adding a {@link Record} whose {@code getName()} returns {@code null}
     * must cause a {@link NullPointerException} because the method dereferences the name.
     */
    @Test
    void addRecord_recordWithNullName_throwsNPE() {
        // Mock a Record that returns null for getName().
        Record mockRecord = mock(Record.class);
        when(mockRecord.getName()).thenReturn(null);
        when(mockRecord.getRRsetType()).thenReturn(Type.A);
        // The other methods are not used by addRecord, but we stub them to avoid NPEs
        // if the implementation accesses them later.
        when(mockRecord.getTTL()).thenReturn(TTL);
        when(mockRecord.getDClass()).thenReturn(DClass.IN);
        when(mockRecord.getType()).thenReturn(Type.A);
        assertThrows(NullPointerException.class, () -> zone.addRecord(mockRecord));
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
│  └─ Zone_addRecord_Test ✔
│     ├─ addRecord_recordWithNullName_throwsNPE() ✘ 
│     │        Unnecessary stubbings detected.
│     │        Clean & maintainable test code requires zero unnecessary code.
│     │        Following stubbings are unnecessary (click to navigate to relevant line of code):
│     │          1. -> at org.xbill.DNS.Zone_addRecord_Test.addRecord_recordWithNullName_throwsNPE(Zone_addRecord_Test.java:163)
│     │          2. -> at org.xbill.DNS.Zone_addRecord_Test.addRecord_recordWithNullName_throwsNPE(Zone_addRecord_Test.java:164)
│     │          3. -> at org.xbill.DNS.Zone_addRecord_Test.addRecord_recordWithNullName_throwsNPE(Zone_addRecord_Test.java:165)
│     │        Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
│     ├─ addRecord_nameOutsideZone_throws() ✔
│     ├─ addRecord_replaceSOA_success() ✘ expected: <null> but was: <{ example. 3600 IN SOA [ns.example. zoneadmin.example. 1 21600 7200 2160000 3600] }>
│     ├─ addRecord_nullRecord_throws() ✔
│     ├─ addRecord_soaOwnerMismatch_throws() ✔
│     └─ addRecord_nullZoneOrigin_throwsNPE() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:Zone_addRecord_Test:addRecord_recordWithNullName_throwsNPE()
    MethodSource [className = 'org.xbill.DNS.Zone_addRecord_Test', methodName = 'addRecord_recordWithNullName_throwsNPE', methodParameterTypes = '']
    => org.mockito.exceptions.misusing.UnnecessaryStubbingException: 
Unnecessary stubbings detected.
Clean & maintainable test code requires zero unnecessary code.
Following stubbings are unnecessary (click to navigate to relevant line of code):
  1. -> at org.xbill.DNS.Zone_addRecord_Test.addRecord_recordWithNullName_throwsNPE(Zone_addRecord_Test.java:163)
  2. -> at org.xbill.DNS.Zone_addRecord_Test.addRecord_recordWithNullName_throwsNPE(Zone_addRecord_Test.java:164)
  3. -> at org.xbill.DNS.Zone_addRecord_Test.addRecord_recordWithNullName_throwsNPE(Zone_addRecord_Test.java:165)
Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
       org.mockito.junit.jupiter.MockitoExtension.afterEach(MockitoExtension.java:197)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAfterEachCallbacks$12(TestMethodTestDescriptor.java:261)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAllAfterMethodsOrCallbacks$13(TestMethodTestDescriptor.java:277)
       org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeAllAfterMethodsOrCallbacks$14(TestMethodTestDescriptor.java:277)
       org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder(CollectionUtils.java:217)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeAllAfterMethodsOrCallbacks(TestMethodTestDescriptor.java:276)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeAfterEachCallbacks(TestMethodTestDescriptor.java:260)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:145)
       org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
       [...]
  JUnit Jupiter:Zone_addRecord_Test:addRecord_replaceSOA_success()
    MethodSource [className = 'org.xbill.DNS.Zone_addRecord_Test', methodName = 'addRecord_replaceSOA_success', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: <null> but was: <{ example. 3600 IN SOA [ns.example. zoneadmin.example. 1 21600 7200 2160000 3600] }>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertNull.failNotNull(AssertNull.java:50)
       org.junit.jupiter.api.AssertNull.assertNull(AssertNull.java:35)
       org.junit.jupiter.api.AssertNull.assertNull(AssertNull.java:30)
       org.junit.jupiter.api.Assertions.assertNull(Assertions.java:279)
       org.xbill.DNS.Zone_addRecord_Test.addRecord_replaceSOA_success(Zone_addRecord_Test.java:131)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]

Test run finished after 604 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         6 tests found           ]
[         0 tests skipped         ]
[         6 tests started         ]
[         0 tests aborted         ]
[         4 tests successful      ]
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
 

api document of method addRecord(T): 

   * Adds a record to the zone. If there is an existing {@link RRset} of the same {@link Name} and
   * {@link Type}, the record is <b>added</b> to this set.
   *
   * @param r The record to add.
   * @throws IllegalArgumentException if {@code name} is {@code null} or if the rrset name does not
   *     match the zone origin.
   

dependent classes: 
class `org.xbill.DNS.Type`:
	dependent fields:
		int org.xbill.DNS.Type.SOA;
	
class `org.xbill.DNS.Zone`:
	dependent fields:
		org.xbill.DNS.SOARecord org.xbill.DNS.Zone.SOA;
		org.xbill.DNS.Name org.xbill.DNS.Zone.origin;
	related functions:
		method `private void fromXFR(ZoneTransferIn)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`;
		method `private void removeRRsetWithoutLock(Name, int)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`;
		method `public void removeRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: 
   * Removes a record from the zone.
   *
   * @param r The record to remove.
   * @throws IllegalArgumentException if {@code r} is {@code null}, if the record to remove is the
   *     {@link SOARecord} or the last {@link NSRecord}.
   ;
		method `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`;
		method `public void addRRset(RRset)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: 
   * Adds an RRset to the zone.
   *
   * @implNote An existing {@link RRset} of the same {@link Name} and {@link Type} is
   *     <b>replaced</b>.
   * @param rrset The RRset to add.
   * @see RRset
   * @throws IllegalArgumentException if {@code rrset} is {@code null} or if the rrset name is not a
   *     {@link Name#subdomain(Name) subdomain} of the zone origin (or, in case of a SOA, is not
   *     equal to the zone origin).
   ;
		method `public SetResponse findRecords(Name, int)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: 
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
		method `private void validate()` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`;
class `org.xbill.DNS.dnssec.ValUtils`:
	related functions:
		method `public static boolean nsecProvesNameError(SRRset, NSECRecord, Name)` returns `boolean`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: 
   * Determine if the given NSEC proves a NameError (NXDOMAIN) for a given qname.
   *
   * @param set The RRset that contains the NSEC.
   * @param nsec The NSEC to check.
   * @param qname The qname to check against.
   * @return true if the NSEC proves the condition.
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
