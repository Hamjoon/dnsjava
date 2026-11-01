```java
package org.xbill.DNS;

import org.junit.*;
import java.net.UnknownHostException;
import static org.junit.Assert.*;
import java.util.stream.Stream;

/**
 * JUnit‑4 version of the {@code Message.findRRset} tests.
 *
 * <p>This class avoids JUnit‑5 / Jupiter APIs because the execution
 * environment provides only the JUnit‑4 engine. All tests are written
 * with the classic JUnit‑4 annotations and assertions.</p>
 */
public class Message_findRRset_2_Test {

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

    @Before
    public void setupBeforeEach() throws UnknownHostException, TextParseException {
        // Fresh message for every test – the default constructor creates an empty header.
        message = new Message();
        // Use a simple, absolute name.
        testName = Name.fromConstantString("example.com.");
        // Create two different records so we can test type‑sensitivity.
        aRecord = Record.newRecord(testName, Type.A, DClass.IN);
        aaaaRecord = Record.newRecord(testName, Type.AAAA, DClass.IN);
    }

    @After
    public void teardownAfterEach() {
        // Help the garbage collector; not strictly necessary but keeps the pattern clear.
        message = null;
        testName = null;
        aRecord = null;
        aaaaRecord = null;
    }

    /**
     * The method should return {@code false} when the message contains no records.
     */
    @Test
    public void findRRset_emptyMessage_returnsFalse() {
        assertFalse(message.findRRset(testName, Type.A));
    }

    /**
     * The method should locate a matching RRset in the ANSWER section.
     */
    @Test
    public void findRRset_recordInAnswer_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue(message.findRRset(testName, Type.A));
    }

    /**
     * The method should locate a matching RRset in the AUTHORITY section.
     */
    @Test
    public void findRRset_recordInAuthority_returnsTrue() {
        message.addRecord(aRecord, Section.AUTHORITY);
        assertTrue(message.findRRset(testName, Type.A));
    }

    /**
     * The method should locate a matching RRset in the ADDITIONAL section.
     */
    @Test
    public void findRRset_recordInAdditional_returnsTrue() {
        message.addRecord(aRecord, Section.ADDITIONAL);
        assertTrue(message.findRRset(testName, Type.A));
    }

    /**
     * The method must respect the requested type – a different type should not be found.
     */
    @Test
    public void findRRset_wrongType_returnsFalse() {
        // Add an AAAA record but query for type A.
        message.addRecord(aaaaRecord, Section.ANSWER);
        assertFalse(message.findRRset(testName, Type.A));
    }

    /**
     * When the same name exists with multiple types, the method should succeed for any matching type.
     */
    @Test
    public void findRRset_multipleTypes_oneMatches_returnsTrue() {
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(aaaaRecord, Section.ANSWER);
        assertTrue(message.findRRset(testName, Type.AAAA));
    }

    /**
     * Verify that a name that does not exist in any section yields {@code false}.
     */
    @Test
    public void findRRset_nonexistentName_returnsFalse() throws TextParseException {
        Name otherName = Name.fromConstantString("other.example.com.");
        message.addRecord(aRecord, Section.ANSWER);
        assertFalse(message.findRRset(otherName, Type.A));
    }

    /**
     * Passing {@code null} as the name should result in a {@link NullPointerException}
     * propagated from the overloaded method.
     */
    @Test(expected = NullPointerException.class)
    public void findRRset_nullName_throwsNPE() {
        message.findRRset(null, Type.A);
    }

    /**
     * Root name handling – the root label "." is a valid DNS name.
     * The method must correctly locate records whose name is the root.
     */
    @Test
    public void findRRset_rootName_successAndFailure() throws TextParseException {
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
    public void findRRset_nameCaseInsensitivity() throws TextParseException {
        // Record already added in @BeforeEach (lower‑case name).
        message.addRecord(aRecord, Section.ANSWER);
        Name mixedCase = Name.fromConstantString("ExAmPlE.CoM.");
        assertTrue(message.findRRset(mixedCase, Type.A));
    }

    /**
     * Edge type values – negative, zero and the maximum 16‑bit value.
     * No matching records are added, so the method should safely return {@code false}.
     */
    @Test
    public void findRRset_edgeTypeValues() {
        int[] edgeTypes = { -1, 0, 65535 };
        for (int type : edgeTypes) {
            assertFalse("Edge type value " + type + " should yield false when no matching RR exists",
                    message.findRRset(testName, type));
        }
    }

    /**
     * Wildcard handling – the method does **not** perform DNS wildcard expansion.
     * Therefore a direct lookup for a sub‑domain should return {@code false}
     * even if a wildcard record is present.
     */
    @Test
    public void findRRset_wildcardName_returnsFalse() throws TextParseException {
        Name wildcard = Name.fromConstantString("*.example.com.");
        Record wildcardRecord = Record.newRecord(wildcard, Type.A, DClass.IN);
        message.addRecord(wildcardRecord, Section.ANSWER);
        Name query = Name.fromConstantString("sub.example.com.");
        assertFalse(message.findRRset(query, Type.A));
    }
}
```