package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Permission;
import java.util.stream.Stream;

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
        // a plain Message instance is enough – the method under test does not depend on its state
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

    private static Stream<Arguments> authorityAllowedProvider() {
        return Stream.of(Arguments.of(Type.SOA), Arguments.of(Type.NS), Arguments.of(Type.DS), Arguments.of(Type.NSEC), Arguments.of(Type.NSEC3));
    }

    private static Stream<Arguments> authorityDisallowedProvider() {
        return Stream.of(Arguments.of(Type.A), Arguments.of(Type.MX));
    }

    private static Stream<Arguments> additionalAllowedProvider() {
        return Stream.of(Arguments.of(Type.A), Arguments.of(Type.AAAA));
    }

    private static Stream<Arguments> additionalDisallowedProvider() {
        return Stream.of(Arguments.of(Type.NS), Arguments.of(Type.SOA));
    }

    private static Stream<Arguments> nonAuthorityAdditionalProvider() {
        return Stream.of(Arguments.of(Type.A, Section.ANSWER), Arguments.of(Type.NS, Section.QUESTION));
    }

    private static Stream<Arguments> hardeningDisabledProvider() {
        return Stream.of(Arguments.of(Type.MX, Section.AUTHORITY), Arguments.of(Type.CNAME, Section.ADDITIONAL));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of(-1), Arguments.of(99999));
    }

    private static Stream<Arguments> invalidSectionProvider() {
        return Stream.of(Arguments.of(-1), Arguments.of(999));
    }

    @Test
    void authority_allowedTypes_returnTrue() {
        int[] allowed = { Type.SOA, Type.NS, Type.DS, Type.NSEC, Type.NSEC3 };
        for (int t : allowed) {
            assertTrue(message.isTypeAllowedInSection(t, Section.AUTHORITY), "type " + Type.string(t) + " should be allowed in AUTHORITY");
        }
    }

    @Test
    void authority_disallowedType_returnsFalseWhenHardeningEnabled() {
        // default property is "true" → unknown types are *not* allowed
        assertFalse(message.isTypeAllowedInSection(Type.A, Section.AUTHORITY), "type A should NOT be allowed in AUTHORITY when hardening is true");
    }

    @Test
    void additional_allowedTypes_returnTrue() {
        int[] allowed = { Type.A, Type.AAAA };
        for (int t : allowed) {
            assertTrue(message.isTypeAllowedInSection(t, Section.ADDITIONAL), "type " + Type.string(t) + " should be allowed in ADDITIONAL");
        }
    }

    @Test
    void additional_disallowedType_returnsFalseWhenHardeningEnabled() {
        assertFalse(message.isTypeAllowedInSection(Type.MX, Section.ADDITIONAL), "type MX should NOT be allowed in ADDITIONAL when hardening is true");
    }

    @Test
    void unknownType_returnsTrueWhenHardeningDisabled() {
        System.setProperty("dnsjava.harden_unknown_additional", "false");
        // pick a type that is not explicitly allowed in either section
        // not in the allow‑list
        int unknown = Type.SRV;
        assertTrue(message.isTypeAllowedInSection(unknown, Section.AUTHORITY), "when hardening is disabled, unknown type should be allowed in AUTHORITY");
        assertTrue(message.isTypeAllowedInSection(unknown, Section.ADDITIONAL), "when hardening is disabled, unknown type should be allowed in ADDITIONAL");
    }

    @Test
    void invalidType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.isTypeAllowedInSection(invalidType, Section.AUTHORITY), "Invalid type " + invalidType + " should cause IllegalArgumentException");
    }

    @Test
    void invalidSection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.isTypeAllowedInSection(Type.A, invalidSection), "Invalid section " + invalidSection + " should cause IllegalArgumentException");
    }

    /* ---------- Authority Section - Disallowed Types (hardening enabled) ---------- */
    @ParameterizedTest(name = "[{index}] type={0} disallowed in AUTHORITY")
    @MethodSource("authorityDisallowedProvider")
    void authority_disallowedTypes_returnFalseWhenHardeningEnabled(int type) {
        // default property is "true" → unknown types are *not* allowed
        assertFalse(message.isTypeAllowedInSection(type, Section.AUTHORITY), "type " + Type.string(type) + " should NOT be allowed in AUTHORITY when hardening is true");
    }

    /* ---------- Additional Section - Disallowed Types (hardening enabled) ---------- */
    @ParameterizedTest(name = "[{index}] type={0} disallowed in ADDITIONAL")
    @MethodSource("additionalDisallowedProvider")
    void additional_disallowedTypes_returnFalseWhenHardeningEnabled(int type) {
        assertFalse(message.isTypeAllowedInSection(type, Section.ADDITIONAL), "type " + Type.string(type) + " should NOT be allowed in ADDITIONAL when hardening is true");
    }

    /* ---------- Non‑Authority/Additional Sections ---------- */
    @ParameterizedTest(name = "[{index}] type={0} section={1} → fallback to hardening flag")
    @MethodSource("nonAuthorityAdditionalProvider")
    void nonAuthorityAdditional_sections_fallbackToHardening(int type, int section) {
        // With default hardening (true) the fallback should be false
        assertFalse(message.isTypeAllowedInSection(type, section), "type " + Type.string(type) + " in section " + section + " should fallback to false");
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
        SecurityException thrown = assertThrows(SecurityException.class, () -> message.isTypeAllowedInSection(Type.A, Section.AUTHORITY), "A SecurityException should be propagated when property access is denied");
        assertTrue(thrown.getMessage().contains("Access denied"));
    }
}
