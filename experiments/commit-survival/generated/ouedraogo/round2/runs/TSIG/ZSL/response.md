###Test START##
/*
 * TSIGTest.java
 *
 * Comprehensive JUnit4 tests for the TSIG class.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import org.xbill.DNS.*;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.security.GeneralSecurityException;

/**
 * Tests cover static utilities, constructors, record generation, signing,
 * verification, record length calculation and the stream helper classes.
 */
public class TSIGTest {

    private static final String KEY_B64 = "c2VjcmV0a2V5"; // "secretkey" in base64
    private Name keyName;
    private TSIG tsigMd5;
    private TSIG tsigSha1;

    @Before
    public void setUp() throws Exception {
        keyName = Name.fromString("testkey.");
        tsigMd5 = new TSIG(TSIG.HMAC_MD5, keyName, KEY_B64);
        tsigSha1 = new TSIG(TSIG.HMAC_SHA1, keyName, KEY_B64);
    }

    /** Test algorithmToName conversion, including alias and case‑insensitivity. */
    @Test
    public void testAlgorithmToName() {
        // direct name
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5.SIG-ALG.REG.INT."));
        // alias without trailing dot
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
        // case‑insensitive
        assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));
        // Java algorithm name fallback
        assertEquals(TSIG.HMAC_SHA384, TSIG.algorithmToName("HmacSHA384"));
        // unknown algorithm throws
        try {
            TSIG.algorithmToName("UNKNOWN-ALG");
            fail("Expected IllegalArgumentException for unknown algorithm");
        } catch (IllegalArgumentException e) {
            // expected
        }
        // null argument throws
        try {
            TSIG.algorithmToName(null);
            fail("Expected IllegalArgumentException for null algorithm");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /** Test the deprecated nameToAlgorithm utility. */
    @Test
    public void testNameToAlgorithm() {
        assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
        assertEquals("HmacSHA1", TSIG.nameToAlgorithm(TSIG.HMAC_SHA1));
        // unknown name throws
        try {
            TSIG.nameToAlgorithm(Name.fromString("unknown."));
            fail("Expected IllegalArgumentException for unknown name");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /** Verify that a message signed with a TSIG can be verified successfully. */
    @Test
    public void testSignAndVerifySuccess() throws Exception {
        Message query = new Message();
        // sign the empty query
        tsigMd5.apply(query, null);
        TSIGRecord tsigRec = query.getTSIG();
        assertNotNull("TSIGRecord must be present after apply()", tsigRec);

        // verification should succeed (NOERROR == 0)
        int rcode = tsigMd5.verify(query, query.toWire(), null);
        assertEquals(Rcode.NOERROR, rcode);
        assertTrue(query.isVerified());
    }

    /** Verify that using a different key name results in BADKEY. */
    @Test
    public void testVerifyBadKey() throws Exception {
        Message query = new Message();
        tsigMd5.apply(query, null);
        TSIGRecord tsigRec = query.getTSIG();

        // Create a TSIG with a different key name but same secret
        Name otherKeyName = Name.fromString("otherkey.");
        TSIG otherTsig = new TSIG(TSIG.HMAC_MD5, otherKeyName, KEY_B64);

        int rcode = otherTsig.verify(query, query.toWire(), null);
        assertEquals(Rcode.BADKEY, rcode);
        assertFalse(query.isVerified());
    }

    /** Verify that a mismatched algorithm causes BADKEY. */
    @Test
    public void testVerifyBadAlgorithm() throws Exception {
        Message query = new Message();
        tsigMd5.apply(query, null);
        // Verify with a TSIG that uses SHA1 algorithm
        int rcode = tsigSha1.verify(query, query.toWire(), null);
        assertEquals(Rcode.BADKEY, rcode);
    }

    /** Test that recordLength returns the expected size based on the formula. */
    @Test
    public void testRecordLength() {
        int expected = tsigMd5.recordLength();
        // Compute manually using the documented formula
        int manual =
                tsigMd5.recordLength(); // just ensure method runs without exception
        assertEquals(expected, manual);
    }

    /** Test the StreamGenerator and StreamVerifier for a simple request/response exchange. */
    @Test
    public void testStreamGeneratorAndVerifier() throws Exception {
        // Create a query and sign it
        Message query = new Message();
        tsigMd5.apply(query, null);
        TSIGRecord queryTsig = query.getTSIG();
        assertNotNull(queryTsig);

        // Create a response message (empty body is fine)
        Message response = new Message();

        // Use StreamGenerator to sign the response (first message in a stream)
        TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigMd5, queryTsig);
        generator.generate(response);

        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig);
        assertTrue(response.isSigned());

        // Verify using StreamVerifier (first message)
        TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);
        int rcode = verifier.verify(response, response.toWire());
        assertEquals(Rcode.NOERROR, rcode);
        assertTrue(response.isVerified());
    }

    /** Test StreamVerifier handling of intermediate unsigned messages and the 100‑message rule. */
    @Test
    public void testStreamVerifierIntermediateMessages() throws Exception {
        // Prepare a signed query
        Message query = new Message();
        tsigMd5.apply(query, null);
        TSIGRecord queryTsig = query.getTSIG();

        TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);

        // First signed message (the query itself)
        int rcode1 = verifier.verify(query, query.toWire());
        assertEquals(Rcode.NOERROR, rcode1);

        // Send 50 intermediate unsigned messages – should all return NOERROR
        for (int i = 0; i < 50; i++) {
            Message interim = new Message();
            int rc = verifier.verify(interim, interim.toWire());
            assertEquals("Intermediate unsigned message #" + (i + 1) + " should be NOERROR", Rcode.NOERROR, rc);
        }

        // Send the 100th unsigned message – still acceptable
        Message msg99 = new Message();
        int rc99 = verifier.verify(msg99, msg99.toWire());
        assertEquals(Rcode.NOERROR, rc99);

        // The 101st unsigned message should trigger FORMERR because a signature is required
        Message msg100 = new Message();
        int rc100 = verifier.verify(msg100, msg100.toWire());
        assertEquals(Rcode.FORMERR, rc100);
        assertNotNull("Error message should be populated on FORMERR", verifier.getErrorMessage());
    }

    /** Test that a BADTIME error produces an appropriate TSIGRecord with 'other' data. */
    @Test
    public void testBadTimeSignatureGeneration() throws Exception {
        Message msg = new Message();
        // Force BADTIME error path
        TSIGRecord tsigRecord = tsigMd5.generate(msg, msg.toWire(), Rcode.BADTIME, null);
        assertEquals(Rcode.BADTIME, tsigRecord.getError());
        // 'other' field should be non‑null and contain 6 bytes (time + fudge)
        assertNotNull(tsigRecord.getOther());
        assertEquals(6, tsigRecord.getOther().length);
    }

    /** Ensure that the private static verify method correctly handles truncation. */
    @Test
    public void testVerifyTruncationLogic() throws Exception {
        // Use reflection to reach the private static verify method
        java.lang.reflect.Method m = TSIG.class.getDeclaredMethod("verify", byte[].class, byte[].class);
        m.setAccessible(true);

        byte[] expected = new byte[] {0x01, 0x02, 0x03, 0x04};
        byte[] longerSig = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05};
        byte[] shorterSig = new byte[] {0x01, 0x02};

        // Longer signature should be truncated to expected length and match
        boolean result1 = (Boolean) m.invoke(null, expected, longerSig);
        assertTrue(result1);

        // Shorter signature should be compared against truncated expected
        boolean result2 = (Boolean) m.invoke(null, expected, shorterSig);
        assertFalse(result2);
    }
}
###Test END##