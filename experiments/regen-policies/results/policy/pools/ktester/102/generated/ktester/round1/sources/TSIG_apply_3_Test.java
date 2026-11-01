package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class TSIG_apply_3_Test {

    /**
     * A TSIG key used for all tests.
     */
    private static TSIG tsigKey;

    /**
     * A simple DNS query message used as the base for the tests.
     */
    private Message queryMessage;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the HMAC‑SHA256 algorithm, a dummy domain name and a short base‑64 key.
        // The constructor resolves the algorithm name to the appropriate MAC algorithm.
        tsigKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), "12345678");
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a minimal query:  www.example.  IN  A
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    static Stream<Arguments> nullMessageProvider() {
        return Stream.of(// case 1: all nulls
        Arguments.of(null, 0, null), // case 2: null Message but non‑null old TSIGRecord
        Arguments.of(null, 0, new TSIGRecord(Name.fromString("example.com."), TSIG.HMAC_SHA256, 0, new byte[0], 0, 0, 0, new byte[0])));
    }

    static Stream<Arguments> invalidErrorCodeProvider() {
        return Stream.of(// negative error code
        Arguments.of(new Message(), -1, null), // error code far beyond defined TSIG errors (RFC 2845 defines up to 16)
        Arguments.of(new Message(), 9999, null));
    }

    /**
     * Verify that {@code apply} adds a TSIG record with a NOERROR status and that the
     * resulting message can be verified successfully.
     */
    @Test
    void applyAddsSignatureAndVerifiesSuccessfully() throws IOException {
        // Apply a TSIG with Rcode.NOERROR (0) and no previous TSIG record.
        tsigKey.apply(queryMessage, Rcode.NOERROR, null);
        // Serialize the signed message and parse it back.
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        // The verification should succeed and the message should be marked as signed/verified.
        int result = tsigKey.verify(parsed, wire, null);
        assertEquals(Rcode.NOERROR, result, "Verification should return NOERROR");
        assertTrue(parsed.isSigned(), "Message should be marked as signed");
        assertTrue(parsed.isVerified(), "Message should be marked as verified");
    }

    /**
     * Verify that the error code supplied to {@code apply} is stored in the generated
     * {@link TSIGRecord}. The verification routine should return the same error code.
     */
    @Test
    void applyPropagatesErrorCodeInSignature() throws IOException {
        // Use a non‑zero error code (e.g., BADKEY = 16) to see that it is reflected.
        final int errorCode = Rcode.BADKEY;
        tsigKey.apply(queryMessage, errorCode, null);
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        TSIGRecord tsigRecord = parsed.getTSIG();
        assertNotNull(tsigRecord, "TSIG record must be present");
        assertEquals(errorCode, tsigRecord.getError(), "TSIGRecord.error should match the supplied error code");
        // Verification should return the same error code.
        int verifyResult = tsigKey.verify(parsed, wire, null);
        assertEquals(errorCode, verifyResult, "Verification should return the supplied error code");
    }

    /**
     * Ensure that calling {@code apply} with a previous TSIG record (the “old” argument)
     * does not throw and still produces a verifiable signature.
     */
    @Test
    void applyWithPreviousTsigDoesNotThrowAndVerifies() throws IOException {
        // First, sign the message to obtain an initial TSIG record.
        tsigKey.apply(queryMessage, Rcode.NOERROR, null);
        byte[] firstWire = queryMessage.toWire(Message.MAXLENGTH);
        Message firstParsed = new Message(firstWire);
        TSIGRecord firstTsig = firstParsed.getTSIG();
        assertNotNull(firstTsig, "Initial TSIG record must exist");
        // Create a fresh message (e.g., a response) and apply using the previous TSIG.
        Record answer = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        Message response = Message.newResponse(queryMessage);
        response.addRecord(answer, Section.ANSWER);
        // Apply using the previously generated TSIG as the “old” record.
        assertDoesNotThrow(() -> tsigKey.apply(response, Rcode.NOERROR, firstTsig), "apply should not throw when an old TSIGRecord is supplied");
        byte[] responseWire = response.toWire(Message.MAXLENGTH);
        Message parsedResponse = new Message(responseWire);
        // Verify the response signature.
        int verifyResult = tsigKey.verify(parsedResponse, responseWire, null);
        assertEquals(Rcode.NOERROR, verifyResult, "Verification of response should succeed");
        assertTrue(parsedResponse.isSigned(), "Response should be marked as signed");
        assertTrue(parsedResponse.isVerified(), "Response should be marked as verified");
    }

    /* -------------------------------------------------------------
     *  Group: apply_nullMessage
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] null Message should throw NPE")
    @MethodSource("nullMessageProvider")
    void apply_nullMessage(Message m, int error, TSIGRecord old) {
        assertThrows(NullPointerException.class, () -> tsigKey.apply(m, error, old), "Calling apply with a null Message must throw NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Group: apply_invalidErrorCode
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] error={1} should throw IllegalArgumentException")
    @MethodSource("invalidErrorCodeProvider")
    void apply_invalidErrorCode(Message m, int error, TSIGRecord old) {
        assertThrows(IllegalArgumentException.class, () -> tsigKey.apply(m, error, old), "Negative or out‑of‑range error codes must cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Group: apply_unsupportedAlgorithm
     * ------------------------------------------------------------- */
    @Test
    void apply_unsupportedAlgorithm_throwsGeneralSecurityException() {
        // Build a TSIGRecord that references an unknown algorithm name.
        TSIGRecord unsupported = new TSIGRecord(Name.fromString("example.com."), // algorithm not recognised by TSIG
        Name.fromString("unsupported-alg."), 0, new byte[0], 0, 0, 0, new byte[0]);
        // empty message
        Message m = new Message();
        assertThrows(GeneralSecurityException.class, () -> tsigKey.apply(m, Rcode.NOERROR, unsupported), "Using an unsupported algorithm should raise GeneralSecurityException");
    }

    /* -------------------------------------------------------------
     *  Group: apply_messageAlreadySigned
     * ------------------------------------------------------------- */
    @Test
    void apply_messageAlreadySigned_throwsIllegalStateException() throws IOException {
        // First sign the message – this adds a TSIG record.
        tsigKey.apply(queryMessage, Rcode.NOERROR, null);
        assertNotNull(queryMessage.getTSIG(), "Message should contain a TSIG after first apply");
        // Attempt to apply a second TSIG to the same message.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> tsigKey.apply(queryMessage, Rcode.NOERROR, null), "Applying TSIG to a message that already has a TSIG must throw IllegalStateException");
        // Optional sanity check on the exception message (implementation‑specific).
        assertTrue(ex.getMessage().toLowerCase().contains("already signed"), "Exception message should indicate the message is already signed");
    }
}
