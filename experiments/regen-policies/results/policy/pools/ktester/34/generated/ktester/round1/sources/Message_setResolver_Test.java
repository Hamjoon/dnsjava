package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_setResolver_Test {

    private Message message;

    @Mock
    private Resolver // Mockito will create a mock implementation
    mockResolver;

    /**
     * A dummy abstract class that implements {@link Resolver}.
     */
    abstract static class DummyResolver implements Resolver {
    }

    /**
     * A dummy abstract subclass used to verify that subclass instances are handled.
     */
    abstract static class CustomResolver implements Resolver {
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

    /* --------------------------------------------------------------------- */
    private static Stream<Resolver> dummyResolverProvider() {
        return Stream.of(mock(DummyResolver.class), mock(DummyResolver.class));
    }

    @Test
    void setResolver_withNonNullResolver_storesResolver() {
        // GIVEN – a fresh Message instance and a mocked Resolver (provided by @Mock)
        assertTrue(message.getResolver().isEmpty(), "Resolver should initially be empty");
        // WHEN – setResolver is called with a non‑null resolver
        Resolver mockResolver = mock(Resolver.class);
        message.setResolver(mockResolver);
        // THEN – getResolver returns an Optional containing the same resolver instance
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after setting");
        assertSame(mockResolver, opt.get(), "Returned resolver should be the same instance that was set");
    }

    @Test
    void setResolver_withNullResolver_clearsResolver() {
        // GIVEN – a resolver has already been set
        Resolver mockResolver = mock(Resolver.class);
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
        Resolver mockResolver = mock(Resolver.class);
        msg.setResolver(mockResolver);
        // THEN – header and its ID remain unchanged
        Header afterHeader = msg.getHeader();
        assertSame(originalHeader, afterHeader, "Header instance should remain the same");
        assertEquals(originalId, afterHeader.getID(), "Header ID should remain unchanged after setResolver");
    }

    /**
     * Overwrites an existing resolver with a different one.
     */
    @Test
    void setResolver_overwritesWithDifferentResolver() {
        // first resolver
        Resolver first = mock(DummyResolver.class);
        // second resolver
        Resolver second = mock(DummyResolver.class);
        // set first, then second
        message.setResolver(first);
        assertSame(first, message.getResolver().orElseThrow(), "First resolver should be stored");
        message.setResolver(second);
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after second set");
        assertSame(second, opt.get(), "Second resolver must overwrite the first");
    }

    /**
     * Two independent Message instances keep independent resolver state.
     */
    @Test
    void setResolver_isIndependentBetweenMessages() {
        Message messageA = new Message();
        Message messageB = new Message();
        Resolver resolverA = mock(DummyResolver.class);
        messageA.setResolver(resolverA);
        // messageB never gets a resolver
        assertTrue(messageB.getResolver().isEmpty(), "Resolver of messageB should stay empty");
        assertTrue(messageA.getResolver().isPresent(), "Resolver of messageA should be present");
        assertSame(resolverA, messageA.getResolver().orElseThrow(), "MessageA should return the resolver that was set");
    }

    /**
     * Setting a resolver, then clearing it with null.
     */
    @Test
    void setResolver_nullAfterNonNull_clearsResolver() {
        Resolver resolver = mock(DummyResolver.class);
        message.setResolver(resolver);
        assertTrue(message.getResolver().isPresent(), "Precondition: resolver present");
        // clear with null
        message.setResolver(null);
        assertTrue(message.getResolver().isEmpty(), "Resolver should be empty after setting null");
    }

    /**
     * Setting the same resolver instance twice is idempotent.
     */
    @Test
    void setResolver_idempotent_sameInstance() {
        Resolver sameResolver = mock(DummyResolver.class);
        message.setResolver(sameResolver);
        // second call with same instance
        message.setResolver(sameResolver);
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present");
        assertSame(sameResolver, opt.get(), "Stored resolver must be the same instance after idempotent set");
    }

    /**
     * Resolver stays unchanged after unrelated Message operations.
     */
    @Test
    void setResolver_afterOtherOperations_resolverUnaffected() {
        Resolver resolver = mock(DummyResolver.class);
        message.setResolver(resolver);
        // invoke unrelated methods
        Record dummyRecord = mock(Record.class);
        message.addRecord(dummyRecord, Section.ANSWER);
        // just retrieve header
        message.getHeader();
        // convert to wire format (may throw)
        byte[] wire = message.toWire();
        // verify resolver unchanged
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should still be present");
        assertSame(resolver, opt.get(), "Resolver instance must remain unchanged");
        assertNotNull(wire, "Wire representation should not be null");
    }

    /**
     * Storing a subclass of Resolver works as expected.
     */
    @Test
    void setResolver_subclassResolver_storesSubclassInstance() {
        // mock a concrete subclass of Resolver
        Resolver customResolver = mock(CustomResolver.class);
        message.setResolver(customResolver);
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present");
        assertSame(customResolver, opt.get(), "Stored resolver must be the same subclass instance");
        assertTrue(opt.get() instanceof CustomResolver, "Returned resolver should be an instance of CustomResolver");
    }

    /**
     * After multiple sets, a final null clears the resolver.
     */
    @Test
    void setResolver_overwriteWithNullAfterMultipleSets_clearsResolver() {
        Resolver first = mock(DummyResolver.class);
        Resolver second = mock(DummyResolver.class);
        message.setResolver(first);
        message.setResolver(second);
        // now clear
        message.setResolver(null);
        assertTrue(message.getResolver().isEmpty(), "Resolver must be empty after final null assignment");
    }
}
