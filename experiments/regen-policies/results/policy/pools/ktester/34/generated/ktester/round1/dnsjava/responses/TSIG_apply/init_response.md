```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link TSIG#apply(Message, TSIGRecord)} and its overloads.
 *
 * <p>The tests focus on the observable behaviour of the method:
 *
 * <ul>
 *   <li>the message becomes signed (a TSIG record is added);
 *   <li>the generated TSIG can be verified with {@link TSIG#verify(Message, byte[], TSIGRecord)};
 *   <li>the overload that receives an existing TSIGRecord (the “old” record) works correctly.
 * </ul>
 *
 * No private fields or methods are accessed – the tests use only the public API of {@link
 * TSIG}, {@link Message} and {@link TSIGRecord}.
 */
@ExtendWith(MockitoExtension.class)
class TSIG_apply_Test {

  /** A deterministic clock so that the generated timestamps are repeatable. */
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));

  /** The TSIG key used for all tests. */
  private TSIG tsig;

  @BeforeAll
  static void setupBeforeAll() {
    // No global resources to initialise.
  }

  @BeforeEach
  void setupBeforeEach() {
    // Use the HMAC‑SHA256 algorithm with a known key.  The constructor that accepts a
    // Clock lets us keep the timestamp stable, which makes verification deterministic.
    tsig =
        new TSIG(
            TSIG.HMAC_SHA256,
            Name.fromString("example."),
            new SecretKeySpec(base64.fromString("12345678"), "HmacSHA256"),
            FIXED_CLOCK);
  }

  @AfterEach
  void teardownAfterEach() {
    // Nothing to clean up per‑test.
  }

  @AfterAll
  static void teardownAfterAll() {
    // No global resources to release.
  }

  /**
   * Verify that {@code apply(message, null)} adds a TSIG record, that the message reports being
   * signed, and that the generated signature can be verified.
   */
  @Test
  void applyAddsSignatureAndVerificationSucceeds() throws IOException {
    // Build a simple query.
    Record question =
        Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
    Message query = Message.newQuery(question);

    // Apply the TSIG (old record is {@code null}).
    tsig.apply(query, null);

    // The message must now be marked as signed.
    assertTrue(query.isSigned(), "Message should be marked as signed after apply");

    // Wire‑format the message and verify the signature.
    byte[] wire = query.toWire(Message.MAXLENGTH);
    Message parsed = new Message(wire);
    int result = tsig.verify(parsed, wire, null);
    assertEquals(Rcode.NOERROR, result, "Verification of a freshly signed query must succeed");
    assertTrue(parsed.isVerified(), "Parsed message should be marked as verified");
  }

  /**
   * Verify the overload {@code apply(message, oldTSIG, true)} where an existing TSIG record from a
   * previous message is supplied.  The new message must contain a valid TSIG that can be verified
   * using the original TSIG as the “request” record.
   */
  @Test
  void applyWithOldRecordFullSignatureSucceeds() throws IOException {
    // ----------------------------------------------------------------------
    // 1. Create and sign a query – this gives us an “old” TSIG record.
    // ----------------------------------------------------------------------
    Record qQuestion =
        Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
    Message query = Message.newQuery(qQuestion);
    tsig.apply(query, null);
    TSIGRecord oldTsig = query.getTSIG();
    assertNotNull(oldTsig, "The query must contain a TSIG record after signing");

    // ----------------------------------------------------------------------
    // 2. Build a response that references the old TSIG record.
    // ----------------------------------------------------------------------
    Message response = new Message(query.getHeader().getID());
    response.getHeader().setFlag(Flags.QR); // mark as a response
    response.addRecord(query.getQuestion(), Section.QUESTION);
    // add a dummy answer so the message is not empty
    Record answer =
        new ARecord(
            qQuestion.getName(),
            DClass.IN,
            300,
            InetAddress.getByAddress(new byte[] {1, 2, 3, 4}));
    response.addRecord(answer, Section.ANSWER);

    // ----------------------------------------------------------------------
    // 3. Apply the TSIG using the overload that receives the old record and requests a
    //    full signature.
    // ----------------------------------------------------------------------
    tsig.apply(response, oldTsig, true);

    // The response must now be signed.
    assertTrue(response.isSigned(), "Response should be signed after apply with old record");

    // ----------------------------------------------------------------------
    // 4. Verify the response using the original TSIG as the request TSIG.
    // ----------------------------------------------------------------------
    byte[] responseWire = response.toWire(Message.MAXLENGTH);
    Message parsedResponse = new Message(responseWire);
    int verifyResult = tsig.verify(parsedResponse, responseWire, oldTsig);
    assertEquals(
        Rcode.NOERROR,
        verifyResult,
        "Verification of the response that used the old TSIG record must succeed");
    assertTrue(parsedResponse.isVerified(), "Parsed response should be marked as verified");
  }
}
```