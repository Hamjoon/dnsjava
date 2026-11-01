


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentSkipListMap;

@ExtendWith(MockitoExtension.class)
class Zone_toString_Test {

    private static Name ZONE_NAME;

    private static SOARecord SOA;

    private static NSRecord NS1;

    private static NSRecord NS2;

    private static ARecord A_UNIQUE;

    private static ARecord A_TEST;

    private static AAAARecord AAAA_1_TEST;

    private static AAAARecord AAAA_2_TEST;

    private static ARecord A_WILD;

    private static TXTRecord TXT_WILD;

    private Zone zone;

    private static final Name ROOT = Name.fromConstantString(".");

    private static final Name EXAMPLE = Name.fromConstantString("example.");

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a deterministic zone that mirrors the one used in ZoneTest.
        ZONE_NAME = Name.fromConstantString("example.");
        SOA = new SOARecord(ZONE_NAME, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostadmin", ZONE_NAME), 1, 21600L, 7200L, 2160000L, 3600L);
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
        Record[] records = new Record[] { SOA, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD };
        zone = assertDoesNotThrow(() -> new Zone(ZONE_NAME, records));
    }

    @AfterEach
    void teardownAfterEach() {
        zone = null;
    }

    /**
     * Creates a {@link Zone} with the given origin and records.
     */
    private Zone createZone(Name origin, Record... records) {
        return assertDoesNotThrow(() -> new Zone(origin, records));
    }

    /**
     * Returns a simple SOA record for the supplied origin.
     */
    private SOARecord makeSOA(Name origin) throws IOException {
        return new SOARecord(origin, DClass.IN, 3600L, Name.fromConstantString("ns1." + origin), new Name("hostmaster", origin), 1, 7200L, 3600L, 1209600L, 3600L);
    }

    /**
     * Returns a simple NS record for the supplied origin.
     */
    private NSRecord makeNS(Name origin, String ns) throws IOException {
        return new NSRecord(origin, DClass.IN, 3600L, Name.fromConstantString(ns));
    }

