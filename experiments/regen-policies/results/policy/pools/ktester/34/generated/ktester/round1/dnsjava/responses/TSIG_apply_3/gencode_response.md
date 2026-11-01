```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TSIG#apply(Message, int, TSIGRecord)}.
 *
 * <p>The tests are grouped by the logical groups described in the JSON test‑case
 * specification.  Each group is covered by a single test method (parameterized
 * where appropriate).  Real {@link Message} objects are used whenever possible;
 * {@link TSIGRecord} and {@link Message} are mocked only when the behaviour of the
 * {@code apply} implementation cannot be exercised with a concrete instance.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_3_Test {

    /** Fixed clock so that the generated TSIG timestamps are deterministic. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /** TSIG instance used by most tests (HMAC‑SHA256 algorithm). */
    private static TSIG tsig;

    /** Message that will be signed in each test. */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // Algorithm HMAC_SHA256, name "example.", key "12345678" (base‑64 string).
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678", FIXED_CLOCK);
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

    /* ---------------------------------------------------------------------- */
    /*  Null argument handling                                               */
    /* ---------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullArgumentProvider")
    void nullArgumentHandling(String description, Message m, int error, TSIGRecord old,
                              Class<? extends Throwable> expected) {
        assertThrows(expected, () -> tsig.apply(m, error, old));
    }

    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(
                // null Message -> NPE
                Arguments.of("null Message must cause NPE",
                        null, 0, null, NullPointerException.class),

                // negative error code -> IllegalArgumentException
                Arguments.of("negative error code must cause IAE",
                        mock(Message.class), -1, null, IllegalArgumentException.class)
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  Error code branching                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    void errorCodeZeroGeneratesNormalTSIG() throws Exception {
        tsig.apply(message, Rcode.NOERROR, null);
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec, "TSIG record should be present");
        assertEquals(Rcode.NOERROR, rec.getError(),
                "TSIG record error must match supplied error (NOERROR)");
    }

    @Test
    void errorCodeNonZeroIsPropagatedInTSIGRecord() throws Exception {
        int error = Rcode.NOTIMP; // 9
        tsig.apply(message, error, null);
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec, "TSIG record should be present");
        assertEquals(error, rec.getError(),
                "TSIG record error must match supplied non‑zero error");
    }

    /* ---------------------------------------------------------------------- */
    /*  Old TSIG record handling                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    void oldIsNullMeansFirstTSIGInTransaction() throws Exception {
        // No old record – the generated TSIG should have a timeSigned equal to the
        // fixed clock instant (the first TSIG in a transaction).
        tsig.apply(message, Rcode.NOERROR, null);
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec);
        assertEquals(Instant.now(FIXED_CLOCK), rec.getTimeSigned(),
                "When old == null the timeSigned must be the current instant");
    }

    @Test
    void oldRecordTimeSignedIsCopiedWhenPresent() throws Exception {
        // Create an old TSIGRecord with a known timeSigned using a different TSIG
        // instance (same algorithm/key to keep it valid).
        TSIG oldTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678", FIXED_CLOCK);
        Message request = Message.newQuery(Record.newRecord(Name.fromString("a.example."), Type.A, DClass.IN));
        oldTsig.apply(request, Rcode.NOERROR, null);
        TSIGRecord oldRec = request.getTSIG();

        // Apply on a fresh message using the old record.
        tsig.apply(message, Rcode.NOERROR, oldRec);
        TSIGRecord newRec = message.getTSIG();

        assertNotNull(newRec);
        assertEquals(oldRec.getTimeSigned(), newRec.getTimeSigned(),
                "New TSIG must copy the timeSigned from the old TSIG when old != null");
    }

    /* ---------------------------------------------------------------------- */
    /*  Full‑signature flag default (delegation test)                        */
    /* ---------------------------------------------------------------------- */

    @Test
    void threeArgumentOverloadDelegatesToFourArgumentVersion() throws Exception {
        // Spy on the TSIG instance to verify delegation.
        TSIG spy = spy(tsig);
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("b.example."), Type.A, DClass.IN));

        spy.apply(msg, Rcode.NOERROR, null); // three‑arg version

        // Verify that the four‑argument overload was invoked with fullSignature = true.
        verify(spy, times(1)).apply(eq(msg), eq(Rcode.NOERROR), isNull(), eq(true));
    }

    @Test
    void threeArgumentOverloadPropagatesExceptionFromFourArgumentVersion() throws Exception {
        // Create a spy that throws when the four‑argument method is called.
        TSIG spy = spy(tsig);
        doThrow(new TSIGException("forced")).when(spy).apply(any(Message.class), anyInt(), any(), eq(true));

        Message msg = Message.newQuery(Record.newRecord(Name.fromString("c.example."), Type.A, DClass.IN));

        TSIGException ex = assertThrows(TSIGException.class,
                () -> spy.apply(msg, Rcode.NOERROR, null),
                "Exception from the four‑argument method must be propagated");
        assertEquals("forced", ex.getMessage());
    }

    /* ---------------------------------------------------------------------- */
    /*  Time fudge boundary handling                                        */
    /* ---------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("timeFudgeProvider")
    void timeFudgeBoundaryHandling(String description, Instant oldTime, boolean shouldPass) throws Exception {
        // Mock an old TSIGRecord with the supplied timeSigned.
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getTimeSigned()).thenReturn(oldTime);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getKeyName()).thenReturn(Name.fromString("example."));
        when(old.getMac()).thenReturn(new byte[32]); // length for SHA‑256

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

    private static Stream<Arguments> timeFudgeProvider() {
        Instant now = Instant.now(FIXED_CLOCK);
        return Stream.of(
                Arguments.of("just inside fudge window (past)",
                        now.minus(TSIG.FUDGE).plusSeconds(1), true),
                Arguments.of("just outside fudge window (past)",
                        now.minus(TSIG.FUDGE).minusSeconds(1), false),
                Arguments.of("just inside fudge window (future)",
                        now.plus(TSIG.FUDGE).minusSeconds(1), true),
                Arguments.of("just outside fudge window (future)",
                        now.plus(TSIG.FUDGE).plusSeconds(1), false)
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  Algorithm mismatch between old TSIG and new                         */
    /* ---------------------------------------------------------------------- */

    @Test
    void algorithmMismatchBetweenOldAndCurrentThrowsIAE() throws Exception {
        // Old TSIGRecord uses HMAC_MD5 while the current TSIG instance uses HMAC_SHA256.
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_MD5);
        when(old.getKeyName()).thenReturn(Name.fromString("example."));
        when(old.getMac()).thenReturn(new byte[16]); // MD5 length
        when(old.getTimeSigned()).thenReturn(Instant.now(FIXED_CLOCK));

        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(message, Rcode.NOERROR, old),
                "Algorithm mismatch must result in IllegalArgumentException");
    }

    /* ---------------------------------------------------------------------- */
    /*  Empty secret key handling                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    void emptySecretKeyCausesException() {
        TSIG emptyKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "", FIXED_CLOCK);
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("d.example."), Type.A, DClass.IN));
        assertThrows(TSIGException.class,
                () -> emptyKeyTsig.apply(msg, Rcode.NOERROR, null),
                "TSIG constructed with empty secret must fail during apply");
    }

    @Test
    void nullSecretKeyCausesNullPointerException() {
        TSIG nullKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), null, FIXED_CLOCK);
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("e.example."), Type.A, DClass.IN));
        assertThrows(NullPointerException.class,
                () -> nullKeyTsig.apply(msg, Rcode.NOERROR, null),
                "TSIG constructed with null secret must throw NPE");
    }

    /* ---------------------------------------------------------------------- */
    /*  Repeated apply on same Message                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    void applyingWhenMessageAlreadyHasTSIGThrowsIllegalState() throws Exception {
        // First apply succeeds.
        tsig.apply(message, Rcode.NOERROR, null);
        // Second apply should fail because a TSIG is already present.
        assertThrows(IllegalStateException.class,
                () -> tsig.apply(message, Rcode.NOERROR, null),
                "Applying TSIG twice on the same Message must be rejected");
    }

    /* ---------------------------------------------------------------------- */
    /*  Bad time error handling (RCODE = BADTIME)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    void badTimeErrorCodeIsEmbeddedInGeneratedTSIG() throws Exception {
        int badtime = Rcode.BADTIME; // 18
        tsig.apply(message, badtime, null);
        TSIGRecord rec = message.getTSIG();
        assertNotNull(rec);
        assertEquals(badtime, rec.getError(),
                "TSIGRecord error field must be BADTIME when supplied");
        assertEquals(Instant.now(FIXED_CLOCK), rec.getTimeSigned(),
                "timeSigned should be current instant for BADTIME case");
    }

    /* ---------------------------------------------------------------------- */
    /*  Full‑signature length verification (default true)                    */
    /* ---------------------------------------------------------------------- */

    @Test
    void fullSignatureLengthMatchesAlgorithmOutput() throws Exception {
        // Build a message with a couple of records to make the MAC cover more data.
        Record q = Record.newRecord(Name.fromString("x.example."), Type.A, DClass.IN);
        Message msg = Message.newQuery(q);
        msg.addRecord(Record.newRecord(Name.fromString("x.example."), Type.A, DClass.IN, 300,
                "1.2.3.4"), Section.ANSWER);

        tsig.apply(msg, Rcode.NOERROR, null);
        TSIGRecord rec = msg.getTSIG();
        assertNotNull(rec);
        // HMAC‑SHA256 produces a 32‑byte MAC.
        assertEquals(32, rec.getMac().length,
                "MAC length must match the output size of the selected algorithm (SHA‑256 => 32 bytes)");
    }

    /* ---------------------------------------------------------------------- */
    /*  Unsupported algorithm name                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    void unsupportedAlgorithmNameThrowsIllegalArgumentException() {
        Name unknownAlg = Name.fromConstantString("HMAC-FOO.");
        TSIG badAlgTsig = new TSIG(unknownAlg, Name.fromString("example."), "12345678", FIXED_CLOCK);
        Message msg = Message.newQuery(Record.newRecord(Name.fromString("f.example."), Type.A, DClass.IN));
        assertThrows(IllegalArgumentException.class,
                () -> badAlgTsig.apply(msg, Rcode.NOERROR, null),
                "Using an unknown algorithm name must cause IllegalArgumentException");
    }

    /* ---------------------------------------------------------------------- */
    /*  Old TSIG MAC length mismatch                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    void oldTSIGMacLengthMismatchThrowsIllegalArgumentException() throws Exception {
        // Old TSIGRecord reports a MAC length that does not match the algorithm's output.
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getKeyName()).thenReturn(Name.fromString("example."));
        when(old.getMac()).thenReturn(new byte[10]); // truncated MAC
        when(old.getTimeSigned()).thenReturn(Instant.now(FIXED_CLOCK));

        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(message, Rcode.NOERROR, old),
                "MAC length mismatch must be rejected");
    }

    /* ---------------------------------------------------------------------- */
    /*  Error code out of valid range                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    void errorCodeOutsideValidRangeThrowsIllegalArgumentException() {
        int tooLarge = 70000; // exceeds 16‑bit DNS RCODE range
        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(message, tooLarge, null),
                "RCODE values outside 0‑65535 must be rejected");
    }

    /* ---------------------------------------------------------------------- */
    /*  Multiple TSIG records already present                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    void messageWithMultipleTSIGRecordsThrowsIllegalStateException() throws Exception {
        // Manually add two TSIG records to the message (the library does not expose a
        // direct API, so we use reflection to inject the inconsistency).
        TSIGRecord first = tsig.generate(message, new byte[0], Rcode.NOERROR, null);
        TSIGRecord second = tsig.generate(message, new byte[0], Rcode.NOERROR, null);
        // The Message class stores TSIG records in a private field; we set it via reflection.
        java.lang.reflect.Field tsigField = Message.class.getDeclaredField("tsig");
        tsigField.setAccessible(true);
        tsigField.set(message, new TSIGRecord[]{first, second}); // simulate two TSIGs

        assertThrows(IllegalStateException.class,
                () -> tsig.apply(message, Rcode.NOERROR, null),
                "Message containing more than one TSIG must cause IllegalStateException");
    }

    /* ---------------------------------------------------------------------- */
    /*  Old TSIG key name mismatch                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void oldTSIGKeyNameMismatchThrowsIllegalArgumentException() throws Exception {
        // Old TSIGRecord uses a different key name.
        TSIGRecord old = mock(TSIGRecord.class);
        when(old.getAlgorithm()).thenReturn(TSIG.HMAC_SHA256);
        when(old.getKeyName()).thenReturn(Name.fromString("different."));
        when(old.getMac()).thenReturn(new byte[32]);
        when(old.getTimeSigned()).thenReturn(Instant.now(FIXED_CLOCK));

        assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(message, Rcode.NOERROR, old),
                "Key name mismatch between old TSIG and TSIG instance must be rejected");
    }
}
```