package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_getResolver_Test {

    private Message message;

    @Mock
    private Resolver mockResolver;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message instance for each test
        message = new Message();
    }

    /**
     * Supplies arguments for {@link #getResolver_coreScenarios(Resolver, Optional)}.
     *
     * @return a stream of {@link Arguments} where the first element is the resolver to set
     *     (or {@code null}) and the second element is the expected {@link Optional} result.
     */
    private static Stream<Arguments> resolverProvider() {
        // The mock resolver will be injected by Mockito at runtime, but for the static provider we
        // need a placeholder. The test method receives the actual mock instance from the test class
        // field via the parameter injection mechanism.
        return Stream.of(// Case 1: resolver is null → expect empty Optional
        Arguments.of(null, Optional.empty()), // Case 2: resolver is a mock instance → expect Optional.of(mock)
        // The actual mock instance will be supplied by the test method's parameter
        Arguments.of(mock(Resolver.class), Optional.of(mock(Resolver.class))));
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
        // Then: the getter should reflect the most recent assignment
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
        // Then: the getter must return an empty Optional
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isEmpty(), "Resolver should be empty after being set to null");
    }

    /**
     * Parameterized test that covers the core JSON test cases:
     *
     * <ul>
     *   <li>resolver = null → expect {@code Optional.empty()}
     *   <li>resolver = mockResolver → expect {@code Optional.of(mockResolver)}
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] resolver={0}")
    @MethodSource("resolverProvider")
    void getResolver_coreScenarios(Resolver resolverToSet, Optional<Resolver> expected) {
        // Given: optionally set a resolver on the Message
        message.setResolver(resolverToSet);
        // When: we retrieve the resolver via the focal method
        Optional<Resolver> actual = message.getResolver();
        // Then: the result must match the expected Optional
        assertNotNull(actual, "Optional should never be null");
        assertEquals(expected, actual, () -> "Expected getResolver() to return " + expected + " when resolver was set to " + resolverToSet);
    }
}
