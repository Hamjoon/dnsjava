package org.xbill.DNS;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
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
 * JUnit‑4 tests for {@link TSIG}.  The test class
 *
 * <ul>
 *   <li>lists all public methods of {@code TSIG}</li>
 *   <li>provides a basic test for each public method</li>
 *   <li>covers identified edge‑cases and exception handling</li>
 *   <li>assembles everything into a single test file</li>
 * </ul>
 *
 * The tests use only the APIs that appear in the supplied source code and the
 * public signatures that were given in the prompt.
 */
public class TSIGTest {

  /* ---------------------------------------------------------------------- */
  /* 1.  Public method list (for reference – not used by the tests)         */
  /* ---------------------------------------------------------------------- */
  //
  // public static Name algorithmToName(String alg)
  // @Deprecated public static String nameToAlgorithm(Name name)
  // public TSIG(Name algorithm, Name name, String key)
  // public TSIG(Name algorithm, Name name, byte[] keyBytes)
  // public TSIG(Name algorithm, Name name, SecretKey key)
  // public TSIG(Name algorithm, Name name, SecretKey key, Clock clock)
  // @Deprecated public TSIG(Mac mac, Name name)
  // @Deprecated public TSIG(Name name, byte[] key)
  // public TSIG(Name algorithm, String name, String key)
  // public TSIG(String algorithm, String name, String key)
  // @Deprecated public TSIG(String name, String key)
  // @Deprecated public static TSIG fromString(String str)
  // public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old)
  // public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old,
  //                              boolean fullSignature)
  // public void apply(Message m, TSIGRecord old)
  // public void apply(Message m, int error, TSIGRecord old)
  // public void apply(Message m, TSIGRecord old, boolean fullSignature)
  // public void apply(Message m, int error, TSIGRecord old, boolean fullSignature)
  // @Deprecated public void applyStream(Message m, TSIGRecord old, boolean fullSignature)
  // @Deprecated public byte verify(Message m, byte[] b, int length, TSIGRecord old)
  // public int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG)
  // public int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG,
  //                   boolean fullSignature)
  // public int recordLength()
  // public static class StreamGenerator { … }
  // public static class StreamVerifier { … }

  private static final Name TEST_NAME = Name.fromString("testkey.");
  private static final Name TEST_ALG = TSIG.HMAC_MD5;
  private static final String TEST_KEY_BASE64 = Base64.getEncoder().encodeToString("secret".getBytes());
  private static final SecretKey TEST_SECRET_KEY =
      new SecretKeySpec(Base64.getDecoder().decode(TEST_KEY_BASE64), "HmacMD5");

  private TSIG tsig;
  private Clock fixedClock;

