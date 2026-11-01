###Test START##
```java
/*
 * TSIGTest.java
 *
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.TSIG.
 *
 * The tests cover:
 *   • algorithm name conversion (algorithmToName, nameToAlgorithm)
 *   • construction of TSIG objects from various inputs
 *   • generation of TSIG records (full / reduced signatures, error handling)
 *   • application of TSIG records to messages
 *   • verification of TSIG signatures (success and all failure modes)
 *   • recordLength calculation
 *   • StreamGenerator and StreamVerifier utilities
 *
 * The test suite uses only public APIs of the DNS library and standard JDK classes.
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test class for {@link TSIG}.
 */
public class TSIGTest {

  /** A fixed instant used for deterministic tests. */
  private static final Instant FIXED_INSTANT = Instant.parse("2023-01-01T12:00:00Z");
  /** A fixed clock based on the instant above. */
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

  /** Simple domain name used throughout the tests. */
  private static final Name TEST_NAME = Name.fromConstantString("example.com.");

  /** Base64‑encoded key used for HMAC‑SHA256. */
  private static final String KEY_BASE64 = Base64.getEncoder().encodeToString("testkey123".getBytes());

  /** The TSIG key used for most tests (HMAC‑SHA256). */
  private TSIG tsigSha256;

  /** A second TSIG key with a different secret (used for BADKEY test). */
  private TSIG tsigSha256Other;

  /** A simple query message used as a basis for signing / verification. */
  private Message queryMessage;

  @Before
  public void setUp() throws Exception {
    // Create the primary TSIG key (SHA256) using the fixed clock.
    tsigSha256 = new TSIG(TSIG.HMAC_SHA256, TEST_NAME,
        new SecretKeySpec(Base64.getDecoder().decode(KEY_BASE64), "HmacSHA256"), FIXED_CLOCK);

    // Create a different key for negative tests.
    String otherKey = Base64.getEncoder().encodeToString("otherkey".getBytes());
    tsigSha256Other = new TSIG(TSIG.HMAC_SHA256, TEST_NAME,
        new SecretKeySpec(Base64.getDecoder().decode(otherKey), "HmacSHA256"), FIXED_CLOCK);

    // Build a minimal query message (ID 0x1234) with a single A record question.
    queryMessage = new Message(0x1234);
    Record question = Record.newRecord(Name.fromString("www.example.com."), Type.A, DClass.IN);
    queryMessage.addRecord(question, Section.QUESTION);
  }

  /* -------------------------------------------------------------------------
   *  algorithmToName / nameToAlgorithm
   * ------------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_JavaAlgorithmNames() {
    // Java style without dash should be translated.
    Name name = TSIG.algorithmToName("HmacSHA256");
    assertEquals(TSIG.HMAC_SHA256, name);

    // With truncation length.
    Name truncated = TSIG.algorithmToName("HmacSHA256/128");
    assertEquals(TSIG.HMAC_SHA256_128, truncated);
  }

  @Test
  public void testAlgorithmToName_WithTrailingDotAndAlias() {
    // Already RFC name with trailing dot.
    Name name = TSIG.algorithmToName("hmac-md5.");
    assertEquals(TSIG.HMAC_MD5, name);

    // Alias without trailing dot.
    Name alias = TSIG.algorithmToName("hmac-md5");
    assertEquals(TSIG.HMAC_MD5, alias);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_NullArgument() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_UnknownAlgorithm() {
    TSIG.algorithmToName("hmac-unknown.");
  }

  @Test
  public void testNameToAlgorithm_KnownAlgorithms() {
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256_128));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_UnknownName() {
    // Create a synthetic name that is not in the map.
    Name fake = Name.fromConstantString("fake-alg.");
    TSIG.nameToAlgorithm(fake);
  }

  /* -------------------------------------------------------------------------
   *  Constructors
   * ------------------------------------------------------------------------- */

  @Test
  public void testConstructor_FromBase64String() {
    TSIG t = new TSIG(TSIG.HMAC_SHA256, TEST_NAME, KEY_BASE64);
    assertNotNull(t);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidBase64() {
    new TSIG(TSIG.HMAC_SHA256, TEST_NAME, "!!! not base64 !!!");
  }

  @Test
  public void testConstructor_DeprecatedStringForm() {
    // Using the public (String algorithm, String name, String key) ctor.
    TSIG t = new TSIG("hmac-sha256", "example.com.", KEY_BASE64);
    assertNotNull(t);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_FromString_InvalidSpec() {
    TSIG.fromString("invalidspec");
  }

  @Test
  public void testFromString_TwoPart() {
    TSIG t = TSIG.fromString("example.com.:"+KEY_BASE64);
    assertNotNull(t);
  }

  @Test
  public void testFromString_ThreePart() {
    TSIG t = TSIG.fromString("hmac-sha256:example.com.:"+KEY_BASE64);
    assertNotNull(t);
  }

  /* -------------------------------------------------------------------------
   *  recordLength()
   * ------------------------------------------------------------------------- */

  @Test
  public void testRecordLength() {
    int expected = tsigSha256.recordLength();
    // The length must be at least the sum of the fixed fields.
    assertTrue(expected > 0);
    // Verify that the length matches the calculation performed in the method.
    int manual =
        tsigSha256.name.length() + 10 + tsigSha256.alg.length() + 8 +
        2 + TSIG.algLengthMap.get(tsigSha256.alg) + 4 + 8;
    assertEquals(manual, expected);
  }

  /* -------------------------------------------------------------------------
   *  generate() and apply()
   * ------------------------------------------------------------------------- */

  @Test
  public void testGenerateAndApply_FullSignature() {
    // Generate a TSIGRecord with full signature.
    TSIGRecord tsigRec = tsigSha256.generate(queryMessage, queryMessage.toWire(),
        Rcode.NOERROR, null, true);
    assertNotNull(tsigRec);
    assertEquals(TSIG.HMAC_SHA256, tsigRec.getAlgorithm());

    // Apply the record to a fresh response message.
    Message response = new Message(0x1234);
    response.addRecord(Record.newRecord(Name.fromString("www.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    tsigSha256.apply(response, null);
    TSIGRecord added = response.getTSIG();
    assertNotNull(added);
    assertEquals(TSIG.HMAC_SHA256, added.getAlgorithm());
    assertEquals(Duration.ofSeconds(300), added.getFudge());
  }

  @Test
  public void testGenerate_ReducedSignature() {
    // First message (full signature) – we pass an old TSIGRecord to simulate a response.
    TSIGRecord first = tsigSha256.generate(queryMessage, queryMessage.toWire(),
        Rcode.NOERROR, null, true);
    // Second message uses reduced signature (fullSignature = false).
    Message second = new Message(0x1235);
    second.addRecord(Record.newRecord(Name.fromString("www.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    TSIGRecord secondRec = tsigSha256.generate(second, second.toWire(),
        Rcode.NOERROR, first, false);
    assertNotNull(secondRec);
  }

  /* -------------------------------------------------------------------------
   *  verify() – success and all failure modes
   * ------------------------------------------------------------------------- */

  @Test
  public void testVerify_Success() throws Exception {
    // Sign a message.
    Message response = new Message(0x4321);
    response.addRecord(Record.newRecord(Name.fromString("www.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    tsigSha256.apply(response, null);

    // Verify using the same key.
    int rcode = tsigSha256.verify(response, response.toWire(), null);
    assertEquals(Rcode.NOERROR, rcode);
    assertTrue(response.isVerified());
  }

  @Test
  public void testVerify_BadKey() throws Exception {
    // Sign with the primary key.
    Message response = new Message(0x4321);
    response.addRecord(Record.newRecord(Name.fromString("www.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    tsigSha256.apply(response, null);

    // Verify with a different key (different secret).
    int rcode = tsigSha256Other.verify(response, response.toWire(), null);
    assertEquals(Rcode.BADKEY, rcode);
    assertFalse(response.isVerified());
  }

  @Test
  public void testVerify_BadSignature() throws Exception {
    // Sign normally.
    Message response = new Message(0x4321);
    response.addRecord(Record.newRecord(Name.fromString("www.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    tsigSha256.apply(response, null);

    // Corrupt the signature bytes.
    TSIGRecord tsig = response.getTSIG();
    byte[] corrupted = tsig.getSignature().clone();
    corrupted[0] ^= 0xFF; // flip bits
    TSIGRecord corruptedRec = new TSIGRecord(tsig.getName(), tsig.dclass, tsig.ttl,
        tsig.getAlgorithm(), tsig.getTimeSigned(), tsig.getFudge(),
        corrupted, tsig.getOriginalID(), tsig.getError(), tsig.getOther());
    response.removeAllRecords(Section.ADDITIONAL);
    response.addRecord(corruptedRec, Section.ADDITIONAL);

    int rcode = tsigSha256.verify(response, response.toWire(), null);
    assertEquals(Rcode.BADSIG, rcode);
    assertFalse(response.isVerified());
  }

  @Test
  public void testVerify_BadTime() throws Exception {
    // Use a TSIG with a very small fudge (1 second) and a clock far from the signed time.
    Clock farClock = Clock.fixed(FIXED_INSTANT.minus(Duration.ofHours(5)), ZoneOffset.UTC);
    TSIG shortFudgeTsig = new TSIG(TSIG.HMAC_SHA256, TEST_NAME,
        new SecretKeySpec(Base64.getDecoder().decode(KEY_BASE64), "HmacSHA256"), farClock);

    // Sign with the original (fixed) clock.
    Message response = new Message(0x4321);
    response.addRecord(Record.newRecord(Name.fromString("www.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    tsigSha256.apply(response, null); // signed at FIXED_INSTANT

    // Verify with the far‑away clock (fudge = 1 sec by default).
    int rcode = shortFudgeTsig.verify(response, response.toWire(), null);
    assertEquals(Rcode.BADTIME, rcode);
    assertFalse(response.isVerified());
  }

  @Test
  public void testVerify_NoTSIGRecord() throws Exception {
    Message response = new Message(0x4321);
    response.addRecord(Record.newRecord(Name.fromString("www.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    // No TSIG attached.
    int rcode = tsigSha256.verify(response, response.toWire(), null);
    assertEquals(Rcode.FORMERR, rcode);
    assertFalse(response.isVerified());
  }

  /* -------------------------------------------------------------------------
   *  StreamGenerator and StreamVerifier
   * ------------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorProducesConsistentSignatures() throws Exception {
    // Initial query TSIG (simulated)
    TSIGRecord queryTsig = tsigSha256.generate(queryMessage, queryMessage.toWire(),
        Rcode.NOERROR, null, true);

    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigSha256, queryTsig);

    // Generate three sequential responses.
    Message resp1 = new Message(0x1111);
    resp1.addRecord(Record.newRecord(Name.fromString("a.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    generator.generate(resp1);
    assertNotNull(resp1.getTSIG());

    Message resp2 = new Message(0x1112);
    resp2.addRecord(Record.newRecord(Name.fromString("b.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    generator.generate(resp2);
    // Second message may have a reduced signature (but still a TSIG record in this case)
    assertNotNull(resp2.getTSIG());

    Message resp3 = new Message(0x1113);
    resp3.addRecord(Record.newRecord(Name.fromString("c.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    generator.generate(resp3);
    assertNotNull(resp3.getTSIG());

    // Verify the whole stream using StreamVerifier.
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryTsig);
    assertEquals(Rcode.NOERROR, verifier.verify(resp1, resp1.toWire()));
    assertEquals(Rcode.NOERROR, verifier.verify(resp2, resp2.toWire()));
    assertEquals(Rcode.NOERROR, verifier.verify(resp3, resp3.toWire(), true));
    assertTrue(resp3.isVerified());
  }

  @Test
  public void testStreamVerifier_MissingIntermediateSignature() throws Exception {
    // Create a query TSIG.
    TSIGRecord queryTsig = tsigSha256.generate(queryMessage, queryMessage.toWire(),
        Rcode.NOERROR, null, true);

    // Manually craft a stream with a missing TSIG on the 100th message.
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryTsig);

    // Message 1 – has TSIG (first message)
    Message m1 = new Message(0x2001);
    m1.addRecord(Record.newRecord(Name.fromString("a.example.com."), Type.A, DClass.IN),
        Section.ANSWER);
    tsigSha256.apply(m1, null);
    assertEquals(Rcode.NOERROR, verifier.verify(m1, m1.toWire()));

    // Simulate 99 intermediate messages without TSIG.
    for (int i = 2; i <= 100; i++) {
      Message mi = new Message(0x2000 + i);
      mi.addRecord(Record.newRecord(Name.fromString("a"+i+".example.com."),
          Type.A, DClass.IN), Section.ANSWER);
      // No TSIG added.
      if (i < 100) {
        // Should be accepted as intermediate.
        assertEquals(Rcode.NOERROR, verifier.verify(mi, mi.toWire()));
      } else {
        // 100th message must have TSIG – we deliberately omit it.
        int rcode = verifier.verify(mi, mi.toWire());
        assertEquals(Rcode.FORMERR, rcode);
        assertEquals("Missing required signature on message #100", verifier.getErrorMessage());
      }
    }
  }

  /* -------------------------------------------------------------------------
   *  Private helper – verify that signature truncation works for truncated algorithms.
   * ------------------------------------------------------------------------- */

  @Test
  public void testSignatureTruncation_HmacSha256_128() throws Exception {
    // Use the truncated algorithm (SHA256/128 → 16‑byte MAC).
    TSIG truncatedTsig = new TSIG(TSIG.HMAC_SHA256_128, TEST_NAME,
        new SecretKeySpec(Base64.getDecoder().decode(KEY_BASE64), "HmacSHA256"), FIXED_CLOCK);

    Message response = new Message(0x5000);
    response.addRecord(Record.newRecord(Name.fromString("www.example.com."),
        Type.A, DClass.IN), Section.ANSWER);
    truncatedTsig.apply(response, null);

    TSIGRecord tsigRec = response.getTSIG();
    assertNotNull(tsigRec);
    // Verify that the MAC length matches the algorithm‑specific length (16 bytes).
    assertEquals(16, tsigRec.getSignature().length);
    // Full verification should still succeed.
    int rcode = truncatedTsig.verify(response, response.toWire(), null);
    assertEquals(Rcode.NOERROR, rcode);
  }
}
```
###Test END##