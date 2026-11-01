```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#toWire(DNSOutput)}. The tests create a {@link Message},
 * add records, serialize it with {@code toWire}, then deserialize the resulting
 * byte array and verify that the original content is preserved.
 *
 * The test class lives in the same package as {@link Message} so that the
 * package‑private {@code toWire(DNSOutput)} method is directly accessible.
 */
@ExtendWith(MockitoExtension.class)
class Message_toWire_Test {

    /** A reusable {@link DNSOutput} instance used by the tests. */
    private DNSOutput dnsOutput;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        dnsOutput = new DNSOutput(); // default size is sufficient for our tiny messages
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    /**
     * Serialises an empty {@link Message} and checks that a round‑trip via the
     * {@link Message#Message(byte[])} constructor yields an equivalent object.
     */
    @Test
    void toWire_emptyMessage_roundTrip() throws IOException {
        Message empty = new Message();               // header with random ID, no records
        empty.toWire(dnsOutput);                     // invoke the package‑private method
        byte[] wire = dnsOutput.toByteArray();

        // The wire format must be parsable
        Message parsed = new Message(wire);

        // Header IDs should match (the random ID is stored in both objects)
        assertEquals(empty.getHeader().getID(), parsed.getHeader().getID(),
                "Header ID should survive a round‑trip");

        // All sections must be empty
        for (int i = 0; i < 4; i++) {
            assertTrue(parsed.getSection(i).isEmpty(),
                    "Section " + i + " should be empty after round‑trip");
        }

        // The total size must not exceed the DNS maximum length
        assertTrue(wire.length <= Message.MAXLENGTH,
                "Serialized message must respect MAXLENGTH");
    }

    /**
     * Builds a message containing a question, an answer and an additional OPT record,
     * serialises it with {@code toWire}, deserialises the result and verifies that the
     * records are present in the correct sections and retain their data.
     */
    @Test
    void toWire_withRecords_roundTrip() throws Exception {
        // --- Build the original message -------------------------------------------------
        Name name = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = new Message();
        msg.addRecord(question, Section.QUESTION);

        // Answer: an A record pointing to 127.0.0.1
        ARecord aRecord = new ARecord(name, DClass.IN, 300,
                InetAddress.getByName("127.0.0.1"));
        msg.addRecord(aRecord, Section.ANSWER);

        // Additional: an OPT record (EDNS)
        OPTRecord opt = new OPTRecord(4096, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        // --- Serialise ---------------------------------------------------------------
        msg.toWire(dnsOutput);
        byte[] wire = dnsOutput.toByteArray();

        // --- Deserialise -------------------------------------------------------------
        Message parsed = new Message(wire);

        // Verify header ID is preserved
        assertEquals(msg.getHeader().getID(), parsed.getHeader().getID(),
                "Header ID should be identical after round‑trip");

        // Verify question section
        List<Record> qSec = parsed.getSection(Section.QUESTION);
        assertEquals(1, qSec.size(), "Exactly one question record expected");
        assertEquals(question.getName(), qSec.get(0).getName(),
                "Question name should match");
        assertEquals(question.getType(), qSec.get(0).getType(),
                "Question type should match");

        // Verify answer section
        List<Record> aSec = parsed.getSection(Section.ANSWER);
        assertEquals(1, aSec.size(), "Exactly one answer record expected");
        assertTrue(aSec.get(0) instanceof ARecord, "Answer should be an ARecord");
        ARecord parsedA = (ARecord) aSec.get(0);
        assertEquals(aRecord.getAddress(), parsedA.getAddress(),
                "ARecord address should survive round‑trip");

        // Verify additional section (OPT)
        List<Record> addSec = parsed.getSection(Section.ADDITIONAL);
        assertEquals(1, addSec.size(), "Exactly one additional record expected");
        assertTrue(addSec.get(0) instanceof OPTRecord, "Additional should be an OPTRecord");
        OPTRecord parsedOpt = (OPTRecord) addSec.get(0);
        assertEquals(opt.getPayloadSize(), parsedOpt.getPayloadSize(),
                "OPT payload size should be preserved");
    }

    /**
     * Helper method to obtain the underlying byte array from a {@link DNSOutput}.
     * The {@code DNSOutput} class does not expose the array directly, but it provides
     * a {@code toByteArray()} method in the upstream library. If such a method does
     * not exist, we fall back to reflection (which is safe in test code).
     */
    private static byte[] toByteArray(DNSOutput out) {
        try {
            // The library defines a public method; use it if present.
            return out.getClass().getMethod("toByteArray").invoke(out);
        } catch (Exception e) {
            // Fallback: use reflection to read the private field.
            try {
                java.lang.reflect.Field f = out.getClass().getDeclaredField("array");
                f.setAccessible(true);
                byte[] arr = (byte[]) f.get(out);
                int pos = (int) out.getClass().getDeclaredField("pos").get(out);
                byte[] result = new byte[pos];
                System.arraycopy(arr, 0, result, 0, pos);
                return result;
            } catch (Exception ex) {
                throw new RuntimeException("Unable to extract byte array from DNSOutput", ex);
            }
        }
    }
}
```