```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional unit tests for {@link TSIG#apply(Message, int, TSIGRecord, boolean)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>basic successful signing (already present in the original test class),</li>
 *   <li>exceptional paths such as null returns, thrown exceptions, and invalid arguments,</li>
 *   <li>verification that all parameters are correctly forwarded to {@code generate},</li>
 *   <li>state‑overwrites and propagation of runtime exceptions.</li>
 * </ul>
 *
 * <p>All tests run with a deterministic {@link Clock} injected via reflection so that
 * timestamps inside generated {@link TSIGRecord}s are repeatable.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_4_Test {

    /** A deterministic clock so that the generated TSIG timestamps are repeatable. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

    /** TSIG instance used in tests. */
    private TSIG tsig;

    /** A simple query message used as the target of {@code apply}. */
    private Message queryMessage;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Use HMAC‑SHA256 algorithm with a short base‑64 key.
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");

        // Replace the internal clock with a fixed one to make the signature deterministic.
        // The constructor that accepts a Clock is package‑private, so we use reflection.
        try {
            Field clockField = TSIG.class.getDeclaredField("clock");
            clockField.setAccessible(true);
            clockField.set(tsig, FIXED_CLOCK);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        // Build a minimal DNS query message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------
     *  Original tests (kept unchanged)
     * -------------------------------------------------------------- */

    /**
     * Verify that {@code apply} adds a TSIGRecord to the additional section and sets the
     * {@code tsigState} flag.
     */
    @Test
    void applyAddsTSIGRecordAndSetsState() {
        // Act
        tsig.apply(queryMessage, Rcode.NOERROR, null, true);

        // Assert that the message state has been updated.
        assertEquals(Message.TSIG_SIGNED, queryMessage.tsigState,
                "Message tsigState should be TSIG_SIGNED after apply");

        // The additional section must contain exactly one record and it must be a TSIGRecord.
        Record[] additionals = queryMessage.getSectionArray(Section.ADDITIONAL);
        assertEquals(1, additionals.length, "There should be one additional record");
        assertTrue(additionals[0] instanceof TSIGRecord,
                "The additional record must be a TSIGRecord");

        TSIGRecord tsigRecord = (TSIGRecord) additionals[0];
        assertEquals(Rcode.NOERROR, tsigRecord.getError(),
                "TSIGRecord error field should match the supplied error code");
    }

    /**
     * Verify that {@code apply} works when an existing TSIGRecord (the “old” record) is supplied
     * and that the {@code fullSignature} flag does not prevent record creation.
     */
    @Test
    void applyWithOldRecordAndPartialSignature() throws TextParseException {
        // Create an old TSIGRecord – this mimics a previous request/response pair.
        TSIGRecord old = new TSIGRecord(
                Name.fromString("example."),          // name
                DClass.IN,                            // dclass
                0L,                                   // ttl
                TSIG.HMAC_SHA256,                     // algorithm name
                Instant.now(FIXED_CLOCK),             // timeSigned (deterministic)
                TSIG.FUDGE,                           // fudge
                new byte[0],                          // empty signature
                0,                                    // originalID
                Rcode.NOERROR,                        // error
                new byte[0]                           // other
        );

        // Act – use a partial signature (fullSignature = false)
        tsig.apply(queryMessage, Rcode.NOERROR, old, false);

        // Assert the same invariants as the previous test.
        assertEquals(Message.TSIG_SIGNED, queryMessage.tsigState,
                "Message tsigState should be TSIG_SIGNED after apply with old record");

        Record[] additionals = queryMessage.getSectionArray(Section.ADDITIONAL);
        assertEquals(1, additionals.length, "There should be one additional record after apply with old record");
        assertTrue(additionals[0] instanceof TSIGRecord,
                "The additional record must be a TSIGRecord after apply with old record");

        TSIGRecord generated = (TSIGRecord) additionals[0];
        assertEquals(Rcode.NOERROR, generated.getError(),
                "Generated TSIGRecord error should match supplied error code");
    }

    /* --------------------------------------------------------------
     *  Additional tests covering the JSON test matrix
     * -------------------------------------------------------------- */

    /**
     * Verify that {@code apply} propagates a {@link NullPointerException} when the
     * internal {@code generate} method returns {@code null}.
     */
    @Test
    void applyGenerateReturnsNull_throwsNPE() throws Exception {
        // Spy on TSIG and force generate to return null.
        TSIG spyTsig = spy(tsig);
        doReturn(null).when(spyTsig).generate(any(Message.class), any(byte[].class), anyInt(),
                isNull(), anyBoolean());

        Message mockMsg = mock(Message.class);
        when(mockMsg.toWire()).thenReturn(new byte[0]);

        // Expect a NPE because addRecord(null, ...) will be invoked.
        assertThrows(NullPointerException.class,
                () -> spyTsig.apply(mockMsg, 0, null, false),
                "apply should throw NullPointerException when generate returns null");
    }

    /**
     * Verify that {@code apply} forwards *exactly* the parameters received from the
     * {@link Message} to the public {@code generate} overload.
     */
    @Test
    void applyForwardsAllParameters_correctly() throws Exception {
        // Prepare a spy TSIG so we can verify the generate call.
        TSIG spyTsig = spy(tsig);

        // Prepare a Message spy with a deterministic wire representation.
        Message spyMsg = spy(queryMessage);
        byte[] wire = new byte[] {0x01, 0x02, 0x03};
        doReturn(wire).when(spyMsg).toWire();

        // Stub generate to return a dummy TSIGRecord.
        TSIGRecord dummyRecord = mock(TSIGRecord.class);
        doReturn(dummyRecord).when(spyTsig).generate(eq(spyMsg), eq(wire), eq(42), any(TSIGRecord.class), eq(true));

        // Act
        spyTsig.apply(spyMsg, 42, mock(TSIGRecord.class), true);

        // Verify generate was called exactly once with the exact arguments.
        verify(spyTsig, times(1)).generate(eq(spyMsg), eq(wire), eq(42), any(TSIGRecord.class), eq(true));

        // Verify that the returned record is added to the ADDITIONAL section.
        InOrder inOrder = inOrder(spyMsg);
        inOrder.verify(spyMsg).addRecord(dummyRecord, Section.ADDITIONAL);
        inOrder.verify(spyMsg).tsigState = Message.TSIG_SIGNED; // direct field assignment cannot be verified;
        // Instead we assert the state after the call.
        assertEquals(Message.TSIG_SIGNED, spyMsg.tsigState,
                "Message tsigState should be TSIG_SIGNED after successful apply");
    }

    /**
     * Ensure that an existing {@code tsigState} (e.g., {@link Message#TSIG_VERIFIED}) is
     * overwritten by {@code apply}.
     */
    @Test
    void applyOverwritesExistingTsigState() throws Exception {
        // Mock a Message with an initial state.
        Message mockMsg = mock(Message.class);
        // Use a real Message instance to hold the mutable field, then spy it.
        Message realMsg = Message.newQuery(Record.newRecord(Name.fromString("a.example."), Type.A, DClass.IN));
        Field stateField = Message.class.getDeclaredField("tsigState");
        stateField.setAccessible(true);
        stateField.setInt(realMsg, Message.TSIG_VERIFIED); // assume such constant exists

        Message spyMsg = spy(realMsg);
        when(spyMsg.toWire()).thenReturn(new byte[0]);

        tsig.apply(spyMsg, Rcode.NOERROR, null, false);

        assertEquals(Message.TSIG_SIGNED, spyMsg.tsigState,
                "apply should overwrite any previous tsigState with TSIG_SIGNED");
    }

    /**
     * Verify that exceptions thrown by {@link Message#addRecord(Record, int)} propagate out of
     * {@code apply}.
     */
    @Test
    void applyPropagatesExceptionFromAddRecord() throws Exception {
        Message mockMsg = mock(Message.class);
        when(mockMsg.toWire()).thenReturn(new byte[0]);
        doThrow(new IllegalStateException("boom")).when(mockMsg).addRecord(any(Record.class), eq(Section.ADDITIONAL));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> tsig.apply(mockMsg, 0, null, false),
                "Exception from Message.addRecord should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Verify that a non‑null previous {@link TSIGRecord}, a non‑zero error code and
     * {@code fullSignature=true} are all correctly handled.
     */
    @Test
    void apply_oldNonNull_errorNonZero_fullSignatureTrue() throws Exception {
        TSIGRecord old = mock(TSIGRecord.class);
        tsig.apply(queryMessage, 9, old, true);

        assertEquals(Message.TSIG_SIGNED, queryMessage.tsigState,
                "Message should be marked as signed even with non‑zero error and fullSignature=true");
        Record[] additionals = queryMessage.getSectionArray(Section.ADDITIONAL);
        assertEquals(1, additionals.length);
        assertTrue(additionals[0] instanceof TSIGRecord);
        TSIGRecord generated = (TSIGRecord) additionals[0];
        assertEquals(9, generated.getError(),
                "Generated TSIGRecord must contain the supplied error code");
    }

    /**
     * Verify that when {@code old == null}, a non‑zero error and {@code fullSignature=true}
     * still result in a signed message.
     */
    @Test
    void apply_oldNull_errorNonZero_fullSignatureTrue() throws Exception {
        tsig.apply(queryMessage, 5, null, true);

        assertEquals(Message.TSIG_SIGNED, queryMessage.tsigState);
        Record[] additionals = queryMessage.getSectionArray(Section.ADDITIONAL);
        assertEquals(1, additionals.length);
        TSIGRecord generated = (TSIGRecord) additionals[0];
        assertEquals(5, generated.getError(),
                "Generated TSIGRecord must contain the supplied error code");
    }

    /**
     * Passing {@code null} as the {@link Message} argument must result in a
     * {@link NullPointerException}.
     */
    @Test
    void apply_nullMessage_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> tsig.apply(null, 0, null, false),
                "apply should throw NPE when the Message argument is null");
    }

    /**
     * If {@link Message#toWire()} throws a runtime exception, {@code apply} must not catch it.
     */
    @Test
    void apply_messageToWireThrows_exceptionPropagates() throws Exception {
        Message mockMsg = mock(Message.class);
        when(mockMsg.toWire()).thenThrow(new IllegalStateException("wire error"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> tsig.apply(mockMsg, 0, null, false),
                "Exception from Message.toWire should propagate");
        assertEquals("wire error", ex.getMessage());
    }

    /**
     * Stub the public {@code generate} method to throw a {@link RuntimeException} and verify that
     * {@code apply} lets the exception bubble up.
     */
    @Test
    void apply_generateThrowsRuntimeException() throws Exception {
        TSIG spyTsig = spy(tsig);
        doThrow(new RuntimeException("generation failure"))
                .when(spyTsig).generate(any(Message.class), any(byte[].class), anyInt(),
                        any(TSIGRecord.class), anyBoolean());

        Message mockMsg = mock(Message.class);
        when(mockMsg.toWire()).thenReturn(new byte[0]);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> spyTsig.apply(mockMsg, 0, null, false),
                "Exception from generate should propagate");
        assertEquals("generation failure", ex.getMessage());
    }

    /**
     * Supplying a negative DNS error code should cause {@code generate} to throw an
     * {@link IllegalArgumentException}, which must propagate through {@code apply}.
     */
    @Test
    void apply_negativeErrorCode_throwsIllegalArgumentException() {
        // The implementation of generate validates the error code; we simply assert the propagation.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(queryMessage, -1, null, false),
                "Negative error code should cause IllegalArgumentException");
        // The exact message is implementation‑dependent; we only check the type.
    }

    /**
     * When the supplied previous {@link TSIGRecord} has a {@code null} algorithm name,
     * {@code generate} is expected to reject it with an {@link IllegalArgumentException}.
     */
    @Test
    void apply_oldRecordWithNullAlgorithm_throwsIllegalArgumentException() {
        TSIGRecord badOld = mock(TSIGRecord.class);
        when(badOld.getAlgorithm()).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tsig.apply(queryMessage, 0, badOld, false),
                "Old TSIGRecord with null algorithm should cause IllegalArgumentException");
    }
}
```