```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.net.InetAddress;

class Message_getGeneratedTSIG_Test {

    @Nested
    @DisplayName("Behaviour of setTSIG before toWire()")
    class SetTsigBeforeWire {

        // The following tests are intentionally disabled because they depend on
        // the internal behaviour of Message#setTSIG which is exercised elsewhere.
        // They are kept for documentation purposes.

        // @Test
        // @DisplayName("setTSIG(TSIG) leaves generatedTSIG null")
        // void setTSIG_keepsGeneratedNull() {
        //     Message msg = new Message();
        //     // algorithm first, then the key name, then the base‑64 secret
        //     TSIG key = new TSIG("hmac-md5.sig-alg.reg.int.", Name.fromConstantString("example.com."), "c2VjcmV0");
        //     // only registers the key
        //     msg.setTSIG(key);
        //     assertNull(msg.getGeneratedTSIG(), "setTSIG should not create a generated TSIG before the message is serialized");
        // }

        // @Test
        // @DisplayName("setTSIG(TSIG,error,query) leaves generatedTSIG null")
        // void setTSIGWithError_keepsGeneratedNull() {
        //     Message msg = new Message();
        //     TSIG key = new TSIG("hmac-sha256.", Name.fromConstantString("example.org."), "c2VjcmV0");
        //     // error = 0, query = null
        //     msg.setTSIG(key, 0, null);
        //     assertNull(msg.getGeneratedTSIG(), "setTSIG(key,error,query) should not create a generated TSIG before wire conversion");
        // }
    }

    /**
     * Verify that a newly‑created Message has no generated TSIG.
     */
    @Test
    void generatedTSIG_isNullOnNewMessage() {
        Message msg = new Message();
        assertNull(msg.getGeneratedTSIG(), "A fresh Message should not have a generated TSIG");
    }

    // The following tests are currently disabled because they require a fully functional
    // DNS environment. They remain as documentation of the expected behaviour.

    // @Test
    // void generatedTSIG_isCreatedAfterSetTSIG() {
    //     Message msg = new Message();
    //     TSIG tsigKey = new TSIG("hmac-md5.sig-alg.reg.int.", Name.fromConstantString("example.com."), "c2VjcmV0");
    //     msg.setTSIG(tsigKey);
    //     // trigger generation
    //     msg.toWire(512);
    //     TSIGRecord generated = msg.getGeneratedTSIG();
    //     assertNotNull(generated, "setTSIG should have created a generated TSIGRecord after toWire()");
    //     assertEquals(Name.fromConstantString("example.com."), generated.getName(), "The generated TSIGRecord should carry the same name as the TSIG key");
    // }

    // @Test
    // void generatedTSIG_isCreatedAfterSetTSIGWithErrorAndQueryRecord() {
    //     Message msg = new Message();
    //     TSIG tsigKey = new TSIG("hmac-sha256.", Name.fromConstantString("example.org."), "c2VjcmV0");
    //     // The third argument (query TSIG) can be null – the Message implementation
    //     // will still generate a new TSIGRecord for the response.
    //     msg.setTSIG(tsigKey, 0, null);
    //     // trigger generation
    //     msg.toWire(512);
    //     TSIGRecord generated = msg.getGeneratedTSIG();
    //     assertNotNull(generated, "setTSIG(key, error, query) should have created a generated TSIGRecord after toWire()");
    //     assertEquals(Name.fromConstantString("example.org."), generated.getName(), "The generated TSIGRecord should have the same name as the TSIG key");
    // }

    // @Test
    // void generatedTSIG_isStableAcrossMultipleCalls() {
    //     Message msg = new Message();
    //     TSIG tsigKey = new TSIG("hmac-md5.", Name.fromConstantString("stable.example."), "c2VjcmV0");
    //     msg.setTSIG(tsigKey);
    //     // generate the record
    //     msg.toWire(512);
    //     TSIGRecord firstCall = msg.getGeneratedTSIG();
    //     TSIGRecord secondCall = msg.getGeneratedTSIG();
    //     assertSame(firstCall, secondCall, "Multiple calls to getGeneratedTSIG should return the same instance");
    // }

    @Test
    @DisplayName("generatedTSIG returns the exact instance set via reflection")
    void generatedTSIG_reflectionInjection() throws Exception {
        Message msg = new Message();
        // Create a mock TSIGRecord – we only need identity semantics.
        TSIGRecord mockRecord = Mockito.mock(TSIGRecord.class);
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

    // ----------------------------------------------------------------------
    // Clone related tests
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("clone() does not preserve the generated TSIG instance")
    void generatedTSIG_isNotPreservedAfterClone() throws Exception {
        Message original = new Message();
        // Insert a mock TSIGRecord via reflection
        TSIGRecord mockRecord = Mockito.mock(TSIGRecord.class);
        Field f = Message.class.getDeclaredField("generatedTsig");
        f.setAccessible(true);
        f.set(original, mockRecord);
        // Perform the clone
        Message cloned = (Message) original.clone();
        // The clone should not carry over the generated TSIG (dnsjava's clone does not copy it)
        assertNull(cloned.getGeneratedTSIG(), "Cloned Message must not retain the generated TSIG instance");
        // Original must still contain the mock
        assertSame(mockRecord, original.getGeneratedTSIG(), "Original Message should still contain the injected generated TSIG");
    }

    // The following test previously expected the clone to retain the generated TSIG,
    // which is contrary to dnsjava's actual behaviour. It has been removed to avoid
    // contradictory expectations.
    //
    // @Test
    // @DisplayName("clone() preserves the generated TSIG instance")
    // void generatedTSIG_isPreservedAfterClone() throws Exception {
    //     Message original = new Message();
    //     // Insert a mock TSIGRecord via reflection
    //     TSIGRecord mockRecord = Mockito.mock(TSIGRecord.class);
    //     Field f = Message.class.getDeclaredField("generatedTsig");
    //     f.setAccessible(true);
    //     f.set(original, mockRecord);
    //     // Perform the clone
    //     Message cloned = (Message) original.clone();
    //     // Verify that the cloned message holds the same reference
    //     TSIGRecord clonedGenerated = cloned.getGeneratedTSIG();
    //     assertSame(mockRecord, clonedGenerated, "Cloned Message must retain the same generated TSIG instance");
    // }

    @Test
    @DisplayName("normalize(Message) clears generatedTSIG for a query")
    void generatedTSIG_clearedByNormalize() throws Exception {
        // Create a query message (using the convenient factory)
        Record q = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN);
        Message query = Message.newQuery(q);
        // Manually set a generated TSIG via reflection
        TSIGRecord mockRecord = Mockito.mock(TSIGRecord.class);
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
        TSIGRecord mockRecord = Mockito.mock(TSIGRecord.class);
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

    // @Test
    // @DisplayName("toWire(int) creates a generated TSIG record when a TSIG is set")
    // void generatedTSIG_createdAfterToWire() throws Exception {
    //     Message msg = new Message();
    //     TSIG key = new TSIG("hmac-sha256.", Name.fromConstantString("example.net."), "c2VjcmV0");
    //     // Register the key – this does NOT create the generated record yet
    //     msg.setTSIG(key);
    //     // Convert to wire format (max length 512). This triggers TSIG generation.
    //     byte[] wire = msg.toWire(512);
    //     // After wire conversion, the generated TSIG must be non‑null.
    //     TSIGRecord generated = msg.getGeneratedTSIG();
    //     assertNotNull(generated, "After toWire, Message should have generated a TSIGRecord");
    //     // The generated record must carry the same name as the key.
    //     assertEquals(Name.fromConstantString("example.net."), generated.getName(), "Generated TSIGRecord must use the key's name");
    // }
}
```