```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional unit tests for {@link Zone#removeRecord(Record)} that cover the
 * scenarios described in the JSON test‑case specification.
 *
 * <p>These tests complement the baseline tests already present in the
 * {@code Zone_removeRecord_Test} class. They focus on edge‑cases such as:
 *
 * <ul>
 *   <li>Removing a record when no matching RRset exists.</li>
 *   <li>Deleting a record from a multi‑record RRset.</li>
 *   <li>Removing the sole record of an RRset (causing the RRset to be removed).</li>
 *   <li>Removing an NS record when more than one NS exists.</li>
 *   <li>Attempting to remove the only NS record when a signature is present (the
 *   implementation rejects it).</li>
 *   <li>Handling a {@link Record} whose {@code getName()} returns {@code null}.</li>
 * </ul>
 *
 * <p>The tests use real {@link Zone} objects wherever possible; for the
 * {@code null‑name} case a Mockito mock is employed because the {@code Record}
 * class is abstract.
 */
@ExtendWith(MockitoExtension.class)
class Zone_removeRecord_AdditionalTest {

  /* --------------------------------------------------------------------- *
   *  Shared test data – created before each test method                     *
   * --------------------------------------------------------------------- */
  private Name ZONE_NAME;
  private SOARecord SOA1;
  private NSRecord NS1;
  private NSRecord NS2;
  private ARecord A1;
  private ARecord A2;
  private TXTRecord TXT_SINGLE;
  private Zone zone;

  @BeforeEach
  void setUp() throws Exception {
    // zone name: example.
    ZONE_NAME = Name.fromConstantString("example.");

    // mandatory SOA record
    SOA1 =
        new SOARecord(
            ZONE_NAME,
            DClass.IN,
            3600L,
            Name.fromConstantString("ns.example."),
            new Name("hostadmin", ZONE_NAME),
            1,
            21600L,
            7200L,
            2160000L,
            3600L);

    // two NS records – required by the Zone contract
    NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
    NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

    // two A records with the SAME name – they will share one RRset
    A1 = new ARecord(new Name("multitest", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
    A2 = new ARecord(new Name("multitest", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));

    // a single TXT record (used for the “remove RRset when single” test)
    TXT_SINGLE = new TXTRecord(new Name("lonely", ZONE_NAME), DClass.IN, 3600, "only");

    // Build a zone that contains the mandatory records plus the ones we need for the tests.
    Record[] records = {
        SOA1, NS1, NS2, A1, A2, TXT_SINGLE,
    };
    zone = new Zone(ZONE_NAME, records);
  }

  /* --------------------------------------------------------------------- *
   *  Test case: No RRset found (record does not exist in the zone)        *
   * --------------------------------------------------------------------- */
  @Test
  void removeRecord_noRRsetFound_isNoOp() throws Exception {
    // Record with a name that is not present in the zone.
    Record missing =
        new ARecord(new Name("doesnotexist", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));

    assertNull(zone.findExactMatch(missing.getName(), Type.A));
    assertDoesNotThrow(() -> zone.removeRecord(missing));
    // Ensure the zone is unchanged – a known record is still present.
    assertNotNull(zone.findExactMatch(A1.getName(), Type.A));
  }

  /* --------------------------------------------------------------------- *
   *  Test case: Delete RR when the RRset contains multiple records        *
   * --------------------------------------------------------------------- */
  @Test
  void removeRecord_deleteRRWhenMultiple_keepsRRset() {
    // Pre‑condition: both A records are present.
    assertNotNull(zone.findExactMatch(A1.getName(), Type.A));
    assertNotNull(zone.findExactMatch(A2.getName(), Type.A));

    // Remove the first A record.
    assertDoesNotThrow(() -> zone.removeRecord(A1));

    // The second record must still be present, and the RRset must still exist.
    assertNull(zone.findExactMatch(A1.getName(), Type.A));
    assertNotNull(zone.findExactMatch(A2.getName(), Type.A));
  }

  /* --------------------------------------------------------------------- *
   *  Test case: Remove RRset when it is the only record in the set        *
   * --------------------------------------------------------------------- */
  @Test
  void removeRecord_removeRRsetWhenSingle_deletesRRset() {
    // Verify the TXT record exists.
    assertNotNull(zone.findExactMatch(TXT_SINGLE.getName(), Type.TXT));

    // Remove it – it is the only record of its type/name.
    assertDoesNotThrow(() -> zone.removeRecord(TXT_SINGLE));

    // After removal the exact match must be null.
    assertNull(zone.findExactMatch(TXT_SINGLE.getName(), Type.TXT));
  }

  /* --------------------------------------------------------------------- *
   *  Test case: Removing one NS from a multi‑NS RRset                      *
   * --------------------------------------------------------------------- */
  @Test
  void removeRecord_NSMultipleRecords_deletesOnlyOneNS() {
    // Both NS records are present at the zone apex.
    assertNotNull(zone.findExactMatch(NS1.getName(), Type.NS));
    assertNotNull(zone.findExactMatch(NS2.getName(), Type.NS));

    // Remove NS1 – should succeed because another NS remains.
    assertDoesNotThrow(() -> zone.removeRecord(NS1));

    // NS1 must be gone, NS2 must still be present.
    assertNull(zone.findExactMatch(NS1.getName(), Type.NS));
    assertNotNull(zone.findExactMatch(NS2.getName(), Type.NS));
  }

  /* --------------------------------------------------------------------- *
   *  Test case: Removing the sole NS when a signature is present           *
   * --------------------------------------------------------------------- */
  @Test
  void removeRecord_NSSingleWithSignature_throwsIllegalArgumentException() throws Exception {
    // Build a zone that contains ONE NS record plus a signature (RRSIG) for it.
    // The signature is created with dummy values – the cryptographic content
    // is irrelevant for this test.
    NSRecord soleNs = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns-only.example."));
    // Create a minimal RRSIGRecord that signs the NS RRset.
    RRSIGRecord nsSig =
        new RRSIGRecord(
            ZONE_NAME,
            DClass.IN,
            300L,
            Type.NS,
            1,
            0,
            0,
            0,
            Name.fromConstantString("key.example."),
            new byte[] {0}); // dummy signature

    Record[] records = {SOA1, soleNs, nsSig};
    Zone singleNsWithSigZone = new Zone(ZONE_NAME, records);

    // Attempt to remove the only NS – the implementation checks only the size
    // of the NS RRset (ignoring signatures) and therefore throws.
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> singleNsWithSigZone.removeRecord(soleNs));
    assertTrue(ex.getMessage().contains("Cannot remove all NS"));
  }

  /* --------------------------------------------------------------------- *
   *  Test case: Record whose getName() returns null                         *
   * --------------------------------------------------------------------- */
  @Test
  void removeRecord_nullRecordName_throwsNullPointerException() {
    // Create a mock Record that returns null for getName().
    Record mockRecord = mock(Record.class);
    when(mockRecord.getName()).thenReturn(null);
    when(mockRecord.getType()).thenReturn(Type.A);
    when(mockRecord.getRRsetType()).thenReturn(Type.A);

    // The method will attempt to look up an RRset with a null name, which
    // results in a NullPointerException from the underlying implementation.
    assertThrows(NullPointerException.class, () -> zone.removeRecord(mockRecord));
  }
}
```