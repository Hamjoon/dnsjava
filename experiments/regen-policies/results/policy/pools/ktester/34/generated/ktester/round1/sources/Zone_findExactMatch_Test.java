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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Zone_findExactMatch_Test {

    // -------------------------------------------------------------------------
    private Name zoneName;

    private SOARecord soa;

    private NSRecord ns1;

    private NSRecord ns2;

    private ARecord aTest;

    private AAAARecord aaaaTest;

    private ARecord aUnique;

    private Zone zone;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        zoneName = Name.fromConstantString("example.");
        soa = new SOARecord(zoneName, DClass.IN, 3600L, Name.fromConstantString("ns.example."), new Name("hostadmin", zoneName), 1, 21600L, 7200L, 2160000L, 3600L);
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6 = InetAddress.getByName("::1");
        aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600, localhost4);
        aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600, localhost6);
        aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        // Initialise the zone with the mandatory SOA/NS records and a few test records.
        Record[] records = new Record[] { soa, ns1, ns2, aTest, aaaaTest, aUnique };
        zone = new Zone(zoneName, records);
        // Do NOT re‑initialize the zone – the empty constructor would fail validation
        // because a zone must contain at least an SOA and an NS record.
    }

    /**
     * Creates an {@link ARecord} for the supplied name.
     */
    private static ARecord aRecord(Name name, String ip) throws IOException {
        return new ARecord(name, DClass.IN, 3600, InetAddress.getByName(ip));
    }

    /**
     * Creates an {@link MXRecord} for the supplied name.
     */
    private static MXRecord mxRecord(Name name, int preference, String exchange) throws IOException {
        return new MXRecord(name, DClass.IN, 3600, preference, Name.fromConstantString(exchange));
    }

    /**
     * Adds a list of records to the zone using {@link Zone#addRecord(Record)}.
     */
    private void addRecords(Record... records) {
        for (Record r : records) {
            zone.addRecord(r);
        }
    }

    /**
     * Returns a fresh {@link Name} instance with the same textual representation as the argument.
     */
    private static Name freshName(String textual) throws IOException {
        return Name.fromConstantString(textual);
    }

    /**
     * Data for the “null name”, “invalid type”, “non‑existent”, “case‑insensitive”, etc.
     */
    static Stream<Arguments> invalidArgumentProvider() {
        return Stream.of(Arguments.of(null, Type.A, IllegalArgumentException.class, "name must not be null"), Arguments.of(Name.root, -1, InvalidTypeException.class, "type"), Arguments.of(Name.root, 65536, InvalidTypeException.class, "type"));
    }

    /**
     * Data for successful look‑ups where a copy must be returned.
     */
    static Stream<Arguments> exactMatchProvider() throws IOException {
        Name host = Name.fromConstantString("host.example.");
        ARecord a = aRecord(host, "127.0.0.10");
        AAAARecord aaaa = new AAAARecord(host, DClass.IN, 3600, InetAddress.getByName("::1"));
        return Stream.of(Arguments.of(host, Type.A, List.of(a)), Arguments.of(freshName("host.example."), Type.A, List.of(a)), Arguments.of(Name.fromConstantString("HOST.EXAMPLE."), Type.A, List.of(a)));
    }

    /**
     * Data for multiple types stored under the same name.
     */
    static Stream<Arguments> multiTypeProvider() throws IOException {
        Name multi = Name.fromConstantString("multi.example.");
        ARecord a = aRecord(multi, "127.0.0.11");
        MXRecord mx = mxRecord(multi, 10, "mail.example.");
        return Stream.of(Arguments.of(multi, Type.A, a), Arguments.of(multi, Type.MX, mx));
    }

    /* --------------------------------------------------------------------- *
     * Helper method to set private fields of RRset via reflection.
     * --------------------------------------------------------------------- */
    private static void setRRsetNameAndType(RRset rrset, Name name, int type) throws Exception {
        // Name field – try the obvious name first, then fallback to known alternatives.
        Field nameField;
        try {
            nameField = RRset.class.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            // dnsjava older versions
            nameField = RRset.class.getDeclaredField("rrsetName");
        }
        nameField.setAccessible(true);
        nameField.set(rrset, name);
        // Type field – similar fallback logic.
        Field typeField;
        try {
            typeField = RRset.class.getDeclaredField("type");
        } catch (NoSuchFieldException e) {
            // dnsjava older versions
            typeField = RRset.class.getDeclaredField("rrsetType");
        }
        typeField.setAccessible(true);
        typeField.setInt(rrset, type);
    }

    @Test
    void findExactMatch_nullName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> zone.findExactMatch(null, Type.A)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("null");
    }

    @Test
    void findExactMatch_invalidType_throwsInvalidTypeException() {
        // Type.check throws InvalidTypeException for values that are not defined
        assertThatThrownBy(() -> zone.findExactMatch(Name.root, -1)).isInstanceOf(InvalidTypeException.class).hasMessageContaining("type");
    }

    @Test
    void findExactMatch_existingRecord_returnsCopy() throws Exception {
        // The zone contains an A record for "test.example."
        RRset original = zone.findExactMatch(aTest.getName(), Type.A);
        assertThat(original).isNotNull();
        // The method must return a *new* RRset instance (copy) – not the internal one
        RRset result = zone.findExactMatch(aTest.getName(), Type.A);
        assertThat(result).isNotNull();
        // copy, not same reference
        assertThat((Object) result).isNotSameAs(original);
        // equal content
        assertThat(result).isEqualTo(original);
        // Mutating the returned copy must not affect the zone.
        // Use a record with the SAME name & type (required by RRset semantics).
        Record extra = new ARecord(aTest.getName(), DClass.IN, 3600, InetAddress.getByName("127.0.0.99"));
        result.addRR(extra);
        assertThat(result).contains(extra);
        // The zone still sees only the original A record.
        RRset afterMutation = zone.findExactMatch(aTest.getName(), Type.A);
        assertThat(afterMutation).doesNotContain(extra);
    }

    @Test
    void findExactMatch_nonExistingName_returnsNull() {
        Name unknown = Name.fromConstantString("doesnotexist.example.");
        RRset result = zone.findExactMatch(unknown, Type.A);
        assertThat(result).isNull();
    }

    @Test
    void findExactMatch_existingNameWrongType_returnsNull() throws Exception {
        Name host = Name.fromConstantString("host.example.");
        addRecords(aRecord(host, "127.0.0.12"));
        // Query for a type that is not present (TXT)
        RRset result = zone.findExactMatch(host, Type.TXT);
        assertThat(result).isNull();
    }

    @ParameterizedTest(name = "[{index}] name={0}, type={1} → expect {2}")
    @MethodSource("invalidArgumentProvider")
    void findExactMatch_invalidArguments_throwsException(Name name, int type, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        assertThatThrownBy(() -> zone.findExactMatch(name, type)).isInstanceOf(expectedException).hasMessageContaining(expectedMessageFragment);
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
        // type A only
        addRecords(aRecord(host, "127.0.0.14"));
        // ask for MX
        RRset result = zone.findExactMatch(host, Type.MX);
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
    void findExactMatch_emptyRRset_returnsCopyWithZeroRecords() throws Exception {
        Name emptyName = Name.fromConstantString("empty.example.");
        // Create an empty RRset (no records) and add it directly.
        RRset emptyRRset = new RRset();
        // Use reflection to set the private fields 'name' and 'type'
        setRRsetNameAndType(emptyRRset, emptyName, Type.A);
        zone.addRRset(emptyRRset);
        RRset result = zone.findExactMatch(emptyName, Type.A);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(emptyName);
        assertThat(result.getType()).isEqualTo(Type.A);
        // zero records
        assertThat(result.size()).isZero();
        assertThat((Object) result).isNotSameAs(emptyRRset);
    }

    @Test
    void findExactMatch_maxValidType_returnsNullWhenAbsent() throws IOException {
        Name maxName = Name.fromConstantString("maxtype.example.");
        // No record of type 65535 is added.
        RRset result = zone.findExactMatch(maxName, 65535);
        assertThat(result).isNull();
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
}
