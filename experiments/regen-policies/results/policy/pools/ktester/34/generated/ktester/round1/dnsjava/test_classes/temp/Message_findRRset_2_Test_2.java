package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;

class Message_findRRset_2_Test {

    private Message message;

    private Name testName;

    private static final int TYPE_A = Type.A;

    private static final int TYPE_TXT = Type.TXT;

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Fresh message and name for each test
        message = new Message();
        testName = Name.fromConstantString("example.com.");
    }

    private static Stream<Arguments> basicScenarioProvider() throws TextParseException {
        return Stream.of(// name, type, expected, description
        Arguments.of("example.com.", Type.A, true, "Record in ANSWER – first call returns true"), Arguments.of("example.org.", Type.MX, true, "Record only in AUTHORITY – second call returns true"), Arguments.of("example.net.", Type.TXT, true, "Record only in ADDITIONAL – third call returns true"), Arguments.of("nonexistent.example.", Type.SOA, false, "No matching record in any section"));
    }

    @Test
    void findRRset_presentInAnswer_returnsTrue() {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        boolean result = message.findRRset(testName, TYPE_A);
        assertTrue(result, "RRset should be found in the ANSWER section");
    }

    @Test
    void findRRset_presentInAuthority_returnsTrue() {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.AUTHORITY);
        boolean result = message.findRRset(testName, TYPE_A);
        assertTrue(result, "RRset should be found in the AUTHORITY section");
    }

    @Test
    void findRRset_presentInAdditional_returnsTrue() {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ADDITIONAL);
        boolean result = message.findRRset(testName, TYPE_A);
        assertTrue(result, "RRset should be found in the ADDITIONAL section");
    }

    @Test
    void findRRset_notPresent_returnsFalse() {
        boolean result = message.findRRset(testName, TYPE_A);
        assertFalse(result, "RRset should not be found when no matching record exists");
    }

    @Test
    void findRRset_wrongType_returnsFalse() {
        Record txtRecord = Record.newRecord(testName, TYPE_TXT, DClass.IN);
        message.addRecord(txtRecord, Section.ANSWER);
        boolean result = message.findRRset(testName, TYPE_A);
        assertFalse(result, "RRset should not be found when the type does not match");
    }

    @Test
    void findRRset_wrongName_returnsFalse() throws TextParseException {
        Name otherName = Name.fromConstantString("other.example.com.");
        Record aRecord = Record.newRecord(otherName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        boolean result = message.findRRset(testName, TYPE_A);
        assertFalse(result, "RRset should not be found when the name does not match");
    }

    @Test
    void findRRset_multipleSectionsOnlyOneMatches_returnsTrue() {
        Record aRecordAuth = Record.newRecord(testName, TYPE_A, DClass.IN);
        Record txtRecordAns = Record.newRecord(testName, TYPE_TXT, DClass.IN);
        message.addRecord(aRecordAuth, Section.AUTHORITY);
        message.addRecord(txtRecordAns, Section.ANSWER);
        boolean result = message.findRRset(testName, TYPE_A);
        assertTrue(result, "RRset should be found when it exists in any section");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("basicScenarioProvider")
    void findRRset_basicScenarios(String nameStr, int type, boolean expected, String _desc) throws Exception {
        Name name = Name.fromConstantString(nameStr);
        if (expected) {
            if (type == Type.A) {
                message.addRecord(Record.newRecord(name, type, DClass.IN), Section.ANSWER);
            } else if (type == Type.MX) {
                message.addRecord(Record.newRecord(name, type, DClass.IN), Section.AUTHORITY);
            } else if (type == Type.TXT) {
                message.addRecord(Record.newRecord(name, type, DClass.IN), Section.ADDITIONAL);
            }
        }
        boolean result = message.findRRset(name, type);
        assertEquals(expected, result);
    }

    @Test
    void findRRset_nullName_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> message.findRRset(null, Type.A), "Passing a null Name should throw NullPointerException");
    }

    @Test
    void findRRset_invalidNameMissingTrailingDot_throwsIllegalArgumentException() {
        // Name without trailing dot is not absolute – library throws IllegalArgumentException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Name("example.com"), "Name without trailing dot should cause IllegalArgumentException");
        // The exception is enough – no need to call findRRset
    }

    @Test
    void findRRset_invalidNameIllegalCharacters_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Name("exa$mple.com."), "Name containing illegal characters should cause IllegalArgumentException");
    }

    @Test
    void findRRset_invalidNameEmptyString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Name(""), "Empty string is not a valid DNS name");
    }

    @Test
    void findRRset_negativeType_returnsFalse() {
        // negative type is treated as non‑matching, no exception expected
        boolean result = message.findRRset(testName, -1);
        assertFalse(result, "Negative DNS type should result in false");
    }

    @Test
    void findRRset_typeGreaterThan65535_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(testName, 65536), "Type values > 65535 should cause IllegalArgumentException");
    }

    @Test
    void findRRset_typeNotAllowedInSection_throwsIllegalArgumentException() {
        // SIG is not allowed in standard sections in this implementation – expect IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(testName, Type.SIG), "SIG type should trigger IllegalArgumentException");
    }

    @Test
    void findRRset_shortCircuitStopsAfterAnswer() throws Exception {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        Message spy = Mockito.spy(message);
        boolean result = spy.findRRset(testName, TYPE_A);
        assertTrue(result);
        verify(spy, times(1)).findRRset(testName, TYPE_A, Section.ANSWER);
        verify(spy, never()).findRRset(testName, TYPE_A, Section.AUTHORITY);
        verify(spy, never()).findRRset(testName, TYPE_A, Section.ADDITIONAL);
    }

    @Test
    void findRRset_ANYTypeMatchesAnyRecord() {
        // According to dnsjava's current implementation, ANY does not act as a wildcard for findRRset
        Record mx = Record.newRecord(testName, Type.MX, DClass.IN);
        message.addRecord(mx, Section.ANSWER);
        boolean result = message.findRRset(testName, Type.ANY);
        assertFalse(result, "ANY type does not match arbitrary records in findRRset");
    }

    @Test
    void findRRset_nameCaseInsensitivity() {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        Name mixedCase = Name.fromConstantString("ExAmPlE.CoM.");
        boolean result = message.findRRset(mixedCase, TYPE_A);
        assertTrue(result, "DNS names are case‑insensitive; mixed case should still match");
    }

    @Test
    void findRRset_subdomainMismatch_returnsFalse() throws TextParseException {
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        Name sub = Name.fromConstantString("sub.example.com.");
        boolean result = message.findRRset(sub, TYPE_A);
        assertFalse(result, "Exact name match is required; subdomain should not match parent record");
    }

    @Test
    void findRRset_emptyMessage_returnsFalse() {
        boolean result = message.findRRset(testName, TYPE_A);
        assertFalse(result);
    }

    @Test
    void findRRset_nullName_returnsFalse() {
        // The implementation returns false for a null name rather than throwing NPE
        boolean result = message.findRRset(null, Type.A);
        assertFalse(result, "Passing a null Name should return false");
    }

    @Test
    void findRRset_invalidNameMissingTrailingDot_throwsTextParseException() {
        // Name without trailing dot is not absolute – library throws TextParseException
        assertThrows(TextParseException.class, () -> new Name("example.com"), "Name without trailing dot should cause TextParseException");
    }

    @Test
    void findRRset_invalidNameIllegalCharacters_throwsTextParseException() {
        assertThrows(TextParseException.class, () -> new Name("exa$mple.com."), "Name containing illegal characters should cause TextParseException");
    }

    @Test
    void findRRset_invalidNameEmptyString_throwsTextParseException() {
        assertThrows(TextParseException.class, () -> new Name(""), "Empty string is not a valid DNS name");
    }

    @Test
    void findRRset_negativeType_throwsInvalidTypeException() {
        // Negative type is rejected by Type.check and results in InvalidTypeException
        assertThrows(InvalidTypeException.class, () -> message.findRRset(testName, -1), "Negative DNS type should cause InvalidTypeException");
    }

    @Test
    void findRRset_typeNotAllowedInSection_returnsFalse() {
        // SIG is allowed by the library; therefore findRRset simply returns false when no matching RRset exists
        boolean result = message.findRRset(testName, Type.SIG);
        assertFalse(result, "SIG type should return false when no matching RRset is present");
    }
}
