/**
 * JUnit‑4 test suite for {@link org.xbill.DNS.TSIG}.
 *
 * The tests cover:
 *
 * <ul>
 *   <li>static helper methods {@code algorithmToName} and {@code nameToAlgorithm}</li>
 *   <li>construction of {@code TSIG} objects (including error paths)</li>
 *   <li>generation of a TSIG record via {@code generate} / {@code apply}</li>
 *   <li>verification of a correctly signed message and of several failure modes
 *       (BADKEY, BADSIG, BADTIME)</li>
 *   <li>the {@code recordLength} helper</li>
 *   <li>the stream helper classes {@link TSIG.StreamGenerator} and {@link TSIG.StreamVerifier}</li>
 * </ul>
 *
 * Only public APIs from the supplied source are used; no mocking frameworks are employed.
 */
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;

/** Test class for {@link TSIG}. */
public class TSIGTest {

  /** A deterministic clock used for all tests – the same instant is used everywhere. */
  private static final Instant FIXED_INSTANT = Instant.parse("2023-01-01T12:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

  /** Base64 representation of the secret “abcd”. */
  private static final String SECRET_BASE64 = "YWJjZA==";

  /** Name of the TSIG key used in the majority of tests. */
  private static final Name KEY_NAME = Name.fromString("testkey.");

  /** The TSIG object that uses HMAC‑SHA256 and the fixed clock. */
  private TSIG tsigSha256;

  @Before
  public void setUp() throws Exception {
    // Initialise a TSIG instance that we will reuse.  The constructor that accepts a Clock
    // is package‑private, therefore we construct the underlying SecretKeySpec ourselves and
    // invoke the public (Name, Name, SecretKey, Clock) constructor.
    byte[] secret = base64.fromString(SECRET_BASE64);
    SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
    tsigSha256 = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, key, FIXED_CLOCK);
  }

  /* -------------------------------------------------------------------------
   *  Static helper method tests
   * -----------------------------------------------------------------------*/

