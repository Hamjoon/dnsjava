/*
 * TSIGTest.java
 *
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.TSIG.
 *
 * The tests cover:
 *   • algorithm name conversion utilities
 *   • all public constructors
 *   • record generation (generate / apply)
 *   • verification (including BADKEY, BADSIG and BADTIME)
 *   • record length calculation
 *   • StreamGenerator / StreamVerifier interaction
 *
 * The test class uses only the public API listed in the problem statement.
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 tests for {@link TSIG}.
 *
 * @author OpenAI ChatGPT
 */
public class TSIGTest {

  /** Simple base64‑encoded 16‑byte key (all zeros). */
  private static final String ZERO_KEY_B64 = "AAAAAAAAAAAAAAAAAAAAAA==";

  /** Fixed clock used for deterministic timestamps. */
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

  private Name testName;
  private TSIG tsigMd5;
  private TSIG tsigSha256;

  @Before
  public void setUp() throws Exception {
    testName = Name.fromString("example.com.");
    // HMAC‑MD5 key
    tsigMd5 = new TSIG(TSIG.HMAC_MD5, testName, ZERO_KEY_B64);
    // HMAC‑SHA256 key (same key bytes, different algorithm)
    tsigSha256 = new TSIG(TSIG.HMAC_SHA256, testName, ZERO_KEY_B64);
  }

  /* -------------------------------------------------------------------------
   *  algorithmToName / nameToAlgorithm tests
   * ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToNameKnownValues() {
    // Direct RFC name (with trailing dot)
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5.SIG-ALG.REG.INT."));
    // Alias without trailing dot
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    // Case‑insensitive lookup
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));
    // Java algorithm name fallback
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("HmacSHA1"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameNull() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameUnknown() {
    TSIG.algorithmToName("unknown-algorithm");
  }

  @Test
  public void testNameToAlgorithmKnown() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithmUnknown() {
    // Use a Name that is not in the map
    Name bogus = Name.fromString("bogus.");
    TSIG.nameToAlgorithm(bogus);
  }

  /* -------------------------------------------------------------------------
   *  Constructors
   * ---------------------------------------------------------------------- */

  @Test
  public void testConstructorWithBase64String() {
    TSIG t = new TSIG(TSIG.HMAC_MD5, testName, ZERO_KEY_B64);
    assertNotNull(t);
    // Verify algorithm and name are stored correctly via reflection (public getters not present)
    // The test simply ensures no exception is thrown and the object can be used to sign.
    Message m = new Message();
    t.apply(m, null);
    assertNotNull(m.getTSIG());
  }

  @Test
  public void testConstructorWithByteArray() {
    byte[] keyBytes = base64.fromString(ZERO_KEY_B64);
    TSIG t = new TSIG(TSIG.HMAC_SHA1, testName, keyBytes);
    Message m = new Message();
    t.apply(m, null);
    assertEquals(TSIG.HMAC_SHA1, m.getTSIG().getAlgorithm());
  }

  @Test
  public void testConstructorWithSecretKeyAndClock() {
    byte[] keyBytes = base64.fromString(ZERO_KEY_B64);
    SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA384");
    TSIG t = new TSIG(TSIG.HMAC_SHA384, testName, key, FIXED_CLOCK);
    Message m = new Message();
    t.apply(m, null);
    assertEquals(TSIG.HMAC_SHA384, m.getTSIG().getAlgorithm());
  }

