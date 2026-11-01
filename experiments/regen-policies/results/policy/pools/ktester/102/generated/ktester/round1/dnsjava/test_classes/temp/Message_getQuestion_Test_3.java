package org.xbill.DNS;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
// import static org.junit.Assert.*;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;

class Message_getQuestion_Test {

    /**
     * Message instance used by the tests.
     */
    private Message message;

    /**
     * A sample question record used by several tests.
     */
    private Record questionRecord;

    /**
     * A second question record to test ordering.
     */
    private Record secondQuestionRecord;

//     @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        // Create a fresh Message for each test.
        message = new Message();
        // Build a simple A‑record question: www.example.com. IN A
        Name qname = Name.fromConstantString("www.example.com.");
        questionRecord = Record.newRecord(qname, Type.A, DClass.IN);
        // A second question record (different name) to verify ordering.
        Name qname2 = Name.fromConstantString("mail.example.com.");
        secondQuestionRecord = Record.newRecord(qname2, Type.A, DClass.IN);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern symmetrical.
        message = null;
        questionRecord = null;
        secondQuestionRecord = null;
    }

//     @Test
//     void getQuestion_whenQuestionSectionIsEmpty_returnsNull() {
//         // By default a newly constructed Message has an empty QUESTION section.
//         assertNull(message.getQuestion());
//     }

//     @Test
//     void getQuestion_whenQuestionSectionIsNull_returnsNull() {
//         // Simulate a null QUESTION section by directly removing all records.
//         // The public API does not expose a way to set the internal array to null,
//         // but removing all records leaves the list empty, which is equivalent for the
//         // contract of getQuestion().
//         message.removeAllRecords(Section.QUESTION);
//         assertNull(message.getQuestion());
//     }

//     @Test
//     void getQuestion_withSingleRecord_returnsThatRecord() {
//         // Add a single question record.
//         message.addRecord(questionRecord, Section.QUESTION);
//         Record result = message.getQuestion();
//         assertNotNull(result);
//         assertEquals(questionRecord, result);
//         assertEquals(questionRecord.getName(), result.getName());
//     }

//     @Test
//     void getQuestion_withMultipleRecords_returnsFirstRecord() {
//         // Add two question records; the first added should be returned.
//         message.addRecord(questionRecord, Section.QUESTION);
//         message.addRecord(secondQuestionRecord, Section.QUESTION);
//         Record result = message.getQuestion();
//         assertNotNull(result);
//         assertEquals(questionRecord, result);
//     }

//     @Test
//     void getQuestion_doesNotReturnRecordsFromOtherSections() throws IOException {
//         // Add a question record to the QUESTION section.
//         message.addRecord(questionRecord, Section.QUESTION);
//         // Add a record to the ANSWER section – it must not affect getQuestion().
//         Name answerName = Name.fromConstantString("www.example.com.");
//         Record answerRecord = new ARecord(answerName, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
//         message.addRecord(answerRecord, Section.ANSWER);
//         Record result = message.getQuestion();
//         assertEquals(questionRecord, result);
//     }

//     @Test
//     void getQuestion_afterCloningPreservesQuestion() throws CloneNotSupportedException {
//         // Build a query message with a question, then clone it.
//         message.addRecord(questionRecord, Section.QUESTION);
//         Message clone = message.clone();
//         assertNotNull(clone.getQuestion());
//         assertEquals(message.getQuestion(), clone.getQuestion());
//     }

//     @Test
//     void getQuestion_parsesFromWireAndReturnsCorrectRecord() throws IOException {
//         // Construct a message, serialize it, then deserialize to ensure the wire format
//         // correctly restores the question.
//         message.addRecord(questionRecord, Section.QUESTION);
//         byte[] wire = message.toWire();
//         Message fromWire = new Message(wire);
//         Record fromWireQuestion = fromWire.getQuestion();
//         assertNotNull(fromWireQuestion);
//         assertEquals(questionRecord.getName(), fromWireQuestion.getName());
//         assertEquals(questionRecord.getType(), fromWireQuestion.getType());
//     }

//     @Test
//     void getQuestion_questionSectionContainsNullRecord_returnsNull() throws Exception {
//         // Use reflection to inject a List containing a single null element into the QUESTION slot.
//         @SuppressWarnings("unchecked")
//         List<Record>[] sections = (List<Record>[]) new List[4];
//         sections[Section.QUESTION] = Collections.singletonList(null);
//         setPrivateSections(message, sections);
//         assertNull(message.getQuestion());
//     }

//     @Test
//     void getQuestion_questionSectionIsNullArrayEntry_returnsNull() throws Exception {
//         // Simulate a null entry for the QUESTION section (the array exists, but the slot is null).
//         @SuppressWarnings("unchecked")
//         List<Record>[] sections = (List<Record>[]) new List[4];
//         // Do NOT initialise sections[Section.QUESTION]; it stays null.
//         setPrivateSections(message, sections);
//         assertNull(message.getQuestion());
//     }

//     @Test
//     void getQuestion_messageCreatedViaNewQuery_returnsQueryRecord() {
//         // newQuery should place the supplied record into the QUESTION section.
//         Message query = Message.newQuery(questionRecord);
//         Record result = query.getQuestion();
//         assertNotNull(result);
//         assertEquals(questionRecord, result);
//     }

//     @Test
//     void getQuestion_messageCreatedViaNewUpdate_returnsNull() {
//         // Update messages do not have a QUESTION section.
//         Name zone = Name.fromConstantString("example.com.");
//         Message update = Message.newUpdate(zone);
//         assertNull(update.getQuestion());
//     }

//     @Test
//     void getQuestion_questionSectionBecomesEmptyAfterRemoval_returnsNull() {
//         // Add then remove all records; the method must return null afterwards.
//         message.addRecord(questionRecord, Section.QUESTION);
//         assertNotNull(message.getQuestion());
//         message.removeAllRecords(Section.QUESTION);
//         assertNull(message.getQuestion());
//     }

//     @Test
//     void getQuestion_repeatedCallsReturnSameInstance() {
//         message.addRecord(questionRecord, Section.QUESTION);
//         Record first = message.getQuestion();
//         Record second = message.getQuestion();
//         assertSame(first, second);
//     }

//     @Test
//     void getQuestion_removeSoleRecordViaRemoveRecord_returnsNull() {
//         message.addRecord(questionRecord, Section.QUESTION);
//         assertTrue(message.removeRecord(questionRecord, Section.QUESTION));
//         assertNull(message.getQuestion());
//     }

//     @Test
//     void getQuestion_sectionsArrayIsNull_throwsNullPointerException() throws Exception {
//         // Set the entire sections array to null via reflection.
//         setPrivateSections(message, null);
//         try {
//             message.getQuestion();
//             fail("Expected NullPointerException");
//         } catch (NullPointerException e) {
//             // expected
//         }
//     }

//     @Test
//     void getQuestion_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
//         // Create an array shorter than Section.QUESTION (which is index 0) – length 0.
//         @SuppressWarnings("unchecked")
//         List<Record>[] shortArray = (List<Record>[]) new List[0];
//         setPrivateSections(message, shortArray);
//         try {
//             message.getQuestion();
//             fail("Expected ArrayIndexOutOfBoundsException");
//         } catch (ArrayIndexOutOfBoundsException e) {
//             // expected
//         }
//     }

    /* -------------------------------------------------------------------------
   * Helper method to set the private `sections` field via reflection.
   * ----------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static void setPrivateSections(Message msg, List<Record>[] sections) throws NoSuchFieldException, IllegalAccessException {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, sections);
    }
}