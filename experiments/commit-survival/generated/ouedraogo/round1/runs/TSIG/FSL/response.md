###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

/**
 * Unit tests for {@link TSIG}.  The tests cover:
 *
 * <ul>
 *   <li>algorithm name conversion (algorithmToName, nameToAlgorithm)</li>
 *   <li>parsing of legacy TSIG strings (fromString)</li>
 *   <li>record length calculation</li>
 *   <li>generation and application of TSIG records</li>
 *   <li>verification of valid and invalid signatures</li>
 *   <li>time‑fudge handling</li>
 *   <li>stream generation / verification utilities</li>
 * </ul>
 *
 * All objects are created using the public API – no mocking is performed.
 */
public class TSIGTest {

  private static final Name KEY_NAME = Name.fromString("tsig-key.example.");
  private static final String BASE64_SECRET = Base64.getEncoder().encodeToString("secret".getBytes());
  private static final byte[] SECRET_BYTES = base64.fromString(BASE64_SECRET);
  private static final SecretKey SECRET_KEY = new SecretKeySpec(SECRET_BYTES, "HmacMD5");

  /** Helper to create a TSIG instance with a deterministic clock. */
  private static TSIG createTsig(Name algorithm, Clock clock) {
    return new TSIG(algorithm, KEY_NAME, SECRET_KEY, clock);
  }

  /** Helper to create a minimal Message with a known ID. */
  private static Message newMessage() {
    Message m = new Message();
    m.getHeader().setID(0x1234);
    return m;
  }

