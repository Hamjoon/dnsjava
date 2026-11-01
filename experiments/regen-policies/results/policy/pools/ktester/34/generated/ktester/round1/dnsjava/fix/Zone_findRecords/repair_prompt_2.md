


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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class Zone_findRecords_Test {

    /**
     * Common objects used by all tests.
     */
    private Name zoneName;

    private SOARecord soa;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aUnique;

    private ARecord aTest;

    private AAAARecord aaaaTest;

    private ARecord aWild;

    private TXTRecord txtWild;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a minimal but realistic zone (the same data used in the original
        // ZoneTest class). All records are absolute and belong to the zone
        // "example.".
        zoneName = Name.fromConstantString("example.");
        soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
        aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6a);
        aWild = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        txtWild = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");
        Record[] records = { soa, ns1, ns2, aUnique, aTest, aaaaTest, aWild, txtWild };
        zone = new Zone(zoneName, records);
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> zone.findRecords(null, Type.A), "name must not be null");
    }

    @Test
    void findRecords_invalidType_throwsInvalidTypeException() {
        // an arbitrary type value that is not defined by the DNS protocol
        int invalidType = 9999;
        assertThrows(InvalidTypeException.class, () -> zone.findRecords(Name.fromConstantString("example.com."), invalidType));
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_nameNotInZone_returnsNxDomain() {
        Name external = Name.fromConstantString("outside.example.");
        SetResponse response = zone.findRecords(external, Type.A);
        assertNotNull(response);
        assertTrue(response.isNXDOMAIN(), "The response should indicate NXDOMAIN for a name outside the zone");
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_exactMatch_returnsSuccess() {
        SetResponse response = zone.findRecords(aTest.getName(), Type.A);
        assertNotNull(response);
        // In the current dnsjava API a successful lookup is indicated by
        // !isNXDOMAIN() and !isCNAME() etc.  The most direct way is to check that
        // the response is not NXDOMAIN.
        assertFalse(response.isNXDOMAIN(), "The response for an exact match should not be NXDOMAIN");
        // Retrieve the RRset directly from the zone and verify it contains the
        // expected record.
        RRset rrset = zone.findExactMatch(aTest.getName(), Type.A);
        assertNotNull(rrset);
        // RRset provides a list of records via the rrs() method.
        List<Record> rrRecords = rrset.rrs();
        assertTrue(rrRecords.contains(aTest), "The RRset returned by the zone should contain the queried A record");
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_wildcardMatch_returnsSuccess() throws IOException {
        Name query = Name.fromConstantString("foo.wild.example.");
        SetResponse response = zone.findRecords(query, Type.A);
        assertNotNull(response);
        assertFalse(response.isNXDOMAIN(), "Wildcard lookup should not result in NXDOMAIN");
        // The wildcard A record should be returned.
        RRset rrset = zone.findExactMatch(Name.fromConstantString("*.wild.example."), Type.A);
        assertNotNull(rrset);
        List<Record> rrRecords = rrset.rrs();
        assertTrue(rrRecords.contains(aWild), "The RRset must contain the wildcard A record");
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_zoneWithNullOrigin_throwsNullPointerException() throws Exception {
        // Use reflection to set the private 'origin' field to null after construction.
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        originField.set(zone, null);
        // The call to name.subdomain(origin) inside findRecords will now NPE.
        Name query = Name.fromConstantString("sub.example.com.");
        assertThrows(NullPointerException.class, () -> zone.findRecords(query, Type.A));
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
│  └─ Zone_findRecords_Test ✔
│     ├─ findRecords_wildcardMatch_returnsSuccess() ✔
│     ├─ findRecords_nameNotInZone_returnsNxDomain() ✔
│     ├─ findRecords_zoneWithNullOrigin_throwsNullPointerException() ✔
│     ├─ findRecords_exactMatch_returnsSuccess() ✔
│     ├─ findRecords_invalidType_throwsInvalidTypeException() ✘ Expected org.xbill.DNS.InvalidTypeException to be thrown, but nothing was thrown.
│     └─ findRecords_nullName_throwsIllegalArgumentException() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:Zone_findRecords_Test:findRecords_invalidType_throwsInvalidTypeException()
    MethodSource [className = 'org.xbill.DNS.Zone_findRecords_Test', methodName = 'findRecords_invalidType_throwsInvalidTypeException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected org.xbill.DNS.InvalidTypeException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       org.xbill.DNS.Zone_findRecords_Test.findRecords_invalidType_throwsInvalidTypeException(Zone_findRecords_Test.java:74)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 452 ms
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
[         5 tests successful      ]
[         1 tests failed          ]


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
 

api document of method findRecords(Name, int): 

   * Looks up Records in the zone. The answer can be a {@code CNAME} instead of the actual requested
   * type and wildcards are expanded.
   *
   * @param name The name to look up
   * @param type The type to look up
   * @return A SetResponse object
   * @throws IllegalArgumentException if {@code name} is {@code null}.
   * @throws InvalidTypeException if the specified {@code type} is invalid.
   * @see SetResponse
   

return type: 
org.xbill.DNS.SetResponse

dependent classes: 
class `org.xbill.DNS.Name`:
	
class `org.xbill.DNS.SetResponseType`:
	dependent fields:
		org.xbill.DNS.SetResponseType org.xbill.DNS.SetResponseType.NXDOMAIN;
	
class `org.xbill.DNS.Zone`:
	dependent fields:
		org.xbill.DNS.Name org.xbill.DNS.Zone.origin;
	related functions:
		method `public void addRRset(RRset)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: 
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
		method `public void removeRRset(Name, int)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: 
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
		method `public String toMasterFile()` returns `java.lang.String`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: 
   * Returns the contents of the zone in master file format.
   *
   * @see Master
   ;
		method `public void addRecord(T)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: 
   * Adds a record to the zone. If there is an existing {@link RRset} of the same {@link Name} and
   * {@link Type}, the record is <b>added</b> to this set.
   *
   * @param r The record to add.
   * @throws IllegalArgumentException if {@code name} is {@code null} or if the rrset name does not
   *     match the zone origin.
   ;
		method `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`;
		method `private void fromXFR(ZoneTransferIn)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`;
		method `public RRset findExactMatch(Name, int)` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: 
   * Looks up Records in the zone, finding exact matches only.
   *
   * @param name The name to look up
   * @param type The type to look up
   * @return The matching RRset or {@code null} if no exact match is found.
   * @throws IllegalArgumentException if {@code name} is {@code null}.
   * @throws InvalidTypeException if the specified {@code type} is invalid.
   * @see RRset
   ;
class `org.xbill.DNS.SetResponse`:
	related functions:
		method ` static SetResponse ofType(SetResponseType, RRset, boolean)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`;
		method `public boolean isNXDOMAIN()` returns `boolean`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document:  Is the answer to the query that the name does not exist? ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
