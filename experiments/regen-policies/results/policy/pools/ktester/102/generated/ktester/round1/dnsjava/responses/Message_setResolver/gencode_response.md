```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#setResolver(Resolver)} and the corresponding {@link Message#getResolver()}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>storing a non‑null resolver,
 *   <li>clearing the resolver with {@code null},
 *   <li>overwriting an existing resolver,
 *   <li>ensuring that {@code setResolver} does not invoke any method on the supplied resolver.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_setResolver_Test {

    /** Fresh Message instance for each test. */
    private Message message;

    /** A mock Resolver injected by Mockito – used in the first two tests. */
    @Mock
    private Resolver mockResolver;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // The default constructor creates a Message with a random Header.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly necessary.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    @Test
    void setResolver_storesResolverAndIsReturnedByGetResolver() {
        // GIVEN a fresh Message and a mocked Resolver (provided by @Mock).

        // WHEN the resolver is set on the message.
        message.setResolver(mockResolver);

        // THEN getResolver should return an Optional containing the same instance.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after setResolver");
        assertSame(mockResolver, opt.get(), "Returned resolver should be the same instance that was set");
    }

    @Test
    void setResolver_overwritesPreviousResolver() {
        // GIVEN a message with an initial resolver.
        Resolver firstResolver = mock(Resolver.class);
        message.setResolver(firstResolver);
        assertEquals(Optional.of(firstResolver), message.getResolver(),
                "Initial resolver should be stored correctly");

        // WHEN a second resolver is set.
        message.setResolver(mockResolver);

        // THEN the message should now return the second resolver.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should still be present after overwriting");
        assertSame(mockResolver, opt.get(), "Resolver should be overwritten with the new instance");
        assertNotEquals(firstResolver, opt.get(), "Old resolver should no longer be returned");
    }

    @Test
    void setResolver_withNullResolver_resultsInEmptyOptional() {
        // WHEN null is passed to setResolver.
        message.setResolver(null);

        // THEN getResolver should return an empty Optional.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isEmpty(), "Resolver Optional should be empty when null is set");
    }

    @Test
    void setResolver_doesNotInteractWithResolver() {
        // GIVEN a freshly created mock Resolver that records interactions.
        Resolver interactionMock = mock(Resolver.class, "resolverF");

        // WHEN the resolver is set.
        message.setResolver(interactionMock);

        // THEN no method on the resolver should have been called.
        verifyNoInteractions(interactionMock);
    }
}
```