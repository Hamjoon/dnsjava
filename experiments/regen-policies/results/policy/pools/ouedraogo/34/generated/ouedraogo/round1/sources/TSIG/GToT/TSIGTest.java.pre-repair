/*
 * Comprehensive JUnit‑4 test suite for org.xbill.DNS.TSIG.
 *
 * The tests cover:
 *   • Public static helpers (algorithmToName, nameToAlgorithm)
 *   • All public constructors
 *   • Normal signing / applying of a TSIG record
 *   • Verification (including BADKEY, BADSIG, BADTIME scenarios)
 *   • Edge‑case / exception handling
 *   • recordLength() sanity check
 *   • StreamGenerator and StreamVerifier behaviour
 *
 * The tests use only the public API that is present in the supplied source
 * and the minimal helper classes (Name, Message, TSIGRecord, etc.).
 *
 * JUnit‑4 is used (annotations @Test, @Before, etc.).
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link TSIG}.
 */
public class TSIGTest {

  /** A simple HMAC‑MD5 key (base64 of the string "secret"). */
  private static final String KEY_B64 = "c2VjcmV0"; // "secret"
  /** A Name used as the TSIG key name. */
  private static final Name KEY_NAME = Name.fromString("tsig-key.example.");

  /** TSIG instance used by the majority of tests. */
  private TSIG tsig;

  /** A fresh Message for each test. */
  private Message msg;

