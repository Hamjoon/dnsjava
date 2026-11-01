
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
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Iterator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Zone_addRRset_Test {

    private static final String ZONE = "example.";

    private Name zoneName;

    private SOARecord soaRecord;

    private NSRecord nsRecord;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a minimal valid zone (origin + SOA + at least one NS)
        zoneName = Name.fromConstantString(ZONE);
        soaRecord = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster", zoneName), 1, 10800L, 3600L, 604800L, 3600L);
        nsRecord = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns.example."));
        // Initialise the zone with the mandatory records.
        zone = new Zone(zoneName, new Record[] { soaRecord, nsRecord });
    }

    @Test
    void addRRset_NullArgument_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRRset(null), "addRRset(null) should throw IllegalArgumentException");
        assertTrue(ex.getMessage().contains("rrset must not be null"));
    }

    @Test
    void addRRset_SOAOwnerMismatch_Throws() throws IOException {
        // SOA with a different owner name (not the zone origin)
        Name otherName = Name.fromConstantString("other.example.");
        SOARecord otherSOA = new SOARecord(otherName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster", zoneName), 1, 10800L, 3600L, 604800L, 3600L);
        RRset rrset = new RRset(otherSOA);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRRset(rrset), "SOA owner not matching zone origin must throw");
        assertTrue(ex.getMessage().contains("SOA owner"), "Exception message should mention SOA owner mismatch");
    }

    @Test
    void addRRset_SOAInvalidSize_Throws() throws IOException {
        // Create two SOA records with the same origin – this violates the “exactly 1 SOA” rule.
        SOARecord soa2 = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostmaster2", zoneName), 2, 10800L, 3600L, 604800L, 3600L);
        RRset rrset = new RRset(soaRecord, soa2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRRset(rrset), "Adding an RRset with more than one SOA should fail");
        assertTrue(ex.getMessage().contains("exactly 1 SOA"), "Exception message should mention the exact‑one‑SOA requirement");
    }

    @Test
    void addRRset_NameNotSubdomain_Throws() throws IOException {
        // Record whose name is not a sub‑domain of the zone origin.
        Name externalName = Name.fromConstantString("outside.example.com.");
        ARecord aRecord = new ARecord(externalName, DClass.IN, 300L, InetAddress.getByName("1.2.3.4"));
        RRset rrset = new RRset(aRecord);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRRset(rrset), "Adding a record outside the zone should throw");
        assertTrue(ex.getMessage().contains("is absolute and not a subdomain"), "Exception message should mention sub‑domain validation");
    }

    @Test
    void addRRset_RegularRecord_Succeeds() throws IOException {
        // Add a new A record under the zone.
        Name aName = new Name("newhost", zoneName);
        ARecord aRecord = new ARecord(aName, DClass.IN, 300L, InetAddress.getByName("10.0.0.1"));
        RRset rrset = new RRset(aRecord);
        assertDoesNotThrow(() -> zone.addRRset(rrset), "addRRset should succeed for a valid RRset");
        // Verify the record can be retrieved.
        RRset fetched = zone.findExactMatch(aName, Type.A);
        assertNotNull(fetched, "The added RRset should be findable");
        assertEquals(1, fetched.size(), "Exactly one record should be stored");
        assertEquals(aRecord, fetched.first(), "Stored record must equal the added one");
    }

    @Test
    void addRRset_NewSOA_UpdatesZoneSOA() throws IOException {
        // Build a brand‑new SOA record (same owner as the zone origin).
        SOARecord newSOA = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns2.example."), new Name("hostmaster", zoneName), 42, 10800L, 3600L, 604800L, 3600L);
        RRset rrset = new RRset(newSOA);
        // Adding the new SOA should replace the internal SOA reference.
        assertDoesNotThrow(() -> zone.addRRset(rrset), "Adding a valid SOA RRset should not throw");
        assertEquals(newSOA, zone.getSOA(), "Zone SOA should be updated to the newly added record");
    }

    @Test
    void addRRset_SOAZeroRecords_Throws() {
        // Mock an RRset that pretends to be a SOA set but contains no records.
        RRset mockRrset = mock(RRset.class);
        when(mockRrset.getName()).thenReturn(zoneName);
        when(mockRrset.getType()).thenReturn(Type.SOA);
        when(mockRrset.size()).thenReturn(0);
        // first() will not be called because size check fails first.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zone.addRRset(mockRrset), "SOA RRset with zero records must be rejected");
        assertTrue(ex.getMessage().contains("exactly 1 SOA"), "Exception message should mention the exact‑one‑SOA requirement");
    }

    @Test
    void addRRset_NonSOANameEqualsOrigin_Succeeds() throws IOException {
        // NS record whose owner name is exactly the zone origin (non‑SOA).
        NSRecord nsAtOrigin = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        RRset rrset = new RRset(nsAtOrigin);
        assertDoesNotThrow(() -> zone.addRRset(rrset), "Non‑SOA RRset at zone origin should be accepted");
        RRset fetched = zone.findExactMatch(zoneName, Type.NS);
        assertNotNull(fetched, "NS RRset at origin should be findable");
        assertTrue(fetched.contains(nsAtOrigin), "Stored NS record must be present");
        // SOA must stay unchanged.
        assertEquals(soaRecord, zone.getSOA(), "SOA should remain unchanged after adding non‑SOA RRset");
    }

    @Test
    void addRRset_MergeSameNameAndType() throws IOException {
        // First A record.
        Name host = new Name("host", zoneName);
        ARecord a1 = new ARecord(host, DClass.IN, 300L, InetAddress.getByName("192.0.2.1"));
        zone.addRRset(new RRset(a1));
        // Second A record with the same name/type.
        ARecord a2 = new ARecord(host, DClass.IN, 300L, InetAddress.getByName("192.0.2.2"));
        zone.addRRset(new RRset(a2));
        RRset fetched = zone.findExactMatch(host, Type.A);
        assertNotNull(fetched, "Merged RRset should be present");
        assertEquals(2, fetched.size(), "RRset should contain both A records after merging");
        assertTrue(fetched.contains(a1), "First A record must be present");
        assertTrue(fetched.contains(a2), "Second A record must be present");
    }

    @Test
    void addRRset_ReplaceExistingSOA() throws IOException {
        // First SOA (already present from @BeforeEach) has serial 1.
        assertEquals(1, soaRecord.getSerial(), "Initial SOA serial should be 1");
        // New SOA with a higher serial.
        SOARecord newerSOA = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns3.example."), new Name("hostmaster", zoneName), 2, 10800L, 3600L, 604800L, 3600L);
        zone.addRRset(new RRset(newerSOA));
        assertEquals(newerSOA, zone.getSOA(), "Zone SOA should be replaced with the newer record");
    }

    @Test
    void addRRset_RRSIGType_TreatedAsNormal() {
        // Mock a simple RRSIG Record (the concrete class is heavy to construct).
        Record mockRRSIG = mock(Record.class);
        when(mockRRSIG.getName()).thenReturn(new Name("secure", zoneName));
        when(mockRRSIG.getType()).thenReturn(Type.RRSIG);
        when(mockRRSIG.getDClass()).thenReturn(DClass.IN);
        // No need to stub other methods for this test.
        RRset mockRrset = mock(RRset.class);
        when(mockRrset.getName()).thenReturn(new Name("secure", zoneName));
        when(mockRrset.getType()).thenReturn(Type.RRSIG);
        when(mockRrset.size()).thenReturn(1);
        when(mockRrset.first()).thenReturn(mockRRSIG);
        assertDoesNotThrow(() -> zone.addRRset(mockRrset), "RRSIG RRset should be accepted like any other type");
        // Verify that the record is now reachable via findExactMatch.
        RRset fetched = zone.findExactMatch(new Name("secure", zoneName), Type.RRSIG);
        assertNotNull(fetched, "RRSIG RRset should be stored");
        assertEquals(1, fetched.size(), "Stored RRSIG RRset should contain exactly one record");
    }

    @Test
    void addRRset_RRsetWithNullName_ThrowsNPE() {
        // Mock an RRset where getName() returns null.
        RRset mockRrset = mock(RRset.class);
        when(mockRrset.getName()).thenReturn(null);
        when(mockRrset.getType()).thenReturn(Type.A);
        when(mockRrset.size()).thenReturn(1);
        when(mockRrset.first()).thenReturn(mock(Record.class));
        assertThrows(NullPointerException.class, () -> zone.addRRset(mockRrset), "RRset with a null name should cause a NullPointerException");
    }

    @Test
    void addRRset_ZoneWithNullOrigin_ThrowsNPE() throws Exception {
        // Use reflection to set the private 'origin' field to null after construction.
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        originField.set(zone, null);
        // Prepare a normal A record RRset.
        Name aName = new Name("test", Name.fromConstantString(ZONE));
        ARecord aRecord = new ARecord(aName, DClass.IN, 300L, InetAddress.getByName("10.0.0.2"));
        RRset rrset = new RRset(aRecord);
        assertThrows(NullPointerException.class, () -> zone.addRRset(rrset), "Adding any RRset to a Zone with a null origin should throw NullPointerException");
    }

    @Test
    void addRRset_SOAContainingNonSOARecord_ThrowsClassCast() {
        // Mock an RRset that claims to be SOA but contains an ARecord.
        Record aRecord = mock(ARecord.class);
        when(aRecord.getName()).thenReturn(zoneName);
        when(aRecord.getType()).thenReturn(Type.A);
        when(aRecord.getDClass()).thenReturn(DClass.IN);
        RRset mockRrset = mock(RRset.class);
        when(mockRrset.getName()).thenReturn(zoneName);
        when(mockRrset.getType()).thenReturn(Type.SOA);
        when(mockRrset.size()).thenReturn(1);
        // Not an SOARecord!
        when(mockRrset.first()).thenReturn(aRecord);
        assertThrows(ClassCastException.class, () -> zone.addRRset(mockRrset), "SOA RRset containing a non‑SOA record should cause a ClassCastException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Zone_addRRset_Test.java:116: error: cannot find symbol
        assertTrue(fetched.contains(nsAtOrigin), "Stored NS record must be present");
                          ^
  symbol:   method contains(NSRecord)
  location: variable fetched of type RRset
src/test/java/org/xbill/DNS/Zone_addRRset_Test.java:133: error: cannot find symbol
        assertTrue(fetched.contains(a1), "First A record must be present");
                          ^
  symbol:   method contains(ARecord)
  location: variable fetched of type RRset
src/test/java/org/xbill/DNS/Zone_addRRset_Test.java:134: error: cannot find symbol
        assertTrue(fetched.contains(a2), "Second A record must be present");
                          ^
  symbol:   method contains(ARecord)
  location: variable fetched of type RRset
3 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Zone': '\n * A DNS zone. This encapsulates all data related to a zone, and provides convenient lookup methods.\n * A zone always contains a {@link SOARecord} and at least one {@link NSRecord}.\n *\n * @implNote This class uses a {@link java.util.concurrent.locks.ReadWriteLock} to ensure access and\n *     manipulation is safe from multiple threads. Iterators are "weakly consistent" (see the\n *     package info from {@code java.util.concurrent}).\n * @author Brian Wellington\n ', 'api document of method addRRset(RRset)': '\n   * Adds an RRset to the zone.\n   *\n   * @implNote An existing {@link RRset} of the same {@link Name} and {@link Type} is\n   *     <b>replaced</b>.\n   * @param rrset The RRset to add.\n   * @see RRset\n   * @throws IllegalArgumentException if {@code rrset} is {@code null} or if the rrset name is not a\n   *     {@link Name#subdomain(Name) subdomain} of the zone origin (or, in case of a SOA, is not\n   *     equal to the zone origin).\n   ', 'dependent classes': 'class `org.xbill.DNS.RRset`:\n\t\nclass `org.xbill.DNS.Type`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Type.SOA;\n\t\nclass `org.xbill.DNS.Zone`:\n\tdependent fields:\n\t\torg.xbill.DNS.SOARecord org.xbill.DNS.Zone.SOA;\n\t\torg.xbill.DNS.Name org.xbill.DNS.Zone.origin;\n\trelated functions:\n\t\tmethod `private void validate()` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`;\n\t\tmethod `public void removeRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: \n   * Removes a record from the zone.\n   *\n   * @param r The record to remove.\n   * @throws IllegalArgumentException if {@code r} is {@code null}, if the record to remove is the\n   *     {@link SOARecord} or the last {@link NSRecord}.\n   ;\n\t\tmethod `private void fromXFR(ZoneTransferIn)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`;\n\t\tmethod `public SetResponse findRecords(Name, int)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: \n   * Looks up Records in the zone. The answer can be a {@code CNAME} instead of the actual requested\n   * type and wildcards are expanded.\n   *\n   * @param name The name to look up\n   * @param type The type to look up\n   * @return A SetResponse object\n   * @throws IllegalArgumentException if {@code name} is {@code null}.\n   * @throws InvalidTypeException if the specified {@code type} is invalid.\n   * @see SetResponse\n   ;\n\t\tmethod `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`;\n\t\tmethod `public void addRecord(T)` returns `void`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: \n   * Adds a record to the zone. If there is an existing {@link RRset} of the same {@link Name} and\n   * {@link Type}, the record is <b>added</b> to this set.\n   *\n   * @param r The record to add.\n   * @throws IllegalArgumentException if {@code name} is {@code null} or if the rrset name does not\n   *     match the zone origin.\n   ;\nclass `org.xbill.DNS.Record`:\n\trelated functions:\n\t\tmethod `public boolean sameRRset(RRset)` returns `boolean`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: \n   * Determines if this Record could be part of the passed RRset. This compares the name, type, and\n   * class of the Record and the set.\n   *\n   * @since 3.6\n   ;\nclass `org.xbill.DNS.dnssec.ValUtils`:\n\trelated functions:\n\t\tmethod `public static boolean nsecProvesNameError(SRRset, NSECRecord, Name)` returns `boolean`, related with `org.xbill.DNS.Zone.addRRset(RRset)`, api document: \n   * Determine if the given NSEC proves a NameError (NXDOMAIN) for a given qname.\n   *\n   * @param set The RRset that contains the NSEC.\n   * @param nsec The NSEC to check.\n   * @param qname The qname to check against.\n   * @return true if the NSEC proves the condition.\n   ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


