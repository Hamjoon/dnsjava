```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#setTSIG(TSIG, int, TSIGRecord)} and its
 * one‑argument overload.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>basic storage of key, error and query TSIG record,
 *   <li>null handling for each parameter,
 *   <li>boundary values for the error code,
 *   <li>overwrites when the method is invoked repeatedly,
 *   <li>delegation of the {@code setTSIG(TSIG)} overload.
 * </ul>
 *
 * <p>Only the public API is used; private fields {@code tsigkey} and
 * {@code tsigerror} are accessed via reflection, as required by the task
 * description.
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_2_Test {

    private Message message;
    private TSIG tsigKey;               // reused for simple tests
    private TSIGRecord queryTsigRecord; // reused for simple tests

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Fresh Message for each test.
        message = new Message();

        // A generic TSIG key used by a few tests.
        Name alg = Name.fromString("hmac-md5.sig-alg.reg.int.");
        Name name = Name.fromString("example.com.");
        tsigKey = new TSIG(alg, name, "YWJjZGVmZ2hpamtsbW5vcA=="); // dummy base64 key

        // A generic TSIGRecord used by a few tests.
        Name recordName = Name.fromString("example.com.");
        int dclass = DClass.IN;
        long ttl = 0L;
        Instant now = Instant.now();
        Duration fudge = Duration.ofSeconds(300);
        byte[] signature = new byte[]{0x01, 0x02};
        int originalID = 0;
        int error = Rcode.NOERROR;
        byte[] other = new byte[0];

        queryTsigRecord = new TSIGRecord(
                recordName,
                dclass,
                ttl,
                alg,
                now,
                fudge,
                signature,
                originalID,
                error,
                other);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Returns the value of a private field from {@code Message} using reflection. */
    private static Object getPrivateField(Message msg, String fieldName) throws Exception {
        Field f = Message.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(msg);
    }

    /** Factory for distinct TSIG objects – the {@code id} suffix makes them unique. */
    private static TSIG createTSIG(String id) {
        Name alg = Name.fromString("hmac-md5.sig-alg.reg.int.");
        Name name = Name.fromString("key-" + id + ".example.com.");
        return new TSIG(alg, name, "YWJjZGVmZ2hpamtsbW5vcA==");
    }

    /** Factory for distinct TSIGRecord objects – the {@code id} suffix makes them unique. */
    private static TSIGRecord createTSIGRecord(String id) {
        Name recordName = Name.fromString("example.com.");
        int dclass = DClass.IN;
        long ttl = 0L;
        Name alg = Name.fromString("hmac-md5.sig-alg.reg.int.");
        Instant now = Instant.now();
        Duration fudge = Duration.ofSeconds(300);
        byte[] mac = ("mac-" + id).getBytes();
        int originalID = 0;
        int error = Rcode.NOERROR;
        byte[] other = new byte[0];
        return new TSIGRecord(
                recordName,
                dclass,
                ttl,
                alg,
                now,
                fudge,
                mac,
                originalID,
                error,
                other);
    }

    /* --------------------------------------------------------------------- */
    /* Parameterized tests covering the JSON test groups                    */
    /* --------------------------------------------------------------------- */

    /**
     * Provides arguments for the basic, null‑handling and error‑boundary groups.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>TSIG key (may be {@code null})
     *   <li>error code (int)
     *   <li>TSIGRecord query (may be {@code null})
     *   <li>description (used as test display name)
     * </ul>
     */
    private static Stream<Arguments> provideSetTSIGScenarios() {
        return Stream.of(
                // ----- setTSIG basic functionality -----
                Arguments.of(createTSIG("A"), 0, createTSIGRecord("X"),
                        "basic: non‑null key, error 0, non‑null query"),
                Arguments.of(createTSIG("B"), 9, createTSIGRecord("Y"),
                        "basic: non‑null key, error 9 (BADTIME), non‑null query"),

                // ----- setTSIG null handling -----
                Arguments.of(null, 0, null,
                        "null handling: null key, error 0, null query"),
                Arguments.of(null, 5, createTSIGRecord("Z"),
                        "null handling: null key, error 5, non‑null query"),
                Arguments.of(createTSIG("C"), 0, null,
                        "null handling: non‑null key, error 0, null query"),

                // ----- setTSIG error boundary values -----
                Arguments.of(createTSIG("D"), -1, createTSIGRecord("W"),
                        "boundary: negative error code"),
                Arguments.of(createTSIG("E"), Integer.MAX_VALUE, createTSIGRecord("V"),
                        "boundary: max int error code")
        );
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideSetTSIGScenarios")
    void setTSIG_variousScenarios(TSIG key, int error, TSIGRecord query, String _description) throws Exception {
        // WHEN: invoke the three‑argument overload
        message.setTSIG(key, error, query);

        // THEN: the private fields must match the supplied arguments
        assertSame(key, getPrivateField(message, "tsigkey"),
                "Message.tsigkey should be the key passed to setTSIG");
        assertEquals(error, getPrivateField(message, "tsigerror"),
                "Message.tsigerror should be the error passed to setTSIG");
        assertSame(query, message.getTSIG(),
                "Message.getTSIG() should return the query TSIGRecord passed to setTSIG");
    }

    /* --------------------------------------------------------------------- */
    /* Repeated invocations – ensure previous values are overwritten         */
    /* --------------------------------------------------------------------- */

    @Test
    void setTSIG_repeatedInvocations_overwritesPreviousValues() throws Exception {
        // First call – store initial values.
        TSIG firstKey = createTSIG("F");
        TSIGRecord firstRecord = createTSIGRecord("U");
        message.setTSIG(firstKey, 0, firstRecord);

        // Verify first state.
        assertSame(firstKey, getPrivateField(message, "tsigkey"));
        assertEquals(0, getPrivateField(message, "tsigerror"));
        assertSame(firstRecord, message.getTSIG());

        // Second call – new values should replace the old ones.
        TSIG secondKey = createTSIG("G");
        TSIGRecord secondRecord = createTSIGRecord("T");
        message.setTSIG(secondKey, 3, secondRecord);

        // Verify that the second invocation overwrote everything.
        assertSame(secondKey, getPrivateField(message, "tsigkey"),
                "tsigkey should be overwritten by the second call");
        assertEquals(3, getPrivateField(message, "tsigerror"),
                "tsigerror should be overwritten by the second call");
        assertSame(secondRecord, message.getTSIG(),
                "query TSIGRecord should be overwritten by the second call");
    }

    /* --------------------------------------------------------------------- */
    /* Existing explicit tests (kept unchanged)                              */
    /* --------------------------------------------------------------------- */

    @Test
    void setTSIG_withAllParameters_setsQueryTSIG() {
        // GIVEN a Message and a fully‑populated TSIGRecord
        // WHEN setTSIG is invoked with a key, error code and the TSIGRecord
        message.setTSIG(tsigKey, Rcode.NOERROR, queryTsigRecord);

        // THEN the Message should expose the same TSIGRecord via getTSIG()
        assertSame(queryTsigRecord, message.getTSIG(),
                "Message#getTSIG() should return the TSIGRecord supplied to setTSIG");
    }

    @Test
    void setTSIG_withNullQueryTSIG_clearsTSIGRecord() {
        // GIVEN a Message that already has a TSIGRecord attached
        message.setTSIG(tsigKey, Rcode.NOERROR, queryTsigRecord);
        assertNotNull(message.getTSIG(), "Pre‑condition: TSIGRecord should be present");

        // WHEN setTSIG is called with a null queryTSIG
        message.setTSIG(tsigKey, Rcode.NOERROR, null);

        // THEN getTSIG() must return null
        assertNull(message.getTSIG(),
                "Message#getTSIG() should be null after setTSIG is called with a null queryTSIG");
    }

    @Test
    void setTSIG_overload_delegatesToThreeArgVersion() {
        // The overload setTSIG(TSIG) should behave as if it called
        // setTSIG(key, Rcode.NOERROR, null).

        // WHEN the overload is used
        message.setTSIG(tsigKey);

        // THEN no TSIGRecord is attached
        assertNull(message.getTSIG(),
                "Message#getTSIG() should be null when the one‑arg overload is used");
    }
}
```