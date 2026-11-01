package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.Mac;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link TSIG}. The tests cover the public API, algorithm conversion utilities,
 * record generation, application, verification, and the stream helper classes.
 *
 * <p>All tests use real DNS objects – no mocking – and rely only on the classes and methods that
 * appear in the supplied source code or the public API listed in the prompt.
 */
public class TSIGTest {

  private static final String BASE64_KEY = "AAAAAAAAAAAAAAAAAAAAAA=="; // 16 zero bytes
  private Name keyName;
  private TSIG tsigSha256;
  private TSIG tsigSha256Trunc;
  private TSIG tsigMd5;
  private Message emptyMessage;

  @Before
  public void setUp() throws Exception {
    keyName = Name.fromString("testkey.");
    // Normal HMAC‑SHA256 key
    tsigSha256 = new TSIG(TSIG.HMAC_SHA256, keyName, BASE64_KEY);
    // Truncated HMAC‑SHA256‑128 (signature length 16)
    tsigSha256Trunc = new TSIG(TSIG.HMAC_SHA256_128, keyName, BASE64_KEY);
    // HMAC‑MD5 (legacy) key
    tsigMd5 = new TSIG(TSIG.HMAC_MD5, keyName, BASE64_KEY);
    // Simple empty DNS message with a deterministic ID
    emptyMessage = new Message(0x1234);
  }

  /** ---------------------------------------------------------------------- *
   *  Algorithm conversion utilities
   *  ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_StandardNames() {
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1"));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5"));
    // Java style name should be converted
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
    // Truncated Java style name
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
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256_128));
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_Unknown() {
    Name unknown = Name.fromString("unknown-alg.");
    TSIG.nameToAlgorithm(unknown);
  }

  /** ---------------------------------------------------------------------- *
   *  Record length computation
   *  ---------------------------------------------------------------------- */

  @Test
  public void testRecordLengthMatchesComputation() {
    int expected =
        keyName.length()
            + 10
            + TSIG.HMAC_SHA256.length()
            + 8 // time signed + fudge
            + 2 // MAC length field
            + TSIG.algLengthMap.get(TSIG.HMAC_SHA256) // full SHA‑256 MAC size
            + 4 // original ID + error
            + 8; // other data length + max error field
    assertEquals(expected, tsigSha256.recordLength());
  }

  /** ---------------------------------------------------------------------- *
   *  Basic signing and verification
   *  ---------------------------------------------------------------------- */

