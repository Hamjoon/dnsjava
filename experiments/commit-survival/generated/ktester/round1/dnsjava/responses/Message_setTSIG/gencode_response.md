```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Additional tests for {@link Message#setTSIG(TSIG)} covering edge‑cases and internal state.
 *
 * <p>The tests are grouped according to the JSON specification supplied with the
 * task.  They complement the basic functional tests already present in the
 * {@code Message_setTSIG_Test} class.
 */
@ExtendWith(MockitoExtension.class)
class Message_setTSIG_AdditionalTest {

    /** Helper to create a simple A‑record used by many tests. */
    private static Record makeQuestion() throws TextParseException {
        Name qname = Name.fromString("example.com.");
        return Record.newRecord(qname, Type.A, DClass.IN);
    }

    /** --------------------------------------------------------------------- */
    /** 1. Null TSIG key handling – expect IllegalArgumentException (or NPE). */
    @Test
    void setTSIG_nullKey_throwsIllegalArgumentException() {
        Message msg = new Message();
        msg.addRecord(makeQuestion(), Section.QUESTION);
        // The library historically throws NPE, but the contract mentions IllegalArgumentException.
        assertThrows(
                IllegalArgumentException.class,
                () -> msg.setTSIG(null),
                "setTSIG(null) should reject a null key");
    }

    /** --------------------------------------------------------------------- */
    /** 2. Valid TSIG key handling – the call should *not* sign the message yet. */
    @ParameterizedTest
    @MethodSource("validKeyProvider")
    void setTSIG_validKeyDoesNotSign(TSIG key) throws Exception {
        Message msg = new Message();
        msg.addRecord(makeQuestion(), Section.QUESTION);
        // pre‑condition
        assertFalse(msg.isSigned(), "Message must start unsigned");
        assertFalse(msg.isVerified(), "Message must start unverified");
        assertNull(msg.getTSIG(), "No TSIG record should exist before signing");

        // act
        msg.setTSIG(key);

        // post‑condition – still unsigned / unverified, no TSIGRecord yet
        assertFalse(msg.isSigned(), "setTSIG should not mark the message signed");
        assertFalse(msg.isVerified(), "setTSIG should not mark the message verified");
        assertNull(msg.getTSIG(), "TSIGRecord is generated only when the message is signed");
    }

    private static Stream<Arguments> validKeyProvider() throws Exception {
        return Stream.of(
                Arguments.of(new TSIG(Name.fromString("example.com."), "hmac-sha256.", "dGVzdFNlY3JldA==")),
                Arguments.of(new TSIG(Name.fromString("_test._tsig.example.com."), "hmac-md5.", "YWJjZDEyMzQ1"))
        );
    }

    /** --------------------------------------------------------------------- */
    /** 3. Replace existing TSIG key – second call should overwrite the first. */
    @Test
    void setTSIG_replaceExistingKey() throws Exception {
        TSIG firstKey = new TSIG(Name.fromString("first.example.com."), "hmac-sha1.", "c2VjcmV0MQ==");
        TSIG secondKey = new TSIG(Name.fromString("second.example.com."), "hmac-sha256.", "c2VjcmV0Mg==");

        Message msg = new Message();
        msg.addRecord(makeQuestion(), Section.QUESTION);

        // first call – should succeed
        msg.setTSIG(firstKey);
        // second call – should replace the previous key without throwing
        msg.setTSIG(secondKey);

        // internal state must be reset to unsigned after each call
        int tsigState = (int) getPrivateField(msg, "tsigState");
        assertEquals(Message.TSIG_UNSIGNED, tsigState,
                "tsigState must be TSIG_UNSIGNED after the second setTSIG");
        // No TSIGRecord should have been generated yet
        assertNull(msg.getTSIG(), "No TSIGRecord should be present after just setting the key");
    }

    /** --------------------------------------------------------------------- */
    /** 4. TSIG internal state after calling the overloaded setTSIG(TSIG). */
    @Test
    void setTSIG_internalStateAfterCall() throws Exception {
        TSIG key = new TSIG(Name.fromString("state.example.com."), "hmac-sha256.", "c2VjcmV0");
        Message msg = new Message();
        msg.addRecord(makeQuestion(), Section.QUESTION);

        msg.setTSIG(key);

        // Reflectively inspect private fields
        int tsigState = (int) getPrivateField(msg, "tsigState");
        int tsigStart = (int) getPrivateField(msg, "tsigstart");

        assertEquals(Message.TSIG_UNSIGNED, tsigState,
                "tsigState should be TSIG_UNSIGNED (0) after setTSIG");
        assertEquals(-1, tsigStart,
                "tsigstart should be -1 (not yet rendered) after setTSIG");
        assertFalse(msg.isSigned(), "Message should not be signed yet");
        assertFalse(msg.isVerified(), "Message should not be verified yet");
    }

    /** --------------------------------------------------------------------- */
    /** 5. Idempotent call with the same TSIG key – state must stay unchanged. */
    @Test
    void setTSIG_idempotentSameKey() throws Exception {
        TSIG sharedKey = new TSIG(Name.fromString("same.example.com."), "hmac-md5.", "c2VjcmV0");

        Message msg = new Message();
        msg.addRecord(makeQuestion(), Section.QUESTION);

        // first invocation
        msg.setTSIG(sharedKey);
        int stateAfterFirst = (int) getPrivateField(msg, "tsigState");
        int startAfterFirst = (int) getPrivateField(msg, "tsigstart");

        // second invocation with the same key
        msg.setTSIG(sharedKey);
        int stateAfterSecond = (int) getPrivateField(msg, "tsigState");
        int startAfterSecond = (int) getPrivateField(msg, "tsigstart");

        assertEquals(stateAfterFirst, stateAfterSecond,
                "tsigState must remain unchanged after idempotent call");
        assertEquals(startAfterFirst, startAfterSecond,
                "tsigstart must remain unchanged after idempotent call");
    }

    /** --------------------------------------------------------------------- */
    /** 6. Duplicate TSIG record already present – expect IllegalStateException. */
    @Test
    void setTSIG_duplicateRecordThrows() throws Exception {
        TSIG key = new TSIG(Name.fromString("dup.example.com."), "hmac-sha256.", "dGVzdFNlY3JldA==");
        Message msg = new Message();
        msg.addRecord(makeQuestion(), Section.QUESTION);

        // Manually add a TSIGRecord using the TSIG helper (this mimics a prior signing)
        TSIGRecord existing = key.generate(msg, null, 0, null, false);
        msg.addRecord(existing, Section.ADDITIONAL);

        // Now attempting to set another TSIG key must fail
        IllegalStateException ise = assertThrows(
                IllegalStateException.class,
                () -> msg.setTSIG(key),
                "Adding a TSIG when one already exists should throw IllegalStateException");
        assertTrue(ise.getMessage().contains("TSIG"), "Exception message should mention TSIG");
    }

    /** --------------------------------------------------------------------- */
    /** 7. Null TSIG key after a TSIGRecord is already present – still IllegalArgumentException. */
    @Test
    void setTSIG_nullKeyWhenTSIGRecordExists() throws Exception {
        TSIG key = new TSIG(Name.fromString("dup2.example.com."), "hmac-sha256.", "dGVzdFNlY3JldA==");
        Message msg = new Message();
        msg.addRecord(makeQuestion(), Section.QUESTION);
        TSIGRecord existing = key.generate(msg, null, 0, null, false);
        msg.addRecord(existing, Section.ADDITIONAL);

        assertThrows(
                IllegalArgumentException.class,
                () -> msg.setTSIG(null),
                "setTSIG(null) must still reject the call even if a TSIGRecord is present");
    }

    /** --------------------------------------------------------------------- */
    /** 8. TSIG key with empty secret – constructor should reject it. */
    @Test
    void tsigConstructor_emptySecretThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TSIG(Name.fromString("emptysecret.example.com."), "hmac-sha256.", ""),
                "Creating a TSIG with an empty secret must throw IllegalArgumentException");
    }

    /** --------------------------------------------------------------------- */
    /** 9. Unsupported TSIG algorithm – constructor should reject it. */
    @Test
    void tsigConstructor_unsupportedAlgorithmThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TSIG(Name.fromString("example.com."), "unsupported-algo", "dGVzdFNlY3JldA=="),
                "Creating a TSIG with an unsupported algorithm must throw IllegalArgumentException");
    }

    /** --------------------------------------------------------------------- */
    /** 10. TSIG key with null name – constructor should throw NullPointerException. */
    @Test
    void tsigConstructor_nullNameThrows() {
        assertThrows(
                NullPointerException.class,
                () -> new TSIG(null, "hmac-sha256.", "dGVzdFNlY3JldA=="),
                "Creating a TSIG with a null name must throw NullPointerException");
    }

    /** --------------------------------------------------------------------- */
    /** Utility method to read a private int field from Message via reflection. */
    private static Object getPrivateField(Message msg, String fieldName) throws Exception {
        Field f = Message.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(msg);
    }
}
```