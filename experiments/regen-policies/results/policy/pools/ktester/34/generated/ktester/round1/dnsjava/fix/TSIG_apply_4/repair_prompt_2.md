
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import java.time.Clock;
import java.time.Instant;
import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.ParameterizedTest;

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

    /* Supported algorithm names – the real library defines many more,
       but for the tests we only need these three. */
    public static final Name HMAC_SHA256 = Name.fromString("hmac-sha256.");

    public static final Name HMAC_SHA1 = Name.fromString("hmac-sha1.");

    public static final Name HMAC_MD5 = Name.fromString("hmac-md5.");

    /**
     * Default fudge factor (seconds).
     */
    public static final int FUDGE = 300;

    private final Name algorithm;

    private final Name name;

    private final SecretKey key;

    private final Clock clock;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // a deterministic instant – 2023‑01‑01T00:00:00Z
        fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"));
        // algorithm = HMAC‑SHA256, name = "example.", key = "12345678"
        Name algorithm = TSIG.HMAC_SHA256;
        Name name = Name.fromString("example.");
        SecretKey key = new SecretKeySpec("12345678".getBytes(), "HmacSHA256");
        tsig = new TSIG(algorithm, name, key, fixedClock);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // a simple query message – the content of the question is irrelevant for the TSIG logic
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        message = Message.newQuery(question);
    }

    /**
     * Generates a TSIG record with a specific error for a message and adds it
     * to the message.
     *
     * @param m            The message to sign.
     * @param error        The error code (treated as unsigned 16‑bit).
     * @param old          If this message is a response, the TSIG from the request.
     * @param fullSignature {@code true} if this is the first message of a TCP
     *                      connection; {@code false} for subsequent messages.
     */
    public void apply(Message m, int error, TSIGRecord old, boolean fullSignature) {
        if (m == null) {
            throw new NullPointerException("Message is null");
        }
        /* ---- validation of the old TSIGRecord (if supplied) ---- */
        if (old != null) {
            Name alg = getAlgorithmFromRecord(old);
            if (alg == null) {
                // The test expects a NullPointerException when the algorithm is null.
                throw new NullPointerException("Algorithm name is null");
            }
            if (!isSupportedAlgorithm(alg)) {
                // The test expects an IllegalArgumentException for unsupported algorithms.
                throw new IllegalArgumentException("Unsupported algorithm: " + alg);
            }
        }
        /* ---- treat the error as an unsigned 16‑bit value ---- */
        int unsignedError = error & 0xFFFF;
        /* ---- create a (very) simple TSIGRecord ---- */
        TSIGRecord tsigRecord = new TSIGRecord(// owner name
        this.name, // class
        DClass.IN, // ttl (not used for TSIG)
        0L, // algorithm name
        this.algorithm, // time signed
        Instant.now(this.clock), // fudge
        FUDGE, // MAC (empty for test purposes)
        new byte[0], // original ID
        m.getHeader().getID(), // error field
        unsignedError, // other data (empty)
        new byte[0]);
        /* ---- add the record to the ADDITIONAL section ---- */
        m.addRecord(tsigRecord, Section.ADDITIONAL);
        /* ---- mark the message as signed (internal state) ---- */
        try {
            Field tsigStateField = Message.class.getDeclaredField("tsigState");
            tsigStateField.setAccessible(true);
            Field signedConstField = Message.class.getDeclaredField("TSIG_SIGNED");
            signedConstField.setAccessible(true);
            int signedState = signedConstField.getInt(null);
            tsigStateField.setInt(m, signedState);
        } catch (ReflectiveOperationException ignored) {
            /* If the internal fields cannot be accessed the test still succeeds
               because the presence of the TSIGRecord in the ADDITIONAL section
               is sufficient for most checks. */
        }
    }

    /**
     * Returns {@code true} if the algorithm name is one of the supported ones.
     */
    private boolean isSupportedAlgorithm(Name alg) {
        return alg.equals(HMAC_SHA1) || alg.equals(HMAC_SHA256) || alg.equals(HMAC_MD5);
    }

    /**
     * Extracts the algorithm name from a TSIGRecord via reflection.
     */
    private Name getAlgorithmFromRecord(TSIGRecord rec) {
        try {
            Field f = TSIGRecord.class.getDeclaredField("algorithm");
            f.setAccessible(true);
            return (Name) f.get(rec);
        } catch (ReflectiveOperationException e) {
            return null;
        }
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
    void applyWithPreviousTSIGRecord() throws Exception {
        // create a dummy previous TSIGRecord (the constructor arguments are arbitrary but valid)
        TSIGRecord previous = new // name
        TSIGRecord(// dclass
        Name.fromString("example."), // ttl
        DClass.IN, // algorithm
        0L, // timeSigned
        TSIG.HMAC_SHA256, // fudge
        Instant.now(fixedClock), // mac (signature)
        TSIG.FUDGE, // original ID
        new byte[0], // error
        message.getHeader().getID(), // other data
        Rcode.NOERROR, new byte[0]);
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
    // non‑zero error
    "0, false", // negative error (edge case)
    "0, true", "9, false", "-1, false" })
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
    void applyWithOldRecordAndFullSignature() throws Exception {
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
    void applyOldRecordWithNullAlgorithmThrowsNPE() throws Exception {
        TSIGRecord malformed = new // null algorithm name
        TSIGRecord(// null algorithm name
        Name.fromString("example.com."), // null algorithm name
        DClass.IN, // null algorithm name
        300L, null, Instant.now(fixedClock), TSIG.FUDGE, new byte[0], message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        assertThrows(NullPointerException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "A TSIGRecord with a null algorithm should cause NullPointerException");
    }

    /**
     * Exception scenario: unsupported algorithm name → IllegalArgumentException.
     */
    @Test
    void applyOldRecordWithUnsupportedAlgorithmThrowsIAE() throws Exception {
        Name unsupported = Name.fromString("unsupported-alg.");
        TSIGRecord malformed = new TSIGRecord(Name.fromString("example.com."), DClass.IN, 300L, unsupported, Instant.now(fixedClock), TSIG.FUDGE, new byte[0], message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "An unsupported algorithm should cause IllegalArgumentException");
    }

    /**
     * Exception scenario: old TSIGRecord contains a null signature (treated as null key) → NPE.
     */
    @Test
    void applyOldRecordWithNullSignatureThrowsNPE() throws Exception {
        TSIGRecord malformed = new // null signature (simulates missing key)
        TSIGRecord(// null signature (simulates missing key)
        Name.fromString("example.com."), // null signature (simulates missing key)
        DClass.IN, // null signature (simulates missing key)
        300L, // null signature (simulates missing key)
        TSIG.HMAC_SHA256, // null signature (simulates missing key)
        Instant.now(fixedClock), // null signature (simulates missing key)
        TSIG.FUDGE, null, message.getHeader().getID(), Rcode.NOERROR, new byte[0]);
        assertThrows(NullPointerException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "A TSIGRecord with a null signature should cause NullPointerException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:58: error: cannot find symbol
        fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"));
                                                                        ^
  symbol:   variable ZoneId
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:62: error: cannot find symbol
        SecretKey key = new SecretKeySpec("12345678".getBytes(), "HmacSHA256");
                            ^
  symbol:   class SecretKeySpec
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:102: error: no suitable constructor found for TSIGRecord(Name,int,long,Name,Instant,int,byte[],int,int,byte[])
        TSIGRecord tsigRecord = new TSIGRecord(// owner name
                                ^
    constructor TSIGRecord.TSIGRecord(Name,int,long,Name,Date,int,byte[],int,int,byte[]) is not applicable
      (argument mismatch; Instant cannot be converted to Date)
    constructor TSIGRecord.TSIGRecord(Name,int,long,Name,Instant,Duration,byte[],int,int,byte[]) is not applicable
      (argument mismatch; int cannot be converted to Duration)
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:156: error: cannot find symbol
        assertNull(message.getTSIG(), "Message should start without a TSIG record");
        ^
  symbol:   method assertNull(TSIGRecord,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:163: error: cannot find symbol
        assertNotNull(tsigRecord, "TSIG record should have been added to the message");
        ^
  symbol:   method assertNotNull(TSIGRecord,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:165: error: cannot find symbol
        assertTrue(message.isSigned(), "Message should be marked as signed after apply()");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:168: error: cannot find symbol
        assertTrue(addition.length > 0, "Addition section must contain at least one record");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:169: error: cannot find symbol
        assertEquals(tsigRecord, addition[addition.length - 1], "TSIG record should be the last record in the ADDITION section");
        ^
  symbol:   method assertEquals(TSIGRecord,Record,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:182: error: cannot find symbol
        assertNotNull(message.getTSIG(), "TSIG record should be added even when error != NOERROR");
        ^
  symbol:   method assertNotNull(TSIGRecord,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:184: error: cannot find symbol
        assertTrue(message.isSigned(), "Message should be marked as signed regardless of error code");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:208: error: cannot find symbol
        assertNotNull(resulting, "Resulting TSIG record must be present");
        ^
  symbol:   method assertNotNull(TSIGRecord,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:209: error: cannot find symbol
        assertTrue(message.isSigned(), "Message should be signed after applying with a previous TSIG");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:210: error: cannot find symbol
        assertNotEquals(previous, resulting, "A new TSIGRecord should be generated, not reuse the old one");
        ^
  symbol:   method assertNotEquals(TSIGRecord,TSIGRecord,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:225: error: cannot find symbol
        assertNotNull(tsigRecord, "TSIG record must be present");
        ^
  symbol:   method assertNotNull(TSIGRecord,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:232: error: cannot find symbol
            assertEquals(error, actualError, "The TSIGRecord error field should equal the error argument passed to apply()");
            ^
  symbol:   method assertEquals(int,int,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:234: error: cannot find symbol
            fail("Unable to access TSIGRecord.error field via reflection", e);
            ^
  symbol:   method fail(String,ReflectiveOperationException)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:236: error: cannot find symbol
        assertTrue(message.isSigned(), "Message should be marked as signed");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:244: error: cannot find symbol
        assertThrows(NullPointerException.class, () -> tsig.apply(null, Rcode.NOERROR, null, false), "Calling apply with a null Message should throw NullPointerException");
        ^
  symbol:   method assertThrows(Class<NullPointerException>,()->tsig.a[...]alse),String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:255: error: cannot find symbol
        assertNotNull(result, "A TSIG record must be added when old record and fullSignature are supplied");
        ^
  symbol:   method assertNotNull(TSIGRecord,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:256: error: cannot find symbol
        assertTrue(message.isSigned(), "Message should be signed");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:269: error: cannot find symbol
        assertThrows(NullPointerException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "A TSIGRecord with a null algorithm should cause NullPointerException");
        ^
  symbol:   method assertThrows(Class<NullPointerException>,()->tsig.a[...]alse),String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:279: error: cannot find symbol
        assertThrows(IllegalArgumentException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "An unsupported algorithm should cause IllegalArgumentException");
        ^
  symbol:   method assertThrows(Class<IllegalArgumentException>,()->tsig.a[...]alse),String)
  location: class TSIG_apply_4_Test
src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java:295: error: cannot find symbol
        assertThrows(NullPointerException.class, () -> tsig.apply(message, Rcode.NOERROR, malformed, false), "A TSIGRecord with a null signature should cause NullPointerException");
        ^
  symbol:   method assertThrows(Class<NullPointerException>,()->tsig.a[...]alse),String)
  location: class TSIG_apply_4_Test
Note: src/test/java/org/xbill/DNS/TSIG_apply_4_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
23 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.TSIG': '\n * Transaction signature handling. This class generates and verifies TSIG records on messages, which\n * provide transaction security.\n *\n * @see TSIGRecord\n * @author Brian Wellington\n ', 'api document of method apply(Message, int, TSIGRecord, boolean)': '\n   * Generates a TSIG record with a specific error for a message and adds it to the message.\n   *\n   * @param m The message\n   * @param error The error\n   * @param old If this message is a response, the TSIG from the request\n   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and\n   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for\n   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).\n   * @since 3.2\n   ', 'dependent classes': 'class `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Message.tsigState;\n\t\tint org.xbill.DNS.Message.TSIG_SIGNED;\n\trelated functions:\n\t\tmethod `public boolean isSigned()` returns `boolean`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`, api document: \n   * Was this message signed by a TSIG?\n   *\n   * @see TSIG\n   ;\nclass `org.xbill.DNS.TSIGRecord`:\n\t\nclass `org.xbill.DNS.Section`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Section.ADDITIONAL;\n\t\nclass `org.xbill.DNS.TSIG.StreamGenerator`:\n\trelated functions:\n\t\tmethod ` void generate(Message, boolean)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;\nclass `org.xbill.DNS.SimpleResolver`:\n\trelated functions:\n\t\tmethod `private void applyEDNS(Message)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;\nclass `org.xbill.DNS.Update`:\n\trelated functions:\n\t\tmethod `private void newPrereq(Record)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;\nclass `org.xbill.DNS.tools.update`:\n\trelated functions:\n\t\tmethod ` void doGlue(Tokenizer)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;\nclass `org.xbill.DNS.SIG0`:\n\trelated functions:\n\t\tmethod `public static void signMessage(Message, KEYRecord, PrivateKey, SIGRecord, Instant)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`, api document: \n   * Sign a message with SIG(0). The DNS key and private key must refer to the same underlying\n   * cryptographic key.\n   *\n   * @param message The message to be signed\n   * @param key The DNSKEY record to use as part of signing\n   * @param privkey The PrivateKey to use when signing\n   * @param previous If this message is a response, the SIG(0) from the query\n   * @param timeSigned The time instant when the message has been signed.\n   ;\nclass `org.xbill.DNS.tools.jnamed`:\n\trelated functions:\n\t\tmethod ` byte[] buildErrorMessage(Header, int, Record)` returns `byte[]`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


