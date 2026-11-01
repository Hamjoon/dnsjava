import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_clone_Test {

    class NonCloneableMessage extends Message {

        @Override
        public Message clone() {
            // Force a CloneNotSupportedException by calling Object.clone via reflection
            try {
                Method m = Object.class.getDeclaredMethod("clone");
                m.setAccessible(true);
                return (Message) m.invoke(this);
            } catch (Exception e) {
                // Wrap any reflection exception as a RuntimeException – the test expects the original
                // CloneNotSupportedException to be thrown, which will be the cause.
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Helper subclass that deliberately disables {@link Message}'s {@code clone()}
     * implementation by invoking {@code Object.clone()} via reflection.
     * Used in the {@code testCloneSuperCloneThrowsCloneNotSupportedException} test.
     */
    class NonCloneableMessage extends Message {

        @Override
        public Message clone() {
            // Force a CloneNotSupportedException by calling Object.clone via reflection
            try {
                Method m = Object.class.getDeclaredMethod("clone");
                m.setAccessible(true);
                return (Message) m.invoke(this);
            } catch (Exception e) {
                // Wrap any reflection exception as a RuntimeException – the test expects the original
                // CloneNotSupportedException to be thrown, which will be the cause.
                throw new RuntimeException(e);
            }
        }
    }

    class NonCloneableMessage extends Message {

        @Override
        public Message clone() {
            // Force a CloneNotSupportedException by calling Object.clone via reflection
            try {
                Method m = Object.class.getDeclaredMethod("clone");
                m.setAccessible(true);
                return (Message) m.invoke(this);
            } catch (Exception e) {
                // Wrap any reflection exception as a RuntimeException – the test expects the original
                // CloneNotSupportedException to be thrown, which will be the cause.
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Sets a private field on the given object.
     *
     * @param target
     * 		the object whose field should be modified
     * @param fieldName
     * 		the name of the private field
     * @param value
     * 		the value to set
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves the value of a private field.
     *
     * @param target
     * 		the object containing the field
     * @param fieldName
     * 		the name of the private field
     * @return the field value
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Verify that cloning a query message copies the header ID and the question record,
     * while producing distinct Header and Message objects.
     */
    @Test
    void testCloneQuestionMessage() {
        Name qname = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        Message clone = query.clone();
        // Header ID must be equal but Header objects must be different
        assertEquals(query.getHeader().getID(), clone.getHeader().getID(), "Header IDs should be equal");
        assertNotSame(query.getHeader(), clone.getHeader(), "Header instances must be distinct");
        // Question record must be equal (same name, type, class) but not the same object
        assertEquals(query.getQuestion(), clone.getQuestion(), "Question records should be equal");
        assertNotSame(query.getQuestion(), clone.getQuestion(), "Question record instances must be distinct");
    }

    /**
     * Verify that cloning a response message copies all sections deeply.
     * Adding a record to the cloned message must not affect the original.
     */
    @Test
    void testCloneResponseDeepCopySections() throws UnknownHostException {
        // Build a response with records in QUESTION, ANSWER and ADDITIONAL sections
        Name name = Name.fromConstantString("www.example.");
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Record answer = new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        Record additional = new TXTRecord(name, DClass.IN, 0, "extra");
        Message response = new Message();
        // mark as response
        response.getHeader().setFlag(Flags.QR);
        response.addRecord(question, Section.QUESTION);
        response.addRecord(answer, Section.ANSWER);
        response.addRecord(additional, Section.ADDITIONAL);
        Message clone = response.clone();
        // Verify that each section list is a different object
        for (int sec = 0; sec < 4; sec++) {
            List<Record> originalSec = response.getSection(sec);
            List<Record> cloneSec = clone.getSection(sec);
            assertNotSame(originalSec, cloneSec, ("Section list for section " + sec) + " must be a new instance");
            assertEquals(originalSec, cloneSec, ("Contents of section " + sec) + " must be equal after cloning");
        }
        // Mutate the clone and ensure original stays unchanged
        Record newAnswer = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.1"));
        clone.addRecord(newAnswer, Section.ANSWER);
        assertEquals(1, response.getSection(Section.ANSWER).size(), "Original answer section must remain unchanged");
        assertEquals(2, clone.getSection(Section.ANSWER).size(), "Clone must contain the newly added record");
    }

    /**
     * Group: Clone with empty sections and no TSIG records
     */
    @Test
    void testCloneEmptySectionsNoTSIG() throws Exception {
        Message original = new Message();
        // Ensure sections array exists but all entries are null
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(original, new List[Message.Section.values().length]);
        // Ensure header is non‑null
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(original, new Header());
        Message clone = original.clone();
        // Sections array length must match
        List<?>[] originalSections = (List<?>[]) sectionsField.get(original);
        List<?>[] cloneSections = (List<?>[]) sectionsField.get(clone);
        assertEquals(originalSections.length, cloneSections.length, "Sections array length must be preserved");
        // All entries must be null in both arrays
        for (int i = 0; i < originalSections.length; i++) {
            assertNull(originalSections[i], ("Original section entry " + i) + " should be null");
            assertNull(cloneSections[i], ("Cloned section entry " + i) + " should be null");
        }
        // Header must be a distinct clone with equal content
        Header originalHeader = original.getHeader();
        Header clonedHeader = clone.getHeader();
        assertNotSame(originalHeader, clonedHeader, "Header instances must differ");
        assertEquals(originalHeader, clonedHeader, "Header content must be equal");
        // TSIG fields must be null
        assertNull(getPrivateField(clone, "querytsig"));
        assertNull(getPrivateField(clone, "generatedTsig"));
    }

    /**
     * Group: Clone with populated sections (non‑null lists) and no TSIG
     */
    @Test
    void testClonePopulatedSectionsNoTSIG() throws Exception {
        Message original = new Message();
        // Populate each section with a single record
        Name name = Name.fromConstantString("example.com.");
        Record q = Record.newRecord(name, Type.A, DClass.IN);
        Record a = new ARecord(name, DClass.IN, 0, InetAddress.getByName("1.2.3.4"));
        Record txt = new TXTRecord(name, DClass.IN, 0, "test");
        original.addRecord(q, Section.QUESTION);
        original.addRecord(a, Section.ANSWER);
        original.addRecord(txt, Section.ADDITIONAL);
        Message clone = original.clone();
        // Verify deep copy of each non‑null list
        for (int sec = 0; sec < 4; sec++) {
            List<Record> origList = original.getSection(sec);
            List<Record> cloneList = clone.getSection(sec);
            if (origList == null) {
                assertNull(cloneList, ("Cloned list for null original section " + sec) + " must be null");
            } else {
                assertNotSame(origList, cloneList, ("Section list " + sec) + " must be a new instance");
                assertEquals(origList, cloneList, ("Section list " + sec) + " contents must be equal");
            }
        }
        // Mutate original list and ensure clone is unaffected
        Record extra = new ARecord(name, DClass.IN, 0, InetAddress.getByName("5.6.7.8"));
        original.addRecord(extra, Section.ANSWER);
        assertEquals(2, original.getSection(Section.ANSWER).size(), "Original answer section should now have two records");
        assertEquals(1, clone.getSection(Section.ANSWER).size(), "Clone's answer section must remain unchanged");
    }

    /**
     * Group: Clone with mixed null and non‑null sections
     */
    @Test
    void testCloneMixedNullAndNonNullSections() throws Exception {
        Message original = new Message();
        // Manually set sections array with a mixture
        @SuppressWarnings("unchecked")
        List<Record>[] mixed = new List[4];
        mixed[Section.QUESTION] = new LinkedList<>(Collections.singletonList(Record.newRecord(Name.fromConstantString("mix.example."), Type.A, DClass.IN)));
        mixed[Section.ANSWER] = null;
        mixed[Section.AUTHORITY] = new LinkedList<>();
        mixed[Section.ADDITIONAL] = null;
        setPrivateField(original, "sections", mixed);
        Message clone = original.clone();
        @SuppressWarnings("unchecked")
        List<?>[] cloned = (List<?>[]) getPrivateField(clone, "sections");
        assertEquals(mixed.length, cloned.length, "Sections array length must be preserved");
        // Verify null positions stay null and non‑null become new lists
        assertNotNull(cloned[Section.QUESTION], "Question section should be non‑null");
        assertNotSame(mixed[Section.QUESTION], cloned[Section.QUESTION], "Question list must be a new LinkedList instance");
        assertEquals(mixed[Section.QUESTION], cloned[Section.QUESTION], "Question list contents must be equal");
        assertNull(cloned[Section.ANSWER], "Answer section should remain null");
        assertNotNull(cloned[Section.AUTHORITY], "Authority section should be non‑null");
        assertNotSame(mixed[Section.AUTHORITY], cloned[Section.AUTHORITY], "Authority empty list must be a new instance");
        assertTrue(cloned[Section.AUTHORITY].isEmpty(), "Authority list should stay empty");
        assertNull(cloned[Section.ADDITIONAL], "Additional section should remain null");
    }

    /**
     * Group: Clone with empty but non‑null sections
     */
    @Test
    void testCloneEmptyButNonNullSections() throws Exception {
        Message original = new Message();
        // All sections are empty LinkedLists (non‑null)
        @SuppressWarnings("unchecked")
        List<Record>[] emptySections = new List[4];
        for (int i = 0; i < emptySections.length; i++) {
            emptySections[i] = new LinkedList<>();
        }
        setPrivateField(original, "sections", emptySections);
        Message clone = original.clone();
        @SuppressWarnings("unchecked")
        List<?>[] cloned = (List<?>[]) getPrivateField(clone, "sections");
        assertEquals(emptySections.length, cloned.length, "Sections array length must match");
        for (int i = 0; i < cloned.length; i++) {
            assertNotNull(cloned[i], ("Cloned section " + i) + " must be non‑null");
            assertNotSame(emptySections[i], cloned[i], ("Cloned section " + i) + " must be a distinct list instance");
            assertTrue(cloned[i].isEmpty(), ("Cloned section " + i) + " must be empty");
        }
    }

    /**
     * Group: Clone with header mutation independence
     */
    @Test
    void testCloneHeaderMutationIndependence() {
        Message original = new Message();
        Header originalHeader = original.getHeader();
        originalHeader.setID(1234);
        originalHeader.setFlag(Flags.QR);
        Message clone = original.clone();
        // Verify distinct header objects
        Header clonedHeader = clone.getHeader();
        assertNotSame(originalHeader, clonedHeader, "Headers must be different objects");
        assertEquals(originalHeader, clonedHeader, "Headers must be equal in content");
        // Mutate original header
        originalHeader.setID(5678);
        assertNotEquals(originalHeader.getID(), clonedHeader.getID(), "Changing original header ID must not affect clone");
        // Mutate clone header
        clonedHeader.setFlag(Flags.AA);
        assertFalse(originalHeader.getFlag(Flags.AA), "Changing clone header flags must not affect original");
    }

    /**
     * Group: Clone with queryTSIG present
     */
    @Test
    void testCloneWithQueryTSIGPresent() throws Exception {
        Message original = new Message();
        // Mock a TSIGRecord whose cloneRecord returns a distinct mock
        TSIGRecord mockQueryTSIG = mock(TSIGRecord.class);
        TSIGRecord clonedQueryTSIG = mock(TSIGRecord.class);
        when(mockQueryTSIG.cloneRecord()).thenReturn(clonedQueryTSIG);
        setPrivateField(original, "querytsig", mockQueryTSIG);
        // generatedTsig stays null
        Message clone = original.clone();
        TSIGRecord cloneQueryTSIG = (TSIGRecord) getPrivateField(clone, "querytsig");
        assertNotNull(cloneQueryTSIG, "Cloned querytsig must not be null");
        assertNotSame(mockQueryTSIG, cloneQueryTSIG, "Cloned querytsig must be a new instance");
        assertEquals(clonedQueryTSIG, cloneQueryTSIG, "Cloned querytsig content must match mock return");
        assertNull(getPrivateField(clone, "generatedTsig"), "generatedTsig must remain null");
    }

    /**
     * Group: Clone with generatedTSIG present
     */
    @Test
    void testCloneWithGeneratedTSIGPresent() throws Exception {
        Message original = new Message();
        TSIGRecord mockGeneratedTSIG = mock(TSIGRecord.class);
        TSIGRecord clonedGeneratedTSIG = mock(TSIGRecord.class);
        when(mockGeneratedTSIG.cloneRecord()).thenReturn(clonedGeneratedTSIG);
        setPrivateField(original, "generatedTsig", mockGeneratedTSIG);
        // querytsig stays null
        Message clone = original.clone();
        TSIGRecord cloneGeneratedTSIG = (TSIGRecord) getPrivateField(clone, "generatedTsig");
        assertNotNull(cloneGeneratedTSIG, "Cloned generatedTsig must not be null");
        assertNotSame(mockGeneratedTSIG, cloneGeneratedTSIG, "Cloned generatedTsig must be a new instance");
        assertEquals(clonedGeneratedTSIG, cloneGeneratedTSIG, "Cloned generatedTsig content must match mock return");
        assertNull(getPrivateField(clone, "querytsig"), "querytsig must remain null");
    }

    /**
     * Group: Clone with both queryTSIG and generatedTSIG present
     */
    @Test
    void testCloneWithBothTSIGPresent() throws Exception {
        Message original = new Message();
        TSIGRecord mockQueryTSIG = mock(TSIGRecord.class);
        TSIGRecord clonedQueryTSIG = mock(TSIGRecord.class);
        when(mockQueryTSIG.cloneRecord()).thenReturn(clonedQueryTSIG);
        TSIGRecord mockGeneratedTSIG = mock(TSIGRecord.class);
        TSIGRecord clonedGeneratedTSIG = mock(TSIGRecord.class);
        when(mockGeneratedTSIG.cloneRecord()).thenReturn(clonedGeneratedTSIG);
        setPrivateField(original, "querytsig", mockQueryTSIG);
        setPrivateField(original, "generatedTsig", mockGeneratedTSIG);
        Message clone = original.clone();
        TSIGRecord cloneQueryTSIG = (TSIGRecord) getPrivateField(clone, "querytsig");
        TSIGRecord cloneGeneratedTSIG = (TSIGRecord) getPrivateField(clone, "generatedTsig");
        assertNotNull(cloneQueryTSIG);
        assertNotNull(cloneGeneratedTSIG);
        assertNotSame(mockQueryTSIG, cloneQueryTSIG);
        assertNotSame(mockGeneratedTSIG, cloneGeneratedTSIG);
        assertEquals(clonedQueryTSIG, cloneQueryTSIG);
        assertEquals(clonedGeneratedTSIG, cloneGeneratedTSIG);
    }

    /**
     * Group: Clone with zero‑length sections array
     */
    @Test
    void testCloneZeroLengthSectionsArray() throws Exception {
        Message original = new Message();
        // Force a zero‑length sections array via reflection
        setPrivateField(original, "sections", new List[0]);
        Message clone = original.clone();
        @SuppressWarnings("unchecked")
        List<?>[] clonedSections = (List<?>[]) getPrivateField(clone, "sections");
        assertNotNull(clonedSections, "Cloned sections array must not be null");
        assertEquals(0, clonedSections.length, "Cloned sections array length must be zero");
        // Header should still be cloned
        assertNotNull(clone.getHeader(), "Header must be present after cloning");
    }

    /**
     * Group: Clone when sections array is null (expect NPE)
     */
    @Test
    void testCloneSectionsArrayNullThrowsNPE() throws Exception {
        Message original = new Message();
        // force null
        setPrivateField(original, "sections", null);
        assertThrows(NullPointerException.class, original::clone, "Cloning with a null sections array should throw NullPointerException");
    }

    /**
     * Group: Clone when header is null (expect NPE)
     */
    @Test
    void testCloneHeaderNullThrowsNPE() throws Exception {
        Message original = new Message();
        // force null
        setPrivateField(original, "header", null);
        assertThrows(NullPointerException.class, original::clone, "Cloning with a null header should throw NullPointerException");
    }

    /**
     * Group: Clone when queryTSIG.cloneRecord throws IOException
     */
    @Test
    void testCloneQueryTSIGCloneRecordThrowsIOException() throws Exception {
        Message original = new Message();
        TSIGRecord mockQueryTSIG = mock(TSIGRecord.class);
        when(mockQueryTSIG.cloneRecord()).thenThrow(new IOException("mock IO"));
        setPrivateField(original, "querytsig", mockQueryTSIG);
        IOException thrown = assertThrows(IOException.class, original::clone, "IOException from querytsig.cloneRecord() must propagate");
        assertEquals("mock IO", thrown.getMessage());
    }

    /**
     * Group: Clone when generatedTSIG.cloneRecord throws IOException
     */
    @Test
    void testCloneGeneratedTSIGCloneRecordThrowsIOException() throws Exception {
        Message original = new Message();
        TSIGRecord mockGeneratedTSIG = mock(TSIGRecord.class);
        when(mockGeneratedTSIG.cloneRecord()).thenThrow(new IOException("generated IO"));
        setPrivateField(original, "generatedTsig", mockGeneratedTSIG);
        IOException thrown = assertThrows(IOException.class, original::clone, "IOException from generatedTsig.cloneRecord() must propagate");
        assertEquals("generated IO", thrown.getMessage());
    }

    /**
     * Group: Clone when super.clone throws CloneNotSupportedException
     */
    @Test
    void testCloneSuperCloneThrowsCloneNotSupportedException() {
        // Subclass that deliberately disables Cloneable by overriding clone and delegating to super.clone()
        class NonCloneableMessage extends Message {

            @Override
            public Message clone() {
                // Force a CloneNotSupportedException by calling Object.clone via reflection
                try {
                    Method m = Object.class.getDeclaredMethod("clone");
                    m.setAccessible(true);
                    return (Message) m.invoke(this);
                } catch (Exception e) {
                    // Wrap any reflection exception as a RuntimeException – the test expects the original
                    // CloneNotSupportedException to be thrown, which will be the cause.
                    throw new RuntimeException(e);
                }
            }
        }
        NonCloneableMessage msg = new NonCloneableMessage();
        // (because we bypass Message's own clone implementation) will throw CloneNotSupportedException.
        RuntimeException thrown = assertThrows(RuntimeException.class, msg::clone, "CloneNotSupportedException should be wrapped in RuntimeException due to reflection");
        assertTrue(thrown.getCause() instanceof CloneNotSupportedException, "Root cause must be CloneNotSupportedException");
    }
}
