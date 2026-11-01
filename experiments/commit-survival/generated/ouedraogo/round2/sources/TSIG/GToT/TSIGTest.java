/****************************************************************************************
 *  JUnit‑4 test suite for org.xbill.DNS.TSIG
 *
 *  The tests cover:
 *   • All public static methods (algorithmToName, nameToAlgorithm, fromString)
 *   • All public constructors
 *   • Core instance methods: generate, apply, verify, recordLength
 *   • Edge‑case handling and expected exceptions
 *   • StreamGenerator / StreamVerifier interaction
 *
 *  The test class lives in the same package as TSIG so that it can access package‑private
 *  constructors and fields where needed.
 ****************************************************************************************/
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link TSIG}.
 *
 * NOTE: The tests use only the public API that is present in the source code supplied in the
 *       problem statement. No external mocking frameworks are used – real DNS objects are
 *       constructed via their public constructors / factory methods.
 */
public class TSIGTest {

  private static final String BASE64_KEY = Base64.getEncoder().encodeToString("secret".getBytes());
  private static final Name TEST_NAME = Name.fromString("example.com.");
  private static final Name OTHER_NAME = Name.fromString("other.org.");

  private TSIG tsigMD5;
  private TSIG tsigSHA256;

  @Before
  public void setUp() throws Exception {
    // Create two TSIG objects with different algorithms for later use.
    tsigMD5 = new TSIG(TSIG.HMAC_MD5, TEST_NAME, BASE64_KEY);
    tsigSHA256 = new TSIG(TSIG.HMAC_SHA256, TEST_NAME, BASE64_KEY);
  }

