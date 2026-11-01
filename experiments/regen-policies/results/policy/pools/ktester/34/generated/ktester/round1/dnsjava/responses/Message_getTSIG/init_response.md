```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class Message_getTSIG_Test {

    /** Helper to create a simple TSIG key used for TSIGRecord construction. */
    private static TSIG createTestTSIG() {
        // The algorithm name and secret are not relevant for the getTSIG() logic.
        // Using a minimal constructor that exists in dnsjava.
        return new TSIG("test-key.", "hmac-md5", new byte[] {0x00});
    }

    /** Helper to create a TSIGRecord that can be added to a Message. */
    private static TSIGRecord createTestTSIGRecord() {
        Name name = Name.fromConstantString("example.com.");
        return new TSIGRecord(
                name,               // owner name
                DClass.IN,          // class
                0,                  // ttl
                createTestTSIG(),   // key
                0,                  // error code
                new byte[0]);       // other data
    }

    /** Helper to create a generic ARecord for non‑TSIG tests. */
    private static ARecord createTestARecord() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        return new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    }

    @Test
    void getTSIG_NoAdditionalSection_ReturnsNull() {
        Message msg = new Message();               // empty message, no records
        assertNull(msg.getTSIG(), "TSIG should be null when ADDITIONAL count is 0");
    }

    @Test
    void getTSIG_LastAdditionalNotTSIG_ReturnsNull() throws UnknownHostException {
        Message msg = new Message();
        // Add a normal A record to the ADDITIONAL section.
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);
        // Header count for ADDITIONAL is now 1, but the record is not TSIG.
        assertEquals(1, msg.getHeader().getCount(Section.ADDITIONAL));
        assertNull(msg.getTSIG(), "TSIG should be null when last ADDITIONAL record is not TSIG");
    }

    @Test
    void getTSIG_LastAdditionalIsTSIG_ReturnsRecord() {
        Message msg = new Message();
        // First add a normal record (optional, to ensure TSIG is truly the last one).
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);
        // Then add the TSIG record as the final additional record.
        TSIGRecord tsigRec = createTestTSIGRecord();
        msg.addRecord(tsigRec, Section.ADDITIONAL);

        // Verify header count reflects both records.
        assertEquals(2, msg.getHeader().getCount(Section.ADDITIONAL));

        TSIGRecord result = msg.getTSIG();
        assertNotNull(result, "TSIG should be returned when the last ADDITIONAL record is TSIG");
        assertSame(tsigRec, result, "Returned TSIGRecord should be the one that was added");
    }

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
}
```