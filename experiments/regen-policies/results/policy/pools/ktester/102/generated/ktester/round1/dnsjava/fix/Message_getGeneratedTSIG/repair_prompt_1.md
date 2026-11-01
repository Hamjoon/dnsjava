
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
class Message_getGeneratedTSIG_Test {

    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test case.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        message = null;
    }

    /**
     * By default a newly constructed {@link Message} does not contain a generated TSIG.
     */
    @Test
    void getGeneratedTSIG_withoutSettingTSIG_returnsNull() {
        assertNull(message.getGeneratedTSIG(), "A fresh Message should have no generated TSIG");
    }

    /**
     * After attaching a TSIG key with {@code setTSIG(TSIG)} the message should expose a
     * non‑null {@link TSIGRecord} via {@code getGeneratedTSIG()}.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIG_returnsRecord() throws IOException {
        // Prepare a simple TSIG key.
        Name keyName = Name.fromString("testkey.");
        Name algorithm = Name.fromString("hmac-md5.sig-alg.reg.int.");
        byte[] secret = "secret".getBytes();
        TSIG tsigKey = new TSIG(keyName, algorithm, secret);
        // Attach the TSIG to the message. This call populates the generatedTsig field.
        message.setTSIG(tsigKey);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be non‑null after setTSIG");
        // Verify that the generated record carries the same key name.
        assertEquals(keyName, generated.getName(), "Generated TSIG record should use the TSIG key name");
    }

    /**
     * The overloaded {@code setTSIG(TSIG, int, TSIGRecord)} also results in a generated
     * TSIG record being available.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithError_returnsRecord() throws IOException {
        Name keyName = Name.fromString("testkey2.");
        Name algorithm = Name.fromString("hmac-sha256.");
        byte[] secret = "anotherSecret".getBytes();
        TSIG tsigKey = new TSIG(keyName, algorithm, secret);
        // Use an error code of 0 (NOERROR) and no prior query TSIG.
        int error = 0;
        TSIGRecord queryTsig = null;
        // Call the overloaded method.
        message.setTSIG(tsigKey, error, queryTsig);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be non‑null after overloaded setTSIG");
        assertEquals(keyName, generated.getName(), "Generated TSIG record should reflect the TSIG key name");
    }

    /**
     * Supplying a {@code null} TSIG key must leave {@code generatedTsig} untouched
     * (i.e. still {@code null}).
     */
    @Test
    void getGeneratedTSIG_afterSetTSIG_nullKey_returnsNull() throws IOException {
        message.setTSIG((TSIG) null);
        assertNull(message.getGeneratedTSIG(), "Calling setTSIG(null) should not create a generated TSIG");
    }

    /**
     * Verify that the error field stored in the generated TSIG matches the error
     * code supplied to {@code setTSIG(TSIG, int, TSIGRecord)} when the query TSIG is {@code null}.
     */
    @ParameterizedTest
    @CsvSource({ // NOERROR
    // FORMERR (example)
    "0", // NOTAUTH (example)
    "1", "5" })
    void getGeneratedTSIG_afterSetTSIGWithError_nullQueryTsig_errorMatches(int errorCode) throws IOException {
        Name keyName = Name.fromString("errorkey.");
        Name algorithm = Name.fromString("hmac-sha256.");
        TSIG tsigKey = new TSIG(keyName, algorithm, "secret".getBytes());
        // queryTsig is null → MAC should be null in the generated record.
        message.setTSIG(tsigKey, errorCode, null);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG must be present");
        assertEquals(errorCode, generated.getError(), "Generated TSIG error field should match the supplied error code");
        assertNull(generated.getMac(), "When queryTSIG is null the generated MAC should be null");
    }

    /**
     * When a non‑null query TSIG is supplied, the generated TSIG must copy its MAC
     * and store the supplied error code.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithError_andQueryTsig_copiesMacAndError() throws IOException {
        Name keyName = Name.fromString("copykey.");
        Name algorithm = Name.fromString("hmac-sha256.");
        TSIG tsigKey = new TSIG(keyName, algorithm, "secret".getBytes());
        // Build a query TSIGRecord with a known MAC.
        byte[] mac = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        TSIGRecord queryTsig = new TSIGRecord(// name
        keyName, // dclass
        DClass.IN, // ttl (irrelevant for TSIG)
        0L, // algorithm
        algorithm, // time signed
        new Date(), // fudge
        0, // MAC
        mac, // original ID
        0, // error (NOERROR)
        0, // other data
        null);
        // arbitrary non‑zero error
        int errorCode = 9;
        message.setTSIG(tsigKey, errorCode, queryTsig);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG must be present");
        assertEquals(errorCode, generated.getError(), "Generated TSIG error field should match the supplied error code");
        assertArrayEquals(mac, generated.getMac(), "Generated TSIG MAC must be copied from the query TSIG");
    }

    /**
     * Consecutive calls to {@code setTSIG} must overwrite the previous generated TSIG.
     */
    @Test
    void getGeneratedTSIG_afterConsecutiveSetTSIG_calls_lastOneWins() throws IOException {
        // First call – simple keyA
        Name keyA = Name.fromString("keyA.");
        Name algA = Name.fromString("hmac-md5.");
        TSIG tsigA = new TSIG(keyA, algA, "a".getBytes());
        message.setTSIG(tsigA);
        // Second call – keyB with error code 5 and null query TSIG
        Name keyB = Name.fromString("keyB.");
        Name algB = Name.fromString("hmac-sha256.");
        TSIG tsigB = new TSIG(keyB, algB, "b".getBytes());
        int error = 5;
        message.setTSIG(tsigB, error, null);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG must reflect the second setTSIG call");
        assertEquals(keyB, generated.getName(), "Generated TSIG should use the name of the last key (keyB)");
        assertEquals(error, generated.getError(), "Generated TSIG error must match the error supplied in the last call");
        // MAC should be null because the last call used a null query TSIG.
        assertNull(generated.getMac(), "Generated TSIG MAC should be null after the last call used a null query TSIG");
    }

    /**
     * After a successful {@code setTSIG} call, invoking {@code setTSIG(null)} must clear
     * the generated TSIG.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIG_thenClear_returnsNull() throws IOException {
        Name keyName = Name.fromString("clearkey.");
        Name algorithm = Name.fromString("hmac-md5.");
        TSIG tsigKey = new TSIG(keyName, algorithm, "secret".getBytes());
        message.setTSIG(tsigKey);
        assertNotNull(message.getGeneratedTSIG(), "Precondition: generated TSIG must be present before clearing");
        // Clear the TSIG.
        message.setTSIG((TSIG) null);
        assertNull(message.getGeneratedTSIG(), "Generated TSIG should be cleared after setTSIG(null)");
    }

    /**
     * Cloning a signed message must preserve the generated TSIG record.
     */
    @Test
    void getGeneratedTSIG_afterCloningSignedMessage_preservesRecord() throws IOException, CloneNotSupportedException {
        Name keyName = Name.fromString("clonekey.");
        Name algorithm = Name.fromString("hmac-sha256.");
        TSIG tsigKey = new TSIG(keyName, algorithm, "cloneSecret".getBytes());
        int error = 0;
        message.setTSIG(tsigKey, error, null);
        TSIGRecord originalGenerated = message.getGeneratedTSIG();
        assertNotNull(originalGenerated, "Original message must have a generated TSIG before cloning");
        // Clone the message.
        Message cloned = (Message) message.clone();
        TSIGRecord clonedGenerated = cloned.getGeneratedTSIG();
        assertNotNull(clonedGenerated, "Cloned message should retain the generated TSIG");
        // Compare the essential fields.
        assertEquals(originalGenerated.getName(), clonedGenerated.getName(), "Cloned TSIG record name should match the original");
        assertEquals(originalGenerated.getAlgorithm(), clonedGenerated.getAlgorithm(), "Cloned TSIG algorithm should match the original");
        assertEquals(originalGenerated.getError(), clonedGenerated.getError(), "Cloned TSIG error code should match the original");
        assertArrayEquals(originalGenerated.getMac(), clonedGenerated.getMac(), "Cloned TSIG MAC should match the original");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:104: error: cannot find symbol
        assertNull(generated.getMac(), "When queryTSIG is null the generated MAC should be null");
                            ^
  symbol:   method getMac()
  location: variable generated of type TSIGRecord
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:135: error: cannot find symbol
        assertArrayEquals(mac, generated.getMac(), "Generated TSIG MAC must be copied from the query TSIG");
                                        ^
  symbol:   method getMac()
  location: variable generated of type TSIGRecord
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:159: error: cannot find symbol
        assertNull(generated.getMac(), "Generated TSIG MAC should be null after the last call used a null query TSIG");
                            ^
  symbol:   method getMac()
  location: variable generated of type TSIGRecord
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:198: error: cannot find symbol
        assertArrayEquals(originalGenerated.getMac(), clonedGenerated.getMac(), "Cloned TSIG MAC should match the original");
                                           ^
  symbol:   method getMac()
  location: variable originalGenerated of type TSIGRecord
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:198: error: cannot find symbol
        assertArrayEquals(originalGenerated.getMac(), clonedGenerated.getMac(), "Cloned TSIG MAC should match the original");
                                                                     ^
  symbol:   method getMac()
  location: variable clonedGenerated of type TSIGRecord
Note: src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
5 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method getGeneratedTSIG()': '\n   * Gets the generated {@link TSIGRecord}. Only valid if the messages has been converted to wire\n   * format with {@link #toWire(int)} before.\n   *\n   * @return A generated TSIG record or {@code null}.\n   ', 'return type': 'org.xbill.DNS.TSIGRecord', 'dependent classes': 'class `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\torg.xbill.DNS.TSIGRecord org.xbill.DNS.Message.generatedTsig;\n\trelated functions:\n\t\tmethod ` TSIGRecord getGeneratedTSIG()` returns `org.xbill.DNS.TSIGRecord`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`, api document: \n   * Gets the generated {@link TSIGRecord}. Only valid if the messages has been converted to wire\n   * format with {@link #toWire(int)} before.\n   *\n   * @return A generated TSIG record or {@code null}.\n   ;\n\t\tmethod `private boolean toWire(DNSOutput, int)` returns `boolean`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`;\n\t\tmethod `public Message clone()` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`, api document: \n   * Creates a copy of this Message. This is done by the Resolver before adding TSIG and OPT\n   * records, for example.\n   *\n   * @see Resolver\n   * @see TSIGRecord\n   * @see OPTRecord\n   ;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`, api document:  Converts a numeric extended flag into a String ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


