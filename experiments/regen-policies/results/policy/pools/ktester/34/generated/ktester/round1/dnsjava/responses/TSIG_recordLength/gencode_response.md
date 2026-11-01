```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TSIG#recordLength()}.
 *
 * <p>The method is a pure calculation based on the lengths of the {@code Name}
 * objects supplied to the {@link TSIG} constructor. The expected formula
 * (derived from the source) is:
 *
 * <pre>
 * recordLength = name.length() + alg.length() + 48
 * </pre>
 *
 * The tests cover typical values, edge‑cases, null handling, exception
 * propagation and integer overflow.  Mockito is used to mock {@link Name}
 * objects where the real implementation would be cumbersome.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_recordLength_Test {

    /** A dummy key – its content is irrelevant for {@code recordLength()}. */
    private static final String DUMMY_KEY = "aGVsbG93b3JsZA=="; // base64 for "helloworld"

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /*  Simple, deterministic cases (already present in the original file) */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("recordLength – simple fully‑qualified name")
    void recordLength_simpleName() throws TextParseException {
        Name alg = TSIG.HMAC_SHA256;                     // "hmac-sha256."
        Name name = Name.fromString("example.");        // "example."
        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);

        int expected = name.length() + alg.length() + 48;
        assertEquals(expected, tsig.recordLength(),
                "recordLength() should equal name.length() + alg.length() + 48");
    }

    @Test
    @DisplayName("recordLength – multi‑label name")
    void recordLength_multiLabelName() throws TextParseException {
        Name alg = TSIG.HMAC_SHA256;
        Name name = Name.fromString("a.example.");
        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);

        int expected = name.length() + alg.length() + 48;
        assertEquals(expected, tsig.recordLength(),
                "recordLength() should correctly handle multi‑label names");
    }

    @Test
    @DisplayName("recordLength – different algorithm and longer name")
    void recordLength_differentAlgorithmAndLongName() throws TextParseException {
        Name alg = TSIG.HMAC_MD5;
        Name name = Name.fromString("sub.domain.example.");
        TSIG tsig = new TSIG(alg, name, DUMMY_KEY);

        int expected = name.length() + alg.length() + 48;
        assertEquals(expected, tsig.recordLength(),
                "recordLength() must use the actual Name lengths for any algorithm");
    }

    @Test
    @DisplayName("recordLength – key does not influence result")
    void recordLength_keyIrrelevant() throws TextParseException {
        Name alg = TSIG.HMAC_SHA256;
        Name name = Name.fromString("example.");

        TSIG tsig1 = new TSIG(alg, name, DUMMY_KEY);
        TSIG tsig2 = new TSIG(alg, name, "c2VjcmV0a2V5"); // different base64 key

        assertEquals(tsig1.recordLength(), tsig2.recordLength(),
                "recordLength() must be independent of the secret key");
    }

    /* --------------------------------------------------------------------- */
    /*  JSON‑driven groups */
    /* --------------------------------------------------------------------- */

    /** Helper to create a TSIG instance via reflection when we need to inject mocked Names. */
    private TSIG createTsig(Name alg, Name name) throws Exception {
        // The real TSIG constructor used in the original tests takes (Name alg, Name name, String key)
        // If the constructor signature changes, adapt accordingly.
        return new TSIG(alg, name, DUMMY_KEY);
    }

    /* --------------------------- typical values -------------------------- */

    @Test
    @DisplayName("recordLength – typical values: example.com. + hmac‑sha256.")
    void recordLength_typical_exampleCom() throws Exception {
        // Both name and algorithm have wire‑format length 13
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn(13);
        when(alg.length()).thenReturn(13);

        TSIG tsig = createTsig(alg, name);
        int expected = 13 + 13 + 48; // 74
        assertEquals(expected, tsig.recordLength(),
                "Typical case with 13‑byte name and algorithm should be 74");
    }

    @Test
    @DisplayName("recordLength – typical values: root name \".\" + hmac‑md5.")
    void recordLength_typical_root() throws Exception {
        // root name length = 1, algorithm length = 10
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn(1);
        when(alg.length()).thenReturn(10);

        TSIG tsig = createTsig(alg, name);
        int expected = 1 + 10 + 48; // 59
        assertEquals(expected, tsig.recordLength(),
                "Root name + short algorithm should be 59");
    }

    @Test
    @DisplayName("recordLength – typical values: maximal 255‑byte names")
    void recordLength_typical_maxLengths() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn(255);
        when(alg.length()).thenReturn(255);

        TSIG tsig = createTsig(alg, name);
        int expected = 255 + 255 + 48; // 558
        assertEquals(expected, tsig.recordLength(),
                "Maximum length names should yield 558");
    }

    /* -------------------------- null handling --------------------------- */

    @Test
    @DisplayName("recordLength – name field forced to null throws NPE")
    void recordLength_nameNull() throws Exception {
        TSIG tsig = createTsig(TSIG.HMAC_SHA256, Name.fromString("example."));
        // force name = null via reflection
        Field f = TSIG.class.getDeclaredField("name");
        f.setAccessible(true);
        f.set(tsig, null);

        assertThrows(NullPointerException.class, tsig::recordLength,
                "Calling recordLength() with a null name should throw NPE");
    }

    @Test
    @DisplayName("recordLength – alg field forced to null throws NPE")
    void recordLength_algNull() throws Exception {
        TSIG tsig = createTsig(TSIG.HMAC_SHA256, Name.fromString("example."));
        // force alg = null via reflection
        Field f = TSIG.class.getDeclaredField("alg");
        f.setAccessible(true);
        f.set(tsig, null);

        assertThrows(NullPointerException.class, tsig::recordLength,
                "Calling recordLength() with a null algorithm should throw NPE");
    }

    /* ----------------------- small varying lengths ---------------------- */

    @ParameterizedTest(name = "{index} – {0}")
    @MethodSource("smallLengthProvider")
    @DisplayName("recordLength – small varying lengths")
    void recordLength_smallLengths(String description, Name name, Name alg, int expected) throws Exception {
        TSIG tsig = createTsig(alg, name);
        assertEquals(expected, tsig.recordLength(), description);
    }

    static Stream<Arguments> smallLengthProvider() throws TextParseException {
        // a. name "a." (length 2), alg "alg." (length 5) => 55
        Name name1 = Name.fromString("a.");
        Name alg1 = Name.fromString("alg.");
        // b. name length 10, alg length 20 (use mocks for simplicity)
        Name name2 = mock(Name.class);
        Name alg2 = mock(Name.class);
        when(name2.length()).thenReturn(10);
        when(alg2.length()).thenReturn(20);
        return Stream.of(
                Arguments.of("name \"a.\", alg \"alg.\": 2+5+48=55",
                        name1, alg1, 55),
                Arguments.of("mocked name length 10, alg length 20: 10+20+48=78",
                        name2, alg2, 78)
        );
    }

    /* -------------------------- idempotent behavior --------------------- */

    @Test
    @DisplayName("recordLength – idempotent: same value on repeated calls")
    void recordLength_idempotent() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn(13);
        when(alg.length()).thenReturn(13);
        TSIG tsig = createTsig(alg, name);

        int first = tsig.recordLength();
        int second = tsig.recordLength();

        assertEquals(74, first, "First invocation should be 74");
        assertEquals(first, second, "Second invocation must return the same value");
    }

    /* ------------------- after mutable field change (reflection) -------- */

    @Test
    @DisplayName("recordLength – after changing name via reflection")
    void recordLength_afterNameChange() throws Exception {
        // initial name length 13, algorithm length 13 => 74
        Name initialName = mock(Name.class);
        Name alg = mock(Name.class);
        when(initialName.length()).thenReturn(13);
        when(alg.length()).thenReturn(13);
        TSIG tsig = createTsig(alg, initialName);

        // verify initial value
        assertEquals(74, tsig.recordLength());

        // replace the name with a new mock of length 20
        Name newName = mock(Name.class);
        when(newName.length()).thenReturn(20);
        Field f = TSIG.class.getDeclaredField("name");
        f.setAccessible(true);
        f.set(tsig, newName);

        int expected = 20 + 13 + 48; // 81
        assertEquals(expected, tsig.recordLength(),
                "After mutating the name field, recordLength() must reflect the new length");
    }

    /* ------------------------ integer overflow guard -------------------- */

    @Test
    @DisplayName("recordLength – overflow when name length is Integer.MAX_VALUE")
    void recordLength_overflow() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn(Integer.MAX_VALUE);
        when(alg.length()).thenReturn(0);

        TSIG tsig = createTsig(alg, name);
        int expected = Integer.MAX_VALUE + 0 + 48; // overflowed int value
        assertEquals(expected, tsig.recordLength(),
                "Overflow should wrap around according to Java's 32‑bit arithmetic");
    }

    /* -------------------- exception propagation from length() ----------- */

    @Test
    @DisplayName("recordLength – name.length() throws IllegalStateException")
    void recordLength_nameLengthThrows() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenThrow(new IllegalStateException("name broken"));
        when(alg.length()).thenReturn(10); // not reached

        TSIG tsig = createTsig(alg, name);
        assertThrows(IllegalStateException.class, tsig::recordLength,
                "Exception from name.length() must propagate");
    }

    @Test
    @DisplayName("recordLength – alg.length() throws IllegalStateException")
    void recordLength_algLengthThrows() throws Exception {
        Name name = mock(Name.class);
        Name alg = mock(Name.class);
        when(name.length()).thenReturn(10);
        when(alg.length()).thenThrow(new IllegalStateException("alg broken"));

        TSIG tsig = createTsig(alg, name);
        assertThrows(IllegalStateException.class, tsig::recordLength,
                "Exception from alg.length() must propagate");
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
        assertEquals("name broken", ex.getMessage(),
                "The first exception (from name) must be propagated");
        // Verify that alg.length() was never called
        verify(alg, never()).length();
    }

    /* --------------------------------------------------------------------- */
    /*  End of test class                                                   */
    /* --------------------------------------------------------------------- */
}
```