


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
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
    static void setupBeforeAll()throws Exception {
        // Freeze time so that TSIG timestamps are repeatable.
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"));
        // The key material is the base‑64 encoding of the ASCII string “testkey”.
        // “testkey” → dGVzdGtleQ==
        defaultKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "HmacSHA256"));
        // inject the deterministic clock via reflection
        injectClock(defaultKey, fixedClock);
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
     * Helper that injects a deterministic clock into a {@link TSIG} instance via reflection.
     */
    private static void injectClock(TSIG tsig, Clock clock) throws Exception {
        Field clockField = TSIG.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(tsig, clock);
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
        TSIG otherKey = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(base64.fromString("YW5vdGhlcktleQ=="), "HmacSHA256"));
        // keep timestamps identical
        injectClock(otherKey, fixedClock);
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
        Name.fromString("example."), // dclass
        DClass.IN, // ttl
        0L, // algorithm (unknown)
        unknownAlg, // timeSigned
        Instant.now(fixedClock), // fudge
        TSIG.FUDGE, // mac (empty)
        new byte[0], // original ID
        0, // error
        Rcode.NOERROR, // other data
        new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> defaultKey.verify(responseMessage, responseWire, badAlgRecord), "TSIGRecord with unknown algorithm should trigger IllegalArgumentException");
    }

    /**
     * Timestamp farther than {@code TSIG.FUDGE} into the future must cause SecurityException.
     */
    @Test
    void verify_timestampTooFarAhead_throwsSecurityException() throws Exception {
        // Create a request TSIG whose timeSigned is 10 minutes in the future.
        long futureEpoch = Instant.now(fixedClock).plus(Duration.ofMinutes(10)).getEpochSecond();
        TSIGRecord futureTsig = new TSIGRecord(// name
        Name.fromString("example."), // dclass
        DClass.IN, // ttl
        0L, // algorithm
        TSIG.HMAC_SHA256, // timeSigned (future)
        Instant.ofEpochSecond(futureEpoch), // fudge
        TSIG.FUDGE, // mac
        new byte[0], // original ID
        0, // error
        Rcode.NOERROR, // other data
        new byte[0]);
        assertThrows(SecurityException.class, () -> defaultKey.verify(responseMessage, responseWire, futureTsig), "TSIG timestamp beyond fudge window should cause SecurityException");
    }

    /**
     * Malformed secret key (wrong algorithm) must cause GeneralSecurityException during MAC init.
     */
    @Test
    void verify_malformedKey_throwsGeneralSecurityException() throws Exception {
        // Construct a TSIG instance whose secret key is created with an incompatible algorithm.
        // The key bytes are the same, but the algorithm name is "RSA" which is not a HMAC.
        TSIG malformedKeyTsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("example."), new SecretKeySpec(base64.fromString("dGVzdGtleQ=="), "RSA"));
        // Inject the deterministic clock.
        injectClock(malformedKeyTsig, fixedClock);
        // The responseMessage already contains a valid TSIG generated with the *correct* key.
        // Verification with the malformed key should fail during Mac.init().
        assertThrows(GeneralSecurityException.class, () -> malformedKeyTsig.verify(responseMessage, responseWire, validRequestTsig), "Malformed secret key should trigger GeneralSecurityException");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/regen-policies/results/policy/pools/ktester/34/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended

