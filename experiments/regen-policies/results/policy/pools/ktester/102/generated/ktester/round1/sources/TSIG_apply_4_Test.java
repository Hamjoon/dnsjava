package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TSIG_apply_4_Test {

    /**
     * A deterministic clock so that generated timestamps are predictable.
     */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /**
     * The TSIG instance under test – we will spy on it to stub {@code generate}.
     */
    private TSIG tsigSpy;

    /**
     * Fresh message for each test.
     */
    private Message message;

    /**
     * A dummy old TSIG record – can be {@code null} for most tests.
     */
    private TSIGRecord oldRecord;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a real TSIG key (algorithm HMAC‑SHA256, name "example.", key "12345678").
        TSIG realTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");
        // Replace its clock with a fixed one to make the generated timestamp deterministic.
        Field clockField = TSIG.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(realTsig, FIXED_CLOCK);
        // Spy on the TSIG instance so we can stub the generate method when needed.
        tsigSpy = spy(realTsig);
        // Prepare a simple query message – the actual content is irrelevant for the
        // behaviour we are testing.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        message = Message.newQuery(question);
        // No previous TSIG record for these tests.
        oldRecord = null;
    }

    /**
     * Verify that {@code apply(Message, int, TSIGRecord, boolean)} adds the generated
     * {@link TSIGRecord} to the additional section and sets the internal {@code tsigState}
     * to {@link Message#TSIG_SIGNED}.
     */
    @Test
    void testApplyAddsRecordAndSetsState() throws Exception {
        // Arrange ------------------------------------------------------------
        // Stub the generate method to return a mock TSIGRecord.
        TSIGRecord generatedRecord = mock(TSIGRecord.class);
        when(tsigSpy.generate(any(Message.class), any(byte[].class), anyInt(), any(TSIGRecord.class), anyBoolean())).thenReturn(generatedRecord);
        // Act ----------------------------------------------------------------
        tsigSpy.apply(message, Rcode.NOERROR, oldRecord, true);
        // Assert -------------------------------------------------------------
        // 1. The generated record must be present in the ADDITIONAL section.
        List<Record> additionals = message.getSection(Section.ADDITIONAL);
        assertTrue(additionals.contains(generatedRecord), "Generated TSIGRecord should be added to the additional section");
        // 2. The internal tsigState must be TSIG_SIGNED.
        Field tsigStateField = Message.class.getDeclaredField("tsigState");
        tsigStateField.setAccessible(true);
        int tsigState = (int) tsigStateField.get(message);
        assertEquals(Message.TSIG_SIGNED, tsigState, "Message.tsigState should be set to TSIG_SIGNED after apply");
    }

    /**
     * Verify that the three overloads of {@code apply} delegate to the four‑parameter
     * implementation. The test uses a spy to ensure that the core method is invoked
     * exactly once for each overload.
     */
    @Test
    void testApplyOverloadsDelegate() throws Exception {
        // Arrange ------------------------------------------------------------
        TSIGRecord generatedRecord = mock(TSIGRecord.class);
        when(tsigSpy.generate(any(Message.class), any(byte[].class), anyInt(), any(TSIGRecord.class), anyBoolean())).thenReturn(generatedRecord);
        // Act & Assert --------------------------------------------------------
        // 1. apply(Message, TSIGRecord)
        tsigSpy.apply(message, oldRecord);
        verify(tsigSpy, times(1)).apply(eq(message), eq(Rcode.NOERROR), eq(oldRecord), eq(true));
        // Reset interactions for the next call.
        clearInvocations(tsigSpy);
        // 2. apply(Message, int, TSIGRecord)
        tsigSpy.apply(message, Rcode.FORMERR, oldRecord);
        verify(tsigSpy, times(1)).apply(eq(message), eq(Rcode.FORMERR), eq(oldRecord), eq(true));
        clearInvocations(tsigSpy);
        // 3. apply(Message, TSIGRecord, boolean)
        tsigSpy.apply(message, oldRecord, false);
        verify(tsigSpy, times(1)).apply(eq(message), eq(Rcode.NOERROR), eq(oldRecord), eq(false));
    }

    /**
     * {@code apply(null, …)} should throw {@link NullPointerException}
     * because the implementation immediately dereferences the message.
     */
    @Test
    void apply_nullMessage_throwsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> tsigSpy.apply(null, Rcode.NOERROR, null, false), "Calling apply with a null Message must throw NullPointerException");
    }

    /**
     * Supplying a negative DNS error code must result in
     * {@link IllegalArgumentException}. The validation occurs inside
     * {@code generate}, so we stub it to throw the exception when the error
     * argument is negative.
     */
    @Test
    void apply_negativeErrorCode_throwsIllegalArgumentException() throws Exception {
        // Arrange ------------------------------------------------------------
        // Stub generate to throw IllegalArgumentException for a negative error.
        doThrow(new IllegalArgumentException("error code < 0")).when(tsigSpy).generate(any(Message.class), any(byte[].class), eq(-1), any(TSIGRecord.class), anyBoolean());
        // Act & Assert --------------------------------------------------------
        assertThrows(IllegalArgumentException.class, () -> tsigSpy.apply(message, -1, null, false), "Negative error code should cause IllegalArgumentException");
    }

    /**
     * If {@code generate(...)} returns {@code null}, {@code apply} will try to
     * add a {@code null} record to the message, which results in a
     * {@link NullPointerException}.
     */
    @Test
    void apply_generateReturnsNull_throwsNullPointerException() throws Exception {
        // Arrange ------------------------------------------------------------
        // Stub generate to return null.
        when(tsigSpy.generate(any(Message.class), any(byte[].class), anyInt(), any(TSIGRecord.class), anyBoolean())).thenReturn(null);
        // Act & Assert --------------------------------------------------------
        assertThrows(NullPointerException.class, () -> tsigSpy.apply(message, Rcode.NOERROR, null, false), "When generate returns null, apply should throw NullPointerException");
    }
}