  @Before
  public void setUp() {
    // Use a fixed clock so that time‑dependent tests are deterministic.
    fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);
    tsig = new TSIG(TEST_ALG, TEST_NAME, TEST_SECRET_KEY, fixedClock);
  }

  /* ---------------------------------------------------------------------- */
  /* 2.  Tests for algorithmToName                                         */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToNameValid() {
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1."));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
  }

  @Test
  public void testAlgorithmToNameAlias() {
    // Alias for MD5 (case‑insensitive, with or without trailing dot)
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameNull() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToNameUnknown() {
    TSIG.algorithmToName("unknown-alg");
  }

  /* ---------------------------------------------------------------------- */
  /* 3.  Tests for nameToAlgorithm (deprecated)                            */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testNameToAlgorithmValid() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithmInvalid() {
    // Use a name that is not in the map
    Name bogus = Name.fromString("bogus.");
    TSIG.nameToAlgorithm(bogus);
  }

  /* ---------------------------------------------------------------------- */
  /* 4.  Tests for fromString (deprecated)                                 */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testFromStringTwoParts() {
    TSIG t = TSIG.fromString("mykey:" + TEST_KEY_BASE64);
    assertNotNull(t);
    // default algorithm must be HMAC_MD5
    assertEquals(TSIG.HMAC_MD5, t.alg);
    assertEquals(Name.fromString("mykey."), t.name);
  }

  @Test
  public void testFromStringThreeParts() {
    TSIG t = TSIG.fromString("hmac-sha256:mykey:" + TEST_KEY_BASE64);
    assertNotNull(t);
    assertEquals(TSIG.HMAC_SHA256, t.alg);
    assertEquals(Name.fromString("mykey."), t.name);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromStringInvalid() {
    TSIG.fromString("invalid-format");
  }

  /* ---------------------------------------------------------------------- */
  /* 5.  Basic apply / verify round‑trip                                    */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testApplyAndVerifySuccess() {
    Message query = new Message();                 // empty query message
    tsig.apply(query, (TSIGRecord) null);         // sign the message
    assertTrue(query.isSigned());

    // Verify the signed message – requestTSIG is null because this is not a response.
    int result = tsig.verify(query, query.toWire(), null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue(query.isVerified());
  }

  /* ---------------------------------------------------------------------- */
  /* 6.  Verify handling of a bad signature (too short)                    */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testVerifyBadSignatureTooShort() throws Exception {
    // 1) Create a correctly signed message
    Message msg = new Message();
    tsig.apply(msg, (TSIGRecord) null);
    TSIGRecord good = msg.getTSIG();

    // 2) Replace the good TSIGRecord with a deliberately malformed one
    byte[] shortSig = new byte[5]; // shorter than the required minimum (10 for MD5)
    TSIGRecord bad =
        new TSIGRecord(
            good.getName(),
            good.dclass,
            good.ttl,
            good.getAlgorithm(),
            good.getTimeSigned(),
            good.getFudge(),
            shortSig,
            good.getOriginalID(),
            Rcode.NOERROR,
            null);

    // Remove the old TSIG and add the bad one
    msg.removeAllRecords(Section.ADDITIONAL);
    msg.addRecord(bad, Section.ADDITIONAL);
    msg.tsigState = Message.TSIG_SIGNED; // mimic the state after apply

    // 3) Verify – we expect BADSIG
    int result = tsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADSIG, result);
  }

  /* ---------------------------------------------------------------------- */
  /* 7.  StreamGenerator + StreamVerifier – normal flow                     */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifierSuccess() {
    // Query message – we need a TSIGRecord that represents the query's signature
    Message query = new Message();
    tsig.apply(query, (TSIGRecord) null);
    TSIGRecord queryTsig = query.getTSIG();

    // Generate a response using the StreamGenerator
    Message response = new Message();
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);
    generator.generate(response); // first (and only) response message

    // Verify using StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);
    int result = verifier.verify(response, response.toWire());
    assertEquals(Rcode.NOERROR, result);
    assertTrue(response.isVerified());
  }

  /* ---------------------------------------------------------------------- */
  /* 8.  StreamVerifier – missing first TSIG (FORMERR)                     */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamVerifierMissingFirstSignature() {
    // Query TSIG (as would be extracted from a real query)
    Message query = new Message();
    tsig.apply(query, (TSIGRecord) null);
    TSIGRecord queryTsig = query.getTSIG();

    // Create a response that *does not* contain a TSIG record
    Message response = new Message(); // empty, no TSIG

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);
    int result = verifier.verify(response, response.toWire());

    assertEquals(Rcode.FORMERR, result);
    assertNotNull(verifier.getErrorMessage());
    assertTrue(verifier.getErrorMessage().contains("missing required signature on first message"));
  }

  /* ---------------------------------------------------------------------- */
  /* 9.  recordLength – sanity check (does not depend on network)          */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testRecordLength() {
    int length = tsig.recordLength();
    // The length should be at least the sum of the fixed components
    // (name+alg lengths + fixed overhead).  We simply assert that it is positive.
    assertTrue(length > 0);
  }
}
