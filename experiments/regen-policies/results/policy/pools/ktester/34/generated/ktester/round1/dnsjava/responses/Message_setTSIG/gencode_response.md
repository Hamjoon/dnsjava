```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link Message#setTSIG(TSIG)}.
 *
 * <p>The test suite covers normal usage, edge‑cases, error handling and
 * ensures that the internal state of {@link Message} (e.g. {@code tsigState},
 * {@code isSigned()}, header fields and sections) behaves as documented.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_Test {

    private Message message;
    private TSIG tsigKey; // used as a simple default key in many tests

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Fresh message for each test
        message = new Message();

        // A simple, valid TSIG key (algorithm, name, base64 secret)
        tsigKey = new TSIG("hmac-md5", "example.com.", "ABCDABCDABCDABCDABCDAB==");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
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
        assertEquals(Message.TSIG_UNSIGNED, message.tsigState,
                "tsigState should be TSIG_UNSIGNED (0) after null key");
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
        assertEquals(Message.TSIG_SIGNED, message.tsigState,
                "tsigState must be TSIG_SIGNED (3)");
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "A TSIGRecord must be generated");
        assertEquals(key.getName(), tsigRecord.getName(),
                "Generated TSIGRecord name must match the key name");
        assertEquals(key.getAlgorithm(), tsigRecord.getAlgorithm(),
                "Generated TSIGRecord algorithm must match the key algorithm");
    }

    private static Stream<Arguments> validKeyProvider() {
        // 1️⃣ normal key with a base‑64 secret
        TSIG key1 = new TSIG("hmac-md5", "secret.example.", "ABCDABCDABCDABCDABCDAB==");
        // 2️⃣ empty secret (byte[0]) – library does not validate length here
        TSIG key2 = new TSIG(Name.fromString("empty.example."), "hmac-sha256", new byte[0]);
        return Stream.of(
                Arguments.of("Normal TSIG key (base64 secret)", key1),
                Arguments.of("TSIG with empty secret", key2)
        );
    }

    /* -------------------------------------------------------------
     *  Multiple calls – replacement semantics and clearing with null.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_multipleCallsReplaceExistingTSIG() {
        // First call
        TSIG firstKey = new TSIG("hmac-md5", "first.example.", "ABCDABCDABCDABCDABCDAB==");
        message.setTSIG(firstKey);
        TSIGRecord firstRecord = message.getTSIG();
        assertNotNull(firstRecord);
        assertEquals(firstKey.getName(), firstRecord.getName());

        // Second call with a different key
        TSIG secondKey = new TSIG("hmac-sha256", "second.example.", "YWJjZGVmZ2hpamtsbW5vcA==");
        message.setTSIG(secondKey);
        TSIGRecord secondRecord = message.getTSIG();
        assertNotNull(secondRecord);
        assertNotSame(firstRecord, secondRecord, "A new TSIGRecord must replace the old one");
        assertEquals(secondKey.getName(), secondRecord.getName());

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

    /* -------------------------------------------------------------
     *  Exception safety – a TSIG that throws IOException should not
     *  leak the checked exception.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_exceptionSafety_handlesIOExceptionGracefully() throws IOException {
        // Mock a TSIG whose apply() throws IOException
        TSIG brokenKey = Mockito.mock(TSIG.class);
        Mockito.doThrow(new IOException("simulated IO failure"))
                .when(brokenKey).apply(Mockito.any(Message.class), Mockito.isNull());

        // Act – the method must swallow the IOException
        message.setTSIG(brokenKey);

        // Verify that the message is marked as failed and not signed
        assertFalse(message.isSigned(), "Message must not be signed after a failing TSIG");
        assertEquals(Message.TSIG_FAILED, message.tsigState,
                "tsigState should be TSIG_FAILED (4) on IOException");
        assertNull(message.getTSIG(), "No TSIGRecord should be present after failure");
    }

    /* -------------------------------------------------------------
     *  Unicode key name handling.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_unicodeKeyName_isAccepted() {
        Name unicodeName = Name.fromString("ñame.example.");
        TSIG unicodeKey = new TSIG(unicodeName, "hmac-sha256", "unicodeSecret".getBytes(StandardCharsets.UTF_8));

        message.setTSIG(unicodeKey);

        assertTrue(message.isSigned());
        assertEquals(Message.TSIG_SIGNED, message.tsigState);
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec);
        assertEquals(unicodeName, rec.getName(),
                "Unicode name must be preserved in the generated TSIGRecord");
    }

    /* -------------------------------------------------------------
     *  Large secret handling – should stay within MAXLENGTH.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_largeSecret_isHandledWithoutOverflow() {
        byte[] largeSecret = new byte[5_000];
        TSIG largeKey = new TSIG(Name.fromString("large.example."), "hmac-sha256", largeSecret);

        message.setTSIG(largeKey);

        assertTrue(message.isSigned());
        assertEquals(Message.TSIG_SIGNED, message.tsigState);
        assertNotNull(message.getTSIG());
        assertTrue(message.numBytes() <= Message.MAXLENGTH,
                "Message size must not exceed MAXLENGTH after adding a large secret");
    }

    /* -------------------------------------------------------------
     *  Null algorithm – should result in failure state.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_nullAlgorithm_handling_resultsInFailure() {
        TSIG nullAlgKey = new TSIG(Name.fromString("nullalg.example."), null, "secret".getBytes());

        // The library may throw IllegalArgumentException at construction time;
        // if not, setTSIG must handle it gracefully.
        assertDoesNotThrow(() -> message.setTSIG(nullAlgKey),
                "setTSIG should not propagate a checked exception");

        assertFalse(message.isSigned());
        assertEquals(Message.TSIG_FAILED, message.tsigState);
        assertNull(message.getTSIG());
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
        assertEquals(originalID, afterHeader.getID(),
                "Message ID must be unchanged after setTSIG");
        // other header fields are not modified by setTSIG; we only assert ID here.
    }

    /* -------------------------------------------------------------
     *  No side‑effects on other sections.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_noSideEffectOnSections() {
        // Populate non‑TSIG sections with dummy records
        Record answerRec = new ARecord(Name.fromString("a.example."), DClass.IN, 300, InetAddress.getLoopbackAddress());
        Record authRec = new NSRecord(Name.fromString("example."), DClass.IN, 300, Name.fromString("ns.example."));
        Record addRec = new TXTRecord(Name.fromString("txt.example."), DClass.IN, 300, "hello");

        message.addRecord(answerRec, Section.ANSWER);
        message.addRecord(authRec, Section.AUTHORITY);
        message.addRecord(addRec, Section.ADDITIONAL); // non‑TSIG addition

        // Keep snapshots of the sections
        List<Record> answerBefore = new ArrayList<>(message.getSection(Section.ANSWER));
        List<Record> authBefore = new ArrayList<>(message.getSection(Section.AUTHORITY));
        List<Record> addBefore = new ArrayList<>(message.getSection(Section.ADDITIONAL));

        // Act
        message.setTSIG(tsigKey);

        // Verify sections other than the generated TSIG are unchanged
        assertEquals(answerBefore, message.getSection(Section.ANSWER),
                "ANSWER section must stay unchanged");
        assertEquals(authBefore, message.getSection(Section.AUTHORITY),
                "AUTHORITY section must stay unchanged");
        // The ADDITIONAL section should contain the previous records plus exactly one TSIGRecord
        List<Record> addAfter = message.getSection(Section.ADDITIONAL);
        assertEquals(addBefore.size() + 1, addAfter.size(),
                "ADDITIONAL section size should increase by one for the TSIGRecord");
        assertTrue(addAfter.containsAll(addBefore),
                "Existing ADDITIONAL records must be retained");
    }

    /* -------------------------------------------------------------
     *  Invalid key constructions – expect IllegalArgumentException.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new TSIG(null, "hmac-md5", "secret".getBytes()),
                "Constructing TSIG with null name must fail");
    }

    @Test
    void setTSIG_nonAbsoluteName_throwsIllegalArgumentException() {
        Name relative = Name.fromString("relative"); // no trailing dot
        assertThrows(IllegalArgumentException.class,
                () -> new TSIG(relative, "hmac-md5", "secret".getBytes()),
                "Non‑absolute TSIG name must be rejected");
    }

    @Test
    void setTSIG_nameTooLong_throwsIllegalArgumentException() {
        // Build a label of 256 characters (exceeds DNS 255‑octet limit)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) sb.append('a');
        String longLabel = sb.toString();
        Name tooLong = Name.fromString(longLabel + ".example.");
        assertThrows(IllegalArgumentException.class,
                () -> new TSIG(tooLong, "hmac-sha256", "secret".getBytes()),
                "TSIG name exceeding 255 octets must be rejected");
    }

    @Test
    void setTSIG_nullSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new TSIG(Name.fromString("nullsecret.example."), "hmac-md5", (byte[]) null),
                "TSIG with null secret must be rejected");
    }

    @Test
    void setTSIG_unsupportedAlgorithm_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new TSIG(Name.fromString("badalg.example."), "unsupported-algo", "secret".getBytes()),
                "Unsupported algorithm must cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Overly large secret – should cause TSIG_FAILED without throwing.
     * ------------------------------------------------------------- */
    @Test
    void setTSIG_overlyLargeSecret_causesFailureOnWire() {
        byte[] hugeSecret = new byte[70_000]; // larger than MAXLENGTH
        TSIG hugeKey = new TSIG(Name.fromString("bigsecret.example."), "hmac-sha256", hugeSecret);

        // setTSIG must not throw a checked exception
        assertDoesNotThrow(() -> message.setTSIG(hugeKey));

        // Expected failure state because the resulting message would be too big
        assertEquals(Message.TSIG_FAILED, message.tsigState,
                "tsigState should be TSIG_FAILED for an oversized secret");
        assertFalse(message.isSigned(), "Message must not be marked as signed");
        assertNull(message.getTSIG(), "No TSIGRecord should be generated on failure");
    }

    /* -------------------------------------------------------------
     *  Helper to generate a long label (used in name‑too‑long test).
     * ------------------------------------------------------------- */
    private static String generateLongLabel(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append('x');
        return sb.toString();
    }
}
```