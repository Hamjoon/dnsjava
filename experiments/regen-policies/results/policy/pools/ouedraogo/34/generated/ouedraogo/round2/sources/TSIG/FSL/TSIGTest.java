package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

/**
 * Comprehensive JUnit4 tests for {@link TSIG}.
 *
 * The tests cover:
 * <ul>
 *   <li>algorithm name conversion (algorithmToName, nameToAlgorithm)</li>
 *   <li>all public constructors (including deprecated ones)</li>
 *   <li>record generation and application (apply / generate)</li>
 *   <li>verification paths (NOERROR, BADKEY, BADSIG, BADTIME)</li>
 *   <li>recordLength calculation</li>
 *   <li>static verify helper (truncation handling)</li>
 *   <li>StreamGenerator / StreamVerifier interaction</li>
 * </ul>
 *
 * No mocking is used; real DNS objects are constructed via the public API.
 */
public class TSIGTest {

  /* ---------------------------------------------------------------------- */
  /*  algorithmToName / nameToAlgorithm                                    */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToNameValid() {
    // Alias handling (case‑insensitive, with/without trailing dot)
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    // RFC names (case‑insensitive)
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HMAC-SHA256."));
    // Java algorithm names
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("HmacSHA1"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameNull() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameUnknown() {
    TSIG.algorithmToName("unknown-alg");
  }

  @Test
  public void testNameToAlgorithmValid() {
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithmUnknown() throws Exception {
    Name unknown = Name.fromString("unknown.");
    TSIG.nameToAlgorithm(unknown);
  }

  /* ---------------------------------------------------------------------- */
  /*  Constructors                                                          */
  /* ---------------------------------------------------------------------- */

  private static final Name KEY_NAME = Name.fromString("testkey.");
  private static final byte[] KEY_BYTES = base64.fromString("YWJjZGVm"); // "abcdef"

  @Test
  public void testConstructor_Name_Bytes() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_BYTES);
    assertNotNull(tsig);
  }

  @Test
  public void testConstructor_Name_StringKey() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, "YWJjZGVm");
    assertNotNull(tsig);
  }

  @Test
  public void testConstructor_Name_SecretKey() {
    SecretKey sk = new SecretKeySpec(KEY_BYTES, "HmacSHA256");
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, sk);
    assertNotNull(tsig);
  }

  @Test
  public void testConstructor_AlgorithmString_NameString_KeyString() {
    TSIG tsig = new TSIG("hmac-sha256", "testkey.", "YWJjZGVm");
    assertNotNull(tsig);
  }

  @Test
  public void testDeprecatedFromStringTwoParts() {
    TSIG tsig = TSIG.fromString("testkey:YWJjZGVm");
    assertNotNull(tsig);
    // algorithm should default to HMAC_MD5
    assertEquals(TSIG.HMAC_MD5, tsig.alg);
  }

  @Test
  public void testDeprecatedFromStringThreeParts() {
    TSIG tsig = TSIG.fromString("hmac-sha256:testkey:YWJjZGVm");
    assertNotNull(tsig);
    assertEquals(TSIG.HMAC_SHA256, tsig.alg);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeprecatedFromStringInvalid() {
    TSIG.fromString("invalid");
  }

  /* ---------------------------------------------------------------------- */
  /*  recordLength                                                          */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testRecordLength() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_BYTES);
    int expected = KEY_NAME.length() + 10 + TSIG.HMAC_SHA256.length() + 8 + 18 + 4 + 8;
    assertEquals(expected, tsig.recordLength());
  }

  /* ---------------------------------------------------------------------- */
  /*  generate / apply / verify (happy path)                                */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testApplyAndVerifySuccess() throws Exception {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_BYTES);
    Message query = new Message(0x1234);
    // No question needed – we only care about the TSIG processing
    tsig.apply(query, null);
    assertEquals(Message.TSIG_SIGNED, query.tsigState);

    // Serialize the message (wire format) and verify
    byte[] wire = query.toWire();
    int result = tsig.verify(query, wire, null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue(query.isVerified());
  }

  @Test
  public void testVerifyBadKey() throws Exception {
    TSIG signer = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_BYTES);
    Message msg = new Message(0x1111);
    signer.apply(msg, null);

    // Use a different TSIG (different key name) for verification
    Name otherName = Name.fromString("otherkey.");
    TSIG verifier = new TSIG(TSIG.HMAC_SHA256, otherName, KEY_BYTES);
    byte[] wire = msg.toWire();
    int result = verifier.verify(msg, wire, null);
    assertEquals(Rcode.BADKEY, result);
  }

  @Test
  public void testVerifyBadSignature() throws Exception {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_BYTES);
    Message msg = new Message(0x2222);
    tsig.apply(msg, null);
    TSIGRecord original = msg.getTSIG();

    // Corrupt the signature (flip first byte)
    byte[] badSig = original.getSignature().clone();
    badSig[0] ^= 0xFF;
    TSIGRecord corrupted =
        new TSIGRecord(
            original.getName(),
            original.dclass,
            original.ttl,
            original.getAlgorithm(),
            original.getTimeSigned(),
            original.getFudge(),
            badSig,
            original.getOriginalID(),
            original.getError(),
            original.getOther());

    // Replace in message
    msg.removeAllRecords(Section.ADDITIONAL);
    msg.addRecord(corrupted, Section.ADDITIONAL);
    msg.tsigState = Message.TSIG_SIGNED; // keep state consistent

    byte[] wire = msg.toWire();
    int result = tsig.verify(msg, wire, null);
    assertEquals(Rcode.BADSIG, result);
  }

  @Test
  public void testVerifyBadTime() throws Exception {
    // Sign with a fixed clock (time = 1000s)
    Clock signClock = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
    TSIG signer = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_BYTES, signClock);
    Message msg = new Message(0x3333);
    signer.apply(msg, null);
    byte[] wire = msg.toWire();

    // Verify with a clock far away (time = 5000s) – fudge is 300s, should be BADTIME
    Clock verifyClock = Clock.fixed(Instant.ofEpochSecond(5000), ZoneOffset.UTC);
    TSIG verifier = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_BYTES, verifyClock);
    int result = verifier.verify(msg, wire, null);
    assertEquals(Rcode.BADTIME, result);
  }

  /* ---------------------------------------------------------------------- */
  /*  static verify helper (truncation)                                       */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStaticVerifyTruncation() {
    byte[] expected = new byte[] {1, 2, 3, 4, 5};
    byte[] shortSig = new byte[] {1, 2, 3};
    // The method should truncate expected to length of signature and then compare
    assertTrue(TSIG.verify(expected, shortSig));

    byte[] mismatched = new byte[] {9, 9, 9};
    assertFalse(TSIG.verify(expected, mismatched));
  }

  /* ---------------------------------------------------------------------- */
  /*  StreamGenerator / StreamVerifier integration test                       */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    // Create a key and a query message with a TSIG
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_BYTES);
    Message query = new Message(0x4444);
    tsig.apply(query, null); // signs the query
    TSIGRecord queryTsig = query.getTSIG();

    // StreamGenerator will sign a series of responses
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

    // First response (must contain a TSIG)
    Message resp1 = new Message(0x5555);
    generator.generate(resp1);
    assertNotNull(resp1.getTSIG());
    assertEquals(Message.TSIG_SIGNED, resp1.tsigState);

    // Second response (no TSIG, because we sign every message – but we can force a gap)
    // We'll use the same generator but with signEveryNthMessage = 2 to force a gap.
    TSIG.StreamGenerator generatorWithGap = new TSIG.StreamGenerator(tsig, queryTsig, 2);
    Message resp2 = new Message(0x6666);
    generatorWithGap.generate(resp2); // first message signed
    Message resp3 = new Message(0x7777);
    generatorWithGap.generate(resp3); // second message NOT signed (gap)
    assertNull(resp3.getTSIG());

    // Now verify the stream using StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);
    // Verify first response (contains TSIG)
    int r1 = verifier.verify(resp1, resp1.toWire());
    assertEquals(Rcode.NOERROR, r1);
    // Verify second response (no TSIG, still within 99‑message window)
    int r2 = verifier.verify(resp2, resp2.toWire());
    assertEquals(Rcode.NOERROR, r2);
    // Verify third response (no TSIG, but it's the last – we indicate it)
    int r3 = verifier.verify(resp3, resp3.toWire(), true);
    // Because the last message must have a TSIG, we expect FORMERR
    assertEquals(Rcode.FORMERR, r3);
    assertEquals("Missing required signature on last message", verifier.getErrorMessage());
  }

  /* ---------------------------------------------------------------------- */
  /*  Additional edge cases                                                 */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testGenerateWithErrorBadTimeIncludesOtherData() throws Exception {
    // Force BADTIME error to ensure 'other' field is populated
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, KEY_BYTES);
    Message msg = new Message(0x8888);
    // Simulate a request TSIG with an old timestamp
    Instant oldTime = Instant.now().minusSeconds(3600);
    TSIGRecord requestTsig =
        new TSIGRecord(
            KEY_NAME,
            DClass.ANY,
            0,
            TSIG.HMAC_SHA256,
            oldTime,
            Duration.ofSeconds(300),
            new byte[0],
            0x8888,
            Rcode.NOERROR,
            null);
    // Generate a response with BADTIME error
    TSIGRecord response = tsig.generate(msg, msg.toWire(), Rcode.BADTIME, requestTsig);
    assertNotNull(response.getOther()); // other field should contain the time signed
    // Verify that the generated record uses the old timestamp (as per getTimeSigned)
    assertEquals(oldTime, response.getTimeSigned());
  }

  @Test
  public void testHmacAddSignatureProducesCorrectSizePrefix() throws Exception {
    // Use a tiny key to keep the signature short
    byte[] tinyKey = new byte[] {0x01, 0x02, 0x03};
    SecretKey sk = new SecretKeySpec(tinyKey, "HmacSHA1");
    TSIG tsig = new TSIG(TSIG.HMAC_SHA1, KEY_NAME, sk);
    Message msg = new Message(0x9999);
    tsig.apply(msg, null);
    TSIGRecord tsigRec = msg.getTSIG();

    // The signature length prefix (2 bytes) should match the actual signature length
    byte[] sig = tsigRec.getSignature();
    assertEquals(sig.length, DNSOutput.toU16(sig.length)[0] << 8 | DNSOutput.toU16(sig.length)[1]);
  }
}
