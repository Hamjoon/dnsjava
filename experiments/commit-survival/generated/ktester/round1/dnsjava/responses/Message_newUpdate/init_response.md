```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test {

    /** Zone name used for the tests */
    private static Name zoneName;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // a simple absolute name that can be used as a DNS zone
        zoneName = Name.fromConstantString("example.com.");
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed
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
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(),
                "Header opcode should be UPDATE for an Update message");

        // The question section should contain the zone name supplied
        Record question = msg.getQuestion();
        assertNotNull(question, "Update message should have a question record");
        assertEquals(zoneName, question.getName(),
                "Question name of the Update message must match the zone argument");

        // All sections (ANSWER, AUTHORITY, ADDITIONAL) must be empty initially
        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(),
                    "Section " + i + " should be empty for a newly created Update message");
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
}
```