###Test START##
```java
/*
 * TSIGTest.java
 *
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.TSIG.
 *
 * The tests cover:
 *   • All public static methods (algorithmToName, nameToAlgorithm, fromString)
 *   • All public constructors
 *   • Core instance methods (generate, apply, verify, recordLength)
 *   • Edge‑cases and exception handling (null/invalid arguments, unknown algorithms,
 *     malformed key strings, signature length mismatches, tampered messages)
 *
 * The test suite builds real DNS objects (Name, Message, TXTRecord, TSIGRecord)
 * using only the APIs that are part of the supplied source code and its public
 * dependencies – no mocking or external libraries are used.
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 tests for {@link TSIG}.
 */
public class TSIGTest {

  private static final Name KEY_NAME = Name.fromString("testkey.");
  private static final String SECRET = "secret";
  private static final String SECRET_B64 = base64.toString(SECRET.getBytes());
  private static final Name DOMAIN = Name.fromString("example.com.");

  private TSIG tsigMd5;      // HMAC‑MD5 key
  private TSIG tsigSha256;   // HMAC‑SHA256 key
  private Message query;     // a simple DNS query (TXT record)

  @Before
  public void setUp() throws Exception {
    // Create two TSIG objects with different algorithms
    tsigMd5 = new TSIG(TSIG.HMAC_MD5, KEY_NAME, SECRET_B64);
    tsigSha256 = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, SECRET_B64);

    // Build a minimal query message (TXT record)
    TXTRecord txt = new TXTRecord(DOMAIN, DClass.IN, 300, "test");
    query = Message.newQuery(txt);
  }

  /* -----------------------------------------------------------------------
   *  Public static method tests
   * ----------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_Alias() {
    // Alias “HMAC‑MD5” (with and without trailing dot) must map to HMAC_MD5 constant
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5."));
  }

  @Test
  public void testAlgorithmToName_CaseInsensitive() {
    // Verify case‑insensitive handling for a SHA‑256 algorithm
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HMAC-SHA256"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Null() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Unknown() {
    TSIG.algorithmToName("unknown-alg");
  }

  @Test
  public void testNameToAlgorithm_Known() {
    // Known mapping – HMAC_MD5 -> "HmacMD5"
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_Unknown() {
    // Construct a Name that is not in the map
    Name bogus = Name.fromString("bogus-alg.");
    TSIG.nameToAlgorithm(bogus);
  }

  @Test
  public void testFromString_TwoParts_DefaultMd5() {
    // “name:secret” – default algorithm is HMAC_MD5
    String spec = "testkey:" + SECRET_B64;
    TSIG t = TSIG.fromString(spec);
    assertEquals(TSIG.HMAC_MD5, t.alg);
    assertEquals(KEY_NAME, t.name);
  }

  @Test
  public void testFromString_ThreeParts_Sha256() {
    // “alg:name:secret” – explicit algorithm
    String spec = "hmac-sha256:testkey:" + SECRET_B64;
    TSIG t = TSIG.fromString(spec);
    assertEquals(TSIG.HMAC_SHA256, t.alg);
    assertEquals(KEY_NAME, t.name);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromString_InvalidParts() {
    // Too few or too many separators should raise an exception
    TSIG.fromString("onlyonepart");
  }

  /* -----------------------------------------------------------------------
   *  Constructor edge‑case tests
   * ----------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidBase64() {
    // The base64 string is deliberately malformed
    new TSIG(TSIG.HMAC_MD5, KEY_NAME, "!!! not base64 !!!");
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_NullKeyString() {
    // The public constructor that takes a String key checks for null
    new TSIG(TSIG.HMAC_MD5, KEY_NAME, (String) null);
  }

  @Test
  public void testRecordLength_Positive() {
    // recordLength should always be a positive integer
    int len = tsigMd5.recordLength();
    assertTrue("recordLength must be > 0", len > 0);
  }

  /* -----------------------------------------------------------------------
   *  Instance method tests – generate / apply / verify
   * ----------------------------------------------------------------------- */

  @Test
  public void testGenerate_ReturnsTSIGRecord() throws Exception {
    byte[] wire = query.toWire();
    TSIGRecord rec = tsigMd5.generate(query, wire, Rcode.NOERROR, null);
    assertNotNull(rec);
    assertEquals(KEY_NAME, rec.getName());
    assertEquals(TSIG.HMAC_MD5, rec.getAlgorithm());
  }

  @Test
  public void testApplyAndVerify_Success() throws Exception {
    // Apply a TSIG to the query, then verify the same message.
    tsigMd5.apply(query, (TSIGRecord) null);
    byte[] signedWire = query.toWire();

    int result = tsigMd5.verify(query, signedWire, null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue("Message should be marked verified", query.isVerified());
  }

  @Test
  public void testApplyAndVerify_TamperedSignature() throws Exception {
    // Apply TSIG, then corrupt the wire data – verification must fail with BADSIG.
    tsigMd5.apply(query, (TSIGRecord) null);
    byte[] signedWire = query.toWire();

    // Flip the last byte (most likely part of the MAC)
    signedWire[signedWire.length - 1] ^= 0xFF;

    int result = tsigMd5.verify(query, signedWire, null);
    assertEquals(Rcode.BADSIG, result);
    assertFalse("Message should not be verified after tampering", query.isVerified());
  }

  @Test
  public void testVerify_MissingTSIGRecord() throws Exception {
    // A message without a TSIG record must return FORMERR.
    byte[] wire = query.toWire(); // no TSIG applied
    int result = tsigMd5.verify(query, wire, null);
    assertEquals(Rcode.FORMERR, result);
  }

  @Test
  public void testVerify_WrongKey() throws Exception {
    // Sign with one key, verify with a different key – should return BADKEY.
    tsigMd5.apply(query, (TSIGRecord) null);
    byte[] signedWire = query.toWire();

    int result = tsigSha256.verify(query, signedWire, null);
    assertEquals(Rcode.BADKEY, result);
  }

  /* -----------------------------------------------------------------------
   *  Edge‑case tests for internal helper methods (indirectly exercised)
   * ----------------------------------------------------------------------- */

  @Test
  public void testVerifySignature_TooShort() throws Exception {
    // Build a TSIGRecord with an intentionally short signature.
    // The verify method will be exercised via the public verify().
    byte[] shortSig = new byte[5]; // shorter than minDigestLength (10)
    TSIGRecord badSigRec = new TSIGRecord(
        KEY_NAME,
        DClass.ANY,
        0,
        TSIG.HMAC_MD5,
        Instant.now(),
        Duration.ofSeconds(300),
        shortSig,
        query.getHeader().getID(),
        Rcode.NOERROR,
        null);

    query.addRecord(badSigRec, Section.ADDITIONAL);
    byte[] wire = query.toWire();

    int result = tsigMd5.verify(query, wire, null);
    assertEquals(Rcode.BADSIG, result);
  }

  @Test
  public void testVerifyTime_OutsideFudge() throws Exception {
    // Create a TSIGRecord whose timeSigned is far in the past.
    Instant farPast = Instant.now().minus(Duration.ofHours(5));
    TSIGRecord oldRec = new TSIGRecord(
        KEY_NAME,
        DClass.ANY,
        0,
        TSIG.HMAC_MD5,
        farPast,
        Duration.ofSeconds(1), // very small fudge
        new byte[0],
        query.getHeader().getID(),
        Rcode.NOERROR,
        null);

    query.addRecord(oldRec, Section.ADDITIONAL);
    byte[] wire = query.toWire();

    int result = tsigMd5.verify(query, wire, null);
    assertEquals(Rcode.BADTIME, result);
  }

  /* -----------------------------------------------------------------------
   *  StreamGenerator / StreamVerifier integration tests
   * ----------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifier_SimpleRoundTrip() throws Exception {
    // Simulate a query‑response stream with a single message.
    TSIGRecord queryTsig = tsigMd5.generate(query, query.toWire(), Rcode.NOERROR, null);
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigMd5, queryTsig);

    // Create a response message (reuse the same query for simplicity)
    Message response = new Message(query.getHeader().getID());
    generator.generate(response); // signs the response

    // Verify the stream
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);
    int result = verifier.verify(response, response.toWire());
    assertEquals(Rcode.NOERROR, result);
    assertTrue(response.isVerified());
  }

  @Test
  public void testStreamVerifier_MissingIntermediateSignature() throws Exception {
    // Build a stream of three messages where the middle one lacks a TSIG.
    TSIGRecord queryTsig = tsigMd5.generate(query, query.toWire(), Rcode.NOERROR, null);
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigMd5, queryTsig, 2); // sign every 2nd

    // Message 1 (signed)
    Message msg1 = new Message(1);
    generator.generate(msg1, false);

    // Message 2 (unsigned)
    Message msg2 = new Message(2);
    generator.generate(msg2, false); // will be unsigned because signEveryNthMessage=2

    // Message 3 (signed, last)
    Message msg3 = new Message(3);
    generator.generate(msg3, true);

    // Verify the stream
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);
    assertEquals(Rcode.NOERROR, verifier.verify(msg1, msg1.toWire()));
    assertEquals(Rcode.NOERROR, verifier.verify(msg2, msg2.toWire()));
    assertEquals(Rcode.NOERROR, verifier.verify(msg3, msg3.toWire()));
  }

  @Test
  public void testStreamVerifier_MissingRequiredSignature() throws Exception {
    // Build a stream where the 101st message lacks a TSIG – verifier must return FORMERR.
    TSIGRecord queryTsig = tsigMd5.generate(query, query.toWire(), Rcode.NOERROR, null);
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigMd5, queryTsig, 100);

    // Generate 101 messages; the 101st will be unsigned (signEveryNthMessage=100)
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryTsig);
    for (int i = 1; i <= 101; i++) {
      Message m = new Message(i);
      boolean isLast = (i == 101);
      generator.generate(m, isLast);
      int rc = verifier.verify(m, m.toWire(), isLast);
      if (i == 101) {
        assertEquals(Rcode.FORMERR, rc);
      } else {
        assertEquals(Rcode.NOERROR, rc);
      }
    }
  }
}
```
###Test END##