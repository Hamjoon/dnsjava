```java
package org.xbill.DNS;

import java.lang.reflect.Field;
import java.net.InetAddress;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_numBytes_Test {

    /**
     * Verify that a freshly created Message (no‑arg constructor) reports a size of 0.
     */
    @Test
    void numBytes_defaultConstructor() {
        Message m = new Message();
        assertEquals(0, m.numBytes(),
                "Message created with the default constructor should have size 0");
    }

    /**
     * Verify that a Message created with the ID‑based constructor also reports size 0.
     */
    @Test
    void numBytes_idConstructor() {
        Message m = new Message(0x1234);
        assertEquals(0, m.numBytes(),
                "Message created with the id‑based constructor should have size 0");
    }

    /**
     * Verify that when a Message is constructed from a wire‑format byte array,
     * {@code numBytes()} returns the exact number of bytes read (i.e. the length of the array).
     */
    @Test
    void numBytes_fromWire() throws Exception {
        // Build a simple query message
        Name qname = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        // Serialize the message to wire format (default max length)
        byte[] wire = query.toWire();
        // Parse the wire representation back into a Message
        Message parsed = new Message(wire);
        // The size stored inside the parsed message should equal the wire length
        assertEquals(wire.length, parsed.numBytes(),
                "Parsed Message should report size equal to the wire array length");
    }

    /**
     * Verify that cloning a Message preserves the size reported by {@code numBytes()}.
     */
    @Test
    void numBytes_clonePreservesSize() throws Exception {
        // Create a message with a question and one additional record
        Name qname = Name.fromConstantString("example.org.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message original = Message.newQuery(question);
        original.addRecord(new TXTRecord(qname, DClass.IN, 3600, "test"), Section.ADDITIONAL);
        // Serialize and re‑parse to ensure the internal size field is set
        byte[] wire = original.toWire();
        Message parsed = new Message(wire);
        // Clone the parsed message
        Message clone = parsed.clone();
        assertEquals(parsed.numBytes(), clone.numBytes(),
                "Cloned Message should have the same size as the original parsed Message");
    }

    /**
     * Set the private {@code size} field via reflection and verify that {@code numBytes()}
     * returns the injected value unchanged.
     */
    @ParameterizedTest(name = "{index} => injectedSize={0}")
    @CsvSource({ "12345", "-1" })
    void numBytes_reflectionInjectedSize(int injectedSize) throws Exception {
        Message m = new Message();
        // Access the private field 'size'
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        sizeField.setInt(m, injectedSize);
        assertEquals(injectedSize, m.numBytes(),
                "numBytes() should return the raw value set via reflection");
    }

    /**
     * Adding a real record should increase the internal size (i.e. {@code numBytes()} > 0).
     */
    @Test
    void numBytes_afterAddingRecord_increases() throws Exception {
        Message m = new Message();
        assertEquals(0, m.numBytes(), "Initial size must be 0");
        Name name = Name.fromConstantString("test.example.");
        Record a = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("127.0.0.1"));
        m.addRecord(a, Section.ANSWER);
        assertTrue(m.numBytes() > 0,
                "After adding a record the size reported by numBytes() should be > 0");
    }

    /**
     * Adding a {@code null} record must not modify the size.
     */
    @Test
    void numBytes_afterAddingNullRecord_unchanged() {
        Message m = new Message();
        assertEquals(0, m.numBytes(), "Initial size must be 0");
        // According to the API contract, a null record is ignored.
        m.addRecord(null, Section.ANSWER);
        assertEquals(0, m.numBytes(),
                "Adding a null record must leave the size unchanged");
    }

    /**
     * Setting a fresh Header does not affect the size.
     */
    @Test
    void numBytes_setHeader_unchangedOnEmptyMessage() {
        Message m = new Message();
        assertEquals(0, m.numBytes(), "Empty message should start with size 0");
        Header h = new Header();
        m.setHeader(h);
        assertEquals(0, m.numBytes(),
                "Setting a Header on an empty Message must not change its size");
    }

    /**
     * Setting a Header after the message already contains data must keep the size non‑zero.
     */
    @Test
    void numBytes_setHeader_nonZeroAfterAddingRecord() {
        Message m = new Message();
        Name name = Name.fromConstantString("header.test.");
        Record txt = new TXTRecord(name, DClass.IN, 300, "payload");
        m.addRecord(txt, Section.ANSWER);
        int sizeAfterRecord = m.numBytes();
        assertTrue(sizeAfterRecord > 0, "Size must be >0 after adding a record");
        Header newHeader = new Header();
        newHeader.setID(0xABCD);
        m.setHeader(newHeader);
        assertTrue(m.numBytes() > 0,
                "Replacing the Header must not reset the size; it should stay non‑zero");
    }

    /**
     * Adding a TSIG key should increase the size.  The test uses a real {@link TSIG}
     * instance with a simple HMAC‑MD5 key.
     */
    @Test
    void numBytes_setTSIG_nonZero() throws Exception {
        Message m = new Message();
        // No records yet – size should be 0
        assertEquals(0, m.numBytes());
        // Create a minimal TSIG key (name, algorithm, secret)
        Name keyName = Name.fromConstantString("tsigkey.example.");
        // The algorithm name used by dnsjava for HMAC‑MD5
        Name alg = Name.fromString("hmac-md5.");
        TSIG tsig = new TSIG(keyName, alg, "secret".getBytes());
        // Adding the TSIG record
        m.setTSIG(tsig);
        // The size must now be >0 (the TSIG record contributes to the wire length)
        assertTrue(m.numBytes() > 0,
                "Calling setTSIG with a valid key must increase the internal size");
    }

    /**
     * Calling {@code setTSIG(null)} must leave the size unchanged.
     */
    @Test
    void numBytes_setTSIG_nullUnchanged() {
        Message m = new Message();
        assertEquals(0, m.numBytes());
        // should be ignored
        m.setTSIG(null);
        assertEquals(0, m.numBytes(),
                "Calling setTSIG with null must not modify the size");
    }

    /**
     * Removing a previously added record should bring the size back to 0.
     */
    @Test
    void numBytes_removeRecord_backToZero() throws Exception {
        Message m = new Message();
        Name name = Name.fromConstantString("remove.test.");
        Record a = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("192.0.2.1"));
        m.addRecord(a, Section.ANSWER);
        assertTrue(m.numBytes() > 0, "Size must be >0 after adding a record");
        boolean removed = m.removeRecord(a, Section.ANSWER);
        assertTrue(removed, "removeRecord should return true for an existing record");
        assertEquals(0, m.numBytes(),
                "After removal the size should be back to 0");
    }

    /**
     * Attempting to remove a non‑existent record must not change the size.
     */
    @Test
    void numBytes_removeNonExistentRecord_unchanged() throws Exception {
        Message m = new Message();
        Name name = Name.fromConstantString("absent.test.");
        Record a = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("198.51.100.2"));
        // No record added yet
        assertEquals(0, m.numBytes());
        boolean removed = m.removeRecord(a, Section.ANSWER);
        assertFalse(removed,
                "removeRecord should return false for a missing record");
        assertEquals(0, m.numBytes(),
                "Size must stay unchanged when removing a record that was never added");
    }

    /**
     * Cloning a non‑empty Message must preserve the size.
     */
    @Test
    void numBytes_cloneSameSize() throws Exception {
        Message m = new Message();
        Name name = Name.fromConstantString("clone.test.");
        Record txt = new TXTRecord(name, DClass.IN, 300, "clone payload");
        m.addRecord(txt, Section.ANSWER);
        int originalSize = m.numBytes();
        assertTrue(originalSize > 0, "Original message must have non‑zero size");
        Message clone = m.clone();
        assertEquals(originalSize, clone.numBytes(),
                "Clone must report the same size as the original");
    }

    /**
     * Cloning an empty Message must result in size 0 for both instances.
     */
    @Test
    void numBytes_cloneEmptyMessage() throws Exception {
        Message empty = new Message();
        assertEquals(0, empty.numBytes());
        Message clone = empty.clone();
        assertEquals(0, clone.numBytes(),
                "Clone of an empty Message must also have size 0");
    }

    /**
     * After serialisation via {@code toWire()}, the reported size must equal the length of the
     * resulting byte array.
     */
    @Test
    void numBytes_afterToWire_equalsByteArrayLength() throws Exception {
        Message m = new Message();
        Name name = Name.fromConstantString("wire.test.");
        Record a = new ARecord(name, DClass.IN, 300L, InetAddress.getByName("203.0.113.5"));
        m.addRecord(a, Section.ANSWER);
        byte[] wire = m.toWire();
        assertEquals(wire.length, m.numBytes(),
                "numBytes() must match the length of the wire representation");
    }

    /**
     * Serialising with a maximum length (truncation) must result in {@code numBytes()}
     * being less than or equal to that limit.
     */
    @Test
    void numBytes_toWireWithMaxLength_truncated() {
        Message m = new Message();
        Name name = Name.fromConstantString("big.test.");
        // Add many TXT records to exceed 1024 bytes
        for (int i = 0; i < 200; i++) {
            m.addRecord(new TXTRecord(name, DClass.IN, 300, "payload-" + i), Section.ANSWER);
        }
        int maxLen = 1024;
        byte[] wire = m.toWire(maxLen);
        assertTrue(wire.length <= maxLen,
                "Wire array length must not exceed the specified maxLength");
        assertTrue(m.numBytes() <= maxLen,
                "Message.numBytes() must reflect the possibly truncated size");
    }

    /**
     * Normalisation must never increase the size of a Message.
     */
    @Test
    void numBytes_normalize_nonIncreasing() throws Exception {
        // Build a message with an extra (irrelevant) record in the ADDITIONAL section
        Message query = Message.newQuery(
                Record.newRecord(Name.fromConstantString("norm.test."), Type.A, DClass.IN));
        Name extraName = Name.fromConstantString("extra.test.");
        Record extra = new TXTRecord(extraName, DClass.IN, 300, "irrelevant");
        query.addRecord(extra, Section.ADDITIONAL);
        int before = query.numBytes();
        // Normalise against itself – this should strip the irrelevant additional record
        Message normalized = query.normalize(query);
        int after = normalized.numBytes();
        assertTrue(after <= before,
                "After normalization the size must be less than or equal to the original size");
    }

    /**
     * Assigning a mock {@link Resolver} must not affect the internal size.
     */
    @Test
    void numBytes_setResolver_unchanged(@Mock Resolver mockResolver) {
        Message m = new Message();
        assertEquals(0, m.numBytes());
        m.setResolver(mockResolver);
        assertEquals(0, m.numBytes(),
                "Setting a Resolver should not modify the message size");
    }
}
```