###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link TSIG}. The tests cover algorithm conversion, record length calculation,
 * signing, verification, and the stream generator / verifier utilities.
 *
 * <p>All tests use only the public API that is present in the supplied source file or in the
 * allowed dependency signatures.
 */
public class TSIGTest {

  private static final String BASE64_KEY = "c2VjcmV0"; // "secret"
  private Name keyName;
  private TSIG tsigSha256;
  private TSIG tsigMd5;

  @Before
  public void setUp() throws Exception {
    keyName = Name.fromString("testkey.");
    tsigSha256 = new TSIG(TSIG.HMAC_SHA256, keyName, BASE64_KEY);
    tsigMd5 = new TSIG(TSIG.HMAC_MD5, keyName, BASE64_KEY);
  }

  /** Test conversion from algorithm string to {@link Name} – normal and alias forms. */
  @Test
  public void testAlgorithmToNameValid() {
    // Alias without trailing dot
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5"));
    // Java style name, case‑insensitive
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
    // Java style with explicit length (truncated)
    assertEquals(TSIG.HMAC_SHA256_128, TSIG.algorithmToName("HmacSHA256/128"));
    // RFC style with trailing dot
    assertEquals(TSIG.HMAC_SHA384_192, TSIG.algorithmToName("hmac-sha384-192."));
  }

  /** Invalid inputs to {@link TSIG#algorithmToName(String)} must throw {@link IllegalArgumentException}. */
  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameNull() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameUnknown() {
    TSIG.algorithmToName("unknown-alg");
  }

  /** Ensure the deprecated {@code nameToAlgorithm} returns the Java algorithm name for known constants. */
  @Test
  public void testNameToAlgorithm() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacSHA384", TSIG.nameToAlgorithm(TSIG.HMAC_SHA384_192));
  }

  /** Verify that {@link TSIG#recordLength()} returns the expected size based on the chosen algorithm. */
  @Test
  public void testRecordLength() {
    int expected =
        keyName.length()
            + 10
            + TSIG.HMAC_SHA256.length()
            + 8 // time signed + fudge
            + 2 // MAC length field
            + TSIG.algLengthMap.get(TSIG.HMAC_SHA256) // MAC size
            + 4 // original id + error
            + 8; // other data length + max other field
    assertEquals(expected, tsigSha256.recordLength());
  }

  /** Sign a simple message with TSIG and verify it successfully. */
  @Test
  public void testApplyAndVerifySuccess() throws Exception {
    Message msg = new Message(0x1234);
    // Apply TSIG (no previous TSIG record)
    tsigSha256.apply(msg, null);
    assertNotNull("TSIG record should be present after apply()", msg.getTSIG());

    // Verify using the same TSIG instance
    int result = tsigSha256.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue("Message should be marked as verified", msg.isVerified());
  }

  /** Verification must fail with BADKEY when the key name differs. */
  @Test
  public void testVerifyBadKey() throws Exception {
    Message msg = new Message(0x4321);
    tsigSha256.apply(msg, null);

    // Use a different TSIG instance that has a different key name
    Name otherKeyName = Name.fromString("otherkey.");
    TSIG otherTsig = new TSIG(TSIG.HMAC_SHA256, otherKeyName, BASE64_KEY);
    int result = otherTsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADKEY, result);
    assertFalse("Message should not be marked as verified", msg.isVerified());
  }

  /** Test the stream generator and stream verifier utilities for a two‑message exchange. */
  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    // Create a query message and attach a TSIG record (first message in a stream)
    Message query = new Message(0x1111);
    TSIGRecord queryTsig =
        tsigSha256.generate(query, query.toWire(), Rcode.NOERROR, null, true);
    query.addRecord(queryTsig, Section.ADDITIONAL);
    query.tsigState = Message.TSIG_SIGNED;

    // Stream generator will sign subsequent messages, re‑using the MAC
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigSha256, queryTsig);

    // First response message (should contain a TSIG)
    Message response1 = new Message(0x2222);
    generator.generate(response1);
    assertNotNull("First response must contain a TSIG", response1.getTSIG());

    // Second response message (no extra TSIG because signEveryNthMessage defaults to 1)
    Message response2 = new Message(0x3333);
    generator.generate(response2);
    assertNotNull("Second response must also contain a TSIG", response2.getTSIG());

    // Verify both responses using the stream verifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryTsig);

    int r1 = verifier.verify(response1, response1.toWire());
    assertEquals(Rcode.NOERROR, r1);
    assertTrue(response1.isVerified());

    int r2 = verifier.verify(response2, response2.toWire());
    assertEquals(Rcode.NOERROR, r2);
    assertTrue(response2.isVerified());
  }

  /** Ensure that {@link TSIG#generate(Message,byte[],int,TSIGRecord,boolean)} produces a record with
   * the expected algorithm and that the signature length matches the algorithm's defined length. */
  @Test
  public void testGenerateSignatureLength() throws Exception {
    Message msg = new Message(0x5555);
    TSIGRecord rec = tsigMd5.generate(msg, msg.toWire(), Rcode.NOERROR, null, true);
    assertEquals(TSIG.HMAC_MD5, rec.getAlgorithm());

    // MD5 produces a 16‑byte MAC; the record should contain exactly that length.
    assertEquals(16, rec.getSignature().length);
  }

  /** Verify that a TSIG record generated with a truncated algorithm (e.g., SHA256‑128) is correctly
   * truncated to the advertised length. */
  @Test
  public void testTruncatedAlgorithmSignature() throws Exception {
    TSIG tsigTrunc = new TSIG(TSIG.HMAC_SHA256_128, keyName, BASE64_KEY);
    Message msg = new Message(0x7777);
    TSIGRecord rec = tsigTrunc.generate(msg, msg.toWire(), Rcode.NOERROR, null, true);
    assertEquals(TSIG.HMAC_SHA256_128, rec.getAlgorithm());

    // The truncated variant should produce a 16‑byte MAC (half of SHA‑256 length)
    assertEquals(16, rec.getSignature().length);
  }

  /** Test that a TSIG generated with a fixed clock produces reproducible time fields. */
  @Test
  public void testFixedClockTimeSigned() throws Exception {
    Instant fixedInstant = Instant.parse("2023-01-01T00:00:00Z");
    Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
    TSIG tsigFixed = new TSIG(TSIG.HMAC_SHA1, keyName, BASE64_KEY, fixedClock);

    Message msg = new Message(0x9999);
    TSIGRecord rec = tsigFixed.generate(msg, msg.toWire(), Rcode.NOERROR, null, true);
    assertEquals(fixedInstant, rec.getTimeSigned());
  }

  /** Verify that the internal helper {@code writeTsigTimerVariables} correctly encodes the
   *  fudge value. */
  @Test
  public void testWriteTsigTimerVariablesFudge() throws Exception {
    // Use reflection only to access the private static method (allowed for testing).
    java.lang.reflect.Method method =
        TSIG.class.getDeclaredMethod("writeTsigTimerVariables", Instant.class, Duration.class, DNSOutput.class);
    method.setAccessible(true);

    DNSOutput out = new DNSOutput();
    Instant now = Instant.now();
    Duration fudge = Duration.ofSeconds(123);
    method.invoke(null, now, fudge, out);
    byte[] data = out.toByteArray();

    // The last two bytes should be the fudge value (big‑endian)
    int fudgeFromBytes = ((data[data.length - 2] & 0xFF) << 8) | (data[data.length - 1] & 0xFF);
    assertEquals(123, fudgeFromBytes);
  }
}
###Test END##