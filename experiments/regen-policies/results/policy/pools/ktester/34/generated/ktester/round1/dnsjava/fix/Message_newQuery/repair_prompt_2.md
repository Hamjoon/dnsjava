


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;

class Message_newQuery_Test {

    /* --------------------------------------------------------------
     *  Normal behaviour – various record types
     * -------------------------------------------------------------- */
    @Nested
    @DisplayName("newQuery – normal behaviour")
    class NormalBehaviour {

        /**
         * Supplies a few typical question records.
         */
        static Stream<Arguments> normalRecordProvider() throws Exception {
            return Stream.of(Arguments.of(new ARecord(Name.fromString("example.com."), DClass.IN, 3600, InetAddress.getByName("93.184.216.34"))), Arguments.of(new TXTRecord(Name.fromString("test.example."), DClass.IN, 300, "sample text")), Arguments.of(new MXRecord(Name.fromString("mail.example."), DClass.IN, 600, 10, Name.fromString("mx.example."))));
        }

        @ParameterizedTest(name = "[{index}] record type = {0}")
        @MethodSource("normalRecordProvider")
        @DisplayName("initialises header and QUESTION section correctly")
        void newQuery_initialisesHeaderAndQuestionSection(Record question) {
            // Act
            Message query = Message.newQuery(question);
            // Assert – header opcode must be QUERY
            assertEquals(Opcode.QUERY, query.getHeader().getOpcode(), "Header opcode should be QUERY");
            // Assert – recursion‑desired flag must be set
            assertTrue(query.getHeader().getFlag(Flags.RD), "Header should have the RD (recursion desired) flag set");
            // Assert – the question record must be stored and retrievable
            assertNotNull(query.getQuestion(), "Message.getQuestion() should not be null");
            assertEquals(question, query.getQuestion(), "Stored question should be identical to the supplied record");
            // Assert – QUESTION section contains exactly this record
            assertEquals(1, query.getSection(Section.QUESTION).size(), "QUESTION section should contain exactly one record");
            assertEquals(question, query.getSection(Section.QUESTION).get(0), "Record in QUESTION section should be the supplied record");
        }
    }

    /**
     * Verify that a normal query Message is correctly initialised.
     */
    @Test
    void newQuery_initialisesHeaderAndQuestionSection() {
        // Arrange – create a simple A record question
        Name name = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        // Act – build the query Message
        Message query = Message.newQuery(question);
        // Assert – header opcode must be QUERY
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(), "Header opcode should be QUERY");
        // Assert – recursion‑desired flag must be set
        assertTrue(query.getHeader().getFlag(Flags.RD), "Header should have the RD (recursion desired) flag set");
        // Assert – the question record must be stored
        assertNotNull(query.getQuestion(), "Message.getQuestion() should not be null");
        assertEquals(question, query.getQuestion(), "Stored question should be identical to the supplied record");
        // Assert – the QUESTION section must contain exactly this record
        assertEquals(1, query.getSection(Section.QUESTION).size(), "QUESTION section should contain exactly one record");
        assertEquals(question, query.getSection(Section.QUESTION).get(0), "Record in QUESTION section should be the supplied record");
    }

    /**
     * Verify that passing {@code null} as the record results in a {@link NullPointerException}.
     * The implementation does not guard against null, so the NPE is expected.
     */
    @Test
    void newQuery_nullRecord_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Message.newQuery(null), "Message.newQuery(null) should throw NullPointerException");
    }

    /**
     * Ensure that the generated Message does not contain any records in other sections.
     */
    @Test
    void newQuery_otherSectionsAreEmpty() {
        Name name = Name.fromConstantString("test.example.");
        Record question = Record.newRecord(name, Type.MX, DClass.IN);
        Message query = Message.newQuery(question);
        // Sections other than QUESTION must be empty
        assertTrue(query.getSection(Section.ANSWER).isEmpty(), "ANSWER section should be empty");
        assertTrue(query.getSection(Section.AUTHORITY).isEmpty(), "AUTHORITY section should be empty");
        assertTrue(query.getSection(Section.ADDITIONAL).isEmpty(), "ADDITIONAL section should be empty");
    }

    @Test
    @DisplayName("Record with null name causes NullPointerException")
    void newQuery_recordWithNullName_throwsNPE() {
        // Construction of a Record with a null name itself throws NPE in DNSJava,
        // but we assert that newQuery does not mask the problem.
        assertThrows(NullPointerException.class, () -> {
            Record bad = new ARecord(null, DClass.IN, 3600, InetAddress.getByName("93.184.216.34"));
            Message.newQuery(bad);
        });
    }

    /* --------------------------------------------------------------
     *  Header verification independent of record type
     * -------------------------------------------------------------- */
//     @Test
//     @DisplayName("header always initialised correctly irrespective of record type")
//     void newQuery_headerInitializationVerification() {
//         Record txt = new TXTRecord(Name.fromString("test.example."), DClass.IN, 300, "sample text");
//         Message query = Message.newQuery(txt);
//         assertEquals(Opcode.QUERY, query.getHeader().getOpcode(), "Header opcode must be QUERY");
//         assertTrue(query.getHeader().getFlag(Flags.RD), "Header must have RD flag set");
//     }

    /* --------------------------------------------------------------
     *  Independence of multiple invocations
     * -------------------------------------------------------------- */
