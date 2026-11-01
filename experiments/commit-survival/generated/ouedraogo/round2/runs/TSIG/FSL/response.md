###Test START##
package org.xbill.DNS;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.Test;

/**
 * Unit tests for {@link TSIG}. The tests cover:
 *
 * <ul>
 *   <li>algorithm name conversion (algorithmToName, nameToAlgorithm)
 *   <li>record length calculation
 *   <li>generation and verification of a TSIG record on a single message
 *   <li>verification failure when the signed message is altered
 *   <li>basic stream signing (StreamGenerator) and verification (StreamVerifier)
 * </ul>
 *
 * The tests use only the public API that is listed in the source file and the
 * additional signatures provided in the problem statement.
 */
public class TSIGTest {

  /** Helper to create a Base64‑encoded key from raw bytes. */
  private static String base64Key(byte... data) {
    return Base64.getEncoder().encodeToString(data);
  }

  /** A deterministic clock used for stream tests – fixes the instant to a known value. */
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);

  /** Common key name used by many tests. */
  private static final Name KEY_NAME = Name.fromString("testkey.", Name.root);

  /* ------------------------------------------------------------ */
  /*  algorithmToName / nameToAlgorithm                           */
  /* ------------------------------------------------------------ */

  @Test
  public void testAlgorithmToNameValid() {
    // RFC alias (case‑insensitive, with or without trailing dot)
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("HMAC-MD5"));
    assertEquals(TSIG.HMAC_MD5, TSIG.algorithmToName("hmac-md5."));
    // RFC names (different case, with/without dot)
    assertEquals(TSIG.HMAC_SHA256, TSIG.algorithmToName("hmac-sha256"));
    assertEquals(TSIG.HMAC_SHA384, TSIG.algorithmToName("HMAC-SHA384."));
    // Java algorithm name (case‑insensitive)
    assertEquals(TSIG.HMAC_SHA1, TSIG.algorithmToName("HmacSHA1"));
    assertEquals(TSIG.HMAC_SHA512, TSIG.algorithmToName("hmacsHa512"));
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
    assertEquals("HmacMD5", TSIG.nameToAlgorithm(TSIG.HMAC_MD5));
    assertEquals("HmacSHA256", TSIG.nameToAlgorithm(TSIG.HMAC_SHA256));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithmUnknown() {
    // construct a Name that is not present in the map
    Name unknown = Name.fromString("unknown.alg.", Name.root);
    TSIG.nameToAlgorithm(unknown);
  }

  /* ------------------------------------------------------------ */
  /*  recordLength                                                 */
  /* ------------------------------------------------------------ */

  @Test
  public void testRecordLength() {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA1, KEY_NAME, "AQID"); // key = {0x01,0x02,0x03}
    int expected =
        KEY_NAME.length()
            + 10
            + TSIG.HMAC_SHA1.length()
            + 8 // time signed + fudge
            + 18 // MAC length (2) + max MAC (16)
            + 4 // original id + error
            + 8; // other‑data length field (2) + max other (6)
    assertEquals(expected, tsig.recordLength());
  }

  /* ------------------------------------------------------------ */
  /*  generate / apply / verify (single message)                  */
  /* ------------------------------------------------------------ */

  @Test
  public void testGenerateApplyAndVerifySuccess() throws Exception {
    // Use HMAC‑SHA256 with a short key
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, base64Key((byte) 0x01, (byte) 0x02, (byte) 0x03));

    // Create a minimal message (ID 0x1234)
    Message query = new Message(0x1234);
    // No additional records – we only need a header for signing
    tsig.apply(query, null); // sign as a request (no prior TSIG)

    // The message must now contain a TSIG record
    TSIGRecord tsigRec = query.getTSIG();
    assertNotNull("TSIG record should be present after apply()", tsigRec);
    assertEquals(KEY_NAME, tsigRec.getName());
    assertEquals(TSIG.HMAC_SHA256, tsigRec.getAlgorithm());

    // Verify the signature using the same TSIG object
    int result = tsig.verify(query, query.toWire(), null);
    assertEquals("Verification should succeed", Rcode.NOERROR, result);
    assertTrue("Message should be marked verified", query.isVerified());
  }

  @Test
  public void testVerifyFailsWhenMessageIsTampered() throws Exception {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA1, KEY_NAME, base64Key((byte) 0xAA, (byte) 0xBB));

    Message msg = new Message(0x4321);
    tsig.apply(msg, null);
    TSIGRecord rec = msg.getTSIG();
    assertNotNull(rec);

    // Obtain the wire representation and corrupt a single byte after the header
    byte[] wire = msg.toWire();
    // Ensure we do not corrupt the TSIG signature length fields (they are at the end)
    // Flip the first byte of the DNS payload (after the 12‑byte header)
    wire[12] ^= 0xFF;

    // Verify against the corrupted wire
    int result = tsig.verify(msg, wire, null);
    assertEquals("Verification must fail with BADSIG", Rcode.BADSIG, result);
    assertFalse("Message should not be marked verified", msg.isVerified());
  }

  /* ------------------------------------------------------------ */
  /*  StreamGenerator / StreamVerifier (basic two‑message flow)    */
  /* ------------------------------------------------------------ */

  @Test
  public void testStreamGeneratorAndVerifier() throws Exception {
    // Prepare a TSIG key (HMAC‑SHA256) with a deterministic clock
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, base64Key((byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF));

    // -----------------------------------------------------------------
    // Step 1 – create a query message and sign it (this becomes the
    //          “query TSIG” that the stream generator will reference.
    // -----------------------------------------------------------------
    Message query = new Message(0x1000);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();
    assertNotNull(queryTsig);

    // -----------------------------------------------------------------
    // Step 2 – create a StreamGenerator and generate two response messages.
    // -----------------------------------------------------------------
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig);

    Message response1 = new Message(0x1001);
    generator.generate(response1); // first response – must contain a TSIG

    Message response2 = new Message(0x1002);
    generator.generate(response2); // second response – also contains a TSIG because
                                   // signEveryNthMessage defaults to 1

    // Both responses must now carry a TSIG record
    assertNotNull("First response must have TSIG", response1.getTSIG());
    assertNotNull("Second response must have TSIG", response2.getTSIG());

    // -----------------------------------------------------------------
    // Step 3 – verify the two messages using a StreamVerifier.
    // -----------------------------------------------------------------
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // Verify first response (not last)
    int r1 = verifier.verify(response1, response1.toWire(), false);
    assertEquals("First response verification", Rcode.NOERROR, r1);
    assertTrue(response1.isVerified());

    // Verify second response (last message in the stream)
    int r2 = verifier.verify(response2, response2.toWire(), true);
    assertEquals("Second response verification", Rcode.NOERROR, r2);
    assertTrue(response2.isVerified());
  }

  @Test
  public void testStreamVerifierDetectsMissingIntermediateSignature() throws Exception {
    TSIG tsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, base64Key((byte) 0x11, (byte) 0x22));

    // Query with TSIG
    Message query = new Message(0x2000);
    tsig.apply(query, null);
    TSIGRecord queryTsig = query.getTSIG();

    // StreamGenerator that signs only every 2nd message (signEveryNthMessage = 2)
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsig, queryTsig, 2);

    // First response – signed (required)
    Message resp1 = new Message(0x2001);
    generator.generate(resp1);
    assertNotNull(resp1.getTSIG());

    // Second response – NOT signed (because of signEveryNthMessage = 2)
    Message resp2 = new Message(0x2002);
    generator.generate(resp2);
    assertNull("Second response should be unsigned", resp2.getTSIG());

    // Third response – signed again (required because 2 messages have passed)
    Message resp3 = new Message(0x2003);
    generator.generate(resp3);
    assertNotNull(resp3.getTSIG());

    // Verify the stream
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsig, queryTsig);

    // 1st (signed)
    assertEquals(Rcode.NOERROR, verifier.verify(resp1, resp1.toWire(), false));
    // 2nd (unsigned) – should be accepted because < 100 messages since last signed
    assertEquals(Rcode.NOERROR, verifier.verify(resp2, resp2.toWire(), false));
    // 3rd (signed) – verification succeeds
    assertEquals(Rcode.NOERROR, verifier.verify(resp3, resp3.toWire(), true));
  }

  /* ------------------------------------------------------------ */
  /*  Private helper to create a TSIG from raw key bytes (base64)   */
  /* ------------------------------------------------------------ */
  private static TSIG newTsig(Name algorithm, Name name, byte... keyBytes) {
    String b64 = base64.toString(keyBytes);
    return new TSIG(algorithm, name, b64);
  }
}
###Test END##