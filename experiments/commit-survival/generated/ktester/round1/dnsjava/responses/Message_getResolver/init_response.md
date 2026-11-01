```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_getResolver_Test {

    private Message message;

    @Mock
    private Resolver mockResolver;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message instance for each test
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void getResolver_whenNotSet_returnsEmptyOptional() {
        // Given: a newly created Message with no resolver set
        // When
        Optional<Resolver> opt = message.getResolver();
        // Then
        assertNotNull(opt, "Optional should never be null");
        assertTrue(opt.isEmpty(), "Resolver should be empty when not set");
    }

    @Test
    void getResolver_whenSet_returnsOptionalContainingResolver() {
        // Given: a Message with a mock Resolver injected
        message.setResolver(mockResolver);
        // When
        Optional<Resolver> opt = message.getResolver();
        // Then
        assertNotNull(opt, "Optional should never be null");
        assertTrue(opt.isPresent(), "Resolver should be present after being set");
        assertSame(mockResolver, opt.get(), "Returned resolver should be the same instance that was set");
    }

    @Test
    void getResolver_afterChangingResolver_returnsNewResolver() {
        // Given: a Message with an initial resolver
        Resolver firstResolver = mock(Resolver.class);
        Resolver secondResolver = mock(Resolver.class);
        message.setResolver(firstResolver);
        // When: we replace it with another resolver
        message.setResolver(secondResolver);
        // Then
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after being set");
        assertSame(secondResolver, opt.get(), "Returned resolver should reflect the most recently set instance");
    }

    @Test
    void getResolver_afterSettingNull_returnsEmptyOptional() {
        // Given: a Message that previously had a resolver
        message.setResolver(mockResolver);
        // When: we explicitly set the resolver to null
        message.setResolver(null);
        // Then
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isEmpty(), "Resolver should be empty after being set to null");
    }
}
```