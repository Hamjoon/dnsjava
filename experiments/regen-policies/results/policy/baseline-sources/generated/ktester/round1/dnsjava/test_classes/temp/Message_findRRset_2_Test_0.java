package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_findRRset_2_Test {

    private Message message;

    private Name nameA;

    private Name nameB;

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
        Record aRecord = new ARecord(nameA, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        message.addRecord(aRecord, Section.ANSWER);
        // AUTHORITY section – a matching NS record (different type).
        Record nsRecord = new NSRecord(nameA, DClass.IN, 3600, Name.fromConstantString("ns1.example.com."));
        message.addRecord(nsRecord, Section.AUTHORITY);
        // ADDITIONAL section – a matching TXT record (different type).
        Record txtRecord = new TXTRecord(nameA, DClass.IN, 3600, "sample txt");
        message.addRecord(txtRecord, Section.ADDITIONAL);
    }

    @Test
    void findRRset_matchesInAnswerSection() {
        // The A record added in the ANSWER section should be found.
        assertTrue(message.findRRset(nameA, Type.A), "findRRset should return true when a matching RRset exists in the ANSWER section");
    }

    @Test
    void findRRset_matchesInAuthoritySection() {
        // The NS record added in the AUTHORITY section should be found.
        assertTrue(message.findRRset(nameA, Type.NS), "findRRset should return true when a matching RRset exists in the AUTHORITY section");
    }

    @Test
    void findRRset_matchesInAdditionalSection() {
        // The TXT record added in the ADDITIONAL section should be found.
        assertTrue(message.findRRset(nameA, Type.TXT), "findRRset should return true when a matching RRset exists in the ADDITIONAL section");
    }

    @Test
    void findRRset_returnsFalseWhenNoMatch() {
        // No record with nameB has been added, so any type lookup must fail.
        assertFalse(message.findRRset(nameB, Type.A), "findRRset should return false when no matching RRset exists in any section");
    }

    @Test
    void findRRset_returnsFalseWhenTypeDoesNotMatch() {
        // nameA exists but we query for a type that was never added (CNAME).
        assertFalse(message.findRRset(nameA, Type.CNAME), "findRRset should return false when the name exists but the type does not match any RRset");
    }

    @Test
    void findRRset_isShortCircuitingAcrossSections() {
        // Ensure that the method checks all sections: we add a matching record only
        // to the ADDITIONAL section and verify that the result is true even though
        // the ANSWER and AUTHORITY sections do not contain that type.
        Name onlyInAdditional = Name.fromConstantString("only.additional.");
        Record mxRecord = new MXRecord(onlyInAdditional, DClass.IN, 3600, 10, Name.fromConstantString("mail.example.com."));
        // Add only to ADDITIONAL
        message.addRecord(mxRecord, Section.ADDITIONAL);
        // No MX record exists in ANSWER or AUTHORITY, but should still be found.
        assertTrue(message.findRRset(onlyInAdditional, Type.MX), "findRRset should locate a matching RRset even if it appears only in the ADDITIONAL section");
    }

    @Test
    @DisplayName("Short‑circuit when first section (ANSWER) matches")
    void shortCircuit_firstSectionMatches() throws TextParseException {
        Message spy = spy(new Message());
        Name name = Name.fromString("example.com.");
        // Stub only the ANSWER check to return true.
        doReturn(true).when(spy).findRRset(name, Type.A, Section.ANSWER);
        // The other sections should never be invoked.
        doReturn(false).when(spy).findRRset(name, Type.A, Section.AUTHORITY);
        doReturn(false).when(spy).findRRset(name, Type.A, Section.ADDITIONAL);
        assertTrue(spy.findRRset(name, Type.A), "Overall result must be true when first section matches");
        // Verify short‑circuit: only the first call happened.
        verify(spy, times(1)).findRRset(name, Type.A, Section.ANSWER);
        verify(spy, never()).findRRset(name, Type.A, Section.AUTHORITY);
        verify(spy, never()).findRRset(name, Type.A, Section.ADDITIONAL);
    }

    @Test
    @DisplayName("Short‑circuit when second section (AUTHORITY) matches")
    void shortCircuit_secondSectionMatches() throws TextParseException {
        Message spy = spy(new Message());
        Name name = Name.fromString("example.org.");
        // ANSWER returns false, AUTHORITY returns true.
        doReturn(false).when(spy).findRRset(name, Type.MX, Section.ANSWER);
        doReturn(true).when(spy).findRRset(name, Type.MX, Section.AUTHORITY);
        // ADDITIONAL must not be called.
        doReturn(false).when(spy).findRRset(name, Type.MX, Section.ADDITIONAL);
        assertTrue(spy.findRRset(name, Type.MX), "Overall result must be true when second section matches");
        verify(spy, times(1)).findRRset(name, Type.MX, Section.ANSWER);
        verify(spy, times(1)).findRRset(name, Type.MX, Section.AUTHORITY);
        verify(spy, never()).findRRset(name, Type.MX, Section.ADDITIONAL);
    }

    @Test
    @DisplayName("Short‑circuit when third section (ADDITIONAL) matches")
    void shortCircuit_thirdSectionMatches() throws TextParseException {
        Message spy = spy(new Message());
        Name name = Name.fromString("example.net.");
        // First two sections false, third true.
        doReturn(false).when(spy).findRRset(name, Type.SRV, Section.ANSWER);
        doReturn(false).when(spy).findRRset(name, Type.SRV, Section.AUTHORITY);
        doReturn(true).when(spy).findRRset(name, Type.SRV, Section.ADDITIONAL);
        assertTrue(spy.findRRset(name, Type.SRV), "Overall result must be true when third section matches");
        verify(spy, times(1)).findRRset(name, Type.SRV, Section.ANSWER);
        verify(spy, times(1)).findRRset(name, Type.SRV, Section.AUTHORITY);
        verify(spy, times(1)).findRRset(name, Type.SRV, Section.ADDITIONAL);
    }

    @Test
    @DisplayName("All sections return false – no short‑circuit")
    void shortCircuit_allSectionsFalse() throws TextParseException {
        Message spy = spy(new Message());
        Name name = Name.fromString("nonexistent.com.");
        // Stub every overloaded call to return false.
        doReturn(false).when(spy).findRRset(name, Type.TXT, Section.ANSWER);
        doReturn(false).when(spy).findRRset(name, Type.TXT, Section.AUTHORITY);
        doReturn(false).when(spy).findRRset(name, Type.TXT, Section.ADDITIONAL);
        assertFalse(spy.findRRset(name, Type.TXT), "Overall result must be false when all sections are false");
        // All three overloads must have been invoked exactly once.
        verify(spy, times(1)).findRRset(name, Type.TXT, Section.ANSWER);
        verify(spy, times(1)).findRRset(name, Type.TXT, Section.AUTHORITY);
        verify(spy, times(1)).findRRset(name, Type.TXT, Section.ADDITIONAL);
    }

    @Test
    @DisplayName("Null name argument should throw NullPointerException")
    void nullNameArgument() {
        assertThrows(NullPointerException.class, () -> message.findRRset(null, Type.A), "Passing a null Name must result in NullPointerException");
    }

    @Test
    @DisplayName("Negative type argument should simply return false")
    void negativeTypeArgument() {
        // No exception – the method should treat it as a non‑matching type.
        assertFalse(message.findRRset(nameA, -1), "Negative type values must not match any RRset and should return false");
    }

    @Test
    @DisplayName("Zero type argument (invalid) should return false")
    void zeroTypeArgument() {
        assertFalse(message.findRRset(nameA, 0), "Type value 0 is not a defined DNS RR type and must yield false");
    }

    @Test
    @DisplayName("Maximum valid type (65535) with no matching RRset should return false")
    void maxTypeNoMatch() {
        assertFalse(message.findRRset(nameA, 65535), "Even the highest possible type value should return false when no RRset exists");
    }

    @Test
    @DisplayName("Root name with SOA record in AUTHORITY section")
    void rootNameWithSOARecord() throws TextParseException {
        // Build a minimal SOA record for the DNS root.
        // represents "."
        Name root = Name.root;
        Name mName = Name.fromString("ns.root.");
        Name rName = Name.fromString("hostmaster.root.");
        SOARecord soa = new SOARecord(root, DClass.IN, 3600, mName, rName, 2023101501L, 7200, 3600, 1209600, 3600);
        Message msg = new Message();
        msg.addRecord(soa, Section.AUTHORITY);
        assertTrue(msg.findRRset(root, Type.SOA), "A SOA record for the root should be found in the AUTHORITY section");
    }
}
