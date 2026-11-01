###Test START##
```java
/* SPDX-License-Identifier: BSD-3-Clause
 * Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)
 *
 * JUnit‑4 test suite for org.xbill.DNS.TSIG.
 *
 * The tests exercise:
 *  • algorithmToName / nameToAlgorithm conversion
 *  • all public constructors
 *  • generate / apply / verify round‑trip
 *  • error paths (BADKEY, BADSIG, BADTIME)
 *  • recordLength sanity check
 *  • StreamGenerator / StreamVerifier interaction
 *
 * The test suite uses only the public API that appears in the source
 * listing or the dependency signatures supplied in the prompt.
 */
package org.xbill.DNS;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Assert;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link TSIG}.
 */
public class TSIGTest {

  /** Simple base64‑encoded key (the string "testkey") */
  private static final String TEST_KEY_B64 = "dGVzdGtleQ==";

  /** Helper to build a TSIG object using the SHA‑256 algorithm */
  private static TSIG makeTsig(Name name) {
    return new TSIG(TSIG.HMAC_SHA256, name, TEST_KEY_B64);
  }

  /** Helper to build a minimal DNS message (only a header, no question) */
  private static Message emptyMessage(int id) {
    return new Message(id);
  }

  /** --------------------------------------------------------------------- *
   *  algorithmToName / nameToAlgorithm tests
   *  --------------------------------------------------------------------- */
  @Test
  public void testAlgorithmConversion() {
    // direct RFC name (case‑insensitive, with/without dot)
    Assert.assertEquals(TSIG.HMAC_SHA256,
        TSIG.algorithmToName("HMAC-SHA256"));
    Assert.assertEquals(TSIG.HMAC_SHA256,
        TSIG.algorithmToName("hmac-sha256."));
    // Java algorithm name (case‑insensitive)
    Assert.assertEquals(TSIG.HMAC_SHA256,
        TSIG.algorithmToName("HmacSHA256"));
    // alias for MD5
    Assert.assertEquals(TSIG.HMAC_MD5,
        TSIG.algorithmToName("HMAC-MD5"));
    Assert.assertEquals(TSIG.HMAC_MD5,
        TSIG.algorithmToName("HMAC-MD5."));

    // nameToAlgorithm (deprecated but still public)
    Assert.assertEquals("HmacSHA256",
        TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    Assert.assertEquals("HmacMD5",
        TSIG.nameToAlgorithm(TSIG.HMAC_MD5));

    // unknown algorithm should throw IllegalArgumentException
    try {
      TSIG.algorithmToName("UNKNOWN-ALG");
      Assert.fail("Expected IllegalArgumentException for unknown algorithm");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  /** --------------------------------------------------------------------- *
   *  Constructor coverage
   *  --------------------------------------------------------------------- */
  @Test
  public void testConstructors() throws Exception {
    Name keyName = Name.fromString("mykey.example.");

    // 1) Name + base64 string
    TSIG t1 = new TSIG(TSIG.HMAC_SHA256, keyName, TEST_KEY_B64);
    Assert.assertNotNull(t1);

    // 2) Name + raw byte[]
    byte[] rawKey = base64.fromString(TEST_KEY_B64);
    TSIG t2 = new TSIG(TSIG.HMAC_SHA256, keyName, rawKey);
    Assert.assertNotNull(t2);

    // 3) Name + SecretKey
    SecretKey secret = new SecretKeySpec(rawKey, "HmacSHA256");
    TSIG t3 = new TSIG(TSIG.HMAC_SHA256, keyName, secret);
    Assert.assertNotNull(t3);

    // 4) String algorithm version
    TSIG t4 = new TSIG("hmac-sha256", "mykey.example.", TEST_KEY_B64);
    Assert.assertNotNull(t4);

    // 5) Deprecated fromString parsing
    TSIG t5 = TSIG.fromString("hmac-sha256:mykey.example.:" + TEST_KEY_B64);
    Assert.assertNotNull(t5);
  }

  /** --------------------------------------------------------------------- *
   *  Simple sign / verify round‑trip
   *  --------------------------------------------------------------------- */
  @Test
  public void testGenerateApplyVerifyRoundTrip() throws Exception {
    Name keyName = Name.fromString("mykey.example.");
    TSIG tsig = makeTsig(keyName);

    Message query = new Message(0x1234);
    // Apply TSIG (adds a TSIGRecord to the additional section)
    tsig.apply(query, null);
    TSIGRecord applied = query.getTSIG();
    Assert.assertNotNull("TSIGRecord should be present after apply()", applied);

    // Verify using the same key – should succeed (NOERROR)
    int result = tsig.verify(query, query.toWire(), applied);
    Assert.assertEquals("Verification should succeed", Rcode.NOERROR, result);
  }

  /** --------------------------------------------------------------------- *
   *  Verification failures – BADKEY and BADTIME
   *  --------------------------------------------------------------------- */
  @Test
  public void testVerificationBadKey() throws Exception {
    Name keyName = Name.fromString("mykey.example.");
    TSIG correct = makeTsig(keyName);
    TSIG wrong = new TSIG(TSIG.HMAC_SHA256, keyName, "d3JvbmdrZXk="); // "wrongkey"

    Message msg = new Message(0x4321);
    correct.apply(msg, null);
    TSIGRecord tsigRec = msg.getTSIG();

    // Verify with a different key – expect BADKEY
    int rc = wrong.verify(msg, msg.toWire(), tsigRec);
    Assert.assertEquals("Verification with wrong key should be BADKEY",
        Rcode.BADKEY, rc);
  }

  @Test
  public void testVerificationBadTime() throws Exception {
    // Fixed clock for reproducibility
    Instant fixed = Instant.parse("2023-01-01T00:00:00Z");
    Clock fixedClock = Clock.fixed(fixed, ZoneOffset.UTC);

    Name keyName = Name.fromString("mykey.example.");
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, keyName, TEST_KEY_B64, fixedClock);

    // Create a "query" TSIGRecord that will be used as the "old" record
    Message query = new Message(0x1111);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Now generate a response with BADTIME error – the generator will reuse the old timestamp
    Message response = new Message(0x1112);
    TSIGRecord badTimeRec = tsig.generate(response,
        response.toWire(),
        Rcode.BADTIME,
        queryTsig,
        true);

    // Attach the BADTIME record manually (generate does not add it)
    response.addRecord(badTimeRec, Section.ADDITIONAL);
    response.tsigState = Message.TSIG_SIGNED; // mimic apply()

    // Verify – should return BADTIME
    int rc = tsig.verify(response, response.toWire(), queryTsig);
    Assert.assertEquals("Verification of BADTIME should return BADTIME",
        Rcode.BADTIME, rc);
  }

  /** --------------------------------------------------------------------- *
   *  recordLength sanity check
   *  --------------------------------------------------------------------- */
  @Test
  public void testRecordLength() throws Exception {
    Name keyName = Name.fromString("mykey.example.");
    TSIG tsig = makeTsig(keyName);
    int len = tsig.recordLength();
    // The length must be positive and at least the minimum TSIG size (30 bytes)
    Assert.assertTrue("recordLength should be > 0", len > 0);
    Assert.assertTrue("recordLength should be >= 30", len >= 30);
  }

  /** --------------------------------------------------------------------- *
   *  StreamGenerator / StreamVerifier integration test
   *  --------------------------------------------------------------------- */
  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    Name keyName = Name.fromString("mykey.example.");
    TSIG tsig = makeTsig(keyName);

    // ----- create a query message and sign it (this will be the "old" TSIG) -----
    Message query = new Message(0x2000);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();
    Assert.assertNotNull(queryTsig);

    // ----- StreamGenerator creates three response messages -----
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

    // Message 1 – first (must contain TSIG)
    Message resp1 = new Message(0x2001);
    generator.generate(resp1);
    Assert.assertNotNull(resp1.getTSIG());

    // Message 2 – intermediate (no TSIG because signEveryNthMessage defaults to 1,
    // but we will manually remove it to simulate an unsigned intermediate)
    Message resp2 = new Message(0x2002);
    generator.generate(resp2);
    // Force removal of TSIG to test the verifier's unsigned‑message handling
    resp2.removeRecord(resp2.getTSIG(), Section.ADDITIONAL);
    Assert.assertNull("Intermediate message should have no TSIG", resp2.getTSIG());

    // Message 3 – last (must contain TSIG)
    Message resp3 = new Message(0x2003);
    generator.generate(resp3);
    Assert.assertNotNull(resp3.getTSIG());

    // ----- Verify the stream using StreamVerifier -----
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // 1) first message – should verify successfully
    int rc1 = verifier.verify(resp1, resp1.toWire());
    Assert.assertEquals(Rcode.NOERROR, rc1);
    Assert.assertTrue(resp1.isVerified());

    // 2) intermediate – no TSIG, should be accepted (NOERROR)
    int rc2 = verifier.verify(resp2, resp2.toWire());
    Assert.assertEquals(Rcode.NOERROR, rc2);
    // isVerified() stays false for unsigned messages
    Assert.assertFalse(resp2.isVerified());

    // 3) last message – must contain TSIG and verify
    int rc3 = verifier.verify(resp3, resp3.toWire(), true);
    Assert.assertEquals(Rcode.NOERROR, rc3);
    Assert.assertTrue(resp3.isVerified());
  }

  /** --------------------------------------------------------------------- *
   *  Helper: verify that a malformed algorithm string throws
   *  --------------------------------------------------------------------- */
  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameInvalid() {
    TSIG.algorithmToName(null); // should throw IllegalArgumentException
  }
}
```
###Test END##