**Explanation**

The test class lives in the same package (`org.xbill.DNS`) as `Message`, therefore it can call the package‑private method `isTypeAllowedInSection`.  
The tests cover:

| Scenario | What is verified |
|---------|------------------|
| Allowed types in the **AUTHORITY** section (`SOA`, `NS`, `DS`, `NSEC`, `NSEC3`) | method returns `true` |
| Disallowed type in the **AUTHORITY** section (`A`) | method returns `false` when the hardening property is **true** |
| Allowed types in the **ADDITIONAL** section (`A`, `AAAA`) | method returns `true` |
| Disallowed type in the **ADDITIONAL** section (`MX`) | method returns `false` when the hardening property is **true** |
| Effect of the system property `dnsjava.harden_unknown_additional` | when set to `"false"` the method returns `true` for any unknown type/section |
| Validation of arguments (`Type.check`, `Section.check`) | an `IllegalArgumentException` is thrown for an invalid type or section |

The `@BeforeAll` stores the original value of the system property so it can be restored in `@AfterAll`.  
`@BeforeEach` creates a fresh `Message` instance for each test (the instance itself is not used for state, only to invoke the method).  
No mocks are required, but the template already includes Mockito imports – they are simply left unused.

---

```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

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
        // Ensure the system property is reset after each test that may have changed it
        if (originalProperty != null) {
            System.setProperty("dnsjava.harden_unknown_additional", originalProperty);
        } else {
            System.clearProperty("dnsjava.harden_unknown_additional");
        }
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

    /* ---------- AUTHORITY section ---------- */

    @Test
    void authority_allowedTypes_returnTrue() {
        int[] allowed = { Type.SOA, Type.NS, Type.DS, Type.NSEC, Type.NSEC3 };
        for (int t : allowed) {
            assertTrue(message.isTypeAllowedInSection(t, Section.AUTHORITY),
                    "type " + Type.string(t) + " should be allowed in AUTHORITY");
        }
    }

    @Test
    void authority_disallowedType_returnsFalseWhenHardeningEnabled() {
        // default property is "true" → unknown types are *not* allowed
        assertFalse(message.isTypeAllowedInSection(Type.A, Section.AUTHORITY),
                "type A should NOT be allowed in AUTHORITY when hardening is true");
    }

    /* ---------- ADDITIONAL section ---------- */

    @Test
    void additional_allowedTypes_returnTrue() {
        int[] allowed = { Type.A, Type.AAAA };
        for (int t : allowed) {
            assertTrue(message.isTypeAllowedInSection(t, Section.ADDITIONAL),
                    "type " + Type.string(t) + " should be allowed in ADDITIONAL");
        }
    }

    @Test
    void additional_disallowedType_returnsFalseWhenHardeningEnabled() {
        assertFalse(message.isTypeAllowedInSection(Type.MX, Section.ADDITIONAL),
                "type MX should NOT be allowed in ADDITIONAL when hardening is true");
    }

    /* ---------- System property effect ---------- */

    @Test
    void unknownType_returnsTrueWhenHardeningDisabled() {
        System.setProperty("dnsjava.harden_unknown_additional", "false");
        // pick a type that is not explicitly allowed in either section
        int unknown = Type.SRV; // not in the allow‑list
        assertTrue(message.isTypeAllowedInSection(unknown, Section.AUTHORITY),
                "when hardening is disabled, unknown type should be allowed in AUTHORITY");
        assertTrue(message.isTypeAllowedInSection(unknown, Section.ADDITIONAL),
                "when hardening is disabled, unknown type should be allowed in ADDITIONAL");
    }

    /* ---------- Argument validation ---------- */

    @Test
    void invalidType_throwsIllegalArgumentException() {
        int invalidType = -1; // Type.check should reject this
        assertThrows(IllegalArgumentException.class,
                () -> message.isTypeAllowedInSection(invalidType, Section.AUTHORITY));
    }

    @Test
    void invalidSection_throwsIllegalArgumentException() {
        int invalidSection = 5; // valid sections are 0‑3
        assertThrows(IllegalArgumentException.class,
                () -> message.isTypeAllowedInSection(Type.A, invalidSection));
    }
}
```

**Key points**

* The test class is placed in the same package as `Message` so the package‑private method is directly accessible.  
* System property handling guarantees that changes made in a test do not affect other tests or the JVM state.  
* Each test is self‑contained, deterministic, and does not rely on external resources or private members.