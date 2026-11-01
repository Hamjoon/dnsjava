```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#apply(Message, TSIGRecord, boolean)}.
 *
 * The tests verify that invoking {@code apply} adds a TSIG record to the
 * supplied {@link Message} and that the message reports itself as signed.
 * Both the “full‑signature” (`true`) and the “partial‑signature” (`false`)
 * code paths are exercised.
 *
 * No private members of {@link TSIG} or {@link TSIGRecord} are accessed;
 * the public API is used exclusively.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_2_Test {

    /** A deterministic key used for all tests. */
    private static TSIG tsigKey;

    /** The query message that carries the original TSIG record. */
    private Message queryMessage;

    /** The TSIG record extracted from {@link #queryMessage}. */
    private TSIGRecord queryTSIG;

    @BeforeAll
    static void setupBeforeAll() {
        /* Use the SHA‑256 algorithm (the most common choice) and a short,
         * base‑64‑compatible key.  The constructor validates the arguments,
         * so if this succeeds we know the key is usable for the tests. */
        tsigKey = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");
    }

    @BeforeEach
    void setupBeforeEach() throws IOException, TextParseException {
        /* Build a minimal query (A record for www.example.) and sign it.
         * The signed query is then parsed back so that we obtain a genuine
         * {@link TSIGRecord} instance that would be present on a real
         * request. */
        Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
        queryMessage = Message.newQuery(question);
        queryMessage.setTSIG(tsigKey);

        // Serialize and parse back to obtain the TSIGRecord produced by the library.
        byte[] wire = queryMessage.toWire(Message.MAXLENGTH);
        Message parsed = new Message(wire);
        queryTSIG = parsed.getTSIG();

        // Sanity check – the query must be signed for the subsequent tests.
        assertNotNull(queryTSIG, "The generated query should contain a TSIG record");
        assertTrue(parsed.isSigned(), "Parsed query should report as signed");
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release for these unit tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /**
     * Verify that {@code apply(..., true)} (full‑signature) adds a TSIG record
     * to the response and marks the message as signed.
     */
    @Test
    void applyAddsFullSignatureTSIG() throws IOException {
        // Create a response that mirrors the query ID and contains the original question.
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);

        // Apply the TSIG using the “full‑signature” flag.
        tsigKey.apply(response, queryTSIG, true);

        // The response must now be signed and contain a TSIG record.
        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTSIG = response.getTSIG();
        assertNotNull(respTSIG, "Response should contain a TSIG record");
    }

    /**
     * Verify that {@code apply(..., false)} (partial‑signature) also adds a
     * TSIG record and marks the message as signed.
     */
    @Test
    void applyAddsPartialSignatureTSIG() throws IOException {
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);

        // Apply the TSIG using the “partial‑signature” flag.
        tsigKey.apply(response, queryTSIG, false);

        assertTrue(response.isSigned(), "Response should be marked as signed");
        TSIGRecord respTSIG = response.getTSIG();
        assertNotNull(respTSIG, "Response should contain a TSIG record");
    }

    /**
     * Ensure that applying a TSIG with a {@code null} previous record does not
     * throw an exception and still signs the message.  The library treats a
     * missing old record as a “new” transaction.
     */
    @Test
    void applyWithNullOldRecordSignsMessage() throws IOException {
        Message response = new Message(queryMessage.getHeader().getID());
        response.addRecord(queryMessage.getQuestion(), Section.QUESTION);

        // The method overload that accepts a null old record is legal; we test the
        // three‑argument version directly with a null reference.
        tsigKey.apply(response, null, true);

        assertTrue(response.isSigned(), "Response should be signed even when old TSIG is null");
        assertNotNull(response.getTSIG(), "A TSIG record should be generated");
    }
}
```