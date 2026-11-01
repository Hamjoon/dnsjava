```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit‑tests for {@link Message#toString()}.
 *
 * <p>The test suite follows the JSON test‑case description supplied with the
 * task.  Each logical group from the description is represented by one or
 * more test methods.  Public APIs are used wherever possible; reflection is
 * employed only for the few cases that require manipulation of private
 * fields (e.g. forcing a {@code null} header or corrupting a section list).
 *
 * <p>Mockito is used to create {@link OPTRecord} and {@link Message}
 * spies when we need to inject exceptional behaviour.
 */
@ExtendWith(MockitoExtension.class)
class Message_toString_Test {

    /** Fresh Message instance for every test method. */
    private Message msg;

    /** Simple A‑record used by many tests. */
    private Record aRecord;

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        /* Deterministic header ID (0x1234) makes the output stable. */
        msg = new Message(0x1234);

        Name name = Name.fromConstantString("www.example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN, 300,
                InetAddress.getByName("127.0.0.1"));
    }

    /* --------------------------------------------------------------------- *
     *  Helper utilities
     * --------------------------------------------------------------------- */

    /** Sets a private field on the given object via reflection. */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Retrieves a private field value via reflection. */
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Forces the TSIG state to the supplied constant (e.g. VERIFIED, FAILED). */
    private void setTsigState(Message m, int state) {
        setPrivateField(m, "tsigState", state);
    }

    /** Forces the message header to the supplied opcode. */
    private void setOpcode(Message m, int opcode) {
        Header h = m.getHeader();
        h.setOpcode(opcode);
    }

    /* --------------------------------------------------------------------- *
     *  Existing tests (kept unchanged)
     * --------------------------------------------------------------------- */

    @Test
    void toString_basicStructure() {
        msg.addRecord(aRecord, Section.QUESTION);
        msg.addRecord(aRecord, Section.ANSWER);

        String out = msg.toString();

        assertTrue(out.contains("0x1234"));
        assertTrue(out.contains(";; QUESTION:"));
        assertTrue(out.contains(";; ANSWER:"));
        assertTrue(out.contains(";; AUTHORITY:"));
        assertTrue(out.contains(";; ADDITIONAL:"));
        assertTrue(out.contains(aRecord.toString()));
        assertTrue(out.matches("(?s).*;; Message size: \\d+ bytes$"));
    }

    @Test
    void toString_withOPT() {
        OPTRecord opt = new OPTRecord(0);
        msg.addRecord(opt, Section.ADDITIONAL);

        String out = msg.toString();

        assertTrue(out.contains("OPT"));
        assertTrue(out.contains("0x1234"));
    }

    @Test
    void toString_withVerifiedTSIG() {
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0],
                (short) 0, 0, null);
        msg.setTSIG(key);
        // Force the internal state to VERIFIED (value 1 in Message)
        setTsigState(msg, Message.TSIG_VERIFIED);

        String out = msg.toString();

        assertTrue(out.contains(";; TSIG ok"));
    }

    @Test
    void toString_withInvalidTSIG() {
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0],
                (short) 0, 0, null);
        msg.setTSIG(key);
        // Force the internal state to FAILED (value 4 in Message)
        setTsigState(msg, Message.TSIG_FAILED);

        String out = msg.toString();

        assertTrue(out.contains(";; TSIG invalid"));
    }

    /* --------------------------------------------------------------------- *
     *  New tests covering the remaining JSON groups
     * --------------------------------------------------------------------- */

    /** 1. Message without OPT and not signed – default path. */
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

    /** 2. Message with OPT and not signed – OPT block exercised. */
    @Test
    void toString_withOPT_notSigned() {
        OPTRecord opt = new OPTRecord(0);
        msg.addRecord(opt, Section.ADDITIONAL); // makes getOPT() non‑null

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

    /** 3. Opcode UPDATE influences section titles – non‑UPDATE path. */
    @Test
    void toString_opcodeNotUpdate_usesLongString() {
        // Ensure opcode is NOT UPDATE
        setOpcode(msg, Opcode.QUERY); // any non‑UPDATE value
        String out = msg.toString();

        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.longString(i) + ":"));
        }
    }

    /** 4. Opcode UPDATE influences section titles – UPDATE path. */
    @Test
    void toString_opcodeUpdate_usesUpdString() {
        setOpcode(msg, Opcode.UPDATE);
        String out = msg.toString();

        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"));
        }
    }

    /** 5. Full combination: OPT + signed + verified + UPDATE opcode. */
    @Test
    void toString_fullCombination_OPT_signed_verified_UPDATE() {
        // 1️⃣  UPDATE opcode
        setOpcode(msg, Opcode.UPDATE);

        // 2️⃣  OPT record
        OPTRecord opt = new OPTRecord(0);
        msg.addRecord(opt, Section.ADDITIONAL);

        // 3️⃣  TSIG (verified)
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0],
                (short) 0, 0, null);
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

    /** 6. Signed + verified TSIG with UPDATE opcode (no OPT). */
    @Test
    void toString_signedVerified_UPDATE_noOPT() {
        setOpcode(msg, Opcode.UPDATE);
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0],
                (short) 0, 0, null);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_VERIFIED);

        String out = msg.toString();

        assertTrue(out.contains(";; TSIG ok"));
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"));
        }
    }

    /** 7. Signed + invalid TSIG with UPDATE opcode (no OPT). */
    @Test
    void toString_signedInvalid_UPDATE_noOPT() {
        setOpcode(msg, Opcode.UPDATE);
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0],
                (short) 0, 0, null);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_FAILED);

        String out = msg.toString();

        assertTrue(out.contains(";; TSIG invalid"));
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"));
        }
    }

    /** 8. OPT + signed + verified (non‑UPDATE opcode). */
    @Test
    void toString_OPT_signedVerified_nonUpdate() {
        // Ensure non‑UPDATE opcode
        setOpcode(msg, Opcode.QUERY);

        // OPT record
        OPTRecord opt = new OPTRecord(0);
        msg.addRecord(opt, Section.ADDITIONAL);

        // TSIG verified
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0],
                (short) 0, 0, null);
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

    /** 9. OPT + signed + invalid (non‑UPDATE opcode). */
    @Test
    void toString_OPT_signedInvalid_nonUpdate() {
        setOpcode(msg, Opcode.QUERY);
        OPTRecord opt = new OPTRecord(0);
        msg.addRecord(opt, Section.ADDITIONAL);

        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0],
                (short) 0, 0, null);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_FAILED);

        String out = msg.toString();

        assertTrue(out.contains("OPT"));
        assertTrue(out.contains(";; TSIG invalid"));
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.longString(i) + ":"));
        }
    }

    /** 10. OPT + signed + invalid with UPDATE opcode. */
    @Test
    void toString_OPT_signedInvalid_UPDATE() {
        setOpcode(msg, Opcode.UPDATE);
        OPTRecord opt = new OPTRecord(0);
        msg.addRecord(opt, Section.ADDITIONAL);

        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0],
                (short) 0, 0, null);
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

    /** 11. Null header – NPE without OPT. */
    @Test
    void toString_nullHeader_noOPT_throwsNPE() {
        // Force header to null via reflection
        setPrivateField(msg, "header", null);

        NullPointerException npe = assertThrows(NullPointerException.class, () -> msg.toString());
        assertNotNull(npe);
    }

    /** 12. Null header – NPE with OPT present. */
    @Test
    void toString_nullHeader_withOPT_throwsNPE() {
        // Insert a non‑null OPT so that getOPT() != null
        OPTRecord opt = new OPTRecord(0);
        msg.addRecord(opt, Section.ADDITIONAL);
        // Nullify the header
        setPrivateField(msg, "header", null);

        NullPointerException npe = assertThrows(NullPointerException.class, () -> msg.toString());
        assertNotNull(npe);
    }

    /** 13. Null header – NPE with signed TSIG. */
    @Test
    void toString_nullHeader_signed_throwsNPE() {
        Name keyName = Name.fromConstantString("tsig.example.");
        TSIG key = new TSIG(keyName, TSIG.HMAC_MD5, 0, 0, new byte[0],
                (short) 0, 0, null);
        msg.setTSIG(key);
        setTsigState(msg, Message.TSIG_VERIFIED);
        setPrivateField(msg, "header", null);

        NullPointerException npe = assertThrows(NullPointerException.class, () -> msg.toString());
        assertNotNull(npe);
    }

    /** 14. Section internal error – NPE from private sectionToString. */
    @Test
    void toString_sectionListNull_throwsNPE() throws Exception {
        // The internal representation of sections in dnsjava is a List<Record>[] called "sections".
        // We set one of the section arrays to null to provoke NPE inside sectionToString.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        // Corrupt the ANSWER section (index 1)
        sections[Section.ANSWER] = null;

        NullPointerException npe = assertThrows(NullPointerException.class, () -> msg.toString());
        assertNotNull(npe);
    }

    /** 15. OPTRecord.printPseudoSection throws RuntimeException – it must propagate. */
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