/**
 * Comprehensive JUnit‑4 tests for {@link org.xbill.DNS.TSIG}.
 *
 * The tests cover:
 * <ul>
 *   <li>Algorithm name conversion (valid/invalid cases)</li>
 *   <li>Reverse conversion (name → Java algorithm string)</li>
 *   <li>Record length calculation</li>
 *   <li>Signature generation and application to a {@link Message}</li>
 *   <li>Successful verification and various failure modes (bad key, bad signature, bad time)</li>
 *   <li>Stream generation / verification (basic happy‑path)</li>
 * </ul>
 *
 * All objects are created with the public API only – no mocking or subclassing.
 */
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
 * JUnit‑4 test suite for {@link TSIG}.
 */
public class TSIGTest {

  private static final String KEY_B64 = Base64.getEncoder().encodeToString("testkey".getBytes());
  private static final Name KEY_NAME = Name.fromString("mykey.");
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

  private TSIG tsig; // HMAC‑MD5 key used in most tests

  @Before
  public void setUp() {
    // Use the public constructor that takes (algorithm, name, base64‑encoded key)
    tsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, KEY_B64);
    // replace the internal clock with a deterministic one via reflection (allowed for testing)
    try {
      java.lang.reflect.Field clockField = TSIG.class.getDeclaredField("clock");
      clockField.setAccessible(true);
      clockField.set(tsig, FIXED_CLOCK);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** -------------------  algorithmToName / nameToAlgorithm  ------------------- */

  @Test
  public void testAlgorithmToName_AliasAndCaseInsensitive() {
    // Alias "HMAC-MD5" (with and without trailing dot) must resolve to HMAC_MD5
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    // Case‑insensitive lookup for the canonical name
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("HMAC-SHA256."));
  }

  @Test
  public void testAlgorithmToName_JavaAlgorithmName() {
    // Java algorithm name should also be accepted
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HmacMD5"));
    assertEquals(TSIG.HMAC_SHA512, TSIG.algorithmToName("HmacSHA512"));
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
  public void testNameToAlgorithm_KnownAndUnknown() {
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));

