```java
package org.xbill.DNS;

import java.security.SecureRandom;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class TSIG_recordLength_Test {

    // base64 for "helloworld"
    private static final String DUMMY_KEY = "aGVsbG93b3JsZA==";

    /**
     * Helper to create a TSIG instance via reflection when we need to inject mocked Names.
     */
    private TSIG createTsig(Name alg, Name name) throws Exception {
        // The real TSIG constructor used in the original tests takes (Name alg, Name name, String key)
        // If the constructor signature changes, adapt accordingly.
        return new TSIG(alg, name, DUMMY_KEY);
    }

    static Stream<Arguments> smallLengthProvider() throws TextParseException {
        // a. name "a." (length 2), alg "alg." (length 5) => 55
        Name name1 = Name.fromString("a.");
        Name alg1 = Name.fromString("alg.");
        // b. name length 10, alg length 20 (use mocks for simplicity)
        Name name2 = mock(Name.class);
        Name alg2 = mock(Name.class);
        when(name2.length()).thenReturn((short) 10);
        when(alg2.length()).thenReturn((short) 20);
        return Stream.of(
                Arguments.of("name \"a.\", alg \"alg.\": 2+5+48=55", name1, alg1, 55),
                Arguments.of("mocked name length 10, alg length 20: 10+20+48=78", name2, alg2, 78));
    }

    /**
     * Verify the calculation for a simple, fully‑qualified name "example."
     * and the standard HMAC‑SHA256 algorithm name.
     */
    @Test
    void recordLength_simpleName() throws TextParseException {
        // "hmac-sha256."
        Name alg = TSIG.HMAC_SHA256;
        // "example."
        Name name = Name.fromString("example.");
        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);
        // Expected: name.length() = 9, alg.length() = 12, constant part = 48
        int expected = 9 + 12 + 48;
        assertEquals(expected, tsig.recordLength(), "recordLength() should equal name.length() + alg.length() + 48");
    }

    /**
     * Verify the calculation for a multi‑label name "a.example."
     * and the same algorithm.
     */
    @Test
    void recordLength_multiLabelName() throws TextParseException {
        // "hmac-sha256."
        Name alg = TSIG.HMAC_SHA256;
        // "a.example."
        Name name = Name.fromString("a.example.");
        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);
        // Expected: name.length() = 11 (1+1 + 1+7 + 1), alg.length() = 12, constant part = 48
        int expected = 11 + 12 + 48;
        assertEquals(expected, tsig.recordLength(), "recordLength() should correctly handle multi‑label names");
    }

    /**
     * Verify that the calculation works with a different algorithm name
     * (HMAC‑MD5) and a longer domain name.
     */
    @Test
    void recordLength_differentAlgorithmAndLongName() throws TextParseException {
        // "HMAC-MD5.SIG-ALG.REG.INT."
        Name alg = TSIG.HMAC_MD5;
        // three labels
        Name name = Name.fromString("sub.domain.example.");
        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);
        // Compute expected lengths using the Name API
        // wire length of "sub.domain.example."
        int nameLen = name.length();
        // wire length of "HMAC-MD5.SIG-ALG.REG.INT."
        int algLen = alg.length();
        int expected = nameLen + algLen + 48;
        assertEquals(expected, tsig.recordLength(), "recordLength() must use the actual Name lengths for any algorithm");
    }

    /**
     * Ensure that the method does not depend on the key material.
     * Two TSIG objects with the same names but different keys must yield the same length.
     */
    @Test
    void recordLength_keyIrrelevant() throws TextParseException {
        Name alg = TSIG.HMAC_SHA256;
        Name name = Name.fromString("example.");
        TSIG tsig1 = new TSIG(alg, name, DUMMY_KEY);
        // different base64 key
        TSIG tsig2 = new TSIG(alg, name, "c2VjcmV0a2V5");
        assertEquals(tsig1.recordLength(), tsig2.recordLength(), "recordLength() must be independent of the secret key");
    }

    @Test
    @DisplayName("recordLength – typical values: example.com. + hmac‑sha256.")
    void recordLength_typical_exampleCom() throws Exception {
        // Both name and algorithm have wire‑format length 13
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn((short) 13);
        when(alg.length()).thenReturn((short) 13);
        TSIG tsig = createTsig(alg, name);
        // 74
        int expected = 13 + 13 + 48;
        assertEquals(expected, tsig.recordLength(), "Typical case with 13‑byte name and algorithm should be 74");
    }

    @Test
    @DisplayName("recordLength – typical values: root name \".\" + hmac‑md5.")
    void recordLength_typical_root() throws Exception {
        // root name length = 1, algorithm length = 10
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn((short) 1);
        when(alg.length()).thenReturn((short) 10);
        TSIG tsig = createTsig(alg, name);
        // 59
        int expected = 1 + 10 + 48;
        assertEquals(expected, tsig.recordLength(), "Root name + short algorithm should be 59");
    }

    @Test
    @DisplayName("recordLength – typical values: maximal 255‑byte names")
    void recordLength_typical_maxLengths() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn((short) 255);
        when(alg.length()).thenReturn((short) 255);
        TSIG tsig = createTsig(alg, name);
        // 558
        int expected = 255 + 255 + 48;
        assertEquals(expected, tsig.recordLength(), "Maximum length names should yield 558");
    }

    @Test
    @DisplayName("recordLength – name field forced to null throws NPE")
    void recordLength_nameNull() throws Exception {
        TSIG tsig = createTsig(TSIG.HMAC_SHA256, Name.fromString("example."));
        // force name = null via reflection
        Field f = TSIG.class.getDeclaredField("name");
        f.setAccessible(true);
        f.set(tsig, null);
        assertThrows(NullPointerException.class, tsig::recordLength, "Calling recordLength() with a null name should throw NPE");
    }

    @Test
    @DisplayName("recordLength – alg field forced to null throws NPE")
    void recordLength_algNull() throws Exception {
        TSIG tsig = createTsig(TSIG.HMAC_SHA256, Name.fromString("example."));
        // force alg = null via reflection
        Field f = TSIG.class.getDeclaredField("alg");
        f.setAccessible(true);
        f.set(tsig, null);
        assertThrows(NullPointerException.class, tsig::recordLength, "Calling recordLength() with a null algorithm should throw NPE");
    }

    @ParameterizedTest(name = "{index} – {0}")
    @MethodSource("smallLengthProvider")
    @DisplayName("recordLength – small varying lengths")
    void recordLength_smallLengths(String description, Name name, Name alg, int expected) throws Exception {
        TSIG tsig = createTsig(alg, name);
        assertEquals(expected, tsig.recordLength(), description);
    }

    @Test
    @DisplayName("recordLength – idempotent: same value on repeated calls")
    void recordLength_idempotent() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn((short) 13);
        when(alg.length()).thenReturn((short) 13);
        TSIG tsig = createTsig(alg, name);
        int first = tsig.recordLength();
        int second = tsig.recordLength();
        assertEquals(74, first, "First invocation should be 74");
        assertEquals(first, second, "Second invocation must return the same value");
    }

    @Test
    @DisplayName("recordLength – after changing name via reflection")
    void recordLength_afterNameChange() throws Exception {
        // initial name length 13, algorithm length 13 => 74
        Name initialName = mock(Name.class);
        Name alg = mock(Name.class);
        when(initialName.length()).thenReturn((short) 13);
        when(alg.length()).thenReturn((short) 13);
        TSIG tsig = createTsig(alg, initialName);
        // verify initial value
        assertEquals(74, tsig.recordLength());
        // replace the name with a new mock of length 20
        Name newName = mock(Name.class);
        when(newName.length()).thenReturn((short) 20);
        Field f = TSIG.class.getDeclaredField("name");
        f.setAccessible(true);
        f.set(tsig, newName);
        // 81
        int expected = 20 + 13 + 48;
        assertEquals(expected, tsig.recordLength(), "After mutating the name field, recordLength() must reflect the new length");
    }

    @Test
    @DisplayName("recordLength – overflow when name length is maximal short value")
    void recordLength_overflow() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn(Short.MAX_VALUE); // 32767
        when(alg.length()).thenReturn((short) 0);
        TSIG tsig = createTsig(alg, name);
        int expected = Short.MAX_VALUE + 0 + 48;
        assertEquals(expected, tsig.recordLength(), "Overflow should wrap according to Java's 32‑bit arithmetic");
    }

    @Test
    @DisplayName("recordLength – name.length() throws IllegalStateException")
    void recordLength_nameLengthThrows() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenThrow(new IllegalStateException("name broken"));
        // not reached
        when(alg.length()).thenReturn((short) 10);
        TSIG tsig = createTsig(alg, name);
        assertThrows(IllegalStateException.class, tsig::recordLength, "Exception from name.length() must propagate");
    }

    @Test
    @DisplayName("recordLength – alg.length() throws IllegalStateException")
    void recordLength_algLengthThrows() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn((short) 10);
        when(alg.length()).thenThrow(new IllegalStateException("alg broken"));
        TSIG tsig = createTsig(alg, name);
        assertThrows(IllegalStateException.class, tsig::recordLength, "Exception from alg.length() must propagate");
    }

    @Test
    @DisplayName("recordLength – both length() methods throw; name first")
    void recordLength_bothThrow() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenThrow(new IllegalStateException("name broken"));
        when(alg.length()).thenThrow(new IllegalStateException("alg broken"));
        TSIG tsig = createTsig(alg, name);
        // The exception from name.length() should be the one we see.
        IllegalStateException ex = assertThrows(IllegalStateException.class, tsig::recordLength);
        assertEquals("name broken", ex.getMessage(), "The first exception (from name) must be propagated");
        // Verify that alg.length() was never called
        verify(alg, never()).length();
    }
}
```