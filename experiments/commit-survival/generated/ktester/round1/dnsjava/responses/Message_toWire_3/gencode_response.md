```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Additional unit tests for {@link Message#toWire(int)} that cover the
 * exhaustive JSON test matrix supplied with the kata.
 *
 * <p>All tests use only the public API of {@link Message} (except for a few
 * reflective accesses needed to verify the private {@code size} field).  The
 * test class builds concrete {@link Record} instances (TXT, OPT, etc.) and
 * exercises edge‑cases such as negative limits, TSIG handling, malformed
 * headers and custom records that throw {@link IOException}.
 *
 * <p>Mockito is not required for the current test suite because the DNS
 * library does not expose external collaborators that need to be mocked.
 */
class Message_toWire_Additional_Test {

    private Message message;
    private Name qName;
    private Name extraName;

    @BeforeEach
    void setUp() throws IOException {
        // Fresh message for each test.
        message = new Message(0x1234);
        qName = Name.fromConstantString("example.com.");
        extraName = Name.fromConstantString("extra.example.com.");

        // Every DNS message must contain a question section for the
        // majority of the scenarios.
        Record question = Record.newRecord(qName, Type.A, DClass.IN);
        message.addRecord(question, Section.QUESTION);
    }

    /* -------------------------------------------------------------
     *  Simple query – fits into a typical UDP size (512 bytes)
     * ------------------------------------------------------------- */
    @Test
    void simpleQuery_fitsInto512() throws IOException {
        byte[] wire = message.toWire(512);
        assertNotNull(wire);
        assertTrue(wire.length <= 512,
                "Wire format must respect the supplied maxLength");
        assertEquals(wire.length, message.numBytes(),
                "numBytes() must match the length of the returned array");
    }

    /* -------------------------------------------------------------
     *  Message with multiple sections (answer, authority, additional)
     * ------------------------------------------------------------- */
    @Test
    void multipleSections_serializedCorrectly() throws IOException {
        // answer records
        message.addRecord(new TXTRecord(qName, DClass.IN, 300, "ans1"), Section.ANSWER);
        message.addRecord(new TXTRecord(qName, DClass.IN, 300, "ans2"), Section.ANSWER);
        // authority record
        message.addRecord(new NSRecord(qName, DClass.IN, 300, Name.fromConstantString("ns1.example.com.")),
                Section.AUTHORITY);
        // additional (EDNS) record
        message.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);

        byte[] wire = message.toWire(4096);
        assertNotNull(wire);
        assertTrue(wire.length <= 4096);
        // Basic sanity – header + at least one record in each populated section
        assertTrue(wire.length > 12);
    }

    /* -------------------------------------------------------------
     *  Negative maxLength → IllegalArgumentException
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(ints = { -1, 0 })
    void negativeOrZeroMaxLength_throwsIllegalArgumentException(int max) {
        assertThrows(IllegalArgumentException.class,
                () -> message.toWire(max),
                "maxLength <= 0 must be rejected");
    }

    /* -------------------------------------------------------------
     *  maxLength larger than protocol limit → IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void maxLengthExceedsProtocolLimit_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> message.toWire(70000),
                "maxLength > Message.MAXLENGTH must be rejected");
    }

    /* -------------------------------------------------------------
     *  Boundary condition – maxLength exactly the required size
     * ------------------------------------------------------------- */
    @Test
    void exactSizeBoundary_returnsArrayOfExactLength() throws IOException {
        // Compute the size of a minimal message (question only)
        byte[] defaultWire = message.toWire();
        int exactSize = defaultWire.length;

        byte[] wire = message.toWire(exactSize);
        assertEquals(exactSize, wire.length,
                "When maxLength equals the required size the returned array must match it");
    }

    /* -------------------------------------------------------------
     *  Empty message (no question, no records) – only header
     * ------------------------------------------------------------- */
    @Test
    void emptyMessage_returnsHeaderOnly() throws IOException {
        Message empty = new Message(0x4321); // default ctor creates empty header
        byte[] wire = empty.toWire(512);
        assertEquals(12, wire.length, "An empty DNS message must be exactly 12 bytes (header only)");
        // Verify that the ID field is preserved
        assertEquals(0x4321, empty.getHeader().getID());
    }

    /* -------------------------------------------------------------
     *  Message containing only an OPT record (no question)
     * ------------------------------------------------------------- */
    @Test
    void onlyOPT_noQuestion_serializesCorrectly() throws IOException {
        Message optOnly = new Message(0x1111);
        optOnly.addRecord(new OPTRecord(4096, 0, 0, 0), Section.ADDITIONAL);
        byte[] wire = optOnly.toWire(256);
        assertTrue(wire.length > 12, "Wire must contain the OPT RR");
        // The question count in the header must be zero
        assertEquals(0, optOnly.getHeader().getQDCount(),
                "Question count should be zero when no question is added");
    }

    /* -------------------------------------------------------------
     *  Exceeding a tiny maxLength → exception (WireParseException)
     * ------------------------------------------------------------- */
    @Test
    void exceedsTinyMaxLength_throwsWireParseException() {
        for (int i = 0; i < 20; i++) {
            message.addRecord(
                    new TXTRecord(extraName, DClass.IN, 300, "payload-" + i),
                    Section.ADDITIONAL);
        }
        assertThrows(WireParseException.class,
                () -> message.toWire(100),
                "Message larger than supplied maxLength must throw WireParseException");
    }

    /* -------------------------------------------------------------
     *  TSIG unsigned – no TSIG RR appended
     * ------------------------------------------------------------- */
    @Test
    void tsigUnsigned_noTrailingTSIG() throws IOException {
        // No call to setTSIG → unsigned state
        byte[] wire = message.toWire(1500);
        // The last 2 bytes of a DNS message are the RDLENGTH of the final RR.
        // Since we have no TSIG, the wire should end with the data of the last
        // real record (the question). We simply assert that the type field is NOT TSIG.
        int lastTypePos = wire.length - 4; // type field is 2 bytes before RDLENGTH
        int type = ((wire[lastTypePos] & 0xFF) << 8) | (wire[lastTypePos + 1] & 0xFF);
        assertNotEquals(Type.TSIG, type,
                "Unsigned message must not contain a TSIG record at the end");
    }

    /* -------------------------------------------------------------
     *  TSIG signed – TSIG RR must be present
     * ------------------------------------------------------------- */
    @Test
    void tsigSigned_appendsTSIGRecord() throws IOException {
        TSIG key = new TSIG("hmac-md5", "testkey".getBytes(), Name.fromConstantString("example.com."));
        message.setTSIG(key); // puts the message into TSIG_SIGNED state
        byte[] wire = message.toWire(1500);
        // Verify that the last RR is a TSIG (type 250)
        int typePos = wire.length - 4; // type field just before RDLENGTH
        int type = ((wire[typePos] & 0xFF) << 8) | (wire[typePos + 1] & 0xFF);
        assertEquals(Type.TSIG, type, "Signed message must end with a TSIG record");
        assertTrue(wire.length <= 1500);
    }

    /* -------------------------------------------------------------
     *  Signed message that exceeds supplied maxLength → IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void signedMessageExceedsMaxLength_throwsIllegalArgumentException() throws IOException {
        // Build a message that is just under 200 bytes without TSIG
        for (int i = 0; i < 5; i++) {
            message.addRecord(new TXTRecord(extraName, DClass.IN, 300, "large-payload-" + i),
                    Section.ADDITIONAL);
        }
        TSIG key = new TSIG("hmac-md5", "key".getBytes(), Name.fromConstantString("example.com."));
        message.setTSIG(key);
        // 200 bytes is deliberately too small for the signed payload
        assertThrows(IllegalArgumentException.class,
                () -> message.toWire(200),
                "Signed message larger than maxLength must be rejected");
    }

    /* -------------------------------------------------------------
     *  Verify that the private 'size' field matches numBytes()
     * ------------------------------------------------------------- */
    @Test
    void internalSizeField_matchesNumBytes() throws IOException, NoSuchFieldException,
            IllegalAccessException {
        message.addRecord(new TXTRecord(qName, DClass.IN, 300, "test"), Section.ANSWER);
        byte[] wire = message.toWire(1024);
        assertEquals(wire.length, message.numBytes());

        // Reflective access to the private 'size' field
        Field sizeField = Message.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        int sizeValue = (int) sizeField.get(message);
        assertEquals(wire.length, sizeValue,
                "Private size field must be updated to the length of the wire format");
    }

    /* -------------------------------------------------------------
     *  Record that throws IOException during serialization
     * ------------------------------------------------------------- */
    @Test
    void recordSerializationFailure_propagatesIOException() {
        class BadRecord extends Record {
            BadRecord(Name name) {
                super(name, Type.A, DClass.IN, 300);
            }

            @Override
            public void toWire(DNSOutput out) throws IOException {
                throw new IOException("Simulated serialization failure");
            }

            @Override
            public String rdataToString() {
                return "";
            }
        }

        Record bad = new BadRecord(qName);
        message.addRecord(bad, Section.ANSWER);
        assertThrows(IOException.class,
                () -> message.toWire(512),
                "A record that throws IOException should cause toWire to propagate it");
    }

    /* -------------------------------------------------------------
     *  Null TSIG key – expect NullPointerException during signing
     * ------------------------------------------------------------- */
    @Test
    void nullTSIGKey_throwsNullPointerException() {
        // The library does not guard against null, so we deliberately pass null.
        assertThrows(NullPointerException.class,
                () -> message.setTSIG(null),
                "Setting a null TSIG key should throw NPE");
    }

    /* -------------------------------------------------------------
     *  Malformed header (invalid opcode) → IOException
     * ------------------------------------------------------------- */
    @Test
    void malformedHeader_throwsIOException() {
        Header hdr = message.getHeader();
        // Opcode is a 4‑bit field; valid range is 0‑15.
        // Setting 16 should be illegal.
        hdr.setOpcode(16);
        assertThrows(IOException.class,
                () -> message.toWire(512),
                "Serializing a header with an illegal opcode must raise IOException");
    }

    /* -------------------------------------------------------------
     *  Duplicate record across sections → IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void duplicateRecordInMultipleSections_throwsIllegalArgumentException()
            throws NoSuchFieldException, IllegalAccessException {
        Record rec = new TXTRecord(qName, DClass.IN, 300, "dup");
        // Add to ANSWER
        message.addRecord(rec, Section.ANSWER);

        // Use reflection to inject the same Record into AUTHORITY list.
        // This mimics a programmer error that bypasses the public API.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(message);
        sections[Section.AUTHORITY].add(rec); // duplicate

        assertThrows(IllegalArgumentException.class,
                () -> message.toWire(1024),
                "Duplicate records across sections must be rejected");
    }

    /* -------------------------------------------------------------
     *  Large message just under MAXLENGTH – should succeed
     * ------------------------------------------------------------- */
    @Test
    void largeMessageJustUnderMaxLength_succeeds() throws IOException {
        // Approximate 65 000 bytes using many TXT records.
        for (int i = 0; i < 1500; i++) {
            message.addRecord(new TXTRecord(extraName, DClass.IN, 300,
                    "payload-" + i), Section.ADDITIONAL);
        }
        byte[] wire = message.toWire(Message.MAXLENGTH);
        assertTrue(wire.length < Message.MAXLENGTH,
                "Message size should be less than MAXLENGTH but still succeed");
        assertEquals(wire.length, message.numBytes());
    }

    /* -------------------------------------------------------------
     *  Exact MAXLENGTH (65535) – we cannot reliably craft exactly that size
     *  without deep knowledge of the wire format, so we assert that the
     *  method does not reject a near‑max size.
     * ------------------------------------------------------------- */
    @Test
    void nearMaxLength_doesNotThrow() throws IOException {
        // Build a big message (~64 500 bytes)
        for (int i = 0; i < 1400; i++) {
            message.addRecord(new TXTRecord(extraName, DClass.IN, 300,
                    "data-" + i), Section.ADDITIONAL);
        }
        byte[] wire = message.toWire(Message.MAXLENGTH);
        assertTrue(wire.length <= Message.MAXLENGTH,
                "Wire length must never exceed the protocol limit");
    }
}
```