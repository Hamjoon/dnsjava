package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test {

    private static Name zoneName;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // Create a reusable zone name for the generic tests
        zoneName = Name.fromConstantString("example.com.");
    }

    /**
     * Supplies a stream of valid {@link Name} instances.
     */
    private static Stream<Arguments> validZoneProvider() throws TextParseException {
        // 1. Typical zone
        Name typical = Name.fromString("example.com.");
        // 2. Root zone (".")
        Name root = Name.root;
        // 3. Maximum‑length domain name (255 octets). Constructed as:
        //    "a" + "b".repeat(250) + ".com."
        StringBuilder sb = new StringBuilder();
        // first label of length 1
        sb.append('a');
        // second label of length 250 (still valid)
        sb.append("b".repeat(250));
        // final labels
        sb.append(".com.");
        Name maxLength = Name.fromString(sb.toString());
        return Stream.of(Arguments.of(typical), Arguments.of(root), Arguments.of(maxLength));
    }

    /**
     * Supplies a stream of {@link Name} instances that violate DNS naming rules.
     */
    private static Stream<Arguments> invalidZoneProvider() throws TextParseException {
        // 1. Label longer than 63 octets
        Name overLongLabel = Name.fromString("a".repeat(64) + ".example.com.");
        // 2. Wildcard zone (not allowed for updates)
        Name wildcard = Name.fromString("*.example.com.");
        // 3. Empty label (two consecutive dots)
        Name emptyLabel = Name.fromString("example..com.");
        return Stream.of(Arguments.of(overLongLabel), Arguments.of(wildcard), Arguments.of(emptyLabel));
    }

    /**
     * Verify that {@code Message.newUpdate(Name)} returns an {@link Update}
     * instance whose header opcode is set to {@link Opcode#UPDATE}.
     */
    @Test
    void newUpdate_returnsUpdateWithUpdateOpcode() {
        Message m = Message.newUpdate(zoneName);
        assertNotNull(m, "Message.newUpdate should not return null");
        assertTrue(m instanceof Update, "Message.newUpdate should return an instance of Update");
        // The header of an Update message must have the UPDATE opcode.
        int opcode = m.getHeader().getOpcode();
        assertEquals(Opcode.UPDATE, opcode, "Header opcode of an Update message must be UPDATE");
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
        assertEquals(zoneName, question.getName(), "Question name should be the zone supplied to newUpdate");
        // The Update class creates a SOA‑type question for the zone.
        assertEquals(Type.SOA, question.getType(), "Question type for an Update should be SOA");
    }

    /**
     * Ensure that passing {@code null} as the zone name results in a
     * {@link NullPointerException}.  The contract of {@code newUpdate} does not
     * allow a null zone.
     */
    @Test
    void newUpdate_nullZone_throwsException() {
        assertThrows(NullPointerException.class, () -> Message.newUpdate(null), "Message.newUpdate(null) should throw NullPointerException");
    }

    /**
     * Successful creation of an Update message for a variety of valid zone
     * names, including the root zone and a name that reaches the DNS length
     * limit (255 octets).
     */
    @ParameterizedTest(name = "[{index}] zone = {0}")
    @MethodSource("validZoneProvider")
    void newUpdate_validZones_createsUpdate(Name zone) {
        Message m = Message.newUpdate(zone);
        assertNotNull(m, "Message.newUpdate should not return null");
        assertTrue(m instanceof Update, "Message.newUpdate should return an instance of Update");
        // Verify that the question section reflects the supplied zone.
        Record question = m.getQuestion();
        assertNotNull(question, "Update message should contain a question record");
        assertEquals(zone, question.getName(), "Question name should match the zone supplied to newUpdate");
        assertEquals(Type.SOA, question.getType(), "Question type for an Update should be SOA");
        // Header opcode must be UPDATE for all Update messages.
        assertEquals(Opcode.UPDATE, m.getHeader().getOpcode(), "Header opcode must be UPDATE");
    }

    /**
     * Invalid zone names must cause {@link IllegalArgumentException} to be
     * thrown.  The factory method validates the supplied {@link Name}
     * according to DNS rules (no over‑long labels, no wildcards, no empty
     * labels, etc.).
     */
    @ParameterizedTest(name = "[{index}] zone = {0}")
    @MethodSource("invalidZoneProvider")
    void newUpdate_invalidZones_throwsIllegalArgumentException(Name zone) {
        assertThrows(IllegalArgumentException.class, () -> Message.newUpdate(zone), "Message.newUpdate should reject invalid zone: " + zone);
    }
}
