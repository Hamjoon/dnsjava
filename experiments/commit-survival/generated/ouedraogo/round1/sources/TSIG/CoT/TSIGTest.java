/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.TSIG}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public static utility methods {@code algorithmToName} and {@code nameToAlgorithm}</li>
 *   <li>All public constructors (including deprecated ones)</li>
 *   <li>Signature generation via {@code generate(...)} and {@code apply(...)}</li>
 *   <li>Verification via {@code verify(...)} – success and failure cases</li>
 *   <li>Edge‑case handling (null/invalid arguments, unknown algorithms)</li>
 *   <li>Utility methods {@code recordLength()}</li>
 *   <li>Inner helper classes {@code StreamGenerator} and {@code StreamVerifier}</li>
 * </ul>
 *
 * The tests use real DNS objects from the library (no mocking) and a fixed {@link java.time.Clock}
 * to make timestamps deterministic.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;

public class TSIGTest {

  private static final Name KEY_NAME = Name.fromString("testkey.");
  private static final String BASE64_KEY = Base64.getEncoder().encodeToString("secret".getBytes());
  private static final byte[] KEY_BYTES = "secret".getBytes();
  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

  private TSIG tsig;               // primary TSIG used for most tests
  private Message emptyMessage;    // a minimal DNS message (no question/records)

