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
 * Unit tests for {@link Message#getResolver()}.
 *
 * <p>These tests verify the contract of {@code getResolver()} in the following situations:
 *
 * <ul>
 *   <li>When the {@code Message} is newly created and no resolver has been set.</li>
 *   <li>After a resolver is injected via {@code setResolver(Resolver)}.</li>
 *   <li>When the resolver is explicitly cleared by calling {@code setResolver(null)}.</li>
 *   <li>That repeated calls return consistent {@link Optional} instances.</li>
 *   <li>That a later call to {@code setResolver} overwrites any previous resolver.</li>
 * </ul>
 *
 * <p>Mockito is used to create a mock {@link Resolver} instance; no real network operations are
 * performed.
 */
@ExtendWith(MockitoExtension.class)
class Message_getResolver_Test {

    private Message message;

    @Mock
    private Resolver mockResolver;   // Mockito creates a mock Resolver

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
    @DisplayName("getResolver() returns empty Optional when resolver has never been set")
    void getResolver_whenNotSet_returnsEmptyOptional() {
        // Given: a newly constructed Message with no resolver set
        // When
        Optional<Resolver> opt = message.getResolver();
        // Then
        assertTrue(opt.isEmpty(),
                "Expected Optional.empty when resolver is not set");
    }

    @Test
    @DisplayName("getResolver() returns Optional containing the resolver after setResolver")
    void getResolver_whenSet_returnsOptionalContainingResolver() {
        // Given: a Message with a mocked Resolver set
        message.setResolver(mockResolver);
        // When
        Optional<Resolver> opt = message.getResolver();
        // Then
        assertTrue(opt.isPresent(),
                "Expected Optional to contain a Resolver");
        assertSame(mockResolver, opt.get(),
                "Expected the same Resolver instance that was set");
    }

    @Test
    @DisplayName("getResolver() returns empty Optional after resolver is cleared with setResolver(null)")
    void getResolver_afterClearingResolver_returnsEmptyOptional() {
        // Given: a resolver is first set and then cleared
        message.setResolver(mockResolver);
        message.setResolver(null); // explicit clear
        // When
        Optional<Resolver> opt = message.getResolver();
        // Then
        assertTrue(opt.isEmpty(),
                "Expected Optional.empty after resolver has been cleared");
    }

    @Test
    @DisplayName("Multiple calls to getResolver() return consistent results")
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
    @DisplayName("setResolver() overwrites any previously set resolver")
    void setResolver_overwritesPreviousResolver() {
        // Given two distinct mock resolvers
        Resolver firstResolver = mock(Resolver.class);
        Resolver secondResolver = mock(Resolver.class);
        message.setResolver(firstResolver);
        // When the second resolver is set
        message.setResolver(secondResolver);
        // Then the getter should return the second resolver
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(),
                "Resolver should be present after second setResolver");
        assertSame(secondResolver, opt.get(),
                "The resolver returned should be the last one set");
    }
}
```