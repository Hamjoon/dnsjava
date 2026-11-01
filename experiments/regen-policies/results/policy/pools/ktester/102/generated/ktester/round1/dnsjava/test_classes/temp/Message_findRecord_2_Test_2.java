package org.xbill.DNS;

import java.lang.reflect.Field;
import java.util.*;
// import org.junit.*;
// import static org.junit.Assert.*;

class Message_findRecord_2_Test {

    private Message message;

    private Record aRecord;

    private Record txtRecord;

//     @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a fresh Message for each test
        message = new Message();
        // Prepare a couple of records that can be used in the tests
        Name name = Name.fromConstantString("www.example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        message = null;
        aRecord = null;
        txtRecord = null;
    }

//     private static Stream<Arguments> provideFoundInVariousSections() {
//         return // target in ANSWER
//         Stream.// target in ANSWER
//         of(// target in AUTHORITY, ANSWER null
//         Arguments.of(Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN), Section.ANSWER, "The target record is added to the ANSWER section. The loop checks Section.ANSWER first, finds a non‑null list containing the record, and returns true immediately."), // target in ADDITIONAL, previous sections empty or null
//         Arguments.of(Record.newRecord(Name.fromConstantString("example.org."), Type.MX, DClass.IN), Section.AUTHORITY, "The ANSWER section is null, the AUTHORITY section contains the target record. The method skips the null ANSWER, evaluates AUTHORITY, finds the record and returns true."), // target in ADDITIONAL, ANSWER empty list, AUTHORITY non‑null but not containing
//         Arguments.of(Record.newRecord(Name.fromConstantString("example.net."), Type.TXT, DClass.IN), Section.ADDITIONAL, "Both ANSWER and AUTHORITY sections are either null or do not contain the record, but the ADDITIONAL section holds the record. The loop reaches ADDITIONAL, finds the record and returns true."), Arguments.of(Record.newRecord(Name.fromConstantString("example.edu."), Type.AAAA, DClass.IN), Section.ADDITIONAL, "The ANSWER section contains an empty list, the AUTHORITY section contains a non‑null list that does not contain the record, and the ADDITIONAL section contains the record. The method correctly continues past empty/non‑matching sections and finally returns true."));
//     }

//     private static Stream<Arguments> provideNotFoundCases() {
//         return // all sections null
//         Stream.// all sections null
//         of(// sections non‑null but without the record
//         Arguments.of(Record.newRecord(Name.fromConstantString("missing.com."), Type.SRV, DClass.IN), "All three sections (ANSWER, AUTHORITY, ADDITIONAL) are null. The loop never enters the inner condition and returns false after completing all iterations."), // empty lists and null sections
//         Arguments.of(Record.newRecord(Name.fromConstantString("absent.org."), Type.NS, DClass.IN), "Each section is non‑null but contains lists that do not include the target record. The method checks each list, finds `contains(r)` false each time, and finally returns false."), Arguments.of(Record.newRecord(Name.fromConstantString("empty.net."), Type.CNAME, DClass.IN), "ANSWER section is an empty list, AUTHORITY section is null, ADDITIONAL section is an empty list. No `contains` check succeeds, so the method returns false."));
//     }

//     private static Stream<Arguments> provideMultipleSectionCases() {
//         return Stream.of(Arguments.of(Record.newRecord(Name.fromConstantString("multi.com."), Type.A, DClass.IN), "The target record is added to both the ANSWER and AUTHORITY sections. The method should return true after checking the ANSWER section and must not continue to later sections."), Arguments.of(Record.newRecord(Name.fromConstantString("multi.org."), Type.MX, DClass.IN), "The target record appears in ANSWER, AUTHORITY and ADDITIONAL sections simultaneously. The loop should stop at the first match (ANSWER) and return true."));
//     }

//     private static Stream<Arguments> provideNullRecordCases() {
//         return Stream.of(Arguments.of("A null Record is passed. All non‑null sections contain regular records, but `contains(null)` never matches, so the method should safely return false without throwing an exception."), Arguments.of("All three sections are null and the argument is null. The method should simply iterate, skip null sections and finally return false."));
//     }

    /**
     * Clears all sections (sets each entry to {@code null}) using reflection.
     */
    private static void clearAllSections(Message msg) throws Exception {
        @SuppressWarnings("unchecked")
        List<Record>[] empty = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        setSectionsArray(msg, empty);
    }

