```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Message#getSectionRRsets(int)}.
 *
 * <p>The tests verify:
 *
 * <ul>
 *   <li>an empty section returns an empty list;
 *   <li>a single record produces a single {@link RRset};
 *   <li>multiple records that belong to the same RRset are grouped together;
 *   <li>records that differ by name, type or class produce separate RRsets;
 *   <li>an illegal section index causes {@link IllegalArgumentException}.
 * </ul>
 *
 * No private members of {@link Message} or its collaborators are accessed; the tests use only the
 * public API.
 */
class Message_getSectionRRsets_Test {

  private Message message;

  @BeforeAll
  static void setupBeforeAll() {
    // No global resources needed.
  }

  @BeforeEach
  void setupBeforeEach() {
    // Create a fresh, empty message for each test.
    message = new Message();
  }

  @AfterEach
  void teardownAfterEach() {
    // Nothing to clean up for this class.
  }

  @AfterAll
  static void teardownAfterAll() {
    // No global teardown required.
  }

  /** Empty section must yield an empty list. */
  @Test
  void getSectionRRsets_emptySection_returnsEmptyList() {
    List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
    assertNotNull(rrsets, "Result should never be null");
    assertTrue(rrsets.isEmpty(), "Empty section should produce an empty list");
  }

  /** Single record should produce a single RRset containing that record. */
  @Test
  void getSectionRRsets_singleRecord_returnsOneRRset() throws UnknownHostException {
    Name name = Name.fromConstantString("example.com.");
    Record a = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    message.addRecord(a, Section.ANSWER);

    List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
    assertEquals(1, rrsets.size(), "Exactly one RRset expected");

    RRset set = rrsets.get(0);
    assertEquals(name, set.getName(), "RRset name must match the record name");
    assertEquals(Type.A, set.getType(), "RRset type must be A");
    assertEquals(DClass.IN, set.getDClass(), "RRset class must be IN");
    assertEquals(1, set.size(), "RRset should contain the single record");
    assertSame(a, set.first(), "The stored record should be the one we added");
  }

  /** Multiple records with identical name, type and class must be merged into one RRset. */
  @Test
  void getSectionRRsets_multipleSameRRset_recordsMerged() throws UnknownHostException {
    Name name = Name.fromConstantString("example.com.");
    Record a1 = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    Record a2 = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.1"));
    message.addRecord(a1, Section.ANSWER);
    message.addRecord(a2, Section.ANSWER);

    List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
    assertEquals(1, rrsets.size(), "Both A records should be in the same RRset");

    RRset set = rrsets.get(0);
    assertEquals(2, set.size(), "RRset should contain both records");
    assertTrue(set.contains(a1), "First A record must be present");
    assertTrue(set.contains(a2), "Second A record must be present");
  }

  /** Records that differ by type must result in distinct RRsets. */
  @Test
  void getSectionRRsets_differentTypes_separateRRsets() throws UnknownHostException {
    Name name = Name.fromConstantString("example.com.");
    Record a = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    Record mx = new MXRecord(name, DClass.IN, 0, 10, Name.fromConstantString("mail.example.com."));
    message.addRecord(a, Section.ANSWER);
    message.addRecord(mx, Section.ANSWER);

    List<RRset> rrsets = message.getSectionRRsets(Section.ANSWER);
    assertEquals(2, rrsets.size(), "A and MX records should be in separate RRsets");

    // Verify that each RRset contains the appropriate record.
    boolean foundA = false, foundMX = false;
    for (RRset set : rrsets) {
      if (set.getType() == Type.A) {
        foundA = true;
        assertEquals(1, set.size());
        assertTrue(set.contains(a));
      } else if (set.getType() == Type.MX) {
        foundMX = true;
        assertEquals(1, set.size());
        assertTrue(set.contains(mx));
      }
    }
    assertTrue(foundA, "RRset for A records not found");
    assertTrue(foundMX, "RRset for MX records not found");
  }

  /** An illegal section index must cause IllegalArgumentException. */
  @Test
  void getSectionRRsets_invalidSection_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> message.getSectionRRsets(99),
        "Section.check should reject out‑of‑range values");
  }
}
```