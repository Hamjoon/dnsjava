


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
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test {

    /**
     * Zone name used for the tests
     */
    private static Name zoneName;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // a simple absolute name that can be used as a DNS zone
        zoneName = Name.fromConstantString("example.com.");
    }

    /**
     * Supplies a stream of valid zone names for parameterized testing.
     * Currently includes a regular zone and the root zone.
     */
    private static Stream<Name> validZoneProvider() {
        return Stream.of(Name.fromConstantString("example.com."), Name.root);
    }

    /**
     * Verify that {@link Message#newUpdate(Name)} creates an {@link Update}
     * instance with the correct opcode and an empty set of sections.
     */
    @Test
    void newUpdate_createsUpdateMessage() {
        Message msg = Message.newUpdate(zoneName);
        // The concrete class must be Update (a subclass of Message)
        assertTrue(msg instanceof Update, "Message.newUpdate should return an Update instance");
        // Header opcode must be UPDATE
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode should be UPDATE for an Update message");
        // The question section should contain the zone name supplied
        Record question = msg.getQuestion();
        assertNotNull(question, "Update message should have a question record");
        assertEquals(zoneName, question.getName(), "Question name of the Update message must match the zone argument");
        // All sections (ANSWER, AUTHORITY, ADDITIONAL) must be empty initially
        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(), "Section " + i + " should be empty for a newly created Update message");
        }
    }

    /**
     * Verify that passing {@code null} as the zone name results in a
     * {@link NullPointerException}. The {@code Message.newUpdate} method does not
     * perform a null‑check itself, but the {@link Update} constructor (which is
     * invoked internally) will throw NPE when it tries to use the argument.
     */
    @Test
    void newUpdate_nullZone_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Message.newUpdate(null));
    }

    /**
     * Verify that the root zone (represented by {@link Name#root}) is accepted.
     * The created {@link Update} must contain a question record whose name is
     * the root name (".").
     */
    @Test
    void newUpdate_rootZone_createsUpdateMessage() {
        Message msg = Message.newUpdate(Name.root);
        assertTrue(msg instanceof Update, "Message.newUpdate should return an Update instance for the root zone");
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode should be UPDATE for an Update message with root zone");
        Record question = msg.getQuestion();
        assertNotNull(question, "Update message for root zone should have a question record");
        assertEquals(Name.root, question.getName(), "Question name must be the root name (.) for a root‑zone Update message");
    }

    /**
     * Verify that a wildcard zone name (e.g. "*.example.com.") is rejected.
     * According to the DNS dynamic update specification a zone cannot be a
     * wildcard, so the {@link Update} constructor is expected to throw an
     * {@link IllegalArgumentException}.
     */
    @Test
    void newUpdate_wildcardZone_throwsIllegalArgumentException()throws Exception {
        Name wildcard = Name.fromString("*.example.com.");
        assertThrows(IllegalArgumentException.class, () -> Message.newUpdate(wildcard), "A wildcard zone name should cause IllegalArgumentException");
    }

    /**
     * Parameterized test that runs the same assertions for each valid zone name.
     * Demonstrates how additional cases could be added without duplicating code.
     */
    @ParameterizedTest
    @MethodSource("validZoneProvider")
    void newUpdate_validZones_createsUpdateMessage(Name zone) {
        Message msg = Message.newUpdate(zone);
        assertTrue(msg instanceof Update, "Message.newUpdate should return an Update instance");
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode should be UPDATE for an Update message");
        Record question = msg.getQuestion();
        assertNotNull(question, "Update message should have a question record");
        assertEquals(zone, question.getName(), "Question name must match the supplied zone");
        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(), "Section " + i + " should be empty for a newly created Update message");
        }
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/commit-survival/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended

╷
├─ JUnit Jupiter ✔
│  └─ Message_newUpdate_Test ✔
│     ├─ newUpdate_rootZone_createsUpdateMessage() ✔
│     ├─ newUpdate_validZones_createsUpdateMessage(Name) ✔
│     │  ├─ [1] example.com. ✘ Section 0 should be empty for a newly created Update message ==> expected: <true> but was: <false>
│     │  └─ [2] . ✘ Section 0 should be empty for a newly created Update message ==> expected: <true> but was: <false>
│     ├─ newUpdate_nullZone_throwsNPE() ✔
│     ├─ newUpdate_createsUpdateMessage() ✘ Section 0 should be empty for a newly created Update message ==> expected: <true> but was: <false>
│     └─ newUpdate_wildcardZone_throwsIllegalArgumentException() ✘ A wildcard zone name should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (4):
  JUnit Jupiter:Message_newUpdate_Test:newUpdate_validZones_createsUpdateMessage(Name):[1] example.com.
    MethodSource [className = 'org.xbill.DNS.Message_newUpdate_Test', methodName = 'newUpdate_validZones_createsUpdateMessage', methodParameterTypes = 'org.xbill.DNS.Name']
    => org.opentest4j.AssertionFailedError: Section 0 should be empty for a newly created Update message ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_newUpdate_Test.newUpdate_validZones_createsUpdateMessage(Message_newUpdate_Test.java:106)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_newUpdate_Test:newUpdate_validZones_createsUpdateMessage(Name):[2] .
    MethodSource [className = 'org.xbill.DNS.Message_newUpdate_Test', methodName = 'newUpdate_validZones_createsUpdateMessage', methodParameterTypes = 'org.xbill.DNS.Name']
    => org.opentest4j.AssertionFailedError: Section 0 should be empty for a newly created Update message ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_newUpdate_Test.newUpdate_validZones_createsUpdateMessage(Message_newUpdate_Test.java:106)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_newUpdate_Test:newUpdate_createsUpdateMessage()
    MethodSource [className = 'org.xbill.DNS.Message_newUpdate_Test', methodName = 'newUpdate_createsUpdateMessage', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Section 0 should be empty for a newly created Update message ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       org.xbill.DNS.Message_newUpdate_Test.newUpdate_createsUpdateMessage(Message_newUpdate_Test.java:50)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:Message_newUpdate_Test:newUpdate_wildcardZone_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.Message_newUpdate_Test', methodName = 'newUpdate_wildcardZone_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: A wildcard zone name should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.Message_newUpdate_Test.newUpdate_wildcardZone_throwsIllegalArgumentException(Message_newUpdate_Test.java:89)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 720 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[         6 tests found           ]
[         0 tests skipped         ]
[         6 tests started         ]
[         0 tests aborted         ]
[         2 tests successful      ]
[         4 tests failed          ]


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
 

api document of method newUpdate(Name): 

   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled
   * in.
   *
   * @param zone The zone to be updated
   

return type: 
org.xbill.DNS.Message

dependent classes: 
class `org.xbill.DNS.Name`:
	
class `org.xbill.DNS.Message`:
	related functions:
		method `public static Message newUpdate(Name)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document: 
   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled
   * in.
   *
   * @param zone The zone to be updated
   ;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