  @Before
  public void setUp() {
    // construct a TSIG using the MD5 algorithm and a deterministic clock
    tsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, new SecretKeySpec(KEY_BYTES, "HmacMD5"));
    // set the same fixed clock inside the TSIG (uses the constructor that accepts a Clock)
    tsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, new SecretKeySpec(KEY_BYTES, "HmacMD5"), FIXED_CLOCK);
    emptyMessage = new Message(); // a perfectly valid, empty DNS message
  }

  /* --------------------------------------------------------------
   *  Public static utilities
   * -------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_validAlias() {
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5."));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_null() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_unknown() {
    TSIG.algorithmToName("unknown-alg");
  }

  @Test
  public void testNameToAlgorithm_known() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_unknown() {
    Name bogus = Name.fromString("bogus.");
    TSIG.nameToAlgorithm(bogus);
  }

  /* --------------------------------------------------------------
   *  Constructors – happy path & error handling
   * -------------------------------------------------------------- */

  @Test
  public void testConstructor_stringKey() {
    TSIG t = new TSIG(TSIG.HMAC_MD5, KEY_NAME, BASE64_KEY);
    assertNotNull(t);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_invalidBase64() {
    new TSIG(TSIG.HMAC_MD5, KEY_NAME, "!!! not base64 !!!");
  }

  @Test
  public void testDeprecatedFromString_validTwoPart() {
    TSIG t = TSIG.fromString("testkey:" + BASE64_KEY);
    assertNotNull(t);
  }

  @Test
  public void testDeprecatedFromString_validThreePart() {
    TSIG t = TSIG.fromString("hmac-sha1:testkey:" + BASE64_KEY);
    assertNotNull(t);
    assertEquals(TSIG.HMAC_SHA1, t.alg);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeprecatedFromString_invalidFormat() {
    TSIG.fromString("onlyonepart");
  }

  /* --------------------------------------------------------------
   *  Signature generation and verification (basic flow)
   * -------------------------------------------------------------- */

  @Test
  public void testGenerateAndVerify_success() {
    // generate a TSIG record for an empty message
    TSIGRecord tsigRec = tsig.generate(emptyMessage, emptyMessage.toWire(), Rcode.NOERROR, null);
    assertNotNull(tsigRec);
    // verification should succeed
    int result = tsig.verify(emptyMessage, emptyMessage.toWire(), tsigRec);
    assertEquals(Rcode.NOERROR, result);
  }

  @Test
  public void testVerify_badKey() {
    // a second TSIG with a different secret
    TSIG other = new TSIG(TSIG.HMAC_MD5, KEY_NAME,
        new SecretKeySpec("different".getBytes(), "HmacMD5"), FIXED_CLOCK);
    TSIGRecord rec = tsig.generate(emptyMessage, emptyMessage.toWire(), Rcode.NOERROR, null);
    int result = other.verify(emptyMessage, emptyMessage.toWire(), rec);
    assertEquals(Rcode.BADKEY, result);
  }

  @Test
  public void testVerify_badSignature() {
    TSIGRecord rec = tsig.generate(emptyMessage, emptyMessage.toWire(), Rcode.NOERROR, null);
    // corrupt the signature bytes
    byte[] badSig = rec.getSignature().clone();
    badSig[0] ^= 0xFF;
    TSIGRecord corrupted = new TSIGRecord(
        rec.getName(), rec.dclass, rec.ttl, rec.getAlgorithm(),
        rec.getTimeSigned(), rec.getFudge(), badSig,
        rec.getOriginalID(), rec.getError(), rec.getOther());
    int result = tsig.verify(emptyMessage, emptyMessage.toWire(), corrupted);
    assertEquals(Rcode.BADSIG, result);
  }

  @Test
  public void testApplyAddsTsigRecord() {
    tsig.apply(emptyMessage, (TSIGRecord) null);
    TSIGRecord rec = emptyMessage.getTSIG();
    assertNotNull(rec);
    assertEquals(tsig.name, rec.getName());
  }

  @Test
  public void testRecordLength_isPositive() {
    int len = tsig.recordLength();
    assertTrue(len > 0);
  }

  /* --------------------------------------------------------------
   *  Edge cases for verification – missing TSIG, malformed TSIG
   * -------------------------------------------------------------- */

  @Test
  public void testVerify_missingTsig_returnsFormErr() {
    int result = tsig.verify(emptyMessage, emptyMessage.toWire(), null);
    assertEquals(Rcode.FORMERR, result);
  }

  @Test
  public void testVerify_wrongAlgorithm_returnsBadKey() {
    // generate a TSIG with SHA256 algorithm but verify with a TSIG expecting MD5
    TSIG sha256Tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME,
        new SecretKeySpec(KEY_BYTES, "HmacSHA256"), FIXED_CLOCK);
    TSIGRecord rec = sha256Tsig.generate(emptyMessage, emptyMessage.toWire(), Rcode.NOERROR, null);
    int result = tsig.verify(emptyMessage, emptyMessage.toWire(), rec);
    assertEquals(Rcode.BADKEY, result);
  }

  /* --------------------------------------------------------------
   *  StreamGenerator tests
   * -------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testStreamGenerator_invalidSignEveryNth() {
    new TSIG.StreamGenerator(tsig, null, 0);
  }

  @Test
  public void testStreamGenerator_generatesTsigOnFirstMessage() {
    Message msg = new Message();
    TSIGRecord queryTsig = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsig, queryTsig);
    gen.generate(msg);
    TSIGRecord added = msg.getTSIG();
    assertNotNull(added);
    assertEquals(tsig.name, added.getName());
  }

  /* --------------------------------------------------------------
   *  StreamVerifier tests
   * -------------------------------------------------------------- */

  @Test
  public void testStreamVerifier_successfulSequence() {
    // first message (must contain TSIG)
    Message first = new Message();
    TSIGRecord query = tsig.generate(first, first.toWire(), Rcode.NOERROR, null);
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, query);

    // verify first message
    int res1 = verifier.verify(first, first.toWire(), true);
    assertEquals(Rcode.NOERROR, res1);
    assertNull(verifier.getErrorMessage());

    // second message without TSIG (intermediate)
    Message second = new Message();
    int res2 = verifier.verify(second, second.toWire(), false);
    assertEquals(Rcode.NOERROR, res2);
    assertNull(verifier.getErrorMessage());

    // third message with TSIG (last)
    Message third = new Message();
    TSIGRecord last = tsig.generate(third, third.toWire(), Rcode.NOERROR, null);
    third.addRecord(last, Section.ADDITIONAL);
    int res3 = verifier.verify(third, third.toWire(), true);
    assertEquals(Rcode.NOERROR, res3);
    assertNull(verifier.getErrorMessage());
  }

  @Test
  public void testStreamVerifier_missingFirstTsig_returnsFormErr() {
    Message first = new Message(); // no TSIG attached
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, null);
    int result = verifier.verify(first, first.toWire(), true);
    assertEquals(Rcode.FORMERR, result);
    assertEquals("missing required signature on first message", verifier.getErrorMessage());
  }

  @Test
  public void testStreamVerifier_missingIntermediateAfter100Messages() {
    Message first = new Message();
    TSIGRecord query = tsig.generate(first, first.toWire(), Rcode.NOERROR, null);
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, query);
    // verify first message – OK
    verifier.verify(first, first.toWire(), false);

    // generate 99 intermediate messages without TSIG
    for (int i = 0; i < 99; i++) {
      Message m = new Message();
      int r = verifier.verify(m, m.toWire(), false);
      assertEquals(Rcode.NOERROR, r);
    }

    // 100th intermediate message must contain TSIG – we omit it to trigger FORMERR
    Message hundred = new Message();
    int result = verifier.verify(hundred, hundred.toWire(), false);
    assertEquals(Rcode.FORMERR, result);
    assertTrue(verifier.getErrorMessage().contains("Missing required signature on message #101"));
  }

  /* --------------------------------------------------------------
   *  Helper methods (none needed – all logic is inline)
   * -------------------------------------------------------------- */
}
