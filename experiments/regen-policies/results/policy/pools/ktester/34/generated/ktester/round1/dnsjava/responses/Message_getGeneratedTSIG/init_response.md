```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class Message_getGeneratedTSIG_Test {

    /** Verify that a newly‑created Message has no generated TSIG. */
    @Test
    void generatedTSIG_isNullOnNewMessage() {
        Message msg = new Message();               // uses the default constructor
        assertNull(msg.getGeneratedTSIG(),
                "A fresh Message should not have a generated TSIG");
    }

    /** Verify that calling {@code setTSIG(TSIG)} creates a generated TSIG record. */
    @Test
    void generatedTSIG_isCreatedAfterSetTSIG() {
        Message msg = new Message();

        // Create a simple TSIG key – the constructor arguments are
        // (Name name, String algorithm, String secret).  The concrete values
        // are not important for the purpose of this test; they just need to be
        // syntactically valid.
        TSIG tsigKey = new TSIG(
                Name.fromConstantString("example.com."),
                "hmac-md5.sig-alg.reg.int.",   // a common TSIG algorithm name
                "c2VjcmV0"                     // base‑64 for the string "secret"
        );

        // The Message API will generate a TSIGRecord and store it in the
        // private field `generatedTsig`.  The accessor we are testing is
        // `getGeneratedTSIG()`.
        msg.setTSIG(tsigKey);

        TSIGRecord generated = msg.getGeneratedTSIG();
        assertNotNull(generated, "setTSIG should have created a generated TSIGRecord");
        assertEquals(tsigKey.getName(), generated.getName(),
                "The generated TSIGRecord should carry the same name as the TSIG key");
    }

    /** Verify that the overload {@code setTSIG(TSIG, int, TSIGRecord)} also creates a generated TSIG. */
    @Test
    void generatedTSIG_isCreatedAfterSetTSIGWithErrorAndQueryRecord() {
        Message msg = new Message();

        TSIG tsigKey = new TSIG(
                Name.fromConstantString("example.org."),
                "hmac-sha256.",
                "c2VjcmV0"                     // base‑64 for the string "secret"
        );

        // The third argument (query TSIG) can be null – the Message implementation
        // will still generate a new TSIGRecord for the response.
        msg.setTSIG(tsigKey, 0, null);

        TSIGRecord generated = msg.getGeneratedTSIG();
        assertNotNull(generated,
                "setTSIG(key, error, query) should have created a generated TSIGRecord");
        assertEquals(tsigKey.getName(), generated.getName(),
                "The generated TSIGRecord should have the same name as the TSIG key");
    }

    /** Ensure that a generated TSIG can be retrieved repeatedly without side‑effects. */
    @Test
    void generatedTSIG_isStableAcrossMultipleCalls() {
        Message msg = new Message();

        TSIG tsigKey = new TSIG(
                Name.fromConstantString("stable.example."),
                "hmac-md5.",
                "c2VjcmV0"
        );

        msg.setTSIG(tsigKey);

        TSIGRecord firstCall = msg.getGeneratedTSIG();
        TSIGRecord secondCall = msg.getGeneratedTSIG();

        assertSame(firstCall, secondCall,
                "Multiple calls to getGeneratedTSIG should return the same instance");
    }
}
```