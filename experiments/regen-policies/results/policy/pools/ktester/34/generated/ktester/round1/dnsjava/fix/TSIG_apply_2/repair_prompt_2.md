


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// Allows non‑static @MethodSource methods
@TestInstance(Lifecycle.PER_CLASS)
class TSIG_apply_2_Test {

    /**
     * A TSIG key used for all tests.
     */
    private static final TSIG TSIG_KEY = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

    /**
     * The TSIGRecord generated from a signed query – reused as the “old” record.
     */
    private TSIGRecord queryTsigRecord;

    /**
     * A simple query message that will be signed to produce {@link #queryTsigRecord}.
     */
    private Message signedQuery;

    /**
     * A generic TSIG key (HMAC‑SHA256) used for most tests.
     */
    private static final TSIG DEFAULT_TSIG = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // 1️⃣ Build a signed query – this also creates a TSIGRecord we can reuse.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        signedQuery = Message.newQuery(question);
        // attach TSIG key
        signedQuery.setTSIG(DEFAULT_TSIG);
        // forces signing
        byte[] wire = signedQuery.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        // assign to field
        queryTsigRecord = parsed.getTSIG();
        assertNotNull(queryTsigRecord, "The query must contain a TSIGRecord after signing");
    }

    // non‑static because the test class uses PER_CLASS lifecycle
    private Stream<Arguments> nullArgumentProvider() {
        return Stream.of(
                Arguments.of(null, null, false),
                Arguments.of(null, queryTsigRecord, true)
        );
    }

    /* --------------------------------------------------------------------- */
    private static byte[] computeMac(String algorithm, byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        mac.update(data);
        return mac.doFinal();
    }

    /**
     * Verify that {@code apply(message, old, true)} adds a TSIG record,
     * marks the message as signed and leaves a non‑empty signature.
     */
    @Test
    void testApplyAddsFullSignature() throws IOException {
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        DEFAULT_TSIG.apply(response, queryTsigRecord, true);
        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response must contain a TSIGRecord after apply");
        assertNotEquals(0, respTsig.getSignature().length, "Signature bytes should be present");
        // Verify the MAC using the same key – a successful verification proves that
        // the full‑signature MAC was correctly calculated.
        int rcode = DEFAULT_TSIG.verify(response, response.toWire(Message.MAXLENGTH), queryTsigRecord, true);
        assertEquals(Rcode.NOERROR, rcode, "Verification of full signature must succeed");
    }

    /**
     * Verify that {@code apply(message, old, false)} also adds a TSIG record.
     */
    @Test
    void testApplyAddsPartialSignature() throws IOException {
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        DEFAULT_TSIG.apply(response, queryTsigRecord, false);
        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response must contain a TSIGRecord after apply");
        assertNotEquals(0, respTsig.getSignature().length, "Signature bytes should be present even for a partial signature");
        int rcode = DEFAULT_TSIG.verify(response, response.toWire(Message.MAXLENGTH), queryTsigRecord, false);
        assertEquals(Rcode.NOERROR, rcode, "Verification of partial signature must succeed");
    }

    /**
     * Verify that calling {@code apply} with a {@code null} old record does not
     * throw an exception and still produces a signed message.
     */
    @Test
    void testApplyWithNullOldRecord() throws IOException {
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        // The public API also offers apply(message, old) which internally calls
        // the 4‑argument version with fullSignature = false.
        TSIG_KEY.apply(response, (TSIGRecord) null, false);
        assertTrue(response.isSigned(), "Response should be signed even when old TSIG is null");
        assertNotNull(response.getTSIG(), "A TSIGRecord must be generated");
    }

    @ParameterizedTest(name = "[{index}] m={0}, old={1}, full={2}")
    @MethodSource("nullArgumentProvider")
    void testApplyNullMessageThrows(Message m, TSIGRecord old, boolean fullSignature) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DEFAULT_TSIG.apply(m, old, fullSignature),
                "apply should reject a null Message");
        assertTrue(ex.getMessage().toLowerCase().contains("message"));
    }

    @Test
    void testApplyUnknownAlgorithmThrows() throws IOException {
        // Build a TSIG key with an algorithm name that the implementation does not know.
        TSIG unknownAlgKey = new TSIG("unknown-alg.", "example.", "secret");
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> unknownAlgKey.apply(response, queryTsigRecord, false));
        assertTrue(ex.getMessage().toLowerCase().contains("algorithm"));
    }

    @Test
    void testApplyZeroLengthSecret() throws Exception {
        // Empty secret string translates to a zero‑length byte array.
        TSIG zeroSecretKey = new TSIG(TSIG.HMAC_SHA256, "example.", "");
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        // No exception should be thrown.
        zeroSecretKey.apply(response, queryTsigRecord, true);
        assertTrue(response.isSigned(), "Message must be signed even with zero‑length secret");
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig);
        assertNotEquals(0, tsig.getSignature().length, "Signature must be non‑empty");
        // Verify manually using an empty key.
        byte[] wire = response.toWire(Message.MAXLENGTH);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(new byte[0], "HmacSHA256"));
        mac.update(wire);
        byte[] expected = mac.doFinal();
        assertArrayEquals(expected, tsig.getSignature(), "Signature generated with empty secret must match manual HMAC");
    }

    @Test
    void testApplyUnsupportedMacAlgorithmThrows() throws IOException {
        // The name follows the “HMAC‑…” pattern but the JCE provider does not support it.
        TSIG unsupportedKey = new TSIG("HMAC-SHA999", "example.", "secret");
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> unsupportedKey.apply(response, queryTsigRecord, false));
        assertTrue(ex.getCause() instanceof GeneralSecurityException,
                "Root cause should be a GeneralSecurityException");
    }

    @Test
    void testApplyReSignWithDifferentKeyName() throws IOException {
        // Old TSIG (key name "old.example.") – sign a query with this key.
        TSIG oldKey = new TSIG(TSIG.HMAC_SHA256, "old.example.", "sharedSecret");
        Message query = Message.newQuery(Record.newRecord(Name.fromString("test.example."), Type.A, DClass.IN));
        query.setTSIG(oldKey);
        TSIGRecord oldRecord = new Message(query.toWire(Message.MAXLENGTH)).getTSIG();
        assertEquals(Name.fromString("old.example."), oldRecord.getName());
        // New key (different key name, same secret) will be used to sign the response.
        TSIG newKey = new TSIG(TSIG.HMAC_SHA256, "new.example.", "sharedSecret");
        Message response = new Message(query.getHeader().getID());
        response.addRecord(query.getQuestion(), Section.QUESTION);
        newKey.apply(response, oldRecord, true);
        TSIGRecord respRecord = response.getTSIG();
        assertNotNull(respRecord);
        assertEquals(Name.fromString("new.example."), respRecord.getName(),
                "Key name of the generated TSIGRecord must match the new TSIG key");
        assertEquals(oldRecord.getAlgorithm(), respRecord.getAlgorithm(),
                "Algorithm must be preserved from the supplied old record");
        // Verify that the MAC is valid when using the new key.
        int rcode = newKey.verify(response, response.toWire(Message.MAXLENGTH), oldRecord, true);
        assertEquals(Rcode.NOERROR, rcode, "Verification with the new key must succeed");
    }

    @Test
    void testApplyWithExistingTsigAndNullOld() throws IOException {
        // Attach a TSIG to the message using the default key.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        // this creates a TSIGRecord internally
        response.setTSIG(DEFAULT_TSIG);
        TSIGRecord existing = response.getTSIG();
        assertNotNull(existing);
        // Now call apply with old == null – the method should re‑sign using the
        // attached TSIG (i.e., the same key name).
        DEFAULT_TSIG.apply(response, (TSIGRecord) null, true);
        TSIGRecord newRecord = response.getTSIG();
        assertNotNull(newRecord);
        assertNotEquals(existing.getSignature(), newRecord.getSignature(), "Signature must be refreshed");
        assertEquals(existing.getName(), newRecord.getName(), "Key name should stay the same when old is null");
    }

    @Test
    void testFullSignatureVerificationAgainstRequest() throws IOException {
        // Build a response that mirrors the signed query.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        // Apply with fullSignature = true.
        DEFAULT_TSIG.apply(response, queryTsigRecord, true);
        // Verify using the original request TSIG (queryTsigRecord) as the “old”.
        int rcode = DEFAULT_TSIG.verify(response, response.toWire(Message.MAXLENGTH), queryTsigRecord, true);
        assertEquals(Rcode.NOERROR, rcode, "Full‑signature verification should succeed with the original request TSIG");
    }

    @Test
    void testFudgeTimeWindow() throws IOException {
        // Create a message whose internal clock is artificially old.
        Message response = new Message(signedQuery.getHeader().getID());
        response.addRecord(signedQuery.getQuestion(), Section.QUESTION);
        // Force the system clock to be far in the past by mocking Clock is not
        // possible without changing TSIG source. Instead we verify that the
        // generated time‑signed is not older than now + FUDGE.
        DEFAULT_TSIG.apply(response, queryTsigRecord, false);
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig);
        long now = Instant.now().getEpochSecond();
        long signed = tsig.getTimeSigned().getEpochSecond();
        assertTrue(signed <= now + TSIG.FUDGE.getSeconds(),
                "time‑signed must be within the fudge window");
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
│  └─ TSIG_apply_2_Test ✔
│     ├─ testFullSignatureVerificationAgainstRequest() ✘ Bad arguments
│     ├─ testApplyUnsupportedMacAlgorithmThrows() ✘ Unknown algorithm: HMAC-SHA999
│     ├─ testFudgeTimeWindow() ✔
│     ├─ testApplyZeroLengthSecret() ✘ Empty key
│     ├─ testApplyWithNullOldRecord() ✔
│     ├─ testApplyWithExistingTsigAndNullOld() ✘ expected: not <null>
│     ├─ testApplyAddsFullSignature() ✘ Bad arguments
│     ├─ testApplyAddsPartialSignature() ✘ Bad arguments
│     ├─ testApplyReSignWithDifferentKeyName() ✘ Bad arguments
│     ├─ testApplyNullMessageThrows(Message, TSIGRecord, boolean) ✔
│     │  ├─ [1] m=null, old=null, full=false ✘ apply should reject a null Message ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     │  └─ [2] m=null, old=example.  0 ANY TSIG hmac-sha256. 1793414474 300 32 FDcGigsnCdKl... ✘ apply should reject a null Message ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     └─ testApplyUnknownAlgorithmThrows() ✘ Unknown algorithm: unknown-alg.
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (10):
  JUnit Jupiter:TSIG_apply_2_Test:testFullSignatureVerificationAgainstRequest()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testFullSignatureVerificationAgainstRequest', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Bad arguments
       java.base/javax.crypto.Mac.update(Mac.java:528)
       org.xbill.DNS.TSIG.verify(TSIG.java:656)
       org.xbill.DNS.TSIG.verify(TSIG.java:598)
       org.xbill.DNS.TSIG_apply_2_Test.testFullSignatureVerificationAgainstRequest(TSIG_apply_2_Test.java:233)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:TSIG_apply_2_Test:testApplyUnsupportedMacAlgorithmThrows()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testApplyUnsupportedMacAlgorithmThrows', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: HMAC-SHA999
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.TSIG_apply_2_Test.testApplyUnsupportedMacAlgorithmThrows(TSIG_apply_2_Test.java:174)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:TSIG_apply_2_Test:testApplyZeroLengthSecret()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testApplyZeroLengthSecret', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Empty key
       java.base/javax.crypto.spec.SecretKeySpec.<init>(SecretKeySpec.java:111)
       org.xbill.DNS.TSIG.<init>(TSIG.java:279)
       org.xbill.DNS.TSIG_apply_2_Test.testApplyZeroLengthSecret(TSIG_apply_2_Test.java:153)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:TSIG_apply_2_Test:testApplyWithExistingTsigAndNullOld()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testApplyWithExistingTsigAndNullOld', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: not <null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertNotNull.failNull(AssertNotNull.java:49)
       org.junit.jupiter.api.AssertNotNull.assertNotNull(AssertNotNull.java:35)
       org.junit.jupiter.api.AssertNotNull.assertNotNull(AssertNotNull.java:30)
       org.junit.jupiter.api.Assertions.assertNotNull(Assertions.java:304)
       org.xbill.DNS.TSIG_apply_2_Test.testApplyWithExistingTsigAndNullOld(TSIG_apply_2_Test.java:215)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:TSIG_apply_2_Test:testApplyAddsFullSignature()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testApplyAddsFullSignature', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Bad arguments
       java.base/javax.crypto.Mac.update(Mac.java:528)
       org.xbill.DNS.TSIG.verify(TSIG.java:656)
       org.xbill.DNS.TSIG.verify(TSIG.java:598)
       org.xbill.DNS.TSIG_apply_2_Test.testApplyAddsFullSignature(TSIG_apply_2_Test.java:95)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:TSIG_apply_2_Test:testApplyAddsPartialSignature()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testApplyAddsPartialSignature', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Bad arguments
       java.base/javax.crypto.Mac.update(Mac.java:528)
       org.xbill.DNS.TSIG.verify(TSIG.java:656)
       org.xbill.DNS.TSIG.verify(TSIG.java:598)
       org.xbill.DNS.TSIG_apply_2_Test.testApplyAddsPartialSignature(TSIG_apply_2_Test.java:111)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:TSIG_apply_2_Test:testApplyReSignWithDifferentKeyName()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testApplyReSignWithDifferentKeyName', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Bad arguments
       java.base/javax.crypto.Mac.update(Mac.java:528)
       org.xbill.DNS.TSIG.verify(TSIG.java:656)
       org.xbill.DNS.TSIG.verify(TSIG.java:598)
       org.xbill.DNS.TSIG_apply_2_Test.testApplyReSignWithDifferentKeyName(TSIG_apply_2_Test.java:203)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:TSIG_apply_2_Test:testApplyNullMessageThrows(Message, TSIGRecord, boolean):[1] m=null, old=null, full=false
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testApplyNullMessageThrows', methodParameterTypes = 'org.xbill.DNS.Message, org.xbill.DNS.TSIGRecord, boolean']
    => org.opentest4j.AssertionFailedError: apply should reject a null Message ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.TSIG_apply_2_Test.testApplyNullMessageThrows(TSIG_apply_2_Test.java:133)
       [...]
     Caused by: java.lang.NullPointerException: Cannot invoke "org.xbill.DNS.Message.toWire()" because "m" is null
       org.xbill.DNS.TSIG.apply(TSIG.java:517)
       org.xbill.DNS.TSIG.apply(TSIG.java:502)
       org.xbill.DNS.TSIG_apply_2_Test.lambda$testApplyNullMessageThrows$0(TSIG_apply_2_Test.java:134)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
  JUnit Jupiter:TSIG_apply_2_Test:testApplyNullMessageThrows(Message, TSIGRecord, boolean):[2] m=null, old=example.		0	ANY	TSIG	hmac-sha256. 1793414474 300 32 FDcGigsnCdKleS5Pnijv1AK3unLNVJhvIbiW5WtUcMg= NOERROR 0, full=true
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testApplyNullMessageThrows', methodParameterTypes = 'org.xbill.DNS.Message, org.xbill.DNS.TSIGRecord, boolean']
    => org.opentest4j.AssertionFailedError: apply should reject a null Message ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.TSIG_apply_2_Test.testApplyNullMessageThrows(TSIG_apply_2_Test.java:133)
       [...]
     Caused by: java.lang.NullPointerException: Cannot invoke "org.xbill.DNS.Message.toWire()" because "m" is null
       org.xbill.DNS.TSIG.apply(TSIG.java:517)
       org.xbill.DNS.TSIG.apply(TSIG.java:502)
       org.xbill.DNS.TSIG_apply_2_Test.lambda$testApplyNullMessageThrows$0(TSIG_apply_2_Test.java:134)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
  JUnit Jupiter:TSIG_apply_2_Test:testApplyUnknownAlgorithmThrows()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_2_Test', methodName = 'testApplyUnknownAlgorithmThrows', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Unknown algorithm: unknown-alg.
       org.xbill.DNS.TSIG.lambda$algorithmToName$2(TSIG.java:109)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       org.xbill.DNS.TSIG.lambda$algorithmToName$3(TSIG.java:109)
       java.base/java.util.Optional.orElseGet(Optional.java:364)
       org.xbill.DNS.TSIG.algorithmToName(TSIG.java:99)
       org.xbill.DNS.TSIG.<init>(TSIG.java:303)
       org.xbill.DNS.TSIG_apply_2_Test.testApplyUnknownAlgorithmThrows(TSIG_apply_2_Test.java:142)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]

