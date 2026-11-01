```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_getGeneratedTSIG_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message instance for each test
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference – not strictly required but keeps the test tidy
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verify that a newly‑created Message has no generated TSIG record.
     */
    @Test
    void getGeneratedTSIG_returnsNullWhenNotSet() {
        assertNull(message.getGeneratedTSIG(),
                "A Message that has never been signed should return null from getGeneratedTSIG()");
    }

    /**
     * Verify that after calling {@code setTSIG} the generated TSIG record is
     * returned by {@code getGeneratedTSIG}.  The {@code TSIG} class provides a
     * simple constructor that takes a name and a secret; we use a deterministic
     * name and secret so the test does not depend on external resources.
     */
    @Test
    void getGeneratedTSIG_returnsRecordAfterSetTSIG() throws Exception {
        // Arrange – create a TSIG key (algorithm “hmac-md5” is the default)
        Name keyName = Name.fromConstantString("testkey.");
        String secret = "YWJjZGVmZ2hpamtsbW5vcA=="; // base64 for "abcdefghijklmnop"
        TSIG tsig = new TSIG(keyName, secret);

        // Act – associate the TSIG with the message
        message.setTSIG(tsig);

        // Assert – a TSIGRecord should now be present
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should not be null after setTSIG");
        assertEquals(tsig.getName(), generated.getName(),
                "The generated TSIGRecord should carry the same name as the TSIG key");
    }

    /**
     * Verify that the overload {@code setTSIG(TSIG, int, TSIGRecord)} also
     * populates the generated TSIG field.  We pass {@code null} for the query
     * TSIGRecord because it is optional for this test.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithErrorAndQuery() throws Exception {
        // Arrange
        Name keyName = Name.fromConstantString("anotherkey.");
        String secret = "c2VjcmV0c2VjcmV0"; // base64 for "secretsecret"
        TSIG tsig = new TSIG(keyName, secret);
        int error = Rcode.NOERROR; // no error

        // Act
        message.setTSIG(tsig, error, null);

        // Assert
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be present after setTSIG with error code");
        assertEquals(keyName, generated.getName(),
                "Generated TSIGRecord should retain the TSIG key name");
    }
}
```