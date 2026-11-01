package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

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
        assertFalse(message.findRRset(testName, Type.A), "findRRset should be false for an empty message");
    }

    /**
     * The method should locate a matching RRset in the ANSWER section.
     */
    @Test
    void findRRset_recordInAnswer_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue(message.findRRset(testName, Type.A), "findRRset should be true when the record is in the ANSWER section");
    }

    /**
     * The method should locate a matching RRset in the AUTHORITY section.
     */
    @Test
    void findRRset_recordInAuthority_returnsTrue() {
        message.addRecord(aRecord, Section.AUTHORITY);
        assertTrue(message.findRRset(testName, Type.A), "findRRset should be true when the record is in the AUTHORITY section");
    }

    /**
     * The method should locate a matching RRset in the ADDITIONAL section.
     */
    @Test
    void findRRset_recordInAdditional_returnsTrue() {
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRRset(testName, Type.A), "findRRset should be true when the record is in the ADDITIONAL section");
    }

    /**
     * The method must respect the requested type – a different type should not be found.
     */
    @Test
    void findRRset_wrongType_returnsFalse() {
        // Add an AAAA record but query for type A.
        message.addRecord(aaaaRecord, Section.ANSWER);
        assertFalse(message.findRRset(testName, Type.A), "findRRset should be false when only a different type is present");
    }

    /**
     * When the same name exists with multiple types, the method should succeed for any matching type.
     */
    @Test
    void findRRset_multipleTypes_oneMatches_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(aaaaRecord, Section.ANSWER);
        assertTrue(message.findRRset(testName, Type.AAAA), "findRRset should be true for the AAAA type when both A and AAAA records are present");
    }

    /**
     * Verify that a name that does not exist in any section yields {@code false}.
     */
    @Test
    void findRRset_nonexistentName_returnsFalse() throws TextParseException {
        Name otherName = Name.fromConstantString("other.example.com.");
        message.addRecord(aRecord, Section.ANSWER);
        assertFalse(message.findRRset(otherName, Type.A), "findRRset should be false for a name that is not present in any section");
    }

    /**
     * Verify short‑circuit evaluation:
     * <ul>
     *   <li>ANSWER true → AUTHORITY and ADDITIONAL not queried.</li>
     *   <li>ANSWER false, AUTHORITY true → ADDITIONAL not queried.</li>
     *   <li>ANSWER & AUTHORITY false, ADDITIONAL true → overall true.</li>
     *   <li>All false → overall false.</li>
     * </ul>
     */
    @Test
    void findRRset_shortCircuit_logicVerification() {
        // 1️⃣ ANSWER true – should stop after first check.
        Message spy1 = Mockito.spy(new Message());
        spy1.addRecord(aRecord, Section.ANSWER);
        assertTrue(spy1.findRRset(testName, Type.A));
        verify(spy1, times(1)).findRRset(eq(testName), eq(Type.A), eq(Section.ANSWER));
        verify(spy1, never()).findRRset(eq(testName), eq(Type.A), eq(Section.AUTHORITY));
        verify(spy1, never()).findRRset(eq(testName), eq(Type.A), eq(Section.ADDITIONAL));
        // 2️⃣ ANSWER false, AUTHORITY true.
        Message spy2 = Mockito.spy(new Message());
        spy2.addRecord(aRecord, Section.AUTHORITY);
        assertTrue(spy2.findRRset(testName, Type.A));
        verify(spy2, times(1)).findRRset(eq(testName), eq(Type.A), eq(Section.ANSWER));
        verify(spy2, times(1)).findRRset(eq(testName), eq(Type.A), eq(Section.AUTHORITY));
        verify(spy2, never()).findRRset(eq(testName), eq(Type.A), eq(Section.ADDITIONAL));
        // 3️⃣ ANSWER & AUTHORITY false, ADDITIONAL true.
        Message spy3 = Mockito.spy(new Message());
        spy3.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(spy3.findRRset(testName, Type.A));
        verify(spy3, times(1)).findRRset(eq(testName), eq(Type.A), eq(Section.ANSWER));
        verify(spy3, times(1)).findRRset(eq(testName), eq(Type.A), eq(Section.AUTHORITY));
        verify(spy3, times(1)).findRRset(eq(testName), eq(Type.A), eq(Section.ADDITIONAL));
        // 4️⃣ All sections false → overall false.
        Message spy4 = Mockito.spy(new Message());
        assertFalse(spy4.findRRset(testName, Type.A));
        verify(spy4, times(1)).findRRset(eq(testName), eq(Type.A), eq(Section.ANSWER));
        verify(spy4, times(1)).findRRset(eq(testName), eq(Type.A), eq(Section.AUTHORITY));
        verify(spy4, times(1)).findRRset(eq(testName), eq(Type.A), eq(Section.ADDITIONAL));
    }

    /**
     * Passing {@code null} as the name should result in a {@link NullPointerException}
     * propagated from the overloaded method.
     */
    @Test
    void findRRset_nullName_throwsNPE() {
        assertThrows(NullPointerException.class, () -> message.findRRset(null, Type.A), "Calling findRRset with a null Name should throw NullPointerException");
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
        assertTrue(message.findRRset(root, Type.A), "Root name with matching type should be found");
        // Negative case – query for a different type (NS) when only A exists.
        assertFalse(message.findRRset(root, Type.NS), "Root name with non‑matching type should not be found");
    }

    /**
     * DNS names are case‑insensitive. Store the name in lower case and query with mixed case.
     */
    @Test
    void findRRset_nameCaseInsensitivity() throws TextParseException {
        // Record already added in @BeforeEach (lower‑case name).
        message.addRecord(aRecord, Section.ANSWER);
        Name mixedCase = Name.fromConstantString("ExAmPlE.CoM.");
        assertTrue(message.findRRset(mixedCase, Type.A), "Mixed‑case query should succeed because DNS names are case‑insensitive");
    }

    /**
     * Edge type values – negative, zero and the maximum 16‑bit value.
     * No matching records are added, so the method should safely return {@code false}.
     */
    @ParameterizedTest(name = "type={0} → expected false")
    @MethodSource("edgeTypeProvider")
    void findRRset_edgeTypeValues(int type) {
        assertFalse(message.findRRset(testName, type), "Edge type value " + type + " should yield false when no matching RR exists");
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
        assertFalse(message.findRRset(query, Type.A), "Direct lookup for sub.example.com. should be false when only a wildcard record exists");
    }
}
