
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Optional;

class Message_getGeneratedTSIG_Test {

    @Nested
    @DisplayName("Behaviour of setTSIG before toWire()")
    class SetTsigBeforeWire {

        @Test
        @DisplayName("setTSIG(TSIG) leaves generatedTSIG null")
        void setTSIG_keepsGeneratedNull() {
            Message msg = new Message();
            TSIG key = new TSIG(Name.fromConstantString("example.com."), "hmac-md5.sig-alg.reg.int.", // "secret" in base64
            "c2VjcmV0");
            // only registers the key
            msg.setTSIG(key);
            assertNull(msg.getGeneratedTSIG(), "setTSIG should not create a generated TSIG before the message is serialized");
        }

        @Test
        @DisplayName("setTSIG(TSIG,error,query) leaves generatedTSIG null")
        void setTSIGWithError_keepsGeneratedNull() {
            Message msg = new Message();
            TSIG key = new TSIG(Name.fromConstantString("example.org."), "hmac-sha256.", "c2VjcmV0");
            // error = 0, query = null
            msg.setTSIG(key, 0, null);
            assertNull(msg.getGeneratedTSIG(), "setTSIG(key,error,query) should not create a generated TSIG before wire conversion");
        }
    }

    /**
     * Verify that a newly‑created Message has no generated TSIG.
     */
    @Test
    void generatedTSIG_isNullOnNewMessage() {
        // uses the default constructor
        Message msg = new Message();
        assertNull(msg.getGeneratedTSIG(), "A fresh Message should not have a generated TSIG");
    }

    /**
     * Verify that calling {@code setTSIG(TSIG)} creates a generated TSIG record.
     */
    @Test
    void generatedTSIG_isCreatedAfterSetTSIG() {
        Message msg = new Message();
        // Create a simple TSIG key – the constructor arguments are
        // (Name name, String algorithm, String secret).  The concrete values
        // are not important for the purpose of this test; they just need to be
        // syntactically valid.
        TSIG tsigKey = new TSIG(Name.fromConstantString("example.com."), // a common TSIG algorithm name
        "hmac-md5.sig-alg.reg.int.", // base‑64 for the string "secret"
        "c2VjcmV0");
        // The Message API will generate a TSIGRecord and store it in the
        // private field `generatedTsig`.  The accessor we are testing is
        // `getGeneratedTSIG()`.
        msg.setTSIG(tsigKey);
        TSIGRecord generated = msg.getGeneratedTSIG();
        assertNotNull(generated, "setTSIG should have created a generated TSIGRecord");
        assertEquals(tsigKey.getName(), generated.getName(), "The generated TSIGRecord should carry the same name as the TSIG key");
    }

    /**
     * Verify that the overload {@code setTSIG(TSIG, int, TSIGRecord)} also creates a generated TSIG.
     */
    @Test
    void generatedTSIG_isCreatedAfterSetTSIGWithErrorAndQueryRecord() {
        Message msg = new Message();
        TSIG tsigKey = new TSIG(Name.fromConstantString("example.org."), "hmac-sha256.", // base‑64 for the string "secret"
        "c2VjcmV0");
        // The third argument (query TSIG) can be null – the Message implementation
        // will still generate a new TSIGRecord for the response.
        msg.setTSIG(tsigKey, 0, null);
        TSIGRecord generated = msg.getGeneratedTSIG();
        assertNotNull(generated, "setTSIG(key, error, query) should have created a generated TSIGRecord");
        assertEquals(tsigKey.getName(), generated.getName(), "The generated TSIGRecord should have the same name as the TSIG key");
    }

    /**
     * Ensure that a generated TSIG can be retrieved repeatedly without side‑effects.
     */
    @Test
    void generatedTSIG_isStableAcrossMultipleCalls() {
        Message msg = new Message();
        TSIG tsigKey = new TSIG(Name.fromConstantString("stable.example."), "hmac-md5.", "c2VjcmV0");
        msg.setTSIG(tsigKey);
        TSIGRecord firstCall = msg.getGeneratedTSIG();
        TSIGRecord secondCall = msg.getGeneratedTSIG();
        assertSame(firstCall, secondCall, "Multiple calls to getGeneratedTSIG should return the same instance");
    }

