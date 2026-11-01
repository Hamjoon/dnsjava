```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_numBytes_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verify that a freshly created Message (no‑arg constructor) reports a size of 0.
     */
    @Test
    void numBytes_defaultConstructor() {
        Message m = new Message();
        assertEquals(0, m.numBytes(),
                "Message created with the default constructor should have size 0");
    }

    /**
     * Verify that a Message created with the ID‑based constructor also reports size 0.
     */
    @Test
    void numBytes_idConstructor() {
        Message m = new Message(0x1234);
        assertEquals(0, m.numBytes(),
                "Message created with the id‑based constructor should have size 0");
    }

    /**
     * Verify that when a Message is constructed from a wire‑format byte array,
     * {@code numBytes()} returns the exact number of bytes read (i.e. the length of the array).
     */
    @Test
    void numBytes_fromWire() throws IOException {
        // Build a simple query message
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);

        // Serialize the message to wire format
        byte[] wire = query.toWire(); // uses default max length (65535)

        // Parse the wire representation back into a Message
        Message parsed = new Message(wire);

        // The size stored inside the parsed message should equal the wire length
        assertEquals(wire.length, parsed.numBytes(),
                "Parsed Message should report size equal to the wire array length");
    }

    /**
     * Verify that cloning a Message preserves the size reported by {@code numBytes()}.
     */
    @Test
    void numBytes_clonePreservesSize() throws IOException {
        // Create a message with a question and one additional record
        Name qname = Name.fromConstantString("example.org.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message original = Message.newQuery(question);
        original.addRecord(new TXTRecord(qname, DClass.IN, 3600, "test"), Section.ADDITIONAL);

        // Serialize and re‑parse to ensure the internal size field is set
        byte[] wire = original.toWire();
        Message parsed = new Message(wire);

        // Clone the parsed message
        Message clone = parsed.clone();

        assertEquals(parsed.numBytes(), clone.numBytes(),
                "Cloned Message should have the same size as the original parsed Message");
    }
}
```