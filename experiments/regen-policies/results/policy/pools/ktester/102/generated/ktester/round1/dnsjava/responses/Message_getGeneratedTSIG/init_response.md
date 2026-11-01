```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#getGeneratedTSIG()}.
 *
 * <p>The tests verify the default state (no generated TSIG) and the state after a TSIG
 * is attached to the message via the public {@code setTSIG} methods.
 */
@ExtendWith(MockitoExtension.class)
class Message_getGeneratedTSIG_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

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

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * By default a newly constructed {@link Message} does not contain a generated TSIG.
     */
    @Test
    void getGeneratedTSIG_withoutSettingTSIG_returnsNull() {
        assertNull(message.getGeneratedTSIG(),
                "A fresh Message should have no generated TSIG");
    }

    /**
     * After attaching a TSIG key with {@code setTSIG(TSIG)} the message should expose a
     * non‑null {@link TSIGRecord} via {@code getGeneratedTSIG()}.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIG_returnsRecord() throws IOException {
        // Prepare a simple TSIG key. The concrete constructor signature may vary
        // across library versions; the one used here is the most common.
        Name keyName = Name.fromString("testkey.");
        String algorithm = "hmac-md5.sig-alg.reg.int.";
        byte[] secret = "secret".getBytes();

        TSIG tsigKey = new TSIG(keyName, algorithm, secret);
        // Attach the TSIG to the message. This call populates the generatedTsig field.
        message.setTSIG(tsigKey);

        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be non‑null after setTSIG");
        // Verify that the generated record carries the same key name.
        assertEquals(keyName, generated.getName(),
                "Generated TSIG record should use the TSIG key name");
    }

    /**
     * The overloaded {@code setTSIG(TSIG, int, TSIGRecord)} also results in a generated
     * TSIG record being available.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithError_returnsRecord() throws IOException {
        Name keyName = Name.fromString("testkey2.");
        String algorithm = "hmac-sha256.";
        byte[] secret = "anotherSecret".getBytes();

        TSIG tsigKey = new TSIG(keyName, algorithm, secret);

        // Use an error code of 0 (NOERROR) and no prior query TSIG.
        int error = 0;
        TSIGRecord queryTsig = null;

        // Call the overloaded method.
        message.setTSIG(tsigKey, error, queryTsig);

        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be non‑null after overloaded setTSIG");
        assertEquals(keyName, generated.getName(),
                "Generated TSIG record should reflect the TSIG key name");
    }
}
```