```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Message#setTSIG(TSIG)}.
 *
 * <p>The tests verify that a TSIG record is correctly added to the
 * {@link Section#ADDITIONAL} section, that {@link Message#getTSIG()} returns the
 * generated record and that the message reports being signed.</p>
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_Test {

    private Message message;
    private TSIG tsigKey;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a simple query message (question only) – this is a typical
        // starting point for a signed message.
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        message = Message.newQuery(question);

        // Build a TSIG key.  The algorithm name used by the DNS library is
        // "hmac-md5.sig-alg.reg.int." – any valid algorithm works for the test.
        Name alg = Name.fromConstantString("hmac-md5.sig-alg.reg.int.");
        Name name = Name.fromConstantString("key.example.com.");
        // a short dummy key (base64 "YWJjZDEyMw==" == "abcd123")
        byte[] keyBytes = base64.fromString("YWJjZDEyMw==");
        SecretKey secret = new SecretKeySpec(keyBytes, "HmacMD5");
        tsigKey = new TSIG(alg, name, secret, Clock.systemUTC());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this test suite.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /** Verify that calling {@code setTSIG} adds a TSIG record to the additional section. */
    @Test
    void setTSIGAddsRecordToAdditionalSection() {
        // Pre‑condition: no TSIG records present.
        assertTrue(message.getSection(Section.ADDITIONAL).isEmpty(),
                "Additional section should start empty");

        // Act
        message.setTSIG(tsigKey);

        // Post‑condition: exactly one record in the additional section and it is a TSIGRecord.
        assertEquals(1, message.getSection(Section.ADDITIONAL).size(),
                "Additional section should contain one record after setTSIG");
        assertTrue(message.getSection(Section.ADDITIONAL).get(0) instanceof TSIGRecord,
                "Record in additional section should be a TSIGRecord");

        // The generated TSIG record should be accessible via getTSIG().
        TSIGRecord generated = message.getTSIG();
        assertNotNull(generated, "Generated TSIGRecord should not be null");
        assertEquals(tsigKey.getName(), generated.getName(),
                "TSIGRecord name should match the key name");
        assertTrue(message.isSigned(), "Message should report being signed after setTSIG");
    }

    /** Verify that calling {@code setTSIG} with a {@code null} argument throws NPE. */
    @Test
    void setTSIGWithNullKeyThrowsException() {
        assertThrows(NullPointerException.class, () -> message.setTSIG(null),
                "setTSIG(null) should throw NullPointerException");
    }

    /** Verify that a second call to {@code setTSIG} replaces the previous TSIG record. */
    @Test
    void setTSIGReplacesExistingRecord() throws IOException {
        // First TSIG
        message.setTSIG(tsigKey);
        TSIGRecord first = message.getTSIG();
        assertNotNull(first, "First generated TSIGRecord should exist");

        // Build a different key
        Name otherAlg = Name.fromConstantString("hmac-sha256.");
        Name otherName = Name.fromConstantString("otherkey.example.com.");
        byte[] otherKeyBytes = base64.fromString("c2Vjb25kS2V5"); // "secondKey"
        SecretKey otherSecret = new SecretKeySpec(otherKeyBytes, "HmacSHA256");
        TSIG otherTsig = new TSIG(otherAlg, otherName, otherSecret, Clock.systemUTC());

        // Second TSIG – should replace the first one.
        message.setTSIG(otherTsig);
        TSIGRecord second = message.getTSIG();
        assertNotNull(second, "Second generated TSIGRecord should exist");
        assertNotSame(first, second, "TSIGRecord instance should be replaced");

        // Only one TSIGRecord must remain in the additional section.
        assertEquals(1, message.getSection(Section.ADDITIONAL).size(),
                "Only one TSIGRecord should be present after replacement");
        assertEquals(otherTsig.getName(), second.getName(),
                "Replaced TSIGRecord should reflect the new key name");
    }
}
```