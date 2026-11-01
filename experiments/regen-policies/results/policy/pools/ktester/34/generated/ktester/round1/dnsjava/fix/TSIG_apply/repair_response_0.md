```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.InetAddress;
import org.xbill.DNS.utils.base64;
import org.xbill.DNS.TextParseException;   // added import

@ExtendWith(MockitoExtension.class)
class TSIG_apply_Test {

    /**
     * A deterministic clock so that the generated timestamps are repeatable.
     */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));

    /**
     * The TSIG key used for all tests.
     */
    private TSIG tsig;

    @BeforeEach
    void setupBeforeEach() throws TextParseException {   // added throws clause
        // Use the HMAC‑SHA256 algorithm with a known key.  The constructor that accepts a
        // Clock lets us keep the timestamp stable, which makes verification deterministic.
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(base64.fromString("12345678"), "HmacSHA256"), FIXED_CLOCK);
    }

    /**
     * Supplies the argument combinations for {@link #applyNullMessageArgumentThrows(Message, TSIGRecord)}.
     */
    private static Stream<Arguments> nullArgumentProvider() {
        TSIGRecord dummy = mock(TSIGRecord.class);
        return Stream.of(Arguments.of(null, dummy), Arguments.of(null, null));
    }

    /**
     * Verify that {@code apply(message, null)} adds a TSIG record, that the message reports being
     * signed, and that the generated signature can be verified.
     */
    @Test
    void applyAddsSignatureAndVerificationSucceeds() throws IOException, TextParseException {   // added TextParseException
        // Build a simple query.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        // Apply the TSIG (old record is {@code null}).
        tsig.apply(query, null);
        // The message must now be marked as signed.
        assertTrue(query.isSigned(), "Message should be marked as signed after apply");
        // Wire‑format the message and verify the signature.
        byte[] wire = query.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        int result = tsig.verify(parsed, wire, null);
        assertEquals(Rcode.NOERROR, result, "Verification of a freshly signed query must succeed");
        assertTrue(parsed.isVerified(), "Parsed message should be marked as verified");
    }

    /**
     * Verify the overload {@code apply(message, oldTSIG, true)} where an existing TSIG record from a
     * previous message is supplied.  The new message must contain a valid TSIG that can be verified
     * using the original TSIG as the “request” record.
     */
    @Test
    void applyWithOldRecordFullSignatureSucceeds() throws IOException, TextParseException {   // added TextParseException
        // ----------------------------------------------------------------------
        // 1. Create and sign a query – this gives us an “old” TSIG record.
        // ----------------------------------------------------------------------
        Record qQuestion = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message query = Message.newQuery(qQuestion);
        tsig.apply(query, null);
        TSIGRecord oldTsig = query.getTSIG();
        assertNotNull(oldTsig, "The query must contain a TSIG record after signing");
        // ----------------------------------------------------------------------
        // 2. Build a response that references the old TSIG record.
        // ----------------------------------------------------------------------
        Message response = new Message(query.getHeader().getID());
        // mark as a response
        response.getHeader().setFlag(Flags.QR);
        response.addRecord(query.getQuestion(), Section.QUESTION);
        // add a dummy answer so the message is not empty
        Record answer = new ARecord(qQuestion.getName(), DClass.IN, 300, InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 }));
        response.addRecord(answer, Section.ANSWER);
        // ----------------------------------------------------------------------
        // 3. Apply the TSIG using the overload that receives the old record and requests a
        //    full signature.
        // ----------------------------------------------------------------------
        tsig.apply(response, oldTsig, true);
        // The response must now be signed.
        assertTrue(response.isSigned(), "Response should be signed after apply with old record");
        // ----------------------------------------------------------------------
        // 4. Verify the response using the original TSIG as the request TSIG.
        // ----------------------------------------------------------------------
        byte[] responseWire = response.toWire(Message.MAXLENGTH);
        Message parsedResponse = new Message(responseWire);
        int verifyResult = tsig.verify(parsedResponse, responseWire, oldTsig);
        assertEquals(Rcode.NOERROR, verifyResult, "Verification of the response that used the old TSIG record must succeed");
        assertTrue(parsedResponse.isVerified(), "Parsed response should be marked as verified");
    }

    /**
     * The one‑argument overload should delegate to {@code apply(Message, int, TSIGRecord, boolean)}
     * with {@code error = Rcode.NOERROR} and {@code fullSignature = true}. This test uses a Mockito
     * spy to capture the delegation.
     */
    @Test
    void applyDelegatesToFourArgumentOverload() {
        // Spy on the real TSIG instance so that we can verify internal calls.
        TSIG spyTsig = spy(tsig);
        // an empty message is sufficient
        Message msg = new Message();
        TSIGRecord old = mock(TSIGRecord.class);
        // Stub the four‑argument overload to do nothing – we only care about the delegation.
        doNothing().when(spyTsig).apply(eq(msg), eq(Rcode.NOERROR), eq(old), eq(true));
        // Call the one‑argument overload.
        spyTsig.apply(msg, old);
        // Verify that the four‑argument overload was invoked exactly once with the expected arguments.
        verify(spyTsig, times(1)).apply(msg, Rcode.NOERROR, old, true);
    }

    /**
     * The method must reject a {@code null} {@link Message}. The expected behaviour of the library
     * is to throw a {@link NullPointerException}.
     */
    @Test
    void applyThrowsWhenMessageIsNull() {
        TSIGRecord old = mock(TSIGRecord.class);
        assertThrows(NullPointerException.class, () -> tsig.apply(null, old), "apply(null, old) should throw NullPointerException");
    }

    /**
     * When both arguments are {@code null} the method should also fail fast.
     */
    @Test
    void applyThrowsWhenBothArgumentsNull() {
        assertThrows(NullPointerException.class, () -> tsig.apply(null, null), "apply(null, null) should throw NullPointerException");
    }

    /**
     * Parameterised test covering the null‑argument combinations that are expected to raise an
     * exception. This demonstrates the contract that a {@code null} {@link Message} is not allowed,
     * regardless of the state of the {@link TSIGRecord} argument.
     */
    @ParameterizedTest
    @MethodSource("nullArgumentProvider")
    void applyNullMessageArgumentThrows(Message m, TSIGRecord old) {
        assertThrows(NullPointerException.class, () -> tsig.apply(m, old), "apply(null Message, ...) must throw NullPointerException");
    }
}
```