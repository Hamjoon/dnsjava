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
 * The tests cover all scenarios described in the specification:
 * <ul>
 *   <li>Resolver not set → {@code Optional.empty()}</li>
 *   <li>Resolver set → {@code Optional.of(resolver)}</li>
 *   <li>Resolver cleared (set to {@code null}) → {@code Optional.empty()}</li>
 *   <li>Cloned message retains the same resolver reference</li>
 *   <li>Each {@code Message} instance maintains its own resolver state</li>
 *   <li>Subsequent {@code setResolver} overwrites the previous value</li>
 *   <li>Factory methods do not initialise a resolver</li>
 *   <li>Repeated calls to {@code getResolver()} are side‑effect free</li>
 * </ul>
 */
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

    /* --------------------------------------------------------------------- */
    /* Basic behaviour                                                       */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* Advanced scenarios                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void getResolver_afterClone_returnsSameResolverInstance() {
        // Arrange
        message.setResolver(mockResolver);

        // Act & Assert
        Message cloned = assertDoesNotThrow(() -> (Message) message.clone(),
                "Cloning a Message should not throw");
        Optional<Resolver> clonedOpt = cloned.getResolver();

        assertTrue(clonedOpt.isPresent(), "Cloned message should still contain a resolver");
        assertSame(mockResolver, clonedOpt.get(),
                "Resolver instance must be identical after cloning (reference equality)");
    }

    @Test
    void getResolver_independentInstances_doNotShareResolver() {
        // Arrange: first message gets a resolver, second stays untouched
        Message first = new Message();
        Message second = new Message();
        first.setResolver(mockResolver);

        // Act
        Optional<Resolver> firstOpt = first.getResolver();
        Optional<Resolver> secondOpt = second.getResolver();

        // Assert
        assertTrue(firstOpt.isPresent(), "First message should contain the resolver");
        assertSame(mockResolver, firstOpt.get(), "First message must return the resolver that was set");
        assertTrue(secondOpt.isEmpty(), "Second message should have no resolver");
    }

    @Test
    void getResolver_subsequentSetResolver_overwritesPrevious() {
        // Arrange
        Resolver firstResolver = mock(Resolver.class);
        Resolver secondResolver = mock(Resolver.class);
        message.setResolver(firstResolver);
        // Act: overwrite with a different resolver
        message.setResolver(secondResolver);
        Optional<Resolver> opt = message.getResolver();

        // Assert
        assertTrue(opt.isPresent(), "Resolver should be present after being overwritten");
        assertSame(secondResolver, opt.get(),
                "The resolver returned must be the most recently set instance");
    }

    @Test
    void getResolver_factoryMethodsReturnEmptyOptional() {
        // Arrange: create dummy arguments for the factories
        Record dummyRecord = mock(Record.class);
        Name dummyName = mock(Name.class);

        // Act
        Message queryMessage = Message.newQuery(dummyRecord);
        Message updateMessage = Message.newUpdate(dummyName);

        // Assert
        assertTrue(queryMessage.getResolver().isEmpty(),
                "Message created via newQuery() must not have a resolver");
        assertTrue(updateMessage.getResolver().isEmpty(),
                "Message created via newUpdate() must not have a resolver");
    }

    @Test
    void getResolver_multipleCallsAlwaysReturnSameInstance() {
        // Arrange
        message.setResolver(mockResolver);

        // Act & Assert: call getResolver several times and verify the result stays identical
        for (int i = 0; i < 5; i++) {
            Optional<Resolver> opt = message.getResolver();
            assertTrue(opt.isPresent(), "Call #" + (i + 1) + " should return a present Optional");
            assertSame(mockResolver, opt.get(),
                    "Call #" + (i + 1) + " must return the same resolver instance");
        }
    }
}
```