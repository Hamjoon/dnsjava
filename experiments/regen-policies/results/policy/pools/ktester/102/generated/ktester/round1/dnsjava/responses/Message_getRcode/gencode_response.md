```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_getRcode_Test {

    /** The Message instance under test – created as a spy so we can stub its collaborators. */
    private Message messageSpy;

    /** Mocked Header returned by {@code messageSpy.getHeader()}. */
    @Mock
    private Header mockHeader;

    /** Mocked OPTRecord returned by {@code messageSpy.getOPT()}. */
    @Mock
    private OPTRecord mockOpt;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real Message (using the no‑arg constructor) and then spy it.
        Message realMessage = new Message();
        messageSpy = Mockito.spy(realMessage);

        // By default the Message returns a non‑null Header; we stub it to our mock.
        doReturn(mockHeader).when(messageSpy).getHeader();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test contamination.
        Mockito.reset(mockHeader, mockOpt, messageSpy);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- *
     *  Simple cases already covered in the original template – kept for   *
     *  completeness.                                                       *
     * --------------------------------------------------------------------- */

    @Test
    void getRcode_withoutOPT_returnsHeaderRcode() {
        when(mockHeader.getRcode()).thenReturn(2);
        doReturn(null).when(messageSpy).getOPT();

        int rcode = messageSpy.getRcode();

        assertEquals(2, rcode);
        verify(mockHeader, times(1)).getRcode();
        verify(messageSpy, times(1)).getOPT();
    }

    @Test
    void getRcode_withOPT_addsExtendedRcode() {
        when(mockHeader.getRcode()).thenReturn(1);
        when(mockOpt.getExtendedRcode()).thenReturn(3);
        doReturn(mockOpt).when(messageSpy).getOPT();

        int rcode = messageSpy.getRcode();

        int expected = 1 + (3 << 4);
        assertEquals(expected, rcode);
        verify(mockHeader, times(1)).getRcode();
        verify(mockOpt, times(1)).getExtendedRcode();
        verify(messageSpy, times(1)).getOPT();
    }

    /* --------------------------------------------------------------------- *
     *  Branch & arithmetic coverage (no OPT)                                 *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] headerRcode={0} → expected={1}")
    @MethodSource("noOptProvider")
    void getRcode_noOPT_branchCoverage(int headerRcode, int expected) {
        when(mockHeader.getRcode()).thenReturn(headerRcode);
        doReturn(null).when(messageSpy).getOPT();

        assertEquals(expected, messageSpy.getRcode(),
                () -> "Expected rcode " + expected + " when header returns " + headerRcode + " and no OPT");
    }

    private static Stream<Arguments> noOptProvider() {
        return Stream.of(
                Arguments.of(0, 0),   // case 1
                Arguments.of(2, 2),   // case 2
                Arguments.of(0, 0),   // case 6 (opt present but extended 0 – handled in withOPT)
                Arguments.of(5, 5),   // mixed values without OPT
                Arguments.of(7, 7),   // mixed values without OPT
                Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE), // overflow test – no OPT
                Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE)  // underflow test – no OPT
        );
    }

    /* --------------------------------------------------------------------- *
     *  Branch & arithmetic coverage (with OPT)                               *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] header={0}, ext={1} → expected={2}")
    @MethodSource("withOptProvider")
    void getRcode_withOPT_branchCoverage(int headerRcode, int extRcode, int expected) {
        when(mockHeader.getRcode()).thenReturn(headerRcode);
        when(mockOpt.getExtendedRcode()).thenReturn(extRcode);
        doReturn(mockOpt).when(messageSpy).getOPT();

        assertEquals(expected, messageSpy.getRcode(),
                () -> "Expected " + expected + " for header=" + headerRcode + " ext=" + extRcode);
    }

    private static Stream<Arguments> withOptProvider() {
        return Stream.of(
                // original examples
                Arguments.of(0, 1, 16),          // 0 + (1<<4)
                Arguments.of(3, 2, 35),          // 3 + (2<<4)
                Arguments.of(15, 15, 255),       // max 4‑bit values
                // mixed values with OPT
                Arguments.of(5, 0, 5),
                Arguments.of(5, 1, 21),
                Arguments.of(5, 3, 53),
                Arguments.of(20, 5, 100),
                Arguments.of(100, 23, 468),
                // extreme overflow / underflow
                Arguments.of(Integer.MAX_VALUE, 15, Integer.MAX_VALUE + (15 << 4)),
                Arguments.of(Integer.MIN_VALUE, 15, Integer.MIN_VALUE + (15 << 4)),
                // negative header with zero/positive extended
                Arguments.of(-16, 0, -16),
                Arguments.of(-16, 1, 0) // -16 + (1<<4) = 0
        );
    }

    /* --------------------------------------------------------------------- *
     *  Null header handling                                                   *
     * --------------------------------------------------------------------- */

    @Test
    void getRcode_nullHeader_throwsNPE() {
        // Override the default stub so getHeader() returns null.
        doReturn(null).when(messageSpy).getHeader();

        assertThrows(NullPointerException.class, () -> messageSpy.getRcode(),
                "Calling getRcode() with a null Header should throw NPE");
    }

    /* --------------------------------------------------------------------- *
     *  Header.getRcode() exception propagation                                 *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] exception={0}")
    @MethodSource("headerExceptionProvider")
    void getRcode_headerThrowsException(RuntimeException ex) {
        when(mockHeader.getRcode()).thenThrow(ex);
        doReturn(null).when(messageSpy).getOPT();

        RuntimeException thrown = assertThrows(ex.getClass(), () -> messageSpy.getRcode(),
                "Exception from Header.getRcode() must propagate");
        assertSame(ex, thrown);
    }

    private static Stream<Arguments> headerExceptionProvider() {
        return Stream.of(
                Arguments.of(new IllegalStateException("illegal state")),
                Arguments.of(new RuntimeException("generic runtime"))
        );
    }

    /* --------------------------------------------------------------------- *
     *  getOPT() exception propagation                                          *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] exception={0}")
    @MethodSource("optExceptionProvider")
    void getRcode_optThrowsException(RuntimeException ex) {
        when(mockHeader.getRcode()).thenReturn(0);
        doThrow(ex).when(messageSpy).getOPT();

        RuntimeException thrown = assertThrows(ex.getClass(), () -> messageSpy.getRcode(),
                "Exception from getOPT() must propagate");
        assertSame(ex, thrown);
    }

    private static Stream<Arguments> optExceptionProvider() {
        return Stream.of(
                Arguments.of(new NullPointerException("opt NPE")),
                Arguments.of(new IllegalArgumentException("bad opt"))
        );
    }

    /* --------------------------------------------------------------------- *
     *  opt.getExtendedRcode() exception propagation                           *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] exception={0}")
    @MethodSource("optExtendedExceptionProvider")
    void getRcode_optExtendedThrowsException(RuntimeException ex) {
        when(mockHeader.getRcode()).thenReturn(0);
        when(mockOpt.getExtendedRcode()).thenThrow(ex);
        doReturn(mockOpt).when(messageSpy).getOPT();

        RuntimeException thrown = assertThrows(ex.getClass(), () -> messageSpy.getRcode(),
                "Exception from OPTRecord.getExtendedRcode() must propagate");
        assertSame(ex, thrown);
    }

    private static Stream<Arguments> optExtendedExceptionProvider() {
        return Stream.of(
                Arguments.of(new UnsupportedOperationException("unsupported")),
                Arguments.of(new RuntimeException("generic runtime"))
        );
    }
}
```