```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_isTypeAllowedInSection_Test {

    /** Holds the original value of the hardening property so we can restore it after the suite. */
    private static String originalHardeningProperty;

    /** A fresh Message instance for each test – the method under test is package‑private, so we can call it directly. */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original system property (may be null)
        originalHardeningProperty = System.getProperty("dnsjava.harden_unknown_additional");
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original value (or clear it if it was not set)
        if (originalHardeningProperty == null) {
            System.clearProperty("dnsjava.harden_unknown_additional");
        } else {
            System.setProperty("dnsjava.harden_unknown_additional", originalHardeningProperty);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor – the internal state of the Message does not affect the
        // isTypeAllowedInSection logic.
        message = new Message();
        // Ensure the default hardening behaviour for each test unless a test explicitly changes it.
        System.setProperty("dnsjava.harden_unknown_additional", "true");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear any property changes made by a test to avoid cross‑test pollution.
        System.clearProperty("dnsjava.harden_unknown_additional");
    }

    /* --------------------------------------------------------------------- */
    /*  AUTHORITY SECTION – allowed types                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void authority_allows_SOA() {
        assertTrue(message.isTypeAllowedInSection(Type.SOA, Section.AUTHORITY));
    }

    @Test
    void authority_allows_NS() {
        assertTrue(message.isTypeAllowedInSection(Type.NS, Section.AUTHORITY));
    }

    @Test
    void authority_allows_DS() {
        assertTrue(message.isTypeAllowedInSection(Type.DS, Section.AUTHORITY));
    }

    @Test
    void authority_allows_NSEC() {
        assertTrue(message.isTypeAllowedInSection(Type.NSEC, Section.AUTHORITY));
    }

    @Test
    void authority_allows_NSEC3() {
        assertTrue(message.isTypeAllowedInSection(Type.NSEC3, Section.AUTHORITY));
    }

    @Test
    void authority_disallows_A_when_hardening_enabled() {
        // A is not in the whitelist for AUTHORITY → should be false when hardening is true.
        assertFalse(message.isTypeAllowedInSection(Type.A, Section.AUTHORITY));
    }

    @Test
    void authority_allows_A_when_hardening_disabled() {
        System.setProperty("dnsjava.harden_unknown_additional", "false");
        // With hardening disabled the default branch returns true for unknown types.
        assertTrue(message.isTypeAllowedInSection(Type.A, Section.AUTHORITY));
    }

    /* --------------------------------------------------------------------- */
    /*  ADDITIONAL SECTION – allowed types                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void additional_allows_A() {
        assertTrue(message.isTypeAllowedInSection(Type.A, Section.ADDITIONAL));
    }

    @Test
    void additional_allows_AAAA() {
        assertTrue(message.isTypeAllowedInSection(Type.AAAA, Section.ADDITIONAL));
    }

    @Test
    void additional_disallows_MX_when_hardening_enabled() {
        assertFalse(message.isTypeAllowedInSection(Type.MX, Section.ADDITIONAL));
    }

    @Test
    void additional_allows_MX_when_hardening_disabled() {
        System.setProperty("dnsjava.harden_unknown_additional", "false");
        assertTrue(message.isTypeAllowedInSection(Type.MX, Section.ADDITIONAL));
    }

    /* --------------------------------------------------------------------- */
    /*  OTHER SECTIONS – default branch                                       */
    /* --------------------------------------------------------------------- */

    @Test
    void answer_section_respects_hardening_property() {
        // Section.ANSWER is not handled specially – we expect the default behaviour.
        assertFalse(message.isTypeAllowedInSection(Type.A, Section.ANSWER));

        System.setProperty("dnsjava.harden_unknown_additional", "false");
        assertTrue(message.isTypeAllowedInSection(Type.A, Section.ANSWER));
    }

    @Test
    void question_section_respects_hardening_property() {
        assertFalse(message.isTypeAllowedInSection(Type.NS, Section.QUESTION));

        System.setProperty("dnsjava.harden_unknown_additional", "false");
        assertTrue(message.isTypeAllowedInSection(Type.NS, Section.QUESTION));
    }
}
```