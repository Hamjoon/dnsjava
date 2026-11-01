```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link Message#setResolver(Resolver)}.
 *
 * The tests verify that the resolver is stored correctly and can be retrieved via
 * {@link Message#getResolver()}. They also ensure that passing {@code null} clears the resolver
 * without throwing an exception.
 */
@ExtendWith(MockitoExtension.class)
class Message_setResolver_Test {

    private Message message;

    @Mock
    private Resolver mockResolver; // Mockito will create a mock implementation

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor which creates a fresh Header and empty sections.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void setResolver_withNonNullResolver_storesResolver() {
        // GIVEN – a fresh Message instance and a mocked Resolver (provided by @Mock)
        assertTrue(message.getResolver().isEmpty(), "Resolver should initially be empty");

        // WHEN – setResolver is called with a non‑null resolver
        message.setResolver(mockResolver);

        // THEN – getResolver returns an Optional containing the same resolver instance
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after setting");
        assertSame(mockResolver, opt.get(), "Returned resolver should be the same instance that was set");
    }

    @Test
    void setResolver_withNullResolver_clearsResolver() {
        // GIVEN – a resolver has already been set
        message.setResolver(mockResolver);
        assertTrue(message.getResolver().isPresent(), "Precondition: resolver should be present");

        // WHEN – setResolver is called with null
        message.setResolver(null);

        // THEN – getResolver returns an empty Optional
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isEmpty(), "Resolver should be empty after setting null");
    }

    @Test
    void setResolver_doesNotAffectOtherMessageState() {
        // GIVEN – a message with a known header ID
        int originalId = 12345;
        Message msg = new Message(originalId);
        Header originalHeader = msg.getHeader();

        // WHEN – we set a resolver
        msg.setResolver(mockResolver);

        // THEN – header and its ID remain unchanged
        Header afterHeader = msg.getHeader();
        assertSame(originalHeader, afterHeader, "Header instance should remain the same");
        assertEquals(originalId, afterHeader.getID(), "Header ID should remain unchanged after setResolver");
    }
}
```