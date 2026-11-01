
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test {

    @Test
    void newUpdate_createsUpdateMessageWithCorrectZone() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull("Message returned by newUpdate should not be null", msg);
        assertTrue("Message returned by newUpdate should be an instance of Update", msg instanceof Update);
        // The opcode of an Update message must be UPDATE
        assertEquals("Header opcode should be UPDATE", Opcode.UPDATE, msg.getHeader().getOpcode());
        // The question section should contain the zone name
        assertNotNull("Question record should be present", msg.getQuestion());
        assertEquals("Question name should match the zone supplied to newUpdate", zone, msg.getQuestion().getName());
        // All sections other than QUESTION must be empty initially
        assertTrue("ANSWER section must be empty", msg.getSection(Section.ANSWER).isEmpty());
        assertTrue("AUTHORITY section must be empty", msg.getSection(Section.AUTHORITY).isEmpty());
        assertTrue("ADDITIONAL section must be empty", msg.getSection(Section.ADDITIONAL).isEmpty());
    }

    @Test
    void newUpdate_nullName_throwsNullPointerException() {
        // given
        Name nullZone = null;
        // when
        Message.newUpdate(nullZone);
        // then – expect exception
    }

    @Test
    void newUpdate_isDistinctFromNewQuery() {
        // given
        Name name = Name.fromConstantString("example.com.");
        Message updateMsg = Message.newUpdate(name);
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message queryMsg = Message.newQuery(question);
        // then
        assertNotEquals("Update and Query messages must have different opcodes", updateMsg.getHeader().getOpcode(), queryMsg.getHeader().getOpcode());
        assertEquals("Update message opcode must be UPDATE", Opcode.UPDATE, updateMsg.getHeader().getOpcode());
        assertEquals("Query message opcode must be QUERY", Opcode.QUERY, queryMsg.getHeader().getOpcode());
    }

    @ParameterizedTest(name = "[{index}] zone = {0}")
    @CsvSource({ // root zone
    "'example.com.', true", // root zone
    "'sub.domain.example.org.', true", "'., true'" })
    void newUpdate_validNames(String zoneString, boolean isRoot) {
        // given
        Name zone = Name.fromConstantString(zoneString);
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull(msg, "Message should not be null");
        assertTrue(msg instanceof Update, "Message should be an Update instance");
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode must be UPDATE");
        assertNotNull(msg.getQuestion(), "Question record must be present");
        if (isRoot) {
            assertEquals(Name.root, msg.getQuestion().getName(), "Root zone should result in Name.root as the question name");
        } else {
            assertEquals(zone, msg.getQuestion().getName(), "Question name must match the supplied zone");
        }
    }

    @Test
    void newUpdate_initialState_sectionsAreEmpty() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        // then
        assertTrue("ANSWER section must be empty initially", update.getSection(Section.ANSWER).isEmpty());
        assertTrue("AUTHORITY section must be empty initially", update.getSection(Section.AUTHORITY).isEmpty());
        assertTrue("ADDITIONAL section must be empty initially", update.getSection(Section.ADDITIONAL).isEmpty());
    }

    @Test
    void newUpdate_initialState_notSignedAndNoTSIG() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        // then
        assertFalse("A freshly created Update must not be signed", update.isSigned());
        assertNull("A freshly created Update must have no TSIG record attached", update.getTSIG());
    }

    @Test
    void newUpdate_headerFlags_QR_AA_cleared() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        Header header = update.getHeader();
        // then
        assertFalse("QR flag (response) must be cleared for a new Update", header.getFlag(Flags.QR));
        assertFalse("AA flag (authoritative answer) must be cleared for a new Update", header.getFlag(Flags.AA));
    }

    @Test
    void newUpdate_headerFlags_rcodeIsNoError() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        Header header = update.getHeader();
        // then
        assertEquals("RCODE of a new Update must be NOERROR (0)", Rcode.NOERROR, header.getRcode());
    }

    @Test
    void newUpdate_invalidName_nonAbsolute_throwsIllegalArgumentException() {
        // The relative name is created without a trailing dot.
        // Name.fromString will throw IllegalArgumentException for a non‑absolute name,
        // which satisfies the contract of Message.newUpdate.
        // no trailing dot
        Name relative = Name.fromString("example.com");
        Message.newUpdate(relative);
    }

    @Test
    public void newUpdate_validName_nonRoot() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull("Message should not be null", msg);
        assertTrue("Message should be an Update instance", msg instanceof Update);
        assertEquals("Header opcode must be UPDATE", Opcode.UPDATE, msg.getHeader().getOpcode());
        assertNotNull("Question record must be present", msg.getQuestion());
        assertEquals("Question name must match the supplied zone", zone, msg.getQuestion().getName());
    }

    @Test
    public void newUpdate_validName_root() {
        // given
        Name zone = Name.root;
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull("Message should not be null", msg);
        assertTrue("Message should be an Update instance", msg instanceof Update);
        assertEquals("Header opcode must be UPDATE", Opcode.UPDATE, msg.getHeader().getOpcode());
        assertNotNull("Question record must be present", msg.getQuestion());
        assertEquals("Root zone should result in Name.root as the question name", Name.root, msg.getQuestion().getName());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:3: error: cannot find symbol
import static org.junit.Assert.*;
                       ^
  symbol:   class Assert
  location: package org.junit
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:4: error: cannot find symbol
import org.junit.Test;
                ^
  symbol:   class Test
  location: package org.junit
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method newUpdate(Name)': '\n   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled\n   * in.\n   *\n   * @param zone The zone to be updated\n   ', 'return type': 'org.xbill.DNS.Message', 'dependent classes': 'class `org.xbill.DNS.Name`:\n\t\nclass `org.xbill.DNS.Message`:\n\trelated functions:\n\t\tmethod `public static Message newUpdate(Name)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document: \n   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled\n   * in.\n   *\n   * @param zone The zone to be updated\n   ;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document:  Converts a numeric extended flag into a String ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


