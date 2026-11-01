```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Permission;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#isTypeAllowedInSection(int, int)}.
 *
 * <p>The tests are grouped by the logical scenario described in the
 * supplied JSON test‑case document.  Each group is exercised by a
 * single {@code @ParameterizedTest} method that receives the
 * {@code type}, {@code section} and the expected outcome.</p>
 *
 * <p>The {@code @BeforeEach}/{@code @AfterEach} methods preserve and
 * restore the system property {@code dnsjava.harden_unknown_additional}
 * so that tests that modify it do not affect other tests.</p>
 *
 * <p>A special test verifies that a {@link SecurityException} thrown
 * by {@code System.getProperty} is propagated by the method under test.
 * This is achieved by installing a custom {@link SecurityManager}
 * that denies property access.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_isTypeAllowedInSection_Test {

    private Message message;
    private static String originalProperty;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original value so we can restore it later
        originalProperty = System.getProperty("dnsjava.harden_unknown_additional");
    }

    @BeforeEach
    void setupBeforeEach() {
        // A plain Message instance is sufficient – the method does not depend on internal state
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the system property after each test that may have changed it
        if (originalProperty != null) {
            System.setProperty("dnsjava.harden_unknown_additional", originalProperty);
        } else {
            System.clearProperty("dnsjava.harden_unknown_additional");
        }
        // Restore any custom SecurityManager that might have been installed
        System.setSecurityManager(null);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original property value for the whole JVM
        if (originalProperty != null) {
            System.setProperty("dnsjava.harden_unknown_additional", originalProperty);
        } else {
            System.clearProperty("dnsjava.harden_unknown_additional");
        }
    }

    /* ---------- Authority Section - Allowed Types ---------- */
    @ParameterizedTest(name = "[{index}] type={0} allowed in AUTHORITY")
    @MethodSource("authorityAllowedProvider")
    void authority_allowedTypes_returnTrue(int type) {
        assertTrue(message.isTypeAllowedInSection(type, Section.AUTHORITY),
                "type " + Type.string(type) + " should be allowed in AUTHORITY");
    }

    private static Stream<Arguments> authorityAllowedProvider() {
        return Stream.of(
                Arguments.of(Type.SOA),
                Arguments.of(Type.NS),
                Arguments.of(Type.DS),
                Arguments.of(Type.NSEC),
                Arguments.of(Type.NSEC3)
        );
    }

    /* ---------- Authority Section - Disallowed Types (hardening enabled) ---------- */
    @ParameterizedTest(name = "[{index}] type={0} disallowed in AUTHORITY")
    @MethodSource("authorityDisallowedProvider")
    void authority_disallowedTypes_returnFalseWhenHardeningEnabled(int type) {
        // default property is "true" → unknown types are *not* allowed
        assertFalse(message.isTypeAllowedInSection(type, Section.AUTHORITY),
                "type " + Type.string(type) + " should NOT be allowed in AUTHORITY when hardening is true");
    }

    private static Stream<Arguments> authorityDisallowedProvider() {
        return Stream.of(
                Arguments.of(Type.A),
                Arguments.of(Type.MX)
        );
    }

    /* ---------- Additional Section - Allowed Types ---------- */
    @ParameterizedTest(name = "[{index}] type={0} allowed in ADDITIONAL")
    @MethodSource("additionalAllowedProvider")
    void additional_allowedTypes_returnTrue(int type) {
        assertTrue(message.isTypeAllowedInSection(type, Section.ADDITIONAL),
                "type " + Type.string(type) + " should be allowed in ADDITIONAL");
    }

    private static Stream<Arguments> additionalAllowedProvider() {
        return Stream.of(
                Arguments.of(Type.A),
                Arguments.of(Type.AAAA)
        );
    }

    /* ---------- Additional Section - Disallowed Types (hardening enabled) ---------- */
    @ParameterizedTest(name = "[{index}] type={0} disallowed in ADDITIONAL")
    @MethodSource("additionalDisallowedProvider")
    void additional_disallowedTypes_returnFalseWhenHardeningEnabled(int type) {
        assertFalse(message.isTypeAllowedInSection(type, Section.ADDITIONAL),
                "type " + Type.string(type) + " should NOT be allowed in ADDITIONAL when hardening is true");
    }

    private static Stream<Arguments> additionalDisallowedProvider() {
        return Stream.of(
                Arguments.of(Type.NS),
                Arguments.of(Type.SOA)
        );
    }

    /* ---------- Non‑Authority/Additional Sections ---------- */
    @ParameterizedTest(name = "[{index}] type={0} section={1} → fallback to hardening flag")
    @MethodSource("nonAuthorityAdditionalProvider")
    void nonAuthorityAdditional_sections_fallbackToHardening(int type, int section) {
        // With default hardening (true) the fallback should be false
        assertFalse(message.isTypeAllowedInSection(type, section),
                "type " + Type.string(type) + " in section " + section + " should fallback to false");
    }

    private static Stream<Arguments> nonAuthorityAdditionalProvider() {
        return Stream.of(
                Arguments.of(Type.A, Section.ANSWER),
                Arguments.of(Type.NS, Section.QUESTION)
        );
    }

    /* ---------- System Property Harden Unknown Additional Disabled ---------- */
    @ParameterizedTest(name = "[{index}] type={0} section={1} with hardening disabled → true")
    @MethodSource("hardeningDisabledProvider")
    void unknownType_returnsTrueWhenHardeningDisabled(int type, int section) {
        System.setProperty("dnsjava.harden_unknown_additional", "false");
        assertTrue(message.isTypeAllowedInSection(type, section),
                "when hardening is disabled, type " + Type.string(type) + " in section " + section + " should be allowed");
    }

    private static Stream<Arguments> hardeningDisabledProvider() {
        return Stream.of(
                Arguments.of(Type.MX, Section.AUTHORITY),
                Arguments.of(Type.CNAME, Section.ADDITIONAL)
        );
    }

    /* ---------- Invalid Type Handling ---------- */
    @ParameterizedTest(name = "[{index}] invalid type={0}")
    @MethodSource("invalidTypeProvider")
    void invalidType_throwsIllegalArgumentException(int invalidType) {
        assertThrows(IllegalArgumentException.class,
                () -> message.isTypeAllowedInSection(invalidType, Section.AUTHORITY),
                "Invalid type " + invalidType + " should cause IllegalArgumentException");
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(
                Arguments.of(-1),
                Arguments.of(99999)
        );
    }

    /* ---------- Invalid Section Handling ---------- */
    @ParameterizedTest(name = "[{index}] invalid section={0}")
    @MethodSource("invalidSectionProvider")
    void invalidSection_throwsIllegalArgumentException(int invalidSection) {
        assertThrows(IllegalArgumentException.class,
                () -> message.isTypeAllowedInSection(Type.A, invalidSection),
                "Invalid section " + invalidSection + " should cause IllegalArgumentException");
    }

    private static Stream<Arguments> invalidSectionProvider() {
        return Stream.of(
                Arguments.of(-1),
                Arguments.of(999)
        );
    }

    /* ---------- System Property Access Security Exception ---------- */
    @Test
    void systemPropertyAccessSecurityException_isPropagated() {
        // Install a SecurityManager that forbids property reads
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPropertyAccess(String key) {
                if ("dnsjava.harden_unknown_additional".equals(key)) {
                    throw new SecurityException("Access denied to system property");
                }
            }
        });

        SecurityException thrown = assertThrows(SecurityException.class,
                () -> message.isTypeAllowedInSection(Type.A, Section.AUTHORITY),
                "A SecurityException should be propagated when property access is denied");
        assertTrue(thrown.getMessage().contains("Access denied"));
    }
}
```