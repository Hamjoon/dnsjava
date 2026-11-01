package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.net.InetAddress;

@ExtendWith(MockitoExtension.class)
class TSIG_apply_2_Test {

    /**
     * Fixed instant used for all TSIG operations in the test suite.
     */
    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

    /**
     * The clock that always returns {@link #FIXED_INSTANT}.
     */
    private static Clock fixedClock;

    /**
     * A TSIG instance that uses HMAC‑SHA256 and a deterministic key.
     */
    private static TSIG tsig;

    /**
     * Algorithm name constant for HMAC‑SHA256 (defined in {@link TSIG}).
     */
    private static Name algName;

    /**
     * Owner name for the TSIG key (e.g. {@code example.}).
     */
    private static Name keyName;

    /**
     * A TSIG instance that uses HMAC‑SHA256 and a deterministic key.
     */
    private static TSIG tsigSha256;

    /**
     * Algorithm name constant for HMAC‑SHA256 (defined in {@link TSIG}).
     */
    private static Name algSha256;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // deterministic algorithm/name/key
        // algorithm constant
        algName = TSIG.HMAC_SHA256;
        // key owner name
        keyName = Name.fromString("example.");
        // 32‑byte key for HMAC‑SHA256 (all zeroes – content does not matter for the test)
        byte[] rawKey = new byte[32];
        SecretKey secretKey = new SecretKeySpec(rawKey, "HmacSHA256");
        // fixed clock → deterministic timeSigned field
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        // Build the TSIG object using the constructor that accepts a Clock
        tsig = new TSIG(algName, keyName, secretKey, fixedClock);
    }

    /**
     * Verify that {@code apply(message, old, true)} (full signature) adds a TSIG record
     * with the expected algorithm and a 32‑byte HMAC‑SHA256 signature.
     */
    @Test
    void applyAddsFullSignatureTsigRecord() {
        // --------------------------------------------------------------------
        // given – a signed query that will serve as the “old” TSIGRecord
        // --------------------------------------------------------------------
        Message query = new Message(0);
        // Sign the query with a full signature (the boolean flag is true)
        assertDoesNotThrow(() -> tsig.apply(query, null, true));
        TSIGRecord old = query.getTSIG();
        assertNotNull(old, "The query must contain a TSIG record after signing");
        // --------------------------------------------------------------------
        // when – we apply TSIG to a fresh response message using the old record
        // --------------------------------------------------------------------
        Message response = new Message(0);
        assertDoesNotThrow(() -> tsig.apply(response, old, true));
        // --------------------------------------------------------------------
        // then – the response must contain a TSIG record with expected properties
        // --------------------------------------------------------------------
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response should contain a TSIG record after apply");
        assertEquals(algName, respTsig.getAlgorithm(), "Algorithm name must match the TSIG key");
        assertEquals(keyName, respTsig.getName(), "Owner name must match the TSIG key");
        // Signature length for HMAC‑SHA256 is 32 bytes
        assertNotNull(respTsig.getSignature(), "Signature must not be null");
        assertEquals(32, respTsig.getSignature().length, "Signature length for HMAC‑SHA256 should be 32 bytes");
        // The timeSigned field must be the fixed instant (rounded to seconds)
        assertEquals(FIXED_INSTANT.getEpochSecond(), respTsig.getTimeSigned().getEpochSecond(), "timeSigned must be derived from the fixed clock");
    }

    /**
     * Verify that {@code apply(message, old, false)} (no full signature) also adds a TSIG
     * record with the correct algorithm and signature length.
     */
    @Test
    void applyAddsPartialSignatureTsigRecord() {
        // --------------------------------------------------------------------
        // given – a signed query (full signature) to obtain an old TSIGRecord
        // --------------------------------------------------------------------
        Message query = new Message(0);
        assertDoesNotThrow(() -> tsig.apply(query, null, true));
        TSIGRecord old = query.getTSIG();
        assertNotNull(old, "Old TSIGRecord must be present");
        // --------------------------------------------------------------------
        // when – we apply TSIG to a new message but request a partial signature
        // --------------------------------------------------------------------
        Message response = new Message(0);
        assertDoesNotThrow(() -> tsig.apply(response, old, false));
        // --------------------------------------------------------------------
        // then – the response must contain a valid TSIG record
        // --------------------------------------------------------------------
        TSIGRecord respTsig = response.getTSIG();
        assertNotNull(respTsig, "Response should contain a TSIG record");
        assertEquals(algName, respTsig.getAlgorithm(), "Algorithm name must match the TSIG key");
        assertEquals(keyName, respTsig.getName(), "Owner name must match the TSIG key");
        assertEquals(32, respTsig.getSignature().length, "Signature length for HMAC‑SHA256 should be 32 bytes");
        // The timestamp is still derived from the fixed clock, regardless of the flag
        assertEquals(FIXED_INSTANT.getEpochSecond(), respTsig.getTimeSigned().getEpochSecond(), "timeSigned must be derived from the fixed clock");
    }

    @Test
    void applyDelegatesToFourArgOverloadWithNoError() throws Exception {
        // Spy the TSIG instance so we can verify internal calls
        TSIG spy = spy(tsigSha256);
        Message msg = new Message();
        TSIGRecord old = null;
        // fullSignature = true
        spy.apply(msg, old, true);
        // Verify that the four‑argument overload was invoked with Rcode.NOERROR
        verify(spy, times(1)).apply(eq(msg), eq(Rcode.NOERROR), eq(old), eq(true));
    }

    @Test
    void applyThrowsNPEWhenMessageIsNull_fullSignatureTrue() {
        TSIGRecord old = new TSIGRecord(Name.fromConstantString("example.com."), TSIG.HMAC_SHA256, 0, new byte[0], 0, 0, 0, 0, 0, 0);
        assertThrows(NullPointerException.class, () -> tsigSha256.apply(null, old, true));
    }

    @Test
    void applyThrowsNPEWhenMessageIsNull_fullSignatureFalse() {
        assertThrows(NullPointerException.class, () -> tsigSha256.apply(null, null, false));
    }

    @Test
    void applyGeneratesFullLengthMacWhenFullSignatureTrue() throws Exception {
        Message query = Message.createQuery(Name.fromConstantString("example.com."), Type.A);
        tsigSha256.apply(query, null, true);
        TSIGRecord tsig = query.getTSIG();
        assertNotNull(tsig);
        assertEquals(32, tsig.getSignature().length, "Full‑signature must contain the complete SHA‑256 output (32 bytes)");
    }

    @Test
    void applyGeneratesTruncatedMacWhenFullSignatureFalse() throws Exception {
        Message query = Message.createQuery(Name.fromConstantString("example.com."), Type.A);
        tsigSha256.apply(query, null, false);
        TSIGRecord tsig = query.getTSIG();
        assertNotNull(tsig);
        // RFC 2845 recommends truncating to 10 bytes for SHA‑256 when not full‑signature
        assertEquals(10, tsig.getSignature().length, "Truncated signature should be 10 bytes for SHA‑256 when fullSignature is false");
    }

    @Test
    void applyUsesOldRecordButAddsFreshTSIG() throws Exception {
        // Old record (simulating a request TSIG)
        TSIGRecord old = new TSIGRecord(Name.fromConstantString("example.com."), TSIG.HMAC_SHA256, 0, new byte[] { 0x01, 0x02, 0x03, 0x04 }, 0, 0, 0, 0, 0, 0);
        Message response = Message.createQuery(Name.fromConstantString("example.com."), Type.A);
        tsigSha256.apply(response, old, true);
        TSIGRecord newTsig = response.getTSIG();
        assertNotNull(newTsig);
        assertEquals(keyName, newTsig.getName());
        assertEquals(algSha256, newTsig.getAlgorithm());
        // The new MAC must be different from the old one (different timeSigned, etc.)
        assertFalse(java.util.Arrays.equals(old.getSignature(), newTsig.getSignature()), "Generated TSIG must not reuse the MAC from the old record");
    }

    @Test
    void applyTruncatesMacForNonDefaultAlgorithmWhenFullSignatureFalse() throws Exception {
        // Use HMAC‑MD5 (output size 16 bytes) – truncation should still be 10 bytes
        TSIG tsigMd5 = new TSIG(TSIG.HMAC_MD5, keyName, new SecretKeySpec(new byte[16], "HmacMD5"), fixedClock);
        TSIGRecord old = new TSIGRecord(Name.fromConstantString("example.org."), TSIG.HMAC_MD5, 0, new byte[] { 0x0A, 0x0B, 0x0C, 0x0D }, 0, 0, 0, 0, 0, 0);
        Message msg = Message.createQuery(Name.fromConstantString("example.org."), Type.A);
        tsigMd5.apply(msg, old, false);
        TSIGRecord tsig = msg.getTSIG();
        assertNotNull(tsig);
        assertEquals(10, tsig.getSignature().length, "Truncated MAC for MD5 should also be 10 bytes when fullSignature is false");
    }

    @Test
    void applyReplacesExistingTSIGRecord() throws Exception {
        // Message with a pre‑existing TSIG RR
        TSIGRecord existing = new TSIGRecord(Name.fromConstantString("example.net."), TSIG.HMAC_SHA256, 0, new byte[] { 0x01 }, 0, 0, 0, 0, 0, 0);
        Message msg = Message.createQuery(Name.fromConstantString("example.net."), Type.A);
        msg.addRecord(existing, Section.ADDITIONAL);
        // Apply TSIG – should replace the old one
        tsigSha256.apply(msg, null, true);
        TSIGRecord after = msg.getTSIG();
        assertNotNull(after);
        assertEquals(32, after.getSignature().length, "After replacement the MAC length must be the full SHA‑256 size");
        // Ensure only one TSIG RR is present in the ADDITIONAL section
        Record[] additionals = msg.getSectionArray(Section.ADDITIONAL);
        long tsigCount = java.util.Arrays.stream(additionals).filter(r -> r instanceof TSIGRecord).count();
        assertEquals(1L, tsigCount, "Message must contain exactly one TSIG record after apply");
    }

    @Test
    void applySetsErrorFieldToNoErrorOnResponseMessage() throws Exception {
        Message query = Message.createQuery(Name.fromConstantString("example.com."), Type.A);
        Message response = Message.newResponse(query);
        tsigSha256.apply(response, null, true);
        TSIGRecord tsig = response.getTSIG();
        assertNotNull(tsig);
        assertEquals(Rcode.NOERROR, tsig.getError(), "TSIG error field must be NOERROR (0) when apply is invoked without an explicit error");
    }

    @Test
    void applyOnEmptyMessageGeneratesFullMac() throws Exception {
        // no sections populated
        Message empty = new Message();
        tsigSha256.apply(empty, null, true);
        TSIGRecord tsig = empty.getTSIG();
        assertNotNull(tsig);
        assertEquals(32, tsig.getSignature().length, "Even for an empty message the full SHA‑256 MAC must be generated");
    }

    @Test
    void applyWithMultipleAdditionalRecordsGeneratesTruncatedMac() throws Exception {
        Message query = Message.createQuery(Name.fromConstantString("example.com."), Type.A);
        // Add two additional records
        query.addRecord(new ARecord(Name.fromConstantString("example.com."), DClass.IN, 300, InetAddress.getByName("192.0.2.1")), Section.ADDITIONAL);
        query.addRecord(new TXTRecord(Name.fromConstantString("example.com."), DClass.IN, 300, "extra"), Section.ADDITIONAL);
        tsigSha256.apply(query, null, false);
        TSIGRecord tsig = query.getTSIG();
        assertNotNull(tsig);
        assertEquals(10, tsig.getSignature().length, "When fullSignature is false the MAC must be truncated (10 bytes) even with extra records");
    }

    @Test
    void applyRejectsUnsupportedAlgorithmInOldRecord() {
        Name unsupportedAlg = Name.fromConstantString("UNSUPPORTED-ALG.");
        TSIGRecord old = new TSIGRecord(Name.fromConstantString("example.com."), unsupportedAlg, 0, new byte[0], 0, 0, 0, 0, 0, 0);
        Message msg = Message.createQuery(Name.fromConstantString("example.com."), Type.A);
        assertThrows(IllegalArgumentException.class, () -> tsigSha256.apply(msg, old, true));
    }

    @Test
    void applyFailsWhenOldRecordHasNullName() {
        TSIGRecord old = new TSIGRecord(null, TSIG.HMAC_SHA256, 0, new byte[0], 0, 0, 0, 0, 0, 0);
        Message msg = Message.createQuery(Name.fromConstantString("example.org."), Type.A);
        assertThrows(NullPointerException.class, () -> tsigSha256.apply(msg, old, false));
    }

    @Test
    void applyRejectsOldRecordWithOversizedMac() {
        // 64‑byte MAC – exceeds SHA‑256 output size (32)
        byte[] oversizedMac = new byte[64];
        TSIGRecord old = new TSIGRecord(Name.fromConstantString("example.net."), TSIG.HMAC_SHA256, 0, oversizedMac, 0, 0, 0, 0, 0, 0);
        Message msg = Message.createQuery(Name.fromConstantString("example.net."), Type.A);
        assertThrows(IllegalArgumentException.class, () -> tsigSha256.apply(msg, old, true));
    }

    @Test
    void applyFailsWhenMessageContainsMultipleTSIGRecords() throws Exception {
        Message msg = Message.createQuery(Name.fromConstantString("example.com."), Type.A);
        TSIGRecord tsig1 = new TSIGRecord(Name.fromConstantString("example.com."), TSIG.HMAC_SHA256, 0, new byte[] { 0x01 }, 0, 0, 0, 0, 0, 0);
        TSIGRecord tsig2 = new TSIGRecord(Name.fromConstantString("example.com."), TSIG.HMAC_SHA256, 0, new byte[] { 0x02 }, 0, 0, 0, 0, 0, 0);
        msg.addRecord(tsig1, Section.ADDITIONAL);
        msg.addRecord(tsig2, Section.ADDITIONAL);
        assertThrows(IllegalStateException.class, () -> tsigSha256.apply(msg, null, true));
    }

    @Test
    void applyPropagatesRuntimeExceptionWhenMacAlgorithmUnavailable() throws Exception {
        // Construct a TSIG with a deliberately unknown algorithm name.
        // The constructor accepts any Name; the failure occurs when MAC.getInstance is called.
        Name unavailableAlg = Name.fromConstantString("HMAC-UNAVAILABLE.");
        TSIG tsig = new TSIG(unavailableAlg, keyName, new SecretKeySpec(new byte[16], "HmacSHA256"), fixedClock);
        Message msg = Message.createQuery(Name.fromConstantString("example.org."), Type.A);
        // The call should wrap the GeneralSecurityException in a RuntimeException
        assertThrows(RuntimeException.class, () -> tsig.apply(msg, null, false));
    }
}