  @Test
  public void testApplyAndVerify_Success() throws Exception {
    // Sign the empty message
    tsigSha256.apply(emptyMessage, null);
    TSIGRecord tsigRecord = emptyMessage.getTSIG();
    assertNotNull("TSIG record should be present after apply()", tsigRecord);
    // Verify with the same key – should succeed
    int result = tsigSha256.verify(emptyMessage, emptyMessage.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue(emptyMessage.isVerified());
  }

  @Test
  public void testVerify_BadKey() throws Exception {
    tsigSha256.apply(emptyMessage, null);
    // Verify with a different key (different secret)
    TSIG other = new TSIG(TSIG.HMAC_SHA256, keyName, "BBBBBBBBBBBBBBBBBBBBBB==");
    int result = other.verify(emptyMessage, emptyMessage.toWire(), null);
    assertEquals(Rcode.BADKEY, result);
    assertFalse(emptyMessage.isVerified());
  }

  /** ---------------------------------------------------------------------- *
   *  Bad time handling – use a fixed clock far from the verification clock
   *  ---------------------------------------------------------------------- */

  @Test
  public void testVerify_BadTime() throws Exception {
    // Clock for signing that is 10 minutes in the past
    Clock pastClock = Clock.fixed(Instant.now().minus(Duration.ofMinutes(10)), ZoneOffset.UTC);
    TSIG tsigPast = new TSIG(TSIG.HMAC_SHA256, keyName, BASE64_KEY, pastClock);
    tsigPast.apply(emptyMessage, null);
    // Verify with the default clock (now) – should report BADTIME
    int result = tsigPast.verify(emptyMessage, emptyMessage.toWire(), null);
    assertEquals(Rcode.BADTIME, result);
    assertFalse(emptyMessage.isVerified());
  }

  /** ---------------------------------------------------------------------- *
   *  Truncated signature handling (HMAC‑SHA256‑128)
   *  ---------------------------------------------------------------------- */

  @Test
  public void testTruncatedSignatureLength() throws Exception {
    tsigSha256Trunc.apply(emptyMessage, null);
    TSIGRecord rec = emptyMessage.getTSIG();
    assertNotNull(rec);
    // The MAC length for the truncated algorithm is defined as 16 bytes
    assertEquals(16, rec.getSignature().length);
    // Verify still succeeds
    int result = tsigSha256Trunc.verify(emptyMessage, emptyMessage.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
  }

  /** ---------------------------------------------------------------------- *
   *  Stream generation and verification
   *  ---------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    // Create a query message and sign it (simulating a client request)
    Message query = new Message(0x1111);
    tsigSha256.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();
    assertNotNull(queryTsig);

    // StreamGenerator will sign a series of response messages
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigSha256, queryTsig);

    // First response – signed
    Message resp1 = new Message(0x2222);
    generator.generate(resp1);
    TSIGRecord resp1Tsig = resp1.getTSIG();
    assertNotNull(resp1Tsig);
    assertEquals(Rcode.NOERROR, resp1.getRcode());

    // Second response – also signed because signEveryNthMessage default is 1
    Message resp2 = new Message(0x3333);
    generator.generate(resp2);
    TSIGRecord resp2Tsig = resp2.getTSIG();
    assertNotNull(resp2Tsig);

    // Verify the stream with StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryTsig);
    int v1 = verifier.verify(resp1, resp1.toWire(), true); // lastMessage = true for simplicity
    int v2 = verifier.verify(resp2, resp2.toWire(), true);

    assertEquals(Rcode.NOERROR, v1);
    assertEquals(Rcode.NOERROR, v2);
    assertTrue(resp1.isVerified());
    assertTrue(resp2.isVerified());
    assertNull("No FORMERR error message expected", verifier.getErrorMessage());
  }

  /** ---------------------------------------------------------------------- *
   *  Constructor overloads and deprecated helpers
   *  ---------------------------------------------------------------------- */

  @Test
  public void testDeprecatedStringConstructor() {
    // Using the deprecated String‑based constructor (algorithm omitted → defaults to MD5)
    TSIG deprecated = new TSIG("testkey", BASE64_KEY);
    assertEquals(TSIG.HMAC_MD5, deprecated.alg);
    assertEquals(keyName, deprecated.name);
  }

  @Test
  public void testFromStringFactory() {
    // "alg:name:key" format
    TSIG fromStr = TSIG.fromString(TSIG.HMAC_SHA1.toString() + ":testkey:" + BASE64_KEY);
    assertEquals(TSIG.HMAC_SHA1, fromStr.alg);
    assertEquals(keyName, fromStr.name);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromStringInvalidFormat() {
    TSIG.fromString("invalid");
  }

  /** ---------------------------------------------------------------------- *
   *  Private helper verification (indirectly exercised via public methods)
   *  ---------------------------------------------------------------------- */

  @Test
  public void testVerifySignatureTooShort() throws Exception {
    // Build a TSIG with an intentionally short signature (1 byte) by using a bogus record.
    // We create a TSIGRecord manually and then call the private verifySignature through the
    // public verify() path.
    byte[] shortSig = new byte[] {0x00};
    TSIGRecord bogus = new TSIGRecord(
        keyName,
        DClass.ANY,
        0,
        TSIG.HMAC_SHA256,
        Instant.now(),
        Duration.ofSeconds(300),
        shortSig,
        emptyMessage.getHeader().getID(),
        Rcode.NOERROR,
        null);
    emptyMessage.addRecord(bogus, Section.ADDITIONAL);
    // The verify method will compare the short signature against the expected MAC length
    int result = tsigSha256.verify(emptyMessage, emptyMessage.toWire(), null);
    // Expected to fail with BADSIG because the signature is shorter than the minimum digest length
    assertEquals(Rcode.BADSIG, result);
  }
}
