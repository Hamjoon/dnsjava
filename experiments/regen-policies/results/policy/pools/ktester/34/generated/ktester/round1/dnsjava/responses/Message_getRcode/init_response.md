```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

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