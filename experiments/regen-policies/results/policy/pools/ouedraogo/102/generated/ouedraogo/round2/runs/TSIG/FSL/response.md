###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link TSIG}.
 *
 * <p>The tests exercise:
 *
 * <ul>
 *   <li>algorithm name conversion (both RFC and Java style names)
 *   <li>public constructors and the deprecated {@code fromString} factory
 *   <li>record generation, signature length handling (including truncated algorithms)
 *   <li>apply / verify round‑trip (successful and failure scenarios)
 *   <li>record length calculation
 *   <li>private {@code verify} helper (via reflection)
 *   <li>the {@link TSIG.StreamGenerator} and {@link TSIG.StreamVerifier} utilities
 * </ul>
 *
 * <p>All objects are created through the public API – no mocking or subclassing of library
 * classes is performed.
 */
public class TSIGTest {

  /** Helper to create a TSIG instance with a known key (base64 \"YWJjZA==\" → \"abcd\"). */
  private TSIG createTSIG(Name algorithm, String nameStr) {
    String keyB64 = "YWJjZA=="; // \"abcd\"
    Name name = Name.fromString(nameStr, Name.root);
    return new TSIG(algorithm, name, keyB64);
  }

  /** Simple message with a unique header id. */
  private Message newMessage() {
    return new Message();
  }

  @Test
  public void testAlgorithmToName_JavaStyleNames() {
    // HmacSHA1 → hmac-sha1.
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("HmacSHA1"));
    // HmacSHA256/128 → hmac-sha256-128.
    assertEquals(TSIG.HMAC_SHA256_128, TSIG.algorithmToName("HmacSHA256/128"));
    // lower‑case Java name without dash also works
    assertEquals(TSIG.HMAC_SHA512, TSIG.algorithmToName("HmacSHA512"));
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
  public void testNameToAlgorithm_KnownNames() {
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacSHA384", TSIG.nameToAlgorithm(TSIG.HMAC_SHA384));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_Unknown() throws Exception {
    Name unknown = Name.fromString("unknown.", Name.root);
    TSIG.nameToAlgorithm(unknown);
  }

  @Test
  public void testGenerateSignatureLength_TruncatedAlgorithm() throws Exception {
    TSIG tsig = createTSIG(TSIG.HMAC_SHA256_128, "testkey.");
    Message msg = newMessage();

    // Apply the TSIG – this will generate a signature.
    tsig.apply(msg, null);
    TSIGRecord rec = msg.getTSIG();

    // The algorithm HMAC_SHA256_128 is defined to truncate to 16 bytes.
    assertEquals(16, rec.getSignature().length);
    // Ensure the record length reported by the object matches the calculation.
    int expectedLen = tsig.recordLength();
    assertTrue(expectedLen >= rec.getSignature().length);
  }

  @Test
  public void testApplyAndVerify_Success() throws Exception {
    TSIG tsig = createTSIG(TSIG.HMAC_MD5, "mykey.");
    Message msg = newMessage();

    // Sign the message.
    tsig.apply(msg, null);
    assertNotNull(msg.getTSIG());
    assertEquals(Message.TSIG_SIGNED, msg.tsigState);

    // Verify the signature with the SAME TSIG instance.
    int result = tsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
    assertEquals(Message.TSIG_VERIFIED, msg.tsigState);
    assertTrue(msg.isVerified());
  }