  @Test
  public void testStringBasedConstructor() {
    TSIG t = new TSIG("hmac-sha512", "example.com.", ZERO_KEY_B64);
    Message m = new Message();
    t.apply(m, null);
    assertEquals(TSIG.HMAC_SHA512, m.getTSIG().getAlgorithm());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testStringBasedConstructorInvalidKey() {
    // Invalid base64 string should cause IllegalArgumentException
    new TSIG("hmac-sha256", "example.com.", "!!!notbase64!!!");
  }

  /* -------------------------------------------------------------------------
   *  Record generation and basic properties
   * ---------------------------------------------------------------------- */

  @Test
  public void testGenerateAndApplyCreatesValidTSIGRecord() {
    Message query = new Message(0x1234);
    tsigMd5.apply(query, null);
    TSIGRecord tsigRec = query.getTSIG();

    assertNotNull(tsigRec);
    assertEquals(testName, tsigRec.getName());
    assertEquals(TSIG.HMAC_MD5, tsigRec.getAlgorithm());
    // The signature must be non‑empty for a successful signing
    assertTrue(tsigRec.getSignature().length > 0);
    // Verify that the message state is marked as signed
    assertEquals(Message.TSIG_SIGNED, query.tsigState);
  }

  @Test
  public void testRecordLengthMatchesFormula() {
    int expected = testName.length()
        + 10
        + TSIG.HMAC_SHA256.length()
        + 8   // time signed + fudge
        + 18  // MAC length field + max MAC size (16)
        + 4   // original id + error
        + 8;  // other data length field + max other data (6)
    assertEquals(expected, tsigSha256.recordLength());
  }

  /* -------------------------------------------------------------------------
   *  Verification – success, BADKEY, BADSIG, BADTIME
   * ---------------------------------------------------------------------- */

  @Test
  public void testVerifySuccess() throws Exception {
    Message query = new Message(0xABCD);
    tsigSha256.apply(query, null);
    byte[] wire = query.toWire();
    int result = tsigSha256.verify(query, wire, null);
    assertEquals(Rcode.NOERROR, result);
    assertEquals(Message.TSIG_VERIFIED, query.tsigState);
  }

  @Test
  public void testVerifyBadKey() throws Exception {
    // Sign with one key, verify with a different key
    TSIG otherKey = new TSIG(TSIG.HMAC_SHA256, testName, "AAAAAAAAAAAAAAAAAAAAAB=="); // slightly different
    Message query = new Message(0x1111);
    tsigSha256.apply(query, null);
    byte[] wire = query.toWire();
    int result = otherKey.verify(query, wire, null);
    assertEquals(Rcode.BADKEY, result);
    assertEquals(Message.TSIG_FAILED, query.tsigState);
  }

  @Test
  public void testVerifyBadSignature() throws Exception {
    Message query = new Message(0x2222);
    tsigMd5.apply(query, null);
    byte[] wire = query.toWire();

    // Corrupt one byte of the wire after the header (the message body)
    wire[wire.length - 1] ^= 0xFF; // flip bits in the last byte

    int result = tsigMd5.verify(query, wire, null);
    assertEquals(Rcode.BADSIG, result);
    assertEquals(Message.TSIG_FAILED, query.tsigState);
  }

  @Test
  public void testVerifyBadTime() throws Exception {
    // Use a clock that is far in the future to cause BADTIME
    Clock farFuture = Clock.fixed(Instant.parse("2100-01-01T00:00:00Z"), ZoneOffset.UTC);
    TSIG futureTsig = new TSIG(TSIG.HMAC_MD5, testName, ZERO_KEY_B64, farFuture);

    Message query = new Message(0x3333);
    futureTsig.apply(query, null);
    byte[] wire = query.toWire();

    int result = futureTsig.verify(query, wire, null);
    assertEquals(Rcode.BADTIME, result);
    assertEquals(Message.TSIG_FAILED, query.tsigState);
  }

  /* -------------------------------------------------------------------------
   *  StreamGenerator / StreamVerifier integration
   * ---------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifierSuccess() throws Exception {
    // Create a query message with TSIG
    Message query = new Message(0x4444);
    tsigMd5.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // StreamGenerator will sign two response messages
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigMd5, queryTsig);
    Message resp1 = new Message(0x5555);
    Message resp2 = new Message(0x6666);
    generator.generate(resp1);
    generator.generate(resp2);

    // Verify both responses using StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);
    int r1 = verifier.verify(resp1, resp1.toWire());
    int r2 = verifier.verify(resp2, resp2.toWire(), true); // last message

    assertEquals(Rcode.NOERROR, r1);
    assertEquals(Rcode.NOERROR, r2);
    assertEquals(Message.TSIG_VERIFIED, resp1.tsigState);
    assertEquals(Message.TSIG_VERIFIED, resp2.tsigState);
    assertNull(verifier.getErrorMessage());
  }

  @Test
  public void testStreamVerifierWithIntermediateUnsignedMessage() throws Exception {
    // Prepare query with TSIG
    Message query = new Message(0x7777);
    tsigSha256.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // StreamGenerator that signs every 2nd message (signEveryNthMessage = 2)
    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsigSha256, queryTsig, 2);

    // Generate three messages: 1st signed, 2nd unsigned, 3rd signed (last)
    Message msg1 = new Message(0x8888);
    Message msg2 = new Message(0x9999);
    Message msg3 = new Message(0xAAAA);

    gen.generate(msg1);            // signed
    gen.generate(msg2);            // unsigned (intermediate)
    gen.generate(msg3, true);      // signed and marked as last

    // Verify using StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryTsig);
    int r1 = verifier.verify(msg1, msg1.toWire());
    int r2 = verifier.verify(msg2, msg2.toWire()); // intermediate, unsigned
    int r3 = verifier.verify(msg3, msg3.toWire(), true); // last, signed

    assertEquals(Rcode.NOERROR, r1);
    assertEquals(Rcode.NOERROR, r2);
    assertEquals(Rcode.NOERROR, r3);
    assertNull(verifier.getErrorMessage());
  }

  @Test
  public void testStreamVerifierMissingSignatureOnRequiredMessage() throws Exception {
    // Query with TSIG
    Message query = new Message(0xBBBB);
    tsigMd5.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Generator that signs only the first message (default)
    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsigMd5, queryTsig);

    // Produce first signed message
    Message signed = new Message(0xCCCC);
    gen.generate(signed);

    // Produce 101 unsigned messages to trigger FORMERR on the 101st
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);
    // First message is verified normally
    assertEquals(Rcode.NOERROR, verifier.verify(signed, signed.toWire()));

    // Generate 100 unsigned messages
    for (int i = 0; i < 100; i++) {
      Message unsigned = new Message(0x1000 + i);
      // Simulate receipt without a TSIG record
      unsigned.tsigState = Message.TSIG_INTERMEDIATE; // state not important for test
      int result = verifier.verify(unsigned, unsigned.toWire());
      // All but the 100th should be NOERROR
      if (i < 99) {
        assertEquals("Unexpected error at i=" + i, Rcode.NOERROR, result);
      } else {
        // The 100th intermediate without a signature must cause FORMERR
        assertEquals(Rcode.FORMERR, result);
        assertEquals("Missing required signature on message #101", verifier.getErrorMessage());
      }
    }
  }

  /* -------------------------------------------------------------------------
   *  Helper methods (none needed beyond what is used above)
   * ---------------------------------------------------------------------- */
}
