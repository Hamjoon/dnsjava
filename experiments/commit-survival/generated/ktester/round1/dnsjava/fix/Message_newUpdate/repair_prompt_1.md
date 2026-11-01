
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test extends Message {

    /**
     * Zone name used for the tests
     */
    private static Name zoneName;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.*;

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
    void newUpdate_wildcardZone_throwsIllegalArgumentException() throws Exception {
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

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:10: error: illegal start of type
import org.mockito.junit.jupiter.MockitoExtension;
^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:10: error: <identifier> expected
import org.mockito.junit.jupiter.MockitoExtension;
                                                 ^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:11: error: illegal start of type
import org.junit.jupiter.params.ParameterizedTest;
^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:11: error: <identifier> expected
import org.junit.jupiter.params.ParameterizedTest;
                                                 ^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:12: error: illegal start of type
import org.junit.jupiter.api.BeforeAll;
^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:12: error: <identifier> expected
import org.junit.jupiter.api.BeforeAll;
                                      ^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:13: error: illegal start of type
import org.junit.jupiter.api.Test;
^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:13: error: <identifier> expected
import org.junit.jupiter.api.Test;
                                 ^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:14: error: illegal start of type
import java.util.stream.Stream;
^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:14: error: <identifier> expected
import java.util.stream.Stream;
                              ^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:15: error: illegal start of type
import org.junit.jupiter.params.provider.MethodSource
^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:15: error: <identifier> expected
import org.junit.jupiter.params.provider.MethodSource
                                                     ^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:16: error: <identifier> expected
import org.junit.jupiter.api.extension.ExtendWith;
                                                 ^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:17: error: illegal start of type
import org.junit.jupiter.api.*;
^
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:17: error: <identifier> expected
import org.junit.jupiter.api.*;
                             ^
15 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method newUpdate(Name)': '\n   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled\n   * in.\n   *\n   * @param zone The zone to be updated\n   ', 'return type': 'org.xbill.DNS.Message', 'dependent classes': 'class `org.xbill.DNS.Name`:\n\t\nclass `org.xbill.DNS.Message`:\n\trelated functions:\n\t\tmethod `public static Message newUpdate(Name)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document: \n   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled\n   * in.\n   *\n   * @param zone The zone to be updated\n   ;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document:  Converts a numeric extended flag into a String ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


