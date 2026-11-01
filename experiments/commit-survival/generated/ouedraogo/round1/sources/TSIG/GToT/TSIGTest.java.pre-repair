/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.TSIG}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public static methods (algorithmToName, nameToAlgorithm, fromString, getTsigFudge)</li>
 *   <li>All public constructors (including error paths)</li>
 *   <li>generate / apply / verify workflow (happy path)</li>
 *   <li>recordLength calculation</li>
 *   <li>edge‑case handling for signature length, time‑fudge and malformed inputs</li>
 *   <li>StreamGenerator validation (signEveryNthMessage bounds)</li>
 *   <li>StreamVerifier behaviour for missing required TSIG records</li>
 * </ul>
 *
 * The test class uses only the public API that is present in the supplied source file.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive tests for {@link TSIG}.
 */
public class TSIGTest {

  /* --------------------------------------------------------------------- */
  /*  Test data ----------------------------------------------------------- */
  /* --------------------------------------------------------------------- */

  private static final String TEST_KEY_B64 = "dGVzdGtleQ=="; // "testkey"
  private static final byte[] TEST_KEY_BYTES = base64.fromString(TEST_KEY_B64);
  private static final Name TEST_NAME = Name.fromString("example.com.");
  private static final Name TEST_ALG = TSIG.HMAC_MD5; // alias for MD5
  private static final int TEST_ID = 0x1234;

  private TSIG tsig; // a valid TSIG instance used by many tests

  @Before
  public void setUp() {
    tsig = new TSIG(TEST_ALG, TEST_NAME, TEST_KEY_B64);
  }

  /* --------------------------------------------------------------------- */
  /*  1.  Public static methods ------------------------------------------- */
  /* --------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_ValidAndAlias() {
    // direct RFC name
    assertEquals(TSIG.HMAC_SHA1,
        TSIG.algorithmToName("hmac-sha1."));
    // without trailing dot
    assertEquals(TSIG.HMAC_SHA1,
        TSIG.algorithmToName("hmac-sha1"));
    // alias for MD5
    assertEquals(TSIG.HMAC_MD5,
        TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5,
        TSIG.algorithmToName("HMAC-MD5."));
    // java algorithm name case‑insensitive
    assertEquals(TSIG.HMAC_SHA256,
        TSIG.algorithmToName("HmacSHA256"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Null() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Unknown() {
    TSIG.algorithmToName("unknown-alg");
  }

  @Test
  public void testNameToAlgorithm_Valid() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_Invalid() {
    // construct a Name that is not in the map
    Name bogus = Name.fromString("bogus-alg.");
    TSIG.nameToAlgorithm(bogus);
  }

  @Test
  public void testFromString_ValidTwoPart() {
    TSIG t = TSIG.fromString("example.com.:dGVzdGtleQ=="); // default MD5
    assertEquals(TEST_NAME, t.name);
    assertEquals(TSIG.HMAC_MD5, t.alg);
  }

  @Test
  public void testFromString_ValidThreePart() {
    TSIG t = TSIG.fromString("hmac-sha256:example.com.:dGVzdGtleQ==");
    assertEquals(TEST_NAME, t.name);
    assertEquals(TSIG.HMAC_SHA256, t.alg);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromString_InvalidFormat() {
    TSIG.fromString("onlyonepart");
  }

  @Test
  public void testGetTsigFudge_Default() {
    // With no option set the static method should return the constant FUDGE (300 s)
    Duration d = TSIG.getTsigFudge();
    assertEquals(TSIG.FUDGE, d);
  }

  /* --------------------------------------------------------------------- */
  /*  2.  Constructors – happy path --------------------------------------- */
  /* --------------------------------------------------------------------- */

  @Test
  public void testConstructor_NameAndBase64() {
    TSIG t = new TSIG(TSIG.HMAC_SHA1, TEST_NAME, TEST_KEY_B64);
    assertNotNull(t);
    assertEquals(TSIG.HMAC_SHA1, t.alg);
    assertEquals(TEST_NAME, t.name);
  }

