
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class Zone_findRecords_Test {

    /**
     * Common objects used by all tests.
     */
    private Name zoneName;

    private SOARecord soa;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aUnique;

    private ARecord aTest;

    private AAAARecord aaaaTest;

    private ARecord aWild;

    private TXTRecord txtWild;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a minimal but realistic zone (the same data used in the original
        // ZoneTest class). All records are absolute and belong to the zone
        // "example.".
        zoneName = Name.fromConstantString("example.");
        soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("nameserver."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
        aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6a);
        aWild = new ARecord(new Name("*.wild", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        txtWild = new TXTRecord(new Name("*.wild", zoneName), DClass.IN, 3600, "sometext");
        Record[] records = { soa, ns1, ns2, aUnique, aTest, aaaaTest, aWild, txtWild };
        zone = new Zone(zoneName, records);
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> zone.findRecords(null, Type.A), "name must not be null");
    }

    @Test
    void findRecords_invalidType_throwsInvalidTypeException() {
        // an arbitrary type value that is not defined by the DNS protocol
        int invalidType = 9999;
        assertThrows(InvalidTypeException.class, () -> zone.findRecords(Name.fromConstantString("example.com."), invalidType));
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_nameNotInZone_returnsNxDomain() {
        Name external = Name.fromConstantString("outside.example.");
        SetResponse response = zone.findRecords(external, Type.A);
        assertNotNull(response);
        assertTrue(response.isNXDOMAIN(), "The response should indicate NXDOMAIN for a name outside the zone");
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_exactMatch_returnsSuccess() {
        SetResponse response = zone.findRecords(aTest.getName(), Type.A);
        assertNotNull(response);
        // In the current dnsjava API a successful lookup is indicated by
        // !isNXDOMAIN() and !isCNAME() etc.  The most direct way is to check that
        // the response is not NXDOMAIN.
        assertFalse(response.isNXDOMAIN(), "The response for an exact match should not be NXDOMAIN");
        // Retrieve the RRset directly from the zone and verify it contains the
        // expected record.
        RRset rrset = zone.findExactMatch(aTest.getName(), Type.A);
        assertNotNull(rrset);
        assertTrue(rrset.contains(aTest), "The RRset returned by the zone should contain the queried A record");
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_wildcardMatch_returnsSuccess() throws IOException {
        Name query = Name.fromConstantString("foo.wild.example.");
        SetResponse response = zone.findRecords(query, Type.A);
        assertNotNull(response);
        assertFalse(response.isNXDOMAIN(), "Wildcard lookup should not result in NXDOMAIN");
        // The wildcard A record should be returned.
        RRset rrset = zone.findExactMatch(Name.fromConstantString("*.wild.example."), Type.A);
        assertNotNull(rrset);
        assertTrue(rrset.contains(aWild), "The RRset must contain the wildcard A record");
    }

    // ----------------------------------------------------------------------
    @Test
    void findRecords_zoneWithNullOrigin_throwsNullPointerException() throws Exception {
        // Use reflection to set the private 'origin' field to null after construction.
        Field originField = Zone.class.getDeclaredField("origin");
        originField.setAccessible(true);
        originField.set(zone, null);
        // The call to name.subdomain(origin) inside findRecords will now NPE.
        Name query = Name.fromConstantString("sub.example.com.");
        assertThrows(NullPointerException.class, () -> zone.findRecords(query, Type.A));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Zone_findRecords_Test.java:99: error: cannot find symbol
        assertTrue(rrset.contains(aTest), "The RRset returned by the zone should contain the queried A record");
                        ^
  symbol:   method contains(ARecord)
  location: variable rrset of type RRset
src/test/java/org/xbill/DNS/Zone_findRecords_Test.java:112: error: cannot find symbol
        assertTrue(rrset.contains(aWild), "The RRset must contain the wildcard A record");
                        ^
  symbol:   method contains(ARecord)
  location: variable rrset of type RRset
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Zone': '\n * A DNS zone. This encapsulates all data related to a zone, and provides convenient lookup methods.\n * A zone always contains a {@link SOARecord} and at least one {@link NSRecord}.\n *\n * @implNote This class uses a {@link java.util.concurrent.locks.ReadWriteLock} to ensure access and\n *     manipulation is safe from multiple threads. Iterators are "weakly consistent" (see the\n *     package info from {@code java.util.concurrent}).\n * @author Brian Wellington\n ', 'api document of method findRecords(Name, int)': '\n   * Looks up Records in the zone. The answer can be a {@code CNAME} instead of the actual requested\n   * type and wildcards are expanded.\n   *\n   * @param name The name to look up\n   * @param type The type to look up\n   * @return A SetResponse object\n   * @throws IllegalArgumentException if {@code name} is {@code null}.\n   * @throws InvalidTypeException if the specified {@code type} is invalid.\n   * @see SetResponse\n   ', 'return type': 'org.xbill.DNS.SetResponse', 'dependent classes': 'class `org.xbill.DNS.Name`:\n\t\nclass `org.xbill.DNS.SetResponseType`:\n\tdependent fields:\n\t\torg.xbill.DNS.SetResponseType org.xbill.DNS.SetResponseType.NXDOMAIN;\n\t\nclass `org.xbill.DNS.Zone`:\n\tdependent fields:\n\t\torg.xbill.DNS.Name org.xbill.DNS.Zone.origin;\n\trelated functions:\n\t\tmethod `public void addRRset(RRset)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: \n   * Adds an RRset to the zone.\n   *\n   * @implNote An existing {@link RRset} of the same {@link Name} and {@link Type} is\n   *     <b>replaced</b>.\n   * @param rrset The RRset to add.\n   * @see RRset\n   * @throws IllegalArgumentException if {@code rrset} is {@code null} or if the rrset name is not a\n   *     {@link Name#subdomain(Name) subdomain} of the zone origin (or, in case of a SOA, is not\n   *     equal to the zone origin).\n   ;\n\t\tmethod `public void removeRRset(Name, int)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: \n   * Removes an RRset from the zone.\n   *\n   * @param name The name to remove.\n   * @param type The type to remove.\n   * @throws IllegalArgumentException if {@code name} is {@code null}.\n   * @throws InvalidTypeException if the specified {@code type} is invalid or {@link Type#SOA} or\n   *     {@link Type#NS}.\n   * @see RRset\n   * @since 3.6\n   ;\n\t\tmethod `public String toMasterFile()` returns `java.lang.String`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: \n   * Returns the contents of the zone in master file format.\n   *\n   * @see Master\n   ;\n\t\tmethod `public void addRecord(T)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: \n   * Adds a record to the zone. If there is an existing {@link RRset} of the same {@link Name} and\n   * {@link Type}, the record is <b>added</b> to this set.\n   *\n   * @param r The record to add.\n   * @throws IllegalArgumentException if {@code name} is {@code null} or if the rrset name does not\n   *     match the zone origin.\n   ;\n\t\tmethod `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`;\n\t\tmethod `private void fromXFR(ZoneTransferIn)` returns `void`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`;\n\t\tmethod `public RRset findExactMatch(Name, int)` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document: \n   * Looks up Records in the zone, finding exact matches only.\n   *\n   * @param name The name to look up\n   * @param type The type to look up\n   * @return The matching RRset or {@code null} if no exact match is found.\n   * @throws IllegalArgumentException if {@code name} is {@code null}.\n   * @throws InvalidTypeException if the specified {@code type} is invalid.\n   * @see RRset\n   ;\nclass `org.xbill.DNS.SetResponse`:\n\trelated functions:\n\t\tmethod ` static SetResponse ofType(SetResponseType, RRset, boolean)` returns `org.xbill.DNS.SetResponse`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`;\n\t\tmethod `public boolean isNXDOMAIN()` returns `boolean`, related with `org.xbill.DNS.Zone.findRecords(Name, int)`, api document:  Is the answer to the query that the name does not exist? ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


