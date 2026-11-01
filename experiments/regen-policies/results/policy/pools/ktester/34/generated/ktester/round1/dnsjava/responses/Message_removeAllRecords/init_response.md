```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Message#removeAllRecords(int)}.
 *
 * The tests verify that:
 * <ul>
 *   <li>the specified section is cleared and the header count for that section is set to zero;</li>
 *   <li>other sections remain untouched;</li>
 *   <li>an {@link IllegalArgumentException} is thrown for an invalid section index.</li>
 * </ul>
 */
class Message_removeAllRecords_Test {

    private Message msg;
    private Record aRecord;
    private Record txtRecord;
    private Record mxRecord;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        // Build a fresh Message with one record in each of the four sections
        msg = new Message();

        Name name = Name.fromConstantString("www.example.");

        // QUESTION section (0)
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        msg.addRecord(aRecord, Section.QUESTION);

        // ANSWER section (1)
        txtRecord = new TXTRecord(name, DClass.IN, 3600, "test");
        msg.addRecord(txtRecord, Section.ANSWER);

        // AUTHORITY section (2)
        mxRecord = new MXRecord(name, DClass.IN, 3600, 10, Name.fromConstantString("mail.example."));
        msg.addRecord(mxRecord, Section.AUTHORITY);

        // ADDITIONAL section (3)
        Record additional = new ARecord(name, DClass.IN, 3600,
                InetAddress.getByName("127.0.0.1"));
        msg.addRecord(additional, Section.ADDITIONAL);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void removeAllRecords_clearsSpecifiedSection_andLeavesOthersIntact() {
        // Verify initial counts are 1 for each section
        Header header = msg.getHeader();
        assertEquals(1, header.getCount(Section.QUESTION));
        assertEquals(1, header.getCount(Section.ANSWER));
        assertEquals(1, header.getCount(Section.AUTHORITY));
        assertEquals(1, header.getCount(Section.ADDITIONAL));

        // Remove all records from the ANSWER section
        msg.removeAllRecords(Section.ANSWER);

        // ANSWER section should now be empty and its count zero
        List<Record> answerSec = msg.getSection(Section.ANSWER);
        assertTrue(answerSec.isEmpty(), "ANSWER section should be empty after removal");
        assertEquals(0, header.getCount(Section.ANSWER));

        // Other sections must retain their records and counts
        assertEquals(1, header.getCount(Section.QUESTION));
        assertEquals(1, header.getCount(Section.AUTHORITY));
        assertEquals(1, header.getCount(Section.ADDITIONAL));

        assertEquals(1, msg.getSection(Section.QUESTION).size());
        assertEquals(1, msg.getSection(Section.AUTHORITY).size());
        assertEquals(1, msg.getSection(Section.ADDITIONAL).size());
    }

    @Test
    void removeAllRecords_multipleCalls_clearsAllSections() {
        // Remove each section one by one
        for (int i = 0; i < 4; i++) {
            msg.removeAllRecords(i);
        }

        // All sections should be empty and header counts zero
        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(),
                    "Section " + i + " should be empty after removeAllRecords");
            assertEquals(0, msg.getHeader().getCount(i),
                    "Header count for section " + i + " should be zero after removal");
        }
    }

    @Test
    void removeAllRecords_invalidSection_throwsIllegalArgumentException() {
        // Section indices are 0‑3; using 4 should trigger an exception from Section.check
        assertThrows(IllegalArgumentException.class, () -> msg.removeAllRecords(4));
        // Negative index also invalid
        assertThrows(IllegalArgumentException.class, () -> msg.removeAllRecords(-1));
    }
}
```