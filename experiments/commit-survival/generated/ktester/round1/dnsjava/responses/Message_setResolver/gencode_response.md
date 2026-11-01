```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#setResolver(Resolver)}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>the resolver is stored correctly and wrapped in an {@link Optional};
 *   <li>setting {@code null} clears the resolver;
 *   <li>subsequent calls overwrite the previous resolver;
 *   <li>calling the method with the same instance does not change internal state;
 *   <li>the method does not interfere with other {@link Message} state such as the {@link Header}.
 * </ul>
 *
 * <p>All tests use JUnit 5 and Mockito.  A {@link Resolver} mock is injected via {@code @Mock},
 * and additional dummy resolvers are created with {@code Mockito.mock(Resolver.class)} when
 * distinct instances are required.
 */
@ExtendWith(MockitoExtension.class)
class Message_setResolver_Test {

    /** Fresh {@link Message} instance for each test. */
    private Message message;

    /** A mock {@link Resolver} that can be injected via {@link Message#setResolver(Resolver)}. */
    @Mock
    private Resolver mockResolver;

    /* --------------------------------------------------------------------- */
    /*                     Lifecycle methods (JUnit 5)                       */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for every test to avoid cross‑test interference.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the Message and mock are garbage‑collected.
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    /* --------------------------------------------------------------------- */
    /*                               Test cases                              */
    /* --------------------------------------------------------------------- */

    @Test
    void setResolver_assignsResolver_correctly() {
        // GIVEN – a fresh Message (already created in @BeforeEach) and a mock Resolver.
        assertTrue(message.getResolver().isEmpty(),
                "Resolver should be empty before setting it");

        // WHEN – we set the resolver.
        message.setResolver(mockResolver);

        // THEN – getResolver() must return the same instance wrapped in an Optional.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after setting it");
        assertSame(mockResolver, opt.get(),
                "Returned resolver must be the same instance that was set");
    }

    @Test
    void setResolver_withNull_clearsResolver() {
        // GIVEN – a resolver is already set.
        message.setResolver(mockResolver);
        assertTrue(message.getResolver().isPresent(),
                "Pre‑condition: resolver must be present");

        // WHEN – we set the resolver to null.
        message.setResolver(null);

        // THEN – getResolver() must be empty.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isEmpty(), "Resolver should be empty after setting null");
    }

    @Test
    void getResolver_withoutSetting_returnsEmptyOptional() {
        // GIVEN – a fresh Message (no resolver set).

        // WHEN – we query the resolver.
        Optional<Resolver> opt = message.getResolver();

        // THEN – the result must be empty.
        assertTrue(opt.isEmpty(), "Resolver should be empty when never set");
    }

    @Test
    void setResolver_overwritesPreviousResolver() {
        // GIVEN – a first resolver is set.
        Resolver firstResolver = org.mockito.Mockito.mock(Resolver.class);
        message.setResolver(firstResolver);
        assertSame(firstResolver, message.getResolver().orElseThrow(),
                "First resolver should be stored");

        // WHEN – we set a second, different resolver.
        Resolver secondResolver = org.mockito.Mockito.mock(Resolver.class);
        message.setResolver(secondResolver);

        // THEN – the stored resolver is now the second one.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after overwriting");
        assertSame(secondResolver, opt.get(),
                "Resolver should be overwritten with the new instance");
    }

    @Test
    void setResolver_idempotentWithSameInstance() {
        // GIVEN – a resolver instance is set.
        Resolver resolver = org.mockito.Mockito.mock(Resolver.class);
        message.setResolver(resolver);
        assertSame(resolver, message.getResolver().orElseThrow(),
                "Resolver should be stored after first call");

        // WHEN – the same instance is set again.
        message.setResolver(resolver);

        // THEN – the internal reference is unchanged.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should still be present");
        assertSame(resolver, opt.get(),
                "Resolver reference must remain the same after idempotent call");
    }

    @Test
    void setResolver_clearsAfterPreviousNonNull() {
        // GIVEN – a non‑null resolver has been set.
        Resolver resolver = org.mockito.Mockito.mock(Resolver.class);
        message.setResolver(resolver);
        assertTrue(message.getResolver().isPresent(),
                "Pre‑condition: resolver must be present");

        // WHEN – we clear it by passing null.
        message.setResolver(null);

        // THEN – getResolver() returns an empty Optional.
        assertTrue(message.getResolver().isEmpty(),
                "Resolver should be empty after clearing with null");
    }

    @Test
    void setResolver_doesNotAffectHeader() {
        // GIVEN – a custom Header attached to the Message.
        Header originalHeader = new Header(); // Header has a public no‑arg constructor.
        message.setHeader(originalHeader);
        assertSame(originalHeader, message.getHeader(),
                "Pre‑condition: header must be the one we set");

        // WHEN – we set a resolver (any resolver, here a mock).
        Resolver resolver = org.mockito.Mockito.mock(Resolver.class);
        message.setResolver(resolver);

        // THEN – the Header instance is unchanged.
        Header afterHeader = message.getHeader();
        assertSame(originalHeader, afterHeader,
                "Header should remain unchanged after setResolver");
    }
}
```