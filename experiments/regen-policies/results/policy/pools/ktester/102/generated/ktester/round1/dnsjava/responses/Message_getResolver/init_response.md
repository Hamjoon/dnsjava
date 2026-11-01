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
    private Resolver mockResolver;   // Mockito will create a mock Resolver

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message instance before each test
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed
    }

    @Test
    void getResolver_whenNotSet_returnsEmptyOptional() {
        // Given: a newly constructed Message with no resolver set
        // When
        Optional<Resolver> opt = message.getResolver();
        // Then
        assertTrue(opt.isEmpty(), "Expected Optional.empty when resolver is not set");
    }

    @Test
    void getResolver_whenSet_returnsOptionalContainingResolver() {
        // Given: a Message with a mocked Resolver set
        message.setResolver(mockResolver);
        // When
        Optional<Resolver> opt = message.getResolver();
        // Then
        assertTrue(opt.isPresent(), "Expected Optional to contain a Resolver");
        assertSame(mockResolver, opt.get(), "Expected the same Resolver instance that was set");
    }

    @Test
    void getResolver_multipleCalls_consistentResult() {
        // Given
        message.setResolver(mockResolver);
        // When
        Optional<Resolver> firstCall = message.getResolver();
        Optional<Resolver> secondCall = message.getResolver();
        // Then
        assertTrue(firstCall.isPresent() && secondCall.isPresent(),
                "Both calls should return a present Optional");
        assertSame(firstCall.get(), secondCall.get(),
                "Both Optional instances should wrap the same Resolver");
    }

    @Test
    void setResolver_overwritesPreviousResolver() {
        // Given
        Resolver firstResolver = mock(Resolver.class);
        Resolver secondResolver = mock(Resolver.class);
        message.setResolver(firstResolver);
        // When
        message.setResolver(secondResolver);
        // Then
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after second setResolver");
        assertSame(secondResolver, opt.get(),
                "The resolver returned should be the last one set");
    }
}
```