  @Test
  public void testConstructor_NameAndBytes() {
    TSIG t = new TSIG(TSIG.HMAC_SHA384, TEST_NAME, TEST_KEY_BYTES);
    assertNotNull(t);
    assertEquals(TSIG.HMAC_SHA384, t.alg);
    assertEquals(TEST_NAME, t.name);
  }

  @Test
  public void testConstructor_NameAndSecretKey() {
    SecretKey sk = new SecretKeySpec(TEST_KEY_BYTES, "HmacSHA512");
    TSIG t = new TSIG(TSIG.HMAC_SHA512, TEST_NAME, sk);
    assertNotNull(t);
    assertEquals(TSIG.HMAC_SHA512, t.alg);
  }

  @Test
  public void testConstructor_WithClock() {
    Clock fixed = Clock.fixed(Instant.now(), Clock.systemUTC().getZone());
    SecretKey sk = new SecretKeySpec(TEST_KEY_BYTES, "HmacMD5");
    TSIG t = new TSIG(TSIG.HMAC_MD5, TEST_NAME, sk, fixed);
    assertNotNull(t);
    assertEquals(fixed, t.clock);
  }

  /* --------------------------------------------------------------------- */
  /*  3.  Constructors – error handling ----------------------------------- */
  /* --------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidBase64() {
    new TSIG(TSIG.HMAC_MD5, TEST_NAME, "!!! not base64 !!!");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidNameString() {
    // Name.fromString will throw TextParseException which is wrapped as IllegalArgumentException
    new TSIG(TSIG.HMAC_MD5, "invalid name!", TEST_KEY_B64);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testStreamGenerator_InvalidSignEveryNth() {
    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsig, null, 0); // 0 is out of range
  }

  /* --------------------------------------------------------------------- */
  /*  4.  Basic generate / apply / verify workflow ------------------------ */
  /* --------------------------------------------------------------------- */

  @Test
  public void testGenerateApplyVerify_HappyPath() throws Exception {
    // Build a simple query message
    Message query = new Message(TEST_ID);
    query.getHeader().setID(TEST_ID);

    // Apply a TSIG to the query (adds the record)
    tsig.apply(query, null);
    TSIGRecord tsigRec = query.getTSIG();
    assertNotNull("TSIG record should be added to the query", tsigRec);

    // Verify the signed query – should succeed
    int result = tsig.verify(query, query.toWire(), null);
    assertEquals("Verification should return NOERROR", Rcode.NOERROR, result);
    assertTrue("Message should be marked verified", query.isVerified());
  }

  @Test
  public void testRecordLength() {
    int expected = tsig.name.length() + 10 + tsig.alg.length() + 8 + 18 + 4 + 8;
    assertEquals(expected, tsig.recordLength());
  }

  /* --------------------------------------------------------------------- */
  /*  5.  Verify edge cases – signature length handling ------------------- */
  /* --------------------------------------------------------------------- */

