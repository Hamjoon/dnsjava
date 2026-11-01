```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#generate(Message, byte[], int, TSIGRecord, boolean)}.
 *
 * <p>The tests focus on the public overload that forwards to the private implementation.
 * They verify that a {@link TSIGRecord} is produced when the error code is one of the
 * values that trigger HMAC initialisation (NOERROR, BADTIME, BADTRUNC) and that the
 * {@code error} field of the record matches the supplied error code.
 *
 * <p>Reflection is used only to read the {@code error} field of {@link TSIGRecord}
 * because it does not expose a public accessor.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_generate_2_Test {

    /** A TSIG key using HMAC‑SHA256 – the same key used in the original test suite. */
    private static TSIG tsigKey;

    /** A simple DNS query message used as the basis for all generate calls. */
    private Message queryMessage;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a TSIG instance with a known key. The constructor validates the
        // algorithm name and creates the underlying SecretKey.
        tsigKey = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a minimal query message (one question) – this is sufficient for the
        // TSIG.generate method which only needs the wire representation of the
        // message.
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable static state to clean up for these tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release.
    }

    /**
     * Verify that a TSIGRecord is generated for a normal successful response
     * (error = Rcode.NOERROR). The returned record must contain the same error code
     * that was supplied to the generate method.
     */
    @Test
    void generate_successfulResponse_returnsRecordWithNoError() throws Exception {
        // Arrange
        byte[] wire = queryMessage.toWire(); // wire representation of the message
        int errorCode = Rcode.NOERROR;      // error that triggers HMAC initialisation
        TSIGRecord oldRecord = null;        // no previous TSIG record
        boolean fullSignature = true;        // generate a full signature

        // Act
        TSIGRecord result = tsigKey.generate(queryMessage, wire, errorCode, oldRecord, fullSignature);

        // Assert
        assertNotNull(result, "TSIG.generate should return a non‑null record for NOERROR");
        int recordedError = (int) getPrivateField(result, "error");
        assertEquals(errorCode, recordedError,
                "The error field of the generated TSIGRecord must match the supplied error code");
    }

    /**
     * Verify that a TSIGRecord is still generated when the error code is one of the
     * values that require a signature (BADTIME). The record’s error field must reflect
     * the supplied error.
     */
    @Test
    void generate_badTimeError_returnsRecordWithBadTime() throws Exception {
        // Arrange
        byte[] wire = queryMessage.toWire();
        int errorCode = Rcode.BADTIME; // another error that should still produce a TSIGRecord
        TSIGRecord oldRecord = null;
        boolean fullSignature = true;

        // Act
        TSIGRecord result = tsigKey.generate(queryMessage, wire, errorCode, oldRecord, fullSignature);

        // Assert
        assertNotNull(result, "TSIG.generate should return a record for BADTIME");
        int recordedError = (int) getPrivateField(result, "error");
        assertEquals(errorCode, recordedError,
                "The error field of the generated TSIGRecord must match BADTIME");
    }

    /**
     * Verify that when an error code that does **not** trigger HMAC initialisation is
     * supplied (e.g., FORMERR), the generate method still returns a TSIGRecord but
     * the HMAC is not initialised. Because the private implementation is not exposed,
     * we simply check that a record is produced and that its error field matches the
     * supplied code.
     */
    @Test
    void generate_nonSignatureError_returnsRecordWithSuppliedError() throws Exception {
        // Arrange
        byte[] wire = queryMessage.toWire();
        int errorCode = Rcode.FORMERR; // not in the list that triggers HMAC init
        TSIGRecord oldRecord = null;
        boolean fullSignature = true;

        // Act
        TSIGRecord result = tsigKey.generate(queryMessage, wire, errorCode, oldRecord, fullSignature);

        // Assert
        assertNotNull(result, "TSIG.generate should still return a TSIGRecord for FORMERR");
        int recordedError = (int) getPrivateField(result, "error");
        assertEquals(errorCode, recordedError,
                "The error field of the generated TSIGRecord must match FORMERR");
    }

    /**
     * Helper that uses reflection to read a private field from an object.
     *
     * @param target    the object containing the field
     * @param fieldName the name of the private field
     * @return the value of the field
     * @throws Exception if reflection fails
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }
}
```