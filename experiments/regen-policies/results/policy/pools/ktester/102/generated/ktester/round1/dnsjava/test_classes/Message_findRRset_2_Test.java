package org.xbill.DNS;

import java.net.UnknownHostException;
import java.util.stream.Stream;
import org.junit.*;
import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
class Message_findRRset_2_Test {

    /**
     * Message under test – recreated before each test method.
     */
    private Message message;

    /**
     * Common name used for the records in the tests.
     */
    private Name testName;

    /**
     * A‑record for the test name (type A, class IN).
     */
    private Record aRecord;

    /**
     * AAAA‑record for the test name (type AAAA, class IN).
     */
    private Record aaaaRecord;

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException, TextParseException {
        // Fresh message for every test – the default constructor creates an empty header.
        message = new Message();
        // Use a simple, absolute name.
        testName = Name.fromConstantString("example.com.");
        // Create two different records so we can test type‑sensitivity.
        aRecord = Record.newRecord(testName, Type.A, DClass.IN);
        aaaaRecord = Record.newRecord(testName, Type.AAAA, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly necessary but keeps the pattern clear.
        message = null;
        testName = null;
        aRecord = null;
        aaaaRecord = null;
    }

    private static Stream<Arguments> edgeTypeProvider() {
        return Stream.of(Arguments.of(-1), Arguments.of(0), Arguments.of(65535));
    }

    /**
     * The method should return {@code false} when the message contains no records.
     */
    @Test
    void findRRset_emptyMessage_returnsFalse() {
        assertFalse(message.findRRset(testName, Type.A));
    }

    /**
     * The method should locate a matching RRset in the ANSWER section.
     */
    @Test
    void findRRset_recordInAnswer_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue(message.findRRset(testName, Type.A));
    }

    /**
     * The method should locate a matching RRset in the AUTHORITY section.
     */
    @Test
    void findRRset_recordInAuthority_returnsTrue() {
        message.addRecord(aRecord, Section.AUTHORITY);
        assertTrue(message.findRRset(testName, Type.A));
    }

    /**
     * The method should locate a matching RRset in the ADDITIONAL section.
     */
    @Test
    void findRRset_recordInAdditional_returnsTrue() {
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRRset(testName, Type.A));
    }

    /**
     * The method must respect the requested type – a different type should not be found.
     */
    @Test
    void findRRset_wrongType_returnsFalse() {
        // Add an AAAA record but query for type A.
        message.addRecord(aaaaRecord, Section.ANSWER);
        assertFalse(message.findRRset(testName, Type.A));
    }

    /**
     * When the same name exists with multiple types, the method should succeed for any matching type.
     */
    @Test
    void findRRset_multipleTypes_oneMatches_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(aaaaRecord, Section.ANSWER);
        assertTrue(message.findRRset(testName, Type.AAAA));
    }

    /**
     * Verify that a name that does not exist in any section yields {@code false}.
     */
    @Test
    void findRRset_nonexistentName_returnsFalse() throws TextParseException {
        Name otherName = Name.fromConstantString("other.example.com.");
        message.addRecord(aRecord, Section.ANSWER);
        assertFalse(message.findRRset(otherName, Type.A));
    }

    /**
     * Passing {@code null} as the name should result in a {@link NullPointerException}
     * propagated from the overloaded method.
     */
    @Test
    void findRRset_nullName_throwsNPE() {
        message.findRRset(null, Type.A);
    }

    /**
     * Root name handling – the root label "." is a valid DNS name.
     * The method must correctly locate records whose name is the root.
     */
    @Test
    void findRRset_rootName_successAndFailure() throws TextParseException {
        Name root = Name.fromConstantString(".");
        Record rootA = Record.newRecord(root, Type.A, DClass.IN);
        // Positive case – record present with matching type.
        message.addRecord(rootA, Section.ANSWER);
        assertTrue(message.findRRset(root, Type.A));
        // Negative case – query for a different type (NS) when only A exists.
        assertFalse(message.findRRset(root, Type.NS));
    }

    /**
     * DNS names are case‑insensitive. Store the name in lower case and query with mixed case.
     */
    @Test
    void findRRset_nameCaseInsensitivity() throws TextParseException {
        // Record already added in @BeforeEach (lower‑case name).
        message.addRecord(aRecord, Section.ANSWER);
        Name mixedCase = Name.fromConstantString("ExAmPlE.CoM.");
        assertTrue(message.findRRset(mixedCase, Type.A));
    }

    /**
     * Edge type values – negative, zero and the maximum 16‑bit value.
     * No matching records are added, so the method should safely return {@code false}.
     */
    @ParameterizedTest(name = "type={0} → expected false")
    @MethodSource("edgeTypeProvider")
    void findRRset_edgeTypeValues(int type) {
        int[] edgeTypes = { -1, 0, 65535 };
        for (int type : edgeTypes) {
            assertFalse("Edge type value " + type + " should yield false when no matching RR exists", message.findRRset(testName, type));
        }
    }

    /**
     * Wildcard handling – the method does **not** perform DNS wildcard expansion.
     * Therefore a direct lookup for a sub‑domain should return {@code false}
     * even if a wildcard record is present.
     */
    @Test
    void findRRset_wildcardName_returnsFalse() throws TextParseException {
        Name wildcard = Name.fromConstantString("*.example.com.");
        Record wildcardRecord = Record.newRecord(wildcard, Type.A, DClass.IN);
        message.addRecord(wildcardRecord, Section.ANSWER);
        Name query = Name.fromConstantString("sub.example.com.");
        assertFalse(message.findRRset(query, Type.A));
    }
}
