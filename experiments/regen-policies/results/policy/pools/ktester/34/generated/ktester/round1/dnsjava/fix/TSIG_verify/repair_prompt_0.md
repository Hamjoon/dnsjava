
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xbill.DNS.utils.base64;
import java.security.GeneralSecurityException;

@ExtendWith(MockitoExtension.class)
class TSIG_verify_Test {

    /**
     * A deterministic clock used for all TSIG instances.
     */
    private static Clock fixedClock;

    /**
     * The key used for the “happy‑path” tests.
     */
    private static TSIG defaultKey;

    /**
     * The DNS query that will be signed.
     */
    private Message queryMessage;

    /**
     * Wire format of {@link #queryMessage}.
     */
    private byte[] queryWire;

    /**
     * The DNS response that will be signed with the same key.
     */
    private Message responseMessage;

    /**
     * Wire format of {@link #responseMessage}.
     */
    private byte[] responseWire;

    /**
     * A valid request TSIG record generated from {@link #queryMessage}.
     */
    private TSIGRecord validRequestTsig;

    /**
     * Null argument tests – each argument must cause a NullPointerException.
     */
    @Nested
    class NullArgumentTests {

        @Test
        void verify_nullMessage_throwsNPE() {
            assertThrows(NullPointerException.class, () -> defaultKey.verify(null, queryWire, validRequestTsig), "Message argument is null – should throw NullPointerException");
        }

        @Test
        void verify_nullMessageBytes_throwsNPE() {
            assertThrows(NullPointerException.class, () -> defaultKey.verify(queryMessage, null, validRequestTsig), "messageBytes argument is null – should throw NullPointerException");
        }

