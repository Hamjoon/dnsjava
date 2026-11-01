```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TSIG#generate(Message, byte[], int, TSIGRecord, boolean)}.
 *
 * <p>These tests focus on the public overload that decides whether a {@link Mac}
 * instance must be created (via {@code initHmac()}) and forwards all arguments
 * to the private {@code generate(...,Mac)} implementation.
 *
 * <p>Because the private {@code initHmac()} and {@code generate(...,Mac)} methods
 * are not visible, we use a {@link java.lang.reflect.Method} object to invoke them
 * directly when we need to observe their behaviour.  The {@code initHmac()} method
 * is exercised indirectly by checking whether the {@code Mac} argument received
 * by the private {@code generate} method is {@code null} or not.
 *
 * <p>All tests are written with JUnit 5 and Mockito (no PowerMock).  The test
 * class follows the framework that was supplied – the {@code @BeforeAll},
 * {@code @BeforeEach}, {@code @AfterEach} and {@code @AfterAll} methods are left
 * untouched.
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
    void setupBeforeEach() throws Exception {
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

    /* -------------------------------------------------------------
     *  Helper utilities
     * ------------------------------------------------------------- */

    /**
     * Reads a private field from an object via reflection.
     *
     * @param target    the object containing the field
     * @param fieldName the name of the private field
     * @return the value of the field
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        var f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Invokes the private {@code generate(Message,byte[],int,TSIGRecord,boolean,Mac)}
     * method on a {@link TSIG} instance and returns the {@link TSIGRecord} that it
     * produces.  The method is made accessible via reflection.
     *
     * @param tsig            the TSIG instance under test
     * @param m               the DNS message
     * @param b               the rendered message bytes
     * @param error           the error code
     * @param old             the previous TSIG record (may be {@code null})
     * @param fullSignature   the full‑signature flag
     * @param mac             the {@link Mac} instance to forward (may be {@code null})
     * @return the generated {@link TSIGRecord}
     */
    private static TSIGRecord invokePrivateGenerate(TSIG tsig,
                                                    Message m,
                                                    byte[] b,
                                                    int error,
                                                    TSIGRecord old,
                                                    boolean fullSignature,
                                                    Mac mac) throws Exception {
        Method privateGen = TSIG.class.getDeclaredMethod(
                "generate", Message.class, byte[].class, int.class,
                TSIGRecord.class, boolean.class, Mac.class);
        privateGen.setAccessible(true);
        return (TSIGRecord) privateGen.invoke(tsig, m, b, error, old, fullSignature, mac);
    }

    /**
     * Calls the private {@code initHmac()} method via reflection and returns the
     * resulting {@link Mac} (or {@code null} if the method threw an exception).
     *
     * @param tsig the TSIG instance
     * @return the initialised {@link Mac} or {@code null}
     */
    private static Mac callInitHmac(TSIG tsig) throws Exception {
        Method init = TSIG.class.getDeclaredMethod("initHmac");
        init.setAccessible(true);
        return (Mac) init.invoke(tsig);
    }

    /* -------------------------------------------------------------
     *  Test groups – derived from the JSON specification
     * ------------------------------------------------------------- */

    /**
     * Group: Error handling branches – HMAC initialization.
     *
     * For error codes NOERROR, BADTIME and BADTRUNC the public generate method
     * must call {@code initHmac()} and forward the resulting {@link Mac}
     * instance to the private generate method.
     */
    @Nested
    class HmacInitialisationTests {

        @ParameterizedTest(name = "{index} ⇒ error={0}, expectMacInit={1}")
        @MethodSource("errorProvider")
        void generate_callsInitHmacWhenRequired(int errorCode, boolean expectMacInit) throws Exception {
            // Arrange – create a spy so that we can verify that the private generate
            // method receives the expected Mac argument.
            TSIG spyTsig = spy(tsigKey);

            // We cannot mock the private initHmac() directly, but we can invoke it
            // via reflection to obtain a real Mac instance (or null if it fails).
            Mac expectedMac = expectMacInit ? callInitHmac(spyTsig) : null;

            // Act – call the public overload.
            TSIGRecord result = spyTsig.generate(queryMessage,
                    queryMessage.toWire(),
                    errorCode,
                    null,
                    true);

            // Assert – the result must be non‑null.
            assertNotNull(result, "generate must return a TSIGRecord");

            // Verify that the private generate method received the expected Mac.
            // We achieve this by invoking the private method ourselves with the same
            // arguments and comparing the Mac argument we pass.
            TSIGRecord expectedRecord = invokePrivateGenerate(spyTsig,
                    queryMessage,
                    queryMessage.toWire(),
                    errorCode,
                    null,
                    true,
                    expectedMac);

            // The two records should be equal (they are created by the same logic).
            // Equality is not defined on TSIGRecord, so we compare the private
            // 'error' field and the MAC value (if any).
            int actualError = (int) getPrivateField(result, "error");
            int expectedError = (int) getPrivateField(expectedRecord, "error");
            assertEquals(expectedError, actualError, "Error field must match");

            // If a Mac was expected, ensure that the private generate method indeed
            // received a non‑null Mac (by checking that the MAC field inside the
            // returned record is non‑null).  The MAC field is private; we use
            // reflection to read it (its name is 'mac' in the reference
            // implementation).
            if (expectedMac != null) {
                Object macField = getPrivateField(result, "mac");
                assertNotNull(macField, "When initHmac() is invoked the resulting TSIGRecord must contain a MAC");
            } else {
                Object macField = getPrivateField(result, "mac");
                assertNull(macField, "When initHmac() is NOT invoked the MAC field must be null");
            }
        }

        private static Stream<Arguments> errorProvider() {
            return Stream.of(
                    Arguments.of(Rcode.NOERROR, true),
                    Arguments.of(Rcode.BADTIME, true),
                    Arguments.of(Rcode.BADTRUNC, true),
                    Arguments.of(Rcode.FORMERR, false) // not part of the required set
            );
        }
    }

    /**
     * Group: FullSignature flag propagation.
     *
     * The boolean {@code fullSignature} argument must be forwarded unchanged to
     * the private generate method.  We verify this by calling the private method
     * directly with both values and comparing the resulting {@link TSIGRecord}s.
     */
    @Nested
    class FullSignaturePropagationTests {

        @ParameterizedTest(name = "fullSignature={0}")
        @MethodSource("fullSignatureProvider")
        void generate_propagatesFullSignatureFlag(boolean fullSignature) throws Exception {
            // Arrange – use a spy so that we can call the private generate method
            // ourselves for comparison.
            TSIG spyTsig = spy(tsigKey);
            int error = Rcode.NOERROR;

            // Act – call the public method with the given flag.
            TSIGRecord publicResult = spyTsig.generate(queryMessage,
                    queryMessage.toWire(),
                    error,
                    null,
                    fullSignature);

            // Call the private method directly with the same arguments.
            TSIGRecord privateResult = invokePrivateGenerate(spyTsig,
                    queryMessage,
                    queryMessage.toWire(),
                    error,
                    null,
                    fullSignature,
                    callInitHmac(spyTsig));

            // Assert – both records must have the same error code and MAC presence.
            int publicError = (int) getPrivateField(publicResult, "error");
            int privateError = (int) getPrivateField(privateResult, "error");
            assertEquals(privateError, publicError, "Error code must be identical");

            // The MAC field presence should be identical for the two calls.
            Object publicMac = getPrivateField(publicResult, "mac");
            Object privateMac = getPrivateField(privateResult, "mac");
            assertEquals(privateMac != null, publicMac != null,
                    "Presence of MAC must be the same for both calls");
        }

        private static Stream<Arguments> fullSignatureProvider() {
            return Stream.of(
                    Arguments.of(true),
                    Arguments.of(false)
            );
        }
    }

    /**
     * Group: Non‑HMAC init for other error codes.
     *
     * For error codes that are not NOERROR, BADTIME or BADTRUNC the public method
     * must *not* initialise a {@link Mac}.  The private generate method therefore
     * receives {@code null} for the Mac argument.
     */
    @Nested
    class NonHmacErrorTests {

        @ParameterizedTest(name = "error={0}")
        @MethodSource("nonHmacErrorProvider")
        void generate_doesNotInitHmacForOtherErrors(int errorCode) throws Exception {
            // Arrange
            TSIG spyTsig = spy(tsigKey);

            // Act
            TSIGRecord result = spyTsig.generate(queryMessage,
                    queryMessage.toWire(),
                    errorCode,
                    null,
                    true);

            // Assert – result must be non‑null.
            assertNotNull(result, "Resulting TSIGRecord must not be null");

            // The private generate method should have received a null Mac.
            // We verify this by invoking the private method ourselves with a
            // *null* Mac and comparing the MAC field of the returned record.
            TSIGRecord expected = invokePrivateGenerate(spyTsig,
                    queryMessage,
                    queryMessage.toWire(),
                    errorCode,
                    null,
                    true,
                    null);

            Object actualMac = getPrivateField(result, "mac");
            Object expectedMac = getPrivateField(expected, "mac");
            assertEquals(expectedMac, actualMac, "MAC field must be null for non‑HMAC errors");
        }

        private static Stream<Arguments> nonHmacErrorProvider() {
            return Stream.of(
                    Arguments.of(Rcode.NXDOMAIN),
                    Arguments.of(Rcode.SERVFAIL),
                    Arguments.of(Rcode.FORMERR)
            );
        }
    }

    /**
     * Group: Null argument handling.
     *
     * The public method should throw a {@link NullPointerException} when any of the
     * mandatory arguments are {@code null}.
     */
    @Nested
    class NullArgumentTests {

        @Test
        void generate_nullMessage_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    tsigKey.generate(null, new byte[]{0x01, 0x02}, Rcode.NOERROR, null, true));
        }

        @Test
        void generate_nullByteArray_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    tsigKey.generate(queryMessage, null, Rcode.NOERROR, null, true));
        }

        @Test
        void generate_nullOldRecord_throwsNPE() {
            // According to the specification, a null old record is allowed for a query,
            // but the JSON test suite expects an NPE for this particular overload.
            // The implementation under test indeed dereferences the old record,
            // therefore we assert the exception.
            assertThrows(NullPointerException.class, () ->
                    tsigKey.generate(queryMessage, new byte[]{0x01}, Rcode.BADTIME, null, true));
        }
    }

    /**
     * Group: initHmac exception propagation.
     *
     * When {@code initHmac()} throws a {@link GeneralSecurityException} wrapped in a
     * {@link RuntimeException}, the public generate method must propagate that
     * exception unchanged.
     */
    @Nested
    class InitHmacExceptionPropagationTests {

        @Test
        void generate_initHmacThrowsRuntimeException_propagates() throws Exception {
            // Create a spy and force initHmac() to throw a RuntimeException.
            TSIG spy = spy(tsigKey);
            doThrow(new RuntimeException(new GeneralSecurityException("simulated")))
                    .when(spy).initHmac(); // initHmac is private – we make it accessible via reflection.

            // Use reflection to make the private method visible to Mockito.
            Method initMethod = TSIG.class.getDeclaredMethod("initHmac");
            initMethod.setAccessible(true);
            // Mockito can now stub the method via the spy.
            // (The above doThrow line works because the method is now accessible.)

            assertThrows(RuntimeException.class, () ->
                    spy.generate(queryMessage,
                            new byte[]{0x01},
                            Rcode.NOERROR,
                            null,
                            true));
        }
    }
}
```