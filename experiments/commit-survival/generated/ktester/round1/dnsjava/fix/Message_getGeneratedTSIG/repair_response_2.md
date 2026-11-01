```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Base64;

/**
 * Unit tests for {@link Message#getGeneratedTSIG()}.
 *
 * <p>The original tests assumed that {@code setTSIG(...)} immediately creates a
 * {@link TSIGRecord}. In dnsjava the generated record is only populated after the
 * message is converted to wire format via {@link Message#toWire(int)}.  The
 * tests have therefore been adjusted to invoke {@code toWire(...)} where
 * appropriate.</p>
 *
 * <p>Some behavioural differences have also been accommodated:
 * <ul>
 *   <li>{@code setTSIG(null, …)} does not throw – it simply clears the TSIG.</li>
 *   <li>Attempting to inject a value of the wrong type into the private
 *       {@code generatedTsig} field via reflection throws an
 *       {@link IllegalArgumentException} (the JVM enforces the field type).</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class Message_getGeneratedTSIG_Test {

    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        message = null;
    }

    /** A freshly‑created Message should have no generated TSIG. */
    @Test
    void getGeneratedTSIG_returnsNullWhenNotSet() {
        assertNull(message.getGeneratedTSIG(),
                "A Message that has never been signed should return null from getGeneratedTSIG()");
    }

    /** After signing and converting to wire format the generated TSIG should be present. */
    @Test
    void getGeneratedTSIG_returnsRecordAfterSetTSIG() throws Exception {
        Name keyName = Name.fromConstantString("testkey.");
        byte[] secret = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("abcdefghijklmnop".getBytes()));
        TSIG tsig = new TSIG(keyName, secret);

        message.setTSIG(tsig);
        // The generated TSIG is only created after the message is rendered to wire.
        message.toWire(Message.MAXLENGTH);

        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should not be null after setTSIG + toWire");
        assertEquals(keyName, generated.getName(),
                "The generated TSIGRecord should carry the same name as the TSIG key");
    }

    /** Verify that the overload {@code setTSIG(TSIG, int, TSIGRecord)} also populates the field. */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithErrorAndQuery() throws Exception {
        Name keyName = Name.fromConstantString("anotherkey.");
        byte[] secret = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("secretsecret".getBytes()));
        TSIG tsig = new TSIG(keyName, secret);
        int error = Rcode.NOERROR;

        message.setTSIG(tsig, error, null);
        // Render to wire to trigger generation.
        message.toWire(Message.MAXLENGTH);

        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be present after setTSIG with error code");
        assertEquals(keyName, generated.getName(),
                "Generated TSIGRecord should retain the TSIG key name");

        // Verify the stored error code (via accessor or reflection).
        try {
            assertEquals(error, generated.getError(),
                    "Error code stored in generated TSIGRecord should match the supplied value");
        } catch (NoSuchMethodError e) {
            Field errField = TSIGRecord.class.getDeclaredField("error");
            errField.setAccessible(true);
            int actual = (int) errField.get(generated);
            assertEquals(error, actual,
                    "Error code stored in generated TSIGRecord (via reflection) should match");
        }
    }

    /** Using reflection to inject a mock {@link TSIGRecord} should be returned unchanged. */
    @Test
    void getGeneratedTSIG_reflectionInjection_returnsSameInstance() throws Exception {
        TSIGRecord dummy = new TSIGRecord();
        Field f = Message.class.getDeclaredField("generatedTsig");
        f.setAccessible(true);
        f.set(message, dummy);

        TSIGRecord result = message.getGeneratedTSIG();
        assertSame(dummy, result,
                "Injected TSIGRecord should be returned unchanged");
    }

    /** Verify that an error code (BADTIME) is recorded after rendering to wire. */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithBadTimeError() throws Exception {
        Name keyName = Name.fromConstantString("badtime.");
        byte[] secret = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("badtime".getBytes()));
        TSIG tsig = new TSIG(keyName, secret);
        int error = Rcode.BADTIME;

        message.setTSIG(tsig, error, null);
        message.toWire(Message.MAXLENGTH);

        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated,
                "Generated TSIG should not be null even when error code is BADTIME");

        try {
            assertEquals(error, generated.getError(),
                    "Generated TSIGRecord should contain BADTIME error code");
        } catch (NoSuchMethodError e) {
            Field errField = TSIGRecord.class.getDeclaredField("error");
            errField.setAccessible(true);
            int actual = (int) errField.get(generated);
            assertEquals(error, actual, "Error field should be BADTIME");
        }
    }

    /** Verify that NOERROR is also recorded. */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithNoError() throws Exception {
        Name keyName = Name.fromConstantString("noerror.");
        byte[] secret = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("noerror".getBytes()));
        TSIG tsig = new TSIG(keyName, secret);
        int error = Rcode.NOERROR;

        message.setTSIG(tsig, error, null);
        message.toWire(Message.MAXLENGTH);

        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated,
                "Generated TSIG should be present for NOERROR");

        try {
            assertEquals(error, generated.getError(),
                    "Generated TSIGRecord should contain NOERROR error code");
        } catch (NoSuchMethodError e) {
            Field errField = TSIGRecord.class.getDeclaredField("error");
            errField.setAccessible(true);
            int actual = (int) errField.get(generated);
            assertEquals(error, actual, "Error field should be NOERROR");
        }
    }

    /** Cloning a signed message should preserve the same TSIGRecord instance. */
    @Test
    void getGeneratedTSIG_afterCloningSignedMessage_returnsSameReference() throws Exception {
        Name keyName = Name.fromConstantString("clonekey.");
        byte[] secret = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("cloneSecret".getBytes()));
        TSIG tsig = new TSIG(keyName, secret);

        message.setTSIG(tsig);
        message.toWire(Message.MAXLENGTH);
        TSIGRecord originalGenerated = message.getGeneratedTSIG();
        assertNotNull(originalGenerated,
                "Pre‑condition: generated TSIG must be non‑null before cloning");

        Message cloned = message.clone();
        TSIGRecord clonedGenerated = cloned.getGeneratedTSIG();
        assertSame(originalGenerated, clonedGenerated,
                "Cloned Message should contain the exact same TSIGRecord instance");
    }

    /** Cloning an unsigned message should still have no generated TSIG. */
    @Test
    void getGeneratedTSIG_afterCloningUnsignedMessage_returnsNull() throws Exception {
        Message cloned = message.clone();
        assertNull(cloned.getGeneratedTSIG(),
                "Cloned Message of an unsigned Message should also return null for getGeneratedTSIG()");
    }

    /** Resetting the TSIG with {@code null} should clear the generated record. */
    @Test
    void getGeneratedTSIG_afterResettingTSIGWithNull_returnsNull() throws Exception {
        Name keyName = Name.fromConstantString("resetkey.");
        byte[] secret = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("reset".getBytes()));
        TSIG tsig = new TSIG(keyName, secret);

        message.setTSIG(tsig);
        message.toWire(Message.MAXLENGTH);
        assertNotNull(message.getGeneratedTSIG(),
                "Pre‑condition: generated TSIG must be present");

        // Reset with null – the API specifies that passing null clears the TSIG.
        message.setTSIG((TSIG) null);
        assertNull(message.getGeneratedTSIG(),
                "After calling setTSIG(null) the generated TSIG should be cleared (null)");
    }

    /** The overload that takes (TSIG, int, TSIGRecord) should accept a null TSIG and simply clear. */
    @Test
    void getGeneratedTSIG_setTSIGNullWithError_doesNotThrow() {
        // No exception is expected; the method should behave like setTSIG(null).
        assertDoesNotThrow(() -> message.setTSIG(null, Rcode.NOERROR, null),
                "Calling setTSIG(null, ...) should not throw an exception");
        assertNull(message.getGeneratedTSIG(),
                "Generated TSIG should be null after setTSIG(null, …)");
    }

    /** Multiple calls to setTSIG should overwrite the previous record. */
    @Test
    void getGeneratedTSIG_multipleSetTSIG_overwritesPrevious() throws Exception {
        // First key
        Name firstName = Name.fromConstantString("firstkey.");
        byte[] secret = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("first".getBytes()));
        TSIG firstTsig = new TSIG(firstName, secret);
        message.setTSIG(firstTsig);
        message.toWire(Message.MAXLENGTH);
        TSIGRecord afterFirst = message.getGeneratedTSIG();
        assertEquals(firstName, afterFirst.getName(),
                "Generated TSIG after first setTSIG should match first key");

        // Second key – should overwrite
        Name secondName = Name.fromConstantString("secondkey.");
        byte[] secretSecond = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("second".getBytes()));
        TSIG secondTsig = new TSIG(secondName, secretSecond);
        message.setTSIG(secondTsig);
        message.toWire(Message.MAXLENGTH);
        TSIGRecord afterSecond = message.getGeneratedTSIG();
        assertEquals(secondName, afterSecond.getName(),
                "Generated TSIG after second setTSIG should match second key");
        assertNotSame(afterFirst, afterSecond,
                "The TSIGRecord instance should be replaced by the second call");
    }

    /** The last error code supplied should be stored in the generated record. */
    @Test
    void getGeneratedTSIG_multipleSetTSIGWithErrors_lastErrorWins() throws Exception {
        // First call – NOERROR
        Name firstName = Name.fromConstantString("firsterr.");
        byte[] secret = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("first".getBytes()));
        TSIG firstTsig = new TSIG(firstName, secret);
        message.setTSIG(firstTsig, Rcode.NOERROR, null);
        message.toWire(Message.MAXLENGTH);
        TSIGRecord afterFirst = message.getGeneratedTSIG();

        // Second call – BADKEY (error code 2)
        Name secondName = Name.fromConstantString("seconderr.");
        byte[] secretSecond = Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString("second".getBytes()));
        TSIG secondTsig = new TSIG(secondName, secretSecond);
        int errorCode = Rcode.BADKEY;
        message.setTSIG(secondTsig, errorCode, null);
        message.toWire(Message.MAXLENGTH);
        TSIGRecord afterSecond = message.getGeneratedTSIG();

        assertEquals(secondName, afterSecond.getName(),
                "Generated TSIG should reflect the second key after overwrite");

        try {
            assertEquals(errorCode, afterSecond.getError(),
                    "Error code of generated TSIG should be from the last setTSIG call");
        } catch (NoSuchMethodError e) {
            Field errField = TSIGRecord.class.getDeclaredField("error");
            errField.setAccessible(true);
            int actual = (int) errField.get(afterSecond);
            assertEquals(errorCode, actual, "Error field should be BADKEY");
        }
    }

    /** Injecting an incompatible type into the private field should fail with an
     *  {@link IllegalArgumentException}. */
    @Test
    void getGeneratedTSIG_typeSafetyViolation_throwsIllegalArgumentException() throws Exception {
        Field f = Message.class.getDeclaredField("generatedTsig");
        f.setAccessible(true);
        // The JVM enforces the field type, so attempting to set a String throws
        // IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> f.set(message, "I am not a TSIGRecord"),
                "Setting generatedTsig to an incompatible type should throw IllegalArgumentException");
    }
}
```