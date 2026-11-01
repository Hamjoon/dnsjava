


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
import java.util.Date;

@ExtendWith(MockitoExtension.class)
class Zone_removeRecord_Test {

    /* --------------------------------------------------------------------- *
   *  Shared test data – created before each test method                     *
   * --------------------------------------------------------------------- */
    private Name ZONE_NAME;

    private SOARecord SOA1;

    private NSRecord NS1;

    private NSRecord NS2;

    private ARecord A_UNIQUE;

    private ARecord A_TEST;

    private AAAARecord AAAA_1_TEST;

    private AAAARecord AAAA_2_TEST;

    private TXTRecord TXT_WILD;

    private Zone ZONE;

    private ARecord A1;

    private ARecord A2;

    private TXTRecord TXT_SINGLE;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // zone name: example.
        ZONE_NAME = Name.fromConstantString("example.");
        // mandatory SOA record
        SOA1 = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", ZONE_NAME), 1, 21600L, 7200L, 2160000L, 3600L);
        // two NS records – required by the Zone contract
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // a few address records
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");
        A_UNIQUE = new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
        TXT_WILD = new TXTRecord(new Name("wild", ZONE_NAME), DClass.IN, 3600, "some text");
        // Build the zone with a typical set of records
        Record[] records = { SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, TXT_WILD };
        ZONE = new Zone(ZONE_NAME, records);
        // Re‑initialize data for the second zone used in many tests
        // zone name: example.
        ZONE_NAME = Name.fromConstantString("example.");
        // mandatory SOA record
        SOA1 = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", ZONE_NAME), 1, 21600L, 7200L, 2160000L, 3600L);
        // two NS records – required by the Zone contract
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // two A records with the SAME name – they will share one RRset
        A1 = new ARecord(new Name("multitest", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        A2 = new ARecord(new Name("multitest", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        // a single TXT record (used for the “remove RRset when single” test)
        TXT_SINGLE = new TXTRecord(new Name("lonely", ZONE_NAME), DClass.IN, 3600, "only");
        // Build a zone that contains the mandatory records plus the ones we need for the tests.
        Record[] records2 = { SOA1, NS1, NS2, A1, A2, TXT_SINGLE };
        zone = new Zone(ZONE_NAME, records2);
    }

    @Test
    void removeRecord_nullArgument_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ZONE.removeRecord(null));
    }

    @Test
    void removeRecord_SOArecord_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ZONE.removeRecord(SOA1));
    }

    @Test
    void removeRecord_lastNS_throwsIllegalArgumentException() throws IOException {
        // Build a zone that contains exactly one NS (plus the mandatory SOA).
        Record[] minimal = { SOA1, NS1 };
        Zone singleNsZone = new Zone(ZONE_NAME, minimal);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> singleNsZone.removeRecord(NS1));
        assertTrue(ex.getMessage().contains("Cannot remove all NS"));
    }

    @Test
    void removeRecord_recordInMultiRRset_deletesOnlyThatRecord() {
        // Both A_UNIQUE and A_TEST share the same type (A) but have different names.
        // Removing A_UNIQUE must not affect A_TEST.
        assertNotNull(ZONE.findExactMatch(A_UNIQUE.getName(), Type.A));
        assertNotNull(ZONE.findExactMatch(A_TEST.getName(), Type.A));
        assertDoesNotThrow(() -> ZONE.removeRecord(A_UNIQUE));
        assertNull(ZONE.findExactMatch(A_UNIQUE.getName(), Type.A));
        assertNotNull(ZONE.findExactMatch(A_TEST.getName(), Type.A));
    }

    @Test
    void removeRecord_soleRecordRemovesRRset() {
        // TXT_WILD is the only TXT record for its name.
        assertNotNull(ZONE.findExactMatch(TXT_WILD.getName(), Type.TXT));
        assertDoesNotThrow(() -> ZONE.removeRecord(TXT_WILD));
        // After removal the exact match must be null.
        assertNull(ZONE.findExactMatch(TXT_WILD.getName(), Type.TXT));
    }

    @Test
    void removeRecord_nonExistentRecord_isNoOp() throws IOException {
        // Create a record that is *not* present in the zone.
        ARecord notInZone = new ARecord(new Name("doesnotexist", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        // Ensure it truly does not exist before the call.
        assertNull(ZONE.findExactMatch(notInZone.getName(), Type.A));
        // The call must not throw and must leave the zone unchanged.
        assertDoesNotThrow(() -> ZONE.removeRecord(notInZone));
        // Verify that an existing record (A_TEST) is still present.
        assertNotNull(ZONE.findExactMatch(A_TEST.getName(), Type.A));
    }

    /* --------------------------------------------------------------------- *
   *  Test case: No RRset found (record does not exist in the zone)        *
   * --------------------------------------------------------------------- */
    @Test
    void removeRecord_noRRsetFound_isNoOp() throws Exception {
        // Record with a name that is not present in the zone.
        Record missing = new ARecord(new Name("doesnotexist", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        assertNull(zone.findExactMatch(missing.getName(), Type.A));
        assertDoesNotThrow(() -> zone.removeRecord(missing));
        // Ensure the zone is unchanged – a known record is still present.
        assertNotNull(zone.findExactMatch(A1.getName(), Type.A));
    }

    /* --------------------------------------------------------------------- *
   *  Test case: Delete RR when the RRset contains multiple records        *
   * --------------------------------------------------------------------- */
    @Test
    void removeRecord_deleteRRWhenMultiple_keepsRRset() {
        // Pre‑condition: both A records are present.
        assertNotNull(zone.findExactMatch(A1.getName(), Type.A));
        assertNotNull(zone.findExactMatch(A2.getName(), Type.A));
        // Remove the first A record.
        assertDoesNotThrow(() -> zone.removeRecord(A1));
        // The second record must still be present, and the RRset must still exist.
        assertNull(zone.findExactMatch(A1.getName(), Type.A));
        assertNotNull(zone.findExactMatch(A2.getName(), Type.A));
    }

    /* --------------------------------------------------------------------- *
   *  Test case: Remove RRset when it is the only record in the set        *
   * --------------------------------------------------------------------- */
    @Test
    void removeRecord_removeRRsetWhenSingle_deletesRRset() {
        // Verify the TXT record exists.
        assertNotNull(zone.findExactMatch(TXT_SINGLE.getName(), Type.TXT));
        // Remove it – it is the only record of its type/name.
        assertDoesNotThrow(() -> zone.removeRecord(TXT_SINGLE));
        // After removal the exact match must be null.
        assertNull(zone.findExactMatch(TXT_SINGLE.getName(), Type.TXT));
    }

    /* --------------------------------------------------------------------- *
   *  Test case: Removing one NS from a multi‑NS RRset                      *
   * --------------------------------------------------------------------- */
    @Test
    void removeRecord_NSMultipleRecords_deletesOnlyOneNS() {
        // Both NS records are present at the zone apex.
        assertNotNull(zone.findExactMatch(NS1.getName(), Type.NS));
        assertNotNull(zone.findExactMatch(NS2.getName(), Type.NS));
        // Remove NS1 – should succeed because another NS remains.
        assertDoesNotThrow(() -> zone.removeRecord(NS1));
        // NS1 must be gone, NS2 must still be present.
        assertNull(zone.findExactMatch(NS1.getName(), Type.NS));
        assertNotNull(zone.findExactMatch(NS2.getName(), Type.NS));
    }

    /* --------------------------------------------------------------------- *
   *  Test case: Removing the sole NS when a signature is present           *
   * --------------------------------------------------------------------- */
//     @Test
//     void removeRecord_NSSingleWithSignature_throwsIllegalArgumentException() throws Exception {
//         // Build a zone that contains ONE NS record plus a signature (RRSIG) for it.
//         NSRecord soleNs = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns-only.example."));
//         // Create a minimal RRSIGRecord that signs the NS RRset.
//         // Parameters: name, dclass, ttl, typeCovered, algorithm, labelCount,
//         // originalTTL, expiration, inception, keyTag, signer, signature
//         RRSIGRecord nsSig = new RRSIGRecord(ZONE_NAME, DClass.IN, 300L, // typeCovered
//         Type.NS, // algorithm (dummy, e.g., RSASHA1)
//         5, // labelCount
//         1, // originalTTL
//         0L, // expiration (dummy)
//         new Date(), // inception (dummy)
//         new Date(), // keyTag
//         0, Name.fromConstantString("key.example."), // signature
//         new byte[] { 0 });
//         Record[] records = { SOA1, soleNs, nsSig };
//         Zone singleNsWithSigZone = new Zone(ZONE_NAME, records);
//         // Attempt to remove the only NS – the implementation checks only the size
//         // of the NS RRset (ignoring signatures) and therefore throws.
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> singleNsWithSigZone.removeRecord(soleNs));
//         assertTrue(ex.getMessage().contains("Cannot remove all NS"));
//     }

    /* --------------------------------------------------------------------- *
   *  Test case: Record whose getName() returns null                         *
   * --------------------------------------------------------------------- */
    @Test
    void removeRecord_nullRecordName_throwsNullPointerException() {
        // Create a mock Record that returns null for getName().
        Record mockRecord = mock(Record.class);
        when(mockRecord.getName()).thenReturn(null);
        when(mockRecord.getType()).thenReturn(Type.A);
        when(mockRecord.getRRsetType()).thenReturn(Type.A);
        // The method will attempt to look up an RRset with a null name, which
        // results in a NullPointerException from the underlying implementation.
        assertThrows(NullPointerException.class, () -> zone.removeRecord(mockRecord));
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
│  └─ Zone_removeRecord_Test ✔
│     ├─ removeRecord_nonExistentRecord_isNoOp() ✔
│     ├─ removeRecord_lastNS_throwsIllegalArgumentException() ✔
│     ├─ removeRecord_removeRRsetWhenSingle_deletesRRset() ✔
│     ├─ removeRecord_SOArecord_throwsIllegalArgumentException() ✔
│     ├─ removeRecord_soleRecordRemovesRRset() ✔
│     ├─ removeRecord_noRRsetFound_isNoOp() ✔
│     ├─ removeRecord_deleteRRWhenMultiple_keepsRRset() ✘ expected: <null> but was: <{ multitest.example. 3600 IN A [10.0.0.2] }>
│     ├─ removeRecord_recordInMultiRRset_deletesOnlyThatRecord() ✔
│     ├─ removeRecord_nullArgument_throwsIllegalArgumentException() ✔
│     ├─ removeRecord_nullRecordName_throwsNullPointerException() ✔
│     └─ removeRecord_NSMultipleRecords_deletesOnlyOneNS() ✘ expected: <null> but was: <{ example. 300 IN NS [ns2.example.] }>
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:Zone_removeRecord_Test:removeRecord_deleteRRWhenMultiple_keepsRRset()
    MethodSource [className = 'org.xbill.DNS.Zone_removeRecord_Test', methodName = 'removeRecord_deleteRRWhenMultiple_keepsRRset', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: <null> but was: <{ multitest.example. 3600 IN A [10.0.0.2] }>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertNull.failNotNull(AssertNull.java:50)
       org.junit.jupiter.api.AssertNull.assertNull(AssertNull.java:35)
       org.junit.jupiter.api.AssertNull.assertNull(AssertNull.java:30)
       org.junit.jupiter.api.Assertions.assertNull(Assertions.java:279)
       org.xbill.DNS.Zone_removeRecord_Test.removeRecord_deleteRRWhenMultiple_keepsRRset(Zone_removeRecord_Test.java:160)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:Zone_removeRecord_Test:removeRecord_NSMultipleRecords_deletesOnlyOneNS()
    MethodSource [className = 'org.xbill.DNS.Zone_removeRecord_Test', methodName = 'removeRecord_NSMultipleRecords_deletesOnlyOneNS', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: <null> but was: <{ example. 300 IN NS [ns2.example.] }>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertNull.failNotNull(AssertNull.java:50)
       org.junit.jupiter.api.AssertNull.assertNull(AssertNull.java:35)
       org.junit.jupiter.api.AssertNull.assertNull(AssertNull.java:30)
       org.junit.jupiter.api.Assertions.assertNull(Assertions.java:279)
       org.xbill.DNS.Zone_removeRecord_Test.removeRecord_NSMultipleRecords_deletesOnlyOneNS(Zone_removeRecord_Test.java:188)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]

Test run finished after 506 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        11 tests found           ]
[         0 tests skipped         ]
[        11 tests started         ]
[         0 tests aborted         ]
[         9 tests successful      ]
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
 

api document of method removeRecord(Record): 

   * Removes a record from the zone.
   *
   * @param r The record to remove.
   * @throws IllegalArgumentException if {@code r} is {@code null}, if the record to remove is the
   *     {@link SOARecord} or the last {@link NSRecord}.
   

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Type`:
	dependent fields:
		int org.xbill.DNS.Type.SOA;
		int org.xbill.DNS.Type.NS;
	
class `org.xbill.DNS.Zone`:
	related functions:
		method `public void addRecord(T)` returns `void`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
   * Adds a record to the zone. If there is an existing {@link RRset} of the same {@link Name} and
   * {@link Type}, the record is <b>added</b> to this set.
   *
   * @param r The record to add.
   * @throws IllegalArgumentException if {@code name} is {@code null} or if the rrset name does not
   *     match the zone origin.
   ;
		method `public void addRRset(RRset)` returns `void`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
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
		method `private void removeRRsetWithoutLock(Name, int)` returns `void`, related with `org.xbill.DNS.Zone.removeRecord(Record)`;
		method `public void removeRRset(Name, int)` returns `void`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
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
		method `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.removeRecord(Record)`;
		method `private void validate()` returns `void`, related with `org.xbill.DNS.Zone.removeRecord(Record)`;
class `org.xbill.DNS.dnssec.ValUtils`:
	related functions:
		method `public static boolean nsecProvesNameError(SRRset, NSECRecord, Name)` returns `boolean`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
   * Determine if the given NSEC proves a NameError (NXDOMAIN) for a given qname.
   *
   * @param set The RRset that contains the NSEC.
   * @param nsec The NSEC to check.
   * @param qname The qname to check against.
   * @return true if the NSEC proves the condition.
   ;
		method `public static NsecProvesNodataResponse nsecProvesNodata(SRRset, NSECRecord, Name, int)` returns `org.xbill.DNS.dnssec.ValUtils.NsecProvesNodataResponse`, related with `org.xbill.DNS.Zone.removeRecord(Record)`, api document: 
   * Determine if a NSEC proves the NOERROR/NODATA conditions. This will also handle the empty
   * non-terminal (ENT) case and partially handle the wildcard case. If the ownername of 'nsec' is a
   * wildcard, the validator must still be provided proof that qname did not directly exist and that
   * the wildcard is, in fact, *.closest_encloser.
   *
   * @param set The RRset of the NSEC record.
   * @param nsec The NSEC to check
   * @param qname The query name to check against.
   * @param qtype The query type to check against.
   * @return true if the NSEC proves the condition.
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