    /**
     * Sets a specific section list via reflection.
     *
     * @param msg     Message instance
     * @param section Section constant (e.g. {@link Section#ANSWER})
     * @param list    List to store in that section (may be {@code null})
     */
    private static void setSectionList(Message msg, int section, List<Record> list) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        if (sections == null) {
            sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
            f.set(msg, sections);
        }
        sections[section] = list;
    }

    /**
     * Directly replaces the whole {@code sections} array.
     *
     * @param msg      Message instance
     * @param newArray New array (may be {@code null})
     */
    private static void setSectionsArray(Message msg, List<Record>[] newArray) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, newArray);
    }

//     @Test
//     void findRecord_returnsTrueWhenRecordIsInAnswerSection() {
//         message.addRecord(aRecord, Section.ANSWER);
//         assertTrue("findRecord should return true for a record present in the ANSWER section", message.findRecord(aRecord));
//     }

//     @Test
//     void findRecord_returnsFalseWhenRecordIsNotPresent() {
//         assertFalse("findRecord should return false when the record is not present in any section", message.findRecord(txtRecord));
//     }

//     @Test
//     void findRecord_checksAllSections() {
//         message.addRecord(aRecord, Section.QUESTION);
//         message.addRecord(aRecord, Section.ADDITIONAL);
//         assertTrue("findRecord should locate the record regardless of which section it resides in", message.findRecord(aRecord));
//     }

//     @Test
//     void findRecord_returnsFalseWhenRecordExistsInDifferentSection() {
//         message.addRecord(txtRecord, Section.ANSWER);
//         assertFalse("findRecord should return false when the exact Record instance is not present", message.findRecord(aRecord));
//     }

//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("provideFoundInVariousSections")
//     void findRecord_returnsTrueWhenRecordIsFoundInVariousSections(Record target, int sectionToAdd, String description) throws Exception {
//         // target in ANSWER
//         Record target = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
//         clearAllSections(message);
//         message.addRecord(target, Section.ANSWER);
//         assertTrue(message.findRecord(target));
//         // target in AUTHORITY (ANSWER null)
//         target = Record.newRecord(Name.fromConstantString("example.org."), Type.MX, DClass.IN);
//         clearAllSections(message);
//         message.addRecord(target, Section.AUTHORITY);
//         assertTrue(message.findRecord(target));
//         // target in ADDITIONAL (ANSWER & AUTHORITY empty/null)
//         target = Record.newRecord(Name.fromConstantString("example.net."), Type.TXT, DClass.IN);
//         clearAllSections(message);
//         message.addRecord(target, Section.ADDITIONAL);
//         assertTrue(message.findRecord(target));
//         // target in ADDITIONAL, ANSWER empty list, AUTHORITY non‑null but not containing
//         target = Record.newRecord(Name.fromConstantString("example.edu."), Type.AAAA, DClass.IN);
//         clearAllSections(message);
//         setSectionList(message, Section.ANSWER, Collections.emptyList());
//         setSectionList(message, Section.AUTHORITY, Collections.singletonList(Record.newRecord(Name.fromConstantString("other.edu."), Type.A, DClass.IN)));
//         message.addRecord(target, Section.ADDITIONAL);
//         assertTrue(message.findRecord(target));
//     }

//     @ParameterizedTest(name = "[{index}] {1}")
//     @MethodSource("provideNotFoundCases")
//     void findRecord_returnsFalseWhenRecordIsAbsent(Record target, String description) throws Exception {
//         // all sections null
//         clearAllSections(message);
//         Record target = Record.newRecord(Name.fromConstantString("missing.com."), Type.SRV, DClass.IN);
//         assertFalse(message.findRecord(target));
//         // sections non‑null but without the record
//         clearAllSections(message);
//         Record unrelated = Record.newRecord(Name.fromConstantString("other.com."), Type.A, DClass.IN);
//         message.addRecord(unrelated, Section.ANSWER);
//         setSectionList(message, Section.ADDITIONAL, Collections.emptyList());
//         target = Record.newRecord(Name.fromConstantString("absent.org."), Type.NS, DClass.IN);
//         assertFalse(message.findRecord(target));
//         // empty lists and null sections
//         clearAllSections(message);
//         setSectionList(message, Section.ANSWER, Collections.emptyList());
//         setSectionList(message, Section.AUTHORITY, null);
//         setSectionList(message, Section.ADDITIONAL, Collections.emptyList());
//         target = Record.newRecord(Name.fromConstantString("empty.net."), Type.CNAME, DClass.IN);
//         assertFalse(message.findRecord(target));
//     }