  @Test
  public void testVerifySignature_TooShort() throws Exception {
    // Create a TSIG with a known key
    Message msg = new Message();
    tsig.apply(msg, null);
    TSIGRecord rec = msg.getTSIG();

    // Manipulate the signature to be too short (e.g., truncate to 5 bytes)
    byte[] shortSig = new byte[5];
    System.arraycopy(rec.getSignature(), 0, shortSig, 0, 5);
    TSIGRecord badRec = new TSIGRecord(
        rec.getName(),
        rec.dclass,
        rec.ttl,
        rec.getAlgorithm(),
        rec.getTimeSigned(),
        rec.getFudge(),
        shortSig,
        rec.getOriginalID(),
        rec.getError(),
        rec.getOther());

    msg.removeAllRecords(Section.ADDITIONAL);
    msg.addRecord(badRec, Section.ADDITIONAL);
    int result = tsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADSIG, result);
  }

  @Test
  public void testVerifySignature_TooLong() throws Exception {
    Message msg = new Message();
    tsig.apply(msg, null);
    TSIGRecord rec = msg.getTSIG();

    // Extend the signature beyond the HMAC length (e.g., add extra bytes)
    byte[] longSig = new byte[rec.getSignature().length + 10];
    System.arraycopy(rec.getSignature(), 0, longSig, 0, rec.getSignature().length);
    TSIGRecord badRec = new TSIGRecord(
        rec.getName(),
        rec.dclass,
        rec.ttl,
        rec.getAlgorithm(),
        rec.getTimeSigned(),
        rec.getFudge(),
        longSig,
        rec.getOriginalID(),
        rec.getError(),
        rec.getOther());

    msg.removeAllRecords(Section.ADDITIONAL);
    msg.addRecord(badRec, Section.ADDITIONAL);
    int result = tsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADSIG, result);
  }

  /* --------------------------------------------------------------------- */
  /*  6.  StreamGenerator – basic usage ----------------------------------- */
  /* --------------------------------------------------------------------- */

  @Test
  public void testStreamGenerator_Basic() throws Exception {
    Message query = new Message();
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsig, queryTsig);
    Message response = new Message();
    gen.generate(response);
    TSIGRecord respTsig = response.getTSIG();
    assertNotNull("Response should contain a TSIG record", respTsig);
    // Verify the response using the same TSIG instance
    assertEquals(Rcode.NOERROR, tsig.verify(response, response.toWire(), queryTsig));
  }

  /* --------------------------------------------------------------------- */
  /*  7.  StreamVerifier – missing required signatures -------------------- */
  /* --------------------------------------------------------------------- */

  @Test
  public void testStreamVerifier_MissingFirstSignature() throws Exception {
    // First message without TSIG – should be FORMERR
    Message first = new Message();
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, null);
    int result = verifier.verify(first, first.toWire());
    assertEquals(Rcode.FORMERR, result);
  }

  @Test
  public void testStreamVerifier_MissingIntermediateAfter100() throws Exception {
    // Prepare a query with a TSIG (first message)
    Message query = new Message();
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // First verification (should succeed)
    assertEquals(Rcode.NOERROR, verifier.verify(query, query.toWire()));

    // 99 intermediate messages without TSIG – should all succeed
    for (int i = 0; i < 99; i++) {
      Message interim = new Message();
      assertEquals(Rcode.NOERROR, verifier.verify(interim, interim.toWire()));
    }

    // 101st message (i.e. after 100 messages since last signed) without TSIG – should fail
    Message bad = new Message();
    int result = verifier.verify(bad, bad.toWire());
    assertEquals(Rcode.FORMERR, result);
    assertEquals("Missing required signature on message #101", verifier.getErrorMessage());
  }

  /* --------------------------------------------------------------------- */
  /*  8.  verifyTime – fudge handling ------------------------------------ */
  /* --------------------------------------------------------------------- */

  @Test
  public void testVerifyTime_WithinFudge() throws Exception {
    // Use a fixed clock so we can manipulate the TSIG timestamp
    Instant now = Instant.now();
    Clock fixed = Clock.fixed(now, Clock.systemUTC().getZone());

    SecretKey sk = new SecretKeySpec(TEST_KEY_BYTES, "HmacMD5");
    TSIG fixedTsig = new TSIG(TSIG.HMAC_MD5, TEST_NAME, sk, fixed);

    Message query = new Message();
    fixedTsig.apply(query, null);
    TSIGRecord rec = query.getTSIG();

    // The timestamp in the record should be exactly 'now' – verification should pass
    int result = fixedTsig.verify(query, query.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
  }

  @Test
  public void testVerifyTime_OutsideFudge() throws Exception {
    // Clock that is far in the past relative to the TSIG's timeSigned
    Clock farPast = Clock.fixed(Instant.EPOCH, Clock.systemUTC().getZone());

    SecretKey sk = new SecretKeySpec(TEST_KEY_BYTES, "HmacMD5");
    TSIG tsigPast = new TSIG(TSIG.HMAC_MD5, TEST_NAME, sk, farPast);

    Message query = new Message();
    // Apply with a normal (current) clock – the signature will contain 'now'
    tsig.apply(query, null);
    TSIGRecord rec = query.getTSIG();

    // Verify using a verifier whose clock is far in the past – should trigger BADTIME
    int result = tsigPast.verify(query, query.toWire(), null);
    assertEquals(Rcode.BADTIME, result);
  }

  /* --------------------------------------------------------------------- */
  /*  9.  Helper methods (none needed – all logic in tests) ---------------- */
  /* --------------------------------------------------------------------- */
}
