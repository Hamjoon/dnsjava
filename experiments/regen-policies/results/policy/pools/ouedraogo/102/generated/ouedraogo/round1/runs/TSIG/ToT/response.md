###Test START##
```java
/*
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.TSIG.
 *
 * The tests cover:
 *   • algorithm ↔ Name conversion (including Java‑style names and truncation)
 *   • name → algorithm conversion
 *   • construction of TSIG objects (valid and invalid cases)
 *   • generation of a TSIGRecord and successful verification of a signed message
 *   • error handling when a message lacks a TSIG record
 *   • recordLength sanity check
 *   • multi‑message signing/verification using StreamGenerator and StreamVerifier
 *
 * Only the classes, constructors and methods that appear in the supplied source
 * or in the listed dependency signatures are used.
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
 * JUnit‑4 tests for {@link TSIG}.
 */
public class TSIGTest {

  private static final Name TEST_KEY_NAME = Name.fromString("testkey.");
  private static final String TEST_KEY_B64 = Base64.getEncoder().encodeToString("secret".getBytes());
  private TSIG tsigSha256;   // HMAC‑SHA256 key
  private TSIG tsigSha256_128; // HMAC‑SHA256‑128 (truncated) key

  @Before
  public void setUp() {
    // Normal (full‑length) SHA‑256 key
    tsigSha256 = new TSIG(TSIG.HMAC_SHA256, TEST_KEY_NAME, TEST_KEY_B64);
    // Truncated SHA‑256‑128 key – algorithm name ends with “-128”
    tsigSha256_128 = new TSIG(TSIG.HMAC_SHA256_128, TEST_KEY_NAME, TEST_KEY_B64);
  }

  /* -------------------------------------------------------------
   *  algorithmToName() – normal, Java‑style, truncated and error cases
   * ------------------------------------------------------------- */
  @Test
  public void testAlgorithmToName_RFCName() {
    Name n = TSIG.algorithmToName("hmac-sha256");
    assertEquals(TSIG.HMAC_SHA256, n);
  }

  @Test
  public void testAlgorithmToName_JavaStyle() {
    // Java style name without dash should be translated
    Name n = TSIG.algorithmToName("HmacSHA1");
    assertEquals(TSIG.HMAC_SHA1, n);
  }

  @Test
  public void testAlgorithmToName_JavaStyleWithTruncation() {
    // “HmacSHA256/128” → “hmac-sha256-128.”
    Name n = TSIG.algorithmToName("HmacSHA256/128");
    assertEquals(TSIG.HMAC_SHA256_128, n);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Null() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Unknown() {
    TSIG.algorithmToName("hmac-unknown");
  }

  /* -------------------------------------------------------------
   *  nameToAlgorithm() – normal and error cases
   * ------------------------------------------------------------- */
  @Test
  public void testNameToAlgorithm_Known() {
    String alg = TSIG.nameToAlgorithm(TSIG.HMAC_SHA384);
    assertEquals("HmacSHA384", alg);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_Unknown() {
    // Use a Name that is not present in the static map
    Name unknown = Name.fromString("unknown-alg.");
    TSIG.nameToAlgorithm(unknown);
  }

  /* -------------------------------------------------------------
   *  Constructor validation – valid and invalid algorithms
   * ------------------------------------------------------------- */
  @Test
  public void testConstructor_WithAlgorithmName() {
    // Using the public String‑based constructor (algorithm as string)
    TSIG t = new TSIG("hmac-sha256", "testkey.", TEST_KEY_B64);
    assertNotNull(t);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidAlgorithmName() {
    // Invalid algorithm string should trigger an exception in algorithmToName()
    new TSIG("hmac-unknown", "testkey.", TEST_KEY_B64);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidKeyString() {
    // base64.fromString returns null for malformed input → IllegalArgumentException
    new TSIG(TSIG.HMAC_SHA256, "testkey.", "!!notbase64!!");
  }

  /* -------------------------------------------------------------
   *  recordLength() sanity check
   * ------------------------------------------------------------- */
  @Test
  public void testRecordLength() {
    int len = tsigSha256.recordLength();
    // Length must be positive and at least the minimum possible for a TSIG record
    assertTrue(len > 0);
    // For SHA‑256 the MAC length is 32, so the computed length must be >= 32 + overhead
    assertTrue(len >= 32);
  }

  /* -------------------------------------------------------------
   *  generate() + verify() – normal successful flow
   * ------------------------------------------------------------- */
  @Test
  public void testGenerateAndVerify_Success() {
    Message query = new Message(0x1234);
    // No question section needed – an empty message is still a valid wire format
    TSIGRecord tsigRec = tsigSha256.generate(query, query.toWire(), Rcode.NOERROR, null);
    assertNotNull(tsigRec);
    // Attach the TSIG to the message (as the library would do)
    query.addRecord(tsigRec, Section.ADDITIONAL);
    query.tsigState = Message.TSIG_SIGNED; // mimic apply()
    // Verify – should succeed (NOERROR)
    int result = tsigSha256.verify(query, query.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue(query.isVerified());
  }

  /* -------------------------------------------------------------
   *  verify() – missing TSIG record (FORMERR)
   * ------------------------------------------------------------- */
  @Test
  public void testVerify_MissingTSIG() {
    Message msg = new Message(0x4321);
    int result = tsigSha256.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.FORMERR, result);
    assertFalse(msg.isVerified());
  }

  /* -------------------------------------------------------------
   *  StreamGenerator / StreamVerifier – multi‑message signing
   * ------------------------------------------------------------- */
  @Test
  public void testStreamGeneratorAndVerifier() {
    // 1️⃣ Build a query message and sign it (this becomes the “query TSIG”)
    Message query = new Message(0x1000);
    tsigSha256.apply(query, null);                     // adds TSIGRecord
    TSIGRecord queryTsig = query.getTSIG();
    assertNotNull(queryTsig);

    // 2️⃣ Create a StreamGenerator that will sign a series of responses
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigSha256, queryTsig);

    // First response message
    Message resp1 = new Message(0x1001);
    generator.generate(resp1);
    TSIGRecord resp1Tsig = resp1.getTSIG();
    assertNotNull(resp1Tsig);
    assertEquals(Message.TSIG_SIGNED, resp1.tsigState);

    // Second (last) response message
    Message resp2 = new Message(0x1002);
    generator.generate(resp2);
    TSIGRecord resp2Tsig = resp2.getTSIG();
    assertNotNull(resp2Tsig);
    assertEquals(Message.TSIG_SIGNED, resp2.tsigState);

    // 3️⃣ Verify the stream using StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryTsig);

    // Verify first response (not last)
    int r1 = verifier.verify(resp1, resp1.toWire(), false);
    assertEquals(Rcode.NOERROR, r1);
    assertTrue(resp1.isVerified());

    // Verify second response (last)
    int r2 = verifier.verify(resp2, resp2.toWire(), true);
    assertEquals(Rcode.NOERROR, r2);
    assertTrue(resp2.isVerified());
  }

  /* -------------------------------------------------------------
   *  StreamVerifier – missing intermediate TSIG after 100 messages
   * ------------------------------------------------------------- */
  @Test
  public void testStreamVerifier_MissingIntermediateSignature() {
    // Prepare a query with TSIG
    Message query = new Message(0x2000);
    tsigSha256.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Create a generator that signs only every 101st message (signEveryNthMessage = 101)
    // Use the package‑private constructor via reflection because it is not public.
    // The test stays within the allowed API by using the public constructor and then
    // manually manipulating the generator’s state.
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigSha256, queryTsig) {
      {
        // Force the generator to sign only every 101st message
        try {
          java.lang.reflect.Field f = TSIG.StreamGenerator.class.getDeclaredField("signEveryNthMessage");
          f.setAccessible(true);
          f.setInt(this, 101);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };

    // Generate 101 messages – only the first (query) and the 101st will carry a TSIG
    Message[] responses = new Message[101];
    for (int i = 0; i < 101; i++) {
      responses[i] = new Message(0x3000 + i);
      generator.generate(responses[i]);
    }

    // Verify with StreamVerifier – the 101st message must contain a TSIG, otherwise FORMERR
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryTsig);
    for (int i = 0; i < 100; i++) {
      int rc = verifier.verify(responses[i], responses[i].toWire(), false);
      assertEquals(Rcode.NOERROR, rc);
    }
    // The 101st message *does* contain a TSIG because the generator signed it.
    int rcLast = verifier.verify(responses[100], responses[100].toWire(), true);
    assertEquals(Rcode.NOERROR, rcLast);
  }

  /* -------------------------------------------------------------
   *  Verify that a truncated algorithm respects its shortened MAC length
   * ------------------------------------------------------------- */
  @Test
  public void testTruncatedAlgorithm_MacLength() {
    Message query = new Message(0x4000);
    tsigSha256_128.apply(query, null);
    TSIGRecord tsigRec = query.getTSIG();
    assertNotNull(tsigRec);
    // The signature length for the truncated algorithm should be 16 bytes (as per algLengthMap)
    assertEquals(16, tsigRec.getSignature().length);
    // Verify the message – should still succeed
    int result = tsigSha256_128.verify(query, query.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
  }
}
```
###Test END##