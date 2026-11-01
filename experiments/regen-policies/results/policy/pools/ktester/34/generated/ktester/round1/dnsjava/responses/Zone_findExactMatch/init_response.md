```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#findExactMatch(Name, int)}.
 *
 * The tests cover:
 * <ul>
 *   <li>validation of arguments (null name, invalid type)</li>
 *   <li>successful lookup – the method must return a copy of the stored RRset</li>
 *   <li>lookup of a non‑existent name/type – the method must return {@code null}</li>
 *   <li>that the returned copy is independent from the original RRset</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Zone_findExactMatch_Test {

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------
    private Name zoneName;
    private SOARecord soa;
    private NSRecord ns1;
    private NSRecord ns2;
    private ARecord aTest;
    private AAAARecord aaaaTest;
    private ARecord aUnique;
    private Zone zone;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        zoneName = Name.fromConstantString("example.");

        soa = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);

        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6 = InetAddress.getByName("::1");

        aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
        aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6);
        aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600,
                InetAddress.getByName("127.0.0.3"));

        Record[] records = new Record[] {
                soa, ns1, ns2, aTest, aaaaTest, aUnique
        };
        zone = new Zone(zoneName, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the Zone instance is garbage‑collected
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void findExactMatch_nullName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> zone.findExactMatch(null, Type.A))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void findExactMatch_invalidType_throwsInvalidTypeException() {
        // Type.check throws InvalidTypeException for values that are not defined
        assertThatThrownBy(() -> zone.findExactMatch(Name.root, -1))
                .isInstanceOf(InvalidTypeException.class)
                .hasMessageContaining("type");
    }

    @Test
    void findExactMatch_existingRecord_returnsCopy() {
        // The zone contains an A record for "test.example."
        RRset original = zone.findExactMatch(aTest.getName(), Type.A);
        assertThat(original).isNotNull();

        // The method must return a *new* RRset instance (copy) – not the internal one
        RRset result = zone.findExactMatch(aTest.getName(), Type.A);
        assertThat(result).isNotNull();
        assertThat(result).isNotSame(original);               // copy, not same reference
        assertThat(result).isEqualTo(original);              // equal content

        // Mutating the returned copy must not affect the zone
        Record extra = new ARecord(new Name("extra", zoneName), DClass.IN, 3600,
                InetAddress.getByName("127.0.0.9"));
        result.addRR(extra);
        assertThat(result).contains(extra);
        // The zone still sees only the original A record
        RRset afterMutation = zone.findExactMatch(aTest.getName(), Type.A);
        assertThat(afterMutation).doesNotContain(extra);
    }

    @Test
    void findExactMatch_nonExistingName_returnsNull() {
        Name unknown = new Name("doesnotexist", zoneName);
        RRset result = zone.findExactMatch(unknown, Type.A);
        assertThat(result).isNull();
    }

    @Test
    void findExactMatch_existingNameWrongType_returnsNull() {
        // "test.example." exists, but we ask for a TXT type which is not present
        RRset result = zone.findExactMatch(aTest.getName(), Type.TXT);
        assertThat(result).isNull();
    }
}
```