╷
├─ JUnit Jupiter ✔
│  └─ TSIG_verify_Test ✔
│     ├─ verifyResponseWithoutTsig() ✘ Verification of a response lacking TSIG should return BADKEY ==> expected: <17> but was: <1>
│     ├─ verifyValidSignature() ✘ Bad arguments
│     ├─ verify_timestampTooFarAhead_throwsSecurityException() ✘ TSIG timestamp beyond fudge window should cause SecurityException ==> Unexpected exception type thrown, expected: <java.lang.SecurityException> but was: <java.lang.IllegalArgumentException>
│     ├─ verifyWithNullRequestTsig() ✘ Bad arguments
│     ├─ verifyWithWrongKeyFails() ✘ Bad arguments
│     ├─ verify_emptyMessageBytes_throwsIllegalArgumentException() ✘ Empty messageBytes array should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ verify_malformedKey_throwsGeneralSecurityException() ✘ Malformed secret key should trigger GeneralSecurityException ==> Unexpected exception type thrown, expected: <java.security.GeneralSecurityException> but was: <java.lang.IllegalArgumentException>
│     ├─ verify_unsupportedAlgorithm_throwsIllegalArgumentException() ✔
│     └─ NullArgumentTests ✔
│        ├─ verify_nullRequestTsig_throwsNPE() ✘ requestTSIG argument is null – should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
│        ├─ verify_nullMessage_throwsNPE() ✔
│        └─ verify_nullMessageBytes_throwsNPE() ✘ messageBytes argument is null – should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (9):
  JUnit Jupiter:TSIG_verify_Test:verifyResponseWithoutTsig()
    MethodSource [className = 'org.xbill.DNS.TSIG_verify_Test', methodName = 'verifyResponseWithoutTsig', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Verification of a response lacking TSIG should return BADKEY ==> expected: <17> but was: <1>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:563)
       org.xbill.DNS.TSIG_verify_Test.verifyResponseWithoutTsig(TSIG_verify_Test.java:177)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:TSIG_verify_Test:verifyValidSignature()
    MethodSource [className = 'org.xbill.DNS.TSIG_verify_Test', methodName = 'verifyValidSignature', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Bad arguments
       java.base/javax.crypto.Mac.update(Mac.java:528)
       org.xbill.DNS.TSIG.verify(TSIG.java:656)
       org.xbill.DNS.TSIG.verify(TSIG.java:598)
       org.xbill.DNS.TSIG.verify(TSIG.java:575)
       org.xbill.DNS.TSIG_verify_Test.verifyValidSignature(TSIG_verify_Test.java:131)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:TSIG_verify_Test:verify_timestampTooFarAhead_throwsSecurityException()
    MethodSource [className = 'org.xbill.DNS.TSIG_verify_Test', methodName = 'verify_timestampTooFarAhead_throwsSecurityException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: TSIG timestamp beyond fudge window should cause SecurityException ==> Unexpected exception type thrown, expected: <java.lang.SecurityException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.TSIG_verify_Test.verify_timestampTooFarAhead_throwsSecurityException(TSIG_verify_Test.java:228)
       [...]
     Caused by: java.lang.IllegalArgumentException: Bad arguments
       java.base/javax.crypto.Mac.update(Mac.java:528)
       org.xbill.DNS.TSIG.verify(TSIG.java:656)
       org.xbill.DNS.TSIG.verify(TSIG.java:598)
       org.xbill.DNS.TSIG.verify(TSIG.java:575)
       org.xbill.DNS.TSIG_verify_Test.lambda$verify_timestampTooFarAhead_throwsSecurityException$2(TSIG_verify_Test.java:228)
       [...]
  JUnit Jupiter:TSIG_verify_Test:verifyWithNullRequestTsig()
    MethodSource [className = 'org.xbill.DNS.TSIG_verify_Test', methodName = 'verifyWithNullRequestTsig', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Bad arguments
       java.base/javax.crypto.Mac.update(Mac.java:528)
       org.xbill.DNS.TSIG.verify(TSIG.java:656)
       org.xbill.DNS.TSIG.verify(TSIG.java:598)
       org.xbill.DNS.TSIG.verify(TSIG.java:575)
       org.xbill.DNS.TSIG_verify_Test.verifyWithNullRequestTsig(TSIG_verify_Test.java:159)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:TSIG_verify_Test:verifyWithWrongKeyFails()
    MethodSource [className = 'org.xbill.DNS.TSIG_verify_Test', methodName = 'verifyWithWrongKeyFails', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Bad arguments
       java.base/javax.crypto.Mac.update(Mac.java:528)
       org.xbill.DNS.TSIG.verify(TSIG.java:656)
       org.xbill.DNS.TSIG.verify(TSIG.java:598)
       org.xbill.DNS.TSIG.verify(TSIG.java:575)
       org.xbill.DNS.TSIG_verify_Test.verifyWithWrongKeyFails(TSIG_verify_Test.java:148)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:TSIG_verify_Test:verify_emptyMessageBytes_throwsIllegalArgumentException()
    MethodSource [className = 'org.xbill.DNS.TSIG_verify_Test', methodName = 'verify_emptyMessageBytes_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Empty messageBytes array should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.TSIG_verify_Test.verify_emptyMessageBytes_throwsIllegalArgumentException(TSIG_verify_Test.java:186)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:TSIG_verify_Test:verify_malformedKey_throwsGeneralSecurityException()
    MethodSource [className = 'org.xbill.DNS.TSIG_verify_Test', methodName = 'verify_malformedKey_throwsGeneralSecurityException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Malformed secret key should trigger GeneralSecurityException ==> Unexpected exception type thrown, expected: <java.security.GeneralSecurityException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.TSIG_verify_Test.verify_malformedKey_throwsGeneralSecurityException(TSIG_verify_Test.java:243)
       [...]
     Caused by: java.lang.IllegalArgumentException: Bad arguments
       java.base/javax.crypto.Mac.update(Mac.java:528)
       org.xbill.DNS.TSIG.verify(TSIG.java:656)
       org.xbill.DNS.TSIG.verify(TSIG.java:598)
       org.xbill.DNS.TSIG.verify(TSIG.java:575)
       org.xbill.DNS.TSIG_verify_Test.lambda$verify_malformedKey_throwsGeneralSecurityException$3(TSIG_verify_Test.java:243)
       [...]
  JUnit Jupiter:TSIG_verify_Test:NullArgumentTests:verify_nullRequestTsig_throwsNPE()
    MethodSource [className = 'org.xbill.DNS.TSIG_verify_Test$NullArgumentTests', methodName = 'verify_nullRequestTsig_throwsNPE', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: requestTSIG argument is null – should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.TSIG_verify_Test$NullArgumentTests.verify_nullRequestTsig_throwsNPE(TSIG_verify_Test.java:75)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:TSIG_verify_Test:NullArgumentTests:verify_nullMessageBytes_throwsNPE()
    MethodSource [className = 'org.xbill.DNS.TSIG_verify_Test$NullArgumentTests', methodName = 'verify_nullMessageBytes_throwsNPE', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: messageBytes argument is null – should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.TSIG_verify_Test$NullArgumentTests.verify_nullMessageBytes_throwsNPE(TSIG_verify_Test.java:69)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 426 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        11 tests found           ]
[         0 tests skipped         ]
[        11 tests started         ]
[         0 tests aborted         ]
[         2 tests successful      ]
[         9 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class org.xbill.DNS.TSIG: 

 * Transaction signature handling. This class generates and verifies TSIG records on messages, which
 * provide transaction security.
 *
 * @see TSIGRecord
 * @author Brian Wellington
 

api document of method verify(Message, byte[], TSIGRecord): 

   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a
   * TSIG is expected to be present, it is an error if one is not present. After calling this
   * routine, Message.isVerified() may be called on this message.
   *
   * <p>Use {@link StreamVerifier} to validate multiple messages in a stream.
   *
   * @param m The message to verify
   * @param messageBytes An array containing the message in unparsed form. This is necessary since
   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with
   *     the same name compression).
   * @param requestTSIG If this message is a response, the TSIG from the request
   * @return The result of the verification (as an Rcode)
   * @see Rcode
   

return type: 
int

dependent classes: 
class `org.xbill.DNS.Message`:
	
class `org.xbill.DNS.TSIGRecord`:
	
class `org.xbill.DNS.TSIG`:
	related functions:
		method `public int verify(Message, byte[], TSIGRecord)` returns `int`, related with `org.xbill.DNS.TSIG.verify(Message, byte[], TSIGRecord)`, api document: 
   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a
   * TSIG is expected to be present, it is an error if one is not present. After calling this
   * routine, Message.isVerified() may be called on this message.
   *
   * <p>Use {@link StreamVerifier} to validate multiple messages in a stream.
   *
   * @param m The message to verify
   * @param messageBytes An array containing the message in unparsed form. This is necessary since
   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with
   *     the same name compression).
   * @param requestTSIG If this message is a response, the TSIG from the request
   * @return The result of the verification (as an Rcode)
   * @see Rcode
   ;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.TSIG.verify(Message, byte[], TSIGRecord)`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
