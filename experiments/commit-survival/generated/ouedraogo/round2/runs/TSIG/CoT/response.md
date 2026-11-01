###Test START##
```java
package org.xbill.DNS;

import static org.junit.Assert.*;

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
 * Comprehensive JUnit‑4 test suite for {@link TSIG}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>All public constructors and methods.
 *   <li>Normal (happy‑path) behaviour.
 *   <li>Edge‑cases and expected exceptions.
 *   <li>Interaction of {@link TSIG.StreamGenerator} and {@link TSIG.StreamVerifier}
 *       with real {@link Message} and {@link TSIGRecord} objects.
 * </ul>
 *
 * <p>Only the public API shown in the supplied source code is used – no mocking or
 * reflection of internal fields.
 */
public class TSIGTest {

  /** Fixed clock used for deterministic timestamps. */
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

  /** Base64 encoded key ("hello"). */
  private static final String KEY_B64 = Base64.getEncoder().encodeToString("hello".getBytes());

  /** Common names used in the tests. */
  private Name keyName;
  private Name algName;

  /** A TSIG instance that uses HMAC‑MD5 and the fixed clock. */
  private TSIG tsig;

  @Before
  public void setUp() throws Exception {
    keyName = Name.fromString("example.com.");
    algName = TSIG.HMAC_MD5; // default algorithm for most tests
    tsig = new TSIG(algName, keyName, KEY_B64);
  }

  /* -----------------------------------------------------------------------
   *  PUBLIC STATIC METHODS
   * ----------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_validAliases() {
    // Alias handling – upper / lower case, trailing dot
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    // Direct RFC name
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("hmac-sha1."));
    // Java algorithm name
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HmacSHA256"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_null() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_unknown() {
    TSIG.algorithmToName("non‑existent-alg");
  }

  @Test
  public void testNameToAlgorithm_known() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA384", TSIG.nameToAlgorithm(TSIG.HMAC_SHA384));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_unknown() {
    // Construct a Name that is not in the map
    Name unknown = Name.fromString("unknown.alg.");
    TSIG.nameToAlgorithm(unknown);
  }

  /* -----------------------------------------------------------------------
   *  CONSTRUCTORS
   * ----------------------------------------------------------------------- */

  @Test
  public void testConstructor_nameStringKey() {
    TSIG t = new TSIG(TSIG.HMAC_SHA1, "example.org.", KEY_B64);
    assertNotNull(t);
    // recordLength should be positive for any valid key
    assertTrue(t.recordLength() > 0);
  }

  @Test
  public void testConstructor_stringAlgorithm() {
    TSIG t = new TSIG("hmac-sha256", "example.org.", KEY_B64);
    assertNotNull(t);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_invalidKeyString() {
    // Invalid base64 causes an IllegalArgumentException in the constructor body
    new TSIG(TSIG.HMAC_MD5, "example.org.", "!!!notbase64!!!");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_invalidKeyName() {
    // TextParseException wrapped as IllegalArgumentException
    new TSIG(TSIG.HMAC_MD5, "??invalid??", KEY_B64);
  }

  @Test
  public void testDeprecatedFromString_twoParts() {
    TSIG t = TSIG.fromString("example.com.:aGVsbG8=");
    assertNotNull(t);
  }

  @Test
  public void testDeprecatedFromString_threeParts() {
    TSIG t = TSIG.fromString("hmac-sha1:example.com.:aGVsbG8=");
    assertNotNull(t);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeprecatedFromString_invalidSpec() {
    TSIG.fromString("onlyonepart");
  }

  /* -----------------------------------------------------------------------
   *  BASIC GENERATE / APPLY / VERIFY
   * ----------------------------------------------------------------------- */

  @Test
  public void testGenerateRecord_noError() throws Exception {
    Message msg = new Message(0x1234);
    byte[] wire = msg.toWire();
    TSIGRecord rec = tsig.generate(msg, wire, Rcode.NOERROR, null);
    assertNotNull(rec);
    assertEquals(0, rec.getError());
  }

  @Test
  public void testApplyAddsTsigRecord() throws Exception {
    Message msg = new Message(0x4321);
    tsig.apply(msg, null);
    TSIGRecord rec = msg.getTSIG();
    assertNotNull(rec);
    assertEquals(tsig, msg.getTSIG().getAlgorithm().equals(tsig.alg) ? tsig : null);
  }

  @Test
  public void testVerifySuccessful() throws Exception {
    Message query = new Message(0x1111);
    tsig.apply(query, null); // adds TSIG to the query
    byte[] queryWire = query.toWire();

    // Simulate a response that simply echoes the query
    Message response = new Message(0x1111);
    tsig.apply(response, query.getTSIG()); // signs with the request TSIG as "old"
    int result = tsig.verify(response, response.toWire(), query.getTSIG());
    assertEquals(Rcode.NOERROR, result);
    assertTrue(response.isVerified());
  }

  @Test
  public void testVerifyMissingTsig() throws Exception {
    Message msg = new Message(0x2222);
    int result = tsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.FORMERR, result);
    assertFalse(msg.isVerified());
  }

  @Test
  public void testVerifyBadKey() throws Exception {
    // Create a second TSIG with a different algorithm (SHA1)
    TSIG other = new TSIG(TSIG.HMAC_SHA1, keyName, KEY_B64);
    Message query = new Message(0x3333);
    tsig.apply(query, null); // signed with MD5
    Message response = new Message(0x3333);
    // Verify with a different TSIG instance – should detect BADKEY
    int result = other.verify(response, response.toWire(), query.getTSIG());
    assertEquals(Rcode.BADKEY, result);
  }

  /* -----------------------------------------------------------------------
   *  EDGE CASES FOR VERIFY SIGNATURE LENGTH
   * ----------------------------------------------------------------------- */

  @Test
  public void testVerifySignatureTooShort() throws Exception {
    // Create a TSIGRecord with a deliberately short signature
    Message msg = new Message(0x4444);
    tsig.apply(msg, null);
    TSIGRecord original = msg.getTSIG();

    // Clone and truncate the signature to 5 bytes (less than minDigestLength=10)
    byte[] shortSig = new byte[5];
    System.arraycopy(original.getSignature(), 0, shortSig, 0, 5);
    TSIGRecord tampered =
        new TSIGRecord(
            original.getName(),
            original.dclass,
            original.ttl,
            original.getAlgorithm(),
            original.getTimeSigned(),
            original.getFudge(),
            shortSig,
            original.getOriginalID(),
            original.getError(),
            original.getOther());

    Message tamperedMsg = new Message(0x4444);
    tamperedMsg.addRecord(tampered, Section.ADDITIONAL);
    int result = tsig.verify(tamperedMsg, tamperedMsg.toWire(), null);
    assertEquals(Rcode.BADSIG, result);
  }

  @Test
  public void testVerifySignatureTooLong() throws Exception {
    // Create a TSIGRecord with a signature longer than the MAC length
    Message msg = new Message(0x5555);
    tsig.apply(msg, null);
    TSIGRecord original = msg.getTSIG();

    byte[] longSig = new byte[original.getSignature().length + 10];
    System.arraycopy(original.getSignature(), 0, longSig, 0, original.getSignature().length);
    // fill the rest with zeros
    TSIGRecord tampered =
        new TSIGRecord(
            original.getName(),
            original.dclass,
            original.ttl,
            original.getAlgorithm(),
            original.getTimeSigned(),
            original.getFudge(),
            longSig,
            original.getOriginalID(),
            original.getError(),
            original.getOther());

    Message tamperedMsg = new Message(0x5555);
    tamperedMsg.addRecord(tampered, Section.ADDITIONAL);
    int result = tsig.verify(tamperedMsg, tamperedMsg.toWire(), null);
    assertEquals(Rcode.BADSIG, result);
  }

  /* -----------------------------------------------------------------------
   *  STREAM GENERATOR / VERIFIER
   * ----------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorCreatesSignedFirstMessage() throws Exception {
    Message query = new Message(0x6666);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

    Message response = new Message(0x6666);
    generator.generate(response); // first (and only) message in this test

    TSIGRecord respTsig = response.getTSIG();
    assertNotNull(respTsig);
    assertEquals(tsig.alg, respTsig.getAlgorithm());
  }

  @Test
  public void testStreamVerifierAcceptsFirstAndLastSignedMessages() throws Exception {
    // Prepare a query with a TSIG
    Message query = new Message(0x7777);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // StreamGenerator will sign the first and last messages
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

    // First response (signed)
    Message first = new Message(0x7777);
    generator.generate(first);
    // Middle response (unsigned, allowed because <100 messages)
    Message middle = new Message(0x7777);
    generator.generate(middle);
    // Last response (signed)
    Message last = new Message(0x7777);
    generator.generate(last);

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // Verify first (signed)
    int r1 = verifier.verify(first, first.toWire());
    assertEquals(Rcode.NOERROR, r1);
    // Verify middle (unsigned)
    int r2 = verifier.verify(middle, middle.toWire());
    assertEquals(Rcode.NOERROR, r2);
    // Verify last (signed, marked as last)
    int r3 = verifier.verify(last, last.toWire(), true);
    assertEquals(Rcode.NOERROR, r3);
  }

  @Test
  public void testStreamVerifierFailsWhenMissingSignatureAfter100Messages() throws Exception {
    Message query = new Message(0x8888);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig, 1);

    // Generate 101 messages, but we will remove the signature from the 101st
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // First message (signed)
    Message first = new Message(0x8888);
    generator.generate(first);
    assertEquals(Rcode.NOERROR, verifier.verify(first, first.toWire()));

    // 99 unsigned messages – should be okay
    for (int i = 0; i < 99; i++) {
      Message m = new Message(0x8888);
      generator.generate(m); // unsigned because signEveryNthMessage=1 but we are not
                             // using the generator’s internal logic; we just simulate
      assertEquals(Rcode.NOERROR, verifier.verify(m, m.toWire()));
    }

    // 101st message without a TSIG (should be FORMERR)
    Message missing = new Message(0x8888);
    int result = verifier.verify(missing, missing.toWire(), false);
    assertEquals(Rcode.FORMERR, result);
    assertEquals("Missing required signature on message #101", verifier.getErrorMessage());
  }

  /* -----------------------------------------------------------------------
   *  RECORD LENGTH CALCULATION
   * ----------------------------------------------------------------------- */

  @Test
  public void testRecordLengthIsReasonable() {
    int len = tsig.recordLength();
    // The length must be at least the sum of name+alg lengths plus overhead.
    assertTrue(len > (tsig.alg.length() + tsig.name.length()));
  }
}
```
###Test END##