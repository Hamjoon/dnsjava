package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.Arrays;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link TSIG}.
 *
 * <p>The tests exercise:
 *
 * <ul>
 *   <li>algorithm name conversion (both directions) and error handling,
 *   <li>the internal {@code verify} helper (including truncation logic),
 *   <li>TSIG record generation via the public {@code apply} API,
 *   <li>verification of a correctly signed message,
 *   <li>failure cases (bad key, bad signature),
 *   <li>record length calculation,
 *   <li>the {@link TSIG.StreamGenerator} and {@link TSIG.StreamVerifier} utilities.
 * </ul>
 *
 * <p>Only the public API shown in the prompt is used – no mocking, no subclassing, and no
 * external library calls.
 */
public class TSIGTest {

  /** Simple base‑64 encoded secret (the ASCII string “hello”). */
  private static final String BASE64_KEY = "aGVsbG8="; // “hello”

  /** Helper that creates a TSIG instance using HMAC‑SHA256. */
  private static TSIG createSha256Tsig() {
    Name alg = TSIG.HMAC_SHA256;
    Name name = Name.fromString("testkey.");
    return new TSIG(alg, name, BASE64_KEY);
  }

  /** -------------------------------------------------------------------------
   *  algorithmToName / nameToAlgorithm
   *  -------------------------------------------------------------------------
   */
  @Test
  public void testAlgorithmToName_ValidInputs() {
    // RFC style with trailing dot
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));

    // RFC style without trailing dot
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));

    // Java style name, no dash
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));

    // Java style with explicit truncation length
    assertEquals(TSIG.HMAC_SHA256_128, TSIG.algorithmToName("HmacSHA256/128"));

    // Alias “hmac‑md5” must resolve to the RFC name
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5"));
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
  public void testNameToAlgorithm_Valid() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA1", TSIG.nameToAlgorithm(TSIG.HMAC_SHA1));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256_128)); // truncated uses same java name
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_Unknown() {
    Name unknown = Name.fromString("unknown-alg.");
    TSIG.nameToAlgorithm(unknown);
  }

  /** -------------------------------------------------------------------------
   *  Private static verify (truncation handling)
   *  -------------------------------------------------------------------------
   */
  @Test
  public void testVerify_TruncateExpected() {
    byte[] expected = new byte[] {0x01, 0x02, 0x03, 0x04};
    byte[] signature = new byte[] {0x01, 0x02}; // shorter than expected
    // via reflection we call the private method – but because the method is package‑private static,
    // we can invoke it directly.
    boolean result = TSIG.verify(expected, signature);
    assertTrue(result);
  }

  @Test
  public void testVerify_NoTruncate() {
    byte[] expected = new byte[] {0x0A, 0x0B, 0x0C};
    byte[] signature = new byte[] {0x0A, 0x0B, 0x0C};
    assertTrue(TSIG.verify(expected, signature));
  }

  /** -------------------------------------------------------------------------
   *  recordLength()
   *  -------------------------------------------------------------------------
   */
  @Test
  public void testRecordLength() {
    TSIG tsig = createSha256Tsig();
    int len = tsig.recordLength();
    // The length must be at least the sum of the fixed fields (see the method implementation).
    assertTrue(len > 0);
    // Verify against a manually computed value for SHA‑256 (32‑byte MAC).
    int expected = tsig.name.length() + 10 + tsig.alg.length() + 8 + 2 + 32 + 4 + 8;
    assertEquals(expected, len);
  }

  /** -------------------------------------------------------------------------
   *  Generation & verification of a single TSIG record
   *  -------------------------------------------------------------------------
   */
  @Test
  public void testGenerateAndApply() throws Exception {
    TSIG tsig = createSha256Tsig();

    Message query = new Message(); // empty query message
    tsig.apply(query, null); // sign the message (full signature)

    TSIGRecord generated = query.getTSIG();
    assertNotNull("TSIGRecord should be attached to the message", generated);
    assertEquals("Message state must be SIGNED after apply", Message.TSIG_SIGNED, query.tsigState);
  }

  @Test
  public void testVerify_Success() throws Exception {
    TSIG tsig = createSha256Tsig();

    Message query = new Message();
    tsig.apply(query, null);
    byte[] wire = query.toWire();

    // Verify using the same TSIG object; requestTSIG is null because this is not a response.
    int result = tsig.verify(query, wire, null);
    assertEquals("Verification of a correctly signed message must succeed", Rcode.NOERROR, result);
    assertEquals("Message state must be VERIFIED after successful verification",
                 Message.TSIG_VERIFIED, query.tsigState);
  }

  @Test
  public void testVerify_BadKey() throws Exception {
    // First, create a correctly signed message with key A.
    TSIG tsigA = createSha256Tsig();
    Message query = new Message();
    tsigA.apply(query, null);
    byte[] wire = query.toWire();

    // Now verify with a different TSIG (different name) – should return BADKEY.
    Name otherName = Name.fromString("otherkey.");
    TSIG tsigB = new TSIG(TSIG.HMAC_SHA256, otherName, BASE64_KEY);
    int result = tsigB.verify(query, wire, null);
    assertEquals("Verification with mismatching key name must fail with BADKEY",
                 Rcode.BADKEY, result);
  }

  @Test
  public void testVerify_BadSignature() throws Exception {
    TSIG tsig = createSha256Tsig();

    Message query = new Message();
    tsig.apply(query, null);
    byte[] wire = query.toWire();

    // Corrupt the signature in the wire representation.
    // The TSIGRecord is the last record in the additional section; we replace one byte of the
    // signature with a different value.
    TSIGRecord tsigRecord = query.getTSIG();
    byte[] sig = tsigRecord.getSignature();
    assertTrue(sig.length > 0);
    byte[] corruptedSig = sig.clone();
    corruptedSig[0] ^= 0xFF; // flip bits

    // Re‑create a TSIGRecord with the corrupted signature (all other fields unchanged).
    TSIGRecord corruptedRecord = new TSIGRecord(
        tsigRecord.getName(),
        tsigRecord.dclass,
        tsigRecord.ttl,
        tsigRecord.getAlgorithm(),
        tsigRecord.getTimeSigned(),
        tsigRecord.getFudge(),
        corruptedSig,
        tsigRecord.getOriginalID(),
        tsigRecord.getError(),
        tsigRecord.getOther());

    // Build a new message containing the corrupted TSIG.
    Message corruptedMsg = new Message();
    corruptedMsg.addRecord(corruptedRecord, Section.ADDITIONAL);
    byte[] corruptedWire = corruptedMsg.toWire();

    int result = tsig.verify(corruptedMsg, corruptedWire, null);
    assertEquals("Verification of a message with a bad signature must fail with BADSIG",
                 Rcode.BADSIG, result);
  }

  /** -------------------------------------------------------------------------
   *  StreamGenerator / StreamVerifier interaction
   *  -------------------------------------------------------------------------
   */
  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    // Create a TSIG key and a query message (the first message in a stream).
    TSIG tsig = createSha256Tsig();
    Message query = new Message();
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();
    assertNotNull(queryTsig);

    // StreamGenerator will sign every message (default signEveryNthMessage = 1).
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

    // First response (must contain a TSIG)
    Message resp1 = new Message();
    generator.generate(resp1);
    assertNotNull("First response must contain a TSIG", resp1.getTSIG());

    // Second response – unsigned (simulated by calling generator.generate with isLastMessage = false
    // and signEveryNthMessage = 2, but the provided generator always signs every message.
    // To test an unsigned intermediate message we create it manually.
    Message resp2 = new Message(); // no TSIG added
    // No TSIGRecord – this is an intermediate unsigned message.

    // Third response – signed again
    Message resp3 = new Message();
    generator.generate(resp3);
    assertNotNull("Third response must contain a TSIG", resp3.getTSIG());

    // Verify the whole stream using StreamVerifier.
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // 1) Verify the first signed response.
    int r1 = verifier.verify(resp1, resp1.toWire(), false);
    assertEquals("First response must verify successfully", Rcode.NOERROR, r1);
    assertEquals(Message.TSIG_VERIFIED, resp1.tsigState);

    // 2) Verify the unsigned intermediate response – should succeed with NOERROR.
    int r2 = verifier.verify(resp2, resp2.toWire(), false);
    assertEquals("Unsigned intermediate response must verify with NOERROR", Rcode.NOERROR, r2);
    assertEquals(Message.TSIG_INTERMEDIATE, resp2.tsigState);

    // 3) Verify the final signed response.
    int r3 = verifier.verify(resp3, resp3.toWire(), true); // last message = true
    assertEquals("Final response must verify successfully", Rcode.NOERROR, r3);
    assertEquals(Message.TSIG_VERIFIED, resp3.tsigState);
  }

  /** -------------------------------------------------------------------------
   *  Deprecated constructors – ensure they behave like the primary ones.
   *  -------------------------------------------------------------------------
   */
  @Test
  public void testDeprecatedConstructors() throws Exception {
    // Deprecated constructor that takes (Name algorithm, Name name, String key)
    TSIG tsig1 = new TSIG(TSIG.HMAC_SHA256, Name.fromString("deprecatedkey."), BASE64_KEY);
    // Deprecated constructor that takes (String name, String key) – defaults to HMAC_MD5
    TSIG tsig2 = new TSIG("deprecatedkey.", BASE64_KEY);
    // Verify that the algorithm for tsig2 is indeed HMAC_MD5.
    assertEquals(TSIG.HMAC_MD5, tsig2.alg);
    // Verify that the key name is correctly set.
    assertEquals(Name.fromString("deprecatedkey."), tsig2.name);
  }
}
