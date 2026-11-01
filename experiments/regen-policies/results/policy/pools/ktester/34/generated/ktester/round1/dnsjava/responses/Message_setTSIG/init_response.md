```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#setTSIG(TSIG)}.
 *
 * <p>The tests verify that a TSIG record is correctly added to the
 * additional section of a {@link Message} and that the internal state
 * reflects a signed message.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_Test {

    private Message message;
    private TSIG tsigKey;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a fresh Message for each test
        message = new Message();

        // Build a simple TSIG key.
        // The algorithm name "hmac-md5" is recognised by the library.
        // The key string must be valid Base64; "ABCDABCDABCDABCDABCDAB==" is a harmless example.
        tsigKey = new TSIG("hmac-md5", "example.com.", "ABCDABCDABCDABCDABCDAB==");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed
    }

    @Test
    void setTSIG_addsTSIGRecordToAdditionalSection() {
        // Pre‑condition: no TSIG present
        assertNull(message.getTSIG(), "Message should not have a TSIG before setTSIG");

        // Act
        message.setTSIG(tsigKey);

        // Verify that a TSIGRecord is now attached to the message
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "Message.getTSIG() should return a non‑null record after setTSIG");

        // The TSIGRecord must be present in the ADDITIONAL section
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        assertTrue(additional.contains(tsigRecord),
                "Additional section should contain the generated TSIGRecord");

        // Verify that the TSIGRecord fields match the key we supplied
        assertEquals(tsigKey.getName(), tsigRecord.getName(),
                "TSIGRecord name should match the TSIG key name");
        assertEquals(tsigKey.getAlgorithm(), tsigRecord.getAlgorithm(),
                "TSIGRecord algorithm should match the TSIG key algorithm");

        // Header count for the ADDITIONAL section should have been incremented
        assertEquals(1, message.getHeader().getCount(Section.ADDITIONAL),
                "Header count for ADDITIONAL should be 1 after adding TSIG");
    }

    @Test
    void setTSIG_multipleCallsReplaceExistingTSIG() throws IOException {
        // First call
        message.setTSIG(tsigKey);
        TSIGRecord first = message.getTSIG();
        assertNotNull(first, "First TSIGRecord must be present");

        // Create a second distinct key
        TSIG otherKey = new TSIG("hmac-sha256", "other.example.", "YWJjZGVmZ2hpamtsbW5vcA==");

        // Second call – should replace the previous TSIG
        message.setTSIG(otherKey);
        TSIGRecord second = message.getTSIG();
        assertNotNull(second, "Second TSIGRecord must be present");
        assertNotSame(first, second, "A new TSIGRecord should replace the old one");

        // Verify that only one TSIGRecord exists in the ADDITIONAL section
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        long tsigCount = additional.stream().filter(r -> r instanceof TSIGRecord).count();
        assertEquals(1, tsigCount, "Only one TSIGRecord should be present after replacement");
    }

    @Test
    void setTSIG_nullKeyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> message.setTSIG(null),
                "Calling setTSIG with a null key should throw NPE");
    }
}
```