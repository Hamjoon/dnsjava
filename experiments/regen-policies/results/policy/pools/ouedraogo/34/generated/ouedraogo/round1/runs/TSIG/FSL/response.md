###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link TSIG}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>algorithm ↔ name conversion (including error handling)
 *   <li>record length calculation
 *   <li>apply / generate / verify round‑trip
 *   <li>behaviour when a wrong key is used (BADKEY)
 *   <li>generation of a BADTIME record (presence of the “other” field)
 *   <li>stream generation and verification (StreamGenerator / StreamVerifier)
 *   <li>parsing of legacy {@code TSIG.fromString}
 * </ul>
 *
 * All objects are created using the public constructors and factory methods that appear in the
 * supplied source – no mocking or external libraries are used.
 */
public class TSIGTest {

  /** Helper to create a deterministic Clock (UTC) for reproducible timestamps. */
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);

  /** A simple 1‑byte key (base64 “AA==”). */
  private static final String ONE_BYTE_KEY_B64 = "AA==";

  /** Common name used for the TSIG key in the tests. */
  private static final Name KEY_NAME = Name.fromString("testkey.", Name.root);

  /** Build a TSIG instance using the MD5 algorithm and the one‑byte key. */
  private TSIG createMd5Tsig() {
    return new TSIG(TSIG.HMAC_MD5, KEY_NAME, ONE_BYTE_KEY_B64);
  }

  /** Build a TSIG instance using the SHA256 algorithm and the one‑byte key. */
  private TSIG createSha256Tsig() {
    return new TSIG(TSIG.HMAC_SHA256, KEY_NAME, ONE_BYTE_KEY_B64);
  }

  /** --------------------------------------------------------------------- */
  /** Test conversion from algorithm string to Name (valid inputs). */
  @Test
  public void testAlgorithmToNameValid() {
    // Alias handling (case‑insensitive, with/without trailing dot)
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    // Direct RFC names, different cases
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1"));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HMAC-SHA256."));
    // Java algorithm names (the values stored in the map)
    assertEquals(TSIG.HMAC_SHA384, TSIG.algorithmToName("HmacSHA384"));
  }

  /** Test conversion from algorithm string to Name (invalid input). */
  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameInvalid() {
    TSIG.algorithmToName("unsupported-alg");
  }

  /** --------------------------------------------------------------------- */
  /** Test conversion from Name to the Java algorithm string (valid and invalid). */
  @Test
  public void testNameToAlgorithmValid() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA224", TSIG.nameToAlgorithm(TSIG.HMAC_SHA224));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithmInvalid() {
    // Create a Name that is not present in the map
    Name unknown = Name.fromString("unknown.", Name.root);
    TSIG.nameToAlgorithm(unknown);
  }

  /** --------------------------------------------------------------------- */
  /** Verify that the calculated maximum record length matches the expected formula. */
  @Test
  public void testRecordLength() {
    TSIG tsig = createMd5Tsig();
    int expected =
        KEY_NAME.length()
            + 10
            + TSIG.HMAC_MD5.length()
            + 8 // time signed + fudge
            + 18 // MAC length (2) + max MAC (16)
            + 4 // original id + error
            + 8; // other length field (2) + max other data (6)
    assertEquals(expected, tsig.recordLength());
  }

  /** --------------------------------------------------------------------- */
  /** Round‑trip test: apply a TSIG to a message and then verify it successfully. */
  @Test
  public void testApplyAndVerifySuccess() throws Exception {
    TSIG tsig = createMd5Tsig();

    Message query = new Message(); // empty message, id = 0
    tsig.apply(query, null); // sign the message
    assertNotNull("TSIG record should be attached", query.getTSIG());
    assertTrue("Message should be marked as signed", query.isSigned());

    // Verify the same message with the same key
    int result = tsig.verify(query, query.toWire(), null);
    assertEquals("Verification should succeed", Rcode.NOERROR, result);
    assertTrue("Message should be marked as verified", query.isVerified());
  }

  /** --------------------------------------------------------------------- */
  /** Verification with a different key must fail with BADKEY. */
  @Test
  public void testVerifyBadKey() throws Exception {
    TSIG correctKey = createMd5Tsig();
    TSIG wrongKey = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, ONE_BYTE_KEY_B64);

    Message msg = new Message();
    correctKey.apply(msg, null); // signed with MD5 key

    int result = wrongKey.verify(msg, msg.toWire(), null);
    assertEquals("Verification with wrong key must return BADKEY", Rcode.BADKEY, result);
    assertFalse("Message should not be marked as verified", msg.isVerified());
  }

  /** --------------------------------------------------------------------- */
  /** Generate a TSIG record with error BADTIME and ensure the “other” field is present. */
  @Test
  public void testGenerateBadTimeOtherField() throws Exception {
    TSIG tsig = createMd5Tsig();

    Message msg = new Message();
    // generate a BADTIME record (no previous TSIG, fullSignature = true)
    TSIGRecord tsigRecord = tsig.generate(msg, msg.toWire(), Rcode.BADTIME, null, true);
    assertNotNull("TSIGRecord must be returned", tsigRecord);
    assertNotNull("Other field must be present for BADTIME", tsigRecord.getOther());
    assertEquals("Other field length for BADTIME must be 6 bytes", 6, tsigRecord.getOther().length);
  }

  /** --------------------------------------------------------------------- */
  /** Test the deprecated {@code TSIG.fromString} parsing logic. */
  @Test
  public void testFromStringParsing() throws Exception {
    // format: name:secret
    TSIG tsig = TSIG.fromString("mykey.:AA==");
    assertNotNull(tsig);
    // The algorithm should default to HMAC_MD5
    assertEquals(TSIG.HMAC_MD5, tsig.alg);
    // The key name must be parsed correctly (case‑insensitive, trailing dot optional)
    assertEquals(Name.fromString("mykey.", Name.root), tsig.name);
  }

  /** --------------------------------------------------------------------- */
  /** Stream generation + verification round‑trip (first, middle, last messages). */
  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    TSIG tsig = createSha256Tsig();

    // ----- Build a query message with a TSIG record (simulates a client request) -----
    Message query = new Message();
    tsig.apply(query, null); // attach TSIG to the query
    TSIGRecord queryTsig = query.getTSIG();
    assertNotNull(queryTsig);

    // ----- StreamGenerator will sign a series of 3 response messages -----
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);
    Message resp1 = new Message();
    generator.generate(resp1); // first message – signed
    Message resp2 = new Message();
    generator.generate(resp2); // second message – also signed (signEveryNthMessage = 1)
    Message resp3 = new Message();
    generator.generate(resp3); // third (last) message – signed

    // ----- Verify the stream using StreamVerifier -----
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // 1st response – must verify successfully and become the “last signed” message
    int r1 = verifier.verify(resp1, resp1.toWire());
    assertEquals(Rcode.NOERROR, r1);
    assertTrue(resp1.isVerified());

    // 2nd response – also signed, should verify
    int r2 = verifier.verify(resp2, resp2.toWire());
    assertEquals(Rcode.NOERROR, r2);
    assertTrue(resp2.isVerified());

    // 3rd response – last message, verify again
    int r3 = verifier.verify(resp3, resp3.toWire(), true);
    assertEquals(Rcode.NOERROR, r3);
    assertTrue(resp3.isVerified());

    // No error message should have been recorded
    assertNull(verifier.getErrorMessage());
  }

  /** --------------------------------------------------------------------- */
  /** Verify that the internal time‑fudge handling works (using the fixed clock). */
  @Test
  public void testTimeFudgeVerification() throws Exception {
    // Use a TSIG constructed with a fixed clock so we can predict the timestamp.
    TSIG tsig =
        new TSIG(
            TSIG.HMAC_MD5,
            KEY_NAME,
            ONE_BYTE_KEY_B64,
            new SecretKeySpec(base64.fromString(ONE_BYTE_KEY_B64), "HmacMD5"),
            FIXED_CLOCK);

    Message msg = new Message();
    tsig.apply(msg, null);
    // The TSIG record now contains a timestamp exactly equal to FIXED_CLOCK.instant()
    int result = tsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
  }

  /** --------------------------------------------------------------------- */
  /** Ensure that {@code initHmac()} correctly clones a shared Mac instance (covered via
   *  StreamGenerator which re‑uses the same Mac). */
  @Test
  public void testSharedMacReuseInStreamGenerator() throws Exception {
    TSIG tsig = createMd5Tsig();

    Message query = new Message();
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Create a generator that will reuse the same Mac for several messages
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

    Message first = new Message();
    generator.generate(first); // uses shared Mac, signature added
    Message second = new Message();
    generator.generate(second); // same Mac instance should be reused without error

    // Both messages must contain a TSIG record
    assertNotNull(first.getTSIG());
    assertNotNull(second.getTSIG());
  }
}
###Test END##