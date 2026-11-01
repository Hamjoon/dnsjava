```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link Message#toString()}.
 *
 * The tests cover all branches described in the JSON test‑case specification:
 *
 * <ul>
 *   <li>OPT handling (present / absent)</li>
 *   <li>TSIG handling (signed / unsigned, verified / not verified)</li>
 *   <li>Opcode‑specific section headings (QUERY vs UPDATE)</li>
 *   <li>Header null safety</li>
 *   <li>Section content rendering</li>
 *   <li>Custom Rcode handling</li>
 *   <li>Exception propagation from dependent objects</li>
 *   <li>Full‑string integration scenario</li>
 * </ul>
 *
 * Mockito is used to stub {@link Header}, {@link OPTRecord} and other collaborators.
 * Reflection is employed only to set the private {@code tsigState} field that drives
 * {@code isSigned()} / {@code isVerified()} logic.
 */
@ExtendWith(MockitoExtension.class)
class Message_toString_Test {

    /* --------------------------------------------------------------------- */
    /* Helper utilities */
    /* --------------------------------------------------------------------- */

    /** Set a private int field on {@code target}. */
    private static void setPrivateInt(Object target, String fieldName, int value)
            throws Exception {
        Field f = Message.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    /** Retrieve the string representation of a Message for assertions. */
    private static String toString(Message m) {
        return m.toString();
    }

    /* --------------------------------------------------------------------- */
    /* Existing basic tests (kept unchanged) */
    /* --------------------------------------------------------------------- */

    @Test
    void toString_basicMessage_containsSectionsAndRecords() throws Exception {
        Message msg = new Message();                     // empty header, empty sections
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        msg.addRecord(question, Section.QUESTION);

        // add an answer record
        Record answer = new ARecord(qname, DClass.IN, 3600,
                InetAddress.getByName("1.2.3.4"));
        msg.addRecord(answer, Section.ANSWER);

        String out = msg.toString();

        // Section headings
        assertTrue(out.contains(";; QUESTION:"), "Missing QUESTION heading");
        assertTrue(out.contains(";; ANSWER:"),   "Missing ANSWER heading");

        // The question name should appear in the output
        assertTrue(out.contains(qname.toString()), "Question name not present");

        // The answer address should appear (ARecord prints the address)
        assertTrue(out.contains("1.2.3.4"), "Answer address not present");
    }

    @Test
    void toString_withOPT_containsPseudoSection() {
        Message msg = new Message();
        // Create a minimal OPT record (payload size 4096, no other fields)
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        String out = msg.toString();

        // The OPT pseudo‑section is printed by OPTRecord.printPseudoSection().
        // The exact format is implementation dependent, but the string will
        // contain the word "OPT" and the payload size.
        assertTrue(out.contains("OPT"), "OPT pseudo‑section header missing");
        assertTrue(out.contains("4096"), "OPT payload size not rendered");
    }

    /* --------------------------------------------------------------------- */
    /* 1. OPT handling */
    /* --------------------------------------------------------------------- */

    @Test
    void opt_absent_usesHeaderToString() {
        // Mock a Header that returns a known string from toString()
        Header mockHeader = mock(Header.class);
        when(mockHeader.toString()).thenReturn("MOCKED HEADER");
        Message msg = new Message();
        msg.setHeader(mockHeader); // set non‑null header
        // No OPT record added -> getOPT() will be null

        String out = toString(msg);

        assertTrue(out.startsWith("MOCKED HEADER"), "Header toString() not used when OPT is absent");
        // No blank line after header when OPT is null (only a single '\n')
        assertTrue(out.contains("\n\n") == false, "Unexpected blank line when OPT is null");
    }

    @Test
    void opt_present_usesHeaderToStringWithRcode_andPrintPseudoSection() {
        Header mockHeader = mock(Header.class);
        when(mockHeader.toStringWithRcode(anyInt())).thenReturn("HEADER WITH RCODE");
        // getRcode() is used only to pass to toStringWithRcode()
        when(mockHeader.getRcode()).thenReturn(2);
        Message msg = new Message();
        msg.setHeader(mockHeader);

        // Create a spy OPTRecord so we can verify printPseudoSection()
        OPTRecord opt = spy(new OPTRecord(1234, 0, 0, 0));
        doAnswer(invocation -> {
            StringBuilder sb = invocation.getArgument(0);
            sb.append("<<OPT PSEUDO SECTION>>");
            return null;
        }).when(opt).printPseudoSection(any(StringBuilder.class));

        msg.addRecord(opt, Section.ADDITIONAL);

        String out = toString(msg);

        // Header line should be the one from toStringWithRcode()
        assertTrue(out.startsWith("HEADER WITH RCODE"), "Header toStringWithRcode not used when OPT present");
        // Blank line after header
        assertTrue(out.contains("HEADER WITH RCODE\n\n"), "Missing blank line after header when OPT present");
        // Pseudo‑section text must appear
        assertTrue(out.contains("<<OPT PSEUDO SECTION>>"), "OPT pseudo‑section text missing");
    }

    /* --------------------------------------------------------------------- */
    /* 2. TSIG handling */
    /* --------------------------------------------------------------------- */

    @Test
    void tsig_unsigned_noTsigLine() throws Exception {
        Message msg = new Message();
        // tsigState defaults to TSIG_UNSIGNED (0) -> isSigned() false
        String out = toString(msg);
        assertFalse(out.contains(";; TSIG"), "TSIG line should not be present when message is unsigned");
    }

    @Test
    void tsig_signed_and_verified_printsOk() throws Exception {
        Message msg = new Message();
        // Set tsigState to VERIFIED (1)
        setPrivateInt(msg, "tsigState", Message.TSIG_VERIFIED);
        String out = toString(msg);
        assertTrue(out.contains(";; TSIG ok"), "TSIG ok line expected for signed & verified message");
    }

    @Test
    void tsig_signed_not_verified_printsInvalid() throws Exception {
        Message msg = new Message();
        // Set tsigState to SIGNED (3) – signed but not yet verified
        setPrivateInt(msg, "tsigState", Message.TSIG_SIGNED);
        String out = toString(msg);
        assertTrue(out.contains(";; TSIG invalid"), "TSIG invalid line expected for signed but not verified message");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Section header handling (Opcode) */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "Opcode {0} uses {1} for section headings")
    @CsvSource({
            "0, longString",   // Opcode 0 = QUERY (not UPDATE)
            String.valueOf(Opcode.UPDATE) + ", updString"
    })
    void opcode_branch_sectionHeaders(int opcode, String expectedMethod) throws Exception {
        Header mockHeader = mock(Header.class);
        when(mockHeader.getOpcode()).thenReturn(opcode);
        when(mockHeader.toString()).thenReturn("HDR");
        Message msg = new Message();
        msg.setHeader(mockHeader);

        String out = toString(msg);

        // Verify that each of the four section headings uses the correct static method.
        for (int i = 0; i < 4; i++) {
            String prefix = ";; ";
            String expectedHeading = expectedMethod.equals("longString")
                    ? Section.longString(i)
                    : Section.updString(i);
            assertTrue(out.contains(prefix + expectedHeading + ":"), "Incorrect heading for section " + i);
        }
    }

    /* --------------------------------------------------------------------- */
    /* 4. Full‑string integration */
    /* --------------------------------------------------------------------- */

    @Test
    void fullStringIntegration_allBranches() throws Exception {
        // Mock Header to provide custom strings and opcode UPDATE
        Header mockHeader = mock(Header.class);
        when(mockHeader.toStringWithRcode(anyInt())).thenReturn("HEADER WITH RCODE");
        when(mockHeader.getOpcode()).thenReturn(Opcode.UPDATE);
        when(mockHeader.getRcode()).thenReturn(99);
        when(mockHeader.toString()).thenReturn("HEADER DEFAULT");
        Message msg = new Message();
        msg.setHeader(mockHeader);

        // OPT record
        OPTRecord opt = spy(new OPTRecord(4096, 0, 0, 0));
        doAnswer(inv -> {
            StringBuilder sb = inv.getArgument(0);
            sb.append("<<OPT SECTION>>");
            return null;
        }).when(opt).printPseudoSection(any(StringBuilder.class));
        msg.addRecord(opt, Section.ADDITIONAL);

        // TSIG signed and verified
        setPrivateInt(msg, "tsigState", Message.TSIG_VERIFIED);

        // Add a simple answer record to verify section content rendering
        Name name = Name.fromConstantString("example.org.");
        Record answer = new ARecord(name, DClass.IN, 300,
                InetAddress.getByName("5.6.7.8"));
        msg.addRecord(answer, Section.ANSWER);

        String out = toString(msg);

        // Header line (with rcode) + blank line
        assertTrue(out.startsWith("HEADER WITH RCODE\n\n"), "Header line with Rcode missing");

        // OPT pseudo‑section
        assertTrue(out.contains("<<OPT SECTION>>"), "OPT pseudo‑section not rendered");

        // TSIG ok line
        assertTrue(out.contains(";; TSIG ok\n"), "TSIG ok line missing");

        // Section headings must use updString because opcode is UPDATE
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"), "UPDATE heading missing for section " + i);
        }

        // The answer record's string representation must appear after the ANSWER heading
        assertTrue(out.contains(answer.toString()), "Answer record not rendered");

        // Final size line must be present and match Message.numBytes()
        int expectedSize = msg.numBytes();
        assertTrue(out.endsWith(";; Message size: " + expectedSize + " bytes"),
                "Final size line incorrect");
    }

    /* --------------------------------------------------------------------- */
    /* 5. OPT + TSIG combination */
    /* --------------------------------------------------------------------- */

    @Test
    void optAndTsig_invalidCombination() throws Exception {
        Header mockHeader = mock(Header.class);
        when(mockHeader.toStringWithRcode(anyInt())).thenReturn("HDR RCODE");
        when(mockHeader.getRcode()).thenReturn(0);
        when(mockHeader.getOpcode()).thenReturn(0);
        Message msg = new Message();
        msg.setHeader(mockHeader);

        // OPT record
        OPTRecord opt = spy(new OPTRecord(2048, 0, 0, 0));
        doAnswer(inv -> {
            StringBuilder sb = inv.getArgument(0);
            sb.append("OPT PSEUDO");
            return null;
        }).when(opt).printPseudoSection(any(StringBuilder.class));
        msg.addRecord(opt, Section.ADDITIONAL);

        // Signed but not verified
        setPrivateInt(msg, "tsigState", Message.TSIG_SIGNED);

        String out = toString(msg);

        assertTrue(out.contains("HDR RCODE\n\n"), "Header with Rcode missing");
        assertTrue(out.contains("OPT PSEUDO"), "OPT pseudo‑section missing");
        assertTrue(out.contains(";; TSIG invalid\n"), "TSIG invalid line missing");
    }

    @Test
    void tsigOkNoOpt() throws Exception {
        Header mockHeader = mock(Header.class);
        when(mockHeader.toString()).thenReturn("HDR");
        when(mockHeader.getOpcode()).thenReturn(0);
        Message msg = new Message();
        msg.setHeader(mockHeader);

        // Signed and verified
        setPrivateInt(msg, "tsigState", Message.TSIG_VERIFIED);

        String out = toString(msg);

        // No OPT pseudo‑section
        assertFalse(out.contains("OPT"), "OPT pseudo‑section should be absent");
        // Header line should be from Header.toString() because getOPT() is null
        assertTrue(out.startsWith("HDR\n"), "Header default toString not used");
        // TSIG ok line present
        assertTrue(out.contains(";; TSIG ok\n"), "TSIG ok line missing");
    }

    /* --------------------------------------------------------------------- */
    /* 6. Header null safety */
    /* --------------------------------------------------------------------- */

    @Test
    void header_null_throwsNPE() {
        Message msg = new Message();
        // Force header to null via reflection (setHeader(null) would also work)
        msg.setHeader(null);
        assertThrows(NullPointerException.class, msg::toString,
                "Expected NPE when header is null");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Section content rendering */
    /* --------------------------------------------------------------------- */

    @Test
    void answerSection_containsRecordString() throws Exception {
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        when(mockHeader.toString()).thenReturn("HDR");
        when(mockHeader.getOpcode()).thenReturn(0);
        msg.setHeader(mockHeader);

        Name name = Name.fromConstantString("test.com.");
        Record a = new ARecord(name, DClass.IN, 60,
                InetAddress.getByName("9.9.9.9"));
        msg.addRecord(a, Section.ANSWER);

        String out = toString(msg);

        // Locate the ANSWER heading
        String answerHeading = ";; " + Section.longString(Section.ANSWER) + ":";
        int idx = out.indexOf(answerHeading);
        assertTrue(idx >= 0, "ANSWER heading not found");

        // The record's string should appear after the heading (allowing a newline)
        String afterHeading = out.substring(idx + answerHeading.length()).trim();
        assertTrue(afterHeading.contains(a.toString()), "Answer record string not rendered after heading");
    }

    /* --------------------------------------------------------------------- */
    /* 8. Custom Rcode handling */
    /* --------------------------------------------------------------------- */

    @Test
    void customRcode_usedWhenOPTPresent() {
        Header mockHeader = mock(Header.class);
        when(mockHeader.getRcode()).thenReturn(2);
        when(mockHeader.toStringWithRcode(2)).thenReturn("CUSTOM HEADER RCODE=2");
        when(mockHeader.getOpcode()).thenReturn(0);
        Message msg = new Message();
        msg.setHeader(mockHeader);

        // Add an OPT record to force the toStringWithRcode path
        OPTRecord opt = new OPTRecord(512, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        String out = toString(msg);

        assertTrue(out.startsWith("CUSTOM HEADER RCODE=2\n\n"),
                "Header line should use toStringWithRcode with custom rcode");
    }

    /* --------------------------------------------------------------------- */
    /* 9. Opcode UPDATE with TSIG invalid and no OPT */
    /* --------------------------------------------------------------------- */

    @Test
    void opcodeUpdate_unsignedOptAbsent_invalidTsig() throws Exception {
        Header mockHeader = mock(Header.class);
        when(mockHeader.getOpcode()).thenReturn(Opcode.UPDATE);
        when(mockHeader.toString()).thenReturn("HDR");
        Message msg = new Message();
        msg.setHeader(mockHeader);

        // Signed but not verified
        setPrivateInt(msg, "tsigState", Message.TSIG_SIGNED);

        String out = toString(msg);

        // Section headings must use updString
        for (int i = 0; i < 4; i++) {
            assertTrue(out.contains(";; " + Section.updString(i) + ":"), "UPDATE heading missing for section " + i);
        }
        // TSIG invalid line
        assertTrue(out.contains(";; TSIG invalid\n"), "TSIG invalid line missing");
        // No OPT pseudo‑section
        assertFalse(out.contains("OPT"), "OPT pseudo‑section should not be present");
    }

    /* --------------------------------------------------------------------- */
    /* 10. Exception propagation from OPTRecord.printPseudoSection */
    /* --------------------------------------------------------------------- */

    @Test
    void optPrintPseudoSection_throwsException_propagates() {
        Header mockHeader = mock(Header.class);
        when(mockHeader.toStringWithRcode(anyInt())).thenReturn("HDR");
        when(mockHeader.getRcode()).thenReturn(0);
        when(mockHeader.getOpcode()).thenReturn(0);
        Message msg = new Message();
        msg.setHeader(mockHeader);

        // Spy OPTRecord that throws when printPseudoSection is called
        OPTRecord opt = spy(new OPTRecord(123, 0, 0, 0));
        doThrow(new IllegalStateException("boom")).when(opt).printPseudoSection(any(StringBuilder.class));
        msg.addRecord(opt, Section.ADDITIONAL);

        IllegalStateException ex = assertThrows(IllegalStateException.class, msg::toString,
                "Exception from OPTRecord.printPseudoSection should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 11. Exception propagation from Header.toStringWithRcode */
    /* --------------------------------------------------------------------- */

    @Test
    void headerToStringWithRcode_throwsException_propagates() {
        Header mockHeader = mock(Header.class);
        when(mockHeader.getRcode()).thenReturn(999);
        when(mockHeader.toStringWithRcode(999)).thenThrow(new IllegalArgumentException("bad rcode"));
        when(mockHeader.getOpcode()).thenReturn(0);
        Message msg = new Message();
        msg.setHeader(mockHeader);

        // Add an OPT to force the toStringWithRcode call
        OPTRecord opt = new OPTRecord(256, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, msg::toString,
                "Exception from Header.toStringWithRcode should propagate");
        assertEquals("bad rcode", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 12. Internal error in private sectionToString */
    /* --------------------------------------------------------------------- */

    @Test
    void internalSectionToString_nullList_throwsNPE() throws Exception {
        // We cannot directly mock the private method, but we can simulate the
        // condition by clearing a section's internal list via reflection.
        Message msg = new Message();
        Header mockHeader = mock(Header.class);
        when(mockHeader.getOpcode()).thenReturn(0);
        when(mockHeader.toString()).thenReturn("HDR");
        msg.setHeader(mockHeader);

        // Force the internal list for SECTION.ANSWER (index 1) to null.
        // The field name is "sections" (an array of List<Record>) in the real class,
        // but because we don't have source we use reflection to locate it.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        sections[Section.ANSWER] = null; // simulate corrupted internal state

        // The toString call will attempt to render the ANSWER section and hit NPE.
        assertThrows(NullPointerException.class, msg::toString,
                "Expected NPE when internal section list is null");
    }
}
```