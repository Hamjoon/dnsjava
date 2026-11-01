```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#getGeneratedTSIG()} covering basic behaviour,
 * interaction with the {@code setTSIG} mutators, cloning, error handling and
 * type‑safety guarantees.
 *
 * The test suite follows the JSON description supplied with the task and
 * uses reflection only where the API does not expose the required behaviour
 * (e.g. injecting a mock {@code TSIGRecord} directly into the private field).
 */
@ExtendWith(MockitoExtension.class)
class Message_getGeneratedTSIG_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh Message for every test case.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /* --------------------------------------------------------------------- */
    /*  Basic behaviour                                                     */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that a newly‑created Message has no generated TSIG record.
     */
    @Test
    void getGeneratedTSIG_returnsNullWhenNotSet() {
        assertNull(message.getGeneratedTSIG(),
                "A Message that has never been signed should return null from getGeneratedTSIG()");
    }

    /**
     * Using reflection to inject a mock {@link TSIGRecord} into the private
     * {@code generatedTsig} field should cause {@code getGeneratedTSIG()} to
     * return the exact same instance, confirming that the getter simply
     * returns the field without alteration.
     */
    @Test
    void getGeneratedTSIG_reflectionInjection_returnsSameInstance() throws Exception {
        // Arrange: create a dummy TSIGRecord (using the public constructor)
        Name name = Name.fromConstantString("inject.");
        TSIGRecord dummy = new TSIGRecord(name, DClass.IN, 0, name, Rcode.NOERROR, null, null);

        // Inject via reflection
        Field f = Message.class.getDeclaredField("generatedTsig");
        f.setAccessible(true);
        f.set(message, dummy);

        // Act & Assert
        TSIGRecord result = message.getGeneratedTSIG();
        assertSame(dummy, result, "Injected TSIGRecord should be returned unchanged");
    }

    /* --------------------------------------------------------------------- */
    /*  setTSIG() – normal usage                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void getGeneratedTSIG_returnsRecordAfterSetTSIG() throws Exception {
        // Arrange – create a TSIG key (algorithm “hmac-md5” is the default)
        Name keyName = Name.fromConstantString("testkey.");
        String secret = Base64.getEncoder().encodeToString("abcdefghijklmnop".getBytes());
        TSIG tsig = new TSIG(keyName, secret);

        // Act – associate the TSIG with the message
        message.setTSIG(tsig);

        // Assert – a TSIGRecord should now be present
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should not be null after setTSIG");
        assertEquals(tsig.getName(), generated.getName(),
                "The generated TSIGRecord should carry the same name as the TSIG key");
    }

    @Test
    void getGeneratedTSIG_afterSetTSIGWithErrorAndQuery() throws Exception {
        // Arrange
        Name keyName = Name.fromConstantString("anotherkey.");
        String secret = Base64.getEncoder().encodeToString("secretsecret".getBytes());
        TSIG tsig = new TSIG(keyName, secret);
        int error = Rcode.NOERROR; // no error

        // Act
        message.setTSIG(tsig, error, null);

        // Assert
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be present after setTSIG with error code");
        assertEquals(keyName, generated.getName(),
                "Generated TSIGRecord should retain the TSIG key name");
        // Verify that the error field (if accessible) matches the supplied code
        // The TSIGRecord class exposes getError() – if not, we fall back to reflection.
        try {
            assertEquals(error, generated.getError(),
                    "Error code stored in generated TSIGRecord should match the supplied value");
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            // Fallback: reflectively read the private 'error' field
            Field errField = TSIGRecord.class.getDeclaredField("error");
            errField.setAccessible(true);
            int actual = (int) errField.get(generated);
            assertEquals(error, actual,
                    "Error code stored in generated TSIGRecord (via reflection) should match");
        }
    }

    /* --------------------------------------------------------------------- */
    /*  setTSIG() – error code variations                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void getGeneratedTSIG_afterSetTSIGWithBadTimeError() throws Exception {
        Name keyName = Name.fromConstantString("badtime.");
        String secret = Base64.getEncoder().encodeToString("badtime".getBytes());
        TSIG tsig = new TSIG(keyName, secret);
        int error = Rcode.BADTIME; // error code 1

        message.setTSIG(tsig, error, null);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should not be null even when error code is BADTIME");
        // Verify error code
        try {
            assertEquals(error, generated.getError(),
                    "Generated TSIGRecord should contain BADTIME error code");
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            Field errField = TSIGRecord.class.getDeclaredField("error");
            errField.setAccessible(true);
            int actual = (int) errField.get(generated);
            assertEquals(error, actual, "Error field should be BADTIME");
        }
    }

    @Test
    void getGeneratedTSIG_afterSetTSIGWithNoError() throws Exception {
        Name keyName = Name.fromConstantString("noerror.");
        String secret = Base64.getEncoder().encodeToString("noerror".getBytes());
        TSIG tsig = new TSIG(keyName, secret);
        int error = Rcode.NOERROR; // error code 0

        message.setTSIG(tsig, error, null);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be present for NOERROR");
        try {
            assertEquals(error, generated.getError(),
                    "Generated TSIGRecord should contain NOERROR error code");
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            Field errField = TSIGRecord.class.getDeclaredField("error");
            errField.setAccessible(true);
            int actual = (int) errField.get(generated);
            assertEquals(error, actual, "Error field should be NOERROR");
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Cloning behaviour                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void getGeneratedTSIG_afterCloningSignedMessage_returnsSameReference() throws Exception {
        // Arrange – sign the message
        Name keyName = Name.fromConstantString("clonekey.");
        String secret = Base64.getEncoder().encodeToString("cloneSecret".getBytes());
        TSIG tsig = new TSIG(keyName, secret);
        message.setTSIG(tsig);
        TSIGRecord originalGenerated = message.getGeneratedTSIG();
        assertNotNull(originalGenerated, "Pre‑condition: generated TSIG must be non‑null before cloning");

        // Act – clone
        Message cloned = message.clone();

        // Assert – reference equality of generated TSIG
        TSIGRecord clonedGenerated = cloned.getGeneratedTSIG();
        assertSame(originalGenerated, clonedGenerated,
                "Cloned Message should contain the exact same TSIGRecord instance");
    }

    @Test
    void getGeneratedTSIG_afterCloningUnsignedMessage_returnsNull() throws Exception {
        // No TSIG set on original message
        Message cloned = message.clone();
        assertNull(cloned.getGeneratedTSIG(),
                "Cloned Message of an unsigned Message should also return null for getGeneratedTSIG()");
    }

    /* --------------------------------------------------------------------- */
    /*  Resetting TSIG                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    void getGeneratedTSIG_afterResettingTSIGWithNull_returnsNull() throws Exception {
        // Set a TSIG first
        Name keyName = Name.fromConstantString("resetkey.");
        String secret = Base64.getEncoder().encodeToString("reset".getBytes());
        TSIG tsig = new TSIG(keyName, secret);
        message.setTSIG(tsig);
        assertNotNull(message.getGeneratedTSIG(), "Pre‑condition: generated TSIG must be present");

        // Reset with null – API specifies that passing null clears the TSIG
        message.setTSIG((TSIG) null);
        assertNull(message.getGeneratedTSIG(),
                "After calling setTSIG(null) the generated TSIG should be cleared (null)");
    }

    @Test
    void getGeneratedTSIG_setTSIGNullWithError_throwsException() {
        // The overload that takes (TSIG, int, TSIGRecord) should reject a null key.
        assertThrows(NullPointerException.class, () -> {
            message.setTSIG(null, Rcode.NOERROR, null);
        }, "Calling setTSIG(null, ...) should throw NullPointerException (or IllegalArgumentException)");
    }

    /* --------------------------------------------------------------------- */
    /*  Multiple setTSIG invocations                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void getGeneratedTSIG_multipleSetTSIG_overwritesPrevious() throws Exception {
        // First key
        Name firstName = Name.fromConstantString("firstkey.");
        TSIG firstTsig = new TSIG(firstName, Base64.getEncoder().encodeToString("first".getBytes()));
        message.setTSIG(firstTsig);
        TSIGRecord afterFirst = message.getGeneratedTSIG();
        assertEquals(firstName, afterFirst.getName(),
                "Generated TSIG after first setTSIG should match first key");

        // Second key – should overwrite
        Name secondName = Name.fromConstantString("secondkey.");
        TSIG secondTsig = new TSIG(secondName, Base64.getEncoder().encodeToString("second".getBytes()));
        message.setTSIG(secondTsig);
        TSIGRecord afterSecond = message.getGeneratedTSIG();
        assertEquals(secondName, afterSecond.getName(),
                "Generated TSIG after second setTSIG should match second key");
        assertNotSame(afterFirst, afterSecond,
                "The TSIGRecord instance should be replaced by the second call");
    }

    @Test
    void getGeneratedTSIG_multipleSetTSIGWithErrors_lastErrorWins() throws Exception {
        // First call – NOERROR
        Name firstName = Name.fromConstantString("firsterr.");
        TSIG firstTsig = new TSIG(firstName, Base64.getEncoder().encodeToString("first".getBytes()));
        message.setTSIG(firstTsig, Rcode.NOERROR, null);
        TSIGRecord afterFirst = message.getGeneratedTSIG();

        // Second call – BADKEY (error code 2)
        Name secondName = Name.fromConstantString("seconderr.");
        TSIG secondTsig = new TSIG(secondName, Base64.getEncoder().encodeToString("second".getBytes()));
        int errorCode = Rcode.BADKEY; // 2
        message.setTSIG(secondTsig, errorCode, null);
        TSIGRecord afterSecond = message.getGeneratedTSIG();

        assertEquals(secondName, afterSecond.getName(),
                "Generated TSIG should reflect the second key after overwrite");
        // Verify error code of the latest record
        try {
            assertEquals(errorCode, afterSecond.getError(),
                    "Error code of generated TSIG should be from the last setTSIG call");
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            Field errField = TSIGRecord.class.getDeclaredField("error");
            errField.setAccessible(true);
            int actual = (int) errField.get(afterSecond);
            assertEquals(errorCode, actual, "Error field should be BADKEY");
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Type‑safety guard                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void getGeneratedTSIG_typeSafetyViolation_throwsClassCastException() throws Exception {
        // Inject a wrong type (String) into the private field via reflection
        Field f = Message.class.getDeclaredField("generatedTsig");
        f.setAccessible(true);
        f.set(message, "I am not a TSIGRecord");

        // The getter returns Object typed as TSIGRecord, so a ClassCastException
        // should be thrown when the method attempts to return the field.
        assertThrows(ClassCastException.class, () -> {
            message.getGeneratedTSIG();
        }, "If generatedTsig holds an incompatible type, getGeneratedTSIG should throw ClassCastException");
    }
}
```