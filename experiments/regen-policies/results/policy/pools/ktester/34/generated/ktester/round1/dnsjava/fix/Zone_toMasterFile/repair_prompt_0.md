


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
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

class Zone_toMasterFile_Test {

    private Name zoneName;

    private SOARecord soa;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aRecord;

    private AAAARecord aaaaRecord;

    private ARecord wildA;

    private TXTRecord wildTXT;

    private Zone zone;

    /* --------------------------------------------------------------------- */
    private static final class DummyNode {

        private final String marker;

        DummyNode(String marker) {
            this.marker = marker;
        }

        @Override
        public String toString() {
            return marker;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a deterministic zone – the same data that is used in the original
        // ZoneTest class, but limited to a few records that are easy to verify.
        zoneName = Name.fromConstantString("example.");
        soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        aRecord = new ARecord(new Name("www", zoneName), DClass.IN, 3600, localhost4);
        aaaaRecord = new AAAARecord(new Name("www", zoneName), DClass.IN, 3600, InetAddress.getByName("::1"));
        wildA = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        wildTXT = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");
        Record[] records = new Record[] { soa, ns1, ns2, aRecord, aaaaRecord, wildA, wildTXT };
        zone = new Zone(zoneName, records);
        zoneName = Name.fromConstantString("example.");
        /* Create a zone with *no* records – we will inject everything we need
         * via reflection in each test case. */
        zone = new Zone(zoneName, new Record[0]);
    }

    /* --------------------------------------------------------------------- */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void toMasterFile_containsAllRecordsInCorrectOrder()throws Exception {
        // Build a realistic zone using the public constructor (records defined in the original suite)
        // This test is kept for regression – it validates normal behaviour.
        SOARecord soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        NSRecord ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NSRecord ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        ARecord aRecord = new ARecord(new Name("www", zoneName), DClass.IN, 3600, InetAddress.getLoopbackAddress());
        AAAARecord aaaaRecord = new AAAARecord(new Name("www", zoneName), DClass.IN, 3600, InetAddress.getByName("::1"));
        ARecord wildA = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        TXTRecord wildTXT = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");
        Record[] records = new Record[] { soa, ns1, ns2, aRecord, aaaaRecord, wildA, wildTXT };
        Zone realZone = new Zone(zoneName, records);
        String masterFile = realZone.toMasterFile();
        assertThat(masterFile).startsWith(soa.toString());
        assertThat(masterFile).contains(ns1.toString(), ns2.toString(), aRecord.toString(), aaaaRecord.toString(), wildA.toString(), wildTXT.toString());
        long occurrence = masterFile.lines().filter(line -> line.contains(zoneName.toString())).count();
        assertThat(occurrence).isEqualTo(1L);
    }

    @Test
    void toMasterFile_isIdempotent() {
        String first = zone.toMasterFile();
        String second = zone.toMasterFile();
        // The method should be pure – repeated calls must return identical strings
        assertThat(first).isEqualTo(second);
    }

