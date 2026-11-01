```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xbill.DNS.utils.base64;

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
     * Verify that a freshly created Message reports a size of 0 bytes.
     */
    @Test
    void defaultMessage_hasZeroNumBytes() {
        Message m = new Message();               // uses the no‑arg constructor
        assertEquals(0, m.numBytes(),
                "A newly constructed Message should have size 0");
    }

    /**
     * Verify that a Message constructed from a raw DNS wire format reports the
     * correct number of bytes (the length of the supplied byte array).
     */
    @Test
    void messageFromByteArray_reportsCorrectNumBytes() throws Exception {
        // This is the same base64 payload used in the upstream MessageTest.
        byte[] wire = base64.fromString(
                "EEuBgAABAAEABAAIA3d3dwZnb29nbGUDY29tAAABAAHADAABAAEAAAAaAASO+rokwBAAAgABAAFHCwAGA25zMcAQwBAAAgABAAFHCwAGA25zNMAQwBAAAgABAAFHCwAGA25zM8AQwBAAAgABAAFHCwAGA25zMsAQwDwAAQABAADObwAE2O8gCsByAAEAAQABrVEABNjvIgrAYAABAAEAAVqZAATY7yQKwE4AAQABAAK9RQAE2O8mCsA8ABwAAQAD4a0AECABSGBIAgAyAAAAAAAAAArAcgAcAAEAAtDgABAgAUhgSAIANAAAAAAAAAAKwGAAHAABAACSagAQIAFIYEgCADYAAAAAAAAACsBOABwAAQAErVoAECABSGBIAgA4AAAAAAAAAAo="
        );

        Message m = new Message(wire);
        assertEquals(wire.length, m.numBytes(),
                "Message.numBytes() should equal the length of the wire‑format byte array");
    }
}
```