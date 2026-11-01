```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#newUpdate(Name)}.
 *
 * <p>The method should create an {@link Update} instance (a subclass of {@link Message})
 * whose header opcode is set to {@link Opcode#UPDATE} and whose question section
 * contains the supplied zone name.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void newUpdate_createsUpdateMessageWithCorrectZone() {
        // given
        Name zone = Name.fromConstantString("example.com.");

        // when
        Message msg = Message.newUpdate(zone);

        // then
        assertNotNull(msg, "Message returned by newUpdate should not be null");
        assertTrue(msg instanceof Update,
                "Message returned by newUpdate should be an instance of Update");
        // The opcode of an Update message must be UPDATE
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(),
                "Header opcode should be UPDATE");
        // The question section should contain the zone name
        assertNotNull(msg.getQuestion(), "Question record should be present");
        assertEquals(zone, msg.getQuestion().getName(),
                "Question name should match the zone supplied to newUpdate");
        // All sections other than QUESTION must be empty initially
        assertTrue(msg.getSection(Section.ANSWER).isEmpty(),
                "ANSWER section must be empty");
        assertTrue(msg.getSection(Section.AUTHORITY).isEmpty(),
                "AUTHORITY section must be empty");
        assertTrue(msg.getSection(Section.ADDITIONAL).isEmpty(),
                "ADDITIONAL section must be empty");
    }

    @Test
    void newUpdate_nullName_throwsNullPointerException() {
        // given
        Name nullZone = null;

        // when / then
        assertThrows(NullPointerException.class,
                () -> Message.newUpdate(nullZone),
                "Calling newUpdate with a null Name should throw NPE");
    }

    @Test
    void newUpdate_isDistinctFromNewQuery() {
        // given
        Name name = Name.fromConstantString("example.com.");
        Message updateMsg = Message.newUpdate(name);
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message queryMsg = Message.newQuery(question);

        // when / then
        assertNotEquals(updateMsg.getHeader().getOpcode(), queryMsg.getHeader().getOpcode(),
                "Update and Query messages must have different opcodes");
        assertEquals(Opcode.UPDATE, updateMsg.getHeader().getOpcode(),
                "Update message opcode must be UPDATE");
        assertEquals(Opcode.QUERY, queryMsg.getHeader().getOpcode(),
                "Query message opcode must be QUERY");
    }
}
```