Test run finished after 451 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        12 tests found           ]
[         0 tests skipped         ]
[        12 tests started         ]
[         0 tests aborted         ]
[         2 tests successful      ]
[        10 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class org.xbill.DNS.TSIG: 

 * Transaction signature handling. This class generates and verifies TSIG records on messages, which
 * provide transaction security.
 *
 * @see TSIGRecord
 * @author Brian Wellington
 

api document of method apply(Message, TSIGRecord, boolean): 

   * Generates a TSIG record with a specific error for a message and adds it to the message.
   *
   * @param m The message
   * @param old If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and
   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for
   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).
   * @since 3.2
   

dependent classes: 
class `org.xbill.DNS.Message`:
	related functions:
		method `public void setTSIG(TSIG)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord, boolean)`, api document: 
   * Sets the TSIG key to sign a message.
   *
   * @param key The TSIG key.
   * @since 3.5.1
   ;
class `org.xbill.DNS.TSIGRecord`:
	
class `org.xbill.DNS.Rcode`:
	dependent fields:
		int org.xbill.DNS.Rcode.NOERROR;
	
class `org.xbill.DNS.TSIG`:
	related functions:
		method `public TSIGRecord generate(Message, byte[], int, TSIGRecord, boolean)` returns `org.xbill.DNS.TSIGRecord`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord, boolean)`, api document: 
   * Generates a TSIG record with a specific error for a message that has been rendered.
   *
   * @param m The message
   * @param b The rendered message
   * @param error The error
   * @param old If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this {@link TSIGRecord} is the to be added to the first of
   *     many messages in a TCP connection and all TSIG variables (rfc2845, 3.4.2.) should be
   *     included in the signature. {@code false} for subsequent messages with reduced TSIG
   *     variables set (rfc2845, 4.4.).
   * @return The TSIG record to be added to the message
   * @since 3.2
   ;
		method `public void apply(Message, int, TSIGRecord)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord, boolean)`, api document: 
   * Generates a TSIG record with a specific error for a message and adds it to the message.
   *
   * @param m The message
   * @param error The error
   * @param old If this message is a response, the TSIG from the request
   ;
class `org.xbill.DNS.SimpleResolver`:
	related functions:
		method `public CompletionStage<Message> sendAsync(Message, Executor)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.Message>`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord, boolean)`, api document: 
   * Asynchronously sends a message to a single server.
   *
   * @param query The query to send
   * @param executor The service to use for async operations.
   * @return A future that completes when the response has arrived.
   ;
class `org.xbill.DNS.dnssec.ValidatingResolver`:
	related functions:
		method `public CompletionStage<Message> sendAsync(Message, Executor)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.Message>`, related with `org.xbill.DNS.TSIG.apply(Message, TSIGRecord, boolean)`, api document: 
   * Asynchronously sends a message and validates the response with DNSSEC before returning it.
   *
   * @param query The query to send.
   * @param executor The service to use for async operations.
   * @return A future that completes when the query is finished.
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
