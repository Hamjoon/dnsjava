/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.TSIG}.
 *
 * The tests cover:
 * <ul>
 *   <li>Algorithm name conversion (including aliases, case‑insensitivity and error handling)</li>
 *   <li>Round‑trip signing and verification of a {@link org.xbill.DNS.Message}</li>
 *   <li>Behaviour when the signature is altered (BADSIG)</li>
 *   <li>Generation of a BADTIME record (error specific handling)</li>
 *   <li>Correct calculation of {@code recordLength()}</li>
 *   <li>Stream signing and verification using {@link TSIG.StreamGenerator} and {@link TSIG.StreamVerifier}</li>
 *   <li>Deprecated {@code fromString} factory method</li>
 * </ul>
 *
 * All objects are created through the public constructors / factories that exist in the
 * supplied source code – no mocks or subclassing of library types are used.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link TSIG}.
 */
public class TSIGTest {

  private static final String KEY_B64 = Base64.getEncoder().encodeToString("testkey".getBytes());
  private static final Name KEY_NAME = Name.fromString("tsig-key.");
  private static final Name ZONE_NAME = Name.fromString("example.com.");

  private TSIG tsigSha256;
  private Message queryMsg;

  @Before
  public void setUp() throws Exception {
    // Use the SHA‑256 algorithm for the majority of tests.
    tsigSha256 = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_B64);

