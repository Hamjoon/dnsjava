```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_getOPT_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test case
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Message does not hold external resources
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verify that getOPT() returns {@code null} when the ADDITIONAL section does not contain
     * an {@link OPTRecord}.
     */
    @Test
    void getOPT_noOPTRecord_returnsNull() {
        // Add a regular TXT record to the ADDITIONAL section
        Name name = Name.fromConstantString("example.com.");
        Record txt = new TXTRecord(name, DClass.IN, 3600, "sample text");
        message.addRecord(txt, Section.ADDITIONAL);

        // No OPT record present – should return null
        assertNull(message.getOPT(), "Expected getOPT() to return null when no OPTRecord is present");
    }

    /**
     * Verify that getOPT() returns the OPT record that has been added to the ADDITIONAL section.
     */
    @Test
    void getOPT_withOPTRecord_returnsRecord() {
        // Create and add an OPT record
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        // The method should return the same instance
        OPTRecord result = message.getOPT();
        assertNotNull(result, "Expected getOPT() to return a non‑null OPTRecord");
        assertSame(opt, result, "Expected getOPT() to return the exact OPTRecord that was added");
    }

    /**
     * Verify that when multiple records are present in the ADDITIONAL section,
     * getOPT() returns the first {@link OPTRecord} encountered.
     */
    @Test
    void getOPT_multipleRecords_returnsFirstOPT() {
        Name name = Name.fromConstantString("example.org.");

        // Add a non‑OPT record first
        Record txt1 = new TXTRecord(name, DClass.IN, 3600, "first");
        message.addRecord(txt1, Section.ADDITIONAL);

        // Add the OPT record
        OPTRecord opt = new OPTRecord(1232, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        // Add another non‑OPT record after the OPT
        Record txt2 = new TXTRecord(name, DClass.IN, 3600, "second");
        message.addRecord(txt2, Section.ADDITIONAL);

        // getOPT should return the OPT we added (the only OPT in the list)
        assertSame(opt, message.getOPT(), "Expected getOPT() to return the first OPTRecord in the section");
    }

    /**
     * Verify that an OPT record can be retrieved even when it is the only record
     * in the ADDITIONAL section.
     */
    @Test
    void getOPT_onlyOPTRecord_returnsIt() {
        OPTRecord opt = new OPTRecord(512, 0, 0, 0);
        message.addRecord(opt, Section.ADDITIONAL);

        OPTRecord retrieved = message.getOPT();
        assertNotNull(retrieved, "Expected a non‑null OPTRecord");
        assertEquals(opt, retrieved, "Expected the retrieved OPTRecord to be equal to the one added");
    }
}
```