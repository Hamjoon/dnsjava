```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.*;

class Message_getTSIG_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed
    }

    /**
     * When the ADDITIONAL section is empty, getTSIG() must return {@code null}.
     */
    @Test
    void getTSIG_noAdditionalSection_returnsNull() {
        Message msg = new Message();               // empty message, no records
        assertNull(msg.getTSIG(), "TSIG should be null when no additional records are present");
    }

    /**
     * When the ADDITIONAL section contains records but the last one is not a TSIG,
     * getTSIG() must return {@code null}.
     */
    @Test
    void getTSIG_lastRecordNotTSIG_returnsNull() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");
        // add a normal TXT record to the ADDITIONAL section
        msg.addRecord(new TXTRecord(name, DClass.IN, 3600, "test"), Section.ADDITIONAL);
        // verify that the record is present
        assertEquals(1, msg.getHeader().getCount(Section.ADDITIONAL));
        assertNotNull(msg.getSection(Section.ADDITIONAL).get(0));
        // the last record is not a TSIG, so getTSIG should be null
        assertNull(msg.getTSIG(), "TSIG should be null when the last additional record is not a TSIG");
    }

    /**
     * When a TSIG record is the last entry in the ADDITIONAL section,
     * getTSIG() must return that TSIGRecord.
     */
    @Test
    void getTSIG_lastRecordIsTSIG_returnsTSIGRecord() throws IOException {
        Message msg = new Message();

        // create a dummy TSIG key (algorithm and secret are arbitrary for the test)
        Name keyName = Name.fromConstantString("key.example.");
        TSIG tsigKey = new TSIG(keyName, "hmac-md5.sig-alg.reg.int.", new byte[]{0x01, 0x02, 0x03, 0x04});

        // apply the TSIG to the message – this adds a TSIGRecord as the last additional record
        tsigKey.apply(msg, null);

        // sanity check: the header should now report one record in the ADDITIONAL section
        assertEquals(1, msg.getHeader().getCount(Section.ADDITIONAL));

        // retrieve the TSIG via the method under test
        TSIGRecord retrieved = msg.getTSIG();

        assertNotNull(retrieved, "TSIG should be returned when present as the last additional record");
        assertSame(TSIGRecord.class, retrieved.getClass(), "Returned record must be a TSIGRecord");
        // the retrieved record should be the same instance that was added by TSIG.apply()
        TSIGRecord expected = (TSIGRecord) msg.getSection(Section.ADDITIONAL).get(0);
        assertSame(expected, retrieved, "Returned TSIGRecord should be the one stored in the message");
    }

    /**
     * When multiple records exist in the ADDITIONAL section and the last one is a TSIG,
     * getTSIG() must still return the TSIGRecord.
     */
    @Test
    void getTSIG_multipleAdditionalRecords_lastIsTSIG_returnsTSIGRecord() throws IOException {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");

        // add a few non‑TSIG records first
        msg.addRecord(new TXTRecord(name, DClass.IN, 3600, "first"), Section.ADDITIONAL);
        msg.addRecord(new TXTRecord(name, DClass.IN, 3600, "second"), Section.ADDITIONAL);

        // now add a TSIG record
        Name keyName = Name.fromConstantString("key.example.");
        TSIG tsigKey = new TSIG(keyName, "hmac-md5.sig-alg.reg.int.", new byte[]{0x0A, 0x0B, 0x0C});
        tsigKey.apply(msg, null);

        // header count should reflect three additional records
        assertEquals(3, msg.getHeader().getCount(Section.ADDITIONAL));

        // getTSIG should return the last record (the TSIG)
        TSIGRecord tsig = msg.getTSIG();
        assertNotNull(tsig, "TSIG should be returned when it is the last additional record");
        assertEquals(Type.TSIG, tsig.getType(), "Returned record must have type TSIG");
        // ensure it is indeed the last element in the section list
        Record last = msg.getSection(Section.ADDITIONAL).get(2);
        assertSame(tsig, last, "TSIGRecord returned by getTSIG must be the last record in the ADDITIONAL section");
    }
}
```