//     @ParameterizedTest(name = "[{index}] {1}")
//     @MethodSource("provideMultipleSectionCases")
//     void findRecord_earlyExitWhenRecordInMultipleSections(Record target, String description) throws Exception {
//         Record target = Record.newRecord(Name.fromConstantString("multi.com."), Type.A, DClass.IN);
//         message.addRecord(target, Section.ANSWER);
//         message.addRecord(target, Section.AUTHORITY);
//         message.addRecord(target, Section.ADDITIONAL);
//         assertTrue("The method should stop at the first match (ANSWER) and return true", message.findRecord(target));
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("provideNullRecordCases")
//     void findRecord_returnsFalseWhenArgumentIsNull(String description) throws Exception {
//         clearAllSections(message);
//         Record dummy = Record.newRecord(Name.fromConstantString("dummy.com."), Type.A, DClass.IN);
//         message.addRecord(dummy, Section.ANSWER);
//         message.addRecord(dummy, Section.AUTHORITY);
//         message.addRecord(dummy, Section.ADDITIONAL);
//         assertFalse(message.findRecord((Record) null));
//     }

//     @Test
//     void findRecord_returnsTrueWhenListContainsNullBeforeTarget() throws Exception {
//         Record target = Record.newRecord(Name.fromConstantString("nullentry.com."), Type.TXT, DClass.IN);
//         List<Record> list = new ArrayList<>(Arrays.asList(null, target));
//         setSectionList(message, Section.ANSWER, list);
//         assertTrue(message.findRecord(target));
//     }

//     @Test
//     void findRecord_returnsFalseWhenOnlyNullInList() throws Exception {
//         List<Record> list = new ArrayList<>(Collections.singletonList(null));
//         setSectionList(message, Section.ANSWER, list);
//         Record target = Record.newRecord(Name.fromConstantString("onlynull.com."), Type.AAAA, DClass.IN);
//         assertFalse(message.findRecord(target));
//     }

//     @Test
//     void findRecord_findsRecordInAdditionalAfterNullAndEmptySections() throws Exception {
//         setSectionList(message, Section.AUTHORITY, Collections.emptyList());
//         Record target = Record.newRecord(Name.fromConstantString("last.com."), Type.SRV, DClass.IN);
//         message.addRecord(target, Section.ADDITIONAL);
//         assertTrue(message.findRecord(target));
//     }

//     @Test
//     void findRecord_throwsNullPointerExceptionWhenSectionsArrayIsNull() throws Exception {
//         setSectionsArray(message, null);
//         Record target = Record.newRecord(Name.fromConstantString("test.com."), Type.A, DClass.IN);
//         try {
//             message.findRecord(target);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void findRecord_throwsArrayIndexOutOfBoundsExceptionWhenSectionsArrayTooSmall() throws Exception {
//         @SuppressWarnings("unchecked")
//         List<Record>[] tiny = (List<Record>[]) new List[2];
//         setSectionsArray(message, tiny);
//         Record target = Record.newRecord(Name.fromConstantString("tiny.com."), Type.TXT, DClass.IN);
//         try {
//             message.findRecord(target);
//             fail("Expected ArrayIndexOutOfBoundsException");
//         } catch (ArrayIndexOutOfBoundsException ignored) {
//         }
//     }

//     @Test
//     void findRecord_propagatesExceptionFromRecordEquals() throws Exception {
//         // Mockito is available in the classpath of the original project.
//         Record problematic = org.mockito.Mockito.mock(Record.class);
//         org.mockito.Mockito.when(problematic.equals(org.mockito.Mockito.any())).thenThrow(new RuntimeException("equals exploded"));
//         Record dummy = Record.newRecord(Name.fromConstantString("dummy.com."), Type.A, DClass.IN);
//         message.addRecord(dummy, Section.ANSWER);
//         try {
//             message.findRecord(problematic);
//             fail("Expected RuntimeException");
//         } catch (RuntimeException e) {
//             assertEquals("equals exploded", e.getMessage());
//         }
//     }

//     @Before
//     public void setup() throws Exception {
//         message = new Message();
//         Name name = Name.fromConstantString("www.example.com.");
//         aRecord = Record.newRecord(name, Type.A, DClass.IN);
//         txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
//     }

//     @After
//     public void teardown() {
//         message = null;
//         aRecord = null;
//         txtRecord = null;
//     }
}