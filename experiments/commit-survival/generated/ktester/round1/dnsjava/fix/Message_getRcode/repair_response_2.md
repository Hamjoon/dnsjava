```java
package org.xbill.DNS;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_getRcode_Test {

    private Message msg;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test; the default constructor creates a Header with a random ID.
        msg = new Message();
    }

    /**
     * Supplies the data from the JSON test matrix. Each entry contains:
     * <ul>
     *   <li>headerRcode – the value returned by {@code Header.getRcode()}</li>
     *   <li>optExtended – the value returned by {@code OPTRecord.getExtendedRcode()}</li>
     *   <li>expected – the expected result of {@code Message.getRcode()}</li>
     *   <li>description – human readable explanation (used only for test naming)</li>
     * </ul>
     */
    private static Stream<Arguments> rcodeMatrix() {
        return Stream.of(
                // basic cases
                Arguments.of(0, null, 0, "Header 0, no OPT → 0"),
                Arguments.of(3, null, 3, "Header 3, no OPT → 3"),
                // extended rcode cases
                Arguments.of(0, 1, 16, "Header 0, extended 1 → 1<<4 = 16"),
                Arguments.of(2, 5, 82, "Header 2, extended 5 → 5<<4 + 2 = 82"),
                Arguments.of(15, 15, 255, "Header 15, extended 15 → 15<<4 + 15 = 255"),
                Arguments.of(20, 20, 340, "Header 20, extended 20 → 20<<4 + 20 = 340"),
                // negative and zero cases
                Arguments.of(-5, null, -5, "Header -5, no OPT → -5"),
                Arguments.of(7, 0, 7, "Header 7, extended 0 → unchanged"),
                Arguments.of(255, 255, 4335, "Header 255, extended 255 → 255 + (255<<4) = 4335"),
                Arguments.of(-3, 2, 29, "Header -3, extended 2 → -3 + (2<<4) = 29"),
                // overflow cases
                Arguments.of(Integer.MAX_VALUE, 1,
                        (int) ((long) Integer.MAX_VALUE + (1L << 4)),
                        "Header MAX_INT, extended 1 → overflow"),
                Arguments.of(10, -1, -6, "Header 10, extended -1 → 10 + (-1<<4) = -6")
        );
    }

    /**
     * Verify that getRcode() returns the header's rcode when no OPT record is present.
     */
    @Test
    void getRcode_withoutOPT_returnsHeaderRcode() {
        Header header = msg.getHeader();
        // set a known rcode (e.g., SERVFAIL)
        header.setRcode(Rcode.SERVFAIL);
        assertEquals(Rcode.SERVFAIL, header.getRcode(),
                "Header rcode should have been set to SERVFAIL");
        int rcode = msg.getRcode();
        assertEquals(Rcode.SERVFAIL, rcode,
                "Message.getRcode() must return the header rcode when no OPT record exists");
    }

    /**
     * Verify that getRcode() adds the extended rcode from an OPT record (shifted left by 4 bits)
     * to the header's rcode.
     */
    @Test
    void getRcode_withOPT_addsExtendedRcode() {
        Header header = msg.getHeader();
        header.setRcode(Rcode.NOERROR);
        OPTRecord opt = new OPTRecord(512, 5, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        int expected = (5 << 4) + Rcode.NOERROR;
        int actual = msg.getRcode();
        assertEquals(expected, actual,
                "Message.getRcode() should combine header rcode and extended rcode from OPT");
    }

    /**
     * Verify that when both header rcode and extended rcode are non‑zero,
     * they are combined correctly.
     */
    @Test
    void getRcode_withOPT_andHeaderRcode_combinesBoth() {
        Header header = msg.getHeader();
        header.setRcode(Rcode.FORMERR);
        OPTRecord opt = new OPTRecord(1024, 3, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        int expected = (3 << 4) + Rcode.FORMERR;
        int actual = msg.getRcode();
        assertEquals(expected, actual,
                "Message.getRcode() must correctly combine header rcode and extended rcode");
    }

    /**
     * Verify that an OPT record with an extended rcode of zero does not affect the result.
     */
    @Test
    void getRcode_withOPT_extendedZero_isUnchanged() {
        Header header = msg.getHeader();
        header.setRcode(Rcode.NOTIMP);
        OPTRecord opt = new OPTRecord(256, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);
        int actual = msg.getRcode();
        assertEquals(Rcode.NOTIMP, actual,
                "When OPT extended rcode is zero, Message.getRcode() should equal the header rcode");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("rcodeMatrix")
    void getRcode_parameterised(int headerRcode, Integer optExtended, int expected, String _desc) {
        // Build a spy of Message so we can stub getHeader() and getOPT()
        Message spyMsg = Mockito.spy(new Message());

        // ----- Header ----------------------------------------------------
        Header mockHeader = mock(Header.class);
        when(mockHeader.getRcode()).thenReturn(headerRcode);
        doReturn(mockHeader).when(spyMsg).getHeader();

        // ----- OPTRecord ------------------------------------------------
        if (optExtended != null) {
            OPTRecord mockOpt = mock(OPTRecord.class);
            when(mockOpt.getExtendedRcode()).thenReturn(optExtended);
            doReturn(mockOpt).when(spyMsg).getOPT();
        } else {
            doReturn(null).when(spyMsg).getOPT();
        }

        // Execute and verify
        int actual = spyMsg.getRcode();
        assertEquals(expected, actual,
                () -> "Unexpected rcode for header=" + headerRcode + ", optExtended=" + optExtended);
    }

    @Test
    void getRcode_headerIsNull_throwsNPE() throws Exception {
        // Use reflection to set the private 'header' field to null.
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(msg, null);
        // The method should now throw NPE when trying to call header.getRcode()
        assertThrows(NullPointerException.class, msg::getRcode,
                "Calling getRcode() with a null Header must throw NullPointerException");
    }

    @Test
    void getRcode_optExtendedThrows_propagatesException() {
        // Spy the message to inject a mock OPTRecord that throws.
        Message spyMsg = Mockito.spy(new Message());

        // Normal header returning 0
        Header mockHeader = mock(Header.class);
        when(mockHeader.getRcode()).thenReturn(0);
        doReturn(mockHeader).when(spyMsg).getHeader();

        // OPTRecord whose getExtendedRcode throws IllegalStateException
        OPTRecord badOpt = mock(OPTRecord.class);
        when(badOpt.getExtendedRcode()).thenThrow(new IllegalStateException("bad opt"));
        doReturn(badOpt).when(spyMsg).getOPT();

        IllegalStateException ex = assertThrows(IllegalStateException.class, spyMsg::getRcode,
                "Exception from OPTRecord.getExtendedRcode() must propagate");
        assertEquals("bad opt", ex.getMessage());
    }
}
```