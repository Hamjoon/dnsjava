package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Message_setResolver_Test {

    /**
     * Message instance used in the tests.
     */
    private Message message;

    /**
     * A mock Resolver used to verify that the resolver is stored correctly.
     */
    @Mock
    private Resolver mockResolver;

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor – it creates a fresh Message with a random Header.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required but keeps the pattern consistent.
        message = null;
    }

    @Test
    void setResolver_storesResolverAndIsReturnedByGetResolver() {
        // Given: a fresh Message and a mocked Resolver (provided by @Mock).
        // When: the resolver is set on the message.
        message.setResolver(mockResolver);
        // Then: getResolver should return an Optional containing the same instance.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after setResolver");
        assertSame(mockResolver, opt.get(), "Returned resolver should be the same instance that was set");
    }

    @Test
    void setResolver_overwritesPreviousResolver() {
        // Given: a message with an initial resolver.
        Resolver firstResolver = mock(Resolver.class);
        message.setResolver(firstResolver);
        assertEquals(Optional.of(firstResolver), message.getResolver(), "Initial resolver should be stored correctly");
        // When: a second resolver is set.
        message.setResolver(mockResolver);
        // Then: the message should now return the second resolver.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should still be present after overwriting");
        assertSame(mockResolver, opt.get(), "Resolver should be overwritten with the new instance");
        assertNotEquals(firstResolver, opt.get(), "Old resolver should no longer be returned");
    }

    @Test
    void setResolver_withNullResolver_resultsInEmptyOptional() {
        // When: null is passed to setResolver.
        message.setResolver(null);
        // Then: getResolver should return an empty Optional (the implementation stores the value directly).
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
