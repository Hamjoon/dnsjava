


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_findRRset_2_Test {

    private Message message;

    private Name nameA;

    private Name nameB;

    @BeforeEach
    void setupBeforeEach() throws TextParseException, UnknownHostException {
        // Create a fresh Message for every test
        message = new Message();
        // Two distinct names – one that will be used for matching records,
        // another that will stay absent.
        nameA = Name.fromConstantString("example.com.");
        nameB = Name.fromConstantString("absent.com.");
        // Populate the message with a few records in different sections.
        // ANSWER section – a matching A record.
        Record aRecord = new ARecord(nameA, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        message.addRecord(aRecord, Section.ANSWER);
        // AUTHORITY section – a matching NS record (different type).
        Record nsRecord = new NSRecord(nameA, DClass.IN, 3600, Name.fromConstantString("ns1.example.com."));
        message.addRecord(nsRecord, Section.AUTHORITY);
        // ADDITIONAL section – a matching TXT record (different type).
        Record txtRecord = new TXTRecord(nameA, DClass.IN, 3600, "sample txt");
        message.addRecord(txtRecord, Section.ADDITIONAL);
    }

    @Test
    void findRRset_matchesInAnswerSection() {
        // The A record added in the ANSWER section should be found.
        assertTrue(message.findRRset(nameA, Type.A), "findRRset should return true when a matching RRset exists in the ANSWER section");
    }

    @Test
    void findRRset_matchesInAuthoritySection() {
        // The NS record added in the AUTHORITY section should be found.
        assertTrue(message.findRRset(nameA, Type.NS), "findRRset should return true when a matching RRset exists in the AUTHORITY section");
    }

    @Test
    void findRRset_matchesInAdditionalSection() {
        // The TXT record added in the ADDITIONAL section should be found.
        assertTrue(message.findRRset(nameA, Type.TXT), "findRRset should return true when a matching RRset exists in the ADDITIONAL section");
    }

    @Test
    void findRRset_returnsFalseWhenNoMatch() {
        // No record with nameB has been added, so any type lookup must fail.
        assertFalse(message.findRRset(nameB, Type.A), "findRRset should return false when no matching RRset exists in any section");
    }

    @Test
    void findRRset_returnsFalseWhenTypeDoesNotMatch() {
        // nameA exists but we query for a type that was never added (CNAME).
        assertFalse(message.findRRset(nameA, Type.CNAME), "findRRset should return false when the name exists but the type does not match any RRset");
    }

    @Test
    void findRRset_isShortCircuitingAcrossSections() {
        // Ensure that the method checks all sections: we add a matching record only
        // to the ADDITIONAL section and verify that the result is true even though
        // the ANSWER and AUTHORITY sections do not contain that type.
        Name onlyInAdditional = Name.fromConstantString("only.additional.");
        Record mxRecord = new MXRecord(onlyInAdditional, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com."));
        // Add only to ADDITIONAL
        message.addRecord(mxRecord, Section.ADDITIONAL);
        // No MX record exists in ANSWER or AUTHORITY, but should still be found.
        assertTrue(message.findRRset(onlyInAdditional, Type.MX), "findRRset should locate a matching RRset even if it appears only in the ADDITIONAL section");
    }

    @Test
    @DisplayName("Short‑circuit when first section (ANSWER) matches")
    void shortCircuit_firstSectionMatches() throws TextParseException {
        Message spy = spy(new Message());
        Name name = Name.fromString("example.com.");
        // Stub only the ANSWER check to return true.
        doReturn(true).when(spy).findRRset(name, Type.A, Section.ANSWER);
        // The other sections should never be invoked.
        doReturn(false).when(spy).findRRset(name, Type.A, Section.AUTHORITY);
        doReturn(false).when(spy).findRRset(name, Type.A, Section.ADDITIONAL);
        assertTrue(spy.findRRset(name, Type.A), "Overall result must be true when first section matches");
        // Verify short‑circuit: only the first call happened.
        verify(spy, times(1)).findRRset(name, Type.A, Section.ANSWER);
        verify(spy, never()).findRRset(name, Type.A, Section.AUTHORITY);
        verify(spy, never()).findRRset(name, Type.A, Section.ADDITIONAL);
    }

    @Test
    @DisplayName("Short‑circuit when second section (AUTHORITY) matches")
    void shortCircuit_secondSectionMatches() throws TextParseException {
        Message spy = spy(new Message());
        Name name = Name.fromString("example.org.");
        // ANSWER returns false, AUTHORITY returns true.
        doReturn(false).when(spy).findRRset(name, Type.MX, Section.ANSWER);
        doReturn(true).when(spy).findRRset(name, Type.MX, Section.AUTHORITY);
        // ADDITIONAL must not be called.
        doReturn(false).when(spy).findRRset(name, Type.MX, Section.ADDITIONAL);
        assertTrue(spy.findRRset(name, Type.MX), "Overall result must be true when second section matches");
        verify(spy, times(1)).findRRset(name, Type.MX, Section.ANSWER);
        verify(spy, times(1)).findRRset(name, Type.MX, Section.AUTHORITY);
        verify(spy, never()).findRRset(name, Type.MX, Section.ADDITIONAL);
    }

    @Test
    @DisplayName("Short‑circuit when third section (ADDITIONAL) matches")
    void shortCircuit_thirdSectionMatches() throws TextParseException {
        Message spy = spy(new Message());
        Name name = Name.fromString("example.net.");
        // First two sections false, third true.
        doReturn(false).when(spy).findRRset(name, Type.SRV, Section.ANSWER);
        doReturn(false).when(spy).findRRset(name, Type.SRV, Section.AUTHORITY);
        doReturn(true).when(spy).findRRset(name, Type.SRV, Section.ADDITIONAL);
        assertTrue(spy.findRRset(name, Type.SRV), "Overall result must be true when third section matches");
        verify(spy, times(1)).findRRset(name, Type.SRV, Section.ANSWER);
        verify(spy, times(1)).findRRset(name, Type.SRV, Section.AUTHORITY);
        verify(spy, times(1)).findRRset(name, Type.SRV, Section.ADDITIONAL);
    }

    @Test
    @DisplayName("All sections return false – no short‑circuit")
    void shortCircuit_allSectionsFalse() throws TextParseException {
        Message spy = spy(new Message());
        Name name = Name.fromString("nonexistent.com.");
        // Stub every overloaded call to return false.
        doReturn(false).when(spy).findRRset(name, Type.TXT, Section.ANSWER);
        doReturn(false).when(spy).findRRset(name, Type.TXT, Section.AUTHORITY);
        doReturn(false).when(spy).findRRset(name, Type.TXT, Section.ADDITIONAL);
        assertFalse(spy.findRRset(name, Type.TXT), "Overall result must be false when all sections are false");
        // All three overloads must have been invoked exactly once.
        verify(spy, times(1)).findRRset(name, Type.TXT, Section.ANSWER);
        verify(spy, times(1)).findRRset(name, Type.TXT, Section.AUTHORITY);
        verify(spy, times(1)).findRRset(name, Type.TXT, Section.ADDITIONAL);
    }

    @Test
    @DisplayName("Null name argument should throw NullPointerException")
    void nullNameArgument() {
        assertThrows(NullPointerException.class, () -> message.findRRset(null, Type.A), "Passing a null Name must result in NullPointerException");
    }

    @Test
    @DisplayName("Negative type argument should simply return false")
    void negativeTypeArgument() {
        // No exception – the method should treat it as a non‑matching type.
        assertFalse(message.findRRset(nameA, -1), "Negative type values must not match any RRset and should return false");
    }

    @Test
    @DisplayName("Zero type argument (invalid) should return false")
    void zeroTypeArgument() {
        assertFalse(message.findRRset(nameA, 0), "Type value 0 is not a defined DNS RR type and must yield false");
    }

    @Test
    @DisplayName("Maximum valid type (65535) with no matching RRset should return false")
    void maxTypeNoMatch() {
        assertFalse(message.findRRset(nameA, 65535), "Even the highest possible type value should return false when no RRset exists");
    }

    @Test
    @DisplayName("Root name with SOA record in AUTHORITY section")
    void rootNameWithSOARecord() throws TextParseException {
        // Build a minimal SOA record for the DNS root.
        // represents "."
        Name root = Name.root;
        Name mName = Name.fromString("ns.root.");
        Name rName = Name.fromString("hostmaster.root.");
        SOARecord soa = new SOARecord(root, DClass.IN, 3600, mName, rName, 2023101501L, 7200, 3600, 1209600, 3600);
        Message msg = new Message();
        msg.addRecord(soa, Section.AUTHORITY);
        assertTrue(msg.findRRset(root, Type.SOA), "A SOA record for the root should be found in the AUTHORITY section");
    }
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
│  └─ Message_findRRset_2_Test ✔
│     ├─ Negative type argument should simply return false ✔
│     ├─ findRRset_returnsFalseWhenNoMatch() ✔
│     ├─ findRRset_matchesInAnswerSection() ✔
│     ├─ findRRset_matchesInAdditionalSection() ✔
│     ├─ Root name with SOA record in AUTHORITY section ✔
│     ├─ Null name argument should throw NullPointerException ✔
│     ├─ Zero type argument (invalid) should return false ✔
│     ├─ findRRset_isShortCircuitingAcrossSections() ✔
│     ├─ All sections return false – no short‑circuit ✔
│     ├─ findRRset_matchesInAuthoritySection() ✔
│     ├─ Maximum valid type (65535) with no matching RRset should return false ✔
│     ├─ findRRset_returnsFalseWhenTypeDoesNotMatch() ✔
│     ├─ Short‑circuit when first section (ANSWER) matches ✘ 
│     │        Unnecessary stubbings detected.
│     │        Clean & maintainable test code requires zero unnecessary code.
│     │        Following stubbings are unnecessary (click to navigate to relevant line of code):
│     │          1. -> at org.xbill.DNS.Message_findRRset_2_Test.shortCircuit_firstSectionMatches(Message_findRRset_2_Test.java:91)
│     │          2. -> at org.xbill.DNS.Message_findRRset_2_Test.shortCircuit_firstSectionMatches(Message_findRRset_2_Test.java:92)
│     │        Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
│     ├─ Short‑circuit when second section (AUTHORITY) matches ✘ 
│     │        Unnecessary stubbings detected.
│     │        Clean & maintainable test code requires zero unnecessary code.
│     │        Following stubbings are unnecessary (click to navigate to relevant line of code):
│     │          1. -> at org.xbill.DNS.Message_findRRset_2_Test.shortCircuit_secondSectionMatches(Message_findRRset_2_Test.java:109)
│     │        Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
│     └─ Short‑circuit when third section (ADDITIONAL) matches ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:Message_findRRset_2_Test:Short‑circuit when first section (ANSWER) matches
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'shortCircuit_firstSectionMatches', methodParameterTypes = '']
    => org.mockito.exceptions.misusing.UnnecessaryStubbingException: 
Unnecessary stubbings detected.
Clean & maintainable test code requires zero unnecessary code.
Following stubbings are unnecessary (click to navigate to relevant line of code):
  1. -> at org.xbill.DNS.Message_findRRset_2_Test.shortCircuit_firstSectionMatches(Message_findRRset_2_Test.java:91)
  2. -> at org.xbill.DNS.Message_findRRset_2_Test.shortCircuit_firstSectionMatches(Message_findRRset_2_Test.java:92)
Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
       org.mockito.junit.jupiter.MockitoExtension.afterEach(MockitoExtension.java:192)
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
  JUnit Jupiter:Message_findRRset_2_Test:Short‑circuit when second section (AUTHORITY) matches
    MethodSource [className = 'org.xbill.DNS.Message_findRRset_2_Test', methodName = 'shortCircuit_secondSectionMatches', methodParameterTypes = '']
    => org.mockito.exceptions.misusing.UnnecessaryStubbingException: 
Unnecessary stubbings detected.
Clean & maintainable test code requires zero unnecessary code.
Following stubbings are unnecessary (click to navigate to relevant line of code):
  1. -> at org.xbill.DNS.Message_findRRset_2_Test.shortCircuit_secondSectionMatches(Message_findRRset_2_Test.java:109)
Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
       org.mockito.junit.jupiter.MockitoExtension.afterEach(MockitoExtension.java:192)
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

Test run finished after 857 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        15 tests found           ]
[         0 tests skipped         ]
[        15 tests started         ]
[         0 tests aborted         ]
[        13 tests successful      ]
[         2 tests failed          ]


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
 

api document of method findRRset(Name, int): 

   * Determines if an RRset with the given name and type is already present in any section.
   *
   * @see RRset
   * @see Section
   

return type: 
boolean

dependent classes: 
class `org.xbill.DNS.Name`:
	
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ANSWER;
		int org.xbill.DNS.Section.AUTHORITY;
		int org.xbill.DNS.Section.ADDITIONAL;
	
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method `private void addAdditional(Message, int)` returns `void`, related with `org.xbill.DNS.Message.findRRset(Name, int)`;
class `org.xbill.DNS.Message`:
	related functions:
		method `public OPTRecord getOPT()` returns `org.xbill.DNS.OPTRecord`, related with `org.xbill.DNS.Message.findRRset(Name, int)`, api document: 
   * Returns the OPT record from the ADDITIONAL section, if one is present.
   *
   * @see OPTRecord
   * @see Section
   ;
		method `public boolean findRecord(Record)` returns `boolean`, related with `org.xbill.DNS.Message.findRRset(Name, int)`, api document: 
   * Determines if the given record is already present in any section.
   *
   * @see Record
   * @see Section
   ;
		method `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.findRRset(Name, int)`;
class `org.xbill.DNS.Cache`:
	related functions:
		method `private int getCred(int, boolean)` returns `int`, related with `org.xbill.DNS.Message.findRRset(Name, int)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
