package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Message_setTSIG_Test {

    private Message message;

    private TSIG tsigKey;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // fresh message for each test
        message = new Message();
        // TSIG constructor: (algorithm, name, secret) – all as strings
        tsigKey = new TSIG("hmac-md5", "example.com.", "ABCDABCDABCDABCDABCDAB==");
    }

    private static Stream<Arguments> validKeyProvider() {
        // normal key – base‑64 secret
        TSIG key1 = new TSIG("hmac-md5", "secret.example.", "ABCDABCDABCDABCDABCDAB==");
        // empty secret – an empty string is accepted (represents zero‑length secret)
        TSIG key2 = new TSIG("hmac-sha256", "empty.example.", "");
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
        TSIG firstKey = new TSIG("hmac-md5", "first.example.", "ABCDABCDABCDABCDABCDAB==");
        message.setTSIG(firstKey);
        TSIGRecord firstRecord = message.getTSIG();
        assertNotNull(firstRecord);
        assertEquals(tsigName(firstKey), firstRecord.getName());
        // Second call with a different key
        TSIG secondKey = new TSIG("hmac-sha256", "second.example.", "YWJjZGVmZ2hpamtsbW5vcA==");
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
     *  Invalid key constructions – expect IllegalArgumentException.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TSIG("hmac-md5", (String) null, "secret"), "Constructing TSIG with null name must fail");
    }

    @Test
    void setTSIG_nonAbsoluteName_throwsIllegalArgumentException() {
        // No trailing dot -> not absolute
        assertThrows(IllegalArgumentException.class, () -> new TSIG("hmac-md5", "relative", "secret"), "Non‑absolute TSIG name must be rejected");
    }

    @Test
    void setTSIG_nameTooLong_throwsIllegalArgumentException() {
        // Build a label of 256 characters (exceeds DNS 255‑octet limit)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) sb.append('a');
        String longLabel = sb.toString();
        assertThrows(IllegalArgumentException.class, () -> new TSIG("hmac-sha256", longLabel + ".example.", "secret"), "TSIG name exceeding 255 octets must be rejected");
    }

    @Test
    void setTSIG_nullSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TSIG("hmac-md5", "nullsecret.example.", (String) null), "TSIG with null secret must be rejected");
    }

    @Test
    void setTSIG_unsupportedAlgorithm_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TSIG("unsupported-algo", "badalg.example.", "secret"), "Unsupported algorithm must cause IllegalArgumentException");
    }
}
