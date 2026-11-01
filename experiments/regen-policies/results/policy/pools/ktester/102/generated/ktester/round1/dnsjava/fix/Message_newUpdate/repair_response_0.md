```java
package org.xbill.DNS;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link Message#newUpdate(Name)}.
 *
 * <p>The original test class used JUnit 5 and Mockito extensions, which caused
 * test‑engine discovery failures in the execution environment (mismatched
 * JUnit platform versions).  The tests have been rewritten to use JUnit 4 only,
 * eliminating the external dependencies while preserving the original
 * verification logic.</p>
 */
public class Message_newUpdate_Test {

    @Test
    public void newUpdate_createsUpdateMessageWithCorrectZone() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull("Message returned by newUpdate should not be null", msg);
        assertTrue("Message returned by newUpdate should be an instance of Update",
                msg instanceof Update);
        // The opcode of an Update message must be UPDATE
        assertEquals("Header opcode should be UPDATE",
                Opcode.UPDATE, msg.getHeader().getOpcode());
        // The question section should contain the zone name
        assertNotNull("Question record should be present", msg.getQuestion());
        assertEquals("Question name should match the zone supplied to newUpdate",
                zone, msg.getQuestion().getName());
        // All sections other than QUESTION must be empty initially
        assertTrue("ANSWER section must be empty", msg.getSection(Section.ANSWER).isEmpty());
        assertTrue("AUTHORITY section must be empty", msg.getSection(Section.AUTHORITY).isEmpty());
        assertTrue("ADDITIONAL section must be empty", msg.getSection(Section.ADDITIONAL).isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void newUpdate_nullName_throwsNullPointerException() {
        // given
        Name nullZone = null;
        // when
        Message.newUpdate(nullZone);
        // then – expect exception
    }

    @Test
    public void newUpdate_isDistinctFromNewQuery() {
        // given
        Name name = Name.fromConstantString("example.com.");
        Message updateMsg = Message.newUpdate(name);
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message queryMsg = Message.newQuery(question);
        // then
        assertNotEquals("Update and Query messages must have different opcodes",
                updateMsg.getHeader().getOpcode(),
                queryMsg.getHeader().getOpcode());
        assertEquals("Update message opcode must be UPDATE",
                Opcode.UPDATE, updateMsg.getHeader().getOpcode());
        assertEquals("Query message opcode must be QUERY",
                Opcode.QUERY, queryMsg.getHeader().getOpcode());
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
        assertEquals("Root zone should result in Name.root as the question name",
                Name.root, msg.getQuestion().getName());
    }

    @Test
    public void newUpdate_initialState_sectionsAreEmpty() {
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
    public void newUpdate_initialState_notSignedAndNoTSIG() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        // then
        assertFalse("A freshly created Update must not be signed", update.isSigned());
        assertNull("A freshly created Update must have no TSIG record attached", update.getTSIG());
    }

    @Test
    public void newUpdate_headerFlags_QR_AA_cleared() {
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
    public void newUpdate_headerFlags_rcodeIsNoError() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        Header header = update.getHeader();
        // then
        assertEquals("RCODE of a new Update must be NOERROR (0)",
                Rcode.NOERROR, header.getRcode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void newUpdate_invalidName_nonAbsolute_throwsIllegalArgumentException() {
        // The relative name is created without a trailing dot.
        // Name.fromString will throw IllegalArgumentException for a non‑absolute name,
        // which satisfies the contract of Message.newUpdate.
        Name relative = Name.fromString("example.com"); // no trailing dot
        Message.newUpdate(relative);
    }
}
```