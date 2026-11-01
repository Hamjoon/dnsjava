
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
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
        // arrange: add an A record to the ANSWER section
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        // act
        boolean result = message.findRRset(testName, TYPE_A);
        // assert
        assertTrue(result, "RRset should be found in the ANSWER section");
    }

    @Test
    void findRRset_presentInAuthority_returnsTrue() {
        // arrange: add an A record to the AUTHORITY section
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.AUTHORITY);
        // act
        boolean result = message.findRRset(testName, TYPE_A);
        // assert
        assertTrue(result, "RRset should be found in the AUTHORITY section");
    }

    @Test
    void findRRset_presentInAdditional_returnsTrue() {
        // arrange: add an A record to the ADDITIONAL section
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ADDITIONAL);
        // act
        boolean result = message.findRRset(testName, TYPE_A);
        // assert
        assertTrue(result, "RRset should be found in the ADDITIONAL section");
    }

    @Test
    void findRRset_notPresent_returnsFalse() {
        // arrange: message is empty – no records added
        // act
        boolean result = message.findRRset(testName, TYPE_A);
        // assert
        assertFalse(result, "RRset should not be found when no matching record exists");
    }

    @Test
    void findRRset_wrongType_returnsFalse() {
        // arrange: add a TXT record (different type) to ANSWER
        Record txtRecord = Record.newRecord(testName, TYPE_TXT, DClass.IN);
        message.addRecord(txtRecord, Section.ANSWER);
        // act
        // looking for A, not TXT
        boolean result = message.findRRset(testName, TYPE_A);
        // assert
        assertFalse(result, "RRset should not be found when the type does not match");
    }

    @Test
    void findRRset_wrongName_returnsFalse() throws TextParseException {
        // arrange: add an A record for a different name
        Name otherName = Name.fromConstantString("other.example.com.");
        Record aRecord = Record.newRecord(otherName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        // act
        // looking for testName
        boolean result = message.findRRset(testName, TYPE_A);
        // assert
        assertFalse(result, "RRset should not be found when the name does not match");
    }

    @Test
    void findRRset_multipleSectionsOnlyOneMatches_returnsTrue() {
        // arrange: add matching record to AUTHORITY, non‑matching to ANSWER
        Record aRecordAuth = Record.newRecord(testName, TYPE_A, DClass.IN);
        Record txtRecordAns = Record.newRecord(testName, TYPE_TXT, DClass.IN);
        message.addRecord(aRecordAuth, Section.AUTHORITY);
        message.addRecord(txtRecordAns, Section.ANSWER);
        // act
        boolean result = message.findRRset(testName, TYPE_A);
        // assert
        assertTrue(result, "RRset should be found when it exists in any section");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("basicScenarioProvider")
    void findRRset_basicScenarios(String nameStr, int type, boolean expected, String _desc) throws Exception {
        // arrange
        Name name = Name.fromConstantString(nameStr);
        // add records according to the description
        if (expected) {
            // decide which section should contain the record based on the description
            if (type == Type.A) {
                // first case – put in ANSWER
                message.addRecord(Record.newRecord(name, type, DClass.IN), Section.ANSWER);
            } else if (type == Type.MX) {
                // second case – put in AUTHORITY
                message.addRecord(Record.newRecord(name, type, DClass.IN), Section.AUTHORITY);
            } else if (type == Type.TXT) {
                // third case – put in ADDITIONAL
                message.addRecord(Record.newRecord(name, type, DClass.IN), Section.ADDITIONAL);
            }
        }
        // act
        boolean result = message.findRRset(name, type);
        // assert
        assertEquals(expected, result);
    }

    @Test
    void findRRset_nullName_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> message.findRRset(null, Type.A), "Passing a null Name should throw NullPointerException");
    }

    @Test
    void findRRset_invalidNameMissingTrailingDot_throwsIllegalArgumentException() {
        // Name without trailing dot is not absolute – library throws IllegalArgumentException
        Name invalid = assertThrows(IllegalArgumentException.class, // constructor validates absolute name
        () -> new Name("example.com"), "Name without trailing dot should cause IllegalArgumentException");
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
        // SIG0 is not allowed in standard sections – the implementation validates this
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(testName, Type.SIG0), "SIG0 type should trigger IllegalArgumentException");
    }

    @Test
    void findRRset_shortCircuitStopsAfterAnswer() throws Exception {
        // arrange: real message with a record in ANSWER
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        // spy on the message to monitor calls to the 3‑arg overload
        Message spy = Mockito.spy(message);
        // act
        boolean result = spy.findRRset(testName, TYPE_A);
        // assert result is true
        assertTrue(result);
        // verify that the overload was called for ANSWER but never for AUTHORITY or ADDITIONAL
        verify(spy, times(1)).findRRset(testName, TYPE_A, Section.ANSWER);
        verify(spy, never()).findRRset(testName, TYPE_A, Section.AUTHORITY);
        verify(spy, never()).findRRset(testName, TYPE_A, Section.ADDITIONAL);
    }

    @Test
    void findRRset_ANYTypeMatchesAnyRecord() {
        // arrange: add an MX record; ANY type (255) should match it
        Record mx = Record.newRecord(testName, Type.MX, DClass.IN);
        message.addRecord(mx, Section.ANSWER);
        // act
        boolean result = message.findRRset(testName, Type.ANY);
        // assert
        assertTrue(result, "ANY type should match any existing RRset for the name");
    }

    @Test
    void findRRset_nameCaseInsensitivity() {
        // arrange: canonical lower‑case name in the message
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        // mixed‑case query name
        Name mixedCase = Name.fromConstantString("ExAmPlE.CoM.");
        // act
        boolean result = message.findRRset(mixedCase, TYPE_A);
        // assert
        assertTrue(result, "DNS names are case‑insensitive; mixed case should still match");
    }

    @Test
    void findRRset_subdomainMismatch_returnsFalse() throws TextParseException {
        // arrange: parent name record only
        Record aRecord = Record.newRecord(testName, TYPE_A, DClass.IN);
        message.addRecord(aRecord, Section.ANSWER);
        // query for a subdomain that does not exist
        Name sub = Name.fromConstantString("sub.example.com.");
        // act
        boolean result = message.findRRset(sub, TYPE_A);
        // assert
        assertFalse(result, "Exact name match is required; subdomain should not match parent record");
    }

    @Test
    void findRRset_emptyMessage_returnsFalse() {
        // No records added – already covered by a simpler test, kept for clarity
        boolean result = message.findRRset(testName, TYPE_A);
        assertFalse(result);
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_findRRset_2_Test.java:146: error: incompatible types: inference variable T has incompatible bounds
        Name invalid = assertThrows(IllegalArgumentException.class, // constructor validates absolute name
                                   ^
    equality constraints: IllegalArgumentException
    lower bounds: Name,Throwable
  where T is a type-variable:
    T extends Throwable declared in method <T>assertThrows(Class<T>,Executable,String)
src/test/java/org/xbill/DNS/Message_findRRset_2_Test.java:176: error: cannot find symbol
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(testName, Type.SIG0), "SIG0 type should trigger IllegalArgumentException");
                                                                                           ^
  symbol:   variable SIG0
  location: class Type
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method findRRset(Name, int)': '\n   * Determines if an RRset with the given name and type is already present in any section.\n   *\n   * @see RRset\n   * @see Section\n   ', 'return type': 'boolean', 'dependent classes': 'class `org.xbill.DNS.Name`:\n\t\nclass `org.xbill.DNS.Section`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Section.ADDITIONAL;\n\t\tint org.xbill.DNS.Section.ANSWER;\n\t\tint org.xbill.DNS.Section.AUTHORITY;\n\t\nclass `org.xbill.DNS.tools.jnamed`:\n\trelated functions:\n\t\tmethod `private void addAdditional(Message, int)` returns `void`, related with `org.xbill.DNS.Message.findRRset(Name, int)`;\nclass `org.xbill.DNS.Message`:\n\trelated functions:\n\t\tmethod `public OPTRecord getOPT()` returns `org.xbill.DNS.OPTRecord`, related with `org.xbill.DNS.Message.findRRset(Name, int)`, api document: \n   * Returns the OPT record from the ADDITIONAL section, if one is present.\n   *\n   * @see OPTRecord\n   * @see Section\n   ;\n\t\tmethod `public boolean findRecord(Record)` returns `boolean`, related with `org.xbill.DNS.Message.findRRset(Name, int)`, api document: \n   * Determines if the given record is already present in any section.\n   *\n   * @see Record\n   * @see Section\n   ;\nclass `org.xbill.DNS.Cache`:\n\trelated functions:\n\t\tmethod `private int getCred(int, boolean)` returns `int`, related with `org.xbill.DNS.Message.findRRset(Name, int)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


