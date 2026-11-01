


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
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.mockito.MockedStatic;
import java.net.InetAddress;

class Message_getTSIG_Test {

    /**
     * Utility to set a private field via reflection.
     */
    private static void setPrivateField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * When the ADDITIONAL section is empty, getTSIG() must return {@code null}.
     */
    @Test
    void getTSIG_noAdditionalSection_returnsNull() {
        // empty message, no records
        Message msg = new Message();
        assertNull(msg.getTSIG(), "TSIG should be null when no additional records are present");
    }

    /**
     * When the ADDITIONAL section contains records but the last one is not a TSIG,
     * getTSIG() must return {@code null}.
     */
    @Test
    void getTSIG_lastRecordNotTSIG_returnsNull() throws IOException {
        Message msg = new Message();
        // header reports two records
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(2);
        setPrivateField(msg, "header", mockHeader);
        // build additional section: ARecord then MXRecord (MX is not TSIG)
        List<Record> list = new ArrayList<>();
        Name name = Name.fromConstantString("example.com.");
        list.add(new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1")));
        list.add(new MXRecord(name, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com.")));
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        setPrivateField(msg, "sections", sections);
        assertNull(msg.getTSIG(), "TSIG should be null when the last additional record is not a TSIG");
    }

    /**
     * When a TSIG record is the last entry in the ADDITIONAL section,
     * getTSIG() must return that TSIGRecord.
     */
    @Test
    void getTSIG_lastRecordIsTSIG_returnsTSIGRecord() throws IOException {
        Message msg = new Message();
        // create a dummy TSIG key (algorithm and secret are arbitrary for the test)
        Name keyName = Name.fromConstantString("key.example.");
        Name algName = Name.fromConstantString("hmac-md5.sig-alg.reg.int.");
        TSIG tsigKey = new TSIG(keyName, algName, new byte[] { 0x01, 0x02, 0x03, 0x04 });
        // apply the TSIG to the message – this adds a TSIGRecord as the last additional record
        tsigKey.apply(msg, null);
        // sanity check: the header should now report one record in the ADDITIONAL section
        assertEquals(1, msg.getHeader().getCount(Section.ADDITIONAL));
        // retrieve the TSIG via the method under test
        TSIGRecord retrieved = msg.getTSIG();
        assertNotNull(retrieved, "TSIG should be returned when present as the last additional record");
        assertSame(TSIGRecord.class, retrieved.getClass(), "Returned record must be a TSIGRecord");
        // the retrieved record should be the same instance that was added by TSIG.apply()
        TSIGRecord expected = (TSIGRecord) msg.getSection(Section.ADDITIONAL).get(0);
        assertSame(expected, retrieved, "Returned TSIGRecord should be the one stored in the message");
    }

    /**
     * When multiple records exist in the ADDITIONAL section and the last one is a TSIG,
     * getTSIG() must still return the TSIGRecord.
     */
    @Test
    void getTSIG_multipleAdditionalRecords_lastIsTSIG_returnsTSIGRecord() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");
        // add a few non‑TSIG records first
        msg.addRecord(new TXTRecord(name, DClass.IN, 3600, "first"), Section.ADDITIONAL);
        msg.addRecord(new TXTRecord(name, DClass.IN, 3600, "second"), Section.ADDITIONAL);
        // now add a TSIG record
        Name keyName = Name.fromConstantString("key.example.");
        Name algName = Name.fromConstantString("hmac-md5.sig-alg.reg.int.");
        TSIG tsigKey = new TSIG(keyName, algName, new byte[] { 0x0A, 0x0B, 0x0C });
        tsigKey.apply(msg, null);
        // header count should reflect three additional records
        assertEquals(3, msg.getHeader().getCount(Section.ADDITIONAL));
        // getTSIG should return the last record (the TSIG)
        TSIGRecord tsig = msg.getTSIG();
        assertNotNull(tsig, "TSIG should be returned when it is the last additional record");
        assertEquals(Type.TSIG, tsig.getType(), "Returned record must have type TSIG");
        // ensure it is indeed the last element in the section list
        Record last = msg.getSection(Section.ADDITIONAL).get(2);
        assertSame(tsig, last, "TSIGRecord returned by getTSIG must be the last record in the ADDITIONAL section");
    }

    /**
     * When the internal {@code header} field is {@code null},
     * {@code getTSIG()} must throw a {@link NullPointerException}
     * because it attempts to call {@code header.getCount(...)}.
     */
    @Test
    void getTSIG_nullHeader_throwsNPE() {
        Message msg = new Message();
        // force header to null
        setPrivateField(msg, "header", null);
        assertThrows(NullPointerException.class, msg::getTSIG, "Calling getTSIG() with a null header should throw NPE");
    }

    /**
     * When the {@code sections[ADDITIONAL]} entry itself is {@code null},
     * {@code getTSIG()} must throw a {@link NullPointerException}
     * while trying to retrieve the list.
     */
    @Test
    void getTSIG_nullAdditionalSection_throwsNPE() {
        Message msg = new Message();
        // mock header to report one record in ADDITIONAL
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setPrivateField(msg, "header", mockHeader);
        // create an empty sections array but leave the ADDITIONAL slot null
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        setPrivateField(msg, "sections", sections);
        assertThrows(NullPointerException.class, msg::getTSIG, "Null additional section should result in NPE");
    }

    /**
     * If the header reports more records than are actually stored in the
     * {@code sections[ADDITIONAL]} list, {@code getTSIG()} will try to
     * access an invalid index and throw {@link IndexOutOfBoundsException}.
     */
//     @Test
//     void getTSIG_headerCountMismatch_throwsIOOBE() {
//         Message msg = new Message();
//         // header claims 5 records, but we only store 2
//         Header mockHeader = mock(Header.class);
//         when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(5);
//         setPrivateField(msg, "header", mockHeader);
//         // create a list with two generic records
//         List<Record> list = new ArrayList<>();
//         Name name = Name.fromConstantString("example.com.");
//         list.add(new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1")));
//         list.add(new MXRecord(name, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com.")));
//         // install the list into the sections array
//         @SuppressWarnings("unchecked")
//         List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
//         sections[Section.ADDITIONAL] = list;
//         setPrivateField(msg, "sections", sections);
//         assertThrows(IndexOutOfBoundsException.class, msg::getTSIG, "Header count larger than actual list size should cause IOBE");
//     }

    /**
     * When the last record **is** a TSIGRecord, {@code getTSIG()} must return
     * that exact instance.
     */
    @Test
    void getTSIG_lastRecordIsTSIG_returnsInstance() {
        Message msg = new Message();
        // header reports one record
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setPrivateField(msg, "header", mockHeader);
        // create a TSIGRecord (using the real TSIG.apply would also work,
        // but we construct it manually to keep the test focused)
        Name keyName = Name.fromConstantString("key.example.");
        TSIGRecord tsigRecord = mock(TSIGRecord.class);
        // force the public 'type' field to be Type.TSIG
        setPrivateField(tsigRecord, "type", Type.TSIG);
        List<Record> list = new ArrayList<>();
        list.add(tsigRecord);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        setPrivateField(msg, "sections", sections);
        TSIGRecord result = msg.getTSIG();
        assertNotNull(result, "TSIG should be returned when present as the last record");
        assertSame(tsigRecord, result, "Returned TSIGRecord must be the same instance stored in the section");
    }

    /**
     * When the ADDITIONAL section contains several records and the **last**
     * one is a TSIGRecord, {@code getTSIG()} must still return that TSIGRecord.
     */
//     @Test
//     void getTSIG_multipleRecords_lastIsTSIG_returnsTSIG() {
//         Message msg = new Message();
//         // header reports three records
//         Header mockHeader = mock(Header.class);
//         when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(3);
//         setPrivateField(msg, "header", mockHeader);
//         // first two are generic, third is TSIG
//         List<Record> list = new ArrayList<>();
//         Name name = Name.fromConstantString("example.com.");
//         list.add(new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1")));
//         list.add(new MXRecord(name, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com.")));
//         TSIGRecord tsigRecord = mock(TSIGRecord.class);
//         setPrivateField(tsigRecord, "type", Type.TSIG);
//         list.add(tsigRecord);
//         @SuppressWarnings("unchecked")
//         List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
//         sections[Section.ADDITIONAL] = list;
//         setPrivateField(msg, "sections", sections);
//         TSIGRecord result = msg.getTSIG();
//         assertNotNull(result, "TSIG should be returned when it is the last additional record");
//         assertSame(tsigRecord, result, "Returned TSIGRecord must be the last element in the list");
//     }

    /**
     * A TSIGRecord that is **not** the last element must be ignored;
     * {@code getTSIG()} should return {@code null}.
     */
//     @Test
//     void getTSIG_TSIGNotLast_returnsNull() {
//         Message msg = new Message();
//         // header reports two records
//         Header mockHeader = mock(Header.class);
//         when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(2);
//         setPrivateField(msg, "header", mockHeader);
//         // first is TSIG, second is ARecord
//         List<Record> list = new ArrayList<>();
//         TSIGRecord tsigRecord = mock(TSIGRecord.class);
//         setPrivateField(tsigRecord, "type", Type.TSIG);
//         list.add(tsigRecord);
//         Name name = Name.fromConstantString("example.com.");
//         list.add(new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1")));
//         @SuppressWarnings("unchecked")
//         List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
//         sections[Section.ADDITIONAL] = list;
//         setPrivateField(msg, "sections", sections);
//         assertNull(msg.getTSIG(), "TSIG not being the last record must result in null");
//     }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/commit-survival/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]

╷
├─ JUnit Jupiter ✔
│  └─ Message_getTSIG_Test ✔
│     ├─ getTSIG_nullAdditionalSection_throwsNPE() ✔
│     ├─ getTSIG_lastRecordIsTSIG_returnsInstance() ✘ java.lang.NoSuchFieldException: type
│     ├─ getTSIG_noAdditionalSection_returnsNull() ✔
│     ├─ getTSIG_lastRecordNotTSIG_returnsNull() ✔
│     ├─ getTSIG_multipleAdditionalRecords_lastIsTSIG_returnsTSIGRecord() ✘ Unknown algorithm: key.example.
│     ├─ getTSIG_lastRecordIsTSIG_returnsTSIGRecord() ✘ Unknown algorithm: key.example.
│     └─ getTSIG_nullHeader_throwsNPE() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (3):
  JUnit Jupiter:Message_getTSIG_Test:getTSIG_lastRecordIsTSIG_returnsInstance()
    MethodSource [className = 'org.xbill.DNS.Message_getTSIG_Test', methodName = 'getTSIG_lastRecordIsTSIG_returnsInstance', methodParameterTypes = '']
    => java.lang.RuntimeException: java.lang.NoSuchFieldException: type
       org.xbill.DNS.Message_getTSIG_Test.setPrivateField(Message_getTSIG_Test.java:24)
       org.xbill.DNS.Message_getTSIG_Test.getTSIG_lastRecordIsTSIG_returnsInstance(Message_getTSIG_Test.java:185)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchFieldException: type
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       org.xbill.DNS.Message_getTSIG_Test.setPrivateField(Message_getTSIG_Test.java:20)
       [...]
  JUnit Jupiter:Message_getTSIG_Test:getTSIG_multipleAdditionalRecords_lastIsTSIG_returnsTSIGRecord()
    MethodSource [className = 'org.xbill.DNS.Message_getTSIG_Test', methodName = 'getTSIG_multipleAdditionalRecords_lastIsTSIG_returnsTSIGRecord', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: key.example.
       org.xbill.DNS.TSIG.nameToAlgorithm(TSIG.java:125)
       org.xbill.DNS.TSIG.<init>(TSIG.java:195)
       org.xbill.DNS.Message_getTSIG_Test.getTSIG_multipleAdditionalRecords_lastIsTSIG_returnsTSIGRecord(Message_getTSIG_Test.java:99)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:Message_getTSIG_Test:getTSIG_lastRecordIsTSIG_returnsTSIGRecord()
    MethodSource [className = 'org.xbill.DNS.Message_getTSIG_Test', methodName = 'getTSIG_lastRecordIsTSIG_returnsTSIGRecord', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: key.example.
       org.xbill.DNS.TSIG.nameToAlgorithm(TSIG.java:125)
       org.xbill.DNS.TSIG.<init>(TSIG.java:195)
       org.xbill.DNS.Message_getTSIG_Test.getTSIG_lastRecordIsTSIG_returnsTSIGRecord(Message_getTSIG_Test.java:71)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]

Test run finished after 2208 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         7 tests found           ]
[         0 tests skipped         ]
[         7 tests started         ]
[         0 tests aborted         ]
[         4 tests successful      ]
[         3 tests failed          ]


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
 

api document of method getTSIG(): 

   * Returns the TSIG record from the ADDITIONAL section, if one is present.
   *
   * @see TSIGRecord
   * @see TSIG
   * @see Section
   

return type: 
org.xbill.DNS.TSIGRecord

dependent classes: 
class `org.xbill.DNS.Record`:
	dependent fields:
		int org.xbill.DNS.Record.type;
	
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	related functions:
		method `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.getTSIG()`;
		method `public Record getQuestion()` returns `org.xbill.DNS.Record`, related with `org.xbill.DNS.Message.getTSIG()`, api document: 
   * Returns the first record in the QUESTION section.
   *
   * @see Record
   * @see Section
   ;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.getTSIG()`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ADDITIONAL;
	
class `org.xbill.DNS.Type`:
	dependent fields:
		int org.xbill.DNS.Type.TSIG;
	
class `org.xbill.DNS.SetResponse`:
	related functions:
		method `public RRset getNS()` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Message.getTSIG()`, api document:  If the query hit a delegation point, return the NS set. ;
class `org.xbill.DNS.ExtendedResolver`:
	related functions:
		method `public Resolver getResolver(int)` returns `org.xbill.DNS.Resolver`, related with `org.xbill.DNS.Message.getTSIG()`, api document:  Returns the nth resolver used by this ExtendedResolver ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
