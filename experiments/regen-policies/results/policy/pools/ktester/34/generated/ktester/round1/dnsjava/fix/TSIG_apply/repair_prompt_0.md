
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
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
    void setupBeforeEach() {
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
    void applyAddsSignatureAndVerificationSucceeds() throws IOException {
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
    void applyWithOldRecordFullSignatureSucceeds() throws IOException {
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

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/TSIG_apply_Test.java:41: error: unreported exception TextParseException; must be caught or declared to be thrown
        tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(base64.fromString("12345678"), "HmacSHA256"), FIXED_CLOCK);
                                                         ^
1 error

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.TSIG': '\n * Transaction signature handling. This class generates and verifies TSIG records on messages, which\n * provide transaction security.\n *\n * @see TSIGRecord\n * @author Brian Wellington\n ', 'api document of method apply(Message, TSIGRecord)': '\n   * Generates a TSIG record for a message and adds it to the message\n   *\n   * @param m The message\n   * @param old If this message is a response, the TSIG from the request\n   ', 'dependent classes': 'class `org.xbill.DNS.Message`:\n\trelated functions:\n\t\tmethod `public void setTSIG(TSIG)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord)`, api document: \n   * Sets the TSIG key to sign a message.\n   *\n   * @param key The TSIG key.\n   * @since 3.5.1\n   ;\nclass `org.xbill.DNS.TSIGRecord`:\n\t\nclass `org.xbill.DNS.Rcode`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Rcode.NOERROR;\n\t\nclass `org.xbill.DNS.TSIG`:\n\trelated functions:\n\t\tmethod `public TSIGRecord generate(Message, byte[], int, TSIGRecord, boolean)` returns `org.xbill.DNS.TSIGRecord`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord)`, api document: \n   * Generates a TSIG record with a specific error for a message that has been rendered.\n   *\n   * @param m The message\n   * @param b The rendered message\n   * @param error The error\n   * @param old If this message is a response, the TSIG from the request\n   * @param fullSignature {@code true} if this {@link TSIGRecord} is the to be added to the first of\n   *     many messages in a TCP connection and all TSIG variables (rfc2845, 3.4.2.) should be\n   *     included in the signature. {@code false} for subsequent messages with reduced TSIG\n   *     variables set (rfc2845, 4.4.).\n   * @return The TSIG record to be added to the message\n   * @since 3.2\n   ;\n\t\tmethod `public void apply(Message, int, TSIGRecord)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord)`, api document: \n   * Generates a TSIG record with a specific error for a message and adds it to the message.\n   *\n   * @param m The message\n   * @param error The error\n   * @param old If this message is a response, the TSIG from the request\n   ;\nclass `org.xbill.DNS.SimpleResolver`:\n\trelated functions:\n\t\tmethod `public CompletionStage<Message> sendAsync(Message, Executor)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.Message>`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord)`, api document: \n   * Asynchronously sends a message to a single server.\n   *\n   * @param query The query to send\n   * @param executor The service to use for async operations.\n   * @return A future that completes when the response has arrived.\n   ;\nclass `org.xbill.DNS.dnssec.ValidatingResolver`:\n\trelated functions:\n\t\tmethod `public CompletionStage<Message> sendAsync(Message, Executor)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.Message>`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord)`, api document: \n   * Asynchronously sends a message and validates the response with DNSSEC before returning it.\n   *\n   * @param query The query to send.\n   * @param executor The service to use for async operations.\n   * @return A future that completes when the query is finished.\n   ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


