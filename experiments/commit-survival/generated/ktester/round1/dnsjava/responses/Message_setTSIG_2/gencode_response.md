```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xbill.DNS.utils.base64;

/**
 * Additional tests for {@link Message#setTSIG(TSIG, int, TSIGRecord)} that cover
 * the scenarios described in the JSON test‑case specification.
 *
 * <p>All assertions on the internal state use reflection because the fields
 * {@code tsigkey}, {@code tsigerror} and {@code querytsig} are private and have
 * no public getters.</p>
 */
class Message_setTSIG_2_Test {

    /** A simple TSIG key used for most tests. */
    private static TSIG tsigKey;

    /** A dummy TSIGRecord used as the “query TSIG” argument. */
    private static TSIGRecord queryTsigRecord;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // common objects used by a few test cases
        Name alg = Name.fromString("hmac-md5.sig-alg.reg.int.");
        Name name = Name.fromString("example.");
        String key = base64.fromString("abcd");
        tsigKey = new TSIG(alg, name, key);

        Name rrName = Name.fromString("example.");
        int dclass = DClass.IN;
        long ttl = 0;
        Instant now = Instant.now();
        Duration fudge = Duration.ofSeconds(300);
        byte[] signature = new byte[] {0x00};
        byte[] other = new byte[0];

        queryTsigRecord = new TSIGRecord(
                rrName,
                dclass,
                ttl,
                alg,
                now,
                fudge,
                signature,
                0,
                Rcode.NOERROR,
                other);
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Reflectively reads a private field from {@link Message}. */
    private static Object getPrivateField(Message msg, String fieldName) throws Exception {
        Field f = Message.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(msg);
    }

    /** Factory for a generic TSIG instance. */
    private static TSIG makeTSIG(String nameStr, String algStr, String keyStr) throws TextParseException {
        Name name = Name.fromString(nameStr);
        Name alg = Name.fromString(algStr);
        return new TSIG(alg, name, keyStr);
    }

    /** Factory for a generic TSIGRecord. The {@code tsigKey} argument may be null. */
    private static TSIGRecord makeTSIGRecord(String owner,
                                             int dclass,
                                             long ttl,
                                             TSIG tsigKey,
                                             int error,
                                             byte[] mac) throws TextParseException {
        Name name = Name.fromString(owner);
        Instant now = Instant.now();
        Duration fudge = Duration.ofSeconds(300);
        // The DNSjava constructor that accepts a TSIG key is:
        // TSIGRecord(Name name, int dclass, long ttl, TSIG key, int originalID,
        //            byte[] mac, int error, byte[] other)
        // For the purpose of these tests the exact values are irrelevant.
        return new TSIGRecord(name, dclass, ttl, tsigKey, 0, mac, error, new byte[0]);
    }

    /* --------------------------------------------------------------------- */
    /* Parameterized test sources                                            */
    /* --------------------------------------------------------------------- */

    /** Test data for the “basic functionality” group. */
    private static Stream<Arguments> basicFunctionalityProvider() throws Exception {
        return Stream.of(
                // case 1 – normal non‑null key and query TSIG
                Arguments.of(
                        makeTSIG("example.com.", "hmac-md5.", "base64key=="),
                        0,
                        makeTSIGRecord("example.com.", DClass.IN, 300,
                                makeTSIG("example.com.", "hmac-md5.", "base64key=="),
                                0, new byte[0]),
                        "normal non‑null values"
                ),
                // case 2 – null key, non‑null query TSIG
                Arguments.of(
                        null,
                        5,
                        makeTSIGRecord("example.org.", DClass.IN, 300,
                                null,
                                0, new byte[0]),
                        "null key, non‑null query"
                )
        );
    }

    /** Test data for the “null querytsig” group. */
    private static Stream<Arguments> nullQueryTsigProvider() throws Exception {
        return Stream.of(
                Arguments.of(
                        makeTSIG("example.net.", "hmac-sha256.", "c2VjcmV0a2V5=="),
                        0,
                        null,
                        "non‑null key, null query"
                )
        );
    }

    /** Test data for the “both key and querytsig null” group. */
    private static Stream<Arguments> bothNullProvider() {
        return Stream.of(
                Arguments.of(
                        null,
                        0,
                        null,
                        "both key and query null"
                )
        );
    }

    /** Test data for the “overwrite existing values” group. */
    private static Stream<Arguments> overwriteProvider() throws Exception {
        return Stream.of(
                // first call – baseline
                Arguments.of(
                        makeTSIG("first.example.", "hmac-md5.", "firstKey=="),
                        1,
                        makeTSIGRecord("first.example.", DClass.IN, 300,
                                makeTSIG("first.example.", "hmac-md5.", "firstKey=="),
                                0, new byte[0]),
                        "first set"
                ),
                // second call – different values
                Arguments.of(
                        makeTSIG("second.example.", "hmac-sha256.", "secondKey=="),
                        2,
                        makeTSIGRecord("second.example.", DClass.IN, 300,
                                makeTSIG("second.example.", "hmac-sha256.", "secondKey=="),
                                0, new byte[0]),
                        "second set (overwrite)"
                )
        );
    }

    /** Test data for the “error code boundaries” group. */
    private static Stream<Arguments> errorBoundaryProvider() throws Exception {
        return Stream.of(
                // negative error
                Arguments.of(
                        makeTSIG("boundary.example.", "hmac-md5.", "key=="),
                        -1,
                        makeTSIGRecord("boundary.example.", DClass.IN, 300,
                                makeTSIG("boundary.example.", "hmac-md5.", "key=="),
                                0, new byte[0]),
                        "negative error"
                ),
                // upper 16‑bit boundary
                Arguments.of(
                        makeTSIG("boundary.example.", "hmac-md5.", "key=="),
                        65535,
                        makeTSIGRecord("boundary.example.", DClass.IN, 300,
                                makeTSIG("boundary.example.", "hmac-md5.", "key=="),
                                0, new byte[0]),
                        "max unsigned 16‑bit error"
                )
        );
    }

    /** Test data for the “querytsig containing null TSIG key” group. */
    private static Stream<Arguments> queryTsigNullInnerKeyProvider() throws Exception {
        return Stream.of(
                Arguments.of(
                        makeTSIG("outer.example.", "hmac-md5.", "outerKey=="),
                        0,
                        makeTSIGRecord("outer.example.", DClass.IN, 300,
                                null,               // inner TSIG key is null
                                0, new byte[0]),
                        "query TSIG with internal null key"
                )
        );
    }

    /* --------------------------------------------------------------------- */
    /* Parameterized tests                                                   */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] basic functionality – {3}")
    @MethodSource("basicFunctionalityProvider")
    void setTSIG_basicFunctionality(TSIG key, int error, TSIGRecord query, String description) throws Exception {
        Message msg = new Message();
        msg.setTSIG(key, error, query);

        // verify internal state via reflection
        assertSame(key, getPrivateField(msg, "tsigkey"),
                "tsigkey should match the supplied key");
        assertEquals(error, getPrivateField(msg, "tsigerror"),
                "tsigerror should be stored verbatim");
        assertSame(query, getPrivateField(msg, "querytsig"),
                "querytsig reference should be stored unchanged");
    }

    @ParameterizedTest(name = "[{index}] null querytsig – {3}")
    @MethodSource("nullQueryTsigProvider")
    void setTSIG_nullQueryTsig(TSIG key, int error, TSIGRecord query, String description) throws Exception {
        Message msg = new Message();
        msg.setTSIG(key, error, query);

        assertSame(key, getPrivateField(msg, "tsigkey"));
        assertEquals(error, getPrivateField(msg, "tsigerror"));
        assertNull(getPrivateField(msg, "querytsig"),
                "querytsig should be null when null is passed");
    }

    @ParameterizedTest(name = "[{index}] both null – {3}")
    @MethodSource("bothNullProvider")
    void setTSIG_bothNull(TSIG key, int error, TSIGRecord query, String description) throws Exception {
        Message msg = new Message();
        msg.setTSIG(key, error, query);

        assertNull(getPrivateField(msg, "tsigkey"));
        assertEquals(error, getPrivateField(msg, "tsigerror"));
        assertNull(getPrivateField(msg, "querytsig"));
    }

    @ParameterizedTest(name = "[{index}] overwrite – {3}")
    @MethodSource("overwriteProvider")
    void setTSIG_overwrite(TSIG key, int error, TSIGRecord query, String description) throws Exception {
        Message msg = new Message();

        // The first call establishes a baseline; the second call (when this method
        // is invoked the second time) overwrites it.
        msg.setTSIG(key, error, query);

        assertSame(key, getPrivateField(msg, "tsigkey"));
        assertEquals(error, getPrivateField(msg, "tsigerror"));
        assertSame(query, getPrivateField(msg, "querytsig"));
    }

    @ParameterizedTest(name = "[{index}] error boundaries – {3}")
    @MethodSource("errorBoundaryProvider")
    void setTSIG_errorBoundaries(TSIG key, int error, TSIGRecord query, String description) throws Exception {
        Message msg = new Message();
        msg.setTSIG(key, error, query);

        assertSame(key, getPrivateField(msg, "tsigkey"));
        assertEquals(error, getPrivateField(msg, "tsigerror"));
        assertSame(query, getPrivateField(msg, "querytsig"));
    }

    @ParameterizedTest(name = "[{index}] querytsig with null inner key – {3}")
    @MethodSource("queryTsigNullInnerKeyProvider")
    void setTSIG_queryTsigWithNullInnerKey(TSIG key, int error, TSIGRecord query, String description) throws Exception {
        Message msg = new Message();
        msg.setTSIG(key, error, query);

        assertSame(key, getPrivateField(msg, "tsigkey"));
        assertEquals(error, getPrivateField(msg, "tsigerror"));
        TSIGRecord storedQuery = (TSIGRecord) getPrivateField(msg, "querytsig");
        assertSame(query, storedQuery, "querytsig reference must be stored even if its inner key is null");
        // No public API to inspect the inner TSIG key, so we rely on reference equality.
    }

    /* --------------------------------------------------------------------- */
    /* Existing non‑parameterized tests (kept unchanged)                      */
    /* --------------------------------------------------------------------- */

    @Test
    void setTSIG_allParameters_setsFieldsWithoutSideEffects() {
        Message msg = new Message();               // fresh message, empty sections
        int errorCode = Rcode.NOERROR;             // typical “no error” value

        // Act – call the method under test
        msg.setTSIG(tsigKey, errorCode, queryTsigRecord);

        // Assert – the call must not have altered the header or sections.
        assertNotNull(msg.getHeader(), "Header should be present after construction");
        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(),
                    "Section " + i + " should remain empty after setTSIG");
        }
        assertNull(msg.getTSIG(), "Generated TSIGRecord should be null after setTSIG");
    }

    @Test
    void setTSIG_overload_delegatesToFullMethod() {
        Message msg = new Message();

        // The overload simply forwards to setTSIG(key, Rcode.NOERROR, null)
        assertDoesNotThrow(() -> msg.setTSIG(tsigKey));

        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(),
                    "Section " + i + " should remain empty after overload setTSIG");
        }
        assertNull(msg.getTSIG(), "Generated TSIGRecord should still be null after overload setTSIG");
    }

    @Test
    void setTSIG_canBeCalledMultipleTimesWithDifferentKeys() {
        Message msg = new Message();

        // First key
        TSIG firstKey = tsigKey;
        // Second key – create a distinct key to ensure the method accepts a new instance.
        TSIG secondKey = new TSIG(Name.fromString("hmac-sha256."), Name.fromString("other.example."), "wxyz");

        // First call
        msg.setTSIG(firstKey, Rcode.NOERROR, queryTsigRecord);
        // Second call with a different key and a different error code
        msg.setTSIG(secondKey, Rcode.BADSIG, null);

        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(),
                    "Section " + i + " should remain empty after multiple setTSIG calls");
        }
        assertNull(msg.getTSIG(),
                "Generated TSIGRecord should remain null after multiple setTSIG calls");
    }
}
```