  @Before
  public void setUp() throws Exception {
    // Use the explicit algorithm‑name constructor (most common usage).
    tsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, KEY_B64);
    msg = new Message(); // empty message – sufficient for TSIG signing.
  }

  /* ---------------------------------------------------------------------- */
  /* 1.  Static helper methods                                            */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_knownAlgorithm() {
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
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
  public void testNameToAlgorithm_knownName() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_unknownName() {
    // Create a dummy Name that is not in the map.
    Name bogus = Name.fromString("bogus.example.");
    TSIG.nameToAlgorithm(bogus);
  }

  /* ---------------------------------------------------------------------- */
  /* 2.  Constructors                                                      */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testConstructor_NameStringString() {
    TSIG t = new TSIG(TSIG.HMAC_SHA1, "mykey.example.", KEY_B64);
    assertNotNull(t);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_invalidBase64() {
    new TSIG(TSIG.HMAC_MD5, "mykey.example.", "!!! not base64 !!!");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_invalidName() {
    new TSIG(TSIG.HMAC_MD5, "invalid name", KEY_B64);
  }

  @Test
  public void testDeprecatedFromString_twoParts() {
    TSIG t = TSIG.fromString("mykey.example.:c2VjcmV0");
    assertNotNull(t);
  }

  @Test
  public void testDeprecatedFromString_threeParts() {
    TSIG t = TSIG.fromString("hmac-sha256:mykey.example.:c2VjcmV0");
    assertEquals(TSIG.HMAC_SHA256, t.alg);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeprecatedFromString_invalidParts() {
    TSIG.fromString("onlyonepart");
  }

  /* ---------------------------------------------------------------------- */
  /* 3.  Basic signing / applying                                          */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testApplyAndVerify_success() throws Exception {
    // Apply a TSIG to the message.
    tsig.apply(msg, (TSIGRecord) null);
    assertTrue(msg.isSigned());

    // Verify the signed message.
    int rcode = tsig.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.NOERROR, rcode);
    assertTrue(msg.isVerified());
  }

  @Test
  public void testApplyWithOldRecord() throws Exception {
    // First message – generate a TSIG record.
    TSIGRecord first = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    msg.addRecord(first, Section.ADDITIONAL);
    msg.tsigState = Message.TSIG_SIGNED;

    // Second message – apply using the previous TSIG as “old”.
    Message second = new Message();
    tsig.apply(second, first);
    assertTrue(second.isSigned());

    // Verify the second message using the first as the request TSIG.
    int rcode = tsig.verify(second, second.toWire(), first);
    assertEquals(Rcode.NOERROR, rcode);
    assertTrue(second.isVerified());
  }

  /* ---------------------------------------------------------------------- */
  /* 4.  Edge‑case verification                                            */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testVerify_badKey() throws Exception {
    // Create a second TSIG with a different key name.
    TSIG other = new TSIG(TSIG.HMAC_MD5, Name.fromString("other.example."), KEY_B64);
    tsig.apply(msg, (TSIGRecord) null);
    int rcode = other.verify(msg, msg.toWire(), null);
    assertEquals(Rcode.BADKEY, rcode);
  }

  @Test
  public void testVerify_badTime() throws Exception {
    // Manipulate the fudge to be very small and set a far‑future time.
    TSIGRecord record = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    // Create a TSIG with a clock that is far in the future.
    Clock farFuture = Clock.offset(Clock.systemUTC(), Duration.ofDays(10));
    TSIG futureTsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, base64.fromString(KEY_B64), farFuture);
    Message futureMsg = new Message();
    futureMsg.addRecord(record, Section.ADDITIONAL);
    futureMsg.tsigState = Message.TSIG_SIGNED;

    int rcode = futureTsig.verify(futureMsg, futureMsg.toWire(), null);
    assertEquals(Rcode.BADTIME, rcode);
  }

  @Test
  public void testVerify_badSignature() throws Exception {
    tsig.apply(msg, (TSIGRecord) null);
    // Corrupt the signature in the TSIG record.
    TSIGRecord tsigRec = msg.getTSIG();
    byte[] badSig = tsigRec.getSignature().clone();
    badSig[0] ^= 0xFF; // flip a bit
    TSIGRecord corrupt = new TSIGRecord(
        tsigRec.getName(),
        tsigRec.dclass,
        tsigRec.ttl,
        tsigRec.getAlgorithm(),
        tsigRec.getTimeSigned(),
        tsigRec.getFudge(),
        badSig,
        tsigRec.getOriginalID(),
        tsigRec.getError(),
        tsigRec.getOther());

    Message corruptMsg = new Message();
    corruptMsg.addRecord(corrupt, Section.ADDITIONAL);
    corruptMsg.tsigState = Message.TSIG_SIGNED;

    int rcode = tsig.verify(corruptMsg, corruptMsg.toWire(), null);
    assertEquals(Rcode.BADSIG, rcode);
  }

  /* ---------------------------------------------------------------------- */
  /* 5.  recordLength sanity check                                         */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testRecordLength() {
    int len = tsig.recordLength();
    // Length must be at least the sum of the fixed fields (see Javadoc).
    assertTrue(len > 0);
    // For MD5 the calculated length is deterministic.
    // name length + 10 + alg length + 8 + 18 + 4 + 8
    int expected = tsig.name.length() + 10 + tsig.alg.length() + 8 + 18 + 4 + 8;
    assertEquals(expected, len);
  }

  /* ---------------------------------------------------------------------- */
  /* 6.  StreamGenerator basic usage                                      */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamGenerator_singleMessage() throws Exception {
    // Generate a "query" TSIG record to start the stream.
    TSIGRecord queryTsig = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsig, queryTsig);

    Message response = new Message();
    gen.generate(response); // first (and only) message in the stream

    // The response must contain a TSIG record.
    TSIGRecord respTsig = response.getTSIG();
    assertNotNull(respTsig);
    assertEquals(tsig.name, respTsig.getName());

    // Verify the response using the same TSIG key.
    int rcode = tsig.verify(response, response.toWire(), queryTsig);
    assertEquals(Rcode.NOERROR, rcode);
  }

  @Test
  public void testStreamGenerator_multipleMessages() throws Exception {
    // First message (query)
    TSIGRecord queryTsig = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    TSIG.StreamGenerator gen = new TSIG.StreamGenerator(tsig, queryTsig);

    // Simulate a stream of three messages.
    Message m1 = new Message();
    gen.generate(m1); // signed (first)
    Message m2 = new Message();
    gen.generate(m2); // signed (every message, signEveryNthMessage defaults to 1)
    Message m3 = new Message();
    gen.generate(m3); // signed (last)

    // Verify each message in order with a StreamVerifier.
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);
    assertEquals(Rcode.NOERROR, verifier.verify(m1, m1.toWire()));
    assertEquals(Rcode.NOERROR, verifier.verify(m2, m2.toWire()));
    // Last message must be verified as the final one.
    assertEquals(Rcode.NOERROR, verifier.verify(m3, m3.toWire(), true));
  }

  /* ---------------------------------------------------------------------- */
  /* 7.  StreamVerifier error handling                                    */
  /* ---------------------------------------------------------------------- */

  @Test
  public void testStreamVerifier_missingFirstSignature() throws Exception {
    // No TSIG on the first message.
    Message first = new Message();
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, null);
    int rcode = verifier.verify(first, first.toWire());
    assertEquals(Rcode.FORMERR, rcode);
    assertEquals("missing required signature on first message", verifier.getErrorMessage());
  }

  @Test
  public void testStreamVerifier_missingIntermediateSignature() throws Exception {
    // First message signed.
    TSIGRecord queryTsig = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    Message first = new Message();
    tsig.apply(first, (TSIGRecord) null);
    assertEquals(Rcode.NOERROR, verifier.verify(first, first.toWire()));

    // 99 unsigned messages – should be accepted.
    for (int i = 0; i < 99; i++) {
      Message unsigned = new Message();
      assertEquals(Rcode.NOERROR, verifier.verify(unsigned, unsigned.toWire()));
    }

    // 100th message without TSIG – should trigger FORMERR.
    Message missing = new Message();
    int rcode = verifier.verify(missing, missing.toWire());
    assertEquals(Rcode.FORMERR, rcode);
    assertTrue(verifier.getErrorMessage().startsWith("Missing required signature on message #"));
  }

  @Test
  public void testStreamVerifier_missingLastSignature() throws Exception {
    // First message signed.
    TSIGRecord queryTsig = tsig.generate(msg, msg.toWire(), Rcode.NOERROR, null);
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    Message first = new Message();
    tsig.apply(first, (TSIGRecord) null);
    assertEquals(Rcode.NOERROR, verifier.verify(first, first.toWire()));

    // Second message is the last but has no TSIG.
    Message last = new Message();
    int rcode = verifier.verify(last, last.toWire(), true);
    assertEquals(Rcode.FORMERR, rcode);
    assertEquals("Missing required signature on last message", verifier.getErrorMessage());
  }

  /* ---------------------------------------------------------------------- */
  /* 8.  Helper utilities (private) – not part of the public test API        */
  /* ---------------------------------------------------------------------- */

  /**
   * Utility to create a TSIG instance using a raw byte[] key.
   */
  private static TSIG newTsigWithBytes(Name algo, Name name, byte[] keyBytes) {
    return new TSIG(algo, name, keyBytes);
  }
}
