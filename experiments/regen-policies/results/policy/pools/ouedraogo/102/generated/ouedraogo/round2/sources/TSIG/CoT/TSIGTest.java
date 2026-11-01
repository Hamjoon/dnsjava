/**
 * JUnit4 test cases for {@link org.xbill.DNS.TSIG}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public static utility methods (algorithmToName, nameToAlgorithm, fromString)</li>
 *   <li>Constructors and basic object creation</li>
 *   <li>TSIG record generation (generate, apply) for various error codes</li>
 *   <li>Signature verification (verify) including success, bad‑signature and bad‑time cases</li>
 *   <li>Edge cases such as null/invalid algorithm strings and unknown algorithms</li>
 *   <li>Record length calculation</li>
 *   <li>StreamGenerator and StreamVerifier behaviour</li>
 * </ul>
 *
 * The tests use only the public API that is present in the supplied source code.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;

public class TSIGTest {

  /** A simple key (base64) used for most tests – the actual value does not matter. */
  private static final String KEY_B64 = Base64.getEncoder().encodeToString("testkey123".getBytes());

  private Name keyName;
  private TSIG tsigHmacSha256;

  @Before
  public void setUp() throws Exception {
    keyName = Name.fromString("testkey.", Name.root);
    tsigHmacSha256 = new TSIG(TSIG.HMAC_SHA256, keyName, KEY_B64);
  }

  /* ---------------------------------------------------------------------- */
  /*  Public static utility methods                                         */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_JavaStyle_NoTruncation() {
    // Java style algorithm name without dash should be converted
    Name n = TSIG.algorithmToName("HmacSHA1");
    assertEquals(TSIG.HMAC_SHA1, n);
  }

  @Test
  public void testAlgorithmToName_JavaStyle_WithTruncation() {
    // Java style with truncation length
    Name n = TSIG.algorithmToName("HmacSHA256/128");
    assertEquals(TSIG.HMAC_SHA256_128, n);
  }

  @Test
  public void testAlgorithmToName_AddsTrailingDot() {
    Name n = TSIG.algorithmToName("hmac-sha384");
    assertEquals(TSIG.HMAC_SHA384, n);
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
  public void testAlgorithmToName_AliasMd5() {
    // "hmac-md5" (without trailing dot) is an alias for the RFC name
    Name n = TSIG.algorithmToName("hmac-md5");
    assertEquals(TSIG.HMAC_MD5, n);
  }

  @Test
  public void testNameToAlgorithm_KnownAlgorithm() {
    String alg = TSIG.nameToAlgorithm(TSIG.HMAC_SHA224);
    assertEquals("HmacSHA224", alg);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_UnknownAlgorithm() {
    // Construct a Name that is not in the map
    Name unknown = Name.fromString("hmac-foo.", Name.root);
    TSIG.nameToAlgorithm(unknown);
  }

  @Test
  public void testFromString_TwoParts_DefaultAlgorithm() {
    TSIG tsig = TSIG.fromString("testkey:" + KEY_B64);
    // default algorithm is HMAC_MD5
    assertEquals(TSIG.HMAC_MD5, tsig.alg);
    assertEquals(keyName, tsig.name);
  }

  @Test
  public void testFromString_ThreeParts_ExplicitAlgorithm() {
    TSIG tsig = TSIG.fromString("hmac-sha256:testkey:" + KEY_B64);
    assertEquals(TSIG.HMAC_SHA256, tsig.alg);
    assertEquals(keyName, tsig.name);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromString_InvalidSpec() {
    TSIG.fromString("invalidspec");
  }

  /* ---------------------------------------------------------------------- */
  /*  Record length calculation                                               */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testRecordLengthMatchesFormula() {
    // Verify that the method returns the expected value based on the algorithm length map.
    int expected = tsigHmacSha256.name.length()
        + 10
        + tsigHmacSha256.alg.length()
        + 8 // time signed + fudge
        + 2 // MAC length field
        + TSIG.algLengthMap.get(tsigHmacSha256.alg)
        + 4 // original id + error
        + 8; // other data length field + max other length
    assertEquals(expected, tsigHmacSha256.recordLength());
  }

  /* ---------------------------------------------------------------------- */
  /*  Basic generate / apply workflow                                        */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testGenerate_NoError_FullSignature() throws Exception {
    Message m = new Message();
    // Use a simple query to have a non‑empty wire representation
    m.addRecord(new TXTRecord(Name.fromString("example.com.", Name.root),
        DClass.IN, 0, "test"), Section.QUESTION);

    TSIGRecord tsigRec = tsigHmacSha256.generate(m, m.toWire(), Rcode.NOERROR, null, true);
    assertNotNull(tsigRec);
    assertEquals(tsigHmacSha256.name, tsigRec.getName());
    assertEquals(tsigHmacSha256.alg, tsigRec.getAlgorithm());
    // Signature length must not exceed the algorithm‑specific length
    assertTrue(tsigRec.getSignature().length <= TSIG.algLengthMap.get(tsigHmacSha256.alg));
  }

  @Test
  public void testGenerate_BadTime_IncludesOtherField() throws Exception {
    Message m = new Message();
    TSIGRecord tsigRec = tsigHmacSha256.generate(m, m.toWire(), Rcode.BADTIME, null, true);
    assertNotNull(tsigRec.getOther());
    // The "other" field for BADTIME must be exactly 6 bytes (time+fudge)
    assertEquals(6, tsigRec.getOther().length);
  }

  @Test
  public void testApplyAddsTSIGRecordAndSetsState() throws Exception {
    Message m = new Message();
    tsigHmacSha256.apply(m, null);
    TSIGRecord rec = m.getTSIG();
    assertNotNull(rec);
    assertEquals(Message.TSIG_SIGNED, m.tsigState);
  }

  /* ---------------------------------------------------------------------- */
  /*  Verification – success, bad‑signature, bad‑time                        */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testVerify_Successful() throws Exception {
    Message m = new Message();
    // sign the message
    tsigHmacSha256.apply(m, null);
    byte[] wire = m.toWire();
    // verify using the same TSIG key
    int result = tsigHmacSha256.verify(m, wire, null);
    assertEquals(Rcode.NOERROR, result);
    assertEquals(Message.TSIG_VERIFIED, m.tsigState);
  }

  @Test
  public void testVerify_BadSignature() throws Exception {
    Message m = new Message();
    tsigHmacSha256.apply(m, null);
    // corrupt the signature
    TSIGRecord tsig = m.getTSIG();
    byte[] badSig = tsig.getSignature().clone();
    badSig[0] ^= 0xFF; // flip a bit
    TSIGRecord corrupted = new TSIGRecord(
        tsig.getName(),
        tsig.dclass,
        tsig.ttl,
        tsig.getAlgorithm(),
        tsig.getTimeSigned(),
        tsig.getFudge(),
        badSig,
        tsig.getOriginalID(),
        tsig.getError(),
        tsig.getOther()
    );
    // replace the record in the message
    m.removeAllRecords(Section.ADDITIONAL);
    m.addRecord(corrupted, Section.ADDITIONAL);

    int result = tsigHmacSha256.verify(m, m.toWire(), null);
    assertEquals(Rcode.BADSIG, result);
    assertEquals(Message.TSIG_FAILED, m.tsigState);
  }

  @Test
  public void testVerify_BadTime() throws Exception {
    // Use a clock that is far in the past to force a BADTIME error
    Clock farPast = Clock.fixed(Instant.EPOCH, Clock.systemUTC().getZone());
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, keyName, KEY_B64, farPast);

    Message m = new Message();
    tsig.apply(m, null);
    int result = tsig.verify(m, m.toWire(), null);
    assertEquals(Rcode.BADTIME, result);
    assertEquals(Message.TSIG_FAILED, m.tsigState);
  }

  /* ---------------------------------------------------------------------- */
  /*  StreamGenerator – multiple message signing                              */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamGenerator_SignsMultipleMessages() throws Exception {
    // Create a query TSIG (normally from a client request)
    Message query = new Message();
    TSIGRecord queryTsig = tsigHmacSha256.generate(query, query.toWire(), Rcode.NOERROR, null, true);
    query.addRecord(queryTsig, Section.ADDITIONAL);

    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigHmacSha256, queryTsig);

    // Generate three sequential response messages
    Message resp1 = new Message();
    generator.generate(resp1);
    assertNotNull(resp1.getTSIG());
    assertEquals(Message.TSIG_SIGNED, resp1.tsigState);

    Message resp2 = new Message();
    generator.generate(resp2);
    // second message also gets a TSIG (signEveryNthMessage defaults to 1)
    assertNotNull(resp2.getTSIG());

    Message resp3 = new Message();
    generator.generate(resp3);
    assertNotNull(resp3.getTSIG());
  }

  /* ---------------------------------------------------------------------- */
  /*  StreamVerifier – handling of missing signatures                         */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamVerifier_ValidSequence() throws Exception {
    // Prepare a query with a TSIG
    Message query = new Message();
    TSIGRecord queryTsig = tsigHmacSha256.generate(query, query.toWire(), Rcode.NOERROR, null, true);
    query.addRecord(queryTsig, Section.ADDITIONAL);

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigHmacSha256, queryTsig);

    // First response – has TSIG
    Message resp1 = new Message();
    TSIGRecord r1 = tsigHmacSha256.generate(resp1, resp1.toWire(), Rcode.NOERROR, queryTsig, true);
    resp1.addRecord(r1, Section.ADDITIONAL);
    int rc1 = verifier.verify(resp1, resp1.toWire());
    assertEquals(Rcode.NOERROR, rc1);
    assertEquals(Message.TSIG_VERIFIED, resp1.tsigState);

    // Second response – no TSIG, should be accepted as intermediate
    Message resp2 = new Message();
    int rc2 = verifier.verify(resp2, resp2.toWire());
    assertEquals(Rcode.NOERROR, rc2);
    assertEquals(Message.TSIG_INTERMEDIATE, resp2.tsigState);

    // Third (last) response – must contain TSIG
    Message resp3 = new Message();
    TSIGRecord r3 = tsigHmacSha256.generate(resp3, resp3.toWire(), Rcode.NOERROR, null, true);
    resp3.addRecord(r3, Section.ADDITIONAL);
    int rc3 = verifier.verify(resp3, resp3.toWire(), true);
    assertEquals(Rcode.NOERROR, rc3);
    assertEquals(Message.TSIG_VERIFIED, resp3.tsigState);
  }

  @Test
  public void testStreamVerifier_MissingSignatureOnFirstMessage() throws Exception {
    Message query = new Message();
    TSIGRecord queryTsig = tsigHmacSha256.generate(query, query.toWire(), Rcode.NOERROR, null, true);
    query.addRecord(queryTsig, Section.ADDITIONAL);

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigHmacSha256, queryTsig);

    // First response without TSIG – should produce FORMERR
    Message resp1 = new Message();
    int rc = verifier.verify(resp1, resp1.toWire());
    assertEquals(Rcode.FORMERR, rc);
    assertEquals("missing required signature on first message", verifier.getErrorMessage());
  }

  @Test
  public void testStreamVerifier_MissingSignatureAfter100Messages() throws Exception {
    Message query = new Message();
    TSIGRecord queryTsig = tsigHmacSha256.generate(query, query.toWire(), Rcode.NOERROR, null, true);
    query.addRecord(queryTsig, Section.ADDITIONAL);

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigHmacSha256, queryTsig);

    // Simulate 101 messages where the 101st is missing a TSIG
    for (int i = 1; i <= 100; i++) {
      Message m = new Message();
      int rc = verifier.verify(m, m.toWire());
      assertEquals(Rcode.NOERROR, rc);
    }
    // 101st message – missing TSIG, should trigger FORMERR
    Message bad = new Message();
    int rcBad = verifier.verify(bad, bad.toWire());
    assertEquals(Rcode.FORMERR, rcBad);
    assertTrue(verifier.getErrorMessage().contains("Missing required signature on message #101"));
  }

  /* ---------------------------------------------------------------------- */
  /*  Helper constructor used in a test (exposes the private fields)        */
  /* ---------------------------------------------------------------------- */

  // Expose the private fields for assertions in some tests
  private static class TSIG extends org.xbill.DNS.TSIG {
    public final Name alg;
    public final Name name;
    public final Clock clock;

    public TSIG(Name algorithm, Name name, String key) {
      super(algorithm, name, key);
      this.alg = algorithm;
      this.name = name;
      this.clock = Clock.systemUTC();
    }

    public TSIG(Name algorithm, Name name, byte[] keyBytes) {
      super(algorithm, name, keyBytes);
      this.alg = algorithm;
      this.name = name;
      this.clock = Clock.systemUTC();
    }

    public TSIG(Name algorithm, Name name, SecretKey key) {
      super(algorithm, name, key);
      this.alg = algorithm;
      this.name = name;
      this.clock = Clock.systemUTC();
    }

    public TSIG(Name algorithm, Name name, SecretKey key, Clock clock) {
      super(algorithm, name, key, clock);
      this.alg = algorithm;
      this.name = name;
      this.clock = clock;
    }
  }
}
