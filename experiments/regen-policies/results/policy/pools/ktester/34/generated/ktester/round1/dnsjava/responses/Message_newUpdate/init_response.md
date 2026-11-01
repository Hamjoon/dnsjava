```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test {

    private static Name zoneName;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // Create a reusable zone name for the tests
        zoneName = Name.fromConstantString("example.com.");
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
    }

    /**
     * Verify that {@code Message.newUpdate(Name)} returns an {@link Update}
     * instance whose header opcode is set to {@link Opcode#UPDATE}.
     */
    @Test
    void newUpdate_returnsUpdateWithUpdateOpcode() {
        Message m = Message.newUpdate(zoneName);
        assertNotNull(m, "Message.newUpdate should not return null");
        assertTrue(m instanceof Update,
                "Message.newUpdate should return an instance of Update");

        // The header of an Update message must have the UPDATE opcode.
        Opcode opcode = m.getHeader().getOpcode();
        assertEquals(Opcode.UPDATE, opcode,
                "Header opcode of an Update message must be UPDATE");
    }

    /**
     * Verify that the zone name supplied to {@code newUpdate} is stored as the
     * question section of the resulting message.  The question record should
     * have the zone name as its name and the type {@link Type#SOA} (the type
     * used by {@link Update} for the zone record).
     */
    @Test
    void newUpdate_containsZoneInQuestionSection() {
        Message m = Message.newUpdate(zoneName);
        Record question = m.getQuestion();
        assertNotNull(question, "Update message should contain a question record");
        assertEquals(zoneName, question.getName(),
                "Question name should be the zone supplied to newUpdate");
        // The Update class creates a SOA‑type question for the zone.
        assertEquals(Type.SOA, question.getType(),
                "Question type for an Update should be SOA");
    }

    /**
     * Ensure that passing {@code null} as the zone name results in a
     * {@link NullPointerException}.  The contract of {@code newUpdate} does not
     * allow a null zone.
     */
    @Test
    void newUpdate_nullZone_throwsException() {
        assertThrows(NullPointerException.class,
                () -> Message.newUpdate(null),
                "Message.newUpdate(null) should throw NullPointerException");
    }
}
```