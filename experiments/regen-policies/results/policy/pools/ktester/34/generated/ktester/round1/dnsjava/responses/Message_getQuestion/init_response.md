```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Unit tests for {@link Message#getQuestion()}.
 *
 * The tests cover the following scenarios:
 * <ul>
 *   <li>When the QUESTION section is empty the method returns {@code null}.</li>
 *   <li>When a single question record is present it is returned.</li>
 *   <li>When multiple question records are present the first one is returned.</li>
 *   <li>When a message is created via {@code Message.newQuery(Record)} the question is correctly
 *       populated.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Message_getQuestion_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getQuestion_EmptySection_ReturnsNull() {
        Message msg = new Message();               // all sections are empty
        assertNull(msg.getQuestion(), "Expected null when QUESTION section is empty");
    }

    @Test
    void getQuestion_SingleRecord_ReturnsThatRecord() {
        Name name = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = new Message();
        msg.addRecord(question, Section.QUESTION);

        Record result = msg.getQuestion();
        assertNotNull(result, "Expected a non‑null Record");
        assertEquals(question, result, "The returned Record should be the one added");
    }

    @Test
    void getQuestion_MultipleRecords_ReturnsFirstRecord() {
        Name name1 = Name.fromConstantString("first.example.com.");
        Name name2 = Name.fromConstantString("second.example.com.");
        Record q1 = Record.newRecord(name1, Type.A, DClass.IN);
        Record q2 = Record.newRecord(name2, Type.AAAA, DClass.IN);

        Message msg = new Message();
        msg.addRecord(q1, Section.QUESTION);
        msg.addRecord(q2, Section.QUESTION); // second question record

        Record result = msg.getQuestion();
        assertNotNull(result);
        assertEquals(q1, result, "The first question record should be returned");
    }

    @Test
    void getQuestion_NewQueryFactory_ReturnsProvidedQuestion() {
        Name name = Name.fromConstantString("www.google.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);

        Message query = Message.newQuery(question);
        Record result = query.getQuestion();

        assertNotNull(result);
        assertEquals(question, result);
    }

    @Test
    void getQuestion_ParsedFromWire_ReturnsCorrectQuestion() throws IOException {
        // This is the same wire data used in the original MessageTest ctor_byteBuffer test.
        byte[] wire = base64.fromString(
                "EEuBgAABAAEABAAIA3d3dwZnb29nbGUDY29tAAABAAHADAABAAEAAAAaAASO+rokwBAAAgABAAFHCwAGA25zMcAQwBAAAgABAAFHCwAGA25zNMAQwBAAAgABAAFHCwAGA25zM8AQwBAAAgABAAFHCwAGA25zMsAQwDwAAQABAADObwAE2O8gCsByAAEAAQABrVEABNjvIgrAYAABAAEAAVqZAATY7yQKwE4AAQABAAK9RQAE2O8mCsA8ABwAAQAD4a0AECABSGBIAgAyAAAAAAAAAArAcgAcAAEAAtDgABAgAUhgSAIANAAAAAAAAAAKwGAAHAABAACSagAQIAFIYEgCADYAAAAAAAAACsBOABwAAQAErVoAECABSGBIAgA4AAAAAAAAAAo="
        );

        // Prepend length as Message(ByteBuffer) expects the length to have been stripped off.
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(wire.length + 2);
        buf.putShort((short) wire.length);
        buf.put(wire);
        buf.flip();
        buf.getShort(); // discard the length prefix

        Message msg = new Message(buf);
        Record q = msg.getQuestion();

        assertNotNull(q, "Question should be present after parsing");
        assertEquals(Name.fromConstantString("www.google.com."), q.getName(),
                "Parsed question name should match expected");
    }
}
```