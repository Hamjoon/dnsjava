```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link Zone#findExactMatch(Name, int)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>Argument validation (null name, illegal type values).</li>
 *   <li>Successful look‑ups – the method must return a *copy* of the stored {@link RRset}.</li>
 *   <li>Lookup of non‑existent name/type – {@code null} must be returned.</li>
 *   <li>Copy semantics – the returned {@link RRset} must be independent from the original.</li>
 *   <li>Case‑insensitive name handling, distinct {@link Name} instances, and multiple types for the same name.</li>
 *   <li>Behaviour after removal and with empty RRsets.</li>
 *   <li>Boundary values for DNS type identifiers.</li>
 * </ul>
 *
 * The tests use real {@link Zone} instances; no mocking of internal data structures is required.
 */
@ExtendWith(MockitoExtension.class)
class ZoneFindExactMatchTest {

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------
    private Name zoneName;
    private Zone zone;

    @BeforeAll
    static void beforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void beforeEach() throws IOException {
        zoneName = Name.fromConstantString("example.");
        // Initialise an empty zone – records will be added per‑test.
        zone = new Zone(zoneName);
    }

    @AfterEach
    void afterEach() {
        // Nothing to clean up; each test gets a fresh Zone instance.
    }

    @AfterAll
    static void afterAll() {
        // No global resources to release.
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /** Creates an {@link ARecord} for the supplied name. */
    private static ARecord aRecord(Name name, String ip) throws IOException {
        return new ARecord(name, DClass.IN, 3600, InetAddress.getByName(ip));
    }

    /** Creates an {@link MXRecord} for the supplied name. */
    private static MXRecord mxRecord(Name name, int preference, String exchange) throws IOException {
        return new MXRecord(name, DClass.IN, 3600, preference, Name.fromConstantString(exchange));
    }

    /** Adds a list of records to the zone using {@link Zone#addRecord(Record)}. */
    private void addRecords(Record... records) {
        for (Record r : records) {
            zone.addRecord(r);
        }
    }

    /** Returns a fresh {@link Name} instance with the same textual representation as the argument. */
    private static Name freshName(String textual) throws IOException {
        return Name.fromConstantString(textual);
    }

    // -------------------------------------------------------------------------
    // Parameterized test data
    // -------------------------------------------------------------------------

    /** Data for the “null name”, “invalid type”, “non‑existent”, “case‑insensitive”, etc. */
    static Stream<Arguments> invalidArgumentProvider() {
        return Stream.of(
                // null name
                Arguments.of(null, Type.A, IllegalArgumentException.class, "name must not be null"),
                // negative type – Type.check throws InvalidTypeException (subclass of IllegalArgumentException)
                Arguments.of(Name.root, -1, InvalidTypeException.class, "type"),
                // type just above the valid range (65536) – also InvalidTypeException
                Arguments.of(Name.root, 65536, InvalidTypeException.class, "type")
        );
    }

    /** Data for successful look‑ups where a copy must be returned. */
    static Stream<Arguments> exactMatchProvider() throws IOException {
        Name host = Name.fromConstantString("host.example.");
        ARecord a = aRecord(host, "127.0.0.10");
        AAAARecord aaaa = new AAAARecord(host, DClass.IN, 3600,
                InetAddress.getByName("::1"));
        return Stream.of(
                // lower‑case name, same instance
                Arguments.of(host, Type.A, List.of(a)),
                // distinct Name instance with same textual representation
                Arguments.of(freshName("host.example."), Type.A, List.of(a)),
                // case‑insensitive lookup (upper‑case query)
                Arguments.of(Name.fromConstantString("HOST.EXAMPLE."), Type.A, List.of(a))
        );
    }

    /** Data for multiple types stored under the same name. */
    static Stream<Arguments> multiTypeProvider() throws IOException {
        Name multi = Name.fromConstantString("multi.example.");
        ARecord a = aRecord(multi, "127.0.0.11");
        MXRecord mx = mxRecord(multi, 10, "mail.example.");
        return Stream.of(
                Arguments.of(multi, Type.A, a),
                Arguments.of(multi, Type.MX, mx)
        );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] name={0}, type={1} → expect {2}")
    @MethodSource("invalidArgumentProvider")
    void findExactMatch_invalidArguments_throwsException(Name name, int type,
                                                          Class<? extends Throwable> expectedException,
                                                          String expectedMessageFragment) {
        assertThatThrownBy(() -> zone.findExactMatch(name, type))
                .isInstanceOf(expectedException)
                .hasMessageContaining(expectedMessageFragment);
    }

    @Test
    void findExactMatch_nonExistingName_returnsNull() throws IOException {
        Name unknown = Name.fromConstantString("doesnotexist.example.");
        RRset result = zone.findExactMatch(unknown, Type.A);
        assertThat(result).isNull();
    }

    @Test
    void findExactMatch_existingNameWrongType_returnsNull() throws IOException {
        Name host = Name.fromConstantString("host.example.");
        addRecords(aRecord(host, "127.0.0.12"));
        // Query for a type that is not present (TXT)
        RRset result = zone.findExactMatch(host, Type.TXT);
        assertThat(result).isNull();
    }

    @ParameterizedTest(name = "Exact match copy – name={0}, type={1}")
    @MethodSource("exactMatchProvider")
    void findExactMatch_existingRecord_returnsCopy(Name queryName, int type, List<Record> records) throws IOException {
        // Populate the zone with the supplied records
        addRecords(records.toArray(new Record[0]));

        // First call – obtain the original RRset (internal)
        RRset internal = zone.findExactMatch(queryName, type);
        assertThat(internal).isNotNull();

        // Second call – should return a *different* object but equal content
        RRset copy = zone.findExactMatch(queryName, type);
        assertThat(copy).isNotNull();
        assertThat(copy).isNotSame(internal);          // different reference
        assertThat(copy).isEqualTo(internal);           // same data

        // Mutate the copy and verify the zone's data stay unchanged
        Record extra = aRecord(Name.fromConstantString("extra.example."), "127.0.0.99");
        copy.addRR(extra);
        assertThat(copy).contains(extra);
        RRset afterMutation = zone.findExactMatch(queryName, type);
        assertThat(afterMutation).doesNotContain(extra);
    }

    @ParameterizedTest(name = "Multiple types – name={0}, type={1}")
    @MethodSource("multiTypeProvider")
    void findExactMatch_multipleTypes_returnCorrectRRset(Name name, int type, Record record) throws IOException {
        // Add both records (A and MX) for the same name
        addRecords(record);
        // Also add the counterpart type so the zone contains *both* types
        if (type == Type.A) {
            addRecords(mxRecord(name, 20, "mail.example."));
        } else {
            addRecords(aRecord(name, "127.0.0.13"));
        }

        RRset result = zone.findExactMatch(name, type);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result).contains(record);
    }

    @Test
    void findExactMatch_noMatchWhenNameExistsButDifferentType() throws IOException {
        Name host = Name.fromConstantString("host.example.");
        addRecords(aRecord(host, "127.0.0.14")); // type A only
        RRset result = zone.findExactMatch(host, Type.MX); // ask for MX
        assertThat(result).isNull();
    }

    @Test
    void findExactMatch_afterRemoval_returnsNull() throws IOException {
        Name temp = Name.fromConstantString("temp.example.");
        ARecord a = aRecord(temp, "127.0.0.15");
        addRecords(a);

        RRset beforeRemoval = zone.findExactMatch(temp, Type.A);
        assertThat(beforeRemoval).isNotNull();

        zone.removeRRset(temp, Type.A);
        RRset afterRemoval = zone.findExactMatch(temp, Type.A);
        assertThat(afterRemoval).isNull();
    }

    @Test
    void findExactMatch_emptyRRset_returnsCopyWithZeroRecords() throws IOException {
        Name emptyName = Name.fromConstantString("empty.example.");
        // Create an empty RRset (no records) and add it directly.
        RRset emptyRRset = new RRset();
        emptyRRset.setName(emptyName);
        emptyRRset.setType(Type.A);
        zone.addRRset(emptyRRset);

        RRset result = zone.findExactMatch(emptyName, Type.A);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(emptyName);
        assertThat(result.getType()).isEqualTo(Type.A);
        assertThat(result.size()).isZero(); // zero records
        assertThat(result).isNotSame(emptyRRset);
    }

    @Test
    void findExactMatch_maxValidType_returnsNullWhenAbsent() throws IOException {
        Name maxName = Name.fromConstantString("maxtype.example.");
        // No record of type 65535 is added.
        RRset result = zone.findExactMatch(maxName, 65535);
        assertThat(result).isNull();
    }

    // -------------------------------------------------------------------------
    // Additional explicit tests (mirroring the original test class)
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
    void findExactMatch_existingRecord_returnsCopy_unchangedAfterMutation() throws IOException {
        // Populate zone with a known A record
        Name name = Name.fromConstantString("test.example.");
        ARecord a = aRecord(name, "127.0.0.16");
        addRecords(a);

        RRset original = zone.findExactMatch(name, Type.A);
        assertThat(original).isNotNull();

        // Obtain a copy and mutate it
        RRset copy = zone.findExactMatch(name, Type.A);
        Record extra = aRecord(Name.fromConstantString("extra.example."), "127.0.0.99");
        copy.addRR(extra);
        assertThat(copy).contains(extra);

        // Original in the zone must stay unchanged
        RRset afterMutation = zone.findExactMatch(name, Type.A);
        assertThat(afterMutation).doesNotContain(extra);
    }

    @Test
    void findExactMatch_nonExistingName_returnsNull() {
        Name unknown = Name.fromConstantString("doesnotexist.example.");
        RRset result = zone.findExactMatch(unknown, Type.A);
        assertThat(result).isNull();
    }

    @Test
    void findExactMatch_existingNameWrongType_returnsNull() throws IOException {
        Name name = Name.fromConstantString("test.example.");
        addRecords(aRecord(name, "127.0.0.17"));
        RRset result = zone.findExactMatch(name, Type.TXT);
        assertThat(result).isNull();
    }
}
```