  /* -----------------------------------------------------------------------
   *  algorithmToName / nameToAlgorithm
   * ----------------------------------------------------------------------- */
  @Test
  public void testAlgorithmToNameValid() {
    // canonical name
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HmacMD5"));
    // case‑insensitive, with trailing dot
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));
    // alias for MD5
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
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
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithmUnknown() {
    TSIG.nameToAlgorithm(Name.fromString("nonexistent."));
  }

  /* -----------------------------------------------------------------------
   *  fromString parsing
   * ----------------------------------------------------------------------- */
  @Test
  public void testFromStringTwoParts() {
    TSIG tsig = TSIG.fromString("mykey:" + BASE64_SECRET);
    assertNotNull(tsig);
    // default algorithm should be MD5
    assertEquals(TSIG.HMAC_MD5, tsig.alg);
  }

  @Test
  public void testFromStringThreeParts() {
    TSIG tsig = TSIG.fromString("hmac-sha256:mykey:" + BASE64_SECRET);
    assertNotNull(tsig);
    assertEquals(TSIG.HMAC_SHA256, tsig.alg);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromStringInvalid() {
    TSIG.fromString("invalid");
  }

  /* -----------------------------------------------------------------------
   *  recordLength()
   * ----------------------------------------------------------------------- */
  @Test
  public void testRecordLength() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA1, KEY_NAME, SECRET_KEY);
    int len = tsig.recordLength();
    // The exact value is not critical – just verify it is positive and matches the formula.
    assertTrue(len > 0);
    // Basic sanity check against the documented formula
    int expected = KEY_NAME.length() + 10 + TSIG.HMAC_SHA1.length() + 8 + 18 + 4 + 8;
    assertEquals(expected, len);
  }

  /* -----------------------------------------------------------------------
   *  generate / apply
   * ----------------------------------------------------------------------- */
  @Test
  public void testGenerateAndApply() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, SECRET_KEY);
    Message msg = newMessage();

    // generate a TSIGRecord directly
    TSIGRecord rec = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    assertNotNull(rec);
    assertArrayEquals(SECRET_BYTES, tsig.macKey.getEncoded());

    // apply should attach the record and set the state
    tsig.apply(msg, null);
    TSIGRecord attached = msg.getTSIG();
    assertNotNull(attached);
    assertEquals(Message.TSIG_SIGNED, msg.tsigState);
  }

  /* -----------------------------------------------------------------------
   *  verify – happy path
   * ----------------------------------------------------------------------- */
  @Test
  public void testVerifyValidSignature() {
    TSIG tsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, SECRET_KEY);
    Message msg = newMessage();

    // sign the message
    tsig.apply(msg, null);
    TSIGRecord tsigRec = msg.getTSIG();
    assertNotNull(tsigRec);
    byte[] wire = msg.toWire();

    // verification should succeed
    int result = tsig.verify(msg, wire, tsigRec);
    assertEquals(Rcode.NOERROR, result);
    assertEquals(Message.TSIG_VERIFIED, msg.tsigState);
  }

  /* -----------------------------------------------------------------------
   *  verify – bad signature (too short)
   * ----------------------------------------------------------------------- */
  @Test
  public void testVerifySignatureTooShort() {
    TSIG tsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, SECRET_KEY);
    Message msg = newMessage();

    // create a TSIGRecord with a deliberately short signature
    byte[] shortSig = new byte[5]; // shorter than minDigestLength (10)
    TSIGRecord badRec =
        new TSIGRecord(
            KEY_NAME,
            DClass.ANY,
            0,
            TSIG.HMAC_MD5,
            Instant.now(),
            Duration.ofSeconds(300),
            shortSig,
            msg.getHeader().getID(),
            Rcode.NOERROR,
            null);
    msg.addRecord(badRec, Section.ADDITIONAL);
    msg.tsigState = Message.TSIG_SIGNED; // mimic a signed state

    int result = tsig.verify(msg, msg.toWire(), badRec);
    assertEquals(Rcode.BADSIG, result);
    assertEquals(Message.TSIG_FAILED, msg.tsigState);
  }

  /* -----------------------------------------------------------------------
   *  verify – bad signature (too long)
   * ----------------------------------------------------------------------- */
  @Test
  public void testVerifySignatureTooLong() {
    TSIG tsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, SECRET_KEY);
    Message msg = newMessage();

    // create a TSIGRecord with a signature longer than the MAC length (MD5 = 16)
    byte[] longSig = new byte[20];
    new SecureRandom().nextBytes(longSig);
    TSIGRecord badRec =
        new TSIGRecord(
            KEY_NAME,
            DClass.ANY,
            0,
            TSIG.HMAC_MD5,
            Instant.now(),
            Duration.ofSeconds(300),
            longSig,
            msg.getHeader().getID(),
            Rcode.NOERROR,
            null);
    msg.addRecord(badRec, Section.ADDITIONAL);
    msg.tsigState = Message.TSIG_SIGNED;

    int result = tsig.verify(msg, msg.toWire(), badRec);
    assertEquals(Rcode.BADSIG, result);
    assertEquals(Message.TSIG_FAILED, msg.tsigState);
  }

  /* -----------------------------------------------------------------------
   *  verify – bad time (fudge)
   * ----------------------------------------------------------------------- */
  @Test
  public void testVerifyBadTime() {
    // Fixed clock at epoch 0
    Clock fixedClock = Clock.fixed(Instant.EPOCH, java.time.ZoneOffset.UTC);
    TSIG tsig = createTsig(TSIG.HMAC_MD5, fixedClock);
    Message msg = newMessage();

    // Sign the message with the fixed clock (timeSigned = epoch)
    tsig.apply(msg, null);
    TSIGRecord rec = msg.getTSIG();

    // Verify with a clock that is far in the future (simulate delay)
    TSIG verifier = new TSIG(TSIG.HMAC_MD5, KEY_NAME, SECRET_KEY, Clock.systemUTC());

    // The fudge is the default 300 seconds, the time delta will be huge → BADTIME
    int result = verifier.verify(msg, msg.toWire(), rec);
    assertEquals(Rcode.BADTIME, result);
    assertEquals(Message.TSIG_FAILED, msg.tsigState);
  }

  /* -----------------------------------------------------------------------
   *  StreamGenerator – signing multiple messages
   * ----------------------------------------------------------------------- */
  @Test
  public void testStreamGeneratorMultipleMessages() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA1, KEY_NAME, SECRET_KEY);
    Message query = newMessage();
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsig, queryTsig);

    // First response – should contain a TSIG
    Message resp1 = newMessage();
    gen.generate(resp1);
    assertNotNull(resp1.getTSIG());
    assertEquals(Message.TSIG_SIGNED, resp1.tsigState);

    // Second response – also signed because signEveryNthMessage defaults to 1
    Message resp2 = newMessage();
    gen.generate(resp2);
    assertNotNull(resp2.getTSIG());
    assertEquals(Message.TSIG_SIGNED, resp2.tsigState);
  }

  /* -----------------------------------------------------------------------
   *  StreamVerifier – happy path and missing signature detection
   * ----------------------------------------------------------------------- */
  @Test
  public void testStreamVerifierSuccessAndFailure() {
    // Prepare a query with a TSIG
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, SECRET_KEY);
    Message query = newMessage();
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Build a stream verifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // First response – must have TSIG
    Message resp1 = newMessage();
    tsig.apply(resp1, null);
    int r1 = verifier.verify(resp1, resp1.toWire());
    assertEquals(Rcode.NOERROR, r1);
    assertEquals(Message.TSIG_VERIFIED, resp1.tsigState);

    // Second response – intentionally omit TSIG, but it's allowed because fewer than 100 msgs
    Message resp2 = newMessage();
    int r2 = verifier.verify(resp2, resp2.toWire());
    assertEquals(Rcode.NOERROR, r2);
    assertEquals(Message.TSIG_INTERMEDIATE, resp2.tsigState);

    // Simulate 100 unsigned messages and then a required one without TSIG
    TSIG.StreamVerifier verifier2 = new TSIG.StreamVerifier(tsig, queryTsig);
    // generate 99 unsigned messages
    for (int i = 0; i < 99; i++) {
      Message m = newMessage();
      int rc = verifier2.verify(m, m.toWire());
      assertEquals(Rcode.NOERROR, rc);
    }
    // 101st message must have TSIG, we give it without – expect FORMERR
    Message missingSig = newMessage();
    int rc = verifier2.verify(missingSig, missingSig.toWire());
    assertEquals(Rcode.FORMERR, rc);
    assertEquals(Message.TSIG_FAILED, missingSig.tsigState);
    assertNotNull(verifier2.getErrorMessage());
  }
}
###Test END##