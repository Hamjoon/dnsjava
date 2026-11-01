```java
package org.xbill.DNS;

import java.util.Collections;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#getOPT()}.
 *
 * The tests cover the two possible outcomes of the method:
 * <ul>
 *   <li>returns {@code null} when the additional section does not contain an {@link OPTRecord}</li>
 *   <li>returns the {@link OPTRecord} instance when it is present in the additional section</li>
 * </ul>
 *
 * No private members of {@link Message} or its collaborators are accessed.
 */
class Message_getOPT_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that {@code getOPT()} returns {@code null} when the additional
     * section does not contain an {@link OPTRecord}.
     */
    @Test
    void getOPT_noOptRecord_returnsNull() {
        // Arrange: create an empty message and add a non‑OPT record to the additional section.
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        msg.addRecord(aRecord, Section.ADDITIONAL);

        // Act
        OPTRecord result = msg.getOPT();

        // Assert
        assertNull(result, "Expected getOPT() to return null when no OPTRecord is present");
    }

    /**
     * Verify that {@code getOPT()} returns the {@link OPTRecord} instance
     * that was added to the additional section.
     */
    @Test
    void getOPT_optRecordPresent_returnsSameInstance() {
        // Arrange: create a message and add an OPTRecord to the additional section.
        Message msg = new Message();

        // The constructor for OPTRecord is (int payloadSize, int extendedRcode,
        // int version, int flags, List<EDNSOption> options).  An empty option list is fine.
        OPTRecord opt = new OPTRecord(0, 0, 0, 0, Collections.emptyList());
        msg.addRecord(opt, Section.ADDITIONAL);

        // Act
        OPTRecord result = msg.getOPT();

        // Assert
        assertNotNull(result, "Expected getOPT() to return a non‑null OPTRecord");
        assertSame(opt, result, "Expected getOPT() to return the same OPTRecord instance that was added");
    }
}
```