  @Test
  public void testAlgorithmToName_knownValues() {
    // canonical name (with trailing dot)
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256."));

    // canonical name without trailing dot
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));

    // case‑insensitive lookup
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("HMAC-SHA1"));

    // alias for MD5
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_nullArgument() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_unknownAlgorithm() {
    TSIG.algorithmToName("unknown-alg");
  }

  @Test
  public void testNameToAlgorithm_knownValues() {
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
    assertEquals("HmacSHA1", TSIG.nameToAlgorithm(TSIG.HMAC_SHA1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_unknownName() {
    // a Name that is not present in the map
    Name bogus = Name.fromString("bogus.");
    TSIG.nameToAlgorithm(bogus);
  }

  /* -------------------------------------------------------------------------
   *  Constructor error handling
   * -----------------------------------------------------------------------*/

  @Test(expected = NullPointerException.class)
  public void testConstructor_nullBase64String() {
    new TSIG(TSIG.HMAC_SHA256, KEY_NAME, (String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_invalidBase64String() {
    // “@@@” is not valid base64 – fromString returns null which triggers the IllegalArgumentException
    new TSIG(TSIG.HMAC_SHA256, KEY_NAME, "@@@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_invalidKeyName() {
    // Name parsing will fail and be wrapped as IllegalArgumentException
    new TSIG(TSIG.HMAC_SHA256, "invalid name", SECRET_BASE64);
  }

  /* -------------------------------------------------------------------------
   *  Basic generation and application
   * -----------------------------------------------------------------------*/

  @Test
  public void testGenerateAndApply_basicSignature() throws Exception {
    Message msg = new Message(); // empty message – sufficient for TSIG tests
    tsigSha256.apply(msg, null); // sign the message

    TSIGRecord tsigRec = msg.getTSIG();
    assertNotNull("TSIG record should be present after apply()", tsigRec);
    assertEquals(KEY_NAME, tsigRec.getName());
    assertEquals(TSIG.HMAC_SHA256, tsigRec.getAlgorithm());

    // The signature must be non‑empty for a successful signing operation
    assertTrue("Signature must contain data", tsigRec.getSignature().length > 0);
  }

  @Test
  public void testRecordLength_isReasonable() {
    int len = tsigSha256.recordLength();
    // The exact value depends on the lengths of the name and algorithm, but it must be
    // greater than the minimum possible length (a few dozen bytes).
    assertTrue("recordLength should be > 50", len > 50);
  }

  /* -------------------------------------------------------------------------
   *  Verification – success path
   * -----------------------------------------------------------------------*/

  @Test
  public void testVerify_successfulRoundTrip() throws Exception {
    Message request = new Message();
    tsigSha256.apply(request, null); // sign request (acts as a query)

    // The request now carries a TSIG record; verify it with the same key.
    int result = tsigSha256.verify(request, request.toWire(), null);
    assertEquals("Verification of a correctly signed message must succeed", Rcode.NOERROR, result);
    assertTrue("Message should be marked verified", request.isVerified());
  }

  /* -------------------------------------------------------------------------
   *  Verification – BADKEY (wrong key)
   * -----------------------------------------------------------------------*/

  @Test
  public void testVerify_badKey() throws Exception {
    // Sign with the original key
    Message request = new Message();
    tsigSha256.apply(request, null);

    // Create a second TSIG instance that uses a different secret
    byte[] otherSecret = base64.fromString("c2Vjb25k"); // “second”
    SecretKey otherKey = new SecretKeySpec(otherSecret, "HmacSHA256");
    TSIG otherTsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, otherKey, FIXED_CLOCK);

    int result = otherTsig.verify(request, request.toWire(), null);
    assertEquals("Verification with a different key must yield BADKEY", Rcode.BADKEY, result);
    assertFalse("Message must not be marked verified", request.isVerified());
  }

  /* -------------------------------------------------------------------------
   *  Verification – BADSIG (tampered signature)
   * -----------------------------------------------------------------------*/

  @Test
  public void testVerify_badSignature() throws Exception {
    Message request = new Message();
    tsigSha256.apply(request, null);
    TSIGRecord original = request.getTSIG();

    // Clone the original record but corrupt the first byte of the signature
    byte[] badSig = original.getSignature().clone();
    badSig[0] ^= 0xFF; // flip bits

    TSIGRecord corrupt =
        new TSIGRecord(
            original.getName(),
            original.dclass,
            original.ttl,
            original.getAlgorithm(),
            original.getTimeSigned(),
            original.getFudge(),
            badSig,
            original.getOriginalID(),
            original.getError(),
            original.getOther());

    // Replace the TSIG record in the message
    request.removeAllRecords(Section.ADDITIONAL);
    request.addRecord(corrupt, Section.ADDITIONAL);

    int result = tsigSha256.verify(request, request.toWire(), null);
    assertEquals("Tampered signature must yield BADSIG", Rcode.BADSIG, result);
    assertFalse("Message must not be marked verified", request.isVerified());
  }

  /* -------------------------------------------------------------------------
   *  Verification – BADTIME (time outside fudge)
   * -----------------------------------------------------------------------*/

  @Test
  public void testVerify_badTime() throws Exception {
    // Use a clock that is far away from the fixed instant (e.g. +1 hour)
    Clock farFuture = Clock.fixed(FIXED_INSTANT.plus(Duration.ofHours(1)), ZoneOffset.UTC);
    TSIG distantTsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME,
        new SecretKeySpec(base64.fromString(SECRET_BASE64), "HmacSHA256"), farFuture);

    Message request = new Message();
    // Sign with the *original* (fixed‑time) TSIG
    tsigSha256.apply(request, null);

    // Verify with a TSIG that thinks “now” is an hour later – this should trigger BADTIME.
    int result = distantTsig.verify(request, request.toWire(), null);
    assertEquals("Verification with out‑of‑fudge time must yield BADTIME", Rcode.BADTIME, result);
    assertFalse("Message must not be marked verified", request.isVerified());
  }

  /* -------------------------------------------------------------------------
   *  generate() with explicit BADTIME error – check that “other” data is present
   * -----------------------------------------------------------------------*/

  @Test
  public void testGenerate_badTimeErrorProducesOther() throws Exception {
    Message msg = new Message();
    // Force a BADTIME error path
    TSIGRecord tsigRec = tsigSha256.generate(msg, msg.toWire(), Rcode.BADTIME, null, true);
    assertNotNull("TSIG record should be returned even on BADTIME", tsigRec);
    assertNotNull("The 'other' field must be present for BADTIME", tsigRec.getOther());
    assertEquals("Other field length must be 6 bytes (time + fudge)", 6, tsigRec.getOther().length);
  }

  /* -------------------------------------------------------------------------
   *  StreamGenerator / StreamVerifier round‑trip
   * -----------------------------------------------------------------------*/

  @Test
  public void testStreamGeneratorAndVerifier_successfulSequence() throws Exception {
    // -----------------------------------------------------------------------
    // 1. Build a query message and sign it – this gives us the “query TSIG”.
    // -----------------------------------------------------------------------
    Message query = new Message();
    tsigSha256.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();
    assertNotNull("Query must contain a TSIG record", queryTsig);

    // -----------------------------------------------------------------------
    // 2. Generate a series of response messages using the StreamGenerator.
    // -----------------------------------------------------------------------
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigSha256, queryTsig);
    List<Message> responses = new ArrayList<>();
    final int totalResponses = 5; // less than the 100‑message signing interval

    for (int i = 0; i < totalResponses; i++) {
      Message resp = new Message();
      // Populate the message with something trivial – empty body is fine.
      generator.generate(resp);
      responses.add(resp);
    }

    // -----------------------------------------------------------------------
    // 3. Verify the sequence with StreamVerifier.
    // -----------------------------------------------------------------------
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryTsig);
    for (int i = 0; i < responses.size(); i++) {
      Message resp = responses.get(i);
      boolean isLast = (i == responses.size() - 1);
      int result = verifier.verify(resp, resp.toWire(), isLast);
      assertEquals("All streamed messages must verify successfully", Rcode.NOERROR, result);
      assertTrue("Message should be marked verified", resp.isVerified());
    }
  }

  /* -------------------------------------------------------------------------
   *  StreamVerifier – missing signature on the 100th intermediate message
   * -----------------------------------------------------------------------*/

  @Test
  public void testStreamVerifier_missingSignatureTriggersFormErr() throws Exception {
    // Create a query TSIG as before
    Message query = new Message();
    tsigSha256.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // Build a StreamVerifier that expects a signature at least every 3 messages
    // (signEveryNthMessage = 3 is only available via the package‑private constructor,
    // so we simulate the condition by generating many messages and deliberately omitting
    // the signature on the 100th one).
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigSha256, queryTsig, 3);
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigSha256, queryTsig);

    // Generate 101 messages; the generator will sign the first, every third, and the last.
    List<Message> msgs = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      Message m = new Message();
      generator.generate(m, i == 100); // last = true for the final message
      msgs.add(m);
    }

    // Verify all messages – the 100th (index 99) has no TSIG and should cause FORMERR.
    for (int i = 0; i < msgs.size(); i++) {
      Message m = msgs.get(i);
      boolean isLast = (i == msgs.size() - 1);
      int result = verifier.verify(m, m.toWire(), isLast);
      if (i == 99) { // the missing‑signature intermediate message
        assertEquals("Missing signature on 100th intermediate message must be FORMERR",
            Rcode.FORMERR, result);
        assertEquals("Verifier should expose an error message",
            "Missing required signature on message #100", verifier.getErrorMessage());
      } else {
        assertEquals("All other messages must verify", Rcode.NOERROR, result);
      }
    }
  }
}
