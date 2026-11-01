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
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message instance before each test
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid interaction leakage between tests
        reset(mockResolver);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    @Test
    void getResolver_whenNotSet_returnsEmptyOptional() {
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isEmpty(), "Expected Optional to be empty when resolver is not set");
    }

    @Test
    void getResolver_whenSet_returnsOptionalContainingResolver() {
        // Arrange
        message.setResolver(mockResolver);

        // Act
        Optional<Resolver> opt = message.getResolver();

        // Assert
        assertTrue(opt.isPresent(), "Expected Optional to contain a resolver after setting it");
        assertSame(mockResolver, opt.get(), "Expected the resolver returned to be the same instance that was set");
    }

    @Test
    void getResolver_afterSettingNull_returnsEmptyOptional() {
        // Arrange: explicitly set null (allowed by the setter)
        message.setResolver(null);

        // Act
        Optional<Resolver> opt = message.getResolver();

        // Assert
        assertTrue(opt.isEmpty(), "Expected Optional to be empty after setting resolver to null");
    }
}
```