    @SuppressWarnings("unchecked")
    private static Map<Name, Object> getDataMap(Zone z) throws Exception {
        Field f = Zone.class.getDeclaredField("data");
        f.setAccessible(true);
        return (Map<Name, Object>) f.get(z);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void originNode_alwaysRendered_whenNoOtherRecords() throws Exception {
        // originNode = DummyNode("ORIGIN:originNode; ")
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // data map stays empty
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("ORIGIN:originNode; ");
    }

    @Test
    void originNode_notRenderedForEntryMatchingOrigin() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // put a single entry whose key equals the zone origin
        Map<Name, Object> data = getDataMap(zone);
        data.put(zoneName, new DummyNode("NODE:shouldNotAppear; "));
        String master = zone.toMasterFile();
        // only the origin marker must be present
        assertThat(master).isEqualTo("ORIGIN:originNode; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nonOriginEntry_isRendered() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        Map<Name, Object> data = getDataMap(zone);
        Name a = Name.fromConstantString("a.example.");
        data.put(a, new DummyNode("NODE:nodeA; "));
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("ORIGIN:originNode; NODE:nodeA; ");
    }

    @Test
    void multipleNonOriginEntries_areRenderedInMapOrder() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        Map<Name, Object> data = getDataMap(zone);
        Name a = Name.fromConstantString("a.example.");
        Name b = Name.fromConstantString("b.example.");
        data.put(a, new DummyNode("NODE:nodeA; "));
        data.put(b, new DummyNode("NODE:nodeB; "));
        String master = zone.toMasterFile();
        // ConcurrentSkipListMap preserves natural order of Name,
        // which for these names is a.example. then b.example.
        assertThat(master).isEqualTo("ORIGIN:originNode; NODE:nodeA; NODE:nodeB; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptyZone_containsOnlyOrigin() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // data map is already empty
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("ORIGIN:originNode; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullOriginNode_andEmptyData_returnsEmptyString() throws Exception {
        setField(zone, "originNode", null);
        // data empty
        String master = zone.toMasterFile();
        assertThat(master).isEmpty();
    }

    @Test
    void nullOriginNode_withNonOriginEntry_rendersOnlyEntry() throws Exception {
        setField(zone, "originNode", null);
        Map<Name, Object> data = getDataMap(zone);
        Name a = Name.fromConstantString("a.example.");
        data.put(a, new DummyNode("NODE:nodeA; "));
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("NODE:nodeA; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mixedEntries_originKeySkipped_othersRendered() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        Map<Name, Object> data = getDataMap(zone);
        // entry that matches origin – should be skipped
        data.put(zoneName, new DummyNode("NODE:originKey; "));
        // two genuine non‑origin entries
        Name a = Name.fromConstantString("a.example.");
        Name b = Name.fromConstantString("b.example.");
        data.put(a, new DummyNode("NODE:nodeA; "));
        data.put(b, new DummyNode("NODE:nodeB; "));
        String master = zone.toMasterFile();
        assertThat(master).isEqualTo("ORIGIN:originNode; NODE:nodeA; NODE:nodeB; ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullOrigin_throwsNullPointerException() throws Exception {
        // force origin to null
        setField(zone, "origin", null);
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // any iteration over data will hit the NPE
        assertThatThrownBy(() -> zone.toMasterFile()).isInstanceOf(NullPointerException.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullNodeValue_throwsNullPointerException() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        Map<Name, Object> data = getDataMap(zone);
        Name a = Name.fromConstantString("a.example.");
        // deliberately null node
        data.put(a, null);
        assertThatThrownBy(() -> zone.toMasterFile()).isInstanceOf(NullPointerException.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void incompatibleKeyType_throwsClassCastException() throws Exception {
        setField(zone, "originNode", new DummyNode("ORIGIN:originNode; "));
        // bypass the generic type safety via raw map access
        @SuppressWarnings("unchecked")
        Map rawMap = getDataMap(zone);
        rawMap.put("not-a-Name-key", new DummyNode("NODE:nodeA; "));
        assertThatThrownBy(() -> zone.toMasterFile()).isInstanceOf(ClassCastException.class);
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

╷
├─ JUnit Jupiter ✔
│  └─ Zone_toMasterFile_Test ✔
│     ├─ incompatibleKeyType_throwsClassCastException() ✘ example.: no data specified
│     ├─ nonOriginEntry_isRendered() ✘ example.: no data specified
│     ├─ toMasterFile_containsAllRecordsInCorrectOrder() ✘ example.: no data specified
│     ├─ originNode_alwaysRendered_whenNoOtherRecords() ✘ example.: no data specified
│     ├─ toMasterFile_isIdempotent() ✘ example.: no data specified
│     ├─ originNode_notRenderedForEntryMatchingOrigin() ✘ example.: no data specified
│     ├─ nullOrigin_throwsNullPointerException() ✘ example.: no data specified
│     ├─ multipleNonOriginEntries_areRenderedInMapOrder() ✘ example.: no data specified
│     ├─ nullOriginNode_andEmptyData_returnsEmptyString() ✘ example.: no data specified
│     ├─ mixedEntries_originKeySkipped_othersRendered() ✘ example.: no data specified
│     ├─ nullNodeValue_throwsNullPointerException() ✘ example.: no data specified
│     ├─ nullOriginNode_withNonOriginEntry_rendersOnlyEntry() ✘ example.: no data specified
│     └─ emptyZone_containsOnlyOrigin() ✘ example.: no data specified
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (13):
  JUnit Jupiter:Zone_toMasterFile_Test:incompatibleKeyType_throwsClassCastException()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'incompatibleKeyType_throwsClassCastException', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:nonOriginEntry_isRendered()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'nonOriginEntry_isRendered', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:toMasterFile_containsAllRecordsInCorrectOrder()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'toMasterFile_containsAllRecordsInCorrectOrder', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:originNode_alwaysRendered_whenNoOtherRecords()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'originNode_alwaysRendered_whenNoOtherRecords', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:toMasterFile_isIdempotent()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'toMasterFile_isIdempotent', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:originNode_notRenderedForEntryMatchingOrigin()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'originNode_notRenderedForEntryMatchingOrigin', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:nullOrigin_throwsNullPointerException()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'nullOrigin_throwsNullPointerException', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:multipleNonOriginEntries_areRenderedInMapOrder()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'multipleNonOriginEntries_areRenderedInMapOrder', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:nullOriginNode_andEmptyData_returnsEmptyString()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'nullOriginNode_andEmptyData_returnsEmptyString', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:mixedEntries_originKeySkipped_othersRendered()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'mixedEntries_originKeySkipped_othersRendered', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:nullNodeValue_throwsNullPointerException()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'nullNodeValue_throwsNullPointerException', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:nullOriginNode_withNonOriginEntry_rendersOnlyEntry()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'nullOriginNode_withNonOriginEntry_rendersOnlyEntry', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Zone_toMasterFile_Test:emptyZone_containsOnlyOrigin()
    MethodSource [className = 'org.xbill.DNS.Zone_toMasterFile_Test', methodName = 'emptyZone_containsOnlyOrigin', methodParameterTypes = '']
    => java.io.IOException: example.: no data specified
       org.xbill.DNS.Zone.validate(Zone.java:188)
       org.xbill.DNS.Zone.<init>(Zone.java:117)
       org.xbill.DNS.Zone_toMasterFile_Test.setupBeforeEach(Zone_toMasterFile_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]

Test run finished after 77 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        13 tests found           ]
[         0 tests skipped         ]
[        13 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[        13 tests failed          ]


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
 

api document of method toMasterFile(): 

   * Returns the contents of the zone in master file format.
   *
   * @see Master
   

dependent classes: 
class `org.xbill.DNS.Zone`:
	dependent fields:
		java.util.Map<org.xbill.DNS.Name, java.lang.Object> org.xbill.DNS.Zone.data;
		org.xbill.DNS.Name org.xbill.DNS.Zone.origin;
		java.lang.Object org.xbill.DNS.Zone.originNode;
	related functions:
		method `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.toMasterFile()`;
class `org.xbill.DNS.SVCBBase`:
	related functions:
		method `protected String rrToString()` returns `java.lang.String`, related with `org.xbill.DNS.Zone.toMasterFile()`;
		method `protected void rrToWire(DNSOutput, Compression, boolean)` returns `void`, related with `org.xbill.DNS.Zone.toMasterFile()`;
class `org.xbill.DNS.Zone.ZoneIterator`:
	related functions:
		method `public RRset next()` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Zone.toMasterFile()`;
class `org.xbill.DNS.Header`:
	related functions:
		method `public String printFlags()` returns `java.lang.String`, related with `org.xbill.DNS.Zone.toMasterFile()`, api document:  Converts the header's flags into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
