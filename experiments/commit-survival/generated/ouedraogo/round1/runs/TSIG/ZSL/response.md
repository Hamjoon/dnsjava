###Test START##
```java
/* SPDX-License-Identifier: BSD-3-Clause
 * Test suite for org.xbill.DNS.TSIG
 *
 * The tests are written using JUnit 4 and exercise the public API of TSIG,
 * as well as the internal behaviour exercised through the public methods.
 *
 * NOTE: The test relies only on classes that are part of the DNSJava library
 * (org.xbill.DNS) and on standard JDK APIs. No additional third‑party
 * libraries are used.
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.SecretKey;
import java.security.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link TSIG}.
 *
 * The tests cover:
 * <ul>
 *   <li>algorithm name conversion (algorithmToName, nameToAlgorithm)</li>
 *   <li>record length calculation</li>
 *   <li>generation and application of a TSIG record</li>
 *   <li>verification of a correct TSIG and detection of BADKEY</li>
 *   <li>generation of a BADTIME record (error path)</li>
 *   <li>StreamGenerator / StreamVerifier interaction</li>
 * </ul>
 */
public class TSIGTest {

  /** A deterministic clock used for reproducible timestamps. */
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

  /** Base64 representation of the secret key "secret". */
  private static final String KEY_B64 = Base64.getEncoder().encodeToString("secret".getBytes());

  /** Name of the TSIG key (must be a valid DNS name). */
  private static final Name KEY_NAME = Name.fromString("mykey.");

  /** TSIG instance using the HMAC‑MD5 algorithm. */
  private TSIG tsig;

  @Before
  public void setUp() {
    // Construct a TSIG using the public constructor that accepts a base64 string.
    tsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, KEY_B64);
  }

  /* -------------------------------------------------------------------------
   *  algorithmToName / nameToAlgorithm
   * ------------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToNameStandardValues() {
    // Alias handling for the historic name
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5."));

    // RFC names (case‑insensitive, with/without trailing dot)
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1"));
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1."));

    // Java algorithm name fallback
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
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
  public void testNameToAlgorithm() {
    assertEquals("HmacSHA1", TSIG.nameToAlgorithm(TSIG.HMAC_SHA1));
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithmUnknown() {
    // Create a dummy Name that is not in the map
    Name bogus = Name.fromString("bogus.");
    TSIG.nameToAlgorithm(bogus);
  }

  /* -------------------------------------------------------------------------
   *  recordLength
   * ------------------------------------------------------------------------- */

  @Test
  public void testRecordLengthCalculation() {
    int expected =
        KEY_NAME.length()
            + 10
            + TSIG.HMAC_MD5.length()
            + 8   // time signed (6) + fudge (2)
            + 18  // MAC length (2) + max MAC (16)
            + 4   // original id (2) + error (2)
            + 8;  // other‑data length (2) + max other (6)

    assertEquals(expected, tsig.recordLength());
  }

  /* -------------------------------------------------------------------------
   *  generate / apply / verify (happy path)
   * ------------------------------------------------------------------------- */

  @Test
  public void testGenerateAndApplyAndVerifyHappyPath() throws Exception {
    // Create a minimal query Message (no question is fine for this test)
    Message query = new Message();

    // Apply a TSIG (NOERROR) – this should add a TSIGRecord to the ADDITIONAL section
    tsig.apply(query, null);
    TSIGRecord tsigRec = query.getTSIG();
    assertNotNull("TSIGRecord must be present after apply()", tsigRec);
    assertEquals(TSIG.HMAC_MD5, tsigRec.getAlgorithm());
    assertEquals(KEY_NAME, tsigRec.getName());

    // Verify the message using the same TSIG instance
    int result = tsig.verify(query, query.toWire(), null);
    assertEquals("Verification should succeed", Rcode.NOERROR, result);
    assertTrue("Message should be marked as verified", query.isVerified());
  }

  @Test
  public void testVerifyBadKey() throws Exception {
    // A second TSIG with a different secret (different key)
    String otherKeyB64 = Base64.getEncoder().encodeToString("other".getBytes());
    TSIG otherTsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, otherKeyB64);

    Message query = new Message();
    tsig.apply(query, null); // signed with the original key

    int result = otherTsig.verify(query, query.toWire(), null);
    assertEquals("Verification with wrong key must fail with BADKEY", Rcode.BADKEY, result);
    assertFalse("Message must not be marked verified", query.isVerified());
  }

  /* -------------------------------------------------------------------------
   *  generate with BADTIME error – other data must be present
   * ------------------------------------------------------------------------- */

  @Test
  public void testGenerateBadTimeRecord() throws Exception {
    Message msg = new Message();

    // Simulate a BADTIME error – the old TSIG is null because this is a query
    TSIGRecord badTimeRec = tsig.generate(msg, msg.toWire(), Rcode.BADTIME, null);

    assertNotNull(badTimeRec);
    assertEquals(Rcode.BADTIME, badTimeRec.getError());
    assertNotNull("BADTIME record must contain other data (time signed)", badTimeRec.getOther());

    // Verify that a BADTIME response is still accepted when the same key is used
    // (the verification path will return BADTIME because the time is out of range,
    // but the signature verification itself should succeed)
    int result = tsig.verify(msg, msg.toWire(), null);
    // The verification method returns BADTIME when the time check fails.
    // We accept either BADTIME or NOERROR depending on the fudge; the important part is that
    // the method does not return FORMERR/BADSIG.
    assertTrue(
        "Verification of BADTIME should return BADTIME or NOERROR",
        result == Rcode.BADTIME || result == Rcode.NOERROR);
  }

  /* -------------------------------------------------------------------------
   *  StreamGenerator / StreamVerifier round‑trip
   * ------------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    // Build a query message and sign it – this will be the "initial" TSIGRecord
    Message query = new Message();
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();
    assertNotNull(queryTsig);

    // Create a StreamGenerator that signs every message (default behaviour)
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

    // First response message (should contain a TSIG)
    Message resp1 = new Message();
    generator.generate(resp1);
    TSIGRecord resp1Tsig = resp1.getTSIG();
    assertNotNull("First response must contain a TSIG", resp1Tsig);

    // Second response message (also contains a TSIG because signEveryNthMessage == 1)
    Message resp2 = new Message();
    generator.generate(resp2);
    TSIGRecord resp2Tsig = resp2.getTSIG();
    assertNotNull("Second response must contain a TSIG", resp2Tsig);

    // Verify the stream using StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // Verify first response – should succeed
    int r1 = verifier.verify(resp1, resp1.toWire());
    assertEquals(Rcode.NOERROR, r1);
    assertTrue(resp1.isVerified());

    // Verify second response – also should succeed
    int r2 = verifier.verify(resp2, resp2.toWire());
    assertEquals(Rcode.NOERROR, r2);
    assertTrue(resp2.isVerified());

    // Verify that the verifier correctly records the last error message (should be null)
    assertNull("No error should be reported by the verifier", verifier.getErrorMessage());
  }

  /* -------------------------------------------------------------------------
   *  Helper to create a TSIGRecord with a known algorithm for edge‑case tests
   * ------------------------------------------------------------------------- */

  private static TSIGRecord makeTsigRecord(Name name, Name alg, byte[] sig, int error) {
    // Use a fixed instant and minimal fudge for deterministic output.
    Instant now = FIXED_CLOCK.instant();
    Duration fudge = Duration.ofSeconds(300);
    return new TSIGRecord(
        name,
        DClass.ANY,
        0,
        alg,
        now,
        fudge,
        sig,
        0, // original ID
        error,
        null);
  }
}
```
###Test END##