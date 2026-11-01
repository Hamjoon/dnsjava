
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Zone_addRecord_Test {

    private static final long TTL = 3600L;

    private Name zoneName;

    private SOARecord soa1;

    private SOARecord soa2;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aRecord;

    private ARecord aRecord2;

    private Zone zone;

    /**
     * Create a fresh {@link Zone} instance before each test.
     */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        // zone name: example.
        zoneName = Name.fromConstantString("example.");
        // First SOA (will be replaced in one test)
        soa1 = new SOARecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        // Second SOA (used to replace the first one)
        soa2 = new SOARecord(zoneName, DClass.IN, TTL, Name.fromConstantString("ns.example."), new Name("zoneadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        // Two NS records (required for a valid zone)
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        // A couple of A records for later tests
        aRecord = new ARecord(new Name("www", zoneName), DClass.IN, TTL, InetAddress.getByName("127.0.0.1"));
        aRecord2 = new ARecord(new Name("www", zoneName), DClass.IN, TTL, InetAddress.getByName("127.0.0.2"));
        // Initialise the zone with a valid SOA and NS records.
        zone = new Zone(zoneName, soa1, ns1, ns2);
    }

    /**
     * Adding a {@code null} record must throw {@link IllegalArgumentException}.
     */
    @Test
    void addRecord_nullRecord_throws() {
        assertThrows(IllegalArgumentException.class, () -> zone.addRecord(null));
    }

    /**
     * Adding an SOA whose owner does not match the zone origin must fail.
     */
    @Test
    void addRecord_soaOwnerMismatch_throws() throws IOException {
        // Create an SOA whose name is *not* the zone origin.
        Name otherName = Name.fromConstantString("otherzone.");
        SOARecord mismatchedSOA = new SOARecord(otherName, DClass.IN, TTL, Name.fromConstantString("ns.otherzone."), new Name("hostadmin", otherName), 1, 21600L, 7200L, 2160000L, 3600L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRecord(mismatchedSOA));
        assertTrue(ex.getMessage().contains("SOA owner"));
    }

    /**
     * Adding a record whose name is not a sub‑domain of the zone origin must fail.
     */
    @Test
    void addRecord_nameOutsideZone_throws() throws IOException {
        Name outside = Name.fromConstantString("outside.com.");
        ARecord outsideRecord = new ARecord(outside, DClass.IN, TTL, InetAddress.getByName("127.0.0.9"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRecord(outsideRecord));
        assertTrue(ex.getMessage().contains("is absolute and not a subdomain"));
    }

    /**
     * Adding a record for a name/type that does not yet exist creates a new RRset.
     */
    @Test
    void addRecord_newRRset_created() {
        // The zone currently has no A records.
        assertNull(zone.findExactMatch(aRecord.getName(), Type.A));
        zone.addRecord(aRecord);
        RRset rrset = zone.findExactMatch(aRecord.getName(), Type.A);
        assertNotNull(rrset);
        assertEquals(1, rrset.size());
        assertTrue(rrset.containsRR(aRecord));
    }

    /**
     * Adding a record that matches an existing RRset appends to that RRset.
     */
    @Test
    void addRecord_existingRRset_appends() {
        // First add one A record.
        zone.addRecord(aRecord);
        // Then add a second A record with the same name/type.
        zone.addRecord(aRecord2);
        RRset rrset = zone.findExactMatch(aRecord.getName(), Type.A);
        assertNotNull(rrset);
        assertEquals(2, rrset.size());
        assertTrue(rrset.containsRR(aRecord));
        assertTrue(rrset.containsRR(aRecord2));
    }

    /**
     * Adding a new SOA replaces the existing zone SOA.
     */
    @Test
    void addRecord_replaceSOA_success() {
        // Verify initial SOA.
        assertEquals(soa1, zone.getSOA());
        // Add a new SOA (same owner = zone origin).
        zone.addRecord(soa2);
        // The zone's SOA reference must now point to the new object.
        assertEquals(soa2, zone.getSOA());
        // The old SOA must no longer be present in the zone.
        RRset oldSOARRset = zone.findExactMatch(soa1.getName(), Type.SOA);
        assertNull(oldSOARRset);
    }

    /**
     * Adding a record to a {@link Zone} whose {@code origin} field is {@code null}
     * should result in a {@link NullPointerException} when {@code name.subdomain(origin)}
     * is evaluated.
     */
    @Test
    void addRecord_nullZoneOrigin_throwsNPE() throws Exception {
        // Construct a normal zone first.
        Zone zoneWithNullOrigin = new Zone(zoneName, soa1, ns1, ns2);
        // Use reflection to set the private 'origin' field to null.
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        originField.set(zoneWithNullOrigin, null);
        // Attempt to add a valid A record – we expect NPE.
        assertThrows(NullPointerException.class, () -> zoneWithNullOrigin.addRecord(aRecord));
    }

    /**
     * Adding a {@link Record} whose {@code getName()} returns {@code null}
     * must cause a {@link NullPointerException} because the method dereferences the name.
     */
    @Test
    void addRecord_recordWithNullName_throwsNPE() {
        // Mock a Record that returns null for getName().
        Record mockRecord = mock(Record.class);
        when(mockRecord.getName()).thenReturn(null);
        when(mockRecord.getRRsetType()).thenReturn(Type.A);
        // The other methods are not used by addRecord, but we stub them to avoid NPEs
        // if the implementation accesses them later.
        when(mockRecord.getTTL()).thenReturn(TTL);
        when(mockRecord.getDClass()).thenReturn(DClass.IN);
        when(mockRecord.getType()).thenReturn(Type.A);
        assertThrows(NullPointerException.class, () -> zone.addRecord(mockRecord));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Zone_addRecord_Test.java:96: error: cannot find symbol
        assertTrue(rrset.containsRR(aRecord));
                        ^
  symbol:   method containsRR(ARecord)
  location: variable rrset of type RRset
src/test/java/org/xbill/DNS/Zone_addRecord_Test.java:111: error: cannot find symbol
        assertTrue(rrset.containsRR(aRecord));
                        ^
  symbol:   method containsRR(ARecord)
  location: variable rrset of type RRset
src/test/java/org/xbill/DNS/Zone_addRecord_Test.java:112: error: cannot find symbol
        assertTrue(rrset.containsRR(aRecord2));
                        ^
  symbol:   method containsRR(ARecord)
  location: variable rrset of type RRset
3 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Zone': '\n * A DNS zone. This encapsulates all data related to a zone, and provides convenient lookup methods.\n * A zone always contains a {@link SOARecord} and at least one {@link NSRecord}.\n *\n * @implNote This class uses a {@link java.util.concurrent.locks.ReadWriteLock} to ensure access and\n *     manipulation is safe from multiple threads. Iterators are "weakly consistent" (see the\n *     package info from {@code java.util.concurrent}).\n * @author Brian Wellington\n ', 'api document of method addRecord(T)': '\n   * Adds a record to the zone. If there is an existing {@link RRset} of the same {@link Name} and\n   * {@link Type}, the record is <b>added</b> to this set.\n   *\n   * @param r The record to add.\n   * @throws IllegalArgumentException if {@code name} is {@code null} or if the rrset name does not\n   *     match the zone origin.\n   ', 'dependent classes': 'class `org.xbill.DNS.Type`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Type.SOA;\n\t\nclass `org.xbill.DNS.Zone`:\n\tdependent fields:\n\t\torg.xbill.DNS.SOARecord org.xbill.DNS.Zone.SOA;\n\t\torg.xbill.DNS.Name org.xbill.DNS.Zone.origin;\n\trelated functions:\n\t\tmethod `private void fromXFR(ZoneTransferIn)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`;\n\t\tmethod `private void removeRRsetWithoutLock(Name, int)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`;\n\t\tmethod `public void removeRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: \n   * Removes a record from the zone.\n   *\n   * @param r The record to remove.\n   * @throws IllegalArgumentException if {@code r} is {@code null}, if the record to remove is the\n   *     {@link SOARecord} or the last {@link NSRecord}.\n   ;\n\t\tmethod `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`;\n\t\tmethod `public void addRRset(RRset)` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: \n   * Adds an RRset to the zone.\n   *\n   * @implNote An existing {@link RRset} of the same {@link Name} and {@link Type} is\n   *     <b>replaced</b>.\n   * @param rrset The RRset to add.\n   * @see RRset\n   * @throws IllegalArgumentException if {@code rrset} is {@code null} or if the rrset name is not a\n   *     {@link Name#subdomain(Name) subdomain} of the zone origin (or, in case of a SOA, is not\n   *     equal to the zone origin).\n   ;\n\t\tmethod `public SetResponse findRecords(Name, int)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: \n   * Looks up Records in the zone. The answer can be a {@code CNAME} instead of the actual requested\n   * type and wildcards are expanded.\n   *\n   * @param name The name to look up\n   * @param type The type to look up\n   * @return A SetResponse object\n   * @throws IllegalArgumentException if {@code name} is {@code null}.\n   * @throws InvalidTypeException if the specified {@code type} is invalid.\n   * @see SetResponse\n   ;\n\t\tmethod `private void validate()` returns `void`, related with `org.xbill.DNS.Zone.addRecord(T)`;\nclass `org.xbill.DNS.dnssec.ValUtils`:\n\trelated functions:\n\t\tmethod `public static boolean nsecProvesNameError(SRRset, NSECRecord, Name)` returns `boolean`, related with `org.xbill.DNS.Zone.addRecord(T)`, api document: \n   * Determine if the given NSEC proves a NameError (NXDOMAIN) for a given qname.\n   *\n   * @param set The RRset that contains the NSEC.\n   * @param nsec The NSEC to check.\n   * @param qname The qname to check against.\n   * @return true if the NSEC proves the condition.\n   ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


