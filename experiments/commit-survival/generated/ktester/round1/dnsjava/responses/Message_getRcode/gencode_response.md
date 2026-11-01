```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#getRcode()}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>basic behaviour when no OPT record is present</li>
 *   <li>combination of header rcode and extended rcode from an OPT record</li>
 *   <li>edge‑cases such as zero, negative, overflow and maximum values</li>
 *   <li>exception propagation when the internal {@link Header} or {@link OPTRecord}
 *   are malformed.</li>
 * </ul>
 *
 * <p>All tests use the public API only; when we need to inject a {@code null}
 * header we resort to reflection (the requirement forbids direct field access).</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_getRcode_Test {

    private Message msg;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for each test. The default constructor creates a Header.
        msg = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    /* --------------------------------------------------------------------- */
    /*  Simple non‑OPT scenarios – already covered in the original template */
    /* --------------------------------------------------------------------- */

    @Test
    void getRcode_withoutOPT_returnsHeaderRcode() {
        Header header = msg.getHeader();
        header.setRcode(Rcode.SERVFAIL);
        assertEquals(Rcode.SERVFAIL, header.getRcode(),
                "Header rcode should have been set to SERVFAIL");

        int rcode = msg.getRcode();
        assertEquals(Rcode.SERVFAIL, rcode,
                "Message.getRcode() must return the header rcode when no OPT record exists");
    }

    @Test
    void getRcode_withOPT_addsExtendedRcode() {
        Header header = msg.getHeader();
        header.setRcode(Rcode.NOERROR);
        OPTRecord opt = new OPTRecord(512, 5, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        int expected = (5 << 4) + Rcode.NOERROR;
        assertEquals(expected, msg.getRcode(),
                "Message.getRcode() should combine header rcode and extended rcode from OPT");
    }

    @Test
    void getRcode_withOPT_andHeaderRcode_combinesBoth() {
        Header header = msg.getHeader();
        header.setRcode(Rcode.FORMERR); // 1

        OPTRecord opt = new OPTRecord(1024, 3, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        int expected = (3 << 4) + Rcode.FORMERR; // 48 + 1 = 49
        assertEquals(expected, msg.getRcode(),
                "Message.getRcode() must correctly combine header rcode and extended rcode");
    }

    @Test
    void getRcode_withOPT_extendedZero_isUnchanged() {
        Header header = msg.getHeader();
        header.setRcode(Rcode.NOTIMP); // 4

        OPTRecord opt = new OPTRecord(256, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        assertEquals(Rcode.NOTIMP, msg.getRcode(),
                "When OPT extended rcode is zero, Message.getRcode() should equal the header rcode");
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests for the exhaustive JSON matrix                 */
    /* --------------------------------------------------------------------- */

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
                // group: Message.getRcode
                Arguments.of(0, null, 0,
                        "Header 0, no OPT → 0"),
                Arguments.of(3, null, 3,
                        "Header 3, no OPT → 3"),
                Arguments.of(0, 1, 16,
                        "Header 0, extended 1 → 1<<4 = 16"),
                Arguments.of(2, 5, 82,
                        "Header 2, extended 5 → 5<<4 + 2 = 82"),
                Arguments.of(15, 15, 255,
                        "Header 15, extended 15 → 15<<4 + 15 = 255"),
                Arguments.of(20, 20, 340,
                        "Header 20, extended 20 → 20<<4 + 20 = 340"),
                Arguments.of(-5, null, -5,
                        "Header -5, no OPT → -5"),

                // group: Message.getRcode - additional and edge cases
                Arguments.of(7, 0, 7,
                        "Header 7, extended 0 → unchanged"),
                Arguments.of(255, 255, 4335,
                        "Header 255, extended 255 → 255 + (255<<4) = 4335"),
                Arguments.of(-3, 2, 29,
                        "Header -3, extended 2 → -3 + (2<<4) = 29"),
                Arguments.of(Integer.MAX_VALUE, 1,
                        Integer.MAX_VALUE + (1 << 4), // overflow expected
                        "Header MAX_INT, extended 1 → overflow"),
                Arguments.of(10, -1, -6,
                        "Header 10, extended -1 → 10 + (-1<<4) = -6")
        );
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("rcodeMatrix")
    void getRcode_parameterised(int headerRcode, Integer optExtended, int expected, String _desc) {
        // -----------------------------------------------------------------
        // Build a spy of Message so we can stub getHeader() and getOPT()
        // -----------------------------------------------------------------
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
            // No OPT record
            doReturn(null).when(spyMsg).getOPT();
        }

        // -----------------------------------------------------------------
        // Execute and verify
        // -----------------------------------------------------------------
        int actual = spyMsg.getRcode();
        assertEquals(expected, actual,
                () -> "Unexpected rcode for header=" + headerRcode + ", optExtended=" + optExtended);
    }

    /* --------------------------------------------------------------------- */
    /*  Exception scenarios                                                   */
    /* --------------------------------------------------------------------- */

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