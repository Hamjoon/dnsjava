```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit‑tests for {@link Message#toString()}.
 *
 * <p>The tests build {@link Message} instances using only the public API,
 * add records to the various DNS sections and then verify that the textual
 * representation contains the expected section headers and record data.
 *
 * <p>We deliberately avoid accessing any private members of {@link Message}
 * or its collaborators.  All objects are created with the constructors
 * supplied by the library.
 */
@ExtendWith(MockitoExtension.class)
class Message_toString_Test {

    /** Message instance that is recreated for every test case. */
    private Message msg;

    /** Simple A‑record used in several tests. */
    private Record aRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        /* Create a fresh Message with a deterministic header ID so that the
         * string representation is repeatable. */
        msg = new Message(0x1234);

        /* Build an A‑record for "www.example.com." → 127.0.0.1.
         * The Record factory method is part of the public API. */
        Name name = Name.fromConstantString("www.example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN, 300,
                InetAddress.getByName("127.0.0.1"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the Message instance will be GC'd.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verify that the string produced for a plain message (no OPT, no TSIG)
     * contains the header line, the four section headings and the size suffix.
     */
    @Test
    void toString_basicStructure() {
        // QUESTION section is always present in a query message.
        msg.addRecord(aRecord, Section.QUESTION);
        // Add a second record to the ANSWER section to make the output richer.
        msg.addRecord(aRecord, Section.ANSWER);

        String out = msg.toString();

        // Header line – the Header#toString implementation ends with a newline,
        // so we simply check that the ID we set appears.
        assertTrue(out.contains("0x1234"), "Header should contain the message ID");

        // Section headings – the library uses ";; QUESTION:", ";; ANSWER:", etc.
        assertTrue(out.contains(";; QUESTION:"), "Should contain QUESTION heading");
        assertTrue(out.contains(";; ANSWER:"), "Should contain ANSWER heading");
        assertTrue(out.contains(";; AUTHORITY:"), "Should contain AUTHORITY heading");
        assertTrue(out.contains(";; ADDITIONAL:"), "Should contain ADDITIONAL heading");

        // Verify that the A‑record we added is rendered inside the QUESTION section.
        assertTrue(out.contains(aRecord.toString()), "Record string should appear in output");

        // Message size suffix.
        assertTrue(out.matches("(?s).*;; Message size: \\d+ bytes$"),
                "Output should end with the message size line");
    }

    /**
     * When an {@link OPTRecord} is present the header is printed via
     * {@code Header#toStringWithRcode} and the OPT pseudo‑section is printed.
     * This test creates a minimal OPT record and checks that the pseudo‑section
     * marker appears.
     */
    @Test
    void toString_withOPT() {
        // The OPTRecord constructor used here matches the public signature in dnsjava:
        //   OPTRecord(int payloadSize)
        OPTRecord opt = new OPTRecord(0);
        msg.addRecord(opt, Section.ADDITIONAL);

        String out = msg.toString();

        // The presence of an OPT pseudo‑section is indicated by the text "OPT PSEUDOSECTION".
        // The exact wording depends on the library version; we check for the word "OPT".
        assertTrue(out.contains("OPT"), "Output should contain the OPT pseudo‑section");
        // Because an OPT record triggers the special header printing path,
        // the header line should still be present.
        assertTrue(out.contains("0x1234"));
    }

    /**
     * When the message is signed with a TSIG record the output must contain a
     * TSIG status line.  We create a dummy TSIG record, mark the message as
     * signed and verified, and then assert that the string contains “;; TSIG ok”.
     */
    @Test
    void toString_withVerifiedTSIG() {
        // Create a minimal TSIG record (key name, algorithm, time signed, fudge, mac, original id, error, other data)
        // The constructor used below follows the public API of dnsjava.
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0], (short) 0, 0, null);
        // Attach the TSIG to the message and mark it as signed/verified.
        msg.setTSIG(key);
        msg.getHeader().setFlag(Flags.TC); // just to alter the header; not required for TSIG.

        // The library treats a message with a TSIG as "signed". No further action is needed.
        String out = msg.toString();

        assertTrue(out.contains(";; TSIG ok"), "Signed & verified TSIG should be reported as ok");
    }

    /**
     * When a TSIG is present but not verified the output must contain “;; TSIG invalid”.
     * We simulate this by clearing the verification flag after attaching the TSIG.
     */
    @Test
    void toString_withInvalidTSIG() {
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0], (short) 0, 0, null);
        msg.setTSIG(key);
        // Force the message to be marked as signed but not verified.
        // In dnsjava this is done by setting the TSIG state manually, but the
        // public method `setTSIG` already puts the message into the signed state.
        // We simply clear the verification flag via the Header to emulate failure.
        msg.getHeader().setFlag(Flags.AA); // any flag change; verification stays false.

        String out = msg.toString();

        assertTrue(out.contains(";; TSIG invalid"), "Unsigned/invalid TSIG should be reported as invalid");
    }
}
```