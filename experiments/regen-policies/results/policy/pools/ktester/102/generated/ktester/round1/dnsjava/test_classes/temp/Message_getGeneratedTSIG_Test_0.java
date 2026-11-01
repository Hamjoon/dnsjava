package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        assertEquals(keyName, generated.getName(), "Generated TSIG record should use the TSIG key name");
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
    "0", // FORMERR (example)
    "1", // NOTAUTH (example)
    "5" })
    void getGeneratedTSIG_afterSetTSIGWithError_nullQueryTsig_errorMatches(int errorCode) throws IOException {
        Name keyName = Name.fromString("errorkey.");
        TSIG tsigKey = new TSIG(keyName, "hmac-sha256.", "secret".getBytes());
        // queryTsig is null → MAC should be null in the generated record.
        message.setTSIG(tsigKey, errorCode, null);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG must be present");
        assertEquals(errorCode, generated.getError(), "Generated TSIG error field should match the supplied error code");
        assertNull(generated.getMAC(), "When queryTSIG is null the generated MAC should be null");
    }

    /**
     * When a non‑null query TSIG is supplied, the generated TSIG must copy its MAC
     * and store the supplied error code.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithError_andQueryTsig_copiesMacAndError() throws IOException {
        Name keyName = Name.fromString("copykey.");
        TSIG tsigKey = new TSIG(keyName, "hmac-sha256.", "secret".getBytes());
        // Build a query TSIGRecord with a known MAC.
        byte[] mac = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        TSIGRecord queryTsig = new TSIGRecord(// name
        keyName, // dclass
        DClass.IN, // ttl (irrelevant for TSIG)
        0L, tsigKey, // error (NOERROR)
        0, // MAC
        mac);
        // arbitrary non‑zero error
        int errorCode = 9;
        message.setTSIG(tsigKey, errorCode, queryTsig);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG must be present");
        assertEquals(errorCode, generated.getError(), "Generated TSIG error field should match the supplied error code");
        assertArrayEquals(mac, generated.getMAC(), "Generated TSIG MAC must be copied from the query TSIG");
    }

    /**
     * Consecutive calls to {@code setTSIG} must overwrite the previous generated TSIG.
     */
    @Test
    void getGeneratedTSIG_afterConsecutiveSetTSIG_calls_lastOneWins() throws IOException {
        // First call – simple keyA
        Name keyA = Name.fromString("keyA.");
        TSIG tsigA = new TSIG(keyA, "hmac-md5.", "a".getBytes());
        message.setTSIG(tsigA);
        // Second call – keyB with error code 5 and null query TSIG
        Name keyB = Name.fromString("keyB.");
        TSIG tsigB = new TSIG(keyB, "hmac-sha256.", "b".getBytes());
        int error = 5;
        message.setTSIG(tsigB, error, null);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG must reflect the second setTSIG call");
        assertEquals(keyB, generated.getName(), "Generated TSIG should use the name of the last key (keyB)");
        assertEquals(error, generated.getError(), "Generated TSIG error must match the error supplied in the last call");
        // MAC should be null because the last call used a null query TSIG.
        assertNull(generated.getMAC(), "Generated TSIG MAC should be null after the last call used a null query TSIG");
    }

    /**
     * After a successful {@code setTSIG} call, invoking {@code setTSIG(null)} must clear
     * the generated TSIG.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIG_thenClear_returnsNull() throws IOException {
        Name keyName = Name.fromString("clearkey.");
        TSIG tsigKey = new TSIG(keyName, "hmac-md5.", "secret".getBytes());
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
        TSIG tsigKey = new TSIG(keyName, "hmac-sha256.", "cloneSecret".getBytes());
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
        assertArrayEquals(originalGenerated.getMAC(), clonedGenerated.getMAC(), "Cloned TSIG MAC should match the original");
    }
}
