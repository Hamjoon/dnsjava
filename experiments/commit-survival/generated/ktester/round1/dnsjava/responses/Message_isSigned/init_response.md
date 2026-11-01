```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class) // No mocks are required but kept for consistency with template
class Message_isSigned_Test {

    private Message msg;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test; the default constructor sets tsigState to TSIG_UNSIGNED (0)
        msg = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference – not strictly necessary but mirrors the template
        msg = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    @Test
    void isSigned_unsigned_returnsFalse() {
        // By default the constructor leaves tsigState == TSIG_UNSIGNED (0)
        assertFalse(msg.isSigned(), "Message should not be signed when tsigState is TSIG_UNSIGNED");
    }

    @Test
    void isSigned_intermediate_returnsFalse() {
        // TSIG_INTERMEDIATE is defined as 2 in Message
        msg.tsigState = Message.TSIG_INTERMEDIATE;
        assertFalse(msg.isSigned(), "Message should not be signed when tsigState is TSIG_INTERMEDIATE");
    }

    @Test
    void isSigned_signed_returnsTrue() {
        // TSIG_SIGNED is defined as 3 in Message
        msg.tsigState = Message.TSIG_SIGNED;
        assertTrue(msg.isSigned(), "Message should be signed when tsigState is TSIG_SIGNED");
    }

    @Test
    void isSigned_verified_returnsTrue() {
        // TSIG_VERIFIED is defined as 1 in Message
        msg.tsigState = Message.TSIG_VERIFIED;
        assertTrue(msg.isSigned(), "Message should be signed when tsigState is TSIG_VERIFIED");
    }

    @Test
    void isSigned_failed_returnsTrue() {
        // TSIG_FAILED is defined as 4 in Message
        msg.tsigState = Message.TSIG_FAILED;
        assertTrue(msg.isSigned(), "Message should be considered signed when tsigState is TSIG_FAILED");
    }
}
```