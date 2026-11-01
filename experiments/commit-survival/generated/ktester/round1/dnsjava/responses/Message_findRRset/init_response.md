```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Message#findRRset(Name, int, int)}.
 *
 * The tests cover the three logical branches of the method:
 *   1. The requested section array entry is {@code null}.
 *   2. The section exists but does not contain a matching record.
 *   3. The section contains a matching record (same name and type).
 *
 * No private members are accessed and no external resources are required.
 */
@ExtendWith(MockitoExtension.class)
class Message_findRRset_Test {

    /** Message instance used by the tests. */
    private Message message;

    /** Name used for the test records. */
    private Name testName;

    /** InetAddress for A‑record creation. */
    private InetAddress testAddr;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException, TextParseException {
        // Fresh message for every test – the default constructor creates
        // a Message with an empty {@code sections} array (all entries null).
        message = new Message();

        // Simple, absolute domain name.
        testName = Name.fromConstantString("example.com.");

        // IP address for the A record.
        testAddr = InetAddress.getByName("1.2.3.4");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the garbage collector will reclaim objects.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * When the requested section has never been allocated (i.e. {@code null}),
     * {@code findRRset} must return {@code false}.
     */
    @Test
    void findRRset_sectionNull_returnsFalse() {
        // Section.ANSWER == 1 according to org.xbill.DNS.Section
        boolean result = message.findRRset(testName, Type.A, Section.ANSWER);
        assertFalse(result, "findRRset should return false when the section array entry is null");
    }

    /**
     * When the section exists but does not contain a record with the given
     * name/type, the method must return {@code false}.
     */
    @Test
    void findRRset_noMatchingRecord_returnsFalse() throws Exception {
        // Allocate the section by adding a record of a *different* type.
        Record nsRecord = new NSRecord(testName, DClass.IN, 3600, Name.fromConstantString("ns.example.com."));
        message.addRecord(nsRecord, Section.ANSWER);

        // Query for an A record – there is none.
        boolean result = message.findRRset(testName, Type.A, Section.ANSWER);
        assertFalse(result, "findRRset should return false when no matching record is present");
    }

    /**
     * When the section contains a record whose name and type match the query,
     * the method must return {@code true}.
     */
    @Test
    void findRRset_matchingRecord_returnsTrue() throws Exception {
        // Add an A record that matches the query parameters.
        Record aRecord = new ARecord(testName, DClass.IN, 3600, testAddr);
        message.addRecord(aRecord, Section.ANSWER);

        boolean result = message.findRRset(testName, Type.A, Section.ANSWER);
        assertTrue(result, "findRRset should return true when a matching record is present");
    }

    /**
     * Verify that the method distinguishes between different names.
     */
    @Test
    void findRRset_differentName_returnsFalse() throws Exception {
        // Record with a different name but same type.
        Name otherName = Name.fromConstantString("other.example.com.");
        Record aRecord = new ARecord(otherName, DClass.IN, 3600, testAddr);
        message.addRecord(aRecord, Section.ANSWER);

        boolean result = message.findRRset(testName, Type.A, Section.ANSWER);
        assertFalse(result, "findRRset should return false when the name does not match");
    }

    /**
     * Verify that the method distinguishes between different types.
     */
    @Test
    void findRRset_differentType_returnsFalse() throws Exception {
        // Record with same name but different type (TXT instead of A).
        Record txtRecord = new TXTRecord(testName, DClass.IN, 3600, "sample");
        message.addRecord(txtRecord, Section.ANSWER);

        boolean result = message.findRRset(testName, Type.A, Section.ANSWER);
        assertFalse(result, "findRRset should return false when the type does not match");
    }
}
```