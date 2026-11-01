```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_getOPT_Test {

    private Message message;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message instance for each test
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse
        message = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void getOPT_whenNoOPTRecord_returnsNull() {
        assertNull(message.getOPT(), "Expected getOPT() to return null when no OPTRecord is present");
    }

    @Test
    void getOPT_whenOPTRecordInAdditionalSection_returnsOPT() {
        // Create a mock OPTRecord – it will behave as an instance of OPTRecord
        OPTRecord opt = mock(OPTRecord.class);
        // Add it to the ADDITIONAL section
        message.addRecord(opt, Section.ADDITIONAL);
        // Verify that getOPT() returns the same instance
        assertSame(opt, message.getOPT(), "Expected getOPT() to return the OPTRecord that was added");
    }

    @Test
    void getOPT_whenOPTRecordInOtherSection_returnsNull() {
        OPTRecord opt = mock(OPTRecord.class);
        // Add the OPTRecord to a non‑additional section (e.g., ANSWER)
        message.addRecord(opt, Section.ANSWER);
        assertNull(message.getOPT(),
                "Expected getOPT() to return null when OPTRecord is not in the ADDITIONAL section");
    }

    @Test
    void getOPT_whenMultipleRecords_firstOPTReturned() {
        // Non‑OPT record (mocked as generic Record)
        Record nonOpt = mock(Record.class);
        // Two different OPTRecord mocks
        OPTRecord opt1 = mock(OPTRecord.class);
        OPTRecord opt2 = mock(OPTRecord.class);

        // Insert records in the order: non‑OPT, opt1, opt2
        message.addRecord(nonOpt, Section.ADDITIONAL);
        message.addRecord(opt1, Section.ADDITIONAL);
        message.addRecord(opt2, Section.ADDITIONAL);

        // getOPT should return the first OPTRecord encountered (opt1)
        assertSame(opt1, message.getOPT(),
                "Expected getOPT() to return the first OPTRecord in the ADDITIONAL section");
    }
}
```