```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#getRcode()}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Normal behaviour with and without an OPT record.</li>
 *   <li>Edge cases such as maximum standard and extended rcodes.</li>
 *   <li>Exception propagation when the {@link Header} or {@link OPTRecord}
 *       misbehave.</li>
 *   <li>Handling of unexpected negative extended rcodes.</li>
 *   <li>Null {@code Header} leading to {@link NullPointerException}.</li>
 * </ul>
 *
 * <p>Mockito is used to mock {@link Header} and {@link OPTRecord} where required.
 * Reflection is employed only to inject a {@code null} header (the class does not
 * provide a public setter for it).
 */
@ExtendWith(MockitoExtension.class)
class Message_getRcode_Test {

    /** Helper to create a Message with a Header whose rcode is set to the given value. */
    private Message createMessageWithRcode(int rcode) {
        Message msg = new Message();               // uses default Header
        Header hdr = msg.getHeader();
        // Header provides a public setter for the rcode (as in dnsjava)
        hdr.setRcode(rcode);
        return msg;
    }

    /** Helper to create an OPTRecord with the supplied extended rcode. */
    private OPTRecord createOPT(int extendedRcode) {
        // OPTRecord(int payloadSize, int extendedRcode, int version, int flags)
        return new OPTRecord(0, extendedRcode, 0, 0);
    }

    /** Utility to replace the private {@code header} field via reflection. */
    private void injectHeader(Message msg, Header header) throws Exception {
        Field f = Message.class.getDeclaredField("header");
        f.setAccessible(true);
        f.set(msg, header);
    }

    /* --------------------------------------------------------------------- */
    /* Existing straightforward tests (kept unchanged)                     */
    /* --------------------------------------------------------------------- */

    @Test
    void getRcode_withoutOPT_returnsHeaderRcode() {
        // Header rcode = 3 (SERVFAIL)
        Message msg = createMessageWithRcode(3);
        assertEquals(3, msg.getRcode(),
                "When no OPT record is present, getRcode() should return the header's rcode");
    }

    @Test
    void getRcode_withOPT_addsExtendedRcode() {
        // Header rcode = 2 (SERVFAIL)
        Message msg = createMessageWithRcode(2);

        // Extended rcode = 5 (bits 0‑7 of the extended field)
        OPTRecord opt = createOPT(5);
        msg.addRecord(opt, Section.ADDITIONAL);

        // Expected: header rcode + (extendedRcode << 4)
        int expected = 2 + (5 << 4);
        assertEquals(expected, msg.getRcode(),
                "When an OPT record is present, getRcode() should add (extendedRcode << 4) to the header rcode");
    }

    @Test
    void getRcode_multipleScenarios() {
        // Scenario matrix: header rcode x extended rcode
        int[] headerRcodes = {0, 1, 7};
        int[] extendedRcodes = {0, 1, 15};

        for (int h : headerRcodes) {
            for (int e : extendedRcodes) {
                Message msg = createMessageWithRcode(h);
                OPTRecord opt = createOPT(e);
                msg.addRecord(opt, Section.ADDITIONAL);
                int expected = h + (e << 4);
                assertEquals(expected, msg.getRcode(),
                        String.format("Header rcode=%d, extended=%d should yield %d", h, e, expected));
            }
        }
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised tests derived from the supplied JSON test cases        */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "No OPT – headerRcode={0} → expected={1}")
    @CsvSource({
            "0, 0",
            "2, 2"
    })
    void getRcode_noOPT_variousHeaderValues(int headerRcode, int expected) {
        Message msg = createMessageWithRcode(headerRcode);
        assertEquals(expected, msg.getRcode(),
                "When no OPT record is present, the method must return the header rcode unchanged.");
    }

    @Test
    void getRcode_OPT_present_extendedZero() {
        Message msg = createMessageWithRcode(5);
        OPTRecord opt = createOPT(0);
        msg.addRecord(opt, Section.ADDITIONAL);
        assertEquals(5, msg.getRcode(),
                "Extended rcode 0 should not affect the result.");
    }

    @ParameterizedTest(name = "OPT present – header={0}, ext={1} → expected={2}")
    @CsvSource({
            // headerRcode, extendedRcode, expectedResult
            "0, 1, 16",
            "3, 2, 35",
            "15, 3, 63",   // note: description in JSON had a typo (75); correct calculation is 63
            "15, 60, 975"
    })
    void getRcode_OPT_present_variousValues(int headerRcode, int extendedRcode, int expected) {
        Message msg = createMessageWithRcode(headerRcode);
        OPTRecord opt = createOPT(extendedRcode);
        msg.addRecord(opt, Section.ADDITIONAL);
        assertEquals(expected, msg.getRcode(),
                String.format("Header=%d, extended=%d should yield %d", headerRcode, extendedRcode, expected));
    }

    @Test
    void getRcode_headerMaxNoOPT() {
        Message msg = createMessageWithRcode(15);
        assertEquals(15, msg.getRcode(),
                "Maximum standard DNS rcode (15) without OPT must be returned unchanged.");
    }

    @Test
    void getRcode_headerMaxWithZeroExtended() {
        Message msg = createMessageWithRcode(15);
        OPTRecord opt = createOPT(0);
        msg.addRecord(opt, Section.ADDITIONAL);
        assertEquals(15, msg.getRcode(),
                "Maximum header rcode with zero extended rcode must stay 15.");
    }

    @Test
    void getRcode_maxExtendedRcode() {
        Message msg = createMessageWithRcode(15);
        OPTRecord opt = createOPT(255); // 8‑bit max
        msg.addRecord(opt, Section.ADDITIONAL);
        int expected = 15 + (255 << 4); // 15 + 4080 = 4095
        assertEquals(4095, msg.getRcode(),
                "Maximum extended rcode (255) should be shifted and added correctly.");
    }

    @Test
    void getRcode_negativeExtendedRcode() {
        Message msg = createMessageWithRcode(2);
        // Using a mock to return a negative extended rcode
        OPTRecord opt = mock(OPTRecord.class);
        when(opt.getExtendedRcode()).thenReturn(-1);
        msg.addRecord(opt, Section.ADDITIONAL);
        int expected = 2 + (-1 << 4); // 2 - 16 = -14
        assertEquals(-14, msg.getRcode(),
                "Negative extended rcode should be shifted and added, yielding a negative result.");
    }

    /* --------------------------------------------------------------------- */
    /* Exception‑propagation tests                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void getRcode_nullHeader_throwsNullPointerException() throws Exception {
        Message msg = new Message(); // default Header will be replaced
        injectHeader(msg, null);     // force a null header
        assertThrows(NullPointerException.class, msg::getRcode,
                "Calling getRcode() with a null Header should throw NPE.");
    }

    @Test
    void getRcode_headerRcodeThrowsException_propagates() throws Exception {
        Header mockHeader = mock(Header.class);
        when(mockHeader.getRcode()).thenThrow(new IllegalArgumentException("bad rcode"));
        Message msg = new Message();
        injectHeader(msg, mockHeader);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, msg::getRcode);
        assertEquals("bad rcode", ex.getMessage());
    }

    @Test
    void getRcode_getOPTThrowsException_propagates() {
        // Spy the Message to override getOPT()
        Message spyMsg = spy(new Message());
        doThrow(new NullPointerException("OPT failure")).when(spyMsg).getOPT();
        // Ensure header is present so that the NPE originates from getOPT()
        Header hdr = spyMsg.getHeader();
        hdr.setRcode(0);
        assertThrows(NullPointerException.class, spyMsg::getRcode,
                "Exception from getOPT() must propagate unchanged.");
    }

    @Test
    void getRcode_optExtendedRcodeThrowsException_propagates() {
        Message msg = new Message();
        // Mock OPTRecord to throw when getExtendedRcode() is called
        OPTRecord mockOpt = mock(OPTRecord.class);
        when(mockOpt.getExtendedRcode()).thenThrow(new IllegalStateException("bad extended"));
        // Add the mocked OPTRecord to the message
        msg.addRecord(mockOpt, Section.ADDITIONAL);
        // Header rcode can be anything; use 0 for simplicity
        msg.getHeader().setRcode(0);
        IllegalStateException ex = assertThrows(IllegalStateException.class, msg::getRcode);
        assertEquals("bad extended", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* Lifecycle hooks (unchanged)                                           */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required beyond what each test creates.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }
}
```