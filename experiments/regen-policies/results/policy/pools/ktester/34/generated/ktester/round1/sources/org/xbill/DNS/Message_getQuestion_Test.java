package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.*;
import org.xbill.DNS.utils.base64;

@ExtendWith(MockitoExtension.class)
class Message_getQuestion_Test {

    /**
     * The maximum number of sections defined by the library.
     * The actual value is internal; we use a small safe constant (4) which
     * matches the known sections: QUESTION, ANSWER, AUTHORITY, ADDITIONAL.
     */
    private static final int SECTION_ARRAY_SIZE = 4;

    @Test
    void getQuestion_EmptySection_ReturnsNull() {
        // all sections are empty by default
        Message msg = new Message();
        assertNull(msg.getQuestion(), "Expected null when QUESTION section is null or empty");
    }

    @Test
    void getQuestion_SingleRecord_ReturnsThatRecord() {
        Name name = Name.fromConstantString("example.com.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message msg = new Message();
        msg.addRecord(question, Section.QUESTION);
        Record result = msg.getQuestion();
        assertNotNull(result, "Expected a non‑null Record");
        assertEquals(question, result, "The returned Record should be the one that was added");
    }

    @Test
    void getQuestion_MultipleRecords_ReturnsFirstRecord() {
        Name name1 = Name.fromConstantString("first.example.com.");
        Name name2 = Name.fromConstantString("second.example.com.");
        Record q1 = Record.newRecord(name1, Type.A, DClass.IN);
        Record q2 = Record.newRecord(name2, Type.AAAA, DClass.IN);
        Message msg = new Message();
        msg.addRecord(q1, Section.QUESTION);
        // second question record
        msg.addRecord(q2, Section.QUESTION);
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
        byte[] wire = base64.fromString("EEuBgAABAAEABAAIA3d3dwZnb29nbGUDY29tAAABAAHADAABAAEAAAAaAASO+rokwBAAAgABAAFHCwAGA25zMcAQwBAAAgABAAFHCwAGA25zNMAQwBAAAgABAAFHCwAGA25zM8AQwBAAAgABAAFHCwAGA25zMsAQwDwAAQABAADObwAE2O8gCsByAAEAAQABrVEABNjvIgrAYAABAAEAAVqZAATY7yQKwE4AAQABAAK9RQAE2O8mCsA8ABwAAQAD4a0AECABSGBIAgAyAAAAAAAAAArAcgAcAAEAAtDgABAgAUhgSAIANAAAAAAAAAAKwGAAHAABAACSagAQIAFIYEgCADYAAAAAAAAACsBOABwAAQAErVoAECABSGBIAgA4AAAAAAAAAAo=");
        // Prepend length as Message(ByteBuffer) expects the length to have been stripped off.
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(wire.length + 2);
        buf.putShort((short) wire.length);
        buf.put(wire);
        buf.flip();
        // discard the length prefix
        buf.getShort();
        Message msg = new Message(buf);
        Record q = msg.getQuestion();
        assertNotNull(q, "Question should be present after parsing");
        assertEquals(Name.fromConstantString("www.google.com."), q.getName(), "Parsed question name should match expected");
    }

    @Test
    void getQuestion_QuestionListFirstElementNull_ReturnsNull() throws Exception {
        // Build a Message with a QUESTION list whose first entry is null.
        Message msg = new Message();
        // Use reflection to replace the QUESTION section with a custom list.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        List<Record> customList = new ArrayList<>();
        // first element = null
        customList.add(null);
        customList.add(Record.newRecord(Name.fromConstantString("valid.example."), Type.A, DClass.IN));
        // Ensure the array is large enough (Section.QUESTION is 0 in the library)
        if (sections == null || sections.length <= Section.QUESTION) {
            // recreate a correctly sized array if the constructor left it null
            sections = (List<Record>[]) new List[SECTION_ARRAY_SIZE];
            sectionsField.set(msg, sections);
        }
        sections[Section.QUESTION] = customList;
        // The method should see a non‑empty list and return the first element (null)
        Record result = msg.getQuestion();
        assertNull(result, "When the first element of the QUESTION list is null, getQuestion() should return null");
    }

    @Test
    void getQuestion_InternalSectionsArrayIsNull_ThrowsNullPointerException() throws Exception {
        Message msg = new Message();
        // Force the private 'sections' field to be null.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(msg, null);
        // Expect a NPE because the method tries to index into a null array.
        assertThrows(NullPointerException.class, msg::getQuestion, "Accessing getQuestion() with a null internal sections array should throw NullPointerException");
    }

    @Test
    void getQuestion_InternalSectionsArrayTooShort_ThrowsArrayIndexOutOfBoundsException() throws Exception {
        Message msg = new Message();
        // Replace with an empty array (length 0). Section.QUESTION is 0, so indexing fails.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] emptyArray = (List<Record>[]) new List[0];
        sectionsField.set(msg, emptyArray);
        assertThrows(ArrayIndexOutOfBoundsException.class, msg::getQuestion, "Accessing getQuestion() when sections array is too short should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void getQuestion_UnderlyingListThrows_PropagatesException() throws Exception {
        Message msg = new Message();
        // Mock a List that pretends to contain one element but throws on get(0).
        @SuppressWarnings("unchecked")
        List<Record> throwingList = mock(List.class);
        when(throwingList.isEmpty()).thenReturn(false);
        // get(0) will throw; size() is not used by getQuestion() and therefore not stubbed.
        when(throwingList.get(0)).thenThrow(new IllegalStateException("boom"));
        // Inject the mocked list into the QUESTION section via reflection.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        if (sections == null || sections.length <= Section.QUESTION) {
            sections = (List<Record>[]) new List[SECTION_ARRAY_SIZE];
            sectionsField.set(msg, sections);
        }
        sections[Section.QUESTION] = throwingList;
        IllegalStateException ex = assertThrows(IllegalStateException.class, msg::getQuestion, "Exception from the underlying list should propagate");
        assertEquals("boom", ex.getMessage());
    }
}
