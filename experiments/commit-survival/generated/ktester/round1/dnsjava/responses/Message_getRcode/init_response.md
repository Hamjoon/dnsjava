```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class Message_getRcode_Test {

    private Message msg;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test; the default constructor creates a Header with a random ID.
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
        // Use a base rcode that is easy to verify (e.g., NOERROR = 0)
        header.setRcode(Rcode.NOERROR);

        // Create an OPT record with an extended rcode of 5 (binary 0101)
        // The constructor arguments are: payloadSize, extendedRcode, version, flags
        OPTRecord opt = new OPTRecord(512, 5, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        int expected = (5 << 4) + Rcode.NOERROR; // extendedRcode shifted left 4 bits
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
        header.setRcode(Rcode.FORMERR); // 1

        // extended rcode = 3 (binary 0011)
        OPTRecord opt = new OPTRecord(1024, 3, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        int expected = (3 << 4) + Rcode.FORMERR; // (3*16) + 1 = 49
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
        header.setRcode(Rcode.NOTIMP); // 4

        OPTRecord opt = new OPTRecord(256, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        int actual = msg.getRcode();
        assertEquals(Rcode.NOTIMP, actual,
                "When OPT extended rcode is zero, Message.getRcode() should equal the header rcode");
    }
}
```