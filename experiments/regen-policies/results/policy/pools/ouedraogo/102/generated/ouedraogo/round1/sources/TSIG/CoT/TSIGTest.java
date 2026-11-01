package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link TSIG}. The tests cover:
 *
 * <ul>
 *   <li>All public constructors and static methods.
 *   <li>Normal operation of {@code generate}, {@code apply} and {@code verify}.
 *   <li>Edge‑case handling (null / unknown algorithms, malformed keys, signature tampering,
 *       wrong key, missing TSIG on a stream, etc.).
 * </ul>
 *
 * The test class is deliberately self‑contained – it builds real {@link Message},
 * {@link Name}, {@link TSIGRecord} and {@link Mac} instances using only the public API
 * supplied in the prompt.
 */
public class TSIGTest {

  private static final byte[] KEY_BYTES = "testkey".getBytes(); // simple key material
  private static final String KEY_B64 = Base64.getEncoder().encodeToString(KEY_BYTES);
  private static final Name KEY_NAME = Name.fromString("testkey.");

  private TSIG tsigMd5;      // HMAC‑MD5 based TSIG (default algorithm in many tests)
  private TSIG tsigSha1;     // HMAC‑SHA1 based TSIG (used for algorithm‑specific tests)

  @Before
  public void setUp() {
    // Construct two TSIG objects that will be used throughout the tests.
    tsigMd5 = new TSIG(TSIG.HMAC_MD5, KEY_NAME, KEY_B64);
    tsigSha1 = new TSIG(TSIG.HMAC_SHA1, KEY_NAME, KEY_B64);
  }

  /* -------------------------------------------------------------
   *  Public static helper methods
   * ------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_ValidNames() {
    // RFC name with trailing dot
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1."));
    // RFC name without trailing dot – method should add it
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
    // Java style name – conversion to RFC name
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("HmacSHA1"));
    // Truncated Java name
    assertEquals(TSIG.HMAC_SHA256_128, TSIG.algorithmToName("HmacSHA256/128"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Null() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Unknown() {
    TSIG.algorithmToName("hmac-unknown");
  }

  @Test
  public void testNameToAlgorithm_Known() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA1", TSIG.nameToAlgorithm(TSIG.HMAC_SHA1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_Unknown() {
    TSIG.nameToAlgorithm(Name.fromString("unknown."));
  }

  @Test
  public void testFromString_ValidTwoPart() {
    TSIG t = TSIG.fromString("testkey:" + KEY_B64);
    assertNotNull(t);
    // default algorithm is HMAC_MD5
    assertEquals(TSIG.HMAC_MD5, t.alg);
    assertEquals(KEY_NAME, t.name);
  }

  @Test
  public void testFromString_ValidThreePart() {
    TSIG t = TSIG.fromString("hmac-sha1:testkey:" + KEY_B64);
    assertNotNull(t);
    assertEquals(TSIG.HMAC_SHA1, t.alg);
    assertEquals(KEY_NAME, t.name);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromString_Invalid() {
    TSIG.fromString("invalidspec");
  }

  /* -------------------------------------------------------------
   *  Constructors – basic sanity checks
   * ------------------------------------------------------------- */

