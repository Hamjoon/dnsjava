package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_getRcode_Test {

    /**
     * The Message instance under test – created as a spy so we can stub its collaborators.
     */
    private Message messageSpy;

    /**
     * Mocked Header returned by {@code messageSpy.getHeader()}.
     */
    @Mock
    private Header mockHeader;

    /**
     * Mocked OPTRecord returned by {@code messageSpy.getOPT()}.
     */
    @Mock
    private OPTRecord mockOpt;

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
        // Reset mocks after each test to avoid cross‑test interference.
        Mockito.reset(mockHeader, mockOpt, messageSpy);
    }

    private static Stream<Arguments> noOptProvider() {
        return Stream.of(// case 1
        Arguments.of(0, 0), // case 2
        Arguments.of(2, 2), // case 6 (opt present but extended 0 – handled in withOPT)
        Arguments.of(0, 0), // mixed values without OPT
        Arguments.of(5, 5), // mixed values without OPT
        Arguments.of(7, 7), // overflow test – no OPT
        Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE), // underflow test – no OPT
        Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    private static Stream<Arguments> withOptProvider() {
        return Stream.of(// original examples
        // 0 + (1<<4)
        Arguments.of(0, 1, 16), // 3 + (2<<4)
        Arguments.of(3, 2, 35), // max 4‑bit values
        Arguments.of(15, 15, 255), // mixed values with OPT
        Arguments.of(5, 0, 5), Arguments.of(5, 1, 21), Arguments.of(5, 3, 53), Arguments.of(20, 5, 100), Arguments.of(100, 23, 468), // extreme overflow / underflow
        Arguments.of(Integer.MAX_VALUE, 15, Integer.MAX_VALUE + (15 << 4)), Arguments.of(Integer.MIN_VALUE, 15, Integer.MIN_VALUE + (15 << 4)), // negative header with zero/positive extended
        Arguments.of(-16, 0, -16), // -16 + (1<<4) = 0
        Arguments.of(-16, 1, 0));
    }

    private static Stream<Arguments> headerExceptionProvider() {
        return Stream.of(Arguments.of(new IllegalStateException("illegal state")), Arguments.of(new RuntimeException("generic runtime")));
    }

    private static Stream<Arguments> optExceptionProvider() {
        return Stream.of(Arguments.of(new NullPointerException("opt NPE")), Arguments.of(new IllegalArgumentException("bad opt")));
    }

    private static Stream<Arguments> optExtendedExceptionProvider() {
        return Stream.of(Arguments.of(new UnsupportedOperationException("unsupported")), Arguments.of(new RuntimeException("generic runtime")));
    }

    /**
     * Verify that {@code getRcode()} returns the value supplied by the Header when
     * no OPTRecord is present in the message.
     */
    @Test
    void getRcode_withoutOPT_returnsHeaderRcode() {
        // Arrange: Header reports an rcode of 2 (e.g., SERVFAIL).
        when(mockHeader.getRcode()).thenReturn(2);
        // Ensure getOPT() returns null (no OPTRecord in the message).
        doReturn(null).when(messageSpy).getOPT();
        // Act
        int rcode = messageSpy.getRcode();
        // Assert
        assertEquals(2, rcode, "Rcode should equal the header's rcode when no OPTRecord is present");
        // Verify interactions
        verify(mockHeader, times(1)).getRcode();
        verify(messageSpy, times(1)).getOPT();
    }

    /**
     * Verify that {@code getRcode()} adds the extended rcode from an OPTRecord
     * (shifted left by 4 bits) to the base rcode from the Header.
     */
    @Test
    void getRcode_withOPT_addsExtendedRcode() {
        // Arrange: Header reports a base rcode of 1 (e.g., FORMERR).
        when(mockHeader.getRcode()).thenReturn(1);
        // OPTRecord reports an extended rcode of 3.
        when(mockOpt.getExtendedRcode()).thenReturn(3);
        // Stub getOPT() to return our mock OPTRecord.
        doReturn(mockOpt).when(messageSpy).getOPT();
        // Act
        int rcode = messageSpy.getRcode();
        // Expected: base rcode + (extended << 4) = 1 + (3 << 4) = 1 + 48 = 49
        int expected = 1 + (3 << 4);
        assertEquals(expected, rcode, "Rcode should combine header rcode with extended rcode from OPTRecord");
        // Verify interactions
        verify(mockHeader, times(1)).getRcode();
        verify(mockOpt, times(1)).getExtendedRcode();
        verify(messageSpy, times(1)).getOPT();
    }

    @ParameterizedTest(name = "[{index}] headerRcode={0} → expected={1}")
    @MethodSource("noOptProvider")
    void getRcode_noOPT_branchCoverage(int headerRcode, int expected) {
        when(mockHeader.getRcode()).thenReturn(headerRcode);
        doReturn(null).when(messageSpy).getOPT();
        assertEquals(expected, messageSpy.getRcode(), () -> "Expected rcode " + expected + " when header returns " + headerRcode + " and no OPT");
    }

    @ParameterizedTest(name = "[{index}] header={0}, ext={1} → expected={2}")
    @MethodSource("withOptProvider")
    void getRcode_withOPT_branchCoverage(int headerRcode, int extRcode, int expected) {
        when(mockHeader.getRcode()).thenReturn(headerRcode);
        when(mockOpt.getExtendedRcode()).thenReturn(extRcode);
        doReturn(mockOpt).when(messageSpy).getOPT();
        assertEquals(expected, messageSpy.getRcode(), () -> "Expected " + expected + " for header=" + headerRcode + " ext=" + extRcode);
    }

    @Test
    void getRcode_nullHeader_throwsNPE() {
        // Override the default stub so getHeader() returns null.
        doReturn(null).when(messageSpy).getHeader();
        assertThrows(NullPointerException.class, () -> messageSpy.getRcode(), "Calling getRcode() with a null Header should throw NPE");
    }

    @ParameterizedTest(name = "[{index}] exception={0}")
    @MethodSource("headerExceptionProvider")
    void getRcode_headerThrowsException(RuntimeException ex) {
        when(mockHeader.getRcode()).thenThrow(ex);
        doReturn(null).when(messageSpy).getOPT();
        RuntimeException thrown = assertThrows(ex.getClass(), () -> messageSpy.getRcode(), "Exception from Header.getRcode() must propagate");
        assertSame(ex, thrown);
    }

    @ParameterizedTest(name = "[{index}] exception={0}")
    @MethodSource("optExceptionProvider")
    void getRcode_optThrowsException(RuntimeException ex) {
        when(mockHeader.getRcode()).thenReturn(0);
        doThrow(ex).when(messageSpy).getOPT();
        RuntimeException thrown = assertThrows(ex.getClass(), () -> messageSpy.getRcode(), "Exception from getOPT() must propagate");
        assertSame(ex, thrown);
    }

    @ParameterizedTest(name = "[{index}] exception={0}")
    @MethodSource("optExtendedExceptionProvider")
    void getRcode_optExtendedThrowsException(RuntimeException ex) {
        when(mockHeader.getRcode()).thenReturn(0);
        when(mockOpt.getExtendedRcode()).thenThrow(ex);
        doReturn(mockOpt).when(messageSpy).getOPT();
        RuntimeException thrown = assertThrows(ex.getClass(), () -> messageSpy.getRcode(), "Exception from OPTRecord.getExtendedRcode() must propagate");
        assertSame(ex, thrown);
    }
}
