```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;

// Added imports for missing classes
import org.xbill.DNS.TSIGException;
import org.xbill.DNS.Section;
import org.xbill.DNS.ARecord;

@ExtendWith(MockitoExtension.class)
class TSIG_apply_3_Test {

    /**
     * Fixed clock so that the generated TSIG timestamps are deterministic.
     */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /**
     * TSIG instance used by all tests.
     */
    private static TSIG tsig;

    /**
     * Message that will be signed in each test.
     */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a TSIG key with a deterministic clock.
        // Algorithm HMAC_SHA256, name "example.", key "12345678" (raw bytes).
        SecretKey secret = new SecretKeySpec("12345678".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), secret, FIXED_CLOCK);
    }

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Build a simple DNS query message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        message = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        tsig = null;
    }

    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(
                // null Message -> NPE
                Arguments.of("null Message must cause NPE", null, 0, null, NullPointerException.class),
                // negative error code -> IllegalArgumentException
                Arguments.of("negative error code must cause IAE", mock(Message.class), -1, null, IllegalArgumentException.class)
        );
    }

    private static Stream<Arguments> timeFudgeProvider() {
        Instant now = Instant.now(FIXED_CLOCK);
        return Stream.of(
                Arguments.of("just inside fudge window (past)", now.minus(TSIG.FUDGE).plusSeconds(1), true),
                Arguments.of("just outside fudge window (past)", now.minus(TSIG.FUDGE).minusSeconds(1), false),
                Arguments.of("just inside fudge window (future)", now.plus(TSIG.FUDGE).minusSeconds(1), true),
                Arguments.of("just outside fudge window (future)", now.plus(TSIG.FUDGE).plusSeconds(1), false)
        );
    }

    /**
     * Verify that {@code TSIG.apply(Message, int, TSIGRecord)} attaches a TSIG record,
     * marks the message as signed, and that the signature can be verified successfully.
     */
    @Test
    void applyAddsSignatureAndVerifies() throws Exception {
        tsig.apply(message, Rcode.NOERROR, null);
        assertTrue(message.isSigned(), "Message should be marked as signed after apply()");
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should be present after apply()");
        assertEquals(Rcode.NOERROR, tsigRecord.getError(), "TSIG record error should be NOERROR");
        byte[] wire = message.toWire(Message.MAXLENGTH);
        int verifyResult = tsig.verify(message, wire, null);
        assertEquals(Rcode.NOERROR, verifyResult, "Verification should succeed with NOERROR");
        assertTrue(message.isVerified(), "Message should be marked as verified after successful verify()");
    }

    /**
     * Ensure that a non‑zero error code is correctly stored in the generated TSIG record.
     */
    @Test
    void applyWithErrorCode() throws Exception {
        int errorCode = Rcode.BADKEY;
        tsig.apply(message, errorCode, null);
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should be present");
        assertEquals(errorCode, tsigRecord.getError(), "TSIG record must contain the supplied error code");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullArgumentProvider")
    void nullArgumentHandling(String description, Message m, int error, TSIGRecord old, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> tsig.apply(m, error, old));
    }

    @Test
    void errorCodeZeroGeneratesNormalTSIG() throws Exception {
        tsig.apply(message, Rcode.NOERROR, null);
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec, "TSIG record should be present");
        assertEquals(Rcode.NOERROR, rec.getError(), "TSIG record error must match supplied error (NOERROR)");
    }

    @Test
    void errorCodeNonZeroIsPropagatedInTSIGRecord() throws Exception {
        int error = Rcode.NOTIMP;
        tsig.apply(message, error, null);
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec, "TSIG record should be present");
        assertEquals(error, rec.getError(), "TSIG record error must match supplied non‑zero error");
    }

    @Test
    void oldIsNullMeansFirstTSIGInTransaction() throws Exception {
        tsig.apply(message, Rcode.NOERROR, null);
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec);
        assertEquals(Instant.now(FIXED_CLOCK), rec.getTimeSigned(),
                "When old == null the timeSigned must be the current instant");
    }

    @Test
    void oldRecordTimeSignedIsCopiedWhenPresent() throws Exception {
        TSIG oldTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec("12345678".getBytes(StandardCharsets.UTF_8), "HmacSHA256"), FIXED_CLOCK);
        Message request = Message.newQuery(Record.newRecord(Name.fromString("a.example."), Type.A, DClass.IN));
        oldTsig.apply(request, Rcode.NOERROR, null);
        TSIGRecord oldRec = request.getTSIG();
        tsig.apply(message, Rcode.NOERROR, oldRec);
        TSIGRecord newRec = message.getTSIG();
        assertNotNull(newRec);
        assertEquals(oldRec.getTimeSigned(), newRec.getTimeSigned(),
                "New TSIG must copy the timeSigned from the old TSIG when old != null");
    }

    @Test
    void threeArgumentOverloadDelegatesToFourArgumentVersion() throws Exception {
        TSIG spy = spy(tsig);
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("b.example."), Type.A, DClass.IN));
        spy.apply(msg, Rcode.NOERROR, null);
        // verify that the four‑argument overload was called with fullSignature = true
        verify(spy, times(1)).apply(eq(msg), eq(Rcode.NOERROR), isNull(), eq(true));
    }

    @Test
    void threeArgumentOverloadPropagatesExceptionFromFourArgumentVersion() throws Exception {
        TSIG spy = spy(tsig);
        doThrow(new TSIGException("forced")).when(spy).apply(any(Message.class), anyInt(), any(), eq(true));
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("c.example."), Type.A, DClass.IN));
        TSIGException ex = assertThrows(TSIGException.class,
                () -> spy.apply(msg, Rcode.NOERROR, null),
                "Exception from the four‑argument method must be propagated");
        assertEquals("forced", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("timeFudgeProvider")
    void timeFudgeBoundaryHandling(String description, Instant oldTime, boolean shouldPass) throws Exception {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getTimeSigned()).thenReturn(oldTime);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getSignature()).thenReturn(new byte[32]);
        if (shouldPass) {
            tsig.apply(message, Rcode.NOERROR, old);
            TSIGRecord newRec = message.getTSIG();
            assertNotNull(newRec);
            assertTrue(Duration.between(newRec.getTimeSigned(), Instant.now(FIXED_CLOCK)).abs()
                            .compareTo(TSIG.FUDGE) <= 0,
                    "Generated timeSigned must be within the fudge window");
        } else {
            assertThrows(TSIGException.class,
                    () -> tsig.apply(message, Rcode.NOERROR, old),
                    "Old timeSigned outside fudge window must cause TSIGException");
        }
    }

    @Test
    void algorithmMismatchBetweenOldAndCurrentThrowsIAE() throws Exception {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_MD5);
        when(old.getName()).thenReturn(Name.fromString("example."));
        when(old.getSignature()).thenReturn(new byte[16]);
        when(old.getTimeSigned()).thenReturn(Instant.now(FIXED_CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(message, Rcode.NOERROR, old),
                "Algorithm mismatch must result in IllegalArgumentException");
    }

    @Test
    void emptySecretKeyCausesException() {
        SecretKey empty = new SecretKeySpec(new byte[0], "HmacSHA256");
        TSIG emptyKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), empty, FIXED_CLOCK);
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("d.example."), Type.A, DClass.IN));
        assertThrows(TSIGException.class,
                () -> emptyKeyTsig.apply(msg, Rcode.NOERROR, null),
                "TSIG constructed with empty secret must fail during apply");
    }

    @Test
    void nullSecretKeyCausesNullPointerException() {
        TSIG nullKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), (SecretKey) null, FIXED_CLOCK);
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("e.example."), Type.A, DClass.IN));
        assertThrows(NullPointerException.class,
                () -> nullKeyTsig.apply(msg, Rcode.NOERROR, null),
                "TSIG constructed with null secret must throw NPE");
    }

    @Test
    void applyingWhenMessageAlreadyHasTSIGThrowsIllegalState() throws Exception {
        tsig.apply(message, Rcode.NOERROR, null);
        assertThrows(IllegalStateException.class,
                () -> tsig.apply(message, Rcode.NOERROR, null),
                "Applying TSIG twice on the same Message must be rejected");
    }

    @Test
    void badTimeErrorCodeIsEmbeddedInGeneratedTSIG() throws Exception {
        int badtime = Rcode.BADTIME;
        tsig.apply(message, badtime, null);
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec);
        assertEquals(badtime, rec.getError(), "TSIGRecord error field must be BADTIME when supplied");
        assertEquals(Instant.now(FIXED_CLOCK), rec.getTimeSigned(),
                "timeSigned should be current instant for BADTIME case");
    }

    @Test
    void fullSignatureLengthMatchesAlgorithmOutput() throws Exception {
        Record q = Record.newRecord(Name.fromString("x.example."), Type.A, DClass.IN);
        Message msg = Message.newQuery(q);
        // add an answer record
        msg.addRecord(new ARecord(Name.fromString("x.example."), DClass.IN, 300,
                InetAddress.getByName("1.2.3.4")), Section.ANSWER);
        tsig.apply(msg, Rcode.NOERROR, null);
        TSIGRecord rec = msg.getTSIG();
        assertNotNull(rec);
        // HMAC‑SHA256 produces a 32‑byte MAC.
        assertEquals(32, rec.getSignature().length,
                "MAC length must match the output size of the selected algorithm (SHA‑256 => 32 bytes)");
    }

    @Test
    void unsupportedAlgorithmNameThrowsIllegalArgumentException() {
        Name unknownAlg = Name.fromConstantString("HMAC-FOO.");
        TSIG badAlgTsig = new TSIG(unknownAlg, Name.fromString("example."),
                new SecretKeySpec("12345678".getBytes(StandardCharsets.UTF_8), "HmacSHA256"),
                FIXED_CLOCK);
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("f.example."), Type.A, DClass.IN));
        assertThrows(IllegalArgumentException.class,
                () -> badAlgTsig.apply(msg, Rcode.NOERROR, null),
                "Using an unknown algorithm name must cause IllegalArgumentException");
    }

    @Test
    void oldTSIGMacLengthMismatchThrowsIllegalArgumentException() throws Exception {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("example."));
        // truncated MAC
        when(old.getSignature()).thenReturn(new byte[10]);
        when(old.getTimeSigned()).thenReturn(Instant.now(FIXED_CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(message, Rcode.NOERROR, old),
                "MAC length mismatch must be rejected");
    }

    @Test
    void errorCodeOutsideValidRangeThrowsIllegalArgumentException() {
        int tooLarge = 70000;
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(message, tooLarge, null),
                "RCODE values outside 0‑65535 must be rejected");
    }

    @Test
    void messageWithMultipleTSIGRecordsThrowsIllegalStateException() throws Exception {
        TSIGRecord first = tsig.generate(message, new byte[0], Rcode.NOERROR, null);
        TSIGRecord second = tsig.generate(message, new byte[0], Rcode.NOERROR, null);
        java.lang.reflect.Field tsigField = Message.class.getDeclaredField("tsig");
        tsigField.setAccessible(true);
        tsigField.set(message, new TSIGRecord[]{first, second});
        assertThrows(IllegalStateException.class,
                () -> tsig.apply(message, Rcode.NOERROR, null),
                "Message containing more than one TSIG must cause IllegalStateException");
    }

    @Test
    void oldTSIGKeyNameMismatchThrowsIllegalArgumentException() throws Exception {
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getName()).thenReturn(Name.fromString("different."));
        when(old.getSignature()).thenReturn(new byte[32]);
        when(old.getTimeSigned()).thenReturn(Instant.now(FIXED_CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(message, Rcode.NOERROR, old),
                "Key name mismatch between old TSIG and TSIG instance must be rejected");
    }
}
```