  @Test
  public void testVerify_BadKey() throws Exception {
    // First TSIG signs the message.
    TSIG signer = createTSIG(TSIG.HMAC_MD5, "signer.");
    Message msg = newMessage();
    signer.apply(msg, null);

    // Second TSIG (different key name) attempts verification – should get BADKEY.
    TSIG verifier = createTSIG(TSIG.HMAC_MD5, "different.");
    int result = verifier.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADKEY, result);
    assertEquals(Message.TSIG_FAILED, msg.tsigState);
  }

  @Test
  public void testRecordLengthCalculation() {
    TSIG tsig = createTSIG(TSIG.HMAC_SHA1, "lenkey.");
    int calculated = tsig.recordLength();

    // Manual calculation mirroring the implementation.
    int expected = tsig.name.length()
        + 10
        + tsig.alg.length()
        + 8 // time signed + fudge
        + 2 // MAC length field
        + TSIG.algLengthMap.get(tsig.alg)
        + 4 // original id + error
        + 8; // other length field (max)
    assertEquals(expected, calculated);
  }

  @Test
  public void testFromString_ParsingVariations() {
    // name:secret (default algorithm HMAC_MD5)
    TSIG t1 = TSIG.fromString("mykey.:YWJjZA==");
    assertEquals(TSIG.HMAC_MD5, t1.alg);
    assertEquals(Name.fromString("mykey.", Name.root), t1.name);

    // alg:name:secret (explicit algorithm)
    TSIG t2 = TSIG.fromString("hmac-sha256:testkey.:YWJjZA==");
    assertEquals(TSIG.HMAC_SHA256, t2.alg);
    assertEquals(Name.fromString("testkey.", Name.root), t2.name);

    // alg/name/secret (slash separator)
    TSIG t3 = TSIG.fromString("hmac-sha1/testkey.:YWJjZA==");
    assertEquals(TSIG.HMAC_SHA1, t3.alg);
    assertEquals(Name.fromString("testkey.", Name.root), t3.name);
  }

  @Test
  public void testPrivateVerify_TruncatesWhenSignatureShorter() throws Exception {
    // Use reflection to reach the private static verify method.
    Method m = TSIG.class.getDeclaredMethod("verify", byte[].class, byte[].class);
    m.setAccessible(true);

    byte[] expected = new byte[] {1, 2, 3, 4, 5};
    byte[] signature = new byte[] {1, 2, 3}; // shorter than expected

    // Expected behaviour: the method truncates 'expected' to length of signature.
    boolean result = (Boolean) m.invoke(null, expected, signature);
    assertTrue(result);
  }

  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    // Create a TSIG key and a query message that carries a TSIG.
    TSIG key = createTSIG(TSIG.HMAC_SHA256, "streamkey.");
    Message query = newMessage();
    key.apply(query, null); // first message signed
    TSIGRecord queryTsig = query.getTSIG();

    // StreamGenerator will sign every message (default signEveryNthMessage = 1).
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(key, queryTsig);

    // First response (should be signed)
    Message resp1 = new Message();
    generator.generate(resp1);
    assertNotNull(resp1.getTSIG());
    assertEquals(Message.TSIG_SIGNED, resp1.tsigState);

    // Second response (also signed because signEveryNthMessage==1)
    Message resp2 = new Message();
    generator.generate(resp2);
    assertNotNull(resp2.getTSIG());

    // Verify the stream using StreamVerifier.
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(key, queryTsig);

    // Verify first response – must be OK.
    int r1 = verifier.verify(resp1, resp1.toWire());
    assertEquals(Rcode.NOERROR, r1);
    assertTrue(resp1.isVerified());

    // Verify second response – also OK.
    int r2 = verifier.verify(resp2, resp2.toWire());
    assertEquals(Rcode.NOERROR, r2);
    assertTrue(resp2.isVerified());

    // No error messages should have been set.
    assertNull(verifier.getErrorMessage());
  }

  @Test
  public void testVerifyTime_TooOldFails() throws Exception {
    // Build a TSIG with an artificially old timestamp.
    TSIG key = createTSIG(TSIG.HMAC_SHA1, "timekey.");
    Message msg = newMessage();

    // Sign the message normally.
    key.apply(msg, null);
    TSIGRecord tsigRec = msg.getTSIG();

    // Manipulate the timestamp to be far in the past (beyond fudge).
    Instant old = Instant.now().minus(Duration.ofHours(2));
    // Create a new TSIGRecord with same fields but old time.
    TSIGRecord oldRec = new TSIGRecord(
        tsigRec.getName(),
        tsigRec.dclass,
        tsigRec.ttl,
        tsigRec.getAlgorithm(),
        old,
        tsigRec.getFudge(),
        tsigRec.getSignature(),
        tsigRec.getOriginalID(),
        tsigRec.getError(),
        tsigRec.getOther());

    // Replace the TSIG in the message.
    msg.removeAllRecords(Section.ADDITIONAL);
    msg.addRecord(oldRec, Section.ADDITIONAL);
    msg.tsigState = Message.TSIG_SIGNED; // mimic state after apply

    // Verify – should get BADTIME.
    int result = key.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADTIME, result);
    assertEquals(Message.TSIG_FAILED, msg.tsigState);
  }
}
###Test END##