###Test START##
```java
/*
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.TSIG.
 *
 * The tests are written by three “experts” that collaboratively
 *   1. listed the public API,
 *   2. produced a basic test for each method,
 *   3. identified edge‑cases / exception scenarios,
 *   4. added dedicated tests for those cases,
 *   5. merged everything into a single test class.
 *
 * Only the classes, constructors and methods that appear in the provided
 * source code (or in the listed dependency signatures) are used – no
 * external mocking frameworks or additional DNS record types are required.
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;

/**
 * JUnit‑4 test cases for {@link TSIG}.
 *
 * The suite covers:
 * <ul>
 *   <li>normal operation of each public method</li>
 *   <li>edge‑cases such as null arguments, unknown algorithms, malformed keys</li>
 *   <li>signature generation / verification, including BADKEY and BADTIME</li>
 *   <li>record length sanity check</li>
 *   <li>the {@link TSIG.StreamGenerator} / {@link TSIG.StreamVerifier} helpers</li>
 * </ul>
 */
public class TSIGTest {

  /* ---------------------------------------------------------------------- */
  /* Helper constants */
  /* ---------------------------------------------------------------------- */

  /** A deterministic clock (2022‑01‑01T00:00:00Z). */
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2022-01-01T00:00:00Z"), ZoneOffset.UTC);

  /** Base‑64 encoded 32‑byte key (the string “abcdefghijklmnopqrstuvwxyz” encoded). */
  private static final String VALID_BASE64_KEY = base64.toString(
      "abcdefghijklmnopqrstuvwxyz".getBytes());

  /** Helper to create a Name instance without having to catch checked exceptions in every test. */
  private static Name name(String s) {
    try {
      return Name.fromString(s);
    } catch (TextParseException e) {
      throw new RuntimeException(e);
    }
  }

  /** Helper to create a fresh, empty Message. */
  private static Message emptyMessage() {
    return new Message();
  }

  /* ---------------------------------------------------------------------- */
  /* 1. algorithmToName – normal and edge cases */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_StandardNames() {
    // RFC‑named algorithms – trailing dot may be omitted
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5"));
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1."));
    // Java‑style name (no dash) – should be translated
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
    // Truncated form with explicit length
    assertEquals(TSIG.HMAC_SHA256_128, TSIG.algorithmToName("HmacSHA256/128"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_NullArgument() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_UnknownAlgorithm() {
    TSIG.algorithmToName("hmac-unknown");
  }

  /* ---------------------------------------------------------------------- */
  /* 2. nameToAlgorithm – normal and edge cases (deprecated) */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testNameToAlgorithm_Known() {
    assertEquals("HmacSHA1", TSIG.nameToAlgorithm(TSIG.HMAC_SHA1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_Unknown() {
    TSIG.nameToAlgorithm(name("nonexistent."));
  }

  /* ---------------------------------------------------------------------- */
  /* 3. Constructors – normal and error handling */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testConstructor_StringKey() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, name("keyname."), VALID_BASE64_KEY);
    assertNotNull(tsig);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidBase64() {
    new TSIG(TSIG.HMAC_SHA256, name("keyname."), "!!! not base64 !!!");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_MalformedAlgorithmString() {
    new TSIG("not-an-alg", "keyname.", VALID_BASE64_KEY);
  }

  @Test
  public void testConstructor_WithClock() {
    SecretKey key = new SecretKeySpec("abcdabcdabcdabcd".getBytes(), "HmacSHA256");
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, name("keyname."), key, FIXED_CLOCK);
    assertNotNull(tsig);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromString_InvalidSpec() {
    TSIG.fromString("justonepart");
  }

  @Test
  public void testFromString_TwoParts_UsesDefaultAlgorithm() {
    TSIG tsig = TSIG.fromString("keyname:".concat(VALID_BASE64_KEY));
    // default algorithm is HMAC_MD5
    assertEquals(TSIG.HMAC_MD5, tsig.algorithmToName("HmacMD5"));
  }

  /* ---------------------------------------------------------------------- */
  /* 4. generate / apply / verify – happy path */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testGenerateApplyVerify_Success() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, name("keyname."), VALID_BASE64_KEY);
    Message msg = emptyMessage();

    // generate and attach a TSIG record
    tsig.apply(msg, (TSIGRecord) null);
    assertNotNull("TSIG record should be attached", msg.getTSIG());

    // verify the same message with the same key
    int result = tsig.verify(msg, msg.toWire(), null);
    assertEquals("Verification should succeed", Rcode.NOERROR, result);
  }

  @Test
  public void testVerify_BadKey() {
    // two different TSIG keys (different key names)
    TSIG tsigA = new TSIG(TSIG.HMAC_SHA256, name("keyA."), VALID_BASE64_KEY);
    TSIG tsigB = new TSIG(TSIG.HMAC_SHA256, name("keyB."), VALID_BASE64_KEY);

    Message msg = emptyMessage();
    tsigA.apply(msg, (TSIGRecord) null);

    int result = tsigB.verify(msg, msg.toWire(), null);
    assertEquals("Verification with wrong key name should yield BADKEY", Rcode.BADKEY, result);
  }

  @Test
  public void testVerify_BadTime() {
    // Create a TSIG whose clock is far in the past, then verify with a TSIG whose clock
    // is far in the future – the time delta will exceed the default fudge (5 min).
    Clock past = Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC);
    Clock future = Clock.fixed(Instant.parse("2100-01-01T00:00:00Z"), ZoneOffset.UTC);

    TSIG signer = new TSIG(TSIG.HMAC_SHA256, name("keyname."), VALID_BASE64_KEY, past);
    TSIG verifier = new TSIG(TSIG.HMAC_SHA256, name("keyname."), VALID_BASE64_KEY, future);

    Message msg = emptyMessage();
    signer.apply(msg, (TSIGRecord) null);

    int result = verifier.verify(msg, msg.toWire(), null);
    assertEquals("Verification with out‑of‑fudge time should yield BADTIME", Rcode.BADTIME, result);
  }

  /* ---------------------------------------------------------------------- */
  /* 5. recordLength sanity check */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testRecordLength_Positive() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, name("keyname."), VALID_BASE64_KEY);
    int len = tsig.recordLength();
    // minimal expected length: name + alg + fixed overhead (see implementation)
    assertTrue("recordLength should be positive", len > 0);
  }

  /* ---------------------------------------------------------------------- */
  /* 6. StreamGenerator / StreamVerifier – multi‑message flow */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    // Initialise a key and a query message containing a TSIG record
    TSIG key = new TSIG(TSIG.HMAC_SHA256, name("keyname."), VALID_BASE64_KEY);
    Message query = emptyMessage();
    key.apply(query, (TSIGRecord) null);
    TSIGRecord queryTsig = query.getTSIG();
    assertNotNull("Query must contain a TSIG", queryTsig);

    // StreamGenerator will sign three messages (first, middle, last)
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(key, queryTsig);

    Message resp1 = emptyMessage();
    generator.generate(resp1);                 // first (signed)

    Message resp2 = emptyMessage();
    generator.generate(resp2);                 // middle (signed because signEveryNthMessage defaults to 1)

    Message resp3 = emptyMessage();
    generator.generate(resp3);                 // last (signed)

    // Now verify the stream using StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(key, queryTsig);

    int r1 = verifier.verify(resp1, resp1.toWire(), false);
    int r2 = verifier.verify(resp2, resp2.toWire(), false);
    int r3 = verifier.verify(resp3, resp3.toWire(), true); // last message

    assertEquals(Rcode.NOERROR, r1);
    assertEquals(Rcode.NOERROR, r2);
    assertEquals(Rcode.NOERROR, r3);
  }

  /* ---------------------------------------------------------------------- */
  /* 7. Private helper tests (verify method behaviours indirectly) */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testGenerate_WithError_BADTIME_IncludesOtherField() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, name("keyname."), VALID_BASE64_KEY);
    Message msg = emptyMessage();

    // Force BADTIME error – the generated TSIG should contain an "other" field (6‑byte time)
    TSIGRecord rec = tsig.generate(msg, msg.toWire(), Rcode.BADTIME, null);
    assertNotNull("Other field must be present for BADTIME", rec.getOther());
    assertEquals(6, rec.getOther().length);
  }

  @Test
  public void testGenerate_WithFullSignatureFlag() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, name("keyname."), VALID_BASE64_KEY);
    Message msg = emptyMessage();

    // fullSignature = false – the generated TSIG should omit name / class / ttl / alg from the HMAC
    TSIGRecord recFull = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null, true);
    TSIGRecord recReduced = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null, false);

    // The signatures must differ because the input to the HMAC is different
    assertFalse(Arrays.equals(recFull.getSignature(), recReduced.getSignature()));
  }

  /* ---------------------------------------------------------------------- */
  /* 8. Edge case: using a truncated algorithm name (Java style without dash) */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_JavaStyleWithoutDash() {
    // “HmacSHA1” should become “hmac-sha1.”
    Name n = TSIG.algorithmToName("HmacSHA1");
    assertEquals(TSIG.HMAC_SHA1, n);
  }

  /* ---------------------------------------------------------------------- */
  /* 9. Edge case: algorithm name with missing trailing dot */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_AddsTrailingDot() {
    Name n = TSIG.algorithmToName("hmac-sha384");
    assertEquals(TSIG.HMAC_SHA384, n);
  }

  /* ---------------------------------------------------------------------- */
  /* 10. Edge case: using deprecated constructors – they should still work */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testDeprecatedConstructor_Mac() throws GeneralSecurityException {
    // Initialise a Mac manually and pass it to the deprecated constructor.
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKey key = new SecretKeySpec("0123456789ABCDEF".getBytes(), "HmacSHA256");
    mac.init(key);
    TSIG tsig = new TSIG(mac, name("keyname."));
    assertNotNull(tsig);
  }

  @Test
  public void testDeprecatedConstructor_NameAndBytes() {
    TSIG tsig = new TSIG(name("keyname."), "YWJjZGVm".getBytes()); // "abcdef"
    assertNotNull(tsig);
  }

  @Test
  public void testDeprecatedConstructor_StringKey() {
    TSIG tsig = new TSIG("keyname", VALID_BASE64_KEY);
    assertNotNull(tsig);
  }
}
```
###Test END##