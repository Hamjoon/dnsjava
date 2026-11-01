


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
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class TSIG_apply_4_Test {

    /**
     * Fixed clock so that the TSIG timestamp does not vary between test runs.
     */
    private static Clock fixedClock;

    /**
     * The TSIG instance under test.
     */
    private static TSIG tsig;

    /**
     * Message used in each test case.
     */
    private Message message;

    @BeforeAll
    static void setupBeforeAll()throws Exception {
        // a deterministic instant – 2023‑01‑01T00:00:00Z
        fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"));
        // algorithm = HMAC‑SHA256, name = "example.", key = "12345678"
        Name algorithm = TSIG.HMAC_SHA256;
        Name name = Name.fromString("example.");
        SecretKey key = new SecretKeySpec("12345678".getBytes(), "HmacSHA256");
        tsig = new TSIG(algorithm, name, key, fixedClock);
    }

    @BeforeEach
    void setupBeforeEach()throws Exception {
        // a simple query message – the content of the question is irrelevant for the TSIG logic
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        message = Message.newQuery(question);
    }

    /**
     * The standard case: no previous TSIG, NOERROR, full signature.
     */
    @Test
    void applyAddsTSIGRecordAndMarksMessageSigned() {
        // pre‑condition – the message must not already contain a TSIG record
        assertNull(message.getTSIG(), "Message should start without a TSIG record");
        // invoke the focal method
        tsig.apply(message, Rcode.NOERROR, /* old */
        null, /* fullSignature */
        true);
        // post‑condition – a TSIG record must now be present
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "TSIG record should have been added to the message");
        // the message must be marked as signed
        assertTrue(message.isSigned(), "Message should be marked as signed after apply()");
        // the TSIG record must be the last record in the ADDITION section
        Record[] addition = message.getSectionArray(Section.ADDITIONAL);
        assertTrue(addition.length > 0, "Addition section must contain at least one record");
        assertEquals(tsigRecord, addition[addition.length - 1], "TSIG record should be the last record in the ADDITION section");
    }

    /**
     * Apply with a non‑zero error code and a partial (non‑full) signature.
     */
    @Test
    void applyWithErrorAndPartialSignatureAddsTSIGRecord() {
        // invoke the focal method with an error code and a partial signature flag
        tsig.apply(message, Rcode.FORMERR, /* old */
        null, /* fullSignature */
        false);
        // a TSIG record must still be added
        assertNotNull(message.getTSIG(), "TSIG record should be added even when error != NOERROR");
        // the message must still be marked as signed
        assertTrue(message.isSigned(), "Message should be marked as signed regardless of error code");
    }

    /**
     * Apply when a previous TSIGRecord is supplied (e.g., for a response).
     */
    @Test
    void applyWithPreviousTSIGRecord()throws Exception {
        // create a dummy previous TSIGRecord (the constructor arguments are arbitrary but valid)
        TSIGRecord previous = new TSIGRecord(// name
        Name.fromString("example."), // dclass
        DClass.IN, // ttl
        0L, // algorithm
        TSIG.HMAC_SHA256, // timeSigned
        Instant.now(fixedClock), // fudge
        TSIG.FUDGE, // mac (signature)
        new byte[0], // original ID
        message.getHeader().getID(), // error
        Rcode.NOERROR, // other data
        new byte[0]);
        // invoke the focal method with the previous TSIGRecord
        tsig.apply(message, Rcode.NOERROR, previous, true);
        // verify that a new TSIG record replaces the old one
        TSIGRecord resulting = message.getTSIG();
        assertNotNull(resulting, "Resulting TSIG record must be present");
        assertTrue(message.isSigned(), "Message should be signed after applying with a previous TSIG");
        assertNotEquals(previous, resulting, "A new TSIGRecord should be generated, not reuse the old one");
    }

    /**
     * Parameterized test covering various error codes and fullSignature flags.
     */
    @ParameterizedTest(name = "apply(error={0}, fullSignature={1})")
    @CsvSource({ // happy path, non‑full
    // happy path, full
    "0, false", // non‑zero error
    "0, true", // negative error (edge case)
    "9, false", "-1, false" })
    void applyVariousErrorAndSignature(int error, boolean fullSignature) {
        tsig.apply(message, error, null, fullSignature);
        TSIGRecord tsigRecord = message.getTSIG();
        assertNotNull(tsigRecord, "TSIG record must be present");
        // Verify that the error field inside the generated TSIGRecord matches the supplied value.
        // The TSIGRecord class does not expose a public getter for the error, so we use reflection.
        try {
            Field errorField = TSIGRecord.class.getDeclaredField("error");
            errorField.setAccessible(true);
            int actualError = (int) errorField.get(tsigRecord);
            assertEquals(error, actualError, "The TSIGRecord error field should equal the error argument passed to apply()");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to access TSIGRecord.error field via reflection", e);
        }
        assertTrue(message.isSigned(), "Message should be marked as signed");
    }

    /**
     * Passing a null Message should raise NullPointerException.
     */
    @Test
    void applyWithNullMessageThrowsNPE() {
        assertThrows(NullPointerException.class, () -> tsig.apply(null, Rcode.NOERROR, null, false), "Calling apply with a null Message should throw NullPointerException");
    }

    /**
     * Combination of a previous TSIG record and full‑signature flag.
     */
    @Test
    void applyWithOldRecordAndFullSignature()throws Exception {
        TSIGRecord old = new TSIGRecord(Name.fromString("example.com."), DClass.IN, 300L, TSIG.HMAC_SHA1, Instant.now(fixedClock), TSIG.FUDGE, new byte[0], message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        tsig.apply(message, Rcode.NOERROR, old, true);
        TSIGRecord result = message.getTSIG();
        assertNotNull(result, "A TSIG record must be added when old record and fullSignature are supplied");
        assertTrue(message.isSigned(), "Message should be signed");
    }

    /**
     * Exception scenario: old TSIGRecord has a null algorithm name → NPE.
     */
    @Test
    void applyOldRecordWithNullAlgorithmThrowsNPE()throws Exception {
        TSIGRecord malformed = new TSIGRecord(Name.fromString("example.com."), DClass.IN, 300L, // null algorithm name
        null, Instant.now(fixedClock), TSIG.FUDGE, new byte[0], message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        assertThrows(NullPointerException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "A TSIGRecord with a null algorithm should cause NullPointerException");
    }

    /**
     * Exception scenario: unsupported algorithm name → IllegalArgumentException.
     */
    @Test
    void applyOldRecordWithUnsupportedAlgorithmThrowsIAE()throws Exception {
        Name unsupported = Name.fromString("unsupported-alg.");
        TSIGRecord malformed = new TSIGRecord(Name.fromString("example.com."), DClass.IN, 300L, unsupported, Instant.now(fixedClock), TSIG.FUDGE, new byte[0], message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "An unsupported algorithm should cause IllegalArgumentException");
    }

    /**
     * Exception scenario: old TSIGRecord contains a null signature (treated as null key) → NPE.
     */
    @Test
    void applyOldRecordWithNullSignatureThrowsNPE()throws Exception {
        TSIGRecord malformed = new TSIGRecord(Name.fromString("example.com."), DClass.IN, 300L, TSIG.HMAC_SHA256, Instant.now(fixedClock), TSIG.FUDGE, // null signature (simulates missing key)
        null, message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        assertThrows(NullPointerException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "A TSIGRecord with a null signature should cause NullPointerException");
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
│  └─ TSIG_apply_4_Test ✔
│     ├─ applyOldRecordWithNullSignatureThrowsNPE() ✔
│     ├─ applyWithErrorAndPartialSignatureAddsTSIGRecord() ✔
│     ├─ applyOldRecordWithNullAlgorithmThrowsNPE() ✘ Cannot invoke "org.xbill.DNS.Name.isAbsolute()" because "name" is null
│     ├─ applyAddsTSIGRecordAndMarksMessageSigned() ✔
│     ├─ applyWithOldRecordAndFullSignature() ✔
│     ├─ applyOldRecordWithUnsupportedAlgorithmThrowsIAE() ✘ An unsupported algorithm should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ applyWithNullMessageThrowsNPE() ✔
│     ├─ applyVariousErrorAndSignature(int, boolean) ✔
│     │  ├─ apply(error=0, fullSignature=false) ✔
│     │  ├─ apply(error=0, fullSignature=true) ✔
│     │  ├─ apply(error=9, fullSignature=false) ✔
│     │  └─ apply(error=-1, fullSignature=false) ✘ "error" -1 must be an unsigned 16 bit value
│     └─ applyWithPreviousTSIGRecord() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (3):
  JUnit Jupiter:TSIG_apply_4_Test:applyOldRecordWithNullAlgorithmThrowsNPE()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_4_Test', methodName = 'applyOldRecordWithNullAlgorithmThrowsNPE', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "org.xbill.DNS.Name.isAbsolute()" because "name" is null
       org.xbill.DNS.Record.checkName(Record.java:733)
       org.xbill.DNS.TSIGRecord.<init>(TSIGRecord.java:98)
       org.xbill.DNS.TSIG_apply_4_Test.applyOldRecordWithNullAlgorithmThrowsNPE(TSIG_apply_4_Test.java:172)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:TSIG_apply_4_Test:applyOldRecordWithUnsupportedAlgorithmThrowsIAE()
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_4_Test', methodName = 'applyOldRecordWithUnsupportedAlgorithmThrowsIAE', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: An unsupported algorithm should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       org.xbill.DNS.TSIG_apply_4_Test.applyOldRecordWithUnsupportedAlgorithmThrowsIAE(TSIG_apply_4_Test.java:183)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:TSIG_apply_4_Test:applyVariousErrorAndSignature(int, boolean):apply(error=-1, fullSignature=false)
    MethodSource [className = 'org.xbill.DNS.TSIG_apply_4_Test', methodName = 'applyVariousErrorAndSignature', methodParameterTypes = 'int, boolean']
    => java.lang.IllegalArgumentException: "error" -1 must be an unsigned 16 bit value
       org.xbill.DNS.Record.checkU16(Record.java:716)
       org.xbill.DNS.TSIGRecord.<init>(TSIGRecord.java:104)
       org.xbill.DNS.TSIG.generate(TSIG.java:456)
       org.xbill.DNS.TSIG.generate(TSIG.java:378)
       org.xbill.DNS.TSIG.apply(TSIG.java:517)
       org.xbill.DNS.TSIG_apply_4_Test.applyVariousErrorAndSignature(TSIG_apply_4_Test.java:130)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
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
[         9 tests successful      ]
[         3 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class org.xbill.DNS.TSIG: 

 * Transaction signature handling. This class generates and verifies TSIG records on messages, which
 * provide transaction security.
 *
 * @see TSIGRecord
 * @author Brian Wellington
 

api document of method apply(Message, int, TSIGRecord, boolean): 

   * Generates a TSIG record with a specific error for a message and adds it to the message.
   *
   * @param m The message
   * @param error The error
   * @param old If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and
   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for
   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).
   * @since 3.2
   

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		int org.xbill.DNS.Message.tsigState;
		int org.xbill.DNS.Message.TSIG_SIGNED;
	related functions:
		method `public boolean isSigned()` returns `boolean`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`, api document: 
   * Was this message signed by a TSIG?
   *
   * @see TSIG
   ;
class `org.xbill.DNS.TSIGRecord`:
	
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ADDITIONAL;
	
class `org.xbill.DNS.TSIG.StreamGenerator`:
	related functions:
		method ` void generate(Message, boolean)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;
class `org.xbill.DNS.SimpleResolver`:
	related functions:
		method `private void applyEDNS(Message)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;
class `org.xbill.DNS.Update`:
	related functions:
		method `private void newPrereq(Record)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;
class `org.xbill.DNS.tools.update`:
	related functions:
		method ` void doGlue(Tokenizer)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;
class `org.xbill.DNS.SIG0`:
	related functions:
		method `public static void signMessage(Message, KEYRecord, PrivateKey, SIGRecord, Instant)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`, api document: 
   * Sign a message with SIG(0). The DNS key and private key must refer to the same underlying
   * cryptographic key.
   *
   * @param message The message to be signed
   * @param key The DNSKEY record to use as part of signing
   * @param privkey The PrivateKey to use when signing
   * @param previous If this message is a response, the SIG(0) from the query
   * @param timeSigned The time instant when the message has been signed.
   ;
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method ` byte[] buildErrorMessage(Header, int, Record)` returns `byte[]`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
