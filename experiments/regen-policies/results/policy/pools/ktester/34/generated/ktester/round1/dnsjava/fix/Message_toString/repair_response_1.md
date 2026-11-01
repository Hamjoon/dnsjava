```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_toString_Test {

    /**
     * Message instance that is recreated for every test case.
     */
    private Message msg;

    /**
     * Simple A‑record used in several tests.
     */
    private Record aRecord;

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        /* Create a fresh Message with a deterministic header ID so that the
         * string representation is repeatable. */
        msg = new Message(0x1234);
        /* Build an A‑record for "www.example.com." → 127.0.0.1.
         * Use the concrete ARecord class – the generic Record.newRecord
         * overload expects a byte[] for rdata, not an InetAddress. */
        Name name = Name.fromConstantString("www.example.com.");
        aRecord = new ARecord(name, DClass.IN, 300, InetAddress.getByName("127.0.0.1"));
    }

    /**
     * Sets a private field on the given object via reflection.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves a private field value via reflection.
     */
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Forces the TSIG state to the supplied constant (e.g. VERIFIED, FAILED).
     */
    private void setTsigState(Message m, int state) {
        setPrivateField(m, "tsigState", state);
    }

    /**
     * Forces the message header to the supplied opcode.
     */
    private void setOpcode(Message m, int opcode) {
        Header h = m.getHeader();
        h.setOpcode(opcode);
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
        // so we simply check that the ID we set appears (hex or decimal is fine).
        assertTrue(out.contains("0x1234") || out.contains("4660"),
                "Header should contain the message ID");
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
        // The no‑arg OPTRecord constructor creates an empty OPT record.
        OPTRecord opt = new OPTRecord();
        msg.addRecord(opt, Section.ADDITIONAL);
        String out = msg.toString();
        // The presence of an OPT pseudo‑section is indicated by the text "OPT".
        assertTrue(out.contains("OPT"), "Output should contain the OPT pseudo‑section");
        // Because an OPT record triggers the special header printing path,
        // the header line should still be present.
        assertTrue(out.contains("0x1234") || out.contains("4660"));
    }

    /**
     * When the message is signed with a TSIG record the output must contain a
     * TSIG status line.  We create a dummy TSIG record, mark the message as
     * signed and verified, and then assert that the string contains “;; TSIG ok”.
     */
    @Test
    void toString_withVerifiedTSIG() {
        // Create a minimal TSIG record.  The constructor taking (Name, String, byte[])
        // matches the current dnsjava API – the algorithm name is "hmac-md5".
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, "hmac-md5", new byte[0]);
        // Attach the TSIG to the message and mark it as verified.
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_VERIFIED);
        // just to alter the header; not required for TSIG.
        msg.getHeader().setFlag(Flags.TC);
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
        TSIG key = new TSIG(keyName, "hmac-md5", new byte[0]);
        msg.setTSIG(key);
        // Force the message to be marked as signed but not verified.
        setTsigState(msg, Message.TSIG_FAILED);
        String out = msg.toString();
        assertTrue(out.contains(";; TSIG invalid"), "Unsigned/invalid TSIG should be reported as invalid");
    }

    /**
     * 1. Message without OPT and not signed – default path.
     */
    @Test
    void toString_noOPT_notSigned() {
        // No OPT, no TSIG – just a plain message.
        msg.addRecord(aRecord, Section.QUESTION);
        String out = msg.toString();
        // Header printed via Header#toString()
        assertTrue(out.startsWith(msg.getHeader().toString()));
        // No TSIG line
        assertFalse(out.contains(";; TSIG"));
        // Four section headings using longString()
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.longString(i) + ":"));
        }
        // Ends with size line
        assertTrue(out.matches("(?s).*;; Message size: \\d+ bytes$"));
    }

    /**
     * 2. Message with OPT and not signed – OPT block exercised.
     */
    @Test
    void toString_withOPT_notSigned() {
        OPTRecord opt = new OPTRecord();
        // makes getOPT() non‑null
        msg.addRecord(opt, Section.ADDITIONAL);
        String out = msg.toString();
        // Header printed via toStringWithRcode()
        assertTrue(out.contains(msg.getHeader().toStringWithRcode(msg.getRcode())));
        // Two newlines after the header
        assertTrue(out.contains("\n\n"));
        // OPT pseudo‑section appears (the exact text is implementation‑dependent)
        assertTrue(out.contains("OPT"));
        // No TSIG line
        assertFalse(out.contains(";; TSIG"));
        // Four longString section headings
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.longString(i) + ":"));
        }
    }

    /**
     * 3. Opcode UPDATE influences section titles – non‑UPDATE path.
     */
    @Test
    void toString_opcodeNotUpdate_usesLongString() {
        // Ensure opcode is NOT UPDATE
        // any non‑UPDATE value
        setOpcode(msg, Opcode.QUERY);
        String out = msg.toString();
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.longString(i) + ":"));
        }
    }

    /**
     * 4. Opcode UPDATE influences section titles – UPDATE path.
     */
    @Test
    void toString_opcodeUpdate_usesUpdString() {
        setOpcode(msg, Opcode.UPDATE);
        String out = msg.toString();
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"));
        }
    }

    /**
     * 5. Full combination: OPT + signed + verified + UPDATE opcode.
     */
    @Test
    void toString_fullCombination_OPT_signed_verified_UPDATE() {
        // 1️⃣  UPDATE opcode
        setOpcode(msg, Opcode.UPDATE);
        // 2️⃣  OPT record
        OPTRecord opt = new OPTRecord();
        msg.addRecord(opt, Section.ADDITIONAL);
        // 3️⃣  TSIG (verified)
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, "hmac-md5", new byte[0]);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_VERIFIED);
        String out = msg.toString();
        // Header with Rcode + double newline
        assertTrue(out.startsWith(msg.getHeader().toStringWithRcode(msg.getRcode()) + "\n\n"));
        // OPT pseudo‑section appears
        assertTrue(out.contains("OPT"));
        // TSIG ok line appears after the OPT block
        assertTrue(out.contains(";; TSIG ok"));
        // Section titles must be UPDATE‑specific
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"));
        }
        // Ends with size line
        assertTrue(out.matches("(?s).*;; Message size: \\d+ bytes$"));
    }

    /**
     * 6. Signed + verified TSIG with UPDATE opcode (no OPT).
     */
    @Test
    void toString_signedVerified_UPDATE_noOPT() {
        setOpcode(msg, Opcode.UPDATE);
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, "hmac-md5", new byte[0]);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_VERIFIED);
        String out = msg.toString();
        assertTrue(out.contains(";; TSIG ok"));
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"));
        }
    }

    /**
     * 7. Signed + invalid TSIG with UPDATE opcode (no OPT).
     */
    @Test
    void toString_signedInvalid_UPDATE_noOPT() {
        setOpcode(msg, Opcode.UPDATE);
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, "hmac-md5", new byte[0]);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_FAILED);
        String out = msg.toString();
        assertTrue(out.contains(";; TSIG invalid"));
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"));
        }
    }

    /**
     * 8. OPT + signed + verified (non‑UPDATE opcode).
     */
    @Test
    void toString_OPT_signedVerified_nonUpdate() {
        // Ensure non‑UPDATE opcode
        setOpcode(msg, Opcode.QUERY);
        // OPT record
        OPTRecord opt = new OPTRecord();
        msg.addRecord(opt, Section.ADDITIONAL);
        // TSIG verified
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, "hmac-md5", new byte[0]);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_VERIFIED);
        String out = msg.toString();
        // Header with Rcode & double newline
        assertTrue(out.startsWith(msg.getHeader().toStringWithRcode(msg.getRcode()) + "\n\n"));
        // OPT pseudo‑section
        assertTrue(out.contains("OPT"));
        // TSIG ok line after OPT
        assertTrue(out.contains(";; TSIG ok"));
        // Section titles must be longString (non‑UPDATE)
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.longString(i) + ":"));
        }
    }

    /**
     * 9. OPT + signed + invalid (non‑UPDATE opcode).
     */
    @Test
    void toString_OPT_signedInvalid_nonUpdate() {
        setOpcode(msg, Opcode.QUERY);
        OPTRecord opt = new OPTRecord();
        msg.addRecord(opt, Section.ADDITIONAL);
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, "hmac-md5", new byte[0]);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_FAILED);
        String out = msg.toString();
        assertTrue(out.contains("OPT"));
        assertTrue(out.contains(";; TSIG invalid"));
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.longString(i) + ":"));
        }
    }

    /**
     * 10. OPT + signed + invalid with UPDATE opcode.
     */
    @Test
    void toString_OPT_signedInvalid_UPDATE() {
        setOpcode(msg, Opcode.UPDATE);
        OPTRecord opt = new OPTRecord();
        msg.addRecord(opt, Section.ADDITIONAL);
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, "hmac-md5", new byte[0]);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_FAILED);
        String out = msg.toString();
        assertTrue(out.startsWith(msg.getHeader().toStringWithRcode(msg.getRcode()) + "\n\n"));
        assertTrue(out.contains("OPT"));
        assertTrue(out.contains(";; TSIG invalid"));
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"));
        }
    }

    /**
     * 11. Null header – NPE without OPT.
     */
    @Test
    void toString_nullHeader_noOPT_throwsNPE() {
        // Force header to null via reflection
        setPrivateField(msg, "header", null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> msg.toString());
        assertNotNull(npe);
    }

    /**
     * 12. Null header – NPE with OPT present.
     */
    @Test
    void toString_nullHeader_withOPT_throwsNPE() {
        // Insert a non‑null OPT so that getOPT() != null
        OPTRecord opt = new OPTRecord();
        msg.addRecord(opt, Section.ADDITIONAL);
        // Nullify the header
        setPrivateField(msg, "header", null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> msg.toString());
        assertNotNull(npe);
    }

    /**
     * 13. Null header – NPE with signed TSIG.
     */
    @Test
    void toString_nullHeader_signed_throwsNPE() {
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, "hmac-md5", new byte[0]);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_VERIFIED);
        setPrivateField(msg, "header", null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> msg.toString());
        assertNotNull(npe);
    }

    /**
     * 14. Section internal error – NPE from private sectionToString.
     */
    @Test
    void toString_sectionListNull_throwsNPE() throws Exception {
        // The internal representation of sections in dnsjava is a List<Record>[] called "sections".
        // We set the whole array to null to provoke NPE inside sectionToString.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(msg, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> msg.toString());
        assertNotNull(npe);
    }

    /**
     * 15. OPTRecord.printPseudoSection throws RuntimeException – it must propagate.
     */
    @Test
    void toString_optPrintPseudoSection_throwsRuntimeException() {
        // Create a mock OPTRecord that throws when printPseudoSection is invoked.
        OPTRecord mockOpt = mock(OPTRecord.class);
        doThrow(new IllegalStateException("pseudo‑section failure"))
                .when(mockOpt).printPseudoSection(any(StringBuilder.class));
        // Spy the Message so we can stub getOPT() to return the mock.
        Message spyMsg = spy(new Message(0x1234));
        doReturn(mockOpt).when(spyMsg).getOPT();
        IllegalStateException ex = assertThrows(IllegalStateException.class, spyMsg::toString);
        assertEquals("pseudo‑section failure", ex.getMessage());
    }
}
```