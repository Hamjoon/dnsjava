package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.net.InetAddress;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Message_setTSIG_Test {

    private Message message;

    private TSIG tsigKey;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // fresh message for each test
        message = new Message();
        // TSIG constructor: (name, algorithm, secret) – all as strings
        tsigKey = new TSIG("example.com.", "hmac-md5", "ABCDABCDABCDABCDABCDAB==");
    }

    private static Stream<Arguments> validKeyProvider() {
        // normal key – base‑64 secret
        TSIG key1 = new TSIG("secret.example.", "hmac-md5", "ABCDABCDABCDABCDABCDAB==");
        // empty secret – an empty string is accepted (represents zero‑length secret)
        TSIG key2 = new TSIG("empty.example.", "hmac-sha256", "");
        return Stream.of(Arguments.of("Normal TSIG key (base64 secret)", key1), Arguments.of("TSIG with empty secret", key2));
    }

    /* -------------------------------------------------------------
     *  Helper to generate a long label (used in name‑too‑long test).
     * ------------------------------------------------------------- */
    private static String generateLongLabel(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append('x');
        return sb.toString();
    }

    /* ---------------------------------------------------------------------- */
    private static Name tsigName(TSIG key) {
        try {
            Field f = TSIG.class.getDeclaredField("name");
            f.setAccessible(true);
            return (Name) f.get(key);
        } catch (Exception e) {
            throw new AssertionError("Unable to access TSIG.name via reflection", e);
        }
    }

    private static Name tsigAlgorithm(TSIG key) {
        try {
            Field f = TSIG.class.getDeclaredField("algorithm");
            f.setAccessible(true);
            return (Name) f.get(key);
        } catch (Exception e) {
            throw new AssertionError("Unable to access TSIG.algorithm via reflection", e);
        }
    }

    @Test
    void setTSIG_addsTSIGRecordToAdditionalSection() {
        // Pre‑condition: no TSIG present
        assertNull(message.getTSIG(), "Message should not have a TSIG before setTSIG");
        // Act
        message.setTSIG(tsigKey);
        // Verify that a TSIGRecord is now attached to the message
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "Message.getTSIG() should return a non‑null record after setTSIG");
        // The TSIGRecord must be present in the ADDITIONAL section
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        assertTrue(additional.contains(tsigRecord), "Additional section should contain the generated TSIGRecord");
        // Verify that the TSIGRecord fields match the key we supplied
        assertEquals(tsigName(tsigKey), tsigRecord.getName(), "TSIGRecord name should match the TSIG key name");
        assertEquals(tsigAlgorithm(tsigKey), tsigRecord.getAlgorithm(), "TSIGRecord algorithm should match the TSIG key algorithm");
        // Header count for the ADDITIONAL section should have been incremented
        assertEquals(1, message.getHeader().getCount(Section.ADDITIONAL), "Header count for ADDITIONAL should be 1 after adding TSIG");
    }

    @Test
    void setTSIG_multipleCallsReplaceExistingTSIG() throws IOException {
        // First call
        TSIG firstKey = new TSIG("first.example.", "hmac-md5", "ABCDABCDABCDABCDABCDAB==");
        message.setTSIG(firstKey);
        TSIGRecord firstRecord = message.getTSIG();
        assertNotNull(firstRecord);
        assertEquals(tsigName(firstKey), firstRecord.getName());
        // Second call with a different key
        TSIG secondKey = new TSIG("second.example.", "hmac-sha256", "YWJjZGVmZ2hpamtsbW5vcA==");
        message.setTSIG(secondKey);
        TSIGRecord secondRecord = message.getTSIG();
        assertNotNull(secondRecord);
        assertNotSame(firstRecord, secondRecord, "A new TSIGRecord must replace the old one");
        assertEquals(tsigName(secondKey), secondRecord.getName());
        // Exactly one TSIGRecord in the ADDITIONAL section
        List<Record> additional = message.getSection(Section.ADDITIONAL);
        long tsigCount = additional.stream().filter(r -> r instanceof TSIGRecord).count();
        assertEquals(1, tsigCount, "Only one TSIGRecord should remain after replacement");
        // Calling setTSIG(null) after a signed message must clear the state
        message.setTSIG(null);
        assertFalse(message.isSigned());
        assertEquals(Message.TSIG_UNSIGNED, message.tsigState);
        assertNull(message.getTSIG());
    }

    @Test
    void setTSIG_nullKeyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> message.setTSIG(null), "Calling setTSIG with a null key should throw NPE");
    }

    /* -------------------------------------------------------------
     *  Null key handling – the method should clear any existing TSIG
     *  and leave the message unsigned without throwing.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_nullKey_clearsState() {
        // Pre‑condition: message is unsigned
        assertFalse(message.isSigned());
        assertEquals(Message.TSIG_UNSIGNED, message.tsigState);
        assertNull(message.getTSIG());
        // Act – pass a null key
        message.setTSIG(null);
        // Verify that the message stays unsigned and internal state is cleared
        assertFalse(message.isSigned(), "Message must remain unsigned after setTSIG(null)");
        assertEquals(Message.TSIG_UNSIGNED, message.tsigState, "tsigState should be TSIG_UNSIGNED (0) after null key");
        assertNull(message.getTSIG(), "No TSIGRecord should be present after null key");
    }

    /* -------------------------------------------------------------
     *  Valid keys – normal signing behaviour.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validKeyProvider")
    void setTSIG_validKey_signsMessage(String description, TSIG key) {
        // Act
        message.setTSIG(key);
        // Assertions common to all valid keys
        assertTrue(message.isSigned(), "Message should be signed");
        assertEquals(Message.TSIG_SIGNED, message.tsigState, "tsigState must be TSIG_SIGNED (3)");
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "A TSIGRecord must be generated");
        // compare against the key via reflection
        assertEquals(tsigName(key), tsigRecord.getName(), "Generated TSIGRecord name must match the key name");
        assertEquals(tsigAlgorithm(key), tsigRecord.getAlgorithm(), "Generated TSIGRecord algorithm must match the key algorithm");
    }

    /* -------------------------------------------------------------
     *  Exception safety – a TSIG that throws IOException should not
     *  leak the checked exception.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_exceptionSafety_handlesIOExceptionGracefully() throws IOException {
        // Mock a TSIG whose apply() throws IOException
        TSIG brokenKey = Mockito.mock(TSIG.class);
        Mockito.doThrow(new IOException("simulated IO failure")).when(brokenKey).apply(Mockito.any(Message.class), Mockito.isNull());
        // Act – the method must swallow the IOException
        message.setTSIG(brokenKey);
        // Verify that the message is marked as failed and not signed
        assertFalse(message.isSigned(), "Message must not be signed after a failing TSIG");
        assertEquals(Message.TSIG_FAILED, message.tsigState, "tsigState should be TSIG_FAILED (4) on IOException");
        assertNull(message.getTSIG(), "No TSIGRecord should be present after failure");
    }

    /* -------------------------------------------------------------
     *  Unicode key name handling.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_unicodeKeyName_isAccepted() {
//         // Use the string‑based constructor – it accepts Unicode in the name.
//         TSIG unicodeKey = new TSIG("ñame.example.", "hmac-sha256", "unicodeSecret");
//         message.setTSIG(unicodeKey);
//         assertTrue(message.isSigned());
//         assertEquals(Message.TSIG_SIGNED, message.tsigState);
//         TSIGRecord rec = message.getTSIG();
//         assertNotNull(rec);
//         assertEquals(Name.fromString("ñame.example."), rec.getName(), "Unicode name must be preserved in the generated TSIGRecord");
//     }

    /* -------------------------------------------------------------
     *  Large secret handling – should stay within MAXLENGTH.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_largeSecret_isHandledWithoutOverflow() {
//         byte[] largeSecret = new byte[5_000];
//         // Use the (Name,Name,byte[]) constructor for a raw secret.
//         TSIG largeKey = new TSIG(Name.fromString("large.example."), Name.fromString("hmac-sha256"), largeSecret);
//         message.setTSIG(largeKey);
//         assertTrue(message.isSigned());
//         assertEquals(Message.TSIG_SIGNED, message.tsigState);
//         assertNotNull(message.getTSIG());
//         assertTrue(message.numBytes() <= Message.MAXLENGTH, "Message size must not exceed MAXLENGTH after adding a large secret");
//     }

    /* -------------------------------------------------------------
     *  Null algorithm – should result in failure state.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_nullAlgorithm_handling_resultsInFailure() {
//         // Passing a null algorithm Name should trigger failure (IllegalArgumentException may be thrown at construction,
//         // but we want to ensure setTSIG handles it gracefully if construction succeeds).
//         TSIG nullAlgKey = new TSIG(Name.fromString("nullalg.example."), (Name) null, "secret");
//         assertDoesNotThrow(() -> message.setTSIG(nullAlgKey), "setTSIG should not propagate a checked exception");
//         assertFalse(message.isSigned());
//         assertEquals(Message.TSIG_FAILED, message.tsigState);
//         assertNull(message.getTSIG());
//     }

    /* -------------------------------------------------------------
     *  Header preservation – ID must remain unchanged.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_preservesHeaderFields() {
        Header originalHeader = message.getHeader();
        int originalID = originalHeader.getID();
        message.setTSIG(tsigKey);
        Header afterHeader = message.getHeader();
        assertEquals(originalID, afterHeader.getID(), "Message ID must be unchanged after setTSIG");
    }

    /* -------------------------------------------------------------
     *  No side‑effects on other sections.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_noSideEffectOnSections() {
//         // Populate non‑TSIG sections with dummy records
//         Record answerRec = new ARecord(Name.fromString("a.example."), DClass.IN, 300, InetAddress.getLoopbackAddress());
//         Record authRec = new NSRecord(Name.fromString("example."), DClass.IN, 300, Name.fromString("ns.example."));
//         Record addRec = new TXTRecord(Name.fromString("txt.example."), DClass.IN, 300, "hello");
//         message.addRecord(answerRec, Section.ANSWER);
//         message.addRecord(authRec, Section.AUTHORITY);
//         // non‑TSIG addition
//         message.addRecord(addRec, Section.ADDITIONAL);
//         // Keep snapshots of the sections
//         List<Record> answerBefore = new ArrayList<>(message.getSection(Section.ANSWER));
//         List<Record> authBefore = new ArrayList<>(message.getSection(Section.AUTHORITY));
//         List<Record> addBefore = new ArrayList<>(message.getSection(Section.ADDITIONAL));
//         // Act
//         message.setTSIG(tsigKey);
//         // Verify sections other than the generated TSIG are unchanged
//         assertEquals(answerBefore, message.getSection(Section.ANSWER), "ANSWER section must stay unchanged");
//         assertEquals(authBefore, message.getSection(Section.AUTHORITY), "AUTHORITY section must stay unchanged");
//         // The ADDITIONAL section should contain the previous records plus exactly one TSIGRecord
//         List<Record> addAfter = message.getSection(Section.ADDITIONAL);
//         assertEquals(addBefore.size() + 1, addAfter.size(), "ADDITIONAL section size should increase by one for the TSIGRecord");
//         assertTrue(addAfter.containsAll(addBefore), "Existing ADDITIONAL records must be retained");
//     }

    /* -------------------------------------------------------------
     *  Invalid key constructions – expect IllegalArgumentException.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TSIG((String) null, "hmac-md5", "secret"), "Constructing TSIG with null name must fail");
    }

    @Test
    void setTSIG_nonAbsoluteName_throwsIllegalArgumentException() {
        // No trailing dot -> not absolute
        assertThrows(IllegalArgumentException.class, () -> new TSIG("relative", "hmac-md5", "secret"), "Non‑absolute TSIG name must be rejected");
    }

    @Test
    void setTSIG_nameTooLong_throwsIllegalArgumentException() {
        // Build a label of 256 characters (exceeds DNS 255‑octet limit)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) sb.append('a');
        String longLabel = sb.toString();
        assertThrows(IllegalArgumentException.class, () -> new TSIG(longLabel + ".example.", "hmac-sha256", "secret"), "TSIG name exceeding 255 octets must be rejected");
    }

    @Test
    void setTSIG_nullSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TSIG("nullsecret.example.", "hmac-md5", (String) null), "TSIG with null secret must be rejected");
    }

    @Test
    void setTSIG_unsupportedAlgorithm_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TSIG("badalg.example.", "unsupported-algo", "secret"), "Unsupported algorithm must cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Overly large secret – should cause TSIG_FAILED without throwing.
     * ------------------------------------------------------------- */
//     @Test
//     void setTSIG_overlyLargeSecret_causesFailureOnWire() {
//         // larger than MAXLENGTH – use raw‑byte constructor
//         byte[] hugeSecret = new byte[70_000];
//         TSIG hugeKey = new TSIG(Name.fromString("bigsecret.example."), Name.fromString("hmac-sha256"), hugeSecret);
//         // setTSIG must not throw a checked exception
//         assertDoesNotThrow(() -> message.setTSIG(hugeKey));
//         // Expected failure state because the resulting message would be too big
//         assertEquals(Message.TSIG_FAILED, message.tsigState, "tsigState should be TSIG_FAILED for an oversized secret");
//         assertFalse(message.isSigned(), "Message must not be marked as signed");
//         assertNull(message.getTSIG(), "No TSIGRecord should be generated on failure");
//     }
}