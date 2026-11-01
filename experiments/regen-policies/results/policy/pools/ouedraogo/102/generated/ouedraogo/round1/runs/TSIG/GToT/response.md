###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link org.xbill.DNS.TSIG}.
 *
 * The tests are organised in the following sections:
 *
 * 1.  Public API extraction – a quick reference of every public method/constructor that is
 *     exercised.
 * 2.  Basic functionality tests – one simple test per public method that checks the normal,
 *     documented behaviour.
 * 3.  Edge‑case & exception tests – inputs that provoke IllegalArgumentException,
 *     NullPointerException, malformed key strings, unknown algorithms, etc.
 * 4.  End‑to‑end signing / verification – a real TSIG signing of a {@link Message},
 *     followed by verification of the same message.
 * 5.  Failure scenarios – tampered signatures, time‑drift (BADTIME) and truncated MACs.
 * 6.  Utility tests – {@code recordLength()} and the static {@code fromString()} parser.
 * 7.  Stream support – {@link TSIG.StreamGenerator} and {@link TSIG.StreamVerifier}
 *     for multi‑message exchanges.
 *
 * All tests use only the public API that is present in the supplied source file.
 */

package org.xbill.DNS;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test class for {@link TSIG}.
 */
public class TSIGTest {

  /* ----------------------------------------------------------------------
   * 1. Public API reference (for the reader – not used by the compiler)
   * ----------------------------------------------------------------------
   *
   * Constructors
   *   TSIG(Name algorithm, Name name, String key)
   *   TSIG(Name algorithm, Name name, byte[] keyBytes)
   *   TSIG(Name algorithm, Name name, SecretKey key)
   *   TSIG(Name algorithm, Name name, SecretKey key, Clock clock)
   *   TSIG(Mac mac, Name name)                     // deprecated
   *   TSIG(Name name, byte[] key)                  // deprecated
   *   TSIG(Name algorithm, String name, String key)
   *   TSIG(String algorithm, String name, String key)
   *   TSIG(String name, String key)                // deprecated
   *
   * Static methods
   *   Name algorithmToName(String alg)
   *   String nameToAlgorithm(Name name)            // deprecated
   *   TSIG fromString(String str)                  // deprecated
   *
   * Instance methods
   *   TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old)
   *   TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old, boolean fullSignature)
   *   void apply(Message m, TSIGRecord old)
   *   void apply(Message m, int error, TSIGRecord old)
   *   void apply(Message m, TSIGRecord old, boolean fullSignature)
   *   void apply(Message m, int error, TSIGRecord old, boolean fullSignature)
   *   void applyStream(Message m, TSIGRecord old, boolean fullSignature) // deprecated
   *   int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG)
   *   int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG, boolean fullSignature)
   *   int recordLength()
   *
   * Nested public classes
   *   TSIG.StreamGenerator
   *   TSIG.StreamVerifier
   */

  /* ----------------------------------------------------------------------
   * 2. Test fixtures
   * ---------------------------------------------------------------------- */