//     @Test
//     @DisplayName("multiple invocations produce independent Message instances")
//     void newQuery_multipleInvocationsProduceIndependentMessages() {
//         Record mx = new MXRecord(Name.fromString("mail.example."), DClass.IN, 600, 10, Name.fromString("mx.example."));
//         Message first = Message.newQuery(mx);
//         Message second = Message.newQuery(mx);
//         // Different object references – no shared mutable state
//         assertNotSame(first, second, "Each call should return a distinct Message instance");
//         // Header settings must be identical
//         assertEquals(first.getHeader().getOpcode(), second.getHeader().getOpcode(), "Opcode should be identical");
//         assertEquals(first.getHeader().getFlag(Flags.RD), second.getHeader().getFlag(Flags.RD), "RD flag should be identical");
//         // QUESTION sections must contain equal records but be separate lists
//         assertEquals(first.getSection(Section.QUESTION), second.getSection(Section.QUESTION), "QUESTION sections should contain the same record");
//         assertNotSame(first.getSection(Section.QUESTION), second.getSection(Section.QUESTION), "QUESTION section lists must be distinct objects");
//     }

    /* --------------------------------------------------------------
     *  Unsupported record types in QUESTION section
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("OPTRecord is not allowed in the QUESTION section")
    void newQuery_optRecord_throwsIllegalArgumentException() {
        // OPTRecord is used for EDNS options and must not appear in QUESTION
        Record opt = new OPTRecord(4096, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> Message.newQuery(opt), "Adding an OPTRecord to the QUESTION section should throw IllegalArgumentException");
    }

//     @Test
//     @DisplayName("TSIGRecord is not allowed in the QUESTION section")
//     void newQuery_tsigRecord_throwsIllegalArgumentException() {
//         // Construct a minimal TSIGRecord (parameters chosen arbitrarily)
//         Record tsig = new TSIGRecord(Name.fromString("example.com."), DClass.ANY, 0L, // timeSigned
//         Name.fromString("hmac-md5.sig-alg.reg.int."), // fudge
//         new Date(), // mac
//         0, // originalID
//         new byte[] { 0x01, 0x02 }, // error
//         0, // otherData
//         0, new byte[] {});
//         assertThrows(IllegalArgumentException.class, () -> Message.newQuery(tsig), "Adding a TSIGRecord to the QUESTION section should throw IllegalArgumentException");
//     }
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
│  └─ Message_newQuery_Test ✔
│     ├─ Record with null name causes NullPointerException ✔
│     ├─ newQuery_nullRecord_throwsNPE() ✘ Message.newQuery(null) should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
│     ├─ newQuery_initialisesHeaderAndQuestionSection() ✔
│     ├─ OPTRecord is not allowed in the QUESTION section ✘ Adding an OPTRecord to the QUESTION section should throw IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ newQuery_otherSectionsAreEmpty() ✔
│     └─ newQuery – normal behaviour ✔
│        └─ initialises header and QUESTION section correctly ✔
│           ├─ [1] record type = example.com.  3600 IN A 93.184.216.34 ✔
│           ├─ [2] record type = test.example.  300 IN TXT "sample text" ✔
│           └─ [3] record type = mail.example.  600 IN MX 10 mx.example. ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:Message_newQuery_Test:newQuery_nullRecord_throwsNPE()
    MethodSource [className = 'org.xbill.DNS.Message_newQuery_Test', methodName = 'newQuery_nullRecord_throwsNPE', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Message.newQuery(null) should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_newQuery_Test.newQuery_nullRecord_throwsNPE(Message_newQuery_Test.java:82)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:Message_newQuery_Test:OPTRecord is not allowed in the QUESTION section
    MethodSource [className = 'org.xbill.DNS.Message_newQuery_Test', methodName = 'newQuery_optRecord_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Adding an OPTRecord to the QUESTION section should throw IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_newQuery_Test.newQuery_optRecord_throwsIllegalArgumentException(Message_newQuery_Test.java:149)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 101 ms
[         6 containers found      ]
[         0 containers skipped    ]
[         6 containers started    ]
[         0 containers aborted    ]
[         6 containers successful ]
[         0 containers failed     ]
[         8 tests found           ]
[         0 tests skipped         ]
[         8 tests started         ]
[         0 tests aborted         ]
[         6 tests successful      ]
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
 

api document of method newQuery(Record): 

   * Creates a new Message with a random Message ID suitable for sending as a query.
   *
   * @param r A record containing the question
   

return type: 
org.xbill.DNS.Message

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.QUESTION;
	
class `org.xbill.DNS.Message`:
	dependent fields:
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	
class `org.xbill.DNS.Opcode`:
	dependent fields:
		int org.xbill.DNS.Opcode.QUERY;
	
class `org.xbill.DNS.Flags`:
	dependent fields:
		byte org.xbill.DNS.Flags.RD;
	
class `org.xbill.DNS.tools.update`:
	related functions:
		method `public Message newMessage()` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method ` byte[] buildErrorMessage(Header, int, Record)` returns `byte[]`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.ZoneTransferIn`:
	related functions:
		method `private void sendQuery()` returns `void`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.dnssec.SMessage`:
	related functions:
		method `public Message getMessage()` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newQuery(Record)`, api document: 
   * Gets this message as a standard DNSJAVA message.
   *
   * @return This message as a standard DNSJAVA message.
   ;
class `org.xbill.DNS.Update`:
	related functions:
		method `private void newPrereq(Record)` returns `void`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.SimpleResolver`:
	related functions:
		method `private Message sendAXFR(Message)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newQuery(Record)`;
		method `private void applyEDNS(Message)` returns `void`, related with `org.xbill.DNS.Message.newQuery(Record)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