    @Test
    @DisplayName("generatedTSIG returns the exact instance set via reflection")
    void generatedTSIG_reflectionInjection() throws Exception {
        Message msg = new Message();
        // Create a mock TSIGRecord – we only need identity semantics.
        TSIGRecord mockRecord = mock(TSIGRecord.class);
        // Inject the mock into the private field `generatedTsig`.
        Field f = Message.class.getDeclaredField("generatedTsig");
        f.setAccessible(true);
        f.set(msg, mockRecord);
        // The accessor must return the same reference.
        TSIGRecord returned = msg.getGeneratedTSIG();
        assertSame(mockRecord, returned, "getGeneratedTSIG should return the same instance that was injected");
    }

    @Test
    @DisplayName("generatedTSIG stays null after adding a normal A record")
    void generatedTSIG_stillNullAfterNonTSIGRecord() throws Exception {
        Message msg = new Message();
        // Build a simple A record (example.com. IN A 127.0.0.1)
        Name name = Name.fromString("example.com.");
        InetAddress ip = InetAddress.getByName("127.0.0.1");
        Record aRecord = new ARecord(name, DClass.IN, 300, ip);
        // Add it to the ANSWER section (Section.ANSWER == 1 in dnsjava)
        msg.addRecord(aRecord, Section.ANSWER);
        assertNull(msg.getGeneratedTSIG(), "Adding non‑TSIG records must not create a generated TSIG");
    }

    @Test
    @DisplayName("clone() preserves the generated TSIG instance")
    void generatedTSIG_isPreservedAfterClone() throws Exception {
        Message original = new Message();
        // Insert a mock TSIGRecord via reflection
        TSIGRecord mockRecord = mock(TSIGRecord.class);
        Field f = Message.class.getDeclaredField("generatedTsig");
        f.setAccessible(true);
        f.set(original, mockRecord);
        // Perform the clone
        Message cloned = (Message) original.clone();
        // Verify that the cloned message holds the same reference
        TSIGRecord clonedGenerated = cloned.getGeneratedTSIG();
        assertSame(mockRecord, clonedGenerated, "Cloned Message must retain the same generated TSIG instance");
    }

