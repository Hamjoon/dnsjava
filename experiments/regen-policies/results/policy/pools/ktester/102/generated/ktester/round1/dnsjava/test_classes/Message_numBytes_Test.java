package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xbill.DNS.utils.base64;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Message_numBytes_Test {

    /**
     * Parameter source for reflection manipulation tests.
     *
     * @return stream of arguments: (size to set, expected result)
     */
    private static Stream<Arguments> reflectionSizeProvider() {
        return Stream.of(// Positive value
        Arguments.of(12345, 12345), // Negative value – the method does not guard against it
        Arguments.of(-1, -1), // Lower boundary
        Arguments.of(0, 0), // Upper DNS limit (Message.MAXLENGTH)
        Arguments.of(Message.MAXLENGTH, Message.MAXLENGTH));
    }

    /**
     * Verify that a freshly created Message reports a size of 0 bytes.
     */
    @Test
    void defaultMessage_hasZeroNumBytes() {
        // uses the no‑arg constructor
        Message m = new Message();
        assertEquals(0, m.numBytes(), "A newly constructed Message should have size 0");
    }

    /**
     * Verify that a Message constructed from a raw DNS wire format reports the
     * correct number of bytes (the length of the supplied byte array).
     */
    @Test
    void messageFromByteArray_reportsCorrectNumBytes() throws Exception {
        // This is the same base64 payload used in the upstream MessageTest.
        byte[] wire = base64.fromString("EEuBgAABAAEABAAIA3d3dwZnb29nbGUDY29tAAABAAHADAABAAEAAAAaAASO+rokwBAAAgABAAFHCwAGA25zMcAQwBAAAgABAAFHCwAGA25zNMAQwBAAAgABAAFHCwAGA25zM8AQwBAAAgABAAFHCwAGA25zMsAQwDwAAQABAADObwAE2O8gCsByAAEAAQABrVEABNjvIgrAYAABAAEAAVqZAATY7yQKwE4AAQABAAK9RQAE2O8mCsA8ABwAAQAD4a0AECABSGBIAgAyAAAAAAAAAArAcgAcAAEAAtDgABAgAUhgSAIANAAAAAAAAAAKwGAAHAABAACSagAQIAFIYEgCADYAAAAAAAAACsBOABwAAQAErVoAECABSGBIAgA4AAAAAAAAAAo=");
        Message m = new Message(wire);
        assertEquals(wire.length, m.numBytes(), "Message.numBytes() should equal the length of the wire‑format byte array");
    }

    /**
     * Set the private {@code size} field via reflection and verify that {@code numBytes()}
     * returns the same value.
     *
     * @param sizeValue the value to inject into the private field
     * @param expected  the expected return value from {@code numBytes()}
     */
    @ParameterizedTest(name = "size={0}")
    @MethodSource("reflectionSizeProvider")
    void numBytes_reflectionManipulation(int sizeValue, int expected) throws Exception {
        // default constructor, size initially 0
        Message m = new Message();
        // Use reflection to set the private field `size`
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        sizeField.setInt(m, sizeValue);
        assertEquals(expected, m.numBytes(), "numBytes() should return the value injected via reflection");
    }

    /**
     * An empty Message should still report size 0 after {@code toWire()} is called.
     */
    @Test
    void toWire_onEmptyMessage_doesNotChangeNumBytes() {
        // empty message
        Message m = new Message();
        // serialize – should contain only the header
        byte[] wire = m.toWire();
        assertNotNull(wire, "toWire() must return a non‑null byte array");
        assertEquals(0, m.numBytes(), "Calling toWire() on an empty Message must not modify the internal size");
    }

    /**
     * Adding a single simple DNS record (A record) should make {@code numBytes()}
     * at least the minimal DNS message length (12‑byte header). The exact size
     * depends on the record content.
     */
    @Test
    void toWire_afterAddingOneRecord_returnsAtLeastHeaderSize() throws Exception {
        Message m = new Message();
        // Create a minimal A record: example.com. IN A 1.2.3.4
        Name name = Name.fromString("example.com.");
        Record aRecord = new ARecord(name, DClass.IN, 300, InetAddress.getByName("1.2.3.4"));
        // Add the record to the ANSWER section
        m.addRecord(aRecord, Section.ANSWER);
        // Serialize the message
        byte[] wire = m.toWire();
        assertNotNull(wire, "toWire() must return a non‑null byte array after adding a record");
        int reportedSize = m.numBytes();
        assertTrue(reportedSize >= 12, "After adding a record, numBytes() should be >= 12 (minimum DNS header size). Reported: " + reportedSize);
    }
}
