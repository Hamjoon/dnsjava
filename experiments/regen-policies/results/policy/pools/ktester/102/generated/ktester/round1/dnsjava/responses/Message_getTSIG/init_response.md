```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#getTSIG()}.
 *
 * <p>The tests cover the three possible outcomes of the method:
 *
 * <ul>
 *   <li>no additional records → {@code null}
 *   <li>additional records exist but the last one is not a TSIG → {@code null}
 *   <li>the last additional record is a TSIG → the corresponding {@link TSIGRecord}
 * </ul>
 *
 * The {@link Message#setTSIG(TSIG)} API is used to add a TSIG record to the message
 * without touching private fields.
 */
@ExtendWith(MockitoExtension.class)
class Message_getTSIG_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global test resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * When the message contains no records in the ADDITIONAL section,
     * {@code getTSIG()} must return {@code null}.
     */
    @Test
    void getTSIG_noAdditionalSection_returnsNull() {
        Message m = new Message();               // empty message
        assertNull(m.getTSIG(), "TSIG should be null when no additional records exist");
    }

    /**
     * When the ADDITIONAL section has records but the last record is not a TSIG,
     * {@code getTSIG()} must return {@code null}.
     */
    @Test
    void getTSIG_nonTSIGLastRecord_returnsNull() throws IOException {
        Message m = new Message();

        // add a dummy A record to the ADDITIONAL section
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        m.addRecord(aRecord, Section.ADDITIONAL);

        // verify that the record really is present
        assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));
        assertEquals(Type.A, m.getSection(Section.ADDITIONAL).get(0).getType());

        // the last record is not a TSIG → should be null
        assertNull(m.getTSIG(), "TSIG should be null when last additional record is not a TSIG");
    }

    /**
     * When a TSIG record is added (as the last record of the ADDITIONAL section)
     * {@code getTSIG()} must return that {@link TSIGRecord}.
     */
    @Test
    void getTSIG_tsigPresent_returnsTSIGRecord() throws IOException {
        Message m = new Message();

        // create a TSIG key – the constructor matches the one provided by dnsjava
        Name keyName = Name.fromConstantString("mykey.");
        TSIG tsigKey = new TSIG(keyName, "hmac-md5", "secret".getBytes());

        // add the TSIG to the message using the public API
        m.setTSIG(tsigKey);

        // sanity check – the header should now count one additional record
        assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));

        // the method under test should return the TSIGRecord we just added
        TSIGRecord tsigRecord = m.getTSIG();
        assertNotNull(tsigRecord, "TSIGRecord should be returned when TSIG is present");
        assertEquals(Type.TSIG, tsigRecord.getType(), "Returned record must be of type TSIG");
        // the key stored in the TSIGRecord must be the same key we supplied
        assertEquals(tsigKey.getName(), tsigRecord.getName(),
                "TSIGRecord name should match the key name");
    }
}
```