    /**
     * Verifies that {@link Zone#toString()} returns a master‑file representation that contains
     * the textual form of every record that was added to the zone.
     */
    @Test
    void toStringContainsAllRecords() throws Exception {
        Name zoneName = EXAMPLE;
        SOARecord soa = makeSOA(zoneName);
        NSRecord ns1 = makeNS(zoneName, "ns1.example.");
        NSRecord ns2 = makeNS(zoneName, "ns2.example.");
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");
        ARecord aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        ARecord aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
        AAAARecord aaaa1 = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6a);
        AAAARecord aaaa2 = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6b);
        ARecord aWild = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        TXTRecord txtWild = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");
        zone = createZone(zoneName, soa, ns1, ns2, aUnique, aTest, aaaa1, aaaa2, aWild, txtWild);
        String masterFile = zone.toString();
        // Verify each record's textual representation appears.
        List<Record> all = List.of(soa, ns1, ns2, aUnique, aTest, aaaa1, aaaa2, aWild, txtWild);
        for (Record r : all) {
            assertThat(masterFile).contains(r.toString());
        }
        // Basic sanity checks – the output must be non‑empty.
        assertThat(masterFile).isNotEmpty();
    }

    /**
     * Empty zone – only the default $ORIGIN line should be present.
     */
    @Test
    void emptyZoneProducesOnlyOrigin() throws Exception {
        // Create a minimal valid zone (SOA + NS) and then wipe its RRset map.
        Constructor<Zone> ctor = Zone.class.getDeclaredConstructor(Name.class, Record[].class);
        ctor.setAccessible(true);
        Zone rawZone = ctor.newInstance(ROOT, new Record[] { makeSOA(ROOT), makeNS(ROOT, "ns.root.") });
        // Clear the internal map.
        java.lang.reflect.Field mapField = Zone.class.getDeclaredField("rrsets");
        mapField.setAccessible(true);
        mapField.set(rawZone, new ConcurrentSkipListMap<>());
        // The string representation should now consist solely of the $ORIGIN line.
        String result = rawZone.toString();
        assertThat(result).isEqualTo("$ORIGIN .\n");
    }

    /**
     * Zone with only SOA and NS records – both must appear in order.
     */
    @Test
    void zoneWithSoaAndNsOnly() throws IOException {
        SOARecord soa = makeSOA(EXAMPLE);
        NSRecord ns = makeNS(EXAMPLE, "ns.example.");
        zone = createZone(EXAMPLE, soa, ns);
        String master = zone.toString();
        assertThat(master).contains(soa.toString());
        assertThat(master).contains(ns.toString());
        // Ensure the order: SOA first, then NS.
        int soaIdx = master.indexOf(soa.toString());
        int nsIdx = master.indexOf(ns.toString());
        assertTrue(soaIdx >= 0 && nsIdx > soaIdx, "SOA record should appear before NS record");
    }

    /**
     * Custom $ORIGIN directive is correctly emitted.
     */
    @Test
    void customOriginIsPrinted() throws IOException {
        Name customOrigin = Name.fromConstantString("example.com.");
        SOARecord soa = makeSOA(customOrigin);
        NSRecord ns = makeNS(customOrigin, "ns.example.com.");
        zone = createZone(customOrigin, soa, ns);
        String master = zone.toString();
        // The $ORIGIN line should be present.
        assertThat(master).startsWith("$ORIGIN example.com.\n");
        // The SOA line must follow the $ORIGIN line.
        assertThat(master).contains(soa.toString());
    }

    /**
     * Removing a record must be reflected in the master‑file output.
     */
    @Test
    void recordRemovalIsReflected() throws IOException {
        // Build a zone with SOA, NS and an extra A record.
        SOARecord soa = makeSOA(EXAMPLE);
        NSRecord ns = makeNS(EXAMPLE, "ns.example.");
        ARecord a = new ARecord(new Name("example", EXAMPLE), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        zone = createZone(EXAMPLE, soa, ns, a);
        // Remove the A record.
        zone.removeRecord(a);
        String master = zone.toString();
        assertThat(master).contains(ns.toString());
        assertThat(master).doesNotContain(a.toString());
    }

    /**
     * Propagation of IOException from toMasterFile() via a Mockito spy.
     */
//     @Test
//     void toStringPropagatesIOExceptionFromToMasterFile() throws IOException {
//         // Normal zone with required SOA/NS.
//         SOARecord soa = makeSOA(EXAMPLE);
//         NSRecord ns = makeNS(EXAMPLE, "ns.example.");
//         Zone faultyZone = new Zone(EXAMPLE, new Record[] { soa, ns }) {
// 
//             @Override
//             public String toMasterFile() throws IOException {
//                 throw new IOException("forced");
//             }
//         };
//         RuntimeException ex = assertThrows(RuntimeException.class, faultyZone::toString);
//         assertThat(ex).hasCauseInstanceOf(IOException.class).hasMessageContaining("forced");
//     }

    /**
     * Creating a Zone with a null origin should throw NullPointerException.
     */
    @Test
    void nullOriginThrowsNPE() {
        // The public constructor validates the origin and throws IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> new Zone(null, new Record[0]));
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
│  └─ Zone_toString_Test ✔
│     ├─ toStringContainsAllRecords() ✔
│     ├─ zoneWithSoaAndNsOnly() ✔
│     ├─ customOriginIsPrinted() ✘ 
│     │        Expecting actual:
│     │          "example.com.		3600	IN	SOA	ns1.example.com. hostmaster.example.com. 1 7200 3600 1209600 3600
│     │        example.com.		3600	IN	NS	ns.example.com.
│     │        "
│     │        to start with:
│     │          "$ORIGIN example.com.
│     │        "
│     ├─ nullOriginThrowsNPE() ✔
│     ├─ emptyZoneProducesOnlyOrigin() ✘ Invalid name 'ns1..'
│     └─ recordRemovalIsReflected() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:Zone_toString_Test:customOriginIsPrinted()
    MethodSource [className = 'org.xbill.DNS.Zone_toString_Test', methodName = 'customOriginIsPrinted', methodParameterTypes = '']
    => java.lang.AssertionError: 
Expecting actual:
  "example.com.		3600	IN	SOA	ns1.example.com. hostmaster.example.com. 1 7200 3600 1209600 3600
example.com.		3600	IN	NS	ns.example.com.
"
to start with:
  "$ORIGIN example.com.
"

       org.xbill.DNS.Zone_toString_Test.customOriginIsPrinted(Zone_toString_Test.java:165)
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
  JUnit Jupiter:Zone_toString_Test:emptyZoneProducesOnlyOrigin()
    MethodSource [className = 'org.xbill.DNS.Zone_toString_Test', methodName = 'emptyZoneProducesOnlyOrigin', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Invalid name 'ns1..'
       org.xbill.DNS.Name.fromConstantString(Name.java:340)
       org.xbill.DNS.Zone_toString_Test.makeSOA(Zone_toString_Test.java:79)
       org.xbill.DNS.Zone_toString_Test.emptyZoneProducesOnlyOrigin(Zone_toString_Test.java:127)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]

Test run finished after 436 ms
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
 

api document of method toString(): 

   * Returns the contents of the zone as a string.
   *
   * @see #toMasterFile
   

dependent classes: 
class `org.xbill.DNS.Zone`:
	related functions:
		method `public String toString()` returns `java.lang.String`, related with `org.xbill.DNS.Zone.toString()`, api document: 
   * Returns the contents of the zone as a string.
   *
   * @see #toMasterFile
   ;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Zone.toString()`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