  @Test
  public void testConstructor_Name_StringKey() {
    TSIG t = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_B64);
    assertEquals(TSIG.HMAC_SHA256, t.alg);
    assertEquals(KEY_NAME, t.name);
  }

  @Test
  public void testConstructor_Name_ByteArrayKey() {
    TSIG t = new TSIG(TSIG.HMAC_SHA224, KEY_NAME, KEY_BYTES);
    assertEquals(TSIG.HMAC_SHA224, t.alg);
    assertEquals(KEY_NAME, t.name);
  }

  @Test
  public void testConstructor_Name_SecretKey() {
    SecretKey sk = new SecretKeySpec(KEY_BYTES, "HmacSHA384");
    TSIG t = new TSIG(TSIG.HMAC_SHA384, KEY_NAME, sk);
    assertEquals(TSIG.HMAC_SHA384, t.alg);
    assertEquals(KEY_NAME, t.name);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidBase64Key() {
    new TSIG(TSIG.HMAC_MD5, KEY_NAME, "!!! not base64 !!!");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidKeyName() {
    new TSIG(TSIG.HMAC_MD5, Name.fromString("invalid name"), KEY_B64);
  }

  /* -------------------------------------------------------------
   *  generate / apply
   * ------------------------------------------------------------- */

  @Test
  public void testGenerateProducesRecord() {
    Message msg = new Message();
    byte[] wire = msg.toWire();
    TSIGRecord rec = tsigMd5.generate(msg, wire, Rcode.NOERROR, null);
    assertNotNull(rec);
    // The signature length must not exceed the algorithm‑specific maximum.
    assertTrue(rec.getSignature().length <= TSIG.algLengthMap.get(TSIG.HMAC_MD5));
    // Verify that the algorithm stored in the record matches the TSIG instance.
    assertEquals(TSIG.HMAC_MD5, rec.getAlgorithm());
  }

  @Test
  public void testApplyAddsTsigRecord() {
    Message msg = new Message();
    tsigSha1.apply(msg, null);
    TSIGRecord rec = msg.getTSIG();
    assertNotNull(rec);
    assertEquals(TSIG.HMAC_SHA1, rec.getAlgorithm());
    // The message should be marked as signed.
    assertEquals(Message.TSIG_SIGNED, msg.tsigState);
  }

  @Test
  public void testApplyWithErrorAndFullSignature() {
    Message msg = new Message();
    tsigMd5.apply(msg, Rcode.FORMERR, null, false);
    TSIGRecord rec = msg.getTSIG();
    assertNotNull(rec);
    assertEquals(Rcode.FORMERR, rec.getError());
    // fullSignature = false – only reduced variables should be present.
    // We cannot directly inspect the reduced variables, but the call must succeed.
  }

  /* -------------------------------------------------------------
   *  verify – success, bad signature, bad key, bad time
   * ------------------------------------------------------------- */

  @Test
  public void testVerifySuccess() {
    Message query = new Message();
    TSIGRecord queryTsig = tsigMd5.generate(query, query.toWire(), Rcode.NOERROR, null);
    query.addRecord(queryTsig, Section.ADDITIONAL);
    query.tsigState = Message.TSIG_SIGNED;

    // Create a response that contains the same TSIG (as a simple round‑trip test)
    Message response = new Message();
    response.addRecord(queryTsig, Section.ADDITIONAL);
    response.tsigState = Message.TSIG_SIGNED;

    int result = tsigMd5.verify(response, response.toWire(), queryTsig);
    assertEquals(Rcode.NOERROR, result);
    assertEquals(Message.TSIG_VERIFIED, response.tsigState);
  }

  @Test
  public void testVerifyBadSignature() {
    Message msg = new Message();
    TSIGRecord rec = tsigSha1.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    // Corrupt the signature (the array returned is the internal mutable array)
    byte[] sig = rec.getSignature();
    sig[0] ^= 0xFF;
    msg.addRecord(rec, Section.ADDITIONAL);
    msg.tsigState = Message.TSIG_SIGNED;

    int result = tsigSha1.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADSIG, result);
    assertEquals(Message.TSIG_FAILED, msg.tsigState);
  }

  @Test
  public void testVerifyBadKey() {
    // Two TSIG objects that differ in the key name.
    TSIG otherKey = new TSIG(TSIG.HMAC_MD5, Name.fromString("otherkey."), KEY_B64);
    Message msg = new Message();
    TSIGRecord rec = tsigMd5.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    msg.addRecord(rec, Section.ADDITIONAL);
    msg.tsigState = Message.TSIG_SIGNED;

    int result = otherKey.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADKEY, result);
  }

  @Test
  public void testVerifyBadTime() {
    // Use a clock that is far in the future to force a BADTIME condition.
    Clock farFuture = Clock.fixed(Instant.now().plus(Duration.ofHours(5)), Clock.systemUTC().getZone());
    TSIG futureTsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, KEY_B64, farFuture);
    Message msg = new Message();
    TSIGRecord rec = futureTsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    msg.addRecord(rec, Section.ADDITIONAL);
    msg.tsigState = Message.TSIG_SIGNED;

    int result = futureTsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADTIME, result);
  }

  /* -------------------------------------------------------------
   *  recordLength sanity check
   * ------------------------------------------------------------- */

  @Test
  public void testRecordLength() {
    int len = tsigMd5.recordLength();
    // The length must be at least the sum of the components described in the javadoc.
    int minimum = tsigMd5.name.length() + 10 + tsigMd5.alg.length() + 8 + 2
                 + TSIG.algLengthMap.get(tsigMd5.alg) + 4 + 8;
    assertEquals(minimum, len);
  }

  /* -------------------------------------------------------------
   *  StreamGenerator – basic usage
   * ------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorGeneratesTwoMessages() {
    Message query = new Message();
    TSIGRecord queryTsig = tsigMd5.generate(query, query.toWire(), Rcode.NOERROR, null);
    query.addRecord(queryTsig, Section.ADDITIONAL);
    query.tsigState = Message.TSIG_SIGNED;

    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsigMd5, queryTsig);

    // First response
    Message resp1 = new Message();
    gen.generate(resp1);
    assertNotNull(resp1.getTSIG());
    assertEquals(Message.TSIG_SIGNED, resp1.tsigState);

    // Second response – should reuse the same MAC instance
    Message resp2 = new Message();
    gen.generate(resp2);
    assertNotNull(resp2.getTSIG());
    assertEquals(Message.TSIG_SIGNED, resp2.tsigState);
  }

  /* -------------------------------------------------------------
   *  StreamVerifier – success and missing‑signature cases
   * ------------------------------------------------------------- */

  @Test
  public void testStreamVerifierHappyPath() {
    // Build a query with a TSIG record.
    Message query = new Message();
    TSIGRecord queryTsig = tsigMd5.generate(query, query.toWire(), Rcode.NOERROR, null);
    query.addRecord(queryTsig, Section.ADDITIONAL);
    query.tsigState = Message.TSIG_SIGNED;

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);

    // First response (must contain a TSIG)
    Message resp1 = new Message();
    TSIGRecord resp1Tsig = tsigMd5.generate(resp1, resp1.toWire(), Rcode.NOERROR, queryTsig, true);
    resp1.addRecord(resp1Tsig, Section.ADDITIONAL);
    resp1.tsigState = Message.TSIG_SIGNED;
    assertEquals(Rcode.NOERROR, verifier.verify(resp1, resp1.toWire()));

    // Second response – no TSIG (allowed because it is within the 99‑message window)
    Message resp2 = new Message();
    resp2.tsigState = Message.TSIG_INTERMEDIATE; // no TSIG added
    assertEquals(Rcode.NOERROR, verifier.verify(resp2, resp2.toWire()));

    // Final response – must contain a TSIG again
    Message resp3 = new Message();
    TSIGRecord resp3Tsig = tsigMd5.generate(resp3, resp3.toWire(), Rcode.NOERROR, null, true);
    resp3.addRecord(resp3Tsig, Section.ADDITIONAL);
    resp3.tsigState = Message.TSIG_SIGNED;
    assertEquals(Rcode.NOERROR, verifier.verify(resp3, resp3.toWire(), true));
  }

  @Test
  public void testStreamVerifierMissingRequiredSignature() {
    Message query = new Message();
    TSIGRecord queryTsig = tsigMd5.generate(query, query.toWire(), Rcode.NOERROR, null);
    query.addRecord(queryTsig, Section.ADDITIONAL);
    query.tsigState = Message.TSIG_SIGNED;

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);

    // First response – deliberately omit the required TSIG.
    Message resp = new Message();
    int result = verifier.verify(resp, resp.toWire());
    assertEquals(Rcode.FORMERR, result);
    assertEquals("missing required signature on first message", verifier.getErrorMessage());
  }

  @Test
  public void testStreamVerifierMissingSignatureAfter100Messages() {
    Message query = new Message();
    TSIGRecord queryTsig = tsigMd5.generate(query, query.toWire(), Rcode.NOERROR, null);
    query.addRecord(queryTsig, Section.ADDITIONAL);
    query.tsigState = Message.TSIG_SIGNED;

    // Use the package‑private constructor that allows us to set signEveryNthMessage = 100
    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsigMd5, queryTsig, 100);
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);

    // Simulate 101 intermediate messages without a TSIG.
    for (int i = 0; i < 101; i++) {
      Message m = new Message();
      int rc = verifier.verify(m, m.toWire());
      if (i < 99) {
        assertEquals(Rcode.NOERROR, rc);
      } else {
        // The 100th intermediate message must cause FORMERR.
        assertEquals(Rcode.FORMERR, rc);
        assertTrue(verifier.getErrorMessage().contains("Missing required signature on message #"));
      }
    }
  }

  /* -------------------------------------------------------------
   *  Helper constructor used in the 100‑message test above.
   * ------------------------------------------------------------- */
  // The StreamGenerator constructor with three arguments is package‑private.
  // We can access it from the test because the test resides in the same package
  // (org.xbill.DNS). No additional code is required.
}