  /* -------------------------------------------------------------------------
   *  algorithmToName(..) – normal and error paths
   * ---------------------------------------------------------------------- */
  @Test
  public void testAlgorithmToNameValid() {
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));
  }

  @Test
  public void testAlgorithmToNameAliasForMD5() {
    // Both "HMAC-MD5" and "HMAC-MD5." map to the constant HMAC_MD5
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameNull() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameUnknown() {
    TSIG.algorithmToName("unknown-algorithm");
  }

  /* -------------------------------------------------------------------------
   *  nameToAlgorithm(..) – normal and error paths (deprecated)
   * ---------------------------------------------------------------------- */
  @Test
  public void testNameToAlgorithmValid() {
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithmUnknown() {
    TSIG.nameToAlgorithm(OTHER_NAME);
  }

  /* -------------------------------------------------------------------------
   *  fromString(..) – parsing of the legacy string format
   * ---------------------------------------------------------------------- */
  @Test
  public void testFromStringTwoParts() {
    // "name:secret" – uses default algorithm HMAC_MD5
    TSIG tsig = TSIG.fromString(TEST_NAME.toString() + ":" + BASE64_KEY);
    assertNotNull(tsig);
    // internal algorithm should be HMAC_MD5
    assertEquals(TSIG.HMAC_MD5, tsig.alg);
  }

  @Test
  public void testFromStringThreeParts() {
    // "alg:name:secret"
    String spec = "hmac-sha256:" + TEST_NAME + ":" + BASE64_KEY;
    TSIG tsig = TSIG.fromString(spec);
    assertNotNull(tsig);
    assertEquals(TSIG.HMAC_SHA256, tsig.alg);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromStringInvalid() {
    // Only one part – should throw
    TSIG.fromString("invalidstring");
  }

  /* -------------------------------------------------------------------------
   *  recordLength() – deterministic calculation
   * ---------------------------------------------------------------------- */
  @Test
  public void testRecordLength() {
    // The formula is fixed; we compute the expected value directly.
    int expected = TEST_NAME.length() + 10 + TSIG.HMAC_SHA256.length()
        + 8 + 18 + 4 + 8;
    assertEquals(expected, tsigSHA256.recordLength());
  }

  /* -------------------------------------------------------------------------
   *  generate() + apply() + verify() – happy path
   * ---------------------------------------------------------------------- */
  @Test
  public void testGenerateApplyAndVerify() throws Exception {
    Message query = new Message(0x1234);
    // Sign the query with tsigMD5
    tsigMD5.apply(query, null);
    TSIGRecord tsigRecord = query.getTSIG();
    assertNotNull("TSIGRecord should have been added", tsigRecord);
    assertArrayEquals(tsigMD5.name.toWireCanonical(), tsigRecord.getName().toWireCanonical());

    // Verify the signed message using the same TSIG instance
    int result = tsigMD5.verify(query, query.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue(query.isVerified());
  }

  @Test
  public void testVerifyBadKey() throws Exception {
    // Sign with tsigMD5
    Message query = new Message(0x4321);
    tsigMD5.apply(query, null);

    // Attempt verification with a TSIG that uses a *different* key name
    TSIG otherTsig = new TSIG(TSIG.HMAC_MD5, OTHER_NAME, BASE64_KEY);
    int result = otherTsig.verify(query, query.toWire(), null);
    // The name/algorithm mismatch should trigger BADKEY
    assertEquals(Rcode.BADKEY, result);
    assertFalse(query.isVerified());
  }

  /* -------------------------------------------------------------------------
   *  StreamGenerator + StreamVerifier – single‑message stream
   * ---------------------------------------------------------------------- */
  @Test
  public void testStreamGeneratorAndVerifierSingleMessage() throws Exception {
    // Prepare a query and its TSIG record (the "queryTsig")
    Message query = new Message(0x1111);
    tsigSHA256.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Initialise generator and verifier
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigSHA256, queryTsig);
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSHA256, queryTsig);

    // Create a response message (empty payload is fine)
    Message response = new Message(0x1111);
    generator.generate(response); // this adds a TSIGRecord

    // Verify the response – should be NOERROR
    int verifyResult = verifier.verify(response, response.toWire());
    assertEquals(Rcode.NOERROR, verifyResult);
    assertTrue(response.isVerified());
  }

  /* -------------------------------------------------------------------------
   *  StreamVerifier – missing signature on first message (FORMERR)
   * ---------------------------------------------------------------------- */
  @Test
  public void testStreamVerifierMissingSignatureOnFirstMessage() throws Exception {
    // Build a query TSIG (as the "expected" first‑message signature)
    Message query = new Message(0x2222);
    tsigMD5.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMD5, queryTsig);

    // Create a response *without* a TSIG record – this simulates a protocol error.
    Message response = new Message(0x2222);
    int result = verifier.verify(response, response.toWire());
    assertEquals(Rcode.FORMERR, result);
    assertEquals("missing required signature on first message", verifier.getErrorMessage());
  }

  /* -------------------------------------------------------------------------
   *  StreamVerifier – intermediate message without signature (allowed)
   * ---------------------------------------------------------------------- */
  @Test
  public void testStreamVerifierIntermediateUnsignedMessage() throws Exception {
    // Query with TSIG
    Message query = new Message(0x3333);
    tsigSHA256.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSHA256, queryTsig);

    // First response (signed)
    Message firstResp = new Message(0x3333);
    tsigSHA256.apply(firstResp, queryTsig);
    assertEquals(Rcode.NOERROR, verifier.verify(firstResp, firstResp.toWire()));

    // Second response (unsigned, not the last)
    Message middleResp = new Message(0x3333);
    assertEquals(Rcode.NOERROR, verifier.verify(middleResp, middleResp.toWire()));
    // No errorMessage should be set for a valid unsigned intermediate message
    assertNull(verifier.getErrorMessage());
  }

  /* -------------------------------------------------------------------------
   *  StreamVerifier – missing signature after 100 messages (FORMERR)
   * ---------------------------------------------------------------------- */
  @Test
  public void testStreamVerifierMissingSignatureAfterHundred() throws Exception {
    Message query = new Message(0x4444);
    tsigMD5.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Use a generator that signs *every* message (signEveryNthMessage = 1)
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigMD5, queryTsig);
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMD5, queryTsig);

    // Simulate 100 unsigned messages after the first signed one
    for (int i = 0; i < 99; i++) {
      Message unsigned = new Message(0x4444);
      // verify as intermediate (not last)
      int rc = verifier.verify(unsigned, unsigned.toWire());
      assertEquals(Rcode.NOERROR, rc);
    }

    // The 101st message must contain a signature; we omit it to trigger FORMERR
    Message missingSig = new Message(0x4444);
    int rc = verifier.verify(missingSig, missingSig.toWire());
    assertEquals(Rcode.FORMERR, rc);
    assertTrue(verifier.getErrorMessage().contains("Missing required signature on message #101"));
  }
}
