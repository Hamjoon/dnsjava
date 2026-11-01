###Test START##
```java
/*
 * JUnit‑4 test suite for {@link org.xbill.DNS.TSIG}.
 *
 * The tests cover:
 *   – algorithm ↔ name conversion (including error handling)
 *   – construction of TSIG objects from the various public constructors
 *   – generation of TSIG records (signature presence, fields)
 *   – applying a TSIG to a message and successful verification
 *   – detection of tampered signatures (BADSIG)
 *   – time‑validation failures (BADTIME) using a fixed Clock
 *   – recordLength sanity check
 *   – StreamGenerator / StreamVerifier interaction for multi‑message streams
 *
 * All objects are created using only the public API of the DNS library;
 * no mocking or subclassing is performed.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;
import org.xbill.DNS.utils.base64;

/**
 * Test class for {@link TSIG}.
 */
public class TSIGTest {

  /** Helper to create a simple TSIG key using HMAC‑SHA256. */
  private static TSIG createTestKey() throws Exception {
    Name alg = TSIG.HMAC_SHA256;
    Name name = Name.fromString("testkey.");
    byte[] keyBytes = new byte[] {0x01, 0x02, 0x03, 0x04};
    SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
    return new TSIG(alg, name, key);
  }

  /** Helper to create a Message with a known ID. */
  private static Message newMessage(int id) {
    return new Message(id);
  }

  /* ---------------------------------------------------------------------- */
  /* 1. algorithmToName – normal, alias, case‑insensitive and error cases   */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName() {
    // alias handling (case‑insensitive)
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));

    // normal mapping, case‑insensitive
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HMAC-SHA256."));

    // Java algorithm name fallback
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("HmacSHA1"));

    // null argument → IllegalArgumentException
    try {
      TSIG.algorithmToName(null);
      fail("Expected IllegalArgumentException for null algorithm");
    } catch (IllegalArgumentException ignored) {
    }

    // unknown algorithm → IllegalArgumentException
    try {
      TSIG.algorithmToName("unknown-alg");
      fail("Expected IllegalArgumentException for unknown algorithm");
    } catch (IllegalArgumentException ignored) {
    }
  }

  /* ---------------------------------------------------------------------- */
  /* 2. nameToAlgorithm – normal mapping and error case (deprecated method) */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testNameToAlgorithm() {
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));

    // unknown name → IllegalArgumentException
    try {
      TSIG.nameToAlgorithm(Name.fromString("unknown."));
      fail("Expected IllegalArgumentException for unknown name");
    } catch (IllegalArgumentException ignored) {
    }
  }

  /* ---------------------------------------------------------------------- */
  /* 3. Constructors – base64 string, byte[] and SecretKey variants           */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testConstructors() throws Exception {
    // base64 string constructor
    String b64Key = base64.toString(new byte[] {0x0A, 0x0B, 0x0C});
    TSIG tsigFromString = new TSIG(TSIG.HMAC_SHA1, Name.fromString("mykey."), b64Key);
    assertNotNull(tsigFromString);

    // byte[] constructor
    byte[] keyBytes = new byte[] {0x01, 0x02, 0x03};
    TSIG tsigFromBytes = new TSIG(TSIG.HMAC_SHA1, Name.fromString("mykey2."), keyBytes);
    assertNotNull(tsigFromBytes);

    // SecretKey constructor
    SecretKey sk = new SecretKeySpec(keyBytes, "HmacSHA1");
    TSIG tsigFromKey = new TSIG(TSIG.HMAC_SHA1, Name.fromString("mykey3."), sk);
    assertNotNull(tsigFromKey);
  }

  /* ---------------------------------------------------------------------- */
  /* 4. generate – produces a TSIGRecord with a non‑empty signature        */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testGenerateProducesSignature() throws Exception {
    TSIG tsig = createTestKey();
    Message msg = newMessage(0x1234);
    TSIGRecord rec = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    assertNotNull(rec);
    assertEquals(tsig.HMAC_SHA256, rec.getAlgorithm());
    assertEquals(Name.fromString("testkey."), rec.getName());
    assertTrue("Signature should not be empty", rec.getSignature().length > 0);
  }

  /* ---------------------------------------------------------------------- */
  /* 5. apply + verify – happy path                                         */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testApplyAndVerifySuccess() throws Exception {
    TSIG tsig = createTestKey();
    Message msg = newMessage(0x4321);
    tsig.apply(msg, null);               // signs the message
    assertTrue(msg.isSigned());

    byte[] wire = msg.toWire();
    int result = tsig.verify(msg, wire, null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue(msg.isVerified());
  }

  /* ---------------------------------------------------------------------- */
  /* 6. verify detects a tampered signature (BADSIG)                        */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testVerifyDetectsBadSignature() throws Exception {
    TSIG tsig = createTestKey();
    Message msg = newMessage(0x1111);
    tsig.apply(msg, null);
    TSIGRecord good = msg.getTSIG();

    // create a copy with a corrupted signature
    byte[] badSig = Arrays.copyOf(good.getSignature(), good.getSignature().length);
    badSig[0] ^= 0xFF; // flip a bit

    TSIGRecord badRec =
        new TSIGRecord(
            good.getName(),
            DClass.ANY,
            0,
            good.getAlgorithm(),
            good.getTimeSigned(),
            good.getFudge(),
            badSig,
            good.getOriginalID(),
            good.getError(),
            good.getOther());

    // replace the original TSIG record
    msg.removeAllRecords(Section.ADDITIONAL);
    msg.addRecord(badRec, Section.ADDITIONAL);

    int result = tsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADSIG, result);
  }

  /* ---------------------------------------------------------------------- */
  /* 7. verifyTime – BADTIME when clock is far from the signed time        */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testVerifyTimeBadTime() throws Exception {
    // Clock fixed 2 hours in the past, default fudge = 300 s → should be BADTIME
    Clock pastClock = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, Name.fromString("testkey."), new SecretKeySpec(
        new byte[] {0x01, 0x01, 0x01, 0x01}, "HmacSHA256"), pastClock);

    Message msg = newMessage(0x2222);
    tsig.apply(msg, null);
    int result = tsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADTIME, result);
  }

  /* ---------------------------------------------------------------------- */
  /* 8. recordLength – sanity check (value should be >= minimal size)       */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testRecordLength() throws Exception {
    TSIG tsig = createTestKey();
    int len = tsig.recordLength();
    // Minimal possible length is calculated in the Javadoc (≈ 50 bytes)
    assertTrue("recordLength should be non‑negative", len > 0);
    assertTrue("recordLength should be at least 50", len >= 50);
  }

  /* ---------------------------------------------------------------------- */
  /* 9. StreamGenerator / StreamVerifier – multi‑message stream handling     */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    TSIG tsig = createTestKey();

    /* ----- create a query message and sign it ----- */
    Message query = newMessage(0x3333);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();
    assertNotNull(queryTsig);

    /* ----- generate two response messages in a stream ----- */
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);
    Message resp1 = newMessage(0x3334);
    Message resp2 = newMessage(0x3335);

    generator.generate(resp1);                 // first response (signed)
    generator.generate(resp2, true);           // last response (forced signed)

    /* ----- verify the stream using StreamVerifier ----- */
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);
    int r1 = verifier.verify(resp1, resp1.toWire(), false);
    assertEquals(Rcode.NOERROR, r1);
    int r2 = verifier.verify(resp2, resp2.toWire(), true);
    assertEquals(Rcode.NOERROR, r2);
    assertTrue(resp2.isVerified());
  }

  /* ---------------------------------------------------------------------- */
  /* 10. generate with BADTIME error – ensures “other” field is populated   */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testGenerateBadTimeIncludesOtherField() throws Exception {
    TSIG tsig = createTestKey();
    Message msg = newMessage(0x4444);

    // Simulate BADTIME error, providing a previous TSIGRecord (null is fine here)
    TSIGRecord rec = tsig.generate(msg, msg.toWire(), Rcode.BADTIME, null);
    assertNotNull(rec.getOther());
    // The “other” field must be exactly 6 bytes (time + fudge)
    assertEquals(6, rec.getOther().length);
  }
}
```
###Test END##