    try {
      TSIG.nameToAlgorithm(Name.fromString("nonexistent."));
      fail("Expected IllegalArgumentException for unknown algorithm name");
    } catch (IllegalArgumentException expected) {
      // ok
    }
  }

  /** -------------------  recordLength ------------------- */

  @Test
  public void testRecordLength() {
    // The length is deterministic based on the name and algorithm lengths.
    int expected = KEY_NAME.length()
        + 10
        + TSIG.HMAC_MD5.length()
        + 8   // time signed + fudge
        + 18  // MAC length (2) + max MAC (16)
        + 4   // original id + error
        + 8;  // other‑data length field + max other (6)
    assertEquals(expected, tsig.recordLength());
  }

  /** -------------------  generate & apply ------------------- */

  @Test
  public void testGenerateCreatesValidTSIGRecord() {
    Message query = new Message(0x1234);
    // No additional records – a minimal message is sufficient.
    TSIGRecord tsigRec = tsig.generate(query, query.toWire(), Rcode.NOERROR, null);
    assertNotNull(tsigRec);
    assertEquals(KEY_NAME, tsigRec.getName());
    assertEquals(TSIG.HMAC_MD5, tsigRec.getAlgorithm());

    // Signature must be non‑empty and not longer than the MAC length (MD5 = 16)
    byte[] sig = tsigRec.getSignature();
    assertTrue(sig.length > 0);
    assertTrue(sig.length <= 16);
  }

  @Test
  public void testApplyAddsTSIGAndSetsState() {
    Message query = new Message(0x5678);
    tsig.apply(query, null);
    TSIGRecord added = query.getTSIG();
    assertNotNull("TSIGRecord should be attached to the message", added);
    assertEquals(Message.TSIG_SIGNED, query.tsigState);
  }

  /** -------------------  verify (happy path) ------------------- */

  @Test
  public void testVerifySuccessful() {
    // Build a query with a TSIG
    Message query = new Message(0x1111);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Build a response that carries the same TSIG (as a reply)
    Message response = new Message(0x1111);
    // copy the query TSIG as the "requestTSIG" argument
    int rcode = tsig.verify(response, response.toWire(), queryTsig);
    assertEquals(Rcode.NOERROR, rcode);
    assertEquals(Message.TSIG_VERIFIED, response.tsigState);
  }

  /** -------------------  verify – bad key ------------------- */

  @Test
  public void testVerifyBadKey() {
    // Create a second TSIG with a different secret
    TSIG otherKey = new TSIG(TSIG.HMAC_MD5, KEY_NAME, Base64.getEncoder().encodeToString("other".getBytes()));
    // Apply the original TSIG to a query
    Message query = new Message(0x2222);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Attempt verification with the *different* key – should yield BADKEY
    int rcode = otherKey.verify(query, query.toWire(), queryTsig);
    assertEquals(Rcode.BADKEY, rcode);
    assertEquals(Message.TSIG_FAILED, query.tsigState);
  }

  /** -------------------  verify – bad signature ------------------- */

  @Test
  public void testVerifyBadSignature() {
    Message query = new Message(0x3333);
    tsig.apply(query, null);
    TSIGRecord tsigRec = query.getTSIG();

    // Corrupt the signature (flip first byte)
    byte[] badSig = tsigRec.getSignature().clone();
    badSig[0] ^= 0xFF;
    TSIGRecord corrupted =
        new TSIGRecord(
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

    // Replace the original TSIG in the message
    query.removeAllRecords(Section.ADDITIONAL);
    query.addRecord(corrupted, Section.ADDITIONAL);

    int rcode = tsig.verify(query, query.toWire(), null);
    assertEquals(Rcode.BADSIG, rcode);
    assertEquals(Message.TSIG_FAILED, query.tsigState);
  }

  /** -------------------  verify – bad time (fudge) ------------------- */

  @Test
  public void testVerifyBadTime() {
    // Use a clock that is far in the past for generation, but a normal clock for verification
    Clock oldClock = Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC);
    TSIG oldTsig = new TSIG(TSIG.HMAC_MD5, KEY_NAME, KEY_B64);
    // inject the old clock via reflection
    try {
      java.lang.reflect.Field clockField = TSIG.class.getDeclaredField("clock");
      clockField.setAccessible(true);
      clockField.set(oldTsig, oldClock);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Message query = new Message(0x4444);
    oldTsig.apply(query, null);
    TSIGRecord tsigRec = query.getTSIG();

    // Verify with the original (correct) clock – should be BADTIME
    int rcode = tsig.verify(query, query.toWire(), tsigRec);
    assertEquals(Rcode.BADTIME, rcode);
    assertEquals(Message.TSIG_FAILED, query.tsigState);
  }

  /** -------------------  StreamGenerator / StreamVerifier basic flow ------------------- */

  @Test
  public void testStreamGeneratorAndVerifier() {
    // Initial query with TSIG
    Message query = new Message(0x5555);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Stream generator will sign every message (default)
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

    // First response
    Message resp1 = new Message(0x5555);
    generator.generate(resp1);
    assertNotNull(resp1.getTSIG());

    // Second response (also signed)
    Message resp2 = new Message(0x5555);
    generator.generate(resp2);
    assertNotNull(resp2.getTSIG());

    // Verify the two responses using StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);
    int r1 = verifier.verify(resp1, resp1.toWire());
    int r2 = verifier.verify(resp2, resp2.toWire(), true); // last message

    assertEquals(Rcode.NOERROR, r1);
    assertEquals(Rcode.NOERROR, r2);
    assertTrue(resp1.isVerified());
    assertTrue(resp2.isVerified());
  }

  /** -------------------  Helper: create a minimal Message with a question ------------------- */

  private Message createQueryMessage(int id, String qname) throws TextParseException {
    Name name = Name.fromString(qname);
    Record question = Record.newRecord(name, Type.A, DClass.IN);
    Message m = new Message(id);
    m.addRecord(question, Section.QUESTION);
    return m;
  }
}
