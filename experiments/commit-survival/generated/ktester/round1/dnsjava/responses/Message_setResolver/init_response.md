```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_setResolver_Test {

    /** The Message instance used in each test. */
    private Message message;

    /** A mock Resolver that can be injected via {@link Message#setResolver(Resolver)}. */
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
        assertTrue(message.getResolver().isEmpty(), "Resolver should be empty before setting it");

        // WHEN – we set the resolver.
        message.setResolver(mockResolver);

        // THEN – getResolver() must return the same instance wrapped in an Optional.
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isPresent(), "Resolver should be present after setting it");
        assertSame(mockResolver, opt.get(), "Returned resolver must be the same instance that was set");
    }

    @Test
    void setResolver_withNull_clearsResolver() {
        // GIVEN – a resolver is already set.
        message.setResolver(mockResolver);
        assertTrue(message.getResolver().isPresent(), "Pre‑condition: resolver must be present");

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
}
```