    // A minimal query message (no question records – acceptable for signing tests).
    queryMsg = new Message();
    queryMsg.getHeader().setID(0x1234);
  }

  /* ---------------------------------------------------------------------- */
  /*  algorithmToName / nameToAlgorithm                                    */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_AliasesAndCaseInsensitivity() {
    // Alias "HMAC-MD5" (both with and without trailing dot)
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));

    // RFC name with different case
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HMAC-SHA256."));

    // Java algorithm name should also resolve
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_NullArgument() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_UnknownAlgorithm() {
    TSIG.algorithmToName("unknown-alg");
  }

  @Test
  public void testNameToAlgorithm_ValidAndInvalid() {
    // Valid mappings
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));

    // Invalid name should raise an exception
    try {
      TSIG.nameToAlgorithm(Name.fromString("nonexistent."));
      fail("Expected IllegalArgumentException for unknown algorithm name");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  /* ---------------------------------------------------------------------- */
  /*  Basic signing (apply) and verification                               */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testApplyAndVerify_Success() throws Exception {
    // Sign the message
    tsigSha256.apply(queryMsg, null);
    TSIGRecord tsigRec = queryMsg.getTSIG();
    assertNotNull("TSIG record should be present after apply()", tsigRec);

    // Verify using the same TSIG instance
    int result = tsigSha256.verify(queryMsg, queryMsg.toWire(), null);
    assertEquals("Verification should succeed (NOERROR)", Rcode.NOERROR, result);
    assertTrue("Message should be marked as verified", queryMsg.isVerified());
  }

  @Test
  public void testVerify_BadSignature() throws Exception {
    tsigSha256.apply(queryMsg, null);
    TSIGRecord rec = queryMsg.getTSIG();
    assertNotNull(rec);

    // Corrupt the signature (flip first byte)
    byte[] sig = rec.getSignature().clone();
    sig[0] ^= 0xFF;
    TSIGRecord corrupted = new TSIGRecord(
        rec.getName(),
        rec.dclass,
        rec.ttl,
        rec.getAlgorithm(),
        rec.getTimeSigned(),
        rec.getFudge(),
        sig,
        rec.getOriginalID(),
        rec.getError(),
        rec.getOther());

    // Replace the record in the message
    queryMsg.removeAllRecords(Section.ADDITIONAL);
    queryMsg.addRecord(corrupted, Section.ADDITIONAL);

    int result = tsigSha256.verify(queryMsg, queryMsg.toWire(), null);
    assertEquals("Corrupted signature should result in BADSIG", Rcode.BADSIG, result);
    assertFalse("Message should not be verified", queryMsg.isVerified());
  }

  /* ---------------------------------------------------------------------- */
  /*  BADTIME handling                                                      */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testGenerateBadTimeRecord() throws Exception {
    // Force a BADTIME error – the TSIGRecord should contain a 6‑byte "other" field
    TSIGRecord badTimeRec = tsigSha256.generate(
        queryMsg,
        queryMsg.toWire(),
        Rcode.BADTIME,
        null,
        true);

    assertEquals("Error code should be BADTIME", Rcode.BADTIME, badTimeRec.getError());
    assertNotNull("Other data should be present for BADTIME", badTimeRec.getOther());
    assertEquals("Other data length for BADTIME must be 6 bytes",
        6, badTimeRec.getOther().length);
  }

  /* ---------------------------------------------------------------------- */
  /*  recordLength() calculation                                            */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testRecordLength() {
    // Expected length = name + 10 + alg + 8 + 18 + 4 + 8 (see Javadoc)
    int expected = KEY_NAME.length()
        + 10
        + TSIG.HMAC_SHA256.length()
        + 8   // time signed + fudge
        + 18  // MAC length field + max MAC (16)
        + 4   // original id + error
        + 8;  // other length field + max other (6)

    assertEquals("recordLength() must match the documented formula",
        expected, tsigSha256.recordLength());
  }

  /* ---------------------------------------------------------------------- */
  /*  Deprecated fromString factory method                                   */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testFromString_DeprecatedFactory() {
    // "keyname:base64key"
    TSIG fromString = TSIG.fromString("tsig-key:" + KEY_B64);
    assertNotNull("TSIG instance created via fromString()", fromString);
    // Verify that signing works the same way as the regular constructor
    fromString.apply(queryMsg, null);
    assertNotNull("TSIG record should be present", queryMsg.getTSIG());
  }

  /* ---------------------------------------------------------------------- */
  /*  StreamGenerator + StreamVerifier (multi‑message)                     */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    // Prepare a query message with a TSIG (simulates a client request)
    TSIG queryTsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_B64);
    queryTsig.apply(queryMsg, null);
    TSIGRecord queryRec = queryMsg.getTSIG();
    assertNotNull(queryRec);

    // Initialise generator and verifier
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigSha256, queryRec);
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryRec);

    // Create three response messages – first and last must contain TSIG,
    // middle message may be unsigned (generator signs every message by default)
    Message resp1 = new Message();
    resp1.getHeader().setID(0x2001);
    generator.generate(resp1); // signed (first)

    Message resp2 = new Message();
    resp2.getHeader().setID(0x2002);
    generator.generate(resp2); // signed (second)

    Message resp3 = new Message();
    resp3.getHeader().setID(0x2003);
    generator.generate(resp3); // signed (third / last)

    // Verify each response in order
    int r1 = verifier.verify(resp1, resp1.toWire());
    assertEquals("First response should verify (NOERROR)", Rcode.NOERROR, r1);
    assertTrue(resp1.isVerified());

    int r2 = verifier.verify(resp2, resp2.toWire());
    assertEquals("Middle response should verify (NOERROR)", Rcode.NOERROR, r2);
    assertTrue(resp2.isVerified());

    // Last message verification – flag it as the last one to enforce presence of TSIG
    int r3 = verifier.verify(resp3, resp3.toWire(), true);
    assertEquals("Last response should verify (NOERROR)", Rcode.NOERROR, r3);
    assertTrue(resp3.isVerified());
  }

  /* ---------------------------------------------------------------------- */
  /*  Helper: create a fresh TSIG with a deterministic Clock (optional)      */
  /* ---------------------------------------------------------------------- */

  private TSIG createTSIGWithFixedClock(Instant instant) {
    Clock fixed = Clock.fixed(instant, Clock.systemUTC().getZone());
    return new TSIG(TSIG.HMAC_SHA256, KEY_NAME,
        new SecretKeySpec(base64.fromString(KEY_B64), "HmacSHA256"),
        fixed);
  }
}