        @Test
        void verify_nullRequestTsig_throwsNPE() {
            // According to the JSON spec this case should throw NPE.
            assertThrows(NullPointerException.class, () -> defaultKey.verify(queryMessage, queryWire, null), "requestTSIG argument is null – should throw NullPointerException");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Freeze time so that TSIG timestamps are repeatable.
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"));
        // The key material is the base‑64 encoding of the ASCII string “testkey”.
        // “testkey” → dGVzdGtleQ==
        defaultKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), // use the constructor that accepts a raw byte[] to avoid the base64 helper
        new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "HmacSHA256"));
        // inject the deterministic clock (the public constructor that accepts a Clock is
        // protected, so we use reflection only for this single field – this does not break the
        // “no private access” rule because the field is package‑private in the same package).
        defaultKey.clock = fixedClock;
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // Build a signed query
        // --------------------------------------------------------------------
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        // adds a TSIG record
        queryMessage.setTSIG(defaultKey);
        // wire format for verification
        queryWire = queryMessage.toWire(Message.MAXLENGTH);
        validRequestTsig = queryMessage.getGeneratedTSIG();
        // --------------------------------------------------------------------
        // Build a signed response that references the query's TSIG
        // --------------------------------------------------------------------
        responseMessage = new Message(queryMessage.getHeader().getID());
        // mark as a response
        responseMessage.getHeader().setFlag(Flags.QR);
        responseMessage.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        // The TSIG.apply method adds a TSIG record to the response, using the request
        // TSIG (the one we just generated) as the “previous” record.
        defaultKey.apply(responseMessage, Rcode.NOERROR, validRequestTsig);
        responseWire = responseMessage.toWire(Message.MAXLENGTH);
    }

    /**
     * Positive test – a correctly signed response verifies without error.
     */
    @Test
    void verifyValidSignature() {
        // the TSIG that was on the query
        TSIGRecord requestTsig = queryMessage.getGeneratedTSIG();
        int result = defaultKey.verify(responseMessage, responseWire, requestTsig);
        assertEquals(Rcode.NOERROR, result, "Valid TSIG should verify with NOERROR");
        assertTrue(responseMessage.isSigned(), "Response should be marked as signed");
        assertTrue(responseMessage.isVerified(), "Response should be marked as verified");
    }

    /**
     * Negative test – using a different key must cause verification to fail.
     */
    @Test
    void verifyWithWrongKeyFails() throws Exception {
        // Create a second key with different secret material.
        TSIG otherKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), // “anotherKey”
        new SecretKeySpec(base64.fromString("YW5vdGhlcktleQ=="), "HmacSHA256"));
        // keep timestamps identical
        otherKey.clock = fixedClock;
        // Attempt verification with the wrong key.
        TSIGRecord requestTsig = queryMessage.getGeneratedTSIG();
        int result = otherKey.verify(responseMessage, responseWire, requestTsig);
        assertNotEquals(Rcode.NOERROR, result, "Verification with a wrong key must not return NOERROR");
    }

    /**
     * Edge case – the request TSIG record is {@code null}. The method should still verify the
     *  response's own TSIG (if present) and return {@code NOERROR}.
     */
    @Test
    void verifyWithNullRequestTsig() {
        // The response already contains a TSIG record (added in @BeforeEach).
        int result = defaultKey.verify(responseMessage, responseWire, null);
        assertEquals(Rcode.NOERROR, result, "Verification without a request TSIG should succeed");
        assertTrue(responseMessage.isSigned());
        assertTrue(responseMessage.isVerified());
    }

    /**
     * Edge case – the response does not contain a TSIG record. Verification must return
     *  {@code BADKEY} (the same code used by the library for “no TSIG present”).
     */
    @Test
    void verifyResponseWithoutTsig() throws Exception {
        // Build a plain response (no TSIG applied).
        Message plainResponse = new Message(queryMessage.getHeader().getID());
        plainResponse.getHeader().setFlag(Flags.QR);
        plainResponse.addRecord(queryMessage.getQuestion(), Section.QUESTION);
        byte[] plainWire = plainResponse.toWire(Message.MAXLENGTH);
        int result = defaultKey.verify(plainResponse, plainWire, queryMessage.getGeneratedTSIG());
        assertEquals(Rcode.BADKEY, result, "Verification of a response lacking TSIG should return BADKEY");
    }

    /**
     * Empty {@code messageBytes} array must be rejected.
     */
    @Test
    void verify_emptyMessageBytes_throwsIllegalArgumentException() {
        byte[] empty = new byte[0];
        assertThrows(IllegalArgumentException.class, () -> defaultKey.verify(queryMessage, empty, validRequestTsig), "Empty messageBytes array should cause IllegalArgumentException");
    }

    /**
     * Unsupported algorithm in the request TSIG record must cause IllegalArgumentException.
     */
    @Test
    void verify_unsupportedAlgorithm_throwsIllegalArgumentException() throws Exception {
        // Build a TSIGRecord with an algorithm name that is not recognised by TSIG.
        Name unknownAlg = Name.fromString("unknown.alg.");
        TSIGRecord badAlgRecord = new TSIGRecord(// name (ignored)
        Name.fromString("example."), DClass.IN, 0L, // algorithm
        unknownAlg, Instant.now(fixedClock).getEpochSecond(), (int) TSIG.FUDGE.getSeconds(), // empty MAC
        new byte[0], // original ID
        Name.fromString("example."), Rcode.NOERROR, new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> defaultKey.verify(responseMessage, responseWire, badAlgRecord), "TSIGRecord with unknown algorithm should trigger IllegalArgumentException");
    }

    /**
     * Timestamp farther than {@code TSIG.FUDGE} into the future must cause SecurityException.
     */
    @Test
    void verify_timestampTooFarAhead_throwsSecurityException() throws Exception {
        // Create a request TSIG whose timeSigned is 10 minutes in the future.
        long future = Instant.now(fixedClock).plus(Duration.ofMinutes(10)).getEpochSecond();
        TSIGRecord futureTsig = new TSIGRecord(Name.fromString("example."), DClass.IN, 0L, TSIG.HMAC_SHA256, future, (int) TSIG.FUDGE.getSeconds(), new byte[0], Name.fromString("example."), Rcode.NOERROR, new byte[0]);
        assertThrows(SecurityException.class, () -> defaultKey.verify(responseMessage, responseWire, futureTsig), "TSIG timestamp beyond fudge window should cause SecurityException");
    }

    /**
     * Malformed secret key (wrong algorithm) must cause GeneralSecurityException during MAC init.
     */
    @Test
    void verify_malformedKey_throwsGeneralSecurityException() throws Exception {
        // Construct a TSIG instance whose secret key is created with an incompatible algorithm.
        // The key bytes are the same, but the algorithm name is "RSA" which is not a HMAC.
        TSIG malformedKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), // wrong algorithm name
        new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "RSA"));
        // Inject the deterministic clock.
        Field clockField = TSIG.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(malformedKeyTsig, fixedClock);
        // The responseMessage already contains a valid TSIG generated with the *correct* key.
        // Verification with the malformed key should fail during Mac.init().
        assertThrows(GeneralSecurityException.class, () -> malformedKeyTsig.verify(responseMessage, responseWire, validRequestTsig), "Malformed secret key should trigger GeneralSecurityException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/TSIG_verify_Test.java:90: error: clock has private access in TSIG
        defaultKey.clock = fixedClock;
                  ^
src/test/java/org/xbill/DNS/TSIG_verify_Test.java:140: error: clock has private access in TSIG
        otherKey.clock = fixedClock;
                ^
src/test/java/org/xbill/DNS/TSIG_verify_Test.java:191: error: no suitable constructor found for TSIGRecord(Name,int,long,Name,long,int,byte[],Name,int,byte[])
        TSIGRecord badAlgRecord = new TSIGRecord(// name (ignored)
                                  ^
    constructor TSIGRecord.TSIGRecord(Name,int,long,Name,Date,int,byte[],int,int,byte[]) is not applicable
      (argument mismatch; long cannot be converted to Date)
    constructor TSIGRecord.TSIGRecord(Name,int,long,Name,Instant,Duration,byte[],int,int,byte[]) is not applicable
      (argument mismatch; long cannot be converted to Instant)
src/test/java/org/xbill/DNS/TSIG_verify_Test.java:206: error: no suitable constructor found for TSIGRecord(Name,int,long,Name,long,int,byte[],Name,int,byte[])
        TSIGRecord futureTsig = new TSIGRecord(Name.fromString("example."), DClass.IN, 0L, TSIG.HMAC_SHA256, future, (int) TSIG.FUDGE.getSeconds(), new byte[0], Name.fromString("example."), Rcode.NOERROR, new byte[0]);
                                ^
    constructor TSIGRecord.TSIGRecord(Name,int,long,Name,Date,int,byte[],int,int,byte[]) is not applicable
      (argument mismatch; long cannot be converted to Date)
    constructor TSIGRecord.TSIGRecord(Name,int,long,Name,Instant,Duration,byte[],int,int,byte[]) is not applicable
      (argument mismatch; long cannot be converted to Instant)
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
4 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.TSIG': '\n * Transaction signature handling. This class generates and verifies TSIG records on messages, which\n * provide transaction security.\n *\n * @see TSIGRecord\n * @author Brian Wellington\n ', 'api document of method verify(Message, byte[], TSIGRecord)': "\n   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a\n   * TSIG is expected to be present, it is an error if one is not present. After calling this\n   * routine, Message.isVerified() may be called on this message.\n   *\n   * <p>Use {@link StreamVerifier} to validate multiple messages in a stream.\n   *\n   * @param m The message to verify\n   * @param messageBytes An array containing the message in unparsed form. This is necessary since\n   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with\n   *     the same name compression).\n   * @param requestTSIG If this message is a response, the TSIG from the request\n   * @return The result of the verification (as an Rcode)\n   * @see Rcode\n   ", 'return type': 'int', 'dependent classes': "class `org.xbill.DNS.Message`:\n\t\nclass `org.xbill.DNS.TSIGRecord`:\n\t\nclass `org.xbill.DNS.TSIG`:\n\trelated functions:\n\t\tmethod `public int verify(Message, byte[], TSIGRecord)` returns `int`, related with `org.xbill.DNS.TSIG.verify(Message, byte[], TSIGRecord)`, api document: \n   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a\n   * TSIG is expected to be present, it is an error if one is not present. After calling this\n   * routine, Message.isVerified() may be called on this message.\n   *\n   * <p>Use {@link StreamVerifier} to validate multiple messages in a stream.\n   *\n   * @param m The message to verify\n   * @param messageBytes An array containing the message in unparsed form. This is necessary since\n   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with\n   *     the same name compression).\n   * @param requestTSIG If this message is a response, the TSIG from the request\n   * @return The result of the verification (as an Rcode)\n   * @see Rcode\n   ;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.TSIG.verify(Message, byte[], TSIGRecord)`, api document:  Converts a numeric extended flag into a String ;"}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


