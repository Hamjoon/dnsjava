```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_findRRset_2_Test {

    private Message message;
    private Name nameA;
    private Name nameB;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws TextParseException, UnknownHostException {
        // Create a fresh Message for every test
        message = new Message();

        // Two distinct names – one that will be used for matching records,
        // another that will stay absent.
        nameA = Name.fromConstantString("example.com.");
        nameB = Name.fromConstantString("absent.com.");

        // Populate the message with a few records in different sections.
        // ANSWER section – a matching A record.
        Record aRecord = new ARecord(nameA, DClass.IN, 3600,
                InetAddress.getByName("192.0.2.1"));
        message.addRecord(aRecord, Section.ANSWER);

        // AUTHORITY section – a matching NS record (different type).
        Record nsRecord = new NSRecord(nameA, DClass.IN, 3600,
                Name.fromConstantString("ns1.example.com."));
        message.addRecord(nsRecord, Section.AUTHORITY);

        // ADDITIONAL section – a matching TXT record (different type).
        Record txtRecord = new TXTRecord(nameA, DClass.IN, 3600, "sample txt");
        message.addRecord(txtRecord, Section.ADDITIONAL);
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
    void findRRset_matchesInAnswerSection() {
        // The A record added in the ANSWER section should be found.
        assertTrue(message.findRRset(nameA, Type.A),
                "findRRset should return true when a matching RRset exists in the ANSWER section");
    }

    @Test
    void findRRset_matchesInAuthoritySection() {
        // The NS record added in the AUTHORITY section should be found.
        assertTrue(message.findRRset(nameA, Type.NS),
                "findRRset should return true when a matching RRset exists in the AUTHORITY section");
    }

    @Test
    void findRRset_matchesInAdditionalSection() {
        // The TXT record added in the ADDITIONAL section should be found.
        assertTrue(message.findRRset(nameA, Type.TXT),
                "findRRset should return true when a matching RRset exists in the ADDITIONAL section");
    }

    @Test
    void findRRset_returnsFalseWhenNoMatch() {
        // No record with nameB has been added, so any type lookup must fail.
        assertFalse(message.findRRset(nameB, Type.A),
                "findRRset should return false when no matching RRset exists in any section");
    }

    @Test
    void findRRset_returnsFalseWhenTypeDoesNotMatch() {
        // nameA exists but we query for a type that was never added (CNAME).
        assertFalse(message.findRRset(nameA, Type.CNAME),
                "findRRset should return false when the name exists but the type does not match any RRset");
    }

    @Test
    void findRRset_isShortCircuitingAcrossSections() {
        // Ensure that the method checks all sections: we add a matching record only
        // to the ADDITIONAL section and verify that the result is true even though
        // the ANSWER and AUTHORITY sections do not contain that type.
        Name onlyInAdditional = Name.fromConstantString("only.additional.");
        Record mxRecord = new MXRecord(onlyInAdditional, DClass.IN, 3600,
                10, Name.fromConstantString("mail.example.com."));
        // Add only to ADDITIONAL
        message.addRecord(mxRecord, Section.ADDITIONAL);

        // No MX record exists in ANSWER or AUTHORITY, but should still be found.
        assertTrue(message.findRRset(onlyInAdditional, Type.MX),
                "findRRset should locate a matching RRset even if it appears only in the ADDITIONAL section");
    }
}
```