    @Test
    @DisplayName("normalize(Message) clears generatedTSIG for a query")
    void generatedTSIG_clearedByNormalize() throws Exception {
        // Create a query message (using the convenient factory)
        Record q = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN);
        Message query = Message.newQuery(q);
        // Manually set a generated TSIG via reflection
        TSIGRecord mockRecord = mock(TSIGRecord.class);
        Field f = Message.class.getDeclaredField("generatedTsig");
        f.setAccessible(true);
        f.set(query, mockRecord);
        // Normalise – the method is expected to clear any generated TSIG for queries
        Message normalized = query.normalize(query);
        assertNull(normalized.getGeneratedTSIG(), "After normalizing a query, generatedTSIG should be null");
    }

    @Test
    @DisplayName("removeAllRecords does not touch generatedTSIG")
    void generatedTSIG_unaffectedByRemoveAllRecords() throws Exception {
        Message msg = new Message();
        // Inject a mock generated TSIG
        TSIGRecord mockRecord = mock(TSIGRecord.class);
        Field f = Message.class.getDeclaredField("generatedTsig");
        f.setAccessible(true);
        f.set(msg, mockRecord);
        // Add a normal A record to ensure the section is not empty
        Name name = Name.fromString("example.com.");
        InetAddress ip = InetAddress.getByName("127.0.0.1");
        Record aRecord = new ARecord(name, DClass.IN, 300, ip);
        msg.addRecord(aRecord, Section.ANSWER);
        // Remove all records from the ANSWER section
        msg.removeAllRecords(Section.ANSWER);
        // The generated TSIG must still be present
        assertSame(mockRecord, msg.getGeneratedTSIG(), "removeAllRecords must not modify the generated TSIG");
    }

    @Test
    @DisplayName("toWire(int) creates a generated TSIG record when a TSIG is set")
    void generatedTSIG_createdAfterToWire() throws Exception {
        Message msg = new Message();
        TSIG key = new TSIG(Name.fromConstantString("example.net."), "hmac-sha256.", "c2VjcmV0");
        // Register the key – this does NOT create the generated record yet
        msg.setTSIG(key);
        // Convert to wire format (max length 512). This triggers TSIG generation.
        byte[] wire = msg.toWire(512);
        // After wire conversion, the generated TSIG must be non‑null.
        TSIGRecord generated = msg.getGeneratedTSIG();
        assertNotNull(generated, "After toWire, Message should have generated a TSIGRecord");
        // The generated record must carry the same name as the key.
        assertEquals(key.getName(), generated.getName(), "Generated TSIGRecord must use the key's name");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:69: error: cannot find symbol
        assertEquals(tsigKey.getName(), generated.getName(), "The generated TSIGRecord should carry the same name as the TSIG key");
                            ^
  symbol:   method getName()
  location: variable tsigKey of type TSIG
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:85: error: cannot find symbol
        assertEquals(tsigKey.getName(), generated.getName(), "The generated TSIGRecord should have the same name as the TSIG key");
                            ^
  symbol:   method getName()
  location: variable tsigKey of type TSIG
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:106: error: cannot find symbol
        TSIGRecord mockRecord = mock(TSIGRecord.class);
                                ^
  symbol:   method mock(Class<TSIGRecord>)
  location: class Message_getGeneratedTSIG_Test
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:134: error: cannot find symbol
        TSIGRecord mockRecord = mock(TSIGRecord.class);
                                ^
  symbol:   method mock(Class<TSIGRecord>)
  location: class Message_getGeneratedTSIG_Test
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:152: error: cannot find symbol
        TSIGRecord mockRecord = mock(TSIGRecord.class);
                                ^
  symbol:   method mock(Class<TSIGRecord>)
  location: class Message_getGeneratedTSIG_Test
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:166: error: cannot find symbol
        TSIGRecord mockRecord = mock(TSIGRecord.class);
                                ^
  symbol:   method mock(Class<TSIGRecord>)
  location: class Message_getGeneratedTSIG_Test
src/test/java/org/xbill/DNS/Message_getGeneratedTSIG_Test.java:194: error: cannot find symbol
        assertEquals(key.getName(), generated.getName(), "Generated TSIGRecord must use the key's name");
                        ^
  symbol:   method getName()
  location: variable key of type TSIG
7 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method getGeneratedTSIG()': '\n   * Gets the generated {@link TSIGRecord}. Only valid if the messages has been converted to wire\n   * format with {@link #toWire(int)} before.\n   *\n   * @return A generated TSIG record or {@code null}.\n   ', 'return type': 'org.xbill.DNS.TSIGRecord', 'dependent classes': 'class `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\torg.xbill.DNS.TSIGRecord org.xbill.DNS.Message.generatedTsig;\n\trelated functions:\n\t\tmethod ` TSIGRecord getGeneratedTSIG()` returns `org.xbill.DNS.TSIGRecord`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`, api document: \n   * Gets the generated {@link TSIGRecord}. Only valid if the messages has been converted to wire\n   * format with {@link #toWire(int)} before.\n   *\n   * @return A generated TSIG record or {@code null}.\n   ;\n\t\tmethod `public Message clone()` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`, api document: \n   * Creates a copy of this Message. This is done by the Resolver before adding TSIG and OPT\n   * records, for example.\n   *\n   * @see Resolver\n   * @see TSIGRecord\n   * @see OPTRecord\n   ;\n\t\tmethod `private void toWire(DNSOutput, int)` returns `void`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`, api document:  Converts a numeric extended flag into a String ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


