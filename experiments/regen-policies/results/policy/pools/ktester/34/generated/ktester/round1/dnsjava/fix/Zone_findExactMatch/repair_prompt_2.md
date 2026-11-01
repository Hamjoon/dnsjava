


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Zone_findExactMatch_Test {

    // -------------------------------------------------------------------------
    private Name zoneName;

    private SOARecord soa;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aTest;

    private AAAARecord aaaaTest;

    private ARecord aUnique;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        zoneName = Name.fromConstantString("example.");
        soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6 = InetAddress.getByName("::1");
        aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
        aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6);
        aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        // Initialise the zone with the mandatory SOA/NS records and a few test records.
        Record[] records = new Record[] { soa, ns1, ns2, aTest, aaaaTest, aUnique };
        zone = new Zone(zoneName, records);
        // Do NOT re‑initialize the zone – the empty constructor would fail validation
        // because a zone must contain at least an SOA and an NS record.
    }

    /**
     * Creates an {@link ARecord} for the supplied name.
     */
    private static ARecord aRecord(Name name, String ip) throws IOException {
        return new ARecord(name, DClass.IN, 3600, InetAddress.getByName(ip));
    }

    /**
     * Creates an {@link MXRecord} for the supplied name.
     */
    private static MXRecord mxRecord(Name name, int preference, String exchange) throws IOException {
        return new MXRecord(name, DClass.IN, 3600, preference, Name.fromConstantString(exchange));
    }

    /**
     * Adds a list of records to the zone using {@link Zone#addRecord(Record)}.
     */
    private void addRecords(Record... records) {
        for (Record r : records) {
            zone.addRecord(r);
        }
    }

    /**
     * Returns a fresh {@link Name} instance with the same textual representation as the argument.
     */
    private static Name freshName(String textual) throws IOException {
        return Name.fromConstantString(textual);
    }

    /**
     * Data for the “null name”, “invalid type”, “non‑existent”, “case‑insensitive”, etc.
     */
    static Stream<Arguments> invalidArgumentProvider() {
        return Stream.of(Arguments.of(null, Type.A, IllegalArgumentException.class, "name must not be null"), Arguments.of(Name.root, -1, InvalidTypeException.class, "type"), Arguments.of(Name.root, 65536, InvalidTypeException.class, "type"));
    }

    /**
     * Data for successful look‑ups where a copy must be returned.
     */
    static Stream<Arguments> exactMatchProvider() throws IOException {
        Name host = Name.fromConstantString("host.example.");
        ARecord a = aRecord(host, "127.0.0.10");
        AAAARecord aaaa = new AAAARecord(host, DClass.IN, 3600, InetAddress.getByName("::1"));
        return Stream.of(Arguments.of(host, Type.A, List.of(a)), Arguments.of(freshName("host.example."), Type.A, List.of(a)), Arguments.of(Name.fromConstantString("HOST.EXAMPLE."), Type.A, List.of(a)));
    }

    /**
     * Data for multiple types stored under the same name.
     */
    static Stream<Arguments> multiTypeProvider() throws IOException {
        Name multi = Name.fromConstantString("multi.example.");
        ARecord a = aRecord(multi, "127.0.0.11");
        MXRecord mx = mxRecord(multi, 10, "mail.example.");
        return Stream.of(Arguments.of(multi, Type.A, a), Arguments.of(multi, Type.MX, mx));
    }

    /* --------------------------------------------------------------------- *
     * Helper method to set private fields of RRset via reflection.
     * --------------------------------------------------------------------- */
    private static void setRRsetNameAndType(RRset rrset, Name name, int type) throws Exception {
        Field nameField = RRset.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(rrset, name);
        Field typeField = RRset.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.setInt(rrset, type);
    }

    @Test
    void findExactMatch_nullName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> zone.findExactMatch(null, Type.A)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("null");
    }

    @Test
    void findExactMatch_invalidType_throwsInvalidTypeException() {
        // Type.check throws InvalidTypeException for values that are not defined
        assertThatThrownBy(() -> zone.findExactMatch(Name.root, -1)).isInstanceOf(InvalidTypeException.class).hasMessageContaining("type");
    }

    @Test
    void findExactMatch_existingRecord_returnsCopy() throws Exception {
        // The zone contains an A record for "test.example."
        RRset original = zone.findExactMatch(aTest.getName(), Type.A);
        assertThat(original).isNotNull();
        // The method must return a *new* RRset instance (copy) – not the internal one
        RRset result = zone.findExactMatch(aTest.getName(), Type.A);
        assertThat(result).isNotNull();
        // copy, not same reference
        assertThat((Object) result).isNotSameAs(original);
        // equal content
        assertThat(result).isEqualTo(original);
        // Mutating the returned copy must not affect the zone
        Record extra = new ARecord(new Name("extra", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.9"));
        result.addRR(extra);
        assertThat(result).contains(extra);
        // The zone still sees only the original A record
        RRset afterMutation = zone.findExactMatch(aTest.getName(), Type.A);
        assertThat(afterMutation).doesNotContain(extra);
    }

    @Test
    void findExactMatch_nonExistingName_returnsNull() {
        Name unknown = Name.fromConstantString("doesnotexist.example.");
        RRset result = zone.findExactMatch(unknown, Type.A);
        assertThat(result).isNull();
    }

    @Test
    void findExactMatch_existingNameWrongType_returnsNull() throws Exception {
        Name host = Name.fromConstantString("host.example.");
        addRecords(aRecord(host, "127.0.0.12"));
        // Query for a type that is not present (TXT)
        RRset result = zone.findExactMatch(host, Type.TXT);
        assertThat(result).isNull();
    }

    @ParameterizedTest(name = "[{index}] name={0}, type={1} → expect {2}")
    @MethodSource("invalidArgumentProvider")
    void findExactMatch_invalidArguments_throwsException(Name name, int type, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        assertThatThrownBy(() -> zone.findExactMatch(name, type)).isInstanceOf(expectedException).hasMessageContaining(expectedMessageFragment);
    }

    @ParameterizedTest(name = "Multiple types – name={0}, type={1}")
    @MethodSource("multiTypeProvider")
    void findExactMatch_multipleTypes_returnCorrectRRset(Name name, int type, Record record) throws IOException {
        // Add both records (A and MX) for the same name
        addRecords(record);
        // Also add the counterpart type so the zone contains *both* types
        if (type == Type.A) {
            addRecords(mxRecord(name, 20, "mail.example."));
        } else {
            addRecords(aRecord(name, "127.0.0.13"));
        }
        RRset result = zone.findExactMatch(name, type);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result).contains(record);
    }

    @Test
    void findExactMatch_noMatchWhenNameExistsButDifferentType() throws IOException {
        Name host = Name.fromConstantString("host.example.");
        // type A only
        addRecords(aRecord(host, "127.0.0.14"));
        // ask for MX
        RRset result = zone.findExactMatch(host, Type.MX);
        assertThat(result).isNull();
    }

    @Test
    void findExactMatch_afterRemoval_returnsNull() throws IOException {
        Name temp = Name.fromConstantString("temp.example.");
        ARecord a = aRecord(temp, "127.0.0.15");
        addRecords(a);
        RRset beforeRemoval = zone.findExactMatch(temp, Type.A);
        assertThat(beforeRemoval).isNotNull();
        zone.removeRRset(temp, Type.A);
        RRset afterRemoval = zone.findExactMatch(temp, Type.A);
        assertThat(afterRemoval).isNull();
    }

    @Test
    void findExactMatch_emptyRRset_returnsCopyWithZeroRecords() throws Exception {
        Name emptyName = Name.fromConstantString("empty.example.");
        // Create an empty RRset (no records) and add it directly.
        RRset emptyRRset = new RRset();
        // Use reflection to set the private fields 'name' and 'type'
        setRRsetNameAndType(emptyRRset, emptyName, Type.A);
        zone.addRRset(emptyRRset);
        RRset result = zone.findExactMatch(emptyName, Type.A);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(emptyName);
        assertThat(result.getType()).isEqualTo(Type.A);
        // zero records
        assertThat(result.size()).isZero();
        assertThat((Object) result).isNotSameAs(emptyRRset);
    }

    @Test
    void findExactMatch_maxValidType_returnsNullWhenAbsent() throws IOException {
        Name maxName = Name.fromConstantString("maxtype.example.");
        // No record of type 65535 is added.
        RRset result = zone.findExactMatch(maxName, 65535);
        assertThat(result).isNull();
    }

    @Test
    void findExactMatch_existingRecord_returnsCopy_unchangedAfterMutation() throws IOException {
        // Populate zone with a known A record
        Name name = Name.fromConstantString("test.example.");
        ARecord a = aRecord(name, "127.0.0.16");
        addRecords(a);
        RRset original = zone.findExactMatch(name, Type.A);
        assertThat(original).isNotNull();
        // Obtain a copy and mutate it
        RRset copy = zone.findExactMatch(name, Type.A);
        Record extra = aRecord(Name.fromConstantString("extra.example."), "127.0.0.99");
        copy.addRR(extra);
        assertThat(copy).contains(extra);
        // Original in the zone must stay unchanged
        RRset afterMutation = zone.findExactMatch(name, Type.A);
        assertThat(afterMutation).doesNotContain(extra);
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
│  └─ Zone_findExactMatch_Test ✔
│     ├─ findExactMatch_emptyRRset_returnsCopyWithZeroRecords() ✘ name
│     ├─ findExactMatch_noMatchWhenNameExistsButDifferentType() ✔
│     ├─ findExactMatch_existingNameWrongType_returnsNull() ✔
│     ├─ findExactMatch_nullName_throwsIllegalArgumentException() ✔
│     ├─ findExactMatch_afterRemoval_returnsNull() ✔
│     ├─ findExactMatch_existingRecord_returnsCopy() ✘ record does not match rrset
│     ├─ findExactMatch_maxValidType_returnsNullWhenAbsent() ✔
│     ├─ findExactMatch_multipleTypes_returnCorrectRRset(Name, int, Record) ✔
│     │  ├─ Multiple types – name=multi.example., type=1 ✔
│     │  └─ Multiple types – name=multi.example., type=15 ✔
│     ├─ findExactMatch_invalidArguments_throwsException(Name, int, Class, String) ✔
│     │  ├─ [1] name=null, type=1 → expect class java.lang.IllegalArgumentException ✔
│     │  ├─ [2] name=., type=-1 → expect class org.xbill.DNS.InvalidTypeException ✔
│     │  └─ [3] name=., type=65536 → expect class org.xbill.DNS.InvalidTypeException ✔
│     ├─ findExactMatch_invalidType_throwsInvalidTypeException() ✔
│     ├─ findExactMatch_existingRecord_returnsCopy_unchangedAfterMutation() ✘ record does not match rrset
│     └─ findExactMatch_nonExistingName_returnsNull() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (3):
  JUnit Jupiter:Zone_findExactMatch_Test:findExactMatch_emptyRRset_returnsCopyWithZeroRecords()
    MethodSource [className = 'org.xbill.DNS.Zone_findExactMatch_Test', methodName = 'findExactMatch_emptyRRset_returnsCopyWithZeroRecords', methodParameterTypes = '']
    => java.lang.NoSuchFieldException: name
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       org.xbill.DNS.Zone_findExactMatch_Test.setRRsetNameAndType(Zone_findExactMatch_Test.java:120)
       org.xbill.DNS.Zone_findExactMatch_Test.findExactMatch_emptyRRset_returnsCopyWithZeroRecords(Zone_findExactMatch_Test.java:228)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_findExactMatch_Test:findExactMatch_existingRecord_returnsCopy()
    MethodSource [className = 'org.xbill.DNS.Zone_findExactMatch_Test', methodName = 'findExactMatch_existingRecord_returnsCopy', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: record does not match rrset
       org.xbill.DNS.RRset.checkSameRRset(RRset.java:149)
       org.xbill.DNS.RRset.addRR(RRset.java:113)
       org.xbill.DNS.RRset.addRR(RRset.java:102)
       org.xbill.DNS.Zone_findExactMatch_Test.findExactMatch_existingRecord_returnsCopy(Zone_findExactMatch_Test.java:153)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:Zone_findExactMatch_Test:findExactMatch_existingRecord_returnsCopy_unchangedAfterMutation()
    MethodSource [className = 'org.xbill.DNS.Zone_findExactMatch_Test', methodName = 'findExactMatch_existingRecord_returnsCopy_unchangedAfterMutation', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: record does not match rrset
       org.xbill.DNS.RRset.checkSameRRset(RRset.java:149)
       org.xbill.DNS.RRset.addRR(RRset.java:113)
       org.xbill.DNS.RRset.addRR(RRset.java:102)
       org.xbill.DNS.Zone_findExactMatch_Test.findExactMatch_existingRecord_returnsCopy_unchangedAfterMutation(Zone_findExactMatch_Test.java:258)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]

Test run finished after 439 ms
[         6 containers found      ]
[         0 containers skipped    ]
[         6 containers started    ]
[         0 containers aborted    ]
[         6 containers successful ]
[         0 containers failed     ]
[        15 tests found           ]
[         0 tests skipped         ]
[        15 tests started         ]
[         0 tests aborted         ]
[        12 tests successful      ]
[         3 tests failed          ]


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
 

api document of method findExactMatch(Name, int): 

   * Looks up Records in the zone, finding exact matches only.
   *
   * @param name The name to look up
   * @param type The type to look up
   * @return The matching RRset or {@code null} if no exact match is found.
   * @throws IllegalArgumentException if {@code name} is {@code null}.
   * @throws InvalidTypeException if the specified {@code type} is invalid.
   * @see RRset
   

return type: 
org.xbill.DNS.RRset

dependent classes: 
class `org.xbill.DNS.Name`:
	
class `org.xbill.DNS.Zone`:
	related functions:
		method `public void removeRRset(Name, int)` returns `void`, related with `org.xbill.DNS.Zone.findExactMatch(Name, int)`, api document: 
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
		method `public RRset getNS()` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Zone.findExactMatch(Name, int)`, api document:  Returns the zone origin's {@link NSRecord NS records}. ;
		method `public RRset findExactMatch(Name, int)` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Zone.findExactMatch(Name, int)`, api document: 
   * Looks up Records in the zone, finding exact matches only.
   *
   * @param name The name to look up
   * @param type The type to look up
   * @return The matching RRset or {@code null} if no exact match is found.
   * @throws IllegalArgumentException if {@code name} is {@code null}.
   * @throws InvalidTypeException if the specified {@code type} is invalid.
   * @see RRset
   ;
		method `public SetResponse findRecords(Name, int)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Zone.findExactMatch(Name, int)`, api document: 
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
class `org.xbill.DNS.dnssec.SMessage`:
	related functions:
		method `private void checkSectionValidity(int)` returns `void`, related with `org.xbill.DNS.Zone.findExactMatch(Name, int)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
