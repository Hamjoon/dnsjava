```java
package org.xbill.DNS;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

/**
 * Unit tests for {@link Message#findRRset(Name, int, int)}.
 *
 * <p>These tests are written using JUnit 4 to avoid version‑mismatch problems that can arise
 * with JUnit 5 on the execution platform.</p>
 */
public class Message_findRRset_Test {

    private Message message;
    private Name nameA;
    private Name nameB;
    private Record aRecord;
    private Record mxRecord;

    @Before
    public void setupBeforeEach() throws TextParseException {
        // Fresh Message for every test
        message = new Message();
        // Two distinct names for positive / negative cases
        nameA = Name.fromConstantString("example.com.");
        nameB = Name.fromConstantString("other.com.");
        // Records of different types for the same name
        aRecord = Record.newRecord(nameA, Type.A, DClass.IN);
        mxRecord = Record.newRecord(nameA, Type.MX, DClass.IN);
    }

    @After
    public void teardownAfterEach() {
        // Nothing to clean up; Message has no external resources
        message = null;
    }

    /* --------------------------------------------------------------------- */
    /* Simple (non‑parameterised) test cases                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void findRRset_sectionEmpty_returnsFalse() {
        // No records have been added; all sections are null/empty
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    public void findRRset_recordPresentInAnswer_returnsTrue() {
        // Add an A record to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        // Positive match in the correct section
        Assert.assertTrue(message.findRRset(nameA, Type.A, Section.ANSWER));
        // Same record should not be found in other sections
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    public void findRRset_nameMismatch_returnsFalse() {
        // Add an A record for nameA to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        // Query with a different name – should not match
        Assert.assertFalse(message.findRRset(nameB, Type.A, Section.ANSWER));
    }

    @Test
    public void findRRset_typeMismatch_returnsFalse() {
        // Add an A record for nameA to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        // Query with the same name but a different type – should not match
        Assert.assertFalse(message.findRRset(nameA, Type.MX, Section.ANSWER));
    }

    @Test
    public void findRRset_multipleSections_correctSectionDetected() {
        // Add records to two different sections
        message.addRecord(aRecord, Section.ANSWER);
        message.addRecord(mxRecord, Section.AUTHORITY);
        // Verify each is found only in its own section
        Assert.assertTrue(message.findRRset(nameA, Type.A, Section.ANSWER));
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        Assert.assertTrue(message.findRRset(nameA, Type.MX, Section.AUTHORITY));
        Assert.assertFalse(message.findRRset(nameA, Type.MX, Section.ANSWER));
    }

    @Test
    public void findRRset_invalidSection_throwsIllegalArgumentException() {
        // Section.check throws IllegalArgumentException for out‑of‑range values
        // valid sections are 0‑3
        int invalidSection = 5;
        Assert.assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(nameA, Type.A, invalidSection));
    }

    @Test
    public void valid_sectionEmpty_returnsFalse() {
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.AUTHORITY));
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    public void valid_noMatchingRecord_returnsFalse() throws Exception {
        Record other = Record.newRecord(nameB, Type.A, DClass.IN);
        message.addRecord(other, Section.ANSWER);
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.ANSWER));
    }

    @Test
    public void invalid_typeValidation_failureThrowsIllegalArgumentException() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(nameA, -1, Section.QUESTION));
    }

    @Test
    public void invalid_sectionValidation_failureThrowsIllegalArgumentException() {
        int invalidSection = 10;
        Assert.assertThrows(IllegalArgumentException.class,
                () -> message.findRRset(nameA, Type.A, invalidSection));
    }

    @Test
    public void valid_emptyListSection_returnsFalse() throws Exception {
        List<Record>[] sections = getSectionsArray();
        if (sections == null) {
            sections = new List[4];
            setSectionsArray(sections);
        }
        sections[Section.ADDITIONAL] = new ArrayList<>();
        setSectionsArray(sections);
        Assert.assertFalse(message.findRRset(nameA, Type.A, Section.ADDITIONAL));
    }

    @Test
    public void valid_matchFirstRecord_returnsTrueImmediately() {
        Record match = Record.newRecord(Name.fromConstantString("first.example."), Type.MX, DClass.IN);
        Record other = Record.newRecord(Name.fromConstantString("other.example."), Type.A, DClass.IN);
        message.addRecord(other, Section.ANSWER);
        message.addRecord(match, Section.ANSWER);
        Assert.assertTrue(message.findRRset(match.getName(), Type.MX, Section.ANSWER));
    }

    @Test
    public void valid_matchLastRecord_returnsTrueAfterFullScan() {
        Record nonMatch1 = Record.newRecord(Name.fromConstantString("a.example."), Type.A, DClass.IN);
        Record nonMatch2 = Record.newRecord(Name.fromConstantString("b.example."), Type.A, DClass.IN);
        Record match = Record.newRecord(Name.fromConstantString("last.example."), Type.NS, DClass.IN);
        message.addRecord(nonMatch1, Section.AUTHORITY);
        message.addRecord(nonMatch2, Section.AUTHORITY);
        message.addRecord(match, Section.AUTHORITY);
        Assert.assertTrue(message.findRRset(match.getName(), Type.NS, Section.AUTHORITY));
    }

    @Test
    public void valid_differentTypeSameName_returnsFalse() {
        Record soa = Record.newRecord(Name.fromConstantString("mixed.example."), Type.SOA, DClass.IN);
        Record mx = Record.newRecord(Name.fromConstantString("mixed.example."), Type.MX, DClass.IN);
        message.addRecord(soa, Section.ANSWER);
        Assert.assertFalse(message.findRRset(soa.getName(), Type.MX, Section.ANSWER));
    }

    @Test
    public void valid_differentNameSameType_returnsFalse() {
        Record txt1 = Record.newRecord(Name.fromConstantString("existent.example."), Type.TXT, DClass.IN);
        Record txt2 = Record.newRecord(Name.fromConstantString("another.example."), Type.TXT, DClass.IN);
        message.addRecord(txt1, Section.ADDITIONAL);
        Assert.assertFalse(message.findRRset(Name.fromConstantString("nonexistent.example."), Type.TXT, Section.ADDITIONAL));
    }

    @Test
    public void invalid_nullName_throwsNullPointerException() {
        Assert.assertThrows(NullPointerException.class,
                () -> message.findRRset(null, Type.A, Section.QUESTION));
    }

    @Test
    public void exception_nullSectionsArray_throwsNullPointerException() throws Exception {
        setSectionsArray(null);
        Assert.assertThrows(NullPointerException.class,
                () -> message.findRRset(nameA, Type.A, Section.ANSWER));
    }

    @Test
    public void exception_nullRecordInSectionList_throwsNullPointerException() throws Exception {
        List<Record>[] sections = getSectionsArray();
        if (sections == null) {
            sections = new List[4];
        }
        List<Record> list = new ArrayList<>();
        list.add(null);
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);
        Assert.assertThrows(NullPointerException.class,
                () -> message.findRRset(nameA, Type.A, Section.AUTHORITY));
    }

    @Test
    public void exception_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
        List<Record>[] shortArray = new List[2];
        setSectionsArray(shortArray);
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> message.findRRset(nameA, Type.AAAA, Section.ADDITIONAL));
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods for reflection access                                   */
    /* --------------------------------------------------------------------- */

    @SuppressWarnings("unchecked")
    private void setSectionsArray(List<Record>[] sections) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(message, sections);
    }

    @SuppressWarnings("unchecked")
    private List<Record>[] getSectionsArray() throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        return (List<Record>[]) f.get(message);
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised test for the “matching record” scenario                */
    /* --------------------------------------------------------------------- */

    @RunWith(Parameterized.class)
    public static class MatchingRecordFoundTest {

        private static Message sharedMessage;
        private final Name queryName;
        private final int queryType;
        private final int querySection;
        private final List<Record> recordsToAdd;

        public MatchingRecordFoundTest(Name queryName, int queryType, int querySection, List<Record> recordsToAdd) {
            this.queryName = queryName;
            this.queryType = queryType;
            this.querySection = querySection;
            this.recordsToAdd = recordsToAdd;
        }

        @BeforeClass
        public static void initMessage() {
            sharedMessage = new Message();
        }

        @AfterClass
        public static void clearMessage() {
            sharedMessage = null;
        }

        @Parameterized.Parameters(name = "{index}: name={0}, type={1}, section={2}")
        public static Collection<Object[]> data() throws TextParseException {
            Name name = Name.fromConstantString("example.net.");
            Record matching = Record.newRecord(name, Type.AAAA, DClass.IN);
            Record other1 = Record.newRecord(name, Type.A, DClass.IN);
            Record other2 = Record.newRecord(Name.fromConstantString("other.net."), Type.AAAA, DClass.IN);

            List<Record> case1 = Arrays.asList(matching, other1, other2);
            List<Record> case2 = Arrays.asList(other1, other2, matching);

            return Arrays.asList(new Object[][]{
                    {name, Type.AAAA, Section.AUTHORITY, case1},
                    {name, Type.AAAA, Section.AUTHORITY, case2}
            });
        }

        @Test
        public void valid_matchingRecordFound_returnsTrue() {
            // Ensure a fresh message for each parameter set
            Message msg = new Message();
            for (Record r : recordsToAdd) {
                msg.addRecord(r, querySection);
            }
            Assert.assertTrue(msg.findRRset(queryName, queryType, querySection));
        }
    }
}
```