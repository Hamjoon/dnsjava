```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xbill.DNS.utils.base64;

/**
 * Tests for {@link Message#setTSIG(TSIG)}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>the method signs the message and makes {@link Message#isSigned()} true;
 *   <li>the generated {@link TSIGRecord} can be retrieved via {@link Message#getTSIG()};
 *   <li>a {@code null} TSIG key results in a {@link NullPointerException}.
 * </ul>
 *
 * The test class follows the same structure as the existing {@code MessageTest} suite.
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_Test {

    /** A reusable TSIG key used by the tests. */
    private static TSIG tsigKey;

    /** A simple DNS question used to give the message some content. */
    private static Record question;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // Algorithm name for HMAC‑SHA256 (the standard name used by dnsjava)
        Name alg = Name.fromString("hmac-sha256.");
        // Arbitrary name for the TSIG key owner
        Name name = Name.fromString("example.com.");
        // A short base64‑encoded key – the actual value is irrelevant for the test
        String key = base64.fromString("abcd");
        tsigKey = new TSIG(alg, name, key);

        // Build a simple question record (A query for example.com)
        Name qname = Name.fromString("example.com.");
        question = Record.newRecord(qname, Type.A, DClass.IN);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /** Verify that a message becomes signed after {@code setTSIG}. */
    @Test
    void setTSIG_signsMessage() throws UnknownHostException {
        // Build a minimal response message (question + one answer)
        Message msg = new Message();
        msg.addRecord(question, Section.QUESTION);
        msg.addRecord(
                new ARecord(
                        question.getName(),
                        DClass.IN,
                        300,
                        InetAddress.getByName("127.0.0.1")),
                Section.ANSWER);

        // Pre‑condition: the message is not signed
        assertFalse(msg.isSigned(), "Message should start unsigned");

        // Apply the TSIG key
        msg.setTSIG(tsigKey);

        // Post‑condition: the message reports being signed
        assertTrue(msg.isSigned(), "Message should be signed after setTSIG");

        // The TSIG record should be present and non‑null
        TSIGRecord tsigRec = msg.getTSIG();
        assertNotNull(tsigRec, "TSIG record must be generated");
        assertEquals(tsigKey.getName(), tsigRec.getName(), "TSIG owner name must match the key");
    }

    /** Verify that {@code setTSIG(null)} throws {@link NullPointerException}. */
    @Test
    void setTSIG_nullKey_throwsNPE() {
        Message msg = new Message();
        msg.addRecord(question, Section.QUESTION);
        assertThrows(
                NullPointerException.class,
                () -> msg.setTSIG(null),
                "Calling setTSIG with a null key should throw NPE");
    }

    /** Verify that the generated TSIG record can be retrieved via {@code getGeneratedTSIG}. */
    @Test
    void generatedTSIG_isAccessible() {
        Message msg = new Message();
        msg.addRecord(question, Section.QUESTION);
        msg.setTSIG(tsigKey);

        TSIGRecord generated = msg.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be accessible");
        // The generated record should be the same instance returned by getTSIG()
        assertSame(msg.getTSIG(), generated, "Generated TSIG should be identical to getTSIG()");
    }

    /** Verify that a message without any records still accepts a TSIG key. */
    @Test
    void setTSIG_onEmptyMessage() {
        Message empty = new Message();
        // No records are added – we only sign the empty message
        empty.setTSIG(tsigKey);
        assertTrue(empty.isSigned(), "Empty message should be signed after setTSIG");
        assertNotNull(empty.getTSIG(), "TSIG record must exist even for an empty message");
    }
}
```