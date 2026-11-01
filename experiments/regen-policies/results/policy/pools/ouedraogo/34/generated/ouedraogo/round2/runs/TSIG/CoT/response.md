###Test START##
```java
/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.TSIG}.
 *
 * The test class follows the requested steps:
 *
 * 1. List all public methods (including constructors) with their signatures.
 * 2. Provide a basic test for each public method.
 * 3. Identify edge‑case / exception scenarios.
 * 4. Add dedicated tests for those edge cases.
 * 5. Merge everything into a single {@code TSIGTest.java} file.
 *
 * NOTE: The tests use only classes that appear in the supplied source code or in the
 *       public APIs listed in the prompt. No mocking frameworks are used – real DNS objects
 *       are created (e.g. {@link Name}, {@link Message}, {@link TSIGRecord}, {@link ARecord}).
 */
package org.xbill.DNS;

import org.junit.*;
import static org.junit.Assert.*;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Comprehensive tests for the {@code TSIG} class.
 */
public class TSIGTest {

    /* ---------------------------------------------------------------------- */
    /* 1. PUBLIC METHODS (including constructors) of TSIG                     */
    /* ---------------------------------------------------------------------- */
    // static Name algorithmToName(String alg)
    // @Deprecated static String nameToAlgorithm(Name name)
    // static TSIG fromString(String str)
    // public TSIG(Name algorithm, Name name, String key)
    // public TSIG(Name algorithm, Name name, byte[] keyBytes)
    // public TSIG(Name algorithm, Name name, SecretKey key)
    // public TSIG(Name algorithm, Name name, SecretKey key, Clock clock)
    // @Deprecated public TSIG(Mac mac, Name name)
    // @Deprecated public TSIG(Name name, byte[] key)
    // public TSIG(Name algorithm, String name, String key)
    // public TSIG(String algorithm, String name, String key)
    // @Deprecated public TSIG(String name, String key)
    // public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old)
    // public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old, boolean fullSignature)
    // public void apply(Message m, TSIGRecord old)
    // public void apply(Message m, int error, TSIGRecord old)
    // public void apply(Message m, TSIGRecord old, boolean fullSignature)
    // public void apply(Message m, int error, TSIGRecord old, boolean fullSignature)
    // @Deprecated public void applyStream(Message m, TSIGRecord old, boolean fullSignature)
    // public int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG)
    // public int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG, boolean fullSignature)
    // public int recordLength()
    // public static class StreamGenerator { … }
    // public static class StreamVerifier { … }

    /* ---------------------------------------------------------------------- */
    /* Helper constants and objects used by many tests                         */
    /* ---------------------------------------------------------------------- */
    private static final Name KEY_NAME = Name.fromString("tsig-key.");
    private static final String BASE64_KEY = Base64.getEncoder().encodeToString("secret".getBytes());
    private static final Name HMAC_SHA256 = TSIG.HMAC_SHA256;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

    /** A minimal A record used as payload for messages. */
    private static Record sampleARecord() throws Exception {
        return new ARecord(Name.fromString("example.com."), DClass.IN, 300,
                InetAddress.getByName("1.2.3.4"));
    }

    /** Build a simple query message containing a single question. */
    private static Message newQueryMessage() throws Exception {
        Message m = Message.newQuery(sampleARecord());
        return m;
    }

    /* ---------------------------------------------------------------------- */
    /* 2. BASIC tests for each public method                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAlgorithmToName_basicAlias() {
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
        assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    }

    @Test
    public void testAlgorithmToName_caseInsensitive() {
        assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
        assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HMAC-SHA256."));
    }

    @Test
    public void testNameToAlgorithm_basic() {
        assertEquals("HmacSHA1", TSIG.nameToAlgorithm(TSIG.HMAC_SHA1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNameToAlgorithm_unknown() {
        // Construct a name that is not in the map
        Name unknown = Name.fromString("unknown-alg.");
        TSIG.nameToAlgorithm(unknown);
    }

    @Test
    public void testConstructor_fromNameAndBase64String() {
        TSIG tsig = new TSIG(HMAC_SHA256, KEY_NAME, BASE64_KEY);
        assertNotNull(tsig);
        // recordLength should be positive for any valid key
        assertTrue(tsig.recordLength() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_invalidBase64String() {
        new TSIG(HMAC_SHA256, KEY_NAME, "!!! not base64 !!!");
    }

    @Test
    public void testFromString_validTwoPart() {
        TSIG tsig = TSIG.fromString("mykey:" + BASE64_KEY);
        assertNotNull(tsig);
        // default algorithm is MD5
        assertEquals(TSIG.HMAC_MD5, tsig.alg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_invalidFormat() {
        TSIG.fromString("invalid-format-without-colon-or-slash");
    }

    @Test
    public void testGenerate_andApply_basicFlow() throws Exception {
        TSIG tsig = new TSIG(HMAC_SHA256, KEY_NAME, BASE64_KEY);
        Message query = newQueryMessage();

        // Apply TSIG to the query (no previous TSIG record)
        tsig.apply(query, null);
        TSIGRecord added = query.getTSIG();
        assertNotNull("TSIG record should be attached", added);
        assertEquals(KEY_NAME, added.getName());
        assertEquals(TSIG.HMAC_SHA256, added.getAlgorithm());

        // Verify the signed message
        byte[] wire = query.toWire();
        int result = tsig.verify(query, wire, null);
        assertEquals(Rcode.NOERROR, result);
        assertTrue(query.isVerified());
    }

    @Test
    public void testGenerate_errorBadTime_returnsBadTimeOnVerify() throws Exception {
        TSIG tsig = new TSIG(HMAC_SHA256, KEY_NAME, BASE64_KEY);
        Message query = newQueryMessage();

        // Generate a TSIG record with BADTIME error (no MAC is calculated)
        TSIGRecord badTimeRecord = tsig.generate(query, query.toWire(), Rcode.BADTIME, null);
        query.addRecord(badTimeRecord, Section.ADDITIONAL);
        query.tsigState = Message.TSIG_SIGNED; // mimic apply()

        // Verify – the method should notice the BADTIME error and return it
        int result = tsig.verify(query, query.toWire(), null);
        assertEquals(Rcode.BADTIME, result);
    }

    @Test
    public void testRecordLength_nonZero() {
        TSIG tsig = new TSIG(HMAC_SHA256, KEY_NAME, BASE64_KEY);
        assertTrue("recordLength must be > 0", tsig.recordLength() > 0);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Edge‑case / exception scenarios                                    */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testAlgorithmToName_nullArgument() {
        TSIG.algorithmToName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAlgorithmToName_unknownAlgorithm() {
        TSIG.algorithmToName("non‑existent‑alg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStreamGenerator_invalidSignEveryNth() {
        TSIG tsig = new TSIG(HMAC_SHA256, KEY_NAME, BASE64_KEY);
        TSIGRecord dummy = tsig.generate(newQueryMessage(),
                newQueryMessage().toWire(), Rcode.NOERROR, null);
        // Use reflection to invoke the package‑private constructor that checks the range
        // (the public constructor with 2 arguments delegates to it)
        new TSIG.StreamGenerator(tsig, dummy, 0); // invalid, must be >=1
    }

    @Test
    public void testStreamGenerator_generatesSignedMessages() throws Exception {
        TSIG tsig = new TSIG(HMAC_SHA256, KEY_NAME, BASE64_KEY);
        Message query = newQueryMessage();
        TSIGRecord queryTsig = tsig.generate(query, query.toWire(), Rcode.NOERROR, null);
        TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

        Message response = new Message(); // empty response
        generator.generate(response);
        TSIGRecord attached = response.getTSIG();
        assertNotNull("Response should contain a TSIG record", attached);
        assertEquals(KEY_NAME, attached.getName());
    }

    @Test
    public void testStreamVerifier_successfulTwoMessageStream() throws Exception {
        // Build key and initial query TSIG
        TSIG tsig = new TSIG(HMAC_SHA256, KEY_NAME, BASE64_KEY);
        Message query = newQueryMessage();
        TSIGRecord queryTsig = tsig.generate(query, query.toWire(), Rcode.NOERROR, null);
        TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

        // First response – must contain a TSIG
        Message firstResp = new Message();
        TSIGRecord firstTsig = tsig.generate(firstResp, firstResp.toWire(), Rcode.NOERROR, null);
        firstResp.addRecord(firstTsig, Section.ADDITIONAL);
        int rc1 = verifier.verify(firstResp, firstResp.toWire());
        assertEquals(Rcode.NOERROR, rc1);
        assertTrue(firstResp.isVerified());

        // Second response – may omit TSIG because it is within 100 messages
        Message secondResp = new Message();
        int rc2 = verifier.verify(secondResp, secondResp.toWire(), false);
        assertEquals(Rcode.NOERROR, rc2);
        // No TSIG attached, but verifier should have marked it as INTERMEDIATE
        assertEquals(Message.TSIG_INTERMEDIATE, secondResp.tsigState);
    }

    @Test
    public void testStreamVerifier_missingFirstTsig_returnsFormErr() throws Exception {
        TSIG tsig = new TSIG(HMAC_SHA256, KEY_NAME, BASE64_KEY);
        Message query = newQueryMessage();
        TSIGRecord queryTsig = tsig.generate(query, query.toWire(), Rcode.NOERROR, null);
        TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

        // First response without a TSIG record
        Message resp = new Message();
        int rc = verifier.verify(resp, resp.toWire());
        assertEquals(Rcode.FORMERR, rc);
        assertEquals("missing required signature on first message", verifier.getErrorMessage());
    }

    @Test
    public void testVerify_mismatchedAlgorithm_returnsBadKey() throws Exception {
        // TSIG instance with SHA256 algorithm
        TSIG tsigSha256 = new TSIG(HMAC_SHA256, KEY_NAME, BASE64_KEY);
        // Another TSIG instance that will sign the message with MD5
        TSIG tsigMd5 = new TSIG(TSIG.HMAC_MD5, KEY_NAME, BASE64_KEY);

        Message query = newQueryMessage();
        tsigMd5.apply(query, null); // message signed with MD5

        // Verify with the SHA256 key – should get BADKEY
        int rc = tsigSha256.verify(query, query.toWire(), null);
        assertEquals(Rcode.BADKEY, rc);
    }

    @Test
    public void testVerify_signatureTooShort_returnsBadSig() throws Exception {
        // Create a TSIG key with a very short key (1 byte) which leads to a short MAC.
        byte[] tinyKey = new byte[]{0x01};
        TSIG tsig = new TSIG(HMAC_SHA256, KEY_NAME, tinyKey);
        Message query = newQueryMessage();
        tsig.apply(query, null); // this will produce a signature that is too short for SHA256

        // The internal verifySignature method will reject it; we just assert that verification fails.
        int rc = tsig.verify(query, query.toWire(), null);
        assertEquals(Rcode.BADSIG, rc);
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Merge all tests into a single file – already done above                */
    /* ---------------------------------------------------------------------- */

}
```
###Test END##