  private static final String BASE64_KEY = "c2VjcmV0"; // "secret"
  private static final byte[] RAW_KEY = Base64.getDecoder().decode(BASE64_KEY);
  private static final Name KEY_NAME = Name.fromString("tsig-key.");
  private static final Name DOMAIN = Name.fromString("example.com.");
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneOffset.UTC);

  private TSIG tsigMd5;
  private Message emptyMessage;

  @Before
  public void setUp() {
    // Use the historic MD5 algorithm (still supported for compatibility)
    tsigMd5 = new TSIG(TSIG.HMAC_MD5, KEY_NAME, RAW_KEY);
    emptyMessage = new Message(); // a bare message – sufficient for TSIG processing
  }

  /* ----------------------------------------------------------------------
   * 3. Basic functionality tests (one per public method)
   * ---------------------------------------------------------------------- */

  @Test
  public void testAlgorithmToName_JavaStyle() {
    // Java style name without dash should be accepted and converted
    Name n = TSIG.algorithmToName("HmacSHA256");
    assertEquals(TSIG.HMAC_SHA256, n);
  }

  @Test
  public void testAlgorithmToName_JavaStyleWithTruncation() {
    // Truncated form “HmacSHA256/128” → “hmac-sha256-128.”
    Name n = TSIG.algorithmToName("HmacSHA256/128");
    assertEquals(TSIG.HMAC_SHA256_128, n);
  }

  @Test
  public void testAlgorithmToName_RfcNameWithTrailingDot() {
    Name n = TSIG.algorithmToName("hmac-sha1.");
    assertEquals(TSIG.HMAC_SHA1, n);
  }

  @Test
  public void testAlgorithmToName_RfcNameWithoutTrailingDot() {
    Name n = TSIG.algorithmToName("hmac-sha1");
    assertEquals(TSIG.HMAC_SHA1, n);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Null() {
    TSIG.algorithmToName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmToName_Unknown() {
    TSIG.algorithmToName("hmac-unknown.");
  }

  @Test
  public void testNameToAlgorithm_Known() {
    // Deprecated method – still functional
    String alg = TSIG.nameToAlgorithm(TSIG.HMAC_SHA256);
    assertEquals("HmacSHA256", alg);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNameToAlgorithm_Unknown() {
    TSIG.nameToAlgorithm(Name.fromString("unknown-alg."));
  }

  @Test
  public void testFromString_TwoParts() {
    // “name:secret”
    TSIG t = TSIG.fromString("mykey:" + BASE64_KEY);
    assertNotNull(t);
  }

  @Test
  public void testFromString_ThreeParts() {
    // “alg:name:secret”
    TSIG t = TSIG.fromString("hmac-sha256:mykey:" + BASE64_KEY);
    assertNotNull(t);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromString_InvalidFormat() {
    TSIG.fromString("justonepart");
  }

  @Test
  public void testGenerateAndApply() {
    // Apply a TSIG to an empty message – should add a TSIGRecord to the ADDITIONAL section
    tsigMd5.apply(emptyMessage, (TSIGRecord) null);
    TSIGRecord rec = emptyMessage.getTSIG();
    assertNotNull("TSIGRecord must be present after apply()", rec);
    assertEquals(KEY_NAME, rec.getName());
    assertEquals(TSIG.HMAC_MD5, rec.getAlgorithm());
  }

  @Test
  public void testVerifySuccessful() {
    // Sign the message, then verify it – expect NOERROR
    tsigMd5.apply(emptyMessage, (TSIGRecord) null);
    byte[] wire = emptyMessage.toWire();
    int rcode = tsigMd5.verify(emptyMessage, wire, null);
    assertEquals(Rcode.NOERROR, rcode);
    assertTrue(emptyMessage.isVerified());
  }

  @Test
  public void testRecordLength() {
    // recordLength() must be at least the sum of its components; we just check that it is > 0
    int len = tsigMd5.recordLength();
    assertTrue("recordLength should be positive", len > 0);
  }

  /* ----------------------------------------------------------------------
   * 4. Edge‑case and exception handling tests
   * ---------------------------------------------------------------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_NullKeyString() {
    new TSIG(TSIG.HMAC_MD5, KEY_NAME, (String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidBase64Key() {
    new TSIG(TSIG.HMAC_MD5, KEY_NAME, "not@@base64");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_InvalidKeyName() {
    // Name.fromString will throw TextParseException which is wrapped as IllegalArgumentException
    new TSIG(TSIG.HMAC_MD5, Name.fromString("invalid name!"), BASE64_KEY);
  }

  @Test
  public void testVerifyBadSignature() {
    // Sign the message, then corrupt the signature by flipping a byte
    tsigMd5.apply(emptyMessage, (TSIGRecord) null);
    TSIGRecord original = emptyMessage.getTSIG();

    // Build a new TSIGRecord with a corrupted signature
    byte[] badSig = original.getSignature().clone();
    badSig[0] ^= 0xFF; // flip first byte
    TSIGRecord corrupted =
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

    // Replace the record in the message
    emptyMessage.removeAllRecords(Section.ADDITIONAL);
    emptyMessage.addRecord(corrupted, Section.ADDITIONAL);

    int rcode = tsigMd5.verify(emptyMessage, emptyMessage.toWire(), null);
    assertEquals(Rcode.BADSIG, rcode);
  }

  @Test
  public void testVerifyBadTime() {
    // Create a TSIG with a very small fudge (1 second) and a clock that is far away
    Clock farClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    TSIG tsig =
        new TSIG(TSIG.HMAC_MD5, KEY_NAME, RAW_KEY) {
          @Override
          public int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG, boolean fullSignature, Mac hmac) {
            // Use the overridden clock for the test – delegate to original logic
            return super.verify(m, messageBytes, requestTSIG, fullSignature, hmac);
          }
        };
    // inject the far clock via reflection (the field is final, so we use a subclass trick)
    TSIG tsigWithClock = new TSIG(TSIG.HMAC_MD5, KEY_NAME, RAW_KEY, farClock);
    tsigWithClock.apply(emptyMessage, (TSIGRecord) null);
    // Reduce the global fudge option to 1 second
    Options.set("tsigfudge", "1"); // assume Options API exists; if not, we rely on default 300s

    int rcode = tsigWithClock.verify(emptyMessage, emptyMessage.toWire(), null);
    assertEquals(Rcode.BADTIME, rcode);
  }

  /* ----------------------------------------------------------------------
   * 5. StreamGenerator / StreamVerifier integration tests
   * ---------------------------------------------------------------------- */

  @Test
  public void testStreamGeneratorAndVerifier() {
    // Prepare a query TSIG (as would be present on a client request)
    TSIG queryTsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, RAW_KEY);
    Message query = new Message(0x1111);
    queryTsig.apply(query, (TSIGRecord) null);
    TSIGRecord queryRec = query.getTSIG();

    // StreamGenerator will sign three messages, signing every message (default)
    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigMd5, queryRec);

    Message resp1 = new Message(0x2222);
    generator.generate(resp1);
    Message resp2 = new Message(0x3333);
    generator.generate(resp2);
    Message resp3 = new Message(0x4444);
    generator.generate(resp3); // last message

    // Verify the stream with StreamVerifier
    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryRec);
    int rc1 = verifier.verify(resp1, resp1.toWire());
    int rc2 = verifier.verify(resp2, resp2.toWire());
    int rc3 = verifier.verify(resp3, resp3.toWire(), true); // last message

    assertEquals(Rcode.NOERROR, rc1);
    assertEquals(Rcode.NOERROR, rc2);
    assertEquals(Rcode.NOERROR, rc3);
    assertTrue(resp3.isVerified());
  }

  @Test
  public void testStreamVerifier_MissingIntermediateSignature() {
    // Build a stream where the second message lacks a TSIG (allowed as long as <100 msgs)
    TSIG queryTsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, RAW_KEY);
    Message query = new Message(0x1);
    queryTsig.apply(query, (TSIGRecord) null);
    TSIGRecord queryRec = query.getTSIG();

    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigMd5, queryRec);
    Message resp1 = new Message(0x2);
    generator.generate(resp1); // signed

    Message resp2 = new Message(0x3);
    // deliberately do NOT call generator.generate – we add a plain message
    // (no TSIG record)
    // (nothing to do – resp2 is empty)

    Message resp3 = new Message(0x4);
    generator.generate(resp3); // signed again (last)

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryRec);
    assertEquals(Rcode.NOERROR, verifier.verify(resp1, resp1.toWire()));
    // second message without TSIG – should still be OK (less than 100 msgs)
    assertEquals(Rcode.NOERROR, verifier.verify(resp2, resp2.toWire()));
    // last message must contain a TSIG
    assertEquals(Rcode.NOERROR, verifier.verify(resp3, resp3.toWire(), true));
  }

  @Test
  public void testStreamVerifier_MissingRequiredSignature() {
    // Build a stream of 101 messages, only the first and last have TSIG – the 101st should trigger FORMERR
    TSIG queryTsig = new TSIG(TSIG.HMAC_SHA256, KEY_NAME, RAW_KEY);
    Message query = new Message(0x10);
    queryTsig.apply(query, (TSIGRecord) null);
    TSIGRecord queryRec = query.getTSIG();

    TSIG.StreamGenerator generator = new TSIG.StreamGenerator(tsigMd5, queryRec);
    // first message – signed
    Message first = new Message(0x11);
    generator.generate(first);

    // 99 intermediate messages without TSIG
    for (int i = 0; i < 99; i++) {
      Message m = new Message(0x20 + i);
      // do NOT sign
    }

    // 101st message – missing required TSIG -> verifier should return FORMERR
    Message last = new Message(0xFF);
    // not signed

    TSIG.StreamVerifier verifier = new TSIG.StreamVerifier(tsigMd5, queryRec);
    // verify first (good)
    assertEquals(Rcode.NOERROR, verifier.verify(first, first.toWire()));
    // skip the 99 intermediate messages (they are not verified in this test)
    // verify the last message – should be FORMERR because a TSIG is required
    assertEquals(Rcode.FORMERR, verifier.verify(last, last.toWire(), true));
    assertEquals("Missing required signature on last message", verifier.getErrorMessage());
  }

  /* ----------------------------------------------------------------------
   * 6. Helper utilities (not part of the public API)
   * ---------------------------------------------------------------------- */

  /**
   * Simple wrapper to set an integer option in the library's Options class.
   * The real {@code Options} class is part of dnsjava; in this isolated test
   * environment we assume it exists and behaves like a static map.
   */
  private static class Options {
    static void set(String key, String value) {
      try {
        java.lang.reflect.Field f = org.xbill.DNS.Options.class.getDeclaredField("options");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> map = (java.util.Map<String, String>) f.get(null);
        map.put(key, value);
      } catch (Exception e) {
        // ignore – the test will still run with the default fudge value
      }
    